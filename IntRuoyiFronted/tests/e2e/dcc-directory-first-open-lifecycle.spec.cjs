'use strict'

// Component lifecycle regression, not a business E2E or a simulated backend.
// Runs the page's actual form handlers and template node against Vue's renderer.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const vue = require('vue')
const ts = require('typescript')
const { parse, compileScript } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/dcc/controlled-file/directories/index.vue')
const source = fs.readFileSync(filename, 'utf8')
const { descriptor } = parse(source, { filename })
const formNode = descriptor.template.content.match(/<DirectoryForm\b[\s\S]*?\/>/)
assert.ok(formNode, 'the actual lazy DirectoryForm node must exist')
const script = ts.createSourceFile(filename + '.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const names = new Set(['DirectoryForm', 'formRef', 'directories', 'directoryFormMounted', 'pendingDirectoryFormRequest', 'openPendingDirectoryForm', 'openForm'])
const declarations = script.statements.filter((statement) => ts.isVariableStatement(statement)
  && statement.declarationList.declarations.some((declaration) => names.has(declaration.name.getText(script))))
  .map((statement) => statement.getText(script)).join('\n')
const testSource = `<template>${formNode[0]}</template><script setup lang="ts">
import { defineAsyncComponent, ref, nextTick } from 'vue'
${declarations.replaceAll("import('./components/DirectoryForm.vue')", '__loadDirectoryForm()')}
const getList = () => {}
defineExpose({ openForm })
</script>`
const compiled = compileScript(parse(testSource).descriptor, { id: 'directory-first-open', inlineTemplate: true })
const javascript = ts.transpileModule(compiled.content, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText

const renderer = vue.createRenderer({
  createElement: (type) => ({ type, children: [], parent: null }),
  createText: (text) => ({ text, parent: null }),
  createComment: (text) => ({ text, parent: null }),
  insert(node, parent, anchor) {
    if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1)
    node.parent = parent
    const index = anchor ? parent.children.indexOf(anchor) : -1
    if (index < 0) parent.children.push(node)
    else parent.children.splice(index, 0, node)
  },
  remove(node) {
    if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1)
    node.parent = null
  },
  setText: (node, text) => { node.text = text },
  setElementText: (node, text) => { node.text = text },
  patchProp: (node, key, previous, next) => { node[key] = next },
  parentNode: (node) => node.parent,
  nextSibling: (node) => node.parent?.children[node.parent.children.indexOf(node) + 1] || null
})

async function test() {
  const opened = [], errors = []
  const pendingImports = []
  const Form = vue.defineComponent({
    name: 'DirectoryForm',
    setup(_props, { expose }) {
      expose({ open: (type, payload) => { opened.push(JSON.parse(JSON.stringify({ type, payload }))) } })
      return () => vue.h('form')
    }
  })
  const context = {
    exports: {}, require: (id) => { assert.equal(id, 'vue'); return vue },
    // Separate imports may settle before Vue has completed the wrapper's load/mount.
    __loadDirectoryForm: () => new Promise((resolve) => pendingImports.push(resolve))
  }
  vm.runInNewContext(javascript, context, { filename })
  const app = renderer.createApp(context.exports.default)
  app.config.errorHandler = (error) => errors.push(error.message)
  const instance = app.mount({ children: [] })
  try {
    const opening = Promise.resolve(instance.openForm('create')).catch((error) => { errors.push(error.message) })
    await vue.nextTick()
    assert.equal(opened.length, 0, 'an unresolved form must not receive open calls')
    assert.ok(pendingImports.length > 0, 'the first request must trigger lazy loading')
    pendingImports.shift()({ __esModule: true, default: Form })
    for (let tick = 0; tick < 12; tick++) { await Promise.resolve(); await vue.nextTick() }
    await opening
    assert.deepEqual(errors, [], 'first opening must wait for the component instance, not only module import')
    assert.deepEqual(opened, [{ type: 'create', payload: { directories: [] } }], 'first click must open exactly once after mount')

    const row = { id: 42, name: 'DCC241016C12 unit parent' }
    await instance.openForm('create', row)
    assert.deepEqual(opened[1], { type: 'create', payload: { id: 42, parentId: 42, directories: [row] } }, 'child creation must keep its selected parent')
    await instance.openForm('update', row)
    assert.deepEqual(opened[2], { type: 'update', payload: { id: 42, directories: [row] } }, 'editing must keep its exact directory identity')
    assert.equal(opened.length, 3, 'already mounted forms must not replay pending requests')
    console.log('PASS: Vue delayed mount opens on first request; subsequent child-create/edit preserve context')
  } finally {
    app.unmount()
    for (const resolve of pendingImports) resolve({ __esModule: true, default: Form })
  }
}
test().catch((error) => { console.error(error); process.exitCode = 1 })
