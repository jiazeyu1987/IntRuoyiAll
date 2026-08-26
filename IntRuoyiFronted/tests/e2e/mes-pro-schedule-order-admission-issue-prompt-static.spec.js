const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(__dirname, '../../src/views/mes/pro/scheduleorder/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  !pageSource.includes(':selectable="isAdmissionRowSelectable"'),
  '同步工单选择列不得按入池资格禁用行'
)
assert.ok(
  pageSource.includes('const rows = selectedWorkOrders.value') &&
    !pageSource.includes('selectedWorkOrders.value.filter(isAdmissionRowSelectable)'),
  '批量入池必须提交全部已选工单，不得在提交前过滤问题工单'
)
assert.ok(
  pageSource.includes('ElMessageBox.alert') &&
    pageSource.includes('工单无法加入排产工单池'),
  '批量入池失败必须用弹窗明确提示问题工单'
)

console.log('PASS: MES schedule order admission issue prompt frontend contract')
