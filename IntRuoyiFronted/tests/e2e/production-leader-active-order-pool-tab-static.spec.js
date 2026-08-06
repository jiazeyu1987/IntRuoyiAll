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
  /<el-dialog[\s\S]*data-team-leader-active-order-add-dialog[\s\S]*title="新增活跃订单"[\s\S]*<el-form-item\s+label="订单号"[\s\S]*<el-select[\s\S]*v-model="activeOrderForm\.workOrderId"[\s\S]*filterable[\s\S]*remote[\s\S]*:remote-method="searchActiveOrderCandidates"[\s\S]*@click="submitAddActiveOrder"/,
  'The 新增活跃订单 dialog must expose one remote searchable 订单号 el-select bound to workOrderId.'
)
assert.doesNotMatch(
  source,
  /data-team-leader-active-order-route-id|data-team-leader-active-order-route-version-id|data-team-leader-active-order-transfer-ids|label="生产订单ID"|label="路线ID"|label="路线版本ID"|label="调拨单ID列表"/,
  'The add dialog must remove old route/version/transfer inputs and only ask for 订单号.'
)
assert.match(
  source,
  /const\s+openActiveOrderDialog\s*=\s*\(\)\s*=>[\s\S]*activeOrderAddDialogVisible\.value\s*=\s*true/,
  'The page must expose an explicit dialog-opening action.'
)
assert.match(
  source,
  /await\s+addTeamLeaderActiveOrder\(\{\s*workOrderId:\s*requirePositiveNumber\(activeOrderForm\.workOrderId,\s*'请选择订单号'\)\s*\}\)/,
  'The dialog submit action must call the add API with workOrderId only.'
)
assert.doesNotMatch(
  source,
  /addTeamLeaderActiveOrder\(\{[\s\S]*(routeId|routeVersionId|transferIds):/,
  'The active-order add request body must not contain old route/version/transfer fields.'
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
assert.match(
  apiSource,
  /export interface TeamLeaderActiveOrderCandidateRespVO\s*\{[\s\S]*workOrderId:\s*number[\s\S]*workOrderCode:\s*string[\s\S]*\}/,
  'The team-leader API must expose active-order candidate response fields.'
)
assert.match(
  apiSource,
  /searchTeamLeaderActiveOrderCandidates[\s\S]*\/mes\/pro\/process-pool\/team-leader\/active-order\/candidates[\s\S]*params:\s*\{\s*keyword\s*\}/,
  'The team-leader API must expose the active-order candidate search endpoint.'
)
assert.match(
  apiSource,
  /export interface TeamLeaderActiveOrderAddReqVO\s*\{\s*workOrderId:\s*number\s*\}/,
  'The active-order add request type must only contain workOrderId.'
)
assert.doesNotMatch(
  apiSource,
  /interface TeamLeaderActiveOrderAddReqVO[\s\S]*(routeId|routeVersionId|transferIds):/,
  'The active-order add request type must not expose route/version/transfer fields.'
)

console.log('PASS: production leader active-order pool tab static contract')
