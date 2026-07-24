const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

assertNotIncludes(dialog, 'width="1280px"', '单元格规则弹框不得继续使用黄框对应的固定 1280px 宽度。')
assertIncludes(
  dialog,
  'width="calc(100vw - 32px)"',
  '单元格规则弹框默认宽度必须接近浏览器视口红框范围，并保留少量边距。'
)
assertIncludes(
  dialog,
  'height: clamp(520px, calc(100vh - 220px), 880px);',
  '规则编辑工作区高度必须跟随视口，避免仍停留在旧黄框高度。'
)
assertIncludes(
  dialog,
  'grid-template-columns: minmax(0, 1fr) 360px;',
  '红框尺寸下右侧规则面板应略加宽，保证字段名、类型和控件类型好点好改。'
)
assertIncludes(
  dialog,
  'overflow: auto;',
  '表单预览和右侧编辑面板必须保留内部滚动，避免近全屏弹框内容溢出。'
)
assertIncludes(
  dialog,
  'batch-record-cell-rules-editor__sheet-scroll',
  '扩大弹框后仍必须通过预览滚动容器展示真实表格。'
)
assertIncludes(
  dialog,
  'batch-record-cell-rules-editor__side-panel',
  '扩大弹框后仍必须保留右侧规则编辑面板。'
)
assertNotIncludes(
  dialog,
  'class="batch-record-cell-rules-editor__save-tip"',
  '截图红框中的右侧保存提示不应再显示。'
)
assertNotIncludes(
  dialog,
  '.batch-record-cell-rules-editor__save-tip',
  '删除保存提示后不应保留无用样式锚点。'
)
assertNotIncludes(dialog, 'batch-record-cell-rules-editor__rule-list', '截图红框中的规则清单不应再显示。')

console.log('PASS: batch record cell rule dialog size static contract')
