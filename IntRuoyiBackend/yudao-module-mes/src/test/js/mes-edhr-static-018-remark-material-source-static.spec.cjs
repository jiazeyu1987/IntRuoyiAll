const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../..')
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')

const formalResolver = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFormalProductionPickListSourceResolver.java'
)
const completionSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderPickListCompletionSourceService.java'
)
const batchQuery = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFeedbackMaterialBatchQueryServiceImpl.java'
)
const activeOrderService = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
)

assert.match(formalResolver, /selectListByProductionOrderNo\(productionOrderNo\)/,
  'formal pick-list sources must be read by production order number')
assert.match(formalResolver, /StrUtil\.trim\(workOrder\.getCode\(\)\)/,
  'production order number must come from the current work-order code')
assert.doesNotMatch(formalResolver, /workOrder\.getRemark\(\)/,
  'formal source resolution must not inspect free-text work-order remarks')
assert.doesNotMatch(formalResolver, /sourceActiveOrderIdFromMarker|SOURCE_ACTIVE_ORDER_/,
  'free-text sourceActiveOrderId markers must not redirect formal source resolution')
assert.doesNotMatch(formalResolver, /MesProcessPoolActiveOrderMapper|activeOrderMapper/,
  'formal source resolution must not load another active order while resolving one work order')
assert.match(completionSource, /sourceResolver\.resolve\(activeOrder\.getWorkOrderId\(\)\)/,
  'completion freezing must resolve sources from the active order work-order identity')
assert.match(batchQuery, /sourceResolver\.resolve\(workOrderId\)/,
  'frontline batch lookup must resolve sources from the requested work-order identity')
assert.match(activeOrderService, /setRemark\("\[SIM-COPY\]\[sourceActiveOrderId="/,
  'simulation copy may keep its human-readable remark marker for audit display')
assert.match(activeOrderService, /\.simulated\(Boolean\.TRUE\)[\s\S]*\.simulationStage\(SIMULATION_STAGE_LATEST_VERSION_COPY\)[\s\S]*\.simulationRunId\(simulationRunId\)/,
  'simulation copies must retain controlled metadata outside the free-text remark')

console.log('mes-edhr-static-018-remark-material-source-static: PASS')
