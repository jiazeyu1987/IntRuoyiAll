const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const panel = fs.readFileSync(path.join(__dirname, 'components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
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
function harness(props) {
  const calls = []
  const names = ['auditScopeTypeValue', 'auditScopeIdValue', 'gxpAuditScopeIsValid',
    'gxpAuditRequestId', 'gxpAuditLoading', 'gxpAuditError', 'gxpAuditEvents',
    'gxpAuditPageNo', 'gxpAuditPageSize', 'gxpAuditTotal', 'loadGxpAudit']
  return { calls, ...declarations(panel, names, { props, ref: vue.ref, computed: vue.computed,
    recordScope: vue.ref(props.recordScope || 'DETAIL_RECORD'), showSummaryTab: vue.ref(!props.embedded),
    getActiveOrderGxpAuditPage: async (...args) => { calls.push(args); return { list: [], total: 0 } } }) }
}
test('BATCH never uses detail.activeOrderId as batchExecutionId and reports missing identity', async () => {
  const h = harness({ recordScope: 'FORMAL_BATCH_SOURCE_DETAIL', detail: { activeOrderId: 77 } })
  await h.loadGxpAudit()
  assert.equal(h.calls.length, 0)
  assert.match(h.gxpAuditError.value, /身份/)
})
for (const query of [{ batchExecutionId: '9223372036854775806' }, { activeOrderId: '9223372036854775805' }]) {
  test(`BATCH passes explicit query ${JSON.stringify(query)} unchanged`, async () => {
    const h = harness({ auditScopeType: 'BATCH', auditScopeId: query, detail: { activeOrderId: 77 } })
    await h.loadGxpAudit()
    assert.deepEqual(h.calls[0]?.slice(0, 2), ['BATCH', query])
  })
}
for (const query of [77, {}, { activeOrderId: '7', batchExecutionId: '8' }, { batchExecutionId: 0 }]) {
  test(`invalid BATCH identity ${JSON.stringify(query)} sends no request`, async () => {
    const h = harness({ auditScopeType: 'BATCH', auditScopeId: query })
    await h.loadGxpAudit()
    assert.equal(h.calls.length, 0)
    assert.match(h.gxpAuditError.value, /身份/)
  })
}
test('TEAM and PQC retain formal identities; embedded does not query', async () => {
  for (const [props, expected] of [[{ detail: { activeOrderId: '77' } }, ['TEAM', '77']],
    [{ pqcReleaseApplicationId: '88', detail: { activeOrderId: '77' } }, ['PQC', '88']]]) {
    const h = harness(props); await h.loadGxpAudit(); assert.deepEqual(h.calls[0].slice(0, 2), expected)
  }
  const h = harness({ embedded: true, recordScope: 'FORMAL_BATCH_SOURCE_DETAIL', detail: { activeOrderId: 77 } })
  await h.loadGxpAudit(); assert.equal(h.calls.length, 0); assert.equal(h.gxpAuditError.value, '')
})
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
  const source = fs.readFileSync(file, 'utf8').replace(/^import .*\n/, '').replace(/export /g, '')
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
