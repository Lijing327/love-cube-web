<template>
  <section class="positive-page">

    <!-- ═══ Hero ═══ -->
    <section class="hero" :class="{ 'hero--dragon-boat': campaign.active }">
      <div class="hero-content">
        <p v-if="campaign.pageKicker" class="hero-kicker">{{ campaign.active ? '🫔 ' : '' }}{{ campaign.pageKicker }}</p>
        <h1 class="hero-title">{{ campaign.pageTitle }}</h1>
        <p class="hero-sub">{{ campaign.pageSubtitle }}</p>
        <div v-if="dailyTopic" class="hero-topic">
          <span class="hero-topic-label">今日话题</span>
          <p class="hero-topic-text">{{ dailyTopic.topicText }}</p>
          <p v-if="dailyTopic.hintText" class="hero-topic-hint">{{ dailyTopic.hintText }}</p>
        </div>
        <router-link
          v-if="campaignTopicLink"
          :to="campaignTopicLink.to"
          class="hero-topic-link"
        >
          {{ campaignTopicLink.label }} →
        </router-link>
        <button type="button" class="hero-btn" @click="scrollToEditor">✏ 写下今日心声</button>
      </div>
      <div class="hero-art" aria-hidden="true">
        <div class="art-glow"></div>
        <div class="art-cup"></div>
        <div class="art-steam art-steam-1"></div>
        <div class="art-steam art-steam-2"></div>
        <div class="art-stem art-stem-1"></div>
        <div class="art-stem art-stem-2"></div>
        <div class="art-bloom art-bloom-1"></div>
        <div class="art-bloom art-bloom-2"></div>
      </div>
    </section>

    <section v-if="campaignBannerLink" class="campaign-strip">
      <div class="campaign-strip-main">
        <span class="campaign-strip-kicker">🌿 {{ campaignBannerLink.kicker }}</span>
        <p class="campaign-strip-text">{{ campaignBannerLink.text }}</p>
      </div>
      <router-link
        v-if="campaignBannerLink.link"
        :to="campaignBannerLink.link"
        class="campaign-strip-link"
      >
        {{ campaignBannerLink.linkLabel }}
      </router-link>
    </section>

    <!-- ═══ Stats Band ═══ -->
    <section class="stats-band">
      <article class="stat-card">
        <div class="stat-icon stat-icon--orange">💬</div>
        <div class="stat-body">
          <div class="stat-num-row">
            <strong class="stat-num">{{ todayCount }}</strong>
            <small class="stat-unit">条</small>
          </div>
          <span class="stat-label">今日分享</span>
          <span class="stat-trend">比昨日 +{{ Math.max(0, todayCount - 3) }}</span>
        </div>
      </article>
      <article class="stat-card">
        <div class="stat-icon stat-icon--pink">❤</div>
        <div class="stat-body">
          <div class="stat-num-row">
            <strong class="stat-num">{{ totalEncourage }}</strong>
            <small class="stat-unit">次</small>
          </div>
          <span class="stat-label">收到鼓励</span>
          <span class="stat-trend">比昨日 +{{ Math.max(0, totalEncourage - 10) }}</span>
        </div>
      </article>
      <article class="stat-card">
        <div class="stat-icon stat-icon--blue">👥</div>
        <div class="stat-body">
          <div class="stat-num-row">
            <strong class="stat-num">{{ activeAuthors }}</strong>
            <small class="stat-unit">人</small>
          </div>
          <span class="stat-label">连续分享人数</span>
          <span class="stat-trend">连续 3 天及以上</span>
        </div>
      </article>
    </section>

    <!-- ═══ Content Layout ═══ -->
    <div class="content-layout">

      <!-- Main Column -->
      <div class="main-column">

        <!-- Editor -->
        <div ref="editorRef">
          <PositiveShareEditor @published="handlePublished" />
        </div>

        <!-- List Card -->
        <section class="list-card">
          <!-- Tab bar -->
          <div class="tab-bar">
            <nav class="main-tabs">
              <button
                v-for="tab in mainTabs"
                :key="tab.value"
                type="button"
                :class="['tab-btn', { active: activeTab === tab.value }]"
                @click="switchTab(tab.value)"
              >{{ tab.label }}</button>
            </nav>
            <nav class="cat-filter">
              <button
                v-for="cat in filterCats"
                :key="cat"
                type="button"
                :class="['filter-btn', { active: activeCategory === cat }]"
                @click="toggleCategory(cat)"
              >{{ cat }}</button>
            </nav>
            <button type="button" class="filter-icon-btn" title="筛选">⇅ 筛选</button>
          </div>

          <!-- Share List -->
          <div class="share-list">
            <div v-if="loading" class="loading-tip">加载中...</div>
            <template v-else>
              <div v-for="item in visibleList" :key="item.id" class="share-item">
                <PositiveShareCard
                  :item="item"
                  :comments-open="expandedShareId === item.id"
                  @like="handleLike"
                  @bookmark="handleBookmark"
                  @comment="openComment"
                  @toggle-comments="toggleComments"
                />
                <div v-if="expandedShareId === item.id" class="comment-section">
                  <p v-if="commentsLoading" class="comment-loading">加载中...</p>
                  <p v-else-if="!comments.length" class="comment-empty">暂无评论，欢迎留下第一句鼓励 ✨</p>
                  <article v-for="comment in comments" :key="comment.id" class="comment-row">
                    <div class="comment-avatar">
                      <img
                        v-if="comment._hasAvatar && !isCommentAvatarError(comment.id)"
                        :src="comment.avatar"
                        :alt="`${comment.username || '用户'}头像`"
                        class="comment-avatar-img"
                        @error="markCommentAvatarError(comment.id)"
                      >
                      <span v-else>{{ commentInitial(comment.username) }}</span>
                    </div>
                    <div class="comment-main">
                      <strong class="comment-author">{{ comment.username || '用户' }}</strong>
                      <p class="comment-text">{{ comment.content }}</p>
                      <time class="comment-time">{{ formatDate(comment.createdAt) }}</time>
                    </div>
                  </article>
                </div>
              </div>

              <div v-if="!visibleList.length" class="list-empty">
                <div class="empty-icon">🌱</div>
                <p class="empty-title">今天还没有心声</p>
                <p class="empty-hint">成为第一个分享正能量的人吧</p>
              </div>
            </template>
          </div>

          <!-- Load More -->
          <div v-if="hasMore && !loading" class="load-more">
            <button type="button" class="load-more-btn" :disabled="loadingMore" @click="loadMore">
              {{ loadingMore ? '加载中...' : '— 查看更多 —' }}
            </button>
          </div>

          <!-- Bottom divider -->
          <p v-if="!hasMore && visibleList.length" class="list-end">— 我是有底线的 —</p>
        </section>
      </div>

      <!-- ═══ Side Panel ═══ -->
      <aside class="side-panel">

        <!-- My Data -->
        <section class="side-card my-data-card">
          <h3 class="side-title">我的心声数据</h3>
          <ul class="my-data-list">
            <li class="my-data-item">
              <span class="my-data-icon" style="background:#fff7f0;color:#f97316">✎</span>
              <span class="my-data-label">我发布的心声</span>
              <span class="my-data-val">{{ myShareCount }} <em>条</em></span>
            </li>
            <li class="my-data-item">
              <span class="my-data-icon" style="background:#fff0f3;color:#e11d48">❤</span>
              <span class="my-data-label">我收到的鼓励</span>
              <span class="my-data-val">{{ myEncourageCount }} <em>次</em></span>
            </li>
            <li class="my-data-item">
              <span class="my-data-icon" style="background:#fffbeb;color:#d97706">★</span>
              <span class="my-data-label">我的心声收藏</span>
              <span class="my-data-val">{{ myFavoriteCount }} <em>条</em></span>
            </li>
            <li class="my-data-item">
              <span class="my-data-icon" style="background:#f0fdf4;color:#16a34a">🌿</span>
              <span class="my-data-label">连续分享天数</span>
              <span class="my-data-val">
                {{ streakDays }} <em>天</em>
                <span v-if="streakDays > 0" class="streak-cheer">继续加油哦！</span>
              </span>
            </li>
          </ul>
        </section>

        <!-- Streak Calendar -->
        <section class="side-card calendar-card">
          <h3 class="side-title">连续分享日历</h3>
          <div class="calendar-grid">
            <span v-for="d in weekDays" :key="d" class="cal-head">{{ d }}</span>
            <span
              v-for="cell in calendarCells"
              :key="cell.key"
              :class="['cal-cell', { checked: cell.hasShare, today: cell.isToday, empty: cell.isEmpty }]"
            >
              <span v-if="cell.hasShare" class="cal-check">✓</span>
            </span>
          </div>
        </section>

        <!-- Warm Note -->
        <section class="side-card warm-note-card">
          <div class="warm-note-art" aria-hidden="true">
            <div class="plant-pot"></div>
            <div class="plant-stem"></div>
            <div class="plant-leaf plant-leaf-l"></div>
            <div class="plant-leaf plant-leaf-r"></div>
          </div>
          <p class="warm-note-text">保持一颗温暖的心</p>
          <p class="warm-note-sub">世界会因你而更美好 🌸</p>
        </section>

        <!-- Weekly creators -->
        <section v-if="weeklyRank.length" class="side-card weekly-rank-card">
          <h3 class="side-title">本周共创榜</h3>
          <ul class="weekly-rank-list">
            <li v-for="item in weeklyRank" :key="item.userId">
              <span class="wr-rank">#{{ item.rank }}</span>
              <span class="wr-name">{{ item.nickname }}</span>
              <strong class="wr-score">{{ item.score }}</strong>
            </li>
          </ul>
        </section>

        <!-- Hot Categories -->
        <section class="side-card hot-cat-card">
          <h3 class="side-title">热门分类</h3>
          <ul class="hot-cat-list">
            <li v-for="cat in hotCategories" :key="cat.name" class="hot-cat-item">
              <span class="hot-cat-dot" :style="{ background: catDotColor(cat.name) }"></span>
              <span class="hot-cat-name">{{ cat.name }}</span>
              <span class="hot-cat-count">{{ cat.count }} 条</span>
            </li>
          </ul>
          <button type="button" class="see-all-btn">查看全部 &rsaquo;</button>
        </section>

        <!-- Daily Quote -->
        <section class="side-card quote-card">
          <div class="quote-mark">"</div>
          <p class="quote-text">不要因为走得太远，忘了为什么出发。</p>
          <p class="quote-author">— 初心</p>
        </section>
      </aside>
    </div>

    <!-- ═══ Comment Modal ═══ -->
    <div v-if="commentTarget" class="modal-mask" @click.self="commentTarget = null">
      <section class="modal-box">
        <h3 class="modal-title">给 TA 一句鼓励</h3>
        <textarea
          v-model.trim="commentContent"
          maxlength="500"
          rows="4"
          placeholder="写下你的鼓励留言..."
          class="modal-textarea"
        />
        <p class="modal-counter">{{ commentContent.length }}/500</p>
        <div class="modal-actions">
          <button type="button" class="modal-cancel" @click="commentTarget = null">取消</button>
          <button type="button" class="modal-submit" :disabled="commentSubmitting" @click="submitComment">
            {{ commentSubmitting ? '提交中...' : '提交留言' }}
          </button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  bookmarkPositiveShare,
  commentPositiveShare,
  fetchPositiveShareComments,
  fetchMyPositiveShares,
  fetchPositiveShares,
  fetchPositiveShareDailyTopic,
  fetchMyPositiveShareStreak,
  fetchPositiveShareWeeklyRankings,
  likePositiveShare,
  unbookmarkPositiveShare,
  unlikePositiveShare
} from '@/api/positiveShare.js'
import { getUserStats } from '@/api/user.js'
import PositiveShareEditor from '@/components/platform/positive/PositiveShareEditor.vue'
import PositiveShareCard from '@/components/platform/positive/PositiveShareCard.vue'
import { getPositiveShareCampaign } from '@/config/positiveShareCampaign.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import { getAvatar } from '@/utils/image.js'
import { userAvatarUrlFromApi } from '@/utils/displayFields.js'

