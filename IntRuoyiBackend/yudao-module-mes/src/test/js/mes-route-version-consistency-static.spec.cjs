const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const flowReqVO = read(
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/flowconfig/MesProRouteFlowConfigSaveReqVO.java'
)
const flowService = read(
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteFlowConfigServiceImpl.java'
)
const scheduleService = read(
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteScheduleConfigServiceImpl.java'
)
const errorCodes = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java')
const packageService = read(
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchRouteConfigPackageServiceImpl.java'
)
const flowConfigApi = read('yudao-ui-admin-vue3/src/api/mes/pro/route/flowconfig.ts')
const routeProcessList = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteProcessList.vue')
const routeFlowGraphDesigner = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

assert.match(errorCodes, /PRO_ROUTE_VERSION_STALE/, '后端必须定义路线版本漂移业务错误。')

assert.match(flowReqVO, /private\s+Long\s+routeVersionId;/, 'flow-config 保存请求必须携带 routeVersionId。')
assert.match(
  flowReqVO,
  /@NotNull\(message = "路线版本编号不能为空"\)[\s\S]{0,120}private\s+Long\s+routeVersionId;/,
  'flow-config 保存请求 routeVersionId 必须为必填字段。'
)
assert.match(
  flowService,
  /MesProRouteVersionMapper\s+routeVersionMapper/,
  'flow-config 服务必须注入路线版本 Mapper。'
)
assert.match(
  flowService,
  /selectActiveByRouteId\(routeId\)/,
  'flow-config 保存必须读取当前启用路线版本。'
)
assert.match(
  flowService,
  /PRO_ROUTE_VERSION_STALE/,
  'flow-config 保存必须在版本漂移时返回业务错误。'
)
assert.match(
  scheduleService,
  /selectActiveByRouteId\(routeVersion\.getRouteId\(\)\)/,
  '路线排产配置保存必须对比当前启用路线版本。'
)
assert.match(
  scheduleService,
  /PRO_ROUTE_VERSION_STALE/,
  '路线排产配置保存必须在版本漂移时返回业务错误。'
)
assert.match(
  packageService,
  /reqVO\.setRouteVersionId\(resolvedContext\.routeVersionId\(\)\)/,
  '配置包导入必须显式携带目标当前路线版本。'
)

assert.match(flowConfigApi, /routeVersionId:\s*number/, '前端 flow-config 保存类型必须包含 routeVersionId。')
assert.match(
  routeProcessList,
  /ProRouteFlowConfigApi\.saveScheduleConfig\(\{\s*routeId:\s*props\.routeId,\s*routeVersionId:\s*routeVersionId\.value,/,
  '工序设置表保存排产 flow-config 必须携带当前 activeRouteVersionId。'
)
assert.match(
  routeProcessList,
  /ProRouteFlowConfigApi\.saveBatchRecordConfig\(\{\s*routeId:\s*props\.routeId,\s*routeVersionId:\s*routeVersionId\.value,/,
  '工序设置表保存批记录 flow-config 必须携带当前 activeRouteVersionId。'
)
assert.match(
  routeFlowGraphDesigner,
  /ProRouteFlowConfigApi\.saveScheduleConfig\(\{\s*routeId:\s*props\.routeId,\s*routeVersionId:\s*activeRouteVersionId,/,
  '关系图选中工序保存排产 flow-config 必须携带当前 activeRouteVersionId。'
)
assert.match(
  routeFlowGraphDesigner,
  /ProRouteFlowConfigApi\.saveBatchRecordConfig\(\{\s*routeId:\s*props\.routeId,\s*routeVersionId:\s*activeRouteVersionId,/,
  '关系图选中工序保存批记录 flow-config 必须携带当前 activeRouteVersionId。'
)

console.log('mes-route-version-consistency-static PASS')
