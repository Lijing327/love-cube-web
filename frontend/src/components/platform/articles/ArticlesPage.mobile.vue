<template>
  <ArticlesDesktop :write-path="writePath" />
</template>

<script setup>
import ArticlesDesktop from '@/components/platform/articles/ArticlesPage.desktop.vue'

defineProps({
  writePath: { type: String, default: '/platform/articles?write=1' }
})
</script>
