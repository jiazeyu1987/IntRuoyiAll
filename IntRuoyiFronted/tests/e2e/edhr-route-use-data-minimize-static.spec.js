const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const routeFlowConfigPanel = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue'), 'utf8')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const reportColumnTemplate = extractBetween(
  routeFlowConfigPanel,
  '<el-table-column v-if="configType === \'BATCH\'" label="批记录表单"',
  '<el-table-column label="备注"'
)
for (const requiredToken of [
  'route-flow-config-panel-filler-cell',
  'openProcessFormPermissionDialog(row, report)'
]) {
  assert.ok(reportColumnTemplate.includes(requiredToken), `route-flow-config-panel page must keep required token: ${requiredToken}`)
}

for (const fillRuleToken of ['填写人：', 'buildFillRuleStatus(report).label']) {
  assert.ok(routeFlowConfigPanel.includes(fillRuleToken), `route-flow-config-panel page must keep fill rule label: ${fillRuleToken}`)
}

for (const redundantToken of [
  '请选择电子批记录报表',
  'resolveFormSlotTypeLabel(report.formSlotType)',
  'route-flow-config-panel-report-list__sort',
  'route-flow-config-panel-report-select',
  'route-flow-config-panel-report-category-tag',
  '生产填写人：{{ buildFillRuleStatus(report).label }}',
  '设置生产填写人',
  'v-for="fillRuleKind in fillRuleKindOptions"',
  '设备填写',
  '质量填写',
  '添加批记录表格',
  'moveBatchRecordReport(scope.row',
  '历史基础字段',
  '槽位类型：',
  '必填策略：',
  '责任角色：',
  '权限范围：',
  '归档属性：',
  'route-flow-config-panel-report-slot-meta',
  'route-flow-config-panel-report-meta-select'
]) {
  assert.ok(!reportColumnTemplate.includes(redundantToken), `route-flow-config-panel page must hide redundant token: ${redundantToken}`)
}

for (const inheritedToken of [
  'placeholder="槽位"',
  'placeholder="必填策略"',
  'placeholder="权限范围"',
  'removeBatchRecordReport(row'
]) {
  assert.ok(reportColumnTemplate.includes(inheritedToken), `route-flow-config-panel page must preserve inherited batch-route config token: ${inheritedToken}`)
}

assert.ok(
  routeFlowConfigPanel.includes('.route-flow-config-panel-filler-cell'),
  'route-flow-config-panel report row must collapse to the required single filler setting cell'
)
assert.ok(!reportColumnTemplate.includes('repeat(4'), 'route-flow-config-panel report row must not keep four internal metadata columns')

console.log('PASS: eDHR route-flow-config-panel data minimize static contract')
