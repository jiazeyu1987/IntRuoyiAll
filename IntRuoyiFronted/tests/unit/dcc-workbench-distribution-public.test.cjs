const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const pagePath = 'src/views/dcc/controlled-file/workbench/index.vue'
const actionsPath = 'src/views/dcc/controlled-file/workflow/workflow-actions.ts'
const listPath = 'src/views/dcc/controlled-file/workflow/PendingWorkflowDistributionList.vue'
const apiPath = 'src/api/dcc/controlledFile/workflowLifecycle.ts'
const fullLong = '9223372036854775709'
const pending = (id = fullLong, date = '2099-10-03', stage = 'FUTURE') => ({ id, fileNumber: 'SOP-001', title: '待下发文件', versionNo: 'B/1', effectiveDate: date, controlledTime: '2026-10-03 08:30:00', status: 'CONTROLLED_PENDING_EFFECTIVE', distributionReminderStage: stage })
const make = (type, text = '') => ({ type, text, children: [], props: {}, parent: null })
const renderer = vue.createRenderer({ createElement: type => make(type), createText: text => make('#text', text), createComment: text => make('#comment', text),
  setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text; node.children = [] },
  patchProp: (node, key, _old, value) => { node.props[key] = value }, insert(node, parent, anchor) {
    if (node.parent) { const index = node.parent.children.indexOf(node); if (index >= 0) node.parent.children.splice(index, 1) }
    node.parent = parent; const at = anchor ? parent.children.indexOf(anchor) : -1
    if (at < 0) parent.children.push(node); else parent.children.splice(at, 0, node)
  }, remove(node) { if (node.parent) { const index = node.parent.children.indexOf(node); if (index >= 0) node.parent.children.splice(index, 1) } node.parent = null },
  parentNode: node => node.parent, nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1], setScopeId() {},
  insertStaticContent(text, parent) { const node = make('#static', text); node.parent = parent; parent.children.push(node); return [node, node] } })
