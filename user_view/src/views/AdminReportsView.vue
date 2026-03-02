<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const router = useRouter()
const reportList = ref([])
const reportPage = ref(1)
const reportSize = ref(10)
const reportTotal = ref(0)
const reportStatus = ref(0)
const reportLoading = ref(false)
const reportError = ref('')
const reportResultMap = ref({})
const reportActionError = ref('')
const reportActionSuccess = ref('')

const reportPages = computed(() => {
  const pages = Math.ceil(reportTotal.value / reportSize.value)
  return pages > 0 ? pages : 1
})

const loadReports = async (targetPage = 1) => {
  reportLoading.value = true
  reportError.value = ''
  reportActionError.value = ''
  reportActionSuccess.value = ''
  try {
    const res = await apiFetch(`/api/reports/admin?page=${targetPage}&size=${reportSize.value}&status=${reportStatus.value}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      reportError.value = data.message || '获取举报列表失败。'
      return
    }
    reportList.value = data.data?.list || []
    reportTotal.value = data.data?.total ?? 0
    reportPage.value = data.data?.page ?? targetPage
  } catch (err) {
    reportError.value = '网络错误，无法获取举报列表。'
  } finally {
    reportLoading.value = false
  }
}

const setReportFilter = (status) => {
  reportStatus.value = status
  loadReports(1)
}

const handleReport = async (reportId) => {
  reportActionError.value = ''
  reportActionSuccess.value = ''
  try {
    const res = await apiFetch(`/api/reports/admin/${reportId}/handle`, {
      method: 'PUT',
      body: JSON.stringify({
        decision: 1,
        result: reportResultMap.value[reportId] || '已处理',
      }),
    })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      reportActionError.value = data.message || '处理失败。'
      return
    }
    reportActionSuccess.value = '已处理举报。'
    loadReports(reportPage.value)
  } catch (err) {
    reportActionError.value = '网络错误，处理失败。'
  }
}

onMounted(() => {
  loadReports()
})
</script>

<template>
  <div class="profile-page">
    <header class="profile-hero">
      <div>
        <span class="hero-tag">举报处理</span>
        <h1>举报队列管理</h1>
        <p>集中处理全站举报与结论。</p>
      </div>
      <div class="profile-actions">
        <button class="ghost-btn" type="button" @click="router.push('/admin')">返回后台</button>
        <button class="ghost-btn" type="button" @click="loadReports(reportPage)">刷新举报</button>
      </div>
    </header>

    <section class="profile-section">
      <div class="tabs">
        <button class="tab-btn" :class="{ active: reportStatus === 0 }" @click="setReportFilter(0)">待处理</button>
        <button class="tab-btn" :class="{ active: reportStatus === 1 }" @click="setReportFilter(1)">已处理</button>
      </div>

      <div v-if="reportLoading" class="feed-empty">加载中...</div>
      <div v-else-if="reportError" class="form-alert error">{{ reportError }}</div>
      <div v-else-if="reportList.length" class="report-list">
        <div v-for="report in reportList" :key="report.id" class="report-card">
          <div class="feed-meta">
            <span>举报ID {{ report.id }}</span>
            <span>对象 {{ report.targetType === 0 ? '内容' : report.targetType === 1 ? '评论' : '用户' }} #{{ report.targetId }}</span>
            <span>状态：{{ report.status === 1 ? '已处理' : '待处理' }}</span>
          </div>
          <h4>{{ report.reason }}</h4>
          <p>{{ report.detail || '未填写补充说明' }}</p>
          <div v-if="report.status === 1 && report.result" class="muted">处理结果：{{ report.result }}</div>
          <div v-else class="report-handle">
            <label class="field">
              <span>处理结果</span>
              <input v-model="reportResultMap[report.id]" type="text" placeholder="填写处理结论" />
            </label>
            <button class="ghost-btn" type="button" @click="handleReport(report.id)">处理完成</button>
          </div>
        </div>
      </div>
      <div v-else class="feed-empty">暂无举报。</div>

      <div class="pager" v-if="reportTotal > reportSize">
        <button class="ghost-btn" type="button" :disabled="reportPage <= 1" @click="loadReports(reportPage - 1)">
          上一页
        </button>
        <span>{{ reportPage }} / {{ reportPages }}</span>
        <button
          class="ghost-btn"
          type="button"
          :disabled="reportPage >= reportPages"
          @click="loadReports(reportPage + 1)"
        >
          下一页
        </button>
      </div>

      <p v-if="reportActionError" class="form-alert error">{{ reportActionError }}</p>
      <p v-if="reportActionSuccess" class="form-alert success">{{ reportActionSuccess }}</p>
    </section>
  </div>
</template>
