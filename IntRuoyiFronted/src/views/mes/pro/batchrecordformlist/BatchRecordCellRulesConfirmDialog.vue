<template>
  <Dialog
    v-model="dialogVisible"
    title="填写配置"
    width="calc(100vw - 32px)"
    :fullscreen="true"
    :default-fullscreen="true"
  >
    <div v-loading="loading" class="batch-record-cell-rules-editor">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />

      <section
        class="batch-record-cell-rules-editor__top-toolbar"
        data-fill-config-toolbar="primary"
      >
        <div class="batch-record-cell-rules-editor__mode-toolbar">
          <el-radio-group
            v-model="activeConfigMode"
            size="small"
            class="batch-record-cell-rules-editor__mode-switch"
          >
            <el-radio-button label="原表单配置" value="source">原表单配置</el-radio-button>
            <el-radio-button label="辅助表单映射" value="assistMapping">辅助表单映射</el-radio-button>
          </el-radio-group>
        </div>

        <div
          class="batch-record-cell-rules-editor__navigation"
          data-fill-config-navigation="same-product-version"
        >
          <span
            class="batch-record-cell-rules-editor__current-form"
            data-fill-config-current-form="name-version"
            :title="currentFormTitleLabel"
          >
            {{ currentFormTitleLabel }}
          </span>
          <div class="batch-record-cell-rules-editor__navigation-controls">
            <el-button
              plain
              size="small"
              :loading="navigationLoading"
              :disabled="!canNavigatePrevious || loading || saving || navigationLoading"
              @click="requestNavigation(-1)"
            >
              上一张
            </el-button>
            <span class="batch-record-cell-rules-editor__navigation-label">
              {{ navigationDisplayLabel }}
            </span>
            <el-button
              plain
              size="small"
              :loading="navigationLoading"
              :disabled="!canNavigateNext || loading || saving || navigationLoading"
              @click="requestNavigation(1)"
            >
              下一张
            </el-button>
          </div>
          <el-tag
            v-if="navigationErrorMessage"
            class="batch-record-cell-rules-editor__navigation-error"
            type="danger"
            effect="plain"
          >
            {{ navigationErrorMessage }}
          </el-tag>
        </div>

      </section>

      <section
        class="batch-record-cell-rules-editor__workspace"
        :class="{ 'batch-record-cell-rules-editor__workspace--assist-mapping': activeConfigMode === 'assistMapping' }"
      >
        <div class="batch-record-cell-rules-editor__preview" data-fill-config-panel="source-form">
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
                    :class="[cell.classNames, { 'is-assist-mapped': isSourceCellMappedToAssistGrid(cell) }]"
                  >
                    <button
                      type="button"
                      class="batch-record-cell-rules-editor__cell-button"
                      :aria-label="activeConfigMode === 'assistMapping' ? '映射原表单元格' : '选择单元格规则'"
                      :aria-pressed="cell.identity === selectedRuleKey"
                      :title="resolveSourceCellAssistMappingTitle(cell)"
                      @click="handleSourceCellClick(cell)"
                    >
                      <span v-if="cell.text" class="batch-record-cell-rules-editor__cell-text">
                        {{ cell.text }}
                      </span>
                      <span v-else class="batch-record-cell-rules-editor__cell-placeholder">
                        第 {{ cell.rowIndex + 1 }} 行第 {{ cell.columnIndex + 1 }} 列
                      </span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <el-empty v-else description="暂无可展示的表单布局" />
        </div>

        <div
          v-if="activeConfigMode === 'assistMapping'"
          class="batch-record-cell-rules-editor__assist-preview-panel"
          data-fill-config-panel="assist-preview"
        >
          <div class="batch-record-cell-rules-editor__assist-preview-scroll">
            <el-empty
              v-if="!selectedAssistSubject"
              description="请在右侧添加并选择责任主体"
            />
            <template v-else>
              <div class="batch-record-cell-rules-editor__assist-grid-meta">
                <strong>{{ selectedAssistSubjectLabel }}</strong>
                <el-tag size="small" effect="plain">
                  辅助表格 {{ assistGridRowCount }} × {{ assistGridColumnCount }}
                </el-tag>
              </div>
              <table class="batch-record-cell-rules-editor__assist-grid">
                <tbody>
                  <tr v-for="gridRow in assistGridPreviewRows" :key="gridRow.rowIndex">
                    <td v-for="gridCell in gridRow.cells" :key="gridCell.key">
                      <button
                        type="button"
                        class="batch-record-cell-rules-editor__assist-grid-cell"
                        :class="{
                          'is-selected': selectedAssistGridCellKey === gridCell.key,
                          'is-mapped': Boolean(gridCell.sourceCell)
                        }"
                        :data-assist-grid-cell="gridCell.key"
                        @click="handleAssistGridCellClick(gridCell.key)"
                        @dblclick.stop="handleAssistGridCellDoubleClick(gridCell)"
                      >
                        <span>{{ gridCell.label }}</span>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </template>
          </div>
        </div>

        <aside
          class="batch-record-cell-rules-editor__side-panel"
          :class="{ 'is-mapping-control': activeConfigMode === 'assistMapping' }"
          data-fill-config-panel="mapping-control"
        >
          <div class="batch-record-cell-rules-editor__side-scroll">
            <div
              v-if="activeConfigMode === 'assistMapping'"
              class="batch-record-cell-rules-editor__control-head"
            >
              <strong>映射控制栏</strong>
              <p>在这里设置辅助表格、责任主体和当前原表字段类型；中间表格会实时更新。</p>
            </div>
            <template v-if="activeConfigMode === 'assistMapping'">
              <section class="batch-record-cell-rules-editor__assist-grid-control">
                <div class="batch-record-cell-rules-editor__assist-grid-control-head">
                  <strong>辅助表格设置</strong>
                  <p>固定表格单元格；先点中间表格格子，再点左侧未灰化原表格。</p>
                </div>
                <div class="batch-record-cell-rules-editor__assist-grid-size">
                  <label>
                    <span>行数</span>
                    <el-input-number
                      v-model="assistGridRowCount"
                      :min="1"
                      :max="20"
                      :controls="false"
                      @change="handleAssistGridSizeChange"
                    />
                  </label>
                  <label>
                    <span>列数</span>
                    <el-input-number
                      v-model="assistGridColumnCount"
                      :min="1"
                      :max="20"
                      :controls="false"
                      @change="handleAssistGridSizeChange"
                    />
                  </label>
                </div>
              </section>

              <section class="batch-record-cell-rules-editor__assist-filler-control">
                <div class="batch-record-cell-rules-editor__assist-grid-control-head">
                  <strong>责任主体</strong>
                  <p>每个个人或角色拥有自己的辅助表格；原表单元格全局只能分配一次。</p>
                </div>
                <div class="batch-record-cell-rules-editor__assist-filler-add">
                  <el-select
                    v-model="pendingAssistSubjectType"
                    placeholder="类型"
                    style="width: 96px"
                  >
                    <el-option
                      v-for="option in assistSubjectTypeOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                  <el-select
                    v-model="pendingAssistSubjectId"
                    filterable
                    clearable
                    placeholder="选择责任主体"
                  >
                    <el-option
                      v-for="option in availableAssistSubjectOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                  <el-button
                    type="primary"
                    plain
                    :disabled="!pendingAssistSubjectId"
                    @click="addAssistResponsibilitySubject"
                  >
                    添加
                  </el-button>
                </div>
                <el-empty
                  v-if="assistResponsibilitySubjects.length === 0"
                  description="请先添加责任主体"
                  :image-size="56"
                />
                <div v-else class="batch-record-cell-rules-editor__assist-filler-list">
                  <article
                    v-for="subject in assistResponsibilitySubjects"
                    :key="subject.subjectKey"
                    class="batch-record-cell-rules-editor__assist-filler-item"
                    :class="{ 'is-selected': selectedAssistSubjectKey === subject.subjectKey }"
                  >
                    <button type="button" @click="selectAssistResponsibilitySubject(subject.subjectKey)">
                      <strong>{{ resolveAssistSubjectLabel(subject) }}</strong>
                      <span>{{ assistGridMappedCountBySubject(subject.subjectKey) }} 个映射</span>
                    </button>
                    <el-button
                      size="small"
                      link
                      type="danger"
                      @click="removeAssistResponsibilitySubject(subject)"
                    >
                      删除
                    </el-button>
                  </article>
                </div>
              </section>
            </template>
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
            </template>

            <el-empty v-else description="请在左侧表单中点击一个单元格" />
          </div>
          <div class="batch-record-cell-rules-editor__side-actions" data-fill-config-actions="side">
            <el-button size="small" @click="dialogVisible = false">关闭</el-button>
            <el-button
              size="small"
              :loading="loading"
              :disabled="!reportId || saving || navigationLoading"
              @click="loadCellRules"
            >
              重新读取
            </el-button>
            <el-button
              size="small"
              type="primary"
              :loading="saving"
              :disabled="!canConfirmRules"
              @click="confirmAllRules"
            >
              保存填写配置
            </el-button>
          </div>
        </aside>
      </section>
    </div>

  </Dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
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

