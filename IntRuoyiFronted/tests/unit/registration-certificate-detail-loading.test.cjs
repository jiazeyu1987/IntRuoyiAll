const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const drain = async () => { for (let i = 0; i < 40; i++) await Promise.resolve(); await vue.nextTick() }
function mount(api, initialId) {
  const route = vue.reactive({ params: { id: initialId }, query: {} })
  const descriptor = parse(fs.readFileSync('src/views/dcc/registration-certificate/detail/index.vue', 'utf8')).descriptor
  const script = compileScript(descriptor, { id: 'registration-detail-test' })
  assert.deepEqual(compileTemplate({ id: 'registration-detail-test', source: descriptor.template.content, compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  const routeUtils = {}
  new Function('exports', transpile(fs.readFileSync('src/utils/routeQueryId.ts', 'utf8')))(routeUtils)
  const exports = {}
  new Function('exports', 'require', 'useMessage', 'window', transpile(script.content))(exports, id => {
    if (id === 'vue') return vue
    if (id === 'vue-router') return { useRoute: () => route }
    if (id === '@/utils/routeQueryId') return routeUtils
    if (id === '@/api/dcc/registrationCertificate') return { getRegistrationCertificateHistory: async () => [], getRegistrationCertificateFileDownloadGrantStatuses: async () => [], ...api }
    if (id.endsWith('.vue')) return { default: {} }
    if (id.endsWith('/facts')) return { parseFacts: JSON.parse }
    return {}
  }, () => ({ error() {}, success() {} }), { innerWidth: 1280, addEventListener() {}, removeEventListener() {} })
  exports.default.render = () => vue.h('section')
  const renderer = vue.createRenderer({ createElement: () => ({ children: [] }), createComment: () => ({}), createText: () => ({}), insert(c, p) { p.children.push(c) }, remove() {}, setText() {}, setElementText() {}, patchProp() {}, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp(exports.default), host = app.mount({ children: [] })
  return { route, state: host.$.setupState, instance: host.$, app }
}
test('detail loads when navigation supplies the certificate after mount and when cached page is activated', async () => {
  const calls = [], c = mount({ getRegistrationCertificateDetail: async (id, version) => { calls.push([id, version]); return { certificateId: id, versionId: version || '24' } } })
  try {
    await drain(); assert.equal(calls.length, 0)
    c.route.params.id = '23'; await drain()
    assert.deepEqual(calls, [['23', '']]); assert.equal(c.state.detail.certificateId, '23')
    c.state.detail = undefined
    for (const activate of c.instance.a || []) activate()
    await drain(); assert.equal(c.state.detail.certificateId, '23'); assert.equal(calls.length, 2)
    c.route.query.versionId = '22'; await drain(); assert.deepEqual(calls.at(-1), ['23', '22'])
  } finally { c.app.unmount() }
})
test('switching certificate clears content and ignores a late response from the preceding certificate', async () => {
  const pending = [], c = mount({ getRegistrationCertificateDetail: id => new Promise(resolve => pending.push({ id, resolve })) }, '19')
  try {
    await drain(); c.route.params.id = '23'; await drain()
    assert.equal(c.state.detail, undefined); assert.equal(c.state.loading, true)
    pending[1].resolve({ certificateId: '23', versionId: '24' }); await drain()
    pending[0].resolve({ certificateId: '19', versionId: '20' }); await drain()
    assert.equal(c.state.detail.certificateId, '23'); assert.equal(c.state.loading, false)
  } finally { c.app.unmount() }
})
test('query denial displays its actual error and clears the preceding certificate', async () => {
  const c = mount({ getRegistrationCertificateDetail: async id => { if (id === '23') throw Error('无权查看注册证'); return { certificateId: id, versionId: '20' } } }, '19')
  try {
    await drain(); assert.equal(c.state.detail.certificateId, '19')
    c.route.params.id = '23'; await drain()
    assert.equal(c.state.detail, undefined); assert.equal(c.state.loadError, '无权查看注册证'); assert.equal(c.state.loading, false)
  } finally { c.app.unmount() }
})
test('returning from approval context on the same detail route loads the formal certificate', async () => {
  const calls = [], c = mount({ getRegistrationCertificateDetail: async id => { calls.push(id); return { certificateId: id, versionId: '24' } } }, '23')
  try {
    await drain(); c.route.query.requestId = '41'; c.route.query.processInstanceId = 'approval-process'; await drain()
    assert.equal(c.state.detail, undefined)
    delete c.route.query.requestId; delete c.route.query.processInstanceId; await drain()
    assert.deepEqual(calls, ['23', '23']); assert.equal(c.state.detail.certificateId, '23')
  } finally { c.app.unmount() }
})
test('empty detail response is an explicit query error rather than a silent empty page', async () => {
  const c = mount({ getRegistrationCertificateDetail: async () => null }, '23')
  try {
    await drain(); assert.equal(c.state.detail, undefined); assert.match(c.state.loadError, /未返回数据/)
  } finally { c.app.unmount() }
})
