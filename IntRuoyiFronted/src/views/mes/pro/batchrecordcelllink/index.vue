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
            v-model="sourceReportId"
            filterable
            placeholder="选择来源"
            class="batch-record-cell-link__select batch-record-cell-link__source-select"
            @change="handleSourceSelectionChange"
          >
            <el-option label="生产工单" :value="PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID" />
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
          <el-button
            class="batch-record-cell-link__source-link-count"
            plain
            aria-label="查看源表单链接详情"
            @click="openRelationDetailDialog"
          >
            {{ sourceLinkCountText }}
          </el-button>
          <el-button
            class="batch-record-cell-link__create-button"
            type="success"
            :loading="saving"
            :disabled="!canCreateRule || saving"
            @click="createRule"
          >
            建立链接
          </el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </header>

      <main class="batch-record-cell-link__form-stage">
        <section
          class="batch-record-cell-link__pane is-source"
          :class="{ 'batch-record-cell-link__work-order-field-panel': sourceType === SOURCE_TYPE_PRODUCTION_WORK_ORDER }"
        >
          <div class="batch-record-cell-link__pane-title">
            <span>{{ sourceType === SOURCE_TYPE_PRODUCTION_WORK_ORDER ? '源字段' : '源表单' }}</span>
            <strong>{{ sourcePanelTitle }}</strong>
          </div>
          <div class="batch-record-cell-link__sheet-scroll">
            <BatchRecordLinkSheet
              :columns="sourceRenderableSheet.columns"
              :rows="sourceRenderableSheet.rows"
              empty-text="请选择源表单"
              @select-cell="selectSourceCell"
            />
          </div>
        </section>
        <section class="batch-record-cell-link__pane is-target">
          <div class="batch-record-cell-link__pane-title">
            <span>目标表单</span>
            <strong>{{ targetForm?.reportName || '未选择' }}</strong>
          </div>
          <div class="batch-record-cell-link__sheet-scroll">
            <BatchRecordLinkSheet
              :columns="targetRenderableSheet.columns"
              :rows="targetRenderableSheet.rows"
              empty-text="请选择目标表单"
              @select-cell="selectTargetCell"
            />
          </div>
        </section>
      </main>

      <el-dialog
        v-model="relationDetailDialogVisible"
        title="源表单链接详情"
        width="920px"
        class="batch-record-cell-link__detail-dialog"
      >
        <div class="batch-record-cell-link__detail-summary">
          <span>{{ sourceType === SOURCE_TYPE_PRODUCTION_WORK_ORDER ? '当前源字段集合' : '当前源表单' }}</span>
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
            <template #default="{ row }">{{ row.overwritePolicy || 'ONLY_WHEN_EMPTY' }}</template>
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
  type BatchRecordCellLinkRuleVO,
  type BatchRecordCellLinkSourceFieldVO,
  type BatchRecordCellLinkWorkbenchContextVO
} from '@/api/mes/pro/batchrecordcelllink'
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
const PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID = 'PRODUCTION_WORK_ORDER'
const PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME = '生产工单'

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

const loading = ref(false)
const saving = ref(false)
const context = ref<BatchRecordCellLinkWorkbenchContextVO>()
const forms = ref<BatchRecordCellLinkFormVO[]>([])
const productionWorkOrderSourceFields = ref<BatchRecordCellLinkSourceFieldVO[]>([])
const rules = ref<BatchRecordCellLinkRuleVO[]>([])
const sourceType = ref(SOURCE_TYPE_BATCH_RECORD_CELL)
const sourceReportId = ref('')
const sourceFieldCode = ref('')
const targetReportId = ref('')
const sourceCells = ref<BatchRecordCellLinkFormCellsVO>()
const targetCells = ref<BatchRecordCellLinkFormCellsVO>()
const selectedSourceCell = ref<BatchRecordCellLinkCellVO>()
const selectedTargetCell = ref<BatchRecordCellLinkCellVO>()
const relationDetailDialogVisible = ref(false)

