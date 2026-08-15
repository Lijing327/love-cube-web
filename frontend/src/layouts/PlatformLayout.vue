<template>
  <div class="platform-layout">
    <header class="platform-header" :class="{ 'is-scrolled': isScrolled }">
      <div class="header-main">
        <div class="nav-wrap">
          <router-link to="/" class="brand" @click="menuOpen = false">
            <span class="brand-logo" aria-hidden="true">
              <img :src="loveCubeIcon" alt="">
            </span>
            <span class="brand-copy">
              <span class="brand-text">Love Cube</span>
              <span class="brand-tag">多元连接平台</span>
            </span>
          </router-link>

          <nav class="nav-links nav-links-desktop">
            <router-link v-for="item in navItems" :key="item.to" :to="item.to" :class="{ 'is-active': isActive(item.to) }">
              {{ item.label }}
            </router-link>
          </nav>

          <form class="nav-search" role="search" @submit.prevent="handleSearch">
            <span class="nav-search-icon" aria-hidden="true">⌕</span>
            <input v-model.trim="searchKeyword" type="search" placeholder="搜索内容、用户、活动..." aria-label="搜索内容">
          </form>

          <div class="account-slot">
            <router-link to="/platform/checkin" class="nav-action"><span aria-hidden="true">✓</span>签到</router-link>
            <router-link to="/messages" class="nav-action nav-action-message"><span aria-hidden="true">□</span>消息通知<i v-if="messageBadge" class="message-badge">{{ messageBadge }}</i></router-link>
            <template v-if="userStore.isLoggedIn">
              <router-link to="/me" class="login-entry">{{ userStore.userInfo?.username || '平台个人中心' }}</router-link>
              <router-link v-if="userStore.isAdmin" to="/admin" class="admin-entry">后台</router-link>
              <button type="button" class="header-logout-btn" @click="handleLogout">退出</button>
            </template>
            <template v-else>
              <router-link to="/login" class="register-entry">登录 / 注册</router-link>
            </template>
          </div>

          <div class="mobile-header-actions">
            <!-- 移动端搜索入口：保持顶部只显示图标，点击进入内容搜索页 -->
            <button class="mobile-icon-btn" type="button" aria-label="搜索内容" @click="goMobileSearch">⌕</button>
            <button
              class="menu-toggle"
              :class="{ 'is-active': menuOpen && mobileMenuSection === 'module' }"
              type="button"
              aria-label="打开导航菜单"
              @click="toggleMobileMenu('module')"
            >
              <span></span><span></span><span></span>
            </button>
            <button
              class="menu-toggle menu-toggle-account"
              :class="{ 'is-active': menuOpen && mobileMenuSection === 'account' }"
              type="button"
              aria-label="打开账号菜单"
              @click="toggleMobileMenu('account')"
            >
              <i aria-hidden="true"></i>
            </button>
          </div>
        </div>
      </div>
    </header>

    <RouteBackButton v-if="showRouteBackButton" class="platform-route-back" />
    <main class="platform-main"><router-view /></main>

    <aside class="co-creation-toolbar" aria-label="平台工具栏">
      <button type="button" class="co-creation-trigger" @click="openCoCreationDialog"><span class="co-creation-trigger-icon" aria-hidden="true">✎</span><span> 共创计划</span></button>
    </aside>

    <transition name="dialog-fade">
      <div v-if="coCreationOpen" class="co-creation-mask" @click.self="closeCoCreationDialog">
        <section class="co-creation-dialog" role="dialog" aria-modal="true" aria-labelledby="co-creation-title">
          <button class="co-creation-close" type="button" aria-label="关闭共创反馈弹窗" @click="closeCoCreationDialog"><span aria-hidden="true"></span></button>
          <header class="co-creation-hero">
            <div class="co-creation-copy">
              <p class="co-creation-kicker">LOVE CUBE CO-CREATION</p>
              <h2 id="co-creation-title">帮我们做得更好</h2>
              <p class="co-creation-subtitle">没有大公司的流程限制，只要合理、好用、合法合规，很多想法都能快速实现。</p>
            </div>
            <div class="survey-illustration" aria-hidden="true"><span class="survey-orbit"></span><span class="survey-clipboard"><i></i><b></b><b></b><b></b></span><span class="survey-pencil"></span><span class="survey-heart"></span></div>
          </header>

          <form class="co-creation-form" @submit.prevent="handleCoCreationSubmit">
            <div class="survey-question"><div class="survey-question-title"><span class="survey-number">1</span><h3>你最关注哪个模块?</h3><em>单选</em></div><label class="survey-select"><select v-model="coCreationForm.focusModule" required><option value="" disabled>请选择你最关注的模块</option><option v-for="item in focusModuleOptions" :key="item" :value="item">{{ item }}</option></select></label></div>
            <div class="survey-question"><div class="survey-question-title"><span class="survey-number">2</span><h3>你来网站最想获得什么?</h3><em>多选</em></div><div class="co-creation-checkboxes"><label v-for="item in goalOptions" :key="item.label" class="co-creation-check-item" :class="{ 'is-selected': coCreationForm.goals.includes(item.label) }" :style="{ '--option-color': item.color, '--option-bg': item.bg }"><input :checked="coCreationForm.goals.includes(item.label)" type="checkbox" @change="toggleGoal(item.label, $event)"><span class="goal-icon" aria-hidden="true">{{ item.icon }}</span><span>{{ item.label }}</span></label></div></div>
            <div class="survey-question"><div class="survey-question-title"><span class="survey-number">3</span><h3>你觉得目前网站最需要改进什么?</h3><em>单选</em></div><label class="survey-select"><select v-model="coCreationForm.improvement" required><option value="" disabled>请选择当前最需要改进的选项</option><option v-for="item in improvementOptions" :key="item" :value="item">{{ item }}</option></select></label></div>
            <div class="survey-question"><div class="survey-question-title"><span class="survey-number">4</span><h3>你最希望网站新增什么功能?</h3><em>必填</em></div><label class="survey-textarea"><textarea v-model.trim="coCreationForm.featureSuggestion" rows="4" maxlength="200" required placeholder="例如：私信聊天、同城活动、AI助手、内容投稿、认证体系、更多本地服务等。"></textarea><span>{{ featureSuggestionCount }}/200</span></label></div>
            <div class="survey-question"><div class="survey-question-title"><span class="survey-number">5</span><h3>你还有其他想说的吗?</h3><em>选填</em></div><label class="survey-textarea"><textarea v-model.trim="coCreationForm.extraComment" rows="3" maxlength="200" placeholder="可以写下你对网站的想法、吐槽、建议或创意。"></textarea><span>{{ extraCommentCount }}/200</span></label></div>
            <div class="survey-question"><div class="survey-question-title"><span class="survey-number">6</span><h3>你在使用网站时遇到过哪些问题?</h3><em>选填</em></div><label class="survey-textarea"><textarea v-model.trim="coCreationForm.usageIssue" rows="3" maxlength="200" placeholder="例如：页面加载慢、按钮不明显、流程不清晰、某功能无法使用等。"></textarea><span>{{ usageIssueCount }}/200</span></label></div>
            <div class="survey-question"><div class="survey-question-title"><span class="survey-number">7</span><h3>如果你愿意，也可以留下联系方式</h3><em>选填</em></div><label class="survey-contact"><input v-model.trim="coCreationForm.contact" type="text" maxlength="60" placeholder="可填写微信号、手机号或邮箱，方便我们联系你"></label></div>
            <p v-if="coCreationMessage" class="co-creation-message" :class="{ 'is-error': coCreationError }">{{ coCreationMessage }}</p>
            <button class="co-creation-submit" type="submit" :disabled="coCreationSubmitting">{{ coCreationSubmitting ? '提交中...' : '提交反馈' }}</button>
            <p class="co-creation-privacy">你也可以留下联系方式；若想直接联系我，可加微信：LinXi-5152。</p>
          </form>
        </section>
      </div>
    </transition>

    <nav class="mobile-quick-nav">
      <router-link to="/platform" :class="{ 'is-active': isMobilePlatformHomeActive() }">首页</router-link>
      <router-link to="/platform/local" :class="{ 'is-active': isActive('/platform/local') }">本地</router-link>
      <router-link to="/platform/positive-share" class="mobile-publish-entry" :class="{ 'is-active': isActive('/platform/positive-share') }">发布</router-link>
      <router-link :to="groupsTabTo" :class="{ 'is-active': isGroupsTabActive() }">团体</router-link>
      <router-link to="/me" :class="{ 'is-active': isActive('/me') || isActive('/platform/me') }">我的</router-link>
    </nav>

    <footer class="platform-footer"><div class="footer-inner"><div class="footer-brand"><p class="footer-title">Love Cube Platform</p><p class="footer-desc">连接真实的人、内容与服务，打造可持续增长的多模块平台。</p></div><div class="footer-cols"><section class="footer-col"><h4>平台</h4><router-link to="/modules">模块中心</router-link><router-link to="/announcements">平台公告</router-link><router-link to="/fellowship-intro">联谊</router-link></section><section class="footer-col"><h4>内容</h4><router-link to="/articles">精选内容</router-link><router-link to="/events">活动中心</router-link><router-link to="/feedback">建议反馈</router-link></section><section class="footer-col"><h4>合规</h4><router-link to="/policies/terms">用户协议</router-link><router-link to="/policies/privacy">隐私政策</router-link><router-link to="/policies/content-policy">内容规范</router-link></section></div></div><div class="footer-bottom">© {{ new Date().getFullYear() }} Love Cube. All rights reserved.</div></footer>

    <transition name="menu-fade"><div v-if="menuOpen" class="mobile-menu-mask" @click="menuOpen = false" /></transition>
    <transition name="menu-slide">
      <nav v-if="menuOpen" class="mobile-menu-panel">
        <form class="mobile-search" role="search" @submit.prevent="handleSearch">
          <span aria-hidden="true">⌕</span>
          <input v-model.trim="searchKeyword" type="search" placeholder="搜索内容、用户、活动...">
        </form>

        <section v-if="mobileMenuSection === 'module'" class="mobile-menu-group">
          <p class="mobile-menu-group-title">模块导航</p>
          <router-link
            v-for="item in mobileModuleNavItems"
            :key="item.to"
            :to="item.to"
            :class="{ 'is-active': isActive(item.to) }"
            @click="menuOpen = false"
          >
            {{ item.label }}
          </router-link>
        </section>

        <section v-else class="mobile-menu-group">
          <p class="mobile-menu-group-title">账号管理</p>
          <router-link
            v-for="item in mobileAccountNavItems"
            :key="item.to"
            :to="item.to"
            :class="{ 'is-active': isActive(item.to) }"
            @click="menuOpen = false"
          >
            {{ item.label }}
          </router-link>
          <router-link v-if="userStore.isAdmin" to="/admin" @click="menuOpen = false">管理后台</router-link>
          <button v-if="userStore.isLoggedIn" type="button" class="mobile-logout" @click="handleLogout">退出登录</button>
        </section>
      </nav>
    </transition>

    <transition name="dialog-fade">
      <div v-if="announcementPopupVisible" class="announcement-popup-mask" @click.self="closeAnnouncementPopup">
        <section class="announcement-popup-dialog" role="dialog" aria-modal="true" aria-labelledby="announcement-popup-title">
          <button type="button" class="announcement-popup-close" aria-label="关闭公告弹窗" @click="closeAnnouncementPopup">×</button>
          <p class="announcement-popup-kicker">平台公告</p>
          <h3 id="announcement-popup-title">{{ announcementPopupItem?.title || '最新公告' }}</h3>
          <p class="announcement-popup-summary">{{ announcementPopupItem?.summary || '平台发布了新的公告内容，点击查看详情。' }}</p>
          <div class="announcement-popup-actions">
            <button type="button" class="announcement-popup-btn announcement-popup-btn-secondary" @click="closeAnnouncementPopup">关闭</button>
            <button type="button" class="announcement-popup-btn announcement-popup-btn-primary" @click="goAnnouncementDetail">查看详情</button>
          </div>
        </section>
      </div>
    </transition>

    <transition name="dialog-fade">
      <div v-if="wechatGuideVisible" class="wechat-guide-mask" @click.self="closeWechatGuide">
        <section class="wechat-guide-dialog" role="dialog" aria-modal="true" aria-labelledby="wechat-guide-title">
          <h3 id="wechat-guide-title">建议使用手机浏览器打开</h3>
          <p>你当前在微信内打开，部分功能可能受限。</p>
          <p>请点击右上角「···」，选择「在浏览器打开」。</p>
          <div class="wechat-guide-actions">
            <button type="button" class="wechat-guide-btn wechat-guide-btn-main" @click="copyCurrentLink">{{ wechatCopyDone ? '链接已复制' : '复制当前链接' }}</button>
            <button type="button" class="wechat-guide-btn" @click="closeWechatGuide">我知道了</button>
          </div>
        </section>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { submitCoCreationFeedback } from '@/api/feedback.js'
