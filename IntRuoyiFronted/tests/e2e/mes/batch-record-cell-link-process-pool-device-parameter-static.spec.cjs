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
  '报工数据参数字段必须纳入逐设备字段过滤，禁止无设备身份参数在左侧列表可见。'
)

assert.ok(
  backendService.includes('PROCESS_POOL_REPORT 参数规则未绑定有效设备') &&
    backendService.includes('requireProcessPoolReportDevice('),
  '后端必须校验参数规则绑定到当前工序的有效正式设备。'
)

assert.match(
  backendService,
  /"deviceParameterReadings\."\s*\+\s*code\s*\+\s*"\.value"[\s\S]*\.forDevice\(rule\.getRouteProcessId\(\), device\)/,
  '设备参数实际值来源字段必须带真实设备作用域。'
)

assert.match(
  backendService,
  /"equipmentParameterRules\."\s*\+\s*code\s*\+\s*"\.standardText"[\s\S]*\.forDevice\(rule\.getRouteProcessId\(\), device\)/,
  '设备参数标准来源字段必须带真实设备作用域。'
)

assert.ok(
  backfillService.includes('baseFieldCode.startsWith("deviceParameterReadings.")') &&
    backfillService.includes('baseFieldCode.startsWith("equipmentParameterRules.")'),
  '回填解析器必须识别带 @device 的参数读数和参数标准字段。'
)

console.log('batch-record-cell-link process-pool device parameter static contract passed')
