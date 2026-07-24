<template>
  <ContentWrap>
    <div class="edhr-batch-history">
      <el-tabs model-value="history" class="edhr-batch-history__tabs" @tab-change="handleTabChange">
        <el-tab-pane label="批次执行" name="execution" />
        <el-tab-pane label="历史批记录" name="history" />
      </el-tabs>

      <el-form :inline="true" :model="queryParams" class="edhr-batch-history__toolbar" @submit.prevent>
        <el-form-item label="批次执行">
          <el-input
            v-model="queryParams.batchExecutionCode"
            clearable
            class="!w-180px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="工单号">
          <el-input v-model="queryParams.workOrderCode" clearable class="!w-170px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="queryParams.batchCode" clearable class="!w-160px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="产品">
          <el-input v-model="queryParams.productCode" clearable class="!w-150px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="路线">
          <el-input v-model="queryParams.routeCode" clearable class="!w-150px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="queryParams.createTime"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            class="!w-300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div class="edhr-batch-history__workbench">
        <aside class="edhr-batch-history__batch-panel">
          <div class="edhr-batch-history__section-head">
            <div>
              <div class="edhr-batch-history__section-title">历史批记录</div>
              <div class="edhr-batch-history__section-subtitle">仅显示已归档批次</div>
            </div>
            <el-tag type="success">已归档</el-tag>
          </div>

          <el-empty v-if="!loading && !batchList.length" description="暂无已归档批记录" />
          <div v-else v-loading="loading" class="edhr-batch-history__batch-list" aria-label="历史批记录列表">
            <button
              v-for="batch in batchList"
              :key="batch.id"
              type="button"
              class="edhr-batch-history__batch-item"
              :class="{ 'is-active': batch.id === selectedBatchId }"
              @click="selectBatch(batch)"
            >
              <span class="edhr-batch-history__batch-code">{{ batch.batchExecutionCode || batch.id }}</span>
              <span class="edhr-batch-history__batch-line">工单 {{ batch.workOrderCode || '--' }}</span>
              <span class="edhr-batch-history__batch-line">批次 {{ batch.batchCode || '--' }}</span>
              <span class="edhr-batch-history__batch-meta">
                <span>{{ batch.routeCode || batch.routeName || '--' }}</span>
                <el-tag size="small" type="success">{{ resolveBatchStatusLabel(batch.status) }}</el-tag>
              </span>
            </button>
          </div>

          <Pagination
            small
            :total="total"
            v-model:page="queryParams.pageNo"
            v-model:limit="queryParams.pageSize"
            @pagination="getBatchList"
          />
        </aside>

        <main class="edhr-batch-history__detail">
          <el-empty v-if="!selectedBatch" description="请选择左侧历史批记录" />
          <template v-else>
            <section class="edhr-batch-history__batch-summary">
              <div>
                <div class="edhr-batch-history__detail-title">
                  {{ selectedBatch.batchExecutionCode || selectedBatch.id }}
                </div>
                <div class="edhr-batch-history__detail-subtitle">
                  {{ selectedBatch.productName || selectedBatch.productCode || '--' }}
                </div>
              </div>
              <el-tag type="success">{{ resolveBatchStatusLabel(selectedBatch.status) }}</el-tag>
            </section>

            <el-descriptions :column="4" border class="edhr-batch-history__summary-table">
              <el-descriptions-item label="工单编码">
                {{ selectedBatch.workOrderCode || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="批次号">
                {{ selectedBatch.batchCode || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="工艺路线">
                {{ selectedBatch.routeCode || selectedBatch.routeName || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="归档时间">
                {{ formatTime(selectedBatch.updateTime) }}
              </el-descriptions-item>
            </el-descriptions>

            <section class="edhr-batch-history__unified-timeline" aria-labelledby="history-unified-timeline-title">
              <div class="edhr-batch-history__unified-head">
                <div>
                  <div id="history-unified-timeline-title" class="edhr-batch-history__section-title">
                    统一时间线
                  </div>
                  <div class="edhr-batch-history__section-subtitle">
                    数据来源：批次事件、任务事件、电子签名、审批记录、归档版本
                  </div>
                </div>
                <el-tag type="primary">{{ unifiedTimelineItems.length }} 条</el-tag>
              </div>

              <div class="edhr-batch-history__source-grid" aria-label="统一时间线数据来源">
                <div
                  v-for="item in timelineSourceSummaryItems"
                  :key="item.label"
                  class="edhr-batch-history__source-item"
                >
                  <span>{{ item.label }}</span>
                  <strong>{{ item.count }}</strong>
                </div>
              </div>

              <el-empty
                v-if="!unifiedTimelineItems.length"
                description="当前批次暂无可聚合的时间线事件"
              />
              <el-timeline v-else class="edhr-batch-history__timeline-list">
                <el-timeline-item
                  v-for="item in unifiedTimelineItems"
                  :key="item.key"
                  :timestamp="formatTime(item.occurredAt)"
                  :type="item.type"
                  placement="top"
                >
                  <article class="edhr-batch-history__timeline-card">
                    <div class="edhr-batch-history__timeline-title">
                      <el-tag size="small" :type="item.type">{{ item.source }}</el-tag>
                      <span>{{ item.title }}</span>
                    </div>
                    <div class="edhr-batch-history__timeline-desc">{{ item.description }}</div>
                    <div v-if="item.proof" class="edhr-batch-history__timeline-proof">
                      {{ item.proof }}
                    </div>
                  </article>
                </el-timeline-item>
              </el-timeline>
            </section>

            <section v-if="dossierItems.length" class="edhr-batch-history__dossier-section">
              <div class="edhr-batch-history__dossier-head">
                <div class="edhr-batch-history__section-title">归档目录</div>
                <el-tag type="success">{{ dossierItems.length }} 项</el-tag>
              </div>
              <div class="edhr-batch-history__dossier-grid">
                <article
                  v-for="item in dossierItems"
                  :key="item.id || `${item.itemType}-${item.itemKey}`"
                  class="edhr-batch-history__dossier-item"
                >
                  <div class="edhr-batch-history__dossier-item-head">
                    <div>
                      <div class="edhr-batch-history__dossier-item-title">
                        {{ resolveDossierItemName(item) }}
                      </div>
                      <div class="edhr-batch-history__dossier-item-subtitle">
                        {{ item.itemType || '--' }} / {{ item.itemKey || '--' }}
                      </div>
                    </div>
                    <el-tag size="small" :type="resolveDossierItemStatusTagType(item.itemStatus)">
                      {{ resolveDossierItemStatusText(item.itemStatus) }}
                    </el-tag>
                  </div>
                  <dl class="edhr-batch-history__dossier-facts">
                    <div>
                      <dt>来源单据</dt>
                      <dd>
                        {{ item.sourceDocType || '--' }}
                        {{ item.sourceDocCode || '--' }}
                      </dd>
                    </div>
                    <div>
                      <dt>检验结果</dt>
                      <dd>{{ item.sourceDocResult || '--' }}</dd>
                    </div>
                    <div>
                      <dt>完成时间</dt>
                      <dd>{{ formatTime(item.completedAt) }}</dd>
                    </div>
                  </dl>
                  <details class="edhr-batch-history__technical-proof">
                    <summary>技术校验</summary>
                    <div class="edhr-batch-history__proof-grid">
                      <div
                        v-for="proof in resolveDossierTechnicalProofs(item)"
                        :key="proof.label"
                        class="edhr-batch-history__proof-item"
                      >
                        <span>{{ proof.label }}</span>
                        <strong>{{ proof.value }}</strong>
                      </div>
                    </div>
                  </details>
                </article>
              </div>
            </section>

            <el-alert
              v-if="timelineError"
              :title="timelineError"
              type="error"
              :closable="false"
              show-icon
              class="edhr-batch-history__timeline-error"
            />

            <section v-loading="timelineLoading" class="edhr-batch-history__record-preview">
              <el-empty
                v-if="!timelineLoading && !executionReviews.length"
                description="当前历史批记录暂无可查看的已填写模板"
              />
              <div v-else class="edhr-batch-history__record-workbench">
                <nav class="edhr-batch-history__process-list" aria-label="历史批记录工序">
                  <button
                    v-for="execution in executionReviews"
                    :key="String(execution.executionId)"
                    type="button"
                    class="edhr-batch-history__process-item"
                    :class="{ 'is-active': String(execution.executionId) === selectedExecutionId }"
                    @click="selectExecution(execution)"
                  >
                    <span class="edhr-batch-history__process-main">
                      <span class="edhr-batch-history__process-sort">
                        {{ execution.routeProcessSort || '--' }}
                      </span>
                      <span class="edhr-batch-history__process-name">
                        {{ execution.processCode || '--' }} {{ execution.processName || '--' }}
                      </span>
                    </span>
                    <span class="edhr-batch-history__process-report">
                      {{ execution.batchRecordReportName || execution.batchRecordReportId || '--' }}
                    </span>
                    <el-tag
                      class="edhr-batch-history__process-status"
                      size="small"
                      :type="resolveExecutionStatusTagType(execution.status)"
                    >
                      {{ resolveExecutionStatusText(execution.status) }}
                    </el-tag>
                  </button>
                </nav>

                <div class="edhr-batch-history__preview">
                  <el-empty
                    v-if="!selectedExecution"
                    description="请选择左侧工序查看模板表格"
                  />
                  <div v-else class="edhr-batch-history__execution-card">
                    <div class="edhr-batch-history__preview-header">
                      <div>
                        <div class="edhr-batch-history__preview-title">
                          {{ selectedExecution.routeProcessSort || '--' }}.
                          {{ selectedExecution.processCode || '--' }}
                          {{ selectedExecution.processName || '--' }}
                        </div>
                        <div class="edhr-batch-history__preview-subtitle">
                          {{ selectedExecution.batchRecordReportName || selectedExecution.batchRecordReportId || '--' }}
                        </div>
                      </div>
                      <el-tag :type="resolveExecutionStatusTagType(selectedExecution.status)">
                        {{ resolveExecutionStatusText(selectedExecution.status) }}
                      </el-tag>
                    </div>

                    <el-descriptions :column="4" border>
                      <el-descriptions-item label="执行编号">
                        {{ selectedExecution.executionCode || '--' }}
                      </el-descriptions-item>
                      <el-descriptions-item label="状态">
                        {{ resolveExecutionStatusText(selectedExecution.status) }}
                      </el-descriptions-item>
                      <el-descriptions-item label="提交时间">
                        {{ formatTime(selectedExecution.submittedAt) }}
                      </el-descriptions-item>
                      <el-descriptions-item label="完成时间">
                        {{ formatTime(selectedExecution.closedAt || selectedExecution.approvedAt) }}
                      </el-descriptions-item>
                    </el-descriptions>

                    <div class="edhr-batch-history__execution-summary">
                      <el-tag
                        v-for="item in resolveExecutionSummaryItems(selectedExecution)"
                        :key="item.label"
                        :type="item.type"
                      >
                        {{ item.label }} {{ item.value }}
                      </el-tag>
                    </div>

                    <EdhrExecutionReadonlyForm
                      :form-view-model="selectedExecution.formViewModel"
                      :signature-records="selectedExecution.signatureRecords"
                    />

                    <section class="edhr-batch-history__attachment-section" aria-labelledby="history-attachment-title">
                      <div class="edhr-batch-history__attachment-head">
                        <div>
                          <div id="history-attachment-title" class="edhr-batch-history__attachment-title">
                            附件证据
                          </div>
                          <div class="edhr-batch-history__attachment-subtitle">
                            来自当前工序执行记录的受控附件账本
                          </div>
                        </div>
                        <el-tag type="info">
                          {{ selectedExecution.attachmentCount || 0 }} 个
                        </el-tag>
                      </div>

                      <el-empty
                        v-if="!selectedExecution.attachmentSummaries?.length"
                        description="暂无附件证据"
                      />
                      <div v-else class="edhr-batch-history__attachment-grid">
                        <article
                          v-for="attachment in selectedExecution.attachmentSummaries"
                          :key="attachment.id || `${attachment.fieldPath}-${attachment.attachmentHash}`"
                          class="edhr-batch-history__attachment-item"
                        >
                          <div class="edhr-batch-history__attachment-item-head">
                            <div class="edhr-batch-history__attachment-name">
                              {{ attachment.fileName || attachment.storagePath || '--' }}
                            </div>
                            <el-tag size="small" type="success">
                              {{ attachment.attachmentAction || '--' }}
                            </el-tag>
                          </div>
                          <div class="edhr-batch-history__attachment-meta">
                            <span>{{ attachment.fieldLabel || attachment.fieldKey || '--' }}</span>
                            <span>{{ attachment.attachmentType || '--' }}</span>
                            <span>{{ formatFileSize(attachment.fileSize) }}</span>
                          </div>
                          <dl class="edhr-batch-history__attachment-facts">
                            <div>
                              <dt>上传人</dt>
                              <dd>{{ attachment.operatorName || attachment.operatorId || '--' }}</dd>
                            </div>
                            <div>
                              <dt>上传时间</dt>
                              <dd>{{ formatTime(attachment.operatedAt) }}</dd>
                            </div>
                          </dl>
                          <details class="edhr-batch-history__technical-proof">
                            <summary>技术校验</summary>
                            <div class="edhr-batch-history__proof-grid">
                              <div
                                v-for="proof in resolveAttachmentTechnicalProofs(attachment)"
                                :key="proof.label"
                                class="edhr-batch-history__proof-item"
                              >
                                <span>{{ proof.label }}</span>
                                <strong>{{ proof.value }}</strong>
                              </div>
                            </div>
                          </details>
                        </article>
                      </div>
                    </section>
                  </div>
                </div>
              </div>
            </section>
          </template>
        </main>
      </div>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import {
  EDHR_BATCH_TASK_STATUS_SKIPPED,
  EDHR_BATCH_STATUS_ARCHIVED,
  getEdhrBatchExecutionPage,
  getEdhrBatchReviewTimeline,
  type EdhrBatchExecutionReviewBatchEvent,
  type EdhrBatchExecutionRespVO,
  type EdhrBatchExecutionDossierItemRespVO,
  type EdhrBatchExecutionReviewFlowEvent,
  type EdhrBatchExecutionReviewTaskEvent,
  type EdhrBatchExecutionReviewExecutionRespVO,
  type EdhrBatchReviewTimelineRespVO
} from '@/api/mes/pro/edhr/batchExecution'
import EdhrExecutionReadonlyForm from '@/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'
import {
  resolveExecutionStatusTagType,
  resolveExecutionStatusText,
  resolveExecutionSummaryItems
} from './executionReviewPresentation'

defineOptions({ name: 'MesProEdhrBatchHistory' })

const router = useRouter()

const loading = ref(false)
const timelineLoading = ref(false)
const loadError = ref('')
const timelineError = ref('')
const batchList = ref<EdhrBatchExecutionRespVO[]>([])
const total = ref(0)
const selectedBatchId = ref<number>()
const selectedExecutionId = ref('')
const timeline = ref<EdhrBatchReviewTimelineRespVO>()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  batchExecutionCode: '',
  workOrderCode: '',
  batchCode: '',
  productCode: '',
  routeCode: '',
  status: EDHR_BATCH_STATUS_ARCHIVED,
  createTime: undefined as string[] | undefined
})

const selectedBatch = computed(() =>
  batchList.value.find((batch) => batch.id === selectedBatchId.value)
)

const executionReviews = computed<EdhrBatchExecutionReviewExecutionRespVO[]>(() =>
  [...(timeline.value?.executionReviews || [])].sort(
    (left, right) => (left.routeProcessSort || 0) - (right.routeProcessSort || 0)
  )
)

const dossierItems = computed<EdhrBatchExecutionDossierItemRespVO[]>(() =>
  [...(timeline.value?.dossierItems || [])].sort((left, right) =>
    String(left.itemKey || '').localeCompare(String(right.itemKey || ''))
  )
)

const selectedExecution = computed(() =>
  executionReviews.value.find((execution) => String(execution.executionId) === selectedExecutionId.value)
)

type TimelineItemType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

interface UnifiedTimelineItem {
  key: string
  source: string
  title: string
  description: string
  occurredAt?: string | number | null
  type: TimelineItemType
  proof?: string
}

const timelineSourceSummaryItems = computed(() => [
  { label: '批次事件', count: timeline.value?.batchEvents?.length || 0 },
  { label: '任务事件', count: timeline.value?.taskEvents?.length || 0 },
  { label: '电子签名', count: timeline.value?.signatureRecords?.length || 0 },
  { label: '审批记录', count: timeline.value?.approvalRecords?.length || 0 },
  { label: '流程干预', count: timeline.value?.flowEvents?.length || 0 },
  { label: '归档版本', count: timeline.value?.archiveVersions?.length || 0 }
])

const createTimelineItem = (
  key: string,
  source: string,
  title: string,
  description: string,
  occurredAt: string | number | null | undefined,
  type: TimelineItemType,
  proof?: string
): UnifiedTimelineItem => ({ key, source, title, description, occurredAt, type, proof })

const parseTaskSpecialPayload = (event: EdhrBatchExecutionReviewTaskEvent) => {
  if (!event.specialPayloadJson?.trim()) return {}
  try {
    return JSON.parse(event.specialPayloadJson) as {
      skipReason?: string
      skipSignatureId?: number
      skippedBy?: number
      skippedAt?: string
      attachments?: Array<{ fileName?: string }>
    }
  } catch (error) {
    return {}
  }
}

const resolveSkippedTaskTimelineDescription = (event: EdhrBatchExecutionReviewTaskEvent) => {
  const payload = parseTaskSpecialPayload(event)
  return [
    `特殊节点已跳过`,
    payload.skipReason ? `原因 ${payload.skipReason}` : '',
    payload.skipSignatureId ? `签名ID ${payload.skipSignatureId}` : '',
    payload.attachments?.length ? `附件 ${payload.attachments.length} 个` : ''
  ]
    .filter(Boolean)
    .join('，')
}

const resolveSkippedTaskTimelineProof = (event: EdhrBatchExecutionReviewTaskEvent) => {
  const payload = parseTaskSpecialPayload(event)
  return [
    payload.skipSignatureId ? `skipSignatureId=${payload.skipSignatureId}` : '',
    payload.skippedBy || event.skippedBy ? `skippedBy=${payload.skippedBy || event.skippedBy}` : '',
    payload.skippedAt || event.skippedAt ? `skippedAt=${payload.skippedAt || event.skippedAt}` : '',
    payload.attachments?.length ? `attachmentCount=${payload.attachments.length}` : ''
  ]
    .filter(Boolean)
    .join(' ; ')
}

const resolveFlowInterventionTimelineTitle = (event: EdhrBatchExecutionReviewFlowEvent) => {
  switch (event.action) {
    case 'RETURN':
      return '流程已退回'
    case 'WITHDRAW':
      return '流程已撤回'
    case 'TRANSFER':
      return '流程责任已转交'
    case 'ADD_SIGN':
      return '流程已加签'
    case 'ADMIN_INTERVENE':
      return '流程已人工干预'
    default:
      return '流程干预已记录'
  }
}

const resolveFlowInterventionTimelineDescription = (event: EdhrBatchExecutionReviewFlowEvent) => {
  return [
    event.reason ? `原因 ${event.reason}` : '',
    event.actorUserId ? `操作人 ${event.actorUserId}` : '',
    event.targetUserId ? `目标人 ${event.targetUserId}` : '',
    event.fromStatus || event.toStatus ? `状态 ${event.fromStatus || '--'} -> ${event.toStatus || '--'}` : ''
  ]
    .filter(Boolean)
    .join('，')
}

const resolveFlowInterventionTimelineProof = (event: EdhrBatchExecutionReviewFlowEvent) => {
  return [
    event.interventionId ? `interventionId=${event.interventionId}` : '',
    event.signoffEvidenceHash ? `签核证据哈希=${event.signoffEvidenceHash}` : '',
    event.integrityCheckResult ? `完整性校验=${event.integrityCheckResult}` : '',
    event.evidenceHash ? `证据哈希=${event.evidenceHash}` : ''
  ]
    .filter(Boolean)
    .join(' ; ')
}

const resolveBatchEventTimelineTitle = (event: EdhrBatchExecutionReviewBatchEvent) => {
  if (event.rejectedAt) return '批次已质量拒收'
  if (event.closedAt) return '批次已关闭'
  return `批次状态 ${resolveBatchStatusLabel(event.status)}`
}

const resolveBatchEventTimelineDescription = (event: EdhrBatchExecutionReviewBatchEvent) => {
  if (event.rejectedAt) {
    return [
      `批次 ${event.batchExecutionCode || event.batchExecutionId || selectedBatchId.value || '--'}`,
      event.rejectReason ? `质量拒收原因 ${event.rejectReason}` : '',
      event.rejectedBy ? `拒收人 ${event.rejectedBy}` : ''
    ]
      .filter(Boolean)
      .join('，')
  }
  if (event.closedAt) {
    return [
      `批次 ${event.batchExecutionCode || event.batchExecutionId || selectedBatchId.value || '--'}`,
      event.closedBy ? `关闭人 ${event.closedBy}` : '',
      `聚合哈希 ${event.aggregateHash || '--'}`
    ]
      .filter(Boolean)
      .join('，')
  }
  return `批次 ${event.batchExecutionCode || event.batchExecutionId || selectedBatchId.value || '--'}，聚合哈希 ${event.aggregateHash || '--'}`
}

const resolveBatchEventTimelineProof = (event: EdhrBatchExecutionReviewBatchEvent) => {
  return [
    event.aggregateHash ? `聚合哈希=${event.aggregateHash}` : '',
    event.closeSignatureId ? `关闭签名ID=${event.closeSignatureId}` : '',
    event.rejectSignatureId ? `质量拒收签名ID=${event.rejectSignatureId}` : ''
  ]
    .filter(Boolean)
    .join(' ; ')
}

const unifiedTimelineItems = computed<UnifiedTimelineItem[]>(() => {
  const current = timeline.value
  if (!current) return []
  const items: UnifiedTimelineItem[] = []

  ;(current.batchEvents || []).forEach((event, index) => {
    items.push(
      createTimelineItem(
        `batch-${index}-${event.batchExecutionId || selectedBatchId.value || ''}`,
        '批次',
        resolveBatchEventTimelineTitle(event),
        resolveBatchEventTimelineDescription(event),
        event.rejectedAt || event.closedAt || event.createTime,
        event.rejectedAt ? 'danger' : event.closedAt ? 'success' : 'primary',
        resolveBatchEventTimelineProof(event)
      )
    )
  })

  ;(current.taskEvents || []).forEach((event, index) => {
    const skipped = event.status === EDHR_BATCH_TASK_STATUS_SKIPPED
    items.push(
      createTimelineItem(
        `task-${index}-${event.taskId || ''}`,
        '任务',
        `${event.processCode || '--'} ${event.processName || '--'}`,
        skipped
          ? resolveSkippedTaskTimelineDescription(event)
          : `任务状态 ${resolveExecutionStatusText(event.status)}，表单 ${event.batchRecordReportName || event.batchRecordReportId || '--'}`,
        event.skippedAt || event.closedAt || event.approvedAt || event.submittedAt || event.openedAt,
        skipped ? 'warning' : event.status === 40 ? 'success' : event.blockerCode ? 'danger' : 'info',
        skipped
          ? resolveSkippedTaskTimelineProof(event)
          : event.blockerCode || event.blockerMessage
          ? `${event.blockerCode || ''} ${event.blockerMessage || ''}`.trim()
          : undefined
      )
    )
  })

  ;(current.signatureRecords || []).forEach((record, index) => {
    items.push(
      createTimelineItem(
        `signature-${index}-${record.id || ''}`,
        '签名',
        `${record.actorName || record.actorId || '--'} ${record.actionType || '--'}`,
        `签名模式 ${record.signatureMode || '--'}，密码校验 ${record.passwordVerified ? '通过' : '未通过/未记录'}`,
        record.signedAt,
        record.passwordVerified ? 'success' : 'warning',
        record.aggregateHash ? `签名哈希=${record.aggregateHash}` : undefined
      )
    )
  })

  ;(current.approvalRecords || []).forEach((record, index) => {
    items.push(
      createTimelineItem(
        `approval-${index}-${record.executionId || ''}-${record.bpmTaskId || ''}`,
        '审批',
        `${record.processCode || '--'} ${record.processName || '--'}`,
        `${record.actorName || '--'} ${record.approvalResult || '--'}，意见：${record.comment || '--'}`,
        record.signedAt,
        record.approvalResult === 'APPROVED' ? 'success' : 'warning',
        record.bpmTaskId ? `bpmTaskId=${record.bpmTaskId}` : undefined
      )
    )
  })

  ;(current.flowEvents || []).forEach((event, index) => {
    items.push(
      createTimelineItem(
        `flow-${index}-${event.id || event.interventionId || ''}`,
        '流程干预',
        resolveFlowInterventionTimelineTitle(event),
        resolveFlowInterventionTimelineDescription(event),
        event.occurredAt,
        event.action === 'TRANSFER' || event.action === 'ADD_SIGN' ? 'warning' : 'danger',
        resolveFlowInterventionTimelineProof(event)
      )
    )
  })

  ;(current.archiveVersions || []).forEach((archive, index) => {
    items.push(
      createTimelineItem(
        `archive-${index}-${archive.id || ''}`,
        '归档',
        `${archive.artifactType || '归档文件'} v${archive.archiveVersion || '--'}`,
        `状态 ${archive.archiveStatus || '--'}，文件 ${archive.fileName || '--'}，大小 ${formatFileSize(archive.fileSize)}`,
        archive.generatedAt,
        archive.archiveStatus === 'SEALED' || archive.canDownloadArchive ? 'success' : 'info',
        archive.contentHash ? `内容哈希=${archive.contentHash}` : archive.failureReason
      )
    )
  })

  return items.sort((left, right) => {
    const leftTime = left.occurredAt ? dayjs(left.occurredAt).valueOf() : 0
    const rightTime = right.occurredAt ? dayjs(right.occurredAt).valueOf() : 0
    return rightTime - leftTime
  })
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const buildQuery = () => ({
  pageNo: queryParams.pageNo,
  pageSize: queryParams.pageSize,
  batchExecutionCode: queryParams.batchExecutionCode.trim() || undefined,
  workOrderCode: queryParams.workOrderCode.trim() || undefined,
  batchCode: queryParams.batchCode.trim() || undefined,
  productCode: queryParams.productCode.trim() || undefined,
  routeCode: queryParams.routeCode.trim() || undefined,
  status: EDHR_BATCH_STATUS_ARCHIVED,
  createTime: queryParams.createTime
})

const applyRouteQuery = () => {
  const routeQuery = router.currentRoute.value.query
  const batchExecutionCode = Array.isArray(routeQuery.batchExecutionCode)
    ? routeQuery.batchExecutionCode[0]
    : routeQuery.batchExecutionCode
  const workOrderCode = Array.isArray(routeQuery.workOrderCode)
    ? routeQuery.workOrderCode[0]
    : routeQuery.workOrderCode
  const batchCode = Array.isArray(routeQuery.batchCode) ? routeQuery.batchCode[0] : routeQuery.batchCode
  if (typeof batchExecutionCode === 'string') queryParams.batchExecutionCode = batchExecutionCode
  if (typeof workOrderCode === 'string') queryParams.workOrderCode = workOrderCode
  if (typeof batchCode === 'string') queryParams.batchCode = batchCode
}

const formatTime = (value?: string | number | null) => {
  if (value == null || value === '') return '--'
  const candidate = dayjs(value)
  return candidate.isValid() ? candidate.format('YYYY-MM-DD HH:mm:ss') : String(value)
}

const formatFileSize = (value?: number | null) => {
  if (value == null || !Number.isFinite(value)) return '--'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

const resolveBatchStatusLabel = (status?: number) => {
  if (status === EDHR_BATCH_STATUS_ARCHIVED) return '已归档'
  return status == null ? '--' : String(status)
}

const resolveDossierItemStatusText = (status?: string | null) => {
  if (status === 'COMPLETED') return '已完成'
  if (status === 'PENDING') return '待完成'
  if (status === 'BLOCKED') return '阻塞'
  return status || '--'
}

const resolveDossierItemStatusTagType = (status?: string | null) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'PENDING') return 'warning'
  return 'danger'
}

const resolveDossierItemName = (item: EdhrBatchExecutionDossierItemRespVO) => {
  if (item.itemType === 'FINAL_INSPECTION') return item.itemName || '成品检'
  return item.itemName || item.itemType || '--'
}

const resolveDossierTechnicalProofs = (item: EdhrBatchExecutionDossierItemRespVO) => [
  {
    label: '来源校验哈希',
    value: item.sourceDocHash || '--'
  }
]

const resolveAttachmentTechnicalProofs = (
  attachment: NonNullable<EdhrBatchExecutionReviewExecutionRespVO['attachmentSummaries']>[number]
) => [
  {
    label: '文件 SHA-256',
    value: attachment.sha256 || '--'
  },
  {
    label: '附件账本哈希',
    value: attachment.attachmentHash || '--'
  }
]

const selectExecution = (execution: EdhrBatchExecutionReviewExecutionRespVO) => {
  selectedExecutionId.value = String(execution.executionId || '')
}

const loadTimeline = async (batchExecutionId: number, preferredExecutionId = selectedExecutionId.value) => {
  timelineLoading.value = true
  timelineError.value = ''
  try {
    timeline.value = await getEdhrBatchReviewTimeline(batchExecutionId)
    const nextExecution = executionReviews.value.find(
      (execution) => String(execution.executionId) === preferredExecutionId
    ) || executionReviews.value[0]
    selectedExecutionId.value = nextExecution ? String(nextExecution.executionId) : ''
  } catch (error) {
    timeline.value = undefined
    selectedExecutionId.value = ''
    timelineError.value = resolveErrorMessage(error, '历史批记录模板内容加载失败。')
  } finally {
    timelineLoading.value = false
  }
}

const selectBatch = async (batch: EdhrBatchExecutionRespVO) => {
  if (selectedBatchId.value === batch.id) return
  selectedBatchId.value = batch.id
  selectedExecutionId.value = ''
  timeline.value = undefined
  await loadTimeline(batch.id, '')
}

const getBatchList = async () => {
  loading.value = true
  loadError.value = ''
  const routeBatchExecutionId = Number(router.currentRoute.value.query.batchExecutionId)
  const previousBatchId = selectedBatchId.value || (Number.isFinite(routeBatchExecutionId) ? routeBatchExecutionId : undefined)
  try {
    const data = await getEdhrBatchExecutionPage(buildQuery())
    batchList.value = data.list || []
    total.value = data.total || 0
    const nextBatch = batchList.value.find((batch) => batch.id === previousBatchId) || batchList.value[0]
    if (nextBatch) {
      selectedBatchId.value = nextBatch.id
      await loadTimeline(nextBatch.id)
    } else {
      selectedBatchId.value = undefined
      selectedExecutionId.value = ''
      timeline.value = undefined
    }
  } catch (error) {
    batchList.value = []
    total.value = 0
    selectedBatchId.value = undefined
    selectedExecutionId.value = ''
    timeline.value = undefined
    loadError.value = resolveErrorMessage(error, '历史批记录列表加载失败。')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getBatchList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.batchExecutionCode = ''
  queryParams.workOrderCode = ''
  queryParams.batchCode = ''
  queryParams.productCode = ''
  queryParams.routeCode = ''
  queryParams.status = EDHR_BATCH_STATUS_ARCHIVED
  queryParams.createTime = undefined
  getBatchList()
}

const handleTabChange = async (name: string | number) => {
  if (name === 'execution') {
    await router.push({ path: '/mes/pro/feedback/edhr-batch-execution' })
  }
}

onMounted(() => {
  applyRouteQuery()
  getBatchList()
})
</script>

<style scoped>
.edhr-batch-history {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-batch-history__tabs {
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
  background: #ffffff;
  padding: 0 16px;
}

.edhr-batch-history__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
  border: 1px solid #dbe3ef;
  border-top: 0;
  background: #ffffff;
  padding: 12px 16px 4px;
}

.edhr-batch-history__workbench {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 14px;
  min-height: 640px;
}

.edhr-batch-history__batch-panel,
.edhr-batch-history__detail {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  min-width: 0;
  padding: 16px;
}

.edhr-batch-history__batch-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-batch-history__section-head,
.edhr-batch-history__batch-summary,
.edhr-batch-history__preview-header,
.edhr-batch-history__batch-meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.edhr-batch-history__section-title,
.edhr-batch-history__detail-title {
  color: #172033;
  font-weight: 700;
}

.edhr-batch-history__section-subtitle,
.edhr-batch-history__detail-subtitle,
.edhr-batch-history__preview-subtitle,
.edhr-batch-history__batch-line,
.edhr-batch-history__process-report {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.4;
}

.edhr-batch-history__batch-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 300px);
  min-height: 420px;
  overflow: auto;
  padding-right: 4px;
}

.edhr-batch-history__batch-item,
.edhr-batch-history__process-item {
  width: 100%;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  color: #172033;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  text-align: left;
  transition: border-color 0.16s ease, background-color 0.16s ease, box-shadow 0.16s ease;
}

.edhr-batch-history__batch-item:hover,
.edhr-batch-history__batch-item:focus-visible,
.edhr-batch-history__process-item:hover,
.edhr-batch-history__process-item:focus-visible {
  border-color: #00a090;
  box-shadow: 0 0 0 2px rgba(0, 160, 144, 0.12);
  outline: none;
}

.edhr-batch-history__batch-item.is-active,
.edhr-batch-history__process-item.is-active {
  border-color: #00a090;
  background: #eefcf9;
  box-shadow: inset 3px 0 0 #00a090;
}

.edhr-batch-history__batch-code {
  color: #172033;
  font-weight: 700;
  word-break: break-all;
}

.edhr-batch-history__detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.edhr-batch-history__summary-table {
  width: 100%;
}

.edhr-batch-history__dossier-section {
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.edhr-batch-history__unified-timeline {
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.edhr-batch-history__unified-head {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.edhr-batch-history__source-grid {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
}

.edhr-batch-history__source-item {
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #fafcff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
}

.edhr-batch-history__source-item span {
  color: #4b5563;
  font-size: 12px;
}

.edhr-batch-history__source-item strong {
  color: #172033;
  font-variant-numeric: tabular-nums;
}

.edhr-batch-history__timeline-list {
  margin-top: 4px;
}

.edhr-batch-history__timeline-card {
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #fafcff;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
}

.edhr-batch-history__timeline-title {
  align-items: center;
  color: #172033;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-weight: 700;
}

.edhr-batch-history__timeline-desc,
.edhr-batch-history__timeline-proof {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.edhr-batch-history__timeline-proof {
  color: #1677ff;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
}

.edhr-batch-history__dossier-head,
.edhr-batch-history__dossier-item-head {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.edhr-batch-history__dossier-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.edhr-batch-history__dossier-item {
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fafcff;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  padding: 12px;
}

.edhr-batch-history__dossier-item-title {
  color: #172033;
  font-weight: 700;
}

.edhr-batch-history__dossier-item-subtitle {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 2px;
}

.edhr-batch-history__dossier-facts {
  display: grid;
  gap: 8px;
  margin: 0;
}

.edhr-batch-history__dossier-facts div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.edhr-batch-history__dossier-facts dt {
  color: #4b5563;
  font-size: 12px;
}

.edhr-batch-history__dossier-facts dd {
  color: #172033;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  line-height: 1.45;
  margin: 0;
  overflow-wrap: anywhere;
}

.edhr-batch-history__timeline-error {
  margin-bottom: 0;
}

.edhr-batch-history__record-preview {
  min-height: 500px;
}

.edhr-batch-history__record-workbench {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 16px;
}

.edhr-batch-history__process-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 360px);
  min-height: 420px;
  overflow: auto;
  padding-right: 4px;
}

.edhr-batch-history__process-main {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.edhr-batch-history__process-sort {
  color: #00a090;
  flex: none;
  font-weight: 700;
  min-width: 24px;
}

.edhr-batch-history__process-name {
  font-weight: 600;
  line-height: 1.4;
}

.edhr-batch-history__process-status {
  align-self: flex-start;
}

.edhr-batch-history__preview {
  min-width: 0;
}

.edhr-batch-history__preview-title {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.edhr-batch-history__execution-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-batch-history__execution-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.edhr-batch-history__attachment-section {
  border-top: 1px solid #edf1f6;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 14px;
}

.edhr-batch-history__attachment-head,
.edhr-batch-history__attachment-item-head {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.edhr-batch-history__attachment-title {
  color: #172033;
  font-weight: 700;
}

.edhr-batch-history__attachment-subtitle,
.edhr-batch-history__attachment-meta {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-batch-history__attachment-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.edhr-batch-history__attachment-item {
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fafcff;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  padding: 12px;
}

.edhr-batch-history__attachment-name {
  color: #172033;
  font-weight: 700;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-history__attachment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.edhr-batch-history__attachment-facts {
  display: grid;
  gap: 8px;
  margin: 0;
}

.edhr-batch-history__attachment-facts div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.edhr-batch-history__attachment-facts dt {
  color: #4b5563;
  font-size: 12px;
}

.edhr-batch-history__attachment-facts dd {
  color: #172033;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  line-height: 1.45;
  margin: 0;
  overflow-wrap: anywhere;
}

.edhr-batch-history__technical-proof {
  border-top: 1px solid #edf1f6;
  padding-top: 10px;
}

.edhr-batch-history__technical-proof summary {
  color: #1677ff;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
}

.edhr-batch-history__technical-proof summary:focus-visible {
  border-radius: 4px;
  outline: 2px solid rgba(22, 119, 255, 0.28);
  outline-offset: 2px;
}

.edhr-batch-history__proof-grid {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.edhr-batch-history__proof-item {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.edhr-batch-history__proof-item span {
  color: #4b5563;
  font-size: 12px;
}

.edhr-batch-history__proof-item strong {
  color: #172033;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

@media (max-width: 1180px) {
  .edhr-batch-history__workbench {
    grid-template-columns: 1fr;
  }

  .edhr-batch-history__batch-list {
    flex-direction: row;
    max-height: none;
    min-height: 0;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .edhr-batch-history__batch-item {
    flex: 0 0 280px;
  }
}

@media (max-width: 960px) {
  .edhr-batch-history__record-workbench {
    grid-template-columns: 1fr;
  }

  .edhr-batch-history__process-list {
    flex-direction: row;
    max-height: none;
    min-height: 0;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .edhr-batch-history__process-item {
    flex: 0 0 240px;
  }
}
</style>
