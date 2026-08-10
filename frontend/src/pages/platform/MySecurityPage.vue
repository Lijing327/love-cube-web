<template>
  <section class="sp-page">
    <header class="sp-head">
      <button type="button" class="sp-back" aria-label="返回" @click="$router.back()">‹</button>
      <h1 class="sp-title">账号安全</h1>
    </header>

    <div class="sp-body">
      <!-- 安全评分 -->
      <div class="sp-card sec-score-card">
        <div class="sec-score-label">安全等级</div>
        <div class="sec-score" :class="secScoreClass">{{ secScoreLabel }}</div>
        <div class="sec-score-hint">{{ secScoreHint }}</div>
      </div>

      <!-- 账号信息 -->
      <div class="sp-card">
        <div class="sp-card-title">账号信息</div>
        <div class="sec-row">
          <div class="sec-row-left">
            <span class="sec-icon">📱</span>
            <div>
              <div class="sec-row-name">手机号</div>
              <div class="sec-row-sub">{{ user?.phone ? maskPhone(user.phone) : '未绑定' }}</div>
            </div>
          </div>
          <button type="button" class="sec-action-btn" @click="goChangePhone">{{ user?.phone ? '换绑' : '绑定' }}</button>
        </div>
        <div class="sec-row">
          <div class="sec-row-left">
            <span class="sec-icon">🔑</span>
            <div>
              <div class="sec-row-name">登录密码</div>
              <div class="sec-row-sub">定期更换密码可提升安全性</div>
            </div>
          </div>
          <button type="button" class="sec-action-btn" @click="goChangePwd">修改</button>
        </div>
        <div class="sec-row">
          <div class="sec-row-left">
            <span class="sec-icon">✉️</span>
            <div>
              <div class="sec-row-name">邮箱</div>
              <div class="sec-row-sub">{{ user?.email ? maskEmail(user.email) : '未绑定' }}</div>
            </div>
          </div>
          <button type="button" class="sec-action-btn" @click="openEmailPanel">{{ user?.email ? '换绑' : '设置' }}</button>
        </div>
        <div class="sec-row no-border">
          <div class="sec-row-left">
            <span class="sec-icon">🪪</span>
            <div>
              <div class="sec-row-name">实名认证</div>
              <div class="sec-row-sub">{{ verifyLabel }}</div>
            </div>
          </div>
          <router-link to="/me/verify" class="sec-action-btn">{{ verifyAction }}</router-link>
        </div>
      </div>

      <!-- 第三方账号 -->
      <div class="sp-card">
        <div class="sp-card-title">第三方账号</div>
        <p class="sec-card-hint">绑定微信后可接收微信通知；当前为模拟绑定，正式授权接入后将替换。</p>
        <div class="sec-row">
          <div class="sec-row-left">
            <span class="sec-icon">💬</span>
            <div>
              <div class="sec-row-name">微信</div>
              <div class="sec-row-sub">
                {{ wechatOfficial ? `${wechatOfficial.nickname || '已绑定'}（模拟）` : '未绑定' }}
              </div>
            </div>
          </div>
          <button
            v-if="!wechatOfficial"
            type="button"
            class="sec-action-btn"
            :disabled="bindingWechat"
            @click="bindWechat"
          >
            {{ bindingWechat ? '绑定中…' : '绑定' }}
          </button>
          <button
            v-else
            type="button"
            class="sec-action-btn danger"
            :disabled="bindingWechat"
            @click="unbindWechat"
          >
            {{ bindingWechat ? '处理中…' : '解绑' }}
          </button>
        </div>
        <div class="sec-row no-border">
          <div class="sec-row-left">
            <span class="sec-icon">🐧</span>
            <div>
              <div class="sec-row-name">QQ</div>
              <div class="sec-row-sub">暂未开放</div>
            </div>
          </div>
          <button type="button" class="sec-action-btn" disabled>敬请期待</button>
        </div>
      </div>

      <!-- 安全建议 -->
      <div class="sp-card">
        <div class="sp-card-title">安全建议</div>
        <div v-for="tip in secTips" :key="tip.text" class="tip-row">
          <span class="tip-icon" :class="tip.ok ? 'ok' : 'warn'">{{ tip.ok ? '✓' : '!' }}</span>
          <span class="tip-text">{{ tip.text }}</span>
        </div>
      </div>

      <!-- 注册信息 -->
      <div class="sp-card">
        <div class="sec-info-row">
          <span>账号 ID</span><strong>{{ userIdDisplay }}</strong>
        </div>
        <div class="sec-info-row">
          <span>注册时间</span><strong>{{ registerDate }}</strong>
        </div>
        <div class="sec-info-row no-border">
          <span>当前状态</span><strong style="color:#059669">正常使用中</strong>
        </div>
      </div>
    </div>

    <!-- 邮箱绑定弹层 -->
    <div v-if="emailPanelOpen" class="sec-overlay" @click.self="closeEmailPanel">
      <div class="sec-dialog" role="dialog" aria-labelledby="email-dialog-title">
        <h2 id="email-dialog-title" class="sec-dialog-title">{{ user?.email ? '换绑邮箱' : '绑定邮箱' }}</h2>
        <p class="sec-dialog-hint">绑定后可使用邮箱登录，并接收邮件通知。</p>
        <label class="sec-field">
          <span>登录密码</span>
          <input v-model.trim="emailForm.password" type="password" placeholder="请输入当前登录密码" autocomplete="current-password">
        </label>
        <label class="sec-field">
          <span>邮箱地址</span>
          <input v-model.trim="emailForm.newEmail" type="email" placeholder="name@example.com" autocomplete="email">
        </label>
        <label class="sec-field">
          <span>确认邮箱</span>
          <input v-model.trim="emailForm.confirmEmail" type="email" placeholder="请再次输入邮箱" autocomplete="email">
        </label>
        <p v-if="emailMessage" class="sec-dialog-msg" :class="{ error: emailError }">{{ emailMessage }}</p>
        <div class="sec-dialog-actions">
          <button type="button" class="sec-dialog-btn ghost" @click="closeEmailPanel">取消</button>
          <button type="button" class="sec-dialog-btn primary" :disabled="emailSubmitting" @click="submitEmail">
            {{ emailSubmitting ? '提交中…' : '确认' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changeEmail } from '@/api/auth.js'
import { getSocialBindings, mockBindWechatOfficial, deleteSocialBinding } from '@/api/socialBindings.js'
import { clearMeCache } from '@/api/user.js'
import { useUserStore } from '@/stores/user.js'

const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.userInfo)

