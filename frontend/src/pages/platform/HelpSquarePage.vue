<template>
  <section class="help-hub operation-shell">
    <section class="operation-hero help-hero">
      <div class="hero-copy">
        <p class="section-kicker">Help Hub</p>
        <h1>互助协作中心</h1>
        <p class="hero-desc">把求助、响应、推进和解决放在同一个工作台里，让每一条需求都能被看见、被承接、被完成。</p>
        <div class="hero-status">
          <span class="status-badge info">当前筛选：{{ currentFilterLabel }}</span>
          <span class="status-badge neutral">协作模式：任务流转</span>
        </div>
        <div class="hero-actions">
          <router-link v-if="userStore.isLoggedIn" :to="helpPath('create')" class="platform-btn platform-btn-primary">发布求助</router-link>
          <router-link v-else to="/login" class="platform-btn platform-btn-primary">登录后发布</router-link>
          <router-link :to="helpPath('my')" class="platform-btn platform-btn-ghost">我的互助</router-link>
        </div>
      </div>
      <div class="hero-metrics">
        <article v-for="metric in heroMetrics" :key="metric.label" class="metric-card" :class="metric.tone">
          <span class="metric-label">{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <p>{{ metric.hint }}</p>
        </article>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="operation-section-head">
        <div>
          <p class="section-kicker">Quick Actions</p>
          <h2>快速行动</h2>
        </div>
      </div>
      <div class="action-grid">
        <router-link class="action-card" :to="userStore.isLoggedIn ? helpPath('create') : '/login'">
          <div class="action-card-head">
            <strong>发布求助</strong>
            <span class="status-badge info">发起</span>
          </div>
          <p>提交一个需要协作处理的互助任务。</p>
        </router-link>
        <router-link class="action-card" :to="helpPath('my')">
          <div class="action-card-head">
            <strong>查看我的求助</strong>
            <span class="status-badge neutral">个人</span>
          </div>
          <p>查看我发布、我响应和待处理的互助记录。</p>
        </router-link>
        <button type="button" class="action-card as-button" @click="setQuickFilter('mine')">
          <div class="action-card-head">
            <strong>查看我参与的</strong>
            <span class="status-badge success">参与</span>
          </div>
          <p>聚焦与我相关的互助事项。</p>
        </button>
        <button type="button" class="action-card as-button warning" @click="setQuickFilter('needsResponse')">
          <div class="action-card-head">
            <strong>查看待响应</strong>
            <span class="status-badge warning">待响应</span>
          </div>
          <p>优先处理尚未收到回应的求助。</p>
        </button>
      </div>
    </section>

    <section class="section-card filter-panel">
      <div class="section-card-head">
        <div>
          <p class="section-kicker">Filters</p>
          <h3>筛选区</h3>
        </div>
        <button type="button" class="platform-btn platform-btn-ghost" @click="resetFilters">重置</button>
      </div>
      <div class="filter-grid">
        <label class="filter-field">
          <span>搜索</span>
          <input v-model.trim="searchKeyword" type="search" placeholder="搜索标题、描述或发起人">
        </label>
        <label class="filter-field">
          <span>分类</span>
          <select v-model="filterType" @change="load(1)">
            <option v-for="t in typeFilters" :key="t.value || 'ALL'" :value="t.value">{{ t.label }}</option>
          </select>
        </label>
        <label class="filter-field">
          <span>状态</span>
          <select v-model="statusFilter">
            <option value="">全部状态</option>
            <option value="active">待响应 / 进行中</option>
            <option value="resolved">已解决</option>
            <option value="mine">我的参与</option>
          </select>
        </label>
        <label class="filter-field">
          <span>时间</span>
          <select v-model="timeFilter">
            <option value="">全部时间</option>
            <option value="today">今日新增</option>
            <option value="week">本周新增</option>
          </select>
        </label>
        <label class="filter-field">
          <span>排序</span>
          <select v-model="sortBy">
            <option value="latest">最近发布</option>
            <option value="updated">最近更新</option>
            <option value="replies">响应数优先</option>
            <option value="urgent">待响应优先</option>
          </select>
        </label>
      </div>
    </section>

    <div class="hub-layout">
      <main class="hub-main">
        <section class="dashboard-section">
          <div class="operation-section-head">
            <div>
              <p class="section-kicker">Requests</p>
              <h2>互助列表</h2>
            </div>
            <span class="status-badge info">当前 {{ filteredItems.length }} 条</span>
          </div>

          <div v-if="loading" class="loading-state">加载中...</div>
          <div v-else-if="error" class="error-state">{{ error }}</div>
          <template v-else>
            <div v-if="filteredItems.length" class="help-list">
              <article v-for="row in filteredItems" :key="row.id" class="section-card help-card">
                <div class="help-card-head">
                  <div>
                    <div class="badge-row">
                      <span class="status-badge info">{{ typeLabel(row.helpType) }}</span>
                      <span :class="['status-badge', statusTone(row)]">{{ requestStatusLabel(row) }}</span>
                      <span v-if="row.isResolved" class="status-badge success">已解决</span>
                    </div>
                    <h3>{{ row.title }}</h3>
                  </div>
                  <span class="reply-count">{{ row.replyCount ?? 0 }} 响应</span>
                </div>
                <p class="help-summary">{{ row.summary || row.content || '暂无描述' }}</p>
                <div class="help-meta">
                  <span>发起人：{{ row.publisherName || '用户' }}</span>
                  <span>发布时间：{{ formatTime(row.createdAt) }}</span>
                  <span>地区：{{ row.region || '未填写' }}</span>
                </div>
                <div class="help-actions">
                  <router-link class="platform-btn platform-btn-primary" :to="helpPath(String(row.id))">查看</router-link>
                  <router-link
                    v-if="canOffer(row)"
                    class="platform-btn platform-btn-ghost"
                    :to="`${helpPath(String(row.id))}?offer=1`"
                  >响应</router-link>
                  <button
                    type="button"
                    :class="['platform-btn', 'platform-btn-ghost', { saved: savedHelpIds.has(row.id) }]"
                    @click="toggleSave(row)"
                  >{{ savedHelpIds.has(row.id) ? '已收藏' : '收藏' }}</button>
                  <button type="button" class="platform-btn platform-btn-ghost" @click="shareHelp(row)">分享</button>
                </div>
              </article>
            </div>
            <div v-else class="empty-state">
              <div>
                <strong>暂无匹配的互助需求</strong>
                <p>调整筛选条件，或发布一条新的互助需求。</p>
              </div>
            </div>
          </template>

          <div v-if="totalPages > 1" class="pager">
            <button type="button" class="platform-btn platform-btn-ghost" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
            <span>第 {{ page }} / {{ totalPages }} 页</span>
            <button type="button" class="platform-btn platform-btn-ghost" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
          </div>
        </section>
      </main>

      <aside class="hub-side">
        <section class="dashboard-section">
          <div class="operation-section-head">
            <div>
              <p class="section-kicker">My Work</p>
              <h2>我的参与</h2>
            </div>
          </div>
          <div class="metric-grid side-metrics">
            <article v-for="metric in myMetrics" :key="metric.label" class="metric-card">
              <span class="metric-label">{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
              <p>{{ metric.hint }}</p>
            </article>
          </div>
        </section>

        <section class="section-card">
          <div class="section-card-head">
            <div>
              <p class="section-kicker">Recommended</p>
              <h3>推荐求助</h3>
            </div>
          </div>
          <div class="recommend-list">
            <article v-for="item in recommendedCards" :key="item.title" class="recommend-card">
              <span :class="['status-badge', item.badgeClass]">{{ item.badge }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </article>
          </div>
        </section>

        <section v-if="leaderboard.length" class="section-card">
          <div class="section-card-head">
            <div>
              <p class="section-kicker">Credit</p>
              <h3>互助达人榜</h3>
            </div>
          </div>
          <ol class="leaderboard-list">
            <li v-for="row in leaderboard" :key="row.userId">
              <span class="lb-rank">#{{ row.rank }}</span>
              <span class="lb-name">{{ row.nickname }}</span>
              <strong>{{ row.creditScore }} 分</strong>
            </li>
          </ol>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchHelpLeaderboard, fetchHelpRequests } from '@/api/help.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import { useUserStore } from '@/stores/user.js'

const { helpPath } = usePlatformPath()
const userStore = useUserStore()
const loading = ref(true)
const error = ref('')
const items = ref([])
const page = ref(1)
const totalPages = ref(1)
const totalCount = ref(0)
const filterType = ref('')
const statusFilter = ref('')
const timeFilter = ref('')
const sortBy = ref('latest')
const searchKeyword = ref('')
const leaderboard = ref([])
const savedHelpIds = ref(new Set())

const typeFilters = [
  { value: '', label: '全部分类' },
  { value: 'JOB_SEEK', label: '找工作' },
  { value: 'RECRUIT', label: '招人' },
  { value: 'FIND_MATERIAL', label: '找资料' },
  { value: 'OFFER_RESOURCE', label: '提供资源' },
  { value: 'ASK_EXP', label: '求经验' },
  { value: 'OTHER', label: '其他' }
]

const TYPE_LABELS = {
  JOB_SEEK: '找工作',
  RECRUIT: '招人',
  FIND_MATERIAL: '找资料',
  OFFER_RESOURCE: '提供资源',
  ASK_EXP: '求经验',
  OTHER: '其他'
}

const todayKey = new Date().toISOString().slice(0, 10)
const weekStart = (() => {
  const d = new Date()
  d.setDate(d.getDate() - 6)
  d.setHours(0, 0, 0, 0)
  return d
})()

const totalReplies = computed(() => items.value.reduce((sum, row) => sum + Number(row.replyCount || 0), 0))
const pendingResponseCount = computed(() => items.value.filter(row => !row.isResolved && Number(row.replyCount || 0) === 0).length)
const resolvedCount = computed(() => items.value.filter(row => row.isResolved || row.status === 'resolved').length)
const myParticipationCount = computed(() => items.value.filter(row => isMine(row)).length)
const todayNewCount = computed(() => items.value.filter(row => String(row.createdAt || '').slice(0, 10) === todayKey).length)

const heroMetrics = computed(() => [
  { label: '当前互助总数', value: totalCount.value || items.value.length, hint: '当前可见的互助任务', tone: 'accent' },
  { label: '待响应求助', value: pendingResponseCount.value, hint: '尚未收到回应的需求', tone: 'warn' },
  { label: '已解决求助', value: resolvedCount.value, hint: '已完成闭环的互助', tone: '' },
  { label: '我的参与', value: myParticipationCount.value, hint: '与我相关的互助事项', tone: '' },
  { label: '今日新增', value: todayNewCount.value, hint: '今天新增的互助需求', tone: '' },
])

const currentFilterLabel = computed(() => {
  const category = typeFilters.find(item => item.value === filterType.value)?.label || '全部分类'
  const status = statusFilter.value ? statusLabel(statusFilter.value) : '全部状态'
  return `${category} / ${status}`
})

const filteredItems = computed(() => {
  const keyword = searchKeyword.value.toLowerCase()
  let list = items.value.filter((row) => {
    const content = `${row.title || ''} ${row.summary || ''} ${row.publisherName || ''}`.toLowerCase()
    if (keyword && !content.includes(keyword)) return false
    if (statusFilter.value === 'active' && (row.isResolved || row.status === 'resolved')) return false
    if (statusFilter.value === 'resolved' && !(row.isResolved || row.status === 'resolved')) return false
    if (statusFilter.value === 'mine' && !isMine(row)) return false
    if (timeFilter.value === 'today' && String(row.createdAt || '').slice(0, 10) !== todayKey) return false
    if (timeFilter.value === 'week' && !isThisWeek(row.createdAt)) return false
    return true
  })

  list = [...list]
  if (sortBy.value === 'replies') {
    list.sort((a, b) => Number(b.replyCount || 0) - Number(a.replyCount || 0))
  } else if (sortBy.value === 'urgent') {
    list.sort((a, b) => Number(a.replyCount || 0) - Number(b.replyCount || 0))
  } else if (sortBy.value === 'updated') {
    list.sort((a, b) => String(b.updatedAt || b.createdAt || '').localeCompare(String(a.updatedAt || a.createdAt || '')))
  } else {
    list.sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
  }
  return list
})

const myMetrics = computed(() => [
  { label: '我发布的', value: items.value.filter(row => String(row.userId) === String(userStore.userId)).length, hint: '当前列表中的我的求助' },
  { label: '我响应的', value: myParticipationCount.value, hint: '当前列表中与我相关的事项' },
  { label: '待我处理的', value: pendingResponseCount.value, hint: '可优先查看待响应任务' },
  { label: '已完成的', value: resolvedCount.value, hint: '当前列表中的解决闭环' },
])

const recommendedCards = computed(() => {
  const urgent = items.value.find(row => !row.isResolved && Number(row.replyCount || 0) === 0)
  const recent = [...items.value].sort((a, b) => String(b.updatedAt || b.createdAt || '').localeCompare(String(a.updatedAt || a.createdAt || '')))[0]
  const hot = [...items.value].sort((a, b) => Number(b.replyCount || 0) - Number(a.replyCount || 0))[0]
  return [
    {
      badge: '急需响应',
      badgeClass: 'warning',
      title: urgent?.title || '暂无急需响应求助',
      desc: urgent ? `${typeLabel(urgent.helpType)} · ${formatTime(urgent.createdAt)}` : '当前列表中的求助都已有回应',
    },
    {
      badge: '最近更新',
      badgeClass: 'info',
      title: recent?.title || '暂无最近更新',
      desc: recent ? `${recent.publisherName || '用户'} · ${formatTime(recent.updatedAt || recent.createdAt)}` : '有新求助后会显示在这里',
    },
    {
      badge: '热门互助',
      badgeClass: 'success',
      title: hot?.title || '暂无热门互助',
      desc: hot ? `${hot.replyCount || 0} 个响应 · ${typeLabel(hot.helpType)}` : '响应数较高的求助会展示在这里',
    },
  ]
})

function typeLabel(code) {
  return TYPE_LABELS[code] || code || '其他'
}

function statusLabel(value) {
  return {
    active: '进行中',
    resolved: '已解决',
    mine: '我的参与',
    pending: '审核中',
    closed: '已关闭',
    rejected: '未通过'
  }[value] || value
}

function requestStatusLabel(row) {
  if (row.isResolved || row.status === 'resolved') return '已解决'
  if (Number(row.replyCount || 0) === 0) return '待响应'
  return statusLabel(row.status || 'active')
}

function statusTone(row) {
  if (row.isResolved || row.status === 'resolved') return 'success'
  if (Number(row.replyCount || 0) === 0) return 'warning'
  return 'info'
}

function formatTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function isThisWeek(iso) {
  if (!iso) return false
  const d = new Date(iso)
  return !Number.isNaN(d.getTime()) && d >= weekStart
}

function isMine(row) {
  if (!userStore.isLoggedIn || !userStore.userId) return false
  return String(row.userId) === String(userStore.userId)
}

function canOffer(row) {
  if (!userStore.isLoggedIn) return true
  return String(row.userId) !== String(userStore.userId) && !row.isResolved
}

function setQuickFilter(mode) {
  if (mode === 'needsResponse') {
    statusFilter.value = 'active'
    sortBy.value = 'urgent'
    return
  }
  if (mode === 'mine') {
    statusFilter.value = 'mine'
  }
}

function resetFilters() {
  searchKeyword.value = ''
  filterType.value = ''
  statusFilter.value = ''
  timeFilter.value = ''
  sortBy.value = 'latest'
  load(1)
}

function toggleSave(row) {
  const next = new Set(savedHelpIds.value)
  if (next.has(row.id)) next.delete(row.id)
  else next.add(row.id)
  savedHelpIds.value = next
}

async function shareHelp(row) {
  const url = `${window.location.origin}${window.location.pathname}#${helpPath(String(row.id))}`
  const title = row.title || '互助需求'
  if (navigator.share) {
    await navigator.share({ title, url })
    return
  }
  await navigator.clipboard?.writeText(url)
}

async function load(p) {
  loading.value = true
  error.value = ''
  page.value = p || page.value
  try {
    const res = await fetchHelpRequests({
      helpType: filterType.value || undefined,
      pageNum: page.value,
      pageSize: 15
    })
    items.value = Array.isArray(res?.list) ? res.list : []
    totalPages.value = Number(res?.totalPages) || 1
    totalCount.value = Number(res?.total) || items.value.length
  } catch (e) {
    error.value = e.message || '加载失败'
    items.value = []
    totalCount.value = 0
  } finally {
    loading.value = false
  }
}

function goPage(p) {
  page.value = p
  load(p)
}

onMounted(async () => {
  load(1)
  try {
    const res = await fetchHelpLeaderboard({ limit: 8 })
    leaderboard.value = Array.isArray(res?.items) ? res.items : []
  } catch {
    leaderboard.value = []
  }
})
</script>

<style scoped>
.help-hub {
  width: calc(100% - 48px);
  margin: 0 auto;
  padding: 24px 0 48px;
  color: var(--lc-text);
}

.help-hero {
  align-items: stretch;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 520px);
}

.hero-copy {
  display: grid;
  align-content: start;
  gap: 14px;
}

.hero-copy h1 {
  margin: 0;
  color: var(--lc-text);
  font-size: 32px;
  line-height: 1.2;
}

.hero-desc {
  max-width: 720px;
  margin: 0;
  color: var(--lc-muted);
  font-size: 14px;
  line-height: 1.75;
}

.hero-status,
.hero-actions,
.badge-row,
.help-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-metrics .metric-card:first-child {
  grid-column: span 2;
}

.action-card {
  color: inherit;
  text-decoration: none;
}

.action-card.as-button {
  width: 100%;
  text-align: left;
  cursor: pointer;
}

.action-card strong {
  color: var(--lc-text);
}

.filter-panel {
  display: grid;
  gap: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) repeat(4, minmax(150px, 1fr));
  gap: 12px;
}

