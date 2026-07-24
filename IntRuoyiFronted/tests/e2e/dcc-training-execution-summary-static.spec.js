const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const executionTab = readSource(
  'src/views/dcc/controlled-file/training/components/TrainingExecutionTab.vue'
)

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const executionTable = extractBetween(
  executionTab,
  '<el-table v-loading="loading" :data="list"',
  '</el-table>'
)

assert.strictEqual(
  packageJson.scripts['e2e:dcc:training-execution-summary:static'],
  'node tests/e2e/dcc-training-execution-summary-static.spec.js',
  'package.json 必须提供 e2e:dcc:training-execution-summary:static 脚本'
)

assert.ok(
  executionTable.includes('data-testid="dcc-training-execution-summary"'),
  '培训执行表必须提供稳定的培训摘要测试标识'
)
assert.ok(executionTable.includes('label="培训摘要"'), '培训执行表必须显示培训摘要列')

for (const removedHeader of ['版本', '累计时长', '状态', '确认完成时间']) {
  assert.ok(
    !executionTable.includes(`label="${removedHeader}"`),
    `培训执行表不应继续显示独立 ${removedHeader} 表头`
  )
}

for (const token of [
  'row.versionNo',
  'row.accumulatedViewSeconds',
  'row.requiredViewSeconds',
  'row.status',
  'formatTrainingProgressText(row.accumulatedViewSeconds, row.requiredViewSeconds)',
  'getTrainingProgressStatusLabel(row.status)',
  'getTrainingExecutionAcknowledgedAt(row)'
]) {
  assert.ok(executionTable.includes(token), `培训摘要必须继续使用真实培训执行字段：${token}`)
}
assert.ok(
  executionTab.includes('row.acknowledgedAt'),
  '培训摘要确认时间 helper 必须继续使用真实 acknowledgedAt 字段'
)

for (const token of ['版本', '时长', '状态', '确认']) {
  assert.ok(executionTable.includes(token), `培训摘要必须展示 ${token}`)
}

for (const behaviorToken of [
  'getTrainingExecutionPage(queryParams)',
  'getFileCategoryList()',
  'getSimpleDeptList()',
  'getSimpleUserList()',
  'handleQuery',
  'resetQuery',
  'openDetail(row.controlledFileId)',
  "openControlledFileViewer(router, route, id, 'training-execution')"
]) {
  assert.ok(executionTab.includes(behaviorToken), `培训执行原有行为必须保留：${behaviorToken}`)
}
assert.ok(
  !executionTab.includes("name: 'DccControlledFileDetail'"),
  '培训执行文件入口不得继续跳普通文件详情页'
)

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(executionTable),
  '培训执行摘要不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC training execution summary static contract')
