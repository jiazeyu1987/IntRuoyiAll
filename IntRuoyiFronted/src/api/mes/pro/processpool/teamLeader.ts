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
  reviewSignatureId: number
  reviewSignatureEmployeeUserId: number
  reviewSignatureSnapshotJson?: string
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

export interface TeamLeaderLossReasonVO {
  id: number
  reasonCode: string
  reasonName: string
  enabled: boolean
}

export interface TeamLeaderProcessConfigParameterVO {
  ruleId?: number
  parameterCode: string
  parameterName?: string
  unit?: string
  valueType: string
  lowerLimit: number | string
  targetValue: number | string
  upperLimit: number | string
  enabled?: boolean
  actualAverage?: number | null
  sampleCount: number
  statisticsStartTime?: string | number
  statisticsEndTime?: string | number
  statisticsWindowDays: number
}

export interface TeamLeaderProcessConfigDeviceVO {
  bindingId?: number
  deviceId: number
  deviceCode?: string
  deviceName?: string
  deviceStatus?: string
  mapped?: boolean
  parameters: TeamLeaderProcessConfigParameterVO[]
}

export interface TeamLeaderProcessConfigRowRespVO {
  routeId: number
  routeCode?: string
  routeName?: string
  routeProcessId: number
  processId: number
  processCode?: string
  processName?: string
  sort?: number
  lossReasons: TeamLeaderLossReasonVO[]
  devices: TeamLeaderProcessConfigDeviceVO[]
}

export interface TeamLeaderLossReasonSaveReqVO {
  routeProcessId: number
  reasonCode: string
  reasonName: string
  enabled?: boolean
  remark?: string
}

export interface TeamLeaderLossReasonUpdateReqVO {
  reasonName: string
  enabled?: boolean
  remark?: string
}
export interface TeamDeviceParameterRuleSaveReqVO {
  routeProcessId: number
  deviceId: number
  parameterCode: string
  parameterName?: string
  unit?: string
  lowerLimit: number | string
  upperLimit: number | string
  targetValue: number | string
  valueType: string
}

export interface TeamLeaderActiveOrderAddReqVO {
  workOrderId: number
}

export interface TeamLeaderActiveOrderCandidateRespVO {
  workOrderId: number
  workOrderCode: string
}

export interface TeamLeaderActiveOrderRemoveReqVO {
  activeOrderId: number
}

export interface TeamProductionPersonnelListReqVO {
  enabled?: boolean
}

export interface TeamPqcPersonnelListReqVO {
  enabled?: boolean
}

export interface TeamPqcPersonnelRespVO {
  scopeId: number
  systemUserId: number
  displayName: string
  username: string
  enabled: boolean
}

export interface TeamPqcPersonnelLinkReqVO {
  systemUserId: number
}

export interface TeamPqcPersonnelStatusUpdateReqVO {
  scopeId: number
  enabled: boolean
}

export interface TeamProductionEmployeeRespVO {
  id: number
  systemUserId?: number
  employeeCode?: string
  employeeName?: string
  displayName?: string
  employeeType?: string
  enabled?: boolean
  disabledAt?: string | number
  signaturePasswordManagedBy?: string
}

export interface TeamFormalEmployeeCandidateRespVO {
  systemUserId: number
  displayName: string
}

export interface TeamTemporaryEmployeeCreateReqVO {
  displayName: string
  signaturePassword: string
}

export interface TeamFormalEmployeeLinkReqVO {
  systemUserId: number
  displayName?: string
}

export interface TeamEmployeeDisplayNameUpdateReqVO {
  employeeProfileId: number
  displayName: string
}

export interface TeamEmployeeStatusUpdateReqVO {
  employeeProfileId: number
  enabled: boolean
}

export interface TeamTemporarySignaturePasswordResetReqVO {
  employeeProfileId: number
  signaturePassword: string
}

export interface TeamEmployeeAuditRespVO {
  id: number
  operatorUserId?: number
  actionType?: string
  targetType?: string
  targetId?: number
  resultStatus?: string
  changeSummary?: string
  auditTime?: string | number
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
  routeProcessId: number
  deviceId: number
}

export interface TeamProcessDefectReasonSaveReqVO extends TeamDefectReasonSaveReqVO {}

export interface TeamLeaderActiveOrderRespVO {
  id: number
  workOrderId: number
  routeId: number
  routeVersionId: number
  erpFixedQuantitySnapshot?: number | string
  activeStatus: string
  businessStatus?: string
  joinedAt?: number
  removedAt?: number
  version?: number
}

export interface TeamLeaderActiveOrderTransferTraceRespVO {
  id: number
  activeOrderId: number
  workOrderId: number
  routeId: number
  routeVersionId: number
  sourceType: string
  direction?: string
  transferId?: number
  transferLineId?: number
  transferDetailId?: number
  materialStockId?: number
  batchId?: number
  itemId?: number
  quantity: number | string
  sourceObjectType?: string
  sourceObjectId?: string
  sourceObjectCode?: string
  sourceStatus?: string
  sourceOccurredAt?: number
  idempotencyKey: string
  sourceSnapshotJson?: string
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
  reviewSignatureId: number
  reviewSignatureEmployeeUserId: number
  reviewSignatureSnapshotJson?: string
  allocations: TeamLeaderReportAllocationLine[]
}

