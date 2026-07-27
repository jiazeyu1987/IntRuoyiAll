<template>
  <Dialog v-model="dialogVisible" title="填写配置" width="calc(100vw - 32px)">
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
          填写配置：左侧选单元格，右侧维护字段类型和辅助行
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
                        <span>{{ valueTypeLabelMap[cell.rule.valueType] || cell.rule.valueType }}</span>
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
                    v-model="selectedRule.valueType"
                    class="!w-1/1"
                    @change="handleSelectedValueTypeChange"
                  >
                    <el-option
                      v-for="option in cellRuleValueTypeOptions"
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
                  >
                    <el-option
                      v-for="option in componentFlagOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
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

                <el-form-item
                  v-if="selectedRule.valueType === 'STRING'"
                  label="下拉选项"
                >
                  <div class="batch-record-cell-rules-editor__dropdown-options">
                    <div class="batch-record-cell-rules-editor__dropdown-switch">
                      <span>作为下拉框填写</span>
                      <el-switch
                        :model-value="selectedRule.constraints?.selectionMode === 'single'"
                        active-text="启用"
                        inactive-text="关闭"
                        @change="toggleSelectedStringDropdown"
                      />
                    </div>
                    <template v-if="selectedRule.constraints?.selectionMode === 'single'">
                      <div
                        v-for="(option, optionIndex) in selectedStringOptions"
                        :key="optionIndex"
                        class="batch-record-cell-rules-editor__dropdown-option"
                      >
                        <el-input
                          :model-value="option.label"
                          maxlength="60"
                          placeholder="选项文本"
                          @input="updateSelectedStringOption(optionIndex, $event)"
                        />
                        <el-button
                          link
                          type="danger"
                          @click="removeSelectedStringOption(optionIndex)"
                        >
                          删除
                        </el-button>
                      </div>
                      <el-button
                        link
                        type="primary"
                        @click="addSelectedStringOption"
                      >
                        新增选项
                      </el-button>
                    </template>
                  </div>
                </el-form-item>

                <el-alert
                  v-if="selectedRule.valueType === 'SIGNATURE'"
                  title="签名单元格会在执行页触发现有电子签名，不作为普通文本保存。"
                  type="info"
                  :closable="false"
                  show-icon
                />
              </el-form>
            </template>

            <section class="batch-record-cell-rules-editor__assist-section">
              <div class="batch-record-cell-rules-editor__assist-head">
                <div>
                  <strong>辅助行配置</strong>
                  <p>把同一行要填写的单元格归在一起，并写清这一行给员工看的描述。</p>
                </div>
                <el-button
                  size="small"
                  type="primary"
                  plain
                  :disabled="!selectedCell"
                  @click="addAssistRowFromSelectedCell"
                >
                  当前单元格新增行
                </el-button>
              </div>

              <el-alert
                v-if="selectedCellAssistRow"
                :title="`当前单元格已在辅助行：${selectedCellAssistRow.description || selectedCellAssistRow.rowKey}`"
                type="success"
                :closable="false"
                show-icon
              />

              <el-empty
                v-if="assistRows.length === 0"
                description="暂无辅助行，请选择单元格后新增"
              />

              <div v-else class="batch-record-cell-rules-editor__assist-list">
                <article
                  v-for="(assistRow, assistRowIndex) in assistRows"
                  :key="assistRow.rowKey"
                  class="batch-record-cell-rules-editor__assist-row"
                  :class="{ 'is-selected': assistRow.rowKey === selectedAssistRowKey }"
                >
                  <button
                    type="button"
                    class="batch-record-cell-rules-editor__assist-select"
                    @click="selectedAssistRowKey = assistRow.rowKey"
                  >
                    <strong>辅助行 {{ assistRowIndex + 1 }}</strong>
                    <span>{{ assistRow.fields.length }} 个单元格</span>
                  </button>
                  <el-input
                    v-model="assistRow.description"
                    maxlength="120"
                    show-word-limit
                    placeholder="例如：记录本工序温度、压力、操作人"
                  />
                  <div class="batch-record-cell-rules-editor__assist-assignment">
                    <strong>辅助行填写人</strong>
                    <div class="batch-record-cell-rules-editor__assist-assignment-grid">
                      <el-select
                        v-model="assistAssignments[assistRow.rowKey].candidateSourceType"
                        placeholder="来源"
                        @change="assistAssignments[assistRow.rowKey].candidateSourceIds = []"
                      >
                        <el-option label="个人" value="USERS" />
                        <el-option label="角色" value="ROLE" />
                      </el-select>
                      <el-select
                        v-model="assistAssignments[assistRow.rowKey].candidateSourceIds"
                        multiple
                        filterable
                        collapse-tags
                        collapse-tags-tooltip
                        placeholder="选择员工或角色"
                      >
                        <el-option
                          v-for="option in buildAssignmentTargetOptions(assistAssignments[assistRow.rowKey].candidateSourceType)"
                          :key="`${assistAssignments[assistRow.rowKey].candidateSourceType}:${option.value}`"
                          :label="option.label"
                          :value="option.value"
                        />
                      </el-select>
                      <el-select
                        v-model="assistAssignments[assistRow.rowKey].completionPolicy"
                        placeholder="完成策略"
                      >
                        <el-option label="任一人完成" value="ANY_ONE" />
                        <el-option label="全部完成" value="ALL" />
                      </el-select>
                    </div>
                  </div>
                  <div class="batch-record-cell-rules-editor__assist-actions">
                    <el-button
                      size="small"
                      :disabled="!selectedCell"
                      @click="assignSelectedCellToAssistRow(assistRow.rowKey)"
                    >
                      加入当前单元格
                    </el-button>
                    <el-button
                      size="small"
                      :disabled="assistRowIndex === 0"
                      @click="moveAssistRow(assistRow.rowKey, -1)"
                    >
                      上移
                    </el-button>
                    <el-button
                      size="small"
                      :disabled="assistRowIndex === assistRows.length - 1"
                      @click="moveAssistRow(assistRow.rowKey, 1)"
                    >
                      下移
                    </el-button>
                    <el-button
                      size="small"
                      type="danger"
                      plain
                      @click="removeAssistRow(assistRow.rowKey)"
                    >
                      删除
                    </el-button>
                  </div>
                </article>
              </div>
            </section>
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
        保存填写配置
      </el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  BatchRecordReportApi,
  type BatchRecordReportAssistRowVO,
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
import {
  EdhrProcessFormPermissionRuleApi,
  type EdhrProcessFormCandidateSourceType,
  type EdhrProcessFormCompletionPolicy,
  type EdhrProcessFormFillAssignment
} from '@/api/mes/pro/edhr/processFormPermissionRule'
import { getSimpleRoleList, type RoleVO } from '@/api/system/role'
import { getSimpleUserList, type UserVO } from '@/api/system/user'

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