import { fetchAnnouncements } from '@/api/platformContent.js'
import { useAppBadge } from '@/composables/useAppBadge.js'
import { useUserStore } from '@/stores/user.js'
import RouteBackButton from '@/components/RouteBackButton.vue'
import loveCubeIcon from '@/assets/brand/love-cube-icon.svg'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const menuOpen = ref(false)
const mobileMenuSection = ref('module')
const isScrolled = ref(false)
const searchKeyword = ref('')
const coCreationOpen = ref(false)
const coCreationSubmitting = ref(false)
const coCreationMessage = ref('')
const coCreationError = ref(false)
const wechatGuideVisible = ref(false)
const wechatCopyDone = ref(false)
const announcementPopupVisible = ref(false)
const announcementPopupItem = ref(null)
const coCreationForm = ref({
  focusModule: '',
  goals: [],
  improvement: '',
  featureSuggestion: '',
  extraComment: '',
  usageIssue: '',
  contact: ''
})

const focusModuleOptions = ['联谊', '活动中心', '内容资讯', '平台公告', '本地服务', 'AI工具', '个人中心', '其他']
const goalOptions = [
  { label: '认识新朋友', icon: '友', color: '#7c3aed', bg: '#f1e9ff' },
  { label: '联谊', icon: '心', color: '#ec4899', bg: '#fde7f3' },
  { label: '获取信息内容', icon: '文', color: '#3b82f6', bg: '#eaf2ff' },
  { label: '参加活动', icon: '+', color: '#16a34a', bg: '#e7f8ee' },
  { label: '使用实用工具', icon: '工', color: '#f97316', bg: '#fff0e6' },
  { label: '了解本地资源', icon: '地', color: '#8b5cf6', bg: '#f0eaff' },
  { label: '随便看看', icon: '看', color: '#f59e0b', bg: '#fff7df' },
  { label: '其他（请注明）', icon: '…', color: '#64748b', bg: '#eef2f7' }
]
const improvementOptions = ['用户太少', '功能太少', '页面不够好看', '不知道怎么玩', '内容太少', '信任感不足', '操作不够方便', '其他']
const featureSuggestionCount = computed(() => coCreationForm.value.featureSuggestion.length)
const extraCommentCount = computed(() => coCreationForm.value.extraComment.length)
const usageIssueCount = computed(() => coCreationForm.value.usageIssue.length)