const bindings = ref([])
const bindingWechat = ref(false)
const emailPanelOpen = ref(false)
const emailSubmitting = ref(false)
const emailMessage = ref('')
const emailError = ref(false)
const emailForm = reactive({
  password: '',
  newEmail: '',
  confirmEmail: ''
})

const wechatOfficial = computed(() =>
  bindings.value.find((b) => b.provider === 'WECHAT_OFFICIAL' && b.bindStatus === 'BOUND')
)

const userIdDisplay = computed(() => user.value?.id || user.value?.userId || '--')
const verifyLabel = computed(() => {
  const s = String(user.value?.verificationStatus || 'none')
  if (s === 'approved') return '已认证'
  if (s === 'pending') return '认证审核中'
  return '未认证'
})
const verifyAction = computed(() => {
  const s = String(user.value?.verificationStatus || 'none')
  if (s === 'approved') return '查看'
  if (s === 'pending') return '查看进度'
  return '去认证'
})
const registerDate = computed(() => {
  const raw = user.value?.createdAt
  if (!raw) return '--'
  return String(raw).replace('T', ' ').slice(0, 10)
})

const secScore = computed(() => {
  let score = 0
  if (user.value?.phone) score++
  if (user.value?.email) score++
  if (user.value?.verificationStatus === 'approved') score++
  return Math.min(score, 2)
})

const secScoreLabel = computed(() => ['待提升', '一般', '良好'][secScore.value] || '待提升')
const secScoreClass = computed(() => ['warn', 'medium', 'ok'][secScore.value] || 'warn')
const secScoreHint = computed(() => {
  if (secScore.value === 0) return '建议绑定手机号、邮箱并完成实名认证'
  if (secScore.value === 1) return '再完成一步即可达到良好安全等级'
  return '账号安全状态良好，请继续保持'
})

