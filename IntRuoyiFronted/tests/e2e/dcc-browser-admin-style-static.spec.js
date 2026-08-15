const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const appStoreSource = readSource('src/store/modules/app.ts')
const browserSource = readSource('src/views/dcc/controlled-file/browser/index.vue')
const rootCssSource = readSource('src/styles/var.css')

assert.match(
  appStoreSource,
  /const\s+DEFAULT_APP_THEME:\s*Required<ThemeTypes>\s*=\s*\{[\s\S]*elColorPrimary:\s*'#009688'[\s\S]*leftMenuBgColor:\s*'#fff'[\s\S]*leftMenuTextColor:\s*'#333'[\s\S]*leftMenuTextActiveColor:\s*'var\(--el-color-primary\)'[\s\S]*topHeaderBgColor:\s*'#fff'/,
  '应用默认主题必须固定为本地管理员截图的绿色主色和白色菜单。'
)

assert.doesNotMatch(
  appStoreSource,
  /theme:\s*wsCache\.get\(CACHE_KEY\.THEME\)\s*\|\|/,
  '应用启动时不得继续用旧缓存主题覆盖统一管理员默认主题。'
)

assert.match(
  appStoreSource,
  /const\s+resetPersistedVisualPreferences\s*=\s*\(\)\s*=>\s*\{[\s\S]*wsCache\.delete\(CACHE_KEY\.LAYOUT\)[\s\S]*wsCache\.delete\(CACHE_KEY\.THEME\)[\s\S]*wsCache\.delete\(CACHE_KEY\.IS_DARK\)[\s\S]*\}/,
  '应用启动必须清理旧浏览器里的布局、主题和深色模式缓存，避免不同浏览器显示不同主题。'
)

assert.match(
  appStoreSource,
  /state:\s*\(\):\s*AppState\s*=>\s*\{[\s\S]*resetPersistedVisualPreferences\(\)/,
  '应用初始化状态时必须先清理旧视觉偏好缓存。'
)

assert.doesNotMatch(
  appStoreSource,
  /wsCache\.set\(CACHE_KEY\.(?:THEME|LAYOUT)/,
  '统一管理员样式后，不得再持久化主题或布局偏好导致不同浏览器样式漂移。'
)

const projectCodeSource = readSource(
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
)
assert.match(
  projectCodeSource,
  /const projectCodeQuickFilterDefinitions: TableQuickFilterDefinition\[\] = \[\s*\{\s*key:\s*'docControlNo'[\s\S]*?label:\s*'文控'[\s\S]*?placeholder:\s*'请输入文控'/,
  'DCC项目代码页快速过滤默认字段必须固定为文控，避免不同浏览器保留旧字段造成首屏不一致。'
)

assert.match(rootCssSource, /--el-color-primary:\s*#009688;/, '首屏 CSS 默认主色必须是管理员绿色。')
assert.match(rootCssSource, /--left-menu-bg-color:\s*#fff;/, '首屏 CSS 默认左侧菜单必须是白底。')
assert.match(rootCssSource, /--left-menu-bg-light-color:\s*#fff;/, '首屏 CSS 默认左侧菜单浅色背景必须是白底。')
assert.match(
  rootCssSource,
  /--left-menu-bg-active-color:\s*RGBA\(0,\s*150,\s*136,\s*0\.1\);/,
  '首屏 CSS 默认左侧菜单激活背景必须是管理员浅绿色。'
)
assert.match(rootCssSource, /--left-menu-text-color:\s*#333;/, '首屏 CSS 默认菜单文字必须是深色。')
assert.match(
  rootCssSource,
  /--left-menu-text-active-color:\s*var\(--el-color-primary\);/,
  '首屏 CSS 默认菜单激活文字必须跟随管理员绿色。'
)
assert.match(
  appStoreSource,
  /logoTitleTextColor:\s*'#033886'/,
  '默认 logo 标题颜色必须显式固定为 INT MEDICAL 蓝，避免 router-link visited/unvisited 在不同浏览器显示漂移。'
)
assert.match(
  rootCssSource,
  /--logo-title-text-color:\s*#033886;/,
  '首屏 CSS 默认 logo 标题颜色必须显式固定为 INT MEDICAL 蓝。'
)
assert.match(
  readSource('src/layout/components/Setting/src/Setting.vue'),
  /logoTitleTextColor:\s*isDarkColor\s*\?\s*'#fff'\s*:\s*'#033886'/,
  '设置面板切换白色菜单主题时不得把 logo 标题颜色重置为继承链接色。'
)
assert.doesNotMatch(rootCssSource, /--left-menu-bg-color:\s*#001529;/, '首屏 CSS 不得保留旧深色菜单背景。')

const defaultColumnsMatch = browserSource.match(
  /const dccBrowserDefaultColumns: UserTableColumnDefinition\[\] = \[([\s\S]*?)\]\s*const dccBrowserQueryInputFields/
)
assert.ok(defaultColumnsMatch, '文件查阅页必须声明默认列配置。')
const defaultColumnsBlock = defaultColumnsMatch[1]

for (const field of ['fileName', 'fileNumber', 'operation']) {
  assert.match(
    defaultColumnsBlock,
    new RegExp(`\\{[^}]*key:\\s*'${field}'[\\s\\S]*?\\}`),
    `${field} 必须保留为默认可见列。`
  )
}

for (const field of ['directory', 'productName', 'category', 'versionSummary', 'remark']) {
  assert.match(
    defaultColumnsBlock,
    new RegExp(`\\{[^}]*key:\\s*'${field}'[\\s\\S]*visible:\\s*false[\\s\\S]*?\\}`),
    `${field} 必须默认隐藏，以匹配管理员截图的三列视图。`
  )
}

assert.match(
  browserSource,
  /const DCC_BROWSER_COLUMN_TABLE_KEY = 'dcc\.controlledFile\.browser\.compactActionsV2'/,
  '文件查阅列配置必须使用管理员样式表格 key，避免旧个人列配置污染默认视图。'
)

assert.match(
  browserSource,
  /useUserTableColumns\(DCC_BROWSER_COLUMN_TABLE_KEY, dccBrowserDefaultColumns\)/,
  '文件查阅显示字段 hook 必须使用管理员样式表格 key。'
)

assert.match(
  browserSource,
  /:table-key="DCC_BROWSER_COLUMN_TABLE_KEY"/,
  '标准列表模板必须使用同一个管理员样式表格 key。'
)

assert.match(
  browserSource,
  /:data-user-table-key="DCC_BROWSER_COLUMN_TABLE_KEY"/,
  '显式表格增强标记必须使用同一个管理员样式表格 key。'
)

console.log('PASS: dcc browser admin style static contract')
