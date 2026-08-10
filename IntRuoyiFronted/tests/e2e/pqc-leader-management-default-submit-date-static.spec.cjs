const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/processpool/index.ts')
const page = fs.readFileSync(pagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

assert.match(
  api,
  /submitDate\?:\s*string/,
  'The timeline API contract must allow submitDate to be omitted.'
)
assert.match(
  page,
  /const\s+getInitialSubmissionDate\s*=\s*\(_leaderType:\s*TeamLeaderType\)\s*=>\s*undefined/,
  'PQC and production submission state must start without a hidden date.'
)
assert.doesNotMatch(
  page,
  /if\s*\(!queryParams\.submitDate\)\s*\{[\s\S]*提交日期不能为空/,
  'Building PQC submission params must not reject an omitted submitDate.'
)
assert.match(
  page,
  /submitDate:\s*getInitialSubmissionDate\(activeLeaderTab\.value\)/,
  'Initial PQC query params must derive an empty date from the current leader type.'
)
assert.match(
  page,
  /submitDate:\s*typeof queryParams\.submitDate === 'string'\s*\?\s*queryParams\.submitDate\.trim\(\) \|\| undefined\s*:\s*undefined/,
  'The formal request must omit an empty submitDate instead of replacing it with today.'
)
assert.doesNotMatch(
  page,
  /const\s+ensureSubmissionQueryDate\s*=|queryParams\.submitDate\s*=\s*getDefaultSubmissionDate\(\)/,
  'No leader submission list may restore a hidden default submitDate.'
)
assert.match(
  page,
  /const\s+resetSubmissionQueryParams[\s\S]*queryParams\.submitDate\s*=\s*getInitialSubmissionDate\(leaderType\)/,
  'Resetting filters must remove the date for default all-date queries.'
)
assert.match(
  page,
  /watch\(\s*activePqcModuleTab[\s\S]*tab === 'management'[\s\S]*await getSubmissionList\(\)/,
  'Switching to PQC管理 must load the unfiltered historical list.'
)
assert.doesNotMatch(
  page,
  /DEFAULT_SUBMISSION_DATE_CONDITION_ID|ensureSubmissionDateCondition\(true\)/,
  'PQC submission list must not create a visible submitDate filter tab by default.'
)

console.log('PASS: PQC管理 omits submitDate while default filters stay empty')
