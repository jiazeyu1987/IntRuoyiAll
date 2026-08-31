const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const api = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')
const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

const extractInterface = (source, name) => {
  const matched = source.match(new RegExp(`export interface ${name}\\s*\\{([\\s\\S]*?)\\n\\}`))
  assert.ok(matched, `missing interface ${name}`)
  return matched[1]
}

const extractBlock = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notStrictEqual(startIndex, -1, `missing block start: ${start}`)
  const endIndex = source.indexOf(end, startIndex)
  assert.notStrictEqual(endIndex, -1, `missing block end: ${end}`)
  return source.slice(startIndex, endIndex)
}

const applyRequest = extractInterface(api, 'TeamLeaderActiveOrderReleaseApplyReqVO')
const requestFields = [...applyRequest.matchAll(/^\s*(\w+)\??\s*:/gm)].map((match) => match[1])
assert.deepStrictEqual(
  requestFields,
  ['activeOrderId', 'idempotencyKey', 'applyRemark'],
  'release apply request must contain exactly the three M0 fields.'
)
assert.match(applyRequest, /activeOrderId:\s*number/)
assert.match(applyRequest, /idempotencyKey:\s*string/)
assert.match(applyRequest, /applyRemark\?:\s*string/)
assert.doesNotMatch(
  api,
  /clientRequestId/,
  'the frontend must not introduce a second client request id.'
)

const blockerResponse = extractInterface(api, 'TeamLeaderActiveOrderReleaseBlockerRespVO')
for (const field of [
  'blockerType',
  'objectType',
  'objectId',
  'objectCode',
  'reason',
  'suggestion',
  'routeProcessId',
  'processId',
  'fieldCode',
  'cellKey'
]) {
  assert.match(blockerResponse, new RegExp(`\\b${field}\\??\\s*:`), `blocker must type ${field}.`)
}
assert.match(blockerResponse, /routeProcessId\?:\s*string/)
assert.match(blockerResponse, /processId\?:\s*string/)
assert.match(blockerResponse, /fieldCode\?:\s*string/)
assert.match(blockerResponse, /cellKey\?:\s*string/)

assert.match(
  api,
  /export type TeamLeaderActiveOrderReleaseApplicationStatus\s*=\s*\|?\s*'PQC_RELEASE_PENDING'[\s\S]*'PQC_RELEASE_REJECTED'[\s\S]*'REPORT_UPLOAD_PENDING'[\s\S]*'MANAGER_RELEASE_PENDING'[\s\S]*'RELEASED'/,
  'release application status must expose the five persistent workflow states.'
)
const applyResponse = extractInterface(api, 'TeamLeaderActiveOrderReleaseApplyRespVO')
for (const field of [
  'applicationId',
  'activeOrderId',
  'workOrderId',
  'routeId',
  'routeVersionId',
  'pqcReleaseWorkTaskId',
  'status',
  'sourceSnapshotHash',
  'version',
  'appliedAt'
]) {
  assert.match(
    applyResponse,
    new RegExp(`^\\s*${field}\\s*:`, 'm'),
    `${field} must be a required receipt field.`
  )
}
assert.doesNotMatch(applyResponse, /generatedDocuments/, 'V4 must not invent generatedDocuments.')

assert.match(page, /data-team-leader-active-order-release-blocker/)
assert.match(page, /data-team-leader-active-order-release-blocker-type/)
assert.match(page, /data-team-leader-active-order-release-blocker-locator/)
assert.match(page, /resolveActiveOrderReleaseBlockerLocator\(blocker\)/)
assert.match(page, /blocker\.reason/)
assert.match(page, /blocker\.suggestion/)
assert.doesNotMatch(
  extractBlock(
    page,
    'v-if="releaseApplicationBlockers.length"',
    'v-model="activeOrderDetailVisible"'
  ),
  /blocker\.reason\s*\|\|\s*blocker\.blockerType/,
  'required blocker reason must not silently fall back to blockerType.'
)

