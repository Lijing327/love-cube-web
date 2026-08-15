/**
 * 移动端全页巡检：逐页打开、截图、收集布局/控制台问题
 */
import { chromium, devices } from 'playwright'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const BASE = process.env.SMOKE_BASE || 'http://localhost:5173'
const OUT = join(process.cwd(), 'scripts', '.page-tour')
mkdirSync(OUT, { recursive: true })

// 用户真实会走的平台移动路径（底栏场景 + /m 灰度）
const PAGES = [
  { id: 'home', path: '/#/platform', title: '平台首页' },
  { id: 'local', path: '/#/platform/local', title: '本地' },
  { id: 'content', path: '/#/platform/content', title: '内容中心' },
  { id: 'publish', path: '/#/platform/positive-share', title: '发布/心声' },
  { id: 'groups', path: '/#/platform/groups', title: '团体大厅' },
  { id: 'groups-create', path: '/#/platform/groups/create', title: '创建团体' },
  { id: 'groups-season', path: '/#/platform/groups/season', title: '赛季榜' },
  { id: 'play', path: '/#/platform/play', title: '玩法中心' },
  { id: 'help', path: '/#/platform/help', title: '互助广场' },
  { id: 'events', path: '/#/platform/events', title: '活动中心' },
  { id: 'articles', path: '/#/platform/articles', title: '文章' },
  { id: 'announcements', path: '/#/announcements', title: '公告' },
  { id: 'modules', path: '/#/modules', title: '模块中心' },
  { id: 'me', path: '/#/platform/me', title: '我的' },
  { id: 'messages', path: '/#/messages', title: '消息中心' },
  { id: 'checkin', path: '/#/platform/checkin', title: '签到' },
  { id: 'login', path: '/#/login', title: '登录' },
  // /m 灰度
  { id: 'm-hub', path: '/#/m/platform', title: 'M玩法Hub' },
  { id: 'm-groups', path: '/#/m/platform/groups', title: 'M团体' },
  { id: 'm-local', path: '/#/m/platform/local', title: 'M本地' },
  { id: 'm-help', path: '/#/m/platform/help', title: 'M互助' },
  { id: 'm-content', path: '/#/m/platform/content', title: 'M内容' },
  { id: 'm-share', path: '/#/m/platform/positive-share', title: 'M心声' },
  { id: 'm-topics', path: '/#/m/platform/topics', title: 'M话题' }
]

