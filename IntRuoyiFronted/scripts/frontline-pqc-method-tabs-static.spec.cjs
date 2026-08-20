const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const apiSource = read('src/api/mes/pro/feedback/index.ts')

assert.match(
  apiSource,
  /export interface FrontlinePqcTaskOptionVO \{[\s\S]*qaItemCode\?: string \| null/,
  'PQC task option must expose the formal QA inspection item identity.'
)

const processItemBlock = blockBetween(
  panelSource,
  'const pqcInspectionItems = computed<PqcInspectionItem[]>',
  'const pqcTaskInspectionItems = computed<PqcInspectionItem[]>'
)
assert.match(
  processItemBlock,
  /deviceState\.selectedProcess\.inspectionItems\.map\(mapPqcInspectionItem\)/,
  'PQC method tabs must be generated from the selected process inspection item list.'
)
assert.doesNotMatch(
  processItemBlock,
  /activePqcTaskOption\.value\?\.inspectionItems/,
  'PQC method tabs must not be limited to the currently selected inspection task.'
)

const taskItemBlock = blockBetween(
  panelSource,
  'const pqcTaskInspectionItems = computed<PqcInspectionItem[]>',
  'const pqcInspectionItemMap = computed'
)
assert.match(
  taskItemBlock,
  /activePqcTaskOption\.value\?\.inspectionItems/,
  'PQC submission item scope must still come from the active formal task snapshot.'
)

const typeTabsBlock = blockBetween(
  panelSource,
  'const pqcInspectionTypeTabs = computed',
  'const activePqcTaskOption = computed'
)
assert.match(
  typeTabsBlock,
  /getPqcTaskOptionsForInspectionItem\(process, activePqcTabKey\.value\)/,
  'PQC inspection type tabs must be filtered by the selected inspection method.'
)

const visibleRoundsBlock = blockBetween(
  panelSource,
  'const pqcVisibleRounds = computed',
  'const templateModeMismatch = computed'
)
assert.match(
  visibleRoundsBlock,
  /pqcTaskOptionIncludesItem\(option, activePqcTabKey\.value\)/,
  'PQC round tabs must only show tasks belonging to the selected inspection method.'
)
assert.doesNotMatch(
  panelSource,
  /gridTemplateColumns:\s*`repeat\(\$\{pqcVisibleRounds\.length\}/,
  'PQC round tabs must not force all tasks into one fixed row.'
)

const selectMethodBlock = blockBetween(
  panelSource,
  'const selectPqcInspectionTab = (itemKey: PqcInspectionItemKey) => {',
  'const getPqcSelectedEquipmentLabel = (item: PqcInspectionItem) => {'
)
assert.match(
  selectMethodBlock,
  /getPqcTaskOptionsForInspectionItem\(process, itemKey\)/,
  'Selecting a method tab must choose a task from that method only.'
)
assert.match(
  selectMethodBlock,
  /selectedPqcInspectionKey\.value = itemKey[\s\S]*applyPqcTaskOptionToSelectedProcess\(option\)[\s\S]*selectedPqcInspectionKey\.value = itemKey/,
  'Selecting a method tab must preserve the selected method after applying the formal task.'
)
assert.match(
  selectMethodBlock,
  /activePqcTaskOptionId\.value !== option\.pqcTaskId/,
  'Selecting a method tab must compare against the raw selected task id so the draft quantity is refreshed.'
)

const selectTypeBlock = blockBetween(
  panelSource,
  'const selectPqcInspectionType = (inspectionType: InspectionType) => {',
  'const selectPqcInspectionTaskOption = (pqcTaskId: number) => {'
)
assert.match(
  selectTypeBlock,
  /findPqcTaskOption\(process, inspectionType, itemKey\)/,
  'Selecting FIRST/PATROL/FINAL must stay inside the selected method context.'
)

const submitItemsBlock = blockBetween(
  panelSource,
  'const buildPqcItemResultsPayload = (): FrontlinePqcItemResultSubmitReqVO[] =>',
  'const getPqcCurrentChoiceValues = (itemKey: PqcInspectionItemKey) =>'
)
assert.match(
  submitItemsBlock,
  /pqcTaskInspectionItems\.value\.map/,
  'PQC submit payload must still use the active task expected item list.'
)
assert.doesNotMatch(
  submitItemsBlock,
  /pqcInspectionItems\.value\.map/,
  'PQC submit payload must not submit every method tab when the active task is item-scoped.'
)

assert.match(
  panelSource,
  /\.pqc-item-tabs\s*\{[\s\S]*grid-template-columns:\s*repeat\(auto-fit, minmax\(104px, 1fr\)\)/,
  'PQC method tabs must wrap as the number of inspection methods grows.'
)
assert.match(
  panelSource,
  /\.frontline-pqc-round-tabs\s*\{[\s\S]*grid-template-columns:\s*repeat\(auto-fit, minmax\(136px, 1fr\)\)[\s\S]*overflow-wrap:\s*anywhere/,
  'PQC task buttons must keep a readable wrapping layout.'
)

console.log('frontline-pqc-method-tabs-static: PASS')
