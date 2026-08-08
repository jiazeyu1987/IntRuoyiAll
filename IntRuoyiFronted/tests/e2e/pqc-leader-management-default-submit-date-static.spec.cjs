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
  /const\s+ensureSubmissionQueryDate\s*=\s*\(\)\s*=>[\s\S]*queryParams\.submitDate\s*=\s*currentSubmitDate/,
  'PQC submission list must keep an internal submitDate for the required backend query.'
)
assert.doesNotMatch(
  page,
  /DEFAULT_SUBMISSION_DATE_CONDITION_ID|ensureSubmissionDateCondition\(true\)/,
  'PQC submission list must not create a visible submitDate filter tab by default.'
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
  /async function getSubmissionList\(\) \{[\s\S]*ensureSubmissionQueryDate\(\)[\s\S]*getTeamLeaderSubmissionPage\(buildSubmissionParams\(\)\)/,
  'PQC submission list must ensure the internal required date when loading.'
)
assert.match(
  page,
  /const\s+resetSubmissionQueryParams[\s\S]*queryParams\.submitDate\s*=\s*getDefaultSubmissionDate\(\)/,
  'Resetting the submission query must restore today instead of clearing the required submitDate.'
)
assert.match(
  page,
  /watch\(\s*activePqcModuleTab[\s\S]*tab === 'management'[\s\S]*await getSubmissionList\(\)/,
  'Switching to PQC管理 must load the submission list through the internal required date.'
)
assert.doesNotMatch(
  page,
  /queryParams\.submitDate\s*=\s*''/,
  'PQC submission code must not clear required submitDate to an empty string before loading.'
)

console.log('PASS: PQC管理 keeps internal submitDate while default filters stay empty')
