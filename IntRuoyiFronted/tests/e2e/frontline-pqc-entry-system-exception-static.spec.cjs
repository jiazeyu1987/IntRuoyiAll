const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const contextSource = read('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')
const apiSource = read('src/api/mes/pro/feedback/index.ts')

const between = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const activeOrderSelectionBlock = between(
  panelSource,
  'const handleSelectActiveOrder = async (',
  'const handleSelectProcess = async ('
)
const pqcSelectionBlock = between(
  activeOrderSelectionBlock,
  'pqcSubmitResultUncertain.value = false',
  'applyActiveOrderToContext(activeOrder)'
)

assert.match(
  apiSource,
  /getPqcProcesses: async \(activeOrderId: number, actualEmployeeId\?: number\)/,
  'PQC process API may accept an actual employee only after the page has a validated employee context.'
)
assert.match(
  contextSource,
  /buildFrontlinePqcEmployeeSwitchPayload[\s\S]*if \(!taskOption\)[\s\S]*throw new Error\('当前PQC任务不能为空'\)/,
  'PQC employee switch remains task-bound and must not run before a task option exists.'
)
assert.match(
  pqcSelectionBlock,
  /selectFrontlinePqcActiveOrder\(\s*deviceState,\s*activeOrder\s*\)/,
  'PQC entry active-order selection must load processes without an employee-scoped default lookup.'
)
assert.doesNotMatch(
  pqcSelectionBlock,
  /selectFrontlinePqcActiveOrder\([\s\S]*currentLoginUserId|selectFrontlinePqcActiveOrder\([\s\S]*actualEmployeeId/,
  'PQC entry must not pass currentLoginUserId as actualEmployeeId before the user has selected a task/process.'
)

const pqcActiveOrderSelectionContextBlock = between(
  contextSource,
  'export const selectFrontlinePqcActiveOrder = async (',
  'export const selectFrontlineProcess = async ('
)
const pqcActiveOrderCacheHitBlock = between(
  pqcActiveOrderSelectionContextBlock,
  'if (cachedProcesses) {',
  'state.processOptions = []'
)
assert.match(
  pqcActiveOrderCacheHitBlock,
  /state\.lastError = undefined/,
  'PQC active-order cache hits must clear stale request errors before rendering the selected order.'
)
assert.match(
  pqcActiveOrderCacheHitBlock,
  /state\.loadingProcesses = false/,
  'PQC active-order cache hits must finish the process loading state.'
)

const pqcProcessSelectionContextBlock = between(
  contextSource,
  'export const selectFrontlinePqcProcess = async (',
  'export const switchFrontlineActualEmployee = async ('
)
const pqcEmployeeCacheHitBlock = between(
  pqcProcessSelectionContextBlock,
  'if (cachedEmployees) {',
  'state.employeeOptions = []'
)
assert.match(
  pqcEmployeeCacheHitBlock,
  /state\.lastError = undefined/,
  'PQC employee cache hits must clear stale request errors before rendering the selected process.'
)
assert.match(
  pqcEmployeeCacheHitBlock,
  /state\.loadingEmployees = false/,
  'PQC employee cache hits must finish the employee loading state.'
)
console.log('frontline-pqc-entry-system-exception-static: PASS')
