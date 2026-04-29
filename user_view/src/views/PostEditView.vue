<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { EditPen, UploadFilled } from '@element-plus/icons-vue'
import { apiFetch, apiUpload } from '../utils/api'
import { getPostTypeLabel, getVisibilityLabel } from '../utils/user'
import UserShell from '../components/user/UserShell.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const success = ref('')

const title = ref('')
const content = ref('')
const postType = ref(0)
const visibility = ref(0)
const location = ref('')
const college = ref('')
const isDraft = ref(false)

const mediaInputs = ref([])
const uploading = ref(false)
const uploadError = ref('')
const fileInputRef = ref(null)
const availableTags = ref([])
const selectedTagIds = ref([])
const loadingTags = ref(false)
const tagsError = ref('')

const selectedTagNames = computed(() => availableTags.value.filter((tag) => selectedTagIds.value.includes(tag.id)).map((tag) => tag.name))

const authGuard = (res) => {
  if (res.status === 401) {
    router.push('/login')
    return true
  }
  return false
}

const loadAvailableTags = async () => {
  loadingTags.value = true
  tagsError.value = ''
  try {
    const res = await apiFetch('/api/tags')
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagsError.value = data.message || '获取标签列表失败。'
      return
    }
    availableTags.value = data.data || []
  } catch {
    tagsError.value = '网络错误，无法获取标签列表。'
  } finally {
    loadingTags.value = false
  }
}

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await apiFetch(`/api/posts/${route.params.id}`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '获取详情失败。'
      return
    }
    const detail = data.data
    title.value = detail.title || ''
    content.value = detail.content || ''
    postType.value = detail.postType ?? 0
    visibility.value = detail.visibility ?? 0
    location.value = detail.location || ''
    college.value = detail.college || ''
    isDraft.value = detail.status === 3
    selectedTagIds.value = detail.tagIds || []
    mediaInputs.value = (detail.media || []).map((item, idx) => ({
      url: item.url,
      mediaType: item.mediaType ?? 0,
      sortOrder: item.sortOrder ?? idx + 1,
    }))
  } catch {
    error.value = '网络错误，无法获取详情。'
  } finally {
    loading.value = false
  }
}

const triggerUpload = () => fileInputRef.value?.click()

const removeMedia = (index) => {
  mediaInputs.value.splice(index, 1)
  mediaInputs.value.forEach((item, idx) => {
    item.sortOrder = idx + 1
  })
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
      if (authGuard(res)) return
      const data = await res.json()
      if (!res.ok || data.code !== 0) {
        uploadError.value = data.message || '上传失败。'
        return
      }
      if (data.data?.url) {
        mediaInputs.value.push({
          url: data.data.url,
          mediaType: file.type.startsWith('video/') ? 1 : 0,
          sortOrder: mediaInputs.value.length + 1,
        })
      }
    }
  } catch {
    uploadError.value = '网络错误，上传失败。'
  } finally {
    uploading.value = false
    if (event.target) event.target.value = ''
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
    const media = mediaInputs.value.filter((item) => item.url).map((item) => ({ url: item.url, mediaType: item.mediaType, sortOrder: item.sortOrder }))
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
    const res = await apiFetch(`/api/posts/${route.params.id}`, { method: 'PUT', body: JSON.stringify(payload) })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '保存失败。'
      return
    }
    success.value = '保存成功。'
    setTimeout(() => router.push(`/posts/${route.params.id}`), 600)
  } catch {
    error.value = '网络错误，请稍后再试。'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAvailableTags()
  loadDetail()
})
</script>

