<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, ChatDotRound, Document, Warning } from '@element-plus/icons-vue'
import { apiFetch } from '../utils/api'
import { formatSnippet } from '../utils/user'
import UserShell from '../components/user/UserShell.vue'

const router = useRouter()
const activeTab = ref('notifications')

const notifications = ref([])
const notifLoading = ref(false)
const notifError = ref('')
const notifPage = ref(1)
const notifSize = ref(10)
const notifTotal = ref(0)
const notifFilter = ref('all')
const unreadCount = ref(0)
const friendRequestActionMap = ref({})
const handledFriendRequestMap = ref({})

const announcements = ref([])
const announcementLoading = ref(false)
const announcementError = ref('')
const announcementPage = ref(1)
const announcementSize = ref(8)
const announcementTotal = ref(0)
const activeAnnouncement = ref(null)

const reportTargetType = ref(0)
const reportTargetId = ref(null)
const reportReason = ref('')
const reportDetail = ref('')
const reportLoading = ref(false)
const reportError = ref('')
const reportSuccess = ref('')
const reportTargetKeyword = ref('')
const reportTargetOptions = ref([])
const reportTargetSearchLoading = ref(false)
const reportList = ref([])
const reportPage = ref(1)
const reportSize = ref(8)
const reportTotal = ref(0)
const reportStatus = ref('all')
const reportListLoading = ref(false)
const reportListError = ref('')
const reportReasons = ['不实信息', '骚扰辱骂', '色情低俗', '违规广告', '侵犯隐私', '其他']
const adminFeedbackReasons = ['功能建议', '使用问题', '申诉说明', '体验反馈', '其他']

const unreadNotifications = computed(() => notifications.value.filter((item) => item.isRead === 0).slice(0, 3))
const currentReportReasons = computed(() => (reportTargetType.value === 3 ? adminFeedbackReasons : reportReasons))

const authGuard = (res) => {
  if (res.status === 401) {
    router.push('/login')
    return true
  }
  return false
}

const getNotificationKind = (item) => {
  const type = Number(item?.type)
  const title = item?.title || ''
  const refType = Number(item?.refType)

  if (type === 0) return 'like'
  if (type === 1) return 'comment'
  if (type === 2) return 'reply'
  if (type === 3) return 'follow'
  if (type === 4) {
    if (refType === 4 || title.includes('好友申请')) return 'friend_request'
    if (title.includes('审核')) return 'audit'
    if (title.includes('举报')) return 'report'
    return 'system'
  }
  if (type === 5) return 'friend_result'
  if (type === 6) return 'audit'
  if (type === 7) return 'report'
  return 'system'
}

const notificationLabel = (item) => {
  const kind = getNotificationKind(item)
  if (kind === 'like') return '点赞提醒'
  if (kind === 'comment') return '评论提醒'
  if (kind === 'reply') return '回复提醒'
  if (kind === 'follow') return '关注提醒'
  if (kind === 'friend_request') return '好友申请'
  if (kind === 'friend_result') return '好友结果'
  if (kind === 'audit') return '审核结果'
  if (kind === 'report') return '举报反馈'
  return '系统通知'
}

const reportStatusLabel = (status) => (status === 1 ? '已处理' : '待处理')
const reportTargetLabel = (type) => {
  if (type === 1) return '评论'
  if (type === 2) return '用户'
  if (type === 3) return '管理员反馈'
  return '内容'
}
const reportTargetFieldLabel = computed(() => {
  if (reportTargetType.value === 0) return '举报内容'
  if (reportTargetType.value === 1) return '举报评论'
  if (reportTargetType.value === 2) return '举报用户名'
  if (reportTargetType.value === 3) return '管理员用户名'
  return '举报对象'
})
const reportTargetPlaceholder = computed(() => {
  if (reportTargetType.value === 0) return '输入内容标题、正文关键词搜索'
  if (reportTargetType.value === 1) return '输入评论内容、作者或内容标题搜索'
  if (reportTargetType.value === 2) return '输入用户名搜索相关用户'
  if (reportTargetType.value === 3) return '输入管理员用户名搜索'
  return '请输入关键词搜索对象'
})

