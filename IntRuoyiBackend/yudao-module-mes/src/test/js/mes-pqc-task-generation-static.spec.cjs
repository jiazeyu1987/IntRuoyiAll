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

console.log('PASS mes-pqc-task-generation-static')
