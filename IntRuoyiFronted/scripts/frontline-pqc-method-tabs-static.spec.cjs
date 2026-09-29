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
  'const pqcTaskInspectionItemMap = computed'
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
assert.match(typeTabsBlock, /PQC_INSPECTION_RULE_ORDER[\s\S]*hasExecutablePqcTaskForRule\(process, ruleKey\)/, 'Rule tabs must represent executable formal task rules.')
assert.match(panelSource, /data-pqc-inspection-rule-selector[\s\S]*@click="selectPqcInspectionRule\(tab.ruleKey\)"/, 'Visible rule controls must invoke the rule handler.')

const selectMethodBlock = blockBetween(
  panelSource,
  'const selectPqcInspectionTab = async (itemKey: PqcInspectionItemKey) => {',
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

assert.match(selectMethodBlock, /applyPqcTaskOptionToSelectedProcess\(option\)[\s\S]*await switchPqcCurrentLoginEmployeeForActiveTask\(\)/, 'Method switching must rebind the employee to the selected formal task.')
const selectRuleBlock = blockBetween(panelSource, 'const selectPqcInspectionRule = async', 'const updatePqcQuantity')
assert.match(selectRuleBlock, /persistCurrentPqcTaskDraft\(\)[\s\S]*selectedPqcInspectionRuleKey\.value = ruleKey/, 'Changing rules must preserve the outgoing draft.')
assert.match(selectRuleBlock, /getPqcTaskOptionForRule\(currentProcess, ruleKey, activePqcTabKey\.value\)/, 'Rule selection must prefer a task for the current method.')
assert.match(selectRuleBlock, /applyPqcTaskOptionToSelectedProcess\(currentRuleOption\)[\s\S]*await switchPqcCurrentLoginEmployeeForActiveTask\(\)/, 'Same-process rule switching must rebind the employee.')
assert.match(selectRuleBlock, /findFirstPqcProcessForInspectionRule\(\s*allSwitchablePqcProcessOptions\.value,\s*ruleKey\s*\)[\s\S]*if \(!targetProcess\)[\s\S]*showFrontlineError[\s\S]*await handleSelectProcess\(targetProcess\)/, 'Cross-process rules must resolve an executable process and explicitly reject missing tasks.')

const selectProcessBlock = blockBetween(panelSource, 'const handleSelectProcess = async', 'const handlePickerProcessClick = async')
assert.match(selectProcessBlock, /clearPqcExecutionSelection\(\)[\s\S]*await selectFrontlinePqcProcess\(deviceState, selectedProcess\)/, 'Cross-process switching must clear old execution and select the formal process.')
assert.match(selectProcessBlock, /applyPqcTaskSnapshotToDraft\(selectedProcess\)[\s\S]*const initialEmployee = findInitialEmployee\(\)[\s\S]*await handleSelectEmployee\(initialEmployee\)/, 'Cross-process switching must bind the new task before selecting its employee.')

const submitItemsBlock = blockBetween(
  panelSource,
  'const buildPqcItemResultsPayload = (',
  'const getPqcCurrentChoiceValues = (itemKey: PqcInspectionItemKey) =>'
)
assert.match(
  submitItemsBlock,
  /taskOption: PqcTaskOptionSnapshot \| undefined = activePqcTaskOption\.value[\s\S]*\(taskOption\?\.inspectionItems \|\| \[\]\)\.map\(mapPqcInspectionItem\)\.map/,
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
  /\.frontline-pqc-type-tabs\s*\{[\s\S]*grid-template-columns:\s*repeat\(auto-fit, minmax\(136px, 1fr\)\)[\s\S]*overflow-wrap:\s*anywhere/,
  'PQC task buttons must keep a readable wrapping layout.'
)

console.log('frontline-pqc-method-tabs-static: PASS')
