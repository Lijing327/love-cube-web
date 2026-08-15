<template>
  <section class="admin-page">
    <div v-if="loading.detail" class="admin-loading">加载团体详情中...</div>
    <div v-else-if="errors.detail" class="admin-error">
      {{ errors.detail }}
      <button type="button" class="admin-btn" @click="loadDetail">重试</button>
      <router-link :to="backListPath" class="admin-btn">返回列表</router-link>
    </div>

    <template v-else-if="group">
      <section class="platform-card">
        <div class="page-head">
          <div class="title-wrap">
            <img v-if="group.coverUrl" :src="group.coverUrl" :alt="group.name">
            <span v-else>{{ group.name.charAt(0) }}</span>
            <div>
              <h1 class="platform-title">{{ group.name }}</h1>
              <p class="platform-subtitle">
                <span class="role-chip">{{ userRoleName }}</span>
                · {{ categoryDisplay(group.category) }} · {{ group.region || '未设置地区' }} · {{ group.memberCount }} 位成员
              </p>
            </div>
          </div>
          <div class="head-actions">
            <button
              v-if="showDisableButton"
              type="button"
              class="admin-btn"
              :disabled="saving"
              @click="toggleStatus"
            >
              {{ isEnabled(group.status) ? '禁用团体' : '启用团体' }}
            </button>
            <router-link :to="backListPath" class="admin-btn">返回列表</router-link>
          </div>
        </div>
      </section>

      <p v-if="message" class="admin-message" :class="{ error: messageType === 'error' }">{{ message }}</p>

      <nav class="detail-tabs" aria-label="团体管理分区">
        <button v-for="tab in tabs" :key="tab.key" type="button" :class="{ active: activeTab === tab.key }" @click="switchTab(tab.key)">
          {{ tab.label }}
          <span v-if="tab.key === 'members' && pendingMembers.length">{{ pendingMembers.length }}</span>
        </button>
      </nav>

      <section v-if="activeTab === 'info'" class="platform-card form-card">
        <div class="section-head">
          <h2>基础信息</h2>
          <span class="admin-tag" :class="statusClass(group.status)">{{ statusLabel(group.status) }}</span>
        </div>
        <div class="form-grid">
          <label class="form-field required">
            <span>团体名称</span>
            <input v-model.trim="form.name" class="admin-input" maxlength="100" placeholder="团体名称">
          </label>
          <label class="form-field">
            <span>地区</span>
            <input v-model.trim="form.region" class="admin-input" maxlength="40" placeholder="例如：北京">
          </label>
          <label class="form-field">
            <span>分类</span>
            <select v-model="form.category" class="admin-input">
              <option v-for="c in categoryOptions" :key="c.value" :value="c.value">{{ c.label }}</option>
            </select>
          </label>
          <label class="form-field">
            <span>加入方式</span>
            <select v-model="form.joinType" class="admin-input">
              <option value="approval">审核加入</option>
              <option value="open">自由加入</option>
            </select>
          </label>
          <label class="form-field">
            <span>状态</span>
            <select v-model="form.status" class="admin-input">
              <option value="active">启用</option>
              <option value="inactive">禁用</option>
              <option value="published">已发布</option>
              <option value="disabled">已禁用</option>
            </select>
          </label>
          <div class="form-field full-width cover-upload-field">
            <span>上传封面图</span>
            <CoverUploadField
              v-model="form.coverUrl"
              :disabled="saving"
              :allow-manual-input="false"
              :show-remove="false"
              empty-label="上传封面"
              change-label="更换封面"
            />
          </div>
          <label class="form-field full-width">
            <span>团体简介</span>
            <textarea v-model.trim="form.description" class="admin-textarea" rows="5" placeholder="团体简介"></textarea>
          </label>
        </div>
        <div class="form-actions">
          <button type="button" class="admin-btn primary" :disabled="saving || contentCheck.state.checking" @click="saveInfo">{{ saving ? '保存中...' : '保存基础信息' }}</button>
        </div>
      </section>

      <section v-else-if="activeTab === 'members'" class="platform-card">
        <div class="section-head">
          <h2>成员列表</h2>
          <div class="filter-btns">
            <button v-for="item in memberStatusOptions" :key="item.value" type="button" :class="{ active: memberStatus === item.value }" @click="loadMembers(item.value)">
              {{ item.label }}
            </button>
          </div>
        </div>
        <div v-if="loading.members" class="tab-loading">成员加载中...</div>
        <div v-else-if="errors.members" class="inline-error">{{ errors.members }}</div>
        <div v-else-if="members.length" class="member-list">
          <article v-for="member in members" :key="member.id || member.userId" class="member-row">
            <img :src="member.avatar" :alt="member.name">
            <div>
              <strong>{{ member.name }}</strong>
              <p>{{ member.joinedAt || member.requestedAt || '未记录时间' }}</p>
              <p v-if="member.status === 'pending' && member.applyReason">申请说明：{{ member.applyReason }}</p>
            </div>
            <div class="member-row-tags">
              <span class="admin-tag" :class="member.role">{{ roleLabel(member.role) }}</span>
              <span class="admin-tag" :class="member.status">{{ memberStatusLabel(member.status) }}</span>
            </div>
            <div class="row-actions">
              <template v-if="member.status === 'pending'">
                <button type="button" class="admin-btn primary" :disabled="saving" @click="approveMemberRecord(member)">通过</button>
                <button type="button" class="admin-btn danger" :disabled="saving" @click="rejectMemberRecord(member)">拒绝</button>
              </template>
              <button v-else type="button" class="admin-btn danger" :disabled="saving" @click="removeMemberRecord(member)">移除</button>
            </div>
          </article>
        </div>
        <div v-else class="empty-hint">暂无成员记录</div>
      </section>

      <section v-else-if="activeTab === 'posts'" class="platform-card">
        <div class="section-head">
          <h2>团体动态</h2>
        </div>
        <div v-if="loading.posts" class="tab-loading">动态加载中...</div>
        <div v-else-if="errors.posts" class="inline-error">{{ errors.posts }}</div>
        <div v-else-if="postItems.length" class="content-list">
          <article v-for="post in postItems" :key="post.id" class="content-row">
            <header>
              <strong>{{ post.authorName || '团体成员' }}</strong>
              <span>{{ formatDateTime(post.createdAt) }}</span>
            </header>
            <p>{{ post.content }}</p>
            <div v-if="post.images.length" class="image-grid">
              <img v-for="image in post.images" :key="image" :src="image" alt="">
            </div>
          </article>
        </div>
        <div v-else class="empty-hint">暂无动态</div>
      </section>

      <section v-else-if="activeTab === 'articles'" class="platform-card">
        <div class="section-head">
          <h2>团体文章</h2>
        </div>
        <div v-if="loading.articles" class="tab-loading">文章加载中...</div>
        <div v-else-if="errors.articles" class="inline-error">{{ errors.articles }}</div>
        <div v-else-if="articleItems.length" class="content-list">
          <article v-for="article in articleItems" :key="article.id" class="content-row">
            <header>
              <strong>{{ article.title }}</strong>
              <span>{{ formatDateTime(article.createdAt) }}</span>
            </header>
            <p>{{ article.summary || article.content }}</p>
            <p v-if="article.authorName" class="article-meta">作者：{{ article.authorName }}</p>
            <div class="row-actions">
              <button type="button" class="admin-btn danger" :disabled="saving" @click="removeArticleRecord(article)">删除</button>
            </div>
          </article>
        </div>
        <div v-else class="empty-hint">暂无文章。成员可在前台团体页或运营台发布。</div>
      </section>

      <section v-else-if="activeTab === 'notices'" class="platform-card">
        <div class="section-head">
          <h2>团体公告</h2>
        </div>
        <div v-if="loading.notices" class="tab-loading">公告加载中...</div>
        <div v-else-if="errors.notices" class="inline-error">{{ errors.notices }}</div>
        <div v-else-if="noticeItems.length" class="content-list">
          <article v-for="notice in noticeItems" :key="notice.id" class="content-row">
            <header>
              <strong>{{ notice.title }}</strong>
              <span>{{ formatDateTime(notice.createdAt) }}</span>
            </header>
            <p>{{ notice.content }}</p>
          </article>
        </div>
        <div v-else class="empty-hint">暂无公告</div>
      </section>

      <!-- 管理员设置（OWNER only） -->
      <section v-else-if="activeTab === 'admins'" class="platform-card">
        <div class="section-head">
          <h2>管理员设置</h2>
        </div>
        <div v-if="loading.admins" class="tab-loading">加载中...</div>
        <div v-else-if="errors.admins" class="inline-error">{{ errors.admins }}</div>
        <div v-else class="member-list">
          <article v-for="admin in groupAdmins" :key="admin.userId" class="member-row">
            <img :src="admin.avatarUrl || DEFAULT_AVATAR" :alt="admin.username">
            <div>
              <strong>{{ admin.username || '未知用户' }}</strong>
              <p>ID: {{ admin.userId }}</p>
            </div>
            <div class="member-row-tags">
              <span class="admin-role-tag" :class="`ga-${admin.role?.toLowerCase()}`">{{ admin.roleName }}</span>
            </div>
            <div class="row-actions">
              <button
                v-if="admin.role !== 'OWNER'"
                type="button"
                class="admin-btn danger"
                :disabled="saving"
                @click="removeGroupAdminRecord(admin)"
              >移除</button>
              <span v-else class="owner-badge">团长</span>
            </div>
          </article>
          <div v-if="!groupAdmins.length" class="empty-hint">暂无管理员记录</div>
        </div>

        <div class="add-admin-form">
          <h3>添加管理员</h3>
          <p class="add-admin-hint">
            请先从已加入成员中选择对方；若对方尚未加入本团体，可填写其用户 ID（数字）。也可在「成员审核」页查看已加入名单与 ID。
          </p>
          <div class="add-admin-stack">
            <label class="add-admin-field">
              <span>从已加入成员选择</span>
              <select
                v-model="newAdminPickerUserId"
                class="admin-input"
                @change="onAdminPickerChange"
              >
                <option value="">请选择成员…</option>
                <option v-for="m in selectableAdminMembers" :key="m.userId" :value="String(m.userId)">
                  {{ m.name }}（ID {{ m.userId }}）
                </option>
              </select>
            </label>
            <label class="add-admin-field">
              <span>或填写用户 ID</span>
              <input
                v-model.trim="newAdminManualUserId"
                class="admin-input"
                type="text"
                inputmode="numeric"
                autocomplete="off"
                placeholder="仅数字，例如 12345"
                @input="onAdminManualInput"
              >
            </label>
          </div>
          <div class="add-admin-row add-admin-actions-row">
            <select v-model="newAdminRole" class="admin-input admin-input-sm">
              <option value="ADMIN">副管理员</option>
              <option value="REVIEWER">审核员</option>
            </select>
            <button type="button" class="admin-btn primary" :disabled="saving || !canSubmitNewAdmin" @click="addGroupAdminRecord">添加</button>
          </div>
        </div>
      </section>
    </template>

    <ContentCheckDialog
      v-if="contentCheck.state.show"
      :suggestion="contentCheck.state.suggestion"
      :hit-words="contentCheck.state.hitWords"
      @use-suggestion="contentCheck.applySuggestion"
      @continue="contentCheck.continueAnyway"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addAdminGroupAdmin,
  approveAdminGroupRequest,
  fetchAdminGroupDetail,
  fetchAdminGroupMembers,
  deleteGroupArticle,
  fetchGroupArticles,
  fetchLegacyGroupPosts,
  getAdminGroupAdmins,
  getAdminGroupJoinRequests,
  rejectAdminGroupRequest,
  removeAdminGroupAdmin,
  removeAdminGroupMember,
  updateAdminGroup
} from '@/api/groups.js'
import CoverUploadField from '@/components/admin/CoverUploadField.vue'
import { useContentCheck } from '@/composables/useContentCheck.js'
import ContentCheckDialog from '@/components/common/ContentCheckDialog.vue'
import { useUserStore } from '@/stores/user.js'
import { PLATFORM_GROUP_CATEGORY_OPTIONS, labelForGroupCategory } from '@/utils/groupCategories.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const contentCheck = useContentCheck()
const groupId = String(route.params.id)
const isMyGroupsContext = route.path.includes('/my-groups/')
const DEFAULT_AVATAR = 'https://api.dicebear.com/7.x/initials/svg?seed=LC&backgroundColor=eff6ff,fdf2f8,eef2ff'

