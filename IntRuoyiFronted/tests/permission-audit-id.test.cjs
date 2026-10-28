const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { webcrypto } = require('node:crypto')
const source = fs.readFileSync(path.join(__dirname, '../src/utils/permission-audit-id.js'), 'utf8')
const load = (crypto) => vm.runInNewContext(source.replace('export const ', 'const ') + '\ncreatePermissionAuditId', { crypto, Uint8Array })

test('HTTP browser API creates unique UUID v4 audit keys without randomUUID', () => {
  const create = load({ getRandomValues: webcrypto.getRandomValues.bind(webcrypto) })
  const keys = new Set(Array.from({ length: 1000 }, () => create('ROLE-MENU', 1474)))
  assert.equal(keys.size, 1000)
  for (const key of keys) assert.match(key, /^SYSTEM-PERM-ROLE-MENU-1474-[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/)
})
test('missing secure random source fails visibly instead of inventing an ID', () => {
  assert.throws(() => load(undefined)('ROLE-MENU', 1), /浏览器不支持安全随机数/)
})
test('all three permission forms use the shared generator and expose failures', () => {
  for (const file of ['role/RoleAssignMenuForm.vue', 'role/RoleDataPermissionForm.vue', 'user/UserAssignRoleForm.vue']) {
    const text = fs.readFileSync(path.join(__dirname, '../src/views/system', file), 'utf8')
    assert.match(text, /import \{ createPermissionAuditId \}/)
    assert.match(text, /catch \(error\) \{\s+message\.error/)
    assert.doesNotMatch(text, /crypto\.randomUUID/)
  }
})
