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
    loadHistoryPage([{ load: async (page) => ({ list: [], total: page === 1 ? 30 : 31 }) }, { load: async () => ({ list: [], total: 0 }) }], 2, 10),
    /数量已变化/
  )
  const calls = []
  let rows = [
    { id: 1, batchExecutionCode: 'B1', status: 40, releaseStatus: 'RELEASED', startedAt: '2026-10-25 09:00:00', pqcReleasedAt: '2026-10-26 09:00:00', releaseApprovedAt: '2026-10-27 09:00:00' },
    { id: 2, batchExecutionCode: 'B2', status: 30, releaseStatus: 'RELEASED', releaseApprovedAt: '2026-10-27 10:00:00' },
    { id: 3, batchExecutionCode: 'B3', status: 60, releaseStatus: 'RELEASED', updateTime: '2026-10-27 11:00:00' },
    { id: 4, batchExecutionCode: 'B4', status: 60, updateTime: '2026-10-27 12:00:00' },
    { id: 5, batchExecutionCode: 'B5', status: 40 }
  ]
  const { buildHistorySources } = load('historySources.ts', {
    '@/api/mes/pro/edhr/batchExecution': {
      getEdhrBatchHistoryPage: async (params) => {
        calls.push(params)
        return { list: rows, total: 25 }
      }
    }
  })
  const filters = {
    workOrderCode: ' WO-1 ', productName: ' 产品 ', batchCode: ' BATCH-01 ',
    tracePerson: ' 张三 ', traceEquipment: ' EQ-01 ', traceMaterial: ' MAT-01 ',
    traceMaterialLot: ' LOT-01 '
  }
  const result = await loadHistoryPage(buildHistorySources(filters, () => true), 3, 10)
  assert.equal(calls.length, 1, '历史列表翻页只请求目标页，不重复请求第一页')
  assert.equal(calls[0].pageNo, 3)
  assert.equal(result.total, 25)
  assert.deepEqual(result.list.map(row => row.statusLabel), ['已放行', '已放行', '已作废', '已作废', '已放行'])
  assert.equal(result.list[0].startedAt, '2026-10-25 09:00:00')
  assert.equal(result.list[0].pqcReleasedAt, '2026-10-26 09:00:00')
  assert.equal(result.list[0].releaseApprovedAt, '2026-10-27 09:00:00')
  assert.equal(result.list[0].occurredAt, undefined)
  assert.equal(result.list[0].batchExecutionId, 1)
  assert.equal(result.list[0].recordCode, 'B1')
  assert.equal(calls[0].releasedOnly, undefined, '不能以已放行条件排除未放行的作废批次')
  assert.equal(calls[0].completedTraceOnly, true, '请求必须明确限定终态批记录')
  assert.deepEqual(calls[0].excludeStatuses, [50], '历史追溯不包含已返工批记录')
  for (const field of ['workOrderCode', 'productName', 'batchCode', 'tracePerson', 'traceEquipment', 'traceMaterial', 'traceMaterialLot']) {
    assert.equal(calls[0][field], filters[field].trim())
  }
  assert.equal(calls[0].traceBusinessTime, undefined)
  const releaseApprovedTime = ['2026-10-01 00:00:00', '2026-10-26 23:59:59']
  await loadHistoryPage(buildHistorySources({ releaseApprovedTime }, () => true), 1, 10)
  assert.deepEqual(calls[1].releaseApprovedTime, releaseApprovedTime)
  assert.equal(calls[1].releasedOnly, true, '放行时间筛选必须启用对应服务端条件')
  assert.throws(() => buildHistorySources({}, () => false), /权限/)
  for (const row of [{ status: 50 }, { status: 50, releaseStatus: 'RELEASED' }, { status: 10 }, { status: 25 }, { status: 30 }]) {
    rows = [{ id: 10, ...row }]
    await assert.rejects(loadHistoryPage(buildHistorySources({}, () => true), 1, 10), /非已放行或已作废/)
  }
  const page = fs.readFileSync(path.join(root, 'BatchRecordHistoryPage.vue'), 'utf8')
  assert.doesNotMatch(page, /v-model="queryParams.recordType"/)
  assert.match(page, /<UnifiedListTemplate/)
  assert.match(page, /useUserTableColumns/)
  assert.match(page, /key: 'startedAt', label: '开始时间'/)
  assert.match(page, /key: 'pqcReleasedAt', label: 'PQC放行时间'/)
  assert.match(page, /key: 'releaseApprovedAt', label: '上市放行时间'/)
  assert.doesNotMatch(page, /业务时间|traceBusinessTime|occurredAt/)
  for (const field of ['tracePerson', 'traceEquipment', 'traceMaterial', 'traceMaterialLot']) {
    assert.ok(page.includes(`queryParamKey: '${field}'`), `${field} remains available in standard filters`)
  }
  console.log('PASS: history terminal labels, void precedence, single-request pagination, filters and permissions')
}
main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
