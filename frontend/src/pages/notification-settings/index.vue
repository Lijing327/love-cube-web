<template>
  <div class="notif-settings-page">
    <van-nav-bar title="通知设置" left-arrow fixed placeholder @click-left="router.back()" />

    <section class="section">
      <h3 class="section-title">邮件与 PushPlus</h3>
      <p class="hint">
        开启后，站内消息将同步投递至邮件或 PushPlus（微信）。PushPlus 需在 pushplus.plus 获取 Token；不接微信公众号/服务号/企业微信。
      </p>
      <van-cell-group inset>
        <van-cell title="邮件通知">
          <template #label>
            <div class="switch-row">
              <span>{{ channelPrefs.hasEmail ? `发送至 ${channelPrefs.emailMasked}` : '请先在账号中绑定邮箱' }}</span>
              <van-switch
                :model-value="channelPrefs.emailEnabled"
                size="20"
                :disabled="!channelPrefs.hasEmail"
                @update:model-value="(v) => (channelPrefs.emailEnabled = v)"
              />
            </div>
          </template>
        </van-cell>
        <van-cell title="PushPlus 微信推送">
          <template #label>
            <div class="switch-row">
              <span>通过 PushPlus 推送到微信</span>
              <van-switch
                :model-value="channelPrefs.pushplusEnabled"
                size="20"
                @update:model-value="(v) => (channelPrefs.pushplusEnabled = v)"
              />
            </div>
            <van-field
              v-model="channelPrefs.pushplusToken"
              label="Token"
              placeholder="在 pushplus.plus 个人中心复制"
              class="token-field"
            />
          </template>
        </van-cell>
        <van-cell>
          <template #title>
            <van-button type="primary" size="small" block plain :loading="testingChannel" @click="sendChannelTest">
              发送测试通知
            </van-button>
          </template>
        </van-cell>
      </van-cell-group>
    </section>

    <section class="section">
      <h3 class="section-title">微信绑定</h3>
      <p class="bind-hint">
        当前为<strong>模拟绑定</strong>，仅用于联调占位；后续接入微信公众号后，可在此完成正式授权并接收模板消息（本阶段后端不接真实微信接口）。
      </p>
      <van-cell-group inset>
        <van-cell v-if="!wechatOfficial" title="微信公众号" value="未绑定 · 微信推送不可用" />
        <van-cell v-else title="微信公众号" :value="(wechatOfficial.nickname || '已绑定') + '（模拟）'" />
        <van-cell>
          <template #title>
            <van-button v-if="!wechatOfficial" type="primary" size="small" block :loading="binding" @click="mockBind">
              模拟绑定微信（联调）
            </van-button>
            <van-button v-else type="danger" size="small" block plain :loading="binding" @click="unbindWechat">
              解绑微信
            </van-button>
          </template>
        </van-cell>
      </van-cell-group>
    </section>

    <section class="section">
      <h3 class="section-title">通知渠道</h3>
      <p class="hint">可按类型分别开启站内消息与微信占位推送。未绑定微信时，「微信」开关无法打开；解绑后将自动关闭所有微信通知开关。</p>
      <van-cell-group inset>
        <template v-for="row in settings" :key="row.type">
          <van-cell :title="labelFor(row.type)">
            <template #label>
              <div class="switch-row">
                <span>站内</span>
                <van-switch :model-value="row.siteEnabled" size="20" @update:model-value="(v) => patch(row, 'siteEnabled', v)" />
              </div>
              <div class="switch-row">
                <span>微信</span>
                <van-switch
                  :model-value="row.wechatEnabled"
                  size="20"
                  @update:model-value="(v) => onWechatToggle(row, v)"
                />
              </div>
            </template>
          </van-cell>
        </template>
      </van-cell-group>
    </section>

    <div class="footer-actions">
      <van-button type="primary" block round :loading="saving" @click="saveAll">保存设置</van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { getNotificationSettings, putNotificationSettings } from '@/api/notificationSettings.js'
import {
  getNotificationChannelPrefs,
  putNotificationChannelPrefs,
  postNotificationChannelTest
} from '@/api/notificationChannelPrefs.js'
import { getSocialBindings, mockBindWechatOfficial, deleteSocialBinding } from '@/api/socialBindings.js'

const router = useRouter()

const LABELS = {
  GROUP_APPLICATION_APPROVED: '团体申请通过',
  GROUP_APPLICATION_REJECTED: '团体申请未通过',
  GROUP_JOIN_REQUEST: '有人申请加入你管理的团体',
  GROUP_WEEKLY_DIGEST: '团体每周经营周报（管理员）',
  CONTENT_MODERATION_PASSED: '内容审核通过',
  CONTENT_MODERATION_REJECTED: '内容审核未通过',
  CONTENT_COMMENTED: '有人评论你的内容',
  PLATFORM_ANNOUNCEMENT: '平台重要公告',
  MATCH_PROFILE_REVIEW_PASSED: '联谊资料审核通过',
  MATCH_PROFILE_REVIEW_REJECTED: '联谊资料审核未通过',
  PROFILE_LIKED: '有人喜欢了你',
  PROFILE_VIEWED: '有人浏览了你',
  CONTENT_LIKED: '有人点赞了你',
  USER_FOLLOWED: '有人关注了你'
}

