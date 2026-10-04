import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { parse, compileScript, compileTemplate } from 'vue/compiler-sfc'

const model = fs.readFileSync(new URL('../src/views/dcc/controlled-file/revision/revision-model.ts', import.meta.url), 'utf8')
const attributesSource = fs.readFileSync(new URL('../src/views/dcc/controlled-file/project-attributes/state.ts', import.meta.url), 'utf8')
const attributesContext = { exports: {} }
vm.createContext(attributesContext)
vm.runInContext(ts.transpileModule(attributesSource, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, attributesContext)
const context = { exports: {}, require: name => {
  assert.equal(name, '../project-attributes/state')
  return attributesContext.exports
} }
vm.createContext(context)
vm.runInContext(ts.transpileModule(model, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, context)
const { revisionChangeLabel, buildRevisionCommand, requireRevisionId, revisionCheckoutLabel } = context.exports
const application = () => ({projectAttributes:{targetMarkets:['CE'],licenseHolder:'Y',actualManufacturer:'N',documentTransfer:'N'},selectedSignoffDepartmentIds:['101','102'],needTraining:false,effectiveDate:'2026-12-20'})

test('all formal types display actual intent regardless of A/2 or B/1', () => {
  assert.equal(revisionChangeLabel('INITIAL'), '初始上传')
  assert.equal(revisionChangeLabel('PARTIAL'), '局部变更')
  assert.equal(revisionChangeLabel('REPLACEMENT'), '换版变更')
  assert.equal(revisionChangeLabel(null), '未记录')
  assert.equal(revisionChangeLabel('legacy'), '未记录')
})
test('selected earlier iteration and exact Long identity survive submission', () => {
  const result = buildRevisionCommand('9007199254740993', '9007199254740995', 'PARTIAL', ' earlier body ', ' key ', application())
  assert.equal(result.controlledBaselineId, '9007199254740993')
  assert.equal(result.selectedIterationId, '9007199254740995')
  assert.equal(result.revisionChangeType, 'PARTIAL')
  assert.equal(result.changeDescription, 'earlier body')
})
test('formal command freezes actual application attributes departments and training choice', () => {
  const actual=application()
  const command=buildRevisionCommand('1','2','PARTIAL','reason','key',actual)
  actual.projectAttributes.targetMarkets.push('FDA');actual.selectedSignoffDepartmentIds.push('103');actual.needTraining=true;actual.effectiveDate='2027-01-01'
  assert.deepEqual(Array.from(command.projectAttributes.targetMarkets),['CE'])
  assert.deepEqual(Array.from(command.selectedSignoffDepartmentIds),['101','102'])
  assert.equal(command.needTraining,false)
  assert.equal(command.effectiveDate,'2026-12-20')
})
test('formal command requires a real application context and rejects repeated departments', () => {
  assert.throws(() => buildRevisionCommand('1','2','PARTIAL','reason','key'))
  const actual=application();actual.selectedSignoffDepartmentIds=['101','101']
  assert.throws(() => buildRevisionCommand('1','2','PARTIAL','reason','key',actual))
})
test('illegal intent, empty context and unsafe numerical identities fail explicitly', () => {
  assert.throws(() => buildRevisionCommand('1', '2', 'MINOR', 'reason', 'key'))
  assert.throws(() => buildRevisionCommand('1', '2', 'PARTIAL', '', 'key'))
  assert.throws(() => buildRevisionCommand('1', '2', 'PARTIAL', 'reason', ''))
  assert.throws(() => requireRevisionId(9007199254740993))
  assert.throws(() => requireRevisionId('0'))
})

const componentSource = fs.readFileSync(new URL('../src/views/dcc/controlled-file/revision/DccRevisionPanel.vue', import.meta.url), 'utf8')
const { descriptor } = parse(componentSource)
test('independent Revision SFC compiles script and template', () => {
  const script = compileScript(descriptor, { id: 'dcc-revision' })
  const template = compileTemplate({ id: 'dcc-revision', source: descriptor.template.content, filename: 'DccRevisionPanel.vue', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
})
test('checkout banner renders the authoritative checkout time as well as actor', () => {
  const banner = descriptor.template.content.match(/<el-alert v-if="checkedOutBy"[^>]+>/)?.[0]
  assert.ok(banner)
  assert.match(banner, /checkedOutTime/)
  assert.match(banner, /revisionCheckoutLabel/)
  assert.match(banner, /checkedOutBy, checkedOutByName, checkedOutTime/)
})
test('selected body preview slot is bound to the actual selected iteration identity', () => {
  const slot=descriptor.template.content.match(/<slot\b[^>]*name="selected-body"[^>]*>/)?.[0]
  assert.ok(slot)
  assert.match(slot, /selectedIteration\?\.canPreview/)
  assert.match(slot, /:selected-iteration-id="selectedId"/)
  assert.match(slot, /:baseline-id="baselineId"/)
})
const setupComponent = submitRevision => {
  const props = { baselineId: '9007199254740993', idempotencyKey: 'key', baselineVersionNo: 'A/1', partialTarget: { versionNo: 'A/2', unavailableReason: null }, replacementTarget: { versionNo: 'B/1', unavailableReason: null }, facts: {}, applicationFacts: application(), iterations: [{ id: '2', versionNo: 'A/1-1', canPartial: true, canReplacement: false, canPreview: true }], canPartial: true, canReplacement: false, submitRevision }
  const emitted = []
  const c = { Error, buildRevisionCommand, revisionChangeLabel, revisionCheckoutLabel,
    defineProps: () => props, defineEmits: () => (...args) => emitted.push(args),
    ref: value => ({ value }), computed: getter => ({ get value() { return getter() } }), watch: () => {} }
  vm.createContext(c)
  const code = descriptor.scriptSetup.content.replace(/^import.*$/gm, '') + '\nglobalThis.state = { submit, selectedId, intent, description, busy, submissionError };'
  vm.runInContext(ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  c.state.selectedId.value = '2'; c.state.intent.value = 'PARTIAL'; c.state.description.value = 'earlier text'
  return { props, state: c.state, emitted }
}
test('failed formal submission keeps selection and exposes the real error', async () => {
  const { state, emitted } = setupComponent(async () => { throw new Error('source offline') })
  await state.submit()
  assert.equal(state.submissionError.value, 'source offline')
  assert.equal(state.selectedId.value, '2'); assert.equal(state.description.value, 'earlier text')
  assert.equal(state.busy.value, false); assert.equal(emitted.length, 0)
})
test('successful formal submission emits the selected identity after the callback succeeds', async () => {
  let command
  const { state, emitted } = setupComponent(async value => { command = value })
  await state.submit()
  assert.equal(command.selectedIterationId, '2'); assert.equal(command.revisionChangeType, 'PARTIAL')
  assert.equal(emitted.length, 1); assert.equal(emitted[0][0], 'submitted')
})
test('repeated clicks cannot submit while first request is pending', async () => {
  let finish; let writes = 0
  const { state } = setupComponent(() => { writes++; return new Promise(resolve => { finish = resolve }) })
  const first = state.submit(); await state.submit()
  assert.equal(writes, 1); finish(); await first
})
test('current permissions, checkout lock and selected visibility block writes', async () => {
  let writes = 0
  for (const alter of [p => { p.canPartial = false }, p => { p.lockedReason = '甲检出中' }, p => { p.checkedOutBy = '88' }, p => { p.iterations[0].canPartial = false }]) {
    const { state, props, emitted } = setupComponent(async () => { writes++ })
    alter(props); await state.submit()
    assert.ok(state.submissionError.value); assert.equal(emitted.length, 0)
  }
  assert.equal(writes, 0)
})
test('late success cannot mark a different selected baseline as submitted', async () => {
  let finish
  const { state, props, emitted } = setupComponent(() => new Promise(resolve => { finish = resolve }))
  const pending = state.submit(); props.baselineId = '9'; finish(); await pending
  assert.equal(emitted.length, 0)
})

test('formal panel displays the server baseline and target without inventing arithmetic', () => {
  assert.match(descriptor.template.content, /baselineVersionNo/)
  assert.match(descriptor.template.content, /targetVersion/)
})
test('server target absence and per-iteration intent permission stop formal submission', async () => {
  let writes = 0
  for (const alter of [p => { p.partialTarget.versionNo = null; p.partialTarget.unavailableReason = 'REVISION_AFTER_Z_UNDEFINED' }, p => { p.iterations[0].canPartial = false; p.iterations[0].canReplacement = true }]) {
    const { state, props, emitted } = setupComponent(async () => { writes++ })
    alter(props); await state.submit()
    assert.ok(state.submissionError.value); assert.equal(emitted.length, 0)
  }
  assert.equal(writes, 0)
})

test('formal command refuses missing and invalid calendar dates instead of inheriting old version date', () => {
  for (const date of [undefined, '', '2026-02-30', '2026-04-31', '1900-02-29', '2026-13-01', '2026-01-01T00:00:00Z', '0000-01-01']) {
    const actual = application(); actual.effectiveDate = date
    assert.throws(() => buildRevisionCommand('1','2','PARTIAL','reason','key',actual))
  }
  const actual = application(); actual.effectiveDate = '2028-02-29'
  assert.equal(buildRevisionCommand('1','2','PARTIAL','reason','key',actual).effectiveDate, '2028-02-29')
})

test('pending submission keeps its clicked effective date when the parent edits a later application', async () => {
  let finish; let command
  const { state, props, emitted } = setupComponent(value => {
    command = value; return new Promise(resolve => { finish = resolve })
  })
  const pending = state.submit()
  props.applicationFacts.effectiveDate = '2027-01-01'
  assert.equal(command.effectiveDate, '2026-12-20')
  finish(); await pending
  assert.equal(emitted[0][1].effectiveDate, '2026-12-20')
})

test('checkout banner preserves authoritative actor identity and timestamp when the directory name is missing', () => {
  assert.equal(revisionCheckoutLabel('9007199254740993', undefined, '2026-10-01 18:00:00'),
    '检出账号：9007199254740993；检出人：未记录；检出时间：2026-10-01 18:00:00')
  assert.equal(revisionCheckoutLabel(undefined, undefined, undefined), '')
  assert.throws(() => revisionCheckoutLabel(9007199254740993, 'actor', 'time'))
})
