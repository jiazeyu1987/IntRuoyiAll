const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const scriptPath = path.resolve(__dirname, 'edhr-full-chain-multi-user-real-flow.e2e.js')
const source = fs.readFileSync(scriptPath, 'utf8')

assert.ok(
  source.includes('sourceRejectedExecutionId'),
  '返工来源必须在修订执行详情中核验 sourceRejectedExecutionId。'
)
assert.ok(
  source.includes('expectedWorkTaskId'),
  '返工待办仍必须校验 workTaskId，避免批次同工序多行时点错任务。'
)
assert.ok(
  source.includes('expectedExecutionId'),
  '返工待办仍必须校验 revisionExecutionId，避免打开非修订草稿。'
)
assert.ok(
  !source.includes('sourceExecutionId'),
  '返工来源不得依赖看板行文案包含原执行数字，应以详情 sourceRejectedExecutionId 为准。'
)

console.log('PASS edhr-rework-source-detail-static')
