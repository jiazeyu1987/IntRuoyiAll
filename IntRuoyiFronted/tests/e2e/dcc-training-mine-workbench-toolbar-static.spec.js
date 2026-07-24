const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const trainingMinePage = readSource('src/views/dcc/controlled-file/training/mine/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const toolbar = extractBetween(trainingMinePage, '<el-form', '</el-form>')
const tableShell = extractBetween(trainingMinePage, '<ContentWrap>\n    <el-table', '</ContentWrap>')

assert.match(
  toolbar,
  /data-testid="dcc-training-mine-toolbar"/,
  '我的培训筛选工具栏必须提供稳定测试标识'
)

assert.match(
  toolbar,
  /<ControlledFileWorkbenchEntry \/>/,
  'DCC 工作台入口必须并入我的培训筛选工具栏'
)

assert.ok(
  toolbar.indexOf('resetQuery') < toolbar.indexOf('ControlledFileWorkbenchEntry'),
  'DCC 工作台入口应位于查询/重置命令之后，形成页面级命令组'
)

assert.doesNotMatch(
  trainingMinePage,
  /<div class="mb-12px flex justify-end">\s*<ControlledFileWorkbenchEntry \/>/,
  '我的培训筛选表单上方不应继续保留孤立右对齐工作台入口行'
)

assert.match(tableShell, /data-testid="dcc-training-summary"/, '我的培训摘要列必须保留')
assert.match(tableShell, /Pagination/, '我的培训分页必须保留')

for (const behaviorToken of [
  'getMyTrainingTaskPage(queryParams)',
  'getFileCategoryList()',
  'getSimpleDeptList()',
  'handleQuery',
  'resetQuery',
  'openTask(row.progressId)',
  'openDetail(row.controlledFileId)',
  "name: 'DccTrainingTask'",
  "openControlledFileViewer(router, route, id, 'training-mine')"
]) {
  assert.ok(trainingMinePage.includes(behaviorToken), `我的培训原有行为必须保留：${behaviorToken}`)
}
assert.ok(
  !trainingMinePage.includes("name: 'DccControlledFileDetail'"),
  '我的培训文件入口不得继续跳普通文件详情页'
)

assert.doesNotMatch(
  toolbar,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '我的培训工作台入口工具栏收敛不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC training mine workbench toolbar static contract')
