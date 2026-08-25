<template>
  <section class="featured-page">
    <section class="featured-hero">
      <div class="hero-copy">
        <p class="hero-kicker">Featured Content</p>
        <h1>精选内容</h1>
        <p>精选攻略、平台公告与活动资讯，一站式浏览。</p>
        <button type="button" class="publish-entry-btn" @click="openPublishDialog">{{ writePath ? '写文章' : '发布内容' }}</button>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <img :src="heroImage" alt="" />
      </div>
    </section>

    <section class="featured-shell">
      <div class="featured-main">
        <div class="toolbar">
          <div class="category-tabs" aria-label="内容分类">
            <button
              v-for="cat in categories"
              :key="cat.value"
              class="category-tab"
              :class="{ active: activeCategory === cat.value }"
              type="button"
              @click="setCategory(cat.value)"
            >
              {{ cat.label }}
            </button>
          </div>

          <select v-model="sortMode" class="sort-select" aria-label="内容排序">
            <option value="latest">最新发布</option>
            <option value="hot">热度优先</option>
            <option value="liked">点赞最多</option>
          </select>
        </div>

        <div v-if="featuredLead" class="content-layout">
          <router-link :to="`/articles/${featuredLead.id}`" class="article-card article-card-lead">
            <div class="article-cover">
              <img :src="featuredLead.coverUrl" :alt="featuredLead.title" loading="lazy" />
              <span class="article-label label-hot">置顶</span>
              <span class="article-label label-category">{{ featuredLead.sourceLabel }}</span>
            </div>
            <div class="article-body">
              <h2>{{ featuredLead.title }}</h2>
              <p>{{ featuredLead.summary }}</p>
              <div class="article-meta">
                <span>{{ featuredLead.author }}</span>
                <span>{{ featuredLead.reads }}阅读</span>
                <span>{{ featuredLead.likes }}</span>
                <time :datetime="featuredLead.date">{{ featuredLead.date }}</time>
              </div>
            </div>
          </router-link>

          <router-link
            v-for="item in secondaryFeatured"
            :key="item.id"
            :to="`/articles/${item.id}`"
            class="article-card article-card-compact"
          >
            <div class="article-cover">
              <img :src="item.coverUrl" :alt="item.title" loading="lazy" />
              <span class="article-label" :class="item.labelClass">{{ item.sourceLabel }}</span>
            </div>
            <div class="article-body">
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary }}</p>
              <div class="article-meta">
                <span>{{ item.author }}</span>
                <span>{{ item.reads }}阅读</span>
                <time :datetime="item.date">{{ item.date }}</time>
              </div>
            </div>
          </router-link>
        </div>

        <div class="article-grid">
          <router-link
            v-for="item in visibleList"
            :key="item.id"
            :to="`/articles/${item.id}`"
            class="article-card article-card-small"
          >
            <div class="article-cover">
              <img :src="item.coverUrl" :alt="item.title" loading="lazy" />
              <span class="article-label" :class="item.labelClass">{{ item.sourceLabel }}</span>
            </div>
            <div class="article-body">
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary }}</p>
              <div class="article-meta">
                <span>{{ item.author }}</span>
                <span>{{ item.reads }}阅读</span>
                <span>{{ item.likes }}</span>
                <time :datetime="item.date">{{ item.date }}</time>
              </div>
            </div>
          </router-link>
        </div>

        <article v-if="!loading && !filteredList.length" class="empty-state">
          <h3>暂无内容</h3>
          <p>当前筛条件下还没有已发布文章，换个分类再看看</p>
        </article>

        <div v-if="hasMore" class="load-more-wrap">
          <button type="button" class="load-more-btn" @click="page++">查看更多内容</button>
        </div>

        <section class="content-policy">
          <div class="policy-icon" aria-hidden="true">❤</div>
          <div>
            <h2>内容规范</h2>
            <p>我们鼓励优质内容分享，严禁发布违法违规低俗色情恶意攻击等不良信息，共同维护良好的社区环境</p>
          </div>
          <router-link to="/policies/content-policy">查看内容规范</router-link>
        </section>
      </div>

      <aside class="featured-aside">
        <section class="aside-card">
          <div class="aside-head">
            <h2>热门话题</h2>
            <router-link to="/articles">查看发布</router-link>
          </div>
          <ol class="topic-list">
            <li v-for="topic in topics" :key="topic.name">
              <span class="topic-rank">{{ topic.rank }}</span>
              <span class="topic-name"># {{ topic.name }}</span>
              <span class="topic-heat">{{ topic.heat }}热度</span>
            </li>
          </ol>
        </section>

        <section class="share-card">
          <div>
            <h2>分享你的故事</h2>
            <p>记录生活、分享美好，结识更多有趣的人</p>
            <router-link to="/articles">进入内容中心</router-link>
          </div>
          <img :src="shareImage" alt="" loading="lazy" />
        </section>

        <section class="aside-card">
          <div class="aside-head">
            <h2>推荐作</h2>
            <router-link to="/articles">查看更多</router-link>
          </div>
          <div class="author-list">
            <article v-for="author in authors" :key="author.name" class="author-item">
              <span class="author-avatar" :class="author.tone">{{ author.initial }}</span>
              <div>
                <h3>{{ author.name }} <em>{{ author.badge }}</em></h3>
                <p>{{ author.desc }}</p>
              </div>
              <button type="button">关注</button>
            </article>
          </div>
        </section>
      </aside>
    </section>

    <div v-if="publishDialogVisible" class="publish-mask" @click.self="closePublishDialog">
      <section class="publish-dialog">
        <header class="publish-head">
          <h2>发布精选内容</h2>
          <button type="button" class="publish-close" @click="closePublishDialog">关闭</button>
        </header>
        <form class="publish-form" @submit.prevent="submitPublish">
          <label>
            <span>标题</span>
            <input v-model.trim="publishForm.title" type="text" maxlength="80" placeholder="请输入标题（最多80字）" />
          </label>
          <label>
            <span>摘要</span>
            <textarea v-model.trim="publishForm.summary" rows="2" maxlength="180" placeholder="请填写摘要（最多180字）" />
          </label>
          <label>
            <span>分类</span>
            <select v-model="publishForm.category">
              <option v-for="cat in categories.filter(item => item.value !== 'all')" :key="cat.value" :value="cat.value">
                {{ cat.label }}
              </option>
            </select>
          </label>
          <label>
            <span>封面图（选填）</span>
            <div class="publish-cover-wrap">
              <img v-if="publishForm.coverUrl" :src="publishForm.coverUrl" alt="封面图预览" class="publish-cover-preview" />
              <div class="publish-cover-actions">
                <button
                  type="button"
                  class="publish-cover-btn"
                  :disabled="publishSubmitting || coverUploading"
                  @click="handlePickCover"
                >
                  {{ coverUploading ? '上传中...' : (publishForm.coverUrl ? '更换图片' : '上传封面图') }}
                </button>
                <button
                  v-if="publishForm.coverUrl"
                  type="button"
                  class="publish-cover-remove"
                  :disabled="publishSubmitting || coverUploading"
                  @click="removeCover"
                >
                  删除
                </button>
              </div>
            </div>
          </label>
          <label>
            <span>正文</span>
            <textarea v-model.trim="publishForm.content" rows="8" maxlength="5000" placeholder="请输入正文内容..." />
          </label>
          <p v-if="publishMessage" class="publish-message" :class="{ 'is-error': publishError }">{{ publishMessage }}</p>
          <div class="publish-actions">
            <button type="button" class="publish-cancel" :disabled="publishSubmitting" @click="closePublishDialog">取消</button>
            <button type="submit" class="publish-submit" :disabled="publishSubmitting">
              {{ publishSubmitting ? '提交中...' : '提交发布' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchArticles, fetchHotTopics, fetchRecommendedAuthors, submitArticle } from '@/api/platformContent.js'
import { useUserStore } from '@/stores/user.js'
import { useImageUpload } from '@/composables/useImageUpload.js'
import heroImage from '@/assets/首页首屏右侧大图.webp'
import leadImage from '@/assets/联谊专区.webp'
import eventImage from '@/assets/活动模块.webp'
import aiImage from '@/assets/AI工具模块卡片.webp'
import announcementImage from '@/assets/公告模块卡片.webp'
import ctaImage from '@/assets/底部横幅 CTA.webp'
import moduleImage from '@/assets/未来扩展模块.webp'

const props = defineProps({
  writePath: { type: String, default: '' }
})

const PAGE_SIZE = 4
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const allItems = ref([])
const activeCategory = ref('all')
const sortMode = ref('latest')
const page = ref(1)
const publishDialogVisible = ref(false)
const publishSubmitting = ref(false)
const publishMessage = ref('')
const publishError = ref(false)
const { pickAndUpload, uploading: coverUploading } = useImageUpload()
const publishForm = reactive({
  title: '',
  summary: '',
  category: '平台资讯',
  coverUrl: '',
  content: ''
})

const categories = [
  { label: '全部', value: 'all' },
  { label: '平台资讯', value: '平台资讯' },
  { label: '活动指南', value: '活动指南' },
  { label: '活动中心', value: '活动中心' },
  { label: '平台攻略', value: '平台攻略' },
  { label: 'AI工具', value: 'AI工具' },
  { label: '本地服务', value: '本地服务' }
]


const topics = ref([])
const authors = ref([])
const AUTHOR_TONES = ['pink', 'blue', 'green', 'violet']

const shareImage = ctaImage

const normalizedItems = computed(() => allItems.value.map((item) => {
  const category = item.category || item.tag || ''
  return {
    id: item.id,
    title: item.title || '',
    summary: item.summary || '',
    category,
    sourceLabel: sourceLabelByCategory(category),
    author: item.authorName || item.author || '',
    reads: item.viewCount > 0 ? formatCount(item.viewCount) : '',
    likes: item.likeCount || item.likes || 0,
    date: formatDate(item.publishedAt || item.createdAt || item.date),
    coverUrl: item.coverUrl || item.cover || '',
    hot: Number(item.viewCount || item.likes || 0),
    labelClass: labelClass(category)
  }
}))

const filteredList = computed(() => {
  const keyword = String(route.query.keyword || '').trim().toLowerCase()
  let list = normalizedItems.value

  if (activeCategory.value !== 'all') {
    list = list.filter(item => item.category === activeCategory.value)
  }

  if (keyword) {
    list = list.filter(item =>
      item.title.toLowerCase().includes(keyword) ||
      item.summary.toLowerCase().includes(keyword) ||
      item.category.toLowerCase().includes(keyword)
    )
  }

  return [...list].sort((a, b) => {
    if (sortMode.value === 'hot') return b.hot - a.hot
    if (sortMode.value === 'liked') return Number(b.likes) - Number(a.likes)
    return new Date(b.date) - new Date(a.date)
  })
})

const featuredLead = computed(() => filteredList.value[0])
const secondaryFeatured = computed(() => filteredList.value.slice(1, 3))
const visibleList = computed(() => filteredList.value.slice(3, 3 + page.value * PAGE_SIZE))
const hasMore = computed(() => filteredList.value.length > 3 + page.value * PAGE_SIZE)

function setCategory(value) {
  activeCategory.value = value
  page.value = 1
}

function formatCount(value) {
  if (typeof value === 'string' && isNaN(Number(value))) return value
  const count = Number(value || 0)
  if (count <= 0) return ''
  if (count >= 10000) return `${(count / 10000).toFixed(1)}w`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return String(count)
}

function formatDate(value) {
  if (!value) return '2026-04-28'
  return String(value).slice(0, 10)
}

function labelClass(category) {
  const map = {
    '平台资讯': 'label-platform',
    '活动指南': 'label-event',
    '活动中心': 'label-event',
    '平台攻略': 'label-platform',
    'AI工具': 'label-ai',
    '本地服务': 'label-life'
  }
  return map[category] || 'label-platform'
}

function sourceLabelByCategory(category) {
  const mapping = {
    '平台资讯': '[平台资讯]',
    '活动指南': '[活动中心]',
    '活动中心': '[活动中心]',
    '平台攻略': '[平台资讯]',
    'AI工具': '[AI工具]',
    '本地服务': '[本地服务]'
  }
  return mapping[category] || '[平台资讯]'
}

function formatHeat(val) {
  const n = Number(val) || 0
  return n >= 1000 ? (n / 1000).toFixed(1) + 'k' : String(n)
}

function openPublishDialog() {
  const nextPath = props.writePath || '/articles'
  if (!userStore.isLoggedIn) {
    userStore.setPostLoginRedirect(nextPath)
    router.push({ path: '/login', query: { redirect: nextPath } })
    return
  }
  if (props.writePath) {
    router.push(props.writePath)
    return
  }
  publishDialogVisible.value = true
  publishMessage.value = ''
  publishError.value = false
}

function closePublishDialog() {
  if (publishSubmitting.value) return
  publishDialogVisible.value = false
}

function resetPublishForm() {
  publishForm.title = ''
  publishForm.summary = ''
  publishForm.category = '平台资讯'
  publishForm.coverUrl = ''
  publishForm.content = ''
}

async function handlePickCover() {
  publishMessage.value = ''
  publishError.value = false
  try {
    const coverUrl = await pickAndUpload({ quality: 0.8 })
    if (!coverUrl) {
      throw new Error('上传失败，请重试')
    }
    publishForm.coverUrl = coverUrl
  } catch (error) {
    publishError.value = true
    publishMessage.value = error?.message || '封面图上传失败，请稍后重试'
  }
}

function removeCover() {
  publishForm.coverUrl = ''
}

async function submitPublish() {
  publishMessage.value = ''
  publishError.value = false
  const title = publishForm.title.trim()
  const content = publishForm.content.trim()
  if (!title || !content) {
    publishError.value = true
    publishMessage.value = '请填写标题和正文'
    return
  }
  publishSubmitting.value = true
  try {
    await submitArticle({
      title,
      summary: publishForm.summary.trim(),
      category: publishForm.category,
      tag: publishForm.category,
      coverUrl: publishForm.coverUrl.trim(),
      content
    })
    publishMessage.value = '投稿已提交，待管理员审核发布'
    resetPublishForm()
    window.setTimeout(() => {
      publishDialogVisible.value = false
      publishMessage.value = ''
    }, 1200)
  } catch (error) {
    publishError.value = true
    publishMessage.value = error.message || '提交失败，请稍后重试'
  } finally {
    publishSubmitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  const [articlesRes, topicsRes, authorsRes] = await Promise.allSettled([
    fetchArticles({ status: 'published' }),
    fetchHotTopics(5),
    fetchRecommendedAuthors(4)
  ])

  if (articlesRes.status === 'fulfilled') {
    allItems.value = Array.isArray(articlesRes.value) ? articlesRes.value : []
  } else {
    allItems.value = []
  }

  if (topicsRes.status === 'fulfilled' && Array.isArray(topicsRes.value) && topicsRes.value.length) {
    topics.value = topicsRes.value.map(t => ({ rank: t.rank, name: t.name, heat: formatHeat(t.heatValue) }))
  }

  if (authorsRes.status === 'fulfilled' && Array.isArray(authorsRes.value) && authorsRes.value.length) {
    authors.value = authorsRes.value.map((a, i) => ({
      name: a.username || 'Love Cube瀹樻柟',
      badge: a.badge || '瀹樻柟',
      desc: a.description || '',
      initial: (a.username || 'L').slice(0, 1),
      tone: AUTHOR_TONES[i % AUTHOR_TONES.length]
    }))
  }

  loading.value = false
})
</script>

<style scoped>
.featured-page {
  background: #f7f9fd;
  padding-bottom: 36px;
}

.featured-hero {
  position: relative;
  overflow: hidden;
  min-height: 268px;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(380px, 620px);
  align-items: center;
  gap: clamp(24px, 3vw, 44px);
  max-width: 1920px;
  margin: 0 auto;
  padding: 42px max(24px, calc((100vw - 1780px) / 2 + 24px));
  background:
    linear-gradient(90deg, rgba(248, 251, 255, 0.98) 0%, rgba(238, 244, 255, 0.88) 58%, rgba(255, 242, 248, 0.92) 100%);
  border-bottom: 1px solid #e6edf7;
}

.featured-hero::before,
.featured-hero::after {
  content: '';
  position: absolute;
  pointer-events: none;
  border-radius: 50%;
  filter: blur(2px);
}

.featured-hero::before {
  width: 320px;
  height: 320px;
  right: -120px;
  top: -140px;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.16) 0%, rgba(124, 92, 255, 0) 72%);
}

