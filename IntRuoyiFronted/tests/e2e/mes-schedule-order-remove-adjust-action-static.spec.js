const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert(fs.existsSync(pagePath), `排产工单页面必须存在：${pagePath}`)

const source = fs.readFileSync(pagePath, 'utf8')

const operationColumnMatch = source.match(
  /<el-table-column label="操作"[\s\S]*?<template #default="\{ row \}">([\s\S]*?)<\/template>\s*<\/el-table-column>/
)
assert(operationColumnMatch, '排产工单页面必须存在行操作列模板。')

const activeRowActionsMatch = operationColumnMatch[1].match(
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
  '排产工单行操作必须保留综合调整按钮。'
)

for (const forbidden of [
  'submitScheduleOrderAdjust',
  'adjustDialogVisible',
  'adjustSaving',
  'adjustTarget',
  'adjustForm'
]) {
  assert(!source.includes(forbidden), `排产工单页面不应再包含调整入口专用代码：${forbidden}`)
}

assert(source.includes('调整排产工单'), '排产工单页面必须提供综合调整弹窗。')
assert(source.includes('submitPriorityAdjust'), '排产工单页面必须保留优先级调整提交方法。')
assert(source.includes('设置承诺交期'), '排产工单页面必须保留设置承诺交期弹窗。')
assert(source.includes('submitPromiseDateReset'), '排产工单页面必须保留承诺交期提交方法。')
assert(source.includes('updateScheduleOrder'), '排产工单页面必须保留更新接口供交期弹窗提交。')

console.log('PASS: MES schedule order remove adjust action static contract')
