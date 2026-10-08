import type { TeamLeaderActiveOrderReworkSourceRespVO } from '@/api/mes/pro/processPool/teamLeader'

const exactId = (value: number | string) => {
  if (typeof value === 'number' && !Number.isSafeInteger(value)) throw new Error('返工周期编号精度无效')
  const id = String(value)
  if (!/^[1-9][0-9]*$/.test(id) || BigInt(id) > 9223372036854775807n) throw new Error('缺少有效返工周期编号')
  return id
}

export const buildReworkSourceDetailLocation = (
  currentActiveOrderId: number | string,
  source: TeamLeaderActiveOrderReworkSourceRespVO | undefined,
  canOpenHistory: boolean
) => {
  if (!source) return undefined
  const current = exactId(currentActiveOrderId), previous = exactId(source.sourceActiveOrderId)
  if (exactId(source.currentActiveOrderId) !== current || previous === current) throw new Error('返工来源与当前周期不匹配')
  exactId(source.reviewId); exactId(source.qaUserId)
  if (source.sourceBusinessStatus !== 'REWORKED' || !source.qaSignature || !source.qaSignatureEvidence || !source.qaSignatureVerification) throw new Error('返工来源证据不完整')
  for (const field of ['reviewCode','nonconformanceReason','reviewOpinion'] as const) {
    if (typeof source[field] !== 'string' || !source[field].trim()) throw new Error('返工来源处置内容不完整')
  }
  if (!source.qaSignature.signerName?.trim() || source.qaSignature.signedAt == null
      || exactId(source.qaSignature.signatureId!) !== exactId(source.qaSignatureEvidence.id)
      || exactId(source.qaSignatureEvidence.actorId) !== exactId(source.qaUserId)
      || source.qaSignatureEvidence.actionCode !== 'QA_DISPOSITION'
      || source.qaSignatureVerification.verificationStatus !== 'VALID'
      || exactId(source.qaSignatureVerification.signatureId) !== exactId(source.qaSignatureEvidence.id)
      || source.qaSignatureVerification.storedContentHash !== source.qaSignatureEvidence.contentHash
      || source.qaSignatureVerification.calculatedContentHash !== source.qaSignatureEvidence.contentHash
      || source.qaSignatureVerification.storedEvidenceHash !== source.qaSignatureEvidence.evidenceHash
      || source.qaSignatureVerification.calculatedEvidenceHash !== source.qaSignatureEvidence.evidenceHash
      || !/^[a-f0-9]{64}$/i.test(source.qaSignatureEvidence.contentHash || '')
      || !/^[a-f0-9]{64}$/i.test(source.qaSignatureEvidence.evidenceHash || '')) throw new Error('返工来源QA签名证据不完整')
  if (!canOpenHistory) return undefined
  return {
    name: 'MesProEdhrBatchExecutionActiveOrderDetail',
    query: { activeOrderId: previous, from: 'history' }
  }
}
