<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { EditPen, Star, Tickets, User } from '@element-plus/icons-vue'
import { apiFetch, apiUpload } from '../utils/api'
import { formatSnippet, getGenderLabel, getInitial } from '../utils/user'
import UserShell from '../components/user/UserShell.vue'
import PostCard from '../components/user/PostCard.vue'

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

const availableTags = ref([])
const selectedTagIds = ref([])
const loadingTags = ref(false)
const tagsError = ref('')

const activeTab = ref('profile')
const pageSize = 6
const postsPage = ref(1)
const favoritesPage = ref(1)
const commentsPage = ref(1)
const postsData = ref({ page: 1, total: 0, list: [] })
const favoritesData = ref({ page: 1, total: 0, list: [] })
const commentsData = ref({ page: 1, total: 0, list: [] })
const loadingProfile = ref(false)
const savingProfile = ref(false)
const profileError = ref('')
const profileSuccess = ref('')
const loadingPosts = ref(false)
const loadingFavorites = ref(false)
const loadingComments = ref(false)
const feedError = ref('')
const avatarInputRef = ref(null)
const uploadingAvatar = ref(false)
const avatarUploadError = ref('')
const avatarUploadMessage = ref('')

const socialTab = ref('following')
const followStats = ref({ followingCount: 0, followerCount: 0 })
const followingData = ref({ page: 1, total: 0, list: [] })
const followersData = ref({ page: 1, total: 0, list: [] })
const followingPage = ref(1)
const followersPage = ref(1)
const loadingFollowing = ref(false)
const loadingFollowers = ref(false)
const socialError = ref('')

const displayName = computed(() => profile.value.username || '同学')
const accountStatus = computed(() =>
  profile.value.status === 0
    ? { label: '账号正常', type: 'success' }
    : { label: '账号受限', type: 'danger' },
)
const avatarText = computed(() => getInitial(displayName.value, '校'))
const completionRate = computed(() => {
  const fields = ['email', 'phone', 'avatarUrl', 'birthday', 'school', 'college', 'grade', 'bio']
  const filled = fields.filter((key) => profile.value[key]).length
  return Math.round((filled / fields.length) * 100)
})

const authGuard = (res) => {
  if (res.status === 401) {
    router.push('/login')
    return true
  }
  return false
}

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
  if (!user.value?.id) return router.push('/login')
  loadingProfile.value = true
  profileError.value = ''
  try {
    const res = await apiFetch(`/api/users/${user.value.id}`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      profileError.value = data.message || '获取资料失败。'
      return
    }
    syncProfile(data.data)
    localStorage.setItem('auth_user', JSON.stringify(data.data))
  } catch (error) {
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
    const res = await apiFetch(`/api/users/${profile.value.id}`, { method: 'PUT', body: JSON.stringify(payload) })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      profileError.value = data.message || '更新失败，请检查输入。'
      return
    }
    syncProfile(data.data)
    localStorage.setItem('auth_user', JSON.stringify(data.data))
    profileSuccess.value = '资料已更新。'
  } catch (error) {
    profileError.value = '网络错误，无法更新资料。'
  } finally {
    savingProfile.value = false
  }
}

const triggerAvatarUpload = () => {
  avatarUploadError.value = ''
  avatarUploadMessage.value = ''
  if (avatarInputRef.value) {
    avatarInputRef.value.click()
  }
}

const clearAvatar = () => {
  profile.value.avatarUrl = ''
  avatarUploadError.value = ''
  avatarUploadMessage.value = '头像已清空，点击“保存资料”后写入数据库。'
}

const handleAvatarChange = async (event) => {
  const [file] = Array.from(event.target?.files || [])
  if (!file) return
  if (!file.type.startsWith('image/')) {
    avatarUploadError.value = '请选择图片文件作为头像。'
    if (event.target) {
      event.target.value = ''
    }
    return
  }

  uploadingAvatar.value = true
  profileError.value = ''
  profileSuccess.value = ''
  avatarUploadError.value = ''
  avatarUploadMessage.value = ''

  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await apiUpload('/api/upload/avatar', formData)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      avatarUploadError.value = data.message || '头像上传失败。'
      return
    }
    profile.value.avatarUrl = data.data?.url || ''
    avatarUploadMessage.value = '头像已上传，点击“保存资料”后写入数据库。'
  } catch (error) {
    avatarUploadError.value = '网络错误，头像上传失败。'
  } finally {
    uploadingAvatar.value = false
    if (event.target) {
      event.target.value = ''
    }
  }
}

