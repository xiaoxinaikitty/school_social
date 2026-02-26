<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const router = useRouter()

const notifications = ref([])
const notifLoading = ref(false)
const notifError = ref('')
const notifPage = ref(1)
const notifSize = ref(10)
const notifTotal = ref(0)
const notifFilter = ref('all')
const unreadCount = ref(0)

const announcements = ref([])
const announcementLoading = ref(false)
const announcementError = ref('')
const announcementPage = ref(1)
const announcementSize = ref(8)
const announcementTotal = ref(0)
const activeAnnouncement = ref(null)

const reportTargetType = ref(0)
const reportTargetId = ref('')
const reportReason = ref('')
const reportDetail = ref('')
const reportLoading = ref(false)
const reportError = ref('')
const reportSuccess = ref('')
const reportList = ref([])
const reportPage = ref(1)
const reportSize = ref(8)
const reportTotal = ref(0)
const reportStatus = ref('all')
const reportListLoading = ref(false)
const reportListError = ref('')

const reportReasons = [
  '不实信息',
  '骚扰辱骂',
  '色情低俗',
  '违规广告',
  '侵犯隐私',
  '其他',
]

const notifPages = computed(() => {
  const pages = Math.ceil(notifTotal.value / notifSize.value)
  return pages > 0 ? pages : 1
})

const announcementPages = computed(() => {
  const pages = Math.ceil(announcementTotal.value / announcementSize.value)
  return pages > 0 ? pages : 1
})

const reportPages = computed(() => {
  const pages = Math.ceil(reportTotal.value / reportSize.value)
  return pages > 0 ? pages : 1
})

const loadUnreadCount = async () => {
  try {
    const res = await apiFetch('/api/notifications/unread-count')
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      return
    }
    unreadCount.value = data.data ?? 0
  } catch (err) {
    // ignore
  }
}

const loadNotifications = async (page = 1) => {
  notifLoading.value = true
  notifError.value = ''
  try {
    const params = new URLSearchParams()
    params.set('page', page.toString())
    params.set('size', notifSize.value.toString())
    if (notifFilter.value === 'unread') {
      params.set('isRead', '0')
    } else if (notifFilter.value === 'read') {
      params.set('isRead', '1')
    }
    const res = await apiFetch(`/api/notifications?${params.toString()}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      notifError.value = data.message || '获取通知失败。'
      return
    }
    notifications.value = data.data?.list || []
    notifTotal.value = data.data?.total ?? 0
    notifPage.value = data.data?.page ?? page
  } catch (err) {
    notifError.value = '网络错误，无法获取通知。'
  } finally {
    notifLoading.value = false
  }
}

const markRead = async (id) => {
  if (!id) return
  try {
    const res = await apiFetch(`/api/notifications/${id}/read`, { method: 'PUT' })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      notifError.value = data.message || '标记已读失败。'
      return
    }
    notifications.value = notifications.value.map((item) =>
      item.id === id ? { ...item, isRead: 1 } : item,
    )
    loadUnreadCount()
  } catch (err) {
    notifError.value = '网络错误，无法标记已读。'
  }
}

const markAllRead = async () => {
  try {
    const res = await apiFetch('/api/notifications/read-all', { method: 'PUT' })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      notifError.value = data.message || '全部已读失败。'
      return
    }
    notifications.value = notifications.value.map((item) => ({ ...item, isRead: 1 }))
    unreadCount.value = 0
  } catch (err) {
    notifError.value = '网络错误，无法全部已读。'
  }
}

const setNotifFilter = (filter) => {
  notifFilter.value = filter
  notifPage.value = 1
  loadNotifications(1)
}

const notificationLabel = (type) => {
  switch (type) {
    case 0:
      return '点赞提醒'
    case 1:
      return '评论提醒'
    case 2:
      return '回复提醒'
    case 3:
      return '关注提醒'
    default:
      return '系统通知'
  }
}

const goNotificationTarget = (item) => {
  if (!item) return
  if (item.refType === 0 && item.refId) {
    router.push(`/posts/${item.refId}`)
    return
  }
  if (item.refType === 2 && item.refId) {
    router.push('/profile')
  }
}

const loadAnnouncements = async (page = 1) => {
  announcementLoading.value = true
  announcementError.value = ''
  try {
    const res = await apiFetch(`/api/announcements?page=${page}&size=${announcementSize.value}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      announcementError.value = data.message || '获取公告失败。'
      return
    }
    announcements.value = data.data?.list || []
    announcementTotal.value = data.data?.total ?? 0
    announcementPage.value = data.data?.page ?? page
  } catch (err) {
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
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      announcementError.value = data.message || '获取公告详情失败。'
      return
    }
    activeAnnouncement.value = data.data
  } catch (err) {
    announcementError.value = '网络错误，无法获取公告详情。'
  }
}

