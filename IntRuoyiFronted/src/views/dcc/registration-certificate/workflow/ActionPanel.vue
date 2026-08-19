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
          <el-input v-model="accessIdempotencyKey" data-field="idempotencyKey" aria-label="访问申请幂等键" />
          <el-button type="primary" :loading="submitting" @click="handleSubmitAccessRequest">提交访问申请</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="审批结果" name="approvalResult">
        <div data-testid="registration-certificate-approval-result-action" class="registration-certificate-workflow__panel">
          <el-alert type="info" :closable="false" title="审批结果由 BPM Native 待办处理，页面只展示后端返回结果，不本地伪造通过。" />
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
  type DccRegistrationCertificateDraftReqVO
} from '@/api/dcc/registrationCertificate'

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
const accessIdempotencyKey = ref('')

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

const handleSubmitAccessRequest = () => runAction('提交访问申请', () =>
  submitRegistrationCertificateAccessRequest({
    certificateId: requireCertificateId(),
    requestType: 'VIEW_OLD_CERTIFICATE',
    purpose: '页面提交的注册证访问申请'
  }, requireIdempotencyKey(accessIdempotencyKey.value)))
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
</style>
