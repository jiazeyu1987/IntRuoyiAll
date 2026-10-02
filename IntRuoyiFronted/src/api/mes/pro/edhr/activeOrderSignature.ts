import request from '@/config/axios'
import type { ActiveOrderAuditIdentity, ActiveOrderAuditScope } from './activeOrderAudit'

export interface ActiveOrderSignatureEvidence {
  activeOrderId: number | string
  signerName?: string
  evidence: {
    id: number | string
    moduleCode: string
    actionCode: string
    subjectType: string
    subjectId: string
    subjectVersion?: string
    actorId: number | string
    meaningLabel?: string
    reason?: string
    signedAt: number
    timeZone?: string
    authenticationMethod?: string
    timeEvidenceId?: string
    contentHash?: string
    evidenceHash?: string
    algorithm?: string
    keyVersion?: string
    policyVersion?: string
    verificationStatus?: string
    canonicalContentJson: string
    beforeContentJson?: string
    afterContentJson?: string
    fieldDiffJson?: string
  }
  verification: {
    signatureId: number | string
    verificationStatus: 'VALID' | 'MISMATCH'
    storedContentHash?: string
    calculatedContentHash?: string
    storedEvidenceHash?: string
    calculatedEvidenceHash?: string
  }
}

const requireExactId = (value: unknown) => {
  if (typeof value === 'number' && (!Number.isSafeInteger(value) || value <= 0)) {
    throw new Error('签名证据查询编号无效')
  }
  if ((typeof value !== 'number' && typeof value !== 'string') ||
      !/^[1-9]\d*$/.test(String(value)) || BigInt(String(value)) > BigInt('9223372036854775807')) {
    throw new Error('签名证据查询编号无效')
  }
  return value as number | string
}

export const buildActiveOrderSignatureRequest = (
  scope: ActiveOrderAuditScope, identity: ActiveOrderAuditIdentity, signatureId: number | string
) => {
  const signature = requireExactId(signatureId)
  if (scope === 'TEAM') {
    return { url: '/mes/pro/process-pool/team-leader/active-order/signature/get',
      params: { activeOrderId: requireExactId(identity), signatureId: signature } }
  }
  if (scope === 'PQC') {
    return { url: '/mes/pro/production-release/pqc/signature/get',
      params: { applicationId: requireExactId(identity), signatureId: signature } }
  }
  if (scope !== 'BATCH' || !identity || typeof identity !== 'object') {
    throw new Error('批次签名证据必须提供明确业务查询身份')
  }
  const hasOrder = identity.activeOrderId !== undefined
  const hasBatch = identity.batchExecutionId !== undefined
  if (hasOrder === hasBatch) throw new Error('批次签名证据只能指定一个业务查询身份')
  const source = hasOrder ? { activeOrderId: requireExactId(identity.activeOrderId) }
    : { batchExecutionId: requireExactId(identity.batchExecutionId) }
  return { url: '/mes/pro/edhr-batch-execution/signature/get', params: { ...source, signatureId: signature } }
}

export const getActiveOrderSignatureEvidence = async (
  scope: ActiveOrderAuditScope, identity: ActiveOrderAuditIdentity, signatureId: number | string
) => await request.get<ActiveOrderSignatureEvidence>(buildActiveOrderSignatureRequest(scope, identity, signatureId))
