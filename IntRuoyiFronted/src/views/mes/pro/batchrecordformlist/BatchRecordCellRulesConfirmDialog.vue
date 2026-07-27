<template>
  <Dialog v-model="dialogVisible" title="单元格规则" width="calc(100vw - 32px)">
    <div v-loading="loading" class="batch-record-cell-rules-editor">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />

      <section class="batch-record-cell-rules-editor__summary">
        <span class="batch-record-cell-rules-editor__name">{{ reportName }}</span>
        <el-tag type="primary" effect="plain">规则 {{ ruleRows.length }}</el-tag>
        <el-tag :type="pendingCount > 0 ? 'warning' : 'success'" effect="plain">
          待确认 {{ pendingCount }}
        </el-tag>
        <el-tag type="info" effect="plain">
          后端待确认 {{ unreviewedFillableCellCount }}
        </el-tag>
        <span class="batch-record-cell-rules-editor__mode">
          规则编辑模式：左侧只选单元格，右侧切换可填写/不可填写
        </span>
      </section>

      <section class="batch-record-cell-rules-editor__workspace">
        <div class="batch-record-cell-rules-editor__preview">
          <div class="batch-record-cell-rules-editor__panel-head">
            <div>
              <strong>只读表单预览</strong>
              <p>点击任意单元格只会选中规则目标，不会触发日期框、签名框或复选框。</p>
            </div>
            <el-tag type="info" effect="plain">只读</el-tag>
          </div>

          <el-alert
            v-if="sheetLayoutError"
            :title="sheetLayoutError"
            type="error"
            :closable="false"
            show-icon
          />
          <div v-else-if="renderedRows.length" class="batch-record-cell-rules-editor__sheet-scroll">
            <table class="batch-record-cell-rules-editor__sheet">
              <colgroup>
                <col
                  v-for="column in renderedColumns"
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
                  >
                    <button
                      type="button"
                      class="batch-record-cell-rules-editor__cell-button"
                      aria-label="选择单元格规则"
                      :aria-pressed="cell.identity === selectedRuleKey"
                      @click="selectRuleCell(cell)"
                    >
                      <span v-if="cell.text" class="batch-record-cell-rules-editor__cell-text">
                        {{ cell.text }}
                      </span>
                      <span v-else class="batch-record-cell-rules-editor__cell-placeholder">
                        第 {{ cell.rowIndex + 1 }} 行第 {{ cell.columnIndex + 1 }} 列
                      </span>
                      <span v-if="cell.rule" class="batch-record-cell-rules-editor__cell-rule">
                        <span>{{ resolveRuleEditorValueTypeLabel(cell.rule) }}</span>
                        <b v-if="cell.rule.required">必填</b>
                      </span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <el-empty v-else description="暂无可展示的表单布局" />
        </div>

        <aside class="batch-record-cell-rules-editor__side-panel">
          <template v-if="selectedCell">
            <div class="batch-record-cell-rules-editor__fillable-toggle">
              <strong>是否可填写</strong>
              <el-switch
                v-model="isSelectedCellFillable"
                active-text="可填写"
                inactive-text="不可填写"
              />
            </div>

            <template v-if="selectedRule">
              <el-form
                label-position="top"
                class="batch-record-cell-rules-editor__form"
              >
                <el-form-item label="字段名称">
                  <el-input
                    v-model="selectedRule.label"
                    maxlength="80"
                    show-word-limit
                    placeholder="请输入字段名称"
                  />
                </el-form-item>

                <el-form-item label="单元格提示词">
                  <el-input
                    v-model="selectedRule.placeholder"
                    maxlength="120"
                    show-word-limit
                    placeholder="请输入单元格空值提示"
                  />
                </el-form-item>

                <el-form-item label="字段说明">
                  <el-input
                    v-model="selectedRule.helpText"
                    type="textarea"
                    :rows="3"
                    maxlength="300"
                    show-word-limit
                    placeholder="说明这个单元格要填写什么内容"
                  />
                </el-form-item>

                <el-form-item label="是否必填">
                  <el-switch
                    v-model="selectedRule.required"
                    active-text="必填"
                    inactive-text="可选"
                  />
                </el-form-item>

                <el-form-item label="字段类型">
                  <el-select
                    v-model="selectedRuleEditorValueType"
                    class="!w-1/1"
                  >
                    <el-option
                      v-for="option in ruleEditorValueTypeOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="控件类型">
                  <el-select
                    v-model="selectedRule.componentFlag"
                    class="!w-1/1"
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请选择或输入控件类型"
                    @change="handleSelectedComponentFlagChange"
                  >
                    <el-option
                      v-for="option in componentFlagOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </el-form-item>

                <el-alert
                  v-if="selectedRule.valueType === 'SIGNATURE'"
                  title="电子签名控件必须对应已启用的签名标记单元格；缺少签名标记时保存会被后端拒绝。"
                  type="warning"
                  :closable="false"
                  show-icon
                />

                <el-form-item
                  v-if="selectedRule.componentFlag === 'select'"
                  label="下拉选项"
                >
                  <el-input
                    v-model="selectedRuleSelectOptionsText"
                    type="textarea"
                    :rows="4"
                    placeholder="每行一个选项；可用 名称=值 设置提交值，例如：合格=PASS"
                  />
                  <p class="batch-record-cell-rules-editor__form-tip">
                    下拉框至少需要两个有效选项，保存时会写入 selectionMode=single 和 options。
                  </p>
                </el-form-item>

                <el-form-item
                  v-if="selectedRule.valueType === 'NUMBER'"
                  label="字段范围"
                >
                  <div class="batch-record-cell-rules-editor__range-grid">
                    <el-input-number
                      v-model="selectedNumericMin"
                      :controls="false"
                      placeholder="最小值"
                      class="!w-1/1"
                      @change="setSelectedNumericConstraint('min', $event)"
                    />
                    <el-input-number
                      v-model="selectedNumericMax"
                      :controls="false"
                      placeholder="最大值"
                      class="!w-1/1"
                      @change="setSelectedNumericConstraint('max', $event)"
                    />
                  </div>
                </el-form-item>
              </el-form>
            </template>
          </template>

          <el-empty v-else description="请在左侧表单中点击一个单元格" />
        </aside>
      </section>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button :loading="loading" :disabled="!reportId || saving" @click="loadCellRules">重新读取</el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="!canConfirmRules"
        @click="confirmAllRules"
      >
        保存规则
      </el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  BatchRecordReportApi,
  type BatchRecordReportCellRuleConstraints,
  type BatchRecordReportCellRuleVO,
  type BatchRecordReportCellRulesRespVO,
  type BatchRecordReportCellValueType,
  type BatchRecordReportVO
} from '@/api/mes/pro/batchrecordreport'
import {
  cellRuleDefaultComponentMap,
  cellRuleValueTypeOptions,
  cleanedRuleConstraints,
  normalizeCellRule,
  normalizeTemplateCellMerge,
  stringifyTemplateCell,
  type TemplateRawCell,
  type TemplateRawLayout
} from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'

