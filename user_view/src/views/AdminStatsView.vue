<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshRight } from '@element-plus/icons-vue'
import AdminShell from '../components/admin/AdminShell.vue'
import { formatDateTime, formatPercent, requestData } from '../utils/admin'

const router = useRouter()
const days = ref(30)
const overview = ref(null)
const userStats = ref([])
const contentStats = ref([])
const loading = ref(false)
const error = ref('')

const rangeLabel = computed(() => {
  if (!overview.value) return '--'
  return `${overview.value.rangeStart || '-'} ~ ${overview.value.rangeEnd || '-'}`
})

const loadAll = async () => {
  loading.value = true
  error.value = ''
  try {
    const [overviewData, userData, contentData] = await Promise.all([
      requestData(router, `/api/admin/stats/overview?days=${days.value}`),
      requestData(router, `/api/admin/stats/users?days=${days.value}`),
      requestData(router, `/api/admin/stats/content?days=${days.value}`),
    ])
    overview.value = overviewData
    userStats.value = userData || []
    contentStats.value = contentData || []
  } catch (err) {
    error.value = err?.message || '获取统计数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>

<template>
  <AdminShell title="数据统计看板" subtitle="把增长、活跃、留存和互动指标收拢到一个结构清晰的分析视图中。">
    <template #actions>
      <div class="admin-toolbar-card">
        <el-select v-model="days" style="width: 140px" @change="loadAll">
          <el-option :value="7" label="近 7 天" />
          <el-option :value="14" label="近 14 天" />
          <el-option :value="30" label="近 30 天" />
          <el-option :value="60" label="近 60 天" />
          <el-option :value="90" label="近 90 天" />
        </el-select>
        <el-button :icon="RefreshRight" type="primary" :loading="loading" @click="loadAll">
          刷新统计
        </el-button>
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
        <el-col :xs="24" :sm="12" :xl="4">
          <el-card class="admin-surface-card admin-kpi-card" shadow="hover">
            <el-statistic title="用户总数" :value="overview?.totalUsers ?? 0" />
            <div class="admin-kpi-note">统计区间 {{ rangeLabel }}</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :xl="4">
          <el-card class="admin-surface-card admin-kpi-card" shadow="hover">
            <el-statistic title="新增用户" :value="overview?.newUsers ?? 0" />
            <div class="admin-kpi-note">周期内新增注册</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :xl="4">
          <el-card class="admin-surface-card admin-kpi-card" shadow="hover">
            <el-statistic title="活跃用户" :value="overview?.activeUsers ?? 0" />
            <div class="admin-kpi-note">活跃行为去重统计</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :xl="4">
          <el-card class="admin-surface-card admin-kpi-card" shadow="hover">
            <el-statistic title="新增内容" :value="overview?.postCount ?? 0" />
            <div class="admin-kpi-note">周期内发布内容量</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :xl="4">
          <el-card class="admin-surface-card admin-kpi-card" shadow="hover">
            <el-statistic title="新增互动" :value="overview?.interactionCount ?? 0" />
            <div class="admin-kpi-note">点赞、评论、收藏、分享</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :xl="4">
          <el-card class="admin-surface-card admin-kpi-card" shadow="hover">
            <el-statistic title="7日留存" :value="Number(((overview?.retention7d ?? 0) * 100).toFixed(1))">
              <template #suffix>%</template>
            </el-statistic>
            <div class="admin-kpi-note">次日留存 {{ formatPercent(overview?.retention1d) }}</div>
          </el-card>
        </el-col>
      </el-row>

      <div class="admin-split">
        <el-card class="admin-glass-card" shadow="never">
          <div class="admin-section-heading">
            <div>
              <h3>留存与增长概览</h3>
              <p>平台健康度的关键指标集合。</p>
            </div>
          </div>
          <div class="admin-form-grid">
            <el-progress
              type="dashboard"
              :percentage="Number(((overview?.retention1d ?? 0) * 100).toFixed(1))"
              color="#2563eb"
            >
              <template #default>
                <div style="font-size:0.86rem;color:#64748b;">次日留存</div>
                <div style="font-size:1.35rem;font-weight:700;color:#0f172a;">
                  {{ formatPercent(overview?.retention1d) }}
                </div>
              </template>
            </el-progress>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="统计区间">{{ rangeLabel }}</el-descriptions-item>
              <el-descriptions-item label="用户总数">{{ overview?.totalUsers ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="总内容量">{{ overview?.totalPosts ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="总互动量">{{ overview?.totalInteractions ?? 0 }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>

        <el-card class="admin-glass-card" shadow="never">
          <div class="admin-section-heading">
            <div>
              <h3>内容活跃度条形视图</h3>
              <p>快速比较周期内核心行为强度。</p>
            </div>
          </div>
          <div class="admin-bar-list">
            <div class="admin-bar-row">
              <span>新增用户</span>
              <div class="admin-bar-track">
                <div class="admin-bar-fill" :style="{ width: `${Math.max((overview?.newUsers ?? 0) / Math.max(overview?.activeUsers ?? 1, 1) * 100, 8)}%` }"></div>
              </div>
              <strong>{{ overview?.newUsers ?? 0 }}</strong>
            </div>
            <div class="admin-bar-row">
              <span>活跃用户</span>
              <div class="admin-bar-track">
                <div class="admin-bar-fill" style="width:100%;"></div>
              </div>
              <strong>{{ overview?.activeUsers ?? 0 }}</strong>
            </div>
            <div class="admin-bar-row">
              <span>新增内容</span>
              <div class="admin-bar-track">
                <div class="admin-bar-fill" :style="{ width: `${Math.max((overview?.postCount ?? 0) / Math.max(overview?.interactionCount ?? 1, 1) * 100, 8)}%` }"></div>
              </div>
              <strong>{{ overview?.postCount ?? 0 }}</strong>
            </div>
            <div class="admin-bar-row">
              <span>新增互动</span>
              <div class="admin-bar-track">
                <div class="admin-bar-fill" style="width:100%;"></div>
              </div>
              <strong>{{ overview?.interactionCount ?? 0 }}</strong>
            </div>
          </div>
        </el-card>
      </div>

      <el-card class="admin-surface-card" shadow="never">
        <div class="admin-section-heading">
          <div>
            <h3>用户趋势明细</h3>
            <p>每日新增、活跃与留存数据。</p>
          </div>
        </div>
        <el-table class="admin-compact-table" :data="userStats" v-loading="loading">
          <el-table-column prop="date" label="日期" width="140" />
          <el-table-column prop="newUsers" label="新增用户" width="120" />
          <el-table-column prop="activeUsers" label="活跃用户" width="120" />
          <el-table-column label="次日留存" width="140">
            <template #default="{ row }">{{ formatPercent(row.retention1d) }}</template>
          </el-table-column>
          <el-table-column label="7日留存" width="140">
            <template #default="{ row }">{{ formatPercent(row.retention7d) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !userStats.length" class="admin-empty" description="暂无用户趋势数据" />
      </el-card>

      <el-card class="admin-surface-card" shadow="never">
        <div class="admin-section-heading">
          <div>
            <h3>内容与互动明细</h3>
            <p>每日内容发布与互动拆解。</p>
          </div>
        </div>
        <el-table class="admin-compact-table" :data="contentStats" v-loading="loading">
          <el-table-column prop="date" label="日期" width="140" />
          <el-table-column prop="postCount" label="发布量" width="110" />
          <el-table-column prop="interactionCount" label="互动量" width="110" />
          <el-table-column prop="likeCount" label="点赞" width="100" />
          <el-table-column prop="commentCount" label="评论" width="100" />
          <el-table-column prop="favoriteCount" label="收藏" width="100" />
          <el-table-column prop="shareCount" label="分享" width="100" />
          <el-table-column label="更新时间" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.date) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !contentStats.length" class="admin-empty" description="暂无内容互动数据" />
      </el-card>
    </div>
  </AdminShell>
</template>
