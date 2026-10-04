const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const clone = value => JSON.parse(JSON.stringify(value))
const attrs = market => ({ targetMarkets: [market], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' })
const attributes = moduleFrom(read('src/views/dcc/controlled-file/project-attributes/state.ts'))
const revision = moduleFrom(read('src/views/dcc/controlled-file/revision/revision-model.ts'), () => attributes)
const workflowActions = moduleFrom(read('src/views/dcc/controlled-file/workflow/workflow-actions.ts'))
const actionProjection = moduleFrom(read('src/api/form-center/actionProjection.ts'))
const lifecycle = moduleFrom(read('src/views/dcc/controlled-file/shared/lifecycle.ts'), () => actionProjection)
function moduleFrom(source, resolve = id => { throw new Error(id) }) {
  const script = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const context = { exports: {}, require: resolve, Error, Date, structuredClone,
    useMessage: () => ({ success() {} }) }
  vm.runInNewContext(script, context)
  return context.exports
}
function mount(name, props, dependencies) {
  const file = `src/views/dcc/controlled-file/detail/${name}.vue`
  const { descriptor, errors } = parse(read(file))
  assert.equal(errors.length, 0)
  const compiled = compileScript(descriptor, { id: name })
  const template = compileTemplate({ source: descriptor.template.content, filename: file, id: name, compilerOptions: { bindingMetadata: compiled.bindings } })
  assert.equal(template.errors.length, 0, template.errors.join('\n'))
  const component = moduleFrom(compiled.content, id => id === 'vue' ? vue : dependencies[id] || (id.endsWith('.vue') ? {} : (() => { throw new Error(id) })())).default
  component.render = () => vue.h('section')
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }), insert: (child, parent) => parent.children.push(child), remove() {}, setText() {}, setElementText() {}, parentNode: () => null, nextSibling: () => null, patchProp() {} })
  const reactive = vue.reactive(props), events = []
  const app = renderer.createApp({ render: () => vue.h(component, { ...reactive, onSubmitted: value => events.push(value) }) })
  const host = app.mount({ children: [] })
  return { state: host.$.subTree.component.setupState, props: reactive, events, app }
}
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
const baseline = { id: 10, masterId: 100, dccProjectCodeId: 20, categoryId: 30, status: 'ACTIVE', versionNo: 'A/1', title: '说明.pdf' }
const draft = { controlledFileId: '12', projectId: '20', applicationType: 'UPLOAD', defaultSource: attrs('CE'), actual: attrs('FDA'), canSubmit: true,
  unavailableReason: null, effectiveDate: '2026-11-01', needTraining: false, selectedSignoffDepartmentIds: null, changeDescription: '初始申请' }
const options = { controlledBaselineId: '10', baselineVersionNo: 'A/1', masterId: '100', sourceOriginalFileName: '说明.pdf', partialTarget: { versionNo: 'A/2', unavailableReason: null }, replacementTarget: { versionNo: 'B/1', unavailableReason: null }, checkedOutBy: null, checkedOutByName: null, checkedOutTime: null, lockedReason: null, iterations: [
  { id: '13', versionNo: 'A/1-3', canPartial: true, canReplacement: true, canPreview: true }, { id: '12', versionNo: 'A/1-2', canPartial: true, canReplacement: true, canPreview: true }] }

test('INITIAL command validates application facts without a fictitious controlled baseline and freezes actual values', () => {
  const facts = { projectAttributes: attrs('CE'), selectedSignoffDepartmentIds: ['9007199254740993'], needTraining: false, effectiveDate: '2028-02-29' }
  const command = revision.buildInitialCommand(' 初始说明 ', ' initial-key ', facts)
  assert.equal(command.revisionChangeType, 'INITIAL')
  assert.equal(command.controlledBaselineId, undefined)
  assert.equal(command.selectedIterationId, undefined)
  assert.equal(command.idempotencyKey, 'initial-key')
  assert.deepEqual(clone(command.selectedSignoffDepartmentIds), ['9007199254740993'])
  facts.projectAttributes.targetMarkets.push('FDA')
  assert.deepEqual(clone(command.projectAttributes.targetMarkets), ['CE'])
  assert.throws(() => revision.buildInitialCommand('初始说明', 'key', { ...facts, effectiveDate: '2027-02-29' }), /日历/)
  assert.throws(() => revision.buildInitialCommand('初始说明', 'key', { ...facts, needTraining: null }), /培训/)
  assert.throws(() => revision.buildInitialCommand('初始说明', 'key', { ...facts, selectedSignoffDepartmentIds: [9007199254740992] }), /身份/)
})

