<template>
  <div class="edhr-form-trace-release">
    <UnifiedListTemplate
      :table-key="releaseTraceTableKey"
      :query-model="queryParams"
      :filter-definitions="releaseQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="releaseQuickFilter.state"
      :selected-filter-definition="releaseQuickFilter.selectedDefinition.value"
      :operator-options="releaseQuickFilter.operatorOptions.value"
      :columns="releaseColumns"
      :column-saving="releaseColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="releaseQuickFilter.updateState"
      @quick-filter-query="releaseQuickFilter.applyQuickFilter"
      @column-change="saveReleaseColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button :loading="loading" type="primary" @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />
          查询
        </el-button>
        <el-button @click="resetQuery">重置</el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-alert
          v-if="loadError"
          :title="loadError"
          type="error"
          :closable="false"
          show-icon
          class="edhr-form-trace-release__alert"
        />
        <el-alert
          v-if="actionError"
          :title="actionError"
          type="error"
          :closable="false"
          show-icon
          class="edhr-form-trace-release__alert"
        />
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          :data-user-table-key="releaseTraceTableKey"
          :data="list"
          border
          stripe
          row-key="batchExecutionId"
          :show-overflow-tooltip="true"
          :empty-text="emptyText"
          @header-dragend="handleReleaseHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isReleaseColumnVisible('releaseObject')"
            :label="objectColumnLabel"
            prop="releaseObject"
            :min-width="getReleaseColumnMinWidthString('releaseObject', 250)"
            v-bind="sortColumnAttrs('releaseObject')"
          >
            <template #default="{ row }">
              <div class="edhr-form-trace-release__strong">{{ row.batchExecutionCode || '--' }}</div>
              <div class="edhr-form-trace-release__muted">工单：{{ row.workOrderCode || '--' }}</div>
              <div class="edhr-form-trace-release__muted">批次：{{ row.batchCode || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReleaseColumnVisible('productRoute')"
            label="产品/路线"
            prop="productRoute"
            :min-width="getReleaseColumnMinWidthString('productRoute', 230)"
            v-bind="sortColumnAttrs('productRoute')"
          >
            <template #default="{ row }">
              <div class="edhr-form-trace-release__strong">{{ row.productName || '--' }}</div>
              <div class="edhr-form-trace-release__muted">产品：{{ row.productCode || '--' }}</div>
              <div class="edhr-form-trace-release__muted">路线：{{ row.routeName || row.routeCode || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReleaseColumnVisible('releaseStatus')"
            :label="statusColumnLabel"
            prop="releaseStatus"
            :width="getReleaseColumnWidthString('releaseStatus', 145)"
            v-bind="sortColumnAttrs('releaseStatus')"
          >
            <template #default="{ row }">
              <el-tag :type="resolveReleaseTagType(row.releaseStatus)">
                {{ resolveReleaseStatusLabel(row.releaseStatus) }}
              </el-tag>
              <div class="edhr-form-trace-release__muted">
                批次状态：{{ resolveBatchExecutionTraceStatusLabel(row.batchExecutionStatus) }}
              </div>
              <div class="edhr-form-trace-release__muted">{{ row.precheckSummary || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReleaseColumnVisible('checkSummary')"
            label="检查摘要"
            prop="checkSummary"
            :width="getReleaseColumnWidthString('checkSummary', 170)"
            v-bind="sortColumnAttrs('checkSummary')"
          >
            <template #default="{ row }">
              <div class="edhr-form-trace-release__metric">
                阻塞 <strong>{{ row.blockingCheckCount || 0 }}</strong>
              </div>
              <div class="edhr-form-trace-release__metric">
                失败 <strong>{{ row.failedCheckCount || 0 }}</strong>
              </div>
              <div class="edhr-form-trace-release__muted">最后：{{ formatDateTime(row.lastPrecheckAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReleaseColumnVisible('qualityGate')"
            label="质量门禁"
            prop="qualityGate"
            :min-width="getReleaseColumnMinWidthString('qualityGate', 260)"
            v-bind="sortColumnAttrs('qualityGate')"
          >
            <template #default="{ row }">
              <div class="edhr-form-trace-release__gate-row">
                <el-tag :type="resolveReleaseCheckResultTagType(row.dhrStatus)">DHR {{ resolveReleaseCheckResultLabel(row.dhrStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.inspectionStatus)">检验 {{ resolveReleaseCheckResultLabel(row.inspectionStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.inventoryStatus)">库存 {{ resolveReleaseCheckResultLabel(row.inventoryStatus) }}</el-tag>
              </div>
              <div class="edhr-form-trace-release__gate-row">
                <el-tag :type="resolveReleaseCheckResultTagType(row.deviationStatus)">偏差 {{ resolveReleaseCheckResultLabel(row.deviationStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.reworkStatus)">返工 {{ resolveReleaseCheckResultLabel(row.reworkStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.scrapStatus)">报废 {{ resolveReleaseCheckResultLabel(row.scrapStatus) }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReleaseColumnVisible('transactionTime')"
            label="事务时间"
            prop="transactionTime"
            :min-width="getReleaseColumnMinWidthString('transactionTime', 180)"
            v-bind="sortColumnAttrs('transactionTime')"
          >
            <template #default="{ row }">
              <div class="edhr-form-trace-release__muted">提交：{{ formatDateTime(row.submittedAt) }}</div>
              <div class="edhr-form-trace-release__muted">批准：{{ formatDateTime(row.approvedAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReleaseColumnVisible('traceActions')"
            label="追溯"
            prop="traceActions"
            :width="getReleaseColumnWidthString('traceActions', 220)"
            fixed="right"
            v-bind="sortColumnAttrs('traceActions')"
          >
            <template #default="{ row }">
              <div class="edhr-form-trace-release__actions">
                <el-button link type="primary" @click="openBatchTrace(row)">
                  追溯
                </el-button>
                <el-button link type="primary" :disabled="!row.releaseTransactionId" @click="openCheckItems(row)">
                  检查项
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-release:event-query']"
                  link
                  type="primary"
                  :disabled="!row.releaseTransactionId"
                  @click="openEventDrawer(row)"
                >
                  事务事件
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-batch-execution-archive:download']"
                  link
                  type="primary"
                  :disabled="row.releaseStatus !== 'RELEASED' || printLoadingBatchExecutionId === row.batchExecutionId"
                  @click="handlePrintArchive(row)"
                >
                  {{ printLoadingBatchExecutionId === row.batchExecutionId ? '打印中...' : '打印' }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>

    <el-drawer v-model="drawerVisible" title="电子批记录放行检查项" size="82%" class="edhr-form-trace-release__drawer">
      <el-alert
        v-if="currentRow"
        :title="`${currentRow.batchExecutionCode} / ${currentRow.precheckSummary || '检查项'}`"
        type="info"
        :closable="false"
        show-icon
        class="edhr-form-trace-release__drawer-alert"
      />
      <el-table
        v-loading="checkItemLoading"
        :data="checkItems"
        stripe
        :show-overflow-tooltip="true"
        empty-text="暂无检查项"
      >
        <el-table-column label="检查项" min-width="210">
          <template #default="{ row }">
            <div class="edhr-form-trace-release__strong">{{ resolveReleaseCheckCodeLabel(row.checkCode) }}</div>
            <div class="edhr-form-trace-release__muted">
              分类：{{ resolveReleaseCheckCategoryLabel(row.checkCategory) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="115">
          <template #default="{ row }">
            <el-tag :type="resolveReleaseCheckResultTagType(row.checkResult)">
              {{ resolveReleaseCheckResultLabel(row.checkResult) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="责任模块" width="110" prop="responsibilityModule" />
        <el-table-column label="源对象" min-width="190">
          <template #default="{ row }">
            <div>{{ row.sourceObjectCode || '--' }}</div>
            <div class="edhr-form-trace-release__muted">{{ resolveReleaseCheckSourceObjectTypeLabel(row.sourceObjectType) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="260" prop="failureReason" />
        <el-table-column label="下一步动作" min-width="260" prop="remediationSuggestion" />
      </el-table>
      <Pagination
        :total="checkItemTotal"
        v-model:page="checkItemQuery.pageNo"
        v-model:limit="checkItemQuery.pageSize"
        @pagination="getCheckItems"
      />
    </el-drawer>

    <el-drawer v-model="eventDrawerVisible" title="电子批记录放行事务事件" size="82%" class="edhr-form-trace-release__drawer">
      <el-table
        v-loading="eventLoading"
        :data="eventList"
        stripe
        :show-overflow-tooltip="true"
        empty-text="暂无事务事件"
      >
        <el-table-column label="事件" width="120">
          <template #default="{ row }">
            <el-tag>{{ resolveReleaseEventLabel(row.eventType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态变化" width="190">
          <template #default="{ row }">
            {{ resolveReleaseStatusLabel(row.fromStatus) }} → {{ resolveReleaseStatusLabel(row.toStatus) }}
          </template>
        </el-table-column>
        <el-table-column label="幂等键" min-width="230" prop="idempotencyKey" />
        <el-table-column label="签核证据" min-width="230" prop="signoffEvidenceHash" />
        <el-table-column label="原因/意见" min-width="260">
          <template #default="{ row }">
            <div>{{ row.reason || row.opinion || '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="110" prop="actorUserId" />
        <el-table-column label="发生时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.occurredAt) }}</template>
        </el-table-column>
      </el-table>
      <Pagination
        :total="eventTotal"
        v-model:page="eventQuery.pageNo"
        v-model:limit="eventQuery.pageSize"
        @pagination="loadEventList"
      />
    </el-drawer>
    <BatchExecutionTraceDrawer
      v-model="traceDrawerVisible"
      :context="traceContext"
    />
  </div>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  getEdhrReleaseCheckItemPage,
  getEdhrReleaseEventPage,
  getEdhrReleasePage,
  type EdhrReleaseCheckItemPageReqVO,
  type EdhrReleaseCheckItemVO,
  type EdhrReleaseEventPageReqVO,
  type EdhrReleaseEventRespVO,
  type EdhrReleasePageReqVO,
  type EdhrReleaseRowVO
} from '@/api/mes/pro/edhr/release'
import {
  EDHR_BATCH_STATUS_ARCHIVED,
  EDHR_BATCH_STATUS_CLOSED,
  EDHR_BATCH_STATUS_CREATED,
  EDHR_BATCH_STATUS_IN_PROGRESS,
  EDHR_BATCH_STATUS_READY_TO_CLOSE,
  EDHR_BATCH_STATUS_REJECTED,
  EDHR_BATCH_STATUS_REWORK_REQUIRED,
  EDHR_BATCH_STATUS_VOIDED,
  getLatestEdhrBatchArchive,
  printEdhrBatchArchive
} from '@/api/mes/pro/edhr/batchExecution'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import BatchExecutionTraceDrawer from './BatchExecutionTraceDrawer.vue'
import type { BatchExecutionTraceContext } from './traceContext'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'
import {
  resolveReleaseCheckCategoryLabel,
  resolveReleaseCheckCodeLabel,
  resolveReleaseCheckResultLabel,
  resolveReleaseCheckResultTagType,
  resolveReleaseCheckSourceObjectTypeLabel,
  resolveReleaseEventLabel,
  resolveReleaseStatusLabel,
  resolveReleaseTagType
} from '@/views/mes/pro/edhr/shared/releaseCheckPresentation'

defineOptions({ name: 'MesProEdhrFormTraceReleaseTab' })

const props = withDefaults(defineProps<{ traceMode?: 'release' | 'reject' }>(), {
  traceMode: 'release'
})

const route = useRoute()
const message = useMessage()
const isRejectTrace = props.traceMode === 'reject'
const releaseTraceTableKey = isRejectTrace
  ? 'mes.pro.edhr.formTrace.reject'
  : 'mes.pro.edhr.formTrace.release'
const objectColumnLabel = isRejectTrace ? '驳回对象' : '放行对象'
const statusColumnLabel = isRejectTrace ? '驳回状态' : '放行状态'
const emptyText = isRejectTrace ? '暂无驳回追溯记录' : '暂无放行追溯记录'
const EDHR_REJECT_TRACE_BATCH_STATUSES = [EDHR_BATCH_STATUS_REJECTED] as const
const EDHR_RELEASE_TRACE_EXCLUDED_BATCH_STATUSES = [EDHR_BATCH_STATUS_REJECTED] as const

const loading = ref(false)
const checkItemLoading = ref(false)
const eventLoading = ref(false)
const printLoadingBatchExecutionId = ref<string>()
const loadError = ref('')
const actionError = ref('')
const list = ref<EdhrReleaseRowVO[]>([])
const total = ref(0)
const drawerVisible = ref(false)
const eventDrawerVisible = ref(false)
const traceDrawerVisible = ref(false)
const currentRow = ref<EdhrReleaseRowVO>()
const traceContext = ref<BatchExecutionTraceContext>()
const checkItems = ref<EdhrReleaseCheckItemVO[]>([])
const eventList = ref<EdhrReleaseEventRespVO[]>([])
const checkItemTotal = ref(0)
const eventTotal = ref(0)
const autoOpenedTraceKey = ref('')

const queryParams = reactive<EdhrReleasePageReqVO & { pageNo: number; pageSize: number }>({
  pageNo: 1,
  pageSize: 10,
  batchExecutionCode:
    typeof route.query.batchExecutionCode === 'string' ? route.query.batchExecutionCode : '',
  workOrderCode: typeof route.query.workOrderCode === 'string' ? route.query.workOrderCode : '',
  batchCode: typeof route.query.batchCode === 'string' ? route.query.batchCode : '',
  productCode: typeof route.query.productCode === 'string' ? route.query.productCode : ''
})

const checkItemQuery = reactive<EdhrReleaseCheckItemPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  releaseTransactionId: '',
  itemStatus: 'OPEN' as const,
  checkResult: ''
})

const eventQuery = reactive<EdhrReleaseEventPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  releaseTransactionId: '',
  eventType: ''
})

const releaseDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'releaseObject', label: objectColumnLabel, minWidth: 250 },
  { key: 'productRoute', label: '产品/路线', minWidth: 230 },
  { key: 'releaseStatus', label: statusColumnLabel, width: 145 },
  { key: 'checkSummary', label: '检查摘要', width: 170 },
  { key: 'qualityGate', label: '质量门禁', minWidth: 260 },
  { key: 'transactionTime', label: '事务时间', minWidth: 180 },
  { key: 'traceActions', label: '追溯', width: 270, hideable: false, business: false }
]

const {
  columns: releaseColumns,
  saving: releaseColumnSaving,
  isColumnVisible: isReleaseColumnVisible,
  getColumnWidthString: getReleaseColumnWidthString,
  getColumnMinWidthString: getReleaseColumnMinWidthString,
  handleHeaderDragend: handleReleaseHeaderDragend,
  saveConfig: saveReleaseColumnConfig
} = useUserTableColumns(releaseTraceTableKey, releaseDefaultColumns)

const releaseQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'batchExecutionCode', label: '批次执行', type: 'text', queryParamKey: 'batchExecutionCode', placeholder: '请输入批次执行' },
  { key: 'workOrderCode', label: '工单号', type: 'text', queryParamKey: 'workOrderCode', placeholder: '请输入工单号' },
  { key: 'batchCode', label: '批次号', type: 'text', queryParamKey: 'batchCode', placeholder: '请输入批次号' },
  { key: 'productCode', label: '产品编码', type: 'text', queryParamKey: 'productCode', placeholder: '请输入产品编码' }
]

const releaseQuickFilter = useTableQuickFilter(
  releaseTraceTableKey,
  releaseQuickFilterDefinitions,
  queryParams,
  getList
)

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage
  }
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  return fallback
}

const resolveBatchExecutionTraceStatusLabel = (status?: number | string | null) => {
  const normalizedStatus = typeof status === 'number' ? status : Number(status)
  const labels: Record<number, string> = {
    [EDHR_BATCH_STATUS_CREATED]: '已创建',
    [EDHR_BATCH_STATUS_IN_PROGRESS]: '执行中',
    [EDHR_BATCH_STATUS_READY_TO_CLOSE]: '待关闭',
    [EDHR_BATCH_STATUS_REWORK_REQUIRED]: '需返工',
    [EDHR_BATCH_STATUS_CLOSED]: '已关闭',
    [EDHR_BATCH_STATUS_ARCHIVED]: '已归档',
    [EDHR_BATCH_STATUS_REJECTED]: '质量终态',
    [EDHR_BATCH_STATUS_VOIDED]: '已作废'
  }
  return Number.isFinite(normalizedStatus) ? labels[normalizedStatus] || String(normalizedStatus) : '--'
}

const buildQuery = (): EdhrReleasePageReqVO => ({
  pageNo: queryParams.pageNo,
  pageSize: queryParams.pageSize,
  batchExecutionCode: queryParams.batchExecutionCode?.trim() || undefined,
  workOrderCode: queryParams.workOrderCode?.trim() || undefined,
  batchCode: queryParams.batchCode?.trim() || undefined,
  productCode: queryParams.productCode?.trim() || undefined,
  batchExecutionStatuses: isRejectTrace ? [...EDHR_REJECT_TRACE_BATCH_STATUSES] : undefined,
  excludeBatchExecutionStatuses: isRejectTrace ? undefined : [...EDHR_RELEASE_TRACE_EXCLUDED_BATCH_STATUSES],
  completedTraceOnly: true,
  releaseStatus: 'RELEASED'
})

async function getList() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrReleasePage(buildQuery())
    list.value = data.list || []
    total.value = data.total || 0
    const requestedBatchExecutionId = String(route.query.autoOpenBatchExecutionId || '').trim()
    if (requestedBatchExecutionId && autoOpenedTraceKey.value !== requestedBatchExecutionId) {
      const matchedRow = list.value.find(
        (row) => String(row.batchExecutionId) === requestedBatchExecutionId
      )
      if (matchedRow) {
        autoOpenedTraceKey.value = requestedBatchExecutionId
        openBatchTrace(matchedRow)
      }
    }
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '电子批记录放行追溯列表加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = async () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.batchExecutionCode = ''
  queryParams.workOrderCode = ''
  queryParams.batchCode = ''
  queryParams.productCode = ''
  await releaseQuickFilter.resetQuickFilter()
}

const handlePrintArchive = async (row: EdhrReleaseRowVO) => {
  if (row.releaseStatus !== 'RELEASED') {
    actionError.value = '当前记录尚未正式放行，不能打印最终归档。'
    message.error(actionError.value)
    return
  }
  printLoadingBatchExecutionId.value = row.batchExecutionId
  actionError.value = ''
  try {
    const archive = await getLatestEdhrBatchArchive(row.batchExecutionId)
    if (!archive?.id) {
      throw new Error('当前批次没有可打印的打印版 PDF。')
    }
    await printEdhrBatchArchive(archive.id, archive.fileName)
    message.info('打印版 PDF 窗口已打开')
  } catch (error) {
    actionError.value = resolveErrorMessage(error, '打印版 PDF 打印入口打开失败。')
    message.error(actionError.value)
  } finally {
    if (printLoadingBatchExecutionId.value === row.batchExecutionId) {
      printLoadingBatchExecutionId.value = undefined
    }
  }
}

const openCheckItems = async (row: EdhrReleaseRowVO) => {
  if (!row.releaseTransactionId) {
    actionError.value = '该放行记录尚未生成放行事务，暂无检查项。'
    return
  }
  currentRow.value = row
  drawerVisible.value = true
  checkItemQuery.pageNo = 1
  checkItemQuery.releaseTransactionId = row.releaseTransactionId
  await getCheckItems()
}

const openBatchTrace = (row: EdhrReleaseRowVO) => {
  traceContext.value = {
    batchExecutionId: row.batchExecutionId,
    batchExecutionCode: row.batchExecutionCode,
    workOrderCode: row.workOrderCode,
    batchCode: row.batchCode,
    releaseTransactionId: row.releaseTransactionId,
    sourceTab: isRejectTrace ? 'reject' : 'release'
  }
  traceDrawerVisible.value = true
}

const getCheckItems = async () => {
  if (!checkItemQuery.releaseTransactionId) {
    checkItems.value = []
    checkItemTotal.value = 0
    return
  }
  checkItemLoading.value = true
  actionError.value = ''
  try {
    const data = await getEdhrReleaseCheckItemPage(checkItemQuery)
    checkItems.value = data.list || []
    checkItemTotal.value = data.total || 0
  } catch (error) {
    checkItems.value = []
    checkItemTotal.value = 0
    actionError.value = resolveErrorMessage(error, '电子批记录放行检查项加载失败。')
    message.error(resolveErrorMessage(error, actionError.value))
  } finally {
    checkItemLoading.value = false
  }
}

const openEventDrawer = async (row: EdhrReleaseRowVO) => {
  if (!row.releaseTransactionId) {
    actionError.value = '该放行记录尚未生成放行事务，暂无事务事件。'
    return
  }
  currentRow.value = row
  eventDrawerVisible.value = true
  eventQuery.pageNo = 1
  eventQuery.releaseTransactionId = row.releaseTransactionId
  await loadEventList()
}

const loadEventList = async () => {
  if (!eventQuery.releaseTransactionId) {
    eventList.value = []
    eventTotal.value = 0
    return
  }
  eventLoading.value = true
  actionError.value = ''
  try {
    const data = await getEdhrReleaseEventPage(eventQuery)
    eventList.value = data.list || []
    eventTotal.value = data.total || 0
  } catch (error) {
    eventList.value = []
    eventTotal.value = 0
    actionError.value = resolveErrorMessage(error, '电子批记录放行事务事件加载失败。')
    message.error(resolveErrorMessage(error, actionError.value))
  } finally {
    eventLoading.value = false
  }
}

const formatDateTime = (value?: string | number) => {
  return formatEdhrDateTime(value)
}

onMounted(() => getList())
</script>

<style scoped>
.edhr-form-trace-release__alert {
  margin: 12px;
}

.edhr-form-trace-release__strong {
  color: #172033;
  font-weight: 600;
  line-height: 1.5;
}

.edhr-form-trace-release__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-form-trace-release__metric {
  color: #263247;
  font-size: 13px;
  line-height: 1.6;
}

.edhr-form-trace-release__metric strong {
  color: #172033;
  font-variant-numeric: tabular-nums;
}

.edhr-form-trace-release__gate-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.edhr-form-trace-release__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.edhr-form-trace-release__drawer :deep(.el-drawer__body) {
  padding-top: 8px;
}

.edhr-form-trace-release__drawer-alert {
  margin-bottom: 12px;
}
</style>
