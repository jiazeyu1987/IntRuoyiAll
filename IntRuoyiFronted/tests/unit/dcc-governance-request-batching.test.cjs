const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')

function load(get) {
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/api/mes/pro/dccProjectGovernance.ts'), 'utf8')
    .replace("import request from '@/config/axios'", '')
  const js = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  const exports = {}
  Function('exports', 'request', js)(exports, { get })
  return exports.getDccProjectGovernanceStatus
}

test('100 Chinese project names use bounded URLs and preserve every name and option', async () => {
  const urls = []
  const names = Array.from({ length: 100 }, (_, i) => `一次性使用血栓抽吸系统（测试项目${i}）`)
  const get = load(async ({ url }) => {
    urls.push(url)
    return new URL(url, 'http://localhost').searchParams.getAll('projectNames')
      .map(projectName => ({ projectName, routeStatus: 'OK' }))
  })
  const result = await get(names, {
    routeStatusRequired: true, mainBatchRecordStatusRequired: true, formSlotStatusRequired: false
  })
  assert.ok(urls.length > 1)
  for (const url of urls) {
    assert.ok(url.length <= 4000)
    const params = new URL(url, 'http://localhost').searchParams
    assert.equal(params.get('routeStatusRequired'), 'true')
    assert.equal(params.get('mainBatchRecordStatusRequired'), 'true')
    assert.equal(params.get('formSlotStatusRequired'), 'false')
  }
  assert.deepEqual(result.map(row => row.projectName), names)
})

test('short requests preserve special characters and omitted options', async () => {
  const names = ['测试 & + / （FDA）', '测试2项目权限验证20261115']
  let calls = 0
  await load(async ({ url }) => {
    calls++
    const params = new URL(url, 'http://localhost').searchParams
    assert.deepEqual(params.getAll('projectNames'), names)
    assert.equal(params.has('formSlotStatusRequired'), false)
    return []
  })(names)
  assert.equal(calls, 1)
})

test('a failed batch rejects the whole lookup instead of returning partial configuration', async () => {
  let calls = 0
  const failure = new Error('HTTP 400')
  const get = load(async () => {
    if (++calls === 2) throw failure
    return [{ projectName: '已完成批次', routeStatus: 'OK' }]
  })
  await assert.rejects(get(Array.from({ length: 100 }, (_, i) => `一次性使用血栓抽吸系统${i}`)), error => error === failure)
  assert.ok(calls > 1)
})

test('empty input sends no request and oversized single names fail before any request', async () => {
  let calls = 0
  const get = load(async () => { calls++; return [] })
  assert.deepEqual(await get([]), [])
  await assert.rejects(get(['项目'.repeat(300)]), /项目名称过长/)
  assert.equal(calls, 0)
})
