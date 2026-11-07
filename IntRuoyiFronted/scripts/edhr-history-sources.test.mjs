import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import test from 'node:test'

const require = createRequire(import.meta.url)
const ts = require('typescript')
const source = readFileSync(new URL('../src/views/mes/pro/edhr-batch/historySources.ts', import.meta.url), 'utf8')
const code = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup() {
  const requests = []
  const module = { exports: {} }
  new Function('require', 'module', 'exports', code)((name) => {
    assert.equal(name, '@/api/mes/pro/edhr/batchExecution')
    return { getEdhrBatchHistoryPage: async (request) => {
      requests.push(request)
      return { total: 1, list: [{ id: 1295, status: 40, batchExecutionCode: 'EDHRB-1295' }] }
    } }
  }, module, module.exports)
  return { build: module.exports.buildHistorySources, requests }
}

test('calendar-day search sends complete backend LocalDateTime boundaries and trace filters', async () => {
  const e = setup()
  const dates = ['2026-11-06', '2026-11-06']
  const [source] = e.build({ releaseApprovedTime: dates, tracePerson: ' 陈丽 ', traceEquipment: ' PUMP-01 ', traceMaterial: ' MAT-01 ', traceMaterialLot: ' LOT-01 ' }, () => true)
  const result = await source.load(3, 20)
  assert.deepEqual(e.requests[0].releaseApprovedTime, ['2026-11-06 00:00:00', '2026-11-06 23:59:59'])
  assert.equal(e.requests[0].releasedOnly, true)
  assert.equal(e.requests[0].tracePerson, '陈丽')
  assert.equal(e.requests[0].traceEquipment, 'PUMP-01')
  assert.equal(e.requests[0].traceMaterial, 'MAT-01')
  assert.equal(e.requests[0].traceMaterialLot, 'LOT-01')
  assert.equal(e.requests[0].pageNo, 3)
  assert.deepEqual(dates, ['2026-11-06', '2026-11-06'])
  assert.equal(result.list[0].batchExecutionId, 1295)
})

test('explicit time bounds retain their precision and absent dates add no release restriction', async () => {
  const e = setup()
  const exact = ['2026-11-06 12:00:00', '2026-11-06 13:00:00']
  await e.build({ releaseApprovedTime: exact }, () => true)[0].load(1, 20)
  assert.deepEqual(e.requests[0].releaseApprovedTime, exact)
  await e.build({}, () => true)[0].load(1, 20)
  assert.equal(e.requests[1].releaseApprovedTime, undefined)
  assert.equal(e.requests[1].releasedOnly, undefined)
})

test('missing history query permission blocks reading rather than returning an empty success', () => {
  const e = setup()
  assert.throws(() => e.build({}, () => false), /没有历史追溯记录的查询权限/)
  assert.equal(e.requests.length, 0)
})
