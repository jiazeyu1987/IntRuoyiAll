const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const vue = require('vue')
const base = 'src/views/dcc/controlled-file/detail/'
function load(name) {
  const context = { exports: {}, Error, require: id => load(id.replace('./', '')) }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(`${base}${name}.ts`, 'utf8'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText, context)
  return context.exports
}
const api = load('controlled-detail-approval')
function fixture() {
  const signature = { id: '71', taskId: 'task', processInstanceId: 'round', actorNicknameSnapshot: '张三',
    signedAt: '2026-11-11 10:00:00', evidenceStatus: 'VALID', actionType: 'APPROVE' }
  const node = { recordId: 'SIGNATURE:71', controlledFileId: '10', bpmRound: 'round', taskId: 'task',
    stageCode: 'MATRIX_REVIEW', nodeName: '会签审核', actorNicknameSnapshot: '张三', signatureId: '71',
    signatureEvidenceStatus: 'VALID', actionType: 'APPROVE', result: '已通过', occurredAt: signature.signedAt, confirmedFields: [] }
  return { file: { id: 10, versionNo: 'C/1', requesterId: 1, processInstanceId: 'round', submittedTime: 1,
    signatureSummaries: [{ ...signature, actorId: 2 }], routeSnapshots: [] },
    evidence: { controlledFileId: '10', bpmRound: 'round', signatures: [signature], nodeRecords: [node] } }
}
test('submission and handling stay separate; missing result never inherits signature success', () => {
  const { file, evidence } = fixture()
  const rows = api.buildControlledApprovalRows(file, evidence, 'round', new Map([[1, '提交人']]))
  assert.equal(rows.length, 2); assert.equal(rows[0].actor, '提交人'); assert.equal(rows[0].signature, '—')
  assert.equal(rows[1].result, '已通过'); assert.equal(rows[1].signature, '有效')
  evidence.nodeRecords[0].result = null
  assert.equal(api.buildControlledApprovalRows(file, evidence, 'round')[1].result, '—')
  evidence.nodeRecords[0].signatureEvidenceStatus = 'INVALID'
  assert.equal(api.buildControlledApprovalRows(file, evidence, 'round')[1].signature, '校验失败')
})
test('same actor, time and task never merge assignment, actual review and independent signature', () => {
  const { file, evidence } = fixture()
  evidence.nodeRecords.unshift({ ...evidence.nodeRecords[0], recordId: 'SIGNATURE:70', signatureId: '70', actionType: 'ASSIGN', result: '已指派',
    confirmedFields: [{ field: 'signoffAssignee', value: '张三' }, { field: 'fileOwner', value: '李四' }] })
  evidence.signatures.push({ ...evidence.signatures[0], id: '70', actionType: 'ASSIGN' }, { ...evidence.signatures[0], id: '72', actionType: null })
  const rows = api.buildControlledApprovalRows(file, evidence, 'round')
  assert.equal(rows.length, 4); assert.equal(rows[1].item, '部门负责人签名指派')
  assert.equal(rows[2].item, '实际会签人会签'); assert.equal(rows[3].item, '签名（职责未记录）')
  assert.equal(rows[3].result, '—'); assert.equal(rows[3].time, evidence.signatures[2].signedAt)
  assert.ok(rows[1].details.includes('指派会签人：张三')); assert.ok(!rows[1].details.some(detail => detail.includes('文件负责人')))
  assert.equal(evidence.signatures.length, 3)
})

