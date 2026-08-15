const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const timelineApi = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/processpool/index.ts'),
  'utf8'
)
const allocationApi = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts'),
  'utf8'
)

assert.match(
  page,
  /data-team-leader-report-overage[\s\S]*待调整/,
  '生产组长报工管理必须用稳定标识展示红色待调整数量'
)
assert.match(
  timelineApi,
  /interface\s+ProcessPoolTimelineReportAllocationVO[\s\S]{0,500}overageQuantity:\s*number[\s\S]{0,200}needsAdjustment:\s*boolean/,
  '报工管理列表类型必须消费后端订单级超量状态'
)
assert.match(
  timelineApi,
  /reportAllocations:\s*ProcessPoolTimelineReportAllocationVO\[\]/,
  '报工管理列表必须把正式分配投影定义为必需字段'
)
assert.match(
  allocationApi,
  /interface\s+TeamLeaderReportAllocationLine[\s\S]{0,600}overageQuantity\?:\s*number\s*\|\s*string[\s\S]{0,200}needsAdjustment\?:\s*boolean/,
  '当前分配快照类型必须消费后端订单级超量状态'
)
assert.match(
  page,
  /data-team-leader-allocation-overage/,
  '分配明细中必须明确显示仍需调整的超出数量'
)

const reportOverageStart = page.indexOf('const resolveProductionReportOverageQuantity =')
const reportOverageEnd = page.indexOf('\nconst allocationTotalQuantity', reportOverageStart)
assert.ok(reportOverageStart >= 0 && reportOverageEnd > reportOverageStart, '必须找到列表超量计算函数')
const reportOverageBlock = page.slice(reportOverageStart, reportOverageEnd)
assert.match(reportOverageBlock, /needsAdjustment[\s\S]*overageQuantity/,
  '列表红色标识必须直接使用后端订单级超量状态')
assert.doesNotMatch(reportOverageBlock, /reportUnallocatedQuantity|erpFixedQuantitySnapshot|outputQuantity\s*-|findReportSelectedActiveOrder/,
  '列表不得再用未分配量或订单总量猜测超量')

assert.match(
  page,
  /v-for="item in row\.reportAllocations"[\s\S]{0,400}:type="item\.needsAdjustment \? 'danger'/,
  '超量订单本身必须在分配订单列显示为红色'
)
assert.doesNotMatch(
  page,
  /prefillSelectedOrderAllocation\(event,\s*snapshot\)/,
  '分配弹窗必须直接展示后端当前分配，不能再用前端预填冒充已保存数据'
)
assert.doesNotMatch(
  page,
  /row\.reportAllocations\s*\|\|\s*\[\]|event\.reportAllocations\s*\|\|\s*\[\]|snapshot\.lines\s*\|\|\s*\[\]|preview\.lines\s*\|\|\s*\[\]/,
  '正式分配投影或快照缺失时必须失败，不能静默降级为空分配'
)

console.log('PASS: team leader production report overage highlight static contract')

