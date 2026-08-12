const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

assert.match(
  page,
  /data-team-leader-report-overage[\s\S]*待调整/,
  '生产组长报工管理必须用稳定标识展示红色待调整数量'
)
assert.match(
  page,
  /resolveProductionReportOverageQuantity[\s\S]*reportAllocations[\s\S]*outputQuantity/,
  '超报数量必须按正式报工和现有分配结果派生'
)
assert.match(
  page,
  /prefillSelectedOrderAllocation[\s\S]*event\.workOrderId[\s\S]*event\.outputQuantity/,
  '打开分配时必须按一线员工选中的订单预填本次全部报工数量'
)
assert.match(
  page,
  /data-team-leader-allocation-overage/,
  '分配明细中必须明确显示仍需调整的超出数量'
)

console.log('PASS: team leader production report overage highlight static contract')

