<template>
  <section class="my-groups-page">
    <header class="page-head">
      <div>
        <h1>我的团体</h1>
        <p>查看你创建、管理及加入的团体</p>
      </div>
      <div class="head-actions">
        <router-link :to="groupsPath()" class="btn-secondary">团体大厅</router-link>
        <router-link :to="groupsPath('create')" class="btn-primary">创建团体</router-link>
      </div>
    </header>

    <nav class="tabs" aria-label="分类">
      <button type="button" :class="{ active: tab === 'created' }" @click="tab = 'created'">
        我创建的 <span class="count">{{ created.length }}</span>
      </button>
      <button type="button" :class="{ active: tab === 'managed' }" @click="tab = 'managed'">
        我管理的 <span class="count">{{ managed.length }}</span>
      </button>
      <button type="button" :class="{ active: tab === 'joined' }" @click="tab = 'joined'">
        我加入的 <span class="count">{{ joined.length }}</span>
      </button>
      <button type="button" :class="{ active: tab === 'pending' }" @click="tab = 'pending'">
        审核中 <span class="count">{{ pending.length }}</span>
      </button>
    </nav>

    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="error" class="error-card">
      <h2>加载失败</h2>
      <p>{{ error }}</p>
      <button type="button" @click="load">重试</button>
    </div>

    <template v-else>
      <div v-if="currentList.length" class="groups-grid">
        <article v-for="group in currentList" :key="group.joinRequestId || group.id" class="group-card">
          <router-link :to="groupsPath(String(group.id))" class="cover-wrap">
            <img
              :src="group.coverUrl"
              :alt="group.name"
              loading="lazy"
              @error="onCoverError($event)"
            >
          </router-link>
          <div class="group-info">
            <div class="title-line">
              <h3>{{ group.name }}</h3>
              <span class="role-pill">{{ group.roleLabel }}</span>
            </div>
            <p class="meta">{{ group.region }} · {{ group.memberCount }} 人 · {{ group.joinModeLabel }}</p>
            <p class="desc">{{ group.description }}</p>
            <div v-if="memberDisplayRowVisible(group)" class="member-display-row" @click.stop>
              <template v-if="!memberDisplayNameTrimmed(group)">
                <p class="member-display-hint-text">{{ MEMBER_DISPLAY_PANEL_HINT }}</p>
              </template>
              <p v-else class="member-display-name-line">
                本团展示姓名：<strong>{{ group.myMemberRealName }}</strong>
              </p>
              <button
                type="button"
                class="member-display-action"
                :disabled="patchingDisplayGroupId === group.id"
                @click="openMemberDisplayPrompt(group)"
              >
                {{
                  patchingDisplayGroupId === group.id
                    ? '保存中…'
                    : memberDisplayNameTrimmed(group)
                      ? '修改'
                      : '补填姓名'
                }}
              </button>
            </div>
            <div class="card-foot">
              <span class="status-tag">{{ group.statusLabel }}</span>
              <div class="card-foot-actions">
                <router-link
                  v-if="group.managed && manageLink(group)"
                  class="enter-btn enter-btn-manage"
                  :to="manageLink(group)"
                >运营台</router-link>
                <router-link class="enter-btn" :to="groupsPath(String(group.id))">{{ group.enterLabel }}</router-link>
              </div>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="empty-card">
        <h3>{{ emptyTitle }}</h3>
        <p>{{ emptyText }}</p>
        <router-link v-if="tab === 'created'" :to="groupsPath('create')" class="btn-primary inline">创建团体</router-link>
        <router-link v-else-if="tab === 'pending'" :to="groupsPath()" class="btn-primary inline">去发现团体</router-link>
        <router-link v-else :to="groupsPath()" class="btn-primary inline">去发现团体</router-link>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchMeGroupsBuckets,
  patchMyGroupMemberRealName,
  resolveSpaceManageEntry
} from '@/api/groups.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import {
  ERR_EMPTY_DISPLAY_NAME_PATCH,
  MEMBER_DISPLAY_PANEL_HINT,
  supplementMemberDisplayTitle
} from '@/utils/groupMemberDisplayName.js'
import { groupCoverUrlFromApi } from '@/utils/displayFields.js'
import { useUserStore } from '@/stores/user.js'