const mainTabs = [
  { label: '最新', value: 'latest' },
  { label: '今日', value: 'today' },
  { label: '热门', value: 'hot' },
  { label: '我的', value: 'my' },
]
const filterCats = ['感恩', '鼓励', '理想', '成长', '生活']
const weekDays = ['一', '二', '三', '四', '五', '六', '日']

const CAT_DOT_COLORS = {
  '感恩': '#ef4444',
  '鼓励': '#f97316',
  '成长': '#22c55e',
  '理想': '#3b82f6',
  '生活': '#ec4899',
  '学习': '#a855f7',
  '工作': '#64748b',
  '人际': '#6366f1',
}

const campaign = computed(() => getPositiveShareCampaign())
const { topicsPath } = usePlatformPath()
const campaignTopicLink = computed(() => {
  if (!campaign.value.topicLink) return null
  return { to: topicsPath(), label: campaign.value.topicLink.label }
})
const campaignBannerLink = computed(() => {
  if (!campaign.value.campaignBanner) return null
  return { ...campaign.value.campaignBanner, link: topicsPath() }
})

const activeTab = ref('latest')
const activeCategory = ref(null)
const list = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const pageNum = ref(1)
const hasMore = ref(false)
const commentTarget = ref(null)
const commentContent = ref('')
const commentSubmitting = ref(false)
const expandedShareId = ref(null)
const commentsLoading = ref(false)
const comments = ref([])
const commentAvatarErrorIds = ref(new Set())
const editorRef = ref(null)

