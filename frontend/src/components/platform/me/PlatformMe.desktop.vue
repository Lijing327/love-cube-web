<template>
  <section class="platform-page me-page">
    <div class="me-mobile" aria-label="平台个人中心（移动端）">
      <!-- 用户信息卡片 -->
      <div class="mh-hero">
        <div class="mh-hero-inner">
          <div class="mh-avatar-wrap">
            <img v-if="showHeroAvatarImg" :src="heroAvatarSrc" class="mh-avatar" alt="头像" />
            <div v-else class="mh-avatar mh-avatar-fb">{{ avatarFallback }}</div>
            <span class="mh-lv-pill">Lv.{{ mobileGrowthLevel.level }}</span>
          </div>
          <div class="mh-hero-info">
            <div class="mh-hero-name">{{ displayName }}</div>
            <div class="mh-hero-tags">
              <span class="mh-verify-tag" :class="{ 'is-verified': user?.verificationStatus === 'approved' }">{{ verifyLabel }}</span>
              <span class="mh-role-tag">{{ roleLabel }}</span>
            </div>
          </div>
          <div class="mh-hero-actions">
            <router-link class="mh-message-btn" to="/me/notifications">
              消息
              <em v-if="unreadCount > 0" class="mh-message-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</em>
            </router-link>
            <button type="button" class="mh-edit-btn" @click="openEditPanel">编辑资料</button>
          </div>
        </div>
        <MhHeroBadgeLink :badges="badges" />
        <div class="mh-hero-meta" aria-label="账号与邀请码">
              <span class="mh-meta-line">
                <span class="mh-meta-id">UID {{ userIdDisplay }}</span>
                <span class="mh-meta-sep" aria-hidden="true">·</span>
                <span class="mh-meta-code">{{ inviteLineValue }}</span>
                <button
                  v-if="inviteCodeDisplay"
                  type="button"
                  class="mh-meta-copy"
                  @click="copyInviteCode"
                >复制</button>
                <router-link
                  v-else-if="inviteCodeReady"
                  class="mh-meta-copy mh-meta-link"
                  to="/fellowship/invite"
                >查看</router-link>
              </span>
              <span v-if="copyFeedback" class="mh-meta-feedback" :class="{ err: copyFeedbackError }">{{ copyFeedback }}</span>
            </div>
        <div class="mh-hero-stats">
          <router-link
            v-for="item in profileLightStats"
            :key="item.label"
            :to="item.to"
            class="mh-hero-stat"
          >
            <strong>{{ item.value }}</strong>
            <small>{{ item.label }}</small>
          </router-link>
        </div>
      </div>

      <WechatNotifyEntryBanner />

      <!-- 成长信息（紧凑：称号 + 数值一行，细进度条一行；任务/徽章见任务中心与上方入口） -->
      <div class="mh-card mh-growth-slim">
        <div class="mh-grow-line1">
          <span class="mh-grow-title">{{ mobileGrowthLevel.name }}</span>
          <span class="mh-grow-nums">
            {{ mobileGrowthLevel.currentExp }}/{{ mobileGrowthLevel.nextExp }}
            <span class="mh-grow-sep" aria-hidden="true">·</span>
            还差 {{ Math.max(0, mobileGrowthLevel.nextExp - mobileGrowthLevel.currentExp) }} 经验
          </span>
        </div>
        <div class="mh-exp-bar mh-exp-bar--slim" role="progressbar" :aria-valuenow="mobileGrowthLevel.currentExp" :aria-valuemin="0" :aria-valuemax="mobileGrowthLevel.nextExp">
          <div class="mh-exp-fill" :style="{ width: mobileGrowthProgress }"></div>
        </div>
      </div>

      <MyInvitePanel
        :invite-code="inviteCodeDisplay"
        :invite-count="inviteCount"
      />

      <!-- 常用功能入口 -->
      <div class="mh-card mh-func-card">
        <div class="mh-func-grid">
          <router-link v-for="item in mobileGridItems" :key="item.title" :to="item.to" class="mh-func-item">
            <span class="mh-func-icon" :class="`mh-tone-${item.tone}`">{{ item.icon }}</span>
            <strong class="mh-func-title">{{ item.title }}</strong>
            <small v-if="item.tip" class="mh-func-tip">{{ item.tip }}</small>
          </router-link>
        </div>
      </div>

      <router-link class="mh-card mh-task-entry" to="/platform/my-activities">
        <span class="mh-task-entry-icon" aria-hidden="true">▣</span>
        <div class="mh-task-entry-body">
          <span class="mh-task-entry-title">我的活动</span>
          <span class="mh-task-entry-meta">报名、签到与互评待办</span>
        </div>
        <span class="mh-task-entry-arrow" aria-hidden="true">›</span>
      </router-link>

      <!-- 任务中心：列表与领取在 /me/tasks，此处仅保留入口 -->
      <router-link class="mh-card mh-task-entry" to="/me/tasks">
        <span class="mh-task-entry-icon" aria-hidden="true">✅</span>
        <div class="mh-task-entry-body">
          <span class="mh-task-entry-title">任务中心</span>
          <span class="mh-task-entry-meta">{{ mobileTaskEntrySubtitle }}</span>
        </div>
        <span class="mh-task-entry-arrow" aria-hidden="true">›</span>
      </router-link>

      <!-- 设置列表 -->
      <div class="mh-card mh-settings-card">
        <router-link class="mh-setting-row" to="/me/verify">
          <span class="mh-setting-icon">🪪</span>
          <span class="mh-setting-label">实名认证</span>
          <span class="mh-setting-value">{{ verifyLabel }}</span>
          <span class="mh-setting-arrow">›</span>
        </router-link>
        <router-link class="mh-setting-row" to="/me/privacy">
          <span class="mh-setting-icon">🔒</span>
          <span class="mh-setting-label">隐私设置</span>
          <span class="mh-setting-arrow">›</span>
        </router-link>
        <router-link class="mh-setting-row" to="/me/notifications">
          <span class="mh-setting-icon">🔔</span>
          <span class="mh-setting-label">消息通知</span>
          <span class="mh-setting-arrow">›</span>
        </router-link>
        <router-link class="mh-setting-row" to="/me/notification-settings">
          <span class="mh-setting-icon">📬</span>
          <span class="mh-setting-label">通知渠道设置</span>
          <span class="mh-setting-arrow">›</span>
        </router-link>
        <router-link class="mh-setting-row" to="/me/feedback">
          <span class="mh-setting-icon">💬</span>
          <span class="mh-setting-label">问题工单（提交可得经验）</span>
          <span class="mh-setting-arrow">›</span>
        </router-link>
        <router-link v-if="userStore.isAdmin" class="mh-setting-row" to="/admin/dashboard">
          <span class="mh-setting-icon">🛠️</span>
          <span class="mh-setting-label">管理中心</span>
          <span class="mh-setting-value">快速处理</span>
          <span class="mh-setting-arrow">›</span>
        </router-link>
        <button type="button" class="mh-setting-row mh-setting-danger" @click="handleLogout">
          <span class="mh-setting-icon">🚪</span>
          <span class="mh-setting-label">退出登录</span>
          <span class="mh-setting-arrow">›</span>
        </button>
      </div>
    </div>

    <div class="me-desktop">
      <MePersonalHub
        :user="user"
        :display-name="displayName"
        :unread-count="unreadCount"
        :growth-level="growthLevel"
        :on-open-settings="openSettingsPanel"
        :on-open-edit="openEditPanel"
      />
    </div>

    <div v-if="editOpen" class="me-modal-backdrop" @click.self="closeEditPanel">
      <section class="me-modal" role="dialog" aria-modal="true" aria-label="编辑个人资料">
        <div class="me-modal-head">
          <h3>编辑个人资料</h3>
          <button type="button" aria-label="关闭" @click="closeEditPanel">×</button>
        </div>
        <form class="me-edit-form" @submit.prevent="handleSaveProfile">
          <label>
            <span>昵称</span>
            <input v-model.trim="editForm.username" type="text" maxlength="20" placeholder="请输入昵称" />
          </label>
          <label>
            <span>头像</span>
            <div class="avatar-uploader">
              <img v-if="editForm.avatar" :src="editForm.avatar" alt="头像预览" />
              <div v-else>{{ avatarFallback }}</div>
              <button type="button" :disabled="uploading || saving" @click="handlePickAvatar">
                {{ uploading ? '上传中...' : '选择图片' }}
              </button>
            </div>
          </label>
          <label>
            <span>所在地/城市</span>
            <input v-model.trim="editForm.location" type="text" maxlength="60" placeholder="例如：河北省 保定市" />
          </label>
          <label>
            <span>个人简介</span>
            <textarea v-model.trim="editForm.bio" rows="3" maxlength="200" placeholder="简单介绍一下你自己" />
          </label>
          <div class="me-edit-subtitle">联谊档案（可与联谊端同步编辑）</div>
          <label>
            <span>性别</span>
            <select v-model="editForm.gender">
              <option value="">未填写</option>
              <option value="male">男</option>
              <option value="female">女</option>
            </select>
          </label>
          <label>
            <span>年龄</span>
            <input v-model.trim="editForm.age" type="number" min="1" placeholder="例如：24（不填则保持）" />
          </label>
          <label>
            <span>出生年份</span>
            <input v-model.trim="editForm.birthYear" type="number" min="1" placeholder="例如：1998（不填则保持）" />
          </label>
          <label>
            <span>婚姻状况</span>
            <select v-model="editForm.maritalStatus">
              <option value="">未填写</option>
              <option value="单身">单身</option>
              <option value="已婚">已婚</option>
              <option value="离异">离异</option>
            </select>
          </label>
          <label>
            <span>职业</span>
            <input v-model.trim="editForm.occupation" type="text" maxlength="64" placeholder="例如：工程师（不填则保持）" />
          </label>
          <label>
            <span>学历</span>
            <select v-model="editForm.education">
              <option value="">未填写</option>
              <option value="高中及以下">高中及以下</option>
              <option value="大专">大专</option>
              <option value="本科">本科</option>
              <option value="硕士">硕士</option>
              <option value="博士">博士</option>
            </select>
          </label>
          <label>
            <span>身高(cm)</span>
            <input v-model.trim="editForm.height" type="number" min="1" placeholder="例如：170（不填则保持）" />
          </label>
          <label>
            <span>交友意向</span>
            <textarea v-model.trim="editForm.intention" rows="2" maxlength="500" placeholder="例如：希望共同成长、生活有趣..." />
          </label>
          <label>
            <span>标签</span>
            <input v-model.trim="editForm.tags" type="text" maxlength="500" placeholder="多个标签用逗号分隔（不填则保持）" />
          </label>
          <p v-if="saveMessage" class="save-message" :class="{ 'is-error': saveError }">{{ saveMessage }}</p>
          <div class="modal-actions">
            <button type="button" class="outline-action" :disabled="saving" @click="resetEditForm">重置</button>
            <button type="submit" class="primary-action" :disabled="saving">{{ saving ? '保存中...' : '保存资料' }}</button>
          </div>
        </form>
      </section>
    </div>

    <div v-if="settingsOpen" class="me-modal-backdrop" @click.self="closeSettingsPanel">
      <section class="me-modal" role="dialog" aria-modal="true" aria-label="账号设置">
        <div class="me-modal-head">
          <h3>账号设置</h3>
          <button type="button" aria-label="关闭" @click="closeSettingsPanel">×</button>
        </div>
        <div class="settings-summary">
          <div class="profile-avatar avatar-fallback">{{ avatarFallback }}</div>
          <div>
            <strong>{{ displayName }}</strong>
            <span>{{ roleLabel }}</span>
          </div>
        </div>
        <div class="settings-list">
          <div><span>账号 ID</span><strong>{{ userIdDisplay }}</strong></div>
          <div><span>手机号</span><strong>{{ user?.phone || '未绑定' }}</strong></div>
          <div><span>认证状态</span><strong>{{ verifyLabel }}</strong></div>
          <div><span>注册时间</span><strong>{{ registerDate }}</strong></div>
        </div>
        <div class="modal-actions settings-actions">
          <button type="button" class="primary-action" @click="openEditPanel">编辑个人资料</button>
          <button type="button" class="outline-action" @click="goChangePhonePage">换绑手机号</button>
          <button type="button" class="outline-action" @click="goChangePasswordPage">修改密码</button>
          <button type="button" class="danger-action" @click="handleLogout">退出登录</button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { getNotifUnreadCountCached } from '@/api/notification.js'
