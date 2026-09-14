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
  'PQC process response must expose selectable FIRST/PATROL task snapshots.'
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
  'const pqcInspectionItemMap'
)
assert.match(
  taskItemMappingBlock,
  /activePqcTaskOption\.value\?\.inspectionItems \|\| \[\]/,
  'PQC submit item scope must come from the selected FIRST/PATROL task option.'
)

const typeTabsBlock = blockBetween(
  panelSource,
  '<div class="frontline-pqc-type-tabs">',
  '<div class="frontline-pqc-round-tabs">'
)
assert.match(
  typeTabsBlock,
  /v-for="tab in pqcInspectionTypeTabs"/,
  'PQC type tabs must be rendered from available formal task types.'
)
assert.doesNotMatch(
  typeTabsBlock,
  /:disabled="!hasPqcTaskOptionForType/,
  'PQC type tabs must not add a second disabled-state restriction for unavailable formal tasks.'
)

const selectTypeBlock = blockBetween(
  panelSource,
  'const selectPqcInspectionType = (inspectionType: InspectionType) => {',
  'const updatePqcQuantity = (field: PqcQuantityField, event: Event) => {'
)
assert.match(
  selectTypeBlock,
  /applyPqcTaskOptionToSelectedProcess\(option\)/,
  'Selecting FIRST/PATROL must apply the matching PQC task snapshot to the current process.'
)
assert.doesNotMatch(
  selectTypeBlock,
  /PQC检验类型来自任务快照，不能在前端切换/,
  'Frontend must no longer hard-block switching between available FIRST/PATROL task snapshots.'
)

const roundTabsBlock = blockBetween(
  panelSource,
  '<div class="frontline-pqc-round-tabs"',
  '<div class="frontline-pqc-form-area">'
)
assert.match(
  roundTabsBlock,
  /@click="selectPqcInspectionTaskOption\(round\.value\)"/,
  'PQC round selection must apply the matching task snapshot instead of only mutating patrolRound.'
)

console.log('PASS: PQC requirement alignment static contract')
