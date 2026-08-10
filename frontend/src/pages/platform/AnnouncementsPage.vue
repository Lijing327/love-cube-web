<template>
  <section class="announcements-page">
    <section class="bulletin-hero">
      <div class="hero-copy">
        <div class="hero-icon" aria-hidden="true">
          <span>!</span>
        </div>
        <div>
          <p class="platform-kicker">Platform Bulletin</p>
          <h1 class="hero-title">平台公告</h1>
          <p class="hero-subtitle">
            聚合发布平台更新、规则说明、功能上新和活动通知，帮助用户快速了解平台公告。
          </p>
          <div class="hero-stats" aria-label="公告概览">
            <article v-for="stat in heroStats" :key="stat.label" class="hero-stat">
              <span class="stat-icon" :class="stat.tone" aria-hidden="true">{{ stat.icon }}</span>
              <strong>{{ stat.value }}</strong>
              <small>{{ stat.label }}</small>
            </article>
          </div>
        </div>
      </div>

      <div class="hero-art" aria-hidden="true">
        <div class="megaphone">
          <span class="horn"></span>
          <span class="body"></span>
          <span class="handle"></span>
          <i class="signal signal-one"></i>
          <i class="signal signal-two"></i>
          <i class="spark spark-one"></i>
          <i class="spark spark-two"></i>
        </div>
      </div>

      <form class="hero-search" role="search" @submit.prevent>
        <input v-model.trim="keyword" type="search" placeholder="搜索公告标题、关键词..." aria-label="搜索公告">
        <span aria-hidden="true">🔍</span>
      </form>
    </section>

    <div class="bulletin-layout">
      <main class="bulletin-main">
        <div class="content-filter" aria-label="公告分类">
          <button
            v-for="cat in filterTabs"
            :key="cat"
            class="filter-btn"
            :class="{ active: cat === activeCategory }"
            type="button"
            @click="setCategory(cat)"
          >
            {{ cat }}
          </button>
          <select v-model="sortMode" class="sort-select" aria-label="排序方式">
            <option value="latest">最新发布</option>
            <option value="views">阅读最高</option>
          </select>
        </div>

        <section class="announcement-list">
          <router-link
            v-for="item in visibleList"
            :key="item.id"
            :to="`/announcements/${item.id}`"
            class="announcement-card"
            :class="[`tone-${getTone(item.category)}`, { 'is-pinned': item.pinned }]"
          >
            <div class="announcement-body">
              <div class="announcement-meta">
                <span>{{ formatDate(item.publishDate || item.date) }}</span>
                <b v-if="item.pinned">重要</b>
                <b v-else-if="item.recommended">推荐</b>
              </div>
              <h2>{{ item.title || '未命名公告' }}</h2>
              <p>{{ item.summary || '暂无摘要信息' }}</p>
              <div class="announcement-tags">
                <span>{{ formatViews(item.viewCount) }} 阅读</span>
                <span>{{ sourceLabel(item.category) }}</span>
              </div>
            </div>
            <div class="announcement-visual" aria-hidden="true">
              <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" loading="lazy">
              <span v-else>{{ getCategoryIcon(item.category) }}</span>
            </div>
            <span class="card-arrow" aria-hidden="true">→</span>
          </router-link>

          <article v-if="!loading && !filteredList.length" class="empty-state">
            <h3>暂无公告</h3>
            <p>当前没有符合条件的公告内容，换个分类或关键词试试。</p>
          </article>
        </section>

        <div v-if="hasMore" class="load-more-wrap">
          <button type="button" class="load-more-btn" @click="page++">查看更多</button>
        </div>
      </main>

      <aside class="bulletin-sidebar" aria-label="公告侧栏">
        <section class="side-card category-card">
          <h3>公告分类</h3>
          <div class="category-grid">
            <button
              v-for="cat in categoryCards"
              :key="cat.name"
              type="button"
              class="category-tile"
              :class="{ active: cat.name === activeCategory }"
              @click="setCategory(cat.name)"
            >
              <span :class="`tone-${cat.tone}`" aria-hidden="true">{{ cat.icon }}</span>
              <strong>{{ cat.name }}</strong>
              <small>{{ cat.count }}</small>
            </button>
          </div>
        </section>

        <section class="side-card">
          <h3>热门阅读</h3>
          <ol class="hot-list">
            <li v-for="(item, index) in hotList" :key="item.id">
              <i>{{ index + 1 }}</i>
              <router-link :to="`/announcements/${item.id}`">{{ item.title || '未命名公告' }}</router-link>
              <span>{{ formatViews(item.viewCount) }}</span>
            </li>
          </ol>
        </section>

        <section class="notify-card">
          <div>
            <h3>想第一时间获取动态？</h3>
            <p>开启平台通知，不错过任何重要更新。</p>
            <router-link to="/me">去设置</router-link>
          </div>
          <span aria-hidden="true">✓</span>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { fetchAnnouncements } from '@/api/platformContent.js'

