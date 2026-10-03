<template>
  <span v-if="canQuery" class="release-task-notification-entry">
    <el-button link type="primary" data-release-task-notification-entry @click="open">
      交接通知
    </el-button>
    <Dialog v-model="visible" title="交接通知" width="900px" data-release-task-notification-dialog>
      <div v-loading="loading">
        <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon data-release-task-notification-error />
        <el-alert v-if="retryError" :title="retryError" type="error" :closable="false" show-icon data-release-task-notification-retry-error />
        <el-button :disabled="loading || retrying" data-release-task-notification-refresh @click="refreshCurrent">
          刷新回执
        </el-button>
        <el-empty v-if="!loading && !loadError && !receipts.length" description="暂无交接通知投递记录" data-release-task-notification-empty />
        <el-table v-if="receipts.length" :data="receipts" row-key="id" border>
          <el-table-column prop="userId" label="收件人编号" width="115" />
          <el-table-column label="投递状态" width="100">
            <template #default="{ row }">{{ statusLabels[row.status] }}</template>
          </el-table-column>
          <el-table-column prop="attemptCount" label="投递次数" width="95" />
          <el-table-column prop="lastAttemptAt" label="最近尝试时间" min-width="170" />
          <el-table-column prop="sentAt" label="送达时间" min-width="170" />
          <el-table-column prop="lastErrorSummary" label="失败原因" min-width="200" />
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button
                v-if="canRetry(row)"
                link
                type="primary"
                :disabled="loading || retrying"
                :data-release-task-notification-retry="row.id"
                @click="selectRetry(row)"
              >
                补发本人通知
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-form v-if="selectedReceipt" label-position="top" @submit.prevent>
          <el-form-item label="补发原因">
            <el-input
              v-model="reason"
              type="textarea"
              :rows="3"
              :maxlength="1000"
              show-word-limit
              :disabled="retrying"
              placeholder="请输入本次补发原因"
              data-release-task-notification-reason
            />
          </el-form-item>
          <el-button
            type="primary"
            :loading="retrying"
            :disabled="loading || retrying || !reason.trim() || reason.length > 1000"
            data-release-task-notification-submit-retry
            @click="submitRetry"
          >
            确认补发
          </el-button>
        </el-form>
      </div>
      <template #footer>
        <el-button data-release-task-notification-close @click="visible = false">关闭</el-button>
      </template>
    </Dialog>
  </span>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { getEdhrRelease } from '@/api/mes/pro/edhr/release'
import {
  listReleaseTaskNotifications,
  retryReleaseTaskNotification,
  type ReleaseTaskNotificationReceipt
} from '@/api/mes/pro/productionRelease/notification'

const props = defineProps<{
  workTaskId?: string | number
  batchExecutionId?: string | number
  releaseTransactionId?: string | number
}>()
const userStore = useUserStore()
const canQuery = computed(() => userStore.permissions.has('*:*:*') ||
  userStore.permissions.has('mes:pro-edhr-work-task:query'))
const actorId = computed(() => String(userStore.getUser.id))
const visible = ref(false)
const loading = ref(false)
const retrying = ref(false)
const loadError = ref('')
const retryError = ref('')
const receipts = ref<ReleaseTaskNotificationReceipt[]>([])
const selectedReceipt = ref<ReleaseTaskNotificationReceipt>()
const reason = ref('')
const resolvedTaskId = ref<string>()
const pendingDeliveryIds = new Set<string>()
const statusLabels = { PENDING: '待投递', FAILED: '投递失败', SENT: '已送达' }
let mounted = true
let generation = 0
let querySequence = 0

const messageOf = (error: unknown) => error instanceof Error ? error.message : String(error)
const exactId = (value: unknown) => {
  if (typeof value === 'string' && /^[1-9]\d*$/.test(value)) return value
  if (typeof value === 'number' && Number.isSafeInteger(value) && value > 0) return String(value)
  throw new Error('交接通知缺少有效的正式身份，请刷新原业务列表。')
}
const current = (token: number) => mounted && visible.value && canQuery.value && token === generation
const clearSelection = () => {
  selectedReceipt.value = undefined
  reason.value = ''
}
const invalidate = () => {
  generation += 1
  querySequence += 1
  resolvedTaskId.value = undefined
  receipts.value = []
  loading.value = false
  retrying.value = false
  loadError.value = ''
  retryError.value = ''
  clearSelection()
}

