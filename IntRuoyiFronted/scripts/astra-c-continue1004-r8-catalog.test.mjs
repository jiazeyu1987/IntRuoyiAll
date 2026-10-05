import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const require = createRequire(import.meta.url)
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const feature = 'src/views/mes/pro/edhr-batch/'
const panelFile = feature + 'components/BatchReverseTracePanel.vue'
const detailFile = feature + 'BatchExecutionActiveOrderDetailPage.vue'
const historyPath = '/mes/pro/feedback/edhr-batch-history'
const detailPath = '/mes/pro/feedback/edhr-batch-execution/active-order-detail'
const copy = (value) => JSON.parse(JSON.stringify(value))
const flush = async () => { for (let i = 0; i < 12; i++) await vue.nextTick() }
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
const item = (n = 1) => ({ category: 'FIELD', evidenceKey: `FIELD:v1:${n}`, sourceView: 'SAVED_RECORD', sourceRef: `row:${n}`, savedValue: '10', valueType: 'number', allowedOperators: ['EQ', 'GT'] })
const catalog = (items = [item()], extras = {}) => ({ anchorBatchExecutionId: '100', catalogVersion: 'v1', categories: [{ category: 'FIELD', status: 'AVAILABLE' }], items, total: items.length, ...extras })
const result = (request, extras = {}) => ({ catalogVersion: 'v1', queryHash: 'hash1', normalizedQuery: copy(request), queryStatus: 'MATCHED', coverageStatus: 'COMPLETE', total: 25, list: [{ batchExecutionId: '200', releaseStatus: 'RELEASED' }], ...extras })

