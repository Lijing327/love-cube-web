<template>
  <div class="home-config-admin">
    <header class="hc-head">
      <h2>首页配置</h2>
      <p>配置首页 Hero、模块封面和能力图片，所有图片通过上传按钮管理，无需手动填写 URL。</p>
    </header>

    <section class="hc-panel">
      <div class="panel-head">
        <h3>Hero 配置</h3>
      </div>
      <div class="hero-form">
        <label class="field">
          <span>标题</span>
          <input v-model="form.hero.title" class="admin-input" type="text">
        </label>
        <label class="field">
          <span>副标题</span>
          <textarea v-model="form.hero.subtitle" class="admin-textarea" rows="3"></textarea>
        </label>
        <div class="grid-2">
          <label class="field">
            <span>主按钮文案</span>
            <input v-model="form.hero.primaryText" class="admin-input" type="text">
          </label>
          <label class="field">
            <span>主按钮链接</span>
            <input v-model="form.hero.primaryLink" class="admin-input" type="text" placeholder="/modules">
          </label>
          <label class="field">
            <span>次按钮文案</span>
            <input v-model="form.hero.secondaryText" class="admin-input" type="text">
          </label>
          <label class="field">
            <span>次按钮链接</span>
            <input v-model="form.hero.secondaryLink" class="admin-input" type="text" placeholder="/articles">
          </label>
        </div>
        <div class="field">
          <span>Hero 图片</span>
          <CoverUploadField v-model="form.hero.imageUrl" :allow-manual-input="false" />
        </div>
      </div>
    </section>

    <section class="hc-panel">
      <div class="panel-head">
        <h3>模块图片配置</h3>
      </div>
      <div class="list-grid">
        <article v-for="item in form.modules" :key="item.moduleKey" class="item-card">
          <div class="item-head">
            <strong>{{ item.title || item.moduleKey }}</strong>
            <label class="inline-check">
              <input v-model="item.enabled" type="checkbox">
              展示
            </label>
          </div>
          <label class="field">
            <span>模块名称</span>
            <input v-model="item.title" class="admin-input" type="text">
          </label>
          <label class="field">
            <span>模块说明</span>
            <textarea v-model="item.desc" class="admin-textarea" rows="2"></textarea>
          </label>
          <div class="grid-2">
            <label class="field">
              <span>排序</span>
              <input v-model.number="item.sortOrder" class="admin-input" type="number" min="0">
            </label>
            <label class="field">
              <span>状态</span>
              <select v-model="item.status" class="admin-input">
                <option value="active">已开启</option>
                <option value="planned">规划中</option>
              </select>
            </label>
          </div>
          <div class="field">
            <span>模块封面</span>
            <CoverUploadField v-model="item.coverUrl" :allow-manual-input="false" />
          </div>
        </article>
      </div>
    </section>

    <section class="hc-panel">
      <div class="panel-head">
        <h3>首页能力图配置</h3>
      </div>
      <div class="list-grid list-grid-ability">
        <article v-for="item in form.abilities" :key="item.abilityKey" class="item-card">
          <div class="item-head">
            <strong>{{ item.title || item.abilityKey }}</strong>
            <label class="inline-check">
              <input v-model="item.enabled" type="checkbox">
              展示
            </label>
          </div>
          <label class="field">
            <span>能力名称</span>
            <input v-model="item.title" class="admin-input" type="text">
          </label>
          <label class="field">
            <span>能力说明</span>
            <textarea v-model="item.desc" class="admin-textarea" rows="2"></textarea>
          </label>
          <label class="field">
            <span>排序</span>
            <input v-model.number="item.sortOrder" class="admin-input" type="number" min="0">
          </label>
          <div class="field">
            <span>能力图片</span>
            <CoverUploadField v-model="item.imageUrl" :allow-manual-input="false" />
          </div>
        </article>
      </div>
    </section>

    <section class="hc-panel">
      <div class="panel-head">
        <h3>平台基础能力背景</h3>
      </div>
      <div class="field">
        <span>宽幅背景图 / 插画</span>
        <CoverUploadField v-model="form.foundation.imageUrl" :allow-manual-input="false" />
      </div>
    </section>

    <section class="hc-panel">
      <div class="panel-head panel-head-with-action">
        <div>
          <h3>用户更新日志</h3>
          <p class="panel-hint">展示于平台「更新日志」页（`/platform/changelog`），仅写用户能感知的功能与体验。</p>
        </div>
        <button type="button" class="admin-btn small" @click="addChangelogItem">新增用户日志</button>
      </div>
      <div class="list-grid list-grid-changelog">
        <article v-for="(item, index) in form.changelog" :key="`${item.version || 'log'}-${index}`" class="item-card">
          <div class="item-head">
            <strong>{{ item.version || `日志 ${index + 1}` }}</strong>
            <button type="button" class="text-btn danger" @click="removeChangelogItem(index)">删除</button>
          </div>
          <div class="grid-2">
            <label class="field">
              <span>版本号</span>
              <input v-model.trim="item.version" class="admin-input" type="text" placeholder="v1.5.0">
            </label>
            <label class="field">
              <span>日期</span>
              <input v-model.trim="item.date" class="admin-input" type="date">
            </label>
          </div>
          <label class="field">
            <span>更新标题</span>
            <input v-model.trim="item.title" class="admin-input" type="text" placeholder="本次版本更新内容">
          </label>
          <label class="field">
            <span>更新详情</span>
            <textarea
              v-model="item.detail"
              class="admin-textarea"
              rows="4"
              placeholder="用户可见要点，每行一条（如：• 新增某某功能）"
            />
          </label>
          <div class="grid-2">
            <label class="field">
              <span>排序</span>
              <input v-model.number="item.sortOrder" class="admin-input" type="number" min="0">
            </label>
            <label class="inline-check">
              <input v-model="item.enabled" type="checkbox">
              展示
            </label>
          </div>
        </article>
      </div>
    </section>

    <section class="hc-panel hc-panel-internal">
      <div class="panel-head panel-head-with-action">
        <div>
          <h3>后台更新记录</h3>
          <p class="panel-hint">仅保存在管理端，不对用户展示。可记录表结构、接口、定时任务、投递等技术变更。</p>
        </div>
        <button type="button" class="admin-btn small" @click="addAdminChangelogItem">新增后台记录</button>
      </div>
      <div class="list-grid list-grid-changelog">
        <article
          v-for="(item, index) in form.adminChangelog"
          :key="`${item.version || 'admin-log'}-${index}`"
          class="item-card item-card-internal"
        >
          <div class="item-head">
            <strong>{{ item.version || `后台记录 ${index + 1}` }}</strong>
            <button type="button" class="text-btn danger" @click="removeAdminChangelogItem(index)">删除</button>
          </div>
          <div class="grid-2">
            <label class="field">
              <span>版本号</span>
              <input v-model.trim="item.version" class="admin-input" type="text" placeholder="v1.6.0">
            </label>
            <label class="field">
              <span>日期</span>
              <input v-model.trim="item.date" class="admin-input" type="date">
            </label>
          </div>
          <label class="field">
            <span>记录标题</span>
            <input v-model.trim="item.title" class="admin-input" type="text" placeholder="后台变更摘要">
          </label>
          <label class="field">
            <span>技术详情</span>
            <textarea
              v-model="item.detail"
              class="admin-textarea"
              rows="4"
              placeholder="迁移版本、服务类、表名等，每行一条"
            />
          </label>
          <div class="grid-2">
            <label class="field">
              <span>排序</span>
              <input v-model.number="item.sortOrder" class="admin-input" type="number" min="0">
            </label>
            <label class="inline-check">
              <input v-model="item.enabled" type="checkbox">
              保留
            </label>
          </div>
        </article>
      </div>
    </section>

    <section class="hc-panel">
      <div class="panel-head panel-head-with-action">
        <h3>待更新配置</h3>
        <button type="button" class="admin-btn small" @click="addPendingUpdateItem">新增待更新</button>
      </div>
      <div class="list-grid list-grid-changelog">
        <article v-for="(item, index) in form.pendingUpdates" :key="`${item.id || 'pending'}-${index}`" class="item-card">
          <div class="item-head">
            <strong>{{ item.title || `待更新 ${index + 1}` }}</strong>
            <button type="button" class="text-btn danger" @click="removePendingUpdateItem(index)">删除</button>
          </div>
          <label class="field">
            <span>标题</span>
            <input v-model.trim="item.title" class="admin-input" type="text" placeholder="问题标题">
          </label>
          <label class="field">
            <span>说明</span>
            <textarea v-model.trim="item.detail" class="admin-textarea" rows="3" placeholder="告诉用户当前进展与计划"></textarea>
          </label>
          <div class="grid-2">
            <label class="field">
              <span>状态标签</span>
              <input v-model.trim="item.status" class="admin-input" type="text" placeholder="排查中 / 开发中 / 待发布">
            </label>
            <label class="field">
              <span>唯一 ID</span>
              <input v-model.trim="item.id" class="admin-input" type="text" placeholder="pending-1">
            </label>
          </div>
        </article>
      </div>
    </section>

    <div class="actions">
      <button type="button" class="admin-btn" :disabled="loading" @click="loadConfig">
        {{ loading ? '加载中...' : '重新加载' }}
      </button>
      <button type="button" class="admin-btn primary" :disabled="saving" @click="saveConfig">
        {{ saving ? '保存中...' : '保存首页配置' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { showToast } from 'vant'
import CoverUploadField from '@/components/admin/CoverUploadField.vue'
import { getAdminHomeConfig, saveAdminHomeConfig } from '@/api/adminContent.js'

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  hero: {
    title: '',
    subtitle: '',
    primaryText: '',
    primaryLink: '',
    secondaryText: '',
    secondaryLink: '',
    imageUrl: ''
  },
  modules: [],
  abilities: [],
  foundation: {
    imageUrl: ''
  },
  changelog: [],
  adminChangelog: [],
  pendingUpdates: []
})

