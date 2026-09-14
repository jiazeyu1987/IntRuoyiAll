const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(frontendRoot, '..')

const panel = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)
const api = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/pro/feedback/index.ts'),
  'utf8'
)
const requestVo = fs.readFileSync(
  path.join(
    workspaceRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcSubmitReqVO.java'
  ),
  'utf8'
)
const controller = fs.readFileSync(
  path.join(
    workspaceRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java'
  ),
  'utf8'
)
const service = fs.readFileSync(
  path.join(
    workspaceRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
  ),
  'utf8'
)

const payloadBuilder = panel.match(
  /const buildPqcInspectionSubmitPayload = \(\): FrontlinePqcInspectionSubmitReqVO => \{[\s\S]*?\n\}/
)?.[0] || ''
const submitInterface = api.match(
  /export interface FrontlinePqcInspectionSubmitReqVO \{([\s\S]*?)\n\}/
)?.[1] || ''

assert.doesNotMatch(panel, /data-pqc-production-submit-select/)
assert.doesNotMatch(panel, /selectedPqcProductionSubmitEventId/)
assert.doesNotMatch(panel, /<span>生产提交事件<\/span>/)
assert.doesNotMatch(payloadBuilder, /productionSubmitEventId\s*:/)
assert.doesNotMatch(submitInterface, /\bproductionSubmitEventId\b/)
assert.doesNotMatch(requestVo, /private Long productionSubmitEventId;/)
assert.doesNotMatch(controller, /\.productionSubmitEventId\(reqVO\.getProductionSubmitEventId\(\)\)/)

assert.match(service, /resolveUniqueProductionSubmitEvent\(/)
assert.match(
  service,
  /candidate -> Objects\.equals\(task\.getRouteProcessId\(\), candidate\.routeProcessId\(\)\)[\s\S]*Objects\.equals\(task\.getProcessId\(\), candidate\.processId\(\)\)/
)
assert.match(service, /command\.setProductionSubmitEventId\(productionSubmit\.eventId\(\)\)/)
assert.match(service, /payload\.put\("productionSubmitEventId", command\.getProductionSubmitEventId\(\)\)/)

console.log('PASS: frontline PQC submit auto-binds production submit event without manual UI binding')
