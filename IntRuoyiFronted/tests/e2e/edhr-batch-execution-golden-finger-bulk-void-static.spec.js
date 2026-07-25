const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const listPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'),
  'utf8'
)
const batchApi = fs.readFileSync(path.join(root, 'src/api/mes/pro/edhr/batchExecution.ts'), 'utf8')
const changeApi = fs.readFileSync(path.join(root, 'src/api/mes/pro/edhr/change.ts'), 'utf8')

assert.match(
  listPage,
  /v-if="hasGoldenFingerPermission"[\s\S]*金手指一键作废/,
  '金手指一键作废按钮必须仅在 hasGoldenFingerPermission 为真时显示。'
)

assert.match(
  listPage,
  /当前筛选条件[\s\S]*跨页[\s\S]*可作废批次/,
  '金手指批量作废弹窗必须明确提示按当前筛选条件跨页作废可作废批次。'
)

assert.match(
  listPage,
  /goldenFingerBulkVoidEdhrBatchExecutions\(\{[\s\S]*filter:\s*buildGoldenFingerBulkVoidFilter\(\)/,
  '金手指批量作废提交必须发送当前筛选条件，而不是当前页勾选结果。'
)

assert.doesNotMatch(
  listPage,
  /submitGoldenFingerBulkVoid[\s\S]*resolveVoidBatchExecutionApproval/,
  '金手指批量作废不得调用单条作废审批解析接口。'
)

assert.match(
  batchApi,
  /export interface EdhrBatchExecutionGoldenFingerBulkVoidReqVO[\s\S]*filter:\s*EdhrBatchExecutionPageReqVO/,
  '批次执行 API 必须声明金手指批量作废请求，并包含当前筛选条件。'
)

assert.match(
  batchApi,
  /goldenFingerBulkVoidEdhrBatchExecutions[\s\S]*\/golden-finger\/bulk-void/,
  '批次执行 API 必须调用金手指批量作废专用接口。'
)

assert.doesNotMatch(
  changeApi,
  /goldenFingerBulkVoid/,
  '金手指批量作废不能挂在正式作废审批 change API 中。'
)

console.log('PASS edhr batch execution golden finger bulk void static contract')