const backListPath = isMyGroupsContext ? '/admin/my-groups' : '/admin/platform/groups'

const userRole = ref('OWNER')
const userRoleName = ref('团长')
const userPermissions = ref([])

const activeTab = ref(typeof route.query.tab === 'string' ? route.query.tab : 'info')
const group = ref(null)
const members = ref([])
const posts = ref([])
const groupAdmins = ref([])
const isFirstDetailLoad = ref(true)
const pendingMembers = ref([])
const postsLoaded = ref(false)
const articlesLoaded = ref(false)
const articles = ref([])
const memberStatus = ref('approved')
const saving = ref(false)
const message = ref('')
const messageType = ref('success')
const newAdminPickerUserId = ref('')
const newAdminManualUserId = ref('')
const newAdminRole = ref('ADMIN')
/** 打开「管理员设置」时拉取的已加入成员，用于添加管理员时按昵称选择 */
const adminCandidateMembers = ref([])

const loading = reactive({ detail: true, members: false, posts: false, notices: false, articles: false, admins: false })
const errors = reactive({ detail: '', members: '', posts: '', notices: '', articles: '', admins: '' })
const form = reactive({ name: '', region: '', category: 'interest', description: '', coverUrl: '', joinType: 'approval', status: 'active' })

const LEGACY_CATEGORY_MAP = {
  study: 'growth',
  family: 'life',
  service: 'volunteer',
  church: 'interest'
}

