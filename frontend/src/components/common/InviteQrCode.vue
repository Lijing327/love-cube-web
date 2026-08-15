<template>
  <div class="invite-qr-root">
    <div
      class="invite-qr-frame"
      role="button"
      tabindex="0"
      aria-label="点击放大邀请码"
      @click.stop="openPreview"
      @keydown.enter.prevent="openPreview"
      @keydown.space.prevent="openPreview"
    >
      <span class="corner corner-tl" aria-hidden="true"></span>
      <span class="corner corner-tr" aria-hidden="true"></span>
      <span class="corner corner-bl" aria-hidden="true"></span>
      <span class="corner corner-br" aria-hidden="true"></span>
      <div class="invite-qr">
        <img v-if="qrSrc" :src="qrSrc" alt="邀请二维码">
        <span v-else>{{ loadingText }}</span>
      </div>
      <span class="qr-zoom-hint">放大</span>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import QRCode from 'qrcode'
import { createInviteUrl } from '@/utils/inviteUrl.js'

const props = defineProps({
  inviteCode: { type: String, default: '' },
  size: { type: Number, default: 168 },
  loadingText: { type: String, default: '生成中...' }
})

const qrSrc = ref('')
const previewOpen = ref(false)
let previewEl = null

watch(
  () => [props.inviteCode, props.size],
  async ([inviteCode, size]) => {
    const value = createInviteUrl(inviteCode)
    if (!value) {
      qrSrc.value = ''
      return
    }
    qrSrc.value = await QRCode.toDataURL(value, {
      width: Math.max(Number(size) || 168, 360),
      margin: 2,
      errorCorrectionLevel: 'H',
      color: {
        dark: '#0f172a',
        light: '#ffffff'
      }
    })
  },
  { immediate: true }
)

function openPreview() {
  if (!qrSrc.value || typeof document === 'undefined') return
  closePreview()
  const mask = document.createElement('div')
  mask.className = 'lc-qr-preview-mask'
  mask.innerHTML = `
    <section class="lc-qr-preview-dialog" role="dialog" aria-modal="true" aria-label="邀请二维码预览">
      <p class="lc-qr-preview-hint">用另一部手机对准扫描</p>
      <img alt="邀请二维码大图">
      <button type="button" class="lc-qr-preview-close">关闭</button>
    </section>
  `
  const img = mask.querySelector('img')
  if (img) img.src = qrSrc.value
  mask.addEventListener('click', (event) => {
    if (event.target === mask) closePreview()
  })
  mask.querySelector('.lc-qr-preview-close')?.addEventListener('click', closePreview)
  document.body.appendChild(mask)
  previewEl = mask
  previewOpen.value = true
  window.addEventListener('keydown', onKeydown)
}

function closePreview() {
  previewOpen.value = false
  window.removeEventListener('keydown', onKeydown)
  if (previewEl?.parentNode) previewEl.parentNode.removeChild(previewEl)
  previewEl = null
}

function onKeydown(event) {
  if (event.key === 'Escape') closePreview()
}

defineExpose({ openPreview })

onBeforeUnmount(() => {
  closePreview()
})
</script>

<style scoped>
.invite-qr-root {
  width: 100%;
}

.invite-qr-frame {
  position: relative;
  display: grid;
  place-items: center;
  width: 100%;
  aspect-ratio: 1;
  border: 1px solid var(--lc-blue-border);
  border-radius: 8px;
  background: var(--lc-surface);
  padding: 8px;
  cursor: pointer;
}

.invite-qr {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 100%;
  aspect-ratio: 1;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
  color: var(--lc-muted);
  font-size: 12px;
  pointer-events: none;
}

.invite-qr img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  pointer-events: none;
}

.qr-zoom-hint {
  position: absolute;
  right: 4px;
  bottom: 4px;
  z-index: 3;
  padding: 1px 5px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  line-height: 1.3;
  pointer-events: none;
}

.corner {
  position: absolute;
  z-index: 2;
  width: 14px;
  height: 14px;
  border-color: var(--lc-blue);
  pointer-events: none;
}

.corner-tl {
  top: 5px;
  left: 5px;
  border-top: 2px solid;
  border-left: 2px solid;
}

.corner-tr {
  top: 5px;
  right: 5px;
  border-top: 2px solid;
  border-right: 2px solid;
}

.corner-bl {
  bottom: 5px;
  left: 5px;
  border-bottom: 2px solid;
  border-left: 2px solid;
}

.corner-br {
  right: 5px;
  bottom: 5px;
  border-right: 2px solid;
  border-bottom: 2px solid;
}
</style>

<style>
.lc-qr-preview-mask {
  position: fixed;
  inset: 0;
  z-index: 4000;
  display: grid;
  place-items: center;
  padding: 24px 16px;
  background: rgba(15, 23, 42, 0.58);
}

.lc-qr-preview-dialog {
  display: grid;
  justify-items: center;
  gap: 12px;
  width: min(100%, 320px);
  padding: 20px 16px 16px;
  border-radius: 16px;
  background: #fff;
}

.lc-qr-preview-hint {
  margin: 0;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.lc-qr-preview-dialog img {
  display: block;
  width: min(72vw, 260px);
  height: auto;
  background: #fff;
}

.lc-qr-preview-close {
  width: 100%;
  height: 40px;
  border: 0;
  border-radius: 10px;
  background: #2563eb;
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}
</style>
