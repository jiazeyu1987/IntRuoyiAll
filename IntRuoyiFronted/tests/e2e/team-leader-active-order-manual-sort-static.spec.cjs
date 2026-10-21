const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const api = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts'),
  'utf8'
)

assert.match(
  api,
  /export\s+interface\s+TeamLeaderActiveOrderMoveReqVO\s*\{[\s\S]*activeOrderId:\s*number[\s\S]*direction:\s*'UP'\s*\|\s*'DOWN'/,
  '活跃订单移动请求必须只提交订单 ID 和明确的上下移动方向。'
)
assert.match(
  api,
  /export\s+const\s+moveTeamLeaderActiveOrder\s*=\s*async[\s\S]*request\.put<boolean>\([\s\S]*\/active-order\/move/,
  '前端必须通过正式 PUT 接口持久化活跃订单顺序。'
)

const activeOrderTableStart = page.indexOf('data-team-leader-active-order-list')
const activeOrderTableEnd = page.indexOf('</UnifiedListTemplate>', activeOrderTableStart)
assert.ok(activeOrderTableStart >= 0 && activeOrderTableEnd > activeOrderTableStart, '必须定位活跃订单表格边界。')
const activeOrderTable = page.slice(activeOrderTableStart, activeOrderTableEnd)
const operationColumn = activeOrderTable.match(
  /<el-table-column\s+label="操作"\s+width="\d+"\s+fixed="right">[\s\S]*?<\/el-table-column>/
)?.[0] || ''
assert.ok(operationColumn, '活跃订单表格必须保留固定操作列。')
assert.doesNotMatch(
  operationColumn,
  /data-team-leader-move-active-order-up|data-team-leader-move-active-order-down|submitMoveActiveOrder/,
  '操作列不得再显示或绑定上移、下移按钮。'
)
assert.doesNotMatch(
  page,
  /const\s+submitMoveActiveOrder|const\s+isFirstActiveOrder|const\s+isLastActiveOrder|activeOrderMoveSubmittingId|activeOrderMoveDirection/,
  '移除排序入口后不得保留无调用的页面处理器及提交状态。'
)

console.log('PASS: team leader active order sort controls removed')
