const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/erp/production/replenishment-list/index.ts')
const page = read('src/views/erp/production/replenishment-list/index.vue')
const profile = read('src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue')
const syncPage = read('src/views/erp/sync/index.vue')

for (const token of [
  '/erp/production-replenishment-list/page',
  'ErpProductionReplenishmentListApi',
  'ErpProductionReplenishmentListVO',
  'ErpProductionReplenishmentListItemVO'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `补料单 API 缺少：${token}`)
}

for (const token of [
  'table-key="erp.production.replenishmentList.main"',
  '生产补料单号',
  '暂无补料明细',
  "runIncrementalSync('PRODUCTION_REPLENISHMENT_LIST')",
  '生产补料单列表增量同步任务已提交',
  "v-hasPermi=\"['erp:kingdee-sync:query']\""
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `补料单页面缺少：${token}`)
}

for (const source of [profile, syncPage]) {
  assert.match(source, /PRODUCTION_REPLENISHMENT_LIST/, 'ERP 同步配置必须列出生产补料单同步类型')
  assert.match(source, /kingdeeProductionReplenishmentListSyncJob/, 'ERP 同步配置必须列出生产补料单 Job 处理器')
  assert.match(source, /生产补料单列表/, 'ERP 同步配置必须展示生产补料单列表名称')
}

console.log('PASS: ERP production replenishment list frontend static contract')
