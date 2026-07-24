const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const fieldAuditPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/FieldAuditPage.vue')
const source = fs.readFileSync(fieldAuditPagePath, 'utf8')

const assertIncludes = (token, message) => {
  assert.ok(source.includes(token), message)
}

const toolbarStart = source.indexOf(
  '<el-form :inline="true" :model="queryParams" class="edhr-field-audit__toolbar"'
)
assert.ok(toolbarStart >= 0, '字段审计页必须保留主查询工具栏。')

const alertStart = source.indexOf('<el-alert v-if="loadError"', toolbarStart)
assert.ok(alertStart > toolbarStart, '字段审计页工具栏后必须保留错误提示和表格。')

const toolbarSource = source.slice(toolbarStart, alertStart)
const advancedStart = toolbarSource.indexOf('<el-form-item class="edhr-field-audit__advanced">')
assert.ok(advancedStart > 0, '字段审计页必须提供高级筛选区。')
const primaryToolbarSource = toolbarSource.slice(0, advancedStart)

for (const label of [
  '执行ID',
  '原因',
  '修改时间',
  '查询',
  '重置',
  '校验当前筛选结果',
  '导出审计链'
]) {
  assert.ok(primaryToolbarSource.includes(label), `主工具栏必须保留字段审计主路径筛选和命令：${label}`)
}

for (const label of ['审计批次', '字段路径', '字段标识', '修改人', '原因关键字']) {
  assert.ok(!primaryToolbarSource.includes(`<el-form-item label="${label}"`), `精确定位筛选不得默认铺在主工具栏：${label}`)
}

for (const token of [
  'fieldAuditAdvancedFilterNames',
  'edhr-field-audit__advanced',
  'edhr-field-audit__advanced-grid',
  '<el-collapse',
  '<el-collapse-item title="高级筛选" name="advanced">',
  '<el-form-item label="审计批次">',
  '<el-form-item label="字段路径">',
  '<el-form-item label="字段标识">',
  '<el-form-item label="修改人">',
  '<el-form-item label="原因关键字">'
]) {
  assertIncludes(token, `字段审计精确定位筛选必须保留在默认收起的高级筛选中：${token}`)
}

assertIncludes(
  'const fieldAuditAdvancedFilterNames = ref<string[]>([])',
  '字段审计高级筛选必须默认收起'
)

for (const queryKey of [
  'auditBatchId: queryParams.auditBatchId?.trim() || undefined',
  'fieldPath: queryParams.fieldPath?.trim() || undefined',
  'fieldKey: queryParams.fieldKey?.trim() || undefined',
  'actorName: queryParams.actorName?.trim() || undefined',
  'reasonKeyword: queryParams.reasonKeyword?.trim() || undefined'
]) {
  assertIncludes(queryKey, `高级筛选布局不得移除原查询参数：${queryKey}`)
}

console.log('PASS: EDHR field audit toolbar advanced static contract')
