const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')

const readSource = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const assertNoStaticImport = (source, importPath, fileLabel) => {
  const staticImportPattern = new RegExp(`^import\\s+.*['"]${importPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"]`, 'm')
  assert(
    !staticImportPattern.test(source),
    `${fileLabel} must not statically import ${importPath}; keep first-open route chunk lean.`
  )
}

const feedbackIndex = readSource('src/views/mes/pro/feedback/index.vue')
const feedbackImportForm = readSource('src/views/mes/pro/feedback/ThirdPartyFeedbackImportForm.vue')
const schedulerWorkbench = readSource('src/views/mes/pro/scheduler-workbench/index.vue')
const scheduleCalendar = readSource('src/views/mes/pro/task/calendar/index.vue')
const scheduleOrder = readSource('src/views/mes/pro/scheduleorder/index.vue')

assertNoStaticImport(feedbackIndex, './ThirdPartyFeedbackImportForm.vue', 'feedback/index.vue')
assertNoStaticImport(feedbackIndex, './ImportAttributionDialog.vue', 'feedback/index.vue')
assertNoStaticImport(feedbackIndex, './FeedbackForm.vue', 'feedback/index.vue')
assertNoStaticImport(feedbackImportForm, 'xlsx', 'ThirdPartyFeedbackImportForm.vue')

const workbenchMountedMatch = schedulerWorkbench.match(/onMounted\(async \(\) => \{([\s\S]*?)\n\}\)/)
assert(workbenchMountedMatch, 'scheduler-workbench/index.vue should keep an explicit onMounted block.')
const workbenchMountedBody = workbenchMountedMatch[1]
for (const settingsLoader of ['loadShiftHoursSetting', 'loadPolicySettings', 'loadSmokeTestStatus']) {
  assert(
    !workbenchMountedBody.includes(settingsLoader),
    `scheduler workbench must defer ${settingsLoader} until the settings dialog opens.`
  )
}
assert(
  /openSchedulerSettingsDialog[\s\S]*ensureSchedulerSettingsLoaded/.test(schedulerWorkbench),
  'scheduler workbench should load settings data on demand when the settings dialog opens.'
)

assert(
  /Promise\.all\(\[\s*loadMonthCalendar\(\),\s*loadDayDetail\(selectedDate\.value\)\s*\]\)/.test(
    scheduleCalendar
  ),
  'schedule calendar first open should load month and selected day detail in parallel after rules are ready.'
)

const scheduleOrderMountedMatch = scheduleOrder.match(/onMounted\(async \(\) => \{([\s\S]*?)\n\}\)/)
assert(scheduleOrderMountedMatch, 'scheduleorder/index.vue should keep an explicit onMounted block.')
const scheduleOrderMountedBody = scheduleOrderMountedMatch[1]
assert(
  scheduleOrderMountedBody.includes('await getScheduleOrderList()'),
  'schedule order page should still load the main list on first open.'
)
assert(
  !scheduleOrderMountedBody.includes('getWorkOrderAdmissionList'),
  'schedule order page must keep work-order admission diff loading behind the secondary tab.'
)

console.log('MES schedule tabs first-open performance static checks passed.')
