const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../../src/views/mes/pro/route/RouteTreePanel.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))

function setup(different = true, cancellation) {
  const out = {}, confirmations = [], writes = [], errors = []
  const preview = { different, fingerprint: 'same-version',
    currentProcesses: Array.from({ length: 40 }, (_, i) => `当前工序${i + 1}：${'完整名称'.repeat(30)}`),
    expectedProcesses: ['目标工序一', '<目标工序二>'] }
  const context = { ...vue, exports: out, onMounted() {}, onActivated() {}, defineExpose() {},
    defineEmits: () => () => {}, setTimeout, clearTimeout,
    useMessage: () => ({ success() {}, error: value => errors.push(value) }),
    require: name => {
      if (name === 'vue') return vue
      if (name === 'element-plus') return { ElMessageBox: { confirm: async (...args) => {
        confirmations.push(args)
        assert.equal(writes.length, 0, '移动必须等待用户确认')
        if (cancellation) throw cancellation
      } } }
      if (name === '@/api/mes/pro/route/tree') return { RouteTreeApi: {
        movePreview: async () => preview, move: async data => writes.push(data), list: async () => []
      } }
      if (name.endsWith('/user')) return { useUserStore: () => ({ getUser: {} }) }
      if (name.endsWith('/auth')) return { authContextRevision: vue.ref(0) }
      if (name === './routeError') return {
        isRouteConfirmCancel: error => error === 'cancel' || error === 'close',
        resolveRouteOperationErrorMessage: error => error.message
      }
      return {}
    }
  }
  const source = descriptor.scriptSetup.content + '\nObject.assign(exports, {move, moveVisible, saveMove})'
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
  } }).outputText, context)
  Object.assign(out.move, { id: 1, parentId: 2 })
  out.moveVisible.value = true
  return { ...out, preview, confirmations, writes, errors }
}

test('long process lists retain every name as text, with wrapping and scrolling', async () => {
  compileScript(descriptor, { id: 'move-confirm' })
  const h = setup()
  await h.saveMove()
  const [content, title, options] = h.confirmations[0]
  assert.equal(title, '工序一致性确认')
  assert.equal(content.props.style.overflowY, 'auto')
  assert.equal(content.props.style.overflowWrap, 'anywhere')
  assert.equal(content.props.style.maxHeight, '60vh')
  assert.equal(options.customStyle.maxWidth, 'calc(100vw - 32px)')
  for (const [index, names] of [h.preview.currentProcesses, h.preview.expectedProcesses].entries()) {
    const section = content.children[index + 1]
    assert.equal(section.children[1].type, 'ol')
    assert.deepEqual(Array.from(section.children[1].children, node => node.children), names)
  }
  assert.deepEqual(JSON.parse(JSON.stringify(h.writes)), [
    { id: 1, parentId: 2, fingerprint: 'same-version', confirmDifference: true }
  ])
  assert.equal(h.moveVisible.value, false)
  assert.deepEqual(h.errors, [])
})

test('cancelling difference confirmation preserves the move dialog and never moves', async () => {
  for (const reason of ['cancel', 'close']) {
    const h = setup(true, reason)
    await h.saveMove()
    assert.equal(h.writes.length, 0)
    assert.equal(h.moveVisible.value, true)
    assert.deepEqual(h.errors, [])
  }
})

test('identical structures move without a difference confirmation', async () => {
  const h = setup(false)
  await h.saveMove()
  assert.equal(h.confirmations.length, 0)
  assert.equal(h.writes.length, 1)
  assert.equal(h.writes[0].confirmDifference, false)
})
