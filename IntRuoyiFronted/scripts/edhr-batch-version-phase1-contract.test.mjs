import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const readSource = (path) => readFileSync(resolve(root, path), 'utf8')

const apiSource = readSource('src/api/mes/pro/batchrecordreport/index.ts')
const pageSource = readSource('src/views/mes/pro/batchrecordtemplate/index.vue')
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

assert.match(pageSource, /data-testid="edhr-batch-version-phase1-panel"/, 'page must keep upgrade version panel')
assert.match(pageSource, /lastWordImportResult/, 'page must retain last import version result')
assert.match(pageSource, /batchRecordVersionId/, 'page must bind imported version id')
assert.match(pageSource, /versionNo/, 'page must render imported version number')
assert.match(pageSource, /versionStatus/, 'page must render imported version status')
assert.match(pageSource, /提交升版审批/, 'page must expose submit approval button')
assert.match(pageSource, /isInitialBatchRecordVersion/, 'page must distinguish V1.0 first version from upgrades')
assert.match(pageSource, /shouldShowBatchRecordVersionPanel/, 'page must hide version prompt panel for V1.0 first version')
assert.match(pageSource, /shouldShowVersionApprovalAction/, 'page must only show approval action for upgrade versions')
assert.doesNotMatch(pageSource, /V1\.0[\s\S]*无需审批/, 'page must not show any V1.0 prompt')
assert.match(pageSource, /batch-record-version-panel__meta/, 'upgrade version panel must use compact metadata layout')
assert.match(pageSource, /handleSubmitVersionApproval/, 'page must implement submit approval handler')
assert.match(
  pageSource,
  /BatchRecordReportApi\.submitBatchRecordVersionApproval/,
  'page must call approval submit API from the real UI path'
)
assert.doesNotMatch(
  pageSource,
  /需提交审批后生效/,
  'success toast must not claim every generated version needs approval'
)
assert.match(
  pageSource,
  /是否升版[\s\S]*生成新版本快照[\s\S]*审批通过后才会覆盖后续可用版本/,
  'duplicate-name import confirmation must explain safe version upgrade semantics'
)
assert.match(pageSource, /cancelButtonText:\s*'否，放弃本次导入'/, 'duplicate-name import cancellation must clearly abandon this import task')
assert.match(pageSource, /confirmButtonText:\s*'升版'/, 'duplicate-name import confirmation must use explicit upgrade action text')
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
