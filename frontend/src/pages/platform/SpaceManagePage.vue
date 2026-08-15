<template>
  <section class="space-manage-page operation-shell">
    <div v-if="redirecting" class="state-card loading-state">正在跳转兼容管理页…</div>

    <template v-else-if="loading">
      <div class="state-card loading-state">加载社区空间中…</div>
    </template>

    <template v-else-if="forbidden">
      <div class="state-card err">
        <h2>无权访问运营台</h2>
        <p>仅社区负责人或管理员可进入运营台。</p>
        <router-link class="btn ghost" :to="groupsPath(String(spaceId))">返回社区主页</router-link>
      </div>
    </template>

    <template v-else-if="loadError">
      <div class="state-card err">
        <h2>加载失败</h2>
        <p>{{ loadError }}</p>
        <button type="button" class="btn primary" @click="bootstrap">重试</button>
      </div>
    </template>

    <template v-else-if="group">
      <div v-if="onboardingActive" class="onboarding-panel platform-card">
        <div class="onboarding-head">
          <strong>欢迎，开始运营你的社区空间</strong>
          <span class="step-label">步骤 {{ onboardingStep }} / 3</span>
        </div>
        <p class="onboarding-hint">{{ onboardingHints[onboardingStep - 1] }}</p>

        <div v-if="onboardingStep === 1" class="onboarding-body">
          <label class="field">
            <span>社区名称</span>
            <input v-model.trim="settingsForm.name" type="text" maxlength="100">
          </label>
          <label class="field">
            <span>简介</span>
            <textarea v-model.trim="settingsForm.description" rows="4" maxlength="2000" />
          </label>
          <label class="field">
            <span>地区（可选）</span>
            <input v-model.trim="settingsForm.region" type="text" maxlength="50">
          </label>
          <button type="button" class="btn primary" :disabled="saving" @click="saveSettingsAndNext">
            {{ saving ? '保存中…' : '保存并继续' }}
          </button>
        </div>

        <div v-else-if="onboardingStep === 2" class="onboarding-body">
          <label class="field">
            <span>公告标题</span>
            <input v-model.trim="noticeForm.title" type="text" maxlength="120" placeholder="例如：欢迎加入本社区">
          </label>
          <label class="field">
            <span>公告内容</span>
            <textarea v-model.trim="noticeForm.content" rows="5" maxlength="5000" placeholder="介绍社区规则与近期安排" />
          </label>
          <div class="onboarding-actions">
            <button type="button" class="btn ghost" @click="onboardingStep = 3">稍后发布</button>
            <button type="button" class="btn primary" :disabled="saving" @click="publishFirstNotice">
              {{ saving ? '发布中…' : '发布公告并继续' }}
            </button>
          </div>
        </div>

        <div v-else class="onboarding-body">
          <p class="share-hint">把下面的链接发给伙伴，邀请他们加入社区。</p>
          <div class="share-box">
            <code>{{ inviteShareUrl }}</code>
            <button type="button" class="btn secondary" @click="copyInviteLink">复制链接</button>
          </div>
          <p v-if="inviteCode" class="meta-line">邀请码：<strong>{{ inviteCode }}</strong></p>
          <button type="button" class="btn primary" @click="finishOnboarding">进入运营台</button>
        </div>
      </div>

      <header class="operation-hero">
        <div class="hero-main">
          <div class="hero-topline">
            <router-link class="back-link" :to="groupsPath()">← 我的社区</router-link>
            <span class="status-badge info">Space 运营控制台</span>
          </div>
          <div class="title-row">
            <img
              v-if="heroCoverUrl"
              :src="heroCoverUrl"
              :alt="group.name"
              class="cover-thumb"
              @error="onHeroCoverError"
            >
            <div v-else class="cover-thumb cover-fallback">{{ group.name?.slice(0, 1) || 'S' }}</div>
            <div class="title-block">
              <div class="title-meta">
                <span class="status-badge">{{ roleLabel }}</span>
                <span class="status-badge" :class="groupStatusTone">{{ groupStatusLabel }}</span>
                <span class="status-badge neutral">加入方式：{{ groupJoinModeLabel }}</span>
              </div>
              <h1>{{ group.name }}</h1>
              <p class="subtitle">面向成员增长、内容活跃、活动组织和打卡营的日常运营工作台。</p>
            </div>
          </div>
          <div class="hero-actions-mobile">
            <router-link class="btn ghost" :to="groupsPath(String(spaceId))">查看主页</router-link>
            <button type="button" class="btn primary" @click="switchTab('overview')">看运营概览</button>
          </div>
        </div>

        <div class="hero-side">
          <div class="hero-stats" aria-label="Space 摘要">
            <article class="hero-stat-card primary">
              <span>成员数</span>
              <strong>{{ group.memberCount ?? 0 }}</strong>
              <small>当前 Space 规模</small>
            </article>
            <article class="hero-stat-card" :class="{ warning: pendingCount > 0 }">
              <span>待审核</span>
              <strong :class="{ warn: pendingCount > 0 }">{{ pendingCount }}</strong>
              <small>{{ pendingCount > 0 ? '需要及时处理' : '暂无待处理' }}</small>
            </article>
            <article class="hero-stat-card">
              <span>运营状态</span>
              <strong>{{ groupStatusLabel }}</strong>
              <small>{{ roleLabel }}权限</small>
            </article>
          </div>
          <div class="hero-task-strip" :class="{ urgent: pendingCount > 0 }">
            <span>{{ pendingCount > 0 ? '当前有成员申请等待审核' : '当前无成员审核积压' }}</span>
            <button type="button" class="link-button" @click="switchTab('members')">
              {{ pendingCount > 0 ? '去审核' : '查看成员' }}
            </button>
          </div>
          <div class="head-actions">
            <router-link class="btn ghost" :to="groupsPath(String(spaceId))">查看主页</router-link>
            <button type="button" class="btn primary" @click="switchTab('overview')">看运营概览</button>
          </div>
        </div>
      </header>

      <p v-if="flash" class="flash" :class="{ err: flashType === 'error' }">{{ flash }}</p>

      <nav class="manage-tabs" aria-label="运营分区">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          :class="{ active: activeTab === tab.key, urgent: tab.key === 'members' && pendingCount }"
          @click="switchTab(tab.key)"
        >
          <span class="tab-label">
            {{ tab.label }}
            <span v-if="tab.key === 'members' && pendingCount" class="badge">{{ pendingCount }}</span>
            <span v-if="tab.key === 'activity-review' && pendingActivityCount" class="badge">{{ pendingActivityCount }}</span>
          </span>
          <small>{{ tab.desc }}</small>
        </button>
      </nav>

      <!-- 概览 -->
      <section v-if="activeTab === 'overview'" class="tab-panel overview-tab-panel">
        <SpaceManageOverviewPanel
          :stats="spaceStats"
          :loading="loadingSpaceStats"
          :error="spaceStatsError"
          @refresh="loadSpaceStats"
          @navigate="switchTab"
        />
      </section>

      <!-- 成员 -->
      <section v-else-if="activeTab === 'members'" class="platform-card tab-panel">
        <div class="section-head operation-section-head">
          <div>
            <p class="section-kicker">Members</p>
            <h2>成员管理</h2>
          </div>
          <div class="filter-row">
            <button
              v-for="opt in memberFilters"
              :key="opt.value"
              type="button"
              :class="{ active: memberStatus === opt.value }"
              @click="loadMembers(opt.value)"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>

        <div class="member-summary-grid">
          <article class="metric-card">
            <span class="metric-label">成员总数</span>
            <strong>{{ memberTotalCount }}</strong>
            <p>当前 Space 成员规模</p>
          </article>
          <article class="metric-card accent">
            <span class="metric-label">活跃成员</span>
            <strong>{{ activeMemberCount }}</strong>
            <p>基于当前列表最近活跃记录</p>
          </article>
          <article class="metric-card">
            <span class="metric-label">最近加入</span>
            <strong>{{ latestJoinedMemberName }}</strong>
            <p>{{ latestJoinedMemberTime }}</p>
          </article>
          <article class="metric-card">
            <span class="metric-label">管理员数量</span>
            <strong>{{ adminMemberCount }}</strong>
            <p>负责人和管理员</p>
          </article>
        </div>

        <div v-if="loadingMembers" class="inline-state">加载中…</div>
        <div v-else-if="members.length" class="member-table">
          <div class="member-table-head">
            <span></span>
            <span>成员</span>
            <span>角色</span>
            <span>加入时间</span>
            <span>最近活跃</span>
            <span>状态</span>
            <span>操作</span>
          </div>
          <article v-for="m in members" :key="m.id" class="member-row">
            <img :src="m.avatarUrl || defaultAvatar" :alt="m.username">
            <div class="member-main">
              <strong>{{ displayMemberName(m) }}</strong>
              <p>{{ m.username || `用户 ${m.userId}` }}</p>
              <p v-if="m.status === 'pending' && m.applyReason" class="apply-reason">申请说明：{{ m.applyReason }}</p>
            </div>
            <div class="member-tags">
              <span class="tag" :class="m.role">{{ roleTag(m.role) }}</span>
            </div>
            <span class="member-meta">{{ formatDate(m.joinedAt || m.requestedAt) }}</span>
            <span class="member-meta">{{ memberLastActive(m) }}</span>
            <span class="status-badge" :class="memberStatusTone(m.status)">{{ memberStatusLabel(m.status) }}</span>
            <div class="member-actions">
              <template v-if="m.status === 'pending'">
                <button type="button" class="btn primary sm" :disabled="saving" @click="approve(m)">通过</button>
                <button type="button" class="btn danger sm" :disabled="saving" @click="reject(m)">拒绝</button>
              </template>
              <button
                v-else-if="m.role !== 'owner' && m.status === 'approved'"
                type="button"
                class="btn danger sm"
                :disabled="saving"
                @click="removeMember(m)"
              >
                移除
              </button>
            </div>
          </article>
        </div>
        <p v-else class="inline-state">暂无成员记录</p>
      </section>

      <!-- 审核 -->
      <section v-else-if="activeTab === 'review'" class="platform-card tab-panel review-panel">
        <div class="section-head operation-section-head">
          <div>
            <p class="section-kicker">Review Center</p>
            <h2>成员审核</h2>
          </div>
          <button type="button" class="btn secondary sm" :disabled="loadingMembers" @click="loadMembers('pending')">
            刷新申请
          </button>
        </div>

        <div class="member-summary-grid review-summary">
          <article class="metric-card warn">
            <span class="metric-label">待审核人数</span>
            <strong>{{ pendingCount }}</strong>
            <p>需要运营处理</p>
          </article>
          <article class="metric-card">
            <span class="metric-label">今日新增申请</span>
            <strong>{{ todayPendingCount }}</strong>
            <p>按申请时间统计</p>
          </article>
          <article class="metric-card accent">
            <span class="metric-label">已处理数量</span>
            <strong>{{ reviewedCount }}</strong>
            <p>本次打开页面后处理</p>
          </article>
        </div>

        <div v-if="loadingMembers" class="inline-state">加载中…</div>
        <div v-else-if="members.length" class="review-list">
          <article v-for="m in members" :key="m.id" class="review-card risk-card warning">
            <img :src="m.avatarUrl || defaultAvatar" :alt="m.username">
            <div class="review-main">
              <div class="review-title">
                <strong>{{ displayMemberName(m) }}</strong>
                <span class="status-badge warning">待处理</span>
              </div>
              <p class="member-meta">申请时间：{{ formatDate(m.requestedAt || m.joinedAt) }}</p>
              <p class="apply-reason">{{ m.applyReason || '暂无申请原因' }}</p>
            </div>
            <div class="review-actions">
              <button type="button" class="btn primary sm" :disabled="saving" @click="approve(m)">通过</button>
              <button type="button" class="btn danger sm" :disabled="saving" @click="reject(m)">拒绝</button>
            </div>
          </article>
        </div>
        <p v-else class="inline-state empty-state">暂无待审核申请</p>
      </section>

      <!-- 公告 -->
      <section v-else-if="activeTab === 'notices'" class="platform-card tab-panel">
        <div class="section-head">
          <h2>公告</h2>
        </div>
        <form class="inline-form" @submit.prevent="submitNotice">
          <label class="field">
            <span>标题</span>
            <input v-model.trim="noticeForm.title" type="text" maxlength="120" required>
          </label>
          <label class="field">
            <span>内容</span>
            <textarea v-model.trim="noticeForm.content" rows="4" maxlength="5000" required />
          </label>
          <button type="submit" class="btn primary" :disabled="saving">{{ saving ? '发布中…' : '发布公告' }}</button>
        </form>
        <div v-if="loadingNotices" class="inline-state">加载中…</div>
        <div v-else-if="notices.length" class="content-list">
          <article v-for="n in notices" :key="n.id" class="content-row">
            <header>
              <strong>{{ n.title }}</strong>
              <time>{{ formatDate(n.createdAt) }}</time>
            </header>
            <p>{{ n.content }}</p>
          </article>
        </div>
        <p v-else class="inline-state">暂无公告，发布第一条吧</p>
      </section>

      <!-- 文章 -->
      <section v-else-if="activeTab === 'articles'" class="platform-card tab-panel">
        <div class="section-head">
          <h2>团体文章</h2>
        </div>
        <form class="inline-form" @submit.prevent="submitArticle">
          <label class="field">
            <span>标题</span>
            <input v-model.trim="articleForm.title" type="text" maxlength="200" required>
          </label>
          <label class="field">
            <span>正文</span>
            <textarea v-model.trim="articleForm.content" rows="6" maxlength="20000" required />
          </label>
          <label class="field">
            <span>封面 URL（可选）</span>
            <input v-model.trim="articleForm.coverUrl" type="text" maxlength="512">
          </label>
          <button type="submit" class="btn primary" :disabled="saving">{{ saving ? '发布中…' : '发布文章' }}</button>
        </form>
        <div v-if="loadingArticles" class="inline-state">加载中…</div>
        <div v-else-if="articles.length" class="content-list">
          <article v-for="a in articles" :key="a.id" class="content-row">
            <header>
              <strong>{{ a.title }}</strong>
              <time>{{ formatDate(a.createdAt) }}</time>
            </header>
            <p>{{ a.summary || a.content }}</p>
          </article>
        </div>
        <p v-else class="inline-state">暂无文章，发布第一篇吧</p>
      </section>

      <!-- 活动 -->
      <section v-else-if="activeTab === 'activities'" class="platform-card tab-panel">
        <div class="section-head">
          <h2>活动</h2>
        </div>
        <form class="inline-form" @submit.prevent="submitActivity">
          <label class="field">
            <span>活动标题</span>
            <input v-model.trim="activityForm.title" type="text" maxlength="200" required>
          </label>
          <label class="field">
            <span>简介</span>
            <textarea v-model.trim="activityForm.description" rows="3" maxlength="2000" />
          </label>
          <div class="field-row">
            <label class="field">
              <span>开始时间</span>
              <input v-model="activityForm.startTime" type="datetime-local" required>
            </label>
            <label class="field">
              <span>结束时间</span>
              <input v-model="activityForm.endTime" type="datetime-local" required>
            </label>
          </div>
          <label class="field">
            <span>地点（可选）</span>
            <input v-model.trim="activityForm.location" type="text" maxlength="200">
          </label>
          <button type="submit" class="btn primary" :disabled="saving">{{ saving ? '创建中…' : '创建活动' }}</button>
        </form>
        <div v-if="loadingActivities" class="inline-state">加载中…</div>
        <div v-else-if="activities.length" class="content-list">
          <article v-for="a in activities" :key="a.id" class="content-row">
            <header>
              <strong>{{ a.title }}</strong>
              <time>{{ formatDate(a.startTime) }}</time>
            </header>
            <p>{{ a.description || '暂无简介' }}</p>
            <p class="meta-line">{{ a.participantCount ?? 0 }} 人报名 · {{ a.location || '线上/待定' }}</p>
          </article>
        </div>
        <p v-else class="inline-state">暂无活动</p>
      </section>

      <!-- 活动审核 -->
      <section v-else-if="activeTab === 'activity-review'" class="tab-panel">
        <SpaceActivityReviewPanel
          ref="activityReviewPanelRef"
          :group-id="spaceId"
          @flash="onCampFlash"
          @updated="onActivityReviewUpdated"
        />
      </section>

      <!-- 打卡营 -->
      <section v-else-if="activeTab === 'camp'" class="tab-panel">
        <SpaceCampaignManagePanel :group-id="spaceId" @flash="onCampFlash" />
      </section>

      <!-- 任务 -->
      <section v-else-if="activeTab === 'tasks'" class="platform-card tab-panel">
        <h2>每日任务</h2>
        <p class="hint">以下为平台预设的社区每日任务，成员完成后可获得成长值。任务规则配置将在后续版本开放。</p>
        <ul class="task-list">
          <li v-for="task in taskCatalog" :key="task.code">
            <div>
              <strong>{{ task.name }}</strong>
              <span class="meta-line">奖励 {{ task.rewardExp }} 成长值</span>
            </div>
            <span class="tag">系统预设</span>
          </li>
        </ul>
      </section>

      <!-- 设置 -->
      <section v-else-if="activeTab === 'settings'" class="platform-card tab-panel">
        <h2>社区设置</h2>
        <form class="settings-form" @submit.prevent="saveSettings">
          <label class="field">
            <span>社区名称</span>
            <input v-model.trim="settingsForm.name" type="text" maxlength="100" required>
          </label>
          <label class="field">
            <span>分类</span>
            <select v-model="settingsForm.type">
              <option v-for="c in categoryOptions" :key="c.value" :value="c.value">{{ c.label }}</option>
            </select>
          </label>
          <label class="field">
            <span>地区</span>
            <input v-model.trim="settingsForm.region" type="text" maxlength="50">
          </label>
          <label class="field">
            <span>加入方式</span>
            <select v-model="settingsForm.joinMode">
              <option value="open">公开加入</option>
              <option value="audit">审核加入</option>
              <option value="invite">仅限邀请</option>
            </select>
          </label>
          <label class="field">
            <span>封面图 URL</span>
            <input v-model.trim="settingsForm.coverUrl" type="url" maxlength="512" placeholder="可选">
          </label>
          <label class="field">
            <span>简介</span>
            <textarea v-model.trim="settingsForm.description" rows="5" maxlength="2000" />
          </label>
          <label class="field">
            <span>标签（逗号分隔，可选）</span>
            <input v-model.trim="settingsForm.tags" type="text" maxlength="120">
          </label>
          <button type="submit" class="btn primary" :disabled="saving">{{ saving ? '保存中…' : '保存设置' }}</button>
        </form>
        <div v-if="inviteCode" class="invite-block">
          <h3>邀请信息</h3>
          <p class="meta-line">邀请码：<strong>{{ inviteCode }}</strong></p>
          <div class="share-box">
            <code>{{ inviteShareUrl }}</code>
            <button type="button" class="btn secondary sm" @click="copyInviteLink">复制链接</button>
          </div>
        </div>
      </section>
    </template>

    <div v-else class="state-card err">
      <h2>暂时无法打开运营台</h2>
      <p>团体信息未加载完成，请返回重试。</p>
      <router-link class="btn ghost" :to="groupsPath()">返回我的团体</router-link>
      <button type="button" class="btn primary" @click="bootstrap">重新加载</button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  approveMember,
  createGroupActivity,
  createGroupNotice,
  createGroupArticle,
  fetchGroupArticles,
  fetchGroupActivities,
  fetchGroupDetail,
  fetchGroupMembers,
  fetchGroupNotices,
  rejectMember,
  resolveSpaceManageEntry,
  removeGroupMember,
  updateGroup
} from '@/api/groups.js'
import { fetchPendingActivityProposals } from '@/api/groupActivityProposals.js'
import { fetchSpaceStats } from '@/api/spaceStats.js'
import SpaceActivityReviewPanel from '@/components/platform/spaces/SpaceActivityReviewPanel.vue'
import SpaceCampaignManagePanel from '@/components/platform/spaces/SpaceCampaignManagePanel.vue'
import SpaceManageOverviewPanel from '@/components/platform/spaces/SpaceManageOverviewPanel.vue'
import { PLATFORM_GROUP_TASK_CATALOG } from '@/constants/platformGroupTasks.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import { useUserStore } from '@/stores/user.js'
import { PLATFORM_GROUP_CATEGORY_OPTIONS } from '@/utils/groupCategories.js'
import { groupCoverUrlFromApi } from '@/utils/displayFields.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { groupsPath } = usePlatformPath()

