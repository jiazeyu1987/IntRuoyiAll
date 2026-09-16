const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (filePath) => fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n')

const viewSource = read(path.join(
  frontendRoot,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
))
const feedbackApiSource = read(path.join(frontendRoot, 'src/api/mes/pro/feedback/index.ts'))

assert.match(
  feedbackApiSource,
  /pqcTaskOptions: FrontlinePqcTaskOptionVO\[\]/,
  'PQC process API type must expose formal task options for switching.'
)
assert.match(
  viewSource,
  /type InspectionType = 'FIRST' \| 'PATROL' \| 'FINAL'/,
  'One-line PQC page must support configured FIRST/PATROL/FINAL inspection types.'
)
assert.match(
  viewSource,
  /const PQC_INSPECTION_RULE_LABELS: Record<FrontlinePqcInspectionRuleKey, string> = \{[\s\S]*FINAL:\s*(?:'末检'|PQC_INSPECTION_TYPE_LABELS\.FINAL)/,
  'PQC page must render FINAL when a formal final-inspection task exists.'
)
assert.doesNotMatch(
  viewSource,
  /@click="selectPqcInspectionType\('FINAL'\)"/,
  'One-line PQC page must not hard-code a FINAL button outside formal task options.'
)
assert.doesNotMatch(
  viewSource,
  /isFinalInspectionSelectable|finalInspectionApplicable === true/,
  'One-line PQC page must not gate task visibility on a frontend final-inspection fallback.'
)
assert.match(
  viewSource,
  /@click="selectPqcInspectionTaskOption\(tab\.value\)"[\s\S]*const selectPqcInspectionTaskOption = async \(pqcTaskId: number\)[\s\S]*applyPqcTaskOptionToSelectedProcess\(option\)/,
  'Selecting a configured inspection button must apply the matching task option snapshot.'
)

console.log('PASS: frontline PQC final inspection displays only from formal task options')
