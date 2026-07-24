const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const backendRoot = path.resolve(root, '../ruoyi-vue-pro')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const strategyEditor = read('src/views/mes/pro/route/components/RouteScheduleStrategyEditor.vue')
const routeProcessList = read('src/views/mes/pro/route/RouteProcessList.vue')
const flowGraphDesigner = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const capacityDiffHint = read('src/views/mes/pro/route/components/CapacityDiffHint.vue')
const capacitySourceTag = read('src/views/mes/pro/route/components/CapacitySourceTag.vue')
const schedulerWorkbenchPage = read('src/views/mes/pro/scheduler-workbench/index.vue')
const scheduleOrderPage = read('src/views/mes/pro/scheduleorder/index.vue')
const schedulerWorkbenchPolicyVo = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/schedulerworkbench/vo/MesProSchedulerWorkbenchPolicySettingsRespVO.java'
)

assert.match(
  strategyEditor,
  /:model-value="modelValue \|\| 'RESOURCE_CALCULATED'"/,
  'strategy editor must default missing strategy to RESOURCE_CALCULATED'
)
assert.match(
  strategyEditor,
  /label="MANUAL_OVERRIDE">产能覆盖<\/el-radio-button>/,
  'manual override option must be named 产能覆盖'
)
assert.match(
  strategyEditor,
  /产能\/h/,
  'manual override input must show hourly capacity unit 产能/h'
)
assert.match(
  schedulerWorkbenchPage,
  /label="产能覆盖\(产能\/h\)"/,
  'scheduler workbench policy setting must label manual override as hourly capacity'
)
assert.match(
  schedulerWorkbenchPage,
  /v-model="policySettingsForm\.defaultFiniteHourlyCapacity"[\s\S]*?:precision="6"/,
  'scheduler workbench default capacity override must allow decimal hourly capacity'
)
assert.match(
  schedulerWorkbenchPolicyVo,
  /默认产能覆盖\(产能\/h\)/,
  'backend policy contract must describe default override as hourly capacity'
)
assert.match(
  schedulerWorkbenchPolicyVo,
  /@DecimalMin\(value = "0", inclusive = false, message = "默认产能覆盖必须大于 0"\)/,
  'backend policy contract must validate default override as positive decimal hourly capacity'
)
assert.doesNotMatch(
  schedulerWorkbenchPolicyVo,
  /默认排产产能覆盖\(h\)/,
  'backend policy contract must not describe override as shift or hour-duration capacity'
)
assert.match(
  strategyEditor,
  /hourlyCapacity\?:\s*number/,
  'strategy editor must accept hourlyCapacity directly'
)
assert.match(
  strategyEditor,
  /update:hourlyCapacity/,
  'strategy editor must emit hourlyCapacity changes directly'
)
assert.doesNotMatch(
  strategyEditor,
  /update:shiftCapacity|manualShiftCapacity|v-model:shift-capacity/,
  'strategy editor must not expose manual override as shift capacity'
)

for (const [label, content] of [
  ['route process list', routeProcessList],
  ['flow graph designer', flowGraphDesigner]
]) {
  assert.match(
    content,
    /hourlyCapacity:\s*normalizeHourlyCapacity\(routeScheduleConfig\?\.hourlyCapacity\)/,
    `${label} draft must initialize override value from hourlyCapacity`
  )
  assert.match(
    content,
    /\b(?:schedulePayload|payload)\.hourlyCapacity\s*=\s*resolve(?:Selected)?HourlyCapacity\(draft\)/,
    `${label} save payload must use hourlyCapacity from the override input`
  )
  assert.doesNotMatch(
    content,
    /shiftCapacity\s*\/\s*shiftHours|缺少班次小时，无法折算每小时产能/,
    `${label} must not divide manual override by shift hours`
  )
}

assert.match(
  routeProcessList,
  /v-model:hourly-capacity="getProcessSettingsDraft\(scope\.row\)\.hourlyCapacity"/,
  'route process list must bind strategy editor to hourly capacity'
)
assert.match(
  capacityDiffHint,
  /manualHourlyCapacity/,
  'capacity diff hint must compare manual hourly capacity directly'
)
assert.doesNotMatch(
  capacityDiffHint,
  /manualShiftCapacity|shiftHours/,
  'capacity diff hint must not convert shift capacity into hourly capacity'
)

for (const [label, content] of [
  ['capacity source tag', capacitySourceTag],
  ['scheduler workbench page', schedulerWorkbenchPage],
  ['schedule order page', scheduleOrderPage]
]) {
  assert.doesNotMatch(content, /排产产能覆盖/, `${label} must not use old 排产产能覆盖 copy`)
  assert.match(content, /产能覆盖/, `${label} must use 产能覆盖 copy`)
}

console.log('mes-route-hourly-capacity-override-static PASS')
