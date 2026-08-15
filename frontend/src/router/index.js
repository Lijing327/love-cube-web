import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { hasEventGuestToken } from '@/utils/eventGuestToken.js'
import platformRoutes from './modules/platform.routes.js'
import fellowshipRoutes from './modules/fellowship.routes.js'
import adminRoutes from './modules/admin.routes.js'
import pcRoutes from './modules/pc.routes.js'
import mobileRoutes from './modules/mobile.routes.js'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    platformRoutes,
    adminRoutes,
    fellowshipRoutes,
    pcRoutes,
    mobileRoutes,
    { path: '/vip', redirect: '/fellowship/vip' },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const token = userStore.token

  if (to.meta.requiresAuth && !token) {
    const eventId = to.params.eventId || to.params.id
    if (to.meta.allowEventGuest && eventId && hasEventGuestToken(eventId)) {
      return true
    }
    userStore.setPostLoginRedirect(to.fullPath)
    if (to.path.startsWith('/fellowship')) {
      return { path: '/fellowship/login', query: { redirect: encodeURIComponent(to.fullPath) } }
    }
    return { path: '/login', query: { redirect: encodeURIComponent(to.fullPath) } }
  }

  if (to.path.startsWith('/fellowship') && to.path !== '/fellowship' && token) {
    if (userStore.userInfo) {
      userStore.refreshCurrentUser().catch(() => null)
    } else {
      await userStore.refreshCurrentUser().catch(() => null)
    }
    if (!userStore.isFellowshipEnabled) {
      const allowedBeforeFellowship = to.matched.some((record) => record.meta.allowBeforeFellowship)
      if (!allowedBeforeFellowship) {
        return { path: '/fellowship', query: { redirect: encodeURIComponent(to.fullPath) } }
      }
    }
  }

  if (to.meta.requiresAdmin) {
    await userStore.refreshCurrentUser().catch(() => null)
    // Load fine-grained permissions (cached after first load)
    await userStore.loadAdminContext().catch(() => null)
    if (!userStore.isAdmin) {
      return { path: '/' }
    }
    // 团长（仅 GROUP_OWNER）：只能访问「我的团体」与无权限提示页，禁止进入全站运营后台其它路径
    if (userStore.isGroupStewardOnly) {
      const p = to.path
      const stewardOk =
        p === '/admin/403' || p === '/admin/my-groups' || p.startsWith('/admin/my-groups/')
      if (!stewardOk) {
        return { path: '/admin/my-groups' }
      }
    }
    // Permission-gated route check
    if (to.meta.permission && !userStore.hasPermission(to.meta.permission)) {
      return { path: '/admin/403' }
    }
  }
})

function isStaleChunkError(error) {
  const name = String(error?.name || '')
  const message = String(error?.message || error || '')
  return (
    name === 'ChunkLoadError' ||
    message.includes('Failed to fetch dynamically imported module') ||
    message.includes('Loading chunk') ||
    message.includes('Loading CSS chunk') ||
    message.includes('Unable to preload CSS')
  )
}

router.onError((error) => {
  if (!isStaleChunkError(error) || typeof window === 'undefined') return
  const key = 'lc-stale-chunk-reload'
  const last = Number(window.sessionStorage.getItem(key) || 0)
  if (last && Date.now() - last < 10000) return
  window.sessionStorage.setItem(key, String(Date.now()))
  window.location.reload()
})

export default router
