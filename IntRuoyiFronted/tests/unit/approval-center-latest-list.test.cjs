const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const source = parse(fs.readFileSync(path.join(__dirname, '../../src/views/approval-center/index.vue'), 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('approval.ts', source, ts.ScriptTarget.Latest, true)
const names = ['approvalListRequestSequence', 'isUnfilteredTodoQuery', 'getList']
const actual = ast.statements.filter(n => ts.isVariableStatement(n) && n.declarationList.declarations.some(d => names.includes(d.name.text))).map(n => n.getText(ast)).join('\n')
function host() {
  const pending = [], badges = [], messages = []
  const c = { list: { value: ['initial'] }, total: { value: 9 }, loading: { value: false }, loadError: { value: '' }, queryParams: { viewType: 'TODO', keyword: '', moduleCode: undefined }, approvalLastLoadedRouteStateKey: undefined,
    buildApprovalCenterRouteStateKey: () => c.queryParams.keyword || 'unfiltered',
    getApprovalTaskPage: query => new Promise((resolve, reject) => pending.push({ resolve, reject, query: { ...query } })),
    approvalTodoBadgeStore: { applyTodoTotal: n => badges.push(n) }, resolveErrorMessage: e => e.message, ElMessage: { error: m => messages.push(m) } }
  vm.createContext(c)
  vm.runInContext(ts.transpileModule(actual + '\nglobalThis.run = getList', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return { c, pending, badges, messages, run: keyword => { c.queryParams.keyword = keyword; return c.run() } }
}
for (const oldFirst of [true, false]) test(`latest list wins; old success finishes ${oldFirst ? 'first' : 'last'}`, async () => {
  const h = host(), old = h.run(''), latest = h.run('target')
  if (oldFirst) {
    h.pending[0].resolve({ list: ['old'], total: 7 }); await old
    assert.equal(h.c.loading.value, true)
    assert.deepEqual(h.c.list.value, ['initial'])
    assert.equal(h.c.approvalLastLoadedRouteStateKey, undefined)
  }
  h.pending[1].resolve({ list: ['target'], total: 1 }); await latest
  if (!oldFirst) { h.pending[0].resolve({ list: ['old'], total: 7 }); await old }
  assert.deepEqual(h.c.list.value, ['target']); assert.equal(h.c.total.value, 1)
  assert.equal(h.c.loadError.value, ''); assert.equal(h.c.loading.value, false)
  assert.equal(h.c.approvalLastLoadedRouteStateKey, 'target'); assert.deepEqual(h.badges, [])
})
for (const oldFirst of [true, false]) test(`stale failure cannot clear current data or end loading (${oldFirst})`, async () => {
  const h = host(), old = h.run(''), latest = h.run('target')
  if (!oldFirst) { h.pending[1].resolve({ list: ['target'], total: 1 }); await latest }
  h.pending[0].reject(new Error('obsolete error')); await old
  assert.deepEqual(h.c.list.value, [oldFirst ? 'initial' : 'target'])
  assert.equal(h.c.loading.value, oldFirst); assert.equal(h.c.loadError.value, ''); assert.deepEqual(h.messages, [])
  if (oldFirst) { h.pending[1].resolve({ list: ['target'], total: 1 }); await latest }
})
test('latest failure stays visible when stale success arrives', async () => {
  const h = host(), old = h.run(''), latest = h.run('target')
  h.pending[1].reject(new Error('current error')); await latest
  h.pending[0].resolve({ list: ['old'], total: 7 }); await old
  assert.equal(h.c.list.value.length, 0); assert.equal(h.c.total.value, 0)
  assert.equal(h.c.loadError.value, 'current error'); assert.deepEqual(h.messages, ['current error'])
  assert.equal(h.c.loading.value, false); assert.equal(h.c.approvalLastLoadedRouteStateKey, undefined); assert.deepEqual(h.badges, [])
})
test('badge eligibility belongs to request snapshot, not later edited inputs', async () => {
  const h = host(), filtered = h.run('target'); h.c.queryParams.keyword = ''
  h.pending[0].resolve({ list: ['target'], total: 1 }); await filtered
  assert.deepEqual(h.badges, [])
  const unfiltered = h.run(''); h.c.queryParams.keyword = 'editing'
  h.pending[1].resolve({ list: ['all'], total: 7 }); await unfiltered
  assert.deepEqual(h.badges, [7])
})
