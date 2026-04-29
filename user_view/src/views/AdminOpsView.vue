<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight } from '@element-plus/icons-vue'
import AdminShell from '../components/admin/AdminShell.vue'
import { formatDateTime, formatPercent, requestData, shortText } from '../utils/admin'

const router = useRouter()
const route = useRoute()
const allowedTabs = ['content', 'users', 'tags', 'announcements', 'recommend']
const activeTab = ref('content')

const tags = ref([])
const tagLoading = ref(false)
const newTag = ref({ name: '', type: 0, status: 1 })
const editingTagId = ref(null)
const editTag = ref({ name: '', type: 0, status: 1 })

const announcements = ref([])
const announcementLoading = ref(false)
const announcementPage = ref(1)
const announcementSize = ref(6)
const announcementTotal = ref(0)
const newAnnouncement = ref({ title: '', content: '', status: 0 })
const editingAnnouncementId = ref(null)
const editAnnouncement = ref({ title: '', content: '', status: 0 })

const postFlagId = ref('')
const postPinned = ref(false)
const postFeatured = ref(false)
const postFlagLoading = ref(false)
const approvedPosts = ref([])
const approvedLoading = ref(false)
const approvedPage = ref(1)
const approvedSize = ref(6)
const approvedTotal = ref(0)
const postSearch = ref('')

const userStatusId = ref('')
const userStatus = ref(1)
const userStatusLoading = ref(false)
const users = ref([])
const userLoading = ref(false)
const userPage = ref(1)
const userSize = ref(6)
const userTotal = ref(0)
const userSearch = ref('')

const recommendConfig = ref({
  enableHot: true,
  enableFollow: true,
  enableTag: true,
  weightHot: 0.4,
  weightTime: 0.2,
  weightQuality: 0.2,
  weightTag: 0.1,
  weightFollow: 0.1,
})
const recommendLoading = ref(false)
const recommendOverviewLoading = ref(false)
const recommendWindow = ref(7)
const recommendOverview = ref({
  exposureCount: 0,
  clickCount: 0,
  clickRate: 0,
  scenes: [],
  topPosts: [],
})

const filteredApprovedPosts = computed(() => {
  const keyword = postSearch.value.trim().toLowerCase()
  if (!keyword) return approvedPosts.value
  return approvedPosts.value.filter((item) => {
    const title = (item.title || '').toLowerCase()
    const content = (item.content || '').toLowerCase()
    const idText = String(item.id || '').toLowerCase()
    return title.includes(keyword) || content.includes(keyword) || idText.includes(keyword)
  })
})

const loadTags = async () => {
  tagLoading.value = true
  try {
    tags.value = (await requestData(router, '/api/admin/tags')) || []
  } catch (err) {
    ElMessage.error(err?.message || '获取标签失败')
  } finally {
    tagLoading.value = false
  }
}

