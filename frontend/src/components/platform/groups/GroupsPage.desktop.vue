<template>
  <section class="groups-page">
    <div class="groups-panel desktop-shell operation-shell">
      <header class="group-list-hero operation-hero">
        <div class="hero-text">
          <p class="section-kicker">Space Workbench</p>
          <h1>我的 Space / 发现 Space</h1>
          <p>用于加入、管理、参与团体，快速回到你正在协作的 Space。</p>
        </div>
        <div class="hero-actions">
          <button type="button" class="platform-btn platform-btn-primary" @click="goCreate">创建 Space</button>
          <router-link :to="myGroupsPath()" class="platform-btn platform-btn-ghost">我的团体</router-link>
          <router-link :to="groupsPath('join')" class="platform-btn platform-btn-ghost">我有邀请码</router-link>
          <router-link :to="groupsPath('season')" class="platform-btn platform-btn-ghost">赛季榜</router-link>
        </div>
        <div class="metric-grid compact group-list-metrics">
          <article v-for="item in listStats" :key="item.label" class="metric-card">
            <span class="metric-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.hint }}</p>
          </article>
        </div>
      </header>

      <div class="toolbar-row section-card">
        <form class="search-bar" role="search" @submit.prevent="onSearch">
          <span aria-hidden="true">⌕</span>
          <input v-model.trim="searchKeyword" type="search" placeholder="搜索团体名称">
          <button type="submit">搜索</button>
        </form>
        <div class="selects">
          <select v-model="category" aria-label="分类" @change="reloadFirstPage">
            <option value="">全部分类</option>
            <option v-for="item in categories" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <select v-model="joinMode" aria-label="加入方式" @change="reloadFirstPage">
            <option value="">全部方式</option>
            <option value="open">公开加入</option>
            <option value="audit">审核加入</option>
            <option value="invite">邀请加入</option>
          </select>
          <select v-model="sort" aria-label="排序" @change="reloadFirstPage">
            <option value="newest">最新创建</option>
            <option value="members">成员最多</option>
          </select>
          <button type="button" class="reset-filter-btn" @click="resetFilters">重置</button>
        </div>
      </div>

      <div v-if="message" class="page-message" :class="{ error: messageType === 'error' }">{{ message }}</div>

      <div class="groups-body">
        <main class="groups-main">
          <div v-if="loading" class="loading-state">加载中...</div>
          <template v-else>
            <div v-if="errors.list" class="error-card">
              <h3>团体列表加载失败</h3>
              <p>{{ errors.list }}</p>
              <button type="button" @click="loadGroups">重试</button>
            </div>
            <div v-else-if="groupItems.length" class="groups-grid">
              <article v-for="group in groupItems" :key="group.id" class="group-card section-card">
                <router-link :to="groupsPath(String(group.id))" class="card-cover">
                  <img :src="group.coverUrl" :alt="group.name">
                </router-link>
                <div class="group-info">
                  <div class="title-line">
                    <h2>{{ group.name }}</h2>
                    <span class="status-badge neutral">{{ group.category }}</span>
                    <span class="status-badge" :class="statusBadgeClass(group)">{{ statusLabel(group) }}</span>
                  </div>
                  <p class="desc">{{ group.description }}</p>
                  <div class="space-card-metrics" aria-label="Space 状态">
                    <span><strong>{{ group.memberCount }}</strong> 成员</span>
                    <span>{{ activityLabel(group) }}</span>
                    <span>{{ group.joinModeLabel }}</span>
                  </div>
                  <div class="tags" v-if="group.tags">
                    <span v-for="t in group.tagList" :key="t" class="tag">{{ t }}</span>
                  </div>
                  <div class="card-actions">
                    <router-link class="platform-btn platform-btn-ghost" :to="groupsPath(String(group.id))">进入</router-link>
                    <button type="button" class="platform-btn platform-btn-primary action-btn" :disabled="actionDisabled(group)" @click="onAction(group)">
                      {{ actionLabel(group) }}
                    </button>
                  </div>
                  <div v-if="memberDisplayRowVisible(group)" class="member-display-compact" @click.stop>
                    <template v-if="!memberDisplayNameTrimmed(group)">
                      <p class="member-display-compact-hint">{{ MEMBER_DISPLAY_PANEL_HINT }}</p>
                    </template>
                    <div v-else class="member-display-compact-name">
                      <span>本团展示姓名：<strong>{{ group.myMemberRealName }}</strong></span>
                    </div>
                    <button
                      type="button"
                      class="member-display-compact-btn"
                      :disabled="patchingDisplayGroupId === group.id"
                      @click="openMemberDisplayPromptForList(group)"
                    >
                      {{ patchingDisplayGroupId === group.id ? '保存中…' : (memberDisplayNameTrimmed(group) ? '修改' : '补填姓名') }}
                    </button>
                  </div>
                </div>
              </article>
            </div>
            <div v-else class="empty-card">
              <h3>暂时没有找到合适的团体</h3>
              <p>可以尝试更换筛选条件，或创建一个新团体。</p>
              <button type="button" class="primary-btn small" @click="goCreate">创建团体</button>
            </div>
          </template>

          <div v-if="totalPages > 1" class="pagination">
            <button type="button" :disabled="page === 1" @click="goPage(-1)">←</button>
            <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
            <button type="button" :disabled="page === totalPages" @click="goPage(1)">→</button>
          </div>
        </main>

        <aside class="groups-aside">
          <section class="aside-card">
            <div class="aside-title"><h2>热门团体</h2></div>
            <div v-if="sideLoading.hot" class="side-state">加载中...</div>
            <div v-else-if="errors.hot" class="side-error">{{ errors.hot }}</div>
            <div v-else class="compact-list">
              <router-link v-for="group in popularGroups" :key="group.id" :to="groupsPath(String(group.id))" class="compact-item">
                <img :src="group.coverUrl" :alt="group.name">
                <div>
                  <strong>{{ group.name }}</strong>
                  <span>{{ group.region }} · {{ group.memberCount }} 人<template v-if="group.postCount"> · {{ group.postCount }} 动态</template></span>
                </div>
              </router-link>
              <div v-if="!popularGroups.length" class="side-state">暂无</div>
            </div>
          </section>

          <section class="aside-card">
            <div class="aside-title"><h2>团体动态</h2></div>
            <div v-if="sideLoading.feed" class="side-state">加载中...</div>
            <div v-else-if="errors.feed" class="side-error">{{ errors.feed }}</div>
            <div v-else class="activity-list">
              <router-link v-for="item in activities" :key="item.id" :to="groupsPath(String(item.groupId), 'posts')" class="activity-item">
                <img :src="item.coverUrl" :alt="item.groupName">
                <div>
                  <strong>{{ item.groupName }}</strong>
                  <p>{{ item.text }}</p>
                </div>
                <time>{{ item.time }}</time>
              </router-link>
              <div v-if="!activities.length" class="side-state">暂无动态</div>
            </div>
          </section>
        </aside>
      </div>
    </div>

    <div class="mobile-shell">
      <header class="m-hero">
        <div>
          <h1>团体广场</h1>
          <p>找到你的团体，或创建属于你的团体</p>
        </div>
        <button type="button" class="m-create" @click="goCreate">创建团体</button>
      </header>

      <form class="m-search" role="search" @submit.prevent="onSearch">
        <span aria-hidden="true">⌕</span>
        <input v-model.trim="searchKeyword" type="search" placeholder="搜索团体名称、地区、标签">
      </form>

      <section class="m-quick-grid" aria-label="快捷入口">
        <router-link class="m-quick-item" :to="myGroupsPath()">我的团体</router-link>
        <router-link class="m-quick-item" :to="groupsPath('season')">赛季榜</router-link>
        <router-link class="m-quick-item" :to="myGroupsPath()">团体管理</router-link>
      </section>

      <div v-if="message" class="page-message" :class="{ error: messageType === 'error' }">{{ message }}</div>

      <main class="m-list-wrap">
        <div v-if="loading" class="loading-state">加载中...</div>
        <template v-else>
          <div v-if="errors.list" class="error-card">
            <h3>团体列表加载失败</h3>
            <p>{{ errors.list }}</p>
            <button type="button" @click="loadGroups">重试</button>
          </div>

          <section v-else-if="groupItems.length" class="m-group-list">
            <article v-for="group in groupItems" :key="group.id" class="m-group-card">
              <router-link :to="groupsPath(String(group.id))" class="m-cover">
                <img :src="group.coverUrl" :alt="group.name">
              </router-link>

              <div class="m-main">
                <div class="m-title-row">
                  <h2>{{ group.name }}</h2>
                  <span class="m-cat">{{ group.category }}</span>
                </div>
                <p class="m-meta">
                  {{ group.region }} · {{ group.memberCount }}人
                  <template v-if="group.postCount"> · {{ group.postCount }}条动态</template>
                </p>
                <p class="m-desc">{{ group.description }}</p>

                <div class="m-statuses">
                  <span class="m-status">{{ group.joinModeLabel }}</span>
                  <span v-if="group.isOwner || group.managed" class="m-status subtle">管理员</span>
                  <span v-if="group.isMember" class="m-status subtle">已加入</span>
                  <span v-if="group.hasPendingRequest" class="m-status subtle">审核中</span>
                </div>

                <div v-if="group.tags" class="m-tags">
                  <span v-for="t in group.tagList.slice(0, 3)" :key="t" class="m-tag">{{ t }}</span>
                </div>

                <div v-if="memberDisplayRowVisible(group)" class="m-member-display" @click.stop>
                  <template v-if="!memberDisplayNameTrimmed(group)">
                    <p class="m-member-display-hint">{{ MEMBER_DISPLAY_PANEL_HINT }}</p>
                  </template>
                  <div v-else class="m-member-display-name">
                    <span>本团：<strong>{{ group.myMemberRealName }}</strong></span>
                  </div>
                  <button
                    type="button"
                    class="m-member-display-btn"
                    :disabled="patchingDisplayGroupId === group.id"
                    @click="openMemberDisplayPromptForList(group)"
                  >
                    {{ patchingDisplayGroupId === group.id ? '保存中…' : (memberDisplayNameTrimmed(group) ? '修改' : '补填') }}
                  </button>
                </div>

                <div class="m-actions">
                  <router-link class="m-btn ghost" :to="groupsPath(String(group.id))">查看团体</router-link>
                  <button type="button" class="m-btn" :disabled="actionDisabled(group)" @click="onAction(group)">
                    {{ mobileActionLabel(group) }}
                  </button>
                </div>
              </div>
            </article>
          </section>

          <section v-else class="m-empty">
            <h3>暂无团体，你可以创建第一个团体</h3>
            <button type="button" class="m-btn" @click="goCreate">创建团体</button>
          </section>
        </template>

        <div v-if="totalPages > 1" class="pagination">
          <button type="button" :disabled="page === 1" @click="goPage(-1)">←</button>
          <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
          <button type="button" :disabled="page === totalPages" @click="goPage(1)">→</button>
        </div>
      </main>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchGroupFeed,
  fetchGroups,
  fetchHotGroups,
  joinGroup,
  patchMyGroupMemberRealName,
  unwrapPlatformGroupList
} from '@/api/groups.js'
import {
  AUDIT_JOIN_MESSAGE_PROMPT,
  ERR_EMPTY_AUDIT_JOIN_MESSAGE,
  ERR_EMPTY_DISPLAY_NAME_PATCH,
  ERR_EMPTY_MEMBER_REAL_NAME,
  JOIN_MEMBER_REAL_NAME_PROMPT,
  MEMBER_DISPLAY_PANEL_HINT,
  supplementMemberDisplayTitle
} from '@/utils/groupMemberDisplayName.js'
import { useUserStore } from '@/stores/user.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import { PLATFORM_GROUP_CATEGORY_OPTIONS, labelForGroupCategory } from '@/utils/groupCategories.js'

