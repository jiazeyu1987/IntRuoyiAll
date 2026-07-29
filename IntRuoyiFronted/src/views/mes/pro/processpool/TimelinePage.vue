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
      <el-empty v-else-if="!detailLoading" description="请选择工序池提交事件" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  getProcessPoolTimelineDetail,
  getProcessPoolTimelinePage,
  type ProcessPoolTimelineDetailVO,
  type ProcessPoolTimelineEventVO,
  type ProcessPoolTimelinePageReqVO
} from '@/api/mes/pro/processpool'
import { formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'MesProProcessPoolTimeline' })

type ViewMode = 'timeline' | 'gantt'

const queryFormRef = ref()
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const loadError = ref('')
const total = ref(0)
const list = ref<ProcessPoolTimelineEventVO[]>([])
const detail = ref<ProcessPoolTimelineDetailVO>()
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
  detail.value = undefined
  try {
    detail.value = await getProcessPoolTimelineDetail(Number(event.id))
  } catch (error) {
    const message = resolveErrorMessage(error, '工序池提交事件详情加载失败')
    ElMessage.error(message)
  } finally {
    detailLoading.value = false
  }
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
