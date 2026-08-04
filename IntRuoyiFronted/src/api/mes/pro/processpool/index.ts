import request from '@/config/axios'

export interface ProcessPoolTimelinePageReqVO {
  pageNo?: number
  pageSize?: number
  submitDate: string
  employeeUserId?: number
  processId?: number
  deviceId?: number
  templateType?: string
  workOrderId?: number
  workOrderCode?: string
}

export interface ProcessPoolTimelineReadonlyActionsVO {
  canModifyOriginalRecord: boolean
  canGenerateAuditCopy: boolean
  canExecuteFifoAllocation: boolean
}

export interface ProcessPoolTimelineEventVO {
  id: number
  processPoolEventId?: number
  processPoolId?: number
  submittedAt?: string | number | Date
  loginUserId?: number
  loginUserName?: string
  actualEmployeeUserId?: number
  actualEmployeeUserName?: string
  signatureEmployeeUserId?: number
  signatureEmployeeUserName?: string
  electronicSignatureId?: number
  deviceId?: number
  deviceCode?: string
  deviceName?: string
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  routeId?: number
  routeCode?: string
  routeProcessId?: number
  processId?: number
  processCode?: string
  processName?: string
  templateType?: string
  templateTypeName?: string
  workOrderId?: number
  workOrderCode?: string
  workOrderName?: string
  sourceFeedbackId?: number
  sourceRecordbookEntryId?: number
  sourceRecordbookEventId?: number
  submittedSummary?: string
  originalPayloadJson?: string
  pqcResult?: string
  pqcSummary?: string
  fifoAllocationStatus?: string
  fifoAllocationSummary?: string
  auditCopyStatus?: string
  auditCopySummary?: string
  submissionReviewStatus?: string
  submissionReviewRemark?: string
  submissionReviewLeaderUserId?: number
  submissionReviewedAt?: string | number | Date
  modificationHistorySummary?: string
}

export interface ProcessPoolTimelineDetailVO extends ProcessPoolTimelineEventVO {
  readonlyActions?: ProcessPoolTimelineReadonlyActionsVO
}

export interface ProductionExecutionTraceBlockerVO {
  code: string
  message: string
  missingObjectType?: string
  resolutionHint?: string
}

export interface ProductionExecutionTraceSourceIdsVO {
  processPoolEventId?: number
  productionSubmitEventId?: number
  pqcEventId?: number
  reviewId?: number
  allocationId?: number
  orderProcessId?: number
  batchRecordExecutionId?: number
  fieldAuditBatchId?: number
}

export interface ProductionExecutionTraceSectionVO {
  status: 'READY' | 'BLOCKED' | 'SKIPPED'
  sourceIds: ProductionExecutionTraceSourceIdsVO
  blockers: ProductionExecutionTraceBlockerVO[]
  lastUpdatedAt?: string | number | Date
}

export interface ProductionExecutionReadOnlyVerificationEntryVO {
  verificationKey: string
  method: string
  path: string
  params: Record<string, unknown>
}

export interface ProductionExecutionEvidenceAnswerVO {
  answerKey: string
  value?: unknown
  section?: string
  sameSource?: boolean
  sourceIds: Record<string, unknown>
  readOnlyVerificationEntries: ProductionExecutionReadOnlyVerificationEntryVO[]
  blockers: ProductionExecutionTraceBlockerVO[]
}

export interface ProductionExecutionSameSourceCheckVO {
  checkKey: string
  passed?: boolean
  sourceIds: Record<string, unknown>
  message?: string
}

export interface ProductionExecutionClosureEvidenceVO {
  processPoolEventId: number
  complete?: boolean
  answers: Record<string, ProductionExecutionEvidenceAnswerVO>
  sameSourceChecks: ProductionExecutionSameSourceCheckVO[]
  blockers: ProductionExecutionTraceBlockerVO[]
}

export interface ProductionExecutionTraceVO {
  processPoolEventId: number
  complete: boolean
  submit: ProductionExecutionTraceSectionVO
  quality: ProductionExecutionTraceSectionVO
  review: ProductionExecutionTraceSectionVO
  allocation: ProductionExecutionTraceSectionVO
  completion: ProductionExecutionTraceSectionVO
  batchRecord: ProductionExecutionTraceSectionVO
  blockers: ProductionExecutionTraceBlockerVO[]
  candidateEvents?: ProcessPoolTimelineEventVO[]
  closureEvidence?: ProductionExecutionClosureEvidenceVO
}

export const getProcessPoolTimelinePage = async (params: ProcessPoolTimelinePageReqVO) => {
  return await request.get<PageResult<ProcessPoolTimelineEventVO[]>>({
    url: '/mes/pro/process-pool/timeline/page',
    params
  })
}

export const getProcessPoolTimelineDetail = async (id: number) => {
  return await request.get<ProcessPoolTimelineDetailVO>({
    url: '/mes/pro/process-pool/timeline/detail',
    params: { id }
  })
}

export const getProductionExecutionTrace = async (processPoolEventId: number) => {
  return await request.get<ProductionExecutionTraceVO>({
    url: '/mes/pro/process-pool/team-leader/production-execution/trace',
    params: { processPoolEventId }
  })
}
