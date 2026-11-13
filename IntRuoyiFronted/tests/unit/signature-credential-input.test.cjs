const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const renderer = vue.createRenderer({
  createComment: () => ({}), insert() {}, remove() {}, parentNode() {}, nextSibling() {}
})
function mountScript(file, props, modules = {}) {
  const { descriptor } = parse(fs.readFileSync(file, 'utf8'))
  const source = compileScript(descriptor, { id: file }).content
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const exports = {}
  new Function('require', 'exports', js)(name => name === 'vue' ? vue : modules[name], exports)
  const values = vue.reactive(props)
  const emitted = []
  let state
  const app = renderer.createApp({ setup() {
    state = exports.default.setup(values, { expose() {}, emit(name, value) { emitted.push(value); values.modelValue = value } })
    return () => null
  } })
  app.mount({})
  return { state, props: values, emitted, close: () => app.unmount() }
}
test('six PIN cells accept digits only, preserve leading zeros, and paste exactly six digits', () => {
  const h = mountScript('src/components/SignatureCredentialInput/PinCodeInput.vue', { modelValue: '' })
  for (const [index, value] of [...'012345'].entries()) h.state.input(index, { target: { value } })
  assert.equal(h.props.modelValue, '012345')
  h.state.input(0, { target: { value: 'x' } })
  assert.equal(h.props.modelValue, '12345')
  h.state.paste({ clipboardData: { getData: () => '1234567' } })
  assert.equal(h.props.modelValue, '12345')
  h.state.paste({ clipboardData: { getData: () => '000012' } })
  assert.equal(h.props.modelValue, '000012')
  h.state.keydown(0, { key: 'Backspace' })
  h.close()
})
const modules = getSignatureSettings => ({
  './PinCodeInput.vue': {},
  '@/api/system/user/profile': { getSignatureSettings },
  '@/api/mes/pro/employeeSignatureCredential': { getEmployeeSignatureQualification: async () => ({ credentialType: 'PIN' }) }
})
const settle = async () => { await Promise.resolve(); await vue.nextTick() }
global.window = new EventTarget()
for (const type of ['PASSWORD', 'PIN']) test(`signing input reads ${type} and refreshes after personal settings are saved`, async () => {
  let credentialType = type
  const h = mountScript('src/components/SignatureCredentialInput/SignatureCredentialInput.vue', { modelValue: 'old' }, modules(async () => ({ credentialType })))
  await settle()
  assert.equal(h.state.resolvedType.value, type)
  assert.equal(h.props.modelValue, '')
  credentialType = type === 'PIN' ? 'PASSWORD' : 'PIN'
  window.dispatchEvent(new Event('signature-settings-changed'))
  await settle()
  assert.equal(h.state.resolvedType.value, credentialType)
  h.close()
})
test('settings lookup failure blocks input and does not assume another method', async () => {
  const h = mountScript('src/components/SignatureCredentialInput/SignatureCredentialInput.vue', { modelValue: 'old' }, modules(async () => { throw new Error('Settings unavailable') }))
  await settle()
  assert.equal(h.state.resolvedType.value, undefined)
  assert.equal(h.state.error.value, 'Settings unavailable')
  assert.equal(h.props.modelValue, '')
  h.close()
})
test('an older settings response cannot override a newly selected employee scope', async () => {
  let resolveOld
  const h = mountScript('src/components/SignatureCredentialInput/SignatureCredentialInput.vue', { modelValue: '' }, modules(() => new Promise(resolve => { resolveOld = resolve })))
  h.props.employeeScope = { employeeProfileId: 7 }
  await settle(); await settle()
  assert.equal(h.state.resolvedType.value, 'PIN')
  resolveOld({ credentialType: 'PASSWORD' })
  await settle()
  assert.equal(h.state.resolvedType.value, 'PIN')
  h.close()
})
test('unknown signature method blocks the field without choosing a default', async () => {
  const h = mountScript('src/components/SignatureCredentialInput/SignatureCredentialInput.vue', { modelValue: 'old' }, modules(async () => ({ credentialType: 'UNKNOWN' })))
  await settle()
  assert.equal(h.state.resolvedType.value, undefined)
  assert.ok(h.state.error.value)
  assert.equal(h.props.modelValue, '')
  h.close()
})
test('clearing a PIN from the parent clears all six cells; disabled cells reject paste', () => {
  const h = mountScript('src/components/SignatureCredentialInput/PinCodeInput.vue', { modelValue: '012345', disabled: true })
  h.state.paste({ clipboardData: { getData: () => '654321' } })
  assert.equal(h.props.modelValue, '012345')
  h.props.modelValue = ''
  assert.deepEqual([...h.state.digits.value], ['', '', '', '', '', ''])
  h.close()
})
test('signing PIN exposes a credential only after all six cells are filled', () => {
  const h = mountScript('src/components/SignatureCredentialInput/PinCodeInput.vue', { modelValue: '', completeOnly: true })
  h.state.input(0, { target: { value: '0' } })
  assert.equal(h.props.modelValue, '')
  assert.equal(h.state.digits.value[0], '0')
  for (const [index, value] of [...'12345'].entries()) h.state.input(index + 1, { target: { value } })
  assert.equal(h.props.modelValue, '012345')
  h.state.input(2, { target: { value: '' } })
  assert.equal(h.props.modelValue, '')
  assert.deepEqual([...h.state.digits.value], ['0', '1', '', '3', '4', '5'])
  h.close()
})

test('production signature reads selected formal worker and reloads on employee changes', async () => {
  const calls = []
  const deps = modules(async () => { throw new Error('Must not read device operator settings') })
  deps['@/api/mes/pro/employeeSignatureCredential'].getFrontlineSignatureSettings = async scope => {
    calls.push(scope.actualEmployeeId)
    return { credentialType: scope.actualEmployeeId === 7 ? 'PIN' : 'PASSWORD' }
  }
  const h = mountScript('src/components/SignatureCredentialInput/SignatureCredentialInput.vue', {
    modelValue: 'old', frontlineScope: { actualEmployeeId: 7, identityDomain: 'SYSTEM_USER' }
  }, deps)
  await settle()
  assert.equal(h.state.resolvedType.value, 'PIN')
  h.props.modelValue = '888888'
  h.props.frontlineScope = { actualEmployeeId: 8, identityDomain: 'SYSTEM_USER' }
  await settle(); await settle()
  assert.equal(h.props.modelValue, '')
  assert.equal(h.state.resolvedType.value, 'PASSWORD')
  assert.deepEqual(calls, [7, 8])
  h.close()
})

test('selected worker lookup failure blocks signing instead of using current login settings', async () => {
  let loginLookups = 0
  const deps = modules(async () => { loginLookups++; return { credentialType: 'PASSWORD' } })
  deps['@/api/mes/pro/employeeSignatureCredential'].getFrontlineSignatureSettings = async () => { throw new Error('Employee unavailable') }
  const h = mountScript('src/components/SignatureCredentialInput/SignatureCredentialInput.vue', {
    modelValue: 'old', frontlineScope: { actualEmployeeId: 7, identityDomain: 'SYSTEM_USER' }
  }, deps)
  await settle()
  assert.equal(h.state.resolvedType.value, undefined)
  assert.equal(h.state.error.value, 'Employee unavailable')
  assert.equal(h.props.modelValue, '')
  assert.equal(loginLookups, 0)
  h.close()
})
