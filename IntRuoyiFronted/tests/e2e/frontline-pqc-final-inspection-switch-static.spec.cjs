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
  /pqcTaskOptions\?: FrontlinePqcTaskOptionVO\[\]/,
  'PQC process API type must expose FIRST/PATROL task options for switching.'
)
assert.match(
  viewSource,
  /type InspectionType = 'FIRST' \| 'PATROL'/,
  'One-line PQC page must only expose FIRST and PATROL inspection types.'
)
assert.doesNotMatch(
  viewSource,
  /@click="selectPqcInspectionType\('FINAL'\)"/,
  'One-line PQC page must not render a FINAL inspection button.'
)
assert.doesNotMatch(
  viewSource,
  /isFinalInspectionSelectable|finalInspectionApplicable === true/,
  'One-line PQC page must not gate first/patrol submission on final-inspection applicability.'
)
assert.match(
  viewSource,
  /findPqcTaskOption[\s\S]*inspectionType[\s\S]*applyPqcTaskOptionToSelectedProcess/,
  'Selecting FIRST or PATROL must apply the matching task option snapshot.'
)

console.log('PASS: frontline PQC first/patrol flow excludes final inspection switch')