import { getUserStatsCached, updateProfile } from '@/api/user.js'
import { getMyFellowshipProfile, updateMyFellowshipProfile } from '@/api/fellowshipProfile.js'
import { getInviteInfo } from '@/api/invite.js'
import { claimAccountTask, getMyGrowth } from '@/api/growth.js'
import { fetchMyGroups, fetchHotGroups } from '@/api/groups.js'
import { useImageUpload } from '@/composables/useImageUpload.js'
import MePersonalHub from '@/components/platform/me/MePersonalHub.desktop.vue'
import MyInvitePanel from '@/components/common/MyInvitePanel.vue'
import MhHeroBadgeLink from '@/components/platform/me/MhHeroBadgeLink.vue'
import WechatNotifyEntryBanner from '@/components/platform/WechatNotifyEntryBanner.vue'
import { getAvatar } from '@/utils/image.js'
import { userAvatarUrlFromApi } from '@/utils/displayFields.js'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const user = computed(() => userStore.userInfo)
const showHeroAvatarImg = computed(() => Boolean(userAvatarUrlFromApi(user.value)))
const heroAvatarSrc = computed(() => getAvatar(user.value))
const unreadCount = ref(0)
const editOpen = ref(false)
const settingsOpen = ref(false)
const saving = ref(false)
const saveMessage = ref('')
const saveError = ref(false)
const { pickAndUpload, uploading } = useImageUpload()
const inviteCode = ref('')
const inviteCount = ref(0)
const inviteCodeReady = ref(false)
const copyFeedback = ref('')
const copyFeedbackError = ref(false)
const editForm = reactive({
  username: '',
  avatar: '',
  location: '',
  bio: '',
  // 联谊档案字段：用于联谊匹配展示，可与联谊端双向编辑
  gender: '',
  age: '',
  birthYear: '',
  maritalStatus: '',
  occupation: '',
  education: '',
  height: '',
  intention: '',
  tags: ''
})
const editFormSnapshot = ref(null)

const myContentCount = ref(0)
const myEventCount = ref(0)
const myFavoriteCount = ref(0)
const myPositiveShareLikeCount = ref(0)
const myFollowingCount = ref(0)
const myFansCount = ref(0)
const myLikesReceived = ref(0)
const growthInfo = ref(null)
const badges = ref([])
const claimingAccountCode = ref('')

function accountGrowthTaskRoute(code) {
  const routes = {
    ACC_AVATAR: { path: '/me', query: { panel: 'edit' } },
    ACC_PHOTO: '/me/profile',
    ACC_BIO: { path: '/me', query: { panel: 'edit' } },
    ACC_JOIN_GROUP: '/platform/groups',
    ACC_FIRST_POST: '/platform/positive-share',
    ACC_BIND_PHONE: '/me/security'
  }
  return routes[code] || '/me'
}

const dashboardAccountTasks = computed(() => {
  const rows = growthInfo.value?.accountTasks
  if (!Array.isArray(rows) || !rows.length) return []
  return rows.map((item) => ({
    code: item.code,
    title: item.name || item.code,
    exp: Number(item.rewardExp ?? 0),
    completed: Boolean(item.completed),
    to: accountGrowthTaskRoute(item.code)
  }))
})

const mobileAccountTasks = computed(() => dashboardAccountTasks.value)

const mobileTaskEntrySubtitle = computed(() => {
  const dailyList = mobileDailyTasks.value
  const dailyTotal = dailyList.length
  const dailyDone = mobileCompletedCount.value
  const acc = mobileAccountTasks.value
  const accLen = acc.length
  const claimable = acc.filter((t) => t.completed).length
  const dailyPart = dailyTotal ? `今日 ${dailyDone}/${dailyTotal}` : '今日任务'
  let accPart = '成长任务已领完'
  if (accLen > 0) {
    accPart =
      claimable > 0
        ? `成长 ${accLen} 项 · ${claimable} 项可领经验`
        : `成长 ${accLen} 项进行中`
  }
  return `${dailyPart} · ${accPart}`
})

async function onClaimAccountTask(code) {
  if (!code || claimingAccountCode.value) return
  claimingAccountCode.value = code
  try {
    const res = await claimAccountTask(code)
    if (res?.claimed) {
      const g = await getMyGrowth()
      growthInfo.value = g || null
      badges.value = Array.isArray(g?.badges) ? g.badges : []
    }
  } catch {
    /* 工作台领取失败时可重新尝试 */
  } finally {
    claimingAccountCode.value = ''
  }
}
const myGroupsList = ref([])
const hotGroupsList = ref([])

const mobileGrowthLevel = computed(() => {
  const d = growthInfo.value
  return {
    level: Number(d?.level ?? 1),
    name: d?.title || '新手用户',
    currentExp: Number(d?.exp ?? 0),
    nextExp: Number(d?.nextLevelExp ?? 100)
  }
})

