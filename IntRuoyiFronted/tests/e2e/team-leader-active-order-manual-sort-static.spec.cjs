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
assert.match(operationColumn, /data-team-leader-move-active-order-up/, '操作列必须提供上移按钮。')
assert.match(operationColumn, /data-team-leader-move-active-order-down/, '操作列必须提供下移按钮。')
assert.match(operationColumn, /aria-label="上移"/, '上移图标按钮必须有可访问名称。')
assert.match(operationColumn, /aria-label="下移"/, '下移图标按钮必须有可访问名称。')
assert.match(
  operationColumn,
  /:disabled="isFirstActiveOrder\(row\)\s*\|\|\s*activeOrderMoveSubmittingId\s*!==\s*undefined"/,
  '首条活跃订单必须禁用上移，提交期间不得重复移动。'
)
assert.match(
  operationColumn,
  /:disabled="isLastActiveOrder\(row\)\s*\|\|\s*activeOrderMoveSubmittingId\s*!==\s*undefined"/,
  '末条活跃订单必须禁用下移，提交期间不得重复移动。'
)
assert.match(operationColumn, /@click="submitMoveActiveOrder\(row,\s*'UP'\)"/, '上移必须绑定 UP 写请求。')
assert.match(operationColumn, /@click="submitMoveActiveOrder\(row,\s*'DOWN'\)"/, '下移必须绑定 DOWN 写请求。')

const moveHandler = page.match(
  /const\s+submitMoveActiveOrder\s*=\s*async[\s\S]*?(?=\nconst\s+submitRemoveActiveOrder)/
)?.[0] || ''
assert.ok(moveHandler, '页面必须实现独立的活跃订单移动处理器。')
assert.match(moveHandler, /await\s+moveTeamLeaderActiveOrder\(/, '移动处理器必须调用正式持久化接口。')
assert.match(moveHandler, /writeCompleted\s*=\s*true[\s\S]*await\s+loadActiveOrders\(\)/, '写成功状态必须先于正式列表刷新。')
assert.match(moveHandler, /排序已保存，但列表刷新失败/, '写成功后刷新失败必须显示分层错误。')
assert.doesNotMatch(moveHandler, /\.splice\(|\.sort\(/, '不得仅在前端本地数组中伪造持久化顺序。')

console.log('PASS: team leader active order manual sort static contract')