.featured-hero::after {
  width: 280px;
  height: 280px;
  left: -140px;
  bottom: -150px;
  background: radial-gradient(circle, rgba(245, 72, 120, 0.16) 0%, rgba(245, 72, 120, 0) 74%);
}

.hero-copy {
  position: relative;
  z-index: 1;
  max-width: 840px;
}

.hero-kicker {
  margin: 0 0 10px;
  color: var(--lc-indigo);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0;
  color: var(--lc-text);
  font-size: clamp(38px, 3.4vw, 52px);
  line-height: 1.15;
  font-weight: 900;
}

.hero-copy p:last-child {
  margin: 14px 0 0;
  color: #526173;
  font-size: 18px;
  line-height: 1.7;
}

.publish-entry-btn {
  margin-top: 16px;
  min-height: 38px;
  border: 1px solid #ff8fb2;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff6f97, var(--lc-rose));
  color: var(--lc-surface);
  font-size: 14px;
  font-weight: 800;
  padding: 0 18px;
  cursor: pointer;
}

.hero-visual {
  position: relative;
  z-index: 1;
  justify-self: center;
  width: min(100%, 560px);
  aspect-ratio: 2 / 1;
}

.hero-visual img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.featured-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: clamp(22px, 2.4vw, 34px);
  width: min(1880px, calc(100% - 40px));
  margin: 30px auto 0;
}

