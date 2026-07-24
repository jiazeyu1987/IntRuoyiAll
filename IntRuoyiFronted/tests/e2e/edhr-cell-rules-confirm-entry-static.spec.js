const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(repoRoot, relativePath))

const formListPage = read('src/views/mes/pro/batchrecordformlist/index.vue')
const dialogPath = 'src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue'

assert.ok(exists(dialogPath), '必须提供真实填写规则确认弹窗组件。')

const dialog = read(dialogPath)

assert.ok(
  formListPage.includes('BatchRecordCellRulesConfirmDialog'),
  '批记录表单页必须挂载填写规则确认组件。'
)
assert.ok(
  formListPage.includes("route.query.action") && formListPage.includes("'cellRules'"),
  '批记录表单页必须消费 action=cellRules 路由参数。'
)
assert.ok(
  formListPage.includes('openCellRulesDialog'),
  '批记录表单页必须能从路由参数或规则按钮打开填写规则确认入口。'
)
assert.ok(
  formListPage.includes("openTemplateAction(selectedReport, 'cellRules')"),
  '批记录表单预览区必须保留规则入口。'
)
assert.ok(
  formListPage.includes('@confirmed="handleCellRulesConfirmed"'),
  '填写规则保存成功后必须刷新当前真实模板预览。'
)

assert.ok(
  dialog.includes('BatchRecordReportApi.getCellRules'),
  '填写规则确认组件必须读取真实 cell-rules 接口。'
)
assert.ok(
  dialog.includes('BatchRecordReportApi.saveCellRules'),
  '填写规则确认组件必须调用真实 saveCellRules 接口。'
)
assert.ok(
  dialog.includes("source: 'MANUAL'") && dialog.includes('reviewed: true'),
  '页面确认自动候选时必须写成人工确认规则，不能保存 AUTO reviewed=true。'
)
assert.ok(
  !/source:\s*['"]AUTO['"][\s\S]{0,80}reviewed:\s*true/.test(dialog),
  '填写规则确认组件不得把 AUTO 规则直接标记为 reviewed=true。'
)
assert.ok(!dialog.includes('catch {}'), '填写规则接口错误不得被空 catch 吞掉。')

console.log('PASS: eDHR cell rules confirm entry static contract')
