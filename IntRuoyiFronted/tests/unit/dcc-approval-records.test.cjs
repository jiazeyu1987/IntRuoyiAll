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
    nodeRecords: [{ recordId: 'SIGNATURE:71', controlledFileId: '10', bpmRound: 'round', taskId: 'task', stageCode: 'MATRIX_REVIEW', signatureId: '71', signatureEvidenceStatus: 'VALID', actionType: 'ASSIGN', confirmedFields: [{ field: 'signoffAssignee', label: '指派会签人', value: '李四' }, { field: 'fileOwner', label: '确认文件负责人', value: '王五' }] }],
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
  assert.equal(api.approvalResultLabel({ actionType: 'TASK_COMPLETED', result: '2' }), '审批通过')
  assert.equal(api.approvalResultLabel({ actionType: 'TASK_COMPLETED', result: '7' }), '审批通过中')
  assert.equal(api.approvalSignatureStatus('INTERNAL'), '状态未记录')
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
test('compiled Vue renders empty text and connects expandable confirmations to the exact signed node', async () => {
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
})
