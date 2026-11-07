import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import test from 'node:test'

const require = createRequire(import.meta.url)
const ts = require('typescript')
const source = readFileSync(new URL('../src/utils/signatureAccess.ts', import.meta.url), 'utf8')
const code = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
const module = { exports: {} }
new Function('module', 'exports', code)(module, module.exports)
const { canAccessSignaturePath: access, filterSignatureMenus: menus } = module.exports
const query = new Set(['signature-governance:policy:query'])

test('explicit record-query permission enables a dedicated reader without administrator privileges', () => {
  const reader = ['edhr01_signature_reader']
  assert.equal(access('/signature-governance/signature-records', reader, query), true)
  assert.equal(access('/signature-governance/signature-records/', reader, query), true)
  for (const tab of ['authorizations', 'retention', 'periodic-review', 'csv-package', 'policy']) {
    assert.equal(access(`/signature-governance/${tab}`, reader, query), false, tab)
  }
})

test('missing query permission still denies the record list, including administrator-role names alone', () => {
  for (const roles of [[], ['edhr01_signature_reader'], ['audit_admin'], ['tenant_admin']]) {
    assert.equal(access('/signature-governance/signature-records', roles, new Set()), false)
  }
  assert.equal(access('/signature-governance/signature-records', [], new Set(['*:*:*'])), true)
})

test('reader navigation exposes only the permitted record menu and preserves management restrictions', () => {
  const routes = [{ path: '/signature-governance', children: [
    { path: 'signature-records' }, { path: 'authorizations' }, { path: 'retention' }, { path: 'policy' }
  ] }]
  assert.deepEqual(menus(routes, ['edhr01_signature_reader'], query)[0].children,
    [{ path: 'signature-records' }])
  assert.deepEqual(menus(routes, ['edhr01_signature_reader'], new Set()), [])
  assert.equal(access('/signature-governance/authorizations', ['tenant_admin'], query), false)
  assert.equal(access('/signature-governance/authorizations', ['tenant_admin'],
    new Set(['dcc:controlled-file:signature:manage'])), true)
})
