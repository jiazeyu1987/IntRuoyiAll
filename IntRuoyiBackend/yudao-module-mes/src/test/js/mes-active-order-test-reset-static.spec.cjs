const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const controller = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java')
const serviceApi = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderService.java')
const service = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java')
const cleanupMapper = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesTeamLeaderDataCleanupMapper.java')
const api = read('IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts')
const workbench = read('IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

const targetCode = 'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'

assert.match(serviceApi, /resetFixedSimulationActiveOrder\s*\(/)
assert.match(controller, /active-order\/simulation\/test-reset/)
assert.match(controller, /mes:pro-process-pool-team-leader:maintain/)
assert.match(service, new RegExp(targetCode.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
assert.match(service, /TenantContextHolder\.getTenantId\(\)/)
assert.match(service, /@Transactional\(rollbackFor = Exception\.class\)/)
for (const required of [
  'deleteFeedbacks',
  'deleteOrderProcessCompletions',
  'deleteFifoAllocationLines',
  'deleteProcessPoolRows',
  'deleteBatchExecutions',
  'deleteExecutions',
  'deleteRecordbooks',
  'deleteFormInstances',
  'deleteEvents',
  'deleteActiveOrders',
  'addActiveOrder'
]) {
  assert.match(service, new RegExp(required), `reset must include ${required}`)
}
for (const appendOnlyDeleteCall of [
  'deleteBatchOrigins',
  'deleteTraceLinks',
  'deleteTraceManifests',
  'deleteTraceOutboxEvents'
]) {
  assert.doesNotMatch(
    service,
    new RegExp(`dataCleanupMapper\\.${appendOnlyDeleteCall}\\(`),
    `${appendOnlyDeleteCall} must not be invoked by fixed test reset because the target table is append-only/WORM evidence`
  )
}
assert.doesNotMatch(cleanupMapper, /selectEventIdsByWorkOrderIds[\s\S]*?deleted\s*=\s*b'0'[\s\S]*?List<Long> selectEventIdsByWorkOrderIds/)

assert.match(api, /resetFixedSimulationActiveOrder/)
assert.match(api, /active-order\/simulation\/test-reset/)
assert.match(workbench, /data-team-leader-reset-fixed-active-order/)
assert.match(workbench, /重置指定测试订单/)
assert.doesNotMatch(workbench.split("const handleResetFixedSimulationActiveOrder = async () => {")[1].split("const handleActiveOrderWorkOrderKeywordChange")[0], /ElMessageBox/)

console.log('mes-active-order-test-reset-static: PASS')