const mobileDailyTasks = computed(() => {
  const codeToRoute = {
    DAILY_LOGIN: '/platform/checkin',
    DAILY_POST: '/platform/positive-share',
    DAILY_COMMENT: '/platform/content?type=mood',
    DAILY_VIEW: '/platform/content',
    DAILY_LIKE: '/platform/content?type=mood'
  }
  const rows = growthInfo.value?.dailyTasks
  if (Array.isArray(rows) && rows.length) {
    return rows.map(item => ({
      title: item.name || item.code,
      exp: Number(item.rewardExp ?? 0),
      done: Boolean(item.completed),
      to: codeToRoute[item.code] || '/me'
    }))
  }
  return [
    { title: '每日签到', exp: 2, done: false, to: '/platform/checkin' },
    { title: '完善资料', exp: 5, done: false, to: { path: '/me', query: { panel: 'edit' } } },
    { title: '发布动态', exp: 10, done: false, to: '/platform/positive-share' },
    { title: '浏览内容', exp: 1, done: false, to: '/articles' }
  ]
})

const mobileGrowthProgress = computed(() => {
  const next = Math.max(1, mobileGrowthLevel.value.nextExp)
  const cur = mobileGrowthLevel.value.currentExp
  return `${Math.min(100, Math.round((cur / next) * 100))}%`
})

const mobileCompletedCount = computed(() => mobileDailyTasks.value.filter(t => t.done).length)

const mobileGridItems = computed(() => {
  const items = [
    { title: '我的资料', icon: '👤', tone: 'violet', to: '/me/profile' },
    { title: '我的团体', icon: '🏠', tone: 'blue', to: '/me/groups' },
    { title: '我的活动', icon: '▣', tone: 'amber', to: '/platform/my-activities', tip: '报名·签到·互评' },
    { title: '我的动态', icon: '📝', tone: 'rose', to: '/me/posts' },
    { title: '心声收藏', icon: '⭐', tone: 'amber', to: '/me/favorites' },
    { title: '点赞心声', icon: '❤', tone: 'rose', to: '/me/likes' },
    { title: '联谊中心', icon: '💞', tone: 'rose', to: '/fellowship' },
    { title: '邀请码', icon: '▭', tone: 'amber', to: '/fellowship/invite', tip: '邀请好友加入' },
    { title: '我的徽章', icon: '🏅', tone: 'amber', to: '/me/badges' },
    { title: '账号安全', icon: '🔐', tone: 'blue', to: '/me/security' },
    { title: '问题工单', icon: '💡', tone: 'violet', to: '/me/feedback', tip: '提交得+5经验' }
  ]
  if (userStore.hasPermission('group.manage.own') || userStore.hasPermission('group.manage.all')) {
    items.splice(2, 0, {
      title: '进入运营台',
      icon: '📋',
      tone: 'green',
      to: '/platform/me/groups',
      tip: '管理你负责的社区空间'
    })
  }
  if (userStore.isAdmin) {
    items.unshift({ title: '管理中心', icon: '🛠️', tone: 'green', to: '/admin/dashboard' })
  }
  return items
})

const growthLevel = computed(() => ({
  level: mobileGrowthLevel.value.level,
  name: mobileGrowthLevel.value.name,
  currentExp: mobileGrowthLevel.value.currentExp,
  nextExp: mobileGrowthLevel.value.nextExp,
  sources: [
    { label: '发布内容', exp: 10 },
    { label: '每日心声', exp: 5 },
    { label: '创建团体', exp: 20 },
    { label: '获得点赞', exp: 2 }
  ]
}))

const dailyTasks = computed(() => {
  const rows = growthInfo.value?.dailyTasks
  const codeToRoute = {
    DAILY_LOGIN: '/platform/checkin',
    DAILY_POST: '/platform/positive-share',
    DAILY_COMMENT: '/platform/content?type=mood',
    DAILY_VIEW: '/platform/content',
    DAILY_LIKE: '/platform/content?type=mood'
  }
  if (Array.isArray(rows) && rows.length) {
    return rows.map(item => {
      const total = Number(item.targetCount || 1)
      const current = Number(item.progress ?? (item.completed ? total : 0))
      return {
        title: item.name || item.code || '日常任务',
        exp: Number(item.rewardExp ?? 0),
        current: Math.min(current, total),
        total,
        done: Boolean(item.completed),
        to: codeToRoute[item.code] || '/me'
      }
    })
  }
  return [
    { title: '每日签到', exp: 2, current: 0, total: 1, done: false, to: '/platform/checkin' },
    { title: '发布每日心声', exp: 5, current: 0, total: 1, done: false, to: '/platform/positive-share' },
    { title: '浏览精选内容', exp: 1, current: 0, total: 1, done: false, to: '/articles' }
  ]
})

const groupInfo = computed(() => {
  const g = myGroupsList.value[0]
  if (!g) return { name: '暂未加入团体', role: '--', members: 0, activity: '--', weekExp: 0 }
  const roleMap = { OWNER: '创建者', ADMIN: '管理员', MEMBER: '成员' }
  const role = g.isOwner ? '创建者' : (roleMap[String(g.myRole || '').toUpperCase()] || '成员')
  return {
    name: g.name || '--',
    role,
    members: Number(g.memberCount || 0),
    activity: g.memberCount > 50 ? '高' : g.memberCount > 20 ? '中等' : '较低',
    weekExp: 0
  }
})

const groupRanking = computed(() =>
  hotGroupsList.value.slice(0, 3).map((g, i) => ({
    rank: i + 1,
    name: g.name || '--',
    activity: Number(g.memberCount || 0)
  }))
)

const displayName = computed(() => user.value?.username || user.value?.nickname || '用户')
const userIdDisplay = computed(() => user.value?.id || user.value?.userId || '--')
const locationDisplay = computed(() => user.value?.location || '未设置')
const avatarFallback = computed(() => String(displayName.value || 'U').slice(0, 1).toUpperCase())
const inviteCodeDisplay = computed(() => inviteCode.value || '')
const inviteLineValue = computed(() => {
  if (inviteCodeDisplay.value) return inviteCodeDisplay.value
  if (!inviteCodeReady.value) return '加载中…'
  return '—'
})
const growthProgress = computed(() => {
  const next = Math.max(1, Number(growthLevel.value.nextExp || 1))
  const cur = Number(growthLevel.value.currentExp || 0)
  return `${Math.min(100, Math.round((cur / next) * 100))}%`
})
const completedTaskCount = computed(() => dailyTasks.value.filter(item => item.done).length)

const roleLabel = computed(() => {
  const role = String(user.value?.role || '').toLowerCase()
  if (role === 'admin' || role === 'super_admin' || role === 'root') return '平台管理员'
  return '普通用户'
})

const verifyLabel = computed(() => {
  const status = String(user.value?.verificationStatus || 'none')
  if (status === 'approved') return '账号已认证'
  if (status === 'pending') return '认证审核中'
  if (status === 'rejected') return '认证未过'
  return '账号未认证'
})

const registerDate = computed(() => {
  const raw = user.value?.createdAt
  if (!raw) return '--'
  return String(raw).replace('T', ' ').slice(0, 10)
})

const profileLightStats = computed(() => [
  { label: '关注', value: myFollowingCount.value, to: '/me/following' },
  { label: '粉丝', value: myFansCount.value, to: '/me/fans' },
  { label: '获赞', value: myLikesReceived.value, to: '/me/received-likes' }
])

const workspaceItems = computed(() => [
  { title: '我的内容', desc: '发布、管理文章', value: `${myContentCount.value} 篇内容`, icon: '▤', tone: 'violet', to: '/platform/positive-share' },
  { title: '每日心声', desc: '记录每日想法', value: `${myContentCount.value} 条内容`, icon: '♡', tone: 'rose', to: '/platform/positive-share' },
  { title: '我的活动', desc: '报名、签到与互评', value: `${myEventCount.value} 个活动`, icon: '▣', tone: 'amber', to: '/platform/my-activities' },
  { title: '消息中心', desc: '系统通知与互动', value: unreadCount.value > 0 ? `${unreadCount.value} 条未读` : '暂无未读', icon: '●', tone: 'blue', to: '/messages' }
])

const overviewItems = computed(() => [
  { label: '发布内容', value: myContentCount.value, icon: '↗', tone: 'violet', to: '/platform/positive-share' },
  { label: '活动参与', value: myEventCount.value, icon: '✦', tone: 'rose', to: '/platform/my-activities' },
  { label: '心声收藏', value: myFavoriteCount.value, icon: '☆', tone: 'amber', to: '/me/favorites' },
  { label: '点赞心声', value: myPositiveShareLikeCount.value, icon: '♥', tone: 'rose', to: '/me/likes' },
  { label: '互动热度', value: myLikesReceived.value, icon: '♨', tone: 'green', to: '/platform/positive-share' },
  { label: '当前等级', value: `Lv.${growthLevel.value.level}`, icon: '◇', tone: 'blue', to: '/modules' }
])