const PAGE_SIZE = 6
const loading = ref(false)
const allItems = ref([])
const activeCategory = ref('全部')
const page = ref(1)
const keyword = ref('')
const sortMode = ref('latest')


const normalizedItems = computed(() => allItems.value.map((item, index) => ({
  ...item,
  id: item.id || `announcement-${index}`,
  category: item.category || '平台公告',
  viewCount: Number(item.viewCount || item.views || 0)
})))

const categories = computed(() => [...new Set(normalizedItems.value.map(i => i.category).filter(Boolean))])
const filterTabs = computed(() => ['全部', ...categories.value])

const filteredList = computed(() => {
  const term = keyword.value.toLowerCase()
  const scoped = activeCategory.value === '全部'
    ? normalizedItems.value
    : normalizedItems.value.filter(i => i.category === activeCategory.value)

  const searched = term
    ? scoped.filter(i => `${i.title || ''} ${i.summary || ''} ${i.category || ''}`.toLowerCase().includes(term))
    : scoped

  return [...searched].sort((a, b) => {
    if (sortMode.value === 'views') return Number(b.viewCount || 0) - Number(a.viewCount || 0)
    return dateValue(b.publishDate || b.date) - dateValue(a.publishDate || a.date)
  })
})

const visibleList = computed(() => filteredList.value.slice(0, page.value * PAGE_SIZE))
const hasMore = computed(() => filteredList.value.length > page.value * PAGE_SIZE)
const totalViews = computed(() => normalizedItems.value.reduce((sum, item) => sum + Number(item.viewCount || 0), 0))
const latestMonthCount = computed(() => normalizedItems.value.filter(item => {
  const date = dateValue(item.publishDate || item.date)
  return date && Date.now() - date < 1000 * 60 * 60 * 24 * 30
}).length)

const heroStats = computed(() => [
  { label: '公告总数', value: normalizedItems.value.length, icon: '告', tone: 'blue' },
  { label: '本月更新', value: latestMonthCount.value, icon: '新', tone: 'violet' },
  { label: '总阅读', value: formatViews(totalViews.value), icon: '读', tone: 'cyan' },
  { label: '通知触达', value: '98%', icon: '达', tone: 'orange' }
])

const categoryCards = computed(() => {
  const base = categories.value.map((name) => ({
    name,
    count: normalizedItems.value.filter(item => item.category === name).length,
    icon: getCategoryIcon(name),
    tone: getTone(name)
  }))
  return [
    ...base,
    {
      name: '全部',
      count: normalizedItems.value.length,
      icon: '全',
      tone: 'blue'
    }
  ]
})

const hotList = computed(() => [...normalizedItems.value]
  .sort((a, b) => Number(b.viewCount || 0) - Number(a.viewCount || 0))
  .slice(0, 5))

function setCategory(cat) {
  activeCategory.value = cat
  page.value = 1
}

watch([activeCategory, keyword, sortMode], () => { page.value = 1 })