.featured-main {
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 24px;
}

.category-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: nowrap;
  min-width: 0;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: thin;
}

.category-tab,
.sort-select,
.load-more-btn {
  min-height: 38px;
  border: 1px solid var(--lc-soft-alt);
  border-radius: 8px;
  background: var(--lc-surface);
  color: #48576b;
  font-size: 14px;
  font-weight: 700;
}

.category-tab {
  flex: 0 0 auto;
  padding: 0 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-tab:hover,
.category-tab.active {
  color: var(--lc-surface);
  border-color: #ff5f8f;
  background: linear-gradient(135deg, #ff668f, var(--lc-rose));
  box-shadow: 0 10px 24px rgba(245, 72, 120, 0.22);
}

.sort-select {
  flex: 0 0 118px;
  padding: 0 12px;
  outline: none;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.34fr) repeat(2, minmax(280px, 0.82fr));
  gap: 20px;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.article-card {
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  color: inherit;
  text-decoration: none;
  border: 1px solid var(--lc-soft-alt);
  border-radius: 10px;
  background: var(--lc-surface);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.07);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.article-card:hover {
  transform: translateY(-2px);
  border-color: #ffc6d7;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.1);
}

.article-card-lead {
  grid-row: span 2;
  box-shadow: 0 20px 46px rgba(15, 23, 42, 0.12);
}

.article-card-lead:hover {
  transform: translateY(-4px);
  border-color: #ffb8ce;
  box-shadow: 0 24px 54px rgba(15, 23, 42, 0.16);
}

.article-card-compact {
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.article-cover {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #eaf0fb;
}

.article-card-lead .article-cover {
  aspect-ratio: 16 / 6.6;
}

.article-cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.article-label {
  position: absolute;
  top: 12px;
  left: 12px;
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 12px;
  border-radius: 999px;
  color: var(--lc-surface);
  font-size: 12px;
  font-weight: 800;
  background: var(--lc-indigo);
}

.label-category {
  top: auto;
  bottom: 12px;
  background: var(--lc-rose);
}

.label-hot,
.label-social {
  background: var(--lc-rose);
}

.label-event,
.label-life {
  background: var(--lc-amber);
}

.label-psychology,
.label-platform {
  background: #7c5cff;
}

.label-city {
  background: #ef6b3f;
}

.label-ai {
  background: #29b980;
}

.article-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 20px;
}

.article-body h2,
.article-body h3 {
  margin: 0;
  color: var(--lc-text);
  line-height: 1.35;
  font-weight: 900;
}

.article-body h2 {
  font-size: 22px;
}

.article-body h3 {
  font-size: 17px;
}

.article-body p {
  margin: 10px 0 0;
  color: var(--lc-muted-light);
  font-size: 14px;
  line-height: 1.7;
}

.article-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: auto;
  padding-top: 16px;
  color: #8896aa;
  font-size: 12px;
}

.featured-aside {
  display: grid;
  gap: 18px;
  align-content: start;
}

.aside-card,
.share-card,
.content-policy,
.empty-state {
  border: 1px solid var(--lc-soft-alt);
  border-radius: 10px;
  background: var(--lc-surface);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.07);
}