const secTips = computed(() => [
  { text: '已绑定手机号', ok: Boolean(user.value?.phone) },
  { text: '已绑定邮箱', ok: Boolean(user.value?.email) },
  { text: '已完成实名认证', ok: user.value?.verificationStatus === 'approved' },
  { text: '定期更换密码', ok: false }
])

function maskPhone(phone) {
  const s = String(phone)
  if (s.length < 7) return s
  return s.slice(0, 3) + '****' + s.slice(-4)
}

function maskEmail(email) {
  const s = String(email).trim()
  const at = s.indexOf('@')
  if (at <= 1) return '***' + s.slice(Math.max(0, at))
  return s.charAt(0) + '***' + s.slice(at)
}

function goChangePwd() {
  router.push('/fellowship/change-password')
}

function goChangePhone() {
  router.push('/fellowship/change-phone')
}

function openEmailPanel() {
  emailForm.password = ''
  emailForm.newEmail = ''
  emailForm.confirmEmail = ''
  emailMessage.value = ''
  emailError.value = false
  emailPanelOpen.value = true
}

function closeEmailPanel() {
  if (emailSubmitting.value) return
  emailPanelOpen.value = false
}

async function submitEmail() {
  emailMessage.value = ''
  emailError.value = false

  if (!emailForm.password) {
    emailMessage.value = '请输入登录密码'
    emailError.value = true
    return
  }
  if (!emailForm.newEmail) {
    emailMessage.value = '请输入邮箱地址'
    emailError.value = true
    return
  }
  if (!EMAIL_PATTERN.test(emailForm.newEmail)) {
    emailMessage.value = '请输入有效的邮箱地址'
    emailError.value = true
    return
  }
  if (!emailForm.confirmEmail) {
    emailMessage.value = '请再次输入邮箱'
    emailError.value = true
    return
  }
  if (emailForm.newEmail !== emailForm.confirmEmail) {
    emailMessage.value = '两次输入的邮箱不一致'
    emailError.value = true
    return
  }

  emailSubmitting.value = true
  try {
    await changeEmail({
      password: emailForm.password,
      newEmail: emailForm.newEmail,
      confirmEmail: emailForm.confirmEmail
    })
    clearMeCache()
    await userStore.refreshCurrentUser()
    emailPanelOpen.value = false
  } catch (error) {
    emailMessage.value = error?.message || '邮箱绑定失败，请稍后重试'
    emailError.value = true
  } finally {
    emailSubmitting.value = false
  }
}

async function loadBindings() {
  try {
    bindings.value = await getSocialBindings()
  } catch {
    bindings.value = []
  }
}

async function bindWechat() {
  bindingWechat.value = true
  try {
    await mockBindWechatOfficial()
    await loadBindings()
  } catch (error) {
    window.alert(error?.message || '微信绑定失败，请稍后重试')
  } finally {
    bindingWechat.value = false
  }
}

async function unbindWechat() {
  if (!wechatOfficial.value) return
  if (!window.confirm('解绑后将关闭所有微信通知开关，确定继续？')) return
  bindingWechat.value = true
  try {
    await deleteSocialBinding(wechatOfficial.value.id)
    await loadBindings()
  } catch (error) {
    window.alert(error?.message || '解绑失败，请稍后重试')
  } finally {
    bindingWechat.value = false
  }
}

onMounted(async () => {
  if (!user.value) await userStore.refreshCurrentUser().catch(() => {})
  await loadBindings()
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
  max-width: 680px; margin: 0 auto;
}
.sp-card {
  background: var(--lc-surface); border: 1px solid var(--lc-soft-alt); border-radius: 18px;
  box-shadow: 0 3px 12px rgba(15,23,42,0.04); margin-bottom: 14px; padding: 16px;
}
.sp-card-title { font-size: 15px; font-weight: 800; margin-bottom: 14px; color: var(--lc-text); }
.sec-card-hint {
  margin: -6px 0 12px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--lc-muted-light);
}

