const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '..', '..')
const readonlyForm = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'),
  'utf8'
)

assert.ok(
  readonlyForm.includes('const hasFillableRule = Boolean(cell?.fillForm || cell?.edhrCellRule)'),
  '只读预览必须把只有 edhrCellRule、没有 fillForm 的可填写规则单元格也视为可显示 placeholder。'
)

assert.ok(
  !readonlyForm.includes('if (!cell?.fillForm || renderedText.trim()) return'),
  'placeholder 展示不能先要求 fillForm，否则规则编辑后只有 edhrCellRule 的数字空格会变成空白。'
)

assert.ok(
  readonlyForm.includes('const rulePlaceholder = cell?.edhrCellRule?.placeholder'),
  '只读预览必须优先读取规则上的 placeholder。'
)

assert.ok(
  readonlyForm.includes("'is-fillable': Boolean(rawCell?.fillForm || rawCell?.edhrCellRule)"),
  '只读预览样式必须把 edhrCellRule-only 单元格按可填写规则单元格呈现。'
)

console.log('PASS: eDHR readonly placeholder visible static contract')