test('approval is named 批准 and owner confirmation is omitted without changing signed evidence', () => {
  const { file, evidence } = fixture()
  Object.assign(evidence.nodeRecords[0], { stageCode: 'MATRIX_APPROVAL', nodeName: '批准并确认文件负责人',
    confirmedFields: [{ field: 'fileOwner', label: '确认文件负责人', value: '文件编制人' }] })
  const row = api.buildControlledApprovalRows(file, evidence, 'round')[1]
  assert.equal(row.item, '批准'); assert.equal(row.details.length, 0)
  assert.equal(row.signature, '有效'); assert.equal(row.result, '已通过')
  assert.equal(evidence.nodeRecords[0].confirmedFields[0].value, '文件编制人')
})
test('signature enrichment needs exact task and round; forged binding fails', () => {
  const { file, evidence } = fixture()
  file.signatureSummaries[0].taskId = 'other-task'
  assert.equal(api.buildControlledApprovalRows(file, evidence, 'round').length, 3)
  evidence.nodeRecords[0].taskId = 'forged'
  assert.throws(() => api.buildControlledApprovalRows(file, evidence, 'round'), /真实电子签名绑定不一致/)
})
test('standalone signatures keep actual task results and readable accounts', () => {
  const { file, evidence } = fixture(); evidence.nodeRecords = []; evidence.signatures = []
  file.signatureSummaries.push({ ...file.signatureSummaries[0], id: '72', actorNicknameSnapshot: null,
    actorUsernameSnapshot: 'business-account', taskActionResult: 'REJECTED', evidenceStatus: 'UNKNOWN' })
  const rows = api.buildControlledApprovalRows(file, evidence, 'round')
  assert.equal(rows[1].result, '—'); assert.equal(rows[2].result, '已驳回')
  assert.equal(rows[2].actor, 'business-account'); assert.equal(rows[2].signature, '状态未记录')
  assert.equal(api.buildControlledApprovalRows(file, undefined, 'another-round').length, 0)
})
test('rules use real requirements and hide codes and other-round rules', () => {
  const { file } = fixture()
  file.routeSnapshots = [{ stageNo: 2, stageCode: 'MATRIX_REVIEW', stageName: 'MATRIX_REVIEW', approveMethod: 'ALL' },
    { stageNo: 1, stageName: '主管审核', approveMethod: 'ANY', approveRatio: 50 }]
  const rules = api.controlledApprovalRules(file, 'round')
  assert.equal(rules[0].requirement, '通过比例 50%'); assert.equal(rules[1].name, '实际会签人会签')
  assert.equal(rules[1].requirement, '全部通过'); assert.equal(api.controlledApprovalRules(file, 'other-round').length, 0)
})

