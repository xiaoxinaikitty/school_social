<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch, apiUpload } from '../utils/api'

const router = useRouter()
const title = ref('')
const content = ref('')
const postType = ref(0)
const visibility = ref(0)
const location = ref('')
const college = ref('')
const isDraft = ref(false)
const loading = ref(false)
const error = ref('')
const success = ref('')

const availableTags = ref([])
const selectedTagIds = ref([])
const loadingTags = ref(false)
const tagsError = ref('')

const mediaInputs = ref([])
const uploading = ref(false)
const uploadError = ref('')
const fileInputRef = ref(null)

const removeMedia = (index) => {
  mediaInputs.value.splice(index, 1)
  mediaInputs.value.forEach((item, idx) => {
    item.sortOrder = idx + 1
  })
}

const triggerUpload = () => {
  uploadError.value = ''
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

const handleFiles = async (event) => {
  const files = Array.from(event.target.files || [])
  if (!files.length) return
  uploading.value = true
  uploadError.value = ''
  try {
    for (const file of files) {
      const formData = new FormData()
      formData.append('file', file)
      const res = await apiUpload('/api/upload', formData)
      if (res.status === 401) {
        router.push('/login')
        return
      }
      const data = await res.json()
      if (!res.ok || data.code !== 0) {
        uploadError.value = data.message || '上传失败。'
        return
      }
      const url = data.data?.url
      if (url) {
        mediaInputs.value.push({
          url,
          mediaType: file.type.startsWith('video/') ? 1 : 0,
          sortOrder: mediaInputs.value.length + 1,
        })
      }
    }
  } catch (err) {
    uploadError.value = '网络错误，上传失败。'
  } finally {
    uploading.value = false
    if (event.target) {
      event.target.value = ''
    }
  }
}

const loadAvailableTags = async () => {
  loadingTags.value = true
  tagsError.value = ''
  try {
    const res = await apiFetch('/api/tags')
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagsError.value = data.message || '获取标签列表失败。'
      return
    }
    availableTags.value = data.data || []
  } catch (err) {
    tagsError.value = '网络错误，无法获取标签列表。'
  } finally {
    loadingTags.value = false
  }
}

const submit = async () => {
  error.value = ''
  success.value = ''
  if (!content.value.trim()) {
    error.value = '请输入正文内容。'
    return
  }
  loading.value = true
  try {
    const media = mediaInputs.value
      .filter((item) => item.url && item.url.trim())
      .map((item) => ({
        url: item.url.trim(),
        mediaType: item.mediaType,
        sortOrder: item.sortOrder,
      }))

    const payload = {
      title: title.value.trim() || null,
      content: content.value.trim(),
      postType: Number(postType.value),
      visibility: Number(visibility.value),
      location: location.value.trim() || null,
      college: college.value.trim() || null,
      draft: isDraft.value,
      tagIds: selectedTagIds.value,
      media,
    }

    const res = await apiFetch('/api/posts', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '发布失败，请检查输入。'
      return
    }
    success.value = isDraft.value ? '草稿已保存。' : '发布成功。'
    setTimeout(() => {
      router.push('/home')
    }, 800)
  } catch (err) {
    error.value = '网络错误，请稍后再试。'
  } finally {
    loading.value = false
  }
}

onMounted(loadAvailableTags)
</script>

<template>
  <div class="post-page">
    <header class="profile-hero">
      <div>
        <span class="hero-tag">内容发布</span>
        <h1>发布你的校园动态</h1>
        <p>支持图文链接、话题标签与可见范围设置。</p>
      </div>
      <div class="profile-actions">
        <button class="ghost-btn" type="button" @click="router.push('/home')">返回主页</button>
      </div>
    </header>

    <section class="profile-section">
      <form class="profile-form" @submit.prevent="submit">
        <div class="grid-2">
          <label class="field">
            <span>标题（可选）</span>
            <input v-model="title" type="text" maxlength="200" placeholder="给内容一个标题" />
          </label>
          <label class="field">
            <span>类型</span>
            <select v-model="postType">
              <option value="0">图文动态</option>
              <option value="1">活动信息</option>
              <option value="2">投票</option>
              <option value="3">二手交易</option>
            </select>
          </label>
        </div>

        <label class="field">
          <span>正文</span>
          <textarea v-model="content" rows="6" placeholder="分享校园动态、活动、学习经验等" required></textarea>
        </label>

        <div class="grid-2">
          <label class="field">
            <span>可见范围</span>
            <select v-model="visibility">
              <option value="0">全校可见</option>
              <option value="1">仅关注可见</option>
              <option value="2">仅自己可见</option>
            </select>
          </label>
          <label class="field">
            <span>位置</span>
            <input v-model="location" type="text" placeholder="图书馆 / 教学楼" />
          </label>
        </div>

        <div class="grid-2">
          <label class="field">
            <span>学院</span>
            <input v-model="college" type="text" placeholder="学院名称" />
          </label>
          <label class="field">
            <span>保存为草稿</span>
            <select v-model="isDraft">
              <option :value="false">否</option>
              <option :value="true">是</option>
            </select>
          </label>
        </div>

        <div class="profile-header">
          <div>
            <h2>选择标签</h2>
            <p>用于推荐和分类。</p>
          </div>
        </div>
        <div class="tag-grid">
          <label
            v-for="tag in availableTags"
            :key="tag.id"
            class="tag-pill"
            :class="{ active: selectedTagIds.includes(tag.id) }"
          >
            <input
              v-model="selectedTagIds"
              type="checkbox"
              :value="tag.id"
              class="tag-check"
            />
            {{ tag.name }}
          </label>
        </div>
        <div v-if="loadingTags" class="feed-empty">标签加载中...</div>
        <div v-else-if="!availableTags.length" class="feed-empty">标签库为空，请先配置标签。</div>
        <p v-if="tagsError" class="form-alert error">{{ tagsError }}</p>

        <div class="profile-header">
          <div>
            <h2>媒体上传</h2>
            <p>从电脑选择图片或视频上传。</p>
          </div>
          <button class="ghost-btn" type="button" @click="triggerUpload" :disabled="uploading">
            {{ uploading ? '上传中...' : '选择文件' }}
          </button>
        </div>
        <input
          ref="fileInputRef"
          class="file-input"
          type="file"
          multiple
          accept="image/*,video/*"
          @change="handleFiles"
        />
        <p v-if="uploadError" class="form-alert error">{{ uploadError }}</p>
        <div class="media-list" v-if="mediaInputs.length">
          <div v-for="(item, index) in mediaInputs" :key="index" class="media-row">
            <div class="media-preview-card">
              <img v-if="item.mediaType === 0" :src="item.url" alt="media" />
              <a v-else :href="item.url" target="_blank" rel="noreferrer">查看视频</a>
            </div>
            <div class="media-meta">
              <span>类型：{{ item.mediaType === 0 ? '图片' : '视频' }}</span>
              <span class="muted">{{ item.url }}</span>
            </div>
            <button class="ghost-btn danger" type="button" @click="removeMedia(index)">删除</button>
          </div>
        </div>

        <div class="profile-actions">
          <button class="primary-btn" type="submit" :disabled="loading">
            {{ loading ? '提交中...' : '提交内容' }}
          </button>
        </div>

        <p v-if="error" class="form-alert error">{{ error }}</p>
        <p v-if="success" class="form-alert success">{{ success }}</p>
      </form>
    </section>
  </div>
</template>
