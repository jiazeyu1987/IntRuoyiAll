const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const candidate = read(
  'main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineActiveOrderCandidate.java'
)
const responseVo = read(
  'main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlineActiveOrderRespVO.java'
)
const controller = read(
  'main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java'
)
const service = read(
  'main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
const serviceTest = read(
  'test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java'
)
const controllerTest = read(
  'test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineActiveOrderControllerTest.java'
)

assert.match(candidate, /import java\.math\.BigDecimal;/)
assert.match(candidate, /String productName,\s+BigDecimal quantity,\s+Long routeId/)
assert.match(responseVo, /import java\.math\.BigDecimal;/)
assert.match(responseVo, /private BigDecimal quantity;/)
assert.match(controller, /respVO\.setQuantity\(candidate\.quantity\(\)\);/)
assert.match(
  service,
  /validateActiveOrderSummary\(workOrder, item\);[\s\S]*item\.getName\(\),\s*workOrder\.getQuantity\(\),[\s\S]*route\.getId\(\)/
)
assert.match(
  service,
  /private static void validateActiveOrderSummary\([\s\S]*StrUtil\.isBlank\(item\.getName\(\)\)[\s\S]*workOrder\.getQuantity\(\) == null[\s\S]*compareTo\(BigDecimal\.ZERO\) <= 0/
)

for (const methodName of [
  'shouldListActiveOrdersFromUnifiedActiveOrderAuthority',
  'shouldRejectActiveOrderWithoutProductionQuantity',
  'shouldRejectActiveOrderWithNonPositiveProductionQuantity',
  'shouldRejectActiveOrderWithoutProductName'
]) {
  assert.ok(serviceTest.includes(`void ${methodName}()`), `Missing service test: ${methodName}`)
}
assert.ok(
  controllerTest.includes('void getPqcActiveOrders_mapsFormalProductNameAndProductionQuantity()'),
  'Controller response mapping must have a focused test.'
)

console.log('PASS: PQC active-order API exposes and validates formal product summary data')
