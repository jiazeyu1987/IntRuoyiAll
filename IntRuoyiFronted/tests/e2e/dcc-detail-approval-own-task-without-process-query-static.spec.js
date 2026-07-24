const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')

assert.match(
  detailPage,
  /const canLoadApprovalDetail = Boolean\(taskId\) && checkPermi\(\['bpm:process-instance:query'\]\)/,
  'DCC detail must gate generic BPM approval detail reads behind bpm:process-instance:query'
)
assert.match(
  detailPage,
  /canLoadApprovalDetail\s*\?\s*ProcessInstanceApi\.getApprovalDetail\(\{ processInstanceId, taskId \}\)\s*:\s*Promise\.resolve\(null\)/,
  'DCC detail must skip the generic approval detail call when the current approver only has task-level permission'
)
assert.match(
  detailPage,
  /approvalTodoTask\.value = detail\?\.todoTask \|\| findCurrentUserTodoTask\(normalizedTaskList\)/,
  'DCC detail must still resolve the current approver task from the task list'
)
assert.match(
  detailPage,
  /TaskApi\.getTaskListByProcessInstanceId\(processInstanceId\)/,
  'DCC detail must keep loading the process task list for stage progress and current task actions'
)
assert.doesNotMatch(
  detailPage,
  /catch\s*\([^)]*\)\s*=>\s*(null|undefined|Promise\.resolve)/,
  'DCC detail must not swallow approval-detail errors; it should avoid unauthorized reads by permission gate'
)

console.log('PASS: DCC detail own task approval static contract')
