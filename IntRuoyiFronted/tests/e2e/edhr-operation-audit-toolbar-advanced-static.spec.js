const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const operationAuditPagePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/OperationAuditPage.vue'
)
const source = fs.readFileSync(operationAuditPagePath, 'utf8')

const assertIncludes = (token, message) => {
  assert.ok(source.includes(token), message)
}

const toolbarStart = source.indexOf(
  '<el-form :inline="true" :model="queryParams" class="edhr-operation-audit__toolbar">'
)
assert.ok(toolbarStart >= 0, '操作审计页必须保留主查询工具栏。')

const alertStart = source.indexOf('<el-alert v-if="loadError"', toolbarStart)
assert.ok(alertStart > toolbarStart, '操作审计页工具栏后必须保留错误提示和表格。')

const toolbarSource = source.slice(toolbarStart, alertStart)
const advancedStart = toolbarSource.indexOf('<el-form-item class="edhr-operation-audit__advanced">')
assert.ok(advancedStart > 0, '操作审计页必须提供高级筛选区。')
const primaryToolbarSource = toolbarSource.slice(0, advancedStart)

for (const label of [
  '对象类型',
  '对象ID',
  '记录类型',
  '动作类型',
  '权限决策',
  '操作结果',
  '发生时间',
  '查询',
  '重置'
]) {
  assert.ok(primaryToolbarSource.includes(label), `主工具栏必须保留对象上下文和业务筛选：${label}`)
}

for (const label of ['执行ID', '批次ID', '操作者ID']) {
  assert.ok(!primaryToolbarSource.includes(`<el-form-item label="${label}"`), `内部 ID 筛选不得默认铺在主工具栏：${label}`)
}

for (const token of [
  'operationAuditAdvancedFilterNames',
  'edhr-operation-audit__advanced',
  'edhr-operation-audit__advanced-grid',
  '<el-collapse',
  '<el-collapse-item title="高级筛选" name="advanced">',
  '<el-form-item label="执行ID">',
  '<el-form-item label="批次ID">',
  '<el-form-item label="操作者ID">'
]) {
  assertIncludes(token, `操作审计内部 ID 筛选必须保留在默认收起的高级筛选中：${token}`)
}

assertIncludes(
  'const operationAuditAdvancedFilterNames = ref<string[]>([])',
  '操作审计高级筛选必须默认收起'
)

for (const queryKey of [
  'batchExecutionId: queryParams.batchExecutionId || undefined',
  'executionId: queryParams.executionId || undefined',
  'actorUserId: queryParams.actorUserId || undefined'
]) {
  assertIncludes(queryKey, `高级筛选布局不得移除原查询参数：${queryKey}`)
}

console.log('PASS: EDHR operation audit toolbar advanced static contract')
