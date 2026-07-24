const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const routePagePath = path.join(root, 'src/views/mes/pro/route/index.vue')
const routeApiPath = path.join(root, 'src/api/mes/pro/route/index.ts')
const routePage = fs.readFileSync(routePagePath, 'utf8')
const routeApi = fs.readFileSync(routeApiPath, 'utf8')

const templateMatch = routePage.match(
  /<UnifiedListTemplate[\s\S]*?table-key="mes\.pro\.route\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '工艺路线列表必须继续使用标准列表模板。')
const template = templateMatch[0]

assert.match(
  routeApi,
  /activeRouteVersionNo\?:\s*string\s*\/\/ 当前激活路线版本号/,
  '路线分页 VO 必须包含后端返回的当前激活路线版本号字段。'
)

assert.match(
  routePage,
  /key:\s*'activeRouteVersionNo'[\s\S]*label:\s*'当前生效版本'/,
  '当前生效版本必须注册到路线列表显示字段配置。'
)
assert.match(
  template,
  /isRouteColumnVisible\('activeRouteVersionNo'\)[\s\S]*label="当前生效版本"[\s\S]*prop="activeRouteVersionNo"/,
  '路线列表必须渲染受显示字段控制的当前生效版本列。'
)
assert.match(
  template,
  /v-if="scope\.row\.activeRouteVersionNo"[\s\S]*openRouteVersionFromList\(scope\.row,\s*'active'\)[\s\S]*v-else[\s\S]*未生成版本/,
  '当前生效版本为空时必须显示占位符，不能留空误导用户。'
)
assert.match(
  template,
  /getRouteColumnMinWidthString\('activeRouteVersionNo',\s*140\)/,
  '当前生效版本列必须使用稳定宽度，避免挤压操作列。'
)

const activeVersionColumnIndex = template.indexOf("isRouteColumnVisible('activeRouteVersionNo')")
const productColumnIndex = template.indexOf("isRouteColumnVisible('productCodes')")
assert.ok(
  activeVersionColumnIndex > -1 && productColumnIndex > -1 && activeVersionColumnIndex < productColumnIndex,
  '当前生效版本列应位于状态/关系图之后、关联产品之前，便于列表扫描。'
)

console.log('PASS: mes route list active version column static contract')
