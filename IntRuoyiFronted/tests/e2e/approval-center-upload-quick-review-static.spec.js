const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const view = fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')

assert.match(
  view,
  /v-if="canReview\(row\)"[\s\S]*?@click="openReviewDialog\(row\)"[\s\S]*?\{\{\s*resolveReviewActionLabel\(row\)\s*\}\}/,
  '待办行内快速审批按钮必须复用现有审核弹窗，并通过专用文案解析显示'
)
assert.match(
  view,
  /const resolveReviewActionLabel = \(row: ApprovalTaskSummaryVO\) =>[\s\S]*?row\.moduleCode === 'DCC' \? '审批' : '审核'/,
  'DCC 上传审批行内按钮必须显示“审批”，其它统一审核任务保持“审核”'
)

const decisionLabelStart = view.indexOf(
  'const resolveDecisionActionLabel = (row: ApprovalTaskSummaryVO) =>'
)
const decisionLabelEnd = view.indexOf(
  'const resolveDecisionDetailDisabledReason = (row: ApprovalTaskSummaryVO) =>',
  decisionLabelStart
)
assert.notEqual(decisionLabelStart, -1, '缺少详情操作文案解析函数')
assert.notEqual(decisionLabelEnd, -1, '缺少详情操作文案解析函数结束标记')
const decisionLabelBlock = view.slice(decisionLabelStart, decisionLabelEnd)
assert.ok(
  decisionLabelBlock.indexOf("actions.includes('PROCESS_IN_MODULE')") <
    decisionLabelBlock.indexOf('canReview(row)'),
  'DCC 快速审批启用后，原“处理”详情入口仍必须优先显示为“处理”'
)

assert.match(view, /@click="openDecisionDetail\(row\)"/, '必须保留原“处理”详情入口')
assert.match(view, /@click="openModuleDetail\(row\)"/, '必须保留原“打开”模块详情入口')

console.log('approval center upload quick review static contract passed')

