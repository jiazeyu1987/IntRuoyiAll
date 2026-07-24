import request from '@/config/axios'

export type EdhrInitBatchStatus = 'DRAFT' | 'PRECHECK_FAILED' | 'PRECHECK_PASSED'
export type EdhrInitIssueLevel = 'BLOCKER' | 'WARNING'
export type EdhrInitIssueStatus = 'OPEN' | 'SUPERSEDED' | 'RESOLVED'

export interface EdhrInitBatchPageReqVO extends PageParam {
  projectCode?: string
  projectName?: string
  targetEnvironment?: string
  targetTenantId?: number
  dataVersion?: string
  status?: EdhrInitBatchStatus
  createTime?: string[]
}

export interface EdhrInitBatchCreateReqVO {
  projectCode: string
  projectName: string
  targetEnvironment: string
  targetTenantId: number
  dataVersion: string
  ownerUserId: number
  approvalOwnerUserId: number
  plannedStartTime?: string
  plannedEndTime?: string
  initScopeJson: string
  remark?: string
}

export interface EdhrInitManifestRespVO {
  id: number
  initBatchId: number
  packageType: string
  manifestHash: string
  sourceFileName: string
  sourceFileUrl?: string
  fileSize?: number
  checksumJson?: string
  manifestJson: string
  uploadStatus: string
  uploadedBy?: number
  uploadedAt?: string
}

export interface EdhrInitManifestUploadReqVO {
  initBatchId: number
  packageType: string
  manifestHash: string
  sourceFileName: string
  sourceFileUrl?: string
  fileSize?: number
  checksumJson?: string
  manifestJson: string
}

export interface EdhrInitBatchRespVO {
  id: number
  projectCode: string
  projectName: string
  targetEnvironment: string
  targetTenantId: number
  dataVersion: string
  ownerUserId: number
  approvalOwnerUserId: number
  plannedStartTime?: string
  plannedEndTime?: string
  initScopeJson: string
  status: EdhrInitBatchStatus
  manifestCount: number
  blockingIssueCount: number
  lastPrecheckAt?: string
  version?: number
  remark?: string
  latestManifestHash?: string
  manifests?: EdhrInitManifestRespVO[]
}

export interface EdhrInitIssuePageReqVO extends PageParam {
  initBatchId: number
  issueLevel?: EdhrInitIssueLevel
  issueStatus?: EdhrInitIssueStatus
  packageType?: string
  sourceFileName?: string
  responsibleName?: string
}

export interface EdhrInitIssueRespVO {
  id: number
  initBatchId: number
  initManifestId?: number
  issueCode: string
  issueLevel: EdhrInitIssueLevel
  issueStatus: EdhrInitIssueStatus
  packageType?: string
  sourceFileName?: string
  sourceRowNo?: number
  sourceFieldName?: string
  objectType?: string
  objectKey?: string
  responsibleUserId?: number
  responsibleName?: string
  issueMessage: string
  remediationSuggestion?: string
  impactScopeJson?: string
}

export interface EdhrInitBatchPrecheckRespVO {
  initBatchId: number
  status: EdhrInitBatchStatus
  manifestCount: number
  issueCount: number
  blockingIssueCount: number
  precheckAt?: string
  issues?: EdhrInitIssueRespVO[]
}

const EDHR_INIT_BATCH_BASE_URL = '/mes/pro/edhr-init-batch'

export const EdhrInitBatchApi = {
  getPage: async (params: EdhrInitBatchPageReqVO) =>
    await request.get<PageResult<EdhrInitBatchRespVO[]>>({
      url: `${EDHR_INIT_BATCH_BASE_URL}/page`,
      params
    }),

  getDetail: async (id: number) =>
    await request.get<EdhrInitBatchRespVO>({
      url: `${EDHR_INIT_BATCH_BASE_URL}/get`,
      params: { id }
    }),

  create: async (data: EdhrInitBatchCreateReqVO) =>
    await request.post<EdhrInitBatchRespVO>({
      url: `${EDHR_INIT_BATCH_BASE_URL}/create`,
      data
    }),

  uploadManifest: async (data: EdhrInitManifestUploadReqVO) =>
    await request.post<EdhrInitManifestRespVO>({
      url: `${EDHR_INIT_BATCH_BASE_URL}/upload`,
      data
    }),

  runPrecheck: async (id: number) =>
    await request.post<EdhrInitBatchPrecheckRespVO>({
      url: `${EDHR_INIT_BATCH_BASE_URL}/precheck`,
      params: { id }
    }),

  getIssuePage: async (params: EdhrInitIssuePageReqVO) =>
    await request.get<PageResult<EdhrInitIssueRespVO[]>>({
      url: `${EDHR_INIT_BATCH_BASE_URL}/issue/page`,
      params
    })
}