.aside-card {
  padding: 20px;
}

.aside-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.aside-head h2,
.share-card h2,
.content-policy h2,
.empty-state h3 {
  margin: 0;
  color: var(--lc-text);
  font-size: 18px;
  font-weight: 900;
}

.aside-head a,
.content-policy a {
  color: var(--lc-indigo);
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
}

.topic-list {
  display: grid;
  gap: 14px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.topic-list li {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  color: var(--lc-slate);
  font-size: 14px;
  font-weight: 800;
}

.topic-rank {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border-radius: 5px;
  background: var(--lc-soft-alt);
  color: var(--lc-muted-light);
  font-size: 12px;
}

.topic-list li:nth-child(-n + 3) .topic-rank {
  color: var(--lc-surface);
  background: #ff6d92;
}

.topic-heat {
  color: #8b98aa;
  font-size: 12px;
  font-weight: 700;
}

.share-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background:
    linear-gradient(135deg, #fff7fa 0%, var(--lc-surface) 52%, #ffe6ee 100%);
  border-color: #ffd5e2;
}

.share-card p {
  margin: 8px 0 16px;
  color: #8b6474;
  font-size: 13px;
  line-height: 1.6;
}

.share-card a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 18px;
  border-radius: 999px;
  color: var(--lc-surface);
  background: var(--lc-rose);
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
}

