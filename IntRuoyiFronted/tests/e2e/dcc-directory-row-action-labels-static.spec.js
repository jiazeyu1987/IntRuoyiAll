const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const directoryPagePath = path.join(
  repoRoot,
  'src/views/dcc/controlled-file/directories/index.vue'
)

const directoryPage = fs.readFileSync(directoryPagePath, 'utf8')
const operationColumnMatch = directoryPage.match(
  /<el-table-column align="center" fixed="right" label="操作"[\s\S]*?<\/el-table-column>/
)

assert.ok(operationColumnMatch, '目录管理页必须保留行内操作列')

const operationColumn = operationColumnMatch[0]

assert.ok(!operationColumn.includes('访问规则'), '目录行操作列不得继续显示“访问规则”按钮')
assert.ok(!operationColumn.includes('新建子目录'), '目录行操作列新建入口应改名为“新建”')
assert.ok(!operationColumn.includes('删除父文件夹'), '目录行操作列删除入口应改名为“删除”')

for (const label of ['新建', '编辑', '删除']) {
  assert.ok(operationColumn.includes(`\n            ${label}\n`), `目录行操作列必须显示“${label}”`)
}

assert.ok(
  operationColumn.includes("@click=\"openForm('create', row)\""),
  '“新建”按钮必须保留新建子目录行为'
)
assert.ok(
  operationColumn.includes("@click=\"openForm('update', row)\""),
  '“编辑”按钮必须保留编辑目录行为'
)
assert.ok(
  operationColumn.includes('@click="handleDeleteParentFolder(row)"'),
  '“删除”按钮必须保留删除父文件夹确认流程'
)
