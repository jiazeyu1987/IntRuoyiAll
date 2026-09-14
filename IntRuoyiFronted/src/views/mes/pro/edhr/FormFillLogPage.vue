<template>
  <ContentWrap v-hasPermi="['mes:pro-edhr-form-fill-log:query']">
    <el-tabs
      v-model="activeLogSource"
      class="edhr-form-fill-log-page__source-tabs"
      data-edhr-form-log-source-tabs
    >
      <el-tab-pane label="表单填写日志" name="FORM_FILL">
    <UnifiedListTemplate
      class="edhr-form-fill-log-page"
      table-key="mes.pro.edhr.formFillLog.main"
      :query-model="queryParams"
      :filter-definitions="formFillLogQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="formFillLogQuickFilter.state"
      :selected-filter-definition="formFillLogQuickFilter.selectedDefinition.value"
      :operator-options="formFillLogQuickFilter.operatorOptions.value"
      :columns="formFillLogColumns"
      :column-saving="formFillLogColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="formFillLogQuickFilter.updateState"
      @quick-filter-query="formFillLogQuickFilter.applyQuickFilter"
      @column-change="saveFormFillLogColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button :loading="loading" type="primary" @click="getList">
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
          class="edhr-form-fill-log-page__alert"
        />
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.edhr.formFillLog.main"
          :data="list"
          border
          stripe
          row-key="auditBatchId"
          :show-overflow-tooltip="true"
          @header-dragend="handleHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isColumnVisible('formName')"
            label="表单名称"
            prop="formName"
            :min-width="getColumnMinWidthString('formName', 180)"
            v-bind="sortColumnAttrs('formName')"
          >
            <template #default="{ row }">{{ row.formName || row.batchRecordReportId || '--' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('batchCode')"
            label="批号"
            prop="batchCode"
            :min-width="getColumnMinWidthString('batchCode', 150)"
            v-bind="sortColumnAttrs('batchCode')"
          >
            <template #default="{ row }">
              <el-button
                v-if="row.batchExecutionId && row.batchCode"
                link
                type="primary"
                @click="openBatchExecutionDetail(row)"
              >
                {{ row.batchCode }}
              </el-button>
              <span v-else>
                {{ row.batchCode || '--' }}
                <el-tag v-if="row.contextStatus !== 'COMPLETE'" class="ml-6px" type="warning" size="small">
                  批次上下文缺失
                </el-tag>
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('workOrderCode')"
            label="生产工单号"
            prop="workOrderCode"
            :min-width="getColumnMinWidthString('workOrderCode', 160)"
            v-bind="sortColumnAttrs('workOrderCode')"
          >
            <template #default="{ row }">
              <el-button
                v-if="row.batchExecutionId && row.workOrderCode"
                link
                type="primary"
                @click="openBatchExecutionWorkOrder(row)"
              >
                {{ row.workOrderCode }}
              </el-button>
              <span v-else>
                {{ row.workOrderCode || '--' }}
                <el-tag v-if="row.contextStatus !== 'COMPLETE'" class="ml-6px" type="warning" size="small">
                  批次上下文缺失
                </el-tag>
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('executionCode')"
            label="执行编号"
            prop="executionCode"
            :min-width="getColumnMinWidthString('executionCode', 160)"
            v-bind="sortColumnAttrs('executionCode')"
          />
          <el-table-column
            v-if="isColumnVisible('actorName')"
            label="填写人"
            prop="actorName"
            :min-width="getColumnMinWidthString('actorName', 120)"
            v-bind="sortColumnAttrs('actorName')"
          >
            <template #default="{ row }">{{ row.actorName || row.actorId || '--' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('changedAt')"
            label="填写时间"
            prop="changedAt"
            :width="getColumnWidthString('changedAt', 210)"
            v-bind="sortColumnAttrs('changedAt')"
          >
            <template #default="{ row }">{{ formatFormLogDateTime(row.changedAt) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('fieldCount')"
            label="填写单元数"
            prop="fieldCount"
            :width="getColumnWidthString('fieldCount', 120)"
            align="center"
            v-bind="sortColumnAttrs('fieldCount')"
          />
          <el-table-column
            v-if="isColumnVisible('cellSummary')"
            label="写入摘要"
            prop="cellSummary"
            :min-width="getColumnMinWidthString('cellSummary', 260)"
            v-bind="sortColumnAttrs('cellSummary')"
          >
            <template #default="{ row }">{{ row.cellSummary || '--' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('evidenceStatus')"
            label="证据状态"
            prop="contextStatus"
            :width="getColumnWidthString('evidenceStatus', 150)"
            v-bind="sortColumnAttrs('contextStatus')"
          >
            <template #default="{ row }">
              <el-tag :type="resolveEvidenceTagType(row.contextStatus)">
                {{ resolveEvidenceLabel(row.contextStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('operation')"
            label="操作"
            prop="operation"
            :width="getColumnWidthString('operation', 120)"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
      </el-tab-pane>
      <el-tab-pane label="报工修改日志" name="PRODUCTION_REPORT_REVISION">
        <div class="edhr-form-fill-log-page__revision-toolbar">
          <el-input
            v-model="productionReportRevisionQuery.workOrderCode"
            clearable
            placeholder="生产工单号"
            @keyup.enter="getProductionReportRevisionList"
          />
          <el-input
            v-model="productionReportRevisionQuery.processKeyword"
            clearable
            placeholder="工序编码或名称"
            @keyup.enter="getProductionReportRevisionList"
          />
          <el-input
            v-model="productionReportRevisionQuery.actualEmployeeName"
            clearable
            placeholder="原报工人"
            @keyup.enter="getProductionReportRevisionList"
          />
          <el-input
            v-model="productionReportRevisionQuery.modifiedByName"
            clearable
            placeholder="修改人"
            @keyup.enter="getProductionReportRevisionList"
          />
          <el-button
            type="primary"
            :loading="productionReportRevisionLoading"
            @click="getProductionReportRevisionList"
          >
            查询
          </el-button>
          <el-button @click="resetProductionReportRevisionQuery">重置</el-button>
        </div>
        <el-alert
          v-if="productionReportRevisionLoadError"
          :title="productionReportRevisionLoadError"
          type="error"
          :closable="false"
          show-icon
          class="edhr-form-fill-log-page__alert"
        />
        <el-table
          v-loading="productionReportRevisionLoading"
          data-production-report-revision-log-table
          :data="productionReportRevisionList"
          border
          stripe
          row-key="revisionId"
          :show-overflow-tooltip="true"
        >
          <el-table-column label="生产工单号" prop="workOrderCode" min-width="160">
            <template #default="{ row }">{{ row.workOrderCode || row.workOrderName || '--' }}</template>
          </el-table-column>
          <el-table-column label="工序" prop="processName" min-width="140">
            <template #default="{ row }">{{ row.processName || row.processCode || '--' }}</template>
          </el-table-column>
          <el-table-column label="原报工人" prop="actualEmployeeName" min-width="120">
            <template #default="{ row }">{{ row.actualEmployeeName || '--' }}</template>
          </el-table-column>
          <el-table-column label="原提交时间" prop="submittedAt" width="210">
            <template #default="{ row }">{{ formatFormLogDateTime(row.submittedAt) }}</template>
          </el-table-column>
          <el-table-column label="修改人" prop="modifiedByName" min-width="120" />
          <el-table-column label="修改时间" prop="modifiedAt" width="210">
            <template #default="{ row }">{{ formatFormLogDateTime(row.modifiedAt) }}</template>
          </el-table-column>
          <el-table-column label="修改原因" prop="changeReason" min-width="220" />
          <el-table-column label="修改字段数" prop="fieldCount" width="120" align="center" />
          <el-table-column label="修改摘要" prop="changeSummary" min-width="220" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openProductionReportRevisionDetail(row)">
                明细
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          v-model:page="productionReportRevisionQuery.pageNo"
          v-model:limit="productionReportRevisionQuery.pageSize"
          :total="productionReportRevisionTotal"
          @pagination="getProductionReportRevisionList"
        />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <el-drawer v-model="detailVisible" title="填写单元格明细" size="760px">
    <el-alert
      v-if="detailError"
      :title="detailError"
      type="error"
      :closable="false"
      show-icon
      class="edhr-form-fill-log-page__alert"
    />
    <el-descriptions v-if="detail" :column="2" border class="mb-12px">
      <el-descriptions-item label="表单名称">{{ detail.formName || detail.batchRecordReportId || '--' }}</el-descriptions-item>
      <el-descriptions-item label="执行编号">{{ detail.executionCode || '--' }}</el-descriptions-item>
      <el-descriptions-item label="批号">{{ detail.batchCode || '--' }}</el-descriptions-item>
      <el-descriptions-item label="生产工单号">{{ detail.workOrderCode || '--' }}</el-descriptions-item>
      <el-descriptions-item label="填写人">{{ detail.actorName || detail.actorId || '--' }}</el-descriptions-item>
      <el-descriptions-item label="填写时间">{{ formatFormLogDateTime(detail.changedAt) }}</el-descriptions-item>
    </el-descriptions>
    <el-table
      v-loading="detailLoading"
      :data="detail?.items || []"
      border
      :show-overflow-tooltip="true"
      row-key="auditItemId"
    >
      <el-table-column label="单元格位置" min-width="240">
        <template #default="{ row }">
          <el-tooltip
            :disabled="!row.fieldPath"
            :content="formatCellLocationTooltip(row)"
            placement="top"
            effect="light"
          >
            <span class="edhr-form-fill-log-page__cell-location">
              <span class="edhr-form-fill-log-page__cell-location-code">
                {{ formatCellLocation(row) }}
              </span>
              <span class="edhr-form-fill-log-page__cell-location-detail">
                {{ formatCellLocationDetail(row) }}
              </span>
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="单元格标签" prop="fieldLabel" min-width="150">
        <template #default="{ row }">{{ row.fieldLabel || row.fieldKey || '--' }}</template>
      </el-table-column>
      <template v-if="usesRecordbookSyncValues">
        <el-table-column label="记录本填写值" prop="recordbookValueDisplay" min-width="180">
          <template #default="{ row }">{{ row.recordbookValueDisplay ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="批记录存储值" prop="batchRecordValueDisplay" min-width="180">
          <template #default="{ row }">{{ row.batchRecordValueDisplay ?? '--' }}</template>
        </el-table-column>
      </template>
      <template v-else>
        <el-table-column label="旧值" prop="oldValueDisplay" min-width="180" />
        <el-table-column label="新值" prop="newValueDisplay" min-width="180" />
      </template>
      <el-table-column label="写入时间" prop="changedAt" width="210">
        <template #default="{ row }">{{ formatFormLogDateTime(row.changedAt) }}</template>
      </el-table-column>
    </el-table>
  </el-drawer>

  <el-drawer
    v-model="productionReportRevisionDetailVisible"
    title="报工修改明细"
    size="760px"
    data-production-report-revision-log-detail-drawer
  >
    <el-alert
      v-if="productionReportRevisionDetailError"
      :title="productionReportRevisionDetailError"
      type="error"
      :closable="false"
      show-icon
      class="edhr-form-fill-log-page__alert"
    />
    <el-descriptions
      v-if="productionReportRevisionDetail"
      :column="2"
      border
      class="mb-12px"
    >
      <el-descriptions-item label="生产工单号">
        {{ productionReportRevisionDetail.workOrderCode || productionReportRevisionDetail.workOrderName || '--' }}
      </el-descriptions-item>
      <el-descriptions-item label="工序">
        {{ productionReportRevisionDetail.processName || productionReportRevisionDetail.processCode || '--' }}
      </el-descriptions-item>
      <el-descriptions-item label="原报工人">
        {{ productionReportRevisionDetail.actualEmployeeName || '--' }}
      </el-descriptions-item>
      <el-descriptions-item label="原提交时间">
        {{ formatFormLogDateTime(productionReportRevisionDetail.submittedAt) }}
      </el-descriptions-item>
      <el-descriptions-item label="修改人">
        {{ productionReportRevisionDetail.modifiedByName || '--' }}
      </el-descriptions-item>
      <el-descriptions-item label="修改时间">
        {{ formatFormLogDateTime(productionReportRevisionDetail.modifiedAt) }}
      </el-descriptions-item>
      <el-descriptions-item label="修改原因" :span="2">
        {{ productionReportRevisionDetail.changeReason || '--' }}
      </el-descriptions-item>
    </el-descriptions>
    <el-table
      v-loading="productionReportRevisionDetailLoading"
      :data="productionReportRevisionDetail?.changes || []"
      border
      :show-overflow-tooltip="true"
      row-key="fieldName"
    >
      <el-table-column label="修改字段" prop="fieldName" min-width="160" />
      <el-table-column label="修改前" prop="beforeValue" min-width="220" />
      <el-table-column label="修改后" prop="afterValue" min-width="220" />
    </el-table>
  </el-drawer>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  getFormFillLogDetail,
  getFormFillLogPage,
  getProductionReportRevisionLogDetail,
  getProductionReportRevisionLogPage,
  type FormFillLogDetailRespVO,
  type FormFillLogItemRespVO,
  type FormFillLogPageReqVO,
  type FormFillLogPageRespVO,
  type ProductionReportRevisionLogDetailRespVO,
  type ProductionReportRevisionLogPageReqVO,
  type ProductionReportRevisionLogPageRespVO
} from '@/api/mes/pro/edhr/formFillLog'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProEdhrFormFillLogPage' })

const FORM_FILL_LOG_TABLE_KEY = 'mes.pro.edhr.formFillLog.main'
const router = useRouter()
const activeLogSource = ref<'FORM_FILL' | 'PRODUCTION_REPORT_REVISION'>('FORM_FILL')

const defaultColumns: UserTableColumnDefinition[] = [
  { key: 'formName', label: '表单名称', minWidth: 180 },
  { key: 'batchCode', label: '批号', minWidth: 150 },
  { key: 'workOrderCode', label: '生产工单号', minWidth: 160 },
  { key: 'executionCode', label: '执行编号', minWidth: 160 },
  { key: 'actorName', label: '填写人', minWidth: 120 },
  { key: 'changedAt', label: '填写时间', width: 210 },
  { key: 'fieldCount', label: '填写单元数', width: 120 },
  { key: 'cellSummary', label: '写入摘要', minWidth: 260 },
  { key: 'evidenceStatus', label: '证据状态', width: 150 },
  { key: 'operation', label: '操作', width: 120, hideable: false, business: false }
]

const {
  columns: formFillLogColumns,
  saving: formFillLogColumnSaving,
  isColumnVisible,
  getColumnWidthString,
  getColumnMinWidthString,
  handleHeaderDragend,
  saveConfig: saveFormFillLogColumnConfig
} = useUserTableColumns(FORM_FILL_LOG_TABLE_KEY, defaultColumns)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  formKeyword: '',
  changedAtRange: [] as string[],
  actorName: '',
  batchCode: '',
  workOrderCode: '',
  executionCode: ''
})
const list = ref<FormFillLogPageRespVO[]>([])
const total = ref(0)
const loading = ref(false)
const loadError = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<FormFillLogDetailRespVO>()
const productionReportRevisionQuery = reactive<ProductionReportRevisionLogPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  workOrderCode: '',
  processKeyword: '',
  actualEmployeeName: '',
  modifiedByName: ''
})
const productionReportRevisionList = ref<ProductionReportRevisionLogPageRespVO[]>([])
const productionReportRevisionTotal = ref(0)
const productionReportRevisionLoading = ref(false)
const productionReportRevisionLoadError = ref('')
const productionReportRevisionDetailVisible = ref(false)
const productionReportRevisionDetailLoading = ref(false)
const productionReportRevisionDetailError = ref('')
const productionReportRevisionDetail = ref<ProductionReportRevisionLogDetailRespVO>()
const usesRecordbookSyncValues = computed(() =>
  detail.value?.items?.some(
    (item) => item.recordbookValueDisplay !== undefined || item.batchRecordValueDisplay !== undefined
  ) === true
)

const formFillLogQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'formKeyword',
    label: '表单',
    type: 'text',
    queryParamKey: 'formKeyword',
    placeholder: '表单名称或报表 ID'
  },
  { key: 'changedAtRange', label: '时间段', type: 'dateRange', queryParamKey: 'changedAtRange' },
  { key: 'actorName', label: '填写人', type: 'text', queryParamKey: 'actorName', placeholder: '填写人姓名' },
  { key: 'batchCode', label: '批号', type: 'text', queryParamKey: 'batchCode', placeholder: '批号' },
  {
    key: 'workOrderCode',
    label: '生产工单号',
    type: 'text',
    queryParamKey: 'workOrderCode',
    placeholder: '生产工单号'
  },
  {
    key: 'executionCode',
    label: '执行编号',
    type: 'text',
    queryParamKey: 'executionCode',
    placeholder: '执行编号'
  }
]

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  return typeof responseMessage === 'string' && responseMessage.trim() ? responseMessage : fallback
}

const normalizeTextParam = (value?: string) => {
  if (typeof value !== 'string') return undefined
  const trimmed = value.trim()
  return trimmed || undefined
}

const formatFormLogDateTime = (value?: string | number | null) => {
  return formatEdhrDateTime(value)
}

const columnIndexToLetters = (columnIndex: number) => {
  let index = columnIndex + 1
  let letters = ''
  while (index > 0) {
    const remainder = (index - 1) % 26
    letters = String.fromCharCode(65 + remainder) + letters
    index = Math.floor((index - 1) / 26)
  }
  return letters
}

