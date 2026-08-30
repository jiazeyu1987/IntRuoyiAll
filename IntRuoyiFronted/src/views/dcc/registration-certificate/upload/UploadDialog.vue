<template>
  <el-dialog
    v-model="dialogVisible"
    class="registration-certificate-upload-dialog"
    destroy-on-close
    data-testid="registration-certificate-upload-dialog"
    title="上传注册证"
    width="920px"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      class="registration-certificate-upload-dialog__form"
      :model="form"
      :rules="rules"
      label-width="124px"
      data-testid="registration-certificate-upload-form"
    >
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="DCC项目代码" prop="projectCodeId">
            <el-select
              v-model="form.projectCodeId"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchProjectCodes"
              :loading="projectCodeLoading"
              placeholder="请选择DCC项目代码"
            >
              <el-option
                v-for="item in projectCodeOptions"
                :key="item.id"
                :label="formatProjectCodeOption(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="公司名称" prop="companyId">
            <el-select
              v-model="form.companyId"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchOwnerCompanies"
              :loading="ownerCompanyLoading"
              placeholder="请选择公司名称"
              data-testid="registration-certificate-upload-owner-company"
            >
              <el-option
                v-for="item in ownerCompanyOptions"
                :key="item.id"
                :label="formatOwnerCompanyOption(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="产品名称" prop="productName">
            <el-input
              v-model="form.productName"
              maxlength="255"
              placeholder="请输入产品名称"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="注册证号" prop="certificateNo">
            <el-input
              v-model="form.certificateNo"
              maxlength="128"
              placeholder="请输入注册证号"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="类别" prop="classification">
            <el-select
              v-model="form.classification"
              clearable
              placeholder="请选择类别"
              data-testid="registration-certificate-upload-classification"
            >
              <el-option
                v-for="option in REGISTRATION_CERTIFICATE_CLASSIFICATION_OPTIONS"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="首次获证日期" prop="firstObtainedDate">
            <el-date-picker
              v-model="form.firstObtainedDate"
              clearable
              format="YYYY-MM-DD"
              placeholder="请选择日期"
              type="date"
              value-format="YYYY-MM-DD"
              @change="revalidateDateFields"
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
              @change="revalidateDateFields"
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
              @change="revalidateDateFields"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否委托生产" prop="entrustedProduction">
            <el-select
              v-model="form.entrustedProduction"
              clearable
              placeholder="请选择是否委托生产"
              data-testid="registration-certificate-upload-entrusted-production"
              @change="handleEntrustedProductionChange"
            >
              <el-option label="是" :value="true" />
              <el-option label="否" :value="false" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否自行生产" prop="selfProduction">
            <el-select
              v-model="form.selfProduction"
              clearable
              placeholder="请选择是否自行生产"
              data-testid="registration-certificate-upload-self-production"
              @change="handleSelfProductionChange"
            >
              <el-option label="是" :value="true" />
              <el-option label="否" :value="false" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col v-if="form.entrustedProduction === true" :span="24">
          <el-form-item label="受托企业" prop="entrustedEnterpriseIds">
            <el-select
              v-model="form.entrustedEnterpriseIds"
              multiple
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchEntrustedEnterprises"
              :loading="entrustedEnterpriseLoading"
              placeholder="受托企业：请选择已启用的受托企业"
              data-testid="registration-certificate-upload-entrusted-enterprises"
            >
              <el-option
                v-for="item in entrustedEnterpriseOptions"
                :key="item.id"
                :label="formatEntrustedEnterpriseOption(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="form.remark"
              :rows="3"
              maxlength="1024"
              placeholder="请输入备注"
              type="textarea"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="注册证文件">
            <el-upload
              v-model:file-list="fileList"
              action="#"
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              data-testid="registration-certificate-upload-file"
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
      <el-button :loading="saving" type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  getUploadEntrustedEnterprises,
  getUploadOwnerCompanies,
  submitRegistrationCertificateUpload,
  type DccRegistrationCertificateUploadCompanyRespVO,
  type DccRegistrationCertificateUploadEntrustedEnterpriseRespVO,
  type DccRegistrationCertificateUploadSubmitReqVO
} from '@/api/dcc/registrationCertificate'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import { generateUUID } from '@/utils'
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules, UploadFile, UploadFiles, UploadUserFile } from 'element-plus'

