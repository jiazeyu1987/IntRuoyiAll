const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const readonlyForm = read('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')

assert(
  readonlyForm.includes('checkboxLabelText: string'),
  '只读模板渲染单元格必须携带 checkboxLabelText，避免 checkbox 只显示方框。'
)
assert(
  readonlyForm.includes('const checkboxLabelText = resolveReadonlyCheckboxLabelText(rowIndex, columnIndex, rawCell)'),
  '只读模板渲染行必须为 checkbox 单元格解析 fillForm.labelText。'
)
assert(
  readonlyForm.includes('v-if="cell.checkboxLabelText"') &&
    readonlyForm.includes('class="edhr-template-sheet__checkbox-label"') &&
    readonlyForm.includes('{{ cell.checkboxLabelText }}'),
  'checkbox 控件旁边必须把识别出的 labelText 作为可见文本渲染。'
)
assert(
  readonlyForm.includes('const resolveReadonlyCheckboxLabelText = (') &&
    readonlyForm.includes('cell?.fillForm?.labelText') &&
    readonlyForm.includes('cell?.fillForm?.label') &&
    readonlyForm.includes('cell?.edhrCellRule?.label'),
  'checkbox label 必须兼容识别 JSON 的 labelText 与持久化规则 label，而不是表名或坐标硬编码。'
)
assert(
  readonlyForm.includes('const normalizeReadonlyCheckboxLabelText = ('),
  'checkbox label 渲染前必须统一清理 Word checkbox 符号和尾随下划线。'
)
assert(
  readonlyForm.includes('.edhr-template-sheet__checkbox-label {'),
  'checkbox label 必须有独立样式，保证文字显示在 checkbox 旁边且可换行。'
)
assert(
  readonlyForm.match(/v-if="cell\.checkboxState !== null"/g)?.length >= 2,
  '普通预览和自适应预览两个渲染分支都必须覆盖 checkbox label。'
)

console.log('PASS: eDHR readonly checkbox label static contract')
