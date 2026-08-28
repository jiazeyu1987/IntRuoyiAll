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
      label-width="110px"
      data-testid="registration-certificate-upload-form"
    >
      <el-row :gutter="16">
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
              @change="handleProjectCodeChange"
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
          <el-form-item label="公司名称" prop="companyName">
            <el-input
              v-model="form.companyName"
              maxlength="255"
              placeholder="请输入公司名称"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目代码">
            <el-input v-model="form.projectCode" placeholder="选择DCC项目代码后自动带出" readonly />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="产品名称">
            <el-input v-model="form.productName" placeholder="选择DCC项目代码后自动带出" readonly />
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
            <el-input
              v-model="form.classification"
              maxlength="64"
              placeholder="请输入类别"
            />
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
  submitRegistrationCertificateUpload,
  type DccRegistrationCertificateUploadSubmitReqVO
} from '@/api/dcc/registrationCertificate'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCode,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import { getProduct } from '@/api/mdm/product'
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
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)

type RegistrationCertificateUploadForm = Omit<
  DccRegistrationCertificateUploadSubmitReqVO,
  'projectCodeId' | 'remark'
> & {
  projectCodeId?: number | string
  projectCode: string
  productName: string
  remark: string
}

const form = reactive<RegistrationCertificateUploadForm>({
  projectCodeId: undefined,
  companyName: '',
  projectCode: '',
  productName: '',
  certificateNo: '',
  firstObtainedDate: '',
  effectiveDate: '',
  expiryDate: '',
  classification: '',
  remark: ''
})

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const rules = reactive<FormRules>({
  projectCodeId: [{ required: true, message: '请选择DCC项目代码', trigger: 'change' }],
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  certificateNo: [{ required: true, message: '请输入注册证号', trigger: 'blur' }],
  firstObtainedDate: [{ required: true, message: '请选择首次获证日期', trigger: 'change' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  expiryDate: [{ required: true, message: '请选择有效期至', trigger: 'change' }],
  classification: [{ required: true, message: '请输入类别', trigger: 'blur' }]
})

const resetForm = () => {
  form.projectCodeId = undefined
  form.companyName = ''
  form.projectCode = ''
  form.productName = ''
  form.certificateNo = ''
  form.firstObtainedDate = ''
  form.effectiveDate = ''
  form.expiryDate = ''
  form.classification = ''
  form.remark = ''
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
      status: DCC_PROJECT_CODE_STATUS_ENABLE,
      requireDccProductCode: true
    })
    projectCodeOptions.value = page.list || []
  } finally {
    projectCodeLoading.value = false
  }
}

const searchProjectCodes = async (keyword: string) => {
  await loadProjectCodes(keyword)
}

const formatProjectCodeOption = (item: DccProjectCodeRespVO) => {
  const parts = [item.projectCode, item.projectName].filter(Boolean)
  return parts.join(' - ')
}

const applyProjectCode = async (projectCodeId?: number | string) => {
  if (!projectCodeId) {
    form.projectCode = ''
    form.productName = ''
    return
  }
  let projectCode = projectCodeOptions.value.find((item) => item.id === projectCodeId)
  if (!projectCode) {
    projectCode = await getProjectCode(projectCodeId)
  }
  form.projectCode = projectCode.projectCode || ''
  if (projectCode.productMasterId) {
    const product = await getProduct(projectCode.productMasterId)
    form.productName = product.nameCn || ''
  } else {
    form.productName = ''
  }
}

const handleProjectCodeChange = async (projectCodeId: number | string | undefined) => {
  form.projectCodeId = projectCodeId
  await applyProjectCode(projectCodeId)
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
  if (!selectedFile.value) {
    message.error('请先选择注册证文件')
    return
  }
  if (!form.projectCodeId) {
    message.error('请选择DCC项目代码')
    return
  }
  const payload = new FormData()
  payload.append('companyName', form.companyName.trim())
  payload.append('projectCodeId', String(form.projectCodeId))
  payload.append('certificateNo', form.certificateNo.trim())
  payload.append('firstObtainedDate', form.firstObtainedDate)
  payload.append('effectiveDate', form.effectiveDate)
  payload.append('expiryDate', form.expiryDate)
  payload.append('classification', form.classification.trim())
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
  } catch (error) {
    message.error((error as { message?: string })?.message || '提交失败')
    throw error
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
    await loadProjectCodes('')
    if (form.projectCodeId) {
      await applyProjectCode(form.projectCodeId)
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
    padding: 20px 24px !important;
  }

  .el-dialog__footer {
    padding: 16px 24px 20px;
  }

  .registration-certificate-upload-dialog__form {
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
</style>
