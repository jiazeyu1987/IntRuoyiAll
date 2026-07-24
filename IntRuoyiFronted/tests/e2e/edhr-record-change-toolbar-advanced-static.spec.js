const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const recordChangePagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/RecordChangePage.vue')
const source = fs.readFileSync(recordChangePagePath, 'utf8')

const assertIncludes = (token, message) => {
  assert.ok(source.includes(token), message)
}

const toolbarStart = source.indexOf(
  '<el-form :inline="true" :model="queryParams" class="edhr-record-change__toolbar"'
)
assert.ok(toolbarStart >= 0, '变更记录页必须保留主查询工具栏。')

const alertStart = source.indexOf('<el-alert v-if="loadError"', toolbarStart)
assert.ok(alertStart > toolbarStart, '变更记录页工具栏后必须保留错误提示和表格。')

const toolbarSource = source.slice(toolbarStart, alertStart)

for (const label of ['变更类型', '状态', '范围', '查询']) {
  assert.ok(toolbarSource.includes(label), `主工具栏必须保留变更业务筛选：${label}`)
}

for (const removedLabel of ['重置', '高级筛选', '执行ID', '批次ID']) {
  assert.ok(!toolbarSource.includes(removedLabel), `截图黄框内容必须从变更记录工具栏删除：${removedLabel}`)
}

for (const removedToken of [
  'recordChangeAdvancedFilterNames',
  'edhr-record-change__advanced',
  'edhr-record-change__advanced-grid',
  '<el-collapse-item title="高级筛选" name="advanced">',
  '<el-form-item label="执行ID">',
  '<el-form-item label="批次ID">'
]) {
  assert.ok(!source.includes(removedToken), `变更记录页不应保留已删除高级筛选实现：${removedToken}`)
}

assert.ok(!toolbarSource.includes('<el-collapse'), '变更记录工具栏不应保留高级筛选折叠面板。')

for (const queryKey of [
  'batchExecutionId: parsePositiveNumber(route.query.batchExecutionId)',
  'executionId: parsePositiveNumber(route.query.executionId)',
  'queryParams.executionId = parsePositiveNumber(queryParams.executionId)',
  'queryParams.batchExecutionId = parsePositiveNumber(queryParams.batchExecutionId)'
]) {
  assertIncludes(queryKey, `高级筛选布局不得移除原查询参数：${queryKey}`)
}

console.log('PASS: EDHR record change toolbar advanced static contract')
