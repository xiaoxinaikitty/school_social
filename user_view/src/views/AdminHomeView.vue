<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound,
  Flag,
  Histogram,
  Promotion,
  User,
} from '@element-plus/icons-vue'
import AdminShell from '../components/admin/AdminShell.vue'
import { formatPercent, requestData } from '../utils/admin'

const router = useRouter()

const pendingCount = ref(0)
const reportPendingCount = ref(0)
const pendingPreview = ref([])
const overview = ref(null)
const userStats = ref([])
const contentStats = ref([])
const statsDays = ref(14)
const loading = ref(false)
const error = ref('')

const activeSeries = computed(() => userStats.value.slice(-7))
const activePath = computed(() => {
  const series = activeSeries.value
  if (!series.length) return ''
  const width = 260
  const height = 84
  const max = Math.max(...series.map((item) => item.activeUsers ?? 0), 1)
  return series
    .map((item, index) => {
      const x = (index / Math.max(series.length - 1, 1)) * width
      const y = height - ((item.activeUsers ?? 0) / max) * (height - 14) - 7
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`
    })
    .join(' ')
})

const interactionBars = computed(() => {
  const source = overview.value || {}
  const total = Math.max(
    source.postCount ?? 0,
    source.interactionCount ?? 0,
    source.newUsers ?? 0,
    source.activeUsers ?? 0,
    1,
  )
  return [
    { label: '新增用户', value: source.newUsers ?? 0 },
    { label: '活跃用户', value: source.activeUsers ?? 0 },
    { label: '新增内容', value: source.postCount ?? 0 },
    { label: '新增互动', value: source.interactionCount ?? 0 },
  ].map((item) => ({
    ...item,
    width: `${Math.max((item.value / total) * 100, 8)}%`,
  }))
})

const summaryCards = computed(() => [
  {
    label: '待审内容',
    value: pendingCount.value,
    note: '需要人工处理的内容队列',
    icon: Promotion,
    tag: '审核',
  },
  {
    label: '待处理举报',
    value: reportPendingCount.value,
    note: '用户提交的风险事件',
    icon: Flag,
    tag: '风控',
  },
  {
    label: '活跃用户',
    value: overview.value?.activeUsers ?? 0,
    note: `近 ${statsDays.value} 天平台活跃表现`,
    icon: User,
    tag: '活跃',
  },
  {
    label: '新增互动',
    value: overview.value?.interactionCount ?? 0,
    note: '包含点赞、评论、收藏与分享',
    icon: ChatDotRound,
    tag: '互动',
  },
])

const reloadAll = async () => {
  loading.value = true
  error.value = ''
  try {
    const [pendingData, reportData, previewData, overviewData, userData, contentData] = await Promise.all([
      requestData(router, '/api/admin/posts?page=1&size=1&status=0'),
      requestData(router, '/api/reports/admin?page=1&size=1&status=0'),
      requestData(router, '/api/admin/posts?page=1&size=4&status=0'),
      requestData(router, `/api/admin/stats/overview?days=${statsDays.value}`),
      requestData(router, `/api/admin/stats/users?days=${statsDays.value}`),
      requestData(router, `/api/admin/stats/content?days=${statsDays.value}`),
    ])

    pendingCount.value = pendingData?.total ?? 0
    reportPendingCount.value = reportData?.total ?? 0
    pendingPreview.value = previewData?.list || []
    overview.value = overviewData
    userStats.value = userData || []
    contentStats.value = contentData || []
  } catch (err) {
    error.value = err?.message || '数据加载失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  reloadAll()
})
</script>

<template>
  <AdminShell title="管理员控制台" subtitle="统一掌握内容审核、用户风险、运营发布与趋势表现。">
    <template #actions>
      <div class="admin-toolbar-card">
        <el-select v-model="statsDays" style="width: 132px" @change="reloadAll">
          <el-option :value="7" label="近 7 天" />
          <el-option :value="14" label="近 14 天" />
          <el-option :value="30" label="近 30 天" />
        </el-select>
        <el-button :icon="Histogram" plain @click="router.push('/admin/stats')">深入分析</el-button>
        <el-button :loading="loading" type="primary" @click="reloadAll">刷新看板</el-button>
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

      <el-row :gutter="18">
        <el-col v-for="card in summaryCards" :key="card.label" :xs="24" :sm="12" :xl="6">
          <el-card class="admin-surface-card admin-kpi-card" shadow="hover">
            <div class="admin-kpi-head">
              <div class="admin-kpi-icon">
                <el-icon><component :is="card.icon" /></el-icon>
              </div>
              <el-tag effect="dark" round>{{ card.tag }}</el-tag>
            </div>
            <el-statistic :title="card.label" :value="card.value" />
            <div class="admin-kpi-note">{{ card.note }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="18">
        <el-col :xs="24" :xl="14">
          <el-card class="admin-glass-card" shadow="never">
            <div class="admin-section-heading">
              <div>
                <h3>核心动作区</h3>
                <p>把最常用的后台动作前置，缩短管理路径。</p>
              </div>
            </div>
            <div class="admin-action-grid">
              <div class="admin-action-tile">
                <h4>处理内容审核</h4>
                <p>当前有 {{ pendingCount }} 条待审内容，建议优先清空堆积。</p>
                <el-button type="primary" @click="router.push('/admin/audit')">进入审核台</el-button>
              </div>
              <div class="admin-action-tile">
                <h4>查看举报中心</h4>
                <p>待处理举报 {{ reportPendingCount }} 条，可快速核定风险。</p>
                <el-button plain @click="router.push('/admin/reports')">打开举报页</el-button>
              </div>
              <div class="admin-action-tile">
                <h4>发布运营公告</h4>
                <p>统一处理公告、标签和推荐策略，提升内容分发质量。</p>
                <el-button plain @click="router.push('/admin/ops')">前往运营配置</el-button>
              </div>
              <div class="admin-action-tile">
                <h4>查看增长趋势</h4>
                <p>根据时间范围切换，快速识别增长、活跃与留存变化。</p>
                <el-button plain @click="router.push('/admin/stats')">查看趋势</el-button>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :xl="10">
          <el-card class="admin-glass-card" shadow="never">
            <div class="admin-section-heading">
              <div>
                <h3>审核提醒</h3>
                <p>待审内容预览，帮助快速切换到高优先级任务。</p>
              </div>
            </div>
            <el-timeline class="admin-timeline">
              <el-timeline-item
                v-for="item in pendingPreview"
                :key="item.id"
                placement="top"
                :timestamp="item.createdAt || '--'"
              >
                <div style="display:flex;justify-content:space-between;gap:12px;align-items:flex-start;">
                  <div>
                    <div style="font-weight:600;color:#0f172a;">#{{ item.id }} {{ item.title || '未命名内容' }}</div>
                    <div style="margin-top:4px;color:#64748b;">
                      {{ item.content?.slice(0, 54) || '暂无正文摘要' }}
                    </div>
                  </div>
                  <el-button text type="primary" @click="router.push('/admin/audit')">处理</el-button>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty
              v-if="!pendingPreview.length && !loading"
              class="admin-empty"
              description="当前没有待审内容"
            />
          </el-card>
        </el-col>
      </el-row>

      <div class="admin-insight-grid">
        <div class="admin-insight-card">
          <h4>留存健康度</h4>
          <p>直接观察新增用户的沉淀能力。</p>
          <el-progress
            type="dashboard"
            :percentage="Number(((overview?.retention1d ?? 0) * 100).toFixed(1))"
            :stroke-width="12"
            color="#2563eb"
          >
            <template #default>
              <div style="font-size:0.88rem;color:#64748b;">次日留存</div>
              <div style="font-size:1.4rem;font-weight:700;color:#0f172a;">
                {{ formatPercent(overview?.retention1d) }}
              </div>
            </template>
          </el-progress>
          <div class="admin-insight-foot">
            7 日留存 {{ formatPercent(overview?.retention7d) }}
          </div>
        </div>

        <div class="admin-insight-card">
          <h4>活跃走势</h4>
          <p>近 7 个统计节点的活跃用户变化。</p>
          <svg viewBox="0 0 260 84" class="admin-mini-sparkline" preserveAspectRatio="none">
            <path :d="activePath" />
          </svg>
          <div class="admin-insight-foot">
            当前活跃 {{ overview?.activeUsers ?? 0 }} 人
          </div>
        </div>

        <div class="admin-insight-card">
          <h4>运营强度</h4>
          <p>新增用户、内容与互动量级对比。</p>
          <div class="admin-bar-list">
            <div v-for="item in interactionBars" :key="item.label" class="admin-bar-row">
              <span>{{ item.label }}</span>
              <div class="admin-bar-track">
                <div class="admin-bar-fill" :style="{ width: item.width }"></div>
              </div>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AdminShell>
</template>
