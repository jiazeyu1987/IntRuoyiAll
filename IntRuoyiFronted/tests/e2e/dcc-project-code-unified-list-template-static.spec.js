const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(
  root,
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
)
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/,
  'DCC 项目代码主列表必须导入标准列表模板。'
)
assert.match(
  source,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  'DCC 项目代码主列表必须接入显示字段和列宽持久化 hook。'
)
assert.match(
  source,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  'DCC 项目代码主列表必须接入快速过滤 hook。'
)

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="dcc\.projectCode\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, 'DCC 项目代码主列表必须使用稳定 tableKey 接入标准列表模板。')
const template = templateMatch[0]

assert.match(
  template,
  /class="dcc-project-code-list-template"/,
  '项目代码标准列表必须提供页面级响应式布局作用域。'
)
assert.match(template, /:query-model="queryParams"/, '标准模板必须绑定原查询参数。')
assert.match(
  template,
  /:quick-filter-state="projectCodeQuickFilter\.state"/,
  '标准模板必须绑定项目代码快速过滤状态。'
)
assert.match(
  template,
  /@quick-filter-query="projectCodeQuickFilter\.applyQuickFilter"/,
  '标准模板必须触发快速过滤查询。'
)
assert.match(template, /:columns="projectCodeColumns"/, '标准模板必须绑定显示字段配置。')
assert.match(
  template,
  /@column-change="saveProjectCodeColumnConfig"/,
  '显示字段变化必须自动保存。'
)
assert.match(
  template,
  /:show-column-reset="false"/,
  '项目代码列表必须隐藏独立的重置列按钮。'
)
assert.match(template, /v-model:page="queryParams\.pageNo"/, '模板分页必须绑定 pageNo。')
assert.match(template, /v-model:limit="queryParams\.pageSize"/, '模板分页必须绑定 pageSize。')
assert.match(template, /@pagination="getList"/, '模板分页必须复用原列表查询。')
assert.doesNotMatch(
  template,
  /<template #extra-filters>/,
  '项目代码列表不得继续渲染类别、优先级和状态附加筛选。'
)
assert.match(
  template,
  /<template #actions>[\s\S]*openForm\('create'\)[\s\S]*openImportDialog[\s\S]*handleExport[\s\S]*handleBatchAiCategoryProjectCodes/,
  '新增、导入、导出和批量 AI 分类操作必须保留。'
)
assert.doesNotMatch(
  template,
  /<template #actions>[\s\S]*(handleQuery|resetQuery)/,
  '项目代码列表不得继续渲染重复的查询和重置按钮。'
)
assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-key="dcc\.projectCode\.main"[\s\S]*@header-dragend="handleProjectCodeHeaderDragend"/,
  '项目代码表格必须接入列宽拖拽持久化。'
)

for (const field of [
  'docControlNo',
  'primaryCode',
  'projectName',
  'projectCode',
  'category',
  'associatedFileCount',
  'updateTime',
  'actions'
]) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到项目代码列配置。`)
  assert.match(
    template,
    new RegExp(`isProjectCodeColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}

assert.match(
  source,
  /const projectCodeQuickFilterDefinitions: TableQuickFilterDefinition\[\] = \[/,
  '必须定义项目代码快速过滤字段。'
)
for (const [key, label, queryParamKey] of [
  ['docControlNo', '文控', 'keyword'],
  ['primaryCode', '主编码', 'keyword'],
  ['projectName', '项目名称', 'projectName'],
  ['projectCode', '项目代码', 'projectCode'],
  ['category', '类别', 'category']
]) {
  assert.match(
    source,
    new RegExp(
      `key:\\s*'${key}'[\\s\\S]*label:\\s*'${label}'[\\s\\S]*queryParamKey:\\s*'${queryParamKey}'`
    ),
    `快速过滤字段下拉必须展示项目代码列名：${label}。`
  )
}
assert.doesNotMatch(
  source,
  /label: '关键词'/,
  '项目代码快速过滤字段下拉不得显示泛化的关键词，应显示列名。'
)
assert.match(
  source,
  /useTableQuickFilter\([\s\S]*'dcc\.projectCode\.main'[\s\S]*projectCodeQuickFilterDefinitions[\s\S]*queryParams[\s\S]*getList/,
  '快速过滤必须用标准 hook 连接原查询参数和列表加载。'
)
assert.match(
  source,
  /useUserTableColumns\('dcc\.projectCode\.main', projectCodeDefaultColumns\)/,
  '显示字段必须使用稳定 tableKey 持久化。'
)

const mainListEnd = source.indexOf('<el-dialog')
const mainListSource = source.slice(0, mainListEnd)
assert.doesNotMatch(
  mainListSource,
  /<Pagination[\s\S]*@pagination="getList"[\s\S]*\/>/,
  '项目代码主列表分页必须由标准列表模板承载。'
)
assert.doesNotMatch(
  mainListSource,
  /<el-form[\s\S]*class="-mb-15px"/,
  '项目代码主列表必须移除旧独立搜索表单。'
)
assert.doesNotMatch(
  source,
  /\.dcc-project-code-list-template\s*:deep\(\.unified-list-template__query-form\)/,
  '项目代码页面不得覆盖标准列表查询栏布局，操作区应复用模板同行排列。'
)
assert.doesNotMatch(
  source,
  /\.dcc-project-code-list-template\s*:deep\(\.unified-list-template__toolbar-actions\)/,
  '项目代码操作区不得强制独占下一行。'
)
assert.doesNotMatch(
  source,
  /\.dcc-project-code-list-template\s*:deep\(\.unified-list-template__toolbar\)/,
  '项目代码操作按钮必须复用标准列表模板的右对齐布局。'
)

console.log('PASS: dcc project code unified list template static contract')