type StringSelectOption = {
  label: string
  value: string
}

type AssistAssignmentDraft = {
  candidateSourceType: EdhrProcessFormCandidateSourceType
  candidateSourceIds: number[]
  completionPolicy: EdhrProcessFormCompletionPolicy
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
const selectedAssistRowKey = ref('')
const ruleRows = ref<BatchRecordReportCellRuleVO[]>([])
const assistRows = ref<BatchRecordReportAssistRowVO[]>([])
const assistAssignments = reactive<Record<string, AssistAssignmentDraft>>({})
const simpleUserOptions = ref<UserVO[]>([])
const simpleRoleOptions = ref<RoleVO[]>([])
const sheetLayout = ref<RuleEditorRawLayout | null>(null)
const summary = reactive({
  unreviewedFillableCellCount: 0
})

const DEFAULT_COLUMN_WIDTH = 150
const DEFAULT_ROW_HEIGHT = 34
const ASSIST_ROW_KEY_PREFIX = 'ASSIST_ROW'

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
  { label: '数字输入 input-number', value: 'input-number' },
  { label: '日期 date', value: 'date' },
  { label: '日期时间 datetime', value: 'datetime' },
  { label: '复选框 checkbox', value: 'checkbox' },
  { label: '电子签名 signature', value: 'signature' },
  { label: '文件上传 upload-file', value: 'upload-file' },
  { label: '图片上传 upload-image', value: 'upload-image' }
]

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

const cellIdentity = (rowIndex: number, columnIndex: number) => `${rowIndex}:${columnIndex}`

