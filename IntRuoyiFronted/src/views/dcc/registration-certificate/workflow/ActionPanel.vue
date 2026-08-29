<template>
  <ContentWrap class="registration-certificate-workflow" data-testid="registration-certificate-workflow-actions">
    <template #header>
      <span>注册证业务操作</span>
    </template>
    <el-alert
      v-if="lastActionError"
      class="registration-certificate-workflow__alert"
      type="error"
      show-icon
      :closable="false"
      :title="lastActionError"
    />
    <el-alert
      v-if="lastActionResult"
      class="registration-certificate-workflow__alert"
      type="success"
      show-icon
      :closable="false"
      :title="lastActionResult"
    />

    <el-tabs v-model="activeAction" class="registration-certificate-workflow__tabs">
      <el-tab-pane v-if="!props.readOnly && (!props.certificateStatus || props.certificateStatus === 'DRAFT')" label="草稿维护" name="draft">
        <div data-testid="registration-certificate-draft-action" class="registration-certificate-workflow__panel">
          <el-form :model="draftForm" inline data-testid="registration-certificate-draft-form">
            <el-form-item label="所属公司"><el-input v-model="draftForm.ownerCompanyId" aria-label="公司 ID" /></el-form-item>
            <el-form-item label="产品"><el-input v-model="draftForm.productMasterId" aria-label="产品 ID" /></el-form-item>
            <el-form-item label="项目代码"><el-input v-model="draftForm.projectCodeId" aria-label="项目代码 ID（可选）" /></el-form-item>
            <el-form-item label="首次获证"><el-date-picker v-model="draftForm.firstObtainedDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
            <el-form-item label="注册证号"><el-input v-model="draftForm.certificateNo" /></el-form-item>
            <el-form-item label="生效日期"><el-date-picker v-model="draftForm.effectiveDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
            <el-form-item label="有效期至"><el-date-picker v-model="draftForm.expiryDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
            <el-form-item label="类别"><el-input v-model="draftForm.classification" /></el-form-item>
            <el-form-item label="备注">
              <el-input v-model="draftForm.remark" maxlength="1024" show-word-limit />
            </el-form-item>
          </el-form>
          <el-button type="primary" :loading="submitting" @click="handleCreateDraft">创建草稿</el-button>
          <el-button :loading="submitting" @click="handleUpdateDraft">更新草稿</el-button>
          <el-button type="danger" plain :loading="submitting" @click="handleDeleteDraft">删除草稿</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="!props.readOnly && (!props.certificateStatus || props.certificateStatus === 'DRAFT')" label="正式化" name="formalize">
        <div data-testid="registration-certificate-formalize-action" class="registration-certificate-workflow__panel">
          <el-button type="primary" :loading="submitting" @click="handleFormalize">正式化</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="!props.readOnly && props.certificateStatus !== 'DRAFT'" label="延续" name="renewal">
        <div data-testid="registration-certificate-renewal-action" class="registration-certificate-workflow__panel">
          <el-alert type="info" :closable="false" title="延续注册证请从注册证列表对应行点击“延续”，填写三项日期并上传延续注册证文件后进入注册部经理审批。" />
          <el-button type="warning" plain :loading="submitting" @click="handleVoidRenewal">作废延续候选</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="!props.readOnly && props.certificateStatus !== 'DRAFT'" label="变更/作废" name="change">
        <div data-testid="registration-certificate-change-action" class="registration-certificate-workflow__panel">
          <el-form data-testid="registration-certificate-change-form" label-width="118px">
            <el-form-item label="批准日期">
              <el-date-picker v-model="changeForm.approvalDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="变更内容">
              <el-select
                v-model="changeForm.changeTypes"
                multiple
                collapse-tags
                data-change-type-values="PRODUCT_NAME,MODEL_SPECIFICATION,STRUCTURE_COMPOSITION,INTENDED_USE,TECHNICAL_REQUIREMENTS,REGISTRANT_NAME,RESIDENCE_ADDRESS,PRODUCTION_ADDRESS,OTHER_CONTENT"
              >
                <el-option v-for="item in changeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item
              v-for="item in selectedStructuredChangeTypes"
              :key="item.value"
              :label="item.label"
            >
              <el-input
                v-model="changeForm.structuredValues[item.value]"
                :placeholder="item.placeholder"
              />
            </el-form-item>
            <el-form-item v-if="changeForm.changeTypes.includes('OTHER_CONTENT')" label="其他说明">
              <el-input v-model="changeForm.otherDescription" maxlength="4096" />
            </el-form-item>
            <template v-if="changeForm.changeTypes.includes('PRODUCTION_ADDRESS')">
              <el-form-item label="是否委托生产">
                <el-select v-model="changeForm.entrustedProduction">
                  <el-option label="是" :value="true" />
                  <el-option label="否" :value="false" />
                </el-select>
              </el-form-item>
              <el-form-item label="是否自行生产">
                <el-select v-model="changeForm.selfProduction">
                  <el-option label="是" :value="true" />
                  <el-option label="否" :value="false" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="changeForm.entrustedProduction" label="受托企业">
                <el-input
                  v-model="changeForm.entrustedEnterpriseNames"
                  placeholder="受托企业：请输入变更后的受托企业，多个请换行"
                  type="textarea"
                />
              </el-form-item>
            </template>
            <el-form-item label="变更批件文件">
              <el-upload
                v-model:file-list="changeFileList"
                action="#"
                :auto-upload="false"
                :limit="1"
                :on-change="handleChangeFileChange"
                :on-remove="handleChangeFileRemove"
                accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
                data-testid="registration-certificate-change-approval-file"
              >
                <el-button>
                  <Icon icon="ep:upload" class="mr-5px" />选择文件
                </el-button>
              </el-upload>
            </el-form-item>
          </el-form>
          <el-button type="primary" :loading="submitting" @click="handleSubmitChange">提交变更</el-button>
          <el-button type="danger" plain :loading="submitting" @click="handleVoidCertificate">作废证书</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="!props.readOnly && props.certificateStatus !== 'DRAFT'" label="支持文件" name="supporting">
        <div data-testid="registration-certificate-supporting-document-action" class="registration-certificate-workflow__panel">
          <el-form inline data-testid="registration-certificate-supporting-document-form">
            <el-form-item label="材料类型"><el-select v-model="supportingForm.documentType"><el-option label="延续受理单" value="RENEWAL_ACCEPTANCE_RECEIPT" /><el-option label="立卷发补单" value="RENEWAL_SUPPLEMENT_NOTICE" /></el-select></el-form-item>
            <el-form-item label="支持文件业务文件 ID"><el-input v-model="supportingForm.businessFileId" aria-label="文件中心返回的业务文件 ID" /></el-form-item>
          </el-form>
          <el-button type="primary" :loading="submitting" @click="handleUploadSupportingDocument">上传支持文件</el-button>
          <el-tag type="success">上传后直接生效</el-tag>
        </div>
      </el-tab-pane>
      <el-tab-pane label="访问申请" name="access">
        <div data-testid="registration-certificate-access-request-action" class="registration-certificate-workflow__panel">
          <el-radio-group v-model="accessRequestType" aria-label="访问申请类型">
            <el-radio-button value="VIEW_OLD_CERTIFICATE">查看旧证</el-radio-button>
            <el-radio-button value="DOWNLOAD_FILE">下载文件</el-radio-button>
          </el-radio-group>
          <template v-if="accessRequestType === 'DOWNLOAD_FILE'">
            <el-select
              v-model="selectedDownloadBusinessFileId"
              class="registration-certificate-workflow__file-select"
              data-testid="registration-certificate-download-file-select"
              placeholder="选择下载文件"
            >
              <el-option
                v-for="file in downloadFileOptions"
                :key="String(file.businessFileId)"
                :label="file.label"
                :value="file.businessFileId"
              />
            </el-select>
            <el-alert
              :type="hasDownloadFacts ? 'info' : 'warning'"
              :title="hasDownloadFacts ? '下载所需正式事实已由系统带出' : '当前档案缺少项目代码或可下载文件，下载已锁定'"
              :closable="false"
            />
          </template>
          <el-button
            type="primary"
            :disabled="accessRequestType === 'DOWNLOAD_FILE' && !hasDownloadFacts"
            :loading="submitting"
            @click="handleSubmitAccessRequest"
          >提交访问申请</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="审批结果" name="approvalResult">
        <div data-testid="registration-certificate-approval-result-action" class="registration-certificate-workflow__panel">
          <el-alert type="info" :closable="false" title="审批结果由 BPM Native 待办处理，页面只展示后端返回结果，不本地伪造通过。" />
          <el-input v-model="accessReason" data-field="accessReason" aria-label="撤回或撤销原因" />
          <el-button :disabled="!accessRequestId" :loading="submitting" @click="handleRefreshAccessStatus">刷新申请状态</el-button>
          <el-button
            v-if="accessStatus && ['SUBMITTED', 'BPM_BOUND'].includes(accessStatus.requestStatus)"
            type="warning"
            plain
            :loading="submitting"
            @click="handleWithdrawAccessRequest"
          >撤回申请</el-button>
          <el-descriptions v-if="accessStatus" :column="2" border class="registration-certificate-workflow__status">
            <el-descriptions-item label="申请状态">{{ accessStatus.requestStatus }}</el-descriptions-item>
            <el-descriptions-item label="BPM 实例">{{ accessStatus.bpmProcessInstanceId || '未创建' }}</el-descriptions-item>
            <el-descriptions-item label="BPM 状态">{{ accessStatus.bpmBindingStatus || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="申请用途">{{ accessStatus.purpose }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="accessStatus" :data="accessStatus.grants" row-key="grantId" class="registration-certificate-workflow__grants">
            <el-table-column label="授权 ID" prop="grantId" width="120" />
            <el-table-column label="文件 ID" prop="businessFileId" width="120" />
            <el-table-column label="授权状态" prop="status" width="120" />
            <el-table-column label="操作" width="260">
              <template #default="scope">
                <el-button
                  v-if="scope.row.status === 'ACTIVE' && scope.row.businessFileId"
                  link
                  type="primary"
                  :loading="submitting"
                  @click="handleDownloadGrant(scope.row.businessFileId)"
                >下载</el-button>
                <el-button
                  v-if="scope.row.status === 'ACTIVE'"
                  link
                  type="danger"
                  :loading="submitting"
                  @click="handleRevokeGrant(scope.row.grantId)"
                >撤销授权</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { UploadFile, UploadFiles, UploadUserFile } from 'element-plus'
import {
  createRegistrationCertificateDraft,
  updateRegistrationCertificateDraft,
  deleteRegistrationCertificateDraft,
  formalizeRegistrationCertificate,
  voidRegistrationCertificateRenewalCandidate,
  submitRegistrationCertificateChange,
  voidRegistrationCertificate,
  uploadRegistrationCertificateSupportingDocument,
  submitRegistrationCertificateAccessRequest,
  getRegistrationCertificateAccessRequestStatus,
  withdrawRegistrationCertificateAccessRequest,
  revokeRegistrationCertificateGrant,
  downloadRegistrationCertificateFile,
  type DccRegistrationCertificateAccessRequestStatusVO,
  type DccRegistrationCertificateDraftReqVO
} from '@/api/dcc/registrationCertificate'
import { downloadByData } from '@/utils/filt'
import { generateUUID } from '@/utils'

type DownloadableFileOption = {
  businessFileId: number | string
  fileKind: string
  label: string
}

const props = defineProps<{
  certificateId?: number | string
  versionId?: number | string
  snapshotRevision?: number
  rowVersion?: number
  projectCodeId?: number | string
  businessFileId?: number | string
  downloadableFiles?: DownloadableFileOption[]
  supportingDocumentId?: number | string
  initialAction?: 'draft' | 'formalize' | 'renewal' | 'change' | 'supporting' | 'access' | 'approvalResult'
  readOnly?: boolean
  certificateStatus?: string
}>()

const activeAction = ref(props.initialAction || 'draft')
const submitting = ref(false)
const lastActionError = ref('')
const lastActionResult = ref('')
const accessRequestType = ref<'VIEW_OLD_CERTIFICATE' | 'DOWNLOAD_FILE'>('VIEW_OLD_CERTIFICATE')
const accessRequestId = ref<number | string>('')
const accessReason = ref('')
const accessStatus = ref<DccRegistrationCertificateAccessRequestStatusVO>()
const pendingVersionId = ref<number | string>()
const activeSupportingDocumentId = ref<number | string>(props.supportingDocumentId ?? '')
const selectedDownloadBusinessFileId = ref<number | string>('')

const operationKeys = {
  draftCreate: ref(''),
  draftUpdate: ref(''),
  draftDelete: ref(''),
  formalize: ref(''),
  renewalUpload: ref(''),
  renewalVoid: ref(''),
  changeSubmit: ref(''),
  certificateVoid: ref(''),
  supportingUpload: ref(''),
  accessSubmit: ref(''),
  downloadGrant: ref('')
}

type IdempotencyOperation = keyof typeof operationKeys

const draftForm = reactive<DccRegistrationCertificateDraftReqVO>({
  ownerCompanyId: '',
  productMasterId: '',
  projectCodeId: '',
  firstObtainedDate: '',
  certificateNo: '',
  effectiveDate: '',
  expiryDate: '',
  classification: '',
  remark: ''
})

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

const changeTypeOptions = [
  ...structuredChangeTypeOptions,
  { label: '其他内容', value: 'OTHER_CONTENT' }
] as const

type StructuredChangeType = (typeof structuredChangeTypeOptions)[number]['value']
type ChangeType = StructuredChangeType | 'OTHER_CONTENT'

const changeForm = reactive({
  approvalDate: '',
  changeTypes: [] as ChangeType[],
  structuredValues: {} as Partial<Record<StructuredChangeType, string>>,
  otherDescription: '',
  entrustedProduction: undefined as boolean | undefined,
  selfProduction: undefined as boolean | undefined,
  entrustedEnterpriseNames: ''
})
const changeFileList = ref<UploadUserFile[]>([])
const selectedChangeFile = ref<File | null>(null)
const supportingForm = reactive({
  documentType: 'RENEWAL_ACCEPTANCE_RECEIPT',
  businessFileId: props.businessFileId || ''
})

const selectedStructuredChangeTypes = computed(() =>
  structuredChangeTypeOptions.filter((item) => changeForm.changeTypes.includes(item.value))
)

const downloadFileOptions = computed<DownloadableFileOption[]>(() => {
  return (props.downloadableFiles ?? []).filter((file) =>
    Boolean(file.businessFileId) && Boolean(file.fileKind)
  )
})

const hasDownloadFacts = computed(() =>
  Boolean(props.projectCodeId && selectedDownloadBusinessFileId.value)
)

watch(
  downloadFileOptions,
  (options) => {
    const current = String(selectedDownloadBusinessFileId.value || '')
    if (options.some((option) => String(option.businessFileId) === current)) {
      return
    }
    selectedDownloadBusinessFileId.value = options[0]?.businessFileId ?? ''
  },
  { immediate: true }
)

const handleChangeFileChange = (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  selectedChangeFile.value = uploadFile.raw ?? null
  changeFileList.value = uploadFiles.slice(-1) as UploadUserFile[]
}

const handleChangeFileRemove = () => {
  selectedChangeFile.value = null
  changeFileList.value = []
}

const draftPayload = (): DccRegistrationCertificateDraftReqVO => ({
  ...draftForm,
  projectCodeId: draftForm.projectCodeId || undefined,
  remark: (draftForm.remark ?? '').trim() || undefined
})

const getOrCreateIdempotencyKey = (operation: IdempotencyOperation) => {
  const keyRef = operationKeys[operation]
  if (!keyRef.value) {
    keyRef.value = `DCC-REG-CERT-${operation.toUpperCase()}-${generateUUID()}`
  }
  return keyRef.value
}

const resetIdempotencyKey = (operation: IdempotencyOperation) => {
  operationKeys[operation].value = ''
}

const requireCertificateId = () => {
  if (!props.certificateId) {
    throw new Error('缺少注册证主档 ID，无法执行该操作。')
  }
  return props.certificateId
}

const requireVersionId = () => {
  if (!props.versionId) {
    throw new Error('缺少注册证版本 ID，无法执行该操作。')
  }
  return props.versionId
}

const requirePendingVersionId = () => {
  if (!pendingVersionId.value) {
    throw new Error('当前没有系统生成的延续候选，无法执行作废操作。')
  }
  return pendingVersionId.value
}

const requireBusinessFileId = () => {
  return props.businessFileId
}

const requireProjectCodeId = () => {
  if (!props.projectCodeId) {
    throw new Error('缺少项目代码，无法提交文件下载申请。')
  }
  return props.projectCodeId
}

const requireSelectedDownloadBusinessFileId = () => {
  if (!selectedDownloadBusinessFileId.value) {
    throw new Error('请选择需要下载的注册证业务文件。')
  }
  return selectedDownloadBusinessFileId.value
}

const requireAccessRequestId = () => {
  if (!accessRequestId.value) {
    throw new Error('缺少访问申请 ID，无法读取审批状态。')
  }
  return accessRequestId.value
}

const requireAccessReason = () => {
  if (!accessReason.value.trim()) {
    throw new Error('请填写撤回或撤销原因。')
  }
  return accessReason.value.trim()
}

const requireRowVersion = () => {
  if (!props.rowVersion || props.rowVersion <= 0) {
    throw new Error('缺少当前行版本，无法执行该操作。')
  }
  return props.rowVersion
}

const requireSnapshotRevision = () => {
  if (!props.snapshotRevision || props.snapshotRevision <= 0) {
    throw new Error('缺少快照版本，无法执行该操作。')
  }
  return props.snapshotRevision
}

const runAction = async (
  name: string,
  action: () => Promise<unknown>,
  operation?: IdempotencyOperation
) => {
  submitting.value = true
  lastActionError.value = ''
  lastActionResult.value = ''
  try {
    const result = await action()
    if (operation) resetIdempotencyKey(operation)
    lastActionResult.value = `${name}已提交，后端结果：${String(result ?? '')}`
  } catch (error) {
    lastActionError.value = error instanceof Error ? error.message : String(error || `${name}失败`)
    throw error
  } finally {
    submitting.value = false
  }
}

const handleCreateDraft = () => runAction('创建草稿', () =>
  createRegistrationCertificateDraft(draftPayload(), getOrCreateIdempotencyKey('draftCreate')), 'draftCreate')

const handleUpdateDraft = () => runAction('更新草稿', () =>
  updateRegistrationCertificateDraft(requireCertificateId(), {
    ...draftPayload(),
    expectedRowVersion: requireRowVersion(),
    expectedSnapshotRevision: requireSnapshotRevision()
  }, getOrCreateIdempotencyKey('draftUpdate')), 'draftUpdate')

const handleDeleteDraft = () => runAction('删除草稿', () =>
  deleteRegistrationCertificateDraft(
    requireCertificateId(),
    requireRowVersion(),
    requireSnapshotRevision(),
    getOrCreateIdempotencyKey('draftDelete')
  ), 'draftDelete')

const handleFormalize = () => runAction('正式化', () =>
  formalizeRegistrationCertificate(requireCertificateId(), {
    expectedRowVersion: requireRowVersion(),
    expectedSnapshotRevision: requireSnapshotRevision(),
    businessFileId: requireBusinessFileId()
  }, getOrCreateIdempotencyKey('formalize')), 'formalize')

const handleVoidRenewal = () => runAction('作废延续候选', () =>
  voidRegistrationCertificateRenewalCandidate(requireCertificateId(), requirePendingVersionId(), {
    expectedRowVersion: requireRowVersion(),
    voidReason: '页面提交的延续候选作废原因'
  }, getOrCreateIdempotencyKey('renewalVoid')), 'renewalVoid')

const requireChangeApprovalDate = () => {
  if (!changeForm.approvalDate) {
    throw new Error('请选择变更批件批准日期。')
  }
  return changeForm.approvalDate
}

const normalizeText = (value?: string) => (value ?? '').trim()

const appendStructuredChangeValues = (payload: FormData) => {
  for (const item of structuredChangeTypeOptions) {
    if (!changeForm.changeTypes.includes(item.value)) {
      continue
    }
    const nextValue = normalizeText(changeForm.structuredValues[item.value])
    if (!nextValue) {
      throw new Error(`请填写${item.placeholder}。`)
    }
    payload.append(`structuredValues[${item.value}]`, nextValue)
  }
}

const requireOtherDescription = () => {
  if (!changeForm.changeTypes.includes('OTHER_CONTENT')) {
    return
  }
  const description = normalizeText(changeForm.otherDescription)
  if (!description) {
    throw new Error('请填写其他内容说明。')
  }
  return description
}

const requireProductionRelation = () => {
  if (!changeForm.changeTypes.includes('PRODUCTION_ADDRESS')) {
    return
  }
  if (changeForm.entrustedProduction === undefined || changeForm.selfProduction === undefined) {
    throw new Error('请选择是否委托生产和是否自行生产。')
  }
  if (!changeForm.entrustedProduction && !changeForm.selfProduction) {
    throw new Error('委托生产和自行生产不可同时选择否。')
  }
  const entrustedNames = changeForm.entrustedEnterpriseNames
    .split(/\r?\n|[,，;；]/)
    .map((value) => value.trim())
    .filter(Boolean)
  if (changeForm.entrustedProduction && entrustedNames.length === 0) {
    throw new Error('委托生产为是时，请填写受托企业。')
  }
  if (!changeForm.entrustedProduction && entrustedNames.length > 0) {
    throw new Error('委托生产为否时，不可填写受托企业。')
  }
  return {
    entrustedProduction: changeForm.entrustedProduction,
    selfProduction: changeForm.selfProduction,
    entrustedEnterprisesJson: JSON.stringify(
      entrustedNames.map((enterpriseName) => ({ enterpriseName }))
    )
  }
}

const buildChangePayload = () => {
  if (changeForm.changeTypes.length === 0) {
    throw new Error('请选择至少一项变更内容。')
  }
  if (!selectedChangeFile.value) {
    throw new Error('请先选择变更批件文件。')
  }
  const payload = new FormData()
  payload.append('expectedRowVersion', String(requireRowVersion()))
  payload.append('approvalDate', requireChangeApprovalDate())
  appendStructuredChangeValues(payload)
  const otherDescription = requireOtherDescription()
  if (otherDescription) {
    payload.append('otherDescription', otherDescription)
  }
  const productionRelation = requireProductionRelation()
  if (productionRelation) {
    payload.append('entrustedProduction', String(productionRelation.entrustedProduction))
    payload.append('selfProduction', String(productionRelation.selfProduction))
    payload.append('entrustedEnterprisesJson', productionRelation.entrustedEnterprisesJson)
  }
  payload.append('file', selectedChangeFile.value)
  return payload
}

const handleSubmitChange = () => runAction('提交变更', () =>
  submitRegistrationCertificateChange(
    requireCertificateId(),
    buildChangePayload(),
    getOrCreateIdempotencyKey('changeSubmit')
  ), 'changeSubmit')

const handleVoidCertificate = () => runAction('作废证书', () =>
  voidRegistrationCertificate(requireCertificateId(), {
    expectedRowVersion: requireRowVersion(),
    approvalDate: changeForm.approvalDate,
    voidReason: '页面提交的证书作废原因'
  }, getOrCreateIdempotencyKey('certificateVoid')), 'certificateVoid')

const handleUploadSupportingDocument = () => runAction('上传支持文件', async () => {
  const result = await uploadRegistrationCertificateSupportingDocument(requireCertificateId(), {
    versionId: requireVersionId(),
    businessFileId: supportingForm.businessFileId || requireBusinessFileId(),
    documentType: supportingForm.documentType
  }, getOrCreateIdempotencyKey('supportingUpload'))
  activeSupportingDocumentId.value = result.supportingDocumentId
  return result
}, 'supportingUpload')

const handleSubmitAccessRequest = () => runAction('提交访问申请', async () => {
  const requestType = accessRequestType.value
  const payload = requestType === 'DOWNLOAD_FILE'
    ? {
        certificateId: requireCertificateId(),
        requestType,
        purpose: '页面提交的注册证文件下载申请',
        projectCodeId: requireProjectCodeId(),
        businessFileIds: [requireSelectedDownloadBusinessFileId()]
      }
    : {
        certificateId: requireCertificateId(),
        requestType,
        purpose: '页面提交的旧注册证查看申请'
      }
  const requestId = await submitRegistrationCertificateAccessRequest(
    payload,
    getOrCreateIdempotencyKey('accessSubmit')
  )
  accessRequestId.value = requestId
  await handleRefreshAccessStatus()
  return requestId
}, 'accessSubmit')

const handleRefreshAccessStatus = () => runAction('刷新申请状态', async () => {
  const status = await getRegistrationCertificateAccessRequestStatus(requireAccessRequestId())
  accessStatus.value = status
  return status.requestStatus
})

const handleWithdrawAccessRequest = () => runAction('撤回访问申请', async () => {
  const result = await withdrawRegistrationCertificateAccessRequest(
    requireAccessRequestId(),
    { reason: requireAccessReason() }
  )
  await handleRefreshAccessStatus()
  return result.status
})

const handleRevokeGrant = (grantId: number | string) => runAction('撤销访问授权', async () => {
  const result = await revokeRegistrationCertificateGrant(grantId, { reason: requireAccessReason() })
  await handleRefreshAccessStatus()
  return result
})

const handleDownloadGrant = (businessFileId: number | string) => runAction('下载注册证文件', async () => {
  const result = await downloadRegistrationCertificateFile(
    businessFileId,
    getOrCreateIdempotencyKey('downloadGrant')
  )
  downloadByData(result.blob, result.fileName, result.blob.type || 'application/octet-stream')
  return result.fileName
}, 'downloadGrant')
</script>

<style scoped>
.registration-certificate-workflow__alert {
  margin-bottom: 12px;
}

.registration-certificate-workflow__panel {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.registration-certificate-workflow__panel :deep(.el-input) {
  max-width: 320px;
}

.registration-certificate-workflow__file-select {
  width: 240px;
}

.registration-certificate-workflow__status,
.registration-certificate-workflow__grants {
  width: 100%;
}
</style>
