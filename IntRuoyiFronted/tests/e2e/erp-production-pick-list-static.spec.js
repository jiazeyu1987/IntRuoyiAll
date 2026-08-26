const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(root, '..')
const apiPath = path.join(root, 'src/api/erp/production/pick-list/index.ts')
const pagePath = path.join(root, 'src/views/erp/production/pick-list/index.vue')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')
const sqlPath = path.join(backendRoot, 'sql/mysql/20260813_erp_kingdee_production_pick_list_sync.sql')

for (const requiredPath of [apiPath, pagePath, sqlPath]) {
  assert(fs.existsSync(requiredPath), 'required file must exist: ' + requiredPath)
}

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')
const sql = fs.readFileSync(sqlPath, 'utf8')

for (const token of [
  'ErpProductionPickListVO',
  'ErpProductionPickListItemVO',
  '/erp/production-pick-list/page',
  'getPage'
]) {
  assert(api.includes(token), 'production pick list API must expose ' + token)
}

for (const token of [
  'ErpProductionPickListApi.getPage',
  "ErpKingdeeSyncApi.runIncrementalSync('PRODUCTION_PICK_LIST')",
  '生产领料单号',
  '生产订单编号',
  '单据日期',
  '单据状态',
  '物料编码',
  '物料名称',
  '规格型号',
  '单位',
  '申请数量',
  '实发数量',
  '仓库',
  '库存状态',
  '车间',
  'ERP 修改时间',
  '最后同步时间'
]) {
  assert(page.includes(token), 'production pick list page must render ' + token)
}

assert(!page.includes('ErpProductionMaterialListApi'), 'production pick list must not reuse material list API')
assert(!page.includes('kingdeeProductionMaterialListSyncJob'), 'production pick list must not trigger material list job')
assert(!page.includes('PRD_PPBOM'), 'production pick list page must not present material list form id')

for (const forbidden of ['新增', '审核', '下推', '作废', '库存扣减', 'submitProductionPickList']) {
  assert(!page.includes(forbidden), 'production pick list page must remain readonly and not expose ' + forbidden)
}

for (const token of [
  'erp_kingdee_production_pick_list',
  'erp_kingdee_production_pick_list_item',
  'PRD_PickMtrl',
  'kingdeeProductionPickListSyncJob',
  'erp:production-pick-list:query'
]) {
  assert(sql.includes(token), 'migration must include ' + token)
}

console.log('PASS: ERP production pick list static contract')