const loadAvailableTags = async () => {
  loadingTags.value = true
  tagsError.value = ''
  try {
    const res = await apiFetch('/api/tags')
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagsError.value = data.message || '获取标签列表失败。'
      return
    }
    availableTags.value = data.data || []
  } catch (error) {
    tagsError.value = '网络错误，无法获取标签列表。'
  } finally {
    loadingTags.value = false
  }
}

const loadUserTags = async () => {
  if (!user.value?.id) return
  try {
    const res = await apiFetch(`/api/users/${user.value.id}/tags`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagsError.value = data.message || '获取标签失败。'
      return
    }
    selectedTagIds.value = (data.data || []).map((item) => item.tagId)
  } catch (error) {
    tagsError.value = '网络错误，无法获取标签。'
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
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      tagsError.value = data.message || '更新标签失败。'
    }
  } catch (error) {
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
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      feedError.value = data.message || '获取发布失败。'
      return
    }
    postsData.value = data.data
    postsPage.value = data.data.page
  } catch (error) {
    feedError.value = '网络错误，无法获取发布。'
  } finally {
    loadingPosts.value = false
  }
}

const loadFavorites = async (page = 1) => {
  if (!user.value?.id) return
  loadingFavorites.value = true
  feedError.value = ''
  try {
    const res = await apiFetch(`/api/users/${user.value.id}/favorites?page=${page}&size=${pageSize}`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      feedError.value = data.message || '获取收藏失败。'
      return
    }
    favoritesData.value = data.data
    favoritesPage.value = data.data.page
  } catch (error) {
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
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      feedError.value = data.message || '获取评论失败。'
      return
    }
    commentsData.value = data.data
    commentsPage.value = data.data.page
  } catch (error) {
    feedError.value = '网络错误，无法获取评论。'
  } finally {
    loadingComments.value = false
  }
}

const loadFollowStats = async () => {
  socialError.value = ''
  try {
    const res = await apiFetch('/api/follows/stats')
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      socialError.value = data.message || '获取关注统计失败。'
      return
    }
    followStats.value = data.data || { followingCount: 0, followerCount: 0 }
  } catch (error) {
    socialError.value = '网络错误，无法获取关注统计。'
  }
}

const loadFollowing = async (page = 1) => {
  loadingFollowing.value = true
  socialError.value = ''
  try {
    const res = await apiFetch(`/api/follows/following?page=${page}&size=6`)
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      socialError.value = data.message || '获取关注列表失败。'
      return
    }
    followingData.value = data.data
    followingPage.value = data.data.page
  } catch (error) {
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
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      socialError.value = data.message || '获取粉丝列表失败。'
      return
    }
    followersData.value = data.data
    followersPage.value = data.data.page
  } catch (error) {
    socialError.value = '网络错误，无法获取粉丝列表。'
  } finally {
    loadingFollowers.value = false
  }
}

const followUser = async (userId) => {
  if (!userId) return
  try {
    const res = await apiFetch(`/api/follows/${userId}`, { method: 'POST' })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      socialError.value = data.message || '关注失败。'
      return
    }
    await Promise.all([loadFollowStats(), loadFollowing(followingPage.value || 1), loadFollowers(followersPage.value || 1)])
  } catch (error) {
    socialError.value = '网络错误，关注失败。'
  }
}

const unfollowUser = async (userId) => {
  if (!userId) return
  try {
    const res = await apiFetch(`/api/follows/${userId}`, { method: 'DELETE' })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      socialError.value = data.message || '取消关注失败。'
      return
    }
    await Promise.all([loadFollowStats(), loadFollowing(followingPage.value || 1), loadFollowers(followersPage.value || 1)])
  } catch (error) {
    socialError.value = '网络错误，取消关注失败。'
  }
}

const switchContentTab = (tab) => {
  activeTab.value = tab
  if (tab === 'posts') loadPosts(postsPage.value || 1)
  if (tab === 'favorites') loadFavorites(favoritesPage.value || 1)
  if (tab === 'comments') loadComments(commentsPage.value || 1)
}

const switchSocialTab = (tab) => {
  socialTab.value = tab
  if (tab === 'following') loadFollowing(followingPage.value || 1)
  if (tab === 'followers') loadFollowers(followersPage.value || 1)
}

const deletePost = async (postId) => {
  try {
    const res = await apiFetch(`/api/posts/${postId}`, { method: 'DELETE' })
    if (authGuard(res)) return
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      feedError.value = data.message || '删除失败。'
      return
    }
    loadPosts(postsPage.value || 1)
  } catch (error) {
    feedError.value = '网络错误，无法删除。'
  }
}

