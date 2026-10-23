const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const attrs = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
const saved = { applicationEntryMode: 'DRAFT', id: '901', instanceCode: 'FORM-901', status: 'DRAFT', bpmProcessInstanceId: null,
  controlledFileId: '20', dccProjectCodeId: '5', versionNo: 'A/1', attributeRound: 2,
  applicationContextKey: 'DCC_OBSOLETE:901:FORM-901', defaultSource: attrs, actualAttributes: attrs,
  attributesSubmitted: false, reason: '本次原因', idempotencyKey: 'actual-key', selectedSignoffDepartmentIds: ['51'],
  preparationReady: false, unavailableReason: 'DCC_OBSOLETE_EXECUTION_PREPARATION_REQUIRED',
  approvalMatrixChanged: false, approvalMatrixBefore: null, approvalMatrixAfter: null,
  approvalMatrixBaselineToken: null, approvalMatrixConfirmationToken: null,
  context: { objectId: '20', objectVersion: 'A/1', systemCode: 'DCC', objectType: 'CONTROLLED_FILE', actionCode: 'OBSOLETE' } }
function load(response, failure) {
  const calls = []
  const c = { exports: {}, require: name => {
    if (name === '@/config/axios') return { default: {
      get: async args => { calls.push(args); if (failure) throw failure; return response },
      post: async args => { calls.push(args); if (failure) throw failure; return response } } }
    if (name === 'axios') return { default: {}, AxiosError: class extends Error {} }
    if (name === 'element-plus') return { ElMessageBox: {} }
    if (name === '@/config/axios/config') return { config: {} }
    if (name === '@/utils/auth') return { getTenantId: () => '1', getVisitTenantId: () => undefined }
    if (name === '@/utils/filt') return {}
    if (name.endsWith('project-attributes/state')) return { validateAttributes: value => value }
    throw new Error('Unexpected module: ' + name)
  } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/api/dcc/controlledFile/workflow.ts', 'utf8').replace(/import\.meta\.env/g, '__viteEnv'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return { api: c.exports, calls }
}
const request = { phase: 'OPEN', reason: '本次原因', idempotencyKey: 'actual-key',
  projectAttributes: attrs, selectedSignoffDepartmentIds: ['51'] }
test('read-only draft lookup uses the existing DCC file scope and does not create or submit a generic form', async () => {
  const state = load(saved)
  const result = await state.api.getControlledFileObsoleteApplicationDraft('20')
  assert.equal(result.id, '901')
  assert.equal(state.calls[0].url, '/dcc/controlled-files/20/obsolete-application-draft')
  assert.equal(state.calls[0].data, undefined)
  assert.equal(await load(null).api.getControlledFileObsoleteApplicationDraft('20'), null)
})
test('OPEN preserves exact server draft facts and supplied explicit phase with local error handling', async () => {
  const state = load(saved)
  const result = await state.api.obsoleteControlledFile('20', request)
  assert.equal(result.attributeRound, 2)
  assert.equal(state.calls[0].data.phase, 'OPEN')
  assert.equal(state.calls[0].ignoreErrorMessage, true)
  assert.equal(state.calls[0].url, '/dcc/controlled-files/20/obsolete')
})
test('invalid or omitted phase and non-OPEN missing exact form scope reject before any write', async () => {
  for (const change of [{ phase: undefined }, { phase: 'LATEST' }, { phase: 'SUBMIT' },
    { phase: 'SAVE', formInstanceId: '901', formInstanceCode: 'FORM-901', attributeRound: 2,
      applicationContextKey: 'DCC_OBSOLETE:901:FORM-901' }]) {
    const state = load(saved)
    await assert.rejects(state.api.obsoleteControlledFile('20', { ...request, ...change }), /作废/)
    assert.equal(state.calls.length, 0)
  }
})
test('response mismatch, missing saved facts, unsafe identities or wrong submit status cannot report success', async () => {
  for (const change of [{ id: 9223372036854775000 }, { controlledFileId: '21' },
    { versionNo: '' }, { attributeRound: 0 }, { applicationContextKey: 'client-context' },
    { defaultSource: null }, { reason: undefined }, { selectedSignoffDepartmentIds: null },
    { preparationReady: true, unavailableReason: 'NOT_READY' }]) {
    await assert.rejects(load({ ...saved, ...change }).api.obsoleteControlledFile('20', request), /作废/)
  }
  const submit = { ...request, phase: 'SUBMIT', formInstanceId: '901', formInstanceCode: 'FORM-901',
    attributeRound: 2, applicationContextKey: 'DCC_OBSOLETE:901:FORM-901' }
  await assert.rejects(load(saved).api.obsoleteControlledFile('20', submit), /作废/)
})
test('SAVE false remains an explicit draft command and service failures keep their actual error', async () => {
  const data = { ...request, phase: 'SAVE', prepareExecution: false, formInstanceId: '901',
    formInstanceCode: 'FORM-901', attributeRound: 2, applicationContextKey: 'DCC_OBSOLETE:901:FORM-901' }
  const state = load(saved)
  await state.api.obsoleteControlledFile('20', data)
  assert.equal(state.calls[0].data.prepareExecution, false)
  const failure = new Error('actual preparation source not ready')
  await assert.rejects(load(saved, failure).api.obsoleteControlledFile('20', data), error => error === failure)
})
const sourceExpectation = { reworkSourceFormInstanceId: '801', reworkSourceFormInstanceCode: 'OLD-801',
  reworkSourceBpmProcessInstanceId: 'old-bpm', reworkSourceAttributeRound: 1 }
const sourceFields = { ...sourceExpectation, reworkSourceDigest: 'a'.repeat(64), reworkSourceStatus: 'REJECTED', reworkSourceVersionNo: 'A/1' }
const sourceMatrix = { formatVersion: 'DCC_APPROVAL_MATRIX_BASELINE_V2', tenantId: '1', categoryId: '2',
  fileTypeTaxonomyId: '6', routeId: '31', routeVersionNo: 1, matrixDigest: 'b'.repeat(64), nodes: [{ id: '701',
    stageType: 'REVIEW', subjectName: '原会签部门', marker: '●', approveMethod: 'ALL', approveRatio: 100, ruleRemark: '' }] }
const relations = { tenantId: '1', fileId: '20', masterId: '10', projectId: '5', applicationType: 'OBSOLETE', attributeRound: 1,
  bpmRound: 'old-bpm', recorded: false, unavailableReason: 'HISTORICAL_RELATION_NOT_RECORDED', relationDigest: 'c'.repeat(64),
  originalUploadSelections: [], completeSelections: [], stages: [] }
const reworkSource = { ...saved, ...sourceFields, applicationEntryMode: 'REWORK_SOURCE', id: '801', instanceCode: 'OLD-801',
  status: 'REJECTED', bpmProcessInstanceId: 'old-bpm', attributeRound: 1, applicationContextKey: 'DCC_OBSOLETE:801:OLD-801',
  attributesSubmitted: true, reworkSourceApprovalMatrix: sourceMatrix, reworkSourceRelations: relations, reworkSourceRelationDigest: relations.relationDigest }
test('actual rework source read sends exact selected old tuple and preserves frozen matrix and relations', async () => {
  const state = load(reworkSource)
  const result = await state.api.getControlledFileObsoleteApplicationDraft('20', sourceExpectation)
  assert.equal(result.applicationEntryMode, 'REWORK_SOURCE')
  assert.deepEqual(state.calls[0].params, sourceExpectation)
  assert.equal(result.reworkSourceApprovalMatrix.matrixDigest, sourceMatrix.matrixDigest)
  assert.equal(result.reworkSourceRelations.bpmRound, 'old-bpm')
})
test('source selection has no draft identity and never defaults to a candidate or permits it as POST result', async () => {
  const selection = { applicationEntryMode: 'SOURCE_SELECTION_REQUIRED', controlledFileId: '20', dccProjectCodeId: '5', versionNo: 'A/1',
    preparationReady: false, unavailableReason: 'SOURCE_SELECTION_REQUIRED', reworkSourceCandidates: [{ ...sourceFields, reason: '原原因', available: true, unavailableReason: null },
      { ...sourceFields, reworkSourceFormInstanceId: '802', reworkSourceFormInstanceCode: 'OLD-802', reworkSourceBpmProcessInstanceId: 'other-old-bpm', reworkSourceAttributeRound: 2, reason: '另一来源', available: true, unavailableReason: null }] }
  const result = await load(selection).api.getControlledFileObsoleteApplicationDraft('20')
  assert.equal(result.id, undefined);assert.equal(result.reworkSourceCandidates.length, 2)
  await assert.rejects(load(reworkSource).api.obsoleteControlledFile('20', request), /作废/)
})
test('partial selected source or borrowed relation identity cannot silently become a trusted read or write', async () => {
  const state = load(reworkSource)
  await assert.rejects(state.api.getControlledFileObsoleteApplicationDraft('20', { reworkSourceFormInstanceId: '801' }), /作废/)
  assert.equal(state.calls.length, 0)
  for (const change of [{ applicationEntryMode: undefined }, { reworkSourceFormInstanceId: '802' },
    { reworkSourceRelations: { ...relations, bpmRound: 'foreign' } }, { reworkSourceApprovalMatrix: null }])
    await assert.rejects(load({ ...reworkSource, ...change }).api.getControlledFileObsoleteApplicationDraft('20'), /作废/)
  const write = load(saved)
  await assert.rejects(write.api.obsoleteControlledFile('20', { ...request, reworkSourceFormInstanceId: '801' }), /作废/)
  assert.equal(write.calls.length, 0)
})
test('rework OPEN returns an independent new draft and the exact new request key rather than its old source key', async () => {
  const data = { ...request, ...sourceExpectation, reworkSourceDigest: sourceFields.reworkSourceDigest, idempotencyKey: 'new-rework-key' }
  const next = { ...saved, ...sourceFields, idempotencyKey: data.idempotencyKey, reworkSourceApprovalMatrix: sourceMatrix,
    reworkSourceRelations: relations, reworkSourceRelationDigest: relations.relationDigest }
  const state = load(next)
  const result = await state.api.obsoleteControlledFile('20', data)
  assert.equal(result.id, '901');assert.equal(result.attributeRound, 2)
  assert.equal(state.calls[0].data.reworkSourceFormInstanceId, '801')
  await assert.rejects(load({ ...next, idempotencyKey: 'old-key' }).api.obsoleteControlledFile('20', data), /作废/)
})
test('legitimate nullable raw matrix facts remain readable without replacing original rules', async () => {
  const nullable = { ...sourceMatrix, nodes: [{ ...sourceMatrix.nodes[0], subjectName: null, marker: null, approveMethod: null, ruleRemark: null }] }
  const result = await load({ ...reworkSource, reworkSourceApprovalMatrix: nullable }).api.getControlledFileObsoleteApplicationDraft('20')
  assert.equal(result.reworkSourceApprovalMatrix.nodes[0].ruleRemark, null)
})
const stage = { signatureEvidenceStatus: 'VALID', signatureIdentityMatches: true, snapshot: {
  header: { id: '711', tenantId: '1', sourceFileId: '20', sourceMasterId: '10', projectId: '5',
    processInstanceId: 'old-bpm', taskId: 'old-task', obligationId: 'old-duty', stageCode: 'MATRIX_REVIEW', nodeInstanceId: 'old-exec',
    departmentId: '51', actorId: '99', signatureId: '712', signedAt: '2026-10-12 12:00:00', commandKey: 'old-command',
    payloadHash: 'd'.repeat(64), expectedDigest: 'e'.repeat(64), completeDigest: 'f'.repeat(64), actionType: 'ASSIGN', reason: '原签名事实',
    applicationType: 'OBSOLETE', attributeRound: 1, formActionInstanceId: '801' },
  members: [{ tenantId: '1', snapshotId: '711', relatedMasterId: '13', selectedControlledFileId: '23', projectId: '5',
    fileNumberSnapshot: 'RELATED-SOP', fileNameSnapshot: '关联正文.pdf', versionNoSnapshot: 'A/1', origin: 'SIGNED', supplemental: true,
    assigneeUserId: null, dueAt: null }] } }
test('signed relation headers and members must belong to the exact old source and cannot bypass source scope', async () => {
  const response = changed => ({ ...reworkSource, reworkSourceRelations: { ...relations, recorded: true, unavailableReason: null, stages: [changed] } })
  assert.equal((await load(response(stage)).api.getControlledFileObsoleteApplicationDraft('20')).reworkSourceRelations.stages.length, 1)
  for (const header of [{ sourceFileId: '21' }, { sourceMasterId: '11' }, { projectId: '6' }, { processInstanceId: 'foreign-bpm' },
    { formActionInstanceId: '802' }, { attributeRound: 2 }])
    await assert.rejects(load(response({ ...stage, snapshot: { ...stage.snapshot, header: { ...stage.snapshot.header, ...header } } })).api.getControlledFileObsoleteApplicationDraft('20'), /作废/)
  await assert.rejects(load(response({ ...stage, signatureIdentityMatches: false })).api.getControlledFileObsoleteApplicationDraft('20'), /作废/)
  await assert.rejects(load(response({ ...stage, snapshot: { ...stage.snapshot, members: [{ ...stage.snapshot.members[0], snapshotId: '713' }] } })).api.getControlledFileObsoleteApplicationDraft('20'), /作废/)
})
test('SUBMIT accepts the exact EFFECTIVE receipt and never treats rejected or abandoned rounds as a successful submission', async () => {
  const data = { ...request, phase: 'SUBMIT', formInstanceId: '901', formInstanceCode: 'FORM-901', attributeRound: 2,
    applicationContextKey: 'DCC_OBSOLETE:901:FORM-901' }
  const submitted = { ...saved, status: 'EFFECTIVE', bpmProcessInstanceId: 'real-next-bpm', attributesSubmitted: true }
  assert.equal((await load(submitted).api.obsoleteControlledFile('20', data)).status, 'EFFECTIVE')
  for (const status of ['REJECTED', 'ABANDONED', 'EFFECT_FAILED', 'MYSTERY'])
    await assert.rejects(load({ ...submitted, status }).api.obsoleteControlledFile('20', data), /作废/)
})
test('unavailable historical candidate may have genuinely missing reason without blocking the other complete source', async () => {
  const selection = { applicationEntryMode: 'SOURCE_SELECTION_REQUIRED', controlledFileId: '20', dccProjectCodeId: '5', versionNo: 'A/1',
    preparationReady: false, unavailableReason: 'SOURCE_SELECTION_REQUIRED', reworkSourceCandidates: [{ ...sourceFields, reason: '原原因', available: true, unavailableReason: null },
      { ...sourceFields, reworkSourceFormInstanceId: '802', reworkSourceFormInstanceCode: 'OLD-802', reworkSourceBpmProcessInstanceId: 'other-old-bpm',
        reworkSourceAttributeRound: 2, reworkSourceDigest: null, reason: null, available: false, unavailableReason: 'DCC_OBSOLETE_REWORK_RECORDED_PREPARATION_REQUIRED' }] }
  const result = await load(selection).api.getControlledFileObsoleteApplicationDraft('20')
  assert.equal(result.reworkSourceCandidates[1].reason, null);assert.equal(result.reworkSourceCandidates[0].available, true)
  await assert.rejects(load({ ...selection, reworkSourceCandidates: [{ ...selection.reworkSourceCandidates[0], reason: null }, selection.reworkSourceCandidates[1]] }).api.getControlledFileObsoleteApplicationDraft('20'), /作废/)
})
