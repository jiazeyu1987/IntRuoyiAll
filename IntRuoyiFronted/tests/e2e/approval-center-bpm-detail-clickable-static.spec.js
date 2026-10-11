const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const approvalCenter = fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')

assert.match(
  approvalCenter,
  /const resolveDecisionDetailRoute = \(row: ApprovalTaskSummaryVO\) => \{[\s\S]*?return row\.decisionDetailRoute[\s\S]*?return row\.moduleCode !== 'BPM' \? row\.detailRoute : undefined/,
  'BPM 业务详情只能使用正式决策入口；其它模块保留原详情入口。'
)

assert.doesNotMatch(
  approvalCenter,
  /resolveDecisionDetailRoute[\s\S]{0,260}isBpmProcessDetailOnly\(row\) \? '' : row\.detailRoute/,
  '审批中心主详情入口不得再用空字符串禁用 BPM 流程详情。'
)

assert.doesNotMatch(
  approvalCenter,
  /该 BPM 审批未返回业务详情入口，请补齐 provider 映射/,
  '待办列表详情不可因 provider 映射缺失显示不可点击提示。'
)

console.log('approval center BPM detail clickable static contract passed')
