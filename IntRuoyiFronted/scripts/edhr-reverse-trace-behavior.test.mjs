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
    vue, 'vue-router': { useRoute: () => route, useRouter: () => router },
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
      app.directive('loading', {})
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

async function ready(env = environment()) {
  const mounted = env.mount()
  mounted.props.visible = true
  await flush()
  assert.equal(mounted.state.conditions.value.length, 1, 'catalog loaded into actual setup')
  return { ...env, ...mounted }
}

test('R6 dates retain multiple custom conditions and new metadata', async () => {
  const e = await ready(environment({ catalog: () => catalog([item(1), item(2)]) })); const s = e.state
  s.addConditionFromItem(item(2)); s.conditions.value[0].operator = 'GT'; s.conditions.value[0].value = '42'; s.conditions.value[1].value = '17'
  await s.runQuery(); const before = copy(s.conditions.value)
  e.api.getReverseTraceCatalog = async () => catalog([{ ...item(2), label: 'updated' }, item(1)], { catalogVersion: 'v2' })
  s.releaseApprovedTime.value = ['2026-03-01', '2026-04-01']; await flush()
  assert.deepEqual(s.conditions.value.map(c => [c.conditionId, c.item.evidenceKey, c.operator, c.value]), before.map(c => [c.conditionId, c.item.evidenceKey, c.operator, c.value]))
  assert.equal(s.conditions.value[1].item.label, 'updated'); assert.equal(s.queryResponse.value, undefined); e.unmount()
})
for (const defect of ['missing', 'sourceView', 'sourceRef', 'semanticIdentity', 'qualifiers', 'valueType', 'unit', 'operator', 'value', 'blocked']) test(`R6 invalid ${defect} remains visible and blocks query`, async () => {
  const original = { ...item(), semanticIdentity: 'field', qualifiers: { version: '1' }, unit: 'C' }
  const e = await ready(environment({ catalog: () => catalog([original]) })); const s = e.state
  s.conditions.value[0].operator = 'GT'; s.conditions.value[0].value = '42'
  const changed = { ...original }
  if (['sourceView', 'sourceRef', 'semanticIdentity', 'valueType', 'unit'].includes(defect)) changed[defect] = 'changed'
  if (defect === 'qualifiers') changed.qualifiers = { version: '2' }
  if (defect === 'operator') changed.allowedOperators = ['EQ']
  if (defect === 'value') changed.allowedValues = ['10']
  e.api.getReverseTraceCatalog = async () => catalog(defect === 'missing' ? [item(99)] : [changed], { catalogVersion: 'v2', ...(defect === 'blocked' ? { categories: [{ category: 'FIELD', status: 'BLOCKED' }] } : {}) })
  s.releaseApprovedTime.value = ['2026-03-01', '2026-04-01']; await flush()
  assert.equal(s.conditions.value[0].value, '42'); assert.equal(s.conditions.value[0].operator, 'GT')
  await s.runQuery(); assert.equal(e.calls.query.length, 0); assert.match(s.blockedReason.value, /条件/); e.unmount()
})
for (const failure of [false, true]) test(`R6 newer scope wins over old ${failure ? 'failure' : 'success'}`, async () => {
  const e = await ready(); const s = e.state; s.conditions.value[0].value = '42'
  const older = deferred(), newer = deferred(); let count = 0
  e.api.getReverseTraceCatalog = () => ++count === 1 ? older.promise : newer.promise
  s.releaseApprovedTime.value = ['2026-01-01', '2026-02-01']; s.releaseApprovedTime.value = ['2026-03-01', '2026-04-01']
  newer.resolve(catalog([item()], { catalogVersion: 'newest' })); await flush()
  if (failure) older.reject(new Error('old error')); else older.resolve(catalog([item(99)]))
  await flush(); assert.equal(s.catalog.value.catalogVersion, 'newest'); assert.equal(s.catalogError.value, '')
  assert.equal(s.conditions.value[0].value, '42'); e.unmount()
})
for (const phase of ['pending', 'failed', 'changed-version']) test(`R6 reopen preserves drafts after ${phase}`, async () => {
  const e = await ready(environment({ catalog: () => catalog([item(1), item(2)]) })); const s = e.state
  s.addConditionFromItem(item(2)); s.conditions.value[0].operator = 'GT'; s.conditions.value[0].value = '42'; await s.runQuery()
  const pending = deferred(); e.api.getReverseTraceCatalog = () => pending.promise
  s.releaseApprovedTime.value = ['2026-03-01', '2026-04-01']
  if (phase === 'failed') { pending.reject(new Error('offline')); await flush(); await s.runQuery(); assert.equal(e.calls.query.length, 1) }
  if (phase === 'changed-version') { pending.resolve(catalog([item(1), item(2)], { catalogVersion: 'v2' })); await flush() }
  s.close(); e.props.visible = false; await flush()
  if (phase === 'pending') { pending.resolve(catalog([item(99)])); await flush() }
  e.api.getReverseTraceCatalog = async () => catalog([item(1), item(2)], { catalogVersion: 'v2' }); e.props.visible = true; await flush()
  assert.equal(s.conditions.value.length, 2); assert.equal(s.conditions.value[0].value, '42'); assert.equal(s.conditions.value[0].operator, 'GT')
  assert.deepEqual([...s.releaseApprovedTime.value], ['2026-03-01', '2026-04-01']); assert.equal(s.queryResponse.value, undefined); e.unmount()
})

