const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const listPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'),
  'utf8'
)
const changeApi = fs.readFileSync(path.join(root, 'src/api/mes/pro/edhr/change.ts'), 'utf8')

assert.match(
  listPage,
  /openVoidDialog\(row\)/,
  '批次执行列表行“作废”必须打开当前行作废确认弹窗，不能跳转到手填或查询页面。'
)

assert.doesNotMatch(
  listPage,
  /openVoidChangePage[\s\S]*router\.push\(\{[\s\S]*edhr-change/,
  '批次执行列表行“作废”不得只跳转变更记录页。'
)

assert.match(
  listPage,
  /requestVoidBatchExecution\(\{[\s\S]*batchExecutionId:\s*selectedVoidBatch\.value\.id/,
  '作废提交必须直接使用当前行 batchExecutionId。'
)

assert.match(listPage, /selectedVoidBatch\.value\.batchExecutionCode/, '确认弹窗必须展示当前行批次执行编码。')
assert.match(listPage, /selectedVoidBatch\.value\.workOrderCode/, '确认弹窗必须展示当前行工单号。')
assert.doesNotMatch(listPage, /v-model="voidForm\.batchExecution/, '作废弹窗不得让用户手填批次执行编号。')
assert.doesNotMatch(listPage, /v-model="voidForm\.workOrder/, '作废弹窗不得让用户手填订单号。')

assert.match(
  changeApi,
  /requestVoidBatchExecution[\s\S]*\/mes\/pro\/edhr-change\/void-batch-execution\/request/,
  '前端 API 必须调用批次执行作废申请专用接口。'
)

console.log('PASS edhr batch execution row void static contract')