defineOptions({ name: 'BatchRecordCellRulesConfirmDialog' })

type ReportLike = Pick<BatchRecordReportVO, 'reportId' | 'reportName' | 'batchRecordName'>

type RuleEditorRawRow = {
  height?: unknown
  cells?: Record<string, TemplateRawCell>
}

type RuleEditorRawLayout = TemplateRawLayout & {
  rows?: Record<string, RuleEditorRawRow>
  cols?: Record<string, { width?: unknown }>
}

type RuleEditorColumn = {
  columnIndex: number
  widthPercent: number
}

type RuleEditorCell = {
  identity: string
  rowIndex: number
  columnIndex: number
  text: string
  rowSpan: number
  colSpan: number
  rule?: BatchRecordReportCellRuleVO
  classNames: Record<string, boolean>
}

type RuleEditorRow = {
  rowIndex: number
  height: number
  cells: RuleEditorCell[]
}

type NumericConstraintKey = 'min' | 'max' | 'scale' | 'precision'

type SelectOption = {
  label: string
  value: string
}

const props = defineProps<{
  modelValue: boolean
  report?: ReportLike | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirmed: [value: BatchRecordReportCellRulesRespVO]
}>()

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const sheetLayoutError = ref('')
const selectedRuleKey = ref('')
const ruleRows = ref<BatchRecordReportCellRuleVO[]>([])
const sheetLayout = ref<RuleEditorRawLayout | null>(null)
const summary = reactive({
  unreviewedFillableCellCount: 0
})

