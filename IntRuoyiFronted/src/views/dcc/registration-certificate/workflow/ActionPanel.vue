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
          <el-alert type="info" :closable="false" title="审批结果由审批中心待办处理，页面仅展示系统返回的正式结果。" />
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
            <el-descriptions-item label="申请状态">{{ formatRegistrationCertificateWorkflowStatus(accessStatus.requestStatus) }}</el-descriptions-item>
            <el-descriptions-item label="审批流程实例">{{ accessStatus.bpmProcessInstanceId || '未创建' }}</el-descriptions-item>
            <el-descriptions-item label="审批状态">{{ formatRegistrationCertificateWorkflowStatus(accessStatus.bpmBindingStatus) }}</el-descriptions-item>
            <el-descriptions-item label="申请用途">{{ accessStatus.purpose }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="accessStatus" :data="accessStatus.grants" row-key="grantId" class="registration-certificate-workflow__grants">
            <el-table-column label="授权 ID" prop="grantId" width="120" />
            <el-table-column label="文件 ID" prop="businessFileId" width="120" />
            <el-table-column label="授权状态" prop="status" width="120">
              <template #default="scope">
                {{ formatRegistrationCertificateWorkflowStatus(scope.row.status) }}
              </template>
            </el-table-column>
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
import {
  formatRegistrationCertificateWorkflowStatus,
  resolveRegistrationCertificateUserMessage
} from '../shared/state'
import {
  createRegistrationCertificateDraft,
  updateRegistrationCertificateDraft,
  deleteRegistrationCertificateDraft,
  formalizeRegistrationCertificate,
  voidRegistrationCertificateRenewalCandidate,
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
  initialAccessRequestType?: 'VIEW_OLD_CERTIFICATE' | 'DOWNLOAD_FILE'
  initialDownloadBusinessFileId?: number | string
  supportingDocumentId?: number | string
  initialAction?: 'draft' | 'formalize' | 'renewal' | 'supporting' | 'access' | 'approvalResult'
  readOnly?: boolean
  certificateStatus?: string
}>()

const activeAction = ref(props.initialAction || 'draft')
const submitting = ref(false)
const lastActionError = ref('')
const lastActionResult = ref('')
const accessRequestType = ref<'VIEW_OLD_CERTIFICATE' | 'DOWNLOAD_FILE'>(
  props.initialAccessRequestType || 'VIEW_OLD_CERTIFICATE'
)
const accessRequestId = ref<number | string>('')
const accessReason = ref('')
const accessStatus = ref<DccRegistrationCertificateAccessRequestStatusVO>()
const pendingVersionId = ref<number | string>()
const activeSupportingDocumentId = ref<number | string>(props.supportingDocumentId ?? '')
const selectedDownloadBusinessFileId = ref<number | string>(
  props.initialDownloadBusinessFileId ?? ''
)

const operationKeys = {
  draftCreate: ref(''),
  draftUpdate: ref(''),
  draftDelete: ref(''),
  formalize: ref(''),
  renewalUpload: ref(''),
  renewalVoid: ref(''),
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

const supportingForm = reactive({
  documentType: 'RENEWAL_ACCEPTANCE_RECEIPT',
  businessFileId: props.businessFileId || ''
})

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
    const selected = options.find((option) => String(option.businessFileId) === current)
    if (selected) {
      selectedDownloadBusinessFileId.value = selected.businessFileId
      return
    }
    selectedDownloadBusinessFileId.value = options[0]?.businessFileId ?? ''
  },
  { immediate: true }
)

watch(
  () => props.initialAccessRequestType,
  (value) => {
    if (value) {
      accessRequestType.value = value
    }
  }
)

watch(
  () => props.initialDownloadBusinessFileId,
  (value) => {
    if (value !== undefined && value !== null && value !== '') {
      selectedDownloadBusinessFileId.value = value
    }
  }
)

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
    await action()
    if (operation) resetIdempotencyKey(operation)
    lastActionResult.value = `${name}成功`
  } catch (error) {
    lastActionError.value = resolveRegistrationCertificateUserMessage(error, `${name}失败`)
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
