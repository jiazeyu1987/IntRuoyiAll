<template>
  <ContentWrap>
    <div v-loading="loading" class="edhr-batch-template-simulate">
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <section v-else-if="currentTask && templateData" class="edhr-batch-template-simulate__panel">
        <div class="edhr-batch-template-simulate__header">
          <div>
            <el-button link type="primary" @click="handleBack">
              {{ backButtonLabel }}
            </el-button>
            <div class="edhr-batch-template-simulate__title">
              {{ currentTask.routeProcessSort || '--' }}.
              {{ currentTask.processCode || '--' }}
              {{ currentTask.processName || '--' }}
            </div>
            <div class="edhr-batch-template-simulate__subtitle">
              {{ currentTask.batchRecordReportName || currentTask.batchRecordReportId || '--' }}
            </div>
          </div>
          <el-tag type="primary">模拟填写</el-tag>
        </div>

        <el-descriptions :column="6" border>
          <el-descriptions-item label="模板ID">
            {{ currentTask.batchRecordReportId || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="表格顺序">
            {{ currentTask.batchRecordSort || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="填写字段">
            {{ summary.fillableCount }}
          </el-descriptions-item>
          <el-descriptions-item label="签名位">
            {{ summary.signatureCount }}
          </el-descriptions-item>
          <el-descriptions-item label="必填">
            {{ summary.requiredCount }}
          </el-descriptions-item>
          <el-descriptions-item label="附件规则">
            {{ summary.attachmentRuleCount }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="edhr-batch-template-simulate__workbench">
          <section class="edhr-batch-template-simulate__column">
            <div class="edhr-batch-template-simulate__surface">
              <div class="edhr-batch-template-simulate__surface-head">
                <div class="edhr-batch-template-simulate__section-title">模板内填写</div>
                <div class="edhr-batch-template-simulate__surface-note">左侧直接在原模板格内模拟填写。</div>
              </div>
              <div class="edhr-batch-template-simulate__surface-body fit-to-viewport width-only">
                <EdhrExecutionTemplateEditableForm
                  v-model="simulationValues"
                  :sheet-layout-json="templateData.sheetLayoutJson"
                  :cell-rules="templateData.rules"
                  :signature-markers="templateData.markers"
                  fit-to-viewport
                  @signature-action="handleSignatureAction"
                />
              </div>
            </div>
          </section>

          <section class="edhr-batch-template-simulate__column">
            <div class="edhr-batch-template-simulate__surface">
              <div class="edhr-batch-template-simulate__surface-head">
                <div class="edhr-batch-template-simulate__section-title">表单显示</div>
                <div class="edhr-batch-template-simulate__surface-note">未签名将保持原模板签名位占位。</div>
              </div>
              <div class="edhr-batch-template-simulate__surface-body fit-to-viewport width-only">
                <EdhrExecutionReadonlyForm
                  :form-view-model="previewFormViewModel"
                  :signature-records="signatureRecords"
                  fit-to-viewport
                />
              </div>
            </div>
          </section>
        </div>
      </section>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  type BatchRecordReportCellRuleVO,
  BatchRecordReportApi,
  type BatchRecordReportSignatureCellMarkerVO
} from '@/api/mes/pro/batchrecordreport'
import {
  getEdhrBatchExecution,
  type EdhrBatchExecutionRespVO,
  type EdhrBatchExecutionReviewFormViewModel,
  type EdhrBatchExecutionReviewSignatureRecord,
  type EdhrBatchExecutionTaskRespVO
} from '@/api/mes/pro/edhr/batchExecution'
import {
  buildTemplateFieldIdentity,
  buildTemplateSimulationFields,
  cleanedAttachmentRule,
  normalizeCellRule,
  type TemplateSimulationField,
  type TemplateSimulationValueMap
} from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'
import type { TemplateEditableCellContext } from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'
import EdhrExecutionReadonlyForm from '@/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'
import EdhrExecutionTemplateEditableForm from '@/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue'
import { parsePositiveRouteQueryId, sameRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'MesProEdhrBatchExecutionTemplateSimulate' })

type TemplateData = {
  sheetLayoutJson: string
  rules: BatchRecordReportCellRuleVO[]
  markers: BatchRecordReportSignatureCellMarkerVO[]
}

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const detail = ref<EdhrBatchExecutionRespVO>()
const templateData = ref<TemplateData>()
const simulationValues = ref<TemplateSimulationValueMap>({})

const batchExecutionId = computed(() => parsePositiveRouteQueryId(route.query.id))
const taskId = computed(() => parsePositiveRouteQueryId(route.query.taskId))
const directReportId = computed(() => String(route.query.reportId || '').trim())
const directReportName = computed(() => String(route.query.reportName || '').trim())
const directBatchRecordName = computed(() => String(route.query.batchRecordName || '').trim())
const returnTo = computed(() => String(route.query.returnTo || '').trim())
const returnLabel = computed(() => String(route.query.returnLabel || '').trim())
const backButtonLabel = computed(() => returnLabel.value || '返回')

const assertPositiveId = (value: string, label: string) => {
  if (!value) {
    throw new Error(`缺少有效${label}，无法进入模板模拟填写。`)
  }
  return value
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const selectedTask = computed(() =>
  (detail.value?.tasks || []).find((task) => sameRouteQueryId(task.id, taskId.value))
)

const currentTask = computed<EdhrBatchExecutionTaskRespVO | undefined>(() => {
  if (selectedTask.value) return selectedTask.value
  if (!directReportId.value) return undefined
  return {
    id: 0,
    routeProcessSort: 0,
    processCode: 'TMP',
    processName: directBatchRecordName.value || '电子批记录模板',
    batchRecordReportId: directReportId.value,
    batchRecordReportName: directReportName.value || directReportId.value,
    batchRecordSort: 0,
    recordCategory: 'TEMPLATE'
  } as unknown as EdhrBatchExecutionTaskRespVO
})

const simulationFields = computed<TemplateSimulationField[]>(() => {
  if (!templateData.value) return []
  return buildTemplateSimulationFields(templateData.value.rules, templateData.value.markers || [])
})

const summary = computed(() => {
  const fillableCount = simulationFields.value.length
  const signatureCount = simulationFields.value.filter((field) => field.componentKind === 'signature').length
  const requiredCount = simulationFields.value.filter((field) => field.required).length
  const attachmentRuleCount = simulationFields.value.filter((field) => Boolean(field.attachmentRule)).length
  return { fillableCount, signatureCount, requiredCount, attachmentRuleCount }
})

const previewCellValues = computed(() => {
  return simulationFields.value
    .filter((field) => field.componentKind !== 'attachment')
    .filter((field) => field.componentKind !== 'signature')
    .map((field) => {
      const rawValue = simulationValues.value[field.fieldIdentity]
      if (field.componentKind === 'checkbox') {
        return {
          rowIndex: field.rowIndex,
          columnIndex: field.columnIndex,
          valueType: field.valueType,
          value: rawValue === true,
          valueDisplay: rawValue === true ? 'true' : 'false'
        }
      }
      return {
        rowIndex: field.rowIndex,
        columnIndex: field.columnIndex,
        valueType: field.valueType,
        value: rawValue ?? '',
        valueDisplay: rawValue == null ? '' : String(rawValue),
        unit: field.unit || undefined
      }
    })
})

const previewSnapshotJson = computed(() =>
  JSON.stringify({
    fields: simulationFields.value
      .filter((field) => Boolean(cleanedAttachmentRule(field.attachmentRule)))
      .map((field) => ({
        rowIndex: field.rowIndex,
        columnIndex: field.columnIndex,
        attachmentRule: field.attachmentRule
      }))
  })
)

const previewFormViewModel = computed<EdhrBatchExecutionReviewFormViewModel>(() => ({
  sheetLayoutJson: templateData.value?.sheetLayoutJson || '',
  executionSnapshotJson: previewSnapshotJson.value,
  cellValuesJson: JSON.stringify(previewCellValues.value),
  remark: '',
  signatureCellMarkers: templateData.value?.markers || []
}))

const signatureRecords = computed<EdhrBatchExecutionReviewSignatureRecord[]>(() => {
  return []
})

const message = useMessage()

const handleSignatureAction = (context: TemplateEditableCellContext) => {
  const actionLabel = context.signatureLabel || '电子签名'
  message.warning(`${actionLabel}需在真实 eDHR 执行页通过密码电子签名完成。`)
}

const handleBack = async () => {
  if (returnTo.value) {
    await router.push(returnTo.value)
    return
  }
  await router.back()
}

const resetSimulationValues = () => {
  const nextValues: TemplateSimulationValueMap = {}
  simulationFields.value.forEach((field) => {
    const fieldIdentity = buildTemplateFieldIdentity(field.rowIndex, field.columnIndex)
    if (field.componentKind === 'checkbox') {
      nextValues[fieldIdentity] = false
      return
    }
    if (field.componentKind === 'number') {
      nextValues[fieldIdentity] = null
      return
    }
    if (field.componentKind === 'signature') return
    if (field.componentKind === 'attachment') {
      nextValues[fieldIdentity] = ''
      return
    }
    nextValues[fieldIdentity] = ''
  })
  simulationValues.value = nextValues
}

const loadTemplate = async (task: EdhrBatchExecutionTaskRespVO) => {
  const reportId = String(task.batchRecordReportId || '').trim()
  if (!reportId) {
    throw new Error('当前任务缺少模板ID，无法进入模拟填写。')
  }
  const [cellRuleResp, markerResp] = await Promise.all([
    BatchRecordReportApi.getCellRules(reportId),
    BatchRecordReportApi.getSignatureCellMarkers(reportId)
  ])
  const rawRules = cellRuleResp.suggestions?.length ? cellRuleResp.suggestions : cellRuleResp.rules || []
  const rules = rawRules
    .map(normalizeCellRule)
    .sort((left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex)
  const sheetLayoutJson = cellRuleResp.sheetLayoutJson || markerResp.sheetLayoutJson || ''
  if (!sheetLayoutJson.trim()) {
    throw new Error('缺少电子批记录模板布局，无法进入模板模拟填写。')
  }
  if (!rules.length) {
    throw new Error('模板缺少单元格规则，无法进入模板模拟填写。')
  }
  templateData.value = {
    sheetLayoutJson,
    rules,
    markers: markerResp.markers || []
  }
}

const loadPage = async () => {
  loading.value = true
  loadError.value = ''
  detail.value = undefined
  templateData.value = undefined
  simulationValues.value = {}
  try {
    if (directReportId.value) {
      await loadTemplate({
        id: 0,
        routeProcessSort: 0,
        processCode: 'TMP',
        processName: directBatchRecordName.value || '电子批记录模板',
        batchRecordReportId: directReportId.value,
        batchRecordReportName: directReportName.value || directReportId.value,
        batchRecordSort: 0,
        recordCategory: 'TEMPLATE'
      } as unknown as EdhrBatchExecutionTaskRespVO)
      resetSimulationValues()
      return
    }

    const currentBatchExecutionId = assertPositiveId(batchExecutionId.value, '批次执行ID')
    const currentTaskId = assertPositiveId(taskId.value, '任务ID')
    detail.value = await getEdhrBatchExecution(currentBatchExecutionId)
    const task = (detail.value?.tasks || []).find((item) => sameRouteQueryId(item.id, currentTaskId))
    if (!task) {
      throw new Error('当前批次中找不到对应模板任务，无法进入模拟填写。')
    }
    if (!task.batchRecordReportId) {
      throw new Error('当前任务缺少模板ID，无法进入模拟填写。')
    }
    await loadTemplate(task)
    resetSimulationValues()
  } catch (error) {
    detail.value = undefined
    templateData.value = undefined
    loadError.value = resolveErrorMessage(error, '模拟填写加载失败。')
  } finally {
    loading.value = false
  }
}

onMounted(loadPage)
</script>

<style scoped>
.edhr-batch-template-simulate {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-batch-template-simulate__panel {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 16px;
}

.edhr-batch-template-simulate__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.edhr-batch-template-simulate__title {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.edhr-batch-template-simulate__subtitle {
  color: #4b5563;
  font-size: 13px;
  margin-top: 4px;
}

.edhr-batch-template-simulate__workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.edhr-batch-template-simulate__column {
  min-width: 0;
}

.edhr-batch-template-simulate__surface {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
}

.edhr-batch-template-simulate__surface-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 42px;
}

.edhr-batch-template-simulate__surface-body {
  display: flex;
  min-width: 0;
  width: 100%;
  height: auto;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f7f9fc;
  padding: 12px;
  overflow: visible;
}

.edhr-batch-template-simulate__section-title {
  color: #172033;
  font-weight: 600;
}

.edhr-batch-template-simulate__surface-note {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1080px) {
  .edhr-batch-template-simulate__workbench {
    grid-template-columns: 1fr;
  }
}
</style>
