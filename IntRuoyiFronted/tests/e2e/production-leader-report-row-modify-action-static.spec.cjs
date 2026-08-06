const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const source = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

const markerIndex = source.indexOf('data-team-leader-detail-event-id')
assert.notEqual(markerIndex, -1, '报工管理行操作必须保留详情按钮稳定锚点。')
const operationStart = source.lastIndexOf('<el-table-column', markerIndex)
const operationEnd = source.indexOf('</el-table-column>', markerIndex)
assert.notEqual(operationStart, -1, '必须能定位报工管理操作列开始。')
assert.notEqual(operationEnd, -1, '必须能定位报工管理操作列结束。')
const operationColumn = source.slice(operationStart, operationEnd)

assert.match(
  operationColumn,
  /data-team-leader-correction-event-id="String\(row\.id\)"[\s\S]*@click="openCorrection\(row\)"[\s\S]*>\s*修改\s*<\/el-button>/,
  '报工管理行级第三个动作必须显示“修改”，并打开正式原始记录修改弹窗。'
)
assert.doesNotMatch(
  operationColumn,
  /标记异常|@click="prefillAbnormal\(row\)"/,
  '报工管理行操作不得继续显示“标记异常”或预填异常上报。'
)
assert.match(
  source,
  /const canCorrectSubmission = \(row: ProcessPoolTimelineEventVO\) =>\s*isProductionLeader\.value \|\| row\.submissionReviewStatus === 'REJECTED'/,
  '生产组长报工管理必须允许行级修改入口；PQC 仍只允许复核不正确后修改。'
)
assert.match(
  source,
  /if \(!canCorrectSubmission\(event\)\) \{[\s\S]*ElMessage\.error\('只有生产报工或复核不正确的提交可以修改'\)[\s\S]*return[\s\S]*\}/,
  'openCorrection 必须二次阻断不允许修改的行，并使用“修改”口径提示。'
)
assert.doesNotMatch(
  source,
  /const prefillAbnormal = \(event: ProcessPoolTimelineEventVO\) =>/,
  '旧的行级异常预填函数必须移除，避免后续误绑定。'
)
assert.match(
  source,
  /data-team-leader-abnormal-report[\s\S]*订单异常上报[\s\S]*markAndReportWorkOrderAbnormal/,
  '独立“异常”模块必须继续保留正式异常上报链路。'
)

console.log('PASS: production leader report row modify action static contract')