// Execute compiled application setup with Vue's actual renderer, reactivity and lifecycle.
// Only external I/O/child views are stubbed. No application algorithm is copied here.
function environment(overrides = {}) {
  const calls = { catalog: [], query: [], evidence: [], detail: [], push: [], messages: [] }
  const identity = vue.reactive({ userId: 7, tenantId: 1, visitTenantId: undefined, permissions: new Set(['mes:pro-edhr-batch-execution:query']) })
  const route = vue.reactive({ path: historyPath, query: {}, ...overrides.route })
  const api = {
    getReverseTraceCatalog: async (request) => { calls.catalog.push(copy(request)); return (overrides.catalog || (() => catalog()))(request) },
    queryReverseTrace: async (request) => { calls.query.push(copy(request)); return (overrides.query || result)(request) },
    getReverseTraceEvidence: async (request) => { calls.evidence.push(copy(request)); return (overrides.evidence || (() => ({ catalogVersion: 'v1', queryHash: 'hash1', targetBatchExecutionId: request.targetBatchExecutionId, evidenceStatus: 'MATCHED', total: 150, items: [{ conditionId: 'C1', actualValue: '10' }] })))(request) }
  }
  const cache = new Map()
  const user = { get getUser() { return { id: identity.userId } }, get getPermissions() { return identity.permissions } }
  const router = { push: async (target) => { calls.push.push(copy(target)); Object.assign(route, target) }, replace: async (target) => { Object.assign(route, target) } }
  const dependencies = {
    vue, '@vueuse/core': { useWindowSize: () => ({ width: vue.ref(1280), height: vue.ref(720) }) }, 'vue-router': { useRoute: () => route, useRouter: () => router },
    'element-plus': { ElMessage: Object.fromEntries(['warning', 'error', 'info'].map((k) => [k, (v) => calls.messages.push(v)])) },
    '@/api/mes/pro/edhr/reverseTrace': api,
    '@/utils/auth': { getTenantId: () => identity.tenantId, getVisitTenantId: () => identity.visitTenantId },
    '@/store/modules/user': { useUserStore: () => user },
    '@/api/mes/pro/edhr/batchExecution': {
      getEdhrBatchActiveOrderDetail: async (params) => {
        calls.detail.push(copy(params))
        return (overrides.detail || (async (request) => ({ id: request.batchExecutionId || request.activeOrderId, processes: [{}] })))(params)
      },
      getEdhrBatchExecutionPage: overrides.history || (async () => ({ list: [], total: 0 }))
    }
  }
  function load(filename) {
    if (cache.has(filename)) return cache.get(filename).exports
    const module = { exports: {} }; cache.set(filename, module)
    const source = readFileSync(filename, 'utf8')
    const code = filename.endsWith('.vue') ? compileScript(parse(source).descriptor, { id: filename }).content : source
    const js = ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
    const localRequire = (name) => {
      if (dependencies[name]) return dependencies[name]
      if (name.endsWith('.vue')) return { default: { render: () => null } }
      if (name.startsWith('.') || name.startsWith('@/')) {
        const base = name.startsWith('@/') ? path.join(root, 'src', name.slice(2)) : path.resolve(path.dirname(filename), name)
        return load(existsSync(base) ? base : base + '.ts')
      }
      return require(name)
    }
    new Function('require', 'module', 'exports', 'window', js)(localRequire, module, module.exports, { scrollY: 0, scrollTo() {} })
    return module.exports
  }
  const nodes = []
  const renderer = vue.createRenderer({ createElement: (type) => {
    const node = { type, attrs: {}, parent: null, children: [], scrollTop: 0, closest(selector) {
      for (let n = this; n; n = n.parent) if (selector === '.' + n.attrs.class) return n
      return null
    } }; nodes.push(node); return node
  }, createText: () => ({}), createComment: () => ({}), insert(node, parent) { node.parent = parent; (parent.children ||= []).push(node) }, remove() {}, setText() {}, setElementText() {}, parentNode: (n) => n.parent, nextSibling: () => null, patchProp(node, key, prev, value) { node.attrs[key] = value } })
  function mount(file = panelFile, values = {}, renderTemplate = false) {
    const component = load(path.join(root, file)).default
    const props = vue.reactive({ visible: false, anchorBatchExecutionId: '100', ...values })
    let state
    let render = () => null
    if (renderTemplate) {
      const source = parse(readFileSync(path.join(root, file), 'utf8')).descriptor
      const script = compileScript(source, { id: file })
      const template = compileTemplate({ source: source.template.content, filename: file, id: file, compilerOptions: { bindingMetadata: script.bindings } })
      assert.equal(template.errors.length, 0)
      const module = { exports: {} }
      new Function('require', 'module', 'exports', ts.transpileModule(template.code, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText)(require, module, module.exports)
      render = module.exports.render
    }
    const wrapper = { ...component, render, setup(p, ctx) { state = component.setup(p, ctx); return state } }
    const app = renderer.createApp({ render: () => vue.h(wrapper, props) })
    if (renderTemplate) {
      app.directive('loading', { mounted(n, b) { n.loading = b.value }, updated(n, b) { n.loading = b.value } })
      app.component('ElDrawer', { setup(_, { attrs, slots }) { return () => vue.h('section', { ...attrs, class: 'el-drawer' }, [vue.h('div', { class: 'el-drawer__body' }, slots.default?.())]) } })
      app.component('ElTableColumn', { render: () => null })
      for (const name of ['ContentWrap', 'ElForm', 'ElFormItem', 'Pagination', 'ElAlert', 'ElTag', 'ElDatePicker', 'ElTabs', 'ElTabPane', 'ElButton', 'ElEmpty', 'ElSelect', 'ElOption', 'ElInput', 'ElTable', 'ElPagination', 'ElCollapse', 'ElCollapseItem', 'ElDescriptions', 'ElDescriptionsItem']) {
        app.component(name, { setup(_, { attrs, slots }) { return () => vue.h('div', attrs, slots.default?.()) } })
      }
    }
    app.mount({})
    return { state, props, nodes, unmount: () => app.unmount() }
  }
  return { mount, calls, route, identity, api }
}

const fact = (category, n = 1) => ({ ...item(n), category, evidenceKey: `${category}:v1:${n}`, sourceRef: `${category}:row:${n}` })
const pageFor = (request, items = [fact(request.category)], extra = {}) => catalog(items, {
  categories: [{ category: request.category, status: 'AVAILABLE' }], ...extra
})
async function open(e = environment({ catalog: pageFor }), render = false) {
  const m = e.mount(panelFile, { visible: true }, render); await flush()
  return { ...e, ...m }
}
async function choose(e, category) {
  e.state.activeCategory.value = category
  const running = e.state.handleCategoryChange(category); await flush(); return running
}

test('initial category alone reads every page and never publishes the first page as complete', async () => {
  const last = deferred()
  const e = await open(environment({ catalog: r => r.pageNo === 1
    ? pageFor(r, Array.from({ length: 100 }, (_, i) => fact(r.category, i)), { total: 101 }) : last.promise }))
  try {
    assert.deepEqual(e.calls.catalog.map(r => [r.category, r.pageNo, r.pageSize]), [['FIELD', 1, 100], ['FIELD', 2, 100]])
    assert.deepEqual(e.state.categoryItems('FIELD'), [])
    assert.equal(e.state.conditions.value.length, 0)
    await e.state.runQuery(); assert.equal(e.calls.query.length, 0)
    last.resolve(pageFor({ category: 'FIELD' }, [fact('FIELD', 100)], { total: 101 })); await flush()
    assert.equal(e.state.categoryItems('FIELD').length, 101)
    assert.equal(e.state.categoryCatalogs.value.FIELD.total, 101)
    assert.equal(e.state.conditions.value.length, 1)
  } finally { e.unmount() }
})

test('switch is usable during a large category load; only the selected list is masked and unused pagination stops', async () => {
  const field = deferred()
  const e = await open(environment({ catalog: r => r.category === 'FIELD' ? field.promise : pageFor(r) }), true)
  try {
    const panel = e.nodes.find(n => n.attrs.class === 'batch-reverse-trace-panel')
    assert.notEqual(panel.loading, true, 'a category response must not mask the whole panel')
    e.state.activeCategory.value = 'PERSON'; await e.state.handleCategoryChange('PERSON'); await flush()
    assert.equal(e.state.categoryItems('PERSON').length, 1)
    e.state.addConditionFromItem(e.state.categoryItems('PERSON')[0])
    await e.state.runQuery(); await e.state.loadEvidence('200')
    assert.equal(e.calls.query.length, 1, 'an unselected loading category cannot block complete selected conditions')
    assert.equal(e.calls.evidence.length, 1)
    field.resolve(pageFor({ category: 'FIELD' }, Array.from({ length: 100 }, (_, i) => fact('FIELD', i)), { total: 8935 })); await flush()
    assert.equal(e.calls.catalog.filter(r => r.category === 'FIELD').length, 1, 'do not continue unused ninety-page traversal')
    assert.deepEqual(e.state.categoryItems('FIELD'), [])
    assert.equal(e.state.conditions.value[0].category, 'PERSON')
  } finally { e.unmount() }
})

test('concurrent requests for one category share the same natural load; completed categories remain distinct', async () => {
  const p = deferred(); const e = await open()
  try {
    e.api.getReverseTraceCatalog = r => { e.calls.catalog.push(copy(r)); return p.promise }
    e.state.activeCategory.value = 'PERSON'
    const a = e.state.handleCategoryChange('PERSON'), b = e.state.handleCategoryChange('PERSON')
    assert.equal(e.calls.catalog.filter(r => r.category === 'PERSON').length, 1)
    p.resolve(pageFor({ category: 'PERSON' })); await Promise.all([a, b])
    await e.state.handleCategoryChange('PERSON')
    assert.equal(e.calls.catalog.filter(r => r.category === 'PERSON').length, 1)
    assert.equal(e.state.categoryItems('FIELD').length, 1); assert.equal(e.state.categoryItems('PERSON').length, 1)
  } finally { e.unmount() }
})

for (const lateFailure of ['http', 'version']) test(`unused category late ${lateFailure} preserves errors and respects result ownership`, async () => {
  const field = deferred()
  const e = await open(environment({ catalog: r => r.category === 'FIELD' ? field.promise : pageFor(r) }), true)
  try {
    e.state.activeCategory.value = 'PERSON'; await e.state.handleCategoryChange('PERSON')
    e.state.addConditionFromItem(e.state.categoryItems('PERSON')[0])
    await e.state.runQuery(); await e.state.loadEvidence('200')
    assert.equal(e.state.queryResponse.value?.queryHash, 'hash1')
    assert.equal(e.state.evidenceResponse.value?.targetBatchExecutionId, '200')
    if (lateFailure === 'http') field.reject(new Error('unused FIELD natural read failed'))
    else field.resolve(pageFor({ category: 'FIELD' }, [fact('FIELD')], { catalogVersion: 'v2' }))
    await flush()
    assert.ok(e.state.categoryErrors.value.FIELD, 'keep the failed category error visible')
    assert.equal(e.state.categoryLoading.value.FIELD, false)
    assert.equal(e.state.conditions.value[0].category, 'PERSON')
    assert.equal(e.state.queryResponse.value?.queryHash, lateFailure === 'http' ? 'hash1' : undefined)
    assert.equal(e.state.evidenceResponse.value?.targetBatchExecutionId, lateFailure === 'http' ? '200' : undefined)
    if (lateFailure === 'version') assert.match(e.state.catalogError.value, /版本/)
  } finally { e.unmount() }
})

for (const defect of ['page-version', 'page-total', 'short-page', 'duplicate', 'wrong-category', 'wrong-anchor', 'missing-total', 'http-failure', 'status-change']) {
  test(`category ${defect} fails without partial publication or query`, async () => {
    const first = Array.from({ length: 100 }, (_, i) => fact('FIELD', i))
    const e = await open(environment({ catalog: r => {
      if (r.pageNo === 1) return pageFor(r, first, { total: 101 })
      if (defect === 'http-failure') throw new Error('natural read failed')
      return pageFor(r, defect === 'short-page' ? [] : [fact(defect === 'wrong-category' ? 'PERSON' : 'FIELD', defect === 'duplicate' ? 0 : 100)], {
        total: defect === 'missing-total' ? undefined : defect === 'page-total' ? 102 : 101,
        catalogVersion: defect === 'page-version' ? 'v2' : 'v1',
        ...(defect === 'wrong-anchor' ? { anchorBatchExecutionId: '200' } : {}),
        ...(defect === 'status-change' ? { categories: [{ category: 'FIELD', status: 'BLOCKED', reasonCode: 'SOURCE_MISSING', reason: 'blocked' }] } : {})
      })
    } }))
    try {
      assert.deepEqual(e.state.categoryItems('FIELD'), [])
      assert.ok(e.state.categoryErrors.value.FIELD)
      assert.equal(e.state.categoryLoading.value.FIELD, false)
      await e.state.runQuery(); assert.equal(e.calls.query.length, 0)
    } finally { e.unmount() }
  })
}

test('cross-category version conflict revokes previous results and preserves selected conditions visibly', async () => {
  const e = await open()
  try {
    await e.state.runQuery(); const draft = copy(e.state.conditions.value)
    e.api.getReverseTraceCatalog = r => pageFor(r, [fact(r.category)], { catalogVersion: 'v2' })
    e.state.activeCategory.value = 'PERSON'; await e.state.handleCategoryChange('PERSON')
    assert.match(e.state.catalogError.value, /版本/)
    assert.deepEqual(copy(e.state.conditions.value), draft)
    assert.equal(e.state.queryResponse.value, undefined)
    await e.state.runQuery(); assert.equal(e.calls.query.length, 1)
    assert.deepEqual(e.state.categoryItems('PERSON'), [])
  } finally { e.unmount() }
})

test('formal single-category BLOCKED with no version/count remains a visible rejection', async () => {
  const e = await open(environment({ catalog: r => ({ anchorBatchExecutionId: '100', total: null, items: [], categories: [{ category: r.category, status: 'BLOCKED', reasonCode: 'SOURCE_MISSING', reason: 'formal source unavailable' }] }) }))
  try {
    assert.equal(e.state.categoryStatus('FIELD').reason, 'formal source unavailable')
    assert.equal(e.state.categoryErrors.value.FIELD, undefined)
    await e.state.runQuery(); assert.equal(e.calls.query.length, 0)
  } finally { e.unmount() }
})

for (const outcome of ['complete', 'failed', 'conflicting']) test(`restored cross-category conditions require fresh complete reads: ${outcome}`, async () => {
  const e = await open(); let back
  try {
    e.state.activeCategory.value = 'PERSON'; await e.state.handleCategoryChange('PERSON')
    e.state.addConditionFromItem(e.state.categoryItems('PERSON')[0])
    await e.state.runQuery(); await e.state.loadEvidence('200')
    const key = e.state.saveState(), draft = copy(e.state.conditions.value); e.unmount()
    const p = deferred(); e.api.getReverseTraceCatalog = r => r.category === 'PERSON' ? p.promise : pageFor(r)
    back = e.mount(panelFile, { visible: true, restoreKey: key }); await flush()
    assert.deepEqual(back.state.conditions.value.map(c => c.conditionId), draft.map(c => c.conditionId))
    assert.equal(back.state.queryResponse.value, undefined); assert.equal(back.state.evidenceResponse.value, undefined)
    await back.state.runQuery(); assert.equal(e.calls.query.length, 1)
    if (outcome === 'failed') p.reject(new Error('fresh category failure'))
    else p.resolve(pageFor({ category: 'PERSON' }, [fact('PERSON')], { catalogVersion: outcome === 'conflicting' ? 'v2' : 'v1' }))
    await flush()
    assert.deepEqual(back.state.conditions.value.map(c => c.conditionId), draft.map(c => c.conditionId))
    assert.equal(back.state.queryResponse.value?.queryHash, outcome === 'complete' ? 'hash1' : undefined)
    assert.equal(back.state.evidenceResponse.value?.targetBatchExecutionId, outcome === 'complete' ? '200' : undefined)
    if (outcome !== 'complete') { await back.state.runQuery(); assert.equal(e.calls.query.length, 1) }
  } finally { if (back) back.unmount(); else e.unmount() }
})

test('restoring cached state never overwrites an edit made while another selected category is loading', async () => {
  const e = await open(); let back
  try {
    e.state.activeCategory.value = 'PERSON'; await e.state.handleCategoryChange('PERSON'); e.state.addConditionFromItem(e.state.categoryItems('PERSON')[0]); await e.state.runQuery()
    const key = e.state.saveState(); e.unmount()
    const p = deferred(); e.api.getReverseTraceCatalog = r => r.category === 'PERSON' ? p.promise : pageFor(r)
    back = e.mount(panelFile, { visible: true, restoreKey: key }); await flush()
    back.state.conditions.value[0].value = '42'
    p.resolve(pageFor({ category: 'PERSON' })); await flush()
    assert.equal(back.state.conditions.value[0].value, '42')
    assert.equal(back.state.queryResponse.value, undefined)
  } finally { if (back) back.unmount(); else e.unmount() }
})

for (const action of ['close', 'scope', 'identity']) test(`late category response cannot reintroduce an invalidated ${action} snapshot`, async () => {
  const e = await open(); const p = deferred()
  try {
    e.api.getReverseTraceCatalog = () => p.promise
    e.state.activeCategory.value = 'PERSON'; const running = e.state.handleCategoryChange('PERSON')
    if (action === 'close') { e.state.close(); e.props.visible = false }
    if (action === 'scope') { e.api.getReverseTraceCatalog = pageFor; e.state.releaseApprovedTime.value = ['2026-10-01', '2026-10-04'] }
    if (action === 'identity') e.identity.permissions.clear()
    await flush(); p.resolve(pageFor({ category: 'PERSON' }, [fact('PERSON', 999)])); await running; await flush()
    assert.ok(!e.state.categoryItems('PERSON').some(i => i.sourceRef === 'PERSON:row:999'))
    assert.equal(e.state.queryResponse.value, undefined)
  } finally { e.unmount() }
})

test('selected category completeness blocks query even when another complete category provides a version', async () => {
  const e = await open(); const p = deferred()
  try {
    e.api.getReverseTraceCatalog = r => r.category === 'PERSON' ? p.promise : pageFor(r)
    e.state.activeCategory.value = 'PERSON'; const running = e.state.handleCategoryChange('PERSON')
    e.state.conditions.value.push({ conditionId: 'C2', category: 'PERSON', item: fact('PERSON'), operator: 'EQ', value: '10' })
    await e.state.runQuery(); assert.equal(e.calls.query.length, 0)
    p.resolve(pageFor({ category: 'PERSON' })); await running
    await e.state.runQuery(); assert.equal(e.calls.query.length, 1)
    assert.equal(e.calls.query[0].conditions.length, 2)
  } finally { e.unmount() }
})
