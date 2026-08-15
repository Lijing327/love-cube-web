<template>
  <div v-if="showIosTip" class="ios-tip" role="dialog" aria-label="添加到主屏幕">
    <p v-if="iosTipMode === 'safari'">
      想放在主屏幕：点底部 <strong>分享</strong>，再点 <strong>添加到主屏幕</strong>。
    </p>
    <p v-else-if="iosTipMode === 'wechat'">
      微信里加不了主屏幕。请点右上角 <strong>···</strong>，选 <strong>在 Safari 中打开</strong>。
    </p>
    <p v-else>
      当前浏览器加不了主屏幕。请复制链接，到 <strong>Safari</strong> 打开后再添加。
    </p>
    <div class="ios-tip-actions">
      <button v-if="iosTipMode !== 'safari'" type="button" class="ios-tip-ok" @click="copyPageLink">
        {{ copyDone ? '已复制' : '复制链接' }}
      </button>
      <button type="button" class="ios-tip-ok" :class="{ ghost: iosTipMode !== 'safari' }" @click="dismissIosTip">知道了</button>
    </div>
  </div>
</template>

<script setup>
import { usePwa } from '@/composables/usePwa.js'

const { showIosTip, iosTipMode, copyDone, copyPageLink, dismissIosTip } = usePwa()
</script>

<style scoped>
.ios-tip {
  position: fixed;
  left: 12px;
  right: 12px;
  bottom: calc(16px + env(safe-area-inset-bottom, 0px));
  z-index: 180;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 12px 12px 14px;
  border: 1px solid var(--lc-blue-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12);
}

.ios-tip p {
  margin: 0;
  flex: 1;
  color: var(--lc-text);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.45;
}

.ios-tip strong {
  color: var(--lc-blue);
}

.ios-tip-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 6px;
}

.ios-tip-ok {
  min-height: 32px;
  padding: 0 12px;
  border: 0;
  border-radius: 999px;
  background: var(--lc-blue);
  color: var(--lc-surface);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.ios-tip-ok.ghost {
  background: var(--lc-blue-light);
  color: var(--lc-blue);
}

@media (max-width: 767px) {
  .ios-tip {
    bottom: calc(76px + env(safe-area-inset-bottom, 0px));
  }
}
</style>
