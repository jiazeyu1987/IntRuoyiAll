const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')

const source = fs.readFileSync(path.join(__dirname, '../../src/views/dcc/controlled-file/upload/index.vue'), 'utf8')
const watcher = source.match(/watch\(\s*\(\) => route\.fullPath,[\s\S]*?\n\)/)[0]

function setup() {
  const calls = []
  const route = { path: '/dcc/controlled-file/upload', fullPath: '/dcc/controlled-file/upload' }
  let callback
  const context = {
    route, uploadRoutePath: route.path, formData: { processType: 'CONTROLLED_FILE' },
    isExternalReview: { value: false },
    watch: (_source, handler) => { callback = handler },
    resolveProcessTypeByRoute: () => { calls.push('process'); return 'CONTROLLED_FILE' },
    ...Object.fromEntries(['applyDccProjectCodeProductNumber', 'refreshRouteReadiness', 'loadApprovalUsers', 'loadSignoffDepartments']
      .map(name => [name, () => { calls.push(name) }]))
  }
  vm.runInNewContext(watcher, context)
  return { calls, route, context, change: () => callback() }
}

test('cached upload does not request or mutate when navigating to other tabs', () => {
  const state = setup()
  for (const target of ['/dcc/controlled-file/project-code', '/dcc/controlled-file/browser', '/system/user']) {
    state.route.path = target
    state.change()
    assert.deepEqual(state.calls, [])
    assert.equal(state.context.formData.processType, 'CONTROLLED_FILE')
  }
})

test('returning to upload and changing its query refreshes all required data', () => {
  const state = setup()
  state.route.path = '/dcc/controlled-file/project-code'
  state.change()
  state.calls.length = 0
  state.route.path = '/dcc/controlled-file/upload'
  for (const query of ['', '?project=123']) {
    state.route.fullPath = state.route.path + query
    state.change()
    assert.deepEqual(state.calls.splice(0), ['process', 'applyDccProjectCodeProductNumber', 'refreshRouteReadiness', 'loadApprovalUsers', 'loadSignoffDepartments'])
  }
})

test('external review keeps its signoff restriction', () => {
  const state = setup()
  state.context.isExternalReview.value = true
  state.change()
  assert.equal(state.calls.includes('loadSignoffDepartments'), false)
  assert.equal(state.calls.includes('loadApprovalUsers'), true)
})

test('route ownership is captured at setup using the AppView path cache key', () => {
  assert.match(source, /const route = useRoute\(\)\s*\nconst uploadRoutePath = route\.path/)
})
