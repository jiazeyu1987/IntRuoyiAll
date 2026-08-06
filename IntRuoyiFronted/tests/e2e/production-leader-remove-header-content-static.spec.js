const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
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

const productionBlocks = [
  ['data-team-leader-production-personnel-tab', '人员管理'],
  ['data-team-leader-report-workbench', '报工管理'],
  ['data-team-leader-active-order-pool-tab', '活跃订单池'],
  ['data-role-matrix-daily-close', '看板'],
  ['data-team-leader-abnormal-report', '异常'],
  ['data-team-leader-loss-reason-tab', '损耗管理'],
  ['data-team-leader-config-center', '班组配置']
]

for (const [marker, label] of productionBlocks) {
  const block = sliceContentWrapByMarker(marker)
  assert.doesNotMatch(
    block,
    /<div\s+v-if="showProductionModuleTabs"\s+class="team-leader-workbench__embedded-header">/,
    `${label} must not render the redundant production leader title and subtitle above the module tabs.`
  )
  assert.match(
    block,
    /data-production-leader-module-tabs/,
    `${label} must keep the production module tabs after removing the redundant header.`
  )
}

const personnelBlock = sliceContentWrapByMarker('data-team-leader-production-personnel-tab')
assert.doesNotMatch(
  personnelBlock,
  /<div\s+class="team-leader-workbench__section-title">生产人员档案<\/div>/,
  '人员管理 must not render the redundant 生产人员档案 section title.'
)
assert.doesNotMatch(
  personnelBlock,
  /只维护已关联当前生产组长的员工/,
  '人员管理 must not render the redundant personnel maintenance description.'
)
assert.doesNotMatch(
  personnelBlock,
  /刷新人员档案/,
  '人员管理 must not render the redundant refresh personnel button.'
)
assert.match(
  personnelBlock,
  /data-team-leader-open-personnel-dialog/,
  '人员管理 must keep the 新增人员 action.'
)
assert.match(
  personnelBlock,
  /data-team-leader-production-personnel-list/,
  '人员管理 must keep the personnel list.'
)
assert.match(
  personnelBlock,
  /@pagination="refreshProductionPersonnel"/,
  'Removing the visible refresh button must not remove list pagination refresh behavior.'
)

const pqcPersonnelBlock = sliceContentWrapByMarker('data-pqc-leader-personnel-tab')
assert.match(
  pqcPersonnelBlock,
  /<div\s+v-if="showPqcModuleTabs"\s+class="team-leader-workbench__embedded-header">/,
  'PQC leader header is outside the requested deletion scope and must remain.'
)

console.log('PASS: production leader redundant header content removal static contract')
