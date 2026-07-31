const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const executionPage = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue'),
  'utf8'
)

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)

const topbarMatch = executionPage.match(
  /<div class="edhr-fill-workspace__assist-topbar">[\s\S]*?\r?\n\s*<\/div>\r?\n\r?\n\s*<el-dialog/
)
assert.ok(topbarMatch, '辅助填写顶部栏模板必须存在。')
const topbar = topbarMatch[0]

assertIncludes(
  topbar,
  'edhr-fill-workspace__assist-context-strip',
  '顶部 3 个切换按钮必须包在左侧 2/3 上下文区域。'
)
assertIncludes(
  topbar,
  'edhr-fill-workspace__assist-action-reserve',
  '顶部栏右侧必须保留 1/3 操作按钮区域。'
)
assert.match(
  topbar,
  /edhr-fill-workspace__assist-context-strip[\s\S]*?edhr-fill-workspace__assist-switch-grid[\s\S]*?任务 \/ 批次[\s\S]*?工序[\s\S]*?填写人[\s\S]*?edhr-fill-workspace__assist-action-reserve/,
  '任务/批次、工序、填写人 3 个按钮必须位于预留区之前的左侧上下文区域。'
)

const assertStyleContains = (selector, token, message) => {
  const index = executionPage.indexOf(selector)
  assert.notEqual(index, -1, `${selector} 样式必须存在。`)
  const styleBlock = executionPage.slice(index, executionPage.indexOf('}', index) + 1)
  assertIncludes(styleBlock, token, message)
}

assertStyleContains(
  '.edhr-fill-workspace__assist-topbar',
  'grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);',
  '顶部栏必须使用 2fr + 1fr 布局，让 3 个按钮只占 2/3 宽度。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-context-strip',
  'min-width: 0;',
  '左侧上下文区域必须允许内容在 2/3 宽度内收敛。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-action-reserve',
  'min-height: 60px;',
  '右侧预留区域必须有稳定高度，后续按钮进入时不会挤压顶部栏。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-action-reserve',
  'justify-content: flex-end;',
  '右侧预留区域必须按未来按钮操作区靠右对齐。'
)

console.log('PASS: eDHR assist topbar reserves right action area')
