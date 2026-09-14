const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

function indexOfRequired(source, needle) {
  const index = source.indexOf(needle)
  assert.ok(index >= 0, `${needle} must exist.`)
  return index
}

function extractColumn(source, prop) {
  const propNeedle = `prop="${prop}"`
  const propIndex = indexOfRequired(source, propNeedle)
  const start = source.lastIndexOf('<el-table-column', propIndex)
  assert.ok(start >= 0, `${prop} column must start with el-table-column.`)
  const end = source.indexOf('</el-table-column>', propIndex)
  assert.ok(end > propIndex, `${prop} column must close.`)
  return source.slice(start, end + '</el-table-column>'.length)
}

function extractArray(source, name) {
  const start = indexOfRequired(source, `const ${name}: UserTableColumnDefinition[] = [`)
  const end = source.indexOf(']\n', start)
  assert.ok(end > start, `${name} array must close.`)
  return source.slice(start, end)
}

const processColumnIndex = indexOfRequired(page, 'prop="process"')
const workplaceColumnIndex = indexOfRequired(page, 'prop="clearanceWorkplace"')
const materialColumnIndex = indexOfRequired(page, 'prop="clearanceMaterial"')
const cleaningColumnIndex = indexOfRequired(page, 'prop="clearanceCleaning"')
const workOrderColumnIndex = indexOfRequired(page, 'prop="workOrder"')

assert.ok(
  processColumnIndex < workplaceColumnIndex &&
    workplaceColumnIndex < materialColumnIndex &&
    materialColumnIndex < cleaningColumnIndex &&
    cleaningColumnIndex < workOrderColumnIndex,
  'report table columns must be ordered as 工序、清场、物料、清洁、生产工单.'
)

for (const [prop, label, key] of [
  ['clearanceWorkplace', '清场', 'workplace'],
  ['clearanceMaterial', '物料', 'material'],
  ['clearanceCleaning', '清洁', 'cleaning']
]) {
  const column = extractColumn(page, prop)
  assert.match(column, new RegExp(`label="${label}"`), `${label} column label must be stable.`)
  assert.match(
    column,
    new RegExp(`isSubmissionColumnVisible\\('${prop}'\\)`),
    `${label} column must honor table column visibility.`
  )
  assert.match(
    column,
    new RegExp(`resolveProductionClearanceConfirmationText\\(row, '${key}'\\)`),
    `${label} column must render from the submitted clearance snapshot key.`
  )
}

const defaultColumns = extractArray(page, 'productionSubmissionDefaultColumns')
const defaultOrder = [
  "'process'",
  "'clearanceWorkplace'",
  "'clearanceMaterial'",
  "'clearanceCleaning'",
  "'workOrder'"
].map((needle) => indexOfRequired(defaultColumns, `key: ${needle}`))
assert.deepEqual(
  [...defaultOrder].sort((a, b) => a - b),
  defaultOrder,
  'production default columns must keep clearance columns between process and workOrder.'
)

assert.match(
  page,
  /type ProductionClearanceConfirmationColumnKey = 'workplace' \| 'material' \| 'cleaning'/,
  'clearance column keys must match the three frontline production confirmations.'
)
assert.match(
  page,
  /const resolveProductionClearanceConfirmationText = \([\s\S]*rootPayload\?\.clearanceConfirmations[\s\S]*item\.key === key[\s\S]*confirmation\.confirmed === true[\s\S]*'已选'[\s\S]*confirmation\.confirmed === false[\s\S]*'未选'[\s\S]*'--'/,
  'clearance columns must parse originalPayloadJson clearanceConfirmations and avoid defaulting missing snapshots.'
)

console.log('PASS: team leader production report shows frontline clearance confirmation columns')
