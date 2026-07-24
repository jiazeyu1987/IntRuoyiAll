const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const domainTracePagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/DomainTracePage.vue')
const source = fs.readFileSync(domainTracePagePath, 'utf8')

const assertIncludes = (token, message) => {
  assert.ok(source.includes(token), message)
}

const toolbarStart = source.indexOf(
  '<el-form :inline="true" :model="queryParams" class="edhr-domain-trace__toolbar"'
)
assert.ok(toolbarStart >= 0, '主数据追溯页必须保留主查询工具栏。')

const alertStart = source.indexOf('<el-alert v-if="loadError"', toolbarStart)
assert.ok(alertStart > toolbarStart, '主数据追溯页工具栏后必须保留错误提示和表格。')

const toolbarSource = source.slice(toolbarStart, alertStart)
const advancedStart = toolbarSource.indexOf('<el-form-item class="edhr-domain-trace__advanced">')
assert.ok(advancedStart > 0, '主数据追溯页必须提供高级筛选区。')
const primaryToolbarSource = toolbarSource.slice(0, advancedStart)

for (const label of ['执行编号', '状态', '校验时间', '查询', '重置', '刷新']) {
  assert.ok(primaryToolbarSource.includes(label), `主工具栏必须保留主数据追溯主路径筛选和命令：${label}`)
}

for (const label of ['工单号', '批次号']) {
  assert.ok(!primaryToolbarSource.includes(`<el-form-item label="${label}"`), `生产上下文辅助筛选不得默认铺在主工具栏：${label}`)
}

for (const token of [
  'domainTraceAdvancedFilterNames',
  'edhr-domain-trace__advanced',
  'edhr-domain-trace__advanced-grid',
  '<el-collapse',
  '<el-collapse-item title="高级筛选" name="advanced">',
  '<el-form-item label="工单号">',
  '<el-form-item label="批次号">'
]) {
  assertIncludes(token, `主数据追溯生产上下文筛选必须保留在默认收起的高级筛选中：${token}`)
}

assertIncludes(
  'const domainTraceAdvancedFilterNames = ref<string[]>([])',
  '主数据追溯高级筛选必须默认收起'
)

for (const queryKey of [
  'workOrderCode: queryParams.workOrderCode?.trim() || undefined',
  'batchCode: queryParams.batchCode?.trim() || undefined'
]) {
  assertIncludes(queryKey, `高级筛选布局不得移除原查询参数：${queryKey}`)
}

console.log('PASS: EDHR domain trace toolbar advanced static contract')
