const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/dcc/controlled-file/browser/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/,
  '文件查阅列表必须导入标准列表模板。'
)
assert.match(
  source,
  /import \{ isSearchModelInputEmpty \} from '@\/utils\/search'/,
  '文件查阅列表接入标准模板后必须使用模型字段判断空查询。'
)
assert.match(
  source,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  '文件查阅列表必须保留显示字段和列宽持久化 hook。'
)
assert.match(
  source,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  '文件查阅列表必须保留快速过滤 hook。'
)

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?:table-key="DCC_BROWSER_COLUMN_TABLE_KEY"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '文件查阅列表必须使用稳定 tableKey 接入标准列表模板。')
const template = templateMatch[0]

assert.match(
  template,
  /class="browser-list-template"/,
  '文件查阅标准列表必须提供页面级样式作用域。'
)
assert.match(template, /:query-model="queryParams"/, '标准模板必须绑定原查询参数。')
assert.match(
  template,
  /:filter-definitions="dccBrowserQuickFilterDefinitions"/,
  '标准模板必须绑定文件查阅快速过滤定义。'
)
assert.match(
  template,
  /:quick-filter-state="dccBrowserQuickFilter\.state"/,
  '标准模板必须绑定快速过滤状态。'
)
assert.match(
  template,
  /@quick-filter-query="dccBrowserQuickFilter\.applyQuickFilter"/,
  '标准模板必须触发快速过滤查询。'
)
assert.match(template, /:columns="dccBrowserColumns"/, '标准模板必须绑定显示字段配置。')
assert.match(
  template,
  /@column-change="saveDccBrowserColumnConfig"/,
  '显示字段变化必须自动保存。'
)
assert.match(
  template,
  /@column-reset="resetDccBrowserColumnConfig"/,
  '显示字段重置必须复用原列配置重置逻辑。'
)
assert.match(template, /v-model:page="queryParams\.pageNo"/, '模板分页必须绑定 pageNo。')
assert.match(template, /v-model:limit="queryParams\.pageSize"/, '模板分页必须绑定 pageSize。')
assert.match(template, /@pagination="handlePagination"/, '模板分页必须复用原分页处理。')

assert.match(
  template,
  /<template #extra-filters>[\s\S]*v-model="searchScope"[\s\S]*:options="browserSearchScopeOptions"[\s\S]*@change="handleSearchScopeChange"/,
  '当前目录/全域切换必须保留在标准模板附加筛选区。'
)
assert.doesNotMatch(
  template,
  /<template #extra-filters>[\s\S]*label="状态"[\s\S]*v-model="queryParams\.status"[\s\S]*<\/template>/,
  '文件查阅标准模板附加筛选区不得继续显示状态筛选。'
)
assert.doesNotMatch(
  template,
  /<template #extra-filters>[\s\S]*label="类别"[\s\S]*v-model="queryParams\.categoryId"[\s\S]*<\/template>/,
  '文件查阅标准模板附加筛选区不得继续显示类别筛选。'
)
assert.match(template, /:show-column-reset="false"/, '文件查阅页必须隐藏重置列入口。')
assert.match(
  template,
  /<template #actions>[\s\S]*handleMetadataExport[\s\S]*openMetadataImportDialog[\s\S]*handleRecognitionRecordExport[\s\S]*handleRecognitionMigrationExport[\s\S]*openRecognitionMigrationImportDialog[\s\S]*openBatchRecognitionDialog/,
  '导入导出和识别操作必须保留在标准模板操作区。'
)
assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*:data-user-table-key="DCC_BROWSER_COLUMN_TABLE_KEY"[\s\S]*:empty-text="tableEmptyText"[\s\S]*@header-dragend="handleDccBrowserHeaderDragend"/,
  '文件查阅表格必须保留表格 key、空状态和列宽拖拽持久化。'
)

for (const field of [
  'fileName',
  'fileNumber',
  'directory',
  'productName',
  'category',
  'versionSummary',
  'remark',
  'operation'
]) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到文件查阅列配置。`)
  assert.match(
    template,
    new RegExp(`isDccBrowserColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}

assert.match(
  template,
  /openPreview\(getSelectedVersion\(row\)\.id\)[\s\S]*openDownload\(getSelectedVersion\(row\)\.id\)[\s\S]*handleBrowserRowCommand\(command, row\)/,
  '预览、下载和更多行操作必须保留。'
)

const mainListEnd = source.indexOf('<el-dialog')
const mainListSource = source.slice(0, mainListEnd)
assert.doesNotMatch(
  mainListSource,
  /<el-form[\s\S]*class="-mb-15px/,
  '文件查阅主列表必须移除旧独立搜索表单。'
)
assert.doesNotMatch(
  mainListSource,
  /<Pagination[\s\S]*@pagination="handlePagination"[\s\S]*\/>/,
  '文件查阅主列表分页必须由标准列表模板承载。'
)
assert.doesNotMatch(
  source,
  /import UserTableColumnSettings from '@\/components\/UserTableColumnSettings\/index.vue'/,
  '文件查阅页面不得继续直接渲染显示字段入口，应由标准模板承载。'
)
assert.doesNotMatch(
  source,
  /import TableQuickFilter from '@\/components\/TableQuickFilter\/index.vue'/,
  '文件查阅页面不得继续直接渲染快速过滤组件，应由标准模板承载。'
)
assert.doesNotMatch(source, /queryFormRef/, '接入标准模板后不应依赖旧表单 ref。')

console.log('PASS: dcc browser unified list template static contract')
