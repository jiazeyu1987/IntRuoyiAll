const { test } = require('node:test'), assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const root = require('node:path').resolve(__dirname, '../..')
function load(file, request = {}) {
  const c = { exports: {}, Error, Date, BigInt, Set, JSON, require: id => {
    if (id === '@/config/axios') return { default: request }
    if (id.endsWith('approval-matrix')) return require('./dcc-upload-test-loader.cjs').loadUploadMatrix()
    if (id.endsWith('signoff-departments')) return require('./dcc-upload-test-loader.cjs').loadUploadDepartments()
    if (id.endsWith('project-attributes/state')) return { validateAttributes: value => value }
    throw new Error(id)
  } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(root + '/src/' + file, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, c)
  return c.exports
}
const attributes = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
test('DELAYED accepts absent upload time and never accepts a preset date', () => {
  const helper = load('views/dcc/controlled-file/upload/submitter.ts')
  const selected = helper.buildUploadEffectiveChoice('DELAYED', null, '2026-10-14')
  assert.equal(selected.effectiveMode, 'DELAYED'); assert.equal(selected.effectiveDate, null)
  for (const date of ['', '2026-10-15']) assert.throws(() => helper.buildUploadEffectiveChoice('DELAYED', date, '2026-10-14'))
})
test('durable delayed draft restores its saved mode without manufacturing a time', async () => {
  const saved = { controlledFileId: '20', projectId: '5', applicationType: 'UPLOAD', actual: attributes, defaultSource: attributes,
    effectiveMode: 'DELAYED', effectiveDate: null, needTraining: false, selectedSignoffDepartmentIds: ['7'],
    selectedDistributionDepartmentIds: [], canSubmit: true, unavailableReason: null, changeDescription: '',
    executionContract: 'PUBLIC_MATRIX_NATIVE_V2', preparedProcessDefinitionId: 'dcc-controlled-file-upload:6:real', preparedDefinitionDigest: 'a'.repeat(64) }
  const api = load('api/dcc/controlledFile/applicationRead.ts', { get: async () => saved })
  const result = await api.getWorkingApplicationAttributes('20')
  assert.equal(result.effectiveMode, 'DELAYED'); assert.equal(result.effectiveDate, null)
})
test('delayed save carries null date and rejects an upload-time calendar value before POST', async () => {
  const calls = [], api = load('api/dcc/controlledFile/applicationRead.ts', { post: async req => { calls.push(req); return true } })
  const selection = { effectiveMode: 'DELAYED', effectiveDate: null, selectedSignoffDepartmentIds: ['7'], selectedDistributionDepartmentIds: [], needTraining: false, changeDescription: '' }
  assert.equal(await api.saveWorkingApplicationAttributes('20', attributes, selection), true)
  assert.equal(calls[0].data.effectiveMode, 'DELAYED'); assert.equal(calls[0].data.effectiveDate, null)
  await assert.rejects(api.saveWorkingApplicationAttributes('20', attributes, { ...selection, effectiveDate: '2026-10-15' }))
  assert.equal(calls.length, 1)
})
test('new exact V6 round is accepted only with its corresponding V2 execution contract', () => {
  const api = load('api/dcc/controlledFile/applicationRead.ts')
  const row = { controlledFileId: '20', applicationType: 'REVISION', attributeRound: 2, bpmRound: 'R', flowContract: {
    tenantId: '1', controlledFileId: '20', applicationType: 'REVISION', attributeRound: 2, bpmRound: 'R', status: 'READY', unavailableReason: null,
    processDefinitionKey: 'dcc-controlled-file-revision', processDefinitionId: 'dcc-controlled-file-revision:6:real', deploymentId: 'deployment',
    definitionDigest: 'b'.repeat(64), executionContract: 'PUBLIC_MATRIX_NATIVE_V2', flowRule: 'PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM',
    needTraining: true, formInstanceId: null, formInstanceCode: null } }
  assert.equal(api.validateControlledFileRoundFlowContract(row).flowRule, 'PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM')
  assert.throws(() => api.validateControlledFileRoundFlowContract({ ...row, flowContract: { ...row.flowContract, executionContract: 'PUBLIC_MATRIX_NATIVE_V1' } }))
})
test('matrix V2 execution preserves V1 public derivation and exact saved historical V1 cannot silently upgrade', () => {
  const m = require('./dcc-upload-test-loader.cjs'), api = m.loadUploadMatrix()
  const scope = { tenantId: '1', projectId: '5', categoryId: '2', taxonomyId: '4', actionType: 'NEW', applicationContextKey: 'K' }
  const ready = m.matrixReadiness({ ready: true, blockers: [], nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'] }] })
  const v2 = { ...ready, executionContract: 'PUBLIC_MATRIX_NATIVE_V2', preparedProcessDefinitionId: 'dcc-controlled-file-upload:6:exact', derivationVersion: 'DCC_PUBLIC_MATRIX_NATIVE_V1' }
  const state = new api.UploadApprovalMatrixState(scope), request = state.begin()
  state.accept(request, v2); assert.equal(state.selection().executionContract, 'PUBLIC_MATRIX_NATIVE_V2')
  const history = new api.UploadApprovalMatrixState({ ...scope, controlledFileId: '20' }, ready.matrixBefore, {
    controlledFileId: '20', projectId: '5', executionContract: 'PUBLIC_MATRIX_NATIVE_V1', preparedProcessDefinitionId: ready.preparedProcessDefinitionId, preparedDefinitionDigest: ready.preparedDefinitionDigest })
  assert.equal(history.begin().request.executionContract, 'PUBLIC_MATRIX_NATIVE_V1')
  assert.throws(() => history.accept(history.begin(), v2))
})
test('delayed display keeps selection, actual effectiveness and daily activation independent', () => {
  const facts = load('views/dcc/controlled-file/workflow/effective-facts.ts').effectiveFacts({ effectiveMode: 'DELAYED', effectiveDate: null,
    delayedEffectiveTime: '2026-11-01 10:00:00', delayedEffectiveConfirmedAt: '2026-10-14 12:30:00.123', effectiveTime: null, activatedTime: null })
  assert.equal(facts.modeText, '延迟生效'); assert.equal(facts.presetDateVisible, false)
  assert.equal(facts.delayedEffectiveText, '2026-11-01 10:00:00'); assert.equal(facts.effectiveText, '尚未生效')
  assert.equal(facts.activatedText, '尚未日常启用')
})
