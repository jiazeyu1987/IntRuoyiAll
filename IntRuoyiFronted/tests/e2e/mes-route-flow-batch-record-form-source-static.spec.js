const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const flowGraph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const flowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')

assert.match(
  flowConfigApi,
  /batchRecordReports\?:\s*ProRouteFlowBatchRecordVO\[\][\s\S]*formBindings\?:\s*ProRouteFlowFormBindingVO\[\]/,
  '工序配置响应必须分别承载正式批记录表单和表单槽位。'
)

assert.match(
  flowGraph,
  /const getSelectedBatchRecordForms = \(\) =>[\s\S]*selectedLegacyBatchRecords\.value[\s\S]*isMainBatchRecordForm/,
  '右侧批记录表单必须只从当前路线工序的 batchRecordReports 读取。'
)
assert.match(
  flowGraph,
  /const buildBatchRecordFormValue = \(\) =>[\s\S]*getSelectedBatchRecordForms\(\)[\s\S]*getLegacyBatchRecordDisplayName/,
  '批记录表单字段值必须使用正式批记录表单专用构建函数。'
)
assert.match(
  flowGraph,
  /const buildBatchRecordFormLinks = \(\): ProcessDetailLinkItem\[\] =>[\s\S]*getSelectedBatchRecordForms\(\)[\s\S]*openLegacyBatchRecordTargetLink/,
  '批记录表单链接必须使用正式批记录报表 ID。'
)
assert.match(
  flowGraph,
  /const isRouteNodeBatchRecordFormConfigured = \(node: RouteFlowNodeVO\) =>[\s\S]*getRouteNodeBatchRecordForms\(node\)[\s\S]*isLegacyBatchRecordConfigured/,
  '批记录表单节点红绿状态必须只读取对应 routeProcessId 的正式批记录表单。'
)
assert.match(
  flowGraph,
  /key:\s*'batchRecordFormNames'[\s\S]*value:\s*buildBatchRecordFormValue\(\)[\s\S]*links:\s*buildBatchRecordFormLinks\(\)/,
  '批记录表单字段不得继续调用表单槽位值或链接构建函数。'
)
assert.match(
  flowGraph,
  /fieldKey === 'batchRecordFormNames'[\s\S]*isRouteNodeBatchRecordFormConfigured\(node\)/,
  '点击批记录表单字段时必须按正式批记录表单标记节点。'
)

const batchRecordValueBlock = flowGraph.match(
  /const buildBatchRecordFormValue = \(\) =>([\s\S]*?)\n\}/
)
assert.ok(batchRecordValueBlock, '必须存在正式批记录表单值构建函数。')
assert.doesNotMatch(
  batchRecordValueBlock[1],
  /selectedRecordBindings|getRecordBindingsBySlotType|formBindings/,
  '正式批记录表单值不得读取表单槽位。'
)

assert.match(
  flowGraph,
  /const buildFormSlotSummaryValue = \(\) => \{[\s\S]*selectedRecordBindings\.value/,
  '表单槽位汇总必须继续只读取 formBindings 对应状态。'
)
assert.match(
  flowGraph,
  /const loadBatchRecordAttachmentOwners = async[\s\S]*getBatchRecordAttachmentOwners/,
  '工序开始附件上传人必须继续走 batchRecordAttachmentOwners 独立接口。'
)

console.log('PASS: MES route flow batch record form source static contract')
