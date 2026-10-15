const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = name => fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file', name), 'utf8')
const compile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const display = {}; new Function('exports', compile(read('shared/version-display.ts')))(display)
const { descriptor } = parse(read('detail/index.vue'))
const ast = ts.createSourceFile('detail.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = name => {
  const result = ast.statements.find(s => s.declarationList?.declarations.some(d => d.name.text === name))
  assert.ok(result, name); return result.getText(ast)
}
const elements = []
const walk = n => { if (n.type === 1) elements.push(n); for (const child of n.children || []) walk(child) }
walk(descriptor.template.ast)
const attr = (n, name) => n.props.find(p => p.type === 6 && p.name === name)?.value?.content
const node = (type, text = '') => ({ type, text, children: [], props: {}, parent: null })
const renderer = vue.createRenderer({
  createElement: type => node(type), createText: text => node('#text', text), createComment: () => node('#comment'),
  patchProp: (n, key, _old, value) => { n.props[key] = value },
  setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text; n.children = [] },
  insert: (n, parent, anchor) => { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = parent; const i = parent.children.indexOf(anchor); i < 0 ? parent.children.push(n) : parent.children.splice(i, 0, n) },
  remove: n => { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = null },
  parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] || null
})
const text = n => n.text + n.children.map(text).join('')
function render(template, values) {
  const script = `const fixture=__fixture;${Object.keys(values).map(k => `const ${k}=fixture.${k};`).join('\n')}const getDccVersionDisplayText=__display;`
  const compiled = compileScript(parse(`<template>${template}</template><script setup>${script}</script>`).descriptor, { id: 'a12-actual-template', inlineTemplate: true })
  const exports = {}; new Function('exports', 'require', '__fixture', '__display', compile(compiled.content))(exports, () => vue, values, display.getDccVersionDisplayText)
  const target = node('root'), errors = [], app = renderer.createApp(exports.default)
  app.config.warnHandler = () => {}; app.config.errorHandler = e => errors.push(e)
  for (const tag of ['el-descriptions-item', 'el-tag']) app.component(tag, { setup: (_, ctx) => () => vue.h(tag, ctx.attrs, ctx.slots.default?.()) })
  app.component('el-table', { props: ['data'], setup: (p, { slots }) => { vue.provide('rows', p.data); return () => vue.h('table', {}, slots.default?.()) } })
  app.component('el-table-column', { setup: (_, ctx) => { const rows = vue.inject('rows'); return () => vue.h('column', ctx.attrs, rows.map(row => ctx.slots.default ? ctx.slots.default({ row }) : String(row[ctx.attrs.prop] ?? ''))) } })
  app.mount(target); return { target, errors, unmount: () => app.unmount() }
}
const run = (names, values, result) => {
  const exports = {}
  new Function('exports', ...Object.keys(values), 'getDccVersionDisplayText', compile(names.map(declaration).join('\n') + '\nexports.result=' + result))(exports, ...Object.values(values), display.getDccVersionDisplayText)
  return exports.result
}

test('actual generic header and basic version facts render only 草稿 while formal A/1 stays unchanged', () => {
  const fragments = elements.filter(n => (n.tag === 'span' && /fileDetail\?\.versionNo/.test(n.loc.source)) || (n.tag === 'el-descriptions-item' && ['版本号', '来源受控版本', '选用正文小版本'].includes(attr(n, 'label'))))
  assert.equal(fragments.length, 4)
  for (const versionNo of ['A/1-2', 'A/1']) {
    const fileDetail = Object.freeze({ id: '9223372036854775702', versionNo, revisionSourceVersionNo: 'A/1', selectedIterationVersionNo: versionNo })
    const h = render(fragments.map(n => n.loc.source).join(''), { fileDetail })
    try { assert.deepEqual(h.errors, []); if (versionNo.includes('-')) { assert.match(text(h.target), /草稿/); assert.doesNotMatch(text(h.target), /A\/1-2/) } else assert.match(text(h.target), /A\/1/); assert.equal(fileDetail.versionNo, versionNo) }
    finally { h.unmount() }
  }
})

