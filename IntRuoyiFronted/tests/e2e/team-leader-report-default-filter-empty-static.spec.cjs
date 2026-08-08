const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const page = fs
  .readFileSync(path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

assert.match(
  page,
  /const\s+ensureSubmissionQueryDate\s*=\s*\(\)\s*=>[\s\S]*queryParams\.submitDate\s*=\s*currentSubmitDate/,
  'The report list may keep an internal submitDate for the required backend query.'
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
  /ensureSubmissionDateCondition\(true\)|DEFAULT_SUBMISSION_DATE_CONDITION_ID/,
  'Report management must not initialize a visible submitDate filter tab by default.'
)

assert.match(
  page,
  /async function getSubmissionList\(\) \{[\s\S]*ensureSubmissionQueryDate\(\)[\s\S]*getTeamLeaderSubmissionPage\(buildSubmissionParams\(\)\)/,
  'The first list load must ensure the internal required date without creating visible filters.'
)

assert.match(
  page,
  /const\s+resetSubmissionMultiFilter\s*=\s*async\s*\(\)\s*=>[\s\S]*clearSubmissionVisibleFilterState\(\)[\s\S]*resetSubmissionQueryParams\(leaderType\)[\s\S]*await getSubmissionList\(\)/,
  'Resetting report filters must leave the visible multi-filter state empty while reloading through the internal date query.'
)

const resetBlock = page.slice(
  page.indexOf('const resetSubmissionMultiFilter = async () =>'),
  page.indexOf('watch(activePqcModuleTab')
)
assert.doesNotMatch(
  resetBlock,
  /ensureSubmissionDateCondition|updateSubmissionMultiFilterState\(\{[\s\S]*key:\s*'submitDate'/,
  'Resetting filters must not recreate submitDate as a visible condition.'
)

assert.match(
  page,
  /const\s+applyDiscoveredSubmissionDate\s*=\s*\(submitDate:\s*string\)\s*=>[\s\S]*queryParams\.pageNo = 1[\s\S]*queryParams\.submitDate = submitDate/,
  'Nearest-date discovery may update the internal query date.'
)

const discoveredBlock = page.slice(
  page.indexOf('const applyDiscoveredSubmissionDate = (submitDate: string) =>'),
  page.indexOf('const ensureSubmissionQueryDate')
)
assert.doesNotMatch(
  discoveredBlock,
  /updateSubmissionMultiFilterState|appliedConditions|conditions:/,
  'Nearest-date discovery must not turn the internal date into a visible applied filter tab.'
)

assert.match(
  page,
  /const\s+isSubmissionDefaultDateDiscoveryContext\s*=\s*\(\)\s*=>[\s\S]*submissionMultiFilterState\.conditions\.length === 0[\s\S]*submissionMultiFilterState\.appliedConditions\.length === 0/,
  'Default-date discovery must only run when the user has no visible or applied filters.'
)

assert.match(
  page,
  /const\s+applySubmissionMultiFilter\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*await applySubmissionMultiFilterState\(\)[\s\S]*\}/,
  'Applying non-date filters must use the internal submitDate instead of requiring a visible submitDate filter.'
)

assert.doesNotMatch(
  page,
  /hasSubmissionDateCondition|提交日期是必填筛选条件/,
  'Report management must not force users to add submitDate as a visible filter when default filters are none.'
)

assert.match(
  page,
  /onMounted\(\(\) => \{[\s\S]*clearInitialSubmissionVisibleDefaultFilter\(\)[\s\S]*ensureSubmissionQueryDate\(\)[\s\S]*getSubmissionList\(\)/,
  'Initial report load must clear any legacy visible submitDate state before ensuring the internal query date.'
)

console.log('PASS: production report default multi-filter stays empty')