function historyFixture() {
  const { file, evidence } = fixture()
  Object.assign(file, { masterId: 100, versionNo: 'A/7', revisionChangeType: 'PARTIAL', versionHistory: [{ id: 9, versionNo: 'A/6' }, { id: 8, versionNo: 'B/1' }] })
  const files = { 10: file, 9: { ...file, id: 9, versionNo: 'A/6', processInstanceId: 'initial', signatureSummaries: [] },
    8: { ...file, id: 8, versionNo: 'B/1', processInstanceId: 'replacement', signatureSummaries: [] } }
  const mapping = (id, type, round, change, attr = 1) => ({ controlledFileId: String(id), applicationType: type,
    bpmRound: round, revisionChangeType: change, attributeRound: attr, applicationTime: `2026-11-${10 + attr} 10:00:00` })
  const rounds = { 10: [mapping(10, 'REVISION', 'round', 'PARTIAL'), mapping(10, 'REVISION', 'retry', 'PARTIAL', 2)],
    9: [mapping(9, 'UPLOAD', 'initial')], 8: [mapping(8, 'REVISION', 'replacement', 'REPLACEMENT')] }
  const calls = []
  const readers = {
    detail: async (id, context) => { calls.push({ id, context }); return files[id] },
    rounds: async id => rounds[id],
    evidence: async (id, type, round) => ({ controlledFileId: String(id), versionNo: files[id].versionNo,
      applicationType: type, bpmRound: round, attributeRound: rounds[id].find(r => r.bpmRound === round).attributeRound,
      signatures: [], nodeRecords: [{ ...evidence.nodeRecords[0], recordId: `TASK:${round}`, taskId: `task:${round}`,
        controlledFileId: String(id), bpmRound: round, signatureId: null, signatureEvidenceStatus: null }] })
  }
  return { file, files, readers, calls }
}
test('initial, partial, replacement and repeated same-version applications retain distinct groups and filters clear', async () => {
  const { file, readers, calls } = historyFixture()
  const groups = await api.loadControlledApprovalGroups(file, readers)
  assert.equal(groups.length, 4)
  assert.deepEqual(Array.from(groups, g => g.applicationLabel), ['局部变更', '局部变更', '初始上传', '换版变更'])
  assert.equal(new Set(groups.map(g => g.key)).size, 4)
  assert.ok(groups.every(g => !g.error))
  assert.equal(groups[0].rows.length, 3); assert.equal(groups[1].rows.length, 2)
  assert.equal(groups[2].version, 'A/6')
  assert.equal(calls.find(call => call.id === 9).context.intent, 'VERSION_HISTORY')
  assert.equal(calls.some(call => call.context.intent === 'APPROVAL_HISTORY'), false)
  assert.equal(api.filterControlledApprovalGroups(groups, 'A/7', '').length, 2)
  assert.equal(api.filterControlledApprovalGroups(groups, '', 'UPLOAD').length, 1)
  assert.equal(api.filterControlledApprovalGroups(groups, 'A/6', 'REVISION').length, 0)
  assert.equal(api.filterControlledApprovalGroups(groups, '', '').length, 4)
  assert.equal(api.filterControlledApprovalGroups(groups, '', '换版变更').length, 1)
  assert.equal(api.filterControlledApprovalGroups(groups, '', '局部变更').length, 2)
})
test('association failure retains independently authorized signatures in their own application only', async () => {
  const { file, readers } = historyFixture()
  readers.evidence = async () => { throw new Error('unavailable') }
  const groups = await api.loadControlledApprovalGroups(file, readers)
  assert.ok(groups[0].error)
  assert.equal(groups[0].rows.find(r => r.actor === '张三').signature, '有效')
  assert.equal(groups[1].rows.some(r => r.actor === '张三'), false)
  readers.rounds = async () => { throw new Error('unavailable') }
  const independent = await api.loadControlledApprovalGroups(file, readers)
  assert.ok(independent[0].error)
  assert.equal(independent[0].applicationLabel, '申请信息未记录')
  assert.equal(independent[0].rows.length, 2)
})
test('denied historical versions and foreign logical files never supply records', async () => {
  const { file, files, readers } = historyFixture()
  files[9] = { ...files[9], masterId: 999 }
  const detail = readers.detail
  readers.detail = async (id, context) => { if (Number(id) === 8) throw new Error('denied'); return detail(id, context) }
  const groups = await api.loadControlledApprovalGroups(file, readers)
  assert.equal(groups.filter(g => g.error).length, 2)
  assert.ok(groups.filter(g => g.version !== 'A/7').every(g => g.rows.length === 0))
})
test('wrong-version evidence is rejected without suppressing independent signatures', async () => {
  const { file, readers } = historyFixture()
  const read = readers.evidence
  readers.evidence = async (...args) => ({ ...await read(...args), versionNo: 'WRONG' })
  const groups = await api.loadControlledApprovalGroups(file, readers)
  assert.ok(groups.every(g => g.error)); assert.equal(groups[0].rows.length, 2)
  assert.equal(groups[0].rows[1].result, '—')
})
function compilePanel(overrides = {}) {
  const { parse, compileScript } = require('vue/compiler-sfc')
  const { descriptor, errors } = parse(fs.readFileSync(`${base}ControlledDetailApprovalPanel.vue`, 'utf8'))
  assert.equal(errors.length, 0)
  const script = compileScript(descriptor, { id: 'resume', inlineTemplate: true })
  const context = { exports: {}, Error, require: id => {
    if (overrides[id]) return overrides[id]
    if (id === 'vue') return vue
    if (id === './controlled-detail-approval') return api
    if (id === './presentation') return { formatControlledFileDateTime: v => String(v || '—') }
    if (id === '../shared/version-display') return { getDccVersionDisplayText: v => v }
    if (id === '../shared/signature-evidence') return { getDccSignatureOpinionLabel: v => v || '未记录' }
    return {}
  } }
  vm.runInNewContext(ts.transpileModule(script.content, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, context)
  return context.exports.default
}
test('six columns and inline expansion retain row identity; invalid context hides all rows', () => {
  const refs = []
  const component = compilePanel({ vue: { ...vue, ref(v) { const r = vue.ref(v); refs.push(r); return r },
    watch() {}, onBeforeUnmount() {}, resolveComponent: name => ({ name }), resolveDirective: () => ({}), withDirectives: node => node } })
  const { file } = fixture(), props = vue.reactive({ file, contextError: '' })
  const render = component.setup(props, { expose() {} })
  refs[0].value = [{ key: 'group', version: 'A/7', applicationType: 'REVISION', applicationLabel: '局部变更', error: '',
    rows: [{ key: 'node', item: '指派', details: ['会签人：张三'], actor: '张三', signature: '有效', result: '已指派' }] }]
  let table = render(props, []).children[0].children.find(n => n.type?.name === 'el-table')
  let columns = table.children.default(), rows = table.props.data
  assert.deepEqual(Array.from(columns, n => n.props.label), ['办理事项', '办理人', '记录时间', '处理结果', '审批意见', '电子签名'])
  assert.deepEqual(Array.from(table.props['span-method']({ row: rows[0], columnIndex: 0 })), [1, 6])
  function collect(nodes) { return nodes.flatMap(n => [n, ...(Array.isArray(n?.children) ? collect(n.children) : [])]) }
  assert.equal(rows.length, 1)
  const groupButton = collect(columns[0].children.default({ row: rows[0] })).find(n => n.type === 'button')
  assert.equal(groupButton.props['aria-expanded'], false)
  groupButton.props.onClick()
  table = render(props, []).children[0].children.find(n => n.type?.name === 'el-table')
  columns = table.children.default(); rows = table.props.data
  assert.equal(rows.length, 2)
  assert.equal(collect(columns[0].children.default({ row: rows[0] })).find(n => n.type === 'button').props['aria-expanded'], true)
  assert.equal(table.props['row-class-name']({ row: rows[1] }), '')
  collect(columns[0].children.default({ row: rows[1] })).find(n => n.type === 'button').props.onClick()
  assert.equal(table.props['row-class-name']({ row: rows[1] }), 'approval-expanded-row')
  assert.equal(rows[1].key, 'group:node')
  groupButton.props.onClick()
  assert.equal(render(props, []).children[0].children.find(n => n.type?.name === 'el-table').props.data.length, 1)
  props.contextError = '无效上下文'
  assert.equal(render(props, []).children[0].children.some(n => n.type?.name === 'el-table'), false)
})

test('application groups expand independently even for the same version and retain identity through filtering', () => {
  const refs = []
  const component = compilePanel({ vue: { ...vue, ref(v) { const r = vue.ref(v); refs.push(r); return r },
    watch() {}, onBeforeUnmount() {}, resolveComponent: name => ({ name }), resolveDirective: () => ({}), withDirectives: node => node } })
  const { file } = fixture(), props = vue.reactive({ file, contextError: '' })
  const render = component.setup(props, { expose() {} })
  refs[0].value = ['first', 'second'].map(key => ({ key, version: 'A/7', applicationType: key === 'first' ? 'UPLOAD' : 'REVISION',
    applicationLabel: key === 'first' ? '初始上传' : '局部变更', error: '', rows: [{ key: 'node', item: '审核', details: [] }] }))
  const table = () => render(props, []).children[0].children.find(n => n.type?.name === 'el-table')
  const keys = () => Array.from(table().props.data, row => row.key)
  function collect(nodes) { return nodes.flatMap(n => [n, ...(Array.isArray(n?.children) ? collect(n.children) : [])]) }
  const toggle = key => {
    const current = table()
    collect(current.children.default()[0].children.default({ row: current.props.data.find(row => row.key === key) }))
      .find(n => n.type === 'button').props.onClick()
  }
  assert.deepEqual(keys(), ['first', 'second'])
  toggle('first'); assert.deepEqual(keys(), ['first', 'first:node', 'second'])
  toggle('second'); assert.deepEqual(keys(), ['first', 'first:node', 'second', 'second:node'])
  toggle('first'); assert.deepEqual(keys(), ['first', 'second', 'second:node'])
  refs[4].value = 'UPLOAD'; assert.deepEqual(keys(), ['first'])
  refs[4].value = ''; assert.deepEqual(keys(), ['first', 'second', 'second:node'])
  toggle('second'); assert.deepEqual(keys(), ['first', 'second'])
})
test('context changes discard late responses and clear filters and expansion', async () => {
  let callback, resolveGroups; const refs = []
  const component = compilePanel({ vue: { ...vue, ref(v) { const r = vue.ref(v); refs.push(r); return r },
    watch(source, fn) { callback = fn }, onBeforeUnmount() {} },
    './controlled-detail-approval': { ...api, loadControlledApprovalGroups: () => new Promise(resolve => { resolveGroups = resolve }) } })
  const { file } = fixture(), props = vue.reactive({ file, contextError: '' })
  component.setup(props, { expose() {} })
  const pending = callback()
  refs[3].value = 'A/7'; refs[4].value = 'UPLOAD'; refs[5].value = ['old']; refs[6].value = ['old-group']
  props.contextError = '无效上下文'; await callback()
  resolveGroups([{ key: 'stale' }]); await pending
  assert.equal(refs[0].value.length, 0); assert.equal(refs[2].value, '无效上下文')
  assert.equal(refs[3].value, ''); assert.equal(refs[4].value, ''); assert.equal(refs[5].value.length, 0)
  assert.equal(refs[6].value.length, 0)
})



test('a supplied one-round context does not narrow the separately authorized all-application query', async () => {
  const { file, readers } = historyFixture()
  let received
  const read = readers.rounds
  readers.rounds = async (id, context) => { if (String(id) === String(file.id)) received = context; return read(id, context) }
  const groups = await api.loadControlledApprovalGroups(file, readers, undefined, {
    intent: 'APPROVAL_HISTORY', controlledFileId: '10', expectedVersionNo: 'A/7', applicationType: 'REVISION', bpmRound: 'round'
  })
  assert.equal(received, undefined)
  assert.equal(groups.filter(group => group.version === 'A/7').length, 2)
})

test('authorized approval evidence remains readable when separate APPROVAL_HISTORY body reads are denied', async () => {
  const { file, readers } = historyFixture()
  const detail = readers.detail, evidence = readers.evidence
  let bodyRequests = 0
  const evidenceRequests = []
  readers.detail = async (id, context) => {
    if (context.intent === 'APPROVAL_HISTORY') { bodyRequests++; throw new Error('1080000012') }
    return detail(id, context)
  }
  readers.evidence = async (id, type, round, context) => {
    evidenceRequests.push({ id, type, round, context })
    const result = await evidence(id, type, round)
    result.nodeRecords[0].confirmedFields = [{ field: 'signoffAssignee', label: '指派会签人', value: '李四' }]
    return result
  }
  const groups = await api.loadControlledApprovalGroups(file, readers)
  assert.equal(bodyRequests, 0)
  assert.equal(evidenceRequests.length, 4)
  assert.ok(evidenceRequests.every(request => request.context?.intent !== 'APPROVAL_HISTORY'))
  assert.equal(evidenceRequests.find(request => request.id === 10).context, undefined)
  assert.equal(evidenceRequests.find(request => request.id === 9).context.intent, 'VERSION_HISTORY')
  assert.ok(groups.every(group => !group.error))
  assert.ok(groups.every(group => group.rows.some(row => row.details.includes('指派会签人：李四'))))
})
