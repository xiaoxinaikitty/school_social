<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, Close } from '@element-plus/icons-vue'
import AdminShell from '../components/admin/AdminShell.vue'
import { formatDateTime, requestData } from '../utils/admin'

const route = useRoute()
const router = useRouter()

const detail = ref(null)
const tags = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const reviewNote = ref('')

const statusMeta = computed(() => {
  if (detail.value?.status === 1) return { label: '已通过', type: 'success' }
  if (detail.value?.status === 2) return { label: '已驳回', type: 'danger' }
  if (detail.value?.status === 3) return { label: '草稿', type: 'info' }
  return { label: '待审核', type: 'warning' }
})

const postTypeLabel = computed(() => {
  if (detail.value?.postType === 1) return '活动'
  if (detail.value?.postType === 2) return '求助'
  if (detail.value?.postType === 3) return '二手'
  if (detail.value?.postType === 4) return '投票'
  return '动态'
})

const visibilityLabel = computed(() => {
  if (detail.value?.visibility === 1) return '仅本校可见'
  if (detail.value?.visibility === 2) return '仅自己可见'
  return '公开可见'
})

const tagNames = computed(() => {
  const tagMap = new Map(tags.value.map((item) => [item.id, item.name]))
  return (detail.value?.tagIds || []).map((tagId) => tagMap.get(tagId) || `标签 ${tagId}`)
})

const imageList = computed(() =>
  (detail.value?.media || [])
    .filter((item) => item.mediaType === 0)
    .map((item) => item.url),
)

const loadTags = async () => {
  tags.value = (await requestData(router, '/api/tags')) || []
}

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  try {
    detail.value = await requestData(router, `/api/admin/posts/${route.params.id}`)
  } catch (err) {
    error.value = err?.message || '获取审核详情失败'
  } finally {
    loading.value = false
  }
}

const submitReview = async (decision) => {
  if (!detail.value?.id) return
  saving.value = true
  try {
    const data = await requestData(router, `/api/admin/posts/${detail.value.id}/review`, {
      method: 'PUT',
      body: JSON.stringify({
        decision,
        note: reviewNote.value.trim() || null,
      }),
    })
    detail.value = {
      ...detail.value,
      status: data?.status ?? decision,
      publishedAt: data?.publishedAt ?? detail.value.publishedAt,
      updatedAt: data?.updatedAt ?? detail.value.updatedAt,
    }
    ElMessage.success(decision === 1 ? '内容已通过' : '内容已驳回')
  } catch (err) {
    ElMessage.error(err?.message || '审核失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadTags(), loadDetail()])
})
</script>

<template>
  <AdminShell title="审核内容详情" subtitle="在后台独立查看内容正文、媒体和元信息，避免跳入用户端详情页。">
    <template #actions>
      <div class="admin-toolbar-card">
        <el-button plain :icon="ArrowLeft" @click="router.push('/admin/audit')">返回审核列表</el-button>
      </div>
    </template>

    <div class="admin-stack">
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />

      <el-card v-else-if="loading" class="admin-surface-card" shadow="never">
        <el-skeleton animated :rows="12" />
      </el-card>

      <template v-else-if="detail">
        <div class="admin-split">
          <el-card class="admin-surface-card" shadow="never">
            <div class="admin-section-heading">
              <div>
                <h3>{{ detail.title || '未命名内容' }}</h3>
                <p>内容 ID：#{{ detail.id }}，发布者：用户 {{ detail.userId }}</p>
              </div>
              <el-tag :type="statusMeta.type" round>{{ statusMeta.label }}</el-tag>
            </div>

            <div class="admin-toolbar-card" style="margin-bottom:18px;">
              <el-tag round effect="light">{{ postTypeLabel }}</el-tag>
              <el-tag round type="info" effect="light">{{ visibilityLabel }}</el-tag>
              <el-tag v-if="detail.college" round type="success" effect="light">{{ detail.college }}</el-tag>
              <el-tag v-if="detail.location" round type="warning" effect="light">{{ detail.location }}</el-tag>
              <el-tag v-for="tag in tagNames" :key="tag" round effect="plain">{{ tag }}</el-tag>
            </div>

            <div class="admin-expand-content" style="min-height:220px;">
              {{ detail.content || '暂无正文内容' }}
            </div>

            <div v-if="detail.media?.length" class="admin-audit-media-grid">
              <div v-for="item in detail.media" :key="item.id || item.url" class="admin-audit-media-card">
                <el-image
                  v-if="item.mediaType === 0"
                  :src="item.url"
                  :preview-src-list="imageList"
                  fit="cover"
                  style="width:100%;height:240px;"
                />
                <div v-else class="admin-audit-media-fallback">
                  <div style="font-weight:600;color:#0f172a;">视频资源</div>
                  <div style="margin-top:8px;color:#64748b;word-break:break-all;">{{ item.url }}</div>
                  <el-button type="primary" plain style="margin-top:16px;">
                    <a :href="item.url" target="_blank" rel="noreferrer">打开资源</a>
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>

          <div class="admin-side-stack">
            <el-card class="admin-glass-card" shadow="never">
              <div class="admin-section-heading">
                <div>
                  <h3>内容元信息</h3>
                  <p>审核前先确认发布时间、互动量和状态信息。</p>
                </div>
              </div>

              <div class="admin-expand-meta admin-expand-meta--single">
                <div>
                  <span>创建时间</span>
                  <strong>{{ formatDateTime(detail.createdAt) }}</strong>
                </div>
                <div>
                  <span>更新时间</span>
                  <strong>{{ formatDateTime(detail.updatedAt) }}</strong>
                </div>
                <div>
                  <span>发布时间</span>
                  <strong>{{ formatDateTime(detail.publishedAt) }}</strong>
                </div>
                <div>
                  <span>点赞数</span>
                  <strong>{{ detail.likeCount ?? 0 }}</strong>
                </div>
                <div>
                  <span>评论数</span>
                  <strong>{{ detail.commentCount ?? 0 }}</strong>
                </div>
                <div>
                  <span>收藏数</span>
                  <strong>{{ detail.favoriteCount ?? 0 }}</strong>
                </div>
                <div>
                  <span>浏览量</span>
                  <strong>{{ detail.viewCount ?? 0 }}</strong>
                </div>
              </div>
            </el-card>

            <el-card class="admin-glass-card" shadow="never">
              <div class="admin-section-heading">
                <div>
                  <h3>审核操作</h3>
                  <p>可填写审核意见，并直接执行通过或驳回。</p>
                </div>
              </div>

              <el-input
                v-model="reviewNote"
                type="textarea"
                :rows="5"
                resize="none"
                placeholder="填写审核意见，可选"
              />

              <div class="admin-side-stack" style="margin-top:18px;">
                <el-button type="primary" :icon="Check" :loading="saving" @click="submitReview(1)">
                  通过内容
                </el-button>
                <el-button type="danger" plain :icon="Close" :loading="saving" @click="submitReview(2)">
                  驳回内容
                </el-button>
              </div>
            </el-card>
          </div>
        </div>
      </template>
    </div>
  </AdminShell>
</template>
