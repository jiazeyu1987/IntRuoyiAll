const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
function helper() {
  const context = { exports: {} }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/detail/native-approval-progress.ts', 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
const flow = (training = true, rule = 'PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL', type = 'UPLOAD') => ({
  tenantId: '1', controlledFileId: '10', applicationType: type, attributeRound: 1, bpmRound: 'A',
  status: 'READY', unavailableReason: null, processDefinitionKey: 'dcc-controlled-file-' + type.toLowerCase(),
  processDefinitionId: 'exact-definition', deploymentId: 'deployment', definitionDigest: 'a'.repeat(64),
  executionContract: rule.startsWith('LEGACY') ? 'LEGACY_V4_ACTION' : 'PUBLIC_MATRIX_NATIVE_V1',
  flowRule: rule, needTraining: type === 'OBSOLETE' ? null : training,
  formInstanceId: type === 'OBSOLETE' ? '50' : null, formInstanceCode: type === 'OBSOLETE' ? 'FORM-50' : null
})
const scope = (contract = flow()) => ({ fileId: '10', bpmRound: 'A', applicationType: contract.applicationType, flowContract: contract })
const facts = { fileId: '10', fileBpmRound: 'A', needTraining: false, status: 'PENDING_SUPERVISOR_REVIEW' }
test('the exact per-round contract selects v5 document-before-training and preserves the proven old sequence', () => {
  const h = helper()
  const keys = value => Array.from(h.buildNativeApprovalProgress(scope(value), facts, []), row => row.stageCode)
  assert.deepEqual(keys(flow()), ['SUPERVISOR_REVIEW', 'MATRIX_REVIEW', 'MATRIX_APPROVAL', 'DOC_CONTROL_REVIEW',
    'APPLICANT_TRAINING_RECORD', 'CONTROLLED', 'DISTRIBUTED'])
  assert.deepEqual(keys(flow(true, 'LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL')),
    ['MATRIX_REVIEW', 'MATRIX_APPROVAL', 'APPLICANT_TRAINING_RECORD', 'DOC_CONTROL_REVIEW', 'CONTROLLED', 'DISTRIBUTED'])
  assert.equal(keys(flow(false)).includes('APPLICANT_TRAINING_RECORD'), false)
})
test('obsolete uses the exact form rule and ends at approval without document, training or dispatch stages', () => {
  const h = helper()
  const obsolete = scope(flow(null, 'PUBLIC_V5_OBSOLETE_APPROVAL_END', 'OBSOLETE'))
  assert.deepEqual(Array.from(h.buildNativeApprovalProgress(obsolete, facts, []), row => row.stageCode),
    ['SUPERVISOR_REVIEW', 'MATRIX_REVIEW', 'MATRIX_APPROVAL'])
})
test('unavailable or malformed flow metadata cannot turn into a guessed old native diagram', () => {
  const h = helper()
  for (const value of [undefined, { ...flow(), status: 'NOT_RECORDED', executionContract: null, flowRule: null },
    { ...flow(), bpmRound: 'B' }, { ...flow(), applicationType: 'REVISION' }, { ...flow(), needTraining: null },
    { ...flow(), flowRule: 'UNKNOWN' }]) {
    assert.throws(() => h.buildNativeApprovalProgress({ ...scope(), flowContract: value }, facts, []), /流程合同|培训/)
  }
})
test('historical A completion and training choice never borrow current B status, records or timestamps', () => {
  const h = helper()
  const stages = h.buildNativeApprovalProgress(scope(flow(true)), {
    ...facts, fileBpmRound: 'B', needTraining: false, status: 'ACTIVE', trainingRecordAvailable: true,
    controlledTime: 'B-control', activatedTime: 'B-activate', distributedTime: 'B-dispatch'
  }, [{ id: 'actual-A-review', processInstanceId: 'A', taskDefinitionKey: 'MATRIX_REVIEW', status: 2 }])
  assert.equal(stages.find(row => row.stageCode === 'MATRIX_REVIEW').isCompleted, true)
  for (const key of ['APPLICANT_TRAINING_RECORD', 'CONTROLLED', 'DISTRIBUTED']) {
    const stage = stages.find(row => row.stageCode === key)
    assert.equal(stage.isCompleted, false)
    assert.equal(stage.isCurrent, false)
    assert.match(stage.sameLayerHint, /未记录/)
  }
})
test('controlled pending-effective does not advance distribution until actual activation', () => {
  const h = helper(), s = scope(flow(false))
  const waiting = h.buildNativeApprovalProgress(s, { ...facts, status: 'CONTROLLED_PENDING_EFFECTIVE', controlledTime: 'controlled' }, [])
  assert.equal(waiting.find(row => row.stageCode === 'DISTRIBUTED').isCurrent, false)
  const active = h.buildNativeApprovalProgress(s, { ...facts, status: 'ACTIVE', controlledTime: 'controlled', activatedTime: 'activated' }, [])
  assert.equal(active.find(row => row.stageCode === 'DISTRIBUTED').isCurrent, true)
  const sent = h.buildNativeApprovalProgress(s, { ...facts, status: 'ACTIVE', controlledTime: 'controlled',
    activatedTime: 'activated', distributedTime: 'dispatched' }, [])
  assert.equal(sent.find(row => row.stageCode === 'DISTRIBUTED').isCompleted, true)
})
test('selection retains unavailable exact-round metadata for evidence while progress refuses an invented sequence', () => {
  const h = helper(), unavailable = { ...flow(), status: 'NOT_RECORDED', unavailableReason: 'HISTORICAL_CONTRACT_NOT_RECORDED',
    executionContract: null, flowRule: null }
  const resolved = h.resolveApprovalProgressScope({ fileId: '10', bpmRound: 'A', nativeBpmRound: 'B',
    rounds: [{ controlledFileId: '10', applicationType: 'UPLOAD', bpmRound: 'A', attributeRound: 1, flowContract: unavailable }] })
  assert.equal(resolved.bpmRound, 'A')
  assert.equal(resolved.flowContract.status, 'NOT_RECORDED')
})
test('handling and signature dialog flow text consume the same exact selected contract', () => {
  const h = helper()
  assert.equal(typeof h.getNativeApprovalProcessText, 'function')
  const text = h.getNativeApprovalProcessText(scope())
  assert.match(text, /主管.*会签.*批准.*文控.*培训.*受控.*生效.*下发/)
  assert.match(h.getNativeApprovalProcessText(scope(flow(true, 'LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL'))), /培训.*文控/)
  assert.match(h.getNativeApprovalProcessText({ ...scope(), flowContract: undefined }), /未记录/)
})
test('actual parent keeps unavailable-flow warning separate from valid round evidence and historical route snapshots', () => {
  const h = helper()
  const ast = ts.createSourceFile('detail.ts', parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8'))
    .descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declarations = names => names.map(name => ast.statements.find(node => ts.isVariableStatement(node)
    && node.declarationList.declarations.some(d => d.name.text === name)).getText(ast)).join('\n')
  const unavailable = { ...scope(), flowContract: { ...flow(), status: 'NOT_RECORDED', executionContract: null, flowRule: null } }
  const context = { exports: {}, computed: vue.computed, approvalProgressScope: vue.ref(unavailable),
    fileDetail: vue.ref({ id: '10', processInstanceId: 'B', needTraining: false, routeSnapshots: [{ stageCode: 'MATRIX_REVIEW', resolvedUserIds: ['B-actor'] }] }),
    stageProgressList: vue.ref([{ stageCode: 'STALE' }]), approvalProgressError: vue.ref(''), approvalTaskList: vue.ref([]),
    applicationApprovalRead: vue.ref({ processInstanceId: 'A', taskId: '' }), fileStatus: vue.ref('ACTIVE'),
    buildNativeApprovalProgress: h.buildNativeApprovalProgress, getNativeApprovalFlowUnavailableReason: h.getNativeApprovalFlowUnavailableReason,
    buildDccTaskStageProgress: () => { throw new Error('must not guess legacy') } }
  vm.runInNewContext(ts.transpileModule(declarations(['syncStageProgress', 'getStageRouteSnapshot'])
    + '\nexports.sync=syncStageProgress;exports.snapshot=getStageRouteSnapshot',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  context.exports.sync()
  assert.equal(context.stageProgressList.value.length, 0)
  assert.match(context.approvalProgressError.value, /未记录/)
  assert.equal(context.applicationApprovalRead.value.processInstanceId, 'A')
  assert.equal(context.exports.snapshot({ stageCode: 'MATRIX_REVIEW' }), undefined)
})
test('actual record formatter uses this-round training selection and describes automatic dispatch after effectiveness', () => {
  const ast = ts.createSourceFile('detail.ts', parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8'))
    .descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(node => ts.isVariableStatement(node)
    && node.declarationList.declarations.some(d => d.name.text === 'formatStageSignatureStatus'))
  const context = { exports: {}, String, approvalProgressScope: vue.ref(scope()),
    approvalProcessInstanceId: vue.ref('A'), isWorkingBrowserDetailCurrent: vue.ref(true),
    fileDetail: vue.ref({ id: '10', processInstanceId: 'A', needTraining: false, trainingRecordAvailable: true,
      controlledTime: 'controlled', activatedTime: null }), getStageSignatures: () => [] }
  vm.runInNewContext(ts.transpileModule(declaration.getText(ast) + '\nexports.format=formatStageSignatureStatus',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  assert.equal(context.exports.format({ stageCode: 'APPLICANT_TRAINING_RECORD' }), '线下培训记录已上传')
  assert.match(context.exports.format({ stageCode: 'DISTRIBUTED' }), /生效.*自动下发/)
  context.fileDetail.value.activatedTime = 'activated'
  assert.match(context.exports.format({ stageCode: 'DISTRIBUTED' }), /系统自动下发/)
})
test('actual handling flow text cannot display an old selected scope before current file and round are verified', () => {
  const h = helper()
  const ast = ts.createSourceFile('detail.ts', parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8'))
    .descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(node => ts.isVariableStatement(node)
    && node.declarationList.declarations.some(d => d.name.text === 'approvalHandlingProcessText'))
  const context = { exports: {}, computed: vue.computed, approvalProgressScope: vue.ref(scope()),
    currentApprovalApplicationType: vue.ref(undefined), getNativeApprovalProcessText: h.getNativeApprovalProcessText }
  vm.runInNewContext(ts.transpileModule(declaration.getText(ast) + '\nexports.text=approvalHandlingProcessText',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  assert.equal(context.exports.text.value, '本次申请身份尚未核验')
  context.currentApprovalApplicationType.value = 'UPLOAD'
  assert.match(context.exports.text.value, /主管.*文控.*培训/)
})
