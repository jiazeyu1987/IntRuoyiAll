const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const signaturePagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/SignaturePage.vue')
const source = fs.readFileSync(signaturePagePath, 'utf8')

const assertIncludes = (token, message) => {
  assert.ok(source.includes(token), message)
}

const toolbarStart = source.indexOf('<el-form :inline="true" :model="queryParams" class="edhr-query__toolbar">')
assert.ok(toolbarStart >= 0, '签名记录页必须保留主查询工具栏。')

const tableStart = source.indexOf('<el-alert v-if="loadError"', toolbarStart)
assert.ok(tableStart > toolbarStart, '签名记录页工具栏后必须保留错误提示和表格。')

const toolbarSource = source.slice(toolbarStart, tableStart)
const advancedStart = toolbarSource.indexOf('<el-form-item class="edhr-query__advanced">')
assert.ok(advancedStart > 0, '签名记录页必须提供高级筛选区。')
const primaryToolbarSource = toolbarSource.slice(0, advancedStart)

for (const label of ['执行编号', '签名人', '动作', '签名时间', '查询', '重置']) {
  assert.ok(primaryToolbarSource.includes(label), `主工具栏必须保留业务筛选和操作：${label}`)
}

for (const label of ['执行ID', '签名人ID', '流程实例', 'BPM任务']) {
  assert.ok(!primaryToolbarSource.includes(`<el-form-item label="${label}"`), `技术筛选不得默认铺在主工具栏：${label}`)
}

for (const token of [
  'signatureAdvancedFilterNames',
  'edhr-query__advanced',
  'edhr-query__advanced-grid',
  '<el-collapse',
  '<el-collapse-item title="高级筛选" name="advanced">',
  '<el-form-item label="执行ID">',
  '<el-form-item label="签名人ID">',
  '<el-form-item label="流程实例">',
  '<el-form-item label="BPM任务">'
]) {
  assertIncludes(token, `签名记录技术筛选必须保留在默认收起的高级筛选中：${token}`)
}

assertIncludes(
  'const signatureAdvancedFilterNames = ref<string[]>([])',
  '签名记录高级筛选必须默认收起'
)

for (const queryKey of [
  'executionId: Number.isFinite(queryParams.executionId) ? queryParams.executionId : undefined',
  'actorId: Number.isFinite(queryParams.actorId) ? queryParams.actorId : undefined',
  'processInstanceId: queryParams.processInstanceId.trim() || undefined',
  'bpmTaskId: queryParams.bpmTaskId.trim() || undefined'
]) {
  assertIncludes(queryKey, `高级筛选布局不得移除原查询参数：${queryKey}`)
}

console.log('PASS: EDHR signature toolbar advanced static contract')