const quickActions = computed(() => [
  { title: '我的活动', desc: '报名、签到与互评', icon: '▣', tone: 'amber', to: '/platform/my-activities' },
  { title: '内容中心', desc: '管理文章和内容', icon: '▤', tone: 'violet', to: '/platform/positive-share' },
  { title: '模块中心', desc: '管理平台模块', icon: '▦', tone: 'green', to: '/modules' },
  { title: '通知中心', desc: '查看系统通知', icon: '●', tone: 'rose', to: '/messages' },
  { title: '账号设置', desc: '账号与安全', icon: '◇', tone: 'green', to: { path: '/me', query: { panel: 'settings' } } },
  { title: '邀请码', desc: '邀请好友加入', icon: '▭', tone: 'amber', to: '/fellowship/invite' },
  { title: '编辑资料', desc: '修改个人资料', icon: '✎', tone: 'blue', to: { path: '/me', query: { panel: 'edit' } } }
])

async function refreshUnreadCount() {
  const notifRes = await getNotifUnreadCountCached().catch(() => null)
  unreadCount.value = Number(notifRes?.count ?? notifRes?.unreadCount ?? 0)
}

function handleNotifReadAll() {
  unreadCount.value = 0
}

watch(
  () => user.value,
  (value) => {
    // 编辑弹窗打开时，以联谊资料加载结果为准；避免用户刷新覆盖正在编辑的表单
    if (editOpen.value) return
    editForm.username = value?.username || ''
    editForm.avatar = userAvatarUrlFromApi(value) || String(value?.avatar || '').trim()
    editForm.location = value?.location || ''
    editForm.bio = value?.bio || ''
  },
  { immediate: true }
)

async function openEditPanel() {
  editOpen.value = true
  settingsOpen.value = false
  saveMessage.value = ''
  saveError.value = false

  await loadFellowshipProfileForEdit()
  // 加载完成后生成一次快照：用于“重置”
  editFormSnapshot.value = JSON.parse(JSON.stringify(editForm))
}

async function loadFellowshipProfileForEdit() {
  const d = await getMyFellowshipProfile().catch(() => null)
  if (!d) return

  editForm.username = d.nickname || editForm.username
  editForm.avatar = d.avatarUrl || editForm.avatar
  editForm.location = d.city || editForm.location
  editForm.bio = d.bio || editForm.bio

  editForm.gender = d.gender || ''
  editForm.age = d.age ?? ''
  editForm.birthYear = d.birthYear ?? ''
  editForm.maritalStatus = d.maritalStatus || ''
  editForm.occupation = d.occupation || ''
  editForm.education = d.education || ''
  editForm.height = d.height ?? ''
  editForm.intention = d.intention || ''
  editForm.tags = d.tags || ''
}

function closeEditPanel() {
  editOpen.value = false
}

function openSettingsPanel() {
  settingsOpen.value = true
  editOpen.value = false
  if (route.query?.panel !== 'settings') {
    router.replace({ path: '/me', query: { ...route.query, panel: 'settings' } })
  }
}

function closeSettingsPanel() {
  settingsOpen.value = false
}

function resetEditForm() {
  if (editFormSnapshot.value) {
    Object.assign(editForm, JSON.parse(JSON.stringify(editFormSnapshot.value)))
  } else {
    editForm.username = user.value?.username || ''
    editForm.avatar = userAvatarUrlFromApi(user.value) || String(user.value?.avatar || '').trim()
    editForm.location = user.value?.location || ''
    editForm.bio = user.value?.bio || ''
  }
  saveMessage.value = ''
  saveError.value = false
}

async function handlePickAvatar() {
  saveMessage.value = ''
  saveError.value = false
  try {
    const avatarUrl = await pickAndUpload({ quality: 0.8 })
    if (!avatarUrl) throw new Error('上传失败，请重试')
    editForm.avatar = avatarUrl
  } catch (error) {
    saveError.value = true
    saveMessage.value = error?.message || '头像上传失败，请稍后重试'
  }
}

async function handleSaveProfile() {
  saveMessage.value = ''
  saveError.value = false
  const username = editForm.username.trim()
  if (!username) {
    saveError.value = true
    saveMessage.value = '昵称不能为空'
    return
  }
  if (username.length > 20) {
    saveError.value = true
    saveMessage.value = '昵称最多 20 个字符'
    return
  }

  const parsedAge = editForm.age === '' || editForm.age == null ? null : Number(editForm.age)
  if (parsedAge !== null && (!Number.isInteger(parsedAge) || parsedAge <= 0)) {
    saveError.value = true
    saveMessage.value = '年龄必须是大于 0 的整数'
    return
  }

  const parsedBirthYear = editForm.birthYear === '' || editForm.birthYear == null ? null : Number(editForm.birthYear)
  if (parsedBirthYear !== null && (!Number.isInteger(parsedBirthYear) || parsedBirthYear <= 0)) {
    saveError.value = true
    saveMessage.value = '出生年份必须是大于 0 的整数'
    return
  }

  const parsedHeight = editForm.height === '' || editForm.height == null ? null : Number(editForm.height)
  if (parsedHeight !== null && (!Number.isInteger(parsedHeight) || parsedHeight <= 0)) {
    saveError.value = true
    saveMessage.value = '身高必须是大于 0 的整数'
    return
  }
  saving.value = true
  try {
    await updateProfile({
      username,
      profilePhoto: editForm.avatar,
      location: editForm.location,
      bio: editForm.bio
    })

    // 联谊档案保存：用联谊接口重算完成度、并把联谊字段同步到对应表
    const fellowshipPayload = {}
    if (editForm.gender) fellowshipPayload.gender = editForm.gender
    if (parsedAge !== null) fellowshipPayload.age = parsedAge
    if (parsedBirthYear !== null) fellowshipPayload.birthYear = parsedBirthYear
    if (editForm.maritalStatus) fellowshipPayload.maritalStatus = editForm.maritalStatus
    if (editForm.occupation) fellowshipPayload.occupation = editForm.occupation
    if (editForm.education) fellowshipPayload.education = editForm.education
    if (parsedHeight !== null) fellowshipPayload.height = parsedHeight
    if (editForm.intention) fellowshipPayload.intention = editForm.intention
    if (editForm.tags) fellowshipPayload.tags = editForm.tags

    try {
      await updateMyFellowshipProfile(fellowshipPayload)
    } catch (e) {
      saveError.value = true
      saveMessage.value = e?.response?.data?.message || e?.message || '联谊档案保存失败，请稍后重试'
    }

    await userStore.refreshCurrentUser().catch(() => {})
    if (!saveError.value) saveMessage.value = '资料已更新'
    window.setTimeout(() => {
      if (!saveError.value) closeEditPanel()
    }, 600)
  } catch (error) {
    saveError.value = true
    saveMessage.value = error?.message || '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

function handleLogout() {
  userStore.logout()
  router.replace('/login')
}

function goChangePasswordPage() {
  closeSettingsPanel()
  router.push('/fellowship/change-password')
}

function goChangePhonePage() {
  closeSettingsPanel()
  router.push('/fellowship/change-phone')
}

async function copyInviteCode() {
  if (!inviteCodeDisplay.value) return
  copyFeedback.value = ''
  copyFeedbackError.value = false
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(inviteCodeDisplay.value)
    } else {
      const input = document.createElement('input')
      input.value = inviteCodeDisplay.value
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    copyFeedback.value = '已复制'
    window.setTimeout(() => { copyFeedback.value = '' }, 2000)
  } catch {
    copyFeedbackError.value = true
    copyFeedback.value = '复制失败'
    window.setTimeout(() => { copyFeedback.value = '' }, 2000)
  }
}

onMounted(async () => {
  window.addEventListener('platform-notif-read-all', handleNotifReadAll)
  if (route.query?.panel === 'settings') openSettingsPanel()
  if (route.query?.panel === 'edit') openEditPanel()
  if (!user.value) await userStore.refreshCurrentUser().catch(() => {})
  if (userStore.token) await userStore.loadAdminContext().catch(() => null)
  await refreshUnreadCount()
  const [statsRes, inviteRes, growthRes, myGroupsRes, hotGroupsRes] = await Promise.allSettled([
    getUserStatsCached(), getInviteInfo(), getMyGrowth(), fetchMyGroups(), fetchHotGroups()
  ])
  if (statsRes.status === 'fulfilled' && statsRes.value) {
    myContentCount.value = Number(statsRes.value.contentCount ?? 0)
    myEventCount.value = Number(statsRes.value.eventCount ?? 0)
    myFavoriteCount.value = Number(statsRes.value.positiveShareBookmarkCount ?? 0)
    myPositiveShareLikeCount.value = Number(statsRes.value.positiveShareLikeCount ?? 0)
    myFollowingCount.value = Number(statsRes.value.followingCount ?? 0)
    myFansCount.value = Number(statsRes.value.fansCount ?? 0)
    myLikesReceived.value = Number(statsRes.value.likesReceived ?? 0)
  }
  if (inviteRes.status === 'fulfilled') {
    inviteCode.value = String(inviteRes.value?.inviteCode || inviteRes.value?.code || '').trim()
    inviteCount.value = Number(inviteRes.value?.inviteCount ?? 0)
  }
  inviteCodeReady.value = true
  if (growthRes.status === 'fulfilled') {
    growthInfo.value = growthRes.value || null
    badges.value = Array.isArray(growthRes.value?.badges) ? growthRes.value.badges : []
  }
  if (myGroupsRes.status === 'fulfilled') {
    const raw = myGroupsRes.value
    myGroupsList.value = Array.isArray(raw) ? raw : (Array.isArray(raw?.items) ? raw.items : [])
  }
  if (hotGroupsRes.status === 'fulfilled') {
    const raw = hotGroupsRes.value
    hotGroupsList.value = Array.isArray(raw) ? raw : (Array.isArray(raw?.items) ? raw.items : [])
  }
})

watch(
  () => route.query?.panel,
  (panel) => {
    if (panel === 'settings') openSettingsPanel()
    if (panel === 'edit') openEditPanel()
  }
)

onBeforeUnmount(() => {
  window.removeEventListener('platform-notif-read-all', handleNotifReadAll)
})
</script>

<style scoped>
.me-page {
  --me-bg: var(--lc-bg);
  --me-bg-soft: var(--lc-bg);
  --me-card: var(--lc-surface);
  --me-border: var(--lc-soft-alt);
  --me-text: var(--lc-text);
  --me-muted: var(--lc-muted-light);
  --me-primary: var(--lc-violet);
  --me-primary-dark: var(--lc-indigo);
  --me-white: var(--lc-surface);
  --me-success: var(--lc-emerald);
  --me-danger: var(--lc-red);
  --me-text-soft: var(--lc-slate);
  --me-border-strong: var(--lc-border);
  --me-violet: var(--lc-indigo);
  --me-rose: var(--lc-pink);
  --me-amber: var(--lc-amber);
  --me-blue: var(--lc-blue);
  --me-medal: #9a6700;
  --me-orange: var(--lc-orange);
  --me-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
  min-height: 100vh;
  background: var(--me-bg);
  color: var(--me-text);
}

:global(.platform-route-back) {
  display: none !important;
}

.me-desktop {
  display: none;
}

.me-shell {
  width: min(100%, 760px);
  margin: 0 auto;
  padding: 24px 18px calc(92px + env(safe-area-inset-bottom));
}

.me-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.me-aside,
.me-main-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.me-layout .growth-card,
.me-layout .task-card,
.me-main-stack .workspace-grid,
.me-main-stack .overview-card,
.me-main-stack .group-card,
.me-main-stack .quick-grid {
  margin-top: 0;
}

.me-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 4px 16px;
}

