const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relative) => fs.readFileSync(path.join(moduleRoot, relative), 'utf8').replace(/\r\n/g, '\n')
const controller = read('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java')
const service = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java')
const mapper = read('src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesTeamLeaderDataCleanupMapper.java')

assert.match(controller, /@GetMapping\("\/active-order\/data-cleanup\/preview"\)/)
assert.match(controller, /@PostMapping\("\/active-order\/data-cleanup\/execute"\)/)
assert.equal((controller.match(/mes:pro-process-pool-team-leader:maintain/g) || []).length >= 2, true)
assert.match(service, /previewDataCleanup\(Long leaderUserId\)/)
assert.match(service, /executeDataCleanup\(Long leaderUserId,/) 
assert.match(service, /TenantContextHolder\.getTenantId\(\)/)
assert.match(service, /PRO_PROCESS_POOL_DATA_CLEANUP_SCOPE_CHANGED/)
assert.match(service, /@Transactional\(rollbackFor = Exception\.class\)/)
assert.match(service, /removeActiveOrder\(MesTeamLeaderActiveOrderRemoveReqBO/) 
assert.doesNotMatch(service, /批次执行已进入正式终态，不能物理删除/)
assert.match(service, /selectCleanupOrdersWithRelated\(leaderUserId, false, true\)/)
assert.match(service, /selectCleanupOrdersWithRelated\(leaderUserId, true, true\)/)
assert.match(service, /resolveCleanupEventIds\(\)/)
assert.match(service, /selectAllCleanupEventIds\(currentTenantId\(\)\)/)
assert.match(service, /resolveCleanupBatchExecutionIds\(\)/)
assert.match(service, /selectAllBatchExecutionIds\(currentTenantId\(\)\)/)
assert.match(service, /selectWorkOrderIdsByEventIds\(currentTenantId\(\), eventIds\)/)
assert.match(service, /selectWorkOrderIdsByBatchExecutionIds\(currentTenantId\(\), batchIds\)/)
assert.match(service, /resolveCleanupWorkOrderIds\(historyOrders,\s*java\.util\.stream\.Stream\.concat\(eventWorkOrderIds\.stream\(\), batchWorkOrderIds\.stream\(\)\)\.toList\(\)\)/)
assert.match(service, /resolveCleanupProcessPoolIds\(\)/)
assert.match(service, /selectAllProcessPoolIds\(currentTenantId\(\)\)/)
const cleanupOrdersStart = service.indexOf('private List<MesProcessPoolActiveOrderDO> selectCleanupOrders(Long leaderUserId, boolean forUpdate,')
const cleanupOrdersEnd = service.indexOf('private List<MesProcessPoolActiveOrderDO> selectCleanupOrdersWithRelated', cleanupOrdersStart)
const cleanupOrdersBlock = service.slice(cleanupOrdersStart, cleanupOrdersEnd)
assert.match(
  cleanupOrdersBlock,
  /if \(!includeRemoved\) \{\s*query\.eq\(MesProcessPoolActiveOrderDO::getActiveStatus, STATUS_ACTIVE\);\s*\}/,
  '历史清理范围必须覆盖 CLOSED/COMPLETED/RELEASED 等非 ACTIVE 残留状态，不能只查 ACTIVE/REMOVED'
)
assert.doesNotMatch(
  cleanupOrdersBlock,
  /List\.of\(STATUS_ACTIVE,\s*STATUS_REMOVED\)/,
  '历史清理范围不能被 ACTIVE/REMOVED 枚举限制，否则会漏清已完工批次和放行残留'
)
assert.match(service, /List<Long> ownedOrderIds = ownedOrders\.stream\(\)\s*\.map\(MesProcessPoolActiveOrderDO::getId\)/)
assert.match(service, /selectRelatedCleanupOrders\(currentTenantId\(\), leaderUserId, workOrderIds, ownedOrderIds,\s*includeRemoved, forUpdate\)/)
assert.match(service, /Collectors\.toMap\(MesProcessPoolActiveOrderDO::getId/)
assert.match(service, /Comparator\.comparing\(MesProcessPoolActiveOrderDO::getId/)
assert.doesNotMatch(service, /目标工单同时属于其它生产组长活跃订单/)
assert.match(mapper, /selectRelatedCleanupOrders\(/)
assert.match(mapper, /selectAllCleanupEventIds\(/)
assert.match(mapper, /SELECT id FROM mes_pro_process_pool_event WHERE tenant_id = #\{tenantId\} AND deleted = b'0' ORDER BY id/)
assert.match(mapper, /selectAllBatchExecutionIds\(/)
assert.match(mapper, /SELECT id FROM mes_pro_edhr_batch_execution WHERE tenant_id = #\{tenantId\} AND deleted = b'0' ORDER BY id/)
assert.match(mapper, /selectWorkOrderIdsByEventIds\(/)
assert.match(mapper, /SELECT DISTINCT work_order_id FROM mes_pro_process_pool_event/)
assert.match(mapper, /selectWorkOrderIdsByBatchExecutionIds\(/)
assert.match(mapper, /selectAllProcessPoolIds\(/)
assert.match(mapper, /SELECT id FROM mes_pro_process_pool WHERE tenant_id = #\{tenantId\} AND deleted = b'0' ORDER BY id/)
assert.match(mapper, /DELETE FROM mes_pro_process_pool WHERE tenant_id = #\{tenantId\} AND id IN/)
assert.match(mapper, /@Param\("includeRemoved"\) boolean includeRemoved/)
assert.match(mapper, /@Param\("excludeActiveOrderIds"\) Collection<Long> excludeActiveOrderIds/)
assert.match(mapper, /id NOT IN\s*<foreach collection='excludeActiveOrderIds'/)
assert.match(mapper, /<if test='includeRemoved == false'>AND active_status = 'ACTIVE'<\/if>/)
assert.doesNotMatch(mapper, /active_status IN \('ACTIVE', 'REMOVED'\)/)
assert.match(mapper, /<if test='forUpdate'>FOR UPDATE<\/if>/)
assert.match(mapper, /softRemoveActiveOrders\(/)
assert.match(mapper, /SET active_status = 'REMOVED'/)
const softRemoveStart = mapper.indexOf('int softRemoveActiveOrders')
const softRemoveSqlStart = mapper.lastIndexOf('@Update({', softRemoveStart)
const softRemoveSqlEnd = mapper.indexOf('@Select({', softRemoveStart)
const softRemoveBlock = mapper.slice(softRemoveSqlStart, softRemoveSqlEnd)
assert.doesNotMatch(softRemoveBlock, /leader_user_id\s*=\s*#\{actorUserId\}/)
assert.doesNotMatch(service, /deleteActiveOrders\(tenantId, historyOrderIds\)/)
assert.doesNotMatch(service, /deleteActiveOrderAudits\(tenantId, historyOrderIds\)/)
assert.match(service, /releaseApplicationMapper\.selectListByActiveOrderIdsForUpdate\(historyOrderIds\)/)
assert.match(service, /deletePqcReleaseNonconformanceReviews\(tenantId, releaseApplicationIds\)/)
assert.match(service, /deleteReleaseWorkTasks\(tenantId, releaseApplicationIds\)/)
assert.match(service, /deleteReleaseApplications\(tenantId, historyOrderIds\)/)
assert.match(service, /deleteReleaseTransactionsByIds\(tenantId, releaseTransactionIds\)/)
assert.match(mapper, /tenant_id = #\{tenantId\}/)
assert.match(mapper, /DELETE FROM mes_pro_process_pool_active_order_release_application/)
assert.match(mapper, /business_scope_type = 'RELEASE_APPLICATION'/)
assert.match(mapper, /source_type = 'PQC_RELEASE'/)
assert.match(mapper, /deleteEvents\(/)
assert.match(mapper, /deleteFifoAllocationLines\(/)
assert.match(mapper, /deleteProcessPoolRowsByIds\(/)
assert.match(mapper, /deleteFeedbackSurplusAllocations\(/)
assert.match(mapper, /deleteFeedbackSurplusPools\(/)
assert.match(mapper, /deleteActiveOrderVersionUpgradeRequests\(/)
assert.match(mapper, /deletePrintTasks\(/)
assert.match(mapper, /deletePrintEvents\(/)
assert.match(mapper, /selectLabelInstanceIds\(/)
assert.match(mapper, /deleteBatchExecutions\(/)
assert.match(mapper, /deleteRecordbooks\(/)
assert.match(mapper, /deleteFormInstances\(/)
assert.doesNotMatch(service, /dataCleanupMapper\.deleteRepeatRowGroups\(/)
assert.doesNotMatch(mapper, /DELETE FROM mes_pro_batch_record_repeat_row_group/)
for (const appendOnlyDeleteCall of [
  'deleteExecutionFieldAuditItems',
  'deleteExecutionFieldAuditBatches',
  'deleteDomainTraceItems',
  'deleteDomainTraceSnapshots',
  'deleteExecutionArchiveEvents',
  'deleteExecutionArchives',
  'deleteBatchOrigins',
  'deleteTraceLinks',
  'deleteTraceManifests',
  'deleteTraceOutboxEvents'
]) {
  assert.doesNotMatch(
    service,
    new RegExp(`dataCleanupMapper\\.${appendOnlyDeleteCall}\\(`),
    `${appendOnlyDeleteCall} must not be invoked by test-data cleanup because the target table is append-only/WORM evidence`
  )
}

console.log('PASS: MES team leader data cleanup static contract')
