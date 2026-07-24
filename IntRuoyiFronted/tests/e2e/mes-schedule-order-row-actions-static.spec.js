const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const scheduleOrderPath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')

assert(fs.existsSync(scheduleOrderPath), `排产工单页面必须存在：${scheduleOrderPath}`)

const source = fs.readFileSync(scheduleOrderPath, 'utf8')
const operationColumnMatch = source.match(
  /<el-table-column label="操作"[\s\S]*?<template #default="\{ row \}">([\s\S]*?)<\/template>\s*<\/el-table-column>/
)

assert(operationColumnMatch, '排产工单页面必须存在行操作列模板。')

const operationColumnSource = operationColumnMatch[1]
const activeRowActionsMatch = operationColumnSource.match(
  /<div v-else class="schedule-order-pool__row-actions">([\s\S]*?)<\/div>/
)

assert(activeRowActionsMatch, '排产工单页面必须存在非冻结行操作模板。')

const activeRowActionsSource = activeRowActionsMatch[1]

assert(
  /<el-button[\s\S]*?@click="openProcessDialog\(row\)"[\s\S]*?>[\s\S]*?查看[\s\S]*?<\/el-button>/.test(
    activeRowActionsSource
  ),
  '排产工单行操作必须保留查看按钮。'
)

assert(
  /<el-button[\s\S]*?@click="openPromiseDateDialog\(row\)"[\s\S]*?>[\s\S]*?交期[\s\S]*?<\/el-button>/.test(
    activeRowActionsSource
  ),
  '排产工单行操作必须保留交期按钮。'
)

assert(
  /<el-button[\s\S]*?@click="openPriorityDialog\(row\)"[\s\S]*?>[\s\S]*?调整[\s\S]*?<\/el-button>/.test(
    activeRowActionsSource
  ),
  '排产工单行操作必须保留只调整优先级的调整按钮。'
)

assert(
  /<el-button[\s\S]*?@click="openFreezeDialog\(row\)"[\s\S]*?>[\s\S]*?冻结[\s\S]*?<\/el-button>/.test(
    activeRowActionsSource
  ),
  '排产工单行操作必须保留冻结按钮。'
)

for (const forbidden of [
  'openUnfreezeDialog(row)',
  'openOperationLogDialog(row)',
  ':command="`${row.id}:compare`"',
  ':command="`${row.id}:process`"',
  ':command="`${row.id}:delete`"',
  ':command="`${row.id}:trace`"',
  '更多',
  '解冻',
  '追溯'
]) {
  assert(!activeRowActionsSource.includes(forbidden), `排产工单行操作不应再包含：${forbidden}`)
}

console.log('PASS: MES schedule order row actions static contract')