const navItems = [
  { to: '/', label: '首页' },
  { to: '/platform/local', label: '本地' },
  { to: '/platform/positive-share', label: '每日心声' },
  { to: '/platform/help', label: '互助广场' },
  { to: '/modules', label: '模块中心' },
  { to: '/announcements', label: '平台公告' },
  { to: '/articles', label: '精选内容' },
  { to: '/events', label: '活动' },
  { to: '/platform/groups', label: '团队' },
  { to: '/fellowship-intro', label: '联谊' }
]

const mobileModuleNavItems = [
  { to: '/platform/play', label: '全部功能' },
  { to: '/', label: '首页' },
  { to: '/platform/local', label: '本地推荐' },
  { to: '/platform/help', label: '互助' },
  { to: '/modules', label: '模块中心' },
  { to: '/announcements', label: '平台公告' },
  { to: '/articles', label: '精选内容' },
  { to: '/events', label: '活动中心' },
  { to: '/fellowship-intro', label: '联谊' }
]
const mobileAccountNavItems = computed(() => {
  const accountItems = [{ to: '/messages', label: '消息通知中心' }]
  if (userStore.isLoggedIn) {
    accountItems.unshift({ to: '/me', label: '账号中心' })
    return accountItems
  }
  accountItems.unshift({ to: '/login', label: '登录 / 注册' })
  return accountItems
})
const { badgeText: messageBadge } = useAppBadge()
const navHomePaths = computed(() => new Set([
  ...navItems.map(item => item.to),
  '/feedback',
  '/platform/events'
]))
const showRouteBackButton = computed(() => !navHomePaths.value.has(route.path))
const WECHAT_GUIDE_KEY = 'platform-wechat-guide-dismissed-date'
const ANNOUNCEMENT_DISMISS_PREFIX = 'platform-announcement-dismissed'

watch(() => route.fullPath, () => {
  menuOpen.value = false
  userStore.syncCurrentUser()
})

const groupsTabTo = computed(() => (
  userStore.isLoggedIn ? '/platform/me/groups' : '/platform/groups'
))

function isActive(basePath) {
  if (basePath === '/') return route.path === '/'
  return route.path === basePath || route.path.startsWith(`${basePath}/`)
}

function isGroupsTabActive() {
  const p = route.path
  return (
    p === '/me/groups' ||
    p === '/platform/me/groups' ||
    p === '/platform/groups' ||
    p.startsWith('/platform/groups/')
  )
}

/** 底部「首页」仅高亮真正的平台首页，避免 /platform/local 等子路径误匹配 isActive('/platform') */
function isMobilePlatformHomeActive() {
  const p = route.path
  return (
    p === '/' ||
    p === '/platform' ||
    p === '/platform/changelog' ||
    p === '/platform/pending-updates'
  )
}

function handleLogout() {
  userStore.logout()
  menuOpen.value = false
  router.push('/')
}

function handleSearch() {
  const keyword = searchKeyword.value.trim()
  menuOpen.value = false
  if (!keyword) {
    router.push('/articles')
    return
  }
  router.push({ path: '/articles', query: { keyword } })
}

function toggleMobileMenu(section) {
  if (menuOpen.value && mobileMenuSection.value === section) {
    menuOpen.value = false
    return
  }
  mobileMenuSection.value = section
  menuOpen.value = true
}

function goMobileSearch() {
  menuOpen.value = false
  router.push('/articles')
}

function openCoCreationDialog() {
  coCreationOpen.value = true
  coCreationMessage.value = ''
  coCreationError.value = false
}

function closeCoCreationDialog() {
  if (coCreationSubmitting.value) return
  coCreationOpen.value = false
}

