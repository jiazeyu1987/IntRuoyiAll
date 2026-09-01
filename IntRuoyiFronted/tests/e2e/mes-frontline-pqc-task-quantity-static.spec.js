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
  /const createPqcTaskDraftState = \(taskOption: PqcTaskOptionSnapshot\): PqcTaskDraftState => \(\{[\s\S]*inspectionQuantity: taskOption\.plannedInspectionQuantity/,
  'PQC task snapshot must provide the planned quantity as the initial suggested value.'
)

const quantityFieldBlock = blockBetween(
  '<label for="frontlinePqcInspectionQuantity">检验</label>',
  '<label for="frontlinePqcScrapQuantity">损耗</label>'
)
assert.doesNotMatch(
  quantityFieldBlock,
  /:disabled="isPqcInspectionQuantityLocked"/,
  'PQC inspection quantity controls must remain editable after a task is selected.'
)

assert.match(
  source,
  /const resizePqcPieceValuesForCurrentTask = \(taskOption: PqcTaskOptionSnapshot\) =>[\s\S]*slice\(0, quantity\)/,
  'PQC piece values must be resized when the operator adjusts actual inspection quantity.'
)

const updateQuantityBlock = blockBetween(
  'const updatePqcQuantity = (field: PqcQuantityField, event: Event) => {',
  'const adjustPqcQuantity = (field: PqcQuantityField, delta: number) => {'
)
assert.doesNotMatch(
  updateQuantityBlock,
  /isPqcInspectionQuantityLocked/,
  'direct PQC inspection quantity input must persist the operator adjustment.'
)

const adjustQuantityBlock = blockBetween(
  'const adjustPqcQuantity = (field: PqcQuantityField, delta: number) => {',
  'const handleResetPqc = () => {'
)
assert.doesNotMatch(
  adjustQuantityBlock,
  /isPqcInspectionQuantityLocked/,
  'PQC inspection quantity +/- controls must persist the operator adjustment.'
)

assert.match(
  source,
  /const getPqcExactPieceValuesForTask = \(\s*itemKey: PqcInspectionItemKey,\s*taskOption: PqcTaskOptionSnapshot \| undefined[\s\S]*const quantity = getPqcInspectionQuantityForTask\(taskOption\)[\s\S]*values\.length !== quantity[\s\S]*throw new Error\(/,
  'PQC submit must fail fast when stored piece value count differs from actual inspection quantity.'
)
assert.match(
  source,
  /const assertPqcSubmissionSampleQuantities = \(\) =>[\s\S]*getPqcExactPieceValuesForSubmit\(itemKey\)/,
  'PQC submit must validate every inspection item before calling backend submit.'
)

const handleValidateBlock = blockBetween('const handleValidate = async () => {', 'const assertFormalPayloadContext = () => {')
assert.match(
  handleValidateBlock,
  /assertPqcFormalSubmissionReady\(\)[\s\S]*assertPqcSignatureAndQuantityReady\(\)[\s\S]*assertPqcCurrentProcessAllMethodSubmissionReady\(\)[\s\S]*assertPqcInspectionDisplayFieldsReady\(\)[\s\S]*Object\.assign\(/,
  'PQC sample quantity validation must run before template validation and formal submit.'
)

for (const [name, start, end, expectedPattern] of [
  [
    'item results payload',
    'const buildPqcItemResultsPayload = (',
    'const buildPqcItemDetailsPayload = (',
    /getPqcExactPieceValuesForTask\(item\.key, taskOption\)/
  ],
  [
    'item details payload',
    'const buildPqcItemDetailsPayload = (',
    'const assertPqcCurrentProcessAllMethodSubmissionReady = () => {',
    /getPqcExactPieceValuesForTask\(item\.key, taskOption\)/
  ],
  [
    'task raw piece values payload',
    'const buildPqcPieceValuesPayloadForTask = (taskOption: PqcTaskOptionSnapshot) => {',
    'const buildPqcPieceValuesPayload = () =>',
    /getPqcExactPieceValuesForTask\(item\.key, taskOption\)/
  ],
  [
    'active raw piece values payload',
    'const buildPqcPieceValuesPayload = () =>',
    'const buildPqcInspectionSubmitPayloadForTask = (',
    /getPqcExactPieceValuesForSubmit\(item\.key\)/
  ]
]) {
  const block = blockBetween(start, end)
  assert.match(
    block,
    expectedPattern,
    `PQC ${name} must use exact piece values for submit.`
  )
  assert.doesNotMatch(
    block,
    /\.slice\(0,\s*pqcInspectionQuantity\.value\)/,
    `PQC ${name} must not silently truncate sample values.`
  )
}

console.log('PASS: mes frontline PQC task quantity static contract')
