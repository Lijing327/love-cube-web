import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useUserStore } from '@/stores/user.js'

const DECLINE_KEY = 'lc-pwa-install-declined-at'
const IOS_TIP_KEY = 'lc-pwa-ios-tip-dismissed'
const RETRY_MS = 3 * 24 * 60 * 60 * 1000

function isStandalone() {
  if (typeof window === 'undefined') return false
  return window.matchMedia('(display-mode: standalone)').matches
    || window.navigator.standalone === true
}

function recentlyDeclined() {
  try {
    const at = Number(localStorage.getItem(DECLINE_KEY) || 0)
    return at > 0 && Date.now() - at < RETRY_MS
  } catch {
    return false
  }
}

function markDeclined() {
  try {
    localStorage.setItem(DECLINE_KEY, String(Date.now()))
  } catch {
    // ignore
  }
}

function getIosBrowser() {
  if (typeof navigator === 'undefined') return ''
  if (isStandalone()) return ''
  const ua = navigator.userAgent || ''
  if (!/iphone|ipad|ipod/i.test(ua)) return ''
  if (/micromessenger/i.test(ua)) return 'wechat'
  if (/crios|fxios|edgios/i.test(ua)) return 'other'
  if (/safari/i.test(ua) && !/chrome|android/i.test(ua)) return 'safari'
  return 'other'
}

function isIosSafari() {
  return getIosBrowser() === 'safari'
}

function needsSafariSwitch() {
  const kind = getIosBrowser()
  return kind === 'wechat' || kind === 'other'
}

function iosTipDismissed() {
  try {
    return localStorage.getItem(IOS_TIP_KEY) === '1'
  } catch {
    return false
  }
}

export function usePwa() {
  const userStore = useUserStore()
  const showIosTip = ref(false)
  const iosTipMode = ref('safari')
  const copyDone = ref(false)
  let deferredPrompt = null
  let prompted = false
  let iosTipTimer = 0

  function clearIosTipTimer() {
    if (iosTipTimer) {
      window.clearTimeout(iosTipTimer)
      iosTipTimer = 0
    }
  }

  function resolveIosTipMode() {
    const kind = getIosBrowser()
    if (kind === 'safari') return 'safari'
    if (kind === 'wechat') return 'wechat'
    return 'switch'
  }

  function shouldShowIosTip() {
    return userStore.isLoggedIn && !iosTipDismissed() && (isIosSafari() || needsSafariSwitch())
  }

  function scheduleIosTip() {
    clearIosTipTimer()
    if (!shouldShowIosTip()) {
      showIosTip.value = false
      return
    }
    iosTipMode.value = resolveIosTipMode()
    iosTipTimer = window.setTimeout(() => {
      iosTipTimer = 0
      if (!shouldShowIosTip()) return
      iosTipMode.value = resolveIosTipMode()
      showIosTip.value = true
    }, 2000)
  }

  async function copyPageLink() {
    const currentUrl = window.location.href
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(currentUrl)
      } else {
        const input = document.createElement('input')
        input.value = currentUrl
        document.body.appendChild(input)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
      }
      copyDone.value = true
    } catch {
      copyDone.value = false
    }
  }

  function dismissIosTip() {
    showIosTip.value = false
    try {
      localStorage.setItem(IOS_TIP_KEY, '1')
    } catch {
      // ignore
    }
  }

  function onBeforeInstall(event) {
    event.preventDefault()
    deferredPrompt = event
  }

  async function onFirstGesture() {
    if (prompted || !deferredPrompt || isStandalone() || recentlyDeclined()) return
    prompted = true
    const event = deferredPrompt
    deferredPrompt = null
    event.prompt()
    const result = await event.userChoice.catch(() => null)
    if (result?.outcome !== 'accepted') {
      markDeclined()
    }
    detachGestures()
  }

  function detachGestures() {
    window.removeEventListener('pointerdown', onFirstGesture, true)
    window.removeEventListener('keydown', onFirstGesture, true)
  }

  watch(() => userStore.isLoggedIn, (loggedIn) => {
    if (!loggedIn) {
      clearIosTipTimer()
      showIosTip.value = false
      return
    }
    scheduleIosTip()
  })

  onMounted(async () => {
    scheduleIosTip()

    if (!isStandalone() && !recentlyDeclined()) {
      window.addEventListener('beforeinstallprompt', onBeforeInstall)
      window.addEventListener('pointerdown', onFirstGesture, true)
      window.addEventListener('keydown', onFirstGesture, true)
    }

    if (import.meta.env.DEV) return
    try {
      const { registerSW } = await import('virtual:pwa-register')
      const updateSW = registerSW({
        immediate: true,
        onNeedRefresh() {
          updateSW(true)
        }
      })
    } catch {
      // 开发环境或未生成 SW 时忽略
    }
  })

  onUnmounted(() => {
    clearIosTipTimer()
    window.removeEventListener('beforeinstallprompt', onBeforeInstall)
    detachGestures()
  })

  return {
    showIosTip,
    iosTipMode,
    copyDone,
    copyPageLink,
    dismissIosTip
  }
}
