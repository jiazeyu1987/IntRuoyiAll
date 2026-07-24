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

assert.match(
  source,
  /import UserTableColumnSettings from '@\/components\/UserTableColumnSettings\/index.vue'/,
  '工艺流程列表必须直接导入显示字段组件，才能移动到快速过滤旁。'
)
assert.match(
  template,
  /:show-column-settings="false"/,
  '标准模板内置右侧显示字段入口必须关闭，避免继续出现在右侧。'
)
assert.doesNotMatch(template, /@column-reset="resetRouteColumnConfig"/, '删除重置列后不应绑定列重置事件。')
assert.doesNotMatch(source, /resetRouteColumnConfig/, '删除重置列后不应保留废弃 resetRouteColumnConfig。')

const extraFiltersMatch = template.match(/<template #extra-filters>([\s\S]*?)<\/template>/)
assert.ok(extraFiltersMatch, '显示字段按钮必须放入 extra-filters 插槽，位于快速过滤查询按钮右侧红框位置。')
const extraFilters = extraFiltersMatch[1]

assert.match(extraFilters, /<UserTableColumnSettings/, 'extra-filters 插槽必须渲染显示字段按钮。')
assert.match(extraFilters, /:columns="routeColumns"/, '显示字段按钮必须继续绑定路线列配置。')
assert.match(extraFilters, /:saving="routeColumnSaving"/, '显示字段按钮必须继续绑定保存状态。')
assert.match(extraFilters, /:show-reset="false"/, '显示字段按钮必须显式隐藏重置列。')
assert.match(extraFilters, /@change="saveRouteColumnConfig"/, '显示字段变化必须继续自动保存。')
assert.doesNotMatch(extraFilters, /@reset=/, '显示字段移动后不应提供重置列事件。')

const extraFiltersIndex = template.indexOf('<template #extra-filters>')
const actionsIndex = template.indexOf('<template #actions>')
assert.ok(extraFiltersIndex > -1 && actionsIndex > -1 && extraFiltersIndex < actionsIndex, '显示字段必须位于导入导出操作左侧。')

const actionsMatch = template.match(/<template #actions>([\s\S]*?)<\/template>/)
assert.ok(actionsMatch, '导入导出操作必须继续位于 actions 插槽。')
const actions = actionsMatch[1]
assert.doesNotMatch(actions, /UserTableColumnSettings/, 'actions 插槽不应再渲染显示字段按钮。')
assert.match(actions, /handleRouteWorkbookExcelImport/, '导入按钮必须保留在右侧操作区。')
assert.match(actions, /handleExport/, '导出按钮必须保留在右侧操作区。')

console.log('PASS: mes pro route display fields moved left and reset removed static contract')
