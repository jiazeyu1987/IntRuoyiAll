const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const positionPage = readSource('src/views/dcc/controlled-file/positions/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const positionTable = extractBetween(
  positionPage,
  '<el-table v-loading="loading" :data="filteredPositions"',
  '</el-table>'
)

assert.strictEqual(
  packageJson.scripts['e2e:dcc:position-summary:static'],
  'node tests/e2e/dcc-position-summary-static.spec.js',
  'package.json 必须提供 e2e:dcc:position-summary:static 脚本'
)

assert.ok(
  positionTable.includes('data-testid="dcc-position-summary"'),
  '文控岗位表必须提供稳定的岗位摘要测试标识'
)
assert.ok(positionTable.includes('label="岗位摘要"'), '文控岗位表必须显示岗位摘要列')

for (const removedHeader of ['启用状态', '已分配人数', '创建时间']) {
  assert.ok(
    !positionTable.includes(`label="${removedHeader}"`),
    `文控岗位表不应继续显示独立 ${removedHeader} 表头`
  )
}

for (const token of [
  'row.active',
  'getPositionAssignmentCountLabel(row)',
  'row.createTime',
  'formatDate(row.createTime)'
]) {
  assert.ok(positionTable.includes(token), `岗位摘要必须继续使用真实岗位字段或现有派生逻辑：${token}`)
}

for (const token of ['启用', '人数', '创建']) {
  assert.ok(positionTable.includes(token), `岗位摘要必须展示 ${token}`)
}

for (const behaviorToken of [
  'openCreateDialog',
  'openAssignmentDialog(row)',
  'saveApprovalPositionAssignments',
  'submitCreatePosition',
  'saveAssignments',
  'isUploaderDerivedPosition(row)',
  'isAuthorizedRepresentativePosition(row)',
  'getAssignmentSummary(row)'
]) {
  assert.ok(positionPage.includes(behaviorToken), `文控岗位原有行为必须保留：${behaviorToken}`)
}

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(positionTable),
  '文控岗位摘要不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC position summary static contract')
