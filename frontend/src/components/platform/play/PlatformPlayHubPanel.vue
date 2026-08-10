<template>
  <div class="play-hub" :class="`play-hub--${shell}`">
    <header class="play-hub-head">
      <h1>{{ title }}</h1>
      <p>{{ subtitle }}</p>
    </header>

    <section class="play-hub-grid" aria-label="全部功能">
      <router-link
        v-for="item in items"
        :key="itemKey(item)"
        :to="item.to"
        class="play-hub-card"
        :class="item.tone"
      >
        <span class="play-hub-icon" aria-hidden="true">{{ item.icon }}</span>
        <span class="play-hub-title">{{ item.title }}</span>
        <span class="play-hub-sub">{{ item.sub }}</span>
      </router-link>
    </section>

    <section class="play-hub-tip">
      <p>
        {{ tip.text }}
        <router-link v-if="tip.linkTo" :to="tip.linkTo">{{ tip.linkLabel }}</router-link>
        <template v-if="tip.suffix">{{ tip.suffix }}</template>
      </p>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { getPlayHubItems, getPlayHubTip } from '@/constants/platformPlayHub.js'

const props = defineProps({
  shell: {
    type: String,
    default: 'platform',
    validator: value => ['platform', 'pc', 'm'].includes(value)
  },
  title: {
    type: String,
    default: '全部功能'
  },
  subtitle: {
    type: String,
    default: '签到、任务、心声、互助与本地服务 — 移动端功能地图'
  }
})

const items = computed(() => getPlayHubItems(props.shell))
const tip = computed(() => getPlayHubTip(props.shell))

function itemKey(item) {
  if (typeof item.to === 'string') return item.to
  return `${item.to.path}?${JSON.stringify(item.to.query || {})}`
}
</script>

<style scoped>
.play-hub {
  min-height: 100%;
  padding: 20px 16px calc(80px + env(safe-area-inset-bottom, 0px));
  background: linear-gradient(180deg, #f8f9ff 0%, #f4f5fb 180px, #f4f5fb 100%);
}

.play-hub--pc {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  background: transparent;
}

.play-hub-head h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: var(--lc-text, #1a2236);
}

.play-hub--pc .play-hub-head h1 {
  font-size: 28px;
}

.play-hub-head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--lc-subtle, #6b7280);
  line-height: 1.5;
}

.play-hub--pc .play-hub-head p {
  margin-top: 8px;
  font-size: 15px;
}

.play-hub-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 20px;
}

.play-hub--pc .play-hub-grid {
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 28px;
}

.play-hub-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 14px;
  border-radius: 16px;
  text-decoration: none;
  background: var(--lc-surface, #fff);
  border: 1px solid var(--lc-soft, #e8ecf4);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
  min-height: 108px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.play-hub--pc .play-hub-card {
  gap: 6px;
  padding: 22px 20px;
  min-height: 130px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
}

.play-hub-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.08);
}

.play-hub-icon {
  font-size: 22px;
  line-height: 1;
}

.play-hub--pc .play-hub-icon {
  font-size: 26px;
}

.play-hub-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--lc-text, #1a2236);
}

.play-hub--pc .play-hub-title {
  font-size: 17px;
}

.play-hub-sub {
  font-size: 11px;
  color: var(--lc-subtle, #8898aa);
  line-height: 1.4;
}

.play-hub--pc .play-hub-sub {
  font-size: 13px;
}

.play-hub-card.violet { border-color: #ddd6fe; background: linear-gradient(145deg, #fff, #f5f3ff); }
.play-hub-card.orange { border-color: #fed7aa; background: linear-gradient(145deg, #fff, #fff7ed); }
.play-hub-card.pink { border-color: #fbcfe8; background: linear-gradient(145deg, #fff, #fff1f5); }
.play-hub-card.blue { border-color: #bfdbfe; background: linear-gradient(145deg, #fff, #eff6ff); }
.play-hub-card.green { border-color: #bbf7d0; background: linear-gradient(145deg, #fff, #ecfdf5); }
.play-hub-card.indigo { border-color: #c7d2fe; background: linear-gradient(145deg, #fff, #eef2ff); }
.play-hub-card.amber { border-color: #fde68a; background: linear-gradient(145deg, #fff, #fffbeb); }
.play-hub-card.cyan { border-color: #a5f3fc; background: linear-gradient(145deg, #fff, #ecfeff); }

.play-hub-tip {
  margin-top: 20px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px dashed var(--lc-soft, #d8dee9);
}

.play-hub--pc .play-hub-tip {
  margin-top: 28px;
  padding: 0;
  border: none;
  background: transparent;
}

.play-hub-tip p {
  margin: 0;
  font-size: 12px;
  color: var(--lc-subtle, #6b7280);
  line-height: 1.55;
}

.play-hub--pc .play-hub-tip p {
  font-size: 14px;
}

.play-hub-tip a {
  color: var(--lc-blue, #2563eb);
  text-decoration: none;
}

@media (max-width: 980px) {
  .play-hub--pc .play-hub-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 767px) {
  .play-hub-grid {
    gap: 8px;
    margin-top: 14px;
  }

  .play-hub-card {
    min-height: 0;
    padding: 12px 10px;
    gap: 2px;
    border-radius: 12px;
  }

  .play-hub-icon {
    font-size: 18px;
  }

  .play-hub-title {
    font-size: 14px;
  }

  .play-hub-sub {
    font-size: 11px;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .play-hub-tip {
    margin-top: 12px;
    padding: 10px 12px;
  }
}
</style>
