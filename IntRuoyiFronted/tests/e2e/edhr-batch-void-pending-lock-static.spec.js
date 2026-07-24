const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const listPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'),
  'utf8'
)

assert.match(
  listPage,
  /const resolveBatchVoidOperationState = \(row: EdhrBatchExecutionRespVO\): BatchVoidOperationState =>[\s\S]*if \(row\.pendingVoidChangeEventId\) \{[\s\S]*row\.canWithdrawVoidRequest === true[\s\S]*'pending-withdrawable'[\s\S]*'pending-readonly'/,
  '待作废锁定应以后端返回的 pendingVoidChangeEventId 与 canWithdrawVoidRequest 为准，不应再由前端猜测状态。'
)

assert.doesNotMatch(
  listPage,
  /pendingVoidChangeStatus\s*===\s*['"]SUBMITTED['"]/,
  '前端不能因为 pendingVoidChangeStatus 文案或枚举漂移而放出正常操作。'
)

assert.match(
  listPage,
  /resolveBatchVoidOperationState\(row\) === 'pending-withdrawable'[\s\S]*撤回作废申请[\s\S]*resolveBatchVoidOperationState\(row\) === 'pending-readonly'[\s\S]*作废申请中[\s\S]*v-else class="edhr-batch-page__actions"/,
  '待作废行必须进入专用操作区，正常操作只能在最终 v-else 分支渲染。'
)

assert.match(
  listPage,
  /const canOpenCurrentUserFillTask = \(row: EdhrBatchExecutionRespVO\) =>\s*\n\s*resolveBatchVoidOperationState\(row\) === 'normal' && Boolean\(resolveCurrentUserFillTask\(row\)\)/,
  '待作废行不能继续放出去填写入口。'
)

console.log('PASS edhr batch pending void lock static contract')
