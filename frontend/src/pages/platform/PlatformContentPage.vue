<template>
  <main class="content-hub operation-shell">
    <header class="hub-hero operation-hero">
      <div class="hero-copy">
        <p class="section-kicker">Content Hub</p>
        <h1>内容中心</h1>
        <p>聚合平台公告、文章、活动、本地资源与每日心声，快速发现值得继续阅读和参与的内容。</p>
        <div class="filter-summary">
          <span class="status-badge info">{{ activeTypeLabel }}</span>
          <span class="status-badge neutral">{{ activeSortLabel }}</span>
          <span class="status-badge neutral">{{ activeTimeLabel }}</span>
          <span v-if="keyword" class="status-badge warning">搜索：{{ keyword }}</span>
        </div>
      </div>
      <div class="metric-grid compact hero-metrics">
        <article v-for="item in hubMetrics" :key="item.label" class="metric-card">
          <span class="metric-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.hint }}</p>
        </article>
      </div>
    </header>

    <section class="filter-panel section-card" :class="{ 'is-open': filterOpen }" aria-label="内容筛选">
      <div class="filter-toolbar">
        <button type="button" class="filter-toggle" :aria-expanded="filterOpen" @click="filterOpen = !filterOpen">
          <strong>筛选</strong>
          <span class="filter-toggle-meta">{{ filterSummaryText }}</span>
          <span class="filter-toggle-icon">{{ filterOpen ? '收起' : '展开' }}</span>
        </button>
        <button type="button" class="filter-reset" @click="resetFilters">重置</button>
      </div>
      <div class="section-card-head filter-head-desktop">
        <h3>筛选内容</h3>
        <button type="button" class="platform-btn platform-btn-ghost" @click="resetFilters">重置</button>
      </div>
      <div class="filter-body">
        <div class="filter-grid">
          <label class="search">
            <span>搜索</span>
            <input v-model.trim="keyword" placeholder="搜索标题、摘要、作者">
          </label>
          <label>
            <span>分类</span>
            <select v-model="activeCategory">
              <option value="all">全部分类</option>
              <option v-for="item in categoryOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>时间</span>
            <select v-model="activeTime">
              <option value="all">全部时间</option>
              <option value="week">本周</option>
              <option value="month">本月</option>
            </select>
          </label>
        </div>
        <div class="tab-row">
          <span>内容类型</span>
          <ContentTabs v-model="activeType" :tabs="contentTypes" />
        </div>
        <div class="tab-row">
          <span>排序</span>
          <ContentFilterBar v-model="activeSort" :sorts="PLATFORM_SORTS" />
        </div>
      </div>
    </section>

    <section class="dashboard-section recommend-section">
      <div class="operation-section-head">
        <div>
          <p class="section-kicker">Recommended</p>
          <h2>推荐内容</h2>
        </div>
        <button type="button" class="platform-btn platform-btn-primary" @click="goPublish">发布内容</button>
      </div>
      <div v-if="loading" class="loading-state">内容加载中...</div>
      <div v-else-if="loadError" class="error-state">
        <h3>内容加载失败</h3>
        <p>{{ loadError }}</p>
        <button type="button" class="platform-btn platform-btn-ghost" @click="loadContent">重试</button>
      </div>
      <div v-else class="recommend-grid">
        <article v-for="item in recommendedItems" :key="item.id" class="section-card recommend-card">
          <span class="status-badge" :class="item.pinned ? 'warning' : 'info'">{{ item.pinned ? '官方推荐' : typeLabel(item.type) }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.summary || '暂无摘要' }}</p>
          <div class="recommend-meta">
            <span>{{ item.authorName }}</span>
            <span>{{ item.createdAt || '未记录时间' }}</span>
            <span>{{ interactionCount(item) }} 互动</span>
          </div>
        </article>
        <p v-if="!recommendedItems.length" class="empty-state compact">暂无推荐内容</p>
      </div>
    </section>

    <section class="content-layout">
      <section class="dashboard-section list-section">
        <div class="operation-section-head">
          <div>
            <p class="section-kicker">Resources</p>
            <h2>内容列表</h2>
          </div>
          <span class="status-badge neutral">当前 {{ filtered.length }} 条</span>
        </div>
        <div v-if="loading" class="loading-state">内容加载中...</div>
        <div v-else-if="loadError" class="error-state">{{ loadError }}</div>
        <ContentList
          v-else-if="filtered.length"
          :items="filtered"
          @like="handleLike"
          @comment="handleComment"
          @view="handleView"
          @share="handleShare"
        />
        <ContentEmpty v-else @publish="goPublish" />
      </section>

      <aside class="section-card recent-panel">
        <div class="section-card-head">
          <h3>最近阅读</h3>
          <span class="status-badge neutral">{{ recentReading.length }} 条</span>
        </div>
        <div v-if="recentReading.length" class="recent-list">
          <button v-for="item in recentReading" :key="item.id" type="button" class="recent-item" @click="handleView(item)">
            <strong>{{ item.title }}</strong>
            <span>{{ typeLabel(item.type) }} · {{ item.createdAt || '最近' }}</span>
          </button>
        </div>
        <p v-else class="empty-state compact">阅读内容后会在这里留下记录</p>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ContentTabs from '@/components/platform/content/ContentTabs.vue'
