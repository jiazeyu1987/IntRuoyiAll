<template>
  <section data-testid="dcc-automatic-distribution">
    <slot name="frozen-evidence"></slot>
    <p>下发版本：{{ versionNo }}；{{ effectiveChoice }}</p>
    <p>实际生效时间：{{ formatDateTimeValue(facts.effectiveTime, '尚未记录') }}</p>
    <p v-if="automaticResultLoading">自动下发结果读取中。</p>
    <el-alert v-else-if="automaticResultError || automaticProjectionError" :title="automaticResultError || automaticProjectionError" type="error" :closable="false" show-icon />
    <el-alert v-else-if="automaticResult?.status === 'FAILED'" :title="'自动下发失败：' + (automaticResult.errorMessage || automaticResult.errorCode || '失败原因尚未记录')" type="error" :closable="false" show-icon />
    <p v-else-if="automaticResult?.status === 'NOT_CREATED'">尚未建立自动下发批次。</p>
    <p v-else-if="automaticResult?.status === 'PENDING'">自动下发事件已接收，等待准备。</p>
    <p v-else-if="automaticResult?.status === 'DISPATCH_READY'">下发义务已准备，通知尚未全部送达。</p>
    <p v-else-if="automaticResult?.status === 'SENT'">本批通知已全部送达；电子签收另行记录。</p>
    <p v-if="!automaticProjectionError && automaticResult?.failureAt">实际失败时间：{{ formatDateTimeValue(automaticResult.failureAt, '尚未记录') }}</p>
    <el-button v-if="retryEligible" type="primary" :loading="retryBusy" :disabled="retryBusy || automaticResultLoading" @click="retryDistribution">重试自动下发</el-button>
    <p v-if="retryError" role="alert">{{ retryError }}</p>
    <p v-if="retryOutcome?.status === 'FAILED'" role="alert">重试后仍未完成下发：{{ retryOutcome.errorMessage || retryOutcome.errorCode || '失败原因尚未记录' }}</p>
    <p v-if="!effective">文件受控且生效之后，系统按本次保存的下发部门全员自动通知；当前不创建签收任务。</p>
    <p v-else-if="!facts.distributedTime">文件已受控并生效，自动下发尚未建立完成，请核对下发结果。</p>
    <p v-else>本次下发义务已建立。发送、打开和电子签名收到分别记录。</p>
    <p>通知查阅限定本次下发版本；下载、打印和文件维护仍按各自权限办理。</p>
    <el-alert v-if="receiptError" :title="receiptError" type="error" :closable="false" show-icon />
    <el-alert v-else-if="receiptProjection.error" :title="receiptProjection.error" type="error" :closable="false" show-icon />
    <p v-else-if="receipts === undefined">逐人发送与签收事实尚未读取。</p>
    <template v-else>
      <el-button :loading="receiptsLoading" @click="emit('refresh')">刷新发送与签收记录</el-button>
      <el-table :data="receiptProjection.rows" row-key="recipientId" empty-text="当前没有下发签收义务">
        <el-table-column label="接收部门"><template #default="{ row }">{{ row.departmentName || '部门名称尚未记录' }}</template></el-table-column>
        <el-table-column label="接收人员"><template #default="{ row }">{{ row.currentUserName || '账号名称尚未记录' }}</template></el-table-column>
        <el-table-column label="发送状态"><template #default="{ row }">{{ sendLabels[row.messageStatus] }}</template></el-table-column>
        <el-table-column label="实际发送时间"><template #default="{ row }">{{ formatDateTimeValue(row.sentAt, '未发送') }}</template></el-table-column>
        <el-table-column label="打开时间"><template #default="{ row }">{{ formatDateTimeValue(row.readAt, '未记录打开') }}</template></el-table-column>
        <el-table-column label="签收状态"><template #default="{ row }">{{ row.acknowledgedAt ? '已签' : '未签' }}</template></el-table-column>
        <el-table-column label="实际签名时间"><template #default="{ row }">{{ formatDateTimeValue(row.signedAt, '未签') }}</template></el-table-column>
        <el-table-column label="签名人"><template #default="{ row }">{{ row.signerNameSnapshot || '未签' }}</template></el-table-column>
        <el-table-column label="完成时间"><template #default="{ row }">{{ formatDateTimeValue(row.distributionCompletedAt, '未完成') }}</template></el-table-column>
        <el-table-column prop="errorMessage" label="发送失败原因" />
      </el-table>
    </template>
  </section>
</template>
<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import type { WorkflowId, WorkflowDistributionScope, WorkflowRecipientOption } from './workflow-actions'
import { validateDistributionFacts, validateAutomaticDistributionRetryResult, type DistributionRecipientFact, type DistributionLifecycleFacts, type AutomaticDistributionResult, type AutomaticDistributionRetryCommand } from './distribution-facts'
import { isWorkflowId } from './workflow-actions'
import { formatDateTimeValue } from '@/utils/formatTime'

