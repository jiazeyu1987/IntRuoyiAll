<template>
  <el-card data-active-order-handoff-panel class="mb-4">
    <template #header><span>我的生产交接</span><el-button class="ml-4" :loading="loading" @click="load">刷新交接</el-button></template>
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-4" />
    <el-table :data="tasks" v-loading="loading" row-key="id">
      <el-table-column label="当轮订单" prop="activeOrderId" width="150" />
      <el-table-column label="交接事项"><template #default="{ row }">{{ labels[row.taskType] }}</template></el-table-column>
      <el-table-column label="原因 / 业务意见" prop="reason" min-width="200" />
      <el-table-column label="状态" width="95"><template #default="{ row }">{{ states[row.status] }}</template></el-table-column>
      <el-table-column label="操作" width="225"><template #default="{ row }">
        <el-button link type="primary" :disabled="row.status === 'CANCELED'" @click="openTask(row)">{{ row.status === 'TODO' ? '处理原轮次' : '查看原轮次' }}</el-button>
        <el-button link type="primary" @click="showReceipts(row)">通知回执</el-button>
      </template></el-table-column>
    </el-table>
    <el-dialog v-model="resultOpen" title="原周期作废交接结果" width="680px">
      <el-descriptions v-if="result" :column="1" border>
        <el-descriptions-item label="活跃周期">{{ result.activeOrderId }}</el-descriptions-item>
        <el-descriptions-item label="原评审 / 轮次">{{ result.sourceId }} / {{ result.roundId }}</el-descriptions-item>
        <el-descriptions-item label="正式处置意见">{{ result.reason }}</el-descriptions-item>
        <el-descriptions-item label="完成者">{{ result.completedBy }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ result.completedAt }}</el-descriptions-item>
      </el-descriptions>
      <el-alert title="这是原周期已正式完成的作废结果，只能查看。" type="info" :closable="false" class="mt-4" />
    </el-dialog>
    <el-dialog v-model="receiptOpen" title="本人交接通知回执" width="680px" @close="closeReceipt">
      <el-alert v-if="receiptError" :title="receiptError" type="error" :closable="false" class="mb-4" />
      <el-table :data="receipts" v-loading="receiptLoading">
        <el-table-column label="投递状态"><template #default="{ row }">{{ deliveryStates[row.status] }}</template></el-table-column>
        <el-table-column label="尝试次数" prop="attemptCount" />
        <el-table-column label="失败原因" prop="lastErrorSummary" min-width="220" />
        <el-table-column label="发送时间" prop="sentAt" />
      </el-table>
      <el-input v-model="retryReason" type="textarea" maxlength="1000" placeholder="填写通知重试原因" class="mt-4" />
      <template #footer>
        <el-button :loading="receiptLoading" @click="refreshReceipts">刷新回执</el-button>
        <el-button type="primary" :loading="retrying" :disabled="!retryable || !retryReason.trim() || receiptLoading" @click="retry">重试本人通知</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as api from '@/api/mes/pro/handoff'
