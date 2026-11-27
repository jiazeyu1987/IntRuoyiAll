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
const group = { id: 1, name: '测试目录', nodeType: 'GROUP', children: [] }
const flow = { id: 2, name: '测试流程', nodeType: 'FLOW', children: [] }
function setup(node = flow) {
  const out = {}, errors = [], successes = [], events = [], requests = [], confirmations = []
  const state = { permission: true, remaining: [], lists: 0 }
  const remove = async (type, id) => {
    requests.push([type, id])
    if (state.failure) throw state.failure
  }
  const context = { ...vue, exports: out, onMounted() {}, onActivated() {}, defineExpose() {},
    defineEmits: () => event => events.push(event),
    useMessage: () => ({ success: value => successes.push(value), error: value => errors.push(value) }),
    setTimeout, clearTimeout,
    require: name => {
      if (name === 'element-plus') return { ElMessageBox: { confirm: async (...args) => {
        confirmations.push(args)
        if (state.confirm) await state.confirm()
      } } }
      if (name === '@/api/mes/pro/route') return { ProRouteApi: {
        getRoute: async () => ({}), deleteRoute: id => remove('FLOW', id)
      } }
      if (name === '@/api/mes/pro/route/tree') return { RouteTreeApi: {
        group: async data => { requests.push({...data}); if (state.pending) await state.pending; if (state.failure) throw state.failure; return 3 }, deleteGroup: id => remove('GROUP', id), list: async () => { state.lists++; return state.remaining }
      } }
      if (name.endsWith('/hasPermi')) return { hasPermission: () => state.permission }
      if (name.endsWith('/user')) return { useUserStore: () => ({ getUser: {} }) }
      if (name.endsWith('/auth')) return { authContextRevision: vue.ref(0) }
      if (name === './routeError') return {
        isRouteConfirmCancel: error => error === 'cancel' || error === 'close',
        resolveRouteOperationErrorMessage: error => error.message
      }
      return {}
    }
  }
  const source = descriptor.scriptSetup.content + '\nObject.assign(exports, {selected, rows, deleting, loading, saving, publishing, productBusy, leaderBusy, coefficientBusy, deleteSelectedNode, group, groupError, groupVisible, openGroup, saveGroup})'
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
  } }).outputText, context)
  out.rows.value = node ? [node] : []
  out.selected.value = node
  return { ...out, state, requests, confirmations, errors, successes, events }
}

test('required group inputs do not send requests and errors persist in dialog', async () => {
  const h = setup(); h.openGroup()
  await h.saveGroup()
  assert.equal(h.requests.length, 0)
  assert.ok(h.groupError.value)
  assert.equal(h.groupVisible.value, true)
  assert.match(descriptor.template.content, /v-if="groupError"[^>]*:title="groupError"[^>]*:closable="false"/)
})
test('failed creation preserves inputs and reason, retry succeeds and refreshes once', async () => {
  const h = setup(); h.openGroup()
  Object.assign(h.group, {name: ' New ', sourceRouteId: 2})
  h.state.failure = Error('nodes[0] missing sopFileUrl')
  await h.saveGroup()
  assert.equal(h.groupError.value, h.state.failure.message)
  assert.equal(h.groupVisible.value, true)
  assert.equal(h.group.name, ' New ')
  assert.equal(h.saving.value, false)
  assert.equal(h.state.lists, 0)
  assert.equal(h.successes.length, 0)
  h.state.failure = undefined
  h.state.remaining = [{id:3, name:'New', nodeType:'GROUP'}]
  await h.saveGroup()
  assert.equal(h.groupError.value, '')
  assert.equal(h.groupVisible.value, false)
  assert.equal(h.state.lists, 1)
  assert.equal(h.selected.value.id, 3)
  assert.deepEqual(h.events, ['changed'])
  assert.equal(h.successes.length, 1)
  assert.equal(h.requests[1].name, 'New')
})
test('double submission sends only one pending creation request', async () => {
  const h = setup(); h.openGroup()
  Object.assign(h.group, {name:'New', sourceRouteId:2})
  let release
  h.state.pending = new Promise(resolve => { release = resolve })
  const pending = h.saveGroup()
  await h.saveGroup()
  assert.equal(h.requests.length, 1)
  assert.equal(h.saving.value, true)
  release(); await pending
  assert.equal(h.state.lists, 1)
  assert.equal(h.saving.value, false)
})
