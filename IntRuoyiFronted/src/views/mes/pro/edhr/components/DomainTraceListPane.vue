<template>
  <div class="edhr-domain-trace-pane">
    <UnifiedListTemplate
      table-key="mes.pro.edhr.traceDrawer.domainTrace"
      :query-model="queryParams"
      :filter-definitions="domainTraceQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="domainTraceQuickFilter.state"
      :selected-filter-definition="domainTraceQuickFilter.selectedDefinition.value"
      :operator-options="domainTraceQuickFilter.operatorOptions.value"
      :columns="domainTraceColumns"
      :column-saving="domainTraceColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="domainTraceQuickFilter.updateState"
      @quick-filter-query="domainTraceQuickFilter.applyQuickFilter"
      @column-change="saveDomainTraceColumnConfig"
      @pagination="getList"
    >
      <template #extra-filters>
        <el-form-item label="校验时间">
          <el-date-picker
            v-model="verifiedAtRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="!w-360px"
          />
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
          class="edhr-domain-trace-pane__alert"
        />

        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.edhr.traceDrawer.domainTrace"
          :data="list"
          border
          stripe
          row-key="executionId"
          :show-overflow-tooltip="true"
          empty-text="暂无主数据追溯记录"
          @header-dragend="handleDomainTraceHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isDomainTraceColumnVisible('evidenceExpand')"
            type="expand"
            :width="getDomainTraceColumnWidthString('evidenceExpand', 44)"
          >
            <template #default="{ row }">
              <div class="edhr-domain-trace-pane__evidence">
                <div class="edhr-domain-trace-pane__evidence-title">追溯证据</div>
                <div class="edhr-domain-trace-pane__evidence-grid">
                  <div class="edhr-domain-trace-pane__evidence-item">
                    <span>追溯哈希</span>
                    <strong>{{ row.domainTraceHash || '--' }}</strong>
                  </div>
                  <div class="edhr-domain-trace-pane__evidence-item">
                    <span>快照编号</span>
                    <strong>{{ row.domainTraceSnapshotId || '--' }}</strong>
                  </div>
                  <div class="edhr-domain-trace-pane__evidence-item">
                    <span>首项对象</span>
                    <strong>{{ resolveFirstItemObject(row) }}</strong>
                  </div>
                  <div class="edhr-domain-trace-pane__evidence-item">
                    <span>首项来源</span>
                    <strong>{{ resolveFirstItemSource(row) }}</strong>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isDomainTraceColumnVisible('traceSummary')"
            label="追溯概况"
            prop="traceSummary"
            :min-width="getDomainTraceColumnMinWidthString('traceSummary', 260)"
            v-bind="sortColumnAttrs({ key: 'traceSummary', prop: 'executionCode' })"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">
                {{ row.executionCode || row.executionId || '--' }}
              </el-button>
              <div class="edhr-domain-trace-pane__muted">
                {{ row.workOrderCode || '未关联工单' }} / {{ row.batchCode || '未关联批次' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isDomainTraceColumnVisible('status')"
            label="追溯状态"
            prop="status"
            :width="getDomainTraceColumnWidthString('status', 120)"
            align="center"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="{ row }">
              <el-tag :type="resolveTraceStatusType(row.status, row)">
                {{ resolveTraceStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isDomainTraceColumnVisible('blockerSummary')"
            label="阻塞摘要"
            prop="blockerSummary"
            :min-width="getDomainTraceColumnMinWidthString('blockerSummary', 280)"
            v-bind="sortColumnAttrs('blockerSummary')"
          >
            <template #default="{ row }">
              <div :class="{ 'edhr-domain-trace-pane__danger': rowHasBlockers(row) }">
                {{ resolveBlockerSummary(row) }}
              </div>
              <div class="edhr-domain-trace-pane__muted">{{ resolveBlockerCount(row) }} 项阻塞</div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isDomainTraceColumnVisible('itemCount')"
            label="追溯项"
            prop="itemCount"
            :width="getDomainTraceColumnWidthString('itemCount', 110)"
            align="center"
            v-bind="sortColumnAttrs('itemCount')"
          >
            <template #default="{ row }">
              <el-tag type="info">{{ resolveItemCount(row) }} 项</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isDomainTraceColumnVisible('verifiedAt')"
            label="最近校验"
            prop="verifiedAt"
            :width="getDomainTraceColumnWidthString('verifiedAt', 180)"
            v-bind="sortColumnAttrs('verifiedAt')"
          >
            <template #default="{ row }">
              {{ formatEdhrDateTime(row.verifiedAt, '未校验') }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isDomainTraceColumnVisible('actions')"
            label="操作"
            prop="actions"
            :width="getDomainTraceColumnWidthString('actions', 100)"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </div>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  EDHR_DOMAIN_TRACE_QUERY_PERMISSION,
  EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP,
  EDHR_DOMAIN_TRACE_STATUS_TAG_TYPE_MAP,
  getEdhrDomainTracePage,
  type EdhrDomainTracePageReqVO,
  type EdhrDomainTracePageRowVO,
  type EdhrDomainTraceStatus
} from '@/api/mes/pro/edhr/domainTrace'
import { hasPermission } from '@/directives/permission/hasPermi'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProEdhrDomainTraceListPane' })

const props = withDefaults(
  defineProps<{
    executionId?: string | number
    executionCode?: string
    workOrderCode?: string
    batchCode?: string
    autoLoad?: boolean
    pageSize?: number
  }>(),
  {
    autoLoad: true,
    pageSize: 10
  }
)

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrDomainTracePageRowVO[]>([])
const total = ref(0)
const verifiedAtRange = ref<string[]>([])

const statusOptions: Array<{ label: string; value: EdhrDomainTraceStatus }> = [
  { label: EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP.VERIFIED, value: 'VERIFIED' },
  { label: EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP.BLOCKED, value: 'BLOCKED' },
  { label: EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP.UNVERIFIED, value: 'UNVERIFIED' }
]

const parsePositiveNumber = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  const parsedValue = typeof rawValue === 'string' || typeof rawValue === 'number' ? Number(rawValue) : NaN
  return Number.isFinite(parsedValue) && parsedValue > 0 ? parsedValue : undefined
}

const normalizeText = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return rawValue == null ? '' : String(rawValue).trim()
}

const queryParams = reactive<EdhrDomainTracePageReqVO>({
  pageNo: 1,
  pageSize: props.pageSize,
  executionId: parsePositiveNumber(props.executionId),
  executionCode: normalizeText(props.executionCode),
  workOrderCode: normalizeText(props.workOrderCode),
  batchCode: normalizeText(props.batchCode),
  status: undefined
})

const domainTraceDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'evidenceExpand', label: '追溯证据', width: 44, hideable: false, business: false },
  { key: 'traceSummary', label: '追溯概况', minWidth: 260 },
  { key: 'status', label: '追溯状态', width: 120 },
  { key: 'blockerSummary', label: '阻塞摘要', minWidth: 280 },
  { key: 'itemCount', label: '追溯项', width: 110 },
  { key: 'verifiedAt', label: '最近校验', width: 180 },
  { key: 'actions', label: '操作', width: 100, hideable: false, business: false, sortable: false }
]

