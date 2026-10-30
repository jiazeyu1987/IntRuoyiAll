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
assert.equal(allowed(records, ['tenant_admin'], new Set([manage, query])), false)
assert.equal(allowed(authorizations, ['tenant_admin'], new Set([manage])), false)
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
assert.equal(filter(menus, ['tenant_admin'], new Set([manage]))[0].children.length, 1)
assert.equal(filter(menus, ['electronic_signature_admin'], new Set([manage, query]))[0].children[1].children.length, 2)
assert.equal(menus[0].children[1].children.length, 2, 'Filtering must not mutate granted routes')
console.log('signature menu access: 11 assertions passed')