function resetCoCreationForm() {
  coCreationForm.value = {
    focusModule: '',
    goals: [],
    improvement: '',
    featureSuggestion: '',
    extraComment: '',
    usageIssue: '',
    contact: ''
  }
}

function toggleGoal(option, event) {
  const checked = Boolean(event?.target?.checked)
  const values = [...coCreationForm.value.goals]
  if (checked && !values.includes(option)) {
    values.push(option)
  }
  if (!checked && values.includes(option)) {
    coCreationForm.value.goals = values.filter(item => item !== option)
    return
  }
  coCreationForm.value.goals = values
}

function waitMockSubmit() {
  return new Promise(resolve => {
    window.setTimeout(resolve, 420)
  })
}

async function handleCoCreationSubmit() {
  coCreationMessage.value = ''
  coCreationError.value = false
  const focusModule = coCreationForm.value.focusModule
  const goals = Array.isArray(coCreationForm.value.goals) ? coCreationForm.value.goals : []
  const improvement = coCreationForm.value.improvement
  const featureSuggestion = coCreationForm.value.featureSuggestion.trim()
  const extraComment = coCreationForm.value.extraComment.trim()
  const usageIssue = coCreationForm.value.usageIssue.trim()
  const contact = coCreationForm.value.contact.trim()

  if (!focusModule || !goals.length || !improvement || !featureSuggestion) {
    coCreationError.value = true
    coCreationMessage.value = '请先完成必填项：Q1、Q2、Q3、Q4'
    return
  }

  coCreationSubmitting.value = true
  try {
    const content = [
      `Q1-最关注模块：${focusModule}`,
      `Q2-来站目标：${goals.join('、')}`,
      `Q3-最需要改进：${improvement}`,
      `Q4-新增功能建议：${featureSuggestion}`,
      extraComment ? `Q5-其他想法：${extraComment}` : '',
      usageIssue ? `Q6-使用问题反馈：${usageIssue}` : ''
    ].filter(Boolean).join('\n')

    try {
      await submitCoCreationFeedback({
        module: focusModule,
        goals,
        missing: improvement,
        content,
        contact
      })
    } catch (error) {
      await waitMockSubmit()
    }
    coCreationMessage.value = '感谢参与平台共建，你的建议已记录。'
    resetCoCreationForm()
    window.setTimeout(() => {
      coCreationOpen.value = false
      coCreationMessage.value = ''
    }, 1400)
  } catch (error) {
    coCreationError.value = true
    coCreationMessage.value = error.message || '提交失败，请稍后再试'
  } finally {
    coCreationSubmitting.value = false
  }
}
function handleScroll() {
  isScrolled.value = window.scrollY > 8
}

function isMobileWechat() {
  const ua = window.navigator.userAgent.toLowerCase()
  const isWechat = ua.includes('micromessenger')
  const isMobile = /android|iphone|ipad|ipod|mobile/i.test(ua)
  return isWechat && isMobile
}

function closeWechatGuide() {
  wechatGuideVisible.value = false
  try {
    localStorage.setItem(WECHAT_GUIDE_KEY, new Date().toDateString())
  } catch (error) {
    // ignore storage exceptions
  }
}

async function copyCurrentLink() {
  const currentUrl = window.location.href
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(currentUrl)
    } else {
      const input = document.createElement('input')
      input.value = currentUrl
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    wechatCopyDone.value = true
  } catch (error) {
    wechatCopyDone.value = false
  }
}

function setupWechatGuide() {
  if (!isMobileWechat()) return
  const today = new Date().toDateString()
  const dismissedDate = localStorage.getItem(WECHAT_GUIDE_KEY)
  if (dismissedDate === today) return
  wechatGuideVisible.value = true
}

function getAnnouncementDismissKey(userId, announcementId) {
  return `${ANNOUNCEMENT_DISMISS_PREFIX}:${userId}:${announcementId}`
}

function closeAnnouncementPopup() {
  const userId = String(userStore.userId || '')
  const announcementId = String(announcementPopupItem.value?.id || '')
  if (userId && announcementId) {
    try {
      localStorage.setItem(getAnnouncementDismissKey(userId, announcementId), '1')
    } catch (error) {
      // ignore storage exceptions
    }
  }
  announcementPopupVisible.value = false
}

function goAnnouncementDetail() {
  const announcementId = String(announcementPopupItem.value?.id || '')
  closeAnnouncementPopup()
  if (announcementId) {
    router.push(`/announcements/${announcementId}`)
    return
  }
  router.push('/announcements')
}

async function maybeShowAnnouncementPopup() {
  if (!userStore.isLoggedIn) return
  const userId = String(userStore.userId || '')
  if (!userId) return
  try {
    const list = await fetchAnnouncements({ status: 'published' })
    const latest = Array.isArray(list)
      ? list.find(item => Boolean(item?.popupEnabled))
      : null
    const announcementId = String(latest?.id || '')
    if (!announcementId) return
    const dismissed = localStorage.getItem(getAnnouncementDismissKey(userId, announcementId))
    if (dismissed) return
    announcementPopupItem.value = latest
    announcementPopupVisible.value = true
  } catch (error) {
    // ignore announcement popup failure
  }
}

