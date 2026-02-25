<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { apiFetch } from '../utils/api'

const username = ref('')
const password = ref('')
const email = ref('')
const phone = ref('')
const school = ref('')
const college = ref('')
const grade = ref('')
const gender = ref('')
const bio = ref('')
const loading = ref(false)
const error = ref('')
const success = ref('')

const submit = async () => {
  error.value = ''
  success.value = ''
  const name = username.value.trim()
  const pass = password.value
  const emailVal = email.value.trim()
  const phoneVal = phone.value.trim()
  if (!name || !pass) {
    error.value = '用户名和密码不能为空。'
    return
  }
  if (name.length < 3 || name.length > 20) {
    error.value = '用户名长度需为 3-20 位。'
    return
  }
  if (pass.length < 6 || pass.length > 32) {
    error.value = '密码长度需为 6-32 位。'
    return
  }
  if (emailVal && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailVal)) {
    error.value = '邮箱格式不正确。'
    return
  }
  if (phoneVal && !/^[0-9]{8,20}$/.test(phoneVal)) {
    error.value = '手机号需为 8-20 位数字。'
    return
  }
  loading.value = true
  try {
    const payload = {
      username: name,
      password: pass,
      school: school.value.trim() || undefined,
      college: college.value.trim() || undefined,
      grade: grade.value.trim() || undefined,
      bio: bio.value.trim() || undefined,
      gender: gender.value ? Number(gender.value) : undefined,
    }
    if (emailVal) {
      payload.email = emailVal
    }
    if (phoneVal) {
      payload.phone = phoneVal
    }

    const res = await apiFetch('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      error.value = data.message || data.error || '注册失败，请检查输入格式。'
      return
    }
    success.value = '注册成功，可前往登录。'
  } catch (err) {
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
        <p class="eyebrow">学生注册</p>
        <h1 class="hero-title">创建你的校园账号。</h1>
        <p class="hero-copy">
          进入社区，分享内容、关注社团，并获得个性化推荐。
        </p>
        <div class="hero-cards">
          <div class="mini-card" style="animation-delay: 0.05s">
            <p class="mini-title">角色：学生</p>
            <p class="mini-note">管理员账号由系统统一创建。</p>
          </div>
          <div class="mini-card" style="animation-delay: 0.12s">
            <p class="mini-title">信息真实</p>
            <p class="mini-note">填写真实信息，互动更可靠。</p>
          </div>
          <div class="mini-card" style="animation-delay: 0.2s">
            <p class="mini-title">个性内容流</p>
            <p class="mini-note">兴趣标签决定推荐方向。</p>
          </div>
        </div>
      </section>

      <main class="auth-panel">
        <div class="panel-head">
          <h2>创建账号</h2>
          <p>填写基本信息，邮箱或手机号可选。</p>
        </div>

        <form class="auth-form" @submit.prevent="submit">
          <div class="grid-2">
            <label class="field">
              <span>用户名</span>
              <input v-model="username" type="text" placeholder="3-20 位用户名" minlength="3" maxlength="20" required />
            </label>
            <label class="field">
              <span>密码</span>
              <input v-model="password" type="password" placeholder="6-32 位密码" minlength="6" maxlength="32" required />
            </label>
          </div>
          <div class="grid-2">
            <label class="field">
              <span>邮箱（可选）</span>
              <input v-model="email" type="email" placeholder="name@school.edu" />
            </label>
            <label class="field">
              <span>手机号（可选）</span>
              <input v-model="phone" type="text" placeholder="8-20 位数字" pattern="^[0-9]{8,20}$" />
            </label>
          </div>
          <div class="grid-2">
            <label class="field">
              <span>学校</span>
              <input v-model="school" type="text" placeholder="学校名称" />
            </label>
            <label class="field">
              <span>学院</span>
              <input v-model="college" type="text" placeholder="学院名称" />
            </label>
          </div>
          <div class="grid-2">
            <label class="field">
              <span>年级</span>
              <input v-model="grade" type="text" placeholder="如 2024" />
            </label>
            <label class="field">
              <span>性别</span>
              <select v-model="gender">
                <option value="">未设置</option>
                <option value="0">保密</option>
                <option value="1">男</option>
                <option value="2">女</option>
              </select>
            </label>
          </div>
          <label class="field">
            <span>个人简介</span>
            <textarea v-model="bio" rows="3" placeholder="简单介绍一下自己"></textarea>
          </label>

          <button class="primary-btn" type="submit" :disabled="loading">
            {{ loading ? '正在创建...' : '创建账号' }}
          </button>

          <p v-if="error" class="form-alert error">{{ error }}</p>
          <p v-if="success" class="form-alert success">{{ success }}</p>
        </form>

        <div class="panel-foot">
          <span>已有账号？</span>
          <RouterLink class="link" to="/login">去登录</RouterLink>
        </div>
      </main>
    </div>
  </div>
</template>
