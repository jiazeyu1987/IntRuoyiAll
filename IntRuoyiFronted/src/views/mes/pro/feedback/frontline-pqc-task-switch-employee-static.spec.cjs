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
  ['selectPqcInspectionTab', 'const getPqcSelectedEquipmentLabel', 'option'],
  ['selectPqcInspectionRule', 'const updatePqcQuantity', 'currentRuleOption']
]

for (const [functionName, endMarker, optionName] of taskSelectionFunctions) {
  const functionSource = sliceBetween(
    panelSource,
    `const ${functionName} = async`,
    endMarker,
    `${functionName} task selection`
  )
  assert.match(
    functionSource,
    new RegExp(`applyPqcTaskOptionToSelectedProcess\\(${optionName}\\)`),
    `${functionName} must update the selected PQC task option`
  )
  assert.match(
    functionSource,
    /await switchPqcCurrentLoginEmployeeForActiveTask\(\)/,
    `${functionName} must rebind the current PQC employee after task switching`
  )
}

const ruleSelection = sliceBetween(panelSource, 'const selectPqcInspectionRule = async',
  'const updatePqcQuantity', 'PQC inspection rule selection')
assert.match(panelSource, /@click="selectPqcInspectionRule\(tab\.ruleKey\)"/)
assert.match(panelSource, /@click="selectPqcInspectionTab\(item\.key\)"/)
assert.match(ruleSelection, /getPqcTaskOptionForRule\(currentProcess, ruleKey, activePqcTabKey\.value\)/)
assert.match(ruleSelection, /activePqcTaskOptionId\.value !== currentRuleOption\.pqcTaskId \|\| !deviceState\.selectedEmployee/)
assert.match(ruleSelection, /applyPqcTaskOptionToSelectedProcess\(currentRuleOption\)[\s\S]*await switchPqcCurrentLoginEmployeeForActiveTask\(\)/)
assert.match(ruleSelection, /findFirstPqcProcessForInspectionRule\([\s\S]*await handleSelectProcess\(targetProcess\)/)
assert.match(ruleSelection, /if \(!targetProcess\)[\s\S]*clearPqcTaskOptionDraft\(\)[\s\S]*showFrontlineError/)
const processSelection = sliceBetween(panelSource, 'const handleSelectProcess = async',
  'const handlePickerProcessClick', 'formal process selection')
assert.match(processSelection, /await selectFrontlinePqcProcess\(deviceState, selectedProcess\)/)
assert.match(processSelection, /applyPqcTaskSnapshotToDraft\(selectedProcess\)/)
assert.match(processSelection, /findInitialEmployee\(\)[\s\S]*await handleSelectEmployee\(initialEmployee\)/)
const employeeSelection = sliceBetween(panelSource, 'const handleSelectEmployee = async',
  'watch(currentLoginUserId', 'formal employee selection')
assert.match(employeeSelection, /!isCurrentLoginEmployee\(employee\)/)
assert.match(employeeSelection, /await switchFrontlinePqcActualEmployee\(deviceState, activePqcTaskOption\.value, employee\.userId\)/)

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
