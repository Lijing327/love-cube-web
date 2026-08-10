<template>
  <div class="groups-m">
    <header class="groups-m-head">
      <button type="button" class="back" aria-label="返回" @click="goBack()">‹</button>
      <div>
        <h1>团体广场</h1>
        <p>发现团体 · 申请加入 · 创建属于你的小组</p>
      </div>
      <button type="button" class="create-btn" @click="goCreate">创建</button>
    </header>

    <form class="search" role="search" @submit.prevent="reload">
      <input v-model.trim="keyword" type="search" placeholder="搜索团体名称">
      <button type="submit">搜索</button>
    </form>

    <div class="quick">
      <router-link to="/m/platform/my-groups">我的团体</router-link>
      <router-link :to="groupsPath('season')">赛季榜</router-link>
    </div>

    <p v-if="message" class="flash" :class="{ err: messageType === 'error' }">{{ message }}</p>

    <section v-if="hotGroups.length" class="hot">
      <h2>热门团体</h2>
      <div class="hot-scroll">
        <router-link
          v-for="group in hotGroups"
          :key="`hot-${group.id}`"
          :to="groupsPath(String(group.id))"
          class="hot-chip"
        >
          {{ group.name }}
        </router-link>
      </div>
    </section>

    <p v-if="loading" class="state">加载中…</p>
    <p v-else-if="error" class="state err">{{ error }}</p>

    <article v-for="group in groups" v-else :key="group.id" class="card">
      <router-link :to="groupsPath(String(group.id))" class="cover">
        <img :src="group.coverUrl" :alt="group.name">
      </router-link>
      <div class="body">
        <div class="title-row">
          <h3>{{ group.name }}</h3>
          <span class="cat">{{ group.category }}</span>
        </div>
        <p class="meta">{{ group.region }} · {{ group.memberCount }} 人 · {{ group.joinModeLabel }}</p>
        <p class="desc">{{ group.description }}</p>
        <div class="actions">
          <router-link class="btn ghost" :to="groupsPath(String(group.id))">查看</router-link>
          <button type="button" class="btn primary" :disabled="actionDisabled(group)" @click="onAction(group)">
            {{ actionLabel(group) }}
          </button>
        </div>
      </div>
    </article>

    <p v-if="!loading && !error && !groups.length" class="state">暂无团体，试试创建第一个</p>

    <div v-if="totalPages > 1" class="pager">
      <button type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button type="button" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchGroups, fetchHotGroups, joinGroup, unwrapPlatformGroupList } from '@/api/groups.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import { useBackNavigation } from '@/composables/useBackNavigation.js'
import {
  AUDIT_JOIN_MESSAGE_PROMPT,
  ERR_EMPTY_AUDIT_JOIN_MESSAGE,
  ERR_EMPTY_MEMBER_REAL_NAME,
  JOIN_MEMBER_REAL_NAME_PROMPT
} from '@/utils/groupMemberDisplayName.js'
import { useUserStore } from '@/stores/user.js'
import { labelForGroupCategory } from '@/utils/groupCategories.js'

const PAGE_SIZE = 10
const { goBack } = useBackNavigation('/m/platform')
const DEFAULT_COVER = 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=640&q=80'

const router = useRouter()
const userStore = useUserStore()
const { groupsPath } = usePlatformPath()

const loading = ref(false)
const error = ref('')
const groups = ref([])
const hotGroups = ref([])
const keyword = ref('')
const page = ref(1)
const listTotal = ref(0)
const message = ref('')
const messageType = ref('success')

const totalPages = computed(() => Math.max(1, Math.ceil(listTotal.value / PAGE_SIZE)))

function joinModeLabel(joinKey) {
  if (joinKey === 'open') return '公开加入'
  if (joinKey === 'invite') return '邀请加入'
  return '审核加入'
}

function normalizeGroup(item) {
  const joinKey = item.joinModeKey || (item.joinMode === 'free' ? 'open' : item.joinMode === 'invite' ? 'invite' : 'audit')
  return {
    id: item.id,
    name: item.name || '未命名团体',
    category: labelForGroupCategory(item.category),
    region: item.location || item.region || '未设置地区',
    memberCount: Number(item.memberCount || 0),
    description: item.description || '暂无简介',
    coverUrl: item.coverUrl || DEFAULT_COVER,
    joinModeKey: joinKey,
    joinModeLabel: joinModeLabel(joinKey),
    isMember: Boolean(item.isMember),
    managed: Boolean(item.managed),
    isOwner: Boolean(item.isOwner),
    hasPendingRequest: Boolean(item.hasPendingRequest)
  }
}

function actionLabel(g) {
  if (!userStore.isLoggedIn) return '登录加入'
  if ((g.isOwner || g.managed) && g.isMember) return '管理'
  if (g.isMember) return '已加入'
  if (g.hasPendingRequest) return '审核中'
  if (g.joinModeKey === 'invite') return '仅限邀请'
  if (g.joinModeKey === 'open') return '加入'
  return '申请加入'
}

function actionDisabled(g) {
  if ((g.isOwner || g.managed) && g.isMember) return false
  if (g.joinModeKey === 'invite' && !g.isMember) return true
  if (g.isMember || g.hasPendingRequest) return true
  return false
}

