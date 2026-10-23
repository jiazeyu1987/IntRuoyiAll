const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const directory = path.join(root, 'src/views/dcc/controlled-file/workbench')
const compile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const helper = {}; new Function('exports', compile(fs.readFileSync(path.join(directory, '../shared/version-display.ts'), 'utf8')))(helper)
const presentation = {}
new Function('exports', 'require', compile(fs.readFileSync(path.join(directory, 'presentation.ts'), 'utf8')))(presentation, name => {
  if (name === '../shared/version-display') return helper
  if (name === '../shared/lifecycle' || name === '../shared/handlingSummary' || name === '@/utils/formatTime') return {}
  throw Error(name)
})
const first = Object.freeze({ controlledFileId: '9223372036854775701', versionNo: 'A/1-1', status: 'WORKING' })
const second = Object.freeze({ controlledFileId: '9223372036854775702', versionNo: 'A/1-2', status: 'WORKING' })

test('real workbench impact presentation labels each working candidate without changing raw identity or comparison facts', () => {
  const labels = [first, second].map(presentation.getWorkbenchImpactVersionLabel)
  assert.equal(labels[0], `草稿 · 文件 #${first.controlledFileId}`)
  assert.equal(labels[1], `草稿 · 文件 #${second.controlledFileId}`)
  assert.doesNotMatch(labels.join(' '), /A\/1-[12]/)
  assert.equal(first.versionNo, 'A/1-1'); assert.equal(second.versionNo, 'A/1-2')
  assert.equal(second.controlledFileId, '9223372036854775702')
})

test('impact presentation preserves the formal version text exactly', () => {
  const option = Object.freeze({ controlledFileId: first.controlledFileId, versionNo: 'AA/2', status: 'ACTIVE' })
  assert.equal(presentation.getWorkbenchImpactVersionLabel(option), 'AA/2')
})

test('actual impact radio displays draft while both controls and user selection preserve the exact original IDs', async () => {
  const { descriptor } = parse(fs.readFileSync(path.join(directory, 'index.vue'), 'utf8'))
  const find = n => n.type === 1 && n.tag === 'el-radio-group' && n.loc.source.includes('revisionDialog.selectedRevisionControlledFileId') ? n : (n.children || []).map(find).find(Boolean)
  const radio = find(descriptor.template.ast); assert.ok(radio)
  const fixture = { selectedRevisionControlledFileId: second.controlledFileId, options: { sourceIterations: [first, second], openMajorRevision: null } }
  const source = `<template>${radio.loc.source}</template><script setup>const revisionDialog=fixture;const controlledFileStatusLabel=s=>s;const getWorkbenchImpactVersionLabel=format;</script>`
  const script = compileScript(parse(source).descriptor, { id: 'a17-original-radio', inlineTemplate: true })
  const exports = {}; new Function('exports', 'require', 'fixture', 'format', compile(script.content))(exports, () => vue, fixture, presentation.getWorkbenchImpactVersionLabel)
  const node = type => ({ type, text: '', children: [], props: {}, parent: null })
  const renderer = vue.createRenderer({ createElement: node, createText: text => ({ ...node('#text'), text }), createComment: () => node('#comment'), insert: (n, p, anchor) => { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = p; const index = p.children.indexOf(anchor); index < 0 ? p.children.push(n) : p.children.splice(index, 0, n) }, remove: n => { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = null }, setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text; n.children = [] }, patchProp: (n, key, _old, value) => { n.props[key] = value }, parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] || null })
  const app = renderer.createApp(exports.default), target = node('root')
  app.config.warnHandler = () => {}
  for (const tag of ['el-radio-group', 'el-radio', 'el-tag']) app.component(tag, { setup: (_, ctx) => () => vue.h(tag, ctx.attrs, ctx.slots.default?.()) })
  app.mount(target)
  const text = n => n.text + n.children.map(text).join('')
  const nodes = (n, type) => [...(n.type === type ? [n] : []), ...n.children.flatMap(child => nodes(child, type))]
  try {
    assert.match(text(target), /草稿/)
    assert.doesNotMatch(text(target), /A\/1-[12]/)
    assert.equal(fixture.selectedRevisionControlledFileId, second.controlledFileId)
    const radios = nodes(target, 'el-radio')
    assert.deepEqual(radios.map(n => n.props.label), [first.controlledFileId, second.controlledFileId])
    const group = nodes(target, 'el-radio-group')[0]
    group.props['onUpdate:modelValue'](first.controlledFileId); await vue.nextTick()
    assert.equal(fixture.selectedRevisionControlledFileId, first.controlledFileId)
    group.props['onUpdate:modelValue'](second.controlledFileId); await vue.nextTick()
    assert.equal(fixture.selectedRevisionControlledFileId, second.controlledFileId)
    assert.equal(first.versionNo, 'A/1-1'); assert.equal(second.versionNo, 'A/1-2')
  } finally { app.unmount() }
})

test('current full workbench script imports the formatter and its template compiles with real bindings', () => {
  const filename = path.join(directory, 'index.vue')
  const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: filename })
  assert.ok(script.bindings.getWorkbenchImpactVersionLabel)
  const template = compileTemplate({ filename, id: filename, source: descriptor.template.content, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
})
