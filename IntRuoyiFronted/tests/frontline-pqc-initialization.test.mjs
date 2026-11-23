import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { runInNewContext } from 'node:vm'
import { parse, compileScript, compileTemplate } from 'vue/compiler-sfc'

const filename = 'FrontlineFixedTemplatePanel.vue'
const source = readFileSync(new URL('../src/views/mes/pro/feedback/' + filename, import.meta.url), 'utf8')
const between = (start, end) => {
  const a = source.indexOf(start), b = source.indexOf(end, a)
  assert.ok(a >= 0 && b > a)
  return source.slice(a, b)
}
const ref = value => ({ value })
const computed = fn => ({ get value() { return fn() } })
const deferred = () => {
  let resolve, reject
  const promise = new Promise((a, b) => { resolve = a; reject = b })
  return { promise, resolve, reject }
}
const harness = () => {
  const catalog = deferred(), refresh = deferred(), errors = [], opened = []
  const runtime = {
    initialHandoffEpoch: 0, frontlinePagePath: '/fill', route: { path: '/fill' }, computed, frontlineInitializing: ref(true), isPqcMode: ref(true), frontlinePanelRef: ref(undefined),
    payloadLoading: ref(false), pqcSignatureDialogVisible: ref(false), pqcSubmitResultUncertain: ref(false), pqcPatrolRoundCreating: ref(false),
    deviceState: { loadingActiveOrders: false, loadingProcesses: false, loadingEmployees: false, loadingTemplate: false },
    activePicker: ref(undefined), activeOrderKeyword: ref('previous'), activeOrderSearchInputRef: ref({ focus: () => opened.push('focus') }),
    nextTick: fn => fn(), document: { addEventListener() {} }, window: { addEventListener() {} },
    syncPqcFullscreenState() {}, handlePqcForegroundRefresh() {}, scheduleProductionViewportScaleUpdate() {},
    ResizeObserver: class { observe() {} }, resolveErrorMessage: error => error.message,
    initialHandoffPageMounted: false, hasInitialHandoffQuery: ref(false), hasReturnNotification: ref(false),
    hydrateContextFromRoute() {}, FrontlineTemplateApi: { getCatalog: () => catalog.promise },
    catalog: ref([]), draft: { fieldValues: {} }, buildPqcFieldValues: () => ({ checked: true }),
    refreshPqcActiveOrdersAndEnsureSelection: () => refresh.promise.then(() => { runtime.activePicker.value = undefined }),
    showFrontlineError: error => errors.push(error), onMounted: fn => { runtime.mount = fn }
  }
  const script = between('const isPqcProcessNavigationBlocked =', 'const isPqcProcessPreviousDisabled =') +
    between('const openPicker =', 'const closePicker =') +
    between('onMounted(async () => {', 'onUnmounted(() => {') +
    '\nObject.assign(globalThis, { isPqcProcessNavigationBlocked, openPicker })'
  runInNewContext(stripTypeScriptTypes(script), runtime)
  return { runtime, catalog, refresh, errors, opened }
}

test('catalog and automatic order selection finish before a PQC picker can open', async () => {
  const h = harness(), r = h.runtime, mounted = r.mount()
  assert.equal(r.isPqcProcessNavigationBlocked.value, true)
  r.openPicker('order')
  assert.equal(r.activePicker.value, undefined)
  h.catalog.resolve([{ code: 'real-template' }])
  await Promise.resolve(); await Promise.resolve()
  assert.equal(r.isPqcProcessNavigationBlocked.value, true)
  r.openPicker('process')
  assert.equal(r.activePicker.value, undefined)
  h.refresh.resolve()
  await mounted
  assert.equal(r.frontlineInitializing.value, false)
  assert.equal(r.isPqcProcessNavigationBlocked.value, false)
  r.openPicker('order')
  assert.equal(r.activePicker.value, 'order')
  assert.equal(r.activeOrderKeyword.value, '')
  assert.deepEqual(h.opened, ['focus'])
  assert.equal(h.errors.length, 0)
})

