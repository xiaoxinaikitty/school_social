<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const loading = ref(false)
const error = ref('')
const related = ref([])
const relatedLoading = ref(false)
const relatedError = ref('')
const stats = ref(null)
const statsLoading = ref(false)
const statsError = ref('')

const currentUser = ref(null)
const liked = ref(false)
const favorited = ref(false)
const following = ref(false)
const actionError = ref('')
const actionSuccess = ref('')

const commentContent = ref('')
const commentLoading = ref(false)
const commentError = ref('')
const comments = ref([])
const commentPage = ref(1)
const commentSize = 8
const commentTotal = ref(0)

const replyMap = ref({})
const replyDrafts = ref({})
const commentLikeMap = ref({})

const isOwner = computed(() => {
  if (!currentUser.value || !detail.value) return false
  return Number(currentUser.value.id) === Number(detail.value.userId)
})

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await apiFetch(`/api/posts/${route.params.id}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '获取详情失败。'
      return
    }
    detail.value = data.data
    await Promise.all([loadRelated(), loadStats(), loadComments(1)])
  } catch (err) {
    error.value = '网络错误，无法获取详情。'
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  statsLoading.value = true
  statsError.value = ''
  try {
    const res = await apiFetch(`/api/posts/${route.params.id}/stats`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      statsError.value = data.message || '获取统计失败。'
      return
    }
    stats.value = data.data
  } catch (err) {
    statsError.value = '网络错误，无法获取统计。'
  } finally {
    statsLoading.value = false
  }
}

const loadRelated = async () => {
  relatedLoading.value = true
  relatedError.value = ''
  try {
    const res = await apiFetch(`/api/posts/${route.params.id}/related`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      relatedError.value = data.message || '获取相关推荐失败。'
      return
    }
    related.value = data.data || []
  } catch (err) {
    relatedError.value = '网络错误，无法获取相关推荐。'
  } finally {
    relatedLoading.value = false
  }
}

const loadComments = async (page = 1) => {
  commentLoading.value = true
  commentError.value = ''
  try {
    const res = await apiFetch(
      `/api/comments?postId=${route.params.id}&page=${page}&size=${commentSize}`,
    )
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      commentError.value = data.message || '获取评论失败。'
      return
    }
    comments.value = data.data?.list || []
    commentTotal.value = data.data?.total ?? 0
    commentPage.value = data.data?.page ?? page
  } catch (err) {
    commentError.value = '网络错误，无法获取评论。'
  } finally {
    commentLoading.value = false
  }
}

const loadReplies = async (parentId) => {
  if (!parentId) return
  replyMap.value = {
    ...replyMap.value,
    [parentId]: {
      ...(replyMap.value[parentId] || {}),
      loading: true,
      error: '',
      expanded: true,
    },
  }
  try {
    const res = await apiFetch(
      `/api/comments?postId=${route.params.id}&parentId=${parentId}&page=1&size=20`,
    )
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      replyMap.value = {
        ...replyMap.value,
        [parentId]: {
          ...(replyMap.value[parentId] || {}),
          loading: false,
          error: data.message || '获取回复失败。',
          expanded: true,
        },
      }
      return
    }
    replyMap.value = {
      ...replyMap.value,
      [parentId]: {
        list: data.data?.list || [],
        total: data.data?.total ?? 0,
        loading: false,
        error: '',
        expanded: true,
      },
    }
  } catch (err) {
    replyMap.value = {
      ...replyMap.value,
      [parentId]: {
        ...(replyMap.value[parentId] || {}),
        loading: false,
        error: '网络错误，无法获取回复。',
        expanded: true,
      },
    }
  }
}

const toggleReplies = (parentId) => {
  const existing = replyMap.value[parentId]
  if (existing?.expanded) {
    replyMap.value = {
      ...replyMap.value,
      [parentId]: {
        ...existing,
        expanded: false,
      },
    }
    return
  }
  loadReplies(parentId)
}

const submitComment = async () => {
  commentError.value = ''
  actionSuccess.value = ''
  actionError.value = ''
  const content = commentContent.value.trim()
  if (!content) {
    commentError.value = '请输入评论内容。'
    return
  }
  commentLoading.value = true
  try {
    const res = await apiFetch('/api/comments', {
      method: 'POST',
      body: JSON.stringify({
        postId: Number(route.params.id),
        content,
        parentId: null,
      }),
    })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      commentError.value = data.message || '发表评论失败。'
      return
    }
    commentContent.value = ''
    await Promise.all([loadComments(1), loadStats()])
    actionSuccess.value = '评论已发布。'
  } catch (err) {
    commentError.value = '网络错误，无法发表评论。'
  } finally {
    commentLoading.value = false
  }
}

const submitReply = async (parentId) => {
  if (!parentId) return
  const draft = replyDrafts.value[parentId] || ''
  const content = draft.trim()
  if (!content) {
    replyMap.value = {
      ...replyMap.value,
      [parentId]: {
        ...(replyMap.value[parentId] || {}),
        error: '请输入回复内容。',
      },
    }
    return
  }
  replyMap.value = {
    ...replyMap.value,
    [parentId]: {
      ...(replyMap.value[parentId] || {}),
      loading: true,
      error: '',
    },
  }
  try {
    const res = await apiFetch('/api/comments', {
      method: 'POST',
      body: JSON.stringify({
        postId: Number(route.params.id),
        content,
        parentId,
      }),
    })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      replyMap.value = {
        ...replyMap.value,
        [parentId]: {
          ...(replyMap.value[parentId] || {}),
          loading: false,
          error: data.message || '回复失败。',
          expanded: true,
        },
      }
      return
    }
    replyDrafts.value = { ...replyDrafts.value, [parentId]: '' }
    await Promise.all([loadReplies(parentId), loadStats()])
  } catch (err) {
    replyMap.value = {
      ...replyMap.value,
      [parentId]: {
        ...(replyMap.value[parentId] || {}),
        loading: false,
        error: '网络错误，无法回复。',
        expanded: true,
      },
    }
  } finally {
    replyMap.value = {
      ...replyMap.value,
      [parentId]: {
        ...(replyMap.value[parentId] || {}),
        loading: false,
        expanded: true,
      },
    }
  }
}

const deleteComment = async (commentId) => {
  if (!commentId) return
  if (!confirm('确定要删除该评论吗？')) return
  try {
    const res = await apiFetch(`/api/comments/${commentId}`, { method: 'DELETE' })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      commentError.value = data.message || '删除失败。'
      return
    }
    await Promise.all([loadComments(commentPage.value || 1), loadStats()])
  } catch (err) {
    commentError.value = '网络错误，无法删除评论。'
  }
}

const toggleLike = async () => {
  actionError.value = ''
  actionSuccess.value = ''
  const postId = Number(route.params.id)
  try {
    if (!liked.value) {
      const res = await apiFetch('/api/likes', {
        method: 'POST',
        body: JSON.stringify({ targetType: 0, targetId: postId }),
      })
      if (res.status === 401) {
        router.push('/login')
        return
      }
      const data = await res.json()
      if (!res.ok || data.code !== 0) {
        actionError.value = data.message || '点赞失败。'
        return
      }
      liked.value = true
    } else {
      const res = await apiFetch(`/api/likes?targetType=0&targetId=${postId}`, {
        method: 'DELETE',
      })
      if (res.status === 401) {
        router.push('/login')
        return
      }
      const data = await res.json()
      if (!res.ok || data.code !== 0) {
        actionError.value = data.message || '取消点赞失败。'
        return
      }
      liked.value = false
    }
    await loadStats()
  } catch (err) {
    actionError.value = '网络错误，操作失败。'
  }
}

const toggleFavorite = async () => {
  actionError.value = ''
  actionSuccess.value = ''
  const postId = Number(route.params.id)
  try {
    if (!favorited.value) {
      const res = await apiFetch('/api/favorites', {
        method: 'POST',
        body: JSON.stringify({ postId }),
      })
      if (res.status === 401) {
        router.push('/login')
        return
      }
      const data = await res.json()
      if (!res.ok || data.code !== 0) {
        actionError.value = data.message || '收藏失败。'
        return
      }
      favorited.value = true
    } else {
      const res = await apiFetch(`/api/favorites/${postId}`, { method: 'DELETE' })
      if (res.status === 401) {
        router.push('/login')
        return
      }
      const data = await res.json()
      if (!res.ok || data.code !== 0) {
        actionError.value = data.message || '取消收藏失败。'
        return
      }
      favorited.value = false
    }
    await loadStats()
  } catch (err) {
    actionError.value = '网络错误，操作失败。'
  }
}

const toggleFollow = async () => {
  actionError.value = ''
  actionSuccess.value = ''
  if (!detail.value) return
  const followeeId = detail.value.userId
  if (!followeeId) return
  try {
    if (!following.value) {
      const res = await apiFetch(`/api/follows/${followeeId}`, { method: 'POST' })
      if (res.status === 401) {
        router.push('/login')
        return
      }
      const data = await res.json()
      if (!res.ok || data.code !== 0) {
        actionError.value = data.message || '关注失败。'
        return
      }
      following.value = true
      actionSuccess.value = '已关注该用户。'
    } else {
      const res = await apiFetch(`/api/follows/${followeeId}`, { method: 'DELETE' })
      if (res.status === 401) {
        router.push('/login')
        return
      }
      const data = await res.json()
      if (!res.ok || data.code !== 0) {
        actionError.value = data.message || '取消关注失败。'
        return
      }
      following.value = false
      actionSuccess.value = '已取消关注。'
    }
  } catch (err) {
    actionError.value = '网络错误，操作失败。'
  }
}

const sharePost = async () => {
  actionError.value = ''
  actionSuccess.value = ''
  try {
    const res = await apiFetch('/api/shares', {
      method: 'POST',
      body: JSON.stringify({ postId: Number(route.params.id), channel: 'web' }),
    })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      actionError.value = data.message || '分享失败。'
      return
    }
    actionSuccess.value = '已记录分享。'
  } catch (err) {
    actionError.value = '网络错误，分享失败。'
  }
}

const toggleCommentLike = async (comment) => {
  if (!comment?.id) return
  const current = !!commentLikeMap.value[comment.id]
  try {
    if (!current) {
      const res = await apiFetch('/api/likes', {
        method: 'POST',
        body: JSON.stringify({ targetType: 1, targetId: comment.id }),
      })
      if (res.status === 401) {
        router.push('/login')
        return
      }
      const data = await res.json()
      if (!res.ok || data.code !== 0) {
        commentError.value = data.message || '点赞失败。'
        return
      }
      comment.likeCount = (comment.likeCount || 0) + 1
      commentLikeMap.value = { ...commentLikeMap.value, [comment.id]: true }
    } else {
      const res = await apiFetch(`/api/likes?targetType=1&targetId=${comment.id}`, {
        method: 'DELETE',
      })
      if (res.status === 401) {
        router.push('/login')
        return
      }
      const data = await res.json()
      if (!res.ok || data.code !== 0) {
        commentError.value = data.message || '取消点赞失败。'
        return
      }
      comment.likeCount = Math.max((comment.likeCount || 0) - 1, 0)
      commentLikeMap.value = { ...commentLikeMap.value, [comment.id]: false }
    }
  } catch (err) {
    commentError.value = '网络错误，操作失败。'
  }
}

watch(() => route.params.id, () => {
  liked.value = false
  favorited.value = false
  following.value = false
  replyMap.value = {}
  replyDrafts.value = {}
  commentLikeMap.value = {}
  loadDetail()
})

onMounted(() => {
  try {
    const savedUser = localStorage.getItem('auth_user')
    if (savedUser) {
      currentUser.value = JSON.parse(savedUser)
    }
  } catch (err) {
    currentUser.value = null
  }
  loadDetail()
})
</script>

<template>
  <div class="post-page">
    <header class="profile-hero">
      <div>
        <span class="hero-tag">内容详情</span>
        <h1>{{ detail?.title || '未命名内容' }}</h1>
        <p>发布于 {{ detail?.createdAt || '-' }}</p>
      </div>
      <div class="profile-actions">
        <button class="ghost-btn" type="button" @click="router.push('/profile')">返回个人中心</button>
        <button class="ghost-btn" type="button" @click="router.push(`/posts/${route.params.id}/edit`)">编辑</button>
      </div>
    </header>

    <section class="profile-section">
      <div v-if="loading" class="feed-empty">加载中...</div>
      <div v-else-if="error" class="form-alert error">{{ error }}</div>
      <div v-else-if="detail" class="post-detail">
        <div class="interaction-bar">
          <div class="interaction-stats">
            <span>点赞 {{ stats?.likeCount ?? detail.likeCount ?? 0 }}</span>
            <span>评论 {{ stats?.commentCount ?? detail.commentCount ?? 0 }}</span>
            <span>收藏 {{ stats?.favoriteCount ?? detail.favoriteCount ?? 0 }}</span>
            <span>浏览 {{ stats?.viewCount ?? detail.viewCount ?? 0 }}</span>
          </div>
          <div class="interaction-actions">
            <button class="ghost-btn" type="button" @click="toggleLike">
              {{ liked ? '已点赞' : '点赞' }}
            </button>
            <button class="ghost-btn" type="button" @click="toggleFavorite">
              {{ favorited ? '已收藏' : '收藏' }}
            </button>
            <button class="ghost-btn" type="button" @click="sharePost">分享</button>
            <button
              v-if="!isOwner"
              class="ghost-btn"
              type="button"
              @click="toggleFollow"
            >
              {{ following ? '取消关注' : '关注作者' }}
            </button>
          </div>
        </div>
        <p v-if="actionError" class="form-alert error">{{ actionError }}</p>
        <p v-if="actionSuccess" class="form-alert success">{{ actionSuccess }}</p>
        <p v-if="statsError" class="form-alert error">{{ statsError }}</p>
        <p class="post-content">{{ detail.content }}</p>
        <div class="post-meta">
          <span>可见范围：{{ detail.visibility === 0 ? '全校' : detail.visibility === 1 ? '关注' : '仅自己' }}</span>
          <span>类型：{{ detail.postType ?? 0 }}</span>
          <span class="status-pill" :class="detail.status === 0 ? 'pending' : detail.status === 1 ? 'approved' : detail.status === 2 ? 'rejected' : 'draft'">
            {{ detail.status === 0 ? '待审' : detail.status === 1 ? '通过' : detail.status === 2 ? '驳回' : '草稿' }}
          </span>
          <span v-if="detail.location">位置：{{ detail.location }}</span>
          <span v-if="detail.college">学院：{{ detail.college }}</span>
        </div>

        <div v-if="detail.media && detail.media.length" class="media-preview">
          <div v-for="media in detail.media" :key="media.id" class="media-item">
            <img v-if="media.mediaType === 0" :src="media.url" alt="media" />
            <a v-else :href="media.url" target="_blank" rel="noreferrer">查看视频</a>
          </div>
        </div>
      </div>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>评论区</h2>
          <p>分享你的观点，参与讨论。</p>
        </div>
      </div>
      <form class="comment-form" @submit.prevent="submitComment">
        <label class="field">
          <span>发表评论</span>
          <textarea v-model="commentContent" rows="3" placeholder="写下你的评论..."></textarea>
        </label>
        <div class="profile-actions">
          <button class="primary-btn" type="submit" :disabled="commentLoading">
            {{ commentLoading ? '发布中...' : '发布评论' }}
          </button>
        </div>
        <p v-if="commentError" class="form-alert error">{{ commentError }}</p>
      </form>

      <div v-if="commentLoading" class="feed-empty">评论加载中...</div>
      <div v-else-if="comments.length === 0" class="feed-empty">暂无评论，快来第一条吧。</div>
      <div v-else class="comment-list">
        <div v-for="item in comments" :key="item.id" class="comment-item">
          <div class="comment-body">
            <div class="comment-meta">
              <span>用户 {{ item.userId }}</span>
              <span>{{ item.createdAt }}</span>
            </div>
            <p>{{ item.content }}</p>
            <div class="comment-actions">
              <button class="ghost-btn" type="button" @click="toggleCommentLike(item)">
                赞 {{ item.likeCount ?? 0 }}
              </button>
              <button class="ghost-btn" type="button" @click="toggleReplies(item.id)">
                {{ replyMap[item.id]?.expanded ? '收起回复' : '查看回复' }}
              </button>
              <button
                v-if="currentUser?.id === item.userId"
                class="ghost-btn danger"
                type="button"
                @click="deleteComment(item.id)"
              >
                删除
              </button>
            </div>
          </div>

          <div v-if="replyMap[item.id]?.expanded" class="reply-section">
            <div v-if="replyMap[item.id]?.loading" class="feed-empty">回复加载中...</div>
            <div v-else-if="replyMap[item.id]?.error" class="form-alert error">{{ replyMap[item.id].error }}</div>
            <div v-else-if="replyMap[item.id]?.list?.length" class="reply-list">
              <div v-for="reply in replyMap[item.id].list" :key="reply.id" class="reply-item">
                <div class="comment-meta">
                  <span>用户 {{ reply.userId }}</span>
                  <span>{{ reply.createdAt }}</span>
                </div>
                <p>{{ reply.content }}</p>
                <div class="comment-actions">
                  <button class="ghost-btn" type="button" @click="toggleCommentLike(reply)">
                    赞 {{ reply.likeCount ?? 0 }}
                  </button>
                  <button
                    v-if="currentUser?.id === reply.userId"
                    class="ghost-btn danger"
                    type="button"
                    @click="deleteComment(reply.id)"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>
            <div v-else class="feed-empty">暂无回复。</div>

            <div class="reply-form">
              <label class="field">
                <span>回复评论</span>
              <input v-model="replyDrafts[item.id]" type="text" placeholder="回复这条评论..." />
              </label>
              <button class="ghost-btn" type="button" @click="submitReply(item.id)">回复</button>
            </div>
          </div>
        </div>
      </div>

      <div class="pager" v-if="commentTotal > commentSize">
        <button class="ghost-btn" type="button" :disabled="commentPage <= 1" @click="loadComments(commentPage - 1)">
          上一页
        </button>
        <span>{{ commentPage }} / {{ Math.ceil(commentTotal / commentSize) }}</span>
        <button
          class="ghost-btn"
          type="button"
          :disabled="commentPage >= Math.ceil(commentTotal / commentSize)"
          @click="loadComments(commentPage + 1)"
        >
          下一页
        </button>
      </div>
    </section>

    <section class="profile-section">
      <div class="profile-header">
        <div>
          <h2>相关推荐</h2>
          <p>基于话题标签推荐的相似内容。</p>
        </div>
        <button class="ghost-btn" type="button" @click="loadRelated">刷新</button>
      </div>
      <div v-if="relatedLoading" class="feed-empty">加载中...</div>
      <div v-else-if="relatedError" class="form-alert error">{{ relatedError }}</div>
      <div v-else-if="related.length" class="feed-grid">
        <article v-for="item in related" :key="item.id" class="feed-item">
          <h4>{{ item.title || '未命名内容' }}</h4>
          <p>{{ item.content || '暂无内容' }}</p>
          <div class="feed-meta">
            <span>点赞 {{ item.likeCount ?? 0 }}</span>
            <span>评论 {{ item.commentCount ?? 0 }}</span>
            <span>收藏 {{ item.favoriteCount ?? 0 }}</span>
          </div>
          <div class="feed-actions">
            <RouterLink class="ghost-btn" :to="`/posts/${item.id}`">查看详情</RouterLink>
          </div>
        </article>
      </div>
      <div v-else class="feed-empty">暂无相关推荐。</div>
    </section>
  </div>
</template>
