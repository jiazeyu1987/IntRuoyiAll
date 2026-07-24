const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const approvalTaskPage = readSource('src/views/dcc/controlled-file/approval-tasks/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const approvalTaskTable = extractBetween(
  approvalTaskPage,
  '<el-table v-loading="loading" :data="list"',
  '</el-table>'
)

assert.strictEqual(
  packageJson.scripts['e2e:dcc:approval-task-summary:static'],
  'node tests/e2e/dcc-approval-task-summary-static.spec.js',
  'package.json 必须提供 e2e:dcc:approval-task-summary:static 脚本'
)

assert.ok(
  approvalTaskTable.includes('data-testid="dcc-approval-task-summary"'),
  '审批任务表必须提供稳定的审批摘要测试标识'
)
assert.ok(approvalTaskTable.includes('label="审批摘要"'), '审批任务表必须显示审批摘要列')

for (const removedHeader of ['版本号', '当前状态', '当前阶段', '同层进度']) {
  assert.ok(
    !approvalTaskTable.includes(`label="${removedHeader}"`),
    `审批任务常用视图不应继续显示独立 ${removedHeader} 表头`
  )
}

for (const token of [
  'row.controlledFile?.versionNo',
  'getDccControlledFileStatusTagType(row.controlledFile?.status)',
  'getDccControlledFileStatusLabel(row.controlledFile?.status)',
  'row.currentStageLabel',
  'row.sameLayerProgressText',
  'row.sameLayerHint'
]) {
  assert.ok(approvalTaskTable.includes(token), `审批摘要必须继续使用真实待办字段：${token}`)
}

for (const token of ['版本', '状态', '阶段', '同层']) {
  assert.ok(approvalTaskTable.includes(token), `审批摘要必须展示 ${token}`)
}

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
  assert.ok(approvalTaskPage.includes(behaviorToken), `审批任务原有行为必须保留：${behaviorToken}`)
}

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(approvalTaskTable),
  '审批任务摘要不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC approval task summary static contract')
