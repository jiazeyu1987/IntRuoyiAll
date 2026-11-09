const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const read = p => fs.readFileSync('src/' + p, 'utf8')
const load = (file, dependencies = {}) => {
  const scope = { exports: {}, TextEncoder, TextDecoder, btoa, atob, Error,
    require: id => { if (!(id in dependencies)) throw Error(id); return dependencies[id] } }
  vm.runInNewContext(ts.transpileModule(read(file), { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, scope)
  return scope.exports
}
test('attachment metadata and bytes retain exact context and the stamped PDF blob', async () => {
  const helpers = load('api/dcc/controlledFile/bodyReadContext.ts')
  const requests = [], bytes = new Blob(['%PDF-1.4 already-stamped-content'])
  const api = load('api/common/filePreview.ts', {
    axios: { default: { get: async (url, options) => { requests.push({ url, ...options }); return { data: bytes, headers: {} } } } },
    '@/config/axios': { default: { get: async options => { requests.push(options); return { viewerToken: 'token' } } } },
    '@/config/axios/config': { config: { base_url: '/admin-api', request_timeout: 1000 } },
    '@/utils': { generateUUID: () => 'request' },
    '@/api/dcc/controlledFile/bodyReadContext': helpers,
    '@/api/dcc/controlledFile/workflow': {
      parseControlledFilePreviewMetadata: x => x, buildControlledFileBinaryHeaders: x => x,
      decodePreviewWatermark: () => ({ traceCode: 'trace' }), getAxiosHeader: () => '',
      DCC_REQUEST_ID_HEADER: 'request-id', DCC_VIEWER_TOKEN_HEADER: 'viewer-token'
    }
  })
  const context = { intent: 'APPROVAL_HISTORY', controlledFileId: '20', expectedVersionNo: 'A/1', applicationType: 'UPLOAD', bpmRound: 'round', taskId: 'task' }
  const source = api.buildDccControlledFileAttachmentPreviewSource('20', '77', context)
  const result = await api.previewOnlineFileWithWatermark(source)
  assert.equal(requests[0].params.bodyReadContext, requests[1].headers['X-DCC-Body-Read-Context'])
  assert.deepEqual(JSON.parse(Buffer.from(requests[0].params.bodyReadContext, 'base64url')), context)
  assert.equal(result.blob, bytes)
  assert.throws(() => api.buildDccControlledFileAttachmentPreviewSource('21', '77', context))
})
test('business opinion presentation preserves original evidence', () => {
  const api = load('views/dcc/controlled-file/shared/signature-evidence.ts')
  const raw = '同意；会签指派事实：部门=10，人员=99，哈希=abcdef'
  assert.equal(api.getDccSignatureOpinionLabel(raw), '同意')
  assert.equal(api.getDccSignatureOpinionLabel('请检查正文中的哈希说明'), '请检查正文中的哈希说明')
  assert.equal(raw, '同意；会签指派事实：部门=10，人员=99，哈希=abcdef')
  assert.match(read('views/dcc/controlled-file/detail/DetailApplicationHistory.vue'), /getDccSignatureOpinionLabel\(row.comment\)/)
})
test('PDF uses original content without another formal stamp and retains trace overlay', () => {
  const source = read('views/dcc/controlled-file/view/index.vue')
  assert.match(source, /await page.render\(/)
  assert.doesNotMatch(source, /drawControlledPreviewStamp/)
  assert.match(source, /backgroundImage: watermarkBackground/)
})
test('actual PDF render loop paints the supplied pages once without painting a second stamp', async () => {
  const source = parse(read('views/dcc/controlled-file/view/index.vue')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('viewer.ts', source, ts.ScriptTarget.Latest, true)
  const fn = ast.statements.find(s => s.declarationList?.declarations.some(d => d.name.text === 'renderPdfPages')).getText(ast)
  let rendered = 0
  const context = new Proxy({}, { get: (_, key) => { throw Error('Unexpected additional canvas drawing: ' + String(key)) } })
  const page = { getViewport: () => ({ width: 600, height: 800 }), cleanup() {},
    render: options => { assert.equal(options.canvasContext, context); rendered++; return { promise: Promise.resolve() } } }
  const scope = { exports: {}, getDocument: () => ({ promise: Promise.resolve({ numPages: 1, getPage: async () => page }) }),
    clonePdfBytesForWorker: x => x.slice(), pdfLoadingTask: {}, pdfDocument: {}, pageEntries: {},
    nextTick: async () => {}, isCurrentRenderVersion: () => true, window: { devicePixelRatio: 1 },
    canvasRefMap: new Map([[1, { style: {}, getContext: () => context }]]) }
  vm.runInNewContext(ts.transpileModule(fn + '\nexports.render=renderPdfPages', { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, scope)
  await scope.exports.render(new Uint8Array([37, 80, 68, 70]), 1, 1)
  assert.equal(rendered, 1)
})
test('BPM summary selects the original round and task before metadata', () => {
  assert.match(read('views/bpm/processInstance/detail/index.vue'), /getControlledFile\(selectedFileId, \{\s*bpmRound: selectedInstance.id,\s*taskId: summaryTaskId/)
})
test('initial detail render does not resolve an identity before the file is loaded', () => {
  const raw = read('views/dcc/controlled-file/detail/index.vue')
  assert.doesNotMatch(raw, /:body-read-context="resolveDetailBodyReadContext\(\)"/)
  const ast = ts.createSourceFile('detail.ts', parse(raw).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(s => s.declarationList?.declarations.some(d => d.name.text === 'detailBodyPreviewState')).getText(ast)
  const scope = { exports: {}, computed: require('vue').computed, fileDetail: { value: undefined },
    isWorkingBrowserDetailCurrent: { value: false }, resolveDetailBodyReadContext: () => { throw Error('must not run') } }
  vm.runInNewContext(ts.transpileModule(declaration + '\nexports.state=detailBodyPreviewState', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, scope)
  assert.equal(scope.exports.state.value.context, undefined)
  assert.equal(scope.exports.state.value.error, '')
})
test('changed Vue components compile and narrow action cells wrap', () => {
  for (const file of ['views/bpm/processInstance/detail/index.vue', 'views/dcc/controlled-file/detail/index.vue',
    'views/dcc/controlled-file/detail/ControlledFileAttachments.vue', 'views/dcc/controlled-file/detail/DetailApplicationHistory.vue',
    'views/dcc/controlled-file/browser/index.vue', 'views/dcc/controlled-file/view/index.vue']) {
    const { descriptor, errors } = parse(read(file), { filename: file }); assert.deepEqual(errors, [])
    const script = compileScript(descriptor, { id: file })
    const result = compileTemplate({ source: descriptor.template.content, filename: file, id: file,
      compilerOptions: { bindingMetadata: script.bindings } })
    assert.deepEqual(result.errors, [])
  }
  const css = read('views/dcc/controlled-file/browser/index.vue').match(/\.browser-row-actions \{([^}]+)\}/)[1]
  assert.match(css, /flex-wrap: wrap/); assert.doesNotMatch(css, /grid-template-columns/)
  assert.match(read('views/dcc/controlled-file/detail/index.vue'), /主文档预览/)
})
