export const API_BASE = import.meta.env.VITE_API_BASE || ''

export const apiFetch = (path, options = {}) => {
  const token = localStorage.getItem('auth_token')
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  return fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })
}

export const apiUpload = (path, formData, options = {}) => {
  const token = localStorage.getItem('auth_token')
  const headers = {
    ...(options.headers || {}),
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  return fetch(`${API_BASE}${path}`, {
    method: 'POST',
    body: formData,
    ...options,
    headers,
  })
}

export const buildWsUrl = (path, params = {}) => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  let origin = `${protocol}//${window.location.host}`
  if (/^https?:\/\//.test(API_BASE)) {
    const baseUrl = new URL(API_BASE)
    origin = `${baseUrl.protocol === 'https:' ? 'wss:' : 'ws:'}//${baseUrl.host}`
  }
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      query.set(key, String(value))
    }
  })
  return `${origin}${path}${query.toString() ? `?${query.toString()}` : ''}`
}

export const buildAssetUrl = (path) => {
  if (!path) return ''
  if (/^(https?:)?\/\//.test(path) || path.startsWith('data:') || path.startsWith('blob:')) {
    return path
  }
  if (!API_BASE) {
    return path
  }
  return `${API_BASE}${path.startsWith('/') ? path : `/${path}`}`
}