const spaceId = computed(() => String(route.params.id || ''))
const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=LC&backgroundColor=eff6ff'

const loading = ref(true)
const loadError = ref('')
const forbidden = ref(false)
const redirecting = ref(false)
const group = ref(null)
const saving = ref(false)
const flash = ref('')
const flashType = ref('success')

const activeTab = ref(normalizeTab(route.query.tab))
const pendingCount = ref(0)
const pendingActivityCount = ref(0)
const activityReviewPanelRef = ref(null)
const spaceStats = ref(null)
const loadingSpaceStats = ref(false)
const spaceStatsError = ref('')

const members = ref([])
const memberStatus = ref('approved')
const loadingMembers = ref(false)
const reviewedCount = ref(0)

const notices = ref([])
const loadingNotices = ref(false)
const noticeForm = reactive({ title: '', content: '' })

const articles = ref([])
const loadingArticles = ref(false)
const articleForm = reactive({ title: '', content: '', coverUrl: '' })

const activities = ref([])
const loadingActivities = ref(false)
const activityForm = reactive({
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  location: ''
})

const settingsForm = reactive({
  name: '',
  type: 'interest',
  region: '',
  joinMode: 'audit',
  coverUrl: '',
  description: '',
  tags: ''
})

const inviteCode = ref('')
const heroCoverUrl = ref('')
const onboardingStep = ref(1)
const taskCatalog = PLATFORM_GROUP_TASK_CATALOG
const categoryOptions = PLATFORM_GROUP_CATEGORY_OPTIONS

