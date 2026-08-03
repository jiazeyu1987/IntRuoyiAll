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
      <el-tab-pane label="批记录表单" name="recordForm">
        <section class="edhr-form-trace-batch-trace__section">
          <div class="edhr-form-trace-batch-trace__section-head">
            <div>
              <div class="edhr-form-trace-batch-trace__section-title">批记录表单详情</div>
              <div class="edhr-form-trace-batch-trace__muted">
                使用归档时固化的执行快照、模板布局和单元格值，以批次执行填写页同款表格只读展示。
              </div>
            </div>
            <el-tag type="success">只读追溯</el-tag>
          </div>

          <el-empty
            v-if="!recordExecutionReviews.length"
            description="当前追溯上下文暂无可查看的批记录表单快照"
          />
          <div v-else class="edhr-form-trace-batch-trace__record-workbench">
            <nav
              class="edhr-form-trace-batch-trace__record-process-list"
              aria-label="表单追溯批记录工序"
            >
              <button
                v-for="execution in recordExecutionReviews"
                :key="String(execution.executionId)"
                type="button"
                class="edhr-form-trace-batch-trace__record-process-item"
                :class="{ 'is-active': String(execution.executionId) === selectedRecordExecutionId }"
                @click="selectRecordExecution(execution)"
              >
                <span class="edhr-form-trace-batch-trace__record-process-main">
                  <span class="edhr-form-trace-batch-trace__record-process-sort">
                    {{ execution.routeProcessSort || '--' }}
                  </span>
                  <span class="edhr-form-trace-batch-trace__record-process-name">
                    {{ execution.processCode || '--' }} {{ execution.processName || '--' }}
                  </span>
                </span>
                <span class="edhr-form-trace-batch-trace__record-process-report">
                  {{ execution.batchRecordReportName || execution.batchRecordReportId || '--' }}
                </span>
                <el-tag
                  class="edhr-form-trace-batch-trace__record-process-status"
                  size="small"
                  :type="resolveExecutionStatusTagType(execution.status)"
                >
                  {{ resolveExecutionStatusText(execution.status) }}
                </el-tag>
              </button>
            </nav>

            <div class="edhr-form-trace-batch-trace__record-preview">
              <el-empty
                v-if="!selectedRecordExecution"
                description="请选择左侧工序查看批记录表单"
              />
              <article v-else class="edhr-form-trace-batch-trace__record-card">
                <div class="edhr-form-trace-batch-trace__record-header">
                  <div>
                    <div class="edhr-form-trace-batch-trace__record-title">
                      {{ selectedRecordExecution.routeProcessSort || '--' }}.
                      {{ selectedRecordExecution.processCode || '--' }}
                      {{ selectedRecordExecution.processName || '--' }}
                    </div>
                    <div class="edhr-form-trace-batch-trace__muted">
                      {{ selectedRecordExecution.batchRecordReportName || selectedRecordExecution.batchRecordReportId || '--' }}
                    </div>
                  </div>
                  <el-tag :type="resolveExecutionStatusTagType(selectedRecordExecution.status)">
                    {{ resolveExecutionStatusText(selectedRecordExecution.status) }}
                  </el-tag>
                </div>

                <div class="edhr-form-trace-batch-trace__snapshot-source">
                  <el-tag
                    v-for="item in selectedRecordSnapshotEvidenceItems"
                    :key="item.label"
                    :type="item.available ? 'success' : 'danger'"
                    effect="plain"
                  >
                    {{ item.label }}{{ item.available ? '已固化' : '缺失' }}
                  </el-tag>
                </div>

                <EdhrExecutionReadonlyForm
                  embedded
                  :form-view-model="selectedRecordExecution.formViewModel"
                  :signature-records="selectedRecordExecution.signatureRecords"
                />

                <section
                  class="edhr-form-trace-batch-trace__attachment-section"
                  aria-labelledby="form-trace-record-attachment-title"
                >
                  <div class="edhr-form-trace-batch-trace__attachment-head">
                    <div>
                      <div
                        id="form-trace-record-attachment-title"
                        class="edhr-form-trace-batch-trace__attachment-title"
                      >
                        附件证据
                      </div>
                      <div class="edhr-form-trace-batch-trace__muted">
                        来自当前工序执行记录的受控附件账本。
                      </div>
                    </div>
                    <el-tag type="info">
                      {{ selectedRecordExecution.attachmentCount || 0 }} 个
                    </el-tag>
                  </div>

                  <el-empty
                    v-if="!selectedRecordExecution.attachmentSummaries?.length"
                    description="暂无附件证据"
                  />
                  <div v-else class="edhr-form-trace-batch-trace__attachment-grid">
                    <article
                      v-for="attachment in selectedRecordExecution.attachmentSummaries"
                      :key="attachment.id || `${attachment.fieldPath}-${attachment.attachmentHash}`"
                      class="edhr-form-trace-batch-trace__attachment-item"
                    >
                      <div class="edhr-form-trace-batch-trace__attachment-item-head">
                        <div class="edhr-form-trace-batch-trace__attachment-name">
                          {{ attachment.fileName || attachment.storagePath || '--' }}
                        </div>
                        <el-tag size="small" type="success">
                          {{ attachment.attachmentAction || '--' }}
                        </el-tag>
                      </div>
                      <div class="edhr-form-trace-batch-trace__attachment-meta">
                        <span>{{ attachment.fieldLabel || attachment.fieldKey || '--' }}</span>
                        <span>{{ attachment.attachmentType || '--' }}</span>
                        <span>{{ formatFileSize(attachment.fileSize) }}</span>
                      </div>
                      <dl class="edhr-form-trace-batch-trace__attachment-facts">
                        <div>
                          <dt>上传人</dt>
                          <dd>{{ attachment.operatorName || attachment.operatorId || '--' }}</dd>
                        </div>
                        <div>
                          <dt>上传时间</dt>
                          <dd>{{ formatTraceTime(attachment.operatedAt) }}</dd>
                        </div>
                      </dl>
                      <details class="edhr-form-trace-batch-trace__technical-proof">
                        <summary>技术校验</summary>
                        <div class="edhr-form-trace-batch-trace__proof-grid">
                          <div
                            v-for="proof in resolveAttachmentTechnicalProofs(attachment)"
                            :key="proof.label"
                            class="edhr-form-trace-batch-trace__proof-item"
                          >
                            <span>{{ proof.label }}</span>
                            <strong>{{ proof.value }}</strong>
                          </div>
                        </div>
                      </details>
                    </article>
                  </div>
                </section>
              </article>
            </div>
          </div>
        </section>
      </el-tab-pane>

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
import EdhrExecutionReadonlyForm from '@/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'
import {
  getEdhrBatchReviewTimeline,
  type EdhrBatchExecutionReviewAttachmentSummary,
  type EdhrBatchExecutionReviewExecutionRespVO,
  type EdhrBatchReviewTimelineRespVO
} from '@/api/mes/pro/edhr/batchExecution'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'
import {
  resolveExecutionStatusTagType,
  resolveExecutionStatusText
} from '@/views/mes/pro/edhr-batch/executionReviewPresentation'
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
const activeTab = ref<'recordForm' | 'fieldResponsibility' | 'operationAudit' | 'signatures' | 'releaseEvents'>(
  'recordForm'
)
const selectedRecordExecutionId = ref('')
const selectedResponsibilityExecutionId = ref<number>()
const selectedSignatureExecutionId = ref<number>()

