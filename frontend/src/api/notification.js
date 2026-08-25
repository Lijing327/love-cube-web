import request from './request.js'

let unreadCountCache = null
let unreadCountCacheAt = 0
let unreadCountPending = null

export const getNotifications = (params = {}) => {
  const p = typeof params === 'number' ? { page: 0, pageSize: Math.min(200, Math.max(1, params)), limit: params } : params
  return request.get('/notifications', {
    params: {
      page: p.page ?? 0,
      pageSize: p.pageSize ?? p.size ?? 20,
      read: p.read,
      tab: p.tab,
      type: p.type,
      limit: p.limit
    }
  })
}

export const getNotificationsByType = (type, limit = 20) =>
  getNotifications({ page: 0, pageSize: limit, type })

/** 兼容新版分页响应 { items, content } 与旧版纯数组 */
export function unwrapNotificationList(res) {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.items)) return res.items
  if (Array.isArray(res.content)) return res.content
  if (Array.isArray(res.list)) return res.list
  if (Array.isArray(res.records)) return res.records
  return []
}

export const getNotifUnreadCount = () => request.get('/notifications/unread-count')

export async function markNotifRead(id) {
  const res = await request.put(`/notifications/${id}/read`)
  clearNotifUnreadCache()
  return res
}

export async function markAllNotifRead() {
  const res = await request.put('/notifications/read-all')
  clearNotifUnreadCache()
  return res
}

export async function deleteNotification(id) {
  const res = await request.delete(`/notifications/${id}`)
  clearNotifUnreadCache()
  return res
}

export async function deleteNotificationsByType(type) {
  const res = await request.delete('/notifications/by-type', { params: { type } })
  clearNotifUnreadCache()
  return res
}

export async function getNotifUnreadCountCached(maxAgeMs = 15000) {
  const now = Date.now()
  const bypassCache = maxAgeMs != null && maxAgeMs <= 0
  if (!bypassCache && unreadCountCache && now - unreadCountCacheAt < maxAgeMs) return unreadCountCache
  if (unreadCountPending) return unreadCountPending
  unreadCountPending = getNotifUnreadCount()
    .then((res) => {
      unreadCountCache = res
      unreadCountCacheAt = Date.now()
      return res
    })
    .finally(() => {
      unreadCountPending = null
    })
  return unreadCountPending
}

export function clearNotifUnreadCache() {
  unreadCountCache = null
  unreadCountCacheAt = 0
  unreadCountPending = null
}
