<template>
  <section class="vision-panel">
    <img class="vision-art" :src="visionImage" alt="" aria-hidden="true">
    <div>
      <p>平台愿景</p>
      <h2>打造一个开放、真诚、有价值的社区，让人与内容相遇，创造更多可能。</h2>
    </div>

    <dl>
      <div>
        <dt>持续开发中</dt>
        <dd>不断优化</dd>
      </div>
      <div>
        <dt>用户共建</dt>
        <dd>共同成长</dd>
      </div>
      <div>
        <dt>长期运营</dt>
        <dd>稳定服务</dd>
      </div>
    </dl>

    <div class="vision-mark" aria-hidden="true">◇</div>
  </section>
</template>

<script setup>
import visionImage from '@/assets/底部横幅 CTA.webp'
</script>

<style scoped>
.vision-panel {
  position: relative;
  display: grid;
  align-content: space-between;
  gap: var(--lc-space-8);
  min-height: 282px;
  padding: var(--lc-space-8);
  border: 1px solid rgba(221, 214, 254, 0.9);
  border-radius: var(--lc-radius);
  background:
    linear-gradient(135deg, rgba(245, 243, 255, 0.92), rgba(239, 246, 255, 0.82));
  box-shadow: var(--lc-shadow-sm);
  overflow: hidden;
}

.vision-panel > *:not(.vision-art) {
  position: relative;
  z-index: 1;
}

.vision-art {
  position: absolute;
  right: 20px;
  bottom: -18px;
  width: 230px;
  opacity: 0.72;
  pointer-events: none;
}

.vision-panel p {
  margin: 0;
  color: var(--lc-blue);
  font-size: var(--lc-text-xl);
  font-weight: 950;
}

.vision-panel h2 {
  max-width: 760px;
  margin: var(--lc-space-3) 0 0;
  color: var(--lc-text);
  font-size: clamp(22px, 1.7vw, 28px);
  line-height: 1.55;
}

.vision-panel dl {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--lc-space-4);
  margin: 0;
}

.vision-panel dl div {
  padding: var(--lc-space-4);
  border-radius: var(--lc-radius-sm);
  background: rgba(255, 255, 255, 0.8);
}

.vision-panel dt {
  color: var(--lc-blue);
  font-size: 20px;
  font-weight: 950;
}

.vision-panel dd {
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  font-weight: 800;
}

.vision-mark {
  position: absolute;
  right: 42px;
  top: 36px;
  display: grid;
  place-items: center;
  width: 128px;
  height: 128px;
  border-radius: var(--lc-radius);
  color: rgba(124, 58, 237, 0.26);
  background: rgba(255, 255, 255, 0.52);
  font-size: 74px;
  transform: rotate(12deg);
}

@media (max-width: 680px) {
  .vision-panel {
    gap: 14px;
    min-height: 0;
    padding: 16px 14px;
    border-radius: 16px;
  }

  .vision-panel p {
    font-size: 18px;
  }

  .vision-panel h2 {
    margin-top: 8px;
    font-size: 20px;
    line-height: 1.42;
  }

  .vision-panel dl {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .vision-panel dl div {
    padding: 10px 8px;
    border-radius: 12px;
  }

  .vision-panel dt {
    font-size: 14px;
    line-height: 1.3;
  }

  .vision-panel dd {
    margin-top: 4px;
    font-size: 12px;
    line-height: 1.35;
  }

  .vision-mark {
    display: none;
  }

  .vision-art {
    display: none;
  }
}

@media (max-width: 390px) {
  .vision-panel {
    gap: 12px;
    padding: 14px 12px;
  }

  .vision-panel p {
    font-size: 17px;
  }

  .vision-panel h2 {
    font-size: 18px;
  }

  .vision-panel dt {
    font-size: 13px;
  }

  .vision-panel dd {
    font-size: 11px;
  }
}
</style>
