import request from '@/config/axios'
import type {
  EdhrBatchArchiveVisibility,
  EdhrBatchExecutionTaskRespVO,
  EdhrBatchFormSlotType,
  EdhrBatchOwnerRoleKey,
  EdhrBatchRequiredPolicy,
  EdhrRecordCategory,
  EdhrRouteId,
  EdhrSignatureTimeReqVO,
  EdhrValidationProfile
} from '../edhr/batchExecution'

export type EdhrExecutionFormSlotType = EdhrBatchFormSlotType
export type EdhrExecutionRequiredPolicy = EdhrBatchRequiredPolicy
export type EdhrExecutionOwnerRoleKey = EdhrBatchOwnerRoleKey
export type EdhrExecutionArchiveVisibility = EdhrBatchArchiveVisibility

// MES 生产报工 VO
export interface ProFeedbackVO {
  id: number // 编号
  code: string // 报工单编号
  type: number // 报工类型
  channel: string // 报工途径
  feedbackTime: Date // 报工时间
  excelProductCode?: string
  excelProductName?: string
  excelProcessCode?: string
  excelProcessName?: string
  excelDepartment?: string
  excelEmployeeNo?: string
  excelEmployeeName?: string
  excelSectionLeader?: string
  excelFeedbackTime?: string | number | Date
  workstationId: number // 工作站编号
  workstationCode: string // 工作站编码
  workstationName: string // 工作站名称
  routeId: number // 工艺路线编号
  routeCode: string // 工艺路线编码
  processId: number // 工序编号
  processCode: string // 工序编码
  processName: string // 工序名称
  checkFlag: boolean // 是否需要检验
  workOrderId: number // 生产工单编号
  workOrderCode: string // 工单编码
  workOrderName: string // 工单名称
  batchCode?: string // 批次号
  taskId: number // 生产任务编号
  taskCode: string // 任务编码
  scheduleOrderId?: number
  scheduleOrderProcessId?: number
  itemId: number // 产品物料编号
  itemCode: string // 物料编码
  itemName: string // 物料名称
  itemSpecification: string // 规格型号
  unitMeasureId: number // 单位编号
  unitMeasureName: string // 单位名称
  expireDate: Date // 过期日期
  scheduledQuantity: number // 排产数量
  feedbackQuantity: number // 本次报工数量
  qualifiedQuantity: number // 合格品数量
  unqualifiedQuantity: number // 不良品数量
  uncheckQuantity: number // 待检测数量
  laborScrapQuantity: number // 工废数量
  materialScrapQuantity: number // 料废数量
  otherScrapQuantity: number // 其他废品数量
  feedbackUserId: number // 报工用户编号
  feedbackUserNickname: string // 报工人昵称
  approveUserId: number // 审核用户编号
  approveUserNickname: string // 审核人昵称
  status: number // 状态
  remark: string // 备注
  sourceImportRecordId?: number
  sourceImportFileName?: string
  sourceImportSheetName?: string
  sourceImportRowNo?: number
  sourceImportAttributionTime?: string | number | Date
  approvalImpactText?: string
}

export interface ProFrontlineLossDetailReqVO {
  reasonId: number
  reasonCode?: string
  reasonName?: string
  quantity: number
}

export interface ProFrontlineSelectedDeviceReqVO {
  deviceId: number
  deviceCode?: string
  deviceName?: string
}

export type ProFrontlineParameterStatus = 'NORMAL' | 'BELOW_LOWER' | 'ABOVE_UPPER'

export interface ProFrontlineDeviceParameterReadingReqVO {
  deviceId: number
  deviceCode?: string
  deviceName?: string
  parameterCode: string
  parameterName?: string
  unit?: string
  value?: number
  lowerLimit?: number | string
  upperLimit?: number | string
  parameterStatus: ProFrontlineParameterStatus
}

export interface ProFrontlineFeedbackPayloadReqVO {
  code?: string
  type?: number
  workstationId: number
  routeId: number
  processId: number
  workOrderId?: number
  taskId?: number
  scheduleOrderId?: number
  scheduleOrderProcessId?: number
  itemId?: number
  expireDate?: string | number | Date
  scheduledQuantity?: number
  outputQuantity: number
  lossQuantity: number
  laborScrapQuantity?: number
  materialScrapQuantity?: number
  otherScrapQuantity?: number
  lossReasonId?: number
  lossDetails?: ProFrontlineLossDetailReqVO[]
  selectedDevice?: ProFrontlineSelectedDeviceReqVO
  deviceParameterReadings?: ProFrontlineDeviceParameterReadingReqVO[]
  approveUserId: number
  remark?: string
}

export interface ProFrontlineRecordbookPayloadReqVO {
  recordbookId: number
  entryTitle: string
  entryContent: Record<string, unknown>
  equipmentParameters: Record<string, unknown>
  tagCodes?: string[]
  idempotencyKey: string
  remark?: string
}

