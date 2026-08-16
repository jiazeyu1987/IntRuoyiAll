const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../../../..')
const mapperPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
)
const eventDoPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/MesProProcessPoolEventDO.java'
)
const migrationPath = path.join(
  repoRoot,
  'IntRuoyiBackend/sql/mysql/20260813_mes_production_report_management_summary.sql'
)
const summaryServicePath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesProductionReportManagementSummaryService.java'
)
const poolQuantityServicePath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationPoolQuantityService.java'
)
const timelineReadDoPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/ProcessPoolTimelineEventReadDO.java'
)
const timelineServicePath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineServiceImpl.java'
)
const eventServicePath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventServiceImpl.java'
)
const allocationCommandPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java'
)
const orderChangePath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationOrderChangeService.java'
)
const correctionPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolProductionReportCorrectionService.java'
)
const releaseServicePath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java'
)
const workbenchServicePath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderWorkbenchServiceImpl.java'
)

assert(fs.existsSync(mapperPath), '报工管理列表 mapper 必须存在。')
assert(fs.existsSync(eventDoPath), '工序池事件 DO 必须存在。')
assert(fs.existsSync(migrationPath), '报工管理汇总字段正式迁移必须存在。')

const mapper = fs.readFileSync(mapperPath, 'utf8')
const normalizedMapper = mapper.replace(/<!\[CDATA\[<>\]\]>/g, '<>')
const workbenchFilter = normalizedMapper.match(
  /<if test="reqVO\.allocationView == 'WORKBENCH'">([\s\S]*?)<\/if>/
)?.[1] || ''
const eventDo = fs.readFileSync(eventDoPath, 'utf8')
const migration = fs.readFileSync(migrationPath, 'utf8')
const summaryService = fs.readFileSync(summaryServicePath, 'utf8')
const poolQuantityService = fs.readFileSync(poolQuantityServicePath, 'utf8')
const timelineReadDo = fs.readFileSync(timelineReadDoPath, 'utf8')
const timelineService = fs.readFileSync(timelineServicePath, 'utf8')
const eventService = fs.readFileSync(eventServicePath, 'utf8')
const allocationCommand = fs.readFileSync(allocationCommandPath, 'utf8')
const orderChange = fs.readFileSync(orderChangePath, 'utf8')
const correction = fs.readFileSync(correctionPath, 'utf8')
const releaseService = fs.readFileSync(releaseServicePath, 'utf8')
const workbenchService = fs.readFileSync(workbenchServicePath, 'utf8')

for (const field of [
  'reportManagementStatus',
  'reportOutputQuantity',
  'reportAllocatedQuantity',
  'reportUnallocatedQuantity',
  'reportReleaseStatus'
]) {
  assert(eventDo.includes(field), '工序池事件必须持久化报工管理汇总字段：' + field)
}