async function fetchList(append = false) {
  const currentPage = pageNum.value
  if (!append) loading.value = true
  try {
    const api = activeTab.value === 'my' ? fetchMyPositiveShares : fetchPositiveShares
    const res = await api({ tab: activeTab.value, pageNum: currentPage, pageSize: 10 })
    const nextList = Array.isArray(res?.list) ? res.list : []
    list.value = append ? list.value.concat(nextList) : nextList
    hasMore.value = Boolean(res?.hasMore)
  } catch {
    if (!append) list.value = []
    hasMore.value = false
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const todayStr = new Date().toISOString().slice(0, 10)

const todayCount = computed(() =>
  list.value.filter(item => String(item.createdAt || '').slice(0, 10) === todayStr).length
)
const totalEncourage = computed(() =>
  list.value.reduce((sum, item) => sum + Number(item.encourageCount || 0), 0)
)
const activeAuthors = computed(() => new Set(list.value.map(item => item.userId)).size)
const myShareCount = computed(() => list.value.filter(item => item.mine).length)
const myEncourageCount = computed(() =>
  list.value.filter(item => item.mine).reduce((sum, item) => sum + Number(item.encourageCount || 0), 0)
)
const myFavoriteCount = ref(0)
const dailyTopic = ref(null)
const shareStreak = ref({ currentStreak: 0, longestStreak: 0, checkedToday: false })
const weeklyRank = ref([])
const streakDays = computed(() => Number(shareStreak.value?.currentStreak ?? 0))

const hotCategories = computed(() => {
  const counter = {}
  for (const item of list.value) {
    const key = item.category || '其他'
    counter[key] = (counter[key] || 0) + 1
  }
  return Object.entries(counter)
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5)
})

const visibleList = computed(() => {
  if (!activeCategory.value) return list.value
  return list.value.filter(item => item.category === activeCategory.value)
})

const calendarCells = computed(() => {
  const cells = []
  const today = new Date()
  const dayOfWeek = (today.getDay() + 6) % 7
  const startDate = new Date(today)
  startDate.setDate(today.getDate() - dayOfWeek - 14)

  const shareDates = new Set(
    list.value
      .filter(item => item.mine)
      .map(item => String(item.createdAt || '').slice(0, 10))
  )

  for (let i = 0; i < 21; i++) {
    const d = new Date(startDate)
    d.setDate(startDate.getDate() + i)
    const ds = d.toISOString().slice(0, 10)
    cells.push({
      key: ds,
      hasShare: shareDates.has(ds),
      isToday: ds === todayStr,
      isEmpty: false,
    })
  }
  return cells
})

function catDotColor(name) {
  return CAT_DOT_COLORS[name] || '#94a3b8'
}

function toggleCategory(cat) {
  activeCategory.value = activeCategory.value === cat ? null : cat
}

function switchTab(tab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  pageNum.value = 1
  fetchList(false)
}

async function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  loadingMore.value = true
  pageNum.value += 1
  await fetchList(true)
}

