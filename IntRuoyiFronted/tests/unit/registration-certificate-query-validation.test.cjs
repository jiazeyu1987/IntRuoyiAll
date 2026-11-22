const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const vue = require('vue')
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const validation = {}
new Function('exports', transpile(fs.readFileSync('src/views/dcc/registration-certificate/shared/queryValidation.ts', 'utf8')))(validation)
const requests = {}
new Function('exports', 'require', transpile(fs.readFileSync('src/hooks/web/useServerListFilter.ts', 'utf8')))(requests, () => vue)
const page = parse(fs.readFileSync('src/views/dcc/registration-certificate/index/index.vue', 'utf8')).descriptor
const ast = ts.createSourceFile('page.ts', page.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declarations = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(item => ['loadPage', 'loadOldIndexPage'].includes(item.name.getText(ast))))
const loadCode = transpile(declarations.map(item => item.getText(ast)).join('\n'))
function setup(api) {
  const messages = [], currentQueryError = vue.ref(''), oldQueryError = vue.ref('')
  const context = {
    ...validation, currentQueryError, oldQueryError, message: { error: value => messages.push(value) },
    queryParams: {}, oldQueryParams: {}, currentRequest: requests.useLatestListRequest(), oldRequest: requests.useLatestListRequest(),
    getRegistrationCertificatePage: api, getRegistrationCertificateOldIndexPage: api,
    list: vue.ref([]), total: vue.ref(0), selectedCertificates: vue.ref([]), oldList: vue.ref([]), oldTotal: vue.ref(0)
  }
  const handlers = new Function(...Object.keys(context), loadCode + ';return {loadPage,loadOldIndexPage}')( ...Object.values(context))
  return { ...context, ...handlers, messages }
}
for (const [start, end] of [['firstObtainedStart','firstObtainedEnd'], ['approvalStart','approvalEnd'], ['effectiveStart','effectiveEnd'], ['expiryStart','expiryEnd'], ['ceIssueStart','ceIssueEnd'], ['listingStart','listingEnd']]) {
  test(`${start} rejects reversed dates before requesting either certificate list`, async () => {
    let calls = 0
    const c = setup(async () => { calls++; return { list: ['new'], total: 1 } })
    for (const [query, load, error] of [[c.queryParams,c.loadPage,c.currentQueryError], [c.oldQueryParams,c.loadOldIndexPage,c.oldQueryError]]) {
      Object.assign(query, { [start]: '2026-11-21', [end]: '2026-11-20' })
      await assert.doesNotReject(load())
      assert.match(error.value, /日期起始不能晚于截止/)
      assert.equal(calls, 0)
      query[end] = '2026-11-21'
      await load()
      assert.equal(error.value, '')
      calls = 0
    }
  })
}
test('invalid query invalidates an earlier in-flight result without replacing rows', async () => {
  let resolve
  const c = setup(() => new Promise(done => { resolve = done }))
  const first = c.loadPage()
  Object.assign(c.queryParams, { expiryStart: '2026-11-21', expiryEnd: '2026-11-20' })
  await c.loadPage()
  resolve({ list: ['obsolete'], total: 1 })
  await first
  assert.deepEqual(c.list.value, [])
  assert.equal(c.currentRequest.loading.value, false)
})
test('valid query network failures remain failures rather than empty success pages', async () => {
  const c = setup(async () => { throw new Error('服务不可用') })
  await assert.rejects(c.loadPage(), /服务不可用/)
  assert.equal(c.total.value, 0)
})
