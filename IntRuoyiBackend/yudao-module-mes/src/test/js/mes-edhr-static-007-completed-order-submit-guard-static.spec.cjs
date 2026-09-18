const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const backend = path.resolve(__dirname, '../../../../')
const mes = path.join(backend, 'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes')
const tests = path.join(backend, 'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes')

const read = (base, file) => fs.readFileSync(path.join(base, file), 'utf8')
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

const authService = read(mes, 'service/pro/frontline/MesFrontlineSubmitAuthorizationServiceImpl.java')
const authorizeActiveOrder = sliceMethod(authService, 'public void authorizeActiveOrder(')
assert(authorizeActiveOrder.includes('selectByIdForUpdate(activeOrderId)'),
  'frontline submit authorization must lock the active order before checking source boundary')
assert(authorizeActiveOrder.includes('assertActiveOrderOpenForProduction(activeOrder)'),
  'frontline submit authorization must reject completed or release-application locked active orders')
const openForProduction = sliceMethod(authService,
  'private void assertActiveOrderOpenForProduction(MesProcessPoolActiveOrderDO activeOrder)')
assert(openForProduction.includes('!"ACTIVE".equals(activeOrder.getBusinessStatus())'),
  'frontline submit authorization must reject completed businessStatus before production writes')
assert(openForProduction.includes('releaseStateService.isReleaseApplicationLockedForUpdate(activeOrder.getId())'),
  'frontline submit authorization must reject any existing release application before production writes')

const allocationService = read(mes, 'service/pro/processpool/team/MesReportAllocationCommandService.java')
const initialAllocation = sliceMethod(allocationService, 'public void createInitialAllocation(')
assert(initialAllocation.includes('activeOrderMapper.selectByIdForUpdate(activeOrderId)')
  && initialAllocation.includes('assertActiveOrdersOpenForProduction(List.of(activeOrder))'),
  'initial allocation must lock and reject completed or release-application active orders before insertBatch')
const saveAllocation = sliceMethod(allocationService,
  'public MesReportAllocationSnapshot save(MesReportAllocationSaveCommand command)')
assert(saveAllocation.includes('selectActiveListByLeaderForUpdate(command.getLeaderUserId())')
  && saveAllocation.includes('assertActiveOrdersOpenForProduction(releaseCandidates, activeById)')
  && saveAllocation.indexOf('assertActiveOrdersOpenForProduction(releaseCandidates, activeById)')
  < saveAllocation.indexOf('findReleasedActiveOrderIdsForUpdate(releaseCandidates)'),
  'allocation save must reject completed/release-application orders before editable writes')
const rejectProduction = sliceMethod(allocationService,
  'public Long rejectProductionSubmission(Long eventId, Long leaderUserId,')
assert(rejectProduction.includes('assertActiveOrdersOpenForProduction(activeOrderIds, Map.of())')
  && rejectProduction.indexOf('assertActiveOrdersOpenForProduction(activeOrderIds, Map.of())')
  < rejectProduction.indexOf('supersedeCurrentRows'),
  'production rejection must reject release-application locked source boundaries before rollback writes')
const allocationOpenGuard = sliceMethod(allocationService,
  'private void assertActiveOrdersOpenForProduction(Collection<Long> activeOrderIds,')
assert(allocationOpenGuard.includes('!"ACTIVE".equals(activeOrder.getBusinessStatus())'),
  'allocation guard must reject completed active orders')
assert(allocationOpenGuard.includes('findReleaseApplicationLockedActiveOrderIdsForUpdate(activeOrderIds)'),
  'allocation guard must reject any existing release application, not only final released transactions')

const releaseState = read(mes, 'service/pro/processpool/team/MesReportAllocationReleaseStateService.java')
assert(releaseState.includes('findReleaseApplicationLockedActiveOrderIdsForUpdate')
  && releaseState.includes('resolveReleaseApplicationLockedActiveOrderIds')
  && !releaseState.includes('transaction != null && RELEASED.equals(transaction.getReleaseStatus())) {\n                locked'),
  'release source lock must treat application existence as closed production boundary')

const authTest = read(tests, 'service/pro/frontline/MesFrontlineSubmitAuthorizationTest.java')
assert(authTest.includes('shouldRejectCompletedActiveOrderBeforeProductionSubmit')
  && authTest.includes('shouldRejectReleaseApplicationLockedActiveOrderBeforeProductionSubmit'),
  'frontline authorization regression tests must cover completed and release-application locked orders')
const allocationTest = read(tests, 'service/pro/processpool/team/MesReportAllocationCommandServiceTest.java')
assert(allocationTest.includes('shouldRejectInitialAllocationWhenActiveOrderAlreadyCompleted')
  && allocationTest.includes('shouldRejectInitialAllocationWhenReleaseApplicationAlreadyExists')
  && allocationTest.includes('shouldRejectAllocationSaveWhenActiveOrderAlreadyCompleted')
  && allocationTest.includes('shouldRejectAllocationSaveWhenReleaseApplicationAlreadyExists')
  && allocationTest.includes('shouldRejectProductionSubmissionBeforeWritingWhenAnyAllocationWasReleased'),
  'allocation regression tests must cover initial allocation, save and rejection lock boundaries')
const releaseStateTest = read(tests, 'service/pro/processpool/team/MesReportAllocationReleaseStateServiceTest.java')
assert(releaseStateTest.includes('productionWriteLockMustTreatAnyReleaseApplicationAsClosedSourceBoundary'),
  'release-state regression test must cover application-existence production lock semantics')

console.log('PASS: EDHR-STATIC-007 completed order production submit guard contract')
