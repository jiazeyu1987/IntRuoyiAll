const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const approvalTaskPage = readSource('src/views/dcc/controlled-file/approval-tasks/index.vue')
const approvalCenterPage = readSource('src/views/approval-center/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const approvalCenterTable = extractBetween(
  approvalCenterPage,
  '<el-table\n              v-loading="loading"',
  '</el-table>'
)

assert.strictEqual(
  packageJson.scripts['e2e:dcc:approval-task-summary:static'],
  'node tests/e2e/dcc-approval-task-summary-static.spec.js',
  'package.json 必须提供 e2e:dcc:approval-task-summary:static 脚本'
)

for (const redirectToken of [
  "path: '/approval-center'",
  "moduleCode: 'DCC'",
  "viewType: 'TODO'"
]) {
  assert.ok(approvalTaskPage.includes(redirectToken), `DCC 待办入口必须定向到统一审批中心：${redirectToken}`)
}

for (const summaryToken of [
  'label="业务摘要"',
  'row.moduleCode === \'DCC\'',
  'data-testid="approval-center-dcc-key-fields"',
  'resolveDccKeyFields(row)',
  'data-testid="approval-center-dcc-business-context"',
  'row.businessContextTags',
  'resolveBusinessContextTagLabel(tag)'
]) {
  assert.ok(approvalCenterTable.includes(summaryToken), `统一审批中心必须保留 DCC 审批摘要：${summaryToken}`)
}

for (const actionToken of [
  'canReviewAction(row)',
  'openReviewAction(row)',
  'openModuleDetail(row)'
]) {
  assert.ok(approvalCenterTable.includes(actionToken), `统一审批中心必须保留真实审批动作：${actionToken}`)
}

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(approvalCenterTable),
  '审批任务摘要不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC approval task summary static contract')
