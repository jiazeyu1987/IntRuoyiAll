const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const page = fs.readFileSync(pagePath, 'utf8')

const previewMatch = page.match(/<section class="batch-record-form-preview">[\s\S]*?<\/section>/)
assert.ok(previewMatch, '批记录表单页必须保留右侧表单预览区域。')
const preview = previewMatch[0]

const headerMatch = preview.match(/<div class="batch-record-form-preview__header">[\s\S]*?<div v-loading="templatePreview\.loading"/)
assert.ok(headerMatch, '右侧预览区域必须有顶部 header。')
const header = headerMatch[0]

assert.ok(
  !header.includes('<span>表单预览</span>'),
  '右侧预览头部不得继续显示黄框内“表单预览”文案。'
)
assert.ok(
  header.includes('class="batch-record-form-preview__title"') &&
    header.includes("{{ selectedReport?.reportName || '未选择表单' }}"),
  '右侧预览头部仍需保留当前表单名称上下文。'
)

for (const [label, handler] of [
  ['填写', 'openSimulate(selectedReport)'],
  ['签名', "openTemplateAction(selectedReport, 'signature')"],
  ['规则', "openTemplateAction(selectedReport, 'cellRules')"],
  ['链接', 'handleCellLinks(selectedReport)']
]) {
  assert.ok(header.includes(`>${label}</el-button>`), `红框按钮必须缩短为两个字：${label}`)
  assert.ok(header.includes(handler), `缩短按钮后必须保留原处理函数：${handler}`)
}

for (const oldLabel of ['模拟填写', '签名位', '单元格规则', '单元格链接']) {
  assert.ok(!header.includes(`>${oldLabel}</el-button>`), `右侧预览头部不应继续显示旧按钮文案：${oldLabel}`)
}

console.log('PASS: eDHR batch record form list preview header short labels static contract')