defineOptions({ name: 'DccRegistrationCertificateUploadDialog' })

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const message = useMessage()
const formRef = ref<FormInstance>()
const saving = ref(false)
const projectCodeLoading = ref(false)
const projectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const ownerCompanyLoading = ref(false)
const ownerCompanyOptions = ref<DccRegistrationCertificateUploadCompanyRespVO[]>([])
const entrustedEnterpriseLoading = ref(false)
const entrustedEnterpriseOptions = ref<DccRegistrationCertificateUploadEntrustedEnterpriseRespVO[]>([])
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)
const REGISTRATION_CERTIFICATE_CLASSIFICATION_OPTIONS = ['三类', '二类', '一类'] as const
const DATE_ORDER_MESSAGE = '注册证日期顺序不正确：首次获证日期不能晚于生效日期，生效日期必须早于有效期至'

type RegistrationCertificateUploadForm = Omit<
  DccRegistrationCertificateUploadSubmitReqVO,
  'projectCodeId' | 'companyId' | 'entrustedProduction' | 'selfProduction' | 'entrustedEnterpriseIds' | 'remark'
> & {
  projectCodeId?: number | string
  companyId?: number | string
  productName: string
  entrustedProduction?: boolean
  selfProduction?: boolean
  entrustedEnterpriseIds: Array<number | string>
  remark: string
}

const form = reactive<RegistrationCertificateUploadForm>({
  projectCodeId: undefined,
  companyId: undefined,
  productName: '',
  certificateNo: '',
  firstObtainedDate: '',
  effectiveDate: '',
  expiryDate: '',
  classification: '',
  entrustedProduction: undefined,
  selfProduction: undefined,
  entrustedEnterpriseIds: [],
  remark: ''
})

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

function validateProductionRelation(
  _rule: unknown,
  _value: unknown,
  callback: (error?: Error) => void
) {
  if (form.entrustedProduction === false && form.selfProduction === false) {
    callback(new Error('是否委托生产和是否自行生产不能同时为否'))
    return
  }
  callback()
}

function validateEntrustedEnterpriseIds(
  _rule: unknown,
  _value: unknown,
  callback: (error?: Error) => void
) {
  if (form.entrustedProduction === true && form.entrustedEnterpriseIds.length === 0) {
    callback(new Error('请选择受托企业'))
    return
  }
  callback()
}

function isDateOrderValid() {
  if (!form.firstObtainedDate || !form.effectiveDate || !form.expiryDate) {
    return true
  }
  if (form.firstObtainedDate > form.effectiveDate || form.effectiveDate >= form.expiryDate) {
    return false
  }
  return true
}

function validateDateOrder(
  _rule: unknown,
  _value: unknown,
  callback: (error?: Error) => void
) {
  if (!isDateOrderValid()) {
    callback(new Error(DATE_ORDER_MESSAGE))
    return
  }
  callback()
}

