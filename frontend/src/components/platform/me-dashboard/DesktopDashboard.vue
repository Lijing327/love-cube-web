<template>
  <section class="dash operation-shell" aria-label="平台个人工作台">
    <header class="me-hero operation-hero">
      <div class="hero-profile">
        <div class="avatar-wrap">
          <img v-if="avatarSrc" :src="avatarSrc" class="avatar" alt="头像">
          <div v-else class="avatar avatar-fallback" aria-hidden="true">{{ avatarFallback }}</div>
        </div>
        <div class="hero-main">
          <p class="section-kicker">Personal Workbench</p>
          <div class="name-row">
            <h1>{{ displayName }}</h1>
            <span class="status-badge info">{{ roleLabel }}</span>
            <span class="status-badge" :class="verifyBadgeClass">{{ verificationLabel }}</span>
          </div>
          <p class="hero-meta">
            UID {{ userIdDisplay }} · {{ locationDisplay || '未设置所在地' }}
          </p>
          <div class="completion-line">
            <div>
              <span>资料完整度</span>
              <strong>{{ profileCompletion }}%</strong>
            </div>
            <i aria-hidden="true"><b :style="{ width: `${profileCompletion}%` }"></b></i>
          </div>
          <div class="invite-line">
            <span>邀请码：<strong>{{ inviteCodeDisplay || '加载中' }}</strong></span>
            <button type="button" class="mini-btn" :disabled="!inviteCodeDisplay" @click="onCopyInvite">复制</button>
            <em v-if="copyFeedback" :class="{ 'is-error': copyFeedbackError }">{{ copyFeedback }}</em>
          </div>
          <div class="hero-actions">
            <button type="button" class="platform-btn platform-btn-primary" @click="onOpenEdit">编辑资料</button>
            <router-link class="platform-btn platform-btn-ghost" :to="messagesPath()">查看消息</router-link>
            <router-link class="platform-btn platform-btn-ghost" :to="myGroupsPath()">进入我的 Space</router-link>
            <router-link class="platform-btn platform-btn-ghost" to="/platform/my-activities">我的活动</router-link>
          </div>
        </div>
      </div>

      <aside class="hero-invite-qr" aria-label="邀请二维码">
        <InviteQrCode :invite-code="inviteCodeDisplay" :size="148" loading-text="生成中…" />
        <p class="qr-caption">扫码注册 · 已邀请 {{ inviteCount }} 人</p>
        <router-link class="qr-link" to="/fellowship/invite">邀请详情</router-link>
      </aside>

      <div class="metric-grid compact hero-metrics">
        <article v-for="item in heroMetrics" :key="item.label" class="metric-card">
          <span class="metric-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.hint }}</p>
        </article>
      </div>
    </header>

    <section class="dashboard-section">
      <div class="operation-section-head">
        <div>
          <p class="section-kicker">Today</p>
          <h2>今日状态</h2>
        </div>
        <button type="button" class="platform-btn platform-btn-ghost" @click="scrollToTasks">查看任务</button>
      </div>
      <div class="metric-grid today-grid">
        <article v-for="item in todayStatusItems" :key="item.label" class="metric-card" :class="item.tone">
          <span class="metric-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.hint }}</p>
        </article>
      </div>
    </section>

    <div class="dashboard-main-grid">
      <section ref="tasksPanelRef" class="section-card task-section">
        <div class="section-card-head">
          <div>
            <p class="section-kicker">Tasks</p>
            <h3>今日待办</h3>
          </div>
          <span class="status-badge neutral">{{ completedTaskCount }}/{{ dailyTaskTotal }} 已完成</span>
        </div>

        <div v-if="accountTasks.length" class="task-group">
          <div class="task-group-title">
            <strong>成长任务</strong>
            <span class="status-badge warning">待领取 {{ accountClaimableCount }}</span>
          </div>
          <article v-for="task in accountTasks.slice(0, 3)" :key="task.code" class="task-row">
            <span class="task-state" :class="{ done: task.completed }" aria-hidden="true"></span>
            <div>
              <strong>{{ task.title }}</strong>
              <small>+{{ task.exp }} 经验</small>
            </div>
            <button
              v-if="task.completed"
              type="button"
              class="platform-btn platform-btn-ghost task-action"
              :disabled="claimingAccountCode === task.code"
              @click="$emit('claim-account-task', task.code)"
            >
              {{ claimingAccountCode === task.code ? '领取中' : '领取' }}
            </button>
            <router-link v-else class="platform-btn platform-btn-ghost task-action" :to="task.to">去完成</router-link>
          </article>
        </div>

        <div class="task-group">
          <div class="task-group-title">
            <strong>每日任务</strong>
            <span class="status-badge info">每日刷新</span>
          </div>
          <article v-for="task in dailyTasks.slice(0, 4)" :key="task.title" class="task-row">
            <span class="task-state" :class="{ done: task.done }" aria-hidden="true"></span>
            <div>
              <strong>{{ task.title }}</strong>
              <small>{{ task.done ? '已完成' : `${task.current || 0}/${task.total || 1}` }} · +{{ task.exp }} 经验</small>
            </div>
            <router-link v-if="task.to && !task.done" class="platform-btn platform-btn-ghost task-action" :to="task.to">去完成</router-link>
            <span v-else class="status-badge success">完成</span>
          </article>
          <p v-if="!dailyTasks.length" class="empty-state compact">暂无今日任务</p>
        </div>
      </section>

      <section class="section-card assets-section">
        <div class="section-card-head">
          <div>
            <p class="section-kicker">Assets</p>
            <h3>我的资产</h3>
          </div>
          <span class="status-badge neutral">Lv.{{ growthLevel?.level || 1 }}</span>
        </div>
        <div class="asset-grid">
          <router-link v-for="item in assetItems" :key="item.label" class="asset-card" :to="item.to || '/'">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em>{{ item.hint }}</em>
          </router-link>
        </div>
      </section>

      <section class="section-card recent-section">
        <div class="section-card-head">
          <div>
            <p class="section-kicker">Recent</p>
            <h3>最近动态</h3>
          </div>
          <router-link class="platform-btn platform-btn-ghost" :to="contentPath()">内容流</router-link>
        </div>
        <div class="recent-list">
          <article v-for="item in recentItems" :key="item.label" class="recent-item">
            <span class="status-badge" :class="item.tone">{{ item.label }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.hint }}</p>
            </div>
          </article>
        </div>
      </section>
    </div>

    <section class="section-card quick-section">
      <div class="section-card-head">
        <div>
          <p class="section-kicker">Shortcuts</p>
          <h3>快捷入口</h3>
        </div>
      </div>
      <div class="action-grid quick-grid">
        <router-link
          v-for="item in normalizedQuickActions"
          :key="item.title"
          class="action-card"
          :to="item.to"
        >
          <div class="action-card-head">
            <strong>{{ item.title }}</strong>
            <span class="status-badge neutral">{{ item.tag }}</span>
          </div>
          <p>{{ item.desc }}</p>
        </router-link>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { toFullUrl } from '@/utils/image.js'
