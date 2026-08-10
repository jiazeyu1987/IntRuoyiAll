import request from '@/config/axios'
import type {
  ProcessPoolTimelineDetailVO,
  ProcessPoolTimelineEventVO,
  ProcessPoolTimelinePageReqVO
} from '@/api/mes/pro/processpool'

export type TeamLeaderType = 'PRODUCTION' | 'PQC'
export type SubmissionReviewStatus = 'APPROVED' | 'REJECTED'
export type DeviceParameterValueType = 'INTEGER' | 'DECIMAL' | 'TEXT_STANDARD' | 'SELECT'

export interface TeamLeaderSubmissionPageReqVO extends ProcessPoolTimelinePageReqVO {
  leaderType: TeamLeaderType
}

export interface TeamLeaderSubmissionReviewReqVO {
  eventId: number
  leaderType: TeamLeaderType
  reviewStatus: SubmissionReviewStatus
  reviewRemark?: string
  signaturePassword: string
}

export interface WorkOrderAbnormalReportReqVO {
  workOrderId: number
  abnormalDescription: string
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
  valueType: DeviceParameterValueType
  standardText: string
  lowerLimit?: number | string | null
  targetValue?: number | string | null
  upperLimit?: number | string | null
  optionValues?: string[]
  defaultText?: string | null
  decimalScale?: number | null
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

export interface TeamLeaderResponsibleRouteRespVO {
  routeId: number
  routeCode?: string
  routeName: string
}

export interface TeamLeaderProcessConfigListReqVO {
  routeKeyword?: string
  processKeyword?: string
  lossReasonKeyword?: string
  deviceKeyword?: string
  parameterKeyword?: string
}

export interface TeamLeaderLossReasonSaveReqVO {
  routeProcessId: number
  reasonName: string
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
  standardText: string
  lowerLimit?: number | string | null
  upperLimit?: number | string | null
  targetValue?: number | string | null
  valueType: DeviceParameterValueType
  optionValues?: string[]
  defaultText?: string | null
  decimalScale?: number | null
}

export interface TeamLeaderActiveOrderAddReqVO {
  workOrderId: number
}

export interface TeamLeaderActiveOrderCandidateRespVO {
  workOrderId: number
  workOrderCode: string
  eligible: boolean
  ineligibleReason?: string
}

export interface TeamLeaderActiveOrderRemoveReqVO {
  activeOrderId: number
}

export interface TeamLeaderActiveOrderMoveReqVO {
  activeOrderId: number
  direction: 'UP' | 'DOWN'
}

export interface TeamLeaderActiveOrderListRequestOptions {
  ignoreErrorMessage?: boolean
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
  disabled?: boolean
  disabledReason?: string
  occupiedByOtherPqcLeader?: boolean
  occupiedLeaderUserId?: number
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

export type TeamLeaderActiveOrderReleaseApplicationStatus =
  | 'BLOCKED'
  | 'PENDING_RELEASE_APPROVAL'

export interface TeamLeaderActiveOrderRespVO {
  id: number
  workOrderId: number
  workOrderCode?: string
  productName?: string
  productCode?: string
  quantity?: number | string
  routeId: number
  routeName: string
  routeVersionId: number
  routeVersionNo: string
  erpFixedQuantitySnapshot?: number | string
  productionCoefficient?: number | string
  productionProgressPercent: number | string
  inspectionProgressPercent: number | string
  activeStatus: string
  businessStatus?: string
  joinedAt?: number
  removedAt?: number
  version?: number
  abnormal: boolean
  abnormalReason?: string
  abnormalReportedAt?: number
  releaseApplicationStatus?: TeamLeaderActiveOrderReleaseApplicationStatus
  releaseApplicationBlockerSummary?: string
  releaseApprovalWorkTaskId?: number
}

export interface TeamLeaderActiveOrderReleaseApplyReqVO {
  activeOrderId: number
  idempotencyKey: string
  applyRemark?: string
}

export interface TeamLeaderActiveOrderReleaseBlockerRespVO {
  blockerType: string
  objectType: string
  objectId: string
  objectCode: string
  reason: string
  suggestion: string
  routeProcessId?: number
  processId?: number
  fieldCode?: string
  cellKey?: string
}

export interface TeamLeaderActiveOrderReleaseDossierSummaryRespVO {
  batchRecordCount: number
  processInspectionFormCount: number
  lossReportFormCount: number
  signatureEvidenceCount: number
  sourceSnapshotHash: string
}

export interface TeamLeaderActiveOrderReleaseApplyRespVO {
  applicationId: number
  activeOrderId: number
  workOrderId: number
  workOrderCode: string | null
  batchExecutionId: number | null
  releaseTransactionId: number | null
  releaseApprovalWorkTaskId: number | null
  status: TeamLeaderActiveOrderReleaseApplicationStatus
  statusName: string
  dossierSummary: TeamLeaderActiveOrderReleaseDossierSummaryRespVO
  blockers: TeamLeaderActiveOrderReleaseBlockerRespVO[]
  appliedAt: string | number
}

export interface TeamDeviceRespVO {
  deviceId: number
  deviceCode: string
  deviceName: string
  deviceStatus: 'ENABLED' | 'REPAIRING' | 'DISABLED'
  enabled: boolean
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
  allocationId?: number
  activeOrderId: number
  workOrderId?: number
  workOrderCode?: string
  routeProcessId?: number
  processId?: number
  allocatedQuantity: number | string
  allocationMode?: 'FIFO' | 'MANUAL'
  remainingQuantityBeforeAllocation?: number | string
  released?: boolean
  editable?: boolean
}

export interface TeamLeaderReportAllocationPreviewReqVO {
  eventId: number
  leaderType: TeamLeaderType
}

export interface TeamLeaderReportAllocationPreviewRespVO {
  eventId?: number
  version?: number
  poolQuantity: number | string
  releasedAllocatedQuantity?: number | string
  editableAllocatedQuantity?: number | string
  totalAllocatedQuantity: number | string
  unallocatedQuantity: number | string
  lines: TeamLeaderReportAllocationLine[]
}

export type TeamLeaderReportAllocationSnapshotRespVO = TeamLeaderReportAllocationPreviewRespVO

export interface TeamLeaderReportAllocationAuditRespVO {
  id: number
  eventId: number
  allocationVersion: number
  sourceAllocationId?: number
  activeOrderId: number
  workOrderId: number
  routeProcessId: number
  processId: number
  beforeQuantity: number
  afterQuantity: number
  deltaQuantity: number
  actorUserId: number
  adjustmentReason: string
  allocationMode: 'FIFO' | 'MANUAL' | 'SYSTEM'
  changeSource: 'INITIAL_BASELINE' | 'FIFO' | 'MANUAL' | 'ORDER_CHANGE'
  occurredAt: string
}

export interface TeamLeaderReportAllocationConfirmReqVO {
  eventId: number
  leaderType: TeamLeaderType
  allocationMode: 'FIFO' | 'MANUAL'
  expectedVersion?: number
  idempotencyKey?: string
  reviewRemark?: string
  signaturePassword?: string
  allocations: TeamLeaderReportAllocationLine[]
}

const requireReviewSignaturePayload = (
  data: Pick<TeamLeaderSubmissionReviewReqVO, 'signaturePassword'>
) => {
  if (!data.signaturePassword?.trim()) {
    throw new Error('请输入电子签名密码')
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

export const getTeamLeaderProcessConfigList = async (params: TeamLeaderProcessConfigListReqVO = {}) => {
  return await request.get<TeamLeaderProcessConfigRowRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/process-config/list',
    params
  })
}

export const getTeamLeaderResponsibleRouteList = async () => {
  return await request.get<TeamLeaderResponsibleRouteRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/responsible-routes'
  })
}