const addTag = async () => {
  if (!newTag.value.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  try {
    await requestData(router, '/api/admin/tags', {
      method: 'POST',
      body: JSON.stringify({
        name: newTag.value.name.trim(),
        type: Number(newTag.value.type),
        status: Number(newTag.value.status),
      }),
    })
    ElMessage.success('标签已新增')
    newTag.value = { name: '', type: 0, status: 1 }
    loadTags()
  } catch (err) {
    ElMessage.error(err?.message || '新增标签失败')
  }
}

const startEditTag = (tag) => {
  editingTagId.value = tag.id
  editTag.value = { name: tag.name, type: tag.type ?? 0, status: tag.status ?? 1 }
}

const cancelEditTag = () => {
  editingTagId.value = null
  editTag.value = { name: '', type: 0, status: 1 }
}

const saveTag = async (id) => {
  try {
    await requestData(router, `/api/admin/tags/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: editTag.value.name.trim(),
        type: Number(editTag.value.type),
        status: Number(editTag.value.status),
      }),
    })
    ElMessage.success('标签已更新')
    cancelEditTag()
    loadTags()
  } catch (err) {
    ElMessage.error(err?.message || '更新标签失败')
  }
}

const deleteTag = async (id) => {
  try {
    await ElMessageBox.confirm('删除后不可恢复，是否继续？', '删除标签', { type: 'warning' })
    await requestData(router, `/api/admin/tags/${id}`, { method: 'DELETE' })
    ElMessage.success('标签已删除')
    loadTags()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err?.message || '删除标签失败')
    }
  }
}

const loadAnnouncements = async (page = 1) => {
  announcementLoading.value = true
  try {
    const data = await requestData(router, `/api/admin/announcements?page=${page}&size=${announcementSize.value}`)
    announcements.value = data?.list || []
    announcementTotal.value = data?.total ?? 0
    announcementPage.value = data?.page ?? page
  } catch (err) {
    ElMessage.error(err?.message || '获取公告失败')
  } finally {
    announcementLoading.value = false
  }
}

const addAnnouncement = async () => {
  if (!newAnnouncement.value.title.trim() || !newAnnouncement.value.content.trim()) {
    ElMessage.warning('请输入公告标题与内容')
    return
  }
  try {
    await requestData(router, '/api/admin/announcements', {
      method: 'POST',
      body: JSON.stringify({
        title: newAnnouncement.value.title.trim(),
        content: newAnnouncement.value.content.trim(),
        status: Number(newAnnouncement.value.status),
      }),
    })
    ElMessage.success('公告已发布')
    newAnnouncement.value = { title: '', content: '', status: 0 }
    loadAnnouncements(1)
  } catch (err) {
    ElMessage.error(err?.message || '发布公告失败')
  }
}

const startEditAnnouncement = (item) => {
  editingAnnouncementId.value = item.id
  editAnnouncement.value = {
    title: item.title || '',
    content: item.content || '',
    status: item.status ?? 0,
  }
}

const cancelEditAnnouncement = () => {
  editingAnnouncementId.value = null
  editAnnouncement.value = { title: '', content: '', status: 0 }
}

const saveAnnouncement = async (id) => {
  try {
    await requestData(router, `/api/admin/announcements/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        title: editAnnouncement.value.title.trim(),
        content: editAnnouncement.value.content.trim(),
        status: Number(editAnnouncement.value.status),
      }),
    })
    ElMessage.success('公告已更新')
    cancelEditAnnouncement()
    loadAnnouncements(announcementPage.value)
  } catch (err) {
    ElMessage.error(err?.message || '更新公告失败')
  }
}

const deleteAnnouncement = async (id) => {
  try {
    await ElMessageBox.confirm('删除公告后将无法恢复，是否继续？', '删除公告', { type: 'warning' })
    await requestData(router, `/api/admin/announcements/${id}`, { method: 'DELETE' })
    ElMessage.success('公告已删除')
    loadAnnouncements(announcementPage.value)
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err?.message || '删除公告失败')
    }
  }
}

const loadApprovedPosts = async (page = 1) => {
  approvedLoading.value = true
  try {
    const data = await requestData(router, `/api/admin/posts?page=${page}&size=${approvedSize.value}&status=1`)
    approvedPosts.value = data?.list || []
    approvedTotal.value = data?.total ?? 0
    approvedPage.value = data?.page ?? page
  } catch (err) {
    ElMessage.error(err?.message || '获取内容失败')
  } finally {
    approvedLoading.value = false
  }
}

const selectPostForFlags = (post) => {
  postFlagId.value = post.id
  postPinned.value = Boolean(post.pinned)
  postFeatured.value = Boolean(post.featured)
  ElMessage.success(`已选择内容 #${post.id}`)
}

const updateFlags = async () => {
  if (!postFlagId.value) {
    ElMessage.warning('请先选择内容')
    return
  }
  postFlagLoading.value = true
  try {
    await requestData(router, `/api/admin/posts/${postFlagId.value}/flags`, {
      method: 'PUT',
      body: JSON.stringify({
        pinned: postPinned.value ? 1 : 0,
        featured: postFeatured.value ? 1 : 0,
      }),
    })
    ElMessage.success('内容标记已更新')
    loadApprovedPosts(approvedPage.value)
  } catch (err) {
    ElMessage.error(err?.message || '更新内容标记失败')
  } finally {
    postFlagLoading.value = false
  }
}

