import request from '@/config/axios'

export interface SrmSupplierPortalApplicationPageReqVO extends PageParam {
  id?: number
  companyName?: string
  contactName?: string
  applicationStatus?: string
}

export interface SrmSupplierPortalApplicationVO {
  id?: number
  userId?: number
  supplierId?: number
  companyName: string
  unifiedSocialCreditCode: string
  contactName: string
  contactPhone: string
  contactEmail: string
  qualificationAttachmentUrls: string
  qualificationExpireDate?: string
  bankName: string
  bankAccount: string
  bankAddress: string
  applicationStatus?: string
  applicationStatusLabel?: string
  submitterName?: string
  submittedTime?: string
  auditName?: string
  auditTime?: string
  auditRemark?: string
}

export interface SrmSupplierPortalApplicationAuditReqVO {
  id: number
  auditRemark?: string
}

export const srmSupplierPortalStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '审核通过', value: 'APPROVED' },
  { label: '审核驳回', value: 'REJECTED' }
] as const

export const SrmSupplierPortalApi = {
  getMyApplication: async () => {
    return await request.get<SrmSupplierPortalApplicationVO>({ url: '/srm/supplier-portal/my' })
  },
  saveDraft: async (data: SrmSupplierPortalApplicationVO) => {
    return await request.post<SrmSupplierPortalApplicationVO>({
      url: '/srm/supplier-portal/save-draft',
      data
    })
  },
  submit: async (data: SrmSupplierPortalApplicationVO) => {
    return await request.post<SrmSupplierPortalApplicationVO>({
      url: '/srm/supplier-portal/submit',
      data
    })
  },
  getApplicationPage: async (params: SrmSupplierPortalApplicationPageReqVO) => {
    return await request.get<PageResult<SrmSupplierPortalApplicationVO[]>>({
      url: '/srm/supplier-portal/page',
      params
    })
  },
  approve: async (data: SrmSupplierPortalApplicationAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-portal/approve',
      data
    })
  },
  reject: async (data: SrmSupplierPortalApplicationAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-portal/reject',
      data
    })
  }
}
