<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import AdminShell from '../components/admin/AdminShell.vue'
import { formatDateTime, requestData, shortText } from '../utils/admin'

const router = useRouter()
const reportList = ref([])
const reportPage = ref(1)
const reportSize = ref(10)
const reportTotal = ref(0)
const reportStatus = ref(0)
const reportLoading = ref(false)
const reportError = ref('')
const reportResultMap = ref({})
const reportDecisionMap = ref({})

const loadReports = async (targetPage = 1) => {
  reportLoading.value = true
  reportError.value = ''
  try {
    const data = await requestData(
      router,
      `/api/reports/admin?page=${targetPage}&size=${reportSize.value}&status=${reportStatus.value}`,
    )
    reportList.value = data?.list || []
    reportTotal.value = data?.total ?? 0
    reportPage.value = data?.page ?? targetPage
  } catch (err) {
    reportError.value = err?.message || '获取举报列表失败'
  } finally {
    reportLoading.value = false
  }
}

const handleReport = async (reportId) => {
  try {
    await requestData(router, `/api/reports/admin/${reportId}/handle`, {
      method: 'PUT',
      body: JSON.stringify({
        decision: reportDecisionMap.value[reportId] ?? 1,
        result: reportResultMap.value[reportId] || '已处理',
      }),
    })
    ElMessage.success('举报已处理')
    loadReports(reportPage.value)
  } catch (err) {
    ElMessage.error(err?.message || '举报处理失败')
  }
}

onMounted(() => {
  loadReports()
})
</script>

<template>
  <AdminShell title="举报处理中心" subtitle="集中处理举报工单，保留处理结论与说明，形成清晰风控闭环。">
    <template #actions>
      <div class="admin-filter-group">
        <el-radio-group v-model="reportStatus" @change="loadReports(1)">
          <el-radio-button :label="0">待处理</el-radio-button>
          <el-radio-button :label="1">已处理</el-radio-button>
        </el-radio-group>
        <el-button :icon="RefreshRight" plain @click="loadReports(reportPage)">刷新</el-button>
      </div>
    </template>

    <div class="admin-stack">
      <el-alert
        v-if="reportError"
        type="error"
        :closable="false"
        show-icon
        :title="reportError"
      />

      <el-card class="admin-surface-card" shadow="never">
        <div class="admin-section-heading">
          <div>
            <h3>举报工单列表</h3>
            <p>展开记录后可直接写入结论、说明并完成处理。</p>
          </div>
          <el-tag round effect="plain">共 {{ reportTotal }} 条举报</el-tag>
        </div>

        <el-table class="admin-compact-table" :data="reportList" v-loading="reportLoading">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="admin-expand-panel">
                <div class="admin-expand-meta">
                  <div>
                    <span>举报对象</span>
                    <strong>
                      {{ row.targetType === 0 ? '内容' : row.targetType === 1 ? '评论' : '用户' }} #{{ row.targetId }}
                    </strong>
                  </div>
                  <div>
                    <span>提交时间</span>
                    <strong>{{ formatDateTime(row.createdAt) }}</strong>
                  </div>
                  <div>
                    <span>当前状态</span>
                    <strong>{{ row.status === 1 ? '已处理' : '待处理' }}</strong>
                  </div>
                </div>

                <div class="admin-expand-content">{{ row.detail || '举报人未填写补充说明' }}</div>

                <div style="margin-top:18px;" class="admin-form-grid">
                  <el-select v-model="reportDecisionMap[row.id]" :disabled="row.status === 1">
                    <el-option :value="1" label="属实" />
                    <el-option :value="2" label="不属实" />
                  </el-select>
                  <el-input
                    v-model="reportResultMap[row.id]"
                    :disabled="row.status === 1"
                    type="textarea"
                    :rows="3"
                    placeholder="填写处理说明"
                  />
                </div>

                <div style="margin-top:18px;display:flex;gap:12px;justify-content:flex-end;">
                  <el-button
                    v-if="row.status !== 1"
                    type="primary"
                    @click="handleReport(row.id)"
                  >
                    标记为已处理
                  </el-button>
                  <el-tag v-else type="success" round>
                    {{ row.decision === 1 ? '属实' : '不属实' }} / {{ row.result || '已处理' }}
                  </el-tag>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="id" label="举报 ID" width="96" />
          <el-table-column label="举报原因" min-width="220">
            <template #default="{ row }">
              <div style="font-weight:600;color:#0f172a;">{{ row.reason || '未命名原因' }}</div>
              <div style="margin-top:4px;color:#64748b;">{{ shortText(row.detail, 72) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="举报对象" width="180">
            <template #default="{ row }">
              {{ row.targetType === 0 ? '内容' : row.targetType === 1 ? '评论' : '用户' }} #{{ row.targetId }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'" round>
                {{ row.status === 1 ? '已处理' : '待处理' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                :disabled="row.status === 1"
                type="primary"
                link
                @click="handleReport(row.id)"
              >
                处理
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="!reportLoading && !reportList.length"
          class="admin-empty"
          description="当前筛选条件下没有举报数据"
        />

        <div class="admin-table-footer" v-if="reportTotal > reportSize">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="reportPage"
            :page-size="reportSize"
            :total="reportTotal"
            @current-change="loadReports"
          />
        </div>
      </el-card>
    </div>
  </AdminShell>
</template>