.me-header h1 {
  margin: 0;
  font-size: clamp(26px, 7vw, 30px);
  line-height: 1.12;
  font-weight: 800;
  letter-spacing: 0;
}

.me-header p {
  margin: 8px 0 0;
  color: var(--me-muted);
  font-size: 14px;
  line-height: 1.45;
}

.icon-button {
  width: 42px;
  height: 42px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--me-text);
  font-size: 24px;
  cursor: pointer;
}

.dashboard-card {
  border: 1px solid var(--me-border);
  border-radius: 20px;
  background: var(--me-card);
  box-shadow: var(--me-shadow);
}

.profile-card {
  padding: 22px 24px;
}

.profile-main {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
}

.profile-avatar {
  width: 84px;
  height: 84px;
  flex: 0 0 auto;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.16);
}

.avatar-fallback {
  display: grid;
  place-items: center;
  color: var(--me-white);
  background: linear-gradient(135deg, var(--me-primary), var(--me-primary-dark));
  font-size: 28px;
  font-weight: 800;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-badges {
  display: flex;
  flex: 0 0 auto;
  gap: 12px;
  margin-left: auto;
}

.profile-badges span {
  position: relative;
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9), 0 12px 24px rgba(15, 23, 42, 0.08);
  font-size: 20px;
  font-weight: 900;
}

.profile-badges span::after {
  position: absolute;
  inset: 7px 9px auto;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0));
  content: "";
  pointer-events: none;
}

.profile-badges .tone-amber {
  background: linear-gradient(145deg, #fff8e8, #fff0c9);
}

.profile-badges .tone-rose {
  background: linear-gradient(145deg, #fff1f7, #ffe3ef);
}

.profile-badges .tone-blue {
  background: linear-gradient(145deg, var(--lc-blue-light), var(--lc-blue-light));
}

.profile-name-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.profile-name-row h2 {
  margin: 0;
  font-size: 20px;
  line-height: 1.25;
  font-weight: 800;
}

.creator-tag {
  border-radius: 999px;
  padding: 4px 9px;
  color: var(--me-primary-dark);
  background: var(--lc-indigo-soft);
  font-size: 12px;
  font-weight: 700;
}

.profile-info p,
.invite-row {
  margin: 7px 0 0;
  color: var(--me-muted);
  font-size: 13px;
}

.invite-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.invite-row button {
  border: 0;
  border-radius: 999px;
  padding: 5px 11px;
  color: var(--me-primary-dark);
  background: #f0efff;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.invite-row em,
.save-message {
  color: var(--me-success);
  font-size: 12px;
  font-style: normal;
}

.invite-row em.is-error,
.save-message.is-error {
  color: var(--me-danger);
}

.profile-light-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  width: min(100%, 330px);
  margin: -2px 0 0 auto;
  padding: 8px 12px;
  border: 1px solid rgba(226, 232, 240, 0.72);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.76));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.96), 0 10px 22px rgba(15, 23, 42, 0.04);
}

.profile-light-stats span {
  position: relative;
  display: grid;
  gap: 3px;
  text-align: center;
}

.profile-light-stats span + span::before {
  position: absolute;
  left: -5px;
  top: 7px;
  width: 1px;
  height: 28px;
  background: linear-gradient(180deg, rgba(226, 232, 240, 0), var(--lc-border), rgba(226, 232, 240, 0));
  content: "";
}

.profile-light-stats small,
.overview-item small,
.quick-item small,
.workspace-item small {
  color: var(--me-muted);
  font-size: 12px;
}

.profile-light-stats strong {
  font-size: 17px;
}

.profile-actions,
.modal-actions,
.group-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 18px;
}

.profile-actions {
  margin-top: 24px;
}

.primary-action,
.outline-action,
.danger-action,
.group-actions a,
.task-item a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
}

.profile-edit-action::before {
  margin-right: 8px;
  content: "✎";
  font-size: 15px;
}

.fellowship-action::before {
  margin-right: 8px;
  content: "♡";
  font-size: 16px;
}

.group-enter-action::before,
.group-post-action::before,
.group-member-action::before {
  margin-right: 6px;
  font-size: 13px;
}

.group-enter-action::before {
  content: "↗";
}

.group-post-action::before {
  content: "✈";
}

.group-member-action::before {
  content: "♙";
}

.primary-action {
  border: 0;
  color: var(--me-white);
  background: linear-gradient(135deg, var(--me-primary), var(--me-primary-dark));
}

.outline-action,
.group-actions a {
  border: 1px solid var(--lc-border);
  color: var(--me-text);
  background: var(--lc-surface);
}

.danger-action {
  border: 1px solid var(--lc-red-light);
  color: var(--me-danger);
  background: #fff5f5;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.section-head a,
.ranking-head a {
  color: var(--me-muted);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.growth-card,
.task-card,
.overview-card,
.group-card {
  margin-top: 16px;
  padding: 22px 24px;
}

.growth-body {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr) 210px;
  gap: 24px;
  align-items: center;
}

.level-badge {
  position: relative;
  display: grid;
  place-items: center;
  width: 96px;
  height: 96px;
  color: var(--me-white);
  background: radial-gradient(circle at 32% 24%, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0) 30%),
    linear-gradient(145deg, var(--lc-violet) 0%, var(--me-primary) 48%, var(--me-primary-dark) 100%);
  clip-path: polygon(25% 5%, 75% 5%, 100% 50%, 75% 95%, 25% 95%, 0 50%);
  font-size: 25px;
  font-weight: 900;
  text-shadow: 0 1px 8px rgba(40, 33, 160, 0.22);
  filter: drop-shadow(0 14px 22px rgba(79, 70, 229, 0.22));
}

