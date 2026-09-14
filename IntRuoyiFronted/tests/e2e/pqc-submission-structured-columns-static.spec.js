const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const source = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
  .replace(/\r\n/g, '\n')
const frontlinePanel = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

const reportMarker = 'data-team-leader-report-workbench'
const reportStart = source.indexOf(reportMarker)
assert.ok(reportStart >= 0, 'submission workbench must keep a stable report workbench marker.')
const tableStart = source.indexOf('<el-table', reportStart)
const tableEnd = source.indexOf('</el-table>', tableStart)
assert.ok(tableStart > reportStart && tableEnd > tableStart, 'submission table block must be locatable.')
const tableBlock = source.slice(tableStart, tableEnd)

const extractConstArrayBlock = (name) => {
  const marker = `const ${name}`
  const start = source.indexOf(marker)
  assert.ok(start >= 0, `${name} must be declared.`)
  const nextConst = source.indexOf('\nconst ', start + marker.length)
  assert.ok(nextConst > start, `${name} block must have a clear end marker.`)
  return source.slice(start, nextConst)
}

const submissionDefaultColumnsBlock = [
  extractConstArrayBlock('productionSubmissionDefaultColumns'),
  extractConstArrayBlock('pqcSubmissionDefaultColumns')
].join('\n')

for (const label of ['PQC', '提交内容']) {
  assert.doesNotMatch(
    tableBlock,
    new RegExp(`label="${label}"`),
    `submission main table must remove the red-box ${label} column.`
  )
}
assert.match(
  tableBlock,
  /label="生产工单"[\s\S]*data-pqc-leader-work-order/,
  'PQC structured list must keep production work order as an explicit submitted-data column.'
)
for (const removedKey of ['workOrderCode', 'pqcResult', 'submissionContent']) {
  assert.doesNotMatch(
    submissionDefaultColumnsBlock,
    new RegExp(`\\{ key: '${removedKey}'`),
    `submission default columns must not keep removed key ${removedKey}.`
  )
}

for (const key of [
  'completionQuantity',
  'lossQuantity',
  'lossBreakdown',
  'equipmentSnapshot',
  'parameterSnapshot'
]) {
  assert.match(
    source,
    new RegExp(`isSubmissionColumnVisible\\('${key}'\\)`),
    `submission table must render structured column ${key}.`
  )
  assert.match(
    source,
    new RegExp(`\\{ key: '${key}'`),
    `submission default columns must include structured key ${key}.`
  )
}

for (const resolver of [
  'resolveSubmissionCompletionQuantity',
  'resolveSubmissionLossQuantity',
  'resolveSubmissionLossBreakdownItems',
  'resolveSubmissionEquipmentItems',
  'resolveSubmissionParameterItems',
  'isPqcSampleOutOfRange'
]) {
  assert.match(source, new RegExp(`const ${resolver}\\s*=`), `missing structured resolver ${resolver}.`)
}

assert.match(
  tableBlock,
  /data-team-leader-completion-quantity/,
  'completion quantity column must expose a stable marker.'
)
assert.match(tableBlock, /data-team-leader-loss-quantity/, 'loss quantity column must expose a stable marker.')
assert.match(tableBlock, /data-team-leader-loss-breakdown/, 'loss breakdown column must expose a stable marker.')
assert.match(tableBlock, /data-team-leader-equipment-snapshot/, 'equipment column must expose a stable marker.')
assert.match(tableBlock, /data-team-leader-parameter-snapshot/, 'parameter column must expose a stable marker.')
assert.match(
  tableBlock,
  /team-leader-workbench__parameter-value[\s\S]*is-out-of-range/,
  'parameter values must be able to mark out-of-range samples in red without blocking submit.'
)

assert.match(
  source,
  /pqcItemDetails|itemResults/,
  'PQC list must read item-level submitted facts from pqcItemDetails/itemResults.'
)
assert.match(
  source,
  /standardLowerLimit[\s\S]*standardUpperLimit[\s\S]*isPqcSampleOutOfRange/,
  'PQC list must compare samples against frozen lower and upper limits for red warning display.'
)
assert.doesNotMatch(
  source,
  /throw new Error\([^)]*standardLowerLimit|throw new Error\([^)]*standardUpperLimit/,
  'out-of-range parameter display must not add submit-blocking lower/upper limit errors on the leader page.'
)

assert.match(
  frontlinePanel,
  /buildProductionLossDetailsPayload[\s\S]*lossReasonDetails/,
  'production submit raw payload must snapshot per-reason loss quantities for the leader list.'
)
assert.match(
  frontlinePanel,
  /buildProductionEquipmentParameterRulesPayload[\s\S]*equipmentParameterRules/,
  'production submit raw payload must snapshot device parameter lower and upper limits for red warning display.'
)
assert.match(
  frontlinePanel,
  /rawPayload:\s*buildProductionStructuredRawPayload\(rawPayload,\s*formalContext\)/,
  'production submit must persist the structured raw payload snapshot with the formal submit context.'
)
assert.match(
  frontlinePanel,
  /activeOrderProcess:[\s\S]*activeOrderProcessSnapshotId:\s*formalContext\.activeOrderProcessSnapshotId/,
  'production structured raw payload must preserve the active-order process snapshot identity.'
)

console.log('PASS: PQC submission structured columns static contract')
