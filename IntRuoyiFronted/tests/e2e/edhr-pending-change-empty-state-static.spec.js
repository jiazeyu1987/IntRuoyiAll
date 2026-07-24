const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const executionPagePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/ExecutionPage.vue'
)

const source = fs.readFileSync(executionPagePath, 'utf8')

const assertIncludes = (token, message) => {
  assert.ok(source.includes(token), message)
}

for (const token of [
  'edhr-page-shell__field-audit-empty',
  '暂无待保存变更',
  '修改字段或附件后，会在这里核对变更内容、原因和审计基准。',
  'v-if="hasPendingFieldAuditChanges"',
  'v-else',
  'fieldAuditReasonForm',
  'pendingFieldChanges',
  'pendingAttachmentChanges'
]) {
  assertIncludes(token, `待保存变更区必须有无变更空状态和有变更条件展示：${token}`)
}

assert.ok(
  /v-if="hasPendingFieldAuditChanges"[\s\S]*class="edhr-page-shell__field-audit-reason"/.test(source),
  '原因分类和原因说明表单必须仅在存在待保存字段或附件变更时显示。'
)

assert.ok(
  /v-if="hasPendingFieldAuditChanges"[\s\S]*class="edhr-page-shell__field-audit-table"/.test(source),
  '待保存变更表必须仅在存在待保存字段或附件变更时显示。'
)

assert.ok(
  /v-if="fieldAuditOpenGateError && hasPendingFieldAuditChanges"/.test(source),
  '保存门禁提示必须覆盖字段变更和附件变更，不能只看 pendingFieldChanges。'
)

assert.ok(
  /v-if="hasPendingAttachmentChanges"[\s\S]*待保存附件/.test(source),
  '存在待保存附件但无字段变更时，页面也必须给出可见附件摘要，不能显示空表格。'
)

console.log('PASS: EDHR pending change empty-state static contract')