const {
  columns: domainTraceColumns,
  saving: domainTraceColumnSaving,
  isColumnVisible: isDomainTraceColumnVisible,
  getColumnWidthString: getDomainTraceColumnWidthString,
  getColumnMinWidthString: getDomainTraceColumnMinWidthString,
  handleHeaderDragend: handleDomainTraceHeaderDragend,
  saveConfig: saveDomainTraceColumnConfig
} = useUserTableColumns('mes.pro.edhr.traceDrawer.domainTrace', domainTraceDefaultColumns)

const domainTraceQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'executionCode', label: '执行编号', type: 'text', queryParamKey: 'executionCode', placeholder: '请输入执行编号' },
  { key: 'workOrderCode', label: '工单号', type: 'text', queryParamKey: 'workOrderCode', placeholder: '请输入工单号' },
  { key: 'batchCode', label: '批次号', type: 'text', queryParamKey: 'batchCode', placeholder: '请输入批次号' },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: statusOptions
  }
]

const domainTraceQuickFilter = useTableQuickFilter(
  'mes.pro.edhr.traceDrawer.domainTrace',
  domainTraceQuickFilterDefinitions,
  queryParams,
  getList
)

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const syncExternalContext = () => {
  if (props.executionId !== undefined) {
    queryParams.executionId = parsePositiveNumber(props.executionId)
  }
  if (props.executionCode !== undefined) {
    queryParams.executionCode = normalizeText(props.executionCode)
  }
  if (props.workOrderCode !== undefined) {
    queryParams.workOrderCode = normalizeText(props.workOrderCode)
  }
  if (props.batchCode !== undefined) {
    queryParams.batchCode = normalizeText(props.batchCode)
  }
}