const PAGE_SIZE = 8
const DEFAULT_COVER = 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=640&q=80'

const router = useRouter()
const userStore = useUserStore()
const { groupsPath, myGroupsPath } = usePlatformPath()

const loading = ref(false)
const listTotal = ref(0)
const remoteGroups = ref([])
const hotGroups = ref([])
const groupFeed = ref([])

const searchKeyword = ref('')
const category = ref('')
const joinMode = ref('')
const sort = ref('newest')
const page = ref(1)

const message = ref('')
const messageType = ref('success')
const errors = reactive({ list: '', hot: '', feed: '' })
const sideLoading = reactive({ hot: false, feed: false })
const patchingDisplayGroupId = ref(null)

const categories = [...PLATFORM_GROUP_CATEGORY_OPTIONS]

const groupItems = computed(() => remoteGroups.value)
const totalPages = computed(() => Math.max(1, Math.ceil(listTotal.value / PAGE_SIZE)))
const activities = computed(() => groupFeed.value)
const popularGroups = computed(() => hotGroups.value)
const listStats = computed(() => {
  const rows = groupItems.value
  const joined = rows.filter((g) => g.isMember).length
  const pending = rows.filter((g) => g.hasPendingRequest).length
  const joinable = rows.filter((g) => !g.isMember && !g.hasPendingRequest && g.joinModeKey !== 'invite').length
  const active = rows.filter((g) => g.lastActiveLabel || g.postCount > 0).length
  return [
    { label: '我加入的', value: joined, hint: '当前列表内已加入' },
    { label: '可加入的', value: joinable, hint: '可直接申请或加入' },
    { label: '待审核的', value: pending, hint: '申请处理中' },
    { label: '活跃 Space', value: active, hint: '近期有动态' }
  ]
})