function dateValue(value) {
  if (!value) return 0
  const normalized = String(value).replace(' ', 'T')
  const time = new Date(normalized).getTime()
  return Number.isNaN(time) ? 0 : time
}

function formatDate(value) {
  if (!value) return '待发布'
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatViews(value) {
  const num = Number(value || 0)
  if (num >= 10000) return `${(num / 10000).toFixed(1)}万`
  return num.toLocaleString('zh-CN')
}

function getCategoryIcon(category = '') {
  if (category.includes('活动')) return '活'
  if (category.includes('功能')) return '功'
  if (category.includes('安全')) return '安'
  if (category.includes('规则')) return '规'
  if (category.includes('更新')) return '新'
  return '告'
}

function getTone(category = '') {
  if (category.includes('活动')) return 'orange'
  if (category.includes('功能')) return 'blue'
  if (category.includes('安全')) return 'green'
  if (category.includes('规则')) return 'violet'
  return 'pink'
}

function sourceLabel(category = '') {
  if (String(category).includes('活动')) return '[活动中心]'
  if (String(category).includes('功能')) return '[平台资讯]'
  if (String(category).includes('安全')) return '[平台资讯]'
  if (String(category).includes('规则')) return '[平台资讯]'
  return '[平台资讯]'
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await fetchAnnouncements({ status: 'published' })
    allItems.value = Array.isArray(data) ? data : []
  } catch {
    allItems.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.announcements-page {
  width: calc(100% - 48px);
  margin: 12px auto 36px;
  color: var(--lc-text);
}

.bulletin-hero {
  position: relative;
  min-height: 312px;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(280px, 0.85fr) minmax(260px, 360px);
  align-items: center;
  gap: 28px;
  padding: 48px 56px 42px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 18px;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 118, 156, 0.18), transparent 32%),
    radial-gradient(circle at 72% 18%, rgba(76, 117, 255, 0.2), transparent 30%),
    linear-gradient(120deg, #fff7fb 0%, var(--lc-blue-light) 56%, var(--lc-bg) 100%);
  box-shadow: 0 24px 70px rgba(79, 70, 229, 0.12);
}

.hero-copy {
  display: grid;
  grid-template-columns: 92px 1fr;
  align-items: start;
  gap: 26px;
  min-width: 0;
}

.hero-icon {
  width: 92px;
  height: 92px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9), 0 18px 34px rgba(236, 72, 153, 0.12);
}

.hero-icon span {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border: 4px solid var(--lc-rose);
  border-radius: 50% 50% 46% 46%;
  color: var(--lc-rose);
  font-size: 26px;
  font-weight: 900;
}

.hero-title {
  margin: 9px 0 0;
  font-size: clamp(34px, 3.2vw, 52px);
  line-height: 1.05;
  font-weight: 900;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 620px;
  margin: 16px 0 0;
  color: var(--lc-muted);
  font-size: 17px;
  line-height: 1.7;
  font-weight: 700;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(112px, 1fr));
  gap: 14px;
  margin-top: 28px;
}

