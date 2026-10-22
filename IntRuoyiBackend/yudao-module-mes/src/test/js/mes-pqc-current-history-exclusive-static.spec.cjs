const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const mapper = fs.readFileSync(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
  ),
  'utf8'
).replace(/\r\n/g, '\n')

const currentViewBlock = mapper.match(
  /<if test="reqVO\.pqcFormView == 'CURRENT'">[\s\S]*?<\/if>/
)
assert.ok(currentViewBlock, 'PQC current view filter must exist')
assert.match(
  currentViewBlock[0],
  /pool_event\.event_type = 'PQC_INSPECTION'/,
  'PQC management must only include PQC inspection events'
)
assert.doesNotMatch(
  currentViewBlock[0],
  /latest_submission_review\.review_status\s+IS NULL[\s\S]*latest_submission_review\.review_status\s*<!\[CDATA\[<>\]\]>\s*'APPROVED'/,
  'PQC management must keep approved submissions visible before release'
)
assert.match(
  currentViewBlock[0],
  /NOT EXISTS[\s\S]*release_transaction\.release_status = 'RELEASED'/,
  'PQC management must exclude submissions after the active order is formally released'
)

const historyViewBlock = mapper.match(
  /<if test="reqVO\.pqcFormView == 'HISTORY'">[\s\S]*?<\/if>/
)
assert.ok(historyViewBlock, 'PQC history view filter must exist')
assert.match(
  historyViewBlock[0],
  /latest_submission_review\.review_status = 'APPROVED'/,
  'PQC history must include only forms whose latest review is APPROVED'
)

console.log('PASS: PQC management keeps approved active-order submissions visible and excludes released orders')
