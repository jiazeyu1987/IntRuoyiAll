<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import * as NotifyMessageApi from '@/api/system/notify/message'
import { useUserStoreWithOut } from '@/store/modules/user'
import { propTypes } from '@/utils/propTypes'
import {
  getNotifyMessageTarget,
  hasNotifyMessageTarget,
  navigateToNotifyMessageTarget
} from '@/utils/notifyMessageNavigation'

defineOptions({ name: 'Message' })

defineProps({
  color: propTypes.string.def('')
})

const router = useRouter()
const { push } = router
const userStore = useUserStoreWithOut()
const activeName = ref('notice')
const unreadCount = ref(0) // 未读消息数量
const list = ref<NotifyMessageApi.NotifyMessageVO[]>([]) // 消息列表

const normalizeMessageText = (value: unknown) => String(value ?? '').replace(/\s+/g, ' ').trim()

const escapeRegExp = (value: string) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const stripEntryInfo = (content: unknown) =>
  normalizeMessageText(content)
    .replace(
      /(?:^|[\s，,。；;])入口(?:信息)?\s*[:：]\s*(?:https?:\/\/\S+|\/\S+|[^\s，,。；;]+)/g,
      ' '
    )
    .replace(/\s+/g, ' ')
    .trim()

const removeSourcePrefix = (content: string, source: string) => {
  if (!source) {
    return content
  }
  return content.replace(new RegExp(`^${escapeRegExp(source)}\\s*[:：]\\s*`), '').trim()
}

const splitMessageContent = (content: string) => {
  const colonMatch = content.match(/^([^：:。；;]{2,32})\s*[：:]\s*(.+)$/)
  if (colonMatch) {
    return {
      headline: colonMatch[1].trim(),
      body: colonMatch[2].trim()
    }
  }
  if (content.length > 42) {
    return {
      headline: content.slice(0, 42).trim(),
      body: content.slice(42).trim()
    }
  }
  return {
    headline: content,
    body: ''
  }
}

const getMessageDisplay = (item: NotifyMessageApi.NotifyMessageVO) => {
  const source = normalizeMessageText(item.templateNickname) || '站内信'
  const content = removeSourcePrefix(stripEntryInfo(item.templateContent), source)
  const { headline, body } = splitMessageContent(content)

  return {
    source,
    headline: headline || '站内信提醒',
    body
  }
}

const handleMessageCardClick = async (item: NotifyMessageApi.NotifyMessageVO) => {
  const target = getNotifyMessageTarget(item)
  if (!target) {
    return
  }
  if (!item.readStatus) {
    await NotifyMessageApi.updateNotifyMessageRead(item.id)
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  await navigateToNotifyMessageTarget(router, target)
}

// 获得消息列表
const getList = async () => {
  list.value = await NotifyMessageApi.getUnreadNotifyMessageList()
  unreadCount.value = list.value.length
}

// 获得未读消息数
const getUnreadCount = async () => {
  NotifyMessageApi.getUnreadNotifyMessageCount().then((data) => {
    unreadCount.value = data
  })
}

// 跳转我的站内信
const goMyList = () => {
  push({
    name: 'Profile',
    query: {
      tab: 'notifyMessage'
    }
  })
}

// ========== 初始化 =========
onMounted(() => {
  // 首次加载小红点
  getUnreadCount()
  // 轮询刷新小红点
  setInterval(
    () => {
      if (userStore.getIsSetUser) {
        getUnreadCount()
      } else {
        unreadCount.value = 0
      }
    },
    1000 * 60 * 2
  )
})
</script>
<template>
  <div class="message">
    <ElPopover :width="420" placement="bottom" trigger="click">
      <template #reference>
        <ElBadge :is-dot="unreadCount > 0" class="item">
          <Icon :size="18" class="cursor-pointer" icon="ep:bell" :color="color" @click="getList" />
        </ElBadge>
      </template>
      <ElTabs v-model="activeName">
        <ElTabPane label="我的站内信" name="notice">
          <el-scrollbar class="message-list">
            <template v-for="item in list" :key="item.id">
              <div
                class="message-card"
                :class="{ 'message-card--clickable': hasNotifyMessageTarget(item) }"
                :role="hasNotifyMessageTarget(item) ? 'button' : undefined"
                :tabindex="hasNotifyMessageTarget(item) ? 0 : undefined"
                @click="handleMessageCardClick(item)"
                @keydown.enter="handleMessageCardClick(item)"
                @keydown.space.prevent="handleMessageCardClick(item)"
              >
                <div class="message-card__icon">
                  <Icon :size="18" icon="ep:message" />
                </div>
                <div class="message-card__content">
                  <div class="message-card__meta">
                    <span class="message-card__source">{{ getMessageDisplay(item).source }}</span>
                    <span class="message-card__date">{{ formatDate(item.createTime) }}</span>
                  </div>
                  <div class="message-card__headline" :title="getMessageDisplay(item).headline">
                    {{ getMessageDisplay(item).headline }}
                  </div>
                  <div
                    v-if="getMessageDisplay(item).body"
                    class="message-card__body"
                    :title="getMessageDisplay(item).body"
                  >
                    {{ getMessageDisplay(item).body }}
                  </div>
                </div>
              </div>
            </template>
          </el-scrollbar>
        </ElTabPane>
      </ElTabs>
      <!-- 更多 -->
      <div style="margin-top: 10px; text-align: right">
        <XButton preIcon="ep:view" title="查看全部" type="primary" @click="goMyList" />
      </div>
    </ElPopover>
  </div>
</template>
<style lang="scss" scoped>
.message-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 260px;
  line-height: 45px;
}

.message-list {
  height: 400px;

  :deep(.el-scrollbar__view) {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 4px 4px 2px;
  }
}

.message-card {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #fff;
  border: 1px solid #e5ebf3;
  border-radius: 8px;

  &:hover {
    background: #fafcff;
    border-color: #dbe3ef;
  }
}

.message-card--clickable {
  cursor: pointer;

  &:focus-visible {
    outline: 2px solid #1677ff;
    outline-offset: 2px;
  }

  .message-card__headline {
    color: #1677ff;
  }
}

.message-card__icon {
  display: flex;
  flex: 0 0 30px;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  margin-top: 2px;
  color: #1677ff;
  background: #eef5ff;
  border-radius: 6px;
}

.message-card__content {
  min-width: 0;
  flex: 1;
}

.message-card__meta {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.message-card__source {
  min-width: 0;
  padding: 2px 7px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  color: #1677ff;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #eef5ff;
  border-radius: 5px;
}

.message-card__date {
  flex: 0 0 auto;
  font-size: 12px;
  color: #8a95a6;
}

.message-card__headline {
  margin-bottom: 5px;
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  color: #172033;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-card__body {
  display: -webkit-box;
  overflow: hidden;
  font-size: 13px;
  line-height: 20px;
  color: #4b5563;
  text-overflow: ellipsis;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
</style>