test('R6 scope refresh after return key reopens latest draft and revokes old cache', async () => {
  const e = await ready(); await e.state.runQuery(); const key = e.state.saveState(); e.unmount()
  const back = e.mount(panelFile, { visible: true, restoreKey: key }); await flush()
  back.state.conditions.value[0].value = '42'; back.state.releaseApprovedTime.value = ['2026-03-01', '2026-04-01']; await flush()
  back.state.close(); back.props.visible = false; await flush(); back.props.visible = true; await flush()
  assert.equal(back.state.conditions.value[0].value, '42'); assert.equal(back.state.queryResponse.value, undefined); back.unmount()
})
for (const action of ['edit', 'remove', 'reset']) test(`R6 pending catalog survives condition ${action}`, async () => {
  const e = await ready(); const s = e.state; const pending = deferred()
  e.api.getReverseTraceCatalog = () => pending.promise
  s.releaseApprovedTime.value = ['2026-03-01', '2026-04-01']; await flush()
  if (action === 'edit') s.conditions.value[0].value = '42'
  if (action === 'remove') s.removeCondition(0)
  if (action === 'reset') s.resetConditions()
  await flush(); assert.equal(s.loadingCatalog.value, true)
  pending.resolve(catalog([item()], { catalogVersion: 'v2' })); await flush()
  assert.equal(s.catalog.value?.catalogVersion, 'v2'); assert.equal(s.loadingCatalog.value, false)
  if (action === 'edit') assert.equal(s.conditions.value[0].value, '42')
  else assert.equal(s.conditions.value.length, 0)
  e.unmount()
})

test('R6 editing restored draft during catalog load supersedes cached input', async () => {
  const e = await ready(); const key = e.state.saveState(); e.unmount()
  const pending = deferred(); e.api.getReverseTraceCatalog = () => pending.promise
  const m = e.mount(panelFile, { visible: true, restoreKey: key }); await flush()
  m.state.conditions.value[0].value = '42'; await flush()
  pending.resolve(catalog()); await flush()
  assert.equal(m.state.conditions.value[0].value, '42')
  assert.equal(m.state.catalog.value?.catalogVersion, 'v1')
  assert.equal(m.state.queryResponse.value, undefined); assert.equal(m.state.dirty.value, true)
  m.unmount()
})

test('R6 invalid condition renders an alert and removal while query is disabled', async () => {
  const e = environment(); const m = e.mount(panelFile, { visible: true }, true); await flush()
  e.api.getReverseTraceCatalog = async () => catalog([item(99)], { catalogVersion: 'v2' })
  m.state.releaseApprovedTime.value = ['2026-03-01', '2026-04-01']; await flush()
  assert.ok(m.nodes.some(n => n.attrs.role === 'alert'))
  assert.ok(m.nodes.some(n => n.attrs['data-edhr-reverse-trace-remove-condition'] !== undefined))
  assert.equal(m.nodes.find(n => n.attrs['data-edhr-reverse-trace-query'] !== undefined).attrs.disabled, true)
  m.state.removeCondition(0); assert.equal(m.state.conditions.value.length, 0); m.unmount()
})

