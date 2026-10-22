const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const file = name => path.join(root, 'src/views/dcc/controlled-file/upload', name)
const load = name => {
  const c = { exports: {}, Error, Date, JSON, require: id => id.endsWith('approval-matrix') ? require('./dcc-upload-test-loader.cjs').loadUploadMatrix() : id.endsWith('signoff-departments') ? departments : { validateAttributes: value => value } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file(name), 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return c.exports
}
const departments = load('signoff-departments.ts')
const helpers = load('submitter.ts')
const long = '9223372036854775709'
const today = '2026-10-11'
const actual = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
const draft = { processType: 'CONTROLLED_FILE', changeType: 'NEW', categoryId: 2, fileTypeTaxonomyId: '4', dccProjectCodeId: '3', projectFolderId: '5', projectFolderChangeReason: '登记', fileName: 'source.pdf', fileNumber: 'SOP-1', versionNo: 'A/1', relatedControlledFileIds: [], projectAttributes: actual, selectedSignoffDepartmentIds: ['7'], selectedDistributionDepartmentIds: [long], effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', effectiveDate: null, needTraining: false }
const uploaded = { uploadTicket: 'UT1', sessionId: 'S1', fileName: 'source.pdf' }
draft.approvalMatrixSelection = require('./dcc-upload-test-loader.cjs').matrixSelection()
const page = parse(fs.readFileSync(file('index.vue'), 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('upload.ts', page, ts.ScriptTarget.Latest, true)
const declaration = name => ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.text === name)).getText(ast)
const ref = value => ({ value })
const route = ids => ({ ready: true, blockers: [], nodes: [{ stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: ids }] })
const readiness = (selected, distribution, transport) => {
  const calls = []
  const c = { exports: {}, Error, JSON, Promise, ...departments,
    userStore:{getUser:{id:'71',deptId:'7'}},
    formData: { categoryId: '2', fileTypeTaxonomyId: '4', dccProjectCodeId: '3', processType: 'CONTROLLED_FILE', selectedSignoffDepartmentIds: selected, selectedDistributionDepartmentIds: distribution },
    ...require('./dcc-upload-test-loader.cjs').loadUploadMatrix(), approvalMatrixState: ref(), approvalMatrixScopeKey: '',
    getTenantId: () => '1', getVisitTenantId: () => undefined, referenceIdentity: String, uploadFileTypeIdentity: String, createControlledFileUploadSessionId: () => 'scope',
    isCustomApprovalSource: ref(false), selectedFileTypeTaxonomyPath: ref({approvalSourceMode:'PUBLIC_MATRIX'}),
    isExternalReview: ref(false), routeReadinessRequestSeq: 0, routeReadiness: ref(), routeReadinessLoading: ref(false), routeReadinessError: ref(''), routeReadinessSelectionKey: ref(''),
    signoffSelectionCategoryKey: departments.signoffCategoryKey('2', 'CONTROLLED_FILE'), resolveUploadErrorMessage: error => error.message,
    checkControlledFileRouteReadiness: async request => { calls.push(request); const result = transport ? await transport(request) : route(request.selectedSignoffDepartmentIds || ['7', long]); return require('./dcc-upload-test-loader.cjs').matrixReadiness(result) }
  }
  vm.runInNewContext(ts.transpileModule(declaration('ensureUploadApprovalMatrix') + '\n' + declaration('refreshRouteReadiness') + '\nexports.run=refreshRouteReadiness', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return { c, calls }
}

test('actual readiness initially copies the public defaults then never overwrites independently edited lists', async () => {
  const { c, calls } = readiness(undefined, undefined)
  await c.exports.run()
  assert.equal(calls[0].selectedSignoffDepartmentIds, undefined)
  assert.deepEqual(Array.from(c.formData.selectedSignoffDepartmentIds), ['7', long])
  assert.deepEqual(Array.from(c.formData.selectedDistributionDepartmentIds), ['7', long])
  c.formData.selectedSignoffDepartmentIds = ['8']; c.formData.selectedDistributionDepartmentIds = []
  await c.exports.run()
  assert.deepEqual(Array.from(c.formData.selectedDistributionDepartmentIds), [])
  assert.equal(calls.length, 2)
})

test('IC31 a changed supervisor source in the separate default-department read cannot authorize the earlier selected route',async()=>{
  let count=0
  const {c,calls}=readiness(['8'],undefined,async request=>{
    const result=route(request.selectedSignoffDepartmentIds||['7',long])
    result.nodes.push({stageCode:'SUPERVISOR_REVIEW',resolvedUserIds:[++count===1?'72':'73']})
    return result
  })
  assert.equal(await c.exports.run(),false)
  assert.equal(calls.length,2);assert.equal(c.routeReadiness.value,undefined)
  assert.deepEqual(Array.from(c.formData.selectedSignoffDepartmentIds),['8'])
  assert.equal(c.formData.selectedDistributionDepartmentIds,undefined)
  assert.match(c.routeReadinessError.value,/主管.*变化/)
})

test('IC31 default-department source token never replaces the formal source token bound to the actual selected department set',async()=>{
  const {c,calls}=readiness(['8'],undefined,async request=>({...route(request.selectedSignoffDepartmentIds||['7',long]),
    nodes:[...route(request.selectedSignoffDepartmentIds||['7',long]).nodes,{stageCode:'SUPERVISOR_REVIEW',resolvedUserIds:['72']}],
    supervisorSourceDigest:(request.selectedSignoffDepartmentIds?'c':'d').repeat(64),
    supervisorSourceToken:request.selectedSignoffDepartmentIds?'official-selected-8-token':'official-default-departments-token'}))
  assert.equal(await c.exports.run(),true,c.routeReadinessError.value);assert.equal(calls.length,2)
  const selected=c.approvalMatrixState.value.selection('SUBMIT')
  assert.equal(selected.expectedSupervisorSourceDigest,'c'.repeat(64));assert.equal(selected.supervisorSourceToken,'official-selected-8-token')
  assert.deepEqual(Array.from(c.formData.selectedDistributionDepartmentIds),['7',long])
  assert.deepEqual(Array.from(c.formData.selectedSignoffDepartmentIds),['8'])
})

test('a missing distribution selection is initialized from a separate default read, never hand-edited signoff', async () => {
  const { c, calls } = readiness(['8'], undefined)
  await c.exports.run()
  assert.deepEqual(Array.from(c.formData.selectedSignoffDepartmentIds), ['8'])
  assert.deepEqual(Array.from(c.formData.selectedDistributionDepartmentIds), ['7', long])
  assert.equal(calls.length, 2)
  assert.equal(calls[1].selectedSignoffDepartmentIds, undefined)
})

test('a stale default read cannot change either list after a category switch', async () => {
  let release
  const { c } = readiness(['8'], undefined, request => request.selectedSignoffDepartmentIds ? route(['8']) : new Promise(resolve => { release = resolve }))
  const pending = c.exports.run(); await new Promise(resolve => setImmediate(resolve))
  assert.equal(typeof release, 'function')
  c.formData.categoryId = '3'; c.routeReadinessRequestSeq++; c.formData.selectedDistributionDepartmentIds = ['9']
  release(route(['7', long])); await pending
  assert.deepEqual(Array.from(c.formData.selectedDistributionDepartmentIds), ['9'])
})

test('IC13 a project change during a pending read cannot authorize the new project with an old response', async () => {
  let release
  const { c } = readiness(['8'], ['9'], () => new Promise(resolve => { release = resolve }))
  const pending = c.exports.run(); await new Promise(resolve => setImmediate(resolve))
  c.formData.dccProjectCodeId = '6'
  release(route(['8'])); await pending
  assert.equal(c.routeReadiness.value, undefined)
  assert.deepEqual(Array.from(c.formData.selectedDistributionDepartmentIds), ['9'])
  assert.throws(() => c.approvalMatrixState.value.selection(), /读取|配置/)
})

test('IC13 upload request scope stays stable across business edits and equals the generated submit selection scope', async () => {
  const { c, calls } = readiness(['8'], ['9'])
  await c.exports.run()
  const original = c.approvalMatrixState.value
  c.formData.effectiveDate = '2099-01-01'; c.formData.selectedDistributionDepartmentIds = []
  await c.exports.run()
  assert.equal(c.approvalMatrixState.value, original)
  assert.equal(calls[0].applicationContextKey, calls[1].applicationContextKey)
  assert.equal(calls[0].dccProjectCodeId, '3'); assert.equal(calls[0].categoryId, '2'); assert.equal(calls[0].actionType, 'NEW')
  const selection = c.approvalMatrixState.value.selection()
  assert.equal(selection.applicationContextKey, calls[0].applicationContextKey)
  assert.equal(calls[1].approvalMatrixBaselineToken, 'fixture-server-baseline')
  assert.deepEqual(Array.from(c.formData.selectedDistributionDepartmentIds), [])
})

test('IC13 formal runtime blocker without runnable nodes is displayed from readiness without invented defaults', async () => {
  const { c } = readiness(['8'], ['9'], async () => ({ ready: false, nodes: [], blockers: [{ reasonCode: 'PUBLIC_MATRIX_DERIVATION_UNSUPPORTED', message: '原规则40：可选批准尚无选择来源' }] }))
  assert.equal(await c.exports.run(), false)
  assert.equal(c.routeReadinessError.value, '')
  assert.equal(c.routeReadiness.value.blockers[0].reasonCode, 'PUBLIC_MATRIX_DERIVATION_UNSUPPORTED')
  assert.match(c.routeReadiness.value.blockers[0].message, /原规则40/)
  assert.deepEqual(Array.from(c.formData.selectedSignoffDepartmentIds), ['8'])
  assert.deepEqual(Array.from(c.formData.selectedDistributionDepartmentIds), ['9'])
})

test('explicit mode is mandatory and signature mode never manufactures or accepts a preset date', () => {
  assert.equal(typeof helpers.buildUploadEffectiveChoice, 'function')
  for (const mode of [undefined, null, '', 'DEFAULT', false]) assert.throws(() => helpers.buildUploadEffectiveChoice(mode, null, today), /生效方式/)
  const effect = helpers.buildUploadEffectiveChoice('AFTER_STANDARDIZATION_SIGNATURE', null, today)
  assert.equal(effect.effectiveMode, 'AFTER_STANDARDIZATION_SIGNATURE'); assert.equal(effect.effectiveDate, null)
  for (const date of ['', today, '2026-01-01']) assert.throws(() => helpers.buildUploadEffectiveChoice('AFTER_STANDARDIZATION_SIGNATURE', date, today), /日期/)
})

test('date mode requires a real calendar date at entry and again when submitted across midnight', () => {
  assert.equal(typeof helpers.buildUploadEffectiveChoice, 'function')
  for (const date of [null, '', '2026-10-10', '2026-02-29']) assert.throws(() => helpers.buildUploadEffectiveChoice('PRESET_DATE', date, today), /日期/)
  assert.equal(helpers.buildUploadEffectiveChoice('PRESET_DATE', today, today).effectiveDate, today)
  assert.throws(() => helpers.buildUploadEffectiveChoice('PRESET_DATE', today, '2026-10-12'), /今天/)
})

test('actual mode-change handler clears a previous preset date without supplying a new fake date', () => {
  const c = { exports: {}, formData: { effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', effectiveDate: '2099-11-01' }, formRef: ref({ clearValidate() {} }) }
  vm.runInNewContext(ts.transpileModule(declaration('handleEffectiveModeChange') + '\nexports.run=handleEffectiveModeChange', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  c.exports.run(); assert.equal(c.formData.effectiveDate, null)
  c.formData.effectiveMode = 'PRESET_DATE'; c.exports.run(); assert.equal(c.formData.effectiveDate, null)
})

test('new draft leaves the decision unfilled and disabled submit uses the actual effect validation', () => {
  const c = { exports: {}, resolveProcessTypeByRoute: () => 'CONTROLLED_FILE', isExternalReview: ref(false), DEFAULT_MANUAL_VERSION_NO: 'V1.0', DEFAULT_CONTROLLED_INITIAL_VERSION_NO: 'A/1' }
  vm.runInNewContext(ts.transpileModule(declaration('createInitialUploadDraft') + '\nexports.draft=createInitialUploadDraft()', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  assert.equal(c.exports.draft.effectiveMode, null); assert.equal(c.exports.draft.effectiveDate, null)
  assert.equal(c.exports.draft.selectedDistributionDepartmentIds, undefined)
  const gate = { exports: {}, Boolean, computed: fn => ({ get value() { return fn() } }), effectiveSelectionValidation: ref({ valid: false }), selectedCategory: ref(undefined) }
  vm.runInNewContext(ts.transpileModule(declaration('submitBlockedByRouteReadiness') + '\nexports.blocked=submitBlockedByRouteReadiness', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, gate)
  assert.equal(gate.exports.blocked.value, true)
  gate.effectiveSelectionValidation.value.valid = true
  assert.equal(gate.exports.blocked.value, false)
})

test('distribution explicitly permits empty arrays and rejects missing, duplicate or imprecise identities', () => {
  assert.equal(typeof departments.normalizeDistributionDepartments, 'function')
  assert.deepEqual(Array.from(departments.normalizeDistributionDepartments([])), [])
  assert.deepEqual(Array.from(departments.normalizeDistributionDepartments([long])), [long])
  for (const value of [null, undefined, [long, long], [9007199254740992], ['0'], ['9223372036854775808']]) assert.throws(() => departments.normalizeDistributionDepartments(value), /下发|身份/)
})

test('initial independent lists consume the formal default route response without aliasing', () => {
  assert.equal(typeof departments.initialUploadDepartmentSelection, 'function')
  const source = { ready: true, blockers: [], nodes: [{ stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: ['7', long] }] }
  const selection = departments.initialUploadDepartmentSelection(source)
  selection.selectedSignoffDepartmentIds.splice(0, 1)
  assert.deepEqual(Array.from(selection.selectedDistributionDepartmentIds), ['7', long])
  selection.selectedDistributionDepartmentIds.push('8')
  assert.deepEqual(source.nodes[0].candidateSourceIds, ['7', long])
  for (const bad of [undefined, { ...source, nodes: [] }, { ...source, nodes: [{ ...source.nodes[0], candidateSourceIds: [] }] }]) assert.throws(() => departments.initialUploadDepartmentSelection(bad))
})

test('real production payload sends explicit mode, null date and a detached independent distribution list', () => {
  const payload = helpers.buildSubmitPayload(draft, uploaded)
  assert.equal(payload.effectiveMode, 'AFTER_STANDARDIZATION_SIGNATURE')
  assert.equal(payload.effectiveDate, null)
  assert.deepEqual(Array.from(payload.selectedDistributionDepartmentIds), [long])
  payload.selectedDistributionDepartmentIds.push('8')
  assert.deepEqual(draft.selectedDistributionDepartmentIds, [long])
  const empty = helpers.buildSubmitPayload({ ...draft, selectedDistributionDepartmentIds: [] }, uploaded)
  assert.deepEqual(Array.from(empty.selectedDistributionDepartmentIds), [])
  for (const mode of [undefined, null, 'LEGACY']) assert.throws(() => helpers.buildSubmitPayload({ ...draft, effectiveMode: mode }, uploaded), /生效方式/)
})

test('external review preserves its independent date-only contract without a new required mode', () => {
  const external = { ...draft, processType: 'EXTERNAL_REVIEW', effectiveMode: null, effectiveDate: '2099-10-11' }
  const payload = helpers.buildSubmitPayload(external, uploaded)
  assert.equal(Object.hasOwn(payload, 'effectiveMode'), false); assert.equal(payload.effectiveDate, '2099-10-11')
  assert.throws(() => helpers.buildSubmitPayload({ ...external, effectiveDate: null }, uploaded), /日期/)
  const page = fs.readFileSync(file('index.vue'), 'utf8')
  assert.match(page, /<el-form-item v-if="!isExternalReview" label="生效方式"/)
})

test('real submitter service hands the exact new choices to its production submit dependency', async () => {
  const requests = []
  const service = helpers.createUploadSubmitterService({ submit: async payload => { requests.push(payload); return '9007199254740993' } })
  assert.equal(await service.submit(draft, uploaded), '9007199254740993')
  assert.equal(requests[0].effectiveMode, 'AFTER_STANDARDIZATION_SIGNATURE'); assert.equal(requests[0].effectiveDate, null)
  assert.deepEqual(Array.from(requests[0].selectedDistributionDepartmentIds), [long])
  await assert.rejects(service.submit({ ...draft, effectiveMode: null }, uploaded), /生效方式/)
  assert.equal(requests.length, 1)
})

test('durable working draft creation allows an unfilled choice and retains all concrete business input', async () => {
  assert.equal(typeof helpers.buildWorkingDraftPayload, 'function')
  const unfilled = { ...draft, effectiveMode: null, effectiveDate: null, remark: '草稿说明' }
  const payload = helpers.buildWorkingDraftPayload(unfilled, uploaded)
  assert.equal(payload.effectiveMode == null, true); assert.equal(payload.effectiveDate, null)
  assert.deepEqual(Array.from(payload.selectedDistributionDepartmentIds), [long])
  assert.equal(payload.readOnlyUploadTicket, 'UT1'); assert.equal(payload.sourceUploadTicket, undefined); assert.equal(payload.projectFolderId, '5'); assert.equal(payload.remark, '草稿说明')
  const calls = []
  const service = helpers.createUploadSubmitterService({ createWorking: async value => { calls.push(value); return 11 }, submit: async () => { throw Error('must not submit') } })
  assert.equal(await service.saveDraft(unfilled, uploaded), 11)
  assert.equal(calls.length, 1)
  await assert.rejects(service.submit(unfilled, uploaded), /生效方式/)
})

test('confirmation states business mode and independent department names with department-wide distribution', () => {
  const facts = { projectName: '甲', folderName: '目录', fileTypeName: 'SOP', relatedFiles: [], signoffDepartments: [{ id: '7', name: '会签甲' }], distributionDepartments: [{ id: long, name: '下发乙' }], approvers: [{ id: '9', name: '批准' }], approvalRule: '全部批准',compiler:{id:'71',name:'编制甲',deptId:'7',deptName:'编制部门'},supervisor:{id:'72',name:'主管乙',deptId:'8',deptName:'主管部门'} }
  const text = helpers.buildUploadConfirmationSummary(draft, uploaded, facts).map(row => `${row.label}:${row.value}`).join('\n')
  assert.match(text, /生效方式.*文控审核后立即生效/); assert.match(text, /真实.*签名|签名.*时间/)
  assert.match(text, /会签部门.*会签甲/); assert.match(text, /下发部门.*下发乙/); assert.match(text, /全.*人员|全员/)
  assert.doesNotMatch(text, /AFTER_STANDARDIZATION_SIGNATURE|PRESET_DATE|预设生效日期:/)
  const none = helpers.buildUploadConfirmationSummary({ ...draft, selectedDistributionDepartmentIds: [] }, uploaded, { ...facts, distributionDepartments: [] }).map(row => row.value).join('\n')
  assert.match(none, /未选择下发部门/)
  assert.throws(() => helpers.buildUploadConfirmationSummary(draft, uploaded, { ...facts, distributionDepartments: [{ id: '8', name: '错误部门' }] }), /下发/)
})

test('the actual form presents immediate/delayed business choices and separate editable distribution control', () => {
  const page = fs.readFileSync(file('index.vue'), 'utf8')
  const { descriptor, errors } = parse(page, { filename: file('index.vue') }); assert.deepEqual(errors, [])
  assert.match(descriptor.template.content, /文控审核后立即生效/); assert.match(descriptor.template.content, /延迟生效/)
  assert.match(descriptor.template.content, /v-model="formData.effectiveMode"/)
  assert.match(descriptor.template.content, /v-model="formData.selectedDistributionDepartmentIds"/)
  assert.match(descriptor.template.content, /所选.*部门.*全.*人员|所选.*部门.*全员/)
  compileScript(descriptor, { id: 'ic05' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file('index.vue'), id: 'ic05' }).errors, [])
})

test('IC31 actual payload builders preserve native source pairs and allow absent source only for explicit working drafts',()=>{
  const selection=require('./dcc-upload-test-loader.cjs').matrixSelection()
  for(const changeType of ['NEW','REVISION']){
    const request=helpers.buildSubmitPayload({...draft,changeType,approvalMatrixSelection:selection},uploaded)
    assert.equal(request.expectedSupervisorSourceDigest,selection.expectedSupervisorSourceDigest)
    assert.equal(request.supervisorSourceToken,selection.supervisorSourceToken)
  }
  const absent={...selection,expectedSupervisorSourceDigest:undefined,supervisorSourceToken:undefined}
  const saved=helpers.buildWorkingDraftPayload({...draft,approvalMatrixSelection:absent},uploaded)
  assert.equal(saved.expectedSupervisorSourceDigest,undefined);assert.equal(saved.supervisorSourceToken,undefined)
  assert.throws(()=>helpers.buildSubmitPayload({...draft,approvalMatrixSelection:absent},uploaded),/主管.*来源/)
  const partial={...selection,supervisorSourceToken:undefined}
  assert.throws(()=>helpers.buildWorkingDraftPayload({...draft,approvalMatrixSelection:partial},uploaded),/主管.*来源/)
})

 test('dual versions retain attachments and bind the editable source in both submit and working drafts', () => {
  const editable = { ...uploaded, uploadTicket: 'EDITABLE', fileName: 'source.docx' }
  const attachment = { ...uploaded, uploadTicket: 'ATTACHMENT', sessionId: 'attachment-session', fileName: 'evidence.pdf' }
  for (const build of [helpers.buildSubmitPayload, helpers.buildWorkingDraftPayload]) {
    const result = build(draft, uploaded, undefined, [attachment], editable)
    assert.equal(result.readOnlyUploadTicket, uploaded.uploadTicket)
    assert.equal(result.editableUploadTicket, 'EDITABLE')
    assert.equal(result.sourceFileName, 'source.docx')
    assert.equal(result.originalUploadTicket, undefined)
    assert.equal(result.attachmentUploadTickets[0].uploadTicket, 'ATTACHMENT')
    assert.throws(() => build(draft, uploaded, undefined, [], { ...editable, sessionId: 'other' }), /会话/)
  }
})
