<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const router = useRouter()

const tags = ref([])
const tagLoading = ref(false)
const tagError = ref('')
const newTag = ref({ name: '', type: 0, status: 1 })
const editingTagId = ref(null)
const editTag = ref({ name: '', type: 0, status: 1 })

const announcements = ref([])
const announcementLoading = ref(false)
const announcementError = ref('')
const announcementPage = ref(1)
const announcementSize = ref(6)
const announcementTotal = ref(0)
const newAnnouncement = ref({ title: '', content: '', status: 0 })
const editingAnnouncementId = ref(null)
const editAnnouncement = ref({ title: '', content: '', status: 0 })

const postFlagId = ref('')
const postPinned = ref(false)
const postFeatured = ref(false)
const postFlagLoading = ref(false)
const postFlagMessage = ref('')
const approvedPosts = ref([])
const approvedLoading = ref(false)
const approvedError = ref('')
const approvedPage = ref(1)
const approvedSize = ref(6)
const approvedTotal = ref(0)
const postSearch = ref('')
const userStatusId = ref('')
const userStatus = ref(1)
const userStatusLoading = ref(false)
const userStatusMessage = ref('')
const users = ref([])
const userLoading = ref(false)
const userError = ref('')
const userPage = ref(1)
const userSize = ref(6)
const userTotal = ref(0)
const userSearch = ref('')
const recommendConfig = ref({
  enableHot: true,
  enableFollow: true,
  enableTag: true,
  weightHot: 0.4,
  weightTime: 0.2,
  weightQuality: 0.2,
  weightTag: 0.1,
  weightFollow: 0.1,
})
const recommendLoading = ref(false)
const recommendMessage = ref('')

const announcementPages = computed(() => {
  const pages = Math.ceil(announcementTotal.value / announcementSize.value)
  return pages > 0 ? pages : 1
})
const approvedPages = computed(() => {
  const pages = Math.ceil(approvedTotal.value / approvedSize.value)
  return pages > 0 ? pages : 1
})

const filteredApprovedPosts = computed(() => {
  const keyword = postSearch.value.trim().toLowerCase()
  if (!keyword) return approvedPosts.value
  return approvedPosts.value.filter((item) => {
    const title = (item.title || '').toLowerCase()
    const content = (item.content || '').toLowerCase()
    const idText = String(item.id || '').toLowerCase()
    return title.includes(keyword) || content.includes(keyword) || idText.includes(keyword)
  })
})

const userPages = computed(() => {
  const pages = Math.ceil(userTotal.value / userSize.value)
  return pages > 0 ? pages : 1
})
const loadTags = async () => {
  tagLoading.value = true
  tagError.value = ''
  try {
    const res = await apiFetch('/api/admin/tags')
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagError.value = data.message || '获取标签失败。'
      return
    }
    tags.value = data.data || []
  } catch (err) {
    tagError.value = '网络错误，无法获取标签。'
  } finally {
    tagLoading.value = false
  }
}

const addTag = async () => {
  tagError.value = ''
  if (!newTag.value.name.trim()) {
    tagError.value = '请输入标签名称。'
    return
  }
  try {
    const res = await apiFetch('/api/admin/tags', {
      method: 'POST',
      body: JSON.stringify({
        name: newTag.value.name.trim(),
        type: Number(newTag.value.type),
        status: Number(newTag.value.status),
      }),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagError.value = data.message || '新增失败。'
      return
    }
    newTag.value = { name: '', type: 0, status: 1 }
    loadTags()
  } catch (err) {
    tagError.value = '网络错误，无法新增标签。'
  }
}

const startEditTag = (tag) => {
  editingTagId.value = tag.id
  editTag.value = { name: tag.name, type: tag.type ?? 0, status: tag.status ?? 1 }
}

const cancelEditTag = () => {
  editingTagId.value = null
  editTag.value = { name: '', type: 0, status: 1 }
}

const saveTag = async (id) => {
  try {
    const res = await apiFetch(`/api/admin/tags/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: editTag.value.name.trim(),
        type: Number(editTag.value.type),
        status: Number(editTag.value.status),
      }),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagError.value = data.message || '更新失败。'
      return
    }
    cancelEditTag()
    loadTags()
  } catch (err) {
    tagError.value = '网络错误，无法更新标签。'
  }
}

const deleteTag = async (id) => {
  if (!confirm('确定删除该标签吗？')) return
  try {
    const res = await apiFetch(`/api/admin/tags/${id}`, { method: 'DELETE' })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagError.value = data.message || '删除失败。'
      return
    }
    loadTags()
  } catch (err) {
    tagError.value = '网络错误，无法删除标签。'
  }
}

const loadAnnouncements = async (page = 1) => {
  announcementLoading.value = true
  announcementError.value = ''
  try {
    const res = await apiFetch(`/api/admin/announcements?page=${page}&size=${announcementSize.value}`)
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

const addAnnouncement = async () => {
  announcementError.value = ''
  if (!newAnnouncement.value.title.trim() || !newAnnouncement.value.content.trim()) {
    announcementError.value = '请输入公告标题与内容。'
    return
  }
  try {
    const res = await apiFetch('/api/admin/announcements', {
      method: 'POST',
      body: JSON.stringify({
        title: newAnnouncement.value.title.trim(),
        content: newAnnouncement.value.content.trim(),
        status: Number(newAnnouncement.value.status),
      }),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      announcementError.value = data.message || '发布失败。'
      return
    }
    newAnnouncement.value = { title: '', content: '', status: 0 }
    loadAnnouncements(1)
  } catch (err) {
    announcementError.value = '网络错误，无法发布公告。'
  }
}

const startEditAnnouncement = (item) => {
  editingAnnouncementId.value = item.id
  editAnnouncement.value = {
    title: item.title || '',
    content: item.content || '',
    status: item.status ?? 0,
  }
}

const cancelEditAnnouncement = () => {
  editingAnnouncementId.value = null
  editAnnouncement.value = { title: '', content: '', status: 0 }
}

const saveAnnouncement = async (id) => {
  try {
    const res = await apiFetch(`/api/admin/announcements/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        title: editAnnouncement.value.title.trim(),
        content: editAnnouncement.value.content.trim(),
        status: Number(editAnnouncement.value.status),
      }),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      announcementError.value = data.message || '更新失败。'
      return
    }
    cancelEditAnnouncement()
    loadAnnouncements(announcementPage.value)
  } catch (err) {
    announcementError.value = '网络错误，无法更新公告。'
  }
}

