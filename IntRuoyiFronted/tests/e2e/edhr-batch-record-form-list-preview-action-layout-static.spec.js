const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const page = fs.readFileSync(pagePath, 'utf8')

const previewMatch = page.match(/<section class="batch-record-form-preview">[\s\S]*?<\/section>/)
assert.ok(previewMatch, '批记录表单页必须保留右侧表单预览区域。')
const preview = previewMatch[0]

const headerIndex = preview.indexOf('class="batch-record-form-preview__header"')
const actionsIndex = preview.indexOf('class="batch-record-form-preview__actions"')
const bodyIndex = preview.indexOf('class="batch-record-form-preview__body"')
assert.ok(headerIndex >= 0, '右侧预览区域必须有顶部 header。')
assert.ok(actionsIndex > headerIndex, '操作按钮必须放在右侧预览顶部 header 内。')
assert.ok(actionsIndex < bodyIndex, '操作按钮必须位于表单预览 body 之前。')

for (const snippet of [
  "openDesigner(selectedReport.reportId, 'preview')",
  "openDesigner(selectedReport.reportId, 'edit')",
  'openSimulate(selectedReport)',
  "openTemplateAction(selectedReport, 'signature')",
  "openTemplateAction(selectedReport, 'cellRules')",
  'handleCellLinks(selectedReport)',
  'handleRename(selectedReport)',
  'handleDelete(selectedReport)'
]) {
  assert.ok(preview.includes(snippet), `右侧顶部操作区必须保留动作：${snippet}`)
}

const bodyMatch = preview.match(/<div v-loading="templatePreview\.loading" class="batch-record-form-preview__body">[\s\S]*?<\/div>\s*<\/section>/)
assert.ok(bodyMatch, '右侧表单预览 body 必须包裹原表单内容。')
assert.ok(
  bodyMatch[0].includes('class="batch-record-form-preview__frame"') &&
    bodyMatch[0].includes('EdhrExecutionReadonlyForm'),
  '原来的表单预览内容必须放在操作区下方的 body/frame 区域。'
)
assert.ok(
  !bodyMatch[0].includes('batch-record-form-preview__actions'),
  '表单预览 body 内不得再承载操作按钮。'
)

assert.ok(!page.includes('prop="operation"'), '批记录表单列表不应继续保留操作列。')
assert.ok(!page.includes("key: 'operation'"), '显示字段配置不应继续暴露操作列。')
assert.ok(!page.includes('label="操作"'), '批记录表单列表不应继续显示操作列标题。')

console.log('PASS: eDHR batch record form list preview action layout static contract')
