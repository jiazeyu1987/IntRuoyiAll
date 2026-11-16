import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import test from 'node:test'
import ts from 'typescript'

const source = fs.readFileSync(new URL('../src/components/UploadFile/src/useUpload.ts', import.meta.url), 'utf8')
const compile = (value) => ts.transpileModule(value, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const info = { configId: 10, path: 'tenants/11/a.pdf', url: 'https://storage.invalid/a.pdf', uploadUrl: 'https://storage.invalid/upload' }
const options = { file: { name: 'a.pdf', size: 5, type: 'application/pdf' }, onProgress: () => {} }

function load(mode = 'client') {
  const revision = { value: 0 }
  const calls = []
  const api = {
    getFilePresignedUrl: async (...args) => { calls.push(['presign', ...args]); return info },
    createFile: async (...args) => { calls.push(['create', ...args]); return 1 },
    updateFile: async (...args) => { calls.push(['upload', ...args]); return { code: 0, data: info.url } }
  }
  const storage = { put: async (...args) => { calls.push(['put', ...args]) } }
  const context = vm.createContext({ exports: {}, env: { VITE_UPLOAD_TYPE: mode },
    require: (name) => {
      if (name === '@/api/infra/file') return api
      if (name === '@/utils/auth') return { authContextRevision: revision }
      if (name === 'axios') return { default: storage }
      throw new Error(`Unexpected import: ${name}`)
    }
  })
  vm.runInContext(compile(source.replaceAll('import.meta.env', 'env')), context)
  return { upload: context.exports.useUpload('docs').httpRequest, api, storage, calls, revision }
}

test('direct upload waits for metadata registration and propagates registration errors', async () => {
  const state = load()
  const gate = deferred()
  const started = deferred()
  state.api.createFile = (...args) => { started.resolve(args); return gate.promise }
  let finished = false
  const pending = state.upload(options).finally(() => { finished = true })
  const [record, guard] = await started.promise
  assert.equal(record.path, info.path)
  assert.equal(guard(), true)
  assert.equal(finished, false)
  gate.resolve(1)
  assert.equal((await pending).data, info.url)
  state.api.createFile = async () => { throw new Error('metadata unavailable') }
  await assert.rejects(state.upload(options), /metadata unavailable/)
})

for (const stage of ['presign', 'put', 'create', 'server']) {
  test(`tenant switch during ${stage} cannot publish old upload results`, async () => {
    const state = load(stage === 'server' ? 'server' : 'client')
    const gate = deferred()
    const started = deferred()
    const owner = stage === 'put' ? state.storage : state.api
    const name = { presign: 'getFilePresignedUrl', put: 'put', create: 'createFile', server: 'updateFile' }[stage]
    owner[name] = (...args) => { started.resolve(args); return gate.promise }
    const pending = state.upload(options)
    const args = await started.promise
    state.revision.value++
    if (stage !== 'put') assert.equal(args.at(-1)(), false, 'HTTP requests and auth refresh replay retain the original context guard')
    gate.resolve(stage === 'presign' ? info : stage === 'server' ? { code: 0, data: info.url } : 1)
    await assert.rejects(pending, /租户已切换/)
    if (stage === 'presign') assert.equal(state.calls.some(([kind]) => kind === 'put'), false)
    if (stage === 'presign' || stage === 'put') assert.equal(state.calls.some(([kind]) => kind === 'create'), false)
  })
}

test('file API passes context guards to the authenticated request interceptor', async () => {
  const calls = []
  const request = Object.fromEntries(['get', 'post', 'upload'].map((method) => [method, async (value) => { calls.push(value) }]))
  const context = vm.createContext({ exports: {}, require: () => ({ default: request }) })
  vm.runInContext(compile(fs.readFileSync(new URL('../src/api/infra/file/index.ts', import.meta.url), 'utf8')), context)
  const guard = () => false
  await context.exports.getFilePresignedUrl('a.pdf', 'docs', guard)
  await context.exports.createFile({}, guard)
  await context.exports.updateFile({}, () => {}, guard)
  assert.equal(calls.length, 3)
  for (const call of calls) assert.equal(call.isRequestContextCurrent, guard)
})