const parsePositiveNumber = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  const parsed = Number(rawValue)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const formatTraceTime = (value?: string | number | Date) => formatEdhrDateTime(value)

const formatFileSize = (value?: number | null) => {
  if (value == null || !Number.isFinite(value)) return '--'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

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

const recordExecutionReviews = computed<EdhrBatchExecutionReviewExecutionRespVO[]>(() =>
  [...(timeline.value?.executionReviews || [])].sort(
    (left, right) => (left.routeProcessSort || 0) - (right.routeProcessSort || 0)
  )
)

const selectedRecordExecution = computed(() =>
  recordExecutionReviews.value.find(
    (execution) => String(execution.executionId || '') === selectedRecordExecutionId.value
  )
)

const selectedRecordSnapshotEvidenceItems = computed(() => {
  const formViewModel = selectedRecordExecution.value?.formViewModel
  return [
    { label: '执行快照', available: Boolean(formViewModel?.executionSnapshotJson) },
    { label: '模板布局', available: Boolean(formViewModel?.sheetLayoutJson) },
    { label: '单元格值', available: Boolean(formViewModel?.cellValuesJson) }
  ]
})

const selectedSignatureExecutionCode = computed(
  () =>
    executionEntries.value.find((entry) => entry.executionId === selectedSignatureExecutionId.value)
      ?.executionCode || ''
)

const selectRecordExecution = (execution: EdhrBatchExecutionReviewExecutionRespVO) => {
  selectedRecordExecutionId.value = String(execution.executionId || '')
}

const resolveAttachmentTechnicalProofs = (attachment: EdhrBatchExecutionReviewAttachmentSummary) => [
  {
    label: '文件 SHA-256',
    value: attachment.sha256 || '--'
  },
  {
    label: '附件账本哈希',
    value: attachment.attachmentHash || '--'
  }
]

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const syncSelectedExecutions = () => {
  const recordEntries = recordExecutionReviews.value
  const preferredRecordExecution =
    recordEntries.find((execution) => execution.executionId === contextExecutionId.value) ||
    recordEntries[0]
  if (
    !selectedRecordExecutionId.value ||
    !recordEntries.some((execution) => String(execution.executionId || '') === selectedRecordExecutionId.value)
  ) {
    selectedRecordExecutionId.value = preferredRecordExecution
      ? String(preferredRecordExecution.executionId || '')
      : ''
  }

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
    selectedRecordExecutionId.value = ''
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
    activeTab.value = 'recordForm'
    await loadTimeline()
  },
  { immediate: true }
)

