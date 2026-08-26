import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { submitArticle } from '@/api/platformContent.js'
import { useImageUpload } from '@/composables/useImageUpload.js'

export const ARTICLE_WRITE_LIMITS = {
  title: 80,
  content: 5000,
  tag: 12,
  maxTags: 5,
  summary: 180
}

export const ARTICLE_WRITE_CATEGORIES = [
  '平台资讯',
  '活动指南',
  '活动中心',
  '平台攻略',
  'AI工具',
  '本地服务'
]

export function useArticleWrite(options = {}) {
  const router = useRouter()
  const listPath = options.listPath || '/pc/platform/articles'
  const { pickAndUpload, uploading: coverUploading } = useImageUpload()
  const submitting = ref(false)
  const message = ref('')
  const isError = ref(false)
  const tags = ref([])
  const form = reactive({
    title: '',
    summary: '',
    category: ARTICLE_WRITE_CATEGORIES[0],
    coverUrl: '',
    content: '',
    tagDraft: ''
  })

  function normalizeTag(raw) {
    return String(raw || '')
      .replace(/[,，]/g, ' ')
      .trim()
      .slice(0, ARTICLE_WRITE_LIMITS.tag)
  }

  function addTag(raw) {
    const tag = normalizeTag(raw)
    if (!tag) return
    if (tags.value.includes(tag)) {
      form.tagDraft = ''
      return
    }
    if (tags.value.length >= ARTICLE_WRITE_LIMITS.maxTags) {
      isError.value = true
      message.value = `最多添加 ${ARTICLE_WRITE_LIMITS.maxTags} 个标签`
      return
    }
    tags.value = [...tags.value, tag]
    form.tagDraft = ''
  }

  function removeTag(index) {
    tags.value = tags.value.filter((_, i) => i !== index)
  }

  function onTagKeydown(event) {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault()
      addTag(form.tagDraft)
    } else if (event.key === 'Backspace' && !form.tagDraft && tags.value.length) {
      tags.value = tags.value.slice(0, -1)
    }
  }

  function onTagBlur() {
    addTag(form.tagDraft)
  }

  async function pickCover() {
    message.value = ''
    isError.value = false
    try {
      const url = await pickAndUpload({ quality: 0.8 })
      if (!url) {
        throw new Error('上传失败，请重试')
      }
      form.coverUrl = url
    } catch (error) {
      if (error?.message === '未选择文件') return
      isError.value = true
      message.value = error?.message || '封面图上传失败，请稍后重试'
    }
  }

  function removeCover() {
    form.coverUrl = ''
  }

  function resetForm() {
    form.title = ''
    form.summary = ''
    form.category = ARTICLE_WRITE_CATEGORIES[0]
    form.coverUrl = ''
    form.content = ''
    form.tagDraft = ''
    tags.value = []
  }

  async function submit() {
    message.value = ''
    isError.value = false
    const title = form.title.trim()
    const content = form.content.trim()
    if (!title || !content) {
      isError.value = true
      message.value = '请填写标题和正文'
      return false
    }
    if (form.tagDraft.trim()) {
      addTag(form.tagDraft)
    }
    const summary = form.summary.trim() || content.slice(0, ARTICLE_WRITE_LIMITS.summary)
    const category = ARTICLE_WRITE_CATEGORIES.includes(form.category)
      ? form.category
      : ARTICLE_WRITE_CATEGORIES[0]
    submitting.value = true
    try {
      await submitArticle({
        title,
        content,
        summary,
        category,
        coverUrl: form.coverUrl.trim(),
        tags: tags.value,
        tag: tags.value.join(',')
      })
      message.value = '投稿已提交，待管理员审核发布'
      resetForm()
      window.setTimeout(() => {
        router.push(listPath)
      }, 1000)
      return true
    } catch (error) {
      isError.value = true
      message.value = error?.message || '提交失败，请稍后重试'
      return false
    } finally {
      submitting.value = false
    }
  }

  return {
    form,
    tags,
    submitting,
    coverUploading,
    message,
    isError,
    addTag,
    removeTag,
    onTagKeydown,
    onTagBlur,
    pickCover,
    removeCover,
    submit,
    limits: ARTICLE_WRITE_LIMITS,
    categories: ARTICLE_WRITE_CATEGORIES
  }
}
