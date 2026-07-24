const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/schedulerWorkbench/index.ts'),
  'utf8'
)

for (const label of [
  '同步时',
  '重排时',
  '优先级',
  '保护项',
  '智能排产',
  '产能模式',
  '产能覆盖(产能/h)',
  '夜班',
  '人数',
  '人效h'
]) {
  assert(pageSource.includes(label), `排产员工作台必须显示 ${label}`)
}

for (const token of [
  'policySettingsForm.erpWorkOrderSyncTime',
  'policySettingsForm.nightlyReplanTime',
  'policySettingsForm.priorityRule',
  'policySettingsForm.protectReportedTasks',
  'policySettingsForm.protectCompletedTasks',
  'policySettingsForm.protectLockedTasks',
  'policySettingsForm.defaultScheduleUseEnabled',
  'policySettingsForm.defaultScheduleCapacityMode',
  'policySettingsForm.defaultFiniteHourlyCapacity',
  'policySettingsForm.defaultNightShiftEnabled',
  'policySettingsForm.defaultWorkerQuantity',
  'policySettingsForm.defaultWorkerSingleHourlyCapacity'
]) {
  assert(pageSource.includes(token), `排产员工作台策略表单缺少 ${token}`)
}

assert(
  apiSource.includes('/mes/pro/scheduler-workbench/policy-settings') &&
    apiSource.includes('getPolicySettings') &&
    apiSource.includes('savePolicySettings'),
  '排产员工作台 API 必须提供策略设置读写接口。'
)

console.log('mes-scheduler-workbench-policy-settings-static.spec.js passed')
