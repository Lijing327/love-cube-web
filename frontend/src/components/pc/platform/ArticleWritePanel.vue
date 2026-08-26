<template>
  <section class="write-pc">
    <header class="write-head">
      <router-link :to="listPath" class="back">← 返回精选内容</router-link>
      <div class="write-head-copy">
        <h1>写文章</h1>
        <p>按精选内容卡片的结构填写：封面、分类、标题、摘要和正文。</p>
      </div>
    </header>

    <div class="write-grid">
      <form class="write-main" @submit.prevent="submit">
        <div
          class="cover-zone"
          :class="{ 'has-cover': form.coverUrl }"
          @click="!form.coverUrl && !coverUploading && pickCover()"
        >
          <img v-if="form.coverUrl" :src="form.coverUrl" alt="封面预览" class="cover-img">
          <span v-if="form.category" class="cover-badge">{{ form.category }}</span>
          <div class="cover-empty" v-if="!form.coverUrl">
            <strong>上传封面</strong>
            <span>选填 · 显示在精选内容卡片顶部</span>
          </div>
          <div class="cover-tools" @click.stop>
            <button type="button" class="cover-btn" :disabled="submitting || coverUploading" @click="pickCover">
              {{ coverUploading ? '上传中…' : (form.coverUrl ? '更换' : '选择图片') }}
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
        </div>

        <div class="write-body">
          <label class="title-field">
            <span class="sr-only">标题</span>
            <input
              v-model="form.title"
              class="title-input"
              type="text"
              :maxlength="limits.title"
              placeholder="请输入标题"
            >
          </label>
          <label class="content-field">
            <span class="sr-only">正文</span>
            <textarea
              v-model="form.content"
              class="content-input"
              :maxlength="limits.content"
              placeholder="写下正文…"
            />
          </label>
          <p class="count">{{ form.content.length }}/{{ limits.content }}</p>
        </div>
      </form>

      <aside class="write-side">
        <section class="side-panel">
          <h2>卡片信息</h2>

          <label class="field">
            <span>分类</span>
            <select v-model="form.category" class="side-select">
              <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>

          <label class="field">
            <span>摘要 <em>{{ form.summary.length }}/{{ limits.summary }}</em></span>
            <textarea
              v-model="form.summary"
              class="summary-input"
              rows="3"
              :maxlength="limits.summary"
              placeholder="一句话介绍，不填则截取正文开头"
            />
          </label>

          <div class="field">
            <span>话题标签</span>
            <div class="tag-box" @click="focusTagInput">
              <span v-for="(tag, index) in tags" :key="`${tag}-${index}`" class="tag-chip">
                {{ tag }}
                <button type="button" class="tag-remove" :aria-label="`移除 ${tag}`" @click="removeTag(index)">×</button>
              </span>
              <input
                ref="tagInputRef"
                v-model="form.tagDraft"
                class="tag-input"
                type="text"
                :maxlength="limits.tag"
                placeholder="回车添加，最多 5 个"
                @keydown="onTagKeydown"
                @blur="onTagBlur"
              >
            </div>
          </div>
        </section>

        <p v-if="message" class="status" :class="{ error: isError }">{{ message }}</p>

        <div class="actions">
          <button type="button" class="btn primary" :disabled="submitting" @click="submit">
            {{ submitting ? '提交中…' : '提交审核' }}
          </button>
          <router-link :to="listPath" class="btn ghost">取消</router-link>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useArticleWrite } from '@/composables/platform/useArticleWrite.js'

const props = defineProps({
  listPath: { type: String, default: '/pc/platform/articles' }
})

const tagInputRef = ref(null)
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

function focusTagInput() {
  tagInputRef.value?.focus()
}
</script>

<style scoped>
.write-pc {
  max-width: 1180px;
  margin: 0 auto;
  padding: var(--lc-space-6) var(--lc-space-6) var(--lc-space-16);
}

.write-head {
  display: flex;
  align-items: baseline;
  gap: var(--lc-space-5);
  margin-bottom: var(--lc-space-5);
}

.back {
  flex-shrink: 0;
  color: var(--lc-blue);
  text-decoration: none;
  font-size: var(--lc-text-sm);
  font-weight: 600;
}

.write-head-copy h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: var(--lc-text);
  letter-spacing: -0.02em;
}

