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
      <el-tab-pane label="草稿维护" name="draft">
        <div data-testid="registration-certificate-draft-action" class="registration-certificate-workflow__panel">
          <el-input v-model="draftIdempotencyKey" data-field="idempotencyKey" aria-label="草稿幂等键" />
          <el-button type="primary" :loading="submitting" @click="handleCreateDraft">创建草稿</el-button>
          <el-button :loading="submitting" @click="handleUpdateDraft">更新草稿</el-button>
          <el-button type="danger" plain :loading="submitting" @click="handleDeleteDraft">删除草稿</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="正式化" name="formalize">
        <div data-testid="registration-certificate-formalize-action" class="registration-certificate-workflow__panel">
          <el-input v-model="formalizeIdempotencyKey" data-field="idempotencyKey" aria-label="正式化幂等键" />
          <el-button type="primary" :loading="submitting" @click="handleFormalize">正式化</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="延续" name="renewal">
        <div data-testid="registration-certificate-renewal-action" class="registration-certificate-workflow__panel">
          <el-input v-model="renewalIdempotencyKey" data-field="idempotencyKey" aria-label="延续幂等键" />
          <el-button type="primary" :loading="submitting" @click="handleUploadRenewal">上传延续候选</el-button>
          <el-button type="warning" plain :loading="submitting" @click="handleVoidRenewal">作废延续候选</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="变更/作废" name="change">
        <div data-testid="registration-certificate-change-action" class="registration-certificate-workflow__panel">
          <el-input v-model="changeIdempotencyKey" data-field="idempotencyKey" aria-label="变更幂等键" />
          <el-button type="primary" :loading="submitting" @click="handleSubmitChange">提交变更</el-button>
          <el-button type="danger" plain :loading="submitting" @click="handleVoidCertificate">作废证书</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="支持文件" name="supporting">
        <div data-testid="registration-certificate-supporting-document-action" class="registration-certificate-workflow__panel">
          <el-input v-model="supportingIdempotencyKey" data-field="idempotencyKey" aria-label="支持文件幂等键" />
          <el-button type="primary" :loading="submitting" @click="handleUploadSupportingDocument">上传支持文件</el-button>
          <el-button :loading="submitting" @click="handleConfirmSupportingDocument">确认支持文件</el-button>
          <el-button type="warning" plain :loading="submitting" @click="handleRejectSupportingDocument">驳回支持文件</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="访问申请" name="access">
        <div data-testid="registration-certificate-access-request-action" class="registration-certificate-workflow__panel">
          <el-radio-group v-model="accessRequestType" aria-label="访问申请类型">
            <el-radio-button value="VIEW_OLD_CERTIFICATE">查看旧证</el-radio-button>
            <el-radio-button value="DOWNLOAD_FILE">下载文件</el-radio-button>
          </el-radio-group>
          <el-input
            v-if="accessRequestType === 'DOWNLOAD_FILE'"
            v-model="accessProjectCodeId"
            data-field="accessProjectCodeId"
            aria-label="下载项目代码 ID"
          />
          <el-input
            v-if="accessRequestType === 'DOWNLOAD_FILE'"
            :model-value="String(props.businessFileId || '')"
            aria-label="注册证业务文件 ID"
            readonly
          />
          <el-input v-model="accessIdempotencyKey" data-field="idempotencyKey" aria-label="访问申请幂等键" />
          <el-button type="primary" :loading="submitting" @click="handleSubmitAccessRequest">提交访问申请</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="审批结果" name="approvalResult">
        <div data-testid="registration-certificate-approval-result-action" class="registration-certificate-workflow__panel">
          <el-alert type="info" :closable="false" title="审批结果由 BPM Native 待办处理，页面只展示后端返回结果，不本地伪造通过。" />
          <el-input v-model="accessRequestId" data-field="accessRequestId" aria-label="访问申请 ID" />
          <el-input v-model="accessReason" data-field="accessReason" aria-label="撤回或撤销原因" />
          <el-input v-model="downloadAttemptKey" data-field="downloadAttemptKey" aria-label="下载尝试键" />
          <el-button :loading="submitting" @click="handleRefreshAccessStatus">刷新申请状态</el-button>
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
import { reactive, ref } from 'vue'
import {
  createRegistrationCertificateDraft,
  updateRegistrationCertificateDraft,
  deleteRegistrationCertificateDraft,
  formalizeRegistrationCertificate,
  uploadRegistrationCertificateRenewalCandidate,
  voidRegistrationCertificateRenewalCandidate,
  submitRegistrationCertificateChange,
  voidRegistrationCertificate,
  uploadRegistrationCertificateSupportingDocument,
  confirmRegistrationCertificateSupportingDocument,
  rejectRegistrationCertificateSupportingDocument,
  submitRegistrationCertificateAccessRequest,
  getRegistrationCertificateAccessRequestStatus,
  withdrawRegistrationCertificateAccessRequest,
  revokeRegistrationCertificateGrant,
  downloadRegistrationCertificateFile,
  type DccRegistrationCertificateAccessRequestStatusVO,
  type DccRegistrationCertificateDraftReqVO
} from '@/api/dcc/registrationCertificate'
import { downloadByData } from '@/utils/filt'

