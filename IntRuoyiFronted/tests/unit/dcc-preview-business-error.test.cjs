const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync('src/api/dcc/controlledFile/workflow.ts', 'utf8')
const ast = ts.createSourceFile('workflow.ts', source, ts.ScriptTarget.Latest, true)
const names = ['DccControlledFileContractError', 'getAxiosHeader',
  'assertControlledFileDownloadBusinessResult', 'previewControlledFileWithWatermark']
const selected = ast.statements.filter(s => names.includes(s.name?.text) ||
  s.declarationList?.declarations.some(d => names.includes(d.name?.text)))
function host(response) {
  const context = {
    Blob, Error, JSON, Number, String, Array,
    axiosConfig: { base_url: '/admin-api', request_timeout: 1000 },
    axios: { get: async () => response },
    normalizeDccBodyReadContext: () => undefined,
    buildDccBodyReadHeaders: () => ({}),
    buildControlledFileBinaryHeaders: x => x,
    DCC_VIEWER_TOKEN_HEADER: 'token', DCC_VIEWER_TOKEN_ID_HEADER: 'id',
    DCC_VIEWER_TOKEN_NONCE_HEADER: 'nonce', DCC_ACCESS_EVENT_CODE_HEADER: 'event',
    DCC_WATERMARK_TRACE_CODE_HEADER: 'trace', CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER: 'watermark',
    watermarkReads: 0,
    decodePreviewWatermark: value => {
      context.watermarkReads++
      if (!value) throw new Error('missing watermark')
      return { text: value }
    }
  }
  const code = selected.map(s => s.getText(ast).replace(/^export /, '')).join('\n') +
    '\nthis.preview = previewControlledFileWithWatermark'
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context
}
test('preview reports the actual storage business error before checking watermark headers', async () => {
  const h = host({ data: new Blob([JSON.stringify({ code: 1080000374, msg: '正文读取失败', data: { storage: 'unavailable' } })]), headers: { 'content-type': 'application/json;charset=UTF-8' } })
  await assert.rejects(h.preview('42', {}), e => e.code === 1080000374 && e.message === '正文读取失败' && e.details.storage === 'unavailable')
  assert.equal(h.watermarkReads, 0)
})
test('invalid JSON preview errors fail explicitly before watermark validation', async () => {
  for (const body of ['invalid', JSON.stringify({ code: 0, msg: 'success' })]) {
    const h = host({ data: new Blob([body], { type: 'application/json' }), headers: {} })
    await assert.rejects(h.preview('42', {}), /预览错误响应/)
    assert.equal(h.watermarkReads, 0)
  }
})
test('binary preview still requires watermark and keeps the original content', async () => {
  const blob = new Blob(['%PDF-1.7 content'], { type: 'application/pdf' })
  const h = host({ data: blob, headers: { watermark: 'controlled watermark' } })
  const result = await h.preview('42', { watermarkTraceCode: 'trace-42' })
  assert.equal(result.blob, blob)
  assert.equal(result.watermark.traceCode, 'trace-42')
  const missing = host({ data: blob, headers: {} })
  await assert.rejects(missing.preview('42', {}), /missing watermark/)
})