.filter-field {
  display: grid;
  gap: 6px;
}

.filter-field span {
  color: var(--lc-muted);
  font-size: 12px;
  font-weight: 900;
}

.filter-field input,
.filter-field select {
  width: 100%;
  min-height: 38px;
  box-sizing: border-box;
  border: 1px solid var(--lc-border);
  border-radius: 8px;
  background: var(--lc-surface);
  color: var(--lc-text);
  font: inherit;
  padding: 0 10px;
}

.filter-field input:focus,
.filter-field select:focus {
  outline: none;
  border-color: var(--lc-blue);
  box-shadow: 0 0 0 3px var(--lc-blue-light);
}

.hub-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.hub-main,
.hub-side {
  display: grid;
  gap: 16px;
}

.help-list {
  display: grid;
  gap: 12px;
}

.help-card {
  display: grid;
  gap: 12px;
  box-shadow: none;
}

.help-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.help-card h3 {
  margin: 8px 0 0;
  color: var(--lc-text);
  font-size: 18px;
  line-height: 1.45;
}

.reply-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 76px;
  min-height: 34px;
  border: 1px solid var(--lc-border);
  border-radius: 999px;
  background: var(--lc-bg);
  color: var(--lc-blue);
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.help-summary {
  margin: 0;
  color: var(--lc-muted);
  font-size: 14px;
  line-height: 1.7;
}

