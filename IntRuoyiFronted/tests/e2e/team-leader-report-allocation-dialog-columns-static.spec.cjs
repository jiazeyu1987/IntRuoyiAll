const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const allocationStart = page.indexOf('data-team-leader-allocation-table')
assert(allocationStart >= 0, '分配报工表格必须存在。')
const allocationEnd = page.indexOf('data-team-leader-allocation-summary', allocationStart)
assert(allocationEnd > allocationStart, '必须能定位分配报工表格边界。')
const allocationTable = page.slice(allocationStart, allocationEnd)
const allocationOrderValueStyle =
  page.match(/\.team-leader-workbench__allocation-order-value\s*\{([^}]*)\}/)?.[1] || ''
const allocationQuantityCellStyle =
  page.match(/\.team-leader-workbench__allocation-quantity-cell\s*\{([^}]*)\}/)?.[1] || ''

assert.doesNotMatch(allocationTable, /label="FIFO 剩余"/, '红框内 FIFO 剩余列必须删除。')
assert.match(
  page,
  /<el-dialog\s+v-model="reviewVisible"[\s\S]*width="min\(1480px, calc\(100vw - 24px\)\)"[\s\S]*class="team-leader-workbench__review-dialog"/,
  '分配报工弹框必须扩大到 1480px 上限，并保留小屏响应式边距。'
)
assert.match(
  allocationTable,
  /<el-table-column\s+label="生产订单号"\s+min-width="145"[\s\S]*class="team-leader-workbench__allocation-order-select"[\s\S]*<template #label>[\s\S]*formatAllocationOrderCode\(row\)/,
  '生产订单号必须独立成列，并保留活跃订单选择入口。'
)
assert.match(
  allocationTable,
  /<el-table-column\s+label="产品名称"\s+min-width="150"[\s\S]*formatAllocationOrderProductName\(row\)/,
  '产品名称必须使用正式字段独立成列。'
)
assert.match(
  allocationTable,
  /<el-table-column\s+label="产品编码"\s+min-width="140"[\s\S]*formatAllocationOrderProductCode\(row\)/,
  '产品编码必须使用正式字段独立成列。'
)
assert.match(
  allocationTable,
  /<el-table-column\s+label="订单数量"\s+width="90"[\s\S]*formatAllocationOrderQuantity\(row\)/,
  '订单数量必须使用正式字段独立成列。'
)
assert.doesNotMatch(
  allocationTable,
  /<el-table-column\s+label="活跃订单"|team-leader-workbench__allocation-order-label/,
  '四项订单信息不得继续合并显示在原活跃订单列中。'
)
assert.match(
  allocationTable,
  /<el-table-column\s+label="分配数量"\s+min-width="270"/,
  '分配数量列必须使用弹性最小宽度吸收剩余空间。'
)
assert.match(allocationTable, /label="状态"[\s\S]*label="操作"/, '状态和操作列必须保留。')
assert.match(
  allocationOrderValueStyle,
  /white-space:\s*normal;/,
  '拆分后的订单文字必须允许完整换行。'
)
assert.match(
  allocationOrderValueStyle,
  /overflow-wrap:\s*anywhere;/,
  '拆分后的订单长编码必须允许在自身列内换行。'
)
assert.match(
  allocationQuantityCellStyle,
  /grid-template-columns:\s*minmax\(88px, 1fr\) repeat\(3, max-content\);/,
  '数量输入和三个快捷按钮必须使用稳定的单行网格。'
)
assert.match(
  page,
  /\.team-leader-workbench__allocation-overage\s*\{[\s\S]*grid-column:\s*1\s*\/\s*-1;/,
  '超量提示存在时必须独占下一行，不能挤压主控制行。'
)
assert.match(
  allocationTable,
  /data-team-leader-allocation-clear[\s\S]*data-team-leader-allocation-overage/,
  '超量提示必须位于三个快捷按钮之后，避免自动网格把按钮推到下一行。'
)

console.log('PASS: allocation dialog columns and full-display contract is wired')
