<template>
  <section class="articles-m">
    <header class="head">
      <div>
        <h1>精选内容</h1>
        <p>阅读已发布文章，也可以投稿</p>
      </div>
      <router-link to="/m/platform/articles/write" class="write-btn">写文章</router-link>
    </header>

    <p v-if="loading" class="state">加载中…</p>
    <p v-else-if="!list.length" class="state">暂无已发布内容</p>

    <router-link
      v-for="item in list"
      :key="item.id"
      :to="`/articles/${item.id}`"
      class="article-card"
    >
      <h2>{{ item.title }}</h2>
      <p>{{ item.summary || '暂无摘要' }}</p>
      <div class="meta">
        <span v-if="item.tag">{{ item.tag }}</span>
        <time v-if="item.publishDate || item.createdAt">{{ formatDate(item.publishDate || item.createdAt) }}</time>
      </div>
    </router-link>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { usePlatformArticles } from '@/composables/platform/usePlatformArticles.js'

const { loading, list, refresh } = usePlatformArticles()

function formatDate(value) {
  return value ? String(value).slice(0, 10) : ''
}

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.articles-m {
  min-height: 100%;
  background: var(--lc-bg);
  padding: var(--lc-space-4) var(--lc-space-4) calc(var(--lc-space-16) + env(safe-area-inset-bottom));
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--lc-space-3);
  margin-bottom: var(--lc-space-4);
}

.head h1 {
  margin: 0;
  font-size: var(--lc-text-xl);
  font-weight: 800;
}

.head p {
  margin: var(--lc-space-1) 0 0;
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
}

.write-btn {
  flex-shrink: 0;
  height: 36px;
  padding: 0 var(--lc-space-3);
  border-radius: var(--lc-radius-sm);
  background: var(--lc-blue);
  color: var(--lc-surface);
  font-size: var(--lc-text-sm);
  font-weight: 700;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}

.state {
  margin: var(--lc-space-8) 0;
  text-align: center;
  color: var(--lc-subtle);
}

.article-card {
  display: grid;
  gap: var(--lc-space-2);
  margin-bottom: var(--lc-space-3);
  padding: var(--lc-space-4);
  background: var(--lc-surface);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-sm);
  text-decoration: none;
  color: inherit;
}

.article-card h2 {
  margin: 0;
  font-size: var(--lc-text-md);
  font-weight: 700;
}

.article-card p {
  margin: 0;
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
  line-height: 1.5;
}

.meta {
  display: flex;
  gap: var(--lc-space-3);
  color: var(--lc-subtle);
  font-size: var(--lc-text-xs);
}
</style>
