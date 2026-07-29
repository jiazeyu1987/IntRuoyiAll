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

assertIncludes(
  executionPage,
  ':width="assistSwitchDialogWidth"',
  '切换弹框宽度必须由类型控制，工序切换不能继续使用固定 680px 窄弹框。'
)
assertIncludes(
  executionPage,
  "assistSwitchDialogType.value === 'process' ? 'min(1560px, calc(100vw - 280px))' : '680px'",
  '工序切换弹框必须放大到接近页面主体红框区域，普通任务/填写人切换保持 680px。'
)
assertIncludes(
  executionPage,
  "is-process-switch-dialog': assistSwitchDialogType === 'process'",
  '工序切换弹框必须有独立 class，用于大尺寸布局和高度约束。'
)

const processMenuMatch = executionPage.match(
  /<div[\s\S]{0,300}data-assist-switch-menu="process"[\s\S]*?(?=\r?\n\s*<div\s+v-else\s+class="edhr-fill-workspace__assist-switch-menu")/
)
assert.ok(processMenuMatch, '必须保留工序切换菜单模板。')
const processMenu = processMenuMatch[0]

assertIncludes(
  processMenu,
  'edhr-fill-workspace__assist-switch-process-menu',
  '工序切换菜单必须使用专用大尺寸容器。'
)
assertIncludes(
  processMenu,
  'data-assist-switch-process-grid',
  '工序候选必须放入 grid 容器，不能直接沿用纵向列表。'
)
assertIncludes(
  processMenu,
  'edhr-fill-workspace__assist-process-card',
  '每个工序候选必须按卡片样式展示。'
)
assertNotIncludes(
  processMenu,
  '<template v-else>\r\n                          <button',
  '工序候选不能继续直接用 template + button 纵向列表渲染。'
)

const assertStyleContains = (selector, token, message) => {
  const index = executionPage.indexOf(selector)
  assert.notEqual(index, -1, `${selector} 样式必须存在。`)
  const styleBlock = executionPage.slice(index, executionPage.indexOf('}', index) + 1)
  assertIncludes(styleBlock, token, message)
}

assertStyleContains(
  ':global(.edhr-fill-workspace__assist-switch-dialog.is-process-switch-dialog .el-dialog__body)',
  'max-height: calc(100vh - 180px);',
  '工序切换弹框 body 必须接近单屏可用高度。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-switch-process-menu',
  'min-height: min(72vh, 760px);',
  '工序切换菜单必须有大屏高度，避免只显示窄列表。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-switch-process-grid',
  'grid-template-columns: repeat(6, minmax(0, 1fr));',
  '工序切换首屏必须使用 6 列 grid，配合 5 行至少容纳 30 张卡片。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-switch-process-grid',
  'grid-auto-rows: minmax(64px, auto);',
  '工序卡片行高必须紧凑，确保单屏至少 5 行。'
)
assertStyleContains(
  '.edhr-fill-workspace__assist-process-card',
  'min-height: 64px;',
  '工序卡片必须保持紧凑卡片高度，满足至少 30 卡片容量。'
)

console.log('PASS: edhr assist process switch dialog grid static contract')