const DEFAULT_COVER = 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=640&q=80'

const userStore = useUserStore()
const { groupsPath } = usePlatformPath()

const loading = ref(false)
const error = ref('')
const tab = ref('created')

const created = ref([])
const managed = ref([])
const joined = ref([])
const pending = ref([])
const patchingDisplayGroupId = ref(null)

const currentList = computed(() => {
  if (tab.value === 'created') return created.value
  if (tab.value === 'managed') return managed.value
  if (tab.value === 'pending') return pending.value
  return joined.value
})

const emptyTitle = computed(() => {
  if (tab.value === 'created') return '还没有创建的团体'
  if (tab.value === 'managed') return '没有作为管理员参与的团体'
  if (tab.value === 'pending') return '没有待审核的加入申请'
  return '还没有加入的团体'
})

const emptyText = computed(() => {
  if (tab.value === 'created') return '创建第一个团体，邀请伙伴一起成长。'
  if (tab.value === 'managed') return '由团长将你设为管理员后，会显示在这里。'
  if (tab.value === 'pending') return '在团体大厅申请加入需要审核的团体后，会显示在这里。'
  return '去团体大厅搜索感兴趣的团体并加入。'
})

function manageLink(group) {
  if (!group?.managed || group.id == null || group.id === '') return null
  const entry = resolveSpaceManageEntry(group.id)
  if (!entry) return null
  // 非站点管理员但为团长：仍可进后台「我的团体」运营页
  if (entry.path?.startsWith('/admin/my-groups') && !userStore.isAdmin && !userStore.isGroupStewardOnly) {
    return groupsPath(String(group.id))
  }
  return entry
}

function onCoverError(event) {
  const img = event?.target
  if (!img || img.dataset.fallbackApplied === '1') return
  img.dataset.fallbackApplied = '1'
  img.src = DEFAULT_COVER
}

function mapGroupStatusLabel(status, hasPendingRequest) {
  if (hasPendingRequest) return '审核中'
  const s = String(status || '').trim().toLowerCase()
  if (s === 'published' || s === 'active') return '进行中'
  return status || '进行中'
}

function normalize(item, tabKey) {
  const joinKey = item.joinModeKey || (item.joinMode === 'free' ? 'open' : item.joinMode === 'invite' ? 'invite' : 'audit')
  const joinModeLabel = joinKey === 'open' ? '公开加入' : joinKey === 'invite' ? '仅限邀请' : '审核加入'
  let roleLabel = '成员'
  if (tabKey === 'created') roleLabel = '团长'
  else if (tabKey === 'managed') roleLabel = '管理员'
  else if (tabKey === 'pending') roleLabel = '审核中'
  else if (item.myRole === 'owner') roleLabel = '团长'
  else if (item.hasPendingRequest) roleLabel = '审核中'

  const statusLabel = mapGroupStatusLabel(item.status, item.hasPendingRequest)
  const enterLabel = item.applicationPending ? '查看' : item.managed ? '查看主页' : '进入'

  return {
    id: item.id,
    name: item.name || '未命名团体',
    category: item.category || item.typeName || item.type || '团体',
    region: item.location || item.region || '未设置地区',
    memberCount: Number(item.memberCount || 0),
    description: item.description || '暂无团体简介',
    coverUrl: groupCoverUrlFromApi(item, DEFAULT_COVER),
    joinModeLabel,
    roleLabel,
    statusLabel,
    enterLabel,
    managed: Boolean(item.managed),
    hasPendingRequest: Boolean(item.hasPendingRequest),
    applicationPending: Boolean(item.applicationPending),
    joinRequestId: item.joinRequestId != null ? item.joinRequestId : null,
    myMemberRealName: item.myMemberRealName != null ? String(item.myMemberRealName) : ''
  }
}

function memberDisplayNameTrimmed(g) {
  return String(g.myMemberRealName ?? '').trim()
}

/** 已通过且可展示补填：分桶内待审核不展示 */
function memberDisplayRowVisible(group) {
  return userStore.isLoggedIn && !group.hasPendingRequest
}

