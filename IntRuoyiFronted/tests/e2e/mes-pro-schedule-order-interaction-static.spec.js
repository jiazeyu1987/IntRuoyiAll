const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert(fs.existsSync(pagePath), '排产工单页面必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')

const operationWidthMatch = pageSource.match(/<el-table-column label="操作" width="(\d+)"[\s\S]*?fixed="right"/)
assert.ok(operationWidthMatch, '排产工单操作列必须固定在右侧。')
assert(
  Number(operationWidthMatch[1]) <= 240,
  `排产工单操作列应收敛到 240px 以内，当前为 ${operationWidthMatch[1]}px。`
)
assert(pageSource.includes('schedule-order-pool__row-actions'), '行操作必须使用紧凑容器。')

for (const token of [
  'openProcessDialog(row)',
  'openPriorityDialog(row)',
  'openPromiseDateDialog(row)',
  'openFreezeDialog(row)',
  'openManualFinishDialog(row)'
]) {
  assert(pageSource.includes(token), `操作列必须保留原有行操作：${token}`)
}

const operationColumnStart = pageSource.indexOf('<el-table-column label="操作"')
const operationColumnEnd = pageSource.indexOf('</el-table-column>', operationColumnStart)
assert(operationColumnStart >= 0 && operationColumnEnd > operationColumnStart, '操作列必须存在。')
const operationColumn = pageSource.slice(operationColumnStart, operationColumnEnd)
assert(
  !/openProcessDialog\(row\)">工序快照[\s\S]*?handleSyncProgress\(row\)">同步进度/.test(
    operationColumn
  ),
  '工序快照和同步进度不得继续作为并列直达按钮挤在操作列。'
)

console.log('PASS: MES schedule order interaction static contract')