const onboardingActive = computed(() => route.query.onboarding === '1')
const onboardingHints = [
  '先完善社区名称与简介，让成员一眼看懂你的社区定位。',
  '发布第一条公告，告诉大家社区要做什么。',
  '复制邀请链接，把第一批成员拉进来。'
]

const tabs = [
  { key: 'overview', label: '运营概览', desc: '指标与风险' },
  { key: 'members', label: '成员管理', desc: '角色与活跃' },
  { key: 'review', label: '成员审核', desc: '准入待办' },
  { key: 'notices', label: '公告触达', desc: '同步安排' },
  { key: 'articles', label: '团体文章', desc: '成员长文' },
  { key: 'activities', label: '活动运营', desc: '线下/线上' },
  { key: 'activity-review', label: '活动审核', desc: '成员投稿' },
  { key: 'camp', label: '打卡营', desc: '进度跟进' },
  { key: 'tasks', label: '任务管理', desc: '行动执行' },
  { key: 'settings', label: 'Space 设置', desc: '资料与权限' }
]

const memberFilters = [
  { label: '已加入', value: 'approved' },
  { label: '待审核', value: 'pending' },
  { label: '全部', value: 'all' }
]

const roleLabel = computed(() => {
  const r = group.value?.myRole
  if (r === 'owner') return '负责人'
  if (r === 'admin') return '管理员'
  return '运营者'
})

