const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const page = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = read('src/api/mes/pro/processpool/teamLeader.ts')

assert.match(
  page,
  /data-team-leader-active-order-detail[\s\S]*@click="openActiveOrderSubmissionDetail\(row\)"[\s\S]*>\s*详情\s*<\/el-button>[\s\S]*data-team-leader-remove-active-order/,
  '活跃订单详情按钮必须位于上下移与移除操作之间，并绑定当前订单。'
)
assert.match(
  page,
  /data-team-leader-active-order-detail-dialog[\s\S]*v-loading="activeOrderDetailLoading"[\s\S]*activeOrderDetailError[\s\S]*retryActiveOrderSubmissionDetail[\s\S]*activeOrderSubmissionDetail\.processes/,
  '详情对话框必须由正式请求驱动，并覆盖加载、失败重试和工序数据状态。'
)
assert.match(
  page,
  /v-for="process in activeOrderSubmissionDetail\.processes"[\s\S]*应提数量[\s\S]*已提交[\s\S]*提交记录/,
  '详情必须以工序为一级分组并显示应提数量、已提交合计和提交次数。'
)
for (const label of ['提交数量', '提交人', '审核人', '提交时间']) {
  assert.match(page, new RegExp(`label="${label}"`), `每次正式提交明细必须显示${label}。`)
}
assert.match(
  page,
  /submission\.reviewerName\s*\|\|\s*'未审核'/,
  '没有正式审核记录的提交必须明确显示未审核。'
)
assert.match(page, /暂无提交记录/, '没有提交的工序必须保留并显示明确空态。')
assert.match(
  page,
  /const\s+openActiveOrderSubmissionDetail\s*=\s*async[\s\S]*activeOrderDetailVisible\.value\s*=\s*true[\s\S]*await\s+loadActiveOrderSubmissionDetail/,
  '点击详情必须先打开对话框，再加载当前活跃订单的正式详情。'
)
assert.match(
  api,
  /export interface TeamLeaderActiveOrderDetailRespVO[\s\S]*processes:\s*TeamLeaderActiveOrderProcessDetailRespVO\[\]/,
  '前端 API 必须声明按工序分组的详情响应。'
)
assert.match(
  api,
  /getTeamLeaderActiveOrderDetail[\s\S]*\/mes\/pro\/process-pool\/team-leader\/active-order\/detail[\s\S]*activeOrderId/,
  '前端必须调用正式活跃订单详情接口。'
)

console.log('PASS: production leader active-order process submission detail static contract')
