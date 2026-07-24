const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const batchApi = readSource('src/api/mes/pro/edhr/batchExecution.ts')
const batchDetailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

for (const requiredSnippet of [
  'EdhrBatchExecutionReexecuteReqVO',
  'sourceRejectedBatchExecutionId: number',
  'attemptNo?: number',
  'supersededByBatchExecutionId?: number',
  'reexecutedByChangeEventId?: number',
  'reexecuteRejectedEdhrBatchExecution',
  '/reexecute-rejected-batch'
]) {
  assert.match(batchApi, new RegExp(requiredSnippet.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `批次 API 必须包含 ${requiredSnippet}。`)
}

for (const requiredSnippet of [
  'isQualityTerminalStage',
  'qualityTerminalReleaseActionItems',
  'canReexecuteRejectedBatch',
  'reexecuteDialogVisible',
  'reexecuteForm.reason',
  'submitReexecuteRejectedBatch',
  'reexecuteRejectedEdhrBatchExecution',
  '申请重开原记录',
  '重新执行同批号',
  '误拒收',
  '真拒收重做',
  '同批号重做原因不能为空',
  '来源拒收批次',
  '重做新批次'
]) {
  assert.match(batchDetailPage, new RegExp(requiredSnippet.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `批次详情页必须包含 ${requiredSnippet}。`)
}

assert.match(
  batchDetailPage,
  /const\s+qualityTerminalReleaseActionItems[\s\S]*?reopen-original-rejected-batch[\s\S]*?申请重开原记录[\s\S]*?reexecute-rejected-same-batch[\s\S]*?重新执行同批号/,
  '质量终态动作区必须同时提供误拒收重开原记录和真拒收同批号重做入口。'
)
assert.match(
  batchDetailPage,
  /if \(stageKey === 'quality-terminal'\) \{[\s\S]*?return qualityTerminalReleaseActionItems\(\)/,
  '质量已拒收阶段必须使用专属动作列表，不能复用普通关闭阶段动作。'
)
assert.match(
  batchDetailPage,
  /router\.replace\([\s\S]*?id:\s*String\(reexecuted\.id\)/,
  '创建同批号新执行尝试后必须跳转到新批次执行，避免继续停留在原拒收记录。'
)
assert.doesNotMatch(batchDetailPage, /activeContextKey\s*=\s*null|defaultSuccess|mock|fixture|demo/i, '前端不得通过清空上下文、mock 或默认成功掩盖拒收后重做。')

console.log('PASS: eDHR quality terminal reopen/reexecute static contract')
