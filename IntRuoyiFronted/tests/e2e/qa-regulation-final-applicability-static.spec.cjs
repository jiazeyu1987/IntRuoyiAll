const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const qaPagePath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const qaApiPath = path.join(workspaceRoot, 'IntRuoyiFronted/src/api/mes/qc/template/index.ts')

const qaSource = fs.readFileSync(qaPagePath, 'utf8')
const apiSource = fs.readFileSync(qaApiPath, 'utf8')

assert.match(
  apiSource,
  /finalInspectionApplicable:\s*boolean/,
  'QA regulation API type must require explicit final-inspection applicability.'
)
assert.match(
  apiSource,
  /finalInspectionNotApplicableReason\?:\s*string/,
  'QA regulation API type must expose final-inspection not-applicable evidence.'
)
assert.match(
  qaSource,
  /key:\s*'notApplicableReason'[\s\S]*label:\s*'不适用依据'/,
  'QA regulation rules table must include a visible not-applicable evidence column.'
)
assert.match(
  qaSource,
  /row\.key === 'FINAL' && !row\.required[\s\S]*v-model="row\.notApplicableReason"/,
  'The final-inspection row must require an editable not-applicable reason when disabled.'
)
assert.match(
  qaSource,
  /const finalInspectionApplicable\s*=\s*Boolean\(finalRule\?\.required\)/,
  'Save payload must derive finalInspectionApplicable from the explicit FINAL rule switch.'
)
assert.match(
  qaSource,
  /finalInspectionNotApplicableReason\s*=[\s\S]*finalRule\?\.notApplicableReason\?\.trim\(\)/,
  'Save payload must submit the trimmed final-inspection not-applicable reason.'
)
assert.match(
  qaSource,
  /if\s*\(!finalInspectionApplicable && !finalInspectionNotApplicableReason\)[\s\S]*末检不适用时必须填写正式依据/,
  'Frontend must block save/publish when final inspection is disabled without evidence.'
)
assert.match(
  qaSource,
  /if\s*\(!rule\)\s*{[\s\S]*return \[\][\s\S]*}/,
  'Disabled inspection types must not be serialized as implicit QA regulation items.'
)
assert.doesNotMatch(
  qaSource,
  /finalInspectionApplicable:\s*false\s*,\s*finalInspectionNotApplicableReason:\s*undefined/,
  'Frontend must not hardcode a default final-inspection bypass.'
)

console.log('PASS qa-regulation-final-applicability-static')
