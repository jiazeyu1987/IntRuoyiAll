const assert = require('assert')
const fs = require('fs')

const servicePath =
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
const testPath =
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java'

const service = fs.readFileSync(servicePath, 'utf8')
const test = fs.readFileSync(testPath, 'utf8')

assert.match(
  service,
  /Map<Long,\s*TaskGate>\s+taskGateMap\s*=\s*buildReviewTimelineTaskGateMap\(batch,\s*tasks\);/,
  '历史 review-timeline 必须通过专用门禁摘要构建，不能直接调用当前流转门禁阻断只读历史。'
)

assert.match(
  service,
  /private Map<Long,\s*TaskGate>\s+buildReviewTimelineTaskGateMap\(MesProEdhrBatchExecutionDO batch,[\s\S]*List<MesProEdhrBatchExecutionTaskDO>\s+tasks\)[\s\S]*batch != null && !isActiveBatch\(batch\)[\s\S]*readonlyReviewTimelineGate\(\)[\s\S]*return buildTaskGateMap\(batch,\s*tasks\);/,
  '终态历史批次必须跳过当前流转门禁，活动批次仍使用正式门禁校验。'
)

assert.match(
  service,
  /private TaskGate readonlyReviewTimelineGate\(\)[\s\S]*new TaskGate\(false,\s*"历史批次只读"\)/,
  '终态历史批次应明确标记只读，不应伪装成可切换或可填写。'
)

assert.match(
  service,
  /boolean readonlyReviewTimeline\s*=\s*batch != null && !isActiveBatch\(batch\);[\s\S]*\.map\(task -> toExecutionReview\(task,\s*readonlyReviewTimeline\)\)/,
  '历史批记录预览必须把终态只读上下文传入执行快照组装。'
)

assert.match(
  service,
  /private List<EdhrBatchExecutionReviewTimelineRespVO\.SignatureCellMarker>\s+resolveSignatureCellMarkers\([\s\S]*boolean persistedExecutionOnly\)[\s\S]*extractSignatureCellMarkers\(execution\.getExecutionSnapshotJson\(\)\)[\s\S]*extractSignatureCellMarkers\(execution\.getSheetLayoutJson\(\)\)[\s\S]*if \(!layoutMarkers\.isEmpty\(\) \|\| persistedExecutionOnly\)\s*\{[\s\S]*return layoutMarkers;[\s\S]*jimuReportGateway\.getReportJson/,
  '终态历史执行预览应只用已持久化执行快照/布局解析签名单元格，不应因当前 Jimu 报表缺失阻断历史。'
)

assert.doesNotMatch(
  service,
  /emptyReviewTimeline|isArchivedHistoryMissingBatchConfig/,
  '历史页不应因为缺少 BATCH 门禁配置清空已持久化的批记录执行快照。'
)

assert.match(
  test,
  /void getReviewTimeline_returnsPersistedHistoryWhenArchivedRouteGateConfigMissing\(\)[\s\S]*incompleteFrozenBatchTaskConfigSnapshotJson\(\)[\s\S]*deleteById[\s\S]*batchExecutionService\.getReviewTimeline\(batch\.getId\(\)\)[\s\S]*assertEquals\(1,\s*timeline\.getExecutionReviews\(\)\.size\(\)\);[\s\S]*assertEquals\("BRE-8051"/,
  'Java 回归测试必须覆盖归档历史批次缺失门禁配置时仍返回已持久化的批记录执行快照。'
)

console.log('PASS edhr-history-missing-batch-config-static')
