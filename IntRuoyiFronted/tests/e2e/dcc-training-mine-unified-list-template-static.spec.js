const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/dcc/controlled-file/training/mine/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="dcc\.controlledFile\.trainingMine\.main"[\s\S]*?<\/UnifiedListTemplate>/
)

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
  '我的培训列表必须导入标准列表模板。'
)
assert.match(
  source,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  '我的培训列表必须接入显示字段和列宽持久化 hook。'
)
assert.match(
  source,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  '我的培训列表必须接入标准快速过滤 hook。'
)
assert.ok(templateMatch, '我的培训列表必须使用稳定 tableKey 接入标准列表模板。')

const template = templateMatch[0]
assert.match(
  template,
  /:filter-definitions="trainingMineQuickFilterDefinitions"/,
  '我的培训标准列表必须提供快速过滤字段定义。'
)
assert.match(template, /:columns="trainingMineColumns"/, '我的培训标准列表必须提供显示字段配置。')
assert.match(
  template,
  /@quick-filter-query="trainingMineQuickFilter\.applyQuickFilter"/,
  '我的培训快速过滤查询必须由标准 hook 执行。'
)
assert.match(
  template,
  /@column-change="saveTrainingMineColumnConfig"/,
  '我的培训显示字段变更必须保存到用户列配置。'
)
assert.match(template, /@pagination="getList"/, '我的培训分页必须由标准列表模板触发查询。')
assert.doesNotMatch(
  template,
  /<template #extra-filters>[\s\S]*<\/template>/,
  '我的培训页不得渲染红框中的重复筛选区。'
)
assert.doesNotMatch(
  template,
  /<template #actions>[\s\S]*<\/template>/,
  '我的培训页不得渲染红框中的查询、重置和 DCC 工作台操作区。'
)
assert.doesNotMatch(
  source,
  /ControlledFileWorkbenchEntry|isSearchModelInputEmpty|const handleQuery|const resetQuery|@click="handleQuery"|@click="resetQuery"/,
  '我的培训页必须删除红框中的 DCC 工作台、查询、重置和重复筛选逻辑。'
)
assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-column-explicit[\s\S]*data-user-table-key="dcc\.controlledFile\.trainingMine\.main"[\s\S]*@header-dragend="handleTrainingMineHeaderDragend"/,
  '我的培训表格必须接入列宽拖拽持久化。'
)
assert.match(
  source,
  /useTableQuickFilter\([\s\S]*'dcc\.controlledFile\.trainingMine\.main'[\s\S]*trainingMineQuickFilterDefinitions[\s\S]*queryParams[\s\S]*getList/,
  '我的培训必须用标准 hook 连接快速过滤和查询。'
)
assert.match(
  source,
  /useUserTableColumns\('dcc\.controlledFile\.trainingMine\.main', trainingMineDefaultColumns\)/,
  '我的培训必须用稳定 tableKey 保存显示字段。'
)
assert.doesNotMatch(
  source,
  /<Pagination[\s\S]*@pagination="getList"[\s\S]*\/>/,
  '我的培训分页必须由标准列表模板承载。'
)
