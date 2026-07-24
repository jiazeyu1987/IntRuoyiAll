const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/dcc/controlled-file/browser/index.vue'),
  'utf8'
)

const styleMatch = source.match(/<style lang="scss" scoped>([\s\S]*?)<\/style>/)
assert.ok(styleMatch, '文件查阅页面必须保留 scoped 样式区。')
const style = styleMatch[1]

const pickRule = (selector, message) => {
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = style.match(new RegExp(`${escapedSelector}\\s*\\{[\\s\\S]*?\\n\\}`))
  assert.ok(match, message)
  return match[0]
}

const listWrapStyle = pickRule('.browser-list-wrap', '右侧列表容器必须单独声明高度策略。')
assert.match(
  listWrapStyle,
  /height:\s*auto/,
  '右侧列表容器必须按表格、分页实际内容自适应高度。'
)
assert.doesNotMatch(
  listWrapStyle,
  /min-height:\s*calc\(100vh\s*-\s*120px\)/,
  '右侧列表容器不得强制填满视口，否则文件行和分页之间会出现大块表格内部空白。'
)
assert.doesNotMatch(
  listWrapStyle,
  /display:\s*flex/,
  '右侧列表卡片不得强制 flex 填充，否则表格区域会被拉高。'
)

const listTemplateStyle = pickRule('.browser-list-template', '标准列表模板实例必须有局部样式。')
assert.doesNotMatch(
  listTemplateStyle,
  /flex:\s*1/,
  '标准列表模板实例不得填充剩余视口高度，否则分页不能紧跟文件行。'
)

const tableShellStyle = pickRule(
  '.browser-list-template :deep(.unified-list-template__table-shell)',
  '文件查阅必须声明标准列表模板表格区域样式。'
)
assert.doesNotMatch(
  tableShellStyle,
  /flex:\s*1/,
  '表格区域不得填充剩余高度，否则最后一行与分页之间会出现红框空白。'
)

console.log('PASS: dcc browser remove table inner gap static contract')
