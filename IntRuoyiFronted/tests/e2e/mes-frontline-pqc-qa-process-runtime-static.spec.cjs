const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const apiSource = read('src/api/mes/pro/feedback/index.ts')
const contextSource = read('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')
const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

assert.match(
  apiSource,
  /export interface FrontlinePqcSwitchActualEmployeeReqVO[\s\S]*activeOrderId: number[\s\S]*regulationVersionId: number[\s\S]*qaProcessId: number[\s\S]*pqcTaskId: number[\s\S]*actualEmployeeId: number/,
  'PQC employee switch must use the complete formal QA task identity.'
)
assert.match(
  contextSource,
  /activeOrderId:\s*activeOrder\.activeOrderId[\s\S]*pqcTaskId:\s*taskOption\.pqcTaskId/,
  'PQC employee switch payload must use activeOrderId and selected pqcTaskId.'
)
assert.match(
  contextSource,
  /employeeSwitchRequestToken[\s\S]*switchFrontlinePqcActualEmployee[\s\S]*requestToken[\s\S]*!== requestToken/,
  'Late PQC employee switch responses must be isolated by request token.'
)
assert.match(
  panelSource,
  /const clearPqcExecutionSelection[\s\S]*selectedEmployee = undefined[\s\S]*clearPqcTaskOptionDraft/,
  'Changing PQC order, process, or task must clear actual employee and draft state.'
)
assert.ok(
  !panelSource.includes('一线PQC员工已锁定为当前登录账号'),
  'The page must allow switching to any server-authorized PQC employee.'
)
assert.match(
  panelSource,
  /inspectionRuleKey === 'PATROL_AM'[\s\S]*inspectionRuleKey === 'PATROL_PM'/,
  'PATROL_AM and PATROL_PM must have separate visible labels and task identities.'
)

console.log('PASS: frontline PQC QA process runtime contract')
