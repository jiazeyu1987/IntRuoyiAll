const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

for (const token of [
  ':class="getScheduleWorkOrderCodeClass(row)"',
  ':class="getAdmissionWorkOrderCodeClass(row)"',
  'MesProWorkOrderStatusEnum',
  'SCHEDULE_ORDER_STATUS_FINISHED = 3',
  'getScheduleWorkOrderCodeClass',
  'getAdmissionWorkOrderCodeClass',
  'schedule-order-pool__work-order-code--scheduled',
  'schedule-order-pool__work-order-code--unscheduled',
  'schedule-order-pool__work-order-code--finished'
]) {
  assert.ok(source.includes(token), `排产工单页必须按状态渲染工单编码颜色: ${token}`)
}

const finishedIndex = source.indexOf('row.workOrderStatus === MesProWorkOrderStatusEnum.FINISHED')
const admittedIndex = source.indexOf('isAdmissionRowAdmitted(row)')
const scheduleClassIndex = source.indexOf('const getScheduleWorkOrderCodeClass')
const scheduleClassSource = source.slice(scheduleClassIndex, source.indexOf('const getDiffStatusText'))

assert.ok(
  scheduleClassSource.includes('row.status === SCHEDULE_ORDER_STATUS_FINISHED'),
  '主排产工单列表必须按排产工单状态 3 判断已完成，不能误用生产工单 FINISHED=2'
)
assert.ok(
  !scheduleClassSource.includes('MesProWorkOrderStatusEnum.FINISHED'),
  '主排产工单列表的排产状态不得使用生产工单状态枚举，否则生产中 status=2 会误显示为绿色'
)
assert.ok(finishedIndex >= 0, '入池弹窗工单编码颜色必须显式判断生产工单已完成状态')
assert.ok(admittedIndex >= 0, '入池弹窗工单编码颜色必须显式判断是否已参与排产')
assert.ok(
  finishedIndex < admittedIndex,
  '生产工单已完成必须优先于已参与排产显示绿色'
)

console.log('PASS: MES schedule order work order code color contract')
