const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const page = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteProductList.vue'),
  'utf8'
)
const api = fs.readFileSync(
  path.resolve(process.cwd(), 'src/api/mes/pro/route/product/index.ts'),
  'utf8'
)

assert.match(
  api,
  /getRouteProductListByRoute:\s*async\s*\(routeId:\s*number,\s*routeVersionId\?:\s*MesRouteId\)[\s\S]*params:\s*\{\s*routeId,\s*routeVersionId\s*\}/,
  '关联产品列表 API 必须把当前路线版本编号传给后端。'
)
assert.match(
  page,
  /getRouteProductListByRoute\(\s*props\.routeId,\s*props\.routeVersionEditContext\?\.routeVersionId\s*\)/,
  '候选编辑页列表刷新必须携带当前候选版本编号。'
)
assert.match(page, /row-key="itemId"/, '候选快照行必须以正式产品 itemId 作为稳定行身份。')
assert.match(
  page,
  /sourceItemId:\s*row\.itemId/,
  '打开复制弹窗时必须保存候选快照的正式产品 itemId。'
)
assert.match(
  page,
  /copyCandidateRouteProduct\(\{[\s\S]*routeId:\s*props\.routeId[\s\S]*routeVersionId[\s\S]*sourceItemId:\s*copyFormData\.value\.sourceItemId/,
  '提交候选产品复制时必须按路线、版本和正式 itemId 定位候选快照项。'
)
assert.match(
  page,
  /deleteCandidateRouteProduct\(\s*props\.routeId,\s*row\.itemId,\s*requireCandidateRouteVersionId/,
  '候选产品删除必须按路线、版本和正式 itemId 定位候选快照项。'
)

console.log('mes-route-product-candidate-list-static PASS')
