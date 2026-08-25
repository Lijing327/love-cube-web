<template>
  <div class="notif-center-page">
    <van-nav-bar title="消息中心" left-arrow fixed placeholder @click-left="router.back()">
      <template #right>
        <span class="nav-unread" v-if="unreadTotal > 0">未读 {{ unreadTotal > 99 ? '99+' : unreadTotal }}</span>
        <van-icon name="setting-o" class="nav-settings" @click.stop="router.push('/fellowship/notification-settings')" />
      </template>
    </van-nav-bar>

    <div class="toolbar">
      <van-button size="small" type="primary" plain :disabled="!unreadTotal" @click="onReadAll">全部已读</van-button>
    </div>

    <van-tabs v-model:active="tabIndex" sticky offset-top="46" color="#ff5f84" @change="onTabChange">
      <van-tab v-for="t in tabs" :key="t.key" :title="t.title" />
    </van-tabs>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        :key="tabIndex"
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <van-swipe-cell v-for="item in list" :key="item.id">
          <div class="notif-row" :class="{ unread: !item.isRead, read: item.isRead }" @click="openItem(item)">
            <div class="notif-dot-wrap">
              <span v-if="!item.isRead" class="dot" />
            </div>
            <div class="notif-body">
              <p class="title">{{ item.title }}</p>
              <p class="content">{{ item.content }}</p>
              <p class="meta">
                <span class="type-tag">{{ typeLabel(item.type) }}</span>
                <span class="time">{{ formatTime(item.createdAt) }}</span>
              </p>
            </div>
          </div>
          <template #right>
            <van-button square type="danger" text="删除" class="swipe-del" @click.stop="removeItem(item)" />
          </template>
        </van-swipe-cell>
        <van-empty
          v-if="!loading && !list.length"
          description="暂无消息，有新动态时会在这里提醒你"
        />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import {
  getNotifications,
  getNotifUnreadCount,
  markNotifRead,
  markAllNotifRead,
  deleteNotification,
  unwrapNotificationList
} from '@/api/notification.js'
import { reportGrowthCampaignClick } from '@/api/growthCampaign.js'
import { formatTime } from '@/utils/format.js'
import { toRouterPath } from '@/utils/appPath.js'

const router = useRouter()

const tabs = [
  { key: 'all', title: '全部', api: null },
  { key: 'unread', title: '未读', api: 'UNREAD' },
  { key: 'group', title: '团体', api: 'GROUP' },
  { key: 'review', title: '审核', api: 'REVIEW' },
  { key: 'interaction', title: '互动', api: 'INTERACTION' },
  { key: 'announcement', title: '公告', api: 'ANNOUNCEMENT' },
  { key: 'system', title: '系统', api: 'SYSTEM' }
]

const TYPE_LABELS = {
  GROUP_APPLICATION_APPROVED: '团体',
  GROUP_APPLICATION_REJECTED: '团体',
  GROUP_JOIN_REQUEST: '团体',
  GROUP_WEEKLY_DIGEST: '团体周报',
  CONTENT_MODERATION_PASSED: '审核',
  CONTENT_MODERATION_REJECTED: '审核',
  MATCH_PROFILE_REVIEW_PASSED: '审核',
  MATCH_PROFILE_REVIEW_REJECTED: '审核',
  PLATFORM_ANNOUNCEMENT: '公告',
  PROFILE_LIKED: '互动',
  PROFILE_VIEWED: '互动',
  CONTENT_LIKED: '互动',
  CONTENT_COMMENTED: '互动',
  USER_FOLLOWED: '互动',
  MUTUAL_MATCH: '互动',
  GROWTH_OPERATOR_REMINDER: '运营'
}

const tabIndex = ref(0)
const list = ref([])
const page = ref(0)
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const unreadTotal = ref(0)

const activeTabApi = computed(() => tabs[tabIndex.value]?.api ?? null)

function typeLabel(type) {
  if (!type) return '通知'
  return TYPE_LABELS[type] || type
}

async function loadUnreadBadge() {
  try {
    const r = await getNotifUnreadCount()
    unreadTotal.value = Number(r?.count || 0)
  } catch {
    unreadTotal.value = 0
  }
}

