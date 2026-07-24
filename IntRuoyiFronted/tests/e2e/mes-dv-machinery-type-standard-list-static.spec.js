const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mes/dv/machinery/type/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
  '设备类型列表必须导入标准列表模板。'
)
assert.match(
  source,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  '设备类型列表必须接入显示字段和列宽持久化 hook。'
)
assert.match(
  source,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  '设备类型列表必须接入标准快速过滤 hook。'
)

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="mes\.dv\.machineryType\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '设备类型列表必须用稳定 tableKey 接入 UnifiedListTemplate。')
const template = templateMatch[0]

assert.match(template, /:query-model="queryParams"/, '标准模板必须绑定原查询模型。')
assert.match(
  template,
  /:filter-definitions="machineryTypeQuickFilterDefinitions"/,
  '标准模板必须绑定设备类型快速过滤字段。'
)
assert.match(
  template,
  /:show-quick-filter-label="false"/,
  '设备类型列表应隐藏标准模板快速过滤文案。'
)
assert.match(
  template,
  /:quick-filter-state="machineryTypeQuickFilter\.state"/,
  '标准模板必须绑定快速过滤状态。'
)
assert.match(
  template,
  /@update:quick-filter-state="machineryTypeQuickFilter\.updateState"/,
  '标准模板必须同步快速过滤状态。'
)
assert.match(
  template,
  /@quick-filter-query="handleQuery"/,
  '标准模板必须触发设备类型查询。'
)
assert.match(template, /:columns="machineryTypeColumns"/, '标准模板必须绑定显示字段配置。')
assert.match(
  template,
  /@column-change="saveMachineryTypeColumnConfig"/,
  '显示字段变化必须自动保存。'
)
assert.match(
  template,
  /@column-reset="resetMachineryTypeColumnConfig"/,
  '显示字段重置必须接入标准列配置重置。'
)
assert.match(template, /v-model:page="queryParams\.pageNo"/, '模板分页必须绑定 pageNo。')
assert.match(template, /v-model:limit="queryParams\.pageSize"/, '模板分页必须绑定 pageSize。')
assert.match(template, /@pagination="handlePagination"/, '模板分页必须由页面处理分页事件。')

assert.match(
  template,
  /<template #actions>[\s\S]*machineryTypeQuickFilter\.resetQuickFilter[\s\S]*重置[\s\S]*openForm\('create'\)[\s\S]*新增[\s\S]*toggleExpandAll[\s\S]*展开\/折叠[\s\S]*<\/template>/,
  '设备类型列表必须在标准模板操作区保留重置、新增和展开/折叠。'
)

assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-column-explicit[\s\S]*data-user-table-key="mes\.dv\.machineryType\.main"[\s\S]*:data="paginatedMachineryTypeRows"[\s\S]*row-key="id"[\s\S]*:default-expand-all="isExpandAll"[\s\S]*@header-dragend="handleMachineryTypeHeaderDragend"/,
  '设备类型表格必须在标准模板中保留树形数据、展开状态和列宽拖拽持久化。'
)

for (const field of ['code', 'name', 'status', 'sort', 'createTime', 'operation']) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到设备类型列配置。`)
}

assert.match(
  source,
  /\{\s*key:\s*'name'[\s\S]*hideable:\s*false/,
  '设备类型名称作为树形主列必须不可隐藏。'
)
assert.match(
  source,
  /\{\s*key:\s*'operation'[\s\S]*hideable:\s*false[\s\S]*business:\s*false/,
  '操作列必须不可隐藏，并标记为非业务字段。'
)

for (const field of ['code', 'status', 'sort', 'createTime']) {
  assert.match(
    template,
    new RegExp(`isMachineryTypeColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}
assert.doesNotMatch(
  template,
  /isMachineryTypeColumnVisible\('name'\)/,
  '设备类型名称树形主列不得被列配置隐藏。'
)
assert.doesNotMatch(
  template,
  /isMachineryTypeColumnVisible\('operation'\)/,
  '操作列不得被列配置隐藏。'
)

assert.match(
  source,
  /const machineryTypeQuickFilterDefinitions = computed<TableQuickFilterDefinition\[\]>\(\(\) => \[/,
  '必须定义设备类型快速过滤字段。'
)
assert.match(
  source,
  /key:\s*'name'[\s\S]*label:\s*'类型名称'[\s\S]*type:\s*'text'[\s\S]*queryParamKey:\s*'name'/,
  '快速过滤必须承接原类型名称查询。'
)
assert.match(
  source,
  /key:\s*'status'[\s\S]*label:\s*'状态'[\s\S]*type:\s*'select'[\s\S]*queryParamKey:\s*'status'/,
  '快速过滤必须承接原状态查询。'
)
assert.match(
  source,
  /useTableQuickFilter\([\s\S]*MACHINERY_TYPE_TABLE_KEY[\s\S]*machineryTypeQuickFilterDefinitions[\s\S]*queryParams[\s\S]*getList/,
  '快速过滤必须用标准 hook 连接原查询模型和查询处理。'
)
assert.match(
  source,
  /useUserTableColumns\(MACHINERY_TYPE_TABLE_KEY, machineryTypeDefaultColumns\)/,
  '显示字段必须使用稳定 tableKey 持久化。'
)
assert.match(
  source,
  /const paginatedMachineryTypeRows = computed\(\(\) =>[\s\S]*machineryTypeTreeRows\.value\.slice/,
  '标准模板分页必须使用客户端分页后的树形行。'
)

const contentBeforeForm = source.slice(0, source.indexOf('<MachineryTypeForm'))
assert.doesNotMatch(
  contentBeforeForm,
  /<el-form[\s\S]*class="-mb-15px"/,
  '旧独立查询表单必须移除。'
)
assert.doesNotMatch(
  contentBeforeForm,
  /<ContentWrap>\s*<!-- 列表 -->/,
  '旧独立列表容器必须交给标准列表模板管理。'
)

console.log('PASS: MES DV machinery type standard list template static contract')