const groupStatusLabel = computed(() => {
  const status = String(group.value?.status || '').toLowerCase()
  if (status === 'inactive' || status === 'offline') return '已停用'
  if (status === 'pending') return '待审核'
  if (status === 'rejected') return '未通过'
  return '运行中'
})

const groupStatusTone = computed(() => {
  const status = String(group.value?.status || '').toLowerCase()
  if (status === 'inactive' || status === 'offline' || status === 'rejected') return 'danger'
  if (status === 'pending') return 'warning'
  return 'success'
})

const groupJoinModeLabel = computed(() => {
  const mode = group.value?.joinModeKey || group.value?.joinMode || settingsForm.joinMode
  if (mode === 'open') return '公开'
  if (mode === 'invite') return '邀请'
  return '审核'
})

const memberTotalCount = computed(() => group.value?.memberCount ?? members.value.length)
const activeMemberCount = computed(() => members.value.filter((m) => hasRecentActivity(m)).length)
const adminMemberCount = computed(() =>
  members.value.filter((m) => ['owner', 'admin'].includes(String(m.role || '').toLowerCase())).length
)
const latestJoinedMember = computed(() => {
  return [...members.value]
    .filter((m) => m.joinedAt || m.requestedAt)
    .sort((a, b) => new Date(b.joinedAt || b.requestedAt) - new Date(a.joinedAt || a.requestedAt))[0]
})
const latestJoinedMemberName = computed(() => latestJoinedMember.value ? displayMemberName(latestJoinedMember.value) : '—')
const latestJoinedMemberTime = computed(() => latestJoinedMember.value ? formatDate(latestJoinedMember.value.joinedAt || latestJoinedMember.value.requestedAt) : '暂无加入记录')
const todayPendingCount = computed(() => {
  const today = new Date().toDateString()
  return members.value.filter((m) => {
    if (m.status !== 'pending') return false
    const raw = m.requestedAt || m.joinedAt
    if (!raw) return false
    const d = new Date(raw)
    return !Number.isNaN(d.getTime()) && d.toDateString() === today
  }).length
})

const inviteShareUrl = computed(() => {
  const base = typeof window !== 'undefined' ? window.location.origin : ''
  const path = groupsPath(String(spaceId.value))
  if (inviteCode.value) {
    return `${base}/#${path}?invite=${inviteCode.value}`
  }
  return `${base}/#${path}`
})

function normalizeTab(raw) {
  const key = typeof raw === 'string' ? raw : 'overview'
  return tabs.some((t) => t.key === key) ? key : 'overview'
}

function switchTab(key) {
  activeTab.value = key
  router.replace({ query: { ...route.query, tab: key } })
  if (key === 'overview') loadSpaceStats()
  if (key === 'members') loadMembers(memberStatus.value)
  if (key === 'review') loadMembers('pending')
  if (key === 'notices') loadNotices()
  if (key === 'articles') loadArticles()
  if (key === 'activities') loadActivities()
  if (key === 'activity-review') loadPendingActivityCount()
}

function showFlash(msg, type = 'success') {
  flash.value = msg
  flashType.value = type
  window.setTimeout(() => { flash.value = '' }, 4000)
}

function onCampFlash(msg, type = 'success') {
  showFlash(msg, type)
}

