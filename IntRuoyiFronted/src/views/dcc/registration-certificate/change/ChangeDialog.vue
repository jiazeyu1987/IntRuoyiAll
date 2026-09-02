<template>
  <el-dialog
    v-model="dialogVisible"
    class="registration-certificate-change-dialog"
    data-testid="registration-certificate-change-dialog"
    destroy-on-close
    title="变更/作废"
    width="860px"
    @closed="resetForm"
  >
    <el-descriptions v-if="certificate" :column="2" border class="mb-16px">
      <el-descriptions-item label="证件编号">{{ certificate.certificateNo }}</el-descriptions-item>
      <el-descriptions-item label="产品">{{ certificate.productName }}</el-descriptions-item>
      <el-descriptions-item label="当前版本">第 {{ certificate.versionNo }} 版</el-descriptions-item>
      <el-descriptions-item label="当前状态">{{ certificate.status }}</el-descriptions-item>
    </el-descriptions>
    <el-alert
      v-if="detailLoadError"
      class="mb-16px"
      type="error"
      show-icon
      :closable="false"
      :title="detailLoadError"
    />
    <el-form
      v-loading="detailLoading"
      element-loading-text="正在加载注册证当前信息"
      class="registration-certificate-change-dialog__form"
      label-width="128px"
      data-testid="registration-certificate-change-form"
    >
      <el-row :gutter="24">
        <el-col :span="12" :xs="24">
          <el-form-item label="批准日期" required>
            <el-date-picker v-model="form.approvalDate" clearable format="YYYY-MM-DD" placeholder="请选择批准日期" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </el-col>
        <el-col :span="12" :xs="24">
          <el-form-item label="变更内容" required>
            <el-select v-model="form.changeTypes" multiple collapse-tags collapse-tags-tooltip placeholder="请选择变更内容" data-change-type-values="PRODUCT_NAME,MODEL_SPECIFICATION,STRUCTURE_COMPOSITION,INTENDED_USE,TECHNICAL_REQUIREMENTS,REGISTRANT_NAME,RESIDENCE_ADDRESS,PRODUCTION_ADDRESS,OTHER_CONTENT">
              <el-option v-for="item in changeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col v-for="item in selectedStructuredChangeTypes" :key="item.value" :span="12" :xs="24">
          <el-form-item :label="item.label" required><el-input v-model="form.structuredValues[item.value]" :placeholder="item.placeholder" /></el-form-item>
        </el-col>
        <el-col v-if="form.changeTypes.includes('OTHER_CONTENT')" :span="24" :xs="24">
          <el-form-item label="其他说明" required><el-input v-model="form.otherDescription" maxlength="4096" placeholder="请输入其他内容说明" type="textarea" :rows="3" /></el-form-item>
        </el-col>
        <template v-if="form.changeTypes.includes('PRODUCTION_ADDRESS')">
          <el-col :span="12" :xs="24">
            <el-form-item label="是否委托生产" required><el-select v-model="form.entrustedProduction" placeholder="请选择"><el-option label="是" :value="true" /><el-option label="否" :value="false" /></el-select></el-form-item>
          </el-col>
          <el-col :span="12" :xs="24">
            <el-form-item label="是否自行生产" required><el-select v-model="form.selfProduction" placeholder="请选择"><el-option label="是" :value="true" /><el-option label="否" :value="false" /></el-select></el-form-item>
          </el-col>
          <el-col v-if="form.entrustedProduction" :span="24" :xs="24">
            <el-form-item label="受托企业：" required><el-input v-model="form.entrustedEnterpriseNames" placeholder="请输入受托企业，多个请换行" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </template>
        <el-col :span="24" :xs="24">
          <el-form-item label="变更批件文件" required>
            <el-upload v-model:file-list="fileList" action="#" :auto-upload="false" :limit="1" :on-change="handleFileChange" :on-remove="handleFileRemove" accept=".pdf,.doc,.docx,.png,.jpg,.jpeg" data-testid="registration-certificate-change-approval-file">
              <el-button><Icon icon="ep:upload" class="mr-5px" />选择文件</el-button>
            </el-upload>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
      <el-button
        :disabled="saving || detailLoading || Boolean(detailLoadError)"
        :loading="saving"
        type="primary"
        @click="submit"
      >确认</el-button>
      <el-button
        :disabled="saving || detailLoading || Boolean(detailLoadError)"
        type="danger"
        plain
        @click="voidCertificate"
      >作废证书</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { UploadFile, UploadFiles, UploadUserFile } from 'element-plus'
import {
  getRegistrationCertificateDetail,
  submitRegistrationCertificateChange,
  voidRegistrationCertificate,
  type DccRegistrationCertificateDetailVO,
  type DccRegistrationCertificatePageItemVO
} from '@/api/dcc/registrationCertificate'
import { generateUUID } from '@/utils'
import { resolveRegistrationCertificateUserMessage } from '../shared/state'