onMounted(() => {
  handleScroll()
  setupWechatGuide()
  maybeShowAnnouncementPopup()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

watch(
  () => [userStore.isLoggedIn, userStore.userId],
  ([loggedIn, userId], [prevLoggedIn, prevUserId]) => {
    if (!loggedIn) {
      announcementPopupVisible.value = false
      announcementPopupItem.value = null
      return
    }
    if (!userId) return
    if (!prevLoggedIn || userId !== prevUserId) {
      maybeShowAnnouncementPopup()
    }
  }
)

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.platform-layout {
  min-height: 100vh;
  background: #f7f9fd;
  color: #111827;
}

.platform-header {
  position: sticky;
  top: 0;
  z-index: 100;
  padding-top: env(safe-area-inset-top, 0px);
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  -webkit-backdrop-filter: none;
  backdrop-filter: none;
  transition: box-shadow 0.22s ease;
}

.platform-header.is-scrolled {
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.header-main,
.nav-wrap,
.footer-inner {
  width: 100%;
  margin: 0 auto;
}

.nav-wrap {
  min-height: 58px;
  padding: 0 16px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) minmax(180px, 320px) auto auto;
  align-items: center;
  gap: 12px;
  margin: 0 auto;
  width: min(100% - 32px, 1720px);
  border-radius: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 152px;
  color: inherit;
  text-decoration: none;
}

.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid rgba(251, 207, 232, 0.8);
  box-shadow: 0 8px 18px rgba(236, 72, 153, 0.16);
}

.brand-logo img {
  width: 100%;
  height: 100%;
  display: block;
}

.brand-copy {
  display: inline-flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-text {
  font-size: 18px;
  font-weight: 800;
  color: #ec4899;
}

.brand-tag {
  margin-top: 1px;
  font-size: 10px;
  color: #64748b;
  font-weight: 600;
}

.nav-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  min-width: 0;
  overflow: hidden;
}

.nav-links a {
  position: relative;
  color: #334155;
  text-decoration: none;
  font-weight: 700;
  font-size: 15px;
  line-height: 1;
  white-space: nowrap;
  padding: 20px 2px 18px;
  transition: color 0.2s ease, opacity 0.2s ease;
  opacity: 0.9;
}

.nav-links a:hover,
.nav-links a.router-link-exact-active,
.nav-links a.is-active {
  color: var(--lc-blue);
  opacity: 1;
}

.nav-links a.router-link-exact-active::after,
.nav-links a.is-active::after {
  content: '';
  position: absolute;
  left: 6px;
  right: 6px;
  bottom: 6px;
  height: 2px;
  border-radius: 999px;
  background: var(--lc-blue);
}

.nav-search,
.mobile-search {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #edf1f7;
  background: #f8fafc;
  color: #7c8a9d;
}

.nav-search {
  height: 34px;
  border-radius: 999px;
  padding: 0 12px;
  justify-self: stretch;
  width: 100%;
  min-width: 0;
  border: 1px solid #dbeafe;
  background: #f8fbff;
  color: #64748b;
  backdrop-filter: blur(10px);
}

.account-slot {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.nav-search-icon {
  color: #94a3b8;
  font-size: 14px;
  line-height: 1;
}

.nav-search input,
.mobile-search input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.nav-search input::placeholder,
.mobile-search input::placeholder {
  color: #94a3b8;
}

.nav-action,
.admin-entry,
.login-entry,
.register-entry {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 30px;
  color: #334155;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.header-logout-btn {
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
  color: var(--lc-muted);
  border-radius: 999px;
  height: 30px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.header-logout-btn:hover {
  background: var(--lc-blue-light);
  color: var(--lc-blue);
}

.nav-action:hover,
.admin-entry:hover,
.login-entry:hover,
.register-entry:hover {
  color: #ffffff;
}

.login-entry,
.register-entry {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message-badge {
  display: grid;
  place-items: center;
  min-width: 16px;
  height: 16px;
  padding: 0 5px;
  border-radius: 999px;
  background: #ff4d7f;
  color: #fff;
  font-size: 11px;
  font-style: normal;
}

.register-entry {
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.52);
  background: rgba(255, 255, 255, 0.18);
}

.menu-toggle {
  display: inline-flex;
  border: 1px solid #e1e8f2;
  background: #fff;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
  color: #243449;
  cursor: pointer;
}

.menu-toggle.is-active {
  border-color: #c7d2fe;
  background: #eef2ff;
}

.mobile-header-actions {
  display: none;
  align-items: center;
  gap: 6px;
}

.mobile-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid #e1e8f2;
  border-radius: 10px;
  background: #ffffff;
  color: #1e3a8a;
  font-size: 18px;
  font-weight: 700;
}

.menu-toggle span {
  width: 18px;
  height: 2px;
  border-radius: 999px;
  background: #243449;
}

.menu-toggle-account i {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-radius: 50%;
  position: relative;
}

.menu-toggle-account i::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -8px;
  width: 14px;
  height: 7px;
  border: 2px solid currentColor;
  border-top: 0;
  border-radius: 0 0 999px 999px;
  transform: translateX(-50%);
}

.platform-main {
  min-height: calc(100vh - 74px - 212px);
}

.co-creation-toolbar {
  position: fixed;
  right: 18px;
  top: 48%;
  z-index: var(--lc-z-sticky);
  transform: translateY(-50%);
}

.co-creation-trigger {
  display: grid;
  justify-items: center;
  gap: 7px;
  width: 54px;
  padding: 12px 8px;
  border: 1px solid rgba(191, 219, 254, 0.95);
  border-radius: 8px;
  color: var(--lc-blue);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--lc-shadow);
  font-size: 13px;
  font-weight: 900;
  line-height: 1.2;
  cursor: pointer;
  transition: var(--lc-transition);
}

.co-creation-trigger:hover {
  color: var(--lc-surface);
  background: linear-gradient(135deg, var(--lc-blue), var(--lc-blue-mid));
  box-shadow: var(--lc-shadow-blue);
  transform: translateY(-2px);
}

.co-creation-trigger-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: var(--lc-surface);
  background: linear-gradient(135deg, var(--lc-pink), var(--lc-blue));
  font-size: 14px;
}

.co-creation-mask {
  position: fixed;
  inset: 0;
  z-index: var(--lc-z-modal);
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(226, 238, 255, 0.78);
  backdrop-filter: blur(8px);
}

.co-creation-dialog {
  position: relative;
  width: min(100%, 920px);
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 48px 44px 34px;
  border: 1px solid rgba(219, 234, 254, 0.95);
  border-radius: 16px;
  background:
    radial-gradient(circle at 83% 0%, rgba(219, 234, 254, 0.78), transparent 25%),
    radial-gradient(circle at 72% 12%, rgba(191, 219, 254, 0.7), transparent 17%),
    var(--lc-surface);
  box-shadow: 0 26px 70px rgba(37, 99, 235, 0.16);
}

.co-creation-close {
  position: absolute;
  top: 22px;
  right: 22px;
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border: 1px solid rgba(191, 219, 254, 0.95);
  border-radius: 50%;
  color: var(--lc-blue);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.12);
  cursor: pointer;
}

.co-creation-close span,
.co-creation-close span::after {
  display: block;
  width: 24px;
  height: 3px;
  border-radius: 999px;
  background: currentColor;
}

.co-creation-close span {
  transform: rotate(45deg);
}

.co-creation-close span::after {
  content: '';
  transform: rotate(90deg);
}

.co-creation-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;
  align-items: center;
  min-height: 170px;
  margin-bottom: 24px;
}

.co-creation-kicker {
  margin: 0 0 18px;
  color: var(--lc-blue);
  font-size: 17px;
  font-weight: 900;
  line-height: 1.2;
}

