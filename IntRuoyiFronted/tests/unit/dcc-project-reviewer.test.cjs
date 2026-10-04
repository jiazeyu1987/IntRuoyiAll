const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
function load(relative, resolve = () => { throw new Error('unexpected dependency') }) {
  const source = fs.readFileSync(path.join(root, relative), 'utf8')
  const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const exports = {}; new Function('exports', 'require', output)(exports, resolve); return exports
}
const id = '9223372036854775707'
const config = { configured: true, reviewerUserId: id, reviewerUsername: 'reviewer', reviewerNickname: '审核人员', enabled: true }

test('configuration API reads real status and saves exact Long with an explicit reason', async () => {
  const calls = []
  const request = { get: async args => { calls.push(args); return config }, put: async args => { calls.push(args); return config } }
  const api = load('src/api/dcc/controlledFile/projectProductRequests.ts', name => {
    if (name === '@/config/axios') return { default: request }
    if (name.endsWith('/project-reviewer')) return load('src/views/dcc/controlled-file/basic-data/components/project-reviewer.ts')
    throw new Error(name)
  })
  assert.deepEqual(await api.getDccProjectReviewerConfiguration(), config)
  await api.configureDccProjectReviewer({ reviewerUserId: id, reason: '配置质量负责人' })
  assert.equal(calls[1].url, '/dcc/project-product-requests/reviewer-config')
  assert.equal(calls[1].data.reviewerUserId, id)
  await assert.rejects(() => api.configureDccProjectReviewer({ reviewerUserId: 9007199254740992, reason: '配置' }))
  await assert.rejects(() => api.configureDccProjectReviewer({ reviewerUserId: id, reason: ' ' }))
  assert.equal(calls.length, 2)
})

test('missing and disabled reviewer configurations cannot authorize submission', () => {
  const model = load('src/views/dcc/controlled-file/basic-data/components/project-reviewer.ts')
  assert.throws(() => model.requireConfiguredReviewer({ configured: false, reviewerUserId: null, reviewerUsername: null, reviewerNickname: null, enabled: false }), /配置/)
  assert.throws(() => model.requireConfiguredReviewer({ ...config, enabled: false }), /启用/)
  assert.deepEqual(model.requireConfiguredReviewer(config), config)
})

test('review permission uses the immutable request reviewer, never current configuration or admin identity', () => {
  const model = load('src/views/dcc/controlled-file/basic-data/components/project-reviewer.ts')
  const row = { status: 'PENDING_REVIEW', configuredReviewerUserId: id }
  assert.equal(model.canReviewProjectProduct(row, id), true)
  assert.equal(model.canReviewProjectProduct(row, '1'), false)
  assert.equal(model.canReviewProjectProduct({ ...row, configuredReviewerUserId: undefined }, id), false)
  assert.equal(model.canReviewProjectProduct({ ...row, status: 'PENDING_APPROVAL' }, id), false)
  assert.equal(model.canReviewProjectProduct(row, 9007199254740992), false)
})

test('public create and history page mount real reviewer configuration and frozen identities', () => {
  const source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'), 'utf8')
  assert.match(source, /<ProjectReviewerConfiguration\b/)
  assert.match(source, /configuredReviewerNickname/)
  assert.match(source, /canReviewProjectProduct\(row, userStore\.getUser\.id\)/)
  assert.match(source, /data-testid="dcc-project-product-records-open"/)
  assert.match(source, /v-if="projectProductMode === 'create'"/)
  const submit = source.slice(source.indexOf('const submitProjectProductRequest ='), source.indexOf('const handleProjectProductAction ='))
  assert.match(submit, /requireConfiguredReviewer\(await getDccProjectReviewerConfiguration\(\)\)/)
})

