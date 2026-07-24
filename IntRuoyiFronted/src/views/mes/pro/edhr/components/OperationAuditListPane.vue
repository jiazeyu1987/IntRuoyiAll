<template>
  <div class="edhr-operation-audit-pane">
    <UnifiedListTemplate
      table-key="mes.pro.edhr.operationAudit"
      :query-model="queryParams"
      :filter-definitions="operationAuditQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="operationAuditQuickFilter.state"
      :selected-filter-definition="operationAuditQuickFilter.selectedDefinition.value"
      :operator-options="operationAuditQuickFilter.operatorOptions.value"
      :columns="operationAuditColumns"
      :column-saving="operationAuditColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="operationAuditQuickFilter.updateState"
      @quick-filter-query="operationAuditQuickFilter.applyQuickFilter"
      @column-change="saveOperationAuditColumnConfig"
      @pagination="getList"
    >
      <template #extra-filters>
        <el-form-item v-if="showObjectFilters" label="对象类型" required>
          <el-input v-model="queryParams.objectType" clearable class="!w-160px" />
        </el-form-item>
        <el-form-item v-if="showObjectFilters" label="对象ID" required>
          <el-input v-model="queryParams.objectId" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="发生时间">
          <el-date-picker
            v-model="occurredAtRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="!w-360px"
          />
        </el-form-item>
        <el-form-item class="edhr-operation-audit-pane__advanced">
          <el-collapse v-model="operationAuditAdvancedFilterNames">
            <el-collapse-item title="高级筛选" name="advanced">
              <div class="edhr-operation-audit-pane__advanced-grid">
                <el-form-item label="执行ID">
                  <el-input
                    v-model="queryParams.executionId"
                    clearable
                    class="!w-140px"
                  />
                </el-form-item>
                <el-form-item label="批次ID">
                  <el-input
                    v-model="queryParams.batchExecutionId"
                    clearable
                    class="!w-140px"
                  />
                </el-form-item>
                <el-form-item label="操作者ID">
                  <el-input-number
                    v-model="queryParams.actorUserId"
                    :min="1"
                    :controls="false"
                    class="!w-140px"
                  />
                </el-form-item>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-form-item>
      </template>

      <template #actions>
        <el-button :loading="loading" type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-alert
          v-if="loadError"
          :title="loadError"
          type="error"
          :closable="false"
          show-icon
          class="edhr-operation-audit-pane__alert"
        />

        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.edhr.operationAudit"
          :data="list"
          border
          stripe
          row-key="id"
          :show-overflow-tooltip="true"
          empty-text="暂无操作审计记录，请输入对象类型和对象ID后查询"
          @header-dragend="handleOperationAuditHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isOperationAuditColumnVisible('id')"
            label="事件ID"
            prop="id"
            :width="getOperationAuditColumnWidthString('id', 110)"
            v-bind="sortColumnAttrs('id')"
          />
          <el-table-column
            v-if="isOperationAuditColumnVisible('object')"
            label="对象"
            prop="object"
            :min-width="getOperationAuditColumnMinWidthString('object', 220)"
            v-bind="sortColumnAttrs({ key: 'object', prop: 'objectType' })"
          >
            <template #default="{ row }">
              <div class="edhr-operation-audit-pane__strong">
                {{ resolveOperationAuditObjectTypeLabel(row.objectType) }}（ID：{{ row.objectId || '--' }}）
              </div>
              <div class="edhr-operation-audit-pane__muted">执行ID：{{ row.executionId || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isOperationAuditColumnVisible('action')"
            label="动作"
            prop="action"
            :min-width="getOperationAuditColumnMinWidthString('action', 180)"
            v-bind="sortColumnAttrs({ key: 'action', prop: 'operationType' })"
          >
            <template #default="{ row }">
              <div>{{ resolveOperationActionLabel(row) }}</div>
              <div class="edhr-operation-audit-pane__muted">动作类型：{{ resolveOperationTypeLabel(row.operationType) }}</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isOperationAuditColumnVisible('resultStatus')"
            label="结果"
            prop="resultStatus"
            :width="getOperationAuditColumnWidthString('resultStatus', 110)"
            v-bind="sortColumnAttrs('resultStatus')"
          >
            <template #default="{ row }">
              <el-tag :type="resolveResultTagType(row.resultStatus)">
                {{ resolveResultStatusLabel(row.resultStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isOperationAuditColumnVisible('permissionDecision')"
            label="权限决策"
            prop="permissionDecision"
            :width="getOperationAuditColumnWidthString('permissionDecision', 120)"
            v-bind="sortColumnAttrs('permissionDecision')"
          >
            <template #default="{ row }">
              <el-tag :type="resolvePermissionDecisionTagType(row.permissionDecision)">
                {{ resolvePermissionDecisionLabel(row.permissionDecision) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isOperationAuditColumnVisible('actor')"
            label="操作者"
            prop="actor"
            :min-width="getOperationAuditColumnMinWidthString('actor', 150)"
            v-bind="sortColumnAttrs({ key: 'actor', prop: 'actorUserId' })"
          >
            <template #default="{ row }">
              {{ row.actorUsername || row.actorUserId || '--' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isOperationAuditColumnVisible('occurredAt')"
            label="发生时间"
            prop="occurredAt"
            :width="getOperationAuditColumnWidthString('occurredAt', 180)"
            v-bind="sortColumnAttrs('occurredAt')"
          />
          <el-table-column
            v-if="isOperationAuditColumnVisible('operation')"
            label="操作"
            prop="operation"
            :width="getOperationAuditColumnWidthString('operation', 90)"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>

    <Dialog title="操作审计详情" v-model="detailVisible" width="860px">
      <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" show-icon />
      <div v-else-if="detail" class="edhr-operation-audit-pane__detail">
        <el-descriptions title="事件摘要" :column="2" border>
          <el-descriptions-item label="事件ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="请求ID">{{ detail.requestId || '--' }}</el-descriptions-item>
          <el-descriptions-item label="对象">
            {{ resolveOperationAuditObjectTypeLabel(detail.objectType) }}（ID：{{ detail.objectId || '--' }}）
          </el-descriptions-item>
          <el-descriptions-item label="动作">
            {{ resolveOperationActionLabel(detail) }}
          </el-descriptions-item>
          <el-descriptions-item label="结果">
            <el-tag :type="resolveResultTagType(detail.resultStatus)">
              {{ resolveResultStatusLabel(detail.resultStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="权限决策">
            <el-tag :type="resolvePermissionDecisionTagType(detail.permissionDecision)">
              {{ resolvePermissionDecisionLabel(detail.permissionDecision) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作者">
            {{ detail.actorUsername || detail.actorUserId || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="发生时间">{{ detail.occurredAt || '--' }}</el-descriptions-item>
          <el-descriptions-item label="失败码">{{ detail.failureCode || '--' }}</el-descriptions-item>
          <el-descriptions-item label="失败说明">{{ detail.failureMessage || '--' }}</el-descriptions-item>
        </el-descriptions>

        <el-collapse
          v-model="operationAuditDetailTechnicalEvidenceNames"
          class="edhr-operation-audit-pane__technical-evidence"
        >
          <el-collapse-item title="技术证据" name="technical-evidence">
            <div class="edhr-operation-audit-pane__technical-title">审计证据</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="变更前摘要哈希">
                {{ detail.beforeSummaryHash || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="变更后摘要哈希">
                {{ detail.afterSummaryHash || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="前序审计哈希">
                {{ detail.previousAuditHash || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="当前审计哈希">
                {{ detail.auditHash || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="元数据" :span="2">
                <pre class="edhr-operation-audit-pane__metadata">{{ detail.metadataJson || '--' }}</pre>
              </el-descriptions-item>
            </el-descriptions>
          </el-collapse-item>
        </el-collapse>
      </div>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  EdhrOperationAuditApi,
  type EdhrOperationAuditPermissionDecision,
  type EdhrOperationAuditRespVO,
  type EdhrOperationAuditResultStatus
} from '@/api/mes/pro/edhr/operationAudit'
import type { EdhrRecordCategory } from '@/api/mes/pro/edhr/batchExecution'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  OPERATION_AUDIT_PERMISSION_DECISION_OPTIONS as permissionDecisionOptions,
  OPERATION_AUDIT_RESULT_OPTIONS as operationResultOptions,
  resolveOperationActionLabel,
  resolveOperationAuditObjectTypeLabel,
  resolveOperationAuditPermissionDecisionLabel as resolvePermissionDecisionLabel,
  resolveOperationAuditPermissionDecisionTagType as resolvePermissionDecisionTagType,
  resolveOperationAuditResultStatusLabel as resolveResultStatusLabel,
  resolveOperationAuditResultTagType as resolveResultTagType,
  resolveOperationTypeLabel
} from '@/views/mes/pro/edhr/shared/releaseCheckPresentation'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'MesProEdhrOperationAuditListPane' })

const props = withDefaults(
  defineProps<{
    objectType?: string
    objectId?: string | number
    batchExecutionId?: string | number
    showObjectFilters?: boolean
    autoLoad?: boolean
    pageSize?: number
  }>(),
  {
    showObjectFilters: true,
    autoLoad: true,
    pageSize: 10
  }
)

const route = useRoute()
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrOperationAuditRespVO[]>([])
const total = ref(0)
const occurredAtRange = ref<string[]>([])
const operationAuditAdvancedFilterNames = ref<string[]>([])
const operationAuditDetailTechnicalEvidenceNames = ref<string[]>([])
const detailVisible = ref(false)
const detail = ref<EdhrOperationAuditRespVO>()
const detailError = ref('')

const recordCategoryOptions: Array<{ label: string; value: EdhrRecordCategory }> = [
  { label: '批记录表', value: 'BATCH_RECORD' },
  { label: '内部记录表', value: 'INTERNAL_RECORD' }
]

const parsePositiveQueryNumber = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  const parsed = Number(rawValue)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const parseQueryText = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' ? rawValue.trim() : ''
}

const normalizeText = (value: unknown) => (value == null ? '' : String(value).trim())

const queryParams = reactive({
  pageNo: 1,
  pageSize: props.pageSize,
  objectType: normalizeText(props.objectType) || parseQueryText(route.query.objectType),
  objectId: normalizeText(props.objectId) || parseQueryText(route.query.objectId),
  executionId: parsePositiveRouteQueryId(route.query.executionId) || undefined,
  batchExecutionId: parsePositiveRouteQueryId(props.batchExecutionId ?? route.query.batchExecutionId) || undefined,
  workTaskId: parsePositiveRouteQueryId(route.query.workTaskId) || undefined,
  routeId: parsePositiveRouteQueryId(route.query.routeId) || undefined,
  routeProcessId: parsePositiveRouteQueryId(route.query.routeProcessId) || undefined,
  reportId: parseQueryText(route.query.reportId),
  recordCategory: undefined as EdhrRecordCategory | undefined,
  operationType: '',
  permissionDecision: undefined as EdhrOperationAuditPermissionDecision | undefined,
  resultStatus: undefined as EdhrOperationAuditResultStatus | undefined,
  actorUserId: undefined as number | undefined
})

const operationAuditDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'id', label: '事件ID', width: 110 },
  { key: 'object', label: '对象', minWidth: 220 },
  { key: 'action', label: '动作', minWidth: 180 },
  { key: 'resultStatus', label: '结果', width: 110 },
  { key: 'permissionDecision', label: '权限决策', width: 120 },
  { key: 'actor', label: '操作者', minWidth: 150 },
  { key: 'occurredAt', label: '发生时间', width: 180 },
  { key: 'operation', label: '操作', width: 90, hideable: false, business: false, sortable: false }
]

const {
  columns: operationAuditColumns,
  saving: operationAuditColumnSaving,
  isColumnVisible: isOperationAuditColumnVisible,
  getColumnWidthString: getOperationAuditColumnWidthString,
  getColumnMinWidthString: getOperationAuditColumnMinWidthString,
  handleHeaderDragend: handleOperationAuditHeaderDragend,
  saveConfig: saveOperationAuditColumnConfig
} = useUserTableColumns('mes.pro.edhr.operationAudit', operationAuditDefaultColumns)

const operationAuditQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'operationType', label: '动作类型', type: 'text', queryParamKey: 'operationType', placeholder: '请输入动作类型' },
  {
    key: 'permissionDecision',
    label: '权限决策',
    type: 'select',
    queryParamKey: 'permissionDecision',
    options: permissionDecisionOptions
  },
  {
    key: 'resultStatus',
    label: '操作结果',
    type: 'select',
    queryParamKey: 'resultStatus',
    options: operationResultOptions
  },
  {
    key: 'recordCategory',
    label: '记录类型',
    type: 'select',
    queryParamKey: 'recordCategory',
    options: recordCategoryOptions
  },
  { key: 'actorUserId', label: '操作者ID', type: 'text', queryParamKey: 'actorUserId', placeholder: '请输入操作者ID' }
]

const operationAuditQuickFilter = useTableQuickFilter(
  'mes.pro.edhr.operationAudit',
  operationAuditQuickFilterDefinitions,
  queryParams,
  getList
)

const hasObjectContext = computed(() => Boolean(queryParams.objectType.trim() && queryParams.objectId.trim()))
const hasBatchContext = computed(() => Boolean(parsePositiveRouteQueryId(queryParams.batchExecutionId)))
const shouldQueryBatchContextOnly = computed(() => !props.showObjectFilters && hasBatchContext.value)

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const syncExternalContext = () => {
  const objectType = normalizeText(props.objectType)
  const objectId = normalizeText(props.objectId)
  const batchExecutionId = parsePositiveRouteQueryId(props.batchExecutionId)
  if (objectType) queryParams.objectType = objectType
  if (objectId) queryParams.objectId = objectId
  if (batchExecutionId) queryParams.batchExecutionId = batchExecutionId
}

const assertObjectContext = () => {
  if (!hasObjectContext.value && !shouldQueryBatchContextOnly.value) {
    throw new Error('对象类型和对象ID不能为空，不能查询对象级电子批记录操作审计。')
  }
}

const buildQuery = () => {
  assertObjectContext()
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    objectType: shouldQueryBatchContextOnly.value ? undefined : queryParams.objectType.trim(),
    objectId: shouldQueryBatchContextOnly.value ? undefined : queryParams.objectId.trim(),
    batchExecutionId: parsePositiveRouteQueryId(queryParams.batchExecutionId) || undefined,
    executionId: parsePositiveRouteQueryId(queryParams.executionId) || undefined,
    workTaskId: parsePositiveRouteQueryId(queryParams.workTaskId) || undefined,
    routeId: parsePositiveRouteQueryId(queryParams.routeId) || undefined,
    routeProcessId: parsePositiveRouteQueryId(queryParams.routeProcessId) || undefined,
    reportId: queryParams.reportId.trim() || undefined,
    recordCategory: queryParams.recordCategory,
    operationType: queryParams.operationType.trim() || undefined,
    actorUserId: parsePositiveQueryNumber(queryParams.actorUserId),
    permissionDecision: queryParams.permissionDecision,
    resultStatus: queryParams.resultStatus,
    occurredAt: occurredAtRange.value.length ? occurredAtRange.value : undefined
  }
}

async function getList() {
  syncExternalContext()
  loading.value = true
  loadError.value = ''
  try {
    const data = await EdhrOperationAuditApi.getPage(buildQuery())
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '电子批记录操作审计加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const openDetail = async (id: number) => {
  detailVisible.value = true
  detail.value = undefined
  detailError.value = ''
  operationAuditDetailTechnicalEvidenceNames.value = []
  try {
    detail.value = await EdhrOperationAuditApi.get(id)
  } catch (error) {
    detailError.value = resolveErrorMessage(error, '电子批记录操作审计详情加载失败，请联系管理员。')
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = async () => {
  queryParams.pageNo = 1
  queryParams.pageSize = props.pageSize
  queryParams.executionId = undefined
  queryParams.batchExecutionId = undefined
  queryParams.workTaskId = undefined
  queryParams.routeId = undefined
  queryParams.routeProcessId = undefined
  queryParams.reportId = ''
  queryParams.recordCategory = undefined
  queryParams.operationType = ''
  queryParams.permissionDecision = undefined
  queryParams.resultStatus = undefined
  queryParams.actorUserId = undefined
  occurredAtRange.value = []
  syncExternalContext()
  if (props.showObjectFilters) {
    queryParams.objectType = ''
    queryParams.objectId = ''
    list.value = []
    total.value = 0
    loadError.value = ''
    operationAuditQuickFilter.updateState({
      fieldKey: operationAuditQuickFilterDefinitions[0]?.key,
      operator: 'contains',
      value: undefined
    })
    return
  }
  await operationAuditQuickFilter.resetQuickFilter()
}

watch(
  () => [props.objectType, props.objectId, props.batchExecutionId, props.autoLoad],
  async () => {
    syncExternalContext()
    if (props.autoLoad && hasObjectContext.value) {
      queryParams.pageNo = 1
      await getList()
    }
  },
  { immediate: true }
)

defineExpose({ reload: getList })
</script>

<style scoped>
.edhr-operation-audit-pane {
  min-height: 0;
}

.edhr-operation-audit-pane__alert {
  margin: 12px;
}

.edhr-operation-audit-pane__advanced {
  display: block;
  width: 100%;
  margin-right: 0;
}

.edhr-operation-audit-pane__advanced :deep(.el-form-item__content) {
  width: 100%;
}

.edhr-operation-audit-pane__advanced :deep(.el-collapse) {
  width: 100%;
  border-top: 1px solid #edf1f6;
  border-bottom: 0;
}

.edhr-operation-audit-pane__advanced :deep(.el-collapse-item__header) {
  min-height: 40px;
  color: #172033;
  font-weight: 600;
}

.edhr-operation-audit-pane__advanced-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, max-content));
  gap: 0 12px;
}

.edhr-operation-audit-pane :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-operation-audit-pane :deep(.el-table__row) {
  height: 52px;
}

.edhr-operation-audit-pane__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-operation-audit-pane__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-operation-audit-pane__detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-operation-audit-pane__technical-evidence {
  padding: 0 16px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-operation-audit-pane__technical-evidence :deep(.el-collapse-item__header) {
  min-height: 48px;
  color: #172033;
  font-weight: 600;
}

.edhr-operation-audit-pane__technical-evidence :deep(.el-collapse-item__wrap) {
  border-bottom: 0;
}

.edhr-operation-audit-pane__technical-evidence :deep(.el-collapse-item__content) {
  padding-bottom: 4px;
}

.edhr-operation-audit-pane__technical-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-operation-audit-pane__metadata {
  max-height: 180px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
