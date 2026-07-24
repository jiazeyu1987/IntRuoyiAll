const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const component = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const processApi = read('src/api/mes/pro/process/index.ts')
const sharedColumns = read('src/views/mes/pro/route/routeProcessSettingsColumns.ts')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

assertIncludes(component, 'processDetailFieldOptions', 'process detail field option map')
assertIncludes(component, 'selectedProcessDetailFields', 'selected process detail rows')
assertIncludes(component, 'routeProcessSettingColumns.value', 'selected fields follow process setting columns')
assertIncludes(component, 'isRouteProcessSettingsDetailColumnKey', 'selected fields filter by shared process setting columns')
assertIncludes(component, 'processDetailFieldOptionMap', 'selected fields resolve by shared column key')
assertIncludes(component, 'ROUTE_PROCESS_SETTINGS_TABLE_KEY', 'route graph shared table key')
assertIncludes(component, 'routeProcessSettingsDefaultColumns', 'route graph shared column definitions')
assertIncludes(component, 'data-flow-detail-field', 'stable selected detail field selector')
assertNotIncludes(component, "editor: 'key-switch'", 'key process field renders as read-only column value')
assertIncludes(component, 'selectedProcessDetailFieldKeys', 'selected detail fields keep user interest keys')
assertIncludes(component, 'selectedProcessDetailFieldToAdd', 'selected detail fields expose add target')
assertIncludes(component, 'availableProcessDetailFieldOptions', 'selected detail fields expose available process setting columns')
assertIncludes(component, 'handleAddProcessDetailField', 'selected detail fields support adding interested columns')
assertIncludes(component, 'handleRemoveProcessDetailField', 'selected detail fields support removing uninterested columns')
assertIncludes(component, 'PROCESS_DETAIL_FIELD_CONFIG_TABLE_KEY', 'selected detail fields persist user interest config')
assertIncludes(component, "key: 'batchRecordFormNames'", 'batch record selectable field comes from shared settings')
assertIncludes(component, "key: 'lossReportFormNames'", 'loss report selectable field comes from shared settings')
assertIncludes(component, "key: 'processInspectionFormNames'", 'process inspection selectable field comes from shared settings')
assertIncludes(component, "key: 'parameterRecordFormNames'", 'parameter record selectable field comes from shared settings')
assertIncludes(component, "key: 'relationList'", 'relation list selectable field comes from shared settings')
assertIncludes(component, 'buildRouteProcessRelationListSummary()', 'relation list selectable field displays relation summary')
assertIncludes(component, 'ProRouteProcessApi.getRouteProcessListByRoute(props.routeId)', 'route graph loads process setting rows')
assertIncludes(
  component,
  'ProProcessApi.getProcess(node.processId, { routeId: props.routeId })',
  'selected process detail must request data in current route context'
)
assertIncludes(
  processApi,
  'getProcess: async (id: number, params?: { routeId?: number })',
  'process detail API must accept route-scoped params'
)
assertIncludes(processApi, 'params: { id, ...params }', 'process detail API must pass routeId as request param')
assertIncludes(sharedColumns, "key: 'processCode'", 'shared columns include process code')
assertIncludes(sharedColumns, "key: 'shiftCapacity'", 'shared columns include schedule strategy')

for (const removed of [
  '<dl class="route-flow-graph-designer__process-detail-meta">'
]) {
  assertNotIncludes(component, removed, 'independent selected detail field controls removed')
}

assertNotIncludes(component, "key: 'productionFillerNames'", 'removed production filler selectable field')
assertNotIncludes(component, "key: 'qualityFillerNames'", 'removed quality filler selectable field')
assertNotIncludes(component, "key: 'equipmentFillerNames'", 'removed equipment filler selectable field')

console.log('mes-route-flow-selectable-detail-fields-static PASS')