const DEFAULT_COLUMN_WIDTH = 150
const DEFAULT_ROW_HEIGHT = 34

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const reportId = computed(() => String(props.report?.reportId || '').trim())
const reportName = computed(
  () => props.report?.reportName || props.report?.batchRecordName || props.report?.reportId || '-'
)
const unreviewedFillableCellCount = computed(() => summary.unreviewedFillableCellCount)
const valueTypeLabelMap = Object.fromEntries(
  cellRuleValueTypeOptions.map((option) => [option.value, option.label])
) as Record<string, string>

const componentFlagBaseOptions = [
  { label: '文本输入 input-text', value: 'input-text' },
  { label: '下拉框 select', value: 'select' },
  { label: '数字输入 input-number', value: 'input-number' },
  { label: '日期 date', value: 'date' },
  { label: '日期时间 datetime', value: 'datetime' },
  { label: '复选框 checkbox', value: 'checkbox' },
  { label: '电子签名 signature', value: 'signature' },
  { label: '文件上传 upload-file', value: 'upload-file' },
  { label: '图片上传 upload-image', value: 'upload-image' }
]

const componentFlagValueTypeMap: Record<string, BatchRecordReportCellValueType> = {
  'input-text': 'STRING',
  'input-textarea': 'STRING',
  select: 'STRING',
  'input-number': 'NUMBER',
  date: 'DATE',
  datetime: 'DATETIME',
  checkbox: 'BOOLEAN',
  signature: 'SIGNATURE'
}

