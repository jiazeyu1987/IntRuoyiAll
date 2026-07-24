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
const roleApi = readSource('src/api/system/role/index.ts')
const rolePage = readSource('src/views/system/role/index.vue')
const roleForm = readSource('src/views/system/role/RoleForm.vue')
const categoryForm = readSource('src/views/system/role/RoleCategoryForm.vue')
const migrationSql = readWorkspaceSource('ruoyi-vue-pro/sql/mysql/20260707_system_role_category_management.sql')

assert.equal(
  packageJson.scripts?.['e2e:system:role-category:static'],
  'node tests/e2e/system-role-category-static.spec.js',
  'package.json must expose the system role category static contract.'
)

for (const token of [
  'interface RoleCategoryVO',
  '/system/role-category/list',
  '/system/role-category/enabled-list',
  '/system/role-category/create',
  '/system/role-category/update',
  '/system/role-category/delete?id='
]) {
  assert.ok(roleApi.includes(token), `Role API must expose role category contract: ${token}`)
}

for (const token of [
  '角色分类',
  '像文件夹一样管理权限角色',
  'permission-role-category__item',
  "queryParams.categoryId === category.id ? 'is-active' : ''",
  "v-hasPermi=\"['system:role-category:create']\"",
  "v-hasPermi=\"['system:role-category:update']\"",
  "v-hasPermi=\"['system:role-category:delete']\"",
  'selectCategory(category)',
  'getRoleCategoryList()'
]) {
  assert.ok(rolePage.includes(token), `Role page must keep left folder category behavior: ${token}`)
}

assert.ok(
  rolePage.includes('label="所属分类"') || rolePage.includes('prop="categoryName"'),
  'Role table must expose the role category column.'
)

assert.ok(
  roleApi.includes('assignedUserCount?: number'),
  'Role API type must expose assignedUserCount returned by the backend.'
)

assert.ok(
  rolePage.includes('label="分配人数"') &&
    rolePage.includes('prop="assignedUserCount"') &&
    rolePage.includes('permission-role-table__number'),
  'Role table must expose the backend assigned user count column.'
)

for (const token of [
  'prop="categoryId"',
  '角色分类不能为空',
  'getEnabledRoleCategoryList()',
  'categoryOptions'
]) {
  assert.ok(roleForm.includes(token), `Role form must require enabled category selection: ${token}`)
}

for (const token of ['新增角色分类', '编辑角色分类', '分类名称不能为空', '分类标识不能为空']) {
  assert.ok(categoryForm.includes(token), `Role category form must preserve category maintenance UI: ${token}`)
}

for (const token of ['展厅', '批记录', '排产', '文控', 'SRM', '菜单']) {
  assert.ok(migrationSql.includes(token), `Migration SQL must seed default category: ${token}`)
}

assert.ok(
  migrationSql.includes('SELECT unmatched_roles AS unmatched_roles') &&
    migrationSql.includes('SIGNAL SQLSTATE') &&
    migrationSql.includes('角色分类迁移存在未匹配历史角色'),
  'Migration SQL must fail fast with unmatched historical role list.'
)

assert.ok(
  migrationSql.includes('system:role-category:query'),
  'Migration SQL must seed role category query permission.'
)

assert.doesNotMatch(
  `${roleApi}\n${rolePage}\n${roleForm}\n${categoryForm}`,
  /catch\s*\{\s*\}|mock|placeholder data|fallback|降级|吞异常/i,
  'Role category management must not introduce mock, placeholder, fallback, downgrade, or silent catch.'
)

console.log('PASS: system role category static contract')
