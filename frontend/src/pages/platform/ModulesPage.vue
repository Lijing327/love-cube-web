<template>
  <div class="modules-page">
    <div class="mc">
      <section class="mp-hero">
        <div>
          <h1 class="mp-title">模块中心</h1>
          <p class="mp-sub">探索内容、活动、社交与服务入口</p>
          <div class="mp-stats">
            <div v-for="item in stats" :key="item.label" class="mp-stat">
              <p class="mp-stat-value">{{ item.value }}</p>
              <p class="mp-stat-label">{{ item.label }}</p>
            </div>
          </div>
        </div>
        <div class="mp-hero-art" aria-hidden="true">
          <svg class="hero-svg" viewBox="0 0 340 180" role="presentation">
            <defs>
              <linearGradient id="cubePink" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#ffa1c3" />
                <stop offset="100%" stop-color="#ff5ea0" />
              </linearGradient>
              <linearGradient id="cubeBlue" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#8bb3ff" />
                <stop offset="100%" stop-color="#5d84ff" />
              </linearGradient>
              <linearGradient id="cubeCyan" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#95e3ff" />
                <stop offset="100%" stop-color="#57baff" />
              </linearGradient>
            </defs>
            <circle cx="170" cy="96" r="72" fill="#f7dff0" opacity="0.35" />
            <rect x="131" y="44" rx="16" ry="16" width="78" height="78" fill="url(#cubePink)" />
            <rect x="220" y="58" rx="14" ry="14" width="44" height="56" fill="url(#cubeBlue)" />
            <rect x="86" y="70" rx="14" ry="14" width="36" height="46" fill="url(#cubeCyan)" />
            <path d="M170 97.5l-7.8-7.2c-7.8-7.2-12.9-11.8-12.9-17.6 0-4.6 3.6-8.2 8.2-8.2 2.7 0 5.2 1.2 6.9 3.2 1.7-2 4.2-3.2 6.9-3.2 4.6 0 8.2 3.6 8.2 8.2 0 5.8-5.1 10.5-12.9 17.7l-7.6 7.1z" fill="#ffffff" />
          </svg>
        </div>
      </section>

      <section class="mp-main">
        <div class="mp-grid">
          <component
            v-for="mod in modules"
            :key="mod.moduleKey"
            :is="mod.status === 'active' ? 'router-link' : 'div'"
            :to="mod.status === 'active' ? mod.entryRoute : undefined"
            class="mp-card"
            :class="{ 'mp-card--planned': mod.status === 'planned' }"
          >
            <div class="mp-card-head">
              <span class="mp-icon" :style="{ color: mod.color, background: mod.iconBg }">
                <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
                  <path :d="mod.icon" />
                </svg>
              </span>
              <span class="mp-badge" :class="mod.status === 'active' ? 'mp-badge--active' : 'mp-badge--planned'">
                {{ mod.status === 'active' ? '已开放' : '规划中' }}
              </span>
            </div>
            <h2 class="mp-name">{{ mod.name }}</h2>
            <p class="mp-desc">{{ mod.description }}</p>
            <p class="mp-enter">{{ mod.status === 'active' ? '进入模块 →' : '敬请期待' }}</p>
          </component>
        </div>

        <aside class="mp-side">
          <section class="side-card">
            <div class="side-title-row">
              <h3>我的常用</h3>
              <span class="side-manage">管理</span>
            </div>
            <router-link class="side-link" to="/fellowship">联谊</router-link>
            <router-link class="side-link" to="/platform/positive-share">每日心声</router-link>
            <router-link class="side-link" to="/events">活动中心</router-link>
            <router-link class="side-link" to="/articles">内容资讯</router-link>
            <router-link class="side-link" to="/announcements">公告通知</router-link>
          </section>
          <section class="discover-card">
            <div class="discover-bg" aria-hidden="true" />
            <p class="discover-title">发现更多精彩</p>
            <p class="discover-text">̽๦ģ飬ֳ</p>
            <button type="button" class="discover-btn">去探</button>
          </section>
        </aside>
      </section>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchHomeConfig, fetchModulesStats } from '@/api/platformContent.js'

