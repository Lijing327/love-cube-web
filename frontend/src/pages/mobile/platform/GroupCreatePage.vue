<template>
  <div class="group-create-m">
    <header class="head">
      <button type="button" class="back" aria-label="返回" @click="goBack()">‹</button>
      <h1>创建团体</h1>
    </header>
    <GroupCreatePage class="body" />
  </div>
</template>

<script setup>
import GroupCreatePage from '@/pages/platform/GroupCreatePage.vue'
import { useBackNavigation } from '@/composables/useBackNavigation.js'

const { goBack } = useBackNavigation('/m/platform/groups')
</script>

<style scoped>
.group-create-m {
  min-height: 100vh;
  background: var(--lc-bg, #f4f5fb);
}

.head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 14px 10px;
  background: #fff;
  border-bottom: 1px solid var(--lc-soft, #e8ecf4);
}

.back {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--lc-soft, #e8ecf4);
  color: var(--lc-text, #0f172a);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.head h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: var(--lc-text, #0f172a);
}

.body {
  display: block;
}

.body :deep(.create-page) {
  padding-top: 12px;
}

.body :deep(.page-head) {
  display: none;
}
</style>
