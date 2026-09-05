import request from '@/config/axios'
import type {
  ProcessPoolTimelineDetailVO,
  ProcessPoolTimelineEventVO,
  ProcessPoolTimelinePageReqVO
} from '@/api/mes/pro/processpool'

export type TeamLeaderType = 'PRODUCTION' | 'PQC'
export type SubmissionReviewStatus = 'APPROVED' | 'REJECTED'
export type DeviceParameterValueType =
  | 'INTEGER'
  | 'DECIMAL'
  | 'TEXT_STANDARD'
  | 'SELECT'
  | 'BOOLEAN'

export interface TeamLeaderSubmissionPageReqVO extends ProcessPoolTimelinePageReqVO {
  leaderType: TeamLeaderType
  pqcFormView?: 'CURRENT' | 'HISTORY'
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
  overagePercent?: number | string | null
  lossReasons: TeamLeaderLossReasonVO[]
  devices: TeamLeaderProcessConfigDeviceVO[]
}

export interface TeamLeaderProcessOverageLimitSaveReqVO {
  routeProcessId: number
  processId: number
  overagePercent: number | string
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

export type TeamLeaderActiveOrderCandidateState = 'ADDABLE' | 'REUSABLE' | 'RECOVERABLE' | 'BLOCKED'

export interface TeamLeaderActiveOrderCandidateRespVO {
  workOrderId: number
  workOrderCode: string
  candidateState: TeamLeaderActiveOrderCandidateState
  eligible: boolean
  ineligibleReason?: string
}

export type TeamLeaderActiveOrderCommitAction = 'ADD' | 'REUSE' | 'RECOVER'

export interface TeamLeaderActiveOrderAddRespVO {
  activeOrderId: number
  action: TeamLeaderActiveOrderCommitAction
  workOrderId: string
}

export interface TeamLeaderActiveOrderRemoveReqVO {
  activeOrderId: number
}

export interface TeamLeaderActiveOrderMoveReqVO {
  activeOrderId: number
  direction: 'UP' | 'DOWN'
}

export interface TeamLeaderActiveOrderRebuildReqVO {
  activeOrderId: number
  confirmDeleteHistoricalRuntimeData?: boolean
}

export interface TeamLeaderActiveOrderRebuildPreviewRespVO {
  activeOrderId: number
  hasHistoricalRuntimeData: boolean
  productionReportCount: number
  productionProgressCount: number
  pqcInspectionResultCount: number
  processSnapshotCount: number
  pqcTaskCount: number
  releaseApplicationCount: number
  eventCount: number
}

export interface TeamLeaderActiveOrderRebuildResultRespVO {
  activeOrderId: number
  historicalRuntimeDataDeleted: boolean
  deletedProductionReportCount: number
  deletedProductionProgressCount: number
  deletedPqcInspectionResultCount: number
  deletedProcessSnapshotCount: number
  deletedPqcTaskCount: number
  rebuiltProcessSnapshotCount: number
  rebuiltPqcTaskCount: number
}

export interface TeamLeaderActiveOrderVersionUpgradeVersionLineVO {
  objectType: string
  objectName?: string
  objectId?: number
  currentVersionId?: number
  currentVersionNo?: string
  targetVersionId?: number
  targetVersionNo?: string
  changed?: boolean
}

export interface TeamLeaderActiveOrderVersionUpgradePreviewRespVO {
  activeOrderId: number
  workOrderId?: number
  workOrderCode?: string
  allLatestFormalVersions: boolean
  perVersionSelectionAllowed: false
  submittable: boolean
  blockers: string[]
  currentVersions: TeamLeaderActiveOrderVersionUpgradeVersionLineVO[]
  targetVersions: TeamLeaderActiveOrderVersionUpgradeVersionLineVO[]
}

export interface TeamLeaderActiveOrderVersionUpgradeSubmitReqVO {
  activeOrderId: number
  idempotencyKey: string
  upgradeReason: string
  confirmRestartFromBeginning: boolean
}

export interface TeamLeaderActiveOrderVersionUpgradeSubmitRespVO {
  activeOrderId: number
  requestCode?: string
  approvalStatus?: string
  freezeStatus?: string
}

export interface TeamLeaderActiveOrderSimulationReqVO {
  activeOrderId: number
}

export interface TeamLeaderActiveOrderSimulationRespVO {
  activeOrderId: number
  productionSubmitCount: number
  productionReviewCount: number
  pqcSubmitCount: number
  pqcReviewCount: number
  productionProgressPercent: number | string
  inspectionProgressPercent: number | string
}

export interface TeamLeaderActiveOrderSimulationCopyReqVO {
  sourceActiveOrderId: number
  simulationRunId: string
}

export interface TeamLeaderActiveOrderSimulationCopyRespVO {
  activeOrderId: number
  workOrderId: number
  workOrderCode: string
  workOrderName: string
  routeId: number
  routeVersionId: number
  routeVersionNo: string
  qaRegulationVersionId: number
  simulationRunId: string
}

export interface Stage2_5BackfillBatchExecutionSimulationReqVO {
  simulationRunId: string
  activeOrderId: number
  expectedVersion: number
}

export interface Stage2_5BackfillBatchExecutionSimulationRespVO {
  simulationRunId: string
  cleanedSimulationRunId?: string
  batchExecutionId: number
  batchExecutionCode?: string
  completionReceiptId?: number
  detailPath: string
  batchExecutionSnapshot: Record<string, unknown>
  blockers?: string[]
}

export interface Stage6IdiSimulationReqVO {
  simulationRunId: string
  stage5SimulationRunId: string
  batchExecutionId: number
}

export interface Stage6IdiSimulationRespVO {
  simulationRunId: string
  cleanedSimulationRunId?: string
  workOrderId?: number
  workOrderCode?: string
  activeOrderId?: number
  completionReceiptId?: number
  completionStatus?: string
  productionSubmitCount?: number
  productionReviewCount?: number
  pqcSubmitCount?: number
  pqcReviewCount?: number
  releasePreparationStatus?: string
  traceEntryPath: string
  batchExecutionId?: number
  executionId?: number
  releaseTransactionId?: number
  releaseDecisionId?: number
  releaseReceiptId?: string
  releaseSnapshot?: Record<string, unknown>
  traceabilitySnapshot?: Record<string, unknown>
}

export interface Stage1ActiveOrderCompleteSimulationReqVO {
  simulationRunId: string
  templateActiveOrderId: number
}

export interface Stage1ActiveOrderCompleteSimulationRespVO {
  simulationRunId: string
  cleanedSimulationRunId?: string
  activeOrderId: number
  workOrderId: number
  pickListId: number
  productionSubmitCount: number
  productionReviewCount: number
  pqcSubmitCount: number
  pqcReviewCount: number
  productionProgressPercent: number | string
  inspectionProgressPercent: number | string
  productionProgress100: boolean
  inspectionProgress100: boolean
  completionButtonEnabled: boolean
  activeOrderCompleteSnapshot: Record<string, unknown>
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
  | 'PQC_RELEASE_PENDING'
  | 'PQC_RELEASE_REJECTED'
  | 'REPORT_UPLOAD_PENDING'
  | 'MANAGER_RELEASE_PENDING'
  | 'RELEASED'

export interface TeamLeaderActiveOrderProcessRemainingQuantity {
  routeProcessId?: number
  processId?: number
  plannedQuantity?: number | string
  allocatedQuantity?: number | string
  remainingQuantity?: number | string
  quantityConflict?: boolean
  overageQuantity?: number | string
}

export interface TeamLeaderActiveOrderSubmissionDeviceDetailRespVO {
  deviceId?: number
  deviceCode?: string
  deviceName?: string
}

export interface TeamLeaderActiveOrderSubmissionDetailRespVO {
  eventId: number
  submittedQuantity: number | string
  submitterName: string
  reviewerName?: string
  submittedAt: string | number
  quantityConflict?: boolean
  devices: TeamLeaderActiveOrderSubmissionDeviceDetailRespVO[]
}

export interface TeamLeaderActiveOrderInputMaterialDetailRespVO {
  materialId: number
  materialCode: string
  materialName: string
  materialSpecification?: string
  batchCodes: string[]
  requestedQuantity?: number | string
  actualQuantity?: number | string
  baseActualQuantity?: number | string
  sourcePickListIds: number[]
  sourcePickListNos: string[]
  sourcePickListItemIds: number[]
  sourceSnapshotHash?: string
}

export interface TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO {
  aggregateDetailId: number
  sampleNo?: number
  itemCode?: string
  itemName?: string
  inspectionMethod?: string
  standardText?: string
  measuredValue?: string
  itemResult?: string
  judgement?: string
  selectedEquipmentName?: string
  selectedEquipmentNumber?: string
}

export interface TeamLeaderActiveOrderPqcSubmissionDetailRespVO {
  pqcTaskId: number
  pqcTaskIds?: number[]
  submittedEventId?: number
  submittedEventIds?: number[]
  qaProcessId?: number
  qaProcessCode?: string
  qaProcessName?: string
  inspectionType?: string
  businessDate?: string
  shiftCode?: string
  roundNo?: number
  actualInspectionQuantity?: number
  taskStatus?: string
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
}

export interface TeamLeaderActiveOrderProcessDetailRespVO {
  routeProcessId: number
  processId: number
  processCode?: string
  processName: string
  requiredQuantity: number | string
  submittedQuantity: number | string
  submissionCount: number
  inputMaterials: TeamLeaderActiveOrderInputMaterialDetailRespVO[]
  submissions: TeamLeaderActiveOrderSubmissionDetailRespVO[]
  pqcSubmissions: TeamLeaderActiveOrderPqcSubmissionDetailRespVO[]
  quantityConflict?: boolean
  overageQuantity?: number | string
}

export interface TeamLeaderActiveOrderDetailRespVO {
  activeOrderId: number
  workOrderId: number
  workOrderCode: string
  routeName: string
  processes: TeamLeaderActiveOrderProcessDetailRespVO[]
}

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
  processRemainingQuantities?: TeamLeaderActiveOrderProcessRemainingQuantity[]
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
  releaseApplicationId?: string
  pqcReleaseWorkTaskId?: string
  releaseApplicationStatus?: TeamLeaderActiveOrderReleaseApplicationStatus
  releaseSourceSnapshotHash?: string
  releaseApplicationVersion?: number
  quantityConflict?: boolean
  hasQuantityConflict?: boolean
  quantityConflictProcessCount?: number
  overageQuantity?: number | string
  simulated?: boolean
  simulationStage?: string
  simulationRunId?: string
  stage1GeneratedActiveOrderId?: string | number
  stage1GeneratedWorkOrderCode?: string
}

export interface TeamLeaderActiveOrderReleaseApplyReqVO {
  activeOrderId: number
  idempotencyKey: string
  applyRemark?: string
}

export interface TeamLeaderActiveOrderReleaseBlockerRespVO {
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

export interface TeamLeaderActiveOrderReleaseFailureRespVO {
  stage?: string
  currentStatus?: TeamLeaderActiveOrderReleaseApplicationStatus
  blockers: TeamLeaderActiveOrderReleaseBlockerRespVO[]
}

export interface TeamLeaderActiveOrderReleaseApplyRespVO {
  applicationId: string
  activeOrderId: string
  workOrderId: string
  workOrderCode?: string | null
  batchCode?: string | null
  routeId: string
  routeVersionId: string
  pqcReleaseWorkTaskId: string
  status: TeamLeaderActiveOrderReleaseApplicationStatus
  sourceSnapshotHash: string
  version: number
  appliedAt: string | number
}

export interface TeamDeviceRespVO {
  deviceId: number
  deviceCode: string
  deviceName: string
  deviceStatus: 'ENABLED' | 'REPAIRING' | 'DISABLED'
  enabled: boolean
}

export interface PqcItemEquipmentItemVO {
  itemCode: string
  itemName?: string
  inspectionMethod?: string
  standardText?: string
  samplingPlanText?: string
}

export interface PqcItemEquipmentNumberConfigVO {
  id?: number
  equipmentNumber: string
  enabled?: boolean
  sort?: number
}

export interface PqcItemEquipmentGroupConfigVO {
  id?: number
  equipmentId: number
  equipmentCode?: string
  equipmentName?: string
  enabled?: boolean
  defaultFlag?: boolean
  sort?: number
  equipmentNumbers: PqcItemEquipmentNumberConfigVO[]
}

export interface PqcItemEquipmentConfigVO {
  itemCode: string
  itemName?: string
  equipmentGroups: PqcItemEquipmentGroupConfigVO[]
}

export interface PqcItemEquipmentConfigSaveReqVO {
  itemCode: string
  itemNameSnapshot?: string
  equipmentGroups: PqcItemEquipmentGroupConfigVO[]
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
  overageQuantity?: number | string
  needsAdjustment?: boolean
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

export const getTeamLeaderProcessConfigList = async (
  params: TeamLeaderProcessConfigListReqVO = {}
) => {
  return await request.get<TeamLeaderProcessConfigRowRespVO[]>({
    url: '/mes/pro/process-pool/team-leader/process-config/list',
    params
  })
}

export const saveTeamLeaderProcessOverageLimit = async (
  data: TeamLeaderProcessOverageLimitSaveReqVO
) => {
  return await request.post<TeamLeaderProcessConfigRowRespVO>({
    url: '/mes/pro/process-pool/team-leader/process-config/overage-limit/save',
    data
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

export const getTeamLeaderActiveOrderDetail = async (activeOrderId: number) => {
  return await request.get<TeamLeaderActiveOrderDetailRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/detail',
    params: { activeOrderId }
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

export const getTeamLeaderActiveOrderRelease = async (activeOrderId: number | string) => {
  return await request.get<TeamLeaderActiveOrderReleaseApplyRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/release/get',
    params: { activeOrderId },
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
  return await request.post<TeamLeaderActiveOrderAddRespVO>({
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

export const previewTeamLeaderActiveOrderRebuild = async (activeOrderId: number) => {
  return await request.get<TeamLeaderActiveOrderRebuildPreviewRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/rebuild/preview',
    params: { activeOrderId },
    ignoreErrorMessage: true
  })
}

export const rebuildTeamLeaderActiveOrder = async (
  data: TeamLeaderActiveOrderRebuildReqVO
) => {
  return await request.post<TeamLeaderActiveOrderRebuildResultRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/rebuild',
    data,
    ignoreErrorMessage: true
  })
}

export const previewTeamLeaderActiveOrderVersionUpgrade = async (activeOrderId: number) => {
  return await request.get<TeamLeaderActiveOrderVersionUpgradePreviewRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/version-upgrade/preview',
    params: { activeOrderId },
    ignoreErrorMessage: true
  })
}

export const submitTeamLeaderActiveOrderVersionUpgrade = async (
  data: TeamLeaderActiveOrderVersionUpgradeSubmitReqVO
) => {
  return await request.post<TeamLeaderActiveOrderVersionUpgradeSubmitRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/version-upgrade/submit',
    data,
    ignoreErrorMessage: true
  })
}

export const simulateTeamLeaderActiveOrderCompletion = async (
  data: TeamLeaderActiveOrderSimulationReqVO
) => {
  return await request.post<TeamLeaderActiveOrderSimulationRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/simulate-completion',
    data,
    ignoreErrorMessage: true
  })
}

