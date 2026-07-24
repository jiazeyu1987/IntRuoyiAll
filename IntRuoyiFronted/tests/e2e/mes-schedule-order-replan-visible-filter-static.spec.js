const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.join(__dirname, 'mes-schedule-order-replan-881mo090863-real-flow.e2e.js'),
  'utf8'
)

assert.doesNotMatch(
  source,
  /locator\('input\[placeholder="请输入工单编码"\]'\)\.first\(\)\s*\n\s*await codeInput\.fill/,
  'MES replan real E2E must not fill the first 工单编码 input because the first matching control can be hidden'
)
assert.match(
  source,
  /fillFirstVisible\(page\.locator\('input\[placeholder="请输入排产工单号"\]'\),\s*TARGET_CODE,\s*'schedule order code'\)/,
  'MES replan real E2E must fill the visible 排产工单号 filter for the schedule-order list'
)
assert.ok(
  source.includes(`page.locator('.table-quick-filter[data-table-key="mes.pro.scheduleOrder.main"]').getByRole('button', { name: '查询' }).click()`),
  'MES replan real E2E must click the current quick-filter 查询 button scoped to the schedule-order list'
)
assert.doesNotMatch(
  source,
  /getByRole\('button',\s*\{\s*name:\s*'搜索'\s*\}/,
  'MES replan real E2E must not use the retired 搜索 button label'
)
assert.match(
  source,
  /\.el-select-dropdown__item:visible/,
  'MES replan real E2E must select the visible Element Plus tenant option instead of relying on Enter'
)
