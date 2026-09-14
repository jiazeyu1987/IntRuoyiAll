const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const dialogStart = source.indexOf('<Dialog :title="shortageDialogTitle"')
const dialogEnd = source.indexOf('<Dialog :title="daySummaryDialogTitle"', dialogStart)

assert.notEqual(dialogStart, -1, 'Shortage dialog must exist.')
assert.notEqual(dialogEnd, -1, 'Day summary dialog must follow shortage dialog.')

const dialogSource = source.slice(dialogStart, dialogEnd)

for (const hiddenToken of [
  '<el-tabs',
  'issueDialogActiveTab',
  'label="错误 / 阻塞"',
  'label="警告"',
  'label="级别"',
  'label="工序"',
  'label="工作站"',
  'label="工单"',
  'label="物料"',
  'label="说明"'
]) {
  assert.ok(!dialogSource.includes(hiddenToken), `Shortage dialog must hide ${hiddenToken}.`)
}

for (const keptColumn of [
  'label="物料编码"',
  'label="物料名称"',
  'label="订单总需求"',
  'label="库存数量"',
  'label="缺口"'
]) {
  assert.ok(dialogSource.includes(keptColumn), `Shortage dialog must keep ${keptColumn}.`)
}

for (const keptToken of [
  'issueDialogVisibleRows',
  'buildMaterialCodeLabel',
  'buildMaterialNameLabel',
  'buildQuantityLabel(row.requiredQty)',
  'buildQuantityLabel(row.availableQty)',
  'buildQuantityLabel(row.shortageQty)'
]) {
  assert.ok(dialogSource.includes(keptToken), `Shortage dialog must preserve ${keptToken}.`)
}

assert.ok(
  dialogSource.indexOf('label="订单总需求"') < dialogSource.indexOf('label="库存数量"'),
  'Shortage dialog must show total order demand before actual stock.'
)
assert.ok(
  dialogSource.indexOf('label="库存数量"') < dialogSource.indexOf('label="缺口"'),
  'Shortage dialog must show actual stock before shortage quantity.'
)

assert.ok(
  !dialogSource.includes('shortageQty ?? row.requiredQty'),
  'Shortage quantity must be rendered from shortageQty without requiredQty fallback.'
)

for (const removedHelper of [
  'issueDialogActiveTab',
  'issueDialogErrorRows',
  'issueDialogWarningRows',
  'buildMaterialLabel',
  'mapIssueSeverityType',
  'canOpenIssueWorkOrder',
  'openIssueWorkOrder',
  'buildIssueWorkOrderLabel',
  'canOpenIssueProcess',
  'canOpenIssueWorkstation',
  'openIssueProcess',
  'openIssueWorkstation',
  'buildIssueProcessLabel',
  'buildIssueWorkstationLabel'
]) {
  assert.ok(!source.includes(removedHelper), `Removed shortage columns must not leave unused helper ${removedHelper}.`)
}

assert.doesNotMatch(source, /catch\s*\{\s*\}/, 'Schedule calendar shortage column cleanup must not introduce empty catch blocks.')

console.log('PASS: MES schedule calendar shortage dialog columns static contract')
