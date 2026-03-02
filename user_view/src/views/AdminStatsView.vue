<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const router = useRouter()

const days = ref(30)
const overview = ref(null)
const userStats = ref([])
const contentStats = ref([])
const loading = ref(false)
const error = ref('')

const rangeLabel = computed(() => {
  if (!overview.value) return ''
  return `${overview.value.rangeStart || '-'} ~ ${overview.value.rangeEnd || '-'}`
})

const formatPercent = (value) => {
  if (value === null || value === undefined) return '0%'
  const pct = Number(value) * 100
  if (Number.isNaN(pct)) return '0%'
  return `${pct.toFixed(1)}%`
}

const request = async (path) => {
  const res = await apiFetch(path)
  if (res.status === 401) {
    router.push('/login')
    throw new Error('未登录或登录已过期')
  }
  const data = await res.json()
  if (!res.ok || data.code !== 0) {
    throw new Error(data.message || '请求失败')
  }
  return data.data
}

const loadAll = async () => {
  loading.value = true
  error.value = ''
  try {
    const [overviewData, userData, contentData] = await Promise.all([
      request(`/api/admin/stats/overview?days=${days.value}`),
      request(`/api/admin/stats/users?days=${days.value}`),
      request(`/api/admin/stats/content?days=${days.value}`),
    ])
    overview.value = overviewData
    userStats.value = userData || []
    contentStats.value = contentData || []
  } catch (err) {
    error.value = err?.message || '网络错误，无法获取统计数据。'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>

<template>
  <div class="profile-page">
    <header class="profile-hero">
      <div>
        <span class="hero-tag">数据统计</span>
        <h1>平台数据看板</h1>
        <p>聚合展示用户增长、活跃、留存与内容互动指标。</p>
        <div class="hero-meta">
          <span class="badge">统计区间：{{ rangeLabel || '-' }}</span>
        </div>
      </div>
      <div class="profile-actions">
        <label class="field">
          <span>统计天数</span>
          <select v-model.number="days" @change="loadAll">
            <option :value="7">近 7 天</option>
            <option :value="14">近 14 天</option>
            <option :value="30">近 30 天</option>
            <option :value="60">近 60 天</option>
            <option :value="90">近 90 天</option>
            <option :value="180">近 180 天</option>
          </select>
        </label>
        <button class="ghost-btn" type="button" @click="loadAll" :disabled="loading">
          {{ loading ? '加载中...' : '刷新' }}
        </button>
        <button class="ghost-btn" type="button" @click="router.push('/admin')">返回后台</button>
      </div>
    </header>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>概览</h2>
          <p>核心规模指标与留存表现。</p>
        </div>
      </div>
      <div v-if="error" class="form-alert error">{{ error }}</div>
      <div v-else class="feed-grid">
        <div class="stat-card">
          <p class="stat-label">用户总数</p>
          <p class="stat-value">{{ overview?.totalUsers ?? 0 }}</p>
          <p class="stat-note">新增 {{ overview?.newUsers ?? 0 }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">活跃用户</p>
          <p class="stat-value">{{ overview?.activeUsers ?? 0 }}</p>
          <p class="stat-note">统计期内活跃</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">内容总量</p>
          <p class="stat-value">{{ overview?.totalPosts ?? 0 }}</p>
          <p class="stat-note">新增 {{ overview?.postCount ?? 0 }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">互动总量</p>
          <p class="stat-value">{{ overview?.totalInteractions ?? 0 }}</p>
          <p class="stat-note">新增 {{ overview?.interactionCount ?? 0 }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">次日留存</p>
          <p class="stat-value">{{ formatPercent(overview?.retention1d) }}</p>
          <p class="stat-note">相对新增用户</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">7 日留存</p>
          <p class="stat-value">{{ formatPercent(overview?.retention7d) }}</p>
          <p class="stat-note">相对新增用户</p>
        </div>
      </div>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>用户增长/活跃/留存</h2>
          <p>每日新增、活跃与留存比例。</p>
        </div>
      </div>
      <div v-if="loading" class="feed-empty">加载中...</div>
      <div v-else-if="!userStats.length" class="feed-empty">暂无用户统计数据。</div>
      <div v-else class="feed-grid">
        <div v-for="item in userStats" :key="item.date" class="feed-item">
          <div class="feed-item-top">
            <span class="feed-label">{{ item.date }}</span>
          </div>
          <div class="feed-meta">
            <span>新增 {{ item.newUsers ?? 0 }}</span>
            <span>活跃 {{ item.activeUsers ?? 0 }}</span>
          </div>
          <div class="feed-meta">
            <span>次日留存 {{ formatPercent(item.retention1d) }}</span>
            <span>7日留存 {{ formatPercent(item.retention7d) }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>内容发布量与互动量</h2>
          <p>每日内容发布与互动（点赞/评论/收藏/分享）。</p>
        </div>
      </div>
      <div v-if="loading" class="feed-empty">加载中...</div>
      <div v-else-if="!contentStats.length" class="feed-empty">暂无内容统计数据。</div>
      <div v-else class="feed-grid">
        <div v-for="item in contentStats" :key="item.date" class="feed-item">
          <div class="feed-item-top">
            <span class="feed-label">{{ item.date }}</span>
          </div>
          <div class="feed-meta">
            <span>发布 {{ item.postCount ?? 0 }}</span>
            <span>互动 {{ item.interactionCount ?? 0 }}</span>
          </div>
          <div class="feed-meta">
            <span>赞 {{ item.likeCount ?? 0 }}</span>
            <span>评 {{ item.commentCount ?? 0 }}</span>
            <span>藏 {{ item.favoriteCount ?? 0 }}</span>
            <span>分享 {{ item.shareCount ?? 0 }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
