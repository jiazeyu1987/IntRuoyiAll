const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const runner = fs.readFileSync(path.resolve(root, 'tests/e2e/edhr-ai-loop/runner.cjs'), 'utf8')
const workTaskPage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue'), 'utf8')
const batchDetailPage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'), 'utf8')
const packageJson = JSON.parse(fs.readFileSync(path.resolve(root, 'package.json'), 'utf8'))

for (const nodeType of [
  'INCOMING_INSPECTION_REPORT',
  'STERILIZATION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_RECORD'
]) {
  assert.match(runner, new RegExp(nodeType), `runner missing report node ${nodeType}`)
  assert.match(batchDetailPage, new RegExp(nodeType), `batch detail missing report node ${nodeType}`)
}

for (const selector of [
  'data-edhr-work-task-page',
  'data-edhr-work-task-work-order-filter',
  'data-production-release-report-open'
]) {
  assert.match(workTaskPage, new RegExp(selector), `work task board missing ${selector}`)
}

for (const selector of [
  'data-edhr-batch-detail-page',
  'data-production-release-report-upload',
  'data-production-release-report-complete',
  'data-production-release-report-complete-dialog',
  'data-production-release-sterilization-batch',
  'data-production-release-report-complete-confirm',
  'data-production-release-report-stage-receipt'
]) {
  assert.match(batchDetailPage, new RegExp(selector), `batch detail page missing ${selector}`)
}

assert.match(runner, /async function uploadReleaseReportsS06\(page, manifestOrder, pqcRelease, runDir\)/)
assert.match(runner, /createReportFixtureFiles\(runDir, manifestOrder\)/)
assert.match(runner, /\/Count 1/, 'S06 report fixtures must be one-page PDFs, not zero-page placeholder files')
assert.match(runner, /xref/, 'S06 report fixtures must include a PDF xref table for upload/archive parsers')
assert.match(runner, /startxref/, 'S06 report fixtures must include startxref for deterministic PDF parsing')
assert.match(runner, /data-production-release-report-open/)
assert.match(runner, /data-production-release-report-upload/)
assert.match(runner, /setInputFiles/)
assert.match(runner, /\/task\/special-node\/attachment\/prepare-upload/)
assert.match(runner, /data-production-release-report-complete/)
assert.match(runner, /data-production-release-sterilization-batch/)
assert.match(runner, /\/task\/special-node\/complete/)
assert.match(runner, /MANAGER_RELEASE_PENDING/)
assert.match(runner, /managerReleaseWorkTaskId/)
assert.match(runner, /const reportUpload = await runWithStage\('S06',[\s\S]*uploadReleaseReportsS06\(page, mainOrder, pqcRelease, runDir\)/)
assert.match(runner, /failedStage:\s*null/, 'after S08 succeeds, runner must clear failedStage')
assert.match(runner, /stageResults\(null,\s*'PASS'\)/, 'S01-S08 PASS must be explicit')
assert.doesNotMatch(runner, /page\.request\.(post|get|put|delete)/)
assert.doesNotMatch(runner, /fetch\(/)

assert.equal(packageJson.scripts['e2e:edhr:ai-loop:report-upload-s06:static'], 'node tests/e2e/edhr-ai-loop-report-upload-s06-static.spec.cjs')

console.log('PASS: eDHR AI loop S06 four report uploads static contract')
