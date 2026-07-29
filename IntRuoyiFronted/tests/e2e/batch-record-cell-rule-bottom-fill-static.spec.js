const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

assert.ok(
  dialog.includes('height: calc(100vh - 84px);'),
  '填写配置编辑器必须占满全屏弹窗正文高度，去掉底部空白占位。'
)
assert.ok(
  /\.batch-record-cell-rules-editor__workspace\s*\{[\s\S]*?flex:\s*1;[\s\S]*?height:\s*auto;/m.test(dialog),
  '填写配置主工作区必须用 flex 填充剩余高度，让原表单、辅助预览和控制栏延伸到底部。'
)
assert.ok(
  !dialog.includes('height: clamp(520px, calc(100vh - 220px), 880px);'),
  '保存区不在弹窗页脚后，工作区不得继续按旧页脚预留底部空白。'
)

console.log('PASS: batch record cell rule bottom fill static contract')
