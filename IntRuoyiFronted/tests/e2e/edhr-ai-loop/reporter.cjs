const fs = require('node:fs')
const path = require('node:path')

const EXIT_CODES = Object.freeze({ PASS: 0, BUSINESS_FAILURE: 10, PRECONDITION_BLOCKED: 20,
  INFRASTRUCTURE_BLOCKED: 30, TEST_HARNESS_FAILURE: 40 })
const ERROR_TYPES = Object.freeze([
  'PRECONDITION_BLOCKED',
  'UI_ACTION_FAILED',
  'BUSINESS_ASSERTION',
  'IDEMPOTENCY_FAILURE',
  'TRACEABILITY_FAILURE',
  'INFRASTRUCTURE_BLOCKED',
  'TEST_HARNESS_FAILURE'
])
const REQUIRED_RESULT_FIELDS = Object.freeze([
  'schemaVersion',
  'runId',
  'status',
  'failedStage',
  'action',
  'expected',
  'actual',
  'errorType',
  'pageUrl',
  'screenshot',
  'trace',
  'targetRequests',
  'targetRequestEvidenceFlushed',
  'candidateCodePaths',
  'stages',
  'startedAt',
  'finishedAt'
])

function exitCodeForErrorType(errorType) {
  if (!ERROR_TYPES.includes(errorType)) throw new Error(`unknown eDHR AI E2E errorType: ${errorType}`)
  if (errorType === 'PRECONDITION_BLOCKED') return EXIT_CODES.PRECONDITION_BLOCKED
  if (errorType === 'INFRASTRUCTURE_BLOCKED') return EXIT_CODES.INFRASTRUCTURE_BLOCKED
  if (errorType === 'UI_ACTION_FAILED' || errorType === 'TEST_HARNESS_FAILURE') return EXIT_CODES.TEST_HARNESS_FAILURE
  return EXIT_CODES.BUSINESS_FAILURE
}

function classifyError(error) {
  const explicitType = error && typeof error === 'object' ? error.errorType : null
  if (explicitType) {
    return { errorType: explicitType, exitCode: exitCodeForErrorType(explicitType) }
  }
  const message = error instanceof Error ? error.message : String(error || '')
  if (/page\.goto: Timeout \d+ms exceeded/i.test(message)) {
    return { errorType: 'INFRASTRUCTURE_BLOCKED', exitCode: EXIT_CODES.INFRASTRUCTURE_BLOCKED }
  }
  if (/net::ERR_|ECONNREFUSED|ECONNRESET|ENOTFOUND|ETIMEDOUT|ERR_CONNECTION_REFUSED|ERR_CONNECTION_RESET/i.test(message)) {
    return { errorType: 'INFRASTRUCTURE_BLOCKED', exitCode: EXIT_CODES.INFRASTRUCTURE_BLOCKED }
  }
  if (/locator|strict mode violation|waiting for|Timeout \d+ms exceeded|element is not|not visible|not enabled|click|fill|select option|dialog|button/i.test(message)) {
    return { errorType: 'UI_ACTION_FAILED', exitCode: EXIT_CODES.TEST_HARNESS_FAILURE }
  }
  return { errorType: 'BUSINESS_ASSERTION', exitCode: EXIT_CODES.BUSINESS_FAILURE }
}

