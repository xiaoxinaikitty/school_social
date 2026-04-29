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

const messages = ref([])
const messagesLoading = ref(false)
const messageError = ref('')
const messagePage = ref(1)
const messageSize = 30
const messageTotal = ref(0)

const createRoomName = ref('')
const createRoomDescription = ref('')
const creatingRoom = ref(false)

const messageDraft = ref('')
const actionError = ref('')
const actionSuccess = ref('')
const wsConnected = ref(false)
const messageListRef = ref(null)

let socket = null

const selectedRoom = computed(() =>
  rooms.value.find((item) => Number(item.id) === Number(currentRoomId.value)) || null,
)

const messagePages = computed(() => {
  const pages = Math.ceil(messageTotal.value / messageSize)
  return pages > 0 ? pages : 1
})

const formatTime = (value) => {
  if (!value) return '暂无'
  return String(value).replace('T', ' ')
}

const roomInitial = (room) => {
  if (!room?.name) return '群'
  return room.name.slice(0, 1)
}

const messageInitial = (message) => {
  if (message?.senderName) return message.senderName.slice(0, 1)
  return '聊'
}

const isMine = (message) => Number(message?.senderId) === Number(currentUser.value?.id)

const closeSocket = () => {
  wsConnected.value = false
  if (socket) {
    socket.close()
    socket = null
  }
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
  } catch (err) {
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
  } catch (err) {
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
        actionError.value = payload.message || '群聊消息发送失败。'
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
    } catch (err) {
      actionError.value = '收到无法识别的群聊消息。'
    }
  }
  socket.onclose = () => {
    if (Number(currentRoomId.value) === Number(roomId)) {
      wsConnected.value = false
    }
  }
  socket.onerror = () => {
    actionError.value = '群聊实时连接失败，请稍后重试。'
  }
}

const activateRoom = async (roomId) => {
  currentRoomId.value = roomId
  actionError.value = ''
  actionSuccess.value = ''
  messageError.value = ''
  const room = rooms.value.find((item) => Number(item.id) === Number(roomId))
  closeSocket()
  messages.value = []
  messagePage.value = 1
  messageTotal.value = 0
  if (!room) return
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
      roomError.value = data.message || '获取群聊列表失败。'
      return
    }
    rooms.value = data.data || []
    const targetId = preferredRoomId
      || currentRoomId.value
      || rooms.value.find((item) => item.joined === 1)?.id
      || rooms.value[0]?.id
      || null
    if (!targetId) return
    if (!activate) {
      return
    }
    await activateRoom(targetId)
  } catch (err) {
    roomError.value = '网络错误，无法获取群聊列表。'
  } finally {
    roomsLoading.value = false
  }
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
    actionSuccess.value = '群聊已创建。'
    await loadRooms(data.data?.id)
  } catch (err) {
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
  } catch (err) {
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
  } catch (err) {
    actionError.value = '网络错误，无法退出群聊。'
  }
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
  } catch (err) {
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
        <span class="hero-tag">社区群聊</span>
        <h1>实时校园交流空间</h1>
        <p>支持建群、加群、查看历史消息与实时发言。</p>
      </div>
      <div class="profile-actions">
        <button class="ghost-btn" type="button" @click="router.push('/home')">返回主页</button>
        <button class="ghost-btn" type="button" @click="loadRooms(currentRoomId)">刷新群聊</button>
      </div>
    </header>

    <section class="profile-section chat-layout">
      <aside class="chat-sidebar">
        <div class="profile-header">
          <div>
            <h2>群聊列表</h2>
            <p>{{ rooms.length }} 个群聊可用</p>
          </div>
        </div>

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

        <div v-if="roomError" class="form-alert error">{{ roomError }}</div>
        <div v-if="roomsLoading" class="feed-empty">群聊列表加载中...</div>
        <div v-else-if="rooms.length === 0" class="feed-empty">暂无群聊，先创建一个吧。</div>
        <div v-else class="chat-room-list">
          <article
            v-for="room in rooms"
            :key="room.id"
            class="chat-room-card"
            :class="{ active: Number(room.id) === Number(currentRoomId) }"
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
                  <span class="chat-pill" :class="{ active: room.joined === 1 }">
                    {{ room.joined === 1 ? '已加入' : '未加入' }}
                  </span>
                </div>
                <p class="chat-room-desc">{{ room.lastMessage || room.description || '暂无消息，欢迎发言。' }}</p>
                <div class="chat-room-foot">
                  <span>{{ room.memberCount || 0 }} 人</span>
                  <span>{{ formatTime(room.lastMessageAt || room.createdAt) }}</span>
                  <span v-if="room.unreadCount" class="chat-unread">{{ room.unreadCount }}</span>
                </div>
              </div>
            </div>
            <div class="chat-room-actions">
              <button
                v-if="room.joined !== 1"
                class="ghost-btn"
                type="button"
                @click.stop="joinRoom(room.id)"
              >
                加入
              </button>
              <button
                v-else-if="Number(room.ownerId) !== Number(currentUser?.id)"
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
          <h3>请选择一个群聊</h3>
          <p>左侧可加入已有群聊，或创建新的校园群。</p>
        </div>

        <template v-else>
          <div class="profile-header">
            <div>
              <h2>{{ selectedRoom.name }}</h2>
              <p>{{ selectedRoom.description || '公开社区群聊，欢迎实时交流。' }}</p>
            </div>
            <div class="discover-actions">
              <span class="chat-status" :class="{ online: wsConnected }">
                {{ wsConnected ? '实时连接中' : selectedRoom.joined === 1 ? '连接未建立' : '尚未加入群聊' }}
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
                <span>发送群消息</span>
                <textarea
                  v-model="messageDraft"
                  rows="3"
                  maxlength="500"
                  placeholder="输入你想发送的内容，按发送即可实时同步到群里。"
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