.level-badge::before {
  position: absolute;
  inset: -9px;
  z-index: -1;
  background: linear-gradient(145deg, var(--lc-indigo-soft), var(--lc-blue-border));
  clip-path: inherit;
  content: "";
}

.level-badge::after {
  position: absolute;
  inset: 12px 20px auto;
  height: 18px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0));
  content: "";
  pointer-events: none;
}

.level-content h3 {
  margin: 6px 0 8px;
  font-size: 18px;
}

.level-content p {
  margin: 0;
  color: var(--me-muted);
  font-size: 13px;
}

.level-content h3,
.level-content p {
  word-break: keep-all;
}

.progress-track,
.mini-progress {
  overflow: hidden;
  height: 8px;
  border-radius: 999px;
  background: var(--lc-soft-alt);
}

.progress-track {
  margin-top: 16px;
}

.progress-track span,
.mini-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--me-primary);
}

.progress-meta {
  margin-top: 8px;
  color: var(--me-muted);
  font-size: 13px;
  text-align: right;
}

.exp-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 8px 0 8px 24px;
  border-left: 1px solid var(--me-border);
  list-style: none;
}

.exp-list li {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: var(--me-muted);
  font-size: 13px;
}

.exp-list li::before {
  width: 20px;
  flex: 0 0 auto;
  color: var(--me-primary);
  font-size: 15px;
  font-weight: 900;
  content: "▧";
}

.exp-list li:nth-child(2)::before {
  color: var(--me-rose);
  content: "♡";
}

.exp-list li:nth-child(3)::before {
  content: "♙";
}

.exp-list li:nth-child(4)::before {
  content: "♧";
}

.growth-card .exp-list li {
  align-items: center;
}

.growth-card .exp-list li::before {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: 1px solid var(--me-border);
  border-radius: 9px;
  background: linear-gradient(145deg, var(--lc-indigo-soft), var(--lc-surface));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.95), 0 8px 16px rgba(79, 70, 229, 0.08);
  font-size: 13px;
}

.growth-card .exp-list li:nth-child(2)::before {
  background: linear-gradient(145deg, #fff0f7, var(--lc-surface));
}

.growth-card .exp-list li:nth-child(3)::before {
  background: linear-gradient(145deg, var(--lc-indigo-soft), var(--lc-surface));
}

.growth-card .exp-list li:nth-child(4)::before {
  color: var(--me-blue);
  background: linear-gradient(145deg, var(--lc-blue-light), var(--lc-surface));
}

.exp-list li span {
  flex: 1;
}

.exp-list strong {
  color: var(--me-muted);
}

.task-head h2 span {
  margin-left: 6px;
  color: var(--me-muted);
  font-size: 13px;
  font-weight: 500;
}

.task-head p {
  margin: 0;
  color: var(--me-muted);
  font-size: 13px;
  font-weight: 700;
}

.task-head b {
  color: var(--me-primary);
  font-weight: 900;
}

.task-grid {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding: 2px 0 4px;
  scrollbar-width: none;
}

.task-grid::-webkit-scrollbar {
  display: none;
}

.task-item {
  display: grid;
  box-sizing: border-box;
  flex: 0 0 calc((100% - 14px) / 2);
  justify-items: center;
  width: auto;
  min-width: 0;
  padding: 16px 14px 14px;
  border: 1px solid var(--me-border);
  border-radius: 16px;
  background: linear-gradient(145deg, var(--lc-bg), var(--lc-surface));
  text-align: center;
}

.task-item:nth-child(n + 2) {
  background: linear-gradient(145deg, #fffaf2, #fffdf8);
}

.task-item.is-done {
  background: linear-gradient(145deg, var(--lc-bg), var(--lc-surface));
}

.task-check {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: var(--me-white);
  background: var(--me-primary);
  font-size: 18px;
  font-weight: 900;
}

.task-item h3 {
  margin: 9px 0 4px;
  font-size: 13px;
  line-height: 1.35;
}

.task-item p,
.task-item small {
  margin: 0;
  color: var(--me-muted);
  font-size: 12px;
}

.mini-progress {
  width: 100%;
  height: 6px;
  margin: 10px 0 6px;
}

.task-item a {
  min-height: 30px;
  width: 100%;
  margin-top: 9px;
  border: 1px solid var(--lc-blue-border);
  color: var(--me-primary-dark);
  background: var(--lc-surface);
}

.task-item.is-done a {
  color: var(--me-muted);
  border-color: var(--me-border-strong);
}

.workspace-grid,
.quick-grid {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.workspace-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.quick-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.workspace-item,
.quick-item,
.overview-item {
  display: grid;
  gap: 7px;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.workspace-item {
  min-height: 136px;
  padding: 18px 16px;
}

.workspace-item .soft-icon {
  color: var(--me-white);
  box-shadow: 0 12px 20px rgba(79, 70, 229, 0.16);
}

.workspace-item .tone-violet {
  background: linear-gradient(135deg, var(--lc-violet), var(--me-primary-dark));
}

.workspace-item .tone-rose {
  background: linear-gradient(135deg, var(--lc-rose), var(--lc-pink));
}

.workspace-item .tone-amber {
  background: linear-gradient(135deg, var(--lc-orange), var(--lc-orange));
}

.workspace-item .tone-blue {
  background: linear-gradient(135deg, var(--lc-blue), var(--lc-blue));
}

.workspace-item strong,
.quick-item strong {
  font-size: 15px;
}

.workspace-item em {
  color: var(--me-muted);
  font-size: 13px;
  font-style: normal;
}

.soft-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 12px;
  font-size: 20px;
  font-weight: 900;
}

.tone-violet { color: var(--me-violet); background: var(--lc-indigo-soft); }
.tone-rose { color: var(--me-rose); background: var(--lc-pink-light); }
.tone-amber { color: var(--me-amber); background: var(--lc-amber-light); }
.tone-blue { color: var(--me-blue); background: var(--lc-blue-light); }
.tone-green { color: var(--me-success); background: var(--lc-green-light); }

.overview-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(62px, 1fr));
  gap: 16px;
  overflow: hidden;
}

.overview-item {
  min-width: 0;
  justify-items: center;
  padding: 6px 2px;
  text-align: center;
}

.overview-item strong {
  font-size: 22px;
}

.group-summary {
  display: flex;
  align-items: center;
  gap: 14px;
}

.group-avatar {
  display: grid;
  place-items: center;
  width: 74px;
  height: 74px;
  flex: 0 0 auto;
  border-radius: 50%;
  color: var(--me-white);
  background: linear-gradient(135deg, var(--me-primary), var(--me-primary-dark));
  box-shadow: 0 10px 18px rgba(109, 93, 251, 0.22);
  font-weight: 900;
}

.group-summary h3 {
  margin: 0 0 7px;
  font-size: 17px;
}

.group-summary h3 span {
  margin-left: 6px;
  border-radius: 999px;
  padding: 3px 8px;
  color: var(--me-primary-dark);
  background: var(--lc-indigo-soft);
  font-size: 12px;
}

.group-summary p {
  margin: 4px 0 0;
  color: var(--me-muted);
  font-size: 13px;
}

.group-summary i {
  display: inline-block;
  width: 1px;
  height: 10px;
  margin: 0 8px;
  background: var(--lc-border);
}

.group-actions {
  grid-template-columns: repeat(3, minmax(110px, 1fr));
  max-width: 360px;
  margin-left: auto;
}

.group-actions a {
  min-height: 36px;
  font-size: 13px;
}

.ranking-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 20px;
}

.ranking-head h3 {
  margin: 0;
  font-size: 15px;
}

.ranking-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.ranking-list li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 0 12px;
  border: 1px solid var(--me-border);
  border-radius: 12px;
  background: var(--me-bg-soft);
}

.rank-medal {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: var(--me-medal);
  background: #fff1c2;
  font-weight: 900;
}

.ranking-list li:nth-child(2) .rank-medal {
  color: var(--me-muted);
  background: var(--lc-soft-alt);
}

.ranking-list li:nth-child(3) .rank-medal {
  color: var(--me-orange);
  background: var(--lc-orange-light);
}

.ranking-list small {
  grid-column: 2;
  color: var(--me-muted);
}

.quick-item {
  justify-items: center;
  min-height: 108px;
  padding: 16px 8px;
  text-align: center;
}

