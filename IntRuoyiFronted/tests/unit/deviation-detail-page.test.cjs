const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { ref } = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/DeviationDetailPage.vue'), 'utf8')
function context(overrides = {}) {
  const state = { sequence: 0, route: { fullPath: '/detail?deviationId=12&workTaskId=34', query: { deviationId: '12', workTaskId: '34', taskType: 'DEVIATION_CORRECT' } },
    session: { syncSession: () => ({ key: 'tenant1' }), isCurrent: key => key === 'tenant1' },
    deviationId: ref(12), taskNavigationLoading: ref(false), taskNavigationError: ref(''), initialTab: ref(),
    requireExactJavaLong: value => { assert.match(value, /^[1-9]\d*$/); return value },
    getDeviationTaskNavigation: async id => ({ id, taskType: 'DEVIATION_CLOSE', businessScopeId: '12' }),
    resolveDeviationTaskTab: task => { assert.equal(task.taskType, 'DEVIATION_CLOSE'); return 'disposition' } }
  Object.assign(state, overrides)
  const ast = ts.createSourceFile('page.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(statement => ts.isVariableStatement(statement) && statement.declarationList.declarations.some(item => item.name.getText(ast) === 'resolveTaskContext'))
  const js = ts.transpileModule(declaration.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const load = Function(...Object.keys(state), `${js}; return resolveTaskContext`)(...Object.values(state))
  return { state, load }
}
test('deviation detail page selects the formal responsibility instead of query task type', async () => {
  const { state, load } = context()
  await load()
  assert.equal(state.initialTab.value, 'disposition')
  assert.equal(state.taskNavigationError.value, '')
  assert.equal(state.taskNavigationLoading.value, false)
})
test('permission failures and mismatched task context prevent detail entry', async () => {
  for (const failure of ['lookup', 'scope']) {
    const { state, load } = context({
      [failure === 'lookup' ? 'getDeviationTaskNavigation' : 'resolveDeviationTaskTab']: () => { throw Error('无权办理此偏差') }
    })
    await load()
    assert.equal(state.initialTab.value, undefined)
    assert.equal(state.taskNavigationError.value, '无权办理此偏差')
  }
})
test('late task lookup cannot select another route work area', async () => {
  let complete
  const { state, load } = context({ getDeviationTaskNavigation: () => new Promise(resolve => { complete = resolve }) })
  const pending = load()
  state.route.fullPath = '/detail?deviationId=99'
  complete({ id: '34', taskType: 'DEVIATION_CLOSE', businessScopeId: '12' })
  await pending
  assert.equal(state.initialTab.value, undefined)
})
test('deviation detail page script and template compile', () => {
  const { descriptor } = parse(source)
  const script = compileScript(descriptor, { id: 'deviation-page' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: 'DeviationDetailPage.vue', id: 'deviation-page', compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
})
