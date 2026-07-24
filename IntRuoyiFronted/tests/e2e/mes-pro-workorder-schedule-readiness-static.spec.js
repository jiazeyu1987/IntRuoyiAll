const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/workorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

for (const token of [
  '排产准备',
  '准备原因',
  'scheduleReadinessMap',
  'loadScheduleReadiness',
  'MesProScheduleOrderApi.getAdmissionDiff',
  'getScheduleReadinessStatusText',
  'getScheduleReadinessReasonText',
  'openScheduleReadinessAction',
  'READY_TO_ADMIT',
  'ALREADY_ADMITTED',
  'BLOCKED_MISSING_ROUTE',
  'BLOCKED_INVALID_FINITE_CAPACITY',
  '加入排产池',
  '查看排产工单',
  '处理阻断'
]) {
  assert.ok(!source.includes(token), `生产工单页不得展示或处理排产工单内容: ${token}`)
}

assert.ok(!source.includes("from '@/api/mes/pro/scheduleorder'"), '生产工单页不得依赖排产工单 API。')
assert.ok(
  !source.includes(':type="scope.row.temporaryFrozen ?') && !source.includes('sortWorkOrderTreeByFrozen'),
  '生产工单列表不得使用 temporaryFrozen 改变编号颜色或排序。'
)
assert.ok(
  source.includes('DICT_TYPE.MES_PRO_WORK_ORDER_STATUS') && !source.includes('已冻结'),
  '生产工单状态列只展示生产工单状态字典，不展示排产冻结状态。'
)

console.log('PASS: MES work order page is decoupled from schedule order readiness')
