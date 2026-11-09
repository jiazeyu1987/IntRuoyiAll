const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const { body } = require('./dcc-detail-read-test-loader.cjs')
const drain = async () => { for (let i = 0; i < 30; i++) await Promise.resolve(); await vue.nextTick() }
const projects = [{ id: '11', projectName: '任务项目', projectCode: 'TASK' }, { id: '12', projectName: '另一项目', projectCode: 'NEXT' }]
const options = () => ({ projects, folders: [{ id: '21', parentId: '0', name: '技术资料' }, { id: '22', parentId: '21', name: '规范' }], fileTypes: [{ categoryId: '31', fileTypeTaxonomyId: '41', label: '其他' }], departments: [{ id: '51', name: '文控' }] })
function mount(api = {}) {
  const filename = 'src/views/dcc/controlled-file/controlled-upload/index.vue'
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  const script = compileScript(descriptor, { id: filename })
  assert.deepEqual(compileTemplate({ id: filename, source: descriptor.template.content, compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  const exports = {}, calls = [], navigation = []
  let uuid = 0
  new Function('exports', 'require', ts.transpileModule(script.content, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText)(exports, id => {
    if (id === 'vue') return vue
    if (id === 'vue-router') return { useRouter: () => ({ push: value => navigation.push(value) }) }
    if (id === '@/api/dcc/controlledFile/bodyReadContext') return body
    if (id === '@/utils') return { generateUUID: () => `uuid-${++uuid}` }
    if (id === '@/utils/formatTime') return { formatDateTimeValue: x => x }
    if (id === '@/api/dcc/controlledFile/controlledImports') return { getControlledImportOptions: async () => options(), importControlledFile: async data => { calls.push(data); return { id: '61', importSource: 'CONTROLLED_IMPORT' } }, ...api }
    if (id.endsWith('.vue')) return { default: {} }
    throw Error('Unexpected import: ' + id)
  })
  exports.default.render = () => vue.h('section')
  const renderer = vue.createRenderer({ createElement: () => ({ children: [] }), createComment: () => ({}), createText: () => ({}), insert: (c, p) => p.children.push(c), remove() {}, setText() {}, setElementText() {}, patchProp() {}, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp(exports.default), host = app.mount({ children: [] })
  return { state: host.$.setupState, app, calls, navigation }
}
async function fill(c) {
  await drain(); const s = c.state
  s.projectId = '11'; await s.loadProject()
  s.folderId = '22'; s.categoryId = '31'; s.fileNumber = ' TASK-001 '; s.versionNo = ' A/1 '
  s.file = new File(['actual content'], 'original-name.pdf', { type: 'application/pdf' })
}
test('actual independent upload sends server-authorized mapping and original file, with automatic name and attachment bytes', async () => {
  const c = mount()
  try {
    await fill(c); assert.deepEqual(c.state.folderOptions.map(x => x.path), ['技术资料', '技术资料 / 规范'])
    c.state.attachmentFiles = [{ name: 'evidence.txt', raw: new File(['evidence'], 'evidence.txt', { type: 'text/plain' }) }]
    await c.state.submit(); assert.equal(c.calls.length, 1)
    assert.equal(await c.calls[0].get('attachments').text(), 'evidence')
    const data = c.calls[0]
    for (const [key, value] of Object.entries({ dccProjectCodeId: '11', projectFolderId: '22', categoryId: '31', fileTypeTaxonomyId: '41', fileNumber: 'TASK-001', versionNo: 'A/1' })) assert.equal(data.get(key), value)
    for (const key of ['controlledTime', 'effectiveTime', 'distributionDepartmentIds']) assert.equal(data.get(key), null)
    assert.equal(data.get('file').name, 'original-name.pdf')
    assert.equal(await data.get('file').text(), 'actual content')
    for (const key of ['fileName', 'uploadedBy', 'importedBy', 'importedTime', 'source']) assert.equal(data.get(key), null)
    assert.equal(c.state.result.id, '61')
  } finally { c.app.unmount() }
})
test('missing metadata, empty material and forged unauthorized IDs never post', async () => {
  const c = mount()
  try {
    await fill(c); c.state.categoryId = '999'; await c.state.submit(); assert.match(c.state.error, /当前授权/)
    c.state.categoryId = '31'; c.state.versionNo = 'I/1'; await c.state.submit(); assert.match(c.state.error, /版本格式/)
    c.state.versionNo = 'A/1'; c.state.file = new File([], 'empty.pdf'); await c.state.submit(); assert.match(c.state.error, /非空/)
    assert.equal(c.calls.length, 0)
  } finally { c.app.unmount() }
})
test('failed POST retains one idempotency key; successful next form gets a new key', async () => {
  const keys = []; let failed = true
  const c = mount({ importControlledFile: async data => { keys.push(data.get('idempotencyKey')); if (failed) { failed = false; throw Error('上传未完成') } return { id: '61' } } })
  try {
    await fill(c); await c.state.submit(); assert.match(c.state.error, /上传未完成/)
    await c.state.submit(); assert.deepEqual(keys, ['uuid-1', 'uuid-1'])
    c.state.reset(); assert.equal(c.state.idempotencyKey, 'uuid-2'); assert.equal(c.state.file, undefined)
  } finally { c.app.unmount() }
})
test('project switch clears mapping and ignores a previous project late response', async () => {
  let resolveFirst
  const c = mount({ getControlledImportOptions: id => id === '11' ? new Promise(resolve => { resolveFirst = resolve }) : Promise.resolve({ ...options(), folders: [{ id: '99', parentId: '0', name: '新项目目录' }] }) })
  try {
    await drain(); c.state.projectId = '11'; const first = c.state.loadProject()
    c.state.projectId = '12'; await c.state.loadProject(); resolveFirst(options()); await first
    assert.deepEqual(c.state.folderOptions, [{ id: '99', path: '新项目目录' }])
    assert.equal(c.state.folderId, ''); assert.equal(c.state.categoryId, '')
  } finally { c.app.unmount() }
})
test('invalid project folder shape leaves form blocked and shows the real error', async () => {
  const c = mount({ getControlledImportOptions: async () => ({ ...options(), folders: [{ id: '21', parentId: '99', name: '孤立目录' }] }) })
  try {
    await drain(); c.state.projectId = '11'; await c.state.loadProject()
    assert.match(c.state.error, /上级缺失/); assert.equal(c.state.optionsReady, false)
    await c.state.submit(); assert.equal(c.calls.length, 0)
  } finally { c.app.unmount() }
})
test('future import success navigation carries canonical version context on the first detail request', async () => {
  const c = mount()
  try {
    await drain()
    c.state.result = { id: '61', versionNo: 'A/1', status: 'CONTROLLED_PENDING_EFFECTIVE', importSource: 'CONTROLLED_IMPORT' }
    c.state.openResult()
    assert.equal(c.navigation[0].path, '/dcc/controlled-file/detail/61')
    const context = body.readDccBodyReadContextRoute('61', c.navigation[0].query)
    assert.equal(context.intent, 'VERSION_HISTORY'); assert.equal(context.expectedVersionNo, 'A/1')
    assert.equal(context.bpmRound, undefined)
    assert.equal(c.navigation[0].query.from, 'controlled-import')
    assert.equal(c.navigation[0].query.management, '1')
    assert.equal(c.navigation[0].query.returnTo, '/dcc/controlled-file/controlled-upload')
    c.state.result.status = 'ACTIVE'; c.state.openResult()
    assert.equal(body.readDccBodyReadContextRoute('61', c.navigation[1].query).intent, 'DAILY')
    assert.equal(c.navigation[1].query.from, 'controlled-import')
  } finally { c.app.unmount() }
})

test('blocked duplicate type stays visible but cannot submit while a valid type can submit', async () => {
  const c = mount({ getControlledImportOptions: async () => ({ ...options(), fileTypes: [
    { categoryId: '31', fileTypeTaxonomyId: '41', label: '有效类型' },
    { categoryId: '32', fileTypeTaxonomyId: '42', label: '重复类型（关联多个启用文件类别）', uploadBlocker: '关联多个启用文件类别' }
  ] }) })
  try {
    await fill(c); assert.equal(c.state.options.fileTypes.length, 2)
    c.state.categoryId = '32'; await c.state.submit(); assert.match(c.state.error, /多个启用/); assert.equal(c.calls.length, 0)
    c.state.categoryId = '31'; await c.state.submit(); assert.equal(c.calls.length, 1)
    const source = fs.readFileSync('src/views/dcc/controlled-file/controlled-upload/index.vue', 'utf8')
    assert.match(source, /:disabled="Boolean\(item.uploadBlocker\)"/)
  } finally { c.app.unmount() }
})
