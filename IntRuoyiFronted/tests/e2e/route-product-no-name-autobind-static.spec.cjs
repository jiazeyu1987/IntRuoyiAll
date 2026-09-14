const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const readFrontend = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const readBackend = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, 'IntRuoyiBackend', relativePath), 'utf8')

const routeProductList = readFrontend('src/views/mes/pro/route/RouteProductList.vue')
const routeList = readFrontend('src/views/mes/pro/route/index.vue')
const routeProductApi = readFrontend('src/api/mes/pro/route/product/index.ts')
const routeProductController = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesProRouteProductController.java'
)
const routeProductService = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteProductService.java'
)
const routeProductServiceImpl = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteProductServiceImpl.java'
)
const routeProductErrorCodes = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java'
)

assert.match(routeProductList, /关联产品/)
assert.match(routeProductList, /getItemByCode\(code\)/)
assert.match(routeProductList, /itemId:\s*item\.id/)
assert.match(routeProductList, /createRouteProduct\(payload\)/)
for (const source of [routeProductList, routeList]) {
  assert.doesNotMatch(source, /补齐产品|从生产订单补齐|handleBind(?:RouteProducts|FromWorkOrders)/)
}

assert.doesNotMatch(
  routeProductApi,
  /BindFromWorkOrders|bind-from-work-orders|previewBindFromWorkOrders|bindFromWorkOrders/
)
assert.doesNotMatch(
  routeProductController,
  /BindFromWorkOrders|bind-from-work-orders|previewBindFromWorkOrders|bindFromWorkOrders/
)
assert.doesNotMatch(
  routeProductService,
  /BindFromWorkOrders|previewBindFromWorkOrders|bindFromWorkOrders/
)
assert.doesNotMatch(
  routeProductServiceImpl,
  /BindFromWorkOrders|previewBindFromWorkOrders|bindFromWorkOrders|buildBindFromWorkOrdersResult/
)
assert.doesNotMatch(
  routeProductErrorCodes,
  /PRO_ROUTE_PRODUCT_WORK_ORDER_MATCH_EMPTY|PRO_ROUTE_PRODUCT_ROUTE_NAME_EMPTY/
)
for (const relativePath of [
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/product/MesProRouteProductBindFromWorkOrdersReqVO.java',
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/product/MesProRouteProductBindFromWorkOrdersRespVO.java',
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProRouteProductBindFromWorkOrdersTest.java'
]) {
  assert.equal(
    fs.existsSync(path.join(workspaceRoot, 'IntRuoyiBackend', relativePath)),
    false,
    `旧名称补齐专用文件必须删除：${relativePath}`
  )
}

console.log('PASS: route product binding has no route-name-based work-order auto-bind path')
