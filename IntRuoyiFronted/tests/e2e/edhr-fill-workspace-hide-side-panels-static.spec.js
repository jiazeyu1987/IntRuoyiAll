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

assert.ok(workspaceRail, '填写页必须保留左侧操作栏。')
assert.ok(
  !executionPage.includes('edhr-fill-workspace__save-signature-dialog'),
  '填写页不得保留保存草稿前置签名弹窗。'
)

for (const hiddenToken of [
  'edhr-fill-workspace__meta',
  '<dt>生产批号</dt>',
  '<dt>工序</dt>',
  'edhr-fill-workspace__field-audit-reason',
  'placeholder="请输入字段变更原因"',
  'v-if="preReleaseEditNotice"',
  ':title="preReleaseEditNotice"',
  'v-if="goldenFingerNotice"',
  ':title="goldenFingerNotice"'
]) {
  assert.ok(!workspaceRail.includes(hiddenToken), `左侧操作栏不得显示红框信息：${hiddenToken}`)
}

for (const retainedAlert of [
  'revisionLockNotice',
  'fieldAuditOpenGateError',
  'fieldAuditSaveError'
]) {
  assert.ok(workspaceRail.includes(retainedAlert), `左侧操作栏必须保留真实告警：${retainedAlert}`)
}

assert.match(
  executionPage,
  /const fieldAuditSaveGateError = computed\(\(\) => fieldAuditOpenGateError\.value\)/,
  '保存草稿按钮只能受结构性门禁控制，不能因为原因尚未填写而禁用。'
)
assert.match(
  workspaceRail,
  /handleSaveFieldAuditChanges[\s\S]*openSubmitDialog[\s\S]*toggleFillWorkspaceFullscreen/,
  '左侧操作栏隐藏红框信息后仍必须保留保存、提交执行和最大化。'
)

console.log('PASS: EDHR fill workspace hides side red-box panels')
