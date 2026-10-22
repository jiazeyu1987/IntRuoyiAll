const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const vm = require('node:vm')
const ts = require('typescript')

const page = fs.readFileSync(
  path.join(__dirname, '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const start = page.indexOf('const enrichSubmissionMaterialNames = async')
const end = page.indexOf('const resolvePqcItemSnapshotDetails', start)
assert.ok(start >= 0 && end > start)
const source = ts.transpileModule(page.slice(start, end), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None }
}).outputText

function setup(getItemNames, cached = {}) {
  const cache = { value: cached }
  const context = vm.createContext({
    MdItemApi: { getItemNames },
    submissionMaterialNameCache: cache,
    resolveProductionMaterialRows: (row) => row.materialDetails,
    isPlaceholderMaterialName: (name) => name === '物料名称未记录' || /^物料\s*\d+$/.test(name)
  })
  vm.runInContext(source + '\nthis.enrich = enrichSubmissionMaterialNames', context)
  return { enrich: context.enrich, cache }
}

test('deduplicates missing materials into one batch and reuses cached names', async () => {
  let calls = 0
  const { enrich, cache } = setup(async (ids) => {
    calls++
    assert.deepEqual(Array.from(ids), [11, 12])
    return { 11: '弹簧', 12: '导管' }
  }, { 13: '已有名称' })
  const rows = [{ materialDetails: [
    { materialId: 11 }, { materialId: 11 }, { materialId: 12, materialName: '物料12' },
    { materialId: 13 }, { materialId: 14, materialName: '已记录名称' }
  ] }]
  await enrich(rows)
  await enrich(rows)
  assert.equal(calls, 1)
  assert.equal(cache.value['11'], '弹簧')
  assert.equal(cache.value['12'], '导管')
})

test('does not request names when all material snapshots are complete', async () => {
  const { enrich } = setup(() => assert.fail('unexpected name lookup'))
  await enrich([{ materialDetails: [{ materialId: 11, materialName: '弹簧' }] }])
  await enrich([])
})

test('missing batch result fails without caching a partial result', async () => {
  const { enrich, cache } = setup(async () => ({ 11: '弹簧' }))
  await assert.rejects(
    enrich([{ materialDetails: [{ materialId: 11 }, { materialId: 12 }] }]),
    /物料 12 缺少真实物料名称/
  )
  assert.deepEqual(cache.value, {})
})

test('lookup failure propagates to the page error handler', async () => {
  const failure = new Error('物料服务不可用')
  const { enrich } = setup(async () => { throw failure })
  await assert.rejects(enrich([{ materialDetails: [{ materialId: 11 }] }]), (e) => e === failure)
})