.hero-stat {
  min-height: 72px;
  display: grid;
  grid-template-columns: 34px 1fr;
  grid-template-rows: auto auto;
  column-gap: 10px;
  align-items: center;
  padding: 13px 16px;
  border: 1px solid rgba(226, 232, 240, 0.72);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.stat-icon {
  grid-row: 1 / 3;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 900;
}

.hero-stat strong {
  font-size: 20px;
  line-height: 1;
}

.hero-stat small {
  margin-top: 4px;
  color: var(--lc-muted-light);
  font-weight: 700;
}

.hero-art {
  min-height: 230px;
  display: grid;
  place-items: center;
}

.megaphone {
  position: relative;
  width: 260px;
  height: 190px;
  transform: rotate(-8deg);
}

.horn,
.body,
.handle,
.signal,
.spark {
  position: absolute;
  display: block;
}

.horn {
  left: 42px;
  top: 54px;
  width: 112px;
  height: 86px;
  border-radius: 52% 12px 12px 52%;
  background: linear-gradient(135deg, var(--lc-surface) 0%, #cfdcff 42%, #5b6dff 100%);
  box-shadow: 18px 22px 36px rgba(59, 91, 255, 0.24);
}

.horn::before {
  content: '';
  position: absolute;
  left: -18px;
  top: 22px;
  width: 36px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6c7cff, #eef3ff);
}

.body {
  right: 46px;
  top: 38px;
  width: 78px;
  height: 120px;
  border-radius: 50% 18px 18px 50%;
  background: linear-gradient(135deg, var(--lc-blue-light), #7c8cff 64%, #4257ff);
  box-shadow: inset 10px 0 18px rgba(255, 255, 255, 0.62);
}

.handle {
  left: 112px;
  top: 124px;
  width: 56px;
  height: 50px;
  border-radius: 8px 8px 18px 18px;
  background: linear-gradient(180deg, #ff6ea0, #ff3f72);
  transform: rotate(14deg);
}

.signal-one {
  left: 10px;
  top: 24px;
  width: 82px;
  height: 22px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, #ff75a6);
}

.signal-two {
  left: 3px;
  top: 142px;
  width: 105px;
  height: 20px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, #ffc0a8);
}

.spark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff6d9a, #ffb8cc);
}

.spark-one {
  right: 18px;
  top: 4px;
  transform: rotate(32deg);
}

.spark-two {
  right: 10px;
  bottom: 12px;
  width: 20px;
  height: 20px;
  background: linear-gradient(135deg, var(--lc-blue), var(--lc-blue-border));
}

.hero-search {
  align-self: start;
  margin-top: 48px;
  height: 54px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px 0 24px;
  border: 1px solid var(--lc-blue-light);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 14px 36px rgba(37, 99, 235, 0.1);
}

.hero-search input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--lc-text);
  font-size: 14px;
  font-weight: 700;
}

.hero-search input::placeholder {
  color: var(--lc-subtle);
}

.hero-search span {
  color: var(--lc-slate);
  font-size: 28px;
  line-height: 1;
}

.bulletin-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  margin-top: 28px;
  align-items: start;
}

.bulletin-main,
.announcement-list,
.bulletin-sidebar {
  display: grid;
  gap: 18px;
}

.content-filter {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.filter-btn,
.sort-select,
.load-more-btn {
  border: 1px solid #e5edf8;
  background: rgba(255, 255, 255, 0.82);
  color: var(--lc-muted);
  border-radius: 10px;
  min-height: 42px;
  padding: 0 22px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.18s ease;
}

.filter-btn:hover,
.sort-select:hover,
.load-more-btn:hover {
  border-color: var(--lc-blue-border);
  color: var(--lc-blue);
  transform: translateY(-1px);
}

.filter-btn.active {
  border-color: transparent;
  background: linear-gradient(135deg, #ff5f84, #ec4075);
  color: var(--lc-surface);
  box-shadow: 0 12px 24px rgba(236, 72, 153, 0.22);
}

.sort-select {
  margin-left: auto;
  padding-right: 16px;
}

.announcement-card {
  position: relative;
  min-height: 148px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(190px, 260px) 32px;
  align-items: center;
  gap: 22px;
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  border: 1px solid var(--lc-border);
  border-left: 4px solid var(--accent);
  border-radius: 12px;
  padding: 22px 24px 22px 28px;
  background:
    linear-gradient(90deg, var(--lc-surface) 0%, rgba(255, 255, 255, 0.94) 58%, var(--soft) 100%);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.07);
  transition: all 0.2s ease;
}

.announcement-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--accent) 42%, var(--lc-border));
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.1);
}

.announcement-body {
  min-width: 0;
}

.announcement-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--lc-muted-light);
  font-size: 13px;
  font-weight: 800;
}

