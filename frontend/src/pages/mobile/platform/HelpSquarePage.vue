<template>
  <div class="help-m">
    <header class="help-m-head">
      <button type="button" class="help-m-back" @click="goBack()">‹ 玩法</button>
      <h1>互助广场</h1>
      <p>发布需求 · 回应互助 · 达人榜</p>
      <div class="help-m-actions">
        <router-link v-if="userStore.isLoggedIn" :to="helpPath('create')" class="btn primary">发布需求</router-link>
        <router-link v-else to="/login" class="btn primary">登录发布</router-link>
        <router-link :to="helpPath('my')" class="btn ghost">我的互助</router-link>
      </div>
    </header>

    <div class="help-m-filters">
      <button
        v-for="t in typeFilters"
        :key="t.value || 'ALL'"
        type="button"
        :class="['chip', { active: filterType === t.value }]"
        @click="filterType = t.value; load(1)"
      >
        {{ t.label }}
      </button>
    </div>

    <section v-if="leaderboard.length" class="help-m-lb">
      <h2>互助达人榜</h2>
      <p class="sub">信用分 = 回应×1 + 被接受×3 + 成功×10</p>
      <ol>
        <li v-for="row in leaderboard" :key="row.userId">
          <span class="rank">#{{ row.rank }}</span>
          <span class="name">{{ row.nickname }}</span>
          <span class="score">{{ row.creditScore }} 分</span>
        </li>
      </ol>
    </section>

    <p v-if="error" class="banner err">{{ error }}</p>
    <p v-else-if="loading" class="banner">加载中…</p>

    <article v-for="row in items" v-else :key="row.id" class="help-m-card">
      <div class="top">
        <span class="pill">{{ typeLabel(row.helpType) }}</span>
        <span class="status">进行中</span>
      </div>
      <h2>{{ row.title }}</h2>
      <p class="sum">{{ row.summary }}</p>
      <p class="meta">{{ row.publisherName }} · {{ row.region || '地区未填' }} · {{ formatTime(row.createdAt) }}</p>
      <p class="meta">回应 {{ row.replyCount ?? 0 }} 人</p>
      <div class="card-actions">
        <router-link class="btn sm ghost" :to="helpPath(String(row.id))">详情</router-link>
        <router-link
          v-if="canOffer(row)"
          class="btn sm primary"
          :to="`${helpPath(String(row.id))}?offer=1`"
        >我能帮忙</router-link>
      </div>
    </article>

    <p v-if="!loading && !error && !items.length" class="empty">暂无需求，欢迎发布第一条</p>

    <div v-if="totalPages > 1" class="pager">
      <button type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button type="button" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchHelpLeaderboard, fetchHelpRequests } from '@/api/help.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import { useBackNavigation } from '@/composables/useBackNavigation.js'
import { useUserStore } from '@/stores/user.js'

const { helpPath } = usePlatformPath()
const { goBack } = useBackNavigation('/m/platform')
const userStore = useUserStore()
const loading = ref(true)
const error = ref('')
const items = ref([])
const page = ref(1)
const totalPages = ref(1)
const filterType = ref('')
const leaderboard = ref([])

const typeFilters = [
  { value: '', label: '全部' },
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

function typeLabel(code) {
  return TYPE_LABELS[code] || code
}

function formatTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function canOffer(row) {
  if (!userStore.isLoggedIn) return true
  return String(row.userId) !== String(userStore.userId)
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
  } catch (e) {
    error.value = e.message || '加载失败'
    items.value = []
  } finally {
    loading.value = false
  }
}

function goPage(p) {
  if (p < 1 || p > totalPages.value) return
  load(p)
}

onMounted(async () => {
  await load(1)
  try {
    const res = await fetchHelpLeaderboard({ limit: 8 })
    leaderboard.value = Array.isArray(res?.items) ? res.items : []
  } catch {
    leaderboard.value = []
  }
})
</script>

<style scoped>
.help-m {
  min-height: 100vh;
  padding: 12px 14px 28px;
  background: linear-gradient(180deg, #eff6ff 0%, #f4f5fb 80px, #f4f5fb 100%);
}

.help-m-back {
  font-size: 14px;
  color: var(--lc-blue, #2563eb);
  text-decoration: none;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
}

.help-m-head h1 {
  margin: 4px 0 0;
  font-size: 20px;
  font-weight: 800;
}

.help-m-head p {
  margin: 2px 0 8px;
  font-size: 12px;
  color: var(--lc-subtle, #6b7280);
}

.help-m-actions {
  display: flex;
  gap: 8px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  border: 1px solid transparent;
}

.btn.primary {
  background: var(--lc-blue, #2563eb);
  color: #fff;
}

.btn.ghost {
  background: #fff;
  border-color: var(--lc-soft, #cbd5e1);
  color: var(--lc-text, #334155);
}

.help-m-filters {
  display: flex;
  gap: 6px;
  margin-top: 14px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.chip {
  flex-shrink: 0;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid var(--lc-soft, #e2e8f0);
  background: #fff;
  font-size: 12px;
}

.chip.active {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #1d4ed8;
  font-weight: 600;
}

.help-m-lb {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
}

.help-m-lb h2 {
  margin: 0;
  font-size: 14px;
}

.help-m-lb .sub {
  margin: 4px 0 8px;
  font-size: 11px;
  color: var(--lc-subtle, #6b7280);
}

.help-m-lb ol {
  margin: 0;
  padding: 0;
  list-style: none;
}

.help-m-lb li {
  display: flex;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px solid #f1f5f9;
}

.help-m-lb .rank {
  font-weight: 700;
  color: #2563eb;
  min-width: 28px;
}

.help-m-lb .name {
  flex: 1;
}

.banner {
  text-align: center;
  padding: 20px;
  font-size: 13px;
  color: var(--lc-subtle, #6b7280);
}

.banner.err {
  color: #dc2626;
}

.help-m-card {
  margin-top: 12px;
  padding: 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
}

.help-m-card .top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
}

.status {
  font-size: 11px;
  color: #16a34a;
}

.help-m-card h2 {
  margin: 8px 0 4px;
  font-size: 16px;
}

.sum,
.meta {
  margin: 0;
  font-size: 12px;
  color: var(--lc-subtle, #6b7280);
  line-height: 1.5;
}

.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.btn.sm {
  padding: 8px 12px;
  font-size: 12px;
}

.empty {
  text-align: center;
  padding: 24px;
  font-size: 13px;
  color: var(--lc-subtle, #6b7280);
}

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.pager button {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--lc-soft, #cbd5e1);
  background: #fff;
  font-size: 13px;
}

.pager button:disabled {
  opacity: 0.5;
}
</style>
