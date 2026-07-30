<template>
  <ContentWrap>
    <div class="process-pool-write">
      <div class="process-pool-write__header">
        <div>
          <div class="process-pool-write__title">原始记录修改</div>
          <div class="process-pool-write__subtitle">仅允许修改未被 FIFO 分配锁定的原始记录，并要求重新电子签名。</div>
        </div>
      </div>

      <el-alert v-if="submitError" :title="submitError" type="error" :closable="false" show-icon />
      <el-alert
        v-if="resultRevisionId"
        :title="`原始记录修改编号：${resultRevisionId}`"
        type="success"
        :closable="false"
        show-icon
      />

      <el-form class="process-pool-write__form" :model="revisionForm" label-width="160px" @submit.prevent>
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="工序池提交事件ID">
              <el-input-number v-model="revisionForm.eventId" :min="1" :controls="false" class="process-pool-write__number" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="修改人用户ID">
              <el-input-number v-model="revisionForm.modifiedByUserId" :min="1" :controls="false" class="process-pool-write__number" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="修改签名ID">
              <el-input-number v-model="revisionForm.revisionSignatureId" :min="1" :controls="false" class="process-pool-write__number" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="签名员工用户ID">
              <el-input-number
                v-model="revisionForm.revisionSignatureUserId"
                :min="1"
                :controls="false"
                class="process-pool-write__number"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="变更原因">
          <el-input v-model="revisionForm.changeReason" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="修改后payload JSON">
          <el-input v-model="revisionForm.afterPayloadJson" type="textarea" :rows="8" resize="vertical" />
        </el-form-item>

        <el-form-item label="修改签名快照JSON">
          <el-input
            v-model="revisionForm.revisionSignatureSnapshotJson"
            type="textarea"
            :rows="5"
            resize="vertical"
          />
        </el-form-item>

        <el-form-item label="字段变更JSON">
          <el-input v-model="revisionForm.changedFieldsJson" type="textarea" :rows="12" resize="vertical" />
        </el-form-item>

        <div class="process-pool-write__actions">
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            <Icon icon="ep:check" class="mr-5px" />
            修改并重新签名
          </el-button>
        </div>
      </el-form>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  updateProcessPoolOriginalRecord,
  type ProcessPoolEventRevisionFieldChangeVO,
  type ProcessPoolEventRevisionUpdateReqVO
} from '@/api/mes/pro/processpool/eventRevision'

defineOptions({ name: 'MesProProcessPoolEventRevision' })

const revisionForm = reactive({
  eventId: undefined as number | undefined,
  modifiedByUserId: undefined as number | undefined,
  revisionSignatureId: undefined as number | undefined,
  revisionSignatureUserId: undefined as number | undefined,
  changeReason: '',
  afterPayloadJson: '{\n  "outputQuantity": 100,\n  "lossQuantity": 0\n}',
  revisionSignatureSnapshotJson: '{\n  "signType": "REVISION",\n  "signedAt": "2026-07-30T00:00:00+08:00"\n}',
  changedFieldsJson:
    '[\n' +
    '  {\n' +
    '    "fieldCode": "outputQuantity",\n' +
    '    "fieldName": "输出数量",\n' +
    '    "beforeValue": "90",\n' +
    '    "afterValue": "100",\n' +
    '    "affectsQuantityFragment": true,\n' +
    '    "sourceQuantityFragmentId": 1,\n' +
    '    "originalField": "OUTPUT_QUANTITY"\n' +
    '  }\n' +
    ']'
})

const submitLoading = ref(false)
const submitError = ref('')
const resultRevisionId = ref<number>()

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

const buildRequestPayload = (): ProcessPoolEventRevisionUpdateReqVO => {
  parseJsonField<Record<string, unknown>>(revisionForm.afterPayloadJson, '修改后payload JSON')
  parseJsonField<Record<string, unknown>>(revisionForm.revisionSignatureSnapshotJson, '修改签名快照JSON')
  const changedFields = parseJsonField<ProcessPoolEventRevisionFieldChangeVO[]>(
    revisionForm.changedFieldsJson,
    '字段变更JSON'
  )
  if (!Array.isArray(changedFields) || changedFields.length === 0) {
    throw new Error('字段变更JSON必须是非空数组')
  }
  if (changedFields.some((item) => typeof item.affectsQuantityFragment !== 'boolean')) {
    throw new Error('字段变更JSON中 affectsQuantityFragment 必须是 true 或 false')
  }
  if (!revisionForm.changeReason.trim()) {
    throw new Error('变更原因不能为空')
  }

  return {
    eventId: requirePositiveNumber(revisionForm.eventId, '工序池提交事件ID'),
    afterPayload: revisionForm.afterPayloadJson.trim(),
    changeReason: revisionForm.changeReason.trim(),
    revisionSignatureId: requirePositiveNumber(revisionForm.revisionSignatureId, '修改签名ID'),
    revisionSignatureUserId: requirePositiveNumber(revisionForm.revisionSignatureUserId, '签名员工用户ID'),
    revisionSignatureSnapshot: revisionForm.revisionSignatureSnapshotJson.trim(),
    modifiedByUserId: requirePositiveNumber(revisionForm.modifiedByUserId, '修改人用户ID'),
    changedFields
  }
}

const handleSubmit = async () => {
  submitLoading.value = true
  submitError.value = ''
  resultRevisionId.value = undefined
  try {
    resultRevisionId.value = await updateProcessPoolOriginalRecord(buildRequestPayload())
    ElMessage.success('原始记录已修改并重新签名')
  } catch (error) {
    submitError.value = resolveErrorMessage(error, '原始记录修改失败')
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
