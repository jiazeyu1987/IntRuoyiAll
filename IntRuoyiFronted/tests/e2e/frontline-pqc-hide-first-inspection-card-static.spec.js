const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const viewPath = path.join(
  root,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(viewPath, 'utf8').replace(/\r\n/g, '\n')

const ruleSelectorMarker = source.indexOf('data-pqc-inspection-rule-selector')
const typeTabsStart = source.lastIndexOf('<section', ruleSelectorMarker)
const typeTabsEnd = source.indexOf('<footer class="frontline-pqc-submit-bar">', ruleSelectorMarker)
assert.ok(
  ruleSelectorMarker >= 0 && typeTabsStart >= 0 && typeTabsEnd > ruleSelectorMarker,
  'PQC inspection type rule panel must exist.'
)

const typeTabsBlock = source.slice(typeTabsStart, typeTabsEnd)

assert.match(
  typeTabsBlock,
  /v-for="tab in pqcInspectionTypeTabs"/,
  'PQC inspection type cards must be rendered only from formal task options in the current order.'
)
assert.match(
  typeTabsBlock,
  /:key="tab\.ruleKey"/,
  'PQC inspection type cards must use the formal inspection rule as their stable key.'
)
assert.match(
  typeTabsBlock,
  /:data-pqc-inspection-rule-tab="tab\.ruleKey"/,
  'PQC inspection type cards must expose a stable per-rule DOM anchor.'
)
assert.match(
  typeTabsBlock,
  /:data-pqc-inspection-type-tab="tab\.type"/,
  'PQC inspection type cards must expose a stable per-type DOM anchor.'
)
assert.match(
  typeTabsBlock,
  /:class="\{ active: selectedPqcInspectionRuleKey === tab\.ruleKey \}"/,
  'PQC inspection type active state must follow the independent selected formal task rule.'
)
assert.match(
  typeTabsBlock,
  /@click="selectPqcInspectionRule\(tab\.ruleKey\)"/,
  'PQC inspection type card clicks must select the formal rule before resolving a process task snapshot.'
)
assert.match(
  typeTabsBlock,
  /\{\{\s*tab\.label\s*\}\}/,
  'PQC inspection type cards must display the label resolved from the formal task type.'
)
assert.doesNotMatch(
  typeTabsBlock,
  /:disabled="!hasPqcTaskOptionForType\('FIRST'\)"/,
  'A process without FIRST must not leave a disabled first-inspection card visible.'
)
assert.doesNotMatch(
  typeTabsBlock,
  /:disabled="!hasPqcTaskOptionForType\('PATROL'\)"/,
  'A process without PATROL must not leave a disabled patrol-inspection card visible.'
)

assert.match(
  source,
  /const PQC_INSPECTION_TYPE_LABELS: Record<InspectionType, string> = \{[\s\S]*FIRST:\s*'首检'[\s\S]*PATROL:\s*'巡检'[\s\S]*\}/,
  'PQC type labels must be centralized by formal inspection type.'
)
assert.match(
  source,
  /const pqcInspectionTypeTabs = computed<\{[\s\S]*?ruleKey: FrontlinePqcInspectionRuleKey[\s\S]*?type: InspectionType[\s\S]*?label: string[\s\S]*?\}\[\]\>\(\(\) =>[\s\S]*PQC_INSPECTION_RULE_ORDER[\s\S]*allSwitchablePqcProcessOptions\.value[\s\S]*hasExecutablePqcTaskForRule\(process, ruleKey\)/,
  'PQC visible type cards must be deduplicated from the current order formal pqcTaskOptions by rule key.'
)
assert.doesNotMatch(
  typeTabsBlock,
  /tab\.value|data-pqc-task-option|selectPqcInspectionTaskOption/,
  'The independent inspection type panel must not expose current-process task ids as visible button identity.'
)
assert.doesNotMatch(
  source,
  /const hasPqcTaskOptionForType = /,
  'PQC visible type cards must not be driven by a fixed card plus disabled-state helper.'
)

console.log('PASS: frontline PQC hides first inspection card when no FIRST task exists')
