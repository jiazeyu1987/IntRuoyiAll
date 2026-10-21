const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript'), vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const filename = 'src/views/dcc/controlled-file/browser/index.vue'
const compile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText

function mountEntry(overrides = {}, latest = true) {
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
  const nodes = []
  const walk = node => {
    if (node.type === 1 && node.props.some(p => p.type === 6 && p.name === 'data-testid' && p.value?.content === 'dcc-working-type-storage-recovery-open')) nodes.push(node.loc.source)
    for (const child of node.children || []) walk(child)
  }
  walk(descriptor.template.ast)
  for (const fragment of nodes) assert.match(fragment, /v-hasPermi="\['dcc:controlled-file:submit'\]"/)
  const source = ts.createSourceFile('browser.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const names = ['canEditVersion', 'canSubmitWorkingIteration', 'canOpenWorkingTypeStorageRecovery']
  const declarations = names.flatMap(name => source.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(source) === name)).map(s => s.getText(source)))
  const file = { id: '2054545668044084206', masterId: '2054545668044079175', requesterId: '99', status: 'WORKING', changeType: 'NEW', checkedOut: false, actionProjection: { actionLocked: true, pendingRequestId: null }, ...overrides }
  const opened = []
  const context = { exports: {}, userStore: { getUser: { id: '99' } }, isLatestVersionSelected: () => latest,
    getSelectedVersion: () => file, Boolean, String }
  vm.runInNewContext(compile(declarations.join('\n') + '\nexports.eligible = typeof canOpenWorkingTypeStorageRecovery === "function" ? canOpenWorkingTypeStorageRecovery : () => false'), context)
  const values = { row: {}, getSelectedVersion: () => file, canOpenWorkingTypeStorageRecovery: context.exports.eligible,
    openWorkingTypeStorageRecovery: target => opened.push(target.id) }
  const script = Object.keys(values).map(key => `const ${key}=values.${key}`).join(';')
  const compiled = compileScript(parse(`<template><div>${nodes.join('')}</div></template><script setup>const values=__values;${script}</script>`).descriptor, { id: 'c10-recovery-entry', inlineTemplate: true })
  const exported = {}; new Function('exports', 'require', '__values', compile(compiled.content))(exported, () => vue, values)
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [], props: {} }), createText: text => ({ text }), createComment: () => ({}),
    insert: (n, p) => p.children.push(n), remove() {}, setText: (n, t) => n.text = t, setElementText: (n, t) => n.text = t,
    patchProp: (n, k, _old, value) => n.props[k] = value, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp(exported.default); app.config.warnHandler = () => {}; app.directive('hasPermi', {})
  app.component('el-button', { setup: (_, c) => () => vue.h('button', c.attrs, c.slots.default?.()) })
  const root = { children: [] }; app.mount(root)
  const find = n => n.props?.['data-testid'] === 'dcc-working-type-storage-recovery-open' ? n : (n.children || []).map(find).find(Boolean)
  return { app, button: find(root), opened }
}

test('actual browser row renders an independent recovery entry and passes the exact selected file', () => {
  const h = mountEntry()
  try { assert.ok(h.button, 'Original working object needs an entry independent of its failing detail'); h.button.props.onClick(); assert.deepEqual(h.opened, ['2054545668044084206']) }
  finally { h.app.unmount() }
})

test('actual recovery entry excludes other requester, submitted, checked-out, revision and formal versions', () => {
  for (const changed of [{ requesterId: '98' }, { processInstanceId: 'bpm' }, { checkedOut: true }, { checkedOutBy: '99' },
    { changeType: 'REVISION' }, { status: 'ACTIVE' }, { actionProjection: undefined }, { actionProjection: { actionLocked: true, pendingRequestId: 'pending' } }]) {
    const h = mountEntry(changed)
    try { assert.equal(h.button, undefined); assert.equal(h.opened.length, 0) } finally { h.app.unmount() }
  }
  const historical = mountEntry({}, false)
  try { assert.equal(historical.button, undefined); assert.equal(historical.opened.length, 0) } finally { historical.app.unmount() }
})
