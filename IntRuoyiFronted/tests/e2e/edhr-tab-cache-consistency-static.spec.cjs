const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const router = read('src/router/modules/remaining.ts')

const routeBlock = (routePath) => {
  const marker = `path: '${routePath}'`
  const start = router.indexOf(marker)
  assert(start >= 0, `必须存在目标路由：${routePath}`)
  const end = router.indexOf('\n      {', start + marker.length)
  return router.slice(start, end < 0 ? router.length : end)
}

const routeContracts = [
  {
    path: 'pro/feedback/edhr-execution/form',
    name: 'MesProFeedbackEdhrExecutionForm',
    component: '@/views/mes/pro/edhr/ExecutionPage.vue'
  },
  {
    path: 'pro/feedback/edhr-domain-trace/detail',
    name: 'MesProFeedbackEdhrDomainTraceDetail',
    component: '@/views/mes/pro/edhr/DomainTraceDetailPage.vue'
  },
  {
    path: 'pro/feedback/edhr-field-audit/detail',
    name: 'MesProFeedbackEdhrFieldAuditDetail',
    component: '@/views/mes/pro/edhr/FieldAuditDetailPage.vue'
  },
  {
    path: 'pro/feedback/edhr-operation-audit',
    name: 'MesProFeedbackEdhrOperationAudit',
    component: '@/views/mes/pro/edhr/OperationAuditPage.vue'
  },
  {
    path: 'pro/feedback/edhr-approval/detail',
    name: 'MesProFeedbackEdhrApprovalDetail',
    component: '@/views/mes/pro/edhr/ApprovalDetailPage.vue'
  },
  {
    path: 'pro/feedback/edhr-batch-execution/review',
    name: 'MesProEdhrBatchExecutionReview',
    component: '@/views/mes/pro/edhr-batch/BatchExecutionReviewPage.vue'
  },
  {
    path: 'pro/feedback/edhr-batch-history',
    name: 'MesProEdhrBatchHistory',
    component: '@/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue'
  },
  {
    path: 'pro/feedback/edhr-dhr-template',
    name: 'MesProFeedbackEdhrDhrTemplate',
    component: '@/views/mes/pro/edhr-dhr-template/DhrTemplatePage.vue'
  }
]

for (const contract of routeContracts) {
  const block = routeBlock(contract.path)
  assert(block.includes(`name: '${contract.name}'`), `${contract.path} 路由名称必须保持稳定。`)
  assert(
    block.includes(`import('${contract.component}')`),
    `${contract.path} 必须使用与 keep-alive 名称匹配的页面组件。`
  )
  assert.match(block, /noCache:\s*false/, `${contract.path} 必须启用 keep-alive 缓存。`)
  assert.doesNotMatch(block, /noCache:\s*true/)
}

const execution = read('src/views/mes/pro/edhr/ExecutionPage.vue')
assert(
  execution.includes("defineOptions({ name: 'MesProFeedbackEdhrExecutionForm' })"),
  '执行表单组件名称必须与路由名称一致。'
)
for (const marker of [
  '() => [route.name, route.query.id, route.query.workTaskId] as const',
  "routeName !== 'MesProFeedbackEdhrExecutionForm'",
  'loadedExecutionContextKey.value === nextExecutionContextKey',
  'loadExecution()'
]) {
  assert(execution.includes(marker), `执行表单缓存 watcher 缺少：${marker}`)
}

const domainTrace = read('src/views/mes/pro/edhr/DomainTraceDetailPage.vue')
for (const marker of [
  '() => [route.name, route.query.executionId] as const',
  "routeName !== 'MesProFeedbackEdhrDomainTraceDetail'",
  'nextExecutionId === Number(detail.value?.executionId)',
  'loadDetail()'
]) {
  assert(domainTrace.includes(marker), `主数据追溯缓存 watcher 缺少：${marker}`)
}

const fieldAudit = read('src/views/mes/pro/edhr/FieldAuditDetailPage.vue')
for (const marker of [
  'route.name,',
  'route.query.executionId,',
  'route.query.auditBatchId,',
  'route.query.auditItemId',
  "routeName !== 'MesProFeedbackEdhrFieldAuditDetail'",
  'loadedDetailQueryKey.value === nextDetailQueryKey',
  'loadDetail()'
]) {
  assert(fieldAudit.includes(marker), `字段审计缓存 watcher 缺少：${marker}`)
}

const review = read('src/views/mes/pro/edhr-batch/BatchExecutionReviewPage.vue')
assert(
  review.includes("defineOptions({ name: 'MesProEdhrBatchExecutionReview' })") &&
    review.includes('BatchExecutionDetailPage'),
  '批次复盘必须使用具备独立 keep-alive 名称的融合壳组件。'
)

console.log('PASS: all confirmed eDHR tabs use stable keep-alive cache contracts.')
