<template>
  <section class="platform-page messages-page">
    <WechatNotifyEntryBanner />

    <div class="platform-card messages-header">
      <h1>消息通知中心</h1>
      <p>统一接收平台通知。团体周报只发给管理员，用来看本周打卡和待办；可删除单条，或关闭后续周报。</p>
    </div>

    <div class="platform-card">
      <p class="messages-lead">
        团体周报只发给管理员，用来看本周打卡和待办。可点「删除」去掉已收到的，或「不再接收周报」关闭以后的推送。
      </p>
      <p v-if="actionHint" class="action-hint">{{ actionHint }}</p>
      <van-tabs v-model:active="activeTab" class="messages-tabs">
        <van-tab title="全部">
          <div class="list-wrap">
            <p v-if="!allMessages.length" class="empty-hint">暂无通知</p>
            <article
              v-for="item in allMessages"
              :key="`all-${item.id}`"
              class="message-item"
              @click="openItem(item)"
            >
              <div class="message-main">
                <p class="message-kicker">{{ typeLabel(item) }}</p>
                <p class="message-title">{{ item.title }}</p>
                <p class="message-content">{{ item.content }}</p>
                <div class="msg-actions">
                  <button
                    v-if="groupIdOf(item)"
                    type="button"
                    class="msg-btn"
                    @click.stop="openItem(item)"
                  >查看团体</button>
                  <button
                    v-if="isWeeklyDigest(item)"
                    type="button"
                    class="msg-btn"
                    :disabled="busy"
                    @click.stop="unsubscribeWeeklyDigest"
                  >不再接收周报</button>
                  <button
                    type="button"
                    class="msg-btn msg-btn-danger"
                    :disabled="busy"
                    @click.stop="removeItem(item)"
                  >删除</button>
                </div>
              </div>
              <small>{{ item.time }}</small>
            </article>
          </div>
        </van-tab>

        <van-tab title="系统通知">
          <div class="list-wrap">
            <p v-if="!platformNotices.length" class="empty-hint">暂无系统通知</p>
            <article
              v-for="item in platformNotices"
              :key="`platform-${item.id}`"
              class="message-item"
              @click="openItem(item)"
            >
              <div class="message-main">
                <p class="message-kicker">{{ typeLabel(item) }}</p>
                <p class="message-title">{{ item.title }}</p>
                <p class="message-content">{{ item.content }}</p>
                <div class="msg-actions">
                  <button
                    v-if="groupIdOf(item)"
                    type="button"
                    class="msg-btn"
                    @click.stop="openItem(item)"
                  >查看团体</button>
                  <button
                    v-if="isWeeklyDigest(item)"
                    type="button"
                    class="msg-btn"
                    :disabled="busy"
                    @click.stop="unsubscribeWeeklyDigest"
                  >不再接收周报</button>
                  <button
                    type="button"
                    class="msg-btn msg-btn-danger"
                    :disabled="busy"
                    @click.stop="removeItem(item)"
                  >删除</button>
                </div>
              </div>
              <small>{{ item.time }}</small>
            </article>
          </div>
        </van-tab>

        <van-tab title="活动通知">
          <div class="list-wrap">
            <p v-if="!eventNotices.length" class="empty-hint">暂无活动通知</p>
            <article
              v-for="item in eventNotices"
              :key="`event-${item.id}`"
              class="message-item"
              @click="openItem(item)"
            >
              <div class="message-main">
                <p class="message-kicker">{{ typeLabel(item) }}</p>
                <p class="message-title">{{ item.title }}</p>
                <p class="message-content">{{ item.content }}</p>
                <div class="msg-actions">
                  <button
                    type="button"
                    class="msg-btn msg-btn-danger"
                    :disabled="busy"
                    @click.stop="removeItem(item)"
                  >删除</button>
                </div>
              </div>
              <small>{{ item.time }}</small>
            </article>
          </div>
        </van-tab>

        <van-tab title="内容互动">
          <div class="list-wrap">
            <p v-if="!contentInteractions.length" class="empty-hint">暂无内容互动</p>
            <article
              v-for="item in contentInteractions"
              :key="`content-${item.id}`"
              class="message-item"
              @click="openItem(item)"
            >
              <div class="message-main">
                <p class="message-kicker">{{ typeLabel(item) }}</p>
                <p class="message-title">{{ item.title }}</p>
                <p class="message-content">{{ item.content }}</p>
                <div class="msg-actions">
                  <button
                    type="button"
                    class="msg-btn msg-btn-danger"
                    :disabled="busy"
                    @click.stop="removeItem(item)"
                  >删除</button>
                </div>
              </div>
              <small>{{ item.time }}</small>
            </article>
          </div>
        </van-tab>

        <van-tab title="联谊消息">
          <div class="fellowship-entry">
            <p>联谊消息在业务模块内查看，消息通知中心仅提供入口</p>
            <router-link class="platform-btn platform-btn-primary" to="/m/fellowship/messages">去看看</router-link>
          </div>
        </van-tab>

        <van-tab title="订单消息">
          <div class="fellowship-entry">
            <p>订单与会员消息将在平台会员体系上线后统一推送</p>
            <router-link class="platform-btn platform-btn-primary" to="/m/platform/member">查看平台会员</router-link>
          </div>
        </van-tab>
      </van-tabs>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getNotifications,
  getNotificationsByType,
  markAllNotifRead,
  unwrapNotificationList,
  deleteNotification,
  deleteNotificationsByType
} from '@/api/notification.js'
import { putNotificationSettings } from '@/api/notificationSettings.js'
import WechatNotifyEntryBanner from '@/components/platform/WechatNotifyEntryBanner.vue'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import { toRouterPath } from '@/utils/appPath.js'