.quick-item .soft-icon {
  width: 36px;
  height: 36px;
}

.me-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(15, 23, 42, 0.42);
}

.me-modal {
  width: min(100%, 430px);
  max-height: calc(100vh - 36px);
  overflow: auto;
  border-radius: 20px;
  background: var(--lc-surface);
  padding: 18px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}

.me-modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.me-modal-head h3 {
  margin: 0;
  font-size: 18px;
}

.me-modal-head button {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: var(--lc-soft);
  font-size: 20px;
}

.me-edit-form {
  display: grid;
  gap: 14px;
}

.me-edit-form label {
  display: grid;
  gap: 7px;
  color: var(--me-text-soft);
  font-size: 13px;
  font-weight: 800;
}

.me-edit-form input,
.me-edit-form textarea {
  width: 100%;
  border: 1px solid var(--lc-border);
  border-radius: 12px;
  padding: 11px 12px;
  color: var(--me-text);
  background: var(--lc-surface);
  font: inherit;
  outline: 0;
}

.me-edit-form select {
  width: 100%;
  border: 1px solid var(--lc-border);
  border-radius: 12px;
  padding: 11px 12px;
  color: var(--me-text);
  background: var(--lc-surface);
  font: inherit;
  outline: 0;
}

.me-edit-subtitle {
  margin-top: 6px;
  color: var(--me-text-soft);
  font-size: 13px;
  font-weight: 900;
  line-height: 1.35;
}

.avatar-uploader {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-uploader img,
.avatar-uploader div {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  object-fit: cover;
  color: var(--me-white);
  background: linear-gradient(135deg, var(--me-primary), var(--me-primary-dark));
  font-weight: 900;
}

.avatar-uploader button {
  border: 1px solid var(--lc-border);
  border-radius: 12px;
  padding: 10px 12px;
  color: var(--me-text);
  background: var(--lc-surface);
}

.settings-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.settings-summary .profile-avatar {
  width: 52px;
  height: 52px;
  font-size: 20px;
}

.settings-summary strong,
.settings-summary span {
  display: block;
}

.settings-summary span {
  margin-top: 4px;
  color: var(--me-muted);
  font-size: 13px;
}

.settings-list {
  border: 1px solid var(--me-border);
  border-radius: 14px;
  overflow: hidden;
}

.settings-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 14px;
  border-bottom: 1px solid var(--me-border);
  color: var(--me-muted);
  font-size: 13px;
}

.settings-list div:last-child {
  border-bottom: 0;
}

.settings-list strong {
  color: var(--me-text);
}

.settings-actions {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 767px) {
  :global(.platform-header),
  :global(.platform-footer),
  :global(.co-creation-toolbar) {
    display: none !important;
  }

  .me-shell {
    padding-top: calc(12px + env(safe-area-inset-top));
  }

  .growth-body {
    grid-template-columns: 84px minmax(0, 1fr);
    gap: 14px;
    align-items: start;
  }

  .exp-list {
    grid-column: 1 / -1;
    margin-top: 6px;
    padding: 12px 0 0;
    border-top: 1px solid var(--me-border);
    border-left: 0;
  }

  .level-badge {
    width: 80px;
    height: 80px;
    font-size: 22px;
  }

  .workspace-item,
  .quick-item {
    min-height: 0;
    padding: 12px 10px;
    gap: 6px;
  }

  .workspace-item {
    min-height: 88px;
  }

  .quick-item {
    min-height: 72px;
  }
}

@media (min-width: 768px) {
  .me-shell {
    padding-top: 36px;
  }

  .me-modal {
    width: min(100%, 480px);
  }
}

@media (min-width: 1024px) {
  .me-page {
    width: min(calc(100% - 32px), 1720px);
    max-width: none;
    margin: 20px auto 48px;
    padding: 0;
  }

  .me-mobile {
    display: none;
  }

  .me-desktop {
    display: block;
    width: 100%;
  }

  .me-shell {
    width: 100%;
    padding: 16px 16px 48px;
  }

  .me-header h1 {
    font-size: 32px;
  }

  .me-header p {
    font-size: 15px;
  }

  .me-layout {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 372px;
    grid-template-rows: auto 1fr;
    gap: 22px 28px;
    align-items: start;
  }

  .me-layout > .profile-card {
    grid-column: 1;
    grid-row: 1;
  }

  .me-layout > .me-aside {
    grid-column: 2;
    grid-row: 1 / span 2;
    align-self: start;
  }

  .me-layout > .me-main-stack {
    grid-column: 1;
    grid-row: 2;
  }

  .me-aside {
    position: sticky;
    top: 84px;
    z-index: 1;
  }

  .profile-card {
    padding: 26px 28px;
  }

  .profile-main {
    grid-template-columns: 100px minmax(0, 1fr) auto;
    gap: 24px;
  }

  .profile-avatar {
    width: 100px;
    height: 100px;
  }

  .avatar-fallback {
    font-size: 32px;
  }

  .profile-name-row h2 {
    font-size: 22px;
  }

  .profile-light-stats {
    width: min(100%, 360px);
  }

  .profile-actions {
    max-width: 520px;
  }

  .me-aside .growth-body {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .me-aside .growth-body .level-badge {
    align-self: center;
  }

  .me-aside .exp-list {
    padding-top: 14px;
    padding-left: 0;
    border-top: 1px solid var(--me-border);
    border-left: 0;
  }

  .me-aside .task-grid {
    flex-wrap: wrap;
    overflow-x: visible;
    justify-content: flex-start;
    gap: 12px;
  }

  .me-aside .task-item {
    flex: 1 1 calc(50% - 6px);
    max-width: calc(50% - 6px);
    min-width: 0;
    box-sizing: border-box;
  }

  .me-main-stack .group-actions {
    max-width: none;
    margin-left: 0;
  }

  .me-main-stack .ranking-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .me-main-stack .quick-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 1280px) {
  .me-shell {
    width: min(100%, 1240px);
  }

  .me-layout {
    grid-template-columns: minmax(0, 1fr) 400px;
    gap: 24px 32px;
  }
}

@media (min-width: 520px) {
  .task-item {
    flex-basis: calc((100% - 28px) / 3);
  }
}

@media (min-width: 720px) {
  .task-item {
    flex-basis: calc((100% - 42px) / 4);
  }
}

@media (max-width: 420px) {
  .me-shell {
    padding-right: 14px;
    padding-left: 14px;
  }

  .profile-card {
    padding: 16px;
  }

  .profile-main {
    grid-template-columns: 72px minmax(0, 1fr);
    align-items: center;
  }

  .profile-avatar {
    width: 72px;
    height: 72px;
  }

  .profile-light-stats {
    width: 100%;
    margin-top: 16px;
  }

  .growth-body {
    grid-template-columns: auto minmax(0, 1fr);
    gap: 14px;
  }

  .exp-list {
    grid-column: 1 / -1;
    padding: 12px 0 0;
    border-top: 1px solid var(--me-border);
    border-left: 0;
  }

  .level-badge {
    width: 72px;
    height: 72px;
    font-size: 21px;
  }

  .workspace-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .profile-actions {
    grid-template-columns: 1fr;
  }
}

/* ═══════════════════════════════════════════════════════════
   Mobile personal center redesign  (mh-* namespace)
   Only applies inside .me-mobile, hidden on ≥ 1024 px
   ═══════════════════════════════════════════════════════════ */

.me-mobile {
  padding: calc(18px + env(safe-area-inset-top)) 14px calc(84px + env(safe-area-inset-bottom));
  background: var(--lc-bg);
  min-height: 100vh;
}

/* ── Hero card ─────────────────────────────────────────── */
.mh-hero {
  border-radius: 20px;
  background: linear-gradient(135deg, var(--lc-violet) 0%, var(--lc-indigo) 55%, var(--lc-indigo) 100%);
  padding: 20px 18px 16px;
  margin-bottom: 14px;
}

.mh-hero-inner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.mh-avatar-wrap {
  position: relative;
  flex: 0 0 auto;
}

.mh-avatar {
  display: block;
  width: 66px;
  height: 66px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.55);
}

.mh-avatar-fb {
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.22);
  color: var(--lc-surface);
  font-size: 24px;
  font-weight: 800;
}

.mh-lv-pill {
  position: absolute;
  bottom: -5px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--lc-surface);
  color: var(--me-primary);
  border-radius: 999px;
  padding: 1px 8px;
  font-size: 10px;
  font-weight: 800;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
}

.mh-hero-info {
  flex: 1;
  min-width: 0;
  padding-top: 2px;
}

.mh-hero-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--lc-surface);
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mh-hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 6px;
}

