const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const scope = {}
new Function('exports', transpile(fs.readFileSync('src/views/dcc/registration-certificate/index/exportScope.ts', 'utf8')))(scope)
const drain = async () => { for (let i = 0; i < 15; i++) await Promise.resolve(); await vue.nextTick() }
function mount(api) {
  const file = 'src/views/dcc/registration-certificate/index/ExcelExportButton.vue'
  const { descriptor, errors } = parse(fs.readFileSync(file, 'utf8'))
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: file })
  assert.deepEqual(compileTemplate({ id: file, source: descriptor.template.content, compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  const exports = {}, saved = [], messages = []
  new Function('exports', 'require', transpile(script.content))(exports, id => {
    if (id === 'vue') return vue
    if (id === 'element-plus') return { ElMessage: { success: value => messages.push(value) } }
    if (id === '@/utils/download') return { default: { excel: (...args) => saved.push(args) } }
    if (id === './exportScope') return scope
    if (id.endsWith('/excelExport')) return api
    throw Error(id)
  })
  exports.default.render = () => vue.h('section')
  const props = vue.reactive({ query: { pageNo: 2, pageSize: 10, productName: '生效产品', sortField: 'certificateNo', sortOrder: 'desc' }, draft: { productName: '草稿产品' }, recordIds: [21, 11], total: 50, disabledReason: '' })
  const renderer = vue.createRenderer({ createElement: () => ({ children: [] }), createComment: () => ({}), createText: () => ({}), insert: (c,p) => p.children.push(c), remove() {}, setText() {}, setElementText() {}, patchProp() {}, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp({ render: () => vue.h(exports.default, props) })
  const host = app.mount({ children: [] })
  return { state: host.$.subTree.component.setupState, props, saved, messages, app }
}
const preview = { snapshotId: 'snapshot-1', requestId: 'request-1', mode: 'FILTERED', count: 50, types: { DOMESTIC: 50 }, maxRecords: 1000, allowed: true }
test('scope comparison ignores pagination, preserves sort separately, and deep clones drafts', () => {
  const a = { pageNo: 1, pageSize: 10, productName: 'A', fieldFilters: { remark: 'x' }, sortField: 'certificateNo', sortOrder: 'asc' }
  const b = { ...a, pageNo: 9, pageSize: 100, sortOrder: 'desc' }
  assert.equal(scope.exportQueryKey(a), scope.exportQueryKey(b))
  assert.notEqual(scope.exportQueryKey(a, true), scope.exportQueryKey(b, true))
  const cloned = scope.cloneExportQuery(a); cloned.fieldFilters.remark = 'draft'
  assert.equal(a.fieldFilters.remark, 'x')
  assert.equal(scope.exportQueryKey({ fieldFilters: {}, productName: '' }), '{}')
})
test('preview uses applied query; duplicate generation is blocked and retry reuses request', async () => {
  const calls = []; let finish
  const c = mount({
    previewExcelExport: async (...args) => { calls.push(['preview', ...args]); return preview },
    fetchExcelExport: async (p, retry) => { calls.push(['fetch', p.requestId, retry]); if (!retry) return new Promise(resolve => { finish = resolve }); return { blob: new Blob(['xlsx']), fileName: '台账.xlsx', exported: 49, skipped: 1 } },
    getExcelExportStatus: async () => ({ state: 'READY', expiresAt: '2026-11-24T00:05:00Z' })
  })
  try {
    await c.state.open('FILTERED')
    assert.equal(calls[0][2].productName, '生效产品')
    assert.deepEqual(calls[0][3], [])
    assert.equal(c.state.hasDraft, true)
    const first = c.state.generate(); await drain(); await c.state.generate()
    assert.equal(calls.filter(x => x[0] === 'fetch').length, 1)
    let closed = false; c.state.close(() => { closed = true }); assert.equal(closed, false)
    finish({ blob: new Blob(['xlsx']), fileName: '台账.xlsx', exported: 49, skipped: 1 }); await first
    await c.state.retry()
    assert.deepEqual(calls.filter(x => x[0] === 'fetch').map(x => x.slice(1)), [['request-1', false], ['request-1', true]])
    assert.equal(c.saved.length, 2); assert.match(c.messages[0], /49 条，跳过 1 条/)
    c.state.visible = false; await c.state.open('SELECTED')
    assert.equal(calls.filter(x => x[0] === 'preview').length, 2)
    assert.deepEqual(calls.filter(x => x[0] === 'preview').at(-1)[3], [21, 11])
  } finally { c.app.unmount() }
})
test('unknown result survives close/reopen; terminal failure permits a new scope', async () => {
  let previews = 0
  const c = mount({ previewExcelExport: async () => { previews++; return preview }, fetchExcelExport: async () => { throw Error('网络断开') }, getExcelExportStatus: async () => ({ state: 'EXPIRED', reason: '5分钟窗口过期' }) })
  try {
    await c.state.open('SELECTED'); await c.state.generate()
    assert.equal(c.state.submitted, true); assert.match(c.state.error, /网络断开/)
    c.state.visible = false; await c.state.open('FILTERED'); assert.equal(previews, 1)
    await c.state.retry(); assert.equal(c.state.terminal, true)
    await c.state.open('FILTERED'); assert.equal(previews, 2)
  } finally { c.app.unmount() }
})
