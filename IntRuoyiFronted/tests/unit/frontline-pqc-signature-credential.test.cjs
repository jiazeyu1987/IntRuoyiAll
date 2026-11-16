const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const { ref } = require('vue')

const source = fs.readFileSync('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue', 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', script, ts.ScriptTarget.Latest, true)
function harness(getSignatureSettings) {
  const errors = []
  let current = true
  const deps = {
    getSignatureSettings, payloadLoading: ref(false), hasReturnNotification: ref(false),
    isInitialHandoffBlocked: ref(false), pqcSubmitResultUncertain: ref(false), isPqcMode: ref(true),
    clearFrontlineError() {}, showFrontlineError: error => errors.push(error),
    assertPqcFormalSubmissionReady() {}, assertPqcSignatureAndQuantityReady() {},
    assertPqcCurrentProcessAllMethodSubmissionReady() {}, assertPqcCurrentProcessInspectionDisplayFieldsReady() {},
    draft: { fieldValues: {} }, buildPqcFieldValues: () => ({}), payloadPreview: ref(),
    pqcSignaturePassword: ref(''), pqcSignatureCredentialType: ref(), pqcSignatureDialogVisible: ref(false),
    pqcSubmitGuard: { capture: () => () => current },
    approvalInvalidation: { syncSession: () => ({ key: 'test-session' }) },
    // Stop before writing business data: these tests exercise credential selection and validation only.
    buildPqcInspectionSubmitPayloads() { throw new Error('Reached payload construction') },
    cloneDeep: value => value
  }
  const names = ['handleValidate', 'handleConfirmPqcSubmit', 'handlePqcSignatureCredentialUpdate']
  const statements = ast.statements.filter(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
  assert.equal(statements.length, names.length)
  const code = ts.transpileModule(statements.map(s => s.getText(ast)).join('\n') +
    `\nreturn { ${names.join(', ')} }`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return { ...deps, errors, invalidate: () => { current = false },
    ...new Function(...Object.keys(deps), code)(...Object.values(deps)) }
}

for (const credentialType of ['PIN', 'PASSWORD']) {
  test(`PQC reads the account's configured ${credentialType} method before opening the dialog`, async () => {
    const h = harness(async () => ({ credentialType }))
    h.pqcSignaturePassword.value = 'previous-credential'
    await h.handleValidate()
    assert.equal(h.pqcSignatureCredentialType.value, credentialType)
    assert.equal(h.pqcSignatureDialogVisible.value, true)
    assert.equal(h.pqcSignaturePassword.value, '')
    assert.equal(h.payloadLoading.value, false)
    assert.deepEqual(h.errors, [])
  })
}

test('failed settings lookup blocks signing and surfaces the actual error', async () => {
  const failure = new Error('Settings unavailable')
  const h = harness(async () => { throw failure })
  await h.handleValidate()
  assert.equal(h.pqcSignatureDialogVisible.value, false)
  assert.equal(h.pqcSignatureCredentialType.value, undefined)
  assert.deepEqual(h.errors, [failure])
  assert.equal(h.payloadLoading.value, false)
})

test('missing or unknown settings never assume password or PIN mode', async () => {
  for (const credentialType of [undefined, 'UNKNOWN']) {
    const h = harness(async () => ({ credentialType }))
    await h.handleValidate()
    assert.equal(h.pqcSignatureDialogVisible.value, false)
    assert.equal(h.errors.length, 1)
  }
})

test('a response from an old PQC context cannot open its signature dialog', async () => {
  let resolve
  const h = harness(() => new Promise(done => { resolve = done }))
  const pending = h.handleValidate()
  h.invalidate()
  resolve({ credentialType: 'PIN' })
  await pending
  assert.equal(h.pqcSignatureDialogVisible.value, false)
  assert.equal(h.pqcSignatureCredentialType.value, undefined)
})

test('PIN mode rejects login passwords and malformed PINs before submitting', async () => {
  for (const input of ['Example@2026', '12345', '1234567', ' 000001', '１２３４５６']) {
    const h = harness(async () => ({ credentialType: 'PIN' }))
    await h.handleValidate()
    h.pqcSignaturePassword.value = input
    await h.handleConfirmPqcSubmit()
    assert.match(h.errors[0], /六位数字 PIN/)
  }
})

test('valid PINs retain leading zeros and password mode retains the complete password', async () => {
  for (const [credentialType, input] of [['PIN', '000001'], ['PASSWORD', 'Example@2026!']]) {
    const h = harness(async () => ({ credentialType }))
    await h.handleValidate()
    h.pqcSignaturePassword.value = input
    await h.handleConfirmPqcSubmit()
    assert.equal(h.errors[0].message, 'Reached payload construction')
    assert.equal(h.pqcSignaturePassword.value, input)
  }
})

test('completing six PIN digits automatically confirms once with leading zeros intact', async () => {
  const h = harness(async () => ({ credentialType: 'PIN' }))
  await h.handleValidate()
  for (const value of ['', '0', '01', '012', '0123', '01234']) {
    await h.handlePqcSignatureCredentialUpdate(value)
    assert.deepEqual(h.errors, [])
  }
  await h.handlePqcSignatureCredentialUpdate('012345')
  assert.equal(h.pqcSignaturePassword.value, '012345')
  assert.equal(h.errors.length, 1)
  assert.equal(h.errors[0].message, 'Reached payload construction')
  await h.handlePqcSignatureCredentialUpdate('012345')
  assert.equal(h.errors.length, 1)
})

test('passwords, malformed PINs, closed dialogs and in-flight submits never auto-confirm', async () => {
  for (const state of ['PASSWORD', 'invalid', 'closed', 'loading']) {
    const h = harness(async () => ({ credentialType: state === 'PASSWORD' ? 'PASSWORD' : 'PIN' }))
    await h.handleValidate()
    if (state === 'closed') h.pqcSignatureDialogVisible.value = false
    if (state === 'loading') h.payloadLoading.value = true
    await h.handlePqcSignatureCredentialUpdate(state === 'invalid' ? '12345x' : '123456')
    assert.deepEqual(h.errors, [])
  }
})
