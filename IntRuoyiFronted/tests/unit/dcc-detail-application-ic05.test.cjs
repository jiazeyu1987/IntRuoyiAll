const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const file = path.join(root, 'src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue')
const descriptor = parse(fs.readFileSync(file, 'utf8')).descriptor
const source = descriptor.scriptSetup.content
const ast = ts.createSourceFile('panel.ts', source, ts.ScriptTarget.Latest, true)
const declaration = name => {
  const st = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.text === name))
  assert.ok(st, 'actual panel declaration ' + name); return st.getText(ast)
}
const load = (name, deps = {}) => {
  const c = { exports: {}, Error, Date, JSON, require: id => deps[id] || { validateAttributes: value => value } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/', name), 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return c.exports
}
const departments = load('upload/signoff-departments.ts')
const matrixHelper = require('./dcc-upload-test-loader.cjs')
const matrixModule = matrixHelper.loadUploadMatrix()
const effects = load('upload/submitter.ts', { './signoff-departments': departments, './approval-matrix': matrixModule })
const revisions = load('revision/revision-model.ts')
const versionDisplay = load('shared/version-display.ts')
// Explicit isolated saved-file response; no production manifest hash is computed here.
const savedAttachmentFacts = () => ({
  ordinaryAttachmentManifestHash: 'd37c10fa9b05e2a6c281ed4f9ab6d1306587a4203f91b7de02ce4a9032f851bc',
  attachments: [
    { attachmentId: '9223372036854775701', controlledFileId: '11', fileName: 'One.pdf', attachmentPurpose: 'ATTACHMENT', sortNo: 0 },
    { attachmentId: '9223372036854775702', controlledFileId: '11', fileName: 'Two.docx', attachmentPurpose: 'ATTACHMENT', sortNo: 1 }
  ]
})
const actual = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
const saved = { controlledFileId: '11', projectId: '5', applicationType: 'UPLOAD', defaultSource: actual, actual, effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', effectiveDate: null, selectedSignoffDepartmentIds: ['7'], selectedDistributionDepartmentIds: ['8'], needTraining: false, changeDescription: '本次说明', canSubmit: true, unavailableReason: null, draftApprovalMatrix: matrixHelper.matrixFixture() }
function host(overrides = {}) {
  const calls = { writes: [], events: [], confirmations: [], defaults: 0 }
  const c = { exports: {}, Error, JSON, Promise, structuredClone, ...vue, ...departments, ...effects, ...matrixModule, approvalMatrixState: vue.ref(), getTenantId: () => '1', getVisitTenantId: () => undefined, validateAttributes: value => value,
    props: { file: { id: '11', masterId: '1', status: 'WORKING', versionNo: 'A/1', categoryId: 2, fileTypeTaxonomyId: '4', dccProjectCodeId: '5', revisionBaseActiveControlledFileId: '10', sourceOriginalFileName: 'source.pdf', fileNumber: 'SOP-1', categoryName: 'SOP' } },
    newActionPreparationEligible: vue.ref(false), preparedNewActionBaseline: vue.ref(false), newActionPreparationFacts: vue.ref(),
    generation: 0, selectionGeneration: 0, working: vue.ref(true), initial: vue.ref(true), opened: vue.ref(false), loading: vue.ref(false), busy: vue.ref(false), error: vue.ref(''), snapshot: vue.ref(), selectedApplicationFile: vue.ref(), revisionOptions: vue.ref(), preparationBlockers: vue.ref([]),
    effectiveMode: vue.ref(null), effectiveDate: vue.ref(null), selectedDepartments: vue.ref([]), selectedDistributionDepartments: vue.ref(null), needTraining: vue.ref(false), description: vue.ref(''), departmentChoiceMissing: vue.ref(false), distributionChoiceMissing: vue.ref(false),
    departments: vue.ref([]), approvalAccounts: vue.ref([]), approvalGroups: vue.ref([]), approvalCapabilityLimitations: vue.ref([]), approverNames: vue.ref(''), approvalRule: vue.ref(''), canSubmit: vue.ref(false), idempotencyKey: vue.ref('K'), previewSource: vue.ref(), selectedIterationId: vue.ref(''), selectedIntent: vue.ref(), snapshotIntent: vue.ref(), selectionLoading: vue.ref(false), submitted: vue.ref(false),
    getWorkingApplicationAttributes: async () => structuredClone(saved), getProjectDefaults: async () => { calls.defaults++; return { ...actual, targetMarkets: ['NMPA'] } },
    getControlledFileRevisionOptions: async id => ({ controlledBaselineId: id, masterId: '1', baselineVersionNo: 'A/1', iterations: [{id:'11',versionNo:'A/1-1',canPartial:true,canReplacement:true,canPreview:true}] }),
    getSimpleDeptList: async () => [{ id: 7, name: '会签甲', status: 0 }, { id: 8, name: '下发乙', status: 0 }], getSimpleUserList: async () => [{ id: 9, nickname: '批准丙', status: 0 }],
    checkControlledFileRouteReadiness: async () => matrixHelper.matrixReadiness({ ready: true, blockers: [], nodes: [{ stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: ['7'] }, { stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'], approveMethod: 'ALL' }] }),
    ...versionDisplay, readApplicationOrdinaryAttachmentExpectation: revisions.readApplicationOrdinaryAttachmentExpectation,
    generateUUID: () => 'K', isWorkflowId: value => value === '11', buildInitialCommand: revisions.buildInitialCommand,
    submitControlledFileWorkingIteration: async (id, payload) => { calls.writes.push({ id, payload }); return '11' }, emit: (...args) => calls.events.push(args), message: { success() {} }, ElMessageBox: { confirm: async content => calls.confirmations.push(content) }, ...overrides
  }
  c.props.file = { ...savedAttachmentFacts(), ...c.props.file }
  const code = ['isPublicMatrixExecution', 'applicationProjection', 'applicationFacts', 'applicationSelectionError', 'applicationMatrixScope', 'displaySavedNewAction', 'prepareNewActionBaseline', 'open', 'refreshPanelMatrix', 'preparePanelMatrix', 'confirmation', 'departmentSelectionKey', 'saveAttributes', 'submitInitial'].map(declaration).join('\n') + '\nexports={open,prepareNewActionBaseline,saveAttributes,submitInitial,applicationFacts};'
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return { c, calls }
}
test('formal saved signature mode and independent lists reopen without reading changed project defaults', async () => {
  const { c, calls } = host(); await c.exports.open()
  assert.equal(c.effectiveMode.value, 'AFTER_STANDARDIZATION_SIGNATURE'); assert.equal(c.effectiveDate.value, null)
  assert.deepEqual(Array.from(c.selectedDistributionDepartments.value), ['8']); assert.deepEqual(Array.from(c.selectedDepartments.value), ['7'])
  assert.equal(calls.defaults, 0)
  assert.equal(c.exports.applicationFacts.value.effectiveMode, 'AFTER_STANDARDIZATION_SIGNATURE')
  assert.deepEqual(Array.from(c.exports.applicationFacts.value.selectedDistributionDepartmentIds), ['8'])
})

test('IC13 actual saved preview request binds exact file project category action and the same outgoing context', async () => {
  const requests = []
  const { c, calls } = host({ checkControlledFileRouteReadiness: async request => {
    requests.push(request)
    return matrixHelper.matrixReadiness({ ready: true, blockers: [], nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'], approveMethod: 'ALL' }] })
  } })
  await c.exports.open(); await c.exports.submitInitial()
  assert.equal(requests[0].controlledFileId, '11'); assert.equal(requests[0].dccProjectCodeId, '5')
  assert.equal(requests[0].categoryId, '2'); assert.equal(requests[0].actionType, 'NEW')
  assert.equal(requests[0].applicationContextKey, 'K'); assert.equal(calls.writes[0].payload.applicationContextKey, 'K')
  assert.equal(c.approvalMatrixState.value.before.matrixDigest, saved.draftApprovalMatrix.matrixDigest)
})
test('unrecorded saved mode and list remain absent and cannot generate a request', async () => {
  const { c, calls } = host({ getWorkingApplicationAttributes: async () => ({ ...saved, effectiveMode: null, selectedDistributionDepartmentIds: null }) })
  await c.exports.open(); assert.equal(c.effectiveMode.value, null); assert.equal(c.selectedDistributionDepartments.value, null)
  assert.equal(c.exports.applicationFacts.value, undefined)
  await c.exports.submitInitial(); assert.equal(calls.writes.length, 0)
})

test('a formally saved empty signoff draft reopens for correction but cannot submit', async () => {
  const { c, calls } = host({ getWorkingApplicationAttributes: async () => ({ ...saved, selectedSignoffDepartmentIds: [], canSubmit: false, unavailableReason: '本次会签部门尚未填写' }) })
  await c.exports.open()
  assert.ok(c.snapshot.value, c.error.value); assert.deepEqual(Array.from(c.selectedDepartments.value), [])
  assert.equal(c.error.value, '本次会签部门尚未填写')
  assert.equal(c.exports.applicationFacts.value, undefined)
  await c.exports.submitInitial(); assert.equal(calls.writes.length, 0)
})
test('initial confirmation and payload contain exact saved independent choices and second click cannot duplicate', async () => {
  const { c, calls } = host(); await c.exports.open(); await c.exports.submitInitial(); await c.exports.submitInitial()
  assert.equal(calls.writes.length, 1, c.error.value); assert.equal(calls.events[0][1], '11')
  assert.equal(calls.writes[0].payload.effectiveMode, 'AFTER_STANDARDIZATION_SIGNATURE'); assert.equal(calls.writes[0].payload.effectiveDate, null)
  assert.deepEqual(Array.from(calls.writes[0].payload.selectedDistributionDepartmentIds), ['8'])
  const text = typeof calls.confirmations[0] === 'string' ? calls.confirmations[0] : JSON.stringify(calls.confirmations[0])
  assert.match(text, /下发乙/); assert.match(text, /全.*人员|全员/); assert.match(text, /生效方式：文控审核后立即生效/); assert.doesNotMatch(text, /AFTER_STANDARDIZATION_SIGNATURE/)
})
test('real panel has business choices, independent distribution and honest training responsibility and compiles', () => {
  assert.match(descriptor.template.content, /文控审核后立即生效/); assert.match(descriptor.template.content, /延迟生效/)
  assert.match(descriptor.template.content, /v-model="effectiveMode"/); assert.match(descriptor.template.content, /v-model="selectedDistributionDepartments"/)
  assert.doesNotMatch(descriptor.template.content, /文控上传本次线下培训/)
  compileScript(descriptor, { id: 'ic05-panel' }); assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file, id: 'ic05-panel' }).errors, [])
})

test('IC08 save sends complete current selections and verifies the formal saved readback', async () => {
  let current = structuredClone(saved); const writes = [], success = []
  const { c } = host({
    getWorkingApplicationAttributes: async () => structuredClone(current),
    saveWorkingApplicationAttributes: async (id, actual, selection) => { writes.push({ id, actual, selection }); current = { ...current, ...selection, actual }; return true },
    message: { success: text => success.push(text) }
  })
  await c.exports.open(); c.selectedDistributionDepartments.value = []; c.description.value = '新说明'
  await c.exports.saveAttributes()
  assert.equal(writes.length, 1)
  assert.equal(writes[0].selection.effectiveMode, 'AFTER_STANDARDIZATION_SIGNATURE')
  assert.equal(writes[0].selection.effectiveDate, null)
  assert.deepEqual(Array.from(writes[0].selection.selectedDistributionDepartmentIds), [])
  assert.equal(writes[0].selection.changeDescription, '新说明')
  assert.match(success[0], /本次申请草稿已保存/)
})

test('IC08 department readback accepts canonical ordering of the same independently saved members', async () => {
  let current = structuredClone(saved); const success = []
  const { c } = host({
    getWorkingApplicationAttributes: async () => structuredClone(current),
    saveWorkingApplicationAttributes: async (_id, actual, selection) => {
      current = { ...current, ...selection, actual,
        selectedSignoffDepartmentIds: [...selection.selectedSignoffDepartmentIds].sort(),
        selectedDistributionDepartmentIds: [...selection.selectedDistributionDepartmentIds].sort() }
    }, message: { success: text => success.push(text) }
  })
  await c.exports.open()
  c.selectedDepartments.value = ['9', '7']; c.selectedDistributionDepartments.value = ['8', '10']
  await c.exports.saveAttributes()
  assert.equal(c.error.value, '')
  assert.equal(success.length, 1)
  assert.deepEqual(JSON.parse(JSON.stringify(c.snapshot.value.selectedSignoffDepartmentIds)), ['7', '9'])
  assert.deepEqual(JSON.parse(JSON.stringify(c.snapshot.value.selectedDistributionDepartmentIds)), ['10', '8'])
  assert.deepEqual(JSON.parse(JSON.stringify(c.selectedDepartments.value)), ['9', '7'], 'do not reorder the user selection in place')
})

test('IC08 saved department members accept safe numeric transport without changing either selection', async () => {
  let current = structuredClone(saved); const success = []
  const { c } = host({
    getWorkingApplicationAttributes: async () => structuredClone(current),
    saveWorkingApplicationAttributes: async (_id, actual, selection) => {
      current = { ...current, ...selection, actual,
        selectedSignoffDepartmentIds: [...selection.selectedSignoffDepartmentIds].reverse().map(Number),
        selectedDistributionDepartmentIds: [...selection.selectedDistributionDepartmentIds].reverse().map(Number) }
    }, message: { success: text => success.push(text) }
  })
  await c.exports.open()
  c.selectedDepartments.value = ['9', '7']; c.selectedDistributionDepartments.value = ['8', '10']
  await c.exports.saveAttributes()
  assert.equal(c.error.value, '')
  assert.equal(success.length, 1)
  assert.deepEqual(Array.from(c.selectedDepartments.value), ['9', '7'])
  assert.deepEqual(Array.from(c.selectedDistributionDepartments.value), ['8', '10'])
})

test('IC08 unsafe numeric saved department identity cannot be accepted as its rounded string', async () => {
  let current = structuredClone(saved); const success = []
  const { c } = host({
    getWorkingApplicationAttributes: async () => structuredClone(current),
    saveWorkingApplicationAttributes: async (_id, actual, selection) => {
      current = { ...current, ...selection, actual, selectedDistributionDepartmentIds: [9007199254740992] }
    }, message: { success: text => success.push(text) }
  })
  await c.exports.open(); c.selectedDistributionDepartments.value = ['9007199254740992']
  await c.exports.saveAttributes()
  assert.equal(success.length, 0)
  assert.ok(c.error.value)
})

test('IC08 a mismatching formal readback never claims all selections were persisted', async () => {
  const success = []
  const { c } = host({ saveWorkingApplicationAttributes: async () => true, message: { success: text => success.push(text) } })
  await c.exports.open(); c.selectedDistributionDepartments.value = []
  await c.exports.saveAttributes()
  assert.equal(success.length, 0); assert.match(c.error.value, /已保存.*核对|回显.*一致|保存.*回显/)
})

test('saved PUBLIC application shows approval groups from its exact original raw source rather than unrelated flat defaults', async () => {
  const frozen = [{ stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: ['7'] }, { stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['10'], approveMethod: 'ALL' }]
  const { c } = host({ props: { file: { id: '11', status: 'WORKING', masterId: '1', dccProjectCodeId: '5', categoryId: 2, fileTypeTaxonomyId: '4', versionNo: 'A/1', routeSnapshots: frozen } },
    getWorkingApplicationAttributes: async () => ({ ...structuredClone(saved), draftApprovalMatrix: matrixHelper.matrixFixture({approvalIds:['10']}) }),
    checkControlledFileRouteReadiness: async () => matrixHelper.matrixReadiness({ready:true,blockers:[],nodes:[{stageCode:'MATRIX_APPROVAL',resolvedUserIds:['10'],approveMethod:'ALL'}]},{approvalIds:['10']}),
    getSimpleUserList: async () => [{ id: 9, nickname: '当前批准人', status: 0 }, { id: 10, nickname: '原冻结批准人', status: 0 }] })
  await c.exports.open()
  assert.equal(c.approverNames.value, '原冻结批准人')
  assert.equal(c.approvalGroups.value[0].sourceNodeId, '41')
  assert.match(c.approvalGroups.value[0].summary, /原冻结批准人/)
})

test('IC13 saved panel cancels exact before/after review without submitting or overwriting saved facts', async () => {
  const { c, calls } = host({ checkControlledFileRouteReadiness: async () => matrixHelper.matrixReadiness({ ready: true, blockers: [], nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'], approveMethod: 'ALL' }] }, {}, 1, 2) })
  await c.exports.open(); c.ElMessageBox.confirm = async content => { calls.confirmations.push(content); throw 'cancel' }
  await c.exports.submitInitial()
  assert.equal(calls.writes.length, 0); assert.equal(calls.confirmations.length, 1)
  assert.equal(c.effectiveMode.value, 'AFTER_STANDARDIZATION_SIGNATURE'); assert.deepEqual(Array.from(c.selectedDistributionDepartments.value), ['8'])
  assert.equal(c.approvalMatrixState.value.before.routeVersionNo, 1)
})

test('IC13 saved initial payload includes the server comparison selection after actual explicit confirmation', async () => {
  const { c, calls } = host({ checkControlledFileRouteReadiness: async () => matrixHelper.matrixReadiness({ ready: true, blockers: [], nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'], approveMethod: 'ALL' }] }, {}, 1, 2) })
  await c.exports.open(); await c.exports.submitInitial()
  assert.equal(calls.confirmations.length, 2); assert.equal(calls.writes.length, 1)
  const payload = calls.writes[0].payload
  assert.equal(payload.expectedApprovalRouteVersionNo, 2); assert.equal(payload.approvalMatrixConfirmationToken, 'fixture-confirm-2'); assert.equal(payload.matrixChangeConfirmed, true)
  assert.equal(payload.applicationContextKey, 'K'); assert.equal(payload.effectiveDate, null)
  assert.equal(payload.executionContract, 'PUBLIC_MATRIX_NATIVE_V1')
  assert.equal(payload.expectedProcessDefinitionId, 'dcc-controlled-file-upload:5:fixture-exact')
  assert.equal(payload.expectedDefinitionDigest, 'a'.repeat(64))
})

test('IC16 not-ready exact definition is reported as the server preparation blocker with no new writes', async () => {
  const response = matrixHelper.matrixReadiness({ ready: false, nodes: [], blockers: [{ reasonCode: 'EXECUTION_CONTRACT_NEW_DEFINITION_NOT_READY', message: 'EXECUTION_CONTRACT_NEW_DEFINITION_NOT_DEPLOYED' }] })
  response.preparedProcessDefinitionId = null; response.preparedDefinitionDigest = null
  const { c, calls } = host({ checkControlledFileRouteReadiness: async () => response })
  await c.exports.open(); await c.exports.submitInitial()
  assert.equal(calls.writes.length, 0); assert.equal(c.submitted.value, false)
  assert.match(c.error.value, /EXECUTION_CONTRACT_NEW_DEFINITION_NOT_READY.*NOT_DEPLOYED/)
})

test('IC16 an already prepared different definition cannot be silently substituted during the final business confirmation', async () => {
  const { c, calls } = host()
  await c.exports.open()
  c.ElMessageBox.confirm = async content => {
    calls.confirmations.push(content)
    const next = matrixHelper.matrixReadiness({ ready: true, nodes: [], blockers: [] })
    next.preparedProcessDefinitionId = 'dcc-controlled-file-upload:6:new-exact'
    next.preparedDefinitionDigest = 'b'.repeat(64)
    const state = c.approvalMatrixState.value
    state.accept(state.begin(), next)
    await state.confirmChange(async () => true)
  }
  await c.exports.submitInitial()
  assert.equal(calls.writes.length, 0)
  assert.match(c.error.value, /流程准备|定义|配置.*变化/)
})

test('IC13 runtime signers not ready do not prevent saving an unfilled durable draft with an actual baseline', async () => {
  let current = { ...structuredClone(saved), effectiveMode: null, effectiveDate: null, canSubmit: false, unavailableReason: '未填写生效方式' }
  const writes = []
  const { c } = host({ getWorkingApplicationAttributes: async () => structuredClone(current),
    checkControlledFileRouteReadiness: async () => matrixHelper.matrixReadiness({ ready: false, blockers: [{ message: '批准账号签名未就绪' }], nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'], approveMethod: 'ALL' }] }),
    saveWorkingApplicationAttributes: async (id, actual, selection) => { writes.push(selection); current = { ...current, ...selection, actual }; return true } })
  await c.exports.open(); await c.exports.saveAttributes()
  assert.equal(writes.length, 1, c.error.value); assert.equal(writes[0].effectiveMode, null)
  assert.equal(writes[0].matrixChangeConfirmed, false)
})

test('IC13 a stale submission and failed matrix reload remain visible without an unhandled command or fake success', async () => {
  let reads = 0
  const { c, calls } = host({
    checkControlledFileRouteReadiness: async () => {
      reads++
      if (reads > 2) throw new Error('正式矩阵重新读取失败')
      return matrixHelper.matrixReadiness({ ready: true, blockers: [], nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'], approveMethod: 'ALL' }] })
    }, submitControlledFileWorkingIteration: async () => { throw matrixHelper.matrixRejection() }
  })
  await c.exports.open(); await assert.doesNotReject(c.exports.submitInitial())
  assert.equal(calls.events.length, 0); assert.equal(c.submitted.value, false)
  assert.match(c.error.value, /配置.*变化.*重新读取失败.*正式矩阵/)
})

test('IC13 formal rejection data plus fresh runtime blockers remain visible in the initial handler', async () => {
  let reads = 0
  const { c, calls } = host({ checkControlledFileRouteReadiness: async () => {
    reads++
    return matrixHelper.matrixReadiness({ ready: reads <= 2, blockers: reads > 2 ? [{ reasonCode: 'SIGNATURE_MISSING', stageName: '批准', message: '真实签名资格缺失' }] : [], nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'], approveMethod: 'ALL' }] }, {}, 1, reads > 2 ? 3 : 1)
  }, submitControlledFileWorkingIteration: async () => { throw matrixHelper.matrixRejection({}, 'APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED') } })
  await c.exports.open(); await assert.doesNotReject(c.exports.submitInitial())
  assert.equal(calls.events.length, 0); assert.equal(c.submitted.value, false)
  assert.match(c.error.value, /APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED/)
  assert.match(c.error.value, /SIGNATURE_MISSING.*真实签名资格缺失/)
})

test('IC13 saved panel exposes exact unsupported raw rule reason when readiness has no runtime nodes', async () => {
  const { c, calls } = host({ checkControlledFileRouteReadiness: async () => matrixHelper.matrixReadiness({ ready: false, nodes: [], blockers: [{ reasonCode: 'PUBLIC_MATRIX_DERIVATION_UNSUPPORTED', message: '原规则40：可选批准尚无选择来源' }] }) })
  await c.exports.open()
  assert.match(c.error.value, /PUBLIC_MATRIX_DERIVATION_UNSUPPORTED.*原规则40/)
  assert.equal(c.canSubmit.value, false); assert.equal(calls.writes.length, 0)
})

test('PUBLIC initial confirmation shows its formal original approval group without flattened source inference', async () => {
  const { c, calls } = host()
  await c.exports.open(); await c.exports.submitInitial()
  assert.equal(calls.writes.length, 1)
  const text = calls.confirmations[0]
  assert.match(text, /原批准组 41.*正式批准主体/); assert.match(text, /实际参与：参与.*组内全部批准/)
  assert.match(text, /批准丙.*账号ID 9/)
})

test('PUBLIC original group change during business confirmation is refused even when matrix expected fields stay the same', async () => {
  const { c, calls } = host()
  await c.exports.open()
  c.ElMessageBox.confirm = async text => { calls.confirmations.push(text); c.approvalGroups.value[0].applied = false }
  await c.exports.submitInitial()
  assert.equal(calls.writes.length, 0)
  assert.match(c.error.value, /原组|批准组|配置.*变化/)
})

test('IC21 an eligible new REVISION body opens actual inputs for explicit preparation without a normal preview or write', async () => {
  let previews = 0
  const newAction = { ...structuredClone(saved), applicationType: 'REVISION', draftApprovalMatrix: null,
    newActionBaselineRequired: true, newActionBaselinePreparationAllowed: true, canSubmit: false, unavailableReason: '本次升版尚未准备审批配置',
    executionContract: 'PUBLIC_MATRIX_NATIVE_V1', preparedProcessDefinitionId: 'dcc-controlled-file-revision:5:fixture-exact', preparedDefinitionDigest: 'a'.repeat(64) }
  const { c, calls } = host({ getWorkingApplicationAttributes: async () => newAction,
    checkControlledFileRouteReadiness: async () => { previews++; throw Error('ordinary null-baseline preview forbidden') } })
  await c.exports.open()
  assert.ok(c.snapshot.value, c.error.value); assert.equal(c.newActionPreparationEligible.value, true)
  assert.equal(previews, 0); assert.equal(calls.writes.length, 0)
  assert.deepEqual(Array.from(c.selectedDistributionDepartments.value), ['8'])
  assert.equal(c.snapshot.value.draftApprovalMatrix, null)
  assert.equal(c.revisionOptions.value.controlledBaselineId, '10')
})

const crossActionFacts = () => ({ ...structuredClone(saved), applicationType: 'REVISION', draftApprovalMatrix: null,
  newActionBaselineRequired: true, newActionBaselinePreparationAllowed: true, canSubmit: false, unavailableReason: '本次升版尚未准备审批配置',
  executionContract: 'PUBLIC_MATRIX_NATIVE_V1', preparedProcessDefinitionId: 'dcc-controlled-file-revision:5:fixture-exact', preparedDefinitionDigest: 'a'.repeat(64) })
const crossActionReadiness = () => ({ ...matrixHelper.matrixReadiness({ ready: true, blockers: [], nodes: [
  { stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: ['7'] }, { stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'], approveMethod: 'ALL' }] }, { actionType: 'REVISION' }),
  newActionBaselineRequired: true, newActionBaselinePreparationAllowed: true })

test('IC21 cancellation of explicit new-action preview preserves raw NULL and all input with zero persistence calls', async () => {
  const requests = [], writes = []
  const { c } = host({ getWorkingApplicationAttributes: async () => crossActionFacts(),
    checkControlledFileRouteReadiness: async request => { requests.push(request); return crossActionReadiness() },
    saveWorkingApplicationAttributes: async (...args) => { writes.push(args); return true } })
  await c.exports.open()
  c.description.value = '手工说明'; c.selectedDistributionDepartments.value = []
  c.ElMessageBox.confirm = async () => { throw 'cancel' }
  await c.exports.prepareNewActionBaseline()
  assert.equal(requests.length, 1); assert.equal(requests[0].controlledFileId, '11'); assert.equal(requests[0].actionType, 'REVISION')
  assert.equal(requests[0].prepareNewActionBaseline, true); assert.equal(requests[0].executionContract, 'PUBLIC_MATRIX_NATIVE_V1')
  assert.equal(requests[0].expectedProcessDefinitionId, crossActionFacts().preparedProcessDefinitionId)
  assert.equal(requests[0].expectedDefinitionDigest, crossActionFacts().preparedDefinitionDigest)
  assert.equal(writes.length, 0); assert.equal(c.snapshot.value.draftApprovalMatrix, null)
  assert.equal(c.preparedNewActionBaseline.value, false); assert.equal(c.description.value, '手工说明')
  assert.deepEqual(Array.from(c.selectedDistributionDepartments.value), [])
})

test('IC21 explicit preparation uses returned exact expectations only on a separate durable working save and then reopens actual raw', async () => {
  let current = crossActionFacts(); const writes = [], success = [], requests = []
  const { c } = host({ getWorkingApplicationAttributes: async () => structuredClone(current),
    checkControlledFileRouteReadiness: async request => { requests.push(request); return crossActionReadiness() },
    saveWorkingApplicationAttributes: async (id, actual, selection) => {
      writes.push({ id, actual, selection })
      current = { ...current, ...selection, actual, draftApprovalMatrix: matrixHelper.matrixFixture(), newActionBaselineRequired: false, newActionBaselinePreparationAllowed: false, canSubmit: true, unavailableReason: null }
      return true
    }, message: { success: text => success.push(text) } })
  await c.exports.open(); await c.exports.prepareNewActionBaseline()
  assert.equal(writes.length, 0); assert.equal(c.snapshot.value.draftApprovalMatrix, null)
  assert.equal(c.preparedNewActionBaseline.value, true)
  await c.exports.saveAttributes()
  assert.equal(writes.length, 1, c.error.value)
  const payload = writes[0].selection
  assert.equal(payload.prepareNewActionBaseline, true); assert.equal(payload.applicationContextKey, 'K')
  assert.equal(payload.expectedProcessDefinitionId, 'dcc-controlled-file-revision:5:fixture-exact')
  assert.equal(payload.expectedApprovalRouteId, '9007199254740993'); assert.equal(payload.approvalMatrixBaselineToken, 'fixture-server-baseline')
  assert.equal(c.preparedNewActionBaseline.value, false); assert.ok(c.snapshot.value.draftApprovalMatrix)
  assert.match(success[0], /草稿已保存/)
  const reopened = host({ getWorkingApplicationAttributes: async () => structuredClone(current), checkControlledFileRouteReadiness: async request => {
    assert.equal(request.prepareNewActionBaseline, undefined); return matrixHelper.matrixReadiness({ ready: true, nodes: [
      { stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: ['7'] }, { stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'], approveMethod: 'ALL' }], blockers: [] }, { actionType: 'REVISION' })
  } })
  await reopened.c.exports.open(); assert.equal(reopened.c.newActionPreparationEligible.value, false)
  assert.ok(reopened.c.snapshot.value.draftApprovalMatrix)
})

test('IC21 false or missing eligibility never opens an empty raw bypass and late explicit response stays read-only', async () => {
  for (const patch of [{ newActionBaselineRequired: undefined }, { newActionBaselinePreparationAllowed: false }]) {
    const { c } = host({ getWorkingApplicationAttributes: async () => ({ ...crossActionFacts(), ...patch }) })
    await c.exports.open(); assert.equal(c.newActionPreparationEligible.value, false)
    assert.match(c.error.value, /基线未记录/)
  }
  let release
  const { c } = host({ getWorkingApplicationAttributes: async () => crossActionFacts(), checkControlledFileRouteReadiness: () => new Promise(resolve => { release = resolve }) })
  await c.exports.open(); const pending = c.exports.prepareNewActionBaseline()
  c.generation++; release(crossActionReadiness()); await pending
  assert.equal(c.preparedNewActionBaseline.value, false); assert.equal(c.snapshot.value.draftApprovalMatrix, null)
})

test('IC21 current unsupported execution response is displayed from readiness without pretending a baseline was persisted', async () => {
  const response = crossActionReadiness(); response.ready = false; response.nodes = []; response.runtimeRuleGroups = []
  response.preparedProcessDefinitionId = null; response.preparedDefinitionDigest = null
  response.blockers = [{ reasonCode: 'EXECUTION_CONTRACT_NEW_DEFINITION_NOT_READY', message: '目标定义尚未正式部署' }]
  const { c } = host({ getWorkingApplicationAttributes: async () => crossActionFacts(), checkControlledFileRouteReadiness: async () => response })
  await c.exports.open(); await c.exports.prepareNewActionBaseline()
  assert.equal(c.preparedNewActionBaseline.value, false); assert.equal(c.snapshot.value.draftApprovalMatrix, null)
  assert.match(c.error.value, /EXECUTION_CONTRACT_NEW_DEFINITION_NOT_READY.*目标定义/)
})

test('IC21 returned prepare flags false or a changed context during the review cannot be used as save eligibility', async () => {
  const response = crossActionReadiness(); response.newActionBaselinePreparationAllowed = false
  const { c } = host({ getWorkingApplicationAttributes: async () => crossActionFacts(), checkControlledFileRouteReadiness: async () => response })
  await c.exports.open(); await c.exports.prepareNewActionBaseline()
  assert.equal(c.preparedNewActionBaseline.value, false); assert.match(c.error.value, /资格/)
  const second = host({ getWorkingApplicationAttributes: async () => crossActionFacts(), checkControlledFileRouteReadiness: async () => crossActionReadiness() })
  await second.c.exports.open()
  second.c.ElMessageBox.confirm = async () => { second.c.idempotencyKey.value = 'CHANGED-CONTEXT' }
  await second.c.exports.prepareNewActionBaseline()
  assert.equal(second.c.preparedNewActionBaseline.value, false)
  assert.equal(second.c.snapshot.value.draftApprovalMatrix, null)
})

test('IC21 missing exact saved definition prevents the preparation button and a substituted response never accepts its new raw', async () => {
  for (const patch of [{ preparedProcessDefinitionId: undefined }, { preparedDefinitionDigest: undefined }, { executionContract: 'LEGACY_V4_ACTION' }]) {
    const { c } = host({ getWorkingApplicationAttributes: async () => ({ ...crossActionFacts(), ...patch }) })
    await c.exports.open(); assert.equal(c.newActionPreparationEligible.value, false)
    assert.match(c.error.value, /基线未记录/)
  }
  const changed = crossActionReadiness(); changed.preparedProcessDefinitionId = 'dcc-controlled-file-revision:6:other'
  changed.preparedDefinitionDigest = 'b'.repeat(64)
  const { c } = host({ getWorkingApplicationAttributes: async () => crossActionFacts(), checkControlledFileRouteReadiness: async () => changed })
  await c.exports.open(); await c.exports.prepareNewActionBaseline()
  assert.equal(c.preparedNewActionBaseline.value, false); assert.equal(c.snapshot.value.draftApprovalMatrix, null)
  assert.match(c.error.value, /已保存.*精确.*不一致/)
})

test('IC21 a successful POST without the actual new raw baseline in GET never reports persistence success', async () => {
  const success = [], writes = []
  const { c } = host({ getWorkingApplicationAttributes: async () => crossActionFacts(), checkControlledFileRouteReadiness: async () => crossActionReadiness(),
    saveWorkingApplicationAttributes: async (...args) => { writes.push(args); return true }, message: { success: text => success.push(text) } })
  await c.exports.open(); await c.exports.prepareNewActionBaseline(); await c.exports.saveAttributes()
  assert.equal(writes.length, 1); assert.equal(success.length, 0)
  assert.match(c.error.value, /草稿已保存.*基线.*未确认/)
  assert.equal(c.preparedNewActionBaseline.value, true)
})



test('IC13 saved file without its original raw baseline never substitutes latest configuration as the old baseline', async () => {
  const { c, calls } = host({ getWorkingApplicationAttributes: async () => ({ ...saved, draftApprovalMatrix: null }) })
  await c.exports.open()
  assert.match(c.error.value, /原.*审批矩阵.*未记录|原.*基线.*缺失/)
  assert.equal(c.canSubmit.value, false)
  await c.exports.submitInitial(); assert.equal(calls.writes.length, 0)
})