function onActivityReviewUpdated(count) {
  pendingActivityCount.value = Number(count || 0)
  loadSpaceStats()
}

async function loadPendingActivityCount() {
  try {
    const res = await fetchPendingActivityProposals(spaceId.value)
    const data = res?.data ?? res
    pendingActivityCount.value = Number(data?.pendingCount ?? data?.total ?? 0)
  } catch {
    pendingActivityCount.value = 0
  }
}

function formatDate(v) {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  return d.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function displayMemberName(m) {
  return m.memberRealName || m.username || `用户 ${m.userId}`
}

function roleTag(role) {
  if (role === 'owner') return '负责人'
  if (role === 'admin') return '管理员'
  return '成员'
}

function memberStatusLabel(status) {
  if (status === 'pending') return '待审核'
  if (status === 'approved') return '已加入'
  if (status === 'rejected') return '已拒绝'
  return status || '—'
}

function memberStatusTone(status) {
  if (status === 'pending') return 'warning'
  if (status === 'approved') return 'success'
  if (status === 'rejected') return 'danger'
  return 'neutral'
}

function memberLastActive(m) {
  const raw = m.lastActiveAt || m.lastActivityAt || m.lastSeenAt || m.lastLoginAt || m.updatedAt
  return raw ? formatDate(raw) : '暂无记录'
}

function hasRecentActivity(m) {
  const raw = m.lastActiveAt || m.lastActivityAt || m.lastSeenAt || m.lastLoginAt
  if (!raw) return false
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return false
  return Date.now() - d.getTime() <= 30 * 24 * 60 * 60 * 1000
}

function fillSettingsFromGroup(g) {
  settingsForm.name = g.name || ''
  settingsForm.type = g.type || g.category || 'interest'
  settingsForm.region = g.region || g.location || ''
  const jm = g.joinModeKey || g.joinMode || 'audit'
  settingsForm.joinMode = jm === 'open' ? 'open' : jm === 'invite' ? 'invite' : 'audit'
  settingsForm.coverUrl = g.coverUrl || ''
  settingsForm.description = g.description || ''
  settingsForm.tags = g.tags || ''
  inviteCode.value = g.inviteCode || ''
  heroCoverUrl.value = groupCoverUrlFromApi(g, '')
}

function onHeroCoverError() {
  heroCoverUrl.value = ''
}

async function ensureAuth() {
  if (!userStore.isLoggedIn) {
    userStore.setPostLoginRedirect(route.fullPath)
    await router.push('/login')
    return false
  }
  await userStore.loadAdminContext().catch(() => null)
  return true
}

async function bootstrap() {
  const legacyEntry = resolveSpaceManageEntry(spaceId.value, {
    tab: typeof route.query.tab === 'string' ? route.query.tab : 'info'
  })
  if (legacyEntry?.path?.startsWith('/admin/my-groups')) {
    redirecting.value = true
    loading.value = false
    await router.replace(legacyEntry)
    return
  }

  loading.value = true
  loadError.value = ''
  forbidden.value = false
  group.value = null

  try {
    const ok = await ensureAuth()
    if (!ok) {
      loadError.value = '请先登录后再进入运营台'
      return
    }

    const detail = await fetchGroupDetail(spaceId.value)
    const data = detail?.data ?? detail
    if (!data || data.id == null) {
      loadError.value = '团体不存在或已下线'
      return
    }
    group.value = data

    const canManage =
      Boolean(data?.managed) ||
      Boolean(data?.isOwner) ||
      ['owner', 'admin'].includes(String(data?.myRole || '').toLowerCase()) ||
      userStore.hasPermission('group.manage.all')
    if (!canManage) {
      forbidden.value = true
      return
    }

    fillSettingsFromGroup(data)
    pendingCount.value = Number(data?.pendingMemberCount ?? 0)
    inviteCode.value = data?.inviteCode || inviteCode.value

    await loadSpaceStats()
    await loadPendingActivityCount()
    if (activeTab.value === 'members') await loadMembers(memberStatus.value)
    if (activeTab.value === 'review') await loadMembers('pending')
    if (activeTab.value === 'notices') await loadNotices()
    if (activeTab.value === 'activities') await loadActivities()
    if (activeTab.value === 'activity-review') await loadPendingActivityCount()
  } catch (err) {
    loadError.value = err?.message || '无法加载社区空间'
    group.value = null
  } finally {
    loading.value = false
  }
}

async function loadSpaceStats() {
  loadingSpaceStats.value = true
  spaceStatsError.value = ''
  try {
    const res = await fetchSpaceStats(spaceId.value)
    spaceStats.value = res?.data ?? res
    const pending = Number(spaceStats.value?.memberGrowth?.pendingMembers)
    if (!Number.isNaN(pending)) {
      pendingCount.value = pending
    }
    const pendingActivities = Number(spaceStats.value?.content?.pendingActivityProposals)
    if (!Number.isNaN(pendingActivities)) {
      pendingActivityCount.value = pendingActivities
    }
  } catch (err) {
    spaceStatsError.value = err?.message || '数据加载失败'
  } finally {
    loadingSpaceStats.value = false
  }
}

async function loadMembers(status = 'approved') {
  memberStatus.value = status
  loadingMembers.value = true
  try {
    const res = await fetchGroupMembers(spaceId.value, { status })
    members.value = Array.isArray(res) ? res : res?.data ?? []
    if (status === 'pending') {
      pendingCount.value = members.value.length
    }
  } catch (err) {
    showFlash(err?.message || '成员加载失败', 'error')
    members.value = []
  } finally {
    loadingMembers.value = false
  }
}

async function loadNotices() {
  loadingNotices.value = true
  try {
    const res = await fetchGroupNotices(spaceId.value)
    notices.value = Array.isArray(res) ? res : res?.data ?? []
  } catch (err) {
    showFlash(err?.message || '公告加载失败', 'error')
    notices.value = []
  } finally {
    loadingNotices.value = false
  }
}

async function loadArticles() {
  loadingArticles.value = true
  try {
    const res = await fetchGroupArticles(spaceId.value)
    articles.value = Array.isArray(res) ? res : res?.data ?? []
  } catch (err) {
    showFlash(err?.message || '文章加载失败', 'error')
    articles.value = []
  } finally {
    loadingArticles.value = false
  }
}

async function submitArticle() {
  if (!articleForm.title.trim() || !articleForm.content.trim()) {
    showFlash('请填写文章标题与正文', 'error')
    return
  }
  saving.value = true
  try {
    await createGroupArticle(spaceId.value, {
      title: articleForm.title,
      content: articleForm.content,
      coverUrl: articleForm.coverUrl || undefined
    })
    articleForm.title = ''
    articleForm.content = ''
    articleForm.coverUrl = ''
    await loadArticles()
    showFlash('文章已发布')
  } catch (err) {
    showFlash(err?.message || '发布失败', 'error')
  } finally {
    saving.value = false
  }
}

async function loadActivities() {
  loadingActivities.value = true
  try {
    const res = await fetchGroupActivities(spaceId.value, { page: 1, pageSize: 50 })
    activities.value = res?.items ?? (Array.isArray(res) ? res : [])
  } catch (err) {
    showFlash(err?.message || '活动加载失败', 'error')
    activities.value = []
  } finally {
    loadingActivities.value = false
  }
}

function buildSettingsPayload() {
  return {
    name: settingsForm.name,
    type: settingsForm.type,
    region: settingsForm.region,
    joinMode: settingsForm.joinMode,
    coverUrl: settingsForm.coverUrl || undefined,
    description: settingsForm.description,
    tags: settingsForm.tags || undefined
  }
}

async function saveSettings() {
  saving.value = true
  try {
    await updateGroup(spaceId.value, buildSettingsPayload())
    const detail = await fetchGroupDetail(spaceId.value)
    group.value = detail?.data ?? detail
    fillSettingsFromGroup(group.value)
    showFlash('设置已保存')
    return true
  } catch (err) {
    showFlash(err?.message || '保存失败', 'error')
    return false
  } finally {
    saving.value = false
  }
}

async function saveSettingsAndNext() {
  const ok = await saveSettings()
  if (ok) onboardingStep.value = 2
}

async function submitNotice() {
  if (!noticeForm.title.trim() || !noticeForm.content.trim()) {
    showFlash('请填写公告标题与内容', 'error')
    return false
  }
  saving.value = true
  try {
    await createGroupNotice(spaceId.value, {
      title: noticeForm.title,
      content: noticeForm.content
    })
    noticeForm.title = ''
    noticeForm.content = ''
    await loadNotices()
    showFlash('公告已发布')
    return true
  } catch (err) {
    showFlash(err?.message || '发布失败', 'error')
    return false
  } finally {
    saving.value = false
  }
}

async function publishFirstNotice() {
  const ok = await submitNotice()
  if (ok) onboardingStep.value = 3
}

function toIsoLocal(dtLocal) {
  if (!dtLocal) return ''
  const d = new Date(dtLocal)
  if (Number.isNaN(d.getTime())) return ''
  return d.toISOString().slice(0, 19)
}

async function submitActivity() {
  const startTime = toIsoLocal(activityForm.startTime)
  const endTime = toIsoLocal(activityForm.endTime)
  if (!startTime || !endTime) {
    showFlash('请填写有效的活动时间', 'error')
    return
  }
  saving.value = true
  try {
    await createGroupActivity(spaceId.value, {
      title: activityForm.title,
      description: activityForm.description,
      startTime,
      endTime,
      location: activityForm.location || undefined,
      maxParticipants: 0
    })
    activityForm.title = ''
    activityForm.description = ''
    activityForm.startTime = ''
    activityForm.endTime = ''
    activityForm.location = ''
    await loadActivities()
    showFlash('活动已创建')
  } catch (err) {
    showFlash(err?.message || '创建失败', 'error')
  } finally {
    saving.value = false
  }
}

async function approve(member) {
  saving.value = true
  try {
    await approveMember(spaceId.value, member.id)
    await loadMembers(memberStatus.value)
    const detail = await fetchGroupDetail(spaceId.value)
    group.value = detail?.data ?? detail
    pendingCount.value = Number(group.value?.pendingMemberCount ?? pendingCount.value)
    reviewedCount.value += 1
    showFlash('已通过入团申请')
  } catch (err) {
    showFlash(err?.message || '操作失败', 'error')
  } finally {
    saving.value = false
  }
}

async function reject(member) {
  saving.value = true
  try {
    await rejectMember(spaceId.value, member.id)
    await loadMembers(memberStatus.value)
    pendingCount.value = Math.max(0, pendingCount.value - 1)
    reviewedCount.value += 1
    showFlash('已拒绝入团申请')
  } catch (err) {
    showFlash(err?.message || '操作失败', 'error')
  } finally {
    saving.value = false
  }
}

async function removeMember(member) {
  if (!window.confirm(`确定移除成员「${displayMemberName(member)}」？`)) return
  saving.value = true
  try {
    await removeGroupMember(spaceId.value, member.id)
    await loadMembers(memberStatus.value)
    showFlash('成员已移除')
  } catch (err) {
    showFlash(err?.message || '移除失败', 'error')
  } finally {
    saving.value = false
  }
}

async function copyInviteLink() {
  try {
    await navigator.clipboard.writeText(inviteShareUrl.value)
    showFlash('链接已复制')
  } catch {
    showFlash('复制失败，请手动选择链接', 'error')
  }
}

function finishOnboarding() {
  const q = { ...route.query }
  delete q.onboarding
  router.replace({ path: route.path, query: q })
}

watch(
  () => route.params.id,
  () => bootstrap()
)

watch(
  () => route.query.tab,
  (tab) => {
    const next = normalizeTab(tab)
    if (next !== activeTab.value) {
      switchTab(next)
    }
  }
)

onMounted(() => {
  bootstrap()
})
</script>

<style scoped>
.space-manage-page {
  width: min(100% - 48px, 1360px);
  margin: var(--lc-space-4) auto calc(var(--lc-space-8) + 72px + env(safe-area-inset-bottom, 0px));
  padding: var(--lc-space-5);
  color: var(--lc-text);
  border-radius: 12px;
  background: color-mix(in srgb, var(--lc-bg) 74%, var(--lc-surface));
}

.state-card {
  display: grid;
  justify-items: center;
  gap: var(--lc-space-3);
  min-height: 220px;
  padding: var(--lc-space-8);
  border: 1px solid var(--lc-border);
  border-radius: 10px;
  background: var(--lc-surface);
  text-align: center;
  color: var(--lc-muted);
  box-shadow: 0 8px 22px rgb(15 23 42 / 5%);
}

.state-card.err {
  border-color: color-mix(in srgb, var(--lc-red) 30%, var(--lc-border));
  background: color-mix(in srgb, var(--lc-red-light) 72%, var(--lc-surface));
}

.state-card.err h2 {
  margin: 0 0 var(--lc-space-2);
  color: var(--lc-text);
}

.operation-hero {
  margin-bottom: var(--lc-space-5);
  box-shadow: 0 16px 36px rgb(15 23 42 / 7%);
}

.hero-main,
.hero-side {
  min-width: 0;
}

.hero-side {
  display: grid;
  align-content: space-between;
  gap: var(--lc-space-3);
}

.hero-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-3);
  margin-bottom: var(--lc-space-4);
}

