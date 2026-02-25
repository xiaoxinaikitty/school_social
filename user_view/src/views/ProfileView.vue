<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const router = useRouter()
const user = ref(null)

const profile = ref({
  id: null,
  username: '',
  email: '',
  phone: '',
  avatarUrl: '',
  gender: '',
  birthday: '',
  school: '',
  college: '',
  grade: '',
  bio: '',
  status: 0,
  lastLoginAt: '',
  createdAt: '',
})

const loadingProfile = ref(false)
const savingProfile = ref(false)
const profileError = ref('')
const profileSuccess = ref('')

const availableTags = ref([])
const selectedTagIds = ref([])
const loadingTags = ref(false)
const tagsError = ref('')

const activeTab = ref('posts')
const postsPage = ref(1)
const favoritesPage = ref(1)
const commentsPage = ref(1)
const pageSize = 6

const postsData = ref({ page: 1, size: pageSize, total: 0, list: [] })
const favoritesData = ref({ page: 1, size: pageSize, total: 0, list: [] })
const commentsData = ref({ page: 1, size: pageSize, total: 0, list: [] })
const loadingPosts = ref(false)
const loadingFavorites = ref(false)
const loadingComments = ref(false)
const feedError = ref('')

const followStats = ref({ followingCount: 0, followerCount: 0 })
const socialTab = ref('following')
const followingData = ref({ page: 1, size: 6, total: 0, list: [] })
const followersData = ref({ page: 1, size: 6, total: 0, list: [] })
const followingPage = ref(1)
const followersPage = ref(1)
const loadingFollowing = ref(false)
const loadingFollowers = ref(false)
const socialError = ref('')

const statusMap = (status) => {
  switch (status) {
    case 0:
      return { label: '待审', tone: 'pending' }
    case 1:
      return { label: '通过', tone: 'approved' }
    case 2:
      return { label: '驳回', tone: 'rejected' }
    case 3:
      return { label: '草稿', tone: 'draft' }
    default:
      return { label: '未知', tone: 'unknown' }
  }
}

const displayName = computed(() => {
  if (profile.value?.username) return profile.value.username
  return '同学'
})

const syncProfile = (data) => {
  profile.value = {
    id: data.id,
    username: data.username || '',
    email: data.email || '',
    phone: data.phone || '',
    avatarUrl: data.avatarUrl || '',
    gender: data.gender === null || data.gender === undefined ? '' : String(data.gender),
    birthday: data.birthday || '',
    school: data.school || '',
    college: data.college || '',
    grade: data.grade || '',
    bio: data.bio || '',
    status: data.status ?? 0,
    lastLoginAt: data.lastLoginAt || '',
    createdAt: data.createdAt || '',
  }
}

const loadProfile = async () => {
  if (!user.value?.id) {
    router.push('/login')
    return
  }
  loadingProfile.value = true
  profileError.value = ''
  try {
    const res = await apiFetch(`/api/users/${user.value.id}`)
    if (res.status === 401) {
      profileError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      profileError.value = data.message || '获取资料失败。'
      return
    }
    syncProfile(data.data)
    localStorage.setItem('auth_user', JSON.stringify(data.data))
  } catch (err) {
    profileError.value = '网络错误，无法获取资料。'
  } finally {
    loadingProfile.value = false
  }
}