.mh-verify-tag,
.mh-role-tag {
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 700;
}

.mh-verify-tag {
  background: rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.8);
}

.mh-verify-tag.is-verified {
  background: rgba(16, 185, 129, 0.28);
  color: var(--lc-green-light);
}

.mh-role-tag {
  background: rgba(255, 255, 255, 0.13);
  color: rgba(255, 255, 255, 0.65);
}

.mh-hero-meta {
  margin-top: 10px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.72);
}

.mh-hero :deep(.mh-badge-row) {
  margin-top: 14px;
}

.mh-meta-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 6px;
  line-height: 1.35;
}

.mh-meta-id {
  flex: 0 0 auto;
  color: rgba(255, 255, 255, 0.55);
  font-weight: 600;
}

.mh-meta-sep {
  color: rgba(255, 255, 255, 0.35);
  user-select: none;
}

.mh-meta-code {
  min-width: 0;
  font-weight: 800;
  letter-spacing: 0.03em;
  color: rgba(255, 255, 255, 0.95);
  word-break: break-all;
}

.mh-meta-copy {
  flex: 0 0 auto;
  margin-left: 2px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.12);
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  cursor: pointer;
  text-decoration: none;
  line-height: 1.3;
}

.mh-meta-link {
  display: inline-block;
}

.mh-meta-feedback {
  display: block;
  margin-top: 2px;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.8);
}

.mh-meta-feedback.err {
  color: #fecaca;
}

.mh-hero-actions {
  flex: 0 0 auto;
  display: grid;
  gap: 8px;
  justify-items: end;
}

.mh-message-btn,
.mh-edit-btn {
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.14);
  color: var(--lc-surface);
  font-size: 12px;
  font-weight: 700;
  padding: 6px 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
  text-decoration: none;
}

.mh-message-btn {
  position: relative;
}

.mh-message-badge {
  position: absolute;
  top: -6px;
  right: -7px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ff4d6d;
  color: #fff;
  font-size: 9px;
  line-height: 16px;
  font-style: normal;
  text-align: center;
}

.mh-message-btn:active,
.mh-edit-btn:active {
  background: rgba(255, 255, 255, 0.25);
}

.mh-hero-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.mh-hero-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  text-align: center;
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.mh-hero-stat:active {
  opacity: 0.82;
}

.mh-hero-stat + .mh-hero-stat {
  border-left: 1px solid rgba(255, 255, 255, 0.2);
}

.mh-hero-stat strong {
  font-size: 17px;
  font-weight: 800;
  color: var(--lc-surface);
  line-height: 1;
}

.mh-hero-stat small {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
}

/* ── Shared card base ────────────────────────────────────── */
.mh-card {
  margin-bottom: 14px;
  border-radius: 18px;
  background: var(--lc-surface);
  border: 1px solid var(--lc-soft-alt);
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
  padding: 16px;
}

.mh-task-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  text-decoration: none;
  color: inherit;
  -webkit-tap-highlight-color: transparent;
}

.mh-task-entry:active {
  background: var(--lc-bg);
}

.mh-task-entry-icon {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.12), rgba(99, 102, 241, 0.14));
  font-size: 18px;
}

.mh-task-entry-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.mh-task-entry-title {
  font-size: 14px;
  font-weight: 800;
  color: var(--lc-text);
}

.mh-task-entry-meta {
  font-size: 11px;
  color: var(--lc-subtle);
  line-height: 1.35;
}

.mh-task-entry-arrow {
  flex: 0 0 auto;
  font-size: 20px;
  color: var(--lc-subtle);
  font-weight: 300;
}

/* ── Growth slim bar ─────────────────────────────────────── */
.mh-growth-slim {
  padding: 10px 14px 12px;
}

.mh-grow-line1 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.mh-grow-title {
  font-size: 13px;
  font-weight: 800;
  color: var(--lc-text);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mh-grow-nums {
  flex: 0 0 auto;
  font-size: 11px;
  font-weight: 600;
  color: var(--lc-subtle);
  white-space: nowrap;
}

.mh-grow-sep {
  margin: 0 4px;
  opacity: 0.45;
}

.mh-exp-bar {
  height: 8px;
  border-radius: 999px;
  background: var(--lc-soft-alt);
  overflow: hidden;
}

.mh-exp-bar--slim {
  height: 5px;
}

.mh-exp-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--lc-violet), var(--lc-indigo));
  transition: width 0.5s ease;
}

/* ── Function grid ───────────────────────────────────────── */
.mh-func-card {
  padding: 14px 8px 10px;
}

.mh-func-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 2px;
}

.mh-func-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 12px 4px 10px;
  text-decoration: none;
  color: inherit;
  border-radius: 12px;
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.mh-func-item:active {
  background: var(--lc-soft);
}

.mh-func-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 14px;
  font-size: 22px;
  line-height: 1;
}

.mh-func-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--lc-text-deep);
  text-align: center;
  line-height: 1.2;
}

.mh-func-tip {
  font-size: 10px;
  color: var(--lc-indigo);
  line-height: 1;
}

.mh-tone-violet { background: var(--lc-indigo-soft); }
.mh-tone-blue   { background: var(--lc-blue-light); }
.mh-tone-rose   { background: var(--lc-pink-light); }
.mh-tone-amber  { background: var(--lc-amber-light); }
.mh-tone-green  { background: var(--lc-green-light); }

/* ── Task card ───────────────────────────────────────────── */
.mh-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.mh-card-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--lc-text);
}

.mh-card-meta {
  font-size: 10px;
  color: var(--lc-subtle);
}

.mh-task-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.mh-task-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 12px;
  background: var(--lc-bg);
  border: 1px solid var(--lc-soft-alt);
}

.mh-task-row.done {
  background: var(--lc-emerald-light);
  border-color: var(--lc-green-light);
}

.mh-task-check {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1.5px solid var(--lc-border);
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 800;
  color: transparent;
}

.mh-task-row.done .mh-task-check {
  background: var(--lc-emerald);
  border-color: var(--lc-emerald);
  color: var(--lc-surface);
}

.mh-task-body {
  flex: 1;
  min-width: 0;
}

.mh-task-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--lc-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mh-task-exp {
  font-size: 10px;
  color: var(--lc-subtle);
  margin-top: 1px;
}

.mh-task-action {
  flex: 0 0 auto;
  border: 1px solid var(--lc-blue-border);
  border-radius: 999px;
  padding: 4px 11px;
  font-size: 11px;
  font-weight: 700;
  color: var(--lc-indigo);
  background: var(--lc-surface);
  text-decoration: none;
  white-space: nowrap;
}

.mh-task-action.done {
  border-color: var(--lc-green-light);
  color: var(--lc-emerald);
  background: var(--lc-emerald-light);
}

/* ── Settings card ───────────────────────────────────────── */
.mh-settings-card {
  padding: 0;
  overflow: hidden;
}

.mh-setting-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-bottom: 1px solid var(--lc-soft);
  background: var(--lc-surface);
  cursor: pointer;
  text-align: left;
  text-decoration: none;
  color: inherit;
  -webkit-tap-highlight-color: transparent;
}

.mh-setting-row:last-child {
  border-bottom: 0;
}

.mh-setting-row:active {
  background: var(--lc-bg);
}

.mh-setting-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
  flex: 0 0 auto;
  line-height: 1;
}

.mh-setting-label {
  flex: 1;
  font-size: 14px;
  color: var(--lc-text-deep);
  font-weight: 500;
}

.mh-setting-value {
  font-size: 12px;
  color: var(--lc-subtle);
}

.mh-setting-arrow {
  font-size: 20px;
  color: var(--lc-border);
  line-height: 1;
}

.mh-setting-danger .mh-setting-label {
  color: var(--lc-red);
}

/* ── Responsive tweaks (≤ 390 px, iPhone 13 等) ─────────── */
@media (max-width: 390px) {
  .mh-hero-inner {
    gap: 10px;
  }

  .mh-message-btn,
  .mh-edit-btn {
    font-size: 11px;
    padding: 5px 10px;
  }

  .mh-hero-name {
    font-size: 17px;
  }
}

/* ── Responsive tweaks (≤ 375 px) ───────────────────────── */
@media (max-width: 375px) {
  .me-mobile {
    padding-left: 10px;
    padding-right: 10px;
  }

  .mh-hero {
    padding: 16px 14px 14px;
  }

  .mh-avatar {
    width: 58px;
    height: 58px;
  }

  .mh-avatar-fb {
    font-size: 20px;
  }

  .mh-func-icon {
    width: 40px;
    height: 40px;
    font-size: 19px;
  }

  .mh-func-title {
    font-size: 11px;
  }
}
</style>

