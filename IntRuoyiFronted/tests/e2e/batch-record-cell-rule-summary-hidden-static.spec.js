const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const dialog = fs
  .readFileSync(
    path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue'),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

assert.ok(
  !dialog.includes('batch-record-cell-rules-editor__summary'),
  '单元格规则弹窗顶部红框汇总栏不应再渲染。'
)

assert.ok(!dialog.includes('待确认 {{ pendingCount }}'), '顶部待确认数量标签不应再显示。')
assert.ok(
  !dialog.includes('后端待确认 {{ unreviewedFillableCellCount }}'),
  '顶部后端待确认数量标签不应再显示。'
)
assert.ok(
  !dialog.includes('辅助表单映射：先选辅助格，再点未分配原表格'),
  '辅助表单映射右上提示文案不应再显示。'
)
assert.ok(
  !dialog.includes('原表单配置：左侧选单元格，右侧维护字段类型'),
  '原表单配置右上提示文案不应再显示。'
)

assert.ok(dialog.includes('batch-record-cell-rules-editor__workspace'), '隐藏顶部汇总栏不得移除主工作区。')
assert.ok(dialog.includes('batch-record-cell-rules-editor__mode-switch'), '隐藏顶部汇总栏不得移除模式切换入口。')
assert.ok(dialog.includes('原表单配置'), '隐藏顶部汇总栏不得移除原表单配置入口。')
assert.ok(dialog.includes('辅助表单映射'), '隐藏顶部汇总栏不得移除辅助表单映射入口。')
assert.ok(dialog.includes('原表单'), '隐藏顶部汇总栏不得移除左侧原表单预览。')
assert.ok(dialog.includes('batch-record-cell-rules-editor__side-panel'), '隐藏顶部汇总栏不得移除右侧规则编辑面板。')
assert.ok(dialog.includes('保存填写配置'), '隐藏顶部汇总栏不得移除保存按钮。')

console.log('PASS: batch record cell rule summary hidden static contract')
