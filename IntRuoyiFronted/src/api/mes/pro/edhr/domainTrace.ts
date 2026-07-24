import request from '@/config/axios'
import type { EdhrRouteId } from './batchExecution'

const EDHR_DOMAIN_TRACE_BASE_URL = '/mes/pro/batch-record-execution/domain-trace'

export const EDHR_DOMAIN_TRACE_QUERY_PERMISSION =
  'mes:pro-batch-record-execution:domain-trace-query'
export const EDHR_DOMAIN_TRACE_VERIFY_PERMISSION =
  'mes:pro-batch-record-execution:domain-trace-verify'

export type EdhrDomainTraceStatus = 'VERIFIED' | 'BLOCKED' | 'UNVERIFIED'

export type EdhrDomainTraceItemStatus = 'VERIFIED' | 'BLOCKED' | 'MISSING' | 'NOT_APPLICABLE'

export interface EdhrDomainTraceDetailReqVO {
  executionId: EdhrRouteId
}

export interface EdhrDomainTracePageReqVO extends PageParam {
  executionId?: EdhrRouteId
  executionCode?: string
  workOrderCode?: string
  batchCode?: string
  status?: EdhrDomainTraceStatus
  verifiedAtStart?: string
  verifiedAtEnd?: string
}

export interface EdhrDomainTraceBlockerVO {
  itemType: string
  itemKey: string
  blockerCode: string
  blockerMessage: string
}

export interface EdhrDomainTraceItemVO {
  itemType: string
  itemKey: string
  itemName?: string
  sourceId?: string
  sourceCode?: string
  sourceVersion?: string
  snapshotJson?: string
  snapshotHash?: string
  status: EdhrDomainTraceItemStatus | EdhrDomainTraceStatus | string
  blockerReason?: string
}

export interface EdhrDomainTracePageRowVO {
  executionId: number
  executionCode?: string
  workOrderCode?: string
  batchCode?: string
  status: EdhrDomainTraceStatus
  domainTraceHash?: string
  domainTraceSnapshotId?: string
  verifiedAt?: string
  blockerCount?: number
  itemCount?: number
  blockers?: EdhrDomainTraceBlockerVO[]
  items?: EdhrDomainTraceItemVO[]
}

export interface EdhrDomainTraceDetailRespVO extends EdhrDomainTracePageRowVO {
  verifiedBy?: number
  verifiedByName?: string
  blockers: EdhrDomainTraceBlockerVO[]
  items: EdhrDomainTraceItemVO[]
}

export interface EdhrDomainTraceVerifyReqVO {
  executionId: EdhrRouteId
  expectedDomainTraceHash?: string
}

export interface EdhrDomainTraceVerifyRespVO extends EdhrDomainTraceDetailRespVO {}

export const EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP: Record<EdhrDomainTraceStatus, string> = {
  VERIFIED: '已校验',
  BLOCKED: '已阻塞',
  UNVERIFIED: '未校验'
}

export const EDHR_DOMAIN_TRACE_STATUS_TAG_TYPE_MAP: Record<EdhrDomainTraceStatus, string> = {
  VERIFIED: 'success',
  BLOCKED: 'danger',
  UNVERIFIED: 'warning'
}

export const getEdhrDomainTraceDetail = async (params: EdhrDomainTraceDetailReqVO) => {
  return await request.get<EdhrDomainTraceDetailRespVO>({
    url: `${EDHR_DOMAIN_TRACE_BASE_URL}/detail`,
    params
  })
}

export const getEdhrDomainTracePage = async (params: EdhrDomainTracePageReqVO) => {
  return await request.get<PageResult<EdhrDomainTracePageRowVO[]>>({
    url: `${EDHR_DOMAIN_TRACE_BASE_URL}/page`,
    params
  })
}

export const verifyEdhrDomainTrace = async (data: EdhrDomainTraceVerifyReqVO) => {
  return await request.post<EdhrDomainTraceVerifyRespVO>({
    url: `${EDHR_DOMAIN_TRACE_BASE_URL}/verify`,
    data
  })
}