.write-head-copy p {
  margin: 4px 0 0;
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
  line-height: 1.45;
}

.write-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: var(--lc-space-5);
  align-items: start;
}

.write-main,
.side-panel {
  background: var(--lc-surface);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius);
  box-shadow: var(--lc-shadow-sm);
}

.write-main {
  overflow: hidden;
  min-height: 640px;
  display: flex;
  flex-direction: column;
}

.cover-zone {
  position: relative;
  aspect-ratio: 16 / 7;
  background: linear-gradient(180deg, var(--lc-blue-light), var(--lc-soft));
  border-bottom: 1px solid var(--lc-border);
}

.cover-zone:not(.has-cover) {
  cursor: pointer;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-badge {
  position: absolute;
  left: var(--lc-space-4);
  top: var(--lc-space-4);
  z-index: 1;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: var(--lc-surface);
  font-size: 12px;
  font-weight: 700;
}

.cover-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--lc-blue-mid);
  pointer-events: none;
}

.cover-empty strong {
  font-size: var(--lc-text-base);
}

.cover-empty span {
  color: var(--lc-muted);
  font-size: var(--lc-text-xs);
}

.cover-tools {
  position: absolute;
  right: var(--lc-space-4);
  bottom: var(--lc-space-4);
  display: flex;
  gap: var(--lc-space-2);
}

.cover-btn {
  height: 32px;
  padding: 0 12px;
  border: 0;
  border-radius: 999px;
  background: var(--lc-surface);
  color: var(--lc-text);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: var(--lc-shadow-sm);
}

.cover-btn.danger {
  color: var(--lc-red);
}

.cover-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.write-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: var(--lc-space-6);
}

.title-input,
.content-input,
.tag-input {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--lc-text);
  font: inherit;
  outline: none;
}

.title-input {
  padding-bottom: var(--lc-space-3);
  border-bottom: 1px solid var(--lc-border);
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.content-field {
  flex: 1;
  display: flex;
  margin-top: var(--lc-space-4);
}

.content-input {
  flex: 1;
  min-height: 320px;
  resize: vertical;
  line-height: 1.8;
  font-size: var(--lc-text-md);
}

.count {
  margin: var(--lc-space-3) 0 0;
  text-align: right;
  color: var(--lc-subtle);
  font-size: var(--lc-text-xs);
}

.write-side {
  position: sticky;
  top: 88px;
  display: grid;
  gap: var(--lc-space-3);
}

.side-panel {
  display: grid;
  gap: var(--lc-space-4);
  padding: var(--lc-space-5);
}

.side-panel h2 {
  margin: 0;
  font-size: var(--lc-text-base);
  font-weight: 800;
}

.field {
  display: grid;
  gap: var(--lc-space-2);
}

.field span {
  display: flex;
  justify-content: space-between;
  color: var(--lc-slate);
  font-size: var(--lc-text-sm);
  font-weight: 700;
}

.field em {
  font-style: normal;
  font-weight: 500;
  color: var(--lc-subtle);
}

.side-select,
.summary-input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-sm);
  background: var(--lc-soft);
  color: var(--lc-text);
  font: inherit;
  padding: 10px 12px;
}

.summary-input {
  resize: vertical;
  min-height: 84px;
  line-height: 1.55;
}

.tag-box {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-2);
  min-height: 44px;
  padding: var(--lc-space-2);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-sm);
  background: var(--lc-soft);
  cursor: text;
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
  min-width: 88px;
  padding: var(--lc-space-1);
  font-size: var(--lc-text-sm);
}

.status {
  margin: 0;
  font-size: var(--lc-text-sm);
  color: var(--lc-emerald);
}

.status.error {
  color: var(--lc-red);
}

.actions {
  display: grid;
  gap: var(--lc-space-2);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  border-radius: var(--lc-radius-sm);
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
}

.btn.primary {
  border: 0;
  background: var(--lc-blue);
  color: var(--lc-surface);
}

.btn.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.ghost {
  border: 1px solid var(--lc-blue-border);
  background: var(--lc-surface);
  color: var(--lc-blue);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}

@media (max-width: 980px) {
  .write-grid {
    grid-template-columns: 1fr;
  }

  .write-side {
    position: static;
  }

  .write-main {
    min-height: 0;
  }
}
</style>
