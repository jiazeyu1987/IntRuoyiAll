<template>
  <div
    class="edhr-readonly-form"
    :class="{ 'is-embedded': embedded, 'is-height-fit': fitMode === 'height' }"
  >
    <el-alert
      v-if="parseError"
      :title="parseError"
      type="error"
      :closable="false"
      show-icon
      class="edhr-readonly-form__alert"
    />

    <template v-else>
      <EdhrTemplateFitViewport
        v-if="fitToViewport"
        class="edhr-readonly-form__fit-viewport"
        :width-only="fitMode !== 'height'"
      >
        <div class="edhr-readonly-form__sheet-wrap is-fit-to-viewport">
          <table class="edhr-template-sheet">
            <colgroup>
              <col
                v-for="column in columns"
                :key="column.columnIndex"
                :style="{ width: `${column.widthPercent}%` }"
              />
            </colgroup>
            <tbody>
              <tr
                v-for="row in renderedRows"
                :key="row.rowIndex"
                :style="{ height: `${row.height}px` }"
              >
                <td
                  v-for="cell in row.cells"
                  :key="cell.identity"
                  :rowspan="cell.rowSpan"
                  :colspan="cell.colSpan"
                  :class="cell.classNames"
                  :style="cell.cellStyle"
                >
                  <span
                    v-if="cell.ruleBadge && !cell.fillablePlaceholder && cell.checkboxState === null"
                    class="edhr-template-sheet__rule-type-badge"
                    :class="`is-${cell.ruleBadge.tone}`"
                    :title="cell.ruleTooltip"
                    :aria-label="cell.ruleTooltip"
                  >
                    {{ cell.ruleBadge.symbol }}
                  </span>
                  <span
                    v-if="cell.checkboxState !== null"
                    class="edhr-template-sheet__checkbox-control"
                    :class="{ 'is-checked': cell.checkboxState }"
                    aria-hidden="true"
                  >
                    <span class="edhr-template-sheet__checkbox-box">
                      {{ cell.checkboxState ? '✓' : '' }}
                    </span>
                    <span
                      v-if="cell.checkboxLabelText"
                      class="edhr-template-sheet__checkbox-label"
                    >
                      {{ cell.checkboxLabelText }}
                    </span>
                  </span>
                  <span v-else-if="cell.text.trim()" class="edhr-template-sheet__cell-text">
                    {{ cell.text }}
                  </span>
                  <span
                    v-if="cell.fillablePlaceholder"
                    class="edhr-template-sheet__fillable-placeholder"
                    :class="{ 'is-type-symbol': cell.placeholderIsTypeSymbol }"
                    :title="cell.ruleTooltip"
                    :aria-label="cell.ruleTooltip"
                  >
                    {{ cell.fillablePlaceholder }}
                  </span>
                  <span v-if="cell.attachmentRuleText" class="edhr-template-sheet__attachment-rule">
                    {{ cell.attachmentRuleText }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </EdhrTemplateFitViewport>
      <div v-else class="edhr-readonly-form__sheet-wrap">
        <table class="edhr-template-sheet">
          <colgroup>
            <col
              v-for="column in columns"
              :key="column.columnIndex"
              :style="{ width: `${column.widthPercent}%` }"
            />
          </colgroup>
          <tbody>
            <tr
              v-for="row in renderedRows"
              :key="row.rowIndex"
              :style="{ height: `${row.height}px` }"
            >
              <td
                v-for="cell in row.cells"
                :key="cell.identity"
                :rowspan="cell.rowSpan"
                :colspan="cell.colSpan"
                :class="cell.classNames"
                :style="cell.cellStyle"
              >
                <span
                  v-if="cell.ruleBadge && !cell.fillablePlaceholder && cell.checkboxState === null"
                  class="edhr-template-sheet__rule-type-badge"
                  :class="`is-${cell.ruleBadge.tone}`"
                  :title="cell.ruleTooltip"
                  :aria-label="cell.ruleTooltip"
                >
                  {{ cell.ruleBadge.symbol }}
                </span>
                <span
                  v-if="cell.checkboxState !== null"
                  class="edhr-template-sheet__checkbox-control"
                  :class="{ 'is-checked': cell.checkboxState }"
                  aria-hidden="true"
                >
                  <span class="edhr-template-sheet__checkbox-box">
                    {{ cell.checkboxState ? '✓' : '' }}
                  </span>
                  <span
                    v-if="cell.checkboxLabelText"
                    class="edhr-template-sheet__checkbox-label"
                  >
                    {{ cell.checkboxLabelText }}
                  </span>
                </span>
                <span v-else-if="cell.text.trim()" class="edhr-template-sheet__cell-text">
                  {{ cell.text }}
                </span>
                <span
                  v-if="cell.fillablePlaceholder"
                  class="edhr-template-sheet__fillable-placeholder"
                  :class="{ 'is-type-symbol': cell.placeholderIsTypeSymbol }"
                  :title="cell.ruleTooltip"
                  :aria-label="cell.ruleTooltip"
                >
                  {{ cell.fillablePlaceholder }}
                </span>
                <span v-if="cell.attachmentRuleText" class="edhr-template-sheet__attachment-rule">
                  {{ cell.attachmentRuleText }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="props.formViewModel?.remark" class="edhr-readonly-form__remark">
        <span class="edhr-readonly-form__remark-label">备注</span>
        <span>{{ props.formViewModel.remark }}</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type {
  EdhrBatchExecutionReviewFormViewModel,
  EdhrBatchExecutionReviewSignatureRecord,
  EdhrSignatureCellMarker
} from '@/api/mes/pro/edhr/batchExecution'
import type { BatchRecordReportCellValueType } from '@/api/mes/pro/batchrecordreport'
import {
  cellRuleDefaultComponentMap,
  cleanedSelectOptions,
  resolveTemplateRuleState,
  resolveTemplateRuleTooltip,
  resolveTemplateRuleTypeBadge,
  resolveTemplateCellCssStyle,
  templateSimulationComponentMap,
  type TemplateRawStyle,
  type TemplateEditableCellContext,
  type TemplateRuleState,
  type TemplateRuleTypeBadge,
  type TemplateSimulationComponentKind
} from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'
import { formatEdhrDateTime, toEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'
import EdhrTemplateFitViewport from './EdhrTemplateFitViewport.vue'

defineOptions({ name: 'EdhrExecutionReadonlyForm' })

type RawLayoutCell = {
  text?: unknown
  value?: unknown
  merge?: unknown
  style?: unknown
  edhrDiagonalSlash?: boolean
  edhrDiagonalSlashDirection?: 'TL2BR' | 'TR2BL' | 'BOTH'
  fillForm?: Record<string, unknown>
  edhrCellRule?: Record<string, unknown>
  edhrSignature?: RawSignatureCellMarker
}

type RawSignatureCellMarker = {
  enabled?: boolean
  actionType?: string
  label?: string
  displayFormat?: string
}

type RawLayoutRow = {
  height?: unknown
  cells?: Record<string, RawLayoutCell>
}

type RawLayout = {
  rows?: Record<string, RawLayoutRow>
  cols?: Record<string, { width?: unknown }>
  styles?: TemplateRawStyle[]
  merges?: unknown[]
}

type SnapshotPayload = {
  layout?: RawLayout
  meta?: Record<string, unknown>
  fields?: RawSnapshotField[]
}

type RawSnapshotField = {
  rowIndex?: unknown
  columnIndex?: unknown
  attachmentRule?: unknown
}

type CellValue = {
  rowIndex?: unknown
  columnIndex?: unknown
  valueType?: unknown
  value?: unknown
  valueDisplay?: unknown
  unit?: unknown
}

type ParseState<T> = {
  value?: T
  error: string
}

type CellValueMapState = {
  map: Map<string, CellValue>
  error: string
}

type RenderedColumn = {
  columnIndex: number
  widthPercent: number
}

type RenderedCell = {
  identity: string
  text: string
  checkboxState: boolean | null
  checkboxLabelText: string
  rowSpan: number
  colSpan: number
  classNames: Record<string, boolean>
  cellStyle: Record<string, string>
  attachmentRuleText?: string
  fillablePlaceholder?: string
  placeholderIsTypeSymbol?: boolean
  ruleBadge?: TemplateRuleTypeBadge
  ruleState?: TemplateRuleState
  ruleTooltip?: string
}

type RenderedRow = {
  rowIndex: number
  height: number
  cells: RenderedCell[]
}

const DEFAULT_COLUMN_WIDTH = 160
const DEFAULT_ROW_HEIGHT = 30
const normalizedCellRuleValueTypes = new Set(['STRING', 'NUMBER', 'DATE', 'DATETIME', 'BOOLEAN', 'SIGNATURE'])

const props = defineProps<{
  formViewModel?: EdhrBatchExecutionReviewFormViewModel
  signatureRecords?: EdhrBatchExecutionReviewSignatureRecord[]
  fitToViewport?: boolean
  fitMode?: 'width' | 'height'
  embedded?: boolean
}>()

const fitMode = computed(() => props.fitMode || 'width')

const parseJson = <T,>(raw: string | undefined, label: string): T | undefined => {
  if (!raw || !raw.trim()) return undefined
  try {
    return JSON.parse(raw) as T
  } catch (error) {
    const message = error instanceof Error ? error.message : '未知错误'
    throw new Error(`${label} 解析失败：${message}`)
  }
}

const parseStateError = (error: unknown, fallback: string) =>
  error instanceof Error && error.message.trim() ? error.message : fallback

const snapshotState = computed<ParseState<SnapshotPayload>>(() => {
  try {
    return {
      value: parseJson<SnapshotPayload>(props.formViewModel?.executionSnapshotJson, '执行快照'),
      error: ''
    }
  } catch (error) {
    return {
      value: undefined,
      error: parseStateError(error, '执行快照解析失败。')
    }
  }
})

const snapshot = computed(() => snapshotState.value.value)

const sheetLayoutState = computed<ParseState<RawLayout>>(() => {
  try {
    return {
      value: parseJson<RawLayout>(props.formViewModel?.sheetLayoutJson, '模板布局'),
      error: ''
    }
  } catch (error) {
    return {
      value: undefined,
      error: parseStateError(error, '模板布局解析失败。')
    }
  }
})

const sheetLayout = computed(() => sheetLayoutState.value.value)

const layoutState = computed<ParseState<RawLayout>>(() => {
  const upstreamError = snapshotState.value.error || sheetLayoutState.value.error
  if (upstreamError) {
    return {
      value: undefined,
      error: upstreamError
    }
  }
  const snapshotLayout = snapshot.value?.layout
  const directLayout = sheetLayout.value
  const candidate = hasRenderableRows(snapshotLayout) ? snapshotLayout : directLayout
  if (!hasRenderableRows(candidate)) {
    return {
      value: undefined,
      error: '缺少电子批记录模板布局，无法按原模板展示填写结果。'
    }
  }
  return {
    value: candidate,
    error: ''
  }
})

const layout = computed(() => layoutState.value.value)

const hasRenderableRows = (candidate: RawLayout | undefined): candidate is RawLayout => {
  return Boolean(candidate?.rows && Object.keys(candidate.rows).length > 0)
}

const cellValueMapState = computed<CellValueMapState>(() => {
  try {
    const parsed = parseJson<CellValue[]>(props.formViewModel?.cellValuesJson, '单元格值')
    const map = new Map<string, CellValue>()
    if (!parsed) return { map, error: '' }
    if (!Array.isArray(parsed)) {
      throw new Error('单元格值必须是数组。')
    }
    parsed.forEach((item) => {
      const rowIndex = Number(item.rowIndex)
      const columnIndex = Number(item.columnIndex)
      if (!Number.isInteger(rowIndex) || !Number.isInteger(columnIndex)) return
      map.set(`${rowIndex}:${columnIndex}`, item)
    })
    return { map, error: '' }
  } catch (error) {
    return {
      map: new Map<string, CellValue>(),
      error: parseStateError(error, '单元格值解析失败。')
    }
  }
})

const cellValueMap = computed(() => cellValueMapState.value.map)
const parseError = computed(
  () => layoutState.value.error || cellValueMapState.value.error
)

const signatureCellMarkers = computed(() => {
  const markers = new Map<string, EdhrSignatureCellMarker>()
  ;(props.formViewModel?.signatureCellMarkers || []).forEach((marker) => {
    if (!marker.enabled || !Number.isInteger(marker.rowIndex) || !Number.isInteger(marker.columnIndex)) return
    markers.set(`${marker.rowIndex}:${marker.columnIndex}`, marker)
  })
  return markers
})

const attachmentRuleMap = computed(() => {
  const map = new Map<string, string>()
  ;(snapshot.value?.fields || []).forEach((field) => {
    const rowIndex = Number(field.rowIndex)
    const columnIndex = Number(field.columnIndex)
    if (!Number.isInteger(rowIndex) || !Number.isInteger(columnIndex)) return
    const ruleText = formatAttachmentRule(field)
    if (ruleText) map.set(`${rowIndex}:${columnIndex}`, ruleText)
  })
  return map
})

const rowIndexes = computed(() => {
  const rows = layout.value?.rows || {}
  return Object.keys(rows)
    .map((key) => Number(key))
    .filter((key) => Number.isInteger(key))
    .sort((a, b) => a - b)
})

const columnIndexes = computed(() => {
  const layoutValue = layout.value
  if (!layoutValue) return [] as number[]
  const set = new Set<number>()
  Object.keys(layoutValue.cols || {}).forEach((key) => {
    const columnIndex = Number(key)
    if (Number.isInteger(columnIndex)) set.add(columnIndex)
  })
  Object.entries(layoutValue.rows || {}).forEach(([rowKey, row]) => {
    const rowIndex = Number(rowKey)
    if (!Number.isInteger(rowIndex)) return
    Object.entries(row.cells || {}).forEach(([columnKey, cell]) => {
      const columnIndex = Number(columnKey)
      if (!Number.isInteger(columnIndex)) return
      const merge = normalizeCellMerge(cell)
      for (let index = columnIndex; index < columnIndex + merge.colSpan; index += 1) {
        set.add(index)
      }
    })
  })
  return Array.from(set).sort((a, b) => a - b)
})

const columns = computed<RenderedColumn[]>(() => {
  const configuredWidths = columnIndexes.value.map((columnIndex) => {
    const configuredWidth = Number(layout.value?.cols?.[String(columnIndex)]?.width)
    return Number.isFinite(configuredWidth) && configuredWidth > 0 ? configuredWidth : DEFAULT_COLUMN_WIDTH
  })
  const totalWidth = configuredWidths.reduce((total, width) => total + width, 0)
  return columnIndexes.value.map((columnIndex) => {
    const configuredWidth = configuredWidths[columnIndexes.value.indexOf(columnIndex)]
    return {
      columnIndex,
      widthPercent: totalWidth > 0 ? (configuredWidth / totalWidth) * 100 : 100 / columnIndexes.value.length
    }
  })
})

const coveredCellSet = computed(() => {
  const covered = new Set<string>()
  Object.entries(layout.value?.rows || {}).forEach(([rowKey, row]) => {
    const rowIndex = Number(rowKey)
    if (!Number.isInteger(rowIndex)) return
    Object.entries(row.cells || {}).forEach(([columnKey, cell]) => {
      const columnIndex = Number(columnKey)
      if (!Number.isInteger(columnIndex)) return
      const merge = normalizeCellMerge(cell)
      for (let rowOffset = 0; rowOffset < merge.rowSpan; rowOffset += 1) {
        for (let columnOffset = 0; columnOffset < merge.colSpan; columnOffset += 1) {
          if (rowOffset === 0 && columnOffset === 0) continue
          covered.add(`${rowIndex + rowOffset}:${columnIndex + columnOffset}`)
        }
      }
    })
  })
  return covered
})

const renderedRows = computed<RenderedRow[]>(() => {
  const rows = layout.value?.rows || {}
  return rowIndexes.value.map((rowIndex) => {
    const rawRow = rows[String(rowIndex)] || {}
    const rawHeight = Number(rawRow.height)
    const renderedCells: RenderedCell[] = []
    columnIndexes.value.forEach((columnIndex) => {
      if (coveredCellSet.value.has(`${rowIndex}:${columnIndex}`)) return
      const rawCell = rawRow.cells?.[String(columnIndex)]
      const merge = normalizeCellMerge(rawCell)
      const signatureMarker = resolveSignatureMarker(rowIndex, columnIndex, rawCell)
      const checkboxState = resolveReadonlyCheckboxState(rowIndex, columnIndex, rawCell)
      const checkboxLabelText = resolveReadonlyCheckboxLabelText(rowIndex, columnIndex, rawCell)
      const displayedCheckboxLabelText = checkboxState !== null ? checkboxLabelText : ''
      const text = checkboxState !== null ? '' : resolveCellText(rowIndex, columnIndex, rawCell)
      const attachmentRuleText = attachmentRuleMap.value.get(`${rowIndex}:${columnIndex}`)
      const ruleContext = resolveReadonlyRuleContext(rowIndex, columnIndex, rawCell, signatureMarker, attachmentRuleText)
      const ruleState = ruleContext ? resolveTemplateRuleState(ruleContext) : undefined
      const ruleBadge = ruleContext ? resolveTemplateRuleTypeBadge(ruleContext) : undefined
      const fillablePlaceholder = resolveFillablePlaceholder(rawCell, text, ruleBadge)
      const displayFillablePlaceholder = checkboxState !== null ? '' : fillablePlaceholder
      renderedCells.push({
        identity: `${rowIndex}:${columnIndex}`,
        text,
        checkboxState,
        checkboxLabelText: displayedCheckboxLabelText,
        rowSpan: merge.rowSpan,
        colSpan: merge.colSpan,
        attachmentRuleText,
        fillablePlaceholder: displayFillablePlaceholder,
        placeholderIsTypeSymbol: Boolean(displayFillablePlaceholder && ruleBadge?.symbol === displayFillablePlaceholder),
        ruleBadge,
        ruleState,
        ruleTooltip: ruleContext ? resolveTemplateRuleTooltip(ruleContext) : '',
        cellStyle: resolveTemplateCellCssStyle(rawCell, layout.value?.styles),
        classNames: {
          'edhr-template-sheet__cell': true,
          'is-fillable': Boolean(rawCell?.fillForm || rawCell?.edhrCellRule),
          'is-static': !rawCell?.fillForm && !rawCell?.edhrCellRule,
          'is-rule-auto': ruleState === 'auto',
          'is-rule-reviewed': ruleState === 'reviewed',
          'is-rule-manual': ruleState === 'manual',
          'is-rule-error': ruleState === 'error',
          'is-empty': !text && !displayedCheckboxLabelText,
          'is-section-title': isSectionTitle(rawCell, merge),
          'is-signature-cell': Boolean(signatureMarker),
          'is-signature-signed': Boolean(signatureMarker && findSignatureRecord(signatureMarker)),
          'is-signature-pending': Boolean(signatureMarker && !findSignatureRecord(signatureMarker)),
          'is-checkbox-cell': checkboxState !== null,
          'is-attachment-rule-cell': Boolean(attachmentRuleText),
          'is-diagonal-slash': Boolean(rawCell?.edhrDiagonalSlash),
          'is-diagonal-slash-tl2br': rawCell?.edhrDiagonalSlashDirection === 'TL2BR'
        }
      })
    })
    return {
      rowIndex,
      height: Number.isFinite(rawHeight) && rawHeight > 0 ? Math.max(rawHeight, 24) : DEFAULT_ROW_HEIGHT,
      cells: renderedCells
    }
  })
})

const normalizeCellMerge = (cell: RawLayoutCell | undefined) => {
  if (!Array.isArray(cell?.merge)) return { rowSpan: 1, colSpan: 1 }
  const rowDelta = Number(cell.merge[0])
  const columnDelta = Number(cell.merge[1])
  return {
    rowSpan: Number.isInteger(rowDelta) && rowDelta >= 0 ? rowDelta + 1 : 1,
    colSpan: Number.isInteger(columnDelta) && columnDelta >= 0 ? columnDelta + 1 : 1
  }
}

const readString = (value: unknown) => (typeof value === 'string' ? value.trim() : '')

const readBoolean = (value: unknown) => value === true || String(value).toLowerCase() === 'true'

const normalizeReadonlyRuleValueType = (
  rawValue: unknown,
  componentFlag: string,
  signatureMarker: EdhrSignatureCellMarker | RawSignatureCellMarker | undefined
): BatchRecordReportCellValueType => {
  if (signatureMarker?.enabled) return 'SIGNATURE'
  const lowerComponent = componentFlag.toLowerCase()
  const compactComponent = lowerComponent.replace(/[\s_-]+/g, '')
  if (lowerComponent.includes('checkbox') || lowerComponent.includes('boolean')) return 'BOOLEAN'
  if (lowerComponent.includes('datetime') || lowerComponent.includes('日期时间')) return 'DATETIME'
  if (lowerComponent.includes('date') || lowerComponent.includes('日期')) return 'DATE'
  if (lowerComponent.includes('时间')) return 'DATETIME'
  if (lowerComponent.includes('number') || lowerComponent.includes('数字')) return 'NUMBER'
  if (
    lowerComponent.includes('signature') ||
    lowerComponent.includes('sign') ||
    lowerComponent.includes('电子签名') ||
    lowerComponent.includes('签名') ||
    lowerComponent.includes('签字') ||
    compactComponent.includes('electronicsignature')
  ) return 'SIGNATURE'
  const normalized = String(rawValue || '').trim().toUpperCase()
  if (normalizedCellRuleValueTypes.has(normalized)) {
    return normalized as BatchRecordReportCellValueType
  }
  return 'STRING'
}

const resolveReadonlyComponentKind = (
  valueType: BatchRecordReportCellValueType,
  componentFlag: string,
  signatureMarker: EdhrSignatureCellMarker | RawSignatureCellMarker | undefined,
  attachmentRuleText: string | undefined,
  options: TemplateEditableCellContext['options'] = []
): TemplateSimulationComponentKind => {
  if (signatureMarker?.enabled || valueType === 'SIGNATURE') return 'signature'
  const lowerComponent = componentFlag.toLowerCase()
  const compactComponent = lowerComponent.replace(/[\s_-]+/g, '')
  if (
    lowerComponent.includes('signature') ||
    lowerComponent.includes('sign') ||
    lowerComponent.includes('电子签名') ||
    lowerComponent.includes('签名') ||
    lowerComponent.includes('签字') ||
    compactComponent.includes('electronicsignature')
  ) {
    return 'signature'
  }
  if (
    attachmentRuleText ||
    lowerComponent.includes('upload-file') ||
    lowerComponent.includes('upload-image') ||
    lowerComponent.includes('upload-images') ||
    lowerComponent.includes('attachment') ||
    compactComponent.includes('uploadfile') ||
    lowerComponent.includes('附件') ||
    lowerComponent.includes('文件') ||
    lowerComponent.includes('图片')
  ) {
    return 'attachment'
  }
  if (
    lowerComponent.includes('radio-group') ||
    lowerComponent.includes('radio') ||
    lowerComponent.includes('option-group') ||
    lowerComponent.includes('single-choice') ||
    lowerComponent.includes('checkbox-group') ||
    compactComponent.includes('radiogroup') ||
    compactComponent.includes('optiongroup') ||
    compactComponent.includes('singlechoice') ||
    lowerComponent.includes('单选')
  ) {
    return 'radio'
  }
  if (lowerComponent.includes('number') || lowerComponent.includes('数字')) return 'number'
  if (lowerComponent.includes('datetime') || lowerComponent.includes('date-time') || lowerComponent.includes('日期时间')) {
    return 'datetime'
  }
  if (lowerComponent.includes('date') || lowerComponent.includes('日期')) return 'date'
  if (lowerComponent.includes('时间')) return 'datetime'
  if (
    valueType === 'STRING' &&
    (lowerComponent.includes('select') ||
      lowerComponent.includes('dropdown') ||
      Boolean(options?.length))
  ) {
    return 'select'
  }
  if (lowerComponent.includes('checkbox') || lowerComponent.includes('boolean')) return 'checkbox'
  return templateSimulationComponentMap[valueType] || 'text'
}

const resolveReadonlyRuleContext = (
  rowIndex: number,
  columnIndex: number,
  cell: RawLayoutCell | undefined,
  signatureMarker: EdhrSignatureCellMarker | RawSignatureCellMarker | undefined,
  attachmentRuleText: string | undefined
): TemplateEditableCellContext | undefined => {
  const identity = `${rowIndex}:${columnIndex}`
  const filledValue = cellValueMap.value.get(identity) as CellValue | undefined
  const rule = cell?.edhrCellRule || {}
  const fillForm = cell?.fillForm || {}
  if (!cell?.fillForm && !cell?.edhrCellRule && !signatureMarker?.enabled && !attachmentRuleText && !filledValue) {
    return undefined
  }

  const rawComponentFlag =
    readString(fillForm.componentFlag) ||
    readString(fillForm.component) ||
    readString(rule.componentFlag)
  const valueType = normalizeReadonlyRuleValueType(
    filledValue?.valueType || rule.valueType || fillForm.valueType,
    rawComponentFlag,
    signatureMarker
  )
  const componentFlag = rawComponentFlag || cellRuleDefaultComponentMap[valueType]
  const fillFormConstraints =
    fillForm.constraints && typeof fillForm.constraints === 'object'
      ? (fillForm.constraints as Record<string, unknown>)
      : {}
  const ruleConstraints =
    rule.constraints && typeof rule.constraints === 'object'
      ? (rule.constraints as Record<string, unknown>)
      : {}
  const constraints = { ...fillFormConstraints, ...ruleConstraints }
  const options = cleanedSelectOptions(constraints.options || fillForm.options || rule.options)

  return {
    fieldIdentity: identity,
    rowIndex,
    columnIndex,
    valueType,
    componentKind: resolveReadonlyComponentKind(
      valueType,
      componentFlag,
      signatureMarker,
      attachmentRuleText,
      options
    ),
    componentFlag,
    label:
      readString(rule.label) ||
      readString(fillForm.label) ||
      normalizeReadonlyCheckboxLabelText(fillForm.labelText) ||
      `第 ${rowIndex + 1} 行第 ${columnIndex + 1} 列`,
    required: readBoolean(rule.required) || readBoolean(fillForm.required),
    source: readString(rule.source) || readString(fillForm.source),
    reviewed: readBoolean(rule.reviewed) || readBoolean(fillForm.reviewed),
    unit: readString(rule.unit) || readString(fillForm.unit) || readString(filledValue?.unit) || undefined,
    format: readString(constraints.format) || readString(fillForm.format) || undefined,
    options
  }
}

function formatAttachmentRule(field: RawSnapshotField) {
  const rawRule = field.attachmentRule
  if (!rawRule || typeof rawRule !== 'object' || Array.isArray(rawRule)) return ''
  const rule = rawRule as Record<string, unknown>
  const minCount = Number(rule.minCount)
  const maxCount = Number(rule.maxCount)
  const groupKey = typeof rule.groupKey === 'string' ? rule.groupKey.trim() : ''
  const parts = [
    rule.required === true ? '必需附件' : '可选附件',
    Number.isFinite(minCount) && minCount > 0 ? `至少 ${minCount} 个` : '',
    Number.isFinite(maxCount) && maxCount > 0 ? `最多 ${maxCount} 个` : '',
    groupKey ? `组 ${groupKey}` : ''
  ].filter(Boolean)
  return parts.join('，')
}

const resolveFillablePlaceholder = (
  cell: RawLayoutCell | undefined,
  renderedText: string,
  ruleBadge?: TemplateRuleTypeBadge
) => {
  const hasFillableRule = Boolean(cell?.fillForm || cell?.edhrCellRule)
  if (!hasFillableRule || renderedText.trim()) return ''
  const placeholder = cell?.fillForm?.placeholder
  const rulePlaceholder = cell?.edhrCellRule?.placeholder
  let placeholderText = typeof rulePlaceholder === 'string' ? rulePlaceholder.trim() : ''
  if (!placeholderText) {
    placeholderText = typeof placeholder === 'string' ? placeholder.trim() : ''
  }
  if (!placeholderText || placeholderText === '请填写' || placeholderText === '?') {
    return ruleBadge ? '' : '?'
  }
  return placeholderText
}

const normalizeReadonlyCheckboxLabelText = (value: unknown) => {
  const text = stringifyValue(value)
    .replace(/^[\s□☐☑☒]+/u, '')
    .replace(/[＿_]{2,}\s*$/u, '')
    .replace(/\s+/g, ' ')
    .trim()
  return text
}

const resolveReadonlyCheckboxLabelText = (
  rowIndex: number,
  columnIndex: number,
  cell: RawLayoutCell | undefined
) => {
  if (!cell?.fillForm && !cell?.edhrCellRule) return ''
  void rowIndex
  void columnIndex
  return normalizeReadonlyCheckboxLabelText(
    cell?.fillForm?.labelText || cell?.fillForm?.label || cell?.edhrCellRule?.label
  )
}

const resolveCellText = (rowIndex: number, columnIndex: number, cell: RawLayoutCell | undefined) => {
  const signatureText = resolveSignatureText(resolveSignatureMarker(rowIndex, columnIndex, cell))
  if (signatureText != null) return signatureText
  const filledValue = cellValueMap.value.get(`${rowIndex}:${columnIndex}`)
  if (filledValue) {
    const filledValueText = normalizeReadonlyFillableRawText(cell, stringifyCellValue(filledValue))
    if (filledValueText) return filledValueText
  }
  const fillValue = cell?.fillForm?.value
  if (fillValue != null && fillValue !== '') {
    const fillValueText = normalizeReadonlyFillableRawText(cell, stringifyValue(fillValue))
    if (fillValueText) return fillValueText
  }
  if (cell?.value != null && cell.value !== '') {
    const valueText = normalizeReadonlyFillableRawText(cell, stringifyValue(cell.value))
    if (valueText) return valueText
  }
  if (cell?.text != null && cell.text !== '') {
    const cellText = normalizeReadonlyFillableRawText(cell, stringifyValue(cell.text))
    if (cellText) return cellText
  }
  return ''
}

const normalizeReadonlyFillableRawText = (cell: RawLayoutCell | undefined, text: string) => {
  const normalizedText = text.trim()
  if (
    (cell?.fillForm || cell?.edhrCellRule) &&
    (!normalizedText || normalizedText === '请填写' || normalizedText === '?')
  ) {
    return ''
  }
  return text
}

const resolveReadonlyCheckboxState = (
  rowIndex: number,
  columnIndex: number,
  cell: RawLayoutCell | undefined
): boolean | null => {
  const filledValue = cellValueMap.value.get(`${rowIndex}:${columnIndex}`) as CellValue | undefined
  const rawValueType = String(
    filledValue?.valueType || cell?.edhrCellRule?.valueType || cell?.fillForm?.valueType || ''
  ).toUpperCase()
  const rawType = String(
    cell?.fillForm?.componentFlag ||
      cell?.fillForm?.component ||
      cell?.edhrCellRule?.componentFlag ||
      ''
  ).toLowerCase()
  const isBooleanValueType = rawValueType === 'BOOLEAN'
  if (!isBooleanValueType && !rawType.includes('checkbox') && !rawType.includes('boolean')) {
    return null
  }
  const value = filledValue?.value ?? cell?.fillForm?.value ?? cell?.value ?? false
  return value === true || String(value).toLowerCase() === 'true'
}

const resolveSignatureMarker = (
  rowIndex: number,
  columnIndex: number,
  cell: RawLayoutCell | undefined
) => {
  const directMarker = cell?.edhrSignature
  if (directMarker?.enabled) return directMarker
  return signatureCellMarkers.value.get(`${rowIndex}:${columnIndex}`)
}

const resolveSignatureText = (marker: EdhrSignatureCellMarker | RawSignatureCellMarker | undefined) => {
  if (!marker?.enabled || !marker.actionType) return undefined
  const signature = findSignatureRecord(marker)
  if (!signature) return '未签名'
  const actor = signature.actorName || (signature.actorId != null ? String(signature.actorId) : '未知签名人')
  return `${actor}\n${formatSignatureTime(signature.signedAt)}`
}

const findSignatureRecord = (marker: EdhrSignatureCellMarker | RawSignatureCellMarker | undefined) => {
  if (!marker?.actionType) return undefined
  const matched = [...(props.signatureRecords || [])]
    .filter((record) => record.actionType === marker.actionType)
    .sort((left, right) => toTime(left.signedAt) - toTime(right.signedAt))
  return matched[matched.length - 1]
}

const formatSignatureTime = (value: string | undefined) => {
  return formatEdhrDateTime(value)
}

const toTime = (value: string | undefined) => {
  return toEdhrDateTime(value)?.getTime() ?? 0
}

const stringifyValue = (value: unknown) => {
  if (value == null) return ''
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return JSON.stringify(value)
}

const stringifyCellValue = (cellValue: CellValue) => {
  const valueType = String(cellValue.valueType || '').toUpperCase()
  const display =
    typeof cellValue.valueDisplay === 'string' && cellValue.valueDisplay.trim()
      ? cellValue.valueDisplay.trim()
      : stringifyValue(cellValue.value)
  if (valueType === 'BOOLEAN') {
    return cellValue.value === true || String(cellValue.value).toLowerCase() === 'true' ? '☑' : '☐'
  }
  if (valueType === 'NUMBER') {
    const unit = typeof cellValue.unit === 'string' && cellValue.unit.trim() ? ` ${cellValue.unit.trim()}` : ''
    return `${display}${unit}`.trim()
  }
  return display === 'null' || display == null ? '' : display
}

const isSectionTitle = (cell: RawLayoutCell | undefined, merge: { rowSpan: number; colSpan: number }) => {
  const text = String(cell?.text || '').trim()
  return Boolean(text) && merge.colSpan >= Math.max(3, Math.floor(columnIndexes.value.length / 2))
}
</script>

<style scoped>
.edhr-readonly-form {
  border: 1px solid #d8e1ee;
  border-radius: 4px;
  background: #fff;
  padding: 12px;
  width: 100%;
  height: auto;
}

.edhr-readonly-form.is-embedded {
  border: none;
  border-radius: 0;
  padding: 0;
  background: transparent;
}

.edhr-readonly-form.is-height-fit {
  height: 100%;
  min-height: 0;
}

.edhr-readonly-form__alert {
  margin-bottom: 12px;
}

.edhr-readonly-form__fit-viewport {
  width: 100%;
  height: auto;
}

.edhr-readonly-form.is-height-fit .edhr-readonly-form__fit-viewport {
  height: 100%;
}

.edhr-readonly-form__sheet-wrap {
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  border: 1px solid #1f2937;
  background: #fff;
}

.edhr-readonly-form__sheet-wrap.is-fit-to-viewport {
  overflow: visible;
}

.edhr-template-sheet {
  width: max(100%, 960px);
  min-width: 960px;
  border-collapse: collapse;
  table-layout: fixed;
  color: #111827;
  font-size: 12px;
  line-height: 1.3;
}

.edhr-template-sheet__cell {
  position: relative;
  border: 1px solid #1f2937;
  padding: 3px 4px;
  text-align: center;
  vertical-align: middle;
  white-space: pre-wrap;
  word-break: break-word;
}

.edhr-template-sheet__cell.is-static {
  background: #fff;
  font-weight: inherit;
}

.edhr-template-sheet__cell.is-diagonal-slash {
  background: #fff;
}

.edhr-template-sheet__cell.is-diagonal-slash::after {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: #1f2937;
  clip-path: polygon(calc(100% - 1px) 0, 100% 0, 1px 100%, 0 100%);
  pointer-events: none;
  content: '';
}

.edhr-template-sheet__cell.is-diagonal-slash-tl2br::after {
  clip-path: polygon(0 0, 1px 0, 100% calc(100% - 1px), 100% 100%);
}

.edhr-template-sheet__cell.is-fillable {
  background: #f8fffd;
  color: #0f766e;
  font-weight: 500;
  box-shadow: inset 0 0 0 1px rgba(15, 118, 110, 0.32);
}

.edhr-template-sheet__cell.is-empty {
  color: #9ca3af;
}

.edhr-template-sheet__cell.is-section-title {
  background: #fff;
  font-weight: 700;
}

.edhr-template-sheet__cell.is-signature-cell {
  background: #f8fbff;
  color: #0f766e;
  font-weight: 700;
}

.edhr-template-sheet__cell.is-signature-signed {
  box-shadow: inset 0 0 0 1px #14b8a6;
}

.edhr-template-sheet__cell.is-signature-pending {
  color: #9ca3af;
  font-weight: 500;
}

.edhr-template-sheet__cell.is-attachment-rule-cell {
  background: #f8fafc;
}

.edhr-template-sheet__cell.is-checkbox-cell {
  color: #0f766e;
}

.edhr-template-sheet__cell.is-rule-auto {
  border-style: dashed;
  border-color: #fdba74;
  background: #fff7ed;
  color: #9a3412;
}

.edhr-template-sheet__cell.is-rule-reviewed {
  border-color: #86efac;
  background: #f0fdf4;
  color: #166534;
}

.edhr-template-sheet__cell.is-rule-manual {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.edhr-template-sheet__cell.is-rule-error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.edhr-template-sheet__cell-text {
  display: inline-block;
  min-height: 16px;
}

.edhr-template-sheet__rule-type-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  z-index: 1;
  display: inline-flex;
  width: 16px;
  height: 16px;
  align-items: center;
  justify-content: center;
  border: 1px solid #cbd5e1;
  border-radius: 5px;
  background: #ffffff;
  color: #263247;
  font-size: 9px;
  font-weight: 700;
  line-height: 1;
  pointer-events: auto;
}

.edhr-template-sheet__rule-type-badge.is-text {
  border-color: #cbd5e1;
  color: #334155;
}

.edhr-template-sheet__rule-type-badge.is-number {
  border-color: #93c5fd;
  color: #1d4ed8;
}

.edhr-template-sheet__rule-type-badge.is-date,
.edhr-template-sheet__rule-type-badge.is-datetime {
  border-color: #67e8f9;
  color: #0e7490;
}

.edhr-template-sheet__rule-type-badge.is-boolean {
  border-color: #86efac;
  color: #15803d;
}

.edhr-template-sheet__rule-type-badge.is-signature {
  border-color: #c4b5fd;
  color: #6d28d9;
}

.edhr-template-sheet__rule-type-badge.is-attachment {
  border-color: #fdba74;
  color: #c2410c;
}

.edhr-template-sheet__checkbox-control {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  max-width: 100%;
  min-height: 18px;
  vertical-align: middle;
  white-space: normal;
}

.edhr-template-sheet__checkbox-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 12px;
  height: 12px;
  box-sizing: border-box;
  border: 1px solid #172033;
  background: #fff;
  color: #0f766e;
  font-size: 11px;
  line-height: 1;
}

.edhr-template-sheet__checkbox-control.is-checked .edhr-template-sheet__checkbox-box {
  border-color: #0f766e;
}

.edhr-template-sheet__checkbox-label {
  display: inline-block;
  min-width: 0;
  color: #172033;
  line-height: 1.25;
  text-align: left;
  word-break: break-word;
}

.edhr-template-sheet__fillable-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(56px, 100%);
  min-width: 0;
  min-height: 20px;
  margin: 0 auto;
  box-sizing: border-box;
  padding: 1px 8px;
  border-bottom: 1px solid #0f766e;
  color: #0f766e;
  font-size: 12px;
  line-height: 1.4;
}

.edhr-template-sheet__fillable-placeholder.is-type-symbol {
  width: auto;
  min-width: 22px;
  padding: 1px 6px;
  border: 1px solid #cbd5e1;
  border-radius: 5px;
  background: #ffffff;
  color: #263247;
  font-weight: 700;
}

.edhr-template-sheet__attachment-rule {
  display: block;
  margin-top: 4px;
  color: #b45309;
  font-size: 12px;
  line-height: 1.35;
}

.edhr-readonly-form__remark {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  color: #374151;
  font-size: 13px;
}

.edhr-readonly-form__remark-label {
  color: #6b7280;
  font-weight: 600;
}
</style>
