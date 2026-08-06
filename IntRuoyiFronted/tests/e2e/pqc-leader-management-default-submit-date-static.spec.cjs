const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8')

assert.match(
  page,
  /formatDateTimeValue[\s\S]*formatDate[\s\S]*from '@\/utils\/formatTime'/,
  'PQC submission list should use the shared date formatter to build an API-compatible YYYY-MM-DD default date.'
)
assert.match(
  page,
  /const\s+DEFAULT_SUBMISSION_DATE_CONDITION_ID\s*=\s*'submitDate'/,
  'PQC submission list must use a stable visible submitDate condition id.'
)
assert.match(
  page,
  /const\s+getDefaultSubmissionDate\s*=\s*\(\)\s*=>\s*formatDate\(new Date\(\),\s*'YYYY-MM-DD'\)/,
  'PQC submission list must default the required submitDate to today.'
)
assert.match(
  page,
  /submitDate:\s*getDefaultSubmissionDate\(\)/,
  'PQC submission query params must initialize submitDate so the first load can reach the backend.'
)
assert.match(
  page,
  /const\s+ensureSubmissionDateCondition[\s\S]*key:\s*'submitDate'[\s\S]*operator:\s*'eq'[\s\S]*updateSubmissionMultiFilterState\(\{[\s\S]*conditions:\s*nextConditions/,
  'PQC submission list must keep the default submitDate as a visible multi-filter condition.'
)
assert.match(
  page,
  /const\s+resetSubmissionQueryParams[\s\S]*queryParams\.submitDate\s*=\s*getDefaultSubmissionDate\(\)/,
  'Resetting the submission query must restore today instead of clearing the required submitDate.'
)
assert.match(
  page,
  /watch\(\s*activePqcModuleTab[\s\S]*tab === 'management'[\s\S]*ensureSubmissionDateCondition\(\)[\s\S]*await getSubmissionList\(\)/,
  'Switching to PQC管理 must make the required date visible and load the submission list.'
)
assert.doesNotMatch(
  page,
  /queryParams\.submitDate\s*=\s*''/,
  'PQC submission code must not clear required submitDate to an empty string before loading.'
)

console.log('PASS: PQC管理 defaults required submitDate and loads visible submissions')