for (const phase of ['catalog', 'active-orders']) {
  test(`initial ${phase} failure remains visible and releases the initialization lock`, async () => {
    const h = harness(), mounted = h.runtime.mount(), error = new Error(phase + ' unavailable')
    if (phase === 'catalog') h.catalog.reject(error)
    else { h.catalog.resolve([]); await Promise.resolve(); h.refresh.reject(error) }
    await mounted
    assert.equal(h.errors[0], error)
    assert.equal(h.errors.length, 1)
    assert.equal(h.runtime.frontlineInitializing.value, false)
    assert.equal(h.runtime.isPqcProcessNavigationBlocked.value, false)
  })
}

for (const field of ['loadingActiveOrders', 'loadingProcesses', 'loadingEmployees', 'loadingTemplate']) {
  test(`PQC pickers remain blocked while ${field} is true`, () => {
    const { runtime: r } = harness()
    r.frontlineInitializing.value = false
    r.deviceState[field] = true
    assert.equal(r.isPqcProcessNavigationBlocked.value, true)
    r.openPicker('order')
    assert.equal(r.activePicker.value, undefined)
    r.deviceState[field] = false
    assert.equal(r.isPqcProcessNavigationBlocked.value, false)
  })
}

test('real PQC template binds order controls to the loading guard without a production-process selector', () => {
  const { descriptor, errors } = parse(source, { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'pqc-initialization-test' })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: 'pqc-initialization-test', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  assert.match(source, /data-pqc-order-summary-card\s+:disabled="isPqcProcessNavigationBlocked"/)
  assert.doesNotMatch(source, /data-pqc-production-process-select/)
})

for(const failure of [false,true])test(`mount late catalog ${failure?'failure':'success'} cannot pollute departed page`,async()=>{
 const h=harness(),r=h.runtime,pending=r.mount()
 r.initialHandoffEpoch++;r.route.path='/qa';r.catalog.value=[{code:'new-page'}]
 if(failure)h.catalog.reject(Error('old catalog failed'));else h.catalog.resolve([{code:'old-page'}])
 await pending
 assert.equal(r.catalog.value[0].code,'new-page');assert.deepEqual(h.errors,[]);assert.deepEqual(r.draft.fieldValues,{})
})

for(const departed of [false,true])test(`mount refresh synchronous epoch advance preserves ${departed?'stale isolation':'current errors'}`,async()=>{
 const h=harness(),r=h.runtime,started=deferred()
 r.refreshPqcActiveOrdersAndEnsureSelection=()=>{r.initialHandoffEpoch+=2;started.resolve();return h.refresh.promise}
 const pending=r.mount();h.catalog.resolve([])
 await started.promise
 if(departed){r.initialHandoffEpoch++;r.route.path='/qa'}
 h.refresh.reject(Error('returned unavailable'));await pending
 assert.equal(h.errors.length,departed?0:1)
 if(!departed)assert.equal(h.errors[0].message,'returned unavailable')
})
for(const departed of [false,true])test(`foreground refresh epoch advance preserves ${departed?'stale isolation':'current errors'}`,async()=>{
 const h=harness(),r=h.runtime
 r.document.visibilityState='visible';r.refreshPqcActiveOrdersAndEnsureSelection=()=>{r.initialHandoffEpoch+=2;return h.refresh.promise}
 const script=between('const handlePqcForegroundRefresh =', 'onMounted(async () => {')+';globalThis.runForeground=handlePqcForegroundRefresh'
 runInNewContext(stripTypeScriptTypes(script),r);r.runForeground()
 if(departed){r.initialHandoffEpoch++;r.route.path='/qa'}
 h.refresh.reject(Error('returned unavailable'));await Promise.resolve();await Promise.resolve()
 assert.equal(h.errors.length,departed?0:1)
})

test('current handoff build error remains visible after initializer advances epoch',async()=>{
 const h=harness(),r=h.runtime;r.hasInitialHandoffQuery.value=true
 r.initializeInitialHandoffSelection=async()=>{r.initialHandoffEpoch+=2;return r.initialHandoffEpoch}
 r.buildPqcFieldValues=()=>{throw Error('current build failed')}
 const pending=r.mount();h.catalog.resolve([]);await pending
 assert.equal(h.errors.length,1);assert.equal(h.errors[0].message,'current build failed')
})
