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
