<template>
  <router-link v-if="ready && !bound" to="/platform/wechat-helper" class="wh-entry-banner">
    <span class="wh-entry-icon" aria-hidden="true">💬</span>
    <span class="wh-entry-body">
      <strong class="wh-entry-title">开启微信通知</strong>
      <span class="wh-entry-sub">保存入口 + 绑定 PushPlus，重要消息及时推送到微信</span>
    </span>
    <span class="wh-entry-arrow" aria-hidden="true">›</span>
  </router-link>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getNotificationChannelPrefs } from '@/api/notificationChannelPrefs.js'
import { isPushplusBound } from '@/utils/pushplus.js'

const ready = ref(false)
const bound = ref(false)

onMounted(async () => {
  try {
    const data = await getNotificationChannelPrefs()
    bound.value = isPushplusBound(data)
  } catch {
    bound.value = false
  } finally {
    ready.value = true
  }
})
</script>

<style scoped>
.wh-entry-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(238, 90, 143, 0.28);
  background: linear-gradient(135deg, rgba(255, 245, 250, 0.98), rgba(248, 250, 255, 0.96));
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
  text-decoration: none;
  color: inherit;
}
.wh-entry-icon {
  flex: 0 0 auto;
  font-size: 22px;
  line-height: 1;
}
.wh-entry-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.wh-entry-title {
  font-size: 14px;
  font-weight: 800;
  color: var(--lc-text-deep, #1e293b);
}
.wh-entry-sub {
  font-size: 12px;
  line-height: 1.45;
  color: var(--lc-muted-light, #64748b);
}
.wh-entry-arrow {
  flex: 0 0 auto;
  font-size: 20px;
  color: var(--lc-indigo, #4f46e5);
  font-weight: 700;
}
</style>
