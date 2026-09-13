const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')

function read(relativePath) {
  const fullPath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(fullPath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(fullPath, 'utf8')
}

function elementByTestId(source, testId) {
  const marker = `data-testid="${testId}"`
  const markerIndex = source.indexOf(marker)
  assert.notEqual(markerIndex, -1, `missing test id: ${testId}`)
  const start = source.lastIndexOf('<el-button', markerIndex)
  assert.notEqual(start, -1, `${testId} must be rendered by an el-button`)
  const end = source.indexOf('</el-button>', markerIndex)
  assert.notEqual(end, -1, `${testId} button must have a closing tag`)
  return source.slice(start, end + '</el-button>'.length)
}

function assertPermission(block, permission, message) {
  assert.ok(block.includes(permission), message)
}

function assertNoPermission(block, permission, message) {
  assert.ok(!block.includes(permission), message)
}

const panelSource = read(
  'IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
)
const apiSource = read('IntRuoyiFronted/src/api/dcc/controlledFile/projectCodes.ts')
const controllerSource = read(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/DccProductOnboardingController.java'
)
const serviceSource = read(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/onboarding/DccProductOnboardingService.java'
)
const serviceImplSource = read(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/onboarding/DccProductOnboardingServiceImpl.java'
)
const mapperSource = read(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/projectcode/DccProductOnboardingRequestMapper.java'
)

const opener = elementByTestId(panelSource, 'dcc-product-onboarding-open')
assertPermission(
  opener,
  'dcc:project-code:create',
  'product onboarding entry must remain reachable to request creators'
)
assertPermission(
  opener,
  'dcc:project-code:update',
  'product onboarding entry must be reachable to approval-only users with update permission'
)

const submitButton = elementByTestId(panelSource, 'dcc-product-onboarding-submit')
assertPermission(
  submitButton,
  'dcc:project-code:create',
  'new onboarding request submission must be guarded by create permission'
)
assertNoPermission(
  submitButton,
  'dcc:project-code:update',
  'update permission alone must not expose the create-request submit action'
)

const approveButton = elementByTestId(panelSource, 'dcc-product-onboarding-approve')
assertPermission(
  approveButton,
  'dcc:project-code:update',
  'onboarding approval must be guarded by update permission'
)
assertNoPermission(
  approveButton,
  'dcc:project-code:create',
  'create permission alone must not expose the approval action'
)

assert.ok(
  panelSource.includes('data-testid="dcc-product-onboarding-pending-list"'),
  'dialog must render the pending onboarding list for continuation'
)
assert.ok(
  panelSource.includes('data-testid="dcc-product-onboarding-recover"'),
  'dialog must provide a pending-request recovery action'
)
assert.ok(
  panelSource.includes('getPendingProductOnboardingRequests'),
  'project code page must load pending onboarding requests from the formal API'
)
assert.ok(
  panelSource.includes('loadPendingProductOnboardingRequests'),
  'project code page must have a dedicated pending request loader'
)
assert.ok(
  panelSource.includes('const loaders = [loadPendingProductOnboardingRequests()]'),
  'opening the dialog must load pending approvals before any create-only product lookup'
)
assert.ok(
  panelSource.includes('if (canCreateProductOnboardingRequest.value)'),
  'MDM product lookup must be limited to users who can create onboarding requests'
)
assert.ok(
  panelSource.includes('applyPendingProductOnboardingRequest'),
  'project code page must be able to restore an existing pending request into the approval form'
)
assert.ok(
  panelSource.includes(':disabled="Boolean(productOnboardingCreatedRequestId)"'),
  'restored pending requests must lock the form so approval does not become an edit fallback'
)

assert.ok(
  apiSource.includes('getPendingProductOnboardingRequests'),
  'frontend API must expose pending onboarding request query'
)
assert.ok(
  apiSource.includes("url: '/dcc/product-onboarding-requests/pending'"),
  'frontend API must call the formal pending onboarding endpoint'
)

assert.match(
  controllerSource,
  /@GetMapping\("\/pending"\)[\s\S]*?@PreAuthorize\("@ss\.hasPermission\('dcc:project-code:create'\) or @ss\.hasPermission\('dcc:project-code:update'\)"\)[\s\S]*?getPendingRequests\(/,
  'backend pending endpoint must allow create or update permission'
)
assert.match(
  controllerSource,
  /@PostMapping\("\/\{id:\\\\d\+\}\/approve"\)[\s\S]*?@PreAuthorize\("@ss\.hasPermission\('dcc:project-code:update'\)"\)[\s\S]*?approveRequest\(/,
  'backend approve endpoint must require update permission'
)
assert.ok(serviceSource.includes('getPendingRequests()'), 'service contract must expose pending request query')
assert.ok(
  serviceImplSource.includes('return requestMapper.selectPendingList()'),
  'service implementation must read formal pending requests'
)
assert.ok(mapperSource.includes('selectPendingList()'), 'mapper must expose pending request list query')
assert.ok(
  mapperSource.includes('DccProductOnboardingStatusConstants.PENDING_APPROVAL'),
  'pending query must filter by PENDING_APPROVAL status'
)

console.log('PASS: DCC-STATIC-016 product onboarding approval entry contract')
