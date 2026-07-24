const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const component = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

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

assertIncludes(component, "ProProcessApi,", 'process setting detail API import')
assertNotIncludes(component, 'type ProProcessBatchRecordFormLinkVO', 'process detail form link type import removed')
assertIncludes(component, 'type ProProcessMachineryVO', 'process detail machinery link type import')
assertIncludes(component, 'type ProProcessVO', 'process setting detail VO type import')
assertIncludes(component, 'ProRouteFlowConfigApi', 'selected process route flow config API import')
assertIncludes(component, 'selectedProcessDetail', 'selected process detail state')
assertIncludes(component, 'selectedProcessMachineryList', 'selected process machinery link state')
assertIncludes(component, 'selectedProcessDetailLoading', 'selected process detail loading state')
assertIncludes(component, 'selectedProcessAttributes', 'selected process editable attributes state')
assertIncludes(component, 'selectedRecordBindings', 'selected process record binding state')
assertIncludes(component, 'loadSelectedProcessDetail', 'selected process detail loader')
assertIncludes(component, 'ProProcessApi.getProcess(node.processId, { routeId: props.routeId })', 'route scoped process setting detail source')
assertIncludes(component, 'getTemplatePool', 'selected process form slot editor must load real form center template options')
assertIncludes(component, 'ProProcessApi.getProcessMachineryList(node.processId)', 'process machinery detail link source')
assertIncludes(component, 'route-flow-graph-designer__process-detail-sidebar', 'left detail sidebar')
assertIncludes(component, 'data-flow-action="open-process-detail-link"', 'clickable selected detail link action')
assertNotIncludes(component, 'openBatchRecordFormLink', 'batch record form link handler removed')
assertIncludes(component, 'openMachineryTargetLink', 'machinery target link handler')
assertNotIncludes(component, '<RouteFlowConfigPanel', 'old node config panel removed')
assertIncludes(component, 'data-flow-field-editor', 'selected process detail must preserve editable process setting fields')
assertIncludes(component, 'PROCESS_DETAIL_EDITABLE_FIELD_KEYS', 'selected process detail editable fields must stay behind explicit whitelist')
assertIncludes(component, 'loadFormTemplateOptions', 'form slot editor must use real template search options')
assertNotIncludes(component, 'data-flow-action="save-selected-process-settings"', 'dedicated selected process setting save action removed')
assertNotIncludes(component, '保存工序设置', 'selected process setting save label removed')
assertNotIncludes(component, '保存属性', 'generic attribute save label removed')
assertNotIncludes(component, '选中工序详情', 'removed left detail panel title')
assertIncludes(component, 'data-flow-panel="selected-process-detail"', 'stable selected detail panel selector')
assertIncludes(component, 'route-flow-graph-designer__selected-full-name', 'selected process full name toolbar slot')
assertIncludes(component, 'v-if="selectedNode"', 'selected full name only appears when a node is selected')
assertIncludes(component, ':title="selectedNodeFullName"', 'selected full name title keeps complete value')
assertIncludes(component, '{{ selectedNodeFullName }}', 'selected full name visible text')
assertIncludes(component, '工序编码', 'process code detail label')
assertIncludes(component, '批记录表单', 'batch record form detail label')
assertIncludes(component, '请选择工序查看详情', 'empty selected process hint')
assertIncludes(component, 'routeProcessSettingColumns.value', 'selected process detail fields follow process setting columns')
assertIncludes(component, 'isRouteProcessSettingsDetailColumnKey', 'selected process detail filters by shared columns')
assertNotIncludes(component, '工艺要求', 'process attention detail is removed because it is not a process setting list column')
assertNotIncludes(component, 'type ProProcessFillerLinkVO', 'removed process detail filler link type')
assertNotIncludes(component, 'openFillerTargetLink', 'removed filler target link handler')
assertNotIncludes(component, '生产填写人', 'removed production filler detail label')
assertNotIncludes(component, '质量填写人', 'removed quality filler detail label')
assertNotIncludes(component, '设备填写人', 'removed equipment filler detail label')
assertIncludes(component, 'selectedRouteProcessId,', 'selection detail watch')
assertIncludes(component, 'void loadSelectedProcessDetail(selectedNode.value)', 'selection triggers process setting detail load')
assertNotIncludes(component, '<h4>工序列表</h4>', 'left process list title')
assertNotIncludes(component, 'route-flow-graph-designer__process-list', 'left process list body')
assertNotIncludes(component, 'data-flow-action="select-process"', 'left process list selection action')
assertNotIncludes(component, '请选择工序查看详情</span>', 'toolbar selected name placeholder text')

console.log('mes-route-flow-selected-process-detail-static PASS')
