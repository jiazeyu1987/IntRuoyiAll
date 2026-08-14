const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(
  frontendRoot,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const extractFunctionBlock = (name) => {
  const start = source.indexOf(`const ${name} = async`)
  assert.ok(start >= 0, `missing function: ${name}`)
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, `missing function body: ${name}`)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(openIndex + 1, index)
      }
    }
  }
  assert.fail(`unterminated function: ${name}`)
}

const employeeBlock = extractFunctionBlock('handleSelectEmployee')

assert.match(
  employeeBlock,
  /const shouldClosePickerImmediately = !isPqcMode\.value/,
  'employee selection must explicitly separate production immediate close from PQC validation flow.'
)
assert.match(
  employeeBlock,
  /const selectionRequestId = shouldClosePickerImmediately[\s\S]*\+\+productionEmployeeSelectionRequestId/,
  'production employee selection must keep the stale request guard token.'
)

const pqcGuardIndex = employeeBlock.indexOf(
  'if (isPqcMode.value && !isCurrentLoginEmployee(employee))'
)
const immediateCloseGuardIndex = employeeBlock.indexOf('if (shouldClosePickerImmediately)')
const immediateCloseIndex = employeeBlock.indexOf('closePicker()', immediateCloseGuardIndex)
const productionSwitchIndex = employeeBlock.indexOf(
  'await switchFrontlineActualEmployee(deviceState, employee.userId)'
)
assert.ok(pqcGuardIndex >= 0, 'PQC locked-employee validation must remain in employee selection.')
assert.ok(
  immediateCloseGuardIndex > pqcGuardIndex,
  'PQC illegal-employee validation must run before any immediate close branch.'
)
assert.ok(immediateCloseIndex >= 0, 'production employee selection must close picker immediately.')
assert.ok(productionSwitchIndex >= 0, 'production employee selection must await formal switch result.')
assert.ok(
  immediateCloseIndex < productionSwitchIndex,
  'production employee picker must close before waiting for switch-employee/template loading.'
)

assert.match(
  employeeBlock,
  /const result = isPqcMode\.value[\s\S]*await switchFrontlinePqcActualEmployee\(deviceState, employee\.userId\)[\s\S]*await switchFrontlineActualEmployee\(deviceState, employee\.userId\)/,
  'employee selection must still await formal PQC or production switch result.'
)
assert.match(
  employeeBlock,
  /if \(shouldClosePickerImmediately && selectionRequestId !== productionEmployeeSelectionRequestId\)/,
  'production employee selection must ignore stale switch results after rapid clicks.'
)
assert.match(
  employeeBlock,
  /if \(!shouldClosePickerImmediately\) \{[\s\S]*closePicker\(\)[\s\S]*\}/,
  'PQC employee selection must keep close-after-validation behavior.'
)
assert.doesNotMatch(
  employeeBlock,
  /catch\s*\(/,
  'employee selection must not swallow switch errors or fake successful switching.'
)

console.log('PASS: frontline production employee picker immediate close static contract')
