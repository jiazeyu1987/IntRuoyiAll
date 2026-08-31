<template>
  <ContentWrap :body-style="{ padding: '0px' }" class="!mb-0">
    <div v-loading="loading" class="batch-record-cell-link">
      <header class="batch-record-cell-link__toolbar">
        <div class="batch-record-cell-link__title">
          <strong>批记录单元格链接</strong>
          <span>左侧选择源表单，右侧切换目标表单并点选目标单元格</span>
        </div>
        <div class="batch-record-cell-link__controls">
          <el-select
            v-model="linkMode"
            data-batch-record-repeat-row-group-mode
            class="batch-record-cell-link__mode-select"
          >
            <el-option label="单元格链接" :value="LINK_MODE_CELL_LINK" />
            <el-option label="重复行组" :value="LINK_MODE_REPEAT_ROW_GROUP" />
          </el-select>
          <el-select
            v-model="sourceReportId"
            filterable
            placeholder="选择来源"
            class="batch-record-cell-link__select batch-record-cell-link__source-select"
            @change="handleSourceSelectionChange"
          >
            <el-option label="生产工单" :value="PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID" />
            <el-option
              label="报工数据"
              :value="PROCESS_POOL_REPORT_SOURCE_REPORT_ID"
            />
            <el-option
              label="领料单数据"
              :value="PRODUCTION_PICK_LIST_SOURCE_REPORT_ID"
              :disabled="!hasFormalRouteProcessContext"
            />
            <el-option
              label="一线PQC数据"
              :value="PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID"
              :disabled="!hasFormalRouteProcessContext"
            />
            <el-option
              v-for="form in forms"
              :key="form.reportId"
              :label="form.reportName"
              :value="form.reportId"
            />
          </el-select>
          <el-select
            v-model="targetReportId"
            filterable
            placeholder="选择目标表单"
            class="batch-record-cell-link__select batch-record-cell-link__target-select"
            :disabled="!targetForms.length"
            @change="handleTargetReportChange"
          >
            <el-option
              v-for="form in targetForms"
              :key="form.reportId"
              :label="form.reportName"
              :value="form.reportId"
            />
          </el-select>
          <el-select
            v-if="sourceType === SOURCE_TYPE_PROCESS_POOL_REPORT"
            v-model="aggregationStrategy"
            placeholder="选择多笔汇总方式"
            class="batch-record-cell-link__select batch-record-cell-link__aggregation-select"
            clearable
          >
            <el-option
              v-for="option in availableAggregationOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-button
            class="batch-record-cell-link__source-link-count"
            plain
            aria-label="查看源表单链接详情"
            @click="openRelationDetailDialog"
          >
            {{ sourceLinkCountText }}
          </el-button>
          <el-button
            v-if="linkMode === LINK_MODE_CELL_LINK"
            class="batch-record-cell-link__create-button"
            type="success"
            :loading="saving"
            :disabled="!canCreateRule || saving"
            @click="createRule"
          >
            建立链接
          </el-button>
          <el-button v-if="linkMode === LINK_MODE_REPEAT_ROW_GROUP" :disabled="!selectedTargetCell" @click="setRepeatTemplateFromSelectedTarget">选择模板记录</el-button>
          <el-button v-if="linkMode === LINK_MODE_REPEAT_ROW_GROUP" :disabled="!selectedTargetCell" @click="includeSelectedTargetRowInRepeatArea">加入重复区域</el-button>
          <el-button v-if="linkMode === LINK_MODE_REPEAT_ROW_GROUP" :disabled="!canCreateRepeatRowMapping" @click="createRepeatRowMapping">建立模板链接</el-button>
          <el-button v-if="linkMode === LINK_MODE_REPEAT_ROW_GROUP" type="success" :loading="saving" :disabled="!canSaveRepeatRowGroup || saving" @click="saveRepeatRowGroup">保存重复行组</el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </header>

      <main class="batch-record-cell-link__form-stage">
        <section
          class="batch-record-cell-link__pane is-source"
          :class="{
            'batch-record-cell-link__work-order-field-panel': isStructuredSourceSelected,
            'batch-record-cell-link__process-pool-source-panel': isProcessPoolReportSelected,
            'batch-record-cell-link__pqc-source-panel': isPqcAggregateSelected
          }"
          :data-process-pool-report-source-fields="sourceType === SOURCE_TYPE_PROCESS_POOL_REPORT ? 'true' : undefined"
          :data-process-pool-report-field-count="sourceType === SOURCE_TYPE_PROCESS_POOL_REPORT ? filteredProcessPoolReportSourceFields.length : undefined"
          :data-production-pick-list-source-fields="sourceType === SOURCE_TYPE_PRODUCTION_PICK_LIST ? 'true' : undefined"
          :data-production-pick-list-field-count="sourceType === SOURCE_TYPE_PRODUCTION_PICK_LIST ? filteredProductionPickListSourceFields.length : undefined"
          :data-pqc-aggregate-source-fields="sourceType === SOURCE_TYPE_PQC_AGGREGATE_DETAIL ? 'true' : undefined"
          :data-pqc-aggregate-field-count="sourceType === SOURCE_TYPE_PQC_AGGREGATE_DETAIL ? filteredPqcAggregateSourceFields.length : undefined"
        >
          <div class="batch-record-cell-link__pane-title">
            <span>{{ isStructuredSourceSelected ? '源字段' : '源表单' }}</span>
            <strong>{{ sourcePanelTitle }}</strong>
          </div>
          <div
            v-if="isProcessPoolReportSelected"
            class="batch-record-cell-link__process-pool-selector"
            data-process-pool-context-selector
          >
            <div class="batch-record-cell-link__process-pool-selector-main">
              <span>DCC项目代码</span>
              <el-select
                v-model="selectedProcessPoolDccProjectCodeId"
                class="batch-record-cell-link__process-pool-select"
                placeholder="请输入项目名称或项目代码搜索"
                filterable
                remote
                clearable
                reserve-keyword
                :remote-method="loadProcessPoolDccProjectCodeOptions"
                :loading="processPoolDccProjectCodeLoading"
                data-process-pool-dcc-project-select
                @visible-change="(visible) => visible && loadProcessPoolDccProjectCodeOptions()"
                @change="handleProcessPoolDccProjectCodeChange"
              >
                <el-option
                  v-for="projectCode in processPoolDccProjectCodeOptions"
                  :key="projectCode.id"
                  :label="formatDccProjectCodeOption(projectCode)"
                  :value="projectCode.id"
                />
              </el-select>
            </div>
            <div class="batch-record-cell-link__process-pool-selector-main">
              <span>工序</span>
              <el-select
                v-model="selectedProcessPoolRouteProcessId"
                class="batch-record-cell-link__process-pool-select"
                placeholder="选择工序"
                filterable
                clearable
                :disabled="!selectedProcessPoolDccProjectCodeId && !processPoolRouteProcesses.length"
                data-process-pool-route-process-select
                @change="handleProcessPoolRouteProcessChange"
              >
                <el-option
                  v-for="process in processPoolRouteProcesses"
                  :key="process.id"
                  :label="`${process.sort ?? '-'}. ${process.processName || '未命名工序'}`"
                  :value="process.id"
                />
              </el-select>
            </div>
          </div>
          <div v-if="isPqcAggregateSelected" class="batch-record-cell-link__pqc-process-selector" data-pqc-process-selector>
            <div class="batch-record-cell-link__pqc-process-selector-main">
              <span>工序名称</span>
              <el-select
                v-model="selectedPqcQaProcessId"
                class="batch-record-cell-link__pqc-process-select"
                placeholder="选择工序"
                filterable
                data-pqc-process-select
                @change="handlePqcProcessChange"
              >
                <el-option
                  v-for="process in pqcProcesses"
                  :key="process.id"
                  :label="`${process.sort ?? '-'}. ${process.processName || '未命名工序'}`"
                  :value="process.id"
                />
              </el-select>
            </div>
            <div class="batch-record-cell-link__pqc-process-meta">
              <span>当前工序序号</span>
              <strong>{{ selectedPqcQaProcess?.sort ?? '-' }}</strong>
              <span>当前工序</span>
              <strong>{{ selectedPqcQaProcess?.processName || '请选择工序' }}</strong>
            </div>
          </div>
          <div
            class="batch-record-cell-link__sheet-scroll batch-record-cell-link__source-sheet-scroll"
            data-cell-link-scroll-pane="source"
          >
            <BatchRecordLinkSheet
              :columns="sourceRenderableSheet.columns"
              :rows="sourceRenderableSheet.rows"
              :empty-text="sourceSheetEmptyText"
              @select-cell="selectSourceCell"
            />
          </div>
        </section>
        <section class="batch-record-cell-link__pane is-target">
          <div class="batch-record-cell-link__pane-title">
            <span>目标表单</span>
            <strong>{{ targetForm?.reportName || '未选择' }}</strong>
          </div>
          <div
            class="batch-record-cell-link__sheet-scroll batch-record-cell-link__target-sheet-scroll"
            data-cell-link-scroll-pane="target"
          >
            <BatchRecordLinkSheet
              :columns="targetRenderableSheet.columns"
              :rows="targetRenderableSheet.rows"
              empty-text="请选择目标表单"
              @select-cell="selectTargetCell"
            />
          </div>
        </section>
      </main>

      <section v-if="linkMode === LINK_MODE_REPEAT_ROW_GROUP" class="batch-record-cell-link__repeat-panel" data-repeat-row-group-candidate-list>
        <div>
          <strong>重复行组</strong>
          <span>模板行：{{ repeatTemplateRowText }}；重复区域：{{ repeatAreaText }}</span>
        </div>
        <el-table :data="repeatRowGroupCandidateRecords" size="small" border max-height="180">
          <el-table-column prop="recordSequence" label="序号" width="80" />
          <el-table-column label="记录行" width="140">
            <template #default="{ row }">第 {{ row.startRowIndex + 1 }} 行</template>
          </el-table-column>
          <el-table-column label="投影预览" min-width="260">
            <template #default="{ row }">
              <span v-for="mapping in repeatRowGroupMappings" :key="mapping.sourceFieldCode" class="batch-record-cell-link__projection">
                {{ mapping.sourceFieldName || mapping.sourceFieldCode }} -> {{ projectionTargetCellKey(row, mapping) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <el-dialog
        v-model="relationDetailDialogVisible"
        title="源表单链接详情"
        width="920px"
        class="batch-record-cell-link__detail-dialog"
      >
        <div class="batch-record-cell-link__detail-summary">
          <span>{{ isStructuredSourceSelected ? '当前源字段集合' : '当前源表单' }}</span>
          <strong>{{ sourcePanelTitle }}</strong>
          <em>{{ sourceLinkCountText }}</em>
        </div>
        <el-table
          v-if="sourceLinkedRules.length"
          :data="sourceLinkedRules"
          max-height="420"
          size="small"
          border
        >
          <el-table-column label="源单元格" min-width="220">
            <template #default="{ row }">
              {{ row.sourceLabel || row.sourceCellKey }}
            </template>
          </el-table-column>
          <el-table-column label="目标" min-width="280">
            <template #default="{ row }">
              {{ row.targetReportName }} / {{ row.targetLabel || row.targetCellKey }}
            </template>
          </el-table-column>
          <el-table-column label="策略" width="150">
            <template #default="{ row }">
              {{ row.aggregationStrategy || row.overwritePolicy || 'ONLY_WHEN_EMPTY' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled === false ? 'info' : 'success'" size="small">
                {{ row.enabled === false ? '停用' : '启用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="focusRelationRule(row)">定位</el-button>
              <el-button link type="danger" :disabled="saving" @click="removeRuleByIndex(row.ruleIndex)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无表单链接关系" />
      </el-dialog>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref, watch, type PropType } from 'vue'
import {
  BatchRecordCellLinkApi,
  type BatchRecordCellLinkCellVO,
  type BatchRecordCellLinkFormCellsVO,
  type BatchRecordCellLinkFormVO,
  type BatchRecordCellLinkPqcProcessVO,
  type BatchRecordCellLinkRouteProcessVO,
  type BatchRecordCellLinkRuleVO,
  type BatchRecordCellLinkSourceFieldVO,
  type BatchRecordCellLinkWorkbenchContextVO,
  type BatchRecordRepeatRowGroupMappingVO,
  type BatchRecordRepeatRowGroupRecordVO,
  type BatchRecordRepeatRowGroupVO
} from '@/api/mes/pro/batchrecordcelllink'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import {
  normalizeTemplateCellMerge,
  stringifyTemplateCell,
  type TemplateRawLayout
} from '../batchrecord-shared/batchRecordTemplateRules'

defineOptions({ name: 'MesProBatchRecordCellLink' })

interface RenderedCell {
  key: string
  rowIndex: number
  columnIndex: number
  text: string
  rowSpan: number
  colSpan: number
  classNames: Record<string, boolean>
  dataAttrs: Record<string, string | undefined>
  cellMeta?: BatchRecordCellLinkCellVO
}

interface RenderedColumn {
  columnIndex: number
  widthPercent: number
}

interface RenderedRow {
  rowIndex: number
  height: number
  cells: RenderedCell[]
}

interface RenderedSheet {
  columns: RenderedColumn[]
  rows: RenderedRow[]
}

type SourceLinkedRule = BatchRecordCellLinkRuleVO & { ruleIndex: number }
type ProcessPoolSourceValueType = 'NUMBER' | 'STRING' | 'BOOLEAN'
interface ProcessPoolReportAggregationOption {
  value: string
  label: string
  sourceValueTypes: readonly ProcessPoolSourceValueType[]
}

type CellLinkRawCell = Parameters<typeof normalizeTemplateCellMerge>[0] & {
  fillForm?: unknown
  value?: unknown
  text?: unknown
}

type CellLinkRawRow = {
  height?: unknown
  cells?: Record<string, CellLinkRawCell>
}

type CellLinkRawLayout = Omit<TemplateRawLayout, 'rows'> & {
  cols?: Record<string, { width?: unknown }>
  rows?: Record<string, CellLinkRawRow>
}

const DEFAULT_COLUMN_WIDTH = 120
const DEFAULT_ROW_HEIGHT = 32
const EMPTY_FILLABLE_PLACEHOLDER = '?'
const SOURCE_TYPE_BATCH_RECORD_CELL = 'BATCH_RECORD_CELL'
const SOURCE_TYPE_PRODUCTION_WORK_ORDER = 'PRODUCTION_WORK_ORDER'
const SOURCE_TYPE_PROCESS_POOL_REPORT = 'PROCESS_POOL_REPORT'
const SOURCE_TYPE_PRODUCTION_PICK_LIST = 'PRODUCTION_PICK_LIST'
const SOURCE_TYPE_PQC_AGGREGATE_DETAIL = 'PQC_AGGREGATE_DETAIL'
const LINK_MODE_CELL_LINK = 'CELL_LINK'
const LINK_MODE_REPEAT_ROW_GROUP = 'REPEAT_ROW_GROUP'
const PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID = 'PRODUCTION_WORK_ORDER'
const PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME = '生产工单'
const PROCESS_POOL_REPORT_SOURCE_REPORT_ID = 'PROCESS_POOL_REPORT'
const PROCESS_POOL_REPORT_SOURCE_REPORT_NAME = '报工数据'
const PRODUCTION_PICK_LIST_SOURCE_REPORT_ID = 'PRODUCTION_PICK_LIST'
const PRODUCTION_PICK_LIST_SOURCE_REPORT_NAME = '领料单数据'
const PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID = 'PQC_AGGREGATE_DETAIL'
const PQC_AGGREGATE_DETAIL_SOURCE_REPORT_NAME = '一线PQC数据'
const PROCESS_POOL_REPORT_AGGREGATION_OPTIONS: readonly ProcessPoolReportAggregationOption[] = [
  { value: 'SUM', label: '求和', sourceValueTypes: ['NUMBER'] },
  { value: 'LIST', label: '按顺序合并', sourceValueTypes: ['STRING', 'BOOLEAN'] },
  { value: 'DISTINCT_LIST', label: '去重后合并', sourceValueTypes: ['STRING', 'BOOLEAN'] },
  { value: 'FIRST', label: '第一笔', sourceValueTypes: ['NUMBER', 'STRING', 'BOOLEAN'] },
  { value: 'LAST', label: '最后一笔', sourceValueTypes: ['NUMBER', 'STRING', 'BOOLEAN'] },
  { value: 'MIN', label: '最小值', sourceValueTypes: ['NUMBER'] },
  { value: 'MAX', label: '最大值', sourceValueTypes: ['NUMBER'] }
]
const PROCESS_POOL_REPORT_QUANTITY_AGGREGATION_SOURCE_FIELDS = new Set([
  'outputQuantity',
  'lossQuantity',
  'totalQuantity'
])
const PROCESS_POOL_REPORT_SIGNATURE_TARGET_SOURCE_FIELDS = new Set([
  'signatureUserId',
  'reviewSignatureUserId'
])
const SIGNATURE_TARGET_LABEL_KEYWORDS = ['签名', '操作人', '复核人'] as const

const BatchRecordLinkSheet = defineComponent({
  name: 'BatchRecordLinkSheet',
  props: {
    columns: {
      type: Array as PropType<RenderedColumn[]>,
      required: true
    },
    rows: {
      type: Array as PropType<RenderedRow[]>,
      required: true
    },
    emptyText: {
      type: String,
      required: true
    }
  },
  emits: ['select-cell'],
  setup(props, { emit }) {
    return () =>
      props.rows.length
        ? h(
            'table',
            { class: 'batch-record-cell-link-sheet' },
            [
              h(
                'colgroup',
                props.columns.map((column) =>
                  h('col', {
                    key: column.columnIndex,
                    style: { width: `${column.widthPercent}%` }
                  })
                )
              ),
              h(
                'tbody',
                props.rows.map((row) =>
                  h(
                    'tr',
                    {
                      key: row.rowIndex,
                      class: 'batch-record-cell-link-sheet__row',
                      style: { height: `${row.height}px` }
                    },
                    row.cells.map((cell) =>
                      h(
                        'td',
                        {
                          key: cell.key,
                          rowspan: cell.rowSpan,
                          colspan: cell.colSpan,
                          class: cell.classNames,
                          ...cell.dataAttrs,
                          onClick: () => emit('select-cell', cell.cellMeta)
                        },
                        h('span', { class: 'batch-record-cell-link-sheet__text' }, cell.text)
                      )
                    )
                  )
                )
              )
            ]
          )
        : h('div', { class: 'batch-record-cell-link__empty' }, props.emptyText)
  }
})

const route = useRoute()
const router = useRouter()
const message = useMessage()
const requestedSourceReportId = String(route.query.sourceReportId || '')
const requestedTargetRouteProcessId = parseNumber(route.query.routeProcessId)
const requestedTargetReportId = String(route.query.targetReportId || '')
const requestedProcessPoolDccProjectCodeId = parseNumber(route.query.dccProjectCodeId)
const requestedProcessPoolDccProjectCodeKeyword = String(route.query.dccProjectCode || '')

const loading = ref(false)
const saving = ref(false)
const context = ref<BatchRecordCellLinkWorkbenchContextVO>()
const forms = ref<BatchRecordCellLinkFormVO[]>([])
const processPoolDccProjectCodeLoading = ref(false)
const processPoolDccProjectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const processPoolDccProjectCodeInitialOptionsLoaded = ref(false)
const selectedProcessPoolDccProjectCodeId = ref<number | undefined>(requestedProcessPoolDccProjectCodeId)
const processPoolRouteProcesses = ref<BatchRecordCellLinkRouteProcessVO[]>([])
const selectedProcessPoolRouteProcessId = ref<number>()
const pqcProcesses = ref<BatchRecordCellLinkPqcProcessVO[]>([])
const selectedPqcQaProcessId = ref<number>()
const productionWorkOrderSourceFields = ref<BatchRecordCellLinkSourceFieldVO[]>([])
const processPoolReportSourceFields = ref<BatchRecordCellLinkSourceFieldVO[]>([])
const productionPickListSourceFields = ref<BatchRecordCellLinkSourceFieldVO[]>([])
const pqcAggregateSourceFields = ref<BatchRecordCellLinkSourceFieldVO[]>([])
const rules = ref<BatchRecordCellLinkRuleVO[]>([])
const sourceType = ref(resolveSourceTypeByReportId(requestedSourceReportId))
const sourceReportId = ref(requestedSourceReportId)
const sourceFieldCode = ref('')
const aggregationStrategy = ref('')
const lastSyncedProcessPoolAggregationSourceFieldCode = ref('')
const targetReportId = ref('')
const sourceCells = ref<BatchRecordCellLinkFormCellsVO>()
const targetCells = ref<BatchRecordCellLinkFormCellsVO>()
const selectedSourceCell = ref<BatchRecordCellLinkCellVO>()
const selectedTargetCell = ref<BatchRecordCellLinkCellVO>()
const relationDetailDialogVisible = ref(false)
const linkMode = ref(LINK_MODE_CELL_LINK)
const repeatRowGroups = ref<BatchRecordRepeatRowGroupVO[]>([])
if (requestedTargetRouteProcessId !== undefined) {
  selectedProcessPoolRouteProcessId.value = requestedTargetRouteProcessId
}
const repeatTemplateStartRowIndex = ref<number>()
const repeatTemplateEndRowIndex = ref<number>()
const repeatAreaStartRowIndex = ref<number>()
const repeatAreaEndRowIndex = ref<number>()
const repeatRowGroupMappings = ref<BatchRecordRepeatRowGroupMappingVO[]>([])
const repeatRowGroupCandidateRecords = computed<BatchRecordRepeatRowGroupRecordVO[]>(() => {
  if (repeatAreaStartRowIndex.value === undefined || repeatAreaEndRowIndex.value === undefined) {
    return []
  }
  const startRowIndex = Math.min(repeatAreaStartRowIndex.value, repeatAreaEndRowIndex.value)
  const endRowIndex = Math.max(repeatAreaStartRowIndex.value, repeatAreaEndRowIndex.value)
  return Array.from({ length: endRowIndex - startRowIndex + 1 }, (_, index) => {
    const rowIndex = startRowIndex + index
    return {
      recordSequence: index + 1,
      startRowIndex: rowIndex,
      endRowIndex: rowIndex,
      recordKey: `R${index + 1}:${rowIndex}`
    }
  })
})
const repeatTemplateRowText = computed(() =>
  repeatTemplateStartRowIndex.value === undefined
    ? '未选择'
    : repeatTemplateStartRowIndex.value === repeatTemplateEndRowIndex.value
      ? `第 ${repeatTemplateStartRowIndex.value + 1} 行`
      : `第 ${repeatTemplateStartRowIndex.value + 1}-${(repeatTemplateEndRowIndex.value || repeatTemplateStartRowIndex.value) + 1} 行`
)
const repeatAreaText = computed(() => {
  if (repeatAreaStartRowIndex.value === undefined || repeatAreaEndRowIndex.value === undefined) {
    return '未选择'
  }
  const startRowIndex = Math.min(repeatAreaStartRowIndex.value, repeatAreaEndRowIndex.value)
  const endRowIndex = Math.max(repeatAreaStartRowIndex.value, repeatAreaEndRowIndex.value)
  return startRowIndex === endRowIndex
    ? `第 ${startRowIndex + 1} 行`
    : `第 ${startRowIndex + 1}-${endRowIndex + 1} 行`
})
const canCreateRepeatRowMapping = computed(() => Boolean(
  sourceType.value === SOURCE_TYPE_PROCESS_POOL_REPORT &&
  selectedSourceCell.value &&
  selectedTargetCell.value &&
  repeatTemplateStartRowIndex.value !== undefined &&
  repeatTemplateEndRowIndex.value !== undefined
))
const canSaveRepeatRowGroup = computed(() => Boolean(
  activeTargetRouteProcessId.value !== undefined &&
  targetReportId.value &&
  repeatTemplateStartRowIndex.value !== undefined &&
  repeatTemplateEndRowIndex.value !== undefined &&
  repeatAreaStartRowIndex.value !== undefined &&
  repeatAreaEndRowIndex.value !== undefined &&
  repeatRowGroupCandidateRecords.value.length > 0 &&
  repeatRowGroupMappings.value.length > 0
))

const isProductionWorkOrderSelected = computed(() => sourceReportId.value === PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID)
const isProcessPoolReportSelected = computed(() => sourceReportId.value === PROCESS_POOL_REPORT_SOURCE_REPORT_ID)
const isProductionPickListSelected = computed(() => sourceReportId.value === PRODUCTION_PICK_LIST_SOURCE_REPORT_ID)
const isPqcAggregateSelected = computed(() => sourceReportId.value === PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID)
const isStructuredSourceSelected = computed(
  () => isProductionWorkOrderSelected.value ||
    isProcessPoolReportSelected.value ||
    isProductionPickListSelected.value ||
    isPqcAggregateSelected.value
)
const sourceForm = computed(() =>
  isStructuredSourceSelected.value
    ? undefined
    : forms.value.find((form) => form.reportId === sourceReportId.value)
)
const targetForm = computed(() => forms.value.find((form) => form.reportId === targetReportId.value))
const selectedProcessPoolRouteProcess = computed(() =>
  processPoolRouteProcesses.value.find((process) =>
    Number(process.id) === Number(selectedProcessPoolRouteProcessId.value)
  )
)
const selectedPqcQaProcess = computed(() =>
  pqcProcesses.value.find((process) => Number(process.id) === Number(selectedPqcQaProcessId.value))
)
const activeTargetRouteProcessId = computed(() => {
  if (isProcessPoolReportSelected.value) {
    return selectedProcessPoolRouteProcessId.value ?? requestedTargetRouteProcessId
  }
  return targetForm.value?.routeProcessId ?? requestedTargetRouteProcessId
})
const hasFormalRouteProcessContext = computed(() => Boolean(
  context.value?.routeId && (
    activeTargetRouteProcessId.value !== undefined ||
    forms.value.some((form) => form.routeProcessId !== undefined && form.routeProcessId !== null)
  )
))
const filteredProcessPoolReportSourceFields = computed(() => {
  const targetRouteProcessId = activeTargetRouteProcessId.value
  if (isProcessPoolReportSelected.value && targetRouteProcessId === undefined) {
    return []
  }
  return processPoolReportSourceFields.value.filter((field) =>
    field.routeProcessId === undefined ||
    field.routeProcessId === null ||
    field.routeProcessId === targetRouteProcessId
  ).filter((field) =>
    !isProcessPoolDeviceParameterMetadataSourceField(field)
  ).filter((field) =>
    !isProcessPoolDeviceSourceField(field) ||
    (isProcessPoolDeviceGroupSourceField(field)
      ? Boolean(field.deviceName)
      : field.deviceId !== undefined && Boolean(field.deviceCode) && Boolean(field.deviceName))
  )
})
const filteredProductionPickListSourceFields = computed(() => {
  const targetRouteProcessId = activeTargetRouteProcessId.value
  return productionPickListSourceFields.value.filter((field) => field.routeProcessId === targetRouteProcessId)
})
const filteredPqcAggregateSourceFields = computed(() => {
  const targetQaProcessId = selectedPqcQaProcessId.value
  return pqcAggregateSourceFields.value.filter((field) =>
    targetQaProcessId !== undefined &&
    field.qaProcessId !== undefined &&
    field.qaProcessId !== null &&
    Number(field.qaProcessId) === Number(targetQaProcessId)
  )
})
const targetForms = computed(() => {
  if (isStructuredSourceSelected.value) {
    if (isProcessPoolReportSelected.value) {
      const targetRouteProcessId = activeTargetRouteProcessId.value
      return targetRouteProcessId === undefined
        ? forms.value
        : forms.value.filter((form) =>
            form.routeProcessId === undefined ||
            form.routeProcessId === null ||
            Number(form.routeProcessId) === Number(targetRouteProcessId)
          )
    }
    return forms.value
  }
  const candidates = forms.value.filter((form) => form.reportId !== sourceReportId.value)
  return candidates.length ? candidates : forms.value
})
const canCreateRule = computed(() => Boolean(
  selectedSourceCell.value &&
  selectedTargetCell.value &&
  canUseTargetCellWithSource(selectedTargetCell.value) &&
  targetReportId.value &&
  (!isStructuredSourceSelected.value || activeTargetRouteProcessId.value !== undefined) &&
  (sourceType.value !== SOURCE_TYPE_PROCESS_POOL_REPORT || aggregationStrategy.value)
))
const availableAggregationOptions = computed(() => {
  const sourceValueType = (selectedSourceCell.value?.valueType || 'STRING') as ProcessPoolSourceValueType
  return PROCESS_POOL_REPORT_AGGREGATION_OPTIONS.filter((option) =>
    option.sourceValueTypes.includes(sourceValueType)
  )
})

function resolveDefaultProcessPoolReportAggregationStrategy(cell?: BatchRecordCellLinkCellVO) {
  const sourceFieldCode = String(cell?.sourceFieldCode || cell?.cellKey || '')
  const sourceValueType = (cell?.valueType || 'STRING') as ProcessPoolSourceValueType
  if (sourceValueType === 'NUMBER' &&
    PROCESS_POOL_REPORT_QUANTITY_AGGREGATION_SOURCE_FIELDS.has(sourceFieldCode)) {
    return 'SUM'
  }
  if (PROCESS_POOL_REPORT_AGGREGATION_OPTIONS.some((option) =>
    option.value === 'LAST' && option.sourceValueTypes.includes(sourceValueType))) {
    return 'LAST'
  }
  return PROCESS_POOL_REPORT_AGGREGATION_OPTIONS.find((option) =>
    option.sourceValueTypes.includes(sourceValueType)
  )?.value || ''
}

function syncProcessPoolReportAggregationStrategy(cell?: BatchRecordCellLinkCellVO) {
  if (sourceType.value !== SOURCE_TYPE_PROCESS_POOL_REPORT) {
    return
  }
  const sourceFieldCodeForAggregation = String(cell?.sourceFieldCode || cell?.cellKey || '')
  if (!sourceFieldCodeForAggregation) {
    aggregationStrategy.value = ''
    lastSyncedProcessPoolAggregationSourceFieldCode.value = ''
    return
  }
  const currentStrategyMatchesFieldType = availableAggregationOptions.value.some(
    (option) => option.value === aggregationStrategy.value
  )
  if (!currentStrategyMatchesFieldType ||
    !aggregationStrategy.value ||
    lastSyncedProcessPoolAggregationSourceFieldCode.value !== sourceFieldCodeForAggregation) {
    aggregationStrategy.value = resolveDefaultProcessPoolReportAggregationStrategy(cell)
  }
  lastSyncedProcessPoolAggregationSourceFieldCode.value = sourceFieldCodeForAggregation
}

function isProcessPoolReportSignatureTargetSourceCell(cell?: BatchRecordCellLinkCellVO) {
  const sourceFieldCode = String(cell?.sourceFieldCode || cell?.cellKey || '')
  return cell?.sourceType === SOURCE_TYPE_PROCESS_POOL_REPORT &&
    PROCESS_POOL_REPORT_SIGNATURE_TARGET_SOURCE_FIELDS.has(sourceFieldCode)
}

function isSignatureTargetCell(cell?: BatchRecordCellLinkCellVO) {
  const label = String(cell?.label || '')
  return Boolean(
    cell?.signatureCell ||
    (
      cell?.linkableAsTarget &&
      SIGNATURE_TARGET_LABEL_KEYWORDS.some((keyword) => label.includes(keyword))
    )
  )
}

function canUseTargetCellWithSource(
  cell?: BatchRecordCellLinkCellVO,
  sourceCell: BatchRecordCellLinkCellVO | undefined = selectedSourceCell.value
) {
  if (!cell) {
    return false
  }
  if (isSignatureTargetCell(cell)) {
    return isProcessPoolReportSignatureTargetSourceCell(sourceCell)
  }
  return Boolean(cell.linkableAsTarget)
}

function clearSelectedTargetCellIfSourceCannotUseIt(sourceCell?: BatchRecordCellLinkCellVO) {
  if (selectedTargetCell.value && !canUseTargetCellWithSource(selectedTargetCell.value, sourceCell)) {
    selectedTargetCell.value = undefined
  }
}
const sourceRuleKeys = computed(() => new Set(rules.value.map((rule) => `${rule.sourceReportId}:${rule.sourceCellKey}`)))
const targetRuleKeys = computed(() => new Set(rules.value.map((rule) => `${rule.targetReportId}:${rule.targetCellKey}`)))
const sourceLinkedRules = computed<SourceLinkedRule[]>(() =>
  rules.value
    .map((rule, ruleIndex) => ({ ...rule, ruleIndex }))
    .filter((rule) => {
      if (rule.enabled === false) return false
      const ruleSourceType = normalizeRuleSourceType(rule)
      if (isStructuredSourceSelected.value) {
        return ruleSourceType === sourceType.value
      }
      return ruleSourceType === SOURCE_TYPE_BATCH_RECORD_CELL && rule.sourceReportId === sourceReportId.value
    })
)
const sourceLinkCountText = computed(() => `${sourceLinkedRules.value.length} 个链接`)
const currentProcessPoolReportSourceTitle = computed(() =>
  selectedProcessPoolRouteProcess.value
    ? `${selectedProcessPoolRouteProcess.value.sort ?? '-'}. ${selectedProcessPoolRouteProcess.value.processName || '未命名工序'}的一线生产字段`
    : `${targetForm.value?.reportName || '当前工序'}的一线生产字段`
)
const currentPqcAggregateSourceTitle = computed(() =>
  selectedPqcQaProcess.value
    ? `${selectedPqcQaProcess.value.sort ?? '-'}. ${selectedPqcQaProcess.value.processName || '未命名工序'}的一线PQC字段`
    : '请选择工序后查看一线PQC字段'
)
const sourceSheetEmptyText = computed(() => {
  if (isProcessPoolReportSelected.value) {
    if (loading.value) {
      return '正在加载当前工序的一线生产字段'
    }
    if (!selectedProcessPoolDccProjectCodeId.value && !hasFormalRouteProcessContext.value) {
      return '请选择DCC项目代码'
    }
    if (activeTargetRouteProcessId.value === undefined) {
      return '请选择工序'
    }
    if (!filteredProcessPoolReportSourceFields.value.length) {
      return '当前工序暂无正式一线生产字段'
    }
    return '请选择源表单'
  }
  if (!isPqcAggregateSelected.value) {
    return '请选择源表单'
  }
  if (loading.value) {
    return '正在加载当前工序的一线PQC字段'
  }
  if (selectedPqcQaProcessId.value === undefined) {
    return '请选择QA规程工序'
  }
  if (!filteredPqcAggregateSourceFields.value.length) {
    return '当前工序暂无正式一线PQC字段'
  }
  return '请选择源表单'
})
const sourcePanelTitle = computed(() =>
  sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER
    ? PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME
    : sourceType.value === SOURCE_TYPE_PROCESS_POOL_REPORT
      ? currentProcessPoolReportSourceTitle.value
      : sourceType.value === SOURCE_TYPE_PRODUCTION_PICK_LIST
        ? PRODUCTION_PICK_LIST_SOURCE_REPORT_NAME
        : sourceType.value === SOURCE_TYPE_PQC_AGGREGATE_DETAIL
          ? currentPqcAggregateSourceTitle.value
      : sourceForm.value?.reportName || '未选择'
)

const openRelationDetailDialog = () => {
  relationDetailDialogVisible.value = true
}

const focusRelationRule = async (rule: SourceLinkedRule) => {
  if (rule.targetReportId && targetReportId.value !== rule.targetReportId) {
    targetReportId.value = rule.targetReportId
    selectedTargetCell.value = undefined
    await loadTargetCells()
  }
  relationDetailDialogVisible.value = false
}

const removeRuleByIndex = async (index: number) => {
  if (index < 0 || index >= rules.value.length) {
    return
  }
  const nextRules = rules.value.filter((_, ruleIndex) => ruleIndex !== index)
  await persistRules(nextRules, `单元格链接已删除，共 ${nextRules.length} 条。`)
}

const selectedSourceSummary = computed(() => {
  if (!selectedSourceCell.value) return '未选择源单元格'
  return `${sourcePanelTitle.value} / ${selectedSourceCell.value.label || selectedSourceCell.value.cellKey}`
})
const selectedTargetSummary = computed(() => {
  if (!selectedTargetCell.value) return '未选择目标单元格'
  return `${targetForm.value?.reportName || targetReportId.value} / ${selectedTargetCell.value.label || selectedTargetCell.value.cellKey}`
})

watch([selectedSourceSummary, selectedTargetSummary], () => {
  // 保留选择摘要计算，便于 Vue DevTools 和后续问题定位，不参与页面渲染。
}, { flush: 'post' })

const sourceRenderableSheet = computed(() =>
  buildRenderableSheet(sourceCells.value, 'source', selectedSourceCell.value, sourceRuleKeys.value)
)
const targetRenderableSheet = computed(() =>
  buildRenderableSheet(targetCells.value, 'target', selectedTargetCell.value, targetRuleKeys.value)
)

onMounted(loadWorkbenchContext)

watch(targetForms, (items) => {
  if (items.length && !items.some((item) => item.reportId === targetReportId.value)) {
    targetReportId.value = items[0].reportId
  }
})

const resolveDefaultSourceReportId = (defaultSourceReportId: string, requestedTargetReportId?: string) => {
  if (!requestedTargetReportId || defaultSourceReportId !== requestedTargetReportId) {
    return defaultSourceReportId
  }
  return forms.value.find((form) => form.reportId !== requestedTargetReportId)?.reportId || ''
}

async function loadWorkbenchContext() {
  loading.value = true
  try {
    const routeProcessIdForContext = isProcessPoolReportSelected.value
      ? selectedProcessPoolRouteProcessId.value ?? requestedTargetRouteProcessId
      : requestedTargetRouteProcessId
    const data = await BatchRecordCellLinkApi.getWorkbenchContext({
      routeId: parseNumber(route.query.routeId),
      definitionId: parseNumber(route.query.definitionId),
      versionId: parseNumber(route.query.versionId),
      sourceReportId: sourceReportId.value || String(route.query.sourceReportId || ''),
      templateId: parseNumber(route.query.templateId),
      versionNo: String(route.query.versionNo || ''),
      routeProcessId: routeProcessIdForContext,
      qaProcessId: selectedPqcQaProcessId.value,
      dccProjectCodeId: isProcessPoolReportSelected.value ? selectedProcessPoolDccProjectCodeId.value : undefined
    })
    context.value = data
    forms.value = data.forms || []
    if (isProcessPoolReportSelected.value && !processPoolDccProjectCodeInitialOptionsLoaded.value) {
      await loadProcessPoolDccProjectCodeOptions(requestedProcessPoolDccProjectCodeKeyword)
      processPoolDccProjectCodeInitialOptionsLoaded.value = true
    }
    if (isProcessPoolReportSelected.value && data.dccProjectCodeId) {
      selectedProcessPoolDccProjectCodeId.value = data.dccProjectCodeId
    }
    processPoolRouteProcesses.value = (data.routeProcesses || []).slice().sort((left, right) =>
      (left.sort ?? Number.MAX_SAFE_INTEGER) - (right.sort ?? Number.MAX_SAFE_INTEGER)
    )
    if (requestedTargetRouteProcessId !== undefined &&
      processPoolRouteProcesses.value.some((process) => Number(process.id) === Number(requestedTargetRouteProcessId))) {
      selectedProcessPoolRouteProcessId.value = requestedTargetRouteProcessId
    }
    pqcProcesses.value = (data.pqcProcesses || []).slice().sort((left, right) =>
      (left.sort ?? Number.MAX_SAFE_INTEGER) - (right.sort ?? Number.MAX_SAFE_INTEGER)
    )
    productionWorkOrderSourceFields.value = (data.sourceFields || []).filter(
      (field) => field.sourceType === SOURCE_TYPE_PRODUCTION_WORK_ORDER
    )
    processPoolReportSourceFields.value = (data.sourceFields || []).filter(
      (field) => field.sourceType === SOURCE_TYPE_PROCESS_POOL_REPORT
    )
    productionPickListSourceFields.value = (data.sourceFields || []).filter(
      (field) => field.sourceType === SOURCE_TYPE_PRODUCTION_PICK_LIST
    )
    pqcAggregateSourceFields.value = (data.sourceFields || []).filter(
      (field) => field.sourceType === SOURCE_TYPE_PQC_AGGREGATE_DETAIL
    )
    rules.value = data.rules || []
    const requestedTargetForm = forms.value.find((form) =>
      (!requestedTargetReportId || form.reportId === requestedTargetReportId) &&
      (routeProcessIdForContext === undefined || form.routeProcessId === undefined ||
        form.routeProcessId === null || Number(form.routeProcessId) === Number(routeProcessIdForContext))
    )
    if ((requestedTargetReportId || routeProcessIdForContext !== undefined) && !requestedTargetForm) {
      throw new Error(
        `当前路线工序未绑定目标批记录表单：routeProcessId=${routeProcessIdForContext || '-'}，reportId=${requestedTargetReportId || '-'}`
      )
    }
    const defaultSourceReportId = resolveDefaultSourceReportId(data.defaultSourceReportId || forms.value[0]?.reportId || '', requestedTargetForm?.reportId)
    sourceReportId.value = sourceReportId.value && sourceReportId.value !== requestedTargetForm?.reportId
      ? sourceReportId.value
      : defaultSourceReportId
    sourceType.value = resolveSourceTypeByReportId(sourceReportId.value)
    targetReportId.value = requestedTargetForm?.reportId || data.defaultTargetReportId || targetForms.value[0]?.reportId || ''
    sourceFieldCode.value = currentStructuredSourceFields()[0]?.fieldCode || ''
    await Promise.all([loadSourceCells(), loadTargetCells()])
  } catch (error) {
    message.error(resolveErrorMessage(error, '批记录单元格链接工作台加载失败。'))
  } finally {
    loading.value = false
  }
}

const loadProcessPoolDccProjectCodeOptions = async (keyword = '') => {
  processPoolDccProjectCodeLoading.value = true
  try {
    const page = await getProjectCodePage({
      pageNo: 1,
      pageSize: 20,
      keyword: keyword.trim() || undefined,
      status: DCC_PROJECT_CODE_STATUS_ENABLE,
      routeConfigured: true,
      mainBatchRecordConfigured: true
    })
    processPoolDccProjectCodeOptions.value = page.list || []
  } catch (error) {
    message.error(resolveErrorMessage(error, '加载DCC项目代码失败。'))
    throw error
  } finally {
    processPoolDccProjectCodeLoading.value = false
  }
}

const formatDccProjectCodeOption = (projectCode: DccProjectCodeRespVO) =>
  [projectCode.projectCode, projectCode.projectName, projectCode.id].filter(Boolean).join(' / ')

const handleProcessPoolDccProjectCodeChange = async () => {
  selectedProcessPoolRouteProcessId.value = undefined
  processPoolRouteProcesses.value = []
  processPoolReportSourceFields.value = []
  selectedSourceCell.value = undefined
  selectedTargetCell.value = undefined
  sourceFieldCode.value = ''
  sourceCells.value = undefined
  if (!selectedProcessPoolDccProjectCodeId.value) {
    await loadSourceCells()
    return
  }
  await loadWorkbenchContext()
}

const handleProcessPoolRouteProcessChange = async () => {
  selectedSourceCell.value = undefined
  selectedTargetCell.value = undefined
  sourceFieldCode.value = ''
  sourceCells.value = undefined
  if (!selectedProcessPoolRouteProcessId.value) {
    await loadSourceCells()
    return
  }
  await loadWorkbenchContext()
}

const handlePqcProcessChange = async () => {
  selectedSourceCell.value = undefined
  selectedTargetCell.value = undefined
  sourceFieldCode.value = ''
  sourceCells.value = undefined
  if (selectedPqcQaProcessId.value === undefined) {
    return
  }
  await loadWorkbenchContext()
}

const handleSourceSelectionChange = async () => {
  selectedSourceCell.value = undefined
  selectedTargetCell.value = undefined
  sourceCells.value = undefined
  sourceType.value = isProductionWorkOrderSelected.value
    ? SOURCE_TYPE_PRODUCTION_WORK_ORDER
    : isProcessPoolReportSelected.value
      ? SOURCE_TYPE_PROCESS_POOL_REPORT
    : isProductionPickListSelected.value
      ? SOURCE_TYPE_PRODUCTION_PICK_LIST
      : isPqcAggregateSelected.value
        ? SOURCE_TYPE_PQC_AGGREGATE_DETAIL
      : SOURCE_TYPE_BATCH_RECORD_CELL
  aggregationStrategy.value = ''
  lastSyncedProcessPoolAggregationSourceFieldCode.value = ''
  if (isStructuredSourceSelected.value) {
    sourceFieldCode.value = currentStructuredSourceFields()[0]?.fieldCode || ''
  }
  if (isProcessPoolReportSelected.value) {
    await loadProcessPoolDccProjectCodeOptions()
    if (selectedProcessPoolDccProjectCodeId.value || parseNumber(route.query.routeId) !== undefined) {
      await loadWorkbenchContext()
      return
    }
    processPoolRouteProcesses.value = []
    processPoolReportSourceFields.value = []
    sourceFieldCode.value = ''
    await Promise.all([loadSourceCells(), loadTargetCells()])
    return
  }
  if (isPqcAggregateSelected.value) {
    selectedPqcQaProcessId.value = undefined
    pqcAggregateSourceFields.value = []
    await loadWorkbenchContext()
    return
  }
  const requestedTargetForm = targetForms.value.find((form) =>
    (!requestedTargetReportId || form.reportId === requestedTargetReportId) &&
    (activeTargetRouteProcessId.value === undefined || form.routeProcessId === undefined ||
      form.routeProcessId === null || form.routeProcessId === activeTargetRouteProcessId.value)
  )
  if (requestedTargetForm) {
    targetReportId.value = requestedTargetForm.reportId
  }
  if (!targetForms.value.some((form) => form.reportId === targetReportId.value)) {
    targetReportId.value = targetForms.value[0]?.reportId || ''
  }
  await Promise.all([loadSourceCells(), loadTargetCells()])
}

const handleTargetReportChange = async () => {
  selectedTargetCell.value = undefined
  if (sourceType.value === SOURCE_TYPE_PROCESS_POOL_REPORT ||
    sourceType.value === SOURCE_TYPE_PRODUCTION_PICK_LIST ||
    sourceType.value === SOURCE_TYPE_PQC_AGGREGATE_DETAIL) {
    selectedSourceCell.value = undefined
    sourceFieldCode.value = currentStructuredSourceFields()[0]?.fieldCode || ''
    await Promise.all([loadSourceCells(), loadTargetCells()])
    return
  }
  await loadTargetCells()
}

const loadSourceCells = async () => {
  if (sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER) {
    sourceCells.value = buildProductionWorkOrderFieldCells(productionWorkOrderSourceFields.value)
    selectedSourceCell.value = sourceCells.value.cells.find((cell) => cell.sourceFieldCode === sourceFieldCode.value)
    return
  }
  if (sourceType.value === SOURCE_TYPE_PROCESS_POOL_REPORT) {
    sourceCells.value = buildSourceFieldCells(filteredProcessPoolReportSourceFields.value, PROCESS_POOL_REPORT_SOURCE_REPORT_ID,
      currentProcessPoolReportSourceTitle.value, SOURCE_TYPE_PROCESS_POOL_REPORT)
    selectedSourceCell.value = sourceCells.value.cells.find((cell) => cell.sourceFieldCode === sourceFieldCode.value)
    syncProcessPoolReportAggregationStrategy(selectedSourceCell.value)
    return
  }
  if (sourceType.value === SOURCE_TYPE_PRODUCTION_PICK_LIST) {
    sourceCells.value = buildSourceFieldCells(filteredProductionPickListSourceFields.value, PRODUCTION_PICK_LIST_SOURCE_REPORT_ID,
      PRODUCTION_PICK_LIST_SOURCE_REPORT_NAME,
      SOURCE_TYPE_PRODUCTION_PICK_LIST)
    selectedSourceCell.value = sourceCells.value.cells.find((cell) => cell.sourceFieldCode === sourceFieldCode.value)
    return
  }
  if (sourceType.value === SOURCE_TYPE_PQC_AGGREGATE_DETAIL) {
    sourceCells.value = buildSourceFieldCells(filteredPqcAggregateSourceFields.value, PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID,
      currentPqcAggregateSourceTitle.value,
      SOURCE_TYPE_PQC_AGGREGATE_DETAIL)
    selectedSourceCell.value = sourceCells.value.cells.find((cell) => cell.sourceFieldCode === sourceFieldCode.value)
    return
  }
  sourceCells.value = sourceReportId.value
    ? await BatchRecordCellLinkApi.getFormCells({
        reportId: sourceReportId.value,
        versionId: context.value?.batchRecordVersionId
      })
    : undefined
}

const loadTargetCells = async () => {
  targetCells.value = targetReportId.value
    ? await BatchRecordCellLinkApi.getFormCells({
        reportId: targetReportId.value,
        versionId: context.value?.batchRecordVersionId
      })
    : undefined
}

const selectSourceCell = (cell?: BatchRecordCellLinkCellVO) => {
  if (!cell) return
  if (!cell.linkableAsSource) {
    message.warning('该源单元格不是可填写单元格，不能作为自动带值来源。')
    return
  }
  selectedSourceCell.value = cell
  if (isStructuredSourceSelected.value && cell.sourceFieldCode) {
    sourceFieldCode.value = cell.sourceFieldCode
  }
  if (sourceType.value === SOURCE_TYPE_PROCESS_POOL_REPORT
  ) {
    syncProcessPoolReportAggregationStrategy(cell)
  }
  clearSelectedTargetCellIfSourceCannotUseIt(cell)
}

const selectTargetCell = (cell?: BatchRecordCellLinkCellVO) => {
  if (!cell) return
  if (!canUseTargetCellWithSource(cell)) {
    message.warning(isSignatureTargetCell(cell)
      ? '请先选择一线生产的签名用户字段，再链接到签名位。'
      : '目标单元格不是可填写单元格，不能自动带值。')
    return
  }
  selectedTargetCell.value = cell
}

const setRepeatTemplateFromSelectedTarget = () => {
  if (!selectedTargetCell.value) return
  repeatTemplateStartRowIndex.value = selectedTargetCell.value.rowIndex
  repeatTemplateEndRowIndex.value = selectedTargetCell.value.rowIndex
  includeSelectedTargetRowInRepeatArea()
}

const includeSelectedTargetRowInRepeatArea = () => {
  if (!selectedTargetCell.value) return
  const rowIndex = selectedTargetCell.value.rowIndex
  repeatAreaStartRowIndex.value = repeatAreaStartRowIndex.value === undefined ? rowIndex : Math.min(repeatAreaStartRowIndex.value, rowIndex)
  repeatAreaEndRowIndex.value = repeatAreaEndRowIndex.value === undefined ? rowIndex : Math.max(repeatAreaEndRowIndex.value, rowIndex)
}

const createRepeatRowMapping = () => {
  if (!selectedSourceCell.value || !selectedTargetCell.value || repeatTemplateStartRowIndex.value === undefined) return
  const templateTargetCellKey = selectedTargetCell.value.cellKey
  const duplicate = repeatRowGroupMappings.value.some((mapping) =>
    mapping.templateTargetCellKey === templateTargetCellKey || mapping.sourceFieldCode === selectedSourceCell.value?.sourceFieldCode
  )
  if (duplicate) {
    message.warning('重复行组模板链接已存在。')
    return
  }
  const previewMapping = {
    templateTargetRowIndex: selectedTargetCell.value.rowIndex,
    templateTargetColumnIndex: selectedTargetCell.value.columnIndex
  } as BatchRecordRepeatRowGroupMappingVO
  repeatRowGroupMappings.value = [...repeatRowGroupMappings.value, {
    sourceType: SOURCE_TYPE_PROCESS_POOL_REPORT,
    sourceFieldCode: selectedSourceCell.value.sourceFieldCode || selectedSourceCell.value.cellKey,
    sourceFieldName: selectedSourceCell.value.sourceFieldName || selectedSourceCell.value.label,
    sourceValueType: selectedSourceCell.value.valueType,
    templateTargetRowIndex: selectedTargetCell.value.rowIndex,
    templateTargetColumnIndex: selectedTargetCell.value.columnIndex,
    templateTargetCellKey,
    targetValueType: selectedTargetCell.value.valueType,
    projectionTargetCellKey: projectionTargetCellKey(repeatRowGroupCandidateRecords.value[0], previewMapping)
  }]
}

const projectionTargetCellKey = (record: BatchRecordRepeatRowGroupRecordVO | undefined, mapping: BatchRecordRepeatRowGroupMappingVO) => {
  if (!record || repeatTemplateStartRowIndex.value === undefined) return '-'
  const rowOffset = mapping.templateTargetRowIndex - repeatTemplateStartRowIndex.value
  return String(record.startRowIndex + rowOffset) + ':' + String(mapping.templateTargetColumnIndex)
}

const saveRepeatRowGroup = async () => {
  if (activeTargetRouteProcessId.value === undefined || repeatTemplateStartRowIndex.value === undefined || repeatTemplateEndRowIndex.value === undefined ||
    repeatAreaStartRowIndex.value === undefined || repeatAreaEndRowIndex.value === undefined || saving.value) {
    message.warning('请先选择当前工序的模板记录、重复区域和模板链接。')
    return
  }
  saving.value = true
  try {
    const result = await BatchRecordCellLinkApi.saveRepeatRowGroup({
      scopeType: context.value?.scopeType,
      scopeId: context.value?.scopeId,
      routeId: context.value?.routeId,
      batchRecordDefinitionId: context.value?.batchRecordDefinitionId,
      batchRecordVersionId: context.value?.batchRecordVersionId,
      routeProcessId: activeTargetRouteProcessId.value,
      targetReportId: targetReportId.value,
      templateStartRowIndex: repeatTemplateStartRowIndex.value,
      templateEndRowIndex: repeatTemplateEndRowIndex.value,
      repeatAreaStartRowIndex: repeatAreaStartRowIndex.value,
      repeatAreaEndRowIndex: repeatAreaEndRowIndex.value,
      records: repeatRowGroupCandidateRecords.value,
      mappings: repeatRowGroupMappings.value,
      enabled: true
    })
    repeatRowGroups.value = [result, ...repeatRowGroups.value.filter((group) => group.targetReportId !== result.targetReportId)]
    message.success('重复行组对应关系已保存。')
  } catch (error) {
    message.error(resolveErrorMessage(error, '重复行组保存失败。'))
  } finally {
    saving.value = false
  }
}

const createRule = async () => {
  if (!selectedSourceCell.value || !selectedTargetCell.value || !targetForm.value || saving.value) return
  const isProductionWorkOrderSource = sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER
  const isProcessPoolReportSource = sourceType.value === SOURCE_TYPE_PROCESS_POOL_REPORT
  const isProductionPickListSource = sourceType.value === SOURCE_TYPE_PRODUCTION_PICK_LIST
  const isPqcAggregateSource = sourceType.value === SOURCE_TYPE_PQC_AGGREGATE_DETAIL
  const isStructuredSource = isProductionWorkOrderSource || isProcessPoolReportSource ||
    isProductionPickListSource || isPqcAggregateSource
  if (!isStructuredSource && !sourceForm.value) return
  if (isStructuredSource && activeTargetRouteProcessId.value === undefined) {
    message.warning(isProcessPoolReportSource ? '请先选择DCC项目代码和工序。' : '请从具体工序进入批记录链接页后再配置该来源。')
    return
  }
  if (isProcessPoolReportSource && !aggregationStrategy.value) {
    message.warning('请选择多笔报工的汇总方式。')
    return
  }
  const sourceKey = selectedSourceCell.value.cellKey
  const sourceReportIdForPayload = isProductionWorkOrderSource
    ? PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID
    : isProcessPoolReportSource
      ? PROCESS_POOL_REPORT_SOURCE_REPORT_ID
      : isProductionPickListSource
        ? PRODUCTION_PICK_LIST_SOURCE_REPORT_ID
        : isPqcAggregateSource
          ? PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID
        : sourceReportId.value
  const sourceReportNameForPayload = isProductionWorkOrderSource
    ? PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME
    : isProcessPoolReportSource
      ? PROCESS_POOL_REPORT_SOURCE_REPORT_NAME
      : isProductionPickListSource
        ? PRODUCTION_PICK_LIST_SOURCE_REPORT_NAME
        : isPqcAggregateSource
          ? PQC_AGGREGATE_DETAIL_SOURCE_REPORT_NAME
        : sourceForm.value?.reportName
  const targetKey = selectedTargetCell.value.cellKey
  const duplicatePair = rules.value.some(
    (rule) =>
      normalizeRuleSourceType(rule) === sourceType.value &&
      rule.sourceReportId === sourceReportIdForPayload &&
      rule.sourceCellKey === sourceKey &&
      rule.targetReportId === targetReportId.value &&
      rule.targetCellKey === targetKey
  )
  if (duplicatePair) {
    message.warning('该源单元格到目标单元格的链接已存在。')
    return
  }
  const duplicateTarget = rules.value.some(
    (rule) => rule.targetReportId === targetReportId.value && rule.targetCellKey === targetKey
  )
  if (duplicateTarget) {
    message.warning('同一目标单元格只能配置一个来源。')
    return
  }
  const nextRules = [...rules.value, {
    scopeType: context.value?.scopeType,
    scopeId: context.value?.scopeId,
    routeId: context.value?.routeId,
    routeProcessId: activeTargetRouteProcessId.value,
    batchRecordDefinitionId: context.value?.batchRecordDefinitionId,
    batchRecordVersionId: context.value?.batchRecordVersionId,
    sourceType: sourceType.value,
    sourceReportId: sourceReportIdForPayload,
    sourceReportName: sourceReportNameForPayload,
    sourceRowIndex: selectedSourceCell.value.rowIndex,
    sourceColumnIndex: selectedSourceCell.value.columnIndex,
    sourceCellKey: sourceKey,
    sourceFieldCode: selectedSourceCell.value.sourceFieldCode,
    sourceFieldName: selectedSourceCell.value.sourceFieldName,
    sourceLabel: selectedSourceCell.value.label,
    sourceValueType: selectedSourceCell.value.valueType,
    targetReportId: targetReportId.value,
    targetReportName: targetForm.value.reportName,
    targetRowIndex: selectedTargetCell.value.rowIndex,
    targetColumnIndex: selectedTargetCell.value.columnIndex,
    targetCellKey: targetKey,
    targetLabel: selectedTargetCell.value.label,
    targetValueType: selectedTargetCell.value.valueType,
    aggregationStrategy: isProcessPoolReportSource ? aggregationStrategy.value : undefined,
    overwritePolicy: 'ONLY_WHEN_EMPTY',
    enabled: true
  }]
  const saved = await persistRules(nextRules, `单元格链接已建立并保存，共 ${nextRules.length} 条。`)
  if (saved) {
    selectedTargetCell.value = undefined
  }
}

function normalizeRuleSourceType(rule: BatchRecordCellLinkRuleVO) {
  return rule.sourceType || SOURCE_TYPE_BATCH_RECORD_CELL
}

function buildProductionWorkOrderFieldCells(
  fields: BatchRecordCellLinkSourceFieldVO[]
): BatchRecordCellLinkFormCellsVO {
  return buildSourceFieldCells(fields, PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID,
    PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME, SOURCE_TYPE_PRODUCTION_WORK_ORDER)
}

function currentStructuredSourceFields() {
  if (sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER) {
    return productionWorkOrderSourceFields.value
  }
  if (sourceType.value === SOURCE_TYPE_PROCESS_POOL_REPORT) {
    return filteredProcessPoolReportSourceFields.value
  }
  if (sourceType.value === SOURCE_TYPE_PRODUCTION_PICK_LIST) {
    return filteredProductionPickListSourceFields.value
  }
  if (sourceType.value === SOURCE_TYPE_PQC_AGGREGATE_DETAIL) {
    return filteredPqcAggregateSourceFields.value
  }
  return []
}

function buildSourceFieldCells(
  fields: BatchRecordCellLinkSourceFieldVO[],
  reportId: string,
  reportName: string,
  structuredSourceType: string
): BatchRecordCellLinkFormCellsVO {
  const rows = fields.reduce<Record<string, { height: number; cells: Record<string, { text: string; fillForm: unknown }> }>>(
    (acc, field, index) => {
      const displayName = resolveSourceFieldDisplayName(field, structuredSourceType)
      acc[String(index)] = {
        height: DEFAULT_ROW_HEIGHT,
        cells: {
          0: {
            text: displayName,
            fillForm: { field: field.fieldCode }
          }
        }
      }
      return acc
    },
    {}
  )
  return {
    reportId,
    reportName,
    sheetLayoutJson: JSON.stringify({ cols: { 0: { width: 240 } }, rows }),
    cells: fields.map((field, index) => {
      const displayName = resolveSourceFieldDisplayName(field, structuredSourceType)
      return {
        rowIndex: index,
        columnIndex: 0,
        cellKey: field.sourceCellKey || field.fieldCode,
        sourceType: structuredSourceType,
        sourceFieldCode: field.fieldCode,
        sourceFieldName: displayName,
        label: displayName,
        valueType: field.valueType || 'STRING',
        readonly: false,
        signatureCell: false,
        linkableAsSource: true,
        linkableAsTarget: false
      }
    })
  }
}

function resolveSourceFieldDisplayName(field: BatchRecordCellLinkSourceFieldVO, structuredSourceType: string) {
  if (structuredSourceType !== SOURCE_TYPE_PQC_AGGREGATE_DETAIL) {
    return field.fieldName
  }
  const readableSuffix = field.fieldCode.endsWith('|inspectorUserId')
    ? '填写人签名'
    : field.fieldCode.endsWith('|inspectedAt')
      ? '填写时间'
      : field.fieldCode.endsWith('|reviewerUserId')
        ? '复核人签名'
        : field.fieldCode.endsWith('|reviewedAt')
          ? '复核时间'
          : undefined
  if (!readableSuffix) {
    return field.fieldName
  }
  const separatorIndex = field.fieldName.lastIndexOf(' / ')
  return separatorIndex >= 0
    ? `${field.fieldName.slice(0, separatorIndex)} / ${readableSuffix}`
    : readableSuffix
}

const persistRules = async (nextRules: BatchRecordCellLinkRuleVO[], successMessage: string) => {
  if (!context.value || saving.value) return false
  saving.value = true
  try {
    const result = await BatchRecordCellLinkApi.saveRules({
      scopeType: context.value.scopeType,
      scopeId: context.value.scopeId,
      routeId: context.value.routeId,
      routeProcessId: activeTargetRouteProcessId.value,
      batchRecordDefinitionId: context.value.batchRecordDefinitionId,
      batchRecordVersionId: context.value.batchRecordVersionId,
      rules: nextRules
    })
    rules.value = result.rules || []
    message.success(successMessage)
    return true
  } catch (error) {
    message.error(resolveErrorMessage(error, '单元格链接规则保存失败。'))
    return false
  } finally {
    saving.value = false
  }
}

const goBack = async () => {
  const returnTo = String(route.query.returnTo || '')
  const returnLabel = String(route.query.returnLabel || '')
  void returnLabel
  if (returnTo) {
    await router.push(returnTo)
    return
  }
  await router.push({ path: '/mes/pro/batch-record-form-list' })
}

function buildRenderableSheet(
  formCells: BatchRecordCellLinkFormCellsVO | undefined,
  mode: 'source' | 'target',
  selectedCell: BatchRecordCellLinkCellVO | undefined,
  linkedKeys: Set<string>
): RenderedSheet {
  if (!formCells?.sheetLayoutJson) return { columns: [], rows: [] }
  let layout: CellLinkRawLayout
  try {
    layout = JSON.parse(formCells.sheetLayoutJson) as CellLinkRawLayout
  } catch {
    return { columns: [], rows: [] }
  }
  const rows = layout.rows || {}
  const rowIndexes = Object.keys(rows)
    .map(Number)
    .filter(Number.isInteger)
    .sort((left, right) => left - right)
  const columnIndexes = collectRenderedColumnIndexes(layout, rowIndexes, formCells.cells)
  const columns = buildRenderedColumns(layout, columnIndexes)
  const covered = new Set<string>()
  const cellMetaMap = new Map<string, BatchRecordCellLinkCellVO>()
  formCells.cells.forEach((cell) => {
    cellMetaMap.set(cell.cellKey, cell)
    cellMetaMap.set(`${cell.rowIndex}:${cell.columnIndex}`, cell)
  })
  const renderedRows = rowIndexes.map((rowIndex) => {
    const rawRow = rows[String(rowIndex)] || {}
    const cells: RenderedCell[] = []
    columnIndexes.forEach((columnIndex) => {
      const key = `${rowIndex}:${columnIndex}`
      if (covered.has(key)) return
      const rawCell = rawRow.cells?.[String(columnIndex)]
      const merge = normalizeTemplateCellMerge(rawCell)
      for (let rowOffset = 0; rowOffset < merge.rowSpan; rowOffset += 1) {
        for (let columnOffset = 0; columnOffset < merge.colSpan; columnOffset += 1) {
          if (rowOffset || columnOffset) {
            covered.add(`${rowIndex + rowOffset}:${columnIndex + columnOffset}`)
          }
        }
      }
      const meta = cellMetaMap.get(key)
      const reportCellKey = `${formCells.reportId}:${meta?.cellKey || key}`
      const isSelectedCell = Boolean(
        selectedCell &&
          (selectedCell.cellKey === key ||
            (selectedCell.rowIndex === rowIndex && selectedCell.columnIndex === columnIndex))
      )
      const rawText = stringifyTemplateCell(rawCell?.value ?? rawCell?.text)
      const isFillableCell = Boolean(meta || rawCell?.fillForm)
      const text = normalizeRenderedCellText(rawText, isFillableCell)
      const targetSignatureCell = isSignatureTargetCell(meta)
      const targetSelectable = mode === 'target' && canUseTargetCellWithSource(meta)
      cells.push({
        key,
        rowIndex,
        columnIndex,
        text,
        rowSpan: merge.rowSpan,
        colSpan: merge.colSpan,
        cellMeta: meta,
        classNames: {
          'batch-record-cell-link-sheet__cell': true,
          'is-empty': !rawCell || !text,
          'is-static-cell': Boolean(rawCell) && !meta,
          'is-fillable-cell': isFillableCell,
          'is-section-title': isRenderedSectionTitle(text, merge, columnIndexes.length),
          'is-source-selectable': mode === 'source' && Boolean(meta?.linkableAsSource),
          'is-target-selectable': targetSelectable,
          'is-linked': linkedKeys.has(reportCellKey),
          'is-selected': isSelectedCell
        },
        dataAttrs: {
          'data-cell-key': meta?.cellKey || key,
          'data-cell-row-index': String(rowIndex),
          'data-cell-column-index': String(columnIndex),
          'data-cell-signature-cell': targetSignatureCell ? 'true' : undefined
        }
      })
    })
    return {
      rowIndex,
      height: normalizeRenderedRowHeight(rawRow.height),
      cells
    }
  })
  return { columns, rows: renderedRows }
}

function collectRenderedColumnIndexes(
  layout: CellLinkRawLayout,
  rowIndexes: number[],
  cells: BatchRecordCellLinkCellVO[]
): number[] {
  const columnSet = new Set<number>()
  const layoutColumns = layout.cols || {}
  Object.keys(layoutColumns).forEach((columnKey) => {
    const columnIndex = Number(columnKey)
    if (Number.isInteger(columnIndex)) columnSet.add(columnIndex)
  })
  rowIndexes.forEach((rowIndex) => {
    const rawCells = layout.rows?.[String(rowIndex)]?.cells || {}
    Object.entries(rawCells).forEach(([columnKey, rawCell]) => {
      const columnIndex = Number(columnKey)
      if (!Number.isInteger(columnIndex)) return
      const merge = normalizeTemplateCellMerge(rawCell)
      for (let offset = 0; offset < merge.colSpan; offset += 1) {
        columnSet.add(columnIndex + offset)
      }
    })
  })
  cells.forEach((cell) => {
    const columnIndex = Number(cell.columnIndex)
    if (Number.isInteger(columnIndex)) columnSet.add(columnIndex)
  })
  return Array.from(columnSet).sort((left, right) => left - right)
}

function buildRenderedColumns(layout: CellLinkRawLayout, columnIndexes: number[]): RenderedColumn[] {
  const layoutColumns = layout.cols || {}
  const columnWidths = columnIndexes.map((columnIndex) => {
    const width = Number(layoutColumns[String(columnIndex)]?.width)
    return Number.isFinite(width) && width > 0 ? width : DEFAULT_COLUMN_WIDTH
  })
  const totalWidth = columnWidths.reduce((sum, width) => sum + width, 0)
  return columnIndexes.map((columnIndex, index) => ({
    columnIndex,
    widthPercent: totalWidth > 0 ? (columnWidths[index] / totalWidth) * 100 : 100 / columnIndexes.length
  }))
}

function normalizeRenderedRowHeight(height: unknown) {
  const parsed = Number(height)
  return Number.isFinite(parsed) && parsed > 0 ? Math.max(parsed, 24) : DEFAULT_ROW_HEIGHT
}

function normalizeRenderedCellText(text: string, isFillableCell: boolean) {
  if (!isFillableCell) return text
  return text.trim() === '填' || !text.trim() ? EMPTY_FILLABLE_PLACEHOLDER : text
}

function isRenderedSectionTitle(text: string, merge: { rowSpan: number; colSpan: number }, columnCount: number) {
  return Boolean(text.trim()) && merge.colSpan >= Math.max(3, Math.floor(columnCount / 2))
}

function parseNumber(value: unknown): number | undefined {
  const text = Array.isArray(value) ? value[0] : value
  const parsed = Number(text)
  return Number.isFinite(parsed) ? parsed : undefined
}

function resolveSourceTypeByReportId(reportId: string) {
  return reportId === PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID
    ? SOURCE_TYPE_PRODUCTION_WORK_ORDER
    : reportId === PROCESS_POOL_REPORT_SOURCE_REPORT_ID
      ? SOURCE_TYPE_PROCESS_POOL_REPORT
      : reportId === PRODUCTION_PICK_LIST_SOURCE_REPORT_ID
        ? SOURCE_TYPE_PRODUCTION_PICK_LIST
        : reportId === PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID
          ? SOURCE_TYPE_PQC_AGGREGATE_DETAIL
          : SOURCE_TYPE_BATCH_RECORD_CELL
}

function isProcessPoolDeviceSourceField(field: BatchRecordCellLinkSourceFieldVO) {
  return field.fieldCode.startsWith('selectedDevice.') ||
    field.fieldCode.startsWith('deviceMeteringValidity.') ||
    isProcessPoolDeviceParameterValueSourceField(field)
}

function isProcessPoolDeviceParameterValueSourceField(field: BatchRecordCellLinkSourceFieldVO) {
  return field.fieldCode.startsWith('deviceParameterReadings.') &&
    field.fieldCode.includes('.value@deviceGroup:')
}

function isProcessPoolDeviceParameterMetadataSourceField(field: BatchRecordCellLinkSourceFieldVO) {
  return field.fieldCode.startsWith('equipmentParameterRules.') ||
    (field.fieldCode.startsWith('deviceParameterReadings.') &&
      !isProcessPoolDeviceParameterValueSourceField(field))
}

function isProcessPoolDeviceGroupSourceField(field: BatchRecordCellLinkSourceFieldVO) {
  return isProcessPoolDeviceSourceField(field) && field.fieldCode.includes('@deviceGroup:')
}

function resolveErrorMessage(error: unknown, fallback: string) {
  const candidate = error as { message?: string; msg?: string }
  return candidate?.msg || candidate?.message || fallback
}
</script>

<style scoped>
.batch-record-cell-link {
  height: calc(100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding) - var(--app-content-padding) - 2px);
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  background: #f4f7fb;
  color: #172033;
}

.batch-record-cell-link__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-bottom: 1px solid #dbe3ef;
  background: #ffffff;
}

.batch-record-cell-link__title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.batch-record-cell-link__title strong {
  font-size: 18px;
}

.batch-record-cell-link__title span,
.batch-record-cell-link__detail-summary span {
  color: #6b7280;
  font-size: 13px;
}

.batch-record-cell-link__controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
}

.batch-record-cell-link__mode-select {
  width: 136px;
}

.batch-record-cell-link__select {
  min-width: 0;
  width: 260px;
}

.batch-record-cell-link__target-select {
  width: 320px;
}

.batch-record-cell-link__aggregation-select {
  width: 180px;
}

.batch-record-cell-link__source-link-count {
  min-width: 104px;
  color: #1677ff;
  border-color: #b9d7ff;
  background: #f4f9ff;
}

.batch-record-cell-link__repeat-panel {
  display: grid;
  gap: 8px;
  padding: 10px 14px;
  border-top: 1px solid #dbe3ef;
  background: #ffffff;
}

.batch-record-cell-link__repeat-panel > div {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-record-cell-link__repeat-panel span {
  color: #6b7280;
}

.batch-record-cell-link__projection {
  display: inline-block;
  margin-right: 10px;
  color: #0f766e;
}

.batch-record-cell-link__detail-summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: baseline;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fafc;
}

.batch-record-cell-link__detail-summary strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #172033;
}

.batch-record-cell-link__detail-summary em {
  padding: 2px 6px;
  border-radius: 5px;
  background: #eaf5ff;
  color: #1677ff;
  font-style: normal;
  font-size: 12px;
}

.batch-record-cell-link__form-stage {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 1px;
  overflow: hidden;
  background: #dbe3ef;
}

.batch-record-cell-link__pane {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: 44px minmax(0, 1fr);
  overflow: hidden;
  background: #ffffff;
}

.batch-record-cell-link__process-pool-source-panel,
.batch-record-cell-link__pqc-source-panel {
  grid-template-rows: 44px auto minmax(0, 1fr);
}

.batch-record-cell-link__process-pool-selector,
.batch-record-cell-link__pqc-process-selector {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(220px, 1fr);
  gap: 12px;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid #e5edf7;
  background: #f8fbff;
}

.batch-record-cell-link__process-pool-selector-main,
.batch-record-cell-link__pqc-process-selector-main,
.batch-record-cell-link__pqc-process-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.batch-record-cell-link__process-pool-selector-main > span,
.batch-record-cell-link__pqc-process-selector-main > span,
.batch-record-cell-link__pqc-process-meta > span {
  flex: none;
  color: #6b7280;
  font-size: 12px;
}

.batch-record-cell-link__process-pool-select,
.batch-record-cell-link__pqc-process-select {
  min-width: 0;
  flex: 1;
}

.batch-record-cell-link__pqc-process-meta strong {
  min-width: 0;
  overflow: hidden;
  color: #1677ff;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-cell-link__work-order-field-panel {
  background: linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
}

.batch-record-cell-link__pane-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 14px;
  border-bottom: 1px solid #e5edf7;
  background: #f8fafc;
}

