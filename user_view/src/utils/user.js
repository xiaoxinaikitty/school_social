export const getStatusMeta = (status) => {
  switch (status) {
    case 0:
      return { label: '待审核', type: 'warning' }
    case 1:
      return { label: '已发布', type: 'success' }
    case 2:
      return { label: '未通过', type: 'danger' }
    case 3:
      return { label: '草稿', type: 'info' }
    default:
      return { label: '未知', type: 'info' }
  }
}

export const getVisibilityLabel = (value) => {
  if (value === 1) return '仅关注可见'
  if (value === 2) return '仅自己可见'
  return '全校可见'
}

export const getPostTypeLabel = (value) => {
  switch (value) {
    case 1:
      return '活动信息'
    case 2:
      return '投票互动'
    case 3:
      return '二手交易'
    default:
      return '图文动态'
  }
}

export const formatSnippet = (text, limit = 120) => {
  if (!text) return '暂无内容'
  const clean = text.replace(/\s+/g, ' ').trim()
  if (clean.length <= limit) return clean
  return `${clean.slice(0, limit)}...`
}

export const getInitial = (text, fallback = '校') => {
  if (!text) return fallback
  return String(text).trim().slice(0, 1) || fallback
}

export const getGenderLabel = (value) => {
  if (value === 1 || value === '1') return '男'
  if (value === 2 || value === '2') return '女'
  if (value === 0 || value === '0') return '保密'
  return '未设置'
}