async function analyze(page) {
  return page.evaluate(() => {
    const vh = window.innerHeight
    const nav = document.querySelector('.mobile-quick-nav')
    const navTop = nav ? nav.getBoundingClientRect().top : vh
    const issues = []

    // 过高指标卡（竖着占空间）
    const metricCards = [...document.querySelectorAll('.metric-card, .stat-card, .hero-stat, .hero-stats article')]
    const tallMetrics = metricCards.filter((el) => {
      const r = el.getBoundingClientRect()
      return r.height > 100 && r.width > 40
    })
    if (tallMetrics.length >= 2) {
      issues.push(`高指标卡×${tallMetrics.length}(>${100}px)`)
    }

    // 单列 metric-grid 且卡数>=3
    const grids = [...document.querySelectorAll('.metric-grid, .hero-metrics, .stats-band, .hero-stats')]
    for (const g of grids) {
      const kids = [...g.children].filter((c) => c.getBoundingClientRect().height > 20)
      if (kids.length < 3) continue
      const tops = kids.map((c) => Math.round(c.getBoundingClientRect().top))
      const uniqueRows = new Set(tops).size
      if (uniqueRows >= kids.length) issues.push(`${g.className.split(' ')[0] || 'grid'}单列堆叠×${kids.length}`)
    }

    // 固定底栏遮挡可点按钮
    const actions = [...document.querySelectorAll('button.btn.primary, button[type="submit"], .actions .btn, .primary-btn')]
    let obscured = 0
    for (const btn of actions) {
      const r = btn.getBoundingClientRect()
      if (r.height < 8 || r.width < 8) continue
      if (r.bottom > navTop + 2 && r.top < vh) obscured += 1
    }
    if (obscured) issues.push(`按钮被底栏遮挡×${obscured}`)

    // 首屏有效内容占比（正文区）
    const main = document.querySelector('.platform-main, .mobile-layout, main, .content-hub, .groups-page, .help-hub') || document.body
    const mainText = (main.innerText || '').replace(/\s+/g, ' ').trim()
    const h1 = document.querySelector('h1')?.innerText?.trim() || ''

    // 空白过大：首屏只有标题+大卡
    const firstCards = [...document.querySelectorAll('.metric-card, .stat-card, .section-card, .platform-card, .mp-card')]
      .filter((el) => {
        const r = el.getBoundingClientRect()
        return r.top < vh && r.bottom > 0
      })
    const cardArea = firstCards.reduce((s, el) => {
      const r = el.getBoundingClientRect()
      return s + Math.max(0, Math.min(r.bottom, vh) - Math.max(r.top, 0)) * r.width
    }, 0)
    const viewportArea = window.innerWidth * vh
    const cardRatio = viewportArea ? cardArea / viewportArea : 0
    if (cardRatio > 0.55 && firstCards.length <= 4) issues.push(`首屏卡片占比偏高(${Math.round(cardRatio * 100)}%)`)

    return {
      url: location.href,
      h1,
      textLen: mainText.length,
      issues,
      navPresent: !!nav,
      metricCount: metricCards.length
    }
  })
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ ...devices['iPhone 12'], locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(25000)

  const consoleErrors = []
  page.on('pageerror', (err) => consoleErrors.push(err.message))
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(msg.text())
  })

  let groupId = null
  try {
    const api = await fetch('http://localhost:8090/admin/api/groups?page=1&size=1')
    const data = await api.json()
    groupId = data?.items?.[0]?.id || data?.data?.items?.[0]?.id || null
    console.log(`groupId=${groupId || 'none'}`)
  } catch (e) {
    console.log(`groupId fetch failed: ${e.message}`)
  }

  const pages = [...PAGES]
  if (groupId) {
    pages.splice(6, 0, {
      id: 'group-detail',
      path: `/#/platform/groups/${groupId}`,
      title: '团体详情'
    })
    pages.push({
      id: 'm-group-detail',
      path: `/#/m/platform/groups/${groupId}`,
      title: 'M团体详情'
    })
  }

  const report = []

  for (const item of pages) {
    const beforeErr = consoleErrors.length
    try {
      await page.goto(`${BASE}${item.path}`, { waitUntil: 'domcontentloaded', timeout: 25000 })
      await page.waitForTimeout(1200)
      // 等一帧布局
      await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r())))
      const shot = join(OUT, `${String(report.length + 1).padStart(2, '0')}-${item.id}.png`)
      await page.screenshot({ path: shot, fullPage: false })
      const info = await analyze(page)
      const newErrs = consoleErrors.slice(beforeErr)
      const row = {
        ...item,
        finalUrl: page.url(),
        h1: info.h1,
        issues: info.issues,
        consoleErrors: [...new Set(newErrs)].slice(0, 5),
        redirectedToLogin: page.url().includes('/login'),
        screenshot: shot
      }
      report.push(row)
      const flag = row.issues.length || row.consoleErrors.length ? 'WARN' : 'OK'
      console.log(`[${flag}] ${item.title} | ${row.finalUrl.replace(BASE, '')} | issues=${row.issues.join(';') || '-'} | err=${row.consoleErrors.length}`)
    } catch (e) {
      report.push({ ...item, error: String(e.message || e), issues: ['打开失败'], consoleErrors: [], screenshot: null })
      console.log(`[FAIL] ${item.title} — ${e.message}`)
    }
  }

  writeFileSync(join(OUT, 'report.json'), JSON.stringify(report, null, 2), 'utf8')

  const warned = report.filter((r) => (r.issues || []).length || (r.consoleErrors || []).length || r.error)
  console.log('\n=== TOUR SUMMARY ===')
  console.log(`pages=${report.length} clean=${report.length - warned.length} warned=${warned.length}`)
  for (const r of warned) {
    console.log(`- ${r.title}: ${(r.issues || []).join(' / ') || r.error || 'console'} ${r.redirectedToLogin ? '(跳登录)' : ''}`)
  }
  console.log(`screenshots: ${OUT}`)

  await browser.close()
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
