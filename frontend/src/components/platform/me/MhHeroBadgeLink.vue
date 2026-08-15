<template>
  <router-link class="mh-badge-row" to="/me/badges" aria-label="我的徽章">
    <div class="mh-badge-icons" aria-hidden="true">
      <span
        v-for="(item, index) in displayItems"
        :key="`${item.key}-${index}`"
        class="mh-badge-slot"
        :class="[`tone-${item.key}`, { 'is-locked': item.locked }]"
        :style="{ zIndex: index + 1 }"
      >
        <PlatformBadgeIcon :type="item.key" :locked="item.locked" />
      </span>
    </div>

    <span class="mh-badge-copy">
      <span class="mh-badge-text">我的徽章</span>
      <span v-if="badgeSummary" class="mh-badge-count">{{ badgeSummary }}</span>
      <span class="mh-badge-arrow" aria-hidden="true">›</span>
    </span>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import PlatformBadgeIcon from '@/components/platform/badges/PlatformBadgeIcon.vue'
import { resolveBadgeIconKey } from '@/constants/platformBadges.js'

const props = defineProps({
  badges: {
    type: Array,
    default: () => []
  }
})

const PRESET_KEYS = ['star', 'shield', 'heart', 'medal']

const displayItems = computed(() => {
  const list = Array.isArray(props.badges) ? props.badges : []
  if (list.length) {
    return list.slice(0, 4).map((item, index) => ({
      key: resolveBadgeIconKey(item, index),
      locked: item?.unlocked === false
    }))
  }
  return PRESET_KEYS.map(key => ({ key, locked: false }))
})

const badgeSummary = computed(() => {
  const list = Array.isArray(props.badges) ? props.badges : []
  if (!list.length) return ''
  const unlocked = list.filter(item => item?.unlocked !== false).length
  return `${unlocked}/${list.length}`
})
</script>

<style scoped>
.mh-badge-row {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 6px 8px;
  border-radius: 8px;
  background: var(--lc-blue-light);
  border: 1px solid var(--lc-blue-border);
  box-shadow: none;
  text-decoration: none;
  -webkit-tap-highlight-color: transparent;
  backdrop-filter: none;
}

.mh-badge-row:active {
  background: var(--lc-soft);
}

.mh-badge-icons {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  padding-left: 2px;
}

.mh-badge-slot {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--lc-surface);
  box-shadow: 0 0 0 2px var(--lc-blue-border);
}

.mh-badge-slot + .mh-badge-slot {
  margin-left: -10px;
}

.mh-badge-slot.is-locked {
  box-shadow: 0 0 0 2px var(--lc-border);
  opacity: 0.55;
}

.mh-badge-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.mh-badge-text {
  font-size: 13px;
  font-weight: 700;
  color: var(--lc-text);
  line-height: 1.2;
  white-space: nowrap;
}

.mh-badge-count {
  flex: 0 0 auto;
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--lc-surface);
  color: var(--lc-blue);
  font-size: 11px;
  font-weight: 800;
  line-height: 1.2;
}

.mh-badge-arrow {
  flex: 0 0 auto;
  font-size: 18px;
  font-weight: 400;
  color: var(--lc-subtle);
  line-height: 1;
}

@media (max-width: 390px) {
  .mh-badge-row {
    gap: 10px;
    padding: 8px 10px 8px 8px;
  }

  .mh-badge-slot {
    width: 32px;
    height: 32px;
  }

  .mh-badge-slot + .mh-badge-slot {
    margin-left: -8px;
  }

  .mh-badge-text {
    font-size: 12px;
  }

  .mh-badge-count {
    font-size: 10px;
    padding: 2px 6px;
  }
}

@media (max-width: 340px) {
  .mh-badge-count {
    display: none;
  }
}
</style>
