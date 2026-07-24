const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const e2ePath = path.join(repoRoot, 'tests/e2e/mdm-tenant-package-real-setup.e2e.js')
const packageJsonPath = path.join(repoRoot, 'package.json')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const e2eSource = fs.existsSync(e2ePath) ? readUtf8(e2ePath) : ''
const packageJson = JSON.parse(readUtf8(packageJsonPath))

assert.equal(
  packageJson.scripts?.['e2e:dcc:test-tenant-package:check'],
  'node tests/e2e/mdm-tenant-package-real-setup-static.spec.js',
  'package.json must expose the DCC test tenant package static gate'
)
assert.equal(
  packageJson.scripts?.['e2e:dcc:test-tenant-package'],
  'node tests/e2e/mdm-tenant-package-real-setup.e2e.js',
  'package.json must expose the DCC test tenant package real frontend setup path'
)

for (const fragment of [
  "require('playwright')",
  'TENANT_PACKAGE_E2E_ALLOW_WRITE',
  'TENANT_PACKAGE_E2E_APPROVAL',
  'ALLOW_TEST_TENANT_PACKAGE_WRITE',
  'TENANT_PACKAGE_E2E_BASE_URL',
  '172.30.30.57',
  'TARGET_TENANT_NAME',
  '测试租户',
  'SHARED_PACKAGE_ID',
  '111',
  '/system/tenant/package',
  '/system/tenant/list',
  '基础数据',
  '产品主数据',
  '文件提交',
  '文件查阅',
  '文控权限',
  '运行控制台',
  '角色管理',
  'REQUIRED_PACKAGE_MENU_LABELS',
  'MAX_TEST_PACKAGE_SELECTED_TREE_NODES',
  'ensureRequiredPackageMenus',
  'clickVisibleTableAction',
  "['修改', '编辑']",
  'waitForAnyVisibleText',
  'findVisibleFormInput',
  'input[placeholder="${placeholder}"]',
  'system/tenant-package/create',
  'system/tenant-package/update',
  'system/tenant/update'
]) {
  assert.ok(e2eSource.includes(fragment), `setup script must contain ${fragment}`)
}

assert.ok(
  !e2eSource.includes('locator(`text=${requiredText}`).first().waitFor'),
  'setup script must not bind page readiness to the first text match because hidden duplicated menu text can appear before the visible page title'
)
assert.ok(
  !e2eSource.includes("formItem.locator('input').first().fill"),
  'setup script must wait for a visible form input by label or placeholder before filling search fields'
)
assert.ok(
  !e2eSource.includes("openPage(page, '/system/tenant', '租户管理')"),
  'setup script must open the tenant list leaf route, not the tenant management parent route'
)

assert.ok(
  !e2eSource.includes('selectAllSwitch'),
  'setup script must not use the tenant package full-menu select-all switch; it must select only required E2E menus'
)
assert.ok(
  e2eSource.includes('selectedMenuNodeCount') && e2eSource.includes('too many menu nodes'),
  'setup script must fail fast before creating a tenant package when selected menu nodes exceed the E2E scope'
)

assert.ok(
  e2eSource.includes('if (!ALLOW_WRITE)') &&
    e2eSource.includes('set TENANT_PACKAGE_E2E_ALLOW_WRITE=true') &&
    e2eSource.includes('after explicit user approval'),
  'setup script must default to read-only fail-fast before any write'
)
assert.ok(
  e2eSource.includes('assert.notEqual(Number(currentPackageId), SHARED_PACKAGE_ID') ||
    e2eSource.includes('current package is shared package 111'),
  'setup script must refuse to update shared tenant package 111'
)
assert.ok(
  !e2eSource.includes('page.request') &&
    !e2eSource.includes('fetch(') &&
    !e2eSource.includes('axios.') &&
    !e2eSource.includes('request.post') &&
    !e2eSource.includes('request.put'),
  'setup script must use frontend pages and only observe their responses, not direct admin API calls'
)

console.log('PASS: DCC test tenant package setup static contract is present')
