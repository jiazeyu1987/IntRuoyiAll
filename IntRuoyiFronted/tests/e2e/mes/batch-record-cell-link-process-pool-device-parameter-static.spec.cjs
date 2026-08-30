const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendView = fs.readFileSync(
  path.resolve(__dirname, '../../../src/views/mes/pro/batchrecordcelllink/index.vue'),
  'utf8'
)
const backendService = fs.readFileSync(
  path.resolve(
    __dirname,
    '../../../../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/MesProBatchRecordCellLinkServiceImpl.java'
  ),
  'utf8'
)
const backfillService = fs.readFileSync(
  path.resolve(
    __dirname,
    '../../../../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderBatchRecordBackfillServiceImpl.java'
  ),
  'utf8'
)

assert.ok(
  frontendView.includes("field.fieldCode.startsWith('deviceParameterReadings.')") &&
    frontendView.includes("field.fieldCode.startsWith('equipmentParameterRules.')"),
  '报工数据参数字段必须纳入工序设备字段过滤，禁止跨工序参数在左侧列表可见。'
)

assert.ok(
  frontendView.includes('function isProcessPoolDeviceGroupSourceField') &&
    frontendView.includes("field.fieldCode.includes('@deviceGroup:')") &&
    frontendView.includes('Boolean(field.deviceName)') &&
    frontendView.includes('field.deviceId !== undefined && Boolean(field.deviceCode) && Boolean(field.deviceName)'),
  '前端过滤设备字段时必须区分设备名称分组字段和旧物理设备字段，不能要求设备组字段携带物理设备 ID。'
)

assert.ok(
  backendService.includes('PROCESS_POOL_REPORT 参数规则未绑定有效设备') &&
    backendService.includes('requireProcessPoolReportDevice('),
  '后端必须校验参数规则绑定到当前工序的有效正式设备。'
)

assert.ok(
  backendService.includes('processPoolReportDeviceIds(devicesByRouteProcess)') &&
    backendService.includes('.in(MesProcessPoolDeviceParameterRuleDO::getDeviceId, processPoolReportDeviceIds)') &&
    !backendService.includes('.eqIfPresent(MesProcessPoolDeviceParameterRuleDO::getLeaderUserId, processPoolLeaderUserId)') &&
    !backendService.includes('参数规则生产组长不匹配'),
  '参数规则必须按当前生产组长已绑定设备 ID 读取，不能再用规则行的冗余生产组长字段过滤导致参数消失。'
)

assert.match(
  backendService,
  /"deviceParameterReadings\."\s*\+\s*code\s*\+\s*"\.value"[\s\S]*\.forDeviceGroup\(rule\.getRouteProcessId\(\), deviceGroup[\s\S]*false\)/,
  '设备参数实际值来源字段必须按设备名称分组，不能按物理设备编码重复展示。'
)

assert.match(
  backendService,
  /"equipmentParameterRules\."\s*\+\s*code\s*\+\s*"\.standardText"[\s\S]*\.forDeviceGroup\(rule\.getRouteProcessId\(\), deviceGroup[\s\S]*false\)/,
  '设备参数标准来源字段必须按设备名称分组，避免同类多台设备重复。'
)

assert.ok(
  backendService.includes('PROCESS_POOL_DEVICE_GROUP_SCOPE_SEPARATOR') &&
    backendService.includes('encodeProcessPoolDeviceGroupScope('),
  '后端字段编码必须带正式设备名称分组作用域，供回填按实际选中设备过滤。'
)

assert.ok(
  backfillService.includes('baseFieldCode.startsWith("deviceParameterReadings.")') &&
    backfillService.includes('baseFieldCode.startsWith("equipmentParameterRules.")') &&
    backfillService.includes('deviceGroupName'),
  '回填解析器必须识别按设备名称分组的参数读数和参数标准字段。'
)

console.log('batch-record-cell-link process-pool device parameter static contract passed')