.co-creation-dialog h2 {
  margin: 0;
  color: var(--lc-text);
  font-size: 38px;
  line-height: 1.12;
  font-weight: 950;
}

.co-creation-subtitle {
  margin: 18px 0 0;
  color: #5f7194;
  font-size: 18px;
  line-height: 1.6;
  font-weight: 700;
}

.survey-illustration {
  position: relative;
  height: 170px;
}

.survey-orbit {
  position: absolute;
  inset: 16px 28px 14px 18px;
  border-radius: 48% 52% 44% 56%;
  background: linear-gradient(135deg, rgba(191, 219, 254, 0.72), rgba(239, 246, 255, 0.28));
}

.survey-clipboard {
  position: absolute;
  left: 88px;
  top: 28px;
  width: 92px;
  height: 116px;
  border: 8px solid #4d8df7;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 18px 26px rgba(37, 99, 235, 0.26);
  transform: rotate(5deg);
}

.survey-clipboard i {
  position: absolute;
  left: 22px;
  top: -22px;
  width: 48px;
  height: 22px;
  border-radius: 14px 14px 8px 8px;
  background: linear-gradient(135deg, #7bb4ff, #3b82f6);
}

.survey-clipboard b {
  position: relative;
  display: block;
  width: 46px;
  height: 7px;
  margin: 18px 0 0 28px;
  border-radius: 999px;
  background: #c7d8f8;
}

.survey-clipboard b::before {
  content: '';
  position: absolute;
  left: -18px;
  top: -5px;
  width: 11px;
  height: 18px;
  border: solid #6da3ff;
  border-width: 0 4px 4px 0;
  transform: rotate(45deg);
}

.survey-pencil {
  position: absolute;
  left: 164px;
  top: 70px;
  width: 18px;
  height: 94px;
  border-radius: 999px;
  background: linear-gradient(180deg, #2563eb 0 22%, #ffbd6b 22% 80%, #1d4ed8 80%);
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.25);
  transform: rotate(24deg);
}

.survey-heart {
  position: absolute;
  right: 34px;
  top: 62px;
  display: grid;
  place-items: center;
  width: 66px;
  height: 54px;
  border-radius: 14px 14px 14px 4px;
  background: linear-gradient(135deg, #ff9aa9, #ff6179);
  box-shadow: 0 16px 24px rgba(236, 72, 153, 0.18);
}

.survey-heart::before {
  content: '';
  color: #ffffff;
  font-size: 30px;
  line-height: 1;
}

.co-creation-form {
  display: grid;
  gap: 30px;
}

.survey-question {
  display: grid;
  gap: 16px;
}

.survey-question-title {
  display: flex;
  align-items: center;
  gap: 14px;
}

.survey-number {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  border-radius: 8px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f8ff7, #2563eb);
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.24);
  font-size: 18px;
  font-weight: 900;
}

.survey-question-title h3 {
  margin: 0;
  color: var(--lc-text);
  font-size: 23px;
  line-height: 1.25;
  font-weight: 900;
}

.survey-question-title em {
  color: #647899;
  font-size: 16px;
  font-style: normal;
  font-weight: 800;
}

.survey-select {
  position: relative;
  display: block;
}

.survey-select::after {
  content: '';
  position: absolute;
  right: 22px;
  top: 50%;
  width: 10px;
  height: 10px;
  border: solid var(--lc-text);
  border-width: 0 3px 3px 0;
  pointer-events: none;
  transform: translateY(-70%) rotate(45deg);
}

.survey-select select,
.survey-contact input,
.survey-textarea textarea {
  width: 100%;
  border: 1px solid #d8e1ee;
  border-radius: 8px;
  color: var(--lc-text);
  background: rgba(255, 255, 255, 0.86);
  font: inherit;
  font-weight: 700;
  outline: 0;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.survey-select select {
  height: 64px;
  padding: 0 56px 0 20px;
  appearance: none;
  color: #64748b;
  font-size: 20px;
}

.survey-textarea {
  position: relative;
  display: block;
}

.survey-contact {
  display: block;
}

.survey-contact input {
  height: 64px;
  padding: 0 20px;
  color: #41516c;
  font-size: 18px;
}

.survey-textarea textarea {
  min-height: 142px;
  padding: 20px 20px 42px;
  color: #41516c;
  font-size: 18px;
  line-height: 1.7;
  resize: vertical;
}

.survey-textarea span {
  position: absolute;
  right: 18px;
  bottom: 14px;
  color: #7b8aa4;
  font-size: 16px;
  font-weight: 700;
}

.survey-select select:focus,
.survey-contact input:focus,
.survey-textarea textarea:focus {
  border-color: #8bb5ff;
  background: var(--lc-surface);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}

.co-creation-checkboxes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.co-creation-check-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 22px;
  min-height: 80px;
  padding: 12px 16px;
  border: 1px solid #dce5f2;
  border-radius: 8px;
  color: var(--lc-text);
  background: rgba(255, 255, 255, 0.74);
  font-size: 20px;
  font-weight: 900;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.co-creation-check-item:hover,
.co-creation-check-item.is-selected {
  border-color: rgba(37, 99, 235, 0.42);
  box-shadow: 0 12px 26px rgba(37, 99, 235, 0.12);
  transform: translateY(-1px);
}

.co-creation-check-item input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.goal-icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  flex: 0 0 auto;
  border-radius: 12px;
  color: var(--option-color);
  background: var(--option-bg);
  font-size: 28px;
  line-height: 1;
}

.co-creation-message {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  color: var(--lc-green);
  background: var(--lc-green-light);
  font-size: 14px;
  font-weight: 800;
}

.co-creation-message.is-error {
  color: var(--lc-red);
  background: var(--lc-red-light);
}

.co-creation-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 58px;
  border: 0;
  border-radius: 8px;
  color: var(--lc-surface);
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.22);
  font-size: 20px;
  font-weight: 900;
  cursor: pointer;
}

.co-creation-submit:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.co-creation-privacy {
  margin: -18px 0 0;
  color: #7b8aa4;
  font-size: 15px;
  font-weight: 800;
  text-align: center;
}

.co-creation-privacy::before {
  content: '';
  margin-right: 6px;
  color: #a8b6ca;
}

.platform-route-back {
  position: sticky;
  top: 86px;
  z-index: 60;
  margin: 8px 0 0 12px;
  width: fit-content;
}

