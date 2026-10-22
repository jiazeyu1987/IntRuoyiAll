const fs = require('node:fs'),
  path = require('node:path'),
  ts = require('typescript'),
  vm = require('node:vm')
const root = path.resolve(__dirname, '../..'),
  read = (file) => fs.readFileSync(path.join(root, file), 'utf8')
const compile = (source) =>
  ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
module.exports = function loadFixedVersionPreview() {
  const body = { exports: {}, TextEncoder, TextDecoder, btoa, atob, Error, BigInt, Object }
  vm.runInNewContext(compile(read('src/api/dcc/controlledFile/bodyReadContext.ts')), body)
  const calls = { metadata: [], binary: [] },
    source = read('src/api/dcc/controlledFile/workflow.ts')
  const ast = ts.createSourceFile('workflow.ts', source, ts.ScriptTarget.Latest, true)
  const chunks = ['getControlledFilePreviewMetadata', 'previewControlledFileWithWatermark'].map(
    (name) =>
      ast.statements
        .find((s) => s.declarationList?.declarations.some((d) => d.name.text === name))
        .getText(ast)
  )
  const workflow = {
    exports: {},
    ...body.exports,
    Error,
    axiosConfig: { base_url: '/admin-api', request_timeout: 1000 },
    request: {
      get: async (req) => {
        calls.metadata.push(req)
        return {
          viewerToken: 'token',
          viewerTokenId: 'id',
          viewerTokenNonce: 'nonce',
          accessEventCode: 'event',
          watermarkTraceCode: 'trace'
        }
      }
    },
    axios: {
      get: async (url, config) => {
        calls.binary.push({ url, config })
        return { data: 'blob', headers: {} }
      }
    },
    parseControlledFilePreviewMetadata: (value) => value,
    assertDistributionPreviewRequest() {},
    assertDistributionPreviewMetadata() {},
    buildControlledFileBinaryHeaders: (value) => ({ Authorization: 'test-only', ...value }),
    decodePreviewWatermark: () => 'wm',
    getAxiosHeader: () => 'wm',
    DCC_VIEWER_TOKEN_HEADER: 'token',
    DCC_VIEWER_TOKEN_ID_HEADER: 'id',
    DCC_VIEWER_TOKEN_NONCE_HEADER: 'nonce',
    DCC_ACCESS_EVENT_CODE_HEADER: 'event',
    DCC_WATERMARK_TRACE_CODE_HEADER: 'trace',
    CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER: 'wm'
  }
  vm.runInNewContext(compile(chunks.join('\n')), workflow)
  const common = {
    exports: {},
    require: (dep) => {
      if (dep.endsWith('/workflow')) return workflow.exports
      if (dep.endsWith('/bodyReadContext')) return body.exports
      if (dep === 'axios') return { __esModule: true, default: workflow.axios }
      if (dep === '@/config/axios') return { __esModule: true, default: workflow.request }
      if (dep === '@/config/axios/config') return { config: workflow.axiosConfig }
      if (dep === '@/utils') return { generateUUID: () => 'test-request' }
      throw Error(dep)
    }
  }
  vm.runInNewContext(compile(read('src/api/common/filePreview.ts')), common)
  const view = { exports: {}, URLSearchParams }
  vm.runInNewContext(compile(read('src/views/dcc/controlled-file/view/presentation.ts')), view)
  return { body: body.exports, common: common.exports, view: view.exports, calls }
}
