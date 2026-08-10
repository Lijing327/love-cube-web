<template>
  <section class="sp-page wh-page">
    <header class="sp-head">
      <button type="button" class="sp-back" aria-label="返回" @click="$router.back()">‹</button>
      <h1 class="sp-title">微信入口助手</h1>
    </header>

    <div class="sp-body">
      <div class="wh-hero">
        <div class="wh-hero-badge">微信助手</div>
        <h2 class="wh-hero-title">微信入口助手</h2>
        <p class="wh-hero-desc">以后从微信也能找到入口，有消息也能及时收到</p>
      </div>

      <!-- 保存入口 -->
      <div class="sp-card">
        <div class="sp-card-title">① 保存 Love Cube 入口</div>
        <p class="wh-card-hint">把链接存到微信收藏，或扫码后在浏览器打开，下次就能快速回来。</p>

        <div class="wh-url-row">
          <code class="wh-url">{{ siteUrl }}</code>
          <button type="button" class="wh-copy-btn" @click="copySiteUrl">
            {{ copyDone ? '已复制' : '复制链接' }}
          </button>
        </div>

        <div class="wh-qr-wrap">
          <img v-if="qrSrc" :src="qrSrc" alt="Love Cube 入口二维码" class="wh-qr" />
          <span v-else class="wh-qr-loading">生成二维码中…</span>
        </div>

        <ol class="wh-steps">
          <li>用微信扫上方二维码，或复制链接后在微信里打开</li>
          <li>页面打开后，点右上角 <strong>…</strong></li>
          <li>选择 <strong>收藏</strong> 或 <strong>添加到桌面</strong>，方便下次进入</li>
        </ol>
      </div>

      <!-- 开启微信通知 -->
      <div class="sp-card">
        <div class="sp-card-title">② 开启微信通知</div>
        <p class="wh-card-hint">
          PushPlus 是第三方微信消息推送服务，帮你把站内通知同步推送到微信，无需理解复杂的技术概念。
        </p>

        <ol class="wh-steps wh-steps-compact">
          <li>
            打开
            <a href="https://www.pushplus.plus/" target="_blank" rel="noopener noreferrer">pushplus.plus</a>
            ，用微信扫码登录
          </li>
          <li>进入「一对一推送」，复制你的 Token</li>
          <li>粘贴到下方，保存并发送测试通知</li>
        </ol>

        <div v-if="savedToken" class="wh-token-saved">
          已保存 Token：<strong>{{ maskedToken }}</strong>
        </div>

        <label class="wh-field">
          <span class="wh-field-label">PushPlus Token</span>
          <input
            v-model="tokenDraft"
            type="text"
            class="wh-input"
            :placeholder="savedToken ? '如需更换 Token，请在此输入' : '粘贴从 PushPlus 复制的 Token'"
            autocomplete="off"
          />
        </label>

        <p v-if="message" class="wh-msg" :class="{ error: messageError }">{{ message }}</p>

        <button type="button" class="sp-primary-btn" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存并开启微信通知' }}
        </button>
        <button type="button" class="sp-secondary-btn" :disabled="testing" @click="sendTest">
          {{ testing ? '发送中…' : '发送测试通知' }}
        </button>
      </div>

      <!-- 可接收哪些通知 -->
      <div class="sp-card">
        <div class="sp-card-title">③ 开启后可收到这些通知</div>
        <ul class="wh-notif-list">
          <li v-for="item in notifTypes" :key="item.label">
            <span class="wh-notif-icon" aria-hidden="true">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import QRCode from 'qrcode'
import {
  getNotificationChannelPrefs,
  putNotificationChannelPrefs,
  postNotificationChannelTest
} from '@/api/notificationChannelPrefs.js'
import { maskPushplusToken, resolvePlatformEntryUrl } from '@/utils/pushplus.js'

const siteUrl = ref(resolvePlatformEntryUrl())
const qrSrc = ref('')
const copyDone = ref(false)

const savedToken = ref('')
const tokenDraft = ref('')
const saving = ref(false)
const testing = ref(false)
const message = ref('')
const messageError = ref(false)

const maskedToken = computed(() => maskPushplusToken(savedToken.value))

const notifTypes = [
  { icon: '📅', label: '活动提醒' },
  { icon: '📝', label: '报名结果' },
  { icon: '💕', label: '互选成功' },
  { icon: '❤️', label: '有人喜欢' },
  { icon: '✅', label: '团体审核' },
  { icon: '⏰', label: '打卡提醒' }
]

function flash(text, isError = false) {
  message.value = text
  messageError.value = isError
  setTimeout(() => { message.value = '' }, 4000)
}

async function generateQr() {
  try {
    qrSrc.value = await QRCode.toDataURL(siteUrl.value, {
      width: 200,
      margin: 2,
      errorCorrectionLevel: 'H',
      color: { dark: '#0f172a', light: '#ffffff' }
    })
  } catch {
    qrSrc.value = ''
  }
}

async function copySiteUrl() {
  try {
    await navigator.clipboard.writeText(siteUrl.value)
    copyDone.value = true
    setTimeout(() => { copyDone.value = false }, 2000)
  } catch {
    flash('复制失败，请手动长按复制链接', true)
  }
}

function tokenForSave() {
  const draft = tokenDraft.value.trim()
  if (draft) return draft
  return savedToken.value
}

async function load() {
  try {
    const data = await getNotificationChannelPrefs()
    savedToken.value = data.pushplusToken || ''
    tokenDraft.value = ''
  } catch (e) {
    flash(e.message || '加载失败', true)
  }
}

