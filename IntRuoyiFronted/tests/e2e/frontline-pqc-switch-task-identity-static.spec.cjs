const fs = require('fs')
const path = require('path')
const assert = require('assert')

const sourcePath = path.join(
  __dirname,
  '../../src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts'
)
const source = fs.readFileSync(sourcePath, 'utf8')

const functionStart = source.indexOf('export const buildFrontlinePqcEmployeeSwitchPayload')
assert.notStrictEqual(functionStart, -1, 'PQC employee switch payload builder must exist.')

const functionEndMatch = /\r?\n}\r?\n\r?\nexport const buildFrontlinePqcActiveOrderProcessCacheKey/.exec(
  source.slice(functionStart)
)
assert.ok(functionEndMatch, 'PQC employee switch payload builder boundary must stay stable.')

const payloadBuilder = source.slice(functionStart, functionStart + functionEndMatch.index)

assert.match(
  payloadBuilder,
  /regulationVersionId:\s*taskOption\.regulationVersionId/,
  'PQC employee switch must submit the selected task regulationVersionId, not the parent process identity.'
)
assert.match(
  payloadBuilder,
  /qaProcessId:\s*taskOption\.qaProcessId/,
  'PQC employee switch must submit the selected task qaProcessId, not the parent process identity.'
)
assert.doesNotMatch(
  payloadBuilder,
  /regulationVersionId:\s*process\.regulationVersionId/,
  'Parent process regulationVersionId can drift from the selected task option.'
)
assert.doesNotMatch(
  payloadBuilder,
  /qaProcessId:\s*process\.qaProcessId/,
  'Parent process qaProcessId can drift from the selected task option.'
)

console.log('PASS: frontline PQC employee switch uses selected task identity')
