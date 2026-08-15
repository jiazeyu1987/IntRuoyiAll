const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const correctionMarkerIndex = source.indexOf('data-team-leader-correction-event-id')
assert.notEqual(correctionMarkerIndex, -1, '报工管理行操作必须保留修改按钮稳定锚点。')
const operationStart = source.lastIndexOf('<el-table-column', correctionMarkerIndex)
const operationEnd = source.indexOf('</el-table-column>', correctionMarkerIndex)
assert.notEqual(operationStart, -1, '必须能定位报工管理操作列开始。')
assert.notEqual(operationEnd, -1, '必须能定位报工管理操作列结束。')
const operationColumn = source.slice(operationStart, operationEnd)

assert.match(
  operationColumn,
  /v-if="!isProductionLeader && canReviewSubmission\(row\)"[\s\S]*:data-team-leader-review-event-id="String\(row\.id\)"[\s\S]*@click="openReview\(row\)"[\s\S]*>\s*复核\s*<\/el-button>/,
  '生产组长报工管理不得显示复核按钮，PQC 待复核入口仍须保留。'
)

for (const action of [
  ['data-team-leader-correction-event-id', '修改'],
  ['data-production-report-allocation-event-id', '分配']
]) {
  assert.match(
    operationColumn,
    new RegExp(`${action[0]}[\\s\\S]*>\\s*${action[1]}\\s*<\\/el-button>`),
    `删除复核按钮后必须保留“${action[1]}”行操作。`
  )
}

assert.match(
  operationColumn,
  /v-if="!isProductionLeader"[\s\S]*:data-team-leader-detail-event-id="String\(row\.id\)"[\s\S]*@click="openDetail\(row\)"[\s\S]*>\s*详情\s*<\/el-button>/,
  '生产组长报工管理不得显示详情按钮；PQC 详情追溯入口仍须保留。'
)

console.log('PASS: production leader report hides review action')