const categoryOptions = computed(() => {
  const base = [...PLATFORM_GROUP_CATEGORY_OPTIONS]
  const cur = String(form.category || '').trim().toLowerCase()
  if (cur && !base.some((o) => o.value === cur)) {
    base.push({ label: labelForGroupCategory(cur), value: cur })
  }
  return base
})

function categoryDisplay(value) {
  return labelForGroupCategory(value) || '未分类'
}

function normalizeCategoryValue(raw) {
  const key = String(raw || '').trim().toLowerCase()
  if (!key) return 'interest'
  if (PLATFORM_GROUP_CATEGORY_OPTIONS.some((o) => o.value === key)) return key
  return LEGACY_CATEGORY_MAP[key] || key
}

const ALL_TABS = [
  { key: 'info',    label: '基础信息' },
  { key: 'members', label: '成员审核' },
  { key: 'posts',    label: '团体动态' },
  { key: 'articles', label: '团体文章' },
  { key: 'notices',  label: '团体公告' },
  { key: 'admins',   label: '管理员设置' }
]

function tabVisible(key) {
  if (key === 'members') return true
  if (key === 'admins') {
    return userRole.value === 'OWNER' || (userPermissions.value && userPermissions.value.includes('group.manage.admins'))
  }
  if (key === 'info' || key === 'posts' || key === 'notices' || key === 'articles') {
    return userRole.value !== 'REVIEWER'
  }
  return true
}

