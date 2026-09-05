const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const page = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const detailPage = read('src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue')
const detailPanel = read('src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue')
const routes = read('src/router/modules/remaining.ts')
const api = read('src/api/mes/pro/processpool/teamLeader.ts')

assert.match(
  page,
  /data-team-leader-active-order-detail[\s\S]*@click="openActiveOrderSubmissionDetail\(row\)"[\s\S]*>\s*详情\s*<\/el-button>[\s\S]*data-team-leader-remove-active-order/,
  '活跃订单详情按钮必须位于上下移与移除操作之间，并绑定当前订单。'
)
assert.match(
  page,
  /const\s+navigateActiveOrderSubmissionDetail[\s\S]*router\.push[\s\S]*MesProcessPoolActiveOrderSubmissionDetail[\s\S]*activeOrderId/,
  '详情页导航 helper 必须通过路由打开独立详情页面，并携带活跃订单ID。'
)
assert.match(
  page,
  /const\s+openActiveOrderSubmissionDetail\s*=\s*\(row:\s*TeamLeaderActiveOrderRespVO\)[\s\S]*navigateActiveOrderSubmissionDetail\(requirePositiveNumber\(row\.id/,
  '点击列表行详情必须打开当前行自身的活跃订单详情。'
)
assert.doesNotMatch(page, /data-team-leader-active-order-detail-dialog|activeOrderDetailVisible/, '工作台不得继续渲染活跃订单详情弹窗。')
assert.match(
  page,
  /navigateActiveOrderSubmissionDetail\(generatedActiveOrderId, row\.workOrderCode \|\| ''\)/,
  'Stage1 模拟完成后的自动跳转必须打开新生成测试订单，并保留来源订单提示。'
)
assert.match(
  routes,
  /path:\s*'pro\/process-pool\/active-order\/:activeOrderId\/submission-detail'[\s\S]*ActiveOrderSubmissionDetailPage\.vue[\s\S]*name:\s*'MesProcessPoolActiveOrderSubmissionDetail'/,
  '路由必须注册活跃订单提交详情独立页面。'
)
assert.match(detailPage, /data-team-leader-active-order-detail-page[\s\S]*ActiveOrderSubmissionDetailPanel/, '详情页面必须挂载详情展示面板。')
assert.ok(detailPage.includes('getTeamLeaderActiveOrderDetail(requireActiveOrderId())'), '详情页面必须由正式请求驱动。')
assert.match(
  detailPanel,
  /v-for="\(process, processIndex\) in detail\.processes"[\s\S]*应提数量[\s\S]*已提交[\s\S]*提交记录/,
  '详情必须以生产工序为分组并显示应提数量、已提交合计和提交次数。'
)
for (const label of ['提交数量', '设备', '提交人', '审核人', '提交时间']) {
  assert.match(detailPanel, new RegExp(`label="${label}"`), `每次正式生产提交明细必须显示${label}。`)
}
assert.match(detailPanel, /submission\.reviewerName\s*\|\|\s*'未审核'/, '没有正式审核记录的提交必须明确显示未审核。')
assert.match(detailPanel, /暂无一线生产提交/, '没有生产提交的工序必须保留并显示明确空态。')
assert.match(detailPanel, /暂无一线PQC提交/, '没有 PQC 提交时必须显示明确空态。')
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