function joinModeLabel(g) {
  const k = g.joinModeKey || (g.joinMode === 'free' ? 'open' : g.joinMode === 'invite' ? 'invite' : 'audit')
  if (k === 'open') return '公开加入'
  if (k === 'invite') return '邀请加入'
  return '审核加入'
}

function formatLastActive(raw) {
  if (raw == null || raw === '') return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return ''
  const diff = Date.now() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 86400000 * 7) return `${Math.floor(diff / 86400000)} 天前`
  return d.toISOString().slice(0, 10)
}

function normalizeCategory(item) {
  const raw = (item.category || item.typeName || item.type || '').toString().trim()
  return raw ? labelForGroupCategory(raw) : '团体'
}

function normalizeGroup(item) {
  const joinKey = item.joinModeKey || (item.joinMode === 'free' ? 'open' : item.joinMode === 'invite' ? 'invite' : 'audit')
  const tagStr = item.tags || ''
  const tagList = tagStr
    ? tagStr.split(/[,，]/).map((s) => s.trim()).filter(Boolean).slice(0, 6)
    : []
  return {
    id: item.id,
    name: item.name || '未命名团体',
    category: normalizeCategory(item),
    region: item.location || item.region || '未设置地区',
    memberCount: Number(item.memberCount || 0),
    description: item.description || '暂无团体简介',
    coverUrl: item.coverUrl || DEFAULT_COVER,
    joinMode: item.joinMode,
    joinModeKey: joinKey,
    joinModeLabel: joinModeLabel({ joinModeKey: joinKey, joinMode: item.joinMode }),
    ownerName: item.ownerName || '',
    tags: tagStr,
    tagList,
    isMember: Boolean(item.isMember),
    managed: Boolean(item.managed),
    isOwner: Boolean(item.isOwner),
    hasPendingRequest: Boolean(item.hasPendingRequest),
    postCount: Number(item.postCount || 0),
    lastActiveLabel: formatLastActive(item.lastActiveAt),
    myMemberRealName: item.myMemberRealName != null ? String(item.myMemberRealName) : ''
  }
}

