const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const panel = parse(fs.readFileSync('src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue', 'utf8')).descriptor
const table = parse(fs.readFileSync('src/views/dcc/controlled-file/browser/ProjectFolderFileTable.vue', 'utf8')).descriptor
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
function host(scope, edit) {
  const ast = ts.createSourceFile('panel.ts', panel.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const names = ['canManageLibraryFile', 'openOperation', 'openOperationRow']
  const code = ast.statements.filter(n => n.declarationList?.declarations.some(d => names.includes(d.name.text))).map(n => n.getText(ast)).join('\n')
  const calls = [], c = { exports: {}, props: { libraryScope: scope }, libraryCanEdit: vue.ref(edit),
    state: { error: '' }, route: { fullPath: '/dcc/controlled-file/browser?mode=' + scope },
    referenceIdentity: id => id, router: { push: async route => calls.push(route) } }
  vm.runInNewContext(transpile(code + '\nexports.allowed=canManageLibraryFile;exports.open=openOperationRow'), c)
  return { ...c.exports, c, calls }
}
test('QMS and GENERAL require both library and exact file edit grants and eligible lifecycle state', async () => {
  for (const scope of ['QMS', 'GENERAL']) {
    const h = host(scope, true), file = { controlledFileId: '9223372036854775000', canEditFile: true, status: 'ACTIVE' }
    await h.open(file)
    assert.equal(h.calls[0].path, '/dcc/controlled-file/detail/' + file.controlledFileId)
    assert.equal(h.calls[0].query.management, '1')
    assert.equal(h.calls[0].query.returnTo, h.c.route.fullPath)
    for (const invalid of [{ ...file, canEditFile: false }, { ...file, canEditFile: undefined },
      ...['OBSOLETE', 'SUPERSEDED', 'PENDING_MATRIX_APPROVAL'].map(status => ({ ...file, status }))]) {
      assert.equal(h.allowed(invalid), false)
      await h.open(invalid)
      assert.ok(h.c.state.error)
    }
    h.c.libraryCanEdit.value = false
    await h.open(file)
    assert.equal(h.calls.length, 1)
  }
})
test('actual table operation entry renders for an editable library file and remains hidden for viewers', () => {
  const find = node => node.type === 1 && node.tag === 'el-button' && node.loc.source.includes("emit('operation'")
    ? node : (node.children || []).map(find).find(Boolean)
  const button = find(table.template.ast)
  assert.ok(button)
  const compiled = compileScript(parse('<template>' + button.loc.source + '</template><script setup>const {row,readOnly,canManage,loading,emit}=__host;</script>').descriptor,
    { id: 'library-file-operation', inlineTemplate: true })
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [], props: {} }), createText: text => ({ text }), createComment: () => ({}),
    insert: (n, p) => p.children.push(n), remove() {}, setText() {}, setElementText: (n, text) => { n.text = text }, patchProp: (n, key, old, value) => { n.props[key] = value }, parentNode: () => null, nextSibling: () => null })
  for (const editable of [true, false]) {
    const h = host('QMS', editable), file = { controlledFileId: '500', canEditFile: true, status: 'ACTIVE' }, events = []
    const c = { exports: {}, require: () => vue, __host: { row: { file }, readOnly: true, canManage: h.allowed, loading: false, emit: (...args) => events.push(args) } }
    vm.runInNewContext(transpile(compiled.content), c)
    const app = renderer.createApp(c.exports.default)
    app.component('el-button', { setup: (_p, ctx) => () => vue.h('button', ctx.attrs, ctx.slots.default?.()) })
    const root = { children: [] }; app.mount(root)
    try {
      const buttons = root.children.filter(node => node.type === 'button')
      assert.equal(buttons.length, editable ? 1 : 0)
      if (editable) { buttons[0].props.onClick(); assert.equal(events[0][0], 'operation'); assert.equal(events[0][1], file) }
    } finally { app.unmount() }
  }
  assert.match(panel.template.content, /:can-manage="libraryScope \? canManageLibraryFile : undefined"/)
})
