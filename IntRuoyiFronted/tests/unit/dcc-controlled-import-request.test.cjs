const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')

function load(upload) {
  const exports = {}
  const source = fs.readFileSync('src/api/dcc/controlledFile/controlledImports.ts', 'utf8')
  new Function('exports', 'require', ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText)(exports, id => {
    assert.equal(id, '@/config/axios')
    return { default: { upload } }
  })
  return exports
}

test('controlled import waits for the bounded synchronous conversion and download, preserving the real result', async () => {
  let command
  const data = new FormData()
  const result = { id: '61', importSource: 'CONTROLLED_IMPORT' }
  const api = load(async value => { command = value; return { data: result } })
  assert.equal(await api.importControlledFile(data), result)
  assert.equal(command.url, '/dcc/controlled-file-imports')
  assert.equal(command.data, data)
  assert.equal(command.timeout, 2 * 300_000)
})

test('conversion or transport failures remain failures without retrying or inventing an import identity', async () => {
  const failure = new Error('OnlyOffice conversion failed')
  let calls = 0
  const api = load(async () => { calls++; throw failure })
  await assert.rejects(api.importControlledFile(new FormData()), error => error === failure)
  assert.equal(calls, 1)
  await assert.rejects(load(async () => ({ data: {} })).importControlledFile(new FormData()), /缺少正式文件身份/)
})
