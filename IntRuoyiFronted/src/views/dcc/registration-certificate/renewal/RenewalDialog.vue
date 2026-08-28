<template>
  <el-dialog
    v-model="dialogVisible"
    class="registration-certificate-renewal-dialog"
    data-testid="registration-certificate-renewal-dialog"
    destroy-on-close
    title="延续注册证"
    width="720px"
    @closed="handleClosed"
  >
    <el-descriptions v-if="certificate" :column="2" border class="mb-16px">
      <el-descriptions-item label="证件编号">{{ certificate.certificateNo }}</el-descriptions-item>
      <el-descriptions-item label="产品">{{ certificate.productName }}</el-descriptions-item>
      <el-descriptions-item label="当前生效日">{{ certificate.effectiveDate || '—' }}</el-descriptions-item>
      <el-descriptions-item label="当前有效期">{{ certificate.expiryDate || '—' }}</el-descriptions-item>
    </el-descriptions>

    <el-form
      ref="formRef"
      class="registration-certificate-renewal-dialog__form"
      :model="form"
      :rules="rules"
      label-width="120px"
      data-testid="registration-certificate-renewal-form"
    >
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="批准日期" prop="approvalDate">
            <el-date-picker
              v-model="form.approvalDate"
              clearable
              format="YYYY-MM-DD"
              placeholder="请选择日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="生效日期" prop="effectiveDate">
            <el-date-picker
              v-model="form.effectiveDate"
              clearable
              format="YYYY-MM-DD"
              placeholder="请选择日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="有效期至" prop="expiryDate">
            <el-date-picker
              v-model="form.expiryDate"
              clearable
              format="YYYY-MM-DD"
              placeholder="请选择日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="延续注册证文件">
            <el-upload
              v-model:file-list="fileList"
              action="#"
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              data-testid="registration-certificate-renewal-file"
            >
              <el-button>
                <Icon icon="ep:upload" class="mr-5px" />选择文件
              </el-button>
            </el-upload>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
      <el-button :loading="saving" type="primary" @click="submit">提交审批</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  submitRegistrationCertificateRenewal,
  type DccRegistrationCertificatePageItemVO
} from '@/api/dcc/registrationCertificate'
import { generateUUID } from '@/utils'
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormRules, UploadFile, UploadFiles, UploadUserFile } from 'element-plus'

defineOptions({ name: 'RegistrationCertificateRenewalDialog' })

const props = defineProps<{
  modelValue: boolean
  certificate?: DccRegistrationCertificatePageItemVO
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const message = useMessage()
const formRef = ref<FormInstance>()
const saving = ref(false)
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)

const form = reactive({
  approvalDate: '',
  effectiveDate: '',
  expiryDate: ''
})

const certificate = computed(() => props.certificate)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const rules = reactive<FormRules>({
  approvalDate: [{ required: true, message: '请选择批准日期', trigger: 'change' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  expiryDate: [{ required: true, message: '请选择有效期至', trigger: 'change' }]
})

const resetForm = () => {
  form.approvalDate = ''
  form.effectiveDate = ''
  form.expiryDate = ''
  fileList.value = []
  selectedFile.value = null
  formRef.value?.clearValidate()
}

const handleClosed = () => {
  resetForm()
}

const handleFileChange = (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  selectedFile.value = uploadFile.raw ?? null
  fileList.value = uploadFiles as UploadUserFile[]
}

const handleFileRemove = () => {
  selectedFile.value = null
  fileList.value = []
}

const submit = async () => {
  await formRef.value?.validate()
  if (!props.certificate) {
    message.error('缺少当前注册证行，无法提交延续申请')
    return
  }
  if (!selectedFile.value) {
    message.error('请先选择延续注册证文件')
    return
  }

  const payload = new FormData()
  payload.append('approvalDate', form.approvalDate)
  payload.append('effectiveDate', form.effectiveDate)
  payload.append('expiryDate', form.expiryDate)
  payload.append('expectedRowVersion', String(props.certificate.rowVersion))
  payload.append('currentVersionId', String(props.certificate.versionId))
  payload.append('file', selectedFile.value)

  saving.value = true
  try {
    await submitRegistrationCertificateRenewal(
      props.certificate.certificateId,
      payload,
      `DCC-REG-CERT-RENEWAL-${generateUUID()}`
    )
    message.success('延续注册证已提交注册部经理审批')
    dialogVisible.value = false
    emit('saved')
  } catch (error) {
    message.error((error as { message?: string })?.message || '提交延续申请失败')
    throw error
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss">
.registration-certificate-renewal-dialog.el-dialog {
  max-width: calc(100vw - 24px);
}

.registration-certificate-renewal-dialog {
  .el-dialog__body {
    max-height: calc(100vh - 180px);
    overflow-y: auto;
    padding: 20px 24px !important;
  }

  .el-dialog__footer {
    padding: 16px 24px 20px;
  }

  .registration-certificate-renewal-dialog__form {
    .el-form-item {
      margin-bottom: 18px;
    }

    .el-form-item__label {
      height: auto;
      padding: 0;
      margin-bottom: 8px;
      color: #263247;
      font-weight: 600;
      line-height: 20px;
    }

    .el-date-editor {
      width: 100%;
    }
  }
}
</style>
