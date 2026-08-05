const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const blockBetween = (startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.match(
  source,
  /const isPqcInspectionQuantityLocked = computed\(\(\) =>[\s\S]*isPqcMode\.value[\s\S]*hasPqcTaskSnapshot\(deviceState\.selectedProcess\)/,
  'PQC task snapshot must lock inspection quantity to the planned task quantity.'
)

const quantityFieldBlock = blockBetween(
  '<label for="frontlinePqcInspectionQuantity">检验数量</label>',
  '<label for="frontlinePqcScrapQuantity">损耗数量</label>'
)
assert.ok(
  (quantityFieldBlock.match(/:disabled="isPqcInspectionQuantityLocked"/g) || []).length >= 3,
  'PQC inspection quantity decrease button, input, and increase button must all be disabled when task quantity is locked.'
)

const updateQuantityBlock = blockBetween(
  'const updatePqcQuantity = (field: PqcQuantityField, event: Event) => {',
  'const updatePqcSignatureId = (event: Event) => {'
)
assert.match(
  updateQuantityBlock,
  /if \(field === 'inspectionQuantity' && isPqcInspectionQuantityLocked\.value\)[\s\S]*return/,
  'direct PQC inspection quantity input changes must be ignored while task quantity is locked.'
)

const adjustQuantityBlock = blockBetween(
  'const adjustPqcQuantity = (field: PqcQuantityField, delta: number) => {',
  'const handleResetPqc = () => {'
)
assert.match(
  adjustQuantityBlock,
  /if \(field === 'inspectionQuantity' && isPqcInspectionQuantityLocked\.value\)[\s\S]*return/,
  'PQC inspection quantity +/- controls must not mutate planned task quantity.'
)

assert.match(
  source,
  /const getPqcExactPieceValuesForSubmit = \(itemKey: PqcInspectionItemKey\) =>[\s\S]*values\.length !== pqcInspectionQuantity\.value[\s\S]*throw new Error\(/,
  'PQC submit must fail fast when stored piece value count differs from planned task quantity.'
)
assert.match(
  source,
  /const assertPqcSubmissionSampleQuantities = \(\) =>[\s\S]*getPqcExactPieceValuesForSubmit\(itemKey\)/,
  'PQC submit must validate every inspection item before calling backend submit.'
)

const handleValidateBlock = blockBetween('const handleValidate = async () => {', 'const assertFormalPayloadContext = () => {')
assert.match(
  handleValidateBlock,
  /if \(isPqcMode\.value\) \{[\s\S]*assertPqcSubmissionSampleQuantities\(\)[\s\S]*\}[\s\S]*Object\.assign\(/,
  'PQC sample quantity validation must run before template validation and formal submit.'
)

for (const [name, start, end] of [
  ['item results payload', 'const buildPqcItemResultsPayload = () =>', 'const buildPqcItemDetailsPayload = () =>'],
  ['item details payload', 'const buildPqcItemDetailsPayload = () =>', 'const getPqcCurrentChoiceValues ='],
  ['raw piece values payload', 'const buildPqcPieceValuesPayload = () =>', 'const buildPqcInspectionSubmitPayload =']
]) {
  const block = blockBetween(start, end)
  assert.match(
    block,
    /getPqcExactPieceValuesForSubmit\(item\.key\)|getPqcExactPieceValuesForSubmit\(itemKey\)/,
    `PQC ${name} must use exact piece values for submit.`
  )
  assert.doesNotMatch(
    block,
    /\.slice\(0,\s*pqcInspectionQuantity\.value\)/,
    `PQC ${name} must not silently truncate sample values.`
  )
}

console.log('PASS: mes frontline PQC task quantity static contract')
