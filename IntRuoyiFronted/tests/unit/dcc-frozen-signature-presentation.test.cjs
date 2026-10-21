const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript'), vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const read = file => fs.readFileSync('src/views/dcc/controlled-file/detail/' + file, 'utf8')
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const helperContext = { exports: {}, Error, String, Number, Set, Array }
vm.runInNewContext(transpile(read('native-approval-progress.ts')), helperContext)
const helpers = helperContext.exports
function declaration(file, name) {
  const ast = ts.createSourceFile(file, parse(read(file)).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const node = ast.statements.find(row => ts.isVariableStatement(row) && row.declarationList.declarations.some(decl => decl.name.text === name))
  assert.ok(node, 'Actual display declaration ' + name)
  return node.getText(ast)
}
const selected = { id: '2054545668044084260', processInstanceId: 'round-A2', versionNo: 'A/2', status: 'ACTIVE' }
const signature = { taskId: 'signed-task', processInstanceId: selected.processInstanceId, versionNo: 'A/2', actionType: 'ASSIGN', meaningCode: 'MATRIX_REVIEW_ASSIGN' }
function actualTraceRole(signed, file = selected) {
  const c = { exports: {}, ...helpers, fileDetail: vue.ref(file), approvalTaskList: vue.ref([]), approvalProgressScope: vue.ref(undefined),
    userNameMap: vue.ref(new Map()), getSignatureActorSummary: () => 'frozen actor', formatSignatureTraceComment: value => value,
    formatControlledFileDateTime: value => value, getSignatureEvidenceStatusLabel: value => value,
    buildSignatureTraceFileEvidenceText: () => '', hasSignatureTraceFileEvidence: () => false }
  vm.runInNewContext(transpile(declaration('index.vue', 'buildSignatureTraceRow') + '\nexports.row=buildSignatureTraceRow'), c)
  return c.exports.row(signed).traceRole
}
test('formal detail trace uses exact frozen signature meaning when live task scope is intentionally absent', () => {
  for (const [meaningCode, actionType, duty] of [
    ['SUPERVISOR_REVIEW_APPROVE', 'APPROVE', '直属主管'], ['MATRIX_REVIEW_ASSIGN', 'ASSIGN', '部门负责人指派'],
    ['MATRIX_REVIEW_APPROVE', 'APPROVE', '会签人'], ['MATRIX_APPROVAL_APPROVE', 'APPROVE', '批准人'],
    ['DOC_CONTROL_REVIEW_APPROVE', 'APPROVE', '文控审核人']
  ]) assert.equal(actualTraceRole({ ...signature, meaningCode, actionType }), duty)
})
test('missing, contradictory or other-round/version frozen facts never invent historical duty', () => {
  for (const patch of [{ meaningCode: undefined }, { processInstanceId: 'old-round' }, { versionNo: 'A/1' },
    { taskId: undefined }, { actionType: 'APPROVE' }, { meaningCode: 'UNKNOWN_ASSIGN' }])
    assert.equal(actualTraceRole({ ...signature, ...patch }), '签名职责未记录')
})
const header = { sourceFileId: selected.id, processInstanceId: selected.processInstanceId, taskId: 'assignment-task',
  signatureId: '984', actorId: '910512', stageCode: 'MATRIX_REVIEW', actionType: 'ASSIGN', departmentId: '910379' }
const node = { controlledFileId: selected.id, bpmRound: selected.processInstanceId, taskId: header.taskId,
  signatureId: header.signatureId, actorId: header.actorId, stageCode: header.stageCode, actionType: 'ASSIGN',
  assignmentTarget: { userId: '910513', obligationDepartmentName: '原义务部门快照' }, actorDeptNameSnapshot: '签名人当时另一部门' }
function obligation(nodes, value = header) {
  const c = { exports: {}, String, Number, props: { nodeRecords: nodes }, factIdentity: value => String(value) }
  vm.runInNewContext(transpile(declaration('DetailRelationStageEvidence.vue', 'obligationDepartmentName') + '\nexports.name=obligationDepartmentName'), c)
  return c.exports.name(value)
}
test('obligation label uses uniquely bound frozen assignment node and never the signer department', () => {
  assert.equal(obligation([node]), '原义务部门快照')
  assert.equal(obligation([], { ...header, departmentName: 'header已冻结原名' }), 'header已冻结原名')
})
test('missing or mismatched obligation snapshot remains unrecorded rather than consulting current people', () => {
  assert.equal(obligation([]), '未记录')
  for (const patch of [{ controlledFileId: 'other' }, { bpmRound: 'other' }, { taskId: 'other' }, { signatureId: '985' },
    { actorId: 'other' }, { stageCode: 'MATRIX_APPROVAL' }, { actionType: 'APPROVE' }, { assignmentTarget: null }])
    assert.equal(obligation([{ ...node, ...patch }]), '未记录')
  assert.equal(obligation([node, node]), '未记录')
})
test('readonly version records expose the selected checkout reason and hide any leftover reason on an unchecked version', () => {
  const descriptor = parse(read('ControlledFileBrowseRecords.vue')).descriptor
  const find = node => {
    if (node.type === 1 && node.tag === 'el-descriptions-item' && node.props.some(prop => prop.type === 6 && prop.name === 'label' && prop.value?.content === '检出原因')) return node
    return (node.children || []).map(find).find(Boolean)
  }
  const node = find(descriptor.template.ast)
  assert.ok(node, 'Readonly exact-version records must visibly expose 检出原因 for UI isolation acceptance')
  for (const [file, expected] of [[{ checkedOutBy: '910510', checkedOutReason: '仅A2本次原因' }, '仅A2本次原因'],
    [{ checkedOutBy: null, checkedOutReason: '不能借用A2原因' }, '未检出']]) {
    const compiled = compileScript(parse('<template>' + node.loc.source + '</template><script setup>const file=__file</script>').descriptor, { id: 'checkout-reason', inlineTemplate: true })
    const c = { exports: {}, require: () => vue, __file: file }
    vm.runInNewContext(transpile(compiled.content), c)
    const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: () => ({}),
      insert: (n, p) => p.children.push(n), remove() {}, setText() {}, setElementText(n, text) { n.text = text }, patchProp() {}, parentNode: () => null, nextSibling: () => null })
    const root = { children: [] }, app = renderer.createApp(c.exports.default)
    app.component('ElDescriptionsItem', { setup: (props, { slots }) => () => vue.h('span', slots.default?.()) })
    try { app.mount(root); const text = n => (n.text || '') + (n.children || []).map(text).join(''); assert.equal(text(root).trim(), expected) }
    finally { app.unmount() }
  }
})
