const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const compiler = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '..')
const transpile = source => ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText
const load = file => {
  const exports = {}
  vm.runInNewContext(transpile(fs.readFileSync(path.join(root, file), 'utf8')), {
    exports, require
  })
  return exports
}
const { descriptor } = compiler.parse(fs.readFileSync(path.join(root, 'src/components/Dialog/src/Dialog.vue'), 'utf8'))
const script = compiler.compileScript(descriptor, { id: 'dialog-navigation' })
const exportsObject = {}
vm.runInNewContext(transpile(script.content), {
  ...vue, exports: exportsObject,
  require: name => name === '@/utils/propTypes' ? load('src/utils/propTypes.ts')
    : name === '@/utils/is' ? load('src/utils/is.ts') : require(name),
  document: { documentElement: { offsetHeight: 800 } }
})
const Dialog = { ...exportsObject.default, render: () => null }
const renderer = vue.createRenderer({
  createElement: type => ({ type, children: [] }),
  createComment: text => ({ text }), createText: text => ({ text }),
  setText() {}, setElementText() {}, patchProp() {},
  parentNode: node => node.parent,
  nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1],
  insert(node, parent, anchor) {
    if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1)
    node.parent = parent
    const index = anchor ? parent.children.indexOf(anchor) : -1
    if (index < 0) parent.children.push(node)
    else parent.children.splice(index, 0, node)
  },
  remove(node) { node.parent?.children.splice(node.parent.children.indexOf(node), 1) }
})
async function main() {
  const visible = vue.ref(true), sourcePage = vue.ref(true), updates = []
  const Source = { name: 'SourcePage', setup: () => () => vue.h(Dialog, {
    modelValue: visible.value,
    'onUpdate:modelValue': value => { visible.value = value; updates.push(value) }
  }) }
  const Other = { name: 'DestinationPage', render: () => null }
  const app = renderer.createApp({ setup: () => () => vue.h(vue.KeepAlive, {}, {
    default: () => vue.h(sourcePage.value ? Source : Other)
  }) })
  app.mount({ children: [] })
  await vue.nextTick()
  assert.equal(visible.value, true, 'Opening a page must preserve its dialog')
  sourcePage.value = false
  await vue.nextTick()
  assert.equal(visible.value, false, 'Leaving a cached page must close its teleported dialog')
  assert.deepEqual(updates, [false])
  sourcePage.value = true
  await vue.nextTick()
  assert.equal(visible.value, false, 'Returning must not restore the old overlay')
  sourcePage.value = false
  await vue.nextTick()
  assert.deepEqual(updates, [false], 'Closed dialogs must not emit redundant updates')
  app.unmount()
  console.log('PASS dialog closes on cached-page navigation and stays closed on return')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
