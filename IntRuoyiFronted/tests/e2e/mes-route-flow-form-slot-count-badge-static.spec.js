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
  /const getRouteNodeAdditionalFormCount = \(node: RouteFlowNodeVO\) =>[\s\S]*getRouteNodeBatchRecordBindings\(node\)[\s\S]*filter\(\s*\(binding\) =>[\s\S]*normalizeRecordBindingSlotType\(binding\.formSlotType, binding\.formBindingKey\) !== 'MAIN'[\s\S]*\.length/,
  '节点表单数量必须按动态 formBindings 中非 MAIN 槽位行计数'
)
assert.doesNotMatch(
  component.match(/const getRouteNodeAdditionalFormCount = \(node: RouteFlowNodeVO\) =>[\s\S]*?\n\}/)?.[0] || '',
  /isRecordBindingConfigured|formTemplateId/,
  '点击新增表单后新行尚未选择模板也必须立即计数，数量 helper 不得再要求 formTemplateId > 0'
)
assertMatch(
  /const ADDITIONAL_RECORD_BINDING_SLOT_TYPES = RECORD_BINDING_SLOT_TYPES\.filter\(\s*\(slot\) => slot !== 'MAIN'\s*\)/,
  '新增动态表单的候选槽位必须显式排除 MAIN，避免新增后仍被批记录口径排除'
)
assertMatch(
  /const resolveNextAdditionalRecordBindingSlotType = \(\): ProRouteFlowFormSlotType => \{[\s\S]*selectedRecordBindings\.value[\s\S]*normalizeRecordBindingSlotType\(binding\.formSlotType, binding\.formBindingKey\)[\s\S]*ADDITIONAL_RECORD_BINDING_SLOT_TYPES\.find\(\(slot\) => !usedSlotTypes\.has\(slot\)\)[\s\S]*ADDITIONAL_RECORD_BINDING_SLOT_TYPES\[ADDITIONAL_RECORD_BINDING_SLOT_TYPES\.length - 1\]/,
  '新增动态表单必须选择下一个非 MAIN 槽位，已有 1 个动态表单时第二个新增项应计入节点数字 2'
)
assertMatch(
  /const createEmptyRecordBinding = \(\): RouteFlowRecordBinding => \(\{[\s\S]*formSlotType: resolveNextAdditionalRecordBindingSlotType\(\),[\s\S]*formTemplateId: null/,
  '新增表单的本地空绑定不得继续默认 MAIN，选择模板后应立即被数量徽标统计'
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
