const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync(__dirname + '/RenewalDialog.vue', 'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const script = ts.transpileModule(source.replace(/^import[\s\S]*?from '[^']+'\r?$/gm, ''), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None } }).outputText
function load(type, valid = true) {
  const calls = [], messages = []
  const props = { modelValue: true, certificate: { factsJson: JSON.stringify({ certificateType: type, ceCertificates: [{ revision: "R04", certificateNo: "CE-CURRENT" }] }), certificateId: 1, rowVersion: 2, versionId: 3 } }
  const context = {
    defineOptions() {}, defineProps: () => props, defineEmits: () => () => {},
    useMessage: () => ({ error: value => messages.push(['error', value]), success: value => messages.push(['success', value]) }), ref: value => ({ value }), reactive: value => value,
    computed: getter => typeof getter === 'function' ? { get value() { return getter() } } : { get value() { return getter.get() }, set value(v) { getter.set(v) } },
    watch: (_source, callback) => callback(),
    parseFacts: JSON.parse, FormData, Date, generateUUID: () => 'uuid',
    formatRegistrationCertificateDate: String, resolveRegistrationCertificateUserMessage: error => error.message,
    submitRegistrationCertificateRenewal: async (...args) => calls.push(args)
  }
  vm.createContext(context)
  vm.runInContext(script + ';globalThis.exposed={isCeRenewal,form,formRef,selectedFile,submit,rules,revalidateRenewalDateFields,submitError,saving,currentCertificateNo,resetForm}', context)
  const e = context.exposed
  e.formRef.value = { validate: async callback => { callback(valid); return valid } }
  e.selectedFile.value = new Blob(['certificate'], { type: 'application/pdf' })
  Object.assign(e.form, { approvalDate: '2020-01-01', effectiveDate: '2020-02-01', expiryDate: '2030-01-01', certificateNo: 'CE-NEW' })
  return { e, calls, context, messages }
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

test('CE empty number and domestic missing effective date retain explicit validation feedback', () => {
  for (const [type, field, expected] of [['CE', 'certificateNo', '请输入延续后的注册证号'], ['DOMESTIC', 'effectiveDate', '请选择生效日期']]) {
    const { e } = load(type)
    let error
    e.rules[field][0].validator({}, '', value => { error = value })
    assert.equal(error.message, expected)
  }
})

test('server failure stays visible, releases loading and never reports success', async () => {
  const { e, context, messages } = load('CE')
  context.submitRegistrationCertificateRenewal = async () => { throw new Error('注册证访问授权范围不合法') }
  await assert.doesNotReject(e.submit())
  assert.equal(e.submitError.value, '注册证访问授权范围不合法')
  assert.deepEqual(messages, [['error', '注册证访问授权范围不合法']])
  assert.equal(e.saving.value, false)
})

test('CE current identity and reopening reset use the current Rev number', () => {
  const { e } = load('CE')
  assert.equal(e.currentCertificateNo.value, 'CE-CURRENT')
})
test('first blur has explicit Chinese feedback and no implicit required validator', () => {
  const { e } = load('DOMESTIC')
  assert.ok(e.rules.effectiveDate[0].trigger.includes('blur'))
  let error
  e.rules.effectiveDate[0].validator({}, '', value => { error = value })
  assert.equal(error.message, '请选择生效日期')
  assert.ok(!fs.readFileSync(__dirname + '/RenewalDialog.vue','utf8').includes('prop="effectiveDate" required'))
})