.share-card img {
  width: 96px;
  height: 96px;
  object-fit: contain;
}

.author-list {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.author-item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: var(--lc-surface);
  font-size: 14px;
  font-weight: 900;
}

.author-avatar.pink {
  background: var(--lc-rose);
}

.author-avatar.blue {
  background: #68a1ff;
}

.author-avatar.green {
  background: #36c083;
}

.author-avatar.violet {
  background: #a66dff;
}

.author-item h3 {
  margin: 0;
  color: var(--lc-text);
  font-size: 14px;
  font-weight: 900;
}

.author-item em {
  margin-left: 4px;
  padding: 1px 6px;
  border-radius: 999px;
  background: #fff0f5;
  color: var(--lc-rose);
  font-size: 11px;
  font-style: normal;
}

.author-item p {
  margin: 4px 0 0;
  color: #7c8a9d;
  font-size: 12px;
}

.author-item button {
  min-height: 30px;
  padding: 0 14px;
  border: 1px solid #ffd4e0;
  border-radius: 999px;
  color: var(--lc-rose);
  background: #fff7fa;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.content-policy {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  margin-top: 28px;
  padding: 22px;
  border-color: #ffd5e2;
  background: linear-gradient(135deg, #fff7fa 0%, var(--lc-surface) 70%);
}

.policy-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  color: var(--lc-surface);
  background: #ff6d92;
  font-size: 20px;
}

