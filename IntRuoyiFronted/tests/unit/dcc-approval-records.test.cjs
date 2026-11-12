const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
function load(name) {
  const source = fs.readFileSync(`src/views/dcc/controlled-file/detail/${name}.ts`, 'utf8')
  const context = { exports: {}, require: name => load(name.replace('./', '')) }
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, context)
  return context.exports
}
const api = load('approval-records')
function evidence() {
  return { controlledFileId: '10', bpmRound: 'round', signatures: [{ id: '71', taskId: 'task', processInstanceId: 'round', actorNicknameSnapshot: '张三', signedAt: '2026-11-11 10:00:00', evidenceStatus: 'VALID' }],
    nodeRecords: [{ recordId: 'SIGNATURE:71', controlledFileId: '10', bpmRound: 'round', taskId: 'task', stageCode: 'MATRIX_REVIEW', actorNicknameSnapshot: '张三', signatureId: '71', signatureEvidenceStatus: 'VALID', actionType: 'ASSIGN', confirmedFields: [{ field: 'signoffAssignee', label: '指派会签人', value: '李四' }, { field: 'fileOwner', label: '确认文件负责人', value: '王五' }] }],
    relationEvidence: { stages: [{ snapshot: { header: { id: '1', signatureId: '71', taskId: 'task', reason: '补全文件' }, members: [{ assigneeUserId: '8', dueAt: '2026-11-12 10:00:00' }] }, assigneeNames: { '8': '赵六' }, signatureEvidenceStatus: 'INVALID', signatureIdentityMatches: true }] } }
}
test('one record keeps its real signature, confirmations and independent remediation state', () => {
  const data = evidence(), rows = api.buildApprovalRecords(data)
  assert.equal(rows.length, 1)
  assert.equal(rows[0].signature.actorNicknameSnapshot, '张三')
  assert.equal(rows[0].confirmedFields[0].value, '李四')
  assert.equal(rows[0].confirmedFields[1].value, '王五')
  assert.equal(rows[0].relationStages[0].assigneeNames['8'], '赵六')
  assert.equal(api.approvalSignatureStatus(rows[0].relationStages[0].signatureEvidenceStatus), '校验失败')
})
test('stages from another task are never attached; forged signature bindings fail', () => {
  const data = evidence(); data.relationEvidence.stages[0].snapshot.header.taskId = 'other-task'
  assert.equal(api.buildApprovalRecords(data)[0].relationStages.length, 0)
  data.nodeRecords[0].signatureId = '72'
  assert.throws(() => api.buildApprovalRecords(data))
})
test('empty history stays empty and standalone historical signatures remain visible once', () => {
  const data = evidence(); data.nodeRecords = []
  assert.equal(api.buildApprovalRecords(data).length, 1)
  data.signatures = []; data.relationEvidence = null
  assert.equal(api.buildApprovalRecords(data).length, 0)
})
test('numeric BPM task results and unknown signature states are readable without internal codes', () => {
  assert.equal(api.approvalResultLabel({ actionType: 'TASK_COMPLETED', result: '2' }), '已通过')
  assert.equal(api.approvalResultLabel({ actionType: 'TASK_COMPLETED', result: '7' }), '审批通过中')
  assert.equal(api.approvalSignatureStatus('INTERNAL'), '状态未记录')
})

