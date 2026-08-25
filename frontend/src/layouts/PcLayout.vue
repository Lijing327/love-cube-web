<template>
  <div class="pc-layout">
    <header class="pc-header" :class="{ 'is-scrolled': isScrolled }">
      <div class="pc-nav-wrap">
        <router-link to="/pc/platform" class="pc-brand">
          <img :src="loveCubeIcon" class="pc-brand-logo" alt="" />
          <span class="pc-brand-text">Love Cube</span>
        </router-link>

        <nav class="pc-nav-links">
          <router-link v-for="item in navItems" :key="item.to" :to="item.to" :class="{ 'is-active': isActive(item.to) }">
            {{ item.label }}
          </router-link>
        </nav>

        <div class="pc-account">
          <router-link :to="pcPath('checkin')" class="pc-nav-action">✓ 签到</router-link>
          <router-link :to="pcPath('messages')" class="pc-nav-action">□ 消息通知</router-link>
          <template v-if="userStore.isLoggedIn">
            <router-link :to="PC_ME" class="pc-login-entry">{{ userStore.userInfo?.username || '个人中心' }}</router-link>
            <router-link v-if="userStore.isAdmin" to="/admin" class="pc-admin-entry">后台</router-link>
            <button type="button" class="pc-logout-btn" @click="handleLogout">退出</button>
          </template>
          <template v-else>
            <router-link to="/login" class="pc-register-entry">登录 / 注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="pc-main">
      <router-view />
    </main>

    <footer class="pc-footer">
      <div class="pc-footer-inner">
        <div class="pc-footer-brand">
          <p class="pc-footer-title">Love Cube Platform</p>
          <p class="pc-footer-desc">连接真实的人、内容与服务。</p>
        </div>
        <div class="pc-footer-cols">
          <section class="pc-footer-col">
            <h4>平台</h4>
            <router-link :to="pcPath('modules')">模块中心</router-link>
            <router-link :to="pcPath('announcements')">平台公告</router-link>
            <router-link to="/fellowship-intro">联谊</router-link>
          </section>
          <section class="pc-footer-col">
            <h4>内容</h4>
            <router-link :to="pcPath('articles')">精选内容</router-link>
            <router-link :to="PC_EVENTS">活动中心</router-link>
            <router-link to="/feedback">建议反馈</router-link>
          </section>
          <section class="pc-footer-col">
            <h4>合规</h4>
            <router-link to="/policies/terms">用户协议</router-link>
            <router-link to="/policies/privacy">隐私政策</router-link>
            <router-link to="/policies/content-policy">内容规范</router-link>
          </section>
        </div>
      </div>
      <div class="pc-footer-bottom">© {{ new Date().getFullYear() }} Love Cube. All rights reserved.</div>
    </footer>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import {
  PC_EVENTS,
  PC_HOME,
  PC_ME,
  pcPath
} from '@/constants/pcPaths.js'
import loveCubeIcon from '@/assets/brand/love-cube-icon.svg'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isScrolled = ref(false)

const navItems = [
  { to: PC_HOME, label: '首页' },
  { to: pcPath('positive-share'), label: '每日心声' },
  { to: pcPath('modules'), label: '模块中心' },
  { to: pcPath('announcements'), label: '平台公告' },
  { to: pcPath('articles'), label: '精选内容' },
  { to: PC_EVENTS, label: '活动' },
  { to: pcPath('groups'), label: '团队' },
  { to: '/fellowship-intro', label: '联谊' }
]

function isActive(basePath) {
  if (basePath === '/') return route.path === '/'
  return route.path === basePath || route.path.startsWith(`${basePath}/`)
}

function handleLogout() {
  userStore.logout()
  router.push(PC_HOME)
}

function handleScroll() {
  isScrolled.value = window.scrollY > 8
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.pc-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--lc-bg, #f8fafc);
}

.pc-header {
  position: sticky;
  top: 0;
  z-index: 200;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  backdrop-filter: blur(12px);
  transition: box-shadow 0.2s;
}

.pc-header.is-scrolled {
  box-shadow: 0 2px 16px rgba(15, 23, 42, 0.07);
}

.pc-nav-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 32px;
  height: 60px;
}

.pc-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex: 0 0 auto;
}

.pc-brand-logo {
  width: 28px;
  height: 28px;
}

.pc-brand-text {
  font-size: 16px;
  font-weight: 800;
  color: var(--lc-text, #0f172a);
  letter-spacing: -0.3px;
}

.pc-nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.pc-nav-links a {
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--lc-muted, #64748b);
  text-decoration: none;
  transition: color 0.15s, background 0.15s;
  white-space: nowrap;
}

.pc-nav-links a:hover,
.pc-nav-links a.is-active {
  color: var(--lc-blue, #2563eb);
  background: rgba(37, 99, 235, 0.06);
}

.pc-account {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.pc-nav-action {
  font-size: 13px;
  color: var(--lc-muted, #64748b);
  text-decoration: none;
  padding: 5px 10px;
  border-radius: 8px;
  transition: background 0.15s;
  white-space: nowrap;
}

.pc-nav-action:hover {
  background: #f1f5f9;
}

.pc-login-entry {
  font-size: 13px;
  font-weight: 600;
  color: var(--lc-blue, #2563eb);
  text-decoration: none;
  padding: 5px 12px;
  border-radius: 8px;
  background: rgba(37, 99, 235, 0.06);
  transition: background 0.15s;
}

.pc-login-entry:hover {
  background: rgba(37, 99, 235, 0.12);
}

.pc-admin-entry {
  font-size: 12px;
  color: #7c3aed;
  text-decoration: none;
  padding: 4px 9px;
  border-radius: 6px;
  border: 1px solid rgba(124, 58, 237, 0.2);
}

.pc-logout-btn {
  font-size: 13px;
  color: var(--lc-muted, #64748b);
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.pc-logout-btn:hover {
  background: #f1f5f9;
}

.pc-register-entry {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: var(--lc-blue, #2563eb);
  text-decoration: none;
  padding: 6px 16px;
  border-radius: 8px;
  transition: opacity 0.15s;
}

.pc-register-entry:hover {
  opacity: 0.88;
}

.pc-main {
  flex: 1;
}

.pc-footer {
  border-top: 1px solid rgba(148, 163, 184, 0.15);
  background: #fff;
  margin-top: auto;
}

.pc-footer-inner {
  display: flex;
  gap: 48px;
  align-items: flex-start;
  max-width: 1440px;
  margin: 0 auto;
  padding: 40px 32px 32px;
}

.pc-footer-brand {
  flex: 1;
}

.pc-footer-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: var(--lc-text, #0f172a);
}

.pc-footer-desc {
  margin: 0;
  font-size: 13px;
  color: var(--lc-muted, #64748b);
  line-height: 1.6;
  max-width: 320px;
}

.pc-footer-cols {
  display: flex;
  gap: 48px;
}

.pc-footer-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pc-footer-col h4 {
  margin: 0 0 4px;
  font-size: 12px;
  font-weight: 700;
  color: var(--lc-text, #0f172a);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.pc-footer-col a {
  font-size: 13px;
  color: var(--lc-muted, #64748b);
  text-decoration: none;
  transition: color 0.15s;
}

.pc-footer-col a:hover {
  color: var(--lc-blue, #2563eb);
}

.pc-footer-bottom {
  border-top: 1px solid rgba(148, 163, 184, 0.1);
  text-align: center;
  padding: 16px 32px;
  font-size: 12px;
  color: var(--lc-muted, #64748b);
}
</style>