import ContentFilterBar from '@/components/platform/content/ContentFilterBar.vue'
import ContentList from '@/components/platform/content/ContentList.vue'
import ContentEmpty from '@/components/platform/content/ContentEmpty.vue'
import {
  fetchAnnouncements,
  fetchArticles,
  fetchEvents,
  fetchPlatformContentFeed
} from '@/api/platformContent.js'
import {
  commentPositiveShare,
  fetchPositiveShares,
  likePositiveShare,
  unlikePositiveShare
} from '@/api/positiveShare.js'
import { getLocalResources } from '@/api/localResources.js'
import { PLATFORM_CONTENT_TYPES, PLATFORM_SORTS } from '@/mock/platformContent.js'
import { userAvatarUrlFromApi } from '@/utils/displayFields.js'

const route = useRoute()
const router = useRouter()
const keyword = ref('')
const filterOpen = ref(false)
const activeType = ref('local')
const activeSort = ref('latest')
const activeCategory = ref('all')
const activeTime = ref('all')
const contentList = ref([])
const loading = ref(false)
const loadError = ref('')
const recentReading = ref([])
const contentTypes = [{ key: 'local', label: '本地资源' }, ...PLATFORM_CONTENT_TYPES]
const timeFilters = [
  { key: 'all', label: '全部时间' },
  { key: 'week', label: '本周' },
  { key: 'month', label: '本月' }
]

watch(() => route.query.type, (type) => {
  if (typeof type === 'string' && contentTypes.some(item => item.key === type)) activeType.value = type
}, { immediate: true })

const categoryOptions = computed(() => {
  const set = new Set()
  contentList.value.forEach((item) => {
    const tags = Array.isArray(item.tags) ? item.tags : []
    tags.forEach((tag) => { if (tag) set.add(tag) })
    const fallback = typeLabel(item.type)
    if (fallback) set.add(fallback)
  })
  return [...set].slice(0, 12)
})

const filtered = computed(() => {
  let list = contentList.value.filter(item => activeType.value === 'all' || item.type === activeType.value)
  if (activeCategory.value !== 'all') {
    list = list.filter((item) => {
      const tags = Array.isArray(item.tags) ? item.tags : []
      return tags.includes(activeCategory.value) || typeLabel(item.type) === activeCategory.value
    })
  }
  if (activeTime.value !== 'all') list = list.filter(item => isWithinTimeFilter(item.createdAt, activeTime.value))
  if (keyword.value) {
    const q = keyword.value.toLowerCase()
    list = list.filter(item =>
      item.title.toLowerCase().includes(q) ||
      item.summary.toLowerCase().includes(q) ||
      String(item.authorName || '').toLowerCase().includes(q)
    )
  }
  if (activeSort.value === 'hot') list = [...list].sort((a,b)=>(b.likeCount+b.commentCount)-(a.likeCount+a.commentCount))
  if (activeSort.value === 'recommend') list = [...list].sort((a,b)=>Number(b.pinned)-Number(a.pinned))
  if (activeSort.value === 'latest') list = [...list].sort((a,b)=>String(b.createdAt).localeCompare(String(a.createdAt)))
  return list
})

