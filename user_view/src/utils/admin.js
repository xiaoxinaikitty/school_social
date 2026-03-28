import { apiFetch } from './api'

export const requestData = async (router, path, options = {}) => {
  const res = await apiFetch(path, options)
  const data = await res.json().catch(() => ({}))

  if (res.status === 401) {
    router.push('/login')
    throw new Error('未登录或登录已过期')
  }

  if (!res.ok || data.code !== 0) {
    throw new Error(data.message || '请求失败')
  }

  return data.data
}

export const formatDateTime = (value) => {
  if (!value) return '--'
  return String(value).replace('T', ' ')
}

export const formatPercent = (value) => {
  if (value === null || value === undefined) return '0%'
  const pct = Number(value) * 100
  if (Number.isNaN(pct)) return '0%'
  return `${pct.toFixed(1)}%`
}

export const shortText = (value, max = 72) => {
  if (!value) return '暂无内容'
  return value.length > max ? `${value.slice(0, max)}...` : value
}