import { resolveActiveOrderHandoffTarget, navigateToActiveOrderHandoff } from '@/utils/activeOrderHandoffNavigation'
const router = useRouter(), route = useRoute()
const resultOpen = ref(false), result = ref<api.HandoffTask>()
let resultEpoch = 0
const tasks = ref<api.HandoffTask[]>([]), loading = ref(false), error = ref('')
const selected = ref<api.HandoffTask>(), receipts = ref<api.HandoffReceipt[]>([])
const receiptOpen = ref(false), receiptLoading = ref(false), receiptError = ref(''), retryReason = ref(''), retrying = ref(false)
let listEpoch = 0, receiptEpoch = 0
const labels: Record<string, string> = { PRODUCTION_HANDOFF: '生产接手', PRODUCTION_REVIEW: '生产组长复核', PQC_HANDOFF: 'PQC检验接手', PQC_REVIEW: 'PQC组长复核', PRODUCTION_RETURN: '本人生产退回更正', PQC_RETURN: '本人PQC退回更正', QA_REVIEW: 'QA不合格评审', QA_DECISION_HANDOFF: 'QA处置结果交接' }
const states: Record<string, string> = { TODO: '待处理', DONE: '已完成', CANCELED: '周期已关闭' }
const deliveryStates = { PENDING: '待投递', FAILED: '投递失败', SENT: '已发送' }
const failure = (e: unknown) => e instanceof Error ? e.message : String(e)
const load = async () => {
  const epoch = ++listEpoch; loading.value = true; error.value = ''
  try { const rows = await api.myHandoffs(); if (epoch === listEpoch) tasks.value = rows }
  catch (e) { if (epoch === listEpoch) { tasks.value = []; error.value = failure(e) } }
  finally { if (epoch === listEpoch) loading.value = false }
}
const openTask = async (task: api.HandoffTask) => {
  try {
    const target = resolveActiveOrderHandoffTarget({ actionUrl: task.actionUrl, handoffTaskId: task.id, handoffType: task.taskType, activeOrderId: task.activeOrderId })
    if (!target) throw new Error('该交接缺少真实页面入口')
    await navigateToActiveOrderHandoff(router, target)
  } catch (e) { error.value = failure(e) }
}
const closeReceipt = () => { ++receiptEpoch; selected.value = undefined; receipts.value = []; receiptLoading.value = false; retrying.value = false }
const refreshReceipts = async () => {
  const task = selected.value, epoch = ++receiptEpoch
  if (!task) return
  receiptLoading.value = true; receiptError.value = ''; receipts.value = []
  try { const rows = await api.handoffReceipts(task.id); if (epoch === receiptEpoch && selected.value?.id === task.id) receipts.value = rows }
  catch (e) { if (epoch === receiptEpoch) receiptError.value = failure(e) }
  finally { if (epoch === receiptEpoch) receiptLoading.value = false }
}
const showReceipts = async (task: api.HandoffTask) => { selected.value = task; retryReason.value = ''; receiptOpen.value = true; await refreshReceipts() }
const retryable = computed(() => selected.value?.status !== 'CANCELED' && receipts.value.length === 1 && ['PENDING', 'FAILED'].includes(receipts.value[0].status))
const retry = async () => {
  if (!retryable.value || !retryReason.value.trim() || retrying.value) return
  const task = selected.value!, row = receipts.value[0], epoch = receiptEpoch
  retrying.value = true; receiptError.value = ''
  try {
    await api.retryHandoff({ id: row.id, rowVersion: row.rowVersion, reason: retryReason.value.trim() })
    if (epoch === receiptEpoch && selected.value?.id === task.id) { retryReason.value = ''; await refreshReceipts() }
  } catch (e) { if (epoch === receiptEpoch) receiptError.value = failure(e) }
  finally { if (selected.value?.id === task.id) retrying.value = false }
}
const openVoidResult = async () => {
  const epoch = ++resultEpoch; result.value = undefined; resultOpen.value = false
  if (route.query.handoffTaskId === undefined) return
  try {
    const id = route.query.handoffTaskId
    if (typeof id !== 'string' || !/^[1-9][0-9]*$/.test(id)) throw new Error('交接任务身份无效')
    const context = await api.handoffNavigationContext(id)
    if (epoch !== resultEpoch) return
    const task = context.task
    const target = resolveActiveOrderHandoffTarget({ actionUrl: task.actionUrl, handoffTaskId: task.id, handoffType: task.taskType, activeOrderId: task.activeOrderId })
    if (!target || target.path !== '/user/profile' || task.taskType !== 'QA_DECISION_HANDOFF' || task.status !== 'DONE'
      || !task.reason.startsWith('void：') || context.processable || route.query.handoffReadOnly !== '1'
      || Object.entries(target.query).some(([key, value]) => route.query[key] !== value)
      || Object.keys(route.query).some(key => !Object.hasOwn(target.query, key) && key !== 'handoffReadOnly'))
      throw new Error('作废结果入口与原任务、周期或轮次不一致')
    result.value = task; resultOpen.value = true
  } catch (e) { if (epoch === resultEpoch) error.value = failure(e) }
}
watch(() => [route.query.handoffTaskId, route.query.activeOrderId, route.query.reviewId, route.query.roundId, route.query.handoffType, route.query.handoffReadOnly], openVoidResult, { immediate: true })
onMounted(load)
</script>