const submitReport = async () => {
  reportError.value = ''
  reportSuccess.value = ''
  if (!reportTargetId.value) {
    reportError.value = '请输入对象 ID。'
    return
  }
  if (!reportReason.value.trim()) {
    reportError.value = '请选择举报原因。'
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
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      reportError.value = data.message || '提交举报失败。'
      return
    }
    reportSuccess.value = '举报已提交，我们会尽快处理。'
    reportTargetId.value = ''
    reportReason.value = ''
    reportDetail.value = ''
    loadReportList(1)
  } catch (err) {
    reportError.value = '网络错误，无法提交举报。'
  } finally {
    reportLoading.value = false
  }
}

const loadReportList = async (page = 1) => {
  reportListLoading.value = true
  reportListError.value = ''
  try {
    const params = new URLSearchParams()
    params.set('page', page.toString())
    params.set('size', reportSize.value.toString())
    if (reportStatus.value === 'pending') {
      params.set('status', '0')
    } else if (reportStatus.value === 'handled') {
      params.set('status', '1')
    }
    const res = await apiFetch(`/api/reports?${params.toString()}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      reportListError.value = data.message || '获取举报记录失败。'
      return
    }
    reportList.value = data.data?.list || []
    reportTotal.value = data.data?.total ?? 0
    reportPage.value = data.data?.page ?? page
  } catch (err) {
    reportListError.value = '网络错误，无法获取举报记录。'
  } finally {
    reportListLoading.value = false
  }
}

const reportStatusLabel = (status) => {
  if (status === 1) return '已处理'
  return '待处理'
}

const reportTargetLabel = (targetType) => {
  if (targetType === 1) return '评论'
  if (targetType === 2) return '用户'
  return '内容'
}

const setReportFilter = (filter) => {
  reportStatus.value = filter
  reportPage.value = 1
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
  <div class="profile-page">
    <header class="profile-hero">
      <div>
        <span class="hero-tag">互动社交</span>
        <h1>消息与通知中心</h1>
        <p>点赞、评论、关注提醒与公告信息统一查看。</p>
      </div>
      <div class="profile-actions">
        <button class="ghost-btn" type="button" @click="router.push('/home')">返回主页</button>
        <button class="ghost-btn" type="button" @click="loadNotifications(notifPage)">刷新通知</button>
      </div>
    </header>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>消息通知</h2>
          <p>未读 {{ unreadCount }} 条</p>
        </div>
        <div class="discover-actions">
          <button class="ghost-btn" type="button" @click="markAllRead">全部已读</button>
        </div>
      </div>

      <div class="tabs">
        <button class="tab-btn" :class="{ active: notifFilter === 'all' }" @click="setNotifFilter('all')">全部</button>
        <button class="tab-btn" :class="{ active: notifFilter === 'unread' }" @click="setNotifFilter('unread')">未读</button>
        <button class="tab-btn" :class="{ active: notifFilter === 'read' }" @click="setNotifFilter('read')">已读</button>
      </div>

      <div v-if="notifLoading" class="feed-empty">通知加载中...</div>
      <div v-else-if="notifError" class="form-alert error">{{ notifError }}</div>
      <div v-else-if="notifications.length" class="notification-list">
        <div v-for="item in notifications" :key="item.id" class="notification-card" :class="{ unread: item.isRead === 0 }">
          <div>
            <span class="notif-type">{{ notificationLabel(item.type) }}</span>
            <h4>{{ item.title || '通知' }}</h4>
            <p>{{ item.content || '暂无内容' }}</p>
            <span class="notif-time">{{ item.createdAt || '-' }}</span>
          </div>
          <div class="feed-actions">
            <button class="ghost-btn" type="button" @click="goNotificationTarget(item)">查看</button>
            <button class="ghost-btn" type="button" :disabled="item.isRead === 1" @click="markRead(item.id)">
              {{ item.isRead === 1 ? '已读' : '标记已读' }}
            </button>
          </div>
        </div>
      </div>
      <div v-else class="feed-empty">暂无通知。</div>

      <div class="pager" v-if="notifTotal > notifSize">
        <button class="ghost-btn" type="button" :disabled="notifPage <= 1" @click="loadNotifications(notifPage - 1)">
          上一页
        </button>
        <span>{{ notifPage }} / {{ notifPages }}</span>
        <button
          class="ghost-btn"
          type="button"
          :disabled="notifPage >= notifPages"
          @click="loadNotifications(notifPage + 1)"
        >
          下一页
        </button>
      </div>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>系统公告</h2>
          <p>校园与平台公告集中展示。</p>
        </div>
        <button class="ghost-btn" type="button" @click="loadAnnouncements(announcementPage)">刷新公告</button>
      </div>

      <div v-if="announcementLoading" class="feed-empty">公告加载中...</div>
      <div v-else-if="announcementError" class="form-alert error">{{ announcementError }}</div>
      <div v-else-if="announcements.length" class="announcement-grid">
        <div v-for="item in announcements" :key="item.id" class="announcement-card">
          <h4>{{ item.title || '公告' }}</h4>
          <p>{{ item.content || '暂无内容' }}</p>
          <span class="notif-time">{{ item.createdAt || '-' }}</span>
          <div class="feed-actions">
            <button class="ghost-btn" type="button" @click="loadAnnouncementDetail(item.id)">查看详情</button>
          </div>
        </div>
      </div>
      <div v-else class="feed-empty">暂无公告。</div>

      <div v-if="activeAnnouncement" class="announcement-detail">
        <div>
          <span class="notif-type">公告详情</span>
          <h3>{{ activeAnnouncement.title }}</h3>
          <p>{{ activeAnnouncement.content }}</p>
          <span class="notif-time">{{ activeAnnouncement.createdAt || '-' }}</span>
        </div>
        <button class="ghost-btn" type="button" @click="activeAnnouncement = null">收起</button>
      </div>

      <div class="pager" v-if="announcementTotal > announcementSize">
        <button class="ghost-btn" type="button" :disabled="announcementPage <= 1" @click="loadAnnouncements(announcementPage - 1)">
          上一页
        </button>
        <span>{{ announcementPage }} / {{ announcementPages }}</span>
        <button
          class="ghost-btn"
          type="button"
          :disabled="announcementPage >= announcementPages"
          @click="loadAnnouncements(announcementPage + 1)"
        >
          下一页
        </button>
      </div>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>举报与反馈</h2>
          <p>举报违规内容并查看处理进度。</p>
        </div>
      </div>

      <form class="report-form" @submit.prevent="submitReport">
        <div class="grid-2">
          <label class="field">
            <span>举报对象</span>
            <select v-model.number="reportTargetType">
              <option :value="0">内容</option>
              <option :value="1">评论</option>
              <option :value="2">用户</option>
            </select>
          </label>
          <label class="field">
            <span>对象 ID</span>
            <input v-model="reportTargetId" type="number" placeholder="输入对象 ID" />
          </label>
        </div>
        <label class="field">
          <span>举报原因</span>
          <select v-model="reportReason">
            <option value="">请选择原因</option>
            <option v-for="item in reportReasons" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>
        <label class="field">
          <span>补充说明（可选）</span>
          <textarea v-model="reportDetail" rows="3" placeholder="可填写更多描述"></textarea>
        </label>
        <div class="profile-actions">
          <button class="primary-btn" type="submit" :disabled="reportLoading">
            {{ reportLoading ? '提交中...' : '提交举报' }}
          </button>
        </div>
        <p v-if="reportError" class="form-alert error">{{ reportError }}</p>
        <p v-if="reportSuccess" class="form-alert success">{{ reportSuccess }}</p>
      </form>

      <div class="profile-header">
        <div>
          <h3>我的举报记录</h3>
          <p>查看处理状态与结果。</p>
        </div>
      </div>
      <div class="tabs">
        <button class="tab-btn" :class="{ active: reportStatus === 'all' }" @click="setReportFilter('all')">全部</button>
        <button class="tab-btn" :class="{ active: reportStatus === 'pending' }" @click="setReportFilter('pending')">待处理</button>
        <button class="tab-btn" :class="{ active: reportStatus === 'handled' }" @click="setReportFilter('handled')">已处理</button>
      </div>

      <div v-if="reportListLoading" class="feed-empty">加载中...</div>
      <div v-else-if="reportListError" class="form-alert error">{{ reportListError }}</div>
      <div v-else-if="reportList.length" class="report-list">
        <div v-for="item in reportList" :key="item.id" class="report-card">
          <div>
            <span class="notif-type">{{ reportTargetLabel(item.targetType) }}举报</span>
            <h4>{{ item.reason || '举报' }}</h4>
            <p>{{ item.detail || '未填写补充说明' }}</p>
            <div class="feed-meta">
              <span>对象ID {{ item.targetId }}</span>
              <span>状态：{{ reportStatusLabel(item.status) }}</span>
              <span>提交于 {{ item.createdAt || '-' }}</span>
            </div>
            <p v-if="item.result" class="muted">处理结果：{{ item.result }}</p>
          </div>
        </div>
      </div>
      <div v-else class="feed-empty">暂无举报记录。</div>

      <div class="pager" v-if="reportTotal > reportSize">
        <button class="ghost-btn" type="button" :disabled="reportPage <= 1" @click="loadReportList(reportPage - 1)">
          上一页
        </button>
        <span>{{ reportPage }} / {{ reportPages }}</span>
        <button
          class="ghost-btn"
          type="button"
          :disabled="reportPage >= reportPages"
          @click="loadReportList(reportPage + 1)"
        >
          下一页
        </button>
      </div>
    </section>
  </div>
</template>
