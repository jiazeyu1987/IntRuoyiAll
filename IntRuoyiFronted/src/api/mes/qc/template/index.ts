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

export interface QaInspectionRegulationInspectionTypeRuleVO {
  key: string
  inspectionType: 'FIRST' | 'PATROL' | 'FINAL'
  label: string
  roundLabel: string
  required: boolean
  fixedQuantity?: number
  notApplicableReason?: string
  taskRule: string
  releaseGate: string
}

export interface QaInspectionRegulationEquipmentOptionVO {
  equipmentId: number
  equipmentCode: string
  equipmentName: string
  equipmentNumber: string
  defaultFlag?: boolean
  sort?: number
}

export interface QaInspectionRegulationItemVO {
  itemSort: number
  itemCode: string
  itemName: string
  inspectionMethod: string
  inspectionTool: string
  samplingPlanText: string
  standardText: string
  standardLowerLimit?: number
  standardUpperLimit?: number
  standardUnit?: string
  standardPrecision?: number
  equipmentRequired?: boolean
  equipmentOptions?: QaInspectionRegulationEquipmentOptionVO[]
  resultType: 'BOOLEAN' | 'NUMERIC' | 'TEXT'
  applicableInspectionTypes: Array<'FIRST' | 'PATROL' | 'FINAL'>
  firstInspectionQuantity?: number
  patrolInspectionRatio?: number
  critical?: boolean
  failureRule?: string
  sourceNote?: string
  sourceOriginalPage?: number
  sourceOriginalItem?: string
  sourceOriginalExcerpt?: string
  sourceOriginalMethod?: string
}

export interface QaInspectionRegulationProcessVO {
  qaProcessId?: number
  processCode: string
  processName: string
  sort: number
  items: QaInspectionRegulationItemVO[]
}

export interface QaInspectionRegulationPublishedVersionVO {
  dccProjectCodeId: number
  regulationId: number
  publishedVersionId: number
  versionNo: string
  effectiveDate?: string
  publishedAt?: string
  immutable: boolean
  lifecycleStatus: string
  regulationCode: string
  regulationName: string
  finalInspectionApplicable: boolean
  finalInspectionNotApplicableReason?: string
  inspectionTypeRules: QaInspectionRegulationInspectionTypeRuleVO[]
  processes: QaInspectionRegulationProcessVO[]
}

export interface QaInspectionRegulationProjectStatusVO {
  dccProjectCodeId: number
  configured: boolean
  regulationCount: number
  regulationId?: number
  currentVersionId?: number
  regulationCode?: string
  regulationName?: string
  lifecycleStatus?: string
}

export interface QaInspectionRegulationSaveReqVO {
  regulationId?: number
  dccProjectCodeId: number
  regulationCode: string
  regulationName: string
  versionNo: string
  effectiveDate?: string
  finalInspectionApplicable: boolean
  finalInspectionNotApplicableReason?: string
  inspectionTypeRules: QaInspectionRegulationInspectionTypeRuleVO[]
  processes: QaInspectionRegulationProcessVO[]
}

export interface QaInspectionRegulationSaveRespVO {
  dccProjectCodeId: number
  regulationId: number
  draftVersionId: number
  versionNo: string
  lifecycleStatus: string
  immutable: boolean
}

// MES 质检方案 API
export const QcTemplateApi = {
  // 保存正式 QA 检验规程草稿
  saveQaRegulationDraft: async (
    data: QaInspectionRegulationSaveReqVO
  ): Promise<QaInspectionRegulationSaveRespVO> => {
    return await request.post({ url: `/mes/qa/inspection-regulation/draft`, data })
  },

  // 发布正式 QA 检验规程并生成不可变版本
  publishQaRegulation: async (
    data: QaInspectionRegulationSaveReqVO
  ): Promise<QaInspectionRegulationPublishedVersionVO> => {
    return await request.post({ url: `/mes/qa/inspection-regulation/publish`, data })
  },

  // 查询正式 QA 检验规程发布版本只读证据
  getPublishedQaRegulationVersion: async (
    dccProjectCodeId: number,
    versionId?: number
  ): Promise<QaInspectionRegulationPublishedVersionVO> => {
    return await request.get({
      url: `/mes/qa/inspection-regulation/published-version`,
      params: { dccProjectCodeId, ...(versionId ? { versionId } : {}) }
    })
  },

  // 查询 DCC 项目当前 QA 规程配置
  getCurrentQaRegulation: async (
    dccProjectCodeId: number
  ): Promise<QaInspectionRegulationPublishedVersionVO | null> => {
    return await request.get({
      url: `/mes/qa/inspection-regulation/current`,
      params: { dccProjectCodeId }
    })
  },

  // 批量查询 DCC 项目 QA 检验规程配置状态
  getQaRegulationProjectStatuses: async (
    dccProjectCodeIds: number[]
  ): Promise<QaInspectionRegulationProjectStatusVO[]> => {
    if (dccProjectCodeIds.length === 0) {
      return []
    }
    return await request.get({
      url: `/mes/qa/inspection-regulation/project-statuses`,
      params: { dccProjectCodeIds: dccProjectCodeIds.join(',') }
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
