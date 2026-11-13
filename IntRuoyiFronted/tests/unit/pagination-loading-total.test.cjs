const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')

function loadFunction(file, name, context) {
  const source = parse(fs.readFileSync(path.join(root, file), 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('page.ts', source, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  assert.ok(declaration)
  const code = ts.transpileModule(declaration.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), 'let listRequestId = 0, generation = 0, listRequestSequence = 0;\n' + code + `; return ${name};`)(...Object.values(context))
}

function setup(kind, page) {
  let complete, reject
  const requests = []
  const query = vue.reactive({ pageNo: page, pageSize: 10, subjectId: '' })
  const request = params => {
    requests.push(params)
    return new Promise((yes, no) => { complete = yes; reject = no })
  }
  const context = {
    total: vue.ref(83), list: vue.ref([]), rows: vue.ref([]), loading: vue.ref(false),
    error: vue.ref(''), query, queryParams: query,
    clearRoleSelection() {}, RoleApi: { getRolePage: request }, getGxpAuditEventPage: request,
    visible: vue.ref(true), context: () => JSON.stringify(query), view: vue.ref('done'),
    getEdhrWorkTaskDonePage: request, getEdhrWorkTaskCandidateTodoPage: request, getEdhrWorkTaskMyPage: request,
    buildBrowserRouteStateKey: () => String(query.pageNo),
    buildBrowserRequestParams: () => ({ ...query }),
    browserMode: vue.ref('storage'), route: { fullPath: '/dcc/controlled-file/browser', query: {} },
    getBrowserCacheContext: () => 'tenant1:user1', browserListErrorMessage: vue.ref(''),
    isCurrentDirectorySearch: vue.ref(true), selectedDirectoryId: vue.ref('1'),
    markBrowserListLoadedForState() {}, clearBrowserLoadedListState() {},
    getControlledFileBrowserPage: request, resolveInitialSelectedVersionId: item => item.id,
    resolveControlledFileReadErrorMessage: error => error.message
  }
  const files = {
    role: ['src/views/system/role/index.vue', 'getList'],
    audit: ['src/views/system/role/PermissionAuditDialog.vue', 'loadRows'],
    records: ['src/views/Profile/components/ProductionTaskRecords.vue', 'load'],
    browser: ['src/views/dcc/controlled-file/browser/index.vue', 'getList']
  }
  const [file, name] = files[kind]
  const load = loadFunction(file, name, context)
  return { ...context, load, requests, resolve: value => complete(value), reject: error => reject(error) }
}

for (const kind of ['role', 'audit', 'records', 'browser']) {
  test(`${kind}: 第2页、中间页、末页加载期间保留总数，返回目标页数据`, async () => {
    for (const page of [2, 5, 9]) {
      const state = setup(kind, page)
      const pending = state.load()
      await vue.nextTick()
      assert.equal(state.total.value, 83)
      assert.equal(state.loading.value, true)
      assert.equal(state.query.pageNo, page)
      assert.equal(state.requests[0].pageNo, page)
      state.resolve({ list: [{ id: String(page) }], total: 83 })
      await pending
      assert.equal(state.query.pageNo, page)
      const rows = ['role', 'browser'].includes(kind) ? state.list.value : state.rows.value
      assert.equal(rows[0].id, String(page))
      assert.equal(state.loading.value, false)
    }
  })

  test(`${kind}: 当前请求失败仍清空总数并保留原错误`, async () => {
    const state = setup(kind, 2)
    const pending = state.load()
    const error = new Error('original request failed')
    state.reject(error)
    if (kind === 'records') {
      await pending
      assert.equal(state.error.value, error.message)
    } else await assert.rejects(pending, error)
    assert.equal(state.total.value, 0)
    assert.equal(state.loading.value, false)
  })
}

test('DCC 清空目录时仍清空总数且不请求文件列表', async () => {
  const state = setup('browser', 2)
  state.selectedDirectoryId.value = undefined
  await state.load()
  assert.equal(state.total.value, 0)
  assert.equal(state.requests.length, 0)
})
