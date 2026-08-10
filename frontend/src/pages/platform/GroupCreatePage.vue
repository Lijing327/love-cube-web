<template>
  <section class="create-page" :class="{ 'has-tab-clearance': needsTabClearance }">
    <header class="page-head">
      <router-link class="back-link" :to="groupsPath()">← 返回团体大厅</router-link>
      <h1>创建团体</h1>
      <p>填写资料后即可创建；你将自动成为团体所有者。</p>
    </header>

    <form class="create-form" @submit.prevent="submit">
      <label class="field">
        <span class="label-text">团体名称 <em>*</em></span>
        <input v-model.trim="form.name" type="text" required maxlength="40" placeholder="给你的团体起一个清晰的名字">
      </label>

      <label class="field">
        <span class="label-text">团体简介 <em>*</em></span>
        <textarea v-model.trim="form.description" required maxlength="500" rows="5" placeholder="介绍团体定位、适合人群与活动内容"></textarea>
      </label>

      <label class="field">
        <span class="label-text">团体内称呼 / 真实姓名 <em>*</em></span>
        <input
          v-model.trim="form.memberRealName"
          type="text"
          required
          maxlength="64"
          placeholder="如：李四"
        >
        <p class="hint">仅在成员名册与团体互动中展示，不是全站昵称；不会在平台公开主页外泄露。</p>
      </label>

      <label class="field">
        <span class="label-text">分类 <em>*</em></span>
        <select v-model="form.type" required>
          <option v-for="c in categories" :key="c.value" :value="c.value">{{ c.label }}</option>
        </select>
      </label>

      <fieldset class="field join-field">
        <legend class="label-text">加入方式 <em>*</em></legend>
        <label><input v-model="form.joinMode" type="radio" value="open"> 公开加入</label>
        <label><input v-model="form.joinMode" type="radio" value="audit"> 审核加入</label>
        <label><input v-model="form.joinMode" type="radio" value="invite"> 仅限邀请</label>
      </fieldset>

      <label class="field">
        <span class="label-text">封面图</span>
        <input type="file" accept="image/*" class="file-input" @change="onCoverFile">
        <p v-if="uploadingCover" class="hint">上传中…</p>
        <p v-else-if="form.coverUrl" class="hint preview-row">
          <span>已选择封面</span>
          <img :src="form.coverUrl" alt="" class="thumb">
        </p>
        <p v-else class="hint">可选，支持 JPG / PNG；也可在提交后稍后在团体资料中修改。</p>
      </label>

      <label class="field">
        <span class="label-text">地区（可选）</span>
        <input v-model.trim="form.region" type="text" maxlength="50" placeholder="例如：北京">
      </label>

      <label class="field">
        <span class="label-text">标签（可选）</span>
        <input v-model.trim="form.tags" type="text" maxlength="120" placeholder="多个标签用逗号分隔">
      </label>

      <p v-if="errorMsg" class="err">{{ errorMsg }}</p>

      <div class="actions">
        <router-link class="btn ghost" :to="groupsPath()">取消</router-link>
        <button type="submit" class="btn primary" :disabled="submitting">{{ submitting ? '创建中…' : '创建团体' }}</button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createPlatGroup, spaceManagePath } from '@/api/groups.js'
import { uploadImage } from '@/api/upload.js'
import { usePlatformPath } from '@/composables/usePlatformPath.js'
import { useUserStore } from '@/stores/user.js'
import { PLATFORM_GROUP_CATEGORY_OPTIONS } from '@/utils/groupCategories.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { groupsPath } = usePlatformPath()
/** 平台壳有固定底栏；/m、/pc 壳没有，避免操作条悬空过高 */
const needsTabClearance = computed(() => !route.path.startsWith('/m/') && !route.path.startsWith('/pc/'))

const form = reactive({
  name: '',
  description: '',
  memberRealName: '',
  type: 'interest',
  joinMode: 'audit',
  region: '',
  tags: '',
  coverUrl: ''
})

const categories = PLATFORM_GROUP_CATEGORY_OPTIONS

const submitting = ref(false)
const uploadingCover = ref(false)
const errorMsg = ref('')

onMounted(() => {
  if (!userStore.isLoggedIn) {
    userStore.setPostLoginRedirect(groupsPath('create'))
    router.push('/login')
  }
})

async function onCoverFile(e) {
  const file = e.target?.files?.[0]
  if (!file) return
  uploadingCover.value = true
  errorMsg.value = ''
  try {
    const res = await uploadImage(file)
    const url = res?.url
    if (typeof url === 'string' && url.length > 0) {
      form.coverUrl = url
    } else {
      errorMsg.value = '上传成功但未返回图片地址，请稍后重试或使用下方手动填写封面 URL（若表单后续补充）。'
    }
  } catch (err) {
    errorMsg.value = err?.message || '封面上传失败'
  } finally {
    uploadingCover.value = false
  }
}