async function save() {
  const token = tokenForSave()
  if (!token) {
    flash('请先填写 PushPlus Token', true)
    return
  }
  saving.value = true
  try {
    await putNotificationChannelPrefs({
      pushplusEnabled: true,
      pushplusToken: token
    })
    savedToken.value = token
    tokenDraft.value = ''
    flash('已保存，微信通知已开启')
  } catch (e) {
    flash(e.message || '保存失败', true)
  } finally {
    saving.value = false
  }
}

async function sendTest() {
  const token = tokenForSave()
  if (!token) {
    flash('请先填写并保存 Token', true)
    return
  }
  testing.value = true
  try {
    if (tokenDraft.value.trim() || !savedToken.value) {
      await putNotificationChannelPrefs({
        pushplusEnabled: true,
        pushplusToken: token
      })
      savedToken.value = token
      tokenDraft.value = ''
    }
    const res = await postNotificationChannelTest()
    flash(res.message || '测试通知已发送，请查看微信')
  } catch (e) {
    flash(e.message || '发送失败', true)
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  load()
  generateQr()
})
</script>

<style scoped>
.sp-page { min-height: 100vh; background: var(--lc-bg); color: var(--lc-text); }

.sp-head {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center;
  background: var(--lc-surface); border-bottom: 1px solid var(--lc-soft-alt);
}
.sp-back {
  width: 48px; height: 52px; flex: 0 0 auto;
  display: grid; place-items: center;
  border: 0; background: none; font-size: 22px; color: var(--lc-indigo); cursor: pointer;
}
.sp-title { flex: 1; margin: 0; font-size: 17px; font-weight: 800; }

.sp-body {
  padding: 16px 14px calc(80px + env(safe-area-inset-bottom));
  max-width: 480px;
  margin: 0 auto;
}
.sp-card {
  background: var(--lc-surface); border: 1px solid var(--lc-soft-alt); border-radius: 18px;
  box-shadow: 0 3px 12px rgba(15,23,42,0.04); margin-bottom: 14px; padding: 16px;
}
.sp-card-title { font-size: 15px; font-weight: 800; margin-bottom: 10px; color: var(--lc-text); }

.wh-hero {
  margin-bottom: 16px;
  padding: 20px 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.12), rgba(238, 90, 143, 0.1));
  border: 1px solid rgba(79, 70, 229, 0.15);
  text-align: center;
}
.wh-hero-badge {
  display: inline-block;
  margin-bottom: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  font-size: 11px;
  font-weight: 700;
  color: var(--lc-indigo);
}
.wh-hero-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 900;
  color: var(--lc-text-deep);
}
.wh-hero-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
  color: var(--lc-muted-light);
}

.wh-card-hint {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--lc-text-secondary, #646566);
}
.wh-card-hint a {
  color: var(--lc-blue, #1989fa);
}

.wh-url-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}
.wh-url {
  display: block;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--lc-bg);
  border: 1px solid var(--lc-soft-alt);
  font-size: 12px;
  word-break: break-all;
  color: var(--lc-text);
}
.wh-copy-btn {
  align-self: flex-start;
  padding: 8px 16px;
  border: 1px solid var(--lc-indigo);
  border-radius: 999px;
  background: var(--lc-surface);
  color: var(--lc-indigo);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.wh-qr-wrap {
  display: grid;
  place-items: center;
  margin: 0 auto 16px;
  width: 200px;
  height: 200px;
  border-radius: 14px;
  border: 1px solid var(--lc-soft-alt);
  background: #fff;
}
.wh-qr {
  width: 180px;
  height: 180px;
  object-fit: contain;
}
.wh-qr-loading {
  font-size: 12px;
  color: var(--lc-muted-light);
}

.wh-steps {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--lc-text-secondary);
}
.wh-steps-compact {
  margin-bottom: 14px;
}
.wh-steps a {
  color: var(--lc-blue, #1989fa);
}

.wh-token-saved {
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--lc-green-light, rgba(7, 193, 96, 0.08));
  font-size: 13px;
  color: var(--lc-text);
}
.wh-token-saved strong {
  font-family: ui-monospace, monospace;
  letter-spacing: 0.02em;
}

.wh-field { display: block; margin-bottom: 12px; }
.wh-field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--lc-text-secondary);
}
.wh-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid var(--lc-border, #ebedf0);
  border-radius: 8px;
  font-size: 14px;
}

.wh-msg {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--lc-green, #07c160);
  text-align: center;
}
.wh-msg.error { color: var(--lc-red, #ee0a24); }

.sp-primary-btn {
  display: block;
  width: 100%;
  padding: 14px;
  border: 0;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--lc-violet), var(--lc-indigo));
  color: var(--lc-surface);
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.25);
}
.sp-primary-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.sp-secondary-btn {
  display: block;
  width: 100%;
  margin-top: 10px;
  padding: 12px;
  border: 1px solid var(--lc-border, #ebedf0);
  border-radius: 10px;
  background: #fff;
  font-size: 15px;
  color: var(--lc-text, #323233);
  cursor: pointer;
}
.sp-secondary-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.wh-notif-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.wh-notif-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--lc-bg);
  font-size: 13px;
  font-weight: 600;
  color: var(--lc-text);
}
.wh-notif-icon { font-size: 16px; line-height: 1; }

@media (max-width: 767px) {
  :global(.platform-header), :global(.platform-footer), :global(.co-creation-toolbar) {
    display: none !important;
  }
}
@media (min-width: 768px) {
  .sp-body { padding-top: 24px; padding-bottom: 48px; }
}
</style>
