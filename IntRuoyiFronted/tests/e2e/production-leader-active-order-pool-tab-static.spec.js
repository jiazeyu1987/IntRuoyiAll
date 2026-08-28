const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
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

const sliceDialogByMarker = (marker) => {
  const markerIndex = source.indexOf(marker)
  assert.notEqual(markerIndex, -1, `Expected dialog marker in TeamLeaderWorkbenchPage.vue: ${marker}`)
  const start = source.lastIndexOf('<el-dialog', markerIndex)
  const end = source.indexOf('</el-dialog>', markerIndex)
  assert.notEqual(start, -1, `Expected el-dialog start for marker: ${marker}`)
  assert.notEqual(end, -1, `Expected el-dialog end for marker: ${marker}`)
  return source.slice(start, end)
}

const sliceInterfaceBlock = (content, interfaceName) => {
  const start = content.indexOf(`export interface ${interfaceName}`)
  assert.notEqual(start, -1, `Expected interface: ${interfaceName}`)
  const bodyStart = content.indexOf('{', start)
  const bodyEnd = content.indexOf('}', bodyStart)
  assert.notEqual(bodyStart, -1, `Expected interface body start: ${interfaceName}`)
  assert.notEqual(bodyEnd, -1, `Expected interface body end: ${interfaceName}`)
  return content.slice(start, bodyEnd + 1)
}

