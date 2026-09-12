const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8').replace(/\r\n/g, '\n')

const candidateService = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteCandidateConfigServiceImpl.java')
const recognitionSync = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRecognitionDeviceSyncService.java')
const submitService = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java')
const activeOrderController = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java')
const workflowService = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionWorkflowServiceImpl.java')
const materialValidator = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackMaterialSubmissionValidator.java')

assert.match(
  candidateService,
  /updateWrapper[\s\S]*\.eq\(MesProRouteVersionDO::getLifecycleStatus,[\s\S]*STATUS_DRAFT[\s\S]*\.eq\(MesProRouteVersionDO::getActive, Boolean\.FALSE\)/,
  '候选快照原子更新必须同时限定 DRAFT 和非激活状态。'
)

const syncStart = recognitionSync.indexOf('public void sync(')
const syncEnd = recognitionSync.indexOf('private JSONArray readExistingProductionConfigs', syncStart)
assert.ok(syncStart >= 0 && syncEnd > syncStart, '必须能定位识别同步入口。')
const syncBlock = recognitionSync.slice(syncStart, syncEnd)
assert.match(
  syncBlock,
  /routeProcessesFromCandidateSnapshot\(candidate\)/,
  '识别同步必须从目标候选版本快照读取路线工序。'
)
assert.doesNotMatch(
  syncBlock,
  /routeProcessesById\(routeId\)/,
  '识别同步不得用当前路线数据库工序替代候选流程图。'
)

assert.match(
  submitService,
  /normalizeProcessDeviceAuditPayload\(reqVO\)/,
  '无物料和有物料提交都必须在拆分事件前规范化设备审计 rawPayload。'
)
assert.match(
  submitService,
  /static void normalizeProcessDeviceAuditPayload[\s\S]*rawPayload\.put\("selectedDevices"[\s\S]*rawPayload\.put\("deviceParameterReadings"[\s\S]*rawPayload\.put\("deviceMeteringValidity"/,
  '设备审计规范化必须同时覆盖设备、参数和计量状态。'
)

assert.match(
  activeOrderController,
  /toProductionActiveOrderRespVO[\s\S]*\.setRouteVersionId\(activeOrder\.getRouteVersionId\(\)\)/,
  '一线生产活跃订单响应必须返回冻结路线版本 ID。'
)
assert.match(
  activeOrderController,
  /toActiveOrderRespVO[\s\S]*candidate\.routeVersionId\(\)/,
  'PQC 活跃订单响应也必须返回冻结路线版本 ID。'
)
assert.match(
  workflowService,
  /getVersionForUpdate\(Long id\)[\s\S]*selectByIdForUpdate\(id\)[\s\S]*submitCandidate\(Long id\)[\s\S]*getVersionForUpdate\(id\)[\s\S]*withdrawCandidate\(Long id\)[\s\S]*getVersionForUpdate\(id\)/,
  '候选版本写状态操作必须先锁定版本行。'
)
assert.match(
  workflowService,
  /withdrawCandidate\(Long id\)[\s\S]*updateApprovalFieldsToDraft\(candidate\.getId\(\)\)/,
  '候选版本撤回必须把 DRAFT 状态和清空后的审批字段持久化。'
)
assert.match(
  materialValidator,
  /outputQuantity == null \|\| outputQuantity\.compareTo\(BigDecimal\.ZERO\) <= 0/,
  '参与提交的输出物料完成数量必须大于零。'
)
assert.match(
  syncBlock,
  /existingConfigsByRouteProcessId = productionConfigsByRouteProcessId[\s\S]*configsByRouteProcessId = new LinkedHashMap<>\(\)[\s\S]*routeProcesses\.entrySet\(\)[\s\S]*existingConfigsByRouteProcessId\.get/,
  '识别导入的输出集合必须从候选流程图重建，只把旧配置作为当前工序字段来源。'
)

console.log('PASS: frontline route device follow-up backend contract')