function applyConfig(data) {
  form.hero = { ...form.hero, ...(data?.hero || {}) }
  form.modules = Array.isArray(data?.modules) ? data.modules.map(item => ({ ...item })) : []
  form.abilities = Array.isArray(data?.abilities) ? data.abilities.map(item => ({ ...item })) : []
  form.foundation = { ...form.foundation, ...(data?.foundation || {}) }
  form.changelog = Array.isArray(data?.changelog)
    ? data.changelog.map((item, index) => ({ ...item, sortOrder: Number(item?.sortOrder ?? index) }))
    : []
  form.adminChangelog = Array.isArray(data?.adminChangelog)
    ? data.adminChangelog.map((item, index) => ({ ...item, sortOrder: Number(item?.sortOrder ?? index) }))
    : []
  form.pendingUpdates = Array.isArray(data?.pendingUpdates)
    ? data.pendingUpdates.map((item, index) => ({ ...item, id: item?.id || `pending-${index + 1}` }))
    : []
}

async function loadConfig() {
  loading.value = true
  try {
    const data = await getAdminHomeConfig()
    applyConfig(data)
  } catch (error) {
    showToast({ type: 'fail', message: error.message || '加载首页配置失败' })
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  saving.value = true
  try {
    const payload = {
      hero: form.hero,
      modules: form.modules,
      abilities: form.abilities,
      foundation: form.foundation,
      changelog: form.changelog,
      adminChangelog: form.adminChangelog,
      pendingUpdates: form.pendingUpdates
    }
    const result = await saveAdminHomeConfig(payload)
    applyConfig(result)
    showToast({ type: 'success', message: '首页配置已保存' })
  } catch (error) {
    showToast({ type: 'fail', message: error.message || '保存首页配置失败' })
  } finally {
    saving.value = false
  }
}

function addChangelogItem() {
  form.changelog.push({
    version: '',
    title: '',
    date: '',
    detail: '',
    enabled: true,
    sortOrder: form.changelog.length
  })
}

function removeChangelogItem(index) {
  if (index < 0 || index >= form.changelog.length) return
  form.changelog.splice(index, 1)
}

function addAdminChangelogItem() {
  form.adminChangelog.push({
    version: '',
    title: '',
    date: '',
    detail: '',
    enabled: true,
    sortOrder: form.adminChangelog.length
  })
}

function removeAdminChangelogItem(index) {
  if (index < 0 || index >= form.adminChangelog.length) return
  form.adminChangelog.splice(index, 1)
}

function addPendingUpdateItem() {
  form.pendingUpdates.push({
    id: `pending-${form.pendingUpdates.length + 1}`,
    title: '',
    detail: '',
    status: '排查中'
  })
}

function removePendingUpdateItem(index) {
  if (index < 0 || index >= form.pendingUpdates.length) return
  form.pendingUpdates.splice(index, 1)
}

loadConfig()
</script>

<style scoped>
.home-config-admin {
  display: grid;
  gap: 16px;
  padding: 8px 0 20px;
}

.hc-head h2 {
  margin: 0;
  font-size: 24px;
  color: var(--lc-text);
}

.hc-head p {
  margin: 8px 0 0;
  color: var(--lc-muted-light);
}

.hc-panel {
  background: var(--lc-surface);
  border: 1px solid var(--lc-border);
  border-radius: 14px;
  padding: 18px;
  display: grid;
  gap: 14px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
  color: var(--lc-text);
}

.panel-head-with-action {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.panel-hint {
  margin: 6px 0 0;
  color: var(--lc-muted-light);
  font-size: 13px;
  line-height: 1.45;
  max-width: 52rem;
}

.hc-panel-internal {
  border-color: color-mix(in srgb, var(--lc-amber) 35%, var(--lc-border));
  background: color-mix(in srgb, var(--lc-amber) 6%, var(--lc-surface));
}

.item-card-internal {
  border-style: dashed;
}

.hero-form,
.field {
  display: grid;
  gap: 8px;
}

.field > span {
  font-size: 13px;
  color: var(--lc-muted);
  font-weight: 700;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.list-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.list-grid-ability {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.item-card {
  border: 1px solid var(--lc-border);
  border-radius: 12px;
  padding: 14px;
  display: grid;
  gap: 10px;
  background: var(--lc-bg);
}

.item-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.list-grid-changelog {
  grid-template-columns: 1fr;
}

.inline-check {
  display: inline-flex;
  gap: 6px;
  font-size: 12px;
  color: var(--lc-muted);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.admin-btn.small {
  padding: 6px 12px;
  font-size: 12px;
}

.text-btn {
  border: 0;
  background: transparent;
  color: var(--lc-blue);
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}

.text-btn.danger {
  color: var(--lc-red);
}

@media (max-width: 1023px) {
  .list-grid,
  .list-grid-ability,
  .grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>



