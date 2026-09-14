<template>
  <div
    class="edhr-template-editable-form"
    :class="{ 'is-fit-width': fitMode === 'width', 'is-fit-height': fitMode === 'height' }"
  >
    <el-alert
      v-if="parseError"
      :title="parseError"
      type="error"
      :closable="false"
      show-icon
      class="edhr-template-editable-form__alert"
    />

    <template v-else>
      <div
        v-if="props.showRuleLegend"
        class="edhr-template-editable-form__rule-legend"
        aria-label="单元格规则类型图例"
      >
        <span
          v-for="item in ruleLegendItems"
          :key="item.tone"
          class="edhr-template-editable-form__rule-legend-item"
        >
          <span class="edhr-template-editable-form__rule-type-badge" :class="`is-${item.tone}`">
            {{ item.symbol }}
          </span>
          <span>{{ item.label }}</span>
        </span>
        <span class="edhr-template-editable-form__rule-state is-rule-auto">自动待确认</span>
        <span class="edhr-template-editable-form__rule-state is-rule-reviewed">已确认</span>
      </div>

      <EdhrTemplateFitViewport
        v-if="fitMode || fitToViewport"
        class="edhr-template-editable-form__fit-viewport"
        :width-only="fitMode === 'width' || !fitMode"
      >
        <div class="edhr-template-editable-form__sheet-wrap is-fit-to-viewport">
          <table class="edhr-template-editable-form__sheet">
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
                    v-if="cell.ruleBadge && props.cellTypeDisplay === 'badge'"
                    class="edhr-template-editable-form__rule-type-badge"
                    :class="`is-${cell.ruleBadge.tone}`"
                    :title="cell.ruleTooltip"
                    :aria-label="cell.ruleTooltip"
                  >
                    {{ cell.ruleBadge.symbol }}
                  </span>
                  <template v-if="cell.editableContext">
                    <slot name="field" :context="cell.editableContext">
                    <div class="edhr-template-editable-form__editable-cell">
                      <div class="edhr-template-editable-form__editable-head">
                        <span class="edhr-template-editable-form__editable-label">{{ cell.editableContext.label }}</span>
                        <span v-if="cell.editableContext.required" class="edhr-template-editable-form__required">
                          必填
                        </span>
                      </div>

                      <div v-if="cell.editableContext.componentKind === 'signature'" class="edhr-template-editable-form__signature">
                        <span class="edhr-template-editable-form__signature-status">未签名</span>
                        <el-button
                          size="small"
                          type="primary"
                          plain
                          class="edhr-template-editable-form__signature-action"
                          @click="emitSignatureAction(cell.editableContext)"
                        >
                          电子签名
                        </el-button>
                      </div>

                      <div
                        v-else-if="cell.editableContext.componentKind === 'attachment'"
                        class="edhr-template-editable-form__attachment"
                      >
                        <span>{{ formatTemplateAttachmentRule(cell.editableContext.attachmentRule) }}</span>
                        <span>请在当前业务页面完成受控附件上传</span>
                      </div>

                      <el-checkbox
                        v-else-if="cell.editableContext.componentKind === 'checkbox'"
                        :model-value="Boolean(modelValue[cell.editableContext.fieldIdentity])"
                        @update:model-value="
                          (value) => patchField(cell.editableContext!.fieldIdentity, value)
                        "
                      >
                        勾选
                      </el-checkbox>

                      <el-radio-group
                        v-else-if="cell.editableContext.componentKind === 'radio'"
                        :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                        class="edhr-template-editable-form__radio-group"
                        @update:model-value="
                          (value) => patchField(cell.editableContext!.fieldIdentity, String(value || ''))
                        "
                      >
                        <el-radio
                          v-for="option in cell.editableContext.options || []"
                          :key="option.value"
                          :value="option.value"
                        >
                          {{ option.label }}
                        </el-radio>
                      </el-radio-group>

                      <el-select
                        v-else-if="cell.editableContext.componentKind === 'select'"
                        :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                        size="small"
                        class="!w-1/1"
                        clearable
                        :placeholder="cell.editableContext.placeholder || '请选择'"
                        @update:model-value="
                          (value) => patchField(cell.editableContext!.fieldIdentity, value || '')
                        "
                      >
                        <el-option
                          v-for="option in cell.editableContext.options || []"
                          :key="option.value"
                          :label="option.label"
                          :value="option.value"
                        />
                      </el-select>

                      <el-input-number
                        v-else-if="cell.editableContext.componentKind === 'number'"
                        :model-value="resolveNumberValue(modelValue[cell.editableContext.fieldIdentity])"
                        :controls="false"
                        size="small"
                        class="!w-1/1"
                        :placeholder="cell.editableContext.placeholder"
                        @update:model-value="
                          (value) => patchField(cell.editableContext!.fieldIdentity, value == null ? null : value)
                        "
                      />

                      <el-date-picker
                        v-else-if="cell.editableContext.componentKind === 'date'"
                        :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                        type="date"
                        value-format="YYYY-MM-DD"
                        size="small"
                        class="!w-1/1"
                        :placeholder="cell.editableContext.placeholder || '请选择日期'"
                        @update:model-value="
                          (value) => patchField(cell.editableContext!.fieldIdentity, value || '')
                        "
                      />

                      <el-date-picker
                        v-else-if="cell.editableContext.componentKind === 'datetime'"
                        :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                        type="datetime"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        size="small"
                        class="!w-1/1"
                        :placeholder="cell.editableContext.placeholder || '请选择日期时间'"
                        @update:model-value="
                          (value) => patchField(cell.editableContext!.fieldIdentity, value || '')
                        "
                      />

                      <el-input
                        v-else-if="cell.editableContext.componentKind === 'textarea'"
                        :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                        type="textarea"
                        :rows="2"
                        size="small"
                        :placeholder="cell.editableContext.placeholder || '请输入内容'"
                        @update:model-value="
                          (value) => patchField(cell.editableContext!.fieldIdentity, value)
                        "
                      />

                      <el-input
                        v-else
                        :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                        size="small"
                        :placeholder="cell.editableContext.placeholder || '请输入内容'"
                        @update:model-value="
                          (value) => patchField(cell.editableContext!.fieldIdentity, value)
                        "
                      />

                      <div v-if="cell.editHint" class="edhr-template-editable-form__hint">
                        {{ cell.editHint }}
                      </div>
                    </div>
                    </slot>
                  </template>

                  <template v-else>
                    <span class="edhr-template-editable-form__cell-text">{{ cell.text }}</span>
                  </template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </EdhrTemplateFitViewport>

      <div v-else class="edhr-template-editable-form__sheet-wrap">
        <table class="edhr-template-editable-form__sheet">
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
                  v-if="cell.ruleBadge && props.cellTypeDisplay === 'badge'"
                  class="edhr-template-editable-form__rule-type-badge"
                  :class="`is-${cell.ruleBadge.tone}`"
                  :title="cell.ruleTooltip"
                  :aria-label="cell.ruleTooltip"
                >
                  {{ cell.ruleBadge.symbol }}
                </span>
                <template v-if="cell.editableContext">
                  <slot name="field" :context="cell.editableContext">
                  <div class="edhr-template-editable-form__editable-cell">
                    <div class="edhr-template-editable-form__editable-head">
                      <span class="edhr-template-editable-form__editable-label">{{ cell.editableContext.label }}</span>
                      <span v-if="cell.editableContext.required" class="edhr-template-editable-form__required">
                        必填
                      </span>
                    </div>

                    <div v-if="cell.editableContext.componentKind === 'signature'" class="edhr-template-editable-form__signature">
                      <span class="edhr-template-editable-form__signature-status">未签名</span>
                      <el-button
                        size="small"
                        type="primary"
                        plain
                        class="edhr-template-editable-form__signature-action"
                        @click="emitSignatureAction(cell.editableContext)"
                      >
                        电子签名
                      </el-button>
                    </div>

                    <div
                      v-else-if="cell.editableContext.componentKind === 'attachment'"
                      class="edhr-template-editable-form__attachment"
                    >
                      <span>{{ formatTemplateAttachmentRule(cell.editableContext.attachmentRule) }}</span>
                      <span>请在当前业务页面完成受控附件上传</span>
                    </div>

                    <el-checkbox
                      v-else-if="cell.editableContext.componentKind === 'checkbox'"
                      :model-value="Boolean(modelValue[cell.editableContext.fieldIdentity])"
                      @update:model-value="
                        (value) => patchField(cell.editableContext!.fieldIdentity, value)
                      "
                    >
                      勾选
                    </el-checkbox>

                    <el-radio-group
                      v-else-if="cell.editableContext.componentKind === 'radio'"
                      :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                      class="edhr-template-editable-form__radio-group"
                      @update:model-value="
                        (value) => patchField(cell.editableContext!.fieldIdentity, String(value || ''))
                      "
                    >
                      <el-radio
                        v-for="option in cell.editableContext.options || []"
                        :key="option.value"
                        :value="option.value"
                      >
                        {{ option.label }}
                      </el-radio>
                    </el-radio-group>

                    <el-select
                      v-else-if="cell.editableContext.componentKind === 'select'"
                      :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                      size="small"
                      class="!w-1/1"
                      clearable
                      :placeholder="cell.editableContext.placeholder || '请选择'"
                      @update:model-value="
                        (value) => patchField(cell.editableContext!.fieldIdentity, value || '')
                      "
                    >
                      <el-option
                        v-for="option in cell.editableContext.options || []"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                      />
                    </el-select>

                    <el-input-number
                      v-else-if="cell.editableContext.componentKind === 'number'"
                      :model-value="resolveNumberValue(modelValue[cell.editableContext.fieldIdentity])"
                      :controls="false"
                      size="small"
                      class="!w-1/1"
                      :placeholder="cell.editableContext.placeholder"
                      @update:model-value="
                        (value) => patchField(cell.editableContext!.fieldIdentity, value == null ? null : value)
                      "
                    />

                    <el-date-picker
                      v-else-if="cell.editableContext.componentKind === 'date'"
                      :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                      type="date"
                      value-format="YYYY-MM-DD"
                      size="small"
                      class="!w-1/1"
                      :placeholder="cell.editableContext.placeholder || '请选择日期'"
                      @update:model-value="
                        (value) => patchField(cell.editableContext!.fieldIdentity, value || '')
                      "
                    />

                    <el-date-picker
                      v-else-if="cell.editableContext.componentKind === 'datetime'"
                      :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                      type="datetime"
                      value-format="YYYY-MM-DD HH:mm:ss"
                      size="small"
                      class="!w-1/1"
                      :placeholder="cell.editableContext.placeholder || '请选择日期时间'"
                      @update:model-value="
                        (value) => patchField(cell.editableContext!.fieldIdentity, value || '')
                      "
                    />

                    <el-input
                      v-else-if="cell.editableContext.componentKind === 'textarea'"
                      :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                      type="textarea"
                      :rows="2"
                      size="small"
                      :placeholder="cell.editableContext.placeholder || '请输入内容'"
                      @update:model-value="
                        (value) => patchField(cell.editableContext!.fieldIdentity, value)
                      "
                    />

                    <el-input
                      v-else
                      :model-value="resolveStringValue(modelValue[cell.editableContext.fieldIdentity])"
                      size="small"
                      :placeholder="cell.editableContext.placeholder || '请输入内容'"
                      @update:model-value="
                        (value) => patchField(cell.editableContext!.fieldIdentity, value)
                      "
                    />

                    <div v-if="cell.editHint" class="edhr-template-editable-form__hint">
                      {{ cell.editHint }}
                    </div>
                  </div>
                  </slot>
                </template>

                <template v-else>
                  <span class="edhr-template-editable-form__cell-text">{{ cell.text }}</span>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type {
  BatchRecordReportCellRuleVO,
  BatchRecordReportSignatureCellMarkerVO
} from '@/api/mes/pro/batchrecordreport'
import {
  buildTemplateEditableCellContext,
  buildTemplateFieldIdentity,
  formatTemplateAttachmentRule,
  normalizeCellRule,
  normalizeTemplateCellMerge,
  resolveTemplateRuleState,
  resolveTemplateRuleTooltip,
  resolveTemplateRuleTypeBadge,
  resolveTemplateCellCssStyle,
  stringifyTemplateCell,
  templateRuleTypeBadgeLegend,
  type TemplateEditableCellContext,
  type TemplateRawCell,
  type TemplateRawLayout,
  type TemplateRuleState,
  type TemplateRuleTypeBadge,
  type TemplateSimulationValueMap
} from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'
import EdhrTemplateFitViewport from './EdhrTemplateFitViewport.vue'

