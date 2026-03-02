<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { apiFetch } from '../utils/api'

const router = useRouter()
const adminUser = ref(null)

const pendingCount = ref(0)
const pendingLoading = ref(false)
const reportPendingCount = ref(0)
const reportPendingLoading = ref(false)
const pendingPreview = ref([])
const pendingPreviewLoading = ref(false)

const overview = ref(null)
const userStats = ref([])
const contentStats = ref([])
const statsLoading = ref(false)
const statsError = ref('')
const statsDays = ref(14)

const ringStyle = computed(() => {
  const value = overview.value?.retention1d ?? 0
  const pct = Math.max(0, Math.min(1, Number(value) || 0))
  return { '--ring-angle': `${pct * 360}deg` }
})

const activeSeries = computed(() => userStats.value.slice(-7))
const activePoints = computed(() => {
  const series = activeSeries.value
  if (!series.length) return ''
  const width = 220
  const height = 70
  const max = Math.max(...series.map((item) => item.activeUsers ?? 0), 1)
  return series
    .map((item, idx) => {
      const x = (idx / Math.max(series.length - 1, 1)) * width
      const y = height - ((item.activeUsers ?? 0) / max) * (height - 12) - 6
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
})

const contentSeries = computed(() => contentStats.value.slice(-7))
const contentMax = computed(() => {
  const values = contentSeries.value.map((item) => item.postCount ?? 0)
  return Math.max(...values, 1)
})

const formatPercent = (value) => {
  if (value === null || value === undefined) return '0%'
  const pct = Number(value) * 100
  if (Number.isNaN(pct)) return '0%'
  return `${pct.toFixed(1)}%`
}

const requestJson = async (url) => {
  const res = await apiFetch(url)
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

const loadPendingCount = async () => {
  pendingLoading.value = true
  try {
    const data = await requestJson('/api/admin/posts?page=1&size=1&status=0')
    pendingCount.value = data?.total ?? 0
  } catch (err) {
    pendingCount.value = 0
  } finally {
    pendingLoading.value = false
  }
}

const loadReportPendingCount = async () => {
  reportPendingLoading.value = true
  try {
    const data = await requestJson('/api/reports/admin?page=1&size=1&status=0')
    reportPendingCount.value = data?.total ?? 0
  } catch (err) {
    reportPendingCount.value = 0
  } finally {
    reportPendingLoading.value = false
  }
}

const loadPendingPreview = async () => {
  pendingPreviewLoading.value = true
  try {
    const data = await requestJson('/api/admin/posts?page=1&size=3&status=0')
    pendingPreview.value = data?.list || []
  } catch (err) {
    pendingPreview.value = []
  } finally {
    pendingPreviewLoading.value = false
  }
}

const loadStats = async () => {
  statsLoading.value = true
  statsError.value = ''
  try {
    const [overviewData, userData, contentData] = await Promise.all([
      requestJson(`/api/admin/stats/overview?days=${statsDays.value}`),
      requestJson(`/api/admin/stats/users?days=${statsDays.value}`),
      requestJson(`/api/admin/stats/content?days=${statsDays.value}`),
    ])
    overview.value = overviewData
    userStats.value = userData || []
    contentStats.value = contentData || []
  } catch (err) {
    statsError.value = err?.message || '网络错误，无法获取统计数据。'
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  try {
    const savedUser = localStorage.getItem('auth_user')
    if (savedUser) {
      adminUser.value = JSON.parse(savedUser)
    }
  } catch (err) {
    adminUser.value = null
  }
  loadPendingCount()
  loadReportPendingCount()
  loadPendingPreview()
  loadStats()
})
</script>

<template>
  <div class="home-page">
    <header class="home-hero">
      <div class="hero-left">
        <span class="hero-tag">管理员控制台</span>
        <h1>欢迎，{{ adminUser?.username || '管理员' }}。</h1>
        <p>在这里处理内容审核、举报、运营与数据分析。</p>
        <div class="hero-actions">
          <button class="primary-btn" type="button" @click="router.push('/admin/audit')">进入审核</button>
          <button class="ghost-btn" type="button" @click="router.push('/admin/reports')">举报处理</button>
          <button class="ghost-btn" type="button" @click="router.push('/home')">返回用户主页</button>
        </div>
      </div>
      <div class="hero-right">
        <div class="stat-card">
          <p class="stat-label">用户总数</p>
          <p class="stat-value">{{ overview?.totalUsers ?? 0 }}</p>
          <p class="stat-note">近 {{ statsDays }} 天新增 {{ overview?.newUsers ?? 0 }}</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">活跃用户</p>
          <p class="stat-value">{{ overview?.activeUsers ?? 0 }}</p>
          <p class="stat-note">统计期内</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">互动总量</p>
          <p class="stat-value">{{ overview?.totalInteractions ?? 0 }}</p>
          <p class="stat-note">新增 {{ overview?.interactionCount ?? 0 }}</p>
        </div>
      </div>
    </header>

    <section class="module-grid">
      <RouterLink class="module-card link-card" to="/admin/audit">
        <h3>内容审核</h3>
        <p>待审内容清单与审核记录。</p>
        <span class="module-count">待审 {{ pendingLoading ? '...' : pendingCount }}</span>
        <span class="module-tag">审核</span>
        <div class="mini-list">
          <div v-if="pendingPreviewLoading" class="mini-empty">加载中...</div>
          <div v-else-if="!pendingPreview.length" class="mini-empty">暂无待审内容</div>
          <div v-else class="mini-items">
            <div v-for="item in pendingPreview" :key="item.id" class="mini-item">
              <span>#{{ item.id }}</span>
              <span>{{ item.title || '未命名内容' }}</span>
            </div>
          </div>
        </div>
      </RouterLink>
      <RouterLink class="module-card link-card" to="/admin/reports">
        <h3>举报处理</h3>
        <p>用户举报队列与处理结论管理。</p>
        <span class="module-count">
          待处理 {{ reportPendingLoading ? '...' : reportPendingCount }}
        </span>
        <span class="module-tag">举报</span>
      </RouterLink>
      <RouterLink class="module-card link-card" to="/admin/ops">
        <h3>运营配置</h3>
        <p>话题标签、公告与推荐权重。</p>
        <span class="module-tag">运营</span>
      </RouterLink>
      <RouterLink class="module-card link-card" to="/admin/stats">
        <h3>数据统计</h3>
        <p>内容与互动数据可视化。</p>
        <span class="module-tag">看板</span>
      </RouterLink>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>数据分析</h2>
          <p>快速掌握平台核心趋势与留存表现。</p>
        </div>
        <div class="profile-actions">
          <label class="field">
            <span>统计天数</span>
            <select v-model.number="statsDays" @change="loadStats">
              <option :value="7">近 7 天</option>
              <option :value="14">近 14 天</option>
              <option :value="30">近 30 天</option>
            </select>
          </label>
          <button class="ghost-btn" type="button" @click="loadStats" :disabled="statsLoading">
            {{ statsLoading ? '加载中...' : '刷新' }}
          </button>
        </div>
      </div>

      <div v-if="statsError" class="form-alert error">{{ statsError }}</div>

      <div v-else class="analytics-grid">
        <div class="analysis-card">
          <div class="analysis-head">
            <h4>次日留存</h4>
            <p>与新增用户对比</p>
          </div>
          <div class="ring-chart" :style="ringStyle">
            <span class="ring-value">{{ formatPercent(overview?.retention1d) }}</span>
          </div>
          <div class="analysis-foot">
            <span>7 日留存 {{ formatPercent(overview?.retention7d) }}</span>
          </div>
        </div>

        <div class="analysis-card">
          <div class="analysis-head">
            <h4>活跃走势</h4>
            <p>近 7 日活跃用户</p>
          </div>
          <svg class="sparkline" viewBox="0 0 220 70" preserveAspectRatio="none">
            <polyline class="sparkline-line" :points="activePoints" />
          </svg>
          <div class="analysis-foot">
            <span>今日活跃 {{ overview?.activeUsers ?? 0 }}</span>
          </div>
        </div>

        <div class="analysis-card">
          <div class="analysis-head">
            <h4>内容发布</h4>
            <p>近 7 日发布量</p>
          </div>
          <div class="bar-chart">
            <div
              v-for="item in contentSeries"
              :key="item.date"
              class="bar"
              :style="{ height: `${Math.round(((item.postCount ?? 0) / contentMax) * 100)}%` }"
              :title="`${item.date} 发布 ${item.postCount ?? 0}`"
            ></div>
          </div>
          <div class="analysis-foot">
            <span>今日发布 {{ contentSeries.at(-1)?.postCount ?? 0 }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
