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

for (const field of [
  ['pendingRouteVersionId', 'number'],
  ['pendingRouteVersionNo', 'string'],
  ['pendingRouteVersionStatus', 'ProRouteVersionLifecycleStatus'],
  ['pendingRouteVersionCount', 'number']
]) {
  assert.match(
    routeApi,
    new RegExp(`${field[0]}\\?:\\s*${field[1]}`),
    `路线分页 VO 必须包含 ${field[0]} 字段。`
  )
}

assert.match(
  routePage,
  /key:\s*'pendingRouteVersionNo'[\s\S]*label:\s*'待发布版本'/,
  '待发布版本必须注册到路线列表显示字段配置。'
)
assert.match(
  template,
  /isRouteColumnVisible\('pendingRouteVersionNo'\)[\s\S]*label="待发布版本"[\s\S]*prop="pendingRouteVersionNo"/,
  '路线列表必须渲染受显示字段控制的待发布版本列。'
)
assert.match(
  template,
  /formatPendingRouteVersion\(scope\.row\)/,
  '待发布版本列必须格式化版本号、状态和多个候选数量。'
)
assert.match(
  routePage,
  /const formatPendingRouteVersion = \(row: ProRouteVO\) =>/,
  '必须提供待发布版本展示格式化函数。'
)
assert.match(
  routePage,
  /pendingRouteVersionCount[\s\S]*\+\$\{row\.pendingRouteVersionCount - 1\}/,
  '多个候选版本必须在主列表用 +N 提示。'
)
assert.match(
  template,
  /<el-tag[\s\S]*v-if="scope\.row\.pendingRouteVersionNo"[\s\S]*resolveRouteVersionStatusTagType\(scope\.row\.pendingRouteVersionStatus/,
  '存在候选版本时必须用状态 tag 明确区分草稿、审批中和待发布。'
)
assert.match(
  template,
  /v-else[\s\S]*route-list__muted[\s\S]*无/,
  '没有候选版本时必须显示“无”，不能留空误导用户。'
)
assert.match(
  template,
  /getRouteColumnMinWidthString\('pendingRouteVersionNo',\s*160\)/,
  '待发布版本列必须使用稳定宽度，避免挤压操作列。'
)

const activeVersionColumnIndex = template.indexOf("isRouteColumnVisible('activeRouteVersionNo')")
const pendingVersionColumnIndex = template.indexOf("isRouteColumnVisible('pendingRouteVersionNo')")
const productColumnIndex = template.indexOf("isRouteColumnVisible('productCodes')")
assert.ok(
  activeVersionColumnIndex > -1
    && pendingVersionColumnIndex > activeVersionColumnIndex
    && pendingVersionColumnIndex < productColumnIndex,
  '待发布版本列应位于当前生效版本之后、关联产品之前，便于列表扫描。'
)

const actionsLabelIndex = template.indexOf('label="操作"')
const actionsColumnStart = template.lastIndexOf('<el-table-column', actionsLabelIndex)
const actionsColumnEnd = template.indexOf('</el-table-column>', actionsLabelIndex)
const actionsColumn = template.slice(actionsColumnStart, actionsColumnEnd + '</el-table-column>'.length)
assert.doesNotMatch(
  actionsColumn,
  /v-if="isRouteColumnVisible\('actions'\)"/,
  '新增待发布版本列不能再次把操作列变成可隐藏列。'
)

console.log('PASS: mes route list pending version column static contract')