defineOptions({ name: 'EdhrExecutionTemplateEditableForm' })

type RenderedColumn = {
  columnIndex: number
  widthPercent: number
}

type RenderedCell = {
  identity: string
  text: string
  rowSpan: number
  colSpan: number
  editableContext?: TemplateEditableCellContext
  editHint?: string
  ruleBadge?: TemplateRuleTypeBadge
  ruleState?: TemplateRuleState
  ruleTooltip?: string
  classNames: Record<string, boolean>
  cellStyle: Record<string, string>
}

type RenderedRow = {
  rowIndex: number
  height: number
  cells: RenderedCell[]
}

type RawLayoutRow = {
  height?: unknown
  cells?: Record<string, TemplateRawCell>
}

type RichTemplateRawLayout = TemplateRawLayout & {
  rows?: Record<string, RawLayoutRow>
  cols?: Record<string, { width?: unknown }>
}

const DEFAULT_COLUMN_WIDTH = 160
const DEFAULT_ROW_HEIGHT = 30
const MIN_TEXT_EDITABLE_ROW_HEIGHT = 48
const MIN_TALL_EDITABLE_ROW_HEIGHT = 72
const TALL_EDITABLE_COMPONENT_KINDS = new Set<TemplateEditableCellContext['componentKind']>([
  'signature',
  'attachment'
])

