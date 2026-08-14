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

const processBlock = extractFunctionBlock('handleSelectProcess')

assert.match(
  processBlock,
  /const shouldClosePickerImmediately = true/,
  'process picker selection must close immediately for both production and PQC modes.'
)
assert.doesNotMatch(
  processBlock,
  /const shouldClosePickerImmediately = !isPqcMode\.value/,
  'PQC process picker must not wait for async task or employee context loading before closing.'
)

const requestTokenIndex = processBlock.indexOf('++processSelectionRequestId')
const closeGuardIndex = processBlock.indexOf('if (shouldClosePickerImmediately)')
const closeIndex = processBlock.indexOf('closePicker()', closeGuardIndex)
const pqcSelectIndex = processBlock.indexOf(
  'await selectFrontlinePqcProcess(deviceState, selectedProcess)'
)
const productionSelectIndex = processBlock.indexOf(
  'await selectFrontlineProcess(deviceState, selectedProcess)'
)
const employeeSelectIndex = processBlock.indexOf('await handleSelectEmployee(initialEmployee)')

assert.ok(requestTokenIndex >= 0, 'process selection must keep a stale request guard token.')
assert.ok(closeIndex >= 0, 'process selection must close the picker.')
assert.ok(pqcSelectIndex >= 0, 'PQC process selection must still call the formal PQC selector.')
assert.ok(productionSelectIndex >= 0, 'production process selection must still call the formal selector.')
assert.ok(employeeSelectIndex >= 0, 'process selection must still enter the formal employee/template flow.')
assert.ok(
  closeIndex < pqcSelectIndex,
  'PQC process picker must close before awaiting PQC task or employee candidate loading.'
)
assert.ok(
  closeIndex < productionSelectIndex,
  'production process picker must continue closing before runtime config loading.'
)
assert.ok(
  closeIndex < employeeSelectIndex,
  'process picker must close before default employee or template switching.'
)

assert.match(
  processBlock,
  /const selectedProcess = isPqcMode\.value[\s\S]*withPqcTaskOption\(process, getDefaultPqcTaskOption\(process\)\)/,
  'PQC process selection must keep the formal default PQC task snapshot selection.'
)
assert.match(
  processBlock,
  /if \(selectionRequestId !== processSelectionRequestId\) \{[\s\S]*return[\s\S]*\}/,
  'rapid process clicks must ignore stale async selection results after the picker closes.'
)
assert.match(
  processBlock,
  /if \(isPqcMode\.value\) \{[\s\S]*applyPqcTaskSnapshotToDraft\(selectedProcess\)[\s\S]*\}/,
  'PQC process selection must still apply the formal task and regulation snapshot after async selection.'
)
assert.doesNotMatch(
  processBlock,
  /catch\s*\(/,
  'process selection must not swallow formal selector, employee, or template switch errors.'
)

console.log('PASS: frontline PQC process picker autoclose static contract')
