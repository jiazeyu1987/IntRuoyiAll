<template>
  <div class="edhr-form-trace-audit">
    <UnifiedListTemplate
      table-key="mes.pro.edhr.formTrace.audit"
      :query-model="queryParams"
      :filter-definitions="auditQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="auditQuickFilter.state"
      :selected-filter-definition="auditQuickFilter.selectedDefinition.value"
      :operator-options="auditQuickFilter.operatorOptions.value"
      :columns="auditColumns"
      :column-saving="auditColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="auditQuickFilter.updateState"
      @quick-filter-query="auditQuickFilter.applyQuickFilter"
      @column-change="saveAuditColumnConfig"
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
          class="edhr-form-trace-audit__alert"
        />
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.edhr.formTrace.audit"
          :data="list"
          border
          stripe
          row-key="executionId"
          :show-overflow-tooltip="true"
          empty-text="暂无追踪记录"
          @header-dragend="handleAuditHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isAuditColumnVisible('evidenceExpand')"
            type="expand"
            :width="getAuditColumnWidthString('evidenceExpand', 42)"
          >
            <template #default="{ row }">
              <div class="edhr-form-trace-audit__evidence">
                <div class="edhr-form-trace-audit__evidence-title">追踪证据</div>
                <div class="edhr-form-trace-audit__evidence-sections">
                  <div class="edhr-form-trace-audit__evidence-section">
                    <div class="edhr-form-trace-audit__section-title">普通工序填写签名证据</div>
                    <div class="edhr-form-trace-audit__muted">
                      当前工序以填写提交签名作为流转证据，普通工序不要求审核/批准。
                    </div>
                    <el-tag effect="plain" type="success">
                      {{ row.lastEvidenceCategoryName || resolveEvidenceCategoryName(row.lastEventType) }}
                    </el-tag>
                  </div>
                  <div class="edhr-form-trace-audit__evidence-section">
                    <div class="edhr-form-trace-audit__section-title">放行阶段审核/批准证据</div>
                    <div class="edhr-form-trace-audit__muted">
                      质量审核和批准在放行阶段追溯；历史工序审核/批准证据（只读）仅作为既有记录展示。
                    </div>
                    <el-tag effect="plain" type="warning">放行阶段审核/批准证据</el-tag>
                  </div>
                </div>
                <div class="edhr-form-trace-audit__evidence-grid">
                  <div class="edhr-form-trace-audit__evidence-item">
                    <span>执行记录</span>
                    <strong>{{ row.executionId || '--' }}</strong>
                  </div>
                  <div class="edhr-form-trace-audit__evidence-item">
                    <span>工单编号</span>
                    <strong>{{ row.workOrderId || '--' }}</strong>
                  </div>
                  <div class="edhr-form-trace-audit__evidence-item">
                    <span>批次编号</span>
                    <strong>{{ row.batchId || '--' }}</strong>
                  </div>
                  <div class="edhr-form-trace-audit__evidence-item">
                    <span>流程实例</span>
                    <strong>{{ row.processInstanceId || '--' }}</strong>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isAuditColumnVisible('execution')"
            label="执行编号"
            prop="execution"
            :width="getAuditColumnWidthString('execution', 190)"
            v-bind="sortColumnAttrs('execution')"
          >
            <template #default="{ row }">
              <el-button
                v-if="canOpenDetail(row)"
                link
                type="primary"
                class="edhr-form-trace-audit__link"
                @click="openTrackingDetail(row)"
              >
                {{ resolveExecutionDisplay(row) }}
              </el-button>
              <el-tooltip v-else content="缺少执行记录 ID，无法打开执行追踪。">
                <span class="edhr-form-trace-audit__link is-disabled">
                  {{ resolveExecutionDisplay(row) }}
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isAuditColumnVisible('productionContext')"
            label="生产上下文"
            prop="productionContext"
            :min-width="getAuditColumnMinWidthString('productionContext', 270)"
            v-bind="sortColumnAttrs('productionContext')"
          >
            <template #default="{ row }">
              <div>
                <el-button
                  v-if="row.workOrderId"
                  link
                  type="primary"
                  class="edhr-form-trace-audit__link"
                  @click="openWorkOrder(row)"
                >
                  {{ row.workOrderCode || '--' }}
                </el-button>
                <span v-else class="edhr-form-trace-audit__strong">{{ row.workOrderCode || '--' }}</span>
              </div>
              <div class="edhr-form-trace-audit__muted">
                批次：
                <el-button
                  v-if="row.batchId"
                  link
                  type="primary"
                  class="edhr-form-trace-audit__inline-link"
                  @click="openBatch(row)"
                >
                  {{ row.batchCode || '--' }}
                </el-button>
                <span v-else>{{ row.batchCode || '--' }}</span>
              </div>
              <div class="edhr-form-trace-audit__muted">
                {{ row.processName || '--' }} / {{ row.workstationName || '--' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isAuditColumnVisible('stage')"
            label="当前阶段"
            prop="stage"
            :min-width="getAuditColumnMinWidthString('stage', 190)"
            v-bind="sortColumnAttrs('stage')"
          >
            <template #default="{ row }">
              <el-tag :type="resolveStatusType(row.status)" effect="plain">
                {{ resolveStatusLabel(row.status) }}
              </el-tag>
              <div class="edhr-form-trace-audit__muted">{{ row.currentNodeName || '--' }}</div>
              <div class="edhr-form-trace-audit__muted">
                {{ row.currentAssigneeNames?.join('、') || '--' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isAuditColumnVisible('lastEvent')"
            label="最后处理"
            prop="lastEvent"
            :min-width="getAuditColumnMinWidthString('lastEvent', 230)"
            v-bind="sortColumnAttrs('lastEvent')"
          >
            <template #default="{ row }">
              <div class="edhr-form-trace-audit__strong">{{ formatTrackingEvent(row.lastEventType) }}</div>
              <div class="edhr-form-trace-audit__muted">{{ formatTrackingLastEventAt(row.lastEventAt) }}</div>
              <div class="edhr-form-trace-audit__muted">{{ row.lastEventReason || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isAuditColumnVisible('archiveStatus')"
            label="归档状态"
            prop="archiveStatus"
            :width="getAuditColumnWidthString('archiveStatus', 120)"
            v-bind="sortColumnAttrs('archiveStatus')"
          >
            <template #default="{ row }">
              <el-tag :type="formatArchiveStatusType(row.archiveStatus)" effect="plain">
                {{ formatArchiveStatusLabel(row.archiveStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isAuditColumnVisible('traceActions')"
            label="追溯"
            prop="traceActions"
            :width="getAuditColumnWidthString('traceActions', 90)"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button link type="primary" :disabled="!canOpenDetail(row)" @click="openBatchTrace(row)">
                追溯
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
    <BatchForm ref="batchFormRef" />
    <BatchExecutionTraceDrawer
      v-model="traceDrawerVisible"
      :context="traceContext"
    />
  </div>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { EDHR_EXECUTION_STATUS } from '@/api/mes/pro/edhr/approval'
import {
  getEdhrTrackingPage,
  type EdhrTrackingEventType,
  type EdhrTrackingRowVO
} from '@/api/mes/pro/edhr/tracking'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'
import BatchForm from '@/views/mes/wm/batch/BatchForm.vue'
import BatchExecutionTraceDrawer from './BatchExecutionTraceDrawer.vue'
import type { BatchExecutionTraceContext } from './traceContext'

defineOptions({ name: 'MesProEdhrFormTraceAuditTab' })

const route = useRoute()
const router = useRouter()
const batchFormRef = ref<InstanceType<typeof BatchForm>>()
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrTrackingRowVO[]>([])
const total = ref(0)
const traceDrawerVisible = ref(false)
const traceContext = ref<BatchExecutionTraceContext>()

const TRACKING_EVENT_LABELS: Record<EdhrTrackingEventType, string> = {
  CREATE: '创建',
  SAVE_DRAFT: '保存草稿',
  FIELD_CHANGE: '字段变更',
  FORM_REVIEW: '表单复核',
  SUBMIT: '填写提交签名',
  REVIEW_APPROVE: '审核签名',
  APPROVE: '审批通过',
  REJECT: '审批驳回',
  ARCHIVE_SEAL: '归档封存'
}
const ARCHIVE_STATUS_LABELS: Record<NonNullable<EdhrTrackingRowVO['archiveStatus']>, string> = {
  GENERATING: '生成中',
  SEALED: '已封存',
  FAILED: '生成失败'
}

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  executionCode: typeof route.query.executionCode === 'string' ? route.query.executionCode : '',
  workOrderCode: typeof route.query.workOrderCode === 'string' ? route.query.workOrderCode : '',
  batchCode: typeof route.query.batchCode === 'string' ? route.query.batchCode : '',
  processId: undefined as number | undefined,
  workstationId: undefined as number | undefined,
  status: undefined as number | undefined,
  submittedBy: undefined as number | undefined,
  approvedBy: undefined as number | undefined,
  processInstanceId: '',
  actorName: ''
})

const auditDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'evidenceExpand', label: '追踪证据', width: 42, hideable: false, business: false },
  { key: 'execution', label: '执行编号', width: 190 },
  { key: 'productionContext', label: '生产上下文', minWidth: 270 },
  { key: 'stage', label: '当前阶段', minWidth: 190 },
  { key: 'lastEvent', label: '最后处理', minWidth: 230 },
  { key: 'archiveStatus', label: '归档状态', width: 120 },
  { key: 'traceActions', label: '追溯', width: 90, hideable: false, business: false, sortable: false }
]

const {
  columns: auditColumns,
  saving: auditColumnSaving,
  isColumnVisible: isAuditColumnVisible,
  getColumnWidthString: getAuditColumnWidthString,
  getColumnMinWidthString: getAuditColumnMinWidthString,
  handleHeaderDragend: handleAuditHeaderDragend,
  saveConfig: saveAuditColumnConfig
} = useUserTableColumns('mes.pro.edhr.formTrace.audit', auditDefaultColumns)

const auditQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'executionCode', label: '执行编号', type: 'text', queryParamKey: 'executionCode', placeholder: '请输入执行编号' },
  { key: 'workOrderCode', label: '工单号', type: 'text', queryParamKey: 'workOrderCode', placeholder: '请输入工单号' },
  { key: 'batchCode', label: '批次号', type: 'text', queryParamKey: 'batchCode', placeholder: '请输入批次号' },
  { key: 'processInstanceId', label: '流程实例', type: 'text', queryParamKey: 'processInstanceId', placeholder: '请输入流程实例' },
  { key: 'actorName', label: '处理人', type: 'text', queryParamKey: 'actorName', placeholder: '请输入处理人' },
  {
    key: 'status',
    label: '当前阶段',
    type: 'select',
    queryParamKey: 'status',
    options: [
      { label: '草稿', value: EDHR_EXECUTION_STATUS.DRAFT },
      { label: '填写完成', value: EDHR_EXECUTION_STATUS.FILL_COMPLETED },
      { label: '待审批', value: EDHR_EXECUTION_STATUS.SUBMITTED },
      { label: '已关闭', value: EDHR_EXECUTION_STATUS.APPROVED },
      { label: '已驳回', value: EDHR_EXECUTION_STATUS.REJECTED }
    ]
  }
]

const auditQuickFilter = useTableQuickFilter(
  'mes.pro.edhr.formTrace.audit',
  auditQuickFilterDefinitions,
  queryParams,
  getList
)

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const message = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof message === 'string' && message.trim()) return message
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const normalizePositiveProcessId = (value?: number | string | null) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const resolveStatusLabel = (status?: number) => {
  if (status === EDHR_EXECUTION_STATUS.SUBMITTED) return '待审批'
  if (status === EDHR_EXECUTION_STATUS.REJECTED) return '已驳回'
  if (status === EDHR_EXECUTION_STATUS.APPROVED) return '已关闭'
  if (status === EDHR_EXECUTION_STATUS.FILL_COMPLETED) return '填写完成'
  return '草稿'
}

const resolveStatusType = (status?: number) => {
  if (status === EDHR_EXECUTION_STATUS.SUBMITTED) return 'warning'
  if (status === EDHR_EXECUTION_STATUS.REJECTED) return 'danger'
  if (status === EDHR_EXECUTION_STATUS.APPROVED) return 'success'
  if (status === EDHR_EXECUTION_STATUS.FILL_COMPLETED) return 'success'
  return 'info'
}

const formatTrackingEvent = (eventType?: EdhrTrackingEventType) => {
  if (!eventType) return '--'
  const label = TRACKING_EVENT_LABELS[eventType]
  if (!label) {
    throw new Error(`未知追踪最后事件：${String(eventType)}`)
  }
  return label
}

const resolveEvidenceCategoryName = (eventType?: EdhrTrackingEventType) => {
  if (eventType === 'SUBMIT') return '普通工序填写提交证据'
  if (eventType === 'FORM_REVIEW') return '历史工序审核/批准证据（只读）'
  if (eventType === 'APPROVE' || eventType === 'REVIEW_APPROVE' || eventType === 'REJECT') {
    return '放行阶段审核/批准证据'
  }
  if (eventType === 'ARCHIVE_SEAL') return '归档封存证据'
  return '技术追踪证据'
}

const formatArchiveStatusLabel = (archiveStatus?: EdhrTrackingRowVO['archiveStatus']) => {
  if (!archiveStatus) return '未归档'
  const label = ARCHIVE_STATUS_LABELS[archiveStatus]
  if (!label) {
    throw new Error(`未知归档状态：${String(archiveStatus)}`)
  }
  return label
}

const formatArchiveStatusType = (archiveStatus?: EdhrTrackingRowVO['archiveStatus']) => {
  if (archiveStatus === 'SEALED') return 'success'
  if (archiveStatus === 'FAILED') return 'danger'
  if (archiveStatus === 'GENERATING') return 'warning'
  return 'info'
}

const formatTrackingLastEventAt = (lastEventAt?: string | number | Date) =>
  formatEdhrDateTime(lastEventAt)

const resolveExecutionDisplay = (row: EdhrTrackingRowVO) =>
  row.executionCode || (row.executionId ? `#${row.executionId}` : '--')

const canOpenDetail = (row: EdhrTrackingRowVO) =>
  Number.isFinite(Number(row.executionId)) && Number(row.executionId) > 0

async function getList() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrTrackingPage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      executionCode: queryParams.executionCode.trim() || undefined,
      workOrderCode: queryParams.workOrderCode.trim() || undefined,
      batchCode: queryParams.batchCode.trim() || undefined,
      processId: normalizePositiveProcessId(queryParams.processId),
      workstationId: Number.isFinite(queryParams.workstationId) ? queryParams.workstationId : undefined,
      status: queryParams.status as any,
      submittedBy: Number.isFinite(queryParams.submittedBy) ? queryParams.submittedBy : undefined,
      approvedBy: Number.isFinite(queryParams.approvedBy) ? queryParams.approvedBy : undefined,
      processInstanceId: queryParams.processInstanceId.trim() || undefined,
      actorName: queryParams.actorName.trim() || undefined
    })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'eDHR 追踪列表加载失败，请联系管理员。')
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
  queryParams.executionCode = ''
  queryParams.workOrderCode = ''
  queryParams.batchCode = ''
  queryParams.processId = undefined
  queryParams.workstationId = undefined
  queryParams.status = undefined
  queryParams.submittedBy = undefined
  queryParams.approvedBy = undefined
  queryParams.processInstanceId = ''
  queryParams.actorName = ''
  await auditQuickFilter.resetQuickFilter()
}

