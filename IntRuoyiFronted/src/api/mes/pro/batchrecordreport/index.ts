import request from '@/config/axios'

export interface BatchRecordReportVO {
  batchRecordName: string
  formSlotType?: BatchRecordFormSlotType
  routeKey: string
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  productName?: string
  versionNo?: string
  versionStatus?: string
  sourceTableIndex: number
  tableTitle: string
  reportId: string
  reportCode: string
  reportName: string
  sourceFileName: string
  lastImportTime: Date
  updateTime: Date
}

export interface BatchRecordReportPageReqVO extends PageParam {
  reportId?: string
  batchRecordName?: string
  productName?: string
  versionNo?: string
  formSlotType?: BatchRecordFormSlotType
  latestVersionOnly?: boolean
  routeKey?: string
  name?: string
}

export interface BatchRecordReportImportResultVO {
  importedCount: number
  createdCount: number
  updatedCount: number
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  sourceBatchRecordVersionId?: number
  versionNo?: string
  versionStatus?: string
  approvalInstanceId?: string
  routeId?: number
  routeCode?: string
  routeName?: string
  routeVersionId?: number
  routeVersionNo?: string
  routeProcessCount?: number
  batchRecordRouteBindingCount?: number
  boundProductNameCount?: number
  boundProductCodeCount?: number
  skippedProductNames?: string[]
  reports: BatchRecordReportVO[]
}

export interface BatchRecordReportImportRouteProductOptionVO {
  optionKey: string
  routeProductId?: number
  routeId?: number
  routeCode?: string
  routeName?: string
  routeVersionId?: number
  routeVersionNo?: string
  productId?: number
  productCode?: string
  productName: string
  existing?: boolean
}

export type BatchRecordWordImportAction = 'REBUILD_V1' | 'UPGRADE'

export interface BatchRecordReportReferenceBlockerVO {
  versionNo?: string
  referenceName?: string
  count?: number
  cleanupEntrance?: string
  cleanupAction?: string
}

export interface BatchRecordDuplicateRouteVO {
  routeId?: number
  routeCode?: string
  routeName?: string
  routeVersionId?: number
  routeVersionNo?: string
}

export interface BatchRecordReportImportPreflightVO {
  routeKey: string
  batchRecordName: string
  batchRecordDefinitionId?: number
  currentBatchRecordVersionId?: number
  currentBatchRecordVersionNo?: string
  currentBatchRecordVersionStatus?: string
  latestBatchRecordVersionId?: number
  latestBatchRecordVersionNo?: string
  latestBatchRecordVersionStatus?: string
  currentBatchRecordHasMainReports?: boolean
  routeGovernanceStatus?: string
  routeUpgradeRequired?: boolean
  duplicateRoutes?: BatchRecordDuplicateRouteVO[]
  currentRouteId?: number
  currentRouteCode?: string
  currentRouteName?: string
  currentRouteVersionId?: number
  currentRouteVersionNo?: string
  currentRouteVersionActive?: boolean
  hasHistoricalReferences?: boolean
  referenceBlockers?: BatchRecordReportReferenceBlockerVO[]
  allowedActions?: BatchRecordWordImportAction[]
  recommendedAction?: BatchRecordWordImportAction
  nextVersionNo?: string
  routeProductOptions: BatchRecordReportImportRouteProductOptionVO[]
}

export interface BatchRecordVersionApprovalResultVO {
  definitionId?: number
  versionId?: number
  versionStatus?: string
  approvalInstanceId?: string
  approvalEventId?: string
  approvalResult?: string
  processedResult?: string
}

export type BatchRecordFormSlotType = 'MAIN' | 'LOSS_REPORT' | 'PROCESS_INSPECTION' | 'PARAMETER_RECORD'

export interface BatchRecordReportDeleteAllRespVO {
  deletedReportCount: number
  deletedMetadataCount: number
  skippedBoundReportCount: number
  unboundRouteProcessCount?: number
  deletedRouteFlowBindingCount?: number
  unboundRouteFlowProcessConfigCount?: number
}

export interface BatchRecordReportBatchDeleteReqVO {
  reportIds: string[]
  forceUnbind?: boolean
}

export const WORD_IMPORT_REQUEST_TIMEOUT = 10 * 60 * 1000

export interface BatchRecordReportRenameReqVO {
  reportId: string
  reportName: string
}

export type BatchRecordReportReviewSourceType = 'POST' | 'ROLE' | 'USER' | 'DEPT' | 'ROLES' | 'USERS' | 'DEPTS'

export interface BatchRecordReportSignatureCellMarkerVO {
  rowIndex: number
  columnIndex: number
  enabled: boolean
  signatureCellKey?: string
  actionType: 'FORM_REVIEW' | 'SUBMIT' | 'APPROVE'
  label?: string
  displayFormat?: string
  reviewSourceType?: BatchRecordReportReviewSourceType
  reviewSourceId?: number
  reviewSourceIds?: number[]
  reviewSourceName?: string
}

