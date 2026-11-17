const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const { ref } = require('vue')
const file = path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/components/DeviationReceiverSelect.vue')
const source = fs.readFileSync(file, 'utf8')
const ast = ts.createSourceFile('selector.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
function load(context) {
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === 'load'))
  const js = ts.transpileModule(statement.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), `${js}; return load`)(...Object.values(context))
}
function setup() {
  const events = []
  const context = { props: { modelValue: 1 }, candidates: ref([]), loading: ref(false), loaded: ref(false), error: ref(''), generation: 0,
    session: { syncSession: () => ({ key: 'tenant71' }), isCurrent: key => key === 'tenant71' },
    emit: (...event) => events.push(event), getDeviationReceiverCandidates: async () => [{ id: 1, username: 'admin', nickname: '偏差管理员' }] }
  return { context, events }
}
test('dedicated selector uses authoritative candidate endpoint and keeps eligible selection', async () => {
  const { context, events } = setup(); await load(context)()
  assert.equal(context.candidates.value[0].id, 1); assert.equal(context.loaded.value, true); assert.deepEqual(events, [])
  assert.match(source, /:user-options="candidates"/)
  assert.doesNotMatch(source, /getSelectionPage|getSimpleUserList|getUserList/)
})
test('empty candidates remain empty and remove obsolete selection without a user fallback', async () => {
  const { context, events } = setup(); context.getDeviationReceiverCandidates = async () => []
  await load(context)(); assert.deepEqual(context.candidates.value, []); assert.equal(context.loaded.value, true)
  assert.equal(context.error.value, ''); assert.deepEqual(events[0], ['update:modelValue', undefined])
  assert.match(source, /暂无可接收偏差的启用管理员/)
})
test('candidate failure remains visible and clears receiver instead of retaining cached users', async () => {
  const { context, events } = setup(); context.getDeviationReceiverCandidates = async () => { throw new Error('candidate unavailable') }
  await load(context)(); assert.equal(context.error.value, 'candidate unavailable'); assert.equal(context.loaded.value, false)
  assert.deepEqual(context.candidates.value, []); assert.deepEqual(events[0], ['update:modelValue', undefined])
})
test('late response for switched session cannot populate current selector', async () => {
  const { context } = setup(); let finish
  context.getDeviationReceiverCandidates = () => new Promise(resolve => { finish = resolve })
  const pending = load(context)(); context.session.isCurrent = () => false
  finish([{ id: 1 }]); await pending; assert.deepEqual(context.candidates.value, [])
})
test('standalone creation uses the initiator while production leaders keep explicit administrator selection', () => {
  const standalone = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/index.vue'), 'utf8')
  assert.doesNotMatch(standalone, /DeviationReceiverSelect|receiverId|receiverName/)
  const order = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/processpool/components/ActiveOrderDeviationDialog.vue'), 'utf8')
  assert.match(order, /<DeviationReceiverSelect v-model="form\.receiverId"/)
  assert.doesNotMatch(order, /<UserSelectV2[^>]*receiverId/)
  const detail = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/DeviationDetail.vue'), 'utf8')
  assert.match(detail, /<DeviationReviewerSelect v-model="handlingForm\.reviewerAssignments\[node\]" :node="node"/)
})
