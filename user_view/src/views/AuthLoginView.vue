<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const role = ref('user')
const account = ref('')
const password = ref('')
const loginType = ref('auto')
const loading = ref(false)
const error = ref('')
const success = ref('')

const router = useRouter()

const setRole = (value) => {
  role.value = value
}

const submit = async () => {
  error.value = ''
  success.value = ''
  if (!account.value || !password.value) {
    error.value = '请输入账号和密码。'
    return
  }
  loading.value = true
  try {
    const payload = {
      account: account.value.trim(),
      password: password.value,
    }
    if (loginType.value && loginType.value !== 'auto') {
      payload.loginType = loginType.value
    }
    const res = await apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '登录失败。'
      return
    }
    if (data.data?.token) {
      localStorage.setItem('auth_token', data.data.token)
    }
    if (data.data?.user) {
      localStorage.setItem('auth_user', JSON.stringify(data.data.user))
    }
    localStorage.setItem('auth_role', role.value)
    success.value = role.value === 'admin'
      ? '管理员登录成功，已保存登录凭证。'
      : '登录成功，已保存登录凭证。'
    setTimeout(() => {
      router.push('/home')
    }, 300)
  } catch (error) {
    error.value = '网络错误，请稍后再试。'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-hero">
        <div class="brand">
          <span class="brand-dot"></span>
          <span class="brand-name">校园脉搏</span>
        </div>
        <p class="eyebrow">内容驱动的校园社交网络</p>
        <h1 class="hero-title">欢迎回到更懂你的校园内容流。</h1>
        <p class="hero-copy">
          登录后即可查看认证内容、热门话题与个性化推荐。
        </p>
        <div class="role-toggle" role="tablist" aria-label="选择角色">
          <button
            class="role-pill"
            :class="{ active: role === 'user' }"
            type="button"
            @click="setRole('user')"
          >
            学生
          </button>
          <button
            class="role-pill"
            :class="{ active: role === 'admin' }"
            type="button"
            @click="setRole('admin')"
          >
            管理员
          </button>
        </div>
        <div class="hero-cards">
          <div class="mini-card" style="animation-delay: 0.05s">
            <p class="mini-title">实时话题</p>
            <p class="mini-note">校园热议内容每小时更新。</p>
          </div>
          <div class="mini-card" style="animation-delay: 0.12s">
            <p class="mini-title">身份认证</p>
            <p class="mini-note">校园身份加持，互动更安心。</p>
          </div>
          <div class="mini-card" style="animation-delay: 0.2s">
            <p class="mini-title">智能推荐</p>
            <p class="mini-note">兴趣驱动推荐，内容更贴近。</p>
          </div>
        </div>
      </section>

      <main class="auth-panel">
        <div class="panel-head">
          <h2>登录账号</h2>
          <p>支持用户名 / 邮箱 / 手机号登录，可在上方切换角色。</p>
        </div>

        <form class="auth-form" @submit.prevent="submit">
          <label class="field">
            <span>账号</span>
            <input
              v-model="account"
              type="text"
              placeholder="用户名 / 邮箱 / 手机号"
              autocomplete="username"
              required
            />
          </label>
          <label class="field">
            <span>密码</span>
            <input
              v-model="password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              required
            />
          </label>
          <label class="field">
            <span>登录方式</span>
            <select v-model="loginType">
              <option value="auto">自动识别</option>
              <option value="username">用户名</option>
              <option value="email">邮箱</option>
              <option value="phone">手机号</option>
            </select>
          </label>

          <div class="form-meta">
            <span>当前角色：<strong>{{ role === 'admin' ? '管理员' : '学生' }}</strong></span>
            <span class="muted">登录凭证将保存到本地。</span>
          </div>

          <button class="primary-btn" type="submit" :disabled="loading">
            {{ loading ? '正在登录...' : '登录' }}
          </button>

          <p v-if="error" class="form-alert error">{{ error }}</p>
          <p v-if="success" class="form-alert success">{{ success }}</p>
        </form>

        <div class="panel-foot">
          <span>还没有账号？</span>
          <RouterLink class="link" to="/register">立即注册</RouterLink>
        </div>
      </main>
    </div>
  </div>
</template>
