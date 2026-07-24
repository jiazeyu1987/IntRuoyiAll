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

const pageLayoutStyle = pickRule('.browser-page-layout', '文件查阅页面必须声明主布局样式。')
assert.match(
  pageLayoutStyle,
  /position:\s*relative/,
  '主布局应成为左右栏定位上下文，避免左侧目录高度把右侧列表下方撑出空白。'
)
assert.match(
  pageLayoutStyle,
  /display:\s*block/,
  '主布局应按右侧列表自然高度收敛，不应继续让左侧目录参与行高计算。'
)
assert.match(
  pageLayoutStyle,
  /align-items:\s*flex-start/,
  '主布局应保持顶部对齐。'
)

const leftColStyle = pickRule(
  '.browser-page-layout > :deep(.el-col:first-child)',
  '左侧目录列必须单独定位，不能继续撑高整个文件查阅行。'
)
assert.match(leftColStyle, /position:\s*absolute/, '左侧目录列应脱离主布局高度计算。')
assert.match(leftColStyle, /top:\s*0/, '左侧目录列应贴齐主布局顶部。')
assert.match(leftColStyle, /left:\s*0/, '左侧目录列应保持在左侧。')
assert.match(leftColStyle, /width:\s*25%/, '左侧目录列宽应保持原 6/24 布局。')
assert.match(leftColStyle, /max-width:\s*25%/, '左侧目录列最大宽度应保持原 6/24 布局。')

const rightColStyle = pickRule(
  '.browser-page-layout > :deep(.el-col:last-child)',
  '右侧列表列必须接管主布局高度，分页下方不应被左侧目录撑出空白。'
)
assert.match(rightColStyle, /margin-left:\s*25%/, '右侧列表列应保留左侧目录占位。')
assert.match(rightColStyle, /width:\s*75%/, '右侧列表列宽应保持原 18/24 布局。')
assert.match(rightColStyle, /max-width:\s*75%/, '右侧列表列最大宽度应保持原 18/24 布局。')

const listWrapStyle = pickRule('.browser-list-wrap', '右侧列表容器必须单独声明高度策略。')
assert.doesNotMatch(
  listWrapStyle,
  /display:\s*flex/,
  '右侧列表卡片不得强制 flex 填充，否则文件行与分页之间会出现表格内部空白。'
)
assert.match(
  listWrapStyle,
  /height:\s*auto/,
  '右侧列表容器必须按表格和分页内容自适应高度。'
)
assert.doesNotMatch(
  listWrapStyle,
  /min-height:\s*calc\(100vh\s*-\s*120px\)/,
  '右侧列表卡片不得强制填满视口，否则分页上方会出现表格内部空白。'
)
assert.match(
  listWrapStyle,
  /margin-bottom:\s*0\s*!important/,
  '右侧列表卡片不应保留 ContentWrap 默认底部间距。'
)

const listTemplateStyle = pickRule(
  '.browser-list-template',
  '标准列表模板实例必须参与纵向填充。'
)
assert.doesNotMatch(listTemplateStyle, /flex:\s*1/, '标准列表模板实例不得填充剩余视口高度。')

const tableShellStyle = pickRule(
  '.browser-list-template :deep(.unified-list-template__table-shell)',
  '文件查阅必须让标准列表模板的表格区域填充分页上方空间。'
)
assert.doesNotMatch(tableShellStyle, /flex:\s*1/, '表格区域不得填充分页上方空间。')

console.log('PASS: dcc browser bottom whitespace collapse static contract')
