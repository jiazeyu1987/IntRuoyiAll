const { test } = require('node:test'), assert = require('node:assert/strict'), fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const root = require('node:path').resolve(__dirname, '../..')
const source = fs.readFileSync(root + '/src/views/dcc/controlled-file/detail/application-round-context.ts', 'utf8')
const c = { exports: {}, Error, BigInt }
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
const flow = () => ({ controlledFileId: '20', applicationType: 'REVISION', bpmRound: 'R', attributeRound: 7,
  status: 'READY', unavailableReason: null, executionContract: 'PUBLIC_MATRIX_NATIVE_V2', flowRule: 'PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM',
  processDefinitionKey: 'dcc-controlled-file-revision', processDefinitionId: 'revision:6:exact', deploymentId: 'deployment', definitionDigest: 'a'.repeat(64) })
const input = () => ({ fileId: '20', bpmRound: 'R', primaryBpmRound: 'R', blockedReason: '', flowContract: flow() })
test('confirmation uses exact positive attribute round from the verified V6 flow contract', () => {
  assert.equal(typeof c.exports.requireDelayedConfirmationRound, 'function')
  assert.equal(c.exports.requireDelayedConfirmationRound(input()), 7)
})
test('missing pending historical foreign and duplicate identities cannot supply a guessed round', () => {
  for (const patch of [{ flowContract: undefined }, { primaryBpmRound: null }, { blockedReason: '尚未核验' },
    ...[{ attributeRound: undefined }, { attributeRound: 0 }, { attributeRound: 1.2 }, { controlledFileId: '21' }, { bpmRound: 'old' },
      { status: 'NOT_RECORDED' }, { executionContract: 'PUBLIC_MATRIX_NATIVE_V1' }, { flowRule: 'PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL' }]
      .map(patch => ({ flowContract: { ...flow(), ...patch } }))])
    assert.throws(() => c.exports.requireDelayedConfirmationRound({ ...input(), ...patch }))
})
function parent(read) {
  const page = fs.readFileSync(root + '/src/views/dcc/controlled-file/detail/index.vue', 'utf8')
  const descriptor = require('vue/compiler-sfc').parse(page).descriptor
  const ast = ts.createSourceFile('detail.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const st = ast.statements.find(s => s.declarationList?.declarations.some(d => d.name.text === 'loadDelayedConfirmation'))
  assert.ok(st, 'real parent load handler')
  const host = { exports: {}, Error, String, delayedConfirmationSequence: 0, delayedConfirmationContext: { value: undefined },
    delayedConfirmationError: { value: '' }, isDelayedEffectiveTask: { value: true },
    delayedConfirmationIdentity: { value: 'task-T-round-R' },
    approvalProgressScope: { value: { flowContract: flow() } }, approvalTodoTask: { value: { id: 'T' } },
    fileDetail: { value: { id: '20' } }, approvalProcessInstanceId: { value: 'R' },
    applicationRoundSelection: { value: { primaryBpmRound: 'R', blockedReason: '' } },
    requireDelayedConfirmationRound: c.exports.requireDelayedConfirmationRound, getDelayedEffectiveConfirmation: read }
  vm.runInNewContext(ts.transpileModule(st.getText(ast) + '\nexports.load=loadDelayedConfirmation', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, host)
  return host
}
test('actual parent loads exact task and verified attribute round with no nonexistent scope property', async () => {
  const calls = [], host = parent(async (...args) => { calls.push(args); return { taskId: 'T', attributeRound: 7 } })
  await host.exports.load()
  assert.deepEqual(calls[0], ['20', 'T', 'R', 7]); assert.equal(host.delayedConfirmationContext.value.attributeRound, 7)
  host.approvalProgressScope.value.flowContract = { ...flow(), attributeRound: undefined }
  await host.exports.load(); assert.equal(calls.length, 1); assert.equal(host.delayedConfirmationContext.value, undefined)
  assert.match(host.delayedConfirmationError.value, /轮次/)
})
test('actual parent clears prior task facts and ignores a late response after a new task load', async () => {
  let finish; const host = parent(() => new Promise(resolve => { finish = resolve }))
  const pending = host.exports.load()
  host.isDelayedEffectiveTask.value = false; await host.exports.load()
  finish({ taskId: 'old', attributeRound: 7 }); await pending
  assert.equal(host.delayedConfirmationContext.value, undefined)
})
test('actual parent rejects an old response immediately when route identity changed before its next watcher', async () => {
  let finish; const host = parent(() => new Promise(resolve => { finish = resolve }))
  const pending = host.exports.load()
  host.delayedConfirmationIdentity.value = 'different-task-route'
  finish({ taskId: 'old', attributeRound: 7 }); await pending
  assert.equal(host.delayedConfirmationContext.value, undefined)
})
