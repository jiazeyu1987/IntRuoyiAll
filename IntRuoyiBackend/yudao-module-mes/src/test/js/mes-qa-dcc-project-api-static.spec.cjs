const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const controller = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/MesQaInspectionRegulationController.java'
)
const requestVo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationSaveReqVO.java'
)
const responseVo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationPublishedVersionRespVO.java'
)
const statusVo = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationProjectStatusRespVO.java'
)
const service = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)
const regulationMapper = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/qa/regulation/MesQaInspectionRegulationMapper.java'
)

assert.match(requestVo, /private Long dccProjectCodeId;/)
assert.match(requestVo, /private List<InspectionProcess> processes;/)
assert.match(requestVo, /private List<InspectionTypeRule> inspectionTypeRules;/)
assert.doesNotMatch(requestVo, /private Long productId;|private Long routeId;|private Long routeProcessId;|private Long processId;/)
assert.match(responseVo, /private Long dccProjectCodeId;/)
assert.match(responseVo, /private List<InspectionProcess> processes;/)
assert.doesNotMatch(responseVo, /private Long productId;|private Long routeId;|private Long routeProcessId;|private Long processId;/)
assert.match(statusVo, /private Long dccProjectCodeId;/)
assert.doesNotMatch(statusVo, /private Long productId;/)

assert.match(controller, /@RequestParam\("dccProjectCodeId"\) Long dccProjectCodeId/)
assert.match(controller, /@RequestParam\("dccProjectCodeIds"\) List<Long> dccProjectCodeIds/)
assert.match(controller, /@GetMapping\("\/current"\)/)
assert.doesNotMatch(controller, /@RequestParam\("productIds"\)/)

assert.match(regulationMapper, /selectByDccProjectCodeId/)
assert.match(regulationMapper, /selectListByDccProjectCodeIds/)
assert.doesNotMatch(service, /selectByRouteProcess|selectLatestPublished|selectListByProductIds/)
assert.match(service, /DccProjectCodeMapper/)
assert.match(service, /DccProjectCodeStatusConstants\.ENABLE/)
assert.match(service, /MesQaInspectionRegulationProcessMapper/)
assert.match(service, /QA_INSPECTION_REGULATION_VERSION_IMMUTABLE/)

console.log('PASS: DCC-scoped QA regulation backend API contract')