const nodes = (node, predicate) => [...(predicate(node) ? [node] : []), ...node.children.flatMap(child => nodes(child, predicate))]
const text = node => node.text + node.children.map(text).join('')
const find = (root, type, label) => nodes(root, node => node.type === type && (!label || text(node).includes(label)))[0]
const settle = async () => { for (let n = 0; n < 16; n++) { await Promise.resolve(); await vue.nextTick() } }
function mount({ roles = ['doc_control'], permission = true, load = async () => [pending()] } = {}) {
  const user = vue.reactive({ getIsSetUser: true, getRoles: roles, getPermissions: new Set(permission ? ['dcc:controlled-file:distribute'] : []), getUser: { id: '9007199254740993' } })
  const calls = [], routes = [], browserCalls = [], cache = new Map(), tenant = { value: '1' }
  const route = vue.reactive({ fullPath: '/dcc/controlled-file/workbench' })
  const router = { push: async target => routes.push(target) }
  const presentation = { buildDccWorkbenchMetricItems: source => ['approvalTodoTotal', 'pendingDistributionTotal', 'trainingTodoTotal', 'finalizationFailedTotal'].map(key => ({ key, label: key === 'pendingDistributionTotal' ? '待文控下发' : key, routePath: '/old', count: source[key], tone: 'primary' })), resolveWorkbenchErrorMessage: cause => cause.message,
    toWorkbenchFileRow: value => value, toWorkbenchTrainingRow: value => value }
  const request = { get: async args => { calls.push(args); assert.equal(args.url, '/dcc/controlled-file/workflow-lifecycle/pending-distribution'); return load(args.params.remindersOnly) } }
  const evaluate = (file, imports) => {
    if (cache.has(file)) return cache.get(file)
    let source = fs.readFileSync(file, 'utf8')
    if (file.endsWith('.vue')) source = compileScript(parse(source, { filename: file }).descriptor, { id: file, inlineTemplate: true }).content
    const context = { exports: {}, require: imports, ...vue, useRoute: () => route, useRouter: () => router,
      useMessage: () => ({ error() {}, success() {}, warning() {} }), Error, Date, setTimeout, clearTimeout }
    vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
    cache.set(file, context.exports); return context.exports
  }
  const actions = evaluate(actionsPath, name => { throw new Error(name) })
  const lifecycle = () => evaluate(apiPath, name => { if (name === '@/config/axios') return { default: request }; throw new Error(name) })
  const list = () => evaluate(listPath, name => { if (name === 'vue') return vue; if (name === './workflow-actions') return actions; throw new Error(name) }).default
  const component = evaluate(pagePath, name => {
    if (name === 'vue') return vue
    if (name === 'element-plus') return { ElMessageBox: {} }
    if (name === '../workflow/PendingWorkflowDistributionList.vue') return { default: list() }
    if (name === '../workflow/workflow-actions') return actions
    if (name === '@/api/dcc/controlledFile/workflowLifecycle') return lifecycle()
    if (name === '@/store/modules/user') return { useUserStore: () => user }
    if (name === '@/utils/auth') return { getTenantId: () => tenant.value, getVisitTenantId: () => undefined }
    if (name === '@/api/bpm/task') return { getTaskTodoPage: async () => ({ list: [], total: 0 }) }
    if (name === '@/api/bpm/processInstance') return { getProcessInstance: async () => { throw new Error('No todo fixture') } }
    if (name === '@/api/dcc/controlledFile/workflow') return { CONTROLLED_FILE_PROCESS_DEFINITION_KEY: 'DCC', CONTROLLED_FILE_UPLOAD_PROCESS_DEFINITION_KEY: 'NEW', CONTROLLED_FILE_REVISION_PROCESS_DEFINITION_KEY: 'REVISION', CONTROLLED_FILE_OBSOLETE_PROCESS_DEFINITION_KEY: 'OBSOLETE', EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY: 'EXTERNAL', getControlledFileBrowserPage: async args => { browserCalls.push(args); return { list: [], total: 0 } } }
    if (name === '@/api/dcc/controlledFile/training') return { getMyTrainingTaskPage: async () => ({ list: [], total: 0 }) }
    if (name === '@/utils/formatTime') return { formatDateTimeValue: () => '已刷新' }
    if (name === '@/api/dcc/controlledFile/publicationFollowup') return { getMyImpactTaskPage: async () => ({ list: [], total: 0 }) }
    if (name === '../shared/approval') return { buildDccTaskCenterRowView: () => ({}) }
    if (name === '../shared/viewer-navigation') return { openControlledFileViewer: () => { throw new Error('Pending distribution must open manage detail') } }
    if (name === '../shared/publicationFollowupPresentation') return {}
    if (name === './presentation') return presentation
      if (name === '@/api/approval-center') return { getApprovalTaskPage: async () => ({ list: [], total: 0 }) }
      if (name === '@/utils/dccOfflineTrainingRecord') return evaluate('src/utils/dccOfflineTrainingRecord.ts', () => { throw new Error('Unexpected offline helper runtime import') })
      throw new Error('Unexpected workbench import: ' + name)
  }).default
  const root = make('root'), app = renderer.createApp(component)
  app.config.warnHandler = warning => { if (!warning.startsWith('Failed to resolve') && !warning.startsWith('Runtime directive')) throw new Error(warning) }
  for (const tag of ['ContentWrap', 'Icon', 'el-button', 'el-alert', 'el-tag', 'el-checkbox', 'el-empty', 'el-pagination', 'el-dialog', 'el-radio-group', 'el-radio', 'el-input']) app.component(tag, { inheritAttrs: false, setup: (_, context) => () => vue.h(tag, context.attrs, [tag === 'el-alert' ? context.attrs.title : '', context.slots.default?.(), context.slots.footer?.()]) })
  app.component('el-table', { props: ['data'], setup: (props, { slots }) => { vue.provide('rows', vue.toRef(props, 'data')); return () => vue.h('el-table', {}, slots.default?.()) } })
  app.component('el-table-column', { inheritAttrs: false, setup: (_, context) => { const rows = vue.inject('rows'); return () => vue.h('el-table-column', context.attrs, rows.value.map(row => context.slots.default ? context.slots.default({ row }) : String(row[context.attrs.prop] ?? ''))) } })
  app.directive('loading', {}); app.mount(root)
  return { root, app, calls, routes, browserCalls, user, tenant }
}

test('mounted public workbench reads the formal date-sorted list and navigates exact Long to manage detail', async () => {
  const mounted = mount(); try {
    await settle()
    assert.equal(mounted.calls.length, 1, 'real pending workflow list must be mounted and loaded once')
    assert.equal(mounted.calls[0].params.remindersOnly, false)
    for (const value of ['待下发文件', 'B/1', '2026-10-03 08:30:00', '2099-10-03', '生效前不得执行', '后续待下发']) assert.ok(text(mounted.root).includes(value), value)
    find(mounted.root, 'el-button', '办理下发').props.onClick(); await settle()
    assert.equal(mounted.routes[0].path, `/dcc/controlled-file/detail/${fullLong}`)
    assert.equal(mounted.routes[0].query.mode, 'manage')
    assert.equal(mounted.browserCalls.some(query => query.status === 'PENDING_MANUAL_DISTRIBUTION'), false)
  } finally { mounted.app.unmount() }
})

