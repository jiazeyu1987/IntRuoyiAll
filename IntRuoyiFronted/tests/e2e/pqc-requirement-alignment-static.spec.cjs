const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const apiPath = path.join(root, 'src/api/mes/pro/feedback/index.ts')
const panelSource = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')
const apiSource = fs.readFileSync(apiPath, 'utf8').replace(/\r\n/g, '\n')

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.match(
  apiSource,
  /export interface FrontlinePqcTaskOptionVO \{[\s\S]*pqcTaskId: number[\s\S]*qaItemCode\?: string \| null[\s\S]*inspectionType: FrontlinePqcInspectionType[\s\S]*plannedInspectionQuantity: number[\s\S]*inspectionItems: FrontlinePqcInspectionItemVO\[\]/,
  'PQC process response must expose selectable FIRST/PATROL_AM/PATROL_PM/FINAL task snapshots.'
)
assert.match(
  apiSource,
  /pqcTaskOptions: FrontlinePqcTaskOptionVO\[\]/,
  'PQC process response must keep task options on the process card without duplicating process cards.'
)

const tabLabelHelperBlock = blockBetween(
  panelSource,
  'const formatPqcInspectionItemTabLabel',
  'const formatPqcStandardSummary'
)
assert.match(
  tabLabelHelperBlock,
  /item\.itemName\s*\|\|\s*'未配置检验项目名称'/,
  'PQC inspection item tab title must display the formal itemName.'
)
assert.doesNotMatch(
  tabLabelHelperBlock,
  /formatPqcMethodSummary|item\.key|itemCode/,
  'PQC inspection item tab title must not display method summary or internal item identity.'
)

const processItemMappingBlock = blockBetween(
  panelSource,
  'const pqcInspectionItems = computed<PqcInspectionItem[]>',
  'const pqcTaskInspectionItems = computed<PqcInspectionItem[]>'
)
assert.match(
  processItemMappingBlock,
  /deviceState\.selectedProcess\.inspectionItems\.map\(mapPqcInspectionItem\)/,
  'PQC method tabs must come from the selected process inspection item list.'
)

const taskItemMappingBlock = blockBetween(
  panelSource,
  'const pqcTaskInspectionItems = computed<PqcInspectionItem[]>',
  'const pqcTaskInspectionItemMap'
)
assert.match(
  taskItemMappingBlock,
  /activePqcTaskOption\.value\?\.inspectionItems \|\| \[\]/,
  'PQC submit item scope must come from the selected FIRST/PATROL task option.'
)

const typeTabsBlock = blockBetween(
  panelSource,
  'data-pqc-inspection-rule-selector',
  '<footer class="frontline-pqc-submit-bar">'
)
assert.match(
  typeTabsBlock,
  /v-for="tab in pqcInspectionTypeTabs"/,
  'PQC type tabs must be rendered from available formal task rules.'
)
assert.match(
  typeTabsBlock,
  /:key="tab\.ruleKey"[\s\S]*:data-pqc-inspection-rule-tab="tab\.ruleKey"[\s\S]*@click="selectPqcInspectionRule\(tab\.ruleKey\)"/,
  'PQC task tabs must use rule identity and select the independent formal rule.'
)
assert.doesNotMatch(
  typeTabsBlock,
  /:disabled="!hasPqcTaskOptionForType/,
  'PQC type tabs must not add a second disabled-state restriction for unavailable formal tasks.'
)

const selectTaskBlock = blockBetween(
  panelSource,
  'const selectPqcInspectionRule = async (ruleKey: FrontlinePqcInspectionRuleKey) => {',
  'const updatePqcQuantity = (field: PqcQuantityField, event: Event) => {'
)
assert.match(
  selectTaskBlock,
  /selectedPqcInspectionRuleKey\.value = ruleKey[\s\S]*(?:applyPqcTaskOptionToSelectedProcess\(currentRuleOption\)|handleSelectProcess\(targetProcess\))/,
  'Selecting FIRST/PATROL_AM/PATROL_PM/FINAL must apply the matching current-process task or jump to an eligible process.'
)
assert.doesNotMatch(
  selectTaskBlock,
  /PQC检验类型来自任务快照，不能在前端切换/,
  'Frontend must no longer hard-block switching between available PQC task snapshots.'
)

assert.match(
  panelSource,
  /const pqcInspectionTypeTabs = computed<\{[\s\S]*?ruleKey: FrontlinePqcInspectionRuleKey[\s\S]*PQC_INSPECTION_RULE_ORDER[\s\S]*allSwitchablePqcProcessOptions\.value[\s\S]*hasExecutablePqcTaskForRule\(process, ruleKey\)[\s\S]*\.map\(\(ruleKey\) => \(\{[\s\S]*ruleKey,[\s\S]*label: PQC_INSPECTION_RULE_LABELS\[ruleKey\]/,
  'PQC task selection grid must deduplicate item-level tasks by formal rule key at current-order rule level.'
)

console.log('PASS: PQC requirement alignment static contract')
