const assert = require('node:assert/strict')
const test = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = file => parse(fs.readFileSync(path.join(root, file), 'utf8')).descriptor
const listSource = read('src/views/system/notify/my/components/MyNotifyMessageList.vue').scriptSetup.content
const ast = ts.createSourceFile('notify.ts', listSource, ts.ScriptTarget.Latest, true)
const names = new Set(['loading', 'total', 'list', 'readTimeByMessageId', 'queryParams', 'mergeReadTimeOverrides', 'getList', 'wasDeactivated', 'loadError', 'loadSequence'])
const selected = ast.statements.filter(statement =>
  (ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(ast)))) ||
  (ts.isExpressionStatement(statement) && /^on(Mounted|Activated|Deactivated)\(/.test(statement.getText(ast)))
).map(statement => statement.getText(ast)).join('\n')
const js = ts.transpileModule(selected, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText

function harness() {
  let rows = [{ id: 4609 }]
  let failure
  let pageLoader
  const calls = [], errors = [], instances = []
  const api = { getMyNotifyMessagePage: async params => {
    calls.push({ ...params })
    if (failure) throw failure
    if (pageLoader) return pageLoader(params)
    return { list: rows, total: rows.length }
  } }
  const Notify = vue.defineComponent({
    name: 'ActualNotifyListLifecycle',
    setup() {
      const inboxModule = { exports: {} }
      const inboxSource = fs.readFileSync(path.join(root, 'src/views/system/notify/my/notifyInbox.ts'), 'utf8')
      Function('exports', ts.transpileModule(inboxSource, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText)(inboxModule.exports)
      const context = { ...vue, NotifyMessageApi: api, ...inboxModule.exports,
        props: { active: true }, canReadReleaseOutcomes: { value: false }, refreshTableLayout: async () => {} }
      const state = Function(...Object.keys(context), `${js}; return { loading, list, total, loadError, queryParams, getList };`)(...Object.values(context))
      instances.push(state)
      return () => vue.h('section', state.list.value.map(row => vue.h('span', String(row.id))))
    }
  })
  const node = tag => ({ tag, children: [], parent: null })
  const renderer = vue.createRenderer({
    createElement: node, createText: text => ({ ...node('#text'), text }), createComment: node,
    setText: (target, text) => { target.text = text },
    setElementText: (target, text) => { target.text = text },
    parentNode: target => target.parent,
    nextSibling: target => target.parent?.children[target.parent.children.indexOf(target) + 1],
    patchProp() {},
    insert(target, parent, anchor) {
      if (target.parent) target.parent.children.splice(target.parent.children.indexOf(target), 1)
      target.parent = parent
      const position = anchor ? parent.children.indexOf(anchor) : -1
      parent.children.splice(position < 0 ? parent.children.length : position, 0, target)
    },
    remove(target) {
      target.parent.children.splice(target.parent.children.indexOf(target), 1)
      target.parent = null
    }
  })
  return {
    Notify, calls, errors, instances,
    setRows: value => { rows = value }, setFailure: value => { failure = value },
    setPageLoader: value => { pageLoader = value },
    mount(render) {
      const app = renderer.createApp({ render })
      app.config.errorHandler = error => errors.push(error)
      app.mount(node('root'))
      return app
    }
  }
}
async function flush() {
  await vue.nextTick()
  for (let i = 0; i < 20; i++) await Promise.resolve()
  await vue.nextTick()
}

test('ordinary uncached route loads once on mount', async () => {
  const h = harness()
  const app = h.mount(() => vue.h(h.Notify))
  await flush()
  assert.equal(h.calls.length, 1)
  assert.deepEqual(h.instances[0].list.value.map(row => row.id), [4609])
  app.unmount()
})

test('KeepAlive first activation does not duplicate; returning loads newly delivered withdrawal', async () => {
  const h = harness(), visible = vue.ref(true)
  const app = h.mount(() => vue.h(vue.KeepAlive, null, { default: () => visible.value ? vue.h(h.Notify) : vue.h('aside') }))
  await flush()
  assert.equal(h.calls.length, 1)
  visible.value = false
  await flush()
  h.setRows([{ id: 4618 }, { id: 4609 }])
  visible.value = true
  await flush()
  assert.equal(h.calls.length, 2)
  assert.equal(h.instances.length, 1, 'cached list must actually be reused')
  assert.deepEqual(h.instances[0].list.value.map(row => row.id), [4618, 4609])
  app.unmount()
})

test('actual Profile tab condition loads on entering and remounts when entering again', async () => {
  const h = harness(), activeName = vue.ref('workbench')
  const descriptor = read('src/views/Profile/Index.vue')
  const find = item => item.tag === 'MyNotifyMessageList' ? item : (item.children || []).map(find).find(Boolean)
  const element = find(descriptor.template.ast)
  assert.ok(element)
  const result = compileTemplate({ source: element.loc.source, filename: 'profile-notify.vue', id: 'profile-notify', compilerOptions: { mode: 'function', prefixIdentifiers: false, cacheHandlers: false } })
  assert.deepEqual(result.errors, [])
  const render = Function('Vue', result.code)({ ...vue, resolveComponent: () => h.Notify })
  const app = h.mount(() => render({ activeName: activeName.value, handleUnreadNotifyMessageRefresh() {} }, []))
  await flush()
  assert.equal(h.calls.length, 0, 'hidden notification tab must not preload stale rows')
  activeName.value = 'notifyMessage'
  await flush()
  assert.equal(h.calls.length, 1)
  activeName.value = 'workbench'
  await flush()
  h.setRows([{ id: 4618 }])
  activeName.value = 'notifyMessage'
  await flush()
  assert.equal(h.calls.length, 2)
  assert.equal(h.instances.length, 2)
  assert.deepEqual(h.instances[1].list.value.map(row => row.id), [4618])
  app.unmount()
})

test('failed cached refresh exposes the original error and ends loading without inventing rows', async () => {
  const h = harness(), visible = vue.ref(true)
  const app = h.mount(() => vue.h(vue.KeepAlive, null, { default: () => visible.value ? vue.h(h.Notify) : vue.h('aside') }))
  await flush()
  visible.value = false
  await flush()
  const failure = new Error('actual notification request failed')
  h.setFailure(failure)
  visible.value = true
  await flush()
  assert.deepEqual(h.errors, [])
  assert.equal(h.instances[0].loadError.value, failure.message)
  assert.equal(h.instances[0].loading.value, false)
  assert.deepEqual(h.instances[0].list.value, [])
  assert.equal(h.instances[0].total.value, 0)
  app.unmount()
})

test('翻到第2页、中间页和末页时总数不清零，目标页和消息内容保持一致', async () => {
  const h = harness()
  const messages = Array.from({ length: 83 }, (_, index) => ({ id: 9000 - index }))
  h.setPageLoader(async params => ({
    list: messages.slice((params.pageNo - 1) * params.pageSize, params.pageNo * params.pageSize),
    total: messages.length
  }))
  const app = h.mount(() => vue.h(h.Notify))
  await flush()
  const state = h.instances[0]
  for (const pageNo of [2, 5, 9]) {
    let resolvePage
    h.setPageLoader(() => new Promise(resolve => { resolvePage = resolve }))
    state.queryParams.pageNo = pageNo
    const request = state.getList()
    await vue.nextTick()
    assert.equal(state.total.value, 83, '等待接口返回时不能把分页器的总数清零')
    assert.equal(state.loading.value, true)
    assert.equal(state.queryParams.pageNo, pageNo)
    assert.equal(h.calls.at(-1).pageNo, pageNo)
    const pageRows = messages.slice((pageNo - 1) * 10, pageNo * 10)
    resolvePage({ list: pageRows, total: 83 })
    await request
    assert.equal(state.queryParams.pageNo, pageNo)
    assert.deepEqual(state.list.value.map(row => row.id), pageRows.map(row => row.id))
    assert.equal(state.total.value, 83)
  }
  assert.deepEqual(h.calls.map(call => call.pageNo), [1, 2, 5, 9])
  app.unmount()
})

test('真实总数减少导致目标页越界时仍重新查询最后有效页', async () => {
  const h = harness()
  h.setPageLoader(async params => ({
    list: Array.from({ length: 10 }, (_, index) => ({ id: (params.pageNo - 1) * 10 + index })),
    total: 83
  }))
  const app = h.mount(() => vue.h(h.Notify))
  await flush()
  const state = h.instances[0]
  h.setPageLoader(async params => ({ list: params.pageNo === 2 ? [{ id: 88 }] : [], total: 11 }))
  state.queryParams.pageNo = 9
  await state.getList()
  assert.equal(state.queryParams.pageNo, 2)
  assert.equal(state.total.value, 11)
  assert.deepEqual(state.list.value.map(row => row.id), [88])
  assert.deepEqual(h.calls.map(call => call.pageNo), [1, 9, 2])
  app.unmount()
})