test('super_admin without explicit doc_control and accounts without distribute permission never read the list', async () => {
  for (const options of [{ roles: ['super_admin'] }, { roles: ['doc_control'], permission: false }]) {
    const mounted = mount(options); try { await settle(); assert.equal(mounted.calls.length, 0); assert.equal(find(mounted.root, 'el-button', '办理下发'), undefined); assert.equal(mounted.browserCalls.some(query => query.status === 'PENDING_MANUAL_DISTRIBUTION'), false) } finally { mounted.app.unmount() }
  }
})

test('remindersOnly and public refresh go through the existing lifecycle endpoint and remove distributed rows', async () => {
  let reads = 0
  const mounted = mount({ load: async filter => ++reads === 1 ? [pending()] : [{ ...pending(), distributedTime: '2026-10-03 09:00:00' }] })
  try {
    await settle(); const checkbox = find(mounted.root, 'el-checkbox', '仅显示')
    assert.ok(checkbox, 'public reminder filter must be mounted')
    checkbox.props['onUpdate:modelValue'](true); await settle()
    assert.equal(mounted.calls.at(-1).params.remindersOnly, true)
    assert.equal(find(mounted.root, 'el-button', '办理下发'), undefined)
    find(mounted.root, 'el-button', '刷新待处置列表').props.onClick(); await settle()
    assert.equal(mounted.calls.length, 3)
  } finally { mounted.app.unmount() }
})

test('missing reminder configuration is visible without old-status fallback or a zero-success metric', async () => {
  const mounted = mount({ load: async () => { throw new Error('生效提醒提前天数尚未配置') } })
  try {
    await settle(); assert.ok(text(mounted.root).includes('生效提醒提前天数尚未配置'))
    assert.equal(mounted.calls.length, 1)
    assert.equal(mounted.browserCalls.some(query => query.status === 'PENDING_MANUAL_DISTRIBUTION'), false)
    const metric = nodes(mounted.root, node => node.type === 'button' && text(node).includes('待文控下发'))[0]
    assert.ok(text(metric).includes('—'), 'failed count must remain unavailable')
  } finally { mounted.app.unmount() }
})

test('revoking the real role during a pending read removes the public panel and ignores its late result', async () => {
  let finish
  const mounted = mount({ load: () => new Promise(resolve => { finish = resolve }) })
  try {
    await settle(); assert.equal(mounted.calls.length, 1)
    mounted.user.getRoles = ['super_admin']; await settle()
    finish([pending()]); await settle()
    assert.equal(find(mounted.root, 'el-button', '办理下发'), undefined)
    assert.equal(text(mounted.root).includes('待下发文件'), false)
  } finally { mounted.app.unmount() }
})

test('a pending public read cannot display another tenant response after the official cache context changes', async () => {
  let finish
  const mounted = mount({ load: () => new Promise(resolve => { finish = resolve }) })
  try {
    await settle(); mounted.tenant.value = '2'
    finish([pending()]); await settle()
    assert.equal(text(mounted.root).includes('待下发文件'), false)
    assert.ok(text(mounted.root).includes('上下文已变化'))
  } finally { mounted.app.unmount() }
})

test('global workbench refresh re-reads the panel and missing runtime roles cannot expose its read', async () => {
  let loaded = 0
  const mounted = mount({ load: async () => ++loaded === 1 ? [pending()] : [] })
  try {
    await settle(); find(mounted.root, 'el-button', '刷新').props.onClick(); await settle()
    assert.equal(mounted.calls.length, 2)
    assert.equal(find(mounted.root, 'el-button', '办理下发'), undefined)
    mounted.user.getIsSetUser = false; await settle()
    assert.equal(find(mounted.root, 'el-button', '刷新待处置列表'), undefined)
    assert.equal(mounted.calls.length, 2)
  } finally { mounted.app.unmount() }
})

test('network failure exposes formal business detail and never displays an empty-success table', async () => {
  const mounted = mount({ load: async () => { throw { response: { data: { msg: '待下发读取网络失败' } } } } })
  try {
    await settle(); assert.ok(text(mounted.root).includes('待下发读取网络失败'))
    const panel = nodes(mounted.root, node => node.props['data-testid'] === 'dcc-workflow-pending-distribution-list')[0]
    assert.ok(panel); assert.equal(nodes(panel, node => node.type === 'el-table').length, 0)
  } finally { mounted.app.unmount() }
})
