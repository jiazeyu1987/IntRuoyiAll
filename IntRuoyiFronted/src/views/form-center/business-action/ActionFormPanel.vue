<template>
  <div class="form-action-panel">
    <el-alert
      v-if="blockerCode"
      :title="blockerTitle"
      :closable="false"
      show-icon
      type="error"
    />

    <div class="form-action-panel__actions">
      <el-button :disabled="disabled" :loading="loading" type="primary" @click="resolveAction">
        <Icon class="mr-5px" icon="ep:connection" />
        解析
      </el-button>
      <el-button
        :disabled="disabled || Boolean(instanceId) || !resolution?.requiresForm"
        :loading="loading"
        type="primary"
        @click="createInstance"
      >
        <Icon class="mr-5px" icon="ep:document-add" />
        创建
      </el-button>
      <el-button
        :disabled="disabled || !instanceId"
        :loading="loading"
        type="primary"
        plain
        @click="saveDraft"
      >
        <Icon class="mr-5px" icon="ep:document-checked" />
        保存草稿
      </el-button>
      <el-button
        :disabled="disabled || !instanceId"
        :loading="loading"
        type="success"
        @click="submitInstance"
      >
        <Icon class="mr-5px" icon="ep:promotion" />
        提交
      </el-button>
      <el-button
        :disabled="disabled || !instanceId"
        :loading="loading"
        type="warning"
        @click="reworkSubmit"
      >
        <Icon class="mr-5px" icon="ep:refresh-left" />
        重提
      </el-button>
      <el-button
        :disabled="disabled || !instanceId"
        :loading="loading"
        type="danger"
        @click="abandonInstance"
      >
        <Icon class="mr-5px" icon="ep:close" />
        放弃
      </el-button>
    </div>

    <el-descriptions v-if="instanceId" :column="3" border size="small">
      <el-descriptions-item label="实例编号">{{ instanceCode }}</el-descriptions-item>
      <el-descriptions-item label="实例ID">{{ instanceId }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusTagType(instanceStatus)">{{ statusLabel(instanceStatus) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item v-if="bpmProcessInstanceId" label="BPM流程">
        {{ bpmProcessInstanceId }}
      </el-descriptions-item>
    </el-descriptions>

    <section v-if="actionPanelSheetLayoutJson" class="form-action-panel__editable-surface">
      <div class="form-action-panel__editable-head">
        <strong>表单填写</strong>
        <span>{{ actionPanelTemplateTitle }}</span>
      </div>
      <EdhrExecutionTemplateEditableForm
        v-model="editableTemplateFormData"
        :sheet-layout-json="actionPanelSheetLayoutJson"
        :cell-rules="actionPanelCellRules"
        :signature-markers="actionPanelSignatureMarkers"
        :field-identity-map="actionPanelFieldIdentityMap"
        fit-to-viewport
        fit-mode="width"
      />
    </section>

    <el-collapse v-if="snapshots.length" class="form-action-panel__snapshots">
      <el-collapse-item title="快照" name="snapshots">
        <el-table :data="snapshots" border size="small">
          <el-table-column label="版本" prop="snapshotVersion" width="90" />
          <el-table-column label="类型" prop="snapshotType" width="140">
            <template #default="{ row }">
              <el-tag>{{ snapshotTypeLabel(row.snapshotType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" prop="createdTime" width="180" :formatter="dateTimeValueFormatter" />
          <el-table-column label="表单数据" min-width="260">
            <template #default="{ row }">
              <span class="form-action-panel__json-preview">{{ stringifySnapshot(row.formData) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-collapse-item>
    </el-collapse>

    <el-table v-if="resolution?.slots?.length" :data="resolution.slots" border size="small">
      <el-table-column label="槽位" prop="slotCode" width="160" />
      <el-table-column label="模板名称" min-width="180">
        <template #default="{ row }">
          {{ row.templateVersionRef.templateName }}
        </template>
      </el-table-column>
      <el-table-column label="版本" width="120">
        <template #default="{ row }">
          {{ row.templateVersionRef.versionNo }}
        </template>
      </el-table-column>
      <el-table-column label="必填" prop="required" width="100">
        <template #default="{ row }">
          <el-tag :type="row.required ? 'danger' : 'info'">{{ row.required ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import {
  resolveBusinessAction,
  type BusinessActionContextVO,
  type FormActionResolutionVO
} from '@/api/form-center/businessAction'
import { resolveProjectionErrorMessage } from '@/api/form-center/actionProjection'
import {
  abandonFormInstance,
  createFormInstance,
  getInstanceSnapshots,
  reworkSubmitFormInstance,
  saveFormDraft,
  submitFormInstance,
  type FormInstanceSnapshotVO,
  type FormInstanceStatus,
  type SubmitFormInstanceReqVO
} from '@/api/form-center/instance'
import {
  type FormRecognizedFieldVO,
  type FormTemplateListItemVO
} from '@/api/form-center/template'
import type {
  BatchRecordReportCellRuleVO,
  BatchRecordReportCellValueType,
  BatchRecordReportSignatureCellMarkerVO
} from '@/api/mes/pro/batchrecordreport'
import { dateTimeValueFormatter } from '@/utils/formatTime'
import {
  buildTemplateFieldIdentity,
  type TemplateSimulationValueMap
} from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'
import EdhrExecutionTemplateEditableForm from '@/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue'

defineOptions({ name: 'FormCenterBusinessActionPanel' })

const props = defineProps<{
  context: BusinessActionContextVO
  formData: Record<string, unknown>
  idempotencyKey: string
  disabled?: boolean
  initialInstanceId?: number
  initialInstanceCode?: string
  initialInstanceStatus?: FormInstanceStatus
  initialBpmProcessInstanceId?: string
}>()

type FormTemplateJimuSchemaPayload = {
  [key: string]: unknown
  sheetLayoutJson?: unknown
  layout?: unknown
  rows?: unknown
  cellRules?: BatchRecordReportCellRuleVO[]
  signatureCellMarkers?: BatchRecordReportSignatureCellMarkerVO[]
}

const message = useMessage()
const loading = ref(false)
const resolution = ref<FormActionResolutionVO>()
const instanceId = ref<number>()
const instanceCode = ref('')
const instanceStatus = ref<FormInstanceStatus | ''>('')
const bpmProcessInstanceId = ref('')
const snapshots = ref<FormInstanceSnapshotVO[]>([])
const blockerCode = ref('')
const actionFormData = ref<Record<string, unknown>>({})
const actionPanelSheetLayoutJson = ref('')
const actionPanelCellRules = ref<BatchRecordReportCellRuleVO[]>([])
const actionPanelSignatureMarkers = ref<BatchRecordReportSignatureCellMarkerVO[]>([])
const actionPanelFieldIdentityMap = ref<Record<string, string>>({})
const actionPanelTemplateName = ref('')
const actionPanelTemplateVersionNo = ref('')
let actionFormLoadSerial = 0

const editableTemplateFormData = computed<TemplateSimulationValueMap>({
  get: () => actionFormData.value as TemplateSimulationValueMap,
  set: (value) => {
    actionFormData.value = { ...actionFormData.value, ...value }
  }
})

const normalizePositiveNumber = (value: unknown) => {
  const numberValue = Number(value)
  return Number.isInteger(numberValue) && numberValue > 0 ? numberValue : undefined
}

const normalizeNonBlankString = (value: unknown) => {
  const text = String(value || '').trim()
  return text || undefined
}

const normalizeEmbeddedRecognizedFields = (value: unknown): FormRecognizedFieldVO[] => {
  if (value === undefined || value === null) return []
  if (!Array.isArray(value)) {
    throw new Error('动态表单模板识别字段快照格式无效。')
  }
  return value.map((item) => {
    if (!item || typeof item !== 'object') {
      throw new Error('动态表单模板识别字段快照格式无效。')
    }
    const field = item as Record<string, unknown>
    const fieldCode = normalizeNonBlankString(field.fieldCode)
    const label = normalizeNonBlankString(field.label) || fieldCode
    const fieldType = normalizeNonBlankString(field.fieldType)
    if (!fieldCode || !fieldType) {
      throw new Error('动态表单模板识别字段快照缺少字段编码或类型。')
    }
    return {
      fieldCode,
      label,
      fieldType,
      required: Boolean(field.required)
    }
  })
}

const resolveEmbeddedTemplateVersionForActionForm = (): FormTemplateListItemVO | undefined => {
  const templateId = normalizePositiveNumber(props.formData.formTemplateId)
  const versionNo = normalizeNonBlankString(props.formData.formTemplateVersionNo)
  if (!templateId || !versionNo) return undefined
  const jimuSchemaJson = normalizeNonBlankString(props.formData.formTemplateJimuSchemaJson)
  const recognizedFields = normalizeEmbeddedRecognizedFields(
    props.formData.formTemplateRecognizedFields
  )
  if (!jimuSchemaJson && !recognizedFields.length) return undefined
  return {
    templateId,
    templateName: normalizeNonBlankString(props.formData.formTemplateName) || '',
    versionNo,
    status: 'PUBLISHED',
    updatedTime: '',
    jimuSchemaJson,
    recognizedFields
  }
}

const fieldValueType = (fieldType?: string): BatchRecordReportCellValueType => {
  const normalized = String(fieldType || '').toLowerCase()
  if (normalized === 'number') return 'NUMBER'
  if (normalized === 'date') return 'DATE'
  if (normalized === 'datetime') return 'DATETIME'
  if (normalized === 'checkbox') return 'BOOLEAN'
  if (normalized === 'signature') return 'SIGNATURE'
  return 'STRING'
}

const fieldComponentFlag = (fieldType?: string) => {
  const normalized = String(fieldType || '').toLowerCase()
  if (normalized === 'number') return 'input-number'
  if (normalized === 'date') return 'date'
  if (normalized === 'datetime') return 'datetime'
  if (normalized === 'checkbox') return 'checkbox'
  if (normalized === 'signature') return 'signature'
  if (normalized === 'textarea') return 'textarea'
  return 'input-text'
}

const buildRecognizedFieldCellRules = (fields: FormRecognizedFieldVO[]) =>
  fields.map((field, index) => {
    const rowIndex = Math.floor(index / 2) + 3
    const labelColumnIndex = index % 2 === 0 ? 0 : 2
    const inputColumnIndex = labelColumnIndex + 1
    return {
      rowIndex,
      columnIndex: inputColumnIndex,
      valueType: fieldValueType(field.fieldType),
      componentFlag: fieldComponentFlag(field.fieldType),
      required: field.required,
      label: field.label || field.fieldCode,
      placeholder: field.fieldType === 'checkbox' ? '□' : '',
      source: 'AUTO',
      reviewed: false
    } as BatchRecordReportCellRuleVO
  })

const buildRecognizedFieldIdentityMap = (fields: FormRecognizedFieldVO[]) => {
  const map: Record<string, string> = {}
  fields.forEach((field, index) => {
    const rowIndex = Math.floor(index / 2) + 3
    const columnIndex = index % 2 === 0 ? 1 : 3
    const fieldCode = String(field.fieldCode || '').trim()
    if (fieldCode) {
      map[buildTemplateFieldIdentity(rowIndex, columnIndex)] = fieldCode
    }
  })
  return map
}

const buildRecognizedFieldsSheetLayoutJson = (
  template: FormTemplateListItemVO,
  rules: BatchRecordReportCellRuleVO[]
) => {
  const rows: Record<string, { height: number; cells: Record<string, { text: string; merge?: number[] }> }> = {
    '0': {
      height: 28,
      cells: {
        '0': { text: template.templateName, merge: [0, 1] },
        '2': { text: '记录编号' },
        '3': { text: `TPL-${template.templateId}` }
      }
    },
    '1': {
      height: 28,
      cells: {
        '0': { text: '版本' },
        '1': { text: template.versionNo },
        '2': { text: '版本状态' },
        '3': { text: template.status || '' }
      }
    },
    '2': {
      height: 26,
      cells: {
        '0': { text: '识别字段', merge: [0, 3] }
      }
    }
  }
  rules.forEach((rule) => {
    const rowKey = String(rule.rowIndex)
    if (!rows[rowKey]) {
      rows[rowKey] = { height: 36, cells: {} }
    }
    const labelColumnIndex = Math.max(0, rule.columnIndex - 1)
    rows[rowKey].cells[String(labelColumnIndex)] = {
      text: `${rule.label || '字段'}${rule.required ? ' *' : ''}`
    }
    rows[rowKey].cells[String(rule.columnIndex)] = { text: '' }
  })
  return JSON.stringify({
    cols: {
      '0': { width: 140 },
      '1': { width: 220 },
      '2': { width: 140 },
      '3': { width: 220 }
    },
    rows
  })
}

const parseTemplateJimuSchema = (schema?: string): FormTemplateJimuSchemaPayload | undefined => {
  if (!schema?.trim()) return undefined
  const parsed = JSON.parse(schema) as FormTemplateJimuSchemaPayload
  const sheetLayoutJson =
    typeof parsed.sheetLayoutJson === 'string'
      ? parsed.sheetLayoutJson
      : parsed.sheetLayoutJson && typeof parsed.sheetLayoutJson === 'object'
        ? JSON.stringify(parsed.sheetLayoutJson)
        : typeof parsed.layout === 'string'
          ? parsed.layout
          : parsed.layout && typeof parsed.layout === 'object'
            ? JSON.stringify(parsed.layout)
            : parsed.rows && typeof parsed.rows === 'object'
              ? JSON.stringify(parsed)
              : undefined
  return {
    ...parsed,
    sheetLayoutJson,
    cellRules: Array.isArray(parsed.cellRules) ? parsed.cellRules : undefined,
    signatureCellMarkers: Array.isArray(parsed.signatureCellMarkers)
      ? parsed.signatureCellMarkers
      : undefined
  }
}

const resetActionPanelTemplate = () => {
  actionPanelSheetLayoutJson.value = ''
  actionPanelCellRules.value = []
  actionPanelSignatureMarkers.value = []
  actionPanelFieldIdentityMap.value = {}
  actionPanelTemplateName.value = ''
  actionPanelTemplateVersionNo.value = ''
}

const applyLatestDraftSnapshotFormData = () => {
  const latestDraftSnapshot = [...snapshots.value]
    .filter((snapshot) => snapshot.snapshotType === 'DRAFT')
    .sort((left, right) => Number(right.snapshotVersion || 0) - Number(left.snapshotVersion || 0))[0]
  if (!latestDraftSnapshot?.formData) return
  actionFormData.value = { ...actionFormData.value, ...latestDraftSnapshot.formData }
}

const loadTemplateVersionForActionForm = async (serial: number) => {
  const templateId = normalizePositiveNumber(props.formData.formTemplateId)
  const versionNo = normalizeNonBlankString(props.formData.formTemplateVersionNo)
  if (!templateId || !versionNo) {
    resetActionPanelTemplate()
    return
  }
  const embeddedTemplate = resolveEmbeddedTemplateVersionForActionForm()
  if (!embeddedTemplate) {
    resetActionPanelTemplate()
    throw new Error('动态表单运行态缺少 openTask 模板快照，无法渲染。')
  }
  const template = embeddedTemplate
  if (serial !== actionFormLoadSerial) return
  const parsedSchema = parseTemplateJimuSchema(template.jimuSchemaJson)
  const recognizedRules = buildRecognizedFieldCellRules(template.recognizedFields || [])
  actionPanelTemplateName.value = template.templateName || ''
  actionPanelTemplateVersionNo.value = template.versionNo || versionNo
  actionPanelCellRules.value = parsedSchema?.cellRules?.length ? parsedSchema.cellRules : recognizedRules
  actionPanelSignatureMarkers.value = parsedSchema?.signatureCellMarkers || []
  actionPanelFieldIdentityMap.value = buildRecognizedFieldIdentityMap(template.recognizedFields || [])
  const parsedSheetLayoutJson =
    typeof parsedSchema?.sheetLayoutJson === 'string' && parsedSchema.sheetLayoutJson.trim()
      ? parsedSchema.sheetLayoutJson
      : ''
  if (!parsedSheetLayoutJson && !actionPanelCellRules.value.length) {
    throw new Error('当前动态表单模板缺少布局和识别字段，无法渲染。')
  }
  actionPanelSheetLayoutJson.value =
    parsedSheetLayoutJson || buildRecognizedFieldsSheetLayoutJson(template, actionPanelCellRules.value)
}

const blockerTitle = computed(() => {
  if (blockerCode.value === 'FORM_POLICY_NOT_FOUND') return '未找到业务审批策略'
  if (blockerCode.value === 'BPM_BINDING_MISSING') return '审批流程未配置'
  return blockerCode.value
})

const actionPanelTemplateTitle = computed(() => {
  const name = actionPanelTemplateName.value || String(props.formData.formTemplateName || '').trim()
  const versionNo = actionPanelTemplateVersionNo.value || String(props.formData.formTemplateVersionNo || '').trim()
  if (name && versionNo) return `${name} / ${versionNo}`
  return name || versionNo || '动态表单'
})

const surfaceError = (error: unknown) => {
  const response = (error as any)?.response?.data
  const visibleMessage = resolveProjectionErrorMessage(error, '业务动作')
  blockerCode.value = response?.code || visibleMessage || 'FORM_ACTION_BLOCKED'
  message.error(visibleMessage)
}

const runVisibleAction = async (action: () => Promise<void>) => {
  loading.value = true
  blockerCode.value = ''
  try {
    await action()
  } catch (error) {
    surfaceError(error)
    throw error
  } finally {
    loading.value = false
  }
}

const loadActionFormState = async () => {
  const serial = ++actionFormLoadSerial
  loading.value = true
  blockerCode.value = ''
  actionFormData.value = { ...(props.formData || {}) }
  const nextInstanceId = normalizePositiveNumber(props.initialInstanceId)
  if (nextInstanceId) {
    instanceId.value = nextInstanceId
    instanceCode.value = String(props.initialInstanceCode || nextInstanceId)
    instanceStatus.value = props.initialInstanceStatus || 'DRAFT'
    bpmProcessInstanceId.value = String(props.initialBpmProcessInstanceId || '')
  } else {
    instanceId.value = undefined
    instanceCode.value = ''
    instanceStatus.value = ''
    bpmProcessInstanceId.value = ''
    snapshots.value = []
  }
  try {
    await loadTemplateVersionForActionForm(serial)
    if (serial !== actionFormLoadSerial) return
    await loadSnapshots()
  } catch (error) {
    if (serial === actionFormLoadSerial) {
      surfaceError(error)
    }
  } finally {
    if (serial === actionFormLoadSerial) {
      loading.value = false
    }
  }
}

watch(
  () => [
    props.context.actionCode,
    props.context.objectId,
    props.initialInstanceId,
    props.initialInstanceCode,
    props.initialInstanceStatus,
    props.initialBpmProcessInstanceId,
    props.formData.formTemplateId,
    props.formData.formTemplateVersionNo,
    props.formData.formCenterInstanceId
  ],
  () => {
    void loadActionFormState()
  },
  { immediate: true }
)

const buildSubmitPayload = (): SubmitFormInstanceReqVO => {
  const payload: SubmitFormInstanceReqVO = { formData: actionFormData.value }
  const selectedAssignees = actionFormData.value.startUserSelectAssignees
  if (selectedAssignees === undefined || selectedAssignees === null) {
    return payload
  }
  if (Array.isArray(selectedAssignees) || typeof selectedAssignees !== 'object') {
    throw new Error('startUserSelectAssignees 必须是对象')
  }
  const normalized: Record<string, number[]> = {}
  for (const [taskKey, assignees] of Object.entries(selectedAssignees)) {
    if (!taskKey || !Array.isArray(assignees)) {
      throw new Error('startUserSelectAssignees 的每个节点必须配置审批人数组')
    }
    const userIds = assignees.map((item) => Number(item)).filter((item) => Number.isInteger(item) && item > 0)
    if (userIds.length !== assignees.length) {
      throw new Error('startUserSelectAssignees 只能包含正整数用户ID')
    }
    normalized[taskKey] = userIds
  }
  payload.startUserSelectAssignees = normalized
  return payload
}

const statusLabel = (status: FormInstanceStatus | '') => {
  const labels: Record<FormInstanceStatus, string> = {
    DRAFT: '草稿',
    IN_APPROVAL: '审批中',
    REWORKING: '返工中',
    REJECTED: '已驳回',
    ABANDONED: '已放弃',
    PENDING_EFFECT: '待生效',
    EFFECTIVE: '已生效',
    EFFECT_FAILED_PENDING: '生效失败待处理'
  }
  return status ? labels[status] : '-'
}

const statusTagType = (status: FormInstanceStatus | '') => {
  if (status === 'EFFECTIVE') return 'success'
  if (status === 'EFFECT_FAILED_PENDING') return 'danger'
  if (status === 'PENDING_EFFECT' || status === 'IN_APPROVAL') return 'warning'
  if (status === 'REJECTED' || status === 'ABANDONED') return 'info'
  return 'primary'
}

const snapshotTypeLabel = (snapshotType: string) => {
  if (snapshotType === 'SUBMIT') return '提交快照'
  if (snapshotType === 'REWORK_SUBMIT') return '返工提交快照'
  return '草稿快照'
}

const stringifySnapshot = (formData: Record<string, unknown>) => {
  return JSON.stringify(formData || {})
}

async function loadSnapshots() {
  if (!instanceId.value) return
  snapshots.value = await getInstanceSnapshots(instanceId.value)
  applyLatestDraftSnapshotFormData()
}

const resolveAction = async () => {
  await runVisibleAction(async () => {
    resolution.value = await resolveBusinessAction(props.context)
    if (resolution.value.policyType === 'NONE') {
      message.success('已匹配业务审批策略，无需补充表单')
    }
  })
}

const createInstance = async () => {
  await runVisibleAction(async () => {
    const created = await createFormInstance({
      context: props.context,
      idempotencyKey: props.idempotencyKey,
      formData: actionFormData.value
    })
    instanceId.value = created.id
    instanceCode.value = created.instanceCode
    instanceStatus.value = created.status
    bpmProcessInstanceId.value = created.bpmProcessInstanceId || ''
    await loadSnapshots()
  })
}

const submitInstance = async () => {
  if (!instanceId.value) return
  await runVisibleAction(async () => {
    const submitted = await submitFormInstance(instanceId.value!, buildSubmitPayload())
    instanceStatus.value = submitted.status
    bpmProcessInstanceId.value = submitted.bpmProcessInstanceId || ''
    await loadSnapshots()
    message.success('已提交')
  })
}

const saveDraft = async () => {
  if (!instanceId.value) return
  await runVisibleAction(async () => {
    await saveFormDraft(instanceId.value!, { formData: actionFormData.value })
    instanceStatus.value = 'DRAFT'
    await loadSnapshots()
    message.success('草稿已保存')
  })
}

const reworkSubmit = async () => {
  if (!instanceId.value) return
  await runVisibleAction(async () => {
    await reworkSubmitFormInstance(instanceId.value!, buildSubmitPayload())
    instanceStatus.value = 'IN_APPROVAL'
    await loadSnapshots()
    message.success('已重提')
  })
}

const abandonInstance = async () => {
  if (!instanceId.value) return
  await runVisibleAction(async () => {
    await abandonFormInstance(instanceId.value!)
    instanceStatus.value = 'ABANDONED'
    message.success('已放弃')
  })
}
</script>

<style scoped>
.form-action-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-action-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.form-action-panel__editable-surface {
  display: flex;
  min-height: 360px;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px;
}

.form-action-panel__editable-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #334155;
}

.form-action-panel__editable-head strong {
  color: #111827;
}

.form-action-panel__editable-head span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-action-panel__snapshots {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  padding: 0 12px;
}

.form-action-panel__json-preview {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #4b5563;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
