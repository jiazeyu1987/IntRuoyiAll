const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const executionPage = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue'),
  'utf8'
)

const cssRule = (selector) => {
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = executionPage.match(new RegExp(`${escapedSelector}\\s*\\{[\\s\\S]*?\\n\\}`))
  assert.ok(match, `Missing CSS rule: ${selector}`)
  return match[0]
}

const assistRowRule = cssRule('.edhr-fill-workspace__assist-row')
const assistGridRowRule = cssRule('.edhr-fill-workspace__assist-grid .edhr-fill-workspace__assist-row')
const assistControlRule = cssRule('.edhr-fill-workspace__assist-control')

assert.match(
  assistRowRule,
  /min-height:\s*59px/,
  '辅助填写普通列表字段行高度必须缩减到原 74px 的约 80%。'
)
assert.match(
  assistGridRowRule,
  /min-height:\s*94px/,
  '辅助填写网格卡片高度必须缩减到原 118px 的约 80%。'
)
assert.match(
  assistGridRowRule,
  /padding:\s*8px/,
  '辅助填写网格卡片必须减少内边距，避免输入框增高后撑回旧高度。'
)
assert.match(
  assistControlRule,
  /--edhr-assist-control-height:\s*48px/,
  '辅助填写卡片内单行输入控件高度必须从 32px 提升 50% 到 48px。'
)

for (const selector of [
  '.edhr-fill-workspace__assist-control :deep(.el-input__wrapper)',
  '.edhr-fill-workspace__assist-control :deep(.el-select__wrapper)',
  '.edhr-fill-workspace__assist-control :deep(.el-input-number)'
]) {
  assert.match(
    cssRule(selector),
    /height:\s*var\(--edhr-assist-control-height\)/,
    `${selector} 必须使用统一的辅助填写控件高度。`
  )
}

assert.match(
  cssRule('.edhr-fill-workspace__assist-control :deep(.el-date-editor.el-input)'),
  /height:\s*var\(--edhr-assist-control-height\)/,
  '日期输入必须跟随统一的辅助填写控件高度。'
)
assert.match(
  cssRule('.edhr-fill-workspace__assist-control :deep(.el-textarea__inner)'),
  /min-height:\s*var\(--edhr-assist-control-height\)/,
  '多行输入不能低于统一的辅助填写控件高度。'
)
assert.match(
  cssRule('.edhr-fill-workspace__assist-typed-input'),
  /align-items:\s*center/,
  '带单位的数字输入增高后单位必须垂直居中。'
)
assert.match(
  cssRule('.edhr-fill-workspace__choice-group'),
  /min-height:\s*var\(--edhr-assist-control-height\)/,
  '互斥选项组必须跟随统一的辅助填写控件高度。'
)

console.log('PASS: eDHR assist card density static contract')
