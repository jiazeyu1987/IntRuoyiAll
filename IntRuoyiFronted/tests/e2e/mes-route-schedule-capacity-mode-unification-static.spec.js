const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeApi = read('src/api/mes/pro/route/index.ts')
const flowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')
const schedulerWorkbenchApi = read('src/api/mes/pro/schedulerWorkbench/index.ts')
const flowGraphDesigner = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const routeProcessList = read('src/views/mes/pro/route/RouteProcessList.vue')
const strategyEditor = read('src/views/mes/pro/route/components/RouteScheduleStrategyEditor.vue')
const resourcePreview = read('src/views/mes/pro/route/components/RouteResourceCapacityPreview.vue')
const capacitySourceTag = read('src/views/mes/pro/route/components/CapacitySourceTag.vue')
const capacityDiffHint = read('src/views/mes/pro/route/components/CapacityDiffHint.vue')
const schedulerWorkbenchPage = read('src/views/mes/pro/scheduler-workbench/index.vue')
const scheduleOrderApi = read('src/api/mes/pro/scheduleorder/index.ts')
const scheduleOrderPage = read('src/views/mes/pro/scheduleorder/index.vue')
const oldManualOverrideCopy = ['手工', '覆盖'].join('')

for (const [label, content] of [
  ['route API', routeApi],
  ['flow config API', flowConfigApi]
]) {
  assert.match(content, /RESOURCE_CALCULATED/, `${label} must expose resource-calculated mode`)
  assert.match(content, /MANUAL_OVERRIDE/, `${label} must expose manual override mode`)
  assert.match(content, /INFINITE_FORMULA/, `${label} must keep infinite formula mode`)
}