type ReportLike = Pick<BatchRecordReportVO, 'reportId' | 'reportName' | 'batchRecordName' | 'versionNo'>

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

type ConfigMode = 'source' | 'assistMapping'

type AssistResponsibilitySubject = {
  subjectKey: string
  candidateSourceType: EdhrProcessFormCandidateSourceType
  candidateSourceIds: number[]
}

type AssistGridKey = {
  subjectKey: string
  candidateSourceType: EdhrProcessFormCandidateSourceType
  candidateSourceIds: number[]
  rowIndex: number
  columnIndex: number
}

type AssistGridPreviewCell = AssistGridKey & {
  key: string
  label: string
  sourceCell: RuleEditorCell | null
}

type AssistGridPreviewRow = {
  rowIndex: number
  cells: AssistGridPreviewCell[]
}

type SourceCellGridAssignment = AssistGridKey & {
  rowKey: string
  row: BatchRecordReportAssistRowVO
}

const props = defineProps<{
  modelValue: boolean
  report?: ReportLike | null
  canNavigatePrevious?: boolean
  canNavigateNext?: boolean
  navigationLoading?: boolean
  navigationErrorMessage?: string
  navigationLabel?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirmed: [value: BatchRecordReportCellRulesRespVO]
  navigate: [offset: -1 | 1]
}>()

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const sheetLayoutError = ref('')
const activeConfigMode = ref<ConfigMode>('source')
const selectedRuleKey = ref('')
const selectedAssistGridCellKey = ref('')
const selectedAssistSubjectKey = ref('')
const pendingAssistSubjectType = ref<EdhrProcessFormCandidateSourceType>('ROLE')
const pendingAssistSubjectId = ref<number>()
const assistGridRowCount = ref(3)
const assistGridColumnCount = ref(3)
const assistResponsibilitySubjects = ref<AssistResponsibilitySubject[]>([])
const ruleRows = ref<BatchRecordReportCellRuleVO[]>([])
const assistRows = ref<BatchRecordReportAssistRowVO[]>([])
const assistAssignments = reactive<Record<string, AssistAssignmentDraft>>({})
const savedStateSignature = ref('')
const simpleUserOptions = ref<UserVO[]>([])
const simpleRoleOptions = ref<RoleVO[]>([])
const sheetLayout = ref<RuleEditorRawLayout | null>(null)
const summary = reactive({
  unreviewedFillableCellCount: 0
})

const DEFAULT_COLUMN_WIDTH = 150
const DEFAULT_ROW_HEIGHT = 34
const ASSIST_ROW_KEY_PREFIX = 'ASSIST_ROW'
const ASSIST_GRID_ROW_KEY_PREFIX = 'ASSIST_GRID'

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const reportId = computed(() => String(props.report?.reportId || '').trim())
const reportName = computed(
  () => props.report?.reportName || props.report?.batchRecordName || props.report?.reportId || '-'
)
const currentFormTitleLabel = computed(() => {
  const formName = String(
    props.report?.reportName || props.report?.batchRecordName || props.report?.reportId || '-'
  ).trim()
  const versionNo = String(props.report?.versionNo || '').trim()
  return versionNo ? `${formName} / ${versionNo}` : formName
})
const canNavigatePrevious = computed(() => Boolean(props.canNavigatePrevious))
const canNavigateNext = computed(() => Boolean(props.canNavigateNext))
const navigationLoading = computed(() => Boolean(props.navigationLoading))
const navigationErrorMessage = computed(() => String(props.navigationErrorMessage || '').trim())
const navigationDisplayLabel = computed(() => props.navigationLabel || reportName.value)
const unreviewedFillableCellCount = computed(() => summary.unreviewedFillableCellCount)

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
  () => Boolean(reportId.value) && !loading.value && !saving.value && !navigationLoading.value
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