async function submit() {
  if (!userStore.isLoggedIn) {
    userStore.setPostLoginRedirect(groupsPath('create'))
    router.push('/login')
    return
  }
  submitting.value = true
  errorMsg.value = ''
  try {
    const payload = {
      name: form.name,
      description: form.description,
      memberRealName: form.memberRealName,
      type: form.type,
      joinMode: form.joinMode,
      region: form.region || undefined,
      tags: form.tags || undefined,
      coverUrl: form.coverUrl || undefined
    }
    const res = await createPlatGroup(payload)
    const id = res?.id ?? res?.data?.id
    if (id) {
      router.replace(spaceManagePath(String(id), { onboarding: '1' }))
    } else {
      errorMsg.value = '创建成功但未返回团体 ID'
    }
  } catch (err) {
    errorMsg.value = err?.message || '创建失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-page {
  box-sizing: border-box;
  width: min(100% - 24px, 640px);
  margin: var(--lc-space-4) auto calc(var(--lc-space-8) + 24px);
  padding: var(--lc-space-8);
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius);
  background: var(--lc-surface);
  box-shadow: var(--lc-shadow-sm);
}

.back-link {
  display: inline-block;
  margin-bottom: var(--lc-space-3);
  color: var(--lc-blue);
  font-weight: 800;
  text-decoration: none;
}

.page-head h1 {
  margin: 0;
  color: var(--lc-text);
  font-size: 28px;
}

.page-head p {
  margin: var(--lc-space-2) 0 0;
  color: var(--lc-muted);
  font-weight: 700;
}

.create-form {
  display: grid;
  gap: var(--lc-space-5);
  margin-top: var(--lc-space-6);
}

.field {
  display: grid;
  gap: var(--lc-space-2);
  min-width: 0;
}

.label-text {
  color: var(--lc-text);
  font-weight: 900;
}

.label-text em {
  color: var(--lc-red);
  font-style: normal;
}

input[type='text'],
input:not([type]),
select,
textarea {
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  padding: var(--lc-space-3);
  font: inherit;
  font-weight: 700;
  color: var(--lc-text);
  background: var(--lc-surface);
}

textarea {
  resize: vertical;
  min-height: 120px;
}

.file-input {
  max-width: 100%;
  font-size: var(--lc-text-sm);
}

.hint {
  margin: 0;
  color: var(--lc-muted);
  font-size: var(--lc-text-sm);
  font-weight: 700;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.preview-row {
  display: flex;
  align-items: center;
  gap: var(--lc-space-3);
}

.thumb {
  width: 120px;
  height: 72px;
  border-radius: var(--lc-radius-xs);
  object-fit: cover;
  border: 1px solid var(--lc-border);
}

.join-field {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-space-4);
  margin: 0;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-xs);
  padding: var(--lc-space-3) var(--lc-space-4);
}

.join-field label {
  display: flex;
  align-items: center;
  gap: var(--lc-space-2);
  color: var(--lc-muted);
  font-weight: 800;
}

.err {
  margin: 0;
  padding: var(--lc-space-2) var(--lc-space-3);
  border-radius: var(--lc-radius-xs);
  color: var(--lc-red);
  background: var(--lc-red-light);
  font-weight: 800;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--lc-space-3);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 var(--lc-space-5);
  border-radius: var(--lc-radius-xs);
  font-weight: 900;
  text-decoration: none;
  cursor: pointer;
}

.btn.primary {
  border: 0;
  color: var(--lc-surface);
  background: linear-gradient(135deg, var(--lc-pink), var(--lc-blue));
  box-shadow: var(--lc-shadow-blue);
}

.btn.primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn.ghost {
  border: 1px solid var(--lc-border);
  color: var(--lc-blue);
  background: var(--lc-surface);
}

/* 移动端：为底部快捷导航预留空间，避免提交按钮被遮挡 */
@media (max-width: 900px) {
  .create-page {
    width: 100%;
    margin: 0 auto;
    padding: 12px 14px calc(100px + env(safe-area-inset-bottom, 0px));
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .page-head h1 {
    font-size: 22px;
  }

  .page-head p {
    font-size: 13px;
    font-weight: 600;
  }

  .create-form {
    gap: 14px;
    margin-top: 16px;
  }

  input[type='text'],
  input:not([type]),
  select,
  textarea {
    font-size: 16px;
    font-weight: 600;
  }

  .join-field {
    display: grid;
    gap: 10px;
  }

  .actions {
    display: grid;
    grid-template-columns: 1fr 1.4fr;
    gap: 10px;
    margin-top: 4px;
  }

  .btn {
    width: 100%;
    min-height: 44px;
  }

  textarea {
    min-height: 88px;
  }

  .page-head .back-link {
    display: none;
  }
}
</style>