const tabs = computed(() => ALL_TABS.filter(t => tabVisible(t.key)))

const showDisableButton = computed(() =>
  userPermissions.value?.includes('group.delete')
)

const memberStatusOptions = computed(() => {
  if (userRole.value === 'REVIEWER') {
    return [{ label: '待审核', value: 'pending' }]
  }
  return [
    { label: '已加入', value: 'approved' },
    { label: '待审核', value: 'pending' },
    { label: '全部', value: 'all' }
  ]
})

const currentOperatorUserId = computed(() => {
  const u = userStore.syncCurrentUser()
  const id = u?.userId || u?.id || userStore.userId
  const n = Number(id)
  return Number.isFinite(n) && n > 0 ? n : 0
})

/** 已是管理员或当前登录用户不可再添加 */
const selectableAdminMembers = computed(() => {
  const adminIds = new Set(groupAdmins.value.map((a) => Number(a.userId)))
  const self = currentOperatorUserId.value
  return adminCandidateMembers.value.filter((m) => {
    const uid = Number(m.userId)
    if (!Number.isFinite(uid) || uid < 1) return false
    if (adminIds.has(uid)) return false
    if (self && uid === self) return false
    return true
  })
})

const canSubmitNewAdmin = computed(() => {
  const raw = (newAdminPickerUserId.value || '').trim() || (newAdminManualUserId.value || '').trim()
  if (!raw) return false
  const n = Number(raw)
  return Number.isInteger(n) && n > 0
})

const postItems = computed(() => posts.value
  .filter(p => isPostType(p))
  .map(item => ({
    ...item,
    images: parseImages(item.imageUrls)
  })))

const noticeItems = computed(() => posts.value
  .filter(p => isNoticeType(p))
  .map(n => ({
    id: n.id,
    title: noticeTitle(n),
    content: n.content,
    createdAt: n.createdAt
  })))

const articleItems = computed(() => articles.value.map((item) => ({
  id: item.id,
  title: item.title || '未命名文章',
  summary: item.summary || '',
  content: item.content || '',
  authorName: item.authorName || '',
  createdAt: item.createdAt
})))

function isNoticeType(p) {
  const t = String(p.type || '').toLowerCase()
  return t === 'notice' || t === 'announcement' || t === 'bulletin'
}

function isPostType(p) {
  return !isNoticeType(p)
}

function noticeTitle(n) {
  const c = String(n.content || '').trim()
  const line = c.split(/\r?\n/)[0]
  if (line && line.length <= 100) return line
  return '团体公告'
}

async function loadDetail() {
  loading.detail = true
  errors.detail = ''
  try {
    const response = await fetchAdminGroupDetail(groupId)
    const data = unwrapDetail(response)
    if (!data) {
      console.warn('[AdminGroupDetail] empty detail response', response)
      errors.detail = '团体不存在或后台详情接口未返回该团体'
      group.value = null
      return
    }
    userRole.value = data.userRole || 'OWNER'
    userRoleName.value = data.userRoleName || '团长'
    userPermissions.value = Array.isArray(data.userPermissions) ? data.userPermissions : []

    if (isFirstDetailLoad.value) {
      const qTab = typeof route.query.tab === 'string' ? route.query.tab : null
      if (qTab && !tabVisible(qTab)) {
        await router.replace('/admin/403')
        return
      }
      if (!qTab && userRole.value === 'REVIEWER') {
        activeTab.value = 'members'
        await router.replace({ path: route.path, query: { ...route.query, tab: 'members' } })
      } else if (qTab && tabVisible(qTab)) {
        activeTab.value = qTab
      }
      isFirstDetailLoad.value = false
    }

    group.value = normalizeGroup(data)
    syncForm(group.value)
  } catch (err) {
    group.value = null
    errors.detail = err.message || '团体详情加载失败'
  } finally {
    loading.detail = false
  }
}

async function loadTabData(tab) {
  if (tab === 'members') {
    const st = userRole.value === 'REVIEWER' ? 'pending' : memberStatus.value
    if (st === 'pending') {
      await loadMembers(st)
    } else {
      await Promise.all([loadMembers(st), loadPendingCount()])
    }
  }
  if (tab === 'posts') await loadPosts()
  if (tab === 'articles') await loadArticles()
  if (tab === 'notices') await loadNotices()
  if (tab === 'admins') await loadGroupAdmins()
}

