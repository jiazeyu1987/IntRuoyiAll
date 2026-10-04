const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const stateCode = ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/project-attributes/state.ts', 'utf8'),
  { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const stateContext = { exports: {}, Error }
vm.runInNewContext(stateCode, stateContext)
const actual = { targetMarkets: ['CE'], licenseHolder: 'N', actualManufacturer: 'Y', documentTransfer: 'N' }
function bridge(response = true) {
  const calls = []
  const context = { exports: {}, Error, BigInt, Set, require: name => {
    if (name === '@/config/axios') return { default: { post: async request => { calls.push(request); return response } } }
    if (name.endsWith('project-attributes/state')) return stateContext.exports
    throw new Error('Unexpected dependency: ' + name)
  }}
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/api/dcc/controlledFile/applicationRead.ts', 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return { api: context.exports, calls }
}
test('working attributes save uses the exact File and a frozen actual copy with a confirmed response', async () => {
  const state = bridge()
  assert.equal(typeof state.api.saveWorkingApplicationAttributes, 'function')
  const selected = JSON.parse(JSON.stringify(actual))
  const pending = state.api.saveWorkingApplicationAttributes('9223372036854775000', selected)
  selected.targetMarkets.push('FDA')
  assert.equal(await pending, true)
  assert.equal(state.calls[0].url, '/dcc/controlled-files/9223372036854775000/working-attributes')
  assert.deepEqual(JSON.parse(JSON.stringify(state.calls[0].data)), actual)
  assert.equal(state.calls[0].ignoreErrorMessage, true)
})
test('false save response is a visible failure rather than default success', async () => {
  const state = bridge(false)
  assert.equal(typeof state.api.saveWorkingApplicationAttributes, 'function')
  await assert.rejects(state.api.saveWorkingApplicationAttributes('900', actual), /确认|保存/)
})
test('invalid actual attributes do not create an arbitrary File write', async () => {
  const state = bridge()
  assert.equal(typeof state.api.saveWorkingApplicationAttributes, 'function')
  await assert.rejects(state.api.saveWorkingApplicationAttributes('900', { ...actual, targetMarkets: [] }))
  assert.equal(state.calls.length, 0)
})
