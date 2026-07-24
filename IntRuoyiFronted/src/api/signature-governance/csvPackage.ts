import request from '@/config/axios'
import type { SignatureGovernanceBlocker } from './shared'

export type SignatureGovernanceCsvMaterialType =
  | 'URS'
  | 'FRS'
  | 'RISK_ASSESSMENT'
  | 'IQ'
  | 'OQ'
  | 'PQ'
  | 'TRACE_MATRIX'
  | 'ELECTRONIC_SIGNATURE_SOP'
  | 'EVIDENCE_INDEX'

export type SignatureGovernanceCsvMaterialStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED'
export type SignatureGovernanceCsvReleaseGateStatus = 'GO' | 'BLOCKED'

export interface SignatureGovernanceCsvMaterialReqVO {
  type: SignatureGovernanceCsvMaterialType
  documentId: string
  version: string
  status: SignatureGovernanceCsvMaterialStatus
  owner?: string
  reviewers?: string[]
  approvers?: string[]
  sourceEvidence?: string
  changeControlId?: string
  signatureMeaning?: string
}

export interface SignatureGovernanceCsvTraceRelationReqVO {
  requirementRef: string
  designRef: string
  testRef: string
  evidenceRef: string
  owner?: string
  status: SignatureGovernanceCsvMaterialStatus
  blockerRef?: string
  qualityApprovalRef?: string
}

export interface SignatureGovernanceCsvTrainingRecordReqVO {
  trainingId: string
  userId: string
  sopDocumentId: string
  evidenceRef: string
  effective?: boolean
}

export interface SignatureGovernanceCsvChangeControlReqVO {
  changeControlId: string
  status: SignatureGovernanceCsvMaterialStatus
  evidenceRef: string
}

export interface SignatureGovernanceCsvQaApprovalReqVO {
  approvalRef: string
  approver: string
  status: SignatureGovernanceCsvMaterialStatus
  signatureEvidenceRef: string
}

export interface SignatureGovernanceCsvReleaseGateReqVO {
  qualityOwner?: string
  materials?: SignatureGovernanceCsvMaterialReqVO[]
  traceRelations?: SignatureGovernanceCsvTraceRelationReqVO[]
  trainingRecords?: SignatureGovernanceCsvTrainingRecordReqVO[]
  changeControls?: SignatureGovernanceCsvChangeControlReqVO[]
  qaApproval?: SignatureGovernanceCsvQaApprovalReqVO
  recoveryEvidenceRef?: string
  engineeringVerificationPassed?: boolean
}

export interface SignatureGovernanceCsvReleaseGateRespVO {
  status: SignatureGovernanceCsvReleaseGateStatus
  engineeringVerificationPassed: boolean
  qaApproved: boolean
  blockers: SignatureGovernanceBlocker[]
}

export const evaluateSignatureCsvReleaseGate = (
  releaseId: string,
  data: SignatureGovernanceCsvReleaseGateReqVO
): Promise<SignatureGovernanceCsvReleaseGateRespVO> => {
  return request.post({
    url: `/signature-governance/csv/packages/${releaseId}/release-gate`,
    data
  })
}
