const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(process.cwd())
const readSource = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const flowConfigApi = readSource('src/api/mes/pro/route/flowconfig.ts')
const batchExecutionApi = readSource('src/api/mes/pro/edhr/batchExecution.ts')
const feedbackApi = readSource('src/api/mes/pro/feedback/index.ts')
const routeFlowConfigPanel = readSource('src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const batchDetailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const executionPage = readSource('src/views/mes/pro/edhr/ExecutionPage.vue')

for (const token of [
  'ProRouteFlowFormSlotType',
  'ProRouteFlowRequiredPolicy',
  'ProRouteFlowOwnerRoleKey',
  'ProRouteFlowArchiveVisibility',
  'formSlotType?: ProRouteFlowFormSlotType | null',
  'requiredPolicy?: ProRouteFlowRequiredPolicy | null',
  'ownerRoleKey?: ProRouteFlowOwnerRoleKey | null',
  'archiveVisibility?: ProRouteFlowArchiveVisibility | null',
  'permissionScopeId?: number | null'
]) {
  assert.ok(flowConfigApi.includes(token), `route flow API must carry slot metadata token: ${token}`)
}

for (const slotType of ['MAIN', 'PROCESS_INSPECTION', 'LOSS_REPORT', 'PARAMETER_RECORD']) {
  assert.ok(flowConfigApi.includes(slotType), `route flow API must support slot type ${slotType}`)
}

for (const hiddenToken of [
  '历史基础字段',
  'formSlotTypeOptions',
  'resolveFormSlotTypeLabel',
  '主生产表',
  '槽位类型：',
  '必填策略：',
  '责任角色：',
  '权限范围：',
  '归档属性：',
  'route-flow-config-panel-report-slot-meta'
]) {
  assert.ok(!routeFlowConfigPanel.includes(hiddenToken), `route flow page must hide redundant route config token: ${hiddenToken}`)
}

for (const visibleToken of [
  'placeholder="槽位"',
  'placeholder="记录类型"',
  'placeholder="校验策略"',
  'placeholder="必填策略"',
  'placeholder="权限范围"',
  'value="PROCESS_INSPECTION"',
  'value="LOSS_REPORT"',
  'value="PARAMETER_RECORD"',
  'value="OTHER_INTERNAL"'
]) {
  assert.ok(routeFlowConfigPanel.includes(visibleToken), `route flow page must expose inherited slot metadata control: ${visibleToken}`)
}

assert.match(
  routeFlowConfigPanel,
  /batchRecordReports:[\s\S]*formSlotType:[\s\S]*requiredPolicy:[\s\S]*ownerRoleKey:[\s\S]*archiveVisibility:[\s\S]*permissionScopeId:/,
  'route flow save payload must submit slot metadata instead of collapsing to one hard-coded batch record'
)
assert.ok(
  !routeFlowConfigPanel.includes('recordCategory: BATCH_RECORD_CATEGORY,\n                  validationProfile: BATCH_RECORD_VALIDATION_PROFILE'),
  'route flow save payload must not overwrite every slot as the same controlled batch record'
)

for (const token of [
  'formSlotType?: EdhrBatchFormSlotType',
  'requiredPolicy?: EdhrBatchRequiredPolicy',
  'ownerRoleKey?: EdhrBatchOwnerRoleKey',
  'archiveVisibility?: EdhrBatchArchiveVisibility',
  'slotConfigSnapshotHash?: string | null',
  'slotBlockerMessage?: string | null'
]) {
  assert.ok(batchExecutionApi.includes(token), `batch execution API must expose slot token: ${token}`)
}

for (const token of [
  'resolveTaskSlotBlocker',
  'slotConfigSnapshotHash',
  'recordCategory',
  'validationProfile'
]) {
  assert.ok(batchDetailPage.includes(token), `batch detail page must preserve slot blocker validation token: ${token}`)
}

for (const hiddenToken of [
  'slotStatusEntries',
  'resolveSlotStatusEntries',
  'edhr-batch-detail__rail-slot-status-list',
  'edhr-batch-detail__rail-slot-blocker'
]) {
  assert.ok(!batchDetailPage.includes(hiddenToken), `batch detail rail must hide slot status token: ${hiddenToken}`)
}

assert.match(
  batchDetailPage,
  /canOpenTask[\s\S]*resolveTaskSlotBlocker\(row\)[\s\S]*available\s*!==\s*false/,
  'batch detail open button must be disabled when required slot context is missing'
)

for (const token of [
  'formSlotType?: EdhrExecutionFormSlotType',
  'requiredPolicy?: EdhrExecutionRequiredPolicy',
  'ownerRoleKey?: EdhrExecutionOwnerRoleKey',
  'archiveVisibility?: EdhrExecutionArchiveVisibility',
  'slotConfigSnapshotHash?: string | null'
]) {
  assert.ok(feedbackApi.includes(token), `feedback execution API must expose slot context token: ${token}`)
}

for (const token of [
  'slotContextBlockers',
  'hasSlotContextBlockers',
  '缺少槽位快照'
]) {
  assert.ok(executionPage.includes(token), `execution form must preserve slot validation token: ${token}`)
}

assert.match(
  executionPage,
  /:disabled="hasSlotContextBlockers[\s\S]*handleSubmitExecution/,
  'execution submit entry must be disabled when slot context blockers exist'
)

for (const forbiddenToken of ['mock slot', 'placeholder slot', 'fallback slot', '槽位降级', '静默跳过槽位']) {
  assert.ok(
    !`${routeFlowConfigPanel}\n${batchDetailPage}\n${executionPage}`.includes(forbiddenToken),
    `frontend slot implementation must not introduce fallback wording: ${forbiddenToken}`
  )
}

console.log('PASS: eDHR form slot frontend static contract')
