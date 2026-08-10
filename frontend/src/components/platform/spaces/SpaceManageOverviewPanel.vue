<template>
  <section class="overview-panel operation-shell">
    <div class="panel-head">
      <div>
        <p class="section-kicker">Space Operations</p>
        <h2>运营概览</h2>
      </div>
      <button
        type="button"
        class="btn secondary sm"
        :disabled="loading"
        @click="$emit('refresh')"
      >
        {{ loading ? '刷新中…' : '刷新数据' }}
      </button>
    </div>

    <div v-if="loading && !stats" class="inline-state loading-state">加载数据中…</div>
    <div v-else-if="error" class="inline-state error-state">{{ error }}</div>
    <template v-else-if="stats">
      <section class="section-card">
        <div class="section-card-head">
          <div>
            <p class="section-kicker">Metrics</p>
            <h3>关键指标</h3>
          </div>
          <span class="status-badge info">近 7 日 / 30 日</span>
        </div>

        <div class="metric-grid">
          <article
            v-for="metric in keyMetrics"
            :key="metric.key"
            class="metric-card"
            :class="metric.tone"
          >
            <div class="metric-card-head">
              <span class="metric-label">{{ metric.label }}</span>
              <span class="metric-chip">{{ metric.badge }}</span>
            </div>
            <strong>{{ metric.value }}</strong>
            <p>{{ metric.hint }}</p>
          </article>
        </div>
      </section>

      <section class="section-card campaign-card">
        <div class="section-card-head">
          <div>
            <p class="section-kicker">Campaign</p>
            <h3>当前打卡营 / 活动状态</h3>
          </div>
          <span class="status-badge" :class="camp.activeCampaignTitle ? 'success' : 'neutral'">
            {{ camp.activeCampaignTitle ? '进行中' : '未开启' }}
          </span>
        </div>

        <template v-if="camp.activeCampaignTitle">
          <div class="campaign-summary">
            <div class="campaign-primary">
              <strong>{{ camp.activeCampaignTitle }}</strong>
              <p>参与 {{ camp.campaignParticipants ?? 0 }} 人，今日完成 {{ camp.campaignTodayCompleted ?? 0 }} 人</p>
              <button type="button" class="btn secondary sm" @click="$emit('navigate', 'camp')">
                查看打卡营
              </button>
            </div>
            <div class="campaign-progress-panel">
              <div class="progress-label">
                <span>完成进度</span>
                <strong>{{ camp.campaignCompletionRate ?? 0 }}%</strong>
              </div>
              <div class="completion-meter" aria-label="打卡完成率">
                <span :style="{ width: `${safeRate(camp.campaignCompletionRate)}%` }"></span>
              </div>
              <p>{{ fallenBehindTotal > 0 ? `有 ${fallenBehindTotal} 位成员需要提醒` : '当前进度稳定，建议继续保持节奏' }}</p>
            </div>
          </div>

          <div class="metric-grid compact">
            <article class="metric-card">
              <span class="metric-label">参与人数</span>
              <strong>{{ camp.campaignParticipants ?? 0 }}</strong>
              <p>当前营期成员参与规模</p>
            </article>
            <article class="metric-card accent">
              <span class="metric-label">完成进度</span>
              <strong>{{ camp.campaignCompletionRate ?? 0 }}%</strong>
              <p>总完成率</p>
            </article>
            <article class="metric-card">
              <span class="metric-label">今日状态</span>
              <strong>{{ camp.campaignTodayCompleted ?? 0 }}</strong>
              <p>今日已完成打卡人数</p>
            </article>
            <article class="metric-card warn">
              <span class="metric-label">掉队成员</span>
              <strong>{{ fallenBehindTotal }}</strong>
              <p>需要运营提醒</p>
            </article>
          </div>
        </template>
        <p v-else class="empty-note">暂无进行中的打卡营，可从行动建议进入打卡营配置。</p>
      </section>

      <section class="section-card risk-section">
        <div class="section-card-head">
          <div>
            <p class="section-kicker">Risks</p>
            <h3>风险成员</h3>
          </div>
          <span class="status-badge warning">{{ riskTotal }} 项需关注</span>
        </div>

        <div class="risk-grid">
          <article class="risk-card warning">
            <header>
              <span class="risk-dot warning"></span>
              <h4>7 日未活跃</h4>
              <strong>{{ risks.inactiveMembers7d ?? 0 }} 人</strong>
            </header>
            <p class="hint">用于判断 Space 近期沉默风险。</p>
            <button type="button" class="link-button" @click="$emit('navigate', 'members')">查看成员</button>
          </article>

          <article class="risk-card warning">
            <header>
              <span class="risk-dot warning"></span>
              <h4>加入后无活动</h4>
              <strong>{{ noActivityTotal }} 人</strong>
            </header>
            <ul v-if="noActivityList.length" class="member-list">
              <li v-for="m in noActivityList" :key="`na-${m.userId}`">
                <strong>{{ m.username || `成员 ${m.userId}` }}</strong>
                <span class="meta">{{ formatDate(m.joinedAt) }}</span>
              </li>
            </ul>
            <p v-else class="hint">暂无成员进入该风险池。</p>
            <p v-if="noActivityTruncated" class="truncate-hint">仅展示前 20 个</p>
            <button type="button" class="link-button" @click="$emit('navigate', 'members')">跟进成员</button>
          </article>

          <article class="risk-card danger">
            <header>
              <span class="risk-dot danger"></span>
              <h4>打卡营掉队</h4>
              <strong>{{ fallenBehindTotal }} 人</strong>
            </header>
            <ul v-if="fallenBehindList.length" class="member-list">
              <li v-for="m in fallenBehindList" :key="`fb-${m.userId}`">
                <strong>{{ m.username || `成员 ${m.userId}` }}</strong>
                <span class="status-badge warning">缺 {{ m.missedCount ?? 0 }} 天</span>
              </li>
            </ul>
            <p v-else class="hint">暂无掉队成员。</p>
            <p v-if="fallenBehindTruncated" class="truncate-hint">仅展示前 20 个</p>
            <button type="button" class="link-button danger" @click="$emit('navigate', 'camp')">处理掉队</button>
          </article>
        </div>
      </section>

      <section class="section-card action-section">
        <div class="section-card-head">
          <div>
            <p class="section-kicker">Next Actions</p>
            <h3>行动建议</h3>
          </div>
        </div>

        <div class="action-grid">
          <article
            v-for="action in operationActions"
            :key="action.key"
            class="action-card"
            :class="action.tone"
          >
            <div class="action-card-head">
              <span class="todo-dot"></span>
              <span class="status-badge" :class="action.badgeTone">{{ action.badge }}</span>
            </div>
            <h4>{{ action.title }}</h4>
            <p>{{ action.desc }}</p>
            <button type="button" class="btn secondary sm" @click="$emit('navigate', action.target)">
              {{ action.label }}
            </button>
          </article>
        </div>
      </section>
    </template>
    <p v-else class="inline-state empty-state">暂无统计数据</p>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  stats: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' }
})

