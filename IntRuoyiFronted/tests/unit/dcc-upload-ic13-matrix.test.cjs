const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript')
const sourceFile = path.resolve(__dirname, '../../src/views/dcc/controlled-file/upload/approval-matrix.ts')
const node = { id: '40', stageNo: 1, stageCode: 'MATRIX_REVIEW', stageName: '会签', stageOrder: 1, sort: 1, candidateSourceType: 'DEPT', candidateSourceId: '7', candidateSourceIds: '7, 8', approveMethod: 'ALL', approveRatio: 100, requireAllApprovals: true, required: true, stageType: 'SIGNOFF', subjectLabel: '质量', marker: '●', subjectType: 'DEPT', subjectId: '7', subjectName: '质量部', subjectDepartmentPath: '公司/质量部', ruleRemark: '原备注\r\n不改' }
const baseline = version => ({ formatVersion: 'DCC_APPROVAL_MATRIX_BASELINE_V2', tenantId: '1', categoryId: '2', fileTypeTaxonomyId: '4', routeId: '9007199254740993', routeVersionNo: version, actionType: 'LEGACY', effectiveTime: '2026-10-11 00:00:00.000000000', routeRemark: null, nodes: [{ ...node, marker: version === 1 ? '●' : '◎' }], matrixDigest: String(version).repeat(64) })
const scope = { tenantId: '1', projectId: '3', categoryId: '2', taxonomyId: '4', actionType: 'NEW', applicationContextKey: 'scope-1' }
const response = (before = baseline(1), after = before) => ({ ready: true, nodes: [], blockers: [], routeId: after.routeId, routeVersionNo: after.routeVersionNo, matrixDigest: after.matrixDigest, derivationVersion: 'DCC_PUBLIC_MATRIX_NATIVE_V1', applicationActionType: 'NEW', approvalMatrix: after, matrixBefore: before, matrixAfter: after, matrixChangeRequired: before.matrixDigest !== after.matrixDigest, approvalMatrixBaselineToken: 'server-baseline', approvalMatrixConfirmationToken: before.matrixDigest !== after.matrixDigest ? 'server-confirm-' + after.routeVersionNo : null,
  executionContract: 'PUBLIC_MATRIX_NATIVE_V1', preparedProcessDefinitionId: 'dcc-controlled-file-upload:5:exact-def', preparedDefinitionDigest: 'a'.repeat(64), supervisorSourceDigest:'c'.repeat(64), supervisorSourceToken:'official-source-token-A' })
function load() {
  assert.ok(fs.existsSync(sourceFile), 'production matrix state must exist')
  const c = { exports: {}, Error, JSON, Object, Array, Number, String, BigInt, Set }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(sourceFile, 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, c)
  return c.exports
}
test('first server baseline sends exact identities and never defaults explicit confirmation true', () => {
  const m = load(), state = new m.UploadApprovalMatrixState(scope)
  const req = state.begin(); assert.equal(req.request.applicationContextKey, 'scope-1'); assert.equal(req.request.dccProjectCodeId, '3')
  state.accept(req, response()); const selection = state.selection()
  assert.equal(selection.expectedApprovalRouteId, '9007199254740993'); assert.equal(selection.expectedApprovalRouteVersionNo, 1)
  assert.equal(selection.matrixChangeConfirmed, false); assert.equal(selection.approvalMatrixConfirmationToken, null)
  assert.equal(selection.approvalMatrixBaselineToken, 'server-baseline')
})
test('before/after show exact raw rules and cancelled review cannot provide a submission selection', async () => {
  const m = load(), state = new m.UploadApprovalMatrixState(scope); state.accept(state.begin(), response())
  state.accept(state.begin(), response(baseline(1), baseline(2)))
  const shown = state.summary().map(row => row.label + ':' + row.value).join('\n')
  for (const value of ['确认前', '当前', '●', '◎', '7, 8', '原备注', '质量部']) assert.ok(shown.includes(value), value)
  assert.throws(() => state.selection(), /确认/)
  const calls = []
  assert.equal(await state.confirmChange(async () => { calls.push('confirm'); return false }), false)
  assert.deepEqual(calls, ['confirm']); assert.throws(() => state.selection(), /确认/)
  assert.equal(await state.confirmChange(async () => true), true)
  const selected = state.selection(); assert.equal(selected.matrixChangeConfirmed, true); assert.equal(selected.expectedApprovalRouteVersionNo, 2); assert.equal(selected.approvalMatrixConfirmationToken, 'server-confirm-2')
})

