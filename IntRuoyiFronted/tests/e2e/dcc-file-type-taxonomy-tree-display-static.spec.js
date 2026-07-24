const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const source = readSource(
  'src/views/dcc/controlled-file/basic-data/file-type-taxonomy/index.vue'
)
const tableSource = source.slice(source.indexOf('<el-table'), source.indexOf('</el-table>'))
const firstColumnSource = tableSource.slice(
  tableSource.indexOf('<el-table-column'),
  tableSource.indexOf('</el-table-column>') + '</el-table-column>'.length
)

assert.match(
  source,
  /<el-table[\s\S]*:data="paginatedTreeRows"/,
  'DCC 文件分类表格必须使用标准列表模板分页后的树形数据源 paginatedTreeRows'
)
assert.match(
  source,
  /const paginatedTreeRows = computed\(\(\) =>[\s\S]*filteredTreeRows\.value\.slice/,
  'paginatedTreeRows 必须从 filteredTreeRows 分页，不能绕开树形筛选结果'
)
assert.doesNotMatch(
  source,
  /<el-table[\s\S]*:data="filteredRows"/,
  'DCC 文件分类表格不得继续直接使用平铺 filteredRows'
)
assert.ok(source.includes(':tree-props="taxonomyTreeProps"'), '树形表格必须显式声明 children 字段')
assert.match(firstColumnSource, /label="分类名称"/, '树形展开和缩进必须落在分类名称主列')
assert.ok(
  tableSource.indexOf('label="分类名称"') < tableSource.indexOf('label="层级"'),
  '分类名称列必须位于层级列之前，避免树形图标挤在窄层级列里'
)
assert.ok(source.includes('buildTaxonomyTreeRows'), '页面必须集中构建 DCC 文件分类树')
assert.ok(source.includes('filterTaxonomyTreeRows'), '页面必须集中裁剪筛选后的 DCC 文件分类树')
assert.ok(source.includes('taxonomyRowMatchesQuery'), '页面必须复用同一条行筛选规则')
assert.match(
  source,
  /const filteredTreeRows = computed\(\(\) =>[\s\S]*filterTaxonomyTreeRows\([\s\S]*buildTaxonomyTreeRows\(rows\.value\)/,
  'filteredTreeRows 必须从真实 rows 构建树后再按查询条件裁剪'
)
assert.match(
  source,
  /const matchedChildren = filterTaxonomyTreeRows\(row\.children \|\| \[\]\)[\s\S]*rowMatched \|\| matchedChildren\.length > 0/,
  '筛选命中子节点时必须保留父级链路'
)
assert.match(
  source,
  /children: matchedChildren\.length > 0 \? matchedChildren : undefined/,
  '筛选后无子节点的行不应保留空 children，避免显示空展开入口'
)

for (const preservedToken of [
  'rootCreateMode',
  'buildRootTaxonomyCode',
  'resolveNextRootTaxonomySort',
  "openForm('create', undefined, row)",
  "v-hasPermi=\"['dcc:controlled-file:category:manage']\""
]) {
  assert.ok(source.includes(preservedToken), `树形展示不得破坏既有维护能力：${preservedToken}`)
}

assert.doesNotMatch(
  source,
  /mock|placeholder data|fallback|降级|吞异常/i,
  'DCC 文件分类树形展示不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC file type taxonomy tree display static contract')
