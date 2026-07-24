const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const scriptPath = path.resolve(__dirname, 'edhr-dual-track-real-flow.e2e.js')
const source = fs.readFileSync(scriptPath, 'utf8')

const requiredCoverageMarkers = [
  'operatorControlledFill',
  'operatorInternalTraceFill',
  'controlledLimitBlocked',
  'internalTraceReasonRequired',
  'internalTraceNonBlocking',
  'approverApproval',
  'internalReviewerReview',
  'externalAuditorDeniedInternal',
  'requiredInternalTraceCloseBlocked'
]

const requiredRuntimeEvidence = [
  '/mes/pro/edhr-batch-execution/open-or-create',
  '/mes/pro/edhr-batch-execution/task/open',
  '/mes/pro/batch-record-execution/field-audit/save-changes',
  '/mes/pro/batch-record-execution/cosign',
  '/mes/pro/batch-record-execution/submit',
  '/mes/pro/batch-record-execution/approval-pending-page',
  '/mes/pro/batch-record-execution/approve',
  '/mes/pro/edhr-batch-execution/close'
]

const missingMarkers = requiredCoverageMarkers.filter((marker) => !source.includes(marker))
const missingRuntimeEvidence = requiredRuntimeEvidence.filter((endpoint) => !source.includes(endpoint))

assert.deepEqual(
  missingMarkers,
  [],
  `edhr-dual-track-real-flow.e2e.js 缺少双轨真实 E2E 覆盖标记: ${missingMarkers.join(', ')}`
)
assert.deepEqual(
  missingRuntimeEvidence,
  [],
  `edhr-dual-track-real-flow.e2e.js 缺少真实业务动作接口监听: ${missingRuntimeEvidence.join(', ')}`
)

console.log('PASS: edhr dual-track full coverage static gate')