const settings = ref([])
const bindings = ref([])
const channelPrefs = ref({
  emailEnabled: false,
  pushplusEnabled: false,
  pushplusToken: '',
  hasEmail: false,
  emailMasked: ''
})
const saving = ref(false)
const binding = ref(false)
const testingChannel = ref(false)

const wechatOfficial = computed(() =>
  bindings.value.find((b) => b.provider === 'WECHAT_OFFICIAL' && b.bindStatus === 'BOUND')
)

function labelFor(type) {
  return LABELS[type] || type
}

function patch(row, key, val) {
  row[key] = val
}

function onWechatToggle(row, v) {
  if (v && !wechatOfficial.value) {
    showToast({ type: 'fail', message: '请先绑定微信' })
    return
  }
  row.wechatEnabled = v
}

async function load() {
  try {
    const [s, b, ch] = await Promise.all([
      getNotificationSettings(),
      getSocialBindings(),
      getNotificationChannelPrefs()
    ])
    settings.value = Array.isArray(s) ? s.map((x) => ({ ...x })) : []
    bindings.value = Array.isArray(b) ? b : []
    channelPrefs.value = {
      emailEnabled: !!ch.emailEnabled,
      pushplusEnabled: !!ch.pushplusEnabled,
      pushplusToken: ch.pushplusToken || '',
      hasEmail: !!ch.hasEmail,
      emailMasked: ch.emailMasked || ''
    }
  } catch (e) {
    showToast({ type: 'fail', message: e.message || '加载失败' })
  }
}

async function saveAll() {
  if (channelPrefs.value.pushplusEnabled && !channelPrefs.value.pushplusToken.trim()) {
    showToast({ type: 'fail', message: '开启 PushPlus 前请填写 Token' })
    return
  }
  saving.value = true
  try {
    await Promise.all([
      putNotificationSettings(
        settings.value.map((r) => ({
          type: r.type,
          siteEnabled: !!r.siteEnabled,
          wechatEnabled: !!r.wechatEnabled
        }))
      ),
      putNotificationChannelPrefs({
        emailEnabled: !!channelPrefs.value.emailEnabled,
        pushplusEnabled: !!channelPrefs.value.pushplusEnabled,
        pushplusToken: channelPrefs.value.pushplusToken.trim()
      })
    ])
    showToast({ type: 'success', message: '已保存' })
  } catch (e) {
    showToast({ type: 'fail', message: e.message || '保存失败' })
  } finally {
    saving.value = false
  }
}

async function sendChannelTest() {
  testingChannel.value = true
  try {
    const res = await postNotificationChannelTest()
    showToast({ type: 'success', message: res.message || '测试通知已发送' })
  } catch (e) {
    showToast({ type: 'fail', message: e.message || '发送失败' })
  } finally {
    testingChannel.value = false
  }
}

async function mockBind() {
  binding.value = true
  try {
    await mockBindWechatOfficial()
    showToast({ type: 'success', message: '模拟绑定成功' })
    await load()
  } catch (e) {
    showToast({ type: 'fail', message: e.message || '绑定失败' })
  } finally {
    binding.value = false
  }
}

async function unbindWechat() {
  const row = wechatOfficial.value
  if (!row?.id) return
  try {
    await showConfirmDialog({ title: '解绑微信', message: '解绑后将关闭所有类型的微信通知开关，确定继续？' })
  } catch {
    return
  }
  binding.value = true
  try {
    await deleteSocialBinding(row.id)
    showToast({ type: 'success', message: '已解绑，微信通知已关闭' })
    await load()
  } catch (e) {
    showToast({ type: 'fail', message: e.message || '解绑失败' })
  } finally {
    binding.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.notif-settings-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: calc(24px + env(safe-area-inset-bottom));
}
.section {
  margin-top: 12px;
}
.section-title {
  margin: 0 16px 8px;
  font-size: 14px;
  color: #323233;
  font-weight: 600;
}
.bind-hint {
  margin: 0 16px 10px;
  font-size: 12px;
  color: #646566;
  line-height: 1.55;
}
.bind-hint strong {
  color: #323233;
}
.hint {
  margin: 0 16px 10px;
  font-size: 12px;
  color: #8898aa;
  line-height: 1.5;
}
.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  max-width: 220px;
  font-size: 13px;
  color: #646566;
}
.footer-actions {
  padding: 16px;
}
.token-field {
  margin-top: 8px;
  padding-left: 0;
  padding-right: 0;
}
</style>