test('raw semantic differences remain visible even when before and after have the same subject name', () => {
  const m = load(), state = new m.UploadApprovalMatrixState(scope)
  const after = baseline(2); after.nodes[0].subjectType = 'USER'; after.nodes[0].candidateSourceType = 'USER'
  after.nodes[0].required = false; after.nodes[0].requireAllApprovals = false; after.nodes[0].stageType = 'APPROVAL'; after.nodes[0].sort = 9
  state.accept(state.begin(), response(baseline(1), after))
  const shown = state.summary().map(row => row.label + ':' + row.value).join('\n')
  for (const value of ['主体类型 部门', '主体类型 账号', '候选来源 部门', '候选来源 账号', '必须参与 是', '必须参与 否', '全部批准 是', '全部批准 否', '节点排序 9', '主体ID 7']) assert.ok(shown.includes(value), value)
})
test('a v2 decision never authorizes v3 or a late previous context response', async () => {
  const m = load(), state = new m.UploadApprovalMatrixState(scope); state.accept(state.begin(), response(baseline(1), baseline(2)))
  let release; const pending = state.confirmChange(() => new Promise(resolve => release = resolve))
  state.accept(state.begin(), response(baseline(1), baseline(3))); release(true)
  assert.equal(await pending, false); assert.throws(() => state.selection(), /确认/)
  const stale = state.begin(); state.reset({ ...scope, applicationContextKey: 'other', projectId: '9' })
  assert.equal(state.accept(stale, response()), false); assert.throws(() => state.selection(), /读取|配置/)
})

test('a new unresolved preview invalidates both the old dialog decision and a submission selection', async () => {
  const m = load(), state = new m.UploadApprovalMatrixState(scope)
  state.accept(state.begin(), response(baseline(1), baseline(2)))
  let release
  const pending = state.confirmChange(() => new Promise(resolve => { release = resolve }))
  const newRead = state.begin()
  release(true)
  assert.equal(await pending, false)
  assert.throws(() => state.selection(), /读取|校验/)
  state.accept(newRead, response(baseline(1), baseline(2)))
  assert.throws(() => state.selection(), /确认/)
})
test('saved raw baseline remains the exact before and rejects current-as-old substitution', () => {
  const m = load(), saved = baseline(1), state = new m.UploadApprovalMatrixState({ ...scope, controlledFileId: '11' }, saved)
  const req = state.begin(); assert.equal(req.request.controlledFileId, '11')
  state.accept(req, response(saved, baseline(3)))
  assert.equal(state.before.routeVersionNo, 1)
  assert.throws(() => state.accept(state.begin(), response(baseline(3))), /原|基线/)
})
test('missing baseline/tokens, raw fields, lossy Longs and false metadata are real errors', () => {
  const m = load()
  for (const patch of [{ approvalMatrixBaselineToken: null }, { routeId: '8' }, { derivationVersion: null }, { applicationActionType: 'REVISION' }, { matrixChangeRequired: null }, { matrixAfter: null }]) {
    const state = new m.UploadApprovalMatrixState(scope); assert.throws(() => state.accept(state.begin(), { ...response(), ...patch }))
  }
  for (const bad of [{ ...baseline(1), routeId: 9007199254740992 }, { ...baseline(1), matrixDigest: 'bad' }, { ...baseline(1), nodes: [{ ...node, candidateSourceIds: undefined }] }]) assert.throws(() => new m.UploadApprovalMatrixState(scope, bad))
  const state = new m.UploadApprovalMatrixState(scope); assert.throws(() => state.accept(state.begin(), { ...response(baseline(1), baseline(2)), approvalMatrixConfirmationToken: null }), /票据|配置/)
})