.platform-route-back:hover {
  border-color: var(--lc-blue-border);
  color: var(--lc-blue);
}

.mobile-quick-nav {
  display: none;
}

.platform-footer {
  margin-top: 0;
  border-top: 0;
  background: linear-gradient(135deg, #0f1f36, #12284b);
}

.footer-inner {
  width: min(100% - 32px, 1720px);
  padding: 42px 0 30px;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 2fr);
  gap: 30px;
  color: rgba(226, 232, 240, 0.76);
}

.footer-title {
  margin: 0;
  font-size: 20px;
  font-weight: 900;
  color: #ffffff;
}

.footer-desc {
  margin: 12px 0 0;
  line-height: 1.8;
  font-size: 14px;
}

.footer-cols {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.footer-col h4 {
  margin: 0 0 12px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 800;
}

.footer-col a {
  display: block;
  margin-top: 8px;
  color: rgba(226, 232, 240, 0.72);
  text-decoration: none;
  font-size: 13px;
}

.footer-col a:hover {
  color: #ffffff;
}

.footer-bottom {
  width: min(100% - 32px, 1720px);
  margin: 0 auto;
  border-top: 1px solid rgba(148, 163, 184, 0.22);
  color: rgba(226, 232, 240, 0.54);
  font-size: 12px;
  padding: 14px 0 calc(14px + env(safe-area-inset-bottom));
  text-align: center;
}

.mobile-logout {
  border: 1px solid #ffd8e3;
  color: #e84f73;
  background: #fff8fa;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  text-align: left;
  padding: 12px 10px;
}

.mobile-menu-group {
  display: grid;
  gap: 6px;
}

.mobile-menu-group-title {
  margin: 2px 2px 4px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
}

@media (min-width: 768px) and (max-width: 1199px) {
  .platform-header {
    top: 0;
  }

  .nav-wrap {
    grid-template-columns: auto 1fr auto;
    gap: 14px;
    padding: 0 18px;
    width: calc(100% - 24px);
    border-radius: 16px;
  }

  .nav-links-desktop,
  .account-slot {
    display: none;
  }

  .nav-search {
    width: min(100%, 360px);
    justify-self: stretch;
  }

  .menu-toggle {
    display: inline-flex;
  }

  .mobile-header-actions {
    display: inline-flex;
  }

  .mobile-icon-btn {
    display: none;
  }

  .footer-inner {
    grid-template-columns: 1fr;
  }

  .mobile-menu-mask {
    position: fixed;
    inset: 0;
    z-index: 100;
    background: rgba(15, 23, 42, 0.35);
  }

  .mobile-menu-panel {
    position: fixed;
    top: 94px;
    left: 32px;
    right: 32px;
    z-index: 101;
    display: grid;
    gap: 10px;
    padding: 14px;
    border: 1px solid #dbeafe;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.96);
    box-shadow: 0 20px 46px rgba(15, 23, 42, 0.18);
    backdrop-filter: none;
  }

  .mobile-search {
    min-height: 44px;
    border-radius: 8px;
    padding: 0 12px;
  }

  .mobile-menu-group {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .mobile-menu-group-title {
    grid-column: 1 / -1;
  }

  .mobile-menu-panel a {
    color: #334155;
    font-size: 15px;
    font-weight: 700;
    text-decoration: none;
    padding: 12px;
    border-radius: 8px;
  }

  .mobile-logout {
    padding: 12px;
  }

  .mobile-menu-panel a.router-link-exact-active,
  .mobile-menu-panel a.is-active {
    background: #fff2f6;
    color: #e84f73;
  }
}

@media (max-width: 767px) {
  .platform-header {
    top: 0;
    padding: env(safe-area-inset-top, 0px) 0 0;
    background: #ffffff;
    -webkit-backdrop-filter: none;
    backdrop-filter: none;
  }

  .nav-wrap {
    grid-template-columns: 1fr auto;
    min-height: 56px;
    padding: 0 12px;
    gap: 8px;
    width: 100%;
    border-radius: 0;
    background: #ffffff;
    box-shadow: none;
  }

  .brand-logo {
    width: 30px;
    height: 30px;
  }

  .brand-text {
    font-size: 17px;
  }

  .brand-tag {
    display: none;
  }

  .menu-toggle {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    gap: 4px;
  }

  .nav-links-desktop,
  .nav-search,
  .account-slot {
    display: none;
  }

  .mobile-header-actions {
    display: inline-flex;
  }

  .platform-main {
    min-height: calc(100vh - 74px);
    /* 为固定底栏预留空间，避免表单/列表末尾操作被遮挡 */
    padding-bottom: calc(72px + env(safe-area-inset-bottom, 0px));
  }

  .co-creation-toolbar {
    right: 10px;
    top: auto;
    bottom: calc(78px + env(safe-area-inset-bottom));
    transform: none;
  }

  .co-creation-trigger {
    width: auto;
    min-height: 44px;
    grid-auto-flow: column;
    align-items: center;
    padding: 8px 11px 8px 8px;
    border-radius: 999px;
    font-size: 12px;
  }

  .co-creation-trigger:hover {
    transform: none;
  }

  .co-creation-trigger-icon {
    width: 28px;
    height: 28px;
  }

  .co-creation-mask {
    align-items: start;
    overflow-y: auto;
    padding: calc(8px + env(safe-area-inset-top)) 10px calc(66px + env(safe-area-inset-bottom));
  }

  .co-creation-dialog {
    width: 100%;
    max-height: none;
    min-height: min(620px, calc(100dvh - 20px - env(safe-area-inset-bottom)));
    padding: 22px 14px 14px;
    overflow: visible;
  }

  .co-creation-close {
    top: 10px;
    right: 10px;
    width: 34px;
    height: 34px;
  }

  .co-creation-hero {
    grid-template-columns: 1fr;
    gap: 8px;
    min-height: 0;
    margin-bottom: 10px;
  }

  .survey-illustration {
    display: none;
  }

  .co-creation-kicker {
    margin-bottom: 6px;
    padding-right: 38px;
    font-size: 12px;
  }

  .co-creation-dialog h2 {
    font-size: 21px;
    line-height: 1.2;
  }

  .co-creation-subtitle {
    margin-top: 6px;
    font-size: 12px;
    line-height: 1.55;
  }

  .co-creation-form {
    gap: 14px;
  }

  .survey-question {
    gap: 8px;
  }

  .survey-question-title {
    gap: 8px;
    align-items: flex-start;
  }

  .survey-number {
    width: 22px;
    height: 22px;
    font-size: 13px;
  }

  .survey-question-title h3 {
    flex: 1;
    font-size: 16px;
    line-height: 1.35;
  }

  .survey-question-title em {
    font-size: 12px;
    line-height: 22px;
  }

  .survey-select select {
    height: 46px;
    padding: 0 42px 0 12px;
    font-size: 14px;
  }

  .survey-contact input {
    height: 46px;
    padding: 0 12px;
    font-size: 14px;
  }

  .co-creation-checkboxes {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .co-creation-check-item {
    min-height: 52px;
    gap: 10px;
    padding: 8px 10px;
    font-size: 14px;
  }

  .goal-icon {
    width: 34px;
    height: 34px;
    border-radius: 8px;
    font-size: 18px;
  }

  .survey-textarea textarea {
    min-height: 96px;
    padding: 10px 10px 28px;
    font-size: 14px;
    line-height: 1.5;
  }

  .survey-textarea span {
    right: 10px;
    bottom: 8px;
    font-size: 12px;
  }

  .co-creation-submit {
    height: 44px;
    font-size: 15px;
  }

  .co-creation-privacy {
    margin-top: -6px;
    font-size: 11px;
  }

  .platform-route-back {
    top: 68px;
    margin: 4px 0 0 8px;
  }

  .mobile-quick-nav {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 80;
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    align-items: center;
    gap: 4px;
    min-height: 60px;
    padding: 8px 10px calc(8px + env(safe-area-inset-bottom));
    border-top: 1px solid #dbeafe;
    background: rgba(255, 255, 255, 0.98);
    box-shadow: 0 -8px 24px rgba(15, 23, 42, 0.08);
  }

  .mobile-quick-nav a {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #64748b;
    font-size: 10px;
    font-weight: 700;
    text-decoration: none;
  }

  .mobile-quick-nav .mobile-publish-entry {
    width: 64px;
    height: 36px;
    margin: -12px auto 0;
    border-radius: 999px;
    color: #ffffff;
    background: var(--lc-blue);
    box-shadow: var(--lc-shadow-blue);
  }

  .mobile-quick-nav a.router-link-exact-active,
  .mobile-quick-nav a.is-active {
    color: #4f46e5;
  }

  .mobile-quick-nav .mobile-publish-entry.router-link-exact-active,
  .mobile-quick-nav .mobile-publish-entry.is-active {
    color: #ffffff;
    background: var(--lc-blue-mid);
  }

  .platform-footer {
    display: none;
  }

  .footer-inner {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 28px 16px 18px;
  }

  .footer-cols {
    grid-template-columns: 1fr 1fr;
  }

  .mobile-menu-mask {
    position: fixed;
    inset: 0;
    z-index: 100;
    background: rgba(15, 23, 42, 0.35);
  }

  .mobile-menu-panel {
    position: fixed;
    top: calc(82px + env(safe-area-inset-top));
    left: 12px;
    right: 12px;
    z-index: 101;
    display: grid;
    gap: 10px;
    padding: 12px;
    border: 1px solid #dbeafe;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.96);
    box-shadow: 0 16px 32px rgba(15, 23, 42, 0.16);
    backdrop-filter: none;
  }

  .mobile-search {
    min-height: 44px;
    margin-bottom: 0;
    padding: 0 12px;
    border-radius: 8px;
  }

  .mobile-menu-group {
    gap: 6px;
  }

  .mobile-menu-panel a {
    color: #334155;
    font-size: 15px;
    font-weight: 700;
    text-decoration: none;
    padding: 11px 9px;
    border-radius: 8px;
  }

  .mobile-menu-panel a.router-link-exact-active,
  .mobile-menu-panel a.is-active {
    background: #fff2f6;
    color: #e84f73;
  }

  .mobile-logout {
    width: 100%;
  }
}

.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.18s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
}

.menu-slide-enter-active,
.menu-slide-leave-active {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.menu-slide-enter-from,
.menu-slide-leave-to {
  transform: translateY(-8px);
  opacity: 0;
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.18s ease;
}

.dialog-fade-leave-active,
.dialog-fade-leave-to,
.menu-fade-leave-active,
.menu-fade-leave-to {
  pointer-events: none;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

.announcement-popup-mask {
  position: fixed;
  inset: 0;
  z-index: 215;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.5);
}

.announcement-popup-dialog {
  position: relative;
  width: min(92vw, 460px);
  border-radius: 16px;
  border: 1px solid #fbcfe8;
  background: #ffffff;
  padding: 24px 20px 18px;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.28);
}

.announcement-popup-close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  background: #f1f5f9;
  color: #334155;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.announcement-popup-kicker {
  margin: 0 0 8px;
  color: #e84f73;
  font-size: 12px;
  font-weight: 800;
}

.announcement-popup-dialog h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.35;
}