const parseIndexedPathPart = (pathValue: string | undefined, partName: string) => {
  const match = pathValue?.match(new RegExp(`${partName}\\[(\\d+)\\]`))
  return match ? Number(match[1]) : undefined
}

const resolveCellLocationParts = (row: FormFillLogItemRespVO) => {
  const sheetIndex = parseIndexedPathPart(row.fieldPath, 'sheet')
  const rowIndex =
    typeof row.rowIndex === 'number' ? row.rowIndex : parseIndexedPathPart(row.fieldPath, 'rows')
  const columnIndex =
    typeof row.columnIndex === 'number' ? row.columnIndex : parseIndexedPathPart(row.fieldPath, 'cells')
  return { sheetIndex, rowIndex, columnIndex }
}

const formatCellLocation = (row: FormFillLogItemRespVO) => {
  const { rowIndex, columnIndex } = resolveCellLocationParts(row)
  if (typeof rowIndex === 'number' && typeof columnIndex === 'number') {
    return `${columnIndexToLetters(columnIndex)}${rowIndex + 1}`
  }
  if (typeof rowIndex === 'number') return `第${rowIndex + 1}行`
  if (typeof columnIndex === 'number') return `${columnIndexToLetters(columnIndex)}列（第${columnIndex + 1}列）`
  return '位置未记录'
}