test('production request requires a genuine selected matrix while old external review remains outside IC13', () => {
  const m = load()
  const valid = { executionContract: 'PUBLIC_MATRIX_NATIVE_V1', expectedProcessDefinitionId: 'dcc-controlled-file-upload:5:exact-def', expectedDefinitionDigest: 'a'.repeat(64), expectedApprovalRouteId: '9007199254740993', expectedApprovalRouteVersionNo: 2,
    expectedApprovalMatrixDigest: '2'.repeat(64), approvalMatrixBaselineToken: 'server-baseline',
    approvalMatrixConfirmationToken: 'server-confirm-2', matrixChangeConfirmed: true, applicationContextKey: 'scope-1',expectedSupervisorSourceDigest:'c'.repeat(64),supervisorSourceToken:'official-source-token-A' }
  assert.equal(m.requireApprovalMatrixSelection(valid).matrixChangeConfirmed, true)
  for (const patch of [{ expectedApprovalRouteId: undefined }, { expectedApprovalRouteVersionNo: undefined }, { expectedApprovalMatrixDigest: undefined },
    { matrixChangeConfirmed: null }, { applicationContextKey: undefined }, { approvalMatrixConfirmationToken: null }]) assert.throws(() => m.requireApprovalMatrixSelection({ ...valid, ...patch }))
})

test('new matrix helper type-checks against the actual public IC13 interfaces without a global build', () => {
  const apiPath = path.resolve(__dirname, '../../src/api/dcc/controlledFile/workflow.ts')
  const api = ts.createSourceFile(apiPath, fs.readFileSync(apiPath, 'utf8'), ts.ScriptTarget.Latest, true)
  const names = new Set(['ApplicationCustomMatrix', 'ApprovalMatrixBaselineVO', 'ApprovalMatrixConfirmationSelection', 'ControlledFileChangeType',
    'ControlledFileRoutePreviewReqVO', 'ControlledFileRoutePreviewVO', 'ControlledFileRouteReadinessBlockerVO', 'ControlledFileRuntimeRuleGroupVO', 'ControlledFileRouteReadinessVO'])
  const declarations = api.statements.filter(node => node.name && names.has(node.name.text)).map(node => node.getText(api))
  assert.equal(declarations.length, names.size)
  const helper = ts.createSourceFile(sourceFile, fs.readFileSync(sourceFile, 'utf8'), ts.ScriptTarget.Latest, true)
  const code = declarations.join('\n') + '\n' + helper.statements.filter(node => !ts.isImportDeclaration(node)).map(node => node.getText(helper)).join('\n')
  const virtual = path.resolve(__dirname, 'ic13-virtual-typecheck.ts')
  const options = { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS, strict: true, noEmit: true, types: [] }
  const host = ts.createCompilerHost(options)
  const read = host.readFile, exists = host.fileExists, getSource = host.getSourceFile
  const isVirtual = file => path.resolve(file) === virtual
  host.readFile = file => isVirtual(file) ? code : read(file)
  host.fileExists = file => isVirtual(file) || exists(file)
  host.getSourceFile = (file, language, onError, createNew) => isVirtual(file) ? ts.createSourceFile(file, code, language, true) : getSource(file, language, onError, createNew)
  const diagnostics = ts.getPreEmitDiagnostics(ts.createProgram([virtual], options, host))
  assert.deepEqual(diagnostics.map(d => `${d.code}: ${ts.flattenDiagnosticMessageText(d.messageText, '\n')}`), [])
})

