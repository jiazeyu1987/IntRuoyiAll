const assert = require('assert/strict')
const fs = require('fs')
const path = require('path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const backendRoot = path.resolve(moduleRoot, '..')

const readModule = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const service = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewService.java'
)
assert.match(service, /SOURCE_TYPE_PQC_SUBMISSION\s*=\s*"PQC_SUBMISSION"/)
assert.match(service, /SOURCE_TYPE_PQC_RELEASE\s*=\s*"PQC_RELEASE"/)
assert.match(service, /STATUS_PENDING_REVIEW\s*=\s*"pending_review"/)
assert.match(service, /STATUS_CLOSED\s*=\s*"closed"/)
assert.match(service, /DISPOSITION_CONCESSION_RELEASE\s*=\s*"concession_release"/)
assert.match(service, /DISPOSITION_REWORK\s*=\s*"rework"/)
assert.match(service, /DISPOSITION_VOID\s*=\s*"void"/)

const controller = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrNonconformanceReviewController.java'
)
assert.match(controller, /@RequestMapping\("\/mes\/pro\/edhr-nonconformance-review"\)/)
assert.match(controller, /@PostMapping\("\/create"\)/)
assert.match(controller, /@PostMapping\("\/dispose"\)/)
assert.match(controller, /@GetMapping\("\/pending-page"\)/)
assert.match(controller, /@GetMapping\("\/batch-list"\)/)

const mapper = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrNonconformanceReviewMapper.java'
)
assert.match(mapper, /selectPendingByBatchExecutionId/)
assert.match(mapper, /selectPendingCountByWorkOrderId/)
assert.match(mapper, /selectBlockingCountByWorkOrderId/)
assert.match(mapper, /selectListByBatchExecutionId/)

const batchMapper = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrBatchExecutionMapper.java'
)
assert.match(batchMapper, /BATCH_STATUS_FROZEN\s*=\s*15/)
assert.match(batchMapper, /BATCH_STATUS_VOIDED\)/)
assert.match(batchMapper, /else\s*\{\s*queryWrapper\.notIn\(MesProEdhrBatchExecutionDO::getStatus,\s*BATCH_STATUS_VOIDED\);/s)

const batchService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
)
assert.match(batchService, /BATCH_STATUS_FROZEN\s*=\s*15/)
assert.match(batchService, /NONCONFORMANCE_FROZEN_ACTION_LOCK_REASON/)
assert.match(batchService, /ensureBatchNotFrozen\(batchExecutionId,\s*"eDHR批次操作"\)/)
assert.match(batchService, /Objects\.equals\(batch\.getStatus\(\), BATCH_STATUS_FROZEN\)/)
const syncBatchStatusMethod = batchService.match(
  /private void syncBatchStatus\(MesProEdhrBatchExecutionDO batch\)[\s\S]*?\n    private Map<Long, TaskGate>/
)?.[0]
assert(syncBatchStatusMethod, 'syncBatchStatus method must exist')
assert.match(
  syncBatchStatusMethod,
  /Objects\.equals\(batch\.getStatus\(\), BATCH_STATUS_FROZEN\)/,
  'syncBatchStatus must preserve a batch frozen by pending nonconformance review'
)

const releaseService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java'
)
assert.match(releaseService, /ensureBatchNotFrozen\(batch\.getId\(\),\s*"PQC放行"\)/)
assert.match(releaseService, /ensureBatchNotFrozen\(transaction\.getBatchExecutionId\(\),\s*"PQC放行"\)/)

const feedbackService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/MesProFeedbackServiceImpl.java'
)
assert.match(feedbackService, /ensureWorkOrderNotFrozen\(.*"报工"/s)
assert.match(feedbackService, /ensureWorkOrderNotFrozen\(.*"PQC提交"/s)

const timelineReadDo = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/ProcessPoolTimelineEventReadDO.java'
)
assert.match(timelineReadDo, /private Long batchExecutionId;/)

const timelineResp = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolTimelineEventRespVO.java'
)
assert.match(timelineResp, /private Long batchExecutionId;/)

const timelineService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineServiceImpl.java'
)
assert.match(timelineService, /setBatchExecutionId\(event\.getBatchExecutionId\(\)\)/)

const timelineMapperXml = readModule(
  'src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
)
assert.match(timelineMapperXml, /SELECT batch_execution\.id\s+FROM mes_pro_edhr_batch_execution batch_execution/s)
assert.match(timelineMapperXml, /batch_execution\.work_order_id = pool_event\.work_order_id/s)
assert.match(timelineMapperXml, /batch_execution\.route_id = pool_event\.route_id/s)
assert.match(
  timelineMapperXml,
  /CAST\(batch_execution\.batch_code AS BINARY\)\s*=\s*CAST\(work_order\.batch_code AS BINARY\)/s,
  'PQC管理批次匹配必须用精确二进制比较，避免不同表批号字段排序规则不一致导致列表 500。'
)
assert.match(timelineMapperXml, /batch_execution\.status\s+<!\[CDATA\[<>\]\]>\s+60/s)
assert.match(
  timelineMapperXml,
  /ORDER BY COALESCE\(batch_execution\.attempt_no, 1\) DESC,\s*batch_execution\.id DESC\s*LIMIT 1\s*\) AS batchExecutionId/s
)
assert.doesNotMatch(timelineMapperXml, /pqc_submission_trace/)

const domainTraceDetail = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordDomainTraceDetailRespVO.java'
)
assert.match(domainTraceDetail, /NonconformanceReviewTrace/)
assert.match(domainTraceDetail, /disposition/)
assert.match(domainTraceDetail, /qaSignature/)

const domainTraceService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordDomainTraceServiceImpl.java'
)
assert.match(domainTraceService, /NONCONFORMANCE_REVIEW/)
assert.match(domainTraceService, /setNonconformanceReviews/)

const migration = readBackend('sql/mysql/20260830_mes_edhr_nonconformance_review_mvp.sql')
assert.match(migration, /dependsOn=20260608_edhr_batch_execution_schema;/)
assert.doesNotMatch(migration, /dependsOn=[^;]*\.sql/)
assert.match(migration, /CREATE TABLE IF NOT EXISTS `mes_pro_edhr_nonconformance_review`/)
assert.match(migration, /`review_status` varchar\(32\).*pending_review/)
assert.match(migration, /`disposition` varchar\(32\).*concession_release\/rework\/void/)
assert.match(migration, /CREATE PROCEDURE ensure_mes_edhr_nonconformance_review_menu\(\)/)
assert.match(migration, /system_menu id 9008300 is already used by another menu/)
assert.match(migration, /path already exists with a different id/)
assert.match(migration, /\(9008300, 'eDHR不合格评审'/)
assert.match(migration, /\(9008301, 'eDHR不合格评审查询'/)
assert.match(migration, /\(9008302, 'eDHR不合格评审创建'/)
assert.match(migration, /\(9008303, 'eDHR不合格评审处置'/)
assert.doesNotMatch(migration, /\b90017[0-3]\b/)

console.log('mes-edhr-nonconformance-review-mvp-static: PASS')
