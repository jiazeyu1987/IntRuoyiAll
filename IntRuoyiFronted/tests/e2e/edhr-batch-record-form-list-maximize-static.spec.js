const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const readonlyFormPath = path.join(repoRoot, 'src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')

const page = fs.readFileSync(pagePath, 'utf8')
const readonlyForm = fs.readFileSync(readonlyFormPath, 'utf8')

const previewMatch = page.match(/<section class="batch-record-form-preview">[\s\S]*?<\/section>/)
assert.ok(previewMatch, '批记录表单页必须保留右侧表单预览区域。')
const preview = previewMatch[0]

const headerMatch = preview.match(/class="batch-record-form-preview__header"[\s\S]*?<div v-loading="templatePreview\.loading"/)
assert.ok(headerMatch, '右侧预览区域必须保留 header。')
const header = headerMatch[0]

assert.ok(header.includes('@click="enterPreviewMaximize"'), '红框位置必须新增最大化入口。')
assert.ok(header.includes('最大化'), '最大化入口必须显示“最大化”按钮文案。')
assert.ok(
  header.indexOf('enterPreviewMaximize') < header.indexOf("openDesigner(selectedReport.reportId, 'preview')"),
  '最大化按钮必须位于“打开”动作之前。'
)

const focusedMatch = page.match(/<Teleport to="body">[\s\S]*?<\/Teleport>/)
assert.ok(focusedMatch, '最大化预览必须通过 Teleport 覆盖整个应用可视区。')
const focused = focusedMatch[0]

for (const snippet of [
  'v-if="previewMaximized"',
  'class="batch-record-form-focused-preview"',
  'class="batch-record-form-focused-preview__control"',
  'class="batch-record-form-focused-preview__form-name"',
  "{{ selectedReport?.reportName || '未选择表单' }}",
  '上一张',
  '下一张',
  '恢复',
  '高度自适应',
  '宽度自适应',
  '@click="selectPreviewNeighbor(-1)"',
  '@click="selectPreviewNeighbor(1)"',
  '@click="restorePreviewLayout"',
  "@click=\"setPreviewFitMode('height')\"",
  "@click=\"setPreviewFitMode('width')\"",
  ':fit-mode="previewFitMode"',
  'EdhrExecutionReadonlyForm'
]) {
  assert.ok(focused.includes(snippet), `最大化预览缺少必要结构或控制：${snippet}`)
}

for (const forbiddenAction of ['打开', '编辑', '填写', '签名', '规则', '链接', '重命名', '删除']) {
  assert.ok(!focused.includes(`>${forbiddenAction}</el-button>`), `最大化模式不得显示业务动作：${forbiddenAction}`)
}

assert.match(page, /const previewMaximized = ref\(false\)/, '最大化状态必须由显式响应式状态控制。')
assert.match(page, /const previewFitMode = ref<'width' \| 'height'>\('width'\)/, '最大化预览必须默认宽度自适应。')
assert.match(page, /const selectedReportIndex = computed\(\(\) =>/, '上一张/下一张必须基于当前列表顺序计算。')
assert.match(page, /const canPreviewPrevious = computed\(\(\) =>/, '必须显式控制上一张可用状态。')
assert.match(page, /const canPreviewNext = computed\(\(\) =>/, '必须显式控制下一张可用状态。')

assert.ok(readonlyForm.includes("fitMode?: 'width' | 'height'"), '只读表单组件必须接收宽度/高度自适应模式。')
assert.ok(readonlyForm.includes(":width-only=\"fitMode !== 'height'\""), '高度自适应必须关闭 width-only 缩放。')
assert.ok(readonlyForm.includes("'is-height-fit': fitMode === 'height'"), '高度自适应必须让只读表单占满可视高度。')

console.log('PASS: eDHR batch record form list maximize static contract')
