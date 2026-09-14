const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8').replace(/\r\n/g, '\n')

const qaPage = read('src/views/mes/pro/processpool/QaRegulationPage.vue')
const routeProductApi = read('src/api/mes/pro/route/product/index.ts')

const manualRouteOptionMatch = qaPage.match(
  /<el-option\s+v-for="route in manualQaRouteOptions"[\s\S]*?:label="formatManualQaRouteOption\(route\)"[\s\S]*?\/>/
)
assert.ok(
  manualRouteOptionMatch,
  'QA manual route binding must render route options from manualQaRouteOptions.'
)

const manualRouteOptionBlock = manualRouteOptionMatch[0]
assert.match(
  manualRouteOptionBlock,
  /:disabled="false"/,
  'QA manual route binding options must be explicitly selectable, including published/enabled routes.'
)
assert.doesNotMatch(
  manualRouteOptionBlock,
  /isManualQaRouteOptionDisabled|CommonStatusEnum\.ENABLE/,
  'QA manual route binding must not reuse the product-side enabled-route disabled guard.'
)
assert.match(
  qaPage,
  /const formatManualQaRouteOption = \(route: ProRouteVO\) =>[\s\S]*'可绑定'/,
  'QA manual route option label must show selectable QA binding semantics.'
)
assert.doesNotMatch(
  qaPage,
  /已启用，仅回显|所选工艺路线已启用，不能在产品侧变更绑定/,
  'QA page must not show product-side read-only route wording in manual binding.'
)
assert.match(
  qaPage,
  /ProRouteProductApi\.saveQaRegulationRouteProductByItem\(\{ itemId: productId, routeId \}\)/,
  'QA manual route binding must call the QA-specific route-product binding API.'
)
assert.match(
  routeProductApi,
  /saveQaRegulationRouteProductByItem:\s*async \(data: ProRouteProductByItemSaveReqVO\)[\s\S]*\/mes\/pro\/route-product\/save-qa-regulation-route-by-item/,
  'Route-product API wrapper must expose the QA-specific route binding endpoint.'
)

console.log('PASS qa-regulation-manual-route-selectable-static')
