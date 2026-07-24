const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleCalendar/index.ts')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')
assert(fs.existsSync(apiPath), 'Schedule calendar API contract must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert.ok(
  apiSource.includes('ProScheduleCalendarMaterialDemandSummaryVO'),
  'Day detail API must expose material demand summary contract.'
)
assert.ok(
  apiSource.includes('materialDemandSummary?: ProScheduleCalendarMaterialDemandSummaryVO'),
  'Day detail response must include optional materialDemandSummary.'
)

const cardStart = source.indexOf('<div class="detail-summary-grid">')
const cardEnd = source.indexOf('</div>', cardStart)
assert.notEqual(cardStart, -1, 'Day summary grid must exist.')
assert.notEqual(cardEnd, -1, 'Day summary grid must be closed.')
const cardSource = source.slice(cardStart, cardEnd)

for (const token of [
  "isDaySummaryCardClickable('materials')",
  "openDaySummaryDetail('materials')",
  'activeSelectedDayMaterialCount',
  '<label>物料</label>'
]) {
  assert.ok(cardSource.includes(token), `Day summary grid must expose material token: ${token}`)
}

const dialogStart = source.indexOf('<Dialog :title="materialDialogTitle"')
const shortageDialogStart = source.indexOf('<Dialog :title="shortageDialogTitle"', dialogStart)
assert.notEqual(dialogStart, -1, 'Material dialog must exist.')
assert.notEqual(shortageDialogStart, -1, 'Material dialog must be placed before shortage dialog.')
const dialogSource = source.slice(dialogStart, shortageDialogStart)

for (const token of [
  '累计工单',
  '物料种类',
  '缺失物料',
  'label="总物料"',
  'label="订单物料"',
  'label="缺失物料"',
  'selectedDayMaterialTotalRows',
  'selectedDayMaterialWorkOrderRows',
  'selectedDayMaterialShortageRows',
  'label="累计需求"',
  'label="订单需求"',
  'label="可用库存"',
  'label="缺失数量"'
]) {
  assert.ok(dialogSource.includes(token), `Material dialog must include ${token}.`)
}

for (const scriptToken of [
  "type MaterialDialogTab = 'total' | 'orders' | 'shortages'",
  "if (type === 'materials')",
  "openMaterialDialog('total')",
  'selectedDayMaterialDemandSummary',
  'activeSelectedDayMaterialCount'
]) {
  assert.ok(source.includes(scriptToken), `Material dialog script must include ${scriptToken}.`)
}

assert.doesNotMatch(source, /catch\s*\{\s*\}/, 'Material dialog work must not introduce empty catch blocks.')
assert.ok(
  !source.includes('label="??"') && !source.includes('????'),
  'Schedule calendar must not retain garbled night-shift action text.'
)

console.log('PASS: MES schedule calendar material dialog static contract')
