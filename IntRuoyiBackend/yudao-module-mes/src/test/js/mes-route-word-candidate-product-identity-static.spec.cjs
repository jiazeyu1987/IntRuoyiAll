const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const sourcePath = path.resolve(
  __dirname,
  '../../main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteGenerationServiceImpl.java'
)
const source = fs.readFileSync(sourcePath, 'utf8')

const formalSnapshotCalls = source.match(
  /configSnapshots\.put\("products", buildRouteProductSnapshots\(route\.getId\(\)\)\);/g
) || []
assert.equal(
  formalSnapshotCalls.length,
  2,
  '候选路线和新路线都必须把正式 itemId 产品绑定写入 configSnapshots.products'
)
assert.match(
  source,
  /private List<Map<String, Object>> buildRouteProductSnapshots\(Long routeId\)/,
  '必须有统一的正式路线产品快照构造入口'
)
assert.match(
  source,
  /routeProductMapper\.selectListByRouteId\(routeId\)/,
  '正式产品快照必须读取当前路线产品绑定'
)
assert.doesNotMatch(
  source,
  /configSnapshots\.put\("products", productNames\);/,
  '产品名称只能保留为展示信息，禁止继续充当正式产品快照身份'
)

console.log('mes-route-word-candidate-product-identity-static PASS')
