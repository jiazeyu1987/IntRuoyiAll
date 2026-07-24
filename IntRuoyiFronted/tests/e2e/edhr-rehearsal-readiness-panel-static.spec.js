const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/edhr/batchExecution.ts')
const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  apiSource.includes('EdhrRehearsalReadinessResult') &&
    apiSource.includes('getEdhrRehearsalReadiness'),
  'Batch execution API module must expose typed rehearsal readiness request and response.'
)

assert(
  apiSource.includes("url: `${BATCH_EXECUTION_BASE_URL}/rehearsal-readiness`") &&
    apiSource.includes('routeId') &&
    apiSource.includes('executorUserId') &&
    apiSource.includes('approverUserId') &&
    apiSource.includes('archiverUserId'),
  'Readiness API must call the real /rehearsal-readiness endpoint with all required role parameters.'
)

assert(
  pageSource.includes('演练预检') &&
    pageSource.includes('openReadinessDialog') &&
    pageSource.includes('submitReadinessCheck'),
  'Batch execution list page must provide a visible rehearsal readiness entry and submit action.'
)

assert(
  pageSource.includes('readinessForm.routeId') &&
    pageSource.includes('readinessForm.executorUserId') &&
    pageSource.includes('readinessForm.approverUserId') &&
    pageSource.includes('readinessForm.archiverUserId'),
  'Readiness panel must collect route, executor, approver, and archiver identifiers.'
)

assert(
  pageSource.includes('getEdhrRehearsalReadiness') &&
    pageSource.includes('readinessResult') &&
    pageSource.includes('readinessBlockerCount') &&
    pageSource.includes('readinessPassCount'),
  'Readiness panel must call the real API and display pass/blocker summary.'
)

assert(
  pageSource.includes('readinessError') &&
    pageSource.includes('resolveErrorMessage(error') &&
    !pageSource.includes('catch (error) {}'),
  'Readiness panel must expose validation/API errors and must not silently swallow failures.'
)

console.log('PASS: eDHR rehearsal readiness panel static contract')
