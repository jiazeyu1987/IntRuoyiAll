import request from '@/config/axios'

export interface SrmProcurementPlanLineVO {
  id?: number
  lineNo?: string
  materialId: number
  materialCode: string
  materialName: string
  quantity: number
  unit: string
  requiredDate: string
}

export interface SrmProcurementApprovalRecordVO {
  id?: number
  action?: string
  actionLabel?: string
  operatorName?: string
  operationTime?: string
  remark?: string
}

export interface SrmProcurementPlanVO {
  id?: number
  planNo?: string
  planTitle: string
  procurementMethod: string
  procurementMethodLabel?: string
  expectedAmount: number
  planStatus?: string
  planStatusLabel?: string
  remark?: string
  submittedName?: string
  submittedTime?: string
  auditName?: string
  auditTime?: string
  auditRemark?: string
  generatedProjectId?: number
  generatedProjectNo?: string
  generatedProjectType?: string
  generatedTime?: string
  createTime?: string
  lines: SrmProcurementPlanLineVO[]
  approvalRecords?: SrmProcurementApprovalRecordVO[]
}

export interface SrmProcurementPlanPageReqVO extends PageParam {
  planNo?: string
  planTitle?: string
  planStatus?: string
}

export interface SrmProcurementPlanAuditReqVO {
  id: number
  auditRemark?: string
}

export interface SrmProcurementPlanGenerateReqVO {
  id: number
  projectType: string
}

export interface SrmSourcingProjectVO {
  id: number
  projectNo: string
  projectTitle: string
  projectType: string
  projectTypeLabel?: string
  projectStatus: string
  projectStatusLabel?: string
  sourcePlanId: number
  sourcePlanNo: string
  expectedAmount: number
  lines: SrmProcurementPlanLineVO[]
}

export const srmProcurementMethodOptions = [
  { label: '招标', value: 'TENDER' },
  { label: '非招标', value: 'NON_BIDDING' }
] as const

export const srmProcurementPlanStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已生成项目', value: 'GENERATED' }
] as const

export const SrmProcurementPlanApi = {
  getProcurementPlanPage: async (params: SrmProcurementPlanPageReqVO) => {
    return await request.get<PageResult<SrmProcurementPlanVO[]>>({
      url: '/srm/procurement-plan/page',
      params
    })
  },

  getProcurementPlan: async (id: number) => {
    return await request.get<SrmProcurementPlanVO>({
      url: '/srm/procurement-plan/get',
      params: { id }
    })
  },

  createProcurementPlan: async (data: SrmProcurementPlanVO) => {
    return await request.post<number>({
      url: '/srm/procurement-plan/create',
      data
    })
  },

  submitProcurementPlan: async (id: number) => {
    return await request.put<boolean>({
      url: '/srm/procurement-plan/submit',
      params: { id }
    })
  },

  approveProcurementPlan: async (data: SrmProcurementPlanAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/procurement-plan/approve',
      data
    })
  },

  rejectProcurementPlan: async (data: SrmProcurementPlanAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/procurement-plan/reject',
      data
    })
  },

  generateSourcingProject: async (data: SrmProcurementPlanGenerateReqVO) => {
    return await request.post<SrmSourcingProjectVO>({
      url: '/srm/procurement-plan/generate-sourcing',
      data
    })
  }
}
