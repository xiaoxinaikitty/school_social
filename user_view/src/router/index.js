import { createRouter, createWebHistory } from 'vue-router'
import AuthLoginView from '../views/AuthLoginView.vue'
import AuthRegisterView from '../views/AuthRegisterView.vue'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'
import PostCreateView from '../views/PostCreateView.vue'
import PostDetailView from '../views/PostDetailView.vue'
import PostEditView from '../views/PostEditView.vue'
import SocialView from '../views/SocialView.vue'
import AdminHomeView from '../views/AdminHomeView.vue'
import AdminReportsView from '../views/AdminReportsView.vue'
import AdminOpsView from '../views/AdminOpsView.vue'
import AdminStatsView from '../views/AdminStatsView.vue'
import AdminAuditView from '../views/AdminAuditView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: AuthLoginView,
    },
    {
      path: '/register',
      name: 'register',
      component: AuthRegisterView,
    },
    {
      path: '/home',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
    },
    {
      path: '/posts/create',
      name: 'post-create',
      component: PostCreateView,
    },
    {
      path: '/posts/:id',
      name: 'post-detail',
      component: PostDetailView,
    },
    {
      path: '/posts/:id/edit',
      name: 'post-edit',
      component: PostEditView,
    },
    {
      path: '/social',
      name: 'social',
      component: SocialView,
    },
    {
      path: '/admin',
      name: 'admin-home',
      component: AdminHomeView,
    },
    {
      path: '/admin/audit',
      name: 'admin-audit',
      component: AdminAuditView,
    },
    {
      path: '/admin/reports',
      name: 'admin-reports',
      component: AdminReportsView,
    },
    {
      path: '/admin/ops',
      name: 'admin-ops',
      component: AdminOpsView,
    },
    {
      path: '/admin/stats',
      name: 'admin-stats',
      component: AdminStatsView,
    },
  ],
})

router.beforeEach((to) => {
  if (to.path.startsWith('/admin')) {
    const role = localStorage.getItem('auth_role')
    if (role !== 'admin') {
      return '/home'
    }
  }
  return true
})

export default router
