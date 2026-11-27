const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/utils/tagsViewSnapshot.ts'),
  'utf8'
)
const moduleObject = { exports: {} }
vm.runInNewContext(
  ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText,
  { exports: moduleObject.exports }
)
const { snapshotTagsView } = moduleObject.exports
const runtime = new Map()
runtime.set('self', runtime)
Object.defineProperty(runtime, 'danger', {
  enumerable: true,
  get() {
    throw new Error('runtime must not be traversed')
  }
})
const route = {
  name: 'Edit',
  path: '/edit/1',
  fullPath: '/edit/1?tab=flow',
  hash: '',
  query: { tab: 'flow', values: ['a', null] },
  params: { id: '1' },
  meta: { title: 'Editor', activeMenu: '/list', keepAliveName: 'EditorCache', runtime },
  matched: [
    { name: 'Edit', path: '/edit/:id', meta: { icon: 'edit', runtime }, instances: runtime }
  ],
  redirectedFrom: runtime,
  runtime
}
const copy = snapshotTagsView(route)
assert.equal(copy.meta.title, 'Editor')
assert.equal(copy.meta.keepAliveName, 'EditorCache')
assert.equal(copy.query.tab, 'flow')
assert.equal(copy.params.id, '1')
assert.equal(copy.matched[0].meta.icon, 'edit')
assert.equal(copy.meta.runtime, undefined)
assert.equal(copy.runtime, undefined)
copy.query.values.push('b')
copy.params.id = '2'
copy.meta.title = 'Changed'
assert.equal(route.query.values.length, 2)
assert.equal(route.params.id, '1')
assert.equal(route.meta.title, 'Editor')
assert.doesNotThrow(() => JSON.stringify(copy))
console.log('tags-view-snapshot regression passed')
const storeSource = fs.readFileSync(
  path.resolve(__dirname, '../../src/store/modules/tagsView.ts'),
  'utf8'
)
const storeModule = { exports: {} }
vm.runInNewContext(
  ts.transpileModule(storeSource, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
    .outputText,
  {
    exports: storeModule.exports,
    require(id) {
      if (id === 'pinia') return { defineStore: (_name, options) => options }
      if (id.endsWith('tagsViewSnapshot')) return { snapshotTagsView }
      if (id.endsWith('routerHelper')) return { getRawRoute: (v) => v }
      return {}
    }
  }
)
const definition = storeModule.exports.useTagsViewStore
const state = Object.assign(definition.state(), definition.actions)
state.visitedViews = [
  {
    ...route,
    name: 'List',
    path: '/list',
    fullPath: '/list',
    meta: { title: 'List', noCache: false }
  }
]
const old = state.replaceActiveMenuView(route)
assert.equal(old.path, '/list')
assert.equal(state.visitedViews[0].path, '/edit/1')
assert.equal(state.selectedTag.fullPath, route.fullPath)
assert.ok(state.cachedViews.has('EditorCache'))
state.restoreActiveMenuView(route, old)
assert.equal(state.visitedViews[0].path, '/list')
assert.ok(state.cachedViews.has('List'))
console.log('tags-view replace/restore/cache regression passed')
