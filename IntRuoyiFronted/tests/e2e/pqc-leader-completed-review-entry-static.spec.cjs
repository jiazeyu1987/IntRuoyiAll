const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8')

const reviewButtonBlock = source.match(
  /<el-button\s+v-if="!isProductionLeader && canReviewSubmission\(row\)"[\s\S]*?data-team-leader-review-event-id[\s\S]*?>[\s\S]*?复核[\s\S]*?<\/el-button>/
)
assert.ok(reviewButtonBlock, 'PQC review button must be guarded by canReviewSubmission(row)')

const canReviewBlock = source.match(
  /const canReviewSubmission = \(row: ProcessPoolTimelineEventVO\) =>[\s\S]*?const canCorrectSubmission/
)
assert.ok(canReviewBlock, 'canReviewSubmission guard must exist')
assert.match(
  canReviewBlock[0],
  /row\.submissionReviewStatus !== 'APPROVED'/,
  'approved PQC review rows must not expose the review action'
)
assert.match(
  canReviewBlock[0],
  /row\.processInspectionAggregationStatus !== 'AGGREGATED'/,
  'aggregated PQC review rows must not expose the review action'
)
assert.match(
  canReviewBlock[0],
  /!row\.released/,
  'released active orders must continue hiding the review action'
)

const openReviewBlock = source.match(
  /const openReview = async \(event: ProcessPoolTimelineEventVO\) =>[\s\S]*?const openAllocation/
)
assert.ok(openReviewBlock, 'openReview guard must exist')
assert.match(
  openReviewBlock[0],
  /if \(!canReviewSubmission\(event\)\) \{[\s\S]*?已完成复核的提交不能重复复核/,
  'manual review opening must reuse canReviewSubmission and show a deterministic message'
)

console.log('PASS: completed PQC review rows hide repeat review entry')
