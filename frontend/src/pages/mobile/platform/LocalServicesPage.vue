<template>
  <div class="local-m">
    <header class="local-m-head">
      <button type="button" class="back" aria-label="返回" @click="goBack()">‹</button>
      <div>
        <h1>本地服务</h1>
        <p>招聘 · 二手 · 租房 · 同城资源</p>
      </div>
    </header>

    <div class="filter-scroll">
      <button
        v-for="item in filters"
        :key="item.value"
        type="button"
        class="filter-chip"
        :class="{ active: activeType === item.value }"
        @click="activeType = item.value"
      >
        {{ item.label }}
      </button>
    </div>

    <div v-if="loading" class="loading">加载中…</div>

    <article v-for="item in filteredItems" :key="item.id" class="card">
      <div class="card-top">
        <h3>{{ item.title }}</h3>
        <span class="tag">{{ typeLabel(item.type) }}</span>
      </div>
      <p class="meta">{{ item.location || '地点待定' }} · 热度 {{ item.heat || 0 }}</p>
      <p class="summary">{{ item.summary || '暂无简介' }}</p>
      <div class="actions">
        <button type="button" class="btn primary" :disabled="isInterested(item.id)" @click="interest(item)">
          {{ isInterested(item.id) ? '已感兴趣' : '感兴趣' }}
        </button>
      </div>
    </article>

    <div v-if="!loading && !filteredItems.length" class="empty">
      <p>暂无该类资源</p>
      <router-link to="/platform/publish/clue" class="btn primary">提交线索</router-link>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { getLocalResources, markLocalResourceInterest } from '@/api/localResources.js'
import { useBackNavigation } from '@/composables/useBackNavigation.js'

const { goBack } = useBackNavigation('/m/platform')
const loading = ref(false)
const activeType = ref('all')
const items = ref([])
const interestedIds = ref([])

const filters = [
  { value: 'all', label: '全部' },
  { value: 'job', label: '招聘' },
  { value: 'second_hand', label: '二手' },
  { value: 'rental', label: '租房' },
  { value: 'people', label: '找人' },
  { value: 'activity', label: '活动' },
  { value: 'resource', label: '资源' }
]

const filteredItems = computed(() => {
  if (activeType.value === 'all') return items.value
  return items.value.filter((item) => item.type === activeType.value)
})

function typeLabel(type) {
  const map = {
    job: '招聘',
    second_hand: '二手',
    rental: '租房',
    people: '找人',
    activity: '活动',
    group: '圈子',
    resource: '资源'
  }
  return map[type] || '其他'
}

function isInterested(id) {
  return interestedIds.value.includes(id)
}

async function load() {
  loading.value = true
  try {
    items.value = await getLocalResources()
  } catch (e) {
    showToast({ type: 'fail', message: e.message || '加载失败' })
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
  } catch (e) {
    showToast({ type: 'fail', message: e.message || '操作失败' })
  }
}

onMounted(load)
</script>

<style scoped>
.local-m {
  min-height: 100vh;
  padding: 10px 14px calc(88px + env(safe-area-inset-bottom, 0px));
  background: #f4f5fb;
}

.local-m-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.back {
  border: none;
  background: #fff;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  font-size: 22px;
  line-height: 1;
  color: #334155;
  padding: 0;
  cursor: pointer;
}

.local-m-head h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
}

.local-m-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
}

.filter-scroll {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 2px;
  margin-bottom: 8px;
  -webkit-overflow-scrolling: touch;
}

.filter-chip {
  flex: 0 0 auto;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  color: #475569;
}

.filter-chip.active {
  border-color: #6366f1;
  background: #eef2ff;
  color: #4338ca;
  font-weight: 700;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid #e8ecf4;
}

.card-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: flex-start;
}

.card-top h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.tag {
  flex: 0 0 auto;
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #fff7ed;
  color: #c2410c;
  font-weight: 700;
}

.meta {
  margin: 4px 0 0;
  font-size: 11px;
  color: #94a3b8;
}

.summary {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.actions {
  margin-top: 8px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
}

.btn.primary {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
}

.btn:disabled {
  opacity: 0.6;
}

.loading, .empty {
  text-align: center;
  padding: 32px 0;
  color: #94a3b8;
  font-size: 13px;
}

.empty .btn {
  margin-top: 12px;
}
</style>
