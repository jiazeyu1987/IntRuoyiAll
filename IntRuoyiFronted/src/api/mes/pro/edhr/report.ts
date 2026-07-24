import request from '@/config/axios'

const EDHR_REPORT_CATALOG_PAGE_URL = '/mes/pro/edhr-report-catalog/page'
const EDHR_REPORT_CATALOG_DETAIL_URL = '/mes/pro/edhr-report-catalog/detail'
const EDHR_REPORT_DEFINITION_PAGE_URL = '/mes/pro/edhr-report-definition/page'
const EDHR_REPORT_DEFINITION_DETAIL_URL = '/mes/pro/edhr-report-definition/detail'
const EDHR_REPORT_QUERY_RUN_URL = '/mes/pro/edhr-report-query/run'
const EDHR_REPORT_QUERY_EXPORT_AUDIT_URL = '/mes/pro/edhr-report-query/export-audit'
const EDHR_REPORT_QUERY_EXPORT_AUDIT_PAGE_URL = '/mes/pro/edhr-report-query/export-audit/page'

export const EDHR_REPORT_QUERY_PERMISSION = 'mes:pro-edhr-report:query'
export const EDHR_REPORT_EXPORT_PERMISSION = 'mes:pro-edhr-report:export'

export type EdhrReportCatalogStatus = 'ACTIVE' | 'INACTIVE' | string
export type EdhrReportAcceptanceStatus = 'FIRST_SLICE_READY' | string
export type EdhrReportDefinitionStatus = 'PUBLISHED' | 'DRAFT' | 'ARCHIVED' | string
export type EdhrReportDataSourceStatus = 'READY' | 'ABNORMAL' | string
export type EdhrReportExportResultStatus = 'RECORDED' | 'FAILED' | string

export interface EdhrReportCatalogPageReqVO extends PageParam {
  reportCode?: string
  reportName?: string
  reportCategory?: string
  status?: EdhrReportCatalogStatus
  acceptanceStatus?: EdhrReportAcceptanceStatus
}

export interface EdhrReportCatalogRespVO {
  id: number
  reportCode: string
  reportName: string
  reportCategory?: string
  businessPurpose?: string
  primaryDimensions?: string
  relatedDimensions?: string
  dataSourceSummary?: string
  permissionPolicy?: string
  exportPolicy?: string
  status?: EdhrReportCatalogStatus
  acceptanceStatus?: EdhrReportAcceptanceStatus
  sort?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrReportDefinitionPageReqVO extends PageParam {
  reportCode?: string
  reportName?: string
  reportType?: string
  datasetCode?: string
  status?: EdhrReportDefinitionStatus
}

export interface EdhrReportDefinitionRespVO {
  id: number
  reportCode: string
  reportName: string
  reportType?: string
  datasetId?: number
  datasetCode?: string
  datasetVersion?: string
  status?: EdhrReportDefinitionStatus
  caliberVersion?: string
  fieldCaliberJson?: string
  filterSchemaJson?: string
  drilldownTargetJson?: string
  permissionSummaryJson?: string
  dataSourceStatus?: EdhrReportDataSourceStatus
  sampleQueryJson?: string
  publishedAt?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrReportQueryReqVO {
  reportDefinitionId?: number
  reportCode: string
  filterSnapshotJson?: string
}

export interface EdhrReportQueryRespVO {
  reportDefinitionId?: number
  reportCode: string
  reportName: string
  caliberVersion?: string
  dataUpdatedAt?: string
  filterSnapshotJson?: string
  permissionSummaryJson?: string
  dataSourceSummary?: string
  rows: Array<Record<string, unknown>>
}

export interface EdhrReportExportAuditReqVO {
  reportDefinitionId?: number
  reportCode: string
  filterSnapshotJson: string
  permissionSummaryJson: string
  dataRangeSummary: string
}

export interface EdhrReportExportAuditPageReqVO extends PageParam {
  reportCode?: string
  reportName?: string
  operationType?: string
  resultStatus?: EdhrReportExportResultStatus
  operatorUsername?: string
}

export interface EdhrReportExportAuditRespVO {
  id: number
  reportDefinitionId?: number
  reportCode: string
  reportName?: string
  caliberVersion?: string
  operationType?: string
  filterSnapshotJson?: string
  permissionSummaryJson?: string
  dataRangeSummary?: string
  resultStatus?: EdhrReportExportResultStatus
  failureReason?: string
  operatorUserId?: number
  operatorUsername?: string
  occurredAt?: string
  createTime?: string
}

export const getEdhrReportCatalogPage = async (params: EdhrReportCatalogPageReqVO) => {
  return await request.get<PageResult<EdhrReportCatalogRespVO[]>>({
    url: EDHR_REPORT_CATALOG_PAGE_URL,
    params
  })
}

export const getEdhrReportCatalogDetail = async (id: number) => {
  return await request.get<EdhrReportCatalogRespVO>({
    url: EDHR_REPORT_CATALOG_DETAIL_URL,
    params: { id }
  })
}

export const getEdhrReportDefinitionPage = async (params: EdhrReportDefinitionPageReqVO) => {
  return await request.get<PageResult<EdhrReportDefinitionRespVO[]>>({
    url: EDHR_REPORT_DEFINITION_PAGE_URL,
    params
  })
}

export const getEdhrReportDefinitionDetail = async (id: number) => {
  return await request.get<EdhrReportDefinitionRespVO>({
    url: EDHR_REPORT_DEFINITION_DETAIL_URL,
    params: { id }
  })
}

export const runEdhrReportQuery = async (data: EdhrReportQueryReqVO) => {
  return await request.post<EdhrReportQueryRespVO>({
    url: EDHR_REPORT_QUERY_RUN_URL,
    data
  })
}

export const recordEdhrReportExportAudit = async (data: EdhrReportExportAuditReqVO) => {
  return await request.post<EdhrReportExportAuditRespVO>({
    url: EDHR_REPORT_QUERY_EXPORT_AUDIT_URL,
    data
  })
}

export const getEdhrReportExportAuditPage = async (params: EdhrReportExportAuditPageReqVO) => {
  return await request.get<PageResult<EdhrReportExportAuditRespVO[]>>({
    url: EDHR_REPORT_QUERY_EXPORT_AUDIT_PAGE_URL,
    params
  })
}
