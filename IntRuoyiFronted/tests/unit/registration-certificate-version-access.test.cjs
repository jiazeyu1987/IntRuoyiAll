const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const source = fs.readFileSync('src/views/dcc/registration-certificate/index/index.vue', 'utf8')
const descriptor = parse(source).descriptor
const script = compileScript(descriptor, { id: 'version-access' })
function actions(prompt, submit) {
  const begin = script.content.indexOf('const requestingOldAccessId =')
  const end = script.content.indexOf('const openOldDetail', begin)
  const text = script.content.slice(begin, end) + '\nreturn { requestOldAccess, requestingOldAccessId };'
  return new Function('ref', 'ElMessageBox', 'submitRegistrationCertificateAccessRequest', 'createSecureUuid', 'message', ts.transpileModule(text, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText)(require('vue').ref, { prompt }, submit, () => 'uuid', { success() {} })
}
test('old certificate requests retain approval type, prevent duplicates, and release loading', async () => {
  let resolve, calls = []
  const state = actions(() => new Promise(r => { resolve = r }), async (...args) => calls.push(args))
  const first = state.requestOldAccess(42)
  await state.requestOldAccess(42)
  assert.equal(state.requestingOldAccessId.value, '42')
  resolve({ value: ' audit ' }); await first
  assert.equal(calls.length, 1)
  assert.deepEqual(calls[0][0], { certificateId: 42, requestType: 'VIEW_OLD_CERTIFICATE', purpose: 'audit' })
  assert.equal(state.requestingOldAccessId.value, '')
})
test('only dialog cancellation is handled; real failures propagate', async () => {
  for (const reason of ['cancel', 'close']) {
    const state = actions(async () => { throw reason }, () => assert.fail('must not submit'))
    await state.requestOldAccess(42)
    assert.equal(state.requestingOldAccessId.value, '')
  }
  const error = new Error('offline')
  await assert.rejects(actions(async () => { throw error }, () => {}).requestOldAccess(42), error)
  await assert.rejects(actions(async () => ({ value: 'audit' }), async () => { throw error }).requestOldAccess(42), error)
})
test('version list compiles, keys rows by version and routes selected version', () => {
  assert.deepEqual(compileTemplate({ id: 'version-access', source: descriptor.template.content, compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  assert.equal((source.match(/row-key="versionId"/g) || []).length, 2)
  assert.match(source, /v-hasPermi="\['dcc:registration-certificate:access-request:create'\]"/)
  const begin = script.content.indexOf('const openDetail =')
  const end = script.content.indexOf('const openChange', begin)
  let destination
  new Function('router', ts.transpileModule(script.content.slice(begin, end) + '\nopenDetail(42, 84)', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText)({ push(value) { destination = value } })
  assert.deepEqual(destination, { path: '/mdm/registration-certificate/detail/42', query: { versionId: '84' } })
})
