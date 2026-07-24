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
  /type BatchVoidOperationState =[\s\S]*\| 'normal'[\s\S]*\| 'pending-withdrawable'[\s\S]*\| 'pending-readonly'[\s\S]*\| 'voided'[\s\S]*\| 'release-locked'/,
  '批次执行列表需要显式声明作废/放行锁状态机，而不是散落多个按钮判断。'
)

assert.match(
  listPage,
  /const resolveBatchVoidOperationState = \(row: EdhrBatchExecutionRespVO\): BatchVoidOperationState =>[\s\S]*isVoidedBatchExecutionStatus\(row\.status\)[\s\S]*'voided'[\s\S]*row\.pendingVoidChangeEventId[\s\S]*row\.canWithdrawVoidRequest === true[\s\S]*'pending-withdrawable'[\s\S]*'pending-readonly'[\s\S]*row\.releaseActionLocked === true[\s\S]*'release-locked'[\s\S]*'normal'/,
  '操作列状态机必须按 voided -> pending 可撤回 -> pending 只读 -> release locked -> normal 的顺序解析。'
)

assert.match(
  listPage,
  /resolveBatchVoidOperationState\(row\) === 'pending-withdrawable'[\s\S]*撤回作废申请[\s\S]*resolveBatchVoidOperationState\(row\) === 'pending-readonly'[\s\S]*作废申请中[\s\S]*resolveBatchVoidOperationState\(row\) === 'voided'[\s\S]*编辑[\s\S]*resolveBatchVoidOperationState\(row\) === 'release-locked'[\s\S]*<el-button link type="primary" @click="openDetail\(row\)">编辑<\/el-button>[\s\S]*v-else class="edhr-batch-page__actions"/,
  '操作列必须为待处理、非申请人只读、已作废终态、放行锁和正常状态提供互斥分支。'
)

const releaseLockedBranch = listPage.match(
  /resolveBatchVoidOperationState\(row\) === 'release-locked'[\s\S]*?<div v-else class="edhr-batch-page__actions">/
)?.[0]

assert.ok(releaseLockedBranch, '操作列必须保留放行锁 release-locked 分支。')
assert(
  !releaseLockedBranch.includes('releaseActionLockReason') && !releaseLockedBranch.includes('放行审批中'),
  '放行锁分支不应在操作列显示放行锁定说明。'
)

assert.match(
  listPage,
  /const isPendingVoidBatch = \(row: EdhrBatchExecutionRespVO\) =>\s*\n\s*resolveBatchVoidOperationState\(row\)\.startsWith\('pending-'\)/,
  '待作废判断必须派生自统一状态机。'
)

assert.doesNotMatch(listPage, /canOpenCurrentUserFillTask|openCurrentUserFillTask/, '操作列不得继续提供去填写入口。')

console.log('PASS edhr batch void state machine static contract')
