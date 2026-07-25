import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const readSource = (path) => readFileSync(resolve(root, path), 'utf8')

const apiSource = readSource('src/api/mes/pro/batchrecordreport/index.ts')
const pageSource = readSource('src/views/mes/pro/batchrecordformlist/index.vue')
const realE2ESource = readSource('tests/e2e/edhr-batch-version-phase1-real-flow.e2e.js')

assert.match(apiSource, /batchRecordDefinitionId\?:\s*number/, 'import result must expose definition id')
assert.match(apiSource, /batchRecordVersionId\?:\s*number/, 'import result must expose version id')
assert.match(apiSource, /sourceBatchRecordVersionId\?:\s*number/, 'import result must expose source version id')
assert.match(apiSource, /versionNo\?:\s*string/, 'import result must expose version number')
assert.match(apiSource, /versionStatus\?:\s*string/, 'import result must expose version status')
assert.match(apiSource, /submitBatchRecordVersionApproval:\s*async/, 'API must expose submit approval action')
assert.match(
  apiSource,
  /url:\s*'\/mes\/pro\/batch-record-report\/version-approval\/submit'/,
  'submit approval action must call the phase-one backend endpoint'
)

assert.match(pageSource, /lastWordImportResult/, 'page must retain last import version result')
assert.match(pageSource, /batchRecordVersionId/, 'page must bind imported version id')
assert.match(pageSource, /sourceBatchRecordVersionId/, 'page must bind source version id')
assert.match(pageSource, /versionNo/, 'page must render imported version number')
assert.match(pageSource, /versionStatus/, 'page must render imported version status')
assert.match(pageSource, /PENDING_APPROVAL/, 'page must keep pending approval status presentation')
assert.match(pageSource, /已自动提交升版审批/, 'import success must state automatic approval submission for pending upgrades')
assert.match(pageSource, /导入未自动提交升版审批/, 'unexpected upgrade status must fail fast instead of showing success')
assert.match(pageSource, /BatchRecordReportApi\.recognizeUploadedRoute/, 'page must import through the real route recognition API')
assert.match(pageSource, /是否升版本/, 'duplicate-name import confirmation must explain version upgrade semantics')
assert.match(pageSource, /confirmButtonText:\s*'升版'/, 'duplicate-name import confirmation must use explicit upgrade action text')
assert.match(pageSource, /cancelButtonText:\s*'退出导入'/, 'duplicate-name import cancellation must clearly exit this import task')
assert.doesNotMatch(
  pageSource,
  /需提交审批后生效/,
  'success toast must not claim every generated version needs manual approval'
)
assert.doesNotMatch(
  pageSource,
  /src\/views\/mes\/pro\/batchrecordtemplate\/index\.vue/,
  'phase-one contract must not reference the retired batchrecordtemplate page'
)
assert.match(realE2ESource, /\/mes\/pro\/batch-record-form-list/, 'real E2E must use the current batch record form list route')
assert.match(
  realE2ESource,
  /\/admin-api\/mes\/pro\/route\/flow-config['"]/,
  'real E2E must verify route flow config through the current backend endpoint'
)
assert.doesNotMatch(
  realE2ESource,
  /flow-config\/process-config-list/,
  'real E2E must not call the retired route flow config endpoint'
)

console.log('PASS: edhr batch version phase1 contract')