export interface ProFrontlineProcessPoolContextReqVO {
  workOrderId?: number
  taskId?: number
  routeId: number
  routeProcessId: number
  processId: number
  workstationId: number
  deviceId?: number
  deviceAccountUserId: number
  templateType: string
}

export interface ProFrontlineFeedbackSubmitReqVO {
  feedbackPayload: ProFrontlineFeedbackPayloadReqVO
  recordbookPayload?: ProFrontlineRecordbookPayloadReqVO
  processPoolContext: ProFrontlineProcessPoolContextReqVO
  processPoolSubmissionIdempotencyKey: string
  actualEmployeeId: number
  signatureId?: number
  signatureEmployeeId: number
  signaturePassword: string
  rawPayload: Record<string, unknown>
}

export interface ProFrontlineFeedbackSubmitRespVO {
  feedbackId: number
  recordbookEntryId?: number
  recordbookEventId?: number
  processPoolEventId: number
}

export interface FrontlineDeviceRouteProcessVO {
  routeId: number
  routeCode?: string
  routeName?: string
  routeProcessId: number
  processId: number
  processCode?: string
  processName?: string
  sort?: number
  deviceId?: number | null
  deviceCode?: string
  deviceName?: string
  workstationId?: number | null
  workstationCode?: string
  workstationName?: string
  activeOrderId?: number
  pqcTaskId?: number
  regulationVersionId?: number
  finalInspectionApplicable?: boolean
  inspectionType?: string
  businessDate?: string
  shiftCode?: string
  roundNo?: number
  plannedInspectionQuantity?: number
  inspectionItems?: FrontlinePqcInspectionItemVO[]
  pqcTaskOptions?: FrontlinePqcTaskOptionVO[]
  productionSubmitCandidates?: FrontlinePqcProductionSubmitCandidateVO[]
}

export interface FrontlinePqcTaskOptionVO {
  pqcTaskId: number
  regulationVersionId: number
  finalInspectionApplicable?: boolean
  inspectionType: string
  businessDate: string
  shiftCode: string
  roundNo: number
  plannedInspectionQuantity: number
  inspectionItems?: FrontlinePqcInspectionItemVO[]
}

export interface FrontlinePqcProductionSubmitCandidateVO {
  eventId: number
  serverSubmitTime: string | number
}

export interface FrontlinePqcEquipmentOptionVO {
  equipmentId: number
  equipmentCode?: string
  equipmentName?: string
  equipmentNumber: string
  defaultFlag?: boolean
  sort?: number
}

export interface FrontlinePqcInspectionItemVO {
  itemCode: string
  itemName?: string
  inspectionMethod?: string
  standardText?: string
  acceptanceStandard?: string
  processInspectionMethod?: string
  inspectionTool: string | null
  samplingPlanText: string | null
  resultType?: string
  standardLowerLimit?: number | string
  standardUpperLimit?: number | string
  standardUnit?: string
  standardPrecision?: number
  equipmentRequired?: boolean
  equipmentOptions?: FrontlinePqcEquipmentOptionVO[]
}

export interface FrontlineActiveOrderVO {
  workOrderId: number
  workOrderCode?: string
  workOrderName?: string
  productId: number
  productCode?: string
  productName: string
  quantity: number
  routeId: number
  routeCode?: string
  routeName?: string
  latestSubmitTime?: string
}

export interface FrontlineEmployeeCandidateVO {
  userId: number
  username?: string
  nickname?: string
  employeeProfileId?: number
  systemUserId?: number
  employeeCode?: string
  employeeName?: string
  displayName?: string
  employeeType?: string
}

export interface FrontlineTemplateVO {
  templateNo: string
  templateType?: string
  routeProcessId: number
  processId: number
  actualEmployeeId: number
}

export interface FrontlineSwitchActualEmployeeReqVO {
  routeId: number
  routeProcessId: number
  processId: number
  actualEmployeeId: number
}

export interface FrontlinePqcSwitchActualEmployeeReqVO extends FrontlineSwitchActualEmployeeReqVO {
  workOrderId: number
}

export interface FrontlinePqcItemResultSubmitReqVO {
  itemCode: string
  selectedEquipmentId?: number
  selectedEquipmentNumber?: string
  sampleValues: string[]
}

export interface FrontlinePqcInspectionSubmitReqVO {
  activeOrderId?: number
  pqcTaskId: number
  productionSubmitEventId?: number
  regulationVersionId?: number
  workOrderId?: number
  routeId?: number
  routeProcessId?: number
  processId?: number
  inspectionType?: string
  businessDate?: string
  shiftCode?: string
  roundNo?: number
  actualInspectionQuantity: number
  scrapQuantity?: number
  signaturePassword: string
  nonconformanceDescription?: string
  itemResults?: FrontlinePqcItemResultSubmitReqVO[]
  rawPayload?: Record<string, unknown>
  clientSubmitTime?: string
}