const recommendedItems = computed(() => {
  const pinned = contentList.value.filter(item => item.pinned)
  const hot = [...contentList.value].sort((a, b) => interactionCount(b) - interactionCount(a))
  const latest = [...contentList.value].sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
  const seen = new Set()
  return [...pinned, ...hot, ...latest].filter((item) => {
    if (seen.has(item.id)) return false
    seen.add(item.id)
    return true
  }).slice(0, 3)
})

const latestUpdatedLabel = computed(() => {
  const latest = [...contentList.value].sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))[0]
  const raw = String(latest?.createdAt || '').trim()
  const matched = raw.match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (matched) return `${matched[2]}-${matched[3]}`
  return raw || '--'
})

const hubMetrics = computed(() => [
  { label: '内容总量', value: contentList.value.length, hint: '平台聚合内容' },
  { label: '本周新增', value: contentList.value.filter(item => isWithinTimeFilter(item.createdAt, 'week')).length, hint: '7 天内更新' },
  { label: '推荐内容', value: recommendedItems.value.length, hint: '官方推荐与热门' },
  { label: '最近更新', value: latestUpdatedLabel.value, hint: '最新发布时间' }
])

const activeTypeLabel = computed(() => `类型：${contentTypes.find(item => item.key === activeType.value)?.label || '全部'}`)
const activeSortLabel = computed(() => `排序：${PLATFORM_SORTS.find(item => item.key === activeSort.value)?.label || '最新'}`)
const activeTimeLabel = computed(() => `时间：${timeFilters.find(item => item.key === activeTime.value)?.label || '全部时间'}`)
const filterSummaryText = computed(() => {
  const parts = [
    contentTypes.find(item => item.key === activeType.value)?.label,
    PLATFORM_SORTS.find(item => item.key === activeSort.value)?.label,
    timeFilters.find(item => item.key === activeTime.value)?.label
  ]
  if (activeCategory.value !== 'all') parts.push(activeCategory.value)
  if (keyword.value) parts.push(keyword.value)
  return parts.filter(Boolean).join(' · ')
})

function goPublish(){ router.push('/platform/positive-share') }

function resetFilters() {
  keyword.value = ''
  activeType.value = 'all'
  activeSort.value = 'latest'
  activeCategory.value = 'all'
  activeTime.value = 'all'
}

function typeLabel(type) {
  return contentTypes.find(item => item.key === type)?.label || '内容'
}

function interactionCount(item) {
  return Number(item?.likeCount || 0) + Number(item?.commentCount || 0)
}

function isWithinTimeFilter(raw, type) {
  if (type === 'all') return true
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return false
  const diff = Date.now() - date.getTime()
  if (type === 'week') return diff <= 7 * 86400000
  if (type === 'month') return diff <= 31 * 86400000
  return true
}

function unwrapList(res) {
  if (Array.isArray(res)) return res
  if (Array.isArray(res?.data)) return res.data
  if (Array.isArray(res?.data?.list)) return res.data.list
  if (Array.isArray(res?.data?.records)) return res.data.records
  if (Array.isArray(res?.list)) return res.list
  if (Array.isArray(res?.records)) return res.records
  if (Array.isArray(res?.content)) return res.content
  return []
}

