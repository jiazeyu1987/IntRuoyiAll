const assert = require('node:assert/strict')
const { test, before } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const { pathToFileURL } = require('node:url')
const ts = require('typescript')
const pinia = require('pinia')
const root = path.resolve(__dirname, '..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const compile = source => ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
let cloneDeep
let getRawRoute
let useStore

before(async () => {
  cloneDeep = (await import(pathToFileURL(require.resolve('lodash-es/cloneDeep.js')).href)).default
  const source = read('src/utils/routerHelper.ts')
  const ast = ts.createSourceFile('routerHelper.ts', source, ts.ScriptTarget.Latest, true)
  let initializer
  for (const statement of ast.statements) {
    if (!ts.isVariableStatement(statement)) continue
    for (const declaration of statement.declarationList.declarations) {
      if (declaration.name.getText(ast) === 'getRawRoute') initializer = declaration.initializer.getText(ast)
    }
  }
  assert.ok(initializer, 'use the actual route projection helper')
  getRawRoute = Function(compile(`const getRawRoute = ${initializer}`) + '; return getRawRoute;')()
  const dependencies = {
    '@/router': { default: { currentRoute: { value: {} } } },
    '@/utils/routerHelper': { getRawRoute },
    pinia,
    '../index': { store: pinia.createPinia() },
    '@/utils': { findIndex: (list, predicate) => list.findIndex(predicate) },
    './user': { useUserStoreWithOut: () => ({}) },
    'lodash-es': { cloneDeep }
  }
  const module = { exports: {} }
  Function('require', 'module', 'exports', compile(read('src/store/modules/tagsView.ts')))(id => {
    assert.ok(Object.hasOwn(dependencies, id), `unexpected dependency: ${id}`)
    return dependencies[id]
  }, module, module.exports)
  useStore = () => module.exports.useTagsViewStore(pinia.createPinia())
})

const route = (editor = false, instance = {}) => ({
  path: editor ? '/mes/pro/route/edit/900026' : '/mes/pro/route',
  fullPath: editor ? '/mes/pro/route/edit/900026?routeVersionId=4' : '/mes/pro/route?page=2',
  name: editor ? 'MesProRouteEdit' : 'MesProRoute',
  hash: '', params: editor ? { id: '900026' } : {},
  query: editor ? { routeVersionId: '4', routeVersionStatus: 'LEGACY_INCOMPLETE' } : { page: '2', code: ['a', 'b'] },
  meta: { title: '工艺流程', activeMenu: '/mes/pro/route', noTagsView: editor, tagsViewKeyMode: 'path', custom: { value: 'preserved' } },
  matched: [{ name: 'MesProRoute', path: '/mes/pro/route', meta: { title: '工艺流程' },
    instances: { default: instance }, components: { default: { setup() {} } } }]
})
const assertProjected = value => {
  assert.deepEqual(Object.keys(value.matched[0]).sort(), ['meta', 'name', 'path'])
}

test('actual replace snapshots circular matched instances without retaining runtime objects', () => {
  const instance = { registry: new Map() }
  instance.self = instance
  instance.registry.set('component', instance)
  const store = useStore(), original = route(false, instance), editor = route(true, instance)
  store.addView(original)
  const previous = store.replaceActiveMenuView(editor)
  assertProjected(previous)
  assertProjected(store.getVisitedViews[0])
  assert.deepEqual(previous.query, original.query)
  assert.deepEqual(store.getVisitedViews[0].query, editor.query)
  assert.equal(store.getVisitedViews[0].meta.custom.value, 'preserved')
  assert.equal(store.getSelectedTag.fullPath, editor.fullPath)
  original.query.code.push('changed')
  original.meta.custom.value = 'changed'
  editor.query.routeVersionId = 'changed'
  assert.deepEqual(previous.query.code, ['a', 'b'])
  assert.equal(previous.meta.custom.value, 'preserved')
  assert.equal(store.getVisitedViews[0].query.routeVersionId, '4')
  store.restoreActiveMenuView(store.getVisitedViews[0], previous)
  assert.equal(store.getVisitedViews.length, 1)
  assert.equal(store.getVisitedViews[0].fullPath, '/mes/pro/route?page=2')
  assert.equal(store.getCachedViews.includes('MesProRoute'), true)
})

test('deep mounted component graphs fail raw clone but not previous/current route snapshots', () => {
  const instance = {}, store = useStore()
  let node = instance
  for (let index = 0; index < 10000; index++) node = node.child = {}
  const original = route(false, instance), editor = route(true, instance)
  assert.throws(() => cloneDeep(editor), RangeError)
  store.addView(original)
  const previous = store.replaceActiveMenuView(editor)
  assertProjected(previous)
  assertProjected(store.getVisitedViews[0])
  store.restoreActiveMenuView(editor, previous)
  assert.equal(store.getVisitedViews[0].fullPath, original.fullPath)
})

test('snapshot never reads matched instance internals and restores a newly inserted hidden tag', () => {
  const instance = {}
  Object.defineProperty(instance, 'runtime', { enumerable: true, get() { throw new Error('runtime must not be cloned') } })
  const store = useStore(), editor = route(true, instance)
  assert.equal(store.replaceActiveMenuView(editor), undefined)
  assertProjected(store.getVisitedViews[0])
  assert.equal(store.getVisitedViews[0].title, '工艺流程')
  store.restoreActiveMenuView(editor, undefined)
  assert.equal(store.getVisitedViews.length, 0)
})

test('an updated tag with a live matched route is projected again before the next editor replacement', () => {
  const store = useStore(), original = route()
  store.addView(original)
  const updated = route()
  Object.defineProperty(updated.matched[0].instances, 'default', { enumerable: true, get() { throw new Error('live instance traversal') } })
  store.updateVisitedView(updated)
  const previous = store.replaceActiveMenuView(route(true))
  assertProjected(previous)
  assert.deepEqual(previous.query, updated.query)
})