test('initially visible panel loads its anchor exactly once', async () => {
  const e = environment(); const m = e.mount(panelFile, { visible: true }); await flush()
  assert.equal(e.calls.catalog.length, 1); assert.equal(e.calls.catalog[0].anchorBatchExecutionId, '100'); m.unmount()
})
for (const field of ['value', 'operator']) test(`editing ${field} invalidates successful query and evidence`, async () => {
  const e = await ready(); const s = e.state; await s.runQuery(); await s.loadEvidence('200')
  s.conditions.value[0][field] = field === 'value' ? '11' : 'GT'; await flush()
  assert.equal(s.queryResponse.value, undefined); assert.equal(s.evidenceResponse.value, undefined); assert.equal(s.dirty.value, true)
  await s.loadEvidence('200'); assert.equal(e.calls.evidence.length, 1); e.unmount()
})
for (const action of ['edit', 'add', 'remove', 'reset', 'close', 'unmount', 'anchor', 'scope']) test(`delayed query cannot restore results after ${action}`, async () => {
  const pending = deferred(); const e = await ready(environment({ query: () => pending.promise })); const s = e.state
  const running = s.runQuery(); const request = e.calls.query[0]
  if (action === 'edit') s.conditions.value[0].value = '12'
  if (action === 'add') s.addConditionFromItem(item(2))
  if (action === 'remove') s.removeCondition(0)
  if (action === 'reset') s.resetConditions()
  if (action === 'close') s.close()
  if (action === 'unmount') e.unmount()
  if (action === 'anchor') e.props.anchorBatchExecutionId = '101'
  if (action === 'scope') s.releaseApprovedTime.value = ['2026-01-01', '2026-02-01']
  await flush(); pending.resolve(result(request)); await running; await flush()
  assert.equal(s.queryResponse.value, undefined); assert.equal(s.querying.value, false)
  if (action !== 'unmount') e.unmount()
})
test('evidence uses immutable normalized query rather than editable draft', async () => {
  const e = await ready(environment({ query: (r) => result({ ...r, conditions: [{ ...r.conditions[0], value: '10.000', qualifiers: { version: 'formal' } }] }) }))
  await e.state.runQuery(); await e.state.loadEvidence('200')
  assert.equal(e.calls.evidence[0].conditions[0].value, '10.000'); assert.deepEqual(e.calls.evidence[0].qualifiers, undefined)
  assert.deepEqual(e.calls.evidence[0].conditions[0].qualifiers, { version: 'formal' }); e.unmount()
})
test('both add entry points enforce ten conditions', async () => {
  const e = await ready(environment({ catalog: () => catalog(Array.from({ length: 12 }, (_, i) => item(i + 1))) }))
  for (let i = 2; i <= 12; i++) e.state.addConditionFromItem(item(i))
  e.state.addCondition(); assert.equal(e.state.conditions.value.length, 10); e.unmount()
})
for (const defect of ['version', 'total', 'missing-total', 'missing-page', 'duplicate']) test(`catalog rejects ${defect} instead of publishing partial data`, async () => {
  const first = Array.from({ length: 100 }, (_, i) => item(i))
  const e = environment({ catalog: (r) => r.pageNo === 1 ? catalog(first, { total: 101 }) : catalog(defect === 'missing-page' ? [] : [defect === 'duplicate' ? item(0) : item(100)], { total: defect === 'missing-total' ? undefined : defect === 'total' ? 102 : 101, catalogVersion: defect === 'version' ? 'v2' : 'v1' }) })
  const m = e.mount(); m.props.visible = true; await flush()
  assert.equal(m.state.catalog.value, undefined); assert.ok(m.state.catalogError.value); m.unmount()
})
test('catalog reads beyond one hundred pages without silently truncating', async () => {
  const e = environment({ catalog: (r) => catalog(Array.from({ length: r.pageNo === 101 ? 1 : 100 }, (_, i) => item((r.pageNo - 1) * 100 + i)), { total: 10001 }) })
  const m = e.mount(); await m.state.loadCatalog()
  assert.equal(m.state.catalog.value.items.length, 10001); assert.equal(e.calls.catalog.length, 101); m.unmount()
})
test('query and evidence have independent accessible page handlers', async () => {
  const e = await ready(); await e.state.runQuery()
  assert.equal(typeof e.state.changeResultPage, 'function'); assert.equal(typeof e.state.changeEvidencePage, 'function')
  await e.state.changeResultPage(2); await e.state.loadEvidence('200'); await e.state.changeEvidencePage(2)
  assert.equal(e.calls.query.at(-1).pageNo, 2); assert.equal(e.calls.evidence.at(-1).pageNo, 2)
  assert.equal(e.calls.evidence.at(-1).queryHash, 'hash1'); assert.equal(e.state.resultPage.value, 2); e.unmount()
})
test('stale target detail response cannot overwrite a newer batch', async () => {
  const pending = deferred(); const e = environment({ route: { path: detailPath, query: { batchExecutionId: '100', from: historyPath } }, detail: (params) => params.batchExecutionId === '100' ? pending.promise : Promise.resolve({ id: params.batchExecutionId, processes: [{}] }) })
  const m = e.mount(detailFile); e.route.query.batchExecutionId = '200'; await flush()
  pending.resolve({ id: '100', processes: [{}] }); await flush()
  assert.equal(m.state.detail.value.id, '200'); assert.equal(m.state.loading.value, false)
  assert.deepEqual(e.calls.detail, [{ batchExecutionId: '100' }, { batchExecutionId: '200' }]); m.unmount()
})
test('active-order source detail keeps the formal active-order query contract', async () => {
  const e = environment({ route: { path: detailPath, query: { activeOrderId: '300', from: 'execution' } } })
  const m = e.mount(detailFile); await flush()
  assert.deepEqual(e.calls.detail, [{ activeOrderId: '300' }])
  assert.equal(m.state.detail.value.id, '300'); m.unmount()
})
test('parameter standard and status remain display metadata and operators use business labels', async () => {
  const e = await ready(); const s = e.state
  const row = { ...s.conditions.value[0], operator: 'OUT_OF_LIMIT', value: '', item: { ...item(), category: 'PARAMETER', qualifiers: { deviceId: '9', parameterStatus: 'NORMAL', lowerLimit: '0', upperLimit: '10' } } }
  assert.deepEqual(copy(s.toCondition(row).qualifiers), { deviceId: '9' }); assert.equal(s.toCondition(row).value, null)
  assert.equal(s.operatorLabel('OUT_OF_LIMIT'), '超出当时标准'); assert.equal(s.operatorLabel('JUDGEMENT_EQ'), '判定等于'); e.unmount()
})
test('equipment condition uses namespaced equipment identity and retains PQC context for evidence', async () => {
  const pqcEquipment = {
    ...item(1), category: 'EQUIPMENT', evidenceKey: 'EQUIPMENT:PQC:selectedEquipmentId:8801',
    sourceRef: 'pqc-aggregate-snapshot:7001', savedValue: '8801',
    qualifiers: { regulationVersionId: '7101', routeProcessId: '3001', sampleNo: '1', itemCode: 'APPEARANCE', selectedEquipmentId: '8801', selectedEquipmentCode: 'PQC-01', selectedEquipmentNumber: 'EQ-8801' }
  }
  const e = await ready(environment({ catalog: () => catalog([pqcEquipment]) }))
  const condition = e.state.conditions.value[0]
  assert.equal(condition.item.evidenceKey, 'EQUIPMENT:PQC:selectedEquipmentId:8801')
  assert.equal(e.state.toCondition(condition).qualifiers, undefined, 'inspection context must not become an implicit equipment filter')
  assert.match(e.state.formatEvidenceContext(condition.item.qualifiers), /样本：1/)
  assert.match(e.state.formatEvidenceContext(condition.item.qualifiers), /检验项目：APPEARANCE/)
  const productionEquipment = { ...pqcEquipment, evidenceKey: 'EQUIPMENT:PRODUCTION:deviceId:8801' }
  e.state.addConditionFromItem(productionEquipment)
  assert.equal(e.state.conditions.value[1].item.evidenceKey, 'EQUIPMENT:PRODUCTION:deviceId:8801')
  assert.equal(e.state.toCondition(e.state.conditions.value[1]).qualifiers, undefined)
  e.unmount()
})
test('OUT_OF_LIMIT query and evidence preserve a null operand', async () => {
  const outOfLimitItem = { ...item(), category: 'PARAMETER', allowedOperators: ['OUT_OF_LIMIT'], savedValue: '' }
  const e = await ready(environment({ catalog: () => catalog([outOfLimitItem], { categories: [{ category: 'PARAMETER', status: 'AVAILABLE' }] }) }))
  await e.state.runQuery()
  assert.equal(e.calls.query[0].conditions[0].operator, 'OUT_OF_LIMIT')
  assert.equal(e.calls.query[0].conditions[0].value, null)
  await e.state.loadEvidence('200')
  assert.equal(e.calls.evidence[0].conditions[0].operator, 'OUT_OF_LIMIT')
  assert.equal(e.calls.evidence[0].conditions[0].value, null)
  e.unmount()
})
test('non OUT_OF_LIMIT query with an empty operand is rejected before request', async () => {
  const e = await ready()
  e.state.conditions.value[0].operator = 'GT'
  e.state.conditions.value[0].value = ''
  await e.state.runQuery()
  assert.equal(e.calls.query.length, 0)
  assert.match(e.state.blockedReason.value, /必须填写条件值/)
  e.unmount()
})