function configurationComponent({ confirm = async () => true, permitted = true, saveError, readConfig = async () => config } = {}) {
  const Vue = require('vue'), { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
  const file = 'src/views/dcc/controlled-file/basic-data/components/ProjectReviewerConfiguration.vue'
  const descriptor = parse(fs.readFileSync(path.join(root, file), 'utf8')).descriptor
  const compiled = compileScript(descriptor, { id: 'reviewer-config' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file, id: 'reviewer-config', compilerOptions: { bindingMetadata: compiled.bindings } }).errors, [])
  const puts = [], success = [], events = [], reads = []
  const model = load('src/views/dcc/controlled-file/basic-data/components/project-reviewer.ts')
  const api = load('src/api/dcc/controlledFile/projectProductRequests.ts', name => {
    if (name.endsWith('/project-reviewer')) return model
    if (name === '@/config/axios') return { default: {
      get: async request => { reads.push(request); return readConfig() },
      put: async request => { puts.push(request); if (saveError) throw saveError; return config }
    } }
    throw new Error(name)
  })
  const dependencies = {
    vue: Vue, 'element-plus': { ElMessageBox: { confirm } },
    '@/utils/permission': { checkPermi: () => permitted, checkRole: () => permitted },
    '@/store/modules/user': { useUserStore: () => ({ getIsSetUser: true, getRoles: permitted ? ['doc_control'] : [],
      getPermissions: new Set(permitted ? ['dcc:project-code:update'] : []), getUser: { id: 99, username: 'operator' } }) },
    '@/utils/auth': { getTenantId: () => 1, getVisitTenantId: () => undefined },
    '@/api/system/user': { getSimpleUserList: async () => [{ id, nickname: '审核人员' }] },
    '@/api/dcc/controlledFile/projectProductRequests': api, './project-reviewer': model
  }
  const exports = {}
  new Function('exports', 'require', 'useMessage', ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText)(
    exports, name => { if (!(name in dependencies)) throw new Error(name); return dependencies[name] }, () => ({ success: value => success.push(value) }))
  const component = exports.default; component.render = () => Vue.h('section')
  const renderer = Vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }), insert: (child, parent) => parent.children.push(child), remove() {}, setText() {}, setElementText() {}, parentNode: () => null, nextSibling: () => null, patchProp() {} })
  const app = renderer.createApp({ render: () => Vue.h(component, { onChanged: value => events.push(value) }) })
  const host = app.mount({ children: [] })
  return { state: host.$.subTree.component.setupState, puts, success, events, reads, app }
}

test('real configuration component saves the exact selected account and only reports an acknowledged result', async () => {
  const view = configurationComponent()
  try {
    await view.state.open(); view.state.reason = ' 配置审核责任人 '; await view.state.save()
    assert.equal(view.puts.length, 1)
    assert.deepEqual(view.puts[0].data, { reviewerUserId: id, reason: '配置审核责任人' })
    assert.equal(view.events[0].reviewerUserId, id); assert.equal(view.success.length, 1)
    assert.equal(view.state.visible, false)
  } finally { view.app.unmount() }
})

test('cancelled configuration confirmation retains inputs and sends no write', async () => {
  const view = configurationComponent({ confirm: async () => { throw 'cancel' } })
  try {
    await view.state.open(); view.state.reason = '保留的输入'; await view.state.save()
    assert.equal(view.puts.length, 0); assert.equal(view.success.length, 0); assert.equal(view.events.length, 0)
    assert.equal(view.state.selectedUserId, id); assert.equal(view.state.reason, '保留的输入'); assert.equal(view.state.visible, true)
  } finally { view.app.unmount() }
})

test('configuration permission and transport errors never become successful writes', async () => {
  const denied = configurationComponent({ permitted: false })
  try { await denied.state.open(); assert.equal(denied.reads.length, 0); assert.equal(denied.state.visible, false) }
  finally { denied.app.unmount() }
  const failed = configurationComponent({ saveError: new Error('审计保存失败') })
  try {
    await failed.state.open(); failed.state.reason = '修改'; await failed.state.save()
    assert.match(failed.state.error, /审计保存失败/); assert.equal(failed.success.length, 0); assert.equal(failed.events.length, 0)
  } finally { failed.app.unmount() }
})

test('closing the configuration dialog invalidates a late directory/configuration response', async () => {
  let release
  const view = configurationComponent({ readConfig: () => new Promise(resolve => { release = resolve }) })
  try {
    const pending = view.state.open(); view.state.visible = false; release(config); await pending
    assert.equal(view.state.configuration, undefined); assert.equal(view.state.users.length, 0); assert.equal(view.state.loading, false)
  } finally { view.app.unmount() }
})
