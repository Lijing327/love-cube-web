<template>
  <MobileWrite v-if="isWrite && isMobile" list-path="/platform/articles" />
  <PcWrite v-else-if="isWrite" list-path="/platform/articles" />
  <Mobile v-else-if="isMobile" write-path="/platform/articles?write=1" />
  <Desktop v-else write-path="/platform/articles?write=1" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useDevice } from '@/composables/useDevice.js'
import Desktop from '@/components/platform/articles/ArticlesPage.desktop.vue'
import Mobile from '@/components/platform/articles/ArticlesPage.mobile.vue'
import PcWrite from '@/components/pc/platform/ArticleWritePanel.vue'
import MobileWrite from '@/components/mobile/platform/ArticleWritePanel.vue'

const route = useRoute()
const { isMobile } = useDevice()
const isWrite = computed(() => String(route.query.write || '') === '1')
</script>
