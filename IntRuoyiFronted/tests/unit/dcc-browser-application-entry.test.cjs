const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

function handler() {
  const { descriptor } = parse(fs.readFileSync('src/views/dcc/controlled-file/browser/index.vue', 'utf8'))
  const source = ts.createSourceFile('browser.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const declaration = source.statements.find(statement => ts.isVariableStatement(statement)
    && statement.declarationList.declarations.some(item => item.name.getText(source) === 'handleSubmitWorkingIteration'))
  assert.ok(declaration)
  const dependency = name => {
    const statement = source.statements.find(statement => ts.isVariableStatement(statement)
      && statement.declarationList.declarations.some(item => item.name.getText(source) === name))
    assert.ok(statement, `actual ${name} declaration required`)
    return statement.getText(source)
  }
  const calls = { writes: [], destinations: [], successes: [] }
  const env = {
    isValidBrowserOptionId: id => typeof id === 'string' && /^[1-9][0-9]*$/.test(id),
    userStore: { getUser: { id: '99' } },
    openManagement: id => { calls.destinations.push(id) },
    ElMessageBox: { confirm: async () => {} },
    submitApprovalLoadingId: { value: undefined },
    assertWorkingIterationRouteReadiness: async () => {},
    submitControlledFileWorkingIteration: async (...args) => { calls.writes.push(args); return '100' },
    createWorkingIterationSubmitIdempotencyKey: () => 'key',
    deleteBrowserMutationIdempotencyKey() {}, getList: async () => {},
    message: { success: text => calls.successes.push(text), error() {}, warning() {} },
    resolveBrowserErrorMessage: error => String(error), Error
  }
  const code = dependency('canEditVersion') + '\n' + dependency('canSubmitWorkingIteration')
    + '\n' + declaration.getText(source) + '\nglobalThis.invoke=handleSubmitWorkingIteration;'
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  return { invoke: env.invoke, calls }
}
test('browser approval entry opens the exact selected version to complete its application', async () => {
  const state = handler()
  await state.invoke({ id: '1' }, {
    id: '9223372036854775000', versionNo: 'B/1-2', needTraining: false,
    status: 'WORKING', requesterId: '99', checkedOut: false,
    actionProjection: { actionLocked: false }
  })
  assert.deepEqual(state.calls.destinations, ['9223372036854775000'])
  assert.equal(state.calls.writes.length, 0)
  assert.equal(state.calls.successes.length, 0)
})
test('browser approval entry cannot route an unavailable working selection', async () => {
  const state = handler()
  await state.invoke({ id: '1' }, {
    id: '42', versionNo: 'B/1-2', status: 'WORKING', requesterId: '99',
    checkedOut: false, actionProjection: { actionLocked: true }
  })
  assert.equal(state.calls.destinations.length, 0)
  assert.equal(state.calls.writes.length, 0)
})
