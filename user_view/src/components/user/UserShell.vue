<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Plus, User, House, ChatDotRound } from '@element-plus/icons-vue'
import { getInitial } from '../../utils/user'

const props = defineProps({
  section: {
    type: String,
    default: 'home',
  },
})

const route = useRoute()
const router = useRouter()
const currentUser = ref(null)
const currentRole = ref('user')

const navItems = [
  { key: 'home', label: '内容广场', path: '/home', icon: House },
  { key: 'social', label: '互动中心', path: '/social', icon: ChatDotRound },
  { key: 'profile', label: '我的空间', path: '/profile', icon: User },
]

const avatarText = computed(() => getInitial(currentUser.value?.username, '校'))
const showAdminEntry = computed(() => currentRole.value === 'admin')

const navigate = (path) => {
  if (route.path === path) return
  router.push(path)
}

onMounted(() => {
  try {
    const savedUser = localStorage.getItem('auth_user')
    if (savedUser) {
      currentUser.value = JSON.parse(savedUser)
    }
    const savedRole = localStorage.getItem('auth_role')
    if (savedRole) {
      currentRole.value = savedRole
    }
  } catch {
    currentUser.value = null
  }
})
</script>

<template>
  <div class="user-shell">
    <div class="user-shell__backdrop"></div>
    <header class="user-shell__topbar">
      <div class="user-shell__brand" @click="navigate('/home')">
        <div class="user-shell__brand-mark">脉</div>
        <div>
          <p class="user-shell__brand-title">校园社交平台</p>
          <p class="user-shell__brand-subtitle">Campus Social Feed</p>
        </div>
      </div>

      <nav class="user-shell__nav">
        <button
          v-for="item in navItems"
          :key="item.key"
          class="user-shell__nav-item"
          :class="{ 'is-active': props.section === item.key }"
          type="button"
          @click="navigate(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="user-shell__actions">
        <el-button class="user-shell__ghost" plain @click="navigate('/social')">
          <el-icon><Bell /></el-icon>
          消息
        </el-button>
        <el-button type="primary" @click="navigate('/posts/create')">
          <el-icon><Plus /></el-icon>
          发布内容
        </el-button>
        <el-button
          v-if="showAdminEntry"
          class="user-shell__ghost"
          plain
          @click="navigate('/admin/stats')"
        >
          管理后台
        </el-button>
        <button class="user-shell__avatar" type="button" @click="navigate('/profile')">
          <span>{{ avatarText }}</span>
        </button>
      </div>
    </header>

    <main class="user-shell__body">
      <slot />
    </main>
  </div>
</template>
