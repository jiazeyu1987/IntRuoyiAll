const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleorder/index.ts')
const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

for (const token of [
  'completionFilter',
  "'INCOMPLETE'",
  '/mes/pro/schedule-order/manual-finish',
  '/mes/pro/schedule-order/revoke-manual-finish',
  'manualFinished',
  'manualFinishedTime',
  'manualFinishedBy',
  'manualFinishedReason',
  'MesProScheduleOrderActionReqVO'
]) {
  assert(apiSource.includes(token), `排产工单 API 必须包含 ${token}`)
}

for (const token of [
  "label: '完成状态'",
  "queryParamKey: 'completionFilter'",
  '未完成',
  '全部',
  '已完成',
  '强制完成',
  '撤销强制完成',
  '强制完成原因',
  '撤销强制完成原因',
  '有权限人员执行的强制关闭操作',
  'await message.confirm',
  'manualFinishDialogMode',
  'manualFinishScheduleOrder',
  'revokeManualFinishScheduleOrder',
  'openManualFinishDialog',
  'openRevokeManualFinishDialog',
  'submitManualFinishAction'
]) {
  assert(pageSource.includes(token), `排产工单页面必须包含 ${token}`)
}

assert(
  pageSource.includes(
    "completionFilter: undefined as 'INCOMPLETE' | 'ALL' | 'COMPLETED' | undefined"
  ) && !pageSource.includes("completionFilter: 'INCOMPLETE'"),
  '排产工单页面首屏必须保持完成状态空条件，不得预置隐藏的未完成状态筛选。'
)
assert(
  pageSource.includes("v-hasPermi=\"['mes:pro-schedule-order:manual-finish']\""),
  '强制完成按钮必须受排产员强制关闭权限控制。'
)
assert(
  pageSource.includes("v-hasPermi=\"['mes:pro-schedule-order:revoke-complete']\""),
  '撤销强制完成按钮必须受撤销权限控制。'
)
assert(
  pageSource.includes('汇总按 100% 展示，以下工序仍保留真实进度，可撤销强制完成'),
  '工艺路线弹窗必须说明强制关闭仅覆盖汇总并保留真实工序进度。'
)
assert(
  pageSource.includes('确认强制完成该排产工单吗') &&
    pageSource.includes('确认撤销该排产工单的强制完成吗'),
  '强制完成和撤销都必须要求二次确认。'
)

console.log('PASS: MES schedule order manual finish static contract')
