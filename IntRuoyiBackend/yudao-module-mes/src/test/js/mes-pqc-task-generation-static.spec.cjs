const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

function sliceBetween(source, startNeedle, endNeedle, label) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker`)
  return source.slice(start, end)
}

const service = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
)
const mapper = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/pqc/MesPqcInspectionTaskMapper.java'
)
const processSnapshotMapper = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderProcessSnapshotMapper.java'
)
const regulationVersionDO = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/qa/regulation/MesQaInspectionRegulationVersionDO.java'
)
const regulationSaveReqVO = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationSaveReqVO.java'
)
const regulationPublishedRespVO = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationPublishedVersionRespVO.java'
)
const regulationService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)
const regulationSchema = read(
  'IntRuoyiBackend/sql/mysql/20260802_mes_qa_inspection_regulation.sql'
)
const releaseCompletenessService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesOrderReleaseCompletenessServiceImpl.java'
)
const errors = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java'
)
const test = read(
  'IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceTest.java'
)
const releaseCompletenessTest = read(
  'IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesOrderReleaseCompletenessServiceTest.java'
)
const regulationServiceTest = read(
  'IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceTest.java'
)
const insertPqcInspectionTask = sliceBetween(
  service,
  'private void insertPqcInspectionTask(MesPqcInspectionTaskDO task)',
  'private Integer resolveFixedInspectionQuantity(',
  'insertPqcInspectionTask'
)

assert.match(service, /MesQaInspectionRegulationMapper/, 'PQC 任务生成必须读取正式 QA 规程。')
assert.match(service, /MesQaInspectionRegulationItemMapper/, 'PQC 任务生成必须读取发布规程项目。')
assert.match(service, /MesPqcInspectionTaskMapper/, 'PQC 任务生成必须写入正式 PQC task 表。')
assert.match(service, /selectPublishedByRouteProcess\(productId,[\s\S]*process\.getRouteProcessId\(\),[\s\S]*process\.getProcessId\(\)\)/,
  '生成任务必须按产品 + 路线版本 + 工序查已发布 QA 规程。')
assert.match(service, /inspectionRegulationItemMapper\.selectListByVersionId\(regulation\.getCurrentVersionId\(\)\)/,
  '生成任务必须按发布版本读取规程项目。')
assert.match(service, /SHIFT_AM\s*=\s*"AM"/, '上午巡检必须有独立 AM 班次身份。')
assert.match(service, /SHIFT_PM\s*=\s*"PM"/, '下午巡检必须有独立 PM 班次身份。')
assert.match(service, /buildPqcTask\([\s\S]*INSPECTION_TYPE_PATROL[\s\S]*SHIFT_AM[\s\S]*resolvePatrolInspectionQuantity/,
  '生成器必须创建上午巡检任务。')
assert.match(service, /buildPqcTask\([\s\S]*INSPECTION_TYPE_PATROL[\s\S]*SHIFT_PM[\s\S]*resolvePatrolInspectionQuantity/,
  '生成器必须创建下午巡检任务，不能复用上午任务。')
assert.match(service, /setScale\(0,\s*RoundingMode\.CEILING\)/,
  '巡检比例数量必须向上取整，例如 301×5% = 16。')
assert.match(insertPqcInspectionTask, /selectByIdentity\(task\.getActiveOrderId\(\),[\s\S]*task\.getRoundNo\(\)\)/,
  '写任务前必须按完整身份检查重复任务。')
assert.match(insertPqcInspectionTask, /catch\s*\(DuplicateKeyException ex\)[\s\S]*PRO_PQC_INSPECTION_TASK_IDENTITY_CONFLICT/,
  '数据库唯一键冲突必须转为明确重复任务错误。')
assert.doesNotMatch(insertPqcInspectionTask, /catch\s*\(DuplicateKeyException ex\)[\s\S]*return\s*;/,
  '重复任务冲突不得被静默吞掉。')

assert.match(mapper, /selectByIdentity\(Long activeOrderId,\s*Long routeProcessId,[\s\S]*LocalDate businessDate,[\s\S]*String shiftCode,\s*Integer roundNo\)/,
  'PQC task mapper 必须暴露完整身份查询。')
assert.match(processSnapshotMapper, /selectListByActiveOrderId\(Long activeOrderId\)/,
  '放行完整性必须能按 activeOrderId 读取工序快照。')
assert.match(errors, /PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED/, '缺规程/缺项目必须有专用生成阻塞错误码。')
assert.match(errors, /PRO_PQC_INSPECTION_TASK_IDENTITY_CONFLICT/, '重复身份必须有专用错误码。')

assert.match(regulationVersionDO, /finalInspectionApplicable/, 'QA 规程发布版本必须持久化末检是否适用。')
assert.match(regulationVersionDO, /finalInspectionNotApplicableReason/, 'QA 规程发布版本必须持久化末检不适用依据。')
assert.match(regulationSaveReqVO, /finalInspectionApplicable/, 'QA 规程保存/发布请求必须显式携带末检适用性。')
assert.match(regulationSaveReqVO, /finalInspectionNotApplicableReason/, 'QA 规程保存/发布请求必须携带末检不适用依据字段。')
assert.match(regulationPublishedRespVO, /finalInspectionApplicable/, '发布版本响应必须返回末检适用性证据。')
assert.match(regulationPublishedRespVO, /finalInspectionNotApplicableReason/, '发布版本响应必须返回末检不适用依据。')
assert.match(regulationSchema, /`final_inspection_applicable`\s+bit\(1\)\s+DEFAULT NULL\s+COMMENT '末检是否适用'/,
  'QA 规程版本表必须有可审计的末检适用性字段，且不能给默认成功值。')
assert.match(regulationSchema, /`final_inspection_not_applicable_reason`\s+varchar\(512\)\s+DEFAULT NULL\s+COMMENT '末检不适用依据'/,
  'QA 规程版本表必须有可审计的末检不适用依据字段。')
assert.match(regulationService, /validateFinalInspectionApplicability\([^)]*reqVO/,
  '发布/保存必须校验末检适用性为显式配置。')
assert.match(regulationService, /reqVO\.getFinalInspectionApplicable\(\)\s*==\s*null/,
  '未显式配置末检适用性必须阻塞。')
assert.match(regulationService, /Boolean\.FALSE\.equals\(reqVO\.getFinalInspectionApplicable\(\)\)[\s\S]*StrUtil\.isBlank\(reqVO\.getFinalInspectionNotApplicableReason\(\)\)/,
  '末检不适用时必须有非空依据。')
assert.match(regulationService, /Boolean\.TRUE\.equals\(reqVO\.getFinalInspectionApplicable\(\)\)[\s\S]*requiredType[^;]*"FINAL"|requiredType[^;]*"FINAL"[\s\S]*Boolean\.TRUE\.equals\(reqVO\.getFinalInspectionApplicable\(\)\)/,
  '末检适用时发布仍必须要求 FINAL 规则。')
assert.match(regulationService, /Boolean\.FALSE\.equals\(reqVO\.getFinalInspectionApplicable\(\)\)[\s\S]*actualTypes\.contains\("FINAL"\)/,
  '末检不适用时不能同时保存 FINAL 检验项目造成矛盾。')
assert.match(service, /MesQaInspectionRegulationVersionMapper/, 'PQC 任务生成必须读取发布版本末检适用性证据。')
assert.match(service, /requireRegulationVersion\([^)]*regulation/,
  'PQC 任务生成必须按 currentVersionId 读取正式发布版本。')
assert.match(service, /Boolean\.TRUE\.equals\(version\.getFinalInspectionApplicable\(\)\)[\s\S]*INSPECTION_TYPE_FINAL/,
  '末检适用时生成器必须生成 FINAL 任务。')
assert.match(service, /Boolean\.FALSE\.equals\(version\.getFinalInspectionApplicable\(\)\)[\s\S]*getFinalInspectionNotApplicableReason/,
  '末检不适用时生成器必须读取并校验不适用依据后跳过 FINAL。')
assert.match(service, /getFinalInspectionApplicable\(\)\s*==\s*null/,
  '发布版本缺末检适用性配置时生成器必须 fail fast。')
assert.match(releaseCompletenessService, /MesQaInspectionRegulationVersionMapper/,
  '放行完整性必须读取发布版本末检适用性证据。')
assert.match(releaseCompletenessService, /isFinalInspectionApplicableForSnapshot/,
  '放行完整性必须按工序快照和发布版本判断是否需要 FINAL。')
assert.match(releaseCompletenessService, /Boolean\.FALSE\.equals\(version\.getFinalInspectionApplicable\(\)\)[\s\S]*getFinalInspectionNotApplicableReason/,
  '放行完整性只允许有明确不适用依据时跳过 FINAL。')
assert.match(releaseCompletenessService, /if\s*\(isFinalInspectionApplicableForSnapshot\(tasks,\s*snapshot,\s*missing\)\)\s*\{[\s\S]*requirePqcTaskIdentity\(tasks,\s*snapshot,\s*"FINAL",\s*"FINAL"/,
  '末检适用时放行完整性必须继续要求 FINAL 任务。')

assert.match(test, /shouldGenerateFormalPqcTasksFromPublishedRegulationWhenAddingActiveOrder/,
  'JUnit 必须覆盖发布规程生成正式任务。')
assert.match(test, /assertPqcTask\(tasks\.get\(1\),\s*"PATROL",\s*"AM",\s*16\)/,
  'JUnit 必须证明 301×5% 上午巡检向上取整为 16。')
assert.match(test, /assertPqcTask\(tasks\.get\(2\),\s*"PATROL",\s*"PM",\s*16\)/,
  'JUnit 必须证明下午巡检与上午任务身份分离。')
assert.match(test, /shouldRejectActiveOrderWhenPublishedPqcRegulationMissing/,
  'JUnit 必须覆盖缺少已发布规程时阻塞。')
assert.match(test, /shouldRejectActiveOrderWhenPqcTaskIdentityAlreadyExists/,
  'JUnit 必须覆盖重复任务身份时阻塞。')
assert.match(releaseCompletenessService, /processSnapshotMapper\.selectListByActiveOrderId\(activeOrder\.getId\(\)\)/,
  '放行检查必须按活跃订单工序快照计算预期 PQC 任务集合。')
assert.match(releaseCompletenessService, /requirePqcTaskIdentity\(tasks,\s*snapshot,\s*"FIRST",\s*"FIRST"/,
  '放行检查必须要求首检任务身份。')
assert.match(releaseCompletenessService, /requirePqcTaskIdentity\(tasks,\s*snapshot,\s*"PATROL",\s*"AM"/,
  '放行检查必须要求上午巡检任务身份。')
assert.match(releaseCompletenessService, /requirePqcTaskIdentity\(tasks,\s*snapshot,\s*"PATROL",\s*"PM"/,
  '放行检查必须要求下午巡检任务身份。')
assert.match(releaseCompletenessService, /requirePqcTaskIdentity\(tasks,\s*snapshot,\s*"FINAL",\s*"FINAL"/,
  '放行检查必须要求末检任务身份。')
assert.match(releaseCompletenessService, /缺少预期 PQC 检验任务身份/,
  '放行检查缺少预期 PQC 任务时必须阻塞。')
assert.match(releaseCompletenessTest, /evaluateInspectionResultBlocksWhenConfirmedPqcTasksMissExpectedPatrolPmIdentity/,
  'JUnit 必须覆盖已有任务已确认但缺少下午巡检身份时阻塞。')
assert.match(releaseCompletenessTest, /evaluateInspectionResultPassesWhenConfirmedPqcTasksCoverExpectedIdentities/,
  'JUnit 必须覆盖 FIRST/PATROL AM/PATROL PM/FINAL 全部确认才通过。')
assert.match(regulationServiceTest, /publish_allowsMissingFinalOnlyWhenExplicitlyNotApplicable/,
  'JUnit 必须覆盖末检明确不适用且有依据时允许缺少 FINAL 项目。')
assert.match(test, /shouldSkipFinalPqcTaskWhenPublishedRegulationMarksFinalInspectionNotApplicable/,
  'JUnit 必须覆盖发布版本明确末检不适用时不生成 FINAL 任务。')
assert.match(releaseCompletenessTest, /evaluateInspectionResultPassesWithoutFinalWhenRegulationMarksFinalNotApplicable/,
  'JUnit 必须覆盖有明确末检不适用依据时放行不要求 FINAL。')

console.log('PASS mes-pqc-task-generation-static')
