<template>
  <section class="activity-hub operation-shell">
    <section class="operation-hero activity-hero">
      <div class="hero-copy">
        <p class="section-kicker">Activity Hub</p>
        <h1>我的活动</h1>
        <p class="hero-desc">报名、签到、互评与投稿，一站掌握你的活动参与进度。</p>
        <div class="hero-actions">
          <router-link :to="eventsPath()" class="platform-btn platform-btn-ghost">浏览活动广场</router-link>
        </div>
      </div>
      <div class="hero-metrics">
        <article v-for="metric in kpiMetrics" :key="metric.label" class="metric-card" :class="metric.tone">
          <span class="metric-label">{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <p>{{ metric.hint }}</p>
        </article>
      </div>
    </section>

    <nav class="hub-tabs" aria-label="活动视图">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
        <span v-if="tab.badge" class="tab-badge">{{ tab.badge }}</span>
      </button>
    </nav>

    <div v-if="loading" class="inline-state">加载中…</div>
    <div v-else-if="error" class="inline-state error-state">
      <p>{{ error }}</p>
      <button type="button" class="platform-btn platform-btn-primary" @click="loadAll">重试</button>
    </div>

    <template v-else>
      <!-- Tab: 我报名的 -->
      <section v-if="activeTab === 'signups'" class="tab-panel">
        <div v-if="allSignups.length" class="card-list">
          <article v-for="item in allSignups" :key="item.key" class="activity-item-card platform-card">
            <header class="card-head">
              <div>
                <span class="type-pill">{{ item.typeLabel }}</span>
                <h3>{{ item.title }}</h3>
                <p class="meta-line">{{ formatTime(item.startTime || item.eventTime) }} · {{ item.location || '待定' }}</p>
              </div>
              <span class="status-badge" :class="item.displayTone">{{ item.displayLabel }}</span>
            </header>
            <div class="status-row">
              <span class="status-badge" :class="item.participationTone">{{ item.participationLabel }}</span>
              <span v-if="item.checkedIn" class="muted">签到于 {{ formatTime(item.checkedInAt) }}</span>
              <span v-else-if="item.pendingReviewCount > 0" class="warn-text">待互评 {{ item.pendingReviewCount }} 人</span>
            </div>
            <div class="card-actions">
              <router-link :to="item.detailTo" class="platform-btn platform-btn-ghost sm">查看详情</router-link>
              <button
                v-if="item.canCheckin"
                type="button"
                class="platform-btn platform-btn-primary sm"
                @click="openCheckin(item)"
              >去签到</button>
              <button
                v-if="item.canReviewAction"
                type="button"
                class="platform-btn platform-btn-primary sm"
                @click="openReview(item)"
              >去互评</button>
            </div>
          </article>
        </div>
        <p v-else class="inline-state">你还没有报名任何活动，去活动广场看看吧。</p>
      </section>

      <!-- Tab: 我的投稿 -->
      <section v-else-if="activeTab === 'proposals'" class="tab-panel">
        <div v-if="proposals.length" class="card-list">
          <article v-for="item in proposals" :key="item.id" class="activity-item-card platform-card">
            <header class="card-head">
              <div>
                <h3>{{ item.title }}</h3>
                <p class="meta-line">{{ item.groupName }} · 投稿于 {{ formatTime(item.createdAt) }}</p>
              </div>
              <span class="status-badge" :class="proposalTone(item.status)">{{ item.statusLabel }}</span>
            </header>
            <p class="desc">{{ item.description || '暂无简介' }}</p>
            <p class="meta-line">{{ formatTime(item.startTime) }} — {{ formatTime(item.endTime) }}</p>
            <p v-if="item.reviewComment && item.status === 'rejected'" class="review-note">审核意见：{{ item.reviewComment }}</p>
          </article>
        </div>
        <div v-else class="inline-state empty-proposals">
          <p>暂无投稿记录。</p>
          <p class="muted">成员可在所属 Space 团体详情中发起活动投稿；审核进度也会出现在这里。</p>
        </div>
      </section>

      <!-- Tab: 待办事项 -->
      <section v-else class="tab-panel">
        <div v-if="todoItems.length" class="card-list">
          <article v-for="item in todoItems" :key="item.key" class="activity-item-card platform-card todo-card">
            <div class="todo-icon" :class="item.todoType">{{ item.todoIcon }}</div>
            <div class="todo-body">
              <strong>{{ item.todoTitle }}</strong>
              <p>{{ item.todoDesc }}</p>
            </div>
            <router-link v-if="item.todoTo" :to="item.todoTo" class="platform-btn platform-btn-primary sm">{{ item.todoAction }}</router-link>
            <button v-else type="button" class="platform-btn platform-btn-primary sm" @click="item.todoHandler?.()">{{ item.todoAction }}</button>
          </article>
        </div>
        <p v-else class="inline-state">暂无待办，一切顺利。</p>
      </section>
    </template>

    <!-- 签到弹层 -->
    <div v-if="checkinOpen" class="dialog-backdrop" @click.self="checkinOpen = false">
      <div class="dialog-card" role="dialog" aria-labelledby="checkin-dialog-title">
        <h3 id="checkin-dialog-title">活动签到</h3>
        <p class="dialog-hint">请输入现场 6 位签到码</p>
        <input v-model.trim="checkinCode" type="text" maxlength="6" class="checkin-input" placeholder="例如 A1B2C3">
        <p v-if="checkinError" class="form-error">{{ checkinError }}</p>
        <div class="dialog-actions">
          <button type="button" class="platform-btn platform-btn-ghost" @click="checkinOpen = false">取消</button>
          <button type="button" class="platform-btn platform-btn-primary" :disabled="checkinSaving" @click="submitCheckin">
            {{ checkinSaving ? '签到中…' : '确认签到' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 互评弹层 -->
    <div v-if="reviewOpen" class="dialog-backdrop" @click.self="reviewOpen = false">
      <div class="dialog-card review-dialog" role="dialog" aria-labelledby="review-dialog-title">
        <h3 id="review-dialog-title">活动互评</h3>
        <p class="dialog-hint">为同行伙伴评分（1-5 星），帮助建立信任。</p>
        <div v-if="reviewLoading" class="inline-state">加载候选人…</div>
        <div v-for="user in reviewCandidates" :key="user.userId" class="review-row">
          <div class="review-user">
            <strong>{{ user.nickname || '伙伴' }}</strong>
            <div class="star-row">
              <button
                v-for="n in 5"
                :key="n"
                type="button"
                class="star-btn"
                :class="{ on: (reviewDraft[user.userId]?.rating || 0) >= n }"
                @click="setRating(user.userId, n)"
              >★</button>
            </div>
          </div>
          <textarea
            v-if="!user.reviewed"
            v-model="reviewDraft[user.userId].comment"
            rows="2"
            maxlength="100"
            placeholder="可选：一句话评价"
          />
          <span v-else class="status-badge success">已评</span>
          <button
            v-if="!user.reviewed"
            type="button"
            class="platform-btn platform-btn-primary sm"
            @click="submitReview(user.userId)"
          >提交</button>
        </div>
        <p v-if="!reviewLoading && !reviewCandidates.length" class="inline-state">暂无可评价对象</p>
        <button type="button" class="platform-btn platform-btn-ghost close-review" @click="reviewOpen = false">关闭</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getMyEventSignups, checkinEvent, fetchEventReviewCandidates, submitEventReview } from '@/api/platformContent.js'
import { checkinGroupActivity, fetchGroupActivityReviewCandidates, submitGroupActivityReview } from '@/api/groups.js'
import {
  fetchMyGroupActivitySignups,
  fetchMyActivityProposalsAll,
  fetchMyHostedActivityCount
} from '@/api/myActivities.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import {
  getActivityDisplayStatus,
  getParticipationStatusLabel,
  getParticipationStatusTone
} from '@/utils/activityStatus.js'

const { eventsPath } = usePlatformPath()

const loading = ref(true)
const error = ref('')
const activeTab = ref('signups')
const platformSignups = ref([])
const groupSignups = ref([])
const proposals = ref([])
const hostedCount = ref(0)

const checkinOpen = ref(false)
const checkinCode = ref('')
const checkinError = ref('')
const checkinSaving = ref(false)
const checkinTarget = ref(null)

const reviewOpen = ref(false)
const reviewLoading = ref(false)
const reviewCandidates = ref([])
const reviewDraft = reactive({})
const reviewTarget = ref(null)

const tabs = computed(() => [
  { key: 'signups', label: '我报名的', badge: allSignups.value.length || '' },
  { key: 'proposals', label: '我的投稿', badge: proposals.value.filter(p => p.status === 'pending').length || '' },
  { key: 'todos', label: '待办事项', badge: todoItems.value.length || '' }
])

const allSignups = computed(() => {
  const platform = (platformSignups.value || []).map((item) => normalizeSignup(item, 'PLATFORM'))
  const group = (groupSignups.value || []).map((item) => normalizeSignup(item, 'GROUP'))
  return [...platform, ...group].sort((a, b) => {
    const ta = new Date(a.startTime || a.eventTime || 0).getTime()
    const tb = new Date(b.startTime || b.eventTime || 0).getTime()
    return tb - ta
  })
})

const kpiMetrics = computed(() => [
  { label: '已报名', value: allSignups.value.length, hint: '平台 + 团体活动', tone: 'info' },
  { label: '待签到', value: pendingCheckinCount.value, hint: '可现场签到', tone: 'warning' },
  { label: '待互评', value: pendingReviewCount.value, hint: '活动结束后', tone: 'rose' },
  { label: '我的投稿', value: proposals.value.length, hint: `${proposals.value.filter(p => p.status === 'pending').length} 条审核中`, tone: 'violet' },
  { label: '我发起的', value: hostedCount.value, hint: '已发布团体活动', tone: 'green' }
])

const pendingCheckinCount = computed(() =>
  allSignups.value.filter((i) => i.canCheckin).length
)

const pendingReviewCount = computed(() =>
  allSignups.value.reduce((sum, i) => sum + Number(i.pendingReviewCount || 0), 0)
)

const todoItems = computed(() => {
  const items = []
  allSignups.value.filter((i) => i.canCheckin).forEach((i) => {
    items.push({
      key: `checkin-${i.key}`,
      todoType: 'checkin',
      todoIcon: '✓',
      todoTitle: `待签到：${i.title}`,
      todoDesc: formatTime(i.startTime || i.eventTime),
      todoAction: '去签到',
      todoHandler: () => openCheckin(i)
    })
  })
  allSignups.value.filter((i) => i.canReviewAction).forEach((i) => {
    items.push({
      key: `review-${i.key}`,
      todoType: 'review',
      todoIcon: '★',
      todoTitle: `待互评：${i.title}`,
      todoDesc: `还有 ${i.pendingReviewCount} 位伙伴待评价`,
      todoAction: '去互评',
      todoHandler: () => openReview(i)
    })
  })
  proposals.value.filter((p) => p.status === 'pending').forEach((p) => {
    items.push({
      key: `proposal-${p.id}`,
      todoType: 'proposal',
      todoIcon: '◷',
      todoTitle: `投稿审核中：${p.title}`,
      todoDesc: `${p.groupName} · 等待运营者审核`,
      todoAction: '查看投稿',
      todoTo: `/platform/groups/${p.groupId}/my-activity-proposals`
    })
  })
  return items
})

function normalizeSignup(item, sourceType) {
  const isGroup = sourceType === 'GROUP'
  const display = getActivityDisplayStatus({
    status: item.activityStatus || 'published',
    eventTime: item.eventTime || item.startTime,
    startTime: item.startTime,
    endTime: item.endTime
  })
  const participationLabel = getParticipationStatusLabel(item.status)
  const participationTone = getParticipationStatusTone(item.status)
  const eventEnded = Boolean(item.eventEnded)
  const checkedIn = Boolean(item.checkedIn)
  const checkinEnabled = Boolean(item.checkinEnabled)
  const canCheckin = checkinEnabled && !checkedIn && !eventEnded
  const canReviewAction = Boolean(item.canReview) && Number(item.pendingReviewCount || 0) > 0

  return {
    key: isGroup ? `g-${item.activityId}` : `e-${item.eventId}`,
    sourceType,
    typeLabel: isGroup ? '团体活动' : '平台活动',
    title: item.title,
    location: item.location,
    startTime: item.startTime,
    eventTime: item.eventTime,
    endTime: item.endTime,
    status: item.status,
    checkedIn,
    checkedInAt: item.checkedInAt,
    pendingReviewCount: item.pendingReviewCount,
    displayLabel: display.label,
    displayTone: display.tone,
    participationLabel,
    participationTone,
    canCheckin,
    canReviewAction,
    detailTo: isGroup ? item.detailPath : `/events/${item.eventId}`,
    eventId: item.eventId,
    activityId: item.activityId,
    groupId: item.groupId
  }
}

function proposalTone(status) {
  if (status === 'pending') return 'warning'
  if (status === 'rejected') return 'danger'
  if (status === 'published') return 'success'
  return 'neutral'
}

function formatTime(value) {
  if (!value) return '-'
  const d = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return String(value)
  return d.toLocaleString('zh-CN', { hour12: false })
}

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    const [platform, group, proposalRows, hosted] = await Promise.all([
      getMyEventSignups().catch(() => []),
      fetchMyGroupActivitySignups().catch(() => []),
      fetchMyActivityProposalsAll().catch(() => []),
      fetchMyHostedActivityCount().catch(() => ({ count: 0 }))
    ])
    platformSignups.value = Array.isArray(platform) ? platform : []
    groupSignups.value = Array.isArray(group) ? group : []
    proposals.value = Array.isArray(proposalRows) ? proposalRows : []
    hostedCount.value = Number(hosted?.count ?? 0)
  } catch (e) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openCheckin(item) {
  checkinTarget.value = item
  checkinCode.value = ''
  checkinError.value = ''
  checkinOpen.value = true
}