function flash(text, type = 'success') {
  messageType.value = type
  message.value = text
  window.setTimeout(() => { message.value = '' }, 2400)
}

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchGroups({
      keyword: keyword.value || undefined,
      page: page.value,
      pageSize: PAGE_SIZE
    })
    const items = unwrapPlatformGroupList(data)
    groups.value = items.map(normalizeGroup)
    listTotal.value = typeof data?.total === 'number' ? data.total : items.length
  } catch (e) {
    groups.value = []
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadHot() {
  try {
    const data = await fetchHotGroups()
    hotGroups.value = (Array.isArray(data) ? data : []).map(normalizeGroup).slice(0, 8)
  } catch {
    hotGroups.value = []
  }
}

function reload() {
  page.value = 1
  loadList()
}

function goPage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  loadList()
}

function goCreate() {
  const createPath = groupsPath('create')
  if (!userStore.isLoggedIn) {
    userStore.setPostLoginRedirect(createPath)
    router.push('/login')
    return
  }
  router.push(createPath)
}

function onAction(group) {
  if (!userStore.isLoggedIn) {
    userStore.setPostLoginRedirect(groupsPath())
    router.push('/login')
    return
  }
  if (group.isOwner || group.managed) {
    router.push(groupsPath(String(group.id)))
    return
  }
  if (group.joinModeKey === 'invite') return
  applyJoin(group)
}

async function applyJoin(group) {
  const nameInput = window.prompt(JOIN_MEMBER_REAL_NAME_PROMPT, '')
  if (nameInput === null) return
  const memberRealName = String(nameInput).trim()
  if (!memberRealName) {
    flash(ERR_EMPTY_MEMBER_REAL_NAME, 'error')
    return
  }
  let applyMessage = ''
  if (group.joinModeKey === 'audit') {
    const input = window.prompt(AUDIT_JOIN_MESSAGE_PROMPT, '')
    if (input === null) return
    applyMessage = input.trim()
    if (!applyMessage) {
      flash(ERR_EMPTY_AUDIT_JOIN_MESSAGE, 'error')
      return
    }
  }
  try {
    const res = await joinGroup(group.id, { message: applyMessage, memberRealName })
    await Promise.all([loadList(), loadHot()])
    flash(res?.message || (res?.joined ? '加入成功' : '申请已提交'), 'success')
  } catch (e) {
    flash(e.message || '操作失败', 'error')
  }
}

onMounted(async () => {
  await Promise.all([loadList(), loadHot()])
})
</script>

<style scoped>
.groups-m {
  min-height: 100vh;
  padding: 12px 14px 28px;
  background: linear-gradient(180deg, #ecfdf5 0%, #f4f5fb 80px, #f4f5fb 100%);
}

.groups-m-head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 8px;
  align-items: start;
}

.back {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
  text-decoration: none;
  color: var(--lc-indigo, #4f46e5);
  font-size: 22px;
  padding: 0;
  cursor: pointer;
}

.groups-m-head h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.groups-m-head p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--lc-subtle, #64748b);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.create-btn {
  border: none;
  border-radius: 10px;
  padding: 8px 12px;
  background: var(--lc-emerald, #10b981);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.search {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.search input {
  flex: 1;
  border: 1px solid var(--lc-soft, #e2e8f0);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
}

.search button {
  border: none;
  border-radius: 10px;
  padding: 0 14px;
  background: var(--lc-indigo, #4f46e5);
  color: #fff;
  font-weight: 700;
}

.quick {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.quick a,
.quick button {
  flex: 1;
  text-align: center;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid var(--lc-soft, #e2e8f0);
  background: #fff;
  font-size: 13px;
  text-decoration: none;
  color: var(--lc-text, #334155);
}

.flash {
  margin: 10px 0 0;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--lc-emerald, #059669);
}

.flash.err {
  color: #dc2626;
}

.hot {
  margin-top: 14px;
  padding: 12px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
}

.hot h2 {
  margin: 0 0 8px;
  font-size: 14px;
}

.hot-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.hot-chip {
  flex-shrink: 0;
  padding: 8px 12px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  font-size: 12px;
  text-decoration: none;
  border: 1px solid #bbf7d0;
}

.state {
  text-align: center;
  padding: 24px 0;
  color: var(--lc-subtle, #94a3b8);
  font-size: 13px;
}

.state.err {
  color: #dc2626;
}

.card {
  margin-top: 12px;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
}

.cover {
  display: block;
  height: 72px;
  overflow: hidden;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.body {
  padding: 10px 12px 12px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: flex-start;
}

.title-row h3 {
  margin: 0;
  font-size: 16px;
}

.cat {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  white-space: nowrap;
}

.meta,
.desc {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--lc-subtle, #64748b);
  line-height: 1.5;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.btn {
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
  text-decoration: none;
  border: 1px solid transparent;
  cursor: pointer;
}

.btn.ghost {
  background: #fff;
  border-color: var(--lc-soft, #cbd5e1);
  color: var(--lc-text, #334155);
}

.btn.primary {
  background: var(--lc-indigo, #4f46e5);
  color: #fff;
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
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
}
</style>