async function switchTab(tab) {
  if (!tabVisible(tab)) {
    await router.replace('/admin/403')
    return
  }
  activeTab.value = tab
  await router.replace({ path: route.path, query: { ...route.query, tab } })
  await loadTabData(tab)
}

watch(
  () => route.query.tab,
  async (t) => {
    if (loading.detail) return
    const key = typeof t === 'string' ? t : 'info'
    if (!tabVisible(key)) {
      await router.replace('/admin/403')
      return
    }
    if (activeTab.value !== key) {
      activeTab.value = key
      await loadTabData(key)
    }
  }
)

async function loadGroupAdmins() {
  loading.admins = true
  errors.admins = ''
  adminCandidateMembers.value = []
  try {
    const [admRes, memRes] = await Promise.allSettled([
      getAdminGroupAdmins(groupId),
      fetchAdminGroupMembers(groupId)
    ])
    if (admRes.status !== 'fulfilled') throw admRes.reason
    const data = admRes.value
    groupAdmins.value = Array.isArray(data) ? data : data?.data ?? []
    if (memRes.status === 'fulfilled') {
      adminCandidateMembers.value = unwrapList(memRes.value).map(normalizeMemberRow)
    }
  } catch (err) {
    errors.admins = err.message || '管理员列表加载失败'
    groupAdmins.value = []
    adminCandidateMembers.value = []
  } finally {
    loading.admins = false
  }
}

function onAdminPickerChange() {
  if ((newAdminPickerUserId.value || '').trim()) newAdminManualUserId.value = ''
}

function onAdminManualInput() {
  if ((newAdminManualUserId.value || '').trim()) newAdminPickerUserId.value = ''
}

function resetNewAdminForm() {
  newAdminPickerUserId.value = ''
  newAdminManualUserId.value = ''
}

async function addGroupAdminRecord() {
  const raw = (newAdminPickerUserId.value || '').trim() || (newAdminManualUserId.value || '').trim()
  if (!raw) return
  const uid = Number(raw)
  if (!Number.isInteger(uid) || uid < 1) {
    flash('请输入有效的用户 ID', 'error')
    return
  }
  saving.value = true
  try {
    await addAdminGroupAdmin(groupId, { userId: uid, role: newAdminRole.value })
    flash('管理员已添加')
    resetNewAdminForm()
    await loadGroupAdmins()
  } catch (err) {
    flash(err.message || '添加失败', 'error')
  } finally {
    saving.value = false
  }
}

async function removeGroupAdminRecord(admin) {
  if (!window.confirm(`确定移除管理员 ${admin.username || admin.userId}？`)) return
  saving.value = true
  try {
    await removeAdminGroupAdmin(groupId, admin.userId)
    flash('管理员已移除')
    await loadGroupAdmins()
  } catch (err) {
    flash(err.message || '移除失败', 'error')
  } finally {
    saving.value = false
  }
}

async function loadMembers(status = 'approved') {
  let st = status
  if (userRole.value === 'REVIEWER') st = 'pending'
  memberStatus.value = st
  loading.members = true
  errors.members = ''
  try {
    if (st === 'pending') {
      const data = await getAdminGroupJoinRequests(groupId, 'pending')
      members.value = unwrapList(data).map(normalizeJoinRequest)
      pendingMembers.value = members.value
    } else if (st === 'all') {
      const [pendingRes, approvedResult] = await Promise.allSettled([
        getAdminGroupJoinRequests(groupId, 'pending'),
        fetchAdminGroupMembers(groupId)
      ])
      const pending = pendingRes.status === 'fulfilled' ? unwrapList(pendingRes.value).map(normalizeJoinRequest) : []
      const approved = approvedResult.status === 'fulfilled' ? unwrapList(approvedResult.value).map(normalizeMemberRow) : []
      members.value = [...pending, ...approved]
      pendingMembers.value = pending
      if (pendingRes.status === 'rejected') throw pendingRes.reason
      if (approvedResult.status === 'rejected') throw approvedResult.reason
    } else {
      const data = await fetchAdminGroupMembers(groupId)
      members.value = unwrapList(data).map(normalizeMemberRow)
    }
  } catch (err) {
    members.value = []
    errors.members = err.message || '成员列表加载失败'
  } finally {
    loading.members = false
  }
}

async function loadPendingCount() {
  try {
    const data = await getAdminGroupJoinRequests(groupId, 'pending')
    pendingMembers.value = unwrapList(data).map(normalizeJoinRequest)
  } catch {
    pendingMembers.value = []
  }
}

async function loadPosts() {
  if (postsLoaded.value) return
  loading.posts = true
  errors.posts = ''
  try {
    const data = await fetchLegacyGroupPosts(groupId)
    posts.value = unwrapList(data)
    postsLoaded.value = true
  } catch (err) {
    posts.value = []
    errors.posts = err.message || '团体动态加载失败'
  } finally {
    loading.posts = false
  }
}