function handlePublished(created) {
  if (!created) return
  if (activeTab.value === 'my') {
    list.value = [created, ...list.value]
    loadGameplayMeta()
    return
  }
  if ((activeTab.value === 'latest' || activeTab.value === 'today') && created.status === 'PUBLISHED') {
    list.value = [created, ...list.value]
  }
  loadGameplayMeta()
}

async function handleLike(item) {
  try {
    const res = item.liked ? await unlikePositiveShare(item.id) : await likePositiveShare(item.id)
    item.liked = Boolean(res?.liked)
    item.encourageCount = Number(res?.encourageCount ?? item.encourageCount ?? 0)
  } catch {
    // ignore
  }
}

async function handleBookmark(item) {
  try {
    const res = item.bookmarked ? await unbookmarkPositiveShare(item.id) : await bookmarkPositiveShare(item.id)
    item.bookmarked = Boolean(res?.bookmarked)
    await refreshBookmarkStat()
  } catch {
    // ignore
  }
}

async function refreshBookmarkStat() {
  try {
    const s = await getUserStats()
    myFavoriteCount.value = Number(s?.positiveShareBookmarkCount ?? 0)
  } catch {
    // ignore
  }
}

function openComment(item) {
  commentTarget.value = item
  commentContent.value = ''
}

async function submitComment() {
  if (!commentTarget.value || !commentContent.value || commentSubmitting.value) return
  commentSubmitting.value = true
  try {
    const res = await commentPositiveShare(commentTarget.value.id, { content: commentContent.value })
    commentTarget.value.commentCount = Number(res?.commentCount ?? commentTarget.value.commentCount ?? 0)
    if (expandedShareId.value === commentTarget.value.id) {
      await loadComments(commentTarget.value.id)
    }
    commentTarget.value = null
    commentContent.value = ''
  } catch {
    // ignore
  } finally {
    commentSubmitting.value = false
  }
}

async function toggleComments(item) {
  if (expandedShareId.value === item.id) {
    expandedShareId.value = null
    comments.value = []
    return
  }
  expandedShareId.value = item.id
  await loadComments(item.id)
}

