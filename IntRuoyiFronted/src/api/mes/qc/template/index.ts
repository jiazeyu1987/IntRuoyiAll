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
export type QaInspectionRegulationOwnerModule = 'MES_QA' | 'MES_QA_COMMON'

export interface QaInspectionRegulationParsedItemVO {
  itemSort: number
  itemCode: string
  itemName: string
  inspectionMethod: string
  inspectionTool: string
  samplingPlanText: string
  standardText: string
  resultType: QaInspectionRegulationResultType
  applicableInspectionTypes: Array<'FIRST' | 'PATROL'>
  firstInspectionQuantity?: number
  patrolInspectionRatio?: number
}

export interface QaInspectionRegulationParsedProcessVO {
  processCode: string
  processName: string
  sort: number
  items: QaInspectionRegulationParsedItemVO[]
}

export interface QaInspectionRegulationParseVO {
  schemaVersion: number
  sourceFileName: string
  regulationCode: string
  regulationName: string
  versionNo: string
  effectiveDate: string
  processes: QaInspectionRegulationParsedProcessVO[]
}

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

export interface QaCommonRegulationBindingVO {
  bindingId?: number
  productDccProjectCodeId: number
  productId: number
  commonRegulationSetId?: number
  commonRegulationSetVersionId?: number
  commonRegulationSetCode?: string
  commonRegulationSetName?: string
  commonRegulationSetVersionNo?: string
  commonDccProjectCodeId: number
  commonRegulationId: number
  commonRegulationVersionId: number
  commonRegulationCode: string
  commonRegulationName: string
  versionNo: string
  lifecycleStatus: string
  effectiveDate?: string
  publishedAt?: string
  scopeCode: string
  bindingStatus: 'ENABLED' | 'DISABLED'
}

export interface QaCommonRegulationVersionOptionVO {
  commonDccProjectCodeId: number
  commonRegulationId: number
  commonRegulationVersionId: number
  commonRegulationCode: string
  commonRegulationName: string
  versionNo: string
  lifecycleStatus: string
  effectiveDate?: string
  publishedAt?: string
}

export interface QaCommonRegulationBindReqVO {
  dccProjectCodeId: number
  commonRegulationSetVersionId: number
  changeReason?: string
}

export interface QaCommonRegulationSetMemberVO {
  id?: number
  commonDccProjectCodeId: number
  commonRegulationId: number
  commonRegulationVersionId: number
  commonRegulationCode: string
  commonRegulationName: string
  versionNo: string
  lifecycleStatus: string
  effectiveDate?: string
  publishedAt?: string
  sort?: number
  memberRole?: string
  remark?: string
  processes: QaInspectionRegulationProcessVO[]
}

export interface QaCommonRegulationSetVersionVO {
  id?: number
  setId?: number
  versionNo: string
  lifecycleStatus: string
  effectiveDate?: string
  publishedAt?: string
  retiredAt?: string
  remark?: string
  currentPublished?: boolean
  members: QaCommonRegulationSetMemberVO[]
}

export interface QaCommonRegulationSetVO {
  id?: number
  setCode: string
  setName: string
  setStatus: 'ENABLED' | 'DISABLED'
  currentVersionId?: number
  remark?: string
  versions: QaCommonRegulationSetVersionVO[]
}

export interface QaCommonRegulationSetSaveReqVO {
  id?: number
  setCode: string
  setName: string
  setStatus?: 'ENABLED' | 'DISABLED'
  remark?: string
}

export interface QaCommonRegulationSetVersionMemberSaveReqVO {
  commonRegulationVersionId: number
  sort?: number
  memberRole?: string
  remark?: string
}

export interface QaCommonRegulationSetVersionSaveReqVO {
  id?: number
  setId: number
  versionNo: string
  lifecycleStatus?: 'DRAFT' | 'PUBLISHED'
  effectiveDate?: string
  remark?: string
  members: QaCommonRegulationSetVersionMemberSaveReqVO[]
}

export interface QaCommonRegulationSetVersionOptionVO {
  commonRegulationSetId: number
  commonRegulationSetVersionId: number
  commonRegulationSetCode: string
  commonRegulationSetName: string
  versionNo: string
  lifecycleStatus: string
  effectiveDate?: string
  publishedAt?: string
  memberCount: number
}

