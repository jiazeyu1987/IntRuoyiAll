const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const helperPath = 'tests/e2e/signature-governance-real-flow-helper.js'
const e2eFiles = [
  'tests/e2e/signature-governance-retention-recovery.e2e.js',
  'tests/e2e/signature-governance-periodic-review.e2e.js',
  'tests/e2e/signature-governance-csv-package.e2e.js',
  'tests/e2e/signature-governance-policy.e2e.js'
]

for (const file of [helperPath, ...e2eFiles]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const helperSource = readText(helperPath)
const pageSource = readText('src/views/signature-governance/index.vue')
const dccSignatureSource = readText('src/views/dcc/controlled-file/signatures/index.vue')
const edhrSignatureSource = readText('src/views/mes/pro/edhr/SignaturePage.vue')

assert.doesNotMatch(pageSource, /<el-tabs/)
assert.doesNotMatch(pageSource, /<el-tab-pane/)
assert.doesNotMatch(pageSource, /signature-governance__toolbar/)
assert.doesNotMatch(pageSource, /signature-governance__tabs/)
assert.doesNotMatch(pageSource, /统一入口 \| 文件签名、批记录签名、授权与治理策略/)
assert.doesNotMatch(helperSource, /\.el-tabs__item/)
assert.doesNotMatch(helperSource, /\.el-tab-pane/)
assert.match(dccSignatureSource, /<component\s+:is="signaturePageShell"/)
assert.match(edhrSignatureSource, /<component\s+:is="signaturePageShell"/)
assert.match(dccSignatureSource, /\(?isEmbedded\.value\s*\?\s*'div'\s*:\s*ContentWrap\)?/)
assert.match(edhrSignatureSource, /\(?isEmbedded\.value\s*\?\s*'div'\s*:\s*ContentWrap\)?/)
assert.doesNotMatch(dccSignatureSource, /<ContentWrap[^>]*dcc-signature-page--embedded/)
assert.doesNotMatch(edhrSignatureSource, /<ContentWrap[^>]*edhr-signature-page--embedded/)

for (const envName of [
  'SIGNATURE_GOVERNANCE_E2E_BASE_URL',
  'SIGNATURE_GOVERNANCE_E2E_TENANT',
  'SIGNATURE_GOVERNANCE_E2E_USERNAME',
  'SIGNATURE_GOVERNANCE_E2E_PASSWORD'
]) {
  assert.match(helperSource, new RegExp(envName), `${envName} must be required`)
}

for (const endpoint of [
  '/signature-governance/retention/precheck',
  '/signature-governance/retention/dcc-evidence-receipts',
  '/signature-governance/retention/edhr-archive-receipts',
  '/signature-governance/periodic-review/batches',
  '/signature-governance/csv/packages/',
  '/signature-governance/policies/current'
]) {
  assert.match(helperSource, new RegExp(endpoint.replace(/[/-]/g, (match) => `\\${match}`)))
}

assert.match(helperSource, /\/dcc\/electronic-signatures\/page/)
assert.match(helperSource, /batch-record-execution-archive\/page/)
assert.match(helperSource, /edhr-release\/page/)
assert.match(helperSource, /edhr-validation-package\/page/)
assert.match(helperSource, /training-executions\/page/)
assert.match(helperSource, /edhr-change\/page/)
assert.match(helperSource, /loadDccSignatureCandidate/)
assert.match(helperSource, /使用此样本自动回填/)
assert.match(helperSource, /真实文件签名样本/)
for (const forbiddenManualEnv of [
  'SIGNATURE_GOVERNANCE_E2E_RETENTION_DCC_OBJECT_KEY',
  'SIGNATURE_GOVERNANCE_E2E_RETENTION_EDHR_OBJECT_KEY',
  'SIGNATURE_GOVERNANCE_E2E_RETENTION_EDHR_VERSION_ID',
  'SIGNATURE_GOVERNANCE_E2E_RETENTION_EDHR_ARCHIVE_HASH',
  'SIGNATURE_GOVERNANCE_E2E_RETENTION_EDHR_SIGNATURE_HASH',
  'SIGNATURE_GOVERNANCE_E2E_RETENTION_RECOVERY_BACKUP_ID',
  'SIGNATURE_GOVERNANCE_E2E_RETENTION_RECOVERY_EXPECTED_SHA256',
  'SIGNATURE_GOVERNANCE_E2E_REVIEW_SOURCE_TABLE',
  'SIGNATURE_GOVERNANCE_E2E_REVIEW_SOURCE_HASH',
  'SIGNATURE_GOVERNANCE_E2E_REVIEW_ACTION_CODE',
  'SIGNATURE_GOVERNANCE_E2E_CSV_MATERIAL_DOCUMENT_ID',
  'SIGNATURE_GOVERNANCE_E2E_CSV_TRACE_REQUIREMENT_REF',
  'SIGNATURE_GOVERNANCE_E2E_CSV_TRAINING_ID',
  'SIGNATURE_GOVERNANCE_E2E_CSV_CHANGE_CONTROL_ID',
  'SIGNATURE_GOVERNANCE_E2E_CSV_QA_APPROVAL_REF',
  'SIGNATURE_GOVERNANCE_E2E_RETENTION_ENDPOINT',
  'SIGNATURE_GOVERNANCE_E2E_RETENTION_BUCKET',
  'SIGNATURE_GOVERNANCE_E2E_RETENTION_RECOVERY_RUNTIME',
  'SIGNATURE_GOVERNANCE_E2E_REVIEW_OWNER',
  'SIGNATURE_GOVERNANCE_E2E_REVIEW_PERIOD_CODE',
  'SIGNATURE_GOVERNANCE_E2E_REVIEW_RULE_VERSION',
  'SIGNATURE_GOVERNANCE_E2E_REVIEW_DUE_DATE',
  'SIGNATURE_GOVERNANCE_E2E_CSV_QUALITY_OWNER',
  'SIGNATURE_GOVERNANCE_E2E_CSV_QA_APPROVER'
]) {
  assert.doesNotMatch(helperSource, new RegExp(forbiddenManualEnv))
}

for (const blockerCode of [
  'REVIEW_OWNER_MISSING',
  'QA_APPROVAL_MISSING'
]) {
  assert.match(helperSource, new RegExp(blockerCode))
}

assert.doesNotMatch(helperSource, /process\.env\.[A-Z0-9_]+\s*\|\|/)
assert.doesNotMatch(helperSource, /localhost:8081|172\.30\.30\.58:8081|aoteman|admin123|测试租户/)
assert.doesNotMatch(helperSource, /mock|fallback|TODO/i)
assert.doesNotMatch(helperSource, /\/signature-governance\/overview/)
assert.match(helperSource, /redirect=\/signature-governance\/signature-records/)
assert.match(helperSource, /assertSelectedTenant/)
assert.match(helperSource, /Login tenant mismatch/)
assert.match(helperSource, /\.el-form-item.*\.el-select/s)
assert.match(helperSource, /runReviewScenario\(page, config\)/)
assert.match(helperSource, /材料类型/)
assert.match(helperSource, /追溯关系/)
assert.match(helperSource, /培训记录/)
assert.match(helperSource, /变更控制/)
assert.match(helperSource, /QA批准/)
assert.match(helperSource, /记录DCC回执/)
assert.match(helperSource, /记录eDHR回执/)
assert.doesNotMatch(helperSource, /clickVisibleButton\(panel, '记录恢复演练'\)/)
assert.match(helperSource, /assertReady\(data, 'retention precheck'\)/)
assert.match(helperSource, /assertReady\([^,]+, 'current policy'\)/)
assert.match(helperSource, /\/signature-governance\/signature-records/)
assert.match(helperSource, /\/signature-governance\/authorizations/)
assert.doesNotMatch(helperSource, /\/signature-governance\?tab=/)
assert.doesNotMatch(helperSource, /\/signature-governance\/file-signatures/)
assert.doesNotMatch(helperSource, /\/signature-governance\/batch-signatures/)
assert.doesNotMatch(helperSource, /\/dcc\/controlled-file\/signatures/)
assert.doesNotMatch(helperSource, /\/mes\/pro\/feedback\/edhr-signatures/)
assert.match(helperSource, /moduleStatuses/)
assert.match(helperSource, /policySourcePresent === true/)
assert.match(helperSource, /authorityConfirmed === true/)

for (const file of e2eFiles) {
  const source = readText(file)
  assert.match(source, /runSignatureGovernanceScenario/)
  assert.doesNotMatch(source, /mock|fallback|TODO/i)
}

console.log('signature governance E2E static contract passed')
