const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const runner = fs.readFileSync(path.resolve(root, 'tests/e2e/edhr-ai-loop/runner.cjs'), 'utf8')
const workTaskPage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue'), 'utf8')
const pqcReleasePage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/production-release/PqcProductionReleasePage.vue'), 'utf8')
const nonconformancePage = fs.readFileSync(path.resolve(root, 'src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'), 'utf8')
const remainingRoutes = fs.readFileSync(path.resolve(root, 'src/router/modules/remaining.ts'), 'utf8')
const packageJson = JSON.parse(fs.readFileSync(path.resolve(root, 'package.json'), 'utf8'))

for (const selector of [
  'data-edhr-work-task-page',
  'data-edhr-work-task-work-order-filter',
  'data-edhr-work-task-query',
  'data-pqc-release-open'
]) {
  assert.match(workTaskPage, new RegExp(selector), `work task board missing ${selector}`)
}

for (const selector of [
  'data-pqc-production-release-work-order-filter',
  'data-pqc-production-release-query',
  'data-pqc-production-release-work-order-code',
  'data-pqc-production-release-approve',
  'data-pqc-production-release-dialog',
  'data-pqc-production-release-signature-password',
  'data-pqc-production-release-approval-opinion',
  'data-pqc-production-release-confirm',
  'data-pqc-production-release-batch-execution-id'
]) {
  assert.match(pqcReleasePage, new RegExp(selector), `PQC production release page missing ${selector}`)
}
for (const selector of [
  'data-edhr-ncr-page',
  'data-edhr-ncr-active-order-detail',
  'data-edhr-ncr-active-order-detail-back',
  'data-edhr-ncr-create-reason',
  'data-edhr-ncr-create-submit',
  'data-edhr-ncr-review-material',
  'data-edhr-ncr-review-opinion',
  'data-edhr-ncr-signature-password',
  'data-edhr-ncr-concession-release',
  'data-edhr-ncr-disposition-result',
  'data-edhr-ncr-qa-signature'
]) {
  assert.match(nonconformancePage, new RegExp(selector), `nonconformance review page missing ${selector}`)
}
assert.match(nonconformancePage, /ActiveOrderSubmissionDetailPanel/)
assert.match(nonconformancePage, /getNonconformanceReviewActiveOrderDetail/)
assert.match(nonconformancePage, /data-edhr-ncr-active-order-detail[\s\S]*>\s*详情\s*</)
assert.doesNotMatch(nonconformancePage, /@click\.stop="selectReview\(row\)"[\s\S]*>\s*处理\s*</)
assert.doesNotMatch(pqcReleasePage, /data-pqc-production-release-report-task-list/)
assert.doesNotMatch(pqcReleasePage, />\s*查看批记录\s*</)

assert.match(runner, /async function completePqcReleaseNonconformanceReview\(page, manifestOrder, row, runDir\)/)
assert.match(runner, /async function approvePqcProductionReleaseS05\(page, manifestOrder, completion, runDir\)/)
assert.match(runner, /\/mes\/pro\/feedback\/edhr-work-task/)
assert.match(runner, /data-pqc-release-open/)
assert.match(runner, /data-pqc-production-release-work-order-filter/)
assert.match(runner, /data-pqc-production-release-approve/)
assert.match(runner, /data-pqc-production-release-nonconformance/)
assert.match(runner, /data-edhr-ncr-create-submit/)
assert.match(runner, /data-edhr-ncr-concession-release/)
assert.match(runner, /NONCONFORMANCE_REVIEW_CREATE/)
assert.match(runner, /NONCONFORMANCE_REVIEW_DISPOSE/)
assert.match(runner, /data-pqc-production-release-signature-password/)
assert.match(runner, /\/mes\/pro\/production-release\/pqc\/approve/)
assert.match(runner, /REPORT_UPLOAD_PENDING/)
assert.match(runner, /reportUploadTasks[\s\S]*length[\s\S]*4/)
assert.match(runner, /const pqcRelease = await runWithStage\('S05',[\s\S]*approvePqcProductionReleaseS05\(page, mainOrder, completion, runDir\)/)
assert.match(remainingRoutes, /path:\s*'\/mes\/production-release\/pqc'[\s\S]*path:\s*''[\s\S]*PqcProductionReleasePage[\s\S]*name:\s*'MesPqcProductionReleaseActionUrl'/)
assert.match(remainingRoutes, /path:\s*'production-release\/pqc'[\s\S]*PqcProductionReleasePage[\s\S]*name:\s*'MesPqcProductionRelease'/)
assert.match(runner, /failedStage:\s*null/, 'after S08 succeeds, runner must clear failedStage')
assert.match(runner, /stageResults\(null,\s*'PASS'\)/, 'S01-S08 PASS must be explicit')
assert.doesNotMatch(runner, /page\.request\.(post|get|put|delete)/)
assert.doesNotMatch(runner, /fetch\(/)

assert.equal(packageJson.scripts['e2e:edhr:ai-loop:release-s05:static'], 'node tests/e2e/edhr-ai-loop-release-s05-static.spec.cjs')

console.log('PASS: eDHR AI loop S05 PQC production release static contract')
