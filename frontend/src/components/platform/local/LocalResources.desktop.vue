<template>
  <section class="local-page">
    <header class="local-hero platform-card">
      <p class="hero-kicker">Local Discovery</p>
      <h1>本地资源</h1>
      <p>发现附近的人、活动、圈子和实用信息</p>
      <div class="hero-insights">
        <span class="insight-chip">资源总数 {{ items.length }}</span>
        <span class="insight-chip">进行中 {{ filteredItems.length }}</span>
        <span class="insight-chip">高热度 {{ hotItems.length }}</span>
      </div>
      <p class="hero-contribute">
        本地条目由运营审核后展示。你也可以
        <router-link class="hero-contribute-link" to="/platform/publish/clue">提交资源线索</router-link>
        ，通过后会出现在这里。
      </p>
    </header>

    <section class="local-filters platform-card">
      <button
        v-for="item in filters"
        :key="item.value"
        type="button"
        class="local-filter-btn"
        :class="{ active: activeType === item.value }"
        @click="activeType = item.value"
      >
        {{ item.label }}
      </button>
    </section>

    <section class="local-list">
      <article v-for="item in filteredItems" :key="item.id" class="local-card platform-card">
        <div class="local-card-head">
          <h3>{{ item.title }}</h3>
          <span class="local-type-tag">{{ typeLabel(item.type) }}</span>
        </div>
        <p class="local-meta">
          <span>地点：{{ item.location || '待补充' }}</span>
          <span>时间：{{ formatTime(item.eventTime) }}</span>
          <span>热度：{{ item.heat || 0 }} / 感兴趣 {{ item.interestCount || 0 }}</span>
        </p>
        <p class="local-summary">{{ item.summary || '暂无简介' }}</p>
        <div class="local-actions">
          <button
            type="button"
            class="platform-btn platform-btn-primary"
            :disabled="isInterested(item.id)"
            @click="interest(item)"
          >
            {{ isInterested(item.id) ? '已感兴趣' : '感兴趣' }}
          </button>
          <button type="button" class="platform-btn" @click="viewDetail(item)">查看详情</button>
        </div>
      </article>
    </section>

    <section v-if="!loading && !filteredItems.length" class="local-empty platform-card">
      <h3>暂无本地资源</h3>
      <p>欢迎提交线索（需登录），审核通过后会公开显示；也可发布动态或加入团体，获得更多推荐。</p>
      <div class="local-empty-actions">
        <router-link class="platform-btn platform-btn-primary" to="/platform/publish/clue">提交资源线索</router-link>
        <router-link class="platform-btn" to="/platform/positive-share">去发布动态</router-link>
        <router-link class="platform-btn" to="/platform/groups">去加入团体</router-link>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { getLocalResourceDetail, getLocalResources, markLocalResourceInterest } from '@/api/localResources.js'

const filters = [
  { value: 'all', label: '全部' },
  { value: 'job', label: '招聘' },
  { value: 'second_hand', label: '二手' },
  { value: 'rental', label: '租房' },
  { value: 'people', label: '找人' },
  { value: 'activity', label: '找活动' },
  { value: 'group', label: '找圈子' },
  { value: 'resource', label: '实用资源' }
]

const loading = ref(false)
const activeType = ref('all')
const items = ref([])
const interestedIds = ref([])

const filteredItems = computed(() => {
  if (activeType.value === 'all') return items.value
  return items.value.filter(item => item.type === activeType.value)
})
const hotItems = computed(() => items.value.filter(item => Number(item.heat || 0) >= 20))

function typeLabel(type) {
  const map = {
    job: '招聘',
    second_hand: '二手',
    rental: '租房',
    people: '找人',
    activity: '找活动',
    group: '找圈子',
    resource: '实用资源'
  }
  return map[type] || '未分类'
}

function formatTime(time) {
  if (!time) return '待定'
  return String(time).replace('T', ' ').slice(0, 16)
}

function isInterested(id) {
  return interestedIds.value.includes(id)
}

async function load() {
  loading.value = true
  try {
    items.value = await getLocalResources()
  } catch (error) {
    showToast({ type: 'fail', message: error.message || '加载本地资源失败' })
  } finally {
    loading.value = false
  }
}

async function interest(item) {
  if (isInterested(item.id)) return
  try {
    const res = await markLocalResourceInterest(item.id)
    item.interestCount = Number(res?.interestCount ?? item.interestCount ?? 0)
    item.heat = Number(res?.heat ?? item.heat ?? 0)
    interestedIds.value.push(item.id)
    showToast({ type: 'success', message: '已标记感兴趣' })
  } catch (error) {
    showToast({ type: 'fail', message: error.message || '操作失败' })
  }
}

async function viewDetail(item) {
  try {
    const detail = await getLocalResourceDetail(item.id)
    showToast({ message: detail?.summary || '暂无更多详情' })
  } catch (error) {
    showToast({ type: 'fail', message: error.message || '详情加载失败' })
  }
}

onMounted(load)
</script>

