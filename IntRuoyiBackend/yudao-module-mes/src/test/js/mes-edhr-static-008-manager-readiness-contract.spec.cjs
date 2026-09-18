const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const backend = path.resolve(__dirname, '../../../../')
const mes = path.join(backend, 'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes')

const read = (file) => fs.readFileSync(path.join(mes, file), 'utf8')

const sliceMethod = (source, signature) => {
  const startAt = source.indexOf(signature)
  assert.notEqual(startAt, -1, `missing method anchor: ${signature}`)
  const bodyStart = source.indexOf('{', startAt)
  assert.notEqual(bodyStart, -1, `missing method body: ${signature}`)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    const char = source[i]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(startAt, i + 1)
      }
    }
  }
  assert.fail(`unterminated method body: ${signature}`)
}

const businessReadiness = read('service/pro/productionrelease/manager/MesProductionReleaseBusinessReadinessService.java')
const resolveReadiness = sliceMethod(
  businessReadiness,
  'public MesProductionReleaseBusinessReadiness resolveBusinessReadinessChecks(MesProEdhrBatchExecutionDO batch)'
)
assert(resolveReadiness.includes('buildDhrCompletenessCheck(batch)')
  && resolveReadiness.includes('releaseCompletenessService.evaluateInspectionResult(batch)')
  && resolveReadiness.includes('releaseCompletenessService.evaluateDeviationClosed(batch)')
  && resolveReadiness.includes('releaseCompletenessService.evaluateReworkClosed(batch)')
  && resolveReadiness.includes('releaseCompletenessService.evaluateScrapRecorded(batch)')
  && resolveReadiness.includes('releaseCompletenessService.evaluateInventoryConsistency(batch)'),
'EDHR-STATIC-008: manager readiness must read all six formal business checks')
assert(resolveReadiness.includes('checks.size()'),
  'EDHR-STATIC-008: required check count must match the formal check collection size')
assert(resolveReadiness.includes('DigestUtil.sha256Hex(snapshotJson)'),
  'EDHR-STATIC-008: readiness evidence must be hashable and persisted as a snapshot')

const isFailedCheck = sliceMethod(businessReadiness, 'private boolean isFailedCheck(MesOrderReleaseCompletenessCheck item)')
assert(isFailedCheck.includes('validateCheckItem(item)')
  && isFailedCheck.includes('CHECK_RESULT_NOT_APPLICABLE')
  && isFailedCheck.includes('CHECK_RESULT_PASS'),
'EDHR-STATIC-008: not applicable checks must be distinct from failed checks, and unknown items must fail fast')
const isBlockingCheck = sliceMethod(businessReadiness, 'private boolean isBlockingCheck(MesOrderReleaseCompletenessCheck item)')
assert(isBlockingCheck.includes('STATUS_PRECHECK_REQUIRED')
  && isBlockingCheck.includes('SEVERITY_BLOCKER'),
'EDHR-STATIC-008: unverified PRECHECK_REQUIRED and blocking severity must block release')
const validateCheckItem = sliceMethod(businessReadiness, 'private void validateCheckItem(MesOrderReleaseCompletenessCheck item)')
assert(validateCheckItem.includes('KNOWN_RESULTS.contains(item.checkResult())')
  && validateCheckItem.includes('unknown business readiness check result'),
'EDHR-STATIC-008: unknown readiness results must not be treated as success')

const initializer = read('service/pro/productionrelease/manager/MesProductionReleaseManagerStageInitializerImpl.java')
const buildTransaction = sliceMethod(
  initializer,
  'private MesProEdhrReleaseTransactionDO buildTransaction('
)
for (const field of [
  'DhrStatus',
  'InspectionStatus',
  'DeviationStatus',
  'ReworkStatus',
  'ScrapStatus',
  'InventoryStatus'
]) {
  assert(buildTransaction.includes(`.set${field}(businessReadiness.`),
    `EDHR-STATIC-008: set${field} must come from formal readiness, not a hard-coded PASS`)
  assert(!buildTransaction.includes(`.set${field}("PASS")`),
    `EDHR-STATIC-008: set${field} must not be hard-coded to PASS`)
}
assert(buildTransaction.includes('.setRequiredCheckCount(businessReadiness.requiredCheckCount())')
  && buildTransaction.includes('.setFailedCheckCount(businessReadiness.failedCheckCount())')
  && buildTransaction.includes('.setBlockingCheckCount(businessReadiness.blockingCheckCount())')
  && !buildTransaction.includes('.setRequiredCheckCount(4)'),
'EDHR-STATIC-008: transaction counts must come from formal readiness results')
assert(buildTransaction.includes('businessReadinessSnapshotHash')
  && buildTransaction.includes('businessReadinessSnapshotJson'),
'EDHR-STATIC-008: manager transaction must retain the business readiness snapshot')

const approval = read('service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceImpl.java')
assert(approval.includes('requireBusinessReadiness(application, batch)')
  && approval.includes('businessReadinessService.resolveBusinessReadinessChecks(batch)'),
'EDHR-STATIC-008: final manager approval must recompute business readiness before signoff release')

console.log('PASS: EDHR-STATIC-008 manager readiness contract')
