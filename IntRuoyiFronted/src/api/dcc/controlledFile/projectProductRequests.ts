import request from '@/config/axios'

export interface DccProjectProductCreateReqVO {
  projectName: string
  projectCode: string
  projectLeader: string
  productCode: string
  productName: string
  classification: '一类' | '二类' | '三类'
  remark?: string
}

export interface DccProjectProductCreateRespVO extends DccProjectProductCreateReqVO {
  id: number
  status: string
  applicantUserId?: number
  reviewerUserId?: number
  approverUserId?: number
  reviewReason?: string
  approvalReason?: string
  rejectReason?: string
  writeErrorCode?: string
  writeErrorMessage?: string
  generatedProjectCodeId?: number
  generatedProductCatalogId?: number
  relationId?: number
  submittedTime?: string
  reviewedTime?: string
  approvedTime?: string
  completedTime?: string
  failedTime?: string
}

export interface DccProjectProductApprovalActionReqVO {
  reason: string
}

const requestUrl = '/dcc/project-product-requests'

export const createDccProjectProductRequest = async (
  data: DccProjectProductCreateReqVO
): Promise<number> => {
  return await request.post({ url: `${requestUrl}/create`, data })
}

export const getDccProjectProductRequests = async (): Promise<DccProjectProductCreateRespVO[]> => {
  return await request.get({ url: `${requestUrl}/pending` })
}

export const reviewDccProjectProductRequest = async (
  id: number,
  data: DccProjectProductApprovalActionReqVO,
  approve: boolean
): Promise<DccProjectProductCreateRespVO> => {
  return await request.post({
    url: `${requestUrl}/${id}/review/${approve ? 'approve' : 'reject'}`,
    data
  })
}

export const approveDccProjectProductRequest = async (
  id: number,
  data: DccProjectProductApprovalActionReqVO,
  approve: boolean
): Promise<DccProjectProductCreateRespVO> => {
  return await request.post({
    url: `${requestUrl}/${id}/approve/${approve ? 'approve' : 'reject'}`,
    data
  })
}

export const retryDccProjectProductRequestWrite = async (
  id: number
): Promise<DccProjectProductCreateRespVO> => {
  return await request.post({ url: `${requestUrl}/${id}/retry-write` })
}
