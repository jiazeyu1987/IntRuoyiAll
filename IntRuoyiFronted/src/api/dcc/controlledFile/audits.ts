import request from '@/config/axios'

export interface DccControlledFileAuditPageReqVO extends PageParam {
  accessEventCode?: string
  watermarkTraceCode?: string
  controlledFileId?: number
  userId?: number
  actionType?: string
  result?: string
  failureCode?: string
  occurredAt?: string[]
}

export interface DccControlledFileAuditRespVO {
  id: number
  accessEventId?: number | null
  accessEventCode?: string | null
  watermarkTraceCode?: string | null
  controlledFileId?: number | null
  fileNumber?: string | null
  fileVersionNo?: string | null
  userId?: number | null
  userIdentifier?: string | null
  userDisplayName?: string | null
  deptId?: number | null
  deptName?: string | null
  tenantName?: string | null
  actionType?: string | null
  purpose?: string | null
  result?: string | null
  failureCode?: string | null
  reason?: string | null
  sourceIp?: string | null
  requestId?: string | null
  userAgent?: string | null
  privacyMode?: string | null
  watermarkPayloadJson?: string | null
  occurredAt?: string | null
  issuedAt?: string | null
  expiresAt?: string | null
  createTime?: string | null
}

export const getControlledFileAuditPage = async (
  params: DccControlledFileAuditPageReqVO
): Promise<PageResult<DccControlledFileAuditRespVO[]>> => {
  return await request.get({ url: '/dcc/controlled-file-audits/page', params })
}
