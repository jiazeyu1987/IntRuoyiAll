import request from '@/config/axios'

const EDHR_OQ_PQ_CASE_PAGE_URL = '/mes/pro/edhr-oq-pq/case/page'
const EDHR_OQ_PQ_CASE_CREATE_URL = '/mes/pro/edhr-oq-pq/case/create'
const EDHR_OQ_PQ_RUN_PAGE_URL = '/mes/pro/edhr-oq-pq/run/page'
const EDHR_OQ_PQ_RUN_CREATE_URL = '/mes/pro/edhr-oq-pq/run/create'
const EDHR_OQ_PQ_RUN_SUBMIT_STEP_URL = '/mes/pro/edhr-oq-pq/run/submit-step'
const EDHR_OQ_PQ_RUN_COMPLETE_URL = '/mes/pro/edhr-oq-pq/run/complete'
const EDHR_OQ_PQ_DEVIATION_PAGE_URL = '/mes/pro/edhr-oq-pq/deviation/page'
const EDHR_OQ_PQ_DEVIATION_REMEDIATE_URL = '/mes/pro/edhr-oq-pq/deviation/remediate'
const EDHR_OQ_PQ_DEVIATION_RETEST_URL = '/mes/pro/edhr-oq-pq/deviation/retest'
const EDHR_OQ_PQ_DEVIATION_CLOSE_URL = '/mes/pro/edhr-oq-pq/deviation/close'

export const EDHR_OQ_PQ_QUERY_PERMISSION = 'mes:pro-edhr-oq-pq:query'
export const EDHR_OQ_PQ_CREATE_PERMISSION = 'mes:pro-edhr-oq-pq:create'
export const EDHR_OQ_PQ_EXECUTE_PERMISSION = 'mes:pro-edhr-oq-pq:execute'
export const EDHR_OQ_PQ_RETEST_PERMISSION = 'mes:pro-edhr-oq-pq:retest'
export const EDHR_OQ_PQ_CLOSE_PERMISSION = 'mes:pro-edhr-oq-pq:close'

export type EdhrOqPqCaseType = 'OQ' | 'PQ' | string
export type EdhrOqPqCaseStatus = 'ACTIVE' | 'INACTIVE' | string
export type EdhrOqPqRunStatus = 'CREATED' | 'RUNNING' | 'DEVIATION_OPEN' | 'PASSED' | 'BLOCKED' | string
export type EdhrOqPqStepResult = 'PASS' | 'FAIL' | 'BLOCKED' | string
export type EdhrOqPqDeviationStatus = 'OPEN' | 'REMEDIATED' | 'RETESTED' | 'CLOSED' | string

export interface EdhrOqPqCaseCreateReqVO {
  packageId: number
  caseCode: string
  caseName: string
  caseType: EdhrOqPqCaseType
  caseVersion: string
  stepNo: string
  stepTitle: string
  expectedResult: string
  evidenceRequirement: string
  ownerName: string
  reviewerName: string
  sort?: number
  remark?: string
}

export interface EdhrOqPqCasePageReqVO extends PageParam {
  packageId?: number
  caseCode?: string
  caseType?: EdhrOqPqCaseType
  caseStatus?: EdhrOqPqCaseStatus
}

export interface EdhrOqPqCaseRespVO {
  id: number
  packageId: number
  caseCode: string
  caseName: string
  caseType: EdhrOqPqCaseType
  caseVersion: string
  caseStatus: EdhrOqPqCaseStatus
  stepNo: string
  stepTitle: string
  expectedResult: string
  evidenceRequirement: string
  ownerName: string
  reviewerName: string
  sort?: number
  remark?: string
  createTime?: string
}

export interface EdhrOqPqRunCreateReqVO {
  packageId: number
  caseId: number
  executionEnvironment: string
  releaseTag: string
  schemaVersion: string
  executorName: string
  reviewerName: string
  realBusinessPath?: string
  realTestDataSource?: string
  targetEnvironmentProof?: string
  attachmentEvidence: string
  evidenceChecksum: string
  remark?: string
}

export interface EdhrOqPqRunPageReqVO extends PageParam {
  packageId?: number
  caseId?: number
  caseType?: EdhrOqPqCaseType
  runStatus?: EdhrOqPqRunStatus
  runCode?: string
}

