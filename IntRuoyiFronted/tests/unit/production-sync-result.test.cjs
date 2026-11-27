const assert = require('node:assert/strict')
const test = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref } = require('vue')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/erp/components/SyncByCodeButton.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('sync.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const submit = ast.statements.find(s => s.declarationList?.declarations.some(d => d.name.text === 'submit')).getText(ast)
const compiled = ts.transpileModule(submit + '; return submit', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
function scenario(error, refreshFailure = false, syncType = 'PRODUCTION_ORDER') {
  const state = { syncing: ref(false), code: ref('881MO104106'), resultMessage: ref(''), resultType: ref('success') }
  let calls = 0
  const request = { post: async () => { calls++; if (!refreshFailure) throw error; return { code: '881MO104106' } } }
  const props = { syncType, onSynced: async () => { if (refreshFailure) throw error } }
  const run = new Function('props', 'request', ...Object.keys(state), compiled)(props, request, ...Object.values(state))
  return { state, run, calls: () => calls }
}
test('production sync: timeout, network and HTTP 500 say unknown without automatic retry', async () => {
  for (const error of [Object.assign(new Error('timeout of 180000ms exceeded'), { code: 'ECONNABORTED' }),
    Object.assign(new Error('Network Error'), { code: 'ERR_NETWORK' }), { response: { status: 500 }, message: 'server error' }]) {
    const s = scenario(error); await s.run()
    assert.match(s.state.resultMessage.value, /同步结果未知.*勿重复提交/)
    assert.equal(s.calls(), 1); assert.equal(s.state.syncing.value, false)
  }
})
test('production sync: business rejection and successful sync with refresh failure stay distinct', async () => {
  const rejected = scenario(new Error('ERP中未找到生产订单')); await rejected.run()
  assert.match(rejected.state.resultMessage.value, /^同步失败/)
  const refreshed = scenario(new Error('Network Error'), true); await refreshed.run()
  assert.match(refreshed.state.resultMessage.value, /^ERP同步成功，但列表刷新失败/)
})
test('other sync types retain existing error behavior', async () => {
  const s = scenario(new Error('Network Error'), false, 'PRODUCT'); await s.run()
  assert.match(s.state.resultMessage.value, /^同步失败/)
})