async function loadComments(shareId) {
  commentsLoading.value = true
  try {
    const res = await fetchPositiveShareComments(shareId, { pageNum: 1, pageSize: 30 })
    const list = Array.isArray(res?.list) ? res.list : []
    comments.value = list.map((c) => ({
      ...c,
      _hasAvatar: Boolean(userAvatarUrlFromApi(c)),
      avatar: getAvatar(c)
    }))
    commentAvatarErrorIds.value = new Set()
  } catch {
    comments.value = []
    commentAvatarErrorIds.value = new Set()
  } finally {
    commentsLoading.value = false
  }
}

function formatDate(value) {
  if (!value) return '刚刚'
  return String(value).replace('T', ' ').slice(0, 16)
}

function commentInitial(username) {
  const safeName = String(username || '用户').trim()
  return safeName ? safeName.slice(0, 1) : '用'
}

function isCommentAvatarError(commentId) {
  return commentAvatarErrorIds.value.has(commentId)
}

function markCommentAvatarError(commentId) {
  commentAvatarErrorIds.value.add(commentId)
}

function scrollToEditor() {
  editorRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function loadGameplayMeta() {
  try {
    const [topicRes, streakRes, rankRes] = await Promise.allSettled([
      fetchPositiveShareDailyTopic(),
      fetchMyPositiveShareStreak(),
      fetchPositiveShareWeeklyRankings({ limit: 5 })
    ])
    if (topicRes.status === 'fulfilled') dailyTopic.value = topicRes.value
    if (streakRes.status === 'fulfilled') shareStreak.value = streakRes.value || shareStreak.value
    if (rankRes.status === 'fulfilled') {
      weeklyRank.value = Array.isArray(rankRes.value?.items) ? rankRes.value.items : []
    }
  } catch {
    /* silent */
  }
}

onMounted(() => {
  fetchList(false)
  refreshBookmarkStat()
  loadGameplayMeta()
})
</script>

<style scoped>
/* ── Page Root ── */
.positive-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ══════════════════════════════
   Hero
══════════════════════════════ */
.hero {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 300px;
  min-height: 200px;
  border-radius: 20px;
  background: linear-gradient(120deg, #fff8f4 0%, #fdecd8 45%, #f9d8ae 100%);
  overflow: hidden;
  border: 1px solid #f5dfc8;
}

.hero-content {
  padding: 36px 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0;
  z-index: 1;
}

.hero--dragon-boat {
  background: linear-gradient(120deg, #f0fdf4 0%, #dcfce7 45%, #bbf7d0 100%);
  border-color: #86efac;
}

.hero-kicker {
  margin: 0 0 4px;
  font-size: 12px;
  font-weight: 800;
  color: #15803d;
  letter-spacing: 0.04em;
}

.hero-title {
  margin: 0;
  color: #1a1714;
  font-size: clamp(30px, 3.5vw, 44px);
  font-weight: 800;
  line-height: 1.2;
}

.hero-topic-link {
  display: inline-block;
  margin-top: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #15803d;
  text-decoration: none;
}

.campaign-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 14px;
  background: linear-gradient(90deg, #ecfdf5, #f0fdf4);
  border: 1px solid #86efac;
}

.campaign-strip-kicker {
  display: block;
  font-size: 12px;
  font-weight: 800;
  color: #15803d;
}

.campaign-strip-text {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #166534;
}

.campaign-strip-link {
  flex-shrink: 0;
  padding: 8px 12px;
  border-radius: 999px;
  background: #16a34a;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.hero-sub {
  margin: 8px 0 0;
  color: #7c6850;
  font-size: 14px;
  line-height: 1.65;
}

.hero-topic {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(249, 115, 22, 0.2);
}

.hero-topic-label {
  display: inline-block;
  font-size: 11px;
  font-weight: 800;
  color: var(--lc-orange);
  letter-spacing: 0.04em;
}

.hero-topic-text {
  margin: 6px 0 0;
  font-size: 15px;
  font-weight: 700;
  color: #5c4a32;
  line-height: 1.5;
}

.hero-topic-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #9a8568;
}

.weekly-rank-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.weekly-rank-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--lc-soft);
  font-size: 13px;
}

.wr-rank {
  font-weight: 800;
  color: var(--lc-indigo);
  min-width: 28px;
}

.wr-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wr-score {
  color: var(--lc-pink);
  font-weight: 800;
}

.hero-btn {
  margin-top: 22px;
  align-self: flex-start;
  height: 40px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--lc-orange), var(--lc-orange));
  color: var(--lc-surface);
  font-size: 14px;
  font-weight: 700;
  padding: 0 22px;
  cursor: pointer;
  box-shadow: 0 3px 10px rgba(249, 115, 22, 0.4);
  transition: transform 0.15s, opacity 0.15s;
}

.hero-btn:hover {
  transform: translateY(-2px);
  opacity: 0.92;
}

