import request from '@/config/axios'

export type FormFillLogContextStatus =
  | 'COMPLETE'
  | 'BATCH_CONTEXT_MISSING'
  | 'EXECUTION_MISSING'

export type FormFillLogHashStatus = 'VALID' | 'UNKNOWN' | 'CHECK_REQUIRED'

export interface FormFillLogPageReqVO extends PageParam {
  batchRecordReportId?: string
  formKeyword?: string
  changedAtStart?: string
  changedAtEnd?: string
  actorId?: number
  actorName?: string
  batchCode?: string
  workOrderCode?: string
  executionCode?: string
}

export interface FormFillLogPageRespVO {
  auditBatchId: number
  executionId?: number
  executionCode?: string
  batchRecordReportId?: string
  formName?: string
  batchExecutionId?: number
  batchCode?: string
  workOrderCode?: string
  actorId?: number
  actorName?: string
  changedAt?: string
  fieldCount?: number
  cellSummary?: string
  contextStatus: FormFillLogContextStatus | string
  hashStatus?: FormFillLogHashStatus | string
}

export interface FormFillLogItemRespVO {
  auditItemId: number
  fieldPath?: string
  fieldKey?: string
  fieldLabel?: string
  rowIndex?: number
  columnIndex?: number
  oldValueDisplay?: string
  newValueDisplay?: string
  recordbookValueDisplay?: string
  batchRecordValueDisplay?: string
  changedAt?: string
}

export interface FormFillLogDetailRespVO extends Omit<FormFillLogPageRespVO, 'cellSummary'> {
  items: FormFillLogItemRespVO[]
}

export interface ProductionReportRevisionLogPageReqVO extends PageParam {
  workOrderCode?: string
  processKeyword?: string
  actualEmployeeName?: string
  modifiedByName?: string
  modifiedAtStart?: string
  modifiedAtEnd?: string
}

export interface ProductionReportRevisionLogChangeVO {
  fieldName: string
  beforeValue: string
  afterValue: string
}

export interface ProductionReportRevisionLogPageRespVO {
  revisionId: number
  eventId: number
  workOrderCode?: string
  workOrderName?: string
  processCode?: string
  processName?: string
  actualEmployeeName?: string
  submittedAt?: string
  modifiedByName: string
  modifiedAt?: string
  changeReason: string
  signatureConfirmed: boolean
  fieldCount: number
  changeSummary: string
}

export interface ProductionReportRevisionLogDetailRespVO
  extends ProductionReportRevisionLogPageRespVO {
  changes: ProductionReportRevisionLogChangeVO[]
}

export const getFormFillLogPage = async (params: FormFillLogPageReqVO) => {
  return await request.get<PageResult<FormFillLogPageRespVO[]>>({
    url: '/mes/pro/batch-record-execution/form-fill-log/page',
    params
  })
}

export const getFormFillLogDetail = async (auditBatchId: number) => {
  return await request.get<FormFillLogDetailRespVO>({
    url: '/mes/pro/batch-record-execution/form-fill-log/detail',
    params: { auditBatchId }
  })
}

export const getProductionReportRevisionLogPage = async (
  params: ProductionReportRevisionLogPageReqVO
) => {
  return await request.get<PageResult<ProductionReportRevisionLogPageRespVO[]>>({
    url: '/mes/pro/batch-record-execution/form-fill-log/production-report-revision/page',
    params
  })
}

export const getProductionReportRevisionLogDetail = async (revisionId: number) => {
  return await request.get<ProductionReportRevisionLogDetailRespVO>({
    url: '/mes/pro/batch-record-execution/form-fill-log/production-report-revision/detail',
    params: { revisionId }
  })
}
