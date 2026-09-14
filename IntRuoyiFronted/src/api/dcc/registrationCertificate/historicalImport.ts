import request from '@/config/axios'

export interface DccRegistrationCertificateHistoricalImportPageReqVO extends PageParam {
  sourceHash?: string
}

export interface DccRegistrationCertificateHistoricalImportRespVO {
  id: number
  sourceHash: string
  sourceRow: number
  payloadHash: string
  outcomeCertificateId?: number
  outcomeVersionId?: number
  outcomeSnapshotId?: number
  restrictedReasons: string[]
  ownerCompanyId?: number
  ownerCompanyCode?: string
  ownerCompanyName?: string
  certificateId?: number
  certificateNo?: string
  versionNo?: number
  productName?: string
  actorId?: number
  result: string
  resultCode?: string
  requestTraceId: string
  occurredAt: string
}

export const getHistoricalImportPage = async (
  params: DccRegistrationCertificateHistoricalImportPageReqVO
): Promise<PageResult<DccRegistrationCertificateHistoricalImportRespVO[]>> => {
  return await request.get({ url: '/dcc/registration-certificates/historical-import/page', params })
}
