const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.resolve(moduleRoot, relativePath), 'utf8')
const sliceBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.ok(startIndex >= 0, `Cannot find start anchor: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.ok(endIndex > startIndex, `Cannot find end anchor after: ${start}`)
  return source.slice(startIndex, endIndex)
}

const activeOrderService = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
)
const completionProgressPort = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionProgressPortImpl.java'
)
const orderProcessCompletionService = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderOrderProcessCompletionService.java'
)
const outputMaterialCalculator = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesOutputMaterialProgressCalculator.java'
)
const eventMapper = read(
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProProcessPoolEventMapper.java'
)
const routeCandidateConfigService = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteCandidateConfigServiceImpl.java'
)

const snapshotBuilderBlock = sliceBetween(
  activeOrderService,
  'private ProductionProcessConfigSnapshot resolveProductionProcessConfigSnapshot',
  'private JSONObject requireRouteProductionProcessConfig'
)
assert(
  snapshotBuilderBlock.includes('requireOutputMaterialIds(activeOrder, process, routeProductionConfig.getJSONArray("outputMaterialIds"))') &&
    snapshotBuilderBlock.includes('snapshot.put("outputMaterialIds", JSON.parseArray(outputMaterialIdsJson))') &&
    snapshotBuilderBlock.includes('JSON.toJSONString(snapshot)'),
  'Active-order production config snapshot must write frozen outputMaterialIds from the formal route snapshot.'
)

const routeConfigBlock = sliceBetween(
  activeOrderService,
  'private JSONObject requireRouteProductionProcessConfig',
  'private String canonicalProductionArray'
)
assert(
  routeConfigBlock.includes('config.getJSONArray("outputMaterialIds") == null'),
  'Active-order creation must fail fast when the formal route production config lacks outputMaterialIds.'
)

const routeValidationBlock = sliceBetween(
  routeCandidateConfigService,
  'public static void validateProductionProcessConfigs',
  'private static void validateDeviceConfiguration'
)
assert(
  routeValidationBlock.includes('validateOutputMaterialIds(routeVersionId, config.getJSONArray("outputMaterialIds"))'),
  'Route production config validation must require a non-empty frozen outputMaterialIds list.'
)

assert(
  outputMaterialCalculator.includes('final class MesOutputMaterialProgressCalculator') &&
    outputMaterialCalculator.includes('calculateConservativeProcessProgress') &&
    outputMaterialCalculator.includes('parseRequiredOutputMaterialIds') &&
    outputMaterialCalculator.includes('PRODUCTION_OUTPUT_MATERIAL_IDS_REQUIRED') &&
    outputMaterialCalculator.includes('currentAllocationQuantityByEventId') &&
    outputMaterialCalculator.includes('outputQuantity.min(allocatedQuantity)') &&
    !outputMaterialCalculator.includes('return allocationProgress == null ? BigDecimal.ZERO : allocationProgress'),
  'Shared output-material progress calculator must fail fast on missing outputMaterialIds, cap event materialDetails by formal allocation quantity, and must not fall back to allocation progress.'
)

assert(
  eventMapper.includes('selectProductionSubmitsByWorkOrderIdsAndRouteIds') &&
    eventMapper.includes('in(MesProProcessPoolEventDO::getWorkOrderId, workOrderIds)') &&
    eventMapper.includes('in(MesProProcessPoolEventDO::getRouteId, routeIds)'),
  'Production event mapper must support bulk list-progress reads by work order and route.'
)

const listProgressBlock = sliceBetween(
  activeOrderService,
  'private Map<Long, ActiveOrderProgress> loadActiveOrderProgress',
  'private BigDecimal requireAllocationQuantity'
)
assert(
  listProgressBlock.includes('processPoolEventMapper.selectProductionSubmitsByWorkOrderIdsAndRouteIds') &&
    listProgressBlock.includes('resolveConservativeProcessProgressQuantities') &&
    listProgressBlock.includes('MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress') &&
    listProgressBlock.includes('progressQuantityByProcess'),
  'Active-order list progress and remaining quantities must use the same conservative output-material quantity.'
)

assert(
  completionProgressPort.includes('MesProProcessPoolEventMapper') &&
    completionProgressPort.includes('eventMapper.selectProductionSubmitsByWorkOrderAndRouteForUpdate') &&
    completionProgressPort.includes('MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress'),
  'Completion gate progress port must calculate production completion from frozen output materials and locked production events.'
)

assert(
  orderProcessCompletionService.includes('MesProcessPoolActiveOrderProcessSnapshotMapper') &&
    orderProcessCompletionService.includes('MesProProcessPoolEventMapper') &&
    orderProcessCompletionService.includes('eventMapper.selectProductionSubmitsByWorkOrderAndRouteForUpdate') &&
    orderProcessCompletionService.includes('MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress'),
  'Order process completion records must use the same conservative output-material completion quantity as the release gate.'
)

console.log('PASS: EDHR-STATIC-013 output-material snapshot and progress contract')