function actionLabel(g) {
  if (!userStore.isLoggedIn && needsInteraction(g)) return '登录后加入'
  if ((g.isOwner || g.managed) && g.isMember) return '管理'
  if (g.isMember) return '已加入'
  if (g.hasPendingRequest) return '审核中'
  if (g.joinModeKey === 'invite') return '仅限邀请'
  if (g.joinModeKey === 'open') return '加入团体'
  return '申请加入'
}

function mobileActionLabel(g) {
  if ((g.isOwner || g.managed) && g.isMember) return '管理'
  if (g.isMember) return '已加入'
  if (g.hasPendingRequest) return '审核中'
  if (g.joinModeKey === 'invite') return '仅限邀请'
  return '申请加入'
}

function needsInteraction(g) {
  if (g.isMember || g.hasPendingRequest) return false
  return true
}

function actionDisabled(g) {
  if ((g.isOwner || g.managed) && g.isMember) return false
  if (g.joinModeKey === 'invite' && !g.isMember) return true
  if (g.isMember || g.hasPendingRequest) return true
  return false
}

function statusLabel(g) {
  if (g.isOwner) return 'Owner'
  if (g.managed) return '管理员'
  if (g.isMember) return '已加入'
  if (g.hasPendingRequest) return '待审核'
  if (g.joinModeKey === 'invite') return '邀请加入'
  return '可申请'
}

function statusBadgeClass(g) {
  if (g.isMember) return 'success'
  if (g.hasPendingRequest) return 'warning'
  if (g.isOwner || g.managed) return 'info'
  if (g.joinModeKey === 'invite') return 'neutral'
  return 'info'
}

function activityLabel(g) {
  if (g.lastActiveLabel) return `活跃 ${g.lastActiveLabel}`
  if (g.postCount) return `${g.postCount} 条动态`
  return '暂无动态'
}

function memberDisplayNameTrimmed(g) {
  return String(g.myMemberRealName ?? '').trim()
}

function memberDisplayRowVisible(g) {
  return userStore.isLoggedIn && g.isMember && !g.hasPendingRequest
}

