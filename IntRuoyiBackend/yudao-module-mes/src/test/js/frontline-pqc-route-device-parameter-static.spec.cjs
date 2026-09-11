const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const service = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java')
const vo = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcProcessRespVO.java')

assert.match(
  vo,
  /class PqcEquipmentOption[\s\S]*private List<PqcDeviceParameter> parameters;/,
  'Frontline PQC equipment options must expose route-owned device parameters.'
)
assert.match(
  vo,
  /class PqcDeviceParameter[\s\S]*private String parameterCode;[\s\S]*private BigDecimal lowerLimit;[\s\S]*private BigDecimal upperLimit;[\s\S]*private List<String> optionValues;/,
  'Frontline PQC device parameter VO must include identity, range, and select options.'
)
assert.match(
  service,
  /resolvePqcRouteDeviceContext\([\s\S]*MesDeviceParameterSnapshotCodec\.STATE_FROZEN[\s\S]*MesDeviceSelectionSnapshotCodec\.parse\(selectionSnapshotJson\)[\s\S]*MesDeviceParameterSnapshotCodec\.parse\(snapshotJson\)/,
  'Frontline PQC must read both devices and parameters from the frozen active-order route process snapshot.'
)
assert.match(
  service,
  /rules\.stream\(\)\.anyMatch\(rule ->[\s\S]*!Objects\.equals\(rule\.getRouteProcessId\(\), identity\.routeProcessId\(\)\)[\s\S]*!Objects\.equals\(rule\.getProcessId\(\), identity\.processId\(\)\)/,
  'Frontline PQC must fail fast when a frozen parameter belongs to another routeProcessId or processId.'
)
assert.match(
  service,
  /Collectors\.groupingBy\(MesDeviceParameterSnapshotRule::getDeviceId[\s\S]*routeDeviceContext\.deviceCodes\(\)\.contains\([\s\S]*normalizeEquipmentCode\(option\.getEquipmentCode\(\)\)/,
  'Frontline PQC parameters must be grouped by route deviceId and QA equipment must intersect frozen route devices by formal device code.'
)
assert.doesNotMatch(
  service,
  /MesProcessPoolDeviceParameterRuleDO::getLeaderUserId/,
  'Frontline PQC must not filter route-owned device parameters by production leader.'
)

console.log('PASS: frontline PQC route device parameter static contract')
