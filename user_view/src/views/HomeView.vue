<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const user = ref(null)
const role = ref('user')
const router = useRouter()
const discoverRef = ref(null)

const displayName = computed(() => {
  if (user.value?.username) return user.value.username
  return role.value === 'admin' ? '管理员' : '同学'
})

const roleLabel = computed(() => (role.value === 'admin' ? '管理员' : '学生'))

const feedType = ref('recommend')
const feeds = ref([])
const feedLoading = ref(false)
const feedError = ref('')
const page = ref(1)
const size = ref(9)
const total = ref(0)

const tags = ref([])
const tagLoading = ref(false)
const tagError = ref('')
const activeTagId = ref(null)

const searchKeyword = ref('')
const searchTagId = ref('')

const unreadCount = ref(0)

const totalPages = computed(() => {
  const pages = Math.ceil(total.value / size.value)
  return pages > 0 ? pages : 1
})

const feedTitle = computed(() => {
  switch (feedType.value) {
    case 'follow':
      return '关注流'
    case 'hot':
      return '热门内容'
    case 'latest':
      return '最新内容'
    case 'topic':
      return '话题内容'
    case 'search':
      return '搜索结果'
    default:
      return '推荐流'
  }
})

const emptyHint = computed(() => {
  switch (feedType.value) {
    case 'follow':
      return '还没有关注任何人，去发现更多同学吧。'
    case 'topic':
      return '该话题下暂无内容。'
    case 'search':
      return '没有找到匹配的内容。'
    case 'hot':
      return '热门内容暂未生成。'
    case 'latest':
      return '暂无最新内容。'
    default:
      return '暂无推荐内容。'
  }
})

const activeTagName = computed(() => {
  const match = tags.value.find((item) => item.id === activeTagId.value)
  return match ? match.name : '话题'
})

const goDiscover = () => {
  discoverRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const logout = async () => {
  try {
    await apiFetch('/api/auth/logout', { method: 'POST' })
  } catch (err) {
    // Ignore network errors on logout
  } finally {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
    localStorage.removeItem('auth_role')
    router.push('/login')
  }
}

const normalizeStatus = (status) => {
  if (status === 0) return { label: '待审', className: 'pending' }
  if (status === 1) return { label: '通过', className: 'approved' }
  if (status === 2) return { label: '驳回', className: 'rejected' }
  return { label: '草稿', className: 'draft' }
}

const formatSnippet = (text) => {
  if (!text) return '暂无内容'
  const clean = text.replace(/\s+/g, ' ').trim()
  if (clean.length <= 90) return clean
  return `${clean.slice(0, 90)}...`
}

const loadTags = async () => {
  tagLoading.value = true
  tagError.value = ''
  try {
    const res = await apiFetch('/api/tags')
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagError.value = data.message || '话题加载失败。'
      return
    }
    tags.value = data.data || []
  } catch (err) {
    tagError.value = '网络错误，无法获取话题列表。'
  } finally {
    tagLoading.value = false
  }
}

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

