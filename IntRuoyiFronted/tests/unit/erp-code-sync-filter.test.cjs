const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/erp/components/useErpCodeSyncFilter.ts'), 'utf8')
const context = { exports: {} }
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, context)
const { useErpCodeSyncFilter } = context.exports

test('successful code synchronization clears conflicting filters and preserves page size', async () => {
  const query = { pageNo: 8, pageSize: 20, sourceBillNo: 'OLD', materialName: 'Other', quickFilter: { value: 'Old' } }
  const state = { value: 'Other', conditions: [{ key: 'materialName' }], appliedConditions: [{ key: 'materialName' }], activeConditionId: 'old' }
  let reloadCount = 0
  let columnFiltersCleared = false
  const filter = useErpCodeSyncFilter(query, state, async () => {
    reloadCount++
    assert.equal(query.syncCode, 'BILL001')
    assert.equal(query.pageNo, 1)
    assert.equal(query.materialName, undefined)
    assert.equal(query.sourceBillNo, undefined)
    assert.equal(query.quickFilter, undefined)
    assert.equal(columnFiltersCleared, true)
  }, () => { columnFiltersCleared = true })
  await filter.handleCodeSynced('BILL001')
  assert.equal(query.pageSize, 20)
  assert.equal(state.conditions.length, 0)
  assert.equal(state.appliedConditions.length, 0)
  assert.equal(reloadCount, 1)
})

test('show all removes the exact code restriction', async () => {
  const query = { pageNo: 2, pageSize: 10, syncCode: 'BILL001' }
  let reloaded = false
  const filter = useErpCodeSyncFilter(query, { appliedConditions: [] }, async () => { reloaded = true }, () => {})
  await filter.clearCodeSyncFilter()
  assert.equal(query.syncCode, undefined)
  assert.equal(query.pageNo, 1)
  assert.equal(reloaded, true)
})

test('list refresh failure propagates to the synchronization result', async () => {
  const failure = new Error('List refresh failed')
  const filter = useErpCodeSyncFilter({ pageNo: 1, pageSize: 10 }, { appliedConditions: [] }, async () => { throw failure }, () => {})
  await assert.rejects(filter.handleCodeSynced('BILL001'), failure)
})

test('all eight list entries and the synchronization dialog compile', () => {
  const pages = ['erp/product/product', 'erp/stock/stock', 'erp/production/bom-list',
    'erp/production/material-list', 'erp/production/pick-list', 'erp/production/replenishment-list',
    'erp/production/inventory-list', 'mes/pro/workorder'].map(page => `src/views/${page}/index.vue`)
  for (const filename of [...pages, 'src/views/erp/components/SyncByCodeButton.vue']) {
    const sfc = fs.readFileSync(path.join(root, filename), 'utf8')
    const parsed = parse(sfc, { filename })
    assert.equal(parsed.errors.length, 0, filename)
    compileScript(parsed.descriptor, { id: filename })
    const template = compileTemplate({ source: parsed.descriptor.template.content, filename, id: filename })
    assert.equal(template.errors.length, 0, `${filename}: ${template.errors}`)
  }
})