const isProductionWorkOrderSelected = computed(() => sourceReportId.value === PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID)
const sourceForm = computed(() =>
  isProductionWorkOrderSelected.value
    ? undefined
    : forms.value.find((form) => form.reportId === sourceReportId.value)
)
const targetForm = computed(() => forms.value.find((form) => form.reportId === targetReportId.value))
const targetForms = computed(() => {
  if (isProductionWorkOrderSelected.value) {
    return forms.value
  }
  const candidates = forms.value.filter((form) => form.reportId !== sourceReportId.value)
  return candidates.length ? candidates : forms.value
})
const canCreateRule = computed(() => Boolean(selectedSourceCell.value && selectedTargetCell.value && targetReportId.value))
const sourceRuleKeys = computed(() => new Set(rules.value.map((rule) => `${rule.sourceReportId}:${rule.sourceCellKey}`)))
const targetRuleKeys = computed(() => new Set(rules.value.map((rule) => `${rule.targetReportId}:${rule.targetCellKey}`)))
const sourceLinkedRules = computed<SourceLinkedRule[]>(() =>
  rules.value
    .map((rule, ruleIndex) => ({ ...rule, ruleIndex }))
    .filter((rule) => {
      if (rule.enabled === false) return false
      const ruleSourceType = normalizeRuleSourceType(rule)
      return sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER
        ? ruleSourceType === SOURCE_TYPE_PRODUCTION_WORK_ORDER
        : ruleSourceType === SOURCE_TYPE_BATCH_RECORD_CELL && rule.sourceReportId === sourceReportId.value
    })
)
const sourceLinkCountText = computed(() => `${sourceLinkedRules.value.length} 个链接`)
const sourcePanelTitle = computed(() =>
  sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER
    ? PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME
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

async function loadWorkbenchContext() {
  loading.value = true
  try {
    const data = await BatchRecordCellLinkApi.getWorkbenchContext({
      routeId: parseNumber(route.query.routeId),
      definitionId: parseNumber(route.query.definitionId),
      versionId: parseNumber(route.query.versionId),
      sourceReportId: String(route.query.sourceReportId || '')
    })
    context.value = data
    forms.value = data.forms || []
    productionWorkOrderSourceFields.value = data.sourceFields || []
    rules.value = data.rules || []
    sourceType.value = SOURCE_TYPE_BATCH_RECORD_CELL
    sourceReportId.value = data.defaultSourceReportId || forms.value[0]?.reportId || ''
    sourceFieldCode.value = productionWorkOrderSourceFields.value[0]?.fieldCode || ''
    targetReportId.value = data.defaultTargetReportId || targetForms.value[0]?.reportId || ''
    await Promise.all([loadSourceCells(), loadTargetCells()])
  } catch (error) {
    message.error(resolveErrorMessage(error, '批记录单元格链接工作台加载失败。'))
  } finally {
    loading.value = false
  }
}

const handleSourceSelectionChange = async () => {
  selectedSourceCell.value = undefined
  selectedTargetCell.value = undefined
  sourceType.value = isProductionWorkOrderSelected.value
    ? SOURCE_TYPE_PRODUCTION_WORK_ORDER
    : SOURCE_TYPE_BATCH_RECORD_CELL
  if (sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER && !sourceFieldCode.value) {
    sourceFieldCode.value = productionWorkOrderSourceFields.value[0]?.fieldCode || ''
  }
  if (!targetForms.value.some((form) => form.reportId === targetReportId.value)) {
    targetReportId.value = targetForms.value[0]?.reportId || ''
  }
  await Promise.all([loadSourceCells(), loadTargetCells()])
}

const handleTargetReportChange = async () => {
  selectedTargetCell.value = undefined
  await loadTargetCells()
}

const loadSourceCells = async () => {
  if (sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER) {
    sourceCells.value = buildProductionWorkOrderFieldCells(productionWorkOrderSourceFields.value)
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
  if (sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER && cell.sourceFieldCode) {
    sourceFieldCode.value = cell.sourceFieldCode
  }
}

const selectTargetCell = (cell?: BatchRecordCellLinkCellVO) => {
  if (!cell) return
  if (!cell.linkableAsTarget) {
    message.warning('目标单元格不是可填写单元格或属于签名位，不能自动带值。')
    return
  }
  selectedTargetCell.value = cell
}

const createRule = async () => {
  if (!selectedSourceCell.value || !selectedTargetCell.value || !targetForm.value || saving.value) return
  const isProductionWorkOrderSource = sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER
  if (!isProductionWorkOrderSource && !sourceForm.value) return
  const sourceKey = selectedSourceCell.value.cellKey
  const sourceReportIdForPayload = isProductionWorkOrderSource
    ? PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID
    : sourceReportId.value
  const sourceReportNameForPayload = isProductionWorkOrderSource
    ? PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME
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
  const rows = fields.reduce<Record<string, { height: number; cells: Record<string, { text: string; fillForm: unknown }> }>>(
    (acc, field, index) => {
      acc[String(index)] = {
        height: DEFAULT_ROW_HEIGHT,
        cells: {
          0: {
            text: field.fieldName,
            fillForm: { field: field.fieldCode }
          }
        }
      }
      return acc
    },
    {}
  )
  return {
    reportId: PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID,
    reportName: PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME,
    sheetLayoutJson: JSON.stringify({ cols: { 0: { width: 240 } }, rows }),
    cells: fields.map((field, index) => ({
      rowIndex: index,
      columnIndex: 0,
      cellKey: field.fieldCode,
      sourceType: SOURCE_TYPE_PRODUCTION_WORK_ORDER,
      sourceFieldCode: field.fieldCode,
      sourceFieldName: field.fieldName,
      label: field.fieldName,
      valueType: field.valueType || 'STRING',
      readonly: false,
      signatureCell: false,
      linkableAsSource: true,
      linkableAsTarget: false
    }))
  }
}

const persistRules = async (nextRules: BatchRecordCellLinkRuleVO[], successMessage: string) => {
  if (!context.value || saving.value) return false
  saving.value = true
  try {
    const result = await BatchRecordCellLinkApi.saveRules({
      scopeType: context.value.scopeType,
      scopeId: context.value.scopeId,
      routeId: context.value.routeId,
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
          'is-target-selectable': mode === 'target' && Boolean(meta?.linkableAsTarget),
          'is-linked': linkedKeys.has(reportCellKey),
          'is-selected': isSelectedCell
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

function resolveErrorMessage(error: unknown, fallback: string) {
  const candidate = error as { message?: string; msg?: string }
  return candidate?.msg || candidate?.message || fallback
}
</script>

<style scoped>
.batch-record-cell-link {
  min-height: calc(100vh - 112px);
  display: grid;
  grid-template-rows: 76px minmax(0, 1fr);
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

.batch-record-cell-link__select {
  min-width: 0;
  width: 260px;
}

.batch-record-cell-link__target-select {
  width: 320px;
}

.batch-record-cell-link__source-link-count {
  min-width: 104px;
  color: #1677ff;
  border-color: #b9d7ff;
  background: #f4f9ff;
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
  background: #dbe3ef;
}

.batch-record-cell-link__pane {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: 44px minmax(0, 1fr);
  background: #ffffff;
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
  overflow: auto;
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