const loadUnreadCount = async () => {
  try {
    const res = await apiFetch('/api/notifications/unread-count')
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) return
    unreadCount.value = data.data ?? 0
  } catch {
    // Ignore
  }
}

const loadNotifications = async (page = 1) => {
  notifLoading.value = true
  notifError.value = ''
  try {
    const params = new URLSearchParams({ page: String(page), size: String(notifSize.value) })
    if (notifFilter.value === 'unread') params.set('isRead', '0')
    if (notifFilter.value === 'read') params.set('isRead', '1')
    const res = await apiFetch(`/api/notifications?${params.toString()}`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      notifError.value = data.message || '获取通知失败。'
      return
    }
    notifications.value = data.data?.list || []
    notifTotal.value = data.data?.total ?? 0
    notifPage.value = data.data?.page ?? page
  } catch {
    notifError.value = '网络错误，无法获取通知。'
  } finally {
    notifLoading.value = false
  }
}

const markRead = async (id) => {
  if (!id) return
  try {
    const res = await apiFetch(`/api/notifications/${id}/read`, { method: 'PUT' })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      notifError.value = data.message || '标记已读失败。'
      return
    }
    notifications.value = notifications.value.map((item) => (item.id === id ? { ...item, isRead: 1 } : item))
    loadUnreadCount()
  } catch {
    notifError.value = '网络错误，无法标记已读。'
  }
}

const markAllRead = async () => {
  try {
    const res = await apiFetch('/api/notifications/read-all', { method: 'PUT' })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      notifError.value = data.message || '全部已读失败。'
      return
    }
    notifications.value = notifications.value.map((item) => ({ ...item, isRead: 1 }))
    unreadCount.value = 0
  } catch {
    notifError.value = '网络错误，无法全部已读。'
  }
}

const goNotificationTarget = (item) => {
  const kind = getNotificationKind(item)
  if (item.refType === 0 && item.refId) return router.push(`/posts/${item.refId}`)
  if (item.refType === 2 && item.refId) return router.push('/profile')
  if (item.refType === 3) return router.push('/chat')
  if (kind === 'friend_request' || kind === 'friend_result') return router.push('/chat')
}

const respondFriendRequest = async (item, approved) => {
  if (!item?.refId) return
  friendRequestActionMap.value = {
    ...friendRequestActionMap.value,
    [item.refId]: true,
  }
  try {
    const res = await apiFetch(`/api/friends/requests/${item.refId}/respond`, {
      method: 'POST',
      body: JSON.stringify({ approved }),
    })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      notifError.value = data.message || '处理好友申请失败。'
      if (data.message && data.message.includes('已经处理过')) {
        handledFriendRequestMap.value = {
          ...handledFriendRequestMap.value,
          [item.refId]: true,
        }
      }
      return
    }
    handledFriendRequestMap.value = {
      ...handledFriendRequestMap.value,
      [item.refId]: true,
    }
    await Promise.all([loadNotifications(notifPage.value || 1), loadUnreadCount()])
  } catch {
    notifError.value = '网络错误，无法处理好友申请。'
  } finally {
    friendRequestActionMap.value = {
      ...friendRequestActionMap.value,
      [item.refId]: false,
    }
  }
}

const loadAnnouncements = async (page = 1) => {
  announcementLoading.value = true
  announcementError.value = ''
  try {
    const res = await apiFetch(`/api/announcements?page=${page}&size=${announcementSize.value}`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      announcementError.value = data.message || '获取公告失败。'
      return
    }
    announcements.value = data.data?.list || []
    announcementTotal.value = data.data?.total ?? 0
    announcementPage.value = data.data?.page ?? page
    if (!activeAnnouncement.value && announcements.value.length) {
      activeAnnouncement.value = announcements.value[0]
    }
  } catch {
    announcementError.value = '网络错误，无法获取公告。'
  } finally {
    announcementLoading.value = false
  }
}

const loadAnnouncementDetail = async (id) => {
  if (!id) return
  announcementError.value = ''
  try {
    const res = await apiFetch(`/api/announcements/${id}`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      announcementError.value = data.message || '获取公告详情失败。'
      return
    }
    activeAnnouncement.value = data.data
  } catch {
    announcementError.value = '网络错误，无法获取公告详情。'
  }
}

