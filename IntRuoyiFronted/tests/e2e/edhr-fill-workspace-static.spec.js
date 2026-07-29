import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(currentDir, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.resolve(frontendRoot, relativePath), 'utf8')

const routeModule = read('src/router/modules/remaining.ts')
const batchDetailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const editableForm = read(
  'src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue'
)
const fitViewport = read('src/views/mes/pro/edhr/components/EdhrTemplateFitViewport.vue')
const workspaceRail = executionPage.match(
  /<div ref="fillWorkspaceRef" class="edhr-fill-workspace">[\s\S]*?<\/aside>/
)?.[0]
const loadErrorIndex = executionPage.indexOf('class="edhr-fill-workspace__load-error"')
const submitSignDialogIndex = executionPage.indexOf(
  'class="edhr-fill-workspace__submit-sign-dialog"'
)
const resultDialogIndex = executionPage.indexOf('class="edhr-fill-workspace__result-dialog"')
const submitSignDialogBlock =
  submitSignDialogIndex >= 0
    ? executionPage.slice(submitSignDialogIndex - 240, submitSignDialogIndex + 520)
    : ''
const resultDialogBlock =
  resultDialogIndex >= 0
    ? executionPage.slice(resultDialogIndex - 240, resultDialogIndex + 520)
    : ''

assert.ok(workspaceRail, '填写工作区必须包含左侧控制栏')

assert.match(
  routeModule,
  /path:\s*'pro\/feedback\/edhr-execution\/form'[\s\S]*ExecutionPage\.vue/,
  '填写工作区必须复用现有隐藏执行表单路由'
)
assert.match(
  batchDetailPage,
  /path:\s*'\/mes\/pro\/feedback\/edhr-execution\/form'/,
  '批次详情的开始填写入口必须打开填写工作区'
)

for (const token of [
  'EdhrExecutionTemplateEditableForm',
  'edhr-fill-workspace',
  'edhr-fill-workspace__rail',
  'edhr-fill-workspace__canvas',
  '适应宽度',
  '适应高度',
  "ref<'width' | 'height'>('width')",
  ':fit-mode="fitMode"',
  'pendingFieldChanges.length',
  'ref="fillWorkspaceRef"',
  '最大化',
  '退出全屏',
  'requestFullscreen()',
  'document.exitFullscreen()',
  'fullscreenchange'
]) {
  assert.ok(executionPage.includes(token), `填写工作区缺少契约标记：${token}`)
}

assert.match(
  executionPage,
  /\.edhr-fill-workspace__rail\s*\{[\s\S]*width:\s*136px/,
  '左侧控制栏必须固定为 136px，确保全屏操作按钮完整显示'
)
assert.match(
  executionPage,
  /\.edhr-fill-workspace:fullscreen\s*\{[\s\S]*height:\s*100vh/,
  '填写工作区全屏时必须占满浏览器视口'
)
assert.ok(
  submitSignDialogIndex > 0 && submitSignDialogIndex < loadErrorIndex,
  '提交执行签名弹框必须渲染在全屏填写工作区内部，避免浏览器全屏时被遮挡'
)
assert.ok(
  resultDialogIndex > submitSignDialogIndex && resultDialogIndex < loadErrorIndex,
  '保存/提交结果弹框必须渲染在全屏填写工作区内部，避免浏览器全屏时被遮挡'
)
assert.match(
  submitSignDialogBlock,
  /:append-to-body="false"/,
  '提交执行签名弹框不得 teleport 到 body，否则全屏工作区会遮挡弹框'
)
assert.match(
  resultDialogBlock,
  /:append-to-body="false"/,
  '保存/提交结果弹框不得 teleport 到 body，否则全屏工作区会遮挡弹框'
)
assert.match(
  executionPage,
  /v-if="isTrackingReadonlyMode"[\s\S]*EdhrExecutionReadonlyForm/,
  '追踪只读模式必须继续使用原有只读表单'
)
assert.match(
  executionPage,
  /<div\s+v-if="isTrackingReadonlyMode"\s+class="edhr-page-shell__toolbar">/,
  '非追踪填写模式必须隐藏截图红框中的外层标题和右上角工具栏'
)
assert.ok(
  !executionPage.includes('class="edhr-page-shell__form"'),
  '非追踪模式不得继续渲染旧通用字段网格'
)
assert.ok(
  !executionPage.includes('class="edhr-fill-workspace__assist-original"'),
  '辅助模式顶栏不得继续渲染截图红框中的看原表入口'
)
assert.ok(!executionPage.includes('看原表'), '辅助模式顶栏不得继续显示看原表按钮文本')
assert.ok(
  !workspaceRail.includes('type="danger"') && !workspaceRail.includes('>返回</el-button>'),
  '左侧操作区不得继续渲染截图红框中的返回按钮'
)

for (const token of [
  'edhr-fill-workspace__heading',
  'edhr-fill-workspace__meta',
  'edhr-fill-workspace__change-summary',
  'edhr-fill-workspace__field-audit-reason',
  '待保存变更',
  '执行编号',
  '生产工单',
  '生产批号',
  '<dt>工序</dt>',
  '工作站',
  '填写对象',
  'showFormReviewSignAction',
  '>刷新</el-button>'
]) {
  assert.ok(!workspaceRail.includes(token), `精简控制栏不得继续显示：${token}`)
}

assert.match(
  workspaceRail,
  /openFieldAuditSignatureDialog[\s\S]*openSubmitDialog[\s\S]*edhr-fill-workspace__fullscreen-action/,
  '精简控制栏必须保留保存、提交执行和最大化'
)
assert.match(
  executionPage,
  /\.edhr-fill-workspace__fit-actions\s*\{[\s\S]*grid-template-columns:\s*1fr/,
  '显示方式按钮必须纵向单列排列'
)
assert.match(
  executionPage,
  /\.edhr-fill-workspace__rail-actions\s*\{[\s\S]*grid-template-columns:\s*1fr/,
  '底部操作按钮必须纵向单列排列'
)
assert.match(
  executionPage,
  /\.edhr-fill-workspace__rail-actions :deep\(\.el-button\)\s*\{[\s\S]*padding-inline:\s*8px/,
  '窄控制栏按钮必须使用紧凑横向内边距，避免全屏文字被遮挡'
)

for (const token of [
  "fitMode?: 'width' | 'height'",
  '<slot name="field"',
  "fitMode === 'width'",
  'height: 100%'
]) {
  assert.ok(editableForm.includes(token) || fitViewport.includes(token), `模板适应组件缺少契约标记：${token}`)
}

assert.match(
  fitViewport,
  /props\.widthOnly\s*\?\s*widthScale\s*:\s*Math\.min\(widthScale,\s*heightScale\)/,
  '适应宽度必须按可用宽度缩放，模板原始宽度较小时也应放大占满画布'
)
assert.ok(
  !fitViewport.includes('Math.min(widthScale, 1)'),
  '适应宽度不得把放大比例封顶为 1'
)

assert.ok(
  !editableForm.includes('模拟页不支持上传附件'),
  '正式填写组件不得保留模拟页附件占位提示'
)

console.log('PASS: eDHR fill workspace static contract')