export interface BatchRecordReportSignatureCellMarkersRespVO {
  reportId: string
  sheetLayoutJson?: string
  markers: BatchRecordReportSignatureCellMarkerVO[]
}

export interface BatchRecordReportSignatureCellMarkersReqVO {
  reportId: string
  markers: BatchRecordReportSignatureCellMarkerVO[]
}

export type BatchRecordReportCellValueType =
  | 'STRING'
  | 'NUMBER'
  | 'DATE'
  | 'DATETIME'
  | 'BOOLEAN'
  | 'SIGNATURE'

export interface BatchRecordReportCellRuleConstraints {
  min?: number
  max?: number
  scale?: number
  precision?: number
  minLength?: number
  maxLength?: number
  format?: string
  [key: string]: unknown
}

export interface BatchRecordReportCellAttachmentRuleVO {
  required?: boolean
  minCount?: number
  maxCount?: number
  attachmentType?: string
  groupKey?: string
}

export interface BatchRecordReportAssistRowFieldVO {
  rowIndex: number
  columnIndex: number
}

export interface BatchRecordReportAssistRowVO {
  rowKey: string
  description: string
  sort: number
  fields: BatchRecordReportAssistRowFieldVO[]
}

export interface BatchRecordReportCellRuleVO {
  rowIndex: number
  columnIndex: number
  valueType: BatchRecordReportCellValueType
  componentFlag?: string
  required?: boolean
  label?: string
  placeholder?: string
  helpText?: string
  constraints?: BatchRecordReportCellRuleConstraints
  unit?: string
  source?: string
  confidence?: number
  reviewed?: boolean
  attachmentRule?: BatchRecordReportCellAttachmentRuleVO
}

export interface BatchRecordReportCellRulesRespVO {
  reportId: string
  sheetLayoutJson?: string
  rules: BatchRecordReportCellRuleVO[]
  suggestions: BatchRecordReportCellRuleVO[]
  unreviewedFillableCellCount: number
  assistRows?: BatchRecordReportAssistRowVO[]
}

export interface BatchRecordReportCellRulesReqVO {
  reportId: string
  rules: BatchRecordReportCellRuleVO[]
  assistRows?: BatchRecordReportAssistRowVO[]
}

