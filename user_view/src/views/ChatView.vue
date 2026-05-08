<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch, buildWsUrl } from '../utils/api'

const router = useRouter()

const currentUser = ref(null)
const rooms = ref([])
const roomsLoading = ref(false)
const roomError = ref('')
const currentRoomId = ref(null)
const activeMode = ref('public')

const messages = ref([])
const messagesLoading = ref(false)
const messageError = ref('')
const messagePage = ref(1)
const messageSize = 30
const messageTotal = ref(0)

const createRoomName = ref('')
const createRoomDescription = ref('')
const creatingRoom = ref(false)

const friendKeyword = ref('')
const friendCandidates = ref([])
const friendSearchLoading = ref(false)
const friendSearchError = ref('')
const friendPage = ref(1)
const friendSize = ref(6)
const friendTotal = ref(0)
const sendingFriendRequestId = ref(null)
const friendSearchSubmitted = ref(false)

const messageDraft = ref('')
const actionError = ref('')
const actionSuccess = ref('')
const wsConnected = ref(false)
const messageListRef = ref(null)

let socket = null

const publicRooms = computed(() => rooms.value.filter((item) => Number(item.roomType) !== 1))
const directRooms = computed(() => rooms.value.filter((item) => Number(item.roomType) === 1))
const displayedRooms = computed(() => (activeMode.value === 'friend' ? directRooms.value : publicRooms.value))
const selectedRoom = computed(() =>
  rooms.value.find((item) => Number(item.id) === Number(currentRoomId.value)) || null,
)
const selectedRoomIsDirect = computed(() => Number(selectedRoom.value?.roomType) === 1)

const messagePages = computed(() => {
  const pages = Math.ceil(messageTotal.value / messageSize)
  return pages > 0 ? pages : 1
})

const friendPages = computed(() => {
  const pages = Math.ceil(friendTotal.value / friendSize.value)
  return pages > 0 ? pages : 1
})

const roomSectionTitle = computed(() => (activeMode.value === 'friend' ? '我的好友' : '社区群聊'))
const roomSectionHint = computed(() => (
  activeMode.value === 'friend'
    ? `${directRooms.value.length} 位好友可直接私聊`
    : `${publicRooms.value.length} 个群聊可用`
))
const showFriendSearchResults = computed(() => friendSearchSubmitted.value || friendSearchLoading.value || !!friendSearchError.value)

const formatTime = (value) => {
  if (!value) return '暂无'
  return String(value).replace('T', ' ')
}

const roomInitial = (room) => {
  if (!room?.name) return activeMode.value === 'friend' ? '友' : '群'
  return room.name.slice(0, 1)
}

const messageInitial = (message) => {
  if (message?.senderName) return message.senderName.slice(0, 1)
  return '聊'
}

const isMine = (message) => Number(message?.senderId) === Number(currentUser.value?.id)

const candidateActionLabel = (item) => {
  if (item.relationStatus === 'friend') return item.directRoomId ? '发消息' : '已是好友'
  if (item.relationStatus === 'request_sent') return '已申请'
  if (item.relationStatus === 'request_received') return '去通知处理'
  return '添加好友'
}

const closeSocket = () => {
  wsConnected.value = false
  if (socket) {
    socket.close()
    socket = null
  }
}

