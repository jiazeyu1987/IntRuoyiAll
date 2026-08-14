const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')

const formalQaPermission = 'mes:qa-inspection-regulation:query'
const oldTeamLeaderPermission = 'mes:pro-process-pool-team-leader:query'

const routePath = path.join(frontendRoot, 'src/router/modules/remaining.ts')
const originalMenuSqlPath = path.join(backendRoot, 'sql/mysql/20260804_mes_edhr_qa_menu.sql')
const qaRoleMigrationPath = path.join(
  backendRoot,
  'sql/mysql/20260806_mes_qa_role_permission_tab.sql'
)

for (const requiredPath of [routePath, originalMenuSqlPath, qaRoleMigrationPath]) {
  assert.ok(fs.existsSync(requiredPath), `required file must exist: ${requiredPath}`)
}

const routeSource = fs.readFileSync(routePath, 'utf8')
const originalMenuSql = fs.readFileSync(originalMenuSqlPath, 'utf8')
const qaRoleMigrationSql = fs.readFileSync(qaRoleMigrationPath, 'utf8')
const upperMigrationSql = qaRoleMigrationSql.toUpperCase()

const qaRouteMatch = routeSource.match(
  /path:\s*'pro\/process-pool\/qa-regulation'[\s\S]*?activeMenu:\s*'\/mes\/pro\/process-pool\/qa-regulation'[\s\S]*?permission:\s*\[[^\]]+\]/
)
assert.ok(qaRouteMatch, 'QA regulation route block must be present.')
const qaRouteBlock = qaRouteMatch[0]

assert.match(
  qaRouteBlock,
  /permission:\s*\['mes:qa-inspection-regulation:query'\]/,
  'QA route must use the formal QA permission instead of the shared team-leader permission.'
)
assert.doesNotMatch(
  qaRouteBlock,
  new RegExp(oldTeamLeaderPermission.replaceAll(':', ':')),
  'QA route must not reuse the shared team-leader permission.'
)

assert.match(
  originalMenuSql,
  /900434[\s\S]*'QA'[\s\S]*'mes:qa-inspection-regulation:query'[\s\S]*'\/mes\/pro\/process-pool\/qa-regulation'/,
  'Fresh eDHR QA menu seed must declare the formal QA menu permission.'
)

assert.match(
  qaRoleMigrationSql,
  /UPDATE\s+`system_menu`[\s\S]*`permission`\s*=\s*'mes:qa-inspection-regulation:query'[\s\S]*`id`\s*=\s*900434/,
  'Incremental migration must correct already-seeded QA menu permission.'
)
assert.match(
  qaRoleMigrationSql,
  /INSERT\s+INTO\s+`system_role`[\s\S]*'QA'[\s\S]*'qa'/,
  'Migration must create the QA permission role when it is missing.'
)
assert.match(
  qaRoleMigrationSql,
  /UPDATE\s+`system_role`[\s\S]*`code`\s*=\s*'qa'/,
  'Migration must recover an existing QA role instead of duplicating it.'
)
assert.match(
  qaRoleMigrationSql,
  /INSERT\s+INTO\s+`system_role_menu`/,
  'Migration must bind the QA page menu to the QA role.'
)
assert.match(
  qaRoleMigrationSql,
  /INSERT\s+INTO\s+`system_user_role`[\s\S]*`username`\s*=\s*'admin'/,
  'Migration must assign the QA role to admin so admin can perform QA selection.'
)
assert.match(
  qaRoleMigrationSql,
  /SELECT\s+1\s+AS\s+`tenant_id`/,
  'Migration must explicitly include tenant 1 because admin is not necessarily reached through tenant packages.'
)
assert.match(
  qaRoleMigrationSql,
  /SET\s+`role_menu`\.`deleted`\s*=\s*b'1'[\s\S]*`role`\.`code`\s*<>\s*'qa'/,
  'Migration must soft-restrict existing QA menu grants to the QA role only.'
)

for (const permissionMenuId of [5631, 5633]) {
  assert.match(
    qaRoleMigrationSql,
    new RegExp(`SELECT\\s+${permissionMenuId}\\s+AS\\s+\`menu_id\``),
    `QA role must retain backend QA regulation API permission menu ${permissionMenuId}.`
  )
}

for (const forbidden of [
  'DELETE FROM `SYSTEM_ROLE_MENU`',
  'DELETE FROM SYSTEM_ROLE_MENU',
  'DELETE FROM `SYSTEM_USER_ROLE`',
  'DELETE FROM SYSTEM_USER_ROLE',
  'TRUNCATE TABLE',
  'DROP TABLE `SYSTEM_ROLE_MENU`',
  'DROP TABLE SYSTEM_ROLE_MENU'
]) {
  assert.ok(!upperMigrationSql.includes(forbidden), `migration must not use destructive SQL: ${forbidden}`)
}

console.log('PASS QA regulation role permission static contract')