const ICONS = {
  heart:    'M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z',
  calendar: 'M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z',
  article:  'M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z',
  bell:     'M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z',
  map:      'M20.5 3l-.16.03L15 5.1 9 3 3.36 4.9c-.21.07-.36.25-.36.48V20.5c0 .28.22.5.5.5l.16-.03L9 18.9l6 2.1 5.64-1.9c.21-.07.36-.25.36-.48V3.5c0-.28-.22-.5-.5-.5zM15 19l-6-2.11V5l6 2.11V19z',
  robot:    'M20 9V7c0-1.1-.9-2-2-2h-3c0-1.66-1.34-3-3-3S9 3.34 9 5H6c-1.1 0-2 .9-2 2v2c-1.66 0-3 1.34-3 3s1.34 3 3 3v4c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2v-4c1.66 0 3-1.34 3-3s-1.34-3-3-3zm-2 10H6V7h12v12zm-9-6c-.83 0-1.5-.67-1.5-1.5S8.17 10 9 10s1.5.67 1.5 1.5S9.83 13 9 13zm6 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm-3 2.5c-1.38 0-2.5-.56-2.5-1.25h5c0 .69-1.12 1.25-2.5 1.25z'
}

const configuredModules = ref([])
const stats = ref([
  { value: '--', label: '开放模块' },
  { value: '--', label: '使用中' }
])

const defaultModules = [
  {
    moduleKey:   'fellowship',
    name:        '联谊',
    description: '真实资料认证、私信互动与举报治理的联谊社交服务',
    status:      'active',
    entryRoute:  '/fellowship',
    icon:        ICONS.heart,
    iconBg:      '#fff0f4',
    color:       '#f45b7a',
    sort:        1
  },
  {
    moduleKey:   'positive-share',
    name:        '每日心声',
    description: '发布正能量想法、感恩与成长感悟，传递温暖鼓励',
    status:      'active',
    entryRoute:  '/platform/positive-share',
    icon:        ICONS.heart,
    iconBg:      '#fff0f4',
    color:       '#ec4899',
    sort:        2
  },
  {
    moduleKey:   'events',
    name:        '活动中心',
    description: '平台活动、线下聚会主题活动展示与报名',
    status:      'active',
    entryRoute:  '/events',
    icon:        ICONS.calendar,
    iconBg:      '#eff6ff',
    color:       '#1f4fd8',
    sort:        3
  },
  {
    moduleKey:   'articles',
    name:        '内容资讯',
    description: '平台资讯、生活内容干货文章发布与阅读',
    status:      'active',
    entryRoute:  '/articles',
    icon:        ICONS.article,
    iconBg:      '#f0fdf4',
    color:       '#059669',
    sort:        4
  },
  {
    moduleKey:   'announcements',
    name:        '公告通知',
    description: '平台公告与重要通知实时送达',
    status:      'active',
    entryRoute:  '/announcements',
    icon:        ICONS.bell,
    iconBg:      '#ecfeff',
    color:       '#0891b2',
    sort:        5
  },
  {
    moduleKey:   'local-services',
    name:        '本地服务',
    description: '招聘、二手、租房等同城信息',
    status:      'active',
    entryRoute:  '/m/platform/local',
    icon:        ICONS.map,
    iconBg:      '#fffbeb',
    color:       '#d97706',
    sort:        6
  },
  {
    moduleKey:   'ai-tools',
    name:        'AI 工具',
    description: '资料润色、破冰话术等效率助手',
    status:      'active',
    entryRoute:  '/m/fellowship/ai-tools',
    icon:        ICONS.robot,
    iconBg:      '#f5f3ff',
    color:       '#7c3aed',
    sort:        7
  }
]