async function openMemberDisplayPromptForList(group) {
  if (!memberDisplayRowVisible(group) || patchingDisplayGroupId.value === group.id) return
  const hasName = Boolean(memberDisplayNameTrimmed(group))
  const title = supplementMemberDisplayTitle(hasName)
  const def = memberDisplayNameTrimmed(group)
  const input = window.prompt(title, def)
  if (input === null) return
  const memberRealName = String(input).trim()
  if (!memberRealName) {
    flashMessage(ERR_EMPTY_DISPLAY_NAME_PATCH, 'error')
    return
  }
  patchingDisplayGroupId.value = group.id
  try {
    await patchMyGroupMemberRealName(group.id, memberRealName)
    await Promise.all([loadGroups({ resetPage: false }), loadHotGroups()])
    flashMessage('已保存', 'success')
  } catch (error) {
    flashMessage(error.message || '保存失败', 'error')
  } finally {
    patchingDisplayGroupId.value = null
  }
}

function goCreate() {
  if (!userStore.isLoggedIn) {
    userStore.setPostLoginRedirect(groupsPath('create'))
    router.push('/login')
    return
  }
  router.push(groupsPath('create'))
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
  message.value = ''
  const nameInput = window.prompt(JOIN_MEMBER_REAL_NAME_PROMPT, '')
  if (nameInput === null) return
  const memberRealName = String(nameInput).trim()
  if (!memberRealName) {
    flashMessage(ERR_EMPTY_MEMBER_REAL_NAME, 'error')
    return
  }
  let applyMessage = ''
  if (group.joinModeKey === 'audit') {
    const input = window.prompt(AUDIT_JOIN_MESSAGE_PROMPT, '')
    if (input === null) return
    applyMessage = input.trim()
    if (!applyMessage) {
      flashMessage(ERR_EMPTY_AUDIT_JOIN_MESSAGE, 'error')
      return
    }
  }
  try {
    const res = await joinGroup(group.id, { message: applyMessage, memberRealName })
    const idx = remoteGroups.value.findIndex((x) => x.id === group.id)
    if (idx >= 0) {
      remoteGroups.value[idx].isMember = Boolean(res?.joined)
      remoteGroups.value[idx].hasPendingRequest = Boolean(res?.pending)
      if (remoteGroups.value[idx].isMember) {
        remoteGroups.value[idx].memberCount += 1
      }
    }
    await Promise.all([loadGroups({ resetPage: false }), loadHotGroups()])
    flashMessage(res?.message || (res?.joined ? '加入成功' : '申请已提交'), 'success')
  } catch (error) {
    flashMessage(error.message || '操作失败', 'error')
  }
}

function flashMessage(text, type = 'success') {
  messageType.value = type
  message.value = text
  window.setTimeout(() => {
    message.value = ''
  }, 2400)
}

function buildListParams() {
  return {
    keyword: searchKeyword.value || undefined,
    category: category.value || undefined,
    joinMode: joinMode.value || undefined,
    sort: sort.value === 'newest' ? 'newest' : undefined,
    page: page.value,
    pageSize: PAGE_SIZE,
    status: undefined
  }
}

async function loadGroups(opts = {}) {
  if (opts.resetPage !== false) page.value = 1
  loading.value = true
  errors.list = ''
  try {
    const data = await fetchGroups(buildListParams())
    const items = unwrapPlatformGroupList(data)
    remoteGroups.value = items.map(normalizeGroup)
    listTotal.value = typeof data?.total === 'number' ? data.total : items.length
  } catch (error) {
    remoteGroups.value = []
    listTotal.value = 0
    errors.list = error.message || '无法加载团体列表'
  } finally {
    loading.value = false
  }
}

async function loadHotGroups() {
  sideLoading.hot = true
  errors.hot = ''
  try {
    const data = await fetchHotGroups()
    const arr = Array.isArray(data) ? data : []
    hotGroups.value = arr.map(normalizeGroup)
  } catch (error) {
    hotGroups.value = []
    errors.hot = error.message || '热门团体加载失败'
  } finally {
    sideLoading.hot = false
  }
}

async function loadGroupFeed() {
  sideLoading.feed = true
  errors.feed = ''
  try {
    const data = await fetchGroupFeed()
    const arr = Array.isArray(data) ? data : []
    groupFeed.value = arr.map((item) => ({
      id: item.id,
      groupId: item.groupId,
      groupName: item.groupName || '团体',
      text: item.text || item.content || '',
      time: item.time || '',
      coverUrl: item.coverUrl || item.avatarUrl || DEFAULT_COVER
    }))
  } catch (error) {
    groupFeed.value = []
    errors.feed = error.message || '动态加载失败'
  } finally {
    sideLoading.feed = false
  }
}

