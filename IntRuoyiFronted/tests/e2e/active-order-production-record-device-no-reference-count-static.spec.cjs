const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)

const deviceTableStart = component.indexOf('data-active-order-production-record-device-table')
const deviceParameterTableStart = component.indexOf(
  'data-active-order-production-record-device-parameter-table'
)
assert.ok(deviceTableStart >= 0, '生产记录表单必须保留设备表。')
assert.ok(deviceParameterTableStart > deviceTableStart, '设备参数明细表必须位于设备表之后。')

const deviceTableBlock = component.slice(deviceTableStart, deviceParameterTableStart)

assert.doesNotMatch(
  deviceTableBlock,
  /参数数量|参考值|row\.parameters\.length/,
  '生产记录表单的设备表不应显示参数数量/参考值类汇总。'
)
assert.match(deviceTableBlock, /label="设备名称"/, '设备表必须保留设备名称列。')
assert.match(deviceTableBlock, /label="设备编号"/, '设备表必须保留设备编号列。')
assert.match(deviceTableBlock, /label="计量状态"/, '设备表必须保留计量状态列。')

const parameterTableBlock = component.slice(deviceParameterTableStart)
assert.match(
  parameterTableBlock,
  /label="参数名称"[\s\S]*label="参数范围"[\s\S]*label="提交值"/,
  '设备参数明细表必须继续展示参数名称、参数范围和提交值。'
)

console.log('PASS: active order production record device table no reference count static contract')
