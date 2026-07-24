const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const nasPagePath = path.join(repoRoot, 'src/views/system/nas/index.vue')
const workflowApiPath = path.join(repoRoot, 'src/api/dcc/controlledFile/workflow.ts')
const submitterPath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/submitter.ts')
const uploadPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/index.vue')
const externalReviewPagePath = path.join(
  repoRoot,
  'src/views/dcc/controlled-file/external-review/index.vue'
)
const metadataDialogPath = path.join(
  repoRoot,
  'src/views/dcc/controlled-file/shared/ControlledFileMetadataDialog.vue'
)

const nasSource = fs.readFileSync(nasPagePath, 'utf8')
const workflowApiSource = fs.readFileSync(workflowApiPath, 'utf8')
const submitterSource = fs.readFileSync(submitterPath, 'utf8')
const uploadPageSource = fs.readFileSync(uploadPagePath, 'utf8')
const externalReviewPageSource = fs.readFileSync(externalReviewPagePath, 'utf8')
const metadataDialogSource = fs.readFileSync(metadataDialogPath, 'utf8')

assert(
  !/productMasterId:\s*\[\{\s*required:\s*true/.test(nasSource),
  'NAS transfer dialog must not require DCC product selection'
)
assert(
  !nasSource.includes('DCC 产品缺少启用且带 DCC 产品码的产品，请先维护产品后再转移/导入'),
  'NAS transfer dialog must not block non-product files when DCC product options are empty'
)
assert.match(
  nasSource,
  /const buildLocalFolderImportSessionPayload[\s\S]*productMasterId:\s*transferDialog\.form\.productMasterId\s*\?\?\s*undefined/,
  'Local folder import session payload must send an empty product binding as undefined'
)
assert.match(
  nasSource,
  /productMasterId:\s*transferDialog\.form\.productMasterId\s*\?\?\s*undefined/,
  'NAS transfer payload must send an empty product binding as undefined instead of a fake product id'
)

assert.match(
  workflowApiSource,
  /productMasterId\?:\s*number\s*\|\s*null/,
  'DCC submit API type must allow productMasterId to be omitted or null'
)
assert(
  !/assertRequiredNumber\(payload,\s*['"]productMasterId['"]/.test(workflowApiSource),
  'DCC submit API contract guard must not require productMasterId'
)

assert.match(
  submitterSource,
  /if\s*\(!productMasterId\)\s*\{[\s\S]*if\s*\(!trimText\(productCode\)\)\s*\{[\s\S]*valid:\s*true/,
  'Upload submitter must accept an empty product binding'
)
assert(
  !/productMasterId:\s*\[\{\s*required:\s*true/.test(uploadPageSource),
  'Controlled file upload page must not require product selection'
)
assert(
  !/productMasterId:\s*\[\{\s*required:\s*true/.test(externalReviewPageSource),
  'External review page must not require product selection'
)
assert.match(
  metadataDialogSource,
  /if\s*\(metadataForm\.productMasterId\s*&&\s*!selectedProduct\.value\?\.dccProductCode\)/,
  'Metadata dialog must only require a valid DCC product code when a product is selected'
)

console.log('PASS: DCC optional product binding static contract')
