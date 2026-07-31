<template>
  <el-drawer
    v-model="drawerVisible"
    title="批次执行追溯"
    size="86%"
    destroy-on-close
    class="edhr-form-trace-batch-trace"
  >
    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      :closable="false"
      show-icon
      class="edhr-form-trace-batch-trace__alert"
    />
    <el-alert
      :title="traceTitle"
      type="info"
      :closable="false"
      show-icon
      class="edhr-form-trace-batch-trace__alert"
    />

    <el-skeleton v-if="loading" :rows="8" animated />
    <el-tabs v-else v-model="activeTab" class="edhr-form-trace-batch-trace__tabs">
      <el-tab-pane label="单元责任" name="fieldResponsibility">
        <section class="edhr-form-trace-batch-trace__section">
          <div class="edhr-form-trace-batch-trace__section-head">
            <div>
              <div class="edhr-form-trace-batch-trace__section-title">单元格填写责任</div>
              <div class="edhr-form-trace-batch-trace__muted">
                复用现有字段责任汇总，展示每个单元格当前值、首次有效填写人、最后操作人和填写时间。
              </div>
            </div>
            <el-select
              v-if="executionEntries.length > 1"
              v-model="selectedResponsibilityExecutionId"
              class="!w-280px"
              placeholder="选择执行记录"
              filterable
            >
              <el-option
                v-for="entry in executionEntries"
                :key="entry.executionId"
                :label="entry.label"
                :value="entry.executionId"
              />
            </el-select>
          </div>
          <FieldAuditPage
            v-if="selectedResponsibilityExecutionId"
            :key="`responsibility-${selectedResponsibilityExecutionId}`"
            embedded
            initial-view="responsibility"
            :initial-execution-id="selectedResponsibilityExecutionId"
          />
          <el-empty v-else description="当前追溯上下文缺少执行记录，无法展示单元格填写责任" />
        </section>
      </el-tab-pane>

      <el-tab-pane label="操作审计" name="operationAudit">
        <OperationAuditListPane
          v-if="traceBatchExecutionId"
          object-type="BATCH_EXECUTION"
          :object-id="String(traceBatchExecutionId)"
          :batch-execution-id="traceBatchExecutionId"
          :show-object-filters="false"
        />
        <OperationAuditListPane
          v-else-if="selectedSignatureExecutionId"
          object-type="BATCH_RECORD_EXECUTION"
          :object-id="String(selectedSignatureExecutionId)"
          :show-object-filters="false"
        />
        <el-empty v-else description="当前追溯上下文缺少批次或执行记录，无法展示按钮操作审计" />
      </el-tab-pane>

      <el-tab-pane label="电子签名" name="signatures">
        <section class="edhr-form-trace-batch-trace__section">
          <div class="edhr-form-trace-batch-trace__section-head">
            <div>
              <div class="edhr-form-trace-batch-trace__section-title">电子签名记录</div>
              <div class="edhr-form-trace-batch-trace__muted">
                展示签名人、签名动作、签名时间和签名审计证据。
              </div>
            </div>
            <el-select
              v-if="executionEntries.length > 1"
              v-model="selectedSignatureExecutionId"
              class="!w-280px"
              placeholder="选择执行记录"
              filterable
            >
              <el-option
                v-for="entry in executionEntries"
                :key="entry.executionId"
                :label="entry.label"
                :value="entry.executionId"
              />
            </el-select>
          </div>
          <SignaturePage
            v-if="selectedSignatureExecutionId"
            :key="`signature-${selectedSignatureExecutionId}`"
            embedded
            :initial-execution-id="selectedSignatureExecutionId"
            :initial-execution-code="selectedSignatureExecutionCode"
          />
          <el-empty v-else description="当前追溯上下文缺少执行记录，无法展示电子签名记录" />
        </section>
      </el-tab-pane>

      <el-tab-pane v-if="traceReleaseTransactionId" label="放行事件" name="releaseEvents">
        <ReleaseEventListPane :release-transaction-id="traceReleaseTransactionId" />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script setup lang="ts">
import FieldAuditPage from '@/views/mes/pro/edhr/FieldAuditPage.vue'
import SignaturePage from '@/views/mes/pro/edhr/SignaturePage.vue'
import OperationAuditListPane from '@/views/mes/pro/edhr/components/OperationAuditListPane.vue'
import ReleaseEventListPane from '@/views/mes/pro/edhr/components/ReleaseEventListPane.vue'
import {
  getEdhrBatchReviewTimeline,
  type EdhrBatchReviewTimelineRespVO
} from '@/api/mes/pro/edhr/batchExecution'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'
import type { BatchExecutionTraceContext } from './traceContext'

defineOptions({ name: 'MesProEdhrFormTraceBatchExecutionTraceDrawer' })

interface TraceExecutionEntry {
  executionId: number
  executionCode: string
  processName: string
  batchRecordReportName: string
  statusLabel: string
  submittedAtText: string
  label: string
}

interface TraceExecutionEntrySource {
  executionId?: string | number
  executionCode?: string
  processName?: string
  batchRecordReportName?: string
  status?: string | number
  submittedAt?: string | number
  signedAt?: string | number
}

