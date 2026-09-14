const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const panelSource = fs.readFileSync(
  path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker`)
  return source.slice(start, end)
}

const taskSwitchHelper = sliceBetween(
  panelSource,
  'const switchPqcCurrentLoginEmployeeForActiveTask',
  'const handleValidate',
  'PQC task employee switch helper'
)

assert.match(
  taskSwitchHelper,
  /findCurrentLoginEmployee\(\)/,
  'PQC task switching must resolve the current logged-in PQC employee'
)
assert.match(
  taskSwitchHelper,
  /activePqcTaskOption\.value/,
  'PQC task switching must use the currently selected task option'
)
assert.match(
  taskSwitchHelper,
  /await handleSelectEmployee\(employee\)/,
  'PQC task switching must complete the employee switch before submit'
)
assert.match(
  taskSwitchHelper,
  /当前登录账号未返回PQC人员候选/,
  'Missing PQC employee candidates must remain an explicit blocking error'
)

const taskSelectionFunctions = [
  ['selectPqcInspectionTab', 'const getPqcSelectedEquipmentLabel'],
  ['selectPqcInspectionType', 'const selectPqcInspectionTaskOption'],
  ['selectPqcInspectionTaskOption', 'const updatePqcQuantity']
]

for (const [functionName, endMarker] of taskSelectionFunctions) {
  const functionSource = sliceBetween(
    panelSource,
    `const ${functionName} = async`,
    endMarker,
    `${functionName} task selection`
  )
  assert.match(
    functionSource,
    /applyPqcTaskOptionToSelectedProcess\(option\)/,
    `${functionName} must update the selected PQC task option`
  )
  assert.match(
    functionSource,
    /await switchPqcCurrentLoginEmployeeForActiveTask\(\)/,
    `${functionName} must rebind the current PQC employee after task switching`
  )
}

const pqcSubmitButton = sliceBetween(
  panelSource,
  '<button\n            class="frontline-pqc-submit-button"',
  '</button>',
  'PQC submit button'
)
assert.match(
  pqcSubmitButton,
  /deviceState\.loadingTemplate|isPqcSubmitBlocked/,
  'PQC submit must stay disabled while the new employee context is switching'
)

console.log('frontline-pqc-task-switch-employee-static: PASS')
