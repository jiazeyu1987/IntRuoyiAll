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
const assistPanelStart = executionPage.indexOf(
  '<section v-if="fillViewMode === \'assist\'" class="edhr-fill-workspace__assist-panel">'
)
const originalFormStart = executionPage.indexOf('<EdhrExecutionTemplateEditableForm', assistPanelStart)
const assistPanel =
  assistPanelStart >= 0 && originalFormStart > assistPanelStart
    ? executionPage.slice(assistPanelStart, originalFormStart)
    : undefined
const assistRowStart = assistPanel?.indexOf('class="edhr-fill-workspace__assist-row"') ?? -1
const assistRowEnd =
  assistPanel && assistRowStart >= 0 ? assistPanel.indexOf('</article>', assistRowStart) : -1
const assistRowTemplate =
  assistPanel && assistRowStart >= 0 && assistRowEnd > assistRowStart
    ? assistPanel.slice(assistRowStart, assistRowEnd)
    : undefined

assert.ok(workspaceRail, '填写页必须保留左侧操作栏。')
assert.ok(assistPanel, '填写页必须保留辅助填写字段区域。')
assert.ok(assistRowTemplate, '填写页必须保留辅助填写字段卡片模板。')
assert.ok(
  !executionPage.includes('edhr-fill-workspace__save-signature-dialog'),
  '填写页不得保留保存草稿前置签名弹窗。'
)
assert.match(
  executionPage,
  /<div\s+v-if="isTrackingReadonlyMode"\s+class="edhr-page-shell__toolbar">/,
  '非追踪填写页不得显示截图红框中的外层标题和右上角工具栏。'
)

for (const hiddenToken of [
  'edhr-fill-workspace__meta',
  '<dt>生产批号</dt>',
  '<dt>工序</dt>',
  'edhr-fill-workspace__change-summary',
  '待保存变更',
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

for (const hiddenAssistToken of [
  'edhr-fill-workspace__assist-title',
  '我的填写项',
  'edhr-fill-workspace__assist-missing-jump',
  '还差 {{ assistMissingFieldCount }} 项',
  'edhr-fill-workspace__assist-summary',
  '未完成摘要',
  '必填、附件和签名已完成，可以提交执行。'
]) {
  assert.ok(!assistPanel.includes(hiddenAssistToken), `辅助填写区域不得显示红框信息：${hiddenAssistToken}`)
}

for (const hiddenAssistCardToken of [
  'edhr-fill-workspace__assist-help',
  'edhr-fill-workspace__assist-source',
  'field.helpText || field.placeholder',
  '字段说明未配置',
  '位置：第 {{ field.rowIndex + 1 }} 行 / 第 {{ field.columnIndex + 1 }} 列',
  '<el-tag',
  'resolveAssistFieldStatusLabel(field)',
  'resolveAssistFieldStatusTagType(field)'
]) {
  assert.ok(
    !assistRowTemplate.includes(hiddenAssistCardToken),
    `辅助填写卡片内部不得显示红框元信息：${hiddenAssistCardToken}`
  )
}

for (const retainedAssistToken of [
  'edhr-fill-workspace__assist-topbar',
  'edhr-fill-workspace__assist-switch-grid',
  '任务 / 批次',
  'assistTaskSwitchLabel',
  '工序',
  'assistProcessSwitchLabel',
  '填写人',
  'assistFillerSwitchLabel'
]) {
  assert.ok(assistPanel.includes(retainedAssistToken), `辅助填写区域必须保留切换卡：${retainedAssistToken}`)
}

assert.ok(
  assistPanel.includes('edhr-fill-workspace__assist-row'),
  '隐藏辅助顶栏后仍必须保留真实字段填写行。'
)

assert.match(
  executionPage,
  /const fieldAuditSaveGateError = computed\(\(\) => fieldAuditOpenGateError\.value\)/,
  '保存草稿按钮只能受结构性门禁控制，不能因为原因尚未填写而禁用。'
)
assert.match(
  workspaceRail,
  /openFieldAuditSignatureDialog[\s\S]*openSubmitDialog[\s\S]*toggleFillWorkspaceFullscreen/,
  '左侧操作栏隐藏红框信息后仍必须保留保存、提交执行和最大化。'
)

console.log('PASS: EDHR fill workspace hides side red-box panels')