const props = withDefaults(
  defineProps<{
    sheetLayoutJson?: string
    cellRules?: BatchRecordReportCellRuleVO[]
    signatureMarkers?: BatchRecordReportSignatureCellMarkerVO[]
    modelValue: TemplateSimulationValueMap
    fieldIdentityMap?: Record<string, string>
    fitToViewport?: boolean
    fitMode?: 'width' | 'height'
    showRuleLegend?: boolean
    cellTypeDisplay?: 'badge' | 'background'
  }>(),
  {
    showRuleLegend: true,
    cellTypeDisplay: 'badge'
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: TemplateSimulationValueMap]
  signatureAction: [context: TemplateEditableCellContext]
}>()

const parseJson = <T,>(raw: string | undefined, label: string): T | undefined => {
  if (!raw?.trim()) return undefined
  try {
    return JSON.parse(raw) as T
  } catch (error) {
    const message = error instanceof Error ? error.message : '未知错误'
    throw new Error(`${label} 解析失败：${message}`)
  }
}

const parsedLayoutState = computed<{
  layout?: RichTemplateRawLayout
  error: string
}>(() => {
  try {
    const parsed = parseJson<RichTemplateRawLayout>(props.sheetLayoutJson, '模板布局')
    if (!parsed?.rows || !Object.keys(parsed.rows).length) {
      return {
        error: '缺少电子批记录模板布局，无法渲染模板内填写。'
      }
    }
    return {
      layout: parsed,
      error: ''
    }
  } catch (error) {
    return {
      error: error instanceof Error ? error.message : '模板布局解析失败。'
    }
  }
})