import { userAvatarUrlFromApi } from '@/utils/displayFields.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import InviteQrCode from '@/components/common/InviteQrCode.vue'

const props = defineProps({
  user: { type: Object, default: null },
  displayName: { type: String, required: true },
  userIdDisplay: { type: [String, Number], required: true },
  locationDisplay: { type: String, required: true },
  inviteCodeDisplay: { type: String, required: true },
  inviteCount: { type: Number, default: 0 },
  unreadCount: { type: Number, default: 0 },
  joinedSpaceCount: { type: Number, default: null },
  copyFeedback: { type: String, default: '' },
  copyFeedbackError: { type: Boolean, default: false },
  profileLightStats: { type: Array, required: true },
  growthLevel: { type: Object, required: true },
  growthProgress: { type: String, required: true },
  completedTaskCount: { type: Number, required: true },
  dailyTasks: { type: Array, required: true },
  accountTasks: { type: Array, default: () => [] },
  claimingAccountCode: { type: String, default: '' },
  overviewItems: { type: Array, required: true },
  groupInfo: { type: Object, required: true },
  groupRanking: { type: Array, required: true },
  quickActions: { type: Array, required: true },
  onOpenSettings: { type: Function, required: true },
  onOpenEdit: { type: Function, required: true },
  onCopyInvite: { type: Function, required: true },
})

defineEmits(['claim-account-task'])

const tasksPanelRef = ref(null)
const { myGroupsPath, messagesPath, contentPath } = usePlatformPath()

const dailyTaskTotal = computed(() => props.dailyTasks.length)
const accountClaimableCount = computed(() => props.accountTasks.filter((t) => t.completed).length)
const avatarFallback = computed(() => String(props.displayName || 'U').trim().slice(0, 1).toUpperCase())
const avatarSrc = computed(() => {
  const raw = userAvatarUrlFromApi(props.user)
  return raw ? toFullUrl(raw) : ''
})

const roleLabel = computed(() => {
  const role = String(props.user?.role || '').toLowerCase()
  if (role === 'admin' || role === 'super_admin' || role === 'root') return '平台管理员'
  return '普通用户'
})

const verificationStatus = computed(() => String(props.user?.verificationStatus || 'none').toLowerCase())
const verificationLabel = computed(() => {
  if (verificationStatus.value === 'approved') return '账号已认证'
  if (verificationStatus.value === 'pending') return '认证审核中'
  if (verificationStatus.value === 'rejected') return '认证未通过'
  return '账号未认证'
})
const verifyBadgeClass = computed(() => {
  if (verificationStatus.value === 'approved') return 'success'
  if (verificationStatus.value === 'pending') return 'warning'
  if (verificationStatus.value === 'rejected') return 'danger'
  return 'neutral'
})

