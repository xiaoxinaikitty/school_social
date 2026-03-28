<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Flag, Promotion, Star, Position } from '@element-plus/icons-vue'
import { apiFetch } from '../utils/api'
import { formatSnippet, getInitial, getPostTypeLabel, getStatusMeta, getVisibilityLabel } from '../utils/user'
import UserShell from '../components/user/UserShell.vue'
import PostCard from '../components/user/PostCard.vue'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const stats = ref(null)
const related = ref([])
const comments = ref([])
const currentUser = ref(null)

const loading = ref(false)
const error = ref('')
const relatedLoading = ref(false)
const relatedError = ref('')
const statsLoading = ref(false)
const statsError = ref('')
const commentsLoading = ref(false)
const commentSubmitting = ref(false)
const commentError = ref('')

const commentContent = ref('')
const commentPage = ref(1)
const commentTotal = ref(0)
const commentSize = 8
const replyMap = ref({})
const replyDrafts = ref({})
const commentLikeMap = ref({})

const liked = ref(false)
const favorited = ref(false)
const following = ref(false)
const actionError = ref('')
const actionSuccess = ref('')

const showReportForm = ref(false)
const reportTarget = ref({ targetType: 0, targetId: null })
const reportReason = ref('')
const reportDetail = ref('')
const reportLoading = ref(false)
const reportError = ref('')
const reportSuccess = ref('')
const reportReasons = ['不实信息', '骚扰辱骂', '色情低俗', '违规广告', '侵犯隐私', '其他']

const isOwner = computed(() => Number(currentUser.value?.id) === Number(detail.value?.userId))
const statusMeta = computed(() => getStatusMeta(detail.value?.status))
const authorName = computed(() => detail.value?.username || `用户 ${detail.value?.userId ?? ''}`)
const authorInitial = computed(() => getInitial(authorName.value, '校'))
const mediaPreviewUrls = computed(() => (detail.value?.media || []).filter((item) => item.mediaType === 0).map((item) => item.url))

const resetState = () => {
  liked.value = false
  favorited.value = false
  following.value = false
  replyMap.value = {}
  replyDrafts.value = {}
  commentLikeMap.value = {}
  showReportForm.value = false
  reportTarget.value = { targetType: 0, targetId: null }
  reportReason.value = ''
  reportDetail.value = ''
  reportError.value = ''
  reportSuccess.value = ''
}

const authGuard = (res) => {
  if (res.status === 401) {
    router.push('/login')
    return true
  }
  return false
}

const loadStats = async () => {
  statsLoading.value = true
  statsError.value = ''
  try {
    const res = await apiFetch(`/api/posts/${route.params.id}/stats`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      statsError.value = data.message || '获取统计失败。'
      return
    }
    stats.value = data.data
  } catch (error) {
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
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      relatedError.value = data.message || '获取相关推荐失败。'
      return
    }
    related.value = data.data || []
  } catch (error) {
    relatedError.value = '网络错误，无法获取相关推荐。'
  } finally {
    relatedLoading.value = false
  }
}

const loadComments = async (page = 1) => {
  commentsLoading.value = true
  commentError.value = ''
  try {
    const res = await apiFetch(`/api/comments?postId=${route.params.id}&page=${page}&size=${commentSize}`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      commentError.value = data.message || '获取评论失败。'
      return
    }
    comments.value = data.data?.list || []
    commentTotal.value = data.data?.total ?? 0
    commentPage.value = data.data?.page ?? page
  } catch (error) {
    commentError.value = '网络错误，无法获取评论。'
  } finally {
    commentsLoading.value = false
  }
}

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await apiFetch(`/api/posts/${route.params.id}`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '获取详情失败。'
      return
    }
    detail.value = data.data
    await Promise.all([loadStats(), loadRelated(), loadComments(1)])
  } catch (error) {
    error.value = '网络错误，无法获取详情。'
  } finally {
    loading.value = false
  }
}