/** 聚合接口体：兼容 { announcements, ... } 或 { data: { ... } } */
function unwrapContentFeedBody(raw) {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return null
  const inner = raw.data && typeof raw.data === 'object' && !Array.isArray(raw.data) ? raw.data : raw
  if (
    inner &&
    typeof inner === 'object' &&
    ('announcements' in inner || 'articles' in inner || 'events' in inner || 'positiveShares' in inner)
  ) {
    return inner
  }
  return null
}

function mergeNormalizedFromFeedBody(body) {
  const announcements = unwrapList(body.announcements).map(normalizeAnnouncement)
  const articles = unwrapList(body.articles).map(normalizeArticle)
  const events = unwrapList(body.events).map(normalizeEvent)
  const moods = unwrapList(body.positiveShares).map(normalizePositiveShare)
  return [...announcements, ...articles, ...events, ...moods]
}

async function loadContentListLegacy() {
  const [localRes, annRes, artRes, eventRes, moodRes] = await Promise.allSettled([
    getLocalResources({ type: 'all' }),
    fetchAnnouncements({ status: 'published', limit: 30 }),
    fetchArticles({ status: 'published', limit: 30 }),
    fetchEvents({ status: 'published', limit: 30 }),
    fetchPositiveShares({ tab: 'latest', pageNum: 1, pageSize: 50 })
  ])
  const locals = localRes.status === 'fulfilled' ? unwrapList(localRes.value).map(normalizeLocalResource) : []
  const announcements = annRes.status === 'fulfilled' ? unwrapList(annRes.value).map(normalizeAnnouncement) : []
  const articles = artRes.status === 'fulfilled' ? unwrapList(artRes.value).map(normalizeArticle) : []
  const events = eventRes.status === 'fulfilled' ? unwrapList(eventRes.value).map(normalizeEvent) : []
  const moods = moodRes.status === 'fulfilled' ? unwrapList(moodRes.value).map(normalizePositiveShare) : []
  return [...locals, ...announcements, ...articles, ...events, ...moods]
}

function resolveTypeFromArticle(item) {
  const category = String(item?.category || item?.tag || '').toLowerCase()
  const summary = `${item?.title || ''} ${item?.summary || ''}`.toLowerCase()
  if (category.includes('ai') || summary.includes('ai')) return 'ai'
  if (category.includes('攻略') || summary.includes('攻略')) return 'guide'
  if (category.includes('心声') || summary.includes('心声')) return 'mood'
  if (category.includes('动态') || summary.includes('动态')) return 'dynamic'
  if (category.includes('活动') || summary.includes('活动')) return 'event'
  return 'guide'
}

function normalizeArticle(item) {
  return {
    id: item.id ?? `article-${Math.random()}`,
    title: item.title || '未命名内容',
    summary: item.summary || item.description || '',
    type: resolveTypeFromArticle(item),
    cover: item.coverUrl || item.cover || '',
    images: [],
    tags: item.tag ? [item.tag] : [],
    authorName: item.authorName || item.author || '平台编辑',
    avatar: userAvatarUrlFromApi(item),
    createdAt: String(item.publishedAt || item.createdAt || '').slice(0, 10),
    likeCount: Number(item.likeCount || item.likes || 0),
    commentCount: Number(item.commentCount || 0),
    pinned: Boolean(item.pinned)
  }
}

function normalizeAnnouncement(item) {
  return {
    id: item.id ?? `announcement-${Math.random()}`,
    title: item.title || '平台公告',
    summary: item.summary || item.content || '',
    type: 'dynamic',
    cover: item.coverUrl || item.cover || '',
    images: [],
    tags: ['平台公告'],
    authorName: item.authorName || item.publisher || '平台运营',
    avatar: userAvatarUrlFromApi(item),
    createdAt: String(item.publishDate || item.createdAt || '').slice(0, 10),
    likeCount: Number(item.likeCount || 0),
    commentCount: Number(item.commentCount || 0),
    pinned: Boolean(item.pinned)
  }
}

