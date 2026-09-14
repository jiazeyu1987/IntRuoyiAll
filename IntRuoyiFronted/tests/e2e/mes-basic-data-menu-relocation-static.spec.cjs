const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const repoRoot = path.resolve(frontendRoot, '..')
const migrationPath = path.join(
  repoRoot,
  'IntRuoyiBackend',
  'sql',
  'mysql',
  '20260903_mes_basic_data_menu_relocation.sql'
)

const migration = fs.readFileSync(migrationPath, 'utf8')
const projectCodePanel = fs.readFileSync(
  path.join(frontendRoot, 'src', 'views', 'dcc', 'controlled-file', 'basic-data', 'components', 'ProjectCodeTabPanel.vue'),
  'utf8'
)
const productPage = fs.readFileSync(
  path.join(frontendRoot, 'src', 'views', 'mdm', 'product', 'index.vue'),
  'utf8'
)
const controlledFileDetail = fs.readFileSync(
  path.join(frontendRoot, 'src', 'views', 'dcc', 'controlled-file', 'detail', 'index.vue'),
  'utf8'
)
const projectCodeMigration = migration.match(/UPDATE\s+`system_menu`[\s\S]*?WHERE\s+`id`\s*=\s*990210[\s\S]*?;/u)?.[0] || ''
const showroomProductMigration = migration.match(/UPDATE\s+`system_menu`[\s\S]*?WHERE\s+`id`\s*=\s*990201[\s\S]*?;/u)?.[0] || ''

assert.match(projectCodeMigration, /`parent_id`\s*=\s*5101/u,
  'DCC项目代码必须迁入 MES系统 > 基础数据')
assert.match(showroomProductMigration, /`parent_id`\s*=\s*5101/u,
  '展厅主数据必须迁入 MES系统 > 基础数据')
assert.doesNotMatch(projectCodeMigration, /`(permission|component|component_name)`\s*=/u,
  'DCC项目代码迁移不得重写权限或组件')
assert.doesNotMatch(showroomProductMigration, /`(permission|component|component_name)`\s*=/u,
  '展厅主数据迁移不得重写权限或组件')
assert.match(migration, /START\s+TRANSACTION;[\s\S]*COMMIT;/u,
  '两个菜单更新必须在同一事务内执行')

for (const [sourceName, source] of [
  ['DCC项目代码页签', projectCodePanel],
  ['展厅主数据页签', productPage],
  ['受控文件详情', controlledFileDetail]
]) {
  assert.doesNotMatch(source, /['"]\/mdm\/(project-code|product)['"]/u, `${sourceName}不得继续跳转全局基础数据路径`)
}

assert.match(projectCodePanel, /['"]\/mes\/md\/showroom-product['"]/u,
  'DCC项目代码页签应跳转展厅主数据 MES 路径')
assert.match(productPage, /['"]\/mes\/md\/dcc-project-code['"]/u,
  '展厅主数据页签应跳转 DCC项目代码 MES 路径')
assert.match(controlledFileDetail, /['"]\/mes\/md\/dcc-project-code['"]/u,
  '受控文件详情应跳转 DCC项目代码 MES 路径')
console.log('MES基础数据菜单归属静态合同通过')