const deleteAnnouncement = async (id) => {
  if (!confirm('确定删除该公告吗？')) return
  try {
    const res = await apiFetch(`/api/admin/announcements/${id}`, { method: 'DELETE' })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      announcementError.value = data.message || '删除失败。'
      return
    }
    loadAnnouncements(announcementPage.value)
  } catch (err) {
    announcementError.value = '网络错误，无法删除公告。'
  }
}

const updateFlags = async () => {
  postFlagMessage.value = ''
  if (!postFlagId.value) {
    postFlagMessage.value = '请输入内容 ID。'
    return
  }
  postFlagLoading.value = true
  try {
    const res = await apiFetch(`/api/admin/posts/${postFlagId.value}/flags`, {
      method: 'PUT',
      body: JSON.stringify({
        pinned: postPinned.value ? 1 : 0,
        featured: postFeatured.value ? 1 : 0,
      }),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      postFlagMessage.value = data.message || '更新失败。'
      return
    }
    postFlagMessage.value = '已更新内容标记。'
  } catch (err) {
    postFlagMessage.value = '网络错误，无法更新。'
  } finally {
    postFlagLoading.value = false
  }
}
const loadApprovedPosts = async (page = 1) => {
  approvedLoading.value = true
  approvedError.value = ''
  try {
    const res = await apiFetch(`/api/admin/posts?page=${page}&size=${approvedSize.value}&status=1`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      approvedError.value = data.message || '获取内容失败。'
      return
    }
    approvedPosts.value = data.data?.list || []
    approvedTotal.value = data.data?.total ?? 0
    approvedPage.value = data.data?.page ?? page
  } catch (err) {
    approvedError.value = '网络错误，无法获取内容。'
  } finally {
    approvedLoading.value = false
  }
}

const selectPostForFlags = (post) => {
  if (!post) return
  postFlagId.value = post.id
  postFlagMessage.value = `已选择内容 #${post.id}`
}

const updateUserStatus = async () => {
  userStatusMessage.value = ''
  if (!userStatusId.value) {
    userStatusMessage.value = '请输入用户 ID。'
    return
  }
  userStatusLoading.value = true
  try {
    const res = await apiFetch(`/api/admin/users/${userStatusId.value}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status: Number(userStatus.value) }),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      userStatusMessage.value = data.message || '更新失败。'
      return
    }
    userStatusMessage.value = '用户状态已更新。'
    loadUsers(userPage.value)
  } catch (err) {
    userStatusMessage.value = '网络错误，无法更新。'
  } finally {
    userStatusLoading.value = false
  }
}
const loadUsers = async (page = 1) => {
  userLoading.value = true
  userError.value = ''
  try {
    const params = new URLSearchParams()
    params.set('page', page)
    params.set('size', userSize.value)
    if (userSearch.value.trim()) {
      params.set('keyword', userSearch.value.trim())
    }
    const res = await apiFetch(`/api/admin/users?${params.toString()}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      userError.value = data.message || '获取用户失败。'
      return
    }
    users.value = data.data?.list || []
    userTotal.value = data.data?.total ?? 0
    userPage.value = data.data?.page ?? page
  } catch (err) {
    userError.value = '网络错误，无法获取用户。'
  } finally {
    userLoading.value = false
  }
}

const selectUserForStatus = (user) => {
  if (!user) return
  userStatusId.value = user.id
  userStatus.value = user.status ?? 0
  userStatusMessage.value = `已选择用户 #${user.id}`
}

const loadRecommendConfig = async () => {
  recommendLoading.value = true
  recommendMessage.value = ''
  try {
    const res = await apiFetch('/api/admin/recommend/config')
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      recommendMessage.value = data.message || '获取配置失败。'
      return
    }
    recommendConfig.value = data.data || recommendConfig.value
  } catch (err) {
    recommendMessage.value = '网络错误，无法获取配置。'
  } finally {
    recommendLoading.value = false
  }
}

