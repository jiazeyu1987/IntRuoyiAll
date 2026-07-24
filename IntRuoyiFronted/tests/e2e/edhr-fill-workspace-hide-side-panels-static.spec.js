import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(currentDir, '../..')
const executionPage = fs.readFileSync(
  path.resolve(frontendRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue'),
  'utf8'
)

const workspaceRail = executionPage.match(
  /<aside class="edhr-fill-workspace__rail">[\s\S]*?<\/aside>/
)?.[0]
const fieldAuditDialog = executionPage.match(
  /<el-dialog[\s\S]*class="edhr-fill-workspace__save-signature-dialog"[\s\S]*?<\/el-dialog>/
)?.[0]

assert.ok(workspaceRail, '填写页必须保留左侧操作栏。')
assert.ok(fieldAuditDialog, '填写页必须保留保存草稿前置签名弹窗。')

for (const hiddenToken of [
  'edhr-fill-workspace__meta',
  '<dt>生产批号</dt>',
  '<dt>工序</dt>',
  'edhr-fill-workspace__field-audit-reason',
  'placeholder="请输入字段变更原因"'
]) {
  assert.ok(!workspaceRail.includes(hiddenToken), `左侧操作栏不得显示红框信息：${hiddenToken}`)
}

assert.match(
  fieldAuditDialog,
  /<el-form-item label="原因分类" required>[\s\S]*fieldAuditReasonForm\.reasonCategory/,
  '变更原因分类必须移到字段变更电子签名弹窗中填写。'
)
assert.match(
  fieldAuditDialog,
  /<el-form-item label="原因说明" required>[\s\S]*fieldAuditReasonForm\.reasonText[\s\S]*placeholder="请输入字段变更原因"/,
  '变更原因说明必须移到字段变更电子签名弹窗中填写。'
)
assert.match(
  executionPage,
  /const canOpenFieldAuditSignatureDialog = computed\(\(\) => !fieldAuditOpenGateError\.value\)/,
  '保存按钮打开弹窗时只能受结构性门禁控制，不能因为原因尚未填写而禁用。'
)
assert.match(
  executionPage,
  /const fieldAuditSaveGateError = computed\(\(\) => fieldAuditOpenGateError\.value \|\| fieldAuditReasonGateError\.value\)/,
  '最终保存仍必须校验变更原因，不能绕过字段审计原因。'
)
assert.match(
  workspaceRail,
  /openFieldAuditSignatureDialog[\s\S]*openSubmitDialog[\s\S]*toggleFillWorkspaceFullscreen/,
  '左侧操作栏隐藏红框信息后仍必须保留保存、提交执行和最大化。'
)

console.log('PASS: EDHR fill workspace hides side red-box panels')
