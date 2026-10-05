import request from '@/config/axios'
import type { ActiveOrderAuditIdentity, ActiveOrderAuditScope } from './activeOrderAudit'

export interface ActiveOrderCorrection {
  revisionId: number | string
  eventId: number | string
  eventType: 'PRODUCTION_SUBMIT' | 'PQC_INSPECTION'
  processName: string
  revisedAt: number | string
  reason: string
  signerName: string
  signatureId: number | string
  signedAt: number | string
  verificationStatus: 'VALID'
  changes: { fieldCode: string; fieldName: string; beforeValue?: string; afterValue?: string }[]
}
export interface ActiveOrderCorrectionTimeline {
  activeOrderId: number | string
  corrections: ActiveOrderCorrection[]
}
const exactId = (value: unknown) => {
  if ((typeof value !== 'number' && typeof value !== 'string') ||
      (typeof value === 'number' && !Number.isSafeInteger(value)) ||
      !/^[1-9]\d*$/.test(String(value)) || BigInt(String(value)) > 9223372036854775807n) {
    throw new Error('补正历史查询编号无效')
  }
  return value
}
export const buildActiveOrderCorrectionRequest = (scope: ActiveOrderAuditScope, identity: ActiveOrderAuditIdentity) => {
  if (scope === 'TEAM') return { url: '/mes/pro/process-pool/team-leader/active-order/corrections', params: { activeOrderId: exactId(identity) } }
  if (scope === 'PQC') return { url: '/mes/pro/production-release/pqc/corrections', params: { applicationId: exactId(identity) } }
  if (scope !== 'BATCH' || !identity || typeof identity !== 'object') throw new Error('补正历史缺少明确业务查询身份')
  const active = identity.activeOrderId !== undefined
  const batch = identity.batchExecutionId !== undefined
  if (active === batch) throw new Error('补正历史只能指定一个批次业务查询身份')
  return { url: '/mes/pro/edhr-batch-execution/corrections', params: active
    ? { activeOrderId: exactId(identity.activeOrderId) } : { batchExecutionId: exactId(identity.batchExecutionId) } }
}
export const getActiveOrderCorrections = async (scope: ActiveOrderAuditScope, identity: ActiveOrderAuditIdentity) =>
  await request.get<ActiveOrderCorrectionTimeline>(buildActiveOrderCorrectionRequest(scope, identity))