const fetchReportTargets = async (keyword = '') => {
  const q = keyword.trim()
  reportTargetKeyword.value = q
  if (!q) {
    reportTargetOptions.value = []
    return
  }
  reportTargetSearchLoading.value = true
  try {
    const params = new URLSearchParams({
      targetType: String(reportTargetType.value),
      keyword: q,
      limit: '10',
    })
    const res = await apiFetch(`/api/reports/targets?${params.toString()}`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      reportError.value = data.message || '搜索举报对象失败。'
      return
    }
    reportTargetOptions.value = data.data || []
  } catch {
    reportError.value = '网络错误，无法搜索举报对象。'
  } finally {
    reportTargetSearchLoading.value = false
  }
}

const handleReportTargetTypeChange = (value) => {
  reportTargetType.value = value
  reportTargetId.value = null
  reportReason.value = ''
  reportTargetKeyword.value = ''
  reportTargetOptions.value = []
}

const submitReport = async () => {
  reportError.value = ''
  reportSuccess.value = ''
  if (!reportTargetId.value || !reportReason.value.trim()) {
    reportError.value = '请选择反馈对象并选择原因。'
    return
  }
  reportLoading.value = true
  try {
    const res = await apiFetch('/api/reports', {
      method: 'POST',
      body: JSON.stringify({
        targetType: reportTargetType.value,
        targetId: Number(reportTargetId.value),
        reason: reportReason.value.trim(),
        detail: reportDetail.value.trim() || null,
      }),
    })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      reportError.value = data.message || '提交举报失败。'
      return
    }
    reportSuccess.value = reportTargetType.value === 3 ? '反馈已提交，我们会尽快处理。' : '举报已提交，我们会尽快处理。'
    reportTargetId.value = null
    reportTargetKeyword.value = ''
    reportTargetOptions.value = []
    reportReason.value = ''
    reportDetail.value = ''
    loadReportList(1)
  } catch {
    reportError.value = '网络错误，无法提交举报。'
  } finally {
    reportLoading.value = false
  }
}

const loadReportList = async (page = 1) => {
  reportListLoading.value = true
  reportListError.value = ''
  try {
    const params = new URLSearchParams({ page: String(page), size: String(reportSize.value) })
    if (reportStatus.value === 'pending') params.set('status', '0')
    if (reportStatus.value === 'handled') params.set('status', '1')
    const res = await apiFetch(`/api/reports?${params.toString()}`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      reportListError.value = data.message || '获取举报记录失败。'
      return
    }
    reportList.value = data.data?.list || []
    reportTotal.value = data.data?.total ?? 0
    reportPage.value = data.data?.page ?? page
  } catch {
    reportListError.value = '网络错误，无法获取举报记录。'
  } finally {
    reportListLoading.value = false
  }
}

const changeNotifFilter = (filter) => {
  notifFilter.value = filter
  loadNotifications(1)
}

const changeReportFilter = (filter) => {
  reportStatus.value = filter
  loadReportList(1)
}

onMounted(() => {
  loadUnreadCount()
  loadNotifications()
  loadAnnouncements()
  loadReportList()
})
</script>

