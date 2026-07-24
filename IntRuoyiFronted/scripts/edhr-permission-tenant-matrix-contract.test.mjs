import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import assert from 'node:assert/strict'

const root = process.cwd()
const packageJsonPath = path.join(root, 'package.json')
const e2ePath = path.join(root, 'tests', 'e2e', 'edhr-permission-tenant-matrix.e2e.js')
const fixturePath = path.join(
  root,
  'doc',
  'tasks',
  '20260528-edhr-role-tenant-e2e-gate',
  'scripts',
  'prepare-edhr-role-matrix-fixtures.cjs'
)

function readText(filePath) {
  assert.ok(fs.existsSync(filePath), `Missing required file: ${path.relative(root, filePath)}`)
  return fs.readFileSync(filePath, 'utf8')
}

test('package scripts wire the eDHR role/tenant matrix gate', () => {
  const packageJson = JSON.parse(readText(packageJsonPath))
  assert.equal(
    packageJson.scripts['e2e:edhr:permission-matrix:check'],
    'node --check tests/e2e/edhr-permission-tenant-matrix.e2e.js'
  )
  assert.equal(
    packageJson.scripts['e2e:edhr:permission-matrix'],
    'node tests/e2e/edhr-permission-tenant-matrix.e2e.js'
  )
})

test('Playwright matrix E2E has fail-fast tenant, write, and explicit no-permission guards', () => {
  const e2e = readText(e2ePath)
  assert.match(e2e, /EDHR_MATRIX_EXECUTOR_USERNAME/)
  assert.match(e2e, /DEFAULT_ROLE_USERNAMES/)
  for (const username of [
    'edhrmatrixexecutor',
    'edhrmatrixapprover',
    'edhrmatrixarchiver',
    'edhrmatrixreadonly',
    'edhrmatrixdenied'
  ]) {
    assert.match(e2e, new RegExp(username))
    assert.match(username, /^[a-zA-Z0-9]{4,30}$/)
    assert.doesNotMatch(username, /_/)
  }
  assert.match(e2e, /EDHR_MATRIX_ADMIN_BASE_URL/)
  assert.match(e2e, /EDHR_MATRIX_ADMIN_TENANT/)
  assert.match(e2e, /EDHR_MATRIX_ADMIN_USERNAME/)
  assert.match(e2e, /EDHR_MATRIX_ADMIN_PASSWORD/)
  assert.match(e2e, /adminRuntime/)
  assert.match(e2e, /FORBIDDEN_MUTATING_TENANTS/)
  assert.match(e2e, /芋道源码/)
  assert.match(e2e, /yudao/)
  assert.match(e2e, /prod/)
  assert.match(e2e, /production/)
  assert.match(e2e, /installEdhrWriteGuard/)
  assert.match(e2e, /batch-record-execution/)
  assert.match(e2e, /POST\|PUT\|PATCH\|DELETE/)
  assert.match(e2e, /NO_PERMISSION_PATTERNS/)
  assert.match(e2e, /requireRendered/)
  assert.match(e2e, /allowed-path smoke must render eDHR UI/)
  assert.match(
    e2e,
    /await runRoute\(page, testRuntime, routeSpec, config\.accounts\[routeSpec\.role\], \{\s*writeGuard: true,\s*requireRendered: true\s*\}\)/s
  )
  assert.match(e2e, /空白成功不可通过/)
  assert.match(e2e, /result\.json/)
  assert.match(e2e, /real-e2e-evidence\.md/)
})

test('fixture script is dry-run first and tenant 122 scoped', () => {
  const fixture = readText(fixturePath)
  assert.match(fixture, /--apply/)
  assert.match(fixture, /DRY_RUN/)
  assert.match(fixture, /TENANT_ID = 122/)
  assert.match(fixture, /5100/)
  assert.match(fixture, /5700/)
  assert.match(fixture, /900023/)
  assert.match(fixture, /900024/)
  assert.match(fixture, /menuIds/)
  assert.match(fixture, /assertTenant122/)
  assert.match(fixture, /aoteman/)
  for (const username of [
    'edhrmatrixexecutor',
    'edhrmatrixapprover',
    'edhrmatrixarchiver',
    'edhrmatrixreadonly',
    'edhrmatrixdenied'
  ]) {
    assert.match(fixture, new RegExp(`username: '${username}'`))
    assert.match(username, /^[a-zA-Z0-9]{4,30}$/)
    assert.doesNotMatch(username, /_/)
  }
  assert.match(fixture, /legacyUsername: 'edhr_matrix_executor'/)
  assert.match(fixture, /UPDATE system_users legacy_user/)
  assert.match(
    fixture,
    /legacy_user\.password_update_time = NOW\(\)/,
    'legacy username migration must refresh password_update_time so copied password hashes are login-usable.'
  )
  assert.match(
    fixture,
    /INSERT INTO system_users \([^)]*password_update_time[^)]*\)/,
    'matrix user creation must set password_update_time.'
  )
  assert.match(
    fixture,
    /SELECT seed\.next_id,[\s\S]*NOW\(\), @operator, NOW\(\), @operator, NOW\(\), b'0', @tenant_id/,
    'matrix user creation must populate password_update_time with NOW().'
  )
  assert.match(
    fixture,
    /password_update_time = COALESCE\(password_update_time, NOW\(\)\)/,
    'rerunning apply must repair existing matrix users whose password_update_time is NULL.'
  )
  for (const table of ['system_role', 'system_users', 'system_user_role', 'system_role_menu']) {
    assert.match(
      fixture,
      new RegExp(`FROM \\(SELECT COALESCE\\(MAX\\(id\\), 0\\) \\+ 1 AS next_id FROM ${table}\\) seed`),
      `${table} id allocation must use a derived seed subquery so false insert predicates emit zero rows.`
    )
    assert.doesNotMatch(
      fixture,
      new RegExp(`SELECT COALESCE\\(MAX\\(id\\), 0\\) \\+ 1,[\\s\\S]*?FROM ${table}\\s+WHERE`),
      `${table} must not use aggregate MAX(id)+1 with a direct WHERE predicate because MySQL emits one NULL aggregate row.`
    )
  }
  assert.match(fixture, /role_menu/)
  assert.match(fixture, /denied.*no eDHR menus/s)
  assert.doesNotMatch(fixture, /console\.log\([^)]*password/i, 'fixture must not print passwords')
  assert.doesNotMatch(fixture, /passwordHash[^]*JSON\.stringify/, 'fixture JSON must not expose password hashes')
})