async function openMemberDisplayPrompt(group) {
  if (!memberDisplayRowVisible(group) || patchingDisplayGroupId.value === group.id) return
  const hasName = Boolean(memberDisplayNameTrimmed(group))
  const title = supplementMemberDisplayTitle(hasName)
  const def = memberDisplayNameTrimmed(group)
  const input = window.prompt(title, def)
  if (input === null) return
  const memberRealName = String(input).trim()
  if (!memberRealName) {
    window.alert(ERR_EMPTY_DISPLAY_NAME_PATCH)
    return
  }
  patchingDisplayGroupId.value = group.id
  try {
    await patchMyGroupMemberRealName(group.id, memberRealName)
    await load()
  } catch (err) {
    window.alert(err?.message || '保存失败')
  } finally {
    patchingDisplayGroupId.value = null
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (userStore.token) {
      await userStore.loadAdminContext().catch(() => null)
    }
    const data = await fetchMeGroupsBuckets()
    const raw = data?.data ?? data
    created.value = (raw?.createdGroups ?? []).map((g) => normalize(g, 'created'))
    managed.value = (raw?.managedGroups ?? []).map((g) => normalize(g, 'managed'))
    joined.value = (raw?.joinedGroups ?? []).map((g) => normalize(g, 'joined'))
    pending.value = (raw?.pendingJoinGroups ?? []).map((g) => normalize(g, 'pending'))

    if (created.value.length) tab.value = 'created'
    else if (joined.value.length) tab.value = 'joined'
    else if (managed.value.length) tab.value = 'managed'
    else if (pending.value.length) tab.value = 'pending'
    else tab.value = 'joined'
  } catch (err) {
    error.value = err.message || '无法加载我的团体'
    created.value = []
    managed.value = []
    joined.value = []
    pending.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.my-groups-page {
  width: calc(100% - var(--lc-space-8));
  margin: var(--lc-space-4) auto 0;
  padding: var(--lc-space-8);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius);
  background: var(--lc-surface);
  box-shadow: var(--lc-shadow-sm);
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--lc-space-4);
  flex-wrap: wrap;
}

.page-head h1 {
  margin: 0;
  color: var(--lc-text);
  font-size: 30px;
}

.page-head p {
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  font-weight: 700;
}

.head-actions {
  display: flex;
  gap: var(--lc-space-3);
}

.btn-secondary,
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 var(--lc-space-4);
  border-radius: var(--lc-radius-xs);
  font-weight: 900;
  text-decoration: none;
}

.btn-secondary {
  border: 1px solid var(--lc-border);
  color: var(--lc-blue);
  background: var(--lc-surface);
}

.btn-primary {
  border: 0;
  color: var(--lc-surface);
  background: linear-gradient(135deg, var(--lc-pink), var(--lc-blue));
  box-shadow: var(--lc-shadow-blue);
}

.btn-primary.inline {
  margin-top: var(--lc-space-4);
}

.tabs {
  display: flex;
  gap: var(--lc-space-2);
  margin-top: var(--lc-space-6);
  border-bottom: 1px solid var(--lc-border);
  flex-wrap: wrap;
}

.tabs button {
  position: relative;
  padding: var(--lc-space-3) var(--lc-space-4);
  border: 0;
  background: transparent;
  color: var(--lc-muted);
  font-size: var(--lc-text-base);
  font-weight: 900;
  cursor: pointer;
}

.tabs button.active {
  color: var(--lc-blue);
}

.tabs button.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 3px;
  border-radius: 999px;
  background: var(--lc-blue);
}

.count {
  display: inline-block;
  min-width: 22px;
  padding: 0 6px;
  border-radius: 999px;
  margin-left: 4px;
  color: var(--lc-blue);
  background: var(--lc-blue-light);
  font-size: var(--lc-text-xs);
}

.groups-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--lc-space-4);
  margin-top: var(--lc-space-6);
}

.group-card {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  gap: var(--lc-space-4);
  padding: var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-sm);
  background: var(--lc-surface);
  box-shadow: 0 7px 18px rgba(15, 23, 42, 0.04);
}

.cover-wrap {
  display: block;
  border-radius: var(--lc-radius-xs);
  overflow: hidden;
}