const releaseFlow = extractBlock(
  page,
  'const releaseApplicationIdempotencyKeys',
  'const submitMoveActiveOrder'
)
assert.match(
  releaseFlow,
  /const getOrCreateActiveOrderReleaseIdempotencyKey[\s\S]*releaseApplicationIdempotencyKeys\.get\(row\.id\)[\s\S]*releaseApplicationIdempotencyKeys\.set\(row\.id, idempotencyKey\)/,
  'a retry must reuse one stable idempotency key for the active order.'
)
assert.doesNotMatch(
  releaseFlow,
  /idempotencyKey:\s*buildActiveOrderReleaseIdempotencyKey\(row\)/,
  'the submit call must not generate a fresh idempotency key on every retry.'
)
assert.match(
  releaseFlow,
  /applyTeamLeaderActiveOrderRelease\(\{\s*activeOrderId(?:\s*,|\s*:)[\s\S]*idempotencyKey(?:\s*,|\s*:)[\s\S]*applyRemark\s*:[\s\S]*\}\)/,
  'the page must submit the three formal M0 request fields.'
)
const applyCall =
  releaseFlow.match(/applyTeamLeaderActiveOrderRelease\(\{[\s\S]*?\n\s*\}\)/)?.[0] || ''
assert.doesNotMatch(
  applyCall,
  /(?:batchExecutionId|releaseTransactionId|releaseApprovalWorkTaskId|sourceSnapshotHash|signatureEvidenceCount)\s*:/,
  'the apply request must not construct backend ids, source hashes, or signature evidence.'
)
assert.doesNotMatch(
  releaseFlow,
  /result\.(?:batchExecutionId|releaseTransactionId|releaseApprovalWorkTaskId|status|dossierSummary)\s*=(?!=)/,
  'the page must never overwrite ids, status, or evidence from the formal backend receipt.'
)

assert.match(
  releaseFlow,
  /const confirmActiveOrderReleaseApplicationReceipt[\s\S]*await getTeamLeaderActiveOrderRelease\(row\.id\)/,
  'an uncertain apply response must use the formal read-only active-order receipt keyed by activeOrderId.'
)
assert.match(
  releaseFlow,
  /const isDefinitiveActiveOrderReleaseBusinessFailure[\s\S]*typeof code === 'number'/,
  'a backend business error must be distinguishable from an uncertain network response.'
)
assert.match(
  releaseFlow,
  /catch \(writeError\) \{[\s\S]*resolveActiveOrderReleaseFailure\(writeError\)[\s\S]*isDefinitiveActiveOrderReleaseBusinessFailure\(writeError\)[\s\S]*releaseApplicationLocks\.delete\(row\.id\)[\s\S]*recoverUncertainActiveOrderReleaseApplication\(row, writeError\)/,
  'the write-error branch must show definitive completion/configuration failures directly and only recover genuinely uncertain responses.'
)
assert.match(
  releaseFlow,
  /releaseApplicationLocks\.set\(row\.id, 'UNCERTAIN'\)/,
  'failed or ambiguous receipt confirmation must enter an explicit uncertain lock state.'
)
assert.match(releaseFlow, /申请响应不确定[\s\S]*请人工核对/)
assert.match(
  page,
  /:disabled="\s*!canApplyActiveOrderRelease\(row\)\s*\|\|\s*isActiveOrderReleaseApplicationLocked\(row\.id\)/,
  'a locally confirmed or uncertain write must disable duplicate submission.'
)
assert.match(page, /data-team-leader-active-order-release-uncertain/)

assert.match(
  releaseFlow,
  /releaseApplicationLocks\.set\(row\.id, 'CONFIRMED_REFRESH_FAILED'\)[\s\S]*`申请已提交，但列表刷新失败：\$\{resolveErrorMessage\(refreshError, '列表刷新失败'\)\}`/,
  'a refresh failure after a formal write receipt must preserve and state the successful write fact.'
)
assert.match(
  releaseFlow,
  /const refreshedReceipt = activeOrderOptions\.value\.find\([\s\S]*refreshedReceipt\?\.releaseApplicationId === result\.applicationId[\s\S]*refreshedReceipt\.releaseApplicationStatus === result\.status[\s\S]*releaseApplicationLocks\.delete\(row\.id\)[\s\S]*releaseApplicationLocks\.set\(row\.id, 'CONFIRMED_NOT_PROJECTED'\)/,
  'a successful list request must not unlock duplicate submission until it projects the formal write status.'
)
assert.match(
  releaseFlow,
  /assertActiveOrderReleaseApplicationReceipt\(result, row\.id, true\)/,
  'the UI must consume a formal backend receipt rather than fabricate success state.'
)
assert.match(
  releaseFlow,
  /系统将先完成生产订单和正式资料回填，再提交生产放行申请并生成一个PQC待办/,
  'the confirmation copy must disclose the formal completion and backfill step before the PQC application.'
)

console.log('PASS: team leader active-order release application static contract')