for (const origin of ['history', 'detail']) test(`return restores original ${origin} anchor across remount`, async () => {
  const e = await ready(environment({ route: origin === 'detail' ? { path: detailPath, query: { batchExecutionId: '100', from: historyPath } } : { path: historyPath, query: { batchCode: 'original' } } }))
  await e.state.runQuery(); await e.state.loadEvidence('200'); await e.state.openTargetDetail('200')
  const target = e.calls.push.at(-1)
  assert.equal(typeof target.query.reverseTraceReturn, 'string', 'navigation must carry an opaque return reference')
  assert.notEqual(target.query.tab, 'reverseTrace', 'target must not silently become a new anchor')
  e.unmount(); const detail = e.mount(detailFile); await flush(); await detail.state.goBack(); detail.unmount()
  assert.equal(e.route.path, origin === 'history' ? historyPath : detailPath)
  if (origin === 'detail') assert.equal(e.route.query.batchExecutionId, '100')
  assert.equal(e.route.query.reverseTraceAnchor, '100')
  const back = e.mount(panelFile, { visible: true, anchorBatchExecutionId: '100', restoreKey: e.route.query.reverseTraceRestore }); await flush()
  assert.equal(back.state.queryResponse.value?.queryHash, 'hash1'); assert.equal(back.state.evidenceResponse.value?.targetBatchExecutionId, '200')
  assert.deepEqual([...back.state.evidenceOpen.value], ['evidence']); assert.equal(e.calls.query.length, 1); back.unmount()
})
test('lost return state prompts requery instead of displaying old success', async () => {
  const e = environment(); const m = e.mount(panelFile, { visible: true, restoreKey: 'expired-key' }); await flush()
  assert.match(m.state.blockedReason.value, /重新查询/); assert.equal(m.state.queryResponse.value, undefined); m.unmount()
})

