const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/feedback/index.vue')
const apiPath = path.join(root, 'src/api/mes/pro/feedback/index.ts')

assert.ok(fs.existsSync(pagePath), `生产报工页面必须存在：${pagePath}`)
assert.ok(fs.existsSync(apiPath), `生产报工 API 类型必须存在：${apiPath}`)

const source = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

const tableStart = source.indexOf('<ContentWrap v-if="activeTab === \'feedback\'">')
const tableEnd = source.indexOf('</el-table>', tableStart)
assert.ok(tableStart >= 0 && tableEnd > tableStart, '必须保留正式报工列表表格。')
const feedbackTable = source.slice(tableStart, tableEnd)

for (const field of [
  'itemCode',
  'itemName',
  'processCode',
  'processName',
  'feedbackUserNickname',
  'feedbackTime'
]) {
  assert.ok(apiSource.includes(`${field}:`), `ProFeedbackVO 必须声明正式报工字段 ${field}。`)
}

for (const helper of [
  'resolveFeedbackProductCode',
  'resolveFeedbackProductName',
  'resolveFeedbackProcessCode',
  'resolveFeedbackProcessName',
  'resolveFeedbackEmployeeName',
  'resolveFeedbackDisplayTime'
]) {
  assert.ok(source.includes(`const ${helper}`), `报工列表必须提供 ${helper}，避免普通正式报工行显示为空。`)
  assert.ok(feedbackTable.includes(`${helper}(scope.row)`), `正式报工表格必须调用 ${helper}(scope.row)。`)
}

assert.match(
  source,
  /const resolveFeedbackProductCode = \(row: ProFeedbackVO\) =>\s*resolveFeedbackDisplayText\(row\.excelProductCode,\s*row\.itemCode\)/,
  '产品代码列必须按导入快照字段和正式产品编码共同显示。'
)
assert.match(
  source,
  /const resolveFeedbackProductName = \(row: ProFeedbackVO\) =>\s*resolveFeedbackDisplayText\(row\.excelProductName,\s*row\.itemName\)/,
  '产品名称列必须按导入快照字段和正式产品名称共同显示。'
)
assert.match(
  source,
  /const resolveFeedbackProcessCode = \(row: ProFeedbackVO\) =>\s*resolveFeedbackDisplayText\(row\.excelProcessCode,\s*row\.processCode\)/,
  '工序编码列必须按导入快照字段和正式工序编码共同显示。'
)
assert.match(
  source,
  /const resolveFeedbackProcessName = \(row: ProFeedbackVO\) =>\s*resolveFeedbackDisplayText\(row\.excelProcessName,\s*row\.processName\)/,
  '工序名称列必须按导入快照字段和正式工序名称共同显示。'
)
assert.match(
  source,
  /const resolveFeedbackEmployeeName = \(row: ProFeedbackVO\) =>\s*resolveFeedbackDisplayText\(row\.excelEmployeeName,\s*row\.feedbackUserNickname\)/,
  '人员名称列必须按导入快照字段和正式报工人昵称共同显示。'
)
assert.match(
  source,
  /const resolveFeedbackDisplayTime = \(row: ProFeedbackVO\) =>\s*row\.excelFeedbackTime \?\? row\.feedbackTime/,
  '日期列必须在导入快照时间为空时显示正式报工时间。'
)

assert.doesNotMatch(source, /catch\s*\{\s*\}/, '报工页不得吞掉异常。')
console.log('PASS: mes-feedback-list-canonical-display-static')