const formatCellLocationDetail = (row: FormFillLogItemRespVO) => {
  const { sheetIndex, rowIndex, columnIndex } = resolveCellLocationParts(row)
  const sheetLabel = typeof sheetIndex === 'number' ? `表${sheetIndex + 1}` : ''
  if (typeof rowIndex === 'number' && typeof columnIndex === 'number') {
    const position = `第${rowIndex + 1}行，第${columnIndex + 1}列`
    return sheetLabel ? `${sheetLabel} · ${position}` : position
  }
  if (typeof rowIndex === 'number') return sheetLabel ? `${sheetLabel} · 第${rowIndex + 1}行` : `第${rowIndex + 1}行`
  if (typeof columnIndex === 'number') return sheetLabel ? `${sheetLabel} · 第${columnIndex + 1}列` : `第${columnIndex + 1}列`
  return row.fieldLabel || row.fieldKey || '未记录行列'
}

const formatCellLocationTooltip = (row: FormFillLogItemRespVO) => {
  return row.fieldPath ? `原始路径：${row.fieldPath}` : ''
}

const buildQuery = (): FormFillLogPageReqVO => {
  const changedAtRange = Array.isArray(queryParams.changedAtRange) ? queryParams.changedAtRange : []
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    formKeyword: normalizeTextParam(queryParams.formKeyword),
    changedAtStart: changedAtRange[0],
    changedAtEnd: changedAtRange[1],
    actorName: normalizeTextParam(queryParams.actorName),
    batchCode: normalizeTextParam(queryParams.batchCode),
    workOrderCode: normalizeTextParam(queryParams.workOrderCode),
    executionCode: normalizeTextParam(queryParams.executionCode)
  }
}

