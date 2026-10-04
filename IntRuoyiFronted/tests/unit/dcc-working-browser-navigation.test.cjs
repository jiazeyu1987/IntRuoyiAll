const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const FILE = '9223372036854775701', MASTER = '9223372036854775700'
const detail = fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8')
const browser = fs.readFileSync('src/views/dcc/controlled-file/browser/index.vue', 'utf8')
function loadHelper() {
  const source = fs.readFileSync('src/views/dcc/controlled-file/shared/working-browser-navigation.ts', 'utf8')
  const state = { exports: {}, require: id => { throw new Error(id) }, Error, Number, String, BigInt }
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, state)
  return state.exports
}
function declaration(source, name, context) {
  const ast = ts.createSourceFile('page.ts', source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  let selected
  for (const statement of ast.statements) {
    if (ts.isVariableStatement(statement) && statement.declarationList.declarations.some(d => d.name.getText(ast) === name)) selected = statement.getText(ast)
  }
  assert.ok(selected, 'actual public declaration missing: ' + name)
  const compiled = ts.transpileModule(selected + '\nexports.value=' + name, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const state = { exports: {}, Error, String, Number, ...context }
  vm.runInNewContext(compiled, state)
  return state.exports.value
}
const file = (patch = {}) => ({ id: FILE, masterId: MASTER, directoryId: 7, fileNumber: 'TASK-NUMBER', title: 'task.docx', status: 'PENDING_APPLICANT_REWORK', ...patch })
test('working navigation preserves the actual selected file/Master and formal storage directory', () => {
  const helper = loadHelper(), route = helper.buildWorkingBrowserRoute(file())
  assert.equal(route.name, 'DccControlledFileBrowser')
  assert.equal(route.query.browserMode, 'storage')
  assert.equal(route.query.workingFileId, FILE)
  assert.equal(route.query.workingMasterId, MASTER)
  assert.equal(route.query.directoryId, '7')
  assert.equal(route.query.keyword, 'TASK-NUMBER')
  assert.equal(route.query.status, undefined, 'status filter must not hide original formal row/history')
})
test('unsafe, foreign or incomplete navigation identities fail before route construction', () => {
  const helper = loadHelper()
  for (const patch of [{ id: 9007199254740992 }, { id: '0' }, { masterId: undefined }, { directoryId: 9007199254740992 }, { directoryId: null }, { fileNumber: ' ' }]) {
    assert.throws(() => helper.buildWorkingBrowserRoute(file(patch)))
  }
})
test('returned-applicant public handler routes to the original exact version in the storage operation page', () => {
  const routes = [], errors = []
  const context = {
    fileDetail: { value: file() }, router: { push: route => routes.push(route) },
    controlledFileId: { value: FILE }, isWorkingBrowserDetailCurrent: { value: true },
    message: { error: e => errors.push(e) }, buildWorkingBrowserRoute: loadHelper().buildWorkingBrowserRoute
  }
  const handler = declaration(detail, 'openReturnedApplicantReworkInBrowser', {
    openWorkingFileInBrowser: declaration(detail, 'openWorkingFileInBrowser', context)
  })
  handler()
  assert.equal(routes.length, 1)
  assert.equal(routes[0].query.workingFileId, FILE)
  assert.equal(routes[0].query.browserMode, 'storage')
  assert.equal(routes[0].query.directoryId, '7')
  assert.equal(errors.length, 0)
})
test('storage initialization accepts the explicit operation mode, without requiring an invented directory', () => {
  const value = declaration(browser, 'browserMode', { ref: value => ({ value }), route: { query: { browserMode: 'storage' } } })
  assert.equal(value.value, 'storage')
})
test('actual initial selector preserves requested older WORKING rather than defaulting to ACTIVE', () => {
  const options = [{ id: '41', status: 'ACTIVE', versionNo: 'A/1' }, { id: FILE, status: 'WORKING', versionNo: 'A/1-2' }]
  const resolve = declaration(browser, 'resolveInitialSelectedVersionId', {
    route: { query: { workingFileId: FILE, workingMasterId: MASTER } },
    getVersionOptions: () => options, resolveWorkingBrowserSelection: (row, options, query) => {
      assert.equal(String(row.masterId), query.workingMasterId); return options.find(o => o.id === query.workingFileId)?.id
    }
  })
  assert.equal(resolve({ id: '41', masterId: MASTER, currentActiveVersionNo: 'A/1' }), FILE)
})
test('requested file and Master must match the real authorized option together', () => {
  const helper = loadHelper(), options = [{ id: FILE, status: 'WORKING' }]
  assert.equal(helper.resolveWorkingBrowserSelection({ masterId: MASTER }, options, { workingFileId: FILE, workingMasterId: MASTER }), FILE)
  assert.throws(() => helper.resolveWorkingBrowserSelection({ masterId: '2' }, options, { workingFileId: FILE, workingMasterId: MASTER }))
  assert.equal(helper.resolveWorkingBrowserSelection({ masterId: MASTER }, [{ id: '3' }], { workingFileId: FILE, workingMasterId: MASTER }), undefined)
  assert.throws(() => helper.resolveWorkingBrowserSelection({ masterId: MASTER }, options, { workingFileId: FILE }))
})
test('normal management detail provides a real checkout/checkin navigation button and captured file route', () => {
  const { parse } = require('vue/compiler-sfc')
  const template = parse(detail).descriptor.template.content
  assert.ok(template.includes('data-testid="dcc-detail-working-browser"'), 'ordinary management detail must expose checkout/checkin entry')
  const calls = [], errors = []
  const handler = declaration(detail, 'openWorkingFileInBrowser', {
    fileDetail: { value: file() }, router: { push: r => calls.push(r) },
    controlledFileId: { value: FILE }, isWorkingBrowserDetailCurrent: { value: true },
    buildWorkingBrowserRoute: loadHelper().buildWorkingBrowserRoute, message: { error: m => errors.push(m) }
  })
  handler()
  assert.equal(calls[0].query.workingFileId, FILE)
  assert.equal(errors.length, 0)
})
test('kept-alive browser changes from project to storage mode when a selected-file route arrives', () => {
  const { parse } = require('vue/compiler-sfc')
  const script = parse(browser).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const statement = ast.statements.find(s => ts.isExpressionStatement(s) && ts.isCallExpression(s.expression)
    && s.expression.expression.getText(ast) === 'watch' && s.expression.arguments[0]?.getText(ast).includes('route.query.browserMode'))
  assert.ok(statement, 'query operation-mode watcher must exist')
  const callbacks = [], state = { browserMode: { value: 'project' }, route: { query: { browserMode: 'storage', directoryId: '7' } }, watch: (_read, callback) => callbacks.push(callback) }
  vm.runInNewContext(ts.transpileModule(statement.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, state)
  callbacks[0]()
  assert.equal(state.browserMode.value, 'storage')
})
test('browser route synchronization retains the selected version until explicit navigation leaves it', () => {
  const build = declaration(browser, 'buildBrowserRouteQueryFromRememberedState', {
    route: { query: { browserMode: 'storage', workingFileId: FILE, workingMasterId: MASTER } },
    BROWSER_SEARCH_SCOPE_CURRENT: 'current', resolveBrowserPageSize: () => 20, normalizeKeyword: s => s,
    workingBrowserIdentityQuery: loadHelper().workingBrowserIdentityQuery
  })
  const query = build({ directoryId: 7, keyword: 'TASK-NUMBER' })
  assert.equal(query.workingFileId, FILE)
  assert.equal(query.workingMasterId, MASTER)
})
test('detail navigation refuses stale old projection or a current file whose new read has not succeeded', () => {
  for (const context of [{ id: '31', ready: true }, { id: FILE, ready: false }]) {
    const calls = [], errors = []
    const handler = declaration(detail, 'openWorkingFileInBrowser', {
      fileDetail: { value: file() }, controlledFileId: { value: context.id },
      isWorkingBrowserDetailCurrent: { value: context.ready },
      router: { push: r => calls.push(r) }, buildWorkingBrowserRoute: loadHelper().buildWorkingBrowserRoute,
      message: { error: e => errors.push(e) }
    })
    handler()
    assert.equal(calls.length, 0, 'stale projection must never navigate as the current detail')
    assert.equal(errors.length, 1)
  }
})
test('same HTTP parameters do not allow an old list response to select the new route target', async () => {
  let release
  const data = { list: [{ id: '41', masterId: MASTER, versionHistory: [{ id: FILE }, { id: '31' }] }], total: 1 }
  const route = { path: '/dcc/controlled-file/browser', fullPath: '/dcc/controlled-file/browser?workingFileId='+FILE,
    query: { workingFileId: FILE, workingMasterId: MASTER } }
  const state = { route, browserMode: { value: 'storage' }, listRequestSequence: 0,
    buildBrowserRouteStateKey: () => JSON.stringify(route.query), buildBrowserRequestParams: () => ({ directoryId: 7, keyword: 'TASK-NUMBER' }),
    getBrowserCacheContext: () => 'tenant1:actor1', workingBrowserIdentityQuery: loadHelper().workingBrowserIdentityQuery,
    browserListErrorMessage: { value: '' }, list: { value: [] }, total: { value: 0 }, loading: { value: false },
    isCurrentDirectorySearch: { value: true }, selectedDirectoryId: { value: 7 }, marks: [],
    getControlledFileBrowserPage: () => new Promise(resolve => { release = resolve }),
    resolveInitialSelectedVersionId: (_row, query = route.query) => query.workingFileId,
    markBrowserListLoadedForState: value => state.marks.push(value), clearBrowserLoadedListState: () => {},
    resolveControlledFileReadErrorMessage: e => e.message, console: { warn() {} }, JSON }
  const handler = declaration(browser, 'getList', state)
  const pending = handler()
  route.query = { workingFileId: '31', workingMasterId: MASTER }
  route.fullPath = '/dcc/controlled-file/browser?workingFileId=31'
  release(data); await pending
  assert.equal(state.list.value.length, 0, 'old response cannot populate the new version context')
  assert.equal(state.marks.length, 0)
})
