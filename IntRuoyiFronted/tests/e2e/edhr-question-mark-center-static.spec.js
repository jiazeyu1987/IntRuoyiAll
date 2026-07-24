const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const readonlyFormPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'
)
const readonlyForm = fs.readFileSync(readonlyFormPath, 'utf8')
const placeholderStyle = readonlyForm.match(
  /\.edhr-template-sheet__fillable-placeholder\s*\{([\s\S]*?)\}/
)?.[1]

assert.ok(placeholderStyle, '必须保留填写问号占位符样式')
assert.match(
  placeholderStyle,
  /display:\s*flex;/,
  '填写问号必须使用独立块级 flex，避免窄单元格中的行内溢出偏移'
)
assert.match(
  placeholderStyle,
  /width:\s*min\(56px,\s*100%\);/,
  '填写问号下划线宽度必须随窄单元格收缩'
)
assert.match(
  placeholderStyle,
  /min-width:\s*0;/,
  '填写问号不得用固定最小宽度撑出窄单元格'
)
assert.match(
  placeholderStyle,
  /box-sizing:\s*border-box;/,
  '填写问号的内边距必须计入单元格可用宽度'
)
assert.match(
  placeholderStyle,
  /margin:\s*0 auto;/,
  '填写问号占位符必须以所属单元格为基准水平居中'
)
assert.equal(
  (readonlyForm.match(/v-if="cell\.text\.trim\(\)"/g) || []).length,
  2,
  '空文本节点不得与填写问号共同参与行内排版'
)

console.log('PASS: EDHR question-mark placeholders stay centered in narrow cells')