export interface FrontlinePqcInspectionSubmitRespVO {
  pqcTaskId: number
  pqcEventId: number
  pqcRecordId: number
  signatureId: number
  inspectionResult: 'SUCCESS' | 'FAILURE'
  serverSubmitTime: string | number
}

export interface FrontlineSwitchActualEmployeeRespVO {
  loginUserId: number
  actualEmployeeId: number
  routeId: number
  routeProcessId: number
  processId: number
  extraVerificationRequired: boolean
  template: FrontlineTemplateVO
}

export interface FrontlineRuntimeEmployeeVO {
  employeeProfileId: number
  systemUserId?: number
  employeeCode?: string
  employeeName?: string
  displayName?: string
  employeeType?: string
}

export interface FrontlineRuntimeDeviceParameterVO {
  parameterCode: string
  parameterName?: string
  unit?: string
  standardText: string
  lowerLimit?: number | string | null
  upperLimit?: number | string | null
  defaultValue?: number | string | null
  valueType?: string
}

export interface FrontlineRuntimeDeviceVO {
  deviceId: number
  deviceCode?: string
  deviceName?: string
  deviceStatus?: string
  parameters: FrontlineRuntimeDeviceParameterVO[]
}

export interface FrontlineRuntimeDefectReasonVO {
  reasonId: number
  reasonType?: string
  reasonCode: string
  reasonName: string
}

export interface FrontlineProductionSubmitContextVO {
  workOrderId?: number
  workOrderCode?: string
  workOrderName?: string
  taskId?: number
  routeId: number
  routeProcessId: number
  processId: number
  workstationId: number
  itemId?: number
  approveUserId: number
  recordbookId?: number
  scheduleOrderId?: number
  scheduleOrderProcessId?: number
  scheduledQuantity?: number
  expireDate?: string | number | Date
}

export interface FrontlineRuntimeConfigVO {
  routeId: number
  routeProcessId: number
  processId: number
  employees: FrontlineRuntimeEmployeeVO[]
  devices: FrontlineRuntimeDeviceVO[]
  defectReasons: FrontlineRuntimeDefectReasonVO[]
  productionSubmitContext: FrontlineProductionSubmitContextVO
}

export interface ThirdPartyFeedbackImportResultVO {
  sheetCount: number
  importedCount: number
  pendingCount: number
  submittedCount: number
  skippedRows?: number
  feedbackCodes: string[]
  importRecordIds: number[]
  directWorkReportDetails?: DirectWorkReportImportDetailVO[]
  directWorkReportSkipWarnings?: DirectWorkReportSkipWarningVO[]
}

export interface DirectWorkReportImportDetailVO {
  sheetName?: string
  rowNo?: number
  attributionStatus?: string
  workOrderCode: string
  scheduleOrderCode: string
  productCode: string
  productName: string
  workstationCode: string
  workstationName: string
  processCode: string
  processName: string
  feedbackUserCode?: string
  feedbackUserName?: string
  approverName?: string
  feedbackQuantity: number
  beforeReportedQuantity: number
  afterReportedQuantity: number
  reportedQuantityDelta: number
  beforeProgressPercent: number
  afterProgressPercent: number
  progressDeltaPercent: number
  feedbackCode?: string
  resultCode?: string
  resultMessage?: string
  importRecordId: number
  remark?: string
}

export interface DirectWorkReportSkipWarningVO {
  sheetName?: string
  rowNo?: number
  workOrderCode: string
  scheduleOrderCode?: string
  productCode?: string
  productName?: string
  processCode: string
  processName?: string
  feedbackUserCode?: string
  feedbackUserName?: string
  approverName?: string
  feedbackQuantity?: number
  reportedQuantity?: number
  remainingQuantity?: number
  progressPercent?: number
  reasonCode?: string
  reason?: string
}

export interface ProFeedbackImportRecordVO {
  id: number
  attributionStatus: string
  attributionTargetType?: string
  sourceFileName?: string
  sheetName?: string
  rowNo?: number
  taskCode?: string
  workOrderCode?: string
  itemCode?: string
  itemName?: string
  specification?: string
  processCode?: string
  processName?: string
  feedbackQuantity?: number
  feedbackTime?: string | number | Date
  feedbackUserCode?: string
  feedbackUserName?: string
  approverName?: string
  scheduleOrderId?: number
  scheduleOrderProcessId?: number
  feedbackId?: number
  attributionTime?: string | number | Date
  candidateCount?: number
  surplusPoolQuantity?: number
  canModifyAttribution?: boolean
  modifyBlockedReason?: string
  linkedFeedbackCount?: number
  feedbackUserId?: number
  feedbackUserNickname?: string
  approveUserId?: number
  approveUserNickname?: string
  remark?: string
  generatedFeedbackDraft?: boolean
  linkedFeedbackStatus?: number
}

