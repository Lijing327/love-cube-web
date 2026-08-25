import axios from 'axios'
import { storage } from '@f/utils/storage.js'
import { currentAppPath } from '@/utils/appPath.js'

const request = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE_URL || '') + '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = storage.get('token')
  if (token) config.headers['Authorization'] = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  res => res.data,
  err => {
    const status  = err.response?.status
    const message = err.response?.data?.message || err.message || '请求失败'
    if (status === 401) {
      storage.remove('token')
      storage.remove('userId')
      const currentPath = currentAppPath()
      const encoded = encodeURIComponent(currentPath)
      setTimeout(() => { window.location.assign(`/fellowship/login?redirect=${encoded}`) }, 100)
    }
    return Promise.reject(new Error(message))
  }
)

export default request
