const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const detailPanel = path.join(
  root,
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const teamLeaderApi = path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts')

const detailSource = fs.readFileSync(detailPanel, 'utf8')
const apiSource = fs.readFileSync(teamLeaderApi, 'utf8')

function assertIncludes(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`${label} missing expected snippet: ${expected}`)
  }
}

function assertNotIncludes(source, unexpected, label) {
  if (source.includes(unexpected)) {
    throw new Error(`${label} must not include snippet: ${unexpected}`)
  }
}

assertIncludes(
  apiSource,
  'keyFlag?: boolean',
  'active order process detail API type must expose keyFlag'
)
assertIncludes(
  detailSource,
  'data-active-order-production-record-key-flag',
  'production record form title must expose a stable key-flag marker'
)
assertIncludes(
  detailSource,
  'label="总表"',
  'active order detail must expose a summary master tab'
)
assertIncludes(
  detailSource,
  'data-active-order-summary-tab',
  'summary master tab must expose a stable marker'
)
assertIncludes(
  detailSource,
  'data-active-order-summary-product-table',
  'summary tab must render product information table'
)
assertIncludes(
  detailSource,
  '<th>产品名称</th>',
  'summary product table must include product name'
)
assertIncludes(
  detailSource,
  '<th>型号规格</th>',
  'summary product table must include specification'
)
assertIncludes(
  detailSource,
  '<th>生产批号</th>',
  'summary product table must include batch code'
)
assertIncludes(
  detailSource,
  '<th>生产指令</th>',
  'summary product table must include blank production instruction field'
)
assertIncludes(
  detailSource,
  '<th>图号</th>',
  'summary product table must include blank drawing number field'
)
assertIncludes(
  detailSource,
  '<th>生产周期</th>',
  'summary product table must include blank production period field'
)
assertIncludes(
  detailSource,
  '{{ blankSummaryField }}',
  'summary tab must intentionally leave pending business-source fields blank'
)
assertIncludes(
  detailSource,
  'data-active-order-summary-material-batches-table',
  'summary tab must render component batch information table'
)
assertIncludes(
  detailSource,
  'summaryPickListMaterialPairs',
  'summary material batch table must render pick-list materials in two groups per row'
)
assertIncludes(
  detailSource,
  'data-active-order-summary-process-personnel-table',
  'summary tab must render process personnel and assembly date table'
)
assertIncludes(
  detailSource,
  'summaryProcessPersonnelPairs',
  'summary process personnel table must render processes in two groups per row'
)
assertIncludes(
  detailSource,
  "selectedProductionRecordProcess.keyFlag ? '关键工序' : '非关键工序'",
  'production record form must render keyFlag without special-node inference'
)
assertIncludes(
  detailSource,
  'team-leader-workbench__production-record-key-flag',
  'production record form must style the key-flag marker'
)
assertNotIncludes(
  detailSource,
  '<h3>生产记录表单</h3>',
  'production record form must not keep the generic form title as the visible main label'
)
assertIncludes(
  detailSource,
  'team-leader-workbench__production-record-process-title',
  'production record form must promote the process name to the main title'
)
assertIncludes(
  detailSource,
  '<h3 class="team-leader-workbench__production-record-process-title">',
  'production record form process name must be rendered as the title element'
)
assertIncludes(
  detailSource,
  'font-size: 24px;',
  'production record form process title must be visually larger'
)
assertIncludes(
  detailSource,
  'font-weight: 700;',
  'production record form process title must be bold'
)
assertIncludes(
  detailSource,
  'text-align: center;',
  'production record form process title must be centered'
)
assertIncludes(
  detailSource,
  'data-active-order-production-record-two-line-table',
  'production record table must expose the printable two-line table marker'
)
assertIncludes(
  detailSource,
  'data-active-order-production-record-main-row',
  'production record table must render the material and quantity information in the first row'
)
assertIncludes(
  detailSource,
  'data-active-order-production-record-device-row',
  'production record table must render device-related information in the second row'
)
assertIncludes(
  detailSource,
  'buildProductionRecordRowDeviceGroups(row)',
  'production record second row must use the current material row device groups'
)
assertIncludes(
  detailSource,
  'data-active-order-production-record-parameter-horizontal-table',
  'production record device row parameters must use a horizontal grouped table'
)
assertIncludes(
  detailSource,
  'v-for="parameter in deviceGroup.parameters"',
  'production record horizontal parameter table must create one parameter group per submitted parameter'
)
assertIncludes(
  detailSource,
  ':colspan="2"',
  'each production parameter group header must span reference and actual sub-columns'
)
assertNotIncludes(
  detailSource,
  '<th>参数</th>',
  'production record parameter table must not keep the vertical parameter-name column'
)
assertIncludes(
  detailSource,
  '<th>参考值</th>',
  'each production parameter display must expose a reference-value cell header'
)
assertIncludes(
  detailSource,
  '<th>实际值</th>',
  'each production parameter display must expose an actual-value cell header'
)
assertIncludes(
  detailSource,
  'data-active-order-production-record-parameter-reference',
  'production record parameter reference cells must have a stable marker'
)
assertIncludes(
  detailSource,
  'data-active-order-production-record-parameter-actual',
  'production record parameter actual cells must have a stable marker'
)
assertIncludes(
  detailSource,
  '设备名称：{{ deviceGroup.deviceNameText }}',
  'production record device row must display device names in the second row'
)
assertIncludes(
  detailSource,
  '设备编号：{{ deviceGroup.deviceCodeText }}',
  'production record device row must display device codes in the second row'
)
assertIncludes(
  detailSource,
  '<th>操作人</th>',
  'production record table must place operator at the end'
)
assertIncludes(
  detailSource,
  '<th>复核人</th>',
  'production record table must place reviewer at the end'
)
assertIncludes(
  detailSource,
  'page-break-inside: avoid;',
  'production record row groups must avoid splitting across printed pages where possible'
)
assertIncludes(
  detailSource,
  '@media print',
  'production record two-line table must include print-specific styles'
)
assertNotIncludes(
  detailSource,
  'data-active-order-production-record-device-parameter-table',
  'production record form must not keep a separate vertical device-parameter table'
)
assertNotIncludes(
  detailSource,
  '物料使用设备 / 设备参数',
  'production record form must not keep the separate device parameter section title'
)

console.log('PASS: active order production record key flag static contract')