for (const kind of ['catalog', 'evidence']) for (const action of ['edit', 'add', 'remove', 'reset', 'close', 'unmount', 'anchor', 'scope']) test(`delayed ${kind} cannot overwrite after ${action}`, async () => {
  const pending = deferred(); const e = await ready(); const s = e.state
  await s.runQuery()
  let running
  if (kind === 'catalog') { e.api.getReverseTraceCatalog = () => pending.promise; running = s.loadCatalog() }
  else { e.api.getReverseTraceEvidence = () => pending.promise; running = s.loadEvidence('200') }
  if (action === 'edit') {
    if (!s.conditions.value.length) s.addConditionFromItem(item())
    s.conditions.value[0].value = '12'
  }
  if (action === 'add') s.addConditionFromItem(item(2))
  if (action === 'remove') s.removeCondition(0)
  if (action === 'reset') s.resetConditions()
  if (action === 'close') s.close()
  if (action === 'unmount') e.unmount()
  if (action === 'anchor') { e.api.getReverseTraceCatalog = async () => catalog([], { anchorBatchExecutionId: '101' }); e.props.anchorBatchExecutionId = '101' }
  if (action === 'scope') { e.api.getReverseTraceCatalog = async () => catalog([]); s.releaseApprovedTime.value = ['2026-01-01', '2026-02-01'] }
  const keepsCatalog = kind === 'catalog' && ['edit', 'add', 'remove', 'reset'].includes(action)
  const draft = copy(s.conditions.value)
  await flush()
  pending.resolve(kind === 'catalog' ? catalog([item(999)]) : { catalogVersion: 'v1', queryHash: 'hash1', targetBatchExecutionId: '200', evidenceStatus: 'MATCHED', items: [{ actualValue: 'stale' }], total: 1 })
  await running; await flush()
  assert.equal(s.evidenceResponse.value, undefined)
  assert.equal(s.catalog.value?.items.some((v) => v.sourceRef === 'row:999') ?? false, keepsCatalog)
  if (keepsCatalog) {
    assert.deepEqual(copy(s.conditions.value), draft)
    await s.runQuery(); assert.equal(e.calls.query.length, 1)
  }
  assert.equal(s.loadingCatalog.value, false); assert.equal(s.loadingEvidence.value, false)
  if (action !== 'unmount') e.unmount()
})
test('an older evidence request does not settle the newer loading state', async () => {
  const first = deferred(), second = deferred(); const e = await ready(); await e.state.runQuery()
  e.api.getReverseTraceEvidence = (r) => r.targetBatchExecutionId === '200' ? first.promise : second.promise
  const a = e.state.loadEvidence('200'); const b = e.state.loadEvidence('201')
  first.resolve({ catalogVersion: 'v1', queryHash: 'hash1', targetBatchExecutionId: '200', evidenceStatus: 'MATCHED', items: [], total: 0 }); await a
  assert.equal(e.state.loadingEvidence.value, true)
  second.resolve({ catalogVersion: 'v1', queryHash: 'hash1', targetBatchExecutionId: '201', evidenceStatus: 'MATCHED', items: [], total: 0 }); await b
  assert.equal(e.state.loadingEvidence.value, false); assert.equal(e.state.evidenceResponse.value.targetBatchExecutionId, '201'); e.unmount()
})
test('page size changes reset only the corresponding page and keep normalized identity', async () => {
  const e = await ready(environment({ query: (r) => result({ ...r, conditions: [{ ...r.conditions[0], value: '10.000' }] }) }))
  await e.state.runQuery(); await e.state.changeResultPage(2); await e.state.loadEvidence('200'); await e.state.changeEvidencePage(2)
  await e.state.changeEvidencePageSize(50)
  assert.equal(e.calls.evidence.at(-1).pageNo, 1); assert.equal(e.calls.evidence.at(-1).pageSize, 50); assert.equal(e.state.resultPage.value, 2)
  await e.state.changeResultPageSize(50)
  assert.equal(e.calls.query.at(-1).pageNo, 1); assert.equal(e.calls.query.at(-1).pageSize, 50)
  assert.equal(e.calls.query.at(-1).conditions[0].value, '10.000'); e.unmount()
})
for (const changed of ['user', 'tenant', 'visit-tenant', 'permission', 'version']) test(`stored return results are discarded when ${changed} changes`, async () => {
  const e = await ready(); await e.state.runQuery(); await e.state.openTargetDetail('200')
  const key = e.route.query.reverseTraceReturn; e.unmount()
  if (changed === 'user') e.identity.userId = 8
  if (changed === 'tenant') e.identity.tenantId = 2
  if (changed === 'visit-tenant') e.identity.visitTenantId = 2
  if (changed === 'permission') e.identity.permissions.clear()
  if (changed === 'version') e.api.getReverseTraceCatalog = async () => catalog([item()], { catalogVersion: 'v2' })
  const m = e.mount(panelFile, { visible: true, restoreKey: key }); await flush()
  assert.equal(m.state.queryResponse.value, undefined); assert.match(m.state.blockedReason.value, /重新查询/)
  m.unmount()
})
test('return restores result/evidence pages, scope and actual drawer body scroll', async () => {
  const e = await ready(); const s = e.state
  s.releaseApprovedTime.value = ['2026-01-01', '2026-02-01']; await flush()
  await s.runQuery(); await s.changeResultPage(2); await s.loadEvidence('200'); await s.changeEvidencePage(2)
  const body = { scrollTop: 347 }
  s.panelBody.value = { scrollTop: 0, closest: (selector) => selector === '.el-drawer__body' ? body : null }
  s.rememberScroll(); await s.openTargetDetail('200'); const key = e.route.query.reverseTraceReturn; e.unmount()
  e.route.path = historyPath
  const back = e.mount(panelFile, { visible: true, restoreKey: key }); const restoredBody = { scrollTop: 0 }
  back.state.panelBody.value = { scrollTop: 0, closest: () => restoredBody }; await flush()
  assert.equal(restoredBody.scrollTop, 347); assert.equal(back.state.resultPage.value, 2); assert.equal(back.state.evidencePage.value, 2)
  assert.deepEqual([...back.state.releaseApprovedTime.value], ['2026-01-01', '2026-02-01']); back.unmount()
})
test('history return route opens original anchor after remount and same-route reuse', async () => {
  const e = environment({ route: { path: historyPath, query: { reverseTraceRestore: 'lost', reverseTraceAnchor: '100', tab: 'reverseTrace' } } })
  const m = e.mount(feature + 'BatchRecordHistoryPage.vue'); await flush()
  assert.equal(m.state.reverseTraceBatchExecutionId.value, '100'); assert.equal(m.state.reverseTraceVisible.value, true)
  e.route.query.reverseTraceRestore = 'another'; e.route.query.reverseTraceAnchor = '101'; await flush()
  assert.equal(m.state.reverseTraceBatchExecutionId.value, '101'); m.unmount()
})
test('detail return can reopen original panel on the same mounted route', async () => {
  const e = environment({ route: { path: detailPath, query: { batchExecutionId: '200', from: historyPath } } }); const m = e.mount(detailFile); await flush()
  e.route.query = { batchExecutionId: '100', from: historyPath, reverseTraceRestore: 'lost', tab: 'reverseTrace' }; await flush()
  assert.equal(m.state.reverseTraceVisible.value, true); assert.equal(m.state.batchExecutionId.value, '100'); assert.equal(m.state.detail.value.id, '100')
  assert.deepEqual(e.calls.detail.slice(-1)[0], { batchExecutionId: '100' }); m.unmount()
})

