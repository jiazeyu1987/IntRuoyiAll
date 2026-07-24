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
  "label: '完成筛选'",
  "queryParamKey: 'completionFilter'",
  '未完成',
  '全部',
  '已完成',
  '设为已完成',
  '撤销已完成',
  '人工完成',
  '完成原因',
  '撤销原因',
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
  pageSource.includes("completionFilter: 'INCOMPLETE'"),
  '排产工单页面默认必须使用未完成筛选。'
)
assert(
  pageSource.includes("v-hasPermi=\"['mes:pro-schedule-order:manual-finish']\""),
  '设为已完成按钮必须受排产员人工完成权限控制。'
)
assert(
  pageSource.includes("v-hasPermi=\"['mes:pro-schedule-order:revoke-complete']\""),
  '撤销已完成按钮必须受撤销权限控制。'
)
assert(
  pageSource.includes('列表按 100% 展示；以下工序仍显示真实报工进度'),
  '工艺路线弹窗必须提示人工完成仅影响列表口径。'
)
assert(
  pageSource.includes('确认将该排产工单设为已完成吗') &&
    pageSource.includes('确认撤销该排产工单的人工完成吗'),
  '人工完成和撤销都必须要求二次确认。'
)

console.log('PASS: MES schedule order manual finish static contract')
