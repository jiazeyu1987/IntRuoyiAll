const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), `排产工单页面必须存在：${pagePath}`)

const source = fs.readFileSync(pagePath, 'utf8')

assert(
  source.includes('workOrderCode') &&
    source.includes('productCode') &&
    source.includes('productName') &&
    source.includes('productSpecification') &&
    source.includes('message') &&
    source.includes('schedule-order-pool__admission-table__cell--wrap'),
  '待同步差异表必须为工单编码、产品编号、产品名称、规格型号、不可排原因这些列统一开放换行样式。'
)

assert(
  source.includes('schedule-order-pool__admission-cell-text'),
  '待同步差异表长文本列必须使用统一的换行文本 class。'
)

assert(
  source.includes('schedule-order-pool__admission-table__cell--wrap'),
  '待同步差异表必须提供专门的 cell wrap class。'
)

console.log('PASS: MES schedule order admission wrap static contract')
