<template>
  <ContentWrap>
    <div class="process-pool-team-workbench__header">
      <div>
        <div class="process-pool-team-workbench__title">班组长工作台</div>
        <div class="process-pool-team-workbench__subtitle">
          按提交日期查看一线报工、记录本、PQC、FIFO 和审核副本状态。
        </div>
      </div>
      <el-button :loading="loading" @click="getWorkbench">
        <Icon icon="ep:refresh" class="mr-5px" />
        刷新
      </el-button>
    </div>

    <el-alert
      v-if="loadError"
      class="mt-12px"
      :title="loadError"
      type="error"
      :closable="false"
      show-icon
    />

    <el-form
      ref="queryFormRef"
      class="process-pool-team-workbench__query"
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
      <el-form-item label="生产工单" prop="workOrderCode">
        <el-input
          v-model="queryParams.workOrderCode"
          clearable
          placeholder="工单编码"
          class="!w-220px"
        />
      </el-form-item>
      <el-form-item label="工序" prop="processId">
        <el-input-number
          v-model="queryParams.processId"
          :min="1"
          :controls="false"
          placeholder="工序编号"
          class="!w-160px"
        />
      </el-form-item>
      <el-form-item label="员工" prop="employeeUserId">
        <el-input-number
          v-model="queryParams.employeeUserId"
          :min="1"
          :controls="false"
          placeholder="员工编号"
          class="!w-160px"
        />
      </el-form-item>
      <el-form-item label="设备" prop="deviceId">
        <el-input-number
          v-model="queryParams.deviceId"
          :min="1"
          :controls="false"
          placeholder="设备编号"
          class="!w-160px"
        />
      </el-form-item>
      <el-form-item label="模板类型" prop="templateType">
        <el-select
          v-model="queryParams.templateType"
          clearable
          filterable
          placeholder="模板类型"
          class="!w-180px"
        >
          <el-option label="生产简化模板" value="PRODUCTION_SIMPLIFIED" />
          <el-option label="PQC 简化模板" value="PQC_SIMPLIFIED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />
          搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh-left" class="mr-5px" />
          重置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div class="process-pool-team-workbench__metrics">
      <div
        v-for="metric in metricItems"
        :key="metric.key"
        class="process-pool-team-workbench__metric"
      >
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="events"
      class="process-pool-team-workbench__table"
      row-key="id"
      border
    >
      <el-table-column label="提交时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="生产工单" min-width="150" prop="workOrderCode" show-overflow-tooltip />
      <el-table-column label="工序" min-width="120" prop="processName" show-overflow-tooltip />
      <el-table-column label="员工" min-width="120" prop="actualEmployeeUserName" />
      <el-table-column label="设备" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.deviceName || row.deviceCode || '--' }}</template>
      </el-table-column>
      <el-table-column label="提交摘要" min-width="240" prop="submittedSummary" show-overflow-tooltip />
      <el-table-column label="PQC" min-width="150">
        <template #default="{ row }">
          <el-tag :type="resolvePqcTagType(row.pqcResult)" effect="plain">
            {{ row.pqcSummary || row.pqcResult || '--' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="FIFO" min-width="140">
        <template #default="{ row }">{{ row.fifoAllocationStatus || '--' }}</template>
      </el-table-column>
      <el-table-column label="审核副本" min-width="140">
        <template #default="{ row }">{{ row.auditCopyStatus || '--' }}</template>
      </el-table-column>
      <el-table-column label="原始记录修改" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.modificationHistorySummary || '--' }}</template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getWorkbench"
    />
  </ContentWrap>

  <el-drawer v-model="detailVisible" title="班组长只读详情" size="620px" destroy-on-close>
    <div v-loading="detailLoading" class="process-pool-team-workbench__detail">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="提交时间">
          {{ formatDateTime(detail.submittedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="生产工单">
          {{ detail.workOrderCode || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="工序">
          {{ detail.processName || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="员工">
          {{ detail.actualEmployeeUserName || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="提交摘要">
          {{ detail.submittedSummary || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="PQC">
          {{ detail.pqcSummary || detail.pqcResult || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="FIFO">
          {{ detail.fifoAllocationSummary || detail.fifoAllocationStatus || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="审核副本">
          {{ detail.auditCopySummary || detail.auditCopyStatus || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="原始记录修改">
          {{ detail.modificationHistorySummary || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="原始 payload">
          <pre class="process-pool-team-workbench__payload">{{ detail.originalPayloadJson || '--' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else-if="!detailLoading" description="请选择提交事件" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { formatDateTimeValue } from '@/utils/formatTime'
import type {
  ProcessPoolTimelineDetailVO,
  ProcessPoolTimelineEventVO,
  ProcessPoolTimelinePageReqVO
} from '@/api/mes/pro/processpool'
import {
  getProcessPoolTeamLeaderWorkbenchDetail,
  getProcessPoolTeamLeaderWorkbenchPage,
  type ProcessPoolTeamLeaderWorkbenchSummaryVO
} from '@/api/mes/pro/processpool/teamLeaderWorkbench'

defineOptions({ name: 'MesProProcessPoolTeamLeaderWorkbench' })

const emptySummary = (): ProcessPoolTeamLeaderWorkbenchSummaryVO => ({
  visibleEventCount: 0,
  pqcSuccessCount: 0,
  pqcFailureCount: 0,
  fifoPendingCount: 0,
  fifoAllocatedCount: 0,
  auditCopyPendingCount: 0,
  auditCopySubmittedCount: 0,
  modifiedRecordCount: 0
})

const queryFormRef = ref()
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const loadError = ref('')
const total = ref(0)
const events = ref<ProcessPoolTimelineEventVO[]>([])
const detail = ref<ProcessPoolTimelineDetailVO>()
const summary = ref<ProcessPoolTeamLeaderWorkbenchSummaryVO>(emptySummary())

const queryParams = reactive<ProcessPoolTimelinePageReqVO>({
  pageNo: 1,
  pageSize: 20,
  submitDate: new Date().toISOString().slice(0, 10),
  employeeUserId: undefined,
  processId: undefined,
  deviceId: undefined,
  templateType: undefined,
  workOrderId: undefined,
  workOrderCode: undefined
})

const metricItems = computed(() => [
  { key: 'total', label: '提交总数', value: total.value },
  { key: 'visibleEventCount', label: '当前页提交', value: summary.value.visibleEventCount },
  { key: 'pqcFailureCount', label: 'PQC 失败', value: summary.value.pqcFailureCount },
  { key: 'fifoPendingCount', label: 'FIFO 待分配', value: summary.value.fifoPendingCount },
  { key: 'auditCopyPendingCount', label: '审核副本待生成', value: summary.value.auditCopyPendingCount },
  { key: 'modifiedRecordCount', label: '原始记录修改', value: summary.value.modifiedRecordCount }
])

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

const getWorkbench = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getProcessPoolTeamLeaderWorkbenchPage(buildRequestParams())
    events.value = data.events || []
    total.value = data.total || 0
    summary.value = data.summary || emptySummary()
  } catch (error) {
    events.value = []
    total.value = 0
    summary.value = emptySummary()
    loadError.value = resolveErrorMessage(error, '班组长工作台加载失败')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getWorkbench()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  queryParams.pageSize = 20
  queryParams.submitDate = new Date().toISOString().slice(0, 10)
  getWorkbench()
}

const openDetail = async (event: ProcessPoolTimelineEventVO) => {
  if (!Number.isFinite(Number(event.id)) || Number(event.id) <= 0) {
    throw new Error('工序池提交事件编号不能为空')
  }
  detailVisible.value = true
  detailLoading.value = true
  detail.value = undefined
  try {
    detail.value = await getProcessPoolTeamLeaderWorkbenchDetail(Number(event.id))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '班组长工作台详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

const formatDateTime = (value?: string | number | Date) => formatDateTimeValue(value, '--')

const resolvePqcTagType = (pqcResult?: string) => {
  if (pqcResult === 'SUCCESS' || pqcResult === 'PASS') return 'success'
  if (pqcResult === 'FAILURE' || pqcResult === 'FAIL') return 'danger'
  return 'info'
}

onMounted(() => getWorkbench())
</script>

<style scoped>
.process-pool-team-workbench__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.process-pool-team-workbench__title {
  color: #172033;
  font-size: 18px;
  font-weight: 700;
}

.process-pool-team-workbench__subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.process-pool-team-workbench__query {
  margin-top: 16px;
  margin-bottom: -15px;
}

.process-pool-team-workbench__metrics {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.process-pool-team-workbench__metric {
  min-width: 0;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.process-pool-team-workbench__metric span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.process-pool-team-workbench__metric strong {
  display: block;
  margin-top: 6px;
  color: #172033;
  font-size: 22px;
  line-height: 1;
}

.process-pool-team-workbench__table {
  width: 100%;
}

.process-pool-team-workbench__detail {
  min-height: 220px;
}

.process-pool-team-workbench__payload {
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

@media (max-width: 1200px) {
  .process-pool-team-workbench__metrics {
    grid-template-columns: repeat(3, minmax(120px, 1fr));
  }
}

@media (max-width: 768px) {
  .process-pool-team-workbench__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .process-pool-team-workbench__metrics {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>
