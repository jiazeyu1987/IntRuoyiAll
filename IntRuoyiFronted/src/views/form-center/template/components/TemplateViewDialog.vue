<template>
  <Dialog v-model="dialogVisible" title="查看表单模板" width="80%">
    <el-descriptions v-if="templateDetail" :column="2" border size="small">
      <el-descriptions-item label="模板名称" :span="2">
        {{ templateDetail.templateName }}
      </el-descriptions-item>
      <el-descriptions-item label="当前生效版本">
        <span
          v-if="isCurrentEffectiveVersion(templateDetail.status)"
          class="template-view-version-current"
        >
          {{ templateDetail.versionNo }}
        </span>
        <span v-else class="template-view-empty">无</span>
      </el-descriptions-item>
      <el-descriptions-item label="待发布版本">
        <el-tag
          v-if="isPendingTemplateVersion(templateDetail.status)"
          :type="pendingVersionTagType(templateDetail.status)"
          effect="plain"
        >
          {{ templateDetail.versionNo }} {{ statusLabel(templateDetail.status) }}
        </el-tag>
        <span v-else class="template-view-empty">无</span>
      </el-descriptions-item>
      <el-descriptions-item label="版本状态">
        <el-tag :type="statusTagType(templateDetail.status)">
          {{ statusLabel(templateDetail.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="修改时间">
        {{ formatTemplateUpdatedTime(templateDetail.updatedTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">
        {{ templateDetail.remark || '无' }}
      </el-descriptions-item>
    </el-descriptions>
    <div v-if="formViewModel" class="template-view-preview">
      <EdhrExecutionReadonlyForm
        :form-view-model="formViewModel"
        :signature-records="[]"
        fit-to-viewport
        fit-mode="width"
        embedded
      />
    </div>
    <el-empty v-else class="template-view-empty-state" description="当前模板暂无识别字段" />
    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import type {
  FormRecognizedFieldVO,
  FormTemplateListItemVO,
  FormTemplateStatus
} from '@/api/form-center/template'
import type { EdhrBatchExecutionReviewFormViewModel } from '@/api/mes/pro/edhr/batchExecution'
import EdhrExecutionReadonlyForm from '@/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'FormCenterTemplateViewDialog' })

const dialogVisible = ref(false)
const templateDetail = ref<FormTemplateListItemVO>()
const formViewModel = computed(() => buildFormViewModel(templateDetail.value))

const open = (row: FormTemplateListItemVO) => {
  templateDetail.value = { ...row }
  dialogVisible.value = true
}

defineExpose({ open })

const statusLabel = (status: FormTemplateStatus) => {
  const labels: Record<FormTemplateStatus, string> = {
    DRAFT: '草稿',
    PENDING_APPROVAL: '审批中',
    REJECTED: '已驳回',
    READY: '待发布',
    PUBLISHED: '已发布',
    DISABLED: '已停用',
    OBSOLETE: '已作废'
  }
  return labels[status]
}

const statusTagType = (status: FormTemplateStatus) => {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'DISABLED' || status === 'OBSOLETE') return 'info'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

const isCurrentEffectiveVersion = (status: FormTemplateStatus) => status === 'PUBLISHED'

const isPendingTemplateVersion = (status: FormTemplateStatus) => {
  return ['DRAFT', 'READY', 'PENDING_APPROVAL', 'REJECTED'].includes(status)
}

const pendingVersionTagType = (status: FormTemplateStatus) => {
  if (status === 'DRAFT' || status === 'READY') return 'info'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

const formatTemplateUpdatedTime = (value?: string | number | Date) => {
  if (!value) return '-'
  const normalizedValue = typeof value === 'string' && /^\d+$/.test(value) ? Number(value) : value
  return formatDate(normalizedValue as Date, 'YYYY-MM-DD HH:mm:ss')
}

type TemplateViewCellRule = {
  rowIndex: number
  columnIndex: number
  valueType: string
  componentFlag: string
  required: boolean
  label: string
  placeholder?: string
  source?: string
  reviewed?: boolean
}

type TemplateViewJimuSchema = {
  sheetLayoutJson?: string
  cellRules?: TemplateViewCellRule[]
  signatureCellMarkers?: EdhrBatchExecutionReviewFormViewModel['signatureCellMarkers']
}

const fieldValueType = (fieldType?: string) => {
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

const buildRules = (fields: FormRecognizedFieldVO[]): TemplateViewCellRule[] =>
  fields.map((field, index) => {
    const rowIndex = Math.floor(index / 2) + 3
    const labelColumnIndex = index % 2 === 0 ? 0 : 2
    return {
      rowIndex,
      columnIndex: labelColumnIndex + 1,
      valueType: fieldValueType(field.fieldType),
      componentFlag: fieldComponentFlag(field.fieldType),
      required: field.required,
      label: field.label || field.fieldCode,
      placeholder: field.fieldType === 'checkbox' ? '□' : '?',
      source: 'AUTO',
      reviewed: false
    }
  })

const parseJimuSchema = (schema?: string): TemplateViewJimuSchema | undefined =>
  schema?.trim() ? JSON.parse(schema) as TemplateViewJimuSchema : undefined

const buildFormViewModel = (
  template: FormTemplateListItemVO | undefined
): EdhrBatchExecutionReviewFormViewModel | undefined => {
  if (!template) return undefined
  const parsedSchema = parseJimuSchema(template.jimuSchemaJson)
  if (parsedSchema?.sheetLayoutJson?.trim()) {
    return {
      sheetLayoutJson: parsedSchema.sheetLayoutJson,
      executionSnapshotJson: JSON.stringify({ fields: [] }),
      cellValuesJson: JSON.stringify([]),
      remark: template.remark || '',
      signatureCellMarkers: parsedSchema.signatureCellMarkers || []
    }
  }
  const rules = parsedSchema?.cellRules?.length ? parsedSchema.cellRules : buildRules(template.recognizedFields || [])
  if (!rules.length) return undefined
  const rows: Record<string, { height?: number; cells: Record<string, Record<string, unknown>> }> = {
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
        '3': { text: statusLabel(template.status) }
      }
    },
    '2': { height: 26, cells: { '0': { text: '识别字段', merge: [0, 3] } } }
  }
  rules.forEach((rule) => {
    const rowKey = String(rule.rowIndex)
    if (!rows[rowKey]) rows[rowKey] = { height: 36, cells: {} }
    rows[rowKey].cells[String(Math.max(0, rule.columnIndex - 1))] = {
      text: `${rule.label || '字段'}${rule.required ? ' *' : ''}`
    }
    rows[rowKey].cells[String(rule.columnIndex)] = {
      fillForm: rule,
      edhrCellRule: rule
    }
  })
  return {
    sheetLayoutJson: JSON.stringify({
      cols: {
        '0': { width: 140 },
        '1': { width: 220 },
        '2': { width: 140 },
        '3': { width: 220 }
      },
      rows
    }),
    executionSnapshotJson: JSON.stringify({ fields: [] }),
    cellValuesJson: JSON.stringify([]),
    remark: template.remark || '',
    signatureCellMarkers: parsedSchema?.signatureCellMarkers || []
  }
}
</script>

<style scoped>
.template-view-version-current {
  color: #009688;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.template-view-empty {
  color: #8c8c8c;
}

.template-view-preview {
  margin-top: 12px;
  max-height: 60vh;
  overflow: auto;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  padding: 10px;
}

.template-view-empty-state {
  margin-top: 12px;
}
</style>
