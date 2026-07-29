<template>
  <section class="frontline-fixed-template-panel">
    <div class="frontline-fixed-template-panel__header">
      <div>
        <h3>固定模板录入</h3>
        <span>{{ selectedTemplate?.name || '未选择模板' }}</span>
      </div>
      <el-tag type="info" effect="plain">{{ selectedTemplate?.type || 'F3' }}</el-tag>
    </div>

    <el-form
      class="frontline-fixed-template-panel__form"
      :model="context"
      label-width="108px"
      @submit.prevent
    >
      <div class="frontline-fixed-template-panel__context-grid">
        <el-form-item label="模板">
          <el-select v-model="context.templateCode" class="frontline-fixed-template-panel__control">
            <el-option
              v-for="template in catalog"
              :key="template.code"
              :label="template.name"
              :value="template.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生产工单">
          <el-input-number
            v-model="context.workOrderId"
            class="frontline-fixed-template-panel__control"
            :controls="false"
          />
        </el-form-item>
        <el-form-item label="工艺路线">
          <el-input-number
            v-model="context.routeId"
            class="frontline-fixed-template-panel__control"
            :controls="false"
          />
        </el-form-item>
        <el-form-item label="工序">
          <el-input-number
            v-model="context.processId"
            class="frontline-fixed-template-panel__control"
            :controls="false"
          />
        </el-form-item>
        <el-form-item label="路线工序">
          <el-input-number
            v-model="context.routeProcessId"
            class="frontline-fixed-template-panel__control"
            :controls="false"
          />
        </el-form-item>
        <el-form-item label="实际员工">
          <el-input-number
            v-model="context.actualEmployeeId"
            class="frontline-fixed-template-panel__control"
            :controls="false"
          />
        </el-form-item>
      </div>

      <div
        v-if="context.templateCode === FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED"
        class="frontline-fixed-template-panel__field-grid"
      >
        <el-form-item
          v-for="field in productionFields"
          :key="field.code"
          :label="field.label"
        >
          <el-input
            v-if="field.input === 'text'"
            v-model="draft.fieldValues[field.code]"
            class="frontline-fixed-template-panel__control"
            clearable
          />
          <el-input
            v-else-if="field.input === 'json'"
            v-model="draft.fieldValues[field.code]"
            class="frontline-fixed-template-panel__control"
            type="textarea"
            :rows="2"
            clearable
          />
          <el-input-number
            v-else
            v-model="draft.fieldValues[field.code]"
            class="frontline-fixed-template-panel__control"
            :controls="false"
          />
        </el-form-item>
      </div>

      <div
        v-if="context.templateCode === FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED"
        class="frontline-fixed-template-panel__field-grid"
      >
        <el-form-item label="PQC 结果">
          <el-radio-group v-model="draft.fieldValues.PQC_RESULT">
            <el-radio-button
              v-for="option in pqcOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
      </div>

      <div class="frontline-fixed-template-panel__actions">
        <el-button :loading="resolveLoading" @click="handleResolve">
          <Icon icon="ep:connection" class="mr-5px" /> 解析模板
        </el-button>
        <el-button type="primary" :loading="payloadLoading" @click="handleValidate">
          <Icon icon="ep:finished" class="mr-5px" /> 校验 payload
        </el-button>
      </div>
    </el-form>

    <el-descriptions
      v-if="payloadPreview"
      class="frontline-fixed-template-panel__preview"
      :column="3"
      size="small"
      border
    >
      <el-descriptions-item label="模板">{{ payloadPreview.templateCode }}</el-descriptions-item>
      <el-descriptions-item label="工序">{{ payloadPreview.processId }}</el-descriptions-item>
      <el-descriptions-item label="员工">{{ payloadPreview.actualEmployeeId }}</el-descriptions-item>
    </el-descriptions>
  </section>
</template>

