const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file', file), 'utf8')
const descriptor = parse(read('detail/index.vue')).descriptor
const ast = ts.createSourceFile('detail.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = name => {
  const statement = ast.statements.find(node => ts.isVariableStatement(node)
    && node.declarationList.declarations.some(item => ts.isIdentifier(item.name) && item.name.text === name))
  assert.ok(statement, `actual declaration ${name}`)
  return statement.getText(ast)
}
const helpers = {}
vm.runInNewContext(ts.transpileModule(read('view/presentation.ts'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS }
}).outputText, { exports: helpers, URLSearchParams, String })
const display = {}
vm.runInNewContext(ts.transpileModule(read('shared/version-display.ts'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS }
}).outputText, { exports: display, String, Error, TypeError })
function navigation(query = {}, id = '9007199254740993') {
  const calls = [], messages = []
  const c = { exports: {}, String, Object, Error, decodeURIComponent, encodeURIComponent, URLSearchParams,
    computed: vue.computed, route: { path: `/dcc/controlled-file/detail/${id}`,
      fullPath: `/dcc/controlled-file/detail/${id}?viewer=1&from=${query.from || 'browser'}`, query: { viewer: '1', ...query } },
    controlledFileId: vue.ref(id), fileDetail: vue.ref({ id, fileName: '完整文件名称.pdf', currentActiveControlledFileId: '9007199254740999' }),
    isWorkingBrowserDetailCurrent: vue.ref(true), ...helpers,
    message: { error: value => messages.push(value) },
    router: { push: async value => calls.push(value), back: () => calls.push('back'),
      resolve: value => ({ matched: String(value).startsWith('/unknown') ? [] : [{ path: '/registered' }] }) }
  }
  vm.runInNewContext(ts.transpileModule([
    declaration('openViewerTraceability'), declaration('closeViewerMode'), declaration('isBrowserTraceabilityPage'),
    declaration('viewerIdentityError'),
    'exports.open=openViewerTraceability;exports.close=closeViewerMode;exports.readonly=isBrowserTraceabilityPage;'
  ].join('\n'), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return { c, calls, messages, ...c.exports }
}
const node = (type, text = '') => ({ type, text, props: {}, children: [], parent: null })
const renderer = vue.createRenderer({
  createElement: type => node(type), createText: text => node('#text', text), createComment: () => node('#comment'),
  patchProp: (n, key, old, value) => { n.props[key] = value },
  setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text; n.children = [] },
  insert: (n, p, anchor) => { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = p;
    const at = anchor ? p.children.indexOf(anchor) : -1; p.children.splice(at < 0 ? p.children.length : at, 0, n) },
  remove: n => { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1) },
  parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1]
})
const text = n => n.text + n.children.map(text).join('')
const nodes = (n, predicate) => [...(predicate(n) ? [n] : []), ...n.children.flatMap(child => nodes(child, predicate))]
function renderBranch(condition, detail, fresh = true) {
  const branch = descriptor.template.ast.children.find(n => n.type === 1 && n.props.some(p => p.type === 7
    && p.name === 'else-if' && p.exp?.content === condition))
  assert.ok(branch, `actual ${condition} page branch`)
  const template = branch.loc.source.replace(`v-else-if="${condition}"`, '')
  const compiled = compileScript(parse(`<template>${template}</template><script setup>
    import {computed,shallowRef} from 'vue';const fileDetail=shallowRef(__detail);const controlledFileId='9007199254740993';
    const isWorkingBrowserDetailCurrent=__fresh;const viewerMode=${condition === 'viewerMode'};
    const isCurrentActiveVersion=false;const canEditMetadata=false;const categoryNameMap=new Map();
    const directoryNameMap=new Map();const userNameMap=new Map();const deptNameMap=new Map();
    const openDccProjectCode=()=>{};const openMetadataDialog=()=>{};const reloadAll=()=>{};
    const closeViewerMode=()=>{};const openViewerTraceability=()=>{};
    const getDetailStatusLabel=value=>value;const getDetailStatusTagType=()=> 'info';
    const getDccVersionDisplayText=__version;
    ${declaration('viewerIdentityError')}
  </script>`).descriptor, { id: 'g10-real-branch', inlineTemplate: true })
  const c = { exports: {}, require: () => vue, __detail: detail, __fresh: fresh, __version: display.getDccVersionDisplayText }
  vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, c)
  const app = renderer.createApp(c.exports.default), mounted = []
  for (const name of ['ContentWrap', 'el-button', 'el-tag', 'el-alert', 'el-descriptions', 'el-descriptions-item', 'Icon'])
    app.component(name, { setup: (_props, ctx) => () => vue.h(name, ctx.attrs, ctx.slots.default?.()) })
  for (const name of ['ProtectedPdfViewer', 'ControlledFileBasicInfoPanel', 'ControlledFileBrowseRecords'])
    app.component(name, { props: ['file', 'controlledFileId'], setup: props => { mounted.push({ name, ...props }); return () => vue.h(name) } })
  const dom = node('root'); app.mount(dom)
  return { app, dom, mounted }
}

