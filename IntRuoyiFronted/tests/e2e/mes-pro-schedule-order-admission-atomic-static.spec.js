const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const apiPath = path.join(root, 'src/api/mes/pro/scheduleorder/index.ts')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  apiSource.includes('/mes/pro/schedule-order/create-from-work-orders'),
  '待同步工单批量入池必须调用事务型批量接口'
)
assert.ok(
  pageSource.includes('MesProScheduleOrderApi.createFromWorkOrders'),
  '待同步工单入池前端必须一次提交全部选择，避免部分成功'
)
assert.ok(
  !pageSource.includes('for (const workOrder of rows)'),
  '待同步工单入池不得在前端循环调用单条创建接口'
)

console.log('PASS: MES schedule order admission atomic frontend contract')
