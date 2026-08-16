const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/route/RouteProductList.vue'),
  'utf8'
)

assert.match(
  source,
  /placeholder="请输入产品名称或编号"/,
  '关联产品输入框必须明确支持产品名称或编号'
)
assert.match(
  source,
  /Promise\.all\(\[[\s\S]*?MdItemApi\.getItemPage\(\{ \.\.\.queryParams, code: keyword \}\)[\s\S]*?MdItemApi\.getItemPage\(\{ \.\.\.queryParams, name: keyword \}\)/,
  '候选查询必须用同一关键词分别走正式编号和名称查询'
)
assert.match(
  source,
  /const uniqueItems = new Map<number, MdItemVO>\(\)[\s\S]*?uniqueItems\.set\(item\.id, item\)/,
  '编号和名称查询结果必须按正式产品 ID 去重'
)
assert.match(
  source,
  /type RouteProductSuggestion = MdItemVO & \{ value: string; isLinked: boolean \}/,
  '候选模型必须携带当前路线关联状态'
)
assert.match(
  source,
  /Number\(left\.isLinked\)\s*-\s*Number\(right\.isLinked\)/,
  '候选必须按未关联在前、已关联在后排序'
)
assert.match(
  source,
  /route-product-suggestion--unlinked[\s\S]*?\{[\s\S]*?color:\s*var\(--el-color-success\)/,
  '未关联候选必须使用绿色显示'
)
assert.match(
  source,
  /route-product-suggestion--linked[\s\S]*?\{[\s\S]*?color:\s*var\(--el-color-danger\)/,
  '已关联候选必须使用红色显示'
)
assert.match(
  source,
  /\{\{ item\.code \}\}[\s\S]*?\{\{ item\.name \}\}[\s\S]*?\{\{ item\.isLinked \? '已添加' : '未添加' \}\}/,
  '每个候选必须同时显示产品编号、名称和关联状态'
)
assert.match(
  source,
  /const buildRouteProductSavePayload[\s\S]*?itemId:\s*item\.id/,
  '按名称搜索后保存仍必须使用正式产品 itemId'
)

console.log('mes-route-product-name-status-suggestions-static PASS')
