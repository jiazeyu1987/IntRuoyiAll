import type { LifecycleFacts, WorkflowId } from './workflow-actions'

export type DistributionLifecycleFacts = Omit<LifecycleFacts, 'effectiveDate'> & {
  effectiveMode?: 'AFTER_STANDARDIZATION_SIGNATURE' | 'DELAYED' | 'PRESET_DATE' | null
  effectiveDate?: string | null
  effectiveTime?: string | null
}

export interface AutomaticDistributionResult {
  controlledFileId: WorkflowId
  versionNo: string
  approvalProcessInstanceId: string | null
  batchId: WorkflowId | null
  status: 'NOT_CREATED' | 'PENDING' | 'FAILED' | 'DISPATCH_READY' | 'SENT'
  errorCode?: string | null
  errorMessage?: string | null
  failureAt?: string | number | number[] | null
  preparedAt?: string | number | number[] | null
  sentAt?: string | number | number[] | null
  snapshotAt?: string | number | number[] | null
  policyVersion?: string | null
}

export interface AutomaticDistributionRetryCommand {
  controlledFileId: WorkflowId
  batchId: WorkflowId
  versionNo: string
  approvalProcessInstanceId: string
}

export const validateAutomaticDistributionRetryResult = (
  result: AutomaticDistributionResult, expected: AutomaticDistributionRetryCommand
): AutomaticDistributionResult => {
  if (!result || ![expected.controlledFileId, expected.batchId, result.controlledFileId, result.batchId].every(id => id !== null && validId(id))
    || String(result.controlledFileId) !== String(expected.controlledFileId) || String(result.batchId) !== String(expected.batchId)
    || result.versionNo !== expected.versionNo || result.approvalProcessInstanceId !== expected.approvalProcessInstanceId
    || !['PENDING', 'FAILED', 'DISPATCH_READY', 'SENT'].includes(result.status)
    || result.status === 'SENT' && !result.sentAt)
    throw new Error('自动下发重试结果缺少本次精确批次、版本或轮次事实')
  if (result.status === 'FAILED' && (!result.errorCode?.trim() || !result.errorMessage?.trim() || !result.failureAt
    || typeof result.failureAt === 'string' && !result.failureAt.trim()
    || Array.isArray(result.failureAt) && result.failureAt.length === 0))
    throw new Error('自动下发重试结果缺少真实失败事实：错误分类、原因或失败时间')
  return { ...result }
}

export interface DistributionRecipientFact {
  controlledFileId: WorkflowId
  versionNo: string
  distributionId: WorkflowId
  recipientId: WorkflowId
  departmentId: WorkflowId
  departmentName?: string
  userId: WorkflowId
  currentUserName?: string
  signerNameSnapshot?: string
  messageStatus: 'PENDING' | 'SENT' | 'FAILED'
  errorMessage?: string
  sentAt?: string | number | number[]
  readAt?: string | number | number[]
  acknowledgedAt?: string | number | number[]
  signatureId?: WorkflowId
  signedAt?: string | number | number[]
  distributionCompletedAt?: string | number | number[]
}

const validId = (id: WorkflowId): boolean => typeof id === 'number'
  ? Number.isSafeInteger(id) && id > 0
  : /^[1-9]\d*$/.test(id) && BigInt(id) <= 9223372036854775807n

export const validateDistributionFacts = (
  rows: DistributionRecipientFact[], fileId: WorkflowId, versionNo: string
): DistributionRecipientFact[] => {
  if (!Array.isArray(rows) || !validId(fileId) || !versionNo.trim()) throw new Error('下发版本身份不完整')
  const ids = new Set<string>()
  for (const row of rows) {
    if (!row || [row.controlledFileId, row.distributionId, row.recipientId, row.departmentId, row.userId].some(id => !validId(id))
      || String(row.controlledFileId) !== String(fileId) || row.versionNo !== versionNo
      || ids.has(String(row.recipientId))) throw new Error('逐人名单不属于本次精确下发版本或存在重复身份')
    ids.add(String(row.recipientId))
    if (!['PENDING', 'SENT', 'FAILED'].includes(row.messageStatus)
      || (row.messageStatus === 'SENT') !== Boolean(row.sentAt)
      || !row.acknowledgedAt && Boolean(row.signatureId || row.signedAt || row.signerNameSnapshot)
      || row.acknowledgedAt && (!row.signatureId || !validId(row.signatureId) || !row.signedAt))
      throw new Error('发送或签名事实不完整，请核对正式记录')
  }
  return rows.map(row => ({ ...row }))
}

export interface DistributionReceiptCommand {
  controlledFileId: WorkflowId
  distributionId: WorkflowId
  recipientId: WorkflowId
  versionNo: string
  password: string
  comment?: string
}

export const submitDistributionReceipt = async (
  input: DistributionReceiptCommand, save: (request: DistributionReceiptCommand) => Promise<boolean>
): Promise<void> => {
  if ([input.controlledFileId, input.distributionId, input.recipientId].some(id => !validId(id))
    || !input.versionNo.trim() || !input.password.trim() || input.comment !== undefined && input.comment.length > 1000)
    throw new Error('请核对下发版本并填写密码，可选意见不得超过1000字')
  const saved = await save({ ...input, comment: input.comment?.trim() })
  if (saved !== true) throw new Error('签收未返回正式保存成功结果，请刷新核对')
}