defineEmits(['refresh', 'navigate'])

const mg = computed(() => props.stats?.memberGrowth ?? {})
const act = computed(() => props.stats?.activity ?? {})
const camp = computed(() => props.stats?.campaign ?? {})
const content = computed(() => props.stats?.content ?? {})
const risks = computed(() => props.stats?.risks ?? {})

const noActivityList = computed(() => risks.value.joinedButNoActivityMembers ?? [])
const fallenBehindList = computed(() => risks.value.fallenBehindMembers ?? [])

const noActivityTotal = computed(() =>
  risks.value.joinedButNoActivityCount ?? noActivityList.value.length
)

const fallenBehindTotal = computed(() => camp.value.fallenBehindCount ?? 0)

const noActivityTruncated = computed(() =>
  noActivityTotal.value > noActivityList.value.length
)

const fallenBehindTruncated = computed(() =>
  fallenBehindTotal.value > fallenBehindList.value.length
)

const riskTotal = computed(() =>
  Number(risks.value.inactiveMembers7d || 0) +
  Number(noActivityTotal.value || 0) +
  Number(fallenBehindTotal.value || 0)
)

const keyMetrics = computed(() => [
  {
    key: 'members',
    label: '成员总数',
    value: mg.value.totalMembers ?? 0,
    hint: `近 7 日新增 ${mg.value.newMembers7d ?? 0} 人`,
    badge: '规模',
    tone: ''
  },
  {
    key: 'active7d',
    label: '7 日活跃',
    value: act.value.activeMembers7d ?? 0,
    hint: `活跃率 ${act.value.activeRate7d ?? 0}%`,
    badge: '活跃',
    tone: 'accent'
  },
  {
    key: 'active30d',
    label: '30 日活跃',
    value: act.value.activeMembers30d ?? act.value.activeMembers7d ?? 0,
    hint: '用于观察中周期留存',
    badge: '留存',
    tone: ''
  },
  {
    key: 'completion',
    label: '打卡完成率',
    value: `${camp.value.campaignCompletionRate ?? 0}%`,
    hint: camp.value.activeCampaignTitle ? '当前营期完成进度' : '暂无进行中营期',
    badge: '进度',
    tone: Number(camp.value.campaignCompletionRate || 0) >= 60 ? 'accent' : 'warn'
  },
  {
    key: 'member-activities',
    label: '成员投稿',
    value: content.value.memberActivitiesSubmitted ?? 0,
    hint: `已通过 ${content.value.memberActivitiesApproved ?? 0} 个`,
    badge: '共创',
    tone: Number(content.value.memberActivitiesSubmitted || 0) > 0 ? 'accent' : ''
  }
])

