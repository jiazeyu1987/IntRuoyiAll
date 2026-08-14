const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mes/pro/route/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(source, /const ROUTE_LIST_TABLE_KEY = 'mes\.pro\.route\.main[^']*'/, '工艺路线列表必须使用稳定表格 key。')

const templateMatch = source.match(/<UnifiedListTemplate[\s\S]*?<\/UnifiedListTemplate>/)
assert.ok(templateMatch, '工艺路线列表必须继续使用标准列表模板。')
const template = templateMatch[0]

const extraFiltersMatch = template.match(/<template #extra-filters>([\s\S]*?)<\/template>/)
assert.ok(extraFiltersMatch, '工艺路线列表必须保留快速过滤右侧扩展区域。')
const extraFilters = extraFiltersMatch[1]

const actionsMatch = template.match(/<template #actions>([\s\S]*?)<\/template>/)
assert.ok(actionsMatch, '工艺路线列表必须保留标准模板 actions 插槽。')
const actions = actionsMatch[1]

assert.match(extraFilters, /openForm\('create'\)/, '工艺路线列表必须提供新增入口并打开创建表单。')
assert.match(extraFilters, />\s*<Icon[^>]*>\s*新增\s*<\/el-button>/, '新增入口必须显示为“新增”。')
assert.match(
  extraFilters,
  /v-hasPermi="\['mes:pro-route:create'\]"/,
  '新增入口必须继续使用 mes:pro-route:create 权限门禁。'
)
assert.doesNotMatch(actions, /openForm\('create'\)/, '新增入口不能放回旧 actions 槽位。')
assert.match(actions, /handleRouteWorkbookExcelImport/, '路线 Excel 导入入口必须保留。')
assert.match(actions, /handleExport/, '路线导出入口必须保留。')

console.log('PASS: mes pro route create entry is visible without restoring legacy actions slot')
