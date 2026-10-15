const { test } = require('node:test'), assert = require('node:assert/strict'), fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const root = require('node:path').resolve(__dirname, '../..')
const read = file => fs.readFileSync(root + '/src/api/dcc/controlledFile/' + file, 'utf8')
function load(fail = false) {
  const h = { exports: {}, Error, BigInt, TextEncoder, TextDecoder, btoa, atob, Object }
  vm.runInNewContext(ts.transpileModule(read('bodyReadContext.ts'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, h)
  const src = read('workflow.ts'), ast = ts.createSourceFile('workflow.ts', src, ts.ScriptTarget.Latest, true), calls = { metadata: [], binary: [] }
  const wanted = ['getControlledFilePreviewMetadata', 'previewControlledFileWithWatermark']
  const chunks = wanted.map(name => ast.statements.find(s => s.declarationList?.declarations.some(d => d.name.text === name)).getText(ast))
  const c = { exports: {}, ...h.exports, Error, axiosConfig: { base_url: '/admin-api', request_timeout: 1000 },
    request: { get: async req => { calls.metadata.push(req); if (fail) throw new Error('真实metadata失败'); return { viewerToken: 'token', viewerTokenId: 'id', viewerTokenNonce: 'nonce', accessEventCode: 'event', watermarkTraceCode: 'trace' } } },
    axios: { get: async (url, config) => { calls.binary.push({ url, config }); return { data: 'blob', headers: {} } } },
    parseControlledFilePreviewMetadata: x => x, assertDistributionPreviewRequest() {}, assertDistributionPreviewMetadata() {},
    buildControlledFileBinaryHeaders: x => ({ Authorization: 'Bearer credential', ...x }), decodePreviewWatermark: () => 'wm', getAxiosHeader: () => 'wm',
    DCC_VIEWER_TOKEN_HEADER: 'token', DCC_VIEWER_TOKEN_ID_HEADER: 'id', DCC_VIEWER_TOKEN_NONCE_HEADER: 'nonce', DCC_ACCESS_EVENT_CODE_HEADER: 'event', DCC_WATERMARK_TRACE_CODE_HEADER: 'trace',
    CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER: 'wm' }
  vm.runInNewContext(ts.transpileModule(chunks.join('\n'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return { api: c.exports, calls }
}
test('actual wrappers carry identical canonical task context from metadata to binary with existing headers', async () => {
  const p = load(), ctx = { intent: 'APPROVAL_TASK', controlledFileId: '20', expectedVersionNo: 'B/1', applicationType: 'REVISION', bpmRound: 'R', taskId: 'T' }
  await p.api.previewControlledFileWithWatermark('20', undefined, undefined, ctx)
  const wire = p.calls.metadata[0].params.bodyReadContext
  assert.equal(p.calls.binary[0].config.headers['X-DCC-Body-Read-Context'], wire)
  assert.equal(p.calls.binary[0].config.headers.Authorization, 'Bearer credential'); assert.equal(p.calls.binary[0].config.headers.token, 'token')
  assert.equal(JSON.parse(Buffer.from(wire, 'base64url').toString()).taskId, 'T')
})
test('missing context is strict DAILY while dispatch carries only its independent identity', async () => {
  const p = load(); await p.api.previewControlledFileWithWatermark('20')
  assert.equal(JSON.parse(Buffer.from(p.calls.metadata[0].params.bodyReadContext, 'base64url')).intent, 'DAILY')
  const dispatch = { controlledFileId: '20', distributionId: '7', recipientId: '8', versionNo: 'B/1', approvalProcessInstanceId: 'R' }
  await p.api.previewControlledFileWithWatermark('20', undefined, dispatch)
  assert.equal(p.calls.metadata[1].params.bodyReadContext, undefined); assert.equal(p.calls.binary[1].config.headers['X-DCC-Body-Read-Context'], undefined)
  await assert.rejects(p.api.previewControlledFileWithWatermark('20', undefined, dispatch, { intent: 'DAILY', controlledFileId: '20' }))
  await assert.rejects(p.api.getControlledFilePreviewMetadata('20', dispatch, { intent: 'DAILY', controlledFileId: '20' }))
})
test('invalid context and metadata error produce no fallback binary request', async () => {
  const p = load(); await assert.rejects(p.api.previewControlledFileWithWatermark('20', undefined, undefined, { intent: 'DAILY', controlledFileId: '21' }))
  assert.equal(p.calls.metadata.length, 0); assert.equal(p.calls.binary.length, 0)
  const error = load(true); await assert.rejects(error.api.previewControlledFileWithWatermark('20'), /真实metadata失败/)
  assert.equal(error.calls.metadata.length, 1); assert.equal(error.calls.binary.length, 0)
})
