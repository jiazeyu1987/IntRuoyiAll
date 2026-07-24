const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()

const read = (relativePath) => fs.readFileSync(path.resolve(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.resolve(root, relativePath))

const forbiddenExactRoutePattern = /(['"`])\/mes\/pro\/feedback\/edhr-execution\1/
const forbiddenRouterPathPattern = /path:\s*(['"`])pro\/feedback\/edhr-execution\1/

assert.equal(
  exists('src/views/mes/pro/edhr/ExecutionListPage.vue'),
  false,
  '旧 eDHR 执行列表组件必须删除。'
)

const sourceFiles = [
  'src/router/modules/remaining.ts',
  'src/views/mes/pro/feedback/FeedbackForm.vue',
  'src/views/mes/pro/edhr/ExecutionPage.vue',
  'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue',
  'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
]

for (const relativePath of sourceFiles) {
  const source = read(relativePath)
  assert.equal(
    source.includes('MesProFeedbackEdhrExecutionListPage'),
    false,
    `${relativePath} 不得继续引用旧执行列表组件名。`
  )
  assert.equal(
    source.includes('mes/pro/edhr/ExecutionListPage'),
    false,
    `${relativePath} 不得继续引用旧执行列表组件路径。`
  )
  assert.equal(
    forbiddenExactRoutePattern.test(source),
    false,
    `${relativePath} 不得继续写入裸路径 /mes/pro/feedback/edhr-execution。`
  )
  assert.equal(
    forbiddenRouterPathPattern.test(source),
    false,
    `${relativePath} 不得继续注册裸路由 pro/feedback/edhr-execution。`
  )
}

const feedbackForm = read('src/views/mes/pro/feedback/FeedbackForm.vue')
assert.match(feedbackForm, /查看批次执行/, '报工页入口文案必须迁移为查看批次执行。')
assert.match(
  feedbackForm,
  /path:\s*'\/mes\/pro\/feedback\/edhr-batch-execution'/,
  '报工页入口必须跳转批次执行列表。'
)
assert.match(feedbackForm, /workOrderCode/, '报工页跳转必须携带工单编码筛选。')
assert.match(feedbackForm, /batchCode/, '报工页跳转必须携带批次号筛选。')

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
assert.equal(
  executionPage.includes('返回执行列表'),
  false,
  '执行表单不得继续显示返回执行列表。'
)
assert.match(executionPage, /返回批次详情|返回批次执行/, '执行表单返回文案必须指向批次入口。')
assert.match(
  executionPage,
  /path:\s*'\/mes\/pro\/feedback\/edhr-batch-execution\/detail'/,
  '执行表单有 batchExecutionId 时必须返回批次详情。'
)
assert.match(
  executionPage,
  /path:\s*'\/mes\/pro\/feedback\/edhr-batch-execution'/,
  '执行表单无 batchExecutionId 时必须返回批次执行列表。'
)

const batchDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const singleArchiveIndex = batchDetail.indexOf("key: 'single-archive'")
assert.ok(singleArchiveIndex > 0, '批次详情必须保留单表归档入口。')
const singleArchiveBlock = batchDetail.slice(singleArchiveIndex, singleArchiveIndex + 900)
assert.match(
  singleArchiveBlock,
  /path:\s*'\/mes\/pro\/feedback\/edhr-execution\/form'/,
  '单表归档必须跳转执行表单只读视图。'
)
assert.match(singleArchiveBlock, /viewMode:\s*'tracking'/, '单表归档必须进入 tracking 只读视图。')

for (const token of [
  'generateEdhrExecutionArchive',
  'downloadEdhrExecutionArchive',
  'getLatestEdhrExecutionArchive',
  '生成归档打印件',
  '下载归档打印件'
]) {
  assert.ok(executionPage.includes(token), `执行表单必须承接单条执行归档能力：${token}`)
}

const batchList = read('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
assert.match(batchList, /applyRouteQueryFilters/, '批次执行列表必须从 route query 初始化筛选。')
assert.match(batchList, /route\.query\.workOrderCode/, '批次执行列表必须支持 workOrderCode query。')
assert.match(batchList, /route\.query\.batchCode/, '批次执行列表必须支持 batchCode query。')
assert.match(batchList, /route\.query\.batchExecutionCode/, '批次执行列表必须支持 batchExecutionCode query。')