test('actual body renders only selected name, number, version, status and body with a detail entry', () => {
  const h = renderBranch('viewerMode', { id: '9007199254740993', fileName: '历史操作规程.pdf', title: '内部简称', fileNumber: 'SOP-10',
    versionNo: 'A/1', status: 'SUPERSEDED', processInstanceId: 'secret-round', remark: '完整记录备注' })
  try {
    for (const fact of ['历史操作规程.pdf', 'SOP-10', 'A/1', 'SUPERSEDED']) assert.ok(text(h.dom).includes(fact), fact)
    assert.equal(text(h.dom).includes('内部简称'), false)
    assert.ok(nodes(h.dom, n => n.props['data-testid'] === 'dcc-viewer-trace-entry').length)
    assert.deepEqual(h.mounted.map(item => item.name), ['ProtectedPdfViewer'])
    assert.equal(h.mounted[0].controlledFileId, '9007199254740993')
    assert.equal(text(h.dom).includes('完整记录备注'), false)
  } finally { h.app.unmount() }
})

test('actual readonly detail mounts full records for the exact version and never a body or metadata editor', () => {
  for (const fresh of [true, false]) {
    const file = { id: '9007199254740993', fileName: '所选历史正文.pdf', title: '内部简称', versionNo: 'A/1', status: 'SUPERSEDED' }
    const h = renderBranch('isBrowserTraceabilityPage', file, fresh)
    try {
      assert.deepEqual(h.mounted.map(item => item.name), fresh ? ['ControlledFileBasicInfoPanel', 'ControlledFileBrowseRecords'] : [])
      if (fresh) for (const item of h.mounted) assert.equal(item.file, file)
    } finally { h.app.unmount() }
  }
})

test('actual body hides a submitted working iteration while preserving its exact preview ID', () => {
  for (const status of ['WORKING', 'DRAFT', 'PENDING_SUPERVISOR_REVIEW', 'REJECTED']) {
    const h = renderBranch('viewerMode', { id: '9007199254740993', fileName: '精确工作稿.pdf', title: '内部简称', fileNumber: 'SOP-W', versionNo: 'A/1-2', status })
    try {
      assert.ok(text(h.dom).includes('草稿'))
      assert.equal(text(h.dom).includes('A/1-2'), false)
      assert.equal(h.mounted[0].controlledFileId, '9007199254740993')
    } finally { h.app.unmount() }
  }
})

test('list/search/approval/history navigation retains exact Long ID, source, process and task context', async () => {
  for (const from of ['browser', 'project-browser', 'approval-center', 'version-history']) {
    const h = navigation({ from, processInstanceId: 'historical-round', taskId: 'exact-task', applicationType: 'REVISION',
      handling: 'approval', controlledPrint: '1', returnTo: encodeURIComponent('/dcc/controlled-file/browser?q=SOP') })
    await h.open()
    const target = h.calls[0]
    assert.equal(target.path, '/dcc/controlled-file/detail/9007199254740993')
    assert.equal(target.query.from, from)
    assert.equal(target.query.processInstanceId, 'historical-round')
    assert.equal(target.query.taskId, 'exact-task')
    assert.equal(target.query.applicationType, 'REVISION')
    assert.equal(target.query.viewer, undefined)
    assert.equal(target.query.handling, undefined)
    assert.equal(target.query.controlledPrint, undefined)
    h.c.route.query = target.query
    assert.equal(h.readonly.value, true)
    await h.close()
    assert.equal(h.calls[1], h.c.route.fullPath)
  }
})

