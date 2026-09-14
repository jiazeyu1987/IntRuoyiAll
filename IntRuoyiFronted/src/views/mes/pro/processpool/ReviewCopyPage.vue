<template>
  <ContentWrap>
    <div class="process-pool-write">
      <div class="process-pool-write__header">
        <div>
          <div class="process-pool-write__title">审核副本处理</div>
          <div class="process-pool-write__subtitle">按正式上下限生成修正版副本，原始记录保持不变。</div>
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

      <el-form class="process-pool-write__form" :model="reviewForm" label-width="160px" @submit.prevent>
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="工序池提交事件ID">
              <el-input-number v-model="reviewForm.eventId" :min="1" :controls="false" class="process-pool-write__number" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="审核人用户ID">
              <el-input-number v-model="reviewForm.reviewerUserId" :min="1" :controls="false" class="process-pool-write__number" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="审核签名ID">
              <el-input-number v-model="reviewForm.reviewerSignatureId" :min="1" :controls="false" class="process-pool-write__number" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="签名员工用户ID">
              <el-input-number
                v-model="reviewForm.reviewerSignatureUserId"
                :min="1"
                :controls="false"
                class="process-pool-write__number"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="审核签名快照JSON">
          <el-input
            v-model="reviewForm.reviewerSignatureSnapshotJson"
            type="textarea"
            :rows="5"
            resize="vertical"
          />
        </el-form-item>

        <el-form-item label="字段上下限映射JSON">
          <el-input v-model="reviewForm.fieldMappingsJson" type="textarea" :rows="12" resize="vertical" />
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
  reviewerSignatureSnapshotJson: '{\n  "signType": "REVIEW",\n  "signedAt": "2026-07-30T00:00:00+08:00"\n}',
  fieldMappingsJson:
    '[\n' +
    '  {\n' +
    '    "fieldCode": "devicePressure",\n' +
    '    "fieldName": "设备压力",\n' +
    '    "lowerLimit": 20,\n' +
    '    "upperLimit": 40,\n' +
    '    "valueType": "NUMBER"\n' +
    '  }\n' +
    ']'
})

const submitLoading = ref(false)
const submitError = ref('')
const resultReviewCopyId = ref<number>()

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
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
    throw new Error(`${label}必须是合法 JSON`)
  }
}

const requirePositiveNumber = (value: number | undefined, label: string) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${label}必须大于 0`)
  }
  return parsed
}

const buildRequestPayload = (): ProcessPoolReviewCopyGenerateSubmitReqVO => {
  parseJsonField<Record<string, unknown>>(reviewForm.reviewerSignatureSnapshotJson, '审核签名快照JSON')
  const fieldMappings = parseJsonField<ProcessPoolReviewCopyFieldMappingVO[]>(
    reviewForm.fieldMappingsJson,
    '字段上下限映射JSON'
  )
  if (!Array.isArray(fieldMappings) || fieldMappings.length === 0) {
    throw new Error('字段上下限映射JSON必须是非空数组')
  }

  return {
    eventId: requirePositiveNumber(reviewForm.eventId, '工序池提交事件ID'),
    reviewerUserId: requirePositiveNumber(reviewForm.reviewerUserId, '审核人用户ID'),
    reviewerSignatureId: requirePositiveNumber(reviewForm.reviewerSignatureId, '审核签名ID'),
    reviewerSignatureUserId: requirePositiveNumber(reviewForm.reviewerSignatureUserId, '签名员工用户ID'),
    reviewerSignatureSnapshot: reviewForm.reviewerSignatureSnapshotJson.trim(),
    fieldMappings
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