assert.match(
  source,
  /const\s+activeProductionModuleTab\s*=\s*ref<[\s\S]*'activeOrder'[\s\S]*>\('report'\)/,
  'Production leader module state must include the active-order pool tab.'
)
assert.match(
  source,
  /const\s+showProductionActiveOrderModule\s*=\s*computed\([\s\S]*activeProductionModuleTab\.value\s*===\s*'activeOrder'/,
  'The active-order pool content must be controlled by its own production module gate.'
)

const productionModuleTabStripCount = (source.match(/data-production-leader-module-tabs/g) || []).length
const activeOrderTabCount = (source.match(/data-production-leader-module-tab-active-order/g) || []).length
assert.ok(
  productionModuleTabStripCount > 0,
  'The production leader page must retain at least one production module tab strip.'
)
assert.equal(
  activeOrderTabCount,
  productionModuleTabStripCount,
  'Every retained production module tab strip must expose 活跃订单池 after removing the team configuration tab.'
)
const responsibleRouteSummaryCount = (source.match(/data-production-leader-responsible-routes/g) || []).length
const standaloneResponsibleRouteSummaryCount = (
  source.match(
    /<ContentWrap\s+v-if="!showPqcModuleTabs && !showProductionModuleTabs"[\s\S]*?data-production-leader-responsible-routes/
  ) || []
).length
assert.equal(
  standaloneResponsibleRouteSummaryCount,
  1,
  'The generic team-leader workbench mode must keep one standalone responsible route summary.'
)
assert.equal(
  responsibleRouteSummaryCount,
  productionModuleTabStripCount + standaloneResponsibleRouteSummaryCount,
  'Every production module tab strip plus the generic workbench mode must show the responsible route names.'
)
assert.match(
  source,
  /const\s+productionResponsibleRouteNames\s*=\s*computed\([\s\S]*responsibleRouteRows\.value[\s\S]*row\.routeName[\s\S]*seen\.add\(routeName\)[\s\S]*return routeNames/,
  'Production responsible route names must be derived from the formal responsible-routes API rows and de-duplicated by routeName.'
)
assert.doesNotMatch(
  source.match(/const\s+productionResponsibleRouteNames\s*=\s*computed\([\s\S]*?return routeNames[\s\S]*?\n\}\)/)?.[0] || '',
  /processConfigRows|formBindings|activeOrderOptions|routeCode|routeId/,
  'The responsible route header must not infer names from maintenance scope, form slots, active orders, route codes, or route IDs.'
)

const activeOrderBlock = sliceContentWrapByMarker('data-team-leader-active-order-pool-tab')
const activeOrderDialogBlock = sliceDialogByMarker('data-team-leader-active-order-add-dialog')
const activeOrderAddReqBlock = sliceInterfaceBlock(apiSource, 'TeamLeaderActiveOrderAddReqVO')
assert.match(
  activeOrderBlock,
  /data-team-leader-simulate-active-order-stage1[\s\S]*@click="handleSimulateStage1\(row\)"[\s\S]*>\s*<Icon icon="ep:refresh" \/>\s*Stage1模拟\s*<\/el-button>/,
  'The active-order list must expose the independent Stage1 simulation button with the Stage1模拟 label.'
)
assert.doesNotMatch(
  activeOrderBlock,
  /data-team-leader-simulate-active-order-completion|handleSimulateActiveOrderCompletion|ep:magic-stick|模拟完成/,
  'The active-order list must remove the generic magic-wand simulation entry.'
)
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
for (const label of ['活跃池ID', '生产订单号', '路线名称', '版本号', 'ERP生产数量', '加入时间', '操作']) {
  assert.match(activeOrderBlock, new RegExp(`label="${label}"`), `The active-order list must show ${label}.`)
}
assert.doesNotMatch(
  activeOrderBlock,
  /label="路线ID"|label="路线版本ID"|label="状态"\s+width="100"/,
  'The active-order list must not expose route IDs, route-version IDs, or the active status column.'
)
assert.doesNotMatch(
  activeOrderBlock,
  /data-team-leader-active-order-transfer-trace|调拨库存追溯/,
  'The active-order tab must not render the removed transfer-trace panel.'
)

const activeOrderAddDialog = sliceDialogByMarker('data-team-leader-active-order-add-dialog')

assert.match(
  activeOrderAddDialog,
  /<el-dialog[\s\S]*data-team-leader-active-order-add-dialog[\s\S]*title="新增活跃订单"[\s\S]*<el-form-item\s+label="订单号\/产品"[\s\S]*<el-select[\s\S]*v-model="activeOrderForm\.workOrderId"[\s\S]*filterable[\s\S]*remote[\s\S]*:remote-method="searchActiveOrderCandidates"[\s\S]*placeholder="请输入订单号、产品编码或产品名称"[\s\S]*@change="handleActiveOrderCandidateChange"[\s\S]*@clear="handleActiveOrderCandidateClear"[\s\S]*@click="submitAddActiveOrder"/,
  'The 新增活跃订单 dialog must expose one remote searchable 订单号/产品 el-select bound to workOrderId.'
)
assert.match(
  activeOrderAddDialog,
  /<el-option[\s\S]*v-for="candidate in activeOrderCandidateOptions"[\s\S]*team-leader-workbench__active-order-candidate[\s\S]*'is-eligible': candidate\.eligible[\s\S]*符合要求/,
  'The active-order candidate dropdown must render eligible candidates with a green visible 符合要求 marker.'
)
assert.match(
  source,
  /\.team-leader-workbench__active-order-candidate\.is-eligible[\s\S]*color:\s*#16a34a/,
  'The active-order candidate dropdown must style eligible candidates in green.'
)
assert.doesNotMatch(
  activeOrderAddDialog,
  /data-team-leader-active-order-route-id|data-team-leader-active-order-route-version-id|data-team-leader-active-order-transfer-ids|label="生产订单ID"|label="路线ID"|label="路线版本ID"|label="调拨单ID列表"/,
  'The add dialog must remove old route/version/transfer inputs and only ask for 订单号/产品 plus a formal pick list.'
)
assert.match(
  activeOrderAddDialog,
  /<el-form-item\s+label="正式领料单"[\s\S]*data-team-leader-active-order-pick-list[\s\S]*v-model="activeOrderForm\.pickListId"[\s\S]*v-for="option in activeOrderPickListOptions"[\s\S]*:disabled="!option\.selectable"/,
  'The add dialog must require a selectable formal pick list before joining the active-order pool.'
)
assert.match(
  source,
  /const\s+openActiveOrderDialog\s*=\s*\(\)\s*=>[\s\S]*activeOrderAddDialogVisible\.value\s*=\s*true/,
  'The page must expose an explicit dialog-opening action.'
)
assert.match(
  source,
  /const\s+activeOrderCandidateKeyword\s*=\s*ref\(''\)[\s\S]*const\s+findActiveOrderCandidateByCode\s*=[\s\S]*candidate\.workOrderCode\.trim\(\)\s*===\s*workOrderCode\.trim\(\)/,
  'The add dialog must keep the typed order-number keyword and match it against real candidate codes.'
)
assert.match(
  source,
  /const\s+resolveActiveOrderCandidateByKeyword\s*=\s*async\s*\(\)\s*=>[\s\S]*activeOrderCandidateKeyword\.value[\s\S]*searchTeamLeaderActiveOrderCandidates\(keyword\)[\s\S]*return findActiveOrderCandidateByCode\(keyword\)[\s\S]*const\s+requireSelectedActiveOrderCandidateWorkOrderId\s*=\s*async\s*\(\)\s*=>[\s\S]*await resolveActiveOrderCandidateByKeyword\(\)[\s\S]*throw new Error\('请选择订单号\/产品候选'\)[\s\S]*return requirePositiveNumber\(selectedCandidate\.workOrderId,\s*'请选择订单号\/产品候选'\)/,
  'The dialog submit action must resolve exact typed order/product text to a real candidate or block before sending workOrderId.'
)
assert.match(
  source,
  /const\s+workOrderId\s*=\s*await\s+requireSelectedActiveOrderCandidateWorkOrderId\(\)[\s\S]*const\s+receipt\s*=\s*await\s+addTeamLeaderActiveOrder\(\{[\s\S]*workOrderId,[\s\S]*pickListId,[\s\S]*pickListCandidateSnapshotHash/,
  'The dialog submit action must call the add API with a candidate-verified workOrderId and the selected formal pick-list proof.'
)
assert.doesNotMatch(
  source.match(/await\s+addTeamLeaderActiveOrder\(\{[\s\S]*?\}\)/)?.[0] || '',
  /(routeId|routeVersionId|transferIds):/,
  'The active-order add request body must not contain old route/version/transfer fields.'
)
assert.match(
  source,
  /const\s+handleRemoveActiveOrder\s*=\s*async\s*\(row:\s*TeamLeaderActiveOrderRespVO\)[\s\S]*removeTeamLeaderActiveOrder\(\{[\s\S]*activeOrderId:/,
  'Each active-order list row must retain the formal remove action.'
)
assert.match(
  activeOrderBlock,
  /data-team-leader-remove-active-order[\s\S]*@click="handleRemoveActiveOrder\(row\)"[\s\S]*>\s*移除\s*<\/el-button>[\s\S]*data-team-leader-report-active-order-abnormal[\s\S]*@click="openAbnormalDialog\(row\)"[\s\S]*>\s*异常\s*<\/el-button>[\s\S]*data-team-leader-active-order-release-apply[\s\S]*@click="submitActiveOrderReleaseApplication\(row\)"[\s\S]*>\s*完工\s*<\/el-button>/,
  'Each active-order row must expose 移除, 异常, and 完工 labels while retaining the formal actions.'
)
assert.match(
  activeOrderBlock,
  /'team-leader-workbench__abnormal-work-order-id': row\.abnormal[\s\S]*data-team-leader-abnormal-report-dialog/,
  'The active-order list must render abnormal ids in red and host the row-bound abnormal dialog.'
)

const configBlock = sliceContentWrapByMarker('data-team-leader-config-center')
assert.doesNotMatch(
  configBlock,
  /data-team-leader-active-order-config|activeOrderForm|activeOrderRemoveForm/,
  '班组配置 must not duplicate active-order maintenance after the dedicated tab is introduced.'
)

for (const field of [
  'routeId: number',
  'routeName: string',
  'routeVersionId: number',
  'routeVersionNo: string',
  'erpFixedQuantitySnapshot?: number | string',
  'businessStatus?: string',
  'version?: number',
  'abnormal: boolean',
  'abnormalReason?: string'
]) {
  assert.ok(apiSource.includes(field), `TeamLeaderActiveOrderRespVO must include ${field}.`)
}
assert.match(
  apiSource,
  /export interface TeamLeaderActiveOrderCandidateRespVO\s*\{[\s\S]*workOrderId:\s*number[\s\S]*workOrderCode:\s*string[\s\S]*eligible:\s*boolean[\s\S]*ineligibleReason\?:\s*string[\s\S]*\}/,
  'The team-leader API must expose active-order candidate eligibility fields.'
)
assert.match(
  apiSource,
  /searchTeamLeaderActiveOrderCandidates[\s\S]*\/mes\/pro\/process-pool\/team-leader\/active-order\/candidates[\s\S]*params:\s*\{\s*keyword\s*\}/,
  'The team-leader API must expose the active-order candidate search endpoint.'
)
assert.match(
  activeOrderAddReqBlock,
  /export interface TeamLeaderActiveOrderAddReqVO\s*\{\s*workOrderId:\s*number\s*pickListId:\s*string\s*pickListCandidateSnapshotHash:\s*string\s*idempotencyKey:\s*string\s*\}/,
  'The active-order add request type must include workOrderId plus the formal pick-list receipt fields.'
)
assert.doesNotMatch(
  activeOrderAddReqBlock,
  /(routeId|routeVersionId|transferIds):/,
  'The active-order add request type must not expose route/version/transfer fields.'
)

console.log('PASS: production leader active-order pool tab static contract')