const loadReplies = async (parentId) => {
  replyMap.value = { ...replyMap.value, [parentId]: { ...(replyMap.value[parentId] || {}), loading: true, expanded: true, error: '' } }
  try {
    const res = await apiFetch(`/api/comments?postId=${route.params.id}&parentId=${parentId}&page=1&size=20`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      replyMap.value = { ...replyMap.value, [parentId]: { ...(replyMap.value[parentId] || {}), loading: false, expanded: true, error: data.message || '获取回复失败。' } }
      return
    }
    replyMap.value = { ...replyMap.value, [parentId]: { list: data.data?.list || [], loading: false, expanded: true, error: '' } }
  } catch (error) {
    replyMap.value = { ...replyMap.value, [parentId]: { ...(replyMap.value[parentId] || {}), loading: false, expanded: true, error: '网络错误，无法获取回复。' } }
  }
}

const toggleReplies = (parentId) => {
  const current = replyMap.value[parentId]
  if (current?.expanded) {
    replyMap.value = { ...replyMap.value, [parentId]: { ...current, expanded: false } }
    return
  }
  loadReplies(parentId)
}

const submitComment = async () => {
  if (!commentContent.value.trim()) {
    commentError.value = '请输入评论内容。'
    return
  }
  commentSubmitting.value = true
  commentError.value = ''
  try {
    const res = await apiFetch('/api/comments', {
      method: 'POST',
      body: JSON.stringify({ postId: Number(route.params.id), content: commentContent.value.trim(), parentId: null }),
    })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      commentError.value = data.message || '发表评论失败。'
      return
    }
    commentContent.value = ''
    await Promise.all([loadComments(1), loadStats()])
    actionSuccess.value = '评论已发布。'
  } catch (error) {
    commentError.value = '网络错误，无法发表评论。'
  } finally {
    commentSubmitting.value = false
  }
}

const submitReply = async (parentId) => {
  const content = (replyDrafts.value[parentId] || '').trim()
  if (!content) {
    replyMap.value = { ...replyMap.value, [parentId]: { ...(replyMap.value[parentId] || {}), error: '请输入回复内容。' } }
    return
  }
  replyMap.value = { ...replyMap.value, [parentId]: { ...(replyMap.value[parentId] || {}), loading: true, error: '' } }
  try {
    const res = await apiFetch('/api/comments', {
      method: 'POST',
      body: JSON.stringify({ postId: Number(route.params.id), content, parentId }),
    })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      replyMap.value = { ...replyMap.value, [parentId]: { ...(replyMap.value[parentId] || {}), loading: false, error: data.message || '回复失败。', expanded: true } }
      return
    }
    replyDrafts.value = { ...replyDrafts.value, [parentId]: '' }
    await Promise.all([loadReplies(parentId), loadStats()])
  } catch (error) {
    replyMap.value = { ...replyMap.value, [parentId]: { ...(replyMap.value[parentId] || {}), loading: false, error: '网络错误，无法回复。', expanded: true } }
  }
}

const deleteComment = async (commentId) => {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', { type: 'warning' })
  } catch (error) {
    return
  }
  try {
    const res = await apiFetch(`/api/comments/${commentId}`, { method: 'DELETE' })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      commentError.value = data.message || '删除失败。'
      return
    }
    await Promise.all([loadComments(commentPage.value || 1), loadStats()])
  } catch (error) {
    commentError.value = '网络错误，无法删除评论。'
  }
}

const toggleLike = async () => {
  const postId = Number(route.params.id)
  actionError.value = ''
  try {
    const res = !liked.value
      ? await apiFetch('/api/likes', { method: 'POST', body: JSON.stringify({ targetType: 0, targetId: postId }) })
      : await apiFetch(`/api/likes?targetType=0&targetId=${postId}`, { method: 'DELETE' })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      actionError.value = data.message || '操作失败。'
      return
    }
    liked.value = !liked.value
    await loadStats()
  } catch (error) {
    actionError.value = '网络错误，操作失败。'
  }
}

