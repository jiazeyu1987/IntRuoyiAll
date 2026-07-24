const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const readonlyForm = read('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')

assert(
  readonlyForm.includes('checkboxState: boolean | null'),
  '只读模板渲染单元格必须携带 checkboxState，避免把 BOOLEAN 默认值渲染成 false 文本。'
)
assert(
  readonlyForm.includes('const checkboxState = resolveReadonlyCheckboxState(rowIndex, columnIndex, rawCell)'),
  '只读模板渲染行必须先解析 checkbox 状态，再决定文本内容。'
)
assert(
  readonlyForm.includes('v-if="cell.checkboxState !== null"'),
  'checkbox 单元格必须优先渲染 checkbox 视觉控件。'
)
assert(
  readonlyForm.includes('class="edhr-template-sheet__checkbox-control"'),
  'checkbox 单元格必须使用独立 checkbox 控件样式。'
)
assert(
  readonlyForm.includes('resolveReadonlyCheckboxState'),
  '只读模板必须提供通用 checkbox 状态解析方法。'
)
assert(
  readonlyForm.includes("rawType.includes('checkbox')") &&
    readonlyForm.includes("rawValueType === 'BOOLEAN'"),
  'checkbox 状态解析必须同时支持 componentFlag=checkbox 和 valueType=BOOLEAN。'
)
assert(
  !readonlyForm.includes('String(value)') ||
    readonlyForm.indexOf('resolveReadonlyCheckboxState') < readonlyForm.indexOf('stringifyValue'),
  'BOOLEAN/checkbox 值必须先进入 checkbox 渲染分支，不能直接 stringify 成 false。'
)

console.log('PASS: eDHR readonly checkbox control static contract')
