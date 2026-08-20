const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../../..')
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')

const service = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java')
const serviceApi = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderService.java')
const controller = read('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java')

assert.match(serviceApi, /previewRebuildActiveOrder\s*\(/, 'service API must expose rebuild preview')
assert.match(serviceApi, /rebuildActiveOrder\s*\(/, 'service API must expose confirmed rebuild')

assert.match(controller, /@GetMapping\("\/active-order\/rebuild\/preview"\)/, 'controller must expose preview endpoint')
assert.match(controller, /@PostMapping\("\/active-order\/rebuild"\)/, 'controller must expose rebuild endpoint')
assert.match(controller, /mes:pro-process-pool-team-leader:maintain/, 'rebuild endpoint must require maintain permission')

const rebuildMethodStart = service.indexOf('public MesTeamLeaderActiveOrderRebuildResult rebuildActiveOrder')
const rebuild = service.slice(
  service.lastIndexOf('@Transactional(rollbackFor = Exception.class)', rebuildMethodStart),
  service.indexOf('private MesProcessPoolActiveOrderDO requireActiveOrderForRebuild')
)
assert.match(rebuild, /@Transactional\(rollbackFor = Exception\.class\)/, 'rebuild must be one transaction')
assert.match(rebuild, /requireDestructiveConfirmation/, 'historical data rebuild must require destructive confirmation')
assert.match(rebuild, /cleanupActiveOrderRuntimeHistory/, 'rebuild must clean runtime history before rebuilding')
assert.match(rebuild, /refreshActiveOrderSnapshot/, 'rebuild must refresh active-order main snapshot from current data')
assert.match(rebuild, /insertProcessSnapshots/, 'rebuild must recreate production process snapshots')
assert.match(rebuild, /insertPqcInspectionTasks/, 'rebuild must recreate PQC tasks from current QA source')
assert.doesNotMatch(rebuild, /activeOrderMapper\.delete|deleteById\(activeOrder\.getId\(\)\)/, 'rebuild must not physically delete the active order row')

const cleanup = service.slice(
  service.indexOf('private ActiveOrderRebuildCleanupSummary cleanupActiveOrderRuntimeHistory'),
  service.indexOf('private MesTeamLeaderActiveOrderRebuildPreview buildRebuildPreview')
)
for (const required of [
  'reportAllocationMapper.deleteAllByActiveOrderId',
  'reportAllocationStateMapper.deleteByEventIds',
  'reportAllocationAdjustmentAuditMapper.deleteByActiveOrderId',
  'pqcAggregateDetailMapper.deleteByActiveOrderId',
  'pqcPieceDetailMapper.deleteByTaskIds',
  'pqcRecordMapper.deleteByEventIds',
  'submissionReviewMapper.deleteByEventIds',
  'reviewCopyFieldMapper.deleteByEventIds',
  'reviewCopyMapper.deleteByEventIds',
  'eventRevisionDiffMapper.deleteByEventIds',
  'eventRevisionMapper.deleteByEventIds',
  'quantityFragmentMapper.deleteByEventIds',
  'pqcInspectionTaskMapper.deleteByActiveOrderId',
  'processSnapshotMapper.deleteByActiveOrderId',
  'releaseApplicationMapper.deleteByActiveOrderId',
  'processPoolEventMapper.deleteByIds',
  'feedbackMapper.deleteByIds'
]) {
  assert.match(cleanup, new RegExp(required.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${required} must be part of active-order rebuild cleanup`)
}

const eventResolution = service.slice(
  service.indexOf('private Set<Long> resolveActiveOrderRuntimeEventIds'),
  service.indexOf('private ActiveOrderRebuildSnapshotSource refreshActiveOrderSnapshot')
)
assert.match(eventResolution, /MesProcessPoolReportAllocationDO::getEventId/,
  'production report events must be resolved from the current active-order allocation history')
assert.doesNotMatch(eventResolution, /selectProductionSubmitsByWorkOrderAndRouteForUpdate/,
  'rebuild must not delete every production report from the same work order and route')

assert.match(service, /REBUILD_ACTIVE_ORDER/, 'rebuild must write a maintenance audit event')
assert.match(service, /hasHistoricalRuntimeData/, 'preview must tell frontend whether destructive confirmation is required')

console.log('PASS: active-order rebuild snapshots backend contract is explicit')
