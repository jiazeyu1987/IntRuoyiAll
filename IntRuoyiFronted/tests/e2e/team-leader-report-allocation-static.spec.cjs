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
assert.match(
  api,
  /erpFixedQuantitySnapshot\?:\s*number\s*\|\s*string/,
  'manual allocation shortcuts must use the formal active-order ERP fixed quantity snapshot as the order total.'
)
assert.match(
  page,
  /<el-input-number[\s\S]*v-model="row\.allocatedQuantity"[\s\S]*:precision="0"[\s\S]*:step="1"[\s\S]*@change="markManualAllocation"/,
  'allocation quantity input must be constrained to integer steps without removing manual input.'
)
assert.match(
  page,
  /data-team-leader-allocation-max[\s\S]*@click="applyAllocationShortcut\(row,\s*'MAX'\)"[\s\S]*最大/,
  'allocation table must expose a stable row-level 最大 button wired to the formal max shortcut handler.'
)
assert.match(
  page,
  /data-team-leader-allocation-half[\s\S]*@click="applyAllocationShortcut\(row,\s*'HALF'\)"[\s\S]*一半/,
  'allocation table must expose a stable row-level 一半 button wired to the formal half shortcut handler.'
)
assert.match(
  page,
  /const\s+resolveCurrentAllocationRemainingQuantity\s*=[\s\S]*reviewEvent\.value\?\.outputQuantity[\s\S]*allocatedExceptCurrent[\s\S]*currentRemainingQuantity/,
  'allocation shortcut math must derive the current remaining quantity from the report quantity minus other allocation rows.'
)
assert.match(
  page,
  /const\s+resolveAllocationShortcutQuantity\s*=[\s\S]*order\.erpFixedQuantitySnapshot[\s\S]*resolveCurrentAllocationRemainingQuantity\(line\)[\s\S]*Math\.min\(orderQuantity,\s*currentRemainingQuantity\)[\s\S]*Math\.floor\(orderQuantity\s*\/\s*2\)[\s\S]*Math\.min\(halfOrderQuantity,\s*currentRemainingQuantity\)/,
  'allocation shortcut math must cap by both the order total/half total and the current unallocated remaining quantity.'
)
assert.match(
  page,
  /const\s+requirePositiveInteger\s*=[\s\S]*Number\.isInteger\(parsed\)[\s\S]*return parsed/,
  'allocation submit validation must require positive integers instead of accepting decimals.'
)
assert.match(
  page,
  /const\s+normalizeAllocationSubmitQuantity\s*=\s*\(value:\s*unknown,\s*message:\s*string\)\s*=>\s*\{[\s\S]*String\(value\)\.trim\(\) === ''[\s\S]*return 0[\s\S]*parsed < 0[\s\S]*Number\.isInteger\(parsed\)[\s\S]*return parsed/,
  'allocation submit validation must treat blank quantities as 0 while still rejecting negative, decimal, and non-numeric values.'
)
assert.match(
  page,
  /const\s+buildAllocationSubmitLines\s*=\s*\(\):\s*TeamLeaderReportAllocationLine\[\]\s*=>\s*\{[\s\S]*normalizeAllocationSubmitQuantity\(line\.allocatedQuantity,\s*'分配数量必须为0或正整数'\)[\s\S]*allocatedQuantity === 0[\s\S]*activeOrderId === undefined[\s\S]*return \[\][\s\S]*allocatedQuantity/,
  'allocation submit lines must allow 0 or blank quantities and ignore completely blank zero rows.'
)
assert.match(
  page,
  /const\s+resolveCurrentLeaderType\s*=[\s\S]*activeLeaderTab\.value[\s\S]*leaderType !== 'PRODUCTION'[\s\S]*leaderType !== 'PQC'[\s\S]*return leaderType/,
  'allocation preview and confirm must derive leaderType from the current tab instead of mutable filter params.'
)
assert.match(
  page,
  /previewTeamLeaderReportFifoAllocation\(\{\s*eventId,\s*leaderType:\s*resolveCurrentLeaderType\(\)\s*\}\)/,
  'FIFO allocation preview must submit the current tab leaderType so backend validation never receives null from filter state.'
)
assert.match(
  page,
  /const\s+leaderType\s*=\s*resolveCurrentLeaderType\(\)/,
  'allocation confirmation must submit the current tab leaderType so backend validation never receives null from filter state.'
)
assert.match(
  page,
  /const\s+allocatableActiveOrderOptions\s*=\s*computed\(\(\)\s*=>\s*[\s\S]*normalizePositiveNumber\(order\.id\)[\s\S]*\)/,
  'manual allocation selector must expose the formal active-order list without an abnormal-order restriction.'
)
assert.doesNotMatch(
  page.match(/const\s+allocatableActiveOrderOptions[\s\S]*?\n\)/)?.[0] || '',
  /order\.abnormal/,
  'allocation candidate selection must not add an abnormal-order restriction.'
)
assert.match(
  page,
  /const\s+addAllocationLine\s*=[\s\S]*activeOrderId:\s*undefined[\s\S]*allocatedQuantity:\s*0/,
  'new manual allocation rows must start unselected instead of auto-filling a possibly invalid active order.'
)
assert.doesNotMatch(
  page,
  /reviewTeamLeaderSubmission\(\{\s*eventId,[\s\S]{0,220}reviewStatus:\s*reviewForm\.reviewStatus,[\s\S]{0,220}\}\)/,
  'production approval must not use the old review-only endpoint without allocation lines.'
)

const submitReviewStart = page.indexOf('const submitReview = async () => {')
const submitReviewEnd = page.indexOf('const openCorrection =', submitReviewStart)
assert.ok(submitReviewStart >= 0 && submitReviewEnd > submitReviewStart, 'must locate submitReview handler.')
const submitReviewBlock = page.slice(submitReviewStart, submitReviewEnd)
assert.match(
  submitReviewBlock,
  /confirmTeamLeaderReportAllocation\([\s\S]*ElMessage\.success[\s\S]*await\s+Promise\.all\(\[\s*getSubmissionList\(\),\s*loadActiveOrders\(\)\s*\]\)/,
  'after FIFO or manual allocation saves, the page must refresh both the submission list and active-order pool so production progress updates immediately.'
)

console.log('PASS: team leader report allocation static contract is wired')