function onSearch() {
  page.value = 1
  loadGroups({ resetPage: false })
}

function reloadFirstPage() {
  page.value = 1
  loadGroups({ resetPage: false })
}

function resetFilters() {
  searchKeyword.value = ''
  category.value = ''
  joinMode.value = ''
  sort.value = 'newest'
  reloadFirstPage()
}

function goPage(delta) {
  const next = page.value + delta
  const max = Math.max(1, Math.ceil(listTotal.value / PAGE_SIZE))
  if (next < 1 || next > max) return
  page.value = next
  loadGroups({ resetPage: false })
}

onMounted(() => {
  loadGroups({ resetPage: false })
  loadHotGroups()
  loadGroupFeed()
})
</script>

<style scoped>
.groups-page {
  width: calc(100% - var(--lc-space-8));
  margin: var(--lc-space-4) auto 0;
  color: var(--lc-text);
}

.mobile-shell {
  display: none;
}

.groups-panel {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.group-list-hero {
  align-items: stretch;
}

.group-list-metrics {
  margin-top: 0;
}

.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--lc-space-4);
  flex-wrap: wrap;
}

.hero-text h1 {
  margin: 0;
  color: var(--lc-text);
  font-size: 30px;
}

.hero-text p {
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  font-weight: 700;
  max-width: 520px;
}

.hero-actions {
  display: flex;
  gap: var(--lc-space-3);
  flex-wrap: wrap;
}

.primary-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
  height: 42px;
  padding: 0 var(--lc-space-4);
  border: 0;
  border-radius: var(--lc-radius-xs);
  color: var(--lc-surface);
  background: linear-gradient(135deg, var(--lc-pink), var(--lc-blue));
  box-shadow: var(--lc-shadow-blue);
  font-weight: 900;
  cursor: pointer;
}

.primary-btn.small {
  min-width: 100px;
  height: 36px;
  font-size: var(--lc-text-sm);
}

.secondary-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 var(--lc-space-4);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-blue);
  background: var(--lc-surface);
  font-weight: 900;
  text-decoration: none;
}

.toolbar-row {
  display: grid;
  grid-template-columns: minmax(280px, 520px) auto;
  gap: var(--lc-space-5);
  align-items: center;
  margin-top: 0;
}

.search-bar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) 86px;
  align-items: center;
  height: 40px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  overflow: hidden;
  background: var(--lc-surface);
}

.search-bar span {
  padding-left: var(--lc-space-3);
  color: var(--lc-subtle);
}

.search-bar input {
  min-width: 0;
  height: 100%;
  border: 0;
  outline: 0;
  padding: 0 var(--lc-space-3);
  font-weight: 700;
  color: var(--lc-text);
  background: transparent;
}

.search-bar button {
  height: 100%;
  border: 0;
  border-radius: 0;
  color: var(--lc-blue);
  background: var(--lc-blue-light);
  font-weight: 900;
  cursor: pointer;
}

.selects {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--lc-space-3);
}

.selects select {
  height: 38px;
  min-width: 112px;
  padding: 0 var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-muted);
  background: var(--lc-surface);
  font-weight: 700;
}

.reset-filter-btn {
  height: 38px;
  padding: 0 var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-muted);
  background: var(--lc-surface);
  font-weight: 900;
  cursor: pointer;
}

.page-message {
  margin-top: var(--lc-space-3);
  padding: var(--lc-space-2) var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-green);
  background: var(--lc-green-light);
  font-weight: 900;
  font-size: var(--lc-text-sm);
}

.page-message.error {
  color: var(--lc-red);
  background: var(--lc-red-light);
}

.groups-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: var(--lc-space-6);
  margin-top: 0;
}

.loading-state,
.empty-card,
.error-card {
  min-height: 220px;
  display: grid;
  place-items: center;
  text-align: center;
  margin-top: var(--lc-space-4);
}

.empty-card {
  border: 1px dashed var(--lc-border);
  border-radius: var(--lc-radius-sm);
  color: var(--lc-muted);
}

.empty-card h3 {
  margin: 0;
  color: var(--lc-text);
}

.error-card {
  border: 1px solid var(--lc-red-light);
  background: var(--lc-red-light);
  color: var(--lc-red);
}

.groups-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--lc-space-4);
}

.group-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: var(--lc-space-4);
  padding: var(--lc-space-4);
  box-shadow: none;
}