const moduleIconByKey = {
  fellowship: ICONS.heart,
  'positive-share': ICONS.heart,
  events: ICONS.calendar,
  articles: ICONS.article,
  announcements: ICONS.bell,
  'local-services': ICONS.map,
  'ai-tools': ICONS.robot
}

const moduleColorByTone = {
  'tone-blue': { color: '#1f4fd8', iconBg: '#eff6ff' },
  'tone-cyan': { color: '#0891b2', iconBg: '#ecfeff' },
  'tone-green': { color: '#059669', iconBg: '#f0fdf4' },
  'tone-amber': { color: '#d97706', iconBg: '#fffbeb' },
  'tone-violet': { color: '#7c3aed', iconBg: '#f5f3ff' },
  'tone-rose': { color: '#f45b7a', iconBg: '#fff0f4' }
}

const defaultModuleByKey = Object.fromEntries(defaultModules.map(item => [item.moduleKey, item]))

const modules = computed(() => {
  const source = configuredModules.value.length ? configuredModules.value : defaultModules
  return source
    .filter(item => item.enabled !== false)
    .map((item, index) => normalizeModule(item, index))
    .sort((a, b) => a.sort - b.sort)
})

function normalizeModule(item, index) {
  const fallback = defaultModuleByKey[item.moduleKey] || defaultModules[index] || {}
  const tone = moduleColorByTone[item.tone] || {}
  return {
    moduleKey: item.moduleKey || fallback.moduleKey || `module-${index + 1}`,
    name: item.title || item.name || fallback.name || '',
    description: item.desc || item.description || fallback.description || '',
    status: item.status || fallback.status || 'planned',
    entryRoute: item.to || item.entryRoute || fallback.entryRoute || '',
    icon: moduleIconByKey[item.moduleKey] || fallback.icon || ICONS.article,
    iconBg: tone.iconBg || fallback.iconBg || '#eff6ff',
    color: tone.color || fallback.color || '#1f4fd8',
    sort: Number.isFinite(Number(item.sortOrder ?? item.sort)) ? Number(item.sortOrder ?? item.sort) : index + 1
  }
}

onMounted(async () => {
  const [configRes, statsRes] = await Promise.allSettled([
    fetchHomeConfig(),
    fetchModulesStats()
  ])

  if (configRes.status === 'fulfilled') {
    configuredModules.value = Array.isArray(configRes.value?.modules) ? configRes.value.modules : []
  }

  if (statsRes.status === 'fulfilled' && statsRes.value) {
    const s = statsRes.value
    stats.value = [
      { value: String(s.totalModules ?? '--'), label: '开放模块' },
      { value: String(s.activeModules ?? '--'), label: '使用中' }
    ]
  }
})
</script>

<style scoped>
.modules-page {
  --text: var(--lc-text);
  --muted: var(--lc-muted-light);
  --border: var(--lc-border);
  --card: var(--lc-surface);
  --bg: #f4f7fb;
  --radius: 14px;
  min-height: calc(100vh - 68px);
  background: var(--bg);
  padding: 18px 0 30px;
}

.mc {
  max-width: none;
  width: calc(100% - 48px);
  margin-left: auto;
  margin-right: auto;
}

.mp-hero {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 24px;
  align-items: center;
  background: linear-gradient(135deg, #f8f9ff 0%, #edf2ff 48%, #eaf0ff 100%);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 28px;
}

.mp-title {
  margin: 0;
  font-size: clamp(28px, 3vw, 44px);
  font-weight: 800;
  color: var(--text);
}

.mp-sub {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 15px;
}

.mp-stats {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.mp-stat {
  background: var(--lc-surface);
  border: 1px solid #dbe5f3;
  border-radius: 12px;
  padding: 12px 14px;
}

.mp-stat-value {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: var(--lc-blue-dark);
}

.mp-stat-label {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--muted);
}

.mp-hero-art {
  height: 180px;
  border-radius: 18px;
  position: relative;
  background: linear-gradient(160deg, #f7f8ff 0%, #ecf1ff 100%);
  overflow: hidden;
}

.hero-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.mp-main {
  margin-top: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
}

.mp-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.mp-card {
  background: var(--lc-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 14px;
  text-decoration: none;
  color: inherit;
  transition: 0.2s ease;
  min-height: 150px;
}

.mp-card:not(.mp-card--planned):hover {
  transform: translateY(-3px);
  border-color: var(--lc-blue-border);
  box-shadow: 0 14px 26px rgba(30, 64, 175, 0.1);
}

.mp-card--planned {
  opacity: 0.68;
}

.mp-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mp-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.06);
}

.mp-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
}

