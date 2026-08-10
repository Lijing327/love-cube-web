<template>
  <section class="stats-band" aria-label="平台数据">
    <article v-for="item in platformStats" :key="item.label" class="stat-card">
      <span class="stat-icon" :class="item.tone">{{ item.icon }}</span>
      <strong>{{ item.value }}</strong>
      <small>{{ item.label }}</small>
      <em>{{ item.delta }}</em>
    </article>
  </section>
</template>

<script setup>
defineProps({
  platformStats: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.stats-band {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 1px;
  margin-top: 22px;
  padding: 16px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--lc-indigo-light), var(--lc-bg));
}

.stat-card {
  display: grid;
  grid-template-columns: 46px 1fr;
  gap: 5px 12px;
  align-items: center;
  min-height: 88px;
  padding: 14px;
  border: 1px solid var(--lc-border);
  border-radius: 8px;
  background: var(--lc-surface);
}

.stat-icon {
  grid-row: 1 / 4;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 900;
  background: var(--tone-bg);
  color: var(--tone);
}

.stat-card strong {
  color: var(--lc-text);
  font-size: 19px;
}

.stat-card small {
  color: var(--lc-muted);
  font-size: 12px;
}

.stat-card em {
  color: var(--lc-green);
  font-size: 12px;
  font-style: normal;
}

.tone-pink { --tone: var(--lc-pink); --tone-bg: var(--lc-pink-light); }
.tone-violet { --tone: var(--lc-indigo); --tone-bg: var(--lc-indigo-light); }
.tone-blue { --tone: var(--lc-blue); --tone-bg: var(--lc-blue-light); }
.tone-orange { --tone: var(--lc-orange); --tone-bg: var(--lc-orange-light); }
.tone-green { --tone: var(--lc-green); --tone-bg: var(--lc-emerald-light); }
.tone-purple { --tone: var(--lc-purple); --tone-bg: var(--lc-indigo-soft); }

@media (max-width: 1180px) {
  .stats-band {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .stats-band {
    display: flex;
    flex-wrap: nowrap;
    gap: 0;
    margin-top: 12px;
    padding: 8px 4px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .stat-card {
    flex: 1 1 0;
    min-width: 72px;
    min-height: 0;
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
    gap: 2px;
    padding: 8px 4px;
    border: 0;
    border-radius: 0;
    background: transparent;
  }

  .stat-card:not(:first-child) {
    border-left: 1px solid var(--lc-border);
  }

  .stat-icon {
    grid-row: auto;
    width: 28px;
    height: 28px;
    font-size: 12px;
  }

  .stat-card strong {
    font-size: 15px;
  }

  .stat-card small {
    font-size: 11px;
  }

  .stat-card em {
    display: none;
  }
}
</style>
