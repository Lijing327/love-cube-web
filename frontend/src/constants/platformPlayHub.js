import { PC_EVENTS, pcPath } from '@/constants/pcPaths.js'

export const PLATFORM_PLAY = '/platform/play'

/** 平台层 / PC / Mobile 壳共用的「全部功能」入口配置 */
export function getPlayHubItems(shell = 'platform') {
  if (shell === 'pc') return buildPcItems()
  if (shell === 'm') return buildMobileItems()
  return buildPlatformItems()
}

export function getPlayHubTip(shell = 'platform') {
  if (shell === 'pc') {
    return {
      text: '联谊匹配、VIP、活动签到请使用顶部导航进入联谊模块，或访问',
      linkTo: '/fellowship/discover',
      linkLabel: '联谊发现'
    }
  }
  if (shell === 'm') {
    return {
      text: '联谊匹配、VIP、活动签到请从底部 Tab「认识 / 我的」进入。',
      linkTo: '',
      linkLabel: ''
    }
  }
  return {
    text: '联谊匹配、VIP、活动签到请从',
    linkTo: '/fellowship-intro',
    linkLabel: '联谊介绍',
    suffix: '进入，或浏览底部「我的」个人中心。'
  }
}

function buildPlatformItems() {
  return [
    { to: '/me/tasks', icon: '✓', title: '今日任务', sub: '新人包 · 每周挑战', tone: 'violet' },
    { to: '/platform/checkin', icon: '☀', title: '每日签到', sub: '连续登录记录', tone: 'orange' },
    { to: '/platform/positive-share', icon: '♡', title: '每日心声', sub: '话题 · 周榜', tone: 'pink' },
    { to: '/platform/help', icon: '⇄', title: '互助广场', sub: '达人榜 · 发需求', tone: 'blue' },
    { to: '/platform/groups', icon: '◎', title: '团体广场', sub: '发现 · 加入小组', tone: 'green' },
    { to: '/platform/local', icon: '⌂', title: '本地服务', sub: '招聘二手租房', tone: 'amber' },
    { to: '/platform/content', icon: '💬', title: '内容中心', sub: '资讯 · 话题', tone: 'cyan' },
    { to: '/messages', icon: '✉', title: '消息中心', sub: '通知与互动', tone: 'green' },
    { to: { path: '/platform/content', query: { type: 'ai' } }, icon: '✦', title: 'AI 助手', sub: '润色 · 话术', tone: 'violet' },
    { to: '/platform/growth', icon: '★', title: '成长中心', sub: '等级 · 徽章', tone: 'indigo' },
    { to: '/platform/events', icon: '▣', title: '活动中心', sub: '平台活动列表', tone: 'orange' },
    { to: '/announcements', icon: '◇', title: '平台公告', sub: '最新通知', tone: 'blue' },
    { to: '/articles', icon: '文', title: '精选内容', sub: '文章与专栏', tone: 'cyan' },
    { to: '/modules', icon: '⊞', title: '模块中心', sub: '探索全部模块', tone: 'indigo' },
    { to: '/fellowship/invite', icon: '◎', title: '邀请好友', sub: '有效邀请奖励', tone: 'green' },
    { to: '/fellowship/event-signups', icon: '▣', title: '活动报名', sub: '签到 · 互评', tone: 'indigo' },
    { to: '/fellowship-intro', icon: '心', title: '联谊', sub: '认识新朋友', tone: 'pink' }
  ]
}

function buildPcItems() {
  return [
    { to: pcPath('tasks'), icon: '✓', title: '今日任务', sub: '新人包 · 每周挑战', tone: 'violet' },
    { to: pcPath('checkin'), icon: '☀', title: '每日签到', sub: '连续登录记录', tone: 'orange' },
    { to: pcPath('positive-share'), icon: '♡', title: '每日心声', sub: '话题 · 周榜', tone: 'pink' },
    { to: pcPath('help'), icon: '⇄', title: '互助广场', sub: '达人榜 · 发需求', tone: 'blue' },
    { to: pcPath('groups'), icon: '◎', title: '团体广场', sub: '发现 · 加入小组', tone: 'green' },
    { to: pcPath('local'), icon: '⌂', title: '本地服务', sub: '招聘二手租房', tone: 'amber' },
    { to: pcPath('topics'), icon: '💬', title: '话题广场', sub: '兴趣讨论', tone: 'cyan' },
    { to: pcPath('messages'), icon: '✉', title: '消息中心', sub: '通知与互动', tone: 'green' },
    { to: '/fellowship/invite', icon: '◎', title: '邀请好友', sub: '有效邀请奖励', tone: 'green' },
    { to: '/fellowship/event-signups', icon: '▣', title: '活动报名', sub: '签到 · 互评', tone: 'indigo' },
    { to: PC_EVENTS, icon: '▣', title: '活动中心', sub: '平台活动列表', tone: 'orange' }
  ]
}

function buildMobileItems() {
  return [
    { to: '/m/platform/tasks', icon: '✓', title: '今日任务', sub: '新人包 · 每周挑战', tone: 'violet' },
    { to: '/m/platform/checkin', icon: '☀', title: '每日签到', sub: '连续登录记录', tone: 'orange' },
    { to: '/m/platform/positive-share', icon: '♡', title: '每日心声', sub: '话题 · 周榜', tone: 'pink' },
    { to: '/m/platform/help', icon: '⇄', title: '互助广场', sub: '达人榜 · 发需求', tone: 'blue' },
    { to: '/m/platform/groups', icon: '◎', title: '团体广场', sub: '发现 · 加入小组', tone: 'green' },
    { to: '/m/platform/local', icon: '⌂', title: '本地服务', sub: '招聘二手租房', tone: 'amber' },
    { to: '/m/platform/topics', icon: '💬', title: '话题广场', sub: '兴趣讨论', tone: 'cyan' },
    { to: '/m/platform/articles', icon: '文', title: '精选内容', sub: '阅读与投稿', tone: 'cyan' },
    { to: '/m/platform/messages', icon: '✉', title: '消息中心', sub: '通知与互动', tone: 'green' },
    { to: '/m/fellowship/ai-tools', icon: '✦', title: 'AI 助手', sub: '润色 · 话术', tone: 'violet' },
    { to: '/m/platform/member', icon: '★', title: '平台会员', sub: '权益升级', tone: 'indigo' },
    { to: '/m/fellowship/invite', icon: '◎', title: '邀请好友', sub: '有效邀请奖励', tone: 'green' },
    { to: '/m/fellowship/event-signups', icon: '▣', title: '活动报名', sub: '签到 · 互评', tone: 'indigo' }
  ]
}