const parseError = computed(() => parsedLayoutState.value.error)
const layout = computed(() => parsedLayoutState.value.layout)

const normalizedRules = computed(() => (props.cellRules || []).map(normalizeCellRule))

const editableContextMap = computed(() => {
  const markerMap = new Map<string, BatchRecordReportSignatureCellMarkerVO>()
  ;(props.signatureMarkers || [])
    .filter((marker) => marker.enabled)
    .forEach((marker) => markerMap.set(buildTemplateFieldIdentity(marker.rowIndex, marker.columnIndex), marker))
  const map = new Map<string, TemplateEditableCellContext>()
  normalizedRules.value.forEach((rule) => {
    const cellIdentity = buildTemplateFieldIdentity(rule.rowIndex, rule.columnIndex)
    const formDataFieldIdentity = props.fieldIdentityMap?.[cellIdentity] || cellIdentity
    map.set(cellIdentity, {
      ...buildTemplateEditableCellContext(rule, markerMap.get(cellIdentity)),
      fieldIdentity: formDataFieldIdentity
    })
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
  const rows = layout.value?.rows || {}
  const set = new Set<number>()
  Object.keys(layout.value?.cols || {}).forEach((key) => {
    const columnIndex = Number(key)
    if (Number.isInteger(columnIndex)) set.add(columnIndex)
  })
  Object.values(rows).forEach((row) => {
    Object.keys(row.cells || {}).forEach((columnKey) => {
      const columnIndex = Number(columnKey)
      if (Number.isInteger(columnIndex)) set.add(columnIndex)
    })
  })
  normalizedRules.value.forEach((rule) => set.add(rule.columnIndex))
  return Array.from(set).sort((a, b) => a - b)
})

const columns = computed<RenderedColumn[]>(() => {
  const configuredWidths = columnIndexes.value.map((columnIndex) => {
    const configuredWidth = Number(layout.value?.cols?.[String(columnIndex)]?.width)
    return Number.isFinite(configuredWidth) && configuredWidth > 0 ? configuredWidth : DEFAULT_COLUMN_WIDTH
  })
  const totalWidth = configuredWidths.reduce((sum, width) => sum + width, 0)
  return columnIndexes.value.map((columnIndex, index) => ({
    columnIndex,
    widthPercent: totalWidth > 0 ? (configuredWidths[index] / totalWidth) * 100 : 100 / columnIndexes.value.length
  }))
})

const coveredSet = computed(() => {
  const covered = new Set<string>()
  Object.entries(layout.value?.rows || {}).forEach(([rowKey, row]) => {
    const rowIndex = Number(rowKey)
    if (!Number.isInteger(rowIndex)) return
    Object.entries(row.cells || {}).forEach(([columnKey, cell]) => {
      const columnIndex = Number(columnKey)
      if (!Number.isInteger(columnIndex)) return
      const merge = normalizeTemplateCellMerge(cell)
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

const ruleLegendItems = templateRuleTypeBadgeLegend

const resolveEditHint = (context: TemplateEditableCellContext) => {
  const parts: string[] = []
  const signatureLabel = context.signatureLabel?.trim()
  if (signatureLabel) parts.push(signatureLabel)
  if (context.unit) parts.push(`单位 ${context.unit}`)
  if (context.format) parts.push(`格式 ${context.format}`)
  const attachmentText = formatTemplateAttachmentRule(context.attachmentRule)
  if (attachmentText) parts.push(attachmentText)
  return parts.join(' / ')
}

const resolveEditableRowMinHeight = (context: TemplateEditableCellContext) =>
  TALL_EDITABLE_COMPONENT_KINDS.has(context.componentKind)
    ? MIN_TALL_EDITABLE_ROW_HEIGHT
    : MIN_TEXT_EDITABLE_ROW_HEIGHT

const resolveRenderedRowHeight = (rawHeight: unknown, editableHeightFloor = 0) => {
  const rawHeightNumber = Number(rawHeight)
  return Math.max(
    Number.isFinite(rawHeightNumber) && rawHeightNumber > 0 ? rawHeightNumber : DEFAULT_ROW_HEIGHT,
    editableHeightFloor,
    24
  )
}

const editableRowHeightFloorMap = computed<Map<number, number>>(() => {
  const map = new Map<number, number>()
  const rows = layout.value?.rows || {}
  const availableRowIndexes = new Set(rowIndexes.value)
  rowIndexes.value.forEach((rowIndex) => {
    const rawRow = rows[String(rowIndex)] || {}
    columnIndexes.value.forEach((columnIndex) => {
      const editableContext = editableContextMap.value.get(buildTemplateFieldIdentity(rowIndex, columnIndex))
      if (!editableContext) return
      const rawCell = rawRow.cells?.[String(columnIndex)]
      const merge = normalizeTemplateCellMerge(rawCell)
      const rowSpan = Math.max(merge.rowSpan, 1)
      const perRowHeightFloor = Math.ceil(resolveEditableRowMinHeight(editableContext) / rowSpan)
      for (let rowOffset = 0; rowOffset < rowSpan; rowOffset += 1) {
        const spannedRowIndex = rowIndex + rowOffset
        if (!availableRowIndexes.has(spannedRowIndex)) continue
        map.set(spannedRowIndex, Math.max(map.get(spannedRowIndex) || 0, perRowHeightFloor))
      }
    })
  })
  return map
})

const renderedRows = computed<RenderedRow[]>(() => {
  const rows = layout.value?.rows || {}
  return rowIndexes.value.map((rowIndex) => {
    const rawRow = rows[String(rowIndex)] || {}
    const rawHeight = Number(rawRow.height)
    let rowEditableHeightFloor = editableRowHeightFloorMap.value.get(rowIndex) || 0
    const cells: RenderedCell[] = []
    columnIndexes.value.forEach((columnIndex) => {
      if (coveredSet.value.has(`${rowIndex}:${columnIndex}`)) return
      const rawCell = rawRow.cells?.[String(columnIndex)]
      const merge = normalizeTemplateCellMerge(rawCell)
      const editableContext = editableContextMap.value.get(buildTemplateFieldIdentity(rowIndex, columnIndex))
      const ruleState = editableContext ? resolveTemplateRuleState(editableContext) : undefined
      const ruleBadge = editableContext ? resolveTemplateRuleTypeBadge(editableContext) : undefined
      const typeClassName = ruleBadge ? `is-cell-type-${ruleBadge.tone}` : ''
      if (editableContext) {
        rowEditableHeightFloor = Math.max(rowEditableHeightFloor, resolveEditableRowMinHeight(editableContext))
      }
      cells.push({
        identity: `${rowIndex}:${columnIndex}`,
        text: stringifyTemplateCell(rawCell?.value ?? rawCell?.text),
        rowSpan: merge.rowSpan,
        colSpan: merge.colSpan,
        editableContext,
        editHint: editableContext ? resolveEditHint(editableContext) : '',
        ruleBadge,
        ruleState,
        ruleTooltip: editableContext ? resolveTemplateRuleTooltip(editableContext) : '',
        cellStyle: resolveTemplateCellCssStyle(rawCell, layout.value?.styles),
        classNames: {
          'edhr-template-editable-form__cell': true,
          'is-static': !editableContext,
          'is-editable': Boolean(editableContext),
          'is-rule-auto': ruleState === 'auto',
          'is-rule-reviewed': ruleState === 'reviewed',
          'is-rule-manual': ruleState === 'manual',
          'is-rule-error': ruleState === 'error',
          'is-cell-type-background': props.cellTypeDisplay === 'background' && Boolean(ruleBadge),
          [typeClassName]: props.cellTypeDisplay === 'background' && Boolean(ruleBadge),
          'is-signature': editableContext?.componentKind === 'signature',
          'is-attachment': editableContext?.componentKind === 'attachment',
          'is-diagonal-slash': Boolean(rawCell?.edhrDiagonalSlash),
          'is-diagonal-slash-tl2br': rawCell?.edhrDiagonalSlashDirection === 'TL2BR',
          'is-empty': !editableContext && !stringifyTemplateCell(rawCell?.value ?? rawCell?.text)
        }
      })
    })
    return {
      rowIndex,
      height: resolveRenderedRowHeight(rawHeight, rowEditableHeightFloor),
      cells
    }
  })
})

const patchField = (fieldIdentity: string, value: TemplateSimulationValueMap[string]) => {
  emit('update:modelValue', {
    ...props.modelValue,
    [fieldIdentity]: value
  })
}

const emitSignatureAction = (context: TemplateEditableCellContext) => {
  emit('signatureAction', context)
}

const resolveStringValue = (value: TemplateSimulationValueMap[string]) => {
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return ''
}

const resolveNumberValue = (value: TemplateSimulationValueMap[string]) => {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}
</script>

<style scoped>
.edhr-template-editable-form {
  min-width: 0;
  width: 100%;
  height: 100%;
  min-height: 0;
}

.edhr-template-editable-form__alert {
  margin-bottom: 12px;
}

.edhr-template-editable-form.is-fit-width {
  height: auto;
  min-height: 100%;
}

.edhr-template-editable-form__rule-legend {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  padding: 6px 8px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  color: #4b5563;
  font-size: 12px;
}

.edhr-template-editable-form__rule-legend-item,
.edhr-template-editable-form__rule-state {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 22px;
  white-space: nowrap;
}

.edhr-template-editable-form__rule-state {
  padding: 0 6px;
  border-radius: 5px;
  border: 1px solid #dbe3ef;
  font-weight: 600;
}

.edhr-template-editable-form__rule-state.is-rule-auto {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #9a3412;
}

.edhr-template-editable-form__rule-state.is-rule-reviewed {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #166534;
}

.edhr-template-editable-form__fit-viewport {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.edhr-template-editable-form__sheet-wrap {
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  border: 1px solid #1f2937;
  background: #fff;
}

.edhr-template-editable-form__sheet-wrap.is-fit-to-viewport {
  overflow: visible;
}

.edhr-template-editable-form__sheet {
  width: max(100%, 960px);
  min-width: 960px;
  border-collapse: collapse;
  table-layout: fixed;
  color: #111827;
  font-size: 12px;
  line-height: 1.3;
}

.edhr-template-editable-form__cell {
  position: relative;
  border: 1px solid #1f2937;
  padding: 4px;
  text-align: center;
  vertical-align: middle;
  white-space: pre-wrap;
  word-break: break-word;
}

.edhr-template-editable-form__cell.is-static {
  background: #fff;
  color: #172033;
  font-weight: inherit;
}

.edhr-template-editable-form__cell.is-diagonal-slash {
  background: #fff;
}

.edhr-template-editable-form__cell.is-diagonal-slash::after {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: #1f2937;
  clip-path: polygon(calc(100% - 1px) 0, 100% 0, 1px 100%, 0 100%);
  pointer-events: none;
  content: '';
}

.edhr-template-editable-form__cell.is-diagonal-slash-tl2br::after {
  clip-path: polygon(0 0, 1px 0, 100% calc(100% - 1px), 100% 100%);
}

.edhr-template-editable-form__cell.is-editable {
  background: #fafcff;
}

.edhr-template-editable-form__cell.is-rule-auto {
  border-style: dashed;
  border-color: #fdba74;
  background: #fff7ed;
}

.edhr-template-editable-form__cell.is-rule-reviewed {
  border-color: #86efac;
  background: #f0fdf4;
}

.edhr-template-editable-form__cell.is-rule-manual {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.edhr-template-editable-form__cell.is-rule-error {
  border-color: #fecaca;
  background: #fef2f2;
}

.edhr-template-editable-form__cell.is-signature {
  background: #eefcf9;
}

.edhr-template-editable-form__cell.is-attachment {
  background: #fffaf0;
}

.edhr-template-editable-form__cell.is-rule-auto {
  background: #fff7ed;
}

.edhr-template-editable-form__cell.is-rule-reviewed {
  background: #f0fdf4;
}

.edhr-template-editable-form__cell.is-rule-manual {
  background: #eff6ff;
}

.edhr-template-editable-form__cell.is-rule-error {
  background: #fef2f2;
}

.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-text {
  background: #fff7ed;
}

.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-number {
  background: #eff6ff;
}

.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-date {
  background: #ecfeff;
}

.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-datetime {
  background: #f0fdfa;
}

.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-boolean {
  background: #f0fdf4;
}

.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-signature {
  background: #faf5ff;
}

.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-radio {
  background: #f5f3ff;
}

.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-select {
  background: #eff6ff;
}

.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-attachment {
  background: #fef2f2;
}

.edhr-template-editable-form__cell.is-empty {
  color: #9ca3af;
}

.edhr-template-editable-form__rule-type-badge {
  position: absolute;
  top: 3px;
  right: 3px;
  z-index: 1;
  display: inline-flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  border: 1px solid #cbd5e1;
  border-radius: 5px;
  background: #ffffff;
  color: #263247;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  pointer-events: auto;
}

.edhr-template-editable-form__rule-legend-item .edhr-template-editable-form__rule-type-badge {
  position: static;
  flex: 0 0 auto;
}

.edhr-template-editable-form__rule-type-badge.is-text {
  border-color: #cbd5e1;
  color: #334155;
}

.edhr-template-editable-form__rule-type-badge.is-number {
  border-color: #93c5fd;
  color: #1d4ed8;
}

.edhr-template-editable-form__rule-type-badge.is-date,
.edhr-template-editable-form__rule-type-badge.is-datetime {
  border-color: #67e8f9;
  color: #0e7490;
}

.edhr-template-editable-form__rule-type-badge.is-boolean {
  border-color: #86efac;
  color: #15803d;
}

.edhr-template-editable-form__rule-type-badge.is-signature {
  border-color: #c4b5fd;
  color: #6d28d9;
}

.edhr-template-editable-form__rule-type-badge.is-radio {
  border-color: #c4b5fd;
  color: #5b21b6;
}

.edhr-template-editable-form__rule-type-badge.is-select {
  border-color: #93c5fd;
  color: #1d4ed8;
}

.edhr-template-editable-form__rule-type-badge.is-attachment {
  border-color: #fdba74;
  color: #c2410c;
}

.edhr-template-editable-form__editable-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: left;
}

.edhr-template-editable-form__editable-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.edhr-template-editable-form__editable-label {
  color: #172033;
  font-size: 12px;
  font-weight: 600;
}

.edhr-template-editable-form__required {
  color: #dc2626;
  font-size: 12px;
}

.edhr-template-editable-form__signature,
.edhr-template-editable-form__attachment {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.edhr-template-editable-form__signature-status {
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.edhr-template-editable-form__signature-action {
  align-self: flex-start;
}

.edhr-template-editable-form__attachment {
  color: #92400e;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-template-editable-form__radio-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px 8px;
}

.edhr-template-editable-form__hint {
  color: #4b5563;
  font-size: 11px;
  line-height: 1.4;
}

.edhr-template-editable-form__cell-text {
  display: inline-block;
  min-height: 16px;
}
</style>
