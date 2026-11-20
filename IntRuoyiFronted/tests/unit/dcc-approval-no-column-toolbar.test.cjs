const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/components/UserTableColumnGlobalEnhancer/index.vue')
const source = fs.readFileSync(filename, 'utf8')
function harness() {
  const route = vue.reactive({ path: '/dcc/controlled-file/detail/10', query: {} })
  const code = source.slice(source.indexOf('const TARGET_ROUTE_PREFIXES'), source.indexOf('const normalizeText'))
    + source.slice(source.indexOf('const referenceColumnControls ='), source.indexOf('const tabLocalPopoverStyle ='))
  return new Function('computed', 'reactive', 'useRoute', ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
    + '; return { route, panels, tableMap, isTargetRoute }')(vue.computed, vue.reactive, () => route)
}

test('DCC approval tasks suppress column controls while existing list toolbars retain them', () => {
  const h = harness()
  const toolbar = {}
  h.tableMap.set('existing', { tableEl: { isConnected: true, matches: selector => selector === '.project-folder-file-table',
    closest: () => ({ querySelector: selector => selector === '[data-dcc-reference-column-controls]' ? toolbar : undefined }) }, columns: [{}] })
  assert.equal(h.panels.value.length, 1)
  // Task identity and backend stage do not affect the approval-wide exclusion.
  for (const taskId of ['supervisor', 'assignment', 'signoff', 'approval', 'document-control']) {
    h.route.query = { handling: 'approval', from: 'approval-center', taskId }
    assert.equal(h.isTargetRoute(), false, taskId)
    assert.equal(h.panels.value.length, 0, 'existing teleported controls disappear immediately')
  }
  for (const routePath of ['/dcc/controlled-file/browser', '/approval-center', '/mes/pro/workorder']) {
    h.route.path = routePath
    h.route.query = {}
    assert.equal(h.isTargetRoute(), true, routePath)
    assert.equal(h.panels.value.length, 1)
  }
})

test('entering approval clears previous registrations and disconnects observers without scanning tables', () => {
  const h = harness(), calls = []
  h.route.query = { handling: 'approval' }
  h.tableMap.set('existing', {
    wrapperEl: { removeEventListener: name => calls.push(name) },
    observer: { disconnect: () => calls.push('observer') },
    sizeObserver: { disconnect: () => calls.push('sizeObserver') }
  })
  const scan = source.slice(source.indexOf('const scanTables ='), source.indexOf('const scheduleScan ='))
  new Function('isTargetRoute', 'tableMap', 'handleManagedHeaderPointerDown',
    ts.transpileModule(scan, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText + '; scanTables()')(h.isTargetRoute, h.tableMap, () => {})
  assert.equal(h.tableMap.size, 0)
  assert.deepEqual(calls, ['mousedown', 'pointerdown', 'observer', 'sizeObserver'])
})

test('enhancer script and template compile with the approval route exclusion', () => {
  const { descriptor, errors } = parse(source)
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: filename })
  const result = compileTemplate({ source: descriptor.template.content, filename, id: filename,
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(result.errors, [])
})
