const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const executionPage = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue'),
  'utf8'
)

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

const processMenuMatch = executionPage.match(
  /<div[\s\S]{0,300}data-assist-switch-menu="process"[\s\S]*?(?=\r?\n\s*<div\s+v-else\s+class="edhr-fill-workspace__assist-switch-menu")/
)
assert.ok(processMenuMatch, '必须保留工序切换菜单模板。')
const processMenu = processMenuMatch[0]

assertIncludes(
  executionPage,
  'const assistProcessSwitchOrderCode = computed',
  '工序切换弹框必须从当前执行页上下文解析订单号。'
)
assertIncludes(
  processMenu,
  'edhr-fill-workspace__assist-switch-order-code',
  '工序切换弹框顶部中间位置必须显示订单号。'
)
assertIncludes(
  processMenu,
  '{{ assistProcessSwitchOrderCode }}',
  '订单号展示必须绑定当前执行页订单号，不得硬编码。'
)
assertNotIncludes(
  processMenu,
  'resolveAssistProcessSwitchItemSecondaryLabel(item)',
  '工序卡片红框位置的序号、表单项、前置说明不应继续显示。'
)
assertNotIncludes(
  processMenu,
  'edhr-fill-workspace__assist-switch-option-sub',
  '工序卡片内不得继续渲染二级说明行。'
)

const assertStyleContains = (selector, token, message) => {
  const index = executionPage.indexOf(selector)
  assert.notEqual(index, -1, `${selector} 样式必须存在。`)
  const styleBlock = executionPage.slice(index, executionPage.indexOf('}', index) + 1)
  assertIncludes(styleBlock, token, message)
}

assertStyleContains(
  '.edhr-fill-workspace__assist-switch-process-grid',
  'grid-auto-rows: minmax(86px, auto);',
  '工序卡片行高必须从 64px 增加到更高的 86px。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-process-card',
  'min-height: 86px;',
  '每个工序卡片必须更高，给大字体留出空间。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-process-card',
  'padding: 16px 18px;',
  '工序卡片内边距必须增加，避免大字体拥挤。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-process-card .edhr-fill-workspace__assist-switch-option-main',
  'font-size: 16px;',
  '工序卡片主标题字体必须增大。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-process-card :deep(.el-tag)',
  'font-size: 14px;',
  '工序卡片状态标签字体必须增大。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-switch-order-code',
  'justify-self: center;',
  '订单号必须位于弹框头部中间位置。'
)

console.log('PASS: edhr assist process switch card order static contract')