<template>
  <UserShell section="home">
    <section class="campus-hero">
      <span class="campus-hero__eyebrow"><el-icon><EditPen /></el-icon>编辑内容</span>
      <h1 class="campus-hero__title">更新你的校园动态</h1>
      <p class="campus-hero__subtitle">保留创作节奏的同时，把设置、标签和素材重新组织到更稳定的编辑布局里。</p>
    </section>

    <div class="campus-editor-grid">
      <div>
        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">正文编辑</h2>
              <p class="campus-panel__desc">修改标题、正文和核心表达，右侧同步调整发布设置。</p>
            </div>
          </div>
          <el-form label-position="top">
            <el-form-item label="标题">
              <el-input v-model="title" maxlength="200" placeholder="给内容一个明确的标题" show-word-limit />
            </el-form-item>
            <el-form-item label="正文">
              <el-input v-model="content" type="textarea" :rows="14" resize="none" placeholder="继续完善你的内容..." />
            </el-form-item>
          </el-form>
          <el-alert v-if="error" :title="error" type="error" show-icon />
          <el-alert v-if="success" :title="success" type="success" show-icon style="margin-top: 12px" />
        </el-card>
      </div>

      <aside>
        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">发布设置</h2>
              <p class="campus-panel__desc">编辑阶段也能快速调整可见范围和草稿状态。</p>
            </div>
          </div>
          <el-form label-position="top">
            <el-form-item label="内容类型"><el-select v-model="postType"><el-option label="图文动态" :value="0" /><el-option label="活动信息" :value="1" /><el-option label="投票互动" :value="2" /><el-option label="二手交易" :value="3" /></el-select></el-form-item>
            <el-form-item label="可见范围"><el-select v-model="visibility"><el-option label="全校可见" :value="0" /><el-option label="仅关注可见" :value="1" /><el-option label="仅自己可见" :value="2" /></el-select></el-form-item>
            <el-form-item label="位置"><el-input v-model="location" /></el-form-item>
            <el-form-item label="学院"><el-input v-model="college" /></el-form-item>
            <el-form-item label="保存为草稿"><el-switch v-model="isDraft" inline-prompt active-text="是" inactive-text="否" /></el-form-item>
          </el-form>
          <div class="campus-side-list">
            <div class="campus-side-item"><p class="campus-side-item__title">当前类型</p><p class="campus-side-item__desc">{{ getPostTypeLabel(postType) }}</p></div>
            <div class="campus-side-item"><p class="campus-side-item__title">当前可见范围</p><p class="campus-side-item__desc">{{ getVisibilityLabel(visibility) }}</p></div>
          </div>
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">标签</h2>
              <p class="campus-panel__desc">保留内容分发语义，不要丢掉原来的推荐入口。</p>
            </div>
          </div>
          <el-checkbox-group v-model="selectedTagIds" style="display: flex; flex-wrap: wrap; gap: 12px">
            <el-checkbox-button v-for="tag in availableTags" :key="tag.id" :value="tag.id">{{ tag.name }}</el-checkbox-button>
          </el-checkbox-group>
          <el-alert v-if="tagsError" :title="tagsError" type="error" show-icon style="margin-top: 14px" />
          <div v-if="selectedTagNames.length" class="campus-side-list" style="margin-top: 18px">
            <div class="campus-side-item"><p class="campus-side-item__title">已选标签</p><p class="campus-side-item__desc">{{ selectedTagNames.join(' / ') }}</p></div>
          </div>
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">素材库</h2>
              <p class="campus-panel__desc">在编辑状态继续增加或删除素材。</p>
            </div>
            <el-button type="primary" plain :loading="uploading" @click="triggerUpload">
              <el-icon><UploadFilled /></el-icon>
              选择文件
            </el-button>
          </div>
          <input ref="fileInputRef" class="file-input" type="file" multiple accept="image/*,video/*" @change="handleFiles" />
          <el-alert v-if="uploadError" :title="uploadError" type="error" show-icon style="margin-bottom: 14px" />
          <div v-if="mediaInputs.length" class="campus-upload-list">
            <div v-for="(item, index) in mediaInputs" :key="`${item.url}-${index}`" class="campus-upload-item">
              <div class="campus-upload-item__preview">
                <img v-if="item.mediaType === 0" :src="item.url" alt="media" />
                <span v-else>视频</span>
              </div>
              <div>
                <p class="campus-side-item__title">{{ item.mediaType === 0 ? '图片素材' : '视频素材' }}</p>
                <p class="campus-side-item__desc">{{ item.url }}</p>
              </div>
              <el-button type="danger" plain @click="removeMedia(index)">删除</el-button>
            </div>
          </div>
          <el-empty v-else description="还没有素材" />
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-inline-actions">
            <el-button type="primary" size="large" :loading="loading" @click="submit">保存修改</el-button>
            <el-button size="large" plain @click="router.push(`/posts/${route.params.id}`)">返回详情</el-button>
          </div>
        </el-card>
      </aside>
    </div>
  </UserShell>
</template>