test('assignment and actual signoff use separate names while other stages keep their historical name', () => {
  for (const actionType of ['ASSIGN', 'ASSIGNED']) {
    assert.equal(api.approvalNodeLabel({ actionType, stageCode: 'MATRIX_REVIEW', nodeName: '会签指派与实际签名' }), '部门负责人签名指派')
  }
  for (const actionType of ['APPROVE', 'REJECT', 'RETURN', 'TASK_COMPLETED']) {
    assert.equal(api.approvalNodeLabel({ actionType, stageCode: 'MATRIX_REVIEW', nodeName: '会签指派与实际签名' }), '实际会签人会签')
  }
  assert.equal(api.approvalNodeLabel({ actionType: 'APPROVE', stageCode: 'DOC_CONTROL_REVIEW', nodeName: '文控标准化审核' }), '文控标准化审核')
})
test('empty records use text and related files are listed in one place without machine tracing', () => {
  const history = fs.readFileSync('src/views/dcc/controlled-file/detail/DetailApplicationHistory.vue', 'utf8')
  const records = fs.readFileSync('src/views/dcc/controlled-file/detail/DetailApprovalRecords.vue', 'utf8')
  const relations = fs.readFileSync('src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue', 'utf8')
  assert.ok(history.includes('<DetailApprovalRecords'))
  assert.ok(records.includes('v-if="!records.length"'))
  assert.ok(records.includes('<el-table v-else'))
  for (const source of [history, relations]) assert.ok(!source.includes('<DetailRelationStageEvidence'))
  for (const label of ['关联来源：', '所选申请：', 'label="冻结编号"']) assert.ok(!relations.includes(label))
  assert.ok(relations.includes('files.length'))
})
test('compiled Vue renders empty text and connects inline confirmations to the exact signed node', async () => {
  const vue = require('vue'), { renderToString } = require('vue/server-renderer')
  const { parse, compileScript } = require('vue/compiler-sfc')
  const filename = 'src/views/dcc/controlled-file/detail/DetailApprovalRecords.vue'
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  const script = compileScript(descriptor, { id: 'approval-records', inlineTemplate: true })
  const context = { exports: {}, require: id => {
    if (id === 'vue') return vue
    if (id === './approval-records') return api
    if (id === './presentation') return { formatControlledFileDateTime: value => value || '未记录' }
    if (id === '../shared/signature-evidence') return { getDccSignatureOpinionLabel: value => value || '未记录' }
    if (id === '../shared/version-display') return { getDccVersionDisplayText: value => value }
    if (id === './DetailRelationStageEvidence.vue') return { __esModule: true, default: {
      inheritAttrs: false,
      props: ['signatureId', 'taskId'],
      setup: props => () => vue.h('stage-evidence', { 'data-signature': props.signatureId, 'data-task': props.taskId })
    } }
    throw new Error(`Unexpected dependency: ${id}`)
  } }
  vm.runInNewContext(ts.transpileModule(script.content, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, context)
  async function render(data) {
    const app = vue.createSSRApp(context.exports.default, { evidence: data })
    app.component('el-table', { props: ['data'], setup(props, { slots }) {
      vue.provide('rows', props.data); return () => vue.h('table', slots.default?.())
    } })
    app.component('el-table-column', { setup(props, { slots }) {
      const rows = vue.inject('rows'); return () => vue.h('td', rows.map(row => slots.default?.({ row })))
    } })
    return renderToString(app)
  }
  const empty = evidence(); empty.nodeRecords = []; empty.signatures = []; empty.relationEvidence = null
  const text = await render(empty)
  assert.match(text, /暂无前序审批或电子签名记录/); assert.doesNotMatch(text, /<table/)
  const html = await render(evidence())
  for (const name of ['张三', '李四', '王五', '有效']) assert.ok(html.includes(name), name)
  assert.match(html, /data-signature="71"/)
  assert.match(html, /data-task="task"/)
  assert.doesNotMatch(html, /账号 #|签名投影|SIGNATURE:71/)
  const assigned = evidence()
  assigned.nodeRecords[0].assignmentTarget = { userId: '90', nickname: '李四', obligationDepartmentName: '生产部' }
  assigned.nodeRecords[0].confirmedFields.push({ field: 'obligationDepartment', label: '会签义务部门', value: '生产部' })
  assert.equal((await render(assigned)).split('会签义务部门：').length - 1, 1)
  assigned.nodeRecords[0].assignmentTarget = null
  assert.equal((await render(assigned)).split('会签义务部门：').length - 1, 1)
  assigned.nodeRecords[0] = { ...assigned.nodeRecords[0], actionType: 'APPROVE', nodeName: '会签指派与实际签名',
    comment: '3', actorDeptNameSnapshot: '实际会签人所在部门', occurredAt: '2026-11-12 17:21:22', signatureEvidenceStatus: 'INVALID', confirmedFields: [] }
  const actual = await render(assigned)
  for (const fact of ['实际会签人会签', '实际会签人所在部门', '已通过', '>3<', '2026-11-12 17:21:22', '校验失败']) assert.ok(actual.includes(fact), fact)
  assert.doesNotMatch(actual, /会签指派与实际签名|被指派会签人：|会签义务部门：/)
  assert.equal(assigned.signatures[0].actorNicknameSnapshot, '张三')
})

test('wrapped primary cells reserve the same height and observers are cleaned up', () => {
  const { parse, compileScript } = require('vue/compiler-sfc')
  const filename = 'src/views/dcc/controlled-file/detail/DetailApprovalRecords.vue'
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  const script = compileScript(descriptor, { id: 'alignment', inlineTemplate: true })
  const heights = [24, 72], cells = heights.map(height => ({ style: {}, firstElementChild: { getBoundingClientRect: () => ({ height }) } }))
  const root = { querySelectorAll: selector => selector === 'tbody tr'
    ? [{ querySelectorAll: () => cells }] : cells.map(cell => cell.firstElementChild) }
  const hooks = {}, observed = []
  let callback, disconnected = 0
  const vue = { ...require('vue'), ref: () => ({ value: root }), onMounted: fn => { hooks.mount = fn },
    onUpdated: fn => { hooks.update = fn }, onBeforeUnmount: fn => { hooks.unmount = fn } }
  const context = { exports: {}, ResizeObserver: class {
    constructor(fn) { callback = fn }
    observe(cell) { observed.push(cell) }
    disconnect() { disconnected++ }
  }, require: id => id === 'vue' ? vue : id === './approval-records' ? api : {} }
  vm.runInNewContext(ts.transpileModule(script.content, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, context)
  context.exports.default.setup({ evidence: evidence() }, { expose() {} })
  hooks.mount()
  assert.equal(observed.length, 2)
  assert.deepEqual(cells.map(cell => cell.style.minHeight), ['72px', '72px'])
  cells[1].firstElementChild.getBoundingClientRect = () => ({ height: 24 })
  callback()
  assert.deepEqual(cells.map(cell => cell.style.minHeight), ['24px', '24px'])
  hooks.update(); hooks.unmount()
  assert.equal(disconnected, 3)
})


test('records have aligned assignment details in person and department columns, with no signature expander', async () => {
  const source = fs.readFileSync('src/views/dcc/controlled-file/detail/DetailApprovalRecords.vue', 'utf8')
  const labels = ['审批环节', '办理人', '所属部门', '处理结果', '审批意见', '处理时间', '电子签名']
  let previous = -1
  for (const label of labels) { const index = source.indexOf(`label="${label}"`); assert.ok(index > previous); previous = index }
  assert.doesNotMatch(source, /type="expand"|<el-tag/)
  assert.ok(source.indexOf('被指派会签人：') < source.indexOf('label="所属部门"'))
  assert.ok(source.indexOf('会签义务部门：') < source.indexOf('label="处理结果"'))
  assert.match(source, /vertical-align: top/)
  const data = evidence()
  data.nodeRecords[0].assignmentTarget = { userId: '90', nickname: '李四', obligationDepartmentName: '生产部' }
  const assigned = api.buildApprovalRecords(data)
  assert.equal(assigned.length, 1)
  assert.equal(api.approvalResultLabel(assigned[0]), '已指派')
  data.nodeRecords.push({ ...data.nodeRecords[0], recordId: 'SIGNATURE:72', signatureId: '72', actionType: 'APPROVE', assignmentTarget: null, confirmedFields: [] })
  data.signatures.push({ ...data.signatures[0], id: '72', actionType: 'APPROVE' })
  assert.equal(api.buildApprovalRecords(data).length, 2)
})