test('permission loss while open clears saved conditions as well as results', async () => {
  const e = await ready(); await e.state.runQuery(); e.identity.permissions.clear(); await flush()
  assert.equal(e.state.conditions.value.length, 0); assert.equal(e.state.catalog.value, undefined); assert.equal(e.state.queryResponse.value, undefined)
  assert.match(e.state.blockedReason.value, /权限/); e.unmount()
})
test('missing backend operators cannot become an invented EQ condition', async () => {
  const e = await ready(); e.state.removeCondition(0); e.state.addConditionFromItem({ ...item(9), allowedOperators: [] })
  assert.equal(e.state.conditions.value.length, 0); assert.ok(e.calls.messages.some((v) => /比较方式/.test(v))); e.unmount()
})
test('saved return state expires and never allows external return routes', async () => {
  const e = await ready(); await e.state.runQuery(); await e.state.openTargetDetail('200'); const key = e.route.query.reverseTraceReturn; e.unmount()
  const now = Date.now
  try {
    Date.now = () => now() + 31 * 60 * 1000
    const m = e.mount(panelFile, { visible: true, restoreKey: key }); await flush()
    assert.equal(m.state.queryResponse.value, undefined); assert.match(m.state.blockedReason.value, /重新查询/); m.unmount()
  } finally { Date.now = now }
  const unsafe = await ready(environment({ route: { path: 'https://example.invalid', query: {} } })); await unsafe.state.runQuery()
  await assert.rejects(unsafe.state.openTargetDetail('200'), /返回范围/); assert.equal(unsafe.calls.push.length, 0); unsafe.unmount()
})
test('query page hash drift rejects changed results and late API errors remain visible', async () => {
  const e = await ready(); await e.state.runQuery(); e.api.queryReverseTrace = async (r) => result(r, { queryHash: 'changed' })
  await e.state.changeResultPage(2); assert.equal(e.state.queryResponse.value, undefined); assert.match(e.state.blockedReason.value, /重新查询/)
  e.api.queryReverseTrace = async () => { throw new Error('真实查询失败') }; await e.state.runQuery()
  assert.equal(e.state.blockedReason.value, '真实查询失败'); assert.equal(e.state.querying.value, false); e.unmount()
})
test('one blocked category preserves all category reasons and available category queries', async () => {
  const categories = ['FIELD', 'PARAMETER', 'EQUIPMENT', 'PERSON', 'INSPECTION', 'MATERIAL'].map((category) => ({ category, status: category === 'INSPECTION' ? 'BLOCKED' : 'AVAILABLE', reason: category === 'INSPECTION' ? '检验正式来源缺失' : undefined }))
  const e = environment({ catalog: () => catalog([item()], { categories }) }); const m = e.mount(panelFile, { visible: true }); await flush()
  assert.equal(m.state.catalog.value?.categories.length, 6)
  assert.equal(m.state.categoryStatus('INSPECTION').reason, '检验正式来源缺失')
  await m.state.runQuery(); assert.equal(e.calls.query.length, 1); assert.equal(m.state.queryResponse.value.queryStatus, 'MATCHED'); m.unmount()
})
for (const source of ['non-released', 'missing-chain', 'missing-adapter']) test(`formal blocked catalog preserves six reasons without a count for ${source}`, async () => {
  // Exact getCatalog shapes: blockedCatalog omits version; the other global guards retain it.
  const reasonCode = source === 'non-released' ? 'BATCH_SCOPE_INVALID' : 'SOURCE_MISSING'
  const reason = source === 'non-released' ? '当前批次尚未进入上市放行历史，不能进行历史反查' : '六类正式来源适配器未完整接入'
  const categories = ['FIELD', 'PARAMETER', 'EQUIPMENT', 'PERSON', 'INSPECTION', 'MATERIAL'].map((category) => ({ category, status: 'BLOCKED', reasonCode, reason }))
  const response = { anchorBatchExecutionId: '100', categories, items: [], total: null }
  if (source !== 'non-released') response.catalogVersion = 'v1'
  const e = environment({ catalog: () => response }); const m = e.mount(panelFile, { visible: true }, true); await flush()
  assert.equal(m.state.catalogError.value, '')
  assert.deepEqual(copy(m.state.catalog.value), response)
  assert.equal(m.nodes.filter((n) => n.attrs.title === reason).length, 6, 'all six reasons reach rendered category alerts')
  assert.equal(m.state.conditions.value.length, 0); assert.equal(m.state.loadingCatalog.value, false)
  await m.state.runQuery(); assert.equal(e.calls.query.length, 0)
  assert.equal(m.state.queryResponse.value, undefined); assert.equal(m.state.evidenceResponse.value, undefined)
  assert.equal(m.nodes.some((n) => n.attrs['aria-label'] === '反查批次分页'), false)
  m.unmount()
})
test('null catalog total without formal blocked semantics remains an error', async () => {
  const e = environment({ catalog: () => catalog([], { total: null }) }); const m = e.mount(panelFile, { visible: true }); await flush()
  assert.equal(m.state.catalog.value, undefined); assert.match(m.state.catalogError.value, /总数/)
  assert.equal(m.state.conditions.value.length, 0); m.unmount()
})
test('compiled drawer template captures actual parent body scroll events', async () => {
  const e = environment(); const m = e.mount(panelFile, { visible: true }, true); await flush()
  const body = m.nodes.find((n) => n.attrs.class === 'el-drawer__body')
  assert.ok(body); body.scrollTop = 419
  // Native scroll does not bubble: deliver only capture listeners on ancestors.
  for (let n = body.parent; n; n = n.parent) n.attrs?.onScrollCapture?.({ target: body, currentTarget: n })
  assert.equal(m.state.scrollTop.value, 419)
  m.state.scrollTop.value = 0
  body.parent.attrs.onOpened()
  assert.equal(body.scrollTop, 0)
  m.unmount()
})
test('history form inputs and list page survive detail return without relying on route filters', async () => {
  const requests = []
  const e = environment({ history: async (request) => { requests.push(copy(request)); return { list: [], total: 0 } } })
  const history = e.mount(feature + 'BatchRecordHistoryPage.vue', {}, true); await flush()
  for (const [marker, value] of [['work-order', 'WO-7'], ['product-name', '产品甲'], ['batch-code', 'LOT-9'], ['release-time', ['2026-01-01', '2026-02-01']]]) {
    const input = history.nodes.find((n) => `data-edhr-history-${marker}-filter` in n.attrs)
    input.attrs['onUpdate:modelValue'](value)
  }
  const pagination = history.nodes.find((n) => n.attrs['onUpdate:page'])
  pagination.attrs['onUpdate:page'](3); pagination.attrs['onUpdate:limit'](50)
  await flush()
  history.state.openReverseTrace({ id: '100' })
  const p = e.mount(panelFile, { visible: true, anchorBatchExecutionId: history.state.reverseTraceBatchExecutionId.value, historyListState: history.state.queryParams }); await flush()
  await p.state.runQuery(); await p.state.openTargetDetail('200'); p.unmount(); history.unmount()
  const target = e.mount(detailFile); await flush(); await target.state.goBack(); target.unmount()
  const restored = e.mount(feature + 'BatchRecordHistoryPage.vue'); await flush()
  assert.equal(restored.state.queryParams.workOrderCode, 'WO-7'); assert.equal(restored.state.queryParams.productName, '产品甲')
  assert.equal(restored.state.queryParams.batchCode, 'LOT-9'); assert.equal(restored.state.queryParams.pageNo, 3); assert.equal(restored.state.queryParams.pageSize, 50)
  assert.deepEqual([...restored.state.queryParams.releaseApprovedTime], ['2026-01-01', '2026-02-01'])
  assert.deepEqual(requests.at(-1), { pageNo: 3, pageSize: 50, workOrderCode: 'WO-7', productName: '产品甲', batchCode: 'LOT-9', releasedOnly: true, releaseApprovedTime: ['2026-01-01', '2026-02-01'] })
  restored.unmount()
})
test('expired target Return navigates to history with a requery notice and no target anchor', async () => {
  const e = environment({ route: { path: detailPath, query: { batchExecutionId: '200', from: historyPath, reverseTraceReturn: 'expired' } } })
  const target = e.mount(detailFile); await flush(); await target.state.goBack(); target.unmount()
  assert.equal(e.route.path, historyPath); assert.equal(e.route.query.reverseTraceExpired, '1')
  const history = e.mount(feature + 'BatchRecordHistoryPage.vue'); await flush()
  assert.equal(history.state.reverseTraceVisible.value, false); assert.equal(history.state.reverseTraceBatchExecutionId.value, '')
  assert.match(history.state.reverseTraceNotice.value, /重新查询/); history.unmount()
})
for (const outcome of ['success', 'failure']) test(`old history ${outcome} cannot overwrite new filters, page or loading`, async () => {
  const old = deferred(), latest = deferred(); const requests = []
  const e = environment({ history: (request) => { requests.push(copy(request)); return requests.length === 1 ? old.promise : latest.promise } })
  const m = e.mount(feature + 'BatchRecordHistoryPage.vue'); await flush()
  Object.assign(m.state.queryParams, { workOrderCode: 'WO-NEW', pageNo: 3 })
  const running = m.state.getBatchList(); assert.equal(m.state.loading.value, true)
  latest.resolve({ list: [{ id: '300' }], total: 21 }); await running
  assert.equal(requests[1].pageNo, 3); assert.equal(requests[1].workOrderCode, 'WO-NEW')
  if (outcome === 'success') old.resolve({ list: [{ id: '100' }], total: 1 })
  else old.reject(new Error('旧列表请求失败'))
  await flush()
  assert.deepEqual(copy(m.state.batchList.value), [{ id: '300' }]); assert.equal(m.state.total.value, 21)
  assert.equal(m.state.loadError.value, ''); assert.equal(m.state.loading.value, false); m.unmount()
})
test('old history completion cannot end the loading state owned by a pending new page', async () => {
  const old = deferred(), latest = deferred(); let count = 0
  const e = environment({ history: () => ++count === 1 ? old.promise : latest.promise })
  const m = e.mount(feature + 'BatchRecordHistoryPage.vue'); await flush()
  m.state.queryParams.pageNo = 2; const running = m.state.getBatchList()
  old.resolve({ list: [{ id: '100' }], total: 1 }); await flush()
  assert.equal(m.state.loading.value, true); assert.deepEqual(copy(m.state.batchList.value), [])
  latest.resolve({ list: [{ id: '200' }], total: 20 }); await running
  assert.equal(m.state.loading.value, false); assert.equal(m.state.batchList.value[0].id, '200'); m.unmount()
})