onMounted(() => {
  try {
    const savedUser = localStorage.getItem('auth_user')
    if (savedUser) user.value = JSON.parse(savedUser)
  } catch (error) {
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
  <UserShell section="profile">
    <section class="campus-hero">
      <span class="campus-hero__eyebrow"><el-icon><User /></el-icon>个人中心</span>
      <h1 class="campus-hero__title">{{ displayName }} 的内容空间</h1>
      <p class="campus-hero__subtitle">资料、偏好、内容和社交关系现在被拆成更清晰的浏览结构，优先服务于你的个人内容资产管理。</p>
      <div class="campus-hero__chips">
        <el-tag round>{{ profile.school || '未设置学校' }}</el-tag>
        <el-tag round type="success">{{ profile.college || '未设置学院' }}</el-tag>
        <el-tag round type="info">资料完整度 {{ completionRate }}%</el-tag>
      </div>
    </section>

    <div class="campus-profile-grid">
      <div>
        <el-card class="campus-panel" shadow="never">
          <el-tabs :model-value="activeTab" @tab-change="switchContentTab">
            <el-tab-pane label="资料编辑" name="profile" />
            <el-tab-pane label="我的发布" name="posts" />
            <el-tab-pane label="我的收藏" name="favorites" />
            <el-tab-pane label="我的评论" name="comments" />
          </el-tabs>

          <template v-if="activeTab === 'profile'">
            <div class="campus-panel__header">
              <div>
                <h2 class="campus-panel__title">基础资料</h2>
                <p class="campus-panel__desc">完善个人档案后，推荐流会更准确。</p>
              </div>
              <el-button :loading="loadingProfile" plain @click="loadProfile">刷新资料</el-button>
            </div>

            <el-form label-position="top">
              <el-row :gutter="16">
                <el-col :md="12" :xs="24"><el-form-item label="用户名"><el-input v-model="profile.username" disabled /></el-form-item></el-col>
                <el-col :md="12" :xs="24"><el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item></el-col>
                <el-col :md="12" :xs="24"><el-form-item label="手机号"><el-input v-model="profile.phone" /></el-form-item></el-col>
                <el-col :md="12" :xs="24">
                  <el-form-item label="头像上传">
                    <div class="avatar-upload-panel">
                      <div class="avatar-upload-actions">
                        <el-button plain :loading="uploadingAvatar" @click="triggerAvatarUpload">
                          {{ uploadingAvatar ? '上传中...' : '选择本地图片' }}
                        </el-button>
                        <el-button plain type="danger" :disabled="uploadingAvatar || !profile.avatarUrl" @click="clearAvatar">
                          移除头像
                        </el-button>
                      </div>
                      <input
                        ref="avatarInputRef"
                        class="file-input"
                        type="file"
                        accept="image/*"
                        @change="handleAvatarChange"
                      />
                      <span class="field-tip">支持从本地选择图片，上传后点击“保存资料”写入数据库。</span>
                      <el-input :model-value="profile.avatarUrl" readonly placeholder="尚未上传头像" />
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :md="12" :xs="24"><el-form-item label="性别"><el-select v-model="profile.gender"><el-option label="未设置" value="" /><el-option label="保密" value="0" /><el-option label="男" value="1" /><el-option label="女" value="2" /></el-select></el-form-item></el-col>
                <el-col :md="12" :xs="24"><el-form-item label="生日"><el-date-picker v-model="profile.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" /></el-form-item></el-col>
                <el-col :md="12" :xs="24"><el-form-item label="学校"><el-input v-model="profile.school" /></el-form-item></el-col>
                <el-col :md="12" :xs="24"><el-form-item label="学院"><el-input v-model="profile.college" /></el-form-item></el-col>
                <el-col :md="12" :xs="24"><el-form-item label="年级"><el-input v-model="profile.grade" /></el-form-item></el-col>
                <el-col :md="12" :xs="24"><el-form-item label="个性签名"><el-input v-model="profile.bio" /></el-form-item></el-col>
              </el-row>
            </el-form>

            <div class="campus-inline-actions" style="margin-top: 6px">
              <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
            </div>
            <el-alert v-if="profileError" :title="profileError" type="error" show-icon style="margin-top: 14px" />
            <el-alert v-if="profileSuccess" :title="profileSuccess" type="success" show-icon style="margin-top: 14px" />
            <el-alert v-if="avatarUploadError" :title="avatarUploadError" type="error" show-icon style="margin-top: 14px" />
            <el-alert v-if="avatarUploadMessage" :title="avatarUploadMessage" type="success" show-icon style="margin-top: 14px" />

            <div class="campus-panel__header" style="margin-top: 24px">
              <div>
                <h2 class="campus-panel__title">兴趣标签</h2>
                <p class="campus-panel__desc">这些标签决定你在首页会优先刷到什么。</p>
              </div>
              <el-button plain :loading="loadingTags" @click="saveUserTags">保存标签</el-button>
            </div>
            <el-checkbox-group v-model="selectedTagIds" style="display: flex; flex-wrap: wrap; gap: 12px">
              <el-checkbox-button v-for="tag in availableTags" :key="tag.id" :value="tag.id">
                {{ tag.name }}
              </el-checkbox-button>
            </el-checkbox-group>
            <el-empty v-if="!loadingTags && !availableTags.length" description="标签库为空，请先配置标签。" />
            <el-alert v-if="tagsError" :title="tagsError" type="error" show-icon style="margin-top: 14px" />
          </template>

          <template v-if="activeTab === 'posts'">
            <el-alert v-if="feedError" :title="feedError" type="error" show-icon style="margin-bottom: 14px" />
            <div v-if="loadingPosts" class="campus-card-grid"><el-skeleton v-for="index in 2" :key="index" animated :rows="4" /></div>
            <div v-else-if="postsData.list?.length" class="campus-post-grid">
              <PostCard v-for="post in postsData.list" :key="post.id" :post="post" badge="我的发布">
                <template #actions="{ post: item }">
                  <el-button plain @click="router.push(`/posts/${item.id}/edit`)">编辑</el-button>
                  <el-button type="danger" plain @click="deletePost(item.id)">删除</el-button>
                </template>
              </PostCard>
            </div>
            <el-empty v-else description="暂时还没有发布内容。" />
            <div v-if="postsData.total > pageSize" style="margin-top: 20px">
              <el-pagination background layout="prev, pager, next, ->, total" :current-page="postsPage" :page-size="pageSize" :total="postsData.total" @current-change="loadPosts" />
            </div>
          </template>

          <template v-if="activeTab === 'favorites'">
            <el-alert v-if="feedError" :title="feedError" type="error" show-icon style="margin-bottom: 14px" />
            <div v-if="loadingFavorites" class="campus-card-grid"><el-skeleton v-for="index in 2" :key="index" animated :rows="4" /></div>
            <div v-else-if="favoritesData.list?.length" class="campus-post-grid">
              <PostCard v-for="post in favoritesData.list" :key="post.id" :post="post" badge="我的收藏" />
            </div>
            <el-empty v-else description="还没有收藏内容。" />
            <div v-if="favoritesData.total > pageSize" style="margin-top: 20px">
              <el-pagination background layout="prev, pager, next, ->, total" :current-page="favoritesPage" :page-size="pageSize" :total="favoritesData.total" @current-change="loadFavorites" />
            </div>
          </template>

          <template v-if="activeTab === 'comments'">
            <el-alert v-if="feedError" :title="feedError" type="error" show-icon style="margin-bottom: 14px" />
            <div v-if="loadingComments" class="campus-side-list"><el-skeleton v-for="index in 3" :key="index" animated :rows="3" /></div>
            <div v-else-if="commentsData.list?.length" class="campus-side-list">
              <div v-for="comment in commentsData.list" :key="comment.id" class="campus-side-item">
                <p class="campus-side-item__title">评论记录</p>
                <p class="campus-side-item__desc">{{ formatSnippet(comment.content, 140) }}</p>
                <p class="campus-muted" style="margin-top: 10px">{{ comment.createdAt || '-' }}</p>
              </div>
            </div>
            <el-empty v-else description="暂无评论记录。" />
            <div v-if="commentsData.total > pageSize" style="margin-top: 20px">
              <el-pagination background layout="prev, pager, next, ->, total" :current-page="commentsPage" :page-size="pageSize" :total="commentsData.total" @current-change="loadComments" />
            </div>
          </template>
        </el-card>
      </div>

      <aside>
        <el-card class="campus-panel" shadow="never">
          <div class="campus-profile-card">
            <el-avatar :size="84" :src="profile.avatarUrl">{{ avatarText }}</el-avatar>
            <div>
              <h2 class="campus-panel__title" style="margin-bottom: 4px">{{ displayName }}</h2>
              <p class="campus-muted">{{ profile.bio || '这个人很低调，还没有留下签名。' }}</p>
              <div class="campus-profile-card__meta">
                <el-tag round :type="accountStatus.type">{{ accountStatus.label }}</el-tag>
                <el-tag round type="info">{{ getGenderLabel(profile.gender) }}</el-tag>
                <el-tag round>{{ profile.grade || '未设置年级' }}</el-tag>
              </div>
            </div>
          </div>
          <div class="campus-stat-grid" style="margin-top: 22px">
            <div class="campus-stat-box"><p class="campus-stat-box__label">资料完整度</p><p class="campus-stat-box__value">{{ completionRate }}%</p></div>
            <div class="campus-stat-box"><p class="campus-stat-box__label">最近登录</p><p class="campus-stat-box__value" style="font-size: 18px">{{ profile.lastLoginAt || '暂无' }}</p></div>
          </div>
          <el-progress :percentage="completionRate" :stroke-width="10" style="margin-top: 20px" />
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-panel__header">
            <div>
              <h2 class="campus-panel__title">社交关系</h2>
              <p class="campus-panel__desc">把关注和粉丝放在右侧持续可见。</p>
            </div>
          </div>
          <div class="campus-stat-grid">
            <div class="campus-stat-box"><p class="campus-stat-box__label">关注</p><p class="campus-stat-box__value">{{ followStats.followingCount }}</p></div>
            <div class="campus-stat-box"><p class="campus-stat-box__label">粉丝</p><p class="campus-stat-box__value">{{ followStats.followerCount }}</p></div>
          </div>
          <el-alert v-if="socialError" :title="socialError" type="error" show-icon style="margin-top: 14px" />
          <el-tabs :model-value="socialTab" style="margin-top: 18px" @tab-change="switchSocialTab">
            <el-tab-pane label="我关注的" name="following" />
            <el-tab-pane label="我的粉丝" name="followers" />
          </el-tabs>
          <div v-if="socialTab === 'following'" class="campus-list-block">
            <div v-if="loadingFollowing"><el-skeleton v-for="index in 2" :key="index" animated :rows="2" /></div>
            <template v-else-if="followingData.list?.length">
              <div v-for="item in followingData.list" :key="item.id" class="campus-user-row">
                <div class="campus-user-row__meta">
                  <el-avatar>{{ getInitial(item.username, '校') }}</el-avatar>
                  <div>
                    <p class="campus-side-item__title">{{ item.username || '未命名用户' }}</p>
                    <p class="campus-muted">{{ item.school || '未填写学校' }} / {{ item.college || '未填写学院' }}</p>
                  </div>
                </div>
                <el-button type="danger" plain @click="unfollowUser(item.id)">取消关注</el-button>
              </div>
            </template>
            <el-empty v-else description="暂无关注。" />
          </div>
          <div v-if="socialTab === 'followers'" class="campus-list-block">
            <div v-if="loadingFollowers"><el-skeleton v-for="index in 2" :key="index" animated :rows="2" /></div>
            <template v-else-if="followersData.list?.length">
              <div v-for="item in followersData.list" :key="item.id" class="campus-user-row">
                <div class="campus-user-row__meta">
                  <el-avatar>{{ getInitial(item.username, '校') }}</el-avatar>
                  <div>
                    <p class="campus-side-item__title">{{ item.username || '未命名用户' }}</p>
                    <p class="campus-muted">{{ item.school || '未填写学校' }} / {{ item.college || '未填写学院' }}</p>
                  </div>
                </div>
                <el-button plain @click="followUser(item.id)">关注</el-button>
              </div>
            </template>
            <el-empty v-else description="暂无粉丝。" />
          </div>
        </el-card>

        <el-card class="campus-panel" shadow="never">
          <div class="campus-side-list">
            <div class="campus-side-item">
              <p class="campus-side-item__title">快速发布</p>
              <p class="campus-side-item__desc">从个人页直接发新内容，延续创作路径。</p>
              <el-button type="primary" plain @click="router.push('/posts/create')"><el-icon><EditPen /></el-icon>去发布</el-button>
            </div>
            <div class="campus-side-item">
              <p class="campus-side-item__title">查看收藏</p>
              <p class="campus-side-item__desc">快速跳到你沉淀下来的内容资产。</p>
              <el-button plain @click="switchContentTab('favorites')"><el-icon><Star /></el-icon>打开收藏</el-button>
            </div>
            <div class="campus-side-item">
              <p class="campus-side-item__title">回到内容广场</p>
              <p class="campus-side-item__desc">继续浏览校园内容流。</p>
              <el-button plain @click="router.push('/home')"><el-icon><Tickets /></el-icon>继续浏览</el-button>
            </div>
          </div>
        </el-card>
      </aside>
    </div>
  </UserShell>
</template>
