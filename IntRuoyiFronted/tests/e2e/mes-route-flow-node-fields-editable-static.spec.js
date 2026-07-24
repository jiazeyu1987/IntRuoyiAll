const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const componentPath = path.join(
  frontendRoot,
  'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)
const source = fs.readFileSync(componentPath, 'utf8')

const assertMatch = (pattern, message) => {
  assert.match(source, pattern, message)
}
const assertNotIncludes = (unexpected, message) => {
  assert.ok(!source.includes(unexpected), message)
}

for (const key of [
  'productionQuantityFactor',
  'predecessor',
  'successors',
  'keyFlag',
  'checkFlag'
]) {
  assertMatch(
    new RegExp(`PROCESS_DETAIL_EDITABLE_FIELD_KEYS[\\s\\S]*'${key}'`),
    `红框字段 ${key} 必须列入可编辑白名单。`
  )
}

assertMatch(
  /v-if="isProcessDetailFieldEditable\(selectedProcessDetailField\.key\)"[\s\S]*:data-flow-field-editor="selectedProcessDetailField\.key"/,
  '红框可编辑字段必须渲染稳定的内联编辑器选择器。'
)
assertMatch(
  /<el-input-number[\s\S]*selectedProcessDetailField\.key === 'productionQuantityFactor'[\s\S]*:min="0\.000001"[\s\S]*:precision="2"[\s\S]*@change="handleProductionQuantityFactorChange"/,
  '生产系数必须使用大于 0、两位精度的数字编辑器并同步草稿。'
)
assertMatch(
  /ROUTE_NODE_BINDING_STATUS_FIELD_KEYS[\s\S]*'productionQuantityFactor'/,
  '选择生产系数字段时，图节点必须进入覆盖状态绿红判定。'
)
assertMatch(
  /const isProductionQuantityFactorOverridden =[\s\S]*DEFAULT_PRODUCTION_QUANTITY_FACTOR/,
  '生产系数覆盖必须按是否不同于默认 1 判定，不能只看字段存在。'
)
assertMatch(
  /fieldKey === 'productionQuantityFactor'[\s\S]*isRouteNodeProductionQuantityFactorOverridden\(node\)[\s\S]*\? 'bound'[\s\S]*: 'missing'/,
  '生产系数字段选中后，已覆盖节点必须为绿色 bound，未覆盖节点必须为红色 missing。'
)
assertMatch(
  /coverageStatus: attributeLoading \? undefined : getSelectedProductionQuantityFactorCoverageStatus\(\)/,
  '生产系数字段必须派生当前工序覆盖状态，加载中不得显示错误颜色。'
)
assertMatch(
  /data-flow-panel="selected-field-coverage-status"[\s\S]*selectedProcessDetailField\.coverageStatus/,
  '生产系数已覆盖/未覆盖状态必须显示在右侧字段明细下方状态区域。'
)
assertMatch(
  /route-flow-graph-designer__selected-field-coverage[\s\S]*is-\$\{selectedProcessDetailField\.coverageStatus\}[\s\S]*已覆盖[\s\S]*未覆盖/,
  '右侧字段明细状态必须用已覆盖/未覆盖文本和状态 class 表达，不能只依赖颜色。'
)
assertNotIncludes(
  'route-flow-graph-designer__selected-detail-status',
  '静态契约必须禁止把覆盖状态继续放在左侧配置 item 内。'
)
assertMatch(
  /<el-select[\s\S]*selectedProcessDetailField\.key === 'predecessor'[\s\S]*multiple[\s\S]*@change="handlePredecessorChange"/,
  '前置工序必须使用多选编辑器并同步真实关系图边。'
)
assertMatch(
  /<el-select[\s\S]*selectedProcessDetailField\.key === 'successors'[\s\S]*multiple[\s\S]*@change="handleSuccessorsChange"/,
  '后续工序必须使用多选编辑器并同步真实关系图边。'
)
assertMatch(
  /<el-switch[\s\S]*selectedProcessDetailField\.key === 'keyFlag'[\s\S]*@change="handleKeyProcessToggle"/,
  '关键工序必须使用开关并复用候选草稿保存逻辑。'
)
assertMatch(
  /<el-switch[\s\S]*selectedProcessDetailField\.key === 'checkFlag'[\s\S]*@change="handleCheckFlagToggle"/,
  '质检确认必须使用开关并进入候选草稿保存逻辑。'
)
assertMatch(
  /const handleProductionQuantityFactorChange = \(value: number \| undefined\) => \{[\s\S]*selectedProcessAttributeDrafts\[routeProcessId\]\.productionQuantityFactor[\s\S]*markGraphDraftChanged\(\)/,
  '生产系数变更必须更新选中工序属性草稿并标记工作区变更。'
)
assertMatch(
  /const handlePredecessorChange = \(values: number\[\]\) => \{[\s\S]*replaceRouteProcessIncomingEdges\([\s\S]*markGraphDraftChanged\(\)/,
  '前置工序变更必须替换目标工序入边并标记工作区变更。'
)
assertMatch(
  /const handleSuccessorsChange = \(values: number\[\]\) => \{[\s\S]*replaceRouteProcessOutgoingEdges\([\s\S]*markGraphDraftChanged\(\)/,
  '后续工序变更必须替换目标工序出边并标记工作区变更。'
)
assertMatch(
  /const routeProcessCheckFlagBaselines = reactive<Record<number, boolean>>\(\{\}\)/,
  '质检确认必须有独立 baseline，不能只改界面状态。'
)
assertMatch(
  /const hasRouteProcessUpdateDraftChanges = \(\) =>[\s\S]*hasRouteProcessCheckFlagDraftChanges\(\)/,
  '质检确认变更必须触发顶部保存按钮的工作区变更判断。'
)
assertMatch(
  /const buildRouteProcessUpdatePayload = \(\): RouteFlowRouteProcessUpdateReqVO\[\] => \{[\s\S]*checkFlag: Boolean\(node\.checkFlag\)/,
  '质检确认最终必须进入 routeProcessUpdates 保存 payload。'
)

console.log('PASS: MES route flow node fields editable static contract')
