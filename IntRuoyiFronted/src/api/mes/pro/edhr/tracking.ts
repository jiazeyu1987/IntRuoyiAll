import request from '@/config/axios'
import type { EdhrExecutionStatus } from './approval'

export type EdhrTrackingEventType =
  | 'CREATE'
  | 'SAVE_DRAFT'
  | 'FIELD_CHANGE'
  | 'FORM_REVIEW'
  | 'SUBMIT'
  | 'REVIEW_APPROVE'
  | 'APPROVE'
  | 'REJECT'
  | 'ARCHIVE_SEAL'

export interface EdhrTrackingPageReqVO extends PageParam {
  executionCode?: string
  workOrderCode?: string
  batchCode?: string
  processId?: number
  workstationId?: number
  status?: EdhrExecutionStatus
  submittedBy?: number
  approvedBy?: number
  processInstanceId?: string
  actorName?: string
  occurredAtStart?: string
  occurredAtEnd?: string
}

export interface EdhrTrackingRowVO {
  executionId: number
  executionCode: string
  workOrderId: number
  workOrderCode: string
  batchId?: number
  batchCode: string
  processName?: string
  workstationName?: string
  status: EdhrExecutionStatus
  processInstanceId: string
  currentNodeName?: string
  currentAssigneeNames?: string[]
  lastEventType: EdhrTrackingEventType
  lastEvidenceCategory?: EdhrTrackingEvidenceCategory
  lastEvidenceCategoryName?: string
  lastEventReason?: string
  lastEventAt: string
  closedAt?: string
  archiveStatus?: 'GENERATING' | 'SEALED' | 'FAILED'
}

export type EdhrTrackingEvidenceCategory =
  | 'ORDINARY_FILL_SIGNATURE'
  | 'RELEASE_REVIEW_APPROVAL'
  | 'HISTORICAL_FORM_REVIEW_APPROVAL'
  | 'ARCHIVE_SEAL'
  | 'TECHNICAL_TRACE'

export interface EdhrTrackingEventVO {
  eventId: number
  executionId: number
  eventType: EdhrTrackingEventType
  actionType?: 'FORM_REVIEW' | 'SUBMIT' | 'REVIEW_APPROVE' | 'APPROVE' | 'REJECT' | 'ARCHIVE_SEAL'
  evidenceCategory?: EdhrTrackingEvidenceCategory
  evidenceCategoryName?: string
  processInstanceId?: string
  bpmTaskId?: string
  taskDefinitionKey?: string
  nodeName?: string
  actorId?: number
  actorName?: string
  result?: 'PASS' | 'REJECT' | 'FAIL'
  comment?: string
  rejectReason?: string
  signatureId?: number
  occurredAt: string
}

const normalizePositiveIdParam = (value: unknown) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const buildEdhrTrackingPageParams = (params: EdhrTrackingPageReqVO) => ({
  ...params,
  processId: normalizePositiveIdParam(params.processId)
})

export const getEdhrTrackingPage = async (params: EdhrTrackingPageReqVO) => {
  return await request.get<PageResult<EdhrTrackingRowVO[]>>({
    url: '/mes/pro/batch-record-execution/tracking-page',
    params: buildEdhrTrackingPageParams(params)
  })
}

export const getEdhrTrackingTimeline = async (executionId: number) => {
  return await request.get<EdhrTrackingEventVO[]>({
    url: '/mes/pro/batch-record-execution/tracking-timeline',
    params: { executionId }
  })
}
