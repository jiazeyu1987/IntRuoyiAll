const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const pageSource = readText('src/views/signature-governance/index.vue')
const permissionSource = readText('src/store/modules/permission.ts')

assert.match(
  pageSource,
  /const signatureGovernanceAdminTabs(?::\s*ActiveTab\[\])?\s*=\s*\[[\s\S]*'signature-records'[\s\S]*'policy'[\s\S]*\]/,
  'management/governance tabs must be modeled as a positive admin-only tab set'
)
assert.match(
  pageSource,
  /const canViewGovernanceTabs = computed/,
  'page must derive whether the current user can view governance tabs'
)
assert.match(
  pageSource,
  /router\.replace\(signatureTabRoutes\['my-signature'\]\)/,
  'ordinary users who enter the electronic-signature shell must be redirected to My Signature'
)
assert.match(
  pageSource,
  /<SignatureGovernanceRecordsPane v-if="activeTab === 'signature-records' && canViewGovernanceTabs"/,
  'ordinary users must not mount the all-signature records pane'
)
assert.match(
  pageSource,
  /<SignatureGovernanceMySignaturePane v-if="activeTab === 'my-signature'"/,
  'My Signature must remain the ordinary-user visible pane'
)
assert.doesNotMatch(
  pageSource,
  /activeTab\s*===\s*'signature-records'"\s*\/>/,
  'all-signature records must not be mounted solely from the route tab'
)

assert.match(
  permissionSource,
  /SIGNATURE_MY_SIGNATURE_ROUTE_PATH\s*=\s*'my-signature'/,
  'permission route merge must know the My Signature child path'
)
assert.match(
  permissionSource,
  /resolveSignatureGovernanceRedirect/,
  'signature shell redirect must be derived from authorized dynamic children'
)
assert.match(
  permissionSource,
  /resolveHiddenShellRedirect\(\s*staticRoute,\s*dynamicRoute,\s*mergedRoute\.redirect,\s*dynamicChildren\s*\)/,
  'hidden shell merge must pass filtered authorized children into redirect resolution'
)

const hiddenShellStart = permissionSource.indexOf('const mergeHiddenStaticShellRoute =')
const hiddenShellEnd = permissionSource.indexOf('const mergeStaticRoutesWithDynamicRoutes', hiddenShellStart)
assert.notEqual(hiddenShellStart, -1, 'hidden shell merge function must exist')
assert.notEqual(hiddenShellEnd, -1, 'hidden shell merge function must be inspectable')
const hiddenShellSource = permissionSource.slice(hiddenShellStart, hiddenShellEnd)

assert.match(
  hiddenShellSource,
  /const appendUncoveredHiddenStaticChildren = !isSignatureGovernanceShellRoute\(staticRoute\)/,
  'signature-governance must not append unauthorized hidden static children for ordinary users'
)
assert.match(
  hiddenShellSource,
  /appendUncoveredHiddenStaticChildren\s*\?\s*hiddenStaticChildren/,
  'non-signature hidden shells must keep the existing hidden-child merge behavior'
)

console.log('PASS: electronic signature ordinary users only see My Signature static contract')
