<template>
  <el-button link type="primary" data-market-release-receipt-open @click="openReceipt">核验签署结果</el-button>
  <Dialog v-model="visible" title="上市签署回执" width="560px" :fullscreen="false">
    <div v-loading="loading" data-market-release-receipt>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <el-alert v-else-if="!receipt && !loading" title="尚未查到本人的正式回执，结果仍待确认。请稍后重新核验，勿重复签署。" type="warning" :closable="false" show-icon />
      <el-descriptions v-if="receipt" :column="1" border>
        <el-descriptions-item label="核验结果">本人原签署回执核验通过</el-descriptions-item>
        <el-descriptions-item label="审批结果">批准上市放行</el-descriptions-item>
        <el-descriptions-item label="正式回执编号">{{ receipt.receiptId }}</el-descriptions-item>
        <el-descriptions-item label="电子签名编号">{{ receipt.signatureId }}</el-descriptions-item>
        <el-descriptions-item label="签署时间">{{ receipt.committedAt }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <template #footer><el-button :loading="loading" @click="openReceipt">重新核验</el-button><el-button @click="visible = false">关闭</el-button></template>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { Dialog } from '@/components/Dialog'
import { useUserStore } from '@/store/modules/user'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
import { getEdhrRelease } from '@/api/mes/pro/edhr/release'
import { getApprovalReviewReceipt, type ApprovalDecisionReceipt } from '@/api/approval-center'

const props = defineProps<{
  releaseTransactionId: string | number
  idempotencyKey?: string
  sourceTaskId?: string
  prepareId?: string
}>()
const emit = defineEmits<{ confirmed: [receipt: ApprovalDecisionReceipt] }>()
const visible = ref(false)
const loading = ref(false)
const error = ref('')
const receipt = ref<ApprovalDecisionReceipt | null>(null)
const userStore = useUserStore()
const invalidation = useApprovalCenterInvalidationStore()
let mounted = true
const identity = () => JSON.stringify([invalidation.syncSession().key, props.releaseTransactionId, props.idempotencyKey, props.sourceTaskId, props.prepareId])
watch(identity, () => { visible.value = false; receipt.value = null; error.value = '' })
onUnmounted(() => { mounted = false })

const openReceipt = async () => {
  if (loading.value) return
  visible.value = true
  loading.value = true
  error.value = ''
  receipt.value = null
  const actor = String(userStore.getUser.id)
  const transactionId = String(props.releaseTransactionId)
  const originalIdentity = identity()
  const isCurrent = () => mounted && visible.value && originalIdentity === identity()
  try {
    let key = props.idempotencyKey
    let taskId = props.sourceTaskId
    let evidenceHash: string | undefined
    if (!key) {
      const context = await getEdhrRelease(transactionId, { timeout: 60000, ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
      if (!isCurrent()) return
      if (String(context.releaseTransactionId) !== transactionId || String(context.approvedBy) !== actor) {
        throw new Error('只能核验本人签署的该笔上市放行回执。')
      }
      if (context.releaseStatus !== 'RELEASED' || !context.approvalSignoffEvidenceHash) {
        throw new Error('该笔上市放行缺少已生效的签署证据。')
      }
      key = context.approvalIdempotencyKey
      taskId = context.releaseApprovalWorkTaskId
      evidenceHash = context.approvalSignoffEvidenceHash
    }
    if (!key || !taskId) throw new Error('缺少原签署请求或任务信息，无法核验回执。')
    const result = await getApprovalReviewReceipt({ moduleCode: 'EDHR', sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: String(taskId), idempotencyKey: key, prepareId: props.prepareId }, { isRequestContextCurrent: isCurrent })
    if (!isCurrent() || !result) return
    if (result.idempotencyKey !== key || !result.receiptId || !result.signatureId || !result.evidenceHash ||
        (props.prepareId && result.prepareId !== props.prepareId) ||
        (evidenceHash && result.evidenceHash !== evidenceHash) ||
        result.resultCode !== 'APPROVE' || result.resultingStatus !== 'RELEASED') {
      throw new Error('正式回执与原签署请求、签名或批准结果不一致。')
    }
    receipt.value = result
    emit('confirmed', result)
  } catch (cause) {
    if (isCurrent()) error.value = cause instanceof Error ? cause.message : '签署回执读取失败，结果仍待确认。'
  } finally {
    loading.value = false
  }
}
</script>
