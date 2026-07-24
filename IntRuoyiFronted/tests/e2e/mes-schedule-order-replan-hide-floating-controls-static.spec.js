const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const pagePath = path.resolve(root, 'src/views/mes/pro/scheduleorder/index.vue')
const enhancerPath = path.resolve(
  root,
  'src/components/UserTableColumnGlobalEnhancer/index.vue'
)

const pageSource = fs.readFileSync(pagePath, 'utf8')
const enhancerSource = fs.readFileSync(enhancerPath, 'utf8')
const drawerStart = pageSource.indexOf(
  '<el-drawer v-model="replanDrawerVisible" title="排产前检查 / 手动重排"'
)
const drawerEnd = pageSource.indexOf('</el-drawer>', drawerStart)

assert.notEqual(drawerStart, -1, '排产前检查 / 手动重排抽屉必须存在。')
assert.notEqual(drawerEnd, -1, '排产前检查 / 手动重排抽屉必须正常闭合。')

const drawerSource = pageSource.slice(drawerStart, drawerEnd)

const assertTableOptedOut = (marker, tableName) => {
  const markerIndex = drawerSource.indexOf(marker)
  assert.notEqual(markerIndex, -1, `${tableName}必须存在。`)

  const tableStart = drawerSource.lastIndexOf('<el-table', markerIndex)
  const tableOpenEnd = drawerSource.indexOf('>', markerIndex)
  assert.notEqual(tableStart, -1, `${tableName}必须由 el-table 渲染。`)
  assert.notEqual(tableOpenEnd, -1, `${tableName}开始标签必须正常闭合。`)

  const openingTag = drawerSource.slice(tableStart, tableOpenEnd + 1)
  assert(
    openingTag.includes('data-user-table-column-explicit'),
    `${tableName}必须排除全局浮动“显示字段 / 重置”控件。`
  )
}

assertTableOptedOut('v-if="preflightResult?.issues?.length"', '排产前检查问题表')
assertTableOptedOut('v-if="replanIssueRows.length"', '手动重排问题表')

assert(
  enhancerSource.includes("tableEl.hasAttribute('data-user-table-column-explicit')"),
  '全局列增强器必须继续支持显式表格排除契约。'
)
assert(
  pageSource.includes('<UserTableColumnSettings') &&
    pageSource.includes(':columns="scheduleOrderColumns"'),
  '排产工单主列表必须保留显式显示字段入口。'
)

console.log('PASS: MES schedule order replan floating column controls are hidden')