async function submitCheckin() {
  const item = checkinTarget.value
  if (!item || !checkinCode.value.trim()) {
    checkinError.value = '请输入签到码'
    return
  }
  checkinSaving.value = true
  checkinError.value = ''
  try {
    if (item.sourceType === 'GROUP') {
      await checkinGroupActivity(item.groupId, item.activityId, checkinCode.value.trim())
    } else {
      await checkinEvent(item.eventId, checkinCode.value.trim())
    }
    checkinOpen.value = false
    await loadAll()
  } catch (e) {
    checkinError.value = e?.message || '签到失败，请确认签到码是否正确'
  } finally {
    checkinSaving.value = false
  }
}

function openReview(item) {
  reviewTarget.value = item
  reviewOpen.value = true
  loadReviewCandidates(item)
}

async function loadReviewCandidates(item) {
  reviewLoading.value = true
  try {
    let rows = []
    if (item.sourceType === 'GROUP') {
      rows = await fetchGroupActivityReviewCandidates(item.groupId, item.activityId)
    } else {
      rows = await fetchEventReviewCandidates(item.eventId)
    }
    reviewCandidates.value = Array.isArray(rows) ? rows : []
    reviewCandidates.value.forEach((user) => {
      if (!reviewDraft[user.userId]) {
        reviewDraft[user.userId] = { rating: 5, comment: '' }
      }
    })
  } catch {
    reviewCandidates.value = []
  } finally {
    reviewLoading.value = false
  }
}