.back-link {
  display: inline-flex;
  align-items: center;
  color: var(--lc-blue);
  font-weight: 700;
  text-decoration: none;
}

.title-row {
  display: flex;
  gap: var(--lc-space-3);
  align-items: center;
}

.cover-thumb {
  width: 76px;
  height: 76px;
  flex: 0 0 76px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid var(--lc-border);
  background: var(--lc-blue-light);
  box-shadow: 0 8px 18px rgb(15 23 42 / 8%);
}

.cover-fallback {
  display: grid;
  place-items: center;
  color: var(--lc-blue);
  font-size: 24px;
  font-weight: 900;
}

.title-block {
  min-width: 0;
}

.title-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
  margin-bottom: var(--lc-space-2);
}

.operation-hero h1 {
  margin: 0;
  color: var(--lc-text);
  font-size: 34px;
  line-height: 1.18;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--lc-space-2);
}

.hero-stats article {
  min-width: 0;
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: 10px;
  background: var(--lc-surface);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 80%);
}

.hero-stat-card.primary {
  border-color: var(--lc-blue-border);
  background: color-mix(in srgb, var(--lc-blue-light) 62%, var(--lc-surface));
}

.hero-stat-card.warning {
  border-color: color-mix(in srgb, var(--lc-amber) 34%, var(--lc-border));
  background: color-mix(in srgb, var(--lc-amber-light) 62%, var(--lc-surface));
}

