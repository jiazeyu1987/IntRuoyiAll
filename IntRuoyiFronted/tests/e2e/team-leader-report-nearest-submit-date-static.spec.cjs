const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const page = fs
  .readFileSync(path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

assert.match(
  page,
  /const\s+SUBMISSION_DEFAULT_DATE_DISCOVERY_LOOKBACK_DAYS\s*=\s*14/,
  'Production report list must bound the default-date discovery window instead of scanning indefinitely.'
)

assert.match(
  page,
  /const\s+isSubmissionDefaultDateDiscoveryContext\s*=\s*\(\)\s*=>[\s\S]*activeLeaderTab\.value === 'PRODUCTION'[\s\S]*activeProductionModuleTab\.value === 'report'[\s\S]*queryParams\.submitDate === getDefaultSubmissionDate\(\)[\s\S]*!hasNonDateSubmissionQueryParams\(\)[\s\S]*submissionMultiFilterState\.conditions\.length === 0[\s\S]*submissionMultiFilterState\.appliedConditions\.length === 0/,
  'Nearest-date discovery must only run for the default production report date with no visible or applied user filters.'
)

assert.match(
  page,
  /const\s+loadNearestSubmissionDatePage\s*=\s*async\s*\([\s\S]*for\s*\(\s*let dayOffset = 1;\s*dayOffset <= SUBMISSION_DEFAULT_DATE_DISCOVERY_LOOKBACK_DAYS;\s*dayOffset\+\+\s*\)[\s\S]*getTeamLeaderSubmissionPage\(\{[\s\S]*submitDate:\s*candidateSubmitDate[\s\S]*pageNo:\s*1[\s\S]*\}\)[\s\S]*data\.total/,
  'Nearest-date discovery must use the official submission page API, start from prior dates, and request page 1.'
)

assert.match(
  page,
  /const\s+applyDiscoveredSubmissionDate\s*=\s*\(submitDate:\s*string\)\s*=>[\s\S]*queryParams\.pageNo = 1[\s\S]*queryParams\.submitDate = submitDate/,
  'When a nearer submit date is found, only the internal query date should be synchronized.'
)

const discoveredBlock = page.slice(
  page.indexOf('const applyDiscoveredSubmissionDate = (submitDate: string) =>'),
  page.indexOf('const ensureSubmissionQueryDate')
)
assert.doesNotMatch(
  discoveredBlock,
  /updateSubmissionMultiFilterState|appliedConditions|conditions:/,
  'Nearest-date discovery must not create a visible submitDate filter.'
)

assert.match(
  page,
  /const\s+data\s*=\s*await getTeamLeaderSubmissionPage\(buildSubmissionParams\(\)\)[\s\S]*if\s*\(\s*\(data\.total \|\| 0\) === 0 && isSubmissionDefaultDateDiscoveryContext\(\)\s*\)[\s\S]*const\s+nearestPage\s*=\s*await loadNearestSubmissionDatePage\(buildSubmissionParams\(\)\)[\s\S]*applyDiscoveredSubmissionDate\(nearestPage\.submitDate\)[\s\S]*submissionList\.value = nearestPage\.data\.list \|\| \[\]/,
  'The first empty default-today response must be followed by nearest-date discovery before rendering an empty list.'
)

assert.doesNotMatch(
  page,
  /submissionList\.value\s*=\s*\[[^\]]*(mock|placeholder|demo)/i,
  'Production report list must not use mock, placeholder, or demo rows to hide an empty response.'
)

console.log('PASS: production report default date discovers nearest formal submit date')