const validateReceipts = (rows: ReleaseTaskNotificationReceipt[], taskId: string) => {
  if (!Array.isArray(rows)) throw new Error('交接通知回执响应格式无效。')
  const ids = new Set<string>()
  for (const row of rows) {
    if (!row || typeof row.id !== 'string' || typeof row.userId !== 'string' ||
      exactId(row.id) !== row.id || exactId(row.userId) !== row.userId || row.workTaskId !== taskId ||
      !['PENDING', 'FAILED', 'SENT'].includes(row.status) || ids.has(row.id) ||
      !Number.isSafeInteger(row.rowVersion) || row.rowVersion < 0 ||
      !Number.isSafeInteger(row.attemptCount) || row.attemptCount < 0) {
      throw new Error('交接通知回执与当前任务不一致或版本无效，请重新查询。')
    }
    ids.add(row.id)
  }
  return rows
}

const refresh = async (token: number) => {
  if (!current(token) || !resolvedTaskId.value) return
  const taskId = resolvedTaskId.value
  const sequence = ++querySequence
  loading.value = true
  loadError.value = ''
  receipts.value = []
  clearSelection()
  try {
    const rows = await listReleaseTaskNotifications(taskId)
    if (!current(token) || sequence !== querySequence) return
    receipts.value = validateReceipts(rows, taskId)
  } catch (error) {
    if (current(token) && sequence === querySequence) loadError.value = messageOf(error)
  } finally {
    if (current(token) && sequence === querySequence) loading.value = false
  }
}

const open = async () => {
  if (!canQuery.value) return
  invalidate()
  visible.value = true
  const token = generation
  loading.value = true
  try {
    const hasTask = props.workTaskId !== undefined
    const hasBatch = props.batchExecutionId !== undefined
    const hasTransaction = props.releaseTransactionId !== undefined
    if (hasTask ? hasBatch || hasTransaction : !hasBatch || !hasTransaction) {
      throw new Error('交接通知必须提供明确任务，或同一正式批次和上市放行事务。')
    }
    let taskId: string
    if (hasTask) {
      taskId = exactId(props.workTaskId)
    } else {
      const batchId = exactId(props.batchExecutionId)
      const transactionId = exactId(props.releaseTransactionId)
      const release = await getEdhrRelease(transactionId)
      if (!current(token)) return
      if (exactId(release.releaseTransactionId) !== transactionId || exactId(release.batchExecutionId) !== batchId) {
        throw new Error('上市放行事务与所选批次不一致，不能读取交接通知。')
      }
      taskId = exactId(release.releaseApprovalWorkTaskId)
    }
    if (!current(token)) return
    resolvedTaskId.value = taskId
    await refresh(token)
  } catch (error) {
    if (current(token)) loadError.value = messageOf(error)
  } finally {
    if (current(token)) loading.value = false
  }
}

const canRetry = (row: ReleaseTaskNotificationReceipt) => canQuery.value &&
  row.workTaskId === resolvedTaskId.value && row.userId === actorId.value &&
  (row.status === 'PENDING' || row.status === 'FAILED') && !pendingDeliveryIds.has(row.id)

const selectRetry = (row: ReleaseTaskNotificationReceipt) => {
  if (loading.value || retrying.value || !canRetry(row) || !receipts.value.includes(row)) return
  selectedReceipt.value = { ...row }
  reason.value = ''
  retryError.value = ''
}

const submitRetry = async () => {
  if (!canQuery.value || !visible.value || loading.value || retrying.value) return
  const selected = selectedReceipt.value
  const receipt = selected && receipts.value.find(row => row.id === selected.id && row.rowVersion === selected.rowVersion)
  if (!selected || !receipt || !canRetry(receipt)) {
    retryError.value = '回执已变化或当前用户不能补发，请重新查询。'
    return
  }
  const retryReason = reason.value.trim()
  if (!retryReason || reason.value.length > 1000) {
    retryError.value = '补发原因不能为空，且不能超过1000字。'
    return
  }
  const token = generation
  retrying.value = true
  retryError.value = ''
  pendingDeliveryIds.add(receipt.id)
  try {
    const result = await retryReleaseTaskNotification({
      deliveryId: receipt.id,
      expectedVersion: receipt.rowVersion,
      reason: retryReason
    })
    if (result !== true) throw new Error('未收到明确的投递结果，请核对最新回执。')
  } catch (error) {
    if (current(token)) retryError.value = messageOf(error)
  } finally {
    pendingDeliveryIds.delete(receipt.id)
    if (current(token)) {
      await refresh(token)
      if (current(token)) retrying.value = false
    }
  }
}

const refreshCurrent = async () => {
  if (loading.value || retrying.value) return
  if (!resolvedTaskId.value) await open()
  else await refresh(generation)
}

watch(visible, value => { if (!value) invalidate() }, { flush: 'sync' })
watch(() => [props.workTaskId, props.batchExecutionId, props.releaseTransactionId, actorId.value, canQuery.value], () => {
  visible.value = false
  invalidate()
}, { flush: 'sync' })
onBeforeUnmount(() => {
  mounted = false
  invalidate()
})
</script>