const WEEKLY_DIGEST = 'GROUP_WEEKLY_DIGEST'

const router = useRouter()
const { groupsPath } = usePlatformPath()

const activeTab = ref(0)
const platformNotices = ref([])
const eventNotices = ref([])
const contentInteractions = ref([])
const actionHint = ref('')
const busy = ref(false)

const allMessages = computed(() => [
  ...platformNotices.value,
  ...eventNotices.value,
  ...contentInteractions.value
])

function isWeeklyDigest(item) {
  return item?.type === WEEKLY_DIGEST
}

function typeLabel(item) {
  if (isWeeklyDigest(item)) return '团体周报 · 仅管理员'
  if (item?.source === 'event') return '活动通知'
  if (item?.source === 'interaction') return '内容互动'
  return '系统通知'
}

function groupIdOf(item) {
  if (item?.relatedType === 'platform_group' && item.relatedId) return String(item.relatedId)
  const fromLink = String(item?.linkUrl || '').match(/\/groups\/(\d+)/)
  return fromLink ? fromLink[1] : ''
}

function normalizeNotif(item, index, fallbackTitle, source) {
  const created = String(item.createdAt || '')
  return {
    id: item.id || `notice-${index}`,
    type: item.type || '',
    title: item.title || fallbackTitle,
    content: item.content || '',
    time: created.slice(0, 10) || created.slice(0, 16) || '刚刚',
    linkUrl: item.linkUrl || '',
    relatedType: item.relatedType || item.targetType || '',
    relatedId: item.relatedId || item.targetId || '',
    source
  }
}

function openItem(item) {
  const groupId = groupIdOf(item)
  if (groupId) {
    router.push(groupsPath(groupId))
    return
  }
  const path = toRouterPath(item?.linkUrl)
  if (path.startsWith('/')) {
    router.push(path)
  }
}

function dropFromLists(predicate) {
  platformNotices.value = platformNotices.value.filter((row) => !predicate(row))
  eventNotices.value = eventNotices.value.filter((row) => !predicate(row))
  contentInteractions.value = contentInteractions.value.filter((row) => !predicate(row))
}

function canPersistDelete(item) {
  return item?.id != null && !String(item.id).startsWith('notice-')
}

