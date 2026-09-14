const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const candidateService = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteCandidateConfigServiceImpl.java')
const runtimeService = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java')
const pqcService = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java')
const submitService = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java')

assert.match(
  candidateService,
  /validateProductionParameterRule\(routeVersionId, config, rule, deviceIds\)/,
  '候选路线完整配置必须逐条校验参数规则语义和所属工序。'
)
assert.match(
  candidateService,
  /validateProductionDevicesExist[\s\S]*device\.getEnabled\(\)[\s\S]*device\.getDeviceStatus\(\)/,
  '候选路线完整配置必须校验设备主数据存在且可用。'
)
assert.match(
  candidateService,
  /LambdaUpdateWrapper<MesProRouteVersionDO>[\s\S]*MesProRouteVersionDO::getRouteSnapshotSha256[\s\S]*routeVersionMapper\.update\(update, updateWrapper\)/,
  '候选路线保存必须在数据库 UPDATE 条件中原子校验旧快照哈希。'
)

const legacyBranchStart = runtimeService.indexOf('MesDeviceParameterSnapshotCodec.STATE_MISSING_LEGACY.equals(state)')
const legacyBranchEnd = runtimeService.indexOf('if (!MesDeviceParameterSnapshotCodec.STATE_FROZEN.equals(state))', legacyBranchStart)
assert.ok(legacyBranchStart >= 0 && legacyBranchEnd > legacyBranchStart, '必须能定位生产运行态旧快照分支。')
const legacyBranch = runtimeService.slice(legacyBranchStart, legacyBranchEnd)
assert.match(legacyBranch, /throw exception\(/, '生产运行态缺少冻结参数快照必须立即失败。')
assert.doesNotMatch(legacyBranch, /return new ParameterRuntimeSnapshot/, '生产运行态不得把旧快照降级为空配置。')

assert.match(
  pqcService,
  /MesDeviceSelectionSnapshotCodec\.parse[\s\S]*PqcRouteDeviceContext/,
  '一线 PQC 必须解析对应生产工序的冻结设备选择快照。'
)
assert.match(
  pqcService,
  /filter\(option -> routeDeviceContext\.deviceCodes\(\)\.contains\([\s\S]*normalizeEquipmentCode\(option\.getEquipmentCode\(\)\)\)/,
  '一线 PQC 设备选项必须按正式设备编码限制为 QA 允许设备与冻结路线设备的交集。'
)

assert.match(
  submitService,
  /device\.setDeviceCode\(allowed\.deviceCode\(\)\)[\s\S]*setDeviceName\(allowed\.deviceName\(\)\)/,
  '后端必须用冻结会话中的正式设备编号和名称规范化提交设备。'
)

console.log('PASS: frontline route device logic hardening backend contract')