.announcement-meta b {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 9px;
  border-radius: 999px;
  background: var(--soft);
  color: var(--accent);
  font-size: 12px;
}

.announcement-card h2 {
  margin: 10px 0 0;
  color: var(--lc-text);
  font-size: clamp(20px, 1.55vw, 26px);
  line-height: 1.25;
  font-weight: 900;
}

.announcement-card p {
  margin: 10px 0 0;
  color: var(--lc-muted-light);
  font-size: 15px;
  line-height: 1.65;
  font-weight: 650;
}

.announcement-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
}

.announcement-tags span {
  color: var(--lc-muted-light);
  font-size: 13px;
  font-weight: 800;
}

.announcement-visual {
  height: 110px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 10px;
  background:
    radial-gradient(circle at 65% 40%, rgba(255, 255, 255, 0.9), transparent 32%),
    linear-gradient(135deg, var(--soft), var(--lc-surface));
}

.announcement-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.announcement-visual span {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  border-radius: 22px;
  background: linear-gradient(135deg, var(--lc-surface), var(--soft));
  color: var(--accent);
  font-size: 31px;
  font-weight: 900;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.1);
}

.card-arrow {
  color: var(--lc-muted-light);
  font-size: 38px;
  line-height: 1;
}

.side-card,
.notify-card,
.empty-state {
  border: 1px solid var(--lc-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.07);
}

.side-card {
  padding: 24px;
}

.side-card h3,
.notify-card h3,
.empty-state h3 {
  margin: 0;
  color: var(--lc-text);
  font-size: 20px;
  line-height: 1.3;
  font-weight: 900;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 18px;
}

.category-tile {
  min-height: 82px;
  display: grid;
  place-items: center;
  gap: 4px;
  border: 1px solid var(--lc-soft-alt);
  border-radius: 10px;
  background: var(--lc-bg);
  color: var(--lc-muted);
  cursor: pointer;
  transition: all 0.18s ease;
}

.category-tile:hover,
.category-tile.active {
  transform: translateY(-1px);
  border-color: var(--lc-blue-border);
  background: var(--lc-surface);
}

.category-tile span {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 900;
}

.category-tile strong,
.category-tile small {
  font-weight: 800;
}

.category-tile strong {
  font-size: 13px;
}

.category-tile small {
  color: var(--lc-subtle);
  font-size: 12px;
}

.hot-list {
  list-style: none;
  margin: 18px 0 0;
  padding: 0;
  display: grid;
  gap: 15px;
}

