const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const categoryPage = readSource('src/views/dcc/controlled-file/categories/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.ok(categoryPage.includes('label="类别列表"'), '类别页必须保留类别列表页签')
assert.ok(categoryPage.includes('label="审阅矩阵"'), '类别页必须新增 审阅矩阵 页签')
assert.ok(
  categoryPage.includes('CategoryReviewMatrixTable'),
  '类别页必须接入独立的审阅矩阵表格组件'
)

const categoryTable = extractBetween(
  categoryPage,
  '<el-table v-loading="loading" :data="filteredCategories"',
  '</el-table>'
)

assert.strictEqual(
  packageJson.scripts['e2e:dcc:category-governance-summary:static'],
  'node tests/e2e/dcc-category-governance-summary-static.spec.js',
  'package.json 必须提供 e2e:dcc:category-governance-summary:static 脚本'
)

assert.ok(
  categoryTable.includes('data-testid="dcc-category-governance-summary"'),
  '类别列表必须提供稳定的治理摘要测试标识'
)
for (const label of ['治理摘要']) {
  assert.ok(categoryTable.includes(`label="${label}"`), `类别列表必须显示 ${label} 列`)
}

assert.ok(
  !categoryTable.includes('data-testid="dcc-category-approval-summary"'),
  '类别列表不应继续渲染审批摘要测试标识'
)
assert.ok(!categoryTable.includes('label="审批摘要"'), '类别列表不应继续显示 审批摘要 列')

for (const removedHeader of ['启用状态', '分发', '培训', '审核', '批准', '创建时间']) {
  assert.ok(
    !categoryTable.includes(`label="${removedHeader}"`),
    `类别列表不应继续显示独立 ${removedHeader} 表头`
  )
}

for (const token of [
  'row.active',
  'row.distributionRequired',
  'row.trainingRequired',
  'row.createTime',
  'formatDate(row.createTime)'
]) {
  assert.ok(categoryTable.includes(token), `摘要列必须继续使用真实类别字段：${token}`)
}

for (const token of ['启用', '分发', '培训', '创建']) {
  assert.ok(categoryTable.includes(token), `摘要列必须展示 ${token}`)
}

for (const behaviorToken of [
  "openCategoryUploadPolicyDialog(row)",
  "openForm('update', row)",
  'handleDelete(row)',
  'openUploadPolicyDialog',
  'CategoryForm',
  'CategoryReviewMatrixTable',
  'UploadSizePolicyDialog',
  'CategoryUploadSizePolicyDialog'
]) {
  assert.ok(categoryPage.includes(behaviorToken), `类别页原有行为必须保留：${behaviorToken}`)
}

assert.ok(
  !categoryTable.includes('审批矩阵'),
  '类别列表操作列不应继续保留审批矩阵直达按钮，必须统一从审阅矩阵页签维护'
)

for (const token of [
  'const hasApprovalSummary =',
  'const visibleCategories = computed',
  'filteredCategories.value.filter(hasApprovalSummary)'
]) {
  assert.ok(
    !categoryPage.includes(token),
    `类别列表不应再依赖审批摘要过滤隐藏真实类别：${token}`
  )
}

for (const removedToken of [
  'row.signoffPositionIds',
  'row.approvalPositionIds',
  'resolveStagePositionLabel(row.signoffPositionIds)',
  'resolveStagePositionLabel(row.approvalPositionIds)',
  '审核',
  '批准'
]) {
  assert.ok(!categoryTable.includes(removedToken), `类别列表不应继续保留审批摘要内容：${removedToken}`)
}

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(categoryTable),
  '类别治理摘要不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC category governance summary static contract')