const toggleFavorite = async () => {
  const postId = Number(route.params.id)
  actionError.value = ''
  try {
    const res = !favorited.value
      ? await apiFetch('/api/favorites', { method: 'POST', body: JSON.stringify({ postId }) })
      : await apiFetch(`/api/favorites/${postId}`, { method: 'DELETE' })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      actionError.value = data.message || '操作失败。'
      return
    }
    favorited.value = !favorited.value
    await loadStats()
  } catch (error) {
    actionError.value = '网络错误，操作失败。'
  }
}

const toggleFollow = async () => {
  if (!detail.value?.userId) return
  actionError.value = ''
  try {
    const res = !following.value
      ? await apiFetch(`/api/follows/${detail.value.userId}`, { method: 'POST' })
      : await apiFetch(`/api/follows/${detail.value.userId}`, { method: 'DELETE' })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      actionError.value = data.message || '操作失败。'
      return
    }
    following.value = !following.value
    actionSuccess.value = following.value ? '已关注该作者。' : '已取消关注。'
  } catch (error) {
    actionError.value = '网络错误，操作失败。'
  }
}

const sharePost = async () => {
  actionError.value = ''
  try {
    const res = await apiFetch('/api/shares', { method: 'POST', body: JSON.stringify({ postId: Number(route.params.id), channel: 'web' }) })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      actionError.value = data.message || '分享失败。'
      return
    }
    actionSuccess.value = '分享记录已保存。'
  } catch (error) {
    actionError.value = '网络错误，分享失败。'
  }
}

const toggleCommentLike = async (comment) => {
  const current = !!commentLikeMap.value[comment.id]
  try {
    const res = !current
      ? await apiFetch('/api/likes', { method: 'POST', body: JSON.stringify({ targetType: 1, targetId: comment.id }) })
      : await apiFetch(`/api/likes?targetType=1&targetId=${comment.id}`, { method: 'DELETE' })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      commentError.value = data.message || '操作失败。'
      return
    }
    comment.likeCount = current ? Math.max((comment.likeCount || 0) - 1, 0) : (comment.likeCount || 0) + 1
    commentLikeMap.value = { ...commentLikeMap.value, [comment.id]: !current }
  } catch (error) {
    commentError.value = '网络错误，操作失败。'
  }
}

const prepareReport = (targetType, targetId) => {
  reportTarget.value = { targetType, targetId }
  reportReason.value = ''
  reportDetail.value = ''
  reportError.value = ''
  reportSuccess.value = ''
  showReportForm.value = true
}

