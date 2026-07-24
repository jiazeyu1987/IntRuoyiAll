const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const readonlyForm = read('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')

const assertIncludes = (content, token, message) => {
  assert.ok(content.includes(token), message)
}

const assertMatches = (content, pattern, message) => {
  assert.ok(pattern.test(content), message)
}

assertIncludes(
  readonlyForm,
  'fillablePlaceholder?: string',
  '只读模板单元格模型必须携带空填写格占位字段'
)
assertIncludes(
  readonlyForm,
  'const fillablePlaceholder = resolveFillablePlaceholder(rawCell, text)',
  '只读模板渲染行必须从 fillForm 生成可见占位'
)
assertIncludes(
  readonlyForm,
  'v-if="cell.fillablePlaceholder"',
  '只读模板必须只在空填写格上显示占位'
)
assertIncludes(
  readonlyForm,
  'class="edhr-template-sheet__fillable-placeholder"',
  '只读模板必须给空填写格使用独立占位样式'
)
assertMatches(
  readonlyForm,
  /const resolveFillablePlaceholder = \(cell: RawLayoutCell \| undefined, renderedText: string\) => \{[\s\S]*?if \(!cell\?\.fillForm \|\| renderedText\.trim\(\)\) return ''[\s\S]*?placeholderText = placeholderText === '请填写' \? '\?' : placeholderText[\s\S]*?return placeholderText \|\| '\?'[\s\S]*?\}/,
  '只读模板必须只对没有已填值的 fillForm 单元格显示“?”占位'
)
assertIncludes(
  readonlyForm,
  '.edhr-template-sheet__fillable-placeholder {',
  '只读模板必须提供空填写格占位样式'
)
assertIncludes(
  readonlyForm,
  'box-shadow: inset 0 0 0 1px rgba(15, 118, 110, 0.32);',
  '只读模板必须让 fillForm 单元格在红框加载预览中可见'
)

console.log('PASS: electronic batch record readonly fillable preview static contract')
