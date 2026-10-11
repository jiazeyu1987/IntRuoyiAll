const test = require('node:test'), assert = require('node:assert/strict'), fs = require('node:fs'), path = require('node:path'), ts = require('typescript')
const vue = require('vue'), elementPlus = require('element-plus')
const resolver = require('unplugin-vue-components/resolvers').ElementPlusResolver
const root = path.resolve(__dirname, '../..')
function load(file, deps, exports) {
  const source = fs.readFileSync(path.join(root, file), 'utf8')
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(n => !ts.isImportDeclaration(n) && !ts.isExportAssignment(n)).map(n => n.getText(ast).replace(/^export /, '')).join('\n')
  const js = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const imports = ast.statements.filter(ts.isImportDeclaration).flatMap(n => n.importClause?.namedBindings?.elements?.map(e => e.name.text) || []).filter(name => name in deps)
  return Function(...imports, js + '; return {' + exports.join(',') + '}')(...imports.map(name => deps[name]))
}
const renderer = vue.createRenderer({
  patchProp() {}, insert(node, parent) { (parent.children ||= []).push(node) }, remove() {},
  createElement: tag => ({ tag, children: [] }), createText: text => ({ text }), createComment: text => ({ comment: text }),
  setText(node, text) { node.text = text }, setElementText(node, text) { node.text = text }, parentNode: () => null, nextSibling: () => null
})
test('official component resolvers use the same runtime entry and retain styles', async () => {
  const { createElementPlusResolver } = load('build/vite/elementPlusResolver.ts', { ElementPlusResolver: resolver }, ['createElementPlusResolver'])
  const official = resolver(), canonical = createElementPlusResolver()
  for (let index = 0; index < official.length; index++) {
    for (const name of index === 0 ? ['ElDialog', 'ElMessageBox', 'ElNotification', 'ElMessage'] : ['Loading']) {
      const before = await official[index].resolve(name), after = await canonical[index].resolve(name)
      assert.equal(after.from, 'element-plus')
      assert.deepEqual(after, { ...before, from: 'element-plus' })
    }
  }
  assert.equal(await canonical[0].resolve('OrdinaryWidget'), undefined)
  assert.deepEqual(await canonical[0].resolve('ElIconApple'), await official[0].resolve('ElIconApple'))
})
test('installed Vue/ElementPlus share monotonically increasing dialog and programmatic service layers', () => {
  const { setupElementPlus } = load('src/plugins/elementPlus/index.ts', elementPlus, ['setupElementPlus'])
  const increments = [], counter = {}
  const component = label => vue.defineComponent({ setup() { const z = elementPlus.useZIndex(); increments.push([label, z.nextZIndex()]); return () => null } })
  const app = renderer.createApp(vue.defineComponent({ setup: () => () => vue.h('div', Array.from({ length: 14 }, () => vue.h(component('dialog')))) }))
  setupElementPlus(app)
  app.mount(counter)
  for (const name of ['ElMessageBox', 'ElMessage', 'ElNotification']) {
    assert(elementPlus[name]._context === app._context, `${name} must inherit the application context`)
    const vnode = vue.createVNode(component(name)); vnode.appContext = elementPlus[name]._context
    renderer.render(vnode, {})
  }
  assert.equal(increments.length, 17)
  for (let i = 1; i < increments.length; i++) assert(increments[i][1] > increments[i - 1][1])
  assert.equal(app._context.provides[elementPlus.ZINDEX_INJECTION_KEY].current, 17)
  app.unmount()
})
test('both build plugin paths use the canonical resolver and optimization has no duplicate runtime entry', () => {
  const plugins = fs.readFileSync(path.join(root, 'build/vite/index.ts'), 'utf8')
  assert.equal((plugins.match(/resolvers: \[createElementPlusResolver\(\)\]/g) || []).length, 2)
  const optimize = fs.readFileSync(path.join(root, 'build/vite/optimize.ts'), 'utf8')
  assert(!/^\s*'element-plus\/es',\s*$/m.test(optimize))
})

test('installed auto-import and component plugins generate one canonical runtime entry', async () => {
  const { createElementPlusResolver } = load('build/vite/elementPlusResolver.ts', { ElementPlusResolver: resolver }, ['createElementPlusResolver'])
  const autoImport = require('unplugin-auto-import/vite')({ dts: false, resolvers: [createElementPlusResolver()] })
  const components = require('unplugin-vue-components/vite')({ dts: false, dirs: [], resolvers: [createElementPlusResolver()] })
  const autoResult = await autoImport.transform('ElMessageBox.confirm("continue"); ElNotification({message:"notice"});', path.join(root, 'src/__shared_overlay_contract__.ts'))
  const componentResult = await components.transform.call({ error(message) { throw new Error(message) } }, 'const _component_el_dialog = _resolveComponent("el-dialog");', path.join(root, 'src/__shared_overlay_contract__.vue'))
  for (const output of [autoResult, componentResult]) {
    assert(output?.code, 'the actual plugin must produce imports')
    assert.match(output.code, /from ["']element-plus["']/)
    assert.doesNotMatch(output.code, /from ["']element-plus\/es["']/)
  }
  assert.match(componentResult.code, /element-plus\/es\/components\/dialog\/style\/css/)
})
