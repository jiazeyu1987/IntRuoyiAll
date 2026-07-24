const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = read('src/views/mes/pro/batchrecordformlist/index.vue')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

assertIncludes(page, 'BatchRecordCellRulesConfirmDialog', '页面必须继续挂载真实单元格规则弹框。')
assertIncludes(
  page,
  "openTemplateAction(selectedReport, 'cellRules')",
  '右侧表单预览工具栏必须保留规则按钮入口。'
)
assertIncludes(page, "action === 'cellRules'", '右侧规则按钮必须继续复用 cellRules 动作。')
assertIncludes(page, 'openCellRulesDialog(row)', 'cellRules 动作必须继续打开真实规则弹框。')
assertIncludes(page, '@confirmed="handleCellRulesConfirmed"', '保存规则后必须继续刷新真实预览。')

assertNotIncludes(
  page,
  '@click.stop="openCellRulesDialog(row)"',
  '左侧列表行级单元格规则入口不应再显示。'
)
assert.ok(
  !/<el-table-column[^>]*label="操作"[\s\S]{0,260}openCellRulesDialog\(row\)/.test(page),
  '左侧列表不应保留只承载单元格规则的操作列。'
)
assert.ok(
  !/<el-table-column[^>]*label="操作"[\s\S]{0,320}单元格规则/.test(page),
  '左侧列表操作列不应显示“单元格规则”文字。'
)

console.log('PASS: batch record cell rule list action hidden static contract')
