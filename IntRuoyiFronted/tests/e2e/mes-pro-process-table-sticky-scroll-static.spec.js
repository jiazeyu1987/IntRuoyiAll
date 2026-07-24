const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/process/index.vue'), 'utf8')

const mainTableMatch = pageSource.match(
  /<UnifiedListTemplate[\s\S]*?table-key="mes\.pro\.process\.main"[\s\S]*?<template #table(?:="[^"]*")?>[\s\S]*?<el-table([\s\S]*?)>/
)

assert(mainTableMatch, '工序主列表必须继续由 mes.pro.process.main 的 UnifiedListTemplate table 插槽渲染。')

const mainTableAttrs = mainTableMatch[1]

assert.match(
  mainTableAttrs,
  /class="process-main-table"/,
  '工序主表格必须有局部类名，避免滚动样式影响设备明细弹窗或其它表格。'
)

assert.match(
  mainTableAttrs,
  /:height="processMainTableHeight"/,
  '工序主表格必须通过 Element Plus height 使用内部 body wrapper 滚动，从而固定表头。'
)

assert.match(
  pageSource,
  /const processMainTableHeight = 'max\(360px, calc\(100vh - 300px\)\)'/,
  '工序主表格高度必须使用视口计算并设置最小高度，保证横向滚动条位于表格内部可视底部。'
)

assert.match(
  pageSource,
  /\.process-main-table\s*:deep\(\.el-table__body-wrapper\)[\s\S]*overflow-y:\s*auto/,
  '工序主表格 body wrapper 必须允许纵向内部滚动。'
)

assert.match(
  pageSource,
  /\.process-main-table\s*:deep\(\.el-scrollbar__bar\.is-horizontal\)[\s\S]*display:\s*block/,
  '工序主表格横向滚动条必须保持显示在表格内部。'
)

const dialogStart = pageSource.indexOf('<Dialog :title="processMachineryDialogTitle"')
const dialogSource = dialogStart >= 0 ? pageSource.slice(dialogStart) : ''
const dialogTableMatch = dialogSource.match(/<el-table([\s\S]*?)>/)

assert(dialogTableMatch, '设备明细弹窗必须继续渲染自己的 el-table。')

assert.doesNotMatch(
  dialogTableMatch[1],
  /process-main-table/,
  '设备明细弹窗表格不得复用主表格滚动类名。'
)

assert.doesNotMatch(pageSource, /fallback|mock/i, '本次滚动修复不得引入 fallback 或 mock 逻辑。')

console.log('PASS: MES process table keeps header fixed and horizontal scrollbar visible')
