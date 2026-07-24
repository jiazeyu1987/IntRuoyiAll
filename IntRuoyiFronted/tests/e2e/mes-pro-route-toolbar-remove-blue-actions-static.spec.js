const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mes/pro/route/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="mes\.pro\.route\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '工艺流程列表必须继续使用标准列表模板。')
const template = templateMatch[0]

const actionsMatch = template.match(/<template #actions>([\s\S]*?)<\/template>/)
assert.ok(actionsMatch, '工艺流程列表必须保留标准模板 actions 插槽。')
const actions = actionsMatch[1]

assert.doesNotMatch(actions, /handleQuery/, '蓝框内搜索按钮必须删除。')
assert.doesNotMatch(actions, /resetQuery/, '蓝框内重置按钮必须删除。')
assert.doesNotMatch(actions, /openForm\('create'\)/, '蓝框内新增按钮必须删除。')
assert.doesNotMatch(actions, /handleMarkdownImport/, '导入 Markdown 按钮必须删除。')
assert.doesNotMatch(actions, /handleSheet1ExcelImport/, '导入 Sheet1 Excel 按钮必须删除。')
assert.doesNotMatch(actions, />\s*搜索\s*<\/el-button>/, 'actions 插槽中不应再渲染搜索按钮。')
assert.doesNotMatch(actions, />\s*重置\s*<\/el-button>/, 'actions 插槽中不应再渲染重置按钮。')
assert.doesNotMatch(actions, />\s*新增\s*<\/el-button>/, 'actions 插槽中不应再渲染新增按钮。')
assert.doesNotMatch(actions, /导入 Markdown/, 'actions 插槽中不应再渲染导入 Markdown 按钮。')
assert.doesNotMatch(actions, /导入 Sheet1 Excel/, 'actions 插槽中不应再渲染导入 Sheet1 Excel 按钮。')
assert.doesNotMatch(actions, /导入路线 Excel/, '路线 Excel 导入按钮文案必须改为“导入”。')

for (const requiredAction of ['handleRouteWorkbookExcelImport', 'handleExport']) {
  assert.match(actions, new RegExp(requiredAction), `${requiredAction} 必须保留。`)
}
assert.match(actions, />\s*<Icon[^>]*>\s*导入\s*<\/el-button>/, '路线 Excel 导入按钮必须显示为“导入”。')

assert.match(template, /:show-quick-filter-label="false"/, '快速过滤必须继续由标准模板承载。')
assert.match(template, /@quick-filter-query="routeQuickFilter\.applyQuickFilter"/, '快速过滤查询必须保留。')
assert.match(template, /:columns="routeColumns"/, '显示字段入口必须保留。')
assert.match(template, /:show-column-settings="false"/, '标准模板内置右侧显示字段入口必须关闭。')
assert.doesNotMatch(template, /@column-reset="resetRouteColumnConfig"/, '重置列入口必须删除。')
assert.match(template, /<template #extra-filters>[\s\S]*<UserTableColumnSettings[\s\S]*:show-reset="false"/, '显示字段必须移动到快速过滤右侧并隐藏重置列。')

assert.doesNotMatch(source, /const handleQuery = /, '删除搜索按钮后不应保留废弃 handleQuery。')
assert.doesNotMatch(source, /const resetQuery = /, '删除重置按钮后不应保留废弃 resetQuery。')
assert.doesNotMatch(source, /const handleMarkdownImport = /, '删除 Markdown 导入后不应保留废弃 handleMarkdownImport。')
assert.doesNotMatch(source, /const handleSheet1ExcelImport = /, '删除 Sheet1 Excel 导入后不应保留废弃 handleSheet1ExcelImport。')
assert.doesNotMatch(source, /RouteMarkdownImportForm/, '删除 Markdown 导入后不应再引用 RouteMarkdownImportForm。')
assert.doesNotMatch(source, /RouteSheet1ExcelImportForm/, '删除 Sheet1 Excel 导入后不应再引用 RouteSheet1ExcelImportForm。')
assert.doesNotMatch(source, /resetRouteColumnConfig/, '删除重置列后不应保留废弃 resetRouteColumnConfig。')

console.log('PASS: mes pro route toolbar blue actions removed static contract')
