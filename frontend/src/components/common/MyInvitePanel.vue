<template>
  <section class="my-invite-panel" :class="{ compact }" aria-label="我的邀请">
    <div class="invite-main">
      <div class="invite-copy">
        <p class="eyebrow">我的邀请</p>
        <h2>{{ inviteCode || '加载中...' }}</h2>
        <p class="count">已邀请 {{ inviteCount }} 人</p>
      </div>
      <div class="qr-wrap" @click.stop="openQrPreview">
        <InviteQrCode ref="qrRef" :invite-code="inviteCode" :size="196" />
      </div>
    </div>

    <label class="link-row">
      <span>邀请链接</span>
      <input :value="resolvedInviteLink || '生成中...'" readonly>
    </label>

    <div class="actions">
      <button type="button" :disabled="!resolvedInviteLink" @click="copyLink">复制邀请链接</button>
      <slot name="extra" />
    </div>
    <p v-if="feedback" class="feedback" :class="{ 'is-error': feedbackError }">{{ feedback }}</p>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import InviteQrCode from './InviteQrCode.vue'
import { createInviteUrl } from '@/utils/inviteUrl.js'

const props = defineProps({
  inviteCode: { type: String, default: '' },
  inviteCount: { type: Number, default: 0 },
  compact: { type: Boolean, default: false }
})

const resolvedInviteLink = computed(() => createInviteUrl(props.inviteCode))
const feedback = ref('')
const feedbackError = ref(false)
const qrRef = ref(null)

function openQrPreview() {
  qrRef.value?.openPreview?.()
}

async function copyLink() {
  if (!resolvedInviteLink.value) return
  feedback.value = ''
  feedbackError.value = false
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(resolvedInviteLink.value)
    } else {
      const input = document.createElement('input')
      input.value = resolvedInviteLink.value
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    feedback.value = '邀请链接已复制'
  } catch {
    feedback.value = '复制失败，请手动复制'
    feedbackError.value = true
  }
  window.setTimeout(() => {
    feedback.value = ''
  }, 2200)
}
</script>

<style scoped>
.my-invite-panel {
  position: relative;
  display: grid;
  gap: 16px;
  border: 1px solid rgba(99, 102, 241, 0.14);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.94)),
    linear-gradient(135deg, rgba(236, 72, 153, 0.08), rgba(59, 130, 246, 0.08));
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  overflow: hidden;
  padding: 18px;
}

.invite-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 148px;
  gap: 18px;
  align-items: center;
}

.invite-copy {
  min-width: 0;
}

.eyebrow,
.count,
.feedback {
  margin: 0;
  color: var(--lc-muted);
  font-size: 12px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  border: 1px solid rgba(99, 102, 241, 0.16);
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.08);
  color: var(--lc-indigo);
  font-weight: 900;
  padding: 0 10px;
}

h2 {
  margin: 10px 0 7px;
  color: var(--lc-text);
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.12;
  word-break: break-all;
}

.count {
  color: var(--lc-text);
  font-weight: 800;
}

.qr-wrap {
  width: 148px;
  cursor: pointer;
}

.link-row {
  display: grid;
  gap: 8px;
  color: var(--lc-muted);
  font-size: 12px;
  font-weight: 700;
}

.link-row input {
  width: 100%;
  border: 1px solid rgba(203, 213, 225, 0.86);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.04);
  color: var(--lc-text);
  font: inherit;
  outline: 0;
  padding: 11px 12px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.actions button {
  border: 0;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--lc-indigo), #2563eb);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.22);
  color: var(--lc-surface);
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
  padding: 11px 15px;
  transition: transform 0.15s ease, box-shadow 0.15s ease, opacity 0.15s ease;
}

.actions button:not(:disabled):hover {
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.26);
  transform: translateY(-1px);
}

.actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.feedback {
  color: var(--lc-emerald);
}

.feedback.is-error {
  color: var(--lc-red);
}

@media (max-width: 767px) {
  .my-invite-panel {
    gap: 8px;
    padding: 10px;
    border-radius: 10px;
    border: 1px solid var(--lc-border);
    background: var(--lc-surface);
    box-shadow: none;
  }

  .invite-main {
    grid-template-columns: minmax(0, 1fr) 72px;
    gap: 8px;
  }

  h2 {
    margin: 4px 0 2px;
    font-size: 15px;
    font-weight: 800;
  }

  .qr-wrap {
    width: 72px;
  }

  .qr-wrap :deep(img) {
    width: 100%;
    height: auto;
  }

  .link-row input {
    height: 34px;
    padding: 0 10px;
  }

  .actions button {
    width: 100%;
    height: 34px;
    padding: 0;
    box-shadow: none;
    background: var(--lc-blue);
  }

  .my-invite-panel.compact {
    gap: 0;
  }

  .my-invite-panel.compact .link-row {
    display: none;
  }

  .my-invite-panel.compact .actions {
    margin-top: 8px;
  }

  .my-invite-panel.compact .actions button {
    height: 32px;
    font-size: 12px;
  }
}
</style>