async function onLoad() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: 20 }
    if (activeTabApi.value) params.tab = activeTabApi.value
    const res = await getNotifications(params)
    const rows = unwrapNotificationList(res)
    if (page.value === 0) list.value = []
    list.value = list.value.concat(rows)
    const totalPages = res?.totalPages
    const total = Number(res?.total ?? 0)
    page.value += 1
    if (rows.length < 20 || (totalPages != null && page.value >= totalPages)) {
      finished.value = true
    } else if (total > 0 && list.value.length >= total) {
      finished.value = true
    }
  } catch (e) {
    showToast({ type: 'fail', message: e.message || '加载失败' })
    finished.value = true
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  finished.value = false
  page.value = 0
  list.value = []
  try {
    await onLoad()
    await loadUnreadBadge()
  } catch (e) {
    showToast({ type: 'fail', message: e.message || '刷新失败' })
  } finally {
    refreshing.value = false
  }
}

function onTabChange() {
  finished.value = false
  page.value = 0
  list.value = []
  loading.value = false
}

async function openItem(item) {
  if (!item?.isRead) {
    try {
      await markNotifRead(item.id)
      item.isRead = true
      await loadUnreadBadge()
    } catch {
      /* ignore */
    }
  }

  if (item.type === 'GROWTH_OPERATOR_REMINDER' && item.deliveryId) {
    let targetUrl = item.actionUrl || item.linkUrl
    try {
      const res = await reportGrowthCampaignClick(item.deliveryId)
      if (res?.actionUrl) targetUrl = res.actionUrl
    } catch {
      /* 上报失败仍兜底跳转 */
    }
    navigateByUrl(targetUrl)
    return
  }

  if (navigateByUrl(item.linkUrl)) return
  if (item.targetType === 'USER' && item.targetId) {
    router.push(`/fellowship/user-profile/${item.targetId}`)
  }
}

function navigateByUrl(url) {
  const path = toRouterPath(url)
  if (path.startsWith('/')) {
    router.push(path)
    return true
  }
  return false
}

async function onReadAll() {
  if (!unreadTotal.value) return
  try {
    await showConfirmDialog({ title: '全部已读', message: '确定将所有消息标为已读？' })
    await markAllNotifRead()
    list.value = list.value.map((n) => ({ ...n, isRead: true }))
    await loadUnreadBadge()
    if (activeTabApi.value === 'UNREAD') {
      finished.value = false
      page.value = 0
      list.value = []
    }
    showToast({ type: 'success', message: '已全部标为已读' })
  } catch (e) {
    if (e !== 'cancel') showToast({ type: 'fail', message: e.message || '操作失败' })
  }
}

async function removeItem(item) {
  try {
    await showConfirmDialog({ title: '删除消息', message: '确定删除这条消息？' })
    await deleteNotification(item.id)
    list.value = list.value.filter((x) => x.id !== item.id)
    await loadUnreadBadge()
    showToast({ type: 'success', message: '已删除' })
  } catch (e) {
    if (e !== 'cancel') showToast({ type: 'fail', message: e.message || '删除失败' })
  }
}

onMounted(() => {
  loadUnreadBadge()
})
</script>

<style scoped>
.notif-center-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
}
.nav-unread {
  font-size: 12px;
  color: #ff5f84;
  margin-right: 8px;
}
.nav-settings {
  font-size: 18px;
  color: #323233;
  padding: 4px;
}
.toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px 0;
  background: #f5f6f8;
}
.notif-row {
  display: flex;
  gap: 8px;
  padding: 12px 14px;
  background: #fff;
  border-bottom: 1px solid #eef0f3;
}
.notif-row.unread {
  background: linear-gradient(90deg, rgba(255, 95, 132, 0.12) 0, #fff8fa 10px, #fff8fa 100%);
  border-left: 3px solid #ff5f84;
}
.notif-row.read {
  opacity: 0.88;
}
.notif-row.read .title {
  font-weight: 500;
  color: #5c6670;
}
.notif-row.read .content {
  color: #8898aa;
}
.notif-dot-wrap {
  width: 10px;
  padding-top: 4px;
}
.dot {
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff5f84;
}
.notif-body {
  flex: 1;
  min-width: 0;
}
.title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.content {
  margin: 0 0 6px;
  font-size: 13px;
  color: #5c6670;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.meta {
  margin: 0;
  font-size: 12px;
  color: #8898aa;
  display: flex;
  align-items: center;
  gap: 8px;
}
.type-tag {
  background: #eef2f7;
  color: #4a5a6a;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}
.swipe-del {
  height: 100%;
}
</style>