function setRating(userId, rating) {
  if (!reviewDraft[userId]) reviewDraft[userId] = { rating: 5, comment: '' }
  reviewDraft[userId].rating = rating
}

async function submitReview(targetUserId) {
  const item = reviewTarget.value
  const draft = reviewDraft[targetUserId]
  if (!item || !draft?.rating) return
  try {
    if (item.sourceType === 'GROUP') {
      await submitGroupActivityReview(item.groupId, item.activityId, {
        targetUserId,
        rating: draft.rating,
        comment: draft.comment || ''
      })
    } else {
      await submitEventReview(item.eventId, {
        targetUserId,
        rating: draft.rating,
        comment: draft.comment || ''
      })
    }
    await loadAll()
    await loadReviewCandidates(item)
    if (reviewCandidates.value.every((u) => u.reviewed)) {
      reviewOpen.value = false
    }
  } catch (e) {
    checkinError.value = e?.message || '提交失败'
  }
}

onMounted(loadAll)
</script>

<style scoped>
.activity-hub {
  padding-bottom: var(--lc-space-10);
}

.activity-hero .hero-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: var(--lc-space-3);
}

.hub-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
  margin: var(--lc-space-6) 0 var(--lc-space-4);
}

.hub-tabs button {
  border: 1px solid var(--lc-soft-alt);
  background: var(--lc-surface);
  border-radius: 999px;
  padding: 8px 16px;
  cursor: pointer;
  font-weight: 600;
  color: var(--lc-muted);
}

