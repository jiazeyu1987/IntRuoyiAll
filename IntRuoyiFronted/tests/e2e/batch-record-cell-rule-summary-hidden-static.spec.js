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
assert.ok(!dialog.includes('规则编辑模式：左侧只选单元格'), '顶部规则编辑模式提示不应再显示。')

assert.ok(dialog.includes('batch-record-cell-rules-editor__workspace'), '隐藏顶部汇总栏不得移除主工作区。')
assert.ok(dialog.includes('只读表单预览'), '隐藏顶部汇总栏不得移除左侧只读预览。')
assert.ok(dialog.includes('batch-record-cell-rules-editor__side-panel'), '隐藏顶部汇总栏不得移除右侧规则编辑面板。')
assert.ok(dialog.includes('保存规则'), '隐藏顶部汇总栏不得移除保存按钮。')

console.log('PASS: batch record cell rule summary hidden static contract')
