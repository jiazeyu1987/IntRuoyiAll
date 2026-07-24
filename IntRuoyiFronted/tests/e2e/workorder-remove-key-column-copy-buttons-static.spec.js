const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workOrderPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const source = fs.readFileSync(workOrderPagePath, 'utf8')
const tableStart = source.indexOf('<el-table')
const tableEnd = source.indexOf('</el-table>', tableStart)

assert.notEqual(tableStart, -1, '生产工单页面必须包含列表表格。')
assert.notEqual(tableEnd, -1, '生产工单页面表格必须正确闭合。')

const tableSource = source.slice(tableStart, tableEnd)
const keyColumns = ['工单编号', '产品编码', '产品名称', '规格型号', '计划数量']

for (const label of keyColumns) {
  const columnStart = tableSource.indexOf(`label="${label}"`)
  assert.notEqual(columnStart, -1, `生产工单表格必须保留${label}列。`)
  const nextColumnStart = tableSource.indexOf('<el-table-column', columnStart + 1)
  const columnSource =
    nextColumnStart === -1 ? tableSource.slice(columnStart) : tableSource.slice(columnStart, nextColumnStart)
  assert.doesNotMatch(columnSource, /class="work-order-key-copy"/, `${label}列不得渲染复制按钮。`)
  assert.doesNotMatch(columnSource, /ep:copy-document/, `${label}列不得渲染复制图标。`)
  assert.doesNotMatch(columnSource, /aria-label="复制/, `${label}列不得保留复制按钮 aria-label。`)
  assert.doesNotMatch(columnSource, /title="复制/, `${label}列不得保留复制按钮 title。`)
}

assert.match(
  tableSource,
  /label="工单编号"[\s\S]*?@click="openForm\('detail', scope\.row\.id\)"/,
  '工单编号列必须保留点击进入详情能力。'
)
assert.doesNotMatch(source, /handleCopyKeyField/, '生产工单页面不应再保留关键列复制 handler。')
assert.doesNotMatch(source, /handleCopyWorkOrderCode/, '生产工单页面不应再保留工单编号复制 handler。')
assert.doesNotMatch(source, /navigator\.clipboard\.writeText/, '生产工单页面不应再写入剪贴板。')

console.log('PASS: work order key column copy buttons are removed')