const rules = reactive<FormRules>({
  companyId: [{ required: true, message: '请选择公司名称', trigger: 'change' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  certificateNo: [{ required: true, message: '请输入注册证号', trigger: 'blur' }],
  firstObtainedDate: [
    { required: true, message: '请选择首次获证日期', trigger: 'change' },
    { validator: validateDateOrder, trigger: 'change' }
  ],
  effectiveDate: [
    { required: true, message: '请选择生效日期', trigger: 'change' },
    { validator: validateDateOrder, trigger: 'change' }
  ],
  expiryDate: [
    { required: true, message: '请选择有效期至', trigger: 'change' },
    { validator: validateDateOrder, trigger: 'change' }
  ],
  classification: [{ required: true, message: '请选择类别', trigger: 'change' }],
  entrustedProduction: [
    { required: true, message: '请选择是否委托生产', trigger: 'change' },
    { validator: validateProductionRelation, trigger: 'change' }
  ],
  selfProduction: [
    { required: true, message: '请选择是否自行生产', trigger: 'change' },
    { validator: validateProductionRelation, trigger: 'change' }
  ],
  entrustedEnterpriseIds: [{ validator: validateEntrustedEnterpriseIds, trigger: 'change' }]
})

const resetForm = () => {
  form.projectCodeId = undefined
  form.companyId = undefined
  form.productName = ''
  form.certificateNo = ''
  form.firstObtainedDate = ''
  form.effectiveDate = ''
  form.expiryDate = ''
  form.classification = ''
  form.entrustedProduction = undefined
  form.selfProduction = undefined
  form.entrustedEnterpriseIds = []
  form.remark = ''
  ownerCompanyOptions.value = []
  entrustedEnterpriseOptions.value = []
  fileList.value = []
  selectedFile.value = null
  formRef.value?.clearValidate()
}

const handleClosed = () => {
  resetForm()
}

const loadProjectCodes = async (keyword = '') => {
  projectCodeLoading.value = true
  try {
    const page = await getProjectCodePage({
      pageNo: 1,
      pageSize: 20,
      keyword: keyword.trim() || undefined,
      status: DCC_PROJECT_CODE_STATUS_ENABLE
    })
    projectCodeOptions.value = page.list || []
  } finally {
    projectCodeLoading.value = false
  }
}

const searchProjectCodes = async (keyword: string) => {
  await loadProjectCodes(keyword)
}

const loadOwnerCompanies = async (keyword = '') => {
  ownerCompanyLoading.value = true
  try {
    ownerCompanyOptions.value = await getUploadOwnerCompanies({
      keyword: keyword.trim() || undefined
    })
  } finally {
    ownerCompanyLoading.value = false
  }
}

const searchOwnerCompanies = async (keyword: string) => {
  await loadOwnerCompanies(keyword)
}

const loadEntrustedEnterprises = async (keyword = '') => {
  entrustedEnterpriseLoading.value = true
  try {
    entrustedEnterpriseOptions.value = await getUploadEntrustedEnterprises({
      keyword: keyword.trim() || undefined
    })
  } finally {
    entrustedEnterpriseLoading.value = false
  }
}

const searchEntrustedEnterprises = async (keyword: string) => {
  await loadEntrustedEnterprises(keyword)
}

const formatProjectCodeOption = (item: DccProjectCodeRespVO) => {
  const parts = [item.projectCode, item.projectName].filter(Boolean)
  return parts.join(' - ')
}

const formatEntrustedEnterpriseOption = (
  item: DccRegistrationCertificateUploadEntrustedEnterpriseRespVO
) => {
  const parts = [item.name, item.enterpriseCode].filter(Boolean)
  return parts.join(' - ')
}

const formatOwnerCompanyOption = (item: DccRegistrationCertificateUploadCompanyRespVO) => {
  const parts = [item.name, item.enterpriseCode].filter(Boolean)
  return parts.join(' - ')
}

const revalidateProductionFields = () => {
  void formRef.value
    ?.validateField(['entrustedProduction', 'selfProduction', 'entrustedEnterpriseIds'])
    .catch(() => undefined)
}

const revalidateDateFields = () => {
  void formRef.value
    ?.validateField(['firstObtainedDate', 'effectiveDate', 'expiryDate'])
    .catch(() => undefined)
}

const handleEntrustedProductionChange = async () => {
  if (form.entrustedProduction !== true) {
    form.entrustedEnterpriseIds = []
  } else if (entrustedEnterpriseOptions.value.length === 0) {
    await loadEntrustedEnterprises('')
  }
  revalidateProductionFields()
}

const handleSelfProductionChange = () => {
  revalidateProductionFields()
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
  if (!isDateOrderValid()) {
    revalidateDateFields()
    message.error(DATE_ORDER_MESSAGE)
    return
  }
  await formRef.value?.validate()
  if (!selectedFile.value) {
    message.error('请先选择注册证文件')
    return
  }
  const payload = new FormData()
  payload.append('companyId', String(form.companyId))
  payload.append('productName', form.productName.trim())
  if (form.projectCodeId) {
    payload.append('projectCodeId', String(form.projectCodeId))
  }
  payload.append('certificateNo', form.certificateNo.trim())
  payload.append('firstObtainedDate', form.firstObtainedDate)
  payload.append('effectiveDate', form.effectiveDate)
  payload.append('expiryDate', form.expiryDate)
  payload.append('classification', form.classification.trim())
  payload.append('entrustedProduction', String(form.entrustedProduction))
  payload.append('selfProduction', String(form.selfProduction))
  form.entrustedEnterpriseIds.forEach((enterpriseId) => payload.append('entrustedEnterpriseIds', String(enterpriseId)))
  payload.append('remark', form.remark.trim())
  payload.append('file', selectedFile.value)
  saving.value = true
  try {
    await submitRegistrationCertificateUpload(
      payload,
      `DCC-REG-CERT-UPLOAD-${generateUUID()}`
    )
    message.success('已提交审批')
    dialogVisible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) {
      return
    }
    await loadOwnerCompanies('')
    await loadProjectCodes('')
    if (form.entrustedProduction === true) {
      await loadEntrustedEnterprises('')
    }
  }
)
</script>

<style lang="scss">
.registration-certificate-upload-dialog.el-dialog {
  max-width: calc(100vw - 24px);
}

.registration-certificate-upload-dialog {
  .el-dialog__body {
    max-height: calc(100vh - 180px);
    overflow-y: auto;
    padding: 24px 32px !important;
  }

  .el-dialog__footer {
    padding: 16px 24px 20px;
  }

  .registration-certificate-upload-dialog__form {
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
    .el-date-editor,
    .el-textarea {
      width: 100%;
    }

    .el-textarea__inner {
      resize: vertical;
    }
  }
}

@media (max-width: 720px) {
  .registration-certificate-upload-dialog {
    .el-dialog__body {
      padding: 20px 16px !important;
    }

    .registration-certificate-upload-dialog__form {
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
