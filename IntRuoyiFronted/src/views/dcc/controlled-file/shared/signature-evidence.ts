export type DccSignatureTagType = 'success' | 'primary' | 'warning' | 'danger' | 'info'

export const DCC_SIGNATURE_TASK_ACTION_OPTIONS = [
  { label: '审批通过', value: 'APPROVED' },
  { label: '审批驳回', value: 'REJECTED' },
  { label: '流程回退', value: 'RETURNED' },
  { label: '任务转办', value: 'TRANSFERRED' },
  { label: '任务加签', value: 'SIGN_ADDED' },
  { label: '发放签收', value: 'DISTRIBUTION_ACK' },
  { label: '发放签发', value: 'DISTRIBUTION_SIGN' }
] as const

export const DCC_SIGNATURE_MEANING_OPTIONS = [
  { label: '文控审核通过', value: 'DOC_CONTROL_REVIEW_APPROVE' },
  { label: '文控审核驳回', value: 'DOC_CONTROL_REVIEW_REJECT' },
  { label: '文控审核回退', value: 'DOC_CONTROL_REVIEW_RETURN' },
  { label: '文控审核转办', value: 'DOC_CONTROL_REVIEW_TRANSFER' },
  { label: '文控审核加签', value: 'DOC_CONTROL_REVIEW_ADD_SIGN' },
  { label: '会签审核通过', value: 'MATRIX_REVIEW_APPROVE' },
  { label: '会签审核驳回', value: 'MATRIX_REVIEW_REJECT' },
  { label: '会签审核回退', value: 'MATRIX_REVIEW_RETURN' },
  { label: '会签审核转办', value: 'MATRIX_REVIEW_TRANSFER' },
  { label: '会签审核加签', value: 'MATRIX_REVIEW_ADD_SIGN' },
  { label: '会签批准通过', value: 'MATRIX_APPROVAL_APPROVE' },
  { label: '会签批准驳回', value: 'MATRIX_APPROVAL_REJECT' },
  { label: '会签批准回退', value: 'MATRIX_APPROVAL_RETURN' },
  { label: '会签批准转办', value: 'MATRIX_APPROVAL_TRANSFER' },
  { label: '会签批准加签', value: 'MATRIX_APPROVAL_ADD_SIGN' },
  { label: '文控批准通过', value: 'DOC_CONTROL_APPROVAL_APPROVE' },
  { label: '文控批准驳回', value: 'DOC_CONTROL_APPROVAL_REJECT' },
  { label: '文控批准回退', value: 'DOC_CONTROL_APPROVAL_RETURN' },
  { label: '文控批准转办', value: 'DOC_CONTROL_APPROVAL_TRANSFER' },
  { label: '文控批准加签', value: 'DOC_CONTROL_APPROVAL_ADD_SIGN' },
  { label: '发放签收', value: 'DISTRIBUTION_ACK' },
  { label: '发放签发', value: 'DISTRIBUTION_SIGN' },
  { label: '归档盖章', value: 'ARCHIVE_SEAL' },
  { label: '作废确认', value: 'OBSOLETE_CONFIRM' }
] as const

export const DCC_SIGNATURE_EVIDENCE_STATUS_OPTIONS = [
  { label: '已校验', value: 'VALID' },
  { label: '待校验', value: 'PENDING_VERIFY' },
  { label: '校验失败', value: 'INVALID' },
  { label: '历史未绑定', value: 'HISTORICAL_UNBOUND' }
] as const

export const DCC_CONTROLLED_COPY_HASH_STATUS_OPTIONS = [
  { label: '已绑定', value: 'BOUND' },
  { label: '不适用', value: 'NOT_APPLICABLE' },
  { label: '历史未绑定', value: 'HISTORICAL_UNBOUND' }
] as const

export const DCC_SIGNATURE_AUTHORIZATION_STATE_OPTIONS = [
  { label: '未授权', value: 'UNAUTHORIZED' },
  { label: '已启用', value: 'ENABLED' },
  { label: '已停用', value: 'DISABLED' },
  { label: '已锁定', value: 'LOCKED' }
] as const

const signatureActionLabelMap = new Map(
  DCC_SIGNATURE_TASK_ACTION_OPTIONS.map((item) => [item.value, item.label] as const)
)

const signatureMeaningLabelMap = new Map(
  DCC_SIGNATURE_MEANING_OPTIONS.map((item) => [item.value, item.label] as const)
)

const evidenceStatusLabelMap = new Map(
  DCC_SIGNATURE_EVIDENCE_STATUS_OPTIONS.map((item) => [item.value, item.label] as const)
)

const controlledCopyHashStatusLabelMap = new Map(
  DCC_CONTROLLED_COPY_HASH_STATUS_OPTIONS.map((item) => [item.value, item.label] as const)
)

const authorizationStateLabelMap = new Map(
  DCC_SIGNATURE_AUTHORIZATION_STATE_OPTIONS.map((item) => [item.value, item.label] as const)
)

export const getDccSignatureTaskActionLabel = (value: string | undefined) => {
  return value ? signatureActionLabelMap.get(value as never) ?? value : '-'
}

export const getDccSignatureMeaningLabel = (value: string | undefined) => {
  return value ? signatureMeaningLabelMap.get(value as never) ?? value : '-'
}

export const getDccSignatureEvidenceStatusLabel = (value: string | undefined) => {
  return value ? evidenceStatusLabelMap.get(value as never) ?? value : '-'
}

export const getDccSignatureEvidenceStatusTagType = (
  value: string | undefined
): DccSignatureTagType => {
  if (value === 'VALID') {
    return 'success'
  }
  if (value === 'PENDING_VERIFY') {
    return 'warning'
  }
  if (value === 'INVALID') {
    return 'danger'
  }
  return 'info'
}

export const getDccControlledCopyHashStatusLabel = (value: string | undefined) => {
  return value ? controlledCopyHashStatusLabelMap.get(value as never) ?? value : '-'
}

export const getDccControlledCopyHashStatusTagType = (
  value: string | undefined
): DccSignatureTagType => {
  if (value === 'BOUND') {
    return 'success'
  }
  if (value === 'HISTORICAL_UNBOUND') {
    return 'warning'
  }
  return 'info'
}

export const getDccSignatureAuthorizationStateLabel = (value: string | undefined) => {
  return value ? authorizationStateLabelMap.get(value as never) ?? value : '-'
}

export const getDccSignatureAuthorizationStateTagType = (
  value: string | undefined
): DccSignatureTagType => {
  if (value === 'ENABLED') {
    return 'success'
  }
  if (value === 'LOCKED') {
    return 'danger'
  }
  return 'info'
}

export const formatDccHashShort = (value: string | undefined | null) => {
  const trimmed = String(value ?? '').trim()
  return trimmed || '-'
}
