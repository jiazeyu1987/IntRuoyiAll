const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()

const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const syncApi = read('src/api/erp/sync/index.ts')

assert(
  syncApi.includes('runIncrementalSync'),
  'ERP sync API must expose runIncrementalSync for all manual table sync buttons.'
)
assert(
  syncApi.includes("'/erp/kingdee-sync/incremental-sync'"),
  'Manual incremental sync must use the tenant-scoped ERP API.'
)
assert(
  !syncApi.includes('JobApi.getJobPage') && !syncApi.includes('JobApi.runJob'),
  'Manual incremental sync must not bypass tenant scope through the generic job API.'
)

const pages = [
  {
    file: 'src/views/erp/product/product/index.vue',
    syncType: 'PRODUCT',
    forbidden: ['ProductApi.syncKingdeeProducts()']
  },
  {
    file: 'src/views/erp/purchase/order/index.vue',
    syncType: 'PURCHASE_ORDER',
    forbidden: ['PurchaseOrderApi.syncKingdeePurchaseOrders()']
  },
  {
    file: 'src/views/erp/sale/order/index.vue',
    syncType: 'SALE_ORDER',
    forbidden: ['SaleOrderApi.syncKingdeeSaleOrders()']
  },
  {
    file: 'src/views/erp/stock/stock/index.vue',
    syncType: 'STOCK',
    forbidden: ['StockApi.syncKingdeeStocks()']
  },
  {
    file: 'src/views/mes/pro/workorder/index.vue',
    syncType: 'PRODUCTION_ORDER',
    forbidden: ['ProWorkOrderApi.syncKingdeeWorkOrders()']
  },
  {
    file: 'src/views/mes/md/item/index.vue',
    syncType: 'PRODUCT',
    forbidden: ['MdItemApi.syncKingdeeItems()']
  },
  {
    file: 'src/views/erp/production/material-list/index.vue',
    syncType: 'PRODUCTION_MATERIAL_LIST',
    forbidden: ['syncProductionMaterialList()', '/erp/production-material-list/sync-kingdee']
  },
  {
    file: 'src/views/erp/production/bom-list/index.vue',
    syncType: 'BOM',
    forbidden: []
  },
  {
    file: 'src/views/erp/production/inventory-list/index.vue',
    syncType: 'STOCK',
    forbidden: []
  },
  {
    file: 'src/views/erp/production/pick-list/index.vue',
    syncType: 'PRODUCTION_PICK_LIST',
    forbidden: []
  },
  {
    file: 'src/views/erp/stock/kingdeeStockMove/index.vue',
    syncType: 'STOCK_MOVE',
    forbidden: []
  }
]

for (const page of pages) {
  const source = read(page.file)
  assert(source.includes('增量同步'), `${page.file} must expose a visible 增量同步 button.`)
  assert(
    source.includes(`ErpKingdeeSyncApi.runIncrementalSync('${page.syncType}')`),
    `${page.file} must trigger manual sync through the tenant-scoped ERP API.`
  )
  for (const forbidden of page.forbidden) {
    assert(!source.includes(forbidden), `${page.file} must not call full-sync path ${forbidden}.`)
  }
}

const syncPage = read('src/views/erp/sync/index.vue')
assert(syncPage.includes('ErpKingdeeSyncApi.runIncrementalSync(row.type)'), 'ERP sync dashboard must run incremental jobs.')
assert(!syncPage.includes('runProductionMaterialListBackfill'), 'ERP sync dashboard must not keep PML backfill handler.')
assert(!syncPage.includes('syncProductionMaterialList()'), 'ERP sync dashboard must not call PML full backfill API.')

console.log('PASS: ERP manual incremental sync buttons static contract')
