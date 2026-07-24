import request from '@/config/axios'
import type { SrmProcurementApprovalRecordVO } from '@/api/srm/procurement-plan'

export interface SrmFrameworkPlanLineVO {
  id?: number
  frameworkPlanLineId?: number
  materialId: number
  materialCode: string
  materialName: string
  quantity: number
  unit: string
  budgetAmount: number
}

export interface SrmFrameworkPlanVO {
  id?: number
  frameworkPlanNo?: string
  planTitle: string
  supplierId: number
  supplierName?: string
  procurementMethod: string
  procurementMethodLabel?: string
  budgetAmount: number
  validStartDate: string
  validEndDate: string
  planStatus?: string
  planStatusLabel?: string
  remark?: string
  agreementId?: number
  agreementNo?: string
  agreementTime?: string
  lines: SrmFrameworkPlanLineVO[]
  approvalRecords?: SrmProcurementApprovalRecordVO[]
}

export interface SrmFrameworkPlanPageReqVO extends PageParam {
  frameworkPlanNo?: string
  planTitle?: string
  supplierName?: string
  planStatus?: string
}

export interface SrmFrameworkPlanAuditReqVO {
  id: number
  auditRemark?: string
}

export interface SrmFrameworkAgreementVO {
  id?: number
  agreementNo?: string
  frameworkPlanId?: number
  frameworkPlanNo?: string
  supplierId?: number
  supplierName?: string
  procurementMethod?: string
  procurementMethodLabel?: string
  budgetAmount?: number
  validStartDate?: string
  validEndDate?: string
  agreementStatus?: string
  agreementStatusLabel?: string
  remark?: string
  lines?: SrmFrameworkPlanLineVO[]
}

export interface SrmFrameworkAgreementPageReqVO extends PageParam {
  agreementNo?: string
  frameworkPlanNo?: string
  supplierName?: string
}

export const srmFrameworkPlanStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已生成协议', value: 'AGREEMENT_CREATED' }
] as const

export const SrmFrameworkPlanApi = {
  getFrameworkPlanPage: async (params: SrmFrameworkPlanPageReqVO) => {
    return await request.get<PageResult<SrmFrameworkPlanVO[]>>({
      url: '/srm/framework-plan/page',
      params
    })
  },

  getFrameworkPlan: async (id: number) => {
    return await request.get<SrmFrameworkPlanVO>({
      url: '/srm/framework-plan/get',
      params: { id }
    })
  },

  createFrameworkPlan: async (data: SrmFrameworkPlanVO) => {
    return await request.post<number>({
      url: '/srm/framework-plan/create',
      data
    })
  },

  submitFrameworkPlan: async (id: number) => {
    return await request.put<boolean>({
      url: '/srm/framework-plan/submit',
      params: { id }
    })
  },

  approveFrameworkPlan: async (data: SrmFrameworkPlanAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/framework-plan/approve',
      data
    })
  },

  rejectFrameworkPlan: async (data: SrmFrameworkPlanAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/framework-plan/reject',
      data
    })
  },

  createAgreement: async (id: number) => {
    return await request.post<SrmFrameworkAgreementVO>({
      url: '/srm/framework-plan/create-agreement',
      params: { id }
    })
  },

  getAgreementPage: async (params: SrmFrameworkAgreementPageReqVO) => {
    return await request.get<PageResult<SrmFrameworkAgreementVO[]>>({
      url: '/srm/framework-plan/agreement-page',
      params
    })
  }
}
