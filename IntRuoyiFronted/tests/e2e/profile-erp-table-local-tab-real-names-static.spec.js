const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const componentPath = path.join(
  root,
  'src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue'
)
const component = fs.readFileSync(componentPath, 'utf8')
const syncTypesStart = component.indexOf('const syncTypes: ProfileErpSyncType[] = [')
assert.notEqual(syncTypesStart, -1, 'ERP 表格自动同步组件必须声明 syncTypes。')
const syncTypesMatch = component
  .slice(syncTypesStart)
  .match(/const syncTypes: ProfileErpSyncType\[\] = \[[\s\S]*?\r?\n\]\r?\n\r?\nconst loading/)
assert.ok(syncTypesMatch, 'syncTypes 配置块必须可解析。')
const syncTypesBlock = syncTypesMatch[0]

const expectedLocalNames = [
  "localTabName: '产品信息 / 物料产品管理'",
  "localTabName: '产品库存'",
  "localTabName: '库存调拨'",
  "localTabName: '采购订单'",
  "localTabName: '销售订单'",
  "localTabName: '生产工单'",
  "localTabName: '生产领料单列表'",
  "localTabName: '生产用料清单'",
  "localTabName: '物料清单'"
]

for (const token of expectedLocalNames) {
  assert.match(
    syncTypesBlock,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `本地页签名称必须使用真实菜单名称：${token}`
  )
}

for (const legacyName of [
  'ERP商品 / MES物料产品',
  'ERP库存',
  '金蝶调拨单只读列表',
  'ERP采购订单',
  'ERP销售订单',
  'MES生产工单',
  'ERP生产领料单列表',
  'ERP生产用料清单',
  'ERP产品BOM'
]) {
  assert.equal(
    syncTypesBlock.includes(legacyName),
    false,
    `本地页签名称不得继续显示旧名称：${legacyName}`
  )
}

for (const requiredIdentity of [
  "syncType: 'PRODUCT'",
  "handlerName: 'kingdeeProductItemSyncJob'",
  "syncType: 'STOCK'",
  "handlerName: 'kingdeeStockSyncJob'",
  "syncType: 'BOM'",
  "handlerName: 'kingdeeBomSyncJob'"
]) {
  assert.match(
    syncTypesBlock,
    new RegExp(requiredIdentity.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `同步身份和任务处理器必须保持不变：${requiredIdentity}`
  )
}

console.log('profile-erp-table-local-tab-real-names-static PASS')