.card-cover {
  display: block;
  border-radius: var(--lc-radius-xs);
  overflow: hidden;
  align-self: start;
}

.card-cover img {
  width: 72px;
  height: 72px;
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

.title-line h2 {
  margin: 0;
  font-size: var(--lc-text-lg);
  color: var(--lc-text);
}

.pill {
  padding: 3px var(--lc-space-2);
  border-radius: 999px;
  font-size: var(--lc-text-xs);
  font-weight: 900;
  color: var(--lc-blue);
  background: var(--lc-blue-light);
}

.space-card-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
  margin-top: var(--lc-space-3);
}

.space-card-metrics span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: 999px;
  color: var(--lc-muted);
  background: var(--lc-soft);
  font-size: var(--lc-text-xs);
  font-weight: 900;
}

.space-card-metrics strong {
  margin-right: 4px;
  color: var(--lc-text);
}

.meta,
.desc {
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
}

.desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
  margin-top: var(--lc-space-2);
}

.tag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: var(--lc-text-xs);
  font-weight: 800;
  color: var(--lc-muted);
  background: var(--lc-bg);
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--lc-space-3);
  margin-top: auto;
  padding-top: var(--lc-space-3);
}

.link-detail {
  font-size: var(--lc-text-sm);
  font-weight: 900;
  color: var(--lc-blue);
  text-decoration: none;
}

.action-btn {
  min-width: 88px;
}

.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.member-display-compact {
  margin-top: var(--lc-space-2);
  padding: var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  border: 1px dashed var(--lc-blue-border);
  background: var(--lc-blue-light);
  display: flex;
  flex-direction: column;
  gap: var(--lc-space-2);
}

.member-display-compact-hint {
  margin: 0;
  font-size: var(--lc-text-xs);
  font-weight: 700;
  color: var(--lc-muted);
  line-height: 1.45;
}

.member-display-compact-name {
  font-size: var(--lc-text-xs);
  color: var(--lc-muted);
}

.member-display-compact-btn {
  align-self: flex-start;
  min-width: 88px;
  height: 30px;
  padding: 0 var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  border: 1px solid var(--lc-blue-border);
  color: var(--lc-blue);
  background: var(--lc-surface);
  font-weight: 900;
  font-size: var(--lc-text-xs);
  cursor: pointer;
}

.member-display-compact-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--lc-space-4);
  margin-top: var(--lc-space-6);
}

.pagination button {
  min-width: 36px;
  height: 36px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  background: var(--lc-surface);
  font-weight: 900;
  cursor: pointer;
}

.page-info {
  color: var(--lc-muted);
  font-weight: 800;
  font-size: var(--lc-text-sm);
}

.groups-aside {
  display: grid;
  gap: var(--lc-space-4);
  align-content: start;
}

.aside-card {
  padding: var(--lc-space-4);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-sm);
  background: var(--lc-surface);
}

.aside-title h2 {
  margin: 0;
  font-size: var(--lc-text-lg);
}

.compact-list,
.activity-list {
  display: grid;
  gap: var(--lc-space-3);
  margin-top: var(--lc-space-3);
}

.compact-item,
.activity-item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: var(--lc-space-3);
  align-items: center;
  color: inherit;
  text-decoration: none;
}

.compact-item img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.compact-item strong {
  display: block;
  font-size: var(--lc-text-sm);
  color: var(--lc-text);
}

.compact-item span {
  font-size: var(--lc-text-xs);
  color: var(--lc-muted);
}

.activity-item {
  grid-template-columns: 40px minmax(0, 1fr) auto;
}

.activity-item img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.activity-item time {
  font-size: var(--lc-text-xs);
  color: var(--lc-subtle);
}

.side-state,
.side-error {
  margin-top: var(--lc-space-3);
  font-size: var(--lc-text-sm);
  font-weight: 800;
}

.side-error {
  color: var(--lc-red);
}

