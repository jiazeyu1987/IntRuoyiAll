const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()

const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const syncApi = read('src/api/erp/sync/index.ts')

assert(
  syncApi.includes('runIncrementalSyncJob'),
  'ERP sync API must expose runIncrementalSyncJob for all manual table sync buttons.'
)
assert(
  syncApi.includes("import * as JobApi from '@/api/infra/job'") &&
    syncApi.includes('JobApi.getJobPage') &&
    syncApi.includes('JobApi.runJob'),
  'Manual incremental sync must reuse the existing job trigger path.'
)

const pages = [
  {
    file: 'src/views/erp/product/product/index.vue',
    handler: 'kingdeeProductItemSyncJob',
    forbidden: ['ProductApi.syncKingdeeProducts()']
  },
  {
    file: 'src/views/erp/purchase/order/index.vue',
    handler: 'kingdeePurchaseOrderSyncJob',
    forbidden: ['PurchaseOrderApi.syncKingdeePurchaseOrders()']
  },
  {
    file: 'src/views/erp/sale/order/index.vue',
    handler: 'kingdeeSaleOrderSyncJob',
    forbidden: ['SaleOrderApi.syncKingdeeSaleOrders()']
  },
  {
    file: 'src/views/erp/stock/stock/index.vue',
    handler: 'kingdeeStockSyncJob',
    forbidden: ['StockApi.syncKingdeeStocks()']
  },
  {
    file: 'src/views/mes/pro/workorder/index.vue',
    handler: 'kingdeeProductionOrderSyncJob',
    forbidden: ['ProWorkOrderApi.syncKingdeeWorkOrders()']
  },
  {
    file: 'src/views/mes/md/item/index.vue',
    handler: 'kingdeeProductItemSyncJob',
    forbidden: ['MdItemApi.syncKingdeeItems()']
  },
  {
    file: 'src/views/erp/production/material-list/index.vue',
    handler: 'kingdeeProductionMaterialListSyncJob',
    forbidden: ['syncProductionMaterialList()', '/erp/production-material-list/sync-kingdee']
  },
  {
    file: 'src/views/erp/production/bom-list/index.vue',
    handler: 'kingdeeBomSyncJob',
    forbidden: []
  },
  {
    file: 'src/views/erp/production/inventory-list/index.vue',
    handler: 'kingdeeStockSyncJob',
    forbidden: []
  }
]

for (const page of pages) {
  const source = read(page.file)
  assert(source.includes('增量同步'), `${page.file} must expose a visible 增量同步 button.`)
  assert(
    source.includes('ErpKingdeeSyncApi.runIncrementalSyncJob'),
    `${page.file} must trigger manual sync through ErpKingdeeSyncApi.runIncrementalSyncJob.`
  )
  assert(source.includes(page.handler), `${page.file} must trigger ${page.handler}.`)
  for (const forbidden of page.forbidden) {
    assert(!source.includes(forbidden), `${page.file} must not call full-sync path ${forbidden}.`)
  }
}

const syncPage = read('src/views/erp/sync/index.vue')
assert(syncPage.includes('ErpKingdeeSyncApi.runIncrementalSyncJob'), 'ERP sync dashboard must run incremental jobs.')
assert(!syncPage.includes('runProductionMaterialListBackfill'), 'ERP sync dashboard must not keep PML backfill handler.')
assert(!syncPage.includes('syncProductionMaterialList()'), 'ERP sync dashboard must not call PML full backfill API.')

console.log('PASS: ERP manual incremental sync buttons static contract')
