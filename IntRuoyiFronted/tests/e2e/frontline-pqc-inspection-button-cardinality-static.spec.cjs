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

const taskButtonBlock = blockBetween(
  panelSource,
  '<div class="frontline-pqc-type-tabs">',
  '<div class="frontline-pqc-form-area">'
)

assert.match(
  taskButtonBlock,
  /v-for="tab in pqcInspectionTypeTabs"/,
  'PQC task buttons must be generated from the formal visible task list.'
)
assert.match(
  taskButtonBlock,
  /:key="tab\.ruleKey"/,
  'PQC task buttons must use FIRST/PATROL_AM/PATROL_PM/FINAL rule identity as the stable key.'
)
assert.match(
  taskButtonBlock,
  /:data-pqc-inspection-rule-tab="tab\.ruleKey"/,
  'PQC task buttons must expose the formal inspection rule key for UI verification.'
)
assert.match(
  taskButtonBlock,
  /activePqcTaskOption\?\.inspectionRuleKey === tab\.ruleKey/,
  'PQC task button active state must be per formal rule, not per duplicated item task id.'
)
assert.match(
  taskButtonBlock,
  /@click="selectPqcInspectionTaskOption\(tab\.value\)"/,
  'PQC task button clicks must apply the selected formal task snapshot.'
)
assert.doesNotMatch(
  taskButtonBlock,
  /class="frontline-pqc-round-tabs"/,
  'PQC page must not render a second repeated round button grid.'
)

assert.match(
  panelSource,
  /const PQC_INSPECTION_RULE_LABELS: Record<FrontlinePqcInspectionRuleKey, string> = \{[\s\S]*FIRST:\s*(?:'首检'|PQC_INSPECTION_TYPE_LABELS\.FIRST)[\s\S]*PATROL_AM:\s*'上午巡检'[\s\S]*PATROL_PM:\s*'下午巡检'[\s\S]*FINAL:\s*(?:'末检'|PQC_INSPECTION_TYPE_LABELS\.FINAL)/,
  'PQC labels must be centralized by formal inspection rule key.'
)
assert.match(
  panelSource,
  /const getUniquePqcTaskOptionsByRule = \([\s\S]*PQC_INSPECTION_RULE_ORDER[\s\S]*option\.inspectionRuleKey === ruleKey[\s\S]*return orderedOptions/,
  'PQC visible task options must be deduplicated by FIRST/PATROL_AM/PATROL_PM/FINAL.'
)
assert.match(
  panelSource,
  /const pqcInspectionTypeTabs = computed<\{ ruleKey: FrontlinePqcInspectionRuleKey; type: InspectionType; value: number; label: string \}\[\]>\(\(\) => \{[\s\S]*getUniquePqcTaskOptionsByRule\(\s*getPqcTaskOptionsForInspectionItem\(process, activePqcTabKey\.value\)[\s\S]*ruleKey: option\.inspectionRuleKey[\s\S]*value: option\.pqcTaskId/,
  'PQC visible buttons must be one per configured rule and keep the selected task id for submission.'
)
assert.doesNotMatch(
  panelSource,
  /const pqcVisibleRounds = computed/,
  'PQC round buttons must not be generated directly from item-level task options.'
)

console.log('frontline-pqc-inspection-button-cardinality-static: PASS')
