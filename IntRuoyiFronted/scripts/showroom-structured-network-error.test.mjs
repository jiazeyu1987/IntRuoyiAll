import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'

const root = process.cwd()

const loadStructuredErrorModule = () => {
  const sourcePath = path.join(root, 'src/views/showroom-admin/shared/structuredError.ts')
  const source = fs.readFileSync(sourcePath, 'utf8')
  const transpiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020
    }
  }).outputText
  const module = { exports: {} }
  vm.runInNewContext(transpiled, {
    module,
    exports: module.exports
  })
  return module.exports
}

test('showroom network failures include request target and diagnosis instead of bare Network Error', () => {
  const { formatShowroomStructuredError } = loadStructuredErrorModule()

  const formatted = formatShowroomStructuredError(
    {
      message: 'Network Error',
      config: {
        method: 'post',
        baseURL: 'http://localhost:48081/admin-api',
        url: '/showroom/release/publish'
      }
    },
    '展厅发布'
  )

  assert.match(formatted, /^展厅发布失败：NETWORK_RESPONSE_UNAVAILABLE/m)
  assert.match(formatted, /请求：POST http:\/\/localhost:48081\/admin-api\/showroom\/release\/publish/)
  assert.match(formatted, /浏览器没有收到后端响应/)
  assert.match(formatted, /后端服务、网络连通、CORS 或反向代理/)
  assert.match(formatted, /原始错误：Network Error/)
  assert.notEqual(formatted.trim(), '展厅发布失败：Network Error')
})

test('showroom structured backend failures keep existing backend code formatting', () => {
  const { formatShowroomStructuredError } = loadStructuredErrorModule()

  const formatted = formatShowroomStructuredError(
    {
      message: 'SHOWROOM_RELEASE_SOURCE_MISSING: failed to read file',
      data: {
        backendErrorCode: 'SHOWROOM_RELEASE_SOURCE_MISSING',
        targetType: 'COMPANY',
        targetId: 16,
        fileId: 28,
        endpoint: '/showroom/release/publish'
      }
    },
    '展厅发布'
  )

  assert.match(formatted, /^展厅发布失败：SHOWROOM_RELEASE_SOURCE_MISSING/m)
  assert.match(formatted, /目标：COMPANY #16/)
  assert.match(formatted, /资源：fileId=28/)
  assert.match(formatted, /接口：\/showroom\/release\/publish/)
})