assert.doesNotMatch(
  schedulerWorkbenchApi,
  /defaultScheduleCapacityMode:\s*['"][^'"]*FINITE_HOURLY/,
  'scheduler workbench policy API must not expose FINITE_HOURLY as a selectable default mode'
)
assert.match(
  schedulerWorkbenchApi,
  /defaultScheduleCapacityMode:\s*['"]RESOURCE_CALCULATED['"]\s*\|\s*['"]MANUAL_OVERRIDE['"]\s*\|\s*['"]INFINITE_FORMULA['"]/,
  'scheduler workbench policy API must expose the unified default strategy set'
)
assert.doesNotMatch(
  schedulerWorkbenchPage,
  /<el-option[^>]+value=["']FINITE_HOURLY["']/,
  'scheduler workbench settings must not offer legacy FINITE_HOURLY'
)
assert.match(
  schedulerWorkbenchPage,
  /value=["']RESOURCE_CALCULATED["']/,
  'scheduler workbench settings must offer resource-calculated default mode'
)
assert.doesNotMatch(
  schedulerWorkbenchPage,
  new RegExp(`label=["']${oldManualOverrideCopy}["']`),
  'scheduler workbench settings must not expose old manual override label'
)
assert.match(
  schedulerWorkbenchPage,
  /label=["']产能覆盖["']\s+value=["']MANUAL_OVERRIDE["']/,
  'scheduler workbench settings must label MANUAL_OVERRIDE as 产能覆盖'
)
assert.match(
  schedulerWorkbenchPage,
  /defaultScheduleCapacityMode:\s*['"]RESOURCE_CALCULATED['"]/,
  'scheduler workbench local default must follow the unified resource-calculated default'
)
assert.match(
  routeApi,
  /resource-preview/,
  'route API must expose backend resource preview instead of local resource calculation'
)
assert.match(
  strategyEditor,
  /RouteResourceCapacityPreview/,
  'strategy editor must embed resource capacity preview'
)
assert.match(strategyEditor, /CapacityDiffHint/, 'strategy editor must show manual-vs-resource diff')
assert.match(resourcePreview, /getScheduleResourcePreview/, 'resource preview must call backend preview API')
assert.match(capacitySourceTag, /RESOURCE_CALCULATED/, 'capacity source tag must label resource calculated mode')
assert.match(capacityDiffHint, /manualHourlyCapacity/, 'capacity diff hint must compare manual hourly capacity')
for (const [label, content] of [
  ['scheduler workbench page', schedulerWorkbenchPage],
  ['schedule order page', scheduleOrderPage],
  ['capacity source tag', capacitySourceTag],
  ['capacity diff hint', capacityDiffHint]
]) {
  assert.doesNotMatch(content, new RegExp(oldManualOverrideCopy), `${label} must not expose old manual override copy`)
  assert.doesNotMatch(content, /排产产能覆盖/, `${label} must not expose old 排产产能覆盖 copy`)
  assert.match(content, /产能覆盖/, `${label} must expose 产能覆盖 copy`)
}
assert.match(
  scheduleOrderPage,
  /MANUAL_OVERRIDE:\s*['"]产能覆盖['"]/,
  'schedule order capacity mode text must label MANUAL_OVERRIDE as 产能覆盖'
)
assert.match(
  scheduleOrderPage,
  /capacitySourceTextMap[\s\S]*MANUAL_OVERRIDE:\s*['"]产能覆盖['"]/,
  'schedule order capacity source text must label MANUAL_OVERRIDE as 产能覆盖'
)
assert.match(
  capacitySourceTag,
  /case ['"]MANUAL_OVERRIDE['"]:[\s\S]*return ['"]产能覆盖['"]/,
  'capacity source tag must label MANUAL_OVERRIDE as 产能覆盖'
)
assert.match(
  capacityDiffHint,
  /产能覆盖与资源计算差异/,
  'capacity diff hint must explain 产能覆盖 difference'
)

for (const [label, content] of [
  ['flow graph designer', flowGraphDesigner],
  ['route process list', routeProcessList]
]) {
  assert.doesNotMatch(
    content,
    /capacityMode:\s*['"]FINITE_HOURLY['"]/,
    `${label} must not hard-code legacy FINITE_HOURLY when saving schedule capacity`
  )
  assert.match(
    content,
    /normalizeScheduleCapacityMode\(draft\.capacityMode\)/,
    `${label} must normalize legacy FINITE_HOURLY to explicit MANUAL_OVERRIDE before saving`
  )
  assert.match(
    content,
    /nightShiftEnabled:\s*draft\.nightShiftEnabled/,
    `${label} must preserve night shift flag in save payload`
  )
}

assert.match(
  routeProcessList,
  /v-model:night-shift-enabled=/,
  'route process list strategy editor must expose the night shift switch'
)
assert.match(
  routeProcessList,
  /v-model:calendar-rule-id=/,
  'route process list strategy editor must expose the calendar rule id'
)

assert.match(
  flowGraphDesigner,
  /serializeSelectedProcessAttributesDraft[\s\S]*capacityMode:\s*draft\.capacityMode\s*\?\?\s*null/,
  'flow graph draft baseline must include capacityMode so saving the graph does not silently reset strategy mode'
)
assert.match(
  flowGraphDesigner,
  /buildSelectedProcessAttributesDraft[\s\S]*capacityMode:\s*normalizeScheduleCapacityMode\(routeScheduleConfig\?\.capacityMode\)/,
  'flow graph selected attribute draft must initialize missing capacityMode as RESOURCE_CALCULATED while normalizing legacy FINITE_HOURLY'
)
assert.match(
  routeProcessList,
  /capacityMode:\s*normalizeScheduleCapacityMode\(routeScheduleConfig\?\.capacityMode\)/,
  'route process list draft must initialize missing capacityMode as RESOURCE_CALCULATED while normalizing legacy FINITE_HOURLY'
)
assert.match(
  strategyEditor,
  /:model-value="modelValue \|\| 'RESOURCE_CALCULATED'"/,
  'strategy editor must default visible mode to resource-calculated'
)
assert.match(
  strategyEditor,
  /label="MANUAL_OVERRIDE">产能覆盖<\/el-radio-button>/,
  'manual override mode must be visibly named 产能覆盖'
)
assert.match(
  strategyEditor,
  /<span>产能覆盖<\/span>/,
  'manual override capacity field must be labelled 产能覆盖'
)
assert.match(strategyEditor, /nightShiftEnabled\?:\s*boolean\s*\|\s*null/, 'strategy editor props must include nightShiftEnabled')
assert.match(strategyEditor, /calendarRuleId\?:\s*number\s*\|\s*null/, 'strategy editor props must include calendarRuleId')
assert.match(strategyEditor, /update:nightShiftEnabled/, 'strategy editor must emit nightShiftEnabled changes')
assert.match(strategyEditor, /update:calendarRuleId/, 'strategy editor must emit calendarRuleId changes')
assert.match(
  scheduleOrderApi,
  /interface MesProScheduleOrderProcessVO[\s\S]*capacityMode\?:\s*['"]RESOURCE_CALCULATED['"]\s*\|\s*['"]MANUAL_OVERRIDE['"]/,
  'schedule order process API type must expose frozen route strategy capacityMode'
)
assert.match(scheduleOrderPage, /getProcessCapacityModeText/, 'schedule order detail must render process capacity mode text')
assert.match(scheduleOrderPage, /resourceSnapshotJson/, 'schedule order detail must parse and display frozen resource snapshot')
assert.match(scheduleOrderPage, /capacitySource/, 'schedule order detail must render capacity source')

console.log('mes-route-schedule-capacity-mode-unification-static PASS')
