import { createRouter, createWebHistory } from 'vue-router'
import AuthLoginView from '../views/AuthLoginView.vue'
import AuthRegisterView from '../views/AuthRegisterView.vue'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'

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
  ],
})

export default router