const profileCompletion = computed(() => {
  const checks = [
    props.user?.username || props.user?.nickname,
    userAvatarUrlFromApi(props.user),
    props.user?.location,
    props.user?.bio,
    props.user?.phone,
    verificationStatus.value === 'approved'
  ]
  const done = checks.filter(Boolean).length
  return Math.round((done / checks.length) * 100)
})

const joinedSpaces = computed(() => {
  if (props.joinedSpaceCount != null) return Number(props.joinedSpaceCount || 0)
  const name = String(props.groupInfo?.name || '')
  if (!name || name.includes('暂未') || name === '--') return 0
  return 1
})

const heroMetrics = computed(() => [
  { label: '资料完整度', value: `${profileCompletion.value}%`, hint: '头像、简介与认证' },
  { label: '加入 Space', value: joinedSpaces.value, hint: '我参与的团体' },
  { label: '未读消息', value: props.unreadCount, hint: props.unreadCount > 0 ? '需要处理' : '暂无未读' },
  { label: '今日任务', value: `${props.completedTaskCount}/${dailyTaskTotal.value}`, hint: '每日成长任务' }
])

const todayStatusItems = computed(() => [
  { label: '今日待办', value: Math.max(0, dailyTaskTotal.value - props.completedTaskCount), hint: '待完成任务', tone: 'warn' },
  { label: '未读通知', value: props.unreadCount, hint: props.unreadCount > 0 ? '请及时查看' : '状态清爽', tone: props.unreadCount > 0 ? 'warn' : '' },
  { label: '成长任务', value: accountClaimableCount.value, hint: '可领取奖励', tone: accountClaimableCount.value > 0 ? 'accent' : '' },
  { label: '最近 Space', value: props.groupInfo?.name || '暂无', hint: props.groupInfo?.role || '最近参与', tone: '' }
])

const assetItems = computed(() => {
  const map = new Map((props.overviewItems || []).map((item) => [item.label, item]))
  return [
    { label: '我的 Space', value: joinedSpaces.value, hint: props.groupInfo?.name || '查看团体', to: myGroupsPath() },
    { label: '我的内容', value: map.get('发布内容')?.value ?? 0, hint: '发布与管理', to: map.get('发布内容')?.to || contentPath() },
    { label: '我的活动', value: map.get('活动参与')?.value ?? 0, hint: '报名与参与记录', to: map.get('活动参与')?.to || '/' },
    { label: '收藏/记录', value: map.get('心声收藏')?.value ?? 0, hint: '收藏与点赞', to: map.get('心声收藏')?.to || contentPath() },
    { label: '成长等级', value: `Lv.${props.growthLevel?.level || 1}`, hint: `${props.growthLevel?.currentExp || 0}/${props.growthLevel?.nextExp || 100} 经验`, to: '/' }
  ]
})

const recentItems = computed(() => {
  const firstTask = props.dailyTasks.find((task) => task.done) || props.dailyTasks[0]
  const firstRank = props.groupRanking?.[0]
  return [
    { label: '最近发布', tone: 'info', title: `累计发布 ${assetItems.value[1].value} 条内容`, hint: '从内容中心继续管理你的表达' },
    { label: '最近评论', tone: 'neutral', title: `互动热度 ${findOverviewValue('互动热度')}`, hint: '评论、点赞与内容互动会在这里汇总' },
    { label: '最近打卡', tone: firstTask?.done ? 'success' : 'warning', title: firstTask ? firstTask.title : '暂无打卡记录', hint: firstTask?.done ? '今日已完成一项任务' : '今天还可以继续完成任务' },
    { label: '最近加入', tone: 'info', title: props.groupInfo?.name || '暂无 Space', hint: firstRank ? `热门 Space：${firstRank.name}` : '去发现更多 Space' }
  ]
})

const normalizedQuickActions = computed(() =>
  props.quickActions.slice(0, 6).map((item) => ({
    ...item,
    tag: item.title.includes('设置') ? '账号' : item.title.includes('通知') ? '消息' : '入口'
  }))
)

function findOverviewValue(label) {
  const item = props.overviewItems.find((row) => row.label === label)
  return item?.value ?? '--'
}

