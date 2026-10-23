const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const base = path.resolve('src'), read = p => fs.readFileSync(path.join(base, p), 'utf8')
const clone = v => JSON.parse(JSON.stringify(v)), deferred = () => { let resolve; const promise = new Promise(r => resolve = r); return { promise, resolve } }
const tick = async () => { for (let n = 0; n < 22; n++) { await Promise.resolve(); await vue.nextTick() } }
const P = '9007199254740993', F = '9007199254740995', B = '9007199254740997', BF = '9007199254740999'
const project = (id, name) => ({ id, projectName: name, projectCode: name, status: 'ENABLE', projectLeaderUserId: null, projectLeader: null, associatedFileCount: 0 })
const folder = (id, p, name) => ({ id, projectCodeId: p, parentId: '0', name, sortOrder: 0, active: true })
const source = { contextKey: 'A:source', tenantId: '1', masterId: '10', projectId: P, folderId: F, projectName: '源项目A', folderName: '同名folder', fileName: '源.pdf', fileNumber: 'S', versionNo: 'A/1' }
const candidate = { tenantId: '1', controlledFileId: '51', masterId: '50', projectId: B, projectName: '候选B', folderName: '同名folder', fileName: '候选.pdf', fileNumber: 'T', versionNo: 'B/1', status: 'ACTIVE', controlled: true, pendingEffect: false, executable: true, canPreview: false }
function modules(transport) {
  const cache = new Map()
  const load = p => {
    if (cache.has(p)) return cache.get(p)
    const exports = {}
    const code = ts.transpileModule(read(p), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
    const resolve = id => {
      if (id === '@/config/axios') return { default: { get: transport } }
      if (id.startsWith('@/')) return load(id.slice(2) + '.ts')
      if (id.startsWith('.')) return load(path.posix.normalize(path.posix.join(path.posix.dirname(p), id)) + '.ts')
      throw Error(id)
    }
    new Function('exports', 'require', code)(exports, resolve); cache.set(p, exports); return exports
  }
  return { load }
}
const make = (type, text = '') => ({ type, text, children: [], props: {}, parent: null })
const renderer = vue.createRenderer({ createElement: type => make(type), createText: text => make('#text', text), createComment: text => make('#comment', text), setText: (n, t) => n.text = t, setElementText: (n, t) => { n.text = t; n.children = [] }, patchProp: (n, k, o, v) => n.props[k] = v, insert: (n, p, a) => { if (n.parent) { const i = n.parent.children.indexOf(n); if (i >= 0) n.parent.children.splice(i, 1) } n.parent = p; const i = a ? p.children.indexOf(a) : -1; i < 0 ? p.children.push(n) : p.children.splice(i, 0, n) }, remove: n => { if (n.parent) { const i = n.parent.children.indexOf(n); if (i >= 0) n.parent.children.splice(i, 1) } n.parent = null }, parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1], querySelector: () => null, setScopeId() {}, insertStaticContent: (s, p) => { const n = make('#static', s); p.children.push(n); n.parent = p; return [n, n] } })
const nodes = (n, p) => [...(p(n) ? [n] : []), ...n.children.flatMap(x => nodes(x, p))], content = n => n.text + n.children.map(content).join(''), find = (r, type, id) => nodes(r, n => n.type === type && (!id || n.props['data-testid'] === id))[0]
function mount(transport, overrides = {}) {
  const api = modules(transport), file = 'views/dcc/controlled-file/relations/DccFileSelector.vue', { descriptor } = parse(read(file)), code = compileScript(descriptor, { id: file, inlineTemplate: true }).content
  const exports = {}, imports = id => {
    if (id === 'vue') return vue
    if (id.endsWith('.vue')) return {}
    if (id.startsWith('.')) return api.load(path.posix.normalize(path.posix.join(path.posix.dirname(file), id)) + '.ts')
    if (id.startsWith('@/')) return api.load(id.slice(2) + '.ts')
    throw Error(id)
  }
  new Function('exports', 'require', ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText)(exports, imports)
  const queries = [], props = vue.reactive({ modelValue: false, source: clone(source), purpose: 'reference', directories: [], selected: [candidate], loadPage: async q => { queries.push(clone(q)); return { list: [], total: 0 } }, persist: async () => {}, openPreview: async () => {}, ...overrides })
  const root = make('root'), app = renderer.createApp({ render: () => vue.h(exports.default, props) })
  app.config.warnHandler = () => {}
  for (const type of ['el-dialog', 'el-alert', 'el-button', 'el-input', 'el-tree', 'el-tabs', 'el-tab-pane', 'el-pagination', 'el-tag', 'el-checkbox']) app.component(type, { inheritAttrs: false, setup: (_, c) => () => vue.h(type, c.attrs, [c.slots.default?.(), c.slots.footer?.()]) })
  app.component('el-table', { props: ['data'], setup: (p, c) => { vue.provide('rows', vue.toRef(p, 'data')); return () => vue.h('el-table', { ...c.attrs, data: p.data }, c.slots.default?.()) } })
  app.component('el-table-column', { inheritAttrs: false, setup: (_, c) => { const rows = vue.inject('rows'); return () => vue.h('el-table-column', c.attrs, c.slots.default ? rows.value.map(row => c.slots.default({ row })) : rows.value.map(row => String(row[c.attrs.prop] ?? ''))) } })
  app.directive('loading', {}); app.mount(root)
  return { app, props, queries, root, state: () => app._instance.subTree.component.setupState }
}
test('cross-project directory loader uses real server project pages/Long folders and invalidates closed responses', async () => {
  const calls = [], pending = deferred(), api = modules(async q => { calls.push(q); if (q.url === '/dcc/project-codes/page') return { list: [project(B, 'B')], total: 41 }; if (q.url.includes(B)) return pending.promise; return [folder(F, P, '同名folder')] })
  const { SelectorProjectDirectoryState } = api.load('views/dcc/controlled-file/relations/selector-project-directory.ts')
  const state = new SelectorProjectDirectoryState('1')
  await state.open(source); assert.equal(state.total, 41); assert.equal(state.folderId, F)
  const loading = state.selectProject(project(B, 'B')); state.close(); pending.resolve([folder(BF, B, '同名folder')]); await loading
  assert.equal(state.directories.length, 0); assert.equal(state.projectId, undefined)
  assert.equal(calls[0].params.pageNo, 1); assert.equal(calls[0].params.pageSize, 10)
})
test('real selector only loads on open; project search/page and folder switch keep source, selected and target intact', async () => {
  const calls = [], m = mount(async q => { calls.push(q); if (q.url === '/dcc/project-codes/page') return { list: [project(B, '候选B')], total: 41 }; return q.url.includes(B) ? [folder(BF, B, '同名folder')] : [folder(F, P, '同名folder')] })
  try {
    await tick(); assert.equal(calls.length, 0); m.props.modelValue = true; await tick()
    assert.ok(find(m.root, 'el-table', 'dcc-selector-project-list'), 'formal project list must be rendered')
    const list = find(m.root, 'el-table', 'dcc-selector-project-list'); await list.props.onRowClick(project(B, '候选B')); await tick()
    assert.deepEqual(m.queries.at(-1), { keyword: '', pageNo: 1, pageSize: 20, projectId: P, folderId: F })
    const tree = find(m.root, 'el-tree'); assert.equal(tree.props.data[0].projectId, B)
    await tree.props.onNodeClick(tree.props.data[0]); await tick()
    assert.equal(m.queries.at(-1).projectId, B); assert.equal(m.queries.at(-1).folderId, BF)
    assert.equal(m.props.source.projectId, P); assert.ok(content(m.root).includes('源项目A')); assert.ok(content(m.root).includes('已选 1 项'))
    const pagination = find(m.root, 'el-pagination', 'dcc-selector-project-pagination'); assert.equal(pagination.props.total, 41)
    pagination.props['onUpdate:currentPage'](2); await pagination.props.onCurrentChange(); await tick()
    assert.equal(calls.filter(q => q.url === '/dcc/project-codes/page').at(-1).params.pageNo, 2)
  } finally { m.app.unmount() }
})
test('project root never sends project-only selector query, and folder failure remains local while global query works', async () => {
  const m = mount(async q => { if (q.url === '/dcc/project-codes/page') return { list: [project(B, '候选B')], total: 1 }; if (q.url.includes(B)) throw Error('B目录真实权限拒绝'); return [folder(F, P, '同名folder')] })
  try {
    m.props.modelValue = true; await tick(); const prior = m.queries.length
    await find(m.root, 'el-table', 'dcc-selector-project-list').props.onRowClick(project(B, '候选B')); await tick()
    assert.equal(m.queries.length, prior); assert.ok(nodes(m.root, n => n.type === 'el-alert').some(n => n.props.title === 'B目录真实权限拒绝'))
    const tabs = find(m.root, 'el-tabs'); tabs.props['onUpdate:modelValue']('global'); await tabs.props.onTabChange(); await tick()
    assert.equal(m.queries.at(-1).projectId, undefined); assert.equal(m.queries.at(-1).folderId, undefined)
    assert.equal(m.props.source.projectId, P)
  } finally { m.app.unmount() }
})
test('project keyword/pagination uses actual server query and ignores a late previous page after search or close', async () => {
  const delayed = deferred(), calls = [], api = modules(async q => { calls.push(q); if (q.url.includes('/folders')) return [folder(F, P, '同名folder')]; if (q.params.keyword === '迟到') return delayed.promise; return { list: [project(B, '候选B')], total: 83 } })
  const state = new (api.load('views/dcc/controlled-file/relations/selector-project-directory.ts').SelectorProjectDirectoryState)('1')
  await state.open(source); state.keyword = '迟到'; const old = state.searchProjects(); state.keyword = '最新'; await state.searchProjects()
  delayed.resolve({ list: [project(P, '旧页')], total: 1 }); await old
  assert.equal(state.total, 83); assert.equal(state.projects[0].projectName, '候选B')
  assert.equal(calls.filter(q => q.url.endsWith('/page')).at(-1).params.keyword, '最新')
  state.close(); assert.equal(state.total, 0); assert.equal(state.projects.length, 0)
})
test('late source-directory read cannot reopen or replace a different selected project; closing and unmount discard pending reads', async () => {
  const initial = deferred(), m = mount(async q => q.url.endsWith('/page') ? { list: [project(B, '候选B')], total: 1 } : q.url.includes(B) ? [folder(BF, B, '同名folder')] : initial.promise)
  try {
    m.props.modelValue = true; await tick(); await find(m.root, 'el-table', 'dcc-selector-project-list').props.onRowClick(project(B, '候选B')); await tick()
    const tree = find(m.root, 'el-tree'); await tree.props.onNodeClick(tree.props.data[0]); await tick()
    initial.resolve([folder(F, P, '同名folder')]); await tick()
    assert.equal(find(m.root, 'el-tree').props.data[0].projectId, B); assert.equal(m.queries.length, 1); assert.equal(m.queries[0].projectId, B)
    m.props.modelValue = false; await tick(); assert.equal(find(m.root, 'el-tree').props.data.length, 0)
  } finally { m.app.unmount() }
  const pending = deferred(), second = mount(async q => q.url.endsWith('/page') ? pending.promise : [folder(F, P, '同名folder')])
  second.props.modelValue = true; await tick(); second.app.unmount(); pending.resolve({ list: [project(B, '迟到')], total: 10 }); await tick()
  assert.equal(second.queries.length, 0)
})
test('source project without folder sends no invalid root query, then only explicit global lookup requests files', async () => {
  const m = mount(async q => q.url.endsWith('/page') ? { list: [project(B, '候选B')], total: 1 } : [], { source: { ...source, folderId: undefined, folderName: '未记录' } })
  try {
    m.props.modelValue = true; await tick(); assert.equal(m.queries.length, 0)
    const tabs = find(m.root, 'el-tabs'); tabs.props['onUpdate:modelValue']('global'); await tabs.props.onTabChange(); await tick()
    assert.equal(m.queries.length, 1); assert.deepEqual(m.queries[0], { keyword: '', pageNo: 1, pageSize: 20 })
    assert.equal(m.props.source.projectId, P)
  } finally { m.app.unmount() }
})
test('candidate project navigation never changes the persisted reference target and cancel is zero writes', async () => {
  const writes = [], events = [], m = mount(async q => q.url.endsWith('/page') ? { list: [project(B, '候选B')], total: 1 } : q.url.includes(B) ? [folder(BF, B, '同名folder')] : [folder(F, P, '同名folder')], {
    persist: async rows => writes.push({ targetProject: P, targetFolder: F, selected: rows.map(row => row.controlledFileId) }),
    onConfirmed: rows => events.push(rows), 'onUpdate:modelValue': open => { m.props.modelValue = open }
  })
  try {
    m.props.modelValue = true; await tick(); await find(m.root, 'el-table', 'dcc-selector-project-list').props.onRowClick(project(B, '候选B')); await tick()
    nodes(m.root, n => n.type === 'el-button' && content(n) === '取消')[0].props.onClick(); await tick(); assert.equal(writes.length, 0)
    m.props.modelValue = true; await tick(); await find(m.root, 'el-table', 'dcc-selector-project-list').props.onRowClick(project(B, '候选B')); await tick()
    await nodes(m.root, n => n.type === 'el-button' && content(n) === '确认引用')[0].props.onClick(); await tick()
    assert.deepEqual(writes, [{ targetProject: P, targetFolder: F, selected: ['51'] }]); assert.equal(events.length, 1)
    assert.equal(m.props.source.projectId, P); assert.equal(m.props.source.folderId, F)
  } finally { m.app.unmount() }
})