/* CSS Art */
.hero-art {
  position: relative;
  overflow: hidden;
}

.art-glow {
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 180, 80, 0.5) 0%, transparent 70%);
  top: -30px;
  right: 20px;
}

.art-cup {
  position: absolute;
  bottom: 28px;
  right: 60px;
  width: 60px;
  height: 52px;
  border-radius: 0 0 18px 18px;
  background: linear-gradient(180deg, #e8d5b0, #d4b88a);
  border: 2px solid #c9a870;
}

.art-cup::before {
  content: '';
  position: absolute;
  top: 50%;
  right: -16px;
  width: 14px;
  height: 22px;
  border-radius: 0 12px 12px 0;
  border: 2px solid #c9a870;
  border-left: none;
  transform: translateY(-50%);
}

.art-cup::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: -8px;
  right: -8px;
  height: 8px;
  background: #c4a270;
  border-radius: 0 0 4px 4px;
}

.art-steam {
  position: absolute;
  width: 3px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.6);
  animation: steam 2.5s ease-in-out infinite;
}

.art-steam-1 {
  height: 24px;
  bottom: 82px;
  right: 82px;
  animation-delay: 0s;
}

.art-steam-2 {
  height: 18px;
  bottom: 84px;
  right: 98px;
  animation-delay: 0.8s;
}

@keyframes steam {
  0%, 100% { transform: translateY(0) scaleX(1); opacity: 0.6; }
  50% { transform: translateY(-12px) scaleX(1.3); opacity: 0; }
}