export const getTeamLeaderActiveOrderList = async (
  options: TeamLeaderActiveOrderListRequestOptions = {}
) => {
  return await request.get<TeamLeaderActiveOrderRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/active-order/list',
    ignoreErrorMessage: options.ignoreErrorMessage
  })
}

export const applyTeamLeaderActiveOrderRelease = async (
  data: TeamLeaderActiveOrderReleaseApplyReqVO
) => {
  return await request.post<TeamLeaderActiveOrderReleaseApplyRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/release/apply',
    data,
    ignoreErrorMessage: true
  })
}

export const getTeamLeaderActiveOrderTransferTrace = async (activeOrderId: number) => {
  return await request.get<TeamLeaderActiveOrderTransferTraceRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/active-order/transfer-trace',
    params: { activeOrderId }
  })
}

export const searchTeamLeaderActiveOrderCandidates = async (keyword: string) => {
  return await request.get<TeamLeaderActiveOrderCandidateRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/active-order/candidates',
    params: { keyword }
  })
}

export const addTeamLeaderActiveOrder = async (data: TeamLeaderActiveOrderAddReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/active-order/add',
    data
  })
}

export const moveTeamLeaderActiveOrder = async (data: TeamLeaderActiveOrderMoveReqVO) => {
  return await request.put<boolean>({
    url: '/mes/pro/process-pool/team-leader/active-order/move',
    data
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

export const createTeamDevice = async (data: TeamDeviceSaveReqVO) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/team-device/create',
    data
  })
}

export const getTeamDeviceList = async (enabled?: boolean) => {
  return await request.get<TeamDeviceRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/team-device/list',
    params: { enabled }
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
  return await request.post<TeamLeaderReportAllocationSnapshotRespVO>({
    url: '/mes/pro/process-pool/team-leader/submission/allocation/confirm',
    data
  })
}

export const getCurrentTeamLeaderReportAllocation = async (
  eventId: number,
  leaderType: TeamLeaderType
) => {
  return await request.get<TeamLeaderReportAllocationSnapshotRespVO>({
    url: '/mes/pro/process-pool/team-leader/submission/allocation/current',
    params: { eventId, leaderType }
  })
}

export const getTeamLeaderReportAllocationAudit = async (
  eventId: number,
  leaderType: TeamLeaderType
) => {
  return await request.get<TeamLeaderReportAllocationAuditRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/submission/allocation/audit',
    params: { eventId, leaderType }
  })
}