const submitReport = async () => {
  if (!reportTarget.value.targetId || !reportReason.value.trim()) {
    reportError.value = '请选择举报原因。'
    return
  }
  reportLoading.value = true
  reportError.value = ''
  try {
    const res = await apiFetch('/api/reports', {
      method: 'POST',
      body: JSON.stringify({
        targetType: reportTarget.value.targetType,
        targetId: reportTarget.value.targetId,
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
    reportSuccess.value = '举报已提交，我们会尽快处理。'
  } catch (error) {
    reportError.value = '网络错误，无法提交举报。'
  } finally {
    reportLoading.value = false
  }
}

watch(() => route.params.id, () => {
  resetState()
  loadDetail()
})

onMounted(() => {
  try {
    const savedUser = localStorage.getItem('auth_user')
    if (savedUser) currentUser.value = JSON.parse(savedUser)
  } catch (error) {
    currentUser.value = null
  }
  loadDetail()
})
</script>

<template>
  <UserShell section="home">
    <section class="campus-hero">
      <el-page-header @back="router.push('/home')">
        <template #content><span style="font-weight: 700">内容详情</span></template>
        <template #extra>
          <div class="campus-inline-actions">
            <el-button plain @click="router.push('/profile')">我的空间</el-button>
            <el-button v-if="isOwner" type="primary" plain @click="router.push(`/posts/${route.params.id}/edit`)">编辑内容</el-button>
          </div>
        </template>
      </el-page-header>
      <p class="campus-hero__subtitle">正文阅读、互动反馈和相关推荐分栏展示，浏览路径会更顺畅。</p>
    </section>

    <div class="campus-grid campus-grid--detail">
      <div>
        <el-card v-if="loading" class="campus-panel" shadow="never"><el-skeleton animated :rows="10" /></el-card>
        <el-alert v-else-if="error" :title="error" type="error" show-icon />
        <template v-else-if="detail">
          <el-card class="campus-panel" shadow="never">
            <div class="campus-meta-row">
              <el-tag round>{{ getPostTypeLabel(detail.postType) }}</el-tag>
              <el-tag round :type="statusMeta.type">{{ statusMeta.label }}</el-tag>
              <el-tag round type="info">{{ getVisibilityLabel(detail.visibility) }}</el-tag>
              <el-tag v-if="detail.college" round type="success">{{ detail.college }}</el-tag>
              <el-tag v-if="detail.location" round type="warning">{{ detail.location }}</el-tag>
            </div>
            <h1 class="campus-detail-article__title">{{ detail.title || '未命名内容' }}</h1>
            <div class="campus-author">
              <div class="campus-author__meta">
                <el-avatar :size="52">{{ authorInitial }}</el-avatar>
                <div>
                  <p class="campus-author__name">{{ authorName }}</p>
                  <p class="campus-muted">发布于 {{ detail.createdAt || '-' }}</p>
                </div>
              </div>
              <div class="campus-inline-actions">
                <el-button :type="liked ? 'primary' : 'default'" plain @click="toggleLike"><el-icon><Position /></el-icon>{{ liked ? '已点赞' : '点赞' }}</el-button>
                <el-button :type="favorited ? 'warning' : 'default'" plain @click="toggleFavorite"><el-icon><Star /></el-icon>{{ favorited ? '已收藏' : '收藏' }}</el-button>
                <el-button plain @click="sharePost"><el-icon><Promotion /></el-icon>分享</el-button>
                <el-button plain @click="prepareReport(0, detail.id)"><el-icon><Flag /></el-icon>举报</el-button>
                <el-button v-if="!isOwner" plain @click="toggleFollow">{{ following ? '取消关注' : '关注作者' }}</el-button>
              </div>
            </div>
            <div class="campus-article-body">{{ detail.content }}</div>
            <div v-if="detail.media?.length" class="campus-media-grid">
              <div v-for="item in detail.media" :key="item.id || item.url" class="campus-media-card">
                <el-image
                  v-if="item.mediaType === 0"
                  :src="item.url"
                  :preview-src-list="mediaPreviewUrls"
                  fit="cover"
                  style="width: 100%; height: 260px"
                />
                <div v-else style="padding: 18px">
                  <p class="campus-side-item__title">视频内容</p>
                  <p class="campus-side-item__desc">{{ item.url }}</p>
                  <el-button type="primary" plain style="margin-top: 12px">
                    <a :href="item.url" target="_blank" rel="noreferrer">打开视频</a>
                  </el-button>
                </div>
              </div>
            </div>
            <div style="margin-top: 20px">
              <el-alert v-if="actionError" :title="actionError" type="error" show-icon />
              <el-alert v-if="actionSuccess" :title="actionSuccess" type="success" show-icon />
            </div>
          </el-card>

          <el-card class="campus-panel" shadow="never">
            <div class="campus-panel__header">
              <div>
                <h2 class="campus-panel__title">评论讨论</h2>
                <p class="campus-panel__desc">评论和回复统一放在阅读区下方，方便继续追踪讨论。</p>
              </div>
              <el-tag round effect="dark">共 {{ commentTotal }} 条</el-tag>
            </div>
            <el-input v-model="commentContent" type="textarea" :rows="4" resize="none" placeholder="写下你的看法..." />
            <div class="campus-inline-actions" style="margin-top: 14px">
              <el-button type="primary" :loading="commentSubmitting" @click="submitComment">发布评论</el-button>
            </div>
            <el-alert v-if="commentError" :title="commentError" type="error" show-icon style="margin-top: 14px" />
            <div style="margin-top: 22px">
              <div v-if="commentsLoading" class="campus-comment-list"><el-skeleton v-for="index in 3" :key="index" animated :rows="4" /></div>
              <div v-else-if="comments.length" class="campus-comment-list">
                <div v-for="item in comments" :key="item.id" class="campus-comment">
                  <div class="campus-comment__top">
                    <div>
                      <strong>用户 {{ item.userId }}</strong>
                      <p class="campus-muted" style="margin-top: 4px">{{ item.createdAt }}</p>
                    </div>
                    <el-tag round effect="plain">主评论</el-tag>
                  </div>
                  <p class="campus-comment__content">{{ item.content }}</p>
                  <div class="campus-comment__actions">
                    <div class="campus-inline-actions">
                      <el-button plain @click="toggleCommentLike(item)">赞 {{ item.likeCount ?? 0 }}</el-button>
                      <el-button plain @click="toggleReplies(item.id)">{{ replyMap[item.id]?.expanded ? '收起回复' : '查看回复' }}</el-button>
                      <el-button plain @click="prepareReport(1, item.id)">举报</el-button>
                    </div>
                    <el-button v-if="currentUser?.id === item.userId" type="danger" plain @click="deleteComment(item.id)">删除</el-button>
                  </div>
                  <div v-if="replyMap[item.id]?.expanded" class="campus-reply-list">
                    <el-alert v-if="replyMap[item.id]?.error" :title="replyMap[item.id].error" type="error" show-icon />
                    <el-skeleton v-if="replyMap[item.id]?.loading" animated :rows="2" />
                    <template v-else-if="replyMap[item.id]?.list?.length">
                      <div v-for="reply in replyMap[item.id].list" :key="reply.id" class="campus-reply-item">
                        <strong>用户 {{ reply.userId }}</strong>
                        <p class="campus-muted" style="margin-top: 4px">{{ reply.createdAt }}</p>
                        <p class="campus-comment__content" style="margin-bottom: 0">{{ reply.content }}</p>
                        <div class="campus-inline-actions" style="margin-top: 12px">
                          <el-button plain @click="toggleCommentLike(reply)">赞 {{ reply.likeCount ?? 0 }}</el-button>
                          <el-button plain @click="prepareReport(1, reply.id)">举报</el-button>
                          <el-button v-if="currentUser?.id === reply.userId" type="danger" plain @click="deleteComment(reply.id)">删除</el-button>
                        </div>
                      </div>
                    </template>
                    <el-empty v-else description="暂无回复" :image-size="56" />
                    <div class="campus-comment__reply-form">
                      <el-input v-model="replyDrafts[item.id]" placeholder="回复这条评论..." style="flex: 1" />
                      <el-button type="primary" plain @click="submitReply(item.id)">回复</el-button>
                    </div>
                  </div>
                </div>
              </div>
              <el-empty v-else description="还没有评论，成为第一个参与讨论的人。" />
            </div>
            <div v-if="commentTotal > commentSize" style="margin-top: 20px">
              <el-pagination
                background
                layout="prev, pager, next, ->, total"
                :current-page="commentPage"
                :page-size="commentSize"
                :total="commentTotal"
                @current-change="loadComments"
              />
            </div>
          </el-card>
        </template>
      </div>

      <aside>
        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">互动数据</h2>
              <p class="campus-panel__desc">阅读时同步看到反馈热度。</p>
            </div>
          </div>
          <el-alert v-if="statsError" :title="statsError" type="error" show-icon style="margin-bottom: 14px" />
          <div class="campus-stat-grid">
            <div class="campus-stat-box"><p class="campus-stat-box__label">点赞</p><p class="campus-stat-box__value">{{ statsLoading ? '...' : stats?.likeCount ?? detail?.likeCount ?? 0 }}</p></div>
            <div class="campus-stat-box"><p class="campus-stat-box__label">评论</p><p class="campus-stat-box__value">{{ statsLoading ? '...' : stats?.commentCount ?? detail?.commentCount ?? 0 }}</p></div>
            <div class="campus-stat-box"><p class="campus-stat-box__label">收藏</p><p class="campus-stat-box__value">{{ statsLoading ? '...' : stats?.favoriteCount ?? detail?.favoriteCount ?? 0 }}</p></div>
            <div class="campus-stat-box"><p class="campus-stat-box__label">浏览</p><p class="campus-stat-box__value">{{ statsLoading ? '...' : stats?.viewCount ?? detail?.viewCount ?? 0 }}</p></div>
          </div>
          <div class="campus-side-list" style="margin-top: 18px">
            <div class="campus-side-item">
              <p class="campus-side-item__title">内容摘要</p>
              <p class="campus-side-item__desc">{{ formatSnippet(detail?.content, 96) }}</p>
            </div>
          </div>
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">相关推荐</h2>
              <p class="campus-panel__desc">沿着同一兴趣继续浏览。</p>
            </div>
            <el-button plain @click="loadRelated">刷新</el-button>
          </div>
          <el-alert v-if="relatedError" :title="relatedError" type="error" show-icon style="margin-bottom: 14px" />
          <div v-if="relatedLoading" class="campus-side-list"><el-skeleton v-for="index in 2" :key="index" animated :rows="3" /></div>
          <div v-else-if="related.length" class="campus-side-list">
            <PostCard v-for="item in related" :key="item.id" :post="item" badge="相关推荐" compact :show-status="false" />
          </div>
          <el-empty v-else description="暂无相关推荐" />
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">内容反馈</h2>
              <p class="campus-panel__desc">对违规内容或用户快速提交处理。</p>
            </div>
          </div>
          <div class="campus-side-list">
            <div class="campus-side-item">
              <p class="campus-side-item__title">举报当前内容</p>
              <p class="campus-side-item__desc">支持不实信息、广告、辱骂等反馈。</p>
              <el-button type="danger" plain @click="prepareReport(0, detail?.id)">提交举报</el-button>
            </div>
            <div v-if="!isOwner" class="campus-side-item">
              <p class="campus-side-item__title">举报作者</p>
              <p class="campus-side-item__desc">遇到恶意骚扰或违规行为时使用。</p>
              <el-button plain @click="prepareReport(2, detail?.userId)">举报作者</el-button>
            </div>
          </div>
        </el-card>
      </aside>
    </div>

    <el-drawer v-model="showReportForm" title="举报反馈" size="420px">
      <div style="display: flex; flex-direction: column; gap: 16px">
        <el-alert v-if="reportTarget.targetId" :title="`举报对象 ID：${reportTarget.targetId}`" type="warning" show-icon />
        <el-select v-model="reportReason" placeholder="请选择举报原因">
          <el-option v-for="item in reportReasons" :key="item" :label="item" :value="item" />
        </el-select>
        <el-input v-model="reportDetail" type="textarea" :rows="5" resize="none" placeholder="补充说明举报细节，可选" />
        <el-button type="danger" :loading="reportLoading" @click="submitReport">提交举报</el-button>
        <el-alert v-if="reportError" :title="reportError" type="error" show-icon />
        <el-alert v-if="reportSuccess" :title="reportSuccess" type="success" show-icon />
      </div>
    </el-drawer>
  </UserShell>
</template>