const resetMessages = () => {
  messages.value = []
  messagePage.value = 1
  messageTotal.value = 0
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const markCurrentRoomRead = async () => {
  if (!selectedRoom.value?.id || selectedRoom.value.joined !== 1) return
  try {
    await apiFetch(`/api/chat/rooms/${selectedRoom.value.id}/read`, { method: 'PUT' })
  } catch {
    // ignore
  }
}

const loadMessages = async (roomId, page = 1) => {
  if (!roomId) return
  messagesLoading.value = true
  messageError.value = ''
  try {
    const res = await apiFetch(`/api/chat/rooms/${roomId}/messages?page=${page}&size=${messageSize}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      messageError.value = data.message || '获取聊天记录失败。'
      return
    }
    messages.value = data.data?.list || []
    messagePage.value = data.data?.page ?? page
    messageTotal.value = data.data?.total ?? 0
    await scrollToBottom()
    markCurrentRoomRead()
  } catch {
    messageError.value = '网络错误，无法获取聊天记录。'
  } finally {
    messagesLoading.value = false
  }
}

const connectSocket = (roomId) => {
  closeSocket()
  const token = localStorage.getItem('auth_token')
  if (!token || !roomId) return
  socket = new WebSocket(buildWsUrl('/ws/chat', { token, roomId }))
  socket.onopen = () => {
    if (Number(currentRoomId.value) === Number(roomId)) {
      wsConnected.value = true
    }
  }
  socket.onmessage = async (event) => {
    try {
      const payload = JSON.parse(event.data)
      if (payload.code !== 0) {
        actionError.value = payload.message || '聊天消息发送失败。'
        return
      }
      const message = payload.data
      if (!message || Number(message.roomId) !== Number(currentRoomId.value)) {
        return
      }
      if (!messages.value.some((item) => Number(item.id) === Number(message.id))) {
        messages.value = [...messages.value, message]
        messageTotal.value += 1
        await scrollToBottom()
      }
      markCurrentRoomRead()
      await loadRooms(currentRoomId.value, false)
    } catch {
      actionError.value = '收到无法识别的聊天消息。'
    }
  }
  socket.onclose = () => {
    if (Number(currentRoomId.value) === Number(roomId)) {
      wsConnected.value = false
    }
  }
  socket.onerror = () => {
    actionError.value = '聊天实时连接失败，请稍后重试。'
  }
}

const activateRoom = async (roomId) => {
  currentRoomId.value = roomId
  actionError.value = ''
  actionSuccess.value = ''
  messageError.value = ''
  const room = rooms.value.find((item) => Number(item.id) === Number(roomId))
  closeSocket()
  resetMessages()
  if (!room) return
  activeMode.value = Number(room.roomType) === 1 ? 'friend' : 'public'
  if (room.joined !== 1) return
  await loadMessages(roomId, 1)
  connectSocket(roomId)
}

const loadRooms = async (preferredRoomId = null, activate = true) => {
  roomsLoading.value = true
  roomError.value = ''
  try {
    const res = await apiFetch('/api/chat/rooms')
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      roomError.value = data.message || '获取聊天列表失败。'
      return
    }
    rooms.value = data.data || []
    if (!activate) {
      return
    }

    const preferredRoom = preferredRoomId
      ? rooms.value.find((item) => Number(item.id) === Number(preferredRoomId))
      : null
    if (preferredRoom) {
      await activateRoom(preferredRoom.id)
      return
    }

    const currentRoom = currentRoomId.value
      ? rooms.value.find((item) => Number(item.id) === Number(currentRoomId.value))
      : null
    if (currentRoom) {
      await activateRoom(currentRoom.id)
      return
    }

    if (activeMode.value === 'friend' && directRooms.value.length) {
      await activateRoom(directRooms.value[0].id)
      return
    }
    if (activeMode.value === 'public' && publicRooms.value.length) {
      await activateRoom(publicRooms.value[0].id)
      return
    }
    if (publicRooms.value.length) {
      activeMode.value = 'public'
      await activateRoom(publicRooms.value[0].id)
      return
    }
    if (directRooms.value.length) {
      activeMode.value = 'friend'
      await activateRoom(directRooms.value[0].id)
      return
    }

    currentRoomId.value = null
    resetMessages()
  } catch {
    roomError.value = '网络错误，无法获取聊天列表。'
  } finally {
    roomsLoading.value = false
  }
}

const switchMode = async (mode) => {
  if (mode === activeMode.value) return
  activeMode.value = mode
  const current = selectedRoom.value
  const currentMatches = current && ((mode === 'friend' && Number(current.roomType) === 1) || (mode === 'public' && Number(current.roomType) !== 1))
  if (currentMatches) return
  const nextRoom = mode === 'friend' ? directRooms.value[0] : publicRooms.value[0]
  if (nextRoom) {
    await activateRoom(nextRoom.id)
    return
  }
  closeSocket()
  currentRoomId.value = null
  resetMessages()
}

const createRoom = async () => {
  actionError.value = ''
  actionSuccess.value = ''
  const name = createRoomName.value.trim()
  if (!name) {
    actionError.value = '请输入群聊名称。'
    return
  }
  creatingRoom.value = true
  try {
    const res = await apiFetch('/api/chat/rooms', {
      method: 'POST',
      body: JSON.stringify({
        name,
        description: createRoomDescription.value.trim() || null,
      }),
    })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      actionError.value = data.message || '创建群聊失败。'
      return
    }
    createRoomName.value = ''
    createRoomDescription.value = ''
    activeMode.value = 'public'
    actionSuccess.value = '群聊已创建。'
    await loadRooms(data.data?.id)
  } catch {
    actionError.value = '网络错误，无法创建群聊。'
  } finally {
    creatingRoom.value = false
  }
}

const joinRoom = async (roomId) => {
  if (!roomId) return
  actionError.value = ''
  actionSuccess.value = ''
  try {
    const res = await apiFetch(`/api/chat/rooms/${roomId}/join`, { method: 'POST' })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      actionError.value = data.message || '加入群聊失败。'
      return
    }
    actionSuccess.value = '已加入群聊。'
    await loadRooms(roomId)
  } catch {
    actionError.value = '网络错误，无法加入群聊。'
  }
}

const quitRoom = async (roomId) => {
  if (!roomId) return
  actionError.value = ''
  actionSuccess.value = ''
  try {
    const res = await apiFetch(`/api/chat/rooms/${roomId}/quit`, { method: 'POST' })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      actionError.value = data.message || '退出群聊失败。'
      return
    }
    actionSuccess.value = '已退出群聊。'
    await loadRooms(null)
  } catch {
    actionError.value = '网络错误，无法退出群聊。'
  }
}

const loadFriendCandidates = async (page = 1) => {
  const keyword = friendKeyword.value.trim()
  if (!keyword) {
    friendCandidates.value = []
    friendSearchError.value = ''
    friendTotal.value = 0
    friendPage.value = 1
    friendSearchSubmitted.value = false
    return
  }
  friendSearchLoading.value = true
  friendSearchError.value = ''
  friendSearchSubmitted.value = true
  try {
    const params = new URLSearchParams({
      page: String(page),
      size: String(friendSize.value),
    })
    params.set('keyword', keyword)
    const res = await apiFetch(`/api/friends/search?${params.toString()}`)
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      friendSearchError.value = data.message || '获取用户列表失败。'
      return
    }
    friendCandidates.value = data.data?.list || []
    friendPage.value = data.data?.page ?? page
    friendTotal.value = data.data?.total ?? 0
  } catch {
    friendSearchError.value = '网络错误，无法获取用户列表。'
  } finally {
    friendSearchLoading.value = false
  }
}

const sendFriendRequest = async (candidate) => {
  if (!candidate?.id) return
  actionError.value = ''
  actionSuccess.value = ''
  sendingFriendRequestId.value = candidate.id
  try {
    const res = await apiFetch('/api/friends/requests', {
      method: 'POST',
      body: JSON.stringify({
        targetUserId: candidate.id,
      }),
    })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const data = await res.json()
    if (!res.ok || data.code !== 0) {
      actionError.value = data.message || '好友申请发送失败。'
      return
    }
    friendCandidates.value = friendCandidates.value.map((item) => (
      Number(item.id) === Number(candidate.id)
        ? { ...item, relationStatus: 'request_sent' }
        : item
    ))
    actionSuccess.value = '好友申请已发送，等待对方在通知中处理。'
  } catch {
    actionError.value = '网络错误，无法发送好友申请。'
  } finally {
    sendingFriendRequestId.value = null
  }
}

const openDirectChat = async (roomId) => {
  if (!roomId) return
  activeMode.value = 'friend'
  await activateRoom(roomId)
}

const handleCandidateAction = async (candidate) => {
  if (!candidate) return
  if (candidate.relationStatus === 'friend' && candidate.directRoomId) {
    await openDirectChat(candidate.directRoomId)
    return
  }
  if (candidate.relationStatus === 'request_received') {
    router.push('/social')
    return
  }
  if (candidate.relationStatus !== 'none') {
    return
  }
  await sendFriendRequest(candidate)
}

const sendMessage = () => {
  actionError.value = ''
  actionSuccess.value = ''
  const content = messageDraft.value.trim()
  if (!content) {
    actionError.value = '请输入要发送的消息。'
    return
  }
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    actionError.value = '实时连接未建立，请稍后重试。'
    return
  }
  socket.send(JSON.stringify({ content }))
  messageDraft.value = ''
}

onMounted(async () => {
  try {
    const savedUser = localStorage.getItem('auth_user')
    currentUser.value = savedUser ? JSON.parse(savedUser) : null
  } catch {
    currentUser.value = null
  }
  await loadRooms()
})

onBeforeUnmount(() => {
  closeSocket()
})
</script>

<template>
  <div class="profile-page">
    <header class="profile-hero">
      <div>
        <span class="hero-tag">群聊与好友私聊</span>
        <h1>校园实时交流空间</h1>
        <p>支持建群、加群、搜索用户发送好友申请，并在好友通过后开启 1 对 1 私聊。</p>
      </div>
      <div class="profile-actions">
        <button class="ghost-btn" type="button" @click="router.push('/home')">返回主页</button>
        <button class="ghost-btn" type="button" @click="loadRooms(currentRoomId)">刷新聊天列表</button>
      </div>
    </header>

    <section class="profile-section chat-layout">
      <aside class="chat-sidebar">
        <div class="chat-mode-switch">
          <button class="ghost-btn" :class="{ active: activeMode === 'public' }" type="button" @click="switchMode('public')">社区群聊</button>
          <button class="ghost-btn" :class="{ active: activeMode === 'friend' }" type="button" @click="switchMode('friend')">好友聊天</button>
        </div>

        <div class="profile-header">
          <div>
            <h2>{{ roomSectionTitle }}</h2>
            <p>{{ roomSectionHint }}</p>
          </div>
        </div>

        <template v-if="activeMode === 'public'">
          <form class="chat-room-form" @submit.prevent="createRoom">
            <label class="field">
              <span>新建群聊</span>
              <input v-model="createRoomName" type="text" maxlength="50" placeholder="输入群聊名称" />
            </label>
            <label class="field">
              <span>群聊简介</span>
              <input v-model="createRoomDescription" type="text" maxlength="255" placeholder="一句话介绍群聊主题" />
            </label>
            <button class="primary-btn" type="submit" :disabled="creatingRoom">
              {{ creatingRoom ? '创建中...' : '创建群聊' }}
            </button>
          </form>
        </template>

        <template v-else>
          <form class="chat-room-form" @submit.prevent="loadFriendCandidates(1)">
            <label class="field">
              <span>搜索用户</span>
              <input v-model="friendKeyword" type="text" maxlength="50" placeholder="按用户名、学校或学院搜索用户" />
            </label>
            <div class="chat-search-actions">
              <button class="primary-btn" type="submit" :disabled="friendSearchLoading">
                {{ friendSearchLoading ? '搜索中...' : '搜索并添加好友' }}
              </button>
              <button
                class="ghost-btn"
                type="button"
                :disabled="friendSearchLoading || !friendKeyword.trim()"
                @click="friendKeyword = ''; loadFriendCandidates(1)"
              >
                清空搜索
              </button>
            </div>
          </form>

          <div v-if="showFriendSearchResults" class="chat-search-panel">
            <div class="profile-header" style="margin-bottom: 0">
              <div>
                <h2>搜索结果</h2>
                <p>仅展示与你输入关键词匹配的用户。</p>
              </div>
            </div>
            <div v-if="friendSearchError" class="form-alert error">{{ friendSearchError }}</div>
            <div v-else-if="friendSearchLoading" class="feed-empty">用户搜索中...</div>
            <div v-else-if="friendCandidates.length" class="chat-search-results">
              <article v-for="item in friendCandidates" :key="item.id" class="chat-user-card">
                <div class="chat-user-card__meta">
                  <div class="chat-room-avatar">
                    <img v-if="item.avatarUrl" :src="item.avatarUrl" alt="avatar" />
                    <span v-else>{{ roomInitial(item) }}</span>
                  </div>
                  <div>
                    <strong>{{ item.username || `用户 ${item.id}` }}</strong>
                    <p class="chat-room-desc">{{ item.school || '未填写学校' }} / {{ item.college || '未填写学院' }}</p>
                    <p class="chat-user-card__hint">{{ item.bio || '这个人很低调，还没有留下签名。' }}</p>
                  </div>
                </div>
                <button
                  class="ghost-btn"
                  :class="{ danger: item.relationStatus === 'request_received' }"
                  type="button"
                  :disabled="item.relationStatus === 'request_sent' || (item.relationStatus === 'friend' && !item.directRoomId) || sendingFriendRequestId === item.id"
                  @click="handleCandidateAction(item)"
                >
                  {{ sendingFriendRequestId === item.id ? '发送中...' : candidateActionLabel(item) }}
                </button>
              </article>
              <div v-if="friendTotal > friendSize" class="pager" style="margin-top: 10px">
                <button class="ghost-btn" type="button" :disabled="friendPage <= 1 || friendSearchLoading" @click="loadFriendCandidates(friendPage - 1)">上一页</button>
                <span>{{ friendPage }} / {{ friendPages }}</span>
                <button class="ghost-btn" type="button" :disabled="friendPage >= friendPages || friendSearchLoading" @click="loadFriendCandidates(friendPage + 1)">下一页</button>
              </div>
            </div>
            <div v-else class="feed-empty">没有找到匹配用户，请换一个关键词试试。</div>
          </div>
          <div v-else class="feed-empty">需要添加好友时再搜索即可，这里默认只展示你的好友列表。</div>
        </template>

        <div v-if="roomError" class="form-alert error">{{ roomError }}</div>
        <div v-if="roomsLoading" class="feed-empty">聊天列表加载中...</div>
        <div v-else-if="displayedRooms.length === 0" class="feed-empty">
          {{ activeMode === 'friend' ? '暂无好友私聊入口，先搜索用户并发送好友申请。' : '暂无群聊，先创建一个吧。' }}
        </div>
        <div v-else class="chat-room-list">
          <article
            v-for="room in displayedRooms"
            :key="room.id"
            class="chat-room-card"
            :class="{ active: Number(room.id) === Number(currentRoomId), 'is-direct': Number(room.roomType) === 1 }"
            role="button"
            tabindex="0"
            @click="activateRoom(room.id)"
            @keydown.enter="activateRoom(room.id)"
          >
            <div class="chat-room-top">
              <div class="chat-room-avatar">
                <img v-if="room.avatarUrl" :src="room.avatarUrl" alt="room" />
                <span v-else>{{ roomInitial(room) }}</span>
              </div>
              <div class="chat-room-main">
                <div class="chat-room-meta">
                  <strong>{{ room.name }}</strong>
                  <span class="chat-pill" :class="{ active: room.joined === 1 || Number(room.roomType) === 1 }">
                    {{ Number(room.roomType) === 1 ? '好友私聊' : room.joined === 1 ? '已加入' : '未加入' }}
                  </span>
                </div>
                <p class="chat-room-desc">{{ room.lastMessage || room.description || '暂无消息，欢迎发言。' }}</p>
                <div class="chat-room-foot">
                  <span>{{ Number(room.roomType) === 1 ? '双人聊天' : `${room.memberCount || 0} 人` }}</span>
                  <span>{{ formatTime(room.lastMessageAt || room.createdAt) }}</span>
                  <span v-if="room.unreadCount" class="chat-unread">{{ room.unreadCount }}</span>
                </div>
              </div>
            </div>
            <div class="chat-room-actions">
              <button
                v-if="Number(room.roomType) !== 1 && room.joined !== 1"
                class="ghost-btn"
                type="button"
                @click.stop="joinRoom(room.id)"
              >
                加入
              </button>
              <button
                v-else-if="Number(room.roomType) !== 1 && Number(room.ownerId) !== Number(currentUser?.id)"
                class="ghost-btn danger"
                type="button"
                @click.stop="quitRoom(room.id)"
              >
                退出
              </button>
            </div>
          </article>
        </div>
      </aside>

      <div class="chat-shell">
        <div v-if="!selectedRoom" class="chat-empty">
          <h3>{{ activeMode === 'friend' ? '请选择一个好友私聊' : '请选择一个群聊' }}</h3>
          <p>{{ activeMode === 'friend' ? '左侧可搜索用户发送好友申请，或直接进入已通过的好友私聊。' : '左侧可加入已有群聊，或创建新的校园群。' }}</p>
        </div>

        <template v-else>
          <div class="profile-header">
            <div>
              <h2>{{ selectedRoom.name }}</h2>
              <p>{{ selectedRoomIsDirect ? '你们已经成为好友，可以在这里进行一对一私聊。' : (selectedRoom.description || '公开社区群聊，欢迎实时交流。') }}</p>
            </div>
            <div class="discover-actions">
              <span class="chat-status" :class="{ online: wsConnected }">
                {{ wsConnected ? '实时连接中' : selectedRoom.joined === 1 ? '连接未建立' : '尚未加入聊天' }}
              </span>
            </div>
          </div>

          <div v-if="selectedRoom.joined !== 1" class="chat-empty">
            <h3>你还没有加入这个群聊</h3>
            <p>加入后即可查看历史消息并参与实时聊天。</p>
            <button class="primary-btn" type="button" @click="joinRoom(selectedRoom.id)">加入群聊</button>
          </div>

          <template v-else>
            <div class="pager" v-if="messageTotal > messageSize">
              <button class="ghost-btn" type="button" :disabled="messagePage <= 1" @click="loadMessages(selectedRoom.id, messagePage - 1)">
                更早消息
              </button>
              <span>{{ messagePage }} / {{ messagePages }}</span>
              <button
                class="ghost-btn"
                type="button"
                :disabled="messagePage >= messagePages"
                @click="loadMessages(selectedRoom.id, messagePage + 1)"
              >
                更新页
              </button>
            </div>

            <div v-if="messagesLoading" class="feed-empty">聊天记录加载中...</div>
            <div v-else-if="messageError" class="form-alert error">{{ messageError }}</div>
            <div v-else ref="messageListRef" class="chat-stream">
              <div v-if="messages.length === 0" class="feed-empty">还没有消息，来发送第一条吧。</div>
              <div
                v-for="message in messages"
                :key="message.id"
                class="chat-bubble-row"
                :class="{ mine: isMine(message) }"
              >
                <div class="chat-bubble-avatar">
                  <img v-if="message.senderAvatar" :src="message.senderAvatar" alt="avatar" />
                  <span v-else>{{ messageInitial(message) }}</span>
                </div>
                <div class="chat-bubble">
                  <div class="chat-bubble-meta">
                    <strong>{{ message.senderName || `用户 ${message.senderId}` }}</strong>
                    <span>{{ formatTime(message.createdAt) }}</span>
                  </div>
                  <p>{{ message.content }}</p>
                </div>
              </div>
            </div>

            <form class="comment-form" @submit.prevent="sendMessage">
              <label class="field">
                <span>{{ selectedRoomIsDirect ? '发送私聊消息' : '发送群消息' }}</span>
                <textarea
                  v-model="messageDraft"
                  rows="3"
                  maxlength="500"
                  :placeholder="selectedRoomIsDirect ? '输入你想对好友说的话，发送后会实时同步。' : '输入你想发送的内容，按发送即可实时同步到群里。'"
                ></textarea>
              </label>
              <div class="chat-input-actions">
                <span class="field-tip">支持 500 字以内文本消息。</span>
                <button class="primary-btn" type="submit">发送消息</button>
              </div>
            </form>
          </template>
        </template>

        <p v-if="actionError" class="form-alert error">{{ actionError }}</p>
        <p v-if="actionSuccess" class="form-alert success">{{ actionSuccess }}</p>
      </div>
    </section>
  </div>
</template>
