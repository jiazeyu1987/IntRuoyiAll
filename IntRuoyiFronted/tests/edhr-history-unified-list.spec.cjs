const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const root = path.resolve(__dirname, '../src/views/mes/pro/edhr-batch')
function load(file, dependencies = {}) {
  const source = fs.readFileSync(path.join(root, file), 'utf8')
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS }
  }).outputText
  const module = { exports: {} }
  new Function('require', 'module', 'exports', code)(
    (name) => {
      if (!(name in dependencies)) throw new Error('Unexpected dependency: ' + name)
      return dependencies[name]
    },
    module,
    module.exports
  )
  return module.exports
}
const { loadHistoryPage } = load('historyPagination.ts')
async function main() {
  const groups = [
    Array.from({ length: 13 }, (_, i) => 'a' + i),
    [],
    ['b0', 'b1'],
    Array.from({ length: 29 }, (_, i) => 'c' + i),
    ['d0']
  ]
  const sources = groups.map((rows) => ({
    load: async (page, size) => ({
      list: rows.slice((page - 1) * size, page * size),
      total: rows.length
    })
  }))
  const expected = groups.flat()
  for (const size of [1, 7, 10, 20]) {
    const actual = []
    for (let page = 1; page <= Math.ceil(expected.length / size); page++) {
      const result = await loadHistoryPage(sources, page, size)
      assert.equal(result.total, expected.length)
      actual.push(...result.list)
    }
    assert.deepEqual(actual, expected, '跨类型、跨服务端页不得重复或漏行')
  }
  assert.deepEqual(await loadHistoryPage([], 1, 10), { list: [], total: 0 })
  await assert.rejects(
    loadHistoryPage(
      [
        {
          load: async () => {
            throw new Error('读取失败')
          }
        }
      ],
      1,
      10
    ),
    /读取失败/
  )
  await assert.rejects(
    loadHistoryPage([{ load: async (page) => ({ list: [], total: page === 1 ? 30 : 31 }) }], 2, 10),
    /数量已变化/
  )
  const calls = []
  const api = (name) => async (params) => {
    calls.push({ name, params })
    return {
      list: [
        {
          id: 1,
          applicationId: '1',
          deviationCode: 'D1',
          reviewCode: 'N1',
          batchExecutionCode: 'B1'
        }
      ],
      total: 1
    }
  }
  const { buildHistorySources } = load('historySources.ts', {
    '@/api/mes/pro/edhr/batchExecution': { getEdhrBatchExecutionPage: api('batch') },
    '@/api/mes/pro/edhr/nonconformanceReview': { getNonconformanceReviewPage: api('review') },
    '@/api/mes/pro/edhr/deviation': { getDeviationPage: api('deviation') },
    '@/api/mes/pro/productionRelease': {
      getPqcProductionReleasePage: api('voided'),
      PQC_RELEASE_VIEW_VOIDED: 'VOIDED'
    }
  })
  const result = await loadHistoryPage(
    buildHistorySources({ recordType: '', search: '' }, () => true),
    1,
    10
  )
  assert.deepEqual(
    result.list.map((row) => row.typeLabel),
    ['批次执行', '历史追溯']
  )
  assert.equal(new Set(result.list.map((row) => row.key)).size, 2, '不同类型同 ID 不冲突')
  assert.deepEqual(calls[0].params.excludeStatuses, [40, 50, 60])
  assert.equal(calls[0].params.excludeReleased, true)
  assert.equal(calls[1].params.releasedOnly, true)
  assert.deepEqual(calls.map((call) => call.name), ['batch', 'batch'])
  assert.throws(
    () => buildHistorySources({ recordType: '' }, (permissions) =>
      permissions.includes('mes:pro-edhr-deviation:query')),
    /权限/
  )
  for (const recordType of ['deviation', 'nonconformanceReview', 'voided']) {
    assert.throws(() => buildHistorySources({ recordType }, () => true), /权限/)
  }
  assert.throws(
    () => buildHistorySources({ recordType: 'history', search: '' }, () => false),
    /权限/
  )
  calls.length = 0
  await loadHistoryPage(
    buildHistorySources(
      {
        recordType: 'history',
        workOrderCode: ' W1 ',
        releaseApprovedTime: ['start', 'end'],
        search: ''
      },
      () => true
    ),
    1,
    10
  )
  assert.equal(calls.length, 1)
  assert.equal(calls[0].params.workOrderCode, 'W1')
  assert.deepEqual(calls[0].params.releaseApprovedTime, ['start', 'end'])
  const page = fs.readFileSync(path.join(root, 'BatchRecordHistoryPage.vue'), 'utf8')
  assert.doesNotMatch(page, /<EdhrBatchRecordTabs|<el-tab-pane/)
  assert.match(page, /prop="typeLabel" label="类型"/)
  console.log(
    'PASS: unified history pagination, source filters, permissions, errors and type identities'
  )
}
main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