@media (max-width: 1100px) {
  .groups-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .groups-page {
    width: 100%;
    margin: 0;
    padding: 0 12px calc(112px + env(safe-area-inset-bottom));
    box-sizing: border-box;
  }

  .desktop-shell {
    display: none;
  }

  .mobile-shell {
    display: block;
  }

  .m-hero {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 10px;
    margin: 8px 0 6px;
  }

  .m-hero h1 {
    margin: 0;
    font-size: 18px;
    line-height: 1.25;
    color: var(--lc-text);
  }

  .m-hero p {
    margin: 2px 0 0;
    font-size: 12px;
    color: var(--lc-muted);
    line-height: 1.35;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .m-create {
    height: 34px;
    border: 1px solid var(--lc-blue-border);
    background: var(--lc-surface);
    color: var(--lc-blue);
    border-radius: 999px;
    padding: 0 12px;
    font-size: 13px;
    font-weight: 700;
    flex: none;
  }

  .m-search {
    margin-top: 12px;
    height: 42px;
    display: grid;
    grid-template-columns: auto 1fr;
    align-items: center;
    gap: 6px;
    padding: 0 12px;
    border-radius: 14px;
    border: 1px solid var(--lc-border);
    background: var(--lc-surface);
  }

  .m-search span {
    color: var(--lc-subtle);
    font-size: 14px;
  }

  .m-search input {
    border: 0;
    outline: 0;
    font-size: 14px;
    color: var(--lc-text);
    background: transparent;
  }

  .m-quick-grid {
    margin-top: 8px;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px;
  }

  .m-quick-item {
    height: 34px;
    border: 1px solid var(--lc-border);
    border-radius: 10px;
    background: var(--lc-surface);
    display: grid;
    place-items: center;
    color: var(--lc-text);
    text-decoration: none;
    font-size: 12px;
    font-weight: 700;
    padding: 0;
  }

  .m-list-wrap {
    margin-top: 10px;
  }

  .m-group-list {
    display: grid;
    gap: 8px;
  }

  .m-group-card {
    border: 1px solid var(--lc-border);
    background: var(--lc-surface);
    border-radius: 12px;
    padding: 8px;
    display: grid;
    grid-template-columns: 64px minmax(0, 1fr);
    gap: 8px;
  }

  .m-cover {
    border-radius: 10px;
    overflow: hidden;
    display: block;
    height: 64px;
  }

  .m-cover img {
    width: 64px;
    height: 64px;
    object-fit: cover;
  }

  .m-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  .m-title-row h2 {
    margin: 0;
    font-size: 16px;
    color: var(--lc-text);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .m-cat {
    font-size: 11px;
    color: var(--lc-blue);
    background: var(--lc-blue-light);
    border-radius: 999px;
    padding: 2px 8px;
    flex: none;
  }

  .m-meta,
  .m-desc {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--lc-muted);
  }

  .m-desc {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 1;
    overflow: hidden;
    line-height: 1.4;
    margin-top: 4px;
  }

  .m-meta {
    margin-top: 4px;
    font-size: 12px;
  }

  .m-statuses,
  .m-tags {
    margin-top: 6px;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .m-status,
  .m-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 999px;
    background: var(--lc-bg);
    color: var(--lc-muted);
  }

  .m-status.subtle {
    opacity: 0.78;
  }

  .m-member-display {
    margin-top: 6px;
    padding: 6px 8px;
    border-radius: 10px;
    border: 1px dashed rgba(79, 99, 246, 0.35);
    background: rgba(79, 99, 246, 0.06);
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 6px;
  }

  .m-member-display-hint {
    display: none;
  }

  .m-member-display-hint {
    margin: 0;
    font-size: 11px;
    font-weight: 700;
    color: var(--lc-muted);
    line-height: 1.45;
  }

  .m-member-display-name {
    font-size: 11px;
    color: var(--lc-muted);
  }

  .m-member-display-btn {
    align-self: flex-start;
    min-height: 30px;
    padding: 0 12px;
    border-radius: 10px;
    border: 1px solid var(--lc-blue-border);
    background: var(--lc-surface);
    color: var(--lc-blue);
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
  }

  .m-member-display-btn:disabled {
    opacity: 0.65;
  }

  .m-actions {
    margin-top: 8px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .m-btn {
    height: 34px;
    border-radius: 10px;
    border: 1px solid var(--lc-blue-border);
    background: var(--lc-blue);
    color: var(--lc-surface);
    font-size: 13px;
    font-weight: 700;
    text-decoration: none;
    display: grid;
    place-items: center;
    padding: 0;
  }

  .m-btn.ghost {
    background: var(--lc-surface);
    color: var(--lc-blue);
  }

  .m-btn:disabled {
    opacity: 0.65;
  }

  .m-empty {
    border: 1px dashed var(--lc-border);
    border-radius: 16px;
    padding: 20px 14px;
    text-align: center;
    color: var(--lc-muted);
  }

  .m-empty h3 {
    margin: 0 0 10px;
    color: var(--lc-text);
    font-size: 15px;
    line-height: 1.45;
    font-weight: 700;
  }

  .pagination {
    margin-top: 12px;
  }
}
</style>
