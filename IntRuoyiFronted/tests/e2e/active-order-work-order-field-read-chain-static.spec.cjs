const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const panelPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const apiPath = path.join(frontendRoot, 'src/api/mes/pro/processpool/teamLeader.ts')

const panel = fs.readFileSync(panelPath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

assert.match(
  api,
  /export interface TeamLeaderActiveOrderDetailRespVO \{[\s\S]*?demandBillNo\?: string[\s\S]*?productSpecification\?: string[\s\S]*?\n\}/,
  '活跃订单详情前端类型必须暴露生产工单生产指令 demandBillNo 和型号规格 productSpecification。'
)
assert.match(
  panel,
  /<th>生产指令<\/th>\s*<td>\{\{ activeOrderWorkOrderDisplay\.demandBillNo \}\}<\/td>/,
  '总表生产指令必须绑定生产工单 demandBillNo，不能显示空白占位。'
)
assert.doesNotMatch(
  panel,
  /<th>生产指令<\/th>\s*<td>\{\{ blankSummaryField \}\}<\/td>/,
  '总表生产指令不得继续绑定 blankSummaryField。'
)
assert.match(
  panel,
  /demandBillNo:\s*source\.demandBillNo\s*\|\|\s*'-'/,
  'sourceWorkOrder 场景必须从生产工单 demandBillNo 生成展示模型。'
)
assert.match(
  panel,
  /demandBillNo:\s*detail\?\.demandBillNo\s*\|\|\s*'-'/,
  '详情接口场景必须从 detail.demandBillNo 生成展示模型。'
)
assert.match(
  panel,
  /const activeOrderProductSpecificationText = computed\(\(\) => \{[\s\S]*activeOrderWorkOrderDisplay\.value\.productSpecification/,
  '每个生产提交的产品规格展示必须通过生产工单展示模型统一读取。'
)
assert.doesNotMatch(
  panel,
  /<span>产品规格<\/span>\s*<strong>\{\{ detail\.productSpecification \|\| '未记录' \}\}<\/strong>/,
  '生产提交里的产品规格不得绕过生产工单展示模型直接读取 detail.productSpecification。'
)

console.log('PASS: active order work-order field read-chain static contract')