const findRenderedCellByIdentity = (identity: string) => {
  for (const row of renderedRows.value) {
    const cell = row.cells.find((item) => item.identity === identity)
    if (cell) return cell
  }
  return null
}

const selectedCell = computed(() => {
  if (!selectedRuleKey.value) return null
  return findRenderedCellByIdentity(selectedRuleKey.value)
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

const buildAssistSubjectKey = (
  sourceType: EdhrProcessFormCandidateSourceType,
  sourceIds: number[]
) => `${normalizeAssignmentSourceType(sourceType)}:${normalizeAssignmentIds(sourceIds).join(',')}`

const createAssistResponsibilitySubject = (
  sourceType: unknown,
  sourceIds: unknown
): AssistResponsibilitySubject | null => {
  const candidateSourceType = normalizeAssignmentSourceType(sourceType)
  const candidateSourceIds = normalizeAssignmentIds(sourceIds)
  if (candidateSourceIds.length !== 1) return null
  return {
    subjectKey: buildAssistSubjectKey(candidateSourceType, candidateSourceIds),
    candidateSourceType,
    candidateSourceIds
  }
}

const ensureAssistAssignment = (rowKey: string) => {
  if (!assistAssignments[rowKey]) {
    const parsed = parseAssistGridRowKey(rowKey)
    assistAssignments[rowKey] = parsed
      ? {
          candidateSourceType: parsed.candidateSourceType,
          candidateSourceIds: [...parsed.candidateSourceIds],
          completionPolicy: 'ANY_ONE'
        }
      : createDefaultAssistAssignment()
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

const parseAssistGridRowKey = (rowKey: string): AssistGridKey | null => {
  const normalizedRowKey = String(rowKey || '').trim()
  const subjectMatch = normalizedRowKey.match(/^ASSIST_GRID_(USERS|ROLE)(\d+)_R(\d+)_C(\d+)$/)
  const legacyUserMatch = normalizedRowKey.match(/^ASSIST_GRID_U(\d+)_R(\d+)_C(\d+)$/)
  const match = subjectMatch || legacyUserMatch
  if (!match) return null
  const candidateSourceType = subjectMatch ? normalizeAssignmentSourceType(match[1]) : 'USERS'
  const sourceId = Number(subjectMatch ? match[2] : match[1])
  const rowIndex = Number(subjectMatch ? match[3] : match[2])
  const columnIndex = Number(subjectMatch ? match[4] : match[3])
  if (!Number.isInteger(sourceId) || sourceId <= 0) return null
  if (!Number.isInteger(rowIndex) || rowIndex < 0) return null
  if (!Number.isInteger(columnIndex) || columnIndex < 0) return null
  const candidateSourceIds = [sourceId]
  return {
    subjectKey: buildAssistSubjectKey(candidateSourceType, candidateSourceIds),
    candidateSourceType,
    candidateSourceIds,
    rowIndex,
    columnIndex
  }
}

const buildAssistGridRowKey = ({
  candidateSourceType,
  candidateSourceIds,
  rowIndex,
  columnIndex
}: AssistGridKey) => {
  const sourceType = normalizeAssignmentSourceType(candidateSourceType)
  const sourceId = normalizeAssignmentIds(candidateSourceIds)[0]
  return `${ASSIST_GRID_ROW_KEY_PREFIX}_${sourceType}${sourceId}_R${rowIndex}_C${columnIndex}`
}

const parseAssistGridCellKey = (cellKey: string) => parseAssistGridRowKey(cellKey)

const assistUserOptions = computed(() =>
  simpleUserOptions.value.map((user) => ({
    label: user.nickname || user.username || String(user.id),
    value: Number(user.id)
  }))
)

const assistRoleOptions = computed(() =>
  simpleRoleOptions.value.map((role) => ({
    label: role.name || role.code || String(role.id),
    value: Number(role.id)
  }))
)

const assistSubjectTypeOptions = [
  { label: '个人', value: 'USERS' },
  { label: '角色', value: 'ROLE' }
] as const

const assistSubjectOptions = computed(() =>
  pendingAssistSubjectType.value === 'ROLE' ? assistRoleOptions.value : assistUserOptions.value
)

const resolveAssistSubjectLabel = (subject: Pick<
  AssistResponsibilitySubject,
  'candidateSourceType' | 'candidateSourceIds'
>) => {
  const sourceId = subject.candidateSourceIds[0]
  if (subject.candidateSourceType === 'ROLE') {
    return assistRoleOptions.value.find((option) => option.value === sourceId)?.label || `角色 ${sourceId}`
  }
  return assistUserOptions.value.find((option) => option.value === sourceId)?.label || `用户 ${sourceId}`
}

const sortAssistResponsibilitySubjects = (subjects: AssistResponsibilitySubject[]) => {
  const optionOrder = new Map<string, number>()
  assistRoleOptions.value.forEach((option, index) => optionOrder.set(`ROLE:${option.value}`, index))
  assistUserOptions.value.forEach((option, index) => optionOrder.set(`USERS:${option.value}`, index))
  const subjectMap = new Map<string, AssistResponsibilitySubject>()
  subjects.forEach((subject) => {
    if (!subjectMap.has(subject.subjectKey)) subjectMap.set(subject.subjectKey, subject)
  })
  return Array.from(subjectMap.values()).sort((left, right) => {
    const leftTypeOrder = left.candidateSourceType === 'ROLE' ? 0 : 1
    const rightTypeOrder = right.candidateSourceType === 'ROLE' ? 0 : 1
    if (leftTypeOrder !== rightTypeOrder) return leftTypeOrder - rightTypeOrder
    return (
      (optionOrder.get(left.subjectKey) ?? Number.MAX_SAFE_INTEGER) -
        (optionOrder.get(right.subjectKey) ?? Number.MAX_SAFE_INTEGER) ||
      resolveAssistSubjectLabel(left).localeCompare(resolveAssistSubjectLabel(right), 'zh-Hans-CN') ||
      left.candidateSourceIds[0] - right.candidateSourceIds[0]
    )
  })
}

const calculateAssistGridSort = ({ subjectKey, rowIndex, columnIndex }: AssistGridKey) => {
  const subjectIndex = assistResponsibilitySubjects.value.findIndex(
    (subject) => subject.subjectKey === subjectKey
  )
  const normalizedSubjectIndex = subjectIndex >= 0 ? subjectIndex : assistResponsibilitySubjects.value.length
  return normalizedSubjectIndex * 10000 + rowIndex * 100 + columnIndex + 1
}

const normalizeAssistGridSizeValue = (value: unknown) => {
  const numericValue = Number(value)
  return Number.isInteger(numericValue) && numericValue > 0 ? numericValue : 1
}

const orderAssistGridRows = (rows: BatchRecordReportAssistRowVO[]) =>
  [...rows]
    .map((row) => {
      const parsed = parseAssistGridRowKey(row.rowKey)
      return {
        ...row,
        sort: parsed ? calculateAssistGridSort(parsed) : row.sort
      }
    })
    .sort((left, right) => left.sort - right.sort)
    .map((row, index) => ({ ...row, sort: index + 1 }))

const normalizeRuleForDirtyCheck = (rule: BatchRecordReportCellRuleVO) => {
  const normalized = normalizeCellRule(rule)
  return {
    rowIndex: normalized.rowIndex,
    columnIndex: normalized.columnIndex,
    valueType: normalized.valueType,
    componentFlag: normalized.componentFlag || '',
    required: Boolean(normalized.required),
    label: normalized.label || '',
    placeholder: normalized.placeholder || '',
    helpText: normalized.helpText || '',
    constraints: cleanedRuleConstraints(normalized.constraints, normalized.valueType),
    unit: normalized.unit || '',
    attachmentRule: normalized.attachmentRule ? { ...normalized.attachmentRule } : undefined
  }
}

const stableDirtyValue = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(stableDirtyValue)
  if (!value || typeof value !== 'object') return value
  return Object.keys(value as Record<string, unknown>)
    .sort()
    .reduce<Record<string, unknown>>((record, key) => {
      record[key] = stableDirtyValue((value as Record<string, unknown>)[key])
      return record
    }, {})
}

const buildEditableStateSignature = () =>
  JSON.stringify(
    stableDirtyValue({
      rules: sortRules(ruleRows.value).map(normalizeRuleForDirtyCheck),
      assistRows: orderAssistGridRows(normalizeAssistRows(assistRows.value)),
      assistAssignments: Object.keys(assistAssignments)
        .sort()
        .map((rowKey) => ({
          rowKey,
          candidateSourceType: normalizeAssignmentSourceType(assistAssignments[rowKey]?.candidateSourceType),
          candidateSourceIds: normalizeAssignmentIds(assistAssignments[rowKey]?.candidateSourceIds),
          completionPolicy: normalizeAssignmentPolicy(assistAssignments[rowKey]?.completionPolicy)
        })),
      assistGridRowCount: assistGridRowCount.value,
      assistGridColumnCount: assistGridColumnCount.value,
      assistResponsibilitySubjects: assistResponsibilitySubjects.value.map((subject) => ({
        subjectKey: subject.subjectKey,
        candidateSourceType: subject.candidateSourceType,
        candidateSourceIds: normalizeAssignmentIds(subject.candidateSourceIds)
      }))
    })
  )

const markEditableStateClean = () => {
  savedStateSignature.value = buildEditableStateSignature()
}

const hasUnsavedChanges = computed(
  () => Boolean(savedStateSignature.value) && buildEditableStateSignature() !== savedStateSignature.value
)

const requestNavigation = async (offset: -1 | 1) => {
  if (loading.value || saving.value || navigationLoading.value) return
  if (offset < 0 && !canNavigatePrevious.value) return
  if (offset > 0 && !canNavigateNext.value) return
  if (hasUnsavedChanges.value) {
    try {
      await ElMessageBox.confirm(
        '当前填写配置有未保存修改，切换后会放弃这些修改。是否继续？',
        '放弃未保存修改并切换表单',
        {
          confirmButtonText: '放弃并切换',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch (error) {
      if (error === 'cancel' || error === 'close') return
      throw error
    }
  }
  emit('navigate', offset)
}

const selectedAssistSubject = computed(() =>
  assistResponsibilitySubjects.value.find((subject) => subject.subjectKey === selectedAssistSubjectKey.value)
)

const ensureSelectedAssistSubjectStillExists = () => {
  if (selectedAssistSubject.value) return
  selectedAssistSubjectKey.value = assistResponsibilitySubjects.value[0]?.subjectKey || ''
}

const ensureSelectedAssistGridCellStillExists = () => {
  const current = selectedAssistGridCellKey.value
  const parsed = current ? parseAssistGridCellKey(current) : null
  const subject = selectedAssistSubject.value
  if (
    parsed &&
    subject &&
    subject.subjectKey === parsed.subjectKey &&
    parsed.rowIndex < assistGridRowCount.value &&
    parsed.columnIndex < assistGridColumnCount.value &&
    assistResponsibilitySubjects.value.some((item) => item.subjectKey === parsed.subjectKey)
  ) {
    return
  }
  selectedAssistGridCellKey.value = subject
    ? buildAssistGridRowKey({
        ...subject,
        rowIndex: 0,
        columnIndex: 0
      })
    : ''
}

const syncAssistGridStateWithRows = (rows = assistRows.value) => {
  const subjects = [...assistResponsibilitySubjects.value]
  let nextRowCount = normalizeAssistGridSizeValue(assistGridRowCount.value)
  let nextColumnCount = normalizeAssistGridSizeValue(assistGridColumnCount.value)
  rows.forEach((row) => {
    const parsed = parseAssistGridRowKey(row.rowKey)
    if (!parsed) return
    const assignment = assistAssignments[row.rowKey]
    const subject = createAssistResponsibilitySubject(
      assignment?.candidateSourceType || parsed.candidateSourceType,
      assignment?.candidateSourceIds?.length ? assignment.candidateSourceIds : parsed.candidateSourceIds
    )
    if (subject) subjects.push(subject)
    nextRowCount = Math.max(nextRowCount, parsed.rowIndex + 1)
    nextColumnCount = Math.max(nextColumnCount, parsed.columnIndex + 1)
  })
  assistResponsibilitySubjects.value = sortAssistResponsibilitySubjects(subjects)
  assistGridRowCount.value = nextRowCount
  assistGridColumnCount.value = nextColumnCount
  ensureSelectedAssistSubjectStillExists()
  ensureSelectedAssistGridCellStillExists()
}

const applyAssistAssignments = (assignments: EdhrProcessFormFillAssignment[] = []) => {
  assignments.forEach((assignment) => {
    const rowKey = String(assignment.scopeKey || '').trim()
    if (!rowKey) return
    const candidateSourceType = normalizeAssignmentSourceType(assignment.candidateSourceType)
    const candidateSourceIds = normalizeAssignmentIds(assignment.candidateSourceIds)
    assistAssignments[rowKey] = {
      candidateSourceType,
      candidateSourceIds,
      completionPolicy: normalizeAssignmentPolicy(assignment.completionPolicy)
    }
  })
  syncAssistAssignmentsWithRows()
  syncAssistGridStateWithRows()
}

const availableAssistSubjectOptions = computed(() => {
  const selectedKeys = new Set(assistResponsibilitySubjects.value.map((subject) => subject.subjectKey))
  return assistSubjectOptions.value.filter((option) => {
    const subject = createAssistResponsibilitySubject(pendingAssistSubjectType.value, [option.value])
    return subject ? !selectedKeys.has(subject.subjectKey) : false
  })
})

const selectedAssistSubjectLabel = computed(() =>
  selectedAssistSubject.value
    ? resolveAssistSubjectLabel(selectedAssistSubject.value)
    : '未选择责任主体'
)

const assistGridRowMap = computed(() => {
  const map = new Map<string, BatchRecordReportAssistRowVO>()
  assistRows.value.forEach((row) => {
    if (parseAssistGridRowKey(row.rowKey)) {
      map.set(row.rowKey, row)
    }
  })
  return map
})

const findAssistGridRowBySubjectCell = (
  subject: AssistResponsibilitySubject,
  rowIndex: number,
  columnIndex: number
) => {
  const directKey = buildAssistGridRowKey({ ...subject, rowIndex, columnIndex })
  const directRow = assistGridRowMap.value.get(directKey)
  if (directRow) return directRow
  return assistRows.value.find((row) => {
    const parsed = parseAssistGridRowKey(row.rowKey)
    if (!parsed || parsed.rowIndex !== rowIndex || parsed.columnIndex !== columnIndex) return false
    const assignment = assistAssignments[row.rowKey]
    const assignedSubject = createAssistResponsibilitySubject(
      assignment?.candidateSourceType,
      assignment?.candidateSourceIds
    )
    return assignedSubject?.subjectKey === subject.subjectKey
  })
}

const sourceCellGridAssignmentMap = computed(() => {
  const map = new Map<string, SourceCellGridAssignment>()
  assistRows.value.forEach((row) => {
    const parsed = parseAssistGridRowKey(row.rowKey)
    const field = row.fields[0]
    if (!parsed || !field) return
    const key = cellIdentity(field.rowIndex, field.columnIndex)
    if (!map.has(key)) {
      const assignment = assistAssignments[row.rowKey]
      const subject = createAssistResponsibilitySubject(
        assignment?.candidateSourceType || parsed.candidateSourceType,
        assignment?.candidateSourceIds?.length ? assignment.candidateSourceIds : parsed.candidateSourceIds
      )
      map.set(key, { ...parsed, ...(subject || parsed), rowKey: row.rowKey, row })
    }
  })
  return map
})

const isSourceCellMappedToAssistGrid = (cell: RuleEditorCell) =>
  sourceCellGridAssignmentMap.value.has(cell.identity)

const resolveSourceCellAssistMappingTitle = (cell: RuleEditorCell) => {
  const assignment = sourceCellGridAssignmentMap.value.get(cell.identity)
  if (activeConfigMode.value !== 'assistMapping') return '选择单元格规则'
  if (assignment) {
    return `已链接到 ${resolveAssistSubjectLabel(assignment)} 的辅助表格单元格，点击可同步选中`
  }
  if (!selectedAssistGridCellKey.value) return '请先点击黄色辅助表格单元格'
  return '点击后映射到当前辅助表格单元格'
}

const resolveAssistGridPreviewCell = (
  subject: AssistResponsibilitySubject,
  rowIndex: number,
  columnIndex: number
): AssistGridPreviewCell => {
  const key = buildAssistGridRowKey({ ...subject, rowIndex, columnIndex })
  const assistRow = findAssistGridRowBySubjectCell(subject, rowIndex, columnIndex)
  const field = assistRow?.fields[0]
  const sourceCell = field ? findRenderedCellByIdentity(cellIdentity(field.rowIndex, field.columnIndex)) : null
  const rule = field ? ruleMap.value.get(cellIdentity(field.rowIndex, field.columnIndex)) : undefined
  const sourceText = String(sourceCell?.text || '').trim()
  const label = String(rule?.label || sourceText || '点击选择原表格').trim()
  return {
    key: assistRow?.rowKey || key,
    ...subject,
    rowIndex,
    columnIndex,
    label,
    sourceCell
  }
}

const assistGridPreviewRows = computed<AssistGridPreviewRow[]>(() => {
  const subject = selectedAssistSubject.value
  if (!subject) return []
  const rowCount = normalizeAssistGridSizeValue(assistGridRowCount.value)
  const columnCount = normalizeAssistGridSizeValue(assistGridColumnCount.value)
  return Array.from({ length: rowCount }, (_, rowIndex) => ({
    rowIndex,
    cells: Array.from({ length: columnCount }, (_, columnIndex) =>
      resolveAssistGridPreviewCell(subject, rowIndex, columnIndex)
    )
  }))
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
  assistRows.value = normalizeAssistRows(data.assistRows || [])
  assistGridRowCount.value = normalizeAssistGridSizeValue(data.assistGridRowCount)
  assistGridColumnCount.value = normalizeAssistGridSizeValue(data.assistGridColumnCount)
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
  syncAssistGridStateWithRows()
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
  assistRows.value = orderAssistGridRows(
    assistRows.value.filter((row) =>
      row.fields.every((field) => cellIdentity(field.rowIndex, field.columnIndex) !== key)
    )
  )
  syncAssistAssignmentsWithRows()
  selectedRuleKey.value = key
}

const selectAssistResponsibilitySubject = (subjectKey: string) => {
  if (!assistResponsibilitySubjects.value.some((subject) => subject.subjectKey === subjectKey)) return
  selectedAssistSubjectKey.value = subjectKey
  ensureSelectedAssistGridCellStillExists()
}

const addAssistResponsibilitySubject = () => {
  const sourceId = Number(pendingAssistSubjectId.value)
  if (!Number.isFinite(sourceId) || sourceId <= 0) return
  const subject = createAssistResponsibilitySubject(pendingAssistSubjectType.value, [sourceId])
  if (!subject) return
  if (!assistSubjectOptions.value.some((option) => option.value === sourceId)) {
    message.warning('请选择有效责任主体。')
    return
  }
  if (!assistResponsibilitySubjects.value.some((item) => item.subjectKey === subject.subjectKey)) {
    assistResponsibilitySubjects.value = sortAssistResponsibilitySubjects([
      ...assistResponsibilitySubjects.value,
      subject
    ])
  }
  pendingAssistSubjectId.value = undefined
  selectedAssistSubjectKey.value = subject.subjectKey
  ensureSelectedAssistGridCellStillExists()
}

const assistGridMappedCountBySubject = (subjectKey: string) =>
  assistRows.value.filter((row) => {
    const parsed = parseAssistGridRowKey(row.rowKey)
    if (!parsed) return false
    const assignment = assistAssignments[row.rowKey]
    const assignedSubject = createAssistResponsibilitySubject(
      assignment?.candidateSourceType || parsed.candidateSourceType,
      assignment?.candidateSourceIds?.length ? assignment.candidateSourceIds : parsed.candidateSourceIds
    )
    return assignedSubject?.subjectKey === subjectKey
  }).length

const removeAssistResponsibilitySubject = async (subject: AssistResponsibilitySubject) => {
  const mappedCount = assistGridMappedCountBySubject(subject.subjectKey)
  if (mappedCount > 0) {
    try {
      await ElMessageBox.confirm(
        `删除 ${resolveAssistSubjectLabel(subject)} 会同时移除该责任主体的 ${mappedCount} 个映射，是否继续？`,
        '删除责任主体',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch (error) {
      if (error === 'cancel' || error === 'close') return
      throw error
    }
  }
  assistResponsibilitySubjects.value = assistResponsibilitySubjects.value.filter(
    (item) => item.subjectKey !== subject.subjectKey
  )
  assistRows.value = orderAssistGridRows(
    assistRows.value.filter((row) => {
      const parsed = parseAssistGridRowKey(row.rowKey)
      if (!parsed) return true
      const assignment = assistAssignments[row.rowKey]
      const assignedSubject = createAssistResponsibilitySubject(
        assignment?.candidateSourceType || parsed.candidateSourceType,
        assignment?.candidateSourceIds?.length ? assignment.candidateSourceIds : parsed.candidateSourceIds
      )
      return assignedSubject?.subjectKey !== subject.subjectKey
    })
  )
  Object.keys(assistAssignments).forEach((rowKey) => {
    const parsed = parseAssistGridRowKey(rowKey)
    const assignment = assistAssignments[rowKey]
    const assignedSubject = createAssistResponsibilitySubject(
      assignment?.candidateSourceType || parsed?.candidateSourceType,
      assignment?.candidateSourceIds?.length ? assignment.candidateSourceIds : parsed?.candidateSourceIds
    )
    if (assignedSubject?.subjectKey === subject.subjectKey) {
      delete assistAssignments[rowKey]
    }
  })
  if (
    pendingAssistSubjectType.value === subject.candidateSourceType &&
    pendingAssistSubjectId.value === subject.candidateSourceIds[0]
  ) {
    pendingAssistSubjectId.value = undefined
  }
  ensureSelectedAssistSubjectStillExists()
  ensureSelectedAssistGridCellStillExists()
}

const handleAssistGridSizeChange = () => {
  assistGridRowCount.value = normalizeAssistGridSizeValue(assistGridRowCount.value)
  assistGridColumnCount.value = normalizeAssistGridSizeValue(assistGridColumnCount.value)
  const mappedGridKeys = assistRows.value
    .map((row) => parseAssistGridRowKey(row.rowKey))
    .filter((key): key is AssistGridKey => Boolean(key))
  const minRows = Math.max(1, ...mappedGridKeys.map((key) => key.rowIndex + 1))
  const minColumns = Math.max(1, ...mappedGridKeys.map((key) => key.columnIndex + 1))
  if (assistGridRowCount.value < minRows) {
    assistGridRowCount.value = minRows
    message.warning('已有映射位于更靠后的行，需先取消映射后才能缩小行数。')
  }
  if (assistGridColumnCount.value < minColumns) {
    assistGridColumnCount.value = minColumns
    message.warning('已有映射位于更靠后的列，需先取消映射后才能缩小列数。')
  }
  ensureSelectedAssistGridCellStillExists()
}

const handleAssistGridCellClick = (cellKey: string) => {
  const parsed = parseAssistGridCellKey(cellKey)
  if (!parsed || !assistResponsibilitySubjects.value.some((subject) => subject.subjectKey === parsed.subjectKey)) return
  selectedAssistSubjectKey.value = parsed.subjectKey
  selectedAssistGridCellKey.value = cellKey
}

const handleAssistGridCellDoubleClick = (gridCell: AssistGridPreviewCell) => {
  handleAssistGridCellClick(gridCell.key)
  if (!gridCell.sourceCell) return
  removeAssistGridCellMapping(gridCell.key)
}

const removeAssistGridCellMapping = (cellKey = selectedAssistGridCellKey.value) => {
  if (!cellKey) return
  assistRows.value = orderAssistGridRows(assistRows.value.filter((row) => row.rowKey !== cellKey))
  delete assistAssignments[cellKey]
  syncAssistAssignmentsWithRows()
}

const buildAssistGridCellDescription = (cell: RuleEditorCell) => {
  const rule = ruleMap.value.get(cell.identity)
  return String(rule?.helpText || rule?.label || cell.text || '辅助填写项').trim()
}

const selectLinkedAssistGridCellForSourceCell = (cell: RuleEditorCell) => {
  const linkedAssignment = sourceCellGridAssignmentMap.value.get(cell.identity)
  if (!linkedAssignment) return false
  selectedRuleKey.value = cell.identity
  selectedAssistSubjectKey.value = linkedAssignment.subjectKey
  selectedAssistGridCellKey.value = linkedAssignment.rowKey
  return true
}

const mapSourceCellToSelectedAssistGridCell = (cell: RuleEditorCell) => {
  if (activeConfigMode.value !== 'assistMapping') return false
  if (selectLinkedAssistGridCellForSourceCell(cell)) return true
  if (!selectedAssistSubject.value) {
    message.warning('请先在右侧添加并选择责任主体。')
    return true
  }
  const parsed = parseAssistGridCellKey(selectedAssistGridCellKey.value)
  if (!parsed || parsed.subjectKey !== selectedAssistSubject.value.subjectKey) {
    message.warning('请先点击黄色辅助表格中的一个单元格。')
    return true
  }
  selectedRuleKey.value = cell.identity
  if (!selectedRule.value) {
    enableSelectedCellRule()
  }
  const rowKey = buildAssistGridRowKey(parsed)
  const nextRow: BatchRecordReportAssistRowVO = {
    rowKey,
    description: buildAssistGridCellDescription(cell),
    sort: calculateAssistGridSort(parsed),
    fields: [{ rowIndex: cell.rowIndex, columnIndex: cell.columnIndex }]
  }
  assistRows.value = orderAssistGridRows([
    ...assistRows.value.filter((row) => row.rowKey !== rowKey),
    nextRow
  ])
  assistAssignments[rowKey] = {
    candidateSourceType: selectedAssistSubject.value.candidateSourceType,
    candidateSourceIds: [...selectedAssistSubject.value.candidateSourceIds],
    completionPolicy: 'ANY_ONE'
  }
  return true
}

const handleSourceCellClick = (cell: RuleEditorCell) => {
  if (mapSourceCellToSelectedAssistGridCell(cell)) return
  selectRuleCell(cell)
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
  if (ruleRows.value.length === 0) return []
  if (assistResponsibilitySubjects.value.length === 0) {
    throw new Error('请先添加至少一个辅助表格责任主体。')
  }
  const rows = orderAssistGridRows(normalizeAssistRows(assistRows.value))
  if (rows.length === 0) {
    throw new Error('请先在辅助表格中完成原表单元格映射。')
  }
  const assignedCellKeys = new Set<string>()
  rows.forEach((row, rowIndex) => {
    const parsed = parseAssistGridRowKey(row.rowKey)
    if (!parsed) {
      throw new Error('存在旧版辅助映射，请切换到辅助表格后重新映射。')
    }
    const assignment = assistAssignments[row.rowKey]
    const assignedSubject = createAssistResponsibilitySubject(
      assignment?.candidateSourceType || parsed.candidateSourceType,
      assignment?.candidateSourceIds?.length ? assignment.candidateSourceIds : parsed.candidateSourceIds
    )
    if (
      !assignedSubject ||
      !assistResponsibilitySubjects.value.some((subject) => subject.subjectKey === assignedSubject.subjectKey)
    ) {
      throw new Error(`辅助表格 ${rowIndex + 1} 的责任主体已被删除，请重新选择责任主体。`)
    }
    if (parsed.rowIndex >= assistGridRowCount.value || parsed.columnIndex >= assistGridColumnCount.value) {
      throw new Error(`辅助表格 ${rowIndex + 1} 超出当前表格范围，请先取消该映射。`)
    }
    if (!row.description.trim()) {
      throw new Error(`辅助表格 ${rowIndex + 1} 缺少描述。`)
    }
    if (row.fields.length !== 1) {
      throw new Error(`辅助表格 ${rowIndex + 1} 必须且只能映射一个原表单元格。`)
    }
    row.fields.forEach((field) => {
      const key = cellIdentity(field.rowIndex, field.columnIndex)
      if (assignedCellKeys.has(key)) {
        throw new Error(`原表单元格 R${field.rowIndex + 1}C${field.columnIndex + 1} 不能分配给多个责任主体。`)
      }
      assignedCellKeys.add(key)
    })
  })
  const uncoveredRule = ruleRows.value.find((rule) => !assignedCellKeys.has(ruleIdentity(rule)))
  if (uncoveredRule) {
    throw new Error(`原表单元格 R${uncoveredRule.rowIndex + 1}C${uncoveredRule.columnIndex + 1} 尚未分配给责任主体。`)
  }
  return rows
}

const normalizedAssistAssignmentsForSave = (
  rows: BatchRecordReportAssistRowVO[]
): EdhrProcessFormFillAssignment[] => {
  return rows.map((row) => {
    const parsed = parseAssistGridRowKey(row.rowKey)
    if (!parsed) {
      throw new Error('存在无法保存的辅助表格映射。')
    }
    const assignment = assistAssignments[row.rowKey]
    const candidateSourceType = normalizeAssignmentSourceType(assignment?.candidateSourceType)
    const candidateSourceIds = normalizeAssignmentIds(assignment?.candidateSourceIds)
    if (!candidateSourceIds.length) {
      throw new Error(`辅助表格 ${row.rowKey} 缺少责任主体。`)
    }
    return {
      scopeKey: row.rowKey,
      candidateSourceType,
      candidateSourceIds,
      completionPolicy: 'ANY_ONE' as const,
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
    markEditableStateClean()
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
      assistRows: assistRowsForSave,
      assistGridRowCount: normalizeAssistGridSizeValue(assistGridRowCount.value),
      assistGridColumnCount: normalizeAssistGridSizeValue(assistGridColumnCount.value)
    })
    await EdhrProcessFormPermissionRuleApi.saveByReport({
      batchRecordReportId: reportId.value,
      fillAssignments: normalizedAssistAssignmentsForSave(assistRowsForSave)
    })
    applyCellRulesResponse(data)
    markEditableStateClean()
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
  () => pendingAssistSubjectType.value,
  () => {
    pendingAssistSubjectId.value = undefined
  }
)

watch(
  () => [dialogVisible.value, reportId.value] as const,
  ([visible, currentReportId]) => {
    if (!visible) {
      savedStateSignature.value = ''
      return
    }
    if (!currentReportId) return
    activeConfigMode.value = 'source'
    void loadCellRules()
  },
  { immediate: true }
)
</script>

<style scoped>
.batch-record-cell-rules-editor {
  display: flex;
  height: calc(100vh - 84px);
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.batch-record-cell-rules-editor__top-toolbar {
  display: grid;
  min-height: 42px;
  grid-template-columns: minmax(220px, 0.8fr) minmax(280px, 1.2fr);
  gap: 12px;
  align-items: center;
}

.batch-record-cell-rules-editor__mode-toolbar,
.batch-record-cell-rules-editor__navigation {
  display: flex;
  min-width: 0;
  align-items: center;
}

.batch-record-cell-rules-editor__mode-toolbar {
  justify-content: flex-start;
}

.batch-record-cell-rules-editor__mode-switch {
  flex: 0 0 auto;
}

.batch-record-cell-rules-editor__navigation {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) auto minmax(120px, 1fr);
  gap: 8px;
  justify-content: stretch;
  padding: 4px 8px;
  border: 1px solid #f1d36d;
  border-radius: 8px;
  background: #fff8d6;
}

.batch-record-cell-rules-editor__current-form {
  min-width: 0;
  overflow: hidden;
  color: #5a3d05;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-cell-rules-editor__navigation-controls {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.batch-record-cell-rules-editor__navigation-label {
  min-width: 0;
  max-width: 240px;
  overflow: hidden;
  color: #5a3d05;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-cell-rules-editor__navigation-error {
  max-width: 180px;
  overflow: hidden;
  justify-self: end;
  text-overflow: ellipsis;
}

.batch-record-cell-rules-editor__workspace {
  display: grid;
  flex: 1;
  height: auto;
  min-height: 0;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 14px;
}

.batch-record-cell-rules-editor__workspace--assist-mapping {
  grid-template-columns: minmax(320px, 1fr) minmax(280px, 0.85fr) 360px;
}

.batch-record-cell-rules-editor__preview,
.batch-record-cell-rules-editor__assist-preview-panel,
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

.batch-record-cell-rules-editor__assist-preview-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  border-color: #f1d36d;
  background: #fff8d6;
}

.batch-record-cell-rules-editor__side-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 0;
  overflow: hidden;
  padding: 0;
}

.batch-record-cell-rules-editor__side-panel.is-mapping-control {
  border-color: #9cc7ff;
  background: #eaf3ff;
}

.batch-record-cell-rules-editor__side-scroll {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  padding: 12px;
}

.batch-record-cell-rules-editor__side-actions {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #cfe1f8;
  background: #fff;
}

.batch-record-cell-rules-editor__control-head {
  margin: -2px -2px 2px;
  padding: 10px 12px;
  border: 1px solid #b9d7ff;
  border-radius: 8px;
  background: #dcecff;
}

.batch-record-cell-rules-editor__control-head strong {
  display: block;
  color: #123b72;
  font-size: 14px;
}

.batch-record-cell-rules-editor__control-head p {
  margin: 4px 0 0;
  color: #31547c;
  font-size: 12px;
  line-height: 1.4;
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

.batch-record-cell-rules-editor__assist-preview-scroll {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
  padding: 12px;
}

.batch-record-cell-rules-editor__assist-grid-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #5a3d05;
}

.batch-record-cell-rules-editor__assist-grid {
  min-width: 420px;
  width: 100%;
  border-collapse: separate;
  border-spacing: 8px;
  table-layout: fixed;
}

.batch-record-cell-rules-editor__assist-grid td {
  position: relative;
  padding: 0;
  vertical-align: stretch;
}

.batch-record-cell-rules-editor__assist-grid-cell {
  display: flex;
  width: 100%;
  min-height: 96px;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  border: 1px solid #edd07b;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.78);
  color: #5a3d05;
  cursor: pointer;
  font: inherit;
  padding: 12px;
  text-align: left;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}

.batch-record-cell-rules-editor__assist-grid-cell:hover,
.batch-record-cell-rules-editor__assist-grid-cell.is-selected {
  border-color: #d29b13;
  background: #fffdf4;
  box-shadow: 0 0 0 2px rgba(210, 155, 19, 0.18);
}

.batch-record-cell-rules-editor__assist-grid-cell.is-mapped {
  border-color: #d9a821;
  background: #fff;
}

.batch-record-cell-rules-editor__assist-grid-cell.is-mapped.is-selected {
  border-color: #16a34a;
  background: #f0fdf4;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.22);
}

.batch-record-cell-rules-editor__assist-grid-cell span {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.batch-record-cell-rules-editor__workspace--assist-mapping .batch-record-cell-rules-editor__cell.is-selected {
  outline-color: #16a34a;
  box-shadow: inset 0 0 0 1px #16a34a;
}

.batch-record-cell-rules-editor__cell.is-assist-mapped {
  background: #e5e7eb;
  color: #7a8291;
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

.batch-record-cell-rules-editor__cell-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.batch-record-cell-rules-editor__cell-button:disabled:hover {
  background: transparent;
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

.batch-record-cell-rules-editor__assist-grid-control,
.batch-record-cell-rules-editor__assist-filler-control {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  border: 1px solid #b9d7ff;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.66);
}

.batch-record-cell-rules-editor__assist-grid-control-head strong {
  color: #123b72;
  font-size: 13px;
}

.batch-record-cell-rules-editor__assist-grid-control-head p {
  margin: 4px 0 0;
  color: #31547c;
  font-size: 12px;
  line-height: 1.4;
}

.batch-record-cell-rules-editor__assist-grid-size,
.batch-record-cell-rules-editor__assist-filler-add {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
}

.batch-record-cell-rules-editor__assist-grid-size label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #31547c;
  font-size: 12px;
}

.batch-record-cell-rules-editor__assist-filler-add {
  grid-template-columns: minmax(0, 1fr) auto;
}

.batch-record-cell-rules-editor__assist-filler-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.batch-record-cell-rules-editor__assist-filler-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 8px;
  border: 1px solid #cfe0f6;
  border-radius: 8px;
  background: #fff;
}

.batch-record-cell-rules-editor__assist-filler-item.is-selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.batch-record-cell-rules-editor__assist-filler-item > button {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
  border: 0;
  background: transparent;
  color: #172033;
  cursor: pointer;
  font: inherit;
  padding: 0;
  text-align: left;
}

.batch-record-cell-rules-editor__assist-filler-item span {
  color: #667085;
  font-size: 12px;
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

.batch-record-cell-rules-editor__assist-select-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.batch-record-cell-rules-editor__assist-select-copy > span {
  overflow: hidden;
  max-width: 220px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .batch-record-cell-rules-editor__top-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .batch-record-cell-rules-editor__navigation {
    grid-template-columns: minmax(0, 1fr);
    justify-content: stretch;
  }

  .batch-record-cell-rules-editor__navigation-controls {
    justify-content: flex-start;
  }

  .batch-record-cell-rules-editor__navigation-error {
    justify-self: start;
  }

  .batch-record-cell-rules-editor__workspace,
  .batch-record-cell-rules-editor__workspace--assist-mapping {
    height: auto;
    max-height: none;
    grid-template-columns: minmax(0, 1fr);
  }

  .batch-record-cell-rules-editor__preview,
  .batch-record-cell-rules-editor__assist-preview-panel,
  .batch-record-cell-rules-editor__side-panel {
    min-height: 320px;
  }
}

</style>
