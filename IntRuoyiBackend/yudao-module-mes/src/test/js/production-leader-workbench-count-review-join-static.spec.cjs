const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const mapperPath = path.resolve(__dirname, '../../main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml')
const source = fs.readFileSync(mapperPath, 'utf8')
const countBlock = source.match(/<select id="selectTimelineCount"[\s\S]*?<\/select>/)?.[0] || ''
assert.match(countBlock, /reqVO.allocationView == 'WORKBENCH'/)
assert.match(countBlock, /LatestSubmissionReviewJoin/)
assert.match(countBlock, /reqVO.submissionReviewStatus != null or reqVO.pqcFormView != null or reqVO.allocationView == 'WORKBENCH'/)
console.log('PASS production-leader-workbench-count-review-join-static')
