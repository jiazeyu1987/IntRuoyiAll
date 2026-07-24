const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const routeGraph = fs.readFileSync(
  path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'route', 'RouteFlowGraphDesigner.vue'),
  'utf8'
)
const sharedColumns = fs.readFileSync(
  path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'route', 'routeProcessSettingsColumns.ts'),
  'utf8'
)

function assertIncludes(source, expected, label) {
  assert(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

function assertNotIncludes(source, expected, label) {
  assert(!source.includes(expected), `${label}: must not include ${JSON.stringify(expected)}`)
}

assertIncludes(routeGraph, "from '@/hooks/web/useUserTableColumns'", 'route detail field persistence must reuse shared user column hook')
assertIncludes(routeGraph, 'useUserTableColumns(ROUTE_PROCESS_SETTINGS_TABLE_KEY, routeProcessSettingsDefaultColumns)', 'route graph must load process setting list column config')
assertIncludes(routeGraph, 'routeProcessSettingColumns.value', 'visible items must come from process setting column state')
assertIncludes(routeGraph, 'PROCESS_DETAIL_STANDALONE_RESOURCE_FIELD_KEYS', 'resource fields must be normalized into workstation detail')
assertIncludes(routeGraph, 'normalizeProcessDetailFieldKey', 'saved legacy detail keys must pass through a normalizer')
assertIncludes(routeGraph, "return 'workstation'", 'legacy resource detail keys must map to workstation')
assertIncludes(routeGraph, 'processDetailFieldOptionMap.value.get(key)', 'visible item keys must resolve through shared column key map')
assertIncludes(routeGraph, 'loadRouteProcessSettingColumnConfig', 'route graph must refresh shared column config')
assertIncludes(routeGraph, 'ROUTE_PROCESS_SETTINGS_COLUMN_CONFIG_CHANGED_EVENT', 'route graph must listen for process setting column changes')
assertIncludes(routeGraph, 'handleRouteProcessSettingColumnConfigChanged', 'route graph must handle external column config changes')
assertIncludes(sharedColumns, "export const ROUTE_PROCESS_SETTINGS_TABLE_KEY = 'mes.pro.route.process.settings'", 'stable shared table key')
assertIncludes(sharedColumns, 'routeProcessSettingsDefaultColumns', 'shared default column definitions')
assertIncludes(sharedColumns, 'isRouteProcessSettingsDetailColumnKey', 'shared redbox column filter')
assertIncludes(sharedColumns, "key: 'relationList'", 'shared default columns include relation list')
assertIncludes(routeGraph, "key: 'relationList'", 'route detail field options include relation list')
assertIncludes(routeGraph, 'buildRouteProcessRelationListSummary()', 'relation list uses shared relation summary')

for (const removed of [
  'processDetailFieldConfigLoading',
  'processDetailFieldConfigSaving',
  'processDetailFieldConfigReady',
  'processDetailFieldConfigAvailable',
  'processDetailFieldMutationDisabled',
  ':disabled="processDetailFieldMutationDisabled',
  ':disabled="processDetailFieldMutationDisabled || !selectedProcessDetailFieldToAdd"',
  ':disabled="processDetailFieldMutationDisabled || isRequiredProcessAttributeField(field.key)"',
  'nextQuery[PROCESS_DETAIL_FIELD_QUERY_KEY]',
  'localStorage',
  'sessionStorage'
]) {
  assertNotIncludes(routeGraph, removed, 'old query/local storage detail config removed')
}

assertNotIncludes(routeGraph, "key: 'capacitySource',\n      label: getRouteProcessSettingColumnLabel('capacitySource'", 'resource type must not be addable as an independent detail item')
assertNotIncludes(routeGraph, "key: 'standardResource',\n      label: getRouteProcessSettingColumnLabel('standardResource'", 'standard resource must not be addable as an independent detail item')
assertNotIncludes(routeGraph, "key: 'standardShiftCapacity',\n      label: getRouteProcessSettingColumnLabel('standardShiftCapacity'", 'standard shift capacity must not be addable as an independent detail item')

console.log('PASS route flow detail visible items shared-column static contract')
