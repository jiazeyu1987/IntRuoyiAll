const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const apiSource = read('src/api/mes/pro/feedback/index.ts')
const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

assert.match(
  apiSource,
  /export interface FrontlinePqcProductionSubmitCandidateVO[\s\S]*eventId: number[\s\S]*serverSubmitTime: string/,
  'PQC process context must expose formal production-submit candidates.'
)
assert.match(
  apiSource,
  /productionSubmitCandidates\?: FrontlinePqcProductionSubmitCandidateVO\[\]/,
  'Each PQC task process must carry its matching production-submit candidates.'
)
assert.match(
  apiSource,
  /export interface FrontlinePqcInspectionSubmitReqVO[\s\S]*activeOrderId: number[\s\S]*pqcTaskId: number[\s\S]*regulationVersionId: number[\s\S]*qaProcessId: number[\s\S]*actualEmployeeId: number[\s\S]*productionSubmitEventId\?: number[\s\S]*scrapQuantity: number[\s\S]*signaturePassword: string/,
  'Formal PQC submit must send an explicit production event, structured scrap quantity, and one-time signature password.'
)
const submitContract = apiSource.slice(
  apiSource.indexOf('export interface FrontlinePqcInspectionSubmitReqVO'),
  apiSource.indexOf('export interface FrontlinePqcInspectionSubmitRespVO')
)
for (const forbidden of [
  'signatureId: number',
  'signatureEmployeeId: number',
  'signatureSnapshot?: string',
  'inspectionResult: string'
]) {
  assert.ok(!submitContract.includes(forbidden), `PQC submit contract must not trust client field: ${forbidden}`)
}

assert.ok(
  !panelSource.includes("firstRouteQueryNumber(['productionSubmitEventId', 'processPoolEventId'])") &&
    !panelSource.includes("pqcSignatureId.value = firstRouteQueryNumber(['signatureId'])"),
  'PQC formal context must not come from route query parameters.'
)
assert.ok(panelSource.includes('data-pqc-production-submit-select'), 'PQC page must expose production-submit selection.')
assert.ok(panelSource.includes('data-pqc-signature-dialog'), 'PQC submit must open an electronic-signature dialog.')
assert.ok(panelSource.includes('data-pqc-submit-receipt'), 'PQC page must render the formal submit receipt.')
assert.match(
  panelSource,
  /signaturePassword:\s*pqcSignaturePassword\.value/,
  'PQC submit request must use the password entered for this submit.'
)
assert.match(
  panelSource,
  /actualEmployeeId:\s*employee\.userId/,
  'PQC submit request must preserve the explicitly switched actual employee.'
)
assert.match(
  panelSource,
  /scrapQuantity:\s*normalizePqcQuantity\(pqcDraft\.scrapQuantity\)/,
  'PQC submit request must carry structured scrap quantity.'
)
assert.match(
  panelSource,
  /:disabled="payloadLoading \|\| Boolean\(pqcSubmitReceipt\) \|\| pqcSubmitResultUncertain"/,
  'PQC submit must remain clickable for explicit validation and lock only while loading, after a formal submit, or during an uncertain submit state.'
)
const pqcValidateHandler = panelSource.slice(
  panelSource.indexOf('const handleValidate = async () => {'),
  panelSource.indexOf('const closePqcSignatureDialog')
)
assert.match(
  pqcValidateHandler,
  /try \{\n\s+assertPqcFormalSubmissionReady\(\)[\s\S]*catch \(error\) \{\n\s+message\.error\(resolveErrorMessage\(error\)\)\n\s+return/,
  'PQC click validation failures must be surfaced to the operator without escaping the native event handler.'
)
const pqcOrderSelectionHandler = panelSource.slice(
  panelSource.indexOf('const handleSelectActiveOrder = async'),
  panelSource.indexOf('const handleSelectProcess = async')
)
assert.match(
  pqcOrderSelectionHandler,
  /try \{\n\s+processes = await selectFrontlinePqcActiveOrder[\s\S]*catch \(error\) \{[\s\S]*message\.error\(resolveErrorMessage\(error\)\)[\s\S]*\n\s+return/,
  'PQC order initialization failures must remain visible without breaking the mounted page.'
)

console.log('PASS: frontline PQC formal submit static contract')
