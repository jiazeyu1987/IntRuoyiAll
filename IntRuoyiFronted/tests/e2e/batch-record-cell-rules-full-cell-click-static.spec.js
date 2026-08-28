const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const component = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const template = component.slice(
  component.indexOf('<template>'),
  component.indexOf('<script setup')
)
const style = component.slice(component.indexOf('<style scoped>'))

const tdBlockMatch = template.match(
  /<td[\s\S]*?:class="\[cell\.classNames, \{ 'is-assist-mapped': isSourceCellMappedToAssistGrid\(cell\) \}\]"[\s\S]*?>[\s\S]*?<button/
)
assert.ok(tdBlockMatch, '必须能定位原表单元格 td 渲染块。')

const tdBlock = tdBlockMatch[0]
assert.ok(
  tdBlock.includes('@click="handleSourceCellClick(cell, $event)"'),
  '原表单元格 td 必须承接统一点击入口，保证点击空白区域也能选中。'
)
assert.ok(
  template.includes('@click.stop="handleSourceCellClick(cell, $event)"'),
  '内部按钮点击必须停止冒泡，避免 Ctrl/Command 多选时同一次点击被重复切换。'
)
assert.ok(
  /\.batch-record-cell-rules-editor__cell\s*\{[\s\S]*?cursor:\s*pointer;[\s\S]*?\}/.test(style),
  '原表单元格整体必须显示可点击指针。'
)
assert.ok(
  /\.batch-record-cell-rules-editor__cell-button\s*\{[\s\S]*?height:\s*100%;[\s\S]*?\}/.test(style),
  '内部按钮应尽量铺满单元格，减少视觉命中差异。'
)

console.log('PASS batch-record-cell-rules-full-cell-click-static')
