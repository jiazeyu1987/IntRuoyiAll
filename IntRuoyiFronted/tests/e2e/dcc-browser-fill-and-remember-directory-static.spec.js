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

const pageLayoutMatch = style.match(/\.browser-page-layout\s*\{[\s\S]*?\n\}/)
assert.ok(pageLayoutMatch, '文件查阅页面必须声明主布局样式。')
const pageLayoutStyle = pageLayoutMatch[0]

assert.match(
  pageLayoutStyle,
  /height:\s*calc\(100vh\s*-\s*120px\)/,
  '主布局应保持视口内固定高度，配合内部滚动固定表头和分页。'
)
assert.match(
  pageLayoutStyle,
  /min-height:\s*520px/,
  '主布局应保留紧凑最小高度，避免文件查阅操作面板过矮。'
)
assert.doesNotMatch(
  pageLayoutStyle,
  /min-height:\s*640px/,
  '主布局不得使用过高固定最小高度，否则数据少时列表下方会保留空白。'
)
assert.match(
  pageLayoutStyle,
  /align-items:\s*stretch/,
  '主布局应拉伸左右工作区，保持目录和列表底部对齐。'
)

const directoryWrapMatch = style.match(/\.browser-directory-wrap\s*\{[\s\S]*?\n\}/)
assert.ok(directoryWrapMatch, '左侧目录容器必须单独声明高度约束。')
assert.match(
  directoryWrapMatch[0],
  /height:\s*100%/,
  '左侧目录应跟随主布局高度，并在目录区域内部滚动，避免目录树撑开页面。'
)

const listWrapMatch = style.match(/\.browser-list-wrap\s*\{[\s\S]*?\n\}/)
assert.ok(listWrapMatch, '右侧列表容器必须单独声明高度策略。')
assert.match(
  listWrapMatch[0],
  /height:\s*100%/,
  '右侧列表容器必须占满主布局高度，并由表格正文承担内部滚动。'
)

assert.match(
  source,
  /const mergeBrowserRouteQueryWithRememberedDirectory = \(\s*rememberedState\?: DccBrowserRememberedState\s*\)/,
  '文件查阅必须提供路由查询与上次目录的合并逻辑。'
)

const restoreInitialMatch = source.match(
  /const restoreBrowserInitialRouteState = async \(\) => \{[\s\S]*?\n\}/
)
assert.ok(restoreInitialMatch, '文件查阅必须有初始化路由恢复逻辑。')
const restoreInitialSource = restoreInitialMatch[0]

assert.match(
  restoreInitialSource,
  /const rememberedState = readBrowserRememberedState\(\)/,
  '初始化时必须先读取上次查看目录缓存。'
)
assert.match(
  restoreInitialSource,
  /mergeBrowserRouteStateWithRememberedDirectory\(rememberedState\)/,
  '即使当前路由已有分页等查询参数，也必须补回上次查看目录。'
)
assert.match(
  source,
  /directoryTreeRef\.value\?\.setCurrentKey\(selectedDirectoryId\.value\)/,
  '恢复上次目录后必须重新高亮目录树当前节点。'
)

console.log('PASS: dcc browser fill and remember directory static contract')
