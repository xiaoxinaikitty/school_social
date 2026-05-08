<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Compass, Promotion, StarFilled, SwitchButton } from '@element-plus/icons-vue'
import { apiFetch } from '../utils/api'
import { formatSnippet } from '../utils/user'
import UserShell from '../components/user/UserShell.vue'
import PostCard from '../components/user/PostCard.vue'

const user = ref(null)
const role = ref('user')
const router = useRouter()

const feedType = ref('recommend')
const feeds = ref([])
const feedLoading = ref(false)
const feedError = ref('')
const page = ref(1)
const size = ref(8)
const total = ref(0)

const tags = ref([])
const tagLoading = ref(false)
const tagError = ref('')
const activeTagId = ref(null)

const searchKeyword = ref('')
const searchTagId = ref('')

const unreadCount = ref(0)
const recommendTotal = ref(0)
const recommendLoading = ref(false)

const displayName = computed(() => user.value?.username || '用户')
const roleLabel = computed(() => (role.value === 'admin' ? '管理员' : '用户'))
const activeFeedTab = computed(() => (feedType.value === 'search' ? 'search' : feedType.value))
const spotlightPost = computed(() => feeds.value[0] || null)
const sideFeed = computed(() => feeds.value.slice(1, 4))
const hotTags = computed(() => tags.value.slice(0, 10))

const feedTitle = computed(() => {
  switch (feedType.value) {
    case 'follow':
      return '关注流'
    case 'hot':
      return '热议精选'
    case 'latest':
      return '最新发布'
    case 'topic':
      return '话题内容'
    case 'search':
      return '搜索结果'
    default:
      return '个性推荐'
  }
})

const emptyHint = computed(() => {
  switch (feedType.value) {
    case 'follow':
      return '你还没有形成关注流，先去发现更多用户。'
    case 'topic':
      return '这个话题暂时还没有新内容。'
    case 'search':
      return '没有找到匹配的校园动态。'
    case 'hot':
      return '当前暂无热议内容。'
    case 'latest':
      return '暂时没有最新发布。'
    default:
      return '推荐流暂时为空，请稍后刷新。'
  }
})

const activeTagName = computed(() => {
  const match = tags.value.find((item) => item.id === activeTagId.value)
  return match ? match.name : '话题'
})

const hotTopicNote = computed(() => {
  if (!tags.value.length) return '等待话题更新'
  return tags.value
    .slice(0, 3)
    .map((item) => item.name)
    .join(' / ')
})

const feedBadge = computed(() => (feedType.value === 'topic' ? `#${activeTagName.value}` : feedTitle.value))

const trackRecommendationClick = async (postId) => {
  if (feedType.value !== 'recommend' || !postId) return
  try {
    await apiFetch('/api/recommendation-logs/click', {
      method: 'POST',
      body: JSON.stringify({
        postId,
        scene: 0,
      }),
    })
  } catch {
    // Ignore recommendation tracking failures
  }
}

const openPost = (postId) => {
  trackRecommendationClick(postId)
  router.push(`/posts/${postId}`)
}

const handleRecommendedPostOpen = (post) => {
  trackRecommendationClick(post?.id)
}

const logout = async () => {
  try {
    await apiFetch('/api/auth/logout', { method: 'POST' })
  } catch {
    // Ignore network errors on logout
  } finally {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
    localStorage.removeItem('auth_role')
    router.push('/login')
  }
}

const loadTags = async () => {
  tagLoading.value = true
  tagError.value = ''
  try {
    const res = await apiFetch('/api/tags?status=1')
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
    if (!activeTagId.value && tags.value.length) {
      activeTagId.value = tags.value[0].id
    }
  } catch {
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
    if (!res.ok || data.code !== 0) return
    unreadCount.value = data.data ?? 0
  } catch {
    // Ignore
  }
}

const loadRecommendSummary = async () => {
  recommendLoading.value = true
  try {
    const res = await apiFetch('/api/posts/recommend?page=1&size=1')
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) return
    recommendTotal.value = data.data?.total ?? 0
  } catch {
    // Ignore
  } finally {
    recommendLoading.value = false
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
        feedError.value = '请选择一个话题。'
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
        feedError.value = '请输入关键词或选择一个话题。'
        return
      }
      endpoint = '/api/posts/search'
      if (keyword) params.set('keyword', keyword)
      if (resolvedTagId) params.set('tagId', resolvedTagId.toString())
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
  } catch {
    feedError.value = '网络错误，无法获取内容。'
  } finally {
    feedLoading.value = false
  }
}

const setFeed = (type) => {
  feedType.value = type
  page.value = 1
  if (type === 'topic' && !activeTagId.value && tags.value.length) {
    activeTagId.value = tags.value[0].id
  }
  loadFeed()
}

const handleFeedTab = (type) => {
  if (type === 'search') return
  setFeed(type)
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
  setFeed('recommend')
}