const saveRecommendConfig = async () => {
  recommendMessage.value = ''
  recommendLoading.value = true
  try {
    const res = await apiFetch('/api/admin/recommend/config', {
      method: 'PUT',
      body: JSON.stringify(recommendConfig.value),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      recommendMessage.value = data.message || '更新失败。'
      return
    }
    recommendConfig.value = data.data || recommendConfig.value
    recommendMessage.value = '已保存推荐配置。'
  } catch (err) {
    recommendMessage.value = '网络错误，无法保存。'
  } finally {
    recommendLoading.value = false
  }
}

onMounted(() => {
  loadTags()
  loadAnnouncements()
  loadRecommendConfig()
  loadApprovedPosts()
  loadUsers()
})
</script>

<template>
  <div class="profile-page">
    <header class="profile-hero">
      <div>
        <span class="hero-tag">运营配置</span>
        <h1>平台运营与策略设置</h1>
        <p>维护话题标签、公告与推荐策略。</p>
      </div>
      <div class="profile-actions">
        <button class="ghost-btn" type="button" @click="router.push('/admin')">返回后台</button>
        <button class="ghost-btn" type="button" @click="loadTags">刷新标签</button>
      </div>
    </header>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>置顶/精选</h2>
          <p>选择内容并设置置顶与精选标记。</p>
        </div>
      </div>
      <div class="report-form">
        <label class="field">
          <span>内容 ID</span>
          <input v-model="postFlagId" type="number" placeholder="输入内容ID" />
        </label>
        <label class="field">
          <span>搜索内容</span>
          <input v-model="postSearch" type="text" placeholder="输入标题/内容关键词或ID" />
        </label>
        <div v-if="approvedLoading" class="feed-empty">加载内容中...</div>
        <div v-else-if="approvedError" class="form-alert error">{{ approvedError }}</div>
        <div v-else-if="filteredApprovedPosts.length" class="feed-grid">
          <div v-for="post in filteredApprovedPosts" :key="post.id" class="feed-item">
            <div class="feed-item-top">
              <span class="feed-label">内容 #{{ post.id }}</span>
              <span class="status-pill approved">已通过</span>
            </div>
            <h4>{{ post.title || '未命名内容' }}</h4>
            <p>{{ post.content?.slice(0, 80) || '暂无内容' }}</p>
            <div class="feed-meta">
              <span>发布者 {{ post.userId }}</span>
              <span>{{ post.createdAt || '-' }}</span>
            </div>
            <div class="feed-actions">
              <button class="ghost-btn" type="button" @click="selectPostForFlags(post)">选择该内容</button>
              <button class="ghost-btn" type="button" @click="router.push(`/posts/${post.id}`)">查看详情</button>
            </div>
          </div>
        </div>
        <div v-else class="feed-empty">暂无可置顶内容。</div>
        <div class="pager" v-if="approvedTotal > approvedSize">
          <button class="ghost-btn" type="button" :disabled="approvedPage <= 1" @click="loadApprovedPosts(approvedPage - 1)">
            上一页
          </button>
          <span>{{ approvedPage }} / {{ approvedPages }}</span>
          <button
            class="ghost-btn"
            type="button"
            :disabled="approvedPage >= approvedPages"
            @click="loadApprovedPosts(approvedPage + 1)"
          >
            下一页
          </button>
        </div>
        <div class="grid-2">
          <label class="field">
            <span>置顶</span>
            <select v-model="postPinned">
              <option :value="false">否</option>
              <option :value="true">是</option>
            </select>
          </label>
          <label class="field">
            <span>精选</span>
            <select v-model="postFeatured">
              <option :value="false">否</option>
              <option :value="true">是</option>
            </select>
          </label>
        </div>
        <div class="profile-actions">
          <button class="primary-btn" type="button" :disabled="postFlagLoading" @click="updateFlags">
            {{ postFlagLoading ? '提交中...' : '更新标记' }}
          </button>
        </div>
        <p v-if="postFlagMessage" class="form-alert success">{{ postFlagMessage }}</p>
      </div>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>黑名单</h2>
          <p>搜索用户并进行封禁/解禁。</p>
        </div>
      </div>
      <div class="report-form">
        <label class="field">
          <span>用户 ID</span>
          <input v-model="userStatusId" type="number" placeholder="输入用户ID" />
        </label>
        <label class="field">
          <span>搜索用户</span>
          <input v-model="userSearch" type="text" placeholder="输入用户名/邮箱/手机号" />
        </label>
        <div class="feed-actions">
          <button class="ghost-btn" type="button" @click="loadUsers(1)">搜索</button>
          <button class="ghost-btn" type="button" @click="loadUsers(userPage)">刷新</button>
        </div>
        <div v-if="userLoading" class="feed-empty">加载用户中...</div>
        <div v-else-if="userError" class="form-alert error">{{ userError }}</div>
        <div v-else-if="users.length" class="feed-grid">
          <div v-for="user in users" :key="user.id" class="feed-item">
            <div class="feed-item-top">
              <span class="feed-label">用户 #{{ user.id }}</span>
              <span class="status-pill" :class="user.status === 1 ? 'rejected' : 'approved'">
                {{ user.status === 1 ? '封禁' : '正常' }}
              </span>
            </div>
            <h4>{{ user.username || '未命名用户' }}</h4>
            <div class="feed-meta">
              <span>{{ user.email || user.phone || '-' }}</span>
              <span>{{ user.school || '-' }}</span>
              <span>{{ user.college || '-' }}</span>
            </div>
            <div class="feed-actions">
              <button class="ghost-btn" type="button" @click="selectUserForStatus(user)">选择该用户</button>
            </div>
          </div>
        </div>
        <div v-else class="feed-empty">暂无用户。</div>
        <div class="pager" v-if="userTotal > userSize">
          <button class="ghost-btn" type="button" :disabled="userPage <= 1" @click="loadUsers(userPage - 1)">
            上一页
          </button>
          <span>{{ userPage }} / {{ userPages }}</span>
          <button
            class="ghost-btn"
            type="button"
            :disabled="userPage >= userPages"
            @click="loadUsers(userPage + 1)"
          >
            下一页
          </button>
        </div>
        <label class="field">
          <span>状态</span>
          <select v-model.number="userStatus">
            <option :value="1">封禁</option>
            <option :value="0">解禁</option>
          </select>
        </label>
        <div class="profile-actions">
          <button class="primary-btn" type="button" :disabled="userStatusLoading" @click="updateUserStatus">
            {{ userStatusLoading ? '提交中...' : '更新状态' }}
          </button>
        </div>
        <p v-if="userStatusMessage" class="form-alert success">{{ userStatusMessage }}</p>
      </div>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>推荐策略</h2>
          <p>调整推荐权重与开关。</p>
        </div>
      </div>
      <div class="report-form">
        <div class="grid-2">
          <label class="field">
            <span>热门开关</span>
            <select v-model="recommendConfig.enableHot">
              <option :value="true">启用</option>
              <option :value="false">关闭</option>
            </select>
          </label>
          <label class="field">
            <span>关注开关</span>
            <select v-model="recommendConfig.enableFollow">
              <option :value="true">启用</option>
              <option :value="false">关闭</option>
            </select>
          </label>
        </div>
        <div class="grid-2">
          <label class="field">
            <span>兴趣开关</span>
            <select v-model="recommendConfig.enableTag">
              <option :value="true">启用</option>
              <option :value="false">关闭</option>
            </select>
          </label>
          <label class="field">
            <span>热度权重</span>
            <input v-model.number="recommendConfig.weightHot" type="number" step="0.1" min="0" />
          </label>
        </div>
        <div class="grid-2">
          <label class="field">
            <span>时效权重</span>
            <input v-model.number="recommendConfig.weightTime" type="number" step="0.1" min="0" />
          </label>
          <label class="field">
            <span>质量权重</span>
            <input v-model.number="recommendConfig.weightQuality" type="number" step="0.1" min="0" />
          </label>
        </div>
        <div class="grid-2">
          <label class="field">
            <span>兴趣权重</span>
            <input v-model.number="recommendConfig.weightTag" type="number" step="0.1" min="0" />
          </label>
          <label class="field">
            <span>关注权重</span>
            <input v-model.number="recommendConfig.weightFollow" type="number" step="0.1" min="0" />
          </label>
        </div>
        <div class="profile-actions">
          <button class="primary-btn" type="button" :disabled="recommendLoading" @click="saveRecommendConfig">
            {{ recommendLoading ? '保存中...' : '保存配置' }}
          </button>
        </div>
        <p v-if="recommendMessage" class="form-alert success">{{ recommendMessage }}</p>
      </div>
    </section>
  </div>
</template>



