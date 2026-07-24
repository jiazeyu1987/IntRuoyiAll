const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const flowGraph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const sharedColumns = read('src/views/mes/pro/route/routeProcessSettingsColumns.ts')

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

if (exists('src/views/mes/pro/route/RouteProcessList.vue')) {
  throw new Error('RouteProcessList.vue must stay removed; process settings are configured from flow graph')
}

for (const staleEntry of [
  'label="组成工序"',
  '<RouteProcessList',
  "import RouteProcessList from './RouteProcessList.vue'"
]) {
  assertNotIncludes(routeFormContent, staleEntry, `legacy process tab ${staleEntry}`)
}

assertIncludes(
  sharedColumns,
  "export const ROUTE_PROCESS_SETTINGS_TABLE_KEY = 'mes.pro.route.process.settings'",
  'shared process setting column table key'
)
assertIncludes(sharedColumns, "key: 'formSlots'", 'shared columns expose generic form slot column')
assertIncludes(sharedColumns, "label: '表单槽位'", 'shared columns expose generic form slot label')
assertIncludes(sharedColumns, "key: 'relationList'", 'shared columns expose relation list column')
assertIncludes(sharedColumns, "label: '关系清单'", 'shared columns expose relation list label')

for (const [field, label] of [
  ['batchRecordFormNames', '批记录表单'],
  ['lossReportFormNames', '损耗单'],
  ['processInspectionFormNames', '过程检验单'],
  ['parameterRecordFormNames', '参数记录表']
]) {
  assertIncludes(sharedColumns, `key: '${field}'`, `shared columns keep existing slot field ${field}`)
  assertIncludes(sharedColumns, `label: '${label}'`, `shared columns keep existing slot label ${label}`)
}

assertIncludes(
  flowGraph,
  'useUserTableColumns(ROUTE_PROCESS_SETTINGS_TABLE_KEY, routeProcessSettingsDefaultColumns)',
  'flow graph reuses shared process setting columns'
)
assertIncludes(flowGraph, "const FORM_SLOT_AGGREGATE_FIELD_KEY: RouteProcessSettingColumnKey = 'formSlots'", 'form slot aggregate key')
assertIncludes(flowGraph, "label: getRouteProcessSettingColumnLabel('formSlots', '表单槽位')", 'form slot selectable item label')
assertIncludes(flowGraph, 'buildFormSlotSummaryValue()', 'form slot detail summary')
assertIncludes(flowGraph, 'buildFormSlotSummaryLinks()', 'form slot detail links')
assertIncludes(flowGraph, 'isFormSlotAggregateDetailField(field.key)', 'form slot aggregate card branch')
assertIncludes(flowGraph, 'v-for="slot in RECORD_BINDING_TYPES"', 'aggregate card renders every existing slot type')
assertIncludes(flowGraph, 'data-form-slot-aggregate="true"', 'aggregate card stable selector')
assertIncludes(flowGraph, ':data-form-slot-type="slot.formSlotType"', 'slot type stable selector')
assertIncludes(flowGraph, ':data-route-process-setting-field="`form-slot-${slot.formSlotType}`"', 'slot report selector')
assertIncludes(flowGraph, 'handleSelectedRecordBindingReportChangeByType', 'aggregate card report change handler')
assertIncludes(flowGraph, 'handleSelectedRecordBindingInstanceScopeChangeByType', 'aggregate card instance scope handler')
assertIncludes(flowGraph, 'handleSelectedRecordBindingRequiredPolicyChangeByType', 'aggregate card required policy handler')
assertIncludes(flowGraph, 'handleSelectedRecordBindingSharedFormKeyChangeByType', 'aggregate card shared key handler')
assertIncludes(flowGraph, 'handleSelectedRecordBindingFillableScopeChangeByType', 'aggregate card fillable scope handler')
assertIncludes(flowGraph, 'BatchRecordReportApi.getGeneratedReportPage', 'aggregate card reuses existing report search API')
assertIncludes(flowGraph, 'saveBatchRecordConfig', 'aggregate card reuses existing save contract')
assertIncludes(flowGraph, "key: 'relationList'", 'relation list selectable field comes from shared settings')
assertIncludes(
  flowGraph,
  "label: getRouteProcessSettingColumnLabel('relationList', '关系清单')",
  'relation list selectable item label'
)
assertIncludes(flowGraph, 'buildRouteProcessRelationListSummary()', 'relation list detail summary')
assertIncludes(flowGraph, 'visibleBoundaryRelationEdges', 'relation list reuses boundary relation source')
assertIncludes(flowGraph, 'visibleRouteRelationEdges', 'relation list reuses route relation source')

assertNotIncludes(flowGraph, '<RouteFlowConfigPanel', 'must not restore old config panel')
assertNotIncludes(flowGraph, "editor: 'record-form'", 'must not introduce a second record form editor model')

console.log('mes-route-process-settings-columns-static PASS')
