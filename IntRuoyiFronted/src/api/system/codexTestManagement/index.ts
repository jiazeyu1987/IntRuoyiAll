import request from '@/config/axios'

export interface CodexTestCheckpointVO {
  id?: number
  sort: number
  name: string
  expectedText: string
  severity?: string
  remark?: string
}

export interface CodexTestCaseVO {
  id?: number
  name: string
  methodText: string
  testDataText?: string
  defaultExecutionMode: 'SEQUENTIAL' | 'PARALLEL'
  parallelSafe: boolean
  status: 'ENABLE' | 'DISABLE'
  sort?: number
  checkpointCount?: number
  lastExecutionStatus?: string
  lastExecutionTime?: Date
  createTime?: Date
  checkpoints: CodexTestCheckpointVO[]
}

export interface CodexTestCasePageReqVO extends PageParam {
  name?: string
  status?: string
  executionMode?: string
}

export interface CodexTestExecutionStartReqVO {
  targetTenantId: number
  executionMode: 'SEQUENTIAL' | 'PARALLEL'
  caseIds: number[]
}

export interface CodexTestExecutionPageReqVO extends PageParam {
  targetTenantId?: number
  status?: string
  createTime?: Date[]
}

export interface CodexTestCheckpointResultVO {
  id: number
  checkpointSort: number
  checkpointNameSnapshot: string
  expectedTextSnapshot: string
  actualText?: string
  status: 'NOT_RUN' | 'PASS' | 'FAIL' | 'BLOCKED'
  mismatchDescription?: string
  screenshotArtifactId?: number
  completedAt?: Date
}

export interface CodexTestExecutionCaseVO {
  id: number
  caseId: number
  caseNameSnapshot: string
  methodTextSnapshot: string
  testDataTextSnapshot?: string
  checkpointCount: number
  status: string
  runnerSessionId?: number
  claimTime?: Date
  startedAt?: Date
  finishedAt?: Date
  failureReason?: string
  checkpointResults: CodexTestCheckpointResultVO[]
}

export interface CodexTestExecutionVO {
  id: number
  targetTenantId: number
  targetTenantName?: string
  executionMode: string
  status: string
  requestedBy: number
  runnerSessionId?: number
  startedAt?: Date
  finishedAt?: Date
  summary?: string
  createTime?: Date
  cases?: CodexTestExecutionCaseVO[]
}

export const getCodexTestCasePage = (params: CodexTestCasePageReqVO) => {
  return request.get<PageResult<CodexTestCaseVO>>({ url: '/system/codex-test-case/page', params })
}

export const getCodexTestCase = (id: number) => {
  return request.get<CodexTestCaseVO>({ url: '/system/codex-test-case/get?id=' + id })
}

export const createCodexTestCase = (data: CodexTestCaseVO) => {
  return request.post<number>({ url: '/system/codex-test-case/create', data })
}

export const updateCodexTestCase = (data: CodexTestCaseVO) => {
  return request.put<boolean>({ url: '/system/codex-test-case/update', data })
}

export const deleteCodexTestCase = (id: number) => {
  return request.delete<boolean>({ url: '/system/codex-test-case/delete?id=' + id })
}

export const startCodexTestExecution = (data: CodexTestExecutionStartReqVO) => {
  return request.post<number>({ url: '/system/codex-test-execution/start', data })
}

export const cancelCodexTestExecution = (executionId: number) => {
  return request.post<boolean>({
    url: '/system/codex-test-execution/cancel',
    data: { executionId }
  })
}

export const getCodexTestExecutionPage = (params: CodexTestExecutionPageReqVO) => {
  return request.get<PageResult<CodexTestExecutionVO>>({
    url: '/system/codex-test-execution/page',
    params
  })
}

export const getCodexTestExecution = (id: number) => {
  return request.get<CodexTestExecutionVO>({ url: '/system/codex-test-execution/get?id=' + id })
}

export const downloadCodexTestArtifact = (id: number) => {
  return request.download({ url: '/system/codex-test-execution/artifact?id=' + id })
}
