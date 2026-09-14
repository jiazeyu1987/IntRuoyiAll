<template>
  <ContentWrap>
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

    <el-form
      ref="queryFormRef"
      class="process-pool-timeline__query"
      :model="queryParams"
      :inline="true"
      label-width="92px"
    >
      <el-form-item label="提交日期" prop="submitDate">
        <el-date-picker
          v-model="queryParams.submitDate"
          value-format="YYYY-MM-DD"
          type="date"
          placeholder="请选择提交日期"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="员工" prop="employeeUserId">
        <el-input-number
          v-model="queryParams.employeeUserId"
          :min="1"
          :controls="false"
          placeholder="员工编号"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="工序" prop="processId">
        <el-input-number
          v-model="queryParams.processId"
          :min="1"
          :controls="false"
          placeholder="工序编号"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="设备" prop="deviceId">
        <el-input-number
          v-model="queryParams.deviceId"
          :min="1"
          :controls="false"
          placeholder="设备编号"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="模板类型" prop="templateType">
        <el-select
          v-model="queryParams.templateType"
          clearable
          filterable
          placeholder="请选择模板类型"
          class="!w-180px"
        >
          <el-option label="生产简化模板" value="PRODUCTION_SIMPLIFIED" />
          <el-option label="PQC 简化模板" value="PQC_SIMPLIFIED" />
        </el-select>
      </el-form-item>
      <el-form-item label="生产工单" prop="workOrderCode">
        <el-input
          v-model="queryParams.workOrderCode"
          clearable
          placeholder="工单编码"
          class="!w-220px"
        />
      </el-form-item>
      <el-form-item label="工单ID" prop="workOrderId">
        <el-input-number
          v-model="queryParams.workOrderId"
          :min="1"
          :controls="false"
          placeholder="工单编号"
          class="!w-160px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />
          搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" />
          重置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div class="process-pool-timeline">
      <div class="process-pool-timeline__header">
        <div>
          <div class="process-pool-timeline__title">工序池时间轴 / 甘特图</div>
          <div class="process-pool-timeline__subtitle">
            {{ queryParams.submitDate }} · {{ total }} 条提交事件
          </div>
        </div>
        <el-segmented v-model="viewMode" :options="viewModeOptions" />
      </div>

      <div v-if="viewMode === 'timeline'" v-loading="loading" class="process-pool-timeline__list">
        <el-empty v-if="!loading && list.length === 0" description="暂无工序池提交事件" />
        <button
          v-for="event in list"
          :key="event.id"
          type="button"
          class="process-pool-event"
          @click="openDetail(event)"
        >
          <div class="process-pool-event__time">{{ formatDateTime(event.submittedAt) }}</div>
          <div class="process-pool-event__body">
            <div class="process-pool-event__main">
              <strong>{{ event.actualEmployeeUserName || '--' }}</strong>
              <span>{{ event.processName || '--' }}</span>
              <el-tag effect="plain">{{ event.templateTypeName || event.templateType || '--' }}</el-tag>
            </div>
            <div class="process-pool-event__meta">
              <span>生产工单：{{ event.workOrderCode || '--' }}</span>
              <span>设备：{{ event.deviceName || event.deviceCode || '--' }}</span>
              <span>登录账号：{{ event.loginUserName || '--' }}</span>
              <span>电子签名员工：{{ event.signatureEmployeeUserName || '--' }}</span>
            </div>
            <div class="process-pool-event__summary">提交摘要：{{ event.submittedSummary || '--' }}</div>
            <div class="process-pool-event__state">
              <el-tag :type="resolvePqcTagType(event.pqcResult)" effect="plain">
                PQC：{{ event.pqcSummary || event.pqcResult || '--' }}
              </el-tag>
              <el-tag effect="plain">FIFO：{{ event.fifoAllocationStatus || '--' }}</el-tag>
              <el-tag effect="plain">审核副本：{{ event.auditCopyStatus || '--' }}</el-tag>
            </div>
          </div>
        </button>
      </div>

      <div v-else v-loading="loading" class="process-pool-gantt">
        <el-empty v-if="!loading && list.length === 0" description="暂无甘特图事件" />
        <button
          v-for="event in list"
          v-else
          :key="event.id"
          type="button"
          class="process-pool-gantt__row"
          @click="openDetail(event)"
        >
          <div class="process-pool-gantt__label">
            <strong>{{ event.processName || '--' }}</strong>
            <span>{{ event.workOrderCode || '--' }}</span>
          </div>
          <div class="process-pool-gantt__track">
            <div class="process-pool-gantt__bar" :style="resolveGanttBarStyle(event)">
              <span>{{ formatTime(event.submittedAt) }} · {{ event.actualEmployeeUserName || '--' }}</span>
            </div>
          </div>
        </button>
      </div>

      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>
  </ContentWrap>

  <el-drawer
    v-model="detailVisible"
    title="工序池提交事件详情"
    size="620px"
    destroy-on-close
  >
    <div v-loading="detailLoading" class="process-pool-detail">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="服务端提交时间">
          {{ formatDateTime(detail.submittedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="登录账号">
          {{ detail.loginUserName || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="实际填写员工">
          {{ detail.actualEmployeeUserName || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="电子签名员工">
          {{ detail.signatureEmployeeUserName || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="设备">
          {{ detail.deviceName || detail.deviceCode || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="工序">
          {{ detail.processName || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="模板类型">
          {{ detail.templateTypeName || detail.templateType || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="生产工单">
          {{ detail.workOrderCode || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="提交摘要">
          {{ detail.submittedSummary || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="PQC">
          {{ detail.pqcSummary || detail.pqcResult || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="FIFO 分配状态">
          {{ detail.fifoAllocationStatus || '--' }}
          <div class="process-pool-detail__muted">{{ detail.fifoAllocationSummary || '--' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="审核副本状态">
          {{ detail.auditCopyStatus || '--' }}
          <div class="process-pool-detail__muted">{{ detail.auditCopySummary || '--' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="修改历史摘要">
          {{ detail.modificationHistorySummary || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="原始 payload">
          <pre class="process-pool-detail__payload">{{ detail.originalPayloadJson || '--' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="readonlyActions">
          <div class="process-pool-detail__readonly">
            <el-tag effect="plain" type="info">
              原始记录修改：{{ formatReadonlyFlag(detail.readonlyActions?.canModifyOriginalRecord) }}
            </el-tag>
            <el-tag effect="plain" type="info">
              审核副本生成：{{ formatReadonlyFlag(detail.readonlyActions?.canGenerateAuditCopy) }}
            </el-tag>
            <el-tag effect="plain" type="info">
              FIFO 分配执行：{{ formatReadonlyFlag(detail.readonlyActions?.canExecuteFifoAllocation) }}
            </el-tag>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <div
        v-if="detail"
        v-loading="traceLoading"
        class="process-pool-detail__trace"
        data-p0-production-execution-trace
      >
        <div class="process-pool-detail__trace-head">
          <span>生产执行闭环 Trace</span>
          <el-tag v-if="trace" :type="trace.complete === false ? 'danger' : 'success'" effect="dark">
            {{ trace.complete === false ? '未闭环' : '已闭环' }}
          </el-tag>
        </div>
        <el-alert
          v-if="traceLoadError"
          :title="traceLoadError"
          type="error"
          :closable="false"
          show-icon
        />
        <template v-else-if="trace">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="processPoolEventId">
              {{ trace.processPoolEventId }}
            </el-descriptions-item>
            <el-descriptions-item label="提交">
              {{ formatTraceSection(trace.submit) }}
            </el-descriptions-item>
            <el-descriptions-item label="质量">
              {{ formatTraceSection(trace.quality) }}
            </el-descriptions-item>
            <el-descriptions-item label="复核">
              {{ formatTraceSection(trace.review) }}
            </el-descriptions-item>
            <el-descriptions-item label="分配">
              {{ formatTraceSection(trace.allocation) }}
            </el-descriptions-item>
            <el-descriptions-item label="完成">
              {{ formatTraceSection(trace.completion) }}
            </el-descriptions-item>
            <el-descriptions-item label="批记录">
              {{ formatTraceSection(trace.batchRecord) }}
            </el-descriptions-item>
          </el-descriptions>
          <div
            v-if="trace.closureEvidence"
            class="process-pool-detail__closure-evidence"
            data-p0-closure-evidence
          >
            <div class="process-pool-detail__closure-evidence-head">
              <span>闭环证据包：九项审计问题</span>
              <el-tag
                :type="trace.closureEvidence.complete === false ? 'danger' : 'success'"
                effect="plain"
              >
                {{ trace.closureEvidence.complete === false ? '证据不完整' : '证据完整' }}
              </el-tag>
            </div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item
                v-for="answer in closureEvidenceAnswers"
                :key="answer.answerKey"
                :label="closureEvidenceAnswerLabel(answer.answerKey)"
              >
                {{ formatClosureEvidenceAnswer(answer) }}
                <div class="process-pool-detail__muted">
                  来源：{{ formatClosureEvidenceSourceIds(answer) }}
                </div>
                <div class="process-pool-detail__muted">
                  只读复验：{{ formatReadOnlyVerificationEntries(answer.readOnlyVerificationEntries) }}
                </div>
              </el-descriptions-item>
              <el-descriptions-item label="同源校验">
                {{ formatSameSourceChecks(trace.closureEvidence.sameSourceChecks) }}
              </el-descriptions-item>
            </el-descriptions>
            <el-alert
              v-if="trace.closureEvidence.complete === false"
              class="mt-8px"
              :title="formatTraceBlockers(trace.closureEvidence.blockers)"
              type="warning"
              :closable="false"
              show-icon
            />
          </div>
          <el-alert
            v-else-if="trace.complete === true && !trace.closureEvidence"
            class="mt-8px"
            title="trace complete=true 但后端未返回 closureEvidence，不能作为 P0 完整闭环证据。"
            type="error"
            :closable="false"
            show-icon
          />
          <el-alert
            v-if="trace.complete === false"
            class="mt-8px"
            :title="formatTraceBlockers(trace.blockers)"
            type="warning"
            :closable="false"
            show-icon
          />
        </template>
      </div>
      <el-empty v-else-if="!detailLoading" description="请选择工序池提交事件" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  getProductionExecutionTrace,
  getProcessPoolTimelineDetail,
  getProcessPoolTimelinePage,
  type ProductionExecutionEvidenceAnswerVO,
  type ProductionExecutionReadOnlyVerificationEntryVO,
  type ProductionExecutionSameSourceCheckVO,
  type ProductionExecutionTraceBlockerVO,
  type ProductionExecutionTraceSectionVO,
  type ProductionExecutionTraceVO,
  type ProcessPoolTimelineDetailVO,
  type ProcessPoolTimelineEventVO,
  type ProcessPoolTimelinePageReqVO
} from '@/api/mes/pro/processpool'
import { formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'MesProProcessPoolTimeline' })

type ViewMode = 'timeline' | 'gantt'
type ClosureEvidenceAnswerKey =
  | 'who'
  | 'device'
  | 'process'
  | 'quantity'
  | 'quality'
  | 'signature'
  | 'workOrder'
  | 'review'
  | 'batchRecord'

const CLOSURE_EVIDENCE_ANSWER_ORDER: ClosureEvidenceAnswerKey[] = [
  'who',
  'device',
  'process',
  'quantity',
  'quality',
  'signature',
  'workOrder',
  'review',
  'batchRecord'
]

const CLOSURE_EVIDENCE_ANSWER_LABELS: Record<ClosureEvidenceAnswerKey, string> = {
  who: '谁',
  device: '设备',
  process: '工序',
  quantity: '数量',
  quality: '质量',
  signature: '签名',
  workOrder: '生产工单',
  review: '班组长复核',
  batchRecord: '批记录追溯'
}

const queryFormRef = ref()
const loading = ref(false)
const detailLoading = ref(false)
const traceLoading = ref(false)
const detailVisible = ref(false)
const loadError = ref('')
const traceLoadError = ref('')
const total = ref(0)
const list = ref<ProcessPoolTimelineEventVO[]>([])
const detail = ref<ProcessPoolTimelineDetailVO>()
const trace = ref<ProductionExecutionTraceVO>()
const closureEvidenceAnswers = computed<ProductionExecutionEvidenceAnswerVO[]>(() => {
  const answers = trace.value?.closureEvidence?.answers || {}
  return CLOSURE_EVIDENCE_ANSWER_ORDER.map((answerKey) => answers[answerKey]).filter(
    (answer): answer is ProductionExecutionEvidenceAnswerVO => Boolean(answer)
  )
})
const viewMode = ref<ViewMode>('timeline')
const viewModeOptions = [
  { label: '时间轴', value: 'timeline' },
  { label: '甘特图', value: 'gantt' }
]

const queryParams = reactive<ProcessPoolTimelinePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  submitDate: new Date().toISOString().slice(0, 10),
  employeeUserId: undefined,
  processId: undefined,
  deviceId: undefined,
  templateType: undefined,
  workOrderId: undefined,
  workOrderCode: undefined
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const normalizePositiveNumber = (value?: number) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const buildRequestParams = (): ProcessPoolTimelinePageReqVO => {
  if (!queryParams.submitDate) {
    throw new Error('提交日期不能为空')
  }
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    submitDate: queryParams.submitDate,
    employeeUserId: normalizePositiveNumber(queryParams.employeeUserId),
    processId: normalizePositiveNumber(queryParams.processId),
    deviceId: normalizePositiveNumber(queryParams.deviceId),
    templateType: queryParams.templateType || undefined,
    workOrderId: normalizePositiveNumber(queryParams.workOrderId),
    workOrderCode: queryParams.workOrderCode?.trim() || undefined
  }
}

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getProcessPoolTimelinePage(buildRequestParams())
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '工序池时间轴加载失败')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.submitDate = new Date().toISOString().slice(0, 10)
  getList()
}

const openDetail = async (event: ProcessPoolTimelineEventVO) => {
  if (!Number.isFinite(Number(event.id)) || Number(event.id) <= 0) {
    throw new Error('工序池提交事件编号不能为空')
  }
  detailVisible.value = true
  detailLoading.value = true
  traceLoading.value = false
  detail.value = undefined
  trace.value = undefined
  traceLoadError.value = ''
  try {
    detail.value = await getProcessPoolTimelineDetail(Number(event.id))
    await loadProductionExecutionTrace(detail.value.processPoolEventId)
  } catch (error) {
    const message = resolveErrorMessage(error, '工序池提交事件详情加载失败')
    ElMessage.error(message)
  } finally {
    detailLoading.value = false
  }
}

const loadProductionExecutionTrace = async (processPoolEventId?: number) => {
  if (!processPoolEventId) {
    traceLoadError.value = '后端未返回 processPoolEventId，不能打开 P0 生产执行闭环 trace。'
    return
  }
  traceLoading.value = true
  try {
    trace.value = await getProductionExecutionTrace(processPoolEventId)
  } catch (error) {
    trace.value = undefined
    traceLoadError.value = resolveErrorMessage(error, 'P0 生产执行闭环 trace 加载失败')
  } finally {
    traceLoading.value = false
  }
}

const formatTraceBlockers = (blockers?: ProductionExecutionTraceBlockerVO[]) => {
  if (!blockers || blockers.length === 0) {
    return 'trace 未闭环，但后端未返回 blocker；请补齐机器可读阻塞原因。'
  }
  return blockers.map((blocker) => `${blocker.code}: ${blocker.message}`).join('；')
}

const formatTraceSection = (section?: ProductionExecutionTraceSectionVO) => {
  if (!section) return '缺少分组'
  const blockerText = formatTraceBlockers(section.blockers)
  return section.status === 'BLOCKED'
    ? `${section.status} - ${blockerText}`
    : section.status
}

const closureEvidenceAnswerLabel = (answerKey: string) =>
  CLOSURE_EVIDENCE_ANSWER_LABELS[answerKey as ClosureEvidenceAnswerKey] || answerKey

const formatClosureEvidenceValue = (value: unknown) => {
  if (value === null || value === undefined || value === '') return '--'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

const formatClosureEvidenceSourceIds = (answer?: ProductionExecutionEvidenceAnswerVO) => {
  const sourceIds = answer?.sourceIds || {}
  const entries = Object.entries(sourceIds).filter(([, value]) => value !== null && value !== undefined && value !== '')
  if (entries.length === 0) return '缺少正式来源 ID'
  return entries.map(([key, value]) => `${key}=${formatClosureEvidenceValue(value)}`).join('；')
}

const formatClosureEvidenceAnswer = (answer: ProductionExecutionEvidenceAnswerVO) => {
  const sameSourceText = answer.sameSource === false ? '同源失败' : '同源通过'
  const blockerText = answer.blockers?.length ? `；阻塞：${formatTraceBlockers(answer.blockers)}` : ''
  return `${formatClosureEvidenceValue(answer.value)}；${sameSourceText}${blockerText}`
}

const formatReadOnlyVerificationEntries = (entries?: ProductionExecutionReadOnlyVerificationEntryVO[]) => {
  if (!entries || entries.length === 0) return '缺少只读复验入口'
  return entries
    .map((entry) => `${entry.verificationKey || '--'} ${entry.method || 'GET'} ${entry.path || '--'}`)
    .join('；')
}

const formatSameSourceChecks = (checks?: ProductionExecutionSameSourceCheckVO[]) => {
  if (!checks || checks.length === 0) return '缺少同源校验'
  return checks
    .map((check) => `${check.checkKey}: ${check.passed === false ? '失败' : '通过'}`)
    .join('；')
}

const formatDateTime = (value?: string | number | Date) => formatDateTimeValue(value, '--')

const formatTime = (value?: string | number | Date) => {
  const formatted = formatDateTime(value)
  return formatted.length >= 16 ? formatted.slice(11, 16) : formatted
}

const resolvePqcTagType = (pqcResult?: string) => {
  if (pqcResult === 'SUCCESS') return 'success'
  if (pqcResult === 'FAILURE') return 'danger'
  return 'info'
}

const resolveGanttBarStyle = (event: ProcessPoolTimelineEventVO) => {
  const submittedAt = new Date(event.submittedAt || '')
  if (Number.isNaN(submittedAt.getTime())) {
    throw new Error(`工序池提交事件缺少服务端提交时间：eventId=${event.id}`)
  }
  const minutes = submittedAt.getHours() * 60 + submittedAt.getMinutes()
  const left = Math.max(0, Math.min(95, (minutes / 1440) * 100))
  return {
    left: `${left}%`,
    width: '5%'
  }
}

const formatReadonlyFlag = (value?: boolean) => (value ? '允许' : '禁止')

onMounted(() => getList())
</script>

<style scoped>
.process-pool-timeline__query {
  margin-bottom: -15px;
}

.process-pool-timeline {
  min-width: 0;
}

.process-pool-timeline__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.process-pool-timeline__title {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.process-pool-timeline__subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.process-pool-timeline__list {
  display: grid;
  gap: 10px;
  min-height: 220px;
}

.process-pool-event {
  display: grid;
  grid-template-columns: 148px minmax(0, 1fr);
  gap: 14px;
  width: 100%;
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.process-pool-event:hover {
  border-color: #409eff;
  background: #f8fbff;
}

.process-pool-event__time {
  color: #172033;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
}

.process-pool-event__body {
  min-width: 0;
}

.process-pool-event__main,
.process-pool-event__meta,
.process-pool-event__state,
.process-pool-detail__readonly {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.process-pool-event__main {
  color: #172033;
  font-size: 14px;
}

.process-pool-event__meta,
.process-pool-event__summary,
.process-pool-detail__muted {
  margin-top: 8px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.process-pool-event__summary {
  overflow-wrap: anywhere;
}

.process-pool-detail__trace {
  margin-top: 16px;
}

.process-pool-detail__trace-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  font-weight: 600;
}

.process-pool-detail__closure-evidence {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.process-pool-detail__closure-evidence-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #172033;
  font-weight: 600;
}

.process-pool-gantt {
  display: grid;
  gap: 8px;
  min-height: 220px;
}

.process-pool-gantt__row {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 44px;
  padding: 8px 10px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #ffffff;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.process-pool-gantt__label {
  min-width: 0;
}

.process-pool-gantt__label strong,
.process-pool-gantt__label span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-pool-gantt__label span {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.process-pool-gantt__track {
  position: relative;
  height: 28px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: repeating-linear-gradient(
    to right,
    #f8fafc 0,
    #f8fafc 4.166%,
    #edf2f7 4.166%,
    #edf2f7 4.28%
  );
}

.process-pool-gantt__bar {
  position: absolute;
  top: 4px;
  bottom: 4px;
  min-width: 112px;
  max-width: 240px;
  overflow: hidden;
  padding: 0 8px;
  border: 1px solid #0f766e;
  border-radius: 6px;
  background: #14b8a6;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-pool-detail {
  min-height: 220px;
}

.process-pool-detail__payload {
  max-height: 240px;
  overflow: auto;
  margin: 0;
  padding: 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fafc;
  color: #172033;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 768px) {
  .process-pool-timeline__header,
  .process-pool-event,
  .process-pool-gantt__row {
    grid-template-columns: 1fr;
  }

  .process-pool-timeline__header {
    align-items: flex-start;
  }
}
</style>
