<template>
  <div class="edhr-form-trace-change">
    <UnifiedListTemplate
      table-key="mes.pro.edhr.formTrace.change"
      :query-model="queryParams"
      :filter-definitions="changeQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="changeQuickFilter.state"
      :selected-filter-definition="changeQuickFilter.selectedDefinition.value"
      :operator-options="changeQuickFilter.operatorOptions.value"
      :columns="changeColumns"
      :column-saving="changeColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="changeQuickFilter.updateState"
      @quick-filter-query="changeQuickFilter.applyQuickFilter"
      @column-change="saveChangeColumnConfig"
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
          class="edhr-form-trace-change__alert"
        />
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.edhr.formTrace.change"
          :data="list"
          border
          stripe
          row-key="id"
          :show-overflow-tooltip="true"
          empty-text="暂无变更记录"
          @header-dragend="handleChangeHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isChangeColumnVisible('evidenceExpand')"
            type="expand"
            :width="getChangeColumnWidthString('evidenceExpand', 42)"
          >
            <template #default="{ row }">
              <div class="edhr-form-trace-change__evidence">
                <div class="edhr-form-trace-change__evidence-title">变更证据</div>
                <div class="edhr-form-trace-change__evidence-grid">
                  <div class="edhr-form-trace-change__evidence-item">
                    <span>申请签名</span>
                    <strong>{{ row.requestSignatureId || '--' }}</strong>
                  </div>
                  <div class="edhr-form-trace-change__evidence-item">
                    <span>审批签名</span>
                    <strong>{{ row.approvalSignatureId || '--' }}</strong>
                  </div>
                  <div class="edhr-form-trace-change__evidence-item">
                    <span>原链头哈希</span>
                    <strong>{{ row.previousHeadHash || '--' }}</strong>
                  </div>
                  <div class="edhr-form-trace-change__evidence-item">
                    <span>新链头哈希</span>
                    <strong>{{ row.newHeadHash || '--' }}</strong>
                  </div>
                  <div class="edhr-form-trace-change__evidence-item">
                    <span>原归档哈希</span>
                    <strong>{{ row.previousArchiveHash || '--' }}</strong>
                  </div>
                  <div class="edhr-form-trace-change__evidence-item">
                    <span>新归档哈希</span>
                    <strong>{{ row.newArchiveHash || '--' }}</strong>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isChangeColumnVisible('changeCode')"
            label="变更编号"
            prop="changeCode"
            :width="getChangeColumnWidthString('changeCode', 210)"
            v-bind="sortColumnAttrs('changeCode')"
          />
          <el-table-column
            v-if="isChangeColumnVisible('changeType')"
            label="类型"
            prop="changeType"
            :width="getChangeColumnWidthString('changeType', 110)"
            v-bind="sortColumnAttrs('changeType')"
          >
            <template #default="{ row }">
              <el-tag :type="resolveChangeTypeTag(row.changeType)">
                {{ resolveChangeTypeLabel(row.changeType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isChangeColumnVisible('changeStatus')"
            label="状态"
            prop="changeStatus"
            :width="getChangeColumnWidthString('changeStatus', 110)"
            v-bind="sortColumnAttrs('changeStatus')"
          >
            <template #default="{ row }">
              <el-tag :type="resolveChangeStatusTag(row.changeStatus)">
                {{ resolveChangeStatusLabel(row.changeStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isChangeColumnVisible('targetObject')"
            label="对象"
            prop="targetObject"
            :min-width="getChangeColumnMinWidthString('targetObject', 190)"
            v-bind="sortColumnAttrs('targetObject')"
          >
            <template #default="{ row }">
              <div>{{ resolveTargetScopeLabel(row.targetScope) }}</div>
              <div class="edhr-form-trace-change__muted">
                批次ID：
                <el-button
                  v-if="canOpenBatchExecution(row)"
                  link
                  type="primary"
                  class="edhr-form-trace-change__object-link"
                  @click="openBatchExecution(row)"
                >
                  {{ formatObjectId(row.batchExecutionId) }}
                </el-button>
                <span v-else class="edhr-form-trace-change__object-link--disabled">--</span>
              </div>
              <div class="edhr-form-trace-change__muted">
                执行ID：
                <el-button
                  v-if="canOpenExecution(row)"
                  link
                  type="primary"
                  class="edhr-form-trace-change__object-link"
                  @click="openExecution(row)"
                >
                  {{ formatObjectId(row.executionId) }}
                </el-button>
                <span v-else class="edhr-form-trace-change__object-link--disabled">--</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isChangeColumnVisible('statusChange')"
            label="状态变化"
            prop="statusChange"
            :min-width="getChangeColumnMinWidthString('statusChange', 160)"
            v-bind="sortColumnAttrs('statusChange')"
          >
            <template #default="{ row }">
              <div>{{ resolveExecutionStatusLabel(row.previousStatus) }}</div>
              <div class="edhr-form-trace-change__muted">到 {{ resolveExecutionStatusLabel(row.newStatus) }}</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isChangeColumnVisible('reason')"
            label="原因"
            prop="reason"
            :min-width="getChangeColumnMinWidthString('reason', 240)"
            v-bind="sortColumnAttrs('reason')"
          >
            <template #default="{ row }">
              <div>{{ row.reasonCategory || '--' }}</div>
              <div class="edhr-form-trace-change__muted">{{ row.reasonText || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isChangeColumnVisible('requestedAt')"
            label="申请时间"
            prop="requestedAt"
            :width="getChangeColumnWidthString('requestedAt', 180)"
            :formatter="edhrDateTimeFormatter"
            v-bind="sortColumnAttrs('requestedAt')"
          />
          <el-table-column
            v-if="isChangeColumnVisible('effectiveAt')"
            label="生效时间"
            prop="effectiveAt"
            :width="getChangeColumnWidthString('effectiveAt', 180)"
            :formatter="edhrDateTimeFormatter"
            v-bind="sortColumnAttrs('effectiveAt')"
          />
          <el-table-column
            v-if="isChangeColumnVisible('actions')"
            label="操作"
            prop="actions"
            :width="getChangeColumnWidthString('actions', 145)"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openBatchTrace(row)">追溯</el-button>
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>

    <Dialog title="电子批记录变更详情" v-model="detailDialogVisible" width="860px">
      <el-tabs v-model="detailActiveTab" class="edhr-form-trace-change__detail-tabs">
        <el-tab-pane label="批记录表单" name="recordForm">
          <div class="edhr-form-trace-change__record-card">
            <div class="edhr-form-trace-change__record-card-header">
              <div>
                <div class="edhr-form-trace-change__record-card-title">批记录表单快照</div>
                <div class="edhr-form-trace-change__muted">
                  使用表单追溯归档的执行快照打开只读表单，展示方式与批次执行填写页保持一致。
                </div>
              </div>
              <el-button
                type="primary"
                :disabled="!canOpenBatchExecution(selectedChange)"
                @click="openSelectedChangeRecordForm"
              >
                查看批记录表单
              </el-button>
            </div>
            <el-descriptions :column="2" border class="edhr-form-trace-change__detail">
              <el-descriptions-item label="变更编号">
                {{ selectedChange?.changeCode || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                {{ resolveChangeStatusLabel(selectedChange?.changeStatus) }}
              </el-descriptions-item>
              <el-descriptions-item label="批次ID">
                {{ formatObjectId(selectedChange?.batchExecutionId) }}
              </el-descriptions-item>
              <el-descriptions-item label="执行ID">
                {{ formatObjectId(selectedChange?.executionId) }}
              </el-descriptions-item>
            </el-descriptions>
            <el-alert
              class="edhr-form-trace-change__record-alert"
              type="info"
              :closable="false"
              show-icon
              title="点击“查看批记录表单”后，将在右侧打开可视化只读表单；不会进入独立历史批记录页面，也不会触发保存、签名或放行。"
            />
          </div>
        </el-tab-pane>
        <el-tab-pane label="变更详情" name="changeDetail">
          <div class="edhr-form-trace-change__detail-summary">
            <div class="edhr-form-trace-change__detail-summary-item">
              <span>变更编号</span>
              <strong>{{ selectedChange?.changeCode || '--' }}</strong>
            </div>
            <div class="edhr-form-trace-change__detail-summary-item">
              <span>变更对象</span>
              <strong>{{ resolveTargetScopeLabel(selectedChange?.targetScope) }}</strong>
            </div>
            <div class="edhr-form-trace-change__detail-summary-item">
              <span>状态</span>
              <strong>{{ resolveChangeStatusLabel(selectedChange?.changeStatus) }}</strong>
            </div>
            <div class="edhr-form-trace-change__detail-summary-item">
              <span>生效时间</span>
              <strong>{{ formatEdhrDateTime(selectedChange?.effectiveAt) }}</strong>
            </div>
          </div>

          <el-descriptions :column="2" border class="edhr-form-trace-change__detail">
            <el-descriptions-item label="变更类型">{{ resolveChangeTypeLabel(selectedChange?.changeType) }}</el-descriptions-item>
            <el-descriptions-item label="状态变化">
              {{ resolveExecutionStatusLabel(selectedChange?.previousStatus) }} ->
              {{ resolveExecutionStatusLabel(selectedChange?.newStatus) }}
            </el-descriptions-item>
            <el-descriptions-item label="批次ID">
              <el-button
                v-if="canOpenBatchExecution(selectedChange)"
                link
                type="primary"
                class="edhr-form-trace-change__object-link"
                @click="openBatchExecution(selectedChange)"
              >
                {{ formatObjectId(selectedChange?.batchExecutionId) }}
              </el-button>
              <span v-else class="edhr-form-trace-change__object-link--disabled">--</span>
            </el-descriptions-item>
            <el-descriptions-item label="执行ID">
              <el-button
                v-if="canOpenExecution(selectedChange)"
                link
                type="primary"
                class="edhr-form-trace-change__object-link"
                @click="openExecution(selectedChange)"
              >
                {{ formatObjectId(selectedChange?.executionId) }}
              </el-button>
              <span v-else class="edhr-form-trace-change__object-link--disabled">--</span>
            </el-descriptions-item>
            <el-descriptions-item label="原因分类">{{ selectedChange?.reasonCategory || '--' }}</el-descriptions-item>
            <el-descriptions-item label="申请时间">
              {{ formatEdhrDateTime(selectedChange?.requestedAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="原因说明" :span="2">{{ selectedChange?.reasonText || '--' }}</el-descriptions-item>
          </el-descriptions>

          <el-collapse v-model="detailEvidenceNames" class="edhr-form-trace-change__evidence-collapse">
            <el-collapse-item title="链路证据" name="chain-evidence">
              <div class="edhr-form-trace-change__evidence-grid">
                <div class="edhr-form-trace-change__evidence-item">
                  <span>申请签名</span>
                  <strong>{{ selectedChange?.requestSignatureId || '--' }}</strong>
                </div>
                <div class="edhr-form-trace-change__evidence-item">
                  <span>审批签名</span>
                  <strong>{{ selectedChange?.approvalSignatureId || '--' }}</strong>
                </div>
                <div class="edhr-form-trace-change__evidence-item">
                  <span>原链头哈希</span>
                  <strong>{{ selectedChange?.previousHeadHash || '--' }}</strong>
                </div>
                <div class="edhr-form-trace-change__evidence-item">
                  <span>新链头哈希</span>
                  <strong>{{ selectedChange?.newHeadHash || '--' }}</strong>
                </div>
                <div class="edhr-form-trace-change__evidence-item">
                  <span>原归档哈希</span>
                  <strong>{{ selectedChange?.previousArchiveHash || '--' }}</strong>
                </div>
                <div class="edhr-form-trace-change__evidence-item">
                  <span>新归档哈希</span>
                  <strong>{{ selectedChange?.newArchiveHash || '--' }}</strong>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-tab-pane>
      </el-tabs>
    </Dialog>
    <BatchExecutionTraceDrawer
      v-model="traceDrawerVisible"
      :context="traceContext"
    />
  </div>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  EDHR_CHANGE_STATUS_APPROVED,
  EDHR_CHANGE_STATUS_DRAFT,
  EDHR_CHANGE_STATUS_EFFECTIVE,
  EDHR_CHANGE_STATUS_REJECTED,
  EDHR_CHANGE_STATUS_SUBMITTED,
  EDHR_CHANGE_TYPE_VOID,
  getEdhrRecordChange,
  getEdhrRecordChangePage,
  type EdhrRecordChangePageReqVO,
  type EdhrRecordChangeRespVO
} from '@/api/mes/pro/edhr/change'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import {
  edhrDateTimeFormatter,
  formatEdhrDateTime
} from '@/views/mes/pro/edhr/shared/dateTime'
import BatchExecutionTraceDrawer from './BatchExecutionTraceDrawer.vue'
import type { BatchExecutionTraceContext } from './traceContext'

defineOptions({ name: 'MesProEdhrFormTraceChangeTab' })

const EDHR_FORM_TRACE_CHANGE_TYPE = EDHR_CHANGE_TYPE_VOID

const props = withDefaults(
  defineProps<{
    batchExecutionId?: string | number
    executionId?: string | number
    targetScope?: string
    autoLoad?: boolean
    pageSize?: number
  }>(),
  {
    autoLoad: true,
    pageSize: 10
  }
)

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrRecordChangeRespVO[]>([])
const total = ref(0)
const detailDialogVisible = ref(false)
const selectedChange = ref<EdhrRecordChangeRespVO>()
const detailActiveTab = ref<'recordForm' | 'changeDetail'>('recordForm')
const detailEvidenceNames = ref<string[]>([])
const traceDrawerVisible = ref(false)
const traceContext = ref<BatchExecutionTraceContext>()

const normalizeText = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return rawValue == null ? '' : String(rawValue).trim()
}

const queryParams = reactive<EdhrRecordChangePageReqVO>({
  pageNo: 1,
  pageSize: props.pageSize,
  changeType: EDHR_FORM_TRACE_CHANGE_TYPE,
  targetScope: normalizeText(props.targetScope) || normalizeText(route.query.targetScope) || undefined,
  batchExecutionId: parsePositiveRouteQueryId(props.batchExecutionId ?? route.query.batchExecutionId) || undefined,
  executionId: parsePositiveRouteQueryId(props.executionId ?? route.query.executionId) || undefined,
  changeStatus: typeof route.query.changeStatus === 'string' ? route.query.changeStatus : undefined
})

const changeTypeOptions = [
  { label: '作废', value: EDHR_FORM_TRACE_CHANGE_TYPE }
]

const changeStatusOptions = [
  { label: '草稿', value: EDHR_CHANGE_STATUS_DRAFT },
  { label: '已提交', value: EDHR_CHANGE_STATUS_SUBMITTED },
  { label: '已批准', value: EDHR_CHANGE_STATUS_APPROVED },
  { label: '已拒绝', value: EDHR_CHANGE_STATUS_REJECTED },
  { label: '已生效', value: EDHR_CHANGE_STATUS_EFFECTIVE }
]

const changeDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'evidenceExpand', label: '变更证据', width: 42, hideable: false, business: false },
  { key: 'changeCode', label: '变更编号', width: 210 },
  { key: 'changeType', label: '类型', width: 110 },
  { key: 'changeStatus', label: '状态', width: 110 },
  { key: 'targetObject', label: '对象', minWidth: 190 },
  { key: 'statusChange', label: '状态变化', minWidth: 160 },
  { key: 'reason', label: '原因', minWidth: 240 },
  { key: 'requestedAt', label: '申请时间', width: 180 },
  { key: 'effectiveAt', label: '生效时间', width: 180 },
  { key: 'actions', label: '操作', width: 145, hideable: false, business: false }
]

const {
  columns: changeColumns,
  saving: changeColumnSaving,
  isColumnVisible: isChangeColumnVisible,
  getColumnWidthString: getChangeColumnWidthString,
  getColumnMinWidthString: getChangeColumnMinWidthString,
  handleHeaderDragend: handleChangeHeaderDragend,
  saveConfig: saveChangeColumnConfig
} = useUserTableColumns('mes.pro.edhr.formTrace.change', changeDefaultColumns)

const changeQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'changeType',
    label: '变更类型',
    type: 'select',
    queryParamKey: 'changeType',
    options: changeTypeOptions
  },
  {
    key: 'changeStatus',
    label: '状态',
    type: 'select',
    queryParamKey: 'changeStatus',
    options: changeStatusOptions
  },
  {
    key: 'targetScope',
    label: '范围',
    type: 'select',
    queryParamKey: 'targetScope',
    options: [
      { label: '执行记录', value: 'EXECUTION' },
      { label: '批次', value: 'BATCH' }
    ]
  },
  { key: 'batchExecutionId', label: '批次ID', type: 'text', queryParamKey: 'batchExecutionId', placeholder: '请输入批次ID' },
  { key: 'executionId', label: '执行ID', type: 'text', queryParamKey: 'executionId', placeholder: '请输入执行ID' }
]

const changeQuickFilter = useTableQuickFilter(
  'mes.pro.edhr.formTrace.change',
  changeQuickFilterDefinitions,
  queryParams,
  getList
)

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const resolveChangeTypeLabel = (type?: string) => {
  const labels: Record<string, string> = {
    [EDHR_FORM_TRACE_CHANGE_TYPE]: '作废'
  }
  return type ? labels[type] || '未知类型' : '--'
}

const resolveChangeTypeTag = (type?: string) => {
  if (type === EDHR_FORM_TRACE_CHANGE_TYPE) return 'danger'
  return 'info'
}

const resolveChangeStatusLabel = (status?: string) => {
  const labels: Record<string, string> = {
    [EDHR_CHANGE_STATUS_DRAFT]: '草稿',
    [EDHR_CHANGE_STATUS_SUBMITTED]: '已提交',
    [EDHR_CHANGE_STATUS_APPROVED]: '已批准',
    [EDHR_CHANGE_STATUS_REJECTED]: '已拒绝',
    [EDHR_CHANGE_STATUS_EFFECTIVE]: '已生效'
  }
  return status ? labels[status] || '未知状态' : '--'
}

const resolveChangeStatusTag = (status?: string) => {
  if (status === EDHR_CHANGE_STATUS_EFFECTIVE) return 'success'
  if (status === EDHR_CHANGE_STATUS_REJECTED) return 'danger'
  if (status === EDHR_CHANGE_STATUS_SUBMITTED || status === EDHR_CHANGE_STATUS_APPROVED) return 'warning'
  return 'info'
}

const resolveTargetScopeLabel = (scope?: string) => {
  if (scope === 'BATCH') return '批次'
  if (scope === 'EXECUTION') return '执行记录'
  return scope || '--'
}

const resolveExecutionStatusLabel = (status?: string) => {
  if (!status) return '--'
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    SUBMITTED: '待审批',
    APPROVED: '已关闭',
    REJECTED: '已驳回',
    VOIDED: '已作废',
    REOPENED: '已重开'
  }
  return labels[status] || '未知状态'
}

const isPositiveId = (value?: number) => Number.isFinite(Number(value)) && Number(value) > 0

const formatObjectId = (value?: number) => (isPositiveId(value) ? `#${Number(value)}` : '--')

const canOpenBatchExecution = (row?: Pick<EdhrRecordChangeRespVO, 'batchExecutionId'>) =>
  isPositiveId(row?.batchExecutionId)

const canOpenExecution = (row?: Pick<EdhrRecordChangeRespVO, 'executionId'>) => isPositiveId(row?.executionId)

const syncExternalContext = () => {
  if (props.batchExecutionId !== undefined) {
    queryParams.batchExecutionId = parsePositiveRouteQueryId(props.batchExecutionId) || undefined
  }
  if (props.executionId !== undefined) {
    queryParams.executionId = parsePositiveRouteQueryId(props.executionId) || undefined
  }
  if (props.targetScope !== undefined) {
    queryParams.targetScope = normalizeText(props.targetScope) || undefined
  }
}

const normalizeQueryParams = () => {
  queryParams.changeType = EDHR_FORM_TRACE_CHANGE_TYPE
  queryParams.executionId = parsePositiveRouteQueryId(queryParams.executionId) || undefined
  queryParams.batchExecutionId = parsePositiveRouteQueryId(queryParams.batchExecutionId) || undefined
}

async function getList() {
  syncExternalContext()
  normalizeQueryParams()
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrRecordChangePage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      changeType: EDHR_FORM_TRACE_CHANGE_TYPE,
      targetScope: queryParams.targetScope || undefined,
      batchExecutionId: queryParams.batchExecutionId,
      executionId: queryParams.executionId,
      changeStatus: queryParams.changeStatus || undefined
    })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '电子批记录变更记录加载失败。')
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
  queryParams.pageSize = props.pageSize
  queryParams.changeType = EDHR_FORM_TRACE_CHANGE_TYPE
  queryParams.targetScope = undefined
  queryParams.batchExecutionId = undefined
  queryParams.executionId = undefined
  queryParams.changeStatus = undefined
  await changeQuickFilter.resetQuickFilter()
}

const openDetail = async (row: EdhrRecordChangeRespVO) => {
  if (!row.id) {
    message.error('当前变更记录缺少ID，无法查看详情。')
    return
  }
  try {
    selectedChange.value = await getEdhrRecordChange(row.id)
    detailDialogVisible.value = true
    detailActiveTab.value = 'recordForm'
    detailEvidenceNames.value = []
  } catch (error) {
    message.error(resolveErrorMessage(error, '电子批记录变更详情加载失败。'))
  }
}

const openBatchTrace = (row: EdhrRecordChangeRespVO) => {
  traceContext.value = {
    batchExecutionId: row.batchExecutionId,
    executionId: row.executionId,
    sourceTab: 'change'
  }
  traceDrawerVisible.value = true
}

const openSelectedChangeRecordForm = () => {
  if (!selectedChange.value) {
    message.error('当前变更记录未加载，无法打开批记录表单。')
    return
  }
  if (!canOpenBatchExecution(selectedChange.value)) {
    message.error('当前变更记录缺少批次执行 ID，无法打开批记录表单。')
    return
  }
  openBatchTrace(selectedChange.value)
}

const openBatchExecution = async (row?: Pick<EdhrRecordChangeRespVO, 'batchExecutionId'>) => {
  if (!canOpenBatchExecution(row)) {
    message.error('当前变更记录缺少批次执行 ID，无法打开批次详情。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-execution/detail',
    query: { id: String(row!.batchExecutionId) }
  })
}

const openExecution = async (row?: Pick<EdhrRecordChangeRespVO, 'executionId'>) => {
  if (!canOpenExecution(row)) {
    message.error('当前变更记录缺少执行 ID，无法打开执行表单。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-execution/form',
    query: { id: String(row!.executionId) }
  })
}

watch(
  () => [props.batchExecutionId, props.executionId, props.targetScope, props.autoLoad, props.pageSize],
  async () => {
    queryParams.pageSize = props.pageSize
    syncExternalContext()
    if (props.autoLoad) {
      queryParams.pageNo = 1
      await getList()
    }
  },
  { immediate: true }
)

defineExpose({ reload: getList })
</script>

<style scoped>
.edhr-form-trace-change {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-form-trace-change__alert {
  margin-bottom: 12px;
}

.edhr-form-trace-change :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-form-trace-change :deep(.el-table__row) {
  height: 52px;
}

.edhr-form-trace-change__evidence {
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.edhr-form-trace-change__evidence-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-form-trace-change__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.edhr-form-trace-change__evidence-item,
.edhr-form-trace-change__detail-summary-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #ffffff;
}

.edhr-form-trace-change__evidence-item span,
.edhr-form-trace-change__detail-summary-item span {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-form-trace-change__evidence-item strong,
.edhr-form-trace-change__detail-summary-item strong {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.edhr-form-trace-change__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-form-trace-change__object-link {
  height: auto;
  padding: 0;
  font-size: 12px;
  font-weight: 600;
  vertical-align: baseline;
}

.edhr-form-trace-change__object-link--disabled {
  color: #8a94a6;
}

.edhr-form-trace-change__detail-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.edhr-form-trace-change__detail-tabs {
  min-height: 0;
}

.edhr-form-trace-change__record-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.edhr-form-trace-change__record-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.edhr-form-trace-change__record-card-title {
  margin-bottom: 4px;
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.edhr-form-trace-change__record-alert {
  margin-top: 2px;
}

.edhr-form-trace-change__evidence-collapse {
  margin-top: 12px;
}

.edhr-form-trace-change__detail {
  margin-top: 4px;
}
</style>
