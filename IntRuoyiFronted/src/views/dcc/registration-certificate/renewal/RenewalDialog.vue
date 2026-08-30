<template>
  <el-dialog
    v-model="dialogVisible"
    class="registration-certificate-renewal-dialog"
    data-testid="registration-certificate-renewal-dialog"
    destroy-on-close
    title="延续注册证"
    width="860px"
    @closed="handleClosed"
  >
    <el-descriptions v-if="certificate" :column="2" border class="mb-16px">
      <el-descriptions-item label="证件编号">{{ certificate.certificateNo }}</el-descriptions-item>
      <el-descriptions-item label="产品">{{ certificate.productName }}</el-descriptions-item>
      <el-descriptions-item label="当前生效日">{{ formatRegistrationCertificateDate(certificate.effectiveDate) }}</el-descriptions-item>
      <el-descriptions-item label="当前有效期">{{ formatRegistrationCertificateDate(certificate.expiryDate) }}</el-descriptions-item>
    </el-descriptions>

    <el-form
      ref="formRef"
      class="registration-certificate-renewal-dialog__form"
      :model="form"
      :rules="rules"
      label-width="128px"
      data-testid="registration-certificate-renewal-form"
    >
      <el-row :gutter="24">
        <el-col :span="12" :xs="24">
          <el-form-item label="批准日期" prop="approvalDate">
            <el-date-picker
              v-model="form.approvalDate"
              clearable
              format="YYYY-MM-DD"
              placeholder="请选择日期"
              type="date"
              value-format="YYYY-MM-DD"
              @change="revalidateRenewalDateFields"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12" :xs="24">
          <el-form-item label="生效日期" prop="effectiveDate">
            <el-date-picker
              v-model="form.effectiveDate"
              clearable
              format="YYYY-MM-DD"
              placeholder="请选择日期"
              type="date"
              value-format="YYYY-MM-DD"
              @change="revalidateRenewalDateFields"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12" :xs="24">
          <el-form-item label="有效期至" prop="expiryDate">
            <el-date-picker
              v-model="form.expiryDate"
              clearable
              format="YYYY-MM-DD"
              placeholder="请选择日期"
              type="date"
              value-format="YYYY-MM-DD"
              @change="revalidateRenewalDateFields"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12" :xs="24">
          <el-form-item label="类别否变更" prop="categoryChanged">
            <el-select
              v-model="form.categoryChanged"
              placeholder="请选择"
              @change="handleCategoryChanged"
            >
              <el-option label="否" :value="false" />
              <el-option label="是" :value="true" />
            </el-select>
          </el-form-item>
        </el-col>
        <template v-if="form.categoryChanged">
          <el-col :span="12" :xs="24">
            <el-form-item label="注册证号" prop="certificateNo">
              <el-input
                v-model="form.certificateNo"
                clearable
                maxlength="128"
                placeholder="请输入变更后的注册证号"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" :xs="24">
            <el-form-item label="类别" prop="classification">
              <el-select
                v-model="form.classification"
                clearable
                placeholder="请选择变更后的类别"
                data-testid="registration-certificate-renewal-classification"
              >
                <el-option
                  v-for="option in REGISTRATION_CERTIFICATE_RENEWAL_CLASSIFICATION_OPTIONS"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </template>
        <el-col :span="24" :xs="24">
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
import { formatRegistrationCertificateDate } from '../shared/state'

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
const REGISTRATION_CERTIFICATE_RENEWAL_CLASSIFICATION_OPTIONS = ['三类', '二类', '一类'] as const
const RENEWAL_DATE_ORDER_MESSAGE = '注册证日期顺序不正确：批准日期不能晚于生效日期，生效日期必须早于有效期至'
const RENEWAL_APPROVAL_DATE_MESSAGE = '批准日期不能晚于当前日期'

const form = reactive({
  approvalDate: '',
  effectiveDate: '',
  expiryDate: '',
  categoryChanged: false,
  certificateNo: '',
  classification: ''
})

const certificate = computed(() => props.certificate)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

function currentLocalDateText() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function isRenewalDateOrderValid() {
  if (!form.approvalDate || !form.effectiveDate || !form.expiryDate) {
    return true
  }
  return !(form.approvalDate > form.effectiveDate || form.effectiveDate >= form.expiryDate)
}

