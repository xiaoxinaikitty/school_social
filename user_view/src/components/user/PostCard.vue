<script setup>
import { computed } from 'vue'
import { ChatDotRound, Pointer, Star, View } from '@element-plus/icons-vue'
import { formatSnippet, getPostTypeLabel, getStatusMeta } from '../../utils/user'

const props = defineProps({
  post: {
    type: Object,
    required: true,
  },
  badge: {
    type: String,
    default: '',
  },
  compact: {
    type: Boolean,
    default: false,
  },
  showStatus: {
    type: Boolean,
    default: true,
  },
})
const emit = defineEmits(['open'])

const statusMeta = computed(() => getStatusMeta(props.post.status))
const contentPreview = computed(() => formatSnippet(props.post.content, props.compact ? 80 : 130))

const emitOpen = () => {
  emit('open', props.post)
}
</script>

<template>
  <el-card class="campus-post-card" shadow="hover">
    <div class="campus-post-card__head">
      <div class="campus-post-card__badges">
        <el-tag v-if="badge" effect="light" round>{{ badge }}</el-tag>
        <el-tag effect="plain" round type="info">{{ getPostTypeLabel(post.postType) }}</el-tag>
      </div>
      <el-tag v-if="showStatus" :type="statusMeta.type" effect="light" round>
        {{ statusMeta.label }}
      </el-tag>
    </div>

    <RouterLink class="campus-post-card__title" :to="`/posts/${post.id}`" @click="emitOpen">
      {{ post.title || '未命名内容' }}
    </RouterLink>

    <p class="campus-post-card__content">
      {{ contentPreview }}
    </p>

    <div class="campus-post-card__meta">
      <span v-if="post.createdAt">{{ post.createdAt }}</span>
      <span v-if="post.college">{{ post.college }}</span>
      <span v-if="post.location">{{ post.location }}</span>
    </div>

    <div class="campus-post-card__stats">
      <span><el-icon><Pointer /></el-icon>{{ post.likeCount ?? 0 }}</span>
      <span><el-icon><ChatDotRound /></el-icon>{{ post.commentCount ?? 0 }}</span>
      <span><el-icon><Star /></el-icon>{{ post.favoriteCount ?? 0 }}</span>
      <span><el-icon><View /></el-icon>{{ post.viewCount ?? 0 }}</span>
    </div>

    <div class="campus-post-card__footer">
      <el-button type="primary" plain :link="compact" @click="emitOpen(); $router.push(`/posts/${post.id}`)">
        查看详情
      </el-button>
      <slot name="actions" :post="post" />
    </div>
  </el-card>
</template>
