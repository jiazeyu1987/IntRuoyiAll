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

assertIncludes(component, 'route-flow-graph-designer__process-detail-sidebar', 'left process detail sidebar')
assertNotIncludes(component, '选中工序详情', 'removed left process detail title')
assertIncludes(component, 'data-flow-panel="selected-process-detail"', 'stable selected process detail panel')
assertNotIncludes(component, 'data-flow-action="toggle-key-process"', 'key process column must not render an independent red-box switch')
assertIncludes(component, 'loadSelectedProcessDetail', 'selected process detail loader')
assertIncludes(
  component,
  'ProProcessApi.getProcess(node.processId, { routeId: props.routeId })',
  'route-scoped process setting detail source'
)
assertNotIncludes(component, 'keyProcessSavingRouteProcessId', 'key process toggle must not use immediate row-level saving state')
assertIncludes(component, 'saveRouteProcessKeyFlagDrafts', 'key process draft save is persisted by top-level route save')
assertIncludes(component, '关键工序已保存为草稿', 'key process toggle must show draft-save feedback')
assertIncludes(component, 'findDefaultKeyProcessNode', 'default key process resolver')
assertIncludes(component, 'sorted[sorted.length - 1]', 'last process is default key process')
assertIncludes(component, 'applyDefaultKeyProcessLocally', 'default key process local application')
assertIncludes(component, 'routeNodes.value.some((node) => Boolean(node.keyFlag))', 'default only applies when no key process exists')
assertIncludes(component, 'grid-template-columns: 220px minmax(0, 1fr) 260px;', 'left detail sidebar grid layout')
assertIncludes(component, '关键工序', 'key process visible label')
assertIncludes(component, '请选择工序查看详情', 'empty selected process hint')
assertIncludes(component, '工序编码', 'process setting detail field')
assertIncludes(component, "key: 'keyFlag'", 'key process must be a shared process setting detail field')
assertNotIncludes(component, "editor: 'key-switch'", 'key process shared field must render as column value only')
assertIncludes(component, 'routeProcessSettingColumns.value', 'left detail sidebar fields must follow process setting columns')
assertNotIncludes(component, '工艺要求', 'process attention is not a process setting list column')

console.log('mes-route-flow-key-process-sidebar-static PASS')
