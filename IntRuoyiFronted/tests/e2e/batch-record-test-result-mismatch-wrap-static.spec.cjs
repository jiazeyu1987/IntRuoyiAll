const fs = require('fs')
const path = require('path')
const assert = require('assert')

const frontendRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue'),
  'utf8'
)

const resultDialog = page.match(
  /<el-dialog\b(?=[^>]*data-edhr-batch-record-test-result-dialog)[^>]*>[\s\S]*?<\/el-dialog>/
)?.[0]
assert.ok(resultDialog, '必须保留带稳定标识的批记录测试结果弹框。')

assert.match(
  resultDialog,
  /class="edhr-batch-record-test-page__result-dialog"/,
  '结果弹框必须使用专用响应式布局类。'
)
assert.match(
  resultDialog,
  /width="min\(1120px, calc\(100vw - 32px\)\)"/,
  '结果弹框必须在宽屏提供足够空间，并在窄屏保留 16px 视口边距。'
)
assert.match(resultDialog, /top="4vh"/, '长回复弹框必须从视口顶部 4vh 开始，保留正文高度。')

const resultTable = resultDialog.match(
  /<el-table\b(?=[^>]*class="edhr-batch-record-test-page__result-table")[^>]*>[\s\S]*?<\/el-table>/
)?.[0]
assert.ok(resultTable, 'Codex CLI 检查点必须使用专用结果表格。')
assert.match(
  resultTable,
  /:show-overflow-tooltip="false"/,
  '结果表格必须关闭表级单行溢出提示，让长文本直接参与换行。'
)

const expectedColumns = [
  ['检查点', 'min-width', '120'],
  ['状态', 'width', '80'],
  ['预期', 'min-width', '160'],
  ['实际回复', 'min-width', '200'],
  ['不符合描述', 'min-width', '260']
]
for (const [label, widthAttribute, width] of expectedColumns) {
  assert.match(
    resultTable,
    new RegExp(
      `<el-table-column\\b(?=[^>]*label="${label}")(?=[^>]*${widthAttribute}="${width}")[^>]*>`
    ),
    `${label}列必须使用 ${width}px 最小宽度，使五列总宽度可放入窄桌面弹框。`
  )
}

for (const [label, className] of [
  ['实际回复', 'edhr-batch-record-test-page__actual-reply-column'],
  ['不符合描述', 'edhr-batch-record-test-page__mismatch-description-column']
]) {
  const column = resultTable.match(
    new RegExp(`<el-table-column\\b(?=[^>]*label="${label}")[^>]*>[\\s\\S]*?<\\/el-table-column>`)
  )?.[0]
  assert.ok(column, `必须保留${label}列。`)
  assert.match(column, new RegExp(`class-name="${className}"`), `${label}列必须使用专用换行类。`)
  assert.match(
    column,
    /:show-overflow-tooltip="false"/,
    `${label}列必须显式关闭单行溢出提示。`
  )
}

const wrapStyleBlock = page.match(
  /:deep\(\.edhr-batch-record-test-page__actual-reply-column \.cell\),\s*:deep\(\.edhr-batch-record-test-page__mismatch-description-column \.cell\)\s*\{([\s\S]*?)\}/
)?.[1]
assert.ok(wrapStyleBlock, '实际回复与不符合描述必须共享专用换行样式块。')
assert.match(wrapStyleBlock, /white-space:\s*normal;/, '结果长文本必须允许自然换行。')
assert.match(wrapStyleBlock, /overflow-wrap:\s*anywhere;/, '连续长文本必须能在单元格边界断行。')
assert.match(wrapStyleBlock, /word-break:\s*break-word;/, '长单词不得越过结果列边界。')

const dialogBodyStyleBlock = page.match(
  /:global\(\.edhr-batch-record-test-page__result-dialog \.el-dialog__body\)\s*\{([\s\S]*?)\}/
)?.[1]
assert.ok(dialogBodyStyleBlock, '结果弹框正文必须有专用视口高度约束。')
assert.match(
  dialogBodyStyleBlock,
  /max-height:\s*calc\(92vh - 116px\);/,
  '结果弹框正文必须限制在可见视口高度内。'
)
assert.match(dialogBodyStyleBlock, /overflow-y:\s*auto;/, '超长回复必须能在弹框正文内纵向滚动。')

console.log('batch record test mismatch description wrap static contract passed')