const props = defineProps<{
  fileId: WorkflowId
  processInstanceId: string
  versionNo: string
  facts: DistributionLifecycleFacts
  receipts?: DistributionRecipientFact[]
  receiptsLoading?: boolean
  receiptError?: string
  automaticResult?: AutomaticDistributionResult | null
  automaticResultLoading?: boolean
  automaticResultError?: string
  canRetryAutomatic?: boolean
  retryAutomatic?: (request: AutomaticDistributionRetryCommand) => Promise<AutomaticDistributionResult>
  retryContextKey?: string
  canDistribute?: boolean
  departments?: Array<{ id: WorkflowId; name: string }>
  loadRecipients?: (fileId: WorkflowId, departmentId: WorkflowId) => Promise<WorkflowRecipientOption[]>
  save?: (fileId: WorkflowId, scopes: WorkflowDistributionScope[]) => Promise<boolean>
}>()
const emit = defineEmits<{ refresh: []; retried: [result: AutomaticDistributionResult]; saved: [context: { fileId: WorkflowId; processInstanceId: string; versionNo: string }] }>()
const retryBusy = ref(false)
const retryError = ref('')
const retryOutcome = ref<AutomaticDistributionResult>()
let retryGeneration = 0
let mounted = true
const retryContext = () => JSON.stringify([props.retryContextKey, String(props.fileId), props.processInstanceId, props.versionNo, String(props.automaticResult?.batchId)])
watch(retryContext, () => { retryGeneration++; retryBusy.value = false; retryError.value = ''; retryOutcome.value = undefined }, { flush: 'sync' })
onBeforeUnmount(() => { mounted = false; retryGeneration++ })
const effective = computed(() => Boolean(props.facts.controlledTime && props.facts.activatedTime)
  && ['ACTIVE', 'OBSOLETE', 'SUPERSEDED'].includes(props.facts.status))
const sendLabels = { PENDING: '待发送', SENT: '已发送', FAILED: '发送失败' }
const automaticProjectionError = computed(() => {
  const result=props.automaticResult
  if (!result) return ''
  return String(result.controlledFileId) !== String(props.fileId) || result.versionNo !== props.versionNo
    || result.approvalProcessInstanceId !== props.processInstanceId ? '自动下发结果不是本次精确版本或轮次' : ''
})
const retryEligible = computed(() => props.canRetryAutomatic === true && typeof props.retryAutomatic === 'function'
  && effective.value && isWorkflowId(props.fileId) && Boolean(props.processInstanceId.trim() && props.versionNo.trim())
  && retryOutcome.value?.status !== 'SENT' && !automaticProjectionError.value && !props.automaticResultError
  && !props.automaticResultLoading && props.automaticResult?.batchId !== null && props.automaticResult?.batchId !== undefined
  && isWorkflowId(props.automaticResult.batchId) && props.automaticResult.status !== 'SENT' && props.automaticResult.status !== 'NOT_CREATED')
const retryDistribution = async () => {
  if (retryBusy.value || !retryEligible.value || !props.retryAutomatic || !props.automaticResult?.batchId) return
  const generation = retryGeneration
  const context = retryContext()
  const request: AutomaticDistributionRetryCommand = {
    controlledFileId: String(props.fileId), batchId: String(props.automaticResult.batchId), versionNo: props.versionNo,
    approvalProcessInstanceId: props.processInstanceId
  }
  retryBusy.value = true; retryError.value = ''; retryOutcome.value = undefined
  const current = () => mounted && generation === retryGeneration && context === retryContext() && props.canRetryAutomatic === true
  try {
    const returned = await props.retryAutomatic(request)
    if (!current()) return
    const result = validateAutomaticDistributionRetryResult(returned, request)
    retryOutcome.value = result
    emit('retried', result)
    emit('refresh')
  } catch (cause) {
    if (current()) retryError.value = cause instanceof Error ? cause.message : '自动下发重试失败，请刷新核对真实批次事实'
  } finally { if (mounted && generation === retryGeneration) retryBusy.value = false }
}
const effectiveChoice = computed(() => {
  if (props.facts.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE') return '文控标准化签名后生效，无预设日期'
  if (props.facts.effectiveMode === 'PRESET_DATE') return props.facts.effectiveDate?.trim()
    ? '预设生效日期：' + props.facts.effectiveDate : '预设生效日期尚未记录'
  return '生效方式未记录'
})
const receiptProjection = computed(() => {
  if (props.receipts === undefined) return { rows: [], error: '' }
  try { return { rows: validateDistributionFacts(props.receipts, props.fileId, props.versionNo), error: '' } }
  catch (cause) { return { rows: [], error: cause instanceof Error ? cause.message : '逐人事实读取异常' } }
})
</script>
