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

export type QaInspectionRegulationInspectionRuleKey = 'FIRST' | 'PATROL_AM' | 'PATROL_PM' | 'FINAL'
export type QaInspectionRegulationResultType = 'BOOLEAN' | 'NUMERIC' | 'TEXT'

export interface QaInspectionRegulationInspectionTypeRuleVO {
  key: QaInspectionRegulationInspectionRuleKey
  inspectionType: 'FIRST' | 'PATROL' | 'FINAL'
  label: string
  roundLabel: string
  required: boolean
  fixedQuantity?: number
  notApplicableReason?: string
  taskRule: string
  releaseGate: string
}

export interface QaInspectionRegulationItemEquipmentVO {
  equipmentId: number
  equipmentCode: string
  equipmentName: string
  equipmentNumber: string
  defaultFlag?: boolean
  sort?: number
}

export interface QaInspectionRegulationItemEquipmentRefVO {
  equipmentId: number
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
  resultType: QaInspectionRegulationResultType
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
  equipmentOptions: QaInspectionRegulationItemEquipmentVO[]
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
  productionReady?: boolean
  regulationCount: number
  regulationId?: number
  currentVersionId?: number
  regulationCode?: string
  regulationName?: string
  lifecycleStatus?: string
  publishedVersionNo?: string
}

export interface QaInspectionRegulationVersionOptionVO {
  dccProjectCodeId: number
  regulationId: number
  versionId: number
  versionNo: string
  lifecycleStatus: string
  effectiveDate?: string
  publishedAt?: string
  retiredAt?: string
  currentPublished: boolean
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
  processes: QaInspectionRegulationSaveProcessVO[]
}

export interface QaInspectionRegulationSaveProcessVO {
  processCode: string
  processName: string
  sort: number
  items: QaInspectionRegulationSaveItemVO[]
}

export type QaInspectionRegulationSaveItemVO = Omit<
  QaInspectionRegulationItemVO,
  'equipmentOptions'
>

export interface QaInspectionRegulationSaveRespVO {
  dccProjectCodeId: number
  regulationId: number
  draftVersionId: number
  versionNo: string
  lifecycleStatus: string
  immutable: boolean
}

export interface QaInspectionRegulationImportRespVO {
  dccProjectCodeId: number
  regulationId: number
  draftVersionId: number
  regulationCode: string
  regulationName: string
  versionNo: string
  effectiveDate: string
  lifecycleStatus: string
  route: 'CREATE' | 'UPGRADE'
  processCount: number
  itemCount: number
  inheritedItemCount: number
  createdItemCount: number
}

export interface QaInspectionRegulationResetRespVO {
  dccProjectCodeId: number
  regulationId?: number
  versionCount: number
  processCount: number
  itemCount: number
  itemEquipmentCount: number
}

export interface PqcItemEquipmentItemVO {
  dccProjectCodeId: number
  itemCode: string
  itemCodes?: string[]
  projectName: string
  itemName: string
  inspectionMethod?: string
  standardText?: string
  samplingPlanText?: string
}

export interface PqcItemEquipmentNumberConfigVO {
  id?: number
  equipmentNumber: string
  enabled?: boolean
  sort?: number
}

export interface PqcItemEquipmentGroupConfigVO {
  id?: number
  equipmentId: number
  equipmentCode?: string
  equipmentName?: string
  enabled?: boolean
  defaultFlag?: boolean
  sort?: number
  equipmentNumbers: PqcItemEquipmentNumberConfigVO[]
}

export interface PqcItemEquipmentConfigVO {
  itemCode: string
  itemCodes?: string[]
  itemName?: string
  configurationConsistent?: boolean
  equipmentGroups: PqcItemEquipmentGroupConfigVO[]
}

export interface PqcItemEquipmentConfigSaveReqVO {
  itemCode: string
  itemNameSnapshot?: string
  equipmentGroups: PqcItemEquipmentGroupConfigVO[]
}

export interface PqcItemEquipmentBatchConfigSaveReqVO extends PqcItemEquipmentConfigSaveReqVO {
  dccProjectCodeId: number
  itemCodes: string[]
}

// MES 质检方案 API
export const QcTemplateApi = {
  // 保存正式 QA 检验规程草稿
  saveQaRegulationDraft: async (
    data: QaInspectionRegulationSaveReqVO
  ): Promise<QaInspectionRegulationSaveRespVO> => {
    return await request.post({ url: `/mes/qa/inspection-regulation/draft`, data })
  },

  // 解析 QA Word 模板并保存正式规程草稿
  importQaRegulationWordDraft: async (
    data: FormData
  ): Promise<QaInspectionRegulationImportRespVO> => {
    return await request.upload({
      url: `/mes/qa/inspection-regulation/import-word-draft`,
      data,
      headersType: 'multipart/form-data',
      ignoreErrorMessage: true
    })
  },

  // 测试阶段重置指定 DCC 项目的正式 QA 检验规程
  resetQaRegulationForTesting: async (
    dccProjectCodeId: number
  ): Promise<QaInspectionRegulationResetRespVO> => {
    return await request.post({
      url: `/mes/qa/inspection-regulation/test-reset`,
      params: { dccProjectCodeId },
      ignoreErrorMessage: true
    })
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

  // 查询 DCC 项目全部 QA 检验规程版本
  listQaRegulationVersions: async (
    dccProjectCodeId: number
  ): Promise<QaInspectionRegulationVersionOptionVO[]> => {
    return await request.get({
      url: `/mes/qa/inspection-regulation/versions`,
      params: { dccProjectCodeId }
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

  // 查询当前 QA 项目下可维护的租户级 PQC 检验项目
  getPqcItemEquipmentItems: async (
    dccProjectCodeId: number
  ): Promise<PqcItemEquipmentItemVO[]> => {
    return await request.get({
      url: `/mes/pqc/item-equipment/items`,
      params: { dccProjectCodeId }
    })
  },

  // 查询租户级 PQC 检验项目设备配置
  getPqcItemEquipmentConfig: async (itemCode: string): Promise<PqcItemEquipmentConfigVO> => {
    return await request.get({
      url: `/mes/pqc/item-equipment/config`,
      params: { itemCode }
    })
  },

  // 查询当前 QA 项目同名检验项目对应的全部设备配置
  getPqcItemEquipmentConfigBatch: async (
    dccProjectCodeId: number,
    itemCodes: string[]
  ): Promise<PqcItemEquipmentConfigVO> => {
    return await request.get({
      url: '/mes/pqc/item-equipment/config/batch',
      params: { dccProjectCodeId, itemCodes: itemCodes.join(',') }
    })
  },

  // 保存租户级 PQC 检验项目设备配置
  savePqcItemEquipmentConfig: async (
    data: PqcItemEquipmentConfigSaveReqVO
  ): Promise<PqcItemEquipmentConfigVO> => {
    return await request.post({
      url: `/mes/pqc/item-equipment/config`,
      data
    })
  },

  // 原子保存当前 QA 项目同名检验项目对应的全部设备配置
  savePqcItemEquipmentConfigBatch: async (
    data: PqcItemEquipmentBatchConfigSaveReqVO
  ): Promise<PqcItemEquipmentConfigVO> => {
    return await request.post({
      url: '/mes/pqc/item-equipment/config/batch',
      data
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
