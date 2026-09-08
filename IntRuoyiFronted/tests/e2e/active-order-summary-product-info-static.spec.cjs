const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const componentPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const apiPath = path.join(frontendRoot, 'src/api/mes/pro/processpool/teamLeader.ts')

const component = fs.readFileSync(componentPath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

assert.match(
  api,
  /export interface TeamLeaderActiveOrderDetailRespVO \{[\s\S]*?drawingNumber\?: string[\s\S]*?\n\}/,
  '活跃订单详情接口类型必须暴露工单图号 drawingNumber。'
)
assert.match(
  component,
  /<th>图号<\/th>\s*<td>\{\{ activeOrderWorkOrderDisplay\.drawingNumber \}\}<\/td>/,
  '总表图号必须显示生成工单的 drawingNumber，不能继续显示空值。'
)
assert.match(
  component,
  /<th>生产周期<\/th>\s*<td>\{\{ summaryProductionCycleText \}\}<\/td>/,
  '总表生产周期必须显示按生产提交时间计算出的 summaryProductionCycleText。'
)
assert.match(
  component,
  /const summaryProductionCycleText = computed\(\(\) => \{[\s\S]*?submittedAt[\s\S]*?sort\(\(left, right\) => left\.timestamp - right\.timestamp\)[\s\S]*?`[^`]*~[^`]*`/,
  'summaryProductionCycleText 必须基于生产提交 submittedAt 的最早/最晚日期计算。'
)
assert.match(
  component,
  /drawingNumber: detail\?\.drawingNumber \|\| '-'/,
  '详情页无 sourceWorkOrder 时，图号必须来自详情接口 detail.drawingNumber。'
)

console.log('PASS: active-order summary product info static contract')
