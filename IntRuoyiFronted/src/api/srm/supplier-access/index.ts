import request from '@/config/axios'
import type { SrmSupplierRiskVO } from '@/api/srm/supplier-risk'

export interface SrmSupplierReferenceVO {
  id: number
  name: string
}

export interface SrmSupplierAccessVO {
  id?: number
  supplierId: number
  supplierName?: string
  portalContactName?: string
  portalContactPhone?: string
  qualificationExpireDate?: string
  qualificationStatusLabel?: string
  sampleTestStatus?: string
  sampleTestStatusLabel?: string
  trialOrderStatus?: string
  trialOrderStatusLabel?: string
  onboardingStageSummary?: string
  accessStatus?: string
  accessStatusLabel?: string
  enabled?: boolean
  accessRemark?: string
  openHighRiskCount?: number
  eligibilitySummary?: string
  submittedName?: string
  submittedTime?: string
  auditName?: string
  auditTime?: string
  auditRemark?: string
  disabledName?: string
  disabledTime?: string
  createTime?: string
}

export interface SrmSupplierAccessPageReqVO extends PageParam {
  supplierName?: string
  accessStatus?: string
  enabled?: boolean
}

export interface SrmSupplierAccessAuditReqVO {
  id: number
  auditRemark?: string
}

export interface SrmSupplierAccessEnableReqVO {
  id: number
  enabled: boolean
  operationRemark?: string
}

export interface SrmSupplierEligibilityVO {
  supplierId: number
  supplierName?: string
  eligible: boolean
  accessStatus?: string
  accessStatusLabel?: string
  enabled?: boolean
  openHighRiskCount: number
  blockedReason?: string
  openHighRiskSources: string[]
  checkedTime?: string
}

export interface SrmSupplierProfileVO {
  supplierId: number
  supplierName?: string
  accessId?: number
  accessStatus?: string
  accessStatusLabel?: string
  enabled?: boolean
  portalContactName?: string
  portalContactPhone?: string
  qualificationExpireDate?: string
  qualificationStatusLabel?: string
  sampleTestStatus?: string
  sampleTestStatusLabel?: string
  trialOrderStatus?: string
  trialOrderStatusLabel?: string
  onboardingStageSummary?: string
  eligibilitySummary?: string
  accessRemark?: string
  submittedName?: string
  submittedTime?: string
  auditName?: string
  auditTime?: string
  auditRemark?: string
  sampleAuditName?: string
  sampleAuditTime?: string
  sampleAuditRemark?: string
  trialAuditName?: string
  trialAuditTime?: string
  trialAuditRemark?: string
  openHighRiskCount?: number
  riskList?: SrmSupplierRiskVO[]
}

export const srmSupplierAccessStatusOptions = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
] as const

export const SrmSupplierAccessApi = {
  getSupplierAccessPage: async (params: SrmSupplierAccessPageReqVO) => {
    return await request.get<PageResult<SrmSupplierAccessVO[]>>({
      url: '/srm/supplier-access/page',
      params
    })
  },

  createSupplierAccess: async (data: SrmSupplierAccessVO) => {
    return await request.post<number>({
      url: '/srm/supplier-access/create',
      data
    })
  },

  updateSupplierAccess: async (data: SrmSupplierAccessVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-access/update',
      data
    })
  },

  deleteSupplierAccess: async (id: number) => {
    return await request.delete<boolean>({
      url: '/srm/supplier-access/delete',
      params: { id }
    })
  },

  approveSupplierAccess: async (data: SrmSupplierAccessAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-access/approve',
      data
    })
  },

  rejectSupplierAccess: async (data: SrmSupplierAccessAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-access/reject',
      data
    })
  },

  approveSampleTest: async (data: SrmSupplierAccessAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-access/sample/approve',
      data
    })
  },

  rejectSampleTest: async (data: SrmSupplierAccessAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-access/sample/reject',
      data
    })
  },

  approveTrialOrder: async (data: SrmSupplierAccessAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-access/trial/approve',
      data
    })
  },

  rejectTrialOrder: async (data: SrmSupplierAccessAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-access/trial/reject',
      data
    })
  },

  enableSupplierAccess: async (data: SrmSupplierAccessEnableReqVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-access/enable',
      data
    })
  },

  checkSupplierEligibility: async (supplierId: number) => {
    return await request.get<SrmSupplierEligibilityVO>({
      url: '/srm/supplier-access/check',
      params: { supplierId }
    })
  },

  getSupplierProfile: async (supplierId: number) => {
    return await request.get<SrmSupplierProfileVO>({
      url: '/srm/supplier-access/profile',
      params: { supplierId }
    })
  },

  getReferenceSuppliers: async (keyword?: string) => {
    return await request.get<SrmSupplierReferenceVO[]>({
      url: '/srm/supplier-access/reference-suppliers',
      params: { keyword }
    })
  }
}
