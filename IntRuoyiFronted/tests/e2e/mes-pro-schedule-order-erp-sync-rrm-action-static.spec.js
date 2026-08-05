const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const realFlowPath = path.join(root, 'tests/e2e/role-requirement-matrix-real-flow.e2e.js')
const packageJsonPath = path.join(root, 'package.json')

const source = fs.readFileSync(realFlowPath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))

assert.equal(
  packageJson.scripts['e2e:role-requirement-matrix:real'],
  'node tests/e2e/role-requirement-matrix-real-flow.e2e.js',
  'RRM real E2E script must remain the canonical M6 action-evidence runner.'
)

assert.match(
  source,
  /async function verifyScheduleOrderErpCandidateAdmission\(page,\s*config\)[\s\S]*?\/mes\/pro\/schedule-order/,
  'RRM real E2E must include an AC-M01 action that opens the schedule-order page.'
)

assert.match(
  source,
  /async function verifyScheduleOrderErpCandidateAdmission[\s\S]*?getByRole\('tab',\s*\{\s*name:\s*'同步工单'\s*\}\)/,
  'AC-M01 action must use the real "同步工单" tab, not API-only verification.'
)

assert.match(
  source,
  /async function verifyScheduleOrderErpCandidateAdmission[\s\S]*?\/mes\/pro\/schedule-order\/admission-diff[\s\S]*?workOrderCode:\s*config\.productionOrderCode/,
  'AC-M01 action must query candidates by the formal production order code.'
)

assert.match(
  source,
  /async function verifyScheduleOrderErpCandidateAdmission[\s\S]*?BLOCKED_ERP_SYNC_RECORD_MISSING[\s\S]*?selectable\s*===\s*false/,
  'AC-M01 action must prove missing ERP formal identity rows are blocked and not selectable.'
)

assert.match(
  source,
  /key:\s*'scheduleOrderErpCandidateAdmission'[\s\S]*?acceptanceIds:\s*\[\s*'AC-M01'\s*\]/,
  'AC-M01 action evidence must map to AC-M01 in the coverage ledger.'
)

assert.match(
  source,
  /if \(phase\.actionKey === 'joinActiveOrder'\)[\s\S]*?verifyScheduleOrderErpCandidateAdmission\(page,\s*config\)/,
  'production leader phase must run AC-M01 admission evidence before active-order join evidence.'
)
