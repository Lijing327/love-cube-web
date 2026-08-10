/** PushPlus Token 脱敏：前 6 位 + 后 4 位 */
export function maskPushplusToken(token) {
  if (!token) return ''
  const t = String(token).trim()
  if (t.length <= 10) return `${t.slice(0, 2)}****`
  return `${t.slice(0, 6)}****${t.slice(-4)}`
}

export function isPushplusBound(prefs) {
  return Boolean(prefs?.pushplusEnabled && String(prefs?.pushplusToken || '').trim())
}

/** 平台首页入口（适合收藏 / 扫码） */
export function resolvePlatformEntryUrl() {
  const { origin, pathname } = window.location
  return `${origin}${pathname}#/platform`
}
