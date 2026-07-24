const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  pageSource.includes('const previewMaterialRowsByDate = computed'),
  'Preview calendar must derive material rows from local preview data.'
)
assert(
  pageSource.includes('cumulativeRequiredByMaterialId'),
  'Preview material shortage must track cumulative required quantity by material.'
)
assert(
  pageSource.includes('currentShortageQty'),
  'Preview material shortage must expose current shortage through the selected day.'
)
assert(
  pageSource.includes('requiredQty: cumulativeRequiredQty'),
  'Preview shortage rows must expose cumulative requiredQty through the current day.'
)
assert(
  pageSource.includes('availableQty: totalAvailable'),
  'Preview shortage rows must keep availableQty as the current stock snapshot, not the remaining daily quantity.'
)
assert(
  pageSource.includes('Number(item.shortageQty || 0) > 0'),
  'Selected day shortage detail must show only items with positive current shortage.'
)

console.log('PASS: MES schedule calendar preview material shortage uses cumulative current shortage')
