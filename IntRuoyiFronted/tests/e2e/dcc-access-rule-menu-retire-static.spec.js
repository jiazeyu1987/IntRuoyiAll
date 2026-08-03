const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(root, '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const readWorkspaceSource = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const accessRulePage = readSource('src/views/dcc/controlled-file/access-rules/index.vue')
const categoriesPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const directoriesPage = readSource('src/views/dcc/controlled-file/directories/index.vue')
const controlCenterE2e = readSource('tests/e2e/dcc-control-center-buttons-real-flow.e2e.js')
const schema = readWorkspaceSource('IntRuoyiBackend/sql/mysql/20260513_dcc_base_schema.sql')
const renameSql = readWorkspaceSource('IntRuoyiBackend/sql/mysql/20260626_dcc_permission_menu_rename.sql')
const retireSql = readWorkspaceSource('IntRuoyiBackend/sql/mysql/20260626_dcc_access_rule_menu_retire.sql')

assert.equal(
  packageJson.scripts?.['e2e:dcc:access-rule-menu-retire:static'],
  'node tests/e2e/dcc-access-rule-menu-retire-static.spec.js',
  'package.json must expose the DCC access-rule menu retire static contract'
)

assert.ok(
  categoriesPage.includes('label="目录授权"'),
  '文控权限页必须继续保留目录授权页签'
)

for (const token of [
  '访问规则',
  "path: '/dcc/controlled-file/categories'",
  "tab: 'directory-auth'",
  'directoryId: row.id'
]) {
  assert.ok(directoriesPage.includes(token), `目录管理页必须继续通过目录授权页签承接访问规则入口：${token}`)
}

assert.ok(
  accessRulePage.includes("path: '/dcc/controlled-file/categories'") &&
    accessRulePage.includes("tab: 'directory-auth'"),
  '旧访问规则页必须继续作为兼容壳层跳转到文控权限目录授权页签'
)

assert.ok(
  !controlCenterE2e.includes("['access-rules', '/dcc/controlled-file/access-rules']") &&
    !controlCenterE2e.includes("await clickAndReturn(page, '访问规则', '/dcc/controlled-file/access-rules')"),
  '控制中心/导航回归脚本不得再把 access-rules 当成独立可见主入口'
)

assert.ok(
  schema.includes("SELECT 6802, 'DCC访问规则'"),
  '基线 DCC schema 仍应保留 6802 历史菜单记录以兼容旧系统'
)
assert.ok(
  schema.includes("'DccControlledFileAccessRules', 1, b'0'"),
  '基线 DCC schema 必须把 6802 初始化为退役态：status=1, visible=0'
)

assert.ok(
  renameSql.includes('controlled-file/categories') && !renameSql.includes('controlled-file/access-rules'),
  '文控权限改名 SQL 不得误伤 access-rules 入口'
)
assert.ok(
  retireSql.includes("visible` = b'0'") &&
    retireSql.includes('`status` = 1') &&
    retireSql.includes("controlled-file/access-rules"),
  '访问规则退役 SQL 必须显式隐藏 6802 菜单并保留历史记录'
)

assert.doesNotMatch(
  `${accessRulePage}\n${categoriesPage}\n${directoriesPage}\n${controlCenterE2e}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '退役独立访问规则入口不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC access-rule menu retire static contract')