test('all actual generic history, signature and print version columns render helper text, raw rows stay exact', () => {
  const columns = elements.filter(n => n.tag === 'el-table-column' && attr(n, 'prop') === 'versionNo')
  assert.equal(columns.length, 4)
  const rows = Object.freeze([Object.freeze({ id: '9223372036854775702', versionNo: 'A/1-2' }), Object.freeze({ id: '9223372036854775701', versionNo: 'A/1' })])
  const functions = { isVersionHistoryColumnVisible: () => true, getVersionHistoryColumnWidthString: () => '', isControlledPrintRecordColumnVisible: () => true, getControlledPrintRecordColumnWidthString: () => '', isDccSignatureEvidenceColumnVisible: () => true, getDccSignatureEvidenceColumnWidthString: () => '', sortColumnAttrs: () => ({}) }
  const h = render(`<el-table :data="rows">${columns.map(n => n.loc.source).join('')}</el-table>`, { rows, ...functions })
  try { assert.deepEqual(h.errors, []); assert.doesNotMatch(text(h.target), /A\/1-2/); assert.match(text(h.target), /草稿/); assert.match(text(h.target), /A\/1/); assert.equal(rows[0].versionNo, 'A/1-2') }
  finally { h.unmount() }
})

test('actual history source and selected-body fact template hides only structural version fields', () => {
  const column = elements.find(n => n.tag === 'el-table-column' && attr(n, 'label') === '本版变更事实')
  assert.ok(column)
  const row = Object.freeze({ id: '9223372036854775702', revisionSourceVersionNo: 'A/1', selectedIterationVersionNo: 'A/1-2', changeDescription: '原始变更说明保留' })
  const h = render(`<el-table :data="rows">${column.loc.source}</el-table>`, { rows: [row], revisionChangeLabel: () => '局部变更' })
  try { assert.deepEqual(h.errors, []); assert.match(text(h.target), /送审正文：草稿/); assert.doesNotMatch(text(h.target), /A\/1-2/); assert.match(text(h.target), /原始变更说明保留/); assert.equal(row.selectedIterationVersionNo, 'A/1-2') }
  finally { h.unmount() }
})

test('real history identity helper hides work counters without losing the exact file and BPM identity', () => {
  const identity = run(['getVersionHistoryIdentityText'], {}, 'getVersionHistoryIdentityText')
  const version = Object.freeze({ id: '9223372036854775702', versionNo: 'A/1-2', fileNumber: 'F1', title: '历史文件', processInstanceId: 'history-round' })
  assert.match(identity(version), /草稿/); assert.doesNotMatch(identity(version), /A\/1-2/)
  assert.match(identity(version), /9223372036854775702/); assert.match(identity(version), /history-round/); assert.equal(version.versionNo, 'A/1-2')
})

test('real signing result prompt shows draft while untouched response evidence remains bound to raw version', () => {
  const prompt = run(['buildActionSuccessMessage'], {}, 'buildActionSuccessMessage')
  const response = Object.freeze({ versionNo: 'A/1-2', evidenceHashShort: 'abc123' })
  assert.match(prompt('approve', response), /版本 草稿/); assert.doesNotMatch(prompt('approve', response), /A\/1-2/)
  assert.match(prompt('approve', response), /abc123/); assert.equal(response.versionNo, 'A/1-2')
  assert.match(prompt('approve', { versionNo: 'A/1', evidenceHashShort: 'def' }), /版本 A\/1/)
})

test('real printable version fields use draft while the process audit JSON remains byte-for-byte original', () => {
  const file = Object.freeze({ versionNo: 'A/1-2', title: '文件', fileNumber: 'F1' })
  const process = { rawVersion: 'A/1-2', processInstanceId: 'history-round' }
  const build = run(['escapeReceiptHtml', 'buildProcessPrintHtml'], { fileDetail: vue.ref(file) }, 'buildProcessPrintHtml')
  const html = build(process)
  assert.match(html, /<th>版本<\/th><td>草稿<\/td>/)
  assert.ok(html.includes('&quot;rawVersion&quot;: &quot;A/1-2&quot;'))
  assert.equal(process.rawVersion, 'A/1-2'); assert.equal(file.versionNo, 'A/1-2')
  const receipt = run(['escapeReceiptHtml', 'buildDistributionReceiptPrintHtml'], { buildDistributionReceiptRows: () => [{ versionNo: 'A/1-2' }] }, 'buildDistributionReceiptPrintHtml')
  assert.match(receipt(), /<td>草稿<\/td>/); assert.doesNotMatch(receipt(), /A\/1-2/)
})
