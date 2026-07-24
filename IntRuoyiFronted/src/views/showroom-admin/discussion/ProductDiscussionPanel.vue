<template>
  <div class="showroom-discussion-panel">
    <div class="showroom-discussion-panel__toolbar">
      <div>
        <div class="showroom-discussion-panel__section-title">讨论线程</div>
        <div class="showroom-discussion-panel__meta">
          {{ productId ? `产品 ID：${productId}` : '请选择产品查看讨论线程' }}
        </div>
      </div>
      <div class="showroom-discussion-panel__actions">
        <el-select v-model="filters.anchorType" clearable placeholder="锚点类型">
          <el-option label="字段" value="FIELD" />
          <el-option label="模块" value="MODULE" />
          <el-option label="审批单" value="CHANGE_REQUEST" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="线程状态">
          <el-option label="处理中" value="OPEN" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
        <el-button :loading="loading" @click="loadComments">
          <Icon class="mr-5px" icon="ep:refresh" />
          刷新
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      :closable="false"
      show-icon
      title="产品讨论加载失败"
      type="error"
      :description="loadError"
    />

    <div class="showroom-discussion-panel__body">
      <div class="showroom-discussion-panel__list-shell">
        <div class="showroom-discussion-panel__section-title">发起讨论</div>
        <el-form label-width="96px">
          <el-form-item label="锚点类型">
            <el-select v-model="createForm.anchorType">
              <el-option label="字段" value="FIELD" />
              <el-option label="模块" value="MODULE" />
              <el-option label="审批单" value="CHANGE_REQUEST" />
            </el-select>
          </el-form-item>
          <el-form-item label="锚点标识">
            <el-input v-model="createForm.anchorKey" placeholder="字段 key / 模块 code / 审批单上下文" />
          </el-form-item>
          <el-form-item label="审批单 ID">
            <el-input-number v-model="createForm.changeRequestId" :min="1" />
          </el-form-item>
          <el-form-item label="讨论内容">
            <el-input v-model="createForm.content" :rows="4" type="textarea" />
          </el-form-item>
          <el-form-item>
            <el-button :loading="actionLoading" type="primary" @click="handleCreateThread">
              发起讨论
            </el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <el-table
          v-loading="loading"
          :data="threads"
          highlight-current-row
          row-key="root.commentId"
          @current-change="handleCurrentChange"
        >
          <el-table-column label="线程" min-width="120">
            <template #default="{ row }">#{{ row.root.commentId }}</template>
          </el-table-column>
          <el-table-column label="锚点" min-width="120">
            <template #default="{ row }">
              {{ resolveAnchorTypeText(row.root.anchorType) }} / {{ row.root.anchorKey || '未指定' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="resolveDiscussionStatusTagType(row.root.status)">
                {{ resolveDiscussionStatusText(row.root.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="回复数" width="84">
            <template #default="{ row }">{{ row.replies.length }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="showroom-discussion-panel__detail-shell">
        <div class="showroom-discussion-panel__section-title">线程详情</div>
        <el-empty v-if="!activeThread" description="请选择一条讨论线程查看详情" />

        <template v-else>
          <div class="showroom-discussion-panel__thread-card">
            <div class="showroom-discussion-panel__thread-head">
              <div>
                <div class="showroom-discussion-panel__thread-title">
                  {{ resolveAnchorTypeText(activeThread.root.anchorType) }}
                  {{ activeThread.root.anchorKey || '未指定锚点' }}
                </div>
                <div class="showroom-discussion-panel__thread-meta">
                  创建人 #{{ activeThread.root.createdBy }}
                  <span v-if="activeThread.root.changeRequestId">
                    / 审批单 #{{ activeThread.root.changeRequestId }}
                  </span>
                </div>
              </div>
              <el-button
                :disabled="activeThread.root.status === 'RESOLVED'"
                :loading="actionLoading"
                type="success"
                @click="handleResolveThread"
              >
                解决讨论
              </el-button>
            </div>
            <div class="showroom-discussion-panel__thread-content">
              {{ activeThread.root.content }}
            </div>
          </div>

          <div class="showroom-discussion-panel__reply-list">
            <div class="showroom-discussion-panel__reply-title">回复</div>
            <el-empty v-if="activeThread.replies.length === 0" description="当前线程还没有回复" />
            <div
              v-for="reply in activeThread.replies"
              :key="reply.commentId"
              class="showroom-discussion-panel__reply-card"
            >
              <div class="showroom-discussion-panel__reply-meta">
                回复 #{{ reply.commentId }} / 创建人 #{{ reply.createdBy }}
              </div>
              <div>{{ reply.content }}</div>
            </div>
          </div>

          <el-form class="mt-16px" label-width="96px">
            <el-form-item label="回复">
              <el-input
                v-model="replyDraft"
                :rows="4"
                placeholder="回复当前讨论线程"
                type="textarea"
              />
            </el-form-item>
            <el-form-item>
              <el-button :loading="actionLoading" type="primary" @click="handleReply">
                回复
              </el-button>
            </el-form-item>
          </el-form>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import request from '@/config/axios'
import { ShowroomAdminApi } from '@/api/showroom-admin'
import { useUserStore } from '@/store/modules/user'
import {
  buildDiscussionThreads,
  normalizeDiscussionPage,
  resolveAnchorTypeText,
  resolveDiscussionStatusTagType,
  resolveDiscussionStatusText,
  type ShowroomDiscussionThread
} from './contracts'

defineOptions({ name: 'ProductDiscussionPanel' })

const props = withDefaults(
  defineProps<{
    productId?: number | null
  }>(),
  {
    productId: null
  }
)

const message = useMessage()
const userStore = useUserStore()
const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')
const comments = ref<ReturnType<typeof normalizeDiscussionPage>>([])
const productDetail = ref<Record<string, unknown> | null>(null)
const activeThreadId = ref<number | null>(null)

const filters = reactive({
  anchorType: '',
  status: ''
})

const createForm = reactive({
  anchorType: 'FIELD' as 'FIELD' | 'MODULE' | 'CHANGE_REQUEST',
  anchorKey: '',
  changeRequestId: null as number | null,
  content: ''
})

const replyDraft = ref('')

const threads = computed(() => buildDiscussionThreads(comments.value))

const activeThread = computed<ShowroomDiscussionThread | null>(() => {
  return threads.value.find((item) => item.root.commentId === activeThreadId.value) || null
})

const loadComments = async () => {
  if (!props.productId) {
    comments.value = []
    productDetail.value = null
    activeThreadId.value = null
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const [page, detail] = await Promise.all([
      ShowroomAdminApi.getProductCommentPage({
        productId: props.productId,
        anchorType: filters.anchorType || undefined,
        status: filters.status || undefined
      }),
      ShowroomAdminApi.getProduct(props.productId)
    ])
    comments.value = normalizeDiscussionPage(page)
    productDetail.value = detail as Record<string, unknown>
    const nextId =
      activeThreadId.value && threads.value.some((thread) => thread.root.commentId === activeThreadId.value)
        ? activeThreadId.value
        : threads.value[0]?.root.commentId || null
    activeThreadId.value = nextId
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    loadError.value = resolved.message
    message.error(`产品讨论加载失败：${resolved.message}`)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.productId,
  () => {
    void loadComments()
  },
  { immediate: true }
)

const handleCurrentChange = (thread?: ShowroomDiscussionThread) => {
  activeThreadId.value = thread?.root.commentId || null
  replyDraft.value = ''
}

const handleCreateThread = async () => {
  if (!props.productId || !userStore.getUser.id) {
    throw new Error('产品上下文或当前登录用户缺失，无法发起讨论')
  }
  const targetRevisionId =
    productDetail.value?.currentRevisionId && typeof productDetail.value.currentRevisionId === 'number'
      ? productDetail.value.currentRevisionId
      : null
  if (!targetRevisionId) {
    throw new Error('产品详情缺少 currentRevisionId，无法锚定讨论线程')
  }
  if (!createForm.content.trim()) {
    throw new Error('讨论内容不能为空')
  }
  actionLoading.value = true
  try {
    await ShowroomAdminApi.createProductComment({
      productId: props.productId,
      targetRevisionId,
      changeRequestId: createForm.changeRequestId,
      anchorType: createForm.anchorType,
      anchorKey: createForm.anchorKey.trim(),
      createdBy: userStore.getUser.id,
      content: createForm.content.trim()
    })
    createForm.content = ''
    createForm.anchorKey = ''
    createForm.changeRequestId = null
    message.success('讨论线程已创建')
    await loadComments()
  } finally {
    actionLoading.value = false
  }
}

const handleReply = async () => {
  if (!activeThread.value || !userStore.getUser.id) {
    throw new Error('请选择要回复的讨论线程')
  }
  if (!replyDraft.value.trim()) {
    throw new Error('回复内容不能为空')
  }
  actionLoading.value = true
  try {
    await request.post({
      url: '/showroom/product-comment/reply',
      data: {
        commentId: activeThread.value.root.commentId,
        createdBy: userStore.getUser.id,
        content: replyDraft.value.trim()
      }
    })
    replyDraft.value = ''
    message.success('回复已发送')
    await loadComments()
  } finally {
    actionLoading.value = false
  }
}

const handleResolveThread = async () => {
  if (!activeThread.value || !userStore.getUser.id) {
    throw new Error('请选择要解决的讨论线程')
  }
  actionLoading.value = true
  try {
    await request.post({
      url: '/showroom/product-comment/resolve',
      data: {
        commentId: activeThread.value.root.commentId,
        resolvedBy: userStore.getUser.id
      }
    })
    message.success('讨论线程已标记为解决')
    await loadComments()
  } finally {
    actionLoading.value = false
  }
}
</script>

<style scoped>
.showroom-discussion-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.showroom-discussion-panel__toolbar,
.showroom-discussion-panel__list-shell,
.showroom-discussion-panel__detail-shell {
  background: #ffffff;
  border: 1px solid #dbe3ef;
}

.showroom-discussion-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
}

.showroom-discussion-panel__actions {
  display: flex;
  gap: 8px;
}

.showroom-discussion-panel__body {
  display: grid;
  grid-template-columns: minmax(380px, 42%) minmax(0, 1fr);
  gap: 12px;
}

.showroom-discussion-panel__list-shell,
.showroom-discussion-panel__detail-shell {
  padding: 12px;
  border-radius: 0 0 8px 8px;
}

.showroom-discussion-panel__section-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 0.95rem;
  font-weight: 600;
}

.showroom-discussion-panel__meta {
  color: #4b5563;
  font-size: 0.85rem;
}

.showroom-discussion-panel__thread-card,
.showroom-discussion-panel__reply-card {
  padding: 12px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 6px;
}

.showroom-discussion-panel__thread-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.showroom-discussion-panel__thread-title,
.showroom-discussion-panel__reply-title {
  color: #172033;
  font-size: 0.95rem;
  font-weight: 600;
}

.showroom-discussion-panel__thread-meta,
.showroom-discussion-panel__reply-meta {
  margin-top: 4px;
  color: #4b5563;
  font-size: 0.82rem;
}

.showroom-discussion-panel__thread-content {
  color: #263247;
  line-height: 1.7;
}

.showroom-discussion-panel__reply-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}

@media (max-width: 1100px) {
  .showroom-discussion-panel__body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .showroom-discussion-panel__toolbar,
  .showroom-discussion-panel__thread-head {
    align-items: stretch;
    flex-direction: column;
  }

  .showroom-discussion-panel__actions {
    flex-wrap: wrap;
  }
}
</style>
