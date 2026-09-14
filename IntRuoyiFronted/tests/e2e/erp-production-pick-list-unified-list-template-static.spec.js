const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/erp/production/pick-list/index.vue')
assert(fs.existsSync(pagePath), 'production pick list page must exist: ' + pagePath)

const page = fs.readFileSync(pagePath, 'utf8')

for (const token of [
  '<UnifiedListTemplate',
  'table-key="erp.production.pickList.main"',
  'useTableQuickFilter',
  'useUserTableColumns',
  'data-user-table-column-explicit',
  'data-user-table-key="erp.production.pickList.main"',
  'erp.production.pickList.main',
  'ErpProductionPickListApi.getPage',
  "ErpKingdeeSyncApi.runIncrementalSync('PRODUCTION_PICK_LIST')",
  '生产领料单号',
  '生产订单编号',
  '物料编码',
  '申请数量',
  '实发数量',
  '库存状态',
  '最后同步时间'
]) {
  assert(page.includes(token), 'production pick list standard template must include ' + token)
}

assert(page.includes('<template #table>'), 'production pick list must render the table in template slot')
assert(page.includes('<template #actions>'), 'production pick list must render actions in template slot')
assert(!page.includes('<Pagination'), 'production pick list pagination must be owned by UnifiedListTemplate')
assert(!page.includes('<el-form'), 'production pick list query form must be owned by UnifiedListTemplate')
for (const forbidden of ['新增', '审核', '下推', '库存扣减', 'submitProductionPickList']) {
  assert(!page.includes(forbidden), 'production pick list must remain readonly and not expose ' + forbidden)
}

console.log('PASS: ERP production pick list unified list template static contract')