watch([executionEntries, recordExecutionReviews], syncSelectedExecutions)
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

.edhr-form-trace-batch-trace__record-workbench {
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: 14px;
  min-height: 0;
}

.edhr-form-trace-batch-trace__record-process-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 260px);
  overflow: auto;
  padding-right: 4px;
}

.edhr-form-trace-batch-trace__record-process-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: stretch;
  width: 100%;
  padding: 10px 12px;
  color: #172033;
  text-align: left;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.edhr-form-trace-batch-trace__record-process-item:hover,
.edhr-form-trace-batch-trace__record-process-item:focus-visible {
  border-color: #1677ff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.edhr-form-trace-batch-trace__record-process-item.is-active {
  border-color: #1677ff;
  background: #eef6ff;
}

.edhr-form-trace-batch-trace__record-process-main {
  display: flex;
  gap: 8px;
  align-items: center;
}

.edhr-form-trace-batch-trace__record-process-sort {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: #ffffff;
  font-weight: 700;
  background: #1677ff;
  border-radius: 50%;
  flex: 0 0 auto;
}

.edhr-form-trace-batch-trace__record-process-name {
  font-weight: 700;
  line-height: 1.4;
}

.edhr-form-trace-batch-trace__record-process-report {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.4;
}

.edhr-form-trace-batch-trace__record-process-status {
  align-self: flex-start;
}

.edhr-form-trace-batch-trace__record-preview,
.edhr-form-trace-batch-trace__record-card {
  min-width: 0;
}

.edhr-form-trace-batch-trace__record-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
}

.edhr-form-trace-batch-trace__record-header,
.edhr-form-trace-batch-trace__attachment-head,
.edhr-form-trace-batch-trace__attachment-item-head {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.edhr-form-trace-batch-trace__record-title,
.edhr-form-trace-batch-trace__attachment-title {
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.edhr-form-trace-batch-trace__snapshot-source {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.edhr-form-trace-batch-trace__attachment-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #dbe3ef;
}

.edhr-form-trace-batch-trace__attachment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
}

.edhr-form-trace-batch-trace__attachment-item {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.edhr-form-trace-batch-trace__attachment-name {
  color: #172033;
  font-weight: 700;
  word-break: break-all;
}

.edhr-form-trace-batch-trace__attachment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-form-trace-batch-trace__attachment-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 10px 0 0;
}

.edhr-form-trace-batch-trace__attachment-facts div {
  padding: 8px;
  background: #ffffff;
  border-radius: 6px;
}

.edhr-form-trace-batch-trace__attachment-facts dt {
  color: #64748b;
  font-size: 12px;
}

.edhr-form-trace-batch-trace__attachment-facts dd {
  margin: 4px 0 0;
  color: #172033;
  font-weight: 600;
  word-break: break-all;
}

.edhr-form-trace-batch-trace__technical-proof {
  margin-top: 10px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-form-trace-batch-trace__technical-proof summary {
  cursor: pointer;
  font-weight: 700;
}

.edhr-form-trace-batch-trace__proof-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 8px;
  margin-top: 8px;
}

.edhr-form-trace-batch-trace__proof-item {
  padding: 8px;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
}

.edhr-form-trace-batch-trace__proof-item span,
.edhr-form-trace-batch-trace__proof-item strong {
  display: block;
}

.edhr-form-trace-batch-trace__proof-item strong {
  margin-top: 4px;
  color: #172033;
  word-break: break-all;
}

@media (max-width: 960px) {
  .edhr-form-trace-batch-trace__record-workbench {
    grid-template-columns: 1fr;
  }

  .edhr-form-trace-batch-trace__record-process-list {
    max-height: none;
  }
}
</style>
