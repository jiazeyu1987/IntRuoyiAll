const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(
  root,
  'src/views/dcc/controlled-file/basic-data/file-type-taxonomy/index.vue'
)
const source = fs.readFileSync(sourcePath, 'utf8')

const unifiedListMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="dcc\.fileTypeTaxonomy\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(unifiedListMatch, 'DCC 文件分类仍必须使用标准列表模板。')
const unifiedList = unifiedListMatch[0]

const headingMatch = source.match(/<span class="text-18px[\s\S]*?<\/span>/)
assert.ok(headingMatch, 'DCC 文件分类必须保留页面标题。')
assert.match(headingMatch[0], />DCC文件分类<\/span>/, '页面标题应只显示 DCC文件分类。')
assert.doesNotMatch(headingMatch[0], /基础数据\s*\//, '页面标题不应再显示“基础数据 /”。')

assert.match(
  unifiedList,
  /:show-quick-filter-label="false"/,
  '标准列表模板应关闭“快速过滤”文案。'
)
assert.doesNotMatch(
  unifiedList,
  /<template\s+#extra-filters\b[\s\S]*?<\/template>/,
  '红框内层级和启用状态额外筛选应从页面移除。'
)
assert.doesNotMatch(unifiedList, /placeholder="层级"/, '不应再显示层级筛选。')
assert.doesNotMatch(unifiedList, /placeholder="启用状态"/, '不应再显示启用状态筛选。')
assert.doesNotMatch(
  unifiedList,
  /<el-button[\s\S]*?@click="loadData"[\s\S]*?刷新[\s\S]*?<\/el-button>/,
  '红框内刷新按钮应从页面移除。'
)

assert.match(
  unifiedList,
  /<template #actions>[\s\S]*openForm\('create'\)[\s\S]*新增一级[\s\S]*<\/template>/,
  '新增一级按钮必须保留。'
)
assert.match(
  source,
  /key:\s*'keyword'[\s\S]*label:\s*'关键词'[\s\S]*queryParamKey:\s*'keyword'/,
  '关键词快速查询必须保留。'
)
assert.match(
  unifiedList,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*:data="paginatedTreeRows"[\s\S]*:tree-props="taxonomyTreeProps"/,
  '树形表格必须保留。'
)
assert.match(
  unifiedList,
  /data-user-table-key="dcc\.fileTypeTaxonomy\.main"/,
  '显示字段配置入口对应的表格 key 必须保留。'
)
assert.match(
  unifiedList,
  /label="操作"[\s\S]*fixed="right"[\s\S]*新增下级[\s\S]*编辑[\s\S]*删除/,
  '行级维护操作必须保留。'
)

assert.doesNotMatch(
  source,
  /levelMatched|activeMatched|query\.levelNo|query\.active/,
  '删除层级/启用状态筛选后，不应保留不可达的筛选状态。'
)

console.log('PASS: dcc file type taxonomy red box removal static contract')
