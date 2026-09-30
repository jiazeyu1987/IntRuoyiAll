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

const listTemplate = extractBetween(trainingMinePage, '<UnifiedListTemplate', '</UnifiedListTemplate>')

assert.match(
  listTemplate,
  /query-form-test-id="dcc-training-mine-toolbar"/,
  '我的培训标准筛选工具栏必须提供稳定测试标识'
)
assert.match(
  listTemplate,
  /:filter-definitions="trainingMineQuickFilterDefinitions"/,
  '我的培训筛选必须由标准快速过滤定义驱动'
)
assert.match(
  listTemplate,
  /@quick-filter-query="trainingMineQuickFilter\.applyQuickFilter"/,
  '我的培训查询必须由标准快速过滤 hook 触发'
)
assert.doesNotMatch(
  trainingMinePage,
  /ControlledFileWorkbenchEntry|const handleQuery|const resetQuery|<Pagination/,
  '我的培训页不得保留旧独立工作台入口、查询重置 handler 或独立分页'
)
assert.match(listTemplate, /data-testid="dcc-training-summary"/, '我的培训摘要列必须保留')
assert.match(listTemplate, /@pagination="getList"/, '我的培训分页必须由标准列表模板触发')

for (const behaviorToken of [
  'getMyTrainingTaskPage(queryParams)',
  'getFileCategoryList()',
  'getSimpleDeptList()',
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
  listTemplate,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '我的培训统一列表工具栏不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC training mine workbench toolbar static contract')
