const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workOrderPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const source = fs.readFileSync(workOrderPagePath, 'utf8')
const tableStart = source.indexOf('<el-table')
const tableEnd = source.indexOf('</el-table>', tableStart)
assert.notEqual(tableStart, -1, 'Production work order page must include the work order table.')
assert.notEqual(tableEnd, -1, 'Production work order page must close the work order table.')
assert.match(
  source,
  /<UnifiedListTemplate[\s\S]*v-model:page="queryParams\.pageNo"[\s\S]*v-model:limit="queryParams\.pageSize"[\s\S]*@pagination="getList"/,
  'Production work order page must delegate pagination to the unified list template.'
)
const tableSource = source.slice(tableStart, tableEnd)

const keyColumns = [
  { label: '工单编号', prop: 'code' },
  { label: '产品编码', prop: 'productCode' },
  { label: '产品名称', prop: 'productName' },
  { label: '规格型号', prop: 'productSpecification' },
  { label: '计划数量', prop: 'quantity' }
]

const columnIndex = (label) => tableSource.indexOf(`label="${label}"`)

for (const column of keyColumns) {
  assert.notEqual(columnIndex(column.label), -1, `Production work order table must include ${column.label}.`)
  const columnStart = tableSource.indexOf(`label="${column.label}"`)
  const nextColumnStart = tableSource.indexOf('<el-table-column', columnStart + 1)
  const columnSource =
    nextColumnStart === -1 ? tableSource.slice(columnStart) : tableSource.slice(columnStart, nextColumnStart)
  assert.doesNotMatch(columnSource, /class="work-order-key-copy"/, `${column.label} must not render a copy button.`)
  assert.doesNotMatch(columnSource, /ep:copy-document/, `${column.label} must not render a copy icon.`)
  assert.doesNotMatch(columnSource, /title="复制/, `${column.label} must not keep copy button title text.`)
  assert.doesNotMatch(columnSource, /aria-label="复制/, `${column.label} must not keep copy button aria label.`)
}

assert.doesNotMatch(source, /handleCopyKeyField/, 'Production work order page must not keep key column copy handler.')
assert.doesNotMatch(source, /handleCopyWorkOrderCode/, 'Production work order page must not keep work order code copy handler.')
assert.doesNotMatch(source, /navigator\.clipboard\.writeText/, 'Production work order page must not write key column values to clipboard.')

const removedColumns = ['工单名称', 'BOM版本', '冲领料']
for (const label of removedColumns) {
  assert.equal(columnIndex(label), -1, `Production work order table must remove ${label}.`)
}
assert.notEqual(columnIndex('批次号'), -1, 'Production work order table must include 批次号.')
assert(/label="批次号"[\s\S]*?prop="batchCode"/.test(tableSource), '批次号 column must bind row batchCode.')

const expectedOrder = ['工单编号', '产品编码', '产品名称', '规格型号', '计划数量', '批次号']
for (let index = 1; index < expectedOrder.length; index += 1) {
  assert(
    columnIndex(expectedOrder[index - 1]) < columnIndex(expectedOrder[index]),
    `Key column ${expectedOrder[index - 1]} must appear before ${expectedOrder[index]}.`
  )
}

assert(
  columnIndex('计划数量') < columnIndex('生产车间') &&
    columnIndex('计划数量') < columnIndex('计划开工时间') &&
    columnIndex('计划数量') < columnIndex('批次号'),
  'Non-key columns must be placed after the red-box key information columns.'
)

const widthContracts = [
  { label: '工单编号', pattern: /label="工单编号"[\s\S]*?width="340"/ },
  { label: '产品编码', pattern: /label="产品编码"[\s\S]*?width="260"/ },
  { label: '产品名称', pattern: /label="产品名称"[\s\S]*?min-width="340"/ },
  { label: '规格型号', pattern: /label="规格型号"[\s\S]*?min-width="360"/ },
  { label: '计划数量', pattern: /label="计划数量"[\s\S]*?width="180"/ },
  { label: '批次号', pattern: /label="批次号"[\s\S]*?width="160"/ }
]

for (const contract of widthContracts) {
  assert(contract.pattern.test(tableSource), `${contract.label} must be widened to show key information clearly.`)
}

assert(
  /\.work-order-key-cell\s*\{[\s\S]*?display:\s*flex;[\s\S]*?width:\s*100%;[\s\S]*?\}/.test(source),
  'Key column cell layout must occupy the full column width instead of shrinking inline.'
)

assert(
  /\.work-order-key-text\s*\{[\s\S]*?flex:\s*1 1 auto;[\s\S]*?max-width:\s*none;[\s\S]*?\}/.test(source),
  'Key column text must use the full available width before ellipsis is applied.'
)

assert(
  !/\.work-order-key-text\s*\{[\s\S]*?max-width:\s*calc\(100% - 28px\)/.test(source),
  'Key column text must not keep the old fixed max-width that causes premature truncation.'
)

assert(!/catch\s*\{\s*\}/.test(source), 'Production work order page must not silently swallow failures.')

console.log('PASS: work order key columns static contract')
