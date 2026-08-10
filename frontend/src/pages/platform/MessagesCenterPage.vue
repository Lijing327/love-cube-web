<template>
  <section class="platform-page messages-page">
    <WechatNotifyEntryBanner />

    <div class="platform-card messages-header">
      <h1>消息通知中心</h1>
      <p>统一接收平台通知、系统公告、活动通知与内容互动，并提供联谊消息入口</p>
    </div>

    <div class="platform-card">
      <van-tabs v-model:active="activeTab" class="messages-tabs">
        <van-tab title="全部">
          <div class="list-wrap">
            <article v-for="item in allMessages" :key="`all-${item.id}`" class="message-item">
              <div>
                <p class="message-title">{{ item.title }}</p>
                <p class="message-content">{{ item.content }}</p>
              </div>
              <small>{{ item.time }}</small>
            </article>
          </div>
        </van-tab>

        <van-tab title="系统通知">
          <div class="list-wrap">
            <article v-for="item in platformNotices" :key="`platform-${item.id}`" class="message-item">
              <div>
                <p class="message-title">{{ item.title }}</p>
                <p class="message-content">{{ item.content }}</p>
              </div>
              <small>{{ item.time }}</small>
            </article>
          </div>
        </van-tab>

        <van-tab title="活动通知">
          <div class="list-wrap">
            <article v-for="item in eventNotices" :key="`event-${item.id}`" class="message-item">
              <div>
                <p class="message-title">{{ item.title }}</p>
                <p class="message-content">{{ item.content }}</p>
              </div>
              <small>{{ item.time }}</small>
            </article>
          </div>
        </van-tab>

        <van-tab title="内容互动">
          <div class="list-wrap">
            <article v-for="item in contentInteractions" :key="`content-${item.id}`" class="message-item">
              <div>
                <p class="message-title">{{ item.title }}</p>
                <p class="message-content">{{ item.content }}</p>
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
import { getNotifications, getNotificationsByType, markAllNotifRead, unwrapNotificationList } from '@/api/notification.js'
import WechatNotifyEntryBanner from '@/components/platform/WechatNotifyEntryBanner.vue'

const activeTab = ref(0)
const platformNotices = ref([])
const eventNotices = ref([])
const contentInteractions = ref([])

const allMessages = computed(() => [
  ...platformNotices.value,
  ...eventNotices.value,
  ...contentInteractions.value
])

function normalizeNotif(item, index, fallbackTitle) {
  return {
    id: item.id || `notice-${index}`,
    title: item.title || fallbackTitle,
    content: item.content || '',
    time: String(item.createdAt || '').slice(0, 16) || '刚刚'
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
    platformNotices.value = rows.map((item, i) => normalizeNotif(item, i, '平台通知'))
  }
  if (eventRes.status === 'fulfilled') {
    const rows = unwrapNotificationList(eventRes.value)
    eventNotices.value = rows.map((item, i) => normalizeNotif(item, i, '活动提醒'))
  }
  if (interactionRes.status === 'fulfilled') {
    const rows = unwrapNotificationList(interactionRes.value)
    contentInteractions.value = rows.map((item, i) => normalizeNotif(item, i, '内容互动'))
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

.messages-tabs {
  min-height: 380px;
}

.list-wrap {
  padding: 12px 0;
}

.message-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 2px;
  border-bottom: 1px solid var(--lc-border);
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


