import request from '@/config/axios'
import type { SignatureGovernanceBlocker } from './shared'

export type SignatureGovernanceRetentionStatus = 'READY' | 'RECORDED' | 'PASSED' | 'BLOCKED'

export interface SignatureGovernanceRetentionPrecheckReqVO {
  endpoint: string
  bucketName: string
  objectLockEnabled?: boolean
  versioningEnabled?: boolean
  defaultRetentionEnabled?: boolean
  retentionMode?: string
  permissionsVerified?: boolean
  ownerUserId?: number
  sampleDccSignatureId?: number
  sampleEdhrArchiveId?: number
}

export interface SignatureGovernanceRetentionPrecheckRespVO {
  status: SignatureGovernanceRetentionStatus
  ready: boolean
  receiptId?: string | null
  blockers: SignatureGovernanceBlocker[]
}

export interface SignatureGovernanceRetentionReceiptReqVO {
  sourceId?: number
  objectKey?: string
  versionId?: string
  retentionMode?: string
  retainUntil?: string
  sha256?: string
  evidenceHash?: string
  archiveSha256?: string
  signatureHash?: string
  auditEventId?: string
}

export interface SignatureGovernanceRetentionReceiptRespVO {
  status: SignatureGovernanceRetentionStatus
  recorded: boolean
  receiptId?: string | null
  blockers: SignatureGovernanceBlocker[]
}

export type SignatureGovernanceRecoverySampleType = 'DCC_SIGNATURE' | 'EDHR_ARCHIVE'

export interface SignatureGovernanceRecoverySampleReqVO {
  sampleType: SignatureGovernanceRecoverySampleType
  objectKey?: string
  versionId?: string
  expectedSha256?: string
  restoredSha256?: string
  expectedDomainHash?: string
  restoredDomainHash?: string
}

export interface SignatureGovernanceRecoveryRehearsalReqVO {
  backupId?: string
  recoveryRuntime?: string
  ownerReviewed?: boolean
  reportWritten?: boolean
  auditWritten?: boolean
  samples: SignatureGovernanceRecoverySampleReqVO[]
}

export interface SignatureGovernanceRecoveryRehearsalRespVO {
  status: SignatureGovernanceRetentionStatus
  passed: boolean
  blockers: SignatureGovernanceBlocker[]
}

export const precheckSignatureRetention = (
  data: SignatureGovernanceRetentionPrecheckReqVO
): Promise<SignatureGovernanceRetentionPrecheckRespVO> => {
  return request.post({
    url: '/signature-governance/retention/precheck',
    data
  })
}

export const createDccSignatureRetentionReceipt = (
  data: SignatureGovernanceRetentionReceiptReqVO
): Promise<SignatureGovernanceRetentionReceiptRespVO> => {
  return request.post({
    url: '/signature-governance/retention/dcc-evidence-receipts',
    data
  })
}

export const createEdhrArchiveRetentionReceipt = (
  data: SignatureGovernanceRetentionReceiptReqVO
): Promise<SignatureGovernanceRetentionReceiptRespVO> => {
  return request.post({
    url: '/signature-governance/retention/edhr-archive-receipts',
    data
  })
}

export const runSignatureRecoveryRehearsal = (
  data: SignatureGovernanceRecoveryRehearsalReqVO
): Promise<SignatureGovernanceRecoveryRehearsalRespVO> => {
  return request.post({
    url: '/signature-governance/retention/recovery-rehearsals',
    data
  })
}
