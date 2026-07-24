const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  source.includes(':height="scheduleOrderTableHeight"'),
  '排产工单主表必须继续由 Element Plus 表格高度承载滚动。'
)

assert.ok(
  !source.includes("const scheduleOrderTableHeight = 'calc(100vh - 360px)'"),
  '排产工单筛选栏上移后主表高度不得继续扣减 360px，否则分页下方会留下大块空白。'
)

assert.ok(
  source.includes("const scheduleOrderTableHeight = 'calc(100vh - 240px)'"),
  '排产工单主表高度必须扩大到 calc(100vh - 240px)，让列表占用截图红框底部空白。'
)

assert.ok(
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.scheduleOrder\.main"[\s\S]*:show-query-form="false"[\s\S]*<template #table>[\s\S]*<el-table[\s\S]*:height="scheduleOrderTableHeight"/.test(
    source
  ),
  '主列表关闭内置筛选行后，主表高度必须直接应用在主 el-table 上。'
)

console.log('PASS: MES schedule order fill bottom space static contract')
