const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')

function readUtf8(absolutePath) {
  assert.ok(fs.existsSync(absolutePath), `missing required file: ${absolutePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

function assertContains(source, expected, label) {
  assert.ok(source.includes(expected), `missing ${label}: ${expected}`)
}

const routeSource = readUtf8(
  path.join(repoRoot, 'src/router/modules/remaining.ts')
)
const sqlSource = readUtf8(
  path.join(workspaceRoot, 'ruoyi-vue-pro/sql/mysql/20260630_dcc_admin_full_config_menu.sql')
)

for (const fragment of [
  "path: 'controlled-file/admin'",
  "component: () => import('@/views/dcc/controlled-file/admin/index.vue')",
  "name: 'DccControlledFileAdmin'",
  "title: '文控管理员'",
  "activeMenu: '/dcc/controlled-file/admin'",
  "permission: ['dcc:controlled-file:category:manage']"
]) {
  assertContains(routeSource, fragment, 'dcc admin hidden route contract')
}

for (const fragment of [
  "SELECT 6819, '文控管理员', 'dcc:controlled-file:category:manage'",
  "'controlled-file/admin'",
  "'dcc/controlled-file/admin/index'",
  "'DccControlledFileAdmin'",
  'SELECT src.`role_id`, 6819',
  "source_menu.`path` = 'controlled-file/categories'",
  'WHERE src.`menu_id` = source_menu.`id`'
]) {
  assertContains(sqlSource, fragment, 'dcc admin menu SQL contract')
}

console.log('PASS: dcc admin full config route and SQL contract is present')
