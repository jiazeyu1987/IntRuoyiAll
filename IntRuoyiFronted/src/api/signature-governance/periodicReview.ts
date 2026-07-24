import request from '@/config/axios'
import type { SignatureGovernanceBlocker, SignatureGovernanceModuleCode } from './shared'

export type SignatureGovernanceReviewFindingCode =
  | 'VALID'
  | 'SIGNATURE_PERMISSION_EXCEPTION'
  | 'SIGNATURE_LOCK_EXCEPTION'
  | 'SIGNATURE_FAILURE_RECORDED'
  | 'ABNORMAL_SIGNATURE_EVIDENCE'
  | 'HASH_MISMATCH'
  | 'HISTORICAL_UNBOUND'
  | 'POLICY_EXCEPTION'

export type SignatureGovernanceReviewBatchStatus = 'BLOCKED' | 'COLLECTED' | 'SIGNED' | 'CLOSED'

export interface SignatureGovernanceReviewProjectionReqVO {
  moduleCode: SignatureGovernanceModuleCode
  sourceTable: string
  sourceId: string
  sourceHash: string
  actionCode: string
  meaningCode: string
  findingCode: SignatureGovernanceReviewFindingCode
}

export interface SignatureGovernanceReviewBatchCreateReqVO {
  reviewOwner?: string
  periodCode?: string
  ruleVersion?: string
  dueDate?: string
  reason?: string
  scopeModules?: SignatureGovernanceModuleCode[]
  permittedModules?: SignatureGovernanceModuleCode[]
  projections?: SignatureGovernanceReviewProjectionReqVO[]
  reviewSignatureStrategyConfigured?: boolean
}

export interface SignatureGovernanceReviewSnapshotItemRespVO {
  moduleCode: SignatureGovernanceModuleCode
  sourceRef: string
  findingCode: SignatureGovernanceReviewFindingCode
}

export interface SignatureGovernanceReviewBatchRespVO {
  status: SignatureGovernanceReviewBatchStatus
  collectable: boolean
  batchId?: string | null
  snapshotHash?: string | null
  blockers: SignatureGovernanceBlocker[]
  snapshotItems: SignatureGovernanceReviewSnapshotItemRespVO[]
}

export const createSignaturePeriodicReviewBatch = (
  data: SignatureGovernanceReviewBatchCreateReqVO
): Promise<SignatureGovernanceReviewBatchRespVO> => {
  return request.post({
    url: '/signature-governance/periodic-review/batches',
    data
  })
}
