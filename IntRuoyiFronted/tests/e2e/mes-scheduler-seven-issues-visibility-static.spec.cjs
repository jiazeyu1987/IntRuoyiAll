const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const workbench = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)
const workbenchApi = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/schedulerWorkbench/index.ts'),
  'utf8'
)
const scheduleOrder = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduleorder/index.vue'),
  'utf8'
)
const autoScheduleApi = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/task/autoSchedule/index.ts'),
  'utf8'
)
const globalStyles = fs.readFileSync(path.join(repoRoot, 'src/styles/index.scss'), 'utf8')
const nightShiftRealE2E = fs.readFileSync(
  path.join(repoRoot, 'tests/e2e/mes-scheduler-night-shift-save-validation-real.e2e.js'),
  'utf8'
)
const nightShiftCleanBaselinePath = path.join(
  repoRoot,
  'tests/e2e/mes-scheduler-night-shift-clean-baseline-real.e2e.js'
)
assert.ok(
  fs.existsSync(nightShiftCleanBaselinePath),
  '问题 2 必须提供通过真实页面创建任务专用干净排产基线的 E2E。'
)
const nightShiftCleanBaselineE2E = fs.readFileSync(nightShiftCleanBaselinePath, 'utf8')
const target7RealE2E = fs.readFileSync(
  path.join(repoRoot, 'tests/e2e/mes-scheduler-target7-closed-loop-real-flow.e2e.js'),
  'utf8'
)
const nightShiftCapacityStatusRealE2E = fs.readFileSync(
  path.join(repoRoot, 'tests/e2e/mes-scheduler-night-shift-capacity-status-real-readonly.e2e.js'),
  'utf8'
)

assert.match(workbench, /workerCapacityApplicabilityText/)
assert.match(workbenchApi, /workerCapacityApplicabilityText:\s*string/)
assert.doesNotMatch(workbenchApi, /workerCapacityApplicabilityText\?:\s*string/)
assert.doesNotMatch(workbench, /policySettingsForm\.workerCapacityApplicabilityText\s*\|\|/)
assert.match(nightShiftRealE2E, /requiredEnv\('MES_SCHEDULER_NIGHT_E2E_TENANT'\)/)
assert.match(nightShiftRealE2E, /requiredEnv\('MES_SCHEDULER_NIGHT_E2E_USERNAME'\)/)
assert.match(nightShiftRealE2E, /requiredEnv\('MES_SCHEDULER_NIGHT_E2E_PASSWORD'\)/)
assert.match(
  nightShiftRealE2E,
  /requiredPositiveIntegerEnv\('MES_SCHEDULER_NIGHT_E2E_SCHEDULE_ORDER_ID'\)/
)
assert.doesNotMatch(nightShiftRealE2E, /MES_SCHEDULER_NIGHT_E2E_PASSWORD\s*\|\|/)
assert.doesNotMatch(nightShiftRealE2E, /byName\.length \? byName : rows/)
assert.match(nightShiftRealE2E, /row\.scheduleOrderIds\.length === 1/)
assert.match(
  nightShiftRealE2E,
  /Number\(row\.scheduleOrderIds\[0\]\) === config\.scheduleOrderId/
)
assert.match(nightShiftCleanBaselineE2E, /requiredEnv\('MES_SCHEDULER_NIGHT_E2E_TENANT'\)/)
assert.match(nightShiftCleanBaselineE2E, /requiredEnv\('MES_SCHEDULER_NIGHT_E2E_USERNAME'\)/)
assert.match(nightShiftCleanBaselineE2E, /requiredEnv\('MES_SCHEDULER_NIGHT_E2E_PASSWORD'\)/)
assert.match(nightShiftCleanBaselineE2E, /requiredEnv\('MES_SCHEDULER_NIGHT_E2E_ROUTE_CODE'\)/)
assert.match(nightShiftCleanBaselineE2E, /SCHED7-NIGHT-/)
assert.match(nightShiftCleanBaselineE2E, /\/erp\/kingdee-sync/)
assert.match(nightShiftCleanBaselineE2E, /\/admin-api\/erp\/kingdee-sync\/production-order\/create/)
assert.match(nightShiftCleanBaselineE2E, /\/admin-api\/infra\/job\/trigger/)
assert.match(nightShiftCleanBaselineE2E, /\/admin-api\/mes\/pro\/schedule-order\/create-from-work-order/)
assert.doesNotMatch(nightShiftCleanBaselineE2E, /async function api(Post|Put|Delete)/)
assert.match(target7RealE2E, /requiredEnv\('MES_SCHEDULER_TARGET7_E2E_TENANT'\)/)
assert.match(target7RealE2E, /requiredEnv\('MES_SCHEDULER_TARGET7_E2E_USERNAME'\)/)
assert.match(target7RealE2E, /requiredEnv\('MES_SCHEDULER_TARGET7_E2E_PASSWORD'\)/)
assert.doesNotMatch(target7RealE2E, /MES_SCHEDULER_TARGET7_E2E_PASSWORD\s*\|\|/)
assert.match(workbench, /默认允许使用夜班/)
assert.match(workbench, /getNightShiftCapacityStatus/)
assert.match(workbench, /getAutoScheduleJobStatus/)
assert.match(workbench, /自动排产任务未注册/)
assert.match(workbench, /PARTIAL_FAILURE/)
assert.match(workbench, /formatAutoScheduleLatestResultText/)
assert.match(workbench, /latestResultSummary/)
assert.match(workbenchApi, /AutoScheduleJobStatusVO/)
assert.match(workbenchApi, /latestResultSummary\?:\s*string/)
assert.match(workbenchApi, /NightShiftCapacityStatusVO/)
assert.match(nightShiftCapacityStatusRealE2E, /assessTenantJobResult/)
assert.match(nightShiftCapacityStatusRealE2E, /tenantResultAssessment\.expectedStatus/)
assert.match(nightShiftCapacityStatusRealE2E, /PARTIAL_FAILURE/)
assert.match(nightShiftCapacityStatusRealE2E, /FAILURE/)
assert.match(nightShiftCapacityStatusRealE2E, /jobStatus\.latestResultSummary/)
assert.match(
  nightShiftCapacityStatusRealE2E,
  /jobStatusText\.includes\(jobStatus\.latestResultSummary\)/
)
assert.match(nightShiftCapacityStatusRealE2E, /任务级失败不得显示为成功/)
assert.match(scheduleOrder, /计算日期从 00:00 开始/)
assert.match(scheduleOrder, /下一可用班次/)
assert.match(scheduleOrder, /schedule-order-replan-start-date-dialog/)
const replanDialogZIndex = Number(scheduleOrder.match(/:z-index="(\d+)"/)?.[1])
const confirmOverlayZIndex = Number(
  globalStyles.match(/\.app-confirm-message-box-overlay\s*\{[\s\S]*?z-index:\s*(\d+)\s*!important/)?.[1]
)
assert.ok(Number.isFinite(replanDialogZIndex), '开始重排日期确认窗口必须声明明确层级。')
assert.ok(Number.isFinite(confirmOverlayZIndex), '全局二次确认框必须声明明确层级。')
assert.ok(
  replanDialogZIndex < confirmOverlayZIndex,
  'ERP 来源二次确认框必须高于开始重排日期窗口，确保用户可以取消或确认。'
)
assert.match(scheduleOrder, /WARN_ERP_SYNC_RECORD_MISSING/)
assert.match(scheduleOrder, /erpSourceRiskConfirmed: true/)
assert.match(autoScheduleApi, /erpSourceRiskConfirmed\?: boolean/)

console.log('PASS: MES scheduler seven-issues visibility static contract')