export interface ProFeedbackImportBatchSummaryVO {
  sourceFileName?: string
  totalCount: number
  pendingCount: number
  attributedCount: number
  confirmableCount: number
  skippedOtherOrderCount: number
}

export interface ProFeedbackImportConfirmBatchRowReqVO {
  importRecordId: number
  feedbackUserId: number
  feedbackTime: string | number | Date
  approveUserId: number
  remark?: string
}

export interface ProFeedbackImportConfirmBatchReqVO {
  importRecordIds: number[]
  rows: ProFeedbackImportConfirmBatchRowReqVO[]
}

export interface ProFeedbackImportRecordPageReqVO {
  pageNo?: number
  pageSize?: number
  id?: number
  importRecordIds?: number[]
  feedbackId?: number
  attributionStatus?: string
}

export interface ProFeedbackImportCandidateVO {
  targetType?: 'CURRENT_ORDER' | 'EXTERNAL_OTHER_ORDER'
  scheduleOrderId?: number
  scheduleOrderCode: string
  scheduleOrderProcessId: number
  workOrderId?: number
  targetOrderLabel?: string
  externalOtherOrder?: boolean
  workOrderCode: string
  productId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  processId: number
  processCode?: string
  processName?: string
  plannedQuantity?: number
  reportedQuantity?: number
  remainingQuantity?: number
  taskId?: number
  taskCode?: string
  exactWorkOrderMatch?: boolean
  targetProductLabel?: string
  overproduceQuantity?: number
  surplusPoolQuantity?: number
  availableFeedbackQuantity?: number
  selectedQuantity?: number
}

export interface ProFeedbackImportAttributeReqVO {
  importRecordId: number
  targetType?: 'CURRENT_ORDER' | 'EXTERNAL_OTHER_ORDER'
  scheduleOrderId?: number
  scheduleOrderProcessId?: number
  feedbackQuantity?: number
  allocations?: ProFeedbackImportAttributeAllocationReqVO[]
}

export interface ProFeedbackImportAttributeAllocationReqVO {
  targetType: 'CURRENT_ORDER' | 'EXTERNAL_OTHER_ORDER'
  scheduleOrderId?: number
  scheduleOrderProcessId?: number
  feedbackQuantity: number
}

export interface ProFeedbackEdhrEntryContextReqVO {
  workOrderId: number
  taskId: number
  routeId: number
  processId: number
  workstationId: number
  batchCode: string
  routeProcessId?: number
}

export interface ProFeedbackEdhrEntryContextVO {
  workOrderId?: number
  taskId?: number
  routeId?: number
  routeCode?: string
  routeName?: string
  processId?: number
  processCode?: string
  processName?: string
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  batchCode?: string
  routeProcessId?: number
  batchRecordReportId?: string
  batchRecordReportCode?: string
  batchRecordReportName?: string
  formSlotType?: EdhrExecutionFormSlotType
  recordCategory?: EdhrRecordCategory
  validationProfile?: EdhrValidationProfile
  recordbookEnabled?: boolean | null
  requiredPolicy?: EdhrExecutionRequiredPolicy
  ownerRoleKey?: EdhrExecutionOwnerRoleKey
  archiveVisibility?: EdhrExecutionArchiveVisibility
  permissionScopeId?: number | null
  slotConfigSnapshotHash?: string | null
  canOpen?: boolean
  bindingResolved?: boolean
  activeContextKey?: string
}

export interface ProFeedbackEdhrOpenOrCreateReqVO extends ProFeedbackEdhrEntryContextReqVO {
  batchRecordReportId: string
  recordCategory?: EdhrRecordCategory
  validationProfile?: EdhrValidationProfile
  permissionScopeId?: number | null
}

export interface ProFeedbackEdhrOpenOrCreateRespVO {
  id: number
  executionCode?: string
  workOrderId?: number
  taskId?: number
  routeId?: number
  routeCode?: string
  routeName?: string
  processId?: number
  processCode?: string
  processName?: string
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  batchCode?: string
  routeProcessId?: number
  batchRecordReportId?: string
  batchRecordReportCode?: string
  batchRecordReportName?: string
  formSlotType?: EdhrExecutionFormSlotType
  recordCategory?: EdhrRecordCategory
  validationProfile?: EdhrValidationProfile
  requiredPolicy?: EdhrExecutionRequiredPolicy
  ownerRoleKey?: EdhrExecutionOwnerRoleKey
  archiveVisibility?: EdhrExecutionArchiveVisibility
  permissionScopeId?: number | null
  slotConfigSnapshotHash?: string | null
  routeBindingId?: number
  routeBindingSnapshotHash?: string
  recordbookEnabled?: boolean | null
  canOpen?: boolean
  bindingResolved?: boolean
  created?: boolean
  activeContextKey?: string
  status?: number
}

