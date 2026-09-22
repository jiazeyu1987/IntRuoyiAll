const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelSource = fs
  .readFileSync(
    path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const sectionAround = (source, markerToken, endToken) => {
  const marker = source.indexOf(markerToken)
  assert.ok(marker >= 0, `missing marker token: ${markerToken}`)
  const start = source.lastIndexOf('<section', marker)
  assert.ok(start >= 0, `missing section before marker token: ${markerToken}`)
  const end = source.indexOf(endToken, marker)
  assert.ok(end > marker, `missing end token after ${markerToken}: ${endToken}`)
  return source.slice(start, end)
}

const fillPanelBlock = blockBetween(
  panelSource,
  '<section class="frontline-work-panel frontline-pqc-fill-panel">',
  'data-pqc-inspection-rule-selector'
)

assert.doesNotMatch(
  fillPanelBlock,
  /pqcInspectionTypeTabs|data-pqc-inspection-rule-tab/,
  'The quantity form panel must no longer contain the inspection rule selector.'
)

const ruleSelectorBlock = sectionAround(
  panelSource,
  'data-pqc-inspection-rule-selector',
  '<footer class="frontline-pqc-submit-bar">'
)

assert.match(
  ruleSelectorBlock,
  /data-pqc-inspection-rule-selector/,
  'The inspection rule selector must live in its own bottom panel.'
)
assert.match(
  ruleSelectorBlock,
  /v-for="tab in pqcInspectionTypeTabs"/,
  'Inspection rule buttons must still be rendered from formal task options.'
)
assert.match(
  ruleSelectorBlock,
  /:key="tab\.ruleKey"/,
  'Inspection rule buttons must use FIRST/PATROL_AM/PATROL_PM/FINAL as stable keys.'
)
assert.match(
  ruleSelectorBlock,
  /:class="\{ active: selectedPqcInspectionRuleKey === tab\.ruleKey \}"/,
  'The active button must follow the independent selected inspection rule, not the current process default.'
)
assert.match(
  ruleSelectorBlock,
  /@click="selectPqcInspectionRule\(tab\.ruleKey\)"/,
  'Clicking a rule button must select a rule key before resolving the target process.'
)
assert.doesNotMatch(
  ruleSelectorBlock,
  /data-pqc-task-option|selectPqcInspectionTaskOption|tab\.pqcTaskId/,
  'The independent rule selector must not bind clicks or DOM identity to one current-process task id.'
)

assert.match(
  panelSource,
  /const selectedPqcInspectionRuleKey = ref<FrontlinePqcInspectionRuleKey>\(\)/,
  'The page must keep inspection rule selection in an independent state ref.'
)
assert.match(
  panelSource,
  /const hasExecutablePqcTaskForRule = \([\s\S]*process: FrontlinePqcProcessVO[\s\S]*ruleKey: FrontlinePqcInspectionRuleKey[\s\S]*option\.inspectionRuleKey === ruleKey/,
  'Process eligibility must be tested by formal inspectionRuleKey.'
)
assert.match(
  panelSource,
  /const findFirstPqcProcessForInspectionRule = \([\s\S]*hasExecutablePqcTaskForRule\(process, ruleKey\)/,
  'Selecting a rule must be able to jump to the first process supporting that rule.'
)
assert.match(
  panelSource,
  /const filteredPqcProcessOptions = computed\(\(\) => \{[\s\S]*selectedPqcInspectionRuleKey\.value[\s\S]*hasExecutablePqcTaskForRule\(process, ruleKey\)/,
  'PQC process picker and navigation must use the selected inspection rule as filter context.'
)
assert.match(
  panelSource,
  /isPqcMode\.value\s*\?\s*filteredPqcProcessOptions\.value\s*:\s*switchableProcessOptions\.value/,
  'The process picker must show only rule-eligible PQC processes while leaving production untouched.'
)
assert.match(
  panelSource,
  /const selectedPqcProcessIndex = computed\(\(\) => \{[\s\S]*filteredPqcProcessOptions\.value\.findIndex/,
  'PQC left/right navigation must resolve the current process inside the filtered process list.'
)
assert.match(
  panelSource,
  /const previousPqcProcess = computed\(\(\) => \{[\s\S]*filteredPqcProcessOptions\.value\[selectedIndex - 1\]/,
  'The previous PQC arrow must only target the previous process eligible for the selected rule.'
)
assert.match(
  panelSource,
  /const nextPqcProcess = computed\(\(\) => \{[\s\S]*filteredPqcProcessOptions\.value\[selectedIndex \+ 1\]/,
  'The next PQC arrow must only target the next process eligible for the selected rule.'
)
assert.match(
  panelSource,
  /const selectPqcInspectionRule = async \(ruleKey: FrontlinePqcInspectionRuleKey\) => \{[\s\S]*selectedPqcInspectionRuleKey\.value = ruleKey[\s\S]*findFirstPqcProcessForInspectionRule/,
  'Rule selection must set the independent rule first and then resolve the target process.'
)
assert.match(
  panelSource,
  /showFrontlineError\(`当前工序没有可执行的\$\{PQC_INSPECTION_RULE_LABELS\[selectedRuleKey\]\}任务。`\)[\s\S]*return/,
  'Bypassed process selection must reject processes that do not support the selected rule instead of changing the rule.'
)
assert.doesNotMatch(
  panelSource,
  /const nextRuleKey = findFirstPqcInspectionRuleKey\(\[process\]\)[\s\S]*selectedPqcInspectionRuleKey\.value = nextRuleKey/,
  'Process selection must not rebase the selected inspection rule to fit the target process.'
)
assert.match(
  panelSource,
  /const applyPqcTaskSnapshotToDraft = \([\s\S]*selectedPqcInspectionRuleKey\.value[\s\S]*getPqcTaskOptionForRule/,
  'Changing process must preserve the selected rule and bind the same-rule task in the new process.'
)
assert.doesNotMatch(
  panelSource,
  /PQC_INSPECTION_RULE_TYPES\[ruleKey\] === 'PATROL'[\s\S]*hasExecutablePqcTaskForRule/,
  'PATROL_AM and PATROL_PM must not be merged when filtering eligible processes.'
)

console.log('frontline-pqc-inspection-type-process-filter-static: PASS')
