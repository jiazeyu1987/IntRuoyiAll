const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const approvalTaskPage = readSource('src/views/dcc/controlled-file/approval-tasks/index.vue')

assert.match(
  approvalTaskPage,
  /resolveApprovalTaskControlledFileReadErrorMessage/,
  'approval task page must build a contextual controlled-file read error'
)
assert.match(
  approvalTaskPage,
  /DCC 审批任务加载阻断/,
  'approval task page must expose the load blocker as a DCC approval task blocker'
)
assert.match(
  approvalTaskPage,
  /任务「\$\{taskName\}」/,
  'approval task load error must include the BPM task name'
)
assert.match(
  approvalTaskPage,
  /流程 \$\{processInstanceId\}/,
  'approval task load error must include the process instance id'
)
assert.match(
  approvalTaskPage,
  /businessKey=\$\{businessKey\}/,
  'approval task load error must include the businessKey used to read the controlled file'
)
assert.match(
  approvalTaskPage,
  /后端返回：\$\{backendMessage\}/,
  'approval task load error must keep the backend error message'
)
assert.match(
  approvalTaskPage,
  /getControlledFileForApprovalTask\(taskRows\[index\], id\)/,
  'controlled file reads must be tied back to the task row that requested them'
)
assert.match(
  approvalTaskPage,
  /list\.value = \[\]/,
  'approval task page must keep fail-fast empty list behavior when loading is blocked'
)
assert.match(
  approvalTaskPage,
  /total\.value = 0/,
  'approval task page must keep fail-fast total reset behavior when loading is blocked'
)

for (const behaviorToken of [
  'TaskApi.getTaskTodoPage',
  'getProcessInstance',
  'getControlledFile',
  'buildDccTaskCenterRowView',
  'openViewer(row)',
  'openDetail(row)',
  'handleAudit(row)',
  'processInstanceId: row.processInstanceId',
  'taskId: row.id'
]) {
  assert.ok(approvalTaskPage.includes(behaviorToken), `approval task behavior must stay: ${behaviorToken}`)
}

assert.doesNotMatch(
  approvalTaskPage,
  /skipBrokenTask|filterValidTask|ignoreMissingFile|mock|placeholder data|fallback|降级|吞异常|默认成功/i,
  'approval task load blocker context must not skip broken tasks or introduce mock/fallback behavior'
)

console.log('PASS: DCC approval task load error context static contract')
