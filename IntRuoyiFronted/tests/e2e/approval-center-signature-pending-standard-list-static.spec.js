const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const approvalPage = read('src/views/approval-center/index.vue')
const routerModule = read('src/router/modules/remaining.ts')
const approvalApi = read('src/api/approval-center/index.ts')
const profileWorkbench = read('src/views/Profile/components/ProfileWorkbench.vue')

assert.doesNotMatch(
  routerModule,
  /path:\s*'signature-pending'|ApprovalCenterSignaturePending|title:\s*'签名待处理'/,
  '审批中心前端不应再暴露“签名待处理”独立二级菜单，签名任务必须合并到待办。'
)
assert.doesNotMatch(
  approvalPage,
  /<el-tab-pane label="签名待处理"|name="signature-pending"|approval\.center\.signaturePending|SIGNATURE_PENDING:\s*useUserTableColumns|SIGNATURE_PENDING:\s*useTableQuickFilter/,
  '审批中心页面不应再保留“签名待处理”页签、独立表格 key 或独立快捷筛选状态。'
)
assert.doesNotMatch(
  approvalApi,
  /'SIGNATURE_PENDING'/,
  '审批中心前端 API 类型不应继续暴露“签名待处理”独立视图，签名任务统一通过 TODO 待办查询。'
)
assert.doesNotMatch(
  profileWorkbench,
  /SIGNATURE_PENDING/,
  '个人工作台不得继续跳转或声明“签名待处理”独立视图。'
)
assert.match(
  approvalPage,
  /TODO:\s*'approval\.center\.todo\.applicant\.v1'/,
  '签名待处理并入后，待办仍必须保留稳定表格 key。'
)

console.log('PASS: approval center signature tasks are folded into TODO static contract')