const openTrackingDetail = async (row: EdhrTrackingRowVO) => {
  if (!canOpenDetail(row)) {
    throw new Error('缺少执行记录 ID，无法打开执行追踪。')
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-execution/form',
    query: { id: String(row.executionId), viewMode: 'tracking' }
  })
}

const openBatchTrace = (row: EdhrTrackingRowVO) => {
  if (!canOpenDetail(row)) {
    throw new Error('缺少执行记录 ID，无法打开批次执行追溯。')
  }
  traceContext.value = {
    executionId: row.executionId,
    executionCode: row.executionCode,
    workOrderCode: row.workOrderCode,
    batchCode: row.batchCode,
    sourceTab: 'audit'
  }
  traceDrawerVisible.value = true
}

const openWorkOrder = async (row: EdhrTrackingRowVO) => {
  if (!row.workOrderId) {
    throw new Error(`追踪行缺少工单ID：executionId=${row.executionId}`)
  }
  await router.push({
    path: '/mes/pro/work-order',
    query: {
      code: row.workOrderCode,
      openId: String(row.workOrderId)
    }
  })
}

const openBatch = (row: EdhrTrackingRowVO) => {
  if (!row.batchId) {
    throw new Error(`追踪行缺少批次ID：executionId=${row.executionId}`)
  }
  if (!batchFormRef.value) {
    throw new Error('批次详情组件未挂载')
  }
  batchFormRef.value.open(row.batchId)
}

onMounted(() => getList())
</script>

<style scoped>
.edhr-form-trace-audit__alert {
  margin: 12px;
}

.edhr-form-trace-audit__link,
.edhr-form-trace-audit__inline-link {
  padding: 0;
  font-weight: 600;
}

.edhr-form-trace-audit__link.is-disabled {
  color: #9ca3af;
  cursor: not-allowed;
}

.edhr-form-trace-audit__strong {
  color: #172033;
  font-weight: 600;
  line-height: 1.5;
}

.edhr-form-trace-audit__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-form-trace-audit__evidence {
  padding: 14px 18px 16px;
  background: #f7f9fc;
}

.edhr-form-trace-audit__evidence-title,
.edhr-form-trace-audit__section-title {
  color: #172033;
  font-weight: 700;
}

.edhr-form-trace-audit__evidence-sections {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.edhr-form-trace-audit__evidence-section {
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-form-trace-audit__evidence-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.edhr-form-trace-audit__evidence-item {
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-form-trace-audit__evidence-item span {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.edhr-form-trace-audit__evidence-item strong {
  display: block;
  margin-top: 4px;
  color: #172033;
  font-size: 14px;
}
</style>