.sec-score-card { text-align: center; padding: 20px 16px; }
.sec-score-label { font-size: 12px; color: var(--lc-subtle); margin-bottom: 8px; }
.sec-score {
  display: inline-block; font-size: 22px; font-weight: 900;
  padding: 6px 20px; border-radius: 999px; margin-bottom: 8px;
}
.sec-score.ok { background: var(--lc-green-light); color: var(--lc-emerald); }
.sec-score.medium { background: var(--lc-amber-light); color: var(--lc-amber); }
.sec-score.warn { background: var(--lc-red-light); color: var(--lc-red); }
.sec-score-hint { font-size: 12px; color: var(--lc-muted-light); }

.sec-row {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; padding: 13px 0; border-bottom: 1px solid var(--lc-soft);
}
.sec-row.no-border { border-bottom: 0; padding-bottom: 0; }
.sec-row-left { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.sec-icon { font-size: 20px; width: 28px; text-align: center; flex: 0 0 auto; }
.sec-row-name { font-size: 14px; font-weight: 600; color: var(--lc-text-deep); }
.sec-row-sub { font-size: 12px; color: var(--lc-subtle); margin-top: 2px; }
.sec-action-btn {
  flex: 0 0 auto; border: 1px solid var(--lc-soft-alt); border-radius: 999px;
  background: var(--lc-surface); color: var(--lc-indigo);
  font-size: 13px; font-weight: 700; padding: 5px 14px; cursor: pointer;
  text-decoration: none;
}
.sec-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  color: var(--lc-muted-light);
}
.sec-action-btn.danger {
  color: var(--lc-red);
  border-color: var(--lc-red-light);
}

.tip-row {
  display: flex; align-items: center; gap: 10px; padding: 10px 0;
  border-bottom: 1px solid var(--lc-soft);
}
.tip-row:last-child { border-bottom: 0; padding-bottom: 0; }
.tip-icon {
  width: 22px; height: 22px; flex: 0 0 auto;
  display: grid; place-items: center; border-radius: 50%;
  font-size: 12px; font-weight: 800;
}
.tip-icon.ok { background: var(--lc-green-light); color: var(--lc-emerald); }
.tip-icon.warn { background: var(--lc-amber-light); color: var(--lc-amber); }
.tip-text { font-size: 13px; color: var(--lc-slate); }

.sec-info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 11px 0; border-bottom: 1px solid var(--lc-soft); font-size: 13px; color: var(--lc-muted-light);
}
.sec-info-row.no-border { border-bottom: 0; padding-bottom: 0; }
.sec-info-row strong { color: var(--lc-text-deep); }

.sec-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.45);
}
.sec-dialog {
  width: min(100%, 420px);
  padding: 22px 20px 18px;
  border-radius: 18px;
  background: var(--lc-surface);
  border: 1px solid var(--lc-soft-alt);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.18);
}
.sec-dialog-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 800;
  color: var(--lc-text-deep);
}
.sec-dialog-hint {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--lc-muted-light);
}
.sec-field {
  display: block;
  margin-bottom: 12px;
}
.sec-field span {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--lc-slate);
}
.sec-field input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid var(--lc-soft-alt);
  border-radius: 12px;
  font-size: 14px;
  color: var(--lc-text-deep);
  background: var(--lc-bg);
}
.sec-field input:focus {
  outline: none;
  border-color: var(--lc-indigo);
}
.sec-dialog-msg {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--lc-emerald);
}
.sec-dialog-msg.error {
  color: var(--lc-red);
}
.sec-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
.sec-dialog-btn {
  border: 0;
  border-radius: 999px;
  padding: 9px 18px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
.sec-dialog-btn.ghost {
  background: var(--lc-soft);
  color: var(--lc-slate);
}
.sec-dialog-btn.primary {
  background: var(--lc-indigo);
  color: #fff;
}
.sec-dialog-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 767px) {
  :global(.platform-header), :global(.platform-footer), :global(.co-creation-toolbar) {
    display: none !important;
  }
}
@media (min-width: 768px) {
  .sp-body { padding-top: 24px; padding-bottom: 48px; }
}
</style>