<style scoped>
.local-page { width: calc(100% - 48px); margin: 18px auto 36px; display: grid; gap: 14px; }
.local-hero { position: relative; overflow: hidden; background: linear-gradient(118deg, color-mix(in srgb, var(--lc-blue-light) 80%, #fff 20%), var(--lc-surface)); border: 1px solid color-mix(in srgb, var(--lc-blue-border) 78%, #fff 22%); box-shadow: 0 16px 32px -28px color-mix(in srgb, var(--lc-blue) 46%, transparent); }
.local-hero::after { content: ''; position: absolute; right: -72px; top: -86px; width: 240px; height: 240px; border-radius: 50%; background: radial-gradient(circle, color-mix(in srgb, var(--lc-indigo) 28%, transparent), transparent 74%); pointer-events: none; }
.hero-kicker { margin: 0 0 6px; font-size: 12px; font-weight: 700; letter-spacing: 0.1em; text-transform: uppercase; color: var(--lc-indigo); }
.local-hero h1 { margin: 0; font-size: 27px; }
.local-hero p { margin: 8px 0 0; color: var(--lc-muted); }
.hero-insights { margin-top: 12px; display: flex; flex-wrap: wrap; gap: 8px; }
.insight-chip { display: inline-flex; align-items: center; padding: 5px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; color: color-mix(in srgb, var(--lc-text) 72%, var(--lc-blue) 28%); background: color-mix(in srgb, var(--lc-surface) 82%, var(--lc-blue-light) 18%); border: 1px solid color-mix(in srgb, var(--lc-blue-border) 78%, #fff 22%); }
.hero-contribute { margin: 14px 0 0; font-size: 13px; line-height: 1.6; color: var(--lc-muted); max-width: 42em; }
.hero-contribute-link { color: var(--lc-blue); font-weight: 800; text-decoration: underline; text-underline-offset: 3px; }
.hero-contribute-link:hover { color: var(--lc-blue-mid); }
.local-filters { display: flex; flex-wrap: wrap; gap: 8px; box-shadow: 0 10px 24px -24px color-mix(in srgb, var(--lc-text) 60%, transparent); }
.local-filter-btn { border: 1px solid var(--lc-border); background: var(--lc-surface); color: var(--lc-text); border-radius: 999px; padding: 6px 14px; font-weight: 700; cursor: pointer; transition: all 0.2s ease; }
.local-filter-btn.active { border-color: var(--lc-blue-border); background: var(--lc-blue-light); color: var(--lc-blue); }
.local-filter-btn:hover { border-color: var(--lc-blue-border); color: var(--lc-blue); }
.local-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.local-card { border: 1px solid color-mix(in srgb, var(--lc-border) 86%, #fff 14%); box-shadow: 0 14px 28px -24px color-mix(in srgb, var(--lc-text) 40%, transparent); }
.local-card-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; }
.local-card-head h3 { margin: 0; font-size: 18px; }
.local-type-tag { border-radius: 999px; font-size: 12px; padding: 4px 8px; background: var(--lc-indigo-soft); color: var(--lc-indigo); font-weight: 700; }
.local-meta { margin: 10px 0 0; display: flex; flex-wrap: wrap; gap: 12px; color: var(--lc-muted); font-size: 13px; }
.local-summary { margin: 10px 0 0; color: var(--lc-text); line-height: 1.6; min-height: 44px; }
.local-actions { margin-top: 12px; display: flex; gap: 10px; }
.local-empty { text-align: center; }
.local-empty h3 { margin: 0; }
.local-empty p { margin: 8px 0 0; color: var(--lc-muted); }
.local-empty-actions { margin-top: 12px; display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; }
@media (min-width: 768px) and (max-width: 1199px) {
  .local-page { width: calc(100% - 32px); }
}

@media (max-width: 767px) {
  .local-page { width: calc(100% - 24px); margin: 10px auto calc(88px + env(safe-area-inset-bottom)); gap: 10px; }
  .local-hero { padding: 12px 14px; }
  .local-hero h1 { font-size: 20px; margin: 0; }
  .local-hero > p { margin: 4px 0 0; font-size: 12px; line-height: 1.4; }
  .hero-contribute { font-size: 12px; }
  .hero-insights { margin-top: 8px; gap: 6px; }
  .insight-chip { padding: 3px 8px; font-size: 11px; }
  .local-filters { padding: 10px 12px; display: flex; flex-wrap: wrap; gap: 6px; }
  .local-filter-btn { padding: 5px 10px; font-size: 12px; }
  .local-list { grid-template-columns: 1fr; gap: 8px; }
  .local-card { padding: 12px; }
  .local-card-head h3 { font-size: 15px; }
  .local-meta { margin-top: 6px; gap: 8px; font-size: 12px; }
  .local-summary { margin-top: 6px; min-height: 0; font-size: 13px; line-height: 1.45; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  .local-actions { margin-top: 8px; }
  .local-actions .platform-btn { height: 36px; font-size: 13px; flex: 1; }
}
</style>
