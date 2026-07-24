const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const viewPath = path.join(root, 'src/views/approval-center/index.vue')
const apiPath = path.join(root, 'src/api/approval-center/index.ts')

const view = fs.readFileSync(viewPath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

assert.match(view, /<el-button[\s\S]*?>\s*审核\s*<\/el-button>/, '待办列表操作列必须提供“审核”按钮')
assert.match(view, /openReviewDialog\(row\)/, '审核按钮必须打开审批中心审核弹窗')
assert.match(view, /v-model="reviewDialogVisible"/, '审核弹窗必须由 reviewDialogVisible 控制')
assert.match(view, /reviewForm\.result/, '审核弹窗必须提供通过/不通过选择')
assert.match(view, /reviewForm\.reason/, '审核弹窗必须提供不通过原因输入')
assert.match(view, /reviewForm\.result === 'REJECT'[\s\S]*?reviewForm\.reason\.trim\(\)/, '选择不通过时必须校验原因非空')

assert.match(api, /reviewApprovalTask/, '审批中心 API 必须暴露统一审核提交方法')
assert.match(api, /url:\s*['"]\/approval-center\/tasks\/review['"]/, '统一审核提交必须调用 /approval-center/tasks/review')
assert.match(api, /ApprovalTaskReviewReqVO/, '统一审核提交必须声明请求契约类型')

assert.match(view, /formatApprovalTime\(row\.taskCreatedAt \|\| row\.initiatedAt\)/, '时间列必须通过 formatApprovalTime 格式化创建时间')
assert.match(view, /formatApprovalTime\(row\.taskCompletedAt\)/, '时间列必须通过 formatApprovalTime 格式化完成时间')
assert.doesNotMatch(view, /\{\{\s*row\.taskCreatedAt \|\| row\.initiatedAt \|\| '--'\s*\}\}/, '时间列不得直接渲染原始毫秒时间戳')
assert.match(view, /YYYY-MM-DD HH:mm:ss/, '审批中心时间格式必须精确到秒')

console.log('approval center review action static contract passed')
