const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const path = require('node:path')
const ts = require('typescript')
const src = path.resolve(__dirname, '../../src')
const user = { permissions: new Set() }
function load(file) {
  const code = ts.transpileModule(fs.readFileSync(path.join(src, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
  }).outputText
  const exports = {}
  vm.runInNewContext(code, { exports, useI18n: () => ({ t: x => x }), require: name => {
    if (name === '@/store/modules/user') return { useUserStore: () => user }
    if (name === '@/utils/erpDataPermission') return load('utils/erpDataPermission.ts')
    throw new Error('Unexpected dependency: ' + name)
  } })
  return exports
}
const { hasPermission } = load('directives/permission/hasPermi.ts')
test('wildcard retains other business access but cannot restore ERP data access', () => {
  user.permissions = new Set(['*:*:*'])
  assert.equal(hasPermission(['mes:pro-feedback:query']), true)
  assert.equal(hasPermission(['erp:product:query']), false)
  assert.equal(hasPermission(['erp:data-order:operate']), false)
})
test('an explicit ERP product grant does not open production orders', () => {
  user.permissions = new Set(['*:*:*', 'erp:data-product:operate'])
  assert.equal(hasPermission(['erp:data-product:operate']), true)
  assert.equal(hasPermission(['erp:data-order:operate']), false)
})
test('inventory detail and stock summary use independent sync permissions', () => {
  const { erpSyncPermission } = load('utils/erpDataPermission.ts')
  assert.equal(erpSyncPermission('INVENTORY_DETAIL'), 'erp:data-inventory:operate')
  assert.equal(erpSyncPermission('STOCK'), 'erp:data-stock:operate')
  assert.throws(() => erpSyncPermission('UNKNOWN'))
})
