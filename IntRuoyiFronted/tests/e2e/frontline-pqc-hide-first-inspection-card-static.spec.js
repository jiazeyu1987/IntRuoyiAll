const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const viewPath = path.join(
  root,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(viewPath, 'utf8').replace(/\r\n/g, '\n')

const typeTabsStart = source.indexOf('class="frontline-pqc-type-tabs"')
const typeTabsEnd = source.indexOf('class="frontline-pqc-form-area"', typeTabsStart)
assert.ok(typeTabsStart >= 0 && typeTabsEnd > typeTabsStart, 'PQC inspection type tab block must exist.')

const typeTabsBlock = source.slice(typeTabsStart, typeTabsEnd)

assert.match(
  typeTabsBlock,
  /v-for="tab in pqcInspectionTypeTabs"/,
  'PQC inspection type cards must be rendered only from formal task options on the current process.'
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
  /:class="\{ active: activePqcTaskOption\?\.inspectionRuleKey === tab\.ruleKey \}"/,
  'PQC inspection type active state must follow the selected formal task rule.'
)
assert.match(
  typeTabsBlock,
  /@click="selectPqcInspectionTaskOption\(tab\.value\)"/,
  'PQC inspection type card clicks must select the formal task snapshot from the current process.'
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
  /const pqcInspectionTypeTabs = computed<\{ ruleKey: FrontlinePqcInspectionRuleKey; type: InspectionType; value: number; label: string \}\[\]>\(\(\) => \{[\s\S]*getUniquePqcTaskOptionsByRule\(\s*getPqcTaskOptionsForInspectionItem\(process, activePqcTabKey\.value\)[\s\S]*ruleKey: option\.inspectionRuleKey/,
  'PQC visible type cards must be deduplicated from the selected method formal pqcTaskOptions by rule key.'
)
assert.doesNotMatch(
  source,
  /const hasPqcTaskOptionForType = /,
  'PQC visible type cards must not be driven by a fixed card plus disabled-state helper.'
)

console.log('PASS: frontline PQC hides first inspection card when no FIRST task exists')
