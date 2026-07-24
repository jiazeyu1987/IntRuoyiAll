const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  const filePath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const gateSource = readUtf8('scripts/dcc-incremental-backup-restore-real-flow-gate.mjs')
const roleSource = readUtf8('tests/e2e/mdm-role-menu-real-setup.e2e.js')
const productSource = readUtf8('tests/e2e/mdm-product-real-setup.e2e.js')

for (const [label, source, allowName, approvalName, approvalToken] of [
  [
    'role menu setup',
    roleSource,
    'MDM_ROLE_E2E_ALLOW_ASSIGN',
    'MDM_ROLE_E2E_APPROVAL',
    'ALLOW_TEST_MDM_ROLE_MENU_WRITE'
  ],
  [
    'product setup',
    productSource,
    'MDM_PRODUCT_E2E_ALLOW_CREATE',
    'MDM_PRODUCT_E2E_APPROVAL',
    'ALLOW_TEST_MDM_PRODUCT_WRITE'
  ]
]) {
  assert.ok(source.includes(allowName), `${label} must keep an explicit allow-write switch`)
  assert.ok(source.includes(approvalName), `${label} must require an explicit approval token`)
  assert.ok(source.includes(approvalToken), `${label} must check the expected approval token`)
  assert.ok(
    source.includes('throw new Error') && source.includes('explicit user approval'),
    `${label} must fail fast before writes when approval is missing`
  )
}

for (const fragment of [
  'MDM_ROLE_E2E_APPROVAL',
  'ALLOW_TEST_MDM_ROLE_MENU_WRITE',
  'MDM_PRODUCT_E2E_APPROVAL',
  'ALLOW_TEST_MDM_PRODUCT_WRITE'
]) {
  assert.ok(gateSource.includes(fragment), `real flow gate must forward child approval token ${fragment}`)
}

const roleSaveIndex = roleSource.indexOf('/admin-api/system/permission/assign-role-menu')
assert.notEqual(roleSaveIndex, -1, 'role menu setup must contain the real assign-role-menu save request')
const roleReadOnlyGuardIndex = roleSource.lastIndexOf('if (!ALLOW_ASSIGN)', roleSaveIndex)
assert.ok(
  roleReadOnlyGuardIndex > roleSource.indexOf("await checkTreeNode(dialog, '产品主数据')"),
  'role menu setup must return in read-only mode after checking required nodes and before assign-role-menu save'
)
assert.ok(
  roleSource.includes('readOnly: true'),
  'role menu setup must report readOnly=true when required menus are already present but writes are not approved'
)

console.log('PASS: MDM real data prerequisite write guards require explicit approval tokens')