async function loadArticles() {
  if (articlesLoaded.value) return
  loading.articles = true
  errors.articles = ''
  try {
    articles.value = unwrapList(await fetchGroupArticles(groupId))
    articlesLoaded.value = true
  } catch (err) {
    articles.value = []
    errors.articles = err.message || '团体文章加载失败'
  } finally {
    loading.articles = false
  }
}

async function removeArticleRecord(article) {
  if (!window.confirm(`确定删除文章「${article.title}」？`)) return
  saving.value = true
  try {
    await deleteGroupArticle(groupId, article.id)
    articles.value = articles.value.filter((item) => String(item.id) !== String(article.id))
    flash('文章已删除')
  } catch (err) {
    flash(err.message || '删除失败', 'error')
  } finally {
    saving.value = false
  }
}

async function loadNotices() {
  if (postsLoaded.value) return
  loading.notices = true
  errors.notices = ''
  try {
    const data = await fetchLegacyGroupPosts(groupId)
    posts.value = unwrapList(data)
    postsLoaded.value = true
  } catch (err) {
    errors.notices = err.message || '团体公告加载失败'
  } finally {
    loading.notices = false
  }
}

async function saveInfo() {
  if (!form.name) {
    flash('团体名称不能为空', 'error')
    return
  }
  if (form.description) {
    const checkResult = await contentCheck.check(form.description, 'group-info')
    if (!checkResult.ok) return
    if (checkResult.suggestion) form.description = checkResult.suggestion
  }
  saving.value = true
  try {
    await updateAdminGroup(groupId, {
      name: form.name,
      category: form.category,
      description: form.description,
      coverUrl: form.coverUrl,
      joinType: form.joinType,
      status: form.status
    })
    await loadDetail()
    flash('基础信息已保存')
  } catch (err) {
    flash(err.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function toggleStatus() {
  const nextStatus = isEnabled(group.value.status) ? disabledStatus(group.value.status) : enabledStatus(group.value.status)
  form.status = nextStatus
  await saveInfo()
}

async function approveMemberRecord(member) {
  saving.value = true
  try {
    if (member.status === 'pending') {
      await approveAdminGroupRequest(groupId, member.id)
    }
    flash('成员申请已通过')
    if (group.value) group.value.memberCount++
    await loadMembers(memberStatus.value)
  } catch (err) {
    flash(err.message || '审核失败', 'error')
  } finally {
    saving.value = false
  }
}

async function rejectMemberRecord(member) {
  saving.value = true
  try {
    if (member.status === 'pending') {
      await rejectAdminGroupRequest(groupId, member.id)
    }
    flash('成员申请已拒绝')
    await loadMembers(memberStatus.value)
    await loadPendingCount()
  } catch (err) {
    flash(err.message || '审核失败', 'error')
  } finally {
    saving.value = false
  }
}

async function removeMemberRecord(member) {
  if (!window.confirm(`确定移除成员 ${member.name}？`)) return
  saving.value = true
  try {
    await removeAdminGroupMember(groupId, member.userId)
    flash('成员已移除')
    if (group.value) group.value.memberCount--
    await loadMembers(memberStatus.value)
  } catch (err) {
    flash(err.message || '移除失败', 'error')
  } finally {
    saving.value = false
  }
}

function normalizeGroup(item) {
  return {
    id: String(item.id || groupId),
    name: item.name || '未命名团体',
    region: item.region || item.location || '',
    category: item.category || item.typeName || item.type || '',
    description: item.description || '',
    coverUrl: item.coverUrl || '',
    status: item.status || 'active',
    joinType: item.joinType || (item.joinMode === 'free' ? 'open' : 'approval'),
    memberCount: Number(item.memberCount || 0)
  }
}

function normalizeMemberRow(item) {
  return {
    id: item.id,
    userId: item.userId,
    name: item.username || '未命名成员',
    avatar: item.avatarUrl || DEFAULT_AVATAR,
    role: item.role || 'member',
    status: 'approved',
    joinedAt: formatDateTime(item.joinedAt),
    requestedAt: '',
    applyReason: ''
  }
}

function normalizeJoinRequest(item) {
  return {
    id: item.id,
    userId: item.userId,
    name: item.username || '未命名用户',
    avatar: item.avatarUrl || DEFAULT_AVATAR,
    role: 'member',
    status: 'pending',
    joinedAt: '',
    requestedAt: formatDateTime(item.requestedAt),
    applyReason: item.message || ''
  }
}

function syncForm(item) {
  Object.assign(form, {
    name: item.name,
    region: item.region,
    category: normalizeCategoryValue(item.category),
    description: item.description,
    coverUrl: item.coverUrl,
    joinType: item.joinType,
    status: item.status
  })
}

function unwrapList(res) {
  if (Array.isArray(res)) return res
  if (Array.isArray(res?.data)) return res.data
  return []
}

function unwrapDetail(res) {
  if (!res) return null
  if (Object.prototype.hasOwnProperty.call(res, 'data')) {
    const payload = res.data
    if (payload?.data) return payload.data
    return payload || null
  }
  return res
}

function parseImages(value) {
  if (Array.isArray(value)) return value.filter(Boolean)
  if (!value) return []
  return String(value).split(',').map(item => item.trim()).filter(Boolean)
}

function isEnabled(value) {
  return value === 'active' || value === 'published'
}

function enabledStatus(value) {
  return value === 'disabled' ? 'published' : 'active'
}

function disabledStatus(value) {
  return value === 'published' ? 'disabled' : 'inactive'
}

function statusClass(value) {
  return isEnabled(value) ? 'active' : 'inactive'
}

function statusLabel(value) {
  return isEnabled(value) ? '启用' : '禁用'
}

function roleLabel(value) {
  if (value === 'owner') return '负责人'
  if (value === 'admin') return '管理员'
  return '成员'
}

function memberStatusLabel(value) {
  if (value === 'approved') return '已加入'
  if (value === 'pending') return '待审核'
  if (value === 'rejected') return '已拒绝'
  return value || '-'
}

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', { hour12: false })
}

function flash(text, type = 'success') {
  message.value = text
  messageType.value = type
  window.setTimeout(() => {
    message.value = ''
  }, 2200)
}

onMounted(async () => {
  await loadDetail()
  if (errors.detail) return
  // members tab: loadTabData handles pending count internally
  // other tabs: load pending count badge in parallel with tab data
  const tasks = [loadTabData(activeTab.value)]
  if (activeTab.value !== 'members') tasks.push(loadPendingCount())
  await Promise.all(tasks)
})
</script>

<style scoped>
.page-head,
.title-wrap,
.head-actions,
.section-head,
.filter-btns,
.row-actions {
  display: flex;
  align-items: center;
}

.page-head {
  justify-content: space-between;
  gap: 16px;
}

.title-wrap {
  gap: 14px;
}

.title-wrap img,
.title-wrap > span {
  width: 58px;
  height: 42px;
  border-radius: 8px;
  flex-shrink: 0;
}

.title-wrap img {
  object-fit: cover;
}

.title-wrap > span {
  display: grid;
  place-items: center;
  color: var(--lc-surface);
  background: linear-gradient(135deg, var(--lc-pink), var(--lc-blue));
  font-weight: 900;
}

.head-actions {
  gap: 8px;
  flex-wrap: wrap;
}

.head-actions a {
  text-decoration: none;
}

.admin-message {
  margin: 12px 0;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--lc-green);
  background: var(--lc-green-light);
  font-size: 13px;
  font-weight: 900;
}

.admin-message.error,
.inline-error {
  color: var(--lc-red);
  background: var(--lc-red-light);
}

.detail-tabs {
  display: flex;
  gap: 8px;
  margin: 14px 0;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  padding-bottom: 2px;
}

.detail-tabs button {
  position: relative;
  flex: 0 0 auto;
  min-height: 40px;
  border: 1px solid var(--lc-border);
  border-radius: 8px;
  padding: 8px 14px;
  color: var(--lc-muted);
  background: var(--lc-surface);
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
  line-height: 1.2;
}

.detail-tabs button.active {
  border-color: var(--lc-blue-border);
  color: var(--lc-blue);
  background: var(--lc-blue-light);
}

.detail-tabs span {
  display: inline-grid;
  place-items: center;
  min-width: 18px;
  height: 18px;
  margin-left: 6px;
  border-radius: 999px;
  color: var(--lc-surface);
  background: var(--lc-pink);
  font-size: 11px;
}

.section-head {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.section-head h2 {
  margin: 0;
  color: var(--lc-text);
  font-size: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.form-field {
  display: grid;
  gap: 8px;
}

.form-field span {
  color: var(--lc-muted);
  font-size: 13px;
  font-weight: 900;
}

.form-field.required span::after {
  content: ' *';
  color: var(--lc-red);
}

.form-field.full-width {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--lc-border);
}

.filter-btns {
  gap: 8px;
  flex-wrap: wrap;
}

.filter-btns button {
  height: 32px;
  border: 1px solid var(--lc-border);
  border-radius: 8px;
  padding: 0 12px;
  color: var(--lc-muted);
  background: var(--lc-surface);
  font-weight: 900;
  cursor: pointer;
}

.filter-btns button.active {
  border-color: var(--lc-blue-border);
  color: var(--lc-blue);
  background: var(--lc-blue-light);
}

.tab-loading,
.empty-hint,
.inline-error {
  padding: 28px;
  border-radius: 8px;
  color: var(--lc-muted);
  background: var(--lc-bg);
  text-align: center;
  font-weight: 800;
}

.inline-error {
  color: var(--lc-red);
}

.member-list,
.content-list {
  display: grid;
  gap: 12px;
}

.member-row,
.content-row {
  border: 1px solid var(--lc-border);
  border-radius: 10px;
  background: var(--lc-bg);
}

.member-row-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.member-row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
}

.member-row img {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}

.member-row strong {
  color: var(--lc-text);
}

.member-row p {
  margin: 4px 0 0;
  color: var(--lc-subtle);
  font-size: 12px;
}

.row-actions {
  gap: 8px;
  flex-wrap: wrap;
}

.content-row {
  padding: 14px;
}

.content-row header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--lc-text);
  font-weight: 900;
}

