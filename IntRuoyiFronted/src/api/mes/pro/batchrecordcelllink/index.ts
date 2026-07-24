import request from '@/config/axios'
import type { EdhrRouteId } from '../edhr/batchExecution'

export interface BatchRecordCellLinkFormVO {
  id?: number
  batchRecordName?: string
  formSlotType?: string
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  sourceTableIndex?: number
  tableTitle?: string
  reportId: string
  reportCode?: string
  reportName: string
}

export interface BatchRecordCellLinkCellVO {
  rowIndex: number
  columnIndex: number
  cellKey: string
  label?: string
  valueType?: string
  componentFlag?: string
  required?: boolean
  readonly?: boolean
  signatureCell?: boolean
  linkableAsSource?: boolean
  linkableAsTarget?: boolean
}

export interface BatchRecordCellLinkRuleVO {
  id?: number
  scopeType?: string
  scopeId?: number
  routeId?: number
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  sourceReportId: string
  sourceReportName?: string
  sourceRowIndex: number
  sourceColumnIndex: number
  sourceCellKey?: string
  sourceLabel?: string
  sourceValueType?: string
  targetReportId: string
  targetReportName?: string
  targetRowIndex: number
  targetColumnIndex: number
  targetCellKey?: string
  targetLabel?: string
  targetValueType?: string
  overwritePolicy?: string
  templateSnapshotHash?: string
  ruleVersion?: number
  enabled?: boolean
  remark?: string
}

export interface BatchRecordCellLinkWorkbenchContextVO {
  scopeType: string
  scopeId: number
  routeId?: number
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  forms: BatchRecordCellLinkFormVO[]
  defaultSourceReportId?: string
  defaultTargetReportId?: string
  rules: BatchRecordCellLinkRuleVO[]
}

export interface BatchRecordCellLinkFormCellsVO {
  reportId: string
  reportName: string
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  layoutSnapshotHash?: string
  sheetLayoutJson?: string
  cells: BatchRecordCellLinkCellVO[]
}

export interface BatchRecordCellLinkRulesSaveReqVO {
  scopeType?: string
  scopeId?: number
  routeId?: number
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  rules: BatchRecordCellLinkRuleVO[]
}

export interface BatchRecordCellLinkRulesSaveRespVO {
  savedCount: number
  ruleVersion: number
  rules: BatchRecordCellLinkRuleVO[]
}

export interface BatchRecordCellLinkPrefillItemVO {
  targetCellKey: string
  targetRowIndex: number
  targetColumnIndex: number
  value?: unknown
  sourceExecutionId?: number
  sourceReportId?: string
  sourceReportName?: string
  sourceCellKey?: string
  sourceLabel?: string
  ruleId?: number
  ruleVersion?: number
  overwritePolicy?: string
  status?: string
}

export interface BatchRecordCellLinkPrefillRespVO {
  targetExecutionId: number
  prefills: BatchRecordCellLinkPrefillItemVO[]
  conflicts: BatchRecordCellLinkPrefillItemVO[]
}

export const BatchRecordCellLinkApi = {
  getWorkbenchContext: async (params: {
    routeId?: number
    definitionId?: number
    versionId?: number
    sourceReportId?: string
  }) => {
    return await request.get<BatchRecordCellLinkWorkbenchContextVO>({
      url: '/mes/pro/batch-record-cell-link/workbench-context',
      params
    })
  },

  getFormCells: async (params: { reportId: string; versionId?: number }) => {
    return await request.get<BatchRecordCellLinkFormCellsVO>({
      url: '/mes/pro/batch-record-cell-link/form-cells',
      params
    })
  },

  saveRules: async (data: BatchRecordCellLinkRulesSaveReqVO) => {
    return await request.post<BatchRecordCellLinkRulesSaveRespVO>({
      url: '/mes/pro/batch-record-cell-link/rules/save',
      data
    })
  },

  getPrefill: async (targetExecutionId: EdhrRouteId, workTaskId?: EdhrRouteId) => {
    return await request.get<BatchRecordCellLinkPrefillRespVO>({
      url: '/mes/pro/batch-record-cell-link/prefill',
      params: { targetExecutionId, workTaskId }
    })
  }
}