<template>
  <UserShell section="social">
    <section class="campus-hero">
      <span class="campus-hero__eyebrow"><el-icon><Bell /></el-icon>互动中心</span>
      <h1 class="campus-hero__title">消息、公告与举报进度</h1>
      <p class="campus-hero__subtitle">这里不再是信息大杂烩，而是按通知流、公告流和反馈进度重新分区，方便快速处理高优先级事项。</p>
      <div class="campus-hero__chips">
        <el-tag round type="danger">未读 {{ unreadCount }}</el-tag>
        <el-tag round>{{ notifTotal }} 条通知</el-tag>
        <el-tag round type="success">{{ announcementTotal }} 条公告</el-tag>
      </div>
    </section>

    <div class="campus-grid">
      <div>
        <el-card class="campus-panel" shadow="never">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="消息通知" name="notifications" />
            <el-tab-pane label="系统公告" name="announcements" />
            <el-tab-pane label="举报反馈" name="reports" />
          </el-tabs>

          <template v-if="activeTab === 'notifications'">
            <div class="campus-panel__header">
              <div>
                <h2 class="campus-panel__title">消息通知</h2>
                <p class="campus-panel__desc">点赞、评论、回复和关注提醒统一按流展示。</p>
              </div>
              <div class="campus-inline-actions">
                <el-button plain @click="markAllRead">全部已读</el-button>
              </div>
            </div>
            <div class="campus-inline-actions" style="margin-bottom: 16px">
              <el-button :type="notifFilter === 'all' ? 'primary' : 'default'" plain @click="changeNotifFilter('all')">全部</el-button>
              <el-button :type="notifFilter === 'unread' ? 'primary' : 'default'" plain @click="changeNotifFilter('unread')">未读</el-button>
              <el-button :type="notifFilter === 'read' ? 'primary' : 'default'" plain @click="changeNotifFilter('read')">已读</el-button>
            </div>
            <el-alert v-if="notifError" :title="notifError" type="error" show-icon style="margin-bottom: 14px" />
            <div v-if="notifLoading" class="campus-side-list"><el-skeleton v-for="index in 3" :key="index" animated :rows="3" /></div>
            <div v-else-if="notifications.length" class="campus-side-list">
              <div v-for="item in notifications" :key="item.id" class="campus-notification" :class="{ 'is-unread': item.isRead === 0 }">
                <div>
                  <el-tag round :type="item.isRead === 0 ? 'danger' : 'info'">{{ notificationLabel(item) }}</el-tag>
                  <h3 class="campus-side-item__title" style="margin-top: 10px">{{ item.title || '通知' }}</h3>
                  <p class="campus-side-item__desc">{{ item.content || '暂无内容' }}</p>
                  <p class="campus-muted" style="margin-top: 10px">{{ item.createdAt || '-' }}</p>
                </div>
                <div class="campus-inline-actions">
                  <el-button plain @click="goNotificationTarget(item)">查看</el-button>
                  <el-button
                    v-if="getNotificationKind(item) === 'friend_request' && handledFriendRequestMap[item.refId] !== true"
                    type="primary"
                    plain
                    :loading="friendRequestActionMap[item.refId] === true"
                    @click="respondFriendRequest(item, true)"
                  >
                    同意
                  </el-button>
                  <el-button
                    v-if="getNotificationKind(item) === 'friend_request' && handledFriendRequestMap[item.refId] !== true"
                    type="danger"
                    plain
                    :loading="friendRequestActionMap[item.refId] === true"
                    @click="respondFriendRequest(item, false)"
                  >
                    拒绝
                  </el-button>
                  <el-button plain :disabled="item.isRead === 1" @click="markRead(item.id)">
                    {{ item.isRead === 1 ? '已读' : '标记已读' }}
                  </el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无通知。" />
            <div v-if="notifTotal > notifSize" style="margin-top: 20px">
              <el-pagination background layout="prev, pager, next, ->, total" :current-page="notifPage" :page-size="notifSize" :total="notifTotal" @current-change="loadNotifications" />
            </div>
          </template>

          <template v-if="activeTab === 'announcements'">
            <div class="campus-panel__header">
              <div>
                <h2 class="campus-panel__title">系统公告</h2>
                <p class="campus-panel__desc">左侧列表快速浏览，右侧聚焦当前选中的公告。</p>
              </div>
              <el-button plain @click="loadAnnouncements(announcementPage)">刷新公告</el-button>
            </div>
            <el-alert v-if="announcementError" :title="announcementError" type="error" show-icon style="margin-bottom: 14px" />
            <div v-if="announcementLoading" class="campus-announcement-grid"><el-skeleton animated :rows="6" /><el-skeleton animated :rows="8" /></div>
            <div v-else-if="announcements.length" class="campus-announcement-grid">
              <div class="campus-announcement-list">
                <div
                  v-for="item in announcements"
                  :key="item.id"
                  class="campus-announcement-item"
                  :class="{ 'is-active': activeAnnouncement?.id === item.id }"
                >
                  <h3 class="campus-side-item__title">{{ item.title || '公告' }}</h3>
                  <p class="campus-side-item__desc">{{ formatSnippet(item.content, 92) }}</p>
                  <div class="campus-inline-actions" style="margin-top: 12px">
                    <el-button plain @click="loadAnnouncementDetail(item.id)">查看详情</el-button>
                  </div>
                </div>
              </div>
              <div class="campus-announcement-item">
                <el-tag round type="success">当前公告</el-tag>
                <h3 class="campus-side-item__title" style="margin-top: 14px">{{ activeAnnouncement?.title || '请选择公告' }}</h3>
                <p class="campus-side-item__desc" style="margin-top: 12px">{{ activeAnnouncement?.content || '右侧会显示完整公告内容。' }}</p>
                <p class="campus-muted" style="margin-top: 14px">{{ activeAnnouncement?.createdAt || '-' }}</p>
              </div>
            </div>
            <el-empty v-else description="暂无公告。" />
            <div v-if="announcementTotal > announcementSize" style="margin-top: 20px">
              <el-pagination background layout="prev, pager, next, ->, total" :current-page="announcementPage" :page-size="announcementSize" :total="announcementTotal" @current-change="loadAnnouncements" />
            </div>
          </template>

          <template v-if="activeTab === 'reports'">
            <div class="campus-panel__header">
              <div>
                <h2 class="campus-panel__title">举报反馈</h2>
                <p class="campus-panel__desc">支持举报内容、评论、用户，也支持直接向管理员提交反馈。</p>
              </div>
            </div>
            <el-form label-position="top">
              <el-row :gutter="16">
                <el-col :md="8" :xs="24">
                  <el-form-item label="反馈类型">
                    <el-select :model-value="reportTargetType" @change="handleReportTargetTypeChange">
                      <el-option label="内容举报" :value="0" />
                      <el-option label="评论举报" :value="1" />
                      <el-option label="用户举报" :value="2" />
                      <el-option label="管理员反馈" :value="3" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :md="8" :xs="24">
                  <el-form-item :label="reportTargetFieldLabel">
                    <el-select
                      v-model="reportTargetId"
                      filterable
                      remote
                      reserve-keyword
                      clearable
                      :remote-method="fetchReportTargets"
                      :loading="reportTargetSearchLoading"
                      :placeholder="reportTargetPlaceholder"
                    >
                      <el-option
                        v-for="item in reportTargetOptions"
                        :key="item.id"
                        :label="item.displayName"
                        :value="item.id"
                      >
                        <div style="display: flex; flex-direction: column; gap: 4px">
                          <span>{{ item.displayName }}</span>
                          <span style="color: #909399">{{ item.subtitle || `${item.school || '未填学校'} / ${item.college || '未填学院'}` }}</span>
                        </div>
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :md="8" :xs="24"><el-form-item :label="reportTargetType === 3 ? '反馈原因' : '举报原因'"><el-select v-model="reportReason"><el-option v-for="item in currentReportReasons" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
              </el-row>
              <el-form-item label="补充说明"><el-input v-model="reportDetail" type="textarea" :rows="4" resize="none" placeholder="可填写更多描述" /></el-form-item>
            </el-form>
            <div class="campus-inline-actions">
              <el-button :type="reportTargetType === 3 ? 'primary' : 'danger'" :loading="reportLoading" @click="submitReport">
                {{ reportTargetType === 3 ? '提交反馈' : '提交举报' }}
              </el-button>
            </div>
            <el-alert v-if="reportError" :title="reportError" type="error" show-icon style="margin-top: 14px" />
            <el-alert v-if="reportSuccess" :title="reportSuccess" type="success" show-icon style="margin-top: 14px" />

            <div class="campus-panel__header" style="margin-top: 24px">
              <div>
                <h2 class="campus-panel__title">我的举报记录</h2>
                <p class="campus-panel__desc">查看每一条举报的处理进度和结果。</p>
              </div>
            </div>
            <div class="campus-inline-actions" style="margin-bottom: 16px">
              <el-button :type="reportStatus === 'all' ? 'primary' : 'default'" plain @click="changeReportFilter('all')">全部</el-button>
              <el-button :type="reportStatus === 'pending' ? 'primary' : 'default'" plain @click="changeReportFilter('pending')">待处理</el-button>
              <el-button :type="reportStatus === 'handled' ? 'primary' : 'default'" plain @click="changeReportFilter('handled')">已处理</el-button>
            </div>
            <el-alert v-if="reportListError" :title="reportListError" type="error" show-icon style="margin-bottom: 14px" />
            <div v-if="reportListLoading" class="campus-side-list"><el-skeleton v-for="index in 3" :key="index" animated :rows="3" /></div>
            <div v-else-if="reportList.length" class="campus-side-list">
              <div v-for="item in reportList" :key="item.id" class="campus-side-item">
                <div class="campus-inline-actions">
                  <el-tag round>{{ reportTargetLabel(item.targetType) }}</el-tag>
                  <el-tag round :type="item.status === 1 ? 'success' : 'warning'">{{ reportStatusLabel(item.status) }}</el-tag>
                </div>
                <h3 class="campus-side-item__title" style="margin-top: 12px">{{ item.reason || '举报' }}</h3>
                <p class="campus-side-item__desc">{{ item.detail || '未填写补充说明' }}</p>
                <p class="campus-muted" style="margin-top: 10px">
                  对象：{{ item.targetName || `#${item.targetId}` }} · 提交于 {{ item.createdAt || '-' }}
                </p>
                <p v-if="item.result" class="campus-muted" style="margin-top: 6px">处理结果：{{ item.result }}</p>
              </div>
            </div>
            <el-empty v-else description="暂无举报记录。" />
            <div v-if="reportTotal > reportSize" style="margin-top: 20px">
              <el-pagination background layout="prev, pager, next, ->, total" :current-page="reportPage" :page-size="reportSize" :total="reportTotal" @current-change="loadReportList" />
            </div>
          </template>
        </el-card>
      </div>

      <aside>
        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">处理摘要</h2>
              <p class="campus-panel__desc">帮助你判断现在先处理哪一类事项。</p>
            </div>
          </div>
          <div class="campus-stat-grid">
            <div class="campus-stat-box"><p class="campus-stat-box__label">未读通知</p><p class="campus-stat-box__value">{{ unreadCount }}</p></div>
            <div class="campus-stat-box"><p class="campus-stat-box__label">待处理举报</p><p class="campus-stat-box__value">{{ reportList.filter((item) => item.status !== 1).length }}</p></div>
          </div>
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">优先处理</h2>
              <p class="campus-panel__desc">建议你先看未读提醒，再处理平台反馈。</p>
            </div>
          </div>
            <div v-if="unreadNotifications.length" class="campus-side-list">
              <div v-for="item in unreadNotifications" :key="item.id" class="campus-side-item">
              <p class="campus-side-item__title">{{ notificationLabel(item) }}</p>
              <p class="campus-side-item__desc">{{ formatSnippet(item.content, 70) }}</p>
              <el-button text type="primary" @click="goNotificationTarget(item)">立即查看</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无高优先级通知" />
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-side-list">
            <div class="campus-side-item">
              <p class="campus-side-item__title"><el-icon><ChatDotRound /></el-icon> 社区群聊</p>
              <p class="campus-side-item__desc">进入社区群聊，与其他用户实时交流校园动态和热门话题。</p>
              <el-button type="primary" plain @click="router.push('/chat')">进入群聊</el-button>
            </div>
            <div class="campus-side-item">
              <p class="campus-side-item__title"><el-icon><ChatDotRound /></el-icon> 内容互动</p>
              <p class="campus-side-item__desc">消息提醒会优先引导你回到对应内容详情继续互动。</p>
            </div>
            <div class="campus-side-item">
              <p class="campus-side-item__title"><el-icon><Document /></el-icon> 公告阅读</p>
              <p class="campus-side-item__desc">公告改为主从布局，不需要在长列表中反复寻找正文。</p>
            </div>
            <div class="campus-side-item">
              <p class="campus-side-item__title"><el-icon><Warning /></el-icon> 违规反馈</p>
              <p class="campus-side-item__desc">举报提交后，下方会同步展示状态和处理结果。</p>
            </div>
          </div>
        </el-card>
      </aside>
    </div>
  </UserShell>
</template>
