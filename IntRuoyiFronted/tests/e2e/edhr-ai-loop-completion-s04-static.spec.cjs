const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const runner = fs.readFileSync(path.resolve(root, 'tests/e2e/edhr-ai-loop/runner.cjs'), 'utf8')
const manifest = fs.readFileSync(path.resolve(root, 'tests/e2e/edhr-ai-loop/manifest.cjs'), 'utf8')
const pickListPage = fs.readFileSync(
  path.resolve(root, 'src/views/erp/production/pick-list/index.vue'),
  'utf8'
)
const teamLeaderPage = fs.readFileSync(
  path.resolve(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const activeOrderDetailPanel = fs.readFileSync(
  path.resolve(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)
const replenishmentHelper = fs.readFileSync(
  path.resolve(root, 'src/views/mes/pro/processpool/activeOrderReplenishmentConfirmation.ts'),
  'utf8'
)
const packageJson = JSON.parse(fs.readFileSync(path.resolve(root, 'package.json'), 'utf8'))

assert.match(pickListPage, /data-production-pick-list-sync-kingdee/, 'pick list sync button needs a stable selector')
assert.match(pickListPage, /PRODUCTION_PICK_LIST/, 'S04 must trigger the formal production pick-list sync type')
assert.match(activeOrderDetailPanel, /data-active-order-production-record-input-material-pick-list/, 'active order detail must expose production input material pick-list evidence')
assert.match(activeOrderDetailPanel, /data-active-order-production-record-input-material-batch/, 'active order detail must expose production input material batch evidence')

for (const selector of [
  'data-team-leader-active-order-detail',
  'data-team-leader-active-order-release-apply',
  'data-team-leader-active-order-release-status',
  'data-team-leader-active-order-production-progress',
  'data-team-leader-active-order-inspection-progress'
]) {
  assert.match(teamLeaderPage, new RegExp(selector), `active order completion page missing ${selector}`)
}

assert.match(replenishmentHelper, /NO_REPLENISHMENT_CONFIRMATION_REQUIRED/)
assert.match(replenishmentHelper, /return undefined/, 'cancelled no-replenishment confirmation must leave no completion result')

assert.match(runner, /async function triggerProductionPickListSync\(page, manifestOrder\)/)
assert.match(
  runner,
  /async function verifyNoInputMaterialBackfillBeforePickList\(page, manifestOrder\)/,
  'S04 must prove input materials are not already backfilled before the pick-list sync'
)
assert.match(runner, /async function verifyCompletionInputMaterialBackfill\(page, manifestOrder\)/, 'S04 must verify completion backfilled pick-list source into input materials')
assert.match(runner, /\/erp\/production\/pick-list/)
assert.match(runner, /data-production-pick-list-sync-kingdee/)
assert.match(runner, /PRODUCTION_PICK_LIST/)
assert.match(runner, /data-active-order-production-record-input-material-pick-list/)
assert.doesNotMatch(manifest, /pickListCode\s*:/, 'Manifest must not invent a fixed pick-list code that the ERP creation endpoint does not control')
assert.doesNotMatch(runner, /manifestOrder\.pickListCode/, 'S04 must not compare an invented fixed pick-list code')
assert.match(runner, /inputMaterialPickListEvidence/, 'S04 must still record visible formal pick-list evidence after completion')
assert.match(runner, /inputMaterialBatchEvidence/, 'S04 must still record visible formal batch evidence after completion')
assert.match(runner, /prePickListInputMaterialBackfill/, 'S04 result must retain pre-sync empty backfill evidence')
assert.match(
  runner,
  /const prePickListInputMaterialBackfill = await verifyNoInputMaterialBackfillBeforePickList\(page, manifestOrder\)[\s\S]*const pickListSync = await triggerProductionPickListSync\(page, manifestOrder\)/,
  'S04 must check empty input-material backfill before triggering production pick-list sync'
)
assert.match(runner, /async function applyActiveOrderReleaseAndCancelNoReplenishment\(page, manifestOrder\)/)
assert.match(runner, /async function applyActiveOrderReleaseWithNoReplenishmentConfirmation\(page, manifestOrder\)/)
assert.match(runner, /data-team-leader-active-order-release-apply/)
assert.match(runner, /NO_REPLENISHMENT_CONFIRMATION_REQUIRED/)
assert.match(runner, /PQC_RELEASE_PENDING/)
assert.match(runner, /const completion = await runWithStage\('S04',[\s\S]*completeActiveOrderAndApplyRelease\(page, mainOrder\)/)
assert.match(runner, /failedStage:\s*null/, 'after S08 succeeds, runner must clear failedStage')
assert.match(runner, /stageResults\(null,\s*'PASS'\)/, 'S01-S08 PASS must be explicit')
assert.doesNotMatch(runner, /page\.request\.(post|get|put|delete)/)
assert.doesNotMatch(runner, /fetch\(/)

assert.equal(
  packageJson.scripts['e2e:edhr:ai-loop:completion-s04:static'],
  'node tests/e2e/edhr-ai-loop-completion-s04-static.spec.cjs'
)

console.log('PASS: eDHR AI loop S04 completion and no-replenishment static contract')




