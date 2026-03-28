<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  DataAnalysis,
  DocumentChecked,
  Flag,
  HomeFilled,
  Operation,
  PriceTag,
  TrendCharts,
  UserFilled,
} from '@element-plus/icons-vue'

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: '',
  },
})

const route = useRoute()
const router = useRouter()
const adminUser = ref(null)

try {
  const saved = localStorage.getItem('auth_user')
  adminUser.value = saved ? JSON.parse(saved) : null
} catch {
  adminUser.value = null
}

const menuItems = [
  { key: 'stats', label: '数据统计', icon: TrendCharts, to: { path: '/admin/stats' } },
  { key: 'overview', label: '控制总览', icon: HomeFilled, to: { path: '/admin/overview' } },
  { key: 'audit', label: '内容审核', icon: DocumentChecked, to: { path: '/admin/audit' } },
  { key: 'reports', label: '举报处理', icon: Flag, to: { path: '/admin/reports' } },
  { key: 'ops-content', label: '内容运营', icon: Operation, to: { path: '/admin/ops', query: { tab: 'content' } } },
  { key: 'ops-users', label: '用户治理', icon: UserFilled, to: { path: '/admin/ops', query: { tab: 'users' } } },
  { key: 'ops-tags', label: '标签体系', icon: PriceTag, to: { path: '/admin/ops', query: { tab: 'tags' } } },
  { key: 'ops-announcements', label: '公告中心', icon: Bell, to: { path: '/admin/ops', query: { tab: 'announcements' } } },
  { key: 'ops-recommend', label: '推荐策略', icon: DataAnalysis, to: { path: '/admin/ops', query: { tab: 'recommend' } } },
]

const breadcrumbLabelMap = {
  '/admin/overview': '控制总览',
  '/admin/audit': '内容审核',
  '/admin/reports': '举报处理',
  '/admin/ops': '运营配置',
  '/admin/stats': '数据统计',
}

const activeMenu = computed(() => {
  if (route.path === '/admin/stats') return 'stats'
  if (route.path === '/admin/overview') return 'overview'
  if (route.path.startsWith('/admin/audit')) return 'audit'
  if (route.path === '/admin/reports') return 'reports'
  if (route.path === '/admin/ops') {
    const tab = route.query.tab || 'content'
    return `ops-${tab}`
  }
  return 'stats'
})

const breadcrumb = computed(() => {
  if (route.path.startsWith('/admin/audit/')) return '审核详情'
  return breadcrumbLabelMap[route.path] || '后台'
})

const goSite = () => {
  router.push('/home')
}

const selectMenu = (key) => {
  const item = menuItems.find((entry) => entry.key === key)
  if (item) {
    router.push(item.to)
  }
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <div class="admin-brand-mark">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div>
          <div class="admin-brand-name">校园脉搏</div>
          <div class="admin-brand-note">管理控制台</div>
        </div>
      </div>

      <div class="admin-sidebar-copy">
        <p>围绕审核、运营与统计的现代化控制台。</p>
      </div>

      <el-menu class="admin-menu" :default-active="activeMenu" @select="selectMenu">
        <el-menu-item v-for="item in menuItems" :key="item.key" :index="item.key">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="admin-sidebar-footer">
        <div class="admin-user-chip">
          <div class="admin-user-avatar">{{ adminUser?.username?.[0]?.toUpperCase() || 'A' }}</div>
          <div>
            <div class="admin-user-name">{{ adminUser?.username || '管理员' }}</div>
            <div class="admin-user-role">系统管理权限</div>
          </div>
        </div>
        <el-button class="admin-ghost-button" plain @click="goSite">返回前台</el-button>
      </div>
    </aside>

    <section class="admin-workspace">
      <header class="admin-topbar">
        <div>
          <div class="admin-breadcrumb">{{ breadcrumb }}</div>
          <h1 class="admin-page-title">{{ props.title }}</h1>
          <p v-if="props.subtitle" class="admin-page-subtitle">{{ props.subtitle }}</p>
        </div>
        <div class="admin-toolbar">
          <slot name="actions" />
        </div>
      </header>

      <main class="admin-content">
        <slot />
      </main>
    </section>
  </div>
</template>
