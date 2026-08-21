const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

const blockBetween = (startToken, endToken) => {
  const start = panel.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = panel.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return panel.slice(start, end)
}

const mapItemBlock = blockBetween(
  'const mapPqcInspectionItem = (item: FrontlinePqcInspectionItemVO): PqcInspectionItem => ({',
  'const normalizePqcTaskOptionItemKey = (option?: PqcTaskOptionSnapshot) =>'
)
assert.match(
  mapItemBlock,
  /:\s*'合格'/,
  'Choice-based PQC inspection items must default to 合格.'
)

assert.match(
  panel,
  /const ensurePqcDefaultPieceValuesForTask = \(\s*itemKey: PqcInspectionItemKey,\s*taskOption: PqcTaskOptionSnapshot\s*\) =>/,
  'PQC must have a shared helper that materializes default piece values for any inspection-method task.'
)

const defaultValuesBlock = blockBetween(
  'const ensurePqcDefaultPieceValuesForTask = (',
  'const getPqcStoredPieceValuesForTask = ('
)
assert.match(
  defaultValuesBlock,
  /getPqcInspectionQuantityForTask\(taskOption\)[\s\S]*while \([\s\S]*values\.length < quantity\)[\s\S]*values\.push\(item\.defaultValue\)/,
  'Default piece values must be expanded to the task planned inspection quantity.'
)
assert.match(
  defaultValuesBlock,
  /if \(item\.type === 'choice'\)[\s\S]*values\[index\] = item\.defaultValue/,
  'Blank choice values must be normalized back to the default 合格 value.'
)

const exactValuesBlock = blockBetween(
  'const getPqcExactPieceValuesForTask = (',
  'const getPqcExactPieceValuesForSubmit = ('
)
assert.match(
  exactValuesBlock,
  /ensurePqcDefaultPieceValuesForTask\(itemKey, taskOption\)/,
  'Submit-time exact sample validation must materialize defaults before comparing sample count.'
)
assert.doesNotMatch(
  exactValuesBlock,
  /const values = pqcPieceValues\[stateKey\] \|\| \[\]/,
  'Submit-time exact sample validation must not read an uninitialized task as an empty sample array.'
)

assert.doesNotMatch(
  panel,
  /getPqcRelaxedPieceValuesForTask|getPqcRelaxedPieceValuesForSubmit/,
  'PQC formal submit must not keep relaxed sample helpers that can hide empty planned samples.'
)

console.log('frontline-pqc-default-sample-values-static: PASS')
