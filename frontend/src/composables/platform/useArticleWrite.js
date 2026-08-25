import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { submitArticle } from '@/api/platformContent.js'

export const ARTICLE_WRITE_LIMITS = {
  title: 80,
  content: 5000,
  tag: 12,
  maxTags: 5,
  summary: 180
}

export function useArticleWrite(options = {}) {
  const router = useRouter()
  const listPath = options.listPath || '/pc/platform/articles'
  const submitting = ref(false)
  const message = ref('')
  const isError = ref(false)
  const tags = ref([])
  const form = reactive({
    title: '',
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
    submitting.value = true
    try {
      await submitArticle({
        title,
        content,
        summary: content.slice(0, ARTICLE_WRITE_LIMITS.summary),
        tags: tags.value,
        tag: tags.value.join(','),
        category: '平台资讯'
      })
      message.value = '投稿已提交，待管理员审核发布'
      form.title = ''
      form.content = ''
      form.tagDraft = ''
      tags.value = []
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
    message,
    isError,
    addTag,
    removeTag,
    onTagKeydown,
    onTagBlur,
    submit,
    limits: ARTICLE_WRITE_LIMITS
  }
}