const r7Blocked = (reasonCode = 'CATALOG_STALE') => ({
  catalogVersion: 'v1', queryHash: 'hash1', targetBatchExecutionId: '200',
  evidenceStatus: 'BLOCKED', reasonCode, reason: '正式来源校验失败', total: null, items: []
})
const r7Matched = () => ({ catalogVersion: 'v1', queryHash: 'hash1', targetBatchExecutionId: '200', evidenceStatus: 'MATCHED', total: 1, items: [{ conditionId: 'C1', actualValue: '10' }] })
const r7AssertRevoked = (s) => {
  assert.equal(s.queryResponse.value, undefined, 'known-invalid result list and total must be revoked')
  assert.equal(s.successfulQuery.value, undefined)
  assert.equal(s.evidenceResponse.value, undefined)
  assert.equal(s.evidenceTarget.value, '')
  assert.deepEqual([...s.evidenceOpen.value], [])
  assert.equal(s.resultPage.value, 1); assert.equal(s.evidencePage.value, 1)
  assert.equal(s.dirty.value, true); assert.match(s.blockedReason.value, /重新查询/)
}
for (const reason of ['CATALOG_STALE', 'RESULT_STALE', 'SOURCE_CONFLICT', 'SOURCE_MISSING', 'BATCH_SCOPE_INVALID', 'version', 'hash', 'target']) {
  test(`R7 ${reason} revokes success, operations and every recoverable snapshot`, async () => {
    const e = await ready(); const s = e.state
    await s.runQuery(); await s.changeResultPage(2); await s.loadEvidence('200'); await s.changeEvidencePage(2)
    const keys = [s.saveState(), s.saveState()]
    const before = copy(s.conditions.value)
    const response = reason === 'version' ? { ...r7Matched(), catalogVersion: 'v2' }
      : reason === 'hash' ? { ...r7Matched(), queryHash: 'other' }
        : reason === 'target' ? { ...r7Matched(), targetBatchExecutionId: '201' } : r7Blocked(reason)
    e.api.getReverseTraceEvidence = async () => response
    await s.loadEvidence('200'); r7AssertRevoked(s)
    assert.deepEqual(copy(s.conditions.value), before)
    const queries = e.calls.query.length
    await s.changeResultPage(2); await s.openTargetDetail('200')
    assert.equal(e.calls.query.length, queries); assert.equal(e.calls.push.length, 0)
    s.close(); e.props.visible = false; await flush(); e.props.visible = true; await flush()
    assert.equal(s.queryResponse.value, undefined); assert.equal(s.successfulQuery.value, undefined)
    e.unmount()
    for (const key of keys) {
      const back = e.mount(panelFile, { visible: true, restoreKey: key }, true); await flush()
      assert.equal(back.state.queryResponse.value, undefined)
      assert.equal(back.state.successfulQuery.value, undefined)
      assert.match(back.state.blockedReason.value, /重新查询/)
      assert.equal(back.nodes.some(n => 'data-edhr-reverse-trace-results' in n.attrs), false)
      assert.equal(back.nodes.some(n => n.attrs['aria-label'] === '反查批次分页'), false)
      back.unmount()
    }
  })
}
test('R7 transient evidence failure reports error and preserves valid query for retry', async () => {
  const e = await ready(); await e.state.runQuery()
  const success = copy(e.state.queryResponse.value)
  e.api.getReverseTraceEvidence = async () => { throw new Error('网络连接暂时中断') }
  await e.state.loadEvidence('200')
  assert.equal(e.state.blockedReason.value, '网络连接暂时中断')
  assert.deepEqual(copy(e.state.queryResponse.value), success)
  assert.equal(e.state.dirty.value, false)
  e.api.getReverseTraceEvidence = async () => r7Matched()
  await e.state.loadEvidence('200'); assert.equal(e.state.evidenceResponse.value.evidenceStatus, 'MATCHED')
  e.unmount()
})
for (const concurrent of ['older-evidence', 'newer-evidence', 'query-page']) test(`R7 invalidation defeats late ${concurrent}`, async () => {
  const e = await ready(); const s = e.state; await s.runQuery()
  const blocked = deferred(), late = deferred()
  e.api.getReverseTraceEvidence = () => blocked.promise
  let invalidating, pending
  if (concurrent === 'older-evidence') {
    e.api.getReverseTraceEvidence = () => late.promise; pending = s.loadEvidence('200')
    e.api.getReverseTraceEvidence = () => blocked.promise; invalidating = s.loadEvidence('200')
  } else {
    invalidating = s.loadEvidence('200')
    if (concurrent === 'newer-evidence') {
      e.api.getReverseTraceEvidence = () => late.promise; pending = s.loadEvidence('200')
    } else {
      e.api.queryReverseTrace = () => late.promise; pending = s.changeResultPage(2)
    }
  }
  const request = copy(s.successfulQuery.value)
  blocked.resolve(r7Blocked()); await invalidating; r7AssertRevoked(s)
  late.resolve(concurrent === 'query-page' ? result(request) : r7Matched()); await pending
  r7AssertRevoked(s); assert.equal(s.querying.value, false); assert.equal(s.loadingEvidence.value, false)
  e.unmount()
})

