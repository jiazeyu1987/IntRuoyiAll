const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '../..')
const listSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'),
  'utf8'
)
const detailSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)

function extractColumn(source, label) {
  const marker = `label="${label}"`
  const labelIndex = source.indexOf(marker)
  assert.notEqual(labelIndex, -1, `missing ${label} column`)
  const start = source.lastIndexOf('<el-table-column', labelIndex)
  const end = source.indexOf('</el-table-column>', labelIndex)
  assert(start >= 0 && end > start, `${label} column must be an expanded el-table-column`)
  return source.slice(start, end + '</el-table-column>'.length)
}

const workOrderColumn = extractColumn(listSource, '工单')
assert.match(
  workOrderColumn,
  /<el-button[\s\S]*?link[\s\S]*?type="primary"[\s\S]*?@click="openDetail\(row\)"[\s\S]*?row\.workOrderCode/,
  '工单列必须渲染可点击入口且不再携带 work-order focus。'
)

const currentProcessColumn = extractColumn(listSource, '当前工序')
assert.match(
  currentProcessColumn,
  /<el-button[\s\S]*?link[\s\S]*?type="primary"[\s\S]*?@click="openDetail\(row, 'process'\)"[\s\S]*?row\.currentProcessName/,
  '当前工序列必须渲染可点击入口并带 process focus。'
)

const routeColumn = extractColumn(listSource, '路线')
assert.match(
  routeColumn,
  /<el-button[\s\S]*?link[\s\S]*?type="primary"[\s\S]*?@click="openDetail\(row\)"[\s\S]*?row\.routeName/,
  '路线列必须渲染可点击入口且不再携带 route focus。'
)

assert.match(
  listSource,
  /type EdhrBatchExecutionDetailFocus = 'process'/,
  '列表页详情 focus 类型只保留当前工序定位。'
)
assert.match(
  listSource,
  /const openDetail = async \(row: EdhrBatchExecutionRespVO, focus\?: EdhrBatchExecutionDetailFocus\)/,
  'openDetail 必须接收字段来源 focus。'
)
assert.match(
  listSource,
  /focus === 'process'[\s\S]*currentProcessCode[\s\S]*currentProcessName/,
  '当前工序跳转必须携带工序编码或名称，供详情页选中对应工序。'
)

assert.match(
  detailSource,
  /type EdhrBatchExecutionDetailFocus = 'process' \| 'approval'/,
  '详情页必须只保留仍会触发独立展示的 focus。'
)
assert(
  !detailSource.includes('basicInfoDialogVisible') &&
    !detailSource.includes("focus === 'work-order'") &&
    !detailSource.includes("focus === 'route'"),
  '详情页不得再通过 work-order/route focus 打开基础信息弹框。'
)
assert.match(
  detailSource,
  /const applyRouteFocus = \(\) => \{[\s\S]*focus === 'process'[\s\S]*processDetailDialogVisible\.value = true[\s\S]*focus === 'approval'[\s\S]*openReleaseCheckGroup\(\)/,
  '详情页必须继续支持 process/approval 的有效焦点行为。'
)
assert.match(
  detailSource,
  /resolveRouteQueryTaskSelection[\s\S]*processCode[\s\S]*processName[\s\S]*task\.processCode[\s\S]*task\.processName/,
  '详情页必须按 query 中的当前工序编码或名称选中工序上下文。'
)
assert.match(
  detailSource,
  /await loadReviewTimeline\(requestSerial\)[\s\S]*applyRouteFocus\(\)/,
  '详情加载完复盘时间线后必须应用 query focus。'
)

console.log('PASS: edhr batch list clickable fields static contract')
