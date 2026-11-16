const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const descriptor = parse(fs.readFileSync(path.join(root, 'src/views/Profile/Index.vue'), 'utf8')).descriptor
const ast = ts.createSourceFile('notify.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === 'canReadActiveOrderHandoffs'))
assert.ok(declaration)
const find = node => node.tag === 'el-tab-pane' && node.props.some(p => p.name === 'name' && p.value?.content === 'handoffs') ? node : (node.children || []).map(find).find(Boolean)
const element = find(descriptor.template.ast)
assert.ok(element, '个人中心必须提供独立交接页签')
const template = compileTemplate({ source: element.loc.source, filename: 'handoff-entry.vue', id: 'handoff-entry', compilerOptions: { mode: 'function', prefixIdentifiers: false, cacheHandlers: false } })
assert.deepEqual(template.errors, [])
const render = Function('Vue', template.code)({ ...vue, resolveComponent: () => 'handoff-panel' })
function entry(permissions, hasRows = true) {
  const code = ts.transpileModule(declaration.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const enabled = Function('computed', 'hasPermission', code + '; return canReadActiveOrderHandoffs;')(vue.computed, values => values.some(value => permissions.includes(value)))
  return render({ hasHandoffs: enabled.value && hasRows, handoffTasks: [], handleHandoffsLoaded() {} }, [])
}
test('ordinary production, leaders, QA and release query roles reach the actual handoff panel', () => {
  for (const permission of ['mes:pro-feedback:query', 'mes:pro-process-pool-team-leader:query',
    'mes:pro-edhr-nonconformance-review:query', 'mes:pro-edhr-work-task:query']) {
    assert.equal(entry([permission]).type, 'handoff-panel')
    assert.equal(entry([permission]).props.label, '交接列表')
  }
})

test('empty handoffs hide the separate tab and notification list contains no handoff panel', () => {
  assert.equal(entry(['mes:pro-feedback:query'], false).type, vue.Comment)
  const notify = fs.readFileSync(path.join(root, 'src/views/system/notify/my/components/MyNotifyMessageList.vue'), 'utf8')
  assert.ok(!notify.includes('ActiveOrderHandoffPanel'))
})

const stateNames = new Set(['handoffTasks', 'handoffLoadError', 'canReadActiveOrderHandoffs', 'hasHandoffs', 'handleHandoffsLoaded', 'handoffLoadSequence', 'refreshHandoffs'])
const stateSource = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => stateNames.has(d.name.getText(ast)))).map(s => s.getText(ast)).join('\n')
const stateJs = ts.transpileModule(stateSource, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
function state(api, permitted = true) {
  const activeName = vue.ref('notifyMessage')
  return { activeName, ...Function('ref', 'computed', 'hasPermission', 'HandoffApi', 'activeName', stateJs + '; return { handoffTasks, handoffLoadError, hasHandoffs, handleHandoffsLoaded, refreshHandoffs };')(vue.ref, vue.computed, () => permitted, api, activeName) }
}

test('real page state loads rows, hides empty refresh and selects the message tab', async () => {
  let rows = [{ id: '1900000000000000001' }]
  const s = state({ myHandoffs: async () => rows })
  assert.equal(s.hasHandoffs.value, false)
  await s.refreshHandoffs()
  assert.equal(s.hasHandoffs.value, true)
  s.activeName.value = 'handoffs'
  rows = []
  await s.refreshHandoffs()
  assert.equal(s.hasHandoffs.value, false)
  assert.equal(s.activeName.value, 'notifyMessage')
})

test('query errors are exposed and retry restores the handoff entry', async () => {
  let failing = true
  const s = state({ myHandoffs: async () => {
    if (failing) throw new Error('交接查询失败')
    return [{ id: '1' }]
  } })
  await s.refreshHandoffs()
  assert.equal(s.handoffLoadError.value, '交接查询失败')
  assert.equal(s.hasHandoffs.value, false)
  failing = false
  await s.refreshHandoffs()
  assert.equal(s.handoffLoadError.value, '')
  assert.equal(s.hasHandoffs.value, true)
})

test('no permission prevents requests; stale responses cannot restore old rows', async () => {
  const denied = state({ myHandoffs: () => assert.fail('must not query without permission') }, false)
  await denied.refreshHandoffs()
  assert.equal(denied.hasHandoffs.value, false)
  let resolveOld, calls = 0
  const s = state({ myHandoffs: () => ++calls === 1 ? new Promise(resolve => { resolveOld = resolve }) : Promise.resolve([]) })
  const old = s.refreshHandoffs()
  await s.refreshHandoffs()
  resolveOld([{ id: 'old' }])
  await old
  assert.equal(s.hasHandoffs.value, false)
})
test('unrelated notification users and mutation-only permissions cannot mount the handoff panel', () => {
  for (const permissions of [[], ['system:notify-message:query'], ['mes:pro-feedback:create'], ['mes:pro-edhr-work-task:update']]) {
    assert.equal(entry(permissions).type, vue.Comment)
  }
})
