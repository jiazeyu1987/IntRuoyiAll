<template>
  <ContentWrap>
    <div class="process-pool-write">
      <div class="process-pool-write__header">
        <div>
          <div class="process-pool-write__title">审核副本处理</div>
          <div class="process-pool-write__subtitle"
            >按正式上下限生成修正版副本，原始记录保持不变。</div
          >
        </div>
      </div>

      <el-alert v-if="submitError" :title="submitError" type="error" :closable="false" show-icon />
      <el-alert
        v-if="resultReviewCopyId"
        :title="`审核副本编号：${resultReviewCopyId}`"
        type="success"
        :closable="false"
        show-icon
      />

      <el-form
        class="process-pool-write__form"
        :model="reviewForm"
        label-width="160px"
        @submit.prevent
      >
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="工序提交记录编号">
              <el-input-number
                v-model="reviewForm.eventId"
                :min="1"
                :controls="false"
                class="process-pool-write__number"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="审核人编号">
              <el-input-number
                v-model="reviewForm.reviewerUserId"
                :min="1"
                :controls="false"
                class="process-pool-write__number"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="审核签名编号">
              <el-input-number
                v-model="reviewForm.reviewerSignatureId"
                :min="1"
                :controls="false"
                class="process-pool-write__number"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="签名员工编号">
              <el-input-number
                v-model="reviewForm.reviewerSignatureUserId"
                :min="1"
                :controls="false"
                class="process-pool-write__number"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="正式审核签名资料">
          <BusinessJsonInput
            ref="signatureInput"
            v-model="reviewForm.reviewerSignatureSnapshotJson"
            label="正式审核签名资料"
            file-only
            hint="请选择签名系统导出的正式签名资料；审核人、签名编号与签名员工须与该记录一致。"
          />
        </el-form-item>
        <el-form-item label="字段上下限">
          <div style="width: 100%; overflow: auto">
            <el-table
              :data="fieldMappings"
              border
              style="min-width: 900px"
              empty-text="请添加需要按正式上下限审核的字段"
            >
              <el-table-column label="字段名称" min-width="140"
                ><template #default="{ row }"
                  ><el-input v-model="row.fieldName" aria-label="字段名称" /></template
              ></el-table-column>
              <el-table-column label="字段标识" min-width="140"
                ><template #default="{ row }"
                  ><el-input v-model="row.fieldCode" aria-label="字段标识" /></template
              ></el-table-column>
              <el-table-column label="下限" min-width="110"
                ><template #default="{ row }"
                  ><el-input v-model="row.lowerLimit" aria-label="下限" /></template
              ></el-table-column>
              <el-table-column label="上限" min-width="110"
                ><template #default="{ row }"
                  ><el-input v-model="row.upperLimit" aria-label="上限" /></template
              ></el-table-column>
              <el-table-column label="值类型" min-width="120"
                ><template #default="{ row }"
                  ><el-input
                    v-model="row.valueType"
                    placeholder="按模板类型填写"
                    aria-label="值类型" /></template
              ></el-table-column>
              <el-table-column label="影响分配" width="100"
                ><template #default="{ row }"
                  ><el-switch v-model="row.affectsAllocation" aria-label="影响分配" /></template
              ></el-table-column>
              <el-table-column label="分配字段" min-width="140"
                ><template #default="{ row }"
                  ><el-select
                    v-model="row.allocationField"
                    :disabled="!row.affectsAllocation"
                    aria-label="分配字段"
                    ><el-option
                      v-for="item in allocationFields"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value" /></el-select></template
              ></el-table-column>
              <el-table-column label="数量片段编号" min-width="160"
                ><template #default="{ row }"
                  ><el-input-number
                    v-model="row.sourceQuantityFragmentId"
                    :min="1"
                    :controls="false"
                    :disabled="!row.affectsAllocation"
                    aria-label="数量片段编号" /></template
              ></el-table-column>
              <el-table-column label="模板字段元数据" min-width="240"
                ><template #default="{ row }"
                  ><BusinessJsonInput
                    :ref="(input) => setMetadataInput(row, input)"
                    v-model="row.templateFieldMetadataJson"
                    label="模板字段资料" /></template
              ></el-table-column>
              <el-table-column label="操作" width="70"
                ><template #default="{ $index }"
                  ><el-button link type="danger" @click="fieldMappings.splice($index, 1)"
                    >删除</el-button
                  ></template
                ></el-table-column
              >
            </el-table>
            <el-button class="mt-8px" @click="addFieldMapping">添加审核字段</el-button>
          </div>
        </el-form-item>

        <div class="process-pool-write__actions">
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            <Icon icon="ep:check" class="mr-5px" />
            生成并提交审核副本
          </el-button>
        </div>
      </el-form>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import BusinessJsonInput from '@/components/BusinessData/BusinessJsonInput.vue'
import { ElMessage } from 'element-plus'
import {
  generateSubmitProcessPoolReviewCopy,
  type ProcessPoolReviewCopyFieldMappingVO,
  type ProcessPoolReviewCopyGenerateSubmitReqVO
} from '@/api/mes/pro/processpool/reviewCopy'

defineOptions({ name: 'MesProProcessPoolReviewCopy' })