function atomicWriteJson(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  const temporary = `${filePath}.${process.pid}.tmp`
  fs.writeFileSync(temporary, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
  fs.renameSync(temporary, filePath)
}

function normalizeResult(result) {
  const normalized = {
    schemaVersion: 'AI_EDHR_E2E_RESULT_V1',
    runId: result?.runId,
    status: result?.status || 'BLOCKED',
    failedStage: result?.failedStage ?? null,
    action: result?.action ?? null,
    expected: result?.expected || {},
    actual: result?.actual || {},
    errorType: result?.errorType ?? null,
    pageUrl: result?.pageUrl ?? null,
    screenshot: result?.screenshot ?? null,
    trace: result?.trace ?? null,
    targetRequests: Array.isArray(result?.targetRequests) ? result.targetRequests : [],
    targetRequestEvidenceFlushed: result?.targetRequestEvidenceFlushed === true,
    candidateCodePaths: Array.isArray(result?.candidateCodePaths) ? result.candidateCodePaths : [],
    stages: Array.isArray(result?.stages) ? result.stages : [],
    startedAt: result?.startedAt || new Date().toISOString(),
    finishedAt: result?.finishedAt ?? null,
    message: result?.message ?? null
  }
  if (!normalized.runId) throw new Error('result.runId is required')
  if (normalized.errorType) exitCodeForErrorType(normalized.errorType)
  for (const field of REQUIRED_RESULT_FIELDS) {
    if (!Object.prototype.hasOwnProperty.call(normalized, field)) throw new Error(`missing result field: ${field}`)
  }
  return normalized
}

function formatJsonBlock(value) {
  const json = JSON.stringify(value ?? {}, null, 2)
  return ['```json', json, '```'].join('\n')
}

function formatStageTable(stages) {
  if (!stages.length) return '- 无阶段结果记录'
  return [
    '| 阶段 | 状态 |',
    '|---|---|',
    ...stages.map((stage) => `| ${stage.stage || '--'} | ${stage.status || '--'} |`)
  ].join('\n')
}

function formatRequestTable(targetRequests) {
  if (!targetRequests.length) return '- 无页面请求证据'
  return [
    '| 标签 | 方法 | URL | HTTP | 业务码 |',
    '|---|---|---|---:|---:|',
    ...targetRequests.map((request) =>
      `| ${request.label || '--'} | ${request.method || '--'} | ${request.url || '--'} | ${request.httpStatus ?? '--'} | ${request.businessCode ?? '--'} |`
    )
  ].join('\n')
}

function formatCandidateCodePaths(candidateCodePaths) {
  if (!candidateCodePaths.length) return '- 无候选代码路径'
  return candidateCodePaths.map((item) => `- ${item}`).join('\n')
}

function writeRunReport({ rootDir, manifest, result }) {
  const normalizedResult = normalizeResult(result)
  if (!manifest?.runId || normalizedResult.runId !== manifest.runId) throw new Error('result must match manifest runId')
  const runDir = path.resolve(rootDir, manifest.runId)
  atomicWriteJson(path.join(runDir, 'manifest.json'), manifest)
  atomicWriteJson(path.join(runDir, 'result.json'), normalizedResult)
  const report = [`# eDHR AI E2E ${manifest.runId}`, '', `- 状态：${normalizedResult.status}`,
    `- 失败阶段：${normalizedResult.failedStage || '--'}`, `- 错误类型：${normalizedResult.errorType || '--'}`,
    `- 动作：${normalizedResult.action || '--'}`,
    `- 页面：${normalizedResult.pageUrl || '--'}`,
    `- Trace：${normalizedResult.trace || '--'}`,
    `- 截图：${normalizedResult.screenshot || '--'}`,
    `- 页面请求已刷入：${normalizedResult.targetRequestEvidenceFlushed}`,
    `- 消息：${normalizedResult.message || '--'}`,
    '',
    '## 期望值',
    '',
    formatJsonBlock(normalizedResult.expected),
    '',
    '## 实际值',
    '',
    formatJsonBlock(normalizedResult.actual),
    '',
    '## 阶段结果',
    '',
    formatStageTable(normalizedResult.stages),
    '',
    '## 页面请求证据',
    '',
    formatRequestTable(normalizedResult.targetRequests),
    '',
    '## 候选代码路径',
    '',
    formatCandidateCodePaths(normalizedResult.candidateCodePaths),
    ''].join('\n')
  fs.writeFileSync(path.join(runDir, 'report.md'), report, 'utf8')
  return runDir
}

function writeFailureArtifacts({
  rootDir,
  runId,
  mode,
  failedStage,
  errorType,
  action,
  message,
  pageUrl,
  expected = {},
  actual = {},
  targetRequests = [],
  candidateCodePaths = [],
  stages = []
}) {
  const manifest = {
    schemaVersion: 'AI_EDHR_E2E_RUN_V1',
    runId,
    mode,
    resetWorkOrderCode: null,
    createdAt: new Date().toISOString(),
    orders: [],
    expected: {}
  }
  const result = {
    schemaVersion: 'AI_EDHR_E2E_RESULT_V1',
    runId,
    status: 'BLOCKED',
    failedStage,
    errorType,
    action,
    expected,
    actual,
    message,
    pageUrl: pageUrl || null,
    screenshot: null,
    trace: null,
    targetRequests,
    targetRequestEvidenceFlushed: true,
    candidateCodePaths,
    stages,
    startedAt: new Date().toISOString(),
    finishedAt: new Date().toISOString()
  }
  return { runDir: writeRunReport({ rootDir, manifest, result }), manifest, result }
}

module.exports = {
  EXIT_CODES,
  ERROR_TYPES,
  REQUIRED_RESULT_FIELDS,
  exitCodeForErrorType,
  classifyError,
  atomicWriteJson,
  writeRunReport,
  writeFailureArtifacts
}
