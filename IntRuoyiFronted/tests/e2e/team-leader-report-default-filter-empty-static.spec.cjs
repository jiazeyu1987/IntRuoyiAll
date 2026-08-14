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
const resetBlock = page.slice(
  page.indexOf('const resetSubmissionMultiFilter = async () =>'),
  page.indexOf('watch(activePqcModuleTab')
)
const resetQueryBlock = page.slice(
  page.indexOf('const resetSubmissionQueryParams = (leaderType: TeamLeaderType) =>'),
  page.indexOf('const clearSubmissionVisibleFilterState = () =>')
)
const mountedBlock = page.match(/onMounted\(\(\) => \{[\s\S]*?\n\}\)/)?.[0] || ''

assert.match(
  buildSubmissionParamsBlock,
  /submitDate:\s*typeof queryParams\.submitDate === 'string'[\s\S]*queryParams\.submitDate\.trim\(\) \|\| undefined/,
  'The report list must only send submitDate when the user explicitly filters by submit date.'
)

assert.match(
  page,
  /const\s+clearSubmissionVisibleFilterState\s*=\s*\(\)\s*=>[\s\S]*conditions:\s*\[\],[\s\S]*appliedConditions:\s*\[\],[\s\S]*activeConditionId:\s*undefined/,
  'Report management must have an explicit visible-filter reset helper for the empty default state.'
)

assert.match(
  page,
  /const\s+clearInitialSubmissionVisibleDefaultFilter\s*=\s*\(\)\s*=>[\s\S]*submissionMultiFilterState\.appliedConditions\.length > 0[\s\S]*isDefaultSubmitDateOnly[\s\S]*clearSubmissionVisibleFilterState\(\)/,
  'Initial load must clear the legacy visible submitDate condition when it is only the old default filter.'
)

assert.doesNotMatch(
  page,
  /ensureSubmissionDateCondition\(true\)|DEFAULT_SUBMISSION_DATE_CONDITION_ID|const\s+ensureSubmissionQueryDate\s*=|loadNearestSubmissionDatePage|isSubmissionDefaultDateDiscoveryContext|applyDiscoveredSubmissionDate/,
  'Report management must not initialize, hide, or discover a submitDate filter by default.'
)

assert.doesNotMatch(
  mountedBlock,
  /ensureSubmissionQueryDate\(\)|queryParams\.submitDate\s*=\s*getDefaultSubmissionDate\(\)/,
  'The first report load must not add a hidden submitDate before querying.'
)

assert.match(
  resetBlock,
  /clearSubmissionVisibleFilterState\(\)[\s\S]*resetSubmissionQueryParams\(leaderType\)[\s\S]*await getSubmissionList\(\)/,
  'Resetting report filters must leave the visible multi-filter state empty and reload the list.'
)

assert.match(
  resetQueryBlock,
  /queryParams\.submitDate\s*=\s*getInitialSubmissionDate\(leaderType\)/,
  'Resetting filters must use the initial date helper.'
)

assert.match(
  page,
  /const\s+getInitialSubmissionDate\s*=\s*\(_leaderType:\s*TeamLeaderType\)\s*=>\s*undefined/,
  'The initial date helper must default to no submitDate so empty filters mean all dates.'
)

assert.doesNotMatch(
  resetBlock,
  /ensureSubmissionDateCondition|updateSubmissionMultiFilterState\(\{[\s\S]*key:\s*'submitDate'/,
  'Resetting filters must not recreate submitDate as a visible condition.'
)

assert.match(
  page,
  /const\s+applySubmissionMultiFilter\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*await applySubmissionMultiFilterState\(\)[\s\S]*\}/,
  'Applying user filters must use the formal multi-filter state.'
)

assert.doesNotMatch(
  page,
  /hasSubmissionDateCondition|提交日期是必填筛选条件/,
  'Report management must not force users to add submitDate as a visible filter.'
)

console.log('PASS: production report default multi-filter stays empty and all-date')