async function removeItem(item) {
  if (!item || busy.value) return
  if (!window.confirm('确定删除这条通知？')) return
  busy.value = true
  actionHint.value = ''
  try {
    if (canPersistDelete(item)) {
      await deleteNotification(item.id)
    }
    dropFromLists((row) => row.id === item.id)
    actionHint.value = '已删除'
  } catch (error) {
    actionHint.value = error.message || '删除失败'
  } finally {
    busy.value = false
  }
}

async function unsubscribeWeeklyDigest() {
  if (busy.value) return
  if (!window.confirm('关闭后将不再收到团体周报，已收到的周报也会从列表移除。确定关闭？')) return
  busy.value = true
  actionHint.value = ''
  try {
    await putNotificationSettings([
      { type: WEEKLY_DIGEST, siteEnabled: false, wechatEnabled: false }
    ])
    await deleteNotificationsByType(WEEKLY_DIGEST)
    dropFromLists((row) => row.type === WEEKLY_DIGEST)
    actionHint.value = '已关闭团体周报，之后不会再收到'
  } catch (error) {
    actionHint.value = error.message || '关闭失败'
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  try {
    await markAllNotifRead()
  } catch (error) {
    // ignore mark-all failure and keep loading message list
  }
  window.dispatchEvent(new CustomEvent('platform-notif-read-all'))

  const [platformRes, eventRes, interactionRes] = await Promise.allSettled([
    getNotifications(30),
    getNotificationsByType('event', 20),
    getNotificationsByType('interaction', 20)
  ])

  if (platformRes.status === 'fulfilled') {
    const rows = unwrapNotificationList(platformRes.value)
    platformNotices.value = rows.map((item, i) => normalizeNotif(item, i, '平台通知', 'platform'))
  }
  if (eventRes.status === 'fulfilled') {
    const rows = unwrapNotificationList(eventRes.value)
    eventNotices.value = rows.map((item, i) => normalizeNotif(item, i, '活动提醒', 'event'))
  }
  if (interactionRes.status === 'fulfilled') {
    const rows = unwrapNotificationList(interactionRes.value)
    contentInteractions.value = rows.map((item, i) => normalizeNotif(item, i, '内容互动', 'interaction'))
  }
})
</script>

<style scoped>
.messages-page {
  display: grid;
  gap: 14px;
}

.messages-header h1 {
  margin: 0;
}

.messages-header p {
  margin: 8px 0 0;
  color: var(--lc-muted);
}

.messages-lead {
  margin: 0 0 8px;
  color: var(--lc-muted);
  font-size: 13px;
  line-height: 1.5;
}

.action-hint {
  color: var(--lc-blue) !important;
  font-size: 13px;
}

.messages-tabs {
  min-height: 380px;
}

.list-wrap {
  padding: 12px 0;
}

.empty-hint {
  margin: 24px 0;
  text-align: center;
  color: var(--lc-muted);
  font-size: 14px;
}

.message-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 2px;
  border-bottom: 1px solid var(--lc-border);
  cursor: pointer;
}

.message-main {
  min-width: 0;
  flex: 1;
}

.message-kicker {
  margin: 0 0 4px;
  font-size: 12px;
  color: var(--lc-indigo, var(--lc-blue));
  font-weight: 600;
}

.message-title {
  margin: 0;
  font-weight: 700;
}

.message-content {
  margin: 6px 0 0;
  color: var(--lc-muted);
  font-size: 14px;
}

.message-item small {
  white-space: nowrap;
  color: var(--lc-muted);
  font-size: 12px;
}

.msg-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.msg-btn {
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
  color: var(--lc-text);
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
}

.msg-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.msg-btn-danger {
  color: var(--lc-red);
  border-color: var(--lc-red);
}

.fellowship-entry {
  min-height: 220px;
  display: grid;
  place-content: center;
  gap: 12px;
  text-align: center;
}

.fellowship-entry p {
  margin: 0;
  color: var(--lc-muted);
}
</style>
