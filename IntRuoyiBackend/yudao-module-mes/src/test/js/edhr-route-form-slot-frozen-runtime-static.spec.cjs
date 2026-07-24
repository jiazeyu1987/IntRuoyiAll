const fs = require('fs')
const path = require('path')
const assert = require('assert')

const moduleRoot = path.resolve(__dirname, '../../..')
const runtimeService = fs.readFileSync(path.join(
  moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
), 'utf8')

assert.match(
  runtimeService,
  /MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO\(\)[\s\S]*\.setRouteVersionId\(activeRouteVersion\.getId\(\)\)[\s\S]*\.setRouteSnapshotJson\(activeRouteVersion\.getRouteSnapshotJson\(\)\)[\s\S]*buildBatchTaskConfigs\(batch, route, routeProcesses\)/,
  '新建批次必须先把 active route version 快照放到批次对象，再从冻结快照生成任务。'
)

assert.match(
  runtimeService,
  /MesProEdhrBatchExecutionDO newAttempt = new MesProEdhrBatchExecutionDO\(\)[\s\S]*\.setRouteVersionId\(activeRouteVersion\.getId\(\)\)[\s\S]*\.setRouteSnapshotJson\(activeRouteVersion\.getRouteSnapshotJson\(\)\)[\s\S]*buildBatchTaskConfigs\(newAttempt, route, routeProcesses\)/,
  '质量拒收重执行也必须从新 active route version 快照生成任务。'
)

assert.match(
  runtimeService,
  /private List<BatchTaskConfig> buildBatchTaskConfigs\(MesProEdhrBatchExecutionDO batch,[\s\S]*resolveBatchTaskConfigs\(batch, route, routeProcesses\)/,
  '批次上下文任务构建必须调用冻结快照感知的 resolveBatchTaskConfigs。'
)

assert.doesNotMatch(
  runtimeService,
  /List<BatchTaskConfig> taskConfigs = buildBatchTaskConfigs\(route, routeProcesses\);/,
  '新建或重执行批次不得绕过 routeVersionId/routeSnapshotJson 直接读取当前草稿配置。'
)

assert.match(
  runtimeService,
  /JSONArray bindings = processConfig\.getJSONArray\("formBindings"\)/,
  '冻结路线快照必须读取 formBindings 表单槽位配置。'
)

console.log('edhr-route-form-slot-frozen-runtime-static PASS')