export const BatchRecordReportApi = {
  importPilotDoc: async (data: FormData) => {
    const result = await request.upload<{ data: BatchRecordReportImportResultVO }>({
      url: '/mes/pro/batch-record-report/import',
      data
    })
    return result.data
  },

  importImage: async (data: FormData) => {
    const result = await request.upload<{ data: BatchRecordReportImportResultVO }>({
      url: '/mes/pro/batch-record-report/import-image',
      data
    })
    return result.data
  },

  recognizeFixedRoute: async (routeKey: string) => {
    return await request.post<BatchRecordReportImportResultVO>({
      url: '/mes/pro/batch-record-report/recognize-fixed',
      params: { routeKey }
    })
  },

  recognizeUploadedRoute: async (
    file: File,
    routeKey: string,
    batchRecordName: string,
    upgrade: boolean,
    productNames: string[],
    rebuildBatchRecord = true,
    selectedRouteProductIds: number[] = [],
    selectedProductNames: string[] = productNames,
    importAction: BatchRecordWordImportAction = upgrade ? 'UPGRADE' : 'REBUILD_V1',
    expectedSourceVersionId?: number,
    expectedTargetVersionNo?: string,
    routeUpgradeConfirmed = false,
    expectedRouteId?: number,
    expectedRouteVersionId?: number
  ) => {
    const data = new FormData()
    data.append('file', file)
    data.append('routeKey', routeKey)
    data.append('batchRecordName', batchRecordName)
    data.append('upgrade', String(upgrade))
    data.append('importAction', importAction)
    if (expectedSourceVersionId !== undefined) {
      data.append('expectedSourceVersionId', String(expectedSourceVersionId))
    }
    if (expectedTargetVersionNo !== undefined) {
      data.append('expectedTargetVersionNo', expectedTargetVersionNo)
    }
    data.append('routeUpgradeConfirmed', String(routeUpgradeConfirmed))
    if (expectedRouteId !== undefined) {
      data.append('expectedRouteId', String(expectedRouteId))
    }
    if (expectedRouteVersionId !== undefined) {
      data.append('expectedRouteVersionId', String(expectedRouteVersionId))
    }
    data.append('rebuildBatchRecord', String(rebuildBatchRecord))
    productNames.forEach((productName) => data.append('productNames', productName))
    selectedRouteProductIds.forEach((routeProductId) =>
      data.append('selectedRouteProductIds', String(routeProductId))
    )
    selectedProductNames.forEach((productName) => data.append('selectedProductNames', productName))
    const result = await request.upload<{ data: BatchRecordReportImportResultVO }>({
      url: '/mes/pro/batch-record-report/recognize-uploaded',
      data,
      timeout: WORD_IMPORT_REQUEST_TIMEOUT
    })
    return result.data
  },

  preflightUploadedRoute: async (routeKey: string, batchRecordName: string, productNames: string[]) => {
    const params = new URLSearchParams()
    params.append('routeKey', routeKey)
    params.append('batchRecordName', batchRecordName)
    productNames.forEach((productName) => params.append('productNames', productName))
    const query = params.toString()
    return await request.get<BatchRecordReportImportPreflightVO>({
      url: `/mes/pro/batch-record-report/recognize-uploaded/preflight?${query}`
    })
  },

  submitBatchRecordVersionApproval: async (versionId: number) => {
    return await request.post<BatchRecordVersionApprovalResultVO>({
      url: '/mes/pro/batch-record-report/version-approval/submit',
      params: { versionId }
    })
  },

  uploadExtraFormSlot: async (file: File, batchRecordName: string, formSlotType: BatchRecordFormSlotType) => {
    const data = new FormData()
    data.append('file', file)
    data.append('batchRecordName', batchRecordName)
    data.append('formSlotType', formSlotType)
    const result = await request.upload<{ data: BatchRecordReportImportResultVO }>({
      url: '/mes/pro/batch-record-report/upload-extra-slot',
      data,
      timeout: WORD_IMPORT_REQUEST_TIMEOUT
    })
    return result.data
  },

  existsBatchRecordName: async (routeKey: string, batchRecordName: string) => {
    return await request.get<boolean>({
      url: '/mes/pro/batch-record-report/exists',
      params: { routeKey, batchRecordName }
    })
  },

  getBatchRecordNameOptions: async () => {
    return await request.get<string[]>({ url: '/mes/pro/batch-record-report/batch-record-names' })
  },

  getGeneratedReportPage: async (params: BatchRecordReportPageReqVO) => {
    return await request.get({ url: '/mes/pro/batch-record-report/page', params })
  },

  getDesignerPath: async (reportId: string) => {
    return await request.get<{ path: string }>({
      url: '/mes/pro/batch-record-report/designer-path',
      params: { reportId }
    })
  },

  getEditPath: async (reportId: string) => {
    return await request.get<{ path: string }>({
      url: '/mes/pro/batch-record-report/edit-path',
      params: { reportId }
    })
  },

  getSignatureCellMarkers: async (reportId: string) => {
    return await request.get<BatchRecordReportSignatureCellMarkersRespVO>({
      url: '/mes/pro/batch-record-report/signature-cell-markers',
      params: { reportId }
    })
  },

  saveSignatureCellMarkers: async (data: BatchRecordReportSignatureCellMarkersReqVO) => {
    return await request.put<BatchRecordReportSignatureCellMarkersRespVO>({
      url: '/mes/pro/batch-record-report/signature-cell-markers',
      data
    })
  },

  getCellRules: async (reportId: string) => {
    return await request.get<BatchRecordReportCellRulesRespVO>({
      url: '/mes/pro/batch-record-report/cell-rules',
      params: { reportId }
    })
  },

  saveCellRules: async (data: BatchRecordReportCellRulesReqVO) => {
    return await request.put<BatchRecordReportCellRulesRespVO>({
      url: '/mes/pro/batch-record-report/cell-rules',
      data
    })
  },

  renameGeneratedReport: async (data: BatchRecordReportRenameReqVO) => {
    return await request.put({
      url: '/mes/pro/batch-record-report/rename',
      data
    })
  },

  deleteGeneratedReport: async (reportId: string) => {
    return await request.delete({
      url: `/mes/pro/batch-record-report/delete?reportId=${encodeURIComponent(reportId)}`
    })
  },

  deleteGeneratedReports: async (data: BatchRecordReportBatchDeleteReqVO) => {
    return await request.delete<BatchRecordReportDeleteAllRespVO>({
      url: '/mes/pro/batch-record-report/delete-batch',
      data
    })
  },

  deleteGeneratedReportsByBatchRecordName: async (batchRecordName: string, forceUnbind = false) => {
    return await request.delete<BatchRecordReportDeleteAllRespVO>({
      url: '/mes/pro/batch-record-report/delete-by-batch-record-name',
      params: { batchRecordName, forceUnbind }
    })
  },

  deleteGeneratedReportByBatchRecordNameAndFormSlotType: async (
    batchRecordName: string,
    formSlotType: BatchRecordFormSlotType
  ) => {
    return await request.delete({
      url: '/mes/pro/batch-record-report/delete-extra-slot',
      params: { batchRecordName, formSlotType }
    })
  },

  deleteAllGeneratedReports: async (confirm: string) => {
    return await request.delete<BatchRecordReportDeleteAllRespVO>({
      url: '/mes/pro/batch-record-report/delete-all',
      params: { confirm }
    })
  }
}