test('formal rejection reads actual ApiError.details and HTTP response data, not an unrelated generic message', () => {
  const m = load(), h = require('./dcc-upload-test-loader.cjs')
  const error = h.matrixRejection()
  error.message = 'Request failed with status code 400'
  assert.equal(m.isApprovalMatrixStaleError(error), true)
  const result = m.readApprovalMatrixRejection(error)
  assert.equal(result.reasonCode, 'APPROVAL_MATRIX_EXPECTATION_STALE'); assert.equal(result.matrixAfter.routeVersionNo, 3)
  const http = { response: { data: { code: error.code, msg: error.message, data: error.details } } }
  assert.equal(m.readApprovalMatrixRejection(http).approvalMatrixConfirmationToken, 'fixture-confirm-3')
  assert.equal(m.readApprovalMatrixRejection({ code: error.code, msg: error.message, data: error.details }).reasonCode, 'APPROVAL_MATRIX_EXPECTATION_STALE')
  const required = h.matrixRejection({}, 'APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED')
  assert.equal(m.readApprovalMatrixRejection(required).reasonCode, 'APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED')
})

test('the actual Axios interceptor error shape keeps formal comparison fields available to the matrix reader', () => {
  const m = load(), h = require('./dcc-upload-test-loader.cjs')
  const file = path.resolve(__dirname, '../../src/config/axios/service.ts')
  const source = ts.createSourceFile(file, fs.readFileSync(file, 'utf8'), ts.ScriptTarget.Latest, true)
  const st = source.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.text === 'createApiError'))
  assert.ok(st, 'real interceptor error constructor')
  const c = { exports: {}, Error }
  vm.runInNewContext(ts.transpileModule(st.getText(source) + '\nexports.create=createApiError', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  const formal = h.matrixRejection({}, 'APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED')
  const error = c.exports.create(formal.message, formal.code, { code: formal.code, msg: formal.message, data: formal.details })
  const parsed = m.readApprovalMatrixRejection(error)
  assert.equal(parsed.reasonCode, 'APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED')
  assert.equal(parsed.matrixBefore.routeVersionNo, 1); assert.equal(parsed.matrixAfter.routeVersionNo, 3)
  assert.equal(parsed.approvalMatrixConfirmationToken, 'fixture-confirm-3')
})

test('wrong-context comparison rejection never runs a preview reload or accepts its confirmation token', async () => {
  const m = load(), h = require('./dcc-upload-test-loader.cjs'), state = new m.UploadApprovalMatrixState(scope)
  state.accept(state.begin(), response())
  const error = h.matrixRejection({ tenantId: '8' }); let calls = 0
  const feedback = await m.refreshApprovalMatrixRejection(state, error, async () => { calls++; return '' })
  assert.equal(calls, 0); assert.match(feedback, /无法核验.*未接受.*票据/)
  assert.throws(() => state.selection(), /读取/)
})

test('unknown/date/runtime business errors are not matrix-change responses and mismatching code/reason is rejected', () => {
  const m = load(), h = require('./dcc-upload-test-loader.cjs')
  for (const error of [new Error('日期不能早于正式业务当日'), { code: 400, message: 'APPROVAL_MATRIX_EXPECTATION_STALE' },
    { code: 0, details: h.matrixRejection().details }, { code: 1080000362, message: 'APPROVAL_MATRIX_EXPECTATION_STALE' },
    new Error('PUBLIC_MATRIX_DERIVATION_UNSUPPORTED')]) assert.equal(m.isApprovalMatrixStaleError(error), false)
  const error = h.matrixRejection(); error.details.reasonCode = 'OTHER'
  assert.throws(() => m.readApprovalMatrixRejection(error), /原因|分类/)
  error.details.reasonCode = 'APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED'
  assert.throws(() => m.readApprovalMatrixRejection(error), /原因|分类/)
})

