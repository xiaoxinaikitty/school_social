<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const router = useRouter()
const posts = ref([])
const page = ref(1)
const size = ref(8)
const total = ref(0)
const statusFilter = ref(0)
const loading = ref(false)
const error = ref('')
const noteMap = ref({})
const actionError = ref('')
const actionSuccess = ref('')

const totalPages = computed(() => {
  const pages = Math.ceil(total.value / size.value)
  return pages > 0 ? pages : 1
})

const statusLabel = (status) => {
  if (status === 1) return { label: '通过', tone: 'approved' }
  if (status === 2) return { label: '驳回', tone: 'rejected' }
  if (status === 3) return { label: '草稿', tone: 'draft' }
  return { label: '待审', tone: 'pending' }
}

const loadPosts = async (targetPage = 1) => {
  loading.value = true
  error.value = ''
  actionSuccess.value = ''
  actionError.value = ''
  try {
    const res = await apiFetch(`/api/admin/posts?page=${targetPage}&size=${size.value}&status=${statusFilter.value}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '获取审核列表失败。'
      return
    }
    posts.value = data.data?.list || []
    total.value = data.data?.total ?? 0
    page.value = data.data?.page ?? targetPage
  } catch (err) {
    error.value = '网络错误，无法获取审核列表。'
  } finally {
    loading.value = false
  }
}

const setFilter = (status) => {
  statusFilter.value = status
  loadPosts(1)
}

const reviewPost = async (postId, decision) => {
  actionError.value = ''
  actionSuccess.value = ''
  try {
    const res = await apiFetch(`/api/admin/posts/${postId}/review`, {
      method: 'PUT',
      body: JSON.stringify({
        decision,
        note: noteMap.value[postId] || null,
      }),
    })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      actionError.value = data.message || '审核失败。'
      return
    }
    actionSuccess.value = decision === 1 ? '已通过该内容。' : '已驳回该内容。'
    loadPosts(page.value)
  } catch (err) {
    actionError.value = '网络错误，审核失败。'
  }
}

onMounted(() => {
  loadPosts()
})
</script>

<template>
  <div class="profile-page">
    <header class="profile-hero">
      <div>
        <span class="hero-tag">内容审核</span>
        <h1>内容审核与记录</h1>
        <p>集中处理待审内容，保留审核意见。</p>
      </div>
      <div class="profile-actions">
        <button class="ghost-btn" type="button" @click="loadPosts(page)">刷新列表</button>
        <button class="ghost-btn" type="button" @click="router.push('/admin')">返回后台</button>
      </div>
    </header>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>审核列表</h2>
          <p>按状态查看内容审核记录。</p>
        </div>
      </div>

      <div class="tabs">
        <button class="tab-btn" :class="{ active: statusFilter === 0 }" @click="setFilter(0)">待审</button>
        <button class="tab-btn" :class="{ active: statusFilter === 1 }" @click="setFilter(1)">通过</button>
        <button class="tab-btn" :class="{ active: statusFilter === 2 }" @click="setFilter(2)">驳回</button>
      </div>

      <div v-if="loading" class="feed-empty">加载中...</div>
      <div v-else-if="error" class="form-alert error">{{ error }}</div>
      <div v-else-if="posts.length" class="feed-grid">
        <div v-for="post in posts" :key="post.id" class="feed-item">
          <div class="feed-item-top">
            <span class="feed-label">内容 #{{ post.id }}</span>
            <span class="status-pill" :class="statusLabel(post.status).tone">
              {{ statusLabel(post.status).label }}
            </span>
          </div>
          <h4>{{ post.title || '未命名内容' }}</h4>
          <p>{{ post.content?.slice(0, 80) || '暂无内容' }}</p>
          <div class="feed-meta">
            <span>发布者 {{ post.userId }}</span>
            <span>{{ post.createdAt || '-' }}</span>
          </div>
          <label class="field">
            <span>审核备注</span>
            <input v-model="noteMap[post.id]" type="text" placeholder="可填写审核意见" />
          </label>
          <div class="feed-actions">
            <button class="ghost-btn" type="button" @click="reviewPost(post.id, 1)">通过</button>
            <button class="ghost-btn danger" type="button" @click="reviewPost(post.id, 2)">驳回</button>
            <button class="ghost-btn" type="button" @click="router.push(`/posts/${post.id}`)">查看详情</button>
          </div>
        </div>
      </div>
      <div v-else class="feed-empty">暂无内容。</div>

      <div class="pager" v-if="total > size">
        <button class="ghost-btn" type="button" :disabled="page <= 1" @click="loadPosts(page - 1)">
          上一页
        </button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button
          class="ghost-btn"
          type="button"
          :disabled="page >= totalPages"
          @click="loadPosts(page + 1)"
        >
          下一页
        </button>
      </div>

      <p v-if="actionError" class="form-alert error">{{ actionError }}</p>
      <p v-if="actionSuccess" class="form-alert success">{{ actionSuccess }}</p>
    </section>
  </div>
</template>
