const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync(__dirname + '/RenewalDialog.vue', 'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const script = ts.transpileModule(source.replace(/^import[\s\S]*?from '[^']+'\r?$/gm, ''), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None } }).outputText
function load(type, valid = true) {
  const calls = []
  const props = { modelValue: true, certificate: { factsJson: JSON.stringify({ certificateType: type }), certificateId: 1, rowVersion: 2, versionId: 3 } }
  const context = {
    defineOptions() {}, defineProps: () => props, defineEmits: () => () => {},
    useMessage: () => ({ error() {}, success() {} }), ref: value => ({ value }), reactive: value => value,
    computed: getter => typeof getter === 'function' ? { get value() { return getter() } } : { get value() { return getter.get() }, set value(v) { getter.set(v) } },
    parseFacts: JSON.parse, FormData, Date, generateUUID: () => 'uuid',
    formatRegistrationCertificateDate: String, resolveRegistrationCertificateUserMessage: String,
    submitRegistrationCertificateRenewal: async (...args) => calls.push(args)
  }
  vm.createContext(context)
  vm.runInContext(script + ';globalThis.exposed={isCeRenewal,form,formRef,selectedFile,submit}', context)
  const e = context.exposed
  e.formRef.value = { validate: async callback => { callback(valid); return valid } }
  e.selectedFile.value = new Blob(['certificate'], { type: 'application/pdf' })
  Object.assign(e.form, { approvalDate: '2020-01-01', effectiveDate: '2020-02-01', expiryDate: '2030-01-01', certificateNo: 'CE-NEW' })
  return { e, calls }
}
test('CE factsJson selects the CE form and submits only its three editable fields', async () => {
  const { e, calls } = load('CE')
  assert.equal(e.isCeRenewal.value, true)
  await e.submit()
  assert.equal(calls.length, 1)
  const payload = calls[0][1]
  assert.equal(payload.get('approvalDate'), '2020-01-01')
  assert.equal(payload.get('expiryDate'), '2030-01-01')
  assert.equal(payload.get('certificateNo'), 'CE-NEW')
  for (const field of ['effectiveDate', 'categoryChanged', 'classification']) assert.equal(payload.has(field), false)
})
test('domestic renewal keeps three dates and category decision', async () => {
  const { e, calls } = load('DOMESTIC')
  assert.equal(e.isCeRenewal.value, false)
  await e.submit()
  assert.equal(calls.length, 1)
  const payload = calls[0][1]
  for (const field of ['approvalDate', 'effectiveDate', 'expiryDate', 'categoryChanged']) assert.equal(payload.has(field), true)
})
test('normal form validation failure returns without submission or rejected promise', async () => {
  const { e, calls } = load('CE', false)
  await assert.doesNotReject(e.submit())
  assert.equal(calls.length, 0)
})