const buildQuery = (): EdhrDomainTracePageReqVO => {
  syncExternalContext()
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    executionId: parsePositiveNumber(queryParams.executionId),
    executionCode: queryParams.executionCode?.trim() || undefined,
    workOrderCode: queryParams.workOrderCode?.trim() || undefined,
    batchCode: queryParams.batchCode?.trim() || undefined,
    status: queryParams.status,
    verifiedAtStart: verifiedAtRange.value[0] || undefined,
    verifiedAtEnd: verifiedAtRange.value[1] || undefined
  }
}

const rowHasBlockers = (row: EdhrDomainTracePageRowVO) => {
  return row.status === 'BLOCKED' || Number(row.blockerCount || 0) > 0 || Boolean(row.blockers?.length)
}

const resolveBlockerCount = (row: EdhrDomainTracePageRowVO) => {
  return row.blockerCount ?? row.blockers?.length ?? 0
}

const resolveItemCount = (row: EdhrDomainTracePageRowVO) => {
  return row.itemCount ?? row.items?.length ?? 0
}

const resolveTraceStatusLabel = (status?: string) => {
  return status && status in EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP
    ? EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP[status as EdhrDomainTraceStatus]
    : status || '未知'
}

const resolveTraceStatusType = (status?: string, row?: EdhrDomainTracePageRowVO) => {
  if (row && rowHasBlockers(row)) return 'danger'
  return status && status in EDHR_DOMAIN_TRACE_STATUS_TAG_TYPE_MAP
    ? EDHR_DOMAIN_TRACE_STATUS_TAG_TYPE_MAP[status as EdhrDomainTraceStatus]
    : 'warning'
}

const resolveBlockerSummary = (row: EdhrDomainTracePageRowVO) => {
  const firstBlocker = row.blockers?.[0]
  if (firstBlocker?.blockerMessage) {
    return [firstBlocker.itemType, firstBlocker.itemKey, firstBlocker.blockerMessage]
      .filter(Boolean)
      .join(' / ')
  }
  if (firstBlocker?.blockerCode) {
    return [firstBlocker.itemType, firstBlocker.itemKey, firstBlocker.blockerCode]
      .filter(Boolean)
      .join(' / ')
  }
  if (row.status === 'BLOCKED') return '主数据追溯已阻塞，后端未返回阻塞明细。'
  return '无阻塞项'
}

const resolveFirstItemObject = (row: EdhrDomainTracePageRowVO) => {
  const firstItem = row.items?.[0]
  if (!firstItem) return '--'
  return [firstItem.itemType, firstItem.itemKey, firstItem.itemName].filter(Boolean).join(' / ')
}

const resolveFirstItemSource = (row: EdhrDomainTracePageRowVO) => {
  const firstItem = row.items?.[0]
  if (!firstItem) return '--'
  return [firstItem.sourceCode || firstItem.sourceId, firstItem.sourceVersion, firstItem.blockerReason]
    .filter(Boolean)
    .join(' / ')
}

async function getList() {
  if (!hasPermission([EDHR_DOMAIN_TRACE_QUERY_PERMISSION])) {
    list.value = []
    total.value = 0
    loadError.value = '当前账号没有主数据追溯查询权限。'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const pageData = await getEdhrDomainTracePage(buildQuery())
    list.value = pageData.list || []
    total.value = pageData.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '主数据追溯列表加载失败，请联系管理员。')
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
  queryParams.executionId = undefined
  queryParams.executionCode = ''
  queryParams.workOrderCode = ''
  queryParams.batchCode = ''
  queryParams.status = undefined
  verifiedAtRange.value = []
  await domainTraceQuickFilter.resetQuickFilter()
}

const openDetail = async (row: EdhrDomainTracePageRowVO) => {
  if (!row.executionId) {
    message.error('当前追溯记录缺少执行ID，无法查看详情。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-domain-trace/detail',
    query: {
      executionId: String(row.executionId)
    }
  })
}

watch(
  () => [props.executionId, props.executionCode, props.workOrderCode, props.batchCode, props.autoLoad, props.pageSize],
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
.edhr-domain-trace-pane {
  min-height: 0;
}

.edhr-domain-trace-pane__alert {
  margin: 12px;
}

.edhr-domain-trace-pane :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-domain-trace-pane :deep(.el-table__row) {
  height: 52px;
}

.edhr-domain-trace-pane__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-domain-trace-pane__danger {
  color: #c73636;
  font-weight: 600;
}

.edhr-domain-trace-pane__evidence {
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.edhr-domain-trace-pane__evidence-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-domain-trace-pane__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.edhr-domain-trace-pane__evidence-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #ffffff;
}

.edhr-domain-trace-pane__evidence-item span {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-domain-trace-pane__evidence-item strong {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
</style>
