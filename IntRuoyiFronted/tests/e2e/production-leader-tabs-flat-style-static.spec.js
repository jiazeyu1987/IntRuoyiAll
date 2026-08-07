const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const sliceContentWrapByMarker = (marker) => {
  const markerIndex = source.indexOf(marker)
  assert.notEqual(markerIndex, -1, `Expected marker in TeamLeaderWorkbenchPage.vue: ${marker}`)
  const start = source.lastIndexOf('<ContentWrap', markerIndex)
  const end = source.indexOf('</ContentWrap>', markerIndex)
  assert.notEqual(start, -1, `Expected ContentWrap start for marker: ${marker}`)
  assert.notEqual(end, -1, `Expected ContentWrap end for marker: ${marker}`)
  return source.slice(start, end)
}

const assertProductionTabs = (block, label) => {
  assert.match(
    block,
    /class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"[\s\S]*data-production-leader-module-tabs/,
    `${label} must render production module tabs with the shared flat underline style.`
  )
  for (const tabLabel of ['人员管理', '报工管理', '活跃订单池', '看板', '异常', '工序配置']) {
    assert.match(block, new RegExp(`label="${tabLabel}"`), `${label} must keep ${tabLabel} tab visible.`)
  }
}

assert.match(
  source,
  /<ContentWrap\s+v-if="!showPqcModuleTabs\s+&&\s+!showProductionModuleTabs">[\s\S]*team-leader-workbench__header/,
  'Standalone header card must be hidden when production module tabs are embedded in content cards.'
)
assert.doesNotMatch(
  sliceContentWrapByMarker('data-team-leader-type-tabs'),
  /data-production-leader-module-tabs/,
  'Standalone header card must not keep production module tabs above the content area.'
)

const personnelBlock = sliceContentWrapByMarker('data-team-leader-production-personnel-tab')
assert.match(
  personnelBlock,
  /'team-leader-workbench__production-module-card':\s*showProductionModuleTabs/,
  '人员管理 content card must use compact production module card padding.'
)
assertProductionTabs(personnelBlock, '人员管理')
assert.ok(
  personnelBlock.indexOf('data-production-leader-module-tabs') <
    personnelBlock.indexOf('team-leader-workbench__personnel-tabs--embedded'),
  '人员管理 module tabs must appear before the personnel list sub-tab content.'
)

const reportBlock = sliceContentWrapByMarker('data-team-leader-report-workbench')
assert.match(
  reportBlock,
  /'team-leader-workbench__production-module-card':\s*showProductionModuleTabs/,
  '报工管理 content card must use compact production module card padding.'
)
assertProductionTabs(reportBlock, '报工管理')
assert.ok(
  reportBlock.indexOf('data-production-leader-module-tabs') < reportBlock.indexOf('<UnifiedListTemplate'),
  '报工管理 module tabs must appear before the report list template.'
)

const activeOrderBlock = sliceContentWrapByMarker('data-team-leader-active-order-pool-tab')
assert.match(
  activeOrderBlock,
  /'team-leader-workbench__production-module-card':\s*showProductionModuleTabs/,
  '活跃订单池 content card must use compact production module card padding.'
)
assertProductionTabs(activeOrderBlock, '活跃订单池')
assert.ok(
  activeOrderBlock.indexOf('data-production-leader-module-tabs') <
    activeOrderBlock.indexOf('<UnifiedListTemplate'),
  '活跃订单池 module tabs must appear before the active-order list.'
)

const dashboardBlock = sliceContentWrapByMarker('data-role-matrix-daily-close')
assert.match(
  dashboardBlock,
  /'team-leader-workbench__production-module-card':\s*showProductionModuleTabs/,
  '看板 content card must use compact production module card padding.'
)
assertProductionTabs(dashboardBlock, '看板')
assert.ok(
  dashboardBlock.indexOf('data-production-leader-module-tabs') <
    dashboardBlock.indexOf('team-leader-workbench__daily-close-grid'),
  '看板 module tabs must appear before the dashboard list area.'
)

const exceptionBlock = sliceContentWrapByMarker('data-team-leader-abnormal-report')
assert.match(
  exceptionBlock,
  /'team-leader-workbench__production-module-card':\s*showProductionModuleTabs/,
  '异常 content card must use compact production module card padding.'
)
assertProductionTabs(exceptionBlock, '异常')
assert.ok(
  exceptionBlock.indexOf('data-production-leader-module-tabs') < exceptionBlock.indexOf('<el-form'),
  '异常 module tabs must appear before the exception form.'
)

const processConfigBlock = sliceContentWrapByMarker('data-team-leader-process-config-tab')
assert.match(
  processConfigBlock,
  /'team-leader-workbench__production-module-card':\s*showProductionModuleTabs/,
  '工序配置 content card must use compact production module card padding.'
)
assertProductionTabs(processConfigBlock, '工序配置')
assert.ok(
  processConfigBlock.indexOf('data-production-leader-module-tabs') < processConfigBlock.indexOf('<el-table'),
  '工序配置 module tabs must appear before the unified process config table.'
)

const configBlock = sliceContentWrapByMarker('data-team-leader-config-center')
assert.doesNotMatch(
  configBlock,
  /data-production-leader-module-tabs|team-leader-workbench__production-module-card/,
  'The legacy team configuration center must not retain the removed production module tab surface.'
)

assert.match(
  source,
  /\.team-leader-workbench__production-module-card\s+:deep\(\.el-card__body\)\s*\{[\s\S]*padding-top:\s*12px/,
  'Production module cards must use compact top padding.'
)
assert.match(
  source,
  /\.team-leader-workbench__personnel-tabs--embedded\s+:deep\(\.el-tabs__header\)\s*\{[\s\S]*display:\s*none/,
  'The single personnel sub-tab header must be hidden when production module tabs are visible.'
)
assert.match(
  source,
  /\.team-leader-workbench__module-tabs--flat\s+:deep\(\.el-tabs__header\)\s*\{[\s\S]*margin:\s*0 0 12px/,
  'Flat module tabs must keep the same compact spacing as the PQC module tabs.'
)
assert.match(
  source,
  /\.team-leader-workbench__module-tabs--flat\s+:deep\(\.el-tabs__active-bar\)\s*\{[\s\S]*background-color:\s*#00a896/,
  'Flat module tabs must keep the same active underline color as the PQC module tabs.'
)

console.log('PASS: production leader flat module tabs static contract')