export interface EdhrOqPqRunRespVO {
  id: number
  packageId: number
  caseId: number
  caseType: EdhrOqPqCaseType
  runCode: string
  runStatus: EdhrOqPqRunStatus
  executionEnvironment: string
  releaseTag: string
  schemaVersion: string
  executorName: string
  reviewerName: string
  executedAt?: string
  realBusinessPath?: string
  realTestDataSource?: string
  targetEnvironmentProof?: string
  attachmentEvidence: string
  evidenceChecksum: string
  openDeviationCount: number
  conclusion?: string
  blockedReason: string
  nextAction: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrOqPqStepSubmitReqVO {
  runId: number
  actualResult: string
  stepResult: EdhrOqPqStepResult
  attachmentEvidence: string
  evidenceChecksum: string
  remark?: string
}

export interface EdhrOqPqStepResultRespVO {
  id: number
  packageId: number
  caseId: number
  runId: number
  stepNo: string
  stepTitle: string
  expectedResult: string
  actualResult: string
  stepResult: EdhrOqPqStepResult
  executorName: string
  reviewerName: string
  executedAt?: string
  attachmentEvidence: string
  evidenceChecksum: string
  deviationId?: number
  nextAction: string
}

export interface EdhrOqPqDeviationPageReqVO extends PageParam {
  packageId?: number
  runId?: number
  deviationCode?: string
  deviationStatus?: EdhrOqPqDeviationStatus
}

export interface EdhrOqPqDeviationRespVO {
  id: number
  packageId: number
  caseId: number
  runId: number
  stepResultId: number
  deviationCode: string
  deviationTitle: string
  deviationStatus: EdhrOqPqDeviationStatus
  failedActualResult: string
  rootCause?: string
  remediationAction?: string
  remediationOwnerName?: string
  retestResult?: string
  retestEvidence?: string
  retestReviewerName?: string
  closeSignoffName?: string
  closedAt?: string
  blockedReason: string
  nextAction: string
  createTime?: string
}

export interface EdhrOqPqDeviationRemediateReqVO {
  deviationId: number
  rootCause: string
  remediationAction: string
  remediationOwnerName: string
}

export interface EdhrOqPqDeviationRetestReqVO {
  deviationId: number
  retestResult: string
  retestEvidence: string
  retestReviewerName: string
}

export interface EdhrOqPqDeviationCloseReqVO {
  deviationId: number
  closeSignoffName: string
}

export const getEdhrOqPqCasePage = async (params: EdhrOqPqCasePageReqVO) => {
  return await request.get<PageResult<EdhrOqPqCaseRespVO[]>>({
    url: EDHR_OQ_PQ_CASE_PAGE_URL,
    params
  })
}

export const createEdhrOqPqCase = async (data: EdhrOqPqCaseCreateReqVO) => {
  return await request.post<EdhrOqPqCaseRespVO>({
    url: EDHR_OQ_PQ_CASE_CREATE_URL,
    data
  })
}

export const getEdhrOqPqRunPage = async (params: EdhrOqPqRunPageReqVO) => {
  return await request.get<PageResult<EdhrOqPqRunRespVO[]>>({
    url: EDHR_OQ_PQ_RUN_PAGE_URL,
    params
  })
}

export const createEdhrOqPqRun = async (data: EdhrOqPqRunCreateReqVO) => {
  return await request.post<EdhrOqPqRunRespVO>({
    url: EDHR_OQ_PQ_RUN_CREATE_URL,
    data
  })
}

export const submitEdhrOqPqStepResult = async (data: EdhrOqPqStepSubmitReqVO) => {
  return await request.post<EdhrOqPqStepResultRespVO>({
    url: EDHR_OQ_PQ_RUN_SUBMIT_STEP_URL,
    data
  })
}

export const completeEdhrOqPqRun = async (runId: number) => {
  return await request.post<EdhrOqPqRunRespVO>({
    url: EDHR_OQ_PQ_RUN_COMPLETE_URL,
    params: { runId }
  })
}

export const getEdhrOqPqDeviationPage = async (params: EdhrOqPqDeviationPageReqVO) => {
  return await request.get<PageResult<EdhrOqPqDeviationRespVO[]>>({
    url: EDHR_OQ_PQ_DEVIATION_PAGE_URL,
    params
  })
}

export const remediateEdhrOqPqDeviation = async (data: EdhrOqPqDeviationRemediateReqVO) => {
  return await request.post<EdhrOqPqDeviationRespVO>({
    url: EDHR_OQ_PQ_DEVIATION_REMEDIATE_URL,
    data
  })
}

export const retestEdhrOqPqDeviation = async (data: EdhrOqPqDeviationRetestReqVO) => {
  return await request.post<EdhrOqPqDeviationRespVO>({
    url: EDHR_OQ_PQ_DEVIATION_RETEST_URL,
    data
  })
}

export const closeEdhrOqPqDeviation = async (data: EdhrOqPqDeviationCloseReqVO) => {
  return await request.post<EdhrOqPqDeviationRespVO>({
    url: EDHR_OQ_PQ_DEVIATION_CLOSE_URL,
    data
  })
}
