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
  /v-if="!isProductionLeader"[\s\S]*:data-team-leader-detail-event-id="String\(row\.id\)"[\s\S]*@click="openDetail\(row\)"[\s\S]*>\s*详情\s*<\/el-button>/,
  '详情按钮必须只在非生产组长列表中渲染，生产组长报工管理不得显示详情。'
)
assert.match(
  operationColumn,
  /data-team-leader-correction-event-id="String\(row\.id\)"[\s\S]*@click="openCorrection\(row\)"[\s\S]*>\s*修改\s*<\/el-button>/,
  '删除详情后生产组长报工管理必须保留修改入口。'
)
assert.match(
  operationColumn,
  /data-production-report-allocation-event-id="String\(row\.id\)"[\s\S]*@click="openAllocation\(row\)"[\s\S]*>\s*分配\s*<\/el-button>/,
  '删除详情后生产组长报工管理必须保留分配入口。'
)
assert.match(
  source,
  /data-team-leader-detail-event-id/,
  'PQC 组长详情追溯仍必须保留稳定事件锚点。'
)

console.log('PASS: production leader report hides detail action')
