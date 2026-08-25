/** 当前前端路由路径（兼容尚未跳转完的旧 Hash 地址） */
export function currentAppPath() {
  const hash = window.location.hash || ''
  if (hash.startsWith('#/')) {
    return hash.slice(1) || '/'
  }
  return `${window.location.pathname || '/'}${window.location.search || ''}`
}

/** 把通知/邀请里的 /#/path 或完整 URL 收成 Vue Router 可用的路径 */
export function toRouterPath(url) {
  if (!url || typeof url !== 'string') return ''
  const raw = url.trim()
  try {
    if (/^https?:\/\//i.test(raw)) {
      const parsed = new URL(raw)
      if (parsed.hash.startsWith('#/')) {
        return parsed.hash.slice(1) || '/'
      }
      return `${parsed.pathname || '/'}${parsed.search || ''}`
    }
  } catch {
    /* ignore invalid absolute URLs */
  }
  if (raw.startsWith('/#/')) return raw.slice(2) || '/'
  if (raw.startsWith('#/')) return raw.slice(1) || '/'
  return raw
}
