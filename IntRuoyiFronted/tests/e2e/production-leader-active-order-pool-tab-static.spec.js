const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const source = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const apiSource = read('src/api/mes/pro/processpool/teamLeader.ts')

const sliceContentWrapByMarker = (marker) => {
  const markerIndex = source.indexOf(marker)
  assert.notEqual(markerIndex, -1, `Expected marker in TeamLeaderWorkbenchPage.vue: ${marker}`)
  const start = source.lastIndexOf('<ContentWrap', markerIndex)
  const end = source.indexOf('</ContentWrap>', markerIndex)
  assert.notEqual(start, -1, `Expected ContentWrap start for marker: ${marker}`)
  assert.notEqual(end, -1, `Expected ContentWrap end for marker: ${marker}`)
  return source.slice(start, end)
}

assert.match(
  source,
  /const\s+activeProductionModuleTab\s*=\s*ref<[\s\S]*'activeOrder'[\s\S]*>\('personnel'\)/,
  'Production leader module state must include the active-order pool tab.'
)
assert.match(
  source,
  /const\s+showProductionActiveOrderModule\s*=\s*computed\([\s\S]*activeProductionModuleTab\.value\s*===\s*'activeOrder'/,
  'The active-order pool content must be controlled by its own production module gate.'
)

const activeOrderTabCount = (source.match(/data-production-leader-module-tab-active-order/g) || []).length
assert.equal(
  activeOrderTabCount,
  7,
  'Every production module tab strip, including the active-order content card, must expose 活跃订单池.'
)

const activeOrderBlock = sliceContentWrapByMarker('data-team-leader-active-order-pool-tab')
assert.match(
  activeOrderBlock,
  /data-team-leader-active-order-config/,
  'The new active-order tab must retain the stable active-order maintenance marker.'
)
assert.match(
  activeOrderBlock,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.processPool\.teamLeader\.activeOrders"/,
  'The active-order tab must use the standard unified list template.'
)
assert.match(
  activeOrderBlock,
  /<template\s+#actions>[\s\S]*data-team-leader-open-active-order-dialog[\s\S]*新增活跃订单/,
  'The standard list actions area must provide the 新增活跃订单 button.'
)
assert.match(
  activeOrderBlock,
  /:data="pagedActiveOrderRows"[\s\S]*data-team-leader-active-order-list/,
  'The standard list must render all active-order rows through client-side pagination.'
)
for (const label of ['活跃池ID', '生产订单ID', '路线ID', '路线版本ID', 'ERP生产数量', '状态', '加入时间', '操作']) {
  assert.match(activeOrderBlock, new RegExp(`label="${label}"`), `The active-order list must show ${label}.`)
}
assert.match(
  activeOrderBlock,
  /data-team-leader-active-order-transfer-trace/,
  'Moving active-order maintenance into its own tab must retain the transfer trace table.'
)

assert.match(
  source,
  /<el-dialog[\s\S]*data-team-leader-active-order-add-dialog[\s\S]*title="新增活跃订单"[\s\S]*data-team-leader-active-order-route-id[\s\S]*data-team-leader-active-order-route-version-id[\s\S]*data-team-leader-active-order-transfer-ids[\s\S]*@click="submitAddActiveOrder"/,
  'The 新增活跃订单 button must open a dialog that submits the formal active-order payload.'
)
assert.match(
  source,
  /const\s+openActiveOrderDialog\s*=\s*\(\)\s*=>[\s\S]*activeOrderAddDialogVisible\.value\s*=\s*true/,
  'The page must expose an explicit dialog-opening action.'
)
assert.match(
  source,
  /await\s+addTeamLeaderActiveOrder\(\{[\s\S]*workOrderId:[\s\S]*routeId:[\s\S]*routeVersionId:[\s\S]*transferIds:/,
  'The dialog submit action must call the formal add API with the complete payload.'
)
assert.match(
  source,
  /const\s+submitRemoveActiveOrder\s*=\s*async\s*\(row:\s*TeamLeaderActiveOrderRespVO\)[\s\S]*removeTeamLeaderActiveOrder\(\{[\s\S]*activeOrderId:/,
  'Each active-order list row must retain the formal remove action.'
)

const configBlock = sliceContentWrapByMarker('data-team-leader-config-center')
assert.doesNotMatch(
  configBlock,
  /data-team-leader-active-order-config|activeOrderForm|activeOrderRemoveForm|data-team-leader-active-order-transfer-trace/,
  '班组配置 must not duplicate active-order maintenance after the dedicated tab is introduced.'
)

for (const field of [
  'routeId: number',
  'routeVersionId: number',
  'erpFixedQuantitySnapshot?: number | string',
  'businessStatus?: string',
  'version?: number'
]) {
  assert.ok(apiSource.includes(field), `TeamLeaderActiveOrderRespVO must include ${field}.`)
}

console.log('PASS: production leader active-order pool tab static contract')