const requireReviewSignaturePayload = (
  data: Pick<TeamLeaderSubmissionReviewReqVO, 'reviewSignatureId' | 'reviewSignatureEmployeeUserId'>
) => {
  if (!Number.isFinite(Number(data.reviewSignatureId)) || Number(data.reviewSignatureId) <= 0) {
    throw new Error('复核电子签名不能为空')
  }
  if (
    !Number.isFinite(Number(data.reviewSignatureEmployeeUserId)) ||
    Number(data.reviewSignatureEmployeeUserId) <= 0
  ) {
    throw new Error('复核签名员工不能为空')
  }
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
  requireReviewSignaturePayload(data)
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

export const createTeamLeaderLossReason = async (data: TeamLeaderLossReasonSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/loss-reasons',
    data
  })
}

export const updateTeamLeaderLossReason = async (
  id: number,
  data: TeamLeaderLossReasonUpdateReqVO
) => {
  return await request.put<boolean>({
    url: `/mes/pro/process-pool/team-leader/loss-reasons/${id}`,
    data
  })
}

export const deleteTeamLeaderLossReason = async (id: number) => {
  return await request.delete<boolean>({
    url: `/mes/pro/process-pool/team-leader/loss-reasons/${id}`
  })
}

export const getTeamLeaderProcessConfigList = async () => {
  return await request.get<TeamLeaderProcessConfigRowRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/process-config/list'
  })
}

export const getTeamLeaderActiveOrderList = async () => {
  return await request.get<TeamLeaderActiveOrderRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/active-order/list'
  })
}

export const getTeamLeaderActiveOrderTransferTrace = async (activeOrderId: number) => {
  return await request.get<TeamLeaderActiveOrderTransferTraceRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/active-order/transfer-trace',
    params: { activeOrderId }
  })
}

export const addTeamLeaderActiveOrder = async (data: TeamLeaderActiveOrderAddReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/active-order/add',
    data
  })
}

export const searchTeamLeaderActiveOrderCandidates = async (keyword: string) => {
  return await request.get<TeamLeaderActiveOrderCandidateRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/active-order/candidates',
    params: { keyword }
  })
}

export const removeTeamLeaderActiveOrder = async (data: TeamLeaderActiveOrderRemoveReqVO) => {
  return await request.put<boolean>({
    url: '/mes/pro/process-pool/team-leader/active-order/remove',
    data
  })
}

export const getProductionPersonnelList = async (params?: TeamProductionPersonnelListReqVO) => {
  return await request.get<TeamProductionEmployeeRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/employee-profile/list',
    params
  })
}

export const getPqcPersonnelList = async (params?: TeamPqcPersonnelListReqVO) => {
  return await request.get<TeamPqcPersonnelRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/pqc-personnel/list',
    params
  })
}

export const searchPqcFormalEmployeeCandidates = async (keyword: string) => {
  return await request.get<TeamFormalEmployeeCandidateRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/pqc-personnel/formal-candidates',
    params: { keyword }
  })
}

export const linkPqcFormalEmployee = async (data: TeamPqcPersonnelLinkReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/pqc-personnel/formal/link',
    data
  })
}

export const updatePqcPersonnelStatus = async (data: TeamPqcPersonnelStatusUpdateReqVO) => {
  return await request.put<boolean>({
    url: '/mes/pro/process-pool/team-leader/pqc-personnel/status/update',
    data
  })
}

export const searchTeamFormalEmployeeCandidates = async (keyword: string) => {
  return await request.get<TeamFormalEmployeeCandidateRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/employee-profile/formal-candidates',
    params: { keyword }
  })
}

export const createTemporaryTeamEmployee = async (data: TeamTemporaryEmployeeCreateReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/employee-profile/temporary/create',
    data
  })
}

export const linkFormalTeamEmployee = async (data: TeamFormalEmployeeLinkReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/employee-profile/formal/link',
    data
  })
}

export const updateTeamEmployeeDisplayName = async (
  data: TeamEmployeeDisplayNameUpdateReqVO
) => {
  return await request.put<boolean>({
    url: '/mes/pro/process-pool/team-leader/employee-profile/display-name/update',
    data
  })
}

export const updateTeamEmployeeStatus = async (data: TeamEmployeeStatusUpdateReqVO) => {
  return await request.put<boolean>({
    url: '/mes/pro/process-pool/team-leader/employee-profile/status/update',
    data
  })
}

export const resetTemporaryTeamEmployeeSignaturePassword = async (
  data: TeamTemporarySignaturePasswordResetReqVO
) => {
  return await request.put<boolean>({
    url: '/mes/pro/process-pool/team-leader/employee-profile/temp-signature-password/reset',
    data
  })
}

export const getTeamEmployeeAuditList = async (params?: { employeeProfileId?: number }) => {
  return await request.get<TeamEmployeeAuditRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/employee-profile/audit/list',
    params
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

export const saveTeamProcessConfigDeviceBinding = async (data: TeamProcessDeviceBindingSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/process-config/device-binding/save',
    data
  })
}

export const saveTeamProcessConfigDeviceParameterRule = async (data: TeamDeviceParameterRuleSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/process-config/device-parameter-rule/save',
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
  const reviewSignatureId = data.reviewSignatureId
  void reviewSignatureId
  requireReviewSignaturePayload(data)
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/submission/allocation/confirm',
    data
  })
}
