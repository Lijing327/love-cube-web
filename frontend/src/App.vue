<template>
  <div class="app-root">
    <router-view v-slot="{ Component }">
      <Suspense>
        <component :is="Component" />
        <template #fallback>
          <div class="route-fallback" aria-hidden="true"></div>
        </template>
      </Suspense>
    </router-view>
    <PwaShell />
  </div>
</template>

<script setup>
import PwaShell from '@/components/PwaShell.vue'
import { useAppBadge } from '@/composables/useAppBadge.js'

useAppBadge()
</script>

<style scoped>
.app-root {
  min-height: 100vh;
  background: #f4f6fb;
}

.route-fallback {
  min-height: 100vh;
  background: linear-gradient(180deg, #f4f6fb, #eef2f8);
}
</style>
