<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const user = ref(null)
const role = ref('user')
const router = useRouter()

const displayName = computed(() => {
  if (user.value?.username) return user.value.username
  return role.value === 'admin' ? '管理员' : '同学'
})

const roleLabel = computed(() => (role.value === 'admin' ? '管理员' : '学生'))

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
})

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
          <button class="ghost-btn" type="button">去发现</button>
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
        <div class="stat-card">
          <p class="stat-label">互动提醒</p>
          <p class="stat-value">3</p>
          <p class="stat-note">点赞评论集中展示</p>
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
      <article class="module-card">
        <h3>互动社交</h3>
        <p>点赞评论、关注关系与消息提醒。</p>
        <span class="module-tag">社交连接</span>
      </article>
      <RouterLink class="module-card link-card" to="/profile">
        <h3>个人中心</h3>
        <p>个人资料、兴趣标签与内容管理。</p>
        <span class="module-tag">个人档案</span>
      </RouterLink>
    </section>

    <section class="feed-preview">
      <div class="feed-header">
        <h2>今日推荐预览</h2>
        <p>后续将接入后端推荐数据。</p>
      </div>
      <div class="feed-cards">
        <div class="feed-card">
          <h4>「科研竞赛经验分享会」</h4>
          <p>信息学院学长分享竞赛备赛经验，欢迎报名。</p>
          <span>话题：学术</span>
        </div>
        <div class="feed-card">
          <h4>「图书馆自习室预约小技巧」</h4>
          <p>热门时段预约注意事项，避免踩坑。</p>
          <span>话题：学习</span>
        </div>
        <div class="feed-card">
          <h4>「校内音乐节志愿者招募」</h4>
          <p>社团志愿报名，活动日程与福利公开。</p>
          <span>话题：校园活动</span>
        </div>
      </div>
    </section>
  </div>
</template>
