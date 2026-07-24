const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/FieldAuditPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const assertIncludes = (token, message) => {
  assert.ok(source.includes(token), message)
}

const assertNotIncludes = (token, message) => {
  assert.ok(!source.includes(token), message)
}

assertIncludes(
  '<el-table-column label="执行编号" min-width="150">',
  '字段审计执行编号列必须改为模板列，承载执行详情入口。'
)
assertIncludes(
  'class="edhr-field-audit__execution-link"',
  '字段审计执行编号链接必须有稳定样式类。'
)
assertIncludes(
  '@click="openExecution(row)"',
  '字段审计执行编号点击后必须打开对应执行表单。'
)
assertIncludes(
  '{{ row.executionCode || `#${row.executionId}` }}',
  '字段审计执行编号链接必须优先展示业务编号，缺失时保留可追溯 ID 文本。'
)
assertIncludes(
  'if (!row.executionId) {',
  '字段审计执行表单入口必须显式处理缺失 executionId。'
)
assertIncludes(
  "message.error('当前字段审计记录缺少执行ID，无法打开执行表单。')",
  '字段审计执行表单入口缺少 executionId 时必须给出用户可见提示。'
)
assertIncludes(
  "path: '/mes/pro/feedback/edhr-execution/form'",
  '字段审计执行入口必须使用 eDHR 执行表单路由。'
)
assertIncludes(
  'query: { id: String(row.executionId) }',
  '字段审计执行详情入口必须用真实 executionId 构造 query。'
)
assertIncludes(
  '<el-table-column label="操作" width="76" fixed="right">',
  '移除重复执行入口后，字段审计操作列应保持紧凑。'
)
assertIncludes(
  '<el-button link type="primary" @click="openDetail(row)">详情</el-button>',
  '字段审计行操作必须保留审计详情入口。'
)

assertNotIncludes(
  '<el-table-column label="执行编号" prop="executionCode"',
  '字段审计执行编号不应继续只是普通文本列。'
)
assertNotIncludes(
  '定位执行记录',
  '执行编号成为主入口后，操作列不应重复展示定位执行记录按钮。'
)
assertNotIncludes('mock', '字段审计执行入口优化不得使用 mock 数据。')
assertNotIncludes('降级', '字段审计执行入口优化不得引入降级路径。')

console.log('PASS: EDHR field audit execution link static contract')