assert.match(
  normalizedMapper,
  /reqVO\.allocationView == 'WORKBENCH'[\s\S]{0,400}pool_event\.report_management_status\s+IN\s*\([\s\S]{0,150}'UNALLOCATED'[\s\S]{0,150}'PARTIALLY_ALLOCATED'[\s\S]{0,150}'PENDING_RELEASE'/,
  '报工管理必须直接按正式管理状态读取待处理记录。'
)
assert.doesNotMatch(
  workbenchFilter,
  /SELECT\s+SUM\(allocation\.allocated_quantity\)/,
  '报工管理 WORKBENCH 过滤不得继续逐行汇总分配数量。'
)
assert.doesNotMatch(
  workbenchFilter,
  /mes_pro_edhr_release_transaction/,
  '报工管理 WORKBENCH 过滤不得继续逐行查询放行事务。'
)
assert.match(
  mapper,
  /pool_event\.report_allocated_quantity AS reportAllocatedQuantity/,
  '报工管理列表必须读取事件上的正式已分配数量。'
)
assert.match(
  mapper,
  /pool_event\.report_unallocated_quantity AS reportUnallocatedQuantity/,
  '报工管理列表必须读取事件上的正式未分配数量。'
)
assert.match(mapper, /pool_event\.event_type AS eventType/)
assert.match(timelineReadDo, /private String eventType;/)
assert.match(
  timelineService,
  /!MesProProcessPoolEventDO\.EVENT_TYPE_PRODUCTION_SUBMIT\.equals\(event\.getEventType\(\)\)[\s\S]{0,160}setOutputQuantity\(toBigDecimal\(payload\.get\("outputQuantity"\)\)\)/,
  '非生产报工事件必须继续从原始提交载荷读取产量，不能被报工管理汇总字段覆盖。'
)

for (const required of [
  '-- release-migration: allowedEnvironments=test,backup,prod;',
  '20260813_mes_production_report_management_summary',
  'report_management_status',
  'report_output_quantity',
  'report_allocated_quantity',
  'report_unallocated_quantity',
  'report_release_status',
  'mes_pro_process_pool_quantity_fragment',
  'pqcContextSubmitRoot',
  'Production report output sources are inconsistent',
  'idx_mes_pp_event_report_management',
  '20260809_mes_process_pool_report_shared_allocation',
  'Production report output sources are inconsistent'
]) {
  assert(migration.includes(required), '报工管理汇总迁移缺少正式合同：' + required)
}
assert.doesNotMatch(
  migration,
  /GROUP BY\s+event\.id,\s*event\.tenant_id,\s*event\.raw_payload/,
  '迁移回填不得按 raw_payload 原文分组，避免大 JSON 排序拖慢或受格式影响。'
)
assert.match(summaryService, /requireSubmittedOutputQuantity\(event\)/)
assert.match(summaryService, /requirePoolQuantity\(event\)/)
assert.match(poolQuantityService, /event\.getReportOutputQuantity\(\)/)
assert.doesNotMatch(
  poolQuantityService.match(/public BigDecimal requirePoolQuantity[\s\S]*?\n    }/)?.[0] || '',
  /rawPayload|JsonUtils/,
  '运行时分配必须只读已经固化的正式产量。'
)

for (const status of [
  'REPORT_MANAGEMENT_STATUS_UNALLOCATED',
  'REPORT_MANAGEMENT_STATUS_PARTIALLY_ALLOCATED',
  'REPORT_MANAGEMENT_STATUS_PENDING_RELEASE',
  'REPORT_MANAGEMENT_STATUS_ARCHIVED',
  'REPORT_RELEASE_STATUS_NOT_ALLOCATED',
  'REPORT_RELEASE_STATUS_NOT_RELEASED',
  'REPORT_RELEASE_STATUS_PARTIALLY_RELEASED',
  'REPORT_RELEASE_STATUS_RELEASED'
]) {
  assert(summaryService.includes(status), '正式汇总服务缺少状态计算：' + status)
}

assert.match(eventService, /initializeProductionEvent\(event\)[\s\S]{0,120}processPoolEventMapper\.insert\(event\)/)
assert.match(allocationCommand, /reportManagementSummaryService\.refreshProductionEvent\(event\)/)
assert.match(orderChange, /reportManagementSummaryService\.refreshProductionEvent\(event\)/)
assert.match(correction, /event\.setRawPayload\(afterPayloadJson\)[\s\S]{0,150}refreshProductionEvent\(event\)/)
assert.equal(
  (releaseService.match(/refreshByReleaseTransactionId\(transaction\.getId\(\)\)/g) || []).length,
  2,
  '负责人直接放行和审批放行都必须同步报工管理状态。'
)
assert.match(summaryService, /public void lockProductionEventsByReleaseTransactionId\(Long releaseTransactionId\)/)
assert.equal(
  (releaseService.match(/lockProductionEventsByReleaseTransactionId\(reqVO\.getReleaseTransactionId\(\)\)/g) || []).length,
  2,
  '直接放行和审批放行都必须先锁定关联报工事件。'
)
assert.equal(
  (releaseService.match(/lockProductionEventsByReleaseTransactionId\(reqVO\.getReleaseTransactionId\(\)\);[\s\S]{0,180}requireTransactionForUpdate\(reqVO\.getReleaseTransactionId\(\)\)/g) || []).length,
  2,
  '放行链路必须统一按报工事件、放行事务的顺序加锁。'
)
assert.match(workbenchService, /map\(MesProRouteProcessDO::getId\)/)
assert.match(workbenchService, /setRouteProcessIds\(routeProcessIds\)/)
assert.doesNotMatch(workbenchService, /setProcessIds\(processIds\)/)

console.log('PASS production-report-management-summary-static')
