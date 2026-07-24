const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const approvalCenter = fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')

assert.match(
  approvalCenter,
  /const resolveDecisionDetailRoute = \(row: ApprovalTaskSummaryVO\) => \{[\s\S]*?return row\.decisionDetailRoute \|\| row\.detailRoute[\s\S]*?\}/,
  '审批中心主详情入口必须在业务详情缺失时仍打开 BPM 流程详情，不能把 BPM-only 行禁用。'
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
