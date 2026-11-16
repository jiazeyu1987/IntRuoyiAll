const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const page = fs.readFileSync(path.join(__dirname, '../edhr-batch/BatchExecutionActiveOrderDetailPage.vue'), 'utf8')
function declarations(source, names, deps) {
  const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)?.[1] || source
  const ast = ts.createSourceFile('source.ts', script, ts.ScriptTarget.Latest, true)
  const statements = ast.statements.filter(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.every(d => names.includes(d.name.getText(ast))))
  const code = ts.transpileModule(statements.map(s => s.getText(ast)).join('\n') +
    `\nreturn {${names.join(',')}}`, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
  return new Function(...Object.keys(deps), code)(...Object.values(deps))
}
test('parent audit identity exactly matches detail selection including active-order priority', () => {
  const names = ['batchExecutionId', 'parseBatchExecutionId', 'parseActiveOrderId', 'resolveDetailQuery', 'auditDetailQuery']
  for (const query of [{ batchExecutionId: '9223372036854775806' },
    { activeOrderId: '9223372036854775805' }, { activeOrderId: '77', batchExecutionId: '88' }]) {
    const state = declarations(page, names, { route: { query }, computed: vue.computed,
      detail: vue.ref({ activeOrderId: 77 }), loading: vue.ref(false), error: vue.ref('') })
    assert.deepEqual(state.auditDetailQuery.value, state.resolveDetailQuery())
    assert.equal(Object.keys(state.auditDetailQuery.value).length, 1)
  }
  assert.match(page, /audit-scope-type="BATCH"/)
  assert.match(page, /:audit-scope-id="auditDetailQuery"/)
})
test('unloaded or failed parent never evaluates invalid route into a render exception', () => {
  const names = ['batchExecutionId', 'parseBatchExecutionId', 'parseActiveOrderId', 'resolveDetailQuery', 'auditDetailQuery']
  for (const state of [{ detail: undefined, loading: false, error: '' },
    { detail: { activeOrderId: 77 }, loading: true, error: '' },
    { detail: { activeOrderId: 77 }, loading: false, error: '详情加载失败' }]) {
    const h = declarations(page, names, { route: { query: {} }, computed: vue.computed,
      detail: vue.ref(state.detail), loading: vue.ref(state.loading), error: vue.ref(state.error) })
    assert.equal(h.auditDetailQuery.value, undefined)
  }
})
test('audit API preserves explicit BATCH query for page and event', async () => {
  const file = path.resolve(__dirname, '../../../../api/mes/pro/edhr/activeOrderAudit.ts')
  const source = fs.readFileSync(file, 'utf8').replace(/^import [^\r\n]*\r?\n/, '').replace(/export /g, '')
  const code = ts.transpileModule(source + '\nreturn {getActiveOrderGxpAuditPage,getActiveOrderGxpAuditEvent}', {
    compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
  const calls = []
  const api = new Function('request', code)({ get: async config => { calls.push(config) } })
  for (const query of [{ batchExecutionId: '9223372036854775806' }, { activeOrderId: '77' }]) {
    await api.getActiveOrderGxpAuditPage('BATCH', query, { pageNo: 1, pageSize: 50 })
    await api.getActiveOrderGxpAuditEvent('BATCH', query, 12)
    assert.deepEqual(calls.at(-2).params, { pageNo: 1, pageSize: 50, ...query })
    assert.deepEqual(calls.at(-1).params, query)
    assert.match(calls.at(-1).url, /^\/mes\/pro\/edhr-batch-execution\/audit\/get/)
  }
})