test('actual return preserves registered approval source and refuses external/unregistered/malformed destinations', () => {
  const source = '/approval-center?tab=done&processInstanceId=round'
  const h = navigation({ from: 'approval-center', returnTo: encodeURIComponent(source) })
  h.close(); assert.deepEqual(h.calls, [source])
  const withoutReturn = navigation({ from: 'approval-center' }); withoutReturn.close(); assert.deepEqual(withoutReturn.calls, ['back'])
  for (const target of ['https://example.com', '//example.com', '/unknown/path', '/\\example.com', '%invalid']) {
    const invalid = navigation({ returnTo: encodeURIComponent(target) }); invalid.close()
    assert.deepEqual(invalid.calls, []); assert.equal(invalid.messages.length, 1)
  }
})

test('stale, missing or wrong selected detail cannot navigate even when a different current version exists', async () => {
  const h = navigation(); h.c.isWorkingBrowserDetailCurrent.value = false; await h.open()
  h.c.isWorkingBrowserDetailCurrent.value = true; h.c.fileDetail.value = undefined; await h.open()
  h.c.fileDetail.value = { id: '9007199254740999' }; await h.open()
  assert.deepEqual(h.calls, [])
})

test('actual records select and lock the requested historical round instead of the file latest round', () => {
  const records = parse(read('detail/ControlledFileBrowseRecords.vue')).descriptor
  const recordsAst = ts.createSourceFile('records.ts', records.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const statements = ['hasRequestedBpmRound', 'selectedBpmRound'].map(name => recordsAst.statements.find(n =>
    ts.isVariableStatement(n) && n.declarationList.declarations.some(d => d.name.text === name))?.getText(recordsAst) || '')
  const find = n => n.type === 1 && n.tag === 'DetailApplicationHistory' ? n : (n.children || []).map(find).find(Boolean)
  const history = find(records.template.ast)
  for (const query of [{ processInstanceId: 'exact-old-round' }, {}]) {
    const compiled = compileScript(parse(`<template>${history.loc.source}</template><script setup>
      import {computed} from 'vue';const route=__route;const props=__fixture;const file=props.file;const contextKey='exact-view';
      const hasRelationApplicationContext=true;const relationApplicationContextKey=contextKey;const onApplicationSelectionChange=()=>{};
      ${statements.join('\n')}
    </script>`).descriptor, { id: 'actual-history-round', inlineTemplate: true })
    const c = { exports: {}, require: () => vue, __route: { query },
      __fixture: { file: { id: '9007199254740993', processInstanceId: 'newer-round' } } }
    vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, c)
    let seen
    const app = renderer.createApp(c.exports.default)
    app.component('DetailApplicationHistory', { props: ['fileId', 'primaryBpmRound', 'lockPrimaryRound', 'contextKey'],
      setup: props => { seen = { ...props }; return () => vue.h('history') } })
    app.mount(node('root'))
    try {
      assert.equal(seen.fileId, '9007199254740993')
      assert.equal(seen.primaryBpmRound, query.processInstanceId || 'newer-round')
      assert.equal(Boolean(seen.lockPrimaryRound), Boolean(query.processInstanceId))
      assert.equal(seen.contextKey, 'exact-view')
    } finally { app.unmount() }
  }
})

test('actual body and full records do not mount after a denied or stale selected detail read', () => {
  for (const mode of ['viewerMode', 'isBrowserTraceabilityPage']) {
    const denied = renderBranch(mode, undefined, false)
    try { assert.deepEqual(denied.mounted, []) } finally { denied.app.unmount() }
  }
})

test('missing exact file name is visible as a contract error without substituting a title or mounting body', () => {
  const h = renderBranch('viewerMode', { id: '9007199254740993', title: '不能作为完整名称的简称', versionNo: 'A/1', status: 'ACTIVE' })
  try {
    assert.deepEqual(h.mounted, [])
    assert.equal(text(h.dom).includes('不能作为完整名称的简称'), false)
    assert.ok(nodes(h.dom, n => n.type === 'el-alert' && String(n.props.title).includes('完整名称未返回')).length)
  } finally { h.app.unmount() }
})