.mp-badge--active {
  background: #dcfce7cc;
  color: #166534;
}

.mp-badge--planned {
  background: var(--lc-soft-alt);
  color: var(--lc-muted-light);
}

.mp-name {
  margin: 12px 0 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
}

.mp-desc {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.65;
}

.mp-enter {
  margin: 10px 0 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--lc-blue-mid);
}

.mp-side {
  display: grid;
  gap: 12px;
}

.side-card,
.discover-card,
.mp-recommend {
  background: var(--lc-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}

.side-card {
  padding: 14px;
}

.side-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.side-title-row h3 {
  margin: 0;
  font-size: 16px;
}

.side-manage {
  color: var(--lc-subtle);
  font-size: 12px;
}

.side-link {
  display: block;
  margin-top: 10px;
  text-decoration: none;
  color: var(--lc-slate);
  font-size: 14px;
  border-bottom: 1px solid var(--lc-soft);
  padding-bottom: 8px;
}

.side-link:last-child {
  border-bottom: 0;
}

.discover-card {
  position: relative;
  overflow: hidden;
  padding: 16px;
  background: linear-gradient(150deg, #ffe8f1, #ffe2ef 60%, #ffd7e8);
}

.discover-bg {
  position: absolute;
  right: -8px;
  top: -8px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 32% 38%, rgba(255, 255, 255, 0.86) 0, rgba(255, 255, 255, 0.22) 34%, transparent 62%),
    radial-gradient(circle at 65% 66%, rgba(251, 113, 171, 0.22), transparent 62%);
}

.discover-title {
  position: relative;
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: var(--lc-pink);
}

.discover-text {
  position: relative;
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #9d174d;
}

.discover-btn {
  position: relative;
  margin-top: 12px;
  border: 0;
  border-radius: 999px;
  background: var(--lc-pink);
  color: var(--lc-surface);
  font-size: 13px;
  font-weight: 700;
  height: 32px;
  padding: 0 16px;
  cursor: pointer;
}

.mp-recommend {
  margin-top: 16px;
  padding: 14px;
}

.recommend-title {
  margin: 0;
  font-size: 20px;
}

.recommend-list {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.recommend-item {
  margin: 0;
  border-radius: 12px;
  border: 1px solid #e6eef9;
  padding: 12px;
  background: linear-gradient(145deg, #fdfefe, #f5f9ff);
}

.recommend-item-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--lc-text);
}

.recommend-item-desc {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--lc-muted-light);
}

.recommend-action {
  display: inline-block;
  margin-top: 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--lc-blue);
}

@media (max-width: 1279px) {
  .mp-main {
    grid-template-columns: 1fr;
  }

  .mp-side {
    grid-template-columns: 1fr 1fr;
  }

  .mp-grid,
  .recommend-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 991px) {
  .mp-hero {
    grid-template-columns: 1fr;
  }

  .mp-hero-art {
    height: 130px;
  }

  .mp-grid,
  .recommend-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .mc {
    width: calc(100% - 24px);
  }

  .modules-page {
    padding-top: 10px;
  }

  .mp-hero {
    padding: 16px;
    border-radius: 14px;
  }

  .mp-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mp-side {
    grid-template-columns: 1fr;
  }

  .mp-grid,
  .recommend-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .mp-hero-art {
    display: none;
  }

  .mp-card {
    min-height: 0;
    padding: 12px;
  }
}
</style>


