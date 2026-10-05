const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const read = p => fs.readFileSync(p, 'utf8')
const compile = s => ts.transpileModule(s, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
function moduleFrom(source, dependencies = {}) {
  const context = { exports: {}, require: id => { assert.ok(id in dependencies, id); return dependencies[id] } }
  vm.runInNewContext(compile(source), context)
  return context.exports
}
const projection = moduleFrom(read('src/api/form-center/actionProjection.ts'))
const lifecycle = moduleFrom(read('src/views/dcc/controlled-file/shared/lifecycle.ts'), { '@/api/form-center/actionProjection': projection })
const presentation = moduleFrom(read('src/views/dcc/controlled-file/browser/presentation.ts'), { '../shared/lifecycle': lifecycle, '@/utils/formatTime': { formatDateTimeValue: (_value, empty) => empty } })
const { descriptor } = parse(read('src/views/dcc/controlled-file/browser/index.vue'))
const ast = ts.createSourceFile('browser.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const getter = ast.statements.find(n => ts.isVariableStatement(n) && n.declarationList.declarations.some(d => ts.isIdentifier(d.name) && d.name.text === 'getBrowserCurrentActiveRowSummary'))
assert.ok(getter)
const working = { id: '9007199254740993', versionNo: 'A/1-1', status: 'WORKING', currentActiveVersionNo: 'A/1' }
const row = { id: '9007199254740992', directoryId: 4, currentActiveVersionNo: 'A/1', selectedVersionId: working.id, versionHistory: [working] }
function rowSummary() {
  const c = { exports: {}, ...presentation, getSelectedVersion: r => r.versionHistory.find(v => v.id === r.selectedVersionId), getBrowserDirectoryPath: () => '质量管理', isLatestVersionSelected: () => false, isSelectedVersionModifying: () => false }
  vm.runInNewContext(compile(getter.getText(ast) + '\nexports.get=getBrowserCurrentActiveRowSummary;'), c)
  return c.exports.get
}
test('actual presentation classifies selected working/inflight/pending/history and requires formal master identity for current execution', () => {
  const summarize = (status, versionNo = 'A/2', current = 'A/1') => presentation.getBrowserVersionSummary({ status, versionNo }, false, current)
  assert.equal(summarize('WORKING', 'A/1-1').versionKindText, '工作小版本')
  assert.equal(summarize('PENDING_MATRIX_REVIEW').versionKindText, '在途版本')
  assert.equal(summarize('CONTROLLED_PENDING_EFFECTIVE').versionKindText, '受控（待生效）')
  assert.equal(summarize('OBSOLETE').versionKindText, '历史已作废')
  assert.equal(summarize('SUPERSEDED').versionKindText, '历史已替换')
  assert.equal(summarize('ACTIVE', 'A/1').versionKindText, '当前执行受控版本')
  assert.equal(summarize('ACTIVE', 'A/1').isCurrentActiveVersion, true)
  assert.equal(summarize('ACTIVE', 'A/2').isCurrentActiveVersion, false)
  assert.equal(summarize('ACTIVE', 'A/2').versionKindText, '受控版本（非当前执行）')
  assert.equal(summarize('ACTIVE', 'A/1', null).isCurrentActiveVersion, false)
  assert.equal(summarize('ACTIVE', 'A/1', null).versionKindText, '受控版本（执行身份未记录）')
  assert.equal(presentation.getBrowserVersionSummary({ status: 'ACTIVE', versionNo: 'A/1', currentActiveVersionNo: 'A/1' }, false).isCurrentActiveVersion, false)
  assert.equal(summarize(undefined).versionKindText, '版本属性未记录')
  assert.match(summarize('WORKING', 'A/1-1').currentVersionSourceText, /master 当前执行版本 A\/1/)
  assert.match(summarize('ACTIVE', 'A/1', null).currentVersionSourceText, /未记录/)
})
test('actual browser row summary keeps selected version separate from master current execution without changing identity', () => {
  const summary = rowSummary()(row)
  assert.equal(summary.versionNo, 'A/1-1')
  assert.equal(summary.versionKindText, '工作小版本')
  assert.match(summary.currentVersionSource, /master 当前执行版本 A\/1/)
  assert.equal(row.selectedVersionId, working.id)
  assert.equal(row.currentActiveVersionNo, 'A/1')
})
test('both actual name and number metadata templates render working instead of controlled and expose master current version', () => {
  const nodes = []
  const walk = node => { if (node.type === 1 && node.props.some(p => p.type === 6 && p.name === 'data-testid' && ['dcc-browser-current-active-row-summary', 'dcc-browser-file-number-current-active-summary'].includes(p.value?.content))) nodes.push(node); for (const child of node.children || []) walk(child) }
  walk(descriptor.template.ast); assert.equal(nodes.length, 2)
  const remove = n => { if (n?.parent) { const siblings = n.parent.children; siblings.splice(siblings.indexOf(n), 1); n.parent = null } }
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: () => ({}), insert: (n, p, anchor) => { remove(n); n.parent = p; const index = p.children.indexOf(anchor); if (index < 0) p.children.push(n); else p.children.splice(index, 0, n) }, remove, setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text }, patchProp() {}, parentNode: n => n?.parent || null, nextSibling: n => n?.parent?.children[n.parent.children.indexOf(n) + 1] || null })
  for (const node of nodes) {
    const compiled = compileScript(parse('<template>' + node.loc.source + '</template><script setup>const row=__row;const getBrowserCurrentActiveRowSummary=__get;</script>').descriptor, { id: 'actual-browser-version-summary', inlineTemplate: true })
    const c = { exports: {}, require: () => vue, __row: row, __get: rowSummary() }
    vm.runInNewContext(compile(compiled.content), c)
    const app = renderer.createApp(c.exports.default)
    app.component('el-tag', { setup: (_, { slots }) => () => vue.h('span', slots.default?.()) })
    const root = { children: [] }; app.mount(root)
    const text = n => (n.text || '') + (n.children || []).map(text).join('')
    try { const actual = text(root); assert.match(actual, /工作小版本/); assert.doesNotMatch(actual, /历史版/); assert.match(actual, /版本号：A\/1-1/); assert.match(actual, /master 当前执行版本 A\/1/) } finally { app.unmount() }
  }
})
