const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const assert = require('node:assert/strict')

const {
  EXIT_CODES,
  ERROR_TYPES,
  REQUIRED_RESULT_FIELDS,
  exitCodeForErrorType,
  classifyError,
  writeFailureArtifacts
} = require('./edhr-ai-loop/reporter.cjs')

assert.deepEqual(ERROR_TYPES, [
  'PRECONDITION_BLOCKED',
  'UI_ACTION_FAILED',
  'BUSINESS_ASSERTION',
  'IDEMPOTENCY_FAILURE',
  'TRACEABILITY_FAILURE',
  'INFRASTRUCTURE_BLOCKED',
  'TEST_HARNESS_FAILURE'
])

assert.ok(REQUIRED_RESULT_FIELDS.includes('expected'))
assert.ok(REQUIRED_RESULT_FIELDS.includes('actual'))
assert.ok(REQUIRED_RESULT_FIELDS.includes('targetRequestEvidenceFlushed'))
assert.equal(exitCodeForErrorType('UI_ACTION_FAILED'), EXIT_CODES.TEST_HARNESS_FAILURE)
assert.equal(exitCodeForErrorType('TRACEABILITY_FAILURE'), EXIT_CODES.BUSINESS_FAILURE)
assert.equal(classifyError(new Error('locator.waitFor: Timeout 30000ms exceeded')).errorType, 'UI_ACTION_FAILED')
assert.equal(classifyError(new Error('page.goto: Timeout 60000ms exceeded\n  - navigating to "http://127.0.0.1:8081/login", waiting until "domcontentloaded"')).errorType, 'INFRASTRUCTURE_BLOCKED')
assert.equal(classifyError(new Error('net::ERR_CONNECTION_REFUSED')).errorType, 'INFRASTRUCTURE_BLOCKED')
assert.equal(classifyError(new Error('数量不一致')).errorType, 'BUSINESS_ASSERTION')

const output = fs.mkdtempSync(path.join(os.tmpdir(), 'edhr-ai-loop-report-schema-'))
const failure = writeFailureArtifacts({
  rootDir: output,
  runId: 'AI-EDHR-20260915T130000-A1B2',
  mode: 'full',
  failedStage: 'S02',
  errorType: 'UI_ACTION_FAILED',
  action: '一线生产提交',
  message: '按钮不可见',
  pageUrl: '/mes/pro/process-pool',
  expected: { submitButtonVisible: true },
  actual: { submitButtonVisible: false },
  targetRequests: [{ label: 'ACTIVE_ORDER_PAGE', method: 'GET', url: '/admin-api/mes/pro/process-pool/page', httpStatus: 200, businessCode: 0 }],
  targetRequestEvidenceFlushed: true,
  candidateCodePaths: ['IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'],
  stages: [{ stage: 'S01', status: 'PASS' }, { stage: 'S02', status: 'FAIL' }]
})

const result = JSON.parse(fs.readFileSync(path.join(failure.runDir, 'result.json'), 'utf8'))
for (const field of REQUIRED_RESULT_FIELDS) {
  assert.ok(Object.prototype.hasOwnProperty.call(result, field), `missing result field: ${field}`)
}
assert.deepEqual(result.expected, { submitButtonVisible: true })
assert.deepEqual(result.actual, { submitButtonVisible: false })
assert.equal(result.targetRequests[0].label, 'ACTIVE_ORDER_PAGE')
assert.equal(result.targetRequests[0].url, '/admin-api/mes/pro/process-pool/page')
assert.equal(result.targetRequests[0].httpStatus, 200)
assert.equal(result.targetRequests[0].businessCode, 0)
assert.equal(result.targetRequestEvidenceFlushed, true)
assert.equal(result.candidateCodePaths[0], 'IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

const report = fs.readFileSync(path.join(failure.runDir, 'report.md'), 'utf8')
assert.match(report, /## 期望值/)
assert.match(report, /submitButtonVisible/)
assert.match(report, /## 实际值/)
assert.match(report, /false/)
assert.match(report, /## 阶段结果/)
assert.match(report, /S02.*FAIL/)
assert.match(report, /## 页面请求证据/)
assert.match(report, /页面请求已刷入：true/)
assert.match(report, /\/admin-api\/mes\/pro\/process-pool\/page/)
assert.match(report, /## 候选代码路径/)
assert.match(report, /TeamLeaderWorkbenchPage\.vue/)

console.log('PASS: eDHR AI loop failure report schema and error classification')

