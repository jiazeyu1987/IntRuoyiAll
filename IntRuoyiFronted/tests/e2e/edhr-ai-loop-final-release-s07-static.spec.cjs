const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const runner = fs.readFileSync(path.resolve(root, 'tests/e2e/edhr-ai-loop/runner.cjs'), 'utf8')
const workTaskPage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue'), 'utf8')
const packageJson = JSON.parse(fs.readFileSync(path.resolve(root, 'package.json'), 'utf8'))

for (const selector of [
  'data-edhr-work-task-page',
  'data-edhr-work-task-work-order-filter',
  'data-manager-release-approve',
  'data-manager-release-dialog',
  'data-manager-release-signature-password',
  'data-manager-release-approval-opinion',
  'data-manager-release-confirm',
  'data-manager-release-status'
]) {
  assert.match(workTaskPage, new RegExp(selector), `work task board missing ${selector}`)
}

assert.match(runner, /async function approveManagerFinalReleaseS07\(page, manifestOrder, reportUpload\)/)
assert.match(runner, /data-manager-release-approve/)
assert.match(runner, /data-manager-release-signature-password/)
assert.match(runner, /\/approval-center\/tasks\/review/)
assert.match(runner, /\/mes\/pro\/edhr-release\/get/)
assert.match(runner, /releaseStatus[\s\S]*RELEASED/)
assert.match(runner, /const finalRelease = await runWithStage\('S07',[\s\S]*approveManagerFinalReleaseS07\(page, mainOrder, reportUpload\)/)
assert.match(runner, /failedStage:\s*null/, 'after S08 succeeds, runner must clear failedStage')
assert.match(runner, /stageResults\(null,\s*'PASS'\)/, 'S01-S08 PASS must be explicit')
assert.doesNotMatch(runner, /page\.request\.(post|get|put|delete)/)
assert.doesNotMatch(runner, /fetch\(/)

assert.equal(packageJson.scripts['e2e:edhr:ai-loop:final-release-s07:static'], 'node tests/e2e/edhr-ai-loop-final-release-s07-static.spec.cjs')

console.log('PASS: eDHR AI loop S07 manager final release static contract')
