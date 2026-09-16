const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const assert = require('node:assert/strict')
const { createRunId, buildManifest } = require('./edhr-ai-loop/manifest.cjs')
const { EXIT_CODES, REQUIRED_RESULT_FIELDS, writeRunReport, writeFailureArtifacts } = require('./edhr-ai-loop/reporter.cjs')
const { ordersForMode } = require('./edhr-ai-loop/runner.cjs')

const repoRoot = path.resolve(__dirname, '../../..')
const fixturePath = path.join(repoRoot, 'doc/tasks/20260915-edhr-fixed-io-e2e-plan/fixtures.json')
const fixtures = JSON.parse(fs.readFileSync(fixturePath, 'utf8'))

const runId = createRunId(new Date('2026-09-15T04:00:00.000Z'), 'A1B2')
assert.equal(runId, 'AI-EDHR-20260915T040000-A1B2')
const configuredTemplateManifest = buildManifest({ runId, mode: 'full' })
assert.equal(configuredTemplateManifest.templateWorkOrderCode, null)
assert.equal(configuredTemplateManifest.templateSource, 'CONFIGURED_TEMPLATE')
const manifest = buildManifest({ runId, mode: 'full', templateWorkOrderCode: 'EDHR-E2E-TEMPLATE-WO' })
assert.equal(manifest.templateSource, 'EXPLICIT_CODE')
assert.equal(manifest.orders.length, 5)
assert.equal(manifest.orders[0].workOrderCode, `${runId}-O01`)
assert.equal(manifest.orders[0].quantity, 100)
assert.equal(manifest.expected.pqcTaskCount, 16)
assert.deepEqual(ordersForMode(manifest).map((order) => order.slot), ['O01'])
assert.equal(ordersForMode(buildManifest({ runId, mode: 'regression', templateWorkOrderCode: 'EDHR-E2E-TEMPLATE-WO' })).length, 5)
assert.equal(ordersForMode(buildManifest({ runId, mode: 'repeatability', templateWorkOrderCode: 'EDHR-E2E-TEMPLATE-WO' })).length, 5)
assert.ok(fixtures.repeatability.aiLoop.reportErrorTypes.includes('TEST_HARNESS_FAILURE'))
assert.notEqual(createRunId(new Date('2026-09-15T04:00:01.000Z'), 'A1B3'), runId)
assert.throws(() => buildManifest({ runId: 'bad', mode: 'full', templateWorkOrderCode: 'X' }))

const output = fs.mkdtempSync(path.join(os.tmpdir(), 'edhr-ai-loop-'))
const result = { schemaVersion: 'AI_EDHR_E2E_RESULT_V1', runId, status: 'FAIL', failedStage: 'S04',
  action: '完成订单', expected: { lot: 'LOT-A01' }, actual: { lot: null },
  errorType: 'TRACEABILITY_FAILURE', pageUrl: '/mes/pro/process-pool', screenshot: 'S04.png',
  trace: 'trace.zip', targetRequests: [], candidateCodePaths: [], startedAt: '2026-09-15T04:00:00Z',
  finishedAt: '2026-09-15T04:01:00Z' }
const runDir = writeRunReport({ rootDir: output, manifest, result })
const savedResult = JSON.parse(fs.readFileSync(path.join(runDir, 'result.json'), 'utf8'))
for (const field of REQUIRED_RESULT_FIELDS) assert.ok(Object.prototype.hasOwnProperty.call(savedResult, field), `missing result field: ${field}`)
assert.equal(savedResult.runId, result.runId)
assert.equal(savedResult.failedStage, 'S04')
assert.equal(savedResult.errorType, 'TRACEABILITY_FAILURE')
assert.deepEqual(savedResult.expected, result.expected)
assert.deepEqual(savedResult.actual, result.actual)
assert.deepEqual(savedResult.targetRequests, [])
assert.deepEqual(savedResult.candidateCodePaths, [])
const failure = writeFailureArtifacts({ rootDir: output, runId: createRunId(new Date('2026-09-15T04:00:02.000Z'), 'A1B4'),
  mode: 'full', failedStage: 'S02', errorType: 'PRECONDITION_BLOCKED', action: '前置阻塞',
  message: '缺少模板工单', pageUrl: '/login', stages: [{ stage: 'S01', status: 'PASS' }] })
const failureResult = JSON.parse(fs.readFileSync(path.join(failure.runDir, 'result.json'), 'utf8'))
for (const field of REQUIRED_RESULT_FIELDS) assert.ok(Object.prototype.hasOwnProperty.call(failureResult, field), `missing failure result field: ${field}`)
assert.equal(failureResult.stages[0].stage, 'S01')
assert.equal(EXIT_CODES.BUSINESS_FAILURE, 10)
assert.equal(EXIT_CODES.PRECONDITION_BLOCKED, 20)

console.log('PASS: eDHR AI loop manifest and report contract')
