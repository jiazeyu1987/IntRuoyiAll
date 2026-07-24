const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} must include: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const flowGraphDesigner = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const flowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')
const routeResourceTable = read('src/views/mes/pro/route/RouteResourceTable.vue')
const calendarPage = read('src/views/mes/pro/task/calendar/index.vue')
const scheduleOrderPage = read('src/views/mes/pro/scheduleorder/index.vue')

if (exists(['src/views/mes/pro', `schedule${'-'}route`, 'index.vue'].join('/'))) {
  throw new Error('old schedule-route page must be deleted')
}
if (exists(['src/views/mes/pro', `edhr${'-'}batch${'-'}route`, 'index.vue'].join('/'))) {
  throw new Error('old edhr-batch-route page must be deleted')
}
if (exists(['src/views/mes/pro', `route${'-'}use`, 'RouteUsePage.vue'].join('/'))) {
  throw new Error('old route-use page must be deleted')
}
if (exists(['src/api/mes/pro/route', `use${'config'}.ts`].join('/'))) {
  throw new Error('old route-use API file must be deleted')
}
if (exists('src/views/mes/pro/route/RouteFlowConfigPanel.vue')) {
  throw new Error('old route flow config panel must be deleted')
}

assertNotIncludes(routeFormContent, 'label="排产配置" name="schedule-config"', 'route form')
assertNotIncludes(routeFormContent, 'label="批记录配置" name="batch-record-config"', 'route form')
assertNotIncludes(routeFormContent, '<RouteFlowConfigPanel', 'route form')
assertIncludes(routeFormContent, ':target-route-process-id="targetRouteProcessId"', 'route form schedule config deep link')
assertNotIncludes(routeEditPage, "'schedule-config'", 'route edit URL tab resolver')
assertNotIncludes(routeEditPage, "'batch-record-config'", 'route edit URL tab resolver')
assertIncludes(routeEditPage, 'route.query.routeProcessId', 'route edit process deep link resolver')
assertIncludes(routeEditPage, "return 'flow'", 'route edit default tab resolver')
assertNotIncludes(flowGraphDesigner, '<RouteFlowConfigPanel', 'flow graph designer')
assertNotIncludes(flowGraphDesigner, 'data-flow-field-editor', 'flow graph designer selected detail editor')
assertNotIncludes(flowGraphDesigner, 'RouteScheduleStrategyEditor', 'flow graph designer schedule strategy inline editor')
assertIncludes(flowGraphDesigner, 'formatRouteProcessScheduleStrategySummary', 'flow graph designer schedule strategy column summary')
assertNotIncludes(flowGraphDesigner, 'data-flow-action="save-selected-process-settings"', 'flow graph designer dedicated process setting save action removed')
assertNotIncludes(flowGraphDesigner, '保存工序设置', 'flow graph designer process setting save label removed')
assertIncludes(flowGraphDesigner, 'routeProcessSettingColumns.value', 'flow graph designer process setting column source')
assertIncludes(flowGraphDesigner, 'isRouteProcessSettingsDetailColumnKey', 'flow graph designer shared process setting column filter')
assertNotIncludes(flowGraphDesigner, 'REQUIRED_PROCESS_ATTRIBUTE_FIELD_KEYS', 'flow graph designer no longer keeps an independent required field list')
assertNotIncludes(flowGraphDesigner, 'isRequiredProcessAttributeField', 'flow graph designer no longer keeps independent remove guards')
assertIncludes(flowGraphDesigner, "'productionQuantityFactor'", 'flow graph designer production factor required field')
assertIncludes(flowGraphDesigner, "'shiftCapacity'", 'flow graph designer shift capacity required field')
assertIncludes(flowGraphDesigner, "'lossReportFormNames'", 'flow graph designer loss report required field')
assertIncludes(flowGraphDesigner, "'processInspectionFormNames'", 'flow graph designer inspection required field')
assertIncludes(flowGraphDesigner, "'parameterRecordFormNames'", 'flow graph designer parameter record required field')
assertNotIncludes(flowGraphDesigner, '保存属性', 'flow graph designer must not show generic attribute save label')

assertIncludes(flowConfigApi, "url: '/mes/pro/route/flow-config'", 'flow config API')
assertIncludes(flowConfigApi, "url: '/mes/pro/route/flow-config/schedule/save'", 'flow config API')
assertIncludes(flowConfigApi, "url: '/mes/pro/route/flow-config/batch-record/save'", 'flow config API')
assertNotIncludes(flowConfigApi, "url: '/mes/pro/route/flow-config/enabled'", 'flow config API')
assertNotIncludes(flowConfigApi, 'updateEnabled', 'flow config API')
assertNotIncludes(flowConfigApi, 'route-use-config', 'flow config API')

for (const [label, content] of [
  ['route resource table', routeResourceTable],
  ['calendar page', calendarPage],
  ['schedule order page', scheduleOrderPage],
  ['route form content', routeFormContent]
]) {
  assertNotIncludes(content, 'mes:pro-schedule-route', label)
  assertNotIncludes(content, `mes:pro-${'batch'}-record-route`, label)
  assertNotIncludes(content, ['/mes/pro', `schedule${'-'}route`].join('/'), label)
  assertNotIncludes(content, ['/mes/pro/feedback', `edhr${'-'}batch${'-'}route`].join('/'), label)
  assertNotIncludes(content, '工艺排产路线', label)
  assertNotIncludes(content, '工艺批记录路线', label)
}

assertIncludes(calendarPage, "path: '/mes/pro/route'", 'calendar recovery')
assertIncludes(calendarPage, "tab: 'flow'", 'calendar recovery')
assertIncludes(calendarPage, '打开流转关系图工序设置', 'calendar recovery')
assertNotIncludes(calendarPage, '打开工艺流程排产配置', 'calendar recovery')
assertIncludes(scheduleOrderPage, "name: 'MesProRouteEdit'", 'schedule order route entry')
assertIncludes(scheduleOrderPage, "tab: 'flow'", 'schedule order route entry')
assertIncludes(scheduleOrderPage, 'routeProcessId: row.currentRouteProcessId', 'schedule order route entry')

console.log('mes-route-flow-config-unification-static PASS')
