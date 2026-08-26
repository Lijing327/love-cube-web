<template>
  <section class="write-m">
    <header class="head">
      <button type="button" class="back" aria-label="返回" @click="goBack()">‹</button>
      <div>
        <h1>写文章</h1>
        <p>封面、分类会出现在精选内容卡片上，提交后待审核</p>
      </div>
    </header>

    <form class="form" @submit.prevent="submit">
      <label class="field">
        <span>标题 <em>*</em></span>
        <input
          v-model="form.title"
          type="text"
          :maxlength="limits.title"
          placeholder="请输入标题"
        >
      </label>

      <label class="field">
        <span>封面图</span>
        <img v-if="form.coverUrl" :src="form.coverUrl" alt="封面预览" class="cover-preview">
        <div class="cover-actions">
          <button type="button" class="cover-btn" :disabled="submitting || coverUploading" @click="pickCover">
            {{ coverUploading ? '上传中…' : (form.coverUrl ? '更换封面' : '上传封面') }}
          </button>
          <button
            v-if="form.coverUrl"
            type="button"
            class="cover-btn danger"
            :disabled="submitting || coverUploading"
            @click="removeCover"
          >
            删除
          </button>
        </div>
        <small>选填，用于精选内容卡片缩略图</small>
      </label>

      <label class="field">
        <span>分类 <em>*</em></span>
        <select v-model="form.category">
          <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>

      <label class="field">
        <span>摘要</span>
        <textarea
          v-model="form.summary"
          rows="3"
          :maxlength="limits.summary"
          placeholder="一句话介绍这篇文章"
          class="summary"
        />
        <small>选填 {{ form.summary.length }}/{{ limits.summary }}，不填则截取正文开头</small>
      </label>

      <label class="field">
        <span>标签</span>
        <div class="tag-box">
          <span v-for="(tag, index) in tags" :key="`${tag}-${index}`" class="tag-chip">
            {{ tag }}
            <button type="button" class="tag-remove" :aria-label="`移除 ${tag}`" @click="removeTag(index)">×</button>
          </span>
          <input
            v-model="form.tagDraft"
            class="tag-input"
            type="text"
            :maxlength="limits.tag"
            placeholder="输入后回车添加"
            @keydown="onTagKeydown"
            @blur="onTagBlur"
          >
        </div>
        <small>最多 {{ limits.maxTags }} 个，每个不超过 {{ limits.tag }} 字</small>
      </label>

      <label class="field">
        <span>正文 <em>*</em></span>
        <textarea
          v-model="form.content"
          rows="12"
          :maxlength="limits.content"
          placeholder="写下正文…"
        />
        <small>{{ form.content.length }}/{{ limits.content }}</small>
      </label>

      <p v-if="message" class="status" :class="{ error: isError }">{{ message }}</p>

      <button type="submit" class="submit" :disabled="submitting">
        {{ submitting ? '提交中…' : '提交审核' }}
      </button>
    </form>
  </section>
</template>

<script setup>
import { useArticleWrite } from '@/composables/platform/useArticleWrite.js'
import { useBackNavigation } from '@/composables/useBackNavigation.js'

const props = defineProps({
  listPath: { type: String, default: '/m/platform/articles' }
})

const { goBack } = useBackNavigation(props.listPath)
const {
  form,
  tags,
  submitting,
  coverUploading,
  message,
  isError,
  removeTag,
  onTagKeydown,
  onTagBlur,
  pickCover,
  removeCover,
  submit,
  limits,
  categories
} = useArticleWrite({ listPath: props.listPath })
</script>

<style scoped>
.write-m {
  min-height: 100%;
  background: var(--lc-bg);
  padding-bottom: calc(var(--lc-space-16) + env(safe-area-inset-bottom));
}

.head {
  display: flex;
  align-items: center;
  gap: var(--lc-space-2);
  padding: var(--lc-space-4) var(--lc-space-4) var(--lc-space-3);
  background: var(--lc-surface);
  border-bottom: 1px solid var(--lc-border);
}

.back {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--lc-radius-sm);
  background: var(--lc-surface);
  border: 1px solid var(--lc-border);
  color: var(--lc-text);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.head h1 {
  margin: 0;
  font-size: var(--lc-text-lg);
  font-weight: 800;
}

.head p {
  margin: 2px 0 0;
  font-size: var(--lc-text-xs);
  color: var(--lc-muted);
}

.form {
  display: grid;
  gap: var(--lc-space-4);
  padding: var(--lc-space-4);
}

.field {
  display: grid;
  gap: var(--lc-space-2);
}

.field span {
  font-size: var(--lc-text-sm);
  font-weight: 700;
  color: var(--lc-text);
}

.field em {
  color: var(--lc-red);
  font-style: normal;
}

.field input,
.field textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-sm);
  padding: var(--lc-space-3);
  font: inherit;
  background: var(--lc-surface);
  color: var(--lc-text);
}

.field textarea {
  resize: vertical;
  min-height: 220px;
  line-height: 1.7;
}

.field textarea.summary {
  min-height: 88px;
}

.field select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-sm);
  padding: var(--lc-space-3);
  font: inherit;
  background: var(--lc-surface);
  color: var(--lc-text);
}

.cover-preview {
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border-radius: var(--lc-radius-sm);
  border: 1px solid var(--lc-border);
  background: var(--lc-bg);
}

.cover-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
}

.cover-btn {
  height: 36px;
  padding: 0 var(--lc-space-3);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-sm);
  background: var(--lc-surface);
  color: var(--lc-text);
  font-size: var(--lc-text-sm);
  font-weight: 700;
  cursor: pointer;
}

.cover-btn.danger {
  color: var(--lc-red);
  border-color: var(--lc-red-light);
}

.cover-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.field small {
  color: var(--lc-subtle);
  font-size: var(--lc-text-xs);
}

.tag-box {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
  min-height: 44px;
  padding: var(--lc-space-2);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-sm);
  background: var(--lc-surface);
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--lc-space-1);
  padding: 2px var(--lc-space-2);
  border-radius: 999px;
  background: var(--lc-blue-light);
  color: var(--lc-blue-mid);
  font-size: var(--lc-text-sm);
  font-weight: 600;
}

.tag-remove {
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  line-height: 1;
  padding: 0;
}

.tag-input {
  flex: 1;
  min-width: 96px;
  border: 0 !important;
  padding: var(--lc-space-1) !important;
}

.status {
  margin: 0;
  font-size: var(--lc-text-sm);
  color: var(--lc-emerald);
}

.status.error {
  color: var(--lc-red);
}

.submit {
  height: 44px;
  border: 0;
  border-radius: var(--lc-radius-sm);
  background: var(--lc-blue);
  color: var(--lc-surface);
  font-weight: 700;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