test('IC16 new previews and generated selections pin the actual public prepared definition identity', () => {
  const m = load(), state = new m.UploadApprovalMatrixState(scope)
  const request = state.begin(); assert.equal(request.request.executionContract, 'PUBLIC_MATRIX_NATIVE_V2')
  state.accept(request, response())
  const selection = state.selection()
  assert.equal(selection.executionContract, 'PUBLIC_MATRIX_NATIVE_V1')
  assert.equal(selection.expectedProcessDefinitionId, 'dcc-controlled-file-upload:5:exact-def')
  assert.equal(selection.expectedDefinitionDigest, 'a'.repeat(64))
  const repeated = state.begin(); assert.equal(repeated.request.expectedProcessDefinitionId, selection.expectedProcessDefinitionId)
  assert.equal(repeated.request.expectedDefinitionDigest, selection.expectedDefinitionDigest)
})

test('IC16 raw matrix can still be inspected or confirmed when definition is missing, but no new request is generated', async () => {
  const m = load(), state = new m.UploadApprovalMatrixState(scope)
  state.accept(state.begin(), { ...response(baseline(1), baseline(2)), ready: false, executionContract: 'PUBLIC_MATRIX_NATIVE_V1',
    preparedProcessDefinitionId: null, preparedDefinitionDigest: null,
    blockers: [{ reasonCode: 'EXECUTION_CONTRACT_NEW_DEFINITION_NOT_READY', message: 'EXECUTION_CONTRACT_NEW_DEFINITION_NOT_DEPLOYED' }] })
  assert.equal(await state.confirmChange(async () => true), true)
  assert.throws(() => state.selection(), /EXECUTION_CONTRACT_NEW_DEFINITION_NOT_READY.*NOT_DEPLOYED/)
  assert.equal(state.before.routeVersionNo, 1)
  for (const patch of [{ expectedProcessDefinitionId: undefined }, { expectedDefinitionDigest: undefined }, { executionContract: undefined }, { executionContract: 'LEGACY_V4_ACTION' }])
    assert.throws(() => m.requireApprovalMatrixSelection({ ...require('./dcc-upload-test-loader.cjs').matrixSelection(), ...patch }), /流程|合同|定义/)
})

test('IC16 a different actual prepared definition invalidates the previous confirmation even when matrix raw content is unchanged', async () => {
  const m = load(), state = new m.UploadApprovalMatrixState(scope)
  state.accept(state.begin(), response(baseline(1), baseline(2))); await state.confirmChange(async () => true)
  state.accept(state.begin(), { ...response(baseline(1), baseline(2)), preparedProcessDefinitionId: 'dcc-controlled-file-upload:6:other-def', preparedDefinitionDigest: 'b'.repeat(64) })
  assert.throws(() => state.selection(), /确认|核对/)
  await state.confirmChange(async () => true)
  assert.equal(state.selection().expectedProcessDefinitionId, 'dcc-controlled-file-upload:6:other-def')
  const same = new m.UploadApprovalMatrixState(scope); same.accept(same.begin(), response())
  same.accept(same.begin(), { ...response(), preparedProcessDefinitionId: 'dcc-controlled-file-upload:6:other-def', preparedDefinitionDigest: 'b'.repeat(64) })
  assert.throws(() => same.selection(), /流程|确认/)
})

test('IC16 partial, malformed or non-public prepared identities cannot be hidden by a ready response', () => {
  const m = load()
  for (const patch of [{ executionContract: 'LEGACY_V4_ACTION' }, { preparedProcessDefinitionId: '' },
    { preparedProcessDefinitionId: ' def ' }, { preparedProcessDefinitionId: 123 }, { preparedDefinitionDigest: 'bad' },
    { preparedDefinitionDigest: null }, { preparedProcessDefinitionId: null }]) {
    const state = new m.UploadApprovalMatrixState(scope)
    assert.throws(() => state.accept(state.begin(), { ...response(), ...patch }), /流程|合同|定义/)
  }
})

