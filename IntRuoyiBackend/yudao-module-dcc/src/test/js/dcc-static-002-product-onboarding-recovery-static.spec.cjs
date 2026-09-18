const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const extract = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker`)
  return source.slice(start, end)
}

const controller = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/DccProductOnboardingController.java'
)
const service = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/onboarding/DccProductOnboardingService.java'
)
const serviceImpl = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/onboarding/DccProductOnboardingServiceImpl.java'
)
const requestMapper = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/projectcode/DccProductOnboardingRequestMapper.java'
)
const serviceTest = read(
  'src/test/java/cn/iocoder/yudao/module/dcc/service/projectcode/onboarding/DccProductOnboardingServiceImplTest.java'
)

assert.match(
  controller,
  /@GetMapping\("\/pending"\)[\s\S]{0,240}@PreAuthorize\("@ss\.hasPermission\('dcc:project-code:create'\) or @ss\.hasPermission\('dcc:project-code:update'\)"\)[\s\S]{0,220}CommonResult<List<DccProductOnboardingRespVO>> getPendingRequests\(\)/,
  'product onboarding controller must expose a formal pending-list recovery endpoint for applicants and approvers'
)
assert.match(
  controller,
  /onboardingService\.getPendingRequests\(\)\.stream\(\)[\s\S]{0,180}BeanUtils\.toBean\(request, DccProductOnboardingRespVO\.class\)/,
  'pending-list endpoint must return persisted onboarding request snapshots, not transient dialog state'
)

assert.match(
  service,
  /List<DccProductOnboardingRequestDO> getPendingRequests\(\);/,
  'service contract must include pending request recovery'
)
assert.match(
  serviceImpl,
  /public List<DccProductOnboardingRequestDO> getPendingRequests\(\)\s*\{[\s\S]{0,120}return requestMapper\.selectPendingList\(\);[\s\S]{0,20}\}/,
  'service implementation must delegate pending recovery to the mapper'
)
assert.match(
  requestMapper,
  /selectPendingList\(\)[\s\S]{0,220}\.eq\(DccProductOnboardingRequestDO::getStatus,[\s\S]{0,120}DccProductOnboardingStatusConstants\.PENDING_APPROVAL\)/,
  'pending recovery mapper must only list PENDING_APPROVAL requests'
)

assert.match(
  serviceImpl,
  /validateProjectCodeAvailable\(request\.getProjectName\(\),\s*request\.getProjectCode\(\),\s*request\.getId\(\)\);/,
  'approval duplicate check must ignore the current pending request itself'
)
const duplicateCheck = extract(
  serviceImpl,
  'private void validateProjectCodeAvailable(String projectName, String projectCode, Long ignoredRequestId)',
  'private NormalizedOnboardingRequest normalizeCreateReq',
  'product onboarding duplicate check'
)
assert.match(
  duplicateCheck,
  /pendingRequest != null[\s\S]{0,120}ignoredRequestId == null \|\| !ignoredRequestId\.equals\(pendingRequest\.getId\(\)\)/,
  'duplicate check must keep blocking other pending requests while excluding the current request under approval'
)

assert.match(
  serviceTest,
  /createRequest_shouldRejectExistingPendingRequestBeforeWritingRequest/,
  'JUnit regression must keep duplicate pending create blocked'
)
assert.match(
  serviceTest,
  /getPendingRequests_shouldReturnPendingRequestsForRecovery/,
  'JUnit regression must cover pending-list recovery'
)
assert.match(
  serviceTest,
  /approveRequest_shouldIgnoreCurrentPendingRequestWhenCheckingDuplicatePendingProject/,
  'JUnit regression must cover approval of the current pending request'
)

console.log('DCC-STATIC-002 product onboarding recovery contract passed')