.hub-tabs button.active {
  border-color: var(--lc-blue);
  color: var(--lc-blue-dark);
  background: var(--lc-blue-soft);
}

.tab-badge {
  margin-left: 6px;
  background: var(--lc-pink);
  color: #fff;
  border-radius: 999px;
  padding: 0 6px;
  font-size: 11px;
}

.card-list {
  display: grid;
  gap: var(--lc-space-4);
}

.activity-item-card {
  padding: var(--lc-space-5);
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: var(--lc-space-4);
  align-items: flex-start;
}

.card-head h3 {
  margin: var(--lc-space-2) 0 0;
  font-size: 1.05rem;
}

.type-pill {
  font-size: 11px;
  font-weight: 700;
  color: var(--lc-blue-dark);
  background: var(--lc-blue-soft);
  padding: 2px 8px;
  border-radius: 999px;
}

.meta-line {
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
  margin: var(--lc-space-1) 0 0;
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-3);
  align-items: center;
  margin: var(--lc-space-3) 0;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
}

.platform-btn.sm {
  padding: 6px 12px;
  font-size: var(--lc-text-sm);
}

.desc {
  color: var(--lc-text);
  margin: var(--lc-space-2) 0;
}

.review-note {
  color: var(--lc-danger, #b91c1c);
  font-size: var(--lc-text-sm);
  margin-top: var(--lc-space-2);
}

.empty-proposals .muted {
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
  margin-top: var(--lc-space-2);
}

.todo-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: var(--lc-space-4);
  align-items: center;
}