test('public status labels distinguish working, controlled and effective facts and the doc-control training role', () => {
  assert.equal(lifecycle.getDccControlledFileStatusLabel('WORKING'), '工作版本')
  assert.equal(lifecycle.getDccControlledFileStatusLabel('CONTROLLED_PENDING_EFFECTIVE'), '受控（待生效）')
  assert.equal(lifecycle.getDccControlledFileStatusLabel('ACTIVE'), '受控（已生效）')
  assert.equal(lifecycle.getDccControlledFileStatusLabel('PENDING_APPLICANT_TRAINING_RECORD'), '待文控上传培训记录')
  assert.equal(lifecycle.getDccControlledFileStatusLabel('READY_TO_PUBLISH'), '待受控')
})
function application({ file = baseline, confirm = async () => true, readDraft = async () => clone(draft), save = async () => '21' } = {}) {
  let defaultsReads = 0; const requests = []
  const panel = mount('DetailApplicationPanel', { file: clone(file) }, {
    'element-plus': { ElMessageBox: { confirm } }, '@/utils': { generateUUID: () => 'unique-application' },
    '@/api/system/dept': { getSimpleDeptList: async () => [{ id: 50, name: '质量部' }] },
    '@/api/system/user': { getSimpleUserList: async () => [{ id: 60, nickname: '批准人' }] },
    '@/api/dcc/controlledFile/approvalRoutes': { previewApprovalRoute: async () => [{ stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: [50] }, { stageCode: 'MATRIX_APPROVAL', resolvedUserIds: [60] }] },
    '@/api/dcc/controlledFile/projectAttributes': { getProjectDefaults: async () => { defaultsReads++; return attrs('NMPA') } },
    '@/api/dcc/controlledFile/applicationRead': { getWorkingApplicationAttributes: async id => file.status === 'WORKING' ? readDraft(id) : { ...clone(draft), controlledFileId: String(id), applicationType: 'REVISION', selectedSignoffDepartmentIds: ['50'] }, getControlledFileReplacementAttributes: async id => ({ ...clone(draft), controlledFileId: String(id), applicationType: 'REVISION', selectedSignoffDepartmentIds: ['50'] }), getControlledFileRevisionOptions: async () => clone(options) },
    '@/api/dcc/controlledFile/workflow': { submitControlledFileWorkingIteration: async (id, payload) => { requests.push({ id, payload: clone(payload) }); return save(id, payload) } },
    '../project-attributes/state': attributes, '../revision/revision-model': revision,
    '../workflow/workflow-actions': workflowActions,
    './presentation': { formatControlledFileDateTime: value => value }
  })
  return { ...panel, requests, defaultsReads: () => defaultsReads }
}
test('INITIAL preparation reads original draft actual/default separately and never replaces unknown departments with current matrix', async () => {
  const panel = application({ file: { ...baseline, id: 12, status: 'WORKING' } })
  try {
    await panel.state.open()
    assert.equal(panel.defaultsReads(), 0)
    assert.deepEqual(clone(panel.state.snapshot.actual), attrs('FDA'))
    assert.deepEqual(clone(panel.state.snapshot.defaultSource), attrs('CE'))
    assert.deepEqual(clone(panel.state.selectedDepartments), [])
    await panel.state.submitInitial()
    assert.equal(panel.requests.length, 0)
    assert.match(panel.state.error, /会签部门/)
    panel.state.selectedDepartments = ['50']
    await panel.state.submitInitial()
    assert.equal(panel.requests[0].id, 12)
    assert.equal(panel.requests[0].payload.revisionChangeType, 'INITIAL')
    assert.equal(panel.requests[0].payload.controlledBaselineId, undefined)
    assert.deepEqual(panel.requests[0].payload.projectAttributes, attrs('FDA'))
  } finally { panel.app.unmount() }
})
test('formal revision submits an explicitly selected earlier iteration and the actual replacement intent', async () => {
  const panel = application()
  try {
    await panel.state.open()
    await panel.state.selectIteration('12')
    await panel.state.selectIntent('REPLACEMENT')
    panel.state.effectiveDate = '2026-12-01'
    panel.state.updateActual(attrs('FDA'))
    const command = revision.buildRevisionCommand('10', '12', 'REPLACEMENT', '正文已复核', 'unique-application', panel.state.applicationFacts)
    await panel.state.submitRevision(command)
    assert.equal(panel.requests[0].id, '12')
    assert.equal(panel.requests[0].payload.revisionChangeType, 'REPLACEMENT')
    assert.equal(panel.requests[0].payload.effectiveDate, '2026-12-01')
    assert.deepEqual(panel.requests[0].payload.projectAttributes, attrs('FDA'))
    assert.deepEqual(panel.events, ['21'])
  } finally { panel.app.unmount() }
})
test('editing after confirmation opened invalidates the pending formal command', async () => {
  const confirmation = deferred(), panel = application({ confirm: () => confirmation.promise })
  try {
    await panel.state.open(); await panel.state.selectIteration('12'); panel.state.effectiveDate = '2026-12-01'
    const command = revision.buildRevisionCommand('10', '12', 'PARTIAL', '修订', 'key', panel.state.applicationFacts)
    const submit = panel.state.submitRevision(command)
    panel.state.needTraining = true
    confirmation.resolve(true)
    await assert.rejects(submit, /重新确认/)
    assert.equal(panel.requests.length, 0)
  } finally { panel.app.unmount() }
})
test('cancelled INITIAL confirmation retains edited values without sending a request', async () => {
  const panel = application({ file: { ...baseline, id: 12, status: 'WORKING' }, confirm: async () => { throw 'cancel' } })
  try {
    await panel.state.open(); panel.state.selectedDepartments = ['50']; panel.state.description = '保持此说明'
    await panel.state.submitInitial()
    assert.equal(panel.requests.length, 0)
    assert.equal(panel.state.description, '保持此说明')
    assert.deepEqual(clone(panel.state.snapshot.actual), attrs('FDA'))
  } finally { panel.app.unmount() }
})
test('late preparation for the previous file cannot publish attributes into the new file', async () => {
  const pending = deferred(), panel = application({ file: { ...baseline, id: 12, status: 'WORKING' }, readDraft: () => pending.promise })
  try {
    const opening = panel.state.open()
    panel.props.file = { ...baseline, id: 99, status: 'WORKING' }
    await vue.nextTick(); pending.resolve(clone(draft)); await opening
    assert.equal(panel.state.snapshot, undefined)
    assert.equal(panel.state.opened, false)
    assert.equal(panel.requests.length, 0)
  } finally { panel.app.unmount() }
})
test('new detail Vue components compile including real bound table cell expressions', () => {
  for (const name of ['DetailApplicationPanel', 'DetailObsoleteApplication', 'DetailSignoffAssignment', 'DetailRelationArrangements']) {
    const file = `src/views/dcc/controlled-file/detail/${name}.vue`
    const { descriptor } = parse(read(file)); const script = compileScript(descriptor, { id: name })
    const result = compileTemplate({ source: descriptor.template.content, filename: file, id: name, compilerOptions: { bindingMetadata: script.bindings } })
    assert.deepEqual(result.errors, [], name)
  }
})
test('invalid submit receipt never reports completion or accepts a default file identity', async () => {
  const panel = application({ save: async () => false })
  try {
    await panel.state.open(); await panel.state.selectIteration('12'); panel.state.effectiveDate = '2026-12-01'
    const command = revision.buildRevisionCommand('10', '12', 'PARTIAL', '修订', 'key', panel.state.applicationFacts)
    await assert.rejects(panel.state.submitRevision(command), /正式文件身份/)
    assert.deepEqual(panel.events, [])
  } finally { panel.app.unmount() }
})
test('working date responses normalize a real Java date array and reject invalid dates', async () => {
  let response = { ...clone(draft), effectiveDate: [2026, 2, 28] }
  const api = moduleFrom(read('src/api/dcc/controlledFile/applicationRead.ts'), id => {
    if (id === '@/config/axios') return { default: { get: async () => response } }
    if (id.endsWith('/state')) return attributes
    throw new Error(id)
  })
  assert.equal((await api.getWorkingApplicationAttributes('12')).effectiveDate, '2026-02-28')
  response = { ...response, effectiveDate: [2026, 2, 30] }
  await assert.rejects(api.getWorkingApplicationAttributes('12'), /日期/)
})
test('ASSIGN submits validated remediation in the same signed command with the authoritative task context', async () => {
  const calls = [], ready = deferred()
  const panel = mount('DetailSignoffAssignment', { fileId: '10', processInstanceId: 'round-2', taskId: 'task-7' }, {
    '@/api/dcc/controlledFile/applicationRead': { getSignoffAssignmentContext: async () => ({ controlledFileId: '10', processInstanceId: 'round-2', taskId: 'task-7', obligationId: 'obligation-8', departmentId: '50', departmentName: '质量部', assigned: false, canAssign: true, assigneeOptions: [{ id: '60', name: '甲' }] }) },
    '@/api/dcc/controlledFile/workflowLifecycle': { assignWorkflowSignoff: async (id, request) => { calls.push({ id, request: clone(request) }); return true } },
    '@/api/dcc/controlledFile/relations': { listHistoricalRelations: async () => [{ masterId: '80', fileName: '关联文件.pdf', versionNo: 'A/1' }], listRelationArrangements: async () => { ready.resolve(); return [] } },
    '@/api/system/user': { getSimpleUserList: async () => [{ id: 60, nickname: '甲' }] },
    '../workflow/workflow-actions': workflowActions
  })
  try {
    await ready.promise
    for (let turn = 0; turn < 6; turn++) await Promise.resolve()
    await vue.nextTick()
    panel.state.arrangementsRef = { validate: () => [{ relatedMasterId: '80', assigneeUserId: '60', dueAt: '2026-12-01 12:00:00' }] }
    await panel.state.saveAssignment({ taskId: 'task-7', assigneeUserId: '60', password: 'unit-only', reason: '指派' })
    assert.equal(calls[0].id, '10')
    assert.equal(calls[0].request.taskId, 'task-7')
    assert.deepEqual(calls[0].request.relationArrangements, [{ relatedMasterId: '80', assigneeUserId: '60', dueAt: '2026-12-01 12:00:00' }])
    panel.props.processInstanceId = 'different-round'
    await vue.nextTick()
    await assert.rejects(panel.state.saveAssignment({ taskId: 'task-7' }), /上下文/)
    assert.equal(calls.length, 1)
  } finally { panel.app.unmount() }
})
