const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.resolve(workspaceRoot, 'IntRuoyiBackend')
const read = (file) => fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n')

const teamLeaderPage = read(path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
))
const routeService = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteServiceImpl.java'
))
const routeDesigner = read(path.join(
  frontendRoot,
  'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
))
const routeApi = read(path.join(
  frontendRoot,
  'src/api/mes/pro/route/index.ts'
))
const frontlineRuntimeService = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java'
))
const snapshotValidator = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionSnapshotValidator.java'
))
const recognitionSyncService = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRecognitionDeviceSyncService.java'
))

assert.doesNotMatch(
  teamLeaderPage,
  /data-production-leader-module-tab-process-config/,
  '生产组长工作台不应继续显示工序配置页签。'
)
assert.doesNotMatch(
  teamLeaderPage,
  /activeProductionModuleTab\.value === 'processConfig'/,
  '生产组长工作台不应再按 processConfig 页签加载运行维护配置。'
)
assert.match(
  teamLeaderPage,
  /const showProductionProcessConfigModule = computed\(\s*\(\) => false\s*\)/,
  '旧版无页签布局也不应继续展示生产组长工序配置模块。'
)
assert.doesNotMatch(
  teamLeaderPage,
  /loadProcessConfigRows\(\)\.catch\(\(error\) => \{\s*ElMessage\.error\(resolveErrorMessage\(error, '工序配置列表加载失败'\)\)/,
  '生产组长工作台初始化不应再加载已迁移的工序配置接口。'
)
assert.match(
  routeService,
  /PRODUCTION_PROCESS_CONFIGS_KEY = "productionProcessConfigs"/,
  '工艺路线版本快照必须承载生产工序配置列表。'
)
assert.match(
  routeService,
  /PRODUCTION_PROCESS_CONFIG_SCHEMA_VERSION_KEY = "productionProcessConfigSchemaVersion"/,
  '工艺路线版本快照必须承载生产工序配置 schema 版本。'
)
assert.match(
  snapshotValidator,
  /PRODUCTION_PROCESS_CONFIGS_KEY/,
  '路线版本完整性校验必须要求生产工序配置。'
)
assert.match(
  routeDesigner,
  /data-flow-panel="route-production-process-config-editor"/,
  '工艺路线流转图必须提供生产配置编辑入口。'
)
assert.match(
  routeDesigner,
  /saveRouteProductionProcessConfig/,
  '工艺路线生产配置编辑入口必须写入路线候选版本 API。'
)
assert.match(
  routeApi,
  /getRouteProductionProcessConfig/,
  '前端 API 必须支持读取路线版本生产配置。'
)
assert.match(
  recognitionSyncService,
  /selectOpenCandidateByRouteId\(routeId\)/,
  'JSON/Word 识别设备参数同步必须写入工艺路线候选版本。'
)
assert.match(
  recognitionSyncService,
  /saveConfigSnapshots\(candidate\.getId\(\), candidate\.getRouteSnapshotSha256\(\), Map\.of\(/,
  'JSON/Word 识别设备参数同步必须原子覆盖候选版本生产配置快照。'
)
assert.match(
  frontlineRuntimeService,
  /toFrozenProcessDeviceBindings\(parameterSnapshot\.selectionGroups\(\)/,
  '一线活跃订单运行配置必须从冻结设备选择快照恢复设备选项。'
)
assert.match(
  frontlineRuntimeService,
  /activeOrderId == null\s*\?\s*toDefectReasonOptions\(process, leaderUserIds\)\s*:\s*parameterSnapshot\.lossReasons\(\)/,
  '一线活跃订单运行配置必须从冻结损耗原因快照恢复损耗选项。'
)

console.log('PASS route production config ownership static contract')