const operationActions = computed(() => {
  const actions = []

  if (Number(content.value.pendingActivityProposals || 0) > 0) {
    actions.push({
      key: 'pending-activity-proposals',
      badge: `${content.value.pendingActivityProposals} 待审核`,
      badgeTone: 'warning',
      title: '处理成员活动投稿',
      desc: '有成员投稿了新活动，审核通过后将自动发布到活动列表。',
      label: '去审核活动',
      target: 'activity-review',
      tone: 'warning'
    })
  }

  if (Number(mg.value.pendingMembers || 0) > 0) {
    actions.push({
      key: 'pending-members',
      badge: `${mg.value.pendingMembers} 待审核`,
      badgeTone: 'warning',
      title: '先处理成员审核',
      desc: '待审核成员会影响加入体验，建议优先确认是否通过。',
      label: '去审核成员',
      target: 'members',
      tone: 'warning'
    })
  }

  if (noActivityTotal.value > 0 || Number(risks.value.inactiveMembers7d || 0) > 0) {
    actions.push({
      key: 'inactive-members',
      badge: '活跃风险',
      badgeTone: 'warning',
      title: '提醒未活跃成员',
      desc: '可通过公告、活动或打卡任务重新唤起成员参与。',
      label: '发公告',
      target: 'notices',
      tone: 'warning'
    })
  }

  if (fallenBehindTotal.value > 0 || camp.value.activeCampaignTitle) {
    actions.push({
      key: 'campaign',
      badge: camp.value.activeCampaignTitle ? '营期进行中' : '建议开启',
      badgeTone: fallenBehindTotal.value > 0 ? 'danger' : 'info',
      title: fallenBehindTotal.value > 0 ? '查看打卡营掉队情况' : '维护当前打卡营',
      desc: fallenBehindTotal.value > 0
        ? '已有成员掉队，建议进入打卡营查看并提醒。'
        : '持续跟踪参与人数和完成率，必要时发布提醒。',
      label: '查看打卡营',
      target: 'camp',
      tone: fallenBehindTotal.value > 0 ? 'danger' : ''
    })
  }

  if (!actions.length) {
    actions.push(
      {
        key: 'create-activity',
        badge: '增长动作',
        badgeTone: 'info',
        title: '策划一次 Space 活动',
        desc: '当前风险较低，可以通过活动稳定成员参与节奏。',
        label: '建活动',
        target: 'activities',
        tone: ''
      },
      {
        key: 'publish-notice',
        badge: '内容维护',
        badgeTone: 'success',
        title: '发布阶段公告',
        desc: '同步近期安排，让成员知道下一步可以参与什么。',
        label: '发公告',
        target: 'notices',
        tone: ''
      }
    )
  }

  return actions.slice(0, 3)
})