.help-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: var(--lc-subtle);
  font-size: 12px;
  font-weight: 700;
}

.platform-btn.saved {
  border-color: var(--lc-amber);
  background: var(--lc-amber-light);
  color: var(--lc-amber);
}

.empty-state p {
  margin: 6px 0 0;
  font-size: 13px;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
  color: var(--lc-muted);
  font-size: 13px;
  font-weight: 800;
}

.side-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.side-metrics .metric-card {
  min-height: 112px;
}

.recommend-list,
.leaderboard-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.recommend-card {
  display: grid;
  gap: 8px;
  border: 1px solid var(--lc-border);
  border-radius: 10px;
  background: var(--lc-bg);
  padding: 12px;
}

.recommend-card strong {
  color: var(--lc-text);
  font-size: 14px;
  line-height: 1.5;
}

.recommend-card p {
  margin: 0;
  color: var(--lc-muted);
  font-size: 13px;
  line-height: 1.6;
}

.leaderboard-list {
  list-style: none;
  padding: 0;
}

.leaderboard-list li {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--lc-soft);
  font-size: 13px;
}

.lb-rank {
  color: var(--lc-indigo);
  font-weight: 900;
}

.lb-name {
  overflow: hidden;
  color: var(--lc-slate);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leaderboard-list strong {
  color: var(--lc-emerald);
}

@media (max-width: 1180px) {
  .help-hero,
  .hub-layout {
    grid-template-columns: 1fr;
  }

  .hero-metrics {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }

  .hero-metrics .metric-card:first-child {
    grid-column: span 1;
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .help-hub {
    width: calc(100% - 24px);
    padding-top: 10px;
  }

  .help-hero {
    padding: 12px;
    gap: 10px;
  }

  .hero-copy h1 {
    font-size: 20px;
  }

  .hero-desc {
    margin-top: 4px;
    font-size: 12px;
    line-height: 1.45;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .hero-status {
    margin-top: 8px;
  }

  .hero-actions {
    margin-top: 8px;
    gap: 8px;
  }

  .hero-actions .platform-btn {
    height: 38px;
    font-size: 13px;
  }

  .hero-metrics {
    display: flex;
    flex-wrap: nowrap;
    gap: 0;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    margin-top: 8px;
    padding: 6px 4px;
    border: 1px solid var(--lc-border);
    border-radius: 10px;
    background: var(--lc-surface);
  }

  .hero-metrics .metric-card {
    flex: 1 1 0;
    min-width: 68px;
    min-height: 0;
    gap: 2px;
    padding: 6px 4px;
    border: 0;
    border-radius: 0;
    border-top: 0;
    box-shadow: none;
    background: transparent;
    text-align: center;
    justify-items: center;
  }

  .hero-metrics .metric-card:not(:first-child) {
    border-left: 1px solid var(--lc-border);
  }

  .hero-metrics .metric-card:first-child {
    grid-column: auto;
  }

  .hero-metrics .metric-card strong {
    font-size: 15px;
  }

  .hero-metrics .metric-label {
    font-size: 10px;
  }

  .hero-metrics .metric-card p {
    display: none;
  }

  .dashboard-section {
    padding: 12px;
  }

  .operation-section-head {
    margin-bottom: 8px;
  }

  .operation-section-head h2 {
    font-size: 16px;
  }

  .action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .action-card {
    padding: 10px;
    gap: 6px;
  }

  .action-card p {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 11px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .side-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .help-card-head {
    display: grid;
  }

  .reply-count {
    width: fit-content;
  }
}
</style>
