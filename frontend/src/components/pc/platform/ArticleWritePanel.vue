<template>
  <section class="write-pc">
    <header class="write-head">
      <router-link :to="listPath" class="back">← 返回精选内容</router-link>
      <div>
        <p class="kicker">投稿</p>
        <h1>写文章</h1>
        <p>填写标题、正文和标签。提交后由管理员审核，通过后会出现在精选内容。</p>
      </div>
    </header>

    <div class="write-grid">
      <form class="write-main" @submit.prevent="submit">
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
      </form>

      <aside class="write-side">
        <section class="side-card">
          <h2>标签</h2>
          <p class="hint">回车或逗号添加，最多 {{ limits.maxTags }} 个</p>
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
              placeholder="添加标签"
              @keydown="onTagKeydown"
              @blur="onTagBlur"
            >
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
  message,
  isError,
  removeTag,
  onTagKeydown,
  onTagBlur,
  submit,
  limits
} = useArticleWrite({ listPath: props.listPath })

function focusTagInput() {
  tagInputRef.value?.focus()
}
</script>

<style scoped>
.write-pc {
  max-width: 1120px;
  margin: 0 auto;
  padding: var(--lc-space-8) var(--lc-space-6) var(--lc-space-16);
}

.write-head {
  display: flex;
  align-items: flex-start;
  gap: var(--lc-space-5);
  margin-bottom: var(--lc-space-8);
}

.back {
  flex-shrink: 0;
  padding-top: var(--lc-space-1);
  color: var(--lc-blue);
  text-decoration: none;
  font-size: var(--lc-text-base);
}

.kicker {
  margin: 0 0 var(--lc-space-1);
  font-size: var(--lc-text-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--lc-blue);
  font-weight: 700;
}

.write-head h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: var(--lc-text);
}

.write-head p {
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  font-size: var(--lc-text-base);
  line-height: 1.5;
}

.write-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: var(--lc-space-6);
  align-items: start;
}

.write-main,
.side-card {
  background: var(--lc-surface);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius);
  box-shadow: var(--lc-shadow-sm);
}

.write-main {
  display: flex;
  flex-direction: column;
  min-height: 560px;
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
  padding-bottom: var(--lc-space-4);
  border-bottom: 1px solid var(--lc-border);
  font-size: 28px;
  font-weight: 700;
}

.content-field {
  flex: 1;
  display: flex;
  margin-top: var(--lc-space-4);
}

.content-input {
  flex: 1;
  min-height: 420px;
  resize: vertical;
  line-height: 1.75;
  font-size: var(--lc-text-md);
}

.count {
  margin: var(--lc-space-3) 0 0;
  text-align: right;
  color: var(--lc-subtle);
  font-size: var(--lc-text-xs);
}

.side-card {
  padding: var(--lc-space-5);
}

.side-card h2 {
  margin: 0;
  font-size: var(--lc-text-lg);
}

.hint {
  margin: var(--lc-space-1) 0 var(--lc-space-3);
  color: var(--lc-subtle);
  font-size: var(--lc-text-sm);
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
  margin: var(--lc-space-4) 0 0;
  font-size: var(--lc-text-sm);
  color: var(--lc-emerald);
}

.status.error {
  color: var(--lc-red);
}

.actions {
  display: grid;
  gap: var(--lc-space-2);
  margin-top: var(--lc-space-4);
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

  .write-main {
    min-height: 420px;
  }
}
</style>
