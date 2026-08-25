const DEFAULT_PRODUCTION_INVITE_BASE_URL = 'https://lovecube.xifg.com.cn'

function trimTrailingSlash(value) {
  return String(value || '').trim().replace(/\/+$/, '')
}

export function getInviteBaseUrl() {
  const configured = trimTrailingSlash(import.meta.env.VITE_INVITE_BASE_URL)
  if (configured) {
    return configured
  }
  if (import.meta.env.PROD) {
    console.error('VITE_INVITE_BASE_URL is not configured; falling back to production invite domain.')
    return DEFAULT_PRODUCTION_INVITE_BASE_URL
  }
  return trimTrailingSlash(window.location.origin)
}

export function createInviteUrl(inviteCode) {
  const code = String(inviteCode || '').trim()
  if (!code) return ''
  return `${getInviteBaseUrl()}/register?inviteCode=${encodeURIComponent(code)}`
}
