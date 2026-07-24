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
  /<el-button[\s\S]*v-hasPermi="\['mes:pro-schedule-order:update'\]"[\s\S]*@click="openPriorityDialog\(row\)"[\s\S]*>\s*调整\s*<\/el-button>/.test(
    activeRowActionsSource
  ),
  '排产工单行操作必须加回受 update 权限保护的“调整”按钮，并打开优先级弹窗。'
)

assert(source.includes('<Dialog v-model="priorityDialogVisible" title="调整优先级"'), '调整按钮必须打开“调整优先级”弹窗。')
assert(source.includes('priorityTarget?.priorityNo'), '优先级弹窗必须展示当前优先级。')
assert(source.includes('v-model="priorityForm.priorityNo"'), '优先级弹窗必须维护新的优先级表单值。')
assert(source.includes('const openPriorityDialog = (row: MesProScheduleOrderVO) =>'), '调整入口必须有独立打开逻辑。')
assert(source.includes('const submitPriorityAdjust = async () =>'), '调整优先级弹窗必须有独立提交逻辑。')
assert(source.includes("message.warning('优先级必须大于等于 1')"), '优先级调整必须 fail fast 校验优先级。')
assert(
  /MesProScheduleOrderApi\.updatePriority\(\{[\s\S]*id: priorityForm\.id[\s\S]*priorityNo: priorityForm\.priorityNo[\s\S]*\}\)/.test(
    source
  ),
  '调整优先级必须调用 updatePriority，并且只提交 id 和 priorityNo。'
)
assert(source.includes("message.success('优先级已调整')"), '调整优先级成功后必须提示优先级已调整。')
assert(source.includes('await getScheduleOrderList()'), '调整优先级成功后必须刷新排产工单列表。')
assert(!source.includes('title="调整排产工单"'), '不得恢复综合“调整排产工单”弹窗。')
assert(!source.includes('adjustForm.promiseDate'), '调整入口不得维护承诺交期字段。')
assert(!source.includes('adjustForm.remark'), '调整入口不得维护备注字段。')
assert(!source.includes('submitScheduleOrderAdjust'), '不得恢复综合调整提交方法。')
assert(source.includes('openPromiseDateDialog'), '交期入口必须继续保留。')
assert(source.includes('submitPromiseDateReset'), '交期提交逻辑必须继续保留。')

console.log('PASS: MES schedule order priority adjust action static contract')