.hero-stats span {
  display: block;
  color: var(--lc-muted);
  font-size: 12px;
  font-weight: 800;
}

.hero-stats strong {
  display: block;
  margin-top: 6px;
  color: var(--lc-text);
  font-size: 24px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-stats small {
  display: block;
  margin-top: 8px;
  color: var(--lc-muted);
  font-size: 11px;
  font-weight: 700;
}

.hero-stats strong.warn {
  color: var(--lc-amber);
}

.subtitle {
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  font-weight: 600;
  line-height: 1.6;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--lc-space-2);
}

.hero-actions-mobile {
  display: none;
}

.hero-task-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-2);
  padding: 10px 12px;
  border: 1px solid var(--lc-border);
  border-radius: 10px;
  background: var(--lc-bg);
  color: var(--lc-muted);
  font-size: 13px;
  font-weight: 800;
}

.hero-task-strip.urgent {
  border-color: color-mix(in srgb, var(--lc-amber) 36%, var(--lc-border));
  background: color-mix(in srgb, var(--lc-amber-light) 68%, var(--lc-surface));
  color: var(--lc-amber);
}

.link-button {
  border: 0;
  background: transparent;
  color: var(--lc-blue);
  cursor: pointer;
  font-weight: 900;
  white-space: nowrap;
}

.manage-tabs {
  display: flex;
  overflow-x: auto;
  gap: var(--lc-space-2);
  margin-bottom: var(--lc-space-5);
  padding: 10px;
  border: 1px solid var(--lc-border);
  border-radius: 12px;
  background: var(--lc-surface);
  box-shadow: 0 10px 26px rgb(15 23 42 / 5%);
  scrollbar-width: none;
}

.manage-tabs::-webkit-scrollbar {
  display: none;
}

.manage-tabs button {
  display: grid;
  gap: 4px;
  min-width: 132px;
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
  color: var(--lc-muted);
  border-radius: 10px;
  min-height: 58px;
  padding: 10px 12px;
  font-weight: 800;
  cursor: pointer;
  text-align: left;
  transition: border-color .16s ease, background .16s ease, color .16s ease;
}

.manage-tabs button.active {
  border-color: var(--lc-blue);
  color: var(--lc-blue);
  background: color-mix(in srgb, var(--lc-blue) 9%, var(--lc-surface));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--lc-blue) 16%, transparent);
}

.manage-tabs button.urgent:not(.active) {
  border-color: color-mix(in srgb, var(--lc-amber) 42%, var(--lc-border));
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: inherit;
  font-size: 14px;
}

.manage-tabs small {
  color: var(--lc-muted-light);
  font-size: 12px;
  font-weight: 700;
}

.badge {
  display: inline-flex;
  min-width: 20px;
  height: 20px;
  margin-left: 6px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--lc-amber);
  color: #fff;
  font-size: 11px;
  align-items: center;
  justify-content: center;
  font-variant-numeric: tabular-nums;
}

.tab-panel {
  padding: var(--lc-space-5);
  border: 1px solid var(--lc-border);
  border-radius: 12px;
  background: var(--lc-surface);
  box-shadow: 0 10px 26px rgb(15 23 42 / 5%);
}

.overview-tab-panel {
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.tab-panel h2 {
  margin: 0 0 var(--lc-space-4);
  font-size: 20px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: var(--lc-space-3);
  margin-bottom: var(--lc-space-4);
}

.stat-card {
  padding: var(--lc-space-4);
  border-radius: 8px;
  border: 1px solid var(--lc-border);
  background: var(--lc-bg);
  display: grid;
  gap: 4px;
}

.stat-card strong {
  font-size: 24px;
  color: var(--lc-text);
}

.stat-card span {
  color: var(--lc-muted);
  font-weight: 700;
  font-size: 13px;
}

.stat-card.warn strong {
  color: var(--lc-red);
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--lc-space-3);
  flex-wrap: wrap;
  margin-bottom: var(--lc-space-4);
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
}

.filter-row {
  display: flex;
  gap: var(--lc-space-2);
  flex-wrap: wrap;
}

.filter-row button {
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
  border-radius: 8px;
  min-height: 32px;
  padding: 0 10px;
  font-weight: 700;
  cursor: pointer;
}

.filter-row button.active {
  border-color: var(--lc-blue);
  color: var(--lc-blue);
}

.member-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--lc-space-3);
  margin-bottom: var(--lc-space-4);
}

.member-table,
.review-list {
  display: grid;
  gap: var(--lc-space-2);
}

.member-table-head {
  display: grid;
  grid-template-columns: 44px minmax(140px, 1.5fr) 110px 130px 130px 100px 130px;
  gap: var(--lc-space-3);
  padding: 0 var(--lc-space-3) var(--lc-space-2);
  color: var(--lc-muted);
  font-size: 12px;
  font-weight: 900;
}

.member-row {
  display: grid;
  grid-template-columns: 44px minmax(140px, 1.5fr) 110px 130px 130px 100px 130px;
  gap: var(--lc-space-3);
  align-items: center;
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: 10px;
  background: var(--lc-surface);
}

.member-row img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.member-main {
  min-width: 0;
}

.member-main strong {
  display: block;
  overflow: hidden;
  color: var(--lc-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-main p {
  margin: 2px 0 0;
  color: var(--lc-muted);
  font-size: 13px;
}

.apply-reason {
  color: var(--lc-text);
}

.member-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--lc-bg);
  font-size: 12px;
  font-weight: 700;
  color: var(--lc-muted);
}

