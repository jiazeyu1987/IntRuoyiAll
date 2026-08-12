const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)
const packageJson = JSON.parse(fs.readFileSync(path.join(repoRoot, 'package.json'), 'utf8'))

assert.equal(
  packageJson.scripts?.['e2e:mes:scheduler-workbench-policy-refresh:static'],
  'node tests/e2e/scheduler-workbench-policy-refresh-static.spec.js',
  'package.json must expose the scheduler workbench policy refresh static gate'
)

const savePolicyMatch = pageSource.match(
  /const savePolicySettings = async \(\) => \{[\s\S]*?\n\}/
)
assert.ok(savePolicyMatch, 'savePolicySettings function must exist')
const savePolicySource = savePolicyMatch[0]

assert.ok(
  savePolicySource.includes('await SchedulerWorkbenchApi.savePolicySettings({ ...policySettingsForm })'),
  'savePolicySettings must continue saving through the formal scheduler workbench policy API'
)
assert.ok(
  savePolicySource.includes('await Promise.all([loadSummary(), loadProcessWipStatistics()])'),
  'savePolicySettings must refresh summary and process WIP after saving policy settings'
)
assert.ok(
  savePolicySource.indexOf('await Promise.all([loadSummary(), loadProcessWipStatistics()])') >
    savePolicySource.indexOf('await SchedulerWorkbenchApi.savePolicySettings({ ...policySettingsForm })'),
  'refresh must happen after policy settings are saved'
)

console.log('PASS: scheduler workbench policy save refreshes visible capacity outputs')
