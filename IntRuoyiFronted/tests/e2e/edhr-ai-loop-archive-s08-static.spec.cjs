const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '..', '..')
const runner = fs.readFileSync(path.resolve(root, 'tests/e2e/edhr-ai-loop/runner.cjs'), 'utf8')
const workTaskPage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue'), 'utf8')
const detailPage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'), 'utf8')
const historyPage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue'), 'utf8')
const packageJson = require(path.resolve(root, 'package.json'))

for (const selector of [
  'data-edhr-work-task-row-work-order',
  'data-edhr-work-task-row-type',
  'data-edhr-archive-task-open'
]) {
  assert.match(workTaskPage, new RegExp(selector), `work task board missing ${selector}`)
}

for (const selector of [
  'data-edhr-release-action',
  'data-edhr-archive-drawer',
  'data-edhr-archive-generate',
  'data-edhr-archive-version',
  'data-edhr-archive-status'
]) {
  assert.match(detailPage, new RegExp(selector), `batch detail page missing ${selector}`)
}

for (const selector of [
  'data-edhr-batch-history-page',
  'data-edhr-history-work-order-filter',
  'data-edhr-history-batch-code-filter',
  'data-edhr-history-query',
  'data-edhr-history-batch-list',
  'data-edhr-history-batch-item',
  'data-edhr-history-work-order-code',
  'data-edhr-history-batch-code',
  'data-edhr-history-batch-status',
  'data-edhr-history-source-count',
  'data-edhr-history-timeline-item',
  'data-edhr-history-dossier-item',
  'data-edhr-history-process-item',
  'data-edhr-history-attachment-item'
]) {
  assert.match(historyPage, new RegExp(selector), `history page missing ${selector}`)
}

assert.match(runner, /async function archiveAndVerifyHistoryS08\(page, manifestOrder, finalRelease\)/)
assert.match(runner, /async function openArchiveTaskFromWorkTask\(page, manifestOrder, finalRelease\)/)
assert.match(runner, /data-edhr-archive-task-open/)
assert.match(runner, /data-edhr-archive-generate/)
assert.match(runner, /\/mes\/pro\/edhr-batch-execution-archive\/generate/)
assert.match(runner, /\/mes\/pro\/feedback\/edhr-batch-history/)
assert.match(runner, /data-edhr-history-source-count/)
assert.match(runner, /archiveVersions/)
assert.match(runner, /archiveStatus:\s*'SEALED'/)
assert.match(runner, /const archiveTrace = await runWithStage\('S08',[\s\S]*archiveAndVerifyHistoryS08\(page, mainOrder, finalRelease\)/)
assert.match(runner, /status:\s*'PASS'/, 'runner must mark S01-S08 PASS after history trace verification')
assert.match(runner, /failedStage:\s*null/, 'full-chain success result must not keep failedStage')
assert.match(runner, /stageResults\(null,\s*'PASS'\)/, 'stage table must support all stages PASS')

assert.equal(packageJson.scripts['e2e:edhr:ai-loop:archive-s08:static'], 'node tests/e2e/edhr-ai-loop-archive-s08-static.spec.cjs')

console.log('PASS: eDHR AI loop S08 archive and history trace static contract')
