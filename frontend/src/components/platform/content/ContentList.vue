<template>
  <section class="list">
    <ContentCard
      v-for="item in items"
      :key="item.id"
      :item="item"
      @like="$emit('like', item)"
      @comment="$emit('comment', item)"
      @view="$emit('view', item)"
      @share="$emit('share', item)"
    />
  </section>
</template>

<script setup>
import ContentCard from './ContentCard.vue'

defineProps({ items: Array })
defineEmits(['like', 'comment', 'view', 'share'])
</script>

<style scoped>
.list{display:grid;gap:var(--lc-space-3)}
@media (max-width: 767px){
  .list{gap:8px}
}
</style>
