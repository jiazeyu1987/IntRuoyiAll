const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const path = require('node:path')
const filename = path.resolve(__dirname, '../../src/utils/signatureAccess.ts')
const js = ts.transpileModule(fs.readFileSync(filename, 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS }
}).outputText
const context = { exports: {}, Set }
vm.runInNewContext(js, context, { filename })
const { canAccessSignaturePath: allowed, filterSignatureMenus: filter } = context.exports
const manage = 'dcc:controlled-file:signature:manage'
const query = 'signature-governance:policy:query'
const records = '/signature-governance/signature-records'
const authorizations = '/signature-governance/authorizations'
assert.equal(allowed(records, ['tenant_admin'], new Set([manage, query])), true)
assert.equal(allowed(authorizations, ['tenant_admin'], new Set([manage])), true)
assert.equal(allowed(records, ['audit_admin'], new Set([query])), true)
assert.equal(allowed(records, ['electronic_signature_admin'], new Set()), false)
assert.equal(allowed(authorizations, ['audit_admin'], new Set([manage])), false)
assert.equal(allowed(authorizations, ['electronic_signature_admin'], new Set([manage])), true)
assert.equal(allowed(authorizations, ['electronic_signature_admin'], new Set()), false)
assert.equal(allowed('/signature-governance/my-signature', ['tenant_admin'], new Set()), true)
const menus = [{ path: '/system', children: [
  { path: 'user' },
  { path: '/signature-governance', children: [
    { path: 'signature-records' }, { path: authorizations }
  ] }
] }]
assert.equal(filter(menus, ['tenant_admin'], new Set([manage, query]))[0].children[1].children.length, 2)
assert.equal(filter(menus, ['electronic_signature_admin'], new Set([manage, query]))[0].children[1].children.length, 2)
assert.equal(menus[0].children[1].children.length, 2, 'Filtering must not mutate granted routes')
for (const tab of ['signature-records', 'authorizations', 'retention', 'periodic-review', 'csv-package', 'policy', 'my-signature']) {
  assert.equal(allowed(`/signature-governance/${tab}`, ['tenant_admin'], new Set([manage, query])), true, tab)
  if (tab !== 'my-signature') {
    assert.equal(allowed(`/signature-governance/${tab}`, ['operator'], new Set([manage, query])), false, tab)
  }
}
assert.equal(allowed(records, ['tenant_admin'], new Set()), false)
assert.equal(allowed(authorizations, ['tenant_admin'], new Set()), false)
const legacyMenus = [{ path: '/system', children: [
  { path: 'overview' },
  { path: '/signature-governance', children: [
    { path: 'overview' }, { path: '/signature-governance/overview/' }, { path: 'my-signature' }
  ] }
] }]
const filteredLegacyMenus = filter(legacyMenus, ['tenant_admin'], new Set([manage, query]))
assert.equal(filteredLegacyMenus[0].children[0].path, 'overview', 'Other overview menus remain available')
assert.equal(filteredLegacyMenus[0].children[1].children.length, 1)
assert.equal(filteredLegacyMenus[0].children[1].children[0].path, 'my-signature')
assert.equal(legacyMenus[0].children[1].children.length, 3, 'Legacy filtering must not mutate routes')
console.log('signature menu access: passed')