export const simulateStage2_5BackfillBatchExecution = async (
  data: Stage2_5BackfillBatchExecutionSimulationReqVO
) => {
  return await request.post<Stage2_5BackfillBatchExecutionSimulationRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/simulation/stage2-5',
    data,
    ignoreErrorMessage: true
  })
}

export const simulateStage6IdiData = async (data: Stage6IdiSimulationReqVO) => {
  return await request.post<Stage6IdiSimulationRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/simulation/stage6-idpr',
    data,
    ignoreErrorMessage: true
  })
}

export const simulateStage1ActiveOrderCompletion = async (
  data: Stage1ActiveOrderCompleteSimulationReqVO
) => {
  return await request.post<Stage1ActiveOrderCompleteSimulationRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/simulation/stage1',
    data,
    ignoreErrorMessage: true
  })
}

export const copyLatestTeamLeaderSimulationActiveOrder = async (
  data: TeamLeaderActiveOrderSimulationCopyReqVO
) => {
  return await request.post<TeamLeaderActiveOrderSimulationCopyRespVO>({
    url: '/mes/pro/process-pool/team-leader/active-order/simulation/copy-latest',
    data,
    ignoreErrorMessage: true
  })
}

export const cleanupLatestTeamLeaderSimulationActiveOrder = async (activeOrderId: number) => {
  return await request.post<boolean>({
    url: '/mes/pro/process-pool/team-leader/active-order/simulation/copy-latest/cleanup',
    data: { activeOrderId },
    ignoreErrorMessage: true
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

export const updateTeamEmployeeDisplayName = async (data: TeamEmployeeDisplayNameUpdateReqVO) => {
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

export const getPqcItemEquipmentItems = async () => {
  return await request.get<PqcItemEquipmentItemVO[]>({
    url: '/mes/pqc/item-equipment/items'
  })
}

export const getPqcItemEquipmentConfig = async (itemCode: string) => {
  return await request.get<PqcItemEquipmentConfigVO>({
    url: '/mes/pqc/item-equipment/config',
    params: { itemCode }
  })
}

export const savePqcItemEquipmentConfig = async (data: PqcItemEquipmentConfigSaveReqVO) => {
  return await request.post<PqcItemEquipmentConfigVO>({
    url: '/mes/pqc/item-equipment/config',
    data
  })
}

export const updateTeamDeviceStatus = async (data: TeamDeviceStatusUpdateReqVO) => {
  return await request.put<boolean>({
    url: '/mes/pro/process-pool/team-leader/team-device/status/update',
    data
  })
}

export const saveTeamProcessConfigDeviceBinding = async (
  data: TeamProcessDeviceBindingSaveReqVO
) => {
  return await request.post<number>({
    url: '/mes/pro/process-pool/team-leader/process-config/device-binding/save',
    data
  })
}

export const saveTeamProcessConfigDeviceParameterRule = async (
  data: TeamDeviceParameterRuleSaveReqVO
) => {
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