const saveProfile = async () => {
  if (!profile.value.id) return
  savingProfile.value = true
  profileError.value = ''
  profileSuccess.value = ''
  try {
    const payload = {
      email: profile.value.email?.trim() || null,
      phone: profile.value.phone?.trim() || null,
      avatarUrl: profile.value.avatarUrl?.trim() || null,
      gender: profile.value.gender === '' ? null : Number(profile.value.gender),
      birthday: profile.value.birthday || null,
      school: profile.value.school?.trim() || null,
      college: profile.value.college?.trim() || null,
      grade: profile.value.grade?.trim() || null,
      bio: profile.value.bio?.trim() || null,
    }
    const res = await apiFetch(`/api/users/${profile.value.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    if (res.status === 401) {
      profileError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      profileError.value = data.message || '更新失败，请检查输入。'
      return
    }
    syncProfile(data.data)
    localStorage.setItem('auth_user', JSON.stringify(data.data))
    profileSuccess.value = '资料已更新。'
  } catch (err) {
    profileError.value = '网络错误，无法更新资料。'
  } finally {
    savingProfile.value = false
  }
}

const loadUserTags = async () => {
  if (!user.value?.id) return
  loadingTags.value = true
  tagsError.value = ''
  try {
    const res = await apiFetch(`/api/users/${user.value.id}/tags`)
    if (res.status === 401) {
      tagsError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagsError.value = data.message || '获取标签失败。'
      return
    }
    selectedTagIds.value = (data.data || []).map((item) => item.tagId)
  } catch (err) {
    tagsError.value = '网络错误，无法获取标签。'
  } finally {
    loadingTags.value = false
  }
}

const loadAvailableTags = async () => {
  loadingTags.value = true
  tagsError.value = ''
  try {
    const res = await apiFetch('/api/tags')
    if (res.status === 401) {
      tagsError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagsError.value = data.message || '获取标签列表失败。'
      return
    }
    availableTags.value = data.data || []
  } catch (err) {
    tagsError.value = '网络错误，无法获取标签列表。'
  } finally {
    loadingTags.value = false
  }
}

const saveUserTags = async () => {
  if (!user.value?.id) return
  loadingTags.value = true
  tagsError.value = ''
  try {
    const res = await apiFetch(`/api/users/${user.value.id}/tags`, {
      method: 'PUT',
      body: JSON.stringify({ tagIds: selectedTagIds.value }),
    })
    if (res.status === 401) {
      tagsError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagsError.value = data.message || '更新标签失败。'
      return
    }
  } catch (err) {
    tagsError.value = '网络错误，无法更新标签。'
  } finally {
    loadingTags.value = false
  }
}

const loadPosts = async (page = 1) => {
  if (!user.value?.id) return
  loadingPosts.value = true
  feedError.value = ''
  try {
    const res = await apiFetch(`/api/users/${user.value.id}/posts?page=${page}&size=${pageSize}`)
    if (res.status === 401) {
      feedError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      feedError.value = data.message || '获取发布失败。'
      return
    }
    postsData.value = data.data
    postsPage.value = data.data.page
  } catch (err) {
      feedError.value = '网络错误，无法获取发布。'
    } finally {
      loadingPosts.value = false
    }
  }

const viewPost = (postId) => {
  if (!postId) return
  router.push(`/posts/${postId}`)
}

const editPost = (postId) => {
  if (!postId) return
  router.push(`/posts/${postId}/edit`)
}

const deletePost = async (postId) => {
  if (!postId) return
  if (!confirm('确定要删除这条内容吗？')) return
  try {
    const res = await apiFetch(`/api/posts/${postId}`, { method: 'DELETE' })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      feedError.value = data.message || '删除失败。'
      return
    }
    loadPosts(postsPage.value || 1)
  } catch (err) {
    feedError.value = '网络错误，无法删除。'
  }
}

const loadFavorites = async (page = 1) => {
  if (!user.value?.id) return
  loadingFavorites.value = true
  feedError.value = ''
  try {
    const res = await apiFetch(`/api/users/${user.value.id}/favorites?page=${page}&size=${pageSize}`)
    if (res.status === 401) {
      feedError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      feedError.value = data.message || '获取收藏失败。'
      return
    }
    favoritesData.value = data.data
    favoritesPage.value = data.data.page
  } catch (err) {
    feedError.value = '网络错误，无法获取收藏。'
  } finally {
    loadingFavorites.value = false
  }
}

const loadComments = async (page = 1) => {
  if (!user.value?.id) return
  loadingComments.value = true
  feedError.value = ''
  try {
    const res = await apiFetch(`/api/users/${user.value.id}/comments?page=${page}&size=${pageSize}`)
    if (res.status === 401) {
      feedError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      feedError.value = data.message || '获取评论失败。'
      return
    }
    commentsData.value = data.data
    commentsPage.value = data.data.page
  } catch (err) {
    feedError.value = '网络错误，无法获取评论。'
  } finally {
    loadingComments.value = false
  }
}

const loadFollowStats = async () => {
  socialError.value = ''
  try {
    const res = await apiFetch('/api/follows/stats')
    if (res.status === 401) {
      socialError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      socialError.value = data.message || '获取关注统计失败。'
      return
    }
    followStats.value = data.data || { followingCount: 0, followerCount: 0 }
  } catch (err) {
    socialError.value = '网络错误，无法获取关注统计。'
  }
}

const loadFollowing = async (page = 1) => {
  loadingFollowing.value = true
  socialError.value = ''
  try {
    const res = await apiFetch(`/api/follows/following?page=${page}&size=6`)
    if (res.status === 401) {
      socialError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      socialError.value = data.message || '获取关注列表失败。'
      return
    }
    followingData.value = data.data
    followingPage.value = data.data.page
  } catch (err) {
    socialError.value = '网络错误，无法获取关注列表。'
  } finally {
    loadingFollowing.value = false
  }
}

const loadFollowers = async (page = 1) => {
  loadingFollowers.value = true
  socialError.value = ''
  try {
    const res = await apiFetch(`/api/follows/followers?page=${page}&size=6`)
    if (res.status === 401) {
      socialError.value = '登录已过期，请重新登录。'
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      socialError.value = data.message || '获取粉丝列表失败。'
      return
    }
    followersData.value = data.data
    followersPage.value = data.data.page
  } catch (err) {
    socialError.value = '网络错误，无法获取粉丝列表。'
  } finally {
    loadingFollowers.value = false
  }
}

const followUser = async (userId) => {
  if (!userId) return
  socialError.value = ''
  try {
    const res = await apiFetch(`/api/follows/${userId}`, { method: 'POST' })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      socialError.value = data.message || '关注失败。'
      return
    }
    await Promise.all([loadFollowStats(), loadFollowing(followingPage.value || 1), loadFollowers(followersPage.value || 1)])
  } catch (err) {
    socialError.value = '网络错误，关注失败。'
  }
}

const unfollowUser = async (userId) => {
  if (!userId) return
  socialError.value = ''
  try {
    const res = await apiFetch(`/api/follows/${userId}`, { method: 'DELETE' })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      socialError.value = data.message || '取消关注失败。'
      return
    }
    await Promise.all([loadFollowStats(), loadFollowing(followingPage.value || 1), loadFollowers(followersPage.value || 1)])
  } catch (err) {
    socialError.value = '网络错误，取消关注失败。'
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'posts') loadPosts(postsPage.value || 1)
  if (tab === 'favorites') loadFavorites(favoritesPage.value || 1)
  if (tab === 'comments') loadComments(commentsPage.value || 1)
}

onMounted(() => {
  try {
    const savedUser = localStorage.getItem('auth_user')
    if (savedUser) {
      user.value = JSON.parse(savedUser)
    }
  } catch (err) {
    user.value = null
  }
  loadProfile()
  loadAvailableTags()
  loadUserTags()
  loadPosts()
  loadFollowStats()
  loadFollowing()
  loadFollowers()
})
</script>

<template>
  <div class="profile-page">
    <header class="profile-hero">
      <div>
        <span class="hero-tag">个人中心</span>
        <h1>你好，{{ displayName }}。</h1>
        <p>完善资料后可获得更精准的校园推荐。</p>
      </div>
      <div class="profile-actions">
        <button class="ghost-btn" type="button" @click="router.push('/home')">返回主页</button>
        <button class="ghost-btn" type="button" @click="loadProfile" :disabled="loadingProfile">
          {{ loadingProfile ? '刷新中...' : '刷新资料' }}
        </button>
      </div>
    </header>

    <section class="profile-section">
      <div class="profile-content">
        <div class="profile-card">
          <div class="avatar-block">
            <div class="avatar">
              <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="头像" />
              <span v-else>{{ displayName.slice(0, 1) }}</span>
            </div>
            <div>
              <p class="profile-name">{{ displayName }}</p>
              <p class="profile-meta" v-if="profile.createdAt">注册时间：{{ profile.createdAt }}</p>
              <p class="profile-meta">账号状态：{{ profile.status === 0 ? '正常' : '受限' }}</p>
            </div>
          </div>
          <div class="profile-stats">
            <div>
              <span>最近登录</span>
              <strong>{{ profile.lastLoginAt || '暂无' }}</strong>
            </div>
            <div>
              <span>资料完整度</span>
              <strong>持续完善</strong>
            </div>
          </div>
        </div>

        <form class="profile-form" @submit.prevent="saveProfile">
          <div class="grid-2">
            <label class="field">
              <span>用户名</span>
              <input v-model="profile.username" type="text" disabled />
            </label>
            <label class="field">
              <span>邮箱</span>
              <input v-model="profile.email" type="email" placeholder="name@school.edu" />
            </label>
          </div>
          <div class="grid-2">
            <label class="field">
              <span>手机号</span>
              <input v-model="profile.phone" type="text" placeholder="8-20 位数字" />
            </label>
            <label class="field">
              <span>头像地址</span>
              <input v-model="profile.avatarUrl" type="text" placeholder="https://..." />
            </label>
          </div>
          <div class="grid-2">
            <label class="field">
              <span>性别</span>
              <select v-model="profile.gender">
                <option value="">未设置</option>
                <option value="0">保密</option>
                <option value="1">男</option>
                <option value="2">女</option>
              </select>
            </label>
            <label class="field">
              <span>生日</span>
              <input v-model="profile.birthday" type="date" />
            </label>
          </div>
          <div class="grid-2">
            <label class="field">
              <span>学校</span>
              <input v-model="profile.school" type="text" placeholder="学校名称" />
            </label>
            <label class="field">
              <span>学院</span>
              <input v-model="profile.college" type="text" placeholder="学院名称" />
            </label>
          </div>
          <div class="grid-2">
            <label class="field">
              <span>年级</span>
              <input v-model="profile.grade" type="text" placeholder="如 2024" />
            </label>
            <label class="field">
              <span>个性签名</span>
              <input v-model="profile.bio" type="text" placeholder="一句话介绍自己" />
            </label>
          </div>

          <div class="profile-actions">
            <button class="primary-btn" type="submit" :disabled="savingProfile">
              {{ savingProfile ? '保存中...' : '保存资料' }}
            </button>
          </div>

          <p v-if="profileError" class="form-alert error">{{ profileError }}</p>
          <p v-if="profileSuccess" class="form-alert success">{{ profileSuccess }}</p>
        </form>
      </div>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>兴趣标签</h2>
          <p>选择感兴趣的内容标签，推荐更懂你。</p>
        </div>
        <button class="ghost-btn" type="button" @click="saveUserTags" :disabled="loadingTags">
          {{ loadingTags ? '保存中...' : '保存标签' }}
        </button>
      </div>
      <div class="tag-grid">
        <label
          v-for="tag in availableTags"
          :key="tag.id"
          class="tag-pill"
          :class="{ active: selectedTagIds.includes(tag.id) }"
        >
          <input
            v-model="selectedTagIds"
            type="checkbox"
            :value="tag.id"
            class="tag-check"
          />
          {{ tag.name }}
        </label>
      </div>
      <div v-if="!loadingTags && availableTags.length === 0 && !tagsError" class="feed-empty">
        标签库为空，请先在后端配置 tags 数据。
      </div>
      <p v-if="tagsError" class="form-alert error">{{ tagsError }}</p>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>我的内容</h2>
          <p>查看你的发布、收藏与评论记录。</p>
        </div>
      </div>
      <div class="tabs">
        <button class="tab-btn" :class="{ active: activeTab === 'posts' }" @click="switchTab('posts')">
          我的发布
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'favorites' }" @click="switchTab('favorites')">
          我的收藏
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'comments' }" @click="switchTab('comments')">
          我的评论
        </button>
      </div>

      <div v-if="feedError" class="form-alert error">{{ feedError }}</div>

      <div v-if="activeTab === 'posts'" class="feed-list">
        <div v-if="loadingPosts" class="feed-empty">加载中...</div>
        <div v-else-if="!postsData.list || postsData.list.length === 0" class="feed-empty">暂无发布内容。</div>
        <div v-else class="feed-grid">
          <div v-for="post in postsData.list" :key="post.id" class="feed-item">
            <h4>{{ post.title || '未命名内容' }}</h4>
            <p>{{ post.content?.slice(0, 60) || '暂无内容' }}</p>
            <span>发布于：{{ post.createdAt }}</span>
            <span class="status-pill" :class="statusMap(post.status).tone">
              {{ statusMap(post.status).label }}
            </span>
            <div class="feed-actions">
              <button class="ghost-btn" type="button" @click="viewPost(post.id)">查看</button>
              <button class="ghost-btn" type="button" @click="editPost(post.id)">编辑</button>
              <button class="ghost-btn danger" type="button" @click="deletePost(post.id)">删除</button>
            </div>
          </div>
        </div>
        <div class="pager" v-if="postsData.total > pageSize">
          <button class="ghost-btn" type="button" :disabled="postsPage <= 1" @click="loadPosts(postsPage - 1)">
            上一页
          </button>
          <span>{{ postsPage }} / {{ Math.ceil(postsData.total / pageSize) }}</span>
          <button
            class="ghost-btn"
            type="button"
            :disabled="postsPage >= Math.ceil(postsData.total / pageSize)"
            @click="loadPosts(postsPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-if="activeTab === 'favorites'" class="feed-list">
        <div v-if="loadingFavorites" class="feed-empty">加载中...</div>
        <div v-else-if="!favoritesData.list || favoritesData.list.length === 0" class="feed-empty">暂无收藏内容。</div>
        <div v-else class="feed-grid">
          <div v-for="post in favoritesData.list" :key="post.id" class="feed-item">
            <h4>{{ post.title || '未命名内容' }}</h4>
            <p>{{ post.content?.slice(0, 60) || '暂无内容' }}</p>
            <span>收藏于：{{ post.createdAt }}</span>
            <div class="feed-actions">
              <button class="ghost-btn" type="button" @click="viewPost(post.id)">查看</button>
            </div>
          </div>
        </div>
        <div class="pager" v-if="favoritesData.total > pageSize">
          <button
            class="ghost-btn"
            type="button"
            :disabled="favoritesPage <= 1"
            @click="loadFavorites(favoritesPage - 1)"
          >
            上一页
          </button>
          <span>{{ favoritesPage }} / {{ Math.ceil(favoritesData.total / pageSize) }}</span>
          <button
            class="ghost-btn"
            type="button"
            :disabled="favoritesPage >= Math.ceil(favoritesData.total / pageSize)"
            @click="loadFavorites(favoritesPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-if="activeTab === 'comments'" class="feed-list">
        <div v-if="loadingComments" class="feed-empty">加载中...</div>
        <div v-else-if="!commentsData.list || commentsData.list.length === 0" class="feed-empty">暂无评论记录。</div>
        <div v-else class="feed-grid">
          <div v-for="comment in commentsData.list" :key="comment.id" class="feed-item">
            <h4>评论内容</h4>
            <p>{{ comment.content || '暂无内容' }}</p>
            <span>评论于：{{ comment.createdAt }}</span>
          </div>
        </div>
        <div class="pager" v-if="commentsData.total > pageSize">
          <button
            class="ghost-btn"
            type="button"
            :disabled="commentsPage <= 1"
            @click="loadComments(commentsPage - 1)"
          >
            上一页
          </button>
          <span>{{ commentsPage }} / {{ Math.ceil(commentsData.total / pageSize) }}</span>
          <button
            class="ghost-btn"
            type="button"
            :disabled="commentsPage >= Math.ceil(commentsData.total / pageSize)"
            @click="loadComments(commentsPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>关注与粉丝</h2>
          <p>维护你的社交关系与关注动态。</p>
        </div>
      </div>
      <div class="social-stats">
        <div>
          <span>关注</span>
          <strong>{{ followStats.followingCount }}</strong>
        </div>
        <div>
          <span>粉丝</span>
          <strong>{{ followStats.followerCount }}</strong>
        </div>
      </div>
      <div class="tabs">
        <button class="tab-btn" :class="{ active: socialTab === 'following' }" @click="socialTab = 'following'">
          我关注的
        </button>
        <button class="tab-btn" :class="{ active: socialTab === 'followers' }" @click="socialTab = 'followers'">
          我的粉丝
        </button>
      </div>

      <p v-if="socialError" class="form-alert error">{{ socialError }}</p>

      <div v-if="socialTab === 'following'" class="feed-list">
        <div v-if="loadingFollowing" class="feed-empty">加载中...</div>
        <div v-else-if="!followingData.list || followingData.list.length === 0" class="feed-empty">暂无关注。</div>
        <div v-else class="feed-grid">
          <div v-for="userItem in followingData.list" :key="userItem.id" class="feed-item">
            <h4>{{ userItem.username || '未命名用户' }}</h4>
            <p>{{ userItem.school || '未填写学校' }}</p>
            <span>{{ userItem.college || '未填写学院' }}</span>
            <div class="feed-actions">
              <button class="ghost-btn danger" type="button" @click="unfollowUser(userItem.id)">取消关注</button>
            </div>
          </div>
        </div>
        <div class="pager" v-if="followingData.total > 6">
          <button class="ghost-btn" type="button" :disabled="followingPage <= 1" @click="loadFollowing(followingPage - 1)">
            上一页
          </button>
          <span>{{ followingPage }} / {{ Math.ceil(followingData.total / 6) }}</span>
          <button
            class="ghost-btn"
            type="button"
            :disabled="followingPage >= Math.ceil(followingData.total / 6)"
            @click="loadFollowing(followingPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-if="socialTab === 'followers'" class="feed-list">
        <div v-if="loadingFollowers" class="feed-empty">加载中...</div>
        <div v-else-if="!followersData.list || followersData.list.length === 0" class="feed-empty">暂无粉丝。</div>
        <div v-else class="feed-grid">
          <div v-for="userItem in followersData.list" :key="userItem.id" class="feed-item">
            <h4>{{ userItem.username || '未命名用户' }}</h4>
            <p>{{ userItem.school || '未填写学校' }}</p>
            <span>{{ userItem.college || '未填写学院' }}</span>
            <div class="feed-actions">
              <button class="ghost-btn" type="button" @click="followUser(userItem.id)">关注</button>
            </div>
          </div>
        </div>
        <div class="pager" v-if="followersData.total > 6">
          <button class="ghost-btn" type="button" :disabled="followersPage <= 1" @click="loadFollowers(followersPage - 1)">
            上一页
          </button>
          <span>{{ followersPage }} / {{ Math.ceil(followersData.total / 6) }}</span>
          <button
            class="ghost-btn"
            type="button"
            :disabled="followersPage >= Math.ceil(followersData.total / 6)"
            @click="loadFollowers(followersPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </section>
  </div>
</template>
