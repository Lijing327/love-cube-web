<template>
  <article ref="cardEl" class="content-card section-card" :class="{ 'no-cover': !item.cover }">
    <img v-if="item.cover" class="cover" :src="item.cover" alt="">
    <div class="body">
      <div class="card-head">
        <div class="badges">
          <span class="status-badge" :class="item.pinned ? 'warning' : 'info'">{{ item.pinned ? '官方推荐' : displayType }}</span>
          <span v-if="displayCategory" class="status-badge neutral">{{ displayCategory }}</span>
        </div>
        <span class="status-badge success">已发布</span>
      </div>
      <h4>{{ item.title }}</h4>
      <p class="summary">{{ item.summary || '暂无摘要' }}</p>
      <small class="meta">
        <span class="author">
          <img v-if="item.avatar" :src="item.avatar" alt="" class="meta-avatar">
          <span v-else class="meta-avatar-fallback">{{ authorInitial }}</span>
          <span>{{ item.authorName }}</span>
        </span>
        <span>·</span>
        <span>{{ item.createdAt }}</span>
        <span>·</span>
        <span>阅读 {{ readCount }}</span>
        <span>·</span>
        <span>互动 {{ interactionCount }}</span>
      </small>
      <div class="actions">
        <button type="button" class="platform-btn platform-btn-ghost" @click="$emit('view')">查看</button>
        <button type="button" class="platform-btn platform-btn-ghost" @click="$emit('like')">收藏</button>
        <button type="button" class="platform-btn platform-btn-ghost" @click="$emit('share')">分享</button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { recordGrowthAction } from '@/api/growth.js'

const props = defineProps({ item: Object })
defineEmits(['like', 'comment', 'view', 'share'])

const cardEl = ref(null)
let observer = null

onMounted(() => {
  if (!props.item?.id) return
  observer = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting) {
      const today = new Date().toISOString().slice(0, 10)
      recordGrowthAction({ actionType: 'VIEW_CONTENT', bizId: `VIEW_${today}_${props.item.id}` }).catch(() => {})
      observer.disconnect()
      observer = null
    }
  }, { threshold: 0.5 })
  if (cardEl.value) observer.observe(cardEl.value)
})

onBeforeUnmount(() => { if (observer) { observer.disconnect(); observer = null } })

const authorInitial = computed(() => String(props.item?.authorName || '用户').slice(0, 1))
const interactionCount = computed(() => Number(props.item?.likeCount || 0) + Number(props.item?.commentCount || 0))
const readCount = computed(() => Number(props.item?.readCount || props.item?.viewCount || props.item?.heat || interactionCount.value || 0))
const displayType = computed(() => {
  if (props.item?.type === 'local') return '本地资源'
  if (props.item?.type === 'mood') return '每日心声'
  if (props.item?.type === 'event') return '活动'
  if (props.item?.type === 'dynamic') return '动态'
  if (props.item?.type === 'ai') return 'AI'
  if (props.item?.type === 'guide') return '攻略'
  return '内容'
})
const displayCategory = computed(() => {
  const tags = Array.isArray(props.item?.tags) ? props.item.tags : []
  if (tags.length > 0 && tags[0]) return tags[0]
  if (props.item?.type === 'mood') return '每日心声'
  return ''
})
</script>

<style scoped>
.content-card{display:grid;grid-template-columns:112px minmax(0,1fr);gap:var(--lc-space-4);padding:var(--lc-space-4);box-shadow:none}
.content-card.no-cover{grid-template-columns:1fr}
.cover{width:112px;height:92px;object-fit:cover;border-radius:var(--lc-radius-xs)}
.body{min-width:0;display:grid;gap:var(--lc-space-2)}
.card-head,.badges,.meta,.actions{display:flex;align-items:center;gap:var(--lc-space-2);flex-wrap:wrap}
.card-head{justify-content:space-between}
h4{margin:0;color:var(--lc-text);font-size:var(--lc-text-lg);line-height:1.35}
.summary{margin:0;color:var(--lc-muted);font-size:var(--lc-text-sm);line-height:1.6}
.meta{color:var(--lc-subtle);font-size:var(--lc-text-xs)}
.author{display:inline-flex;align-items:center;gap:6px}
.meta-avatar,.meta-avatar-fallback{width:18px;height:18px;flex:0 0 18px;border-radius:50%;display:inline-flex;align-items:center;justify-content:center;overflow:hidden}
.meta-avatar{object-fit:cover}
.meta-avatar-fallback{background:var(--lc-border);color:var(--lc-muted);font-size:11px}
.actions{justify-content:flex-end;margin-top:var(--lc-space-1)}
@media (max-width: 767px){
  .content-card{grid-template-columns:72px minmax(0,1fr);gap:8px;padding:10px;border-radius:10px}
  .content-card.no-cover{grid-template-columns:1fr}
  .cover{width:72px;height:72px}
  .body{gap:4px}
  h4{font-size:15px;line-height:1.3}
  .summary{font-size:12px;line-height:1.45;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
  .actions{justify-content:flex-start;gap:6px}
  .actions .platform-btn{width:auto;height:28px;padding:0 8px;font-size:12px}
}
</style>
