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
  unit?: string
  lowerLimit: number | string
  upperLimit: number | string
  defaultValue?: number | string
  valueType?: string
}

export interface TeamLeaderActiveOrderAddReqVO {
  workOrderId: number
  routeId: number
  routeVersionId: number
}

export interface TeamLeaderActiveOrderRemoveReqVO {
  activeOrderId: number
}

export interface TeamEmployeeProfileSaveReqVO {
  systemUserId?: number
  employeeCode: string
  employeeName: string
  employeeType: string
}

export interface TeamProcessEmployeeBindingSaveReqVO {
  processId: number
  employeeProfileId: number
}

export interface TeamDeviceSaveReqVO {
  deviceCode: string
  deviceName: string
  deviceStatus: 'ENABLED' | 'REPAIRING' | 'DISABLED'
}

export interface TeamDeviceStatusUpdateReqVO {
  deviceId: number
  deviceStatus: 'ENABLED' | 'REPAIRING' | 'DISABLED'
}

export interface TeamProcessDeviceBindingSaveReqVO {
  processId: number
  deviceId: number
}

export interface TeamProcessDefectReasonSaveReqVO extends TeamDefectReasonSaveReqVO {}

export interface TeamLeaderActiveOrderRespVO {
  id: number
  workOrderId: number
  activeStatus: string
  joinedAt?: string
  removedAt?: string
}

export interface TeamLeaderReportAllocationLine {
  activeOrderId: number
  workOrderId?: number
  workOrderCode?: string
  allocatedQuantity: number | string
  remainingQuantityBeforeAllocation?: number | string
}

export interface TeamLeaderReportAllocationPreviewReqVO {
  eventId: number
  leaderType: TeamLeaderType
}

export interface TeamLeaderReportAllocationPreviewRespVO {
  totalAllocatedQuantity: number | string
  lines: TeamLeaderReportAllocationLine[]
}

export interface TeamLeaderReportAllocationConfirmReqVO {
  eventId: number
  leaderType: TeamLeaderType
  allocationMode: 'FIFO' | 'MANUAL'
  reviewRemark?: string
  allocations: TeamLeaderReportAllocationLine[]
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

export const getTeamLeaderActiveOrderList = async () => {
  return await request.get<TeamLeaderActiveOrderRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/active-order/list'
  })
}

export const addTeamLeaderActiveOrder = async (data: TeamLeaderActiveOrderAddReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/active-order/add',
    data
  })
}

export const removeTeamLeaderActiveOrder = async (data: TeamLeaderActiveOrderRemoveReqVO) => {
  return await request.put<boolean>({
    url: '/mes/pro/process-pool/team-leader/active-order/remove',
    data
  })
}

export const createTeamEmployeeProfile = async (data: TeamEmployeeProfileSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/employee-profile/create',
    data
  })
}

export const saveTeamProcessEmployeeBinding = async (data: TeamProcessEmployeeBindingSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/process-employee-binding/save',
    data
  })
}

export const createTeamDevice = async (data: TeamDeviceSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/team-device/create',
    data
  })
}

export const updateTeamDeviceStatus = async (data: TeamDeviceStatusUpdateReqVO) => {
  return await request.put<boolean>({
    url: '/mes/pro/process-pool/team-leader/team-device/status/update',
    data
  })
}

export const saveTeamProcessDeviceBinding = async (data: TeamProcessDeviceBindingSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/process-device-binding/save',
    data
  })
}

export const saveTeamRuntimeDeviceParameterRule = async (data: TeamDeviceParameterRuleSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/runtime-device-parameter-rule/save',
    data
  })
}

export const saveTeamProcessDefectReason = async (data: TeamProcessDefectReasonSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/process-defect-reason/save',
    data
  })
}

export const previewTeamLeaderReportFifoAllocation = async (
  data: TeamLeaderReportAllocationPreviewReqVO
) => {
  return await request.post<TeamLeaderReportAllocationPreviewRespVO>({
    url: '/mes/pro/process-pool/team-leader/submission/allocation/preview-fifo',
    data
  })
}

export const confirmTeamLeaderReportAllocation = async (
  data: TeamLeaderReportAllocationConfirmReqVO
) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/submission/allocation/confirm',
    data
  })
}