function isRenewalApprovalDateInFuture() {
  if (!form.approvalDate) {
    return false
  }
  return form.approvalDate > currentLocalDateText()
}

function validateRenewalDateOrder(
  _rule: unknown,
  _value: unknown,
  callback: (error?: Error) => void
) {
  if (!isRenewalDateOrderValid()) {
    callback(new Error(RENEWAL_DATE_ORDER_MESSAGE))
    return
  }
  callback()
}

function validateRenewalApprovalDate(
  _rule: unknown,
  _value: unknown,
  callback: (error?: Error) => void
) {
  if (isRenewalApprovalDateInFuture()) {
    callback(new Error(RENEWAL_APPROVAL_DATE_MESSAGE))
    return
  }
  callback()
}

const revalidateRenewalDateFields = () => {
  void formRef.value?.validateField(['approvalDate', 'effectiveDate', 'expiryDate'])
}

const rules = reactive<FormRules>({
  approvalDate: [
    { required: true, message: '请选择批准日期', trigger: 'change' },
    { validator: validateRenewalDateOrder, trigger: 'change' },
    { validator: validateRenewalApprovalDate, trigger: 'change' }
  ],
  effectiveDate: [
    { required: true, message: '请选择生效日期', trigger: 'change' },
    { validator: validateRenewalDateOrder, trigger: 'change' }
  ],
  expiryDate: [
    { required: true, message: '请选择有效期至', trigger: 'change' },
    { validator: validateRenewalDateOrder, trigger: 'change' }
  ],
  categoryChanged: [{ required: true, message: '请选择类别否变更', trigger: 'change' }],
  certificateNo: [
    {
      validator: (_rule, value: string, callback) => {
        if (form.categoryChanged && !value?.trim()) {
          callback(new Error('请输入变更后的注册证号'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  classification: [
    {
      validator: (_rule, value: string, callback) => {
        if (form.categoryChanged && !value?.trim()) {
          callback(new Error('请选择变更后的类别'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
})

const resetForm = () => {
  form.approvalDate = ''
  form.effectiveDate = ''
  form.expiryDate = ''
  form.categoryChanged = false
  form.certificateNo = ''
  form.classification = ''
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

const handleCategoryChanged = () => {
  if (!form.categoryChanged) {
    form.certificateNo = ''
    form.classification = ''
    formRef.value?.clearValidate(['certificateNo', 'classification'])
  }
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
  if (!isRenewalDateOrderValid()) {
    message.error(RENEWAL_DATE_ORDER_MESSAGE)
    return
  }
  if (isRenewalApprovalDateInFuture()) {
    message.error(RENEWAL_APPROVAL_DATE_MESSAGE)
    return
  }

  const payload = new FormData()
  payload.append('approvalDate', form.approvalDate)
  payload.append('effectiveDate', form.effectiveDate)
  payload.append('expiryDate', form.expiryDate)
  payload.append('expectedRowVersion', String(props.certificate.rowVersion))
  payload.append('currentVersionId', String(props.certificate.versionId))
  payload.append('categoryChanged', String(form.categoryChanged))
  if (form.categoryChanged) {
    payload.append('certificateNo', form.certificateNo.trim())
    payload.append('classification', form.classification.trim())
  }
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
    padding: 24px 32px !important;
  }

  .el-dialog__footer {
    padding: 16px 24px 20px;
  }

  .registration-certificate-renewal-dialog__form {
    .el-row {
      row-gap: 4px;
    }

    .el-form-item {
      margin-bottom: 20px;
    }

    .el-form-item__label {
      height: auto;
      padding-right: 12px;
      margin-bottom: 0;
      color: #263247;
      font-weight: 600;
      line-height: 32px;
      white-space: nowrap;
    }

    .el-input,
    .el-select,
    .el-date-editor {
      width: 100%;
    }
  }
}

@media (max-width: 720px) {
  .registration-certificate-renewal-dialog {
    .el-dialog__body {
      padding: 20px 16px !important;
    }

    .registration-certificate-renewal-dialog__form {
      .el-form-item {
        display: block;
      }

      .el-form-item__label {
        width: 100% !important;
        justify-content: flex-start;
        padding-right: 0;
        margin-bottom: 8px;
        line-height: 20px;
      }

      .el-form-item__content {
        margin-left: 0 !important;
      }
    }
  }
}
</style>