function normalizeEvent(item) {
  return {
    id: item.id ?? `event-${Math.random()}`,
    title: item.title || '平台活动',
    summary: item.summary || item.description || '',
    type: 'event',
    cover: item.coverUrl || item.cover || '',
    images: [],
    tags: ['活动'],
    authorName: item.authorName || item.organizer || '活动中心',
    avatar: userAvatarUrlFromApi(item),
    createdAt: String(item.eventTime || item.startTime || item.createdAt || '').slice(0, 10),
    likeCount: Number(item.likeCount || 0),
    commentCount: Number(item.commentCount || 0),
    pinned: Boolean(item.pinned)
  }
}

function normalizePositiveShare(item) {
  const resolvedName =
    item.nickname ||
    item.nickName ||
    item.userNickname ||
    item.username ||
    item.userName ||
    item.accountName ||
    item.authorName ||
    item.author ||
    '用户'

  return {
    id: item.id ?? `mood-${Math.random()}`,
    title: item.title || '每日心声',
    summary: item.content || item.summary || '',
    type: 'mood',
    cover: item.coverUrl || item.cover || '',
    images: Array.isArray(item.images) ? item.images : [],
    tags: item.category ? [item.category] : [],
    authorName: resolvedName,
    avatar: userAvatarUrlFromApi(item),
    createdAt: String(item.createdAt || '').slice(0, 10),
    likeCount: Number(item.encourageCount || item.likeCount || 0),
    commentCount: Number(item.commentCount || 0),
    pinned: Boolean(item.pinned),
    liked: Boolean(item.liked),
    source: 'mood'
  }
}

function normalizeLocalResource(item) {
  return {
    id: item.id ?? `local-${Math.random()}`,
    title: item.title || '本地资源',
    summary: item.summary || '暂无简介',
    type: 'local',
    cover: item.coverUrl || '',
    images: [],
    tags: [item.type || '本地资源', item.location || '同城'],
    authorName: '本地推荐',
    avatar: '',
    createdAt: String(item.updatedAt || item.createdAt || '').slice(0, 10),
    likeCount: Number(item.heat || 0),
    commentCount: Number(item.interestCount || 0),
    pinned: Number(item.heat || 0) >= 20
  }
}

async function handleLike(item) {
  if (!item || item.source !== 'mood') return
  try {
    const res = item.liked ? await unlikePositiveShare(item.id) : await likePositiveShare(item.id)
    item.liked = Boolean(res?.liked ?? !item.liked)
    item.likeCount = Number(res?.encourageCount ?? item.likeCount ?? 0)
  } catch {
    // keep UI stable on failure
  }
}

async function handleComment(item) {
  if (!item || item.source !== 'mood') return
  const content = window.prompt('请输入评论内容')
  const text = String(content || '').trim()
  if (!text) return
  try {
    const res = await commentPositiveShare(item.id, { content: text })
    item.commentCount = Number(res?.commentCount ?? item.commentCount ?? 0)
  } catch {
    // keep UI stable on failure
  }
}

function readRecentReading() {
  try {
    const raw = window.localStorage?.getItem('platform-content-recent')
    const rows = raw ? JSON.parse(raw) : []
    recentReading.value = Array.isArray(rows) ? rows.slice(0, 5) : []
  } catch {
    recentReading.value = []
  }
}

function rememberReading(item) {
  if (!item?.id) return
  const next = [
    {
      id: item.id,
      title: item.title,
      type: item.type,
      createdAt: item.createdAt
    },
    ...recentReading.value.filter(row => row.id !== item.id)
  ].slice(0, 5)
  recentReading.value = next
  try {
    window.localStorage?.setItem('platform-content-recent', JSON.stringify(next))
  } catch {
    // local history is best effort
  }
}

function handleView(item) {
  rememberReading(item)
}

async function handleShare(item) {
  rememberReading(item)
  const path = window.location.pathname && window.location.pathname !== '/'
    ? window.location.pathname
    : '/platform/content'
  const url = `${window.location.origin}${path}?content=${encodeURIComponent(item.id)}`
  try {
    await navigator?.clipboard?.writeText(url)
  } catch {
    window.prompt('复制分享链接', url)
  }
}

