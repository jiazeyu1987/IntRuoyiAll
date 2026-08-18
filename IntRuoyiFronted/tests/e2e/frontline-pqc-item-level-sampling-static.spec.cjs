const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const apiSource = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/pro/feedback/index.ts'),
  'utf8'
)
const pageSource = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

assert.match(
  apiSource,
  /export interface FrontlinePqcTaskOptionVO \{[\s\S]*qaItemCode\?: string \| null/,
  'PQC task option must expose the QA inspection item identity.'
)
assert.match(
  pageSource,
  /getPqcTaskOptionsForInspectionItem\(process, itemKey\)[\s\S]*applyPqcTaskOptionToSelectedProcess\(option\)[\s\S]*selectedPqcInspectionKey\.value = itemKey/,
  'Switching method tabs must keep FIRST/PATROL/FINAL inside the selected QA inspection item.'
)
assert.match(
  pageSource,
  /const pqcInspectionItems = computed<PqcInspectionItem\[\]>[\s\S]*deviceState\.selectedProcess\.inspectionItems\.map\(mapPqcInspectionItem\)/,
  'The red-box method tabs must display the process-level inspection methods.'
)
assert.match(
  pageSource,
  /formatPqcInspectionItemTabLabel\(item\)/,
  'Method tab buttons must display the inspection item name through the formal tab label helper.'
)
assert.doesNotMatch(
  pageSource,
  /gridTemplateColumns:\s*`repeat\(\$\{pqcVisibleRounds\.length\}/,
  'Item-level task buttons must not be forced into one fixed row.'
)
assert.match(
  pageSource,
  /\.frontline-pqc-fill-panel\s*\{[\s\S]*grid-template-rows:\s*auto auto minmax\(min-content, 1fr\)[\s\S]*overflow-y:\s*auto/,
  'The PQC panel must grow or scroll when item-level task buttons wrap.'
)
assert.match(
  pageSource,
  /\.frontline-pqc-round-tabs\s*\{[\s\S]*grid-template-columns:\s*repeat\(auto-fit, minmax\(136px, 1fr\)\)[\s\S]*white-space:\s*normal[\s\S]*overflow-wrap:\s*anywhere/,
  'Item-level task buttons must use a readable wrapping grid.'
)

console.log('frontline-pqc-item-level-sampling-static: PASS')
