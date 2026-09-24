import request from '@/config/axios'
import type { TeamLeaderActiveOrderDetailRespVO } from '@/api/mes/pro/processpool/teamLeader'
import type { ErpProductionMaterialListVO } from '@/api/erp/production/material-list'

export const getPqcProductionReleaseOrderDetail = async (applicationId: string) => {
  return await request.get<{
    detail: TeamLeaderActiveOrderDetailRespVO
    productionMaterialLists: ErpProductionMaterialListVO[]
  }>({ url: '/mes/pro/production-release/pqc/order-detail', params: { applicationId } })
}

export const PQC_RELEASE_VIEW_PENDING = 'PENDING' as const
export const PQC_RELEASE_VIEW_RELEASED = 'RELEASED' as const
export const PQC_RELEASE_VIEW_VOIDED = 'VOIDED' as const
export const PQC_RELEASE_VIEW_REWORKED = 'REWORKED' as const
export const PQC_RELEASE_VIEW_CONCESSION_RELEASED = 'CONCESSION_RELEASED' as const

export type MesPqcProductionReleaseViewStatus =
  | typeof PQC_RELEASE_VIEW_PENDING
  | typeof PQC_RELEASE_VIEW_RELEASED
  | typeof PQC_RELEASE_VIEW_VOIDED
  | typeof PQC_RELEASE_VIEW_REWORKED
  | typeof PQC_RELEASE_VIEW_CONCESSION_RELEASED

export type MesProductionReleaseApplicationStatus =
  | 'PQC_RELEASE_PENDING'
  | 'PQC_RELEASE_REJECTED'
  | 'REPORT_UPLOAD_PENDING'
  | 'MANAGER_RELEASE_PENDING'
  | 'RELEASED'

export interface MesProductionReleaseBlockerRespVO {
  blockerType: string
  objectType: string
  objectId?: string
  objectCode?: string
  reason: string
  suggestion: string
  routeProcessId?: string
  processId?: string
  fieldCode?: string
  cellKey?: string
}

export interface MesProductionReleaseFailureRespVO {
  stage?: string
  currentStatus?: MesProductionReleaseApplicationStatus
  blockers: MesProductionReleaseBlockerRespVO[]
}

export interface MesProductionReleaseReportUploadTaskRespVO {
  nodeType: string
  batchTaskId: string
  workTaskId: string
  candidateUserIds: string[]
  status: string
}

export interface MesPqcProductionReleaseApproveReqVO {
  applicationId: string
  pqcReleaseWorkTaskId: string
  expectedVersion: number
  idempotencyKey: string
  signaturePassword: string
  udiControlDocumentNo: string
  approvalOpinion?: string
}

export interface MesPqcProductionReleaseRejectReqVO {
  applicationId: string
  pqcReleaseWorkTaskId: string
  expectedVersion: number
  idempotencyKey: string
  rejectReason: string
}

export interface MesPqcProductionReleaseDecisionRespVO {
  applicationId: string
  pqcReleaseWorkTaskId: string
  decision?: 'APPROVE' | 'REJECT' | 'NONCONFORMANCE_REWORK' | 'NONCONFORMANCE_VOID'
  status: MesProductionReleaseApplicationStatus
  rejectReason?: string
  batchExecutionId?: string
  signatureId?: string
  batchRecordEvidenceIds: string[]
  processInspectionEvidenceIds: string[]
  lossReportEvidenceIds: string[]
  lossReportFormCenterInstanceIds: string[]
  lossReportFieldAuditIds: string[]
  lossReportFieldAuditHeadHashes: string[]
  reportUploadTasks: MesProductionReleaseReportUploadTaskRespVO[]
  sourceSnapshotHash: string
  reportSnapshotHash?: string
  version: number
  decidedBy?: string
  decidedAt?: string | number
  udiControlDocumentNo?: string
}

export interface MesPqcProductionReleasePageReqVO extends PageParam {
  viewStatus: MesPqcProductionReleaseViewStatus
  workOrderCode?: string
  batchCode?: string
}

export interface MesPqcProductionReleasePageItemRespVO {
  applicationId: string
  pqcReleaseWorkTaskId: string
  version: number
  viewStatus: MesPqcProductionReleaseViewStatus
  applicationStatus: MesProductionReleaseApplicationStatus
  activeOrderId?: string
  workOrderId?: string
  workOrderCode?: string
  batchCode?: string
  productId?: string
  batchExecutionId?: string
  appliedAt?: string | number
  appliedBy?: string
  decidedAt?: string | number
  decidedBy?: string
  underReview?: boolean
  nonconformanceReviewId?: string
  nonconformanceDisposition?: string
  nonconformanceReason?: string
  nonconformanceClosedAt?: string | number
  approvalReady?: boolean
  approvalBlockerReason?: string
  approvalBlockerSuggestion?: string
}

export const approvePqcProductionRelease = async (data: MesPqcProductionReleaseApproveReqVO) => {
  return await request.post<MesPqcProductionReleaseDecisionRespVO>({
    url: '/mes/pro/production-release/pqc/approve',
    data
  })
}

export const rejectPqcProductionRelease = async (data: MesPqcProductionReleaseRejectReqVO) => {
  return await request.post<MesPqcProductionReleaseDecisionRespVO>({
    url: '/mes/pro/production-release/pqc/reject',
    data
  })
}

export const getPqcProductionRelease = async (applicationId: string) => {
  return await request.get<MesPqcProductionReleaseDecisionRespVO>({
    url: '/mes/pro/production-release/get',
    params: { applicationId }
  })
}

export const getPqcProductionReleasePage = async (params: MesPqcProductionReleasePageReqVO) => {
  return await request.get<PageResult<MesPqcProductionReleasePageItemRespVO[]>>({
    url: '/mes/pro/production-release/pqc/page',
    params
  })
}