const props = defineProps<{
  certificateId?: number | string
  versionId?: number | string
  snapshotRevision?: number
  rowVersion?: number
  businessFileId?: number | string
  supportingDocumentId?: number | string
}>()

const activeAction = ref('draft')
const submitting = ref(false)
const lastActionError = ref('')
const lastActionResult = ref('')
const draftIdempotencyKey = ref('')
const formalizeIdempotencyKey = ref('')
const renewalIdempotencyKey = ref('')
const changeIdempotencyKey = ref('')
const supportingIdempotencyKey = ref('')
const accessRequestType = ref<'VIEW_OLD_CERTIFICATE' | 'DOWNLOAD_FILE'>('VIEW_OLD_CERTIFICATE')
const accessProjectCodeId = ref<number | string>('')
const accessIdempotencyKey = ref('')
const accessRequestId = ref<number | string>('')
const accessReason = ref('')
const downloadAttemptKey = ref('')
const accessStatus = ref<DccRegistrationCertificateAccessRequestStatusVO>()

const draftForm = reactive<DccRegistrationCertificateDraftReqVO>({
  ownerCompanyId: '',
  productMasterId: '',
  firstObtainedDate: '',
  certificateNo: '',
  approvalDate: '',
  effectiveDate: '',
  expiryDate: '',
  classification: '',
  registrantName: '',
  modelSpecification: '',
  structureComposition: '',
  intendedUse: '',
  technicalRequirements: '',
  residenceAddress: '',
  productionAddress: '',
  entrustedProduction: false,
  selfProduction: true,
  entrustedEnterpriseIds: []
})

