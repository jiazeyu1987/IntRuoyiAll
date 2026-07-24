const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(
  root,
  'src/views/dcc/controlled-file/basic-data/file-type-taxonomy/index.vue'
)
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
  'DCC 文件分类必须导入标准列表模板。'
)
assert.match(
  source,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  'DCC 文件分类必须接入显示字段和列宽持久化 hook。'
)
assert.match(
  source,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  'DCC 文件分类必须接入标准快速过滤 hook。'
)

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="dcc\.fileTypeTaxonomy\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, 'DCC 文件分类必须用稳定 tableKey 接入 UnifiedListTemplate。')
const template = templateMatch[0]

assert.match(
  template,
  /class="dcc-file-type-taxonomy-list-template"/,
  'DCC 文件分类标准列表必须提供页面级样式作用域。'
)
assert.match(template, /:query-model="query"/, '标准模板必须绑定原查询模型。')
assert.match(
  template,
  /:filter-definitions="taxonomyQuickFilterDefinitions"/,
  '标准模板必须绑定 DCC 文件分类快速过滤字段。'
)
assert.match(
  template,
  /:show-quick-filter-label="false"/,
  'DCC 文件分类应隐藏标准模板快速过滤文案。'
)
assert.match(
  template,
  /:quick-filter-state="taxonomyQuickFilter\.state"/,
  '标准模板必须绑定快速过滤状态。'
)
assert.match(
  template,
  /@update:quick-filter-state="taxonomyQuickFilter\.updateState"/,
  '标准模板必须同步快速过滤状态。'
)
assert.match(
  template,
  /@quick-filter-query="taxonomyQuickFilter\.applyQuickFilter"/,
  '标准模板必须触发快速过滤查询。'
)
assert.match(template, /:columns="taxonomyColumns"/, '标准模板必须绑定显示字段配置。')
assert.match(
  template,
  /@column-change="saveTaxonomyColumnConfig"/,
  '显示字段变化必须自动保存。'
)
assert.match(template, /v-model:page="query\.pageNo"/, '模板分页必须绑定 pageNo。')
assert.match(template, /v-model:limit="query\.pageSize"/, '模板分页必须绑定 pageSize。')
assert.match(template, /@pagination="handlePagination"/, '模板分页必须保留分页事件。')
assert.doesNotMatch(
  template,
  /<template\s+#extra-filters\b[\s\S]*?<\/template>/,
  '截图红框内层级和启用状态筛选已要求移除。'
)
assert.match(
  template,
  /<template #actions>[\s\S]*openForm\('create'\)[\s\S]*新增一级[\s\S]*<\/template>/,
  '新增一级操作必须保留在标准模板操作区。'
)
assert.doesNotMatch(
  template,
  /<el-button[\s\S]*?@click="loadData"[\s\S]*?刷新[\s\S]*?<\/el-button>/,
  '截图红框内刷新按钮已要求移除。'
)
assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-key="dcc\.fileTypeTaxonomy\.main"[\s\S]*:data="paginatedTreeRows"[\s\S]*:tree-props="taxonomyTreeProps"[\s\S]*@header-dragend="handleTaxonomyHeaderDragend"/,
  'DCC 文件分类表格必须在标准模板中保留树形数据和列宽拖拽持久化。'
)

for (const field of ['name', 'code', 'levelNo', 'taxonomyPath', 'active', 'sort', 'remark', 'actions']) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到 DCC 文件分类列配置。`)
}

assert.match(
  source,
  /\{\s*key:\s*'name'[\s\S]*hideable:\s*false/,
  '分类名称作为树形主列必须不可隐藏。'
)
assert.match(
  source,
  /\{\s*key:\s*'actions'[\s\S]*hideable:\s*false[\s\S]*business:\s*false/,
  '操作列必须不可隐藏，并标记为非业务字段。'
)

for (const field of ['code', 'levelNo', 'taxonomyPath', 'active', 'sort', 'remark']) {
  assert.match(
    template,
    new RegExp(`isTaxonomyColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}
assert.doesNotMatch(
  template,
  /isTaxonomyColumnVisible\('name'\)/,
  '分类名称树形主列不得被列配置隐藏。'
)
assert.doesNotMatch(
  template,
  /isTaxonomyColumnVisible\('actions'\)/,
  '操作列不得被列配置隐藏。'
)

assert.match(
  source,
  /const taxonomyQuickFilterDefinitions: TableQuickFilterDefinition\[\] = \[/,
  '必须定义 DCC 文件分类快速过滤字段。'
)
assert.match(
  source,
  /key:\s*'keyword'[\s\S]*label:\s*'关键词'[\s\S]*type:\s*'text'[\s\S]*queryParamKey:\s*'keyword'/,
  '快速过滤必须承接原关键词查询。'
)
assert.doesNotMatch(
  source,
  /levelMatched|activeMatched|query\.levelNo|query\.active/,
  '层级和启用状态筛选移除后，不应保留不可达的筛选状态。'
)
assert.match(
  source,
  /useTableQuickFilter\([\s\S]*'dcc\.fileTypeTaxonomy\.main'[\s\S]*taxonomyQuickFilterDefinitions[\s\S]*query[\s\S]*handleQuery/,
  '快速过滤必须用标准 hook 连接原查询模型和查询处理。'
)
assert.match(
  source,
  /useUserTableColumns\('dcc\.fileTypeTaxonomy\.main', taxonomyDefaultColumns\)/,
  '显示字段必须使用稳定 tableKey 持久化。'
)
assert.match(
  source,
  /const paginatedTreeRows = computed\(\(\) =>[\s\S]*filteredTreeRows\.value\.slice/,
  '标准模板分页必须使用客户端分页后的树形行。'
)

const mainListEnd = source.indexOf('<Dialog')
const mainListSource = source.slice(0, mainListEnd)
assert.doesNotMatch(
  mainListSource,
  /<div class="dcc-taxonomy-filter">/,
  '旧独立筛选栏必须移除。'
)
assert.doesNotMatch(
  mainListSource,
  /<Pagination[\s\S]*\/>/,
  'DCC 文件分类分页必须由标准列表模板承载。'
)

console.log('PASS: dcc file type taxonomy unified list template static contract')
