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
        deleteGroup: id => remove('GROUP', id), list: async () => { state.lists++; return state.remaining }
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
  const source = descriptor.scriptSetup.content + '\nObject.assign(exports, {selected, rows, deleting, loading, saving, publishing, productBusy, leaderBusy, coefficientBusy, deleteSelectedNode})'
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
  } }).outputText, context)
  out.rows.value = node ? [node] : []
  out.selected.value = node
  return { ...out, state, requests, confirmations, errors, successes, events }
}

test('delete controls compile and require existing delete permission', () => {
  compileScript(descriptor, { id: 'tree-delete' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'tree-delete' }).errors, [])
  const buttons = [...descriptor.template.content.matchAll(/<el-button\b[^>]*@click="deleteSelectedNode"[^>]*>([^<]+)<\/el-button>/g)]
  assert.deepEqual(buttons.map(button => button[1]), ['删除目录', '删除工艺流程'])
  for (const [button] of buttons) assert.match(button, /v-hasPermi="\['mes:pro-route:delete'\]"/)
})

test('confirmed group and flow deletion use their respective APIs and refresh', async () => {
  for (const node of [group, flow]) {
    const h = setup(node)
    await h.deleteSelectedNode()
    assert.deepEqual(h.requests, [[node.nodeType, node.id]])
    assert.match(h.confirmations[0][0], new RegExp(node.name))
    assert.equal(h.selected.value, undefined)
    assert.equal(h.rows.value.length, 0)
    assert.equal(h.state.lists, 1)
    assert.deepEqual(h.events, ['changed'])
    assert.equal(h.successes.length, 1)
    assert.equal(h.deleting.value, false)
  }
})

test('nonempty directories, missing selection and missing permission never delete', async () => {
  for (const node of [{ ...group, children: [flow] }, undefined]) {
    const h = setup(node); if (!node) h.selected.value = undefined
    await h.deleteSelectedNode()
    assert.equal(h.requests.length, 0)
    assert.equal(h.confirmations.length, 0)
    assert.equal(h.errors.length, 1)
  }
  const h = setup(); h.state.permission = false
  await h.deleteSelectedNode()
  assert.equal(h.requests.length, 0)
  assert.match(h.errors[0], /权限/)
})

test('confirmation cancellation preserves the node without reporting failure', async () => {
  for (const reason of ['cancel', 'close']) {
    const h = setup(); h.state.confirm = async () => { throw reason }
    await h.deleteSelectedNode()
    assert.equal(h.requests.length, 0)
    assert.equal(h.selected.value.id, flow.id)
    assert.equal(h.errors.length, 0)
    assert.equal(h.successes.length, 0)
    assert.equal(h.deleting.value, false)
  }
})

test('backend deletion restrictions retain the node and show the actual failure', async () => {
  const h = setup(); h.state.failure = Error('工艺流程仍被活跃订单引用')
  await h.deleteSelectedNode()
  assert.deepEqual(h.errors, ['工艺流程仍被活跃订单引用'])
  assert.equal(h.selected.value.id, flow.id)
  assert.equal(h.rows.value.length, 1)
  assert.equal(h.successes.length, 0)
  assert.equal(h.state.lists, 0)
  assert.equal(h.deleting.value, false)
})

test('deletion captures the confirmed node and prevents duplicate submissions', async () => {
  const h = setup(); let confirm
  h.rows.value = [flow, group]
  h.state.confirm = () => new Promise(resolve => { confirm = resolve })
  const pending = h.deleteSelectedNode()
  await h.deleteSelectedNode()
  h.selected.value = group; h.state.remaining = [group]
  confirm(); await pending
  assert.deepEqual(h.requests, [['FLOW', flow.id]])
  assert.equal(h.selected.value.id, group.id)
  assert.equal(h.confirmations.length, 1)
})

test('ongoing publication, product writes and tree mutations prevent deletion', async () => {
  for (const key of ['loading', 'saving', 'publishing', 'productBusy', 'leaderBusy', 'coefficientBusy']) {
    const h = setup(); h[key].value = true
    await h.deleteSelectedNode()
    assert.equal(h.confirmations.length, 0)
    assert.equal(h.requests.length, 0)
  }
})