.cover-wrap img {
  width: 132px;
  height: 116px;
  object-fit: cover;
}

.group-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.title-line {
  display: flex;
  align-items: center;
  gap: var(--lc-space-2);
  flex-wrap: wrap;
}

.title-line h3 {
  margin: 0;
  color: var(--lc-text);
  font-size: var(--lc-text-lg);
}

.role-pill {
  padding: 3px var(--lc-space-2);
  border-radius: 999px;
  color: var(--lc-blue);
  background: var(--lc-blue-light);
  font-size: var(--lc-text-xs);
  font-weight: 900;
}

.meta,
.desc {
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
}

.desc {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.member-display-row {
  margin-top: var(--lc-space-2);
  padding: var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  border: 1px dashed var(--lc-blue-border);
  background: var(--lc-blue-light);
  display: flex;
  flex-direction: column;
  gap: var(--lc-space-2);
}

.member-display-hint-text,
.member-display-name-line {
  margin: 0;
  font-size: var(--lc-text-xs);
  font-weight: 700;
  color: var(--lc-muted);
  line-height: 1.45;
}

.member-display-name-line {
  font-weight: 600;
}

.member-display-action {
  align-self: flex-start;
  min-width: 96px;
  height: 32px;
  padding: 0 var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  border: 1px solid var(--lc-blue-border);
  color: var(--lc-blue);
  background: var(--lc-surface);
  font-weight: 900;
  font-size: var(--lc-text-xs);
  cursor: pointer;
}

.member-display-action:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-3);
  margin-top: auto;
  padding-top: var(--lc-space-3);
  flex-wrap: wrap;
}

.card-foot-actions {
  display: flex;
  align-items: center;
  gap: var(--lc-space-2);
  flex-wrap: wrap;
}

.status-tag {
  font-size: var(--lc-text-xs);
  font-weight: 900;
  color: var(--lc-muted);
}

.enter-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  height: 32px;
  padding: 0 var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  border: 1px solid var(--lc-blue-border);
  color: var(--lc-blue);
  font-weight: 900;
  font-size: var(--lc-text-sm);
  text-decoration: none;
}

.enter-btn-manage {
  border-color: var(--lc-pink);
  color: var(--lc-pink);
}

.loading-state,
.empty-card,
.error-card {
  min-height: 200px;
  margin-top: var(--lc-space-6);
  display: grid;
  place-items: center;
  text-align: center;
}

.empty-card {
  border: 1px dashed var(--lc-border);
  border-radius: var(--lc-radius-sm);
  padding: var(--lc-space-8);
  color: var(--lc-muted);
}

.error-card {
  color: var(--lc-red);
  background: var(--lc-red-light);
  border-radius: var(--lc-radius-sm);
}

.error-card button {
  margin-top: var(--lc-space-3);
  padding: var(--lc-space-2) var(--lc-space-4);
  border-radius: var(--lc-radius-xs);
  border: 1px solid var(--lc-red);
  background: var(--lc-surface);
  color: var(--lc-red);
  font-weight: 900;
  cursor: pointer;
}

@media (max-width: 640px) {
  .my-groups-page {
    width: calc(100% - 16px);
    margin-top: 8px;
    padding: 12px 12px calc(80px + env(safe-area-inset-bottom, 0px));
  }

  .page-head h1 {
    font-size: 22px;
  }

  .head-actions {
    width: 100%;
  }

  .head-actions .btn-secondary,
  .head-actions .btn-primary {
    flex: 1;
    height: 40px;
    font-size: 13px;
  }

  .tabs button {
    padding: 10px 12px;
    font-size: 13px;
  }

  .group-card {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .cover-wrap img {
    width: 100%;
    height: 140px;
  }

  .card-foot {
    flex-direction: column;
    align-items: stretch;
  }

  .card-foot-actions {
    width: 100%;
  }

  .enter-btn {
    flex: 1;
    min-height: 36px;
  }
}

@media (max-width: 920px) {
  .my-groups-page {
    width: calc(100% - var(--lc-space-4));
    padding: var(--lc-space-4);
  }

  .groups-grid {
    grid-template-columns: 1fr;
  }
}
</style>