function safeRate(value) {
  const n = Number(value) || 0
  return Math.max(0, Math.min(100, n))
}

function formatDate(v) {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-3);
}

.panel-head h2 {
  margin: 0;
  color: var(--lc-text);
  font-size: 22px;
}

.section-card-head h3 {
  font-size: 16px;
}

.metric-card strong {
  font-size: 32px;
}

.hint,
.empty-note {
  margin: 0;
  color: var(--lc-muted);
  font-size: 13px;
  line-height: 1.55;
}

.campaign-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: var(--lc-space-3);
  margin-top: var(--lc-space-3);
  padding: var(--lc-space-4);
  border: 1px solid var(--lc-blue-border);
  border-radius: 10px;
  background: color-mix(in srgb, var(--lc-blue-light) 58%, var(--lc-surface));
}

.campaign-summary strong {
  color: var(--lc-text);
  font-size: 18px;
}

.campaign-summary p {
  margin: 5px 0 0;
  color: var(--lc-muted);
  font-size: 13px;
  font-weight: 700;
}

.campaign-primary {
  display: grid;
  align-content: start;
  justify-items: start;
  gap: var(--lc-space-2);
}

.campaign-progress-panel {
  display: grid;
  align-content: center;
  gap: var(--lc-space-2);
  min-width: 0;
}

.progress-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-2);
  color: var(--lc-muted);
  font-size: 12px;
  font-weight: 900;
}

.progress-label strong {
  color: var(--lc-blue);
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

.completion-meter {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: color-mix(in srgb, var(--lc-blue-border) 48%, var(--lc-surface));
}

.completion-meter span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--lc-blue);
}

.risk-card header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.risk-card h4,
.action-card h4 {
  margin: 0;
  color: var(--lc-text);
  font-size: 14px;
}

.risk-card header strong {
  color: var(--lc-amber);
  font-size: 13px;
  white-space: nowrap;
}

.risk-card.danger header strong {
  color: var(--lc-red);
}

.member-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: var(--lc-space-2);
}

.member-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-space-2);
  padding: 9px 10px;
  border-radius: 8px;
  border: 1px solid var(--lc-border);
  background: var(--lc-surface);
}

.member-list strong {
  min-width: 0;
  overflow: hidden;
  color: var(--lc-text);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  color: var(--lc-muted);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.todo-dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: var(--lc-blue);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--lc-blue-light) 72%, transparent);
}

.link-button {
  justify-self: start;
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--lc-blue);
  cursor: pointer;
  font-weight: 900;
}

.link-button.danger {
  color: var(--lc-red);
}

.truncate-hint {
  margin: 0;
  color: var(--lc-muted-light);
  font-size: 12px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--lc-border);
  border-radius: 8px;
  background: var(--lc-surface);
  color: var(--lc-text);
  cursor: pointer;
  font-weight: 800;
  text-decoration: none;
}

.btn.secondary {
  background: var(--lc-surface);
}

.btn.secondary:hover:not(:disabled) {
  border-color: var(--lc-blue-border);
  color: var(--lc-blue);
}

.btn.sm {
  min-height: 32px;
  padding: 0 12px;
  font-size: 13px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 980px) {
  .campaign-summary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .panel-head,
  .section-card-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .metric-card p {
    display: none;
  }
}
</style>