async function loadContent() {
  loading.value = true
  loadError.value = ''
  try {
    const [localRaw, raw] = await Promise.all([
      getLocalResources({ type: 'all' }).catch(() => []),
      fetchPlatformContentFeed({ limit: 30 })
    ])
    const localItems = unwrapList(localRaw).map(normalizeLocalResource)
    const body = unwrapContentFeedBody(raw)
    if (body) {
      contentList.value = [...localItems, ...mergeNormalizedFromFeedBody(body)]
      loading.value = false
      return
    }
  } catch (error) {
    loadError.value = error?.message || ''
    // 未部署 content-feed 或网络错误：回退四条接口（与改聚合接口前行为一致）
  }
  try {
    contentList.value = await loadContentListLegacy()
  } catch (error) {
    loadError.value = error?.message || '无法加载内容'
    contentList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  readRecentReading()
  await loadContent()
})
</script>

<style scoped>
.content-hub {
  width: min(100% - 32px, 1320px);
  margin: var(--lc-space-4) auto;
  padding-bottom: calc(var(--lc-space-8) + env(safe-area-inset-bottom));
}

.hub-hero {
  align-items: stretch;
}

.hero-copy h1 {
  margin: 0;
  color: var(--lc-text);
  font-size: 26px;
  line-height: 1.25;
}

.section-card-head h3 {
  font-size: var(--lc-text-lg);
}

.hero-copy p:not(.section-kicker) {
  max-width: 620px;
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  line-height: 1.7;
}

.filter-summary,
.tab-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--lc-space-2);
}

.filter-summary {
  margin-top: var(--lc-space-4);
}

.metric-grid.hero-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 0;
}

.filter-toolbar {
  display: none;
}

.filter-panel {
  display: grid;
  gap: var(--lc-space-4);
}

.filter-body {
  display: grid;
  gap: var(--lc-space-4);
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px 160px;
  gap: var(--lc-space-3);
}

.filter-grid label {
  display: grid;
  gap: var(--lc-space-2);
  color: var(--lc-muted);
  font-size: var(--lc-text-xs);
  font-weight: 900;
}

.filter-grid input,
.filter-grid select {
  width: 100%;
  height: 40px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  padding: 0 var(--lc-space-3);
  color: var(--lc-text);
  background: var(--lc-surface);
}

.tab-row > span {
  flex: 0 0 72px;
  color: var(--lc-muted);
  font-size: var(--lc-text-xs);
  font-weight: 900;
}

.recommend-section,
.list-section {
  display: grid;
  gap: var(--lc-space-4);
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--lc-space-3);
}

.recommend-card {
  display: grid;
  gap: var(--lc-space-2);
}

.recommend-card h3 {
  margin: 0;
  color: var(--lc-text);
  font-size: var(--lc-text-lg);
}

.recommend-card p,
.recommend-meta,
.recent-item span {
  margin: 0;
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
}

.recommend-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--lc-space-4);
}

.recent-panel {
  align-self: start;
  display: grid;
  gap: var(--lc-space-3);
}

.recent-list {
  display: grid;
  gap: var(--lc-space-2);
}

.recent-item {
  display: grid;
  gap: 3px;
  width: 100%;
  text-align: left;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  padding: var(--lc-space-3);
  background: var(--lc-soft);
  cursor: pointer;
}

