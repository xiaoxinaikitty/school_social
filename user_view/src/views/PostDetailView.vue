<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const loading = ref(false)
const error = ref('')

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await apiFetch(`/api/posts/${route.params.id}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '获取详情失败。'
      return
    }
    detail.value = data.data
  } catch (err) {
    error.value = '网络错误，无法获取详情。'
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="post-page">
    <header class="profile-hero">
      <div>
        <span class="hero-tag">内容详情</span>
        <h1>{{ detail?.title || '未命名内容' }}</h1>
        <p>发布于 {{ detail?.createdAt || '-' }}</p>
      </div>
      <div class="profile-actions">
        <button class="ghost-btn" type="button" @click="router.push('/profile')">返回个人中心</button>
        <button class="ghost-btn" type="button" @click="router.push(`/posts/${route.params.id}/edit`)">编辑</button>
      </div>
    </header>

    <section class="profile-section">
      <div v-if="loading" class="feed-empty">加载中...</div>
      <div v-else-if="error" class="form-alert error">{{ error }}</div>
      <div v-else-if="detail" class="post-detail">
        <p class="post-content">{{ detail.content }}</p>
        <div class="post-meta">
          <span>可见范围：{{ detail.visibility === 0 ? '全校' : detail.visibility === 1 ? '关注' : '仅自己' }}</span>
          <span>类型：{{ detail.postType ?? 0 }}</span>
          <span class="status-pill" :class="detail.status === 0 ? 'pending' : detail.status === 1 ? 'approved' : detail.status === 2 ? 'rejected' : 'draft'">
            {{ detail.status === 0 ? '待审' : detail.status === 1 ? '通过' : detail.status === 2 ? '驳回' : '草稿' }}
          </span>
          <span v-if="detail.location">位置：{{ detail.location }}</span>
          <span v-if="detail.college">学院：{{ detail.college }}</span>
        </div>

        <div v-if="detail.media && detail.media.length" class="media-preview">
          <div v-for="media in detail.media" :key="media.id" class="media-item">
            <img v-if="media.mediaType === 0" :src="media.url" alt="media" />
            <a v-else :href="media.url" target="_blank" rel="noreferrer">查看视频</a>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