const componentFlagOptions = computed(() => {
  const optionMap = new Map(componentFlagBaseOptions.map((option) => [option.value, option]))
  ruleRows.value.forEach((rule) => {
    const value = String(rule.componentFlag || '').trim()
    if (value && !optionMap.has(value)) {
      optionMap.set(value, { label: value, value })
    }
  })
  return Array.from(optionMap.values())
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  if (typeof error === 'string' && error.trim()) return error
  const dataMessage = (error as any)?.msg || (error as any)?.message
  if (typeof dataMessage === 'string' && dataMessage.trim()) return dataMessage
  return fallback
}

const ruleIdentity = (rule: Pick<BatchRecordReportCellRuleVO, 'rowIndex' | 'columnIndex'>) =>
  `${rule.rowIndex}:${rule.columnIndex}`

const normalizeRuleSource = (source?: string) => {
  const normalized = String(source || '').trim().toUpperCase()
  return normalized || 'MANUAL'
}

const isConfirmedRule = (rule: BatchRecordReportCellRuleVO) =>
  Boolean(rule.reviewed) && normalizeRuleSource(rule.source) !== 'AUTO'

const pendingCount = computed(() => ruleRows.value.filter((rule) => !isConfirmedRule(rule)).length)
const canConfirmRules = computed(
  () => Boolean(reportId.value) && !loading.value && !saving.value
)

const cloneRecord = <T extends object | undefined>(value: T): T => (value ? ({ ...value } as T) : value)

const isSelectRule = (rule: Pick<BatchRecordReportCellRuleVO, 'componentFlag'>) =>
  String(rule.componentFlag || '').trim().toLowerCase() === 'select'

const normalizeSelectOptions = (rawOptions: unknown): SelectOption[] => {
  if (!Array.isArray(rawOptions)) return []
  const options: SelectOption[] = []
  rawOptions.forEach((rawOption) => {
    let label = ''
    let value = ''
    if (rawOption && typeof rawOption === 'object') {
      const optionRecord = rawOption as Record<string, unknown>
      label = String(optionRecord.label ?? '').trim()
      value = String(optionRecord.value ?? label).trim()
    } else {
      label = String(rawOption ?? '').trim()
      value = label
    }
    if (!label || !value || options.some((option) => option.value === value)) return
    options.push({ label, value })
  })
  return options
}

const parseSelectOptionsText = (text: string): SelectOption[] => {
  const options: SelectOption[] = []
  String(text || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .forEach((line) => {
      const matched = line.match(/^(.+?)(?:\s*[=|]\s*)(.+)$/)
      const label = (matched ? matched[1] : line).trim()
      const value = (matched ? matched[2] : label).trim()
      if (!label || !value || options.some((option) => option.value === value)) return
      options.push({ label, value })
    })
  return options
}

const formatSelectOptionsText = (rawOptions: unknown) =>
  normalizeSelectOptions(rawOptions)
    .map((option) => (option.label === option.value ? option.label : `${option.label}=${option.value}`))
    .join('\n')

const cleanRuleConstraintsForComponent = (
  constraints: BatchRecordReportCellRuleConstraints | undefined,
  valueType: BatchRecordReportCellValueType,
  componentFlag?: string
) => {
  const cleaned = cleanedRuleConstraints(constraints, valueType)
  if (String(componentFlag || '').trim().toLowerCase() !== 'select') {
    delete cleaned.selectionMode
    delete cleaned.options
  }
  return cleaned
}

const applySelectConstraintsToRule = (rule: BatchRecordReportCellRuleVO) => {
  if (!isSelectRule(rule)) return []
  rule.valueType = 'STRING'
  rule.componentFlag = 'select'
  const constraints: BatchRecordReportCellRuleConstraints = rule.constraints || {}
  const options = normalizeSelectOptions(constraints.options)
  constraints.selectionMode = 'single'
  constraints.options = options
  rule.constraints = constraints
  return options
}

const validateSelectOptionsBeforeSave = (rule: BatchRecordReportCellRuleVO) => {
  if (!isSelectRule(rule)) return
  const options = applySelectConstraintsToRule(rule)
  if (options.length < 2) {
    const label = rule.label || `第 ${rule.rowIndex + 1} 行第 ${rule.columnIndex + 1} 列`
    throw new Error(`${label} 的下拉框至少需要两个有效选项。`)
  }
}

const toManualReviewedRule = (rule: BatchRecordReportCellRuleVO): BatchRecordReportCellRuleVO => {
  const normalized = normalizeCellRule(rule)
  const constraints = cleanRuleConstraintsForComponent(
    normalized.constraints,
    normalized.valueType,
    normalized.componentFlag
  )
  const prepared = {
    ...normalized,
    constraints
  }
  applySelectConstraintsToRule(prepared)
  return {
    ...prepared,
    attachmentRule: cloneRecord(prepared.attachmentRule),
    source: 'MANUAL',
    confidence: 1,
    reviewed: true
  }
}

const parseSheetLayout = (sheetLayoutJson?: string): RuleEditorRawLayout | null => {
  if (!sheetLayoutJson?.trim()) return null
  const parsed = JSON.parse(sheetLayoutJson) as RuleEditorRawLayout
  if (!parsed?.rows || !Object.keys(parsed.rows).length) {
    throw new Error('后端未返回有效表单布局，无法进入可视化规则编辑。')
  }
  return parsed
}

const sortRules = (rules: BatchRecordReportCellRuleVO[]) =>
  [...rules].sort((left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex)

const selectedRule = computed(() =>
  ruleRows.value.find((rule) => ruleIdentity(rule) === selectedRuleKey.value)
)

const ruleMap = computed(() => {
  const map = new Map<string, BatchRecordReportCellRuleVO>()
  ruleRows.value.forEach((rule) => map.set(ruleIdentity(rule), rule))
  return map
})

const rowIndexes = computed(() => {
  const rows = sheetLayout.value?.rows || {}
  return Object.keys(rows)
    .map((key) => Number(key))
    .filter((key) => Number.isInteger(key))
    .sort((a, b) => a - b)
})

const columnIndexes = computed(() => {
  const columns = new Set<number>()
  Object.keys(sheetLayout.value?.cols || {}).forEach((key) => {
    const columnIndex = Number(key)
    if (Number.isInteger(columnIndex)) columns.add(columnIndex)
  })
  Object.values(sheetLayout.value?.rows || {}).forEach((row) => {
    Object.keys(row.cells || {}).forEach((key) => {
      const columnIndex = Number(key)
      if (Number.isInteger(columnIndex)) columns.add(columnIndex)
    })
  })
  ruleRows.value.forEach((rule) => columns.add(rule.columnIndex))
  return Array.from(columns).sort((a, b) => a - b)
})

const renderedColumns = computed<RuleEditorColumn[]>(() => {
  const widths = columnIndexes.value.map((columnIndex) => {
    const configuredWidth = Number(sheetLayout.value?.cols?.[String(columnIndex)]?.width)
    return Number.isFinite(configuredWidth) && configuredWidth > 0 ? configuredWidth : DEFAULT_COLUMN_WIDTH
  })
  const totalWidth = widths.reduce((sum, width) => sum + width, 0)
  return columnIndexes.value.map((columnIndex, index) => ({
    columnIndex,
    widthPercent: totalWidth > 0 ? (widths[index] / totalWidth) * 100 : 100
  }))
})

const coveredCellSet = computed(() => {
  const covered = new Set<string>()
  Object.entries(sheetLayout.value?.rows || {}).forEach(([rowKey, row]) => {
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

const resolveRowHeight = (height: unknown) => {
  const numericHeight = Number(height)
  return Math.max(Number.isFinite(numericHeight) && numericHeight > 0 ? numericHeight : DEFAULT_ROW_HEIGHT, 28)
}

const renderedRows = computed<RuleEditorRow[]>(() => {
  const rows = sheetLayout.value?.rows || {}
  return rowIndexes.value.map((rowIndex) => {
    const rawRow = rows[String(rowIndex)] || {}
    const cells: RuleEditorCell[] = []
    columnIndexes.value.forEach((columnIndex) => {
      const identity = `${rowIndex}:${columnIndex}`
      if (coveredCellSet.value.has(identity)) return
      const rawCell = rawRow.cells?.[String(columnIndex)]
      const merge = normalizeTemplateCellMerge(rawCell)
      const text = stringifyTemplateCell(rawCell?.value ?? rawCell?.text)
      const rule = ruleMap.value.get(identity)
      cells.push({
        identity,
        rowIndex,
        columnIndex,
        text,
        rowSpan: merge.rowSpan,
        colSpan: merge.colSpan,
        rule,
        classNames: {
          'batch-record-cell-rules-editor__cell': true,
          'is-empty': !text.trim(),
          'is-rule': Boolean(rule),
          'is-required': Boolean(rule?.required),
          'is-selected': selectedRuleKey.value === identity
        }
      })
    })
    return {
      rowIndex,
      height: resolveRowHeight(rawRow.height),
      cells
    }
  })
})

const selectedCell = computed(() => {
  if (!selectedRuleKey.value) return null
  for (const row of renderedRows.value) {
    const cell = row.cells.find((item) => item.identity === selectedRuleKey.value)
    if (cell) return cell
  }
  return null
})

const ensureSelectedRuleStillExists = () => {
  if (selectedRuleKey.value && selectedCell.value) return
  selectedRuleKey.value = ruleRows.value.length ? ruleIdentity(ruleRows.value[0]) : ''
}

const applyCellRulesResponse = (data: BatchRecordReportCellRulesRespVO) => {
  const sourceRows = [...(data.rules || []), ...(data.suggestions || [])]
  const nextRules = new Map<string, BatchRecordReportCellRuleVO>()
  sourceRows
    .map(normalizeCellRule)
    .sort((left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex)
    .forEach((rule) => nextRules.set(ruleIdentity(rule), rule))
  ruleRows.value = Array.from(nextRules.values())
  summary.unreviewedFillableCellCount = Number(data.unreviewedFillableCellCount) || 0
  try {
    sheetLayout.value = parseSheetLayout(data.sheetLayoutJson)
    sheetLayoutError.value = sheetLayout.value ? '' : '后端未返回表单布局，无法进入可视化规则编辑。'
  } catch (error) {
    sheetLayout.value = null
    sheetLayoutError.value = resolveErrorMessage(error, '表单布局解析失败，无法进入可视化规则编辑。')
  }
  ensureSelectedRuleStillExists()
}

const buildManualRuleFromCell = (cell: RuleEditorCell): BatchRecordReportCellRuleVO =>
  normalizeCellRule({
    rowIndex: cell.rowIndex,
    columnIndex: cell.columnIndex,
    valueType: 'STRING',
    componentFlag: cellRuleDefaultComponentMap.STRING,
    required: false,
    label: cell.text.trim() || `第 ${cell.rowIndex + 1} 行第 ${cell.columnIndex + 1} 列`,
    constraints: {},
    unit: '',
    source: 'MANUAL',
    confidence: 1,
    reviewed: true
  })

const selectRuleCell = (cell: RuleEditorCell) => {
  selectedRuleKey.value = cell.identity
}

const enableSelectedCellRule = () => {
  const cell = selectedCell.value
  if (!cell || ruleMap.value.has(cell.identity)) return
  ruleRows.value = sortRules([...ruleRows.value, buildManualRuleFromCell(cell)])
  selectedRuleKey.value = cell.identity
}

const disableSelectedCellRule = () => {
  const key = selectedRuleKey.value
  if (!key || !ruleMap.value.has(key)) return
  ruleRows.value = ruleRows.value.filter((rule) => ruleIdentity(rule) !== key)
  selectedRuleKey.value = key
}

const isSelectedCellFillable = computed({
  get: () => Boolean(selectedRule.value),
  set: (value: boolean) => {
    if (value) {
      enableSelectedCellRule()
      return
    }
    disableSelectedCellRule()
  }
})

const handleSelectedValueTypeChange = (value: BatchRecordReportCellValueType) => {
  if (!selectedRule.value) return
  selectedRule.value.componentFlag = cellRuleDefaultComponentMap[value]
  selectedRule.value.constraints = cleanRuleConstraintsForComponent(
    selectedRule.value.constraints,
    value,
    selectedRule.value.componentFlag
  )
}

const handleSelectedComponentFlagChange = (value: string) => {
  if (!selectedRule.value) return
  const componentFlag = String(value || '').trim()
  selectedRule.value.componentFlag = componentFlag
  const nextValueType = componentFlagValueTypeMap[componentFlag.toLowerCase()]
  if (nextValueType) {
    selectedRule.value.valueType = nextValueType
  }
  selectedRule.value.constraints = cleanRuleConstraintsForComponent(
    selectedRule.value.constraints,
    selectedRule.value.valueType,
    componentFlag
  )
  if (componentFlag.toLowerCase() === 'select') {
    applySelectConstraintsToRule(selectedRule.value)
  }
}

const ensureSelectedRuleConstraints = () => {
  if (!selectedRule.value) return null
  if (!selectedRule.value.constraints) {
    selectedRule.value.constraints = {}
  }
  return selectedRule.value.constraints
}

const setSelectedNumericConstraint = (key: NumericConstraintKey, value: number | null | undefined) => {
  const constraints = ensureSelectedRuleConstraints()
  if (!constraints) return
  if (value === null || value === undefined) {
    delete constraints[key]
    return
  }
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) {
    delete constraints[key]
    return
  }
  constraints[key] = numericValue
}

const selectedNumericMin = computed({
  get: () => selectedRule.value?.constraints?.min,
  set: (value: number | null | undefined) => setSelectedNumericConstraint('min', value)
})

const selectedNumericMax = computed({
  get: () => selectedRule.value?.constraints?.max,
  set: (value: number | null | undefined) => setSelectedNumericConstraint('max', value)
})

const selectedRuleSelectOptionsText = computed({
  get: () => formatSelectOptionsText(selectedRule.value?.constraints?.options),
  set: (value: string) => {
    if (!selectedRule.value) return
    selectedRule.value.valueType = 'STRING'
    selectedRule.value.componentFlag = 'select'
    const constraints = ensureSelectedRuleConstraints()
    if (!constraints) return
    constraints.selectionMode = 'single'
    constraints.options = parseSelectOptionsText(value)
  }
})

const validateRuleRowsBeforeSave = () => {
  ruleRows.value.forEach(validateSelectOptionsBeforeSave)
  const invalidRule = ruleRows.value.find((rule) => {
    if (rule.valueType !== 'NUMBER') return false
    const min = rule.constraints?.min
    const max = rule.constraints?.max
    return typeof min === 'number' && typeof max === 'number' && min > max
  })
  if (!invalidRule) return
  const label = invalidRule.label || `第 ${invalidRule.rowIndex + 1} 行第 ${invalidRule.columnIndex + 1} 列`
  throw new Error(`${label} 的数字最小值不能大于最大值。`)
}

const loadCellRules = async () => {
  if (!reportId.value) {
    errorMessage.value = '缺少有效表单ID，无法读取填写规则。'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await BatchRecordReportApi.getCellRules(reportId.value)
    applyCellRulesResponse(data)
  } catch (error) {
    const resolved = resolveErrorMessage(error, '填写规则读取失败，请联系管理员。')
    errorMessage.value = resolved
    message.error(resolved)
  } finally {
    loading.value = false
  }
}

const confirmAllRules = async () => {
  if (!reportId.value) {
    throw new Error('缺少有效表单ID，无法确认填写规则。')
  }
  saving.value = true
  errorMessage.value = ''
  try {
    validateRuleRowsBeforeSave()
    const data = await BatchRecordReportApi.saveCellRules({
      reportId: reportId.value,
      rules: ruleRows.value.map(toManualReviewedRule)
    })
    applyCellRulesResponse(data)
    emit('confirmed', data)
    message.success('单元格规则已保存')
    dialogVisible.value = false
  } catch (error) {
    const resolved = resolveErrorMessage(error, '填写规则确认失败，请联系管理员。')
    errorMessage.value = resolved
    message.error(resolved)
  } finally {
    saving.value = false
  }
}

watch(
  () => [dialogVisible.value, reportId.value] as const,
  ([visible, currentReportId]) => {
    if (!visible || !currentReportId) return
    void loadCellRules()
  },
  { immediate: true }
)
</script>

<style scoped>
.batch-record-cell-rules-editor {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
}

.batch-record-cell-rules-editor__summary {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 8px;
}

.batch-record-cell-rules-editor__name {
  min-width: 0;
  max-width: 360px;
  overflow: hidden;
  color: #172033;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-cell-rules-editor__mode {
  margin-left: auto;
  color: #5d667a;
  font-size: 12px;
}

.batch-record-cell-rules-editor__workspace {
  display: grid;
  height: clamp(520px, calc(100vh - 220px), 880px);
  min-height: 0;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 14px;
}

.batch-record-cell-rules-editor__preview,
.batch-record-cell-rules-editor__side-panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
}

.batch-record-cell-rules-editor__preview {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.batch-record-cell-rules-editor__side-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  padding: 12px;
}

.batch-record-cell-rules-editor__panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #e8eef6;
  background: #f7f9fc;
}

.batch-record-cell-rules-editor__panel-head strong {
  display: block;
  color: #172033;
  font-size: 14px;
}

.batch-record-cell-rules-editor__panel-head p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.4;
}

.batch-record-cell-rules-editor__sheet-scroll {
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding: 12px;
  background: #f3f6fb;
}

.batch-record-cell-rules-editor__sheet {
  min-width: 760px;
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  background: #fff;
  color: #172033;
  font-size: 12px;
}

.batch-record-cell-rules-editor__cell {
  min-width: 72px;
  border: 1px solid #cfd8e6;
  background: #fff;
  padding: 0;
  vertical-align: stretch;
}

.batch-record-cell-rules-editor__cell.is-empty {
  background: #fbfcfe;
}

.batch-record-cell-rules-editor__cell.is-rule {
  background: #eff6ff;
}

.batch-record-cell-rules-editor__cell.is-required {
  background: #fff8ed;
}

.batch-record-cell-rules-editor__cell.is-rule.is-required {
  background: #eff6ff;
}

.batch-record-cell-rules-editor__cell.is-selected {
  outline: 2px solid #2563eb;
  outline-offset: -2px;
}

.batch-record-cell-rules-editor__cell-button {
  display: flex;
  width: 100%;
  min-height: 100%;
  align-items: stretch;
  justify-content: space-between;
  gap: 8px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
  padding: 8px;
  text-align: left;
}

.batch-record-cell-rules-editor__cell-button:hover {
  background: rgba(37, 99, 235, 0.08);
}

.batch-record-cell-rules-editor__cell-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: pre-wrap;
}

.batch-record-cell-rules-editor__cell-placeholder {
  color: #a0a8b8;
}

.batch-record-cell-rules-editor__cell-rule {
  display: inline-flex;
  flex: 0 0 auto;
  align-self: flex-start;
  align-items: center;
  gap: 4px;
  color: #2563eb;
  font-size: 11px;
  white-space: nowrap;
}

.batch-record-cell-rules-editor__cell-rule b {
  color: #c2410c;
  font-weight: 600;
}

.batch-record-cell-rules-editor__fillable-toggle {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.batch-record-cell-rules-editor__fillable-toggle strong {
  display: block;
  color: #172033;
  font-size: 13px;
}

.batch-record-cell-rules-editor__form {
  flex: 0 0 auto;
}

.batch-record-cell-rules-editor__range-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
}

.batch-record-cell-rules-editor__form-tip {
  margin: 6px 0 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.4;
}

</style>
