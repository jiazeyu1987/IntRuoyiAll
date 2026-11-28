const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/route/RouteTreePanel.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const draft = { id: 11, routeId: 1, active: false, lifecycleStatus: 'DRAFT' }
const node = { id: 1, nodeType: 'FLOW', pendingStatus: 'DRAFT' }
const flush = async () => { await vue.nextTick(); await new Promise(setImmediate); await vue.nextTick() }
function setup() {
  const out = {}, submitted = [], errors = [], successes = [], events = []
  const state = { versions: [draft], publishable: true, blockers: [], treeBlockers: [], permission: true, lists: 0 }
  const api = {
    getRoute: async () => ({}),
    getRouteVersionList: async () => state.versions,
    getRouteVersionBlockers: async () => {
      if (state.failure) throw state.failure
      return { publishable: state.publishable, blockers: state.blockers }
    }
  }
  const context = { ...vue, exports: out, onMounted() {}, onActivated() {}, defineExpose() {},
    defineEmits: () => event => events.push(event),
    useMessage: () => ({ success: text => successes.push(text), error: text => errors.push(text) }),
    setTimeout, clearTimeout,
    require: name => {
      if (name === '@/api/mes/pro/route') return { ProRouteApi: api }
      if (name === '@/api/mes/pro/route/tree') return { RouteTreeApi: {
        preview: async () => ({ blockers: state.treeBlockers }), list: async () => { state.lists++; return [] }
      } }
      if (name.endsWith('/hasPermi')) return { hasPermission: () => state.permission }
      if (name.endsWith('/user')) return { useUserStore: () => ({ getUser: {} }) }
      if (name.endsWith('/auth')) return { authContextRevision: vue.ref(0) }
      if (name === './routeTreePublish') return { submitRouteVersionWithTreeReview: async version => {
        submitted.push(version.id)
        if (state.submitFailure) throw state.submitFailure
        return { ...version, lifecycleStatus: state.resultStatus || 'ACTIVE' }
      } }
      if (name === './routeError') return {
        isRouteConfirmCancel: error => error === 'cancel' || error === 'close',
        resolveRouteOperationErrorMessage: error => error.message
      }
      return {}
    }
  }
  const code = descriptor.scriptSetup.content + '\nObject.assign(exports, {selected, publishableDraft, publishError, publishing, publishDraft, findPublishableDraft, productBusy, productsChanged, leaderBusy, coefficientBusy})'
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
  } }).outputText, context)
  return { ...out, state, api, submitted, errors, successes, events }
}

test('the publish button template and script compile', () => {
  compileScript(descriptor, { id: 'publish-button' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'publish-button' }).errors, [])
  assert.match(descriptor.template.content, /v-if="publishableDraft && hasPermission\(\['mes:pro-route:version-submit'\]\)"/)
})

test('only a permitted, unblocked draft shows the button', async () => {
  const h = setup()
  h.selected.value = node; await flush()
  assert.equal(h.publishableDraft.value.id, 11)
  for (const status of [undefined, 'ACTIVE', 'PENDING_APPROVAL', 'READY_TO_PUBLISH']) {
    h.selected.value = { ...node, pendingStatus: status }; await flush()
    assert.equal(h.publishableDraft.value, undefined)
  }
  for (const changed of [{ permission: false }, { publishable: false }, { blockers: ['配置不完整'] }, { versions: [] }]) {
    const s = setup(); Object.assign(s.state, changed)
    s.selected.value = { ...node }; await flush()
    assert.equal(s.publishableDraft.value, undefined)
  }
  h.state.treeBlockers = ['子流程冲突']
  h.selected.value = { ...node, nodeType: 'GROUP' }; await flush()
  assert.equal(h.publishableDraft.value, undefined)
})

test('ambiguous drafts and check failures show explicit errors', async () => {
  const h = setup()
  h.state.versions = [draft, { ...draft, id: 12 }]
  h.selected.value = node; await flush()
  assert.match(h.publishError.value, /多个草稿/)
  h.state.versions = [draft]; h.state.failure = Error('读取失败')
  h.selected.value = { ...node }; await flush()
  assert.equal(h.publishError.value, '读取失败')
  assert.equal(h.publishableDraft.value, undefined)
})

test('publishing rechecks state, uses the shared review flow and refreshes the tree', async () => {
  for (const status of ['ACTIVE', 'PENDING_APPROVAL']) {
    const h = setup(); h.state.resultStatus = status
    h.selected.value = node; await flush()
    await Promise.all([h.publishDraft(), h.publishDraft()])
    assert.deepEqual(h.submitted, [11])
    assert.equal(h.state.lists, 1)
    assert.deepEqual(h.events, ['changed'])
    assert.match(h.successes[0], status === 'ACTIVE' ? /发布生效/ : /等待审批/)
    assert.equal(h.publishing.value, false)
  }
  const h = setup(); h.selected.value = node; await flush()
  h.state.versions = [{ ...draft, lifecycleStatus: 'PENDING_APPROVAL' }]
  await h.publishDraft()
  assert.deepEqual(h.submitted, [])
  assert.match(h.errors[0], /状态或发布条件已变化/)
})

test('a stale eligibility response cannot restore a button after switching nodes', async () => {
  const h = setup(); let resolve
  h.api.getRouteVersionList = () => new Promise(done => { resolve = done })
  h.selected.value = node; await vue.nextTick()
  h.selected.value = { id: 2, nodeType: 'FLOW' }
  resolve([draft]); await flush()
  assert.equal(h.publishableDraft.value, undefined)
})

test('review cancellation keeps the draft; submission failures never report success', async () => {
  for (const failure of ['cancel', Error('发布失败')]) {
    const h = setup(); h.state.submitFailure = failure
    h.selected.value = node; await flush(); await h.publishDraft()
    assert.equal(h.successes.length, 0)
    assert.equal(h.errors.length, failure === 'cancel' ? 0 : 1)
    assert.equal(h.publishing.value, false)
    if (failure === 'cancel') assert.equal(h.publishableDraft.value.id, 11)
  }
})

test('product writes block publication and product changes refresh candidate eligibility', async () => {
  const h = setup(); h.selected.value = node; await flush()
  h.productBusy.value = true
  await h.publishDraft()
  assert.deepEqual(h.submitted, [])
  h.productBusy.value = false
  await h.productsChanged()
  assert.equal(h.state.lists, 1)
  assert.deepEqual(h.events, ['changed'])
})

test('quick configuration dialogs block publication until editing finishes', async () => {
  for (const kind of ['leaderBusy', 'coefficientBusy']) {
    const h = setup(); h.selected.value = node; await flush()
    h[kind].value = true
    await h.publishDraft()
    assert.deepEqual(h.submitted, [])
    h[kind].value = false
    await h.publishDraft()
    assert.deepEqual(h.submitted, [11])
  }
})
