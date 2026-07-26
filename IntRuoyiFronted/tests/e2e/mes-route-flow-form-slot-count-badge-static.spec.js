const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

const assertIncludes = (expected, message) => {
  assert.ok(component.includes(expected), `${message}: expected ${JSON.stringify(expected)}`)
}

const assertMatch = (pattern, message) => {
  assert.match(component, pattern, message)
}

assertIncludes(
  'route-flow-graph-designer__node-form-count-badge',
  '工序节点必须在表单槽位选中时渲染表单数量徽标'
)
assertMatch(
  /v-if="\s*selectedProcessDetailFieldKey === FORM_SLOT_AGGREGATE_FIELD_KEY &&\s*getRouteNodeAdditionalFormCount\(data\.routeNode\) > 0\s*"/,
  '数量徽标只能在选中表单槽位且当前工序附加表单数量大于 0 时显示'
)
assertMatch(
  /:aria-label="`已绑定 \$\{getRouteNodeAdditionalFormCount\(data\.routeNode\)\} 个表单`"/,
  '数量徽标必须提供已绑定表单个数的可访问说明'
)
assertMatch(
  /:title="`已绑定 \$\{getRouteNodeAdditionalFormCount\(data\.routeNode\)\} 个表单`"/,
  '数量徽标必须提供已绑定表单个数的悬停说明'
)
assertMatch(
  /const getRouteNodeAdditionalFormCount = \(node: RouteFlowNodeVO\) =>[\s\S]*getRouteNodeBatchRecordBindings\(node\)[\s\S]*filter\(\s*\(binding\) =>[\s\S]*isRecordBindingConfigured\(binding\)[\s\S]*normalizeRecordBindingSlotType\(binding\.formSlotType, binding\.formBindingKey\) !== 'MAIN'[\s\S]*\.length/,
  '节点表单数量必须只统计动态 formBindings 中非 MAIN 的有效附加表单'
)
assertMatch(
  /const getRouteNodeAdditionalFormCount = \(node: RouteFlowNodeVO\) =>[\s\S]*return getRouteNodeBatchRecordBindings\(node\)[\s\S]*\.length[\s\S]*\n\}/,
  '附加表单数量 helper 不得累计 legacy batchRecordReports'
)
assert.doesNotMatch(
  component.match(/const getRouteNodeAdditionalFormCount = \(node: RouteFlowNodeVO\) =>[\s\S]*?\n\}/)?.[0] || '',
  /getRouteNodeLegacyBatchRecords|isLegacyBatchRecordConfigured|configuredLegacyReports/,
  '附加表单数量不得读取 legacy batchRecordReports 或 legacy configured predicates'
)
assertMatch(
  /if \(fieldKey === FORM_SLOT_AGGREGATE_FIELD_KEY\) \{[\s\S]*return getRouteNodeAdditionalFormCount\(node\) > 0 \? 'bound' : 'none'[\s\S]*\}/,
  '表单槽位零绑定必须返回 none，避免继续显示红色未绑定边框'
)
assertMatch(
  /\.route-flow-graph-designer__node-form-count-badge\s*\{[\s\S]*position:\s*absolute;[\s\S]*right:\s*8px;[\s\S]*top:\s*10px;[\s\S]*border:\s*3px solid #facc15/,
  '数量徽标必须绝对定位在节点右上黄框位置，不影响节点正文布局'
)

console.log('PASS: MES route flow form slot count badge static contract')