.art-stem-1 {
  position: absolute;
  bottom: 28px;
  right: 150px;
  width: 3px;
  height: 80px;
  background: linear-gradient(180deg, #7cb87c, #5a9e5a);
  border-radius: 2px;
  transform: rotate(-8deg);
  transform-origin: bottom;
}

.art-stem-2 {
  position: absolute;
  bottom: 28px;
  right: 170px;
  width: 3px;
  height: 60px;
  background: linear-gradient(180deg, #8cc88c, #6ab06a);
  border-radius: 2px;
  transform: rotate(6deg);
  transform-origin: bottom;
}

.art-bloom-1 {
  position: absolute;
  bottom: 102px;
  right: 144px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: radial-gradient(circle, #f4c2a0, #e8a070);
  box-shadow: 0 0 0 5px rgba(244, 194, 160, 0.3);
}

.art-bloom-2 {
  position: absolute;
  bottom: 82px;
  right: 166px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: radial-gradient(circle, #f4d0a0, #e8b870);
  box-shadow: 0 0 0 4px rgba(244, 208, 160, 0.3);
}

/* ══════════════════════════════
   Stats Band
══════════════════════════════ */
.stats-band {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-card {
  border: 1px solid #f0ebe5;
  border-radius: 14px;
  background: var(--lc-surface);
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-icon--orange { background: var(--lc-orange-light); }
.stat-icon--pink   { background: #fff0f3; }
.stat-icon--blue   { background: var(--lc-blue-light); }

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.stat-num-row {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.stat-num {
  color: var(--lc-text);
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

.stat-unit {
  color: var(--lc-subtle);
  font-size: 13px;
}

.stat-label {
  color: var(--lc-muted-light);
  font-size: 12px;
}

.stat-trend {
  color: var(--lc-emerald);
  font-size: 12px;
  font-weight: 600;
}

/* ══════════════════════════════
   Content Layout
══════════════════════════════ */
.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 16px;
  align-items: start;
}

.main-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ══════════════════════════════
   List Card
══════════════════════════════ */
.list-card {
  border: 1px solid #f0ebe5;
  border-radius: 16px;
  background: var(--lc-surface);
  padding: 18px 20px;
}

.tab-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  border-bottom: 1px solid #f3ece6;
}

.main-tabs {
  display: flex;
  gap: 0;
  border-bottom: none;
}

.tab-btn {
  border: none;
  background: transparent;
  color: var(--lc-muted-light);
  font-size: 14px;
  font-weight: 600;
  padding: 6px 14px;
  cursor: pointer;
  position: relative;
  transition: color 0.15s;
}

.tab-btn::after {
  content: '';
  position: absolute;
  bottom: -14px;
  left: 14px;
  right: 14px;
  height: 2px;
  border-radius: 2px;
  background: var(--lc-orange);
  transform: scaleX(0);
  transition: transform 0.2s;
}

.tab-btn.active {
  color: var(--lc-orange);
}

.tab-btn.active::after {
  transform: scaleX(1);
}

.cat-filter {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-left: 4px;
}

.filter-btn {
  border: 1px solid #ece6e0;
  border-radius: 999px;
  background: #faf8f5;
  color: var(--lc-muted-light);
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.filter-btn.active {
  border-color: #fdba74;
  background: var(--lc-orange-light);
  color: var(--lc-orange);
}

.filter-icon-btn {
  margin-left: auto;
  border: 1px solid #ece6e0;
  border-radius: 8px;
  background: #faf8f5;
  color: var(--lc-muted-light);
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

/* Share List */
.share-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.loading-tip {
  text-align: center;
  color: var(--lc-subtle);
  padding: 24px 0;
  font-size: 14px;
}

.share-item {
  display: flex;
  flex-direction: column;
}

.comment-section {
  margin: 0 0 0 46px;
  border: 1px dashed #fde8d0;
  border-radius: 10px;
  background: #fffaf6;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-loading,
.comment-empty {
  margin: 0;
  color: var(--lc-subtle);
  font-size: 13px;
  text-align: center;
  padding: 8px 0;
}

.comment-row {
  background: var(--lc-surface);
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.comment-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #f3f4f6;
  color: var(--lc-muted-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
  flex-shrink: 0;
}

.comment-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-main {
  min-width: 0;
  flex: 1;
}

.comment-author {
  display: block;
  color: var(--lc-slate);
  font-size: 13px;
  font-weight: 700;
}

.comment-text {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.65;
}

.comment-time {
  display: block;
  margin-top: 4px;
  color: var(--lc-subtle);
  font-size: 11px;
}

/* Empty State */
.list-empty {
  text-align: center;
  padding: 40px 20px;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.empty-title {
  margin: 0;
  color: var(--lc-slate);
  font-size: 15px;
  font-weight: 700;
}

.empty-hint {
  margin: 6px 0 0;
  color: var(--lc-subtle);
  font-size: 13px;
}

/* Load More */
.load-more {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.load-more-btn {
  border: 1px solid #ece6e0;
  border-radius: 999px;
  background: var(--lc-surface);
  color: var(--lc-muted-light);
  height: 36px;
  padding: 0 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.load-more-btn:hover {
  border-color: var(--lc-orange);
  color: var(--lc-orange);
}

.list-end {
  margin: 16px 0 0;
  text-align: center;
  color: #d1c4b8;
  font-size: 12px;
}

/* ══════════════════════════════
   Side Panel
══════════════════════════════ */
.side-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-card {
  border: 1px solid #f0ebe5;
  border-radius: 14px;
  background: var(--lc-surface);
  padding: 16px;
}

.side-title {
  margin: 0 0 12px;
  color: var(--lc-text);
  font-size: 14px;
  font-weight: 700;
}

/* My Data */
.my-data-card {
  background: linear-gradient(180deg, var(--lc-surface) 0%, #fff8f4 100%);
}

.my-data-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.my-data-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.my-data-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.my-data-label {
  flex: 1;
  color: #4b5563;
  font-size: 13px;
}

.my-data-val {
  color: var(--lc-text);
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.my-data-val em {
  font-style: normal;
  color: var(--lc-subtle);
  font-size: 12px;
  font-weight: 400;
  margin-left: 1px;
}

.streak-cheer {
  display: block;
  color: var(--lc-orange);
  font-size: 11px;
  font-weight: 600;
  margin-top: 2px;
}

/* Calendar */
.calendar-card {
  background: linear-gradient(180deg, var(--lc-surface) 0%, #fffaf6 100%);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.cal-head {
  text-align: center;
  color: var(--lc-subtle);
  font-size: 11px;
  font-weight: 600;
  padding: 2px 0;
}

.cal-cell {
  aspect-ratio: 1;
  border-radius: 6px;
  background: #f5f0eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  transition: background 0.15s;
}

.cal-cell.checked {
  background: var(--lc-orange-light);
}

.cal-cell.today {
  border: 1.5px solid var(--lc-orange);
}

.cal-check {
  color: var(--lc-orange);
  font-size: 13px;
  font-weight: 700;
}

/* Warm Note */
.warm-note-card {
  background: linear-gradient(135deg, #fffbf3, #fff6e8);
  border-color: #fde8c0;
  text-align: center;
  padding: 18px 16px;
  position: relative;
  overflow: hidden;
}

.warm-note-art {
  position: relative;
  height: 60px;
  margin-bottom: 10px;
}

.plant-pot {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 34px;
  height: 24px;
  background: linear-gradient(180deg, #e8c090, #d4a870);
  border-radius: 0 0 10px 10px;
  clip-path: polygon(10% 0%, 90% 0%, 100% 100%, 0% 100%);
}

.plant-stem {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 30px;
  background: linear-gradient(180deg, #5ca05c, #4a8a4a);
  border-radius: 2px;
}

.plant-leaf {
  position: absolute;
  bottom: 36px;
  width: 20px;
  height: 14px;
  border-radius: 50% 0 50% 0;
  background: linear-gradient(135deg, #7cc07c, #5aa05a);
}

.plant-leaf-l {
  left: calc(50% - 26px);
  transform: rotate(-30deg);
}

.plant-leaf-r {
  right: calc(50% - 26px);
  transform: rotate(30deg) scaleX(-1);
}

.warm-note-text {
  margin: 0;
  color: #92653a;
  font-size: 13px;
  font-weight: 700;
}

.warm-note-sub {
  margin: 4px 0 0;
  color: #b08040;
  font-size: 12px;
}

/* Hot Categories */
.hot-cat-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.hot-cat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hot-cat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.hot-cat-name {
  flex: 1;
  color: var(--lc-slate);
  font-size: 13px;
}

.hot-cat-count {
  color: var(--lc-subtle);
  font-size: 12px;
}

.see-all-btn {
  margin-top: 12px;
  border: none;
  background: transparent;
  color: var(--lc-orange);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  display: block;
  width: 100%;
  text-align: right;
}

/* Quote Card */
.quote-card {
  background: linear-gradient(135deg, #fffbf6, #fff7ee);
  border-color: #f5e4c8;
}

.quote-mark {
  color: #f5c87c;
  font-size: 40px;
  line-height: 0.8;
  margin-bottom: 6px;
  font-family: Georgia, serif;
}

.quote-text {
  margin: 0;
  color: var(--lc-slate);
  font-size: 14px;
  line-height: 1.75;
  font-style: italic;
}

.quote-author {
  margin: 10px 0 0;
  text-align: right;
  color: var(--lc-subtle);
  font-size: 12px;
}

/* ══════════════════════════════
   Comment Modal
══════════════════════════════ */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(15, 23, 42, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.modal-box {
  width: min(520px, 100%);
  border-radius: 16px;
  background: var(--lc-surface);
  padding: 24px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
}

.modal-title {
  margin: 0 0 14px;
  color: var(--lc-text);
  font-size: 16px;
  font-weight: 700;
}

.modal-textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #e5e0da;
  border-radius: 10px;
  padding: 12px 14px;
  font: inherit;
  font-size: 14px;
  resize: vertical;
  color: var(--lc-slate);
}

.modal-textarea:focus {
  outline: none;
  border-color: var(--lc-orange);
}

.modal-counter {
  margin: 6px 0 0;
  text-align: right;
  color: var(--lc-subtle);
  font-size: 12px;
}

.modal-actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.modal-cancel,
.modal-submit {
  height: 36px;
  border-radius: 8px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s;
}

.modal-cancel {
  border: 1px solid #e5e0da;
  background: var(--lc-surface);
  color: var(--lc-muted-light);
}

.modal-submit {
  border: none;
  background: linear-gradient(135deg, var(--lc-orange), var(--lc-orange));
  color: var(--lc-surface);
  box-shadow: 0 2px 6px rgba(249, 115, 22, 0.3);
}

.modal-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ══════════════════════════════
   Responsive
══════════════════════════════ */
@media (max-width: 1100px) {
  .content-layout {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 767px) {
  .hero {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .hero-art {
    display: none;
  }

  .hero-content {
    padding: 14px 14px 12px;
  }

  .hero-title {
    font-size: 22px;
  }

  .hero-sub {
    margin-top: 4px;
    font-size: 12px;
    line-height: 1.45;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .hero-topic {
    margin-top: 8px;
    padding: 10px 12px;
  }

  .hero-topic-text {
    font-size: 13px;
    -webkit-line-clamp: 2;
  }

  .hero-btn {
    margin-top: 10px;
    height: 40px;
    font-size: 14px;
  }

  .stats-band {
    display: flex;
    gap: 0;
    padding: 8px 4px;
    border: 1px solid #f0ebe5;
    border-radius: 12px;
    background: var(--lc-surface);
  }

  .stat-card {
    flex: 1 1 0;
    min-width: 0;
    padding: 8px 6px;
    gap: 6px;
    border: 0;
    border-radius: 0;
    align-items: center;
    background: transparent;
  }

  .stat-card:not(:first-child) {
    border-left: 1px solid #f0ebe5;
  }

  .stat-icon {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    font-size: 14px;
  }

  .stat-num {
    font-size: 16px;
  }

  .stat-trend {
    display: none;
  }

  .stat-unit,
  .stat-label {
    font-size: 11px;
    line-height: 1.2;
  }

  .side-panel {
    grid-template-columns: 1fr;
  }

  .tab-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .filter-icon-btn {
    margin-left: 0;
  }

  .campaign-strip {
    flex-direction: column;
    align-items: flex-start;
  }

  .campaign-strip-link {
    width: 100%;
    text-align: center;
    box-sizing: border-box;
  }

  .hero-sub + .hero-sub {
    display: none;
  }
}
</style>
