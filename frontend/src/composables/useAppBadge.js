import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { getNotifUnreadCountCached } from '@/api/notification.js'
import { useMessageStore } from '@/stores/message.js'
import { useUserStore } from '@/stores/user.js'

const platformUnread = ref(0)
let pollTimer = null
let subscribers = 0
let lastNotified = 0

function parseUnread(res) {
  return Number(res?.count ?? res?.unreadCount ?? 0) || 0
}

function applySystemBadge(count) {
  if (typeof navigator === 'undefined') return
  try {
    if (count > 0 && typeof navigator.setAppBadge === 'function') {
      navigator.setAppBadge(count)
      return
    }
    if (typeof navigator.clearAppBadge === 'function') {
      navigator.clearAppBadge()
    }
  } catch {
    // Badge API 在未安装或权限不足时会失败，忽略即可
  }
}

function maybeNotify(count) {
  if (typeof document === 'undefined' || typeof Notification === 'undefined') return
  if (document.visibilityState !== 'hidden') return
  if (Notification.permission !== 'granted') return
  if (count <= lastNotified) return
  lastNotified = count
  try {
    new Notification('Love Cube', {
      body: count > 99 ? '你有 99+ 条未读消息' : `你有 ${count} 条未读消息`,
      icon: '/pwa/icon-192.png',
      badge: '/pwa/icon-192.png',
      tag: 'lc-unread',
      renotify: true
    })
  } catch {
    // 部分浏览器禁止页面内直接弹通知
  }
}

export async function refreshPlatformUnread() {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) {
    platformUnread.value = 0
    applySystemBadge(0)
    return 0
  }
  try {
    const count = parseUnread(await getNotifUnreadCountCached(15000))
    platformUnread.value = count
    return count
  } catch {
    return platformUnread.value
  }
}

export async function requestAppNotifyPermission() {
  if (typeof Notification === 'undefined') return 'denied'
  if (Notification.permission !== 'default') return Notification.permission
  try {
    return await Notification.requestPermission()
  } catch {
    return 'denied'
  }
}

export function useAppBadge() {
  const messageStore = useMessageStore()
  const userStore = useUserStore()
  const totalUnread = computed(() => platformUnread.value + Number(messageStore.totalUnread || 0))
  const badgeText = computed(() => {
    const n = totalUnread.value
    if (n <= 0) return ''
    return n > 99 ? '99+' : String(n)
  })

  watch(totalUnread, (count) => {
    applySystemBadge(count)
    maybeNotify(count)
  }, { immediate: true })

  watch(() => userStore.isLoggedIn, (loggedIn) => {
    if (!loggedIn) {
      platformUnread.value = 0
      lastNotified = 0
      applySystemBadge(0)
      return
    }
    refreshPlatformUnread()
  })

  onMounted(() => {
    subscribers += 1
    if (subscribers === 1) {
      refreshPlatformUnread()
      pollTimer = window.setInterval(() => {
        refreshPlatformUnread()
      }, 60000)
    }
  })

  onUnmounted(() => {
    subscribers = Math.max(0, subscribers - 1)
    if (subscribers === 0 && pollTimer) {
      window.clearInterval(pollTimer)
      pollTimer = null
    }
  })

  return {
    platformUnread,
    totalUnread,
    badgeText,
    refreshPlatformUnread,
    requestAppNotifyPermission
  }
}
