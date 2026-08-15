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

assert.doesNotMatch(allocationTable, /label="FIFO 剩余"/, '红框内 FIFO 剩余列必须删除。')
assert.match(
  allocationTable,
  /<el-table-column\s+label="活跃订单"\s+min-width="360"[\s\S]*<template #label="\{ label \}">[\s\S]*team-leader-workbench__allocation-order-label/,
  '活跃订单列必须使用弹性最小宽度，并在选择框内提供完整标签。'
)
assert.match(
  allocationTable,
  /<el-table-column\s+label="分配数量"\s+min-width="340"/,
  '分配数量列必须使用弹性最小宽度吸收剩余空间。'
)
assert.match(allocationTable, /label="状态"[\s\S]*label="操作"/, '状态和操作列必须保留。')
assert.match(
  page,
  /\.team-leader-workbench__allocation-order-label\s*\{[\s\S]*white-space:\s*normal;[\s\S]*overflow-wrap:\s*anywhere;/,
  '活跃订单选中内容必须允许完整换行。'
)
assert.match(
  page,
  /\.team-leader-workbench__allocation-quantity-cell\s*\{[\s\S]*grid-template-columns:\s*minmax\(100px, 1fr\) repeat\(3, max-content\);/,
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
