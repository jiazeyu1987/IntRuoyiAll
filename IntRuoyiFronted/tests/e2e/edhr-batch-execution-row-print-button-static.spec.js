const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8')

const operationColumnMatch = source.match(
  /<el-table-column[^>]+prop="operation"[\s\S]*?<div class="edhr-batch-page__actions">([\s\S]*?)<\/div>/
)

assert(operationColumnMatch, '批次执行列表必须保留行内操作区。')

const rowActions = operationColumnMatch[1]

for (const actionText of ['填写', '模板', '作废']) {
  assert(
    rowActions.includes(actionText),
    `批次执行列表行内操作区必须保留“${actionText}”操作。`
  )
}

assert(
  !/>\s*打印\s*<\/el-button>/.test(rowActions),
  '批次执行列表每行操作区不应显示“打印”按钮。'
)

assert(
  !rowActions.includes('handleDownloadArchive(row)'),
  '批次执行列表每行操作区不应绑定行内归档下载/打印处理。'
)

console.log('PASS: eDHR batch execution row print button is hidden')