const loadFeed = async () => {
  feedLoading.value = true
  feedError.value = ''
  try {
    const params = new URLSearchParams()
    params.set('page', page.value.toString())
    params.set('size', size.value.toString())

    let endpoint = '/api/posts/recommend'
    if (feedType.value === 'follow') {
      endpoint = '/api/posts/follow'
    } else if (feedType.value === 'hot') {
      endpoint = '/api/posts/hot'
    } else if (feedType.value === 'latest') {
      endpoint = '/api/posts/latest'
    } else if (feedType.value === 'topic') {
      if (!activeTagId.value) {
        feeds.value = []
        total.value = 0
        feedError.value = '请选择一个话题标签。'
        return
      }
      endpoint = '/api/posts/topic'
      params.set('tagId', activeTagId.value.toString())
    } else if (feedType.value === 'search') {
      const keyword = searchKeyword.value.trim()
      const resolvedTagId = searchTagId.value ? Number(searchTagId.value) : null
      if (!keyword && !resolvedTagId) {
        feeds.value = []
        total.value = 0
        feedError.value = '请输入关键词或选择话题标签。'
        return
      }
      endpoint = '/api/posts/search'
      if (keyword) {
        params.set('keyword', keyword)
      }
      if (resolvedTagId) {
        params.set('tagId', resolvedTagId.toString())
      }
    }

    const res = await apiFetch(`${endpoint}?${params.toString()}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      feedError.value = data.message || '内容加载失败。'
      return
    }
    feeds.value = data.data?.list || []
    total.value = data.data?.total ?? 0
  } catch (err) {
    feedError.value = '网络错误，无法获取内容。'
  } finally {
    feedLoading.value = false
  }
}

const setFeed = (type) => {
  feedType.value = type
  page.value = 1
  loadFeed()
}

const selectTopic = (tagId) => {
  activeTagId.value = tagId
  feedType.value = 'topic'
  page.value = 1
  loadFeed()
}

const submitSearch = () => {
  feedType.value = 'search'
  page.value = 1
  loadFeed()
}

const resetSearch = () => {
  searchKeyword.value = ''
  searchTagId.value = ''
  if (feedType.value === 'search') {
    setFeed('recommend')
  }
}

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value += 1
    loadFeed()
  }
}

const prevPage = () => {
  if (page.value > 1) {
    page.value -= 1
    loadFeed()
  }
}

onMounted(() => {
  try {
    const savedUser = localStorage.getItem('auth_user')
    if (savedUser) {
      user.value = JSON.parse(savedUser)
    }
    const savedRole = localStorage.getItem('auth_role')
    if (savedRole) {
      role.value = savedRole
    }
  } catch (err) {
    user.value = null
  }
  loadTags()
  loadFeed()
  loadUnreadCount()
})
</script>

<template>
  <div class="home-page">
    <header class="home-hero">
      <div class="hero-left">
        <span class="hero-tag">校园社交推荐平台</span>
        <h1>欢迎回来，{{ displayName }}。</h1>
        <p>
          这里是你的个性化校园主页，聚合最新动态、热门话题与推荐内容。
        </p>
        <div class="hero-actions">
          <RouterLink class="primary-btn" to="/posts/create">发布内容</RouterLink>
          <button class="ghost-btn" type="button" @click="goDiscover">去发现</button>
          <button class="ghost-btn" type="button" @click="logout">退出登录</button>
        </div>
        <div class="hero-meta">
          <span>当前角色：<strong>{{ roleLabel }}</strong></span>
          <span v-if="role === 'admin'" class="badge">管理入口待开发</span>
        </div>
      </div>
      <div class="hero-right">
        <div class="stat-card">
          <p class="stat-label">今日推荐</p>
          <p class="stat-value">32</p>
          <p class="stat-note">个性化内容流持续更新</p>
        </div>
        <div class="stat-card">
          <p class="stat-label">热门话题</p>
          <p class="stat-value">8</p>
          <p class="stat-note">覆盖社团、学习与校园生活</p>
        </div>
        <div class="stat-card link-card" role="button" tabindex="0" @click="router.push('/social')" @keydown.enter="router.push('/social')">
          <p class="stat-label">互动提醒</p>
          <p class="stat-value">{{ unreadCount }}</p>
          <p class="stat-note">未读通知实时更新</p>
        </div>
      </div>
    </header>

    <section class="module-grid">
      <article class="module-card">
        <h3>内容推荐</h3>
        <p>基于兴趣标签与行为偏好推送内容。</p>
        <span class="module-tag">推荐流</span>
      </article>
      <article class="module-card">
        <h3>校园动态</h3>
        <p>聚合学院、社团、活动等最新发布。</p>
        <span class="module-tag">动态广场</span>
      </article>
      <RouterLink class="module-card link-card" to="/social">
        <h3>互动社交</h3>
        <p>点赞评论、关注关系与消息提醒。</p>
        <span class="module-tag">社交连接</span>
      </RouterLink>
      <RouterLink class="module-card link-card" to="/profile">
        <h3>个人中心</h3>
        <p>个人资料、兴趣标签与内容管理。</p>
        <span class="module-tag">个人档案</span>
      </RouterLink>
    </section>

    <section ref="discoverRef" class="profile-section discover-section">
      <div class="profile-header">
        <div>
          <h2>内容发现</h2>
          <p>推荐、关注、热门、话题与搜索一站式浏览。</p>
        </div>
        <div class="discover-actions">
          <button class="ghost-btn" type="button" @click="setFeed('recommend')">刷新推荐</button>
          <RouterLink class="ghost-btn" to="/posts/create">去发布</RouterLink>
        </div>
      </div>

      <form class="discover-search" @submit.prevent="submitSearch">
        <label class="field">
          关键词
          <input v-model="searchKeyword" type="text" placeholder="输入标题或正文关键词" />
        </label>
        <label class="field">
          话题
          <select v-model="searchTagId">
            <option value="">全部话题</option>
            <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
          </select>
        </label>
        <button class="primary-btn" type="submit">搜索</button>
        <button class="ghost-btn" type="button" @click="resetSearch">重置</button>
      </form>

      <div class="tabs">
        <button class="tab-btn" :class="{ active: feedType === 'recommend' }" type="button" @click="setFeed('recommend')">推荐</button>
        <button class="tab-btn" :class="{ active: feedType === 'follow' }" type="button" @click="setFeed('follow')">关注</button>
        <button class="tab-btn" :class="{ active: feedType === 'hot' }" type="button" @click="setFeed('hot')">热门</button>
        <button class="tab-btn" :class="{ active: feedType === 'latest' }" type="button" @click="setFeed('latest')">最新</button>
        <button class="tab-btn" :class="{ active: feedType === 'topic' }" type="button" @click="setFeed('topic')">话题</button>
        <button v-if="feedType === 'search'" class="tab-btn active" type="button">搜索结果</button>
      </div>

      <div v-if="feedType === 'topic'" class="tag-grid">
        <button
          v-for="tag in tags"
          :key="tag.id"
          class="tag-pill"
          :class="{ active: activeTagId === tag.id }"
          type="button"
          @click="selectTopic(tag.id)"
        >
          {{ tag.name }}
        </button>
      </div>

      <div v-if="tagLoading && feedType === 'topic'" class="feed-empty">话题加载中...</div>
      <div v-else-if="tagError && feedType === 'topic'" class="form-alert error">{{ tagError }}</div>

      <div v-if="feedLoading" class="feed-empty">内容加载中...</div>
      <div v-else-if="feedError" class="form-alert error">{{ feedError }}</div>
      <div v-else-if="feeds.length" class="feed-grid">
        <article v-for="post in feeds" :key="post.id" class="feed-item">
          <div class="feed-item-top">
            <span class="feed-label">{{ feedType === 'topic' ? `#${activeTagName}` : feedTitle }}</span>
            <span class="status-pill" :class="normalizeStatus(post.status).className">
              {{ normalizeStatus(post.status).label }}
            </span>
          </div>
          <h4>{{ post.title || '未命名内容' }}</h4>
          <p>{{ formatSnippet(post.content) }}</p>
          <div class="feed-meta">
            <span>点赞 {{ post.likeCount ?? 0 }}</span>
            <span>评论 {{ post.commentCount ?? 0 }}</span>
            <span>收藏 {{ post.favoriteCount ?? 0 }}</span>
            <span>浏览 {{ post.viewCount ?? 0 }}</span>
          </div>
          <div class="feed-meta">
            <span>发布于 {{ post.createdAt || '-' }}</span>
            <span v-if="post.college">学院：{{ post.college }}</span>
            <span v-if="post.location">位置：{{ post.location }}</span>
          </div>
          <div class="feed-actions">
            <RouterLink class="ghost-btn" :to="`/posts/${post.id}`">查看详情</RouterLink>
          </div>
        </article>
      </div>
      <div v-else class="feed-empty">{{ emptyHint }}</div>

      <div class="pager">
        <button class="ghost-btn" type="button" :disabled="page <= 1 || feedLoading" @click="prevPage">上一页</button>
        <span>第 {{ page }} / {{ totalPages }} 页 · 共 {{ total }} 条</span>
        <button class="ghost-btn" type="button" :disabled="page >= totalPages || feedLoading" @click="nextPage">下一页</button>
      </div>
    </section>

  </div>
</template>
