const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const apiPath = path.join(root, 'src/api/mes/pro/scheduleorder/index.ts')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert.ok(
  pageSource.includes('const isAdmissionRowSelectable = (row: MesProScheduleOrderAdmissionDiffRowVO) =>') &&
    pageSource.includes("row.admissionStatus === 'READY_TO_ADMIT'"),
  '同步工单弹窗只能勾选 READY_TO_ADMIT 且 selectable=true 的生产工单'
)
assert.ok(
  pageSource.includes('message.warning(\'请先选择需要加入排产工单池的生产工单\')'),
  '同步工单批量入池必须在未选择生产工单时阻止提交'
)
assert.ok(
  pageSource.includes('MesProScheduleOrderApi.createFromWorkOrders({') &&
    pageSource.includes('workOrderIds: rows.map((workOrder) => workOrder.workOrderId)') &&
    !pageSource.includes('promiseDate: workOrderAdmissionPromiseDate.value'),
  '同步工单批量入池必须只提交选中工单 ID，不再要求统一承诺交期'
)
assert.ok(
  pageSource.includes("message.success(`已将 ${rows.length} 个生产工单加入排产工单池`)"),
  '同步工单批量入池成功提示必须包含已加入数量'
)
assert.ok(
  pageSource.includes('await getWorkOrderAdmissionList()') && pageSource.includes('await getScheduleOrderList()'),
  '同步工单批量入池成功后必须刷新待同步列表和排产工单主列表'
)
assert.ok(
  apiSource.includes("url: '/mes/pro/schedule-order/create-from-work-orders'"),
  '前端必须调用后端事务型批量创建排产工单接口'
)

console.log('PASS: MES schedule order batch admission static contract')