.announcement-popup-summary {
  margin: 10px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.65;
}

.announcement-popup-actions {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.announcement-popup-btn {
  border-radius: 999px;
  height: 40px;
  border: 1px solid #dbeafe;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.announcement-popup-btn-secondary {
  background: #ffffff;
  color: #334155;
}

.announcement-popup-btn-primary {
  border-color: #e84f73;
  background: linear-gradient(90deg, #ef5ca7 0%, #8f96f8 100%);
  color: #ffffff;
}

.wechat-guide-mask {
  position: fixed;
  inset: 0;
  z-index: 220;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.55);
}

.wechat-guide-dialog {
  width: min(92vw, 360px);
  border-radius: 16px;
  background: #ffffff;
  padding: 18px 16px 14px;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.28);
}

.wechat-guide-dialog h3 {
  margin: 0 0 10px;
  font-size: 18px;
  line-height: 1.35;
  color: #0f172a;
}

.wechat-guide-dialog p {
  margin: 0 0 8px;
  color: #475569;
  font-size: 14px;
  line-height: 1.55;
}

.wechat-guide-actions {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.wechat-guide-btn {
  border: 1px solid #d0dbe9;
  border-radius: 999px;
  height: 40px;
  background: #ffffff;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.wechat-guide-btn-main {
  border-color: #e84f73;
  background: linear-gradient(90deg, #ef5ca7 0%, #8f96f8 100%);
  color: #ffffff;
}
</style>