<script setup lang="ts">
import {
  FRONTLINE_TEMPLATE_CODES,
  FRONTLINE_PQC_RESULTS,
  FrontlineTemplateApi,
  type FrontlineTemplateDefinitionVO,
  type FrontlineTemplatePayloadVO
} from '@/api/mes/pro/feedbackFrontlineTemplate'
import {
  buildFrontlineTemplatePayload,
  createFrontlineDefaultValues,
  resetFrontlineTemplateDraftForContext,
  resolveFrontlineContextKey,
  type FrontlineTemplateContext,
  type FrontlineTemplateDraft
} from './frontlineTemplate'

const message = useMessage()

const catalog = ref<FrontlineTemplateDefinitionVO[]>([])
const resolveLoading = ref(false)
const payloadLoading = ref(false)
const payloadPreview = ref<FrontlineTemplatePayloadVO>()

const context = reactive<FrontlineTemplateContext>({
  templateCode: FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED
})
const draft = reactive<FrontlineTemplateDraft>({
  fieldValues: createFrontlineDefaultValues(context.templateCode)
})

const productionFields = [
  { code: 'PREVIOUS_PROCESS_INPUT_QUANTITY', label: '上工序输入数量', input: 'number' },
  { code: 'DEVICE', label: '设备', input: 'text' },
  { code: 'DEVICE_PARAMETERS', label: '设备参数', input: 'json' },
  { code: 'OUTPUT_QUANTITY', label: '输出数量', input: 'number' },
  { code: 'SCRAP_QUANTITY', label: '损耗数量', input: 'number' }
] as const

const pqcOptions = [
  { value: 'DETECTION_SUCCESS', label: '检测成功' },
  { value: 'DETECTION_FAILED', label: '检测失败' }
] as const

const selectedTemplate = computed(() =>
  catalog.value.find((template) => template.code === context.templateCode)
)

const frontlineContextKey = computed(() => resolveFrontlineContextKey(context))

watch(
  frontlineContextKey,
  (nextKey, previousKey) => {
    const changed = resetFrontlineTemplateDraftForContext(previousKey, nextKey, draft)
    if (changed) {
      Object.assign(draft.fieldValues, createFrontlineDefaultValues(context.templateCode))
      payloadPreview.value = undefined
    }
  },
  { flush: 'sync' }
)

const handleResolve = async () => {
  resolveLoading.value = true
  try {
    await FrontlineTemplateApi.resolveTemplate({
      actualEmployeeId: context.actualEmployeeId,
      routeProcessId: context.routeProcessId,
      processId: context.processId,
      templateCode: context.templateCode
    })
    message.success('模板已解析')
  } catch (error) {
    message.error(resolveErrorMessage(error))
    throw error
  } finally {
    resolveLoading.value = false
  }
}

const handleValidate = async () => {
  payloadLoading.value = true
  try {
    payloadPreview.value = await FrontlineTemplateApi.validatePayload(
      buildFrontlineTemplatePayload(context, draft.fieldValues)
    )
    message.success('payload 已校验')
  } catch (error) {
    message.error(resolveErrorMessage(error))
    throw error
  } finally {
    payloadLoading.value = false
  }
}

const resolveErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return '固定模板校验失败'
}

onMounted(async () => {
  catalog.value = await FrontlineTemplateApi.getCatalog()
})
</script>

<style scoped lang="scss">
.frontline-fixed-template-panel {
  margin-bottom: 12px;
  padding: 14px 16px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.frontline-fixed-template-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;

  h3 {
    margin: 0;
    color: var(--el-text-color-primary);
    font-size: 16px;
    font-weight: 600;
    line-height: 24px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 18px;
  }
}

.frontline-fixed-template-panel__context-grid,
.frontline-fixed-template-panel__field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr));
  gap: 0 12px;
}

.frontline-fixed-template-panel__control {
  width: 100%;
}

.frontline-fixed-template-panel__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.frontline-fixed-template-panel__preview {
  margin-top: 12px;
}

@media (max-width: 768px) {
  .frontline-fixed-template-panel__context-grid,
  .frontline-fixed-template-panel__field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