.batch-record-cell-link__pane-title span {
  color: #6b7280;
  font-size: 13px;
}

.batch-record-cell-link__sheet-scroll {
  min-height: 0;
  min-width: 0;
  overflow-x: auto;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 8px;
  background: #f8fafc;
}

.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet) {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  background: #ffffff;
  font-size: 12px;
  line-height: 1.25;
  color: #111827;
}

.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell) {
  position: relative;
  min-width: 0;
  padding: 2px 3px;
  border: 1px solid #1f2937;
  text-align: center;
  vertical-align: middle;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell.is-static-cell) {
  background: #f3f4f6;
  font-weight: 600;
}

.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell.is-fillable-cell) {
  background: #f8fffd;
  color: #0f766e;
  font-weight: 500;
}

.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell.is-section-title) {
  background: #e5e7eb;
  font-weight: 700;
}

.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell.is-empty) {
  color: #8a94a6;
  font-weight: 400;
}

.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell.is-source-selectable),
.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell.is-target-selectable) {
  cursor: pointer;
  box-shadow: inset 0 0 0 1px rgba(15, 118, 110, 0.3);
}

.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell.is-source-selectable:hover),
.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell.is-target-selectable:hover) {
  box-shadow: inset 0 0 0 2px #1677ff;
}

.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell.is-linked) {
  background: #eaf5ff;
}

.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell.is-selected) {
  box-shadow: inset 0 0 0 2px #1677ff;
}

.batch-record-cell-link__empty {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a94a6;
  border: 1px dashed #dbe3ef;
  background: #f8fafc;
}

@media (max-width: 1280px) {
  .batch-record-cell-link {
    grid-template-rows: 104px minmax(0, 1fr);
  }
}
</style>
