<template>
  <ContentWrap>
    <div class="process-pool-write">
      <div class="process-pool-write__header">
        <div>
          <div class="process-pool-write__title">原始记录修改</div>
          <div class="process-pool-write__subtitle"
            >仅允许修改未被 先进先出分配锁定的原始记录，并要求重新电子签名。</div
          >
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

      <el-form
        class="process-pool-write__form"
        :model="revisionForm"
        label-width="160px"
        @submit.prevent
      >
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="工序提交记录编号">
              <el-input-number
                v-model="revisionForm.eventId"
                :min="1"
                :controls="false"
                class="process-pool-write__number"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="签名凭据">
              <SignatureCredentialInput
                v-model="revisionForm.signaturePassword"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="变更原因">
          <el-input v-model="revisionForm.changeReason" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="修改后业务资料">
          <BusinessJsonInput
            ref="payloadInput"
            v-model="revisionForm.afterPayloadJson"
            label="修改后业务资料"
          />
        </el-form-item>
        <el-form-item label="字段变更明细">
          <div style="width: 100%; overflow: auto">
            <el-table
              :data="changedFields"
              border
              style="min-width: 920px"
              empty-text="请添加本次需要修改的字段"
            >
              <el-table-column label="字段名称" min-width="140"
                ><template #default="{ row }"
                  ><el-input v-model="row.fieldName" aria-label="字段名称" /></template
              ></el-table-column>
              <el-table-column label="字段标识" min-width="140"
                ><template #default="{ row }"
                  ><el-input v-model="row.fieldCode" aria-label="字段标识" /></template
              ></el-table-column>
              <el-table-column label="修改前" min-width="120"
                ><template #default="{ row }"
                  ><el-input v-model="row.beforeValue" aria-label="修改前" /></template
              ></el-table-column>
              <el-table-column label="修改后" min-width="120"
                ><template #default="{ row }"
                  ><el-input v-model="row.afterValue" aria-label="修改后" /></template
              ></el-table-column>
              <el-table-column label="影响数量片段" width="120"
                ><template #default="{ row }"
                  ><el-switch
                    v-model="row.affectsQuantityFragment"
                    aria-label="影响数量片段" /></template
              ></el-table-column>
              <el-table-column label="数量片段编号" min-width="150"
                ><template #default="{ row }"
                  ><el-input-number
                    v-model="row.sourceQuantityFragmentId"
                    :min="1"
                    :controls="false"
                    :disabled="!row.affectsQuantityFragment"
                    aria-label="数量片段编号" /></template
              ></el-table-column>
              <el-table-column label="原始字段" min-width="150"
                ><template #default="{ row }"
                  ><el-select v-model="row.originalField" aria-label="原始字段"
                    ><el-option
                      v-for="item in originalFields"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value" /></el-select></template
              ></el-table-column>
              <el-table-column label="操作" width="70"
                ><template #default="{ $index }"
                  ><el-button link type="danger" @click="changedFields.splice($index, 1)"
                    >删除</el-button
                  ></template
                ></el-table-column
              >
            </el-table>
            <el-button class="mt-8px" @click="addChangedField">添加变更字段</el-button>
          </div>
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
import SignatureCredentialInput from '@/components/SignatureCredentialInput/SignatureCredentialInput.vue'
import BusinessJsonInput from '@/components/BusinessData/BusinessJsonInput.vue'
import { ElMessage } from 'element-plus'
import {
  updateProcessPoolOriginalRecord,
  type ProcessPoolEventRevisionFieldChangeVO,
  type ProcessPoolEventRevisionUpdateReqVO
} from '@/api/mes/pro/processpool/eventRevision'

defineOptions({ name: 'MesProProcessPoolEventRevision' })

const revisionForm = reactive({
  eventId: undefined as number | undefined,
  signaturePassword: '',
  changeReason: '',
  afterPayloadJson: ''
})

const payloadInput = ref<InstanceType<typeof BusinessJsonInput>>()
const changedFields = ref<ProcessPoolEventRevisionFieldChangeVO[]>([])
const originalFields = [
  { label: '输出数量', value: 'OUTPUT_QUANTITY' },
  { label: '质量状态', value: 'QUALITY_STATUS' },
  { label: '可分配状态', value: 'ALLOCATABLE_STATUS' },
  { label: '备注', value: 'REMARK' }
]
const addChangedField = () =>
  changedFields.value.push({
    fieldCode: '',
    fieldName: '',
    beforeValue: '',
    afterValue: '',
    affectsQuantityFragment: false,
    originalField: undefined as unknown as ProcessPoolEventRevisionFieldChangeVO['originalField']
  })

const submitLoading = ref(false)
const submitError = ref('')
const resultRevisionId = ref<number>()

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
    throw new Error(`${label}格式错误，请核对业务资料`)
  }
}

const requirePositiveNumber = (value: number | undefined, label: string) => {
  const parsed = Number(value)
  if (!Number.isSafeInteger(parsed) || parsed <= 0) {
    throw new Error(`${label}必须大于 0`)
  }
  return parsed
}

const buildRequestPayload = (): ProcessPoolEventRevisionUpdateReqVO => {
  const validationError = payloadInput.value?.validate()
  if (validationError) throw new Error(validationError)
  parseJsonField<Record<string, unknown>>(revisionForm.afterPayloadJson, '修改后业务资料')
  if (!changedFields.value.length) throw new Error('请添加至少一项字段变更')
  for (const [index, item] of changedFields.value.entries()) {
    if (!item.fieldName.trim() || !item.fieldCode.trim() || !item.originalField)
      throw new Error(`第 ${index + 1} 项变更：请填写字段名称、标识并选择原始字段`)
    if (item.affectsQuantityFragment)
      requirePositiveNumber(item.sourceQuantityFragmentId, `第 ${index + 1} 项数量片段编号`)
  }
  if (!revisionForm.changeReason.trim()) {
    throw new Error('变更原因不能为空')
  }
  if (!revisionForm.signaturePassword.trim()) {
    throw new Error('电子签名凭据不能为空')
  }

  return {
    eventId: requirePositiveNumber(revisionForm.eventId, '工序提交记录编号'),
    afterPayload: revisionForm.afterPayloadJson.trim(),
    changeReason: revisionForm.changeReason.trim(),
    signaturePassword: revisionForm.signaturePassword.trim(),
    changedFields: changedFields.value.map((item) => ({ ...item }))
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