.tag.status {
  background: var(--lc-blue-light);
  color: var(--lc-blue);
}

.tag.owner {
  color: var(--lc-blue);
}

.tag.admin {
  color: var(--lc-indigo);
}

.member-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.member-meta {
  color: var(--lc-muted);
  font-size: 13px;
  font-weight: 700;
}

.review-panel {
  border-color: color-mix(in srgb, var(--lc-amber) 18%, var(--lc-border));
}

.review-summary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.review-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: var(--lc-space-3);
  align-items: center;
}

.review-card img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.review-main {
  min-width: 0;
}

.review-title {
  display: flex;
  align-items: center;
  gap: var(--lc-space-2);
  flex-wrap: wrap;
}

.review-title strong {
  color: var(--lc-text);
}

.review-actions {
  display: flex;
  gap: var(--lc-space-2);
}

.inline-form,
.settings-form {
  display: grid;
  gap: var(--lc-space-3);
  margin-bottom: var(--lc-space-5);
  padding-bottom: var(--lc-space-4);
  border-bottom: 1px solid var(--lc-border);
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--lc-space-3);
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  font-weight: 800;
  color: var(--lc-text);
}

.field input,
.field textarea,
.field select {
  width: 100%;
  border: 1px solid var(--lc-border);
  border-radius: 8px;
  padding: 10px 12px;
  font: inherit;
  background: var(--lc-surface);
  color: var(--lc-text);
}

.field input:focus,
.field textarea:focus,
.field select:focus {
  outline: none;
  border-color: var(--lc-blue);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--lc-blue) 12%, transparent);
}

.content-list {
  display: grid;
  gap: var(--lc-space-3);
}

.content-row {
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: 8px;
  background: var(--lc-surface);
}

.content-row header {
  display: flex;
  justify-content: space-between;
  gap: var(--lc-space-2);
  margin-bottom: var(--lc-space-2);
}

.content-row time {
  color: var(--lc-muted);
  font-size: 13px;
}

.task-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: var(--lc-space-3);
}

.task-list li {
  display: flex;
  justify-content: space-between;
  gap: var(--lc-space-3);
  align-items: center;
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: 8px;
  background: var(--lc-surface);
}

.hint,
.meta-line,
.share-hint {
  color: var(--lc-muted);
  font-weight: 600;
  line-height: 1.5;
}

.flash {
  margin: 0 0 var(--lc-space-3);
  padding: 10px 12px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--lc-green) 12%, var(--lc-surface));
  color: var(--lc-green);
  font-weight: 700;
}

.flash.err {
  background: color-mix(in srgb, var(--lc-red) 10%, var(--lc-surface));
  color: var(--lc-red);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  border-radius: 8px;
  padding: 0 14px;
  font-weight: 800;
  border: 1px solid transparent;
  cursor: pointer;
  text-decoration: none;
  transition: border-color .16s ease, background .16s ease, color .16s ease;
}

.btn.primary {
  background: var(--lc-blue);
  color: #fff;
}

.btn.secondary {
  background: var(--lc-surface);
  border-color: var(--lc-border);
  color: var(--lc-text);
}

.btn.ghost {
  background: var(--lc-surface);
  border-color: var(--lc-border);
  color: var(--lc-muted);
}

.btn.danger {
  background: var(--lc-surface);
  border-color: color-mix(in srgb, var(--lc-red) 40%, var(--lc-surface));
  color: var(--lc-red);
}

.btn.secondary:hover:not(:disabled),
.btn.ghost:hover:not(:disabled) {
  border-color: var(--lc-blue-border);
  color: var(--lc-blue);
}

.btn.sm {
  padding: 6px 10px;
  font-size: 13px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.onboarding-panel {
  margin-bottom: var(--lc-space-4);
  padding: var(--lc-space-5);
  border: 1px solid color-mix(in srgb, var(--lc-blue) 25%, var(--lc-surface));
  background: color-mix(in srgb, var(--lc-blue) 4%, var(--lc-surface));
  border-radius: 10px;
}

.onboarding-head {
  display: flex;
  justify-content: space-between;
  gap: var(--lc-space-2);
  align-items: center;
}

.step-label {
  color: var(--lc-blue);
  font-weight: 800;
}

.onboarding-body {
  margin-top: var(--lc-space-3);
  display: grid;
  gap: var(--lc-space-3);
}

.onboarding-actions {
  display: flex;
  gap: var(--lc-space-2);
  flex-wrap: wrap;
}

.share-box {
  display: flex;
  gap: var(--lc-space-2);
  align-items: center;
  flex-wrap: wrap;
}

.share-box code {
  flex: 1;
  min-width: 200px;
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--lc-bg);
  word-break: break-all;
  font-size: 12px;
}

.invite-block {
  margin-top: var(--lc-space-5);
  padding-top: var(--lc-space-4);
  border-top: 1px solid var(--lc-border);
}

@media (max-width: 980px) {
  .space-manage-page {
    width: min(100% - 32px, 960px);
    padding: var(--lc-space-3);
  }

  .hero-side {
    align-content: start;
  }

  .head-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .space-manage-page {
    width: min(100% - 16px, 480px);
    margin-top: var(--lc-space-3);
    margin-bottom: calc(80px + env(safe-area-inset-bottom, 0px));
    padding: 0 0 12px;
  }

  .operation-hero,
  .tab-panel {
    padding: var(--lc-space-4);
  }

  .title-row {
    align-items: flex-start;
  }

  .cover-thumb {
    width: 52px;
    height: 52px;
    flex-basis: 52px;
  }

  .operation-hero h1 {
    font-size: 22px;
  }

  .hero-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px;
  }

  .hero-stats article {
    padding: 8px 6px;
  }

  .hero-stats strong {
    margin-top: 4px;
    font-size: 18px;
  }

  .hero-stats small {
    display: none;
  }

  .member-summary-grid .metric-card,
  .review-summary .metric-card {
    min-height: 0;
  }

  .hero-actions-mobile {
    display: flex;
    flex-wrap: wrap;
    gap: var(--lc-space-2);
    margin-top: var(--lc-space-4);
  }

  .head-actions {
    display: none;
  }

  .hero-actions-mobile .btn {
    width: 100%;
  }

  .manage-tabs button {
    min-width: 118px;
  }

  .member-summary-grid,
  .review-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .member-table-head {
    display: none;
  }

  .member-row {
    grid-template-columns: 44px 1fr;
  }

  .member-tags,
  .member-actions,
  .member-meta,
  .member-row > .status-badge {
    grid-column: 2;
  }

  .review-card {
    grid-template-columns: 48px 1fr;
  }

  .review-actions {
    grid-column: 2;
  }

  .field-row {
    grid-template-columns: 1fr;
  }
}
</style>
