const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const contextService = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
const contextContract = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextService.java'
)
const controller = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java'
)
const teamLeaderService = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
)
const taskOption = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcTaskOption.java'
)
const pqcCandidate = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcProcessCandidate.java'
)
const pqcResponse = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcProcessRespVO.java'
)
const switchRequest = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcSwitchEmployeeReqVO.java'
)
const submitRequest = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcSubmitReqVO.java'
)
const eventService = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventServiceImpl.java'
)
const releaseReader = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl.java'
)
const qaProvenance = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImpl.java'
)

assert.match(contextService, /selectActiveOrderIdsByTaskStatus\([\s\S]{0,160}PQC_TASK_STATUS_PENDING/, 'PQC active-order list must expose only orders with pending PQC tasks.')
assert.match(contextService, /MesQaInspectionRegulationProcessMapper/)
assert.match(contextService, /selectByDccProjectCodeId/)
assert.match(contextService, /selectListByVersionId/)
assert.doesNotMatch(contextService, /selectListByProductIds|selectPublishedByRouteProcess/)
assert.doesNotMatch(contextService, /resolveQaInspectionItemRouteProcesses/)

assert.match(contextContract, /List<MesFrontlinePqcProcessCandidate> listProcessesByActiveOrder/)
assert.match(pqcCandidate, /Long qaProcessId/)
assert.match(pqcCandidate, /String qaProcessCode/)
assert.match(pqcCandidate, /String qaProcessName/)
assert.match(pqcCandidate, /Integer qaProcessSort/)
assert.match(pqcCandidate, /Long regulationVersionId/)
assert.doesNotMatch(pqcCandidate, /routeProcessId|Long processId/)
assert.match(pqcResponse, /private Long qaProcessId;/)
assert.match(pqcResponse, /private String qaProcessCode;/)
assert.match(pqcResponse, /private String qaProcessName;/)
assert.match(pqcResponse, /private Integer qaProcessSort;/)
assert.doesNotMatch(pqcResponse, /routeProcessId|private Long processId;/)

assert.match(taskOption, /Long qaProcessId/)
assert.match(switchRequest, /private Long qaProcessId;/)
assert.match(switchRequest, /private Long regulationVersionId;/)
assert.doesNotMatch(switchRequest, /routeProcessId|private Long processId;/)
assert.match(submitRequest, /private Long qaProcessId;/)
assert.doesNotMatch(submitRequest, /routeProcessId|private Long processId;|productionSubmitEventId/)
assert.match(controller, /CommonResult<List<MesFrontlinePqcProcessRespVO>> getPqcActiveOrderProcesses/)
assert.match(contextService, /validatePqcTaskSubmissionIdentity/)
assert.match(eventService, /getQaProcessId\(\)/)
assert.match(eventService, /PQC事件不得使用MES路线工序身份/)

assert.match(teamLeaderService, /MesQaInspectionRegulationProcessMapper/)
assert.match(teamLeaderService, /selectByQaIdentity/)
assert.match(teamLeaderService, /\.qaProcessId\(/)
assert.doesNotMatch(teamLeaderService, /selectPublishedByRouteProcess/)

assert.match(releaseReader, /selectByDccProjectCodeId\(dccProject\.getId\(\)\)/)
assert.doesNotMatch(releaseReader, /selectPublishedListByStableProcess|selectPublishedByRouteProcess/)
assert.match(qaProvenance, /regulation\.getDccProjectCodeId\(\)/)
assert.doesNotMatch(qaProvenance, /startsWith\(expectedPrefix\)|PQC-.*projectCode/)

console.log('PASS: frontline PQC uses DCC-owned QA process identity')
