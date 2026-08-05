import request from '@/config/axios'

// MES 质检方案 VO
export interface QcTemplateVO {
  id: number // 编号
  code: string // 方案编号
  name: string // 方案名称
  types: number[] // 检测种类
  status: number // 状态
  remark: string // 备注
}

export interface QaInspectionRuleVO {
  inspectionType: string
  itemCode: string
  itemName: string
  inspectionMethod: string
  standardText: string
  resultType: string
  firstInspectionQuantity?: number
  patrolInspectionRatio?: number
}

export interface QaInspectionRegulationPublishedVersionVO {
  regulationId: number
  publishedVersionId: number
  versionNo: string
  publishedAt?: string
  immutable: boolean
  regulationCode: string
  regulationName: string
  productId: number
  productName?: string
  routeId: number
  routeName?: string
  routeVersionId: number
  routeVersionNo?: string
  routeProcessId: number
  processId: number
  routeProcessName?: string
  batchRecordBindingSummary?: string
  firstInspectionRules: QaInspectionRuleVO[]
  patrolInspectionRules: QaInspectionRuleVO[]
  finalInspectionRules: QaInspectionRuleVO[]
}

export interface QaInspectionRegulationProjectStatusVO {
  productId: number
  configured: boolean
  regulationCount: number
  regulationId?: number
  currentVersionId?: number
  regulationCode?: string
  regulationName?: string
  lifecycleStatus?: string
}

// MES 质检方案 API
export const QcTemplateApi = {
  // 查询正式 QA 检验规程发布版本只读证据
  getPublishedQaRegulationVersion: async (versionId?: number) => {
    return await request.get({
      url: `/mes/qa/inspection-regulation/published-version`,
      params: versionId ? { versionId } : undefined
    })
  },

  // 批量查询产品 QA 检验规程配置状态
  getQaRegulationProjectStatuses: async (
    productIds: number[]
  ): Promise<QaInspectionRegulationProjectStatusVO[]> => {
    if (productIds.length === 0) {
      return []
    }
    return await request.get({
      url: `/mes/qa/inspection-regulation/project-statuses`,
      params: { productIds: productIds.join(',') }
    })
  },

  // 查询质检方案分页
  getTemplatePage: async (params: any) => {
    return await request.get({ url: `/mes/qc/template/page`, params })
  },

  // 查询质检方案详情
  getTemplate: async (id: number) => {
    return await request.get({ url: `/mes/qc/template/get?id=` + id })
  },

  // 新增质检方案
  createTemplate: async (data: QcTemplateVO) => {
    return await request.post({ url: `/mes/qc/template/create`, data })
  },

  // 修改质检方案
  updateTemplate: async (data: QcTemplateVO) => {
    return await request.put({ url: `/mes/qc/template/update`, data })
  },

  // 删除质检方案
  deleteTemplate: async (id: number) => {
    return await request.delete({ url: `/mes/qc/template/delete?id=` + id })
  },

  // 导出质检方案 Excel
  exportTemplate: async (params: any) => {
    return await request.download({ url: `/mes/qc/template/export-excel`, params })
  }
}
