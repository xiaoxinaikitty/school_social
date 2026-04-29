<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { PictureFilled, UploadFilled } from '@element-plus/icons-vue'
import { apiFetch, apiUpload } from '../utils/api'
import { getPostTypeLabel, getVisibilityLabel } from '../utils/user'
import UserShell from '../components/user/UserShell.vue'

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

const authGuard = (res) => {
  if (res.status === 401) {
    router.push('/login')
    return true
  }
  return false
}

const selectedTagNames = computed(() => availableTags.value.filter((tag) => selectedTagIds.value.includes(tag.id)).map((tag) => tag.name))

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
    const res = await apiFetch('/api/posts', { method: 'POST', body: JSON.stringify(payload) })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '发布失败，请检查输入。'
      return
    }
    success.value = isDraft.value ? '草稿已保存。' : '发布成功。'
    setTimeout(() => router.push('/home'), 700)
  } catch {
    error.value = '网络错误，请稍后再试。'
  } finally {
    loading.value = false
  }
}

onMounted(loadAvailableTags)
</script>

<template>
  <UserShell section="home">
    <section class="campus-hero">
      <span class="campus-hero__eyebrow"><el-icon><PictureFilled /></el-icon>内容发布</span>
      <h1 class="campus-hero__title">发布新的校园动态</h1>
      <p class="campus-hero__subtitle">主编辑区专注内容创作，右侧负责可见范围、标签和素材管理，发帖过程更接近主流社区产品。</p>
    </section>

    <div class="campus-editor-grid">
      <div>
        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">内容编辑</h2>
              <p class="campus-panel__desc">先把正文写顺，再补充标签、位置和媒体素材。</p>
            </div>
          </div>
          <el-form label-position="top">
            <el-form-item label="标题">
              <el-input v-model="title" maxlength="200" placeholder="给内容一个明确的标题" show-word-limit />
            </el-form-item>
            <el-form-item label="正文">
              <el-input v-model="content" type="textarea" :rows="14" resize="none" placeholder="分享校园动态、活动信息、学习经验或交易内容..." />
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
              <p class="campus-panel__desc">统一管理类型、可见范围和草稿状态。</p>
            </div>
          </div>
          <el-form label-position="top">
            <el-form-item label="内容类型">
              <el-select v-model="postType">
                <el-option label="图文动态" :value="0" />
                <el-option label="活动信息" :value="1" />
                <el-option label="投票互动" :value="2" />
                <el-option label="二手交易" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item label="可见范围">
              <el-select v-model="visibility">
                <el-option label="全校可见" :value="0" />
                <el-option label="仅关注可见" :value="1" />
                <el-option label="仅自己可见" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="位置">
              <el-input v-model="location" placeholder="图书馆 / 教学楼 / 食堂" />
            </el-form-item>
            <el-form-item label="学院">
              <el-input v-model="college" placeholder="学院名称" />
            </el-form-item>
            <el-form-item label="保存为草稿">
              <el-switch v-model="isDraft" inline-prompt active-text="是" inactive-text="否" />
            </el-form-item>
          </el-form>
          <div class="campus-side-list">
            <div class="campus-side-item">
              <p class="campus-side-item__title">当前类型</p>
              <p class="campus-side-item__desc">{{ getPostTypeLabel(postType) }}</p>
            </div>
            <div class="campus-side-item">
              <p class="campus-side-item__title">当前可见范围</p>
              <p class="campus-side-item__desc">{{ getVisibilityLabel(visibility) }}</p>
            </div>
          </div>
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">内容标签</h2>
              <p class="campus-panel__desc">标签决定内容被推荐到什么人面前。</p>
            </div>
          </div>
          <el-checkbox-group v-model="selectedTagIds" style="display: flex; flex-wrap: wrap; gap: 12px">
            <el-checkbox-button v-for="tag in availableTags" :key="tag.id" :value="tag.id">{{ tag.name }}</el-checkbox-button>
          </el-checkbox-group>
          <el-empty v-if="!loadingTags && !availableTags.length" description="标签库为空，请先配置标签。" />
          <el-alert v-if="tagsError" :title="tagsError" type="error" show-icon style="margin-top: 14px" />
          <div v-if="selectedTagNames.length" class="campus-side-list" style="margin-top: 18px">
            <div class="campus-side-item">
              <p class="campus-side-item__title">已选标签</p>
              <p class="campus-side-item__desc">{{ selectedTagNames.join(' / ') }}</p>
            </div>
          </div>
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">媒体素材</h2>
              <p class="campus-panel__desc">支持上传图片和视频。</p>
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
          <el-empty v-else description="还没有上传素材" />
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-inline-actions">
            <el-button type="primary" size="large" :loading="loading" @click="submit">提交内容</el-button>
            <el-button size="large" plain @click="router.push('/home')">返回首页</el-button>
          </div>
        </el-card>
      </aside>
    </div>
  </UserShell>
</template>
