import request from '@/config/axios'
import type { EdhrRouteId } from '../edhr/batchExecution'

export interface BatchRecordCellLinkFormVO {
  id?: number
  batchRecordName?: string
  formSlotType?: string
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  routeProcessId?: number
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
  sourceType?: string
  sourceFieldCode?: string
  sourceFieldName?: string
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
  sourceType?: string
  sourceReportId: string
  sourceReportName?: string
  sourceRowIndex: number
  sourceColumnIndex: number
  sourceCellKey?: string
  sourceFieldCode?: string
  sourceFieldName?: string
  sourceLabel?: string
  sourceValueType?: string
  targetReportId: string
  targetReportName?: string
  targetRowIndex: number
  targetColumnIndex: number
  targetCellKey?: string
  targetLabel?: string
  targetValueType?: string
  aggregationStrategy?: string
  overwritePolicy?: string
  templateSnapshotHash?: string
  ruleVersion?: number
  enabled?: boolean
  remark?: string
}


export interface BatchRecordRepeatRowGroupRecordVO {
  recordSequence: number
  startRowIndex: number
  endRowIndex: number
  recordKey?: string
}

export interface BatchRecordRepeatRowGroupMappingVO {
  sourceType: string
  sourceFieldCode: string
  sourceFieldName?: string
  sourceValueType?: string
  templateTargetRowIndex: number
  templateTargetColumnIndex: number
  templateTargetCellKey?: string
  targetValueType?: string
  projectionTargetCellKey?: string
}

export interface BatchRecordRepeatRowGroupVO {
  id?: number
  scopeType?: string
  scopeId?: number
  routeId?: number
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  routeProcessId: number
  targetReportId: string
  targetReportName?: string
  templateStartRowIndex: number
  templateEndRowIndex: number
  repeatAreaStartRowIndex: number
  repeatAreaEndRowIndex: number
  sourceType?: string
  records: BatchRecordRepeatRowGroupRecordVO[]
  mappings: BatchRecordRepeatRowGroupMappingVO[]
  configVersion?: number
  templateSnapshotHash?: string
  enabled?: boolean
  remark?: string
}

export interface BatchRecordRepeatRowGroupSaveReqVO {
  scopeType?: string
  scopeId?: number
  routeId?: number
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  routeProcessId: number
  targetReportId: string
  templateStartRowIndex: number
  templateEndRowIndex: number
  repeatAreaStartRowIndex: number
  repeatAreaEndRowIndex: number
  records: BatchRecordRepeatRowGroupRecordVO[]
  mappings: BatchRecordRepeatRowGroupMappingVO[]
  enabled?: boolean
  remark?: string
}

export type BatchRecordRepeatRowGroupSaveRespVO = BatchRecordRepeatRowGroupVO
export interface BatchRecordCellLinkWorkbenchContextVO {
  scopeType: string
  scopeId: number
  routeId?: number
  batchRecordDefinitionId?: number
  batchRecordVersionId?: number
  forms: BatchRecordCellLinkFormVO[]
  sourceFields?: BatchRecordCellLinkSourceFieldVO[]
  defaultSourceReportId?: string
  defaultTargetReportId?: string
  rules: BatchRecordCellLinkRuleVO[]
  repeatRowGroups?: BatchRecordRepeatRowGroupVO[]
}

export interface BatchRecordCellLinkSourceFieldVO {
  sourceType: string
  fieldCode: string
  fieldName: string
  valueType?: string
  routeProcessId?: number
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
  repeatRowGroups?: BatchRecordRepeatRowGroupVO[]
}

export interface BatchRecordCellLinkRulesSaveRespVO {
  savedCount: number
  ruleVersion: number
  rules: BatchRecordCellLinkRuleVO[]
  repeatRowGroups?: BatchRecordRepeatRowGroupVO[]
}

export interface BatchRecordCellLinkPrefillItemVO {
  targetCellKey: string
  targetRowIndex: number
  targetColumnIndex: number
  value?: unknown
  sourceExecutionId?: number
  sourceType?: string
  sourceReportId?: string
  sourceReportName?: string
  sourceCellKey?: string
  sourceFieldCode?: string
  sourceFieldName?: string
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
    templateId?: number
    versionNo?: string
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

  saveRepeatRowGroup: async (data: BatchRecordRepeatRowGroupSaveReqVO) => {
    return await request.post<BatchRecordRepeatRowGroupSaveRespVO>({
      url: '/mes/pro/batch-record-cell-link/repeat-row-group/save',
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
