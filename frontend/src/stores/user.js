import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { storage } from '@/utils/storage.js'
import { login as loginApi, register as registerApi } from '@/api/auth.js'
import {
  activateFellowship as activateFellowshipApi,
  clearMeCache,
  deactivateFellowship as deactivateFellowshipApi,
  getMeCached,
  updateFellowshipMatchVisibility as updateFellowshipMatchVisibilityApi
} from '@/api/user.js'
import { getAdminAuthContext } from '@/api/adminContent.js'
import { useFellowshipProfileStore } from '@/stores/fellowshipProfile.js'
import { userAvatarUrlFromApi } from '@/utils/displayFields.js'

const REDIRECT_KEY = 'postLoginRedirect'

function normalizeUser(raw) {
  if (!raw) return null
  const id = String(raw.userId ?? raw.id ?? '')
  return {
    id,
    userId: id,
    username: raw.username ?? raw.nickname ?? '',
    nickname: raw.nickname ?? raw.username ?? '',
    phone: raw.phone ?? raw.phoneNumber ?? '',
    email: raw.email ?? '',
    role: raw.role ?? 'user',
    status: raw.status ?? 'active',
    verificationStatus: raw.verificationStatus ?? 'none',
    verificationRejectReason: raw.verificationRejectReason ?? '',
    photoVerified: Boolean(raw.photoVerified),
    realnameVerified: Boolean(raw.realnameVerified),
    avatar: userAvatarUrlFromApi(raw) || '',
    bio: raw.bio ?? raw.signature ?? '',
    location: raw.location ?? '',
    gender: raw.gender ?? '',
    age: raw.age ?? null,
    maritalStatus: raw.maritalStatus ?? '',
    birthday: raw.birthday ?? '',
    height: raw.height ?? '',
    fellowshipEnabled: Boolean(raw.fellowshipEnabled),
    fellowshipMatchVisible: Boolean(raw.fellowshipMatchVisible),
    photos: Array.isArray(raw.photos) ? raw.photos.filter((u) => u && String(u).trim()) : []
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(storage.get('token') || '')
  const userId = ref(storage.get('userId') || '')
  const id = ref(storage.get('userId') || '')
  const nickname = ref(storage.get('nickname') || '')
  const avatar = ref(storage.get('avatar') || '')
  const userInfo = ref(null)
  const adminContext = ref(null)

  function setAuth(newToken, newUserId) {
    clearMeCache()
    token.value = newToken
    userId.value = String(newUserId)
    storage.set('token', newToken)
    storage.set('userId', String(newUserId))
  }

  function setUserInfo(info) {
    userInfo.value = info
    id.value = String(info?.userId || info?.id || '')
    nickname.value = info?.nickname || info?.username || ''
    avatar.value = userAvatarUrlFromApi(info) || String(info?.avatar || '').trim()
    storage.set('nickname', nickname.value)
    storage.set('avatar', avatar.value)
  }

  async function refreshCurrentUser(meRequestConfig = {}) {
    if (!token.value) {
      userInfo.value = null
      return null
    }
    const me = await getMeCached(15000, meRequestConfig)
    const normalized = normalizeUser(me)
    if (normalized?.userId) {
      userId.value = normalized.userId
      id.value = normalized.userId
      storage.set('userId', normalized.userId)
    }
    setUserInfo(normalized)
    return userInfo.value
  }

  /**
   * 注册接口已成功、但紧随其后的 GET /users/me 可能因副本延迟、网关抖动等失败。
   * 此时不应把「拉取资料失败」当成注册失败，否则会出现库里有用户、前端却提示注册失败。
   */
  async function refreshAfterAuthWithFallback(context) {
    const attempts = 3
    const gapMs = 400
    let lastErr
    for (let i = 0; i < attempts; i++) {
      try {
        clearMeCache()
        await refreshCurrentUser({ skip401Redirect: true })
        return
      } catch (e) {
        lastErr = e
        if (i < attempts - 1) {
          await new Promise((r) => setTimeout(r, gapMs))
        }
      }
    }
    const uid = storage.get('userId') || ''
    if (uid) {
      setUserInfo(
        normalizeUser({
          userId: uid,
          id: uid,
          username: '',
          nickname: ''
        })
      )
    }
    console.warn(`[userStore] ${context}: refreshCurrentUser failed after ${attempts} tries, using minimal profile`, lastErr)
  }

  function syncCurrentUser() {
    if (!token.value) {
      userInfo.value = null
      return null
    }
    if (!userInfo.value) {
      refreshCurrentUser().catch(() => {
        logout()
      })
    }
    return userInfo.value
  }

  async function login(form) {
    const res = await loginApi(form)
    setAuth(res.token, res.userId)
    await refreshAfterAuthWithFallback('login')
    return res
  }

  async function register(form) {
    const res = await registerApi(form)
    setAuth(res.token, res.userId)
    await refreshAfterAuthWithFallback('register')
    return res
  }

  async function activateFellowship() {
    if (!token.value) {
      throw new Error('请先登录')
    }
    const res = await activateFellowshipApi()
    clearMeCache()
    const normalized = normalizeUser(res)
    userInfo.value = normalized
    return normalized
  }

  async function deactivateFellowship() {
    if (!token.value) {
      throw new Error('请先登录')
    }
    const res = await deactivateFellowshipApi()
    clearMeCache()
    const normalized = normalizeUser(res)
    userInfo.value = normalized
    return normalized
  }

  async function updateFellowshipMatchVisibility(visible) {
    if (!token.value) {
      throw new Error('请先登录')
    }
    const res = await updateFellowshipMatchVisibilityApi(Boolean(visible))
    clearMeCache()
    const normalized = normalizeUser(res)
    userInfo.value = normalized
    return normalized
  }

  async function loadAdminContext(force = false) {
    if (adminContext.value && !force) return adminContext.value
    const ctx = await getAdminAuthContext()
    adminContext.value = ctx
    return ctx
  }

  function hasPermission(permission) {
    return adminContext.value?.permissions?.includes(permission) ?? false
  }

  function hasAdminRole(roleCode) {
    return adminContext.value?.roles?.includes(roleCode.toUpperCase()) ?? false
  }

  function logout() {
    token.value = ''
    userId.value = ''
    id.value = ''
    nickname.value = ''
    avatar.value = ''
    userInfo.value = null
    adminContext.value = null
    storage.remove('token')
    storage.remove('userId')
    storage.remove('nickname')
    storage.remove('avatar')
    storage.remove(REDIRECT_KEY)
    clearMeCache()
    useFellowshipProfileStore().clearProfile()
  }

  function setPostLoginRedirect(path) {
    if (!path) return
    storage.set(REDIRECT_KEY, path)
  }

  function consumePostLoginRedirect() {
    const path = storage.get(REDIRECT_KEY)
    storage.remove(REDIRECT_KEY)
    return path || ''
  }

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => {
    // Legacy role check
    if (['admin', 'super_admin', 'root'].includes(String(userInfo.value?.role || '').toLowerCase())) return true
    // Fine-grained: any admin role from context
    return (adminContext.value?.roles?.length ?? 0) > 0
  })
  /** 仅持有 GROUP_OWNER（团长）后台入口、无平台运营类角色的账号 */
  const isGroupStewardOnly = computed(() => {
    if (!token.value || !adminContext.value) return false
    const lr = String(userInfo.value?.role || '').toLowerCase()
    if (['admin', 'super_admin', 'root'].includes(lr)) return false
    const roles = adminContext.value.roles
    if (!Array.isArray(roles) || roles.length !== 1) return false
    return String(roles[0]).toUpperCase() === 'GROUP_OWNER'
  })
  const isFellowshipEnabled = computed(() => Boolean(userInfo.value?.fellowshipEnabled))
  const isFellowshipMatchVisible = computed(() => Boolean(userInfo.value?.fellowshipMatchVisible))

  if (token.value) {
    refreshCurrentUser().catch(() => {
      logout()
    })
  }

  return {
    token,
    id,
    nickname,
    avatar,
    userId,
    userInfo,
    adminContext,
    isLoggedIn,
    isAdmin,
    isGroupStewardOnly,
    isFellowshipEnabled,
    isFellowshipMatchVisible,
    setAuth,
    setUserInfo,
    syncCurrentUser,
    refreshCurrentUser,
    loadAdminContext,
    hasPermission,
    hasAdminRole,
    login,
    register,
    activateFellowship,
    deactivateFellowship,
    updateFellowshipMatchVisibility,
    logout,
    setPostLoginRedirect,
    consumePostLoginRedirect
  }
})