test('R7 a fresh explicit query restores usable results after invalidation', async () => {
  const e = await ready(); const s = e.state; await s.runQuery()
  e.api.getReverseTraceEvidence = async () => r7Blocked()
  await s.loadEvidence('200'); r7AssertRevoked(s)
  await s.runQuery()
  assert.equal(e.calls.query.length, 2); assert.equal(s.dirty.value, false)
  assert.equal(s.blockedReason.value, ''); assert.equal(s.queryResponse.value.coverageStatus, 'COMPLETE')
  e.api.getReverseTraceEvidence = async () => r7Matched()
  await s.loadEvidence('200'); assert.equal(s.evidenceResponse.value.evidenceStatus, 'MATCHED')
  await s.openTargetDetail('200'); assert.equal(e.calls.push.length, 1); e.unmount()
})
test('R7 rejection from a previous query cannot revoke a freshly queried snapshot', async () => {
  const e = await ready(); const s = e.state; await s.runQuery()
  const old = deferred(); e.api.getReverseTraceEvidence = () => old.promise
  const pending = s.loadEvidence('200')
  await s.runQuery()
  old.resolve(r7Blocked()); await pending
  assert.equal(s.queryResponse.value.coverageStatus, 'COMPLETE')
  assert.ok(s.successfulQuery.value); assert.equal(s.dirty.value, false)
  assert.equal(s.blockedReason.value, ''); e.unmount()
})
