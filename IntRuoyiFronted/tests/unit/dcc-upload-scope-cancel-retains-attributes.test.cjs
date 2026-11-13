const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const sfc = require('vue/compiler-sfc')
const base = 'src/views/dcc/controlled-file/'
const transpile = code => ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText

test('cancelling either library scope keeps the mounted project panel and its edited snapshot', async () => {
  const upload = sfc.parse(fs.readFileSync(base + 'upload/index.vue', 'utf8')).descriptor
  const find = node => node.type === 1 && node.tag === 'el-form-item' && node.props.some(p => p.name === 'label' && p.value?.content === '申请属性')
    ? node : (node.children || []).map(find).find(Boolean)
  const attributes = find(upload.template.ast)
  assert.ok(attributes)
  const state = {}
  new Function('exports', transpile(fs.readFileSync(base + 'project-attributes/state.ts', 'utf8')))(state)
  let reads = 0
  const defaults = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
  const childExports = {}
  const child = sfc.compileScript(sfc.parse(fs.readFileSync(base + 'project-attributes/ProjectApplicationAttributes.vue', 'utf8')).descriptor, { id: 'attributes' })
  new Function('exports', 'require', transpile(child.content))(childExports, id => {
    if (id === 'vue') return vue
    if (id === 'element-plus') return { ElMessageBox: { confirm: async () => {} } }
    if (id === '@/api/dcc/controlledFile/projectAttributes') return { getProjectDefaults: async () => { reads++; return defaults } }
    if (id === './state') return state
    if (id.endsWith('.vue')) return { default: {} }
    throw Error(id)
  })
  childExports.default.render = () => vue.h('div')
  const scope = vue.ref(false), panel = vue.ref()
  const source = `<template>${attributes.loc.source}</template><script setup>
    const isExternalReview = false, isLibraryUpload = __scope, projectAttributesPanel = __panel;
    const projectAttributesPanelKey = 0, projectAttributesRestoreSnapshot = undefined, submitLoading = false;
    const captureProjectAttributes = () => {};
  </script>`
  const parent = sfc.compileScript(sfc.parse(source).descriptor, { id: 'scope', inlineTemplate: true })
  const parentExports = {}
  new Function('exports', 'require', '__scope', '__panel', transpile(parent.content))(parentExports, () => vue, scope, panel)
  const renderer = vue.createRenderer({
    createElement: () => ({ children: [], style: {} }), createText: text => ({ text }), createComment: () => ({}),
    insert: (node, parent) => parent.children.push(node), remove() {}, setText() {}, setElementText() {}, patchProp() {}, parentNode: () => null, nextSibling: () => null
  })
  const app = renderer.createApp(parentExports.default)
  app.component('el-form-item', { setup: (_, ctx) => () => vue.h('section', ctx.slots.default?.()) })
  app.component('ProjectApplicationAttributes', childExports.default)
  app.mount({ children: [] })
  try {
    await panel.value.selectProject('1111')
    panel.value.$.setupState.edit({ ...defaults, licenseHolder: 'N', documentTransfer: 'Y', transferTo: '测试工厂' })
    const original = panel.value, snapshot = original.getSnapshot()
    for (const library of ['QMS', 'GENERAL']) {
      scope.value = true; await vue.nextTick()
      assert.equal(panel.value, original, `${library} confirmation must retain the panel`)
      scope.value = false; await vue.nextTick()
      assert.deepEqual(panel.value.getSnapshot(), snapshot)
    }
    assert.equal(reads, 1, 'cancellation must not reload defaults over user edits')
  } finally { app.unmount() }
})