.recent-item strong {
  min-width: 0;
  overflow: hidden;
  color: var(--lc-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state.compact {
  min-height: 96px;
  margin: 0;
}

@media (max-width: 980px) {
  .content-hub {
    width: min(100% - 24px, 720px);
  }

  .hub-hero,
  .filter-grid,
  .recommend-grid,
  .content-layout {
    grid-template-columns: 1fr;
  }

  .metric-grid.hero-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .content-hub {
    width: min(100% - 20px, 720px);
    margin-top: 8px;
    gap: 8px;
    text-size-adjust: 100%;
    -webkit-text-size-adjust: 100%;
  }

  .hub-hero {
    padding: 12px;
    gap: 10px;
  }

  .hero-copy h1 {
    font-size: var(--lc-text-lg);
  }

  .hero-copy p:not(.section-kicker) {
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .filter-summary,
  .filter-head-desktop {
    display: none;
  }

  .section-card-head h3,
  .operation-section-head h2 {
    font-size: var(--lc-text-md);
  }

  .filter-panel {
    gap: 0;
    padding: 6px 10px;
  }

  .filter-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .filter-toggle {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0;
    height: 36px;
    padding: 0;
    border: 0;
    background: transparent;
    color: var(--lc-text);
    text-align: left;
    cursor: pointer;
  }

  .filter-toggle strong {
    flex: 0 0 auto;
    font-size: 14px;
  }

  .filter-toggle-meta {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    color: var(--lc-muted);
    font-size: 12px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .filter-toggle-icon {
    flex: 0 0 auto;
    color: var(--lc-blue);
    font-size: 12px;
    font-weight: 800;
  }

  .filter-reset {
    height: 32px;
    padding: 0 8px;
    border: 0;
    background: transparent;
    color: var(--lc-blue);
    font-size: 13px;
    font-weight: 800;
    cursor: pointer;
  }

  .filter-body {
    display: none;
  }

  .filter-panel.is-open {
    gap: 10px;
    padding-bottom: 10px;
  }

  .filter-panel.is-open .filter-body {
    display: grid;
    gap: 10px;
  }

  .filter-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .filter-grid .search {
    grid-column: 1 / -1;
  }

  .filter-grid label {
    gap: 4px;
    font-size: 11px;
  }

  .filter-grid input,
  .filter-grid select {
    height: 36px;
  }

  .tab-row {
    display: flex;
    flex-wrap: nowrap;
    gap: 8px;
  }

  .tab-row > span {
    display: none;
  }

  /* 四项指标压成单行条，避免大数字把首屏撑满 */
  .metric-grid.hero-metrics {
    display: flex;
    flex-wrap: nowrap;
    gap: 0;
    margin-top: 8px;
    padding: 6px 4px;
    border: 1px solid var(--lc-border);
    border-radius: 10px;
    background: var(--lc-surface);
  }

  .metric-grid.hero-metrics .metric-card {
    flex: 1 1 0;
    min-width: 0;
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

  .metric-grid.hero-metrics .metric-card:not(:first-child) {
    border-left: 1px solid var(--lc-border);
  }

  .metric-grid.hero-metrics .metric-card strong {
    font-size: 15px;
    white-space: nowrap;
  }

  .metric-grid.hero-metrics .metric-label {
    font-size: 10px;
  }

  .metric-grid.hero-metrics .metric-card p {
    display: none;
  }

  .recommend-section,
  .list-section,
  .dashboard-section {
    gap: 8px;
    padding: 10px;
  }

  .recommend-section .section-kicker,
  .list-section .section-kicker {
    display: none;
  }

  .recommend-section .operation-section-head,
  .list-section .operation-section-head {
    flex-direction: row;
    align-items: center;
    margin-bottom: 0;
  }

  .recommend-section .operation-section-head h2,
  .list-section .operation-section-head h2 {
    font-size: 15px;
  }

  .recommend-section .platform-btn {
    width: auto;
    height: 30px;
    padding: 0 10px;
    font-size: 12px;
  }

  .recommend-grid {
    gap: 8px;
  }

  .recommend-card {
    gap: 4px;
    padding: 10px;
    border-radius: 10px;
    box-shadow: none;
  }

  .recommend-card h3 {
    font-size: 15px;
    line-height: 1.35;
  }

  .recommend-card p {
    font-size: 12px;
    line-height: 1.45;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .recommend-meta {
    gap: 8px;
    font-size: 11px;
  }
}
</style>
