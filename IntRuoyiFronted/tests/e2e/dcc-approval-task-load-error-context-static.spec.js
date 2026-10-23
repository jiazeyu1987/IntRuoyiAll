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
const workbenchPage = readSource('src/views/dcc/controlled-file/workbench/index.vue')

assert.match(
  approvalTaskPage,
  /router\.replace\(\{[\s\S]*path:\s*['"]\/approval-center['"][\s\S]*moduleCode:\s*['"]DCC['"][\s\S]*viewType:\s*['"]TODO['"]/,
  'legacy approval task entry must redirect to the unified DCC approval center'
)
assert.match(
  workbenchPage,
  /resolveWorkbenchErrorMessage/,
  'unified DCC workbench must expose a contextual load error'
)
assert.match(
  workbenchPage,
  /DCC 工作台加载失败，请查看接口错误后重试/,
  'workbench load error must preserve a concrete DCC context'
)
assert.match(
  workbenchPage,
  /TaskApi\.getTaskTodoPage/,
  'workbench approval loading must use the BPM TODO source'
)
assert.match(
  workbenchPage,
  /getProcessInstance\(item\.processInstanceId\)/,
  'controlled-file reads must resolve through the task process instance'
)
assert.match(
  workbenchPage,
  /const businessObjectId = String\(processInstance\.businessObjectId \|\| ''\)\.trim\(\)[\s\S]*const businessKey = String\(processInstance\.businessKey \|\| ''\)\.trim\(\)/,
  'controlled-file reads must prefer the process business object id and only then use a numeric business key'
)
assert.match(
  workbenchPage,
  /controlledFileIds\.map\(\(id\) => getControlledFile\(id\)\)/,
  'controlled-file reads must use the resolved process business object identity'
)
assert.match(
  workbenchPage,
  /buildDccTaskCenterRowView/,
  'workbench rows must derive actions from the current task and file state'
)
assert.match(
  workbenchPage,
  /approvalTodoRows\.value = \[\][\s\S]*pendingDistributionRows\.value = \[\][\s\S]*trainingTodoRows\.value = \[\][\s\S]*finalizationFailedRows\.value = \[\]/,
  'workbench must clear visible rows when loading is blocked'
)
assert.match(
  workbenchPage,
  /approvalTodoTotal:\s*0[\s\S]*pendingDistributionTotal:\s*0[\s\S]*trainingTodoTotal:\s*0[\s\S]*finalizationFailedTotal:\s*0/,
  'workbench must reset totals when loading is blocked'
)

assert.doesNotMatch(
  `${approvalTaskPage}\n${workbenchPage}`,
  /skipBrokenTask|filterValidTask|ignoreMissingFile|mock|placeholder data|fallback|降级|吞异常|默认成功/i,
  'approval task load blocker context must not skip broken tasks or introduce mock/fallback behavior'
)

console.log('PASS: DCC approval task load error context static contract')
