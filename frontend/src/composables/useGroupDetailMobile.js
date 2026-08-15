/**
 * 团体详情 — 移动端共享逻辑（/m/platform 与 /platform 均可）
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  approveAdminGroupRequest,
  approveMember,
  cancelGroupActivitySignup,
  claimGroupTask,
  createCheckin,
  createGroupActivity,
  createGroupPoll,
  createGroupPost,
  createGroupPostComment,
  createPlatformCheckinComment,
  deletePlatformCheckinComment,
  deletePlatformGroupPostComment,
  fetchCheckinRankings,
  fetchCheckinSummary,
  fetchGroupActivities,
  fetchGroupDetail,
  fetchGroupMembers,
  fetchGroupArticles,
  createGroupArticle,
  deleteGroupArticle,
  fetchGroupNotices,
  fetchGroupPoll,
  fetchGroupPolls,
  fetchGroupPostComments,
  fetchGroupPosts,
  fetchGroupSeasonRank,
  fetchGroupSeasonRankings,
  generateGroupActivityCheckinCode,
  checkinGroupActivity,
  fetchGroupActivityReviewCandidates,
  submitGroupActivityReview,
  fetchGroupWeeklyDigest,
  sendGroupWeeklyDigest,
  fetchPlatformCheckinComments,
  fetchTodayTasks,
  getAdminGroupJoinRequests,
  isLegacyPlatformGroupId,
  joinGroup,
  likePlatformCheckin,
  patchPlatformGroupMemberRole,
  rejectAdminGroupRequest,
  rejectMember,
  removeGroupMember,
  resolveSpaceManageEntry,
  revealGroupPollResults,
  signUpGroupActivity,
  submitGroupPollVotes,
  toggleGroupPostLike,
  unlikePlatformCheckin,
  unwrapGroupPostsList,
  updateGroupActivity
} from '@/api/groups.js'
import { fetchEvents } from '@/api/platformContent.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import {
  AUDIT_JOIN_MESSAGE_PROMPT,
  ERR_EMPTY_AUDIT_JOIN_MESSAGE,
  ERR_EMPTY_MEMBER_REAL_NAME,
  JOIN_MEMBER_REAL_NAME_PROMPT
} from '@/utils/groupMemberDisplayName.js'
import { useUserStore } from '@/stores/user.js'

const DEFAULT_COVER = 'https://images.unsplash.com/photo-1507692049790-de58290a4334?auto=format&fit=crop&w=800&q=80'
const DEFAULT_AVATAR = 'https://api.dicebear.com/7.x/initials/svg?seed=LC&backgroundColor=eff6ff'

const CHECKIN_TYPES = [
  { value: 'thanks', label: '感谢' },
  { value: 'prayer', label: '心愿' },
  { value: 'study', label: '学习' },
  { value: 'share', label: '分享' },
  { value: 'other', label: '其他' }
]

const CHECKIN_TYPE_LABELS = Object.fromEntries(CHECKIN_TYPES.map(t => [t.value, t.label]))

export function useGroupDetailMobile() {
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  const { groupsPath } = usePlatformPath()

  const group = ref(null)
  const loading = ref({
    detail: true,
    posts: false,
    members: false,
    checkin: false,
    tasks: false,
    activities: false,
    polls: false
  })
  const errors = reactive({ detail: '', posts: '', members: '' })
  const message = ref('')
  const messageType = ref('success')

  const rawPosts = ref([])
  const rawMembers = ref([])
  const rawNotices = ref([])
  const rawArticles = ref([])
  const articleForm = reactive({ title: '', content: '' })
  const publishingArticle = ref(false)
  const expandedArticleId = ref(null)
  const deletingArticleId = ref(null)
  const todayTasks = ref([])
  const activities = ref([])
  const upcomingActivities = ref([])
  const polls = ref([])
  const seasonRank = ref(null)
  const seasonTop = ref([])
  const checkinSummary = reactive({
    checkedInToday: false,
    todayCount: 0,
    myStreakDays: 0,
    recentCheckins: []
  })

  const joining = ref(false)
  const posting = ref(false)
  const checkingIn = ref(false)
  const claimingTask = ref('')
  const postDraft = ref('')
  const checkinForm = reactive({ type: 'thanks', content: '' })

  const signingActivityId = ref(null)
  const cancellingActivityId = ref(null)
  const showCreateActivity = ref(false)
  const showProposalDialog = ref(false)
  const creatingActivity = ref(false)
  const activityForm = reactive({
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    location: '',
    maxParticipants: 0,
    platformEventId: ''
  })
  const platformEvents = ref([])
  const checkinCodeBusyId = ref(null)
  const activityCheckinOpen = ref(false)
  const activityCheckinCode = ref('')
  const activityCheckinTarget = ref(null)
  const activityCheckinSubmitting = ref(false)
  const weeklyDigest = ref(null)
  const sendingDigest = ref(false)
  const reportDialogOpen = ref(false)
  const reportPostId = ref(null)
  const reportTargetUserId = ref(null)
  const activityReviewOpen = ref(false)
  const activityReviewTarget = ref(null)
  const activityReviewCandidates = ref([])
  const activityReviewLoading = ref(false)
  const activityReviewRatings = reactive({})

  const expandedPollId = ref(null)
  const pollDetailCache = reactive({})
  const pollPick = reactive({})
  const votingPollId = ref(null)
  const revealingPollId = ref(null)
  const showPollCreate = ref(false)
  const creatingPoll = ref(false)
  const pollForm = reactive({
    title: '',
    description: '',
    selectionMode: 'single',
    maxChoices: 2,
    startTime: '',
    endTime: '',
    optionsText: ''
  })

  const moderatingMemberId = ref(null)
  const removingMemberId = ref(null)
  const roleChangingMemberId = ref(null)
  const expandedPostId = ref(null)
  const feedComments = reactive({})
  const commentsLoading = reactive({})
  const commentDraft = reactive({})
  const commentPosting = reactive({})
  const deletingCommentKey = ref('')
  const rankingTab = ref('daily')
  const rankingsPayload = reactive({ items: [], currentUser: null })
  const loadingRankings = ref(false)
  const expandedCheckinId = ref(null)
  const checkinCommentsById = reactive({})
  const checkinCommentsLoading = reactive({})
  const checkinCommentDraft = reactive({})
  const checkinCommentPosting = ref(false)
  const checkinCommentsShowAll = reactive({})
  const checkinLikeBusyId = ref(null)

  const activeTab = computed(() => {
    const p = route.path
    if (p.endsWith('/posts')) return 'posts'
    if (p.endsWith('/members')) return 'members'
    if (p.endsWith('/notices')) return 'notices'
    if (p.endsWith('/articles')) return 'articles'
    if (p.endsWith('/checkin')) return 'checkin'
    if (p.endsWith('/tasks')) return 'tasks'
    if (p.endsWith('/activities')) return 'activities'
    if (p.endsWith('/polls')) return 'polls'
    if (p.endsWith('/profile')) return 'profile'
    return 'home'
  })

  function groupTabPath(segment) {
    const id = String(group.value?.id || route.params.id || '')
    return segment && segment !== 'home' ? groupsPath(id, segment) : groupsPath(id)
  }

  const tabs = computed(() => {
    const id = group.value?.id || route.params.id
    const rows = [
      { key: 'home', label: '首页', to: groupTabPath('home') },
      { key: 'posts', label: '动态', to: groupTabPath('posts') },
      { key: 'checkin', label: '打卡', to: groupTabPath('checkin') },
      { key: 'tasks', label: '任务', to: groupTabPath('tasks') },
      { key: 'activities', label: '活动', to: groupTabPath('activities') },
      { key: 'members', label: '成员', to: groupTabPath('members') },
      { key: 'articles', label: '文章', to: groupTabPath('articles') },
      { key: 'notices', label: '公告', to: groupTabPath('notices') },
      { key: 'profile', label: '资料', to: groupTabPath('profile') }
    ]
    if (id && !isLegacyPlatformGroupId(id)) {
      rows.splice(6, 0, { key: 'polls', label: '投票', to: groupTabPath('polls') })
    }
    return rows
  })

  const isPollsSupported = computed(() => {
    const id = group.value?.id || route.params.id
    return Boolean(id && !isLegacyPlatformGroupId(id))
  })

  const joinModeKey = computed(() => {
    const g = group.value
    if (!g) return 'audit'
    return g.joinModeKey || (g.joinMode === 'free' ? 'open' : g.joinMode === 'invite' ? 'invite' : 'audit')
  })

  const inviteCodeFromQuery = computed(() => String(route.query.invite || '').trim().toUpperCase())

  const pendingMemberCount = computed(() => Number(group.value?.pendingMemberCount || 0))

  const showSpaceManageLink = computed(() => {
    const g = group.value
    if (!g || !userStore.isLoggedIn) return false
    if (g.managed || g.canReviewJoins) return true
    return userStore.hasPermission('group.manage.all')
  })

  const spaceManageEntry = computed(() => {
    const id = group.value?.id
    if (!id) return groupsPath()
    return resolveSpaceManageEntry(String(id))
  })

  const spaceManageMembersEntry = computed(() => {
    const id = group.value?.id
    if (!id) return groupsPath()
    return resolveSpaceManageEntry(String(id), { tab: 'members' })
  })

  const joinDisabled = computed(() => {
    if (!group.value) return true
    if (group.value.isMember || group.value.hasPendingRequest) return true
    if (joinModeKey.value === 'invite') return !inviteCodeFromQuery.value
    return false
  })

  const joinButtonText = computed(() => {
    if (!userStore.isLoggedIn) return '登录后加入'
    if (group.value?.isMember) return '已加入'
    if (group.value?.hasPendingRequest) return '审核中'
    if (joinModeKey.value === 'invite') {
      return inviteCodeFromQuery.value ? '验证邀请并加入' : '需要邀请码'
    }
    if (joinModeKey.value === 'open') return '加入团体'
    return '申请加入'
  })

  const posts = computed(() => rawPosts.value.map(normalizePost))
  const members = computed(() => rawMembers.value.map(normalizeMember))
  const notices = computed(() => rawNotices.value.map(normalizeNotice))
  const articles = computed(() => rawArticles.value.map(normalizeArticle))
  const claimableCount = computed(() => todayTasks.value.filter(t => t.completed && !t.claimed).length)

  const currentUserIdNum = computed(() => Number(userStore.userInfo?.id || userStore.userId || 0))

  const rankingEmptyHint = computed(() => {
    if (rankingTab.value !== 'daily') return ''
    if (Number(checkinSummary.todayCount || 0) > 0) return ''
    return '今天还没有人打卡，来成为第一个吧'
  })

  const rankingDisplayRows = computed(() => {
    const items = Array.isArray(rankingsPayload.items) ? rankingsPayload.items : []
    const me = rankingsPayload.currentUser
    if (!me || me.rank == null) return items
    const inTop = items.some(r => r.isCurrentUser)
    if (inTop) return items
    return [...items, { ...me, __appendedMe: true }]
  })

  function flash(text, type = 'success') {
    message.value = text
    messageType.value = type
    window.setTimeout(() => { message.value = '' }, 2200)
  }

  function normalizeGroup(item) {
    const jm = item.joinMode
    const key = item.joinModeKey || (jm === 'free' ? 'open' : jm === 'invite' ? 'invite' : 'audit')
    const joinLabel = key === 'open' ? '公开加入' : key === 'invite' ? '仅限邀请' : '审核加入'
    return {
      id: item.id || route.params.id,
      name: item.name || '未命名团体',
      description: item.description || '暂无简介',
      coverUrl: item.coverUrl || DEFAULT_COVER,
      category: item.category || item.typeName || '团体',
      region: item.location || item.region || '未设置',
      memberCount: Number(item.memberCount || 0),
      postCount: Number(item.postCount || 0),
      joinMode: jm,
      joinModeKey: key,
      joinLabel,
      isMember: Boolean(item.isMember),
      managed: Boolean(item.managed),
      canReviewJoins: Boolean(item.canReviewJoins),
      ownerUserId: item.ownerUserId ?? null,
      createdBy: item.createdBy ?? null,
      hasPendingRequest: Boolean(item.hasPendingRequest),
      pendingMemberCount: Number(item.pendingMemberCount || 0),
      myMemberRealName: item.myMemberRealName || ''
    }
  }

  function normalizePost(item) {
    return {
      id: item.id,
      userId: item.userId,
      author: item.authorName || item.username || '成员',
      avatar: item.authorAvatar || item.avatarUrl || DEFAULT_AVATAR,
      content: item.content || '',
      time: formatDateTime(item.createdAt),
      likes: Number(item.likeCount ?? item.likes ?? 0),
      comments: Number(item.commentCount ?? 0),
      likedByMe: Boolean(item.likedByMe)
    }
  }

  function unwrapCommentItems(res) {
    if (Array.isArray(res?.items)) return res.items
    if (Array.isArray(res?.data?.items)) return res.data.items
    if (Array.isArray(res)) return res
    return []
  }

  function normalizeComment(item) {
    return {
      id: item.id,
      userId: item.userId,
      author: item.authorName || '成员',
      avatar: item.authorAvatarUrl || item.authorAvatar || DEFAULT_AVATAR,
      content: item.content || '',
      time: formatDateTime(item.createdAt)
    }
  }

  function commentKey(postId, commentId) {
    return `${postId}-${commentId}`
  }

  function canDeleteComment(c) {
    if (!group.value) return false
    if (group.value.managed) return true
    return String(c.userId) === String(userStore.userId)
  }

  function memberRoleSlug(role) {
    return String(role ?? '').trim().toLowerCase()
  }

  function memberStatusSlug(status) {
    return String(status ?? '').trim().toLowerCase()
  }

  function isCurrentUserGroupOwnerAligned() {
    if (!group.value || !userStore.isLoggedIn) return false
    const uid = currentUserIdNum.value
    if (!uid) return false
    const myRow = rawMembers.value.find(m => Number(m.userId) === uid)
    if (!myRow || memberRoleSlug(myRow.role) !== 'owner') return false

    const g = group.value
    const declared =
      g.ownerUserId !== null && g.ownerUserId !== undefined && `${g.ownerUserId}`.length > 0
        ? Number(g.ownerUserId)
        : null
    const fallback =
      g.createdBy !== null && g.createdBy !== undefined && `${g.createdBy}`.length > 0
        ? Number(g.createdBy)
        : null

    if (declared != null && Number.isFinite(declared) && declared !== uid) return false
    if (declared == null && fallback != null && Number.isFinite(fallback) && fallback !== uid) {
      return false
    }
    return true
  }

  function canOwnerToggleAdmin(member) {
    const mr = memberRoleSlug(member.role)
    const ms = memberStatusSlug(member.status)
    if (ms !== '' && ms !== 'approved') return false
    if (mr !== 'member' && mr !== 'admin') return false
    return isCurrentUserGroupOwnerAligned()
  }

  function normalizeMember(item) {
    const role = item.role || 'member'
    const status = item.status || 'approved'
    const name =
      (item.memberRealName && String(item.memberRealName).trim()) ||
      item.username ||
      (item.isJoinRequest ? '申请者' : '成员')
    return {
      id: item.id || item.userId,
      userId: item.userId,
      joinRequestId: item.joinRequestId,
      isJoinRequest: Boolean(item.isJoinRequest),
      name,
      avatar: item.avatarUrl || item.avatar || DEFAULT_AVATAR,
      role,
      roleLabel:
        role === 'owner' ? '团长' : role === 'admin' ? '管理员' : status === 'pending' ? '待入团' : '成员',
      status,
      statusLabel: status === 'pending' ? '申请中' : '已加入',
      joinedAt: item.joinedAt || formatDate(item.requestedAt) || '',
      applyReason: item.applyReason || ''
    }
  }

  function mapJoinRequestToMemberRow(r) {
    const real = r.memberRealName && String(r.memberRealName).trim()
    return {
      joinRequestId: r.id,
      id: `jr-${r.id}`,
      userId: r.userId,
      username: r.username,
      memberRealName: real,
      avatarUrl: r.avatarUrl,
      role: 'member',
      status: 'pending',
      requestedAt: r.requestedAt,
      applyReason: r.message || '',
      isJoinRequest: true
    }
  }

  function canAuditMember(member) {
    if (member.status !== 'pending') return false
    if (member.isJoinRequest) return Boolean(group.value?.canReviewJoins)
    return Boolean(group.value?.managed)
  }

  function canRemoveMember(member) {
    return group.value?.managed && member.status === 'approved' && member.role !== 'owner'
  }

  function normalizeArticle(item) {
    return {
      id: item.id,
      title: item.title || '未命名文章',
      summary: item.summary || '',
      content: item.content || '',
      coverUrl: item.coverUrl || '',
      authorUserId: item.authorUserId,
      author: item.authorName || '成员',
      date: formatDate(item.createdAt)
    }
  }

  function normalizeNotice(item) {
    return {
      id: item.id,
      title: item.title || '公告',
      content: item.content || '',
      date: formatDate(item.createdAt)
    }
  }

  function unwrapList(res) {
    if (Array.isArray(res)) return res
    if (Array.isArray(res?.data)) return res.data
    return []
  }

  function formatDate(value) {
    if (!value) return ''
    const d = new Date(value)
    return Number.isNaN(d.getTime()) ? '' : d.toISOString().slice(0, 10)
  }

  function formatDateTime(value) {
    if (!value) return '刚刚'
    return String(value).replace('T', ' ').slice(0, 16)
  }

  async function loadDetail() {
    loading.value.detail = true
    errors.detail = ''
    try {
      const data = await fetchGroupDetail(route.params.id)
      group.value = normalizeGroup(data)
      if (userStore.token && (group.value?.managed || group.value?.canReviewJoins)) {
        await userStore.loadAdminContext().catch(() => null)
      }
    } catch (e) {
      group.value = null
      errors.detail = e.message || '加载失败'
    } finally {
      loading.value.detail = false
    }
  }

  async function loadPosts() {
    if (!group.value?.id) return
    loading.value.posts = true
    errors.posts = ''
    try {
      const res = await fetchGroupPosts(group.value.id, { page: 1, size: 40 })
      rawPosts.value = unwrapGroupPostsList(res)
    } catch (e) {
      rawPosts.value = []
      errors.posts = e.message || '动态加载失败'
    } finally {
      loading.value.posts = false
    }
  }

  async function loadMembers() {
    if (!group.value?.id) return
    loading.value.members = true
    errors.members = ''
    try {
      const gid = group.value.id
      const legacy = isLegacyPlatformGroupId(gid)
      const canSeePending = Boolean(group.value?.managed || group.value?.canReviewJoins)
      const status = legacy && canSeePending ? 'all' : 'approved'
      const members = unwrapList(await fetchGroupMembers(gid, { status }))
      let pendingRows = []
      if (!legacy && canSeePending && joinModeKey.value === 'audit') {
        try {
          const reqs = await getAdminGroupJoinRequests(gid, 'pending')
          const list = Array.isArray(reqs) ? reqs : unwrapList(reqs)
          pendingRows = list.map(mapJoinRequestToMemberRow)
        } catch {
          pendingRows = []
        }
      }
      const approvedIds = new Set(
        members.filter(m => (m.status || 'approved') === 'approved').map(m => Number(m.userId))
      )
      pendingRows = pendingRows.filter(p => !approvedIds.has(Number(p.userId)))
      rawMembers.value = [...pendingRows, ...members]
    } catch (e) {
      rawMembers.value = []
      errors.members = e.message || '成员加载失败'
    } finally {
      loading.value.members = false
    }
  }

  async function loadNotices() {
    if (!group.value?.id) return
    try {
      rawNotices.value = unwrapList(await fetchGroupNotices(group.value.id))
    } catch {
      rawNotices.value = []
    }
  }

  async function loadArticles() {
    if (!group.value?.id) return
    try {
      rawArticles.value = unwrapList(await fetchGroupArticles(group.value.id))
    } catch {
      rawArticles.value = []
    }
  }

  async function submitArticle() {
    const title = articleForm.title.trim()
    const content = articleForm.content.trim()
    if (!title || !content || publishingArticle.value) return
    publishingArticle.value = true
    try {
      await createGroupArticle(group.value.id, { title, content })
      articleForm.title = ''
      articleForm.content = ''
      await loadArticles()
      flash('文章已发布')
    } catch (e) {
      flash(e.message || '发布失败', 'error')
    } finally {
      publishingArticle.value = false
    }
  }

  function canDeleteArticle(item) {
    if (!item) return false
    if (group.value?.managed || group.value?.isOwner) return true
    return Number(item.authorUserId) === currentUserIdNum.value
  }

  async function removeArticle(item) {
    if (!canDeleteArticle(item)) return
    if (!window.confirm(`确定删除文章「${item.title}」？`)) return
    deletingArticleId.value = item.id
    try {
      await deleteGroupArticle(group.value.id, item.id)
      await loadArticles()
      flash('文章已删除')
    } catch (e) {
      flash(e.message || '删除失败', 'error')
    } finally {
      deletingArticleId.value = null
    }
  }

  function toggleArticle(id) {
    expandedArticleId.value = expandedArticleId.value === id ? null : id
  }

  async function loadCheckinSummary() {
    if (!group.value?.id) return
    loading.value.checkin = true
    try {
      const data = await fetchCheckinSummary(group.value.id)
      Object.assign(checkinSummary, {
        checkedInToday: Boolean(data?.checkedInToday),
        todayCount: Number(data?.todayCount ?? 0),
        myStreakDays: Number(data?.myStreakDays ?? 0),
        recentCheckins: Array.isArray(data?.recentCheckins) ? data.recentCheckins : []
      })
    } catch {
      /* silent */
    } finally {
      loading.value.checkin = false
    }
  }

  async function loadTodayTasks() {
    if (!group.value?.id || !userStore.isLoggedIn) return
    loading.value.tasks = true
    try {
      const data = await fetchTodayTasks(group.value.id)
      todayTasks.value = Array.isArray(data) ? data : []
    } catch {
      todayTasks.value = []
    } finally {
      loading.value.tasks = false
    }
  }

  async function loadSeasonInfo() {
    if (!group.value?.id || !isLegacyPlatformGroupId(group.value.id)) {
      seasonRank.value = null
      seasonTop.value = []
      return
    }
    try {
      const [rankRes, topRes] = await Promise.all([
        fetchGroupSeasonRank(group.value.id),
        fetchGroupSeasonRankings({ page: 1, size: 5 })
      ])
      seasonRank.value = rankRes
        ? { ...rankRes, scoringRules: rankRes.scoringRules || topRes?.scoringRules }
        : null
      seasonTop.value = Array.isArray(topRes?.items) ? topRes.items : []
    } catch {
      seasonRank.value = null
      seasonTop.value = []
    }
  }

  async function loadRankings() {
    if (!group.value?.id || !userStore.isLoggedIn || !group.value.isMember) return
    loadingRankings.value = true
    try {
      const res = await fetchCheckinRankings(group.value.id, rankingTab.value)
      rankingsPayload.items = Array.isArray(res?.items) ? res.items : []
      rankingsPayload.currentUser =
        res?.currentUser && Object.keys(res.currentUser).length ? res.currentUser : null
    } catch {
      rankingsPayload.items = []
      rankingsPayload.currentUser = null
    } finally {
      loadingRankings.value = false
    }
  }

  function setRankingTab(tab) {
    if (rankingTab.value === tab) return
    rankingTab.value = tab
    loadRankings()
  }

  function rankMedal(index) {
    if (index === 0) return '🥇'
    if (index === 1) return '🥈'
    if (index === 2) return '🥉'
    return String(index + 1)
  }

  function rankSlotDisplay(row, idx) {
    if (!row.__appendedMe && idx < 3) return rankMedal(idx)
    return String(row.rank ?? idx + 1)
  }

  function visibleCommentsFor(checkinId) {
    const all = checkinCommentsById[checkinId] || []
    if (checkinCommentsShowAll[checkinId]) return all
    return all.slice(0, 3)
  }

  function hasMoreComments(checkinId) {
    const all = checkinCommentsById[checkinId] || []
    return all.length > 3 && !checkinCommentsShowAll[checkinId]
  }

  function showAllCommentsFor(checkinId) {
    checkinCommentsShowAll[checkinId] = true
  }

  async function loadCheckinCommentsList(checkinId) {
    checkinCommentsLoading[checkinId] = true
    try {
      const res = await fetchPlatformCheckinComments(checkinId, { page: 1, size: 50 })
      checkinCommentsById[checkinId] = Array.isArray(res?.items) ? res.items : []
    } catch {
      checkinCommentsById[checkinId] = []
    } finally {
      checkinCommentsLoading[checkinId] = false
    }
  }

  function toggleCommentsBlock(c) {
    const id = c.id
    if (expandedCheckinId.value === id) {
      expandedCheckinId.value = null
      return
    }
    expandedCheckinId.value = id
    checkinCommentsShowAll[id] = false
    if (!checkinCommentsById[id]) loadCheckinCommentsList(id)
  }

  async function toggleLikeCheckin(c) {
    if (!userStore.isLoggedIn || !group.value?.isMember || checkinLikeBusyId.value) return
    const id = c.id
    checkinLikeBusyId.value = id
    try {
      if (c.likedByCurrentUser) {
        const res = await unlikePlatformCheckin(id)
        c.likedByCurrentUser = false
        c.likeCount = Number(res?.likeCount ?? 0)
      } else {
        const res = await likePlatformCheckin(id)
        c.likedByCurrentUser = true
        c.likeCount = Number(res?.likeCount ?? 0)
      }
    } catch (e) {
      flash(e.message || '操作失败', 'error')
    } finally {
      checkinLikeBusyId.value = null
    }
  }

  async function submitCheckinComment(c) {
    const id = c.id
    const text = String(checkinCommentDraft[id] || '').trim()
    if (!text || checkinCommentPosting.value) return
    checkinCommentPosting.value = true
    try {
      await createPlatformCheckinComment(id, { content: text })
      checkinCommentDraft[id] = ''
      c.commentCount = Number(c.commentCount || 0) + 1
      const wasAll = Boolean(checkinCommentsShowAll[id])
      await loadCheckinCommentsList(id)
      if (wasAll) checkinCommentsShowAll[id] = true
    } catch (e) {
      flash(e.message || '评论失败', 'error')
    } finally {
      checkinCommentPosting.value = false
    }
  }

  async function deleteMyCheckinComment(checkinId, commentId) {
    try {
      await deletePlatformCheckinComment(commentId)
      const list = checkinCommentsById[checkinId] || []
      const idx = list.findIndex(x => x.id === commentId)
      if (idx >= 0) list.splice(idx, 1)
      const c = checkinSummary.recentCheckins?.find(x => x.id === checkinId)
      if (c) c.commentCount = Math.max(0, Number(c.commentCount || 0) - 1)
    } catch (e) {
      flash(e.message || '删除失败', 'error')
    }
  }

  async function approvePendingMember(member) {
    if (!canAuditMember(member) || moderatingMemberId.value) return
    moderatingMemberId.value = member.id
    try {
      const res = member.isJoinRequest
        ? await approveAdminGroupRequest(group.value.id, member.joinRequestId)
        : await approveMember(group.value.id, member.id)
      await loadDetail()
      await loadMembers()
      flash(res?.message || '已通过申请')
    } catch (e) {
      flash(e.message || '审核通过失败', 'error')
    } finally {
      moderatingMemberId.value = null
    }
  }

  async function rejectPendingMember(member) {
    if (!canAuditMember(member) || moderatingMemberId.value) return
    moderatingMemberId.value = member.id
    try {
      const res = member.isJoinRequest
        ? await rejectAdminGroupRequest(group.value.id, member.joinRequestId)
        : await rejectMember(group.value.id, member.id)
      await loadDetail()
      await loadMembers()
      flash(res?.message || '已拒绝申请')
    } catch (e) {
      flash(e.message || '拒绝申请失败', 'error')
    } finally {
      moderatingMemberId.value = null
    }
  }

  async function removeMember(member) {
    if (!canRemoveMember(member) || removingMemberId.value) return
    if (!window.confirm(`确定将「${member.name}」移出团体？`)) return
    removingMemberId.value = member.id
    try {
      await removeGroupMember(group.value.id, member.id)
      await loadDetail()
      await loadMembers()
      flash('已移除成员')
    } catch (e) {
      flash(e.message || '移除失败', 'error')
    } finally {
      removingMemberId.value = null
    }
  }

  function formatActivityTime(value) {
    if (!value) return ''
    const d = new Date(value)
    if (Number.isNaN(d.getTime())) return String(value)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }

  function activityStatusLabel(act) {
    if (act.status === 'cancelled') return '已取消'
    if (act.isEnded) return '已结束'
    const start = act.startTime ? new Date(act.startTime).getTime() : 0
    if (start > Date.now()) return '即将开始'
    return '进行中'
  }

  function activityStatusClass(act) {
    if (act.status === 'cancelled') return 'cancelled'
    if (act.isEnded) return 'ended'
    return 'live'
  }

  async function loadActivities() {
    if (!group.value?.id) return
    loading.value.activities = true
    try {
      const res = await fetchGroupActivities(group.value.id, { page: 1, size: 50 })
      activities.value = Array.isArray(res?.items) ? res.items : (Array.isArray(res) ? res : [])
    } catch {
      activities.value = []
    } finally {
      loading.value.activities = false
    }
  }

  async function loadUpcomingActivities() {
    if (!group.value?.id) return
    try {
      const res = await fetchGroupActivities(group.value.id, { filter: 'upcoming', page: 1, size: 3 })
      upcomingActivities.value = Array.isArray(res?.items) ? res.items : (Array.isArray(res) ? res : [])
    } catch {
      upcomingActivities.value = []
    }
  }

  async function loadPolls() {
    if (!group.value?.id || !isPollsSupported.value) {
      polls.value = []
      return
    }
    loading.value.polls = true
    try {
      const res = await fetchGroupPolls(group.value.id, { page: 1, size: 50 })
      polls.value = Array.isArray(res?.items) ? res.items : []
    } catch {
      polls.value = []
    } finally {
      loading.value.polls = false
    }
  }

  async function refreshPollDetail(id) {
    if (!group.value?.id) return
    const d = await fetchGroupPoll(group.value.id, id)
    pollDetailCache[id] = d
    pollPick[id] = Array.isArray(d.mySelections) ? [...d.mySelections] : []
  }

  async function togglePollExpand(id) {
    if (expandedPollId.value === id) {
      expandedPollId.value = null
      return
    }
    expandedPollId.value = id
    try {
      await refreshPollDetail(id)
    } catch (e) {
      flash(e.message || '加载失败', 'error')
      expandedPollId.value = null
    }
  }

  function setPollSinglePick(pollId, optId) {
    pollPick[pollId] = [optId]
  }

  function togglePollMultiPick(pollId, optId, detail, checked) {
    const max = Number(detail.maxChoices || 2)
    let cur = [...(pollPick[pollId] || [])]
    if (checked) {
      if (!cur.includes(optId) && cur.length < max) cur.push(optId)
    } else {
      cur = cur.filter(x => x !== optId)
    }
    pollPick[pollId] = cur
  }

  async function submitPollVote(pollId) {
    const pick = pollPick[pollId] || []
    if (!pick.length) {
      flash('请选择选项', 'error')
      return
    }
    if (votingPollId.value) return
    votingPollId.value = pollId
    try {
      await submitGroupPollVotes(group.value.id, pollId, { optionIds: pick })
      await refreshPollDetail(pollId)
      await loadPolls()
      flash('投票已提交')
    } catch (e) {
      flash(e.message || '提交失败', 'error')
    } finally {
      votingPollId.value = null
    }
  }

  async function revealPollResults(pollId) {
    if (revealingPollId.value) return
    revealingPollId.value = pollId
    try {
      await revealGroupPollResults(group.value.id, pollId)
      await refreshPollDetail(pollId)
      await loadPolls()
      flash('已向全体成员展示票数')
    } catch (e) {
      flash(e.message || '操作失败', 'error')
    } finally {
      revealingPollId.value = null
    }
  }

  async function submitPollCreate() {
    if (!group.value?.id || creatingPoll.value) return
    creatingPoll.value = true
    try {
      const lines = pollForm.optionsText.split(/\r?\n/).map(s => s.trim()).filter(Boolean)
      if (lines.length < 2) {
        flash('请至少填写两个选项', 'error')
        return
      }
      const payload = {
        title: pollForm.title,
        description: pollForm.description,
        selectionMode: pollForm.selectionMode,
        startTime: pollForm.startTime ? `${pollForm.startTime}:00` : '',
        endTime: pollForm.endTime ? `${pollForm.endTime}:00` : '',
        options: lines
      }
      if (pollForm.selectionMode === 'multiple') {
        payload.maxChoices = pollForm.maxChoices
      }
      await createGroupPoll(group.value.id, payload)
      showPollCreate.value = false
      Object.assign(pollForm, {
        title: '',
        description: '',
        selectionMode: 'single',
        maxChoices: 2,
        startTime: '',
        endTime: '',
        optionsText: ''
      })
      expandedPollId.value = null
      await loadPolls()
      flash('投票已发布')
    } catch (e) {
      flash(e.message || '发布失败', 'error')
    } finally {
      creatingPoll.value = false
    }
  }

  async function signUpActivity(act) {
    if (!userStore.isLoggedIn) {
      router.push('/login')
      return
    }
    signingActivityId.value = act.id
    try {
      await signUpGroupActivity(group.value.id, act.id)
      await loadActivities()
      await loadUpcomingActivities()
      flash('报名成功')
    } catch (e) {
      flash(e.message || '报名失败', 'error')
    } finally {
      signingActivityId.value = null
    }
  }

  async function cancelSignUpActivity(act) {
    signingActivityId.value = act.id
    try {
      await cancelGroupActivitySignup(group.value.id, act.id)
      await loadActivities()
      await loadUpcomingActivities()
      flash('已取消报名')
    } catch (e) {
      flash(e.message || '取消失败', 'error')
    } finally {
      signingActivityId.value = null
    }
  }

  async function cancelActivity(act) {
    if (!window.confirm(`确定取消活动「${act.title}」？`)) return
    cancellingActivityId.value = act.id
    try {
      await updateGroupActivity(group.value.id, act.id, { status: 'cancelled' })
      await loadActivities()
      await loadUpcomingActivities()
      flash('活动已取消')
    } catch (e) {
      flash(e.message || '操作失败', 'error')
    } finally {
      cancellingActivityId.value = null
    }
  }

  async function loadPlatformEvents() {
    try {
      const res = await fetchEvents({ status: 'published' })
      platformEvents.value = Array.isArray(res) ? res.slice(0, 20) : []
    } catch {
      platformEvents.value = []
    }
  }

  async function loadWeeklyDigest() {
    if (!group.value?.managed || !isLegacyPlatformGroupId(group.value?.id)) {
      weeklyDigest.value = null
      return
    }
    try {
      weeklyDigest.value = await fetchGroupWeeklyDigest(group.value.id)
    } catch {
      weeklyDigest.value = null
    }
  }

  async function sendWeeklyDigestNow() {
    if (sendingDigest.value || !group.value?.id) return
    sendingDigest.value = true
    try {
      await sendGroupWeeklyDigest(group.value.id)
      flash('周报已发送')
    } catch (e) {
      flash(e.message || '发送失败', 'error')
    } finally {
      sendingDigest.value = false
    }
  }

  function openPostReport(post) {
    reportPostId.value = post.id
    reportTargetUserId.value = post.userId || null
    reportDialogOpen.value = true
  }

  function openActivityCheckin(act) {
    activityCheckinTarget.value = act
    activityCheckinCode.value = ''
    activityCheckinOpen.value = true
  }

  async function submitActivityCheckin() {
    if (!activityCheckinTarget.value || activityCheckinSubmitting.value) return
    activityCheckinSubmitting.value = true
    try {
      await checkinGroupActivity(group.value.id, activityCheckinTarget.value.id, activityCheckinCode.value)
      activityCheckinOpen.value = false
      await loadActivities()
      flash('签到成功')
    } catch (e) {
      flash(e.message || '签到失败', 'error')
    } finally {
      activityCheckinSubmitting.value = false
    }
  }

  async function generateActivityCode(act) {
    if (checkinCodeBusyId.value) return
    checkinCodeBusyId.value = act.id
    try {
      const res = await generateGroupActivityCheckinCode(group.value.id, act.id)
      flash(`签到码：${res.checkinCode || '已生成'}`)
      await loadActivities()
    } catch (e) {
      flash(e.message || '生成失败', 'error')
    } finally {
      checkinCodeBusyId.value = null
    }
  }

  async function openActivityReview(act) {
    activityReviewTarget.value = act
    activityReviewOpen.value = true
    activityReviewLoading.value = true
    try {
      activityReviewCandidates.value = await fetchGroupActivityReviewCandidates(group.value.id, act.id)
    } catch (e) {
      activityReviewCandidates.value = []
      flash(e.message || '加载失败', 'error')
      activityReviewOpen.value = false
    } finally {
      activityReviewLoading.value = false
    }
  }

  function closeActivityReview() {
    activityReviewOpen.value = false
    activityReviewTarget.value = null
    activityReviewCandidates.value = []
  }

  async function submitOneReview(candidate) {
    const rating = activityReviewRatings[candidate.userId] || 5
    try {
      await submitGroupActivityReview(group.value.id, activityReviewTarget.value.id, {
        targetUserId: candidate.userId,
        rating,
        comment: ''
      })
      candidate.reviewed = true
      await loadActivities()
      flash('评价已提交')
    } catch (e) {
      flash(e.message || '提交失败', 'error')
    }
  }

  async function submitActivity() {
    if (!group.value?.id || creatingActivity.value) return
    creatingActivity.value = true
    try {
      const payload = {
        title: activityForm.title,
        description: activityForm.description,
        startTime: activityForm.startTime ? `${activityForm.startTime}:00` : '',
        endTime: activityForm.endTime ? `${activityForm.endTime}:00` : '',
        location: activityForm.location,
        maxParticipants: activityForm.maxParticipants
      }
      if (activityForm.platformEventId) payload.platformEventId = activityForm.platformEventId
      await createGroupActivity(group.value.id, payload)
      showCreateActivity.value = false
      Object.assign(activityForm, {
        title: '',
        description: '',
        startTime: '',
        endTime: '',
        location: '',
        maxParticipants: 0,
        platformEventId: ''
      })
      await loadActivities()
      await loadUpcomingActivities()
      flash('活动已发布')
    } catch (e) {
      flash(e.message || '发布失败', 'error')
    } finally {
      creatingActivity.value = false
    }
  }

  function handleProposeActivity() {
    if (group.value?.managed) {
      flash('你是运营者，可前往运营台直接发布活动。', 'error')
      return
    }
    if (!group.value?.isMember || !userStore.isLoggedIn) return
    showProposalDialog.value = true
  }

  function closeProposalDialog() {
    showProposalDialog.value = false
  }

  function onProposalSubmitted() {
    flash('活动投稿已提交，等待审核')
  }

  async function loadRelated() {
    if (!group.value?.id) return
    await Promise.all([
      loadPosts(),
      loadMembers(),
      loadNotices(),
      loadArticles(),
      loadCheckinSummary(),
      loadTodayTasks(),
      loadUpcomingActivities(),
      loadSeasonInfo(),
      loadWeeklyDigest()
    ])
  }

  watch(showCreateActivity, (open) => {
    if (open && !platformEvents.value.length) loadPlatformEvents()
  })

  async function applyJoin() {
    if (joinDisabled.value || joining.value) return
    if (!userStore.isLoggedIn) {
      userStore.setPostLoginRedirect(route.fullPath.replace(/^#/, '') || groupsPath(String(route.params.id)))
      router.push('/login')
      return
    }
    const nameInput = window.prompt(JOIN_MEMBER_REAL_NAME_PROMPT, '')
    if (nameInput === null) return
    const memberRealName = String(nameInput).trim()
    if (!memberRealName) {
      flash(ERR_EMPTY_MEMBER_REAL_NAME, 'error')
      return
    }
    if (joinModeKey.value === 'invite' && !inviteCodeFromQuery.value) {
      flash('请通过团长分享的邀请链接或邀请码加入', 'error')
      return
    }
    let applyMessage = ''
    if (joinModeKey.value === 'audit') {
      const input = window.prompt(AUDIT_JOIN_MESSAGE_PROMPT, '')
      if (input === null) return
      applyMessage = input.trim()
      if (!applyMessage) {
        flash(ERR_EMPTY_AUDIT_JOIN_MESSAGE, 'error')
        return
      }
    }
    joining.value = true
    try {
      const res = await joinGroup(group.value.id, {
        message: applyMessage,
        memberRealName,
        inviteCode: inviteCodeFromQuery.value
      })
      group.value.isMember = Boolean(res?.joined)
      group.value.hasPendingRequest = Boolean(res?.pending)
      await loadDetail()
      await loadRelated()
      flash(res?.message || '已提交')
    } catch (e) {
      flash(e.message || '加入失败', 'error')
    } finally {
      joining.value = false
    }
  }

  async function submitPost() {
    const text = postDraft.value.trim()
    if (!text || posting.value) return
    posting.value = true
    try {
      await createGroupPost(group.value.id, { content: text })
      postDraft.value = ''
      await loadPosts()
      flash('发布成功')
    } catch (e) {
      flash(e.message || '发布失败', 'error')
    } finally {
      posting.value = false
    }
  }

  async function likePost(post) {
    if (!userStore.isLoggedIn) {
      router.push('/login')
      return
    }
    try {
      const res = await toggleGroupPostLike(group.value.id, post.id)
      const row = rawPosts.value.find(p => p.id === post.id)
      if (row) {
        row.likedByMe = Boolean(res?.liked ?? res?.likedByMe)
        row.likeCount = Number(res?.likeCount ?? row.likeCount ?? 0)
      }
    } catch {
      /* ignore */
    }
  }

  async function toggleCommentPanel(post) {
    if (expandedPostId.value === post.id) {
      expandedPostId.value = null
      return
    }
    expandedPostId.value = post.id
    if (!feedComments[post.id]) {
      await loadCommentsForPost(post.id)
    }
  }

  async function loadCommentsForPost(postId) {
    commentsLoading[postId] = true
    try {
      const res = await fetchGroupPostComments(group.value.id, postId, { page: 1, size: 50 })
      feedComments[postId] = unwrapCommentItems(res).map(normalizeComment)
    } catch (e) {
      feedComments[postId] = []
      flash(e.message || '评论加载失败', 'error')
    } finally {
      commentsLoading[postId] = false
    }
  }

  async function submitComment(post) {
    const key = post.id
    const text = (commentDraft[key] || '').trim()
    if (!text || commentPosting[key]) return
    commentPosting[key] = true
    try {
      await createGroupPostComment(group.value.id, post.id, { content: text })
      commentDraft[key] = ''
      await loadCommentsForPost(post.id)
      await loadPosts()
      flash('评论已发布')
    } catch (e) {
      flash(e.message || '评论失败', 'error')
    } finally {
      commentPosting[key] = false
    }
  }

  async function deleteComment(post, c) {
    if (!canDeleteComment(c) || deletingCommentKey.value) return
    if (!window.confirm('删除这条评论？')) return
    deletingCommentKey.value = commentKey(post.id, c.id)
    try {
      await deletePlatformGroupPostComment(group.value.id, post.id, c.id)
      await loadCommentsForPost(post.id)
      await loadPosts()
    } catch (e) {
      flash(e.message || '删除失败', 'error')
    } finally {
      deletingCommentKey.value = ''
    }
  }

  async function setMemberAsAdmin(member) {
    if (!canOwnerToggleAdmin(member) || member.role !== 'member' || roleChangingMemberId.value) return
    roleChangingMemberId.value = member.id
    try {
      await patchPlatformGroupMemberRole(group.value.id, member.id, { role: 'admin' })
      await loadDetail()
      await loadMembers()
      flash('已设为管理员')
    } catch (e) {
      flash(e.message || '设置失败', 'error')
    } finally {
      roleChangingMemberId.value = null
    }
  }

  async function unsetMemberAdmin(member) {
    if (!canOwnerToggleAdmin(member) || member.role !== 'admin' || roleChangingMemberId.value) return
    roleChangingMemberId.value = member.id
    try {
      await patchPlatformGroupMemberRole(group.value.id, member.id, { role: 'member' })
      await loadDetail()
      await loadMembers()
      flash('已取消管理员')
    } catch (e) {
      flash(e.message || '操作失败', 'error')
    } finally {
      roleChangingMemberId.value = null
    }
  }

  async function submitCheckin() {
    if (!checkinForm.type || checkingIn.value) return
    checkingIn.value = true
    try {
      await createCheckin(group.value.id, {
        checkinType: checkinForm.type,
        content: checkinForm.content
      })
      checkinForm.content = ''
      await loadCheckinSummary()
      await loadTodayTasks()
      await loadRankings()
      flash('打卡成功')
    } catch (e) {
      flash(e.message || '打卡失败', 'error')
    } finally {
      checkingIn.value = false
    }
  }

  async function claimTask(task) {
    if (claimingTask.value) return
    claimingTask.value = task.taskCode
    try {
      const res = await claimGroupTask(group.value.id, task.taskCode)
      await loadTodayTasks()
      flash(res?.message || `已领取 +${task.rewardExp} 经验`)
    } catch (e) {
      flash(e.message || '领取失败', 'error')
    } finally {
      claimingTask.value = ''
    }
  }

  watch(() => route.params.id, async () => {
    await loadDetail()
    await loadRelated()
  })

  watch(activeTab, async (tab) => {
    if (!group.value?.id) return
    if (tab === 'checkin') {
      await loadCheckinSummary()
      await loadRankings()
    } else if (tab === 'tasks') await loadTodayTasks()
    else if (tab === 'activities') await loadActivities()
    else if (tab === 'polls') await loadPolls()
    else if (tab === 'members') await loadMembers()
  })

  onMounted(async () => {
    await loadDetail()
    await loadRelated()
    if (activeTab.value === 'activities') await loadActivities()
    else if (activeTab.value === 'polls') await loadPolls()
    else if (activeTab.value === 'checkin') await loadRankings()
  })

  return {
    CHECKIN_TYPES,
    CHECKIN_TYPE_LABELS,
    group,
    loading,
    errors,
    message,
    messageType,
    tabs,
    activeTab,
    groupTabPath,
    groupsPath,
    joinModeKey,
    inviteCodeFromQuery,
    pendingMemberCount,
    showSpaceManageLink,
    spaceManageEntry,
    spaceManageMembersEntry,
    joinDisabled,
    joinButtonText,
    joining,
    posting,
    checkingIn,
    claimingTask,
    postDraft,
    checkinForm,
    checkinSummary,
    todayTasks,
    claimableCount,
    posts,
    members,
    notices,
    articles,
    articleForm,
    publishingArticle,
    expandedArticleId,
    deletingArticleId,
    submitArticle,
    canDeleteArticle,
    removeArticle,
    toggleArticle,
    activities,
    upcomingActivities,
    polls,
    isPollsSupported,
    seasonRank,
    seasonTop,
    rankingTab,
    loadingRankings,
    rankingEmptyHint,
    rankingDisplayRows,
    expandedCheckinId,
    checkinCommentsById,
    checkinCommentsLoading,
    checkinCommentDraft,
    checkinCommentPosting,
    checkinLikeBusyId,
    currentUserIdNum,
    moderatingMemberId,
    removingMemberId,
    roleChangingMemberId,
    expandedPostId,
    feedComments,
    commentsLoading,
    commentDraft,
    commentPosting,
    deletingCommentKey,
    signingActivityId,
    cancellingActivityId,
    showCreateActivity,
    showProposalDialog,
    creatingActivity,
    activityForm,
    expandedPollId,
    pollDetailCache,
    pollPick,
    votingPollId,
    revealingPollId,
    showPollCreate,
    creatingPoll,
    pollForm,
    formatActivityTime,
    formatDateTime,
    activityStatusLabel,
    activityStatusClass,
    canAuditMember,
    canRemoveMember,
    canOwnerToggleAdmin,
    canDeleteComment,
    commentKey,
    userStore,
    applyJoin,
    submitPost,
    likePost,
    toggleCommentPanel,
    submitComment,
    deleteComment,
    submitCheckin,
    claimTask,
    signUpActivity,
    cancelSignUpActivity,
    cancelActivity,
    submitActivity,
    handleProposeActivity,
    closeProposalDialog,
    onProposalSubmitted,
    loadActivities,
    setRankingTab,
    rankSlotDisplay,
    toggleLikeCheckin,
    toggleCommentsBlock,
    visibleCommentsFor,
    hasMoreComments,
    showAllCommentsFor,
    submitCheckinComment,
    deleteMyCheckinComment,
    approvePendingMember,
    rejectPendingMember,
    removeMember,
    setMemberAsAdmin,
    unsetMemberAdmin,
    togglePollExpand,
    setPollSinglePick,
    togglePollMultiPick,
    submitPollVote,
    revealPollResults,
    submitPollCreate,
    loadDetail,
    platformEvents,
    weeklyDigest,
    sendingDigest,
    sendWeeklyDigestNow,
    reportDialogOpen,
    reportPostId,
    reportTargetUserId,
    openPostReport,
    activityCheckinOpen,
    activityCheckinCode,
    activityCheckinSubmitting,
    openActivityCheckin,
    submitActivityCheckin,
    checkinCodeBusyId,
    generateActivityCode,
    activityReviewOpen,
    activityReviewTarget,
    activityReviewCandidates,
    activityReviewLoading,
    activityReviewRatings,
    openActivityReview,
    closeActivityReview,
    submitOneReview
  }
}
