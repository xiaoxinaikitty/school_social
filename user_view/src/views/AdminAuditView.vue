<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshRight, View } from '@element-plus/icons-vue'
import AdminShell from '../components/admin/AdminShell.vue'
import { formatDateTime, requestData, shortText } from '../utils/admin'

const router = useRouter()
const posts = ref([])
const page = ref(1)
const size = ref(8)
const total = ref(0)
const statusFilter = ref(0)
const loading = ref(false)
const error = ref('')
const noteMap = ref({})

const statusOptions = [
  { label: '待审', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已驳回', value: 2 },
]

const statusLabel = (status) => {
  if (status === 1) return { label: '通过', type: 'success' }
  if (status === 2) return { label: '驳回', type: 'danger' }
  if (status === 3) return { label: '草稿', type: 'info' }
  return { label: '待审', type: 'warning' }
}

const loadPosts = async (targetPage = 1) => {
  loading.value = true
  error.value = ''
  try {
    const data = await requestData(
      router,
      `/api/admin/posts?page=${targetPage}&size=${size.value}&status=${statusFilter.value}`,
    )
    posts.value = data?.list || []
    total.value = data?.total ?? 0
    page.value = data?.page ?? targetPage
  } catch (err) {
    error.value = err?.message || '获取审核列表失败'
  } finally {
    loading.value = false
  }
}

const reviewPost = async (postId, decision) => {
  try {
    await requestData(router, `/api/admin/posts/${postId}/review`, {
      method: 'PUT',
      body: JSON.stringify({
        decision,
        note: noteMap.value[postId] || null,
      }),
    })
    ElMessage.success(decision === 1 ? '内容已通过' : '内容已驳回')
    loadPosts(page.value)
  } catch (err) {
    ElMessage.error(err?.message || '审核失败')
  }
}

const openAuditDetail = (postId) => {
  router.push(`/admin/audit/${postId}`)
}

onMounted(() => {
  loadPosts()
})
</script>

<template>
  <AdminShell title="内容审核中心" subtitle="用结构化表格和展开详情处理待审内容，减少重复跳转。">
    <template #actions>
      <div class="admin-filter-group">
        <el-radio-group v-model="statusFilter" @change="loadPosts(1)">
          <el-radio-button
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.value"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
        <el-button :icon="RefreshRight" plain @click="loadPosts(page)">刷新</el-button>
      </div>
    </template>

    <div class="admin-stack">
      <el-alert
        v-if="error"
        type="error"
        :closable="false"
        show-icon
        :title="error"
      />

      <el-card class="admin-surface-card" shadow="never">
        <div class="admin-section-heading">
          <div>
            <h3>审核队列</h3>
            <p>支持展开查看正文、写入审核意见并直接完成审核动作。</p>
          </div>
          <el-tag round effect="plain">共 {{ total }} 条记录</el-tag>
        </div>

        <el-table class="admin-compact-table" :data="posts" v-loading="loading">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="admin-expand-panel">
                <div class="admin-expand-meta">
                  <div>
                    <span>内容编号</span>
                    <strong>#{{ row.id }}</strong>
                  </div>
                  <div>
                    <span>发布者</span>
                    <strong>{{ row.userId }}</strong>
                  </div>
                  <div>
                    <span>创建时间</span>
                    <strong>{{ formatDateTime(row.createdAt) }}</strong>
                  </div>
                </div>

                <div class="admin-expand-content">{{ row.content || '暂无正文内容' }}</div>

                <div style="margin-top:18px;" class="admin-form-grid">
                  <el-input
                    v-model="noteMap[row.id]"
                    type="textarea"
                    :rows="3"
                    placeholder="填写审核意见，可选"
                  />
                  <div class="admin-side-stack">
                    <el-button type="primary" @click="reviewPost(row.id, 1)">通过内容</el-button>
                    <el-button type="danger" plain @click="reviewPost(row.id, 2)">驳回内容</el-button>
                    <el-button :icon="View" plain @click="openAuditDetail(row.id)">查看审核详情</el-button>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="id" label="内容 ID" width="96" />
          <el-table-column label="标题" min-width="220">
            <template #default="{ row }">
              <div style="font-weight:600;color:#0f172a;">{{ row.title || '未命名内容' }}</div>
              <div style="margin-top:4px;color:#64748b;">{{ shortText(row.content, 66) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="userId" label="发布者" width="96" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusLabel(row.status).type" round>{{ statusLabel(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="reviewPost(row.id, 1)">通过</el-button>
              <el-button type="danger" link @click="reviewPost(row.id, 2)">驳回</el-button>
              <el-button link @click="openAuditDetail(row.id)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="!loading && !posts.length"
          class="admin-empty"
          description="当前筛选条件下没有内容"
        />

        <div class="admin-table-footer" v-if="total > size">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="page"
            :page-size="size"
            :total="total"
            @current-change="loadPosts"
          />
        </div>
      </el-card>
    </div>
  </AdminShell>
</template>
