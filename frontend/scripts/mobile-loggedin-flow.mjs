import { chromium, devices } from 'playwright'
import { mkdirSync } from 'node:fs'
import { join } from 'node:path'

const BASE = 'http://localhost:5173'
const OUT = join(process.cwd(), 'scripts', '.loggedin-flow')
mkdirSync(OUT, { recursive: true })

const phone = process.env.SMOKE_PHONE || ''
const password = process.env.SMOKE_PASSWORD || ''
if (!phone || !password) {
  console.error('请设置环境变量 SMOKE_PHONE / SMOKE_PASSWORD')
  process.exit(1)
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ ...devices['iPhone 12'], locale: 'zh-CN' })
  const page = await context.newPage()

  // API login then inject token
  const loginRes = await page.request.post('http://localhost:8090/admin/api/auth/login', {
    data: { phone, password }
  })
  const loginJson = await loginRes.json()
  if (!loginJson?.token) throw new Error(`login failed: ${JSON.stringify(loginJson)}`)

  await page.goto(`${BASE}/platform`, { waitUntil: 'domcontentloaded' })
  await page.evaluate((payload) => {
    localStorage.setItem('token', payload.token)
    localStorage.setItem('userId', String(payload.userId || ''))
  }, { token: loginJson.token, userId: loginJson.userId })
  await page.reload({ waitUntil: 'networkidle' })
  await page.waitForTimeout(1000)

  // 关闭公告弹窗（会挡住后续点击）
  async function dismissPopups() {
    const close = page.locator('.announcement-popup-mask button, .announcement-popup-close, .announcement-popup-mask .platform-btn').first()
    if (await close.count()) {
      await close.click({ force: true }).catch(() => {})
      await page.waitForTimeout(300)
    }
    await page.evaluate(() => {
      document.querySelectorAll('.announcement-popup-mask').forEach((el) => el.remove())
    })
  }
  await dismissPopups()

  // create group page
  await page.goto(`${BASE}/platform/groups/create`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(800)
  await dismissPopups()
  const onCreate = page.url().includes('/groups/create')
  console.log(`[${onCreate ? 'PASS' : 'FAIL'}] 登录后进入创建页 ${page.url()}`)
  await page.screenshot({ path: join(OUT, '01-create.png') })

  if (onCreate) {
    const name = `冒烟团${Date.now().toString().slice(-5)}`
    await page.locator('.create-form input[type="text"]').nth(0).fill(name)
    await page.locator('.create-form textarea').first().fill('自动化冒烟测试团体，验证移动端创建流程')
    // 称呼字段
    const fields = page.locator('label.field')
    const n = await fields.count()
    for (let i = 0; i < n; i++) {
      const t = await fields.nth(i).innerText()
      if (t.includes('称呼') || t.includes('真实姓名')) {
        await fields.nth(i).locator('input').fill('测试员')
      }
    }
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
    await page.waitForTimeout(300)
    await page.screenshot({ path: join(OUT, '02-create-filled.png') })
    await dismissPopups()
    await page.locator('button[type="submit"]').click({ force: true })
    await page.waitForTimeout(2500)
    await page.screenshot({ path: join(OUT, '03-create-result.png') })
    const ok = page.url().includes('/spaces/') || page.url().includes('/groups/')
    const err = await page.locator('.err').innerText().catch(() => '')
    console.log(`[${ok && !err ? 'PASS' : 'FAIL'}] 创建团体 ${page.url()} ${err}`)
    const createdId = String(page.url()).match(/\/(?:spaces|groups)\/(\d+)/)?.[1]
    if (createdId) {
      const del = await page.request.delete(`http://localhost:8090/admin/api/platform/groups/${createdId}`, {
        headers: { Authorization: `Bearer ${loginJson.token}` }
      })
      if (!del.ok()) {
        await page.request.put(`http://localhost:8090/admin/api/platform/groups/${createdId}`, {
          headers: { Authorization: `Bearer ${loginJson.token}` },
          data: { status: 'disabled' }
        })
      }
      console.log(`[INFO] 已清理冒烟团体 ${createdId}`)
    }
  }

  // layout pages after login
  for (const [id, path] of [
    ['help', '/platform/help'],
    ['season', '/platform/groups/season'],
    ['groups', '/platform/groups'],
    ['share', '/platform/positive-share'],
    ['me', '/platform/me']
  ]) {
    await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' })
    await page.waitForTimeout(700)
    await page.screenshot({ path: join(OUT, `page-${id}.png`) })
    console.log(`shot ${id} -> ${page.url()}`)
  }

  await browser.close()
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