export interface ProFeedbackEdhrExecutionCellValueVO {
  rowIndex: number
  columnIndex: number
  value: string | number | boolean | null
  valueType?: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'SIGNATURE' | 'JSON' | 'NULL'
  valueDisplay?: string
  valueHash?: string
  unit?: string
}

export interface ProFeedbackEdhrSnapshotFieldVO {
  rowIndex?: number
  columnIndex?: number
  fieldKey?: string
  fieldPath?: string
  valueType?: string
  label?: string
  name?: string
  title?: string
  placeholder?: string
  helpText?: string
  required?: boolean
  readonly?: boolean
  disabled?: boolean
  inputType?: string
  componentType?: string
  type?: string
  value?: unknown
  defaultValue?: unknown
  options?: Array<Record<string, unknown> | string | number | boolean>
  [key: string]: unknown
}

export interface ProFeedbackEdhrAssistRowFieldVO {
  rowIndex: number
  columnIndex: number
}

export interface ProFeedbackEdhrAssistRowVO {
  rowKey: string
  description: string
  sort: number
  fields: ProFeedbackEdhrAssistRowFieldVO[]
}

export interface ProFeedbackEdhrExecutionSnapshotVO {
  fields?: ProFeedbackEdhrSnapshotFieldVO[]
  assistRows?: ProFeedbackEdhrAssistRowVO[]
  [key: string]: unknown
}

export interface ProFeedbackEdhrExecutionAttachmentSummaryVO {
  id?: number
  auditBatchId?: number
  signatureId?: number
  executionId?: number
  workTaskId?: number
  rowIndex?: number
  columnIndex?: number
  fieldKey?: string
  fieldPath?: string
  fieldLabel?: string
  attachmentType?: string
  attachmentGroupKey?: string
  attachmentAction?: string
  versionNo?: number
  fileId?: number
  fileUrl?: string
  storageConfigId?: number
  storagePath?: string
  fileName?: string
  contentType?: string
  fileSize?: number
  sha256?: string
  storageRetentionHash?: string
  previousAttachmentHash?: string
  attachmentHash?: string
  operatorId?: number
  operatorName?: string
  operatedAt?: string | number | Date
  reasonCategory?: string
  reasonText?: string
}

export interface ProFeedbackEdhrReviewCandidateUserVO {
  userId: number
  userName?: string
}

export interface ProFeedbackEdhrReviewAssigneeOptionVO {
  signatureCellKey: string
  signatureRowIndex?: number
  signatureColumnIndex?: number
  reviewSourceType?: string
  reviewSourceId?: number
  reviewSourceIds?: number[]
  reviewSourceName?: string
  candidates: ProFeedbackEdhrReviewCandidateUserVO[]
}

export interface ProFeedbackEdhrReviewAssigneeSelectionVO {
  signatureCellKey: string
  selectedUserId: number
}

export interface ProFeedbackEdhrExecutionVO {
  id: number
  executionCode?: string
  templateId?: number
  templateCode?: string
  templateName?: string
  workOrderId?: number
  workOrderCode?: string
  routeId?: number
  routeCode?: string
  routeName?: string
  processId?: number
  processCode?: string
  processName?: string
  batchCode?: string
  status?: number
  revisionRootExecutionId?: number
  revisionNo?: number
  sourceRejectedExecutionId?: number
  supersededByExecutionId?: number
  revisionReason?: string
  revisionParentHash?: string
  activeRevisionFlag?: boolean
  routeProcessId?: number
  taskId?: number
  batchRecordReportId?: string
  batchRecordReportCode?: string
  batchRecordReportName?: string
  formSlotType?: EdhrExecutionFormSlotType
  recordCategory?: EdhrRecordCategory
  validationProfile?: EdhrValidationProfile
  requiredPolicy?: EdhrExecutionRequiredPolicy
  ownerRoleKey?: EdhrExecutionOwnerRoleKey
  archiveVisibility?: EdhrExecutionArchiveVisibility
  permissionScopeId?: number | null
  slotConfigSnapshotHash?: string | null
  routeBindingId?: number
  routeBindingSnapshotHash?: string
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  sheetLayoutJson?: string
  metaJson?: string
  executionSnapshotJson?: string
  cellValues?: ProFeedbackEdhrExecutionCellValueVO[]
  canOpen?: boolean
  canGenerateArchive?: boolean
  canDownloadArchive?: boolean
  preReleaseEditable?: boolean
  preReleaseEditReason?: string
  bindingResolved?: boolean
  created?: boolean
  activeContextKey?: string
  processInstanceId?: string
  submittedBy?: number
  submittedAt?: string | number | Date
  approvedBy?: number
  approvedAt?: string | number | Date
  rejectedBy?: number
  rejectedAt?: string | number | Date
  rejectReason?: string
  approvalSnapshotStatus?: 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  cellValuesHash?: string
  fieldAuditRevision?: number
  fieldAuditHeadHash?: string
  fieldAuditLastBatchId?: number
  reviewAssigneeOptions?: ProFeedbackEdhrReviewAssigneeOptionVO[]
  reviewAssigneeOptionError?: string
  attachmentSummaries?: ProFeedbackEdhrExecutionAttachmentSummaryVO[]
  assistSwitchTasks?: EdhrBatchExecutionTaskRespVO[]
  closedAt?: string
  remark?: string
  creator?: string
  createTime?: string | number | Date
  updateTime?: string | number | Date
  [key: string]: unknown
}

