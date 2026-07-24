const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const approvalTaskPage = readSource('src/views/dcc/controlled-file/approval-tasks/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:approval-task-view-mode:static'],
  'node tests/e2e/dcc-approval-task-view-mode-static.spec.js',
  'package.json must expose the DCC approval task view mode static contract'
)

assert.match(
  approvalTaskPage,
  /data-testid="dcc-approval-task-view-mode"/,
  'approval task page must render a stable common/advanced view switch'
)
assert.match(approvalTaskPage, /常用视图/, 'approval task page must show the common view label')
assert.match(approvalTaskPage, /高级视图/, 'approval task page must show the advanced view label')
assert.match(
  approvalTaskPage,
  /const approvalTaskViewMode = ref<ApprovalTaskViewMode>\('common'\)/,
  'approval task page must default to common view'
)
assert.match(
  approvalTaskPage,
  /approvalTaskViewModeOptions/,
  'approval task page must declare view mode options'
)
assert.match(
  approvalTaskPage,
  /isAdvancedApprovalTaskView/,
  'approval task page must expose advanced view state'
)

const commonColumnLabels = [
  '文件标题',
  '文件编号',
  '文件类别',
  '审批摘要',
  '处理提示',
  '流程发起人',
  '操作'
]
for (const label of commonColumnLabels) {
  assert.match(
    approvalTaskPage,
    new RegExp(`label="${label}"`),
    `common view must keep ${label}`
  )
}

const advancedColumnLabels = ['DCC 审批任务', '任务时间', '流程编号']
for (const label of advancedColumnLabels) {
  const columnPattern = new RegExp(
    `<el-table-column\\s+v-if="isAdvancedApprovalTaskView"\\s+label="${label}"`,
    'm'
  )
  assert.match(approvalTaskPage, columnPattern, `${label} column must only render in advanced view`)
}

assert.match(
  approvalTaskPage,
  /query:\s*\{\s*processInstanceId:\s*row\.processInstanceId\s*\}/,
  'detail navigation must keep processInstanceId for traceability'
)
assert.match(
  approvalTaskPage,
  /taskId:\s*row\.id,\s*processInstanceId:\s*row\.processInstanceId/s,
  'audit navigation must keep task id and processInstanceId'
)

const viewModeTemplateMatch = approvalTaskPage.match(
  /<div class="approval-task-view-toolbar"[\s\S]*?data-testid="dcc-approval-task-view-mode"[\s\S]*?<\/div>\s*<el-table/
)
assert.ok(viewModeTemplateMatch, 'view mode toolbar must stay directly above the approval task table')
const viewModeLogicMatch = approvalTaskPage.match(
  /type ApprovalTaskViewMode = 'common' \| 'advanced'[\s\S]*?const isAdvancedApprovalTaskView/
)
assert.ok(viewModeLogicMatch, 'view mode state and options must stay together')
const viewModeContractSource = `${viewModeTemplateMatch[0]}\n${viewModeLogicMatch[0]}`
assert.doesNotMatch(
  viewModeContractSource,
  /截止|超期|\bSLA\b|deadline|overdue|mock|placeholder|fallback|降级|吞异常/i,
  'approval task view mode must not invent deadline/SLA data or introduce mock/fallback behavior'
)

console.log('PASS: DCC approval task view mode static contract')
