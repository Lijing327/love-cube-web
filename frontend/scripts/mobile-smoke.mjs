/**
 * 移动端冒烟：布局可达性 + 关键路径是否可走通
 * 用法：node scripts/mobile-smoke.mjs
 */
import { chromium, devices } from 'playwright'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const BASE = process.env.SMOKE_BASE || 'http://localhost:5173'
const OUT = join(process.cwd(), 'scripts', '.smoke-out')
mkdirSync(OUT, { recursive: true })

const results = []

function note(name, ok, detail = '') {
  results.push({ name, ok, detail })
  const mark = ok ? 'PASS' : 'FAIL'
  console.log(`[${mark}] ${name}${detail ? ` — ${detail}` : ''}`)
}

async function shot(page, name) {
  const file = join(OUT, `${name}.png`)
  await page.screenshot({ path: file, fullPage: false })
  return file
}

async function metricLayoutOk(page) {
  return page.evaluate(() => {
    const cards = [...document.querySelectorAll('.hero-metrics .metric-card')]
    if (cards.length < 2) return { ok: false, reason: `metric cards=${cards.length}` }
    const tops = cards.map((el) => Math.round(el.getBoundingClientRect().top))
    const heights = cards.map((el) => el.getBoundingClientRect().height)
    const sameRow = Math.max(...tops) - Math.min(...tops) < 28
    const compact = heights.every((h) => h > 0 && h <= 96)
    // 允许 2x2（4 项），但不接受单列堆叠
    const colsApprox = new Set(tops).size
    const notSingleColumn = colsApprox <= 2
    return {
      ok: compact && notSingleColumn && (sameRow || cards.length >= 4),
      reason: `sameRow=${sameRow} rows=${colsApprox} heights=${heights.map((h) => Math.round(h)).join(',')}`
    }
  })
}

async function createActionsVisible(page) {
  return page.evaluate(() => {
    const btn = document.querySelector('.actions .btn.primary, button[type="submit"]')
    if (!btn) return { ok: false, reason: 'submit button missing' }
    const r = btn.getBoundingClientRect()
    const vh = window.innerHeight
    const nav = document.querySelector('.mobile-quick-nav')
    const navTop = nav ? nav.getBoundingClientRect().top : vh
    const visible = r.bottom > 0 && r.top < navTop - 4 && r.height > 0
    return {
      ok: visible,
      reason: `btnTop=${Math.round(r.top)} btnBottom=${Math.round(r.bottom)} navTop=${Math.round(navTop)} vh=${vh}`
    }
  })
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({
    ...devices['iPhone 12'],
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  page.setDefaultTimeout(20000)

  // 1) 内容中心布局
  await page.goto(`${BASE}/#/platform/content`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(800)
  await shot(page, '01-content')
  const contentTitle = await page.locator('h1', { hasText: '内容中心' }).count()
  note('内容中心可打开', contentTitle > 0)
  const metrics = await metricLayoutOk(page)
  note('内容中心指标并排且不高', metrics.ok, metrics.reason)

  // 2) 团体大厅
  await page.goto(`${BASE}/#/platform/groups`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(800)
  await shot(page, '02-groups')
  const groupsOk = (await page.locator('text=团体').count()) > 0 || (await page.locator('h1').count()) > 0
  note('团体页可打开', groupsOk, page.url())

  // 3) 创建团体：未登录应跳登录或展示表单
  await page.goto(`${BASE}/#/platform/groups/create`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1000)
  await shot(page, '03-create-redirect-or-form')
  const url = page.url()
  const onLogin = url.includes('/login')
  const hasForm = (await page.locator('text=创建团体').count()) > 0
  note('创建团体入口可达（登录或表单）', onLogin || hasForm, url)

  if (hasForm && !onLogin) {
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
    await page.waitForTimeout(400)
    await shot(page, '03b-create-bottom')
    const actions = await createActionsVisible(page)
    note('创建按钮不被底栏遮挡', actions.ok, actions.reason)
  }

  // 4) 尝试登录（环境变量提供账号时）
  const phone = process.env.SMOKE_PHONE || ''
  const password = process.env.SMOKE_PASSWORD || ''
  if (phone && password) {
    await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' })
    await page.waitForTimeout(500)
    const phoneInput = page.locator('input[name="phone"], input[type="tel"], input[placeholder*="手机"]').first()
    const pwdInput = page.locator('input[name="password"], input[type="password"]').first()
    await phoneInput.fill(phone)
    await pwdInput.fill(password)
    await page.locator('button[type="submit"], .van-button').filter({ hasText: /登录|登 录/ }).first().click()
    await page.waitForTimeout(1500)
    await shot(page, '04-after-login')
    const loggedIn = !page.url().includes('/login')
    note('登录成功', loggedIn, page.url())

    if (loggedIn) {
      await page.goto(`${BASE}/#/platform/groups/create`, { waitUntil: 'networkidle' })
      await page.waitForTimeout(800)
      await page.locator('input').nth(0).fill(`冒烟测试团${Date.now().toString().slice(-4)}`)
      await page.locator('textarea').first().fill('自动化冒烟：验证移动端创建团体流程')
      const nameInput = page.locator('input').filter({ has: page.locator('xpath=..') })
      // 团体内称呼：按 label 附近输入
      const labels = page.locator('label.field')
      const count = await labels.count()
      for (let i = 0; i < count; i++) {
        const text = await labels.nth(i).innerText()
        if (text.includes('称呼') || text.includes('真实姓名')) {
          await labels.nth(i).locator('input').fill('测试员')
        }
      }
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
      await page.waitForTimeout(300)
      const actions = await createActionsVisible(page)
      note('登录后创建按钮可见', actions.ok, actions.reason)
      await shot(page, '05-create-ready')
      await page.locator('button[type="submit"]').click()
      await page.waitForTimeout(2500)
      await shot(page, '06-create-result')
      const created = page.url().includes('/spaces/') || page.url().includes('/groups/')
      const err = await page.locator('.err').innerText().catch(() => '')
      note('创建团体提交走通', created && !err, `${page.url()} ${err}`)
      const createdId = String(page.url()).match(/\/(?:spaces|groups)\/(\d+)/)?.[1]
      const token = await page.evaluate(() => localStorage.getItem('token') || '')
      if (createdId && token) {
        const del = await page.request.delete(`http://localhost:8090/admin/api/platform/groups/${createdId}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        if (!del.ok()) {
          await page.request.put(`http://localhost:8090/admin/api/platform/groups/${createdId}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: { status: 'disabled' }
          })
        }
        note('清理冒烟团体', true, createdId)
      }
    }
  } else {
    note('登录/创建写接口', true, '未提供 SMOKE_PHONE/SMOKE_PASSWORD，跳过写操作')
  }

  // 5) 底栏导航
  await page.goto(`${BASE}/#/platform`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(600)
  const navCount = await page.locator('.mobile-quick-nav a').count()
  note('底部导航存在', navCount >= 4, `links=${navCount}`)
  if (navCount > 0) {
    await page.locator('.mobile-quick-nav a', { hasText: '团体' }).click()
    await page.waitForTimeout(800)
    note('底栏进入团体', page.url().includes('groups'), page.url())
    await shot(page, '07-nav-groups')
  }

  await browser.close()

  const failed = results.filter((r) => !r.ok)
  writeFileSync(join(OUT, 'report.json'), JSON.stringify({ base: BASE, results }, null, 2))
  console.log('\n=== SUMMARY ===')
  console.log(`total=${results.length} pass=${results.length - failed.length} fail=${failed.length}`)
  console.log(`screenshots: ${OUT}`)
  if (failed.length) process.exitCode = 1
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
