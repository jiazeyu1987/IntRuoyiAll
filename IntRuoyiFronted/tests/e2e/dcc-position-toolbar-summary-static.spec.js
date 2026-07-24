const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const positionPage = readSource('src/views/dcc/controlled-file/positions/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const toolbar = extractBetween(positionPage, '<el-form', '</el-form>')

assert.match(
  toolbar,
  /data-testid="dcc-position-toolbar-summary"/,
  '文控岗位工具栏必须提供稳定的统计上下文测试标识'
)

assert.match(
  toolbar,
  /positionToolbarSummaryText/,
  '文控岗位工具栏必须展示由当前列表数量派生的统计文案'
)

assert.match(
  toolbar,
  /positionToolbarFilterText/,
  '文控岗位工具栏必须展示已应用的筛选上下文'
)

assert.match(
  positionPage,
  /const visiblePositions = computed/,
  '文控岗位必须先派生可见岗位集合，再统计全部数量，避免把隐藏合并岗位计入总数'
)

assert.match(
  positionPage,
  /filteredPositions\.value\.length/,
  '文控岗位统计必须使用当前过滤后的显示数量'
)

assert.match(
  positionPage,
  /visiblePositions\.value\.length/,
  '文控岗位统计必须使用真实可见岗位总量'
)

for (const token of [
  'appliedQueryParams.code',
  'appliedQueryParams.name',
  'appliedQueryParams.active',
  '全部岗位',
  '显示'
]) {
  assert.ok(positionPage.includes(token), `文控岗位工具栏上下文必须覆盖真实筛选状态：${token}`)
}

for (const behaviorToken of [
  'handleQuery',
  'resetQuery',
  'loadData',
  'openCreateDialog',
  'openAssignmentDialog(row)',
  'saveApprovalPositionAssignments',
  'submitCreatePosition',
  'saveAssignments',
  "v-hasPermi=\"['dcc:controlled-file:position:manage']\""
]) {
  assert.ok(positionPage.includes(behaviorToken), `文控岗位原有行为必须保留：${behaviorToken}`)
}

assert.doesNotMatch(
  toolbar,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '文控岗位工具栏上下文不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC position toolbar summary static contract')