.hot-list li {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.hot-list i {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--lc-border);
  color: var(--lc-muted-light);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.hot-list li:nth-child(1) i,
.hot-list li:nth-child(2) i,
.hot-list li:nth-child(3) i {
  background: linear-gradient(135deg, #ff5f84, #ff9f43);
  color: var(--lc-surface);
}

.hot-list a {
  min-width: 0;
  overflow: hidden;
  color: var(--lc-slate);
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 800;
}

.hot-list a:hover {
  color: var(--lc-blue);
}

.hot-list span {
  color: var(--lc-amber);
  font-size: 12px;
  font-weight: 900;
}

.notify-card {
  min-height: 140px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 24px;
  background:
    radial-gradient(circle at 84% 32%, rgba(255, 107, 149, 0.16), transparent 30%),
    linear-gradient(135deg, #fff5f7, var(--lc-surface));
}

.notify-card p {
  margin: 10px 0 18px;
  color: var(--lc-muted-light);
  line-height: 1.6;
  font-weight: 700;
}

.notify-card a {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 22px;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff5f84, #ec4075);
  color: var(--lc-surface);
  text-decoration: none;
  font-size: 13px;
  font-weight: 900;
}

.notify-card > span {
  align-self: center;
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  border-radius: 24px;
  background: linear-gradient(135deg, var(--lc-blue-light), var(--lc-surface));
  color: var(--lc-rose);
  font-size: 34px;
  font-weight: 900;
  transform: rotate(-8deg);
}

.empty-state {
  padding: 34px;
}

.empty-state p {
  margin: 10px 0 0;
  color: var(--lc-muted-light);
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 4px 0 10px;
}

.load-more-btn {
  min-width: 150px;
}

.announcement-card.tone-pink,
.category-tile .tone-pink {
  --accent: var(--lc-rose);
  --soft: var(--lc-pink-light);
}

.announcement-card.tone-blue,
.category-tile .tone-blue,
.blue {
  --accent: var(--lc-blue);
  --soft: var(--lc-blue-light);
}

.category-tile .tone-blue,
.blue {
  background: var(--lc-blue-light);
  color: var(--lc-blue);
}

.announcement-card.tone-orange,
.category-tile .tone-orange,
.orange {
  --accent: var(--lc-amber);
  --soft: var(--lc-orange-light);
}

.category-tile .tone-orange,
.orange {
  background: var(--lc-orange-light);
  color: var(--lc-amber);
}

.announcement-card.tone-green,
.category-tile .tone-green,
.green {
  --accent: var(--lc-emerald);
  --soft: var(--lc-green-light);
}

.category-tile .tone-green,
.green {
  background: var(--lc-green-light);
  color: var(--lc-emerald);
}

.announcement-card.tone-violet,
.category-tile .tone-violet,
.violet {
  --accent: var(--lc-violet);
  --soft: var(--lc-indigo-soft);
}

.category-tile .tone-violet,
.violet {
  background: var(--lc-indigo-soft);
  color: var(--lc-purple);
}

.announcement-card.tone-cyan,
.cyan {
  --accent: #06b6d4;
  --soft: var(--lc-blue-light);
}

.cyan {
  background: var(--lc-blue-light);
  color: #0891b2;
}

.category-tile .tone-pink,
.pink {
  --accent: var(--lc-rose);
  --soft: var(--lc-pink-light);
  background: var(--lc-pink-light);
  color: var(--lc-rose);
}

@media (max-width: 1280px) {
  .bulletin-hero {
    grid-template-columns: minmax(0, 1fr) 300px;
  }

  .hero-search {
    grid-column: 1 / -1;
    width: min(520px, 100%);
    margin-top: 0;
    justify-self: end;
  }

  .hero-stats {
    grid-template-columns: repeat(2, minmax(112px, 1fr));
  }
}

@media (max-width: 1024px) {
  .bulletin-layout {
    grid-template-columns: 1fr;
  }

  .bulletin-sidebar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .notify-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 767px) {
  .announcements-page {
    width: calc(100% - 24px);
    margin: 8px auto 24px;
  }

  .bulletin-hero {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 16px 14px;
    border-radius: 14px;
  }

  .hero-copy {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .hero-icon {
    width: 48px;
    height: 48px;
  }

  .hero-icon span {
    width: 26px;
    height: 26px;
    font-size: 16px;
  }

  .hero-title {
    font-size: 22px;
  }

  .hero-subtitle {
    font-size: 13px;
    line-height: 1.45;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .hero-stats {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .hero-stat {
    padding: 8px 10px;
  }

  .hero-art {
    display: none;
  }

  .megaphone {
    display: none;
  }

  .hero-search {
    width: 100%;
  }

  .content-filter {
    gap: 8px;
  }

  .filter-btn,
  .sort-select {
    flex: 1 1 auto;
    min-height: 40px;
    padding: 0 14px;
    font-size: 13px;
  }

  .sort-select {
    width: 100%;
    margin-left: 0;
  }

  .announcement-card {
    grid-template-columns: 1fr;
    gap: 14px;
    min-height: 0;
    padding: 18px;
  }

  .announcement-visual {
    height: 96px;
  }

  .card-arrow {
    position: absolute;
    right: 16px;
    top: 20px;
    font-size: 28px;
  }

  .bulletin-sidebar {
    grid-template-columns: 1fr;
  }

  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .notify-card > span {
    display: none;
  }
}
</style>

