<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { apiFetch } from '../utils/api'

const router = useRouter()

const email = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const sending = ref(false)
const verifying = ref(false)
const resetting = ref(false)
const verified = ref(false)
const countdown = ref(0)
const error = ref('')
const success = ref('')

let timer = null

const canSendCode = computed(() => countdown.value <= 0 && !sending.value)

const startCountdown = () => {
  countdown.value = 60
  if (timer) {
    clearInterval(timer)
  }
  timer = window.setInterval(() => {
    if (countdown.value <= 1) {
      clearInterval(timer)
      timer = null
      countdown.value = 0
      return
    }
    countdown.value -= 1
  }, 1000)
}

const validateEmail = () => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())

const sendCode = async () => {
  error.value = ''
  success.value = ''
  verified.value = false
  if (!validateEmail()) {
    error.value = '请输入正确的邮箱地址。'
    return
  }
  sending.value = true
  try {
    const res = await apiFetch('/api/auth/password/forgot/send-code', {
      method: 'POST',
      body: JSON.stringify({
        email: email.value.trim(),
      }),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '验证码发送失败。'
      return
    }
    success.value = data.data || '验证码已发送，请查收邮箱。'
    startCountdown()
  } catch {
    error.value = '网络错误，请稍后再试。'
  } finally {
    sending.value = false
  }
}

const verifyCode = async () => {
  error.value = ''
  success.value = ''
  if (!validateEmail()) {
    error.value = '请输入正确的邮箱地址。'
    return
  }
  if (!/^[0-9]{6}$/.test(code.value.trim())) {
    error.value = '请输入 6 位数字验证码。'
    return
  }
  verifying.value = true
  try {
    const res = await apiFetch('/api/auth/password/forgot/verify-code', {
      method: 'POST',
      body: JSON.stringify({
        email: email.value.trim(),
        code: code.value.trim(),
      }),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      verified.value = false
      error.value = data.message || '验证码校验失败。'
      return
    }
    verified.value = true
    success.value = data.data || '验证码校验通过，请设置新密码。'
  } catch {
    error.value = '网络错误，请稍后再试。'
  } finally {
    verifying.value = false
  }
}

const resetPassword = async () => {
  error.value = ''
  success.value = ''
  if (!verified.value) {
    error.value = '请先校验验证码。'
    return
  }
  if (newPassword.value.length < 6 || newPassword.value.length > 32) {
    error.value = '新密码长度需为 6-32 位。'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = '两次输入的新密码不一致。'
    return
  }
  resetting.value = true
  try {
    const res = await apiFetch('/api/auth/password/reset', {
      method: 'POST',
      body: JSON.stringify({
        email: email.value.trim(),
        code: code.value.trim(),
        newPassword: newPassword.value,
      }),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || '密码重置失败。'
      return
    }
    success.value = data.data || '密码已重置，请重新登录。'
    setTimeout(() => {
      router.push('/login')
    }, 600)
  } catch {
    error.value = '网络错误，请稍后再试。'
  } finally {
    resetting.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-hero">
        <div class="brand">
          <span class="brand-dot"></span>
          <span class="brand-name">校园社交平台</span>
        </div>
        <p class="eyebrow">邮箱找回密码</p>
        <h1 class="hero-title">通过邮箱验证码安全重置密码。</h1>
        <p class="hero-copy">
          输入注册邮箱，完成验证码校验后即可设置新密码，旧登录状态会自动失效。
        </p>
        <div class="hero-cards">
          <div class="mini-card" style="animation-delay: 0.05s">
            <p class="mini-title">邮箱必填</p>
            <p class="mini-note">注册邮箱将作为账号安全找回入口。</p>
          </div>
          <div class="mini-card" style="animation-delay: 0.12s">
            <p class="mini-title">单次验证码</p>
            <p class="mini-note">验证码限时有效，使用后立即失效。</p>
          </div>
          <div class="mini-card" style="animation-delay: 0.2s">
            <p class="mini-title">自动下线</p>
            <p class="mini-note">重置密码后旧 token 将不能再使用。</p>
          </div>
        </div>
      </section>

      <main class="auth-panel">
        <div class="panel-head">
          <h2>重置密码</h2>
          <p>先获取验证码并完成校验，再设置新密码。</p>
        </div>

        <form class="auth-form" @submit.prevent="resetPassword">
          <label class="field">
            <span>注册邮箱</span>
            <input
              v-model="email"
              type="email"
              placeholder="name@school.edu"
              autocomplete="email"
              required
            />
          </label>

          <label class="field">
            <span>验证码</span>
            <div class="code-row">
              <input
                v-model="code"
                type="text"
                maxlength="6"
                inputmode="numeric"
                placeholder="请输入 6 位验证码"
              />
              <button class="ghost-btn" type="button" :disabled="!canSendCode" @click="sendCode">
                {{ countdown > 0 ? `${countdown}s` : (sending ? '发送中...' : '发送验证码') }}
              </button>
            </div>
          </label>

          <button class="secondary-btn" type="button" :disabled="verifying" @click="verifyCode">
            {{ verifying ? '校验中...' : (verified ? '已校验通过' : '校验验证码') }}
          </button>

          <div class="grid-2">
            <label class="field">
              <span>新密码</span>
              <input
                v-model="newPassword"
                type="password"
                placeholder="6-32 位新密码"
                minlength="6"
                maxlength="32"
                :disabled="!verified"
                required
              />
            </label>
            <label class="field">
              <span>确认新密码</span>
              <input
                v-model="confirmPassword"
                type="password"
                placeholder="再次输入新密码"
                minlength="6"
                maxlength="32"
                :disabled="!verified"
                required
              />
            </label>
          </div>

          <button class="primary-btn" type="submit" :disabled="resetting || !verified">
            {{ resetting ? '正在重置...' : '确认重置密码' }}
          </button>

          <p v-if="error" class="form-alert error">{{ error }}</p>
          <p v-if="success" class="form-alert success">{{ success }}</p>
        </form>

        <div class="panel-foot">
          <span>想起密码了？</span>
          <RouterLink class="link" to="/login">返回登录</RouterLink>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.ghost-btn,
.secondary-btn {
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.82);
  color: #0f172a;
  border-radius: 16px;
  font-size: 0.95rem;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.ghost-btn {
  min-width: 122px;
  padding: 0 16px;
  height: 52px;
}

.secondary-btn {
  min-height: 52px;
  padding: 0 18px;
}

.ghost-btn:hover:enabled,
.secondary-btn:hover:enabled {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
  border-color: rgba(15, 23, 42, 0.22);
}

.ghost-btn:disabled,
.secondary-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .code-row {
    grid-template-columns: 1fr;
  }

  .ghost-btn,
  .secondary-btn {
    width: 100%;
  }
}
</style>
