const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const auditTab = read('src/views/mes/pro/edhr/form-trace/FormTraceAuditTab.vue')
const changeTab = read('src/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue')
const releaseTab = read('src/views/mes/pro/edhr/form-trace/FormTraceReleaseTab.vue')
const fieldAuditPage = read('src/views/mes/pro/edhr/FieldAuditPage.vue')
const traceDrawerPath = path.join(
  repoRoot,
  'src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue'
)

assert.ok(
  fs.existsSync(traceDrawerPath),
  '表单追溯必须提供可复用的批次执行追溯抽屉组件，避免三个 tab 各自重复实现。'
)
const traceDrawer = fs.readFileSync(traceDrawerPath, 'utf8')

for (const [label, source] of [
  ['审计 tab', auditTab],
  ['变更 tab', changeTab],
  ['放行 tab', releaseTab]
]) {
  assert.match(
    source,
    /BatchExecutionTraceDrawer/,
    `${label} 必须接入统一批次执行追溯抽屉。`
  )
  assert.match(
    source,
    /openBatchTrace\(/,
    `${label} 行操作必须通过 openBatchTrace 打开追溯，而不是跳转到无上下文页面。`
  )
  assert.match(source, />\s*追溯\s*</, `${label} 行操作区必须显示“追溯”按钮。`)
}

assert.match(
  auditTab,
  /\{ key:\s*'traceActions',\s*label:\s*'追溯'[\s\S]*hideable:\s*false[\s\S]*business:\s*false/,
  '审计 tab 必须新增追溯行操作列。'
)
assert.match(
  auditTab,
  /@click="openBatchTrace\(row\)"/,
  '审计 tab 追溯行操作必须打开批次执行追溯抽屉。'
)
assert.match(
  auditTab,
  /executionId:\s*row\.executionId/,
  '审计 tab 的追溯入口必须携带执行记录 ID，支持打开单元责任和签名记录。'
)
assert.match(
  changeTab,
  /@click="openBatchTrace\(row\)"[\s\S]*详情/,
  '变更 tab 操作列必须同时保留详情和新增追溯入口。'
)
assert.match(
  changeTab,
  /batchExecutionId:\s*row\.batchExecutionId[\s\S]*executionId:\s*row\.executionId/,
  '变更 tab 的追溯入口必须携带批次执行 ID 和执行记录 ID。'
)
assert.match(
  releaseTab,
  /@click="openBatchTrace\(row\)"[\s\S]*检查项[\s\S]*事务事件[\s\S]*打印/,
  '放行 tab 追溯列必须在检查项、事务事件、打印之外新增批次追溯入口。'
)
assert.match(
  releaseTab,
  /batchExecutionId:\s*row\.batchExecutionId[\s\S]*releaseTransactionId:\s*row\.releaseTransactionId/,
  '放行 tab 的追溯入口必须携带批次执行 ID 和放行事务 ID。'
)

assert.match(
  traceDrawer,
  /getEdhrBatchReviewTimeline/,
  '批次执行追溯抽屉必须读取现有批次复盘时间线，用于聚合表单、签名和流程证据。'
)
assert.match(
  traceDrawer,
  /FieldAuditPage[\s\S]*initial-view="responsibility"[\s\S]*:initial-execution-id="selectedResponsibilityExecutionId"/,
  '单元责任页签必须嵌入现有字段责任汇总，显示单元格当前值、填写人和填写时间。'
)
assert.match(
  traceDrawer,
  /OperationAuditListPane[\s\S]*object-type="BATCH_EXECUTION"[\s\S]*:batch-execution-id="traceBatchExecutionId"/,
  '操作审计页签必须按批次执行 ID 展示按钮/操作人和发生时间。'
)
assert.match(
  traceDrawer,
  /SignaturePage[\s\S]*:initial-execution-id="selectedSignatureExecutionId"/,
  '电子签名页签必须嵌入现有 eDHR 签名记录，展示签名人和签名时间。'
)
assert.match(
  traceDrawer,
  /ReleaseEventListPane[\s\S]*:release-transaction-id="traceReleaseTransactionId"/,
  '放行批次必须保留放行事务事件只读追溯。'
)
assert.doesNotMatch(
  traceDrawer,
  /submitEdhrRelease|precheckEdhrRelease|approveEdhrRelease|rejectEdhrRelease|withdrawEdhrRelease|requestVoid|approveVoid|saveEdhrFieldChanges|completeEdhr|skipEdhr/,
  '追溯抽屉只能读取证据，不得引入保存、放行、作废、签名或状态变更动作。'
)

assert.match(
  fieldAuditPage,
  /embedded\?:\s*boolean[\s\S]*initialExecutionId\?:\s*number[\s\S]*initialView\?:\s*'responsibility' \| 'audit'/,
  '字段审计页必须支持 embedded + initialExecutionId + initialView，供追溯抽屉直接展示单元责任。'
)

console.log('PASS: form trace batch execution trace actions static contract')
