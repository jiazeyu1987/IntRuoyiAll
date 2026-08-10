const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const page = fs
  .readFileSync(path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

const buildSubmissionParamsBlock = page.match(
  /const\s+buildSubmissionParams\s*=\s*\(\):\s*TeamLeaderSubmissionPageReqVO\s*=>\s*\{[\s\S]*?\n\}/
)?.[0] || ''
const getSubmissionListBlock = page.match(
  /async function getSubmissionList\(\) \{[\s\S]*?\n\}/
)?.[0] || ''
const resetQueryBlock = page.slice(
  page.indexOf('const resetSubmissionQueryParams = (leaderType: TeamLeaderType) =>'),
  page.indexOf('const clearSubmissionVisibleFilterState = () =>')
)
const mountedBlock = page.match(/onMounted\(\(\) => \{[\s\S]*?\n\}\)/)?.[0] || ''
const productionTabWatchBlock = page.match(
  /watch\(activeProductionModuleTab,[\s\S]*?\n\}\)/
)?.[0] || ''

assert.match(
  buildSubmissionParamsBlock,
  /submitDate:\s*typeof queryParams\.submitDate === 'string'[\s\S]*queryParams\.submitDate\.trim\(\) \|\| undefined/,
  'Submission params should only include a submitDate when queryParams has one.'
)

assert.doesNotMatch(
  getSubmissionListBlock,
  /ensureSubmissionQueryDate\(\)/,
  'Default production report list loading must not inject a hidden submitDate.'
)

assert.match(
  resetQueryBlock,
  /queryParams\.submitDate\s*=\s*undefined/,
  'Resetting production report filters should clear submitDate so the default request is all dates.'
)

assert.doesNotMatch(
  resetQueryBlock,
  /getInitialSubmissionDate\(leaderType\)|getDefaultSubmissionDate\(\)/,
  'Resetting production report filters must not restore today as an internal date filter.'
)

assert.doesNotMatch(
  page,
  /const\s+ensureSubmissionQueryDate\s*=|loadNearestSubmissionDatePage|isSubmissionDefaultDateDiscoveryContext|applyDiscoveredSubmissionDate|SUBMISSION_DEFAULT_DATE_DISCOVERY_LOOKBACK_DAYS/,
  'The old hidden today/nearest-date discovery path must be removed for production report defaults.'
)

assert.match(
  buildSubmissionParamsBlock,
  /allocationView:\s*isProductionLeader\.value[\s\S]*isProductionReportHistoryTab\.value[\s\S]*'HISTORY'[\s\S]*'WORKBENCH'/,
  'Production report management and report history must keep separate allocation views.'
)

assert.doesNotMatch(
  productionTabWatchBlock,
  /queryParams\.submitDate\s*=\s*getDefaultSubmissionDate\(\)|ensureSubmissionQueryDate\(\)/,
  'Switching between report management and report history must not add a hidden date filter.'
)

assert.doesNotMatch(
  mountedBlock,
  /ensureSubmissionQueryDate\(\)/,
  'Initial mount must not add a hidden date filter before the first production report request.'
)

assert.match(
  page,
  /const\s+getInitialSubmissionDate\s*=\s*\(_leaderType:\s*TeamLeaderType\)\s*=>\s*undefined/,
  'Initial query state must default to no submitDate for production report and history.'
)

console.log('PASS: production report management and history default to all dates')
