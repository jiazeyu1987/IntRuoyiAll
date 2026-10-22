import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const require = createRequire(import.meta.url)
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const panel = path.join(root, 'src/views/mes/pro/edhr-batch/components/BatchReverseTracePanel.vue')
const { useSize } = await import('../node_modules/element-plus/es/components/splitter/src/hooks/useSize.mjs')
const { addUnit } = await import('../node_modules/element-plus/es/utils/dom/style.mjs')

// Execute the application's full SFC setup/template and installed Element Plus
// sizing hook. Only external viewport input, child rendering and I/O are replaced.
// This is an offline size contract; it does not claim a real DOM/browser PASS.
function mountPanel(width) {
  const viewportWidth = vue.ref(width)
  const route = { path: '/mes/pro/feedback/edhr-batch-history', query: {} }
  const calls = []
  const forbiddenIo = () => { calls.push('unexpected I/O'); throw new Error('Size contract must not perform business I/O') }
  const dependencies = {
    vue,
    '@vueuse/core': { useWindowSize: () => ({ width: viewportWidth, height: vue.ref(720) }) },
    'vue-router': { useRoute: () => route, useRouter: () => ({ push: forbiddenIo }) },
    'element-plus': { ElMessage: { warning: forbiddenIo, error: forbiddenIo, info: forbiddenIo } },
    '@/utils/auth': { getTenantId: () => 1, getVisitTenantId: () => undefined },
    '@/store/modules/user': { useUserStore: () => ({ getUser: { id: 347 }, getPermissions: new Set(['mes:pro-edhr-batch-execution:query']) }) },
    '@/api/mes/pro/edhr/reverseTrace': {
      getReverseTraceCatalog: forbiddenIo, getReverseTraceEvidence: forbiddenIo, queryReverseTrace: forbiddenIo
    }
  }
  const cache = new Map()
  function load(filename) {
    if (cache.has(filename)) return cache.get(filename).exports
    const module = { exports: {} }; cache.set(filename, module)
    const source = readFileSync(filename, 'utf8')
    const code = filename.endsWith('.vue') ? compileScript(parse(source).descriptor, { id: filename }).content : source
    const js = ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
    const localRequire = (name) => {
      if (Object.hasOwn(dependencies, name)) return dependencies[name]
      if (name.startsWith('.') || name.startsWith('@/')) {
        const base = name.startsWith('@/') ? path.join(root, 'src', name.slice(2)) : path.resolve(path.dirname(filename), name)
        return load(existsSync(base) ? base : base + '.ts')
      }
      return require(name)
    }
    new Function('require', 'module', 'exports', 'window', js)(localRequire, module, module.exports, { scrollY: 0, scrollTo: forbiddenIo })
    return module.exports
  }
  const descriptor = parse(readFileSync(panel, 'utf8')).descriptor
  const script = compileScript(descriptor, { id: panel })
  const template = compileTemplate({ source: descriptor.template.content, filename: panel, id: panel, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const module = { exports: {} }
  const templateJs = ts.transpileModule(template.code, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  new Function('require', 'module', 'exports', templateJs)(require, module, module.exports)
  const component = { ...load(panel).default, render: module.exports.render }
  let drawerProps
  const renderer = vue.createRenderer({
    createElement: () => ({}), createText: () => ({}), createComment: () => ({}),
    insert() {}, remove() {}, setText() {}, setElementText() {},
    parentNode: () => null, nextSibling: () => null, patchProp() {}
  })
  const app = renderer.createApp({ render: () => vue.h(component, { visible: false, anchorBatchExecutionId: '900000001228' }) })
  app.component('ElDrawer', {
    props: ['size', 'modelValue'],
    setup(props) { drawerProps = props; return () => null }
  })
  app.directive('loading', {})
  for (const name of ['ElAlert', 'ElButton', 'ElTag', 'ElDatePicker', 'ElEmpty', 'ElTabPane', 'ElTabs', 'ElSelect', 'ElOption', 'ElInput', 'ElTableColumn', 'ElTable', 'ElPagination', 'ElDescriptionsItem', 'ElDescriptions', 'ElCollapseItem', 'ElCollapse']) {
    app.component(name, { render: () => null })
  }
  app.mount({})
  const scope = vue.effectScope()
  // Drawer2.mjs passes addUnit(size) into the second splitter panel for rtl.
  const panels = vue.ref([])
  const containerSize = vue.ref(width)
  const sizes = scope.run(() => useSize(panels, containerSize))
  // The installed useSize watcher is non-immediate; actual panel registration
  // and container resize trigger it. Register panels after the hook subscribes.
  scope.run(() => vue.watch(() => drawerProps.size, (size) => {
    panels.value = [{}, { size: addUnit(size) }]
  }, { immediate: true }))
  return {
    viewportWidth, containerSize, calls,
    get size() { return drawerProps.size },
    get panelWidth() { return sizes.pxSizes.value[1] },
    unmount() { app.unmount(); scope.stop() }
  }
}

for (const [width, expected] of [[1280, 960], [1920, 960], [960, 960], [640, 640], [375, 375]]) {
  test(`actual reverse SFC yields usable ${expected}px drawer in ${width}px viewport`, async () => {
    const m = mountPanel(width)
    try {
      await vue.nextTick()
      assert.equal(m.panelWidth, expected, 'installed Splitter must receive a usable finite drawer width')
      assert.equal(typeof m.size, 'number', 'Drawer size must use the supported numeric contract')
      assert.ok(m.panelWidth > 0 && m.panelWidth <= width)
      assert.deepEqual(m.calls, [])
    } finally { m.unmount() }
  })
}

test('actual SFC recomputes installed splitter width when the viewport shrinks and grows', async () => {
  const m = mountPanel(1280)
  try {
    await vue.nextTick()
    for (const [width, expected] of [[375, 375], [800, 800], [1920, 960]]) {
      m.viewportWidth.value = width; m.containerSize.value = width
      await vue.nextTick(); await vue.nextTick()
      assert.equal(m.panelWidth, expected)
      assert.equal(m.size, expected)
    }
    assert.deepEqual(m.calls, [])
  } finally { m.unmount() }
})

test('installed Element Plus rejects the legacy min CSS expression as a splitter size', async () => {
  const scope = vue.effectScope()
  const panels = vue.ref([])
  const size = vue.ref(1280)
  const parsed = scope.run(() => useSize(panels, size))
  try {
    panels.value = [{}, { size: addUnit('min(100vw, 960px)') }]
    await vue.nextTick()
    assert.equal(parsed.pxSizes.value[1], undefined)
    // Installed split-panel2.mjs reads pxSizes[index] ?? 0.
    assert.equal(parsed.pxSizes.value[1] ?? 0, 0)
  } finally { scope.stop() }
})
