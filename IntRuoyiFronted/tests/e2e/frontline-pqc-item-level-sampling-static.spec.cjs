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
  /export interface FrontlinePqcTaskOptionVO \{[\s\S]*qaItemCode: string/,
  'PQC task option must expose the QA inspection item identity.'
)
assert.match(
  pageSource,
  /activePqcTaskOption\.value\?\.qaItemCode[\s\S]*option\.qaItemCode === activeQaItemCode/,
  'Switching FIRST/PATROL must keep the currently selected QA inspection item.'
)
assert.match(
  pageSource,
  /formatPqcTaskOptionLabel[\s\S]*inspectionItems\?\.\[0\]\?\.itemName/,
  'Item-level task buttons must display the inspection item name.'
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
