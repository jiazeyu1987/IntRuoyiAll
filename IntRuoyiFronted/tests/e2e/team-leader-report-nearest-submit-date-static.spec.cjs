const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const page = fs
  .readFileSync(path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

const getSubmissionListBlock = page.match(
  /async function getSubmissionList\(\) \{[\s\S]*?\n\}/
)?.[0] || ''

assert.doesNotMatch(
  page,
  /SUBMISSION_DEFAULT_DATE_DISCOVERY_LOOKBACK_DAYS|loadNearestSubmissionDatePage|isSubmissionDefaultDateDiscoveryContext|applyDiscoveredSubmissionDate|shiftSubmissionDate/,
  'Production report default loading must not run nearest-date discovery after the all-date default change.'
)

assert.doesNotMatch(
  getSubmissionListBlock,
  /\(data\.total \|\| 0\) === 0[\s\S]*getTeamLeaderSubmissionPage\(\{[\s\S]*submitDate:/,
  'An empty all-date response must render as empty instead of probing prior dates with submitDate.'
)

assert.doesNotMatch(
  page,
  /submissionList\.value\s*=\s*\[[^\]]*(mock|placeholder|demo)/i,
  'Production report list must not use mock, placeholder, or demo rows to hide an empty response.'
)

console.log('PASS: production report no longer performs nearest-date fallback')