export interface QaInspectionRegulationSaveReqVO {
  regulationId?: number
  dccProjectCodeId: number
  ownerModule?: QaInspectionRegulationOwnerModule
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
  publishedVersionId?: number
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

export interface QaInspectionRegulationWordImportOptions {
  ownerModule?: QaInspectionRegulationOwnerModule
  publishAfterImport?: boolean
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
  // 表单解析页只读解析 QA Word，不创建草稿或版本
  parseQaInspectionRegulationJson: async (
    file: File
  ): Promise<QaInspectionRegulationParseVO> => {
    const data = new FormData()
    data.append('file', file)
    const result = await request.upload<{ data: QaInspectionRegulationParseVO }>({
      url: '/mes/qa/inspection-regulation/form-parser-json',
      data,
      timeout: 300000
    })
    return result.data
  },

  // 保存正式 QA 检验规程草稿
  saveQaRegulationDraft: async (
    data: QaInspectionRegulationSaveReqVO
  ): Promise<QaInspectionRegulationSaveRespVO> => {
    return await request.post({ url: `/mes/qa/inspection-regulation/draft`, data })
  },

  // 解析 QA Word 模板并保存正式规程草稿
  importQaRegulationWordDraft: async (
    data: FormData,
    options?: QaInspectionRegulationWordImportOptions
  ): Promise<QaInspectionRegulationImportRespVO> => {
    if (options?.ownerModule) {
      data.set('ownerModule', options.ownerModule)
    }
    if (options?.publishAfterImport !== undefined) {
      data.set('publishAfterImport', String(options.publishAfterImport))
    }
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

  // 查询当前产品绑定的通用检验规程版本
  getCurrentCommonRegulationBinding: async (
    dccProjectCodeId: number
  ): Promise<QaCommonRegulationBindingVO | null> => {
    return await request.get({
      url: `/mes/qa/inspection-regulation/common-binding/current`,
      params: { dccProjectCodeId }
    })
  },

  // 查询可绑定的已发布通用检验规程版本
  listCommonRegulationPublishedVersions: async (): Promise<QaCommonRegulationVersionOptionVO[]> => {
    return await request.get({
      url: `/mes/qa/inspection-regulation/common-binding/published-versions`
    })
  },

  // 查询通用检验规程套列表
  listCommonRegulationSets: async (): Promise<QaCommonRegulationSetVO[]> => {
    return await request.get({ url: `/mes/qa/inspection-regulation/common-sets` })
  },

  // 查询通用检验规程套详情
  getCommonRegulationSet: async (setId: number): Promise<QaCommonRegulationSetVO> => {
    return await request.get({
      url: `/mes/qa/inspection-regulation/common-sets/get`,
      params: { setId }
    })
  },

  // 保存通用检验规程套
  saveCommonRegulationSet: async (
    data: QaCommonRegulationSetSaveReqVO
  ): Promise<QaCommonRegulationSetVO> => {
    return await request.post({ url: `/mes/qa/inspection-regulation/common-sets/save`, data })
  },

  // 删除通用检验规程套
  deleteCommonRegulationSet: async (setId: number): Promise<boolean> => {
    return await request.delete({
      url: `/mes/qa/inspection-regulation/common-sets/delete`,
      params: { setId }
    })
  },

  // 保存通用检验规程套版本
  saveCommonRegulationSetVersion: async (
    data: QaCommonRegulationSetVersionSaveReqVO
  ): Promise<QaCommonRegulationSetVersionVO> => {
    return await request.post({
      url: `/mes/qa/inspection-regulation/common-set-versions/save`,
      data
    })
  },

  // 删除通用检验规程套版本
  deleteCommonRegulationSetVersion: async (setVersionId: number): Promise<boolean> => {
    return await request.delete({
      url: `/mes/qa/inspection-regulation/common-set-versions/delete`,
      params: { setVersionId }
    })
  },

  // 查询可绑定的已发布通用检验规程套版本
  listCommonRegulationPublishedSetVersions: async (): Promise<
    QaCommonRegulationSetVersionOptionVO[]
  > => {
    return await request.get({
      url: `/mes/qa/inspection-regulation/common-binding/published-set-versions`
    })
  },

  // 绑定当前产品的通用检验规程版本
  bindCommonRegulationVersion: async (
    data: QaCommonRegulationBindReqVO
  ): Promise<QaCommonRegulationBindingVO> => {
    return await request.post({
      url: `/mes/qa/inspection-regulation/common-binding/bind`,
      data
    })
  },

  // 解除当前产品的通用检验规程绑定
  unbindCommonRegulation: async (
    dccProjectCodeId: number
  ): Promise<QaCommonRegulationBindingVO | null> => {
    return await request.post({
      url: `/mes/qa/inspection-regulation/common-binding/unbind`,
      params: { dccProjectCodeId }
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
