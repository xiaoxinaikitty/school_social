<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const loading = ref(false)
const error = ref('')
const related = ref([])
const relatedLoading = ref(false)
const relatedError = ref('')

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
    await loadRelated()
  } catch (err) {
    error.value = '网络错误，无法获取详情。'
  } finally {
    loading.value = false
  }
}

const loadRelated = async () => {
  relatedLoading.value = true
  relatedError.value = ''
  try {
    const res = await apiFetch(`/api/posts/${route.params.id}/related`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      relatedError.value = data.message || '获取相关推荐失败。'
      return
    }
    related.value = data.data || []
  } catch (err) {
    relatedError.value = '网络错误，无法获取相关推荐。'
  } finally {
    relatedLoading.value = false
  }
}

watch(() => route.params.id, () => {
  loadDetail()
})

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

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>相关推荐</h2>
          <p>基于话题标签推荐的相似内容。</p>
        </div>
        <button class="ghost-btn" type="button" @click="loadRelated">刷新</button>
      </div>
      <div v-if="relatedLoading" class="feed-empty">加载中...</div>
      <div v-else-if="relatedError" class="form-alert error">{{ relatedError }}</div>
      <div v-else-if="related.length" class="feed-grid">
        <article v-for="item in related" :key="item.id" class="feed-item">
          <h4>{{ item.title || '未命名内容' }}</h4>
          <p>{{ item.content || '暂无内容' }}</p>
          <div class="feed-meta">
            <span>点赞 {{ item.likeCount ?? 0 }}</span>
            <span>评论 {{ item.commentCount ?? 0 }}</span>
            <span>收藏 {{ item.favoriteCount ?? 0 }}</span>
          </div>
          <div class="feed-actions">
            <RouterLink class="ghost-btn" :to="`/posts/${item.id}`">查看详情</RouterLink>
          </div>
        </article>
      </div>
      <div v-else class="feed-empty">暂无相关推荐。</div>
    </section>
  </div>
</template>
