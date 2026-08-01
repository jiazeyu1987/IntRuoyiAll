const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')

assert.match(
  api,
  /previewTeamLeaderReportFifoAllocation\s*=\s*async/,
  'team leader API must expose FIFO report allocation preview.'
)
assert.match(
  api,
  /confirmTeamLeaderReportAllocation\s*=\s*async/,
  'team leader API must expose report confirmation allocation submit.'
)
assert.match(
  api,
  /\/submission\/allocation\/preview-fifo/,
  'FIFO preview must call the formal allocation preview endpoint.'
)
assert.match(
  api,
  /\/submission\/allocation\/confirm/,
  'allocation confirmation must call the formal allocation confirmation endpoint.'
)
assert.match(
  api,
  /TeamLeaderReportAllocationLine/,
  'allocation request/response line types must be explicit and typed.'
)

assert.match(
  page,
  /data-team-leader-fifo-allocation/,
  'review dialog must expose a visible FIFO auto allocation action.'
)
assert.match(
  page,
  /data-team-leader-allocation-table/,
  'review dialog must show an editable active-order allocation table before confirmation.'
)
assert.match(
  page,
  /previewTeamLeaderReportFifoAllocation/,
  'page must load FIFO allocation preview before saving.'
)
assert.match(
  page,
  /confirmTeamLeaderReportAllocation/,
  'page must submit allocation lines through the formal confirmation endpoint.'
)
assert.match(
  page,
  /allocationMode/,
  'page must preserve whether allocation was FIFO generated or manually adjusted.'
)
assert.match(
  page,
  /getTeamLeaderActiveOrderList/,
  'manual allocation order selector must come from active orders.'
)
assert.doesNotMatch(
  page,
  /reviewTeamLeaderSubmission\(\{\s*eventId,[\s\S]{0,220}reviewStatus:\s*reviewForm\.reviewStatus,[\s\S]{0,220}\}\)/,
  'production approval must not use the old review-only endpoint without allocation lines.'
)

console.log('PASS: team leader report allocation static contract is wired')