export interface ProFeedbackEdhrExecutionPageReqVO extends PageParam {
  templateId?: number
  workOrderId?: number
  batchCode?: string
  status?: number
  taskId?: number
  routeProcessId?: number
  activeContextKey?: string
}

export interface ProFeedbackEdhrExecutionRowVO {
  id: number
  executionCode?: string
  templateId?: number
  templateCode?: string
  templateName?: string
  workOrderId?: number
  workOrderCode?: string
  routeId?: number
  routeCode?: string
  routeName?: string
  processId?: number
  processCode?: string
  processName?: string
  routeProcessId?: number
  taskId?: number
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  batchRecordReportId?: string
  batchRecordReportCode?: string
  batchRecordReportName?: string
  formSlotType?: EdhrExecutionFormSlotType
  recordCategory?: EdhrRecordCategory
  validationProfile?: EdhrValidationProfile
  requiredPolicy?: EdhrExecutionRequiredPolicy
  ownerRoleKey?: EdhrExecutionOwnerRoleKey
  archiveVisibility?: EdhrExecutionArchiveVisibility
  permissionScopeId?: number | null
  slotConfigSnapshotHash?: string | null
  routeBindingId?: number
  routeBindingSnapshotHash?: string
  batchCode?: string
  status?: number
  revisionRootExecutionId?: number
  revisionNo?: number
  sourceRejectedExecutionId?: number
  supersededByExecutionId?: number
  revisionReason?: string
  revisionParentHash?: string
  activeRevisionFlag?: boolean
  canGenerateArchive?: boolean
  canDownloadArchive?: boolean
  processInstanceId?: string
  approvalSnapshotStatus?: 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  closedAt?: string
  canOpen?: boolean
  bindingResolved?: boolean
  created?: boolean
  activeContextKey?: string
  creator?: string
  createTime?: string | number | Date
  updateTime?: string | number | Date
}

export interface ProFeedbackEdhrSaveDraftReqVO {
  id: number
  cellValues: ProFeedbackEdhrExecutionCellValueVO[]
  remark?: string
}

export interface ProFeedbackEdhrSubmitReqVO {
  id: number
  workTaskId: EdhrRouteId
  password: string
  comment?: string
  signatureTime?: EdhrSignatureTimeReqVO
  reviewAssigneeSelections?: ProFeedbackEdhrReviewAssigneeSelectionVO[]
}

export interface ProFeedbackEdhrFormReviewSignReqVO {
  executionId: number
  workTaskId: EdhrRouteId
  password: string
  comment?: string
  signatureTime?: EdhrSignatureTimeReqVO
}

export interface ProFeedbackEdhrFormReviewSignRespVO {
  executionId: number
  status: number
  signatureId: number
  actionType: 'FORM_REVIEW'
  meaningText: string
  cellValuesHash?: string
  fieldAuditRevision?: number
  fieldAuditHeadHash?: string
}

export const PRO_FEEDBACK_IMPORT_THIRD_PARTY_XLSX_URL = '/mes/pro/feedback/import-third-party-xlsx'
export const PRO_FEEDBACK_IMPORT_DIRECT_WORK_REPORT_XLSX_URL =
  '/mes/pro/feedback/import-direct-work-report-xlsx'
export const PRO_FEEDBACK_SIMULATE_THIRD_PARTY_XLSX_URL =
  '/mes/pro/feedback/simulate-import-third-party-xlsx'

