const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const readFormItemBlock = (label) => {
  const start = source.indexOf(`<el-form-item label="${label}" required>`)
  assert.notEqual(start, -1, `打开或创建弹框必须保留${label}表单项。`)

  const end = source.indexOf('</el-form-item>', start)
  assert.notEqual(end, -1, `${label}表单项必须完整闭合。`)

  return source.slice(start, end)
}

for (const label of ['生产工单', '工艺路线']) {
  const block = readFormItemBlock(label)
  assert(
    block.includes('popper-class="edhr-batch-page__work-order-select-popper"'),
    `${label}下拉必须使用专用 popper class，避免多行选项被默认下拉高度或宽度裁切。`
  )
}

assert.match(
  source,
  /:global\(\.edhr-batch-page__work-order-select-popper\)\s*\{[\s\S]*min-width:\s*min\(640px,\s*calc\(100vw - 48px\)\)/,
  '打开或创建弹框的工单/路线下拉必须比输入框更宽，并受视口宽度约束。'
)

assert.match(
  source,
  /:global\(\.edhr-batch-page__work-order-select-popper\s+\.el-select-dropdown__item\)\s*\{[\s\S]*height:\s*auto[\s\S]*line-height:\s*normal/,
  '打开或创建弹框的工单/路线下拉选项必须允许多行内容完整展开。'
)

assert.match(
  source,
  /:global\(\.edhr-batch-page__work-order-select-popper\s+\.edhr-batch-page__work-order-option\)\s*\{[\s\S]*min-height:\s*52px/,
  '打开或创建弹框的工单/路线下拉选项必须给双列多行内容保留最小高度。'
)

console.log('PASS: eDHR batch open/create dropdown popper static contract')
