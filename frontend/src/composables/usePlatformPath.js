import { computed } from 'vue'
import { useRoute } from 'vue-router'

/** 根据当前壳（/m/platform、/pc/platform、/platform）生成玩法路径 */
export function usePlatformPath() {
  const route = useRoute()

  const base = computed(() => {
    if (route.path.startsWith('/m/platform')) return '/m/platform'
    if (route.path.startsWith('/pc')) return '/pc/platform'
    return '/platform'
  })

  function platformPath(segment = '') {
    const s = String(segment || '').replace(/^\//, '')
    return s ? `${base.value}/${s}` : base.value
  }

  function eventsPath(id) {
    const eid = id != null && id !== '' ? String(id) : ''
    if (route.path.startsWith('/pc')) {
      return eid ? `/pc/events/${eid}` : '/pc/events'
    }
    if (route.path.startsWith('/m/platform')) {
      return eid ? `/events/${eid}` : '/platform/events'
    }
    if (route.path.startsWith('/platform')) {
      return eid ? `/events/${eid}` : '/platform/events'
    }
    return eid ? `/events/${eid}` : '/events'
  }

  function helpPath(...parts) {
    const tail = parts.filter(Boolean).map(String).join('/')
    return tail ? `${base.value}/help/${tail}` : `${base.value}/help`
  }

  function groupsPath(...parts) {
    const tail = parts.filter(Boolean).map(String).join('/')
    return tail ? `${base.value}/groups/${tail}` : `${base.value}/groups`
  }

  function myGroupsPath() {
    if (route.path.startsWith('/pc') || route.path.startsWith('/m/platform')) {
      return `${base.value}/my-groups`
    }
    return `${base.value}/me/groups`
  }

  function messagesPath() {
    if (route.path.startsWith('/pc')) return '/pc/platform/messages'
    if (route.path.startsWith('/m/platform')) return '/m/platform/messages'
    return '/messages'
  }

  function mePath(query) {
    if (route.path.startsWith('/pc')) {
      return query ? { path: '/pc/platform/me', query } : '/pc/platform/me'
    }
    if (route.path.startsWith('/m/platform')) {
      return query ? { path: '/m/fellowship/me', query } : '/m/fellowship/me'
    }
    return query ? { path: '/me', query } : '/me'
  }

  function playPath() {
    if (route.path.startsWith('/pc')) return '/pc/platform/play'
    if (route.path.startsWith('/m/platform')) return '/m/platform'
    return '/platform/play'
  }

  function topicsPath() {
    if (route.path.startsWith('/pc')) return '/pc/platform/topics'
    if (route.path.startsWith('/m/platform')) return '/m/platform/topics'
    return '/m/platform/topics'
  }

  function modulesPath() {
    if (route.path.startsWith('/pc')) return '/pc/platform/modules'
    return '/modules'
  }

  function contentPath(query) {
    const path = platformPath('content')
    return query ? { path, query } : path
  }

  function announcementsPath(id) {
    const eid = id != null && id !== '' ? String(id) : ''
    if (route.path.startsWith('/pc')) {
      return eid ? `/pc/platform/announcements/${eid}` : '/pc/platform/announcements'
    }
    return eid ? `/announcements/${eid}` : '/announcements'
  }

  function articlesPath(id) {
    const eid = id != null && id !== '' ? String(id) : ''
    if (route.path.startsWith('/pc')) {
      return eid ? `/articles/${eid}` : '/pc/platform/articles'
    }
    if (route.path.startsWith('/m/platform')) {
      return eid ? `/articles/${eid}` : '/m/platform/articles'
    }
    return eid ? `/articles/${eid}` : '/platform/articles'
  }

  function articlesWritePath() {
    if (route.path.startsWith('/pc')) return '/pc/platform/articles/write'
    if (route.path.startsWith('/m/platform')) return '/m/platform/articles/write'
    return '/platform/articles'
  }

  const isMobileShell = computed(() => route.path.startsWith('/m/platform'))
  const isPcShell = computed(() => route.path.startsWith('/pc'))

  return {
    base,
    platformPath,
    helpPath,
    groupsPath,
    myGroupsPath,
    eventsPath,
    messagesPath,
    mePath,
    playPath,
    topicsPath,
    modulesPath,
    contentPath,
    announcementsPath,
    articlesPath,
    articlesWritePath,
    isMobileShell,
    isPcShell
  }
}
