const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const routeGraphPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'route',
  'RouteFlowGraphDesigner.vue'
)

const routeGraph = fs.readFileSync(routeGraphPath, 'utf8')

function assertIncludes(source, expected, label) {
  assert(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

function assertNotIncludes(source, unexpected, label) {
  assert(!source.includes(unexpected), `${label}: unexpected ${JSON.stringify(unexpected)}`)
}

assertIncludes(routeGraph, 'data-flow-detail-link-field', 'detail value link field marker')
assertIncludes(routeGraph, 'data-flow-detail-link-key', 'detail value link key marker')
assertIncludes(routeGraph, 'const selectedProcessDetailFieldKey = ref<ProcessDetailFieldKey>()', 'selected detail field key state')
assertIncludes(routeGraph, 'const selectedProcessDetailField = computed(() =>', 'selected detail field computed value')
assertIncludes(routeGraph, 'processDetailFieldOptionMap.value.get(selectedProcessDetailFieldKey.value)', 'selected detail field must reuse existing option map')
assertIncludes(routeGraph, 'const selectedProcessDetailFieldSource = computed(() =>', 'selected detail field source computed value')
assertIncludes(routeGraph, 'getProcessDetailFieldSourceLabel(selectedProcessDetailFieldKey.value)', 'selected detail field source must reuse existing option groups')
assertIncludes(routeGraph, 'data-flow-action="select-process-detail-field"', 'selected detail field button action')
assertIncludes(routeGraph, 'data-flow-detail-field-button', 'selected detail field button stable marker')
assertIncludes(routeGraph, ':aria-pressed="selectedProcessDetailFieldKey === field.key"', 'selected detail field pressed state')
assertIncludes(routeGraph, ':aria-label="`查看${field.label}字段明细`"', 'selected detail field accessible label')
assertIncludes(routeGraph, '@click="handleSelectProcessDetailField(field.key)"', 'selected detail field click handler')
assertIncludes(routeGraph, 'data-flow-panel="selected-field-detail"', 'right field detail panel marker')
assertIncludes(routeGraph, '点击左侧字段查看明细', 'right field detail empty state')
assertIncludes(routeGraph, 'selectedProcessDetailFieldSource', 'right selected field source label')
assertIncludes(routeGraph, 'formatProcessDetailText(selectedProcessDetailField.value)', 'right field detail value formatter')
assertIncludes(routeGraph, 'selectedProcessDetailField.links', 'right field detail must reuse field links')
assertIncludes(routeGraph, 'selectedProcessDetailField.links?.length', 'right selected field reuses existing detail links')
assertIncludes(routeGraph, ':data-flow-detail-link-field="selectedProcessDetailField.key"', 'right selected field link marker reuses field key')
assertIncludes(routeGraph, 'buildProcessDetailValueLinks', 'central detail value link router')
assertIncludes(routeGraph, 'handleProcessDetailLinkClick(link)', 'detail value click handler')
assertIncludes(routeGraph, "message.error(resolveErrorMessage(error, '详情跳转失败，请检查数据后重试。'))", 'detail value navigation error must be visible')
assertIncludes(routeGraph, "return Boolean(label && label !== '-')", 'configured-missing values still navigate to configuration focus')
assertIncludes(routeGraph, "links: buildProcessDetailValueLinks('workstation'", 'workstation value link')
assertIncludes(routeGraph, "links: buildProcessDetailValueLinks('shiftCapacity'", 'schedule strategy value link')
assertIncludes(routeGraph, "links: buildProcessDetailValueLinks('successors'", 'successor value links')
assertIncludes(routeGraph, "links: buildProcessDetailValueLinks('processCode'", 'process code value link')
assertIncludes(routeGraph, 'links: buildBatchRecordFormLinks()', 'batch record value link')
assertIncludes(routeGraph, 'PROCESS_DETAIL_STANDALONE_RESOURCE_FIELD_KEYS', 'legacy resource focus fields are folded into workstation')
assertIncludes(routeGraph, 'buildWorkstationDetailLinks', 'workstation value must render composite clickable segments')
assertIncludes(routeGraph, 'buildWorkstationMachineryLinks', 'workstation value must include equipment links')
assertIncludes(routeGraph, 'buildWorkstationShiftCapacityLink', 'workstation value must include shift capacity focus link')
assertIncludes(routeGraph, "openSelectedProcessDetailFocusLink('workstation', 'schedule')", 'workstation shift capacity jumps back to workstation capacity focus')
assertIncludes(routeGraph, 'openWorkstationTargetLink', 'workstation navigation handler')
assertIncludes(routeGraph, "path: '/mes/md/workstation'", 'workstation target path')
assertIncludes(routeGraph, "path: '/mes/dv/machinery'", 'workstation equipment target path')
assertIncludes(routeGraph, 'openProcessTargetLink', 'process navigation handler')
assertIncludes(routeGraph, "path: '/mes/pro/process'", 'process target path')
assertIncludes(routeGraph, 'openRecordBindingTargetLink', 'record form navigation handler')
assertIncludes(routeGraph, "path: '/mes/pro/batch-record-form-list'", 'record form target path')
assertIncludes(routeGraph, 'openRouteProcessRelationLink', 'route relation focus handler')
assertIncludes(routeGraph, 'focusRouteProcessNode(relation.routeProcessId)', 'relation click focuses graph node')
assertIncludes(routeGraph, "buildProcessDetailFocusLinks(fieldKey, value, 'schedule')", 'schedule fields jump to schedule focus')
assertIncludes(routeGraph, "buildProcessDetailFocusLinks(fieldKey, value, 'resource')", 'resource fields jump to resource focus')
assertIncludes(routeGraph, "links: buildProcessDetailValueLinks('sort'", 'sort value focuses current graph detail row')
assertIncludes(routeGraph, "links: buildProcessDetailValueLinks('keyFlag'", 'key process value focuses current graph detail row')
assertIncludes(routeGraph, "links: buildProcessDetailValueLinks('checkFlag'", 'quality check value focuses current graph detail row')
assertIncludes(routeGraph, 'visibleRouteRelationEdges', 'relation list must remain available')
assertIncludes(routeGraph, 'visibleBoundaryRelationEdges', 'boundary relation list must remain available')
assertIncludes(routeGraph, 'data-flow-action="select-edge-list"', 'route relation list selection must remain available')
assertNotIncludes(routeGraph, "key: 'capacitySource',\n      label: getRouteProcessSettingColumnLabel('capacitySource'", 'resource type must not remain a standalone detail item')
assertNotIncludes(routeGraph, "key: 'standardResource',\n      label: getRouteProcessSettingColumnLabel('standardResource'", 'standard resource must not remain a standalone detail item')
assertNotIncludes(routeGraph, "key: 'standardShiftCapacity',\n      label: getRouteProcessSettingColumnLabel('standardShiftCapacity'", 'standard shift capacity must not remain a standalone detail item')
assertNotIncludes(routeGraph, 'getProcessFieldDetail', 'must not add redundant field detail API')
assertNotIncludes(routeGraph, 'getRouteProcessFieldDetail', 'must not add redundant route process field detail API')
assertNotIncludes(routeGraph, 'saveSelectedFieldDetail', 'field selection must not add a save API')
assertNotIncludes(routeGraph, 'getProcessDetailFieldDetail', 'field click must not introduce a redundant detail API')
assertNotIncludes(routeGraph, 'selectedProcessDetailValueMap', 'field click must not introduce a second value store')
assertNotIncludes(routeGraph, 'catch {}', 'route graph must not silently swallow navigation errors')
assertNotIncludes(routeGraph, "label !== '未配置'", 'missing configuration labels must remain clickable')

console.log('PASS route flow clickable detail values static contract')