function scrollToTasks() {
  const el = tasksPanelRef.value
  if (el && typeof el.scrollIntoView === 'function') {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}
</script>

<style scoped>
.dash {
  width: min(100% - 40px, 1720px);
  margin: 0 auto;
  padding: 0 0 var(--lc-space-6);
  color: var(--lc-text);
}

.me-hero {
  align-items: stretch;
  grid-template-columns: minmax(0, 1fr) 168px 430px;
}

.hero-invite-qr {
  display: grid;
  gap: var(--lc-space-2);
  align-content: start;
  justify-items: center;
  width: 168px;
}

.qr-caption {
  margin: 0;
  color: var(--lc-muted);
  font-size: 11px;
  line-height: 1.4;
  text-align: center;
}

.qr-link {
  color: var(--lc-blue);
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
}

.qr-link:hover {
  text-decoration: underline;
}

.hero-profile {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: var(--lc-space-5);
  min-width: 0;
}

.avatar-wrap,
.avatar {
  width: 92px;
  height: 92px;
}

.avatar {
  display: block;
  border: 1px solid var(--lc-border);
  border-radius: 50%;
  object-fit: cover;
}

.avatar-fallback {
  display: grid;
  place-items: center;
  color: var(--lc-blue);
  background: var(--lc-blue-light);
  font-size: 30px;
  font-weight: 900;
}

.hero-main {
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
}

.name-row h1 {
  margin: 0;
  color: var(--lc-text);
  font-size: 30px;
  line-height: 1.15;
}

.hero-meta,
.invite-line,
.completion-line span,
.completion-line strong,
.recent-item p,
.asset-card em,
.task-row small,
.action-card p {
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
}

.hero-meta {
  margin: var(--lc-space-2) 0 0;
}

.completion-line {
  display: grid;
  gap: var(--lc-space-2);
  max-width: 520px;
  margin-top: var(--lc-space-4);
}

.completion-line > div,
.invite-line,
.hero-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
}

.completion-line strong {
  color: var(--lc-text);
  font-weight: 900;
}

.completion-line i {
  overflow: hidden;
  display: block;
  height: 8px;
  border-radius: 999px;
  background: var(--lc-soft);
}

.completion-line b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--lc-blue);
}

.invite-line {
  margin-top: var(--lc-space-3);
}

.invite-line strong {
  color: var(--lc-text);
}

.invite-line em {
  color: var(--lc-green);
  font-size: var(--lc-text-xs);
  font-style: normal;
  font-weight: 900;
}

.invite-line em.is-error {
  color: var(--lc-red);
}

.mini-btn {
  min-height: 28px;
  padding: 0 var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-blue);
  background: var(--lc-surface);
  font-weight: 900;
  cursor: pointer;
}

.mini-btn:disabled {
  cursor: not-allowed;
  opacity: .55;
}

.hero-actions {
  margin-top: var(--lc-space-4);
}

.hero-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 0;
}

.dashboard-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, .95fr);
  gap: var(--lc-space-4);
}

.recent-section {
  grid-column: 1 / -1;
}

.task-section,
.assets-section,
.recent-section,
.quick-section {
  display: grid;
  gap: var(--lc-space-4);
}

.task-group {
  display: grid;
  gap: var(--lc-space-3);
}

.task-group-title,
.task-row,
.recent-item {
  display: flex;
  align-items: center;
  gap: var(--lc-space-3);
}

.task-group-title {
  justify-content: space-between;
}

.task-row {
  min-width: 0;
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-soft);
}

.task-row > div {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 2px;
}

.task-row strong,
.recent-item strong,
.action-card strong,
.asset-card strong {
  min-width: 0;
  overflow: hidden;
  color: var(--lc-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-state {
  width: 12px;
  height: 12px;
  border: 2px solid var(--lc-border);
  border-radius: 999px;
  background: var(--lc-surface);
}

.task-state.done {
  border-color: var(--lc-green);
  background: var(--lc-green);
}

.task-action {
  flex: 0 0 auto;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--lc-space-3);
}

.asset-card,
.recent-item {
  min-width: 0;
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-soft);
  text-decoration: none;
}

.asset-card {
  display: grid;
  gap: var(--lc-space-1);
}

.asset-card span {
  color: var(--lc-muted);
  font-size: var(--lc-text-xs);
  font-weight: 900;
}

.asset-card strong {
  font-size: 24px;
  line-height: 1;
}

.recent-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--lc-space-3);
}

.recent-item > div {
  min-width: 0;
}

.recent-item p,
.action-card p {
  margin: 4px 0 0;
}

.quick-grid {
  margin-top: 0;
}

.action-card {
  color: inherit;
  text-decoration: none;
}

.empty-state.compact {
  min-height: 88px;
  margin: 0;
}

@media (max-width: 1180px) {
  .dash {
    width: min(100% - 24px, 1180px);
  }

  .me-hero {
    grid-template-columns: 1fr;
  }

  .hero-invite-qr {
    width: 100%;
    max-width: 200px;
    margin: 0 auto;
  }

  .dashboard-main-grid,
  .recent-list {
    grid-template-columns: 1fr;
  }

  .asset-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .hero-metrics,
  .today-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .hero-metrics .metric-card p,
  .today-grid .metric-card p {
    display: none;
  }
}
</style>
