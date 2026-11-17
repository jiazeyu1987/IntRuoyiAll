const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const { ref, computed } = require('vue')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/components/DeviationResponsibilityRecovery.vue'), 'utf8')
function method(name, context, code = source) {
  const ast = ts.createSourceFile('component.ts', parse(code).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  const js = ts.transpileModule(declaration.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), `${js}; return ${name}`)(...Object.values(context))
}
function setup(mode = 'assign') {
  const sent = [], events = []
  const ctx = { props: { mode, deviationId: 7, deviationVersion: 1, contentVersion: 5, contentHash: 'hash5' },
    visible: ref(true), submitting: ref(false), loading: ref(false), loaded: ref(false), error: ref(''), candidateError: ref(''),
    form: { receiverId: 21, reason: '  根据责任分工调整  ' }, candidates: ref([{ id: 21, username: 'receiver', nickname: '接收人' }]),
    guard: { capture: () => () => true }, emit: event => events.push(event), message: { success: text => events.push(text) },
    assignDeviationReceiver: async value => { sent.push(value); return true }, withdrawDeviationInvestigation: async value => { sent.push(value) } }
  return { ctx, sent, events }
}
test('receiver assignment requires an explicitly selected authoritative candidate and reason', async () => {
  const { ctx, sent } = setup()
  ctx.form.reason = ' '; await method('submit', ctx)(); assert.equal(sent.length, 0)
  ctx.form.reason = '按责任分工'; ctx.form.receiverId = undefined
  await method('submit', ctx)(); assert.equal(sent.length, 0)
  ctx.form.receiverId = 999; await method('submit', ctx)(); assert.equal(sent.length, 0)
  ctx.form.receiverId = 21; await method('submit', ctx)()
  assert.deepEqual(sent[0], { deviationId: 7, expectedDeviationVersion: 1, receiverId: 21, reason: '按责任分工' })
})
test('assignment rejects candidate failure and invalid version without sending request', async () => {
  const { ctx, sent } = setup(); ctx.candidateError.value = '服务不可用'
  await method('submit', ctx)(); ctx.candidateError.value = ''; ctx.props.deviationVersion = undefined
  await method('submit', ctx)(); assert.equal(sent.length, 0)
})
test('withdrawal binds current content version/hash and preserves failed dialog', async () => {
  const { ctx, sent, events } = setup('withdraw')
  ctx.withdrawDeviationInvestigation = async () => { throw new Error('内容已变化') }
  await method('submit', ctx)(); assert.equal(ctx.error.value, '内容已变化'); assert.equal(ctx.visible.value, true); assert.deepEqual(events, [])
  ctx.withdrawDeviationInvestigation = async value => sent.push(value)
  await method('submit', ctx)()
  assert.deepEqual(sent[0], { deviationId: 7, expectedContentVersion: 5, expectedContentHash: 'hash5', reason: '根据责任分工调整' })
  assert.ok(events.includes('changed')); assert.equal(ctx.visible.value, false)
})
test('stale session or record response never closes dialog or refreshes another record', async () => {
  const { ctx, events } = setup('withdraw')
  ctx.guard.capture = () => () => false
  await method('submit', ctx)(); assert.deepEqual(events, []); assert.equal(ctx.visible.value, true)
})
test('a false assignment result is an explicit error instead of success', async () => {
  const { ctx, events } = setup(); ctx.assignDeviationReceiver = async () => false
  await method('submit', ctx)(); assert.match(ctx.error.value, /未成功/); assert.deepEqual(events, []); assert.equal(ctx.visible.value, true)
})
test('candidate loading distinguishes failure and empty, discards changed context, never selects user', async () => {
  const { ctx } = setup(); let key = 'tenant-1/7'; ctx.candidateGeneration = 0; ctx.context = () => key
  ctx.getDeviationRecoveryReceivers = async () => []; ctx.form.receiverId = undefined
  await method('loadCandidates', ctx)(); assert.equal(ctx.loaded.value, true); assert.deepEqual(ctx.candidates.value, []); assert.equal(ctx.form.receiverId, undefined)
  ctx.getDeviationRecoveryReceivers = async () => { throw new Error('资格查询失败') }
  await method('loadCandidates', ctx)(); assert.equal(ctx.candidateError.value, '资格查询失败'); assert.equal(ctx.loaded.value, false)
  let complete; ctx.getDeviationRecoveryReceivers = () => new Promise(resolve => { complete = resolve })
  const loading = method('loadCandidates', ctx)(); key = 'tenant-2/7'; complete([{ id: 21 }]); await loading
  assert.deepEqual(ctx.candidates.value, [])
})
test('recovery component compiles its script and template', () => {
  const { descriptor, errors } = parse(source); assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'recovery' })
  const template = compileTemplate({ id: 'recovery', source: descriptor.template.content, filename: 'recovery.vue', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
})
const detailSource = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/DeviationDetail.vue'), 'utf8')
const correctiveSource = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/components/DeviationCorrectivePanel.vue'), 'utf8')
test('detail only offers missing receiver assignment on open writable records with handling permission', () => {
  const ctx = { computed, props: { readonly: false }, detail: ref({ status: 'OPEN' }), checkPermi: () => true }
  const can = method('canAssignReceiver', ctx, detailSource)
  assert.equal(can.value, true)
  ctx.detail.value.receiverId = 21; assert.equal(can.value, false)
  ctx.detail.value = { status: 'CLOSED' }; assert.equal(can.value, false)
  ctx.detail.value = { status: 'OPEN' }; ctx.props.readonly = true
  assert.equal(method('canAssignReceiver', ctx, detailSource).value, false)
  ctx.props.readonly = false; ctx.checkPermi = () => false
  assert.equal(method('canAssignReceiver', ctx, detailSource).value, false)
})
test('withdrawal is limited to assigned receiver and submitted open record', () => {
  const ctx = { computed, props: { readonly: false }, detail: ref({ status: 'OPEN', receiverId: 21 }), signatureUser: ref({ id: 21 }), handling: ref({ investigationSubmitted: true }), checkPermi: () => true }
  ctx.isInvestigationReceiver = method('isInvestigationReceiver', ctx, detailSource)
  const can = method('canWithdrawInvestigation', ctx, detailSource)
  assert.equal(can.value, true)
  ctx.signatureUser.value.id = 22; assert.equal(can.value, false)
  ctx.signatureUser.value.id = 21; ctx.handling.value.investigationSubmitted = false; assert.equal(can.value, false)
  ctx.handling.value.investigationSubmitted = true; ctx.detail.value.status = 'CLOSED'; assert.equal(can.value, false)
})
test('closed legacy records show historical status without inferring verification or reopening actions', async () => {
  const props = { closed: true, record: { investigationSubmitted: false } }
  assert.equal(method('statusLabel', { computed, props }, correctiveSource).value, '历史记录（已关闭）')
  props.record.correctiveStatus = 'VERIFIED'
  assert.equal(method('statusLabel', { computed, props }, correctiveSource).value, '已关闭')
  props.record.canVerifyCorrective = true; props.record.canSubmitCorrective = true
  let called = false
  const ctx = { props, perform: async () => { called = true } }
  await method('verify', ctx, correctiveSource)(true)
  await method('submit', ctx, correctiveSource)()
  assert.equal(called, false)
})
test('integrated detail and corrective components compile current templates', () => {
  for (const [name, code] of [['Detail', detailSource], ['Corrective', correctiveSource]]) {
    const { descriptor, errors } = parse(code); assert.deepEqual(errors, [])
    const script = compileScript(descriptor, { id: name })
    assert.deepEqual(compileTemplate({ id: name, source: descriptor.template.content, filename: name + '.vue', compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  }
})
