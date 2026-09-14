const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/mes/pro/processpool/teamLeader.ts')
const page = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

for (const token of [
  'quantityConflict?: boolean',
  'overageQuantity?: number | string',
  'quantityConflictProcessCount?: number',
  'hasQuantityConflict?: boolean'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API 类型必须暴露 ${token}`)
}

assert.match(
  page,
  /:row-class-name="resolveActiveOrderRowClassName"/,
  '活跃订单列表必须按后端 quantityConflict 字段整行标红。'
)
assert.match(
  page,
  /data-team-leader-active-order-quantity-conflict/,
  '活跃订单列表必须展示数量冲突提示，避免只靠颜色传达状态。'
)
assert.match(
  page,
  /class="team-leader-workbench__active-order-process-detail"[\s\S]*:class="\{ 'is-quantity-conflict': process\.quantityConflict \}"/,
  '工序详情卡片必须在同工单同工序超量时标红。'
)
assert.match(
  page,
  /:row-class-name="resolveActiveOrderSubmissionRowClassName"/,
  '工序提交明细表必须把同一冲突工序下的全部提交记录标红。'
)
assert.match(
  page,
  /row\.hasQuantityConflict[\s\S]*'生产数量冲突未解决，需先由组长纠错'/,
  '申请完工/放行前端按钮必须在数量冲突未解决时禁用并给出明确原因。'
)

console.log('PASS: production report overage conflict static contract')