const buildProductionReportRevisionQuery = (): ProductionReportRevisionLogPageReqVO => ({
  pageNo: productionReportRevisionQuery.pageNo,
  pageSize: productionReportRevisionQuery.pageSize,
  workOrderCode: normalizeTextParam(productionReportRevisionQuery.workOrderCode),
  processKeyword: normalizeTextParam(productionReportRevisionQuery.processKeyword),
  actualEmployeeName: normalizeTextParam(productionReportRevisionQuery.actualEmployeeName),
  modifiedByName: normalizeTextParam(productionReportRevisionQuery.modifiedByName),
  modifiedAtStart: productionReportRevisionQuery.modifiedAtStart,
  modifiedAtEnd: productionReportRevisionQuery.modifiedAtEnd
})

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getFormFillLogPage(buildQuery())
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '表单填写日志加载失败。')
  } finally {
    loading.value = false
  }
}

const getProductionReportRevisionList = async () => {
  productionReportRevisionLoading.value = true
  productionReportRevisionLoadError.value = ''
  try {
    const data = await getProductionReportRevisionLogPage(buildProductionReportRevisionQuery())
    productionReportRevisionList.value = data.list || []
    productionReportRevisionTotal.value = data.total || 0
  } catch (error) {
    productionReportRevisionList.value = []
    productionReportRevisionTotal.value = 0
    productionReportRevisionLoadError.value = resolveErrorMessage(error, '报工修改日志加载失败。')
  } finally {
    productionReportRevisionLoading.value = false
  }
}