test('IC16 a transient not-ready response never erases the selected definition expectation to permit a latest replacement', () => {
  const m = load(), state = new m.UploadApprovalMatrixState(scope)
  state.accept(state.begin(), response())
  state.accept(state.begin(), { ...response(), ready: false, preparedProcessDefinitionId: null, preparedDefinitionDigest: null,
    blockers: [{ reasonCode: 'EXECUTION_CONTRACT_NEW_DEFINITION_NOT_READY', message: '定义暂不可读取' }] })
  assert.throws(() => state.selection(), /定义暂不可读取/)
  const request = state.begin()
  assert.equal(request.request.expectedProcessDefinitionId, 'dcc-controlled-file-upload:5:exact-def')
  assert.equal(request.request.expectedDefinitionDigest, 'a'.repeat(64))
  state.accept(request, { ...response(), preparedProcessDefinitionId: 'dcc-controlled-file-upload:6:replacement', preparedDefinitionDigest: 'b'.repeat(64) })
  assert.throws(() => state.selection(), /流程.*变化|确认/)
})

const sourceA={supervisorSourceDigest:'c'.repeat(64),supervisorSourceToken:'official-source-token-A'}
const sourceB={supervisorSourceDigest:'d'.repeat(64),supervisorSourceToken:'official-source-token-B'}
test('IC31 native NEW and REVISION selections preserve the exact server source pair rather than manufacture one',()=>{
  for(const actionType of ['NEW','REVISION']){
    const state=new (load().UploadApprovalMatrixState)({...scope,actionType})
    state.accept(state.begin(),{...response(),applicationActionType:actionType,...sourceA})
    const selection=state.selection('SUBMIT')
    assert.equal(selection.expectedSupervisorSourceDigest,sourceA.supervisorSourceDigest)
    assert.equal(selection.supervisorSourceToken,sourceA.supervisorSourceToken)
  }
})
test('IC31 ready native missing, partial or invalid supervisor evidence is rejected instead of ready fallback',()=>{
  for(const patch of [{supervisorSourceDigest:undefined,supervisorSourceToken:undefined},
    {supervisorSourceDigest:undefined},{supervisorSourceToken:undefined},{supervisorSourceDigest:'bad'},
    {supervisorSourceToken:''},{supervisorSourceToken:' spaced '},{supervisorSourceToken:'broken\ntoken'}]){
    const state=new (load().UploadApprovalMatrixState)(scope)
    assert.throws(()=>state.accept(state.begin(),{...response(),...sourceA,...patch}),/主管.*来源|来源.*主管/)
  }
})
test('IC31 unready native source keeps its real matrix and blocker while explicit SAVE omits the absent source pair',()=>{
  const state=new (load().UploadApprovalMatrixState)(scope)
  const unready={...response(),ready:false,supervisorSourceDigest:null,supervisorSourceToken:null,blockers:[{reasonCode:'DIRECT_SUPERVISOR_SOURCE_NOT_CONFIGURED',message:'真实主管来源尚未安装'}]}
  state.accept(state.begin(),unready)
  assert.equal(state.before.matrixDigest,baseline(1).matrixDigest)
  const saved=state.selection('SAVE')
  assert.equal(saved.expectedSupervisorSourceDigest,undefined);assert.equal(saved.supervisorSourceToken,undefined)
  assert.throws(()=>state.selection('SUBMIT'),/DIRECT_SUPERVISOR_SOURCE_NOT_CONFIGURED.*真实主管来源尚未安装/)
})
test('IC31 source changes require fresh confirmation and a cancelled decision never authorizes a replacement supervisor',async()=>{
  const state=new (load().UploadApprovalMatrixState)(scope)
  state.accept(state.begin(),{...response(),...sourceA})
  state.accept(state.begin(),{...response(),...sourceB})
  assert.throws(()=>state.selection('SUBMIT'),/主管.*变化.*确认/)
  const saved=state.selection('SAVE');assert.equal(saved.expectedSupervisorSourceDigest,undefined)
  assert.equal(await state.confirmChange(async rows=>{
    const text=rows.map(row=>row.value).join('\n')
    assert.doesNotMatch(text,/official-source-token|cccccccccccccccc|dddddddddddddddd/)
    return false
  }),false)
  assert.throws(()=>state.selection('SUBMIT'),/主管.*变化.*确认/)
  assert.equal(await state.confirmChange(async()=>true),true)
  assert.equal(state.selection('SUBMIT').expectedSupervisorSourceDigest,sourceB.supervisorSourceDigest)
})
test('IC31 transient source unavailability or another read during confirmation cannot authorize its old source',async()=>{
  const state=new (load().UploadApprovalMatrixState)(scope)
  state.accept(state.begin(),{...response(),...sourceA})
  state.accept(state.begin(),{...response(),ready:false,supervisorSourceDigest:null,supervisorSourceToken:null,blockers:[{reasonCode:'SOURCE_OFFLINE',message:'主管来源读取失败'}]})
  assert.throws(()=>state.selection('SUBMIT'),/SOURCE_OFFLINE|来源/)
  state.accept(state.begin(),{...response(),...sourceB})
  assert.throws(()=>state.selection('SUBMIT'),/主管.*变化.*确认/)
  let release;const pending=state.confirmChange(()=>new Promise(r=>release=r))
  state.begin();release(true);assert.equal(await pending,false)
  assert.throws(()=>state.selection('SUBMIT'),/读取/)
})
test('IC31 standalone selection validates source pairs only for native formal submission and preserves draft and obsolete separation',()=>{
  const m=load(),valid={...require('./dcc-upload-test-loader.cjs').matrixSelection(),expectedSupervisorSourceDigest:undefined,supervisorSourceToken:undefined}
  const pair={...valid,expectedSupervisorSourceDigest:sourceA.supervisorSourceDigest,supervisorSourceToken:sourceA.supervisorSourceToken}
  assert.equal(m.requireApprovalMatrixSelection(pair,{phase:'SUBMIT',actionType:'NEW'}).supervisorSourceToken,sourceA.supervisorSourceToken)
  assert.throws(()=>m.requireApprovalMatrixSelection(valid,{phase:'SUBMIT',actionType:'REVISION'}),/主管.*来源/)
  assert.doesNotThrow(()=>m.requireApprovalMatrixSelection(valid,{phase:'SAVE',actionType:'NEW'}))
  assert.doesNotThrow(()=>m.requireApprovalMatrixSelection(valid,{phase:'SUBMIT',actionType:'OBSOLETE'}))
  for(const phase of ['SUBMIT','SAVE'])assert.throws(()=>m.requireApprovalMatrixSelection({...pair,supervisorSourceToken:undefined},{phase,actionType:'NEW'}),/主管.*来源/)
  const obsolete=new m.UploadApprovalMatrixState({...scope,actionType:'OBSOLETE'})
  obsolete.accept(obsolete.begin(),{...response(),applicationActionType:'OBSOLETE'})
  assert.equal(obsolete.selection().expectedSupervisorSourceDigest,undefined)
})

test('IC31 a matrix-only confirmation while the replacement source is unavailable cannot acknowledge that future supervisor',async()=>{
  const state=new (load().UploadApprovalMatrixState)(scope)
  state.accept(state.begin(),{...response(),...sourceA});state.accept(state.begin(),{...response(),...sourceB})
  state.accept(state.begin(),{...response(),ready:false,supervisorSourceDigest:null,supervisorSourceToken:null,
    blockers:[{reasonCode:'SOURCE_OFFLINE',message:'主管来源暂不可读取'}]})
  assert.equal(await state.confirmChange(async()=>true),true)
  assert.equal(state.selection('SAVE').supervisorSourceToken,undefined)
  state.accept(state.begin(),{...response(),...sourceB})
  assert.throws(()=>state.selection('SUBMIT'),/主管.*变化.*确认/)
  assert.equal(await state.confirmChange(async()=>true),true)
  assert.equal(state.selection('SUBMIT').supervisorSourceToken,sourceB.supervisorSourceToken)
})