defineOptions({ name: 'RegistrationCertificateChangeDialog' })
const props = defineProps<{ modelValue: boolean; certificate?: DccRegistrationCertificatePageItemVO }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; saved: [] }>()
const message = useMessage()
const saving = ref(false)
const detailLoading = ref(false)
const detailLoadError = ref('')
const currentDetail = ref<DccRegistrationCertificateDetailVO>()
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)
const structuredChangeTypeOptions = [
  { label: '产品名称', value: 'PRODUCT_NAME', placeholder: '变更后的产品名称' },
  { label: '型号规格', value: 'MODEL_SPECIFICATION', placeholder: '变更后的型号规格' },
  { label: '结构组成', value: 'STRUCTURE_COMPOSITION', placeholder: '变更后的结构组成' },
  { label: '适用范围', value: 'INTENDED_USE', placeholder: '变更后的适用范围' },
  { label: '产品技术要求', value: 'TECHNICAL_REQUIREMENTS', placeholder: '变更后的产品技术要求' },
  { label: '注册人名称', value: 'REGISTRANT_NAME', placeholder: '变更后的注册人名称' },
  { label: '住所', value: 'RESIDENCE_ADDRESS', placeholder: '变更后的住所' },
  { label: '生产地址', value: 'PRODUCTION_ADDRESS', placeholder: '变更后的生产地址' }
] as const
const changeTypeOptions = [...structuredChangeTypeOptions, { label: '其他内容', value: 'OTHER_CONTENT' }] as const
type StructuredChangeType = (typeof structuredChangeTypeOptions)[number]['value']
type ChangeType = StructuredChangeType | 'OTHER_CONTENT'
const form = reactive({ approvalDate: '', changeTypes: [] as ChangeType[], structuredValues: {} as Partial<Record<StructuredChangeType, string>>, otherDescription: '', entrustedProduction: undefined as boolean | undefined, selfProduction: undefined as boolean | undefined, entrustedEnterpriseNames: '' })
const dialogVisible = computed({ get: () => props.modelValue, set: (value: boolean) => emit('update:modelValue', value) })
const selectedStructuredChangeTypes = computed(() => structuredChangeTypeOptions.filter((item) => form.changeTypes.includes(item.value)))
const handleFileChange = (uploadFile: UploadFile, uploadFiles: UploadFiles) => { selectedFile.value = uploadFile.raw ?? null; fileList.value = uploadFiles.slice(-1) as UploadUserFile[] }
const handleFileRemove = () => { selectedFile.value = null; fileList.value = [] }
const normalizeText = (value?: string) => (value ?? '').trim()
const buildChangePayload = () => {
  if (!props.certificate) throw new Error('缺少当前注册证行，无法提交变更申请')
  if (!currentDetail.value) throw new Error('注册证当前信息尚未加载完成，无法提交变更申请')
  if (!form.approvalDate) throw new Error('请选择变更批件批准日期')
  if (form.changeTypes.length === 0) throw new Error('请选择至少一项变更内容')
  if (!selectedFile.value) throw new Error('请先选择变更批件文件')
  const payload = new FormData()
  payload.append('expectedRowVersion', String(currentDetail.value.rowVersion)); payload.append('approvalDate', form.approvalDate)
  form.changeTypes.forEach((changeType) => payload.append('changeTypes', changeType))
  for (const item of selectedStructuredChangeTypes.value) { const value = normalizeText(form.structuredValues[item.value]); if (!value) throw new Error(`请填写${item.placeholder}`); payload.append(`structuredValues[${item.value}]`, value) }
  if (form.changeTypes.includes('OTHER_CONTENT')) { const description = normalizeText(form.otherDescription); if (!description) throw new Error('请填写其他内容说明'); payload.append('otherDescription', description) }
  if (form.changeTypes.includes('PRODUCTION_ADDRESS')) {
    if (form.entrustedProduction === undefined || form.selfProduction === undefined) throw new Error('请选择是否委托生产和是否自行生产')
    if (!form.entrustedProduction && !form.selfProduction) throw new Error('委托生产和自行生产不可同时选择否。')
    const names = form.entrustedEnterpriseNames.split(/\r?\n|[,，;；]/).map((value) => value.trim()).filter(Boolean)
    if (form.entrustedProduction && names.length === 0) throw new Error('委托生产为是时，请填写受托企业')
    if (!form.entrustedProduction && names.length > 0) throw new Error('委托生产为否时，不可填写受托企业')
    payload.append('entrustedProduction', String(form.entrustedProduction)); payload.append('selfProduction', String(form.selfProduction)); payload.append('entrustedEnterprisesJson', JSON.stringify(names.map((enterpriseName) => ({ enterpriseName }))))
  }
  payload.append('file', selectedFile.value); return payload
}
const submit = async () => {
  saving.value = true
  try {
    if (!props.certificate) throw new Error('缺少当前注册证行，无法提交变更申请')
    await submitRegistrationCertificateChange(props.certificate.certificateId, buildChangePayload(), `DCC-REG-CERT-CHANGE-${generateUUID()}`)
    message.success('变更已提交审核'); dialogVisible.value = false; emit('saved')
  } catch (error) { message.error(resolveRegistrationCertificateUserMessage(error, '提交变更申请失败')); throw error } finally { saving.value = false }
}
const voidCertificate = async () => {
  saving.value = true
  try {
    if (!props.certificate || !currentDetail.value) throw new Error('注册证当前信息尚未加载完成，无法作废证书')
    await voidRegistrationCertificate(props.certificate.certificateId, { expectedRowVersion: currentDetail.value.rowVersion, approvalDate: form.approvalDate, voidReason: '页面提交的证书作废原因' }, `DCC-REG-CERT-VOID-${generateUUID()}`)
    message.success('证书作废已提交审核'); dialogVisible.value = false; emit('saved')
  } catch (error) { message.error(resolveRegistrationCertificateUserMessage(error, '提交证书作废失败')); throw error } finally { saving.value = false }
}
let detailLoadToken = 0