const formFillLogQuickFilter = useTableQuickFilter(
  FORM_FILL_LOG_TABLE_KEY,
  formFillLogQuickFilterDefinitions,
  queryParams,
  getList
)

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.formKeyword = ''
  queryParams.changedAtRange = []
  queryParams.actorName = ''
  queryParams.batchCode = ''
  queryParams.workOrderCode = ''
  queryParams.executionCode = ''
  formFillLogQuickFilter.resetQuickFilter()
}

const resetProductionReportRevisionQuery = () => {
  productionReportRevisionQuery.pageNo = 1
  productionReportRevisionQuery.pageSize = 10
  productionReportRevisionQuery.workOrderCode = ''
  productionReportRevisionQuery.processKeyword = ''
  productionReportRevisionQuery.actualEmployeeName = ''
  productionReportRevisionQuery.modifiedByName = ''
  productionReportRevisionQuery.modifiedAtStart = undefined
  productionReportRevisionQuery.modifiedAtEnd = undefined
  void getProductionReportRevisionList()
}

const openBatchExecutionWorkOrder = (row: FormFillLogPageRespVO) => {
  if (!row.batchExecutionId) return
  router.push({
    path: '/mes/pro/feedback/edhr-batch-execution/detail',
    query: { id: String(row.batchExecutionId), ['focus']: 'work-order' }
  })
}

const openBatchExecutionDetail = (row: FormFillLogPageRespVO) => {
  if (!row.batchExecutionId) return
  router.push({
    path: '/mes/pro/feedback/edhr-batch-execution/detail',
    query: { id: String(row.batchExecutionId) }
  })
}

const openDetail = async (row: FormFillLogPageRespVO) => {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = undefined
  try {
    detail.value = await getFormFillLogDetail(row.auditBatchId)
  } catch (error) {
    detailError.value = resolveErrorMessage(error, '填写单元格明细加载失败。')
  } finally {
    detailLoading.value = false
  }
}

const openProductionReportRevisionDetail = async (row: ProductionReportRevisionLogPageRespVO) => {
  productionReportRevisionDetailVisible.value = true
  productionReportRevisionDetailLoading.value = true
  productionReportRevisionDetailError.value = ''
  productionReportRevisionDetail.value = undefined
  try {
    productionReportRevisionDetail.value = await getProductionReportRevisionLogDetail(row.revisionId)
  } catch (error) {
    productionReportRevisionDetailError.value = resolveErrorMessage(error, '报工修改明细加载失败。')
  } finally {
    productionReportRevisionDetailLoading.value = false
  }
}

const resolveEvidenceLabel = (contextStatus?: string) => {
  if (contextStatus === 'COMPLETE') return '证据完整'
  if (contextStatus === 'EXECUTION_MISSING') return '执行上下文缺失'
  if (contextStatus === 'BATCH_CONTEXT_MISSING') return '批次上下文缺失'
  return contextStatus || '未知'
}

const resolveEvidenceTagType = (contextStatus?: string) => {
  if (contextStatus === 'COMPLETE') return 'success'
  if (contextStatus === 'EXECUTION_MISSING') return 'danger'
  return 'warning'
}

onMounted(() => {
  void getList()
  void getProductionReportRevisionList()
})
</script>

<style scoped>
.edhr-form-fill-log-page__alert {
  margin-bottom: 12px;
}

.edhr-form-fill-log-page__source-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.edhr-form-fill-log-page__revision-toolbar {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr)) auto auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.edhr-form-fill-log-page :deep(.el-table__cell) {
  font-size: 0.9rem;
}

.edhr-form-fill-log-page :deep(.el-table th.el-table__cell) {
  background: #f7f9fc;
  color: #263247;
}

.edhr-form-fill-log-page__cell-location {
  display: inline-flex;
  flex-direction: column;
  gap: 2px;
  color: #263247;
  font-variant-numeric: tabular-nums;
}

.edhr-form-fill-log-page__cell-location-code {
  color: #1677ff;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
}

.edhr-form-fill-log-page__cell-location-detail {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.2;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .edhr-form-fill-log-page__revision-toolbar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
