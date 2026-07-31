import request from '@/config/axios'
import type {
  ProcessPoolTimelineDetailVO,
  ProcessPoolTimelineEventVO,
  ProcessPoolTimelinePageReqVO
} from '@/api/mes/pro/processpool'

export type TeamLeaderType = 'PRODUCTION' | 'PQC'
export type SubmissionReviewStatus = 'APPROVED' | 'REJECTED'

export interface TeamLeaderSubmissionPageReqVO extends ProcessPoolTimelinePageReqVO {
  leaderType: TeamLeaderType
}

export interface TeamLeaderSubmissionReviewReqVO {
  eventId: number
  leaderType: TeamLeaderType
  reviewStatus: SubmissionReviewStatus
  reviewRemark?: string
}

export interface WorkOrderAbnormalReportReqVO {
  workOrderId: number
  routeProcessId?: number
  processId?: number
  sourceEventId?: number
  abnormalReasonCode: string
  abnormalDescription: string
}

export interface TeamEmployeeBindingSaveReqVO {
  processId: number
  employeeUserId: number
}

export interface TeamEmployeeBindingDisableReqVO {
  bindingId: number
}

export interface TeamDefectReasonSaveReqVO {
  routeProcessId?: number
  processId?: number
  reasonType: string
  reasonCode: string
  reasonName: string
}

export interface TeamDeviceParameterRuleSaveReqVO {
  routeProcessId?: number
  processId: number
  deviceId: number
  parameterCode: string
  parameterName?: string
  lowerLimit: number | string
  upperLimit: number | string
  valueType?: string
}

export const getTeamLeaderSubmissionPage = async (params: TeamLeaderSubmissionPageReqVO) => {
  return await request.get<PageResult<ProcessPoolTimelineEventVO[]>>({
    url: '/mes/pro/process-pool/team-leader/submission/page',
    params
  })
}

export const getTeamLeaderSubmissionDetail = async (id: number, leaderType: TeamLeaderType) => {
  return await request.get<ProcessPoolTimelineDetailVO>({
    url: '/mes/pro/process-pool/team-leader/submission/detail',
    params: { id, leaderType }
  })
}

export const reviewTeamLeaderSubmission = async (data: TeamLeaderSubmissionReviewReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/submission/review',
    data
  })
}

export const markAndReportWorkOrderAbnormal = async (data: WorkOrderAbnormalReportReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/work-order/abnormal/report',
    data
  })
}

export const addTeamEmployeeBinding = async (data: TeamEmployeeBindingSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/employee-binding/add',
    data
  })
}

export const disableTeamEmployeeBinding = async (data: TeamEmployeeBindingDisableReqVO) => {
  return await request.put<boolean>({
    url: '/mes/pro/process-pool/team-leader/employee-binding/disable',
    data
  })
}

export const createTeamDefectReason = async (data: TeamDefectReasonSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/defect-reason/create',
    data
  })
}

export const saveTeamDeviceParameterRule = async (data: TeamDeviceParameterRuleSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/device-parameter-rule/save',
    data
  })
}