const reviewForm = reactive({
  eventId: undefined as number | undefined,
  reviewerUserId: undefined as number | undefined,
  reviewerSignatureId: undefined as number | undefined,
  reviewerSignatureUserId: undefined as number | undefined,
  reviewerSignatureSnapshotJson: ''
})

const signatureInput = ref<InstanceType<typeof BusinessJsonInput>>()
const metadataInputs = new Map<
  ProcessPoolReviewCopyFieldMappingVO,
  InstanceType<typeof BusinessJsonInput>
>()
const setMetadataInput = (row: ProcessPoolReviewCopyFieldMappingVO, input: unknown) => {
  if (input) metadataInputs.set(row, input as InstanceType<typeof BusinessJsonInput>)
  else metadataInputs.delete(row)
}
const fieldMappings = ref<ProcessPoolReviewCopyFieldMappingVO[]>([])
const allocationFields = [
  { label: '输出数量', value: 'OUTPUT_QUANTITY' },
  { label: '质量状态', value: 'QUALITY_STATUS' },
  { label: '可分配状态', value: 'ALLOCATABLE_STATUS' },
  { label: '备注', value: 'REMARK' }
]
const addFieldMapping = () =>
  fieldMappings.value.push({
    fieldCode: '',
    fieldName: '',
    lowerLimit: '',
    upperLimit: '',
    valueType: '',
    affectsAllocation: false
  })

const submitLoading = ref(false)
const submitError = ref('')
const resultReviewCopyId = ref<number>()

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage =
    (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

function parseJsonField<T>(value: string, label: string): T {
  if (!value || !value.trim()) {
    throw new Error(`${label}不能为空`)
  }
  try {
    return JSON.parse(value) as T
  } catch (error) {
    throw new Error(`${label}格式错误，请重新导入正式资料`)
  }
}

const requirePositiveNumber = (value: number | undefined, label: string) => {
  const parsed = Number(value)
  if (!Number.isSafeInteger(parsed) || parsed <= 0) {
    throw new Error(`${label}必须大于 0`)
  }
  return parsed
}

const buildRequestPayload = (): ProcessPoolReviewCopyGenerateSubmitReqVO => {
  const validationError =
    signatureInput.value?.validate() ||
    [...metadataInputs.values()].map((input) => input.validate(false)).find(Boolean)
  if (validationError) throw new Error(validationError)
  parseJsonField<Record<string, unknown>>(
    reviewForm.reviewerSignatureSnapshotJson,
    '正式审核签名资料'
  )
  if (!fieldMappings.value.length) throw new Error('请添加至少一个审核字段')
  for (const [index, item] of fieldMappings.value.entries()) {
    if (!item.fieldCode.trim() || !item.fieldName.trim())
      throw new Error(`第 ${index + 1} 项：请填写字段名称和标识`)
    if (
      String(item.lowerLimit).trim() === '' ||
      String(item.upperLimit).trim() === '' ||
      !Number.isFinite(Number(item.lowerLimit)) ||
      !Number.isFinite(Number(item.upperLimit)) ||
      Number(item.lowerLimit) > Number(item.upperLimit)
    )
      throw new Error(`第 ${index + 1} 项：请填写有效上下限，且下限不能大于上限`)
    if (item.affectsAllocation) {
      requirePositiveNumber(item.sourceQuantityFragmentId, `第 ${index + 1} 项数量片段编号`)
      if (!item.allocationField) throw new Error(`第 ${index + 1} 项：请选择分配字段`)
    }
  }

  return {
    eventId: requirePositiveNumber(reviewForm.eventId, '工序提交记录编号'),
    reviewerUserId: requirePositiveNumber(reviewForm.reviewerUserId, '审核人编号'),
    reviewerSignatureId: requirePositiveNumber(reviewForm.reviewerSignatureId, '审核签名编号'),
    reviewerSignatureUserId: requirePositiveNumber(
      reviewForm.reviewerSignatureUserId,
      '签名员工编号'
    ),
    reviewerSignatureSnapshot: reviewForm.reviewerSignatureSnapshotJson,
    fieldMappings: fieldMappings.value.map((item) => ({ ...item }))
  }
}

const handleSubmit = async () => {
  submitLoading.value = true
  submitError.value = ''
  resultReviewCopyId.value = undefined
  try {
    resultReviewCopyId.value = await generateSubmitProcessPoolReviewCopy(buildRequestPayload())
    ElMessage.success('审核副本已生成并提交')
  } catch (error) {
    submitError.value = resolveErrorMessage(error, '审核副本提交失败')
    ElMessage.error(submitError.value)
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.process-pool-write {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.process-pool-write__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.process-pool-write__title {
  font-size: 18px;
  font-weight: 600;
  line-height: 28px;
  color: var(--el-text-color-primary);
}

.process-pool-write__subtitle {
  margin-top: 4px;
  font-size: 13px;
  line-height: 20px;
  color: var(--el-text-color-secondary);
}

.process-pool-write__form {
  max-width: 1100px;
}

.process-pool-write__number {
  width: 100%;
}

.process-pool-write__actions {
  display: flex;
  justify-content: flex-end;
}
</style>
