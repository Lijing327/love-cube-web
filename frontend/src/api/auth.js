import request from './request.js'

/** H5 账号密码登录 */
export function login(data) {
  return request.post('/auth/login', data)
}

/** H5 注册 */
export function register(data) {
  return request.post('/auth/register', data)
}

/** 修改当前登录用户密码 */
export function changePassword(data) {
  return request.put('/auth/password', data)
}

/** 换绑当前登录用户手机号（需验证登录密码） */
export function changePhone(data) {
  return request.put('/auth/phone', data)
}

/** 绑定或换绑当前登录用户邮箱（需验证登录密码） */
export function changeEmail(data) {
  return request.put('/auth/email', data)
}
