const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8').replace(/\r\n/g, '\n')

const extractBlock = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.notEqual(start, -1, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end + endToken.length)
}

const assistPreviewBlock = extractBlock(
  detail,
  'edhr-batch-detail__assist-preview',
  '</section>'
)
const script = detail.slice(detail.indexOf('<script setup'), detail.indexOf('</script>'))
const style = detail.slice(detail.indexOf('<style'), detail.indexOf('</style>'))

assert.match(
  script,
  /const\s+parseDetailPreviewAssistGridRowKey\s*=\s*\(rowKey:\s*string\)[\s\S]*ASSIST_GRID_\(USERS\|ROLE\)[\s\S]*ASSIST_GRID_U/,
  '详情页必须解析当前 USERS/ROLE 和旧版 U 辅助格 rowKey。'
)
assert.match(
  script,
  /const\s+selectedPreviewAssistGrids\s*=\s*computed<DetailPreviewAssistGrid\[\]>\(\(\)\s*=>[\s\S]*subjectKey[\s\S]*rowIndex[\s\S]*columnIndex/,
  '详情页必须按责任主体和辅助格坐标构建网格，而不是扁平化字段。'
)
assert.match(
  script,
  /const\s+selectedPreviewAssistGridSize\s*=\s*computed\(\(\)\s*=>[\s\S]*rowCount[\s\S]*columnCount/,
  '详情页必须从正式辅助格坐标恢复辅助表格行列数。'
)
assert.match(
  script,
  /selectedPreviewSnapshot\.value\?\.assistGridRowCount[\s\S]*selectedPreviewSnapshot\.value\?\.assistGridColumnCount[\s\S]*snapshotRowCount[\s\S]*snapshotColumnCount/,
  '详情页必须优先使用运行快照中的正式辅助表格尺寸，不能仅按已映射格子推断。'
)
assert.match(
  script,
  /gridKey\.rowIndex\s*>=\s*rowCount[\s\S]*gridKey\.columnIndex\s*>=\s*columnCount[\s\S]*超出辅助表格尺寸/,
  '详情页必须阻止超出正式辅助表格尺寸的辅助格伪装成有效配置。'
)
assert.match(
  script,
  /resolveDetailPreviewAssistSubjectLabel[\s\S]*fillableUsers/,
  '详情页必须优先使用批次任务填写人快照显示个人责任主体名称。'
)

assert.ok(
  assistPreviewBlock.includes('v-for="grid in selectedPreviewAssistGrids"'),
  '辅助模式必须按责任主体渲染辅助表格。'
)
assert.ok(
  assistPreviewBlock.includes('辅助表格 {{ grid.rowCount }} × {{ grid.columnCount }}'),
  '辅助模式必须显示与配置预览一致的辅助表格尺寸。'
)
assert.ok(
  assistPreviewBlock.includes('v-for="gridRow in grid.rows"') &&
    assistPreviewBlock.includes('v-for="gridCell in gridRow.cells"'),
  '辅助模式必须逐行逐格渲染固定网格。'
)
assert.ok(
  assistPreviewBlock.includes(':data-assist-grid-cell="gridCell.key"'),
  '每个辅助格必须暴露稳定的配置格子 key。'
)
assert.ok(
  assistPreviewBlock.includes("gridCell.field?.label || '未映射'"),
  '未映射辅助格必须保留并明确显示未映射状态。'
)
assert.ok(
  assistPreviewBlock.includes("gridCell.field ? 'is-mapped' : 'is-empty'"),
  '辅助格必须区分映射和空格状态。'
)
assert.ok(
  !assistPreviewBlock.includes('v-for="field in selectedPreviewAssistFields"'),
  '详情页辅助模式不得继续直接遍历扁平字段列表。'
)

assert.match(
  style,
  /\.edhr-batch-detail__assist-grid\s*\{[\s\S]*border-collapse:\s*separate;[\s\S]*table-layout:\s*fixed;/,
  '详情页辅助表格必须使用与配置预览一致的固定表格布局。'
)
assert.match(
  style,
  /\.edhr-batch-detail__assist-grid-cell\s*\{[\s\S]*min-height:\s*96px;[\s\S]*border-radius:\s*8px;/,
  '详情页辅助格必须使用稳定高度和紧凑圆角。'
)
assert.match(
  style,
  /\.edhr-batch-detail__assist-grid-surface\s*\{[\s\S]*background:\s*#fff8d6;/,
  '详情页辅助表格区域必须使用与配置预览一致的黄色底色。'
)

for (const forbiddenWriteEntrypoint of [
  'openFieldAuditSignatureDialog',
  'openSubmitDialog',
  'saveEdhrFieldChanges',
  'ProFeedbackApi.submitEdhrExecution',
  '<el-upload'
]) {
  assert.ok(
    !assistPreviewBlock.includes(forbiddenWriteEntrypoint),
    `详情页辅助表格必须保持只读：${forbiddenWriteEntrypoint}`
  )
}

console.log('PASS: eDHR batch detail assist grid parity static contract')