.content-row header span {
  color: var(--lc-subtle);
  font-size: 12px;
  font-weight: 700;
}

.content-row p {
  margin: 10px 0 0;
  color: var(--lc-muted);
  line-height: 1.7;
  white-space: pre-line;
}

.article-meta {
  font-size: 12px;
  color: var(--lc-subtle);
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.image-grid img {
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: 8px;
  object-fit: cover;
}

.admin-tag.active,
.admin-tag.approved,
.admin-tag.owner,
.admin-tag.open {
  color: var(--lc-green);
  background: var(--lc-green-light);
}

.admin-tag.inactive,
.admin-tag.member {
  color: var(--lc-muted);
  background: var(--lc-soft);
}

.admin-tag.pending,
.admin-tag.approval {
  color: var(--lc-amber);
  background: var(--lc-amber-light);
}

.admin-tag.rejected,
.admin-tag.admin {
  color: var(--lc-red);
  background: var(--lc-red-light);
}

@media (max-width: 900px) {
  .page-head,
  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .section-head .filter-btns {
    width: 100%;
    justify-content: flex-start;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .member-row {
    grid-template-columns: 48px 1fr;
    grid-template-rows: auto auto auto;
    align-items: start;
    padding: 14px;
    gap: 10px 12px;
  }

  .member-row > img {
    grid-column: 1;
    grid-row: 1;
    width: 48px;
    height: 48px;
  }

  .member-row > div:first-of-type {
    grid-column: 2;
    grid-row: 1;
    min-width: 0;
  }

  .member-row > .member-row-tags {
    grid-column: 1 / -1;
    grid-row: 2;
  }

  .member-row > .row-actions {
    grid-column: 1 / -1;
    grid-row: 3;
    width: 100%;
    justify-content: flex-end;
  }

  .member-row > .row-actions .admin-btn {
    min-height: 44px;
    padding-left: 16px;
    padding-right: 16px;
  }

  .image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .detail-tabs {
    display: grid;
    grid-template-columns: 1fr 1fr;
    overflow-x: visible;
    gap: 10px;
  }

  .detail-tabs button {
    min-height: 48px;
    white-space: normal;
    text-align: center;
    padding: 10px 8px;
    font-size: 13px;
  }

  .detail-tabs button:last-child:nth-child(odd) {
    grid-column: 1 / -1;
  }

  .head-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .head-actions .admin-btn,
  .head-actions a.admin-btn {
    width: 100%;
    min-height: 48px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
  }

  .filter-btns button {
    min-height: 40px;
    padding: 0 14px;
  }

  .add-admin-row {
    flex-direction: column;
    align-items: stretch;
  }

  .add-admin-row .admin-input,
  .add-admin-row .admin-input-sm,
  .add-admin-row .admin-btn {
    width: 100%;
    max-width: none;
    min-height: 44px;
  }

  .add-admin-stack .admin-input,
  .add-admin-actions-row .admin-input,
  .add-admin-actions-row .admin-input-sm,
  .add-admin-actions-row .admin-btn {
    width: 100%;
    max-width: none;
    min-height: 44px;
  }
}

.role-chip {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 800;
  color: var(--lc-blue);
  background: var(--lc-blue-light);
}

.admin-role-tag {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

.admin-role-tag.ga-owner {
  color: var(--lc-amber);
  background: var(--lc-amber-light);
}

.admin-role-tag.ga-admin {
  color: var(--lc-blue);
  background: var(--lc-border);
}

.admin-role-tag.ga-reviewer {
  color: var(--lc-muted);
  background: var(--lc-soft);
}

.owner-badge {
  font-size: 12px;
  font-weight: 800;
  color: var(--lc-muted);
}

.add-admin-form {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--lc-border);
}

.add-admin-form h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--lc-text);
}

.add-admin-hint {
  margin: 0 0 14px;
  padding: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--lc-muted);
}

.add-admin-stack {
  display: grid;
  gap: 14px;
  margin-bottom: 14px;
}

.add-admin-field {
  display: grid;
  gap: 6px;
}

.add-admin-field > span {
  font-size: 13px;
  font-weight: 800;
  color: var(--lc-muted);
}

.add-admin-actions-row {
  margin-top: 4px;
}

.add-admin-actions-row .admin-input-sm {
  flex: 1 1 140px;
  max-width: min(240px, 100%);
}

.add-admin-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.admin-input-sm {
  max-width: 140px;
}
</style>
