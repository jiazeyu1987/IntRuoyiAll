const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const contractPath = 'docs/edhr/existing-edhr-frontend-contract.md'

const read = (relativePath) => {
  const absolutePath = path.resolve(root, relativePath)
  assert(fs.existsSync(absolutePath), `${relativePath} must exist`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertIncludes = (source, needle, message) => {
  assert(source.includes(needle), message)
}

const contract = read(contractPath)

for (const heading of [
  '# eDHR 前端现有合约盘点',
  '## 可复用对象',
  '## 需新增对象',
  '## 禁止改写对象',
  '## 后续 Coding 门禁'
]) {
  assertIncludes(contract, heading, `frontend contract must contain ${heading}`)
}

for (const gap of [
  '初始化与 DHR 模板',
  '独立表单与记录本',
  '流转单',
  '标签',
  '打印管理',
  '放行前检查',
  '报表与看板',
  'CSV/OQ/PQ'
]) {
  assertIncludes(contract, gap, `frontend contract must list missing module ${gap}`)
}

for (const boundary of [
  '不得删除现有 eDHR API 适配层',
  '不得绕过菜单权限和 v-hasPermi',
  '不得用空白页面、默认 0 或静默失败当成验收通过',
  '缺真实路由、菜单、权限或样本时必须 fail fast'
]) {
  assertIncludes(contract, boundary, `frontend contract must declare boundary ${boundary}`)
}

const apiExpectations = {
  'src/api/mes/pro/edhr/batchExecution.ts': [
    "const BATCH_EXECUTION_BASE_URL = '/mes/pro/edhr-batch-execution'",
    "const BATCH_ARCHIVE_BASE_URL = '/mes/pro/edhr-batch-execution-archive'",
    '/page',
    '/open-or-create',
    '/task/open',
    '/close',
    '/quality-reject'
  ],
  'src/api/mes/pro/edhr/change.ts': [
    '/mes/pro/edhr-change/void-execution/request',
    '/mes/pro/edhr-change/reopen-batch/request',
    '/mes/pro/edhr-change/reopen-execution/request',
    '/mes/pro/edhr-change/supplement/request',
    '/mes/pro/edhr-change/page'
  ],
  'src/api/mes/pro/edhr/workTask.ts': [
    '/mes/pro/edhr-work-task/my-page',
    '/mes/pro/edhr-work-task/done-page',
    '/mes/pro/edhr-work-task/candidate-todo-page',
    '/mes/pro/edhr-work-task/route-archive-rule',
    '/mes/pro/edhr-work-task/candidate-signature/complete'
  ],
  'src/api/mes/pro/edhr/permission.ts': [
    '/mes/pro/edhr-permission-scopes/get',
    '/mes/pro/edhr-permission-scopes/save',
    '/mes/pro/edhr-permission-scopes/evaluate'
  ],
  'src/api/mes/pro/edhr/operationAudit.ts': [
    '/mes/pro/edhr-operation-audit/page',
    '/mes/pro/edhr-operation-audit/'
  ],
  'src/api/mes/pro/edhr/fieldAudit.ts': [
    '/mes/pro/batch-record-execution/field-audit/save-changes',
    '/mes/pro/batch-record-execution/field-audit/page',
    '/mes/pro/batch-record-execution/field-audit/detail',
    '/mes/pro/batch-record-execution/field-audit/verify-chain'
  ],
  'src/api/mes/pro/edhr/domainTrace.ts': [
    "const EDHR_DOMAIN_TRACE_BASE_URL = '/mes/pro/batch-record-execution/domain-trace'",
    '/detail',
    '/page',
    '/verify'
  ],
  'src/api/mes/pro/edhr/archive.ts': [
    '/mes/pro/batch-record-execution-archive/generate',
    '/mes/pro/batch-record-execution-archive/page',
    '/mes/pro/batch-record-execution-archive/latest',
    '/mes/pro/batch-record-execution-archive/download'
  ],
  'src/api/mes/pro/edhr/approval.ts': [
    '/mes/pro/batch-record-execution/approval-pending-page',
    '/mes/pro/batch-record-execution/approval-done-page',
    '/mes/pro/batch-record-execution/approval-detail',
    '/mes/pro/batch-record-execution/approve',
    '/mes/pro/batch-record-execution/reject'
  ],
  'src/api/mes/pro/edhr/tracking.ts': [
    '/mes/pro/batch-record-execution/tracking-page',
    '/mes/pro/batch-record-execution/tracking-timeline'
  ],
  'src/api/mes/pro/edhr/signatures.ts': [
    '/mes/pro/batch-record-execution/signature-page'
  ],
  'src/api/mes/pro/edhr/attachment.ts': [
    '/mes/pro/batch-record-execution/attachment/prepare-upload'
  ]
}

for (const [relativePath, fragments] of Object.entries(apiExpectations)) {
  const source = read(relativePath)
  for (const fragment of fragments) {
    assertIncludes(source, fragment, `${relativePath} must keep ${fragment}`)
  }
}

for (const pagePath of [
  'src/views/mes/pro/edhr/ExecutionPage.vue',
  'src/views/mes/pro/edhr/ApprovalPage.vue',
  'src/views/mes/pro/edhr/ApprovalDetailPage.vue',
  'src/views/mes/pro/edhr/TrackingPage.vue',
  'src/views/mes/pro/edhr/SignaturePage.vue',
  'src/views/mes/pro/edhr/RecordChangePage.vue',
  'src/views/mes/pro/edhr/FieldAuditPage.vue',
  'src/views/mes/pro/edhr/FieldAuditDetailPage.vue',
  'src/views/mes/pro/edhr/DomainTracePage.vue',
  'src/views/mes/pro/edhr/DomainTraceDetailPage.vue',
  'src/views/mes/pro/edhr/OperationAuditPage.vue',
  'src/views/mes/pro/edhr/PermissionMatrixPage.vue',
  'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue',
  'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue',
  'src/views/mes/pro/edhr-batch/BatchExecutionReviewPage.vue',
  'src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue',
  'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue'
]) {
  read(pagePath)
}

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
for (const permission of [
  'mes:pro-batch-record-execution:update',
  'mes:pro-batch-record-execution:field-audit-update'
]) {
  assertIncludes(executionPage, permission, `ExecutionPage must keep permission gate ${permission}`)
}

const batchDetailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
for (const permission of [
  'mes:pro-edhr-batch-execution:close',
  'mes:pro-edhr-batch-execution-archive:create',
  'mes:pro-edhr-batch-execution:quality-reject',
  'mes:pro-edhr-change:reopen'
]) {
  assertIncludes(batchDetailPage, permission, `BatchExecutionDetailPage must keep permission gate ${permission}`)
}

console.log('PASS: eDHR existing frontend contract static inventory')
