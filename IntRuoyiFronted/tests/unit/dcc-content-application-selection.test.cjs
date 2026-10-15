const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail', file), 'utf8')
function loadContract() {
  const exports = {}
  vm.runInNewContext(ts.transpileModule(read('relation-contract.ts'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText,
    { exports, require: id => { if (id.endsWith('project-reference-contract')) return {}; throw Error(id) }, Error, String, BigInt, Number })
  return exports
}
const contract = loadContract()
const find = (n, tag) => n.type === 1 && n.tag === tag ? n : (n.children || []).map(child => find(child, tag)).find(Boolean)
const findError = n => n.type === 1 && n.tag === 'el-alert' && n.props.some(p => p.type === 7 && p.name === 'if'
  && p.exp?.content === 'relationApplicationSelectionError') ? n : (n.children || []).map(findError).find(Boolean)
const selection = (round = 'round-A', id = '9007199254740993') => ({ controlledFileId: id, applicationType: 'REVISION', bpmRound: round, attributeRound: 2 })
const renderer = vue.createRenderer({
  createElement: type => ({ type, children: [], props: {}, parent: null }), createText: text => ({ text }), createComment: () => ({}),
  insert: (n, p, anchor) => { if(n.parent)n.parent.children.splice(n.parent.children.indexOf(n),1); n.parent=p;
    const index=anchor?p.children.indexOf(anchor):-1;p.children.splice(index<0?p.children.length:index,0,n) },
  remove: n => { if(n?.parent)n.parent.children.splice(n.parent.children.indexOf(n),1) },
  patchProp: (n, k, old, value) => { n.props[k] = value }, setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text },
  parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n)+1]
})
function mount(file, versionOnly = false) {
  const descriptor = parse(read(file)).descriptor
  const ast = ts.createSourceFile(file, descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const names = ['selectedRelationApplication', 'relationApplicationSelectionError', 'relationApplicationGeneration', 'hasRelationApplicationContext',
    'relationApplicationContextKey', 'relationApplicationSelection', 'onApplicationSelectionChange', 'stopRelationApplicationContext']
  const declarations = names.map(name => ast.statements.find(s => ts.isVariableStatement(s)
    && s.declarationList.declarations.some(d => d.name.text === name))?.getText(ast) || '')
  const templateRoot = file === 'index.vue' ? descriptor.template.ast.children.find(n => n.type === 1 && n.tag === 'template') : descriptor.template.ast
  const history = find(templateRoot, 'DetailApplicationHistory'), relations = find(templateRoot, 'DetailRelationsPanel')
  assert.ok(history); assert.ok(relations)
  const data = vue.reactive({ file: { id: '9007199254740993', masterId: '100', dccProjectCodeId: '20',
    processInstanceId: versionOnly ? undefined : 'round-A' }, route: { query: {}, fullPath: '/exact-A' },
    context: 'context-A', fresh: true, sequence: 1, errors: [] })
  const compiled = compileScript(parse(`<template>${history.loc.source}${findError(templateRoot)?.loc.source || ''}${relations.loc.source}</template><script setup lang="ts">
    import {ref,computed,watch} from 'vue';
    const props=__data;const route=props.route;const file=computed(()=>props.file);const fileDetail=file;
    const controlledFileId=computed(()=>String(props.file.id));const isWorkingBrowserDetailCurrent=computed(()=>props.fresh);
    const workingBrowserLoadedContext=computed(()=>({sequence:props.sequence}));
    const contextKey=computed(()=>props.context);const applicationRoundContextKey=contextKey;
    const selectedBpmRound=computed(()=>props.route.query.processInstanceId||props.file.processInstanceId);
    const hasRequestedBpmRound=computed(()=>!!props.route.query.processInstanceId);
    const applicationRoundSelection=computed(()=>({primaryBpmRound:selectedBpmRound.value,blockedReason:'',lockPrimaryRound:hasRequestedBpmRound.value}));
    const showDetailManagementActions=false;const isExternalReviewProcess=false;
    const assertRelationApplicationSelection=__assert;const message={error:v=>props.errors.push(v)};
    ${declarations.join('\n')}
  </script>`).descriptor, { id: `real-${file}`, inlineTemplate: true })
  const c = { exports: {}, require: () => vue, __data: data, __assert: contract.assertRelationApplicationSelection }
  vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, c)
  let emit, callback, historyProps, relationProps
  const app = renderer.createApp(c.exports.default)
  app.component('DetailApplicationHistory', { props: ['fileId', 'contextKey'], emits: ['selection-change'],
    setup: (props, ctx) => { emit = value => ctx.emit('selection-change', value); historyProps = props;
      callback = vue.getCurrentInstance().vnode.props.onSelectionChange; return () => vue.h('history') } })
  app.component('DetailRelationsPanel', { props: ['applicationSelection'], setup: props => { relationProps=props; return () => vue.h('relations') } })
  app.component('el-alert', { props: ['title'], setup: props => () => vue.h('alert', { title: props.title }) })
  const dom={children:[]};app.mount(dom)
  const errors = n => [...(n.type==='alert'?[n.props.title]:[]),...(n.children||[]).flatMap(errors)]
  return { app, data, relation: () => relationProps.applicationSelection, emit: value => emit(value), oldEmitter: () => callback,
    errors: () => errors(dom), history: () => historyProps }
}
for (const file of ['index.vue', 'ControlledFileBrowseRecords.vue']) {
  test(`${file} actual History event binds exact verified selection and synchronous null to relations`, async () => {
    const h = mount(file)
    try {
      assert.equal(h.relation(), null)
      h.emit(selection()); await vue.nextTick()
      assert.deepEqual(JSON.parse(JSON.stringify(h.relation())), selection())
      h.emit(null); await vue.nextTick(); assert.equal(h.relation(), null)
      h.emit(selection('round-B')); await vue.nextTick(); assert.equal(h.relation().bpmRound, 'round-B')
      h.emit(null); await vue.nextTick(); assert.equal(h.relation(), null)
    } finally { h.app.unmount() }
  })
  test(`${file} actual context reset rejects late previous events and wrong file identities`, async () => {
    const h = mount(file)
    try {
      h.emit(selection()); await vue.nextTick(); const old = h.oldEmitter()
      h.data.context = 'context-B'; h.data.route.fullPath = '/exact-B'; await vue.nextTick()
      assert.equal(h.relation(), null)
      old(selection('late-old')); await vue.nextTick(); assert.equal(h.relation(), null)
      h.emit(selection('foreign', '9007199254740995')); await vue.nextTick(); assert.equal(h.relation(), null)
      assert.ok(h.errors().some(value=>value.includes('正式申请轮次')))
      h.emit(selection('round-C')); await vue.nextTick(); assert.equal(h.relation().bpmRound, 'round-C')
      h.data.file = { ...h.data.file }; h.data.sequence++; await vue.nextTick(); assert.equal(h.relation(), null)
    } finally { h.app.unmount() }
  })
  test(`${file} explicit version-only view passes undefined and cannot reuse an application selection`, async () => {
    const h = mount(file, true)
    try { assert.equal(h.relation(), undefined) } finally { h.app.unmount() }
  })
  test(`${file} actual locked context refuses another BPM and invalid attribute identity`, async () => {
    const h=mount(file)
    try {
      h.data.route.query={processInstanceId:'locked-round'};h.data.route.fullPath='/locked';h.data.context='locked';await vue.nextTick()
      h.emit(selection('wrong-round'));await vue.nextTick();assert.equal(h.relation(),null)
      assert.ok(h.errors().some(value=>value.includes('流程轮次')||value.includes('办理轮次')))
      h.emit({...selection('locked-round'),attributeRound:0});await vue.nextTick();assert.equal(h.relation(),null)
      h.emit(selection('locked-round'));await vue.nextTick();assert.equal(h.relation().bpmRound,'locked-round')
    }finally{h.app.unmount()}
  })
}
