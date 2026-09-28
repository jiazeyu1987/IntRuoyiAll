import request from '@/config/axios'

export type ReverseTraceCategory =
  | 'FIELD'
  | 'PARAMETER'
  | 'EQUIPMENT'
  | 'PERSON'
  | 'INSPECTION'
  | 'MATERIAL'

export interface ReverseTraceTargetScope {
  kind: 'RELEASED_HISTORY'
  releaseApprovedFrom?: string
  releaseApprovedTo?: string
}

export interface ReverseTraceCondition {
  conditionId: string
  evidenceKey: string
  sourceView: string
  operator: string
  value: unknown
  qualifiers?: Record<string, string>
}

export interface ReverseTraceCatalogRequest {
  anchorBatchExecutionId: string
  targetScope: ReverseTraceTargetScope
  category?: ReverseTraceCategory
  pageNo: number
  pageSize: number
}

export interface ReverseTraceCatalogItem {
  evidenceKey: string
  category: ReverseTraceCategory
  sourceView: string
  sourceRef?: string
  semanticIdentity?: string
  label?: string
  valueType?: string
  savedValue?: string
  unit?: string
  qualifiers?: Record<string, string>
  allowedOperators?: string[]
  allowedValues?: string[]
  recordedAt?: string
  recordStatus?: string
  recordedStandard?: string
  parameterStatus?: string
}

export interface ReverseTraceCatalogResponse {
  anchorBatchExecutionId: string
  catalogVersion?: string
  chainContext?: ReverseTraceChainContext
  categories: Array<{
    category: ReverseTraceCategory
    status: string
    reasonCode?: string
    reason?: string
  }>
  items: ReverseTraceCatalogItem[]
  total?: number | null
}

export interface ReverseTraceQueryRequest {
  anchorBatchExecutionId: string
  catalogVersion: string
  sourceVersionDigest?: string
  targetScope: ReverseTraceTargetScope
  logic: 'AND'
  conditions: ReverseTraceCondition[]
  pageNo: number
  pageSize: number
}

export interface ReverseTraceQueryResponse {
  catalogVersion: string
  queryStatus: 'MATCHED' | 'NO_MATCH' | 'BLOCKED' | string
  coverageStatus: 'COMPLETE' | 'BLOCKED' | string
  reasonCode?: string
  reason?: string
  queryHash?: string
  normalizedQuery?: ReverseTraceQueryRequest
  list: Array<{
    batchExecutionId: string
    batchExecutionCode?: string
    batchCode?: string
    workOrderCode?: string
    productName?: string
    releaseStatus?: string
    anchor?: boolean
    matchCount?: number
    conditionIds?: string[]
    matchSummary?: string
  }>
  total?: number
}

export interface ReverseTraceEvidenceRequest extends ReverseTraceQueryRequest {
  queryHash: string
  targetBatchExecutionId: string
}

export interface ReverseTraceEvidenceResponse {
  catalogVersion: string
  queryHash: string
  evidenceStatus: 'MATCHED' | 'BLOCKED' | string
  reasonCode?: string
  reason?: string
  targetBatchExecutionId: string
  chainContext?: ReverseTraceChainContext
  items: Array<{
    conditionId: string
    targetBatchExecutionId?: string
    sourceIdentity?: string
    sourceStage?: string
    sourceAction?: string
    sourceRef?: string
    sourceContext?: Record<string, string>
    actualValue?: string
    recordedStandard?: string
    recordStatus?: string
    occurredAt?: string
    recordedAt?: string
  }>
  total?: number
}

export interface ReverseTraceChainContext {
  anchorBatchExecutionId?: string
  targetBatchExecutionId?: string
  batchExecutionCode?: string
  activeOrderId?: string
  workOrderId?: string
  routeVersionId?: string
  qaVersionRefs?: string[]
  completionReceiptId?: string
  releaseApplicationId?: string
  pqcReleaseWorkTaskId?: string
  releaseTransactionId?: string
  releaseApprovalWorkTaskId?: string
  sourceStatus?: string
}

const REVERSE_TRACE_BASE_URL = '/mes/pro/edhr-batch-execution/reverse-trace'

export const getReverseTraceCatalog = async (params: ReverseTraceCatalogRequest) => {
  return await request.get<ReverseTraceCatalogResponse>({
    url: REVERSE_TRACE_BASE_URL + '/catalog',
    params
  })
}

export const queryReverseTrace = async (data: ReverseTraceQueryRequest) => {
  return await request.post<ReverseTraceQueryResponse>({
    url: REVERSE_TRACE_BASE_URL + '/query',
    data
  })
}

export const getReverseTraceEvidence = async (data: ReverseTraceEvidenceRequest) => {
  return await request.post<ReverseTraceEvidenceResponse>({
    url: REVERSE_TRACE_BASE_URL + '/evidence',
    data
  })
}
