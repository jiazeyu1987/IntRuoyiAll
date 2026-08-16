import request from '@/config/axios'

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
  decision?: 'APPROVE' | 'REJECT'
  status: MesProductionReleaseApplicationStatus
  rejectReason?: string
  batchExecutionId?: string
  batchRecordEvidenceIds: string[]
  processInspectionEvidenceIds: string[]
  lossReportEvidenceIds: string[]
  reportUploadTasks: MesProductionReleaseReportUploadTaskRespVO[]
  sourceSnapshotHash: string
  reportSnapshotHash?: string
  version: number
  decidedBy?: string
  decidedAt?: string | number
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