const handlePageChange = (current) => {
  page.value = current
  loadFeed()
}

onMounted(() => {
  try {
    const savedUser = localStorage.getItem('auth_user')
    if (savedUser) user.value = JSON.parse(savedUser)
    const savedRole = localStorage.getItem('auth_role')
    if (savedRole) role.value = savedRole
  } catch {
    user.value = null
  }
  loadTags()
  loadFeed()
  loadUnreadCount()
  loadRecommendSummary()
})
</script>

<template>
  <UserShell section="home">
    <section class="campus-hero campus-hero--feed">
      <div>
        <span class="campus-hero__eyebrow">
          <el-icon><Compass /></el-icon>
          校园内容广场
        </span>
        <h1 class="campus-hero__title">欢迎回来，{{ displayName }}。</h1>
        <p class="campus-hero__subtitle">
          这里以内容浏览为中心，聚合推荐、关注、热议与话题流。你可以先快速扫一遍重点内容，再进入详情深度互动。
        </p>
        <div class="campus-hero__actions">
          <el-button type="primary" size="large" @click="router.push('/posts/create')">
            发布校园动态
          </el-button>
          <el-button size="large" plain @click="setFeed('recommend')">刷新推荐流</el-button>
          <el-button size="large" plain @click="router.push('/social')">
            <el-icon><Bell /></el-icon>
            查看消息
          </el-button>
          <el-button size="large" plain @click="router.push('/chat')">
            进入群聊
          </el-button>
          <el-button size="large" plain @click="logout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
        <div class="campus-hero__chips">
          <el-tag round effect="light">当前身份：{{ roleLabel }}</el-tag>
          <el-tag round effect="plain" type="success">今日推荐 {{ recommendLoading ? '...' : recommendTotal }}</el-tag>
          <el-tag round effect="plain" type="warning">热门话题 {{ tags.length }}</el-tag>
          <el-tag round effect="plain" type="info">未读提醒 {{ unreadCount }}</el-tag>
        </div>
      </div>

      <div class="campus-hero__stats">
        <el-card class="campus-stat-card" shadow="hover" @click="setFeed('recommend')">
          <p class="campus-stat-card__label">推荐池</p>
          <p class="campus-stat-card__value">{{ recommendLoading ? '...' : recommendTotal }}</p>
          <p class="campus-stat-card__note">根据兴趣和行为持续更新</p>
        </el-card>
        <el-card class="campus-stat-card" shadow="hover" @click="setFeed('topic')">
          <p class="campus-stat-card__label">热门话题</p>
          <p class="campus-stat-card__value">{{ tagLoading ? '...' : tags.length }}</p>
          <p class="campus-stat-card__note">{{ hotTopicNote }}</p>
        </el-card>
        <el-card class="campus-stat-card" shadow="hover" @click="router.push('/social')">
          <p class="campus-stat-card__label">互动提醒</p>
          <p class="campus-stat-card__value">{{ unreadCount }}</p>
          <p class="campus-stat-card__note">评论、关注、系统通知集中查看</p>
        </el-card>
      </div>
    </section>

    <div class="campus-grid">
      <div>
        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">精确检索</h2>
              <p class="campus-panel__desc">按关键词和话题筛选，快速进入你真正想看的内容。</p>
            </div>
          </div>

          <form class="campus-search-grid" @submit.prevent="submitSearch">
            <el-input
              v-model="searchKeyword"
              clearable
              size="large"
              placeholder="搜索标题、正文、活动信息"
            />
            <el-select v-model="searchTagId" size="large" clearable placeholder="全部话题">
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.name"
                :value="String(tag.id)"
              />
            </el-select>
            <el-button type="primary" size="large" native-type="submit">
              <el-icon><Promotion /></el-icon>
              开始搜索
            </el-button>
            <el-button size="large" plain @click="resetSearch">重置</el-button>
          </form>
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">{{ feedTitle }}</h2>
              <p class="campus-panel__desc">
                当前共 {{ total }} 条内容，优先展示适合连续浏览的卡片流布局。
              </p>
            </div>
            <el-tag round effect="dark">{{ feedBadge }}</el-tag>
          </div>

          <el-tabs :model-value="activeFeedTab" @tab-change="handleFeedTab">
            <el-tab-pane label="推荐" name="recommend" />
            <el-tab-pane label="关注" name="follow" />
            <el-tab-pane label="热议" name="hot" />
            <el-tab-pane label="最新" name="latest" />
            <el-tab-pane label="话题" name="topic" />
            <el-tab-pane v-if="feedType === 'search'" label="搜索结果" name="search" />
          </el-tabs>

          <div v-if="feedType === 'topic'" class="campus-topic-row">
            <el-button
              v-for="tag in tags"
              :key="tag.id"
              :type="activeTagId === tag.id ? 'primary' : 'default'"
              plain
              round
              @click="selectTopic(tag.id)"
            >
              # {{ tag.name }}
            </el-button>
          </div>

          <el-alert
            v-if="tagError && feedType === 'topic'"
            :title="tagError"
            type="error"
            show-icon
            class="mb-4"
          />
          <el-alert v-if="feedError" :title="feedError" type="error" show-icon class="mb-4" />

          <div v-if="feedLoading" class="campus-card-grid">
            <el-skeleton v-for="index in 4" :key="index" animated>
              <template #template>
                <el-card class="campus-post-card">
                  <el-skeleton-item variant="h3" style="width: 46%; margin-bottom: 16px" />
                  <el-skeleton-item variant="text" style="width: 90%; margin-bottom: 10px" />
                  <el-skeleton-item variant="text" style="width: 100%; margin-bottom: 10px" />
                  <el-skeleton-item variant="text" style="width: 82%" />
                </el-card>
              </template>
            </el-skeleton>
          </div>
          <div v-else-if="feeds.length" class="campus-post-grid">
            <PostCard
              v-for="post in feeds"
              :key="post.id"
              :post="post"
              :badge="feedType === 'topic' ? `#${activeTagName}` : feedTitle"
              @open="handleRecommendedPostOpen"
            />
          </div>
          <el-empty v-else :description="emptyHint" />

          <div v-if="total > size" style="margin-top: 22px">
            <el-pagination
              background
              layout="prev, pager, next, jumper, ->, total"
              :current-page="page"
              :page-size="size"
              :total="total"
              @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </div>

      <aside>
        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">焦点内容</h2>
              <p class="campus-panel__desc">当前流中最值得优先打开的一条。</p>
            </div>
            <el-icon size="20"><StarFilled /></el-icon>
          </div>

          <div v-if="spotlightPost" class="campus-highlight">
            <el-tag round type="success" effect="light">{{ feedBadge }}</el-tag>
            <h3 class="campus-highlight__title">{{ spotlightPost.title || '未命名内容' }}</h3>
            <p class="campus-panel__desc">{{ formatSnippet(spotlightPost.content, 140) }}</p>
            <div class="campus-highlight__meta">
              <span>点赞 {{ spotlightPost.likeCount ?? 0 }}</span>
              <span>评论 {{ spotlightPost.commentCount ?? 0 }}</span>
              <span>浏览 {{ spotlightPost.viewCount ?? 0 }}</span>
            </div>
            <div class="campus-inline-actions" style="margin-top: 18px">
              <el-button type="primary" @click="openPost(spotlightPost.id)">
                阅读详情
              </el-button>
            </div>
          </div>
          <el-empty v-else description="暂无焦点内容" />

          <div v-if="sideFeed.length" class="campus-side-list" style="margin-top: 18px">
            <div v-for="item in sideFeed" :key="item.id" class="campus-side-item">
              <p class="campus-side-item__title">{{ item.title || '未命名内容' }}</p>
              <p class="campus-side-item__desc">{{ formatSnippet(item.content, 70) }}</p>
              <el-button text type="primary" @click="openPost(item.id)">
                继续阅读
              </el-button>
            </div>
          </div>
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">热度话题</h2>
              <p class="campus-panel__desc">更适合连续刷看的校园主题入口。</p>
            </div>
          </div>

          <div class="campus-topic-row">
            <el-button
              v-for="tag in hotTags"
              :key="tag.id"
              plain
              round
              @click="selectTopic(tag.id)"
            >
              # {{ tag.name }}
            </el-button>
          </div>
          <el-empty v-if="!tagLoading && !hotTags.length" description="暂未配置热门话题" />
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">快捷入口</h2>
              <p class="campus-panel__desc">以浏览和互动为中心的高频操作。</p>
            </div>
          </div>

          <div class="campus-side-list">
            <div class="campus-side-item">
              <p class="campus-side-item__title">互动中心</p>
              <p class="campus-side-item__desc">通知、公告、举报进度统一处理。</p>
              <el-button text type="primary" @click="router.push('/social')">进入互动中心</el-button>
            </div>
            <div class="campus-side-item">
              <p class="campus-side-item__title">社区群聊</p>
              <p class="campus-side-item__desc">加入公共群，实时参与校园聊天与主题讨论。</p>
              <el-button text type="primary" @click="router.push('/chat')">进入群聊</el-button>
            </div>
            <div class="campus-side-item">
              <p class="campus-side-item__title">我的空间</p>
              <p class="campus-side-item__desc">维护资料、偏好标签和个人内容资产。</p>
              <el-button text type="primary" @click="router.push('/profile')">查看个人空间</el-button>
            </div>
            <div class="campus-side-item">
              <p class="campus-side-item__title">发布新内容</p>
              <p class="campus-side-item__desc">支持图文、活动、投票和二手信息。</p>
              <el-button text type="primary" @click="router.push('/posts/create')">去发布</el-button>
            </div>
          </div>
        </el-card>
      </aside>
    </div>
  </UserShell>
</template>