// MES 鐢熶骇鎶ュ伐 API
export const ProFeedbackApi = {
  // 鏌ヨ鐢熶骇鎶ュ伐鍒嗛〉
  getFeedbackPage: async (params: any) => {
    return await request.get({ url: `/mes/pro/feedback/page`, params })
  },
  // 鏌ヨ鐢熶骇鎶ュ伐璇︽儏
  getFeedback: async (id: number) => {
    return await request.get({ url: `/mes/pro/feedback/get?id=` + id })
  },
  // 鏂板鐢熶骇鎶ュ伐
  createFeedback: async (data: ProFeedbackVO) => {
    return await request.post({ url: `/mes/pro/feedback/create`, data })
  },
  // 一线报工与记录本一体提交
  frontlineSubmit: async (data: ProFrontlineFeedbackSubmitReqVO) => {
    return await request.post<ProFrontlineFeedbackSubmitRespVO>({
      url: `/mes/pro/feedback/frontline/submit`,
      data
    })
  },
  // 淇敼鐢熶骇鎶ュ伐
  updateFeedback: async (data: ProFeedbackVO) => {
    return await request.put({ url: `/mes/pro/feedback/update`, data })
  },
  // 鍒犻櫎鐢熶骇鎶ュ伐
  deleteFeedback: async (id: number) => {
    return await request.delete({ url: `/mes/pro/feedback/delete?id=` + id })
  },
  // 瀵煎嚭鐢熶骇鎶ュ伐 Excel
  exportFeedback: async (params: any) => {
    return await request.download({ url: `/mes/pro/feedback/export-excel`, params })
  },
  // 鎻愪氦鎶ュ伐
  submitFeedback: async (id: number) => {
    return await request.put({ url: `/mes/pro/feedback/submit?id=` + id })
  },
  // 椹冲洖鎶ュ伐
  rejectFeedback: async (id: number) => {
    return await request.put({ url: `/mes/pro/feedback/reject?id=` + id })
  },
  // 瀹℃壒鎶ュ伐锛堣繑鍥炲鎵瑰悗鐨勭姸鎬侊級
  approveFeedback: async (id: number) => {
    return await request.put({ url: `/mes/pro/feedback/approve?id=` + id })
  },
  // 获取 eDHR 执行入口上下文
  getEdhrEntryContext: async (params: ProFeedbackEdhrEntryContextReqVO) => {
    return await request.get<ProFeedbackEdhrEntryContextVO>({
      url: `/mes/pro/batch-record-execution/entry-context`,
      params
    })
  },
  // 按上下文打开或创建 eDHR 执行实例
  openOrCreateEdhrByContext: async (data: ProFeedbackEdhrOpenOrCreateReqVO) => {
    return await request.post<ProFeedbackEdhrOpenOrCreateRespVO>({
      url: `/mes/pro/batch-record-execution/open-or-create-by-context`,
      data
    })
  },
  // 获取 eDHR 执行记录
  getEdhrExecution: async (id: EdhrRouteId, workTaskId?: EdhrRouteId) => {
    return await request.get<ProFeedbackEdhrExecutionVO>({
      url: `/mes/pro/batch-record-execution/get`,
      params: { id, workTaskId }
    })
  },
  // 获取 eDHR 执行分页
  getEdhrExecutionPage: async (params: ProFeedbackEdhrExecutionPageReqVO) => {
    return await request.get<PageResult<ProFeedbackEdhrExecutionRowVO[]>>({
      url: `/mes/pro/batch-record-execution/page`,
      params
    })
  },
  // 保存 eDHR 执行草稿
  saveEdhrExecutionDraft: async (data: ProFeedbackEdhrSaveDraftReqVO) => {
    return await request.put({
      url: `/mes/pro/batch-record-execution/save-draft`,
      data
    })
  },
  // 提交 eDHR 执行记录
  submitEdhrExecution: async (data: ProFeedbackEdhrSubmitReqVO) => {
    return await request.put({
      url: `/mes/pro/batch-record-execution/submit`,
      data,
      headers: { 'Content-Type': 'application/json' }
    })
  },
  // 复核签名 eDHR 执行记录
  cosignEdhrExecution: async (data: ProFeedbackEdhrFormReviewSignReqVO) => {
    return await request.put<ProFeedbackEdhrFormReviewSignRespVO>({
      url: `/mes/pro/batch-record-execution/cosign`,
      data
    })
  },
  // 获取设备账号可切换工序
  getFrontlineDeviceAccountProcesses: async () => {
    return await request.get<FrontlineDeviceRouteProcessVO[]>({
      url: `/mes/pro/feedback/frontline/device-account/processes`
    })
  },
  // 获取 PQC 当前活跃订单
  getFrontlinePqcActiveOrders: async () => {
    return await request.get<FrontlineActiveOrderVO[]>({
      url: `/mes/pro/feedback/frontline/device-account/pqc/active-orders`
    })
  },
  // 获取 PQC 活跃订单对应工艺路线工序
  getFrontlinePqcActiveOrderProcesses: async (params: {
    workOrderId: number
    routeId: number
  }) => {
    return await request.get<FrontlineDeviceRouteProcessVO[]>({
      url: `/mes/pro/feedback/frontline/device-account/pqc/active-order/processes`,
      params
    })
  },
  // 获取当前工序可切换员工
  getFrontlineEmployeeCandidates: async (params: {
    routeId: number
    routeProcessId: number
    processId: number
  }) => {
    return await request.get<FrontlineEmployeeCandidateVO[]>({
      url: `/mes/pro/feedback/frontline/device-account/employee-candidates`,
      params
    })
  },
  // 获取生产组长维护的员工填报运行态配置
  getFrontlineRuntimeConfig: async (params: {
    routeId: number
    routeProcessId: number
    processId: number
  }) => {
    return await request.get<FrontlineRuntimeConfigVO>({
      url: `/mes/pro/feedback/frontline/device-account/runtime-config`,
      params
    })
  },
  // 获取 PQC 员工 + PQC 组长
  getFrontlinePqcEmployeeCandidates: async () => {
    return await request.get<FrontlineEmployeeCandidateVO[]>({
      url: `/mes/pro/feedback/frontline/device-account/pqc/personnel`
    })
  },
  // 切换实际填写员工并重新加载当前模板
  switchFrontlineActualEmployee: async (data: FrontlineSwitchActualEmployeeReqVO) => {
    return await request.post<FrontlineSwitchActualEmployeeRespVO>({
      url: `/mes/pro/feedback/frontline/device-account/switch-employee`,
      data
    })
  },
  // PQC 切换实际填写员工并重新加载当前模板
  switchFrontlinePqcActualEmployee: async (data: FrontlinePqcSwitchActualEmployeeReqVO) => {
    return await request.post<FrontlineSwitchActualEmployeeRespVO>({
      url: `/mes/pro/feedback/frontline/device-account/pqc/switch-employee`,
      data
    })
  },
  // PQC 检验提交到工序池
  submitFrontlinePqcInspection: async (data: FrontlinePqcInspectionSubmitReqVO) => {
    return await request.post<FrontlinePqcInspectionSubmitRespVO>({
      url: `/mes/pro/feedback/frontline/device-account/pqc/submit`,
      data
    })
  },
  // 只读确认 PQC 正式提交回执
  getFrontlinePqcSubmitReceipt: async (params: { pqcTaskId: number }) => {
    return await request.get<FrontlinePqcInspectionSubmitRespVO | null>({
      url: `/mes/pro/feedback/frontline/device-account/pqc/submit-receipt`,
      params
    })
  },
  // 瀵煎叆绗笁鏂圭敓浜ф姤宸?Excel
  importThirdPartyXlsx: async (data: FormData) => {
    const result = await request.upload<{ data: ThirdPartyFeedbackImportResultVO }>({
      url: PRO_FEEDBACK_IMPORT_THIRD_PARTY_XLSX_URL,
      data
    })
    return result.data
  },
  importDirectWorkReportXlsx: async (data: FormData) => {
    const result = await request.upload<{ data: ThirdPartyFeedbackImportResultVO }>({
      url: PRO_FEEDBACK_IMPORT_DIRECT_WORK_REPORT_XLSX_URL,
      data
    })
    return result.data
  },
  simulateThirdPartyXlsxImport: async (processCount: number) => {
    return await request.post<ThirdPartyFeedbackImportResultVO>({
      url: PRO_FEEDBACK_SIMULATE_THIRD_PARTY_XLSX_URL,
      params: { processCount }
    })
  },
  getImportRecordPage: async (params: ProFeedbackImportRecordPageReqVO) => {
    return await request.get<PageResult<ProFeedbackImportRecordVO[]>>({
      url: `/mes/pro/feedback/import-record/page`,
      params
    })
  },
  getImportRecordBatchSummary: async (importRecordIds: number[]) => {
    return await request.get<ProFeedbackImportBatchSummaryVO>({
      url: `/mes/pro/feedback/import-record/batch-summary`,
      params: { importRecordIds: importRecordIds.join(',') }
    })
  },
  getImportRecordCandidates: async (importRecordId: number) => {
    return await request.get<ProFeedbackImportCandidateVO[]>({
      url: `/mes/pro/feedback/import-record/candidates`,
      params: { importRecordId }
    })
  },
  attributeImportRecord: async (data: ProFeedbackImportAttributeReqVO) => {
    return await request.post<number>({
      url: `/mes/pro/feedback/import-record/attribute`,
      data
    })
  },
  reattributeImportRecord: async (data: ProFeedbackImportAttributeReqVO) => {
    return await request.post<number>({
      url: `/mes/pro/feedback/import-record/reattribute`,
      data
    })
  },
  confirmImportRecordBatch: async (data: ProFeedbackImportConfirmBatchReqVO) => {
    return await request.post({
      url: `/mes/pro/feedback/import-record/confirm-batch`,
      data
    })
  }
}
