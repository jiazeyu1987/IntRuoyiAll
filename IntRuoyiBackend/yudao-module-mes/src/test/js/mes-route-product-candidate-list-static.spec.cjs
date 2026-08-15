const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../..')
const controller = fs.readFileSync(
  path.join(
    moduleRoot,
    'main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesProRouteProductController.java'
  ),
  'utf8'
)
const service = fs.readFileSync(
  path.join(
    moduleRoot,
    'main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteProductServiceImpl.java'
  ),
  'utf8'
)

assert.match(
  controller,
  /@GetMapping\("\/list-by-route"\)[\s\S]*@RequestParam\(value = "routeVersionId", required = false\) Long routeVersionId[\s\S]*getRouteProductListByRouteId\(routeId, routeVersionId\)/,
  '候选产品列表接口必须接收并传递当前路线版本编号。'
)
assert.match(
  service,
  /getRouteProductListByRouteId\(Long routeId, Long routeVersionId\)[\s\S]*requireReadableRouteVersion\(routeVersionId, routeId\)[\s\S]*resolveConfigSnapshot\(routeVersion, PRODUCTS_CONFIG_KEY\)/,
  '指定候选版本时必须读取该版本的产品快照。'
)
assert.match(
  service,
  /if \(routeVersionId == null\) \{\s*return getRouteProductListByRouteId\(routeId\);\s*\}/,
  '未指定版本时必须继续读取正式产品关系。'
)
assert.match(
  service,
  /if \(products\.isEmpty\(\)\) \{\s*return List\.of\(\);\s*\}/,
  '候选快照明确为空时不得重新带回正式产品。'
)
assert.match(
  controller,
  /@PostMapping\("\/copy-candidate"\)[\s\S]*copyCandidateRouteProduct/,
  '候选列表必须提供按候选快照复制产品的正式入口。'
)
assert.match(
  controller,
  /@DeleteMapping\("\/delete-candidate"\)[\s\S]*deleteCandidateRouteProduct/,
  '候选列表必须提供按候选快照删除产品的正式入口。'
)

console.log('mes-route-product-candidate-list-static PASS')