const requireIdempotencyKey = (value: string) => {
  if (!value || !value.trim()) {
    throw new Error('请填写稳定幂等键后再提交。')
  }
  return value.trim()
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

const requireBusinessFileId = () => {
  if (!props.businessFileId) {
    throw new Error('缺少注册证业务文件 ID，无法执行该操作。')
  }
  return props.businessFileId
}

const requireSupportingDocumentId = () => {
  if (!props.supportingDocumentId) {
    throw new Error('缺少注册证支持文件 ID，无法执行该操作。')
  }
  return props.supportingDocumentId
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

const requireAccessProjectCodeId = () => {
  const value = String(accessProjectCodeId.value || '').trim()
  if (!/^[1-9]\d*$/.test(value)) {
    throw new Error('下载申请必须填写有效的项目代码 ID。')
  }
  return value
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

const runAction = async (name: string, action: () => Promise<unknown>) => {
  submitting.value = true
  lastActionError.value = ''
  lastActionResult.value = ''
  try {
    const result = await action()
    lastActionResult.value = `${name}已提交，后端结果：${String(result ?? '')}`
  } catch (error) {
    lastActionError.value = error instanceof Error ? error.message : String(error || `${name}失败`)
    throw error
  } finally {
    submitting.value = false
  }
}

const handleCreateDraft = () => runAction('创建草稿', () =>
  createRegistrationCertificateDraft(draftForm, requireIdempotencyKey(draftIdempotencyKey.value)))

const handleUpdateDraft = () => runAction('更新草稿', () =>
  updateRegistrationCertificateDraft(requireCertificateId(), {
    ...draftForm,
    expectedRowVersion: requireRowVersion(),
    expectedSnapshotRevision: requireSnapshotRevision()
  }, requireIdempotencyKey(draftIdempotencyKey.value)))

const handleDeleteDraft = () => runAction('删除草稿', () =>
  deleteRegistrationCertificateDraft(
    requireCertificateId(),
    requireRowVersion(),
    requireSnapshotRevision(),
    requireIdempotencyKey(draftIdempotencyKey.value)
  ))

const handleFormalize = () => runAction('正式化', () =>
  formalizeRegistrationCertificate(requireCertificateId(), {
    expectedRowVersion: requireRowVersion(),
    expectedSnapshotRevision: requireSnapshotRevision(),
    businessFileId: requireBusinessFileId()
  }, requireIdempotencyKey(formalizeIdempotencyKey.value)))

const handleUploadRenewal = () => runAction('上传延续候选', () =>
  uploadRegistrationCertificateRenewalCandidate(requireCertificateId(), {
    expectedRowVersion: requireRowVersion(),
    currentVersionId: requireVersionId(),
    businessFileId: requireBusinessFileId(),
    categoryChanged: false,
    approvalDate: draftForm.approvalDate,
    effectiveDate: draftForm.effectiveDate,
    expiryDate: draftForm.expiryDate
  }, requireIdempotencyKey(renewalIdempotencyKey.value)))

const handleVoidRenewal = () => runAction('作废延续候选', () =>
  voidRegistrationCertificateRenewalCandidate(requireCertificateId(), requireVersionId(), {
    expectedRowVersion: requireRowVersion(),
    voidReason: '页面提交的延续候选作废原因'
  }, requireIdempotencyKey(renewalIdempotencyKey.value)))

const handleSubmitChange = () => runAction('提交变更', () =>
  submitRegistrationCertificateChange(requireCertificateId(), {
    expectedRowVersion: requireRowVersion(),
    approvalDate: draftForm.approvalDate,
    otherDescription: '页面提交的其他内容变更说明'
  }, requireIdempotencyKey(changeIdempotencyKey.value)))

const handleVoidCertificate = () => runAction('作废证书', () =>
  voidRegistrationCertificate(requireCertificateId(), {
    expectedRowVersion: requireRowVersion(),
    approvalDate: draftForm.approvalDate,
    voidReason: '页面提交的证书作废原因'
  }, requireIdempotencyKey(changeIdempotencyKey.value)))

const handleUploadSupportingDocument = () => runAction('上传支持文件', () =>
  uploadRegistrationCertificateSupportingDocument(requireCertificateId(), {
    versionId: requireVersionId(),
    businessFileId: requireBusinessFileId(),
    documentType: 'CHANGE_APPROVAL'
  }, requireIdempotencyKey(supportingIdempotencyKey.value)))

const handleConfirmSupportingDocument = () => runAction('确认支持文件', () =>
  confirmRegistrationCertificateSupportingDocument(requireCertificateId(), requireSupportingDocumentId(), {
    versionId: requireVersionId(),
    businessFileId: requireBusinessFileId(),
    expectedRowVersion: requireRowVersion(),
    documentType: 'CHANGE_APPROVAL'
  }, requireIdempotencyKey(supportingIdempotencyKey.value)))

const handleRejectSupportingDocument = () => runAction('驳回支持文件', () =>
  rejectRegistrationCertificateSupportingDocument(requireCertificateId(), requireSupportingDocumentId(), {
    versionId: requireVersionId(),
    businessFileId: requireBusinessFileId(),
    expectedRowVersion: requireRowVersion(),
    documentType: 'CHANGE_APPROVAL',
    rejectReason: '页面提交的支持文件驳回原因'
  }, requireIdempotencyKey(supportingIdempotencyKey.value)))

const handleSubmitAccessRequest = () => runAction('提交访问申请', async () => {
  const requestType = accessRequestType.value
  const payload = requestType === 'DOWNLOAD_FILE'
    ? {
        certificateId: requireCertificateId(),
        requestType,
        purpose: '页面提交的注册证文件下载申请',
        projectCodeId: requireAccessProjectCodeId(),
        businessFileIds: [requireBusinessFileId()]
      }
    : {
        certificateId: requireCertificateId(),
        requestType,
        purpose: '页面提交的旧注册证查看申请'
      }
  const requestId = await submitRegistrationCertificateAccessRequest(
    payload,
    requireIdempotencyKey(accessIdempotencyKey.value)
  )
  accessRequestId.value = requestId
  await handleRefreshAccessStatus()
  return requestId
})

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
    requireIdempotencyKey(downloadAttemptKey.value)
  )
  downloadByData(result.blob, result.fileName, result.blob.type || 'application/octet-stream')
  return result.fileName
})
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

.registration-certificate-workflow__status,
.registration-certificate-workflow__grants {
  width: 100%;
}
</style>
