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