const loadUsers = async (page = 1) => {
  userLoading.value = true
  try {
    const params = new URLSearchParams()
    params.set('page', page)
    params.set('size', userSize.value)
    if (userSearch.value.trim()) {
      params.set('keyword', userSearch.value.trim())
    }
    const data = await requestData(router, `/api/admin/users?${params.toString()}`)
    users.value = data?.list || []
    userTotal.value = data?.total ?? 0
    userPage.value = data?.page ?? page
  } catch (err) {
    ElMessage.error(err?.message || '获取用户失败')
  } finally {
    userLoading.value = false
  }
}

const selectUserForStatus = (user) => {
  userStatusId.value = user.id
  userStatus.value = user.status ?? 0
  ElMessage.success(`已选择用户 #${user.id}`)
}

const updateUserStatus = async () => {
  if (!userStatusId.value) {
    ElMessage.warning('请先选择用户')
    return
  }
  userStatusLoading.value = true
  try {
    await requestData(router, `/api/admin/users/${userStatusId.value}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status: Number(userStatus.value) }),
    })
    ElMessage.success('用户状态已更新')
    loadUsers(userPage.value)
  } catch (err) {
    ElMessage.error(err?.message || '更新用户状态失败')
  } finally {
    userStatusLoading.value = false
  }
}

const loadRecommendConfig = async () => {
  recommendLoading.value = true
  try {
    recommendConfig.value = (await requestData(router, '/api/admin/recommend/config')) || recommendConfig.value
  } catch (err) {
    ElMessage.error(err?.message || '获取推荐配置失败')
  } finally {
    recommendLoading.value = false
  }
}

const loadRecommendOverview = async () => {
  recommendOverviewLoading.value = true
  try {
    recommendOverview.value = (await requestData(router, `/api/admin/recommend/overview?days=${recommendWindow.value}`)) || recommendOverview.value
  } catch (err) {
    ElMessage.error(err?.message || '获取推荐分析失败')
  } finally {
    recommendOverviewLoading.value = false
  }
}

const saveRecommendConfig = async () => {
  recommendLoading.value = true
  try {
    recommendConfig.value = (await requestData(router, '/api/admin/recommend/config', {
      method: 'PUT',
      body: JSON.stringify(recommendConfig.value),
    })) || recommendConfig.value
    ElMessage.success('推荐配置已保存')
    loadRecommendOverview()
  } catch (err) {
    ElMessage.error(err?.message || '保存推荐配置失败')
  } finally {
    recommendLoading.value = false
  }
}

const getSceneLabel = (scene) => {
  if (scene === 0) return '推荐流'
  if (scene === 1) return '关注流'
  if (scene === 2) return '站内位'
  return `场景 ${scene ?? '--'}`
}

const reloadCurrent = () => {
  loadApprovedPosts(approvedPage.value)
  loadUsers(userPage.value)
  loadTags()
  loadAnnouncements(announcementPage.value)
  loadRecommendConfig()
  loadRecommendOverview()
}

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = allowedTabs.includes(tab) ? tab : 'content'
  },
  { immediate: true },
)

watch(activeTab, (tab) => {
  if (route.query.tab !== tab) {
    router.replace({ path: '/admin/ops', query: { tab } })
  }
})

onMounted(() => {
  reloadCurrent()
})
</script>

<template>
  <AdminShell title="运营配置中心" subtitle="把内容曝光、用户治理、标签体系、公告与推荐策略统一收口到同一套后台工作台。">
    <template #actions>
      <div class="admin-toolbar-card">
        <el-button :icon="RefreshRight" plain @click="reloadCurrent">刷新全部模块</el-button>
      </div>
    </template>

    <el-tabs v-model="activeTab" type="border-card" class="admin-tabs">
      <el-tab-pane label="内容运营" name="content">
        <div class="admin-split">
          <el-card class="admin-surface-card" shadow="never">
            <div class="admin-section-heading">
              <div>
                <h3>已通过内容池</h3>
                <p>从已通过内容中挑选置顶或精选对象。</p>
              </div>
            </div>
            <div class="admin-toolbar-card" style="margin-bottom:18px;">
              <el-input v-model="postSearch" clearable placeholder="按标题、内容或 ID 搜索" />
              <el-button plain @click="loadApprovedPosts(1)">刷新内容</el-button>
            </div>
            <el-table class="admin-compact-table" :data="filteredApprovedPosts" v-loading="approvedLoading">
              <el-table-column prop="id" label="ID" width="84" />
              <el-table-column label="内容标题" min-width="220">
                <template #default="{ row }">
                  <div style="font-weight:600;color:#0f172a;">{{ row.title || '未命名内容' }}</div>
                  <div style="margin-top:4px;color:#64748b;">{{ shortText(row.content, 74) }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="userId" label="发布者" width="96" />
              <el-table-column label="发布时间" width="180">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="selectPostForFlags(row)">选择</el-button>
                  <el-button link @click="router.push(`/posts/${row.id}`)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="admin-table-footer" v-if="approvedTotal > approvedSize">
              <el-pagination
                background
                layout="prev, pager, next"
                :current-page="approvedPage"
                :page-size="approvedSize"
                :total="approvedTotal"
                @current-change="loadApprovedPosts"
              />
            </div>
          </el-card>

          <div class="admin-side-stack">
            <el-card class="admin-glass-card" shadow="never">
              <div class="admin-section-heading">
                <div>
                  <h3>曝光位配置</h3>
                  <p>设置内容置顶与精选状态。</p>
                </div>
              </div>
              <el-form label-position="top">
                <el-form-item label="内容 ID">
                  <el-input v-model="postFlagId" placeholder="输入或从左侧选择内容" />
                </el-form-item>
                <el-form-item label="置顶状态">
                  <el-switch v-model="postPinned" inline-prompt active-text="是" inactive-text="否" />
                </el-form-item>
                <el-form-item label="精选状态">
                  <el-switch v-model="postFeatured" inline-prompt active-text="是" inactive-text="否" />
                </el-form-item>
                <el-button type="primary" :loading="postFlagLoading" @click="updateFlags">保存曝光配置</el-button>
              </el-form>
            </el-card>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="用户治理" name="users">
        <div class="admin-split">
          <el-card class="admin-surface-card" shadow="never">
            <div class="admin-section-heading">
              <div>
                <h3>用户列表</h3>
                <p>支持按用户名、邮箱和手机号搜索用户。</p>
              </div>
            </div>
            <div class="admin-toolbar-card" style="margin-bottom:18px;">
              <el-input v-model="userSearch" clearable placeholder="用户名 / 邮箱 / 手机号" />
              <el-button plain @click="loadUsers(1)">搜索</el-button>
            </div>
            <el-table class="admin-compact-table" :data="users" v-loading="userLoading">
              <el-table-column prop="id" label="用户 ID" width="96" />
              <el-table-column label="账号信息" min-width="240">
                <template #default="{ row }">
                  <div style="font-weight:600;color:#0f172a;">{{ row.username || '未命名用户' }}</div>
                  <div style="margin-top:4px;color:#64748b;">{{ row.email || row.phone || '未绑定联系方式' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="所属信息" min-width="200">
                <template #default="{ row }">{{ row.school || '-' }} / {{ row.college || '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'danger' : 'success'" round>
                    {{ row.status === 1 ? '封禁' : '正常' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="selectUserForStatus(row)">选择</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="admin-table-footer" v-if="userTotal > userSize">
              <el-pagination
                background
                layout="prev, pager, next"
                :current-page="userPage"
                :page-size="userSize"
                :total="userTotal"
                @current-change="loadUsers"
              />
            </div>
          </el-card>

          <el-card class="admin-glass-card" shadow="never">
            <div class="admin-section-heading">
              <div>
                <h3>状态处置</h3>
                <p>基于用户 ID 直接执行封禁或解禁。</p>
              </div>
            </div>
            <el-form label-position="top">
              <el-form-item label="用户 ID">
                <el-input v-model="userStatusId" placeholder="输入或从左侧列表选择用户" />
              </el-form-item>
              <el-form-item label="目标状态">
                <el-select v-model="userStatus">
                  <el-option :value="1" label="封禁" />
                  <el-option :value="0" label="解禁" />
                </el-select>
              </el-form-item>
              <el-button type="primary" :loading="userStatusLoading" @click="updateUserStatus">提交状态更新</el-button>
            </el-form>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="标签体系" name="tags">
        <div class="admin-stack">
          <el-card class="admin-glass-card" shadow="never">
            <div class="admin-section-heading">
              <div>
                <h3>新增标签</h3>
                <p>维护前台话题和兴趣标签体系。</p>
              </div>
            </div>
            <div class="admin-form-grid-3">
              <el-input v-model="newTag.name" placeholder="标签名称" />
              <el-select v-model="newTag.type">
                <el-option :value="0" label="普通标签" />
                <el-option :value="1" label="系统标签" />
              </el-select>
              <el-select v-model="newTag.status">
                <el-option :value="1" label="启用" />
                <el-option :value="0" label="停用" />
              </el-select>
            </div>
            <div style="margin-top:16px;">
              <el-button type="primary" :icon="Plus" @click="addTag">新增标签</el-button>
            </div>
          </el-card>

          <el-card class="admin-surface-card" shadow="never">
            <el-table class="admin-compact-table" :data="tags" v-loading="tagLoading">
              <el-table-column prop="id" label="ID" width="84" />
              <el-table-column label="名称" min-width="180">
                <template #default="{ row }">
                  <el-input v-if="editingTagId === row.id" v-model="editTag.name" />
                  <span v-else>{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="120">
                <template #default="{ row }">
                  <el-select v-if="editingTagId === row.id" v-model="editTag.type">
                    <el-option :value="0" label="普通" />
                    <el-option :value="1" label="系统" />
                  </el-select>
                  <el-tag v-else :type="row.type === 1 ? 'info' : 'success'" round>
                    {{ row.type === 1 ? '系统' : '普通' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-select v-if="editingTagId === row.id" v-model="editTag.status">
                    <el-option :value="1" label="启用" />
                    <el-option :value="0" label="停用" />
                  </el-select>
                  <el-tag v-else :type="row.status === 1 ? 'success' : 'warning'" round>
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="{ row }">
                  <template v-if="editingTagId === row.id">
                    <el-button type="primary" link @click="saveTag(row.id)">保存</el-button>
                    <el-button link @click="cancelEditTag">取消</el-button>
                  </template>
                  <template v-else>
                    <el-button type="primary" link @click="startEditTag(row)">编辑</el-button>
                    <el-button type="danger" link @click="deleteTag(row.id)">删除</el-button>
                  </template>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="公告中心" name="announcements">
        <div class="admin-stack">
          <el-card class="admin-glass-card" shadow="never">
            <div class="admin-section-heading">
              <div>
                <h3>发布公告</h3>
                <p>管理前台公告内容和上线状态。</p>
              </div>
            </div>
            <div class="admin-form-grid">
              <el-input v-model="newAnnouncement.title" placeholder="公告标题" />
              <el-select v-model="newAnnouncement.status">
                <el-option :value="1" label="立即上线" />
                <el-option :value="0" label="草稿" />
              </el-select>
            </div>
            <div style="margin-top:16px;">
              <el-input
                v-model="newAnnouncement.content"
                type="textarea"
                :rows="4"
                placeholder="输入公告正文"
              />
            </div>
            <div style="margin-top:16px;">
              <el-button type="primary" :icon="Plus" @click="addAnnouncement">发布公告</el-button>
            </div>
          </el-card>

          <el-card class="admin-surface-card" shadow="never">
            <el-table class="admin-compact-table" :data="announcements" v-loading="announcementLoading">
              <el-table-column prop="id" label="ID" width="84" />
              <el-table-column label="标题" min-width="200">
                <template #default="{ row }">
                  <el-input v-if="editingAnnouncementId === row.id" v-model="editAnnouncement.title" />
                  <span v-else>{{ row.title }}</span>
                </template>
              </el-table-column>
              <el-table-column label="内容" min-width="260">
                <template #default="{ row }">
                  <el-input
                    v-if="editingAnnouncementId === row.id"
                    v-model="editAnnouncement.content"
                    type="textarea"
                    :rows="2"
                  />
                  <span v-else>{{ shortText(row.content, 86) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-select v-if="editingAnnouncementId === row.id" v-model="editAnnouncement.status">
                    <el-option :value="1" label="上线" />
                    <el-option :value="0" label="草稿" />
                  </el-select>
                  <el-tag v-else :type="row.status === 1 ? 'success' : 'info'" round>
                    {{ row.status === 1 ? '上线' : '草稿' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="创建时间" width="180">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="{ row }">
                  <template v-if="editingAnnouncementId === row.id">
                    <el-button type="primary" link @click="saveAnnouncement(row.id)">保存</el-button>
                    <el-button link @click="cancelEditAnnouncement">取消</el-button>
                  </template>
                  <template v-else>
                    <el-button type="primary" link @click="startEditAnnouncement(row)">编辑</el-button>
                    <el-button type="danger" link @click="deleteAnnouncement(row.id)">删除</el-button>
                  </template>
                </template>
              </el-table-column>
            </el-table>
            <div class="admin-table-footer" v-if="announcementTotal > announcementSize">
              <el-pagination
                background
                layout="prev, pager, next"
                :current-page="announcementPage"
                :page-size="announcementSize"
                :total="announcementTotal"
                @current-change="loadAnnouncements"
              />
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="推荐策略" name="recommend">
        <div class="admin-stack">
          <el-card class="admin-surface-card" shadow="never">
            <div class="admin-section-heading">
              <div>
                <h3>推荐效果看板</h3>
                <p>先看曝光、点击和 CTR，再决定是否调整推荐策略。</p>
              </div>
              <div class="admin-toolbar-card">
                <el-select v-model="recommendWindow" style="width:140px" @change="loadRecommendOverview">
                  <el-option :value="7" label="最近 7 天" />
                  <el-option :value="15" label="最近 15 天" />
                  <el-option :value="30" label="最近 30 天" />
                </el-select>
                <el-button plain :loading="recommendOverviewLoading" @click="loadRecommendOverview">刷新效果</el-button>
              </div>
            </div>

            <div class="admin-form-grid" v-loading="recommendOverviewLoading">
              <el-card shadow="never">
                <div style="font-size:0.95rem;color:#64748b;">推荐曝光</div>
                <div style="margin-top:10px;font-size:1.8rem;font-weight:700;color:#0f172a;">
                  {{ recommendOverview.exposureCount || 0 }}
                </div>
              </el-card>
              <el-card shadow="never">
                <div style="font-size:0.95rem;color:#64748b;">推荐点击</div>
                <div style="margin-top:10px;font-size:1.8rem;font-weight:700;color:#0f172a;">
                  {{ recommendOverview.clickCount || 0 }}
                </div>
              </el-card>
              <el-card shadow="never">
                <div style="font-size:0.95rem;color:#64748b;">综合 CTR</div>
                <div style="margin-top:10px;font-size:1.8rem;font-weight:700;color:#0f172a;">
                  {{ formatPercent(recommendOverview.clickRate) }}
                </div>
              </el-card>
            </div>

            <div class="admin-split" style="margin-top:18px;">
              <el-card shadow="never">
                <div class="admin-section-heading">
                  <div>
                    <h3>场景表现</h3>
                    <p>区分不同推荐场景的点击反馈。</p>
                  </div>
                </div>
                <el-table class="admin-compact-table" :data="recommendOverview.scenes || []" empty-text="暂无推荐场景数据">
                  <el-table-column label="场景" min-width="120">
                    <template #default="{ row }">
                      <el-tag round effect="light">{{ getSceneLabel(row.scene) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="exposureCount" label="曝光" width="100" />
                  <el-table-column prop="clickCount" label="点击" width="100" />
                  <el-table-column label="CTR" width="110">
                    <template #default="{ row }">{{ formatPercent(row.clickRate) }}</template>
                  </el-table-column>
                </el-table>
              </el-card>

              <el-card shadow="never">
                <div class="admin-section-heading">
                  <div>
                    <h3>推荐内容 Top</h3>
                    <p>观察高曝光内容是否真正带来点击。</p>
                  </div>
                </div>
                <el-table class="admin-compact-table" :data="recommendOverview.topPosts || []" empty-text="暂无推荐内容数据">
                  <el-table-column prop="postId" label="内容 ID" width="96" />
                  <el-table-column label="标题" min-width="220">
                    <template #default="{ row }">{{ row.title || '未命名内容' }}</template>
                  </el-table-column>
                  <el-table-column prop="exposureCount" label="曝光" width="100" />
                  <el-table-column prop="clickCount" label="点击" width="100" />
                  <el-table-column label="CTR" width="110">
                    <template #default="{ row }">{{ formatPercent(row.clickRate) }}</template>
                  </el-table-column>
                </el-table>
              </el-card>
            </div>
          </el-card>

          <el-card class="admin-glass-card" shadow="never">
            <div class="admin-section-heading">
              <div>
                <h3>推荐配置面板</h3>
                <p>通过开关和权重调节内容分发逻辑。</p>
              </div>
            </div>
            <div class="admin-weight-card">
              <div class="admin-form-grid">
                <el-card shadow="never">
                  <div style="display:flex;justify-content:space-between;align-items:center;">
                    <div>
                      <div style="font-weight:600;color:#0f172a;">热门流开关</div>
                      <div style="margin-top:4px;color:#64748b;font-size:0.9rem;">控制热门内容参与推荐</div>
                    </div>
                    <el-switch v-model="recommendConfig.enableHot" />
                  </div>
                </el-card>
                <el-card shadow="never">
                  <div style="display:flex;justify-content:space-between;align-items:center;">
                    <div>
                      <div style="font-weight:600;color:#0f172a;">关注流开关</div>
                      <div style="margin-top:4px;color:#64748b;font-size:0.9rem;">控制关注关系参与推荐</div>
                    </div>
                    <el-switch v-model="recommendConfig.enableFollow" />
                  </div>
                </el-card>
              </div>

              <el-card shadow="never">
                <div style="display:flex;justify-content:space-between;align-items:center;">
                  <div>
                    <div style="font-weight:600;color:#0f172a;">兴趣标签开关</div>
                    <div style="margin-top:4px;color:#64748b;font-size:0.9rem;">控制兴趣标签权重是否生效</div>
                  </div>
                  <el-switch v-model="recommendConfig.enableTag" />
                </div>
              </el-card>

              <div class="admin-form-grid">
                <el-card shadow="never">
                  <div style="margin-bottom:10px;font-weight:600;color:#0f172a;">热度权重</div>
                  <el-slider v-model="recommendConfig.weightHot" :step="0.1" :min="0" :max="1" show-input />
                </el-card>
                <el-card shadow="never">
                  <div style="margin-bottom:10px;font-weight:600;color:#0f172a;">时效权重</div>
                  <el-slider v-model="recommendConfig.weightTime" :step="0.1" :min="0" :max="1" show-input />
                </el-card>
                <el-card shadow="never">
                  <div style="margin-bottom:10px;font-weight:600;color:#0f172a;">质量权重</div>
                  <el-slider v-model="recommendConfig.weightQuality" :step="0.1" :min="0" :max="1" show-input />
                </el-card>
                <el-card shadow="never">
                  <div style="margin-bottom:10px;font-weight:600;color:#0f172a;">兴趣权重</div>
                  <el-slider v-model="recommendConfig.weightTag" :step="0.1" :min="0" :max="1" show-input />
                </el-card>
                <el-card shadow="never">
                  <div style="margin-bottom:10px;font-weight:600;color:#0f172a;">关注权重</div>
                  <el-slider v-model="recommendConfig.weightFollow" :step="0.1" :min="0" :max="1" show-input />
                </el-card>
              </div>

              <div>
                <el-button type="primary" :loading="recommendLoading" @click="saveRecommendConfig">保存推荐配置</el-button>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </AdminShell>
</template>
