const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mes/pro/route/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="mes\.pro\.route\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '工艺路线列表必须继续使用标准列表模板。')
const template = templateMatch[0]

const activeLabelIndex = template.indexOf('label="当前生效版本"')
assert.notEqual(activeLabelIndex, -1, '必须渲染当前生效版本列。')
const activeColumnStart = template.lastIndexOf('<el-table-column', activeLabelIndex)
const activeColumnEnd = template.indexOf('</el-table-column>', activeLabelIndex)
const activeColumn = template.slice(activeColumnStart, activeColumnEnd + '</el-table-column>'.length)

assert.match(
  activeColumn,
  /@click="openRouteVersionFromList\(scope\.row,\s*'active'\)"/,
  '当前生效版本数值必须可点击，并跳转到生效版本展示页。'
)
assert.match(
  activeColumn,
  /scope\.row\.activeRouteVersionNo/,
  '当前生效版本链接必须显示真实 activeRouteVersionNo。'
)

const pendingLabelIndex = template.indexOf('label="待发布版本"')
assert.notEqual(pendingLabelIndex, -1, '必须渲染待发布版本列。')
const pendingColumnStart = template.lastIndexOf('<el-table-column', pendingLabelIndex)
const pendingColumnEnd = template.indexOf('</el-table-column>', pendingLabelIndex)
const pendingColumn = template.slice(pendingColumnStart, pendingColumnEnd + '</el-table-column>'.length)

assert.match(
  pendingColumn,
  /@click="openRouteVersionFromList\(scope\.row,\s*'pending'\)"/,
  '待发布版本数值必须可点击，并跳转到对应候选版本展示页。'
)
assert.match(
  pendingColumn,
  /formatPendingRouteVersion\(scope\.row\)/,
  '待发布版本链接必须保留版本号、状态和多个候选数量展示。'
)

assert.match(
  source,
  /const openRouteVersionFromList = async \(row: ProRouteVO, target: 'active' \| 'pending'\) =>/,
  '必须提供统一的版本列跳转函数，避免两个列各自拼接路由。'
)
assert.match(
  source,
  /target === 'active'[\s\S]*router\.push\(\{[\s\S]*name:\s*'MesProRouteEdit'[\s\S]*query:\s*\{\s*tab:\s*'flow'\s*\}/,
  '点击当前生效版本必须进入生效版本只读展示，不携带候选 routeVersionId。'
)
assert.match(
  source,
  /target === 'pending'[\s\S]*routeVersionId:\s*String\(row\.pendingRouteVersionId\)[\s\S]*routeVersionNo:\s*row\.pendingRouteVersionNo[\s\S]*routeVersionStatus:\s*row\.pendingRouteVersionStatus/,
  '点击待发布版本必须携带后端返回的候选版本 ID、版本号和状态。'
)
assert.doesNotMatch(
  source,
  /catch\s*\{\s*\}/,
  '版本列跳转失败不得空 catch 静默吞错。'
)

console.log('PASS: mes route list version link navigation static contract')