const resetForm = () => {
  detailLoadToken += 1
  form.approvalDate = ''
  form.changeTypes = []
  form.structuredValues = {}
  form.otherDescription = ''
  form.entrustedProduction = undefined
  form.selfProduction = undefined
  form.entrustedEnterpriseNames = ''
  fileList.value = []
  selectedFile.value = null
  currentDetail.value = undefined
  detailLoading.value = false
  detailLoadError.value = ''
}

const parseEntrustedEnterpriseNames = (raw?: string) => {
  if (!raw?.trim()) return ''
  const parsed = JSON.parse(raw) as unknown
  if (!Array.isArray(parsed)) throw new Error('当前受托企业数据格式无效')
  return parsed.map((item) => {
    if (!item || typeof item !== 'object' || !('enterpriseName' in item)) {
      throw new Error('当前受托企业数据缺少企业名称')
    }
    const name = String(item.enterpriseName ?? '').trim()
    if (!name) throw new Error('当前受托企业名称为空')
    return name
  }).join('\n')
}

const initializeCurrentValues = (detail: DccRegistrationCertificateDetailVO) => {
  form.structuredValues = {
    PRODUCT_NAME: detail.productName,
    MODEL_SPECIFICATION: detail.modelSpecification,
    STRUCTURE_COMPOSITION: detail.structureComposition,
    INTENDED_USE: detail.intendedUse,
    TECHNICAL_REQUIREMENTS: detail.technicalRequirements,
    REGISTRANT_NAME: detail.registrantName,
    RESIDENCE_ADDRESS: detail.residenceAddress,
    PRODUCTION_ADDRESS: detail.productionAddress
  }
  form.entrustedProduction = detail.entrustedProduction
  form.selfProduction = detail.selfProduction
  form.entrustedEnterpriseNames = parseEntrustedEnterpriseNames(detail.entrustedEnterprisesJson)
}

const loadCurrentDetail = async () => {
  if (!props.certificate) {
    detailLoadError.value = '缺少当前注册证行，无法加载注册证当前信息'
    return
  }
  const token = ++detailLoadToken
  detailLoading.value = true
  detailLoadError.value = ''
  currentDetail.value = undefined
  try {
    const detail = await getRegistrationCertificateDetail(props.certificate.certificateId)
    if (token !== detailLoadToken) return
    if (String(detail.certificateId) !== String(props.certificate.certificateId)) {
      throw new Error('注册证详情与当前选择不一致')
    }
    initializeCurrentValues(detail)
    currentDetail.value = detail
  } catch (error) {
    if (token !== detailLoadToken) return
    detailLoadError.value = resolveRegistrationCertificateUserMessage(error, '加载注册证当前信息失败')
  } finally {
    if (token === detailLoadToken) detailLoading.value = false
  }
}

watch(
  () => [props.modelValue, props.certificate?.certificateId] as const,
  ([visible]) => {
    if (!visible) return
    resetForm()
    void loadCurrentDetail()
  },
  { immediate: true }
)
</script>

<style lang="scss">
.registration-certificate-change-dialog.el-dialog { max-width: calc(100vw - 24px); }
.registration-certificate-change-dialog .el-dialog__body { max-height: calc(100vh - 180px); overflow-y: auto; padding: 24px 32px !important; }
.registration-certificate-change-dialog__form .el-row { row-gap: 4px; }
.registration-certificate-change-dialog__form .el-form-item { margin-bottom: 20px; }
.registration-certificate-change-dialog__form .el-form-item__label { height: auto; padding-right: 12px; margin-bottom: 0; color: #263247; font-weight: 600; line-height: 32px; white-space: nowrap; }
.registration-certificate-change-dialog__form .el-input, .registration-certificate-change-dialog__form .el-select, .registration-certificate-change-dialog__form .el-date-editor { width: 100%; }
@media (max-width: 720px) { .registration-certificate-change-dialog .el-dialog__body { padding: 20px 16px !important; } .registration-certificate-change-dialog__form .el-form-item__label { width: 100% !important; justify-content: flex-start; padding-right: 0; margin-bottom: 8px; line-height: 20px; } .registration-certificate-change-dialog__form .el-form-item__content { margin-left: 0 !important; } }
</style>
