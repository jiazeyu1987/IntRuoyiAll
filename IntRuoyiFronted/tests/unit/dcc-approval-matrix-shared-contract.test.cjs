const { test } = require('node:test'), assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript')
const root = path.resolve(__dirname, '../..'), read = file => fs.readFileSync(path.join(root, file), 'utf8')
const raw = JSON.parse(read('tests/fixtures/immutable-public-matrix-v2.fixture.json'))
const attrs = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
const draft = matrix => ({ controlledFileId: '91', projectId: '5', applicationType: 'UPLOAD', defaultSource: attrs, actual: attrs,
  canSubmit: false, unavailableReason: 'NOT_SUBMITTED', effectiveMode: null, effectiveDate: null, needTraining: false,
  selectedSignoffDepartmentIds: null, selectedDistributionDepartmentIds: null, changeDescription: null, draftApprovalMatrix: matrix })
function load(response) {
  const calls = [], context = { exports: {}, Error, Date, Set, BigInt, require: name => {
    if (name === '@/config/axios') return { default: { get: async input => { calls.push(input); return response }, post: async input => { calls.push(input); return true } } }
    if (name.includes('project-attributes/state')) return { validateAttributes: value => value }
    throw Error(name)
  } }
  vm.runInNewContext(ts.transpileModule(read('src/api/dcc/controlledFile/applicationRead.ts'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return { api: context.exports, calls }
}
const matrix = () => ({ ...JSON.parse(JSON.stringify(raw)), matrixDigest: 'a'.repeat(64) })
test('working response preserves complete raw immutable baseline and genuine absent history', async () => {
  const exact = matrix(), state = load(draft(exact))
  const value = await state.api.getWorkingApplicationAttributes('91')
  assert.equal(value.draftApprovalMatrix.routeId, exact.routeId)
  assert.equal(value.draftApprovalMatrix.nodes[0].candidateSourceIds, exact.nodes[0].candidateSourceIds)
  assert.equal(value.draftApprovalMatrix.nodes[0].marker, exact.nodes[0].marker)
  assert.equal((await load(draft(null)).api.getWorkingApplicationAttributes('91')).draftApprovalMatrix, null)
})
test('missing digest, lossy identity or absent raw rules cannot become a trusted draft baseline', async () => {
  for (const invalid of [{ ...matrix(), matrixDigest: 'bad' }, { ...matrix(), routeId: Number(raw.routeId) },
    { ...matrix(), nodes: undefined }, { ...matrix(), nodes: [] }]) {
    await assert.rejects(load(draft(invalid)).api.getWorkingApplicationAttributes('91'))
  }
})
test('working save transmits explicit matrix expectation and never manufactures a confirmation', async () => {
  const state = load(null), selection = { effectiveMode: null, effectiveDate: null, selectedSignoffDepartmentIds: null,
    selectedDistributionDepartmentIds: null, needTraining: false, changeDescription: null,
    expectedApprovalRouteId: raw.routeId, expectedApprovalRouteVersionNo: raw.routeVersionNo,
    expectedApprovalMatrixDigest: 'a'.repeat(64), approvalMatrixBaselineToken: 'actual-server-token',
    approvalMatrixConfirmationToken: null, matrixChangeConfirmed: false, applicationContextKey: 'actual-upload-scope' }
  await state.api.saveWorkingApplicationAttributes('91', attrs, selection)
  assert.equal(state.calls[0].data.expectedApprovalRouteId, raw.routeId)
  assert.equal(state.calls[0].data.matrixChangeConfirmed, false)
  assert.equal(state.calls[0].data.approvalMatrixConfirmationToken, null)
})
test('shared offline schema stores nullable baseline and source identity without historical inference', () => {
  const schema = read('../IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql')
  for (const field of ['draft_approval_matrix_json', 'source_approval_matrix_id', 'source_approval_matrix_version', 'source_approval_matrix_digest', 'derivation_version'])
    assert.match(schema, new RegExp('`' + field + '` [^\\n]*NULL'))
})
test('IC21 discovery keeps real new-action eligibility and rejects partial or contradictory facts', async () => {
  const prepared = { ...draft(null), applicationType: 'REVISION', newActionBaselineRequired: true,
    newActionBaselinePreparationAllowed: true, executionContract: 'PUBLIC_MATRIX_NATIVE_V1',
    preparedProcessDefinitionId: 'actual-revision-definition', preparedDefinitionDigest: 'b'.repeat(64) }
  const read = await load(prepared).api.getWorkingApplicationAttributes('91')
  assert.equal(read.newActionBaselineRequired, true)
  assert.equal(read.newActionBaselinePreparationAllowed, true)
  for (const bad of [
    { ...prepared, newActionBaselineRequired: undefined },
    { ...prepared, newActionBaselinePreparationAllowed: 'true' },
    { ...prepared, newActionBaselineRequired: false },
    { ...prepared, applicationType: 'UPLOAD' },
    { ...prepared, draftApprovalMatrix: matrix() },
    { ...prepared, canSubmit: true },
    { ...prepared, executionContract: undefined },
    { ...prepared, executionContract: 'LEGACY_V4_ACTION' },
    { ...prepared, preparedProcessDefinitionId: null },
    { ...prepared, preparedDefinitionDigest: 'bad' }
  ]) await assert.rejects(load(bad).api.getWorkingApplicationAttributes('91'))
  const disabled = await load({ ...prepared, newActionBaselinePreparationAllowed: false }).api.getWorkingApplicationAttributes('91')
  assert.equal(disabled.newActionBaselinePreparationAllowed, false)
  assert.equal((await load(draft(null)).api.getWorkingApplicationAttributes('91')).newActionBaselinePreparationAllowed, undefined)
})
test('IC21 saves only an explicit boolean preparation request through the existing resource', async () => {
  const selection = { effectiveMode: null, effectiveDate: null, selectedSignoffDepartmentIds: null,
    selectedDistributionDepartmentIds: null, needTraining: null, changeDescription: '本次输入',
    prepareNewActionBaseline: true, approvalMatrixBaselineToken: 'actual-target-scoped-token',
    expectedApprovalRouteId: raw.routeId, expectedApprovalRouteVersionNo: raw.routeVersionNo,
    expectedApprovalMatrixDigest: 'a'.repeat(64), executionContract: 'PUBLIC_MATRIX_NATIVE_V1',
    expectedProcessDefinitionId: 'actual-revision-definition', expectedDefinitionDigest: 'b'.repeat(64),
    applicationContextKey: 'actual-revision-context' }
  const state = load(null)
  await state.api.saveWorkingApplicationAttributes('91', attrs, selection)
  assert.equal(state.calls.length, 1)
  assert.equal(state.calls[0].url, '/dcc/controlled-files/91/working-attributes')
  assert.equal(state.calls[0].data.prepareNewActionBaseline, true)
  assert.equal(state.calls[0].data.approvalMatrixBaselineToken, selection.approvalMatrixBaselineToken)
  for (const flag of [false, null, undefined]) {
    const ordinary = load(null)
    await ordinary.api.saveWorkingApplicationAttributes('91', attrs, { ...selection, prepareNewActionBaseline: flag })
    assert.equal(ordinary.calls[0].data.prepareNewActionBaseline, flag)
  }
  for (const flag of ['true', 1, {}]) {
    const invalid = load(null)
    await assert.rejects(invalid.api.saveWorkingApplicationAttributes('91', attrs, { ...selection, prepareNewActionBaseline: flag }))
    assert.equal(invalid.calls.length, 0)
  }
  for (const field of ['approvalMatrixBaselineToken', 'expectedProcessDefinitionId', 'expectedDefinitionDigest',
    'expectedApprovalRouteId', 'expectedApprovalMatrixDigest', 'expectedApprovalRouteVersionNo', 'applicationContextKey']) {
    const invalid = load(null)
    await assert.rejects(invalid.api.saveWorkingApplicationAttributes('91', attrs, { ...selection, [field]: undefined }))
    assert.equal(invalid.calls.length, 0)
  }
})