.todo-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 700;
}

.todo-icon.checkin { background: #e0f2fe; color: #0369a1; }
.todo-icon.review { background: #fef3c7; color: #b45309; }
.todo-icon.proposal { background: #ede9fe; color: #6d28d9; }

.todo-body p {
  margin: 4px 0 0;
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
}

.warn-text {
  color: #b45309;
  font-size: var(--lc-text-sm);
}

.muted { color: var(--lc-muted); font-size: var(--lc-text-sm); }

.inline-state {
  text-align: center;
  padding: var(--lc-space-8);
  color: var(--lc-muted);
}

.error-state { color: var(--lc-danger, #b91c1c); }

.dialog-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: grid;
  place-items: center;
  z-index: var(--lc-z-modal, 200);
  padding: var(--lc-space-4);
}

.dialog-card {
  width: min(480px, 100%);
  background: var(--lc-surface);
  border-radius: var(--lc-radius);
  padding: var(--lc-space-6);
  box-shadow: var(--lc-shadow-lg);
}

.review-dialog {
  max-height: 85vh;
  overflow: auto;
}

.checkin-input {
  width: 100%;
  margin: var(--lc-space-4) 0;
  padding: 10px 12px;
  border: 1px solid var(--lc-soft-alt);
  border-radius: 8px;
  font-size: 1.1rem;
  letter-spacing: 0.15em;
  text-transform: uppercase;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--lc-space-2);
}

.dialog-hint {
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
  margin-top: var(--lc-space-2);
}

.form-error {
  color: var(--lc-danger, #b91c1c);
  font-size: var(--lc-text-sm);
}

.review-row {
  border-top: 1px solid var(--lc-soft-alt);
  padding: var(--lc-space-3) 0;
  display: grid;
  gap: var(--lc-space-2);
}

.star-row {
  display: flex;
  gap: 2px;
}

.star-btn {
  border: 0;
  background: none;
  color: #d1d5db;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 0;
}

.star-btn.on { color: #f59e0b; }

.close-review {
  margin-top: var(--lc-space-4);
  width: 100%;
}

@media (max-width: 640px) {
  .card-head { flex-direction: column; }
  .todo-card { grid-template-columns: 1fr; }

  .activity-hero .hero-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .activity-hero .hero-metrics .metric-card p {
    display: none;
  }
}
</style>