.content-policy p,
.empty-state p {
  margin: 8px 0 0;
  color: var(--lc-muted-light);
  font-size: 14px;
  line-height: 1.7;
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}

.load-more-btn {
  padding: 0 28px;
  cursor: pointer;
}

.load-more-btn:hover {
  color: var(--lc-rose);
  border-color: #ffc6d7;
}

.empty-state {
  margin-top: 24px;
  padding: 28px;
}

.publish-mask {
  position: fixed;
  inset: 0;
  z-index: 210;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(15, 23, 42, 0.45);
}

.publish-dialog {
  width: min(760px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  border-radius: 16px;
  background: var(--lc-surface);
  border: 1px solid #e6edf7;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.2);
}

.publish-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px 12px;
  border-bottom: 1px solid var(--lc-soft-alt);
}

.publish-head h2 {
  margin: 0;
  color: var(--lc-text);
  font-size: 20px;
  font-weight: 900;
}

.publish-close {
  border: 1px solid #dbe4f2;
  border-radius: 8px;
  background: var(--lc-surface);
  color: var(--lc-muted-light);
  min-height: 32px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.publish-form {
  padding: 16px 20px 20px;
  display: grid;
  gap: 12px;
}

.publish-form label {
  display: grid;
  gap: 6px;
}

.publish-form span {
  color: var(--lc-slate);
  font-size: 13px;
  font-weight: 700;
}

.publish-form input,
.publish-form select,
.publish-form textarea {
  width: 100%;
  border: 1px solid var(--lc-border);
  border-radius: 8px;
  background: var(--lc-surface);
  color: var(--lc-text);
  font: inherit;
  font-size: 14px;
  padding: 10px 12px;
  box-sizing: border-box;
}

.publish-form textarea {
  resize: vertical;
}

.publish-cover-wrap {
  display: grid;
  gap: 8px;
}

.publish-cover-preview {
  width: 100%;
  max-width: 260px;
  aspect-ratio: 16 / 9;
  border-radius: 8px;
  border: 1px solid var(--lc-border);
  object-fit: cover;
  background: var(--lc-bg);
}

.publish-cover-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.publish-cover-btn,
.publish-cover-remove {
  min-height: 34px;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.publish-cover-btn {
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
  color: var(--lc-slate);
}

.publish-cover-remove {
  border: 1px solid var(--lc-red-light);
  background: #fff1f2;
  color: #be123c;
}

.publish-cover-btn:disabled,
.publish-cover-remove:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.publish-message {
  margin: 0;
  border-radius: 8px;
  padding: 10px 12px;
  color: #15803d;
  background: #ecfdf3;
  font-size: 13px;
  font-weight: 700;
}

.publish-message.is-error {
  color: var(--lc-red);
  background: var(--lc-red-light);
}

.publish-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.publish-cancel,
.publish-submit {
  min-height: 36px;
  border-radius: 8px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.publish-cancel {
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
  color: var(--lc-muted);
}

.publish-submit {
  border: 1px solid var(--lc-rose);
  background: var(--lc-rose);
  color: var(--lc-surface);
}

.publish-submit:disabled,
.publish-cancel:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@media (max-width: 1440px) {
  .featured-hero {
    min-height: 236px;
    grid-template-columns: minmax(0, 1fr) minmax(320px, 500px);
    padding: 34px max(20px, calc((100vw - 1320px) / 2 + 20px));
  }

  .featured-hero::before {
    width: 260px;
    height: 260px;
    right: -120px;
    top: -130px;
  }

  .featured-hero::after {
    width: 230px;
    height: 230px;
    left: -120px;
    bottom: -140px;
  }

  .hero-copy h1 {
    font-size: clamp(34px, 3.1vw, 44px);
  }

  .hero-copy p:last-child {
    font-size: 16px;
  }

  .featured-shell {
    grid-template-columns: minmax(0, 1fr) 320px;
    width: min(1400px, calc(100% - 32px));
    gap: 24px;
    margin-top: 24px;
  }

  .content-layout {
    grid-template-columns: minmax(0, 1.16fr) repeat(2, minmax(240px, 0.72fr));
    gap: 16px;
  }

  .article-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }
}

@media (max-width: 1199px) {
  .featured-shell {
    grid-template-columns: 1fr;
    width: calc(100% - 32px);
  }

  .featured-aside {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-layout {
    grid-template-columns: minmax(0, 1fr) minmax(240px, 0.85fr);
  }

  .article-card-lead {
    grid-row: auto;
    grid-column: 1 / -1;
  }

  .article-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .featured-hero {
    grid-template-columns: 1fr;
    min-height: auto;
    gap: 14px;
    margin-top: 0;
    padding: 24px 16px 22px;
  }

  .featured-hero::before,
  .featured-hero::after {
    display: none;
  }

  .hero-copy h1 {
    font-size: 32px;
  }

  .hero-copy p:last-child {
    font-size: 15px;
    line-height: 1.65;
  }

  .hero-visual {
    width: 100%;
  }

  .featured-shell {
    width: calc(100% - 24px);
    margin-top: 14px;
    gap: 18px;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .category-tabs {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .category-tab {
    flex: 0 0 auto;
    min-height: 36px;
    padding: 0 10px;
  }

  .sort-select {
    flex-basis: auto;
    width: 100%;
  }

  .content-layout,
  .article-grid,
  .featured-aside {
    grid-template-columns: 1fr;
  }

  .article-body {
    padding: 16px;
  }

  .content-policy {
    grid-template-columns: 1fr;
  }
}
</style>

