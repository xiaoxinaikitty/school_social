import { createRouter, createWebHistory } from 'vue-router'
import AuthLoginView from '../views/AuthLoginView.vue'
import AuthRegisterView from '../views/AuthRegisterView.vue'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'
import PostCreateView from '../views/PostCreateView.vue'
import PostDetailView from '../views/PostDetailView.vue'
import PostEditView from '../views/PostEditView.vue'

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
  ],
})

export default router
