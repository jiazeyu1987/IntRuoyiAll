const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const helpers = require('./dcc-upload-test-loader.cjs'), moduleUnderTest = helpers.loadUploadMatrix()
const scope = { tenantId: '1', projectId: '5', categoryId: '2', taxonomyId: '4', actionType: 'REVISION', controlledFileId: '11', applicationContextKey: 'same-scope' }
const eligibility = { controlledFileId: '11', projectId: '5', applicationType: 'REVISION', draftApprovalMatrix: null, canSubmit: false, newActionBaselineRequired: true, newActionBaselinePreparationAllowed: true,
  executionContract: 'PUBLIC_MATRIX_NATIVE_V1', preparedProcessDefinitionId: 'dcc-controlled-file-revision:5:fixture-exact', preparedDefinitionDigest: 'a'.repeat(64) }
const response = () => ({ ...helpers.matrixReadiness({ ready: true, nodes: [], blockers: [] }, { actionType: 'REVISION' }), newActionBaselineRequired: true, newActionBaselinePreparationAllowed: true })

test('saved body pins its exact execution identity in the very first matrix read', () => {
  const state = new moduleUnderTest.UploadApprovalMatrixState(scope, helpers.matrixFixture(), eligibility)
  const request = state.begin(['7']).request
  assert.equal(request.expectedProcessDefinitionId, eligibility.preparedProcessDefinitionId)
  assert.equal(request.expectedDefinitionDigest, eligibility.preparedDefinitionDigest)
  assert.equal(request.prepareNewActionBaseline, undefined)
  for(const patch of [{executionContract:null},{executionContract:'LEGACY_V4_ACTION'},{preparedProcessDefinitionId:null},{preparedDefinitionDigest:'bad'}])
    assert.throws(()=>new moduleUnderTest.UploadApprovalMatrixState(scope,helpers.matrixFixture(),{...eligibility,...patch}))
})
test('only exact registered cross-action server flags on missing raw REVISION enable explicit preparation', () => {
  assert.equal(typeof moduleUnderTest.isNewActionBaselinePreparationAllowed, 'function')
  assert.equal(moduleUnderTest.isNewActionBaselinePreparationAllowed(eligibility, scope), true)
  for (const patch of [{ newActionBaselineRequired: undefined }, { newActionBaselinePreparationAllowed: false }, { newActionBaselinePreparationAllowed: 'true' },
    { applicationType: 'UPLOAD' }, { controlledFileId: '12' }, { projectId: '8' }, { canSubmit: true }, { canSubmit: undefined },
    { executionContract: null }, { executionContract: 'LEGACY_V4_ACTION' }, { preparedProcessDefinitionId: undefined },
    { preparedDefinitionDigest: 'bad' }, { draftApprovalMatrix: helpers.matrixFixture() }])
    assert.equal(moduleUnderTest.isNewActionBaselinePreparationAllowed({ ...eligibility, ...patch }, scope), false)
})
test('explicit request and pending-save selection retain actual target and returned token while ordinary flows never set the flag', () => {
  const state = new moduleUnderTest.UploadApprovalMatrixState(scope)
  assert.equal(typeof state.prepareNewActionBaseline, 'function')
  const request = state.prepareNewActionBaseline(eligibility, ['7'])
  assert.equal(request.request.prepareNewActionBaseline, true)
  assert.equal(request.request.controlledFileId, '11'); assert.equal(request.request.executionContract, 'PUBLIC_MATRIX_NATIVE_V1')
  assert.equal(request.request.applicationContextKey, 'same-scope')
  assert.equal(request.request.expectedProcessDefinitionId, eligibility.preparedProcessDefinitionId)
  assert.equal(request.request.expectedDefinitionDigest, eligibility.preparedDefinitionDigest)
  state.accept(request, response())
  const selection = state.newActionSaveSelection()
  assert.equal(selection.prepareNewActionBaseline, true)
  assert.equal(selection.approvalMatrixBaselineToken, 'fixture-server-baseline')
  assert.equal(selection.expectedProcessDefinitionId, 'dcc-controlled-file-revision:5:fixture-exact')
  assert.equal(state.begin(['7']).request.prepareNewActionBaseline, true)
  const saved = new moduleUnderTest.UploadApprovalMatrixState(scope, helpers.matrixFixture())
  assert.throws(() => saved.prepareNewActionBaseline(eligibility), /已保存|覆盖|资格/)
})
test('wrong or missing server preparation flags and a stale target response cannot create a new-action baseline', () => {
  for (const patch of [{ newActionBaselineRequired: false }, { newActionBaselinePreparationAllowed: undefined }]) {
    const state = new moduleUnderTest.UploadApprovalMatrixState(scope)
    const request = state.prepareNewActionBaseline(eligibility)
    assert.throws(() => state.accept(request, { ...response(), ...patch }), /资格|准备/)
  }
  const state = new moduleUnderTest.UploadApprovalMatrixState(scope), request = state.prepareNewActionBaseline(eligibility)
  state.reset({ ...scope, controlledFileId: '12' })
  assert.equal(state.accept(request, response()), false)
  const other = new moduleUnderTest.UploadApprovalMatrixState(scope)
  const seeded = other.prepareNewActionBaseline(eligibility)
  assert.throws(() => other.accept(seeded, { ...response(), preparedProcessDefinitionId: 'dcc-controlled-file-revision:6:wrong-latest', preparedDefinitionDigest: 'b'.repeat(64) }), /已保存|定义.*不一致|准备.*变化/)
})
test('a successfully persisted new baseline clears preparation intent without replacing its original bytes', () => {
  const state = new moduleUnderTest.UploadApprovalMatrixState(scope)
  state.accept(state.prepareNewActionBaseline(eligibility), response())
  state.newActionBaselineSaved(helpers.matrixFixture())
  assert.equal(state.begin().request.prepareNewActionBaseline, undefined)
  assert.throws(() => state.prepareNewActionBaseline(eligibility), /已保存|覆盖|资格/)
})

test('preview wrapper remains the existing route-preview resource and preparation click is never an eager save API', () => {
  const file = path.resolve(__dirname, '../../src/api/dcc/controlledFile/workflow.ts')
  const source = ts.createSourceFile(file, fs.readFileSync(file, 'utf8'), ts.ScriptTarget.Latest, true)
  const declaration = source.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.text === 'checkControlledFileRouteReadiness'))
  assert.match(declaration.getText(source), /\/dcc\/controlled-files\/route-preview/)
  assert.doesNotMatch(declaration.getText(source), /route-readiness/)
})