const props = defineProps<{
  modelValue: boolean
  context?: BatchExecutionTraceContext
}>()
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
const loading = ref(false)
const loadError = ref('')
const timeline = ref<EdhrBatchReviewTimelineRespVO>()
const activeTab = ref<'fieldResponsibility' | 'operationAudit' | 'signatures' | 'releaseEvents'>(
  'fieldResponsibility'
)
const selectedResponsibilityExecutionId = ref<number>()
const selectedSignatureExecutionId = ref<number>()

const parsePositiveNumber = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  const parsed = Number(rawValue)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const formatTraceTime = (value?: string | number | Date) => formatEdhrDateTime(value)

const traceBatchExecutionId = computed(() => parsePositiveNumber(props.context?.batchExecutionId))
const traceReleaseTransactionId = computed(() =>
  parsePositiveNumber(props.context?.releaseTransactionId)
)
const contextExecutionId = computed(() => parsePositiveNumber(props.context?.executionId))
const traceTitle = computed(() => {
  const segments = [
    props.context?.batchExecutionCode || (traceBatchExecutionId.value ? `批次执行 #${traceBatchExecutionId.value}` : ''),
    props.context?.executionCode || (contextExecutionId.value ? `执行 #${contextExecutionId.value}` : ''),
    props.context?.workOrderCode ? `工单：${props.context.workOrderCode}` : '',
    props.context?.batchCode ? `批次：${props.context.batchCode}` : ''
  ].filter(Boolean)
  return segments.length ? segments.join(' / ') : '当前批次执行追溯'
})

const appendExecutionEntry = (
  entries: Map<number, TraceExecutionEntry>,
  source: TraceExecutionEntrySource
) => {
  const executionId = parsePositiveNumber(source.executionId)
  if (!executionId || entries.has(executionId)) return
  const executionCode = String(source.executionCode || props.context?.executionCode || `#${executionId}`)
  const processName = String(source.processName || '').trim()
  const batchRecordReportName = String(source.batchRecordReportName || '').trim()
  const statusLabel = source.status == null ? '--' : `状态 ${source.status}`
  const submittedAtText = formatTraceTime(source.submittedAt || source.signedAt)
  entries.set(executionId, {
    executionId,
    executionCode,
    processName,
    batchRecordReportName,
    statusLabel,
    submittedAtText,
    label: [processName || batchRecordReportName || executionCode, executionCode]
      .filter(Boolean)
      .join(' / ')
  })
}

const executionEntries = computed(() => {
  const entries = new Map<number, TraceExecutionEntry>()
  appendExecutionEntry(entries, {
    executionId: contextExecutionId.value,
    executionCode: props.context?.executionCode
  })
  for (const execution of timeline.value?.executionReviews || []) {
    appendExecutionEntry(entries, execution)
  }
  for (const task of timeline.value?.taskEvents || []) {
    appendExecutionEntry(entries, task)
  }
  for (const signature of timeline.value?.signatureRecords || []) {
    appendExecutionEntry(entries, signature)
  }
  return [...entries.values()].sort((left, right) => left.executionId - right.executionId)
})

const selectedSignatureExecutionCode = computed(
  () =>
    executionEntries.value.find((entry) => entry.executionId === selectedSignatureExecutionId.value)
      ?.executionCode || ''
)

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const syncSelectedExecutions = () => {
  const entries = executionEntries.value
  const firstExecutionId = entries[0]?.executionId
  if (
    !selectedResponsibilityExecutionId.value ||
    !entries.some((entry) => entry.executionId === selectedResponsibilityExecutionId.value)
  ) {
    selectedResponsibilityExecutionId.value = firstExecutionId
  }
  if (
    !selectedSignatureExecutionId.value ||
    !entries.some((entry) => entry.executionId === selectedSignatureExecutionId.value)
  ) {
    selectedSignatureExecutionId.value = firstExecutionId
  }
}

const loadTimeline = async () => {
  const batchExecutionId = traceBatchExecutionId.value
  timeline.value = undefined
  loadError.value = ''
  if (!batchExecutionId) {
    syncSelectedExecutions()
    return
  }
  loading.value = true
  try {
    timeline.value = await getEdhrBatchReviewTimeline(batchExecutionId)
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '批次执行追溯时间线加载失败，请联系管理员。')
  } finally {
    loading.value = false
    syncSelectedExecutions()
  }
}

watch(
  () => [drawerVisible.value, traceBatchExecutionId.value, contextExecutionId.value],
  async () => {
    if (!drawerVisible.value) return
    activeTab.value = 'fieldResponsibility'
    await loadTimeline()
  },
  { immediate: true }
)

watch(executionEntries, syncSelectedExecutions)
</script>

<style scoped>
.edhr-form-trace-batch-trace__alert {
  margin-bottom: 12px;
}

.edhr-form-trace-batch-trace__tabs {
  min-height: 0;
}

.edhr-form-trace-batch-trace__section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-form-trace-batch-trace__section-head {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f7f9fc;
}

.edhr-form-trace-batch-trace__section-title {
  color: #172033;
  font-weight: 700;
}

.edhr-form-trace-batch-trace__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}
</style>
