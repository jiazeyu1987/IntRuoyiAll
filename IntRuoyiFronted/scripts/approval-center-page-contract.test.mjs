import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import assert from 'node:assert/strict'

const root = process.cwd()

const apiPath = join(root, 'src/api/approval-center/index.ts')
const pagePath = join(root, 'src/views/approval-center/index.vue')
const routerPath = join(root, 'src/router/modules/remaining.ts')

assert.equal(existsSync(apiPath), true, 'approval center API module must exist')
assert.equal(existsSync(pagePath), true, 'approval center page must exist')
assert.equal(existsSync(routerPath), true, 'remaining route module must exist')

const api = readFileSync(apiPath, 'utf8')
const page = readFileSync(pagePath, 'utf8')
const router = readFileSync(routerPath, 'utf8')

for (const expected of [
  '/approval-center/modules',
  '/approval-center/tasks/page',
  'ApprovalTaskSummaryVO',
  'ApprovalTaskViewType',
  'businessDeleted',
  'decisionDetailRoute',
  'decisionDetailQuery',
  'getApprovalCenterModules',
  'getApprovalTaskPage'
]) {
  assert.ok(api.includes(expected), `approval center API must contain ${expected}`)
}

for (const label of ['待办', '已办', '我发起的', '抄送我的', '签名待处理']) {
  assert.ok(page.includes(label), `approval center page must expose ${label} tab`)
}

for (const expected of [
  'getApprovalCenterModules',
  'getApprovalTaskPage',
  'detailRoute',
  'detailQuery',
  'decisionDetailRoute',
  'decisionDetailQuery',
  'openDecisionDetail',
  'canOpenDecisionDetail',
  '详情',
  'businessDeleted',
  '已删除',
  'ElMessage.error',
  '<el-table',
  '<Pagination',
  'approvalTabNames',
  'resolveRouteTab',
  '未知审批中心子页签'
]) {
  assert.ok(page.includes(expected), `approval center page must contain ${expected}`)
}

assert.ok(router.includes("path: '/approval-center'"), 'approval center direct route must exist')
assert.ok(router.includes("redirect: '/approval-center/todo'"), 'approval center root route must redirect to todo child route')
assert.ok(router.includes("title: '审批中心'"), 'approval center route title must be 审批中心')
for (const childPath of ['todo', 'done', 'my-initiated', 'cc', 'signature-pending']) {
  assert.ok(router.includes(`path: '${childPath}'`), `approval center must define child route ${childPath}`)
}
assert.ok(router.includes("name: 'ApprovalCenterTodo'"), 'approval center todo child route must exist')
assert.ok(router.includes("permission: ['bpm:task:query']"), 'approval center route must use Phase 1 BPM query permission')

console.log('approval-center page contract passed')