const isValidCellCoordinate = (rowIndex: unknown, columnIndex: unknown) =>
  Number.isInteger(rowIndex) &&
  Number.isInteger(columnIndex) &&
  Number(rowIndex) >= 0 &&
  Number(columnIndex) >= 0

const normalizeAssistRowFields = (
  fields: BatchRecordReportAssistRowVO['fields'] = []
): BatchRecordReportAssistRowVO['fields'] => {
  const fieldMap = new Map<string, BatchRecordReportAssistRowVO['fields'][number]>()
  fields.forEach((field) => {
    if (!isValidCellCoordinate(field.rowIndex, field.columnIndex)) return
    fieldMap.set(cellIdentity(field.rowIndex, field.columnIndex), {
      rowIndex: field.rowIndex,
      columnIndex: field.columnIndex
    })
  })
  return Array.from(fieldMap.values()).sort(
    (left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex
  )
}

const normalizeAssistRows = (
  rows: BatchRecordReportAssistRowVO[] = []
): BatchRecordReportAssistRowVO[] =>
  rows
    .map((row, index) => ({
      rowKey: String(row.rowKey || `${ASSIST_ROW_KEY_PREFIX}_${index + 1}`).trim(),
      description: String(row.description || '').trim(),
      sort: Number.isFinite(Number(row.sort)) ? Number(row.sort) : index + 1,
      fields: normalizeAssistRowFields(row.fields)
    }))
    .filter((row) => row.rowKey)
    .sort((left, right) => left.sort - right.sort)
    .map((row, index) => ({
      ...row,
      sort: index + 1
    }))


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

const toManualReviewedRule = (rule: BatchRecordReportCellRuleVO): BatchRecordReportCellRuleVO => {
  const normalized = normalizeCellRule(rule)
  return {
    ...normalized,
    constraints: cleanedRuleConstraints(normalized.constraints, normalized.valueType),
    attachmentRule: cloneRecord(normalized.attachmentRule),
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

const createDefaultAssistAssignment = (): AssistAssignmentDraft => ({
  candidateSourceType: 'USERS',
  candidateSourceIds: [],
  completionPolicy: 'ANY_ONE'
})

const normalizeAssignmentSourceType = (
  value: unknown
): EdhrProcessFormCandidateSourceType => {
  const normalized = String(value || '').trim().toUpperCase()
  if (normalized === 'USER') return 'USERS'
  return normalized === 'ROLE' ? 'ROLE' : 'USERS'
}

const normalizeAssignmentPolicy = (value: unknown): EdhrProcessFormCompletionPolicy =>
  String(value || '').trim().toUpperCase() === 'ALL' ? 'ALL' : 'ANY_ONE'

const normalizeAssignmentIds = (ids: unknown): number[] =>
  Array.isArray(ids)
    ? Array.from(
        new Set(
          ids
            .map((id) => Number(id))
            .filter((id) => Number.isFinite(id) && id > 0)
        )
      )
    : []

const ensureAssistAssignment = (rowKey: string) => {
  if (!assistAssignments[rowKey]) {
    assistAssignments[rowKey] = createDefaultAssistAssignment()
  }
  return assistAssignments[rowKey]
}

const syncAssistAssignmentsWithRows = (rows = assistRows.value) => {
  const rowKeys = new Set(rows.map((row) => row.rowKey))
  Object.keys(assistAssignments).forEach((rowKey) => {
    if (!rowKeys.has(rowKey)) {
      delete assistAssignments[rowKey]
    }
  })
  rows.forEach((row) => ensureAssistAssignment(row.rowKey))
}

const applyAssistAssignments = (assignments: EdhrProcessFormFillAssignment[] = []) => {
  assignments.forEach((assignment) => {
    const rowKey = String(assignment.scopeKey || '').trim()
    if (!rowKey) return
    assistAssignments[rowKey] = {
      candidateSourceType: normalizeAssignmentSourceType(assignment.candidateSourceType),
      candidateSourceIds: normalizeAssignmentIds(assignment.candidateSourceIds),
      completionPolicy: normalizeAssignmentPolicy(assignment.completionPolicy)
    }
  })
  syncAssistAssignmentsWithRows()
}

const buildAssignmentTargetOptions = (sourceType: EdhrProcessFormCandidateSourceType) => {
  if (normalizeAssignmentSourceType(sourceType) === 'ROLE') {
    return simpleRoleOptions.value.map((role) => ({
      label: role.name || role.code || String(role.id),
      value: Number(role.id)
    }))
  }
  return simpleUserOptions.value.map((user) => ({
    label: user.nickname || user.username || String(user.id),
    value: Number(user.id)
  }))
}

const selectedCellAssistRow = computed(() => {
  const cell = selectedCell.value
  if (!cell) return null
  return (
    assistRows.value.find((row) =>
      row.fields.some(
        (field) => field.rowIndex === cell.rowIndex && field.columnIndex === cell.columnIndex
      )
    ) || null
  )
})

const selectedAssistRow = computed(() =>
  assistRows.value.find((row) => row.rowKey === selectedAssistRowKey.value)
)

const ensureSelectedRuleStillExists = () => {
  if (selectedRuleKey.value && selectedCell.value) return
  selectedRuleKey.value = ruleRows.value.length ? ruleIdentity(ruleRows.value[0]) : ''
}

const ensureSelectedAssistRowStillExists = () => {
  if (selectedAssistRowKey.value && selectedAssistRow.value) return
  selectedAssistRowKey.value = assistRows.value[0]?.rowKey || ''
}

const applyCellRulesResponse = (data: BatchRecordReportCellRulesRespVO) => {
  const sourceRows = [...(data.rules || []), ...(data.suggestions || [])]
  const nextRules = new Map<string, BatchRecordReportCellRuleVO>()
  sourceRows
    .map(normalizeCellRule)
    .sort((left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex)
    .forEach((rule) => nextRules.set(ruleIdentity(rule), rule))
  ruleRows.value = Array.from(nextRules.values())
  assistRows.value = normalizeAssistRows(data.assistRows || [])
  syncAssistAssignmentsWithRows()
  summary.unreviewedFillableCellCount = Number(data.unreviewedFillableCellCount) || 0
  try {
    sheetLayout.value = parseSheetLayout(data.sheetLayoutJson)
    sheetLayoutError.value = sheetLayout.value ? '' : '后端未返回表单布局，无法进入可视化规则编辑。'
  } catch (error) {
    sheetLayout.value = null
    sheetLayoutError.value = resolveErrorMessage(error, '表单布局解析失败，无法进入可视化规则编辑。')
  }
  ensureSelectedRuleStillExists()
  ensureSelectedAssistRowStillExists()
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
  assistRows.value = assistRows.value.map((row) => ({
    ...row,
    fields: row.fields.filter((field) => cellIdentity(field.rowIndex, field.columnIndex) !== key)
  }))
  selectedRuleKey.value = key
}

const removeSelectedCellFromAssistRows = () => {
  const cell = selectedCell.value
  if (!cell) return
  const key = cellIdentity(cell.rowIndex, cell.columnIndex)
  assistRows.value = assistRows.value.map((row) => ({
    ...row,
    fields: row.fields.filter((field) => cellIdentity(field.rowIndex, field.columnIndex) !== key)
  }))
}

const buildAssistRowDescriptionFromSelectedCell = () => {
  const cell = selectedCell.value
  const rule = selectedRule.value
  if (!cell) return ''
  return (
    String(rule?.helpText || rule?.label || cell.text || '').trim() ||
    `第 ${cell.rowIndex + 1} 行填写项`
  )
}

const addAssistRowFromSelectedCell = () => {
  const cell = selectedCell.value
  if (!cell) {
    throw new Error('请先选择一个单元格，再新增辅助行。')
  }
  if (!selectedRule.value) {
    enableSelectedCellRule()
  }
  const rowKey = `${ASSIST_ROW_KEY_PREFIX}_${Date.now()}_${cell.rowIndex}_${cell.columnIndex}`
  removeSelectedCellFromAssistRows()
  assistRows.value = [
    ...assistRows.value,
    {
      rowKey,
      description: buildAssistRowDescriptionFromSelectedCell(),
      sort: assistRows.value.length + 1,
      fields: [{ rowIndex: cell.rowIndex, columnIndex: cell.columnIndex }]
    }
  ]
  ensureAssistAssignment(rowKey)
  selectedAssistRowKey.value = rowKey
}

const assignSelectedCellToAssistRow = (rowKey = selectedAssistRowKey.value) => {
  const cell = selectedCell.value
  const targetRow = assistRows.value.find((row) => row.rowKey === rowKey)
  if (!cell || !targetRow) {
    throw new Error('请先选择单元格和辅助行，再分配归属。')
  }
  if (!selectedRule.value) {
    enableSelectedCellRule()
  }
  const key = cellIdentity(cell.rowIndex, cell.columnIndex)
  assistRows.value = assistRows.value.map((row) => {
    const fieldsWithoutSelectedCell = row.fields.filter(
      (field) => cellIdentity(field.rowIndex, field.columnIndex) !== key
    )
    if (row.rowKey !== rowKey) {
      return { ...row, fields: fieldsWithoutSelectedCell }
    }
    return {
      ...row,
      fields: normalizeAssistRowFields([
        ...fieldsWithoutSelectedCell,
        { rowIndex: cell.rowIndex, columnIndex: cell.columnIndex }
      ])
    }
  })
  selectedAssistRowKey.value = rowKey
}

const moveAssistRow = (rowKey: string, direction: -1 | 1) => {
  const currentIndex = assistRows.value.findIndex((row) => row.rowKey === rowKey)
  const nextIndex = currentIndex + direction
  if (currentIndex < 0 || nextIndex < 0 || nextIndex >= assistRows.value.length) return
  const nextRows = [...assistRows.value]
  const currentRow = nextRows[currentIndex]
  nextRows[currentIndex] = nextRows[nextIndex]
  nextRows[nextIndex] = currentRow
  assistRows.value = nextRows.map((row, index) => ({ ...row, sort: index + 1 }))
  selectedAssistRowKey.value = rowKey
}

const removeAssistRow = (rowKey: string) => {
  assistRows.value = assistRows.value
    .filter((row) => row.rowKey !== rowKey)
    .map((row, index) => ({ ...row, sort: index + 1 }))
  syncAssistAssignmentsWithRows()
  ensureSelectedAssistRowStillExists()
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
  selectedRule.value.constraints = cleanedRuleConstraints(selectedRule.value.constraints, value)
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

const normalizeStringSelectOptions = (options: unknown): StringSelectOption[] => {
  if (!Array.isArray(options)) return []
  return options
    .map((option) => {
      if (option == null) return null
      if (typeof option === 'string' || typeof option === 'number' || typeof option === 'boolean') {
        const value = String(option).trim()
        return value ? { label: value, value } : null
      }
      if (typeof option !== 'object') return null
      const record = option as Record<string, unknown>
      const value = String(record.value ?? record.label ?? '').trim()
      const label = String(record.label ?? value).trim()
      return value ? { label: label || value, value } : null
    })
    .filter((option): option is StringSelectOption => Boolean(option))
}

const selectedStringOptions = computed(() =>
  normalizeStringSelectOptions(selectedRule.value?.constraints?.options)
)

const setSelectedStringOptions = (options: StringSelectOption[]) => {
  const constraints = ensureSelectedRuleConstraints()
  if (!constraints) return
  constraints.selectionMode = 'single'
  constraints.options = options
}

const toggleSelectedStringDropdown = (value: boolean | string | number) => {
  const constraints = ensureSelectedRuleConstraints()
  if (!constraints) return
  if (!value) {
    delete constraints.selectionMode
    delete constraints.options
    return
  }
  const options = selectedStringOptions.value.length
    ? selectedStringOptions.value
    : [
        { label: '选项1', value: '选项1' },
        { label: '选项2', value: '选项2' }
      ]
  setSelectedStringOptions(options)
}

const addSelectedStringOption = () => {
  const nextIndex = selectedStringOptions.value.length + 1
  setSelectedStringOptions([
    ...selectedStringOptions.value,
    { label: `选项${nextIndex}`, value: `选项${nextIndex}` }
  ])
}

const updateSelectedStringOption = (optionIndex: number, value: string | number) => {
  const optionText = String(value || '').trim()
  const nextOptions = selectedStringOptions.value.map((option, index) =>
    index === optionIndex ? { label: optionText, value: optionText } : option
  )
  setSelectedStringOptions(nextOptions)
}

const removeSelectedStringOption = (optionIndex: number) => {
  setSelectedStringOptions(
    selectedStringOptions.value.filter((_, index) => index !== optionIndex)
  )
}

const normalizedAssistRowsForSave = () => {
  const rows = normalizeAssistRows(assistRows.value)
  if (ruleRows.value.length > 0 && rows.length === 0) {
    throw new Error('At least one assist row is required for fillable cells.')
  }
  const assignedCellKeys = new Set<string>()
  rows.forEach((row, rowIndex) => {
    if (!row.description.trim()) {
      throw new Error(`Assist row ${rowIndex + 1} requires a description.`)
    }
    if (row.fields.length === 0) {
      throw new Error(`Assist row ${rowIndex + 1} requires at least one cell.`)
    }
    row.fields.forEach((field) => {
      const key = cellIdentity(field.rowIndex, field.columnIndex)
      if (assignedCellKeys.has(key)) {
        throw new Error(`Cell R${field.rowIndex + 1}C${field.columnIndex + 1} cannot belong to multiple assist rows.`)
      }
      assignedCellKeys.add(key)
    })
  })
  const uncoveredRule = ruleRows.value.find((rule) => !assignedCellKeys.has(ruleIdentity(rule)))
  if (uncoveredRule) {
    throw new Error(`Cell R${uncoveredRule.rowIndex + 1}C${uncoveredRule.columnIndex + 1} is not assigned to an assist row.`)
  }
  return rows
}

const normalizedAssistAssignmentsForSave = (rows: BatchRecordReportAssistRowVO[]) => {
  return rows.map((row, rowIndex) => {
    const assignment = ensureAssistAssignment(row.rowKey)
    const candidateSourceIds = normalizeAssignmentIds(assignment.candidateSourceIds)
    if (candidateSourceIds.length === 0) {
      throw new Error(`辅助行 ${rowIndex + 1} 缺少填写人或角色。`)
    }
    return {
      scopeKey: row.rowKey,
      candidateSourceType: normalizeAssignmentSourceType(assignment.candidateSourceType),
      candidateSourceIds,
      completionPolicy: normalizeAssignmentPolicy(assignment.completionPolicy),
      enabled: true,
      remark: row.description
    }
  })
}

const validateRuleRowsBeforeSave = () => {
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
    const [data, permission, users, roles] = await Promise.all([
      BatchRecordReportApi.getCellRules(reportId.value),
      EdhrProcessFormPermissionRuleApi.getByReport(reportId.value),
      getSimpleUserList(),
      getSimpleRoleList()
    ])
    simpleUserOptions.value = users
    simpleRoleOptions.value = roles
    applyCellRulesResponse(data)
    applyAssistAssignments(permission.fillAssignments || [])
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
    const assistRowsForSave = normalizedAssistRowsForSave()
    const data = await BatchRecordReportApi.saveCellRules({
      reportId: reportId.value,
      rules: ruleRows.value.map(toManualReviewedRule),
      assistRows: assistRowsForSave
    })
    await EdhrProcessFormPermissionRuleApi.saveByReport({
      batchRecordReportId: reportId.value,
      fillAssignments: normalizedAssistAssignmentsForSave(assistRowsForSave)
    })
    applyCellRulesResponse(data)
    emit('confirmed', data)
    message.success('填写配置已保存')
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


.batch-record-cell-rules-editor__dropdown-options,
.batch-record-cell-rules-editor__assist-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.batch-record-cell-rules-editor__dropdown-switch,
.batch-record-cell-rules-editor__assist-head,
.batch-record-cell-rules-editor__assist-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.batch-record-cell-rules-editor__dropdown-option {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.batch-record-cell-rules-editor__assist-section {
  padding-top: 12px;
  border-top: 1px solid #edf1f6;
}

.batch-record-cell-rules-editor__assist-head {
  align-items: flex-start;
}

.batch-record-cell-rules-editor__assist-head strong {
  color: #172033;
  font-size: 13px;
}

.batch-record-cell-rules-editor__assist-head p {
  margin: 3px 0 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.4;
}

.batch-record-cell-rules-editor__assist-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.batch-record-cell-rules-editor__assist-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.batch-record-cell-rules-editor__assist-row.is-selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.batch-record-cell-rules-editor__assist-select {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 0;
  background: transparent;
  color: #172033;
  cursor: pointer;
  font: inherit;
  padding: 0;
  text-align: left;
}

.batch-record-cell-rules-editor__assist-select span {
  color: #667085;
  font-size: 12px;
}

</style>